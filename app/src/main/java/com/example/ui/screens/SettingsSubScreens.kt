package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.IOSOverlayService
import com.example.ui.theme.*
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
    APP_SETTINGS
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
                        IconButton(onClick = { viewModel.scanWifiNetworks(context) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Scan", tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                item {
                    IOSSettingsCard {
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
    val cellularEnabled by viewModel.cellularEnabled.collectAsState()
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
                        IOSSettingsRow(title = "Telkomsel 5G (eSIM)", detail = "Primary • 0812-8888-9999", icon = Icons.Rounded.SimCard, iconBg = Color(0xFF007AFF))
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsRow(title = "Wi-Fi Calling", detail = "On")
                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                        IOSSettingsRow(title = "Carrier Services", detail = "Active")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// 5. General & About SubPage
@Composable
fun AboutSubPage(onBack: () -> Unit) {
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
                    IOSSettingsRow(title = "Name", detail = "iPhone 17 Pro Max")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "iOS Version", detail = "18.5 (Build 22F82)")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Model Name", detail = "iPhone 17 Pro Max")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Model Number", detail = "A3102")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Serial Number", detail = "F2LZ982K0D")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Limited Warranty", detail = "Expires Oct 2027")
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
                    IOSSettingsRow(title = "Applications", detail = "42")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Capacity", detail = "256 GB")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Available", detail = "184.2 GB")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Wi-Fi Address", detail = "A4:C3:61:9D:E4:2A")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Bluetooth", detail = "A4:C3:61:9D:E4:2B")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Modem Firmware", detail = "3.04.01")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "SEID", detail = "0400827391823901")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 6. Sounds & Haptics SubPage (with REAL volume control and ringtone test!)
@Composable
fun SoundsSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var volumeSlider by remember {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
        val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        mutableFloatStateOf(current.toFloat() / max.toFloat())
    }

    var selectedRingtone by remember { mutableStateOf("Reflection (Default)") }
    var hapticsEnabled by remember { mutableStateOf(true) }
    var lockSoundEnabled by remember { mutableStateOf(true) }
    var keyboardClicks by remember { mutableStateOf(true) }

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
                                value = volumeSlider,
                                onValueChange = { newVal ->
                                    volumeSlider = newVal
                                    if (audioManager != null) {
                                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val target = (newVal * max).toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                        viewModel.showDynamicIslandVolume((newVal * 100).toInt())
                                    }
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
                }
            }

            item {
                Text("SOUNDS AND HAPTIC PATTERNS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    ringtones.forEachIndexed { idx, ringtone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRingtone = ringtone
                                    try {
                                        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                                        val r = RingtoneManager.getRingtone(context, defaultUri)
                                        r.play()
                                    } catch (ignored: Exception) {}
                                    viewModel.showDynamicIslandNotification("Ringtone Preview", ringtone, "music")
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ringtone, fontSize = 16.sp, color = Color.Black)
                            if (selectedRingtone == ringtone) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
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
                        onCheckedChange = { keyboardClicks = it }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Lock Sound",
                        checked = lockSoundEnabled,
                        onCheckedChange = { lockSoundEnabled = it }
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "System Haptics",
                        checked = hapticsEnabled,
                        onCheckedChange = { hapticsEnabled = it }
                    )
                }
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
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6))
                ) {
                    Text("Beri Izin 'Tampil di Atas Aplikasi Lain'", fontWeight = FontWeight.Bold, color = Color.White)
                }
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
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Aktifkan iOS Keyboard di Sistem Android", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            item {
                Button(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                ) {
                    Text("Pilih iOS Keyboard Sebagai Default", fontWeight = FontWeight.Bold, color = Color.White)
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
                Button(
                    onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                        } catch (ignored: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                ) {
                    Text("Kelola Default Apps di Sistem Android", fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 10. Battery SubPage (Real Battery Level & Low Power Mode)
@Composable
fun BatterySubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
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
                    IOSSettingsRow(title = "Battery Health & Charging", detail = "Maximum Capacity 98% (Normal)", icon = Icons.Rounded.HealthAndSafety, iconBg = Color(0xFF34C759))
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
    onNavigate: (SettingsSubPage) -> Unit,
    onBack: () -> Unit
) {
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
                    IOSSettingsRow(title = "Software Update", detail = "iOS 18.5", onClick = { onNavigate(SettingsSubPage.SOFTWARE_UPDATE) })
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
                    IOSSettingsRow(title = "iPhone Storage", detail = "184 GB Available", onClick = { onNavigate(SettingsSubPage.STORAGE) })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Background App Refresh", detail = "On")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Date & Time", detail = "24-Hour Time (Auto)")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Keyboard", detail = "2 Keyboards", onClick = { onNavigate(SettingsSubPage.KEYBOARD) })
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Language & Region", detail = "English (US)")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Dictionary", detail = "")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "VPN & Device Management", detail = "Not Connected")
                }
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(title = "Transfer or Reset iPhone", detail = "")
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(title = "Shut Down", detail = "", onClick = {})
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 12. Display & Brightness SubPage
@Composable
fun DisplaySubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var trueTone by remember { mutableStateOf(true) }
    var nightShift by remember { mutableStateOf(false) }
    var autoLock by remember { mutableStateOf("30 Seconds") }
    var alwaysOn by remember { mutableStateOf(true) }
    var brightness by remember { mutableFloatStateOf(0.75f) }

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
                                onValueChange = { brightness = it },
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
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// 13. Personal Hotspot SubPage
@Composable
fun PersonalHotspotSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
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

