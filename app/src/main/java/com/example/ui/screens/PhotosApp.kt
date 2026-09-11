package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.model.PhotoItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

enum class PhotosTab {
    LIBRARY,
    FOR_YOU,
    ALBUMS,
    SEARCH
}

@Composable
fun PhotosApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val photos by viewModel.photoItems.collectAsState()

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            viewModel.loadDevicePhotos(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            storagePermissionLauncher.launch(permissionToRequest)
        } else {
            viewModel.loadDevicePhotos(context)
        }
    }

    var selectedTab by remember { mutableStateOf(PhotosTab.LIBRARY) }
    var selectedPhoto by remember { mutableStateOf<PhotoItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedWidgetPhotoIds by viewModel.selectedWidgetPhotoIds.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            // iOS Status bar (Dark Icons for white background)
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = true
            )

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(IOSBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Photos",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${photos.size} Items",
                        fontSize = 12.sp,
                        color = IOSSystemGray
                    )
                }

                IconButton(onClick = {
                    if (!hasStoragePermission) {
                        storagePermissionLauncher.launch(permissionToRequest)
                    } else {
                        viewModel.loadDevicePhotos(context)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Photos",
                        tint = IOSBlue
                    )
                }
            }

            // Permission banner if permission not granted
            if (!hasStoragePermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tampilkan Foto Perangkat", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("Izinkan akses foto galeri untuk melihat foto di HP ini", fontSize = 11.sp, color = IOSSystemGray)
                        }
                        Button(
                            onClick = { storagePermissionLauncher.launch(permissionToRequest) },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Izinkan", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Main Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    PhotosTab.LIBRARY -> {
                        PhotosLibraryView(
                            photos = photos,
                            onPhotoClick = { selectedPhoto = it }
                        )
                    }
                    PhotosTab.FOR_YOU -> {
                        PhotosForYouView(
                            photos = photos,
                            onPhotoClick = { selectedPhoto = it }
                        )
                    }
                    PhotosTab.ALBUMS -> {
                        PhotosAlbumsView(
                            photos = photos,
                            onPhotoClick = { selectedPhoto = it }
                        )
                    }
                    PhotosTab.SEARCH -> {
                        PhotosSearchView(
                            photos = photos,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onPhotoClick = { selectedPhoto = it }
                        )
                    }
                }
            }

            // iOS Photos Bottom Tab Bar
            HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.8.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xFFF9F9F9)),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhotosBottomNavItem(
                    icon = Icons.Rounded.PhotoLibrary,
                    label = "Library",
                    isSelected = selectedTab == PhotosTab.LIBRARY,
                    onClick = { selectedTab = PhotosTab.LIBRARY }
                )
                PhotosBottomNavItem(
                    icon = Icons.Rounded.AutoAwesome,
                    label = "For You",
                    isSelected = selectedTab == PhotosTab.FOR_YOU,
                    onClick = { selectedTab = PhotosTab.FOR_YOU }
                )
                PhotosBottomNavItem(
                    icon = Icons.Rounded.FolderSpecial,
                    label = "Albums",
                    isSelected = selectedTab == PhotosTab.ALBUMS,
                    onClick = { selectedTab = PhotosTab.ALBUMS }
                )
                PhotosBottomNavItem(
                    icon = Icons.Rounded.Search,
                    label = "Search",
                    isSelected = selectedTab == PhotosTab.SEARCH,
                    onClick = { selectedTab = PhotosTab.SEARCH }
                )
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }

        // Full Screen Photo Viewer Dialog
        selectedPhoto?.let { photo ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Photo Image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { selectedPhoto = null },
                    contentAlignment = Alignment.Center
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
                }

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedPhoto = null },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(photo.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(photo.date, color = Color.LightGray, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isInWidget = selectedWidgetPhotoIds.contains(photo.id)
                        IconButton(
                            onClick = {
                                viewModel.togglePhotoInWidgetSelection(photo.id)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isInWidget) Color(0xFFFF9500) else Color(0x66000000))
                        ) {
                            Icon(
                                imageVector = if (isInWidget) Icons.Rounded.Widgets else Icons.Rounded.AddPhotoAlternate,
                                contentDescription = "Widget",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                try {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        if (photo.imageUri != null) {
                                            putExtra(Intent.EXTRA_STREAM, Uri.parse(photo.imageUri))
                                            type = "image/jpeg"
                                        } else {
                                            putExtra(Intent.EXTRA_TEXT, "Photo: ${photo.title}")
                                            type = "text/plain"
                                        }
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Photo"))
                                } catch (e: Exception) {
                                    // Handled
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000))
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosLibraryView(
    photos: List<PhotoItem>,
    onPhotoClick: (PhotoItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Section 1: Recent Days (Carousel)
        item {
            PhotosSectionHeader(title = "Recent Days")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(photos.take(8)) { photo ->
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(photo) }
                    ) {
                        if (photo.imageUri != null) {
                            AsyncImage(
                                model = photo.imageUri,
                                contentDescription = photo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = photo.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                                contentDescription = photo.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color(0x77000000))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(photo.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(photo.date, color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: All Photos Grid (3x3 matching iOS)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            PhotosSectionHeader(title = "All Photos (${photos.size})")
        }

        item {
            val chunked = photos.chunked(3)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chunked.forEach { rowPhotos ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowPhotos.forEach { photo ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onPhotoClick(photo) }
                            ) {
                                if (photo.imageUri != null) {
                                    AsyncImage(
                                        model = photo.imageUri,
                                        contentDescription = photo.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = photo.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                                        contentDescription = photo.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        // Fill remaining slots in last row if fewer than 3
                        repeat(3 - rowPhotos.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosForYouView(
    photos: List<PhotoItem>,
    onPhotoClick: (PhotoItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Memories", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Relive your favorite moments on iPhone SE 2", fontSize = 13.sp, color = IOSSystemGray)
        }

        items(photos.take(5)) { photo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { onPhotoClick(photo) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (photo.imageUri != null) {
                        AsyncImage(
                            model = photo.imageUri,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = photo.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color(0x88000000))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(photo.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Captured • ${photo.date}", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosAlbumsView(
    photos: List<PhotoItem>,
    onPhotoClick: (PhotoItem) -> Unit
) {
    val albums = listOf(
        Pair("Recents", photos.size),
        Pair("Favorites", (photos.size / 3).coerceAtLeast(1)),
        Pair("Camera Roll", photos.count { it.imageUri != null }),
        Pair("Screenshots", 4),
        Pair("Wallpapers", 6)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("My Albums", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        items(albums) { (albumName, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F2F7))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    ) {
                        val first = photos.firstOrNull()
                        if (first != null) {
                            if (first.imageUri != null) {
                                AsyncImage(
                                    model = first.imageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = first.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    Column {
                        Text(albumName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        Text("$count Photos", fontSize = 12.sp, color = IOSSystemGray)
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = IOSSystemGray
                )
            }
        }
    }
}

@Composable
private fun PhotosSearchView(
    photos: List<PhotoItem>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onPhotoClick: (PhotoItem) -> Unit
) {
    val filtered = remember(searchQuery, photos) {
        if (searchQuery.isBlank()) photos
        else photos.filter { it.title.contains(searchQuery, ignoreCase = true) || it.date.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("Search photos by title, date, or tag", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = IOSSystemGray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = IOSSystemGray)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF2F2F7),
                unfocusedContainerColor = Color(0xFFF2F2F7),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text("${filtered.size} Results", color = IOSSystemGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { photo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onPhotoClick(photo) }
                ) {
                    if (photo.imageUri != null) {
                        AsyncImage(
                            model = photo.imageUri,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = photo.drawableRes.takeIf { it != 0 } ?: R.drawable.photo_widget_nature),
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosSectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "See All",
            fontSize = 14.sp,
            color = IOSBlue,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PhotosBottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) IOSBlue else IOSSystemGray,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) IOSBlue else IOSSystemGray,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
