package com.focusguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusguard.app.service.UsageMonitorService

/**
 * Restores the UsageMonitorService after device reboot so
 * screen time tracking and scheduled blocking continue working.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(
                Intent(context, UsageMonitorService::class.java)
            )
        }
    }
}
