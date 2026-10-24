package com.focusguard.app.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.focusguard.app.data.db.dao.UsageRecordDao
import com.focusguard.app.data.db.entity.UsageRecordEntity
import com.focusguard.app.domain.model.AppUsageStat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: UsageRecordDao
) {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val pm = context.packageManager

    fun getUsageForToday(): Flow<List<AppUsageStat>> =
        dao.getUsageForDate(LocalDate.now().format(fmt)).map { it.toStats() }

    fun getTodayScreenTimeMs(): Flow<Long> =
        dao.getUsageForDate(LocalDate.now().format(fmt)).map { list -> list.sumOf { it.totalTimeMs } }

    /**
     * Reads real foreground time from UsageStatsManager events (same source as
     * Digital Wellbeing) and stores ONE row per app per day.
     */
    @Suppress("DEPRECATION")
    suspend fun syncTodayUsage() = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val startMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMs = System.currentTimeMillis()

        val events = usm.queryEvents(startMs, endMs) ?: return@withContext
        val ev = UsageEvents.Event()
        val fgStart = HashMap<String, Long>()
        val totals = HashMap<String, Long>()
        val launches = HashMap<String, Int>()

        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val pkg = ev.packageName ?: continue
            when (ev.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (!fgStart.containsKey(pkg)) {
                        fgStart[pkg] = ev.timeStamp
                        launches[pkg] = (launches[pkg] ?: 0) + 1
                    }
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val s = fgStart.remove(pkg)
                    if (s != null && ev.timeStamp > s) totals[pkg] = (totals[pkg] ?: 0L) + (ev.timeStamp - s)
                }
            }
        }
        fgStart.forEach { (pkg, s) -> totals[pkg] = (totals[pkg] ?: 0L) + (endMs - s) }

        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName
        val skip = setOf(context.packageName, home, "com.android.systemui")

        totals.filter { it.value >= 1_000L && it.key !in skip }.forEach { (pkg, ms) ->
            val appName = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) { pkg }

            dao.insertOrUpdate(
                UsageRecordEntity(
                    packageName  = pkg,
                    appName      = appName,
                    dateStr      = today.format(fmt),
                    totalTimeMs  = ms,
                    sessionCount = launches[pkg] ?: 0,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            )
        }
    }

    private fun List<UsageRecordEntity>.toStats(): List<AppUsageStat> = map {
        AppUsageStat(
            packageName = it.packageName, appName = it.appName,
            totalTimeMs = it.totalTimeMs, sessionCount = it.sessionCount,
            lastUsed = it.lastUpdatedMs, estimatedReelCount = 0
        )
    }
}
