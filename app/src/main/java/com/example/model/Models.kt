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
    EDITORIAL_SERIF,  // Bodoni Moda High-Fashion Luxury Serif (Didot style)
    SERIF_CLASSIC,    // Cormorant Garamond / Classic Serif
    PLAYFAIR_DISPLAY, // Playfair Display Luxury Editorial
    BOLD_MODERN,      // iOS Bold Sans
    ROUNDED_SLICK,    // iOS Rounded
    TECH_STENCIL,     // Monospace
    MINIMAL_THIN,     // Ultra Light
    ELEGANT_SCRIPT,   // Cursive / Calligraphy
    RETRO_HEAVY,      // Impact / Extra Bold Sans
    CYBER_PUNK        // Futuristic Stencil / Monospace Bold
}

enum class ClockLayoutMode {
    STACKED_EDITORIAL,  // 2-line stacked ("09" above "41") as in user designs
    HORIZONTAL_STANDARD // 1-line horizontal ("09:41") classic iOS
}

data class LockScreenConfig(
    val fontStyle: ClockFontStyle = ClockFontStyle.EDITORIAL_SERIF,
    val clockLayoutMode: ClockLayoutMode = ClockLayoutMode.STACKED_EDITORIAL,
    val clockColorHex: Long = 0xFFFFFFFF,
    val useAutoThemeColor: Boolean = true,
    val clockFontSize: Float = 108f,
    val clockFontWeight: Int = 600,
    val clockOpacity: Float = 1.0f,
    val clockTopPaddingPercent: Float = 0.10f,
    val clockHorizontalOffset: Float = 0f,
    val clockPosX: Float = 0.5f,
    val clockPosY: Float = 0.22f,
    val parallaxIntensity: Float = 1.0f,
    val showBackgroundSun: Boolean = true,
    val showAmbientAtmosphere: Boolean = true,
    val wallpaperId: String = "mountain_depth",
    val depthEffectEnabled: Boolean = true,
    val depthTheme: String = "mountain", // "mountain", "twilight_clouds", "monochrome_wave", "golden_dunes", etc.
    val customForegroundUri: String? = null,
    val showWeatherWidget: Boolean = false,
    val showBatteryWidget: Boolean = false,
    val showCalendarWidget: Boolean = false
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
    val gradientColors: List<Long>? = null,
    val hasDepthEffect: Boolean = false,
    val depthTheme: String = "none"
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

enum class FocusMode(val id: String, val label: String, val iconKey: String, val colorLong: Long) {
    OFF("off", "Mati", "bell", 0xFF8E8E93),
    DO_NOT_DISTURB("dnd", "Jangan Ganggu", "moon", 0xFF5856D6),
    WORK("work", "Bekerja", "work", 0xFF007AFF),
    PERSONAL("personal", "Pribadi", "person", 0xFFFF9500),
    SLEEP("sleep", "Tidur", "bed", 0xFF5AC8FA)
}

enum class BackTapAction(val id: String, val label: String) {
    NONE("none", "Tidak Ada"),
    FLASHLIGHT("flashlight", "Senter"),
    SCREENSHOT("screenshot", "Tangkapan Layar"),
    CONTROL_CENTER("control_center", "Pusat Kontrol"),
    NOTIFICATION_CENTER("notification_center", "Pusat Pemberitahuan"),
    APP_SWITCHER("app_switcher", "Pengalih Aplikasi (App Switcher)"),
    LOCK_SCREEN("lock_screen", "Kunci Layar"),
    MUTE("mute", "Hening / Bisu")
}

data class WidgetSlotData(
    val id: Int,
    val type: String,
    val size: String, // "SMALL" (2x2), "MEDIUM" (4x2), "COMPACT" (2x1), "LARGE" (4x4)
    val row: Int, // 0..5
    val align: String = "LEFT", // "LEFT" or "RIGHT"
    val isEnabled: Boolean = true
)

data class HomeRowLayout(
    val rowIndex: Int,
    val widgets: List<WidgetSlotData>,
    val apps: List<AppItem>,
    val appStartIndex: Int
)




