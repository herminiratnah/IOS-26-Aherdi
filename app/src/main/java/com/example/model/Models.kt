package com.example.model

import com.example.R

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val time: String,
    val isFromMe: Boolean
)

data class MessageThread(
    val id: String,
    val contactName: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val avatarColor: Long = 0xFF4A90E2,
    val messages: List<ChatMessage> = emptyList()
)

data class CallRecord(
    val id: String,
    val contactName: String,
    val phoneNumber: String,
    val callType: String, // "Mobile", "FaceTime Audio", etc.
    val timeLabel: String,
    val isMissed: Boolean = false,
    val avatarColor: Long = 0xFF50E3C2
)

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarColor: Long = 0xFF007AFF
)

data class NoteItem(
    val id: String,
    val title: String,
    val body: String,
    val dateLabel: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val snippet: String get() = body
    val date: String get() = dateLabel
}

data class CalendarEvent(
    val id: String,
    val title: String,
    val timeRange: String,
    val colorHex: Long,
    val dayOfMonth: Int = 15
)

data class AlarmItem(
    val id: String,
    val timeLabel: String,
    val period: String, // AM/PM
    val label: String,
    val isEnabled: Boolean = true
)

data class WorldClockCity(
    val name: String,
    val timeDiff: String,
    val timeFormatted: String,
    val hourOffset: Int
)

data class PhotoItem(
    val id: String,
    val title: String,
    val date: String,
    val drawableRes: Int = 0,
    val imageUri: String? = null
)

data class MailItem(
    val id: String,
    val sender: String,
    val subject: String,
    val preview: String,
    val time: String,
    val isUnread: Boolean = false
)

enum class ClockFontStyle {
    BOLD_MODERN,
    SERIF_CLASSIC,
    ROUNDED_SLICK,
    TECH_STENCIL,
    MINIMAL_THIN
}

data class LockScreenConfig(
    val fontStyle: ClockFontStyle = ClockFontStyle.BOLD_MODERN,
    val clockColorHex: Long = 0xFFFFFFFF,
    val wallpaperId: String = "default",
    val showWeatherWidget: Boolean = true,
    val showBatteryWidget: Boolean = true,
    val showCalendarWidget: Boolean = true
)

data class RootCommandLog(
    val timestamp: String,
    val command: String,
    val output: String,
    val isSuccess: Boolean
)

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Apple Music",
    val durationMs: Long = 210000L,
    val uriString: String? = null,
    val coverGradientStart: Long = 0xFFFF2D55,
    val coverGradientEnd: Long = 0xFF5856D6
)

data class WallpaperItem(
    val id: String,
    val name: String,
    val category: String, // "iOS 26", "Featured", "Astronomy", "Color"
    val drawableRes: Int,
    val gradientColors: List<Long>? = null
)

data class ReminderItem(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueDate: String = "Today",
    val isCompleted: Boolean = false,
    val listName: String = "Reminders",
    val flagColor: Long = 0xFF007AFF
)

data class IOSNotificationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val timestamp: String = "Now",
    val timeMillis: Long = System.currentTimeMillis(),
    val iconKey: String = "bell",
    val accentColor: Long = 0xFF007AFF
)


