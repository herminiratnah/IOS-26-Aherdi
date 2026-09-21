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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.AssistiveTouchView
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
    private val viewModel: IOSViewModel by viewModels()
    private var backTapDetector: BackTapDetector? = null

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
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Panggilan Masuk"
                    viewModel.onIncomingCallReceived(incomingNumber)
                }
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.lockDevice()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        hideSystemBars()

        // Load all installed apps from device into iOS App Library
        viewModel.loadDeviceInstalledApps(this)

        // Register Receivers
        val screenFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenStateReceiver, screenFilter)

        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, batteryFilter)

        @Suppress("DEPRECATION")
        val netFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(connectivityReceiver, netFilter)

        val telFilter = IntentFilter().apply {
            addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        }
        registerReceiver(telephonyReceiver, telFilter)

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
        OverlayBridge.isLauncherForeground.value = true
        IOSOverlayService.start(this)
        if (viewModel.backTapEnabled.value) {
            backTapDetector?.start()
        }
        // Pastikan launcher iOS selalu 100% fullscreen di Y=0 (Dynamic Island dan Status Bar di ujung atas fisik)
        lifecycleScope.launch(Dispatchers.IO) {
            RootExecutor.resetTopDisplayInset()
        }
    }

    override fun onPause() {
        super.onPause()
        OverlayBridge.isLauncherForeground.value = false
    }

    override fun onStop() {
        super.onStop()
        OverlayBridge.isLauncherForeground.value = false
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

    @SuppressLint("GestureBackNavigation")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (viewModel.isDeviceLocked.value) {
                return true
            }
            if (viewModel.isStandByOpen.value) {
                viewModel.closeStandBy()
                return true
            }
            if (viewModel.isShareSheetOpen.value) {
                viewModel.closeShareSheet()
                return true
            }
            if (viewModel.isControlCenterOpen.value) {
                viewModel.closeControlCenter()
                return true
            }
            if (viewModel.isNotificationCenterOpen.value) {
                viewModel.closeNotificationCenter()
                return true
            }
            if (viewModel.isSpotlightOpen.value) {
                viewModel.closeSpotlight()
                return true
            }
            if (viewModel.isSiriOpen.value) {
                viewModel.closeSiri()
                return true
            }
            if (viewModel.activeApp.value != null) {
                viewModel.closeApp()
                return true
            }
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
                    viewModel.showDynamicIslandVolume(percent)
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
                    viewModel.showDynamicIslandVolume(percent)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
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

    // Handle back button / gesture
    BackHandler {
        if (isStandByOpen) {
            viewModel.closeStandBy()
        } else if (isShareSheetOpen) {
            viewModel.closeShareSheet()
        } else if (isSiriOpen) {
            viewModel.closeSiri()
        } else if (isSpotlightOpen) {
            viewModel.closeSpotlight()
        } else if (isControlCenterOpen) {
            viewModel.closeControlCenter()
        } else if (isNotificationCenterOpen) {
            viewModel.closeNotificationCenter()
        } else if (activeApp != null) {
            viewModel.closeApp()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
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
                AppId.MESSAGES -> MessagesApp(viewModel = viewModel)
                AppId.PHONE -> PhoneApp(viewModel = viewModel)
                AppId.SETTINGS -> SettingsApp(viewModel = viewModel)
                AppId.CLOCK -> ClockApp(viewModel = viewModel)
                AppId.CALENDAR -> CalendarApp(viewModel = viewModel)
                AppId.CAMERA -> CameraApp(viewModel = viewModel)
                AppId.PHOTOS -> PhotosApp(viewModel = viewModel)
                AppId.SAFARI -> SafariApp(viewModel = viewModel)
                AppId.MAIL -> MailApp(viewModel = viewModel)
                AppId.NOTES -> NotesApp(viewModel = viewModel)
                AppId.APP_STORE -> AppStoreApp(viewModel = viewModel)
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
                else -> GenericIOSAppPlaceholder(appId = targetApp, viewModel = viewModel)
            }
        }

        // Control Center Overlay (Top-Right swipe down)
        IOSControlCenter(
            viewModel = viewModel,
            isOpen = isControlCenterOpen,
            onDismiss = { viewModel.closeControlCenter() }
        )

        // Notification Center & Lock Screen Overlay (Top-Left swipe down)
        IOSNotificationCenter(
            viewModel = viewModel,
            isOpen = isNotificationCenterOpen,
            onDismiss = { viewModel.closeNotificationCenter() }
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

        // AssistiveTouch (Floating iOS Home Button)
        AssistiveTouchView(viewModel = viewModel)

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
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = true
            )

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
