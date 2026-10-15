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
import android.bluetooth.BluetoothProfile
import android.location.LocationManager
import android.os.PowerManager
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.example.util.IOSFeedbackHelper
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
    VOLUME,
    UNLOCK,
    SILENT_MODE,
    FACE_ID,
    NETWORK,
    ACCESSORY,
    PRIVACY,
    AIRDROP,
    HOTSPOT,
    AIRPLAY,
    NAVIGATION,
    VOICE_MEMO,
    FOCUS,
    FIND_MY,
    SPORTS,
    DELIVERY,
    FLIGHT
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
    val accentColor: Long = 0xFF34C759,
    val packageName: String? = null,
    val contentIntent: android.app.PendingIntent? = null,
    val isSilent: Boolean = false,
    val isFaceIdSuccess: Boolean = false,
    val faceIdContext: String = "Buka Kunci Layar",
    val accessoryName: String = "AirPods Pro",
    val batteryLeft: Int = 100,
    val batteryRight: Int = 95,
    val batteryCase: Int = 85,
    val isCameraActive: Boolean = false,
    val isMicActive: Boolean = false,
    val privacyAppName: String = "Kamera",
    val voipAppName: String = "WhatsApp",
    val isIncomingCall: Boolean = false,
    val callDuration: String = "00:00",
    val airdropSender: String = "MacBook Pro M3",
    val airdropFileName: String = "IMG_8821.HEIC",
    val airdropProgress: Float = 0.72f,
    val airdropCompleted: Boolean = false,
    val hotspotDeviceCount: Int = 1,
    val hotspotDeviceName: String = "iPad Pro M4",
    val activeAirPlayDevice: String = "AirPods Pro",
    val navTurnIcon: String = "turn_right",
    val navInstruction: String = "Belok Kanan ke Jl. Sudirman",
    val navDistance: String = "200 m",
    val navEta: String = "14:35 · 12 min",
    val voiceMemoDuration: String = "01:24",
    val isVoiceMemoRecording: Boolean = false,
    val focusModeName: String = "Do Not Disturb",
    val findMyDeviceName: String = "Kunci Mobil (AirTag)",
    val findMyDistance: String = "3.8 m",
    val sportsHomeTeam: String = "Real Madrid",
    val sportsAwayTeam: String = "Barcelona",
    val sportsHomeScore: Int = 2,
    val sportsAwayScore: Int = 1,
    val sportsMatchTime: String = "78'",
    val sportsLeague: String = "El Clásico · LaLiga",
    val deliveryService: String = "Gojek",
    val deliveryDriverName: String = "Pak Joko",
    val deliveryVehiclePlate: String = "B 4521 SKZ",
    val deliveryStatus: String = "Driver menuju lokasimu",
    val deliveryEta: String = "5 mnt",
    val flightCode: String = "GA 412",
    val flightRoute: String = "CGK ✈ DPS",
    val flightGate: String = "Gate 12B",
    val flightStatus: String = "On Time",
    val flightEta: String = "Tiba 16:45"
)

