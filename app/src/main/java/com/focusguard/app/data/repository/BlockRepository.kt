package com.focusguard.app.data.repository

import com.focusguard.app.data.db.dao.BlockedAppDao
import com.focusguard.app.data.db.entity.BlockedAppEntity
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.domain.model.BlockedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepository @Inject constructor(
    private val dao: BlockedAppDao,
    private val prefs: UserPreferences
) {
    fun getAllBlockedApps(): Flow<List<BlockedApp>> =
        dao.getAllBlockedApps().map { it.map(BlockedAppEntity::toDomain) }

    suspend fun isAppBlocked(packageName: String): Boolean =
        dao.isAppBlocked(packageName)

    /**
     * The real decision: should this app be blocked RIGHT NOW?
     *  - bedtime window  -> always block
     *  - schedule on     -> block inside the schedule
     *  - otherwise       -> block only during focus sessions (default) or always
     */
    suspend fun shouldBlock(packageName: String): Boolean {
        val app = dao.getBlockedApp(packageName) ?: return false
        if (!app.isBlocked) return false

        val now = LocalTime.now()

        if (prefs.bedtimeMode.first()) {
            val start = prefs.bedtimeHour.first() % 24
            val end = prefs.morningLockHour.first() % 24
            val h = now.hour
            val inBedtime = if (start <= end) h in start until end else (h >= start || h < end)
            if (inBedtime) return true
        }

        if (app.scheduleEnabled) {
            val nowMin = now.hour * 60 + now.minute
            val s = app.scheduleStartHour * 60 + app.scheduleStartMinute
            val e = app.scheduleEndHour * 60 + app.scheduleEndMinute
            val inside = if (s <= e) nowMin in s until e else (nowMin >= s || nowMin < e)
            if (inside) return true
        }

        return if (prefs.blockOnlyDuringFocus.first()) prefs.focusActive.first() else true
    }

    suspend fun addBlockedApp(app: BlockedApp) = dao.insertOrUpdate(app.toEntity())
    suspend fun updateBlockedApp(app: BlockedApp) = dao.insertOrUpdate(app.toEntity())
    suspend fun setBlocked(packageName: String, blocked: Boolean) = dao.setBlocked(packageName, blocked)
    suspend fun removeBlockedApp(packageName: String) = dao.deleteByPackage(packageName)

    private fun BlockedAppEntity.toDomain() = BlockedApp(
        packageName = packageName, appName = appName, isBlocked = isBlocked,
        scheduleEnabled = scheduleEnabled,
        scheduleStartHour = scheduleStartHour, scheduleStartMinute = scheduleStartMinute,
        scheduleEndHour = scheduleEndHour, scheduleEndMinute = scheduleEndMinute,
        blockOnlyDuringFocus = blockOnlyDuringFocus
    )

    private fun BlockedApp.toEntity() = BlockedAppEntity(
        packageName = packageName, appName = appName, isBlocked = isBlocked,
        scheduleEnabled = scheduleEnabled,
        scheduleStartHour = scheduleStartHour, scheduleStartMinute = scheduleStartMinute,
        scheduleEndHour = scheduleEndHour, scheduleEndMinute = scheduleEndMinute,
        blockOnlyDuringFocus = blockOnlyDuringFocus
    )
}
