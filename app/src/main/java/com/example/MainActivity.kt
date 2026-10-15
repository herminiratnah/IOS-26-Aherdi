package com.example

import android.annotation.SuppressLint
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.example.service.IOSOverlayService
import com.example.service.OverlayBridge
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.service.IOSNotificationListenerService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.util.RootExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSControlCenter
import com.example.ui.components.IOSNotificationCenter
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSLockScreenView
import com.example.ui.components.SiriOverlay
import com.example.ui.components.IOSSetupWizard
import com.example.ui.components.SpotlightSearchOverlay
import com.example.ui.components.IOSStandByOverlay
import com.example.ui.components.IOSShareSheet
import com.example.ui.components.IOSAppSwitcher
import com.example.ui.components.IOSFpsOverlay
import com.example.ui.screens.*
import com.example.ui.theme.IOSBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.util.BackTapDetector
import com.example.viewmodel.IOSViewModel
import android.content.res.Configuration

class MainActivity : ComponentActivity() {
    val viewModel: IOSViewModel by viewModels()
    private var backTapDetector: BackTapDetector? = null
    var isOpenedFromOverlayAction = false
        private set

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.getStringExtra("EXTRA_ACTION")
        if (action != null) {
            when (action) {
                "OPEN_CONTROL_CENTER" -> {
                    isOpenedFromOverlayAction = true
                    viewModel.openControlCenter()
                }
                "OPEN_NOTIFICATION_CENTER" -> {
                    isOpenedFromOverlayAction = true
                    viewModel.openNotificationCenter()
                }
            }
            return
        }

        if (intent.getBooleanExtra("EXTRA_SCREEN_OFF_LOCK", false)) {
            viewModel.lockDevice()
            return
        }

