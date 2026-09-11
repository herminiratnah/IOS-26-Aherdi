package com.example.service

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class IOSKeyboardService : InputMethodService() {

    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null

    private val keyboardLifecycleOwner = object : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun create() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    override fun onCreate() {
        super.onCreate()
        keyboardLifecycleOwner.create()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onDestroy() {
        keyboardLifecycleOwner.destroy()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this)
        composeView.setViewTreeLifecycleOwner(keyboardLifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(keyboardLifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(keyboardLifecycleOwner)

        composeView.setContent {
            IOSKeyboardUI(
                onKeyPress = { char ->
                    playClickFeedback()
                    currentInputConnection?.commitText(char, 1)
                },
                onDelete = {
                    playClickFeedback()
                    currentInputConnection?.deleteSurroundingText(1, 0)
                },
                onReturn = {
                    playClickFeedback()
                    currentInputConnection?.sendKeyEvent(
                        android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER)
                    )
                    currentInputConnection?.sendKeyEvent(
                        android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER)
                    )
                },
                onSpace = {
                    playClickFeedback()
                    currentInputConnection?.commitText(" ", 1)
                }
            )
        }
        return composeView
    }

    private fun playClickFeedback() {
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f)
        } catch (ignored: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (ignored: Exception) {}
    }
}

enum class KeyboardMode {
    ALPHA_LOWER,
    ALPHA_UPPER,
    NUMBERS,
    SYMBOLS,
    EMOJI
}

