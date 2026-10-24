package com.focusguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.focusguard.app.data.prefs.UserPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FocusGuardApp : Application() {

    @Inject lateinit var prefs: UserPreferences

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // A fresh process means no focus session is really running any more.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            prefs.setFocusActive(false)
            prefs.setActiveSessionId(-1L)
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITOR, "App Monitor", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Background screen-time monitoring" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Screen Time Alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Alerts when you reach your daily reel limit" }
        )
    }

    companion object {
        const val CHANNEL_MONITOR = "app_monitor"
        const val CHANNEL_ALERTS  = "screen_time_alerts"
    }
}
