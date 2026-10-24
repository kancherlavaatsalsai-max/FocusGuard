package com.focusguard.app.data.db.dao

import androidx.room.*
import com.focusguard.app.data.db.entity.ScrollBattleEntity
import com.focusguard.app.data.db.entity.ScrollSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrollSessionDao {

    @Query("SELECT * FROM scroll_sessions WHERE dateStr = :date ORDER BY startTimeMs DESC")
    fun getSessionsForDate(date: String): Flow<List<ScrollSessionEntity>>

    @Query("SELECT SUM(estimatedScrolls) FROM scroll_sessions WHERE dateStr = :date")
    fun getTotalScrollsForDate(date: String): Flow<Int?>

    @Query("""
        SELECT SUM(estimatedScrolls) FROM scroll_sessions 
        WHERE dateStr = :date AND packageName = :pkg
    """)
    fun getScrollsForDateAndPkg(date: String, pkg: String): Flow<Int?>

    @Query("SELECT * FROM scroll_sessions WHERE dateStr >= :from AND dateStr <= :to ORDER BY startTimeMs DESC")
    fun getSessionsInRange(from: String, to: String): Flow<List<ScrollSessionEntity>>

    @Query("SELECT SUM(estimatedScrolls) FROM scroll_sessions WHERE dateStr >= :from AND dateStr <= :to")
    fun getTotalScrollsInRange(from: String, to: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScrollSessionEntity): Long

    @Query("DELETE FROM scroll_sessions WHERE dateStr < :olderThan")
    suspend fun deleteOldSessions(olderThan: String)
}

@Dao
interface ScrollBattleDao {

    @Query("SELECT * FROM scroll_battles ORDER BY id DESC")
    fun getAllBattles(): Flow<List<ScrollBattleEntity>>

    @Query("SELECT * FROM scroll_battles WHERE isActive = 1 ORDER BY id DESC")
    fun getActiveBattles(): Flow<List<ScrollBattleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattle(battle: ScrollBattleEntity): Long

    @Query("UPDATE scroll_battles SET myScrollCount = :count WHERE id = :id")
    suspend fun updateMyCount(id: Long, count: Int)

    @Query("UPDATE scroll_battles SET opponentScrollCount = :count WHERE id = :id")
    suspend fun updateOpponentCount(id: Long, count: Int)

    @Query("UPDATE scroll_battles SET isActive = 0 WHERE id = :id")
    suspend fun endBattle(id: Long)

    @Delete
    suspend fun deleteBattle(battle: ScrollBattleEntity)
}
