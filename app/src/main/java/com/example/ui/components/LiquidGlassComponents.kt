package com.example.ui.components

import com.example.model.AppId
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.R
import com.example.model.PhotoItem
import com.example.model.WeatherData
import com.example.model.IOSNotificationItem
import com.example.util.IOSFeedbackHelper
import com.example.util.AppWidgetHelper
import androidx.compose.ui.viewinterop.AndroidView

/**
 * iOS 26 Liquid Glass Colors & Reflections
 */
val LiquidGlassWhiteHighlight = Color.White.copy(alpha = 0.55f)
val LiquidGlassWhiteMid = Color.White.copy(alpha = 0.18f)
val LiquidGlassWhiteEdge = Color.White.copy(alpha = 0.35f)

val LiquidGlassBorderBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.65f),
        Color.White.copy(alpha = 0.18f),
        Color.White.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.38f)
    ),
    start = Offset(0f, 0f),
    end = Offset(400f, 400f)
)

val LiquidGlassGlossBrush = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.42f),
        Color.White.copy(alpha = 0.14f),
        Color.Transparent,
        Color.White.copy(alpha = 0.06f)
    ),
    start = Offset(0f, 0f),
    end = Offset(150f, 250f)
)

/**
 * High-end Liquid Glass Container for Icons
 * Gives app icons the translucent, refraction-layered, refractive squircle aesthetic of iOS 26.
 */
@Composable
fun LiquidGlassIconBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(15.dp),
    elevation: Dp = 5.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.88f),
                        backgroundColor.copy(alpha = 0.78f)
                    )
                )
            )
            .border(
                BorderStroke(
                    width = 1.2.dp,
                    brush = LiquidGlassBorderBrush
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Main App Icon Content
        content()

        // Top Specular Reflection Arc
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.72f)
                .height(14.dp)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Subtle specular diagonal gloss overlay (Liquid Glass Shine)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LiquidGlassGlossBrush)
        )
    }
}

/**
 * Liquid Glass Container specifically for Third-Party Apps
 * Embeds installed Android apps (WhatsApp, YouTube, Instagram, etc.) into an authentic iOS 26 Liquid Glass crystal.
 */
@Composable
fun LiquidGlassThirdPartyIcon(
    bitmap: Bitmap?,
    appName: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1E293B),
    size: Dp = 58.dp
) {
    val cornerRadius = size * 0.26f
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation = 5.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.88f),
                        backgroundColor.copy(alpha = 0.70f)
                    )
                )
            )
            .border(
                BorderStroke(1.2.dp, LiquidGlassBorderBrush),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner Glass Floating Capsule
        Box(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .clip(RoundedCornerShape(cornerRadius * 0.75f))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)),
                    RoundedCornerShape(cornerRadius * 0.75f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = appName,
                    modifier = Modifier
                        .fillMaxSize(0.88f)
                        .clip(RoundedCornerShape(cornerRadius * 0.55f)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = appName,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        }

        // Top Specular Reflection Arc
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(0.72f)
                .height(size * 0.24f)
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Diagonal liquid gloss reflection overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LiquidGlassGlossBrush)
        )
    }
}

/**
 * Authentic iOS 26 Liquid Glass Built-In App Icon Pack
 * Renders each iOS system app with high-fashion, refraction-layered, precision-crafted styling.
 */
