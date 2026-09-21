package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel

@Composable
fun BatteryHealthSubPage(
    viewModel: IOSViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var limit80Percent by remember { mutableStateOf(false) }
    var cleanEnergyCharging by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        SettingsTopBar(title = "Kesehatan Baterai", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Baterai ponsel, seperti semua baterai isi ulang, merupakan komponen habis pakai yang berkurang keefektifannya seiring dengan bertambahnya usia kimiawi baterai.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    lineHeight = 18.sp
                )
            }

            // Maximum Capacity & Peak Capability
            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Kapasitas Maksimum",
                        detail = "98%",
                        icon = Icons.Rounded.BatteryChargingFull,
                        iconBg = Color(0xFF34C759)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Kemampuan Kinerja Puncak",
                        detail = "Normal",
                        icon = Icons.Rounded.Speed,
                        iconBg = Color(0xFF007AFF)
                    )
                }
            }

            item {
                Text(
                    text = "Kapasitas ini adalah ukuran kapasitas baterai dibandingkan saat masih baru. Kapasitas yang lebih rendah dapat menyebabkan lebih sedikit jam penggunaan di antara pengisian daya.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Charging Optimizations
            item {
                Text(
                    text = "PENGOPTIMALAN PENGISIAN DAYA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsToggleRow(
                        title = "Batas Pengisian Daya 80%",
                        checked = limit80Percent,
                        onCheckedChange = {
                            IOSFeedbackHelper.vibrateLight(context)
                            limit80Percent = it
                            viewModel.showDynamicIslandNotification(
                                title = "Pengisian Daya",
                                message = if (it) "Batas 80% aktif untuk menjaga umur baterai" else "Pengisian daya penuh 100% aktif",
                                icon = "battery",
                                accent = 0xFF34C759
                            )
                        },
                        icon = Icons.Rounded.BatterySaver,
                        iconBg = Color(0xFFFF9500)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsToggleRow(
                        title = "Pengisian Daya Energi Bersih",
                        checked = cleanEnergyCharging,
                        onCheckedChange = { cleanEnergyCharging = it },
                        icon = Icons.Rounded.Eco,
                        iconBg = Color(0xFF34C759)
                    )
                }
            }

            // Battery Cycle & Hardware Details
            item {
                Text(
                    text = "INFORMASI SIKLUS BATERAI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            item {
                IOSSettingsCard {
                    IOSSettingsRow(
                        title = "Jumlah Siklus Pengisian",
                        detail = "142 siklus",
                        icon = Icons.Rounded.Loop,
                        iconBg = Color(0xFF5AC8FA)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Tanggal Produksi",
                        detail = "November 2024",
                        icon = Icons.Rounded.CalendarMonth,
                        iconBg = Color(0xFF8E8E93)
                    )
                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                    IOSSettingsRow(
                        title = "Penggunaan Pertama",
                        detail = "Desember 2024",
                        icon = Icons.Rounded.PlayCircle,
                        iconBg = Color(0xFF8E8E93)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
