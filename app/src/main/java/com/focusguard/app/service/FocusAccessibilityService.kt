package com.focusguard.app.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.data.repository.BlockRepository
import com.focusguard.app.data.repository.BrainPalRepository
import com.focusguard.app.util.WidgetStore
import com.focusguard.app.widget.ClatWidgets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * FocusAccessibilityService
 *  1. BLOCKING  – when a blocked app comes to the front (and the block rules say
 *                 "now"), shows the block screen on top of it.
 *  2. COUNTING  – counts Reels / Shorts by detecting each swipe inside the
 *                 short-video screen of Instagram, YouTube, TikTok, Snapchat.
 *
 * It only looks at window titles/ids to decide *where* you are. It never reads
 * or stores any text, messages or passwords.
 */
@AndroidEntryPoint
class FocusAccessibilityService : AccessibilityService() {

    @Inject lateinit var blockRepository: BlockRepository
    @Inject lateinit var brainPalRepository: BrainPalRepository
    @Inject lateinit var userPreferences: UserPreferences

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var overlay: BlockOverlayManager

    private var currentBlockedPkg: String? = null
    private val tempAllowUntil = HashMap<String, Long>()

    // reel counting
    private class Pending(var count: Int = 0, var firstMs: Long = 0L)
    private val pending = HashMap<String, Pending>()
    private val lastCountMs = HashMap<String, Long>()

    /**
     * Where a short-video screen can be recognised. `ids` = pieces of view-id
     * names that only exist on the Reels/Shorts player. `everything` = the whole
     * app is short videos (TikTok), so any swipe counts.
     */
    private data class Tracked(val ids: List<String>, val everything: Boolean = false)

    private val tracked: Map<String, Tracked> = mapOf(
        "com.instagram.android"          to Tracked(listOf("clips_viewer", "reel_viewer")),
        "com.google.android.youtube"     to Tracked(listOf("reel_", "shorts_")),
        "com.zhiliaoapp.musically"       to Tracked(emptyList(), everything = true),
        "com.ss.android.ugc.trill"       to Tracked(emptyList(), everything = true),
        "com.snapchat.android"           to Tracked(listOf("spotlight")),
    )

    companion object {
        @Volatile var isRunning = false
            private set
        private const val MIN_SWIPE_GAP_MS = 800L
        private const val UNLOCK_MS = 5 * 60 * 1000L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        overlay = BlockOverlayManager(this)
        scope.launch {
            while (isActive) {
                delay(15_000)
                flushPending()
                // catches "focus session just started while a blocked app is already open"
                val pkg = rootInActiveWindow?.packageName?.toString()
                if (pkg != null) evaluateBlocking(pkg)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (isIgnorable(pkg)) return
                if (pkg !in tracked) flushPending()
                evaluateBlocking(pkg)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScroll(pkg)
        }
    }

    // ── Blocking ──────────────────────────────────────────────────────
    private fun isIgnorable(pkg: String) =
        pkg == "com.android.systemui" || pkg == "android" ||
            pkg.contains("inputmethod") || pkg.contains("keyboard")

    private fun evaluateBlocking(pkg: String) {
        if (isIgnorable(pkg) || pkg == packageName) return
        scope.launch {
            val unlocked = (tempAllowUntil[pkg] ?: 0L) > System.currentTimeMillis()
            val block = !unlocked && blockRepository.shouldBlock(pkg)
            val strict = if (block) userPreferences.strictMode.first() else false
            withContext(Dispatchers.Main) {
                if (block) {
                    if (currentBlockedPkg != pkg || !overlay.isShowing) {
                        currentBlockedPkg = pkg
                        overlay.show(
                            appName = labelOf(pkg),
                            daysLeft = WidgetStore.daysLeft(this@FocusAccessibilityService),
                            strictMode = strict,
                            onGoHome = {
                                overlay.hide()
                                currentBlockedPkg = null
                                performGlobalAction(GLOBAL_ACTION_HOME)
                            },
                            onTempUnlock = {
                                tempAllowUntil[pkg] = System.currentTimeMillis() + UNLOCK_MS
                                overlay.hide()
                                currentBlockedPkg = null
                            }
                        )
                    }
                } else if (currentBlockedPkg != null) {
                    overlay.hide()
                    currentBlockedPkg = null
                }
            }
        }
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) { pkg }

    // ── Reel / Short counting ─────────────────────────────────────────
    private fun handleScroll(pkg: String) {
        val cfg = tracked[pkg] ?: return
        val now = System.currentTimeMillis()
        // one swipe fires many scroll events – only count one per swipe
        if (now - (lastCountMs[pkg] ?: 0L) < MIN_SWIPE_GAP_MS) return

        val inShortVideo = cfg.everything || isOnShortVideoScreen(cfg.ids)
        if (!inShortVideo) return

        lastCountMs[pkg] = now
        val p = pending.getOrPut(pkg) { Pending(firstMs = now) }
        p.count++
    }

    @Suppress("DEPRECATION")
    private fun isOnShortVideoScreen(idFragments: List<String>): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            var visited = 0
            var found = false
            while (queue.isNotEmpty() && visited < 500 && !found) {
                val n = queue.removeFirst()
                visited++
                val id = n.viewIdResourceName
                if (id != null && idFragments.any { id.contains(it, ignoreCase = true) }) found = true
                for (i in 0 until n.childCount) n.getChild(i)?.let { queue.add(it) }
            }
            found
        } catch (_: Exception) { false }
    }

    private fun flushPending() {
        if (pending.isEmpty()) return
        val snapshot = HashMap(pending)
        pending.clear()
        val now = System.currentTimeMillis()
        scope.launch {
            var added = 0
            snapshot.forEach { (pkg, p) ->
                brainPalRepository.recordReels(pkg, p.count, p.firstMs, now)
                added += p.count
            }
            if (added > 0) {
                val total = brainPalRepository.getTodayScrollCount().first()
                WidgetStore.setReelsToday(this@FocusAccessibilityService, total)
                ClatWidgets.updateAll(this@FocusAccessibilityService)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        flushPending()
        isRunning = false
        if (::overlay.isInitialized) overlay.hide()
        super.onDestroy()
        scope.cancel()
    }
}
