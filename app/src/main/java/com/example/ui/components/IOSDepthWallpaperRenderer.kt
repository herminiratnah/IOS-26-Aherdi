package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ClockFontStyle
import com.example.model.LockScreenConfig

/**
 * iOS 26 Multi-Layer Depth Effect Wallpaper Renderer.
 * Places the giant lockscreen clock in 3D perspective *behind* the foreground subject
 * (such as alpine mountain peaks, celestial planets, dunes, or modern glass ribbons).
 */
@Composable
fun IOSDepthWallpaperRenderer(
    config: LockScreenConfig,
    wallpaperRes: Int,
    wallpaperUri: String?,
    currentTime: String,
    currentDate: String,
    batteryLevel: Int,
    isBatteryCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val fontStyle = config.fontStyle
    val clockColor = Color(config.clockColorHex)
    val depthTheme = config.depthTheme
    val isDepthEnabled = config.depthEffectEnabled

    val fontFamily = remember(fontStyle) {
        when (fontStyle) {
            ClockFontStyle.SERIF_CLASSIC -> FontFamily.Serif
            ClockFontStyle.TECH_STENCIL -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
    }

    val fontWeight = remember(fontStyle) {
        when (fontStyle) {
            ClockFontStyle.BOLD_MODERN -> FontWeight.SemiBold
            ClockFontStyle.ROUNDED_SLICK -> FontWeight.Medium
            ClockFontStyle.MINIMAL_THIN -> FontWeight.ExtraLight
            else -> FontWeight.Normal
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // LAYER 1: Background Wallpaper
        if (!wallpaperUri.isNullOrBlank()) {
            AsyncImage(
                model = wallpaperUri,
                contentDescription = "Lockscreen Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(
                    id = if (wallpaperRes != 0) wallpaperRes else R.drawable.wallpaper_mountain_depth
                ),
                contentDescription = "Lockscreen Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Ambient twilight / vignette overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.35f)
                        )
                    )
                )
        )

        // LAYER 2: The Giant iOS Lockscreen Clock & Date (Rendered BEHIND foreground subject when depth is ON)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Date Label
            Text(
                text = currentDate,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Center
            )

            // Giant Clock
            Text(
                text = currentTime,
                color = clockColor,
                fontSize = 86.sp,
                fontWeight = fontWeight,
                fontFamily = fontFamily,
                letterSpacing = (-2).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 0.dp)
            )

            // Inline Lock Screen Widgets Row (Weather, Battery, Calendar)
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showWeatherWidget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFFFFD60A),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "30° Jakarta",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (config.showBatteryWidget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBatteryCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                            contentDescription = null,
                            tint = if (isBatteryCharging) Color(0xFF34C759) else Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$batteryLevel%",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (config.showCalendarWidget) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFFFF453A),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "No Events",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // LAYER 3: The Foreground Depth Subject (Mountain / Planet / Dune / Skyline / Glass)
        // Only active when depthEffectEnabled is TRUE and depthTheme != "none"
        if (isDepthEnabled && depthTheme != "none") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                when (depthTheme) {
                    "mountain" -> drawMountainDepthSubject(w, h)
                    "planet" -> drawPlanetDepthSubject(w, h)
                    "dune" -> drawDuneDepthSubject(w, h)
                    "skyline" -> drawSkylineDepthSubject(w, h)
                    "glass_ribbon" -> drawGlassRibbonDepthSubject(w, h)
                    else -> drawMountainDepthSubject(w, h)
                }
            }
        }
    }
}

/**
 * Draws majestic alpine mountain peaks that project *in front* of the clock's lower portion,
 * creating Apple's iconic Mountain Depth Effect.
 */
