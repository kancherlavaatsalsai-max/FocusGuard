package com.focusguard.app.data.repository

import com.focusguard.app.data.db.dao.ScrollBattleDao
import com.focusguard.app.data.db.dao.ScrollSessionDao
import com.focusguard.app.data.db.entity.ScrollBattleEntity
import com.focusguard.app.data.db.entity.ScrollSessionEntity
import com.focusguard.app.domain.model.ShortVideoPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@Singleton
class BrainPalRepository @Inject constructor(
    private val scrollSessionDao: ScrollSessionDao,
    private val scrollBattleDao: ScrollBattleDao
) {
    // ── Reel / Short counting ─────────────────────────────────────────
    fun getTodayScrollCount(): Flow<Int> =
        scrollSessionDao.getTotalScrollsForDate(LocalDate.now().format(fmt)).map { it ?: 0 }

    fun getTodaySessions(): Flow<List<ScrollSessionEntity>> =
        scrollSessionDao.getSessionsForDate(LocalDate.now().format(fmt))

    /** Last 7 days, oldest first: (label, count) */
    fun getWeekScrollCounts(): Flow<List<Pair<String, Int>>> {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(6)
        return scrollSessionDao.getSessionsInRange(weekAgo.format(fmt), today.format(fmt))
            .map { sessions ->
                (6 downTo 0).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    val label = if (daysAgo == 0) "Today"
                    else date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                    val count = sessions
                        .filter { it.dateStr == date.format(fmt) }
                        .sumOf { it.estimatedScrolls }
                    label to count
                }
            }
    }

    /** Called by the accessibility service with the reels it counted since the last flush. */
    suspend fun recordReels(packageName: String, reels: Int, startMs: Long, endMs: Long) {
        if (reels <= 0) return
        val platform = ShortVideoPlatform.values().find { it.packageName == packageName }
        scrollSessionDao.insertSession(
            ScrollSessionEntity(
                packageName      = packageName,
                platformName     = platform?.displayName ?: packageName,
                dateStr          = LocalDate.now().format(fmt),
                startTimeMs      = startMs,
                endTimeMs        = endMs,
                estimatedScrolls = reels
            )
        )
    }

    // ── Battles ───────────────────────────────────────────────────────
    fun getActiveBattles(): Flow<List<ScrollBattleEntity>> = scrollBattleDao.getActiveBattles()

    suspend fun createBattle(opponentName: String): Long =
        scrollBattleDao.insertBattle(
            ScrollBattleEntity(opponentName = opponentName, dateStr = LocalDate.now().format(fmt))
        )

    suspend fun updateMyCount(battleId: Long, count: Int) = scrollBattleDao.updateMyCount(battleId, count)
    suspend fun updateOpponentCount(battleId: Long, count: Int) = scrollBattleDao.updateOpponentCount(battleId, count)
    suspend fun endBattle(battleId: Long) = scrollBattleDao.endBattle(battleId)
}
