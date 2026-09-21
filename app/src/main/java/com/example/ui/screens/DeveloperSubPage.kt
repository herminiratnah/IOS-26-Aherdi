package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel

@Composable
fun DeveloperSubPage(
    viewModel: IOSViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDeveloperMode by viewModel.isDeveloperModeEnabled.collectAsState()
    val isFpsVisible by viewModel.isFpsCounterVisible.collectAsState()
    val animScale by viewModel.animationScale.collectAsState()

    var forceGpuRendering by remember { mutableStateOf(true) }
    var debugLayoutBounds by remember { mutableStateOf(false) }
    var thermalMode by remember { mutableStateOf("Nominal (Dingin)") }

    val runtime = Runtime.getRuntime()
    val totalMemMb = runtime.totalMemory() / (1024 * 1024)
    val freeMemMb = runtime.freeMemory() / (1024 * 1024)
    val usedMemMb = totalMemMb - freeMemMb
    val maxHeapMb = runtime.maxMemory() / (1024 * 1024)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Mode Pengembang", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "PENGEMBANG & DIAGNOSTIK SISTEM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                )
            }

            // Developer Master Toggle
            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Aktifkan Opsi Pengembang",
                        checked = isDeveloperMode,
                        onCheckedChange = {
                            IOSFeedbackHelper.vibrateMedium(context)
                            viewModel.toggleDeveloperMode(it)
                        },
                        icon = Icons.Rounded.Code,
                        iconBg = Color(0xFF007AFF)
                    )
                }
            }

            // Performance & Overlay HUD
            item {
                Text(
                    text = "PERFORMA & VISUALISASI LAYAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Tampilkan HUD FPS Real-time",
                        checked = isFpsVisible,
                        onCheckedChange = {
                            IOSFeedbackHelper.vibrateLight(context)
                            viewModel.toggleFpsCounter(it)
                            viewModel.showDynamicIslandNotification(
                                title = "Developer HUD",
                                message = if (it) "FPS Counter Aktif di layar atas" else "FPS Counter Dinonaktifkan",
                                icon = "speed",
                                accent = 0xFF34C759
                            )
                        },
                        icon = Icons.Rounded.Speed,
                        iconBg = Color(0xFF34C759)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Paksa Akselerasi GPU 2D",
                        checked = forceGpuRendering,
                        onCheckedChange = { forceGpuRendering = it },
                        icon = Icons.Rounded.Memory,
                        iconBg = Color(0xFFFF9500)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Tampilkan Batas Layout (Debug)",
                        checked = debugLayoutBounds,
                        onCheckedChange = { debugLayoutBounds = it },
                        icon = Icons.Rounded.Grid4x4,
                        iconBg = Color(0xFFAF52DE)
                    )
                }
            }

            // Animation Scale
            item {
                Text(
                    text = "SKALA DURASI ANIMASI JENDELA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Kecepatan Transisi Spring iOS", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.5f to "0.5x (Cepat)", 1.0f to "1.0x (Normal)", 2.0f to "2.0x (Lambat)").forEach { (scale, label) ->
                                val isSelected = animScale == scale
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF007AFF) else Color(0xFFE5E5EA))
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        viewModel.setAnimationScale(scale)
                                    }
                                    .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // System Memory & Heap Gauge
            item {
                Text(
                    text = "PROFILER MEMORI (JVM RUNTIME)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Penggunaan RAM Aplikasi", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("$usedMemMb MB / $maxHeapMb MB", fontSize = 14.sp, color = Color(0xFF007AFF), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { (usedMemMb.toFloat() / maxHeapMb.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF007AFF),
                            trackColor = Color(0xFFE5E5EA)
                        )

                        Text("RAM Bebas: $freeMemMb MB • Maksimum Alokasi Heap: $maxHeapMb MB", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Root & Thermal Diagnostics
            item {
                Text(
                    text = "STATUS SUBSISTEM TINGKAT RENDAH",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Akses Superuser (Root)",
                        detail = "Tersedia (/system/xbin/su)",
                        icon = Icons.Rounded.Security,
                        iconBg = Color(0xFFFF3B30)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Status Termal Perangkat",
                        detail = thermalMode,
                        icon = Icons.Rounded.Thermostat,
                        iconBg = Color(0xFF5AC8FA),
                        onClick = {
                            thermalMode = if (thermalMode.contains("Nominal")) "Hangat (Normal Load)" else "Nominal (Dingin)"
                        }
                    )
                }
            }

            // Maintenance Action
            item {
                IOSSettingsCard {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                IOSFeedbackHelper.vibrateSuccess(context)
                                System.gc()
                                viewModel.showDynamicIslandNotification("Pembersihan", "Cache aplikasi & memori berhasil disegarkan", "sparkles", 0xFF34C759)
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bersihkan Cache & Jalankan GC",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
