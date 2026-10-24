package com.focusguard.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.data.repository.BrainPalRepository
import com.focusguard.app.data.repository.SessionRepository
import com.focusguard.app.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val todayFocusMinutes: Long = 0L,
    val reelsWatchedToday: Int = 0,
    val reelLimit: Int = 30,
    val screenTimeMs: Long = 0L,
    val streak: Int = 0,
    val coins: Int = 0,
    val totalSessions: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    sessionRepository: SessionRepository,
    brainPalRepository: BrainPalRepository,
    userPrefs: UserPreferences
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.getTodayFocusMinutes(),
        brainPalRepository.getTodayScrollCount(),
        userPrefs.dailyReelLimit,
        usageRepository.getTodayScreenTimeMs(),
        userPrefs.currentStreak,
        userPrefs.totalCoins,
        sessionRepository.getTotalCompletedSessions()
    ) { v ->
        HomeUiState(
            todayFocusMinutes = v[0] as Long,
            reelsWatchedToday = v[1] as Int,
            reelLimit         = v[2] as Int,
            screenTimeMs      = v[3] as Long,
            streak            = v[4] as Int,
            coins             = v[5] as Int,
            totalSessions     = v[6] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshUsage() {
        viewModelScope.launch { try { usageRepository.syncTodayUsage() } catch (_: Exception) {} }
    }
}
