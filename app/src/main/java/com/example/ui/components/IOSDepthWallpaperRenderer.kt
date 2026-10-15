package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.R
import com.example.model.ClockFontStyle
import com.example.model.ClockLayoutMode
import com.example.model.LockScreenConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// High-Fashion Editorial Typography Families bundled in app/src/main/res/font/
val BodoniModaFontFamily = FontFamily(
    Font(R.font.bodoni_moda, FontWeight.Normal),
    Font(R.font.bodoni_moda, FontWeight.Light),
    Font(R.font.bodoni_moda, FontWeight.Medium),
    Font(R.font.bodoni_moda, FontWeight.SemiBold),
    Font(R.font.bodoni_moda, FontWeight.Bold),
    Font(R.font.bodoni_moda, FontWeight.ExtraBold),
    Font(R.font.bodoni_moda, FontWeight.Black)
)
val CormorantGaramondFontFamily = FontFamily(
    Font(R.font.cormorant_garamond, FontWeight.Normal),
    Font(R.font.cormorant_garamond, FontWeight.Light),
    Font(R.font.cormorant_garamond, FontWeight.Medium),
    Font(R.font.cormorant_garamond, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond, FontWeight.Bold),
    Font(R.font.cormorant_garamond, FontWeight.ExtraBold),
    Font(R.font.cormorant_garamond, FontWeight.Black)
)
val PlayfairDisplayFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Light),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold),
    Font(R.font.playfair_display, FontWeight.ExtraBold),
    Font(R.font.playfair_display, FontWeight.Black)
)

/**
 * Dynamic theme-specific visual profile for lock screen depth effect.
 * Tailors typography, colors, shadows, positions, and ambient sun flares to match each design.
 */
data class DepthThemeProfile(
    val topPaddingPercent: Float,
    val clockColor: Color,
    val clockShadowColor: Color,
    val dateColor: Color,
    val dateLetterSpacing: Float,
    val clockFontSize: Float,
    val clockLineHeight: Float,
    val hasBackgroundSun: Boolean = false,
    val sunPositionX: Float = 0.5f,
    val sunPositionY: Float = 0.5f,
    val sunRadius: Float = 40f,
    val sunGlowColor: Color = Color.White
)