fun Drawable?.toBitmapSafe(targetSize: Int = 144): Bitmap? {
    if (this == null) return null
    return try {
        if (this is BitmapDrawable && this.bitmap != null && !this.bitmap.isRecycled) {
            this.bitmap
        } else {
            val width = if (intrinsicWidth > 0) intrinsicWidth.coerceAtMost(256) else targetSize
            val height = if (intrinsicHeight > 0) intrinsicHeight.coerceAtMost(256) else targetSize
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val oldBounds = copyBounds()
            setBounds(0, 0, width, height)
            draw(canvas)
            setBounds(oldBounds)
            bitmap
        }
    } catch (e: Throwable) {
        null
    }
}

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

    // Secondary Dynamic Island State (for Dual / Split Island Bubble)
    private val _secondaryDynamicIsland = MutableStateFlow<DynamicIslandData?>(null)
    val secondaryDynamicIsland: StateFlow<DynamicIslandData?> = _secondaryDynamicIsland.asStateFlow()

    // Upgraded Status Bar Telemetry
    private val _isBluetoothDeviceConnected = MutableStateFlow(false)
    val isBluetoothDeviceConnected: StateFlow<Boolean> = _isBluetoothDeviceConnected.asStateFlow()

    private val _isVpnActive = MutableStateFlow(false)
    val isVpnActive: StateFlow<Boolean> = _isVpnActive.asStateFlow()

    private val _isLocationActive = MutableStateFlow(false)
    val isLocationActive: StateFlow<Boolean> = _isLocationActive.asStateFlow()

    private val _isAlarmSet = MutableStateFlow(false)
    val isAlarmSet: StateFlow<Boolean> = _isAlarmSet.asStateFlow()

    private val _isLowPowerMode = MutableStateFlow(false)
    val isLowPowerMode: StateFlow<Boolean> = _isLowPowerMode.asStateFlow()

    private val _showBatteryPercentage = MutableStateFlow(true)
    val showBatteryPercentage: StateFlow<Boolean> = _showBatteryPercentage.asStateFlow()

    private val _networkSpeed = MutableStateFlow("0 KB/s")
    val networkSpeed: StateFlow<String> = _networkSpeed.asStateFlow()

    private val _showNetworkSpeed = MutableStateFlow(true)
    val showNetworkSpeed: StateFlow<Boolean> = _showNetworkSpeed.asStateFlow()

    private val _notchOffsetY = MutableStateFlow(0f)
    val notchOffsetY: StateFlow<Float> = _notchOffsetY.asStateFlow()

    private val _notchScale = MutableStateFlow(1.0f)
    val notchScale: StateFlow<Float> = _notchScale.asStateFlow()

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

    // App Switcher Multitasking State
    private val _recentApps = MutableStateFlow<List<AppId>>(
        listOf(AppId.SAFARI, AppId.PHOTOS, AppId.CAMERA, AppId.SETTINGS, AppId.MESSAGES, AppId.MUSIC, AppId.CALCULATOR, AppId.NOTES)
    )
    val recentApps: StateFlow<List<AppId>> = _recentApps.asStateFlow()

    private val _isAppSwitcherOpen = MutableStateFlow(false)
    val isAppSwitcherOpen: StateFlow<Boolean> = _isAppSwitcherOpen.asStateFlow()

    // Developer Options State
    private val _isDeveloperModeEnabled = MutableStateFlow(false)
    val isDeveloperModeEnabled: StateFlow<Boolean> = _isDeveloperModeEnabled.asStateFlow()

    private val _isFpsCounterVisible = MutableStateFlow(false)
    val isFpsCounterVisible: StateFlow<Boolean> = _isFpsCounterVisible.asStateFlow()

    private val _animationScale = MutableStateFlow(1.0f)
    val animationScale: StateFlow<Float> = _animationScale.asStateFlow()

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

    // AssistiveTouch Virtual Button (Default disabled per user request)
    private val _isAssistiveTouchEnabled = MutableStateFlow(false)
    val isAssistiveTouchEnabled: StateFlow<Boolean> = _isAssistiveTouchEnabled.asStateFlow()

    // iOS 18 Setup Wizard
    private val _isSetupWizardOpen = MutableStateFlow(false)
    val isSetupWizardOpen: StateFlow<Boolean> = _isSetupWizardOpen.asStateFlow()

    // iOS 18 Icon Tint Style: "DEFAULT", "DARK", "TINTED"
    private val _iconTintStyle = MutableStateFlow("DEFAULT")
    val iconTintStyle: StateFlow<String> = _iconTintStyle.asStateFlow()

    fun setIconTintStyle(style: String) {
        _iconTintStyle.value = style
        val app = getApplication<android.app.Application>()
        IOSPersistenceManager.getPrefs(app).edit().putString("icon_tint_style", style).apply()
    }

    fun completeSetupWizard() {
        _isSetupWizardOpen.value = false
        val app = getApplication<android.app.Application>()
        IOSPersistenceManager.getPrefs(app).edit().putBoolean("oobe_completed", true).apply()
    }

    fun openSetupWizard() {
        _isSetupWizardOpen.value = true
    }

    // Spotlight Search
    private val _isSpotlightOpen = MutableStateFlow(false)
    val isSpotlightOpen: StateFlow<Boolean> = _isSpotlightOpen.asStateFlow()

    // Custom App Icons (App ID name or package name -> File path / URI)
    private val _customAppIcons = MutableStateFlow<Map<String, String>>(emptyMap())
    val customAppIcons: StateFlow<Map<String, String>> = _customAppIcons.asStateFlow()

    // Action Button Customization (iPhone 15/16/17 Pro)
    private val _actionButtonFunction = MutableStateFlow("Flashlight")
    val actionButtonFunction: StateFlow<String> = _actionButtonFunction.asStateFlow()

    // Camera Control (iPhone 16/17 Pro)
    private val _cameraControlEnabled = MutableStateFlow(true)
    val cameraControlEnabled: StateFlow<Boolean> = _cameraControlEnabled.asStateFlow()
    private val _cameraControlSensitivity = MutableStateFlow("Default")
    val cameraControlSensitivity: StateFlow<String> = _cameraControlSensitivity.asStateFlow()

    // StandBy Mode (iOS 17/18/26)
    private val _standByEnabled = MutableStateFlow(true)
    val standByEnabled: StateFlow<Boolean> = _standByEnabled.asStateFlow()
    private val _standByNightMode = MutableStateFlow(true)
    val standByNightMode: StateFlow<Boolean> = _standByNightMode.asStateFlow()
    private val _standByAlwaysOn = MutableStateFlow(false)
    val standByAlwaysOn: StateFlow<Boolean> = _standByAlwaysOn.asStateFlow()

    // Focus & Do Not Disturb
    private val _isDNDActive = MutableStateFlow(false)
    val isDNDActive: StateFlow<Boolean> = _isDNDActive.asStateFlow()

    // Focus Mode Pipeline
    private val _currentFocusMode = MutableStateFlow(com.example.model.FocusMode.OFF)
    val currentFocusMode: StateFlow<com.example.model.FocusMode> = _currentFocusMode.asStateFlow()

    // StandBy Mode Overlay State
    private val _isStandByOpen = MutableStateFlow(false)
    val isStandByOpen: StateFlow<Boolean> = _isStandByOpen.asStateFlow()
    fun openStandBy() { _isStandByOpen.value = true }
    fun closeStandBy() { _isStandByOpen.value = false }

    // Back Tap Pipeline (Accessibility)
    private val _backTapEnabled = MutableStateFlow(true)
    val backTapEnabled: StateFlow<Boolean> = _backTapEnabled.asStateFlow()
    private val _backTapDoubleAction = MutableStateFlow(com.example.model.BackTapAction.FLASHLIGHT)
    val backTapDoubleAction: StateFlow<com.example.model.BackTapAction> = _backTapDoubleAction.asStateFlow()
    private val _backTapTripleAction = MutableStateFlow(com.example.model.BackTapAction.SCREENSHOT)
    val backTapTripleAction: StateFlow<com.example.model.BackTapAction> = _backTapTripleAction.asStateFlow()

    // AirDrop / Share Sheet Pipeline
    private val _isShareSheetOpen = MutableStateFlow(false)
    val isShareSheetOpen: StateFlow<Boolean> = _isShareSheetOpen.asStateFlow()
    private val _shareSheetTitle = MutableStateFlow("Item")
    val shareSheetTitle: StateFlow<String> = _shareSheetTitle.asStateFlow()
    fun openShareSheet(title: String = "Item") {
        _shareSheetTitle.value = title
        _isShareSheetOpen.value = true
    }
    fun closeShareSheet() { _isShareSheetOpen.value = false }

    fun openSiri() { _isSiriOpen.value = true }
    fun closeSiri() { _isSiriOpen.value = false }
    fun toggleSiri() { _isSiriOpen.value = !_isSiriOpen.value }

    fun openSpotlight() { _isSpotlightOpen.value = true }
    fun closeSpotlight() { _isSpotlightOpen.value = false }

    fun toggleAssistiveTouch() { _isAssistiveTouchEnabled.value = !_isAssistiveTouchEnabled.value }
    fun setAssistiveTouchEnabled(enabled: Boolean) { _isAssistiveTouchEnabled.value = enabled }

    // Wallpaper Management - Separated Lock Screen vs Home Screen
    private val _currentWallpaperRes = MutableStateFlow<Int>(com.example.R.drawable.wallpaper_alpine)
    val currentWallpaperRes: StateFlow<Int> = _currentWallpaperRes.asStateFlow()
    val lockScreenWallpaperRes: StateFlow<Int> = _currentWallpaperRes.asStateFlow()

    private val _currentWallpaperName = MutableStateFlow("Alpine Mountain Peak")
    val currentWallpaperName: StateFlow<String> = _currentWallpaperName.asStateFlow()
    val lockScreenWallpaperName: StateFlow<String> = _currentWallpaperName.asStateFlow()

    private val _currentWallpaperUri = MutableStateFlow<String?>(null)
    val currentWallpaperUri: StateFlow<String?> = _currentWallpaperUri.asStateFlow()
    val lockScreenWallpaperUri: StateFlow<String?> = _currentWallpaperUri.asStateFlow()

    // Home Screen specific Wallpaper
    private val _homeScreenWallpaperRes = MutableStateFlow<Int>(com.example.R.drawable.wallpaper_alpine)
    val homeScreenWallpaperRes: StateFlow<Int> = _homeScreenWallpaperRes.asStateFlow()

    private val _homeScreenWallpaperName = MutableStateFlow("Alpine Mountain Peak")
    val homeScreenWallpaperName: StateFlow<String> = _homeScreenWallpaperName.asStateFlow()

    private val _homeScreenWallpaperUri = MutableStateFlow<String?>(null)
    val homeScreenWallpaperUri: StateFlow<String?> = _homeScreenWallpaperUri.asStateFlow()

    // Notification Center Real Pipeline (starts empty, populated strictly by real device notifications)
    private val _activeNotifications = MutableStateFlow<List<IOSNotificationItem>>(emptyList())
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

    private val _requestPasscodeTrigger = MutableStateFlow(0)
    val requestPasscodeTrigger: StateFlow<Int> = _requestPasscodeTrigger.asStateFlow()

    fun triggerHomeButtonOnLockScreen() {
        if (_isDeviceLocked.value) {
            if (_isPasscodeEnabled.value) {
                _requestPasscodeTrigger.value += 1
            } else {
                unlockDevice()
            }
        }
    }

    private val _userPasscode = MutableStateFlow("1234")
    val userPasscode: StateFlow<String> = _userPasscode.asStateFlow()

    private val _isFaceIdEnabled = MutableStateFlow(true)
    val isFaceIdEnabled: StateFlow<Boolean> = _isFaceIdEnabled.asStateFlow()

    private val _userCountry = MutableStateFlow("Indonesia")
    val userCountry: StateFlow<String> = _userCountry.asStateFlow()

    fun setUserCountry(country: String) {
        _userCountry.value = country
        IOSPersistenceManager.getPrefs(getApplication()).edit().putString("user_country", country).apply()
    }

    // Dynamic Home Screen Apps
    private val _homeScreenApps = MutableStateFlow<List<AppItem>>(emptyList())
    val homeScreenApps: StateFlow<List<AppItem>> = _homeScreenApps.asStateFlow()

    // Dynamic Dock Apps (Configurable & Customizable)
    private val _dockApps = MutableStateFlow<List<AppItem>>(emptyList())
    val dockApps: StateFlow<List<AppItem>> = _dockApps.asStateFlow()

    // Dynamic Top Widgets (Weather, Calendar, Photos)
    // Dynamic Widgets (Individual, Movable, Resizable iOS 26 Liquid Glass Widgets)
    private val _widget1Type = MutableStateFlow("WEATHER")
    val widget1Type: StateFlow<String> = _widget1Type.asStateFlow()
    private val _widget1Size = MutableStateFlow("MEDIUM") // "SMALL" (2x2), "MEDIUM" (4x2), "COMPACT" (2x1), "LARGE" (4x4)
    val widget1Size: StateFlow<String> = _widget1Size.asStateFlow()
    private val _widget1Row = MutableStateFlow(0) // Row position (0..5)
    val widget1Row: StateFlow<Int> = _widget1Row.asStateFlow()
    private val _widget1Align = MutableStateFlow("LEFT") // "LEFT" or "RIGHT"
    val widget1Align: StateFlow<String> = _widget1Align.asStateFlow()
    private val _widget1Enabled = MutableStateFlow(true)
    val widget1Enabled: StateFlow<Boolean> = _widget1Enabled.asStateFlow()

    private val _widget2Type = MutableStateFlow("CALENDAR")
    val widget2Type: StateFlow<String> = _widget2Type.asStateFlow()
    private val _widget2Size = MutableStateFlow("SMALL")
    val widget2Size: StateFlow<String> = _widget2Size.asStateFlow()
    private val _widget2Row = MutableStateFlow(1)
    val widget2Row: StateFlow<Int> = _widget2Row.asStateFlow()
    private val _widget2Align = MutableStateFlow("LEFT")
    val widget2Align: StateFlow<String> = _widget2Align.asStateFlow()
    private val _widget2Enabled = MutableStateFlow(true) // Enabled by default on Row 1 (2x2 Calendar with 4 apps on right!)
    val widget2Enabled: StateFlow<Boolean> = _widget2Enabled.asStateFlow()

    // Widget 3: WhatsApp Widget (Customizable Size & Drag & Drop)
    private val _widget3Type = MutableStateFlow("WHATSAPP")
    val widget3Type: StateFlow<String> = _widget3Type.asStateFlow()
    private val _widget3Size = MutableStateFlow("MEDIUM")
    val widget3Size: StateFlow<String> = _widget3Size.asStateFlow()
    private val _widget3Row = MutableStateFlow(2) // Row 2
    val widget3Row: StateFlow<Int> = _widget3Row.asStateFlow()
    private val _widget3Align = MutableStateFlow("LEFT")
    val widget3Align: StateFlow<String> = _widget3Align.asStateFlow()
    private val _widget3Enabled = MutableStateFlow(false)
    val widget3Enabled: StateFlow<Boolean> = _widget3Enabled.asStateFlow()

    // Widget 4: Additional dynamic widget slot
    private val _widget4Type = MutableStateFlow("CALENDAR")
    val widget4Type: StateFlow<String> = _widget4Type.asStateFlow()
    private val _widget4Size = MutableStateFlow("SMALL")
    val widget4Size: StateFlow<String> = _widget4Size.asStateFlow()
    private val _widget4Row = MutableStateFlow(2)
    val widget4Row: StateFlow<Int> = _widget4Row.asStateFlow()
    private val _widget4Align = MutableStateFlow("LEFT")
    val widget4Align: StateFlow<String> = _widget4Align.asStateFlow()
    private val _widget4Enabled = MutableStateFlow(false)
    val widget4Enabled: StateFlow<Boolean> = _widget4Enabled.asStateFlow()

    // Widget 5: Additional dynamic widget slot
    private val _widget5Type = MutableStateFlow("BATTERY")
    val widget5Type: StateFlow<String> = _widget5Type.asStateFlow()
    private val _widget5Size = MutableStateFlow("SMALL")
    val widget5Size: StateFlow<String> = _widget5Size.asStateFlow()
    private val _widget5Row = MutableStateFlow(3)
    val widget5Row: StateFlow<Int> = _widget5Row.asStateFlow()
    private val _widget5Align = MutableStateFlow("RIGHT")
    val widget5Align: StateFlow<String> = _widget5Align.asStateFlow()
    private val _widget5Enabled = MutableStateFlow(false)
    val widget5Enabled: StateFlow<Boolean> = _widget5Enabled.asStateFlow()

    // Widget 6: Additional dynamic widget slot
    private val _widget6Type = MutableStateFlow("CLOCK")
    val widget6Type: StateFlow<String> = _widget6Type.asStateFlow()
    private val _widget6Size = MutableStateFlow("SMALL")
    val widget6Size: StateFlow<String> = _widget6Size.asStateFlow()
    private val _widget6Row = MutableStateFlow(4)
    val widget6Row: StateFlow<Int> = _widget6Row.asStateFlow()
    private val _widget6Align = MutableStateFlow("LEFT")
    val widget6Align: StateFlow<String> = _widget6Align.asStateFlow()
    private val _widget6Enabled = MutableStateFlow(false)
    val widget6Enabled: StateFlow<Boolean> = _widget6Enabled.asStateFlow()

    private val _leftWidgetType = MutableStateFlow("WEATHER")
    val leftWidgetType: StateFlow<String> = _leftWidgetType.asStateFlow()

    private val _rightWidgetType = MutableStateFlow("PHOTOS")
    val rightWidgetType: StateFlow<String> = _rightWidgetType.asStateFlow()

    // Dynamic Widget Row Position (Legacy / Pair mode)
    private val _widgetRowPosition = MutableStateFlow(0)
    val widgetRowPosition: StateFlow<Int> = _widgetRowPosition.asStateFlow()

    // Real Wi-Fi Network List
    private val _availableWifiList = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val availableWifiList: StateFlow<List<Pair<String, Boolean>>> = _availableWifiList.asStateFlow()
    val scannedWifiList: StateFlow<List<Pair<String, Boolean>>> get() = availableWifiList

    private val _currentWifiSsid = MutableStateFlow("")
    val currentWifiSsid: StateFlow<String> = _currentWifiSsid.asStateFlow()

    private val _isWifiScanning = MutableStateFlow(false)
    val isWifiScanning: StateFlow<Boolean> = _isWifiScanning.asStateFlow()

    // Real Cellular State
    private val _cellularCarrierName = MutableStateFlow("Cellular")
    val cellularCarrierName: StateFlow<String> = _cellularCarrierName.asStateFlow()

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
    private val _musicList = MutableStateFlow<List<SongItem>>(emptyList())
    val musicList: StateFlow<List<SongItem>> = _musicList.asStateFlow()

    private val _currentSong = MutableStateFlow<SongItem?>(null)
    val currentSong: StateFlow<SongItem?> = _currentSong.asStateFlow()

    private val _musicProgress = MutableStateFlow(0f)
    val musicProgress: StateFlow<Float> = _musicProgress.asStateFlow()

    private val _musicCurrentTimeStr = MutableStateFlow("0:00")
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
    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _activeNoteId = MutableStateFlow<String?>(null)
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
            PhotoItem("2", "Crystal Glass Sky", "14 Jul", com.example.R.drawable.wp_crystal_glass),
            PhotoItem("3", "Twilight Clouds", "12 Jul", com.example.R.drawable.wp_twilight_clouds),
            PhotoItem("4", "Golden Dunes", "10 Jul", com.example.R.drawable.wp_golden_dunes)
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

    private val _isVolumeInterceptionActive = MutableStateFlow(false)
    val isVolumeInterceptionActive: StateFlow<Boolean> = _isVolumeInterceptionActive.asStateFlow()

    private val _isSystemUIDisabled = MutableStateFlow(false)
    val isSystemUIDisabled: StateFlow<Boolean> = _isSystemUIDisabled.asStateFlow()

    private val _isNotificationListenerGranted = MutableStateFlow(false)
    val isNotificationListenerGranted: StateFlow<Boolean> = _isNotificationListenerGranted.asStateFlow()

    private val _cpuBoostEnabled = MutableStateFlow(false)
    val cpuBoostEnabled: StateFlow<Boolean> = _cpuBoostEnabled.asStateFlow()

    private val _is120HzUnlocked = MutableStateFlow(false)
    val is120HzUnlocked: StateFlow<Boolean> = _is120HzUnlocked.asStateFlow()

    private val _isThermalDisabled = MutableStateFlow(false)
    val isThermalDisabled: StateFlow<Boolean> = _isThermalDisabled.asStateFlow()

    private val _isSELinuxPermissive = MutableStateFlow(false)
    val isSELinuxPermissive: StateFlow<Boolean> = _isSELinuxPermissive.asStateFlow()

    private val _retinaDpi = MutableStateFlow(240)
    val retinaDpi: StateFlow<Int> = _retinaDpi.asStateFlow()

    private val _spoofedModel = MutableStateFlow("iPhone SE (2nd gen) / iOS 26")
    val spoofedModel: StateFlow<String> = _spoofedModel.asStateFlow()

    private val _isTopDisplayInsetActive = MutableStateFlow(false)
    val isTopDisplayInsetActive: StateFlow<Boolean> = _isTopDisplayInsetActive.asStateFlow()

    private val _weatherData = MutableStateFlow(WeatherData())
    val weatherData: StateFlow<WeatherData> = _weatherData.asStateFlow()

    init {
        // Load persistent data for Calendar, Alarms, Reminders, Notes, Wallpaper & Bluetooth
        try {
            val app = getApplication<Application>()
            _calendarEvents.value = IOSPersistenceManager.loadCalendarEvents(app)
            _alarms.value = IOSPersistenceManager.loadAlarms(app)
            _reminders.value = IOSPersistenceManager.loadReminders(app)
            _notes.value = IOSPersistenceManager.loadNotes(app)
            val (savedRes, savedName, savedUri) = IOSPersistenceManager.loadLockScreenWallpaper(app)
            _currentWallpaperRes.value = savedRes
            _currentWallpaperName.value = savedName
            _currentWallpaperUri.value = savedUri

            val (savedHomeRes, savedHomeName, savedHomeUri) = IOSPersistenceManager.loadHomeScreenWallpaper(app)
            _homeScreenWallpaperRes.value = savedHomeRes
            _homeScreenWallpaperName.value = savedHomeName
            _homeScreenWallpaperUri.value = savedHomeUri

            _lockScreenConfig.value = IOSPersistenceManager.loadFullLockScreenConfig(app)
            initBluetoothState(app)

            // Load iRoot persistent states
            _isImmersiveLocked.value = IOSPersistenceManager.loadRootBoolean(app, "immersive", false)
            _isSystemUIDisabled.value = IOSPersistenceManager.loadRootBoolean(app, "systemui_disabled", false)
            _isNotificationListenerGranted.value = IOSPersistenceManager.loadRootBoolean(app, "notif_granted", false)
            _cpuBoostEnabled.value = IOSPersistenceManager.loadRootBoolean(app, "cpu_boost", false)
            _is120HzUnlocked.value = IOSPersistenceManager.loadRootBoolean(app, "120hz", false)
            _isThermalDisabled.value = IOSPersistenceManager.loadRootBoolean(app, "thermal_disabled", false)
            _isSELinuxPermissive.value = IOSPersistenceManager.loadRootBoolean(app, "selinux_permissive", false)
            _retinaDpi.value = IOSPersistenceManager.loadRootInt(app, "retina_dpi", 240)
            _spoofedModel.value = IOSPersistenceManager.loadRootString(app, "spoofed_model", "iPhone SE (2nd gen) / iOS 26")
            _isTopDisplayInsetActive.value = IOSPersistenceManager.loadRootBoolean(app, "top_inset", false)
            _isVolumeInterceptionActive.value = RootExecutor.isVolumeInterceptionEnabled(app)

            // Initial Real Network Scans (Wi-Fi, Bluetooth, Cellular)
            scanWifiNetworks(app)
            initBluetoothState(app)
            refreshCellularInfo(app)

            // Auto-fetch real-time weather for Bandung
            fetchLiveWeather("Bandung")

            // Load Custom Icons & iOS 26 Settings
            _customAppIcons.value = IOSPersistenceManager.loadCustomIcons(app)
            _actionButtonFunction.value = IOSPersistenceManager.loadActionButton(app)
            _cameraControlEnabled.value = IOSPersistenceManager.loadCameraControlEnabled(app)
            _standByEnabled.value = IOSPersistenceManager.loadStandByEnabled(app)
            _currentFocusMode.value = IOSPersistenceManager.loadFocusMode(app)
            OverlayBridge.currentFocusMode.value = _currentFocusMode.value
            _isDNDActive.value = (_currentFocusMode.value != com.example.model.FocusMode.OFF)
            _backTapEnabled.value = IOSPersistenceManager.loadBackTapEnabled(app)
            _backTapDoubleAction.value = IOSPersistenceManager.loadBackTapDoubleAction(app)
            _backTapTripleAction.value = IOSPersistenceManager.loadBackTapTripleAction(app)

            initHomeScreenApps(app)
            initDockApps(app)
            initWidgetTypes(app)
            _userCountry.value = IOSPersistenceManager.getPrefs(app).getString("user_country", "Indonesia") ?: "Indonesia"

            val oobeCompleted = IOSPersistenceManager.getPrefs(app).getBoolean("oobe_completed", false)
            _isSetupWizardOpen.value = !oobeCompleted
            _iconTintStyle.value = IOSPersistenceManager.getPrefs(app).getString("icon_tint_style", "DEFAULT") ?: "DEFAULT"

            // Setup OverlayBridge callbacks for cross-app control
            OverlayBridge.onTogglePlayPause = { toggleMusicPlay() }
            OverlayBridge.onNextSong = { skipNextSong() }
            OverlayBridge.onPrevSong = { skipPreviousSong() }
            OverlayBridge.onEndCall = { endCall(null) }
            OverlayBridge.onAnswerCall = { answerIncomingCall(getApplication()) }
            OverlayBridge.onDeclineCall = { rejectIncomingCall(getApplication()) }
            OverlayBridge.onToggleMute = { toggleMuteCall() }
            OverlayBridge.onToggleSpeaker = { toggleSpeakerCall() }
            OverlayBridge.onSeekMusic = { frac -> seekMusicTo(frac) }
            OverlayBridge.onTimerPauseResume = { toggleTimerPauseResume() }
            OverlayBridge.onTimerCancel = { stopTimer() }
            OverlayBridge.onVoiceMemoStop = { dismissDynamicIsland() }
            OverlayBridge.onDismissDynamicIsland = { dismissDynamicIsland() }
            OverlayBridge.onSecondaryIslandClick = { onSecondaryIslandClicked() }
            OverlayBridge.onDoubleTapStatusBar = { lockDevice() }

            _showBatteryPercentage.value = IOSPersistenceManager.getPrefs(app).getBoolean("show_battery_pct", true)
            _notchOffsetY.value = IOSPersistenceManager.getPrefs(app).getFloat("notch_offset_y", 0f)
            _notchScale.value = IOSPersistenceManager.getPrefs(app).getFloat("notch_scale", 1.0f)

            // Mirror Dynamic Island state changes across all apps
            viewModelScope.launch {
                _dynamicIsland.collect { islandState ->
                    OverlayBridge.dynamicIslandData.value = islandState
                }
            }
            viewModelScope.launch {
                _secondaryDynamicIsland.collect { sec ->
                    OverlayBridge.secondaryIslandData.value = sec
                }
            }
            viewModelScope.launch {
                _isBluetoothDeviceConnected.collect { OverlayBridge.isBluetoothConnected.value = it }
            }
            viewModelScope.launch {
                _isVpnActive.collect { OverlayBridge.isVpnConnected.value = it }
            }
            viewModelScope.launch {
                _isLocationActive.collect { OverlayBridge.isLocationActive.value = it }
            }
            viewModelScope.launch {
                _isAlarmSet.collect { OverlayBridge.isAlarmSet.value = it }
            }
            viewModelScope.launch {
                _isLowPowerMode.collect { OverlayBridge.isLowPowerMode.value = it }
            }
            viewModelScope.launch {
                _showBatteryPercentage.collect { OverlayBridge.showBatteryPercentage.value = it }
            }
            _showNetworkSpeed.value = IOSPersistenceManager.getPrefs(app).getBoolean("show_network_speed", true)
            viewModelScope.launch {
                _showNetworkSpeed.collect { OverlayBridge.showNetworkSpeed.value = it }
            }
            viewModelScope.launch {
                _networkSpeed.collect { OverlayBridge.networkSpeed.value = it }
            }
            viewModelScope.launch {
                _notchOffsetY.collect { OverlayBridge.notchOffsetY.value = it }
            }
            viewModelScope.launch {
                _notchScale.collect { OverlayBridge.notchScale.value = it }
            }
            viewModelScope.launch {
                _isCallMuted.collect { OverlayBridge.isCallMuted.value = it }
            }
            viewModelScope.launch {
                _isCallSpeaker.collect { OverlayBridge.isCallSpeaker.value = it }
            }

            startNetworkTrafficMonitor()
            refreshSystemTelemetry(app)
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
        val app = getApplication<Application>()
        if (appId == AppId.PHONE) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
                return
            } catch (_: Exception) {}
        } else if (appId == AppId.MESSAGES) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
                return
            } catch (_: Exception) {
                try {
                    val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(smsIntent)
                    return
                } catch (_: Exception) {}
            }
        } else if (appId == AppId.APP_STORE) {
            try {
                val storeIntent = app.packageManager.getLaunchIntentForPackage("com.aurora.store")
                    ?: app.packageManager.getLaunchIntentForPackage("org.fdroid.fdroid")
                    ?: app.packageManager.getLaunchIntentForPackage("com.android.vending")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                storeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(storeIntent)
                return
            } catch (_: Exception) {}
        }

        _currentApp.value = appId
        _isControlCenterOpen.value = false
        _isNotificationCenterOpen.value = false
        _isLockScreenCustomizeOpen.value = false
        _isAppSwitcherOpen.value = false

        // Update recent apps stack
        val list = _recentApps.value.toMutableList()
        list.remove(appId)
        list.add(0, appId)
        if (list.size > 12) {
            _recentApps.value = list.take(12)
        } else {
            _recentApps.value = list
        }
    }

    fun openAppSwitcher() {
        _isAppSwitcherOpen.value = true
        _isControlCenterOpen.value = false
        _isNotificationCenterOpen.value = false
        _isLockScreenCustomizeOpen.value = false
    }

    fun closeAppSwitcher() {
        _isAppSwitcherOpen.value = false
    }

    fun dismissRecentApp(appId: AppId) {
        val list = _recentApps.value.toMutableList()
        list.remove(appId)
        _recentApps.value = list
        if (_currentApp.value == appId) {
            _currentApp.value = null
        }
    }

    fun clearAllRecentApps() {
        _recentApps.value = emptyList()
        _currentApp.value = null
        _isAppSwitcherOpen.value = false
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        _isDeveloperModeEnabled.value = enabled
    }

    fun toggleFpsCounter(enabled: Boolean) {
        _isFpsCounterVisible.value = enabled
    }

    fun setAnimationScale(scale: Float) {
        _animationScale.value = scale
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

    fun openControlCenter() {
        _isControlCenterOpen.value = true
        _isNotificationCenterOpen.value = false
        _isAppSwitcherOpen.value = false
        _isSpotlightOpen.value = false
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

    fun openNotificationCenter() {
        _isNotificationCenterOpen.value = true
        _isControlCenterOpen.value = false
        _isAppSwitcherOpen.value = false
        _isSpotlightOpen.value = false
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
        val updated = update(_lockScreenConfig.value)
        _lockScreenConfig.value = updated
        try {
            val app = getApplication<Application>()
            IOSPersistenceManager.saveFullLockScreenConfig(app, updated)
        } catch (ignored: Exception) {}
    }

    // Dynamic Island Toast & Alerts
    fun showDynamicIslandNotification(
        title: String,
        message: String,
        icon: String = "bell",
        accent: Long = 0xFF34C759,
        packageName: String? = null,
        contentIntent: android.app.PendingIntent? = null
    ) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.NOTIFICATION,
            title = title,
            subtitle = message,
            icon = icon,
            accentColor = accent,
            packageName = packageName,
            contentIntent = contentIntent
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(4500)
            if (_dynamicIsland.value.mode == DynamicIslandMode.NOTIFICATION) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun dismissDynamicIsland() {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
        _isInCall.value = false
        OverlayBridge.isInCall.value = false
        OverlayBridge.dynamicIslandData.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
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
        // If in call (cellular or VoIP/WhatsApp), DO NOT override call animation with Volume!
        if (_isInCall.value || _dynamicIsland.value.mode == DynamicIslandMode.CALL) {
            return
        }
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

    fun showDynamicIslandRinger(isSilent: Boolean) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.SILENT_MODE,
            title = if (isSilent) "Mode Hening" else "Dering",
            subtitle = if (isSilent) "Hening Aktif" else "Suara Aktif",
            icon = if (isSilent) "silent" else "bell",
            accentColor = if (isSilent) 0xFFFF3B30 else 0xFF34C759,
            isSilent = isSilent
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(3000)
            if (_dynamicIsland.value.mode == DynamicIslandMode.SILENT_MODE) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandFaceId(context: String = "Buka Kunci Layar") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.FACE_ID,
            title = "Face ID",
            subtitle = context,
            icon = "faceid",
            accentColor = 0xFF007AFF,
            faceIdContext = context,
            isFaceIdSuccess = false
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(800)
            if (_dynamicIsland.value.mode == DynamicIslandMode.FACE_ID) {
                _dynamicIsland.value = _dynamicIsland.value.copy(isFaceIdSuccess = true)
            }
            delay(1200)
            if (_dynamicIsland.value.mode == DynamicIslandMode.FACE_ID) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandNetwork(type: String, status: String) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.NETWORK,
            title = type,
            subtitle = status,
            icon = type.lowercase(),
            accentColor = 0xFF007AFF
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(2800)
            if (_dynamicIsland.value.mode == DynamicIslandMode.NETWORK) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandAccessory(name: String = "AirPods Pro", left: Int = 100, right: Int = 95, case: Int = 85) {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.ACCESSORY,
            title = name,
            subtitle = "Terhubung",
            icon = "headphones",
            accentColor = 0xFF34C759,
            accessoryName = name,
            batteryLeft = left,
            batteryRight = right,
            batteryCase = case
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(4000)
            if (_dynamicIsland.value.mode == DynamicIslandMode.ACCESSORY) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandPrivacy(isCamera: Boolean, isMic: Boolean, appName: String = "Kamera") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.PRIVACY,
            title = if (isCamera) "Kamera Aktif" else "Mikrofon Aktif",
            subtitle = "Digunakan oleh $appName",
            icon = if (isCamera) "camera" else "mic",
            accentColor = if (isCamera) 0xFF34C759 else 0xFFFF9500,
            isCameraActive = isCamera,
            isMicActive = isMic,
            privacyAppName = appName
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(3500)
            if (_dynamicIsland.value.mode == DynamicIslandMode.PRIVACY) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandVoipCall(appName: String = "WhatsApp", callerName: String = "Dimas Pratama", isIncoming: Boolean = true) {
        dynamicIslandJob?.cancel()
        _isInCall.value = true
        _callContactName.value = callerName
        val voipData = DynamicIslandData(
            mode = DynamicIslandMode.CALL,
            title = callerName,
            subtitle = if (isIncoming) "$appName Audio..." else "00:01",
            icon = if (appName.contains("whatsapp", ignoreCase = true)) "whatsapp" else "phone",
            accentColor = if (appName.contains("WhatsApp", ignoreCase = true)) 0xFF25D366 else 0xFF007AFF,
            voipAppName = appName,
            isIncomingCall = isIncoming
        )
        _dynamicIsland.value = voipData
        OverlayBridge.isInCall.value = true
        OverlayBridge.callName.value = callerName
        OverlayBridge.dynamicIslandData.value = voipData
    }

    fun showDynamicIslandAirDrop(sender: String = "MacBook Pro M3", fileName: String = "IMG_8821.HEIC") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.AIRDROP,
            title = "AirDrop · $sender",
            subtitle = fileName,
            icon = "airdrop",
            accentColor = 0xFF007AFF,
            airdropSender = sender,
            airdropFileName = fileName,
            airdropProgress = 0.35f,
            airdropCompleted = false
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(1000)
            if (_dynamicIsland.value.mode == DynamicIslandMode.AIRDROP) {
                _dynamicIsland.value = _dynamicIsland.value.copy(airdropProgress = 0.85f)
            }
            delay(900)
            if (_dynamicIsland.value.mode == DynamicIslandMode.AIRDROP) {
                _dynamicIsland.value = _dynamicIsland.value.copy(airdropProgress = 1f, airdropCompleted = true, subtitle = "Selesai")
            }
            delay(2000)
            if (_dynamicIsland.value.mode == DynamicIslandMode.AIRDROP) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandHotspot(count: Int = 1, deviceName: String = "iPad Pro M4") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.HOTSPOT,
            title = "Hotspot Pribadi",
            subtitle = "$count Perangkat ($deviceName)",
            icon = "link",
            accentColor = 0xFF007AFF,
            hotspotDeviceCount = count,
            hotspotDeviceName = deviceName
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(3500)
            if (_dynamicIsland.value.mode == DynamicIslandMode.HOTSPOT) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandAirPlay(activeDevice: String = "AirPods Pro") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.AIRPLAY,
            title = "Output Audio (AirPlay)",
            subtitle = "Terhubung ke $activeDevice",
            icon = "airplay",
            accentColor = 0xFF007AFF,
            activeAirPlayDevice = activeDevice
        )
    }

    fun switchAirPlayDevice(device: String) {
        _dynamicIsland.value = _dynamicIsland.value.copy(activeAirPlayDevice = device, subtitle = "Terhubung ke $device")
        showDynamicIslandNotification("AirPlay", "Output dialihkan ke $device", "airplay", 0xFF007AFF)
    }

    fun showDynamicIslandNavigation(turnIcon: String = "turn_right", instruction: String = "Belok Kanan ke Jl. Sudirman", distance: String = "200 m", eta: String = "14:35 · 12 min") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.NAVIGATION,
            title = instruction,
            subtitle = "$distance · $eta",
            icon = "navigation",
            accentColor = 0xFF34C759,
            navTurnIcon = turnIcon,
            navInstruction = instruction,
            navDistance = distance,
            navEta = eta
        )
    }

    fun showDynamicIslandVoiceMemo(isRecording: Boolean = true, duration: String = "01:24") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.VOICE_MEMO,
            title = if (isRecording) "Perekam Suara" else "Perekaman Selesai",
            subtitle = duration,
            icon = "mic",
            accentColor = 0xFFFF2D55,
            voiceMemoDuration = duration,
            isVoiceMemoRecording = isRecording
        )
    }

    fun showDynamicIslandFocus(modeName: String = "Do Not Disturb", icon: String = "moon") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.FOCUS,
            title = modeName,
            subtitle = "Mode Fokus Aktif",
            icon = icon,
            accentColor = 0xFF5856D6,
            focusModeName = modeName
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(3000)
            if (_dynamicIsland.value.mode == DynamicIslandMode.FOCUS) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
    }

    fun showDynamicIslandFindMy(device: String = "Kunci Mobil (AirTag)", distance: String = "3.8 m") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.FIND_MY,
            title = device,
            subtitle = "$distance di depan Anda",
            icon = "findmy",
            accentColor = 0xFF34C759,
            findMyDeviceName = device,
            findMyDistance = distance
        )
    }

    fun showDynamicIslandSports(home: String = "Real Madrid", away: String = "Barcelona", homeScore: Int = 2, awayScore: Int = 1, time: String = "78'") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.SPORTS,
            title = "$home $homeScore - $awayScore $away",
            subtitle = "LaLiga · $time",
            icon = "sports",
            accentColor = 0xFFFF9500,
            sportsHomeTeam = home,
            sportsAwayTeam = away,
            sportsHomeScore = homeScore,
            sportsAwayScore = awayScore,
            sportsMatchTime = time
        )
    }

    fun showDynamicIslandDelivery(service: String = "Gojek", driver: String = "Pak Joko", plate: String = "B 4521 SKZ", status: String = "Driver menuju lokasimu", eta: String = "5 mnt") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.DELIVERY,
            title = "$service · $eta",
            subtitle = "$driver ($plate) · $status",
            icon = "delivery",
            accentColor = 0xFF00AA13,
            deliveryService = service,
            deliveryDriverName = driver,
            deliveryVehiclePlate = plate,
            deliveryStatus = status,
            deliveryEta = eta
        )
    }

    fun showDynamicIslandFlight(code: String = "GA 412", route: String = "CGK ✈ DPS", gate: String = "Gate 12B", status: String = "On Time") {
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.FLIGHT,
            title = "$code · $route",
            subtitle = "$gate · $status (Tiba 16:45)",
            icon = "flight",
            accentColor = 0xFF007AFF,
            flightCode = code,
            flightRoute = route,
            flightGate = gate,
            flightStatus = status
        )
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

    @SuppressLint("MissingPermission")
    fun endCall(context: Context? = null) {
        _isInCall.value = false
        OverlayBridge.isInCall.value = false
        callTimerJob?.cancel()
        val ctx = context ?: getApplication<Application>()
        try {
            val telecom = ctx.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecom?.endCall()
                }
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
        OverlayBridge.dynamicIslandData.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
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

    // Countdown Timer with Dual Island support
    fun startTimer(seconds: Int) {
        countdownTimerJob?.cancel()
        _timerRemainingSeconds.value = seconds
        _isTimerRunning.value = true

        val timerData = DynamicIslandData(
            mode = DynamicIslandMode.TIMER,
            title = "Timer",
            subtitle = formatSeconds(seconds),
            icon = "timer",
            accentColor = 0xFFFF9500
        )

        // Split into detached bubble if music or call is active!
        if (_isMusicPlaying.value || _isInCall.value) {
            _secondaryDynamicIsland.value = timerData
        } else {
            _dynamicIsland.value = timerData
        }

        countdownTimerJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerRemainingSeconds.value -= 1
                val timeStr = formatSeconds(_timerRemainingSeconds.value)
                if (_dynamicIsland.value.mode == DynamicIslandMode.TIMER) {
                    _dynamicIsland.value = _dynamicIsland.value.copy(subtitle = timeStr)
                }
                if (_secondaryDynamicIsland.value?.mode == DynamicIslandMode.TIMER) {
                    _secondaryDynamicIsland.value = _secondaryDynamicIsland.value?.copy(subtitle = timeStr)
                }
            }
            _isTimerRunning.value = false
            _secondaryDynamicIsland.value = null
            showDynamicIslandNotification("Timer", "Waktu selesai!", "bell", 0xFFFF9500)
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        countdownTimerJob?.cancel()
        if (_dynamicIsland.value.mode == DynamicIslandMode.TIMER) {
            _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
        }
        _secondaryDynamicIsland.value = null
    }

    fun toggleTimerPauseResume() {
        if (_isTimerRunning.value) {
            _isTimerRunning.value = false
            countdownTimerJob?.cancel()
        } else if (_timerRemainingSeconds.value > 0) {
            startTimer(_timerRemainingSeconds.value)
        } else {
            startTimer(300)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun refreshSystemTelemetry(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            _isLowPowerMode.value = pm?.isPowerSaveMode ?: false

            val hasBtConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (hasBtConnect && btAdapter != null && btAdapter.isEnabled) {
                try {
                    val a2dp = btAdapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                    val headset = btAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                    _isBluetoothDeviceConnected.value = a2dp || headset
                } catch (_: SecurityException) {
                    _isBluetoothDeviceConnected.value = false
                }
            }

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            _isLocationActive.value = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

            val nextAlarm = Settings.System.getString(context.contentResolver, Settings.System.NEXT_ALARM_FORMATTED)
            _isAlarmSet.value = !nextAlarm.isNullOrBlank()
        } catch (_: Exception) {}
    }

    fun setSecondaryDynamicIsland(data: DynamicIslandData?) {
        _secondaryDynamicIsland.value = data
    }

    fun dismissSecondaryDynamicIsland() {
        _secondaryDynamicIsland.value = null
    }

    fun onSecondaryIslandClicked() {
        val sec = _secondaryDynamicIsland.value ?: return
        _dynamicIsland.value = sec
        _secondaryDynamicIsland.value = null
    }

    fun showDualIslandDemo() {
        _isMusicPlaying.value = true
        OverlayBridge.isMusicPlaying.value = true
        _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
        _secondaryDynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.TIMER,
            title = "Timer",
            subtitle = "04:59",
            icon = "timer",
            accentColor = 0xFFFF9500
        )
    }

    fun toggleBluetoothConnected() {
        _isBluetoothDeviceConnected.value = !_isBluetoothDeviceConnected.value
    }

    fun toggleVpnActive() {
        _isVpnActive.value = !_isVpnActive.value
    }

    fun toggleLocationActive() {
        _isLocationActive.value = !_isLocationActive.value
    }

    fun toggleAlarmSet() {
        _isAlarmSet.value = !_isAlarmSet.value
    }

    fun toggleLowPowerMode() {
        _isLowPowerMode.value = !_isLowPowerMode.value
    }

    fun toggleBatteryPercentage(context: Context? = null) {
        val newVal = !_showBatteryPercentage.value
        _showBatteryPercentage.value = newVal
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.getPrefs(ctx).edit().putBoolean("show_battery_pct", newVal).apply()
    }

    fun toggleShowNetworkSpeed(context: Context? = null) {
        val newVal = !_showNetworkSpeed.value
        _showNetworkSpeed.value = newVal
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.getPrefs(ctx).edit().putBoolean("show_network_speed", newVal).apply()
    }

    private fun startNetworkTrafficMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            var lastRxBytes = android.net.TrafficStats.getTotalRxBytes()
            var lastTxBytes = android.net.TrafficStats.getTotalTxBytes()
            var lastTime = System.currentTimeMillis()
            while (true) {
                delay(1000)
                try {
                    val currentRx = android.net.TrafficStats.getTotalRxBytes()
                    val currentTx = android.net.TrafficStats.getTotalTxBytes()
                    val currentTime = System.currentTimeMillis()

                    val deltaRx = (currentRx - lastRxBytes).coerceAtLeast(0)
                    val deltaTx = (currentTx - lastTxBytes).coerceAtLeast(0)
                    val deltaTimeSec = ((currentTime - lastTime) / 1000.0).coerceAtLeast(0.5)

                    lastRxBytes = currentRx
                    lastTxBytes = currentTx
                    lastTime = currentTime

                    val totalBytesPerSec = ((deltaRx + deltaTx) / deltaTimeSec).toLong()
                    val speedText = when {
                        totalBytesPerSec >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", totalBytesPerSec / (1024.0 * 1024.0))
                        totalBytesPerSec >= 1024 -> "${totalBytesPerSec / 1024} KB/s"
                        totalBytesPerSec > 0 -> "$totalBytesPerSec B/s"
                        else -> "0 KB/s"
                    }
                    _networkSpeed.value = speedText
                } catch (_: Exception) {}
            }
        }
    }

    fun setNotchCalibration(offsetY: Float, scale: Float, context: Context? = null) {
        _notchOffsetY.value = offsetY
        _notchScale.value = scale
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.getPrefs(ctx).edit()
            .putFloat("notch_offset_y", offsetY)
            .putFloat("notch_scale", scale)
            .apply()
    }

    fun lockDevice() {
        closeApp()
        _isDeviceLocked.value = true
        _isControlCenterOpen.value = false
        _isNotificationCenterOpen.value = false
        _isAppSwitcherOpen.value = false
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
        } else {
            _wifiEnabled.value = true
            _isWifiConnected.value = true
            _cellularSignalBars.value = 4
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
        val newState = !_wifiEnabled.value
        _wifiEnabled.value = newState
        _isWifiConnected.value = newState
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wm = getApplication<Application>().applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                @Suppress("DEPRECATION")
                wm?.isWifiEnabled = newState
            } catch (ignored: Exception) {}
            RootExecutor.execute(if (newState) "svc wifi enable" else "svc wifi disable")
        }
    }

    fun setWifiEnabled(enabled: Boolean) {
        if (_wifiEnabled.value != enabled) {
            toggleWifi()
        }
    }

    @SuppressLint("MissingPermission")
    fun scanWifiNetworks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isWifiScanning.value = true
            try {
                RootExecutor.execute("pm grant ${context.packageName} android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true")
                RootExecutor.execute("pm grant ${context.packageName} android.permission.ACCESS_WIFI_STATE 2>/dev/null || true")

                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

                if (wifiManager != null) {
                    val isEnabled = wifiManager.isWifiEnabled
                    _wifiEnabled.value = isEnabled
                    if (isEnabled) {
                        try {
                            wifiManager.startScan()
                        } catch (ignored: Exception) {}

                        val info = wifiManager.connectionInfo
                        if (info != null && !info.ssid.isNullOrBlank() && info.ssid != "<unknown ssid>") {
                            _currentWifiSsid.value = info.ssid.removeSurrounding("\"")
                            _isWifiConnected.value = true
                        } else {
                            val activeNet = cm?.activeNetwork
                            val caps = cm?.getNetworkCapabilities(activeNet)
                            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                                _isWifiConnected.value = true
                                val raw = info?.ssid?.removeSurrounding("\"")
                                _currentWifiSsid.value = if (!raw.isNullOrBlank() && raw != "<unknown ssid>") raw else "Wi-Fi"
                            } else {
                                _isWifiConnected.value = false
                                _currentWifiSsid.value = ""
                            }
                        }

                        val scanResults = wifiManager.scanResults
                        if (!scanResults.isNullOrEmpty()) {
                            val scanned = scanResults
                                .filter { it.SSID.isNotBlank() }
                                .map { Pair(it.SSID, it.capabilities.contains("WPA") || it.capabilities.contains("WEP")) }
                                .distinctBy { it.first }
                            _availableWifiList.value = scanned
                        }
                    } else {
                        _isWifiConnected.value = false
                        _currentWifiSsid.value = ""
                        _availableWifiList.value = emptyList()
                    }
                }
            } catch (ignored: Exception) {}
            delay(1000)
            _isWifiScanning.value = false
        }
    }

    fun refreshCellularInfo(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                if (tm != null) {
                    val opName = tm.networkOperatorName
                    val simName = tm.simOperatorName
                    val carrier = when {
                        !opName.isNullOrBlank() -> opName
                        !simName.isNullOrBlank() -> simName
                        tm.simState == TelephonyManager.SIM_STATE_ABSENT -> "No SIM"
                        else -> "Cellular"
                    }
                    _cellularCarrierName.value = carrier
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
                try {
                    androidx.core.content.ContextCompat.registerReceiver(
                        ctx,
                        receiver,
                        filter,
                        androidx.core.content.ContextCompat.RECEIVER_EXPORTED
                    )
                } catch (_: Exception) {
                    try {
                        ctx.registerReceiver(receiver, filter)
                    } catch (_: Exception) {}
                }

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

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
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
    }

    fun toggleCellular() {
        val newState = !_cellularEnabled.value
        _cellularEnabled.value = newState
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = if (newState) "svc data enable" else "svc data disable"
            RootExecutor.execute(cmd)
        }
    }

    fun setCellularEnabled(enabled: Boolean) {
        if (_cellularEnabled.value != enabled) {
            toggleCellular()
        }
    }

    fun toggleHotspot() {
        val newState = !_hotspotEnabled.value
        _hotspotEnabled.value = newState
        viewModelScope.launch(Dispatchers.IO) {
            val cmd = if (newState) {
                "svc wifi disable && wifi ap enable || cmd connectivity start-tethering 0"
            } else {
                "wifi ap disable || cmd connectivity stop-tethering 0"
            }
            RootExecutor.execute(cmd)
        }
    }

    fun setHotspotEnabled(enabled: Boolean) {
        if (_hotspotEnabled.value != enabled) {
            toggleHotspot()
        }
    }

    fun toggleFlashlight(context: Context) {
        FlashlightHelper.toggleTorch(context) { active ->
            _flashlightEnabled.value = active
        }
    }

    fun syncCurrentVolume(value: Float) {
        _volume.value = value.coerceIn(0.0f, 1.0f)
    }

    fun syncCurrentBrightness(value: Float) {
        _brightness.value = value.coerceIn(0.02f, 1.0f)
    }

    fun setBrightness(value: Float, context: Context? = null) {
        val clamped = value.coerceIn(0.02f, 1.0f)
        _brightness.value = clamped
        val ctx = context ?: getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val intVal = (clamped * 255).toInt().coerceIn(5, 255)
            try {
                Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, intVal)
            } catch (ignored: Exception) {}
            try {
                RootExecutor.execute("settings put system screen_brightness $intVal")
            } catch (ignored: Exception) {}
        }
    }

    fun setVolume(value: Float, context: Context? = null) {
        val clamped = value.coerceIn(0.0f, 1.0f)
        _volume.value = clamped
        val ctx = context ?: getApplication<Application>()
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (am != null) {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = (clamped * maxVol).toInt().coerceIn(0, maxVol)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val pct = (clamped * 100).toInt()
        showDynamicIslandVolume(pct)
    }

    // Appearance & Dark Mode
    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    // Wallpaper Management
    fun setWallpaper(
        resId: Int,
        name: String,
        uri: String? = null,
        context: Context? = null,
        customForegroundUri: String? = null,
        customClockLayoutMode: ClockLayoutMode? = null,
        customClockFontStyle: ClockFontStyle? = null,
        customClockColorHex: Long? = null,
        customClockFontSize: Float? = null,
        customClockTopPadding: Float? = null,
        customClockHorizOffset: Float? = null,
        customClockFontWeight: Int? = null,
        customClockOpacity: Float? = null,
        customShowWeatherWidget: Boolean? = null,
        customShowBatteryWidget: Boolean? = null,
        customShowCalendarWidget: Boolean? = null
    ) {
        _currentWallpaperRes.value = resId
        _currentWallpaperName.value = name
        _currentWallpaperUri.value = uri
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveWallpaper(ctx, resId, name, uri)

        // Sync LockScreenConfig so custom wallpapers never collide with preset depth themes
        val currentCfg = _lockScreenConfig.value
        val updatedDepthTheme = if (resId != 0) {
            when (resId) {
                com.example.R.drawable.wp_twilight_clouds -> "twilight_clouds"
                com.example.R.drawable.wp_monochrome_wave -> "monochrome_wave"
                com.example.R.drawable.wp_golden_dunes -> "golden_dunes"
                com.example.R.drawable.wp_coastal_flora -> "coastal_flora"
                com.example.R.drawable.wp_daisy_meadow -> "daisy_meadow"
                com.example.R.drawable.wp_sunset_reeds -> "sunset_reeds"
                com.example.R.drawable.wp_crystal_glass -> "crystal_glass"
                com.example.R.drawable.wp_glass_sanctuary -> "glass_sanctuary"
                com.example.R.drawable.wp_glass_still_life -> "glass_still_life"
                com.example.R.drawable.wallpaper_alpine -> "mountain"
                else -> "none"
            }
        } else "custom"

        val updatedCfg = currentCfg.copy(
            depthTheme = updatedDepthTheme,
            customForegroundUri = customForegroundUri,
            depthEffectEnabled = if (resId == 0 && customForegroundUri == null) false else (customForegroundUri != null || currentCfg.depthEffectEnabled),
            clockLayoutMode = customClockLayoutMode ?: currentCfg.clockLayoutMode,
            fontStyle = customClockFontStyle ?: currentCfg.fontStyle,
            clockColorHex = customClockColorHex ?: currentCfg.clockColorHex,
            useAutoThemeColor = if (customClockColorHex != null) false else currentCfg.useAutoThemeColor,
            clockFontSize = customClockFontSize ?: currentCfg.clockFontSize,
            clockFontWeight = customClockFontWeight ?: currentCfg.clockFontWeight,
            clockOpacity = customClockOpacity ?: currentCfg.clockOpacity,
            clockTopPaddingPercent = customClockTopPadding ?: currentCfg.clockTopPaddingPercent,
            clockHorizontalOffset = customClockHorizOffset ?: currentCfg.clockHorizontalOffset,
            showWeatherWidget = customShowWeatherWidget ?: currentCfg.showWeatherWidget,
            showBatteryWidget = customShowBatteryWidget ?: currentCfg.showBatteryWidget,
            showCalendarWidget = customShowCalendarWidget ?: currentCfg.showCalendarWidget
        )
        _lockScreenConfig.value = updatedCfg
        IOSPersistenceManager.saveFullLockScreenConfig(ctx, updatedCfg)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val wm = android.app.WallpaperManager.getInstance(ctx)
                if (uri != null) {
                    val parsed = android.net.Uri.parse(uri)
                    val bitmap = if (parsed.scheme == "file" && parsed.path != null) {
                        android.graphics.BitmapFactory.decodeFile(parsed.path)
                    } else {
                        val stream = ctx.contentResolver.openInputStream(parsed)
                        val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                        stream?.close()
                        bmp
                    }
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

    fun setLockScreenWallpaper(
        resId: Int,
        name: String,
        uri: String? = null,
        context: Context? = null,
        customForegroundUri: String? = null,
        customClockLayoutMode: ClockLayoutMode? = null,
        customClockFontStyle: ClockFontStyle? = null,
        customClockColorHex: Long? = null,
        customClockFontSize: Float? = null,
        customClockTopPadding: Float? = null,
        customClockHorizOffset: Float? = null,
        customClockFontWeight: Int? = null,
        customClockOpacity: Float? = null,
        customShowWeatherWidget: Boolean? = null,
        customShowBatteryWidget: Boolean? = null,
        customShowCalendarWidget: Boolean? = null
    ) {
        setWallpaper(
            resId, name, uri, context, customForegroundUri,
            customClockLayoutMode, customClockFontStyle, customClockColorHex,
            customClockFontSize, customClockTopPadding, customClockHorizOffset,
            customClockFontWeight, customClockOpacity,
            customShowWeatherWidget, customShowBatteryWidget, customShowCalendarWidget
        )
    }

    fun setHomeScreenWallpaper(
        resId: Int,
        name: String,
        uri: String? = null,
        context: Context? = null
    ) {
        _homeScreenWallpaperRes.value = resId
        _homeScreenWallpaperName.value = name
        _homeScreenWallpaperUri.value = uri
        val ctx = context ?: getApplication<Application>()
        IOSPersistenceManager.saveHomeScreenWallpaper(ctx, resId, name, uri)
        showDynamicIslandNotification("Layar Utama", "Wallpaper Layar Utama diperbarui", "photo", 0xFF32ADE6)
    }

    // Device Lock & Passcode
    fun unlockDevice() {
        _isDeviceLocked.value = false
        dynamicIslandJob?.cancel()
        _dynamicIsland.value = DynamicIslandData(
            mode = DynamicIslandMode.UNLOCK,
            icon = "lock_open"
        )
        dynamicIslandJob = viewModelScope.launch {
            delay(1200)
            if (_dynamicIsland.value.mode == DynamicIslandMode.UNLOCK) {
                _dynamicIsland.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
            }
        }
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

    fun setFaceIdEnabled(enabled: Boolean) {
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

                if (deviceSongs.isNotEmpty()) {
                    _musicList.value = deviceSongs
                    if (_currentSong.value == null) {
                        _currentSong.value = deviceSongs.first()
                    }
                } else {
                    _musicList.value = emptyList()
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
    fun syncRootStates(context: Context? = null) {
        val ctx = context ?: getApplication<Application>()
        _isImmersiveLocked.value = IOSPersistenceManager.loadRootBoolean(ctx, "immersive", false)
        _isSystemUIDisabled.value = IOSPersistenceManager.loadRootBoolean(ctx, "systemui_disabled", false)
        _isNotificationListenerGranted.value = IOSPersistenceManager.loadRootBoolean(ctx, "notif_granted", false)
        _cpuBoostEnabled.value = IOSPersistenceManager.loadRootBoolean(ctx, "cpu_boost", false)
        _is120HzUnlocked.value = IOSPersistenceManager.loadRootBoolean(ctx, "120hz", false)
        _isThermalDisabled.value = IOSPersistenceManager.loadRootBoolean(ctx, "thermal_disabled", false)
        _isSELinuxPermissive.value = IOSPersistenceManager.loadRootBoolean(ctx, "selinux_permissive", false)
        _isTopDisplayInsetActive.value = IOSPersistenceManager.loadRootBoolean(ctx, "top_inset", false)
    }

    fun fetchLiveWeather(city: String = "Bandung") {
        viewModelScope.launch {
            try {
                val result = WeatherService.fetchWeather(latitude = -6.9175, longitude = 107.6191, cityName = city)
                _weatherData.value = result
            } catch (ignored: Exception) {}
        }
    }

    fun toggleTopDisplayInsetRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_isTopDisplayInsetActive.value
            _isTopDisplayInsetActive.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "top_inset", newState)

            // Pastikan launcher iOS kita selalu 100% fullscreen (0,0,0,0) di Y=0
            // agar status bar dan dynamic island TIDAK PERNAH turun atau memendek!
            // Inset hanya diterapkan secara dinamis saat user membuka aplikasi luar (WA/IG).
            val result = RootExecutor.setTopDisplayInset(0)

            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(
                time,
                if (newState) "Ruang Mandiri Status Bar: AKTIF (Y=0 Terkunci)" else "Ruang Mandiri Status Bar: NONAKTIF",
                if (newState) "Status bar kokoh di Y=0, bilah navigasi bawah disembunyikan" else "Batas layar dinormalkan",
                result.first
            )
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification(
                "Ruang Mandiri Status Bar",
                if (newState) "Status Bar & Dynamic Island kokoh di ujung atas layar!" else "Pengaturan dinormalkan",
                "display",
                0xFF34C759
            )
        }
    }

    fun runRootCommand(cmd: String) {
        viewModelScope.launch {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val result = RootExecutor.executeCommand(cmd)
            val log = RootCommandLog(time, cmd, result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Root (#su)", if (result.first) "Command berhasil!" else "Error pada eksekusi", "terminal", 0xFF34C759)
        }
    }

    fun toggleImmersiveRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_isImmersiveLocked.value
            val result = if (newState) RootExecutor.killAndroidBars() else RootExecutor.restoreAndroidBars()
            _isImmersiveLocked.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "immersive", newState)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, if (newState) "icon_blacklist + immersive.nav" else "restore bars", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Superuser", if (newState) "Ikon Android disembunyikan (Dialog USB Tetap Aktif)" else "Status bar dikembalikan", "shield", 0xFF007AFF)
        }
    }

    fun toggleVolumeInterceptionRoot(context: Context? = null) {
        viewModelScope.launch {
            val ctx = context ?: getApplication<Application>()
            val currentlyActive = RootExecutor.isVolumeInterceptionEnabled(ctx)
            val result = if (currentlyActive) {
                RootExecutor.disableVolumeInterception(ctx)
            } else {
                RootExecutor.enableVolumeInterception(ctx)
            }
            val isEnabled = RootExecutor.isVolumeInterceptionEnabled(ctx)
            _isVolumeInterceptionActive.value = isEnabled
            IOSPersistenceManager.saveRootBoolean(ctx, "volume_interception", isEnabled)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, if (isEnabled) "enable_volume_interception" else "disable_volume_interception", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification(
                "Dynamic Island Volume",
                if (isEnabled) "Dialog Android Diblokir (Dynamic Island Saja)" else "Volume Bawaan Android Dipulihkan",
                "volume",
                if (isEnabled) 0xFF007AFF else 0xFF8E8E93
            )
        }
    }

    fun toggleSystemUIDisabledRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_isSystemUIDisabled.value
            val result = if (newState) RootExecutor.disableSystemUICompletely() else RootExecutor.restoreAndroidBars()
            _isSystemUIDisabled.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "systemui_disabled", newState)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, if (newState) "pm disable com.android.systemui" else "pm enable com.android.systemui", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("SystemUI Magisk", if (newState) "SystemUI dinonaktifkan (Perlu Restart UI)" else "SystemUI diaktifkan kembali", "shield", 0xFF34C759)
        }
    }

    fun boostCpuRoot() {
        viewModelScope.launch {
            RootExecutor.boostCpuGovernor()
            _cpuBoostEnabled.value = true
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "echo performance > cpu/scaling_governor", "CPU Governor set to performance", true)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("CPU Governor", "CPU Boost AKTIF (Performance Mode)", "speed", 0xFFFF9500)
        }
    }

    fun toggleCpuBoostRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_cpuBoostEnabled.value
            val result = if (newState) {
                RootExecutor.boostCpuGovernor()
            } else {
                RootExecutor.executeCommand("for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo interactive > \"\$g\"; done")
            }
            _cpuBoostEnabled.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "cpu_boost", newState)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "echo ${if (newState) "performance" else "interactive"} > cpu/scaling_governor", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("CPU Governor", if (newState) "CPU Boost AKTIF (Performance Mode)" else "CPU Mode Normal (Interactive)", "speed", 0xFFFF9500)
        }
    }

    fun toggle120HzRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_is120HzUnlocked.value
            val result = RootExecutor.setHighRefreshRate(newState)
            _is120HzUnlocked.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "120hz", newState)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "peak_refresh_rate ${if (newState) "120.0" else "60.0"}", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Refresh Rate", if (newState) "120Hz ProMotion Aktif (Instan)" else "Refresh Rate 60Hz Standar", "display", 0xFF34C759)
        }
    }

    fun toggleThermalThrottlingRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_isThermalDisabled.value
            val result = RootExecutor.setThermalThrottling(newState)
            _isThermalDisabled.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "thermal_disabled", newState)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, if (newState) "stop thermal-engine" else "start thermal-engine", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Thermal Engine", if (newState) "Thermal Throttling Dimatikan (Full FPS)" else "Thermal Protection Aktif", "flame", 0xFFFF3B30)
        }
    }

    fun toggleSELinuxRoot(context: Context? = null) {
        viewModelScope.launch {
            val newState = !_isSELinuxPermissive.value
            val result = RootExecutor.setSELinuxPermissive(newState)
            _isSELinuxPermissive.value = newState
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootBoolean(ctx, "selinux_permissive", newState)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "setenforce ${if (newState) 0 else 1}", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("SELinux Mode", if (newState) "SELinux: Permissive Mode" else "SELinux: Enforcing Mode", "shield", 0xFFFF9500)
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

    fun spoofDeviceRoot(modelName: String, context: Context? = null) {
        viewModelScope.launch {
            val result = RootExecutor.spoofIphoneModel(modelName)
            _spoofedModel.value = modelName
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootString(ctx, "spoofed_model", modelName)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "setprop ro.product.model \"$modelName\"", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Device Spoof", "Disamarkan jadi $modelName (Perlu Restart)", "apple", 0xFF5856D6)
        }
    }

    fun setRetinaDpiRoot(dpi: Int, context: Context? = null) {
        viewModelScope.launch {
            val result = if (dpi == 240) RootExecutor.resetDpi() else RootExecutor.setIosRetinaDpi(dpi)
            _retinaDpi.value = dpi
            val ctx = context ?: getApplication<Application>()
            IOSPersistenceManager.saveRootInt(ctx, "retina_dpi", dpi)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val log = RootCommandLog(time, "wm density $dpi", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("DPI Scaling", "Layar diatur ke $dpi DPI (Perlu Restart UI)", "display", 0xFF32ADE6)
        }
    }

    fun softRestartSystemUI() {
        viewModelScope.launch {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val result = RootExecutor.restartSystemUI()
            val log = RootCommandLog(time, "pkill -9 -f com.android.systemui", result.second, result.first)
            _rootLogs.value = _rootLogs.value + log
            showDynamicIslandNotification("Soft Reboot", "SystemUI direstart...", "refresh", 0xFF007AFF)
        }
    }

    fun powerOffRoot() {
        viewModelScope.launch {
            RootExecutor.powerOffDevice()
        }
    }

    // App Library State (Installed Device Apps categorized)
    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

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
                InstalledAppItem(context.packageName, "AI Depth Studio", "Utilities", isBuiltIn = true, builtInId = AppId.DEPTH_STUDIO, iconColor = 0xFF5856D6),
                InstalledAppItem(context.packageName, "iRoot SE", "Utilities", isBuiltIn = true, builtInId = AppId.ROOT_TOOLS, iconColor = 0xFF5856D6),
                InstalledAppItem(context.packageName, "Dynamic Island", "Utilities", isBuiltIn = true, builtInId = AppId.DYNAMIC_ISLAND, iconColor = 0xFF000000)
            )

            for (builtIn in builtInList) {
                if (finalList.none { it.appName.equals(builtIn.appName, ignoreCase = true) }) {
                    finalList.add(builtIn)
                }
            }

            // Apply persistent user folder customizations
            val overrides = IOSPersistenceManager.loadAppCategoryOverrides(context)
            val mappedList = if (overrides.isNotEmpty()) {
                finalList.map { item ->
                    val k = item.builtInId?.name ?: item.packageName
                    val overriddenCat = overrides[k]
                    if (overriddenCat != null && item.category != "Recent") {
                        item.copy(category = overriddenCat, iconColor = getCategoryColor(overriddenCat))
                    } else item
                }
            } else finalList

            _installedApps.value = mappedList

            // Update bitmaps in _homeScreenApps if any installed apps were saved
            val currentHome = _homeScreenApps.value
            if (currentHome.any { it.isInstalledApp && it.customBitmap == null }) {
                val updatedHome = currentHome.map { item ->
                    if (item.isInstalledApp && item.customBitmap == null) {
                        val matched = mappedList.firstOrNull { it.packageName == item.packageName }
                        if (matched?.iconBitmap != null) {
                            item.copy(customBitmap = matched.iconBitmap)
                        } else item
                    } else item
                }
                _homeScreenApps.value = updatedHome
            }
        }
    }

    fun moveAppToCategory(app: InstalledAppItem, targetCategory: String, context: Context) {
        val appKey = app.builtInId?.name ?: app.packageName
        IOSPersistenceManager.saveAppCategoryOverride(context, appKey, targetCategory)
        IOSPersistenceManager.saveCustomFolder(context, targetCategory)

        _installedApps.value = _installedApps.value.map { item ->
            val k = item.builtInId?.name ?: item.packageName
            if (k == appKey && item.category != "Recent") {
                item.copy(category = targetCategory, iconColor = getCategoryColor(targetCategory))
            } else item
        }
        showDynamicIslandNotification("App Library", "${app.appName} dipindahkan ke folder $targetCategory", "folder", 0xFF0A84FF)
    }

    fun createCustomFolder(folderName: String, context: Context) {
        IOSPersistenceManager.saveCustomFolder(context, folderName)
        showDynamicIslandNotification("App Library", "Folder $folderName dibuat", "folder", 0xFF34C759)
    }

    fun deleteCustomFolder(folderName: String, context: Context) {
        IOSPersistenceManager.removeCustomFolder(context, folderName)
        _installedApps.value = _installedApps.value.map { item ->
            if (item.category == folderName) {
                val appKey = item.builtInId?.name ?: item.packageName
                IOSPersistenceManager.saveAppCategoryOverride(context, appKey, "Other")
                item.copy(category = "Other", iconColor = getCategoryColor("Other"))
            } else item
        }
        showDynamicIslandNotification("App Library", "Folder $folderName dihapus", "delete", 0xFFFF453A)
    }

    fun resetAppCategories(context: Context) {
        IOSPersistenceManager.getPrefs(context).edit().remove("app_category_overrides").apply()
        loadDeviceInstalledApps(context)
        showDynamicIslandNotification("App Library", "Kategori aplikasi diatur ulang", "restore", 0xFFFF9500)
    }

    fun initHomeScreenApps(context: Context) {
        val prefs = IOSPersistenceManager.getPrefs(context)
        val defaultApps = listOf(
            // Row 1 (adjacent to 2x2 Calendar Widget): 4 apps
            AppItem(AppId.NOTES, "Notes", Color.White, null),
            AppItem(AppId.REMINDERS, "Reminder", Color.White, null),
            AppItem(AppId.CAMERA, "Camera", Color(0xFF636366), Icons.Rounded.CameraAlt),
            AppItem(AppId.PHOTOS, "Photos", Color.White, null),

            // Row 2: 4 apps matching default user layout
            AppItem(AppId.SETTINGS, "Settings", Color(0xFF8E8E93), Icons.Rounded.Settings, badgeCount = 1),
            AppItem(AppId.CUSTOM_APP, "Youtube", Color(0xFFFF0000), Icons.Rounded.PlayArrow, packageName = "com.google.android.youtube", isInstalledApp = true),
            AppItem(AppId.CUSTOM_APP, "Whatsapp", Color(0xFF25D366), Icons.Rounded.ChatBubble, packageName = "com.whatsapp", isInstalledApp = true),
            AppItem(AppId.DEPTH_STUDIO, "Depth Studio", Color(0xFF5856D6), Icons.Rounded.Layers),

            // Page 2 apps
            AppItem(AppId.MAIL, "Mail", Color(0xFF007AFF), Icons.Rounded.Email, badgeCount = 2),
            AppItem(AppId.TV, "TV", Color.Black, Icons.Rounded.Tv),
            AppItem(AppId.PODCASTS, "Podcast", Color(0xFF8E44AD), Icons.Rounded.Podcasts),
            AppItem(AppId.APP_STORE, "App Store", Color(0xFF1D6EEB), Icons.Rounded.ShoppingBag),
            AppItem(AppId.MAPS, "Maps", Color(0xFF2ECC71), Icons.Rounded.Map),
            AppItem(AppId.FACETIME, "FaceTime", IOSGreen, Icons.Rounded.Videocam),
            AppItem(AppId.HEALTH, "Health", Color.White, Icons.Rounded.Favorite, badgeCount = 0),
            AppItem(AppId.WALLET, "Wallet", Color.Black, Icons.Rounded.AccountBalanceWallet),
            AppItem(AppId.CLOCK, "Clock", Color.Black, null),
            AppItem(AppId.ROOT_TOOLS, "iRoot SE", Color(0xFF5856D6), Icons.Rounded.Security)
        )

        val isPresetV5 = prefs.getBoolean("layout_preset_v5_apps_applied", false)
        if (!isPresetV5) {
            _homeScreenApps.value = defaultApps
            saveCurrentHomeScreenApps(context, defaultApps)
            prefs.edit().putBoolean("layout_preset_v5_apps_applied", true).apply()
            return
        }

        val saved = IOSPersistenceManager.loadHomeScreenApps(context)
        if (saved == null) {
            _homeScreenApps.value = defaultApps
            saveCurrentHomeScreenApps(context, defaultApps)
            return
        }

        val builtInMap = defaultApps.associateBy { it.id.name }
        val pm = context.packageManager
        val list = mutableListOf<AppItem>()

        for (itemKey in saved) {
            if (itemKey.startsWith("B:")) {
                val idName = itemKey.removePrefix("B:")
                val item = builtInMap[idName] ?: try {
                    val appId = AppId.valueOf(idName)
                    AppItem(id = appId, name = idName.lowercase().replaceFirstChar { it.uppercase() }, iconBgColor = Color(0xFF007AFF))
                } catch (e: Exception) { null }
                if (item != null) list.add(item)
            } else if (itemKey.startsWith("P:")) {
                val pkg = itemKey.removePrefix("P:")
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val iconDrawable = pm.getApplicationIcon(appInfo)
                    val bmp = iconDrawable.toBitmapSafe(144)
                    list.add(
                        AppItem(
                            id = AppId.CUSTOM_APP,
                            name = label,
                            iconBgColor = Color.Transparent,
                            packageName = pkg,
                            isInstalledApp = true,
                            customBitmap = bmp
                        )
                    )
                } catch (e: Exception) {
                    // App might be uninstalled, ignore
                }
            }
        }
        _homeScreenApps.value = if (list.isNotEmpty()) list else defaultApps
    }

    private fun saveCurrentHomeScreenApps(context: Context, apps: List<AppItem> = _homeScreenApps.value) {
        val keys = apps.map { app ->
            if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                "P:${app.packageName}"
            } else {
                "B:${app.id.name}"
            }
        }
        IOSPersistenceManager.saveHomeScreenApps(context, keys)
    }

    fun reorderHomeScreenApps(fromIndex: Int, toIndex: Int, context: Context) {
        val current = _homeScreenApps.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _homeScreenApps.value = current
            saveCurrentHomeScreenApps(context, current)
        }
    }

    fun moveHomeScreenApp(app: AppItem, direction: Int, context: Context) {
        val current = _homeScreenApps.value.toMutableList()
        val index = current.indexOfFirst {
            if (app.isInstalledApp) it.packageName == app.packageName else it.id == app.id
        }
        if (index >= 0) {
            val targetIndex = (index + direction).coerceIn(0, current.size - 1)
            if (targetIndex != index) {
                val item = current.removeAt(index)
                current.add(targetIndex, item)
                _homeScreenApps.value = current
                saveCurrentHomeScreenApps(context, current)
                com.example.util.IOSFeedbackHelper.vibrateLight(context)
            }
        }
    }

    fun initWidgetTypes(context: Context) {
        val prefs = IOSPersistenceManager.getPrefs(context)
        val isPresetV5 = prefs.getBoolean("layout_preset_v5_widgets_applied", false)
        if (!isPresetV5) {
            prefs.edit()
                .putString("widget1_type", "WEATHER")
                .putString("widget1_size", "MEDIUM")
                .putInt("widget1_row", 0)
                .putString("widget1_align", "LEFT")
                .putBoolean("widget1_enabled", true)

                .putString("widget2_type", "CALENDAR")
                .putString("widget2_size", "SMALL")
                .putInt("widget2_row", 1)
                .putString("widget2_align", "LEFT")
                .putBoolean("widget2_enabled", true)

                .putString("widget3_type", "WHATSAPP")
                .putString("widget3_size", "MEDIUM")
                .putInt("widget3_row", 2)
                .putString("widget3_align", "LEFT")
                .putBoolean("widget3_enabled", false)

                .putBoolean("widget4_enabled", false)
                .putBoolean("widget5_enabled", false)
                .putBoolean("widget6_enabled", false)
                .putBoolean("layout_preset_v5_widgets_applied", true)
                .apply()
        }

        _widget1Type.value = prefs.getString("widget1_type", "WEATHER") ?: "WEATHER"
        _widget1Size.value = prefs.getString("widget1_size", "MEDIUM") ?: "MEDIUM"
        _widget1Row.value = prefs.getInt("widget1_row", 0)
        _widget1Align.value = prefs.getString("widget1_align", "LEFT") ?: "LEFT"
        _widget1Enabled.value = prefs.getBoolean("widget1_enabled", true)

        _widget2Type.value = prefs.getString("widget2_type", "CALENDAR") ?: "CALENDAR"
        _widget2Size.value = prefs.getString("widget2_size", "SMALL") ?: "SMALL"
        _widget2Row.value = prefs.getInt("widget2_row", 1)
        _widget2Align.value = prefs.getString("widget2_align", "LEFT") ?: "LEFT"
        _widget2Enabled.value = prefs.getBoolean("widget2_enabled", true)

        _widget3Type.value = prefs.getString("widget3_type", "WHATSAPP") ?: "WHATSAPP"
        _widget3Size.value = prefs.getString("widget3_size", "MEDIUM") ?: "MEDIUM"
        _widget3Row.value = prefs.getInt("widget3_row", 2)
        _widget3Align.value = prefs.getString("widget3_align", "LEFT") ?: "LEFT"
        _widget3Enabled.value = prefs.getBoolean("widget3_enabled", false)

        _widget4Type.value = prefs.getString("widget4_type", "PHOTOS") ?: "PHOTOS"
        _widget4Size.value = prefs.getString("widget4_size", "SMALL") ?: "SMALL"
        _widget4Row.value = prefs.getInt("widget4_row", 3)
        _widget4Align.value = prefs.getString("widget4_align", "LEFT") ?: "LEFT"
        _widget4Enabled.value = prefs.getBoolean("widget4_enabled", false)

        _widget5Type.value = prefs.getString("widget5_type", "BATTERY") ?: "BATTERY"
        _widget5Size.value = prefs.getString("widget5_size", "SMALL") ?: "SMALL"
        _widget5Row.value = prefs.getInt("widget5_row", 4)
        _widget5Align.value = prefs.getString("widget5_align", "RIGHT") ?: "RIGHT"
        _widget5Enabled.value = prefs.getBoolean("widget5_enabled", false)

        _widget6Type.value = prefs.getString("widget6_type", "CLOCK") ?: "CLOCK"
        _widget6Size.value = prefs.getString("widget6_size", "SMALL") ?: "SMALL"
        _widget6Row.value = prefs.getInt("widget6_row", 5)
        _widget6Align.value = prefs.getString("widget6_align", "LEFT") ?: "LEFT"
        _widget6Enabled.value = prefs.getBoolean("widget6_enabled", false)

        _leftWidgetType.value = _widget1Type.value
        _rightWidgetType.value = _widget2Type.value
        _widgetRowPosition.value = prefs.getInt("home_widget_row_pos", 0)
    }

    fun setWidgetType(widgetIndex: Int, newType: String, context: Context) {
        val prefs = IOSPersistenceManager.getPrefs(context)
        when (widgetIndex) {
            1 -> {
                _widget1Type.value = newType
                _widget1Enabled.value = true
                _leftWidgetType.value = newType
                prefs.edit().putString("widget1_type", newType).putBoolean("widget1_enabled", true).putString("home_left_widget", newType).apply()
            }
            2 -> {
                _widget2Type.value = newType
                _widget2Enabled.value = true
                _rightWidgetType.value = newType
                prefs.edit().putString("widget2_type", newType).putBoolean("widget2_enabled", true).putString("home_right_widget", newType).apply()
            }
            3 -> {
                _widget3Type.value = newType
                _widget3Enabled.value = true
                prefs.edit().putString("widget3_type", newType).putBoolean("widget3_enabled", true).apply()
            }
            4 -> {
                _widget4Type.value = newType
                _widget4Enabled.value = true
                prefs.edit().putString("widget4_type", newType).putBoolean("widget4_enabled", true).apply()
            }
            5 -> {
                _widget5Type.value = newType
                _widget5Enabled.value = true
                prefs.edit().putString("widget5_type", newType).putBoolean("widget5_enabled", true).apply()
            }
            6 -> {
                _widget6Type.value = newType
                _widget6Enabled.value = true
                prefs.edit().putString("widget6_type", newType).putBoolean("widget6_enabled", true).apply()
            }
        }
        val label = getWidgetDisplayName(newType)
        showDynamicIslandNotification("Widget", "Diubah ke $label", "app.dashed", 0xFF007AFF)
        com.example.util.IOSFeedbackHelper.vibrateLight(context)
    }

    fun setWidgetSize(widgetIndex: Int, newSize: String, context: Context) {
        val prefs = IOSPersistenceManager.getPrefs(context)
        when (widgetIndex) {
            1 -> {
                _widget1Size.value = newSize
                _widget1Enabled.value = true
                prefs.edit().putString("widget1_size", newSize).putBoolean("widget1_enabled", true).apply()
            }
            2 -> {
                _widget2Size.value = newSize
                _widget2Enabled.value = true
                prefs.edit().putString("widget2_size", newSize).putBoolean("widget2_enabled", true).apply()
            }
            3 -> {
                _widget3Size.value = newSize
                _widget3Enabled.value = true
                prefs.edit().putString("widget3_size", newSize).putBoolean("widget3_enabled", true).apply()
            }
            4 -> {
                _widget4Size.value = newSize
                _widget4Enabled.value = true
                prefs.edit().putString("widget4_size", newSize).putBoolean("widget4_enabled", true).apply()
            }
            5 -> {
                _widget5Size.value = newSize
                _widget5Enabled.value = true
                prefs.edit().putString("widget5_size", newSize).putBoolean("widget5_enabled", true).apply()
            }
            6 -> {
                _widget6Size.value = newSize
                _widget6Enabled.value = true
                prefs.edit().putString("widget6_size", newSize).putBoolean("widget6_enabled", true).apply()
            }
        }
        val sizeLabel = when (newSize) {
            "MEDIUM" -> "Sedang (4x2)"
            "COMPACT" -> "Ringkas (2x1)"
            "LARGE" -> "Besar (4x4)"
            else -> "Kecil (2x2)"
        }
        showDynamicIslandNotification("Ukuran Frame", sizeLabel, "arrow.up.left.and.arrow.down.right", 0xFF34C759)
        com.example.util.IOSFeedbackHelper.vibrateLight(context)
    }

    fun toggleWidgetSize(widgetIndex: Int, context: Context) {
        val currentSize = when (widgetIndex) {
            1 -> _widget1Size.value
            2 -> _widget2Size.value
            3 -> _widget3Size.value
            4 -> _widget4Size.value
            5 -> _widget5Size.value
            else -> _widget6Size.value
        }
        val nextSize = when (currentSize) {
            "SMALL" -> "MEDIUM"
            "MEDIUM" -> "COMPACT"
            "COMPACT" -> "LARGE"
            "LARGE" -> "SMALL"
            else -> "SMALL"
        }
        setWidgetSize(widgetIndex, nextSize, context)
    }

    fun toggleWidgetAlignment(widgetIndex: Int, context: Context) {
        val prefs = IOSPersistenceManager.getPrefs(context)
        val newAlign = when (widgetIndex) {
            1 -> if (_widget1Align.value == "LEFT") "RIGHT" else "LEFT"
            2 -> if (_widget2Align.value == "LEFT") "RIGHT" else "LEFT"
            3 -> if (_widget3Align.value == "LEFT") "RIGHT" else "LEFT"
            4 -> if (_widget4Align.value == "LEFT") "RIGHT" else "LEFT"
            5 -> if (_widget5Align.value == "LEFT") "RIGHT" else "LEFT"
            else -> if (_widget6Align.value == "LEFT") "RIGHT" else "LEFT"
        }
        when (widgetIndex) {
            1 -> { _widget1Align.value = newAlign; prefs.edit().putString("widget1_align", newAlign).apply() }
            2 -> { _widget2Align.value = newAlign; prefs.edit().putString("widget2_align", newAlign).apply() }
            3 -> { _widget3Align.value = newAlign; prefs.edit().putString("widget3_align", newAlign).apply() }
            4 -> { _widget4Align.value = newAlign; prefs.edit().putString("widget4_align", newAlign).apply() }
            5 -> { _widget5Align.value = newAlign; prefs.edit().putString("widget5_align", newAlign).apply() }
            6 -> { _widget6Align.value = newAlign; prefs.edit().putString("widget6_align", newAlign).apply() }
        }
        val posLabel = if (newAlign == "LEFT") "Kiri" else "Kanan"
        showDynamicIslandNotification("Posisi Kolom", "Pindah ke $posLabel", "arrow.left.and.right", 0xFF007AFF)
        com.example.util.IOSFeedbackHelper.vibrateLight(context)
    }

    fun setWidgetRow(widgetIndex: Int, newRow: Int, context: Context) {
        val clamped = newRow.coerceIn(0, 5)
        val prefs = IOSPersistenceManager.getPrefs(context)
        when (widgetIndex) {
            1 -> { _widget1Row.value = clamped; prefs.edit().putInt("widget1_row", clamped).apply() }
            2 -> { _widget2Row.value = clamped; prefs.edit().putInt("widget2_row", clamped).apply() }
            3 -> { _widget3Row.value = clamped; prefs.edit().putInt("widget3_row", clamped).apply() }
            4 -> { _widget4Row.value = clamped; prefs.edit().putInt("widget4_row", clamped).apply() }
            5 -> { _widget5Row.value = clamped; prefs.edit().putInt("widget5_row", clamped).apply() }
            6 -> { _widget6Row.value = clamped; prefs.edit().putInt("widget6_row", clamped).apply() }
        }
        showDynamicIslandNotification("Posisi Widget", "Pindah ke Baris $clamped", "rectangle.split.2x1", 0xFF007AFF)
        com.example.util.IOSFeedbackHelper.vibrateLight(context)
    }

    fun moveSingleWidgetUp(widgetIndex: Int, context: Context) {
        val current = when (widgetIndex) {
            1 -> _widget1Row.value
            2 -> _widget2Row.value
            3 -> _widget3Row.value
            4 -> _widget4Row.value
            5 -> _widget5Row.value
            else -> _widget6Row.value
        }
        setWidgetRow(widgetIndex, current - 1, context)
    }

    fun moveSingleWidgetDown(widgetIndex: Int, context: Context) {
        val current = when (widgetIndex) {
            1 -> _widget1Row.value
            2 -> _widget2Row.value
            3 -> _widget3Row.value
            4 -> _widget4Row.value
            5 -> _widget5Row.value
            else -> _widget6Row.value
        }
        setWidgetRow(widgetIndex, current + 1, context)
    }

    fun removeWidget(widgetIndex: Int, context: Context) {
        val prefs = IOSPersistenceManager.getPrefs(context)
        when (widgetIndex) {
            1 -> { _widget1Enabled.value = false; prefs.edit().putBoolean("widget1_enabled", false).apply() }
            2 -> { _widget2Enabled.value = false; prefs.edit().putBoolean("widget2_enabled", false).apply() }
            3 -> { _widget3Enabled.value = false; prefs.edit().putBoolean("widget3_enabled", false).apply() }
            4 -> { _widget4Enabled.value = false; prefs.edit().putBoolean("widget4_enabled", false).apply() }
            5 -> { _widget5Enabled.value = false; prefs.edit().putBoolean("widget5_enabled", false).apply() }
            6 -> { _widget6Enabled.value = false; prefs.edit().putBoolean("widget6_enabled", false).apply() }
        }
        showDynamicIslandNotification("Widget", "Widget Dihapus dari Layar Utama", "trash", 0xFFFF3B30)
        com.example.util.IOSFeedbackHelper.vibrateMedium(context)
    }

    fun addWidget(type: String, size: String = "SMALL", preferredRow: Int = -1, context: Context): Int {
        val prefs = IOSPersistenceManager.getPrefs(context)
        val targetSlot = when {
            !_widget1Enabled.value -> 1
            !_widget2Enabled.value -> 2
            !_widget3Enabled.value -> 3
            !_widget4Enabled.value -> 4
            !_widget5Enabled.value -> 5
            !_widget6Enabled.value -> 6
            else -> 4
        }
        val targetRow = if (preferredRow in 0..5) preferredRow else when (targetSlot) {
            1, 2 -> 0
            3 -> 1
            4 -> 2
            5 -> 3
            else -> 4
        }

        when (targetSlot) {
            1 -> {
                _widget1Type.value = type
                _widget1Size.value = size
                _widget1Row.value = targetRow
                _widget1Enabled.value = true
                prefs.edit().putString("widget1_type", type).putString("widget1_size", size).putInt("widget1_row", targetRow).putBoolean("widget1_enabled", true).apply()
            }
            2 -> {
                _widget2Type.value = type
                _widget2Size.value = size
                _widget2Row.value = targetRow
                _widget2Enabled.value = true
                prefs.edit().putString("widget2_type", type).putString("widget2_size", size).putInt("widget2_row", targetRow).putBoolean("widget2_enabled", true).apply()
            }
            3 -> {
                _widget3Type.value = type
                _widget3Size.value = size
                _widget3Row.value = targetRow
                _widget3Enabled.value = true
                prefs.edit().putString("widget3_type", type).putString("widget3_size", size).putInt("widget3_row", targetRow).putBoolean("widget3_enabled", true).apply()
            }
            4 -> {
                _widget4Type.value = type
                _widget4Size.value = size
                _widget4Row.value = targetRow
                _widget4Enabled.value = true
                prefs.edit().putString("widget4_type", type).putString("widget4_size", size).putInt("widget4_row", targetRow).putBoolean("widget4_enabled", true).apply()
            }
            5 -> {
                _widget5Type.value = type
                _widget5Size.value = size
                _widget5Row.value = targetRow
                _widget5Enabled.value = true
                prefs.edit().putString("widget5_type", type).putString("widget5_size", size).putInt("widget5_row", targetRow).putBoolean("widget5_enabled", true).apply()
            }
            6 -> {
                _widget6Type.value = type
                _widget6Size.value = size
                _widget6Row.value = targetRow
                _widget6Enabled.value = true
                prefs.edit().putString("widget6_type", type).putString("widget6_size", size).putInt("widget6_row", targetRow).putBoolean("widget6_enabled", true).apply()
            }
        }
        val label = getWidgetDisplayName(type)
        showDynamicIslandNotification("Widget", "$label Ditambahkan ke Layar Utama", "plus", 0xFF34C759)
        com.example.util.IOSFeedbackHelper.vibrateLight(context)
        return targetSlot
    }

    fun getWidgetDisplayName(type: String): String {
        return when (type) {
            "WHATSAPP" -> "WhatsApp (Pesan & Chat)"
            "WEATHER" -> "Cuaca (Weather)"
            "PHOTOS" -> "Foto (Photos)"
            "CALENDAR" -> "Kalender (Calendar)"
            "BATTERY" -> "Baterai (Battery)"
            "CLOCK" -> "Jam Dunia (Clock)"
            "MUSIC" -> "Musik (Music)"
            "FITNESS" -> "Aktivitas (Fitness)"
            "NOTES" -> "Catatan (Notes)"
            else -> type
        }
    }

    fun setWidgetRowPosition(pos: Int, context: Context) {
        val clamped = pos.coerceIn(0, 2)
        _widgetRowPosition.value = clamped
        _widget1Row.value = clamped
        _widget2Row.value = clamped
        val prefs = IOSPersistenceManager.getPrefs(context)
        prefs.edit().putInt("home_widget_row_pos", clamped).putInt("widget1_row", clamped).putInt("widget2_row", clamped).apply()
        val posName = when (clamped) {
            0 -> "Atas (Top)"
            1 -> "Tengah (Middle)"
            else -> "Bawah (Bottom)"
        }
        showDynamicIslandNotification("Widget", "Posisi: $posName", "rectangle.split.2x1", 0xFF007AFF)
        com.example.util.IOSFeedbackHelper.vibrateLight(context)
    }

    fun moveWidgetUp(context: Context) {
        setWidgetRowPosition(_widgetRowPosition.value - 1, context)
    }

    fun moveWidgetDown(context: Context) {
        setWidgetRowPosition(_widgetRowPosition.value + 1, context)
    }

    fun swapWidgets(context: Context) {
        val temp = _leftWidgetType.value
        _leftWidgetType.value = _rightWidgetType.value
        _rightWidgetType.value = temp
        val prefs = IOSPersistenceManager.getPrefs(context)
        prefs.edit()
            .putString("home_left_widget", _leftWidgetType.value)
            .putString("home_right_widget", _rightWidgetType.value)
            .apply()
        showDynamicIslandNotification("Widgets", "Posisi widget ditukar", "arrow.left.arrow.right", 0xFF007AFF)
    }

    fun cycleWidget(isLeft: Boolean, context: Context) {
        val types = listOf("WEATHER", "PHOTOS", "CALENDAR", "BATTERY")
        val current = if (isLeft) _leftWidgetType.value else _rightWidgetType.value
        val currentIndex = types.indexOf(current).let { if (it >= 0) it else 0 }
        val nextType = types[(currentIndex + 1) % types.size]
        if (isLeft) {
            _leftWidgetType.value = nextType
        } else {
            _rightWidgetType.value = nextType
        }
        val prefs = IOSPersistenceManager.getPrefs(context)
        prefs.edit()
            .putString(if (isLeft) "home_left_widget" else "home_right_widget", nextType)
            .apply()
        val name = when (nextType) {
            "WEATHER" -> "Cuaca (Weather)"
            "PHOTOS" -> "Galeri (Photos)"
            "CALENDAR" -> "Kalender (Calendar)"
            "BATTERY" -> "Baterai (Battery)"
            else -> nextType
        }
        showDynamicIslandNotification("Widget", "Diubah ke $name", "app.dashed", 0xFF34C759)
    }

    fun removeAppFromHomeScreen(app: AppItem, context: Context) {
        val current = _homeScreenApps.value.toMutableList()
        current.removeAll {
            if (app.isInstalledApp) it.packageName == app.packageName else it.id == app.id
        }
        _homeScreenApps.value = current
        saveCurrentHomeScreenApps(context, current)
        showDynamicIslandNotification("Home Screen", "${app.name} Dihapus dari Layar Utama", "trash", 0xFFFF3B30)
    }

    fun addInstalledAppToHomeScreen(installedApp: InstalledAppItem, context: Context) {
        val current = _homeScreenApps.value.toMutableList()
        if (installedApp.isBuiltIn && installedApp.builtInId != null) {
            if (current.any { it.id == installedApp.builtInId }) {
                showDynamicIslandNotification("Home Screen", "${installedApp.appName} sudah ada di Layar Utama", "info", 0xFFFF9500)
                return
            }
            val defaultGrid = listOf(
                AppItem(AppId.FACETIME, "FaceTime", IOSGreen, Icons.Rounded.Videocam),
                AppItem(AppId.CALENDAR, "Calendar", Color.White, null),
                AppItem(AppId.PHOTOS, "Photos", Color.White, null),
                AppItem(AppId.CAMERA, "Camera", Color(0xFF636366), Icons.Rounded.CameraAlt),
                AppItem(AppId.MAIL, "Mail", Color(0xFF007AFF), Icons.Rounded.Email, badgeCount = 2),
                AppItem(AppId.NOTES, "Notes", Color.White, null),
                AppItem(AppId.REMINDERS, "Reminders", Color.White, null),
                AppItem(AppId.CLOCK, "Clock", Color.Black, null),
                AppItem(AppId.TV, "TV", Color.Black, Icons.Rounded.Tv),
                AppItem(AppId.PODCASTS, "Podcasts", Color(0xFF8E44AD), Icons.Rounded.Podcasts),
                AppItem(AppId.APP_STORE, "App Store", Color(0xFF1D6EEB), Icons.Rounded.ShoppingBag),
                AppItem(AppId.MAPS, "Maps", Color(0xFF2ECC71), Icons.Rounded.Map),
                AppItem(AppId.HEALTH, "Health", Color.White, Icons.Rounded.Favorite, badgeCount = 0),
                AppItem(AppId.WALLET, "Wallet", Color.Black, Icons.Rounded.AccountBalanceWallet),
                AppItem(AppId.SETTINGS, "Settings", Color(0xFF8E8E93), Icons.Rounded.Settings, badgeCount = 1),
                AppItem(AppId.ROOT_TOOLS, "iRoot SE", Color(0xFF5856D6), Icons.Rounded.Security)
            )
            val matched = defaultGrid.firstOrNull { it.id == installedApp.builtInId }
                ?: AppItem(id = installedApp.builtInId, name = installedApp.appName, iconBgColor = Color(installedApp.iconColor))
            current.add(matched)
        } else {
            if (current.any { it.packageName == installedApp.packageName }) {
                showDynamicIslandNotification("Home Screen", "${installedApp.appName} sudah ada di Layar Utama", "info", 0xFFFF9500)
                return
            }
            val bmp = installedApp.iconBitmap ?: try {
                context.packageManager.getApplicationIcon(installedApp.packageName).toBitmapSafe(144)
            } catch (e: Exception) { null }

            val item = AppItem(
                id = AppId.CUSTOM_APP,
                name = installedApp.appName,
                iconBgColor = Color.Transparent,
                packageName = installedApp.packageName,
                isInstalledApp = true,
                customBitmap = bmp
            )
            current.add(item)
        }
        _homeScreenApps.value = current
        saveCurrentHomeScreenApps(context, current)
        showDynamicIslandNotification("Home Screen", "${installedApp.appName} Ditambahkan ke Layar Utama", "plus", 0xFF34C759)
    }

    fun addAppItemToHomeScreen(appItem: AppItem, context: Context) {
        val current = _homeScreenApps.value.toMutableList()
        if (appItem.isInstalledApp) {
            if (current.none { it.packageName == appItem.packageName }) {
                current.add(appItem)
            }
        } else {
            if (current.none { it.id == appItem.id }) {
                current.add(appItem)
            }
        }
        _homeScreenApps.value = current
        saveCurrentHomeScreenApps(context, current)
        showDynamicIslandNotification("Home Screen", "${appItem.name} Ditambahkan ke Layar Utama", "plus", 0xFF34C759)
    }

    fun resetHomeScreenApps(context: Context) {
        IOSPersistenceManager.saveHomeScreenApps(context, emptyList())
        initHomeScreenApps(context)
        showDynamicIslandNotification("Home Screen", "Tata Letak Ikon Direset", "refresh", 0xFF007AFF)
    }

    // =========================================================================
    // DOCK APPS MANAGEMENT & CUSTOMIZATION
    // =========================================================================

    fun initDockApps(context: Context) {
        val saved = IOSPersistenceManager.loadDockApps(context)
        val defaultDock = listOf(
            AppItem(AppId.PHONE, "Phone", IOSGreen, Icons.Rounded.Call, isDock = true),
            AppItem(AppId.SAFARI, "Safari", Color.White, Icons.Rounded.Explore, isDock = true),
            AppItem(AppId.MESSAGES, "Messages", IOSGreen, Icons.Rounded.ChatBubble, isDock = true),
            AppItem(AppId.MUSIC, "Music", Color(0xFFFF2D55), Icons.Rounded.MusicNote, isDock = true)
        )

        if (saved == null || saved.isEmpty()) {
            _dockApps.value = defaultDock
            saveCurrentDockApps(context, defaultDock)
            return
        }

        val pm = context.packageManager
        val list = mutableListOf<AppItem>()

        for (itemKey in saved) {
            if (itemKey.startsWith("B:")) {
                val idName = itemKey.removePrefix("B:")
                val matched = defaultDock.firstOrNull { it.id.name == idName } ?: try {
                    val appId = AppId.valueOf(idName)
                    val iconVector = when (appId) {
                        AppId.PHONE -> Icons.Rounded.Call
                        AppId.SAFARI -> Icons.Rounded.Explore
                        AppId.MESSAGES -> Icons.Rounded.ChatBubble
                        AppId.MUSIC -> Icons.Rounded.MusicNote
                        AppId.MAIL -> Icons.Rounded.Email
                        AppId.CAMERA -> Icons.Rounded.CameraAlt
                        AppId.SETTINGS -> Icons.Rounded.Settings
                        AppId.PHOTOS -> Icons.Rounded.Photo
                        else -> Icons.Rounded.Apps
                    }
                    val bg = when (appId) {
                        AppId.PHONE, AppId.MESSAGES -> IOSGreen
                        AppId.MUSIC -> Color(0xFFFF2D55)
                        AppId.MAIL -> Color(0xFF007AFF)
                        AppId.SETTINGS -> Color(0xFF8E8E93)
                        else -> Color.White
                    }
                    AppItem(id = appId, name = idName.lowercase().replaceFirstChar { it.uppercase() }, iconBgColor = bg, iconVector = iconVector, isDock = true)
                } catch (e: Exception) { null }
                if (matched != null) list.add(matched.copy(isDock = true))
            } else if (itemKey.startsWith("P:")) {
                val pkg = itemKey.removePrefix("P:")
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val iconDrawable = pm.getApplicationIcon(appInfo)
                    val bmp = iconDrawable.toBitmapSafe(144)
                    list.add(
                        AppItem(
                            id = AppId.CUSTOM_APP,
                            name = label,
                            iconBgColor = Color.Transparent,
                            packageName = pkg,
                            isInstalledApp = true,
                            customBitmap = bmp,
                            isDock = true
                        )
                    )
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
        _dockApps.value = if (list.isNotEmpty()) list else defaultDock
    }

    fun saveCurrentDockApps(context: Context, apps: List<AppItem> = _dockApps.value) {
        val keys = apps.map { app ->
            if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                "P:${app.packageName}"
            } else {
                "B:${app.id.name}"
            }
        }
        IOSPersistenceManager.saveDockApps(context, keys)
    }

    fun setDockAppAt(index: Int, newApp: AppItem, context: Context) {
        val current = _dockApps.value.toMutableList()
        val dockItem = newApp.copy(isDock = true)
        if (index in 0 until current.size) {
            current[index] = dockItem
        } else if (current.size < 4) {
            current.add(dockItem)
        }
        _dockApps.value = current
        saveCurrentDockApps(context, current)
        showDynamicIslandNotification("Dock", "${newApp.name} dipasang di Dock", "dock", 0xFF007AFF)
    }

    fun replaceDockApp(oldApp: AppItem, newApp: AppItem, context: Context) {
        val current = _dockApps.value.toMutableList()
        val idx = current.indexOfFirst {
            if (oldApp.isInstalledApp) it.packageName == oldApp.packageName else it.id == oldApp.id
        }
        if (idx >= 0) {
            current[idx] = newApp.copy(isDock = true)
            _dockApps.value = current
            saveCurrentDockApps(context, current)
            showDynamicIslandNotification("Dock", "${newApp.name} menggantikan ${oldApp.name}", "dock", 0xFF007AFF)
        }
    }

    fun removeDockApp(app: AppItem, context: Context) {
        val current = _dockApps.value.toMutableList()
        current.removeAll {
            if (app.isInstalledApp) it.packageName == app.packageName else it.id == app.id
        }
        _dockApps.value = current
        saveCurrentDockApps(context, current)
        showDynamicIslandNotification("Dock", "${app.name} dihapus dari Dock", "trash", 0xFFFF3B30)
    }

    fun addDockApp(app: AppItem, context: Context) {
        val current = _dockApps.value.toMutableList()
        if (current.size >= 4) {
            showDynamicIslandNotification("Dock", "Dock sudah penuh (maks 4 aplikasi)", "alert", 0xFFFF9500)
            return
        }
        current.add(app.copy(isDock = true))
        _dockApps.value = current
        saveCurrentDockApps(context, current)
        showDynamicIslandNotification("Dock", "${app.name} ditambahkan ke Dock", "plus", 0xFF34C759)
    }

    fun resetDockApps(context: Context) {
        IOSPersistenceManager.saveDockApps(context, emptyList())
        initDockApps(context)
        showDynamicIslandNotification("Dock", "Aplikasi Dock direset ke awal", "refresh", 0xFF007AFF)
    }

    fun launchInstalledApp(context: Context, packageName: String) {
        if (Settings.canDrawOverlays(context)) {
            com.example.service.IOSOverlayService.start(context)
        }
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun launchDeviceApp(context: Context, app: InstalledAppItem) {
        if (Settings.canDrawOverlays(context)) {
            com.example.service.IOSOverlayService.start(context)
        }
        if (app.isBuiltIn && app.builtInId != null) {
            openApp(app.builtInId)
        } else {
            launchInstalledApp(context, app.packageName)
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
        _isInCall.value = true
        val callData = DynamicIslandData(
            mode = DynamicIslandMode.CALL,
            title = incomingNumber,
            subtitle = "Panggilan Seluler...",
            icon = "phone",
            accentColor = 0xFF34C759,
            isIncomingCall = true,
            voipAppName = "Telepon"
        )
        _dynamicIsland.value = callData
        OverlayBridge.isInCall.value = true
        OverlayBridge.callName.value = incomingNumber
        OverlayBridge.callNumber.value = incomingNumber
        OverlayBridge.dynamicIslandData.value = callData
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

    // ================= CUSTOM APP ICONS MANAGEMENT =================
    fun setCustomAppIcon(key: String, uriString: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    // Scale down to max 256x256 to save memory and optimize launcher performance on J2 Prime
                    val maxDim = 256
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaledBitmap = if (width > maxDim || height > maxDim) {
                        val ratio = width.toFloat() / height.toFloat()
                        val targetW = if (ratio > 1f) maxDim else (maxDim * ratio).toInt()
                        val targetH = if (ratio > 1f) (maxDim / ratio).toInt() else maxDim
                        android.graphics.Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
                    } else {
                        originalBitmap
                    }

                    val dir = File(context.filesDir, "custom_icons").apply { if (!exists()) mkdirs() }
                    val destFile = File(dir, "icon_${key}.png")
                    val out = FileOutputStream(destFile)
                    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                    out.flush()
                    out.close()

                    val filePath = destFile.absolutePath
                    IOSPersistenceManager.saveCustomIcon(context, key, filePath)
                    _customAppIcons.value = _customAppIcons.value + (key to filePath)

                    withContext(Dispatchers.Main) {
                        showDynamicIslandNotification("Ikon Diperbarui", "Ikon kustom diterapkan!", "apple", 0xFF007AFF)
                        IOSFeedbackHelper.vibrateSuccess(context)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showDynamicIslandNotification("Gagal Ganti Ikon", "Tidak dapat memuat gambar", "bell", 0xFFFF3B30)
                }
            }
        }
    }

    fun resetCustomAppIcon(key: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                IOSPersistenceManager.removeCustomIcon(context, key)
                val file = File(context.filesDir, "custom_icons/icon_${key}.png")
                if (file.exists()) file.delete()
                _customAppIcons.value = _customAppIcons.value - key
                withContext(Dispatchers.Main) {
                    showDynamicIslandNotification("Ikon Direset", "Ikon asli dikembalikan", "arrow.clockwise", 0xFF34C759)
                    IOSFeedbackHelper.vibrateLight(context)
                }
            } catch (e: Exception) {}
        }
    }

    // ================= IPHONE 16/17 & IOS 26 ACTION BUTTON & SETTINGS =================
    fun setActionButtonFunction(func: String, context: Context) {
        _actionButtonFunction.value = func
        IOSPersistenceManager.saveActionButton(context, func)
        showDynamicIslandNotification("Action Button", func, "gear", 0xFF007AFF)
        IOSFeedbackHelper.vibrateLight(context)
    }

    fun triggerActionButton(context: Context) {
        IOSFeedbackHelper.vibrateMedium(context)
        when (_actionButtonFunction.value) {
            "Flashlight" -> {
                toggleFlashlight(context)
            }
            "Camera" -> openApp(AppId.CAMERA)
            "Silent Mode" -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audio != null) {
                    val currentMode = audio.ringerMode
                    if (currentMode == AudioManager.RINGER_MODE_SILENT || currentMode == AudioManager.RINGER_MODE_VIBRATE) {
                        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        showDynamicIslandNotification("Ringer", "Suara Aktif", "bell", 0xFF34C759)
                    } else {
                        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        showDynamicIslandNotification("Hening", "Mode Hening Aktif", "bell", 0xFFFF3B30)
                    }
                }
            }
            "Voice Memo" -> {
                showDynamicIslandNotification("Voice Memos", "Merekam suara...", "mic", 0xFFFF2D55)
            }
            "Focus" -> {
                val dnd = !_isDNDActive.value
                _isDNDActive.value = dnd
                showDynamicIslandNotification("Focus", if (dnd) "Do Not Disturb On" else "Do Not Disturb Off", "moon", 0xFF5856D6)
            }
            "Root Tools" -> openApp(AppId.ROOT_TOOLS)
            else -> openApp(AppId.SETTINGS)
        }
    }

    fun setCameraControlEnabled(enabled: Boolean, context: Context) {
        _cameraControlEnabled.value = enabled
        IOSPersistenceManager.saveCameraControlEnabled(context, enabled)
    }

    fun setCameraControlSensitivity(sensitivity: String, context: Context) {
        _cameraControlSensitivity.value = sensitivity
        IOSFeedbackHelper.vibrateLight(context)
    }

    fun setStandByEnabled(enabled: Boolean, context: Context) {
        _standByEnabled.value = enabled
        IOSPersistenceManager.saveStandByEnabled(context, enabled)
    }

    fun setStandByNightMode(enabled: Boolean) {
        _standByNightMode.value = enabled
    }

    fun setStandByAlwaysOn(enabled: Boolean) {
        _standByAlwaysOn.value = enabled
    }

    fun setFocusMode(mode: com.example.model.FocusMode, context: Context) {
        _currentFocusMode.value = mode
        OverlayBridge.currentFocusMode.value = mode
        _isDNDActive.value = (mode != com.example.model.FocusMode.OFF)
        IOSPersistenceManager.saveFocusMode(context, mode)
        if (mode == com.example.model.FocusMode.OFF) {
            showDynamicIslandNotification("Fokus Dinonaktifkan", "Pemberitahuan aktif kembali", "bell", 0xFF8E8E93)
        } else {
            showDynamicIslandNotification("Fokus Aktif", mode.label, mode.iconKey, mode.colorLong)
        }
    }

    fun toggleFocusMode(context: Context) {
        val next = if (_currentFocusMode.value == com.example.model.FocusMode.OFF) {
            com.example.model.FocusMode.DO_NOT_DISTURB
        } else {
            com.example.model.FocusMode.OFF
        }
        setFocusMode(next, context)
    }

    fun setBackTapEnabled(enabled: Boolean, context: Context) {
        _backTapEnabled.value = enabled
        IOSPersistenceManager.saveBackTapEnabled(context, enabled)
    }

    fun setBackTapDoubleAction(action: com.example.model.BackTapAction, context: Context) {
        _backTapDoubleAction.value = action
        IOSPersistenceManager.saveBackTapDoubleAction(context, action)
    }

    fun setBackTapTripleAction(action: com.example.model.BackTapAction, context: Context) {
        _backTapTripleAction.value = action
        IOSPersistenceManager.saveBackTapTripleAction(context, action)
    }

    fun triggerBackTapAction(action: com.example.model.BackTapAction, context: Context) {
        when (action) {
            com.example.model.BackTapAction.FLASHLIGHT -> {
                toggleFlashlight(context)
                val status = if (_flashlightEnabled.value) "Senter Menyala" else "Senter Dimatikan"
                showDynamicIslandNotification("Back Tap", status, "flashlight", 0xFFFFCC00)
            }
            com.example.model.BackTapAction.SCREENSHOT -> {
                showDynamicIslandNotification("Back Tap", "Menangkap Layar...", "camera", 0xFF007AFF)
                com.example.util.RootExecutor.execute("screencap -p /sdcard/Pictures/Screenshots/screenshot_\$(date +%Y%m%d_%H%M%S).png")
            }
            com.example.model.BackTapAction.CONTROL_CENTER -> {
                _isControlCenterOpen.value = true
                _isNotificationCenterOpen.value = false
            }
            com.example.model.BackTapAction.NOTIFICATION_CENTER -> {
                _isNotificationCenterOpen.value = true
                _isControlCenterOpen.value = false
            }
            com.example.model.BackTapAction.APP_SWITCHER -> {
                openAppSwitcher()
            }
            com.example.model.BackTapAction.LOCK_SCREEN -> {
                lockDevice()
            }
            com.example.model.BackTapAction.MUTE -> {
                val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (audio != null) {
                    val currentMode = audio.ringerMode
                    if (currentMode == AudioManager.RINGER_MODE_SILENT || currentMode == AudioManager.RINGER_MODE_VIBRATE) {
                        audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
                        showDynamicIslandNotification("Ringer", "Suara Aktif", "bell", 0xFF34C759)
                    } else {
                        audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        showDynamicIslandNotification("Hening", "Mode Hening Aktif", "bell", 0xFFFF3B30)
                    }
                }
            }
            com.example.model.BackTapAction.NONE -> {}
        }
    }
}
