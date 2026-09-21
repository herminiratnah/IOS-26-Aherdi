package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AppId
import com.example.model.PhotoItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.IOSBlue
import com.example.ui.theme.IOSYellow
import com.example.util.FlashlightHelper
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class IOSFlashMode {
    OFF,
    ON,
    AUTO
}

@Composable
fun CameraApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val photoItems by viewModel.photoItems.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var selectedMode by remember { mutableStateOf("PHOTO") }
    var selectedZoom by remember { mutableStateOf("1x") }
    var flashMode by remember { mutableStateOf(IOSFlashMode.OFF) }
    var isNightModeOn by remember { mutableStateOf(false) }
    var isLivePhotoOn by remember { mutableStateOf(true) }
    var isControlsDrawerOpen by remember { mutableStateOf(false) }
    var timerSetting by remember { mutableStateOf(0) } // 0 = off, 3 = 3s, 10 = 10s
    var selectedStyle by remember { mutableStateOf("Standard") }
    var exposureCompensation by remember { mutableStateOf(0) } // -2 to +2

    var countdownSeconds by remember { mutableStateOf(0) }
    var isCapturingNightMode by remember { mutableStateOf(false) }
    var showFlashEffect by remember { mutableStateOf(false) }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCameraBound by remember { mutableStateOf(false) }
    var previewPhotoModal by remember { mutableStateOf<PhotoItem?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // Handled
            }
        }
    }

    LaunchedEffect(previewViewInstance, cameraSelector, hasCameraPermission) {
        val pv = previewViewInstance ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(
                    when (flashMode) {
                        IOSFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                        IOSFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                        IOSFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                    }
                )
                .build()
            imageCapture = capture

            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
            cameraControl = camera.cameraControl
            isCameraBound = true
        } catch (e: Exception) {
            isCameraBound = false
        }
    }

    fun executeCapture() {
        coroutineScope.launch {
            if (isNightModeOn) {
                isCapturingNightMode = true
                IOSFeedbackHelper.vibrateMedium(context)
                delay(2000)
                isCapturingNightMode = false
            }
            showFlashEffect = true
            IOSFeedbackHelper.playLockSound(context)
            IOSFeedbackHelper.vibrateSuccess(context)
            takePhotoSafely(
                context = context,
                imageCapture = imageCapture,
                isCameraBound = isCameraBound,
                flashMode = flashMode,
                isLivePhoto = isLivePhotoOn,
                photoFilter = selectedStyle,
                viewModel = viewModel
            )
        }
    }

    fun triggerShutter() {
        if (countdownSeconds > 0 || isCapturingNightMode) return
        if (timerSetting > 0) {
            coroutineScope.launch {
                for (sec in timerSetting downTo 1) {
                    countdownSeconds = sec
                    IOSFeedbackHelper.playClickSound(context)
                    IOSFeedbackHelper.vibrateLight(context)
                    delay(1000)
                }
                countdownSeconds = 0
                executeCapture()
            }
        } else {
            executeCapture()
        }
    }

    // Latest photo thumbnail
    val latestPhoto = photoItems.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            // Status bar
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = false
            )

            // Top Camera Controls Bar matching authentic iOS Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Flash Toggle Button (Off / On / Auto)
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            IOSFlashMode.OFF -> IOSFlashMode.ON
                            IOSFlashMode.ON -> IOSFlashMode.AUTO
                            IOSFlashMode.AUTO -> IOSFlashMode.OFF
                        }
                        imageCapture?.flashMode = when (flashMode) {
                            IOSFlashMode.ON -> ImageCapture.FLASH_MODE_ON
                            IOSFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                            IOSFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                        }
                        IOSFeedbackHelper.vibrateLight(context)
                    }
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            IOSFlashMode.OFF -> Icons.Rounded.FlashOff
                            IOSFlashMode.ON -> Icons.Rounded.FlashOn
                            IOSFlashMode.AUTO -> Icons.Rounded.FlashAuto
                        },
                        contentDescription = "Flash",
                        tint = if (flashMode != IOSFlashMode.OFF) IOSYellow else Color.White
                    )
                }

                // 2. Night Mode Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isNightModeOn) Color(0x33FFCC00) else Color.Transparent)
                        .clickable {
                            isNightModeOn = !isNightModeOn
                            IOSFeedbackHelper.vibrateLight(context)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Nightlight,
                        contentDescription = "Night Mode",
                        tint = if (isNightModeOn) IOSYellow else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    if (isNightModeOn) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "3s",
                            color = IOSYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Quick Controls Drawer Arrow
                IconButton(
                    onClick = {
                        isControlsDrawerOpen = !isControlsDrawerOpen
                        IOSFeedbackHelper.vibrateLight(context)
                    }
                ) {
                    Icon(
                        imageVector = if (isControlsDrawerOpen) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "More Settings",
                        tint = if (isControlsDrawerOpen) IOSYellow else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 4. Live Photo Toggle
                IconButton(
                    onClick = {
                        isLivePhotoOn = !isLivePhotoOn
                        IOSFeedbackHelper.vibrateLight(context)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MotionPhotosOn,
                        contentDescription = "Live Photo",
                        tint = if (isLivePhotoOn) IOSYellow else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Viewfinder View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF151517)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }.also {
                                previewViewInstance = it
                            }
                        }
                    )
                }

                // If camera hardware preview is not active, scenic viewfinder fallback
                if (!hasCameraPermission || !isCameraBound) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.photo_widget_nature),
                            contentDescription = "Camera Viewfinder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x22000000))
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.Center)
                                .border(1.5.dp, IOSYellow, RoundedCornerShape(4.dp))
                        )
                        if (!hasCameraPermission) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xCC000000))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Akses Kamera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Izinkan akses kamera untuk mengambil foto", color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IOSBlue)
                                ) {
                                    Text("Aktifkan Kamera", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Live Photo Badge indicator
                if (isLivePhotoOn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x88000000))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = IOSYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Timer Countdown Overlay
                if (countdownSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$countdownSeconds",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Night Mode Long Exposure progress indicator
                if (isCapturingNightMode) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xCC000000))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = IOSYellow,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "HOLD STILL",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Shutter flash effect
                if (showFlashEffect) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }

                // Zoom levels pill (.5, 1x, 2, 5)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x88000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(".5", "1x", "2", "5").forEach { zoom ->
                        val isSelected = selectedZoom == zoom
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0x66FFFFFF) else Color.Transparent)
                                .clickable {
                                    selectedZoom = zoom
                                    val linear = when (zoom) {
                                        ".5" -> 0.0f
                                        "1x" -> 0.1f
                                        "2" -> 0.4f
                                        "5" -> 0.8f
                                        else -> 0.1f
                                    }
                                    try {
                                        cameraControl?.setLinearZoom(linear)
                                    } catch (e: Exception) {
                                        // Handled
                                    }
                                    IOSFeedbackHelper.vibrateLight(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = zoom,
                                color = if (isSelected) IOSYellow else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Controls Drawer when chevron is tapped (iOS 18/26 drawer)
            AnimatedVisibility(
                visible = isControlsDrawerOpen,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF222224))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timer toggle
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (timerSetting > 0) Color(0x40FFCC00) else Color(0x20FFFFFF))
                            .clickable {
                                timerSetting = when (timerSetting) {
                                    0 -> 3
                                    3 -> 10
                                    else -> 0
                                }
                                IOSFeedbackHelper.vibrateLight(context)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = "Timer",
                            tint = if (timerSetting > 0) IOSYellow else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (timerSetting == 0) "Timer Off" else "${timerSetting}s",
                            color = if (timerSetting > 0) IOSYellow else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Photographic Style selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable {
                                selectedStyle = when (selectedStyle) {
                                    "Standard" -> "Vibrant"
                                    "Vibrant" -> "Warm"
                                    "Warm" -> "Cool"
                                    "Cool" -> "Dramatic B&W"
                                    else -> "Standard"
                                }
                                IOSFeedbackHelper.vibrateLight(context)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Style",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedStyle,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Exposure Step
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (exposureCompensation != 0) Color(0x40FFCC00) else Color(0x20FFFFFF))
                            .clickable {
                                exposureCompensation = when (exposureCompensation) {
                                    0 -> 1
                                    1 -> 2
                                    2 -> -1
                                    -1 -> -2
                                    else -> 0
                                }
                                try {
                                    cameraControl?.setExposureCompensationIndex(exposureCompensation)
                                } catch (e: Exception) {}
                                IOSFeedbackHelper.vibrateLight(context)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Exposure,
                            contentDescription = "Exposure",
                            tint = if (exposureCompensation != 0) IOSYellow else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (exposureCompensation > 0) "+$exposureCompensation EV" else "$exposureCompensation EV",
                            color = if (exposureCompensation != 0) IOSYellow else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Mode Selector (SLO-MO, VIDEO, PHOTO, PORTRAIT, PANO)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("SLO-MO", "VIDEO", "PHOTO", "PORTRAIT", "PANO").forEach { mode ->
                    val isSelected = selectedMode == mode
                    Text(
                        text = mode,
                        color = if (isSelected) IOSYellow else Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable {
                            selectedMode = mode
                            IOSFeedbackHelper.vibrateLight(context)
                        }
                    )
                }
            }

            // Bottom Shutter Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Photo Gallery Thumbnail
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .clickable {
                            if (latestPhoto != null) {
                                previewPhotoModal = latestPhoto
                            } else {
                                viewModel.openApp(AppId.PHOTOS)
                            }
                        }
                ) {
                    if (latestPhoto != null) {
                        if (latestPhoto.imageUri != null) {
                            AsyncImage(
                                model = latestPhoto.imageUri,
                                contentDescription = "Gallery Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = latestPhoto.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                                contentDescription = "Gallery Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.photo_widget_nature),
                            contentDescription = "Gallery",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Center: Circular White Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (selectedMode == "VIDEO") Color(0xFFFF3B30) else Color.White)
                        .clickable {
                            triggerShutter()
                        }
                )

                // Right: Camera Switcher (Flip Front/Back)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                        .clickable {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            IOSFeedbackHelper.vibrateLight(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }

        // In-Camera Quick Fullscreen Photo Viewer Modal
        previewPhotoModal?.let { photo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { previewPhotoModal = null }
            ) {
                if (photo.imageUri != null) {
                    AsyncImage(
                        model = photo.imageUri,
                        contentDescription = photo.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = photo.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                        contentDescription = photo.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Top Bar in Quick Viewer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { previewPhotoModal = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(photo.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    TextButton(onClick = {
                        previewPhotoModal = null
                        viewModel.openApp(AppId.PHOTOS)
                    }) {
                        Text("All Photos", color = IOSBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    LaunchedEffect(showFlashEffect) {
        if (showFlashEffect) {
            delay(120)
            showFlashEffect = false
        }
    }
}

/**
 * Executes a reliable photo capture: tries real CameraX hardware capture first,
 * or writes an artistic high-res snapshot file into storage if in emulator/headless.
 */
private fun takePhotoSafely(
    context: Context,
    imageCapture: ImageCapture?,
    isCameraBound: Boolean,
    flashMode: IOSFlashMode,
    isLivePhoto: Boolean,
    photoFilter: String,
    viewModel: IOSViewModel
) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
    val photoFile = File(photoDir, "IMG_$timestamp.jpg")

    val needPhysicalFlash = (flashMode == IOSFlashMode.ON)
    if (needPhysicalFlash) {
        FlashlightHelper.setTorch(context, true)
    }

    if (imageCapture != null && isCameraBound) {
        imageCapture.flashMode = when (flashMode) {
            IOSFlashMode.ON -> ImageCapture.FLASH_MODE_ON
            IOSFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            IOSFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (needPhysicalFlash) {
                        FlashlightHelper.setTorch(context, false)
                    }
                    viewModel.addCapturedPhoto(photoFile.absolutePath)
                    if (isLivePhoto) {
                        viewModel.showDynamicIslandNotification("Camera", "Live Photo Disimpan", "camera", 0xFF34C759)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (needPhysicalFlash) {
                        FlashlightHelper.setTorch(context, false)
                    }
                    generateAndSaveSnapshot(context, photoFile, timestamp, photoFilter, viewModel)
                }
            }
        )
    } else {
        if (needPhysicalFlash) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(300)
                FlashlightHelper.setTorch(context, false)
            }
        }
        generateAndSaveSnapshot(context, photoFile, timestamp, photoFilter, viewModel)
    }
}

private fun generateAndSaveSnapshot(
    context: Context,
    targetFile: File,
    timestamp: String,
    photoFilter: String,
    viewModel: IOSViewModel
) {
    try {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw camera frame with subtle gradient based on filter
        val paint = Paint().apply { isAntiAlias = true }
        val colors = when (photoFilter) {
            "Vibrant" -> intArrayOf(0xFF0284C7.toInt(), 0xFF0D9488.toInt(), 0xFFF59E0B.toInt())
            "Warm" -> intArrayOf(0xFF78350F.toInt(), 0xFFD97706.toInt(), 0xFFFDE68A.toInt())
            "Cool" -> intArrayOf(0xFF1E3A8A.toInt(), 0xFF3B82F6.toInt(), 0xFF93C5FD.toInt())
            "Dramatic B&W" -> intArrayOf(0xFF000000.toInt(), 0xFF333333.toInt(), 0xFF888888.toInt())
            else -> intArrayOf(0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF0284C7.toInt())
        }
        val shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            colors, null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw viewfinder details & watermark
        paint.shader = null
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 46f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("iPhone Camera • $photoFilter", width / 2f, height / 2f - 40f, paint)

        paint.color = 0xFF94A3B8.toInt()
        paint.textSize = 30f
        canvas.drawText("Captured on $timestamp", width / 2f, height / 2f + 40f, paint)

        // Write to JPEG
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.flush()
        }
        viewModel.addCapturedPhoto(targetFile.absolutePath)
    } catch (e: Exception) {
        viewModel.addCapturedPhoto(targetFile.absolutePath)
    }
}
