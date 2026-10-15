package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.R
import com.example.service.IOSOverlayService
import com.example.ui.components.LockScreenCustomizerSheet
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.util.SiriAiEngine
import com.example.data.IOSPersistenceManager
import com.example.viewmodel.IOSViewModel

enum class SettingsSubPage {
    MAIN,
    PROFILE,
    WIFI,
    BLUETOOTH,
    CELLULAR,
    PERSONAL_HOTSPOT,
    NOTIFICATIONS,
    SOUNDS,
    FOCUS,
    SCREEN_TIME,
    GENERAL,
    ABOUT,
    SOFTWARE_UPDATE,
    STORAGE,
    KEYBOARD,
    DISPLAY,
    WALLPAPER,
    ACCESSIBILITY,
    SIRI,
    FACE_ID,
    EMERGENCY_SOS,
    BATTERY,
    PRIVACY,
    SYSTEM_OVERLAY,
    DEFAULT_APPS,
    APP_SETTINGS,
    DATE_TIME,
    LANGUAGE,
    VPN,
    RESET,
    ACTION_BUTTON,
    CAMERA_CONTROL,
    STANDBY,
    BACK_TAP,
    DEVELOPER,
    BATTERY_HEALTH,
    DYNAMIC_ISLAND
}

@Composable
fun SettingsTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF007AFF)
            )
        }
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black
        )
    }
}

@Composable
fun IOSSettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White),
        content = content
    )
}

@Composable
fun IOSSettingsRow(
    title: String,
    detail: String = "",
    icon: ImageVector? = null,
    iconBg: Color = Color.Transparent,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun IOSSettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconBg: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE5E5EA)
            )
        )
    }
}

