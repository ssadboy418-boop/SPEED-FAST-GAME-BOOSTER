package com.speedfast.booster

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class CacheAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var watchdogIndex = -1

    private val watchdog = Runnable {
        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)
        val index = prefs.getInt("index", -1)

        if (prefs.getBoolean("running", false) && index == watchdogIndex) {
            advanceToNextApp(index)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)
        if (!prefs.getBoolean("running", false)) return

        val pkg = event.packageName?.toString() ?: return

        // Only automate Android Settings screens.
        if (!pkg.contains("settings", ignoreCase = true)) return

        val index = prefs.getInt("index", -1)
        if (index < 0) return

        startWatchdog(index)

        val root = rootInActiveWindow ?: return
        val step = prefs.getInt("step", 0)

        // Some phones show Storage directly; others show Storage & cache.
        if (step == 0) {
            if (clickLabel(root, "clear cache")) {
                cacheClicked(index)
                return
            }

            if (
                clickAnyLabel(
                    root,
                    setOf("storage & cache", "storage and cache", "storage")
                )
            ) {
                prefs.edit().putInt("step", 1).apply()
                return
            }
        }

        if (step == 1 && clickLabel(root, "clear cache")) {
            cacheClicked(index)
        }
    }

    private fun startWatchdog(index: Int) {
        if (watchdogIndex == index) return

        handler.removeCallbacks(watchdog)
        watchdogIndex = index

        // Skip an app if its Settings screen does not respond.
        handler.postDelayed(watchdog, 15000)
    }

    private fun cacheClicked(index: Int) {
        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)
        if (prefs.getInt("index", -1) != index) return

        prefs.edit().putInt("step", 2).apply()

        handler.removeCallbacks(watchdog)
        handler.postDelayed({
            advanceToNextApp(index)
        }, 1800)
    }

    private fun advanceToNextApp(oldIndex: Int) {
        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)

        if (!prefs.getBoolean("running", false)) return
        if (prefs.getInt("index", -1) != oldIndex) return

        handler.removeCallbacks(watchdog)
        watchdogIndex = -1

        val queue = prefs.getString("queue", "")
            ?.split("\\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val nextIndex = oldIndex + 1

        if (nextIndex >= queue.size) {
            finishBatch(queue.size)
            return
        }

        prefs.edit()
            .putInt("index", nextIndex)
            .putInt("step", 0)
            .apply()

        openAppInfo(queue[nextIndex])
        startWatchdog(nextIndex)
    }

    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(intent)
        } catch (_: Exception) {
            val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)
            val index = prefs.getInt("index", -1)
            if (index >= 0) advanceToNextApp(index)
        }
    }

    private fun finishBatch(processedCount: Int) {
        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)

        val initialFree = prefs.getLong("initial_free", 0L)
        val currentFree = android.os.StatFs(
            android.os.Environment.getDataDirectory().path
        ).availableBytes

        // This is an estimate of total storage freed, not exact cache size.
        val estimatedFreed = (currentFree - initialFree).coerceAtLeast(0L)

        prefs.edit()
            .putBoolean("running", false)
            .putBoolean("result_pending", true)
            .putLong("cleaned_bytes", estimatedFreed)
            .putInt("processed_count", processedCount)
            .apply()

        handler.removeCallbacks(watchdog)
        watchdogIndex = -1

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun clickLabel(
        node: AccessibilityNodeInfo,
        wanted: String
    ): Boolean {
        val text = node.text?.toString()?.trim()?.lowercase()
        val description = node.contentDescription?.toString()
            ?.trim()?.lowercase()

        if (text == wanted || description == wanted) {
            var target: AccessibilityNodeInfo? = node

            while (target != null) {
                if (target.isClickable) {
                    return target.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK
                    )
                }
                target = target.parent
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (clickLabel(child, wanted)) return true
        }

        return false
    }

    private fun clickAnyLabel(
        node: AccessibilityNodeInfo,
        labels: Set<String>
    ): Boolean {
        for (label in labels) {
            if (clickLabel(node, label)) return true
        }
        return false
    }

    override fun onInterrupt() {
        handler.removeCallbacks(watchdog)
    }
}
