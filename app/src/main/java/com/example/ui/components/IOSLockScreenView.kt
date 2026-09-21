package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.util.IOSFeedbackHelper
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
    val context = LocalContext.current
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
    val lockScreenConfig by viewModel.lockScreenConfig.collectAsState()
    val isCustomizeOpen by viewModel.isLockScreenCustomizeOpen.collectAsState()

    var showPasscodePad by remember { mutableStateOf(false) }
    var enteredPasscode by remember { mutableStateOf("") }
    var isPasscodeError by remember { mutableStateOf(false) }
    var isFaceIdScanning by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    val currentDateFormatted = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(isLocked) {
        if (isLocked) {
            enteredPasscode = ""
            showPasscodePad = false
            IOSFeedbackHelper.playLockSound(context)
            if (isFaceIdEnabled) {
                isFaceIdScanning = true
                delay(850)
                isFaceIdScanning = false
                IOSFeedbackHelper.vibrateSuccess(context)
                IOSFeedbackHelper.playUnlockSound(context)
                viewModel.unlockDevice()
            }
        }
    }

    fun performUnlock() {
        if (isPasscodeEnabled) {
            showPasscodePad = true
            IOSFeedbackHelper.vibrateLight(context)
        } else {
            IOSFeedbackHelper.vibrateSuccess(context)
            IOSFeedbackHelper.playUnlockSound(context)
            viewModel.unlockDevice()
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (!showPasscodePad) {
                                IOSFeedbackHelper.vibrateMedium(context)
                                viewModel.openLockScreenCustomize()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -35f && !showPasscodePad) {
                            performUnlock()
                        }
                    }
                }
        ) {
            // LAYER 1 & 2: iOS 26 Depth Effect Wallpaper Renderer (Background + Clock Behind Foreground Subject)
            IOSDepthWallpaperRenderer(
                config = lockScreenConfig,
                wallpaperRes = currentWallpaperRes,
                wallpaperUri = currentWallpaperUri,
                currentTime = currentTime,
                currentDate = currentDateFormatted,
                batteryLevel = batteryLevel,
                isBatteryCharging = isBatteryCharging,
                modifier = Modifier.fillMaxSize()
            )

            // Dim overlay when entering passcode
            if (showPasscodePad) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                )
            }

            // Foreground Overlays & Controls
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

                Spacer(modifier = Modifier.height(10.dp))

                // Lock / Face ID Status Icon
                Box(
                    modifier = Modifier.size(34.dp),
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
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (!showPasscodePad) {
                    // Lock Screen Idle Mode: Spacer pushes unlock gesture & shortcuts to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Swipe Up / Tap to Unlock Prompt with HomeBar indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { performUnlock() }
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Swipe up or tap to unlock",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(135.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }

                    // Bottom Quick Shortcuts: Flashlight & Camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 46.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val flashlightOn by viewModel.flashlightEnabled.collectAsState()
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (flashlightOn) Color.White else Color(0x66000000))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    IOSFeedbackHelper.vibrateMedium(context)
                                    viewModel.toggleFlashlight(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlashlightOn,
                                contentDescription = "Flashlight",
                                tint = if (flashlightOn) Color.Black else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Customize Wallpaper hint button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .clickable {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    viewModel.openLockScreenCustomize()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Customize",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable {
                                    IOSFeedbackHelper.vibrateMedium(context)
                                    viewModel.unlockDevice()
                                    viewModel.openApp(AppId.CAMERA)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Camera",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    // AUTHENTIC iOS PASSCODE KEYPAD UI (Properly centered & scaled for thumb reach)
                    Spacer(modifier = Modifier.weight(0.9f))

                    Text(
                        text = "Enter Passcode",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 22.dp)
                    )

                    // 4 Passcode Dots with Spring Error Shake
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier
                            .offset(x = shakeOffset.value.dp)
                            .padding(bottom = 34.dp)
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
                                        else Color.White.copy(alpha = 0.35f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isFilled) Color.White else Color.White.copy(alpha = 0.55f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    // Authentic iOS 3x4 Keypad
                    val padKeys = listOf(
                        Triple("1", "", ""),
                        Triple("2", "A B C", ""),
                        Triple("3", "D E F", ""),
                        Triple("4", "G H I", ""),
                        Triple("5", "J K L", ""),
                        Triple("6", "M N O", ""),
                        Triple("7", "P Q R S", ""),
                        Triple("8", "T U V", ""),
                        Triple("9", "W X Y Z", ""),
                        Triple("emergency", "", ""),
                        Triple("0", "", ""),
                        Triple("delete", "", "")
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (row in 0 until 4) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (col in 0 until 3) {
                                    val (key, subtext, _) = padKeys[row * 3 + col]

                                    when (key) {
                                        "emergency" -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clickable {
                                                        IOSFeedbackHelper.vibrateMedium(context)
                                                        viewModel.unlockDevice()
                                                        viewModel.openApp(AppId.PHONE)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Emergency",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                        "delete" -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clickable {
                                                        IOSFeedbackHelper.vibrateLight(context)
                                                        IOSFeedbackHelper.playClickSound(context)
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
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                            }
                                        }
                                        else -> {
                                            // Number Button: 80.dp circle, frosted glass background, large clean digit
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x38FFFFFF))
                                                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                                                    .clickable {
                                                        IOSFeedbackHelper.vibrateLight(context)
                                                        IOSFeedbackHelper.playClickSound(context)
                                                        if (enteredPasscode.length < 4) {
                                                            enteredPasscode += key
                                                            if (enteredPasscode.length == 4) {
                                                                if (enteredPasscode == userPasscode) {
                                                                    IOSFeedbackHelper.vibrateSuccess(context)
                                                                    IOSFeedbackHelper.playUnlockSound(context)
                                                                    viewModel.unlockDevice()
                                                                } else {
                                                                    isPasscodeError = true
                                                                    IOSFeedbackHelper.vibrateError(context)
                                                                    coroutineScope.launch {
                                                                        // Realistic iOS lock shake
                                                                        shakeOffset.animateTo(
                                                                            targetValue = 24f,
                                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh)
                                                                        )
                                                                        shakeOffset.animateTo(
                                                                            targetValue = -24f,
                                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh)
                                                                        )
                                                                        shakeOffset.animateTo(
                                                                            targetValue = 0f,
                                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                                                        )
                                                                        delay(300)
                                                                        isPasscodeError = false
                                                                        enteredPasscode = ""
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = key,
                                                        color = Color.White,
                                                        fontSize = 36.sp,
                                                        fontWeight = FontWeight.Light,
                                                        lineHeight = 36.sp
                                                    )
                                                    if (subtext.isNotEmpty()) {
                                                        Text(
                                                            text = subtext,
                                                            color = Color.White.copy(alpha = 0.85f),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.8.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1.1f))
                }
            }

            // Lock Screen Customizer Sheet Modal
            if (isCustomizeOpen) {
                LockScreenCustomizerSheet(
                    config = lockScreenConfig,
                    onUpdateConfig = { viewModel.updateLockScreenConfig(it) },
                    onClose = { viewModel.closeLockScreenCustomize() }
                )
            }
        }
    }
}
