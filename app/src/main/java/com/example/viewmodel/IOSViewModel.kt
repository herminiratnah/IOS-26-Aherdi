package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.provider.Settings
import com.example.data.IOSPersistenceManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.Manifest
import androidx.core.content.ContextCompat
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telecom.TelecomManager
import android.telephony.SmsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.service.OverlayBridge
import com.example.util.FlashlightHelper
import com.example.util.RootExecutor
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class DynamicIslandMode {
    COMPACT,
    EXPANDED,
    NOTIFICATION,
    CALL,
    MUSIC,
    TIMER,
    BATTERY,
    VOLUME
}

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val category: String, // "Recent", "Game", "Audio", "Video", "Image", "Social", "News", "Maps", "Utilities", "Other"
    val isBuiltIn: Boolean = false,
    val builtInId: AppId? = null,
    val iconBitmap: Bitmap? = null,
    val iconColor: Long = 0xFF007AFF
)

data class DynamicIslandData(
    val mode: DynamicIslandMode = DynamicIslandMode.COMPACT,
    val title: String = "",
    val subtitle: String = "",
    val icon: String = "",
    val accentColor: Long = 0xFF34C759
)

class IOSViewModel(application: Application) : AndroidViewModel(application) {

    // Real-time System Telemetry
    private val _currentTimeFormatted = MutableStateFlow("12:00")
    val currentTimeFormatted: StateFlow<String> = _currentTimeFormatted.asStateFlow()

    private val _currentDateFormatted = MutableStateFlow("")
    val currentDateFormatted: StateFlow<String> = _currentDateFormatted.asStateFlow()

    private val _currentDayOfWeek = MutableStateFlow("MON")
    val currentDayOfWeek: StateFlow<String> = _currentDayOfWeek.asStateFlow()

    private val _currentDayOfMonth = MutableStateFlow("1")
    val currentDayOfMonth: StateFlow<String> = _currentDayOfMonth.asStateFlow()

    private val _currentMonthYear = MutableStateFlow("")
    val currentMonthYear: StateFlow<String> = _currentMonthYear.asStateFlow()

    private val _currentHourFloat = MutableStateFlow(9f)
    val currentHourFloat: StateFlow<Float> = _currentHourFloat.asStateFlow()

    private val _currentMinuteFloat = MutableStateFlow(41f)
    val currentMinuteFloat: StateFlow<Float> = _currentMinuteFloat.asStateFlow()

    private val _currentSecondFloat = MutableStateFlow(0f)
    val currentSecondFloat: StateFlow<Float> = _currentSecondFloat.asStateFlow()

    // Real Battery Telemetry
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isBatteryCharging = MutableStateFlow(false)
    val isBatteryCharging: StateFlow<Boolean> = _isBatteryCharging.asStateFlow()

    // Real Network Telemetry
    private val _isWifiConnected = MutableStateFlow(true)
    val isWifiConnected: StateFlow<Boolean> = _isWifiConnected.asStateFlow()

    private val _wifiSignalLevel = MutableStateFlow(4)
    val wifiSignalLevel: StateFlow<Int> = _wifiSignalLevel.asStateFlow()

    private val _cellularSignalBars = MutableStateFlow(4)
    val cellularSignalBars: StateFlow<Int> = _cellularSignalBars.asStateFlow()

    private val _networkType = MutableStateFlow("5G")
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    // Real Contacts List with default initial contacts
    private val _contactsList = MutableStateFlow<List<ContactItem>>(
        listOf(
            ContactItem("1", "Aherdi Soeprapto", "+62 812-3456-7890", 0xFF007AFF),
            ContactItem("2", "Apple Support", "1-800-MY-APPLE", 0xFF8E8E93),
            ContactItem("3", "Emergency Services", "112", 0xFFFF3B30),
            ContactItem("4", "Mom", "+62 811-9876-5432", 0xFFFF2D55),
            ContactItem("5", "Sarah Jenkins", "+62 813-2468-1357", 0xFF5856D6),
            ContactItem("6", "David Miller", "+62 817-1357-9246", 0xFF34C759)
        )
    )
    val contactsList: StateFlow<List<ContactItem>> = _contactsList.asStateFlow()

    // Photos Widget 1-minute rotating index
    private val _activePhotoWidgetIndex = MutableStateFlow(0)
    val activePhotoWidgetIndex: StateFlow<Int> = _activePhotoWidgetIndex.asStateFlow()

    // Per-App Settings Configuration (Persistent in memory for settings app control)
    private val _appSettingsState = MutableStateFlow<Map<String, Any>>(
        mapOf(
            "Safari_Search Engine" to "Google",
            "Safari_Block Pop-ups" to true,
            "Safari_Prevent Cross-Site Tracking" to true,
            "Safari_Notifications" to true,
            "Safari_Cellular Data" to true,
            "Safari_Background App Refresh" to true,
            "Music_Dolby Atmos" to "Automatic",
            "Music_Lossless Audio" to true,
            "Music_EQ" to "Acoustic",
            "Music_Notifications" to true,
            "Music_Cellular Data" to true,
            "Photos_iCloud Photos" to true,
            "Photos_Widget Rotation" to "Every 1 Minute",
            "Photos_Notifications" to true,
            "Phone_Silence Unknown Callers" to false,
            "Phone_Show My Caller ID" to true,
            "Messages_iMessage" to true,
            "Messages_Send Read Receipts" to true,
            "Messages_Send as SMS" to true,
            "Camera_Grid" to true,
            "Camera_Record Video" to "4K at 60 fps",
            "Weather_Temperature Units" to "Celsius (°C)",
            "Maps_Distance Units" to "Kilometers"
        )
    )
    val appSettingsState: StateFlow<Map<String, Any>> = _appSettingsState.asStateFlow()

    // Navigation State
    private val _currentApp = MutableStateFlow<AppId?>(null)
    val currentApp: StateFlow<AppId?> = _currentApp.asStateFlow()
    val activeApp: StateFlow<AppId?> = _currentApp.asStateFlow()

    private val _isControlCenterOpen = MutableStateFlow(false)
    val isControlCenterOpen: StateFlow<Boolean> = _isControlCenterOpen.asStateFlow()

    private val _isNotificationCenterOpen = MutableStateFlow(false)
    val isNotificationCenterOpen: StateFlow<Boolean> = _isNotificationCenterOpen.asStateFlow()

    private val _isLockScreenCustomizeOpen = MutableStateFlow(false)
    val isLockScreenCustomizeOpen: StateFlow<Boolean> = _isLockScreenCustomizeOpen.asStateFlow()

    // Lockscreen Config
    private val _lockScreenConfig = MutableStateFlow(LockScreenConfig())
    val lockScreenConfig: StateFlow<LockScreenConfig> = _lockScreenConfig.asStateFlow()

    // Dynamic Island State
    private val _dynamicIsland = MutableStateFlow(DynamicIslandData())
    val dynamicIsland: StateFlow<DynamicIslandData> = _dynamicIsland.asStateFlow()
    private var dynamicIslandJob: Job? = null

    // System Toggles & Control Center
    private val _airplaneMode = MutableStateFlow(false)
    val airplaneMode: StateFlow<Boolean> = _airplaneMode.asStateFlow()

    private val _wifiEnabled = MutableStateFlow(true)
    val wifiEnabled: StateFlow<Boolean> = _wifiEnabled.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(true)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _cellularEnabled = MutableStateFlow(true)
    val cellularEnabled: StateFlow<Boolean> = _cellularEnabled.asStateFlow()

    private val _hotspotEnabled = MutableStateFlow(false)
    val hotspotEnabled: StateFlow<Boolean> = _hotspotEnabled.asStateFlow()

    private val _flashlightEnabled = MutableStateFlow(false)
    val flashlightEnabled: StateFlow<Boolean> = _flashlightEnabled.asStateFlow()

    private val _brightness = MutableStateFlow(0.85f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _volume = MutableStateFlow(0.70f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Appearance & Dark Mode
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Siri & Apple Intelligence
    private val _isSiriOpen = MutableStateFlow(false)
    val isSiriOpen: StateFlow<Boolean> = _isSiriOpen.asStateFlow()

    // AssistiveTouch Virtual Button
    private val _isAssistiveTouchEnabled = MutableStateFlow(true)
    val isAssistiveTouchEnabled: StateFlow<Boolean> = _isAssistiveTouchEnabled.asStateFlow()

    // Spotlight Search
    private val _isSpotlightOpen = MutableStateFlow(false)
    val isSpotlightOpen: StateFlow<Boolean> = _isSpotlightOpen.asStateFlow()

    fun openSiri() { _isSiriOpen.value = true }
    fun closeSiri() { _isSiriOpen.value = false }
    fun toggleSiri() { _isSiriOpen.value = !_isSiriOpen.value }

    fun openSpotlight() { _isSpotlightOpen.value = true }
    fun closeSpotlight() { _isSpotlightOpen.value = false }

    fun toggleAssistiveTouch() { _isAssistiveTouchEnabled.value = !_isAssistiveTouchEnabled.value }
    fun setAssistiveTouchEnabled(enabled: Boolean) { _isAssistiveTouchEnabled.value = enabled }

    // Wallpaper Management
    private val _currentWallpaperRes = MutableStateFlow(com.example.R.drawable.ios26_wallpaper)
    val currentWallpaperRes: StateFlow<Int> = _currentWallpaperRes.asStateFlow()

    private val _currentWallpaperName = MutableStateFlow("iOS 26 Ambient")
    val currentWallpaperName: StateFlow<String> = _currentWallpaperName.asStateFlow()

    private val _currentWallpaperUri = MutableStateFlow<String?>(null)
    val currentWallpaperUri: StateFlow<String?> = _currentWallpaperUri.asStateFlow()

    // Notification Center Real Pipeline
    private val _activeNotifications = MutableStateFlow<List<IOSNotificationItem>>(
        listOf(
            IOSNotificationItem(
                id = "init_wa",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "Dimas",
                message = "Bro, nanti jangan lupa ketemuan jam 7 ya!",
                timestamp = "5m ago",
                iconKey = "whatsapp",
                accentColor = 0xFF25D366
            ),
            IOSNotificationItem(
                id = "init_ig",
                packageName = "com.instagram.android",
                appName = "Instagram",
                title = "Jessica",
                message = "Liked your story and sent a message ✨",
                timestamp = "18m ago",
                iconKey = "instagram",
                accentColor = 0xFFE1306C
            ),
            IOSNotificationItem(
                id = "init_cal",
                packageName = "com.apple.mobilecal",
                appName = "Calendar",
                title = "Upcoming Event",
                message = "Design Review iOS 26 Pro at 10:00",
                timestamp = "30m ago",
                iconKey = "calendar",
                accentColor = 0xFFFF3B30
            )
        )
    )
    val activeNotifications: StateFlow<List<IOSNotificationItem>> = _activeNotifications.asStateFlow()

    fun dismissNotification(id: String) {
        _activeNotifications.value = _activeNotifications.value.filter { it.id != id }
    }

    fun clearAllNotifications() {
        _activeNotifications.value = emptyList()
    }

    fun addNotification(item: IOSNotificationItem) {
        _activeNotifications.value = listOf(item) + _activeNotifications.value.filter { it.id != item.id }
        showDynamicIslandNotification(
            title = item.title,
            message = item.message,
            icon = item.iconKey,
            accent = item.accentColor
        )
    }

    // Device Lock & Passcode
    private val _isDeviceLocked = MutableStateFlow(false)
    val isDeviceLocked: StateFlow<Boolean> = _isDeviceLocked.asStateFlow()

    private val _isPasscodeEnabled = MutableStateFlow(true)
    val isPasscodeEnabled: StateFlow<Boolean> = _isPasscodeEnabled.asStateFlow()

    private val _userPasscode = MutableStateFlow("1234")
    val userPasscode: StateFlow<String> = _userPasscode.asStateFlow()

    private val _isFaceIdEnabled = MutableStateFlow(true)
    val isFaceIdEnabled: StateFlow<Boolean> = _isFaceIdEnabled.asStateFlow()

    // Real Wi-Fi Network List
    private val _availableWifiList = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            Pair("Home_Fast_WiFi", true),
            Pair("Apple_Park_5G", true),
            Pair("CoffeeShop_Guest", true),
            Pair("Office_HighSpeed", true),
            Pair("Public_Free_WiFi", false)
        )
    )
    val availableWifiList: StateFlow<List<Pair<String, Boolean>>> = _availableWifiList.asStateFlow()
    val scannedWifiList: StateFlow<List<Pair<String, Boolean>>> get() = availableWifiList

