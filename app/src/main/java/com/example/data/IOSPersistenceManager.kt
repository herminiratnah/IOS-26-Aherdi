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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

        if (list.isEmpty()) {
            list.add(ReminderItem("rem_1", "Beli kopi Starbucks & pastry", "Pesan via app", "Today", false, "Personal", 0xFFFF9500))
            list.add(ReminderItem("rem_2", "Selesaikan presentasi iOS 26 Pro", "Slides 12-18", "Today", false, "Work", 0xFF007AFF))
            list.add(ReminderItem("rem_3", "Bayar tagihan listrik & internet", "Batas waktu tanggal 15", "Scheduled", false, "Bills", 0xFFFF3B30))
            list.add(ReminderItem("rem_4", "Telepon keluarga sore ini", "Jam 17:00", "Today", true, "Personal", 0xFF34C759))
            saveReminders(context, list)
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

    fun saveWallpaper(context: Context, resId: Int, name: String, uri: String? = null) {
        getPrefs(context).edit()
            .putInt(KEY_WALLPAPER_RES, resId)
            .putString(KEY_WALLPAPER_NAME, name)
            .putString(KEY_WALLPAPER_URI, uri ?: "")
            .apply()
    }

    fun loadWallpaper(context: Context): Triple<Int, String, String?> {
        val prefs = getPrefs(context)
        val resId = prefs.getInt(KEY_WALLPAPER_RES, com.example.R.drawable.ios26_wallpaper)
        val name = prefs.getString(KEY_WALLPAPER_NAME, "iOS 26 Ambient") ?: "iOS 26 Ambient"
        val uri = prefs.getString(KEY_WALLPAPER_URI, null)?.ifEmpty { null }
        return Triple(resId, name, uri)
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

        if (list.isEmpty()) {
            list.add(
                NoteItem(
                    id = "note_1",
                    title = "Fitur iOS 26 Pro",
                    body = "• Dynamic Island & Status Bar melayang di semua aplikasi\n• Kontrol musik real-time dengan live visualizer\n• Sinkronisasi Kalender, Jam & Reminder\n• Desain iOS 18 asli dengan Spring Physics",
                    dateLabel = "Today"
                )
            )
            list.add(
                NoteItem(
                    id = "note_2",
                    title = "Daftar Belanja Mingguan",
                    body = "• Kopi Arabica\n• Croissant butter\n• Susu segar & madu\n• Buah apel & blueberry",
                    dateLabel = "Yesterday"
                )
            )
            saveNotes(context, list)
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
}
