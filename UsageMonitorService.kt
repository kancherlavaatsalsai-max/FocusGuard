package com.focusguard.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.focusguard.app.FocusGuardApp
import com.focusguard.app.MainActivity
import com.focusguard.app.R
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.data.repository.BrainPalRepository
import com.focusguard.app.data.repository.SessionRepository
import com.focusguard.app.data.repository.UsageRepository
import com.focusguard.app.util.WidgetStore
import com.focusguard.app.widget.ClatWidgets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Foreground service. Every 5 minutes it syncs screen time, refreshes the
 * widgets, and fires an alert when your daily Reel limit is reached.
 */
@AndroidEntryPoint
class UsageMonitorService : Service() {

    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var brainPalRepository: BrainPalRepository
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var userPreferences: UserPreferences

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var alertDay: LocalDate? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val SYNC_INTERVAL_MS = 5 * 60 * 1000L
        @Volatile var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch {
            while (isActive) {
                try {
                    usageRepository.syncTodayUsage()
                    refreshWidgetData()
                    checkReelLimit()
                } catch (_: Exception) { /* never crash the monitor */ }
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    private suspend fun refreshWidgetData() {
        WidgetStore.setReelsToday(this, brainPalRepository.getTodayScrollCount().first())
        WidgetStore.setFocusMinutesToday(this, sessionRepository.getTodayFocusMinutes().first())
        ClatWidgets.updateAll(this)
    }

    private suspend fun checkReelLimit() {
        val total = brainPalRepository.getTodayScrollCount().first()
        val limit = userPreferences.dailyReelLimit.first()
        val today = LocalDate.now()
        if (total >= limit && alertDay != today) {
            alertDay = today
            val pi = PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            val n = Notification.Builder(this, FocusGuardApp.CHANNEL_ALERTS)
                .setContentTitle("📱 Reel limit reached")
                .setContentText("You've watched $total Reels/Shorts today (limit $limit). Back to studying!")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            getSystemService(NotificationManager::class.java).notify(2001, n)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, FocusGuardApp.CHANNEL_MONITOR)
            .setContentTitle("FocusGuard")
            .setContentText("Keeping you focused")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }
}