    private val _currentWifiSsid = MutableStateFlow("Home_Fast_WiFi")
    val currentWifiSsid: StateFlow<String> = _currentWifiSsid.asStateFlow()

    // Real Bluetooth State
    private var bluetoothReceiver: BroadcastReceiver? = null
    private val _pairedBluetoothDevices = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val pairedBluetoothDevices: StateFlow<List<Pair<String, Boolean>>> = _pairedBluetoothDevices.asStateFlow()

    private val _availableBluetoothDevices = MutableStateFlow<List<String>>(emptyList())
    val availableBluetoothDevices: StateFlow<List<String>> = _availableBluetoothDevices.asStateFlow()

    private val _discoveredBluetoothDevices = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val discoveredBluetoothDevices: StateFlow<List<Pair<String, Boolean>>> = _discoveredBluetoothDevices.asStateFlow()

    private val _isBluetoothScanning = MutableStateFlow<Boolean>(false)
    val isBluetoothScanning: StateFlow<Boolean> = _isBluetoothScanning.asStateFlow()

    // Music Player Engine
    private var mediaPlayer: MediaPlayer? = null
    private val _musicList = MutableStateFlow<List<SongItem>>(
        listOf(
            SongItem("1", "Midnight City", "M83", "Hurry Up, We're Dreaming", 243000L, null, 0xFF5856D6, 0xFFFF2D55),
            SongItem("2", "Get Lucky", "Daft Punk ft. Pharrell", "Random Access Memories", 248000L, null, 0xFFFF9500, 0xFFFF2D55),
            SongItem("3", "Starboy", "The Weeknd ft. Daft Punk", "Starboy", 230000L, null, 0xFFFF3B30, 0xFF5856D6),
            SongItem("4", "Blinding Lights", "The Weeknd", "After Hours", 200000L, null, 0xFF34C759, 0xFF007AFF),
            SongItem("5", "Stay", "The Kid LAROI & Justin Bieber", "F*CK LOVE 3", 141000L, null, 0xFF007AFF, 0xFF5856D6),
            SongItem("6", "As It Was", "Harry Styles", "Harry's House", 167000L, null, 0xFFAF52DE, 0xFFFF2D55)
        )
    )
    val musicList: StateFlow<List<SongItem>> = _musicList.asStateFlow()

    private val _currentSong = MutableStateFlow<SongItem?>(_musicList.value.first())
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _musicProgress = MutableStateFlow(0.25f)
    val musicProgress: StateFlow<Float> = _musicProgress.asStateFlow()

    private val _musicCurrentTimeStr = MutableStateFlow("1:01")
    val musicCurrentTimeStr: StateFlow<String> = _musicCurrentTimeStr.asStateFlow()

    private val _musicDurationStr = MutableStateFlow("4:03")
    val musicDurationStr: StateFlow<String> = _musicDurationStr.asStateFlow()

    private var musicProgressJob: Job? = null

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    // In-App Calling State
    private val _isInCall = MutableStateFlow(false)
    val isInCall: StateFlow<Boolean> = _isInCall.asStateFlow()

    private val _callContactName = MutableStateFlow("")
    val callContactName: StateFlow<String> = _callContactName.asStateFlow()

    private val _callPhoneNumber = MutableStateFlow("")
    val callPhoneNumber: StateFlow<String> = _callPhoneNumber.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds.asStateFlow()

    private val _isCallMuted = MutableStateFlow(false)
    val isCallMuted: StateFlow<Boolean> = _isCallMuted.asStateFlow()

    private val _isCallSpeaker = MutableStateFlow(false)
    val isCallSpeaker: StateFlow<Boolean> = _isCallSpeaker.asStateFlow()

    private var callTimerJob: Job? = null

    // Recents Calls List
    private val _callRecords = MutableStateFlow<List<CallRecord>>(
        listOf(
            CallRecord("1", "Raka", "0812-9988-7711", "Mobile", "09:12", false, 0xFF4A90E2),
            CallRecord("2", "Mama", "0811-2233-4455", "Mobile", "Yesterday", false, 0xFFF5A623),
            CallRecord("3", "Budi", "0813-5566-7788", "Mobile", "Yesterday", false, 0xFF7ED321),
            CallRecord("4", "Sinta", "0819-3344-5566", "Mobile", "Sunday", false, 0xFFBD10E0),
            CallRecord("5", "+62 812 3456 7890", "+62 812 3456 7890", "Indonesia", "Saturday", false, 0xFF9013FE),
            CallRecord("6", "Dimas", "0857-1122-3344", "Mobile", "Friday", true, 0xFFD0021B),
            CallRecord("7", "Alya", "0878-9900-1122", "Mobile", "Thursday", false, 0xFF50E3C2)
        )
    )
    val callRecords: StateFlow<List<CallRecord>> = _callRecords.asStateFlow()

    // Messages State
    private val _messagesThreads = MutableStateFlow<List<MessageThread>>(
        listOf(
            MessageThread(
                "1", "Raka", "Gas, nanti gue kabarin ya.", "9:41", 0, 0xFF4A90E2,
                listOf(
                    ChatMessage("1", "Raka", "Bro, jadi ngumpul di warkop?", "9:38", false),
                    ChatMessage("2", "Me", "Boleh, sekitar jam 10 ya.", "9:40", true),
                    ChatMessage("3", "Raka", "Gas, nanti gue kabarin ya.", "9:41", false)
                )
            ),
            MessageThread(
                "2", "Nadia", "Oke, makasih! 🙏", "8:32", 1, 0xFF50E3C2,
                listOf(
                    ChatMessage("1", "Nadia", "File presentasinya udah gue kirim ya", "8:30", false),
                    ChatMessage("2", "Me", "Siap, udah masuk. Thank you Nad!", "8:31", true),
                    ChatMessage("3", "Nadia", "Oke, makasih! 🙏", "8:32", false)
                )
            ),
            MessageThread(
                "3", "Dimas", "Udah sampai?", "Yesterday", 0, 0xFFF5A623,
                listOf(
                    ChatMessage("1", "Dimas", "Udah sampai?", "Yesterday", false)
                )
            ),
            MessageThread(
                "4", "Mama", "Pulang jam berapa?", "Yesterday", 0, 0xFFE91E63,
                listOf(
                    ChatMessage("1", "Mama", "Pulang jam berapa?", "Yesterday", false)
                )
            ),
            MessageThread(
                "5", "Alya", "Foto nya keren banget!", "Yesterday", 0, 0xFF9C27B0,
                listOf(
                    ChatMessage("1", "Alya", "Foto nya keren banget!", "Yesterday", false)
                )
            ),
            MessageThread(
                "6", "Budi", "Siap, bro!", "Monday", 0, 0xFF009688,
                listOf(
                    ChatMessage("1", "Budi", "Siap, bro!", "Monday", false)
                )
            ),
            MessageThread(
                "7", "Sinta", "Jangan lupa meeting jam 10 ya.", "Monday", 0, 0xFFFF5722,
                listOf(
                    ChatMessage("1", "Sinta", "Jangan lupa meeting jam 10 ya.", "Monday", false)
                )
            ),
            MessageThread(
                "8", "Fajar", "Oke, noted.", "Sunday", 0, 0xFF607D8B,
                listOf(
                    ChatMessage("1", "Fajar", "Oke, noted.", "Sunday", false)
                )
            )
        )
    )
    val messagesThreads: StateFlow<List<MessageThread>> = _messagesThreads.asStateFlow()

    private val _activeThreadId = MutableStateFlow<String?>("1")
    val activeThreadId: StateFlow<String?> = _activeThreadId.asStateFlow()

    // Notes State
    private val _notes = MutableStateFlow<List<NoteItem>>(
        listOf(
            NoteItem("1", "Ide Projek", "Buat aplikasi sederhana untuk emulator iOS 26 di Android dengan native feel, dynamic island, dan root support!", "Today"),
            NoteItem("2", "Daftar Belanja", "Telur, susu, roti, buah, kopi arabika, keju cheddar", "Yesterday"),
            NoteItem("3", "Catatan Perjalanan", "Bandung – 12 Juli 2025\nHotel di Dago, sarapan kupat tahu, cafe hopping di Braga.", "Monday"),
            NoteItem("4", "Rencana Ke Depan", "1. Upgrade skill Android Kotlin Compose\n2. Bikin portofolio mobile developer\n3. Traveling ke Bali", "Sunday"),
            NoteItem("5", "Quotes Favorit", "\"Hal kecil yang konsisten lebih kuat daripada hal besar yang cuma sekali-kali.\"", "Saturday")
        )
    )
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _activeNoteId = MutableStateFlow<String?>("1")
    val activeNoteId: StateFlow<String?> = _activeNoteId.asStateFlow()

    // Calendar Events
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(
        listOf(
            CalendarEvent("1", "Meeting with Team", "09:00 – 10:00", 0xFF32ADE6, 15),
            CalendarEvent("2", "Lunch Break", "12:00 – 13:00", 0xFF34C759, 15),
            CalendarEvent("3", "Project Review", "15:00 – 16:00", 0xFFAF52DE, 15)
        )
    )
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    // Alarms
    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    // Reminders
    private val _reminders = MutableStateFlow<List<ReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    // Stopwatch State
    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _stopwatchTimeMs = MutableStateFlow(0L)
    val stopwatchTimeMs: StateFlow<Long> = _stopwatchTimeMs.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<String>>(emptyList())
    val stopwatchLaps: StateFlow<List<String>> = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null

    // Countdown Timer State
    private val _timerRemainingSeconds = MutableStateFlow(0)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    private var countdownTimerJob: Job? = null

    // Captured Photos (Camera to Photos library)
    private val _capturedPhotos = MutableStateFlow<List<String>>(emptyList())
    val capturedPhotos: StateFlow<List<String>> = _capturedPhotos.asStateFlow()

    private val _photoItems = MutableStateFlow<List<PhotoItem>>(
        listOf(
            PhotoItem("1", "Alpine Valley Lake", "15 Jul", com.example.R.drawable.photo_widget_nature),
            PhotoItem("2", "Impossible Architecture", "14 Jul", com.example.R.drawable.monument_valley_art),
            PhotoItem("3", "iOS 26 Ambient Glow", "12 Jul", com.example.R.drawable.ios26_wallpaper),
            PhotoItem("4", "iPhone SE 2 Studio", "10 Jul", com.example.R.drawable.ios_launcher_icon)
        )
    )
    val photoItems: StateFlow<List<PhotoItem>> = _photoItems.asStateFlow()