private fun DrawScope.drawMountainDepthSubject(w: Float, h: Float) {
    // Summit Y-coordinates aligned to overlap bottom 30-35% of the clock
    val summitY = h * 0.225f
    val baseH = h

    // 1. Back Ridge (Muted deep indigo)
    val backRidgePath = Path().apply {
        moveTo(0f, h * 0.29f)
        lineTo(w * 0.25f, h * 0.235f)
        lineTo(w * 0.50f, h * 0.265f)
        lineTo(w * 0.82f, h * 0.210f)
        lineTo(w, h * 0.255f)
        lineTo(w, baseH)
        lineTo(0f, baseH)
        close()
    }
    drawPath(
        path = backRidgePath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF191F33), Color(0xFF0D111D)),
            startY = h * 0.20f,
            endY = h * 0.55f
        )
    )

    // 2. Primary Mountain Peak - Left Face (In Shadow)
    val leftFacePath = Path().apply {
        moveTo(w * 0.44f, summitY)
        lineTo(w * 0.12f, h * 0.38f)
        lineTo(0f, h * 0.35f)
        lineTo(0f, baseH)
        lineTo(w * 0.44f, baseH)
        close()
    }
    drawPath(
        path = leftFacePath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1E2842), Color(0xFF0F1524)),
            start = Offset(w * 0.44f, summitY),
            end = Offset(0f, h * 0.45f)
        )
    )

    // 3. Primary Mountain Peak - Right Face (Subtle twilight rim light)
    val rightFacePath = Path().apply {
        moveTo(w * 0.44f, summitY)
        lineTo(w * 0.74f, h * 0.35f)
        lineTo(w, h * 0.39f)
        lineTo(w, baseH)
        lineTo(w * 0.44f, baseH)
        close()
    }
    drawPath(
        path = rightFacePath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF283452), Color(0xFF151C2C)),
            start = Offset(w * 0.44f, summitY),
            end = Offset(w, h * 0.45f)
        )
    )

    // 4. Central Snow Ridge & Summit Peak Snow Cap
    val snowRidgePath = Path().apply {
        moveTo(w * 0.44f, summitY)
        lineTo(w * 0.40f, summitY + 45f)
        lineTo(w * 0.46f, summitY + 80f)
        lineTo(w * 0.42f, summitY + 130f)
        lineTo(w * 0.45f, summitY + 180f)
        lineTo(w * 0.43f, summitY + 185f)
        lineTo(w * 0.38f, summitY + 120f)
        lineTo(w * 0.43f, summitY + 70f)
        lineTo(w * 0.39f, summitY + 40f)
        close()
    }
    drawPath(
        path = snowRidgePath,
        brush = Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.90f), Color(0xFFB8C8E6).copy(alpha = 0.60f)),
            startY = summitY,
            endY = summitY + 185f
        )
    )

    // Sharp summit tip highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = 2.5f,
        center = Offset(w * 0.44f, summitY)
    )

    // 5. Foreground Ridge & Forest Valley base (creates realistic ground depth)
    val valleyPath = Path().apply {
        moveTo(0f, h * 0.42f)
        quadraticTo(w * 0.30f, h * 0.38f, w * 0.55f, h * 0.43f)
        quadraticTo(w * 0.80f, h * 0.47f, w, h * 0.41f)
        lineTo(w, baseH)
        lineTo(0f, baseH)
        close()
    }
    drawPath(
        path = valleyPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0C101A), Color(0xFF05070B)),
            startY = h * 0.40f,
            endY = h
        )
    )
}

/**
 * Draws a celestial curved planet horizon with glowing blue atmospheric rim.
 */
private fun DrawScope.drawPlanetDepthSubject(w: Float, h: Float) {
    val planetTopY = h * 0.24f
    val cx = w * 0.5f
    val cy = h * 0.65f
    val radius = w * 0.85f

    // Atmosphere Outer Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF00C2FF).copy(alpha = 0.65f),
                Color(0xFF0A84FF).copy(alpha = 0.25f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = radius * 1.08f
        ),
        radius = radius * 1.08f,
        center = Offset(cx, cy)
    )

    // Planet Body (Deep space dark matter)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF182236), Color(0xFF070B14)),
            center = Offset(cx, cy * 0.9f),
            radius = radius
        ),
        radius = radius,
        center = Offset(cx, cy)
    )

    // Glowing Atmospheric Edge Rim
    val rimPath = Path().apply {
        moveTo(cx - radius * 0.95f, cy)
        quadraticTo(cx, planetTopY, cx + radius * 0.95f, cy)
        lineTo(cx + radius * 0.95f, cy + 15f)
        quadraticTo(cx, planetTopY + 12f, cx - radius * 0.95f, cy + 15f)
        close()
    }
    drawPath(
        path = rimPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF64D2FF).copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.95f),
                Color(0xFF64D2FF).copy(alpha = 0.85f),
                Color.Transparent
            )
        )
    )
}