fun getDepthThemeProfile(depthTheme: String, wallpaperRes: Int): DepthThemeProfile {
    return when {
        depthTheme == "mountain" || wallpaperRes == R.drawable.wallpaper_alpine -> DepthThemeProfile(
            topPaddingPercent = 0.12f,
            clockColor = Color(0xFFC7D2FE), // Soft Lavender/Indigo as in user design
            clockShadowColor = Color(0x66000000),
            dateColor = Color(0xFFE0E7FF).copy(alpha = 0.95f),
            dateLetterSpacing = 1.5f,
            clockFontSize = 104f,
            clockLineHeight = 88f,
            hasBackgroundSun = false, // Sun is natural in wallpaper
            sunPositionX = 0.50f,
            sunPositionY = 0.36f,
            sunRadius = 42f,
            sunGlowColor = Color(0xFFFFD54F)
        )
        depthTheme == "golden_dunes" || wallpaperRes == R.drawable.wp_golden_dunes -> DepthThemeProfile(
            topPaddingPercent = 0.11f,
            clockColor = Color(0xFFFFF7EA), // Warm golden sunset ivory
            clockShadowColor = Color(0x66FF8F00),
            dateColor = Color(0xFFFFF3E0).copy(alpha = 0.96f),
            dateLetterSpacing = 1.6f,
            clockFontSize = 108f,
            clockLineHeight = 92f,
            hasBackgroundSun = true,
            sunPositionX = 0.56f,
            sunPositionY = 0.44f,
            sunRadius = 38f,
            sunGlowColor = Color(0xFFFFD54F)
        )
        depthTheme == "monochrome_wave" || wallpaperRes == R.drawable.wp_monochrome_wave -> DepthThemeProfile(
            topPaddingPercent = 0.10f,
            clockColor = Color(0xFFFFFFFF), // High-contrast pure silver-white
            clockShadowColor = Color(0x88000000),
            dateColor = Color(0xFFEEEEEE).copy(alpha = 0.95f),
            dateLetterSpacing = 1.6f,
            clockFontSize = 110f,
            clockLineHeight = 94f,
            hasBackgroundSun = true,
            sunPositionX = 0.77f,
            sunPositionY = 0.475f,
            sunRadius = 42f,
            sunGlowColor = Color.White
        )
        depthTheme == "crystal_glass" || wallpaperRes == R.drawable.wp_crystal_glass -> DepthThemeProfile(
            topPaddingPercent = 0.11f,
            clockColor = Color(0xFFFFFFFF).copy(alpha = 0.93f), // Frosted luminous ice glass
            clockShadowColor = Color(0x5500C2FF),
            dateColor = Color(0xFFE1F5FE).copy(alpha = 0.95f),
            dateLetterSpacing = 1.8f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
        depthTheme == "twilight_clouds" || wallpaperRes == R.drawable.wp_twilight_clouds -> DepthThemeProfile(
            topPaddingPercent = 0.11f,
            clockColor = Color(0xFFFFF8E7),
            clockShadowColor = Color(0x66FF8F00),
            dateColor = Color(0xFFFFECB3).copy(alpha = 0.95f),
            dateLetterSpacing = 1.5f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
        depthTheme == "sunset_reeds" || wallpaperRes == R.drawable.wp_sunset_reeds -> DepthThemeProfile(
            topPaddingPercent = 0.11f,
            clockColor = Color(0xFFFFECB3),
            clockShadowColor = Color(0x66FF6F00),
            dateColor = Color(0xFFFFF8E1).copy(alpha = 0.92f),
            dateLetterSpacing = 1.5f,
            clockFontSize = 106f,
            clockLineHeight = 90f
        )
        depthTheme == "daisy_meadow" || wallpaperRes == R.drawable.wp_daisy_meadow -> DepthThemeProfile(
            topPaddingPercent = 0.11f,
            clockColor = Color(0xFFFFFFFF),
            clockShadowColor = Color(0x44000000),
            dateColor = Color(0xFFFFFFFF).copy(alpha = 0.95f),
            dateLetterSpacing = 1.5f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
        depthTheme == "coastal_flora" || wallpaperRes == R.drawable.wp_coastal_flora -> DepthThemeProfile(
            topPaddingPercent = 0.11f,
            clockColor = Color(0xFFFFF9E6),
            clockShadowColor = Color(0x66FF8F00),
            dateColor = Color(0xFFFFF3E0).copy(alpha = 0.95f),
            dateLetterSpacing = 1.5f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
        depthTheme == "glass_sanctuary" || wallpaperRes == R.drawable.wp_glass_sanctuary -> DepthThemeProfile(
            topPaddingPercent = 0.10f,
            clockColor = Color(0xFFFFFFFF),
            clockShadowColor = Color(0x44000000),
            dateColor = Color(0xFFFFFFFF).copy(alpha = 0.95f),
            dateLetterSpacing = 1.8f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
        depthTheme == "glass_still_life" || wallpaperRes == R.drawable.wp_glass_still_life -> DepthThemeProfile(
            topPaddingPercent = 0.10f,
            clockColor = Color(0xFFFFFFFF),
            clockShadowColor = Color(0x5500C2FF),
            dateColor = Color(0xFFFFFFFF).copy(alpha = 0.95f),
            dateLetterSpacing = 1.8f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
        else -> DepthThemeProfile(
            topPaddingPercent = 0.10f,
            clockColor = Color(0xFFFFFFFF),
            clockShadowColor = Color(0x66000000),
            dateColor = Color(0xFFFFFFFF).copy(alpha = 0.94f),
            dateLetterSpacing = 1.4f,
            clockFontSize = 108f,
            clockLineHeight = 92f
        )
    }
}

/**
 * iOS 26 Multi-Layer Depth Effect Wallpaper Renderer.
 *
 * Implements Apple's signature 3D Lock Screen Depth Effect:
 * - Layer 1: Photorealistic Background Wallpaper with subtle 3D parallax.
 * - Layer 2: Stacked 2-Line Editorial Clock (Bodoni Moda / Didot Luxury Serif),
 *            or 1-Line Horizontal Clock, nestled behind the natural foreground subject.
 * - Layer 2.5: Ambient background suns / glowing orbs on the horizon.
 * - Layer 3: High-Resolution Transparent Foreground Cutout (from authentic user designs),
 *            pixel-perfect aligned with Layer 1, projecting in front of the clock numerals.
 * - Layer 4: Living, Creative Ambient Atmospheric Engine (theme-specific particles,
 *            golden rim lights, specular wave glints, fireflies, pollen bokeh, and prismatic refractions).
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
    val depthTheme = config.depthTheme
    val isDepthEnabled = config.depthEffectEnabled
    val isCustomWallpaper = !wallpaperUri.isNullOrBlank()

    val profile = remember(depthTheme, wallpaperRes) {
        getDepthThemeProfile(depthTheme, wallpaperRes)
    }

    val fontFamily = remember(fontStyle) {
        when (fontStyle) {
            ClockFontStyle.EDITORIAL_SERIF -> BodoniModaFontFamily
            ClockFontStyle.SERIF_CLASSIC -> CormorantGaramondFontFamily
            ClockFontStyle.PLAYFAIR_DISPLAY -> PlayfairDisplayFontFamily
            ClockFontStyle.TECH_STENCIL -> FontFamily.Monospace
            ClockFontStyle.MINIMAL_THIN -> CormorantGaramondFontFamily
            ClockFontStyle.ELEGANT_SCRIPT -> FontFamily.Cursive
            ClockFontStyle.RETRO_HEAVY -> FontFamily.SansSerif
            ClockFontStyle.CYBER_PUNK -> FontFamily.Monospace
            else -> FontFamily.SansSerif
        }
    }

    val fontWeight = remember(fontStyle, config.clockFontWeight) {
        if (config.clockFontWeight in 100..900) {
            FontWeight(config.clockFontWeight)
        } else {
            when (fontStyle) {
                ClockFontStyle.BOLD_MODERN -> FontWeight.SemiBold
                ClockFontStyle.ROUNDED_SLICK -> FontWeight.Medium
                ClockFontStyle.MINIMAL_THIN -> FontWeight.ExtraLight
                ClockFontStyle.RETRO_HEAVY -> FontWeight.Black
                ClockFontStyle.CYBER_PUNK -> FontWeight.ExtraBold
                ClockFontStyle.ELEGANT_SCRIPT -> FontWeight.Normal
                else -> FontWeight.Normal
            }
        }
    }

    // Living Creative Atmosphere Animation (only active for built-in themes)
    val hasAmbientAtmosphere = isDepthEnabled && config.showAmbientAtmosphere && !isCustomWallpaper && depthTheme != "custom"
    val ambientBreath = if (hasAmbientAtmosphere) {
        val infiniteTransition = rememberInfiniteTransition(label = "depthAmbientMotion")
        val breath by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambientBreath"
        )
        breath
    } else {
        0f
    }

    val isCustomDepth = !config.customForegroundUri.isNullOrBlank()

    // Clock and depth positions are strictly rock-solid at rest (no shaking or drifting on user touch)
    val clockShiftX = config.clockHorizontalOffset

    // Resolve foreground cutout image matching the current wallpaper (null if custom wallpaper)
    val foregroundCutoutRes = remember(depthTheme, wallpaperRes, isCustomWallpaper) {
        if (isCustomWallpaper || wallpaperRes == 0 || depthTheme == "custom") null else getDepthForegroundRes(depthTheme, wallpaperRes)
    }

    // Format Date string as "Mon, 28 April" or "Monday, 28 April" as in user designs
    val formattedDateText = remember(currentDate) {
        try {
            val now = Date()
            SimpleDateFormat("EEE, d MMMM", Locale.ENGLISH).format(now)
        } catch (_: Throwable) {
            currentDate
        }
    }

    // Split hours and minutes for 2-line stacked editorial layout ("09" above "41")
    val (hourStr, minuteStr) = remember(currentTime) {
        if (currentTime.contains(":")) {
            val parts = currentTime.split(":")
            Pair(parts.getOrElse(0) { "09" }, parts.getOrElse(1) { "41" })
        } else if (currentTime.length >= 4) {
            Pair(currentTime.substring(0, 2), currentTime.substring(2, 4))
        } else {
            Pair("09", "41")
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // LAYER 1: Background Wallpaper with subtle 3D parallax
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = 0f
                    translationY = 0f
                    scaleX = if (isCustomDepth || isCustomWallpaper) 1.0f else 1.05f
                    scaleY = if (isCustomDepth || isCustomWallpaper) 1.0f else 1.05f
                }
        ) {
            if (!wallpaperUri.isNullOrBlank()) {
                AsyncImage(
                    model = wallpaperUri,
                    contentDescription = "Lockscreen Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val bgPainter = safePainterResource(
                    id = if (wallpaperRes > 0) wallpaperRes else R.drawable.wallpaper_alpine,
                    fallbackId = R.drawable.wallpaper_alpine
                )
                Image(
                    painter = bgPainter,
                    contentDescription = "Lockscreen Wallpaper",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Cinematic ambient vignette overlay for high-contrast clock readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.30f)
                            )
                        )
                    )
            )
        }

        // LAYER 2.5: Ambient Horizon Sun (e.g. Glowing Sun behind the wave in Monochrome & Dunes)
        if (isDepthEnabled && config.showBackgroundSun && profile.hasBackgroundSun && !isCustomWallpaper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = 0f
                        translationY = 0f
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width * profile.sunPositionX
                    val cy = size.height * profile.sunPositionY
                    val r = profile.sunRadius
                    val sunGlowPulse = 0.85f + (sin(ambientBreath) * 0.12f).toFloat()

                    // Atmospheric Outer Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                profile.sunGlowColor.copy(alpha = sunGlowPulse * 0.70f),
                                profile.sunGlowColor.copy(alpha = sunGlowPulse * 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = r * 2.4f
                        ),
                        radius = r * 2.4f,
                        center = Offset(cx, cy)
                    )

                    // Brilliant Sun Core
                    drawCircle(
                        color = profile.sunGlowColor.copy(alpha = 0.98f),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // LAYER 2: The Giant iOS Lock Screen Clock & Date (Nestled BEHIND the foreground subject)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenH = maxHeight
            // Compact screen adaptation (e.g. Samsung J2 Prime 540x960 16:9 vs modern 19.5:9 1080x2400)
            val isCompactScreen = screenH < 720.dp
            val scaleFactor = if (isCompactScreen) 0.90f else 1.0f

            val effectiveTopPaddingPercent = if (config.clockTopPaddingPercent > 0.01f) {
                config.clockTopPaddingPercent.coerceIn(0.02f, 0.70f)
            } else {
                profile.topPaddingPercent
            }
            val calculatedTopPadding = screenH * effectiveTopPaddingPercent
            val baseClockColor = if (config.useAutoThemeColor) profile.clockColor else Color(config.clockColorHex)
            val effectiveClockColor = baseClockColor.copy(alpha = (baseClockColor.alpha * config.clockOpacity.coerceIn(0.15f, 1f)))
            val effectiveDateColor = if (config.useAutoThemeColor) profile.dateColor else Color(config.clockColorHex).copy(alpha = 0.95f)
            val baseFontSize = if (config.clockFontSize > 40f) config.clockFontSize else profile.clockFontSize
            val effectiveFontSize = baseFontSize * scaleFactor
            val effectiveLineHeight = effectiveFontSize * 0.78f

            val density = androidx.compose.ui.platform.LocalDensity.current
            val clockShiftXPx = with(density) { clockShiftX.dp.toPx() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = calculatedTopPadding)
                    .graphicsLayer {
                        translationX = clockShiftXPx
                        translationY = 0f
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Editorial Date Label (e.g. "Mon, 28 April" in refined serif)
                Text(
                    text = formattedDateText,
                    color = effectiveDateColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = CormorantGaramondFontFamily,
                    letterSpacing = profile.dateLetterSpacing.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // CLOCK TYPOGRAPHY: Stacked 2-Line Editorial or Classic 1-Line
                if (config.clockLayoutMode == ClockLayoutMode.STACKED_EDITORIAL) {
                    // STACKED 2-LINE EDITORIAL CLOCK ("09" above "41", matching user reference images)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text(
                            text = hourStr,
                            color = effectiveClockColor,
                            fontSize = effectiveFontSize.sp,
                            lineHeight = effectiveLineHeight.sp,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            letterSpacing = (-1.5).sp,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = profile.clockShadowColor,
                                    offset = Offset(0f, 4f),
                                    blurRadius = 14f
                                )
                            )
                        )
                        Text(
                            text = minuteStr,
                            color = effectiveClockColor,
                            fontSize = effectiveFontSize.sp,
                            lineHeight = effectiveLineHeight.sp,
                            fontWeight = fontWeight,
                            fontFamily = fontFamily,
                            letterSpacing = (-1.5).sp,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(
                                    color = profile.clockShadowColor,
                                    offset = Offset(0f, 4f),
                                    blurRadius = 14f
                                )
                            )
                        )
                    }
                } else {
                    // Classic 1-Line Horizontal Clock ("09:41")
                    Text(
                        text = currentTime,
                        color = effectiveClockColor,
                        fontSize = (effectiveFontSize * 0.78f).sp,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        letterSpacing = (-2).sp,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = profile.clockShadowColor,
                                offset = Offset(0f, 6f),
                                blurRadius = 16f
                            )
                        ),
                        modifier = Modifier.padding(vertical = 0.dp)
                    )
                }

                // Optional Lock Screen Widgets Row (Weather, Battery, Calendar)
                if (config.showWeatherWidget || config.showBatteryWidget || config.showCalendarWidget) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.28f))
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
            }
        }

        // LAYER 3: Authentic High-Resolution Foreground Cutout
        // Seamlessly aligned with Layer 1, projecting IN FRONT OF the clock numbers
        if (isDepthEnabled) {
            if (!config.customForegroundUri.isNullOrBlank()) {
                // User's custom AI Depth Subject cutout from Depth Studio (strictly 1:1 aligned)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = 0f
                            translationY = 0f
                            scaleX = 1.0f
                            scaleY = 1.0f
                        }
                ) {
                    AsyncImage(
                        model = config.customForegroundUri,
                        contentDescription = "Foreground Depth Subject",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (!isCustomWallpaper && wallpaperRes > 0 && depthTheme != "custom" && foregroundCutoutRes != null && foregroundCutoutRes != 0) {
                val fgPainter = safePainterResource(
                    id = foregroundCutoutRes,
                    fallbackId = R.drawable.wallpaper_alpine
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = 0f
                            translationY = 0f
                            scaleX = 1.05f
                            scaleY = 1.05f
                        }
                ) {
                    Image(
                        painter = fgPainter,
                        contentDescription = "Foreground Depth Subject",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // LAYER 4: Living, Creative Ambient Atmosphere Engine
        // Adds bespoke organic animations (golden rim pulses, fireflies, wave glints, dust motes)
        if (hasAmbientAtmosphere) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawCreativeAmbientEffects(
                    depthTheme = depthTheme,
                    wallpaperRes = wallpaperRes,
                    w = w,
                    h = h,
                    phase = ambientBreath,
                    dragX = 0f,
                    dragY = 0f
                )
            }
        }
    }
}

/**
 * Maps the active depth theme and wallpaper resource to its authentic alpha cutout asset.
 */
fun getDepthForegroundRes(depthTheme: String, wallpaperRes: Int): Int? {
    return when {
        depthTheme == "twilight_clouds" || wallpaperRes == R.drawable.wp_twilight_clouds -> R.drawable.fg_twilight_clouds
        depthTheme == "monochrome_wave" || wallpaperRes == R.drawable.wp_monochrome_wave -> R.drawable.fg_monochrome_wave
        depthTheme == "golden_dunes" || depthTheme == "dune" || wallpaperRes == R.drawable.wp_golden_dunes -> R.drawable.fg_golden_dunes
        depthTheme == "coastal_flora" || wallpaperRes == R.drawable.wp_coastal_flora -> R.drawable.fg_coastal_flora
        depthTheme == "daisy_meadow" || wallpaperRes == R.drawable.wp_daisy_meadow -> R.drawable.fg_daisy_meadow
        depthTheme == "sunset_reeds" || wallpaperRes == R.drawable.wp_sunset_reeds -> R.drawable.fg_sunset_reeds
        depthTheme == "crystal_glass" || depthTheme == "glass_ribbon" || wallpaperRes == R.drawable.wp_crystal_glass -> R.drawable.fg_crystal_glass
        depthTheme == "glass_sanctuary" || wallpaperRes == R.drawable.wp_glass_sanctuary -> R.drawable.fg_glass_sanctuary
        depthTheme == "glass_still_life" || wallpaperRes == R.drawable.wp_glass_still_life -> R.drawable.fg_glass_still_life
        depthTheme == "mountain" || wallpaperRes == R.drawable.wallpaper_alpine -> R.drawable.fg_mountain
        else -> null
    }
}

/**
 * Crash-proof painter resource loader.
 * Validates that the resource exists and is a valid drawable in the current package resources.
 * Uses Coil's rememberAsyncImagePainter with crossfade to avoid UI blocking and prevent OutOfMemory crashes.
 */
@Composable
fun safePainterResource(
    id: Int,
    fallbackId: Int = R.drawable.wallpaper_alpine
): androidx.compose.ui.graphics.painter.Painter {
    val context = androidx.compose.ui.platform.LocalContext.current
    val verifiedId = remember(id, fallbackId) {
        val safeFallback = if (fallbackId > 0) fallbackId else R.drawable.wallpaper_alpine
        if (id <= 0) return@remember safeFallback
        try {
            val type = context.resources.getResourceTypeName(id)
            if (type == "drawable") id else safeFallback
        } catch (_: Throwable) {
            safeFallback
        }
    }
    return painterResource(id = verifiedId)
}

/**
 * Draws bespoke, living atmospheric particle and lighting effects tailored to each theme.
 * Gives the wallpaper an alive, artistic, dynamic feel that moves with the user's touch and breath.
 */
private fun DrawScope.drawCreativeAmbientEffects(
    depthTheme: String,
    wallpaperRes: Int,
    w: Float,
    h: Float,
    phase: Float,
    dragX: Float,
    dragY: Float
) {
    val effectiveTheme = when {
        depthTheme == "twilight_clouds" || wallpaperRes == R.drawable.wp_twilight_clouds -> "twilight_clouds"
        depthTheme == "monochrome_wave" || wallpaperRes == R.drawable.wp_monochrome_wave -> "monochrome_wave"
        depthTheme == "golden_dunes" || depthTheme == "dune" || wallpaperRes == R.drawable.wp_golden_dunes -> "golden_dunes"
        depthTheme == "coastal_flora" || wallpaperRes == R.drawable.wp_coastal_flora -> "coastal_flora"
        depthTheme == "daisy_meadow" || wallpaperRes == R.drawable.wp_daisy_meadow -> "daisy_meadow"
        depthTheme == "sunset_reeds" || wallpaperRes == R.drawable.wp_sunset_reeds -> "sunset_reeds"
        depthTheme == "crystal_glass" || depthTheme == "glass_ribbon" || wallpaperRes == R.drawable.wp_crystal_glass -> "crystal_glass"
        depthTheme == "glass_sanctuary" || wallpaperRes == R.drawable.wp_glass_sanctuary -> "glass_sanctuary"
        depthTheme == "glass_still_life" || wallpaperRes == R.drawable.wp_glass_still_life -> "glass_still_life"
        else -> "mountain"
    }

    when (effectiveTheme) {
        "twilight_clouds" -> {
            // Warm golden sunset rim light pulse across the cloud ridge
            val pulseAlpha = 0.28f + (sin(phase) * 0.12f).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFB340).copy(alpha = pulseAlpha),
                        Color(0xFFFF7A00).copy(alpha = pulseAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.42f + dragX * 0.1f, h * 0.28f + dragY * 0.1f),
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = Offset(w * 0.42f + dragX * 0.1f, h * 0.28f + dragY * 0.1f)
            )

            // Twinkling warm dusk dust motes
            for (i in 0..5) {
                val motePhase = phase + i * 1.05f
                val mx = w * (0.20f + (i * 0.12f) + sin(motePhase) * 0.04f).toFloat()
                val my = h * (0.22f + (i * 0.035f) + cos(motePhase * 0.8f) * 0.03f).toFloat()
                val mAlpha = (0.35f + sin(motePhase * 2f) * 0.25f).coerceIn(0.1f, 0.7f).toFloat()
                drawCircle(
                    color = Color(0xFFFFD580).copy(alpha = mAlpha),
                    radius = 2.2f + (i % 3) * 0.8f,
                    center = Offset(mx, my)
                )
            }
        }

        "monochrome_wave" -> {
            // Sparkling specular spray points along the cresting ocean wave
            for (i in 0..7) {
                val wavePhase = phase * 1.5f + i * 0.8f
                val wx = w * (0.15f + i * 0.10f + sin(wavePhase) * 0.02f).toFloat()
                val wy = h * (0.26f + sin(i * 0.7f) * 0.04f + cos(wavePhase) * 0.015f).toFloat()
                val wAlpha = (0.4f + sin(wavePhase * 2f) * 0.4f).coerceIn(0.05f, 0.85f).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = wAlpha),
                    radius = 1.8f + (i % 2) * 1.2f,
                    center = Offset(wx + dragX * 0.05f, wy + dragY * 0.05f)
                )
            }
        }

        "golden_dunes" -> {
            // Sun flare specular glint on the curved dune ridge
            val duneGlintAlpha = (0.35f + sin(phase * 1.2f) * 0.18f).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE082).copy(alpha = duneGlintAlpha),
                        Color(0xFFFF9800).copy(alpha = duneGlintAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.52f + dragX * 0.12f, h * 0.29f + dragY * 0.12f),
                    radius = w * 0.38f
                ),
                radius = w * 0.38f,
                center = Offset(w * 0.52f + dragX * 0.12f, h * 0.29f + dragY * 0.12f)
            )

            // Warm desert dust motes catching golden hour sunlight
            for (i in 0..6) {
                val dustPhase = phase * 0.9f + i * 0.95f
                val dx = w * (0.25f + i * 0.09f + sin(dustPhase) * 0.035f).toFloat()
                val dy = h * (0.24f + i * 0.025f + cos(dustPhase) * 0.02f).toFloat()
                val dAlpha = (0.3f + sin(dustPhase * 1.8f) * 0.25f).coerceIn(0.08f, 0.65f).toFloat()
                drawCircle(
                    color = Color(0xFFFFE57F).copy(alpha = dAlpha),
                    radius = 2.0f + (i % 3) * 0.6f,
                    center = Offset(dx, dy)
                )
            }
        }

        "daisy_meadow" -> {
            // Soft floating floral pollen & golden summer sunbeams
            val beamAlpha = 0.22f + (sin(phase * 0.8f) * 0.08f).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF9C4).copy(alpha = beamAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.78f, h * 0.15f),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.78f, h * 0.15f)
            )

            // Floating summer pollen particles drifting upwards
            for (i in 0..7) {
                val pollenPhase = phase * 1.1f + i * 0.78f
                val px = w * (0.18f + i * 0.095f + sin(pollenPhase) * 0.03f).toFloat()
                val py = h * (0.32f - (i * 0.02f) + sin(pollenPhase * 1.4f) * 0.025f).toFloat()
                val pAlpha = (0.32f + sin(pollenPhase * 2f) * 0.28f).coerceIn(0.05f, 0.7f).toFloat()
                drawCircle(
                    color = Color(0xFFFFFDE7).copy(alpha = pAlpha),
                    radius = 1.8f + (i % 3) * 1.0f,
                    center = Offset(px, py)
                )
            }
        }

        "coastal_flora" -> {
            // Shimmering Mediterranean sea reflection highlights
            for (i in 0..6) {
                val seaPhase = phase * 1.4f + i * 0.85f
                val sx = w * (0.45f + i * 0.08f + sin(seaPhase) * 0.015f).toFloat()
                val sy = h * (0.28f + (i % 2) * 0.018f).toFloat()
                val sAlpha = (0.35f + sin(seaPhase * 2.2f) * 0.35f).coerceIn(0.05f, 0.75f).toFloat()
                drawLine(
                    color = Color(0xFFFFE0B2).copy(alpha = sAlpha),
                    start = Offset(sx - 12f, sy),
                    end = Offset(sx + 12f, sy),
                    strokeWidth = 2.0f
                )
            }
        }

        "sunset_reeds" -> {
            // Warm golden firefly embers gently drifting around the lake reeds
            for (i in 0..6) {
                val fireflyPhase = phase * 1.3f + i * 1.1f
                val fx = w * (0.22f + i * 0.11f + sin(fireflyPhase) * 0.045f).toFloat()
                val fy = h * (0.27f + (i * 0.028f) + cos(fireflyPhase * 0.9f) * 0.035f).toFloat()
                val fAlpha = (0.4f + sin(fireflyPhase * 2.5f) * 0.4f).coerceIn(0.1f, 0.9f).toFloat()
                // Glowing outer halo
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = fAlpha * 0.3f),
                    radius = 6.0f,
                    center = Offset(fx, fy)
                )
                // Bright center ember
                drawCircle(
                    color = Color(0xFFFFF9C4).copy(alpha = fAlpha),
                    radius = 2.4f,
                    center = Offset(fx, fy)
                )
            }
        }

        "crystal_glass" -> {
            // Prismatic rainbow specular dispersion highlights along the glass curves
            val prismAlpha = (0.32f + sin(phase * 1.6f) * 0.16f).toFloat()
            val cx = w * 0.48f + dragX * 0.15f
            val cy = h * 0.28f + dragY * 0.15f
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF2D55).copy(alpha = prismAlpha * 0.7f),
                        Color(0xFF007AFF).copy(alpha = prismAlpha * 0.7f),
                        Color(0xFF34C759).copy(alpha = prismAlpha * 0.7f),
                        Color(0xFFFF9500).copy(alpha = prismAlpha * 0.7f),
                        Color(0xFFFF2D55).copy(alpha = prismAlpha * 0.7f)
                    ),
                    center = Offset(cx, cy)
                ),
                radius = 18f,
                center = Offset(cx, cy)
            )
        }

        "glass_sanctuary" -> {
            // Zen minimalist ambient caustic refraction pillar
            val causticAlpha = (0.22f + sin(phase * 0.9f) * 0.09f).toFloat()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = causticAlpha),
                        Color.White.copy(alpha = causticAlpha * 0.3f),
                        Color.Transparent
                    ),
                    startY = h * 0.22f,
                    endY = h * 0.45f
                ),
                topLeft = Offset(w * 0.35f + dragX * 0.05f, h * 0.22f),
                size = androidx.compose.ui.geometry.Size(w * 0.30f, h * 0.23f)
            )
        }

        "glass_still_life" -> {
            // Prismatic reflection shimmer inside the transparent glass orb
            val orbAlpha = (0.35f + sin(phase * 1.5f) * 0.18f).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = orbAlpha),
                radius = 3.5f,
                center = Offset(w * 0.44f + dragX * 0.08f, h * 0.26f + dragY * 0.08f)
            )
        }

        else -> {
            // Alpine Mountain Summit: crisp snow sparkle tip and gentle atmospheric haze
            val snowSparkleAlpha = (0.45f + sin(phase * 2f) * 0.35f).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = snowSparkleAlpha),
                radius = 2.8f,
                center = Offset(w * 0.44f + dragX * 0.05f, h * 0.225f + dragY * 0.05f)
            )
        }
    }
}