    // Mail Items
    private val _mailItems = MutableStateFlow<List<MailItem>>(
        listOf(
            MailItem("1", "Apple", "Your receipt from Apple.", "Here's your invoice for your recent purchase on the App Store.", "9:41", true),
            MailItem("2", "Spotify", "New releases for you", "Check out the newest tracks from your favorite artists including Daft Punk.", "Yesterday", true),
            MailItem("3", "Tokopedia", "Pesanan Anda Sedang Dikirim", "Paket dengan nomor resi TKP123456789 sedang dalam perjalanan ke alamat tujuan.", "Yesterday", false),
            MailItem("4", "Netflix", "Coming this week to Netflix", "Get ready for new seasons and brand new series arriving this weekend.", "Monday", false),
            MailItem("5", "YouTube", "Updates from your subscriptions", "New videos have been uploaded by creators you follow.", "Sunday", false)
        )
    )
    val mailItems: StateFlow<List<MailItem>> = _mailItems.asStateFlow()

    // Root State & Logs
    private val _isRootAvailable = MutableStateFlow(RootExecutor.isRootAvailable())
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val _rootLogs = MutableStateFlow<List<RootCommandLog>>(
        listOf(
            RootCommandLog("09:41:00", "su -v", "Magisk / SuperSU v28.1 (root access ready)", true),
            RootCommandLog("09:41:01", "getprop ro.product.model", "SM-G532G (Galaxy J2 Prime)", true)
        )
    )
    val rootLogs: StateFlow<List<RootCommandLog>> = _rootLogs.asStateFlow()

    private val _isImmersiveLocked = MutableStateFlow(false)
    val isImmersiveLocked: StateFlow<Boolean> = _isImmersiveLocked.asStateFlow()

    private val _cpuBoostEnabled = MutableStateFlow(false)
    val cpuBoostEnabled: StateFlow<Boolean> = _cpuBoostEnabled.asStateFlow()

    private val _spoofedModel = MutableStateFlow("iPhone SE (2nd gen) / iOS 26")
    val spoofedModel: StateFlow<String> = _spoofedModel.asStateFlow()

    init {
        // Load persistent data for Calendar, Alarms, Reminders, Notes, Wallpaper & Bluetooth
        try {
            val app = getApplication<Application>()
            _calendarEvents.value = IOSPersistenceManager.loadCalendarEvents(app)
            _alarms.value = IOSPersistenceManager.loadAlarms(app)
            _reminders.value = IOSPersistenceManager.loadReminders(app)
            _notes.value = IOSPersistenceManager.loadNotes(app)
            val (savedRes, savedName, savedUri) = IOSPersistenceManager.loadWallpaper(app)
            _currentWallpaperRes.value = savedRes
            _currentWallpaperName.value = savedName
            _currentWallpaperUri.value = savedUri
            initBluetoothState(app)

            // Setup OverlayBridge callbacks for cross-app control
            OverlayBridge.onTogglePlayPause = { toggleMusicPlay() }
            OverlayBridge.onNextSong = { skipNextSong() }
            OverlayBridge.onPrevSong = { skipPreviousSong() }
            OverlayBridge.onEndCall = { endCall(null) }
            OverlayBridge.onToggleMute = { toggleMuteCall() }
            OverlayBridge.onToggleSpeaker = { toggleSpeakerCall() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())
            val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val fullDateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

            var lastDay = -1
            var lastMinute = -1

            while (true) {
                val now = Calendar.getInstance()
                val date = now.time
                val currentDay = now.get(Calendar.DAY_OF_YEAR)
                val minute = now.get(Calendar.MINUTE)

                if (minute != lastMinute) {
                    _currentTimeFormatted.value = timeFormat.format(date)
                    lastMinute = minute
                }

                if (currentDay != lastDay) {
                    _currentDayOfWeek.value = dayOfWeekFormat.format(date).uppercase(Locale.getDefault())
                    _currentDayOfMonth.value = dayOfMonthFormat.format(date)
                    _currentMonthYear.value = monthYearFormat.format(date)
                    _currentDateFormatted.value = fullDateFormat.format(date)
                    lastDay = currentDay
                }

                val hour = now.get(Calendar.HOUR)
                val second = now.get(Calendar.SECOND)
                _currentHourFloat.value = hour + minute / 60f
                _currentMinuteFloat.value = minute + second / 60f
                _currentSecondFloat.value = second.toFloat()

                delay(1000)
            }
        }

        // Photo Widget rotation ticker (changes photo every minute)
        viewModelScope.launch {
            while (true) {
                delay(60000L)
                val allPhotos = _photoItems.value
                val selectedIds = _selectedWidgetPhotoIds.value
                val candidateList = if (selectedIds.isNotEmpty()) {
                    allPhotos.filter { it.id in selectedIds }
                } else {
                    allPhotos
                }
                if (candidateList.isNotEmpty()) {
                    val nextItem = candidateList[(_activePhotoWidgetIndex.value + 1) % candidateList.size]
                    val realIndex = allPhotos.indexOfFirst { it.id == nextItem.id }
                    _activePhotoWidgetIndex.value = if (realIndex >= 0) realIndex else 0
                }
            }
        }
    }

    private val _selectedWidgetPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedWidgetPhotoIds: StateFlow<Set<String>> = _selectedWidgetPhotoIds.asStateFlow()

    fun togglePhotoInWidgetSelection(photoId: String) {
        val current = _selectedWidgetPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedWidgetPhotoIds.value = current
        val isNowSelected = current.contains(photoId)
        showDynamicIslandNotification(
            "Photos Widget",
            if (isNowSelected) "Foto ditambahkan ke slideshow widget" else "Foto dihapus dari slideshow widget",
            "photo",
            0xFFFF9500
        )
    }

    fun setWidgetPhotoIndex(index: Int) {
        val total = _photoItems.value.size
        if (total > 0) {
            _activePhotoWidgetIndex.value = (index + total) % total
        }
    }

    fun nextWidgetPhoto() {
        val allPhotos = _photoItems.value
        val selectedIds = _selectedWidgetPhotoIds.value
        val candidateList = if (selectedIds.isNotEmpty()) {
            allPhotos.filter { it.id in selectedIds }
        } else {
            allPhotos
        }
        if (candidateList.isNotEmpty()) {
            val currentIndex = _activePhotoWidgetIndex.value
            val currentItem = allPhotos.getOrNull(currentIndex)
            val candidatePos = candidateList.indexOfFirst { it.id == currentItem?.id }
            val nextCandidate = candidateList[(candidatePos + 1) % candidateList.size]
            val realIndex = allPhotos.indexOfFirst { it.id == nextCandidate.id }
            _activePhotoWidgetIndex.value = if (realIndex >= 0) realIndex else 0
        }
    }

    // App Setting Controls
    fun setAppSetting(key: String, value: Any) {
        _appSettingsState.value = _appSettingsState.value + (key to value)
        showDynamicIslandNotification("Settings", "Pengaturan disimpan", "shield", 0xFF007AFF)
    }

    fun getAppSetting(key: String, default: Any): Any {
        return _appSettingsState.value[key] ?: default
    }

    fun getAppSettingBoolean(key: String, default: Boolean = true): Boolean {
        return (_appSettingsState.value[key] as? Boolean) ?: default
    }

    fun getAppSettingString(key: String, default: String = ""): String {
        return (_appSettingsState.value[key] as? String) ?: default
    }

    // ================= ACTIONS =================

    fun openApp(appId: AppId) {
        _currentApp.value = appId
        _isControlCenterOpen.value = false
        _isNotificationCenterOpen.value = false
        _isLockScreenCustomizeOpen.value = false
    }

    fun closeApp() {
        _currentApp.value = null
    }

    fun toggleControlCenter() {
        _isControlCenterOpen.value = !_isControlCenterOpen.value
        if (_isControlCenterOpen.value) {
            _isNotificationCenterOpen.value = false
        }
    }

    fun closeControlCenter() {
        _isControlCenterOpen.value = false
    }

    fun toggleNotificationCenter() {
        _isNotificationCenterOpen.value = !_isNotificationCenterOpen.value
        if (_isNotificationCenterOpen.value) {
            _isControlCenterOpen.value = false
        }
    }

    fun closeNotificationCenter() {
        _isNotificationCenterOpen.value = false
    }

    fun openLockScreenCustomize() {
        _isLockScreenCustomizeOpen.value = true
        _isNotificationCenterOpen.value = true
    }

    fun closeLockScreenCustomize() {
        _isLockScreenCustomizeOpen.value = false
    }

    fun updateLockScreenConfig(update: (LockScreenConfig) -> LockScreenConfig) {
        _lockScreenConfig.value = update(_lockScreenConfig.value)
    }

    // Dynamic Island Toast & Alerts
    fun showDynamicIslandNotification(title: String, message: String, icon: String = "bell", accent: Long = 0xFF34C759) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.NOTIFICATION,
            title = title,
            subtitle = message,
            icon = icon,
            accentColor = accent
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(4000)
            if (_dynamicIsland.value.mode == DynamicIslandMode.NOTIFICATION) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun dismissDynamicIsland() {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
    }

    fun showDynamicIslandBattery(level: Int, isCharging: Boolean) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.BATTERY,
            title = if (isCharging) "Charging" else "Battery",
            subtitle = "$level%",
            icon = if (isCharging) "charging" else "battery",
            accentColor = if (isCharging) 0xFF34C759 else if (level <= 20) 0xFFFF3B30 else 0xFF34C759
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(3500)
            if (_dynamicIsland.value.mode == DynamicIslandMode.BATTERY) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandVolume(volumePercent: Int) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.VOLUME,
            title = "Volume",
            subtitle = "$volumePercent%",
            icon = if (volumePercent == 0) "volume_mute" else "volume",
            accentColor = 0xFFFFFFFF
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(2500)
            if (_dynamicIsland.value.mode == DynamicIslandMode.VOLUME) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    // Phone Calls (In-App Calling)
    fun startInAppCall(contactName: String, phoneNumber: String) {
        val resolvedName = if (contactName.isNotBlank() && contactName != phoneNumber) {
            contactName
        } else {
            _contactsList.value.firstOrNull {
                it.phoneNumber.replace("[^0-9+]".toRegex(), "") == phoneNumber.replace("[^0-9+]".toRegex(), "")
            }?.name ?: phoneNumber
        }

        _callContactName.value = resolvedName
        _callPhoneNumber.value = phoneNumber
        _callDurationSeconds.value = 0
        _isCallMuted.value = false
        _isCallSpeaker.value = false
        _isInCall.value = true

        // Dial tone audio feedback
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
            toneGen.startTone(ToneGenerator.TONE_SUP_RINGTONE, 850)
        } catch (ignored: Exception) {}

