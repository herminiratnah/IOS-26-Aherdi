package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.IOSBlue
import com.example.ui.theme.IOSBorderGray
import com.example.ui.theme.IOSSystemGray6

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun IOSWebEngineView(
    initialUrl: String,
    title: String,
    modifier: Modifier = Modifier,
    showHeaderBar: Boolean = true,
    customHtml: String? = null,
    onOpenExternal: (() -> Unit)? = null
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var pageTitle by remember { mutableStateOf(title) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var hasError by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        if (showHeaderBar) {
            // Safari Live Web Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSSystemGray6)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        }
                    },
                    enabled = canGoBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) IOSBlue else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    },
                    enabled = canGoForward,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) IOSBlue else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // URL & Live Status capsule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE5E5EA))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLoading) "Connecting live..." else pageTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isLoading) Color(0xFFFF9500) else Color(0xFF34C759))
                        )
                    }
                }

                IconButton(
                    onClick = {
                        hasError = false
                        webViewInstance?.reload()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = "Reload",
                        tint = IOSBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onOpenExternal != null) {
                    IconButton(
                        onClick = onOpenExternal,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.OpenInBrowser,
                            contentDescription = "Safari Browser",
                            tint = IOSBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Loading Progress Bar
            AnimatedVisibility(visible = isLoading && progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = IOSBlue,
                    trackColor = Color.Transparent
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
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
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                                url?.let { currentUrl = it }
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                url?.let { currentUrl = it }
                                pageTitle = view?.title ?: title
                                canGoBack = canGoBack()
                                canGoForward = canGoForward()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    hasError = true
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }

                            override fun onReceivedTitle(view: WebView?, t: String?) {
                                if (!t.isNullOrBlank()) {
                                    pageTitle = t
                                }
                            }
                        }

                        if (customHtml != null) {
                            loadDataWithBaseURL("https://apple.com", customHtml, "text/html", "utf-8", null)
                        } else {
                            loadUrl(initialUrl)
                        }
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                },
                modifier = Modifier.fillMaxSize()
            )

            if (hasError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cannot Load Real-Time Web Data",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Check internet connection or Airplane Mode in Settings.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(IOSBlue)
                            .clickable {
                                hasError = false
                                webViewInstance?.reload()
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
