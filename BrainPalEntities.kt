package com.focusguard.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Records a single continuous scrolling session in a short-video app */
@Entity(tableName = "scroll_sessions")
data class ScrollSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val platformName: String,
    val dateStr: String,         // yyyy-MM-dd
    val startTimeMs: Long,
    val endTimeMs: Long,
    val estimatedScrolls: Int,
    val durationMs: Long = endTimeMs - startTimeMs
)

/** A scroll battle challenge with a friend/study partner */
@Entity(tableName = "scroll_battles")
data class ScrollBattleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val myName: String = "Me",
    val opponentName: String,
    val myScrollCount: Int = 0,
    val opponentScrollCount: Int = 0,
    val dateStr: String,
    val isActive: Boolean = true
)
