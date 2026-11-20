package com.speedfast.booster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val enabled = context.getSharedPreferences("auto_clean", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)

        if (enabled) {
            val serviceIntent = Intent(context, AutoCleanService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: Exception) {
                // Android may restrict background service startup.
            }
        }
    }
}
