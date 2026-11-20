
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
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var ramText: TextView
    private lateinit var storageText: TextView
    private lateinit var statusText: TextView
    private lateinit var ramProgress: ProgressBar
    private lateinit var storageProgress: ProgressBar
    private lateinit var autoCleanButton: Button

    private val bg = Color.rgb(7, 12, 10)
    private val panel = Color.rgb(19, 29, 24)
    private val neon = Color.rgb(57, 255, 20)
    private val muted = Color.rgb(155, 175, 163)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildDashboard()
        refreshStats()
        consumeBatchResult()
    }

    private fun buildDashboard() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(25))
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
            text = "ULTIMATE GAME BOOSTER"
            textSize = 11f
            letterSpacing = 2f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        root.addView(subtitle, match())
        root.addView(space(25))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(24), dp(20), dp(24))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(24, 55, 34),
                    Color.rgb(13, 27, 20),
                    Color.rgb(18, 35, 25)
                )
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(50, 130, 65))
            }
        }

        hero.addView(TextView(this).apply {
            text = "GAMING PERFORMANCE"
            textSize = 12f
            letterSpacing = 1.5f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }, match())

        hero.addView(space(8))

        hero.addView(TextView(this).apply {
            text = "READY TO PLAY?"
            textSize = 23f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }, match())

        hero.addView(space(20))

        val boost = Button(this).apply {
            text = "⚡  BOOST NOW"
            textSize = 17f
            isAllCaps = false
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(
                    Color.rgb(57, 255, 20),
                    Color.rgb(120, 255, 70)
                )
            ).apply {
                cornerRadius = dp(18).toFloat()
            }
            minHeight = dp(62)
            stateListAnimator = null
            setOnClickListener {
                refreshStats()
                Toast.makeText(
                    this@MainActivity,
                    "System information refreshed!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        hero.addView(boost, match())
        root.addView(hero, match())
        root.addView(space(22))

        root.addView(sectionTitle("SYSTEM MONITOR"))
        root.addView(space(12))

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val ramCard = card()
        ramCard.addView(label("RAM MEMORY"))
        ramText = value("Loading...")
        ramCard.addView(ramText)
        ramProgress = progressBar()
        ramCard.addView(ramProgress)
        statsRow.addView(
            ramCard,
            LinearLayout.LayoutParams(0, dp(125), 1f).apply {
                marginEnd = dp(6)
            }
        )

        val storageCard = card()
        storageCard.addView(label("STORAGE"))
        storageText = value("Loading...")
        storageCard.addView(storageText)
        storageProgress = progressBar()
        storageCard.addView(storageProgress)
        statsRow.addView(
            storageCard,
            LinearLayout.LayoutParams(0, dp(125), 1f).apply {
                marginStart = dp(6)
            }
        )

        root.addView(statsRow)
        root.addView(space(22))

        root.addView(sectionTitle("SMART BOOSTER"))
        root.addView(space(12))

        autoCleanButton = actionButton("⚡  START AUTO CLEANER")
        autoCleanButton.setOnClickListener {
            toggleAutoCleaner()
        }
        root.addView(autoCleanButton, match())
        root.addView(space(10))

        val cleanAllButton = actionButton("🧹  CLEAN ALL APPS")
        cleanAllButton.setOnClickListener {
            startBatchCacheClean()
        }
        root.addView(cleanAllButton, match())
        root.addView(space(10))

        val toolsButton = actionButton("☰  MORE TOOLS")
        toolsButton.setOnClickListener {
            showTools()
        }
        root.addView(toolsButton, match())
        root.addView(space(20))

        statusText = TextView(this).apply {
            text = "● SYSTEM READY"
            textSize = 12f
            setTextColor(neon)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 1f
        }
        root.addView(statusText, match())
        root.addView(space(10))

        root.addView(TextView(this).apply {
            text = "NO ROOT REQUIRED  •  ANDROID GAME UTILITY"
            textSize = 9f
            setTextColor(muted)
            gravity = Gravity.CENTER
            letterSpacing = 1f
        }, match())

        scroll.addView(root)
        setContentView(scroll)
        updateAutoCleanButton()
    }

    private fun toggleAutoCleaner() {
        val prefs = getSharedPreferences("auto_clean", MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)

        if (enabled) {
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
                Toast.makeText(
                    this,
                    "Could not start Cleaner: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showTools() {
        val tools = arrayOf(
            "🧹  Clean App Cache",
            "📱  Manage Installed Apps",
            "🗂  App Cache Manager",
            "🔋  Allow Background Running",
            "🎮  Gaming Settings"
        )

        AlertDialog.Builder(this)
            .setTitle("MORE TOOLS")
            .setItems(tools) { _, which ->
                when (which) {
                    0 -> cleanAppCache()
                    1 -> startActivity(
                        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                    )
                    2 -> showCacheApps()
                    3 -> allowBackgroundRunning()
                    4 -> startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun cleanAppCache() {
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

    private fun startBatchCacheClean() {
        val component = android.content.ComponentName(
            this,
            CacheAccessibilityService::class.java
        ).flattenToString()

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        if (!enabledServices.split(":").any {
                it.equals(component, ignoreCase = true)
            }) {
            AlertDialog.Builder(this)
                .setTitle("Accessibility permission needed")
                .setMessage(
                    "চালু করতে Accessibility Settings-এ গিয়ে " +
                    "SPEED FAST GAME BOOSTER নির্বাচন করো।"
                )
                .setPositiveButton("OPEN SETTINGS") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("CANCEL", null)
                .show()
            return
        }

        val apps = packageManager.getInstalledApplications(0)
            .filter {
                it.packageName != packageName &&
                (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .sortedBy {
                packageManager.getApplicationLabel(it).toString()
                    .lowercase(Locale.ROOT)
            }

        if (apps.isEmpty()) {
            Toast.makeText(this, "No installed apps found", Toast.LENGTH_SHORT).show()
            return
        }

        val queue = apps.map { it.packageName }
        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)
        val freeBefore = StatFs(Environment.getDataDirectory().path).availableBytes

        prefs.edit()
            .putString("queue", queue.joinToString("\\n"))
            .putInt("index", 0)
            .putInt("step", 0)
            .putLong("initial_free", freeBefore)
            .putBoolean("running", true)
            .putBoolean("result_pending", false)
            .apply()

        statusText.text = "● CLEANING APPS..."
        Toast.makeText(
            this,
            "Starting cache cleaning: ${queue.size} apps",
            Toast.LENGTH_LONG
        ).show()

        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${queue[0]}")
                )
            )
        } catch (e: Exception) {
            prefs.edit().putBoolean("running", false).apply()
            Toast.makeText(
                this,
                "Could not open App Info: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun consumeBatchResult() {
        val prefs = getSharedPreferences("batch_cache", MODE_PRIVATE)
        if (!prefs.getBoolean("result_pending", false)) return

        val cleaned = prefs.getLong("cleaned_bytes", 0L)
        val count = prefs.getInt("processed_count", 0)

        prefs.edit().putBoolean("result_pending", false).apply()

        refreshStats()
        statusText.text = "● CLEANING FINISHED"

        Toast.makeText(
            this,
            "Finished $count apps. Approx. freed: ${formatBytes(cleaned)}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun allowBackgroundRunning() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (_: Exception) {
            startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::autoCleanButton.isInitialized) {
            updateAutoCleanButton()
            consumeBatchResult()
        }
    }

    private fun updateAutoCleanButton() {
        if (!::autoCleanButton.isInitialized) return

        val enabled = getSharedPreferences("auto_clean", MODE_PRIVATE)
            .getBoolean("enabled", false)

        autoCleanButton.text =
            if (enabled) "⏹  AUTO CLEANER: ON"
            else "⚡  START AUTO CLEANER"

        autoCleanButton.setTextColor(if (enabled) neon else white)
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
                val appPackage = apps[which].activityInfo.packageName

                val details = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$appPackage")
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
        val percent = if (total > 0) {
            (used * 100 / total).toInt()
        } else 0

        ramText.text = "$percent% USED"
        ramProgress.progress = percent.coerceIn(0, 100)

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = stat.totalBytes
        val free = stat.availableBytes
        val usedStorage = totalStorage - free

        val storagePercent = if (totalStorage > 0) {
            (usedStorage * 100 / totalStorage).toInt()
        } else 0

        storageText.text = "$storagePercent% USED"
        storageProgress.progress = storagePercent.coerceIn(0, 100)

        statusText.text = "● SYSTEM STATUS UPDATED"
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(13), dp(14), dp(13), dp(12))
        background = rounded(panel, 18, Color.rgb(39, 65, 48))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 10f
        setTextColor(muted)
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun value(text: String) = TextView(this).apply {
        this.text = text
        textSize = 17f
        setTextColor(neon)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(9), 0, dp(12))
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        letterSpacing = 1.5f
        setTextColor(neon)
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun progressBar() = ProgressBar(
        this,
        null,
        android.R.attr.progressBarStyleHorizontal
    ).apply {
        max = 100
        progress = 0
        progressTintList = android.content.res.ColorStateList.valueOf(neon)
        progressBackgroundTintList =
            android.content.res.ColorStateList.valueOf(
                Color.rgb(45, 60, 50)
            )
    }

    private fun actionButton(text: String) = Button(this).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        setTextColor(white)
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(panel, 16, Color.rgb(55, 100, 65))
        minHeight = dp(56)
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

