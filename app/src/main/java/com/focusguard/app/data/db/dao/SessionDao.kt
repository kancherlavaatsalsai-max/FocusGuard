package com.focusguard.app.data.db.dao

import androidx.room.*
import com.focusguard.app.data.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions ORDER BY startTimeMs DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE state = 'RUNNING' OR state = 'PAUSED' LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT SUM(durationMinutes) FROM sessions WHERE state = 'COMPLETED' AND startTimeMs >= :sinceMs")
    fun getFocusMinutesSince(sinceMs: Long): Flow<Long?>

    @Query("""
        SELECT * FROM sessions 
        WHERE startTimeMs >= :fromMs AND startTimeMs <= :toMs
        ORDER BY startTimeMs DESC
    """)
    fun getSessionsInRange(fromMs: Long, toMs: Long): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE state = 'COMPLETED'")
    fun getTotalCompletedSessions(): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM sessions WHERE state = 'COMPLETED'")
    fun getTotalFocusMinutes(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE sessions SET state = :state WHERE id = :id")
    suspend fun updateSessionState(id: Long, state: String)

    @Query("UPDATE sessions SET endTimeMs = :endMs, state = :state, focusCoinsEarned = :coins WHERE id = :id")
    suspend fun completeSession(id: Long, endMs: Long, state: String, coins: Int)

    @Delete
    suspend fun deleteSession(session: SessionEntity)
}
