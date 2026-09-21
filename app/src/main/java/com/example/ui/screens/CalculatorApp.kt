package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.IOSOrange
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

enum class CalcMode {
    BASIC,
    SCIENTIFIC,
    MATH_NOTES,
    CONVERT
}

data class CalcHistoryItem(
    val equation: String,
    val result: String,
    val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
)

@Composable
fun CalculatorApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()

    var mode by remember { mutableStateOf(CalcMode.BASIC) }
    var isModeMenuOpen by remember { mutableStateOf(false) }
    var isHistoryOpen by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<CalcHistoryItem>() }

    // Core Calculation State
    var displayText by remember { mutableStateOf("0") }
    var previousValue by remember { mutableStateOf<Double?>(null) }
    var currentOperation by remember { mutableStateOf<String?>(null) }
    var isNewEntry by remember { mutableStateOf(true) }
    var isRadMode by remember { mutableStateOf(true) }

    fun onNumber(digit: String) {
        IOSFeedbackHelper.vibrateLight(context)
        if (isNewEntry || displayText == "0") {
            displayText = digit
            isNewEntry = false
        } else {
            if (displayText.length < 12) {
                displayText += digit
            }
        }
    }

    fun onOperator(op: String) {
        IOSFeedbackHelper.vibrateLight(context)
        val current = displayText.toDoubleOrNull() ?: 0.0
        if (previousValue == null) {
            previousValue = current
        } else if (currentOperation != null && !isNewEntry) {
            val result = when (currentOperation) {
                "+" -> previousValue!! + current
                "-" -> previousValue!! - current
                "×" -> previousValue!! * current
                "÷" -> if (current != 0.0) previousValue!! / current else 0.0
                "^" -> previousValue!!.pow(current)
                else -> current
            }
            previousValue = result
            displayText = if (result % 1.0 == 0.0 && !result.isInfinite() && result <= Long.MAX_VALUE && result >= Long.MIN_VALUE) {
                result.toLong().toString()
            } else {
                result.toString()
            }
        }
        currentOperation = op
        isNewEntry = true
    }

    fun onEquals() {
        IOSFeedbackHelper.vibrateMedium(context)
        val current = displayText.toDoubleOrNull() ?: 0.0
        if (previousValue != null && currentOperation != null) {
            val equationStr = "$previousValue $currentOperation $current"
            val result = when (currentOperation) {
                "+" -> previousValue!! + current
                "-" -> previousValue!! - current
                "×" -> previousValue!! * current
                "÷" -> if (current != 0.0) previousValue!! / current else 0.0
                "^" -> previousValue!!.pow(current)
                else -> current
            }
            val formattedResult = if (result % 1.0 == 0.0 && !result.isInfinite() && result <= Long.MAX_VALUE && result >= Long.MIN_VALUE) {
                result.toLong().toString()
            } else {
                result.toString()
            }
            history.add(0, CalcHistoryItem(equationStr, formattedResult))
            displayText = formattedResult
            previousValue = null
            currentOperation = null
            isNewEntry = true
        }
    }

    fun onClear() {
        IOSFeedbackHelper.vibrateLight(context)
        displayText = "0"
        previousValue = null
        currentOperation = null
        isNewEntry = true
    }

    fun onBackspace() {
        if (displayText.length > 1) {
            displayText = displayText.dropLast(1)
            IOSFeedbackHelper.vibrateLight(context)
        } else if (displayText != "0") {
            displayText = "0"
            isNewEntry = true
            IOSFeedbackHelper.vibrateLight(context)
        }
    }

    // Scientific Functions
    fun onScientificFunc(func: String) {
        IOSFeedbackHelper.vibrateLight(context)
        val v = displayText.toDoubleOrNull() ?: 0.0
        val res = when (func) {
            "sin" -> if (isRadMode) sin(v) else sin(Math.toRadians(v))
            "cos" -> if (isRadMode) cos(v) else cos(Math.toRadians(v))
            "tan" -> if (isRadMode) tan(v) else tan(Math.toRadians(v))
            "ln" -> if (v > 0) ln(v) else 0.0
            "log" -> if (v > 0) log10(v) else 0.0
            "sqrt" -> if (v >= 0) sqrt(v) else 0.0
            "sqr" -> v * v
            "cube" -> v * v * v
            "inv" -> if (v != 0.0) 1.0 / v else 0.0
            "pi" -> Math.PI
            "e" -> Math.E
            else -> v
        }
        val formatted = if (res % 1.0 == 0.0 && !res.isInfinite()) res.toLong().toString() else res.toString()
        history.add(0, CalcHistoryItem("$func($v)", formatted))
        displayText = formatted
        isNewEntry = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status bar
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = false
            )

            // Top Header: History icon & Mode title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isHistoryOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = "Riwayat Kalkulasi",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = when (mode) {
                        CalcMode.BASIC -> "Kalkulator"
                        CalcMode.SCIENTIFIC -> "Kalkulator Ilmiah"
                        CalcMode.MATH_NOTES -> "Catatan Matematika"
                        CalcMode.CONVERT -> "Konversi Satuan"
                    },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Placeholder to balance
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Mode Content
            when (mode) {
                CalcMode.BASIC, CalcMode.SCIENTIFIC -> {
                    // Display Area with Swipe to Delete
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (abs(dragAmount) > 25) {
                                        onBackspace()
                                    }
                                }
                            },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            if (previousValue != null && currentOperation != null) {
                                Text(
                                    text = "$previousValue $currentOperation",
                                    color = Color.Gray,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            Text(
                                text = displayText,
                                color = Color.White,
                                fontSize = if (displayText.length > 8) 42.sp else if (displayText.length > 5) 54.sp else 68.sp,
                                fontWeight = FontWeight.Light,
                                textAlign = TextAlign.End,
                                maxLines = 1
                            )
                        }
                    }

                    // Keypads
                    if (mode == CalcMode.SCIENTIFIC) {
                        // Scientific Row Palette
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SciKey("sin") { onScientificFunc("sin") }
                                SciKey("cos") { onScientificFunc("cos") }
                                SciKey("tan") { onScientificFunc("tan") }
                                SciKey("ln") { onScientificFunc("ln") }
                                SciKey("log") { onScientificFunc("log") }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                SciKey(if (isRadMode) "Rad" else "Deg") { isRadMode = !isRadMode }
                                SciKey("√") { onScientificFunc("sqrt") }
                                SciKey("x²") { onScientificFunc("sqr") }
                                SciKey("x^y") { onOperator("^") }
                                SciKey("π") { onScientificFunc("pi") }
                            }
                        }
                    }

                    // Standard Keypad Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Row 1: AC, +/-, %, ÷
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CalcKey(text = if (displayText != "0") "C" else "AC", bg = Color(0xFFA5A5A5), textColor = Color.Black) { onClear() }
                            CalcKey(text = "±", bg = Color(0xFFA5A5A5), textColor = Color.Black) {
                                val v = displayText.toDoubleOrNull() ?: 0.0
                                displayText = (-v).toString().removeSuffix(".0")
                            }
                            CalcKey(text = "%", bg = Color(0xFFA5A5A5), textColor = Color.Black) {
                                val v = displayText.toDoubleOrNull() ?: 0.0
                                displayText = (v / 100.0).toString()
                            }
                            CalcKey(text = "÷", bg = IOSOrange, isSelected = currentOperation == "÷") { onOperator("÷") }
                        }

                        // Row 2: 7, 8, 9, ×
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CalcKey(text = "7") { onNumber("7") }
                            CalcKey(text = "8") { onNumber("8") }
                            CalcKey(text = "9") { onNumber("9") }
                            CalcKey(text = "×", bg = IOSOrange, isSelected = currentOperation == "×") { onOperator("×") }
                        }

                        // Row 3: 4, 5, 6, -
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CalcKey(text = "4") { onNumber("4") }
                            CalcKey(text = "5") { onNumber("5") }
                            CalcKey(text = "6") { onNumber("6") }
                            CalcKey(text = "−", bg = IOSOrange, isSelected = currentOperation == "-") { onOperator("-") }
                        }

                        // Row 4: 1, 2, 3, +
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            CalcKey(text = "1") { onNumber("1") }
                            CalcKey(text = "2") { onNumber("2") }
                            CalcKey(text = "3") { onNumber("3") }
                            CalcKey(text = "+", bg = IOSOrange, isSelected = currentOperation == "+") { onOperator("+") }
                        }

                        // Row 5: Mode Button, 0, ., =
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mode Switcher Button (iOS 18 Icon)
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C2E))
                                    .clickable { isModeMenuOpen = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Calculate,
                                    contentDescription = "Ubah Mode",
                                    tint = IOSOrange,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 0 Key
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF333333))
                                    .clickable { onNumber("0") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("0", color = Color.White, fontSize = 30.sp)
                            }

                            // Decimal
                            CalcKey(text = ".") {
                                if (!displayText.contains(".")) {
                                    displayText += "."
                                    isNewEntry = false
                                }
                            }

                            // Equals
                            CalcKey(text = "=", bg = IOSOrange) { onEquals() }
                        }
                    }
                }

                CalcMode.MATH_NOTES -> {
                    MathNotesView(
                        modifier = Modifier.weight(1f),
                        onSwitchMode = { isModeMenuOpen = true }
                    )
                }

                CalcMode.CONVERT -> {
                    UnitConverterView(
                        modifier = Modifier.weight(1f),
                        onSwitchMode = { isModeMenuOpen = true }
                    )
                }
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                onOpenAppSwitcher = { viewModel.openAppSwitcher() },
                isDark = false
            )
        }

        // Mode Switcher Sheet/Dialog
        if (isModeMenuOpen) {
            AlertDialog(
                onDismissRequest = { isModeMenuOpen = false },
                containerColor = Color(0xFF2C2C2E),
                title = {
                    Text("Pilih Mode Kalkulator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeItem(
                            title = "Kalkulator Dasar",
                            subtitle = "Operasi aritmatika standar",
                            icon = Icons.Rounded.Calculate,
                            isSelected = mode == CalcMode.BASIC,
                            onClick = { mode = CalcMode.BASIC; isModeMenuOpen = false }
                        )
                        ModeItem(
                            title = "Kalkulator Ilmiah",
                            subtitle = "Fungsi trigonometri, logaritma & pangkat",
                            icon = Icons.Rounded.Functions,
                            isSelected = mode == CalcMode.SCIENTIFIC,
                            onClick = { mode = CalcMode.SCIENTIFIC; isModeMenuOpen = false }
                        )
                        ModeItem(
                            title = "Catatan Matematika (Math Notes)",
                            subtitle = "Evaluasi persamaan otomatis ala iOS 18",
                            icon = Icons.Rounded.EditNote,
                            isSelected = mode == CalcMode.MATH_NOTES,
                            onClick = { mode = CalcMode.MATH_NOTES; isModeMenuOpen = false }
                        )
                        ModeItem(
                            title = "Konversi Satuan",
                            subtitle = "Mata uang, panjang, berat, suhu",
                            icon = Icons.Rounded.SwapHoriz,
                            isSelected = mode == CalcMode.CONVERT,
                            onClick = { mode = CalcMode.CONVERT; isModeMenuOpen = false }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isModeMenuOpen = false }) {
                        Text("Tutup", color = IOSOrange)
                    }
                }
            )
        }

        // Calculation History Sheet
        if (isHistoryOpen) {
            AlertDialog(
                onDismissRequest = { isHistoryOpen = false },
                containerColor = Color(0xFF2C2C2E),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Riwayat Kalkulasi", color = Color.White, fontWeight = FontWeight.Bold)
                        if (history.isNotEmpty()) {
                            TextButton(onClick = { history.clear() }) {
                                Text("Hapus", color = Color(0xFFFF453A), fontSize = 13.sp)
                            }
                        }
                    }
                },
                text = {
                    if (history.isEmpty()) {
                        Text("Belum ada riwayat perhitungan.", color = Color.Gray)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(history) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF3A3A3C))
                                        .clickable {
                                            displayText = item.result
                                            isNewEntry = true
                                            isHistoryOpen = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(item.equation, color = Color.Gray, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("= ${item.result}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isHistoryOpen = false }) {
                        Text("Selesai", color = IOSOrange)
                    }
                }
            )
        }
    }
}

