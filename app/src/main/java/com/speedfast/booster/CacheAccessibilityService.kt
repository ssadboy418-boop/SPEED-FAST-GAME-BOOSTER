package com.speedfast.booster

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock

class CacheAccessibilityService : AccessibilityService() {

    private var lastClickTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return

        // Only work inside Android Settings
        if (!pkg.contains("settings", ignoreCase = true)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < 1500) return

        val root = rootInActiveWindow ?: return

        if (clickClearCache(root)) {
            lastClickTime = now
        }
    }

    private fun clickClearCache(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()
            ?.trim()
            ?.lowercase()

        // Never click Clear Data or any other button
        if (text == "clear cache") {
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
            if (clickClearCache(child)) return true
        }

        return false
    }

    override fun onInterrupt() {
    }
}
