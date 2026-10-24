package com.focusguard.app.ui.blocker

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.data.repository.BlockRepository
import com.focusguard.app.domain.model.BlockedApp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstalledApp(val packageName: String, val appName: String)

data class BlockerUiState(
    val installedApps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val blockOnlyDuringFocus: Boolean = true,
    val isLoading: Boolean = true
)

/** Apps most people want to block while studying. */
private val DISTRACTING = setOf(
    "com.instagram.android", "com.google.android.youtube", "com.facebook.katana",
    "com.snapchat.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.twitter.android", "com.reddit.frontpage", "com.netflix.mediaclient",
    "com.whatsapp", "org.telegram.messenger", "com.discord", "in.mohalla.sharechat",
    "com.pinterest", "tv.twitch.android.app"
)

@HiltViewModel
class BlockerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockRepository: BlockRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _search = MutableStateFlow("")
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loaded = MutableStateFlow(false)

    val uiState: StateFlow<BlockerUiState> = combine(
        _apps, blockRepository.getAllBlockedApps(), _search, prefs.blockOnlyDuringFocus, _loaded
    ) { apps, blocked, query, onlyFocus, loaded ->
        val filtered = if (query.isBlank()) apps else apps.filter { it.appName.contains(query, true) }
        BlockerUiState(
            installedApps = filtered,
            blockedPackages = blocked.filter { it.isBlocked }.map { it.packageName }.toSet(),
            searchQuery = query,
            blockOnlyDuringFocus = onlyFocus,
            isLoading = !loaded
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockerUiState())

    init { loadInstalledApps() }

    fun onSearchQueryChange(q: String) { _search.value = q }

    fun setBlockOnlyDuringFocus(v: Boolean) = viewModelScope.launch { prefs.setBlockOnlyDuringFocus(v) }

    fun toggleBlock(app: InstalledApp) = viewModelScope.launch {
        if (app.packageName in uiState.value.blockedPackages) {
            blockRepository.setBlocked(app.packageName, false)
        } else {
            blockRepository.addBlockedApp(BlockedApp(packageName = app.packageName, appName = app.appName))
        }
    }

    /** One tap: block every common distraction app that is installed. */
    fun blockDistractions() = viewModelScope.launch {
        _apps.value.filter { it.packageName in DISTRACTING }.forEach {
            blockRepository.addBlockedApp(BlockedApp(packageName = it.packageName, appName = it.appName))
        }
    }

    private fun loadInstalledApps() = viewModelScope.launch {
        val apps = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcher, 0)
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .filter { it.first != context.packageName }
                .distinctBy { it.first }
                .map { InstalledApp(it.first, it.second) }
                .sortedBy { it.appName.lowercase() }
        }
        _apps.value = apps
        _loaded.value = true
    }
}
