package com.speedfast.booster

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var ramText: TextView
    private lateinit var storageText: TextView
    private lateinit var statusText: TextView
    private lateinit var autoCleanButton: Button

    private val bg = Color.rgb(8, 13, 11)
    private val panel = Color.rgb(19, 28, 24)
    private val neon = Color.rgb(57, 255, 20)
    private val muted = Color.rgb(160, 175, 165)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildDashboard()
        refreshStats()
    }

    private fun buildDashboard() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        val brand = TextView(this).apply {
            text = "⚡ SPEED FAST"
            textSize = 27f
            setTextColor(neon)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(brand, match())

        val subtitle = TextView(this).apply {
            text = "GAME BOOSTER • NO ROOT"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, match())
        root.addView(space(25))

        root.addView(sectionTitle("SYSTEM PERFORMANCE"))
        root.addView(space(10))

        val ramCard = card()
        ramCard.addView(label("RAM MEMORY"))
        ramText = value("Loading...")
        ramCard.addView(ramText)
        root.addView(ramCard, match())
        root.addView(space(12))

        val storageCard = card()
        storageCard.addView(label("INTERNAL STORAGE"))
        storageText = value("Loading...")
        storageCard.addView(storageText)
        root.addView(storageCard, match())
        root.addView(space(22))

        root.addView(sectionTitle("QUICK ACTIONS"))
        root.addView(space(12))

        val boost = actionButton("⚡ BOOST NOW")
        boost.setOnClickListener {
            refreshStats()
            Toast.makeText(
                this,
                "Performance refreshed!",
                Toast.LENGTH_SHORT
            ).show()
        }
        root.addView(boost, match())
        root.addView(space(10))

        val clean = actionButton("🧹 CLEAN APP CACHE")
        clean.setOnClickListener {
            val before = cacheSize(cacheDir)
            clearDirectory(cacheDir)
            val cleaned = before - cacheSize(cacheDir)
            refreshStats()
            Toast.makeText(
                this,
                "Cleaned: ${formatBytes(cleaned)}",
                Toast.LENGTH_LONG
            ).show()
        }
        root.addView(clean, match())
        root.addView(space(10))

        autoCleanButton = actionButton("⚡ START AUTO CLEANER")
        autoCleanButton.setOnClickListener {
            val prefs = getSharedPreferences("auto_clean", MODE_PRIVATE)
            if (prefs.getBoolean("enabled", false)) {
                stopService(Intent(this, AutoCleanService::class.java))
                prefs.edit().putBoolean("enabled", false).apply()
                updateAutoCleanButton()
                Toast.makeText(this, "Auto Cleaner Stopped", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putBoolean("enabled", true).apply()
                try {
                    val serviceIntent = Intent(this, AutoCleanService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    updateAutoCleanButton()
                    Toast.makeText(this, "Auto Cleaner Started", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    prefs.edit().putBoolean("enabled", false).apply()
                    updateAutoCleanButton()
                    Toast.makeText(this, "Could not start Cleaner: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(autoCleanButton, match())
        root.addView(space(10))

        val batteryButton = actionButton("🔋 ALLOW BACKGROUND RUNNING")
        batteryButton.setOnClickListener {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        root.addView(batteryButton, match())
        root.addView(space(10))

        val apps = actionButton("📱 MANAGE INSTALLED APPS")
        apps.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
        root.addView(apps, match())
        root.addView(space(10))

        val cacheManager = actionButton("🧹 APP CACHE MANAGER")
        cacheManager.setOnClickListener {
            showCacheApps()
        }
        root.addView(cacheManager, match())
        root.addView(space(10))

        val gaming = actionButton("🎮 GAMING SETTINGS")
        gaming.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        root.addView(gaming, match())
        root.addView(space(20))

        statusText = TextView(this).apply {
            text = "READY • NO ROOT REQUIRED"
            textSize = 12f
            setTextColor(neon)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(statusText, match())
        root.addView(space(12))

        val note = TextView(this).apply {
            text = "Android restricts access to other apps' private cache. Select an app in Cache Manager and clear its cache from App Info > Storage."
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        root.addView(note, match())

        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        if (::autoCleanButton.isInitialized) updateAutoCleanButton()
    }

    private fun updateAutoCleanButton() {
        val enabled = getSharedPreferences("auto_clean", MODE_PRIVATE)
            .getBoolean("enabled", false)
        autoCleanButton.text =
            if (enabled) "⏹ STOP AUTO CLEANER" else "⚡ START AUTO CLEANER"
    }

    private fun showCacheApps() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val apps = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy {
                it.loadLabel(packageManager).toString().lowercase()
            }

        if (apps.isEmpty()) {
            Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show()
            return
        }

        val names = apps.map {
            it.loadLabel(packageManager).toString()
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("APP CACHE MANAGER")
            .setItems(names) { _, which ->
                val packageName = apps[which].activityInfo.packageName

                val details = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )

                try {
                    startActivity(details)
                    Toast.makeText(
                        this,
                        "Open Storage > Clear cache",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "Could not open App Info",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun refreshStats() {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)

        val total = memory.totalMem
        val used = (total - memory.availMem).coerceAtLeast(0)
        val percent = if (total > 0) used * 100 / total else 0

        ramText.text =
            "$percent% USED • ${formatBytes(used)} / ${formatBytes(total)}"

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = stat.totalBytes
        val free = stat.availableBytes
        val usedStorage = totalStorage - free
        val storagePercent =
            if (totalStorage > 0) usedStorage * 100 / totalStorage else 0

        storageText.text =
            "$storagePercent% USED • ${formatBytes(free)} FREE"

        statusText.text = "SYSTEM STATUS • UPDATED"
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = rounded(panel, 18)
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setTextColor(muted)
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun value(text: String) = TextView(this).apply {
        this.text = text
        textSize = 18f
        setTextColor(neon)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(8), 0, 0)
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(neon)
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun actionButton(text: String) = Button(this).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        setTextColor(neon)
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(panel, 14, neon)
        minHeight = dp(58)
        stateListAnimator = null
    }

    private fun rounded(
        color: Int,
        radius: Int,
        stroke: Int? = null
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun match() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int) =
        (value * resources.displayMetrics.density).toInt()

    private fun cacheSize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { cacheSize(it) } ?: 0
    }

    private fun clearDirectory(directory: File) {
        directory.listFiles()?.forEach {
            if (it.isDirectory) clearDirectory(it)
            it.delete()
        }
    }

    private fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) {
            String.format(Locale.US, "%.2f GB", mb / 1024)
        } else {
            String.format(Locale.US, "%.0f MB", mb)
        }
    }
}