@Composable
fun IOS26BuiltInAppIcon(
    appId: AppId,
    appName: String = "",
    iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    dayOfWeek: String = "MON",
    dayOfMonth: String = "28",
    hourFloat: Float = 9f,
    minuteFloat: Float = 41f,
    secondFloat: Float = 0f,
    size: Dp = 60.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (appId) {
            AppId.CALENDAR -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(size * 0.28f)
                            .background(Color(0xFFFF3B30)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfWeek,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayOfMonth,
                            color = Color(0xFF1C1C1E),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            AppId.PHOTOS -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(size * 0.65f)) {
                        val petalColors = listOf(
                            Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
                            Color(0xFF34C759), Color(0xFF007AFF), Color(0xFFAF52DE)
                        )
                        petalColors.forEachIndexed { i, c ->
                            drawCircle(
                                color = c.copy(alpha = 0.88f),
                                radius = this.size.minDimension / 4.0f,
                                center = Offset(
                                    this.size.width / 2 + (this.size.width / 5f) * kotlin.math.cos(i * kotlin.math.PI.toFloat() / 3f),
                                    this.size.height / 2 + (this.size.height / 5f) * kotlin.math.sin(i * kotlin.math.PI.toFloat() / 3f)
                                )
                            )
                        }
                    }
                }
            }

            AppId.NOTES -> {
                Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    Box(modifier = Modifier.fillMaxWidth().height(size * 0.24f).background(Color(0xFFFFCC00)))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp, vertical = 5.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.5.dp)) {
                            repeat(3) {
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE5E5EA)))
                            }
                        }
                    }
                }
            }

            AppId.REMINDERS -> {
                Column(
                    modifier = Modifier.fillMaxSize().background(Color.White).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF007AFF)))
                        Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color.LightGray))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFFF9500)))
                        Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color.LightGray))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34C759)))
                        Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color.LightGray))
                    }
                }
            }

            AppId.CLOCK -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(size * 0.72f)) {
                        drawCircle(color = Color.White, radius = 2.dp.toPx())

                        val hourAngle = (hourFloat % 12f) * 30f - 90f
                        val hourRad = Math.toRadians(hourAngle.toDouble())
                        val hourLen = this.size.minDimension * 0.28f
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(
                                (center.x + hourLen * kotlin.math.cos(hourRad)).toFloat(),
                                (center.y + hourLen * kotlin.math.sin(hourRad)).toFloat()
                            ),
                            strokeWidth = 2.5f
                        )

                        val minAngle = (minuteFloat % 60f) * 6f - 90f
                        val minRad = Math.toRadians(minAngle.toDouble())
                        val minLen = this.size.minDimension * 0.40f
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(
                                (center.x + minLen * kotlin.math.cos(minRad)).toFloat(),
                                (center.y + minLen * kotlin.math.sin(minRad)).toFloat()
                            ),
                            strokeWidth = 2f
                        )

                        val secAngle = (secondFloat % 60f) * 6f - 90f
                        val secRad = Math.toRadians(secAngle.toDouble())
                        val secLen = this.size.minDimension * 0.45f
                        drawLine(
                            color = Color(0xFFFF9500),
                            start = center,
                            end = Offset(
                                (center.x + secLen * kotlin.math.cos(secRad)).toFloat(),
                                (center.y + secLen * kotlin.math.sin(secRad)).toFloat()
                            ),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }

            AppId.SETTINGS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8E8E93), Color(0xFF636366), Color(0xFF3A3A3C))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFF2F2F7),
                        modifier = Modifier.size(size * 0.58f)
                    )
                }
            }

            AppId.CAMERA -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF48484A), Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.62f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))
                                )
                            )
                            .border(BorderStroke(1.2.dp, Color(0xFF8E8E93)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(size * 0.36f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD60A))
                    )
                }
            }

            AppId.WEATHER -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1D6EEB), Color(0xFF32ADE6), Color(0xFF64D2FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = "Weather",
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 7.dp, end = 7.dp)
                            .size(size * 0.34f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 6.dp, start = 6.dp)
                            .size(size * 0.52f)
                    )
                }
            }

            AppId.MAPS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF70C988), Color(0xFFE5E5EA), Color(0xFF5AC8FA))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = "Maps",
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            AppId.MESSAGES -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF34C759), Color(0xFF28CD41))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubble,
                        contentDescription = "Messages",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.PHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF34C759), Color(0xFF248A3D))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Phone",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.MUSIC -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFA233B), Color(0xFFFB5C74))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "Music",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.56f)
                    )
                }
            }

            AppId.MAIL -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF007AFF), Color(0xFF0A84FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = "Mail",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.CALCULATOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Calculate,
                        contentDescription = "Calculator",
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            AppId.HEALTH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Health",
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(size * 0.54f)
                    )
                }
            }

            AppId.WALLET -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color(0xFFFFD60A),
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.APP_STORE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF007AFF), Color(0xFF0051B3))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBag,
                        contentDescription = "App Store",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.PODCASTS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF8E44AD), Color(0xFF5856D6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Podcasts,
                        contentDescription = "Podcasts",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.TV -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "TV",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.FACETIME -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF34C759), Color(0xFF30D158))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Videocam,
                        contentDescription = "FaceTime",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.STOCKS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = "Stocks",
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }

            AppId.BOOKS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF9500), Color(0xFFFF5E3A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = "Books",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.ROOT_TOOLS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF5856D6), Color(0xFF3634A3))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "iRoot SE",
                        tint = Color(0xFF64D2FF),
                        modifier = Modifier.size(size * 0.54f)
                    )
                }
            }

            AppId.DYNAMIC_ISLAND -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(size * 0.54f)
                            .height(size * 0.24f)
                            .clip(RoundedCornerShape(size * 0.12f))
                            .background(Color.White)
                    )
                }
            }

            AppId.DEPTH_STUDIO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF5856D6), Color(0xFF007AFF), Color(0xFFFF2D55))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = "Depth Studio",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            AppId.SAFARI -> {
                Box(
                    modifier = Modifier
                        .size(size * 0.74f)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF32ADE6), Color(0xFF007AFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Explore,
                        contentDescription = "Safari",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }

            else -> {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = appName,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.50f)
                    )
                }
            }
        }
    }
}

/**
 * High-end Liquid Glass Container for Widgets
 */
@Composable
fun LiquidGlassWidgetCard(
    modifier: Modifier = Modifier,
    backgroundBrush: Brush = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.14f),
            Color.White.copy(alpha = 0.22f)
        )
    ),
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(shape)
            .background(backgroundBrush)
            .border(
                BorderStroke(1.2.dp, LiquidGlassBorderBrush),
                shape = shape
            )
    ) {
        content()

        // Diagonal liquid gloss reflection
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(300f, 300f)
                    )
                )
        )
    }
}

