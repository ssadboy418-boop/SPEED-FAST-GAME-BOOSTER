package com.speedfast.booster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import java.io.File

class AutoCleanService : Service() {

    @Volatile
    private var running = false

    private var cleanerThread: Thread? = null

    companion object {
        private const val CHANNEL_ID = "speed_fast_auto_clean"
        private const val NOTIFICATION_ID = 410
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, makeNotification())

        if (!running) {
            running = true
            cleanerThread = Thread {
                while (running) {
                    try {
                        val cleaned = cleanDirectory(cacheDir)
                        val external = externalCacheDir
                        if (external != null) {
                            cleanDirectory(external)
                        }

                        getSharedPreferences("auto_clean", MODE_PRIVATE)
                            .edit()
                            .putLong("last_clean", System.currentTimeMillis())
                            .putLong("last_cleaned_bytes", cleaned)
                            .apply()

                        Thread.sleep(10_000)
                    } catch (_: InterruptedException) {
                        break
                    } catch (_: Exception) {
                        try {
                            Thread.sleep(10_000)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }.apply {
                name = "SpeedFastAutoCleaner"
                start()
            }
        }

        return START_STICKY
    }

    private fun cleanDirectory(directory: File): Long {
        var deletedBytes = 0L
        val files = directory.listFiles() ?: return 0L

        for (file in files) {
            try {
                if (file.isDirectory) {
                    deletedBytes += cleanDirectory(file)
                }
                val size = if (file.isFile) file.length() else 0L
                if (file.delete()) deletedBytes += size
            } catch (_: Exception) {
                // Skip files Android does not allow this app to delete.
            }
        }
        return deletedBytes
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Cleaner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows Auto Cleaner background status"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun makeNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("SPEED FAST AUTO CLEANER")
            .setContentText("Running • Checking app cache every 10 seconds")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        cleanerThread?.interrupt()
        getSharedPreferences("auto_clean", MODE_PRIVATE)
            .edit()
            .putBoolean("enabled", false)
            .apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
