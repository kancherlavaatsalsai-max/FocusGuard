package com.focusguard.app.data.db.dao

import androidx.room.*
import com.focusguard.app.data.db.entity.UsageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {

    @Query("SELECT * FROM usage_records WHERE dateStr = :date ORDER BY totalTimeMs DESC")
    fun getUsageForDate(date: String): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_records ORDER BY totalTimeMs DESC LIMIT 10")
    fun getTopApps(): Flow<List<UsageRecordEntity>>

    @Query("""
        SELECT 0 AS id, packageName, appName, SUM(totalTimeMs) as totalTimeMs, 
               SUM(sessionCount) as sessionCount, MAX(lastUpdatedMs) as lastUpdatedMs,
               SUM(estimatedReelCount) as estimatedReelCount,
               dateStr
        FROM usage_records 
        WHERE dateStr >= :fromDate AND dateStr <= :toDate 
        GROUP BY packageName
        ORDER BY totalTimeMs DESC
    """)
    fun getUsageInRange(fromDate: String, toDate: String): Flow<List<UsageRecordEntity>>

    @Query("""
        SELECT SUM(estimatedReelCount) FROM usage_records 
        WHERE dateStr = :date AND (packageName = 'com.instagram.android' OR packageName = 'com.google.android.youtube')
    """)
    fun getTotalReelsForDate(date: String): Flow<Int?>

    @Query("SELECT * FROM usage_records WHERE dateStr = :date AND packageName = :pkg LIMIT 1")
    suspend fun getRecord(date: String, pkg: String): UsageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: UsageRecordEntity)

    @Query("DELETE FROM usage_records WHERE dateStr < :olderThan")
    suspend fun deleteOldRecords(olderThan: String)
}
