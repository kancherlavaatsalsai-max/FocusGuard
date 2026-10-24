package com.focusguard.app.data.repository

import com.focusguard.app.data.db.dao.SessionDao
import com.focusguard.app.data.db.entity.SessionEntity
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.domain.model.FocusSession
import com.focusguard.app.domain.model.SessionState
import com.focusguard.app.domain.model.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao,
    private val prefs: UserPreferences
) {
    fun getTotalFocusMinutes(): Flow<Long> = dao.getTotalFocusMinutes().map { it ?: 0L }

    fun getTodayFocusMinutes(): Flow<Long> {
        val startMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.getFocusMinutesSince(startMs).map { it ?: 0L }
    }

    fun getTotalCompletedSessions(): Flow<Int> = dao.getTotalCompletedSessions()

    suspend fun startSession(session: FocusSession): Long {
        val entity = session.copy(startTime = LocalDateTime.now(), state = SessionState.RUNNING).toEntity()
        val id = dao.insertSession(entity)
        prefs.setActiveSessionId(id)
        return id
    }

    suspend fun pauseSession(id: Long) = dao.updateSessionState(id, SessionState.PAUSED.name)
    suspend fun resumeSession(id: Long) = dao.updateSessionState(id, SessionState.RUNNING.name)

    /** Returns the coins earned. Also updates the daily streak. */
    suspend fun completeSession(id: Long, durationMinutes: Int): Int {
        val coins = calculateCoins(durationMinutes)
        dao.completeSession(id, System.currentTimeMillis(), SessionState.COMPLETED.name, coins)
        prefs.addCoins(coins)
        prefs.addFocusMinutes(durationMinutes.toLong())
        prefs.setActiveSessionId(-1L)

        val today = LocalDate.now()
        val last = prefs.lastStreakDate.first()
        val cur = prefs.currentStreak.first()
        val newStreak = when (last) {
            today.toString()               -> cur.coerceAtLeast(1)
            today.minusDays(1).toString()  -> cur + 1
            else                           -> 1
        }
        prefs.setStreak(newStreak, prefs.longestStreak.first())
        prefs.setLastStreakDate(today.toString())
        return coins
    }

    suspend fun abandonSession(id: Long) {
        dao.updateSessionState(id, SessionState.ABANDONED.name)
        prefs.setActiveSessionId(-1L)
    }

    private fun calculateCoins(minutes: Int): Int = when {
        minutes >= 120 -> 50
        minutes >= 60  -> 30
        minutes >= 45  -> 20
        minutes >= 25  -> 10
        else           -> 5
    }
}

private fun FocusSession.toEntity() = SessionEntity(
    id = id, goal = goal, type = type.name,
    durationMinutes = durationMinutes, breakMinutes = breakMinutes,
    startTimeMs = startTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    endTimeMs = endTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
    state = state.name, focusCoinsEarned = focusCoinsEarned, completedPomodoros = completedPomodoros
)