@Composable
fun IOSKeyboardUI(
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onReturn: () -> Unit,
    onSpace: () -> Unit
) {
    var mode by remember { mutableStateOf(KeyboardMode.ALPHA_LOWER) }

    val bgLight = Color(0xFFD1D5DB)
    val keyLight = Color.White
    val specialKeyColor = Color(0xFFAFB5BD)
    val textColor = Color(0xFF1C1C1E)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgLight)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Quick emoji suggestion bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("😀", "😂", "🥰", "😍", "👍", "🔥", "❤️", "✨", "🎉", "🙏").forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onKeyPress(emoji) }
                        .padding(4.dp)
                )
            }
        }

        when (mode) {
            KeyboardMode.ALPHA_LOWER, KeyboardMode.ALPHA_UPPER -> {
                val isUpper = mode == KeyboardMode.ALPHA_UPPER
                val row1 = if (isUpper) listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")

                val row2 = if (isUpper) listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")

                val row3 = if (isUpper) listOf("Z", "X", "C", "V", "B", "N", "M")
                else listOf("z", "x", "c", "v", "b", "n", "m")

                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    row1.forEach { char ->
                        IOSKey(
                            label = char,
                            modifier = Modifier.weight(1f),
                            bg = keyLight,
                            textColor = textColor,
                            onClick = { onKeyPress(char) }
                        )
                    }
                }

                // Row 2
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    row2.forEach { char ->
                        IOSKey(
                            label = char,
                            modifier = Modifier.weight(1f),
                            bg = keyLight,
                            textColor = textColor,
                            onClick = { onKeyPress(char) }
                        )
                    }
                }

                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shift Key
                    IOSKey(
                        icon = if (isUpper) Icons.Rounded.ArrowUpward else Icons.Rounded.Navigation,
                        modifier = Modifier.weight(1.3f),
                        bg = if (isUpper) keyLight else specialKeyColor,
                        iconTint = if (isUpper) Color(0xFF007AFF) else textColor,
                        onClick = {
                            mode = if (isUpper) KeyboardMode.ALPHA_LOWER else KeyboardMode.ALPHA_UPPER
                        }
                    )

                    row3.forEach { char ->
                        IOSKey(
                            label = char,
                            modifier = Modifier.weight(1f),
                            bg = keyLight,
                            textColor = textColor,
                            onClick = { onKeyPress(char) }
                        )
                    }

                    // Backspace Key
                    IOSKey(
                        icon = Icons.Rounded.Backspace,
                        modifier = Modifier.weight(1.3f),
                        bg = specialKeyColor,
                        iconTint = textColor,
                        onClick = onDelete
                    )
                }
            }

            KeyboardMode.NUMBERS -> {
                val numRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                val numRow2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
                val numRow3 = listOf(".", ",", "?", "!", "'")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    numRow1.forEach { char ->
                        IOSKey(label = char, modifier = Modifier.weight(1f), bg = keyLight, textColor = textColor, onClick = { onKeyPress(char) })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    numRow2.forEach { char ->
                        IOSKey(label = char, modifier = Modifier.weight(1f), bg = keyLight, textColor = textColor, onClick = { onKeyPress(char) })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    IOSKey(label = "#+=", modifier = Modifier.weight(1.4f), bg = specialKeyColor, textColor = textColor, onClick = { mode = KeyboardMode.SYMBOLS })
                    numRow3.forEach { char ->
                        IOSKey(label = char, modifier = Modifier.weight(1f), bg = keyLight, textColor = textColor, onClick = { onKeyPress(char) })
                    }
                    IOSKey(icon = Icons.Rounded.Backspace, modifier = Modifier.weight(1.4f), bg = specialKeyColor, iconTint = textColor, onClick = onDelete)
                }
            }

            KeyboardMode.SYMBOLS -> {
                val symRow1 = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
                val symRow2 = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
                val symRow3 = listOf(".", ",", "?", "!", "'")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    symRow1.forEach { char ->
                        IOSKey(label = char, modifier = Modifier.weight(1f), bg = keyLight, textColor = textColor, onClick = { onKeyPress(char) })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    symRow2.forEach { char ->
                        IOSKey(label = char, modifier = Modifier.weight(1f), bg = keyLight, textColor = textColor, onClick = { onKeyPress(char) })
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    IOSKey(label = "123", modifier = Modifier.weight(1.4f), bg = specialKeyColor, textColor = textColor, onClick = { mode = KeyboardMode.NUMBERS })
                    symRow3.forEach { char ->
                        IOSKey(label = char, modifier = Modifier.weight(1f), bg = keyLight, textColor = textColor, onClick = { onKeyPress(char) })
                    }
                    IOSKey(icon = Icons.Rounded.Backspace, modifier = Modifier.weight(1.4f), bg = specialKeyColor, iconTint = textColor, onClick = onDelete)
                }
            }

            KeyboardMode.EMOJI -> {
                // Return to alpha
                mode = KeyboardMode.ALPHA_LOWER
            }
        }

        // Bottom Row: 123 / ABC toggle, Globe/Emoji, Space, Return
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isNumberOrSymbol = mode == KeyboardMode.NUMBERS || mode == KeyboardMode.SYMBOLS
            IOSKey(
                label = if (isNumberOrSymbol) "ABC" else "123",
                modifier = Modifier.weight(1.3f),
                bg = specialKeyColor,
                textColor = textColor,
                onClick = {
                    mode = if (isNumberOrSymbol) KeyboardMode.ALPHA_LOWER else KeyboardMode.NUMBERS
                }
            )

            IOSKey(
                icon = Icons.Rounded.Language,
                modifier = Modifier.weight(1f),
                bg = specialKeyColor,
                iconTint = textColor,
                onClick = {
                    mode = if (mode == KeyboardMode.ALPHA_LOWER) KeyboardMode.ALPHA_UPPER else KeyboardMode.ALPHA_LOWER
                }
            )

            // Space bar
            IOSKey(
                label = "space",
                modifier = Modifier.weight(4.5f),
                bg = keyLight,
                textColor = Color.Gray,
                onClick = onSpace
            )

            // Return Key
            IOSKey(
                label = "return",
                modifier = Modifier.weight(2f),
                bg = Color(0xFF007AFF),
                textColor = Color.White,
                onClick = onReturn
            )
        }
    }
}

@Composable
fun IOSKey(
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    bg: Color,
    textColor: Color = Color.Black,
    iconTint: Color = Color.Black,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .shadow(1.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = if (label.length > 2) 15.sp else 21.sp,
                fontWeight = if (label.length > 2) FontWeight.Normal else FontWeight.Medium,
                color = textColor
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
