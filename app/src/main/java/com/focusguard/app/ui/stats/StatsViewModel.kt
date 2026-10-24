package com.focusguard.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.data.repository.BrainPalRepository
import com.focusguard.app.data.repository.SessionRepository
import com.focusguard.app.data.repository.UsageRepository
import com.focusguard.app.domain.model.AppUsageStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class StatsUiState(
    val todayUsage: List<AppUsageStat> = emptyList(),
    val reelsByPlatform: List<Pair<String, Int>> = emptyList(),
    val weeklyReels: List<Pair<String, Int>> = emptyList(),
    val reelsToday: Int = 0,
    val reelLimit: Int = 30,
    val totalFocusMinutes: Long = 0L,
    val totalSessions: Int = 0,
    val totalCoins: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    usageRepository: UsageRepository,
    sessionRepository: SessionRepository,
    brainPalRepository: BrainPalRepository,
    userPrefs: UserPreferences
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        usageRepository.getUsageForToday(),
        brainPalRepository.getTodaySessions(),
        brainPalRepository.getWeekScrollCounts(),
        sessionRepository.getTotalFocusMinutes(),
        sessionRepository.getTotalCompletedSessions(),
        userPrefs.totalCoins,
        userPrefs.currentStreak,
        userPrefs.longestStreak,
        userPrefs.dailyReelLimit
    ) { v ->
        @Suppress("UNCHECKED_CAST")
        val sessions = v[1] as List<com.focusguard.app.data.db.entity.ScrollSessionEntity>
        StatsUiState(
            todayUsage        = v[0] as List<AppUsageStat>,
            reelsByPlatform   = sessions.groupBy { it.platformName }
                .map { (name, list) -> name to list.sumOf { it.estimatedScrolls } }
                .sortedByDescending { it.second },
            weeklyReels       = v[2] as List<Pair<String, Int>>,
            reelsToday        = sessions.sumOf { it.estimatedScrolls },
            totalFocusMinutes = v[3] as Long,
            totalSessions     = v[4] as Int,
            totalCoins        = v[5] as Int,
            currentStreak     = v[6] as Int,
            longestStreak     = v[7] as Int,
            reelLimit         = v[8] as Int
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
