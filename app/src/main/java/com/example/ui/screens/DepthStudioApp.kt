package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.AppId
import com.example.model.PhotoItem
import com.example.ui.components.HomeBar
import com.example.ui.theme.IOSBlue
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Creative module for AI Depth Effect Wallpaper Studio:
 * - Real 3-layer depth engine: Background -> Big Clock -> Transparent Cutout Subject
 * - Real in-browser image segmentation & edge detection
 * - Direct interactive touch/mouse drag for clock position
 * - Typography, color, format, and weight customization
 * - Photo zoom & pan without losing layer alignment
 * - Manual mask brush (reveal/erase subject) with Undo/Redo
 * - High-resolution wallpaper export (PNG/JPG, with clock, clean photo, transparent cutout)
 * - Native Android bridge: Saves exported wallpaper directly to Android Media Gallery / Photos App,
 *   provides 1-click "Set as Lockscreen Wallpaper", and shows export success dialog with step-by-step tutorial!
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DepthStudioApp(
    viewModel: IOSViewModel,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var lastSavedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var dialogSavedFilename by remember { mutableStateOf("") }

    // File picker launcher for HTML5 <input type="file"> inside WebView
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fileUploadCallback?.onReceiveValue(arrayOf(uri))
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // Helper functions for saving and applying wallpaper
    fun saveBase64Image(base64Data: String, filename: String, applyAsWallpaperDirectly: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val cleanBase64 = if (base64Data.contains(",")) {
                    base64Data.substringAfter(",")
                } else {
                    base64Data
                }
                val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal memproses gambar: Data tidak valid", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val outName = if (filename.isNotBlank()) filename else "DepthWallpaper_$timeStamp.jpg"
                val isPng = outName.endsWith(".png", ignoreCase = true)
                val mimeType = if (isPng) "image/png" else "image/jpeg"
                val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

                // 1. Always save a copy to internal app files to guarantee reliable access
                val internalDir = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }
                val internalFile = File(internalDir, outName)
                FileOutputStream(internalFile).use { out ->
                    bitmap.compress(compressFormat, 95, out)
                }
                var finalUri: Uri = Uri.fromFile(internalFile)

                // 2. Also save to Android MediaStore Gallery for device-wide access
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, outName)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DepthWallpapers")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        if (uri != null) {
                            resolver.openOutputStream(uri)?.use { stream ->
                                bitmap.compress(compressFormat, 95, stream)
                            }
                            contentValues.clear()
                            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            resolver.update(uri, contentValues, null, null)
                            finalUri = uri
                        }
                    } else {
                        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DepthWallpapers")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, outName)
                        FileOutputStream(file).use { out ->
                            bitmap.compress(compressFormat, 95, out)
                        }
                        finalUri = Uri.fromFile(file)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DepthStudio", "MediaStore insert warning: ${e.message}")
                }

                // 3. Register into iOS Photos App feed in ViewModel
                viewModel.addCapturedPhoto(finalUri.toString())

                // 4. If requested, directly apply as Lockscreen Wallpaper
                if (applyAsWallpaperDirectly) {
                    viewModel.setWallpaper(0, "AI Depth Wallpaper", finalUri.toString(), context)
                }

                withContext(Dispatchers.Main) {
                    lastSavedImageUri = finalUri
                    dialogSavedFilename = outName
                    showSuccessDialog = true

                    val toastMsg = if (applyAsWallpaperDirectly) {
                        "✅ Berhasil diekspor & dipasang sebagai Wallpaper Lockscreen!"
                    } else {
                        "✅ Berhasil disimpan ke Galeri Foto ($outName)!"
                    }
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal mengekspor: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveDepthWallpaper(bgBase64: String, filename: String, fgBase64: String?, clockConfigJson: String? = null) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val outBgName = if (filename.isNotBlank()) filename else "DepthWallpaper_$timeStamp.jpg"

                val cleanBg = if (bgBase64.contains(",")) bgBase64.substringAfter(",") else bgBase64
                val bgBytes = Base64.decode(cleanBg, Base64.DEFAULT)
                val bgBitmap = BitmapFactory.decodeByteArray(bgBytes, 0, bgBytes.size)
                if (bgBitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal memproses gambar latar", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val internalDir = File(context.filesDir, "wallpapers").apply { if (!exists()) mkdirs() }
                val internalBgFile = File(internalDir, outBgName)
                FileOutputStream(internalBgFile).use { out ->
                    bgBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                var bgUri: Uri = Uri.fromFile(internalBgFile)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val cv = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, outBgName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DepthWallpapers")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                        val cr = context.contentResolver
                        val mUri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                        if (mUri != null) {
                            cr.openOutputStream(mUri)?.use { s -> bgBitmap.compress(Bitmap.CompressFormat.JPEG, 92, s) }
                            cv.clear()
                            cv.put(MediaStore.MediaColumns.IS_PENDING, 0)
                            cr.update(mUri, cv, null, null)
                            bgUri = mUri
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DepthStudio", "MediaStore bg warning: ${e.message}")
                }

                var fgUri: Uri? = null
                if (!fgBase64.isNullOrBlank()) {
                    val cleanFg = if (fgBase64.contains(",")) fgBase64.substringAfter(",") else fgBase64
                    val fgBytes = Base64.decode(cleanFg, Base64.DEFAULT)
                    val fgBitmap = BitmapFactory.decodeByteArray(fgBytes, 0, fgBytes.size)
                    if (fgBitmap != null) {
                        val outFgName = "DepthCutout_$timeStamp.png"
                        val internalFgFile = File(internalDir, outFgName)
                        FileOutputStream(internalFgFile).use { out ->
                            fgBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        fgUri = Uri.fromFile(internalFgFile)
                    }
                }

                // Parse clock styling sent from Depth Studio
                var customLayoutMode: com.example.model.ClockLayoutMode? = null
                var customFontStyle: com.example.model.ClockFontStyle? = null
                var customColorHex: Long? = null
                var customFontSize: Float? = null
                var customTopPadding: Float? = null
                var customHorizOffset: Float? = null
                var customFontWeight: Int? = null
                var customOpacity: Float? = null
                var customShowWeather: Boolean? = null
                var customShowBattery: Boolean? = null
                var customShowCalendar: Boolean? = null

                if (!clockConfigJson.isNullOrBlank()) {
                    try {
                        val json = org.json.JSONObject(clockConfigJson)
                        val format = json.optString("format", "stacked")
                        val font = json.optString("font", "")
                        val color = json.optString("color", "#FFFFFF")
                        val size = json.optDouble("size", 96.0).toFloat()
                        val weight = json.optInt("weight", 600)
                        val opacity = json.optDouble("opacity", 1.0).toFloat()
                        val posY = json.optDouble("posY", 0.22).toFloat()
                        val posX = json.optDouble("posX", 0.5).toFloat()

                        if (json.has("showWeather")) customShowWeather = json.optBoolean("showWeather", false)
                        if (json.has("showBattery")) customShowBattery = json.optBoolean("showBattery", false)
                        if (json.has("showCalendar")) customShowCalendar = json.optBoolean("showCalendar", false)

                        customLayoutMode = if (format == "stacked") {
                            com.example.model.ClockLayoutMode.STACKED_EDITORIAL
                        } else {
                            com.example.model.ClockLayoutMode.HORIZONTAL_STANDARD
                        }

                        customFontStyle = when {
                            font.contains("Bodoni", ignoreCase = true) -> com.example.model.ClockFontStyle.EDITORIAL_SERIF
                            font.contains("Cormorant", ignoreCase = true) -> com.example.model.ClockFontStyle.SERIF_CLASSIC
                            font.contains("Playfair", ignoreCase = true) -> com.example.model.ClockFontStyle.PLAYFAIR_DISPLAY
                            font.contains("Mono", ignoreCase = true) || font.contains("Courier", ignoreCase = true) -> com.example.model.ClockFontStyle.TECH_STENCIL
                            font.contains("Rounded", ignoreCase = true) || font.contains("Outfit", ignoreCase = true) -> com.example.model.ClockFontStyle.ROUNDED_SLICK
                            font.contains("Minimal", ignoreCase = true) || font.contains("Thin", ignoreCase = true) -> com.example.model.ClockFontStyle.MINIMAL_THIN
                            font.contains("Script", ignoreCase = true) || font.contains("Cursive", ignoreCase = true) -> com.example.model.ClockFontStyle.ELEGANT_SCRIPT
                            font.contains("Retro", ignoreCase = true) || font.contains("Heavy", ignoreCase = true) || font.contains("Impact", ignoreCase = true) -> com.example.model.ClockFontStyle.RETRO_HEAVY
                            font.contains("Cyber", ignoreCase = true) || font.contains("Punk", ignoreCase = true) || font.contains("Stencil", ignoreCase = true) -> com.example.model.ClockFontStyle.CYBER_PUNK
                            else -> com.example.model.ClockFontStyle.BOLD_MODERN
                        }

                        if (color.startsWith("#")) {
                            try {
                                customColorHex = android.graphics.Color.parseColor(color).toLong() and 0xFFFFFFFFL
                            } catch (_: Throwable) {}
                        }

                        if (weight in 100..900) {
                            customFontWeight = weight
                        }
                        customOpacity = opacity.coerceIn(0.15f, 1.0f)

                        // Preview canvas in Depth Studio is 360w x 780h. posY is normalized center.
                        // Calculate exact matching top padding percent for lock screen
                        val dateOffsetNormalized = if (format == "stacked") ((size * 0.78f) + 20f) / 780f else ((size * 0.40f) + 20f) / 780f
                        customTopPadding = (posY - dateOffsetNormalized).coerceIn(0.02f, 0.65f)
                        customHorizOffset = ((posX - 0.5f) * 360f)
                        customFontSize = size.coerceIn(50f, 150f)
                    } catch (e: Exception) {
                        android.util.Log.w("DepthStudio", "Failed parsing clock config: ${e.message}")
                    }
                }

                viewModel.addCapturedPhoto(bgUri.toString())
                viewModel.setWallpaper(
                    resId = 0,
                    name = "AI Depth Wallpaper",
                    uri = bgUri.toString(),
                    context = context,
                    customForegroundUri = fgUri?.toString(),
                    customClockLayoutMode = customLayoutMode,
                    customClockFontStyle = customFontStyle,
                    customClockColorHex = customColorHex,
                    customClockFontSize = customFontSize,
                    customClockTopPadding = customTopPadding,
                    customClockHorizOffset = customHorizOffset,
                    customClockFontWeight = customFontWeight,
                    customClockOpacity = customOpacity,
                    customShowWeatherWidget = customShowWeather,
                    customShowBatteryWidget = customShowBattery,
                    customShowCalendarWidget = customShowCalendar
                )

                withContext(Dispatchers.Main) {
                    lastSavedImageUri = bgUri
                    dialogSavedFilename = outBgName
                    showSuccessDialog = true
                    val msg = if (fgUri != null) {
                        "✅ Berhasil dipasang ke Lock Screen dengan Efek Kedalaman 3D!"
                    } else {
                        "✅ Berhasil dipasang ke Lock Screen!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memasang wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler {
        if (onBack != null) {
            onBack()
        } else {
            viewModel.closeApp()
        }
    }

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
            // Spacer for slim iOS Status Bar
            Spacer(modifier = Modifier.height(38.dp))

            // Studio Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF121214))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (onBack != null) onBack() else viewModel.closeApp()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF242426))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "AI Depth Studio",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(IOSBlue.copy(alpha = 0.25f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "3-LAYER",
                                    color = IOSBlue,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Canvas • TypeScript • AI Segmentation",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Direct 1-Click Set Lock Screen Button
                    Button(
                        onClick = {
                            webViewInstance?.evaluateJavascript("exportWallpaper('set_wallpaper_now');", null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        shape = RoundedCornerShape(17.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "✨ Pasang",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick button to open Photos app
                    IconButton(
                        onClick = {
                            viewModel.openApp(AppId.PHOTOS)
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF242426))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = "Buka Galeri Foto",
                            tint = IOSBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            webViewInstance?.reload()
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF242426))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reload",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // High Performance Modern WebView
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        // Pre-create WebView code cache directories
                        try {
                            val codeCacheDir = File(ctx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
                            File(codeCacheDir, "js").mkdirs()
                            File(codeCacheDir, "wasm").mkdirs()
                        } catch (_: Throwable) {}

                        WebView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            // Enforce Software rendering on WebView to eliminate Mesa DRM rendernode failures and isolated renderer crashes
                            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                allowFileAccess = true
                                allowContentAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                displayZoomControls = false
                                builtInZoomControls = false
                                cacheMode = WebSettings.LOAD_NO_CACHE
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            // Native Android Javascript Interface Bridge
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun saveImageToGallery(base64Data: String, filename: String) {
                                    saveBase64Image(base64Data, filename, applyAsWallpaperDirectly = false)
                                }

                                @JavascriptInterface
                                fun saveAndSetWallpaper(base64Data: String, filename: String) {
                                    saveBase64Image(base64Data, filename, applyAsWallpaperDirectly = true)
                                }

                                @JavascriptInterface
                                fun saveAndSetDepthWallpaper(bgBase64: String, filename: String, fgBase64: String?, clockConfigJson: String?) {
                                    saveDepthWallpaper(bgBase64, filename, fgBase64, clockConfigJson)
                                }

                                @JavascriptInterface
                                fun saveAndSetDepthWallpaper(bgBase64: String, filename: String, fgBase64: String?) {
                                    saveDepthWallpaper(bgBase64, filename, fgBase64, null)
                                }

                                @JavascriptInterface
                                fun openPhotosApp() {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        viewModel.openApp(AppId.PHOTOS)
                                    }
                                }

                                @JavascriptInterface
                                fun openLockScreenSettings() {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        viewModel.openApp(AppId.SETTINGS)
                                    }
                                }
                            }, "AndroidBridge")

                            setDownloadListener { url, _, _, _, _ ->
                                if (!url.isNullOrBlank() && url.startsWith("data:")) {
                                    val isPng = url.contains("image/png")
                                    val ext = if (isPng) "png" else "jpg"
                                    val name = "DepthWallpaper_${System.currentTimeMillis()}.$ext"
                                    saveBase64Image(url, name, applyAsWallpaperDirectly = false)
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                    return true // Prevent host app termination on render process crash
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    fileUploadCallback?.onReceiveValue(null)
                                    fileUploadCallback = filePathCallback
                                    filePickerLauncher.launch("image/*")
                                    return true
                                }
                            }

                            loadUrl("file:///android_asset/depth_studio.html")
                            webViewInstance = this
                        }
                    }
                )
            }

            // Persistent Bottom Bar: Direct 1-Click "Simpan & Pasang Jadi Lock Screen"
            Surface(
                color = Color(0xFF141416),
                border = BorderStroke(1.dp, Color(0xFF26262B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Depth Effect Ready",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "3-Layer Sinkron ke Layar Kunci",
                            color = Color(0xFF9E9EA2),
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = {
                            webViewInstance?.evaluateJavascript("exportWallpaper('set_wallpaper_now');", null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "✨ Simpan & Pasang",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // iOS Home Indicator Bar
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }

        // Export Success & How-To-Use Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCCCCCC),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Wallpaper Berhasil Disimpan!",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "File telah tersimpan rapi dan dapat diakses dengan mudah:",
                            fontSize = 13.sp,
                            color = Color(0xFFAAAAAA)
                        )

                        // Info cards
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2C2C2E))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📁 Lokasi Penyimpanan:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF32ADE6)
                                )
                                Text(
                                    text = "• Galeri Foto HP (Folder: Pictures/DepthWallpapers)\n• Aplikasi 'Photos' bawaan di iOS simulator ini\n• Nama file: $dialogSavedFilename",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2C2C2E))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "📲 Cara Pasang Jadi Lock Screen:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9F0A)
                                )
                                Text(
                                    text = "1. Langsung klik tombol 'Pasang Sekarang' di bawah ini.\n2. Atau buka aplikasi Photos -> pilih foto -> klik 'Set as Lock Screen'.\n3. Atau kunci layar (Lock) untuk melihat langsung hasil tampilan lock screen kamu!",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (lastSavedImageUri != null) {
                                viewModel.setWallpaper(0, "AI Depth Wallpaper", lastSavedImageUri.toString(), context)
                            }
                            showSuccessDialog = false
                            Toast.makeText(context, "Lockscreen wallpaper berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Pasang ke Lock Screen Sekarang", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSuccessDialog = false
                            viewModel.openApp(AppId.PHOTOS)
                        }
                    ) {
                        Text("Buka di Photos", color = Color(0xFF32ADE6), fontSize = 13.sp)
                    }
                }
            )
        }
    }
}
