package com.focusguard.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goal: String = "",
    val type: String = "POMODORO",
    val durationMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val startTimeMs: Long? = null,
    val endTimeMs: Long? = null,
    val state: String = "IDLE",
    val focusCoinsEarned: Int = 0,
    val completedPomodoros: Int = 0
)

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val scheduleStartHour: Int = 9,
    val scheduleStartMinute: Int = 0,
    val scheduleEndHour: Int = 22,
    val scheduleEndMinute: Int = 0,
    val blockOnlyDuringFocus: Boolean = false
)

// One row per app per day (unique index) so repeated syncs REPLACE instead of duplicating.
@Entity(
    tableName = "usage_records",
    indices = [Index(value = ["dateStr", "packageName"], unique = true)]
)
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val dateStr: String,        // yyyy-MM-dd
    val totalTimeMs: Long,
    val sessionCount: Int,
    val estimatedReelCount: Int = 0,
    val lastUpdatedMs: Long = System.currentTimeMillis()
)
