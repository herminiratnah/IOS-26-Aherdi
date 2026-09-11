package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.viewmodel.IOSViewModel

data class SpotlightItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val appId: AppId? = null,
    val action: (() -> Unit)? = null
)

@Composable
fun SpotlightSearchOverlay(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val allSearchItems = remember {
        listOf(
            SpotlightItem("Phone", "System App", Icons.Rounded.Phone, Color(0xFF34C759), AppId.PHONE),
            SpotlightItem("Messages", "System App", Icons.Rounded.ChatBubble, Color(0xFF34C759), AppId.MESSAGES),
            SpotlightItem("Photos", "System App", Icons.Rounded.PhotoLibrary, Color(0xFFFF9500), AppId.PHOTOS),
            SpotlightItem("Camera", "System App", Icons.Rounded.PhotoCamera, Color(0xFF636366), AppId.CAMERA),
            SpotlightItem("Calendar", "System App", Icons.Rounded.CalendarToday, Color(0xFFFF3B30), AppId.CALENDAR),
            SpotlightItem("Notes", "System App", Icons.Rounded.Description, Color(0xFFFFCC00), AppId.NOTES),
            SpotlightItem("Clock & Alarms", "System App", Icons.Rounded.AccessTime, Color(0xFFFF9500), AppId.CLOCK),
            SpotlightItem("Settings", "System App", Icons.Rounded.Settings, Color(0xFF8E8E93), AppId.SETTINGS),
            SpotlightItem("Safari", "Web Browser", Icons.Rounded.Explore, Color(0xFF007AFF), AppId.SAFARI),
            SpotlightItem("Maps", "Navigation", Icons.Rounded.Place, Color(0xFF34C759), AppId.MAPS),
            SpotlightItem("Weather", "Forecast", Icons.Rounded.WbSunny, Color(0xFF5AC8FA), AppId.WEATHER),
            SpotlightItem("Calculator", "Tools", Icons.Rounded.Calculate, Color(0xFFFF9500), AppId.CALCULATOR),
            SpotlightItem("App Store", "Applications", Icons.Rounded.Shop, Color(0xFF007AFF), AppId.APP_STORE),
            SpotlightItem("Music", "Media Player", Icons.Rounded.MusicNote, Color(0xFFFF2D55), AppId.MUSIC),
            SpotlightItem("FaceTime", "Video Call", Icons.Rounded.Videocam, Color(0xFF34C759), AppId.FACETIME)
        )
    }

    // Instant Math Calculation Engine (e.g. 50 * 4, 100 / 2, 25 + 75)
    val mathResult = remember(query) {
        val trimmed = query.trim()
        val regex = Regex("""^(\d+(?:\.\d+)?)\s*([\+\-\*\/xX])\s*(\d+(?:\.\d+)?)$""")
        val match = regex.matchEntire(trimmed)
        if (match != null) {
            val num1 = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val op = match.groupValues[2]
            val num2 = match.groupValues[3].toDoubleOrNull() ?: 0.0
            val res = when (op) {
                "+", "plus" -> num1 + num2
                "-", "minus" -> num1 - num2
                "*", "x", "X" -> num1 * num2
                "/" -> if (num2 != 0.0) num1 / num2 else null
                else -> null
            }
            if (res != null) {
                val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else "%.2f".format(res)
                "$trimmed = $formatted"
            } else null
        } else null
    }

    val filteredItems = remember(query) {
        if (query.isBlank()) {
            allSearchItems.take(6)
        } else {
            allSearchItems.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.subtitle.contains(query, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    keyboardController?.hide()
                    onDismiss()
                }
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent dismiss */ }
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // iOS Frosted Glass Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2C2C2E).copy(alpha = 0.92f))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )

                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = "Search Apps, Math (e.g. 50*4)...",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 15.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                )

                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real-time Math Calculator Card
            if (mathResult != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.95f))
                        .clickable {
                            keyboardController?.hide()
                            viewModel.openApp(AppId.CALCULATOR)
                            onDismiss()
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF9500)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = mathResult,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Calculator Calculation",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.90f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = if (query.isBlank()) "TOP SUGGESTIONS" else "APPLICATIONS & RESULTS",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                items(filteredItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                keyboardController?.hide()
                                item.appId?.let { viewModel.openApp(it) }
                                item.action?.invoke()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.iconColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = item.subtitle,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