@Composable
fun CalcKey(
    text: String,
    bg: Color = Color(0xFF333333),
    textColor: Color = Color.White,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val keyBg = if (isSelected) Color.White else bg
    val keyText = if (isSelected) IOSOrange else textColor

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(keyBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = keyText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun SciKey(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(62.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color(0xFF2C2C2E))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ModeItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF3A3A3C) else Color.Transparent)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) IOSOrange else Color(0xFF48484A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        if (isSelected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = IOSOrange, modifier = Modifier.size(18.dp))
        }
    }
}

// iOS 18 Math Notes Feature
@Composable
fun MathNotesView(modifier: Modifier = Modifier, onSwitchMode: () -> Unit) {
    val mathEquations = remember {
        mutableStateListOf(
            "Biaya Tiket = 250000 * 3 = 750.000",
            "Diskon = 15% * 750000 = 112.500",
            "Total Akhir = 750000 - 112500 = 637.500",
            "Luas Lingkaran = π * 14² = 615.75"
        )
    }
    var newMathInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Catatan Matematika Aktif", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onSwitchMode) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = IOSOrange)
            }
        }
        Text("Ketik persamaan matematika dengan '=' untuk hasil instan", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mathEquations) { eq ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                        .border(1.dp, IOSOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = eq,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Equation Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newMathInput,
                onValueChange = { newMathInput = it },
                placeholder = { Text("Contoh: 15 * 45 =", color = Color.Gray, fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IOSOrange,
                    unfocusedBorderColor = Color.DarkGray
                ),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            Button(
                onClick = {
                    if (newMathInput.isNotBlank()) {
                        // Simple dynamic solver
                        val cleaned = newMathInput.trim().removeSuffix("=")
                        val result = try {
                            val parts = cleaned.split(" ")
                            if (parts.size >= 3) {
                                val a = parts[0].toDoubleOrNull() ?: 0.0
                                val op = parts[1]
                                val b = parts[2].toDoubleOrNull() ?: 0.0
                                when (op) {
                                    "+" -> "${a + b}"
                                    "-" -> "${a - b}"
                                    "*" -> "${a * b}"
                                    "/" -> "${a / b}"
                                    else -> "Dihitung"
                                }
                            } else {
                                "Selesai"
                            }
                        } catch (e: Exception) {
                            "OK"
                        }
                        mathEquations.add("$newMathInput $result")
                        newMathInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IOSOrange)
            ) {
                Text("Hitung")
            }
        }
    }
}

// iOS 18 Unit Converter Feature
@Composable
fun UnitConverterView(modifier: Modifier = Modifier, onSwitchMode: () -> Unit) {
    var inputValue by remember { mutableStateOf("100") }
    var categoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf("Mata Uang", "Panjang", "Berat", "Suhu")

    val num = inputValue.toDoubleOrNull() ?: 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Konverter Satuan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onSwitchMode) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = IOSOrange)
            }
        }

        // Category Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEachIndexed { index, cat ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (categoryIndex == index) IOSOrange else Color(0xFF2C2C2E))
                        .clickable { categoryIndex = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cat,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (categoryIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Input Value
        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text("Nilai Masukan", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = IOSOrange,
                unfocusedBorderColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Conversion Results Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (categoryIndex) {
                0 -> {
                    // Currency: USD base
                    ConversionRow(label = "Dolar AS (USD)", value = String.format(Locale.US, "$%.2f", num))
                    ConversionRow(label = "Rupiah Indonesia (IDR)", value = String.format(Locale.US, "Rp%,d", (num * 15850).toLong()))
                    ConversionRow(label = "Euro (EUR)", value = String.format(Locale.US, "€%.2f", num * 0.92))
                    ConversionRow(label = "Yen Jepang (JPY)", value = String.format(Locale.US, "¥%,d", (num * 152).toLong()))
                }
                1 -> {
                    // Length: Meter base
                    ConversionRow(label = "Meter (m)", value = "$num m")
                    ConversionRow(label = "Kilometer (km)", value = "${num / 1000.0} km")
                    ConversionRow(label = "Kaki (Feet)", value = String.format(Locale.US, "%.2f ft", num * 3.28084))
                    ConversionRow(label = "Inci (Inches)", value = String.format(Locale.US, "%.2f in", num * 39.3701))
                }
                2 -> {
                    // Weight: Kg base
                    ConversionRow(label = "Kilogram (kg)", value = "$num kg")
                    ConversionRow(label = "Gram (g)", value = "${num * 1000} g")
                    ConversionRow(label = "Pon (lbs)", value = String.format(Locale.US, "%.2f lbs", num * 2.20462))
                    ConversionRow(label = "Ons (oz)", value = String.format(Locale.US, "%.2f oz", num * 35.274))
                }
                3 -> {
                    // Temperature: Celsius base
                    ConversionRow(label = "Celsius (°C)", value = "$num °C")
                    ConversionRow(label = "Fahrenheit (°F)", value = String.format(Locale.US, "%.1f °F", (num * 9 / 5) + 32))
                    ConversionRow(label = "Kelvin (K)", value = String.format(Locale.US, "%.1f K", num + 273.15))
                }
            }
        }
    }
}

@Composable
fun ConversionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
