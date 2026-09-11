package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSWebEngineView
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

data class StockItem(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double
)

@Composable
fun StocksApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val airplaneMode by viewModel.airplaneMode.collectAsState()

    var activeTab by remember { mutableStateOf("Watchlist") } // "Watchlist" or "Live Market (Safari)"
    var selectedStock by remember {
        mutableStateOf(StockItem("AAPL", "Apple Inc.", 228.45, +3.12, +1.38))
    }

    val stockList = remember {
        listOf(
            StockItem("AAPL", "Apple Inc.", 228.45, +3.12, +1.38),
            StockItem("NVDA", "NVIDIA Corp.", 124.60, +4.85, +4.05),
            StockItem("MSFT", "Microsoft Corp.", 448.20, +2.10, +0.47),
            StockItem("GOOGL", "Alphabet Inc.", 182.30, -1.25, -0.68),
            StockItem("TSLA", "Tesla Inc.", 252.80, +7.40, +3.02),
            StockItem("AMZN", "Amazon.com Inc.", 191.50, +1.80, +0.95),
            StockItem("BTC-USD", "Bitcoin", 64890.00, +1240.00, +1.95),
            StockItem("S&P 500", "S&P 500 Index", 5588.20, +34.50, +0.62)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                isAirplaneMode = airplaneMode,
                isDarkIcons = false
            )

            // Header & Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stocks",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time market data",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                // Mode switch: Watchlist / Live Web
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Watchlist") IOSBlue else Color.Transparent)
                            .clickable { activeTab = "Watchlist" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Overview", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Live Market (Safari)") IOSBlue else Color.Transparent)
                            .clickable { activeTab = "Live Market (Safari)" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Safari Live", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (activeTab == "Live Market (Safari)") {
                IOSWebEngineView(
                    initialUrl = "https://finance.yahoo.com",
                    title = "Yahoo Finance Live",
                    modifier = Modifier.weight(1f),
                    showHeaderBar = true
                )
            } else {
                // Native Stocks Overview
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Selected Stock Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = selectedStock.symbol,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = selectedStock.name,
                                            fontSize = 14.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$${String.format(java.util.Locale.US, "%.2f", selectedStock.price)}",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        val isPositive = selectedStock.change >= 0
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isPositive) IOSGreen else Color(0xFFFF3B30))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.2f", selectedStock.change)} (${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.2f", selectedStock.changePercent)}%)",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Real-time Chart Indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("1D", "1W", "1M", "1Y", "ALL").forEachIndexed { idx, range ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (idx == 0) Color(0xFF2C2C2E) else Color.Transparent)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(range, color = if (idx == 0) Color.White else Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tickers list
                    items(stockList) { stock ->
                        val isPositive = stock.change >= 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selectedStock.symbol == stock.symbol) Color(0xFF2C2C2E) else Color(0xFF1C1C1E))
                                .clickable { selectedStock = stock }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stock.symbol,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stock.name,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "$${String.format(java.util.Locale.US, "%.2f", stock.price)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .width(76.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPositive) IOSGreen else Color(0xFFFF3B30))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.2f", stock.changePercent)}%",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HomeBar(onGoHome = { viewModel.closeApp() }, isDark = true)
        }
    }
}