/**
 * Draws silky desert dune crests that sweep across the lower portion of the clock.
 */
private fun DrawScope.drawDuneDepthSubject(w: Float, h: Float) {
    val duneY = h * 0.245f

    val dune1 = Path().apply {
        moveTo(0f, h * 0.27f)
        quadraticTo(w * 0.35f, duneY, w * 0.70f, h * 0.29f)
        quadraticTo(w * 0.88f, h * 0.31f, w, h * 0.28f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = dune1,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFD97706), Color(0xFF78350F), Color(0xFF1E1B18)),
            startY = duneY,
            endY = h * 0.6f
        )
    )

    // Dune Rim Light
    val rimPath = Path().apply {
        moveTo(0f, h * 0.27f)
        quadraticTo(w * 0.35f, duneY, w * 0.70f, h * 0.29f)
        quadraticTo(w * 0.88f, h * 0.31f, w, h * 0.28f)
        lineTo(w, h * 0.28f + 5f)
        quadraticTo(w * 0.88f, h * 0.31f + 5f, w * 0.70f, h * 0.29f + 5f)
        quadraticTo(w * 0.35f, duneY + 5f, 0f, h * 0.27f + 5f)
        close()
    }
    drawPath(
        path = rimPath,
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFFFE082), Color(0xFFFFB74D), Color(0xFFFF8A65))
        )
    )
}

/**
 * Draws futuristic metropolis skyline spires rising up in front of the clock digits.
 */
private fun DrawScope.drawSkylineDepthSubject(w: Float, h: Float) {
    val baseY = h * 0.23f

    val cityPath = Path().apply {
        moveTo(0f, h * 0.32f)
        lineTo(w * 0.12f, h * 0.32f)
        lineTo(w * 0.12f, h * 0.27f)
        lineTo(w * 0.18f, h * 0.27f)
        lineTo(w * 0.18f, h * 0.30f)
        lineTo(w * 0.25f, h * 0.30f)
        // Tower Spire
        lineTo(w * 0.38f, h * 0.30f)
        lineTo(w * 0.40f, baseY)
        lineTo(w * 0.42f, h * 0.30f)
        lineTo(w * 0.58f, h * 0.30f)
        lineTo(w * 0.60f, h * 0.25f)
        lineTo(w * 0.72f, h * 0.25f)
        lineTo(w * 0.72f, h * 0.31f)
        lineTo(w * 0.85f, h * 0.31f)
        lineTo(w * 0.85f, h * 0.26f)
        lineTo(w, h * 0.26f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = cityPath,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1E1035), Color(0xFF0F071D), Color(0xFF05020A)),
            startY = baseY,
            endY = h * 0.6f
        )
    )

    // Neon Accent Line
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF2D55), Color(0xFF007AFF), Color(0xFF5856D6))
        ),
        start = Offset(0f, h * 0.33f),
        end = Offset(w, h * 0.33f),
        strokeWidth = 3f
    )
}

/**
 * Draws a 3D translucent frosted glass ribbon looping through the clock numbers.
 */
private fun DrawScope.drawGlassRibbonDepthSubject(w: Float, h: Float) {
    val ribbonY = h * 0.235f

    val glassPath = Path().apply {
        moveTo(0f, h * 0.28f)
        cubicTo(w * 0.25f, ribbonY - 30f, w * 0.65f, ribbonY + 70f, w, h * 0.25f)
        lineTo(w, h * 0.33f)
        cubicTo(w * 0.65f, ribbonY + 150f, w * 0.25f, ribbonY + 50f, 0f, h * 0.36f)
        close()
    }
    drawPath(
        path = glassPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                Color(0xFF32ADE6).copy(alpha = 0.25f),
                Color(0xFFAF52DE).copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.40f)
            ),
            start = Offset(0f, ribbonY),
            end = Offset(w, ribbonY + 100f)
        )
    )

    // Glass Border Highlight
    drawPath(
        path = glassPath,
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.85f))
        ),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
    )
}
