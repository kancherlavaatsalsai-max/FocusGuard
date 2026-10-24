package com.focusguard.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.focusguard.app.ui.theme.FocusGuardTheme
import kotlinx.coroutines.delay

/**
 * Draws the full-screen "Stay focused" screen on top of a blocked app.
 * Uses TYPE_ACCESSIBILITY_OVERLAY, so it needs no extra permission and
 * cannot be killed by background-service limits.
 */
class BlockOverlayManager(private val service: AccessibilityService) {

    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    val isShowing: Boolean get() = view != null

    fun show(
        appName: String,
        daysLeft: Int,
        strictMode: Boolean,
        onGoHome: () -> Unit,
        onTempUnlock: () -> Unit
    ) {
        hide()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val o = OverlayOwner().also { it.start() }
        val v = ComposeView(service).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setContent {
                FocusGuardTheme(darkTheme = true) {
                    BlockScreen(appName, daysLeft, strictMode, onGoHome, onTempUnlock)
                }
            }
        }
        owner = o
        view = v
        try {
            wm.addView(v, params)
        } catch (_: Exception) {
            view = null
            o.stop()
            owner = null
        }
    }

    fun hide() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
        owner?.stop()
        owner = null
    }
}

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun start() {
        controller.performAttach()
        controller.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

private val QUOTES = listOf(
    "Every hour you study now is an hour of regret saved later.",
    "Discipline beats motivation. Put the phone down.",
    "Your future self is watching. Make them proud.",
    "One more reel won't help you crack CLAT. One more page will.",
    "Small steps every day beat big plans someday.",
    "You're not tired, you're distracted. Get back to it."
)

@Composable
private fun BlockScreen(
    appName: String,
    daysLeft: Int,
    strictMode: Boolean,
    onGoHome: () -> Unit,
    onTempUnlock: () -> Unit
) {
    val quote = remember { QUOTES.random() }
    var unlockStarted by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(20) }

    LaunchedEffect(unlockStarted) {
        if (unlockStarted) {
            while (countdown > 0) { delay(1000); countdown-- }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1020), Color(0xFF1A1040)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(92.dp).background(Color(0xFF263159), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFF9FA8DA), modifier = Modifier.size(44.dp))
            }

            Text("Stay Focused 🎯", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

            Text(
                "$appName is blocked right now",
                fontSize = 16.sp, color = Color(0xFFB0B8C8), textAlign = TextAlign.Center
            )

            if (daysLeft > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "⏳ $daysLeft days to CLAT",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp
                    )
                }
            }

            Text(
                "\"$quote\"",
                fontSize = 14.sp, color = Color(0xFF8B949E),
                textAlign = TextAlign.Center, lineHeight = 21.sp
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C6BC0)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Back to studying", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

            if (strictMode) {
                Text(
                    "🔒 Strict mode is ON – no unlock",
                    color = Color(0xFF6E7681), fontSize = 12.sp, textAlign = TextAlign.Center
                )
            } else if (!unlockStarted) {
                TextButton(onClick = { unlockStarted = true }) {
                    Text("I really need it (5 min)", color = Color(0xFF6E7681), fontSize = 13.sp)
                }
            } else if (countdown > 0) {
                Text("Think about it… $countdown s", color = Color(0xFF6E7681), fontSize = 13.sp)
                LinearProgressIndicator(
                    progress = { (20 - countdown) / 20f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(0xFFEF5350)
                )
            } else {
                OutlinedButton(
                    onClick = onTempUnlock,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFEF5350)),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Unlock for 5 minutes", color = Color(0xFFEF5350)) }
            }
        }
    }
}
