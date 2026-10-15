package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.IOSPersistenceManager
import com.example.model.AppId
import com.example.model.AppItem
import com.example.util.AppIconCache
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel
import com.example.viewmodel.InstalledAppItem
import com.example.viewmodel.toBitmapSafe

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLibraryScreen(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val installedApps by viewModel.installedApps.collectAsState()
    val customAppIcons by viewModel.customAppIcons.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFolder by remember { mutableStateOf<String?>(null) }
    var selectedAppForContextMenu by remember { mutableStateOf<InstalledAppItem?>(null) }
    var targetAppKeyForCustomIcon by remember { mutableStateOf<String?>(null) }

    // Folder Customization & Drag-and-Drop States
    var customFolders by remember {
        mutableStateOf(IOSPersistenceManager.loadCustomFolders(context))
    }
    var draggingApp by remember { mutableStateOf<InstalledAppItem?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredFolder by remember { mutableStateOf<String?>(null) }
    val folderBoundsMap = remember { mutableStateMapOf<String, Rect>() }
    var rootLayoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showLibraryManagementDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var appPendingForNewFolder by remember { mutableStateOf<InstalledAppItem?>(null) }
    var showMoveFolderPickerForApp by remember { mutableStateOf<InstalledAppItem?>(null) }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && targetAppKeyForCustomIcon != null) {
            viewModel.setCustomAppIcon(targetAppKeyForCustomIcon!!, uri.toString(), context)
            targetAppKeyForCustomIcon = null
        }
    }

    // Default categories matching authentic iOS App Library
    val defaultCategories = listOf(
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

    // Complete set of all available folders (default + custom created by user)
    val allFolders = remember(customFolders, installedApps) {
        val appCats = installedApps.map { it.category }.distinct()
        (defaultCategories + customFolders + appCats).distinct()
    }

    val activeCategories = remember(installedApps, allFolders, customFolders) {
        allFolders.filter { catName ->
            installedApps.any { it.category == catName } || customFolders.contains(catName)
        }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) emptyList()
        else installedApps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
            .distinctBy { it.packageName }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { rootLayoutCoordinates = it }
            .combinedClickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = {},
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showLibraryManagementDialog = true
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            // App Library Search Pill matching iOS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp)
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



            // Drop zone banner when dragging: Create New Folder target
            AnimatedVisibility(
                visible = draggingApp != null,
                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
            ) {
                val isNewHovered = hoveredFolder == "__CREATE_NEW__"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isNewHovered) Color(0x7034C759) else Color(0x3534C759))
                        .border(if (isNewHovered) 2.dp else 1.dp, Color(0xFF34C759), RoundedCornerShape(18.dp))
                        .onGloballyPositioned { coords ->
                            folderBoundsMap["__CREATE_NEW__"] = coords.boundsInRoot()
                        }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isNewHovered) "Lepas di sini untuk Buat Folder Baru!" else "Tarik ke sini untuk buat folder baru",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
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
                            AppLibraryIconView(
                                app = app,
                                customIconUri = customIconUri,
                                size = 46.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Folder: ${app.category}",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showMoveFolderPickerForApp = app }) {
                                Icon(Icons.Rounded.DriveFileMove, contentDescription = "Pindahkan Folder", tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            } else {
                // 2-Column Grid of iOS Folders
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 36.dp)
                ) {
                    items(activeCategories) { categoryName ->
                        val appsInCat = installedApps.filter { it.category == categoryName }
                        val isCustom = customFolders.contains(categoryName)
                        val isHovered = hoveredFolder == categoryName && draggingApp != null && draggingApp?.category != categoryName

                        AppLibraryFolderCard(
                            folderTitle = categoryName,
                            apps = appsInCat,
                            customAppIcons = customAppIcons,
                            isCustomFolder = isCustom,
                            isDropTargetHovered = isHovered,
                            draggingApp = draggingApp,
                            onRegisterBounds = { bounds ->
                                folderBoundsMap[categoryName] = bounds
                            },
                            onAppClick = { app ->
                                viewModel.launchDeviceApp(context, app)
                            },
                            onAppLongClick = { app ->
                                selectedAppForContextMenu = app
                            },
                            onAppDragStart = { app, rootStart ->
                                draggingApp = app
                                val origin = rootLayoutCoordinates?.positionInRoot() ?: Offset.Zero
                                val localStart = rootStart - origin
                                dragPosition = localStart
                            },
                            onAppDragMove = { delta ->
                                dragPosition += delta
                                val origin = rootLayoutCoordinates?.positionInRoot() ?: Offset.Zero
                                val rootPos = origin + dragPosition
                                hoveredFolder = folderBoundsMap.entries.firstOrNull { (_, rect) ->
                                    rect.contains(rootPos)
                                }?.key
                            },
                            onAppDragEnd = {
                                val target = hoveredFolder
                                val currentApp = draggingApp
                                if (currentApp != null && target != null) {
                                    if (target == "__CREATE_NEW__") {
                                        appPendingForNewFolder = currentApp
                                        newFolderNameInput = ""
                                        showCreateFolderDialog = true
                                    } else if (target != currentApp.category) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        viewModel.moveAppToCategory(currentApp, target, context)
                                        customFolders = IOSPersistenceManager.loadCustomFolders(context)
                                    }
                                }
                                draggingApp = null
                                hoveredFolder = null
                            },
                            onAppDragCancel = {
                                draggingApp = null
                                hoveredFolder = null
                            },
                            onFolderClick = {
                                selectedCategoryFolder = categoryName
                            }
                        )
                    }
                }
            }
        }

        // Floating Dragged Icon Overlay
        if (draggingApp != null) {
            val app = draggingApp!!
            val appKey = app.builtInId?.name ?: app.packageName
            val customIcon = customAppIcons[appKey]

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPosition.x - 36.dp.toPx()).toInt(),
                            (dragPosition.y - 36.dp.toPx()).toInt()
                        )
                    }
                    .size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                AppLibraryIconView(
                    app = app,
                    customIconUri = customIcon,
                    size = 72.dp
                )
            }
        }
    }

    // Modal dialog when tapping folder cluster or expanded view
    selectedCategoryFolder?.let { categoryName ->
        val appsInFolder = installedApps.filter { it.category == categoryName }
        val isCustom = customFolders.contains(categoryName)

        AlertDialog(
            onDismissRequest = { selectedCategoryFolder = null },
            containerColor = Color(0xEE1C1C1E),
            shape = RoundedCornerShape(26.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = categoryName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${appsInFolder.size} aplikasi",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    if (isCustom) {
                        TextButton(
                            onClick = {
                                selectedCategoryFolder = null
                                viewModel.deleteCustomFolder(categoryName, context)
                                customFolders = IOSPersistenceManager.loadCustomFolders(context)
                            }
                        ) {
                            Text("Hapus Folder", color = Color(0xFFFF453A), fontSize = 13.sp)
                        }
                    }
                }
            },
            text = {
                if (appsInFolder.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(42.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Folder Kosong", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Tarik aplikasi ke folder ini untuk mengaturnya", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
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
                                AppLibraryIconView(
                                    app = app,
                                    customIconUri = customIconUri,
                                    size = 44.dp
                                )
                                Text(
                                    text = app.appName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        selectedCategoryFolder = null
                                        showMoveFolderPickerForApp = app
                                    }
                                ) {
                                    Icon(Icons.Rounded.DriveFileMove, contentDescription = "Pindahkan", tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                                }
                            }
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

    // Move to Folder Picker Dialog
    showMoveFolderPickerForApp?.let { app ->
        AlertDialog(
            onDismissRequest = { showMoveFolderPickerForApp = null },
            containerColor = Color(0xFA2C2C2E),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.DriveFileMove, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(24.dp))
                    Column {
                        Text("Pindahkan Aplikasi", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(app.appName, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pilih folder tujuan:", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)

                    // Button to create a new folder directly from picker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x300A84FF))
                            .clickable {
                                appPendingForNewFolder = app
                                showMoveFolderPickerForApp = null
                                newFolderNameInput = ""
                                showCreateFolderDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                        Text("+ Buat Folder Baru...", color = Color(0xFF0A84FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = Color(0x30FFFFFF), thickness = 0.5.dp)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allFolders) { folderName ->
                            val isCurrent = app.category == folderName
                            val count = installedApps.count { it.category == folderName }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) Color(0x200A84FF) else Color(0x15FFFFFF))
                                    .clickable {
                                        viewModel.moveAppToCategory(app, folderName, context)
                                        customFolders = IOSPersistenceManager.loadCustomFolders(context)
                                        showMoveFolderPickerForApp = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent) Icons.Rounded.FolderOpen else Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = if (isCurrent) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = folderName,
                                        color = if (isCurrent) Color(0xFF0A84FF) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = if (isCurrent) "Folder Saat Ini" else "$count app",
                                    color = if (isCurrent) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveFolderPickerForApp = null }) {
                    Text("Batal", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        )
    }

    // Create New Custom Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                appPendingForNewFolder = null
            },
            containerColor = Color(0xFA2C2C2E),
            shape = RoundedCornerShape(22.dp),
            title = {
                Text("Folder Baru", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (appPendingForNewFolder != null) {
                            "Masukkan nama folder untuk memindahkan '${appPendingForNewFolder!!.appName}':"
                        } else {
                            "Masukkan nama folder baru:"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text("Contoh: Favorit, Sosmed, Kerja", color = Color.White.copy(alpha = 0.4f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0A84FF),
                            unfocusedBorderColor = Color(0x40FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newFolderNameInput.trim()
                        if (name.isNotEmpty()) {
                            viewModel.createCustomFolder(name, context)
                            appPendingForNewFolder?.let { app ->
                                viewModel.moveAppToCategory(app, name, context)
                            }
                            customFolders = IOSPersistenceManager.loadCustomFolders(context)
                            showCreateFolderDialog = false
                            appPendingForNewFolder = null
                            newFolderNameInput = ""
                        }
                    }
                ) {
                    Text("Buat Folder", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        appPendingForNewFolder = null
                    }
                ) {
                    Text("Batal", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
                }
            }
        )
    }

    // App Library Management Dialog (Triggered by Long-Pressing anywhere in App Library)
    if (showLibraryManagementDialog) {
        AlertDialog(
            onDismissRequest = { showLibraryManagementDialog = false },
            containerColor = Color(0xF01C1C1E),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0A84FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.FolderCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "Kelola App Library",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // + Buat Folder Baru
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable {
                                showLibraryManagementDialog = false
                                newFolderNameInput = ""
                                appPendingForNewFolder = null
                                showCreateFolderDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                            Text("+ Buat Folder Baru", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }

                    // Reset Pengaturan Folder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable {
                                showLibraryManagementDialog = false
                                viewModel.resetAppCategories(context)
                                customFolders = IOSPersistenceManager.loadCustomFolders(context)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(20.dp))
                            Text("Reset Folder Kategori", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }

                    // Muat Ulang Aplikasi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x20FFFFFF))
                            .clickable {
                                showLibraryManagementDialog = false
                                viewModel.loadDeviceInstalledApps(context)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(20.dp))
                            Text("Muat Ulang Aplikasi (Reload)", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibraryManagementDialog = false }) {
                    Text("Tutup", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                    AppLibraryIconView(
                        app = app,
                        customIconUri = customIconUri,
                        size = 52.dp
                    )
                    Column {
                        Text(
                            text = app.appName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Folder: ${app.category}",
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

                    // Move to Another Folder (Pindahkan ke Folder...)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val target = app
                                selectedAppForContextMenu = null
                                showMoveFolderPickerForApp = target
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pindahkan ke Folder...", color = Color(0xFF0A84FF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Rounded.DriveFileMove, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = Color(0x30FFFFFF), thickness = 0.5.dp)

                    // Add to Home Screen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val target = app
                                selectedAppForContextMenu = null
                                viewModel.addInstalledAppToHomeScreen(target, context)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tambah ke Layar Utama", color = Color(0xFF34C759), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Rounded.AddCircleOutline, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = Color(0x30FFFFFF), thickness = 0.5.dp)

                    // Add to Dock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val target = app
                                selectedAppForContextMenu = null
                                val newAppItem = if (target.isBuiltIn && target.builtInId != null) {
                                    AppItem(id = target.builtInId, name = target.appName, iconBgColor = Color(target.iconColor), isDock = true)
                                } else {
                                    val bmp = target.iconBitmap ?: AppIconCache.getIcon(context, target.packageName)
                                    AppItem(
                                        id = AppId.CUSTOM_APP,
                                        name = target.appName,
                                        iconBgColor = Color.Transparent,
                                        packageName = target.packageName,
                                        isInstalledApp = true,
                                        customBitmap = bmp,
                                        isDock = true
                                    )
                                }
                                viewModel.addDockApp(newAppItem, context)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tambah ke Dock", color = Color(0xFF0A84FF), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Rounded.Dock, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(18.dp))
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
 * iOS App Library 2x2 Folder Card supporting interactive Drag & Drop and custom folder management
 */
@Composable
fun AppLibraryFolderCard(
    folderTitle: String,
    apps: List<InstalledAppItem>,
    customAppIcons: Map<String, String> = emptyMap(),
    isCustomFolder: Boolean = false,
    isDropTargetHovered: Boolean = false,
    draggingApp: InstalledAppItem? = null,
    onRegisterBounds: (Rect) -> Unit,
    onAppClick: (InstalledAppItem) -> Unit,
    onAppLongClick: (InstalledAppItem) -> Unit,
    onAppDragStart: (InstalledAppItem, Offset) -> Unit,
    onAppDragMove: (Offset) -> Unit,
    onAppDragEnd: () -> Unit,
    onAppDragCancel: () -> Unit,
    onFolderClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDropTargetHovered) 1.05f else 1.0f,
        animationSpec = tween(150),
        label = "folderScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onGloballyPositioned { coords ->
                onRegisterBounds(coords.boundsInRoot())
            }
    ) {
        // Frosted glass squircle container
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isDropTargetHovered) Color(0x600A84FF) else Color(0x38FFFFFF)
                )
                .border(
                    BorderStroke(
                        width = if (isDropTargetHovered) 2.5.dp else 0.5.dp,
                        color = if (isDropTargetHovered) Color(0xFF0A84FF) else Color(0x30FFFFFF)
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(11.dp)
        ) {
            if (apps.isEmpty()) {
                // Empty folder placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onFolderClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tarik ke sini",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
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
                                isBeingDragged = draggingApp == app0,
                                onClick = { onAppClick(app0) },
                                onLongClick = { onAppLongClick(app0) },
                                onDragStart = { rootPos -> onAppDragStart(app0, rootPos) },
                                onDragMove = onAppDragMove,
                                onDragEnd = onAppDragEnd,
                                onDragCancel = onAppDragCancel
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
                                isBeingDragged = draggingApp == app1,
                                onClick = { onAppClick(app1) },
                                onLongClick = { onAppLongClick(app1) },
                                onDragStart = { rootPos -> onAppDragStart(app1, rootPos) },
                                onDragMove = onAppDragMove,
                                onDragEnd = onAppDragEnd,
                                onDragCancel = onAppDragCancel
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
                                isBeingDragged = draggingApp == app2,
                                onClick = { onAppClick(app2) },
                                onLongClick = { onAppLongClick(app2) },
                                onDragStart = { rootPos -> onAppDragStart(app2, rootPos) },
                                onDragMove = onAppDragMove,
                                onDragEnd = onAppDragEnd,
                                onDragCancel = onAppDragCancel
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
                                    isBeingDragged = draggingApp == app3,
                                    onClick = { onAppClick(app3) },
                                    onLongClick = { onAppLongClick(app3) },
                                    onDragStart = { rootPos -> onAppDragStart(app3, rootPos) },
                                    onDragMove = onAppDragMove,
                                    onDragEnd = onAppDragEnd,
                                    onDragCancel = onAppDragCancel
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

            // Drop indicator overlay banner when hovering
            if (isDropTargetHovered) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0x880A84FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Lepas di sini",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = folderTitle,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isCustomFolder) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A84FF))
                )
            }
        }
    }
}

@Composable
fun AppLibraryIconView(
    app: InstalledAppItem,
    customIconUri: String? = null,
    size: androidx.compose.ui.unit.Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    if (customIconUri != null) {
        LiquidGlassIconBox(
            modifier = modifier.size(size),
            backgroundColor = Color(app.iconColor),
            shape = RoundedCornerShape(13.5.dp)
        ) {
            AsyncImage(
                model = customIconUri,
                contentDescription = app.appName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else if (!app.isBuiltIn && app.packageName.isNotBlank()) {
        val bmp = app.iconBitmap ?: remember(app.packageName) { AppIconCache.getIcon(ctx, app.packageName) }
        LiquidGlassThirdPartyIcon(
            bitmap = bmp,
            appName = app.appName,
            modifier = modifier,
            backgroundColor = Color(app.iconColor),
            size = size
        )
    } else {
        val effectiveBg = if (app.builtInId != null) Color.Transparent else Color(app.iconColor)
        LiquidGlassIconBox(
            modifier = modifier.size(size),
            backgroundColor = effectiveBg,
            shape = RoundedCornerShape(13.5.dp)
        ) {
            val bmp = app.iconBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (app.builtInId != null) {
                IOS26BuiltInAppIcon(
                    appId = app.builtInId,
                    appName = app.appName,
                    size = size
                )
            } else {
                Icon(
                    imageVector = getAppCategoryIcon(app),
                    contentDescription = app.appName,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderAppIcon(
    app: InstalledAppItem,
    customIconUri: String? = null,
    isBeingDragged: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onDragStart: ((Offset) -> Unit)? = null,
    onDragMove: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onDragCancel: (() -> Unit)? = null
) {
    val view = LocalView.current
    var iconCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = Modifier
            .size(60.dp)
            .scale(if (isBeingDragged) 0.85f else 1.0f)
            .alpha(if (isBeingDragged) 0.35f else 1.0f)
            .onGloballyPositioned { iconCoordinates = it }
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick?.invoke()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AppLibraryIconView(
            app = app,
            customIconUri = customIconUri,
            size = 60.dp
        )
    }
}

@Composable
fun MiniClusterDot(
    app: InstalledAppItem?,
    customIconUri: String? = null
) {
    if (app != null) {
        AppLibraryIconView(
            app = app,
            customIconUri = customIconUri,
            size = 24.dp
        )
    } else {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Transparent)
        )
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
