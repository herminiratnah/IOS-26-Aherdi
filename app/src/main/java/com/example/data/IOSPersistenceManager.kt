package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import com.example.model.AlarmItem
import com.example.model.CalendarEvent
import com.example.model.NoteItem
import com.example.model.ReminderItem
import com.example.util.RootExecutor
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

object IOSPersistenceManager {

    private const val PREFS_NAME = "ios_system_persistence"
    private const val KEY_CALENDAR_EVENTS = "calendar_events"
    private const val KEY_ALARMS = "clock_alarms"
    private const val KEY_REMINDERS = "reminders_list"

    fun getPrefs(context: Context): SharedPreferences {
        val targetContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N && !context.isDeviceProtectedStorage) {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
            if (userManager?.isUserUnlocked == false) {
                context.createDeviceProtectedStorageContext()
            } else {
                context
            }
        } else {
            context
        }
        return try {
            targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (_: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // ================= CALENDAR EVENTS =================

    fun loadCalendarEvents(context: Context): List<CalendarEvent> {
        val list = mutableListOf<CalendarEvent>()

        // 1. Load from local persistent SharedPreferences
        val jsonStr = getPrefs(context).getString(KEY_CALENDAR_EVENTS, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        CalendarEvent(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Event"),
                            timeRange = obj.optString("timeRange", "10:00 – 11:00"),
                            colorHex = obj.optLong("colorHex", 0xFF007AFF),
                            dayOfMonth = obj.optInt("dayOfMonth", Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Also sync and read from real Android device Calendar
        try {
            val calUri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )
            val cursor = context.contentResolver.query(calUri, projection, null, null, null)
            cursor?.use {
                val idCol = it.getColumnIndex(CalendarContract.Events._ID)
                val titleCol = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startCol = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endCol = it.getColumnIndex(CalendarContract.Events.DTEND)

                val cal = Calendar.getInstance()
                while (it.moveToNext()) {
                    val id = if (idCol >= 0) "sys_" + it.getLong(idCol) else UUID.randomUUID().toString()
                    val title = if (titleCol >= 0) it.getString(titleCol) ?: "Event" else "Event"
                    val startMs = if (startCol >= 0) it.getLong(startCol) else 0L
                    val endMs = if (endCol >= 0) it.getLong(endCol) else 0L

                    var day = cal.get(Calendar.DAY_OF_MONTH)
                    var timeRange = "All Day"
                    if (startMs > 0) {
                        val eventCal = Calendar.getInstance().apply { timeInMillis = startMs }
                        day = eventCal.get(Calendar.DAY_OF_MONTH)
                        val startH = String.format("%02d:%02d", eventCal.get(Calendar.HOUR_OF_DAY), eventCal.get(Calendar.MINUTE))
                        if (endMs > startMs) {
                            val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
                            val endH = String.format("%02d:%02d", endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))
                            timeRange = "$startH – $endH"
                        } else {
                            timeRange = startH
                        }
                    }

                    if (list.none { it.id == id || (it.title == title && it.dayOfMonth == day) }) {
                        list.add(CalendarEvent(id, title, timeRange, 0xFF34C759, day))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If completely empty on fresh install, provide standard iOS starter events
        if (list.isEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            list.add(CalendarEvent("init_1", "Design Review iOS 26", "10:00 – 11:30", 0xFF007AFF, today))
            list.add(CalendarEvent("init_2", "Lunch with Steve", "12:30 – 13:30", 0xFFFF9500, today))
            list.add(CalendarEvent("init_3", "Apple Keynote Live", "19:00 – 21:00", 0xFFFF2D55, today))
            saveCalendarEvents(context, list)
        }

        return list
    }

    fun saveCalendarEvents(context: Context, events: List<CalendarEvent>) {
        try {
            val array = JSONArray()
            for (item in events) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("timeRange", item.timeRange)
                    put("colorHex", item.colorHex)
                    put("dayOfMonth", item.dayOfMonth)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_CALENDAR_EVENTS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insertDeviceCalendarEvent(context: Context, title: String, timeRange: String, dayOfMonth: Int) {
        try {
            // Find a valid calendar ID on device
            var calendarId = 1L
            val calCursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                null, null, null
            )
            calCursor?.use {
                if (it.moveToFirst()) {
                    calendarId = it.getLong(0)
                }
            }

            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            cal.set(Calendar.HOUR_OF_DAY, 10)
            cal.set(Calendar.MINUTE, 0)
            val startMillis = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 11)
            val endMillis = cal.timeInMillis

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, "Created from iOS 26 Calendar")
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ================= CLOCK ALARMS =================

    fun loadAlarms(context: Context): List<AlarmItem> {
        val list = mutableListOf<AlarmItem>()
        val jsonStr = getPrefs(context).getString(KEY_ALARMS, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AlarmItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            timeLabel = obj.optString("timeLabel", "07:00"),
                            period = obj.optString("period", "AM"),
                            label = obj.optString("label", "Alarm"),
                            isEnabled = obj.optBoolean("isEnabled", true)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (list.isEmpty()) {
            list.add(AlarmItem("alm_1", "06:30", "AM", "Morning Run", true))
            list.add(AlarmItem("alm_2", "07:15", "AM", "Work Wake Up", true))
            list.add(AlarmItem("alm_3", "08:00", "AM", "Standup Call", false))
            list.add(AlarmItem("alm_4", "22:30", "PM", "Bedtime Reading", false))
            saveAlarms(context, list)
        }

        return list
    }

    fun saveAlarms(context: Context, alarms: List<AlarmItem>) {
        try {
            val array = JSONArray()
            for (item in alarms) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timeLabel", item.timeLabel)
                    put("period", item.period)
                    put("label", item.label)
                    put("isEnabled", item.isEnabled)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_ALARMS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncSystemAlarm(context: Context, hour: Int, minute: Int, label: String, isEnabled: Boolean) {
        if (!isEnabled) return
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Root fallback for setting real alarm without UI prompt
            RootExecutor.execute("am start -a android.intent.action.SET_ALARM --ei android.intent.extra.alarm.HOUR $hour --ei android.intent.extra.alarm.MINUTES $minute --es android.intent.extra.alarm.MESSAGE \"$label\" --ez android.intent.extra.alarm.SKIP_UI true")
        }
    }

    // ================= REMINDERS =================

    fun loadReminders(context: Context): List<ReminderItem> {
        val list = mutableListOf<ReminderItem>()
        val jsonStr = getPrefs(context).getString(KEY_REMINDERS, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ReminderItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            notes = obj.optString("notes", ""),
                            dueDate = obj.optString("dueDate", "Today"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            listName = obj.optString("listName", "Reminders"),
                            flagColor = obj.optLong("flagColor", 0xFF007AFF)
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun saveReminders(context: Context, reminders: List<ReminderItem>) {
        try {
            val array = JSONArray()
            for (item in reminders) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("notes", item.notes)
                    put("dueDate", item.dueDate)
                    put("isCompleted", item.isCompleted)
                    put("listName", item.listName)
                    put("flagColor", item.flagColor)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_REMINDERS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ================= WALLPAPER =================
    private const val KEY_WALLPAPER_RES = "wallpaper_res_id"
    private const val KEY_WALLPAPER_NAME = "wallpaper_name_label"
    private const val KEY_WALLPAPER_URI = "wallpaper_custom_uri"
    private const val KEY_DEPTH_EFFECT = "depth_effect_enabled"
    private const val KEY_DEPTH_THEME = "depth_theme_name"

    fun saveWallpaper(context: Context, resId: Int, name: String, uri: String? = null) {
        saveLockScreenWallpaper(context, resId, name, uri)
    }

    fun saveLockScreenWallpaper(context: Context, resId: Int, name: String, uri: String? = null) {
        getPrefs(context).edit()
            .putInt(KEY_WALLPAPER_RES, resId)
            .putString(KEY_WALLPAPER_NAME, name)
            .putString(KEY_WALLPAPER_URI, uri ?: "")
            .apply()
    }

    fun loadWallpaper(context: Context): Triple<Int, String, String?> = loadLockScreenWallpaper(context)

    fun loadLockScreenWallpaper(context: Context): Triple<Int, String, String?> {
        val prefs = getPrefs(context)
        val rawResId = prefs.getInt(KEY_WALLPAPER_RES, com.example.R.drawable.wallpaper_alpine)
        val name = prefs.getString(KEY_WALLPAPER_NAME, "Alpine Mountain Peak") ?: "Alpine Mountain Peak"
        val uri = prefs.getString(KEY_WALLPAPER_URI, null)?.ifEmpty { null }

        val validResId = if (rawResId <= 0) {
            if (!uri.isNullOrBlank()) 0 else com.example.R.drawable.wallpaper_alpine
        } else {
            try {
                val type = context.resources.getResourceTypeName(rawResId)
                if (type == "drawable") rawResId else resolveWallpaperByName(name)
            } catch (_: Throwable) {
                if (!uri.isNullOrBlank()) 0 else resolveWallpaperByName(name)
            }
        }

        return Triple(validResId, name, uri)
    }

    private const val KEY_HOME_WALLPAPER_RES = "home_wallpaper_res_id"
    private const val KEY_HOME_WALLPAPER_NAME = "home_wallpaper_name"
    private const val KEY_HOME_WALLPAPER_URI = "home_wallpaper_uri"

    fun saveHomeScreenWallpaper(context: Context, resId: Int, name: String, uri: String? = null) {
        getPrefs(context).edit()
            .putInt(KEY_HOME_WALLPAPER_RES, resId)
            .putString(KEY_HOME_WALLPAPER_NAME, name)
            .putString(KEY_HOME_WALLPAPER_URI, uri ?: "")
            .apply()
    }

    fun loadHomeScreenWallpaper(context: Context): Triple<Int, String, String?> {
        val prefs = getPrefs(context)
        val rawResId = prefs.getInt(KEY_HOME_WALLPAPER_RES, com.example.R.drawable.wallpaper_alpine)
        val name = prefs.getString(KEY_HOME_WALLPAPER_NAME, "Alpine Mountain Peak") ?: "Alpine Mountain Peak"
        val uri = prefs.getString(KEY_HOME_WALLPAPER_URI, null)?.ifEmpty { null }

        val validResId = if (rawResId <= 0) {
            if (!uri.isNullOrBlank()) 0 else com.example.R.drawable.wallpaper_alpine
        } else {
            try {
                val type = context.resources.getResourceTypeName(rawResId)
                if (type == "drawable") rawResId else resolveWallpaperByName(name)
            } catch (_: Throwable) {
                if (!uri.isNullOrBlank()) 0 else resolveWallpaperByName(name)
            }
        }

        return Triple(validResId, name, uri)
    }

    fun resolveWallpaperByName(name: String): Int {
        return when (name) {
            "Twilight City Sunset" -> com.example.R.drawable.wp_twilight_clouds
            "Monochrome Tidal Glow" -> com.example.R.drawable.wp_monochrome_wave
            "Golden Desert Dunes" -> com.example.R.drawable.wp_golden_dunes
            "Golden Coastal Sunset" -> com.example.R.drawable.wp_coastal_flora
            "Dreamy Daisy Meadow" -> com.example.R.drawable.wp_daisy_meadow
            "Golden Lake Reeds" -> com.example.R.drawable.wp_sunset_reeds
            "Crystal Glass Sky" -> com.example.R.drawable.wp_crystal_glass
            "Glass Clock Sanctuary" -> com.example.R.drawable.wp_glass_sanctuary
            "Glass Still Life" -> com.example.R.drawable.wp_glass_still_life
            else -> com.example.R.drawable.wallpaper_alpine
        }
    }

    private const val KEY_CLOCK_FONT_STYLE = "lock_clock_font_style"
    private const val KEY_CLOCK_LAYOUT_MODE = "lock_clock_layout_mode"
    private const val KEY_CLOCK_COLOR_HEX = "lock_clock_color_hex"
    private const val KEY_CLOCK_FONT_SIZE = "lock_clock_font_size"
    private const val KEY_CLOCK_FONT_WEIGHT = "lock_clock_font_weight"
    private const val KEY_CLOCK_OPACITY = "lock_clock_opacity"
    private const val KEY_CLOCK_TOP_PADDING = "lock_clock_top_padding"
    private const val KEY_CLOCK_HORIZ_OFFSET = "lock_clock_horiz_offset"
    private const val KEY_CLOCK_POS_X = "lock_clock_pos_x"
    private const val KEY_CLOCK_POS_Y = "lock_clock_pos_y"
    private const val KEY_PARALLAX_INTENSITY = "lock_parallax_intensity"
    private const val KEY_SHOW_BG_SUN = "lock_show_bg_sun"
    private const val KEY_SHOW_AMBIENT_ATMO = "lock_show_ambient_atmo"
    private const val KEY_USE_AUTO_THEME_COLOR = "lock_use_auto_theme_color"
    private const val KEY_WIDGET_WEATHER = "lock_widget_weather"
    private const val KEY_WIDGET_BATTERY = "lock_widget_battery"
    private const val KEY_WIDGET_CALENDAR = "lock_widget_calendar"
    private const val KEY_CUSTOM_FG_URI = "lock_custom_fg_uri"

    fun saveFullLockScreenConfig(context: Context, config: com.example.model.LockScreenConfig) {
        getPrefs(context).edit()
            .putBoolean(KEY_DEPTH_EFFECT, config.depthEffectEnabled)
            .putString(KEY_DEPTH_THEME, config.depthTheme)
            .putString(KEY_CLOCK_FONT_STYLE, config.fontStyle.name)
            .putString(KEY_CLOCK_LAYOUT_MODE, config.clockLayoutMode.name)
            .putLong(KEY_CLOCK_COLOR_HEX, config.clockColorHex)
            .putFloat(KEY_CLOCK_FONT_SIZE, config.clockFontSize)
            .putInt(KEY_CLOCK_FONT_WEIGHT, config.clockFontWeight)
            .putFloat(KEY_CLOCK_OPACITY, config.clockOpacity)
            .putFloat(KEY_CLOCK_TOP_PADDING, config.clockTopPaddingPercent)
            .putFloat(KEY_CLOCK_HORIZ_OFFSET, config.clockHorizontalOffset)
            .putFloat(KEY_CLOCK_POS_X, config.clockPosX)
            .putFloat(KEY_CLOCK_POS_Y, config.clockPosY)
            .putFloat(KEY_PARALLAX_INTENSITY, config.parallaxIntensity)
            .putBoolean(KEY_SHOW_BG_SUN, config.showBackgroundSun)
            .putBoolean(KEY_SHOW_AMBIENT_ATMO, config.showAmbientAtmosphere)
            .putBoolean(KEY_USE_AUTO_THEME_COLOR, config.useAutoThemeColor)
            .putString(KEY_CUSTOM_FG_URI, config.customForegroundUri ?: "")
            .putBoolean(KEY_WIDGET_WEATHER, config.showWeatherWidget)
            .putBoolean(KEY_WIDGET_BATTERY, config.showBatteryWidget)
            .putBoolean(KEY_WIDGET_CALENDAR, config.showCalendarWidget)
            .apply()
    }

    fun saveDepthConfig(context: Context, enabled: Boolean, theme: String) {
        getPrefs(context).edit()
            .putBoolean(KEY_DEPTH_EFFECT, enabled)
            .putString(KEY_DEPTH_THEME, theme)
            .apply()
    }

    fun loadFullLockScreenConfig(context: Context): com.example.model.LockScreenConfig {
        val prefs = getPrefs(context)
        val fontStyleStr = prefs.getString(KEY_CLOCK_FONT_STYLE, com.example.model.ClockFontStyle.EDITORIAL_SERIF.name)
        val fontStyle = try {
            com.example.model.ClockFontStyle.valueOf(fontStyleStr ?: com.example.model.ClockFontStyle.EDITORIAL_SERIF.name)
        } catch (_: Exception) {
            com.example.model.ClockFontStyle.EDITORIAL_SERIF
        }

        val layoutModeStr = prefs.getString(KEY_CLOCK_LAYOUT_MODE, com.example.model.ClockLayoutMode.STACKED_EDITORIAL.name)
        val layoutMode = try {
            com.example.model.ClockLayoutMode.valueOf(layoutModeStr ?: com.example.model.ClockLayoutMode.STACKED_EDITORIAL.name)
        } catch (_: Exception) {
            com.example.model.ClockLayoutMode.STACKED_EDITORIAL
        }

        return com.example.model.LockScreenConfig(
            fontStyle = fontStyle,
            clockLayoutMode = layoutMode,
            clockColorHex = prefs.getLong(KEY_CLOCK_COLOR_HEX, 0xFFFFFFFF),
            useAutoThemeColor = prefs.getBoolean(KEY_USE_AUTO_THEME_COLOR, true),
            clockFontSize = prefs.getFloat(KEY_CLOCK_FONT_SIZE, 108f),
            clockFontWeight = prefs.getInt(KEY_CLOCK_FONT_WEIGHT, 600),
            clockOpacity = prefs.getFloat(KEY_CLOCK_OPACITY, 1.0f),
            clockTopPaddingPercent = prefs.getFloat(KEY_CLOCK_TOP_PADDING, 0.10f),
            clockHorizontalOffset = prefs.getFloat(KEY_CLOCK_HORIZ_OFFSET, 0f),
            clockPosX = prefs.getFloat(KEY_CLOCK_POS_X, 0.5f),
            clockPosY = prefs.getFloat(KEY_CLOCK_POS_Y, 0.22f),
            parallaxIntensity = prefs.getFloat(KEY_PARALLAX_INTENSITY, 1.0f),
            showBackgroundSun = prefs.getBoolean(KEY_SHOW_BG_SUN, true),
            showAmbientAtmosphere = prefs.getBoolean(KEY_SHOW_AMBIENT_ATMO, true),
            depthEffectEnabled = prefs.getBoolean(KEY_DEPTH_EFFECT, true),
            depthTheme = prefs.getString(KEY_DEPTH_THEME, "mountain") ?: "mountain",
            customForegroundUri = prefs.getString(KEY_CUSTOM_FG_URI, null)?.ifEmpty { null },
            showWeatherWidget = prefs.getBoolean(KEY_WIDGET_WEATHER, false),
            showBatteryWidget = prefs.getBoolean(KEY_WIDGET_BATTERY, false),
            showCalendarWidget = prefs.getBoolean(KEY_WIDGET_CALENDAR, false)
        )
    }

    fun loadDepthConfig(context: Context): Pair<Boolean, String> {
        val prefs = getPrefs(context)
        return Pair(
            prefs.getBoolean(KEY_DEPTH_EFFECT, true),
            prefs.getString(KEY_DEPTH_THEME, "mountain") ?: "mountain"
        )
    }

    // ================= NOTES =================
    private const val KEY_NOTES = "notes_list_json"

    fun loadNotes(context: Context): List<NoteItem> {
        val list = mutableListOf<NoteItem>()
        val jsonStr = getPrefs(context).getString(KEY_NOTES, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        NoteItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", "Catatan"),
                            body = obj.optString("body", ""),
                            dateLabel = obj.optString("dateLabel", "Today"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun saveNotes(context: Context, notes: List<NoteItem>) {
        try {
            val array = JSONArray()
            for (item in notes) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("dateLabel", item.dateLabel)
                    put("updatedAt", item.updatedAt)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_NOTES, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ================= IROOT STATES =================

    fun saveRootBoolean(context: Context, key: String, value: Boolean) {
        getPrefs(context).edit().putBoolean("iroot_$key", value).commit()
    }

    fun loadRootBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getPrefs(context).getBoolean("iroot_$key", defaultValue)
    }

    fun saveRootString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString("iroot_$key", value).commit()
    }

    fun loadRootString(context: Context, key: String, defaultValue: String): String {
        return getPrefs(context).getString("iroot_$key", defaultValue) ?: defaultValue
    }

    fun saveRootInt(context: Context, key: String, value: Int) {
        getPrefs(context).edit().putInt("iroot_$key", value).commit()
    }

    fun loadRootInt(context: Context, key: String, defaultValue: Int): Int {
        return getPrefs(context).getInt("iroot_$key", defaultValue)
    }

    // ================= CUSTOM APP ICONS =================
    private const val PREFS_CUSTOM_ICONS = "ios_custom_app_icons"

    fun loadCustomIcons(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_CUSTOM_ICONS, Context.MODE_PRIVATE)
        val result = mutableMapOf<String, String>()
        for ((k, v) in prefs.all) {
            if (v is String && v.isNotBlank()) {
                result[k] = v
            }
        }
        return result
    }

    fun saveCustomIcon(context: Context, appKey: String, filePath: String) {
        context.getSharedPreferences(PREFS_CUSTOM_ICONS, Context.MODE_PRIVATE)
            .edit().putString(appKey, filePath).apply()
    }

    fun removeCustomIcon(context: Context, appKey: String) {
        context.getSharedPreferences(PREFS_CUSTOM_ICONS, Context.MODE_PRIVATE)
            .edit().remove(appKey).apply()
    }

    // ================= IPHONE 16/17 & IOS 26 SETTINGS =================
    fun loadActionButton(context: Context): String =
        getPrefs(context).getString("action_button_function", "Flashlight") ?: "Flashlight"

    fun saveActionButton(context: Context, func: String) =
        getPrefs(context).edit().putString("action_button_function", func).apply()

    fun loadCameraControlEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean("camera_control_enabled", true)

    fun saveCameraControlEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean("camera_control_enabled", enabled).apply()

    fun loadStandByEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean("standby_enabled", true)

    fun saveStandByEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean("standby_enabled", enabled).commit()

    fun loadFocusMode(context: Context): com.example.model.FocusMode {
        val raw = getPrefs(context).getString("focus_mode", "OFF") ?: "OFF"
        return try {
            com.example.model.FocusMode.valueOf(raw)
        } catch (e: Exception) {
            com.example.model.FocusMode.OFF
        }
    }

    fun saveFocusMode(context: Context, mode: com.example.model.FocusMode) =
        getPrefs(context).edit().putString("focus_mode", mode.name).commit()

    fun loadBackTapEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean("back_tap_enabled", true)

    fun saveBackTapEnabled(context: Context, enabled: Boolean) =
        getPrefs(context).edit().putBoolean("back_tap_enabled", enabled).commit()

    fun loadBackTapDoubleAction(context: Context): com.example.model.BackTapAction {
        val raw = getPrefs(context).getString("back_tap_double_action", "FLASHLIGHT") ?: "FLASHLIGHT"
        return try {
            com.example.model.BackTapAction.valueOf(raw)
        } catch (e: Exception) {
            com.example.model.BackTapAction.FLASHLIGHT
        }
    }

    fun saveBackTapDoubleAction(context: Context, action: com.example.model.BackTapAction) =
        getPrefs(context).edit().putString("back_tap_double_action", action.name).commit()

    fun loadBackTapTripleAction(context: Context): com.example.model.BackTapAction {
        val raw = getPrefs(context).getString("back_tap_triple_action", "SCREENSHOT") ?: "SCREENSHOT"
        return try {
            com.example.model.BackTapAction.valueOf(raw)
        } catch (e: Exception) {
            com.example.model.BackTapAction.SCREENSHOT
        }
    }

    fun saveBackTapTripleAction(context: Context, action: com.example.model.BackTapAction) =
        getPrefs(context).edit().putString("back_tap_triple_action", action.name).commit()

    // ================= HOME SCREEN & DOCK APPS PERSISTENCE =================
    private const val KEY_HOME_SCREEN_APPS = "ios_home_screen_apps_list"
    private const val KEY_DOCK_APPS = "ios_dock_apps_list"

    fun loadHomeScreenApps(context: Context): List<String>? {
        val raw = getPrefs(context).getString(KEY_HOME_SCREEN_APPS, null) ?: return null
        if (raw.isBlank()) return emptyList()
        return raw.split(";").filter { it.isNotBlank() }
    }

    fun saveHomeScreenApps(context: Context, apps: List<String>) {
        val raw = apps.joinToString(";")
        getPrefs(context).edit().putString(KEY_HOME_SCREEN_APPS, raw).apply()
    }

    fun loadDockApps(context: Context): List<String>? {
        val raw = getPrefs(context).getString(KEY_DOCK_APPS, null) ?: return null
        if (raw.isBlank()) return emptyList()
        return raw.split(";").filter { it.isNotBlank() }
    }

    fun saveDockApps(context: Context, apps: List<String>) {
        val raw = apps.joinToString(";")
        getPrefs(context).edit().putString(KEY_DOCK_APPS, raw).apply()
    }

    // ================= APP LIBRARY CUSTOM FOLDERS PERSISTENCE =================
    private const val KEY_APP_CATEGORY_OVERRIDES = "app_category_overrides"
    private const val KEY_CUSTOM_FOLDERS = "custom_folders"

    fun loadAppCategoryOverrides(context: Context): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val json = getPrefs(context).getString(KEY_APP_CATEGORY_OVERRIDES, null) ?: return map
        return try {
            val obj = JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
            map
        } catch (_: Exception) {
            map
        }
    }

    fun saveAppCategoryOverride(context: Context, appKey: String, newCategory: String) {
        try {
            val current = loadAppCategoryOverrides(context).toMutableMap()
            current[appKey] = newCategory
            val obj = JSONObject(current as Map<*, *>)
            getPrefs(context).edit().putString(KEY_APP_CATEGORY_OVERRIDES, obj.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCustomFolders(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_CUSTOM_FOLDERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomFolder(context: Context, folderName: String) {
        try {
            val list = loadCustomFolders(context).toMutableList()
            if (!list.contains(folderName)) {
                list.add(folderName)
                val arr = JSONArray(list)
                getPrefs(context).edit().putString(KEY_CUSTOM_FOLDERS, arr.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeCustomFolder(context: Context, folderName: String) {
        try {
            val list = loadCustomFolders(context).toMutableList()
            if (list.remove(folderName)) {
                val arr = JSONArray(list)
                getPrefs(context).edit().putString(KEY_CUSTOM_FOLDERS, arr.toString()).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private const val KEY_GEMINI_API_KEY = "gemini_api_key_custom"
    private const val KEY_GEMINI_MODEL = "gemini_model_custom"

    fun getGeminiApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun saveGeminiApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_API_KEY, key.trim()).apply()
    }

    fun getGeminiModel(context: Context): String {
        return getPrefs(context).getString(KEY_GEMINI_MODEL, "gemini-flash-latest") ?: "gemini-flash-latest"
    }

    fun saveGeminiModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_GEMINI_MODEL, model.trim()).apply()
    }
}
