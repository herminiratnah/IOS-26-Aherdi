package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel
import com.example.viewmodel.InstalledAppItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLibraryScreen(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    val customAppIcons by viewModel.customAppIcons.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFolder by remember { mutableStateOf<String?>(null) }
    var selectedAppForContextMenu by remember { mutableStateOf<InstalledAppItem?>(null) }
    var targetAppKeyForCustomIcon by remember { mutableStateOf<String?>(null) }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && targetAppKeyForCustomIcon != null) {
            viewModel.setCustomAppIcon(targetAppKeyForCustomIcon!!, uri.toString(), context)
            targetAppKeyForCustomIcon = null
        }
    }

    // Authentic categories matching user screenshot
    val categories = listOf(
        "Recent",
        "Game",
        "Audio",
        "Video",
        "Image",
        "Social",
        "News",
        "Maps",
        "Utilities",
        "Other"
    )

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) emptyList()
        else installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
            .distinctBy { it.packageName }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        // App Library Search Pill matching iOS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x40000000))
                .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "App Library",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .clickable { searchQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            // Alphabetical Search Results View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(filteredApps) { app ->
                    val appKey = app.builtInId?.name ?: app.packageName
                    val customIconUri = customAppIcons[appKey]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x30000000))
                            .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { viewModel.launchDeviceApp(context, app) },
                                onLongClick = { selectedAppForContextMenu = app }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(app.iconColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customIconUri != null) {
                                AsyncImage(
                                    model = customIconUri,
                                    contentDescription = app.appName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (app.iconBitmap != null) {
                                Image(
                                    bitmap = app.iconBitmap.asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = getAppCategoryIcon(app),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = app.category,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // 2-Column Grid of iOS Folders matching screenshot
            val activeCategories = remember(installedApps) {
                categories.filter { catName ->
                    installedApps.any { it.category == catName }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(activeCategories) { categoryName ->
                    val appsInCat = installedApps.filter { it.category == categoryName }
                    AppLibraryFolderCard(
                        folderTitle = categoryName,
                        apps = appsInCat,
                        customAppIcons = customAppIcons,
                        onAppClick = { app ->
                            viewModel.launchDeviceApp(context, app)
                        },
                        onAppLongClick = { app ->
                            selectedAppForContextMenu = app
                        },
                        onFolderClick = {
                            selectedCategoryFolder = categoryName
                        }
                    )
                }
            }
        }
    }

    // Modal dialog when tapping folder cluster or expanded view
    selectedCategoryFolder?.let { categoryName ->
        val appsInFolder = installedApps.filter { it.category == categoryName }
        AlertDialog(
            onDismissRequest = { selectedCategoryFolder = null },
            containerColor = Color(0xEE1C1C1E),
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = categoryName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(appsInFolder) { app ->
                        val appKey = app.builtInId?.name ?: app.packageName
                        val customIconUri = customAppIcons[appKey]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x20FFFFFF))
                                .combinedClickable(
                                    onClick = {
                                        selectedCategoryFolder = null
                                        viewModel.launchDeviceApp(context, app)
                                    },
                                    onLongClick = {
                                        selectedCategoryFolder = null
                                        selectedAppForContextMenu = app
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(app.iconColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (customIconUri != null) {
                                    AsyncImage(
                                        model = customIconUri,
                                        contentDescription = app.appName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap.asImageBitmap(),
                                        contentDescription = app.appName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = getAppCategoryIcon(app),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = app.appName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCategoryFolder = null }) {
                    Text("Tutup", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        )
    }

    // 3D Touch / Long Press Context Menu Dialog for any app
    selectedAppForContextMenu?.let { app ->
        val appKey = app.builtInId?.name ?: app.packageName
        val customIconUri = customAppIcons[appKey]

        AlertDialog(
            onDismissRequest = { selectedAppForContextMenu = null },
            containerColor = Color(0xFA2C2C2E),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Color(app.iconColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (customIconUri != null) {
                            AsyncImage(
                                model = customIconUri,
                                contentDescription = app.appName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else if (app.iconBitmap != null) {
                            Image(
                                bitmap = app.iconBitmap.asImageBitmap(),
                                contentDescription = app.appName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = getAppCategoryIcon(app),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = app.appName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.category,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Open App
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val target = app
                                selectedAppForContextMenu = null
                                viewModel.launchDeviceApp(context, target)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Buka Aplikasi", color = Color.White, fontSize = 15.sp)
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = Color(0x30FFFFFF), thickness = 0.5.dp)

                    // Change Icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                targetAppKeyForCustomIcon = appKey
                                selectedAppForContextMenu = null
                                iconPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ganti Ikon (Custom Icon)", color = Color(0xFF0A84FF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                    }

                    if (customIconUri != null) {
                        HorizontalDivider(color = Color(0x30FFFFFF), thickness = 0.5.dp)

                        // Reset Icon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    viewModel.resetCustomAppIcon(appKey, context)
                                    selectedAppForContextMenu = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kembalikan Ikon Bawaan", color = Color(0xFFFF453A), fontSize = 15.sp)
                            Icon(Icons.Rounded.Restore, contentDescription = null, tint = Color(0xFFFF453A), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForContextMenu = null }) {
                    Text("Batal", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }
}

/**
 * iOS App Library 2x2 Folder Card matching user screenshot
 */
@Composable
fun AppLibraryFolderCard(
    folderTitle: String,
    apps: List<InstalledAppItem>,
    customAppIcons: Map<String, String> = emptyMap(),
    onAppClick: (InstalledAppItem) -> Unit,
    onAppLongClick: (InstalledAppItem) -> Unit,
    onFolderClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Frosted glass squircle container
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0x38FFFFFF))
                .border(0.5.dp, Color(0x30FFFFFF), RoundedCornerShape(26.dp))
                .padding(11.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row (Slots 0 and 1)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val app0 = apps.getOrNull(0)
                    if (app0 != null) {
                        val k0 = app0.builtInId?.name ?: app0.packageName
                        FolderAppIcon(
                            app = app0,
                            customIconUri = customAppIcons[k0],
                            onClick = { onAppClick(app0) },
                            onLongClick = { onAppLongClick(app0) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(62.dp))
                    }

                    val app1 = apps.getOrNull(1)
                    if (app1 != null) {
                        val k1 = app1.builtInId?.name ?: app1.packageName
                        FolderAppIcon(
                            app = app1,
                            customIconUri = customAppIcons[k1],
                            onClick = { onAppClick(app1) },
                            onLongClick = { onAppLongClick(app1) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(62.dp))
                    }
                }

                // Bottom Row (Slots 2 and 3/Cluster)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val app2 = apps.getOrNull(2)
                    if (app2 != null) {
                        val k2 = app2.builtInId?.name ?: app2.packageName
                        FolderAppIcon(
                            app = app2,
                            customIconUri = customAppIcons[k2],
                            onClick = { onAppClick(app2) },
                            onLongClick = { onAppLongClick(app2) }
                        )
                    } else {
                        Spacer(modifier = Modifier.size(62.dp))
                    }

                    if (apps.size <= 4) {
                        val app3 = apps.getOrNull(3)
                        if (app3 != null) {
                            val k3 = app3.builtInId?.name ?: app3.packageName
                            FolderAppIcon(
                                app = app3,
                                customIconUri = customAppIcons[k3],
                                onClick = { onAppClick(app3) },
                                onLongClick = { onAppLongClick(app3) }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(62.dp))
                        }
                    } else {
                        // 4th slot: 2x2 Mini Cluster of icons representing remaining apps
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x22000000))
                                .clickable { onFolderClick() }
                                .padding(5.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val app3 = apps.getOrNull(3)
                                    val k3 = app3?.let { it.builtInId?.name ?: it.packageName }
                                    MiniClusterDot(app3, customIconUri = k3?.let { customAppIcons[it] })

                                    val app4 = apps.getOrNull(4)
                                    val k4 = app4?.let { it.builtInId?.name ?: it.packageName }
                                    MiniClusterDot(app4, customIconUri = k4?.let { customAppIcons[it] })
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val app5 = apps.getOrNull(5)
                                    val k5 = app5?.let { it.builtInId?.name ?: it.packageName }
                                    MiniClusterDot(app5, customIconUri = k5?.let { customAppIcons[it] })

                                    val app6 = apps.getOrNull(6)
                                    val k6 = app6?.let { it.builtInId?.name ?: it.packageName }
                                    MiniClusterDot(app6, customIconUri = k6?.let { customAppIcons[it] })
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = folderTitle,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderAppIcon(
    app: InstalledAppItem,
    customIconUri: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(app.iconColor))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (customIconUri != null) {
            AsyncImage(
                model = customIconUri,
                contentDescription = app.appName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (app.iconBitmap != null) {
            Image(
                bitmap = app.iconBitmap.asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = getAppCategoryIcon(app),
                contentDescription = app.appName,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun MiniClusterDot(
    app: InstalledAppItem?,
    customIconUri: String? = null
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (app != null) Color(app.iconColor) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            if (customIconUri != null) {
                AsyncImage(
                    model = customIconUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = getAppCategoryIcon(app),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

fun getAppCategoryIcon(app: InstalledAppItem): ImageVector {
    return when (app.builtInId) {
        AppId.PHONE -> Icons.Rounded.Call
        AppId.SAFARI -> Icons.Rounded.Explore
        AppId.MESSAGES -> Icons.Rounded.ChatBubble
        AppId.MUSIC -> Icons.Rounded.MusicNote
        AppId.MAIL -> Icons.Rounded.Mail
        AppId.PHOTOS -> Icons.Rounded.Photo
        AppId.CAMERA -> Icons.Rounded.CameraAlt
        AppId.WEATHER -> Icons.Rounded.WbSunny
        AppId.CLOCK -> Icons.Rounded.AccessTime
        AppId.CALCULATOR -> Icons.Rounded.Calculate
        AppId.NOTES -> Icons.Rounded.Description
        AppId.SETTINGS -> Icons.Rounded.Settings
        AppId.ROOT_TOOLS -> Icons.Rounded.Security
        AppId.CALENDAR -> Icons.Rounded.CalendarToday
        AppId.HEALTH -> Icons.Rounded.Favorite
        AppId.WALLET -> Icons.Rounded.AccountBalanceWallet
        else -> {
            when (app.category) {
                "Recent" -> Icons.Rounded.History
                "Game" -> Icons.Rounded.SportsEsports
                "Audio" -> Icons.Rounded.Headphones
                "Video" -> Icons.Rounded.PlayCircleFilled
                "Image" -> Icons.Rounded.Image
                "Social" -> Icons.Rounded.Chat
                "News" -> Icons.Rounded.Article
                "Maps" -> Icons.Rounded.Place
                "Utilities" -> Icons.Rounded.Build
                else -> Icons.Rounded.Apps
            }
        }
    }
}