/**
 * Liquid Glass Weather Widget
 */
@Composable
fun LiquidGlassWeatherWidget(
    weatherData: WeatherData,
    isJiggleMode: Boolean,
    onCycleWidget: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier
            .clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xDD2472A4),
                Color(0xBB4A90E2),
                Color(0x9950E3C2)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with City and Cycle Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = weatherData.city,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${weatherData.currentTemp}°",
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                }

                // Cycle Widget Pill in Jiggle Mode
                if (isJiggleMode) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                            .clickable { onCycleWidget() }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = "Ganti Widget",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Ubah",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Bottom Condition & High/Low
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = weatherData.condition,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "H:${weatherData.highTemp}° L:${weatherData.lowTemp}°",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            weatherData.weatherCode in 61..82 -> Icons.Rounded.WaterDrop
                            weatherData.weatherCode == 0 -> Icons.Rounded.WbSunny
                            else -> Icons.Rounded.WbCloudy
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Liquid Glass Photos Widget
 */
@Composable
fun LiquidGlassPhotosWidget(
    currentPhoto: PhotoItem?,
    isJiggleMode: Boolean,
    onNextPhoto: () -> Unit,
    onPickPhotos: () -> Unit,
    onCycleWidget: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() }
    ) {
        // Image
        if (currentPhoto?.imageUri != null) {
            AsyncImage(
                model = currentPhoto.imageUri,
                contentDescription = "Photos Widget",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (currentPhoto?.drawableRes != null && currentPhoto.drawableRes > 0) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = currentPhoto.drawableRes),
                contentDescription = "Photos Widget",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.photo_widget_nature),
                contentDescription = "Photos Widget",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top right glass pills for controls
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isJiggleMode) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { onCycleWidget() }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Autorenew,
                            contentDescription = "Ganti Widget",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text("Ubah", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onNextPhoto() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Photo",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onPickPhotos() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Select Photos",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom frosted label bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Column {
                Text(
                    text = currentPhoto?.title ?: "Memories",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentPhoto?.date ?: "Galeri Foto",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

/**
 * Liquid Glass Calendar Widget
 */
@Composable
fun LiquidGlassCalendarWidget(
    dayOfWeek: String,
    dayOfMonth: String,
    isJiggleMode: Boolean,
    onCycleWidget: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.88f),
                Color.White.copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.82f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayOfWeek.uppercase(),
                    color = Color(0xFFFF3B30),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                if (isJiggleMode) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.15f))
                            .clickable { onCycleWidget() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = "Ganti Widget",
                                tint = Color.Black.copy(alpha = 0.8f),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Ubah",
                                color = Color.Black.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = dayOfMonth,
                color = Color.Black,
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-1).sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF))
                )
                Text(
                    text = "Hari ini • Tidak ada agenda",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Liquid Glass Battery Widget
 */
@Composable
fun LiquidGlassBatteryWidget(
    batteryLevel: Int,
    isCharging: Boolean,
    isJiggleMode: Boolean,
    onCycleWidget: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xDD1C1C1E),
                Color(0xCC2C2C2E),
                Color(0xDD1C1C1E)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                        contentDescription = "Battery",
                        tint = if (isCharging) Color(0xFF34C759) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "iPhone",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isJiggleMode) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { onCycleWidget() }
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = "Ganti Widget",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Ubah",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = "$batteryLevel%",
                color = if (isCharging) Color(0xFF34C759) else Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light
            )

            // Battery bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (batteryLevel / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isCharging) Color(0xFF34C759)
                            else if (batteryLevel <= 20) Color(0xFFFF3B30)
                            else Color.White
                        )
                )
            }
        }
    }
}

/**
 * iOS 26 Dynamic Widget Controls & Position Switcher
 * Allows user to dynamically move widgets to Top, Middle, or Bottom, or swap them.
 */
