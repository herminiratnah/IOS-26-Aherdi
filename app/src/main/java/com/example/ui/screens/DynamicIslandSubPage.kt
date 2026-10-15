package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.IOSOverlayService
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun DynamicIslandSubPage(
    viewModel: IOSViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val showNetworkSpeed by viewModel.showNetworkSpeed.collectAsState()
    val showBatteryPercentage by viewModel.showBatteryPercentage.collectAsState()
    val networkSpeed by viewModel.networkSpeed.collectAsState()

    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
            .statusBarsPadding()
    ) {
        SettingsTopBar(
            title = "Dynamic Island",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1C1C1E), Color(0xFF000000))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                        Text(
                            text = "Dynamic Island iPhone",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Menggantikan seluruh tampilan notifikasi dan dialog SystemUI bawaan Android (volume, pengisian daya, panggilan, musik, peta) di seluruh aplikasi seperti WhatsApp, Instagram, dan Setelan.",
                        color = Color(0xFFA1A1A6),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                IOSOverlayService.start(context)
                                viewModel.showDynamicIslandNotification(
                                    "Dynamic Island",
                                    "Aktif di seluruh aplikasi pihak ketiga!",
                                    "apple",
                                    0xFF007AFF
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aktifkan di Seluruh HP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // =================================================================
            // PENGATURAN STATUS BAR & DYNAMIC ISLAND LEBIH POWERFUL
            // =================================================================
            CategoryHeader(
                title = "Pengaturan Status Bar & Dynamic Island (Super Powerful)",
                subtitle = "Gantikan statusbar bawaan Android dengan fitur lengkap & interaktif"
            )
            IOSSettingsCard {
                IOSSettingsToggleRow(
                    title = "Indikator Aktivitas Jaringan (Spinner iPhone)",
                    checked = showNetworkSpeed,
                    onCheckedChange = { viewModel.toggleShowNetworkSpeed(context) },
                    icon = Icons.Rounded.Sync,
                    iconBg = Color(0xFF007AFF)
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                IOSSettingsToggleRow(
                    title = "Tampilkan Persentase Baterai",
                    checked = showBatteryPercentage,
                    onCheckedChange = { viewModel.toggleBatteryPercentage(context) },
                    icon = Icons.Rounded.BatteryChargingFull,
                    iconBg = Color(0xFF34C759)
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Ketuk Ganda Status Bar untuk Kunci",
                    desc = "Ketuk 2x pada status bar kosong untuk langsung mengunci layar",
                    icon = Icons.Rounded.TouchApp,
                    iconBg = Color(0xFF5856D6),
                    onTest = {
                        viewModel.showDynamicIslandNotification("Status Bar Gestures", "Ketuk 2x status bar aktif untuk kunci layar!", "shield", 0xFF5856D6)
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Dual Island (Bubble Terpisah)",
                    desc = "Animasi split bubble terpisah saat Musik + Timer aktif bersamaan",
                    icon = Icons.Rounded.Timer,
                    iconBg = Color(0xFFFF9500),
                    onTest = { viewModel.showDualIslandDemo() }
                )
            }

            // =================================================================
            // 1. PERINGATAN PERANGKAT KERAS & SISTEM
            // =================================================================
            CategoryHeader(
                title = "1. Peringatan Perangkat Keras & Sistem",
                subtitle = "Volume fisik, sakelar hening, Face ID, daya & baterai, sensor"
            )
            IOSSettingsCard {
                DynamicIslandFeatureRow(
                    title = "Volume Nada Dering",
                    desc = "Bilah indikator volume saat menekan tombol fisik",
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    iconBg = Color(0xFF8E8E93),
                    onTest = { viewModel.showDynamicIslandVolume(75) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Mode Hening (Silent)",
                    desc = "Animasi lonceng hening saat geser sakelar / Action Button",
                    icon = Icons.Rounded.NotificationsOff,
                    iconBg = Color(0xFFFF3B30),
                    onTest = { viewModel.showDynamicIslandRinger(isSilent = true) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Mode Dering (Ring)",
                    desc = "Animasi getaran suara dering aktif",
                    icon = Icons.Rounded.NotificationsActive,
                    iconBg = Color(0xFF34C759),
                    onTest = { viewModel.showDynamicIslandRinger(isSilent = false) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Face ID Unlock",
                    desc = "Animasi gembok berputar dan terbuka saat verifikasi wajah",
                    icon = Icons.Rounded.LockOpen,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandFaceId("Buka Kunci Layar") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Face ID Perbankan (BCA)",
                    desc = "Verifikasi wajah keamanan transaksi aplikasi bank",
                    icon = Icons.Rounded.Security,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandFaceId("BCA Mobile Transfer") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Status Pengisian Daya",
                    desc = "Animasi petir & persentase baterai saat charger dicolok",
                    icon = Icons.Rounded.Bolt,
                    iconBg = Color(0xFF34C759),
                    onTest = { viewModel.showDynamicIslandBattery(88, true) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Peringatan Baterai Lemah (20%)",
                    desc = "Peringatan sistem saat baterai tersisa 20%",
                    icon = Icons.Rounded.BatteryAlert,
                    iconBg = Color(0xFFFF3B30),
                    onTest = { viewModel.showDynamicIslandBattery(20, false) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Peringatan Baterai Kritis (10%)",
                    desc = "Peringatan darurat hubungkan ke charger",
                    icon = Icons.Rounded.BatteryAlert,
                    iconBg = Color(0xFFFF3B30),
                    onTest = { viewModel.showDynamicIslandBattery(10, false) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Koneksi Aksesori AirPods Pro",
                    desc = "Status baterai Earbuds L, R, dan Case saat terhubung",
                    icon = Icons.Rounded.Headphones,
                    iconBg = Color(0xFF2C2C2E),
                    onTest = { viewModel.showDynamicIslandAccessory("AirPods Pro", 100, 95, 85) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Indikator Privasi Kamera",
                    desc = "Titik hijau mendeteksi aplikasi sedang menggunakan kamera",
                    icon = Icons.Rounded.CameraAlt,
                    iconBg = Color(0xFF34C759),
                    onTest = { viewModel.showDynamicIslandPrivacy(isCamera = true, isMic = false, appName = "Kamera") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Indikator Privasi Mikrofon",
                    desc = "Titik oranye mendeteksi aplikasi sedang merekam suara",
                    icon = Icons.Rounded.Mic,
                    iconBg = Color(0xFFFF9500),
                    onTest = { viewModel.showDynamicIslandPrivacy(isCamera = false, isMic = true, appName = "WhatsApp Voice") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Akses Jaringan Wi-Fi",
                    desc = "Pemberitahuan koneksi Wi-Fi cepat di Dynamic Island",
                    icon = Icons.Rounded.Wifi,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandNetwork("Wi-Fi", "Terhubung ke Home_5GHz") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Akses Jaringan Bluetooth",
                    desc = "Pemberitahuan koneksi Bluetooth",
                    icon = Icons.Rounded.Bluetooth,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandNetwork("Bluetooth", "Sony WH-1000XM5 Terhubung") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Mode Pesawat (Airplane Mode)",
                    desc = "Status mode pesawat aktif / nonaktif",
                    icon = Icons.Rounded.Flight,
                    iconBg = Color(0xFFFF9500),
                    onTest = { viewModel.showDynamicIslandNetwork("Airplane Mode", "Mode Pesawat Aktif") }
                )
            }

            // =================================================================
            // 2. PANGGILAN & KOMUNIKASI
            // =================================================================
            CategoryHeader(
                title = "2. Panggilan & Komunikasi",
                subtitle = "Panggilan seluler, WhatsApp, FaceTime, AirDrop & Hotspot"
            )
            IOSSettingsCard {
                DynamicIslandFeatureRow(
                    title = "Panggilan Masuk (Cellular)",
                    desc = "Tombol Terima & Tolak interaktif langsung di Dynamic Island",
                    icon = Icons.Rounded.Call,
                    iconBg = Color(0xFF34C759),
                    onTest = { viewModel.onIncomingCallReceived("Dimas Pratama") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Panggilan VoIP WhatsApp",
                    desc = "Mendukung panggilan suara & video WhatsApp",
                    icon = Icons.Rounded.Chat,
                    iconBg = Color(0xFF25D366),
                    onTest = { viewModel.showDynamicIslandVoipCall("WhatsApp", "Siti Rahma", true) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Panggilan VoIP Telegram",
                    desc = "Mendukung panggilan suara Telegram",
                    icon = Icons.Rounded.Send,
                    iconBg = Color(0xFF2AABEE),
                    onTest = { viewModel.showDynamicIslandVoipCall("Telegram", "Ahmad Fauzi", true) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Panggilan Google Meet / Skype",
                    desc = "Mendukung konferensi audio pihak ketiga",
                    icon = Icons.Rounded.Videocam,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandVoipCall("Google Meet", "Meeting Proyek", false) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "AirDrop File Transfer",
                    desc = "Lingkaran progres berputar halus sampai transfer tuntas",
                    icon = Icons.Rounded.NearMe,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandAirDrop("MacBook Pro M3", "IMG_9920.HEIC (14 MB)") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Hotspot Pribadi (Tethering)",
                    desc = "Ikon rantai dan pemberitahuan perangkat yang menambatkan internet",
                    icon = Icons.Rounded.Link,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandHotspot(1, "iPad Pro M4") }
                )
            }

            // =================================================================
            // 3. PEMUTAR MEDIA & AUDIO
            // =================================================================
            CategoryHeader(
                title = "3. Pemutar Media & Audio",
                subtitle = "Artwork mini album, live waveform equalizer & AirPlay output"
            )
            IOSSettingsCard {
                DynamicIslandFeatureRow(
                    title = "Pemutar Musik (Apple Music)",
                    desc = "Artwork mini dan gelombang suara dinamis bergerak",
                    icon = Icons.Rounded.MusicNote,
                    iconBg = Color(0xFFFF2D55),
                    onTest = {
                        viewModel.toggleMusicPlay()
                        viewModel.showMusicInDynamicIsland()
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Kontrol Musik Melebar (Expanded)",
                    desc = "Linimasa lagu (scrubber), Play, Pause, Next, Previous",
                    icon = Icons.Rounded.PlayCircle,
                    iconBg = Color(0xFFFF2D55),
                    onTest = {
                        viewModel.showMusicInDynamicIsland()
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Menu Output Audio (AirPlay)",
                    desc = "Pintasan cepat pindahkan suara ke AirPods, TV, atau Speaker",
                    icon = Icons.Rounded.Airplay,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandAirPlay("AirPods Pro") }
                )
            }

            // =================================================================
            // 4. NAVIGASI & UTILITAS SISTEM
            // =================================================================
            CategoryHeader(
                title = "4. Navigasi & Utilitas Sistem",
                subtitle = "Peta belokan, hitung mundur timer, voice memo, fokus, Find My"
            )
            IOSSettingsCard {
                DynamicIslandFeatureRow(
                    title = "Peta & Belokan (Turn-by-Turn)",
                    desc = "Arah belokan Apple Maps / Google Maps real-time tanpa buka app",
                    icon = Icons.Rounded.Navigation,
                    iconBg = Color(0xFF34C759),
                    onTest = {
                        viewModel.showDynamicIslandNavigation(
                            turnIcon = "turn_right",
                            instruction = "Belok Kanan ke Jl. Jend. Sudirman",
                            distance = "200 m",
                            eta = "14:35 · 12 min"
                        )
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Peta Belokan Bundaran (Roundabout)",
                    desc = "Simulasi petunjuk jalan bundaran keluar ke-2",
                    icon = Icons.Rounded.Refresh,
                    iconBg = Color(0xFF34C759),
                    onTest = {
                        viewModel.showDynamicIslandNavigation(
                            turnIcon = "roundabout",
                            instruction = "Di bundaran, ambil jalur keluar ke-2",
                            distance = "450 m",
                            eta = "14:40 · 17 min"
                        )
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Timer Langsung (Countdown)",
                    desc = "Hitung mundur waktu detik demi detik secara langsung",
                    icon = Icons.Rounded.Timer,
                    iconBg = Color(0xFFFF9500),
                    onTest = { viewModel.startTimer(180) }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Perekam Suara (Voice Memos)",
                    desc = "Durasi & grafik gelombang perekaman audio latar belakang",
                    icon = Icons.Rounded.Mic,
                    iconBg = Color(0xFFFF2D55),
                    onTest = { viewModel.showDynamicIslandVoiceMemo(true, "01:42") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Mode Fokus: Do Not Disturb",
                    desc = "Ikon bulan dan status Do Not Disturb aktif",
                    icon = Icons.Rounded.Nightlight,
                    iconBg = Color(0xFF5856D6),
                    onTest = { viewModel.showDynamicIslandFocus("Do Not Disturb", "moon") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Mode Fokus: Work / Kerja",
                    desc = "Ikon mode kerja aktif",
                    icon = Icons.Rounded.Work,
                    iconBg = Color(0xFF007AFF),
                    onTest = { viewModel.showDynamicIslandFocus("Work Focus", "work") }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Pencarian Lokasi (Find My / AirTag)",
                    desc = "Panah arah presisi dan tombol putar suara pelacak",
                    icon = Icons.Rounded.NearMe,
                    iconBg = Color(0xFF34C759),
                    onTest = { viewModel.showDynamicIslandFindMy("Kunci Rumah (AirTag)", "3.8 m") }
                )
            }

            // =================================================================
            // 5. AKTIVITAS LANGSUNG (LIVE ACTIVITIES)
            // =================================================================
            CategoryHeader(
                title = "5. Aktivitas Langsung (Live Activities)",
                subtitle = "Skor olahraga, ojek online & pengantaran makanan, status penerbangan"
            )
            IOSSettingsCard {
                DynamicIslandFeatureRow(
                    title = "Skor Olahraga (FotMob / Apple Sports)",
                    desc = "Real Madrid 2 - 1 Barcelona · LaLiga 78'",
                    icon = Icons.Rounded.SportsSoccer,
                    iconBg = Color(0xFFFF9500),
                    onTest = {
                        viewModel.showDynamicIslandSports(
                            home = "Real Madrid",
                            away = "Barcelona",
                            homeScore = 2,
                            awayScore = 1,
                            time = "78'"
                        )
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Ojek Online (Gojek / GoRide)",
                    desc = "Pak Joko (B 4521 SKZ) · Driver menuju lokasimu (5 mnt)",
                    icon = Icons.Rounded.TwoWheeler,
                    iconBg = Color(0xFF00AA13),
                    onTest = {
                        viewModel.showDynamicIslandDelivery(
                            service = "Gojek",
                            driver = "Pak Joko",
                            plate = "B 4521 SKZ",
                            status = "Driver menuju lokasimu",
                            eta = "5 mnt"
                        )
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Pengantaran Makanan (GrabFood)",
                    desc = "Pesanan sedang diantar · Tiba dalam 8 menit",
                    icon = Icons.Rounded.Fastfood,
                    iconBg = Color(0xFF00B14F),
                    onTest = {
                        viewModel.showDynamicIslandDelivery(
                            service = "GrabFood",
                            driver = "Budi Hartono",
                            plate = "B 6182 TXA",
                            status = "Makanan sedang diantar",
                            eta = "8 mnt"
                        )
                    }
                )
                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                DynamicIslandFeatureRow(
                    title = "Status Penerbangan (Flighty)",
                    desc = "Garuda GA 412 (CGK ✈ DPS) · Gate 12B · On Time (Tiba 16:45)",
                    icon = Icons.Rounded.FlightTakeoff,
                    iconBg = Color(0xFF007AFF),
                    onTest = {
                        viewModel.showDynamicIslandFlight(
                            code = "GA 412",
                            route = "CGK ✈ DPS",
                            gate = "Gate 12B",
                            status = "On Time"
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CategoryHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(start = 4.dp, top = 6.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color(0xFF8E8E93)
        )
    }
}

@Composable
private fun DynamicIslandFeatureRow(
    title: String,
    desc: String,
    icon: ImageVector,
    iconBg: Color,
    onTest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTest() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color(0xFF8E8E93),
                    lineHeight = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F2F7))
                .clickable { onTest() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Coba",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF007AFF)
            )
        }
    }
}
