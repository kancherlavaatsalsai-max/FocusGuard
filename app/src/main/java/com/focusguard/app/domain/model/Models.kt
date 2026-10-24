package com.focusguard.app.domain.model

import java.time.LocalDateTime

enum class SessionType { POMODORO, DEEP_WORK, CUSTOM }
enum class SessionState { IDLE, RUNNING, PAUSED, COMPLETED, ABANDONED }

data class FocusSession(
    val id: Long = 0,
    val goal: String = "",
    val type: SessionType = SessionType.POMODORO,
    val durationMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val state: SessionState = SessionState.IDLE,
    val focusCoinsEarned: Int = 0,
    val completedPomodoros: Int = 0
)

data class BlockedApp(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 9,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 22,
    val scheduleEndMinute: Int = 0,
    val blockOnlyDuringFocus: Boolean = false
)

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val sessionCount: Int,
    val lastUsed: Long,
    /** Estimated short-form video count (Reels/Shorts) */
    val estimatedReelCount: Int = 0
)

data class DailyStats(
    val date: String,           // yyyy-MM-dd
    val totalScreenTimeMs: Long,
    val focusTimeMs: Long,
    val distractedTimeMs: Long,
    val reelsWatched: Int,
    val focusSessions: Int,
    val focusCoins: Int,
    val streakDay: Int
)

data class UserProfile(
    val totalFocusCoins: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalFocusMinutes: Long = 0,
    val totalSessionsCompleted: Int = 0,
    val dailyReelLimit: Int = 30,
    val dailyScreenTimeGoalMinutes: Int = 120,
    val strictModeEnabled: Boolean = false,
    val bedtimeModeEnabled: Boolean = false,
    val bedtimeHour: Int = 22,
    val morningLockHour: Int = 9
)