// 1. Profile / Apple Account SubPage
@Composable
fun ProfileSubPage(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Apple Account", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8E8E93)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AS",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aherdi Soeprapto",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "aherdi.soeprapto@icloud.com",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Personal Information", detail = "Name, Phone, Email", icon = Icons.Rounded.Person, iconBg = Color(0xFF007AFF))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Sign-In & Security", detail = "Password, 2FA", icon = Icons.Rounded.Security, iconBg = Color(0xFF5856D6))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Payment & Shipping", detail = "Apple Pay, Visa •••• 4242", icon = Icons.Rounded.Payment, iconBg = Color(0xFF34C759))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Subscriptions", detail = "Apple One (Family)", icon = Icons.Rounded.Loyalty, iconBg = Color(0xFFFF2D55))
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "iCloud", detail = "142 GB of 200 GB Used", icon = Icons.Rounded.Cloud, iconBg = Color(0xFF007AFF))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Media & Purchases", detail = "", icon = Icons.Rounded.ShoppingBag, iconBg = Color(0xFF007AFF))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Find My", detail = "On", icon = Icons.Rounded.LocationOn, iconBg = Color(0xFF34C759))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Family", detail = "5 Members", icon = Icons.Rounded.People, iconBg = Color(0xFF5856D6))
                }
            }

            item {
                Text("DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "iPhone 17 Pro Max", detail = "This iPhone", icon = Icons.Rounded.PhoneIphone, iconBg = Color(0xFF8E8E93))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "MacBook Pro 16\"", detail = "macOS Sequoia", icon = Icons.Rounded.LaptopMac, iconBg = Color(0xFF8E8E93))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "AirPods Pro (2nd Gen)", detail = "Connected", icon = Icons.Rounded.Headphones, iconBg = Color(0xFF8E8E93))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Apple Watch Series 10", detail = "watchOS 11", icon = Icons.Rounded.Watch, iconBg = Color(0xFF8E8E93))
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 2. Wi-Fi SubPage
@Composable
fun WifiSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val wifiEnabled by viewModel.wifiEnabled.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val currentWifiSsid by viewModel.currentWifiSsid.collectAsState()
    val scannedNetworks by viewModel.scannedWifiList.collectAsState()
    val isWifiScanning by viewModel.isWifiScanning.collectAsState()

    var askToJoin by remember { mutableStateOf(true) }
    var autoHotspot by remember { mutableStateOf("Automatic") }
    var selectedNetworkToJoin by remember { mutableStateOf<String?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var showNetworkDetails by remember { mutableStateOf(false) }

    LaunchedEffect(wifiEnabled) {
        if (wifiEnabled) {
            viewModel.scanWifiNetworks(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Wi-Fi", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Wi-Fi",
                        checked = wifiEnabled,
                        onCheckedChange = { viewModel.toggleWifi() },
                        icon = Icons.Rounded.Wifi,
                        iconBg = Color(0xFF007AFF)
                    )
                }
            }

            if (wifiEnabled) {
                if (isWifiConnected) {
                    item {
                        Text("CURRENT NETWORK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                    }
                    item {
                        IOSSettingsCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showNetworkDetails = !showNetworkDetails }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                    Column {
                                        Text(currentWifiSsid, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
                                        Text("Low Data Mode: Off • IP: 192.168.1.104", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Icon(Icons.Rounded.Info, contentDescription = "Details", tint = Color(0xFF007AFF), modifier = Modifier.size(22.dp))
                            }

                            if (showNetworkDetails) {
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(title = "Forget This Network", detail = "", onClick = {
                                    viewModel.toggleWifi()
                                    viewModel.toggleWifi()
                                    showNetworkDetails = false
                                })
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(title = "Private Wi-Fi Address", detail = "Rotating")
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(title = "Router", detail = "192.168.1.1")
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(title = "Subnet Mask", detail = "255.255.255.0")
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                IOSSettingsRow(title = "DNS", detail = "1.1.1.1, 8.8.8.8")
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AVAILABLE NETWORKS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (isWifiScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFF007AFF))
                            }
                            IconButton(onClick = { viewModel.scanWifiNetworks(context) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Scan", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                item {
                    IOSSettingsCard {
                        if (scannedNetworks.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isWifiScanning) "Scanning for Wi-Fi networks..." else "No Wi-Fi networks found. Tap refresh to scan.",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            scannedNetworks.forEachIndexed { index, (name, isLocked) ->
                            val isThisConnected = isWifiConnected && currentWifiSsid == name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isThisConnected) {
                                            showNetworkDetails = true
                                        } else if (isLocked) {
                                            selectedNetworkToJoin = name
                                            passwordInput = ""
                                        } else {
                                            viewModel.connectToWifi(context, name, "")
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isThisConnected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                                    }
                                    Text(
                                        text = name,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        fontWeight = if (isThisConnected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (isLocked) {
                                        Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                    Icon(Icons.Rounded.Wifi, contentDescription = "Signal", tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                                    Icon(Icons.Rounded.Info, contentDescription = "Info", tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                }
                            }
                            if (index < scannedNetworks.size - 1) {
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            }
                        }
                    }
                }
                }

                item {
                    IOSSettingsCard {
                        IOSSettingsToggleRow(
                            title = "Ask to Join Networks",
                            checked = askToJoin,
                            onCheckedChange = { askToJoin = it }
                        )
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsRow(
                            title = "Auto-Join Hotspot",
                            detail = autoHotspot,
                            onClick = {
                                autoHotspot = if (autoHotspot == "Ask to Join") "Automatic" else "Ask to Join"
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Wi-Fi Password Join Dialog
    if (selectedNetworkToJoin != null) {
        AlertDialog(
            onDismissRequest = { selectedNetworkToJoin = null },
            title = { Text("Enter Password for \"$selectedNetworkToJoin\"") },
            text = {
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    placeholder = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val net = selectedNetworkToJoin!!
                        viewModel.connectToWifi(context, net, passwordInput)
                        selectedNetworkToJoin = null
                    }
                ) {
                    Text("Join", fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedNetworkToJoin = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// 3. Bluetooth SubPage
@Composable
fun BluetoothSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val bluetoothEnabled by viewModel.bluetoothEnabled.collectAsState()
    val pairedDevices by viewModel.pairedBluetoothDevices.collectAsState()
    val otherDevices by viewModel.discoveredBluetoothDevices.collectAsState()
    val isScanning by viewModel.isBluetoothScanning.collectAsState()

    LaunchedEffect(bluetoothEnabled) {
        if (bluetoothEnabled) {
            viewModel.scanBluetoothDevices(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Bluetooth", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Bluetooth",
                        checked = bluetoothEnabled,
                        onCheckedChange = { viewModel.toggleBluetooth(context) },
                        icon = Icons.Rounded.Bluetooth,
                        iconBg = Color(0xFF007AFF)
                    )
                }
                Text(
                    text = "This iPhone is discoverable as \"iPhone 17 Pro Max\" while Bluetooth Settings is open.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }

            if (bluetoothEnabled) {
                if (pairedDevices.isNotEmpty()) {
                    item {
                        Text("MY DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                    }

                    item {
                        IOSSettingsCard {
                            pairedDevices.forEachIndexed { index, (name, isConnected) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.connectBluetoothDevice(context, name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 16.sp, color = Color.Black)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isConnected) "Connected" else "Not Connected",
                                            color = if (isConnected) Color(0xFF34C759) else Color.Gray,
                                            fontSize = 14.sp
                                        )
                                        Icon(Icons.Rounded.Info, contentDescription = "Info", tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (index < pairedDevices.size - 1) {
                                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OTHER DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Gray,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Scan Again",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF007AFF),
                                modifier = Modifier
                                    .clickable { viewModel.scanBluetoothDevices(context) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                item {
                    if (otherDevices.isNotEmpty()) {
                        IOSSettingsCard {
                            otherDevices.forEachIndexed { index, (name, _) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.connectBluetoothDevice(context, name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 16.sp, color = Color.Black)
                                    Icon(Icons.Rounded.Info, contentDescription = "Info", tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                }
                                if (index < otherDevices.size - 1) {
                                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                }
                            }
                        }
                    } else {
                        IOSSettingsCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.scanBluetoothDevices(context) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isScanning) "Searching for nearby devices..." else "Tap to scan for Bluetooth devices",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// 4. Cellular SubPage
@Composable
fun CellularSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val cellularEnabled by viewModel.cellularEnabled.collectAsState()
    val cellularCarrierName by viewModel.cellularCarrierName.collectAsState()
    var dataRoaming by remember { mutableStateOf(false) }
    var voiceData by remember { mutableStateOf("5G Auto") }
    var lowDataMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Cellular", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Cellular Data",
                        checked = cellularEnabled,
                        onCheckedChange = { viewModel.toggleCellular() },
                        icon = Icons.Rounded.SignalCellularAlt,
                        iconBg = Color(0xFF34C759)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Cellular Data Options", detail = if (cellularEnabled) "Roaming Off • $voiceData" else "Off")
                }
            }

            if (cellularEnabled) {
                item {
                    IOSSettingsCard {
                        IOSSettingsToggleRow(
                            title = "Data Roaming",
                            checked = dataRoaming,
                            onCheckedChange = {
                                dataRoaming = it
                                viewModel.showDynamicIslandNotification("Data Roaming", if (it) "Roaming Enabled" else "Roaming Disabled", "cellular")
                                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    com.example.util.RootExecutor.execute("settings put global data_roaming ${if (it) 1 else 0}")
                                }
                            }
                        )
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsRow(
                            title = "Voice & Data",
                            detail = voiceData,
                            onClick = {
                                voiceData = when (voiceData) {
                                    "5G Auto" -> "5G On"
                                    "5G On" -> "LTE / 4G"
                                    else -> "5G Auto"
                                }
                                viewModel.showDynamicIslandNotification("Voice & Data", voiceData, "cellular")
                                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val mode = if (voiceData.contains("LTE")) 9 else 20
                                    com.example.util.RootExecutor.execute("settings put global preferred_network_mode $mode")
                                }
                            }
                        )
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsToggleRow(
                            title = "Low Data Mode",
                            checked = lowDataMode,
                            onCheckedChange = { lowDataMode = it }
                        )
                    }
                }

                item {
                    Text("SIMS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                }

                item {
                    IOSSettingsCard {
                        IOSSettingsRow(
                            title = if (cellularCarrierName.isNotBlank() && cellularCarrierName != "Cellular") "$cellularCarrierName (Primary SIM)" else "Primary SIM (Cellular)",
                            detail = if (cellularEnabled) "Active • Primary" else "Off",
                            icon = Icons.Rounded.SimCard,
                            iconBg = Color(0xFF007AFF)
                        )
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsRow(title = "Wi-Fi Calling", detail = "On")
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsRow(title = "Carrier Services", detail = "Active")
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 5. General & About SubPage
@Composable
fun AboutSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val spoofedModel by viewModel.spoofedModel.collectAsState()
    val retinaDpi by viewModel.retinaDpi.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()

    val totalApps = remember {
        try {
            context.packageManager.getInstalledApplications(0).size.toString()
        } catch (e: Exception) {
            "42"
        }
    }

    val (capacityStr, availableStr) = remember {
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
            val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            val totalGb = String.format("%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
            val freeGb = String.format("%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0))
            Pair(totalGb, freeGb)
        } catch (e: Exception) {
            Pair("64.0 GB", "38.5 GB")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "About", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Name", detail = spoofedModel)
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "iOS Version", detail = "26.2 (Build 22F82)")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Model Name", detail = spoofedModel)
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Physical Device", detail = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Android Base", detail = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Screen Density", detail = "$retinaDpi DPI")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Root Privilege", detail = if (isRootAvailable) "Root #SU Active" else "Shell Standard")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Limited Warranty", detail = "AppleCare+ Active")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Songs", detail = "120")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Videos", detail = "14")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Photos", detail = "1,420")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Applications", detail = totalApps)
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Capacity", detail = capacityStr)
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Available", detail = availableStr)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 6. Sounds & Haptics SubPage (Unified real volume control & system pipeline)
@Composable
fun SoundsSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val volume by viewModel.volume.collectAsState()

    var selectedRingtone by remember { mutableStateOf("Reflection (Default)") }
    var currentPlayingRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }
    var currentlyPlayingName by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                currentPlayingRingtone?.stop()
            } catch (ignored: Exception) {}
        }
    }

    var hapticsEnabled by remember { mutableStateOf(IOSFeedbackHelper.hapticsEnabled) }
    var lockSoundEnabled by remember { mutableStateOf(IOSFeedbackHelper.soundsEnabled) }
    var keyboardClicks by remember { mutableStateOf(IOSFeedbackHelper.soundsEnabled) }
    var silentMode by remember {
        mutableStateOf(audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT || audioManager?.ringerMode == AudioManager.RINGER_MODE_VIBRATE)
    }

    val ringtones = listOf("Reflection (Default)", "Opening", "Chimes", "Apex", "Beacon", "Radiate", "Sencha")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Sounds & Haptics", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("RINGTONE AND ALERTS VOLUME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = Color.Gray)
                            Slider(
                                value = volume,
                                onValueChange = { newVal ->
                                    viewModel.setVolume(newVal, context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF007AFF)
                                )
                            )
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Silent Mode (Hening)",
                        checked = silentMode,
                        onCheckedChange = { isSilent ->
                            silentMode = isSilent
                            try {
                                audioManager?.ringerMode = if (isSilent) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
                            } catch (ignored: Exception) {}
                            viewModel.showDynamicIslandNotification(
                                title = if (isSilent) "Silent Mode On" else "Silent Mode Off",
                                message = if (isSilent) "Ringer is silenced" else "Ringer is active",
                                icon = if (isSilent) "bell.slash" else "bell",
                                accent = if (isSilent) 0xFFFF3B30 else 0xFF34C759
                            )
                        }
                    )
                }
            }

            item {
                Text("SOUNDS AND HAPTIC PATTERNS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    ringtones.forEachIndexed { idx, ringtone ->
                        val isPlaying = currentlyPlayingName == ringtone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRingtone = ringtone
                                    if (isPlaying) {
                                        // Stop playing if already active
                                        try {
                                            currentPlayingRingtone?.stop()
                                        } catch (ignored: Exception) {}
                                        currentPlayingRingtone = null
                                        currentlyPlayingName = null
                                    } else {
                                        // Stop previously playing ringtone
                                        try {
                                            currentPlayingRingtone?.stop()
                                        } catch (ignored: Exception) {}
                                        try {
                                            val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                                            val r = RingtoneManager.getRingtone(context, defaultUri)
                                            r.play()
                                            currentPlayingRingtone = r
                                            currentlyPlayingName = ringtone
                                            viewModel.showDynamicIslandNotification("Ringtone Preview", ringtone, "music")
                                        } catch (ignored: Exception) {}
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isPlaying) {
                                    Icon(
                                        Icons.Rounded.GraphicEq,
                                        contentDescription = "Playing",
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    ringtone,
                                    fontSize = 16.sp,
                                    color = if (isPlaying) Color(0xFF007AFF) else Color.Black,
                                    fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isPlaying) {
                                    Text("Hentikan", fontSize = 12.sp, color = Color.Gray)
                                }
                                if (selectedRingtone == ringtone) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        if (idx < ringtones.size - 1) {
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Keyboard Feedback (Clicks)",
                        checked = keyboardClicks,
                        onCheckedChange = {
                            keyboardClicks = it
                            IOSFeedbackHelper.soundsEnabled = it
                            if (it) IOSFeedbackHelper.playClickSound(context)
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Lock Sound",
                        checked = lockSoundEnabled,
                        onCheckedChange = {
                            lockSoundEnabled = it
                            IOSFeedbackHelper.soundsEnabled = it
                            if (it) IOSFeedbackHelper.playLockSound(context)
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "System Haptics",
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            IOSFeedbackHelper.hapticsEnabled = it
                            if (it) IOSFeedbackHelper.vibrateLight(context)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 7. System Overlay SubPage (Dynamic Island & Status Bar across all apps)
@Composable
fun SystemOverlaySubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var isOverlayActive by remember { mutableStateOf(IOSOverlayService.isRunning) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Dynamic Island di Semua Aplikasi", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Aktifkan Status Bar & Dynamic Island",
                        checked = isOverlayActive,
                        onCheckedChange = { enable ->
                            isOverlayActive = enable
                            if (enable) {
                                IOSOverlayService.start(context)
                                viewModel.showDynamicIslandNotification("Dynamic Island Aktif", "Tampil di atas semua aplikasi Android", "bell")
                            } else {
                                IOSOverlayService.stop(context)
                                viewModel.showDynamicIslandNotification("Dynamic Island Mati", "Overlay dihentikan", "bell")
                            }
                        },
                        icon = Icons.Rounded.Layers,
                        iconBg = Color(0xFF5856D6)
                    )
                }
                Text(
                    text = "Fitur ini menampilkan Dynamic Island dan iOS Status Bar melayang secara permanen di atas semua aplikasi Android (WhatsApp, Chrome, YouTube, Game, dll).",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Aktifkan Izin System Overlay Langsung",
                        detail = "Pipeline Root",
                        icon = Icons.Rounded.Security,
                        iconBg = Color(0xFF34C759),
                        onClick = {
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.util.RootExecutor.execute("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    isOverlayActive = true
                                    IOSOverlayService.start(context)
                                    viewModel.showDynamicIslandNotification("Dynamic Island", "Izin Overlay Aktif via System Pipeline", "shield", 0xFF34C759)
                                }
                            }
                        }
                    )
                }
            }

            item {
                val isVolumeInterceptionActive by viewModel.isVolumeInterceptionActive.collectAsState()

                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Blokir Dialog Volume Android",
                        checked = isVolumeInterceptionActive,
                        onCheckedChange = {
                            viewModel.toggleVolumeInterceptionRoot(context)
                        },
                        icon = Icons.Rounded.VolumeUp,
                        iconBg = Color(0xFF007AFF)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Buka Aksesibilitas Sistem (Manual)",
                        detail = if (isVolumeInterceptionActive) "Aktif" else "Atur",
                        icon = Icons.Rounded.Accessibility,
                        iconBg = Color(0xFFFF9500),
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.showDynamicIslandNotification("Aksesibilitas", "Buka Pengaturan -> Aksesibilitas", "volume", 0xFFFF9500)
                            }
                        }
                    )
                }
                Text(
                    text = "Mencegah slider/dialog volume bawaan Android muncul saat menekan tombol volume fisik di WhatsApp, Setelan, YouTube, dll. Volume dialihkan sepenuhnya ke pil Dynamic Island.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 8. iOS Keyboard Settings SubPage
@Composable
fun KeyboardSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Keyboard", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Keyboards",
                        detail = "iOS Keyboard, English",
                        icon = Icons.Rounded.Keyboard,
                        iconBg = Color(0xFF007AFF)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Auto-Capitalization", checked = true, onCheckedChange = {})
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Auto-Correction", checked = true, onCheckedChange = {})
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Check Spelling", checked = true, onCheckedChange = {})
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Enable Dictation", checked = true, onCheckedChange = {})
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Character Preview", checked = true, onCheckedChange = {})
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Aktifkan iOS Keyboard ke Sistem",
                        detail = "Pipeline Langsung",
                        icon = Icons.Rounded.Keyboard,
                        iconBg = Color(0xFF007AFF),
                        onClick = {
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.util.RootExecutor.execute("ime enable ${context.packageName}/.service.IOSKeyboardService && ime set ${context.packageName}/.service.IOSKeyboardService")
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    viewModel.showDynamicIslandNotification("Keyboard", "iOS Keyboard Diaktifkan", "keyboard", 0xFF007AFF)
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 9. Default Apps SubPage
@Composable
fun DefaultAppsSubPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var defaultBrowser by remember { mutableStateOf("Safari (Default)") }
    var defaultCalling by remember { mutableStateOf("Phone (iOS)") }
    var defaultMessaging by remember { mutableStateOf("Messages (iOS)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Default Apps", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Default Browser App", detail = defaultBrowser, icon = Icons.Rounded.Explore, iconBg = Color(0xFF007AFF))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Default Phone App", detail = defaultCalling, icon = Icons.Rounded.Phone, iconBg = Color(0xFF34C759))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Default Messaging App", detail = defaultMessaging, icon = Icons.Rounded.ChatBubble, iconBg = Color(0xFF34C759))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Default Mail App", detail = "Mail", icon = Icons.Rounded.Email, iconBg = Color(0xFF007AFF))
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 10. Battery SubPage (Real Battery Level & Low Power Mode)
@Composable
fun BatterySubPage(viewModel: IOSViewModel, onNavigate: (SettingsSubPage) -> Unit = {}, onBack: () -> Unit) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isBatteryCharging.collectAsState()
    var lowPowerMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Battery", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Low Power Mode",
                        checked = lowPowerMode,
                        onCheckedChange = {
                            lowPowerMode = it
                            viewModel.showDynamicIslandNotification(
                                title = if (it) "Low Power Mode On" else "Low Power Mode Off",
                                message = if (it) "Background activity reduced" else "Normal power usage",
                                icon = "battery",
                                accent = if (it) 0xFFFFCC00 else 0xFF34C759
                            )
                        },
                        icon = Icons.Rounded.BatteryChargingFull,
                        iconBg = Color(0xFFFFCC00)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Battery Percentage", checked = true, onCheckedChange = {})
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Battery Health & Charging",
                        detail = "Maximum Capacity 98% (Normal)",
                        icon = Icons.Rounded.HealthAndSafety,
                        iconBg = Color(0xFF34C759),
                        onClick = { onNavigate(SettingsSubPage.BATTERY_HEALTH) }
                    )
                }
            }

            item {
                Text("LAST 24 HOURS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Current Level", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Text("$batteryLevel% ${if (isCharging) "(Charging)" else ""}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isCharging) Color(0xFF34C759) else Color.Black)
                        }

                        // Battery Bar
                        LinearProgressIndicator(
                            progress = { batteryLevel / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (lowPowerMode) Color(0xFFFFCC00) else if (batteryLevel <= 20) Color(0xFFFF3B30) else Color(0xFF34C759),
                            trackColor = Color(0xFFE5E5EA)
                        )

                        Text("Screen Active: 5h 28m • Screen Idle: 1h 14m", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 11. General SubPage
@Composable
fun GeneralSubPage(
    viewModel: IOSViewModel,
    onNavigate: (SettingsSubPage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showShutdownDialog by remember { mutableStateOf(false) }

    if (showShutdownDialog) {
        AlertDialog(
            onDismissRequest = { showShutdownDialog = false },
            title = { Text("Matikan Perangkat", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin mematikan perangkat ini?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShutdownDialog = false
                        viewModel.powerOffRoot()
                    }
                ) {
                    Text("Matikan (Shut Down)", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showShutdownDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "General", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "About", detail = "iPhone 17 Pro Max", onClick = { onNavigate(SettingsSubPage.ABOUT) })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Software Update", detail = "iOS 26.2", onClick = { onNavigate(SettingsSubPage.SOFTWARE_UPDATE) })
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "AirDrop", detail = "Contacts Only")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "AirPlay & Continuity", detail = "")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Picture in Picture", detail = "Automatic")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "CarPlay", detail = "")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "iPhone Storage", detail = "Storage Overview", onClick = { onNavigate(SettingsSubPage.STORAGE) })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Background App Refresh", detail = "On")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Date & Time",
                        detail = "Auto",
                        onClick = { onNavigate(SettingsSubPage.DATE_TIME) }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Keyboard", detail = "iOS Keyboard", onClick = { onNavigate(SettingsSubPage.KEYBOARD) })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Language & Region",
                        detail = "Indonesian / English",
                        onClick = { onNavigate(SettingsSubPage.LANGUAGE) }
                    )
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "VPN & Device Management",
                        detail = "Not Connected",
                        onClick = { onNavigate(SettingsSubPage.VPN) }
                    )
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Transfer or Reset iPhone",
                        detail = "",
                        onClick = { onNavigate(SettingsSubPage.RESET) }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Shut Down",
                        detail = "",
                        onClick = { showShutdownDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Dedicated Storage SubPage with real system storage analysis
@Composable
fun StorageSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()

    val (usedGb, totalGb, freeGb) = remember {
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
            val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            Triple(
                usedBytes / (1024.0 * 1024.0 * 1024.0),
                totalBytes / (1024.0 * 1024.0 * 1024.0),
                freeBytes / (1024.0 * 1024.0 * 1024.0)
            )
        } catch (e: Exception) {
            Triple(28.4, 64.0, 35.6)
        }
    }

    val usedFraction = (usedGb / totalGb.coerceAtLeast(1.0)).toFloat().coerceIn(0.05f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "iPhone Storage", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = String.format("%.1f GB of %.0f GB Used", usedGb, totalGb),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Text(
                                text = String.format("%.1f GB Free", freeGb),
                                fontSize = 13.sp,
                                color = Color(0xFF34C759),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // iOS-style storage color bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFE5E5EA))
                        ) {
                            // Apps (Red/Orange)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.45f * usedFraction)
                                    .background(Color(0xFFFF3B30))
                            )
                            // System (Gray)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.25f * usedFraction)
                                    .background(Color(0xFF8E8E93))
                            )
                            // Photos/Media (Yellow)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.15f * usedFraction)
                                    .background(Color(0xFFFFCC00))
                            )
                            // Cache & Others (Blue)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.15f * usedFraction)
                                    .background(Color(0xFF007AFF))
                            )
                            // Free remaining
                            if (usedFraction < 0.98f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f - usedFraction)
                                        .background(Color(0xFFE5E5EA))
                                )
                            }
                        }

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
                                Text("Apps", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8E8E93)))
                                Text("System", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFCC00)))
                                Text("Media", fontSize = 11.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF007AFF)))
                                Text("Other", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            item {
                Text("OPTIMIZATION & TOOLS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Bersihkan RAM & Cache Memori (Root)",
                        detail = "Clean Cache",
                        icon = Icons.Rounded.Speed,
                        iconBg = Color(0xFF34C759),
                        onClick = {
                            viewModel.cleanJ2PrimeRamRoot()
                        }
                    )
                }
            }

            item {
                Text("INSTALLED APPLICATIONS (${installedApps.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    installedApps.take(15).forEachIndexed { idx, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        com.example.util.RootExecutor.execute("pm clear ${app.packageName} || rm -rf /data/data/${app.packageName}/cache/*")
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            viewModel.showDynamicIslandNotification(app.appName, "Cache berhasil dibersihkan", "trash", 0xFF007AFF)
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap.asImageBitmap(),
                                        contentDescription = app.appName,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF007AFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(app.appName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                                Column {
                                    Text(app.appName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.Black)
                                    Text(app.packageName, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp))
                        }
                        if (idx < installedApps.take(15).size - 1) {
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Dedicated Software Update SubPage
@Composable
fun SoftwareUpdateSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var autoUpdates by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Software Update", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Automatic Updates",
                        checked = autoUpdates,
                        onCheckedChange = { autoUpdates = it },
                        icon = Icons.Rounded.Sync,
                        iconBg = Color(0xFF34C759)
                    )
                }
            }

            item {
                IOSSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(54.dp)
                        )
                        Text("iOS 26.2", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Your iPhone is up to date with the latest features and security enhancements.", fontSize = 14.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }

            item {
                var isChecking by remember { mutableStateOf(false) }
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isChecking) {
                                isChecking = true
                                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    kotlinx.coroutines.delay(1500)
                                    isChecking = false
                                    viewModel.showDynamicIslandNotification("Software Update", "iOS 26.2 is up to date", "checkmark.circle", 0xFF34C759)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF007AFF),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Checking for Update...", color = Color(0xFF007AFF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("Check for Updates", color = Color(0xFF007AFF), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 12. Display & Brightness SubPage (Direct pipeline to system & window brightness)
@Composable
fun DisplaySubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    var trueTone by remember { mutableStateOf(true) }
    var nightShift by remember { mutableStateOf(false) }
    var autoLock by remember { mutableStateOf("30 Seconds") }
    var alwaysOn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Display & Brightness", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("APPEARANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setDarkMode(false) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 70.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Light", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            RadioButton(selected = !isDarkMode, onClick = { viewModel.setDarkMode(false) })
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setDarkMode(true) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 70.dp, height = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Dark", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            RadioButton(selected = isDarkMode, onClick = { viewModel.setDarkMode(true) })
                        }
                    }
                }
            }

            item {
                Text("BRIGHTNESS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.BrightnessLow, contentDescription = null, tint = Color.Gray)
                            Slider(
                                value = brightness,
                                onValueChange = { newVal ->
                                    viewModel.setBrightness(newVal, context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF007AFF)
                                )
                            )
                            Icon(Icons.Rounded.BrightnessHigh, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "True Tone", checked = trueTone, onCheckedChange = { trueTone = it })
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Night Shift", detail = if (nightShift) "On" else "Off", onClick = { nightShift = !nightShift })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Auto-Lock", detail = autoLock, onClick = {
                        autoLock = when (autoLock) {
                            "30 Seconds" -> "1 Minute"
                            "1 Minute" -> "2 Minutes"
                            "2 Minutes" -> "5 Minutes"
                            else -> "30 Seconds"
                        }
                    })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(title = "Always On Display", checked = alwaysOn, onCheckedChange = { alwaysOn = it })
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 13. Personal Hotspot SubPage
@Composable
fun PersonalHotspotSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val hotspotEnabled by viewModel.hotspotEnabled.collectAsState()
    var password by remember { mutableStateOf("ios26_hotspot") }
    var maxCompatibility by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Personal Hotspot", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Allow Others to Join",
                        checked = hotspotEnabled,
                        onCheckedChange = { viewModel.toggleHotspot() },
                        icon = Icons.Rounded.WifiTethering,
                        iconBg = Color(0xFF34C759)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Wi-Fi Password",
                        detail = password,
                        onClick = { showEditDialog = true }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Maximize Compatibility",
                        checked = maxCompatibility,
                        onCheckedChange = { maxCompatibility = it }
                    )
                }
                Text(
                    text = "Personal Hotspot on your iPhone provides Internet access to other devices signed in to your iCloud account without requiring you to enter the password.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }

            item {
                Text("CONNECTED DEVICES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    if (hotspotEnabled) {
                        IOSSettingsRow(title = "MacBook Pro M3 Max", detail = "Connected • 5 GHz")
                    } else {
                        IOSSettingsRow(title = "No devices connected", detail = "")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showEditDialog) {
        var tempPass by remember { mutableStateOf(password) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Wi-Fi Password") },
            text = {
                OutlinedTextField(
                    value = tempPass,
                    onValueChange = { tempPass = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempPass.length >= 8) {
                            password = tempPass
                            viewModel.showDynamicIslandNotification("Personal Hotspot", "Password Updated", "wifi")
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// 14. iOS 26 Style Wallpaper SubPage
data class WallpaperChoice(
    val id: String,
    val name: String,
    val category: String,
    val resId: Int,
    val hasDepthEffect: Boolean = false,
    val depthTheme: String = "none"
)

@Composable
fun WallpaperSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val currentWallpaperRes by viewModel.currentWallpaperRes.collectAsState()
    val currentWallpaperName by viewModel.currentWallpaperName.collectAsState()
    val currentWallpaperUri by viewModel.currentWallpaperUri.collectAsState()
    val homeScreenWallpaperRes by viewModel.homeScreenWallpaperRes.collectAsState()
    val homeScreenWallpaperUri by viewModel.homeScreenWallpaperUri.collectAsState()
    val lockScreenConfig by viewModel.lockScreenConfig.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setWallpaper(
                resId = 0,
                name = "Custom Photo",
                uri = uri.toString(),
                context = context
            )
            viewModel.updateLockScreenConfig { it.copy(wallpaperId = "custom") }
        }
    }

    val homePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setHomeScreenWallpaper(
                resId = 0,
                name = "Custom Home Photo",
                uri = uri.toString(),
                context = context
            )
        }
    }

    val wallpapers = remember {
        listOf(
            WallpaperChoice("mountain_depth", "Alpine Mountain Peak", "Classic iOS 26 Depth Effect", R.drawable.wallpaper_alpine, hasDepthEffect = true, depthTheme = "mountain"),
            WallpaperChoice("twilight_clouds", "Twilight City Sunset", "Desain 1 • Awan Senja Emas", R.drawable.wp_twilight_clouds, hasDepthEffect = true, depthTheme = "twilight_clouds"),
            WallpaperChoice("monochrome_wave", "Monochrome Tidal Glow", "Desain 2 • Ombak Laut Hitam Putih", R.drawable.wp_monochrome_wave, hasDepthEffect = true, depthTheme = "monochrome_wave"),
            WallpaperChoice("golden_dunes", "Golden Desert Dunes", "Desain 3 • Gumuk Pasir Sahara", R.drawable.wp_golden_dunes, hasDepthEffect = true, depthTheme = "golden_dunes"),
            WallpaperChoice("coastal_flora", "Golden Coastal Sunset", "Desain 4 • Tumbuhan Tebing Laut", R.drawable.wp_coastal_flora, hasDepthEffect = true, depthTheme = "coastal_flora"),
            WallpaperChoice("daisy_meadow", "Dreamy Daisy Meadow", "Desain 5 • Bunga Daisy Pegunungan", R.drawable.wp_daisy_meadow, hasDepthEffect = true, depthTheme = "daisy_meadow"),
            WallpaperChoice("sunset_reeds", "Golden Lake Reeds", "Desain 6 • Rumput Emas Danau", R.drawable.wp_sunset_reeds, hasDepthEffect = true, depthTheme = "sunset_reeds"),
            WallpaperChoice("crystal_glass", "Crystal Glass Sky", "Desain 7 • Pita Kaca Kristal", R.drawable.wp_crystal_glass, hasDepthEffect = true, depthTheme = "crystal_glass"),
            WallpaperChoice("glass_sanctuary", "Glass Clock Sanctuary", "Desain 8 • Zen Sanctuary Kaca", R.drawable.wp_glass_sanctuary, hasDepthEffect = true, depthTheme = "glass_sanctuary"),
            WallpaperChoice("glass_still_life", "Glass Still Life", "Desain 9 • Bola Kaca & Pita Akrilik", R.drawable.wp_glass_still_life, hasDepthEffect = true, depthTheme = "glass_still_life")
        )
    }

    var selectedWallpaper by remember { mutableStateOf(wallpapers.firstOrNull { it.resId == currentWallpaperRes } ?: wallpapers[0]) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(IOSSystemGray6)
        ) {
            SettingsTopBar(title = "Wallpaper & Lock Screen", onBack = onBack)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dual Mockup Previews
                item {
                    Text("CURRENT WALLPAPER PAIR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock Screen Mini Mockup (with Depth Effect rendering!)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(125.dp)
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                                .clickable { viewModel.openApp(com.example.model.AppId.DEPTH_STUDIO) }
                        ) {
                            val liveDate = remember {
                                java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(java.util.Date())
                            }
                            com.example.ui.components.IOSDepthWallpaperRenderer(
                                config = lockScreenConfig,
                                wallpaperRes = currentWallpaperRes,
                                wallpaperUri = currentWallpaperUri,
                                currentTime = currentTime,
                                currentDate = liveDate,
                                batteryLevel = 90,
                                isBatteryCharging = false,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bottom quick buttons overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Layar Terkunci", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.openApp(com.example.model.AppId.DEPTH_STUDIO) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Rounded.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Depth Studio", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Home Screen Mini Mockup (Independent Wallpaper!)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(125.dp)
                                .height(220.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                        ) {
                            if (!homeScreenWallpaperUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = homeScreenWallpaperUri,
                                    contentDescription = "Home Screen Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = com.example.ui.components.safePainterResource(id = homeScreenWallpaperRes, fallbackId = R.drawable.wallpaper_alpine),
                                    contentDescription = "Home Screen Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Home icons mini grid
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 22.dp, start = 10.dp, end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(3) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        repeat(4) {
                                            Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.8f)))
                                        }
                                    }
                                }
                            }
                            // Mini Dock
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                                    .align(Alignment.BottomCenter)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(4) {
                                        Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.9f)))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Layar Utama", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                homePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Rounded.Photo, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pilih Foto", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Unified Depth Studio Card for Lockscreen
            item {
                IOSSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(Color(0xFF5856D6), Color(0xFF007AFF))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pengaturan Layar Kunci di Depth Studio",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Semua font, warna jam, posisi free-transform, pinch-zoom, dan efek kedalaman 3D diatur 100% di Depth Studio.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.openApp(com.example.model.AppId.DEPTH_STUDIO)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka AI Depth Wallpaper Studio", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Independent Home Screen Photo Picker Card
            item {
                Text(
                    "PILIH WALLPAPER KHUSUS LAYAR UTAMA (HOMESCREEN)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                homePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF007AFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pilih Foto Sendiri untuk Layar Utama",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Text(
                                text = "Layar utama terpisah tanpa mengubah layar kunci",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.LightGray
                        )
                    }
                }
            }

            item {
                Text(
                    "KOLEKSI WALLPAPER LAYAR UTAMA (HOMESCREEN)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Wallpapers Collection Cards for Home Screen
            items(wallpapers) { wall ->
                val isCurrentHome = homeScreenWallpaperUri.isNullOrBlank() && wall.resId == homeScreenWallpaperRes
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setHomeScreenWallpaper(wall.resId, wall.name, null, context)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = com.example.ui.components.safePainterResource(id = wall.resId, fallbackId = R.drawable.wallpaper_alpine),
                            contentDescription = wall.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 54.dp, height = 80.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wall.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = wall.category,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            if (isCurrentHome) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF34C759).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Layar Utama Aktif", color = Color(0xFF34C759), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isCurrentHome) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected for Home",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    viewModel.setHomeScreenWallpaper(wall.resId, wall.name, null, context)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Pasang", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
}

// 15. Accessibility SubPage (with Integrasi Sistem Android)
@Composable
fun AccessibilitySubPage(
    viewModel: IOSViewModel,
    onNavigate: (SettingsSubPage) -> Unit,
    onBack: () -> Unit
) {
    val backTapEnabled by viewModel.backTapEnabled.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Accessibility", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("VISION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "VoiceOver", detail = "Off", icon = Icons.Rounded.RecordVoiceOver, iconBg = Color(0xFF007AFF))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Zoom", detail = "Off", icon = Icons.Rounded.ZoomIn, iconBg = Color(0xFF34C759))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Display & Text Size", detail = "Default", icon = Icons.Rounded.TextFields, iconBg = Color(0xFF5856D6))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Motion", detail = "Reduce Motion Off", icon = Icons.Rounded.Animation, iconBg = Color(0xFFFF9500))
                }
            }

            item {
                Text("PHYSICAL AND MOTOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Touch", detail = "Haptic Touch Fast", icon = Icons.Rounded.TouchApp, iconBg = Color(0xFF007AFF))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Back Tap (Ketuk Belakang)",
                        detail = if (backTapEnabled) "Nyala" else "Mati",
                        icon = Icons.Rounded.TouchApp,
                        iconBg = Color(0xFF007AFF),
                        onClick = { onNavigate(SettingsSubPage.BACK_TAP) }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Face ID & Attention", detail = "On", icon = Icons.Rounded.Face, iconBg = Color(0xFF34C759))
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Switch Control", detail = "Off", icon = Icons.Rounded.Tune, iconBg = Color(0xFF8E8E93))
                }
            }

            // INTEGRASI SISTEM ANDROID (MOVED HERE TO ACCESSIBILITY)
            item {
                Text("INTEGRASI SISTEM ANDROID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        icon = Icons.Rounded.Layers,
                        iconBg = Color(0xFF5856D6),
                        title = "Status Bar & Dynamic Island Overlay",
                        detail = "Tampil di Semua Aplikasi",
                        onClick = { onNavigate(SettingsSubPage.SYSTEM_OVERLAY) }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        icon = Icons.Rounded.Keyboard,
                        iconBg = Color(0xFF007AFF),
                        title = "Keyboard iOS (Sistem Default)",
                        detail = "Aktifkan Keyboard",
                        onClick = { onNavigate(SettingsSubPage.KEYBOARD) }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        icon = Icons.Rounded.SettingsApplications,
                        iconBg = Color(0xFF34C759),
                        title = "Default Apps",
                        detail = "Browser, SMS, Telepon",
                        onClick = { onNavigate(SettingsSubPage.DEFAULT_APPS) }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 16. Face ID & Passcode SubPage
@Composable
fun FaceIdPasscodeSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isPasscodeEnabled by viewModel.isPasscodeEnabled.collectAsState()
    val isFaceIdEnabled by viewModel.isFaceIdEnabled.collectAsState()
    var showChangePasscodeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Face ID & Passcode", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Turn Passcode On",
                        checked = isPasscodeEnabled,
                        onCheckedChange = { enabled -> viewModel.togglePasscode(enabled) },
                        icon = Icons.Rounded.Lock,
                        iconBg = Color(0xFF007AFF)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Change Passcode",
                        detail = "",
                        onClick = { showChangePasscodeDialog = true }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Simulated Face ID (Experimental)",
                        checked = isFaceIdEnabled,
                        onCheckedChange = { enabled -> viewModel.toggleFaceId(enabled) },
                        icon = Icons.Rounded.Face,
                        iconBg = Color(0xFF34C759)
                    )
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Lock iPhone Now",
                        detail = "Lock Screen",
                        icon = Icons.Rounded.LockClock,
                        iconBg = Color(0xFFFF9500),
                        onClick = { viewModel.lockDevice() }
                    )
                }
                Text(
                    text = "Lock iPhone to test Face ID and Passcode verification immediately.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showChangePasscodeDialog) {
        var newCode by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasscodeDialog = false },
            title = { Text("Set 4-Digit Passcode") },
            text = {
                OutlinedTextField(
                    value = newCode,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newCode = it },
                    placeholder = { Text("Enter 4 digits (e.g. 1234)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCode.length == 4) {
                            viewModel.setPasscode(newCode)
                            showChangePasscodeDialog = false
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasscodeDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// 17. Siri & Apple Intelligence SubPage (Clean & Non-overlapping with Gemini API Config)
@Composable
fun SiriSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var listenForHeySiri by remember { mutableStateOf(true) }
    var pressSideButton by remember { mutableStateOf(true) }
    var allowSiriWhenLocked by remember { mutableStateOf(true) }
    var writingTools by remember { mutableStateOf(true) }

    // Gemini API Key State
    var savedApiKey by remember {
        mutableStateOf(IOSPersistenceManager.getGeminiApiKey(context))
    }
    var selectedModel by remember {
        mutableStateOf(IOSPersistenceManager.getGeminiModel(context))
    }
    var apiKeyInput by remember { mutableStateOf(savedApiKey) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var showApiKeyInfoDialog by remember { mutableStateOf(false) }

    val isGeminiActive = remember(savedApiKey) {
        SiriAiEngine.isGeminiConfigured(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Siri & Intelligence", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "APPLE INTELLIGENCE (GEMINI AI)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Card 1: Gemini API Configuration & Status
            item {
                IOSSettingsCard {
                    // Status Badge Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFF5856D6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Status AI Siri",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Text(
                                    text = if (isGeminiActive) "Aktif (Google Gemini: $selectedModel)" else "Mode Offline (Perintah Lokal)",
                                    fontSize = 12.sp,
                                    color = if (isGeminiActive) Color(0xFF34C759) else Color(0xFFFF9500),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(onClick = { showApiKeyInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = "Bantuan API Key",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                    // Model Selection Chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Pilih Model AI",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("gemini-flash-latest", "gemini-3.1-flash-lite-preview", "gemini-3.5-flash", "gemini-3.8-flash").forEach { modelName ->
                                val isSelected = selectedModel == modelName
                                val label = when (modelName) {
                                    "gemini-flash-latest" -> "⚡ Flash (Cepat)"
                                    "gemini-3.1-flash-lite-preview" -> "🚀 Flash Lite"
                                    "gemini-3.5-flash" -> "3.5 Flash"
                                    "gemini-3.8-flash" -> "3.8 Flash"
                                    else -> "Flash"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5EA))
                                        .clickable {
                                            selectedModel = modelName
                                            IOSPersistenceManager.saveGeminiModel(context, modelName)
                                            testResultStatus = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                    // Input Field for Gemini API Key
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Google Gemini API Key",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it.trim()
                                testResultStatus = null
                            },
                            placeholder = { Text("Tempel API Key AI Studio di sini...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFD1D1D6)
                            )
                        )

                        // Action Buttons: Simpan & Tes Koneksi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    IOSPersistenceManager.saveGeminiApiKey(context, apiKeyInput)
                                    IOSPersistenceManager.saveGeminiModel(context, selectedModel)
                                    savedApiKey = apiKeyInput
                                    viewModel.showDynamicIslandNotification("Siri", "API Key disimpan!", "sparkles", 0xFF34C759)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Key", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val keyToTest = apiKeyInput.ifBlank { savedApiKey }
                                    isTestingConnection = true
                                    testResultStatus = null
                                    coroutineScope.launch {
                                        val result = SiriAiEngine.testApiKey(keyToTest, context)
                                        isTestingConnection = false
                                        testResultStatus = result
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                enabled = !isTestingConnection && (apiKeyInput.isNotBlank() || savedApiKey.isNotBlank())
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Menguji...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Rounded.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tes Koneksi", fontSize = 13.sp)
                                }
                            }
                        }

                        // Test Status Banner
                        testResultStatus?.let { (success, message) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (success) Color(0x2034C759) else Color(0x20FF3B30))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = message,
                                    color = if (success) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (savedApiKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    IOSPersistenceManager.saveGeminiApiKey(context, "")
                                    savedApiKey = ""
                                    apiKeyInput = ""
                                    testResultStatus = null
                                    viewModel.showDynamicIslandNotification("Siri", "API Key dihapus", "delete", 0xFFFF3B30)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Hapus API Key", color = Color(0xFFFF3B30), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "FITUR KECERDASAN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Writing Tools & Chat Pintar",
                        checked = writingTools,
                        onCheckedChange = { writingTools = it },
                        icon = Icons.Rounded.Edit,
                        iconBg = Color(0xFF5856D6)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Bahasa Respon Siri",
                        detail = "Bahasa Indonesia (Natural)"
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Model AI",
                        detail = if (isGeminiActive) "gemini-2.5-flash (Online)" else "Smart Rule Engine (Offline)"
                    )
                }
            }

            item {
                Text(
                    text = "ASK SIRI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Listen for \"Hey Siri\"",
                        checked = listenForHeySiri,
                        onCheckedChange = { listenForHeySiri = it },
                        icon = Icons.Rounded.RecordVoiceOver,
                        iconBg = Color(0xFF5856D6)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Press Side Button for Siri",
                        checked = pressSideButton,
                        onCheckedChange = { pressSideButton = it }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Allow Siri When Locked",
                        checked = allowSiriWhenLocked,
                        onCheckedChange = { allowSiriWhenLocked = it }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Info Dialog: How to get free API Key
    if (showApiKeyInfoDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = Color(0xFF007AFF))
                    Text("Cara Dapat API Key Gratis", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "1. Buka browser dan kunjungi:\naistudio.google.com/apikey",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "2. Login dengan akun Google kamu.",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "3. Klik tombol \"Create API key\", lalu salin kodenya.",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "4. Tempelkan kodenya di kolom ini lalu klik \"Simpan Key\". Siri langsung bisa diajak ngobrol bebas!",
                        fontSize = 14.sp,
                        color = Color(0xFF34C759),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showApiKeyInfoDialog = false }) {
                    Text("Mengerti", fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                }
            }
        )
    }
}

// 18. Emergency SOS SubPage (Clean & Non-overlapping)
@Composable
fun EmergencySosSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    var callWithHold by remember { mutableStateOf(true) }
    var callWith5Presses by remember { mutableStateOf(true) }
    var crashDetection by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Emergency SOS", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Call with Hold and Release",
                        checked = callWithHold,
                        onCheckedChange = { callWithHold = it },
                        icon = Icons.Rounded.Sos,
                        iconBg = Color(0xFFFF3B30)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Call with 5 Button Presses",
                        checked = callWith5Presses,
                        onCheckedChange = { callWith5Presses = it }
                    )
                }
                Text(
                    text = "Rapidly press the side button 5 times or continuously hold the side button and volume button to initiate an emergency call.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 6.dp)
                )
            }

            item {
                Text("CRASH DETECTION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Call After Severe Crash",
                        checked = crashDetection,
                        onCheckedChange = { crashDetection = it },
                        icon = Icons.Rounded.CarCrash,
                        iconBg = Color(0xFFFF9500)
                    )
                }
            }

            item {
                Text("EMERGENCY CONTACTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Set up Emergency Contacts in Health",
                        detail = "",
                        icon = Icons.Rounded.Favorite,
                        iconBg = Color(0xFFFF2D55)
                    )
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Emergency SOS Call (112)",
                        detail = "Call",
                        icon = Icons.Rounded.Phone,
                        iconBg = Color(0xFFFF3B30),
                        onClick = {
                            viewModel.startInAppCall("Emergency SOS", "112")
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 19. Generic Detail SubPage
@Composable
fun GenericSettingsSubPage(
    title: String,
    items: List<Pair<String, String>>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = title, onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    items.forEachIndexed { idx, (itemTitle, itemDetail) ->
                        IOSSettingsRow(title = itemTitle, detail = itemDetail)
                        if (idx < items.size - 1) {
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 20. App Settings SubPage for all APPS in Settings list
@Composable
fun AppSettingsSubPage(
    appName: String,
    viewModel: IOSViewModel,
    onBack: () -> Unit
) {
    val appSettings by viewModel.appSettingsState.collectAsState()

    fun isEnabled(key: String, default: Boolean = true): Boolean {
        return (appSettings["${appName}_$key"] as? Boolean) ?: default
    }

    fun setSetting(key: String, value: Any) {
        viewModel.setAppSetting("${appName}_$key", value)
    }

    fun getString(key: String, default: String): String {
        return (appSettings["${appName}_$key"] as? String) ?: default
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = appName, onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(IOSBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = appName.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = appName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                            Text(text = "Apple Inc. • Version 18.2", fontSize = 13.sp, color = IOSSystemGray)
                        }
                    }
                }
            }

            // Section: ALLOW <APP> TO ACCESS
            item {
                Text(
                    text = "ALLOW $appName TO ACCESS".uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IOSSystemGray,
                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                )
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Notifications",
                        checked = isEnabled("notifications", true),
                        onCheckedChange = { setSetting("notifications", it) },
                        icon = Icons.Rounded.Notifications,
                        iconBg = Color(0xFFFF3B30)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Cellular Data",
                        checked = isEnabled("cellular_data", true),
                        onCheckedChange = { setSetting("cellular_data", it) },
                        icon = Icons.Rounded.SignalCellularAlt,
                        iconBg = Color(0xFF34C759)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Background App Refresh",
                        checked = isEnabled("background_refresh", true),
                        onCheckedChange = { setSetting("background_refresh", it) },
                        icon = Icons.Rounded.Autorenew,
                        iconBg = Color(0xFF007AFF)
                    )
                }
            }

            // App-specific customization sections
            when (appName) {
                "Safari" -> {
                    item {
                        Text(
                            text = "SEARCH".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            val currentEngine = getString("SearchEngine", "Google")
                            IOSSettingsRow(
                                title = "Search Engine",
                                detail = currentEngine,
                                onClick = {
                                    val engines = listOf("Google", "DuckDuckGo", "Bing", "Yahoo")
                                    val next = engines[(engines.indexOf(currentEngine) + 1) % engines.size]
                                    setSetting("SearchEngine", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Search Engine Suggestions",
                                checked = isEnabled("search_suggestions", true),
                                onCheckedChange = { setSetting("search_suggestions", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Quick Website Search",
                                checked = isEnabled("quick_search", true),
                                onCheckedChange = { setSetting("quick_search", it) }
                            )
                        }
                    }

                    item {
                        Text(
                            text = "GENERAL & PRIVACY".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsToggleRow(
                                title = "Block Pop-ups",
                                checked = isEnabled("BlockPopups", true),
                                onCheckedChange = { setSetting("BlockPopups", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Prevent Cross-Site Tracking",
                                checked = isEnabled("prevent_tracking", true),
                                onCheckedChange = { setSetting("prevent_tracking", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Fraudulent Website Warning",
                                checked = isEnabled("fraud_warning", true),
                                onCheckedChange = { setSetting("fraud_warning", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsRow(
                                title = "Clear History and Website Data",
                                detail = "",
                                onClick = {
                                    viewModel.showDynamicIslandNotification("Safari", "History & Data Website Dihapus", "safari", 0xFF007AFF)
                                }
                            )
                        }
                    }
                }

                "Phone" -> {
                    item {
                        Text(
                            text = "CALLS".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsRow(title = "My Number", detail = "+62 812-3456-7890 (Aherdi Soeprapto)")
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            val announceMode = getString("announce_calls", "Always")
                            IOSSettingsRow(
                                title = "Announce Calls",
                                detail = announceMode,
                                onClick = {
                                    val modes = listOf("Always", "Headphones & Car", "Never")
                                    val next = modes[(modes.indexOf(announceMode) + 1) % modes.size]
                                    setSetting("announce_calls", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Silence Unknown Callers",
                                checked = isEnabled("silence_unknown", false),
                                onCheckedChange = { setSetting("silence_unknown", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Call Waiting",
                                checked = isEnabled("call_waiting", true),
                                onCheckedChange = { setSetting("call_waiting", it) }
                            )
                        }
                    }
                }

                "Messages" -> {
                    item {
                        Text(
                            text = "MESSAGES PREFERENCES".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsToggleRow(
                                title = "iMessage",
                                checked = isEnabled("imessage", true),
                                onCheckedChange = { setSetting("imessage", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Send Read Receipts",
                                checked = isEnabled("read_receipts", true),
                                onCheckedChange = { setSetting("read_receipts", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Send as SMS",
                                checked = isEnabled("send_sms", true),
                                onCheckedChange = { setSetting("send_sms", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Filter Unknown Senders",
                                checked = isEnabled("filter_unknown", false),
                                onCheckedChange = { setSetting("filter_unknown", it) }
                            )
                        }
                    }
                }

                "Music" -> {
                    item {
                        Text(
                            text = "AUDIO QUALITY & PLAYBACK".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            val dolbyMode = getString("dolby", "Automatic")
                            IOSSettingsRow(
                                title = "Dolby Atmos",
                                detail = dolbyMode,
                                onClick = {
                                    val modes = listOf("Automatic", "Always On", "Off")
                                    val next = modes[(modes.indexOf(dolbyMode) + 1) % modes.size]
                                    setSetting("dolby", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsRow(title = "Audio Quality", detail = "Lossless (24-bit/48 kHz)")
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            val eqMode = getString("eq", "Acoustic")
                            IOSSettingsRow(
                                title = "EQ",
                                detail = eqMode,
                                onClick = {
                                    val eqs = listOf("Acoustic", "Bass Booster", "Electronic", "Vocal Booster", "Off")
                                    val next = eqs[(eqs.indexOf(eqMode) + 1) % eqs.size]
                                    setSetting("eq", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Sound Check",
                                checked = isEnabled("sound_check", true),
                                onCheckedChange = { setSetting("sound_check", it) }
                            )
                        }
                    }
                }

                "Photos" -> {
                    item {
                        Text(
                            text = "PHOTOS & WIDGET".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsToggleRow(
                                title = "iCloud Photos",
                                checked = isEnabled("icloud_photos", true),
                                onCheckedChange = { setSetting("icloud_photos", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "View Full HDR",
                                checked = isEnabled("full_hdr", true),
                                onCheckedChange = { setSetting("full_hdr", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            val widgetRotation = getString("widget_rotation", "Every 1 Minute")
                            IOSSettingsRow(
                                title = "Widget Slideshow Rotation",
                                detail = widgetRotation,
                                onClick = {
                                    val rotations = listOf("Every 1 Minute", "Every 5 Minutes", "Hourly", "Daily")
                                    val next = rotations[(rotations.indexOf(widgetRotation) + 1) % rotations.size]
                                    setSetting("widget_rotation", next)
                                }
                            )
                        }
                    }
                }

                "Camera" -> {
                    item {
                        Text(
                            text = "CAMERA SETTINGS".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsRow(title = "Formats", detail = "High Efficiency (HEIF/HEVC)")
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            val videoRes = getString("video_res", "4K at 60 fps")
                            IOSSettingsRow(
                                title = "Record Video",
                                detail = videoRes,
                                onClick = {
                                    val res = listOf("1080p at 30 fps", "1080p at 60 fps", "4K at 30 fps", "4K at 60 fps")
                                    val next = res[(res.indexOf(videoRes) + 1) % res.size]
                                    setSetting("video_res", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Grid",
                                checked = isEnabled("grid", true),
                                onCheckedChange = { setSetting("grid", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Mirror Front Camera",
                                checked = isEnabled("mirror_front", true),
                                onCheckedChange = { setSetting("mirror_front", it) }
                            )
                        }
                    }
                }

                "Weather" -> {
                    item {
                        Text(
                            text = "UNITS & NOTIFICATIONS".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            val tempUnit = getString("temp_unit", "Celsius (°C)")
                            IOSSettingsRow(
                                title = "Temperature",
                                detail = tempUnit,
                                onClick = {
                                    val next = if (tempUnit.startsWith("Celsius")) "Fahrenheit (°F)" else "Celsius (°C)"
                                    setSetting("temp_unit", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Severe Weather Notifications",
                                checked = isEnabled("severe_weather", true),
                                onCheckedChange = { setSetting("severe_weather", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Next-Hour Precipitation",
                                checked = isEnabled("precipitation", true),
                                onCheckedChange = { setSetting("precipitation", it) }
                            )
                        }
                    }
                }

                "Maps" -> {
                    item {
                        Text(
                            text = "DIRECTIONS".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            val transport = getString("preferred_transport", "Driving")
                            IOSSettingsRow(
                                title = "Preferred Type of Travel",
                                detail = transport,
                                onClick = {
                                    val modes = listOf("Driving", "Walking", "Transit", "Cycling")
                                    val next = modes[(modes.indexOf(transport) + 1) % modes.size]
                                    setSetting("preferred_transport", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            val dist = getString("distance_units", "Kilometers")
                            IOSSettingsRow(
                                title = "Distances",
                                detail = dist,
                                onClick = {
                                    val next = if (dist == "Kilometers") "Miles" else "Kilometers"
                                    setSetting("distance_units", next)
                                }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Climate Impacts",
                                checked = isEnabled("climate_impacts", true),
                                onCheckedChange = { setSetting("climate_impacts", it) }
                            )
                        }
                    }
                }

                "Mail" -> {
                    item {
                        Text(
                            text = "MESSAGE LIST & COMPOSING".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsRow(title = "Preview", detail = "2 Lines")
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Ask Before Deleting",
                                checked = isEnabled("ask_delete", true),
                                onCheckedChange = { setSetting("ask_delete", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsToggleRow(
                                title = "Always Bcc Myself",
                                checked = isEnabled("bcc_myself", false),
                                onCheckedChange = { setSetting("bcc_myself", it) }
                            )
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsRow(title = "Signature", detail = "Sent from my iPhone")
                        }
                    }
                }

                else -> {
                    item {
                        Text(
                            text = "STORAGE & CACHE".uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IOSSystemGray,
                            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                        )
                        IOSSettingsCard {
                            IOSSettingsRow(title = "Documents & Data", detail = "48.2 MB")
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            IOSSettingsRow(
                                title = "Clear App Cache",
                                detail = "",
                                onClick = {
                                    viewModel.showDynamicIslandNotification(appName, "Cache berhasil dibersihkan", "trash", 0xFF007AFF)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// 21. Date & Time SubPage (Native iOS with direct system pipeline)
@Composable
fun DateTimeSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    var is24Hour by remember { mutableStateOf(true) }
    var autoTime by remember { mutableStateOf(true) }
    var timeZone by remember { mutableStateOf("Jakarta (WIB • GMT+7)") }
    var showTimeZonePicker by remember { mutableStateOf(false) }

    val timeZones = listOf(
        "Jakarta (WIB • GMT+7)",
        "Makassar (WITA • GMT+8)",
        "Jayapura (WIT • GMT+9)",
        "Singapore (SGT • GMT+8)",
        "Tokyo (JST • GMT+9)",
        "London (GMT+0)",
        "New York (EST • GMT-5)",
        "Cupertino (PST • GMT-8)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Date & Time", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "24-Hour Time",
                        checked = is24Hour,
                        onCheckedChange = {
                            is24Hour = it
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.util.RootExecutor.execute("settings put system time_12_24 ${if (it) "24" else "12"}")
                            }
                        }
                    )
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Set Automatically",
                        checked = autoTime,
                        onCheckedChange = {
                            autoTime = it
                            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                com.example.util.RootExecutor.execute("settings put global auto_time ${if (it) 1 else 0}")
                            }
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Time Zone",
                        detail = timeZone,
                        onClick = { showTimeZonePicker = true }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showTimeZonePicker) {
        AlertDialog(
            onDismissRequest = { showTimeZonePicker = false },
            title = { Text("Select Time Zone", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeZones.forEach { tz ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    timeZone = tz
                                    showTimeZonePicker = false
                                    viewModel.showDynamicIslandNotification("Time Zone", tz, "clock")
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tz, fontSize = 15.sp, color = Color.Black)
                            if (timeZone == tz) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeZonePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// 22. Language & Region SubPage
@Composable
fun LanguageSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    var selectedLang by remember { mutableStateOf("Bahasa Indonesia") }
    var selectedRegion by remember { mutableStateOf("Indonesia") }
    var showLangPicker by remember { mutableStateOf(false) }

    val languages = listOf("Bahasa Indonesia", "English (US)", "English (UK)", "日本語 (Japanese)", "Español")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Language & Region", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("PREFERRED LANGUAGES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "iPhone Language",
                        detail = selectedLang,
                        onClick = { showLangPicker = true }
                    )
                }
            }

            item {
                Text("REGION FORMATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Region", detail = selectedRegion)
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Calendar", detail = "Gregorian")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Temperature", detail = "°C")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Measurement System", detail = "Metric")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "First Day of Week", detail = "Monday")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showLangPicker) {
        AlertDialog(
            onDismissRequest = { showLangPicker = false },
            title = { Text("iPhone Language", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLang = lang
                                    showLangPicker = false
                                    viewModel.showDynamicIslandNotification("Language", lang, "globe")
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lang, fontSize = 15.sp, color = Color.Black)
                            if (selectedLang == lang) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLangPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 23. VPN & Device Management SubPage
@Composable
fun VpnSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    var isVpnConnected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "VPN & Device Management", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("VPN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "VPN Status",
                        checked = isVpnConnected,
                        onCheckedChange = {
                            isVpnConnected = it
                            viewModel.showDynamicIslandNotification("VPN", if (it) "Connected" else "Disconnected", "lock")
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Add VPN Configuration...", detail = "", icon = Icons.Rounded.Add, iconBg = Color(0xFF007AFF), onClick = {})
                }
            }
            item {
                Text("DEVICE MANAGEMENT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }
            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Mobile Device Management", detail = "None")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 24. Transfer or Reset iPhone SubPage
@Composable
fun ResetSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Transfer or Reset iPhone", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Prepare for New iPhone", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                    Text("Make sure you're ready to transfer your apps and data.", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Reset All Settings",
                        detail = "",
                        onClick = { showResetDialog = true }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Reset Network Settings",
                        detail = "",
                        onClick = {
                            viewModel.showDynamicIslandNotification("Network Settings", "Network settings reset", "wifi")
                        }
                    )
                }
            }

            item {
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEraseDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Erase All Content and Settings", color = Color(0xFFFF3B30), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Settings?") },
            text = { Text("This will reset all settings including network, keyboard dictionary, home screen layout, and privacy settings. No data or media will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    viewModel.showDynamicIslandNotification("Reset Settings", "Settings Reset Completed", "checkmark.circle")
                }) {
                    Text("Reset All Settings", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEraseDialog) {
        AlertDialog(
            onDismissRequest = { showEraseDialog = false },
            title = { Text("Erase This iPhone?") },
            text = { Text("Erasing will remove all your personal data, apps, and accounts. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showEraseDialog = false
                    viewModel.showDynamicIslandNotification("Erase iPhone", "Erasing device...", "trash", 0xFFFF3B30)
                }) {
                    Text("Continue", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEraseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ================= IPHONE 16/17 & IOS 26 SUBPAGES =================

@Composable
fun ActionButtonSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val currentFunction by viewModel.actionButtonFunction.collectAsState()

    val functions = listOf(
        Triple("Silent Mode", Icons.Rounded.NotificationsOff, Color(0xFFFF3B30)),
        Triple("Flashlight", Icons.Rounded.FlashlightOn, Color(0xFFFF9500)),
        Triple("Camera", Icons.Rounded.CameraAlt, Color(0xFF007AFF)),
        Triple("Voice Memo", Icons.Rounded.Mic, Color(0xFFFF2D55)),
        Triple("Focus", Icons.Rounded.DoNotDisturbOn, Color(0xFF5856D6)),
        Triple("Root Tools", Icons.Rounded.Terminal, Color(0xFF34C759)),
        Triple("None", Icons.Rounded.Block, Color(0xFF8E8E93))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Action Button", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Hero Card: Action Button Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FF9500)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Action Button",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Press and hold the Action button to quickly access your favorite feature.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.triggerActionButton(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500))
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Action Button", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "CHOOSE AN ACTION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    functions.forEachIndexed { idx, (name, icon, color) ->
                        val isSelected = currentFunction == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActionButtonFunction(name, context)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = name,
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                            }
                        }
                        if (idx < functions.size - 1) {
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun CameraControlSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val enabled by viewModel.cameraControlEnabled.collectAsState()
    val sensitivity by viewModel.cameraControlSensitivity.collectAsState()
    var cleanPreview by remember { mutableStateOf(true) }

    val sensitivities = listOf("Light", "Default", "Firm")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Camera Control", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x33007AFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Camera,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "Camera Control (iPhone 16/17)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "A dedicated tactile switch and force sensor gives you instant access to camera tools.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Camera Control",
                        checked = enabled,
                        onCheckedChange = {
                            viewModel.setCameraControlEnabled(it, context)
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Clean Preview",
                        checked = cleanPreview,
                        onCheckedChange = { cleanPreview = it }
                    )
                }
            }

            item {
                Text(
                    text = "LIGHT PRESS PRESSURE SENSITIVITY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    sensitivities.forEachIndexed { idx, sens ->
                        val isSelected = sensitivity == sens
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCameraControlSensitivity(sens, context)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sens, fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                            }
                        }
                        if (idx < sensitivities.size - 1) {
                            HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun StandBySubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val enabled by viewModel.standByEnabled.collectAsState()
    val nightMode by viewModel.standByNightMode.collectAsState()
    val alwaysOn by viewModel.standByAlwaysOn.collectAsState()
    var showNotifications by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "StandBy", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FF3B30)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NightlightRound,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "StandBy (iOS 26)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "StandBy turns on when iPhone is placed on its side while charging, giving you glanceable info.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "StandBy",
                        checked = enabled,
                        onCheckedChange = {
                            viewModel.setStandByEnabled(it, context)
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Always On Display",
                        checked = alwaysOn,
                        onCheckedChange = {
                            viewModel.setStandByAlwaysOn(it)
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Night Mode (Red Tint)",
                        checked = nightMode,
                        onCheckedChange = {
                            viewModel.setStandByNightMode(it)
                        }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Show Notifications",
                        checked = showNotifications,
                        onCheckedChange = { showNotifications = it }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}



