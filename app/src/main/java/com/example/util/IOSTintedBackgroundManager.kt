package com.example.util

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Intelligent Tinted Background Manager for iOS Status Bar & Dynamic Island.
 * Dynamically detects active third-party apps (WhatsApp, Instagram, YouTube, Chrome, etc.)
 * as well as built-in Android system apps, providing seamless header color matching
 * and intelligent dark/light icon contrast.
 */
object IOSTintedBackgroundManager {

    /**
     * Determines the optimal Status Bar tinted background color and icon theme
     * (Dark Icons vs Light Icons) for any given Android package.
     */
    fun getThemeForPackage(packageName: String, isNightMode: Boolean): Pair<Color, Boolean> {
        val pkg = packageName.lowercase()

        return when {
            // WhatsApp & WhatsApp Business (Clean header matching)
            pkg.contains("com.whatsapp") || pkg.contains("whatsapp") -> {
                if (isNightMode) {
                    Pair(Color(0xFF121B22), false) // WhatsApp Dark Theme Slate
                } else {
                    Pair(Color(0xFFFFFFFF), true)  // WhatsApp Light Theme Crisp White / Header
                }
            }

            // Instagram
            pkg.contains("instagram") -> {
                if (isNightMode) {
                    Pair(Color(0xFF000000), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // YouTube
            pkg.contains("youtube") -> {
                if (isNightMode) {
                    Pair(Color(0xFF0F0F0F), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // TikTok
            pkg.contains("tiktok") || pkg.contains("musically") || pkg.contains("trill") -> {
                Pair(Color(0xFF000000), false)
            }

            // Browsers (Chrome, Samsung Internet, Firefox, Opera, Brave, Edge)
            pkg.contains("chrome") || pkg.contains("sbrowser") || pkg.contains("firefox") ||
            pkg.contains("browser") || pkg.contains("opera") || pkg.contains("brave") || pkg.contains("emmx") -> {
                if (isNightMode) {
                    Pair(Color(0xFF202124), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // Telegram
            pkg.contains("telegram") -> {
                if (isNightMode) {
                    Pair(Color(0xFF17212B), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // Spotify
            pkg.contains("spotify") -> {
                Pair(Color(0xFF121212), false)
            }

            // Facebook & Messenger
            pkg.contains("facebook") || pkg.contains("katana") || pkg.contains("orca") -> {
                if (isNightMode) {
                    Pair(Color(0xFF242526), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // Twitter / X
            pkg.contains("twitter") || pkg.contains("twtr") -> {
                if (isNightMode) {
                    Pair(Color(0xFF000000), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // Google Apps (Gmail, Docs, Drive, Keep, Calendar)
            pkg.contains("google.android.gm") || pkg.contains("apps.docs") || pkg.contains("keep") -> {
                if (isNightMode) {
                    Pair(Color(0xFF1F1F1F), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // Google Maps
            pkg.contains("maps") -> {
                if (isNightMode) {
                    Pair(Color(0xFF202124), false)
                } else {
                    Pair(Color(0xFFFFFFFF), true)
                }
            }

            // E-Commerce & Financial & Super Apps
            pkg.contains("shopee") -> Pair(Color(0xFFEE4D2D), false) // Shopee Orange with white icons
            pkg.contains("tokopedia") -> Pair(Color(0xFFFFFFFF), true) // Tokopedia White header
            pkg.contains("gojek") || pkg.contains("go-jek") -> Pair(Color(0xFF00880C), false) // Gojek Green
            pkg.contains("grab") -> Pair(Color(0xFF00B14F), false) // Grab Green
            pkg.contains("dana") -> Pair(Color(0xFF118EEA), false) // DANA Blue
            pkg.contains("bca") -> Pair(Color(0xFF003D79), false) // BCA Blue
            pkg.contains("capcut") -> Pair(Color(0xFF000000), false) // CapCut Black
            pkg.contains("netflix") -> Pair(Color(0xFF000000), false) // Netflix Black
            pkg.contains("mobilelegends") || pkg.contains("freefire") || pkg.contains("pubg") -> Pair(Color(0xFF000000), false) // Games Black
            pkg.contains("lazada") -> Pair(Color(0xFF0F146D), false) // Lazada Blue

            // Built-in Android System Apps (Samsung, Xiaomi, AOSP, Google)
            pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("contacts") -> {
                if (isNightMode) Pair(Color(0xFF121212), false) else Pair(Color(0xFFFFFFFF), true)
            }

            pkg.contains("messaging") || pkg.contains("mms") -> {
                if (isNightMode) Pair(Color(0xFF121212), false) else Pair(Color(0xFFFFFFFF), true)
            }

            pkg.contains("camera") -> {
                Pair(Color(0xFF000000), false) // Camera view is always pure black
            }

            pkg.contains("gallery") || pkg.contains("photos") -> {
                if (isNightMode) Pair(Color(0xFF000000), false) else Pair(Color(0xFFFFFFFF), true)
            }

            pkg.contains("settings") -> {
                if (isNightMode) Pair(Color(0xFF1C1C1E), false) else Pair(Color(0xFFFFFFFF), true)
            }

            pkg.contains("files") || pkg.contains("documentsui") || pkg.contains("myfiles") -> {
                if (isNightMode) Pair(Color(0xFF1C1C1E), false) else Pair(Color(0xFFFFFFFF), true)
            }

            pkg.contains("vending") -> { // Google Play Store
                if (isNightMode) Pair(Color(0xFF1F1F1F), false) else Pair(Color(0xFFFFFFFF), true)
            }

            // Fallback for any other third-party or system app
            isNightMode -> {
                Pair(Color(0xFF000000), false)
            }
            else -> {
                Pair(Color(0xFFFFFFFF), true)
            }
        }
    }

    /**
     * Intelligent, multi-tier detection of currently focused foreground package.
     * Tier 1: UsageStatsManager (zero overhead, in-process memory lookup)
     * Tier 2: ActivityManager (instantaneous task lookup)
     * Tier 3: Root dumpsys fallback (cached and safe)
     */
    suspend fun detectForegroundPackage(context: Context? = null, selfPackageName: String): String = withContext(Dispatchers.IO) {
        // Tier 1: UsageStatsManager
        if (context != null) {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usm != null) {
                    val now = System.currentTimeMillis()
                    val events = usm.queryEvents(now - 3000, now)
                    val event = UsageEvents.Event()
                    var latestPkg: String? = null
                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                        ) {
                            val p = event.packageName ?: ""
                            if (p.isNotBlank() && p != selfPackageName && !p.contains("com.android.systemui")) {
                                latestPkg = p
                            }
                        }
                    }
                    if (!latestPkg.isNullOrBlank()) {
                        return@withContext latestPkg
                    }
                }
            } catch (_: Exception) {}

            // Tier 2: ActivityManager
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                @Suppress("DEPRECATION")
                val tasks = am?.getRunningTasks(1)
                if (!tasks.isNullOrEmpty()) {
                    val topPkg = tasks[0].topActivity?.packageName ?: ""
                    if (topPkg.isNotBlank() && topPkg != selfPackageName && !topPkg.contains("com.android.systemui")) {
                        return@withContext topPkg
                    }
                }
            } catch (_: Exception) {}
        }

        // Tier 3: Root dumpsys fallback
        try {
            val windowFocusOut = RootExecutor.execute("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'").second
            if (windowFocusOut.isNotBlank()) {
                val match = Regex("""([a-zA-Z0-9_.]+\.[a-zA-Z0-9_.]+)/""").find(windowFocusOut)
                if (match != null) {
                    val pkg = match.groupValues[1]
                    if (pkg != selfPackageName && !pkg.contains("com.android.systemui")) {
                        return@withContext pkg
                    }
                }
            }

            val activityOut = RootExecutor.execute("dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'").second
            if (activityOut.isNotBlank()) {
                val match = Regex("""([a-zA-Z0-9_.]+\.[a-zA-Z0-9_.]+)/""").find(activityOut)
                if (match != null) {
                    val pkg = match.groupValues[1]
                    if (pkg != selfPackageName && !pkg.contains("com.android.systemui")) {
                        return@withContext pkg
                    }
                }
            }
        } catch (_: Exception) {}
        ""
    }
}
