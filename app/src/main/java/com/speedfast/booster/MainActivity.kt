package com.speedfast.booster

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

    private val bg = Color.rgb(8, 13, 11)
    private val panel = Color.rgb(19, 28, 24)
    private val neon = Color.rgb(57, 255, 20)
    private val white = Color.WHITE
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
            text = "GAME BOOSTER  •  NO ROOT"
            textSize = 11f
            letterSpacing = 0.18f
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

        val boost = actionButton("⚡  BOOST NOW")
        boost.setOnClickListener {
            refreshStats()
            Toast.makeText(
                this,
                "Performance refreshed. Android controls background apps.",
                Toast.LENGTH_LONG
            ).show()
        }
        root.addView(boost, match())
        root.addView(space(10))

        val clean = actionButton("🧹  CLEAN APP CACHE")
        clean.setOnClickListener {
            val before = cacheSize(cacheDir)
            clearDirectory(cacheDir)
            val after = cacheSize(cacheDir)
            val cleaned = (before - after).coerceAtLeast(0)
            refreshStats()
            Toast.makeText(
                this,
                "App cache cleaned: ${formatBytes(cleaned)}",
                Toast.LENGTH_LONG
            ).show()
        }
        root.addView(clean, match())
        root.addView(space(10))

        val apps = actionButton("📱  MANAGE INSTALLED APPS")
        apps.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
        root.addView(apps, match())
        root.addView(space(10))

        val gaming = actionButton("🎮  GAMING SETTINGS")
        gaming.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            Toast.makeText(
                this,
                "Set Do Not Disturb and gaming preferences in Settings.",
                Toast.LENGTH_LONG
            ).show()
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
            text = "Note: Android does not allow ordinary apps to force-stop all other apps or erase their private cache. This cleaner removes only this app's own cache."
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        root.addView(note, match())

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun refreshStats() {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)

        val total = memory.totalMem
        val available = memory.availMem
        val used = (total - available).coerceAtLeast(0)
        val percent = if (total > 0) (used * 100 / total).toInt() else 0

        ramText.text = "$percent% USED  •  ${formatBytes(used)} / ${formatBytes(total)}"

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = stat.totalBytes
        val freeStorage = stat.availableBytes
        val usedStorage = (totalStorage - freeStorage).coerceAtLeast(0)
        val storagePercent =
            if (totalStorage > 0) (usedStorage * 100 / totalStorage).toInt() else 0

        storageText.text =
            "$storagePercent% USED  •  ${formatBytes(freeStorage)} FREE"

        statusText.text = "SYSTEM STATUS • UPDATED"
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(panel, 18)
        }
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
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }
    }

    private fun space(height: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun match() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun cacheSize(file: File): Long {
        if (!file.exists()) return 0
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { cacheSize(it) } ?: 0
    }

    private fun clearDirectory(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) clearDirectory(file)
            file.delete()
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
