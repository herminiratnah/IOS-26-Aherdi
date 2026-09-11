package com.example.ui.components

import com.example.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IOSLockScreenView(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val isLocked by viewModel.isDeviceLocked.collectAsState()
    val isPasscodeEnabled by viewModel.isPasscodeEnabled.collectAsState()
    val isFaceIdEnabled by viewModel.isFaceIdEnabled.collectAsState()
    val userPasscode by viewModel.userPasscode.collectAsState()
    val currentWallpaperRes by viewModel.currentWallpaperRes.collectAsState()
    val currentWallpaperUri by viewModel.currentWallpaperUri.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var showPasscodePad by remember { mutableStateOf(false) }
    var enteredPasscode by remember { mutableStateOf("") }
    var isPasscodeError by remember { mutableStateOf(false) }
    var isFaceIdScanning by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val currentDateFormatted = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(isLocked) {
        if (isLocked) {
            enteredPasscode = ""
            showPasscodePad = false
            if (isFaceIdEnabled) {
                isFaceIdScanning = true
                delay(1000)
                isFaceIdScanning = false
                // If passcode is not required or user enabled Face ID, unlock smoothly
                viewModel.unlockDevice()
            }
        }
    }

    AnimatedVisibility(
        visible = isLocked,
        enter = fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Wallpaper
            if (!currentWallpaperUri.isNullOrBlank()) {
                AsyncImage(
                    model = currentWallpaperUri,
                    contentDescription = "Lockscreen Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = if (currentWallpaperRes != 0) currentWallpaperRes else R.drawable.ios26_wallpaper),
                    contentDescription = "Lockscreen Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dim overlay when passcode keypad is active
            if (showPasscodePad) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top iOS Status Bar
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
                    isDarkIcons = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lock icon / Face ID icon
                Box(
                    modifier = Modifier
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFaceIdScanning) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (showPasscodePad) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Lock Status",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (!showPasscodePad) {
                    // Date & Time
                    Text(
                        text = currentDateFormatted,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Text(
                        text = currentTime,
                        color = Color.White,
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Swipe Up / Tap to Unlock Prompt
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (isPasscodeEnabled) {
                                    showPasscodePad = true
                                } else {
                                    viewModel.unlockDevice()
                                }
                            }
                            .padding(bottom = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Swipe up or tap to unlock",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Bottom Quick Shortcuts: Flashlight & Camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val flashlightOn by viewModel.flashlightEnabled.collectAsState()
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (flashlightOn) Color.White else Color(0x55000000))
                                .clickable { viewModel.toggleFlashlight(context) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlashlightOn,
                                contentDescription = "Flashlight",
                                tint = if (flashlightOn) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0x55000000))
                                .clickable {
                                    viewModel.unlockDevice()
                                    viewModel.openApp(AppId.CAMERA)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    // Passcode Pad View
                    Text(
                        text = "Enter Passcode",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 20.dp, bottom = 24.dp)
                    )

                    // 4 Passcode Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPasscode.length
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPasscodeError) Color(0xFFFF3B30)
                                        else if (isFilled) Color.White
                                        else Color.White.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    // Keypad 3x4
                    val padKeys = listOf(
                        Pair("1", ""), Pair("2", "A B C"), Pair("3", "D E F"),
                        Pair("4", "G H I"), Pair("5", "J K L"), Pair("6", "M N O"),
                        Pair("7", "P Q R S"), Pair("8", "T U V"), Pair("9", "W X Y Z"),
                        Pair("", ""), Pair("0", ""), Pair("delete", "")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (row in 0 until 4) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (col in 0 until 3) {
                                    val key = padKeys[row * 3 + col]
                                    if (key.first == "delete") {
                                        Box(
                                            modifier = Modifier
                                                .size(75.dp)
                                                .clickable {
                                                    if (enteredPasscode.isNotEmpty()) {
                                                        enteredPasscode = enteredPasscode.dropLast(1)
                                                    } else {
                                                        showPasscodePad = false
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (enteredPasscode.isEmpty()) "Cancel" else "Delete",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    } else if (key.first.isEmpty()) {
                                        Box(modifier = Modifier.size(75.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(75.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x33FFFFFF))
                                                .clickable {
                                                    if (enteredPasscode.length < 4) {
                                                        enteredPasscode += key.first
                                                        if (enteredPasscode.length == 4) {
                                                            if (enteredPasscode == userPasscode) {
                                                                viewModel.unlockDevice()
                                                            } else {
                                                                isPasscodeError = true
                                                                coroutineScope.launch {
                                                                    delay(600)
                                                                    isPasscodeError = false
                                                                    enteredPasscode = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = key.first,
                                                    color = Color.White,
                                                    fontSize = 32.sp,
                                                    fontWeight = FontWeight.Light
                                                )
                                                if (key.second.isNotEmpty()) {
                                                    Text(
                                                        text = key.second,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Emergency",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .clickable {
                                viewModel.unlockDevice()
                                viewModel.openApp(AppId.PHONE)
                            }
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
