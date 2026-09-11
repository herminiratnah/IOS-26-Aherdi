package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

data class SafariTab(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "Start Page",
    var url: String = "about:blank"
)

data class QuickLink(
    val title: String,
    val url: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun SafariApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()

    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    val searchEngine = viewModel.getAppSetting("Safari_SearchEngine", "Google") as? String ?: "Google"
    val blockPopups = viewModel.getAppSetting("Safari_BlockPopups", true) as? Boolean ?: true

    // Tabs state
    var tabs by remember {
        mutableStateOf(listOf(SafariTab(title = "Start Page", url = "about:blank")))
    }
    var activeTabIndex by remember { mutableIntStateOf(0) }
    var isTabsOverviewOpen by remember { mutableStateOf(false) }
    var isBookmarksOpen by remember { mutableStateOf(false) }

    // History and Bookmarks
    var bookmarks by remember {
        mutableStateOf(
            listOf(
                "Apple" to "https://www.apple.com",
                "Google" to "https://www.google.com",
                "Wikipedia" to "https://www.wikipedia.org"
            )
        )
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("about:blank") }
    var urlInputText by remember { mutableStateOf("") }
    var isEditingUrl by remember { mutableStateOf(false) }
    var pageTitle by remember { mutableStateOf("Start Page") }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    fun resolveSearchUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "about:blank"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        val encoded = Uri.encode(trimmed)
        return when (searchEngine) {
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encoded"
            "Bing" -> "https://www.bing.com/search?q=$encoded"
            "Yahoo" -> "https://search.yahoo.com/search?p=$encoded"
            else -> "https://www.google.com/search?q=$encoded"
        }
    }

    fun loadUrlInActiveTab(url: String) {
        val finalUrl = resolveSearchUrl(url)
        currentUrl = finalUrl
        urlInputText = if (finalUrl == "about:blank") "" else finalUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/")
        isEditingUrl = false
        focusManager.clearFocus()
        if (activeTabIndex in tabs.indices) {
            tabs = tabs.toMutableList().also {
                it[activeTabIndex] = it[activeTabIndex].copy(url = finalUrl, title = if (finalUrl == "about:blank") "Start Page" else finalUrl)
            }
        }
        webViewInstance?.loadUrl(finalUrl)
    }

    val quickFavorites = listOf(
        QuickLink("Google", "https://www.google.com", Icons.Rounded.Search, Color(0xFF4285F4)),
        QuickLink("Apple", "https://www.apple.com", Icons.Rounded.PhoneIphone, Color(0xFF1C1C1E)),
        QuickLink("Wikipedia", "https://www.wikipedia.org", Icons.Rounded.MenuBook, Color(0xFF636366)),
        QuickLink("Detik News", "https://news.detik.com", Icons.Rounded.Article, Color(0xFF007AFF)),
        QuickLink("Kompas", "https://www.kompas.com", Icons.Rounded.Public, Color(0xFFFF9500)),
        QuickLink("YouTube", "https://m.youtube.com", Icons.Rounded.PlayArrow, Color(0xFFFF3B30)),
        QuickLink("GitHub", "https://github.com", Icons.Rounded.Code, Color(0xFF24292E)),
        QuickLink("Reddit", "https://www.reddit.com", Icons.Rounded.Forum, Color(0xFFFF4500))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
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
                isDarkIcons = true
            )

            // Top Safari Address / Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Address Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE5E5EA))
                            .clickable(enabled = !isEditingUrl) {
                                isEditingUrl = true
                                urlInputText = if (currentUrl == "about:blank") "" else currentUrl
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (isEditingUrl) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = IOSSystemGray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BasicTextField(
                                    value = urlInputText,
                                    onValueChange = { urlInputText = it },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            loadUrlInActiveTab(urlInputText)
                                        }
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (urlInputText.isEmpty()) {
                                            Text(
                                                "Search or enter website name",
                                                color = IOSSystemGray,
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (urlInputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { urlInputText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Cancel,
                                            contentDescription = "Clear",
                                            tint = IOSSystemGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "aA",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    if (currentUrl.startsWith("https://")) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Secure",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Text(
                                        text = if (currentUrl == "about:blank") "Search or enter website" else (pageTitle.ifBlank { urlInputText }),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (currentUrl == "about:blank") IOSSystemGray else Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = if (isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            if (isLoading) {
                                                webViewInstance?.stopLoading()
                                            } else {
                                                if (currentUrl == "about:blank") {
                                                    loadUrlInActiveTab("https://www.google.com")
                                                } else {
                                                    webViewInstance?.reload()
                                                }
                                            }
                                        }
                                )
                            }
                        }
                    }

                    if (isEditingUrl) {
                        Text(
                            text = "Cancel",
                            color = IOSBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                isEditingUrl = false
                                focusManager.clearFocus()
                            }
                        )
                    }
                }

                // Loading progress indicator
                if (isLoading && pageProgress in 0.01f..0.99f) {
                    LinearProgressIndicator(
                        progress = { pageProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.BottomCenter),
                        color = IOSBlue,
                        trackColor = Color.Transparent
                    )
                }
            }

            // Main Web View or Start Page
            Box(modifier = Modifier.weight(1f)) {
                if (currentUrl == "about:blank" && !isEditingUrl) {
                    // Start Page (Favorites & Privacy Report)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Favorites",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                userScrollEnabled = false
                            ) {
                                items(quickFavorites) { fav ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            loadUrlInActiveTab(fav.url)
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(fav.color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = fav.icon,
                                                contentDescription = fav.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = fav.title,
                                            fontSize = 11.sp,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Privacy report card
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shield,
                                            contentDescription = null,
                                            tint = IOSBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Privacy Report",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "In the last 30 days, Safari has prevented 58 trackers from profiling you. Search Engine: $searchEngine. Intelligent Tracking Prevention is active.",
                                        fontSize = 13.sp,
                                        color = IOSSystemGray,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // Reading list item
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Frequently Visited",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    bookmarks.forEach { (name, link) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { loadUrlInActiveTab(link) }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Bookmark,
                                                contentDescription = null,
                                                tint = IOSBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                                Text(link, color = IOSSystemGray, fontSize = 12.sp)
                                            }
                                        }
                                        HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                } else {
                    // Full-fledged Android WebView
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadsImagesAutomatically = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1"
                                    if (blockPopups) {
                                        javaScriptCanOpenWindowsAutomatically = false
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        url?.let {
                                            currentUrl = it
                                            urlInputText = it.removePrefix("https://").removePrefix("http://").removeSuffix("/")
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                        pageTitle = view?.title ?: (url ?: "")
                                        url?.let {
                                            currentUrl = it
                                            urlInputText = it.removePrefix("https://").removePrefix("http://").removeSuffix("/")
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        isLoading = false
                                    }
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageProgress = newProgress / 100f
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        title?.let { pageTitle = it }
                                    }
                                }
                                if (currentUrl != "about:blank") {
                                    loadUrl(currentUrl)
                                }
                                webViewInstance = this
                            }
                        },
                        update = { view ->
                            webViewInstance = view
                        }
                    )
                }

                // Tabs Overview Sheet
                if (isTabsOverviewOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xCC000000))
                            .clickable { isTabsOverviewOpen = false }
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tabs.size} Tabs",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                IconButton(
                                    onClick = {
                                        val newTab = SafariTab(title = "Start Page", url = "about:blank")
                                        tabs = tabs + newTab
                                        activeTabIndex = tabs.size - 1
                                        currentUrl = "about:blank"
                                        pageTitle = "Start Page"
                                        urlInputText = ""
                                        isTabsOverviewOpen = false
                                    }
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "New Tab", tint = IOSBlue)
                                }
                            }
                            HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            tabs.forEachIndexed { idx, tab ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (idx == activeTabIndex) IOSSystemGray5 else Color.Transparent)
                                        .clickable {
                                            activeTabIndex = idx
                                            currentUrl = tab.url
                                            pageTitle = tab.title
                                            urlInputText = if (tab.url == "about:blank") "" else tab.url
                                            if (tab.url != "about:blank") {
                                                webViewInstance?.loadUrl(tab.url)
                                            }
                                            isTabsOverviewOpen = false
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tab.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                                        Text(tab.url, fontSize = 12.sp, color = IOSSystemGray, maxLines = 1)
                                    }
                                    if (tabs.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val mutable = tabs.toMutableList()
                                                mutable.removeAt(idx)
                                                tabs = mutable
                                                if (activeTabIndex >= tabs.size) {
                                                    activeTabIndex = tabs.size - 1
                                                }
                                                currentUrl = tabs[activeTabIndex].url
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { isTabsOverviewOpen = false },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Bookmarks Modal
                if (isBookmarksOpen) {
                    AlertDialog(
                        onDismissRequest = { isBookmarksOpen = false },
                        title = { Text("Bookmarks & Reading List", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                bookmarks.forEach { (name, link) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                loadUrlInActiveTab(link)
                                                isBookmarksOpen = false
                                            }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = IOSBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                            Text(link, color = IOSSystemGray, fontSize = 12.sp)
                                        }
                                    }
                                    HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (currentUrl != "about:blank") {
                                            bookmarks = bookmarks + (pageTitle to currentUrl)
                                            viewModel.showDynamicIslandNotification("Safari", "Bookmark ditambahkan", "bookmark", 0xFF007AFF)
                                        }
                                        isBookmarksOpen = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Add Current Page to Bookmarks", color = Color.White)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { isBookmarksOpen = false }) {
                                Text("Close", color = IOSBlue)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Color.White
                    )
                }
            }

            // Bottom Safari Toolbar matching authentic iOS 18
            HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back
                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        } else {
                            currentUrl = "about:blank"
                            pageTitle = "Start Page"
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack || currentUrl != "about:blank") IOSBlue else IOSSystemGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Forward
                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) IOSBlue else IOSSystemGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share
                IconButton(
                    onClick = {
                        if (currentUrl != "about:blank") {
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, currentUrl)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Share $pageTitle")
                                context.startActivity(shareIntent)
                            } catch (e: Exception) {
                                viewModel.showDynamicIslandNotification("Safari", "Tautan disalin: $currentUrl", "safari", 0xFF007AFF)
                            }
                        } else {
                            viewModel.showDynamicIslandNotification("Safari", "Buka halaman untuk membagikan", "safari", 0xFF007AFF)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = IOSBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Bookmarks
                IconButton(
                    onClick = { isBookmarksOpen = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = "Bookmarks",
                        tint = IOSBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Tabs
                IconButton(
                    onClick = { isTabsOverviewOpen = !isTabsOverviewOpen },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.FilterNone,
                            contentDescription = "Tabs",
                            tint = IOSBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${tabs.size}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IOSBlue
                        )
                    }
                }
            }

            // Home indicator bar
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }
    }
}
