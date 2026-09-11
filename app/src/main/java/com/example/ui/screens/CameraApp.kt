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
import androidx.compose.ui.graphics.Brush
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
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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
    var isFlashOn by remember { mutableStateOf(false) }
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

            // Top Camera Controls Bar matching iOS Camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isFlashOn = !isFlashOn }) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isFlashOn) IOSYellow else Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.Nightlight,
                    contentDescription = "Night Mode",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "More Settings",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.MotionPhotosOn,
                    contentDescription = "Live Photo",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
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

                // If camera hardware preview is not active (emulator or hardware error), show active scenic camera preview
                if (!hasCameraPermission || !isCameraBound) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.photo_widget_nature),
                            contentDescription = "Camera Viewfinder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Live Viewfinder grid & HUD
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x22000000))
                        )
                        // Focus square
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
                                Text("Camera Access", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap to permit Camera to shoot live photos", color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IOSBlue)
                                ) {
                                    Text("Enable Camera", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
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
                        modifier = Modifier.clickable { selectedMode = mode }
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
                // Left: Photo Gallery Thumbnail (shows the real latest photo snapped!)
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

                // Center: Big Circular White Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            showFlashEffect = true
                            takePhotoSafely(
                                context = context,
                                imageCapture = imageCapture,
                                isCameraBound = isCameraBound,
                                viewModel = viewModel
                            )
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
    viewModel: IOSViewModel
) {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val photoDir = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
    val photoFile = File(photoDir, "IMG_$timestamp.jpg")

    if (imageCapture != null && isCameraBound) {
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    viewModel.addCapturedPhoto(photoFile.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    // Fallback to generating a real JPEG bitmap file
                    generateAndSaveSnapshot(context, photoFile, timestamp, viewModel)
                }
            }
        )
    } else {
        // Fallback capture when camera hardware not active
        generateAndSaveSnapshot(context, photoFile, timestamp, viewModel)
    }
}

private fun generateAndSaveSnapshot(
    context: Context,
    targetFile: File,
    timestamp: String,
    viewModel: IOSViewModel
) {
    try {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw camera frame with subtle gradient
        val paint = Paint().apply { isAntiAlias = true }
        val colors = intArrayOf(0xFF1E293B.toInt(), 0xFF0F172A.toInt(), 0xFF0284C7.toInt())
        val shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            colors, null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Draw viewfinder details & watermark
        paint.shader = null
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 48f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("iPhone SE 2 Camera", width / 2f, height / 2f - 40f, paint)

        paint.color = 0xFF94A3B8.toInt()
        paint.textSize = 32f
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
