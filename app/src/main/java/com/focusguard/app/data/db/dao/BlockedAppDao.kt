package com.focusguard.app.data.db.dao

import androidx.room.*
import com.focusguard.app.data.db.entity.BlockedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE isBlocked = 1")
    fun getActivelyBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getBlockedApp(pkg: String): BlockedAppEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_apps WHERE packageName = :pkg AND isBlocked = 1)")
    suspend fun isAppBlocked(pkg: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: BlockedAppEntity)

    @Query("UPDATE blocked_apps SET isBlocked = :blocked WHERE packageName = :pkg")
    suspend fun setBlocked(pkg: String, blocked: Boolean)

    @Delete
    suspend fun delete(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)
}