        // Standard Home launcher intent: user pressed physical or virtual HOME button!
        // Return instantly and smoothly to the Springboard Home screen!
        if (intent.action == Intent.ACTION_MAIN) {
            resetToHomeScreen()
        }
    }

    fun resetToHomeScreen() {
        isOpenedFromOverlayAction = false
        if (viewModel.isDeviceLocked.value) {
            // When device is locked, pressing Home button MUST NOT unlock without passcode!
            // Close any open overlays so the lock screen is cleanly visible
            viewModel.closeControlCenter()
            viewModel.closeNotificationCenter()
            viewModel.closeAppSwitcher()
            viewModel.closeSpotlight()
            viewModel.closeSiri()
            viewModel.closeShareSheet()
            viewModel.closeStandBy()
            viewModel.triggerHomeButtonOnLockScreen()
            return
        }
        viewModel.closeApp()
        viewModel.closeControlCenter()
        viewModel.closeNotificationCenter()
        viewModel.closeAppSwitcher()
        viewModel.closeSpotlight()
        viewModel.closeSiri()
        viewModel.closeShareSheet()
        viewModel.closeStandBy()
    }

    fun dismissOverlayAction() {
        if (isOpenedFromOverlayAction) {
            isOpenedFromOverlayAction = false
            moveTaskToBack(true)
        }
    }

    @android.annotation.SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            resetToHomeScreen()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (viewModel.isAppSwitcherOpen.value) {
                viewModel.closeAppSwitcher()
            } else {
                viewModel.openAppSwitcher()
            }
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // First check if an overlay (Spotlight, Control Center, Siri, etc.) is open
            if (handleBackPress()) {
                return true
            }
            // If an app is currently open, delegate back so internal sub-pages can navigate back
            if (viewModel.activeApp.value != null) {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            // Khusus di halaman awal (Home Screen / Springboard):
            // Disable tombol back sepenuhnya agar tidak pernah blank hitam!
            return true
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val fraction = if (maxVol > 0) currentVol.toFloat() / maxVol.toFloat() else 0.5f
                    val percent = (currentVol * 100) / maxVol.coerceAtLeast(1)
                    viewModel.syncCurrentVolume(fraction)
                    // If in call (cellular or VoIP), DO NOT replace Dynamic Island call animation with Volume!
                    val inCall = viewModel.isInCall.value || viewModel.dynamicIsland.value.mode == com.example.viewmodel.DynamicIslandMode.CALL
                    if (!inCall) {
                        viewModel.showDynamicIslandVolume(percent)
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_PLAY_SOUND
                    )
                    val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val fraction = if (maxVol > 0) currentVol.toFloat() / maxVol.toFloat() else 0.5f
                    val percent = (currentVol * 100) / maxVol.coerceAtLeast(1)
                    viewModel.syncCurrentVolume(fraction)
                    val inCall = viewModel.isInCall.value || viewModel.dynamicIsland.value.mode == com.example.viewmodel.DynamicIslandMode.CALL
                    if (!inCall) {
                        viewModel.showDynamicIslandVolume(percent)
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    @android.annotation.SuppressLint("GestureBackNavigation")
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Strictly consume back release so Android never finishes the launcher activity
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    fun handleBackPress(): Boolean {
        return when {
            viewModel.isDeviceLocked.value -> true
            viewModel.isStandByOpen.value -> {
                viewModel.closeStandBy()
                true
            }
            viewModel.isShareSheetOpen.value -> {
                viewModel.closeShareSheet()
                true
            }
            viewModel.isSiriOpen.value -> {
                viewModel.closeSiri()
                true
            }
            viewModel.isSpotlightOpen.value -> {
                viewModel.closeSpotlight()
                true
            }
            viewModel.isControlCenterOpen.value -> {
                viewModel.closeControlCenter()
                dismissOverlayAction()
                true
            }
            viewModel.isNotificationCenterOpen.value -> {
                viewModel.closeNotificationCenter()
                dismissOverlayAction()
                true
            }
            viewModel.isAppSwitcherOpen.value -> {
                viewModel.closeAppSwitcher()
                true
            }
            isOpenedFromOverlayAction -> {
                dismissOverlayAction()
                true
            }
            else -> false
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        private var lastChargingState: Boolean? = null
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (scale > 0) ((level / scale.toFloat()) * 100).toInt() else level

            viewModel.updateBatteryStatus(batteryPct, isCharging)

            if (lastChargingState != null && lastChargingState != isCharging) {
                // Charging state changed - trigger authentic iOS Dynamic Island battery alert
                viewModel.showDynamicIslandBattery(batteryPct, isCharging)
            }
            lastChargingState = isCharging

            if (isCharging && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && viewModel.standByEnabled.value) {
                viewModel.openStandBy()
            }
        }
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            @Suppress("DEPRECATION")
            val activeNetwork = cm.activeNetworkInfo
            val isConnected = activeNetwork?.isConnectedOrConnecting == true
            val isWifi = isConnected && activeNetwork?.type == ConnectivityManager.TYPE_WIFI
            val netType = if (isWifi) "" else if (isConnected) (activeNetwork?.typeName ?: "5G") else "No SIM"
            viewModel.updateNetworkStatus(
                isWifi = isWifi,
                wifiLevel = 4,
                cellularBars = if (isConnected) 4 else 0,
                netType = netType
            )
        }
    }

    private val telephonyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val action = intent.action
            if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val pdus = intent.extras?.get("pdus") as? Array<*>
                val format = intent.getStringExtra("format")
                pdus?.forEach { pdu ->
                    val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    val sender = sms.displayOriginatingAddress ?: "Unknown"
                    val body = sms.displayMessageBody ?: ""
                    viewModel.onSmsReceived(sender, body)
                }
            } else if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Panggilan Masuk"
                        viewModel.onIncomingCallReceived(incomingNumber)
                    }
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        val contact = viewModel.callContactName.value.ifBlank { "Panggilan Suara" }
                        val number = viewModel.callPhoneNumber.value
                        viewModel.startInAppCall(contact, number)
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        viewModel.endCall()
                    }
                }
            }
        }
    }

    fun bringLockScreenToFront() {
        try {
            val lockIntent = Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_SCREEN_OFF_LOCK", true)
            }
            startActivity(lockIntent)
        } catch (_: Throwable) {}
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    backTapDetector?.stop()
                    if (viewModel.isPasscodeEnabled.value) {
                        viewModel.lockDevice()
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (viewModel.backTapEnabled.value) {
                        backTapDetector?.start()
                    }
                    if (viewModel.isDeviceLocked.value) {
                        bringLockScreenToFront()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        hideSystemBars()
        handleIntent(intent)

        // Pre-create WebView cache directories to eliminate Chromium simple_file_enumerator opendir errors
        try {
            val codeCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(codeCacheDir, "js").mkdirs()
            java.io.File(codeCacheDir, "wasm").mkdirs()
        } catch (_: Throwable) {}

        // Activity-level Back Press Protection:
        // On Home Screen (SpringBoard), disable back button completely so launcher NEVER exits to blank screen!
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (handleBackPress()) {
                    return
                }
                if (viewModel.activeApp.value != null) {
                    viewModel.closeApp()
                    return
                }
                // Di halaman awal: strictly do nothing! Never call finish().
            }
        })

        // Load all installed apps from device into iOS App Library
        viewModel.loadDeviceInstalledApps(this)

        // Register Receivers safely with Android 14+ export flags
        try {
            val screenFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                screenStateReceiver,
                screenFilter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}

        try {
            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                batteryReceiver,
                batteryFilter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}

        try {
            @Suppress("DEPRECATION")
            val netFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                connectivityReceiver,
                netFilter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (_: Exception) {}

        try {
            val telFilter = IntentFilter().apply {
                addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            }
            androidx.core.content.ContextCompat.registerReceiver(
                this,
                telephonyReceiver,
                telFilter,
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {}

        // Listen for WhatsApp & other third-party app notifications for Dynamic Island & Notification Center pipeline
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val component = "$packageName/${IOSNotificationListenerService::class.java.name}"
                RootExecutor.execute("cmd notification allow_listener $component")
                val current = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
                if (!current.contains(component)) {
                    val updated = if (current.isBlank()) component else "$current:$component"
                    RootExecutor.execute("settings put secure enabled_notification_listeners \"$updated\"")
                }
            } catch (ignored: Exception) {}
        }

        lifecycleScope.launchWhenStarted {
            IOSNotificationListenerService.notificationFlow.collect { notif ->
                val isCall = notif.iconKey == "phone" ||
                        notif.packageName.contains("dialer", ignoreCase = true) ||
                        notif.packageName.contains("telecom", ignoreCase = true) ||
                        (notif.packageName.contains("whatsapp", ignoreCase = true) && (
                            notif.message.contains("panggilan", ignoreCase = true) ||
                            notif.message.contains("call", ignoreCase = true) ||
                            notif.title.contains("panggilan", ignoreCase = true) ||
                            notif.title.contains("call", ignoreCase = true)
                        ))

                if (isCall) {
                    val isIncoming = notif.message.contains("masuk", ignoreCase = true) ||
                            notif.message.contains("incoming", ignoreCase = true) ||
                            notif.message.contains("audio", ignoreCase = true)
                    val caller = if (notif.title.isNotBlank() && !notif.title.equals("WhatsApp", ignoreCase = true)) notif.title else notif.message
                    viewModel.showDynamicIslandVoipCall(
                        appName = notif.appName,
                        callerName = caller.ifBlank { "Panggilan Masuk" },
                        isIncoming = isIncoming
                    )
                } else {
                    viewModel.addNotification(
                        com.example.model.IOSNotificationItem(
                            packageName = notif.packageName,
                            appName = notif.appName,
                            title = notif.title,
                            message = notif.message,
                            timestamp = "Now",
                            iconKey = notif.iconKey,
                            accentColor = notif.accentColor
                        )
                    )
                    // Pop open Dynamic Island in Launcher Home
                    viewModel.showDynamicIslandNotification(
                        title = notif.appName,
                        message = "${notif.title}: ${notif.message}",
                        icon = notif.iconKey,
                        accent = notif.accentColor,
                        packageName = notif.packageName,
                        contentIntent = notif.contentIntent
                    )
                }
            }
        }

        // Initialize real device volume and brightness
        val initialAudio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (initialAudio != null) {
            val cur = initialAudio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = initialAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if (max > 0) {
                viewModel.syncCurrentVolume(cur.toFloat() / max.toFloat())
            }
        }
        try {
            val currentBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            viewModel.syncCurrentBrightness((currentBrightness / 255f).coerceIn(0.05f, 1.0f))
        } catch (ignored: Exception) {}

        // Connect ViewModel brightness slider directly to Window attributes for instant visual response
        lifecycleScope.launch {
            viewModel.brightness.collect { b ->
                try {
                    val lp = window.attributes
                    lp.screenBrightness = b.coerceIn(0.01f, 1.0f)
                    window.attributes = lp
                } catch (ignored: Exception) {}
            }
        }

        // Initialize iOS Back Tap Hardware Detection
        backTapDetector = BackTapDetector(
            context = this,
            onDoubleTap = {
                if (viewModel.backTapEnabled.value) {
                    viewModel.triggerBackTapAction(viewModel.backTapDoubleAction.value, this)
                }
            },
            onTripleTap = {
                if (viewModel.backTapEnabled.value) {
                    viewModel.triggerBackTapAction(viewModel.backTapTripleAction.value, this)
                }
            }
        )
        lifecycleScope.launch {
            viewModel.backTapEnabled.collect { enabled ->
                if (enabled) {
                    backTapDetector?.start()
                } else {
                    backTapDetector?.stop()
                }
            }
        }

        setContent {
            MyApplicationTheme {
                IOS26LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && viewModel.isBatteryCharging.value && viewModel.standByEnabled.value) {
            viewModel.openStandBy()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && viewModel.isStandByOpen.value) {
            viewModel.closeStandBy()
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.util.AppWidgetHelper.startListening(this)
        OverlayBridge.isLauncherForeground.value = true
        IOSOverlayService.start(this)
        if (viewModel.backTapEnabled.value) {
            backTapDetector?.start()
        }
        val volumeInterceptionUserPref = try {
            com.example.data.IOSPersistenceManager.loadRootBoolean(this, "volume_interception", false)
        } catch (_: Throwable) { false }
        if (volumeInterceptionUserPref && RootExecutor.isRootAvailable() && !RootExecutor.isVolumeInterceptionEnabled(this)) {
            lifecycleScope.launch(Dispatchers.IO) {
                RootExecutor.enableVolumeInterception(this@MainActivity)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        OverlayBridge.isLauncherForeground.value = false
        backTapDetector?.stop()
    }

    override fun onStop() {
        super.onStop()
        com.example.util.AppWidgetHelper.stopListening()
        OverlayBridge.isLauncherForeground.value = false
        backTapDetector?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        backTapDetector?.stop()
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (ignored: Exception) {}
        try {
            unregisterReceiver(batteryReceiver)
        } catch (ignored: Exception) {}
        try {
            unregisterReceiver(connectivityReceiver)
        } catch (ignored: Exception) {}
        try {
            unregisterReceiver(telephonyReceiver)
        } catch (ignored: Exception) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.post {
                hideSystemBars()
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Keep 3-button navigation (Home, Back, Recent) always visible and responsive
        insetsController.show(WindowInsetsCompat.Type.navigationBars())
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
}

@Composable
fun IOS26LauncherRoot(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeApp by viewModel.activeApp.collectAsState()
    val isControlCenterOpen by viewModel.isControlCenterOpen.collectAsState()
    val isNotificationCenterOpen by viewModel.isNotificationCenterOpen.collectAsState()
    val isSiriOpen by viewModel.isSiriOpen.collectAsState()
    val isSpotlightOpen by viewModel.isSpotlightOpen.collectAsState()
    val isStandByOpen by viewModel.isStandByOpen.collectAsState()
    val isShareSheetOpen by viewModel.isShareSheetOpen.collectAsState()
    val shareSheetTitle by viewModel.shareSheetTitle.collectAsState()
    val isAppSwitcherOpen by viewModel.isAppSwitcherOpen.collectAsState()

    // Request necessary telephony, storage & system permissions for real iOS phone/SMS/Music experience
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.loadDeviceContacts(context)
        viewModel.loadDeviceCallLogs(context)
        viewModel.loadDeviceSms(context)
        viewModel.loadLocalMusic(context)
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    // Global Back button / gesture handler:
    // When inside an app, overlay, or sub-screen, back behaves normally.
    // When on the Home Screen (Springboard), back is safely consumed and disabled so the launcher NEVER exits into a blank black screen.
    val isOverlayAction = (context as? MainActivity)?.isOpenedFromOverlayAction == true

    BackHandler(enabled = true) {
        val handled = (context as? MainActivity)?.handleBackPress() ?: false
        if (!handled) {
            when {
                isOverlayAction -> (context as? MainActivity)?.dismissOverlayAction()
                isStandByOpen -> viewModel.closeStandBy()
                isShareSheetOpen -> viewModel.closeShareSheet()
                isSiriOpen -> viewModel.closeSiri()
                isSpotlightOpen -> viewModel.closeSpotlight()
                isControlCenterOpen -> {
                    viewModel.closeControlCenter()
                    (context as? MainActivity)?.dismissOverlayAction()
                }
                isNotificationCenterOpen -> {
                    viewModel.closeNotificationCenter()
                    (context as? MainActivity)?.dismissOverlayAction()
                }
                isAppSwitcherOpen -> viewModel.closeAppSwitcher()
                activeApp != null -> viewModel.closeApp()
                else -> {
                    // Strictly do nothing on Home screen!
                    // This protects the launcher from closing and going blank black.
                }
            }
        }
    }

    val isAnyCenterOpen = isControlCenterOpen || isNotificationCenterOpen
    val centerBlurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isAnyCenterOpen) 36.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "CenterBlurRadius"
    )
    val centerScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnyCenterOpen) 0.965f else 1f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "CenterScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                }
                .then(
                    if (centerBlurRadius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(centerBlurRadius)
                    } else {
                        Modifier
                    }
                )
        ) {
            // App Switcher / Main View with Smooth Transitions
            AnimatedContent(
            targetState = activeApp,
            transitionSpec = {
                if (targetState != null) {
                    // Opening app: zoom in / slide up
                    (slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(280)) +
                            fadeIn(animationSpec = tween(240)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(200))
                        )
                } else {
                    // Going back to Home: zoom out / slide down
                    (fadeIn(animationSpec = tween(240)))
                        .togetherWith(
                            slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(280)) +
                                    fadeOut(animationSpec = tween(200))
                        )
                }
            },
            label = "AppTransition"
        ) { targetApp ->
            when (targetApp) {
                null -> SpringBoardScreen(viewModel = viewModel)
                AppId.SETTINGS -> SettingsApp(viewModel = viewModel)
                AppId.CLOCK -> ClockApp(viewModel = viewModel)
                AppId.CALENDAR -> CalendarApp(viewModel = viewModel)
                AppId.CAMERA -> CameraApp(viewModel = viewModel)
                AppId.PHOTOS -> PhotosApp(viewModel = viewModel)
                AppId.SAFARI -> SafariApp(viewModel = viewModel)
                AppId.MAIL -> MailApp(viewModel = viewModel)
                AppId.NOTES -> NotesApp(viewModel = viewModel)
                AppId.WEATHER -> WeatherApp(viewModel = viewModel)
                AppId.CALCULATOR -> CalculatorApp(viewModel = viewModel)
                AppId.ROOT_TOOLS -> RootToolsApp(viewModel = viewModel)
                AppId.FACETIME -> FaceTimeApp(viewModel = viewModel)
                AppId.MAPS -> MapsApp(viewModel = viewModel)
                AppId.TV -> TVApp(viewModel = viewModel)
                AppId.WALLET -> WalletApp(viewModel = viewModel)
                AppId.PODCASTS -> PodcastsApp(viewModel = viewModel)
                AppId.HEALTH -> HealthApp(viewModel = viewModel)
                AppId.STOCKS -> StocksApp(viewModel = viewModel)
                AppId.BOOKS -> BooksApp(viewModel = viewModel)
                AppId.MUSIC -> MusicApp(viewModel = viewModel)
                AppId.REMINDERS -> RemindersApp(viewModel = viewModel)
                AppId.DYNAMIC_ISLAND -> DynamicIslandSubPage(viewModel = viewModel, onBack = { viewModel.closeApp() })
                AppId.DEPTH_STUDIO -> DepthStudioApp(viewModel = viewModel, onBack = { viewModel.closeApp() })
                else -> GenericIOSAppPlaceholder(appId = targetApp, viewModel = viewModel)
            }
        }
    }

        // Sync wallpaper to IOSBlurHelper for universal frosted blur on all Android versions (including Android 8.1 J2 Prime)
        val currentWallpaperRes by viewModel.currentWallpaperRes.collectAsState()
        val currentWallpaperUri by viewModel.currentWallpaperUri.collectAsState()
        LaunchedEffect(currentWallpaperRes, currentWallpaperUri) {
            com.example.util.IOSBlurHelper.updateWallpaper(context, currentWallpaperRes, currentWallpaperUri)
            com.example.service.OverlayBridge.currentWallpaperRes.value = currentWallpaperRes
            com.example.service.OverlayBridge.currentWallpaperUri.value = currentWallpaperUri
        }

        // Single Global iOS Status Bar & Dynamic Island (Above apps and SpringBoard)
        val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
        val currentTime by viewModel.currentTimeFormatted.collectAsState()
        val batteryLevel by viewModel.batteryLevel.collectAsState()
        val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
        val isWifiConnected by viewModel.isWifiConnected.collectAsState()
        val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
        val cellularBars by viewModel.cellularSignalBars.collectAsState()
        val networkType by viewModel.networkType.collectAsState()
        val airplaneMode by viewModel.airplaneMode.collectAsState()
        val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
        val musicProgress by viewModel.musicProgress.collectAsState()
        val musicCurrentTimeStr by viewModel.musicCurrentTimeStr.collectAsState()
        val musicDurationStr by viewModel.musicDurationStr.collectAsState()
        val currentFocus by viewModel.currentFocusMode.collectAsState()
        val secondaryIslandData by viewModel.secondaryDynamicIsland.collectAsState()
        val networkSpeed by viewModel.networkSpeed.collectAsState()
        val showNetworkSpeed by viewModel.showNetworkSpeed.collectAsState()
        val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()
        val isLowPowerMode by viewModel.isLowPowerMode.collectAsState()
        val isBluetoothConnected by viewModel.isBluetoothDeviceConnected.collectAsState()
        val isLocationActive by viewModel.isLocationActive.collectAsState()
        val isAlarmSet by viewModel.isAlarmSet.collectAsState()
        val isVpnConnected by viewModel.isVpnActive.collectAsState()
        val isDarkIcons = activeApp != null && activeApp != AppId.CAMERA && activeApp != AppId.WEATHER

        IOSStatusBar(
            dynamicIslandData = dynamicIslandData,
            onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
            onOpenControlCenter = { viewModel.toggleControlCenter() },
            onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
            currentTime = currentTime,
            batteryLevel = batteryLevel,
            isBatteryCharging = isBatteryCharging,
            isWifiConnected = isWifiConnected,
            wifiSignalLevel = wifiSignalLevel,
            cellularBars = cellularBars,
            networkType = networkType,
            isAirplaneMode = airplaneMode,
            isDarkIcons = isDarkIcons,
            isMusicPlaying = isMusicPlaying,
            musicProgress = musicProgress,
            musicCurrentTimeStr = musicCurrentTimeStr,
            musicDurationStr = musicDurationStr,
            focusMode = currentFocus,
            secondaryIslandData = secondaryIslandData,
            onSecondaryIslandClick = { viewModel.onSecondaryIslandClicked() },
            networkSpeed = networkSpeed,
            showNetworkSpeed = showNetworkSpeed,
            showBatteryPercentage = showBatteryPercentage,
            isLowPowerMode = isLowPowerMode,
            isBluetoothConnected = isBluetoothConnected,
            isVpnConnected = isVpnConnected,
            isLocationActive = isLocationActive,
            isAlarmSet = isAlarmSet,
            onMusicPlayPause = { viewModel.toggleMusicPlay() },
            onMusicNext = { viewModel.skipNextSong() },
            onMusicPrev = { viewModel.skipPreviousSong() },
            onExpandMusic = { viewModel.showMusicInDynamicIsland() },
            onAnswerCall = { viewModel.answerIncomingCall(context) },
            onDeclineCall = { viewModel.rejectIncomingCall(context) },
            onAirPlaySelect = { dev -> viewModel.switchAirPlayDevice(dev) },
            onLockDevice = { viewModel.lockDevice() },
            onOpenClock = { viewModel.openApp(AppId.CLOCK) },
            onOpenBatteryInfo = { viewModel.toggleLowPowerMode() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Control Center Overlay (Top-Right swipe down)
        IOSControlCenter(
            viewModel = viewModel,
            isOpen = isControlCenterOpen,
            onDismiss = {
                viewModel.closeControlCenter()
                (context as? MainActivity)?.dismissOverlayAction()
            }
        )

        // Notification Center & Lock Screen Overlay (Top-Left swipe down)
        IOSNotificationCenter(
            viewModel = viewModel,
            isOpen = isNotificationCenterOpen,
            onDismiss = {
                viewModel.closeNotificationCenter()
                (context as? MainActivity)?.dismissOverlayAction()
            }
        )

        // Spotlight Search Overlay
        SpotlightSearchOverlay(
            viewModel = viewModel,
            isOpen = isSpotlightOpen,
            onDismiss = { viewModel.closeSpotlight() }
        )

        // Apple Intelligence & Siri Glowing Screen Overlay
        SiriOverlay(
            viewModel = viewModel,
            isOpen = isSiriOpen,
            onDismiss = { viewModel.closeSiri() }
        )

        // iOS StandBy Overlay
        IOSStandByOverlay(
            viewModel = viewModel,
            isOpen = isStandByOpen,
            onDismiss = { viewModel.closeStandBy() }
        )

        // iOS AirDrop & System Share Sheet Overlay
        IOSShareSheet(
            viewModel = viewModel,
            isOpen = isShareSheetOpen,
            title = shareSheetTitle,
            onDismiss = { viewModel.closeShareSheet() }
        )

        // iOS Multitasking App Switcher
        IOSAppSwitcher(
            viewModel = viewModel,
            isOpen = isAppSwitcherOpen,
            onDismiss = { viewModel.closeAppSwitcher() }
        )

        // Developer Performance HUD
        IOSFpsOverlay(viewModel = viewModel)

        // iOS 18 Setup Wizard (OOBE on first run)
        val isSetupWizardOpen by viewModel.isSetupWizardOpen.collectAsState()
        IOSSetupWizard(
            viewModel = viewModel,
            isOpen = isSetupWizardOpen,
            onComplete = { viewModel.completeSetupWizard() }
        )

        // iOS Lock Screen Overlay (Top-level device guard)
        IOSLockScreenView(viewModel = viewModel)
    }
}

@Composable
fun GenericIOSAppPlaceholder(
    appId: AppId,
    viewModel: IOSViewModel
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val appTitle = appId.name.lowercase().replaceFirstChar { it.uppercase() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Spacer for Single Global Status Bar
            Spacer(modifier = Modifier.height(44.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(IOSBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Apps,
                        contentDescription = appTitle,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = appTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Welcome to $appTitle on iOS 26",
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }
    }
}
