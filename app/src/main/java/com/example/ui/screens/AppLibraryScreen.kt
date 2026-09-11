package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@Composable
fun AppLibraryScreen(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedApps by viewModel.installedApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFolder by remember { mutableStateOf<String?>(null) }

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x30000000))
                            .border(0.5.dp, Color(0x20FFFFFF), RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.launchDeviceApp(context, app)
                            }
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
                            if (app.iconBitmap != null) {
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
                        onAppClick = { app ->
                            viewModel.launchDeviceApp(context, app)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x20FFFFFF))
                                .clickable {
                                    selectedCategoryFolder = null
                                    viewModel.launchDeviceApp(context, app)
                                }
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
                                if (app.iconBitmap != null) {
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
}

/**
 * iOS App Library 2x2 Folder Card matching user screenshot
 */
@Composable
fun AppLibraryFolderCard(
    folderTitle: String,
    apps: List<InstalledAppItem>,
    onAppClick: (InstalledAppItem) -> Unit,
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
                        FolderAppIcon(app = app0, onClick = { onAppClick(app0) })
                    } else {
                        Spacer(modifier = Modifier.size(62.dp))
                    }

                    val app1 = apps.getOrNull(1)
                    if (app1 != null) {
                        FolderAppIcon(app = app1, onClick = { onAppClick(app1) })
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
                        FolderAppIcon(app = app2, onClick = { onAppClick(app2) })
                    } else {
                        Spacer(modifier = Modifier.size(62.dp))
                    }

                    if (apps.size <= 4) {
                        val app3 = apps.getOrNull(3)
                        if (app3 != null) {
                            FolderAppIcon(app = app3, onClick = { onAppClick(app3) })
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
                                    MiniClusterDot(apps.getOrNull(3))
                                    MiniClusterDot(apps.getOrNull(4))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MiniClusterDot(apps.getOrNull(5))
                                    MiniClusterDot(apps.getOrNull(6))
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

@Composable
fun FolderAppIcon(
    app: InstalledAppItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(62.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(app.iconColor))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (app.iconBitmap != null) {
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
fun MiniClusterDot(app: InstalledAppItem?) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (app != null) Color(app.iconColor) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (app != null) {
            if (app.iconBitmap != null) {
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
