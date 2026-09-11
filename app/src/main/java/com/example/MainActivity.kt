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
import com.example.ui.components.SiriOverlay
import com.example.ui.components.AssistiveTouchView
import com.example.ui.components.SpotlightSearchOverlay
import com.example.ui.screens.*
import com.example.ui.theme.IOSBlue
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.IOSViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: IOSViewModel by viewModels()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Load all installed apps from device into iOS App Library
        viewModel.loadDeviceInstalledApps(this)

        // Register Receivers
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
            }
        }

        setContent {
            MyApplicationTheme {
                IOS26LauncherRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OverlayBridge.isLauncherForeground.value = true
        IOSOverlayService.start(this)
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
                    val percent = (currentVol * 100) / maxVol.coerceAtLeast(1)
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
                    val percent = (currentVol * 100) / maxVol.coerceAtLeast(1)
                    viewModel.showDynamicIslandVolume(percent)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars()
        if (!hasFocus) {
            // Android SystemUI attempted to expand or pull down shade - close it instantly
            try {
                @Suppress("DEPRECATION")
                @SuppressLint("MissingPermission")
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                sendBroadcast(Intent("com.android.systemui.statusbar.phone.COLLAPSE_PANELS"))
            } catch (ignored: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        insetsController.hide(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
        try {
            @Suppress("DEPRECATION")
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            sendBroadcast(Intent("com.android.systemui.statusbar.phone.COLLAPSE_PANELS"))
        } catch (ignored: Exception) {}
        try {
            val statusBarService = getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val disable = statusBarManager.getMethod("disable", Int::class.javaPrimitiveType)
            // 0x00010000 = DISABLE_EXPAND, 0x00040000 = DISABLE_NOTIFICATION_ALERTS, 0x00020000 = DISABLE_NOTIFICATION_ICONS
            disable.invoke(statusBarService, 0x00010000 or 0x00040000 or 0x00020000 or 0x00100000)
        } catch (ignored: Exception) {}
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
        if (isSiriOpen) {
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

        // AssistiveTouch Floating Virtual Home Button
        AssistiveTouchView(viewModel = viewModel)
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
