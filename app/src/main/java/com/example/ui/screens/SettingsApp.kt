package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun SettingsApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    val airplaneMode by viewModel.airplaneMode.collectAsState()
    val wifiEnabled by viewModel.wifiEnabled.collectAsState()
    val bluetoothEnabled by viewModel.bluetoothEnabled.collectAsState()
    val currentWallpaperName by viewModel.currentWallpaperName.collectAsState()

    val mainScrollState = rememberScrollState()
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }
    var genericPageTitle by remember { mutableStateOf("") }
    var genericPageItems by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    BackHandler {
        if (currentSubPage != SettingsSubPage.MAIN) {
            currentSubPage = SettingsSubPage.MAIN
        } else {
            viewModel.closeApp()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                isDarkIcons = true
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentSubPage) {
                    SettingsSubPage.MAIN -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(mainScrollState)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Settings",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                letterSpacing = (-0.5).sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )

                            // Search Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE3E3E8))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = IOSSystemGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isEmpty()) "Search" else searchQuery,
                                        color = IOSSystemGray,
                                        fontSize = 17.sp
                                    )
                                }
                            }

                            // Profile Banner (Apple Account)
                            IOSSettingsCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currentSubPage = SettingsSubPage.PROFILE }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "AS",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Aherdi Soeprapto",
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Apple Account, iCloud, and more",
                                            fontSize = 13.sp,
                                            color = IOSSystemGray
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = IOSSystemGray3,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Section 1: Connectivity
                            IOSSettingsCard {
                                IOSSettingsToggleRow(
                                    icon = Icons.Rounded.AirplanemodeActive,
                                    iconBg = Color(0xFFFF9500),
                                    title = "Airplane Mode",
                                    checked = airplaneMode,
                                    onCheckedChange = { viewModel.toggleAirplaneMode() }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Wifi,
                                    iconBg = Color(0xFF007AFF),
                                    title = "Wi-Fi",
                                    detail = if (wifiEnabled) (if (isWifiConnected) "Home_Fast_WiFi" else "Not Connected") else "Off",
                                    onClick = { currentSubPage = SettingsSubPage.WIFI }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Bluetooth,
                                    iconBg = Color(0xFF007AFF),
                                    title = "Bluetooth",
                                    detail = if (bluetoothEnabled) "On" else "Off",
                                    onClick = { currentSubPage = SettingsSubPage.BLUETOOTH }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.SignalCellularAlt,
                                    iconBg = Color(0xFF34C759),
                                    title = "Cellular",
                                    detail = "5G Auto",
                                    onClick = { currentSubPage = SettingsSubPage.CELLULAR }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.WifiTethering,
                                    iconBg = Color(0xFF34C759),
                                    title = "Personal Hotspot",
                                    detail = "Wi-Fi Tethering",
                                    onClick = { currentSubPage = SettingsSubPage.PERSONAL_HOTSPOT }
                                )
                            }

                            // Section 2: Notifications, Sounds, Focus, Screen Time
                            IOSSettingsCard {
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Notifications,
                                    iconBg = Color(0xFFFF3B30),
                                    title = "Notifications",
                                    detail = "",
                                    onClick = {
                                        genericPageTitle = "Notifications"
                                        genericPageItems = listOf(
                                            Pair("Scheduled Summary", "Off"),
                                            Pair("Show Previews", "Always"),
                                            Pair("Screen Sharing", "Notifications Off"),
                                            Pair("Messages", "Banners, Sounds, Badges"),
                                            Pair("Phone", "Banners, Sounds, Badges"),
                                            Pair("Mail", "Badges")
                                        )
                                        currentSubPage = SettingsSubPage.APP_SETTINGS
                                    }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.VolumeUp,
                                    iconBg = Color(0xFFFF2D55),
                                    title = "Sounds & Haptics",
                                    detail = "",
                                    onClick = { currentSubPage = SettingsSubPage.SOUNDS }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.DoNotDisturbOn,
                                    iconBg = Color(0xFF5856D6),
                                    title = "Focus",
                                    detail = "Do Not Disturb",
                                    onClick = {
                                        genericPageTitle = "Focus"
                                        genericPageItems = listOf(
                                            Pair("Do Not Disturb", "Set Up"),
                                            Pair("Personal", "Set Up"),
                                            Pair("Sleep", "Set Up"),
                                            Pair("Work", "Set Up"),
                                            Pair("Share Across Devices", "On")
                                        )
                                        currentSubPage = SettingsSubPage.APP_SETTINGS
                                    }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.HourglassBottom,
                                    iconBg = Color(0xFF5856D6),
                                    title = "Screen Time",
                                    detail = "2h 45m today",
                                    onClick = {
                                        genericPageTitle = "Screen Time"
                                        genericPageItems = listOf(
                                            Pair("Downtime", "Schedule downtime"),
                                            Pair("App Limits", "Set time limits for apps"),
                                            Pair("Always Allowed", "Phone, Messages, Maps"),
                                            Pair("Content & Privacy Restrictions", "Off")
                                        )
                                        currentSubPage = SettingsSubPage.APP_SETTINGS
                                    }
                                )
                            }

                            // Section 3: General, Display, Accessibility, Wallpaper, Battery
                            IOSSettingsCard {
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Settings,
                                    iconBg = Color(0xFF8E8E93),
                                    title = "General",
                                    detail = "",
                                    onClick = { currentSubPage = SettingsSubPage.GENERAL }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Tune,
                                    iconBg = Color(0xFF8E8E93),
                                    title = "Control Center",
                                    detail = "",
                                    onClick = { viewModel.toggleControlCenter() }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.BrightnessMedium,
                                    iconBg = Color(0xFF007AFF),
                                    title = "Display & Brightness",
                                    detail = "",
                                    onClick = { currentSubPage = SettingsSubPage.DISPLAY }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Apps,
                                    iconBg = Color(0xFF007AFF),
                                    title = "Home Screen & App Library",
                                    detail = "",
                                    onClick = {
                                        genericPageTitle = "Home Screen"
                                        genericPageItems = listOf(
                                            Pair("Newly Downloaded Apps", "Add to Home Screen"),
                                            Pair("Notification Badges", "Show in App Library")
                                        )
                                        currentSubPage = SettingsSubPage.APP_SETTINGS
                                    }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Accessibility,
                                    iconBg = Color(0xFF007AFF),
                                    title = "Accessibility",
                                    detail = "Vision, Motor, Integrasi Android",
                                    onClick = { currentSubPage = SettingsSubPage.ACCESSIBILITY }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Wallpaper,
                                    iconBg = Color(0xFF32ADE6),
                                    title = "Wallpaper",
                                    detail = currentWallpaperName,
                                    onClick = { currentSubPage = SettingsSubPage.WALLPAPER }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.RecordVoiceOver,
                                    iconBg = Color(0xFF5856D6),
                                    title = "Siri & Apple Intelligence",
                                    detail = "Press Side Button for Siri",
                                    onClick = { currentSubPage = SettingsSubPage.SIRI }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Face,
                                    iconBg = Color(0xFF34C759),
                                    title = "Face ID & Passcode",
                                    detail = "",
                                    onClick = { currentSubPage = SettingsSubPage.FACE_ID }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Sos,
                                    iconBg = Color(0xFFFF3B30),
                                    title = "Emergency SOS",
                                    detail = "Call with Hold and Release",
                                    onClick = { currentSubPage = SettingsSubPage.EMERGENCY_SOS }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.BatteryChargingFull,
                                    iconBg = Color(0xFF34C759),
                                    title = "Battery",
                                    detail = "$batteryLevel%",
                                    onClick = { currentSubPage = SettingsSubPage.BATTERY }
                                )
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(
                                    icon = Icons.Rounded.Lock,
                                    iconBg = Color(0xFF007AFF),
                                    title = "Privacy & Security",
                                    detail = "",
                                    onClick = {
                                        genericPageTitle = "Privacy & Security"
                                        genericPageItems = listOf(
                                            Pair("Location Services", "On"),
                                            Pair("Tracking", "Ask App Not to Track"),
                                            Pair("Contacts Permission", "Granted"),
                                            Pair("Camera Permission", "Granted"),
                                            Pair("Microphone Permission", "Granted"),
                                            Pair("Safety Check", "Review Sharing")
                                        )
                                        currentSubPage = SettingsSubPage.APP_SETTINGS
                                    }
                                )
                            }

                            // Section 4: Apps List
                            Text("APPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                            IOSSettingsCard {
                                val apps = listOf(
                                    Triple("App Store", Icons.Rounded.ShoppingBag, Color(0xFF007AFF)),
                                    Triple("Wallet", Icons.Rounded.AccountBalanceWallet, Color.Black),
                                    Triple("Passwords", Icons.Rounded.Key, Color(0xFF8E8E93)),
                                    Triple("Mail", Icons.Rounded.Email, Color(0xFF007AFF)),
                                    Triple("Contacts", Icons.Rounded.Contacts, Color(0xFF8E8E93)),
                                    Triple("Calendar", Icons.Rounded.CalendarToday, Color(0xFFFF3B30)),
                                    Triple("Notes", Icons.Rounded.Description, Color(0xFFFFCC00)),
                                    Triple("Reminders", Icons.Rounded.FormatListBulleted, Color(0xFF007AFF)),
                                    Triple("Freeform", Icons.Rounded.Brush, Color(0xFF5856D6)),
                                    Triple("TV", Icons.Rounded.Tv, Color.Black),
                                    Triple("Music", Icons.Rounded.MusicNote, Color(0xFFFF2D55)),
                                    Triple("Photos", Icons.Rounded.Photo, Color(0xFFFF9500)),
                                    Triple("Camera", Icons.Rounded.CameraAlt, Color(0xFF8E8E93)),
                                    Triple("Podcasts", Icons.Rounded.Podcasts, Color(0xFFAF52DE)),
                                    Triple("Books", Icons.Rounded.Book, Color(0xFFFF9500)),
                                    Triple("Health", Icons.Rounded.Favorite, Color(0xFFFF2D55)),
                                    Triple("Fitness", Icons.Rounded.FitnessCenter, Color(0xFF34C759)),
                                    Triple("Find My", Icons.Rounded.Radar, Color(0xFF34C759)),
                                    Triple("Maps", Icons.Rounded.Map, Color(0xFF34C759)),
                                    Triple("Shortcuts", Icons.Rounded.FlashOn, Color(0xFF5856D6)),
                                    Triple("Stocks", Icons.Rounded.ShowChart, Color.Black),
                                    Triple("Translate", Icons.Rounded.Translate, Color(0xFF007AFF)),
                                    Triple("Files", Icons.Rounded.Folder, Color(0xFF007AFF)),
                                    Triple("Weather", Icons.Rounded.WbSunny, Color(0xFF007AFF)),
                                    Triple("Phone", Icons.Rounded.Phone, Color(0xFF34C759)),
                                    Triple("Messages", Icons.Rounded.ChatBubble, Color(0xFF34C759)),
                                    Triple("FaceTime", Icons.Rounded.Videocam, Color(0xFF34C759)),
                                    Triple("Safari", Icons.Rounded.Explore, Color(0xFF007AFF))
                                )

                                apps.forEachIndexed { idx, (appName, appIcon, iconCol) ->
                                    IOSSettingsRow(
                                        icon = appIcon,
                                        iconBg = iconCol,
                                        title = appName,
                                        detail = "",
                                        onClick = {
                                            genericPageTitle = appName
                                            genericPageItems = listOf(
                                                Pair("Allow $appName to Access", "Notifications, Cellular Data"),
                                                Pair("Siri & Search", "Learn from this App"),
                                                Pair("Background App Refresh", "On"),
                                                Pair("Language", "Device Language")
                                            )
                                            currentSubPage = SettingsSubPage.APP_SETTINGS
                                        }
                                    )
                                    if (idx < apps.size - 1) {
                                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    SettingsSubPage.PROFILE -> ProfileSubPage(onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.WIFI -> WifiSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.BLUETOOTH -> BluetoothSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.CELLULAR -> CellularSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.PERSONAL_HOTSPOT -> PersonalHotspotSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.SOUNDS -> SoundsSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.GENERAL -> GeneralSubPage(onNavigate = { currentSubPage = it }, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.ABOUT -> AboutSubPage(onBack = { currentSubPage = SettingsSubPage.GENERAL })
                    SettingsSubPage.DISPLAY -> DisplaySubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.WALLPAPER -> WallpaperSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.ACCESSIBILITY -> AccessibilitySubPage(viewModel = viewModel, onNavigate = { currentSubPage = it }, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.FACE_ID -> FaceIdPasscodeSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.SIRI -> SiriSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.EMERGENCY_SOS -> EmergencySosSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.BATTERY -> BatterySubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.SYSTEM_OVERLAY -> SystemOverlaySubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.KEYBOARD -> KeyboardSubPage(viewModel = viewModel, onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.DEFAULT_APPS -> DefaultAppsSubPage(onBack = { currentSubPage = SettingsSubPage.MAIN })
                    SettingsSubPage.APP_SETTINGS -> {
                        AppSettingsSubPage(
                            appName = genericPageTitle.ifEmpty { "Settings" },
                            viewModel = viewModel,
                            onBack = { currentSubPage = SettingsSubPage.MAIN }
                        )
                    }
                    SettingsSubPage.SOFTWARE_UPDATE, SettingsSubPage.STORAGE -> {
                        GenericSettingsSubPage(
                            title = genericPageTitle.ifEmpty { "Settings" },
                            items = genericPageItems.ifEmpty {
                                listOf(Pair("Status", "Configured"), Pair("Version", "18.5"))
                            },
                            onBack = { currentSubPage = SettingsSubPage.MAIN }
                        )
                    }
                    else -> {
                        GenericSettingsSubPage(
                            title = "Settings",
                            items = listOf(Pair("Status", "Configured")),
                            onBack = { currentSubPage = SettingsSubPage.MAIN }
                        )
                    }
                }
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }
    }
}