@Composable
fun LiquidGlassWidgetPositionBar(
    currentPosition: Int,
    onSetPosition: (Int) -> Unit,
    onSwap: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.92f),
                        Color.White.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                BorderStroke(1.dp, LiquidGlassBorderBrush),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Swap button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                    .clickable { onSwap() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Tukar",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Tukar",
                        color = Color(0xFF007AFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.Gray.copy(alpha = 0.3f))
            )

            // Position Pills: Atas | Tengah | Bawah
            val positions = listOf(
                0 to "Atas",
                1 to "Tengah",
                2 to "Bawah"
            )
            positions.forEach { (pos, label) ->
                val isSelected = currentPosition == pos || (pos == 2 && currentPosition >= 2)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color(0xFF007AFF)
                            else Color.Transparent
                        )
                        .clickable { onSetPosition(pos) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }

            // Quick Up / Down arrows
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (currentPosition > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                            .clickable { onMoveUp() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Naik",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (currentPosition < 2) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                            .clickable { onMoveDown() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Turun",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Liquid Glass Widget Controls (Shown in Jiggle Mode)
 * Allows moving this specific widget up/down, toggling size, or opening widget gallery.
 */
@Composable
fun LiquidGlassIndividualWidgetControls(
    widgetIndex: Int,
    widgetTitle: String,
    currentSize: String,
    currentRow: Int,
    onOpenGallery: () -> Unit,
    onToggleSize: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetRow: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.94f),
                        Color.White.copy(alpha = 0.88f)
                    )
                )
            )
            .border(BorderStroke(1.dp, LiquidGlassBorderBrush), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Widget Title & Change button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF007AFF).copy(alpha = 0.14f))
                    .clickable { onOpenGallery() }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = "Ganti",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Ubah: $widgetTitle",
                        color = Color(0xFF007AFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Size Toggle (2x2 vs 4x2)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF34C759).copy(alpha = 0.16f))
                    .clickable { onToggleSize() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (currentSize == "MEDIUM") Icons.Rounded.AspectRatio else Icons.Rounded.CropSquare,
                        contentDescription = "Ukuran",
                        tint = Color(0xFF28A745),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (currentSize == "MEDIUM") "Sedang 4x2" else "Kecil 2x2",
                        color = Color(0xFF28A745),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Position Pills: Atas | Tengah | Bawah
            listOf(0 to "Atas", 1 to "Tengah", 2 to "Bawah").forEach { (row, label) ->
                val isSelected = (currentRow == row || (row == 2 && currentRow >= 2))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF007AFF) else Color.Transparent)
                        .clickable { onSetRow(row) }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.DarkGray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Up / Down arrows
            if (currentRow > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                        .clickable { onMoveUp() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Naik", tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                }
            }
            if (currentRow < 2) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF007AFF).copy(alpha = 0.12f))
                        .clickable { onMoveDown() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Turun", tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Liquid Glass Clock Widget (Jam Dunia & Analog)
 */
@Composable
fun LiquidGlassClockWidget(
    hour: Float,
    minute: Float,
    second: Float,
    isMedium: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            listOf(
                Color(0xDD0F172A),
                Color(0xBB1E293B),
                Color(0x99334155)
            )
        )
    ) {
        if (!isMedium) {
            // Small Clock (2x2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JAKARTA",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "WIB",
                        color = Color(0xFF0A84FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Analog Clock Dial in Canvas
                Box(
                    modifier = Modifier
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2.2f

                        // Outer ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = radius,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 12 hour ticks
                        for (i in 0 until 12) {
                            val angle = (i * 30.0) * Math.PI / 180.0
                            val startX = center.x + (radius - 5.dp.toPx()) * Math.sin(angle).toFloat()
                            val startY = center.y - (radius - 5.dp.toPx()) * Math.cos(angle).toFloat()
                            val endX = center.x + radius * Math.sin(angle).toFloat()
                            val endY = center.y - radius * Math.cos(angle).toFloat()
                            drawLine(
                                color = if (i % 3 == 0) Color.White else Color.White.copy(alpha = 0.4f),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        // Hour hand
                        val hourAngle = ((hour % 12 + minute / 60f) * 30.0) * Math.PI / 180.0
                        val hourX = center.x + (radius * 0.5f) * Math.sin(hourAngle).toFloat()
                        val hourY = center.y - (radius * 0.5f) * Math.cos(hourAngle).toFloat()
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(hourX, hourY),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Minute hand
                        val minAngle = (minute * 6.0) * Math.PI / 180.0
                        val minX = center.x + (radius * 0.72f) * Math.sin(minAngle).toFloat()
                        val minY = center.y - (radius * 0.72f) * Math.cos(minAngle).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.9f),
                            start = center,
                            end = Offset(minX, minY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Second hand (Orange iOS)
                        val secAngle = (second * 6.0) * Math.PI / 180.0
                        val secX = center.x + (radius * 0.85f) * Math.sin(secAngle).toFloat()
                        val secY = center.y - (radius * 0.85f) * Math.cos(secAngle).toFloat()
                        drawLine(
                            color = Color(0xFFFF9500),
                            start = center,
                            end = Offset(secX, secY),
                            strokeWidth = 1.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Center pin
                        drawCircle(color = Color(0xFFFF9500), radius = 2.5.dp.toPx())
                    }
                }

                Text(
                    text = "${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Medium Clock (4x2) Dual Cities
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jakarta
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("JAKARTA", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${hour.toInt().toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                    Text("Hari Ini, +0JAM", color = Color(0xFF0A84FF), fontSize = 11.sp)
                }

                Box(modifier = Modifier.width(1.dp).height(80.dp).background(Color.White.copy(alpha = 0.15f)))

                // Cupertino
                val cupHour = ((hour.toInt() - 14 + 24) % 24)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CUPERTINO", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${cupHour.toString().padStart(2, '0')}:${minute.toInt().toString().padStart(2, '0')}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                    Text("Kemarin, -14JAM", color = Color(0xFFFF9500), fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * Liquid Glass Music / Now Playing Widget
 */
@Composable
fun LiquidGlassMusicWidget(
    songTitle: String = "Starlight Symphony",
    artistName: String = "Liquid Dreams",
    isPlaying: Boolean = true,
    isMedium: Boolean = false,
    onTogglePlay: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            listOf(
                Color(0xDD7928CA),
                Color(0xBB4338CA),
                Color(0x992563EB)
            )
        )
    ) {
        if (!isMedium) {
            // Small Music Widget (2x2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(13.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(BorderStroke(1.dp, LiquidGlassBorderBrush), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = "Musik", tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = songTitle,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Waveform indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val heights = listOf(8.dp, 16.dp, 12.dp, 20.dp, 10.dp, 14.dp, 6.dp)
                    heights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(if (isPlaying) h else 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }
                }
            }
        } else {
            // Medium Music Widget (4x2)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .border(BorderStroke(1.2.dp, LiquidGlassBorderBrush), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Album, contentDescription = "Album", tint = Color.White, modifier = Modifier.size(46.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(songTitle, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(artistName, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, maxLines = 1)

                    // Track bar
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f))) {
                        Box(modifier = Modifier.fillMaxWidth(0.55f).fillMaxHeight().background(Color.White))
                    }

                    // Playback controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(24.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.35f))
                                .clickable { onTogglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass Fitness / Activity Widget
 */
@Composable
fun LiquidGlassFitnessWidget(
    isMedium: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            listOf(
                Color(0xDD000000),
                Color(0xBB18181B),
                Color(0x9927272A)
            )
        )
    ) {
        if (!isMedium) {
            // Small Fitness (2x2)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("AKTIVITAS", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Rounded.DirectionsRun, contentDescription = "Fitness", tint = Color(0xFFFF2D55), modifier = Modifier.size(14.dp))
                }

                // 3 Concentric Activity Rings
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)

                        // Outer ring: Move (Red) 75%
                        drawArc(
                            color = Color(0xFFFF2D55).copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - 34.dp.toPx(), center.y - 34.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 68.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFFFF2D55),
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - 34.dp.toPx(), center.y - 34.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(68.dp.toPx(), 68.dp.toPx())
                        )

                        // Middle ring: Exercise (Lime) 60%
                        drawArc(
                            color = Color(0xFF5856D6).copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - 24.dp.toPx(), center.y - 24.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(48.dp.toPx(), 48.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF34C759),
                            startAngle = -90f,
                            sweepAngle = 220f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - 24.dp.toPx(), center.y - 24.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(48.dp.toPx(), 48.dp.toPx())
                        )

                        // Inner ring: Stand (Cyan) 80%
                        drawArc(
                            color = Color(0xFF007AFF).copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - 14.dp.toPx(), center.y - 14.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(28.dp.toPx(), 28.dp.toPx())
                        )
                        drawArc(
                            color = Color(0xFF007AFF),
                            startAngle = -90f,
                            sweepAngle = 290f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - 14.dp.toPx(), center.y - 14.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(28.dp.toPx(), 28.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Text("540 kcal", color = Color(0xFFFF2D55), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("28 min", color = Color(0xFF34C759), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Medium Fitness (4x2)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rings on left
                Box(modifier = Modifier.size(95.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        drawArc(color = Color(0xFFFF2D55), startAngle = -90f, sweepAngle = 270f, useCenter = false, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round), topLeft = Offset(center.x - 42.dp.toPx(), center.y - 42.dp.toPx()), size = androidx.compose.ui.geometry.Size(84.dp.toPx(), 84.dp.toPx()))
                        drawArc(color = Color(0xFF34C759), startAngle = -90f, sweepAngle = 220f, useCenter = false, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round), topLeft = Offset(center.x - 30.dp.toPx(), center.y - 30.dp.toPx()), size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 60.dp.toPx()))
                        drawArc(color = Color(0xFF007AFF), startAngle = -90f, sweepAngle = 290f, useCenter = false, style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round), topLeft = Offset(center.x - 18.dp.toPx(), center.y - 18.dp.toPx()), size = androidx.compose.ui.geometry.Size(36.dp.toPx(), 36.dp.toPx()))
                    }
                }

                // Stats on right
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF2D55)))
                        Text("Bergerak: 540 / 600 kkal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34C759)))
                        Text("Latihan: 28 / 30 mnt", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF007AFF)))
                        Text("Berdiri: 10 / 12 jam", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Liquid Glass Notes / Catatan Cepat Widget
 */
@Composable
fun LiquidGlassNotesWidget(
    isMedium: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LiquidGlassWidgetCard(
        modifier = modifier.clickable { onClick() },
        backgroundBrush = Brush.linearGradient(
            listOf(
                Color(0xEEF59E0B),
                Color(0xDDFBbf24),
                Color(0xBBFDE047)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.EditNote, contentDescription = "Notes", tint = Color(0xFF78350F), modifier = Modifier.size(18.dp))
                    Text("Catatan", color = Color(0xFF78350F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text("Hari ini", color = Color(0xFF78350F).copy(alpha = 0.7f), fontSize = 10.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "💡 Ide Desain Liquid Glass",
                    color = Color(0xFF451A03),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Efek transparansi iOS 26 dengan rim refleksi ganda & specular sheen.",
                    color = Color(0xFF78350F),
                    fontSize = 11.sp,
                    maxLines = if (isMedium) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(4) {
                    Box(modifier = Modifier.width(if (isMedium) 45.dp else 22.dp).height(2.dp).background(Color(0xFF78350F).copy(alpha = 0.35f)))
                }
            }
        }
    }
}

/**
 * Liquid Glass Widget Gallery Dialog
 * Allows selecting from all 8 authentic iOS 26 Liquid Glass widgets and toggling Small / Medium.
 */
@Composable
fun LiquidGlassWidgetGalleryDialog(
    widgetIndex: Int,
    currentType: String,
    currentSize: String,
    onSelectType: (String) -> Unit,
    onSelectSize: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val widgetOptions = listOf(
        Triple("WHATSAPP", "WhatsApp", "Pesan terbaru, pengirim & indikator belum dibaca"),
        Triple("WEATHER", "Cuaca", "Perkiraan suhu, kondisi real-time & kota"),
        Triple("PHOTOS", "Foto", "Galeri foto memori & slide album dinamis"),
        Triple("CALENDAR", "Kalender", "Hari, tanggal & agenda pertemuan berikutnya"),
        Triple("BATTERY", "Baterai", "Status daya iPhone & perangkat terhubung"),
        Triple("CLOCK", "Jam Dunia", "Jam analog presisi & perbedaan zona waktu"),
        Triple("MUSIC", "Musik", "Pemutar Now Playing, visualizer & kontrol"),
        Triple("FITNESS", "Aktivitas", "Cincin bergerak, latihan, dan berdiri harian"),
        Triple("NOTES", "Catatan Cepat", "Memo penting & catatan ide dalam gelas madu")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF21C1C1E),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Galeri Widget Liquid Glass",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Tutup", color = Color(0xFF0A84FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Pilih widget untuk Slot $widgetIndex dan atur ukurannya",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Size selection toggle (Kecil 2x2, Sedang 4x2, Ringkas 2x1, Besar 4x4)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val sizes = listOf(
                        "SMALL" to "2x2",
                        "MEDIUM" to "4x2",
                        "COMPACT" to "2x1",
                        "LARGE" to "4x4"
                    )
                    sizes.forEach { (sz, label) ->
                        val isSelected = currentSize == sz
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF0A84FF) else Color.Transparent)
                                .clickable { onSelectSize(sz) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // List of Widgets
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(widgetOptions) { (typeKey, title, desc) ->
                        val isSelected = currentType == typeKey
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.15f)
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSelectType(typeKey)
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val icon = when (typeKey) {
                                    "WHATSAPP" -> Icons.Rounded.Chat
                                    "WEATHER" -> Icons.Rounded.CloudQueue
                                    "PHOTOS" -> Icons.Rounded.PhotoCameraBack
                                    "CALENDAR" -> Icons.Rounded.CalendarToday
                                    "BATTERY" -> Icons.Rounded.BatteryChargingFull
                                    "CLOCK" -> Icons.Rounded.AccessTime
                                    "MUSIC" -> Icons.Rounded.MusicNote
                                    "FITNESS" -> Icons.Rounded.DirectionsRun
                                    else -> Icons.Rounded.EditNote
                                }
                                val iconBg = if (typeKey == "WHATSAPP") Color(0xFF25D366) else Color.White.copy(alpha = 0.15f)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(iconBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(22.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(text = desc, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                if (isSelected) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Terpilih", tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai", color = Color(0xFF0A84FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    )
}

/**
 * Authentic WhatsApp Widget from the device (Real Android AppWidget or Real Device Notifications, Zero Placeholder)
 */
@Composable
fun LiquidGlassWhatsAppWidget(
    isJiggleMode: Boolean = false,
    size: String = "SMALL", // "SMALL" (2x2), "MEDIUM" (4x2), "COMPACT" (2x1), "LARGE" (4x4)
    notifications: List<IOSNotificationItem> = emptyList(),
    unreadCount: Int = 0,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInstalled = remember(context) { AppWidgetHelper.isWhatsAppInstalled(context) }
    val nativeWidgetPair = remember(context, isJiggleMode) { AppWidgetHelper.getOrCreateWhatsAppWidgetView(context) }
    val nativeView = nativeWidgetPair.second

    val backgroundBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF1F2C34),
            Color(0xFF121B22)
        )
    )

    LiquidGlassWidgetCard(
        modifier = modifier.clickable {
            IOSFeedbackHelper.vibrateLight(context)
            onClick()
        },
        backgroundBrush = backgroundBrush
    ) {
        if (nativeView != null && !isJiggleMode) {
            // Authentic Native Android WhatsApp Widget Host
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                AndroidView(
                    factory = {
                        (nativeView.parent as? android.view.ViewGroup)?.removeView(nativeView)
                        nativeView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Real Device WhatsApp Data
            val whatsAppNotifs = remember(notifications) {
                notifications.filter { it.packageName.contains("whatsapp", ignoreCase = true) }
            }
            val realCount = if (whatsAppNotifs.isNotEmpty()) whatsAppNotifs.size else unreadCount
            val latestNotif = whatsAppNotifs.firstOrNull()
            val secondNotif = whatsAppNotifs.getOrNull(1)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row: Real WhatsApp Icon + "WhatsApp" + Real Unread Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Chat,
                                contentDescription = "WhatsApp",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "WhatsApp",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (realCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF25D366))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$realCount",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isInstalled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                color = Color(0xFF25D366),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Body: Real WhatsApp message preview from device
                if (!isInstalled) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "WhatsApp Belum Terpasang",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Pasang WhatsApp dari Play Store untuk widget asli.",
                            color = Color(0xFF8696A0),
                            fontSize = 11.sp
                        )
                    }
                } else if (latestNotif != null) {
                    when (size) {
                        "COMPACT" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = latestNotif.title.ifBlank { "Pesan Baru" },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = latestNotif.message.ifBlank { "Foto / Lampiran" },
                                        color = Color(0xFF8696A0),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = latestNotif.timestamp,
                                    color = Color(0xFF8696A0),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        "SMALL" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2A3942)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = latestNotif.title.take(1).uppercase().ifBlank { "W" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = latestNotif.title.ifBlank { "Pesan" },
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = latestNotif.timestamp,
                                                color = Color(0xFF8696A0),
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = latestNotif.message.ifBlank { "Pesan masuk" },
                                            color = Color(0xFF8696A0),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (secondNotif != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x1AFFFFFF))
                                            .padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = secondNotif.title,
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = secondNotif.timestamp,
                                            color = Color(0xFF8696A0),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // MEDIUM (4x2) and LARGE (4x4)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2A3942)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = latestNotif.title.take(1).uppercase().ifBlank { "W" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = latestNotif.title.ifBlank { "Pesan Masuk" },
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(text = latestNotif.timestamp, color = Color(0xFF8696A0), fontSize = 11.sp)
                                        }
                                        Text(
                                            text = latestNotif.message.ifBlank { "Media / Lampiran" },
                                            color = Color(0xFF8696A0),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (secondNotif != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.6.dp)
                                            .background(Color(0x2AFFFFFF))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2A3942)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = secondNotif.title.take(1).uppercase().ifBlank { "W" },
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = secondNotif.title,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(text = secondNotif.timestamp, color = Color(0xFF8696A0), fontSize = 10.sp)
                                            }
                                            Text(
                                                text = secondNotif.message,
                                                color = Color(0xFF8696A0),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No unread messages right now (All caught up)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF25D366).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tidak Ada Pesan Baru",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Semua obrolan telah dibaca • Ketuk untuk membuka WhatsApp",
                                color = Color(0xFF8696A0),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Authentic Resizable Widget Container with 8 Handle Dots matching the video at 00:03-00:08!
 */
@Composable
fun ResizableWidgetContainer(
    isJiggleMode: Boolean,
    currentSize: String, // "SMALL", "MEDIUM", "COMPACT", "LARGE"
    widgetTitle: String,
    onDelete: () -> Unit,
    onCycleSize: () -> Unit,
    onTapWidget: () -> Unit,
    onToggleAlign: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var cornerDragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .padding(if (isJiggleMode) 4.dp else 0.dp)
    ) {
        // Main Widget Body
        content()

        // Authentic iOS Jiggle Mode Resize Frame (Matching video at 00:03-00:08!)
        if (isJiggleMode) {
            // 1. White border outline around widget
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            // 2. Eight handle dots around the frame
            @Composable
            fun HandleDot(align: Alignment, offsetX: Dp = 0.dp, offsetY: Dp = 0.dp) {
                Box(
                    modifier = Modifier
                        .align(align)
                        .offset(offsetX, offsetY)
                        .size(9.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(0.6.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                )
            }

            // Top-Center
            HandleDot(Alignment.TopCenter, 0.dp, (-4.5).dp)
            // Bottom-Center
            HandleDot(Alignment.BottomCenter, 0.dp, 4.5.dp)
            // Left-Center
            HandleDot(Alignment.CenterStart, (-4.5).dp, 0.dp)
            // Right-Center
            HandleDot(Alignment.CenterEnd, 4.5.dp, 0.dp)
            // Top-Right
            HandleDot(Alignment.TopEnd, 2.dp, (-2).dp)
            // Bottom-Left
            HandleDot(Alignment.BottomStart, (-2).dp, 2.dp)

            // 3. Delete Badge ('✕') at Top-Left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-8).dp, y = (-8).dp)
                    .size(24.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF8E8E93).copy(alpha = 0.95f))
                    .border(1.dp, Color.White, CircleShape)
                    .clickable {
                        IOSFeedbackHelper.vibrateMedium(context)
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Hapus Widget",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Swap Side Pill for 2x2 or 2x1 widgets (Left <-> Right)
            if (onToggleAlign != null && (currentSize == "SMALL" || currentSize == "COMPACT")) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-4).dp, y = 6.dp)
                        .shadow(4.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.95f))
                        .border(1.dp, Color(0xFF007AFF).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable {
                            IOSFeedbackHelper.vibrateLight(context)
                            onToggleAlign()
                        }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = "Pindah Sisi",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Posisi",
                            color = Color(0xFF007AFF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 4. Interactive Bottom-Right Resize Handle & Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFF007AFF).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable {
                        IOSFeedbackHelper.vibrateLight(context)
                        onCycleSize()
                    }
                    .pointerInput(currentSize) {
                        detectDragGestures(
                            onDragStart = {
                                cornerDragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                cornerDragOffset += dragAmount
                                if (cornerDragOffset.getDistance() > 40f) {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    onCycleSize()
                                    cornerDragOffset = Offset.Zero
                                }
                            },
                            onDragEnd = { cornerDragOffset = Offset.Zero },
                            onDragCancel = { cornerDragOffset = Offset.Zero }
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AspectRatio,
                        contentDescription = "Ubah Ukuran",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = when (currentSize) {
                            "SMALL" -> "2x2"
                            "MEDIUM" -> "4x2"
                            "COMPACT" -> "2x1"
                            "LARGE" -> "4x4"
                            else -> currentSize
                        },
                        color = Color(0xFF007AFF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Authentic iOS Widget Gallery Dialog for Adding New Widgets
 */
@Composable
fun LiquidGlassAddWidgetDialog(
    onAddWidget: (type: String, size: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf("WHATSAPP") }
    var selectedSize by remember { mutableStateOf("SMALL") } // 2x2 default

    val widgetOptions = listOf(
        Triple("WHATSAPP", "WhatsApp", "Pesan terbaru & obrolan langsung dari perangkat"),
        Triple("WEATHER", "Cuaca", "Perkiraan suhu, cuaca real-time & kota"),
        Triple("PHOTOS", "Foto", "Galeri foto memori & slide foto album"),
        Triple("CALENDAR", "Kalender", "Hari, tanggal & agenda pertemuan berikutnya"),
        Triple("BATTERY", "Baterai", "Status daya iPhone & baterai perangkat"),
        Triple("CLOCK", "Jam Dunia", "Jam analog presisi & perbedaan zona waktu"),
        Triple("MUSIC", "Musik", "Pemutar Now Playing, visualizer & kontrol lagu"),
        Triple("FITNESS", "Aktivitas", "Cincin bergerak, latihan, dan berdiri harian"),
        Triple("NOTES", "Catatan Cepat", "Memo penting & catatan ide dalam gelas madu")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xF51C1C1E),
        shape = RoundedCornerShape(26.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah Widget Baru",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onDismiss() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Batal", color = Color(0xFF0A84FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = "Pilih widget dan ukuran frame untuk ditambahkan ke Layar Utama",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Size segmented control
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pilih Ukuran Frame:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sizes = listOf(
                            "SMALL" to "2x2",
                            "MEDIUM" to "4x2",
                            "COMPACT" to "2x1",
                            "LARGE" to "4x4"
                        )
                        sizes.forEach { (sz, label) ->
                            val isSelected = selectedSize == sz
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF0A84FF) else Color.Transparent)
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        selectedSize = sz
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Widget List
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pilih Jenis Widget:", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(widgetOptions) { (typeKey, title, desc) ->
                            val isSelected = selectedType == typeKey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Color(0xFF0A84FF).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.15f)
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        selectedType = typeKey
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val icon = when (typeKey) {
                                        "WHATSAPP" -> Icons.Rounded.Chat
                                        "WEATHER" -> Icons.Rounded.CloudQueue
                                        "PHOTOS" -> Icons.Rounded.PhotoCameraBack
                                        "CALENDAR" -> Icons.Rounded.CalendarToday
                                        "BATTERY" -> Icons.Rounded.BatteryChargingFull
                                        "CLOCK" -> Icons.Rounded.AccessTime
                                        "MUSIC" -> Icons.Rounded.MusicNote
                                        "FITNESS" -> Icons.Rounded.DirectionsRun
                                        else -> Icons.Rounded.EditNote
                                    }
                                    val iconBg = if (typeKey == "WHATSAPP") Color(0xFF25D366) else Color.White.copy(alpha = 0.15f)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(text = desc, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    if (isSelected) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Terpilih", tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Widget Action Button
                Button(
                    onClick = {
                        IOSFeedbackHelper.vibrateMedium(context)
                        onAddWidget(selectedType, selectedSize)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tambahkan Widget",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {}
    )
}