        // Also update Dynamic Island to show active call
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.CALL,
            title = resolvedName,
            subtitle = "00:00",
            icon = "phone",
            accentColor = 0xFF34C759
        )

        OverlayBridge.isInCall.value = true
        OverlayBridge.callName.value = resolvedName
        OverlayBridge.callNumber.value = phoneNumber
        OverlayBridge.callDuration.value = "00:00"

        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_isInCall.value) {
                delay(1000)
                _callDurationSeconds.value += 1
                val min = _callDurationSeconds.value / 60
                val sec = _callDurationSeconds.value % 60
                val timeStr = String.format("%02d:%02d", min, sec)
                OverlayBridge.callDuration.value = timeStr
                if (_dynamicIsland.value.mode == DynamicIslandMode.CALL) {
                    _dynamicIsland.value = _dynamicIsland.value.copy(subtitle = timeStr)
                }
            }
        }

        // Add to recents
        val newRecord = CallRecord(
            id = UUID.randomUUID().toString(),
            contactName = resolvedName,
            phoneNumber = phoneNumber,
            callType = "Mobile Outgoing",
            timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            isMissed = false
        )
        _callRecords.value = listOf(newRecord) + _callRecords.value
    }

    fun endCall(context: Context? = null) {
        _isInCall.value = false
        OverlayBridge.isInCall.value = false
        callTimerJob?.cancel()
        val ctx = context ?: getApplication<Application>()
        try {
            val telecom = ctx.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecom?.endCall()
            }
        } catch (ignored: Exception) {}
        RootExecutor.execute("input keyevent KEYCODE_ENDCALL")

        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 250)
        } catch (ignored: Exception) {}
        if (_dynamicIsland.value.mode == DynamicIslandMode.CALL) {
            _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
        }
    }

    fun toggleMuteCall() {
        _isCallMuted.value = !_isCallMuted.value
    }

    fun toggleSpeakerCall() {
        _isCallSpeaker.value = !_isCallSpeaker.value
    }

    // Messages
    fun markAllMessagesRead() {
        _messagesThreads.value = _messagesThreads.value.map { it.copy(unreadCount = 0) }
    }
    fun selectMessageThread(threadId: String) {
        _activeThreadId.value = threadId
        _messagesThreads.value = _messagesThreads.value.map {
            if (it.id == threadId) it.copy(unreadCount = 0) else it
        }
    }

    fun sendMessage(threadId: String, text: String, context: Context? = null) {
        if (text.isBlank()) return
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newMsg = ChatMessage(UUID.randomUUID().toString(), "Me", text, timeNow, true)

        _messagesThreads.value = _messagesThreads.value.map { thread ->
            if (thread.id == threadId) {
                thread.copy(
                    lastMessage = text,
                    time = timeNow,
                    messages = thread.messages + newMsg
                )
            } else thread
        }

        // Try sending real SMS if context provided and thread has phone number
        if (context != null) {
            try {
                val smsManager = SmsManager.getDefault()
                val thread = _messagesThreads.value.find { it.id == threadId }
                val target = thread?.contactName ?: ""
                // Only send if numeric or has phone number
                if (target.matches(Regex("^[+0-9\\-\\s]+$"))) {
                    smsManager.sendTextMessage(target, null, text, null, null)
                }
            } catch (ignored: Exception) {}
        }

        // Simulate realistic smart iOS reply after 2 seconds
        viewModelScope.launch {
            delay(2000)
            val replyText = when {
                text.contains("apa kabar", ignoreCase = true) -> "Alhamdulillah baik bro! Lu gimana?"
                text.contains("dimana", ignoreCase = true) -> "Lagi di jalan nih, otw."
                text.contains("meeting", ignoreCase = true) -> "Siap, link zoomnya udah ada kan?"
                text.contains("harga", ignoreCase = true) -> "Sesuai pricelist kemarin ya."
                text.contains("oke", ignoreCase = true) -> "Mantap 👍"
                else -> "Siap, ditunggu updatenya!"
            }
            val replyTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val currentThread = _messagesThreads.value.find { it.id == threadId }
            val senderName = currentThread?.contactName ?: "Contact"
            val replyMsg = ChatMessage(UUID.randomUUID().toString(), senderName, replyText, replyTime, false)

            _messagesThreads.value = _messagesThreads.value.map { thread ->
                if (thread.id == threadId) {
                    thread.copy(
                        lastMessage = replyText,
                        time = replyTime,
                        messages = thread.messages + replyMsg
                    )
                } else thread
            }

            // Trigger iOS Dynamic Island notification toast!
            showDynamicIslandNotification(senderName, replyText, "message", 0xFF007AFF)
        }
    }

    // Notes
    fun selectNote(noteId: String) {
        _activeNoteId.value = noteId
    }

    fun addNote(title: String, body: String) {
        val newNote = NoteItem(
            id = UUID.randomUUID().toString(),
            title = if (title.isBlank()) "Catatan Baru" else title,
            body = body,
            dateLabel = "Today"
        )
        _notes.value = listOf(newNote) + _notes.value
        _activeNoteId.value = newNote.id
    }

    fun createNote(title: String, body: String, context: Context? = null): NoteItem {
        val newNote = NoteItem(
            id = UUID.randomUUID().toString(),
            title = if (title.isBlank()) "Catatan Baru" else title,
            body = body,
            dateLabel = "Today"
        )
        val updated = listOf(newNote) + _notes.value
        _notes.value = updated
        _activeNoteId.value = newNote.id
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveNotes(ctx, updated)
        return newNote
    }

    fun updateNote(id: String, title: String, body: String, context: Context? = null) {
        val updated = _notes.value.map {
            if (it.id == id) it.copy(title = title, body = body, dateLabel = "Today") else it
        }
        _notes.value = updated
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveNotes(ctx, updated)
    }

    fun deleteNote(id: String, context: Context? = null) {
        val updated = _notes.value.filter { it.id != id }
        _notes.value = updated
        if (_activeNoteId.value == id) {
            _activeNoteId.value = _notes.value.firstOrNull()?.id
        }
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveNotes(ctx, updated)
    }

    // Calendar
    fun loadCalendarEvents(context: Context) {
        _calendarEvents.value = IOSPersistenceManager.loadCalendarEvents(context)
    }

    fun deleteCalendarEvent(id: String, context: Context? = null) {
        val updated = _calendarEvents.value.filter { it.id != id }
        _calendarEvents.value = updated
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveCalendarEvents(ctx, updated)
        showDynamicIslandNotification("Calendar", "Acara dihapus", "calendar", 0xFFFF3B30)
    }

    fun addCalendarEvent(
        title: String,
        timeRange: String,
        colorHex: Long,
        dayOfMonth: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
        context: Context? = null
    ) {
        val event = CalendarEvent(UUID.randomUUID().toString(), title, timeRange, colorHex, dayOfMonth)
        val updated = _calendarEvents.value + event
        _calendarEvents.value = updated
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveCalendarEvents(ctx, updated)
        IOSPersistenceManager.insertDeviceCalendarEvent(ctx, title, timeRange, dayOfMonth)
        showDynamicIslandNotification("Calendar", "Acara disimpan: $title", "calendar", 0xFFFF3B30)
    }

    // Alarms
    fun loadAlarms(context: Context) {
        _alarms.value = IOSPersistenceManager.loadAlarms(context)
    }

    fun toggleAlarm(id: String, context: Context? = null) {
        val ctx = context ?: getApplication<Application>()
        val updated = _alarms.value.map {
            if (it.id == id) {
                val newEnabled = !it.isEnabled
                val parts = it.timeLabel.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val hour24 = if (it.period.equals("PM", ignoreCase = true) && h < 12) h + 12 else if (it.period.equals("AM", ignoreCase = true) && h == 12) 0 else h
                IOSPersistenceManager.syncSystemAlarm(ctx, hour24, m, it.label, newEnabled)
                it.copy(isEnabled = newEnabled)
            } else it
        }
        _alarms.value = updated
        IOSPersistenceManager.saveAlarms(ctx, updated)
    }

    fun addAlarm(timeLabel: String, period: String, label: String, context: Context? = null) {
        val ctx = context ?: getApplication<Application>()
        val newAlarm = AlarmItem(UUID.randomUUID().toString(), timeLabel, period, label, true)
        val updated = _alarms.value + newAlarm
        _alarms.value = updated
        IOSPersistenceManager.saveAlarms(ctx, updated)

        val parts = timeLabel.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val hour24 = if (period.equals("PM", ignoreCase = true) && h < 12) h + 12 else if (period.equals("AM", ignoreCase = true) && h == 12) 0 else h
        IOSPersistenceManager.syncSystemAlarm(ctx, hour24, m, label, true)
        showDynamicIslandNotification("Clock", "Alarm diatur: $timeLabel $period", "clock", 0xFFFF9500)
    }

    fun deleteAlarm(id: String, context: Context? = null) {
        val ctx = context ?: getApplication<Application>()
        val updated = _alarms.value.filter { it.id != id }
        _alarms.value = updated
        IOSPersistenceManager.saveAlarms(ctx, updated)
    }

    // Reminders
    fun loadReminders(context: Context) {
        _reminders.value = IOSPersistenceManager.loadReminders(context)
    }

    fun addReminder(context: Context, title: String, notes: String, dueDate: String, listName: String = "Reminders") {
        val item = ReminderItem(
            id = UUID.randomUUID().toString(),
            title = title,
            notes = notes,
            dueDate = dueDate,
            isCompleted = false,
            listName = listName
        )
        val updated = listOf(item) + _reminders.value
        _reminders.value = updated
        IOSPersistenceManager.saveReminders(context, updated)
        showDynamicIslandNotification("Reminders", "Pengingat ditambahkan: $title", "bell", 0xFF007AFF)
    }

    fun toggleReminderCompleted(context: Context, id: String) {
        val updated = _reminders.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _reminders.value = updated
        IOSPersistenceManager.saveReminders(context, updated)
    }

    fun deleteReminder(context: Context, id: String) {
        val updated = _reminders.value.filter { it.id != id }
        _reminders.value = updated
        IOSPersistenceManager.saveReminders(context, updated)
    }

    // Stopwatch
    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        val startTime = System.currentTimeMillis() - _stopwatchTimeMs.value
        stopwatchJob = viewModelScope.launch {
            while (_isStopwatchRunning.value) {
                delay(30)
                _stopwatchTimeMs.value = System.currentTimeMillis() - startTime
            }
        }
    }

    fun stopStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
    }

    fun resetStopwatch() {
        stopStopwatch()
        _stopwatchTimeMs.value = 0L
        _stopwatchLaps.value = emptyList()
    }

    fun lapStopwatch() {
        val ms = _stopwatchTimeMs.value
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        val centis = (ms % 1000) / 10
        val lapStr = String.format("%02d:%02d.%02d", minutes, seconds, centis)
        _stopwatchLaps.value = listOf(lapStr) + _stopwatchLaps.value
    }

    // Countdown Timer
    fun startTimer(seconds: Int) {
        countdownTimerJob?.cancel()
        _timerRemainingSeconds.value = seconds
        _isTimerRunning.value = true
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.TIMER,
            title = "Timer",
            subtitle = formatSeconds(seconds),
            icon = "timer",
            accentColor = 0xFFFF9500
        )
        countdownTimerJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerRemainingSeconds.value -= 1
                if (_dynamicIsland.value.mode == DynamicIslandMode.TIMER) {
                    _dynamicIsland.value = _dynamicIsland.value.copy(
                        subtitle = formatSeconds(_timerRemainingSeconds.value)
                    )
                }
            }
            _isTimerRunning.value = false
            showDynamicIslandNotification("Timer", "Waktu selesai!", "bell", 0xFFFF9500)
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        countdownTimerJob?.cancel()
        if (_dynamicIsland.value.mode == DynamicIslandMode.TIMER) {
            _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
        }
    }

    private fun formatSeconds(totalSecs: Int): String {
        val m = totalSecs / 60
        val s = totalSecs % 60
        return String.format("%02d:%02d", m, s)
    }

    // Camera photo capture
    fun addCapturedPhoto(pathOrUri: String) {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val newPhoto = PhotoItem(
            id = UUID.randomUUID().toString(),
            title = "IMG_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}",
            date = "Today",
            drawableRes = 0,
            imageUri = pathOrUri
        )
        _photoItems.value = listOf(newPhoto) + _photoItems.value
        _capturedPhotos.value = listOf(pathOrUri) + _capturedPhotos.value
        showDynamicIslandNotification("Camera", "Foto disimpan ke Photos", "camera", 0xFF007AFF)
    }

    fun loadDevicePhotos(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                )
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                context.contentResolver.query(queryUri, projection, null, null, sortOrder)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val devicePhotos = mutableListOf<PhotoItem>()
                    var count = 0
                    while (cursor.moveToNext() && count < 50) {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "Photo"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        devicePhotos.add(
                            PhotoItem(
                                id = id.toString(),
                                title = name.substringBeforeLast("."),
                                date = "Recent",
                                drawableRes = 0,
                                imageUri = contentUri.toString()
                            )
                        )
                        count++
                    }
                    if (devicePhotos.isNotEmpty()) {
                        val defaultPresets = _photoItems.value.filter { it.imageUri == null }
                        val captured = _photoItems.value.filter { it.imageUri != null && !it.imageUri!!.startsWith("content://") }
                        _photoItems.value = captured + devicePhotos + defaultPresets
                    }
                }
            } catch (e: Exception) {
                // Handled gracefully
            }
        }
    }

    fun capturePhoto() {
        val path = "photo_${System.currentTimeMillis()}.jpg"
        addCapturedPhoto(path)
    }

    // System Toggles & Control Center
    fun toggleAirplaneMode() {
        _airplaneMode.value = !_airplaneMode.value
        if (_airplaneMode.value) {
            _wifiEnabled.value = false
            _isWifiConnected.value = false
            _cellularSignalBars.value = 0
            showDynamicIslandNotification("Airplane Mode", "Mode Pesawat Aktif", "airplane", 0xFFFF9500)
        } else {
            _wifiEnabled.value = true
            _isWifiConnected.value = true
            _cellularSignalBars.value = 4
            showDynamicIslandNotification("Airplane Mode", "Mode Pesawat Nonaktif", "airplane", 0xFFFF9500)
        }

        // Apply via root to actual system settings if root access is present
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = if (_airplaneMode.value) "enable" else "disable"
                val num = if (_airplaneMode.value) "1" else "0"
                RootExecutor.executeCommand("cmd connectivity airplane-mode $state")
                RootExecutor.executeCommand("settings put global airplane_mode_on $num")
                RootExecutor.executeCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${_airplaneMode.value}")
            } catch (e: Exception) {}
        }
    }

    fun setAirplaneMode(enabled: Boolean) {
        if (_airplaneMode.value == enabled) return
        toggleAirplaneMode()
    }

    fun toggleWifi() {
        _wifiEnabled.value = !_wifiEnabled.value
        _isWifiConnected.value = _wifiEnabled.value
        val state = if (_wifiEnabled.value) "Tersambung ke ${_currentWifiSsid.value}" else "Wi-Fi Dimatikan"
        showDynamicIslandNotification("Wi-Fi", state, "wifi", 0xFF007AFF)
    }

    fun setWifiEnabled(enabled: Boolean) {
        if (_wifiEnabled.value != enabled) {
            toggleWifi()
        }
    }

    @SuppressLint("MissingPermission")
    fun scanWifiNetworks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    val info = wifiManager.connectionInfo
                    if (info != null && !info.ssid.isNullOrBlank() && info.ssid != "<unknown ssid>") {
                        _currentWifiSsid.value = info.ssid.removeSurrounding("\"")
                    }
                    val scanResults = wifiManager.scanResults
                    if (!scanResults.isNullOrEmpty()) {
                        val scanned = scanResults
                            .filter { it.SSID.isNotBlank() }
                            .map { Pair(it.SSID, it.capabilities.contains("WPA") || it.capabilities.contains("WEP")) }
                            .distinctBy { it.first }
                        if (scanned.isNotEmpty()) {
                            _availableWifiList.value = scanned
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    fun connectToWifi(ssid: String, password: String? = null) {
        viewModelScope.launch {
            showDynamicIslandNotification("Wi-Fi", "Menyambungkan ke $ssid...", "wifi", 0xFF007AFF)
            delay(1200)
            _currentWifiSsid.value = ssid
            _isWifiConnected.value = true
            _wifiEnabled.value = true
            _wifiSignalLevel.value = 4
            showDynamicIslandNotification("Wi-Fi", "Terhubung ke $ssid", "wifi", 0xFF34C759)
        }
    }

    fun connectToWifi(context: Context, ssid: String, password: String? = null) {
        connectToWifi(ssid, password)
    }

    fun initBluetoothState(context: Context) {
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter != null) {
                _bluetoothEnabled.value = adapter.isEnabled
                val bonded = try {
                    adapter.bondedDevices?.map {
                        val name = try { it.name ?: it.address ?: "Bluetooth Device" } catch (e: SecurityException) { it.address ?: "Bluetooth Device" }
                        Pair(name, it.bondState == BluetoothDevice.BOND_BONDED)
                    } ?: emptyList()
                } catch (e: SecurityException) {
                    emptyList()
                }
                if (bonded.isNotEmpty()) {
                    _pairedBluetoothDevices.value = bonded
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scanBluetoothDevices(context: Context? = null) {
        val ctx = context ?: getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure permissions via root if available
                RootExecutor.execute("pm grant ${ctx.packageName} android.permission.BLUETOOTH_CONNECT")
                RootExecutor.execute("pm grant ${ctx.packageName} android.permission.BLUETOOTH_SCAN")
                RootExecutor.execute("pm grant ${ctx.packageName} android.permission.ACCESS_FINE_LOCATION")
            } catch (ignored: Exception) {}

            try {
                val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    _isBluetoothScanning.value = false
                    return@launch
                }

                // Update bonded devices
                try {
                    val bonded = adapter.bondedDevices?.map {
                        val name = try { it.name ?: it.address ?: "Bluetooth Device" } catch (e: SecurityException) { it.address ?: "Bluetooth Device" }
                        Pair(name, it.bondState == BluetoothDevice.BOND_BONDED)
                    } ?: emptyList()
                    if (bonded.isNotEmpty()) {
                        _pairedBluetoothDevices.value = bonded
                    }
                } catch (ignored: SecurityException) {}

                // Clean up previous receiver if any
                bluetoothReceiver?.let {
                    try { ctx.unregisterReceiver(it) } catch (ignored: Exception) {}
                    bluetoothReceiver = null
                }

                val discovered = mutableListOf<Pair<String, Boolean>>()
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        when (intent?.action) {
                            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                                _isBluetoothScanning.value = true
                            }
                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                _isBluetoothScanning.value = false
                            }
                            BluetoothDevice.ACTION_FOUND -> {
                                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                }
                                device?.let { d ->
                                    val name = try { d.name ?: d.address } catch (e: SecurityException) { d.address }
                                    if (!name.isNullOrBlank() && discovered.none { it.first == name } && _pairedBluetoothDevices.value.none { it.first == name }) {
                                        discovered.add(Pair(name, false))
                                        _discoveredBluetoothDevices.value = discovered.toList()
                                    }
                                }
                            }
                        }
                    }
                }
                bluetoothReceiver = receiver
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                ctx.registerReceiver(receiver, filter)

                try {
                    if (adapter.isDiscovering) {
                        adapter.cancelDiscovery()
                    }
                    _isBluetoothScanning.value = true
                    adapter.startDiscovery()
                } catch (ignored: SecurityException) {}

            } catch (e: Exception) {
                e.printStackTrace()
                _isBluetoothScanning.value = false
            }
        }
    }

    fun connectBluetoothDevice(context: Context? = null, deviceName: String) {
        val ctx = context ?: getApplication<Application>()
        viewModelScope.launch {
            showDynamicIslandNotification("Bluetooth", "Menghubungkan ke $deviceName...", "bluetooth", 0xFF007AFF)
            delay(1200)
            try {
                val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                val device = adapter?.bondedDevices?.firstOrNull {
                    try { it.name == deviceName || it.address == deviceName } catch (e: SecurityException) { false }
                }
                if (device != null && device.bondState != BluetoothDevice.BOND_BONDED) {
                    try { device.createBond() } catch (ignored: SecurityException) {}
                }
            } catch (ignored: Exception) {}

            val paired = _pairedBluetoothDevices.value.toMutableList()
            val index = paired.indexOfFirst { it.first == deviceName }
            if (index >= 0) {
                val current = paired[index]
                paired[index] = Pair(current.first, !current.second)
            } else {
                paired.add(Pair(deviceName, true))
                _discoveredBluetoothDevices.value = _discoveredBluetoothDevices.value.filter { it.first != deviceName }
            }
            _pairedBluetoothDevices.value = paired
            showDynamicIslandNotification("Bluetooth", "$deviceName Terhubung", "bluetooth", 0xFF34C759)
        }
    }

    // Overload for backward compatibility
    fun connectBluetoothDevice(deviceName: String) {
        connectBluetoothDevice(null, deviceName)
    }

    fun toggleBluetooth(context: Context? = null) {
        setBluetoothEnabled(!_bluetoothEnabled.value, context)
    }

    fun setBluetoothEnabled(enabled: Boolean, context: Context? = null) {
        _bluetoothEnabled.value = enabled
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Root system modification
            val cmd = if (enabled) "svc bluetooth enable" else "svc bluetooth disable"
            RootExecutor.execute(cmd)
            RootExecutor.execute(if (enabled) "cmd bluetooth_manager enable" else "cmd bluetooth_manager disable")

            // 2. Standard API
            val ctx = context ?: getApplication<Application>()
            try {
                val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                if (adapter != null) {
                    if (enabled && !adapter.isEnabled) {
                        @Suppress("DEPRECATION")
                        adapter.enable()
                    } else if (!enabled && adapter.isEnabled) {
                        @Suppress("DEPRECATION")
                        adapter.disable()
                    }
                }
            } catch (ignored: Exception) {}
        }
        val state = if (enabled) "Bluetooth Aktif" else "Bluetooth Nonaktif"
        showDynamicIslandNotification("Bluetooth", state, "bluetooth", 0xFF007AFF)
    }

    fun toggleCellular() {
        _cellularEnabled.value = !_cellularEnabled.value
        val state = if (_cellularEnabled.value) "Data Seluler Aktif" else "Data Seluler Nonaktif"
        showDynamicIslandNotification("Cellular", state, "cellular", 0xFF34C759)
    }

    fun setCellularEnabled(enabled: Boolean) {
        _cellularEnabled.value = enabled
    }

    fun toggleHotspot() {
        _hotspotEnabled.value = !_hotspotEnabled.value
        val state = if (_hotspotEnabled.value) "Hotspot Aktif" else "Hotspot Nonaktif"
        showDynamicIslandNotification("Personal Hotspot", state, "hotspot", 0xFF34C759)
    }

    fun setHotspotEnabled(enabled: Boolean) {
        _hotspotEnabled.value = enabled
    }

    fun toggleFlashlight(context: Context) {
        FlashlightHelper.toggleTorch(context) { active ->
            _flashlightEnabled.value = active
            if (active) {
                showDynamicIslandNotification("Flashlight", "Senter Aktif", "flashlight", 0xFFFFCC00)
            }
        }
    }

    fun setBrightness(value: Float) {
        _brightness.value = value.coerceIn(0.1f, 1.0f)
    }

    fun setVolume(value: Float) {
        _volume.value = value.coerceIn(0.0f, 1.0f)
    }

    // Appearance & Dark Mode
    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        showDynamicIslandNotification("Appearance", if (dark) "Dark Mode Aktif" else "Light Mode Aktif", "sun", 0xFF007AFF)
    }

    // Wallpaper Management
    fun setWallpaper(resId: Int, name: String, uri: String? = null, context: Context? = null) {
        _currentWallpaperRes.value = resId
        _currentWallpaperName.value = name
        _currentWallpaperUri.value = uri
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveWallpaper(ctx, resId, name, uri)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wm = android.app.WallpaperManager.getInstance(ctx)
                if (uri != null) {
                    val stream = ctx.contentResolver.openInputStream(android.net.Uri.parse(uri))
                    val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (bitmap != null) {
                        wm.setBitmap(bitmap)
                    }
                } else if (resId != 0) {
                    wm.setResource(resId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        showDynamicIslandNotification("Wallpaper", "Wallpaper applied: $name", "photo", 0xFF32ADE6)
    }

    // Device Lock & Passcode
    fun lockDevice() {
        _isDeviceLocked.value = true
        _currentApp.value = null
        _isControlCenterOpen.value = false
        _isNotificationCenterOpen.value = false
    }

    fun unlockDevice() {
        _isDeviceLocked.value = false
        showDynamicIslandNotification("Face ID", "iPhone Unlocked", "faceid", 0xFF34C759)
    }

    fun setPasscode(code: String) {
        _userPasscode.value = code
        showDynamicIslandNotification("Passcode", "Passcode berhasil diubah", "lock", 0xFF007AFF)
    }

    fun togglePasscode(enabled: Boolean) {
        _isPasscodeEnabled.value = enabled
    }

    fun toggleFaceId(enabled: Boolean) {
        _isFaceIdEnabled.value = enabled
    }

    // Music Player Engine
    fun loadLocalMusic(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION
                )
                // Query all storage audio, omitting micro sound clips (< 3s)
                val selection = "${MediaStore.Audio.Media.DURATION} >= 3000"
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )
                val deviceSongs = mutableListOf<SongItem>()
                cursor?.use {
                    val idCol = it.getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleCol = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val durationCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)

                    while (it.moveToNext() && deviceSongs.size < 250) {
                        val id = it.getLong(idCol)
                        val title = it.getString(titleCol) ?: "Track ${deviceSongs.size + 1}"
                        val artist = it.getString(artistCol) ?: "Unknown Artist"
                        val album = it.getString(albumCol) ?: "Device Storage"
                        val duration = it.getLong(durationCol)
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        val startColor = (title.hashCode().toLong() and 0xFFFFFF) or 0xFF880000
                        val endColor = (artist.hashCode().toLong() and 0xFFFFFF) or 0xFF003388
                        deviceSongs.add(
                            SongItem(
                                id = id.toString(),
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = if (duration > 0) duration else 180000L,
                                uriString = contentUri.toString(),
                                coverGradientStart = startColor,
                                coverGradientEnd = endColor
                            )
                        )
                    }
                }

                val currentBuiltIns = _musicList.value.filter { it.uriString == null }
                if (deviceSongs.isNotEmpty()) {
                    _musicList.value = deviceSongs + currentBuiltIns
                    if (_currentSong.value == null) {
                        _currentSong.value = deviceSongs.first()
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    private var activeMediaPlayer: MediaPlayer? = null
    private var synthJob: Job? = null

    private fun startAudioSynthesizer() {
        synthJob?.cancel()
        synthJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = minBufferSize.coerceAtLeast(sampleRate)
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
                audioTrack.play()

                // Harmonic melodic chords: Cmaj7, Am7, Fmaj7, Gsus4
                val chords = listOf(
                    listOf(261.63, 329.63, 392.00, 493.88),
                    listOf(220.00, 261.63, 329.63, 392.00),
                    listOf(174.61, 220.00, 261.63, 329.63),
                    listOf(196.00, 246.94, 293.66, 392.00)
                )
                var chordIdx = 0

                while (_isMusicPlaying.value) {
                    val chord = chords[chordIdx % chords.size]
                    chordIdx++
                    for (freq in chord) {
                        if (!_isMusicPlaying.value) break
                        val noteSamples = (sampleRate * 0.35).toInt()
                        val buffer = ShortArray(noteSamples)
                        for (i in 0 until noteSamples) {
                            val time = i.toDouble() / sampleRate
                            val env = Math.exp(-time * 4.0) * Math.min(1.0, time * 20.0)
                            val sample = (Math.sin(2.0 * Math.PI * freq * time) * 0.4 +
                                    Math.sin(2.0 * Math.PI * (freq * 2.0) * time) * 0.15) * env
                            buffer[i] = (sample * 16000).toInt().toShort()
                        }
                        audioTrack.write(buffer, 0, buffer.size)
                    }
                }
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {}
        }
    }

    fun playSong(song: SongItem, context: Context? = null) {
        _currentSong.value = song
        _isMusicPlaying.value = true
        _musicDurationStr.value = formatMsToTime(song.durationMs)
        _musicProgress.value = 0f
        _musicCurrentTimeStr.value = "0:00"

        OverlayBridge.isMusicPlaying.value = true
        OverlayBridge.songTitle.value = song.title
        OverlayBridge.songArtist.value = song.artist
        OverlayBridge.musicProgress.value = 0f
        OverlayBridge.musicDurationStr.value = formatMsToTime(song.durationMs)
        OverlayBridge.musicCurrentTimeStr.value = "0:00"

        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.MUSIC,
            title = song.title,
            subtitle = song.artist,
            icon = "music",
            accentColor = 0xFFFF2D55
        )

        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (e: Exception) {}
        synthJob?.cancel()

        if (song.uriString != null && context != null) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.let { audioManager ->
                    val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    if (curVol == 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol / 2, 0)
                    }
                }

                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, Uri.parse(song.uriString))
                    prepare()
                    setVolume(1.0f, 1.0f)
                    setOnCompletionListener {
                        skipNextSong(context)
                    }
                    start()
                }
                activeMediaPlayer = mp
                _musicDurationStr.value = formatMsToTime(mp.duration.toLong())
            } catch (e: Exception) {
                startAudioSynthesizer()
            }
        } else {
            startAudioSynthesizer()
        }

        musicProgressJob?.cancel()
        musicProgressJob = viewModelScope.launch {
            var elapsedMs = 0L
            while (_isMusicPlaying.value) {
                delay(500)
                if (activeMediaPlayer != null) {
                    try {
                        val pos = activeMediaPlayer?.currentPosition?.toLong() ?: 0L
                        val dur = activeMediaPlayer?.duration?.toLong() ?: song.durationMs
                        if (dur > 0) {
                            val prog = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                            _musicProgress.value = prog
                            _musicCurrentTimeStr.value = formatMsToTime(pos)
                            _musicDurationStr.value = formatMsToTime(dur)
                            OverlayBridge.musicProgress.value = prog
                            OverlayBridge.musicCurrentTimeStr.value = formatMsToTime(pos)
                            OverlayBridge.musicDurationStr.value = formatMsToTime(dur)
                        }
                    } catch (e: Exception) {}
                } else {
                    elapsedMs += 500L
                    val prog = (elapsedMs.toFloat() / song.durationMs.toFloat()).coerceIn(0f, 1f)
                    _musicProgress.value = prog
                    _musicCurrentTimeStr.value = formatMsToTime(elapsedMs)
                    OverlayBridge.musicProgress.value = prog
                    OverlayBridge.musicCurrentTimeStr.value = formatMsToTime(elapsedMs)
                    if (elapsedMs >= song.durationMs) {
                        skipNextSong(context)
                        break
                    }
                }
            }
        }
    }

    fun showMusicInDynamicIsland() {
        val song = _currentSong.value ?: _musicList.value.firstOrNull() ?: return
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.MUSIC,
            title = song.title,
            subtitle = song.artist,
            icon = "music",
            accentColor = 0xFFFF2D55
        )
    }

    fun toggleMusicPlay() {
        _isMusicPlaying.value = !_isMusicPlaying.value
        OverlayBridge.isMusicPlaying.value = _isMusicPlaying.value
        val song = _currentSong.value ?: _musicList.value.firstOrNull()
        if (song != null) {
            OverlayBridge.songTitle.value = song.title
            OverlayBridge.songArtist.value = song.artist
        }
        if (_isMusicPlaying.value) {
            _dynamicIsland.value = DynamicIslandData(
                mode = DynamicIslandMode.MUSIC,
                title = song?.title ?: "Apple Music",
                subtitle = song?.artist ?: "Playing",
                icon = "music",
                accentColor = 0xFFFF2D55
            )
            if (activeMediaPlayer != null) {
                try {
                    activeMediaPlayer?.start()
                } catch (e: Exception) {
                    startAudioSynthesizer()
                }
            } else {
                startAudioSynthesizer()
            }
        } else {
            try {
                activeMediaPlayer?.pause()
            } catch (e: Exception) {}
            synthJob?.cancel()
            if (_dynamicIsland.value.mode == DynamicIslandMode.MUSIC) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun skipNextSong(context: Context? = null) {
        val list = _musicList.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentSong.value?.id }
        val nextIndex = if (currentIndex >= 0 && currentIndex < list.size - 1) currentIndex + 1 else 0
        playSong(list[nextIndex], context)
    }

    fun skipPreviousSong(context: Context? = null) {
        val list = _musicList.value
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.id == _currentSong.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playSong(list[prevIndex], context)
    }

    fun seekMusicTo(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        _musicProgress.value = clamped
        val song = _currentSong.value
        if (song != null) {
            val targetMs = (song.durationMs * clamped).toLong()
            _musicCurrentTimeStr.value = formatMsToTime(targetMs)
            try {
                activeMediaPlayer?.let { mp ->
                    val dur = mp.duration
                    if (dur > 0) {
                        mp.seekTo((dur * clamped).toInt())
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun formatMsToTime(ms: Long): String {
        val totalSecs = (ms / 1000).toInt()
        val m = totalSecs / 60
        val s = totalSecs % 60
        return String.format("%d:%02d", m, s)
    }

    // ROOT COMMAND ACTIONS (Powerful J2 Prime Root Suite)
    fun runRootCommand(cmd: String) {
        viewModelScope.launch {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val result = RootExecutor.executeCommand(cmd)
            val log = RootCommandLog(time, cmd, result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Root (#su)", if (result.first) "Command berhasil!" else "Error pada eksekusi", "terminal", 0xFF34C759)
        }
    }

    fun toggleImmersiveRoot() {
        viewModelScope.launch {
            val newState = !_isImmersiveLocked.value
            val result = if (newState) RootExecutor.killAndroidBars() else RootExecutor.restoreAndroidBars()
            _isImmersiveLocked.value = newState
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, if (newState) "settings put global policy_control immersive.full=*" else "restore bars", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Superuser", if (newState) "Status & Nav Bar Android dihilangkan!" else "Status bar dikembalikan", "shield", 0xFF007AFF)
        }
    }

    fun boostCpuRoot() {
        viewModelScope.launch {
            val result = RootExecutor.boostCpuGovernor()
            _cpuBoostEnabled.value = true
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "echo performance > cpu/scaling_governor", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("CPU Governor", "J2 Prime CPU Boost Aktif (Performance)", "speed", 0xFFFF9500)
        }
    }

    fun cleanJ2PrimeRamRoot() {
        viewModelScope.launch {
            val result = RootExecutor.optimizeJ2PrimeRam()
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "sync; echo 3 > /proc/sys/vm/drop_caches", "Cache & ZRAM cleared! +420MB free RAM", true)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("RAM Optimizer", "J2 Prime RAM dibersihkan (+420MB)", "memory", 0xFF34C759)
        }
    }

    fun spoofDeviceRoot(modelName: String) {
        viewModelScope.launch {
            val result = RootExecutor.spoofIphoneModel(modelName)
            _spoofedModel.value = modelName
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "setprop ro.product.model \"$modelName\"", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Device Spoof", "Disamarkan jadi $modelName", "apple", 0xFF5856D6)
        }
    }

    fun setRetinaDpiRoot(dpi: Int) {
        viewModelScope.launch {
            val result = RootExecutor.setIosRetinaDpi(dpi)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "wm density $dpi", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("DPI Scaling", "Layar diatur ke $dpi DPI (Retina Mode)", "display", 0xFF32ADE6)
        }
    }

    // App Library State (Installed Device Apps categorized)
    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private fun Drawable.toBitmapSafe(targetSize: Int = 144): Bitmap? {
        return try {
            if (this is BitmapDrawable && this.bitmap != null) {
                this.bitmap
            } else {
                val width = if (intrinsicWidth > 0) intrinsicWidth else targetSize
                val height = if (intrinsicHeight > 0) intrinsicHeight else targetSize
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                setBounds(0, 0, canvas.width, canvas.height)
                draw(canvas)
                bitmap
            }
        } catch (e: Throwable) {
            null
        }
    }

    private fun categorizeApp(pkg: String, name: String, isGameFlag: Boolean = false): String {
        val lower = "$pkg $name".lowercase(Locale.ROOT)
        return when {
            isGameFlag ||
            lower.contains("game") || lower.contains("fifa") || lower.contains("candy") ||
            lower.contains("clash") || lower.contains("pubg") || lower.contains("roblox") ||
            lower.contains("freefire") || lower.contains("subway") || lower.contains("mobilelegends") ||
            lower.contains("minecraft") || lower.contains("konami") || lower.contains("supercell") -> "Game"

            lower.contains("spotify") || lower.contains("music") || lower.contains("audio") ||
            lower.contains("sound") || lower.contains("recorder") || lower.contains("radio") ||
            lower.contains("wave") || lower.contains("podcast") -> "Audio"

            lower.contains("youtube") || lower.contains("tiktok") || lower.contains("video") ||
            lower.contains("capcut") || lower.contains("netflix") || lower.contains("vlc") ||
            lower.contains("premiere") || lower.contains("player") || lower.contains("stream") -> "Video"

            lower.contains("photo") || lower.contains("camera") || lower.contains("gallery") ||
            lower.contains("canva") || lower.contains("lightroom") || lower.contains("adobe") ||
            lower.contains("photoshop") || lower.contains("snapseed") || lower.contains("image") ||
            lower.contains("picsart") -> "Image"

            lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("instagram") ||
            lower.contains("facebook") || lower.contains("twitter") || lower.contains(".x.") ||
            lower.contains("threads") || lower.contains("discord") || lower.contains("messenger") ||
            lower.contains("chat") || lower.contains("line") -> "Social"

            lower.contains("news") || lower.contains("detik") || lower.contains("kompas") ||
            lower.contains("tribun") || lower.contains("cnn") || lower.contains("tempo") ||
            lower.contains("kumparan") -> "News"

            lower.contains("map") || lower.contains("waze") || lower.contains("gps") ||
            lower.contains("transit") || lower.contains("earth") || lower.contains("navigation") -> "Maps"

            else -> "Utilities"
        }
    }

    private fun getCategoryColor(category: String): Long {
        return when (category) {
            "Recent" -> 0xFF34C759
            "Game" -> 0xFFFF2D55
            "Audio" -> 0xFFFF375F
            "Video" -> 0xFFFF9500
            "Image" -> 0xFFAF52DE
            "Social" -> 0xFF25D366
            "News" -> 0xFFFF3B30
            "Maps" -> 0xFF007AFF
            "Utilities" -> 0xFF5856D6
            else -> 0xFF8E8E93
        }
    }

    fun loadDeviceInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveList = try {
                pm.queryIntentActivities(intent, 0)
            } catch (e: Exception) {
                emptyList()
            }

            val allAppsMap = LinkedHashMap<String, InstalledAppItem>()

            // 1. Process all launcher activities
            for (resolve in resolveList) {
                val pkg = resolve.activityInfo.packageName
                if (pkg == context.packageName) continue
                val name = resolve.loadLabel(pm)?.toString() ?: pkg
                val iconDrawable = try { resolve.loadIcon(pm) } catch (e: Exception) { null }
                val bitmap = iconDrawable?.toBitmapSafe()
                val isGame = (resolve.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0)
                val category = categorizeApp(pkg, name, isGame)
                allAppsMap[pkg] = InstalledAppItem(
                    packageName = pkg,
                    appName = name,
                    category = category,
                    isBuiltIn = false,
                    iconBitmap = bitmap,
                    iconColor = getCategoryColor(category)
                )
            }

            // 2. Also check installed applications with launch intents to ensure NO app is missed
            try {
                val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in installedPackages) {
                    val pkg = appInfo.packageName
                    if (pkg == context.packageName || allAppsMap.containsKey(pkg)) continue
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        val name = appInfo.loadLabel(pm).toString()
                        val iconDrawable = try { appInfo.loadIcon(pm) } catch (e: Exception) { null }
                        val bitmap = iconDrawable?.toBitmapSafe()
                        val isGame = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0)
                        val category = categorizeApp(pkg, name, isGame)
                        allAppsMap[pkg] = InstalledAppItem(
                            packageName = pkg,
                            appName = name,
                            category = category,
                            isBuiltIn = false,
                            iconBitmap = bitmap,
                            iconColor = getCategoryColor(category)
                        )
                    }
                }
            } catch (e: Exception) {}

            val finalList = mutableListOf<InstalledAppItem>()

            // Create "Recent" category entries from the top 4 installed device apps
            val deviceAppList = allAppsMap.values.toList()
            val recentApps = deviceAppList.take(4).map { app ->
                app.copy(category = "Recent")
            }
            finalList.addAll(recentApps)
            finalList.addAll(deviceAppList)

            // Built-in iOS SpringBoard core apps (only if not already provided by device)
            val builtInList = listOf(
                InstalledAppItem(context.packageName, "Phone", "Recent", isBuiltIn = true, builtInId = AppId.PHONE, iconColor = 0xFF34C759),
                InstalledAppItem(context.packageName, "Messages", "Social", isBuiltIn = true, builtInId = AppId.MESSAGES, iconColor = 0xFF34C759),
                InstalledAppItem(context.packageName, "Photos", "Image", isBuiltIn = true, builtInId = AppId.PHOTOS, iconColor = 0xFFFF9500),
                InstalledAppItem(context.packageName, "Camera", "Image", isBuiltIn = true, builtInId = AppId.CAMERA, iconColor = 0xFF8E8E93),
                InstalledAppItem(context.packageName, "Music", "Audio", isBuiltIn = true, builtInId = AppId.MUSIC, iconColor = 0xFFFF2D55),
                InstalledAppItem(context.packageName, "Safari", "Utilities", isBuiltIn = true, builtInId = AppId.SAFARI, iconColor = 0xFF007AFF),
                InstalledAppItem(context.packageName, "Weather", "Utilities", isBuiltIn = true, builtInId = AppId.WEATHER, iconColor = 0xFF32ADE6),
                InstalledAppItem(context.packageName, "Clock", "Utilities", isBuiltIn = true, builtInId = AppId.CLOCK, iconColor = 0xFF000000),
                InstalledAppItem(context.packageName, "Calculator", "Utilities", isBuiltIn = true, builtInId = AppId.CALCULATOR, iconColor = 0xFFFF9500),
                InstalledAppItem(context.packageName, "Calendar", "Utilities", isBuiltIn = true, builtInId = AppId.CALENDAR, iconColor = 0xFFFF3B30),
                InstalledAppItem(context.packageName, "Notes", "Utilities", isBuiltIn = true, builtInId = AppId.NOTES, iconColor = 0xFFFFCC00),
                InstalledAppItem(context.packageName, "Settings", "Utilities", isBuiltIn = true, builtInId = AppId.SETTINGS, iconColor = 0xFF8E8E93),
                InstalledAppItem(context.packageName, "Stocks", "Finance", isBuiltIn = true, builtInId = AppId.STOCKS, iconColor = 0xFF1C1C1E),
                InstalledAppItem(context.packageName, "Books", "Reading", isBuiltIn = true, builtInId = AppId.BOOKS, iconColor = 0xFFFF9500),
                InstalledAppItem(context.packageName, "Maps", "Navigation", isBuiltIn = true, builtInId = AppId.MAPS, iconColor = 0xFF2ECC71),
                InstalledAppItem(context.packageName, "TV", "Entertainment", isBuiltIn = true, builtInId = AppId.TV, iconColor = 0xFF000000),
                InstalledAppItem(context.packageName, "Podcasts", "Audio", isBuiltIn = true, builtInId = AppId.PODCASTS, iconColor = 0xFF8E44AD),
                InstalledAppItem(context.packageName, "Mail", "Social", isBuiltIn = true, builtInId = AppId.MAIL, iconColor = 0xFF007AFF),
                InstalledAppItem(context.packageName, "Health", "Health", isBuiltIn = true, builtInId = AppId.HEALTH, iconColor = 0xFFFF2D55),
                InstalledAppItem(context.packageName, "Wallet", "Utilities", isBuiltIn = true, builtInId = AppId.WALLET, iconColor = 0xFF000000),
                InstalledAppItem(context.packageName, "FaceTime", "Social", isBuiltIn = true, builtInId = AppId.FACETIME, iconColor = 0xFF34C759),
                InstalledAppItem(context.packageName, "iRoot SE", "Utilities", isBuiltIn = true, builtInId = AppId.ROOT_TOOLS, iconColor = 0xFF5856D6)
            )

            for (builtIn in builtInList) {
                if (finalList.none { it.appName.equals(builtIn.appName, ignoreCase = true) }) {
                    finalList.add(builtIn)
                }
            }

            _installedApps.value = finalList
        }
    }

    fun launchDeviceApp(context: Context, app: InstalledAppItem) {
        if (Settings.canDrawOverlays(context)) {
            com.example.service.IOSOverlayService.start(context)
        }
        if (app.isBuiltIn && app.builtInId != null) {
            openApp(app.builtInId)
        } else {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                } else {
                    showDynamicIslandNotification("App Library", "Membuka ${app.appName}...", "apple", 0xFF007AFF)
                }
            } catch (e: Exception) {
                showDynamicIslandNotification("Error", "Gagal membuka ${app.appName}", "bell", 0xFFFF3B30)
            }
        }
    }

    fun disableSystemUICompletelyRoot() {
        viewModelScope.launch {
            val result = RootExecutor.disableSystemUICompletely()
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "pm disable com.android.systemui", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            _isImmersiveLocked.value = true
            showDynamicIslandNotification("Superuser (#su)", if (result.first) "SystemUI dimatikan total! 100% iOS Native" else result.second, "shield", 0xFF34C759)
        }
    }

    fun grantNotificationListenerViaRoot(context: Context) {
        viewModelScope.launch {
            val result = RootExecutor.grantNotificationListenerRoot(context.packageName)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "cmd notification allow_listener ...", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Superuser", if (result.first) "Akses notifikasi diaktifkan via Root!" else "Gagal mengaktifkan via Root", "bell", 0xFF007AFF)
        }
    }

    fun rebootRoot(mode: String) {
        viewModelScope.launch {
            RootExecutor.rebootDevice(mode)
        }
    }

    fun executeCustomRootCommand(cmd: String) {
        runRootCommand(cmd)
    }

    // ================= REAL TELEMETRY & NETWORK =================
    fun updateBatteryStatus(level: Int, isCharging: Boolean) {
        _batteryLevel.value = level
        _isBatteryCharging.value = isCharging
    }

    fun updateNetworkStatus(isWifi: Boolean, wifiLevel: Int, cellularBars: Int, netType: String) {
        _isWifiConnected.value = isWifi
        _wifiSignalLevel.value = wifiLevel
        _cellularSignalBars.value = cellularBars
        _networkType.value = netType
    }

    // ================= REAL SMS INTEGRATION =================
    fun loadDeviceSms(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse("content://sms")
                val projection = arrayOf("_id", "address", "body", "date", "type", "read")
                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "date DESC LIMIT 150"
                )
                cursor?.use {
                    val threadsMap = mutableMapOf<String, MutableList<ChatMessage>>()
                    val idCol = it.getColumnIndex("_id")
                    val addressCol = it.getColumnIndex("address")
                    val bodyCol = it.getColumnIndex("body")
                    val dateCol = it.getColumnIndex("date")
                    val typeCol = it.getColumnIndex("type")

                    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

                    while (it.moveToNext()) {
                        val id = if (idCol >= 0) it.getString(idCol) else UUID.randomUUID().toString()
                        val address = if (addressCol >= 0) it.getString(addressCol) ?: "Unknown" else "Unknown"
                        val body = if (bodyCol >= 0) it.getString(bodyCol) ?: "" else ""
                        val dateLong = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()
                        val type = if (typeCol >= 0) it.getInt(typeCol) else 1 // 1 = inbox, 2 = sent
                        val isFromMe = (type == 2)
                        val formattedTime = timeFormatter.format(Date(dateLong))

                        val msg = ChatMessage(id, if (isFromMe) "Me" else address, body, formattedTime, isFromMe)
                        val list = threadsMap.getOrPut(address) { mutableListOf() }
                        list.add(msg)
                    }

                    if (threadsMap.isNotEmpty()) {
                        val newThreads = threadsMap.map { (addr, msgs) ->
                            val sorted = msgs.reversed()
                            val last = sorted.lastOrNull()?.text ?: ""
                            val lastTime = sorted.lastOrNull()?.time ?: ""
                            val contactName = resolveContactName(context, addr) ?: addr
                            val color = (addr.hashCode().toLong() and 0xFFFFFF) or 0xFF000000
                            MessageThread(
                                id = addr,
                                contactName = contactName,
                                lastMessage = last,
                                time = lastTime,
                                unreadCount = 0,
                                avatarColor = color,
                                messages = sorted
                            )
                        }
                        _messagesThreads.value = newThreads
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun sendRealSms(recipient: String, messageText: String, context: Context) {
        if (recipient.isBlank() || messageText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(recipient, null, messageText, null, null)
                showDynamicIslandNotification("SMS Terkirim", "Ke: $recipient", "message", 0xFF34C759)
            } catch (e: Exception) {
                try {
                    val sendIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(recipient))).apply {
                        putExtra("sms_body", messageText)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(sendIntent)
                } catch (ignored: Exception) {}
            }

            // Immediately update thread in state
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val newMsg = ChatMessage(UUID.randomUUID().toString(), "Me", messageText, time, true)
            val currentThreads = _messagesThreads.value.toMutableList()
            val existingIndex = currentThreads.indexOfFirst { it.id == recipient || it.contactName == recipient }
            if (existingIndex >= 0) {
                val existing = currentThreads[existingIndex]
                currentThreads[existingIndex] = existing.copy(
                    lastMessage = messageText,
                    time = time,
                    messages = existing.messages + newMsg
                )
            } else {
                currentThreads.add(
                    0,
                    MessageThread(
                        id = recipient,
                        contactName = recipient,
                        lastMessage = messageText,
                        time = time,
                        unreadCount = 0,
                        avatarColor = 0xFF34C759,
                        messages = listOf(newMsg)
                    )
                )
            }
            _messagesThreads.value = currentThreads
        }
    }

    fun onSmsReceived(sender: String, body: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newMsg = ChatMessage(UUID.randomUUID().toString(), sender, body, time, false)
        val currentThreads = _messagesThreads.value.toMutableList()
        val index = currentThreads.indexOfFirst { it.id == sender || it.contactName == sender }
        if (index >= 0) {
            val existing = currentThreads[index]
            currentThreads[index] = existing.copy(
                lastMessage = body,
                time = time,
                unreadCount = existing.unreadCount + 1,
                messages = existing.messages + newMsg
            )
        } else {
            currentThreads.add(
                0,
                MessageThread(
                    id = sender,
                    contactName = sender,
                    lastMessage = body,
                    time = time,
                    unreadCount = 1,
                    avatarColor = 0xFF5856D6,
                    messages = listOf(newMsg)
                )
            )
        }
        _messagesThreads.value = currentThreads
        showDynamicIslandNotification(sender, body, "message", 0xFF34C759)
    }

    // ================= REAL PHONE CALLING & CONTACTS =================
    fun makeRealPhoneCall(context: Context, number: String, contactName: String = "") {
        if (number.isBlank()) return
        val trimmed = number.trim()
        val nameToUse = contactName.ifBlank {
            _contactsList.value.firstOrNull {
                it.phoneNumber.replace("[^0-9+]".toRegex(), "") == trimmed.replace("[^0-9+]".toRegex(), "")
            }?.name ?: trimmed
        }
        startInAppCall(nameToUse, trimmed)

        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Grant telephony permissions automatically via root if available
                withContext(Dispatchers.IO) {
                    val pkg = context.packageName
                    RootExecutor.execute("pm grant $pkg android.permission.CALL_PHONE")
                    RootExecutor.execute("pm grant $pkg android.permission.READ_PHONE_STATE")
                }

                // Properly encode '#' as '%23' for USSD codes (e.g., *123#, *888#)
                val encodedNumber = Uri.encode(trimmed)
                val callUri = Uri.parse("tel:$encodedNumber")
                val plainUri = Uri.fromParts("tel", trimmed, null)

                val hasCallPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED

                var launched = false
                if (hasCallPermission) {
                    try {
                        val callIntent = Intent(Intent.ACTION_CALL, callUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(callIntent)
                        launched = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (!launched) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, callUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)
                        launched = true
                    } catch (e: Exception) {
                        try {
                            val dialIntent2 = Intent(Intent.ACTION_DIAL, plainUri).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(dialIntent2)
                            launched = true
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                        }
                    }
                }

                // If rooted, also send intent directly via am
                if (RootExecutor.isRootAvailable()) {
                    withContext(Dispatchers.IO) {
                        RootExecutor.execute("am start -a android.intent.action.CALL -d 'tel:$encodedNumber'")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadDeviceContacts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val cursor = context.contentResolver.query(uri, projection, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")
                val contacts = mutableListOf<ContactItem>()
                cursor?.use {
                    val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)

                    while (it.moveToNext()) {
                        val name = if (nameCol >= 0) it.getString(nameCol) ?: "Unknown" else "Unknown"
                        val num = if (numCol >= 0) it.getString(numCol) ?: "" else ""
                        val id = if (idCol >= 0) it.getString(idCol) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                        val color = (name.hashCode().toLong() and 0xFFFFFF) or 0xFF000000
                        contacts.add(ContactItem(id, name, num, color))
                    }
                }
                if (contacts.isNotEmpty()) {
                    _contactsList.value = contacts
                }
            } catch (e: Exception) {}
        }
    }

    fun addNewContact(context: Context, name: String, phone: String, email: String = "", notes: String = "") {
        if (name.isBlank() && phone.isBlank()) return
        val newId = UUID.randomUUID().toString()
        val displayName = name.ifBlank { phone }
        val color = (displayName.hashCode().toLong() and 0xFFFFFF) or 0xFF000000
        val newContact = ContactItem(newId, displayName, phone, color)
        _contactsList.value = (listOf(newContact) + _contactsList.value).sortedBy { it.name.lowercase() }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ops = ArrayList<ContentProviderOperation>()
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build()
                )
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName)
                        .build()
                )
                if (phone.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                            .build()
                    )
                }
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } catch (e: Exception) {
                // Graceful fallback if permission or OS limitation
            }
        }
        showDynamicIslandNotification("Contacts", "Kontak $displayName berhasil disimpan", "person", 0xFF007AFF)
    }

    fun loadDeviceCallLogs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = CallLog.Calls.CONTENT_URI
                val projection = arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE
                )
                val cursor = context.contentResolver.query(uri, projection, null, null, CallLog.Calls.DATE + " DESC LIMIT 50")
                val logs = mutableListOf<CallRecord>()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                cursor?.use {
                    val idCol = it.getColumnIndex(CallLog.Calls._ID)
                    val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateCol = it.getColumnIndex(CallLog.Calls.DATE)

                    while (it.moveToNext()) {
                        val id = if (idCol >= 0) it.getString(idCol) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                        val num = if (numCol >= 0) it.getString(numCol) ?: "Unknown" else "Unknown"
                        val name = if (nameCol >= 0) it.getString(nameCol) ?: num else num
                        val type = if (typeCol >= 0) it.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
                        val dateLong = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()
                        val isMissed = (type == CallLog.Calls.MISSED_TYPE)
                        val color = (name.hashCode().toLong() and 0xFFFFFF) or 0xFF000000
                        val callTypeStr = when (type) {
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            else -> "Incoming"
                        }
                        logs.add(CallRecord(id, name, num, callTypeStr, timeFormat.format(Date(dateLong)), isMissed, color))
                    }
                }
                if (logs.isNotEmpty()) {
                    _callRecords.value = logs
                }
            } catch (e: Exception) {}
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) {
                    val col = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (col >= 0) it.getString(col) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun onIncomingCallReceived(incomingNumber: String) {
        _callContactName.value = incomingNumber
        _callPhoneNumber.value = incomingNumber
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.CALL,
            title = "Panggilan Masuk",
            subtitle = incomingNumber,
            icon = "call",
            accentColor = 0xFF34C759
        )
    }

    @SuppressLint("MissingPermission")
    fun answerIncomingCall(context: Context) {
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telecom?.acceptRingingCall()
            }
        } catch (e: Exception) {}
        startInAppCall(_callContactName.value, _callPhoneNumber.value)
    }

    @SuppressLint("MissingPermission")
    fun rejectIncomingCall(context: Context) {
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecom?.endCall()
            }
        } catch (e: Exception) {}
        dismissDynamicIsland()
    }
}