// 14. iOS 17 Style Wallpaper SubPage
data class WallpaperChoice(
    val id: String,
    val name: String,
    val category: String,
    val resId: Int
)

@Composable
fun WallpaperSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val currentWallpaperRes by viewModel.currentWallpaperRes.collectAsState()
    val currentWallpaperName by viewModel.currentWallpaperName.collectAsState()
    val currentWallpaperUri by viewModel.currentWallpaperUri.collectAsState()

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
        }
    }

    val wallpapers = remember {
        listOf(
            WallpaperChoice("ios26", "iOS 26 Ambient", "Featured", R.drawable.ios26_wallpaper),
            WallpaperChoice("titanium", "iOS 18 Pro Titanium", "Flagship", R.drawable.wallpaper_titanium),
            WallpaperChoice("twilight", "Twilight Gradient", "Fluid Abstract", R.drawable.wallpaper_twilight),
            WallpaperChoice("astronomy", "Astronomy & Earth", "Astronomy", R.drawable.wallpaper_astronomy),
            WallpaperChoice("aurora", "Aurora Fjord", "Nature", R.drawable.wallpaper_aurora),
            WallpaperChoice("alpine", "Alpine Vista", "Nature", R.drawable.wallpaper_alpine),
            WallpaperChoice("neon", "Neon Fluid Wave", "Abstract", R.drawable.wallpaper_neon_waves),
            WallpaperChoice("valley", "Monument Sunset", "Collections", R.drawable.monument_valley_art),
            WallpaperChoice("nature", "Emerald Forest", "Nature", R.drawable.photo_widget_nature)
        )
    }

    var selectedWallpaper by remember { mutableStateOf(wallpapers.firstOrNull { it.resId == currentWallpaperRes } ?: wallpapers[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Wallpaper", onBack = onBack)

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
                    // Lock Screen Mini Mockup
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                        ) {
                            if (!currentWallpaperUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentWallpaperUri,
                                    contentDescription = "Lock Screen Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = if (currentWallpaperRes != 0) currentWallpaperRes else R.drawable.ios26_wallpaper),
                                    contentDescription = "Lock Screen Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Clock overlay
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Tuesday, Sep 8", color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
                                Text("09:41", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                            // Bottom quick buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FlashlightOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Lock Screen", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Home Screen Mini Mockup
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black)
                        ) {
                            if (!currentWallpaperUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentWallpaperUri,
                                    contentDescription = "Home Screen Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = if (currentWallpaperRes != 0) currentWallpaperRes else R.drawable.ios26_wallpaper),
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
                        Text("Home Screen", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Choose from Photos Action Card
            item {
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoPickerLauncher.launch(
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
                                .background(Color(0xFF5856D6)),
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
                                text = "Choose from Photos",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Text(
                                text = "Pick any image from your gallery",
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
                Text("WALLPAPER GALLERY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            // Wallpapers Collection Cards
            items(wallpapers) { wall ->
                val isCurrent = currentWallpaperUri.isNullOrBlank() && wall.resId == currentWallpaperRes
                IOSSettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedWallpaper = wall
                                viewModel.setWallpaper(wall.resId, wall.name, null, context)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = wall.resId),
                            contentDescription = wall.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 54.dp, height = 80.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wall.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = wall.category,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF34C759).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Active Wallpaper", color = Color(0xFF34C759), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    selectedWallpaper = wall
                                    viewModel.setWallpaper(wall.resId, wall.name, null, context)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Set", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

// 15. Accessibility SubPage (with Integrasi Sistem Android)
@Composable
fun AccessibilitySubPage(
    viewModel: IOSViewModel,
    onNavigate: (SettingsSubPage) -> Unit,
    onBack: () -> Unit
) {
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
                        title = "iPhone Unlock with Face ID",
                        checked = isFaceIdEnabled,
                        onCheckedChange = { enabled -> viewModel.toggleFaceId(enabled) },
                        icon = Icons.Rounded.Face,
                        iconBg = Color(0xFF34C759)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
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

// 17. Siri & Apple Intelligence SubPage (Clean & Non-overlapping)
@Composable
fun SiriSubPage(viewModel: IOSViewModel, onBack: () -> Unit) {
    var listenForHeySiri by remember { mutableStateOf(true) }
    var pressSideButton by remember { mutableStateOf(true) }
    var allowSiriWhenLocked by remember { mutableStateOf(true) }
    var writingTools by remember { mutableStateOf(true) }

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
                Text("APPLE INTELLIGENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Writing Tools",
                        checked = writingTools,
                        onCheckedChange = { writingTools = it },
                        icon = Icons.Rounded.Edit,
                        iconBg = Color(0xFF5856D6)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Siri Voice",
                        detail = "American (Voice 4)"
                    )
                }
            }

            item {
                Text("ASK SIRI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
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
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
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


