package com.example.util

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object IOSFeedbackHelper {
    var hapticsEnabled: Boolean = true
    var soundsEnabled: Boolean = true

    fun vibrateLight(context: Context) {
        if (!hapticsEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
            }
        } catch (ignored: Exception) {}
    }

    fun vibrateMedium(context: Context) {
        if (!hapticsEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(28, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(28)
                }
            }
        } catch (ignored: Exception) {}
    }

    fun vibrateSuccess(context: Context) {
        if (!hapticsEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 15, 60, 20)
                    val amplitudes = intArrayOf(0, 160, 0, 200)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 15, 60, 20), -1)
                }
            }
        } catch (ignored: Exception) {}
    }

    fun vibrateError(context: Context) {
        if (!hapticsEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 25, 40, 25, 40, 25)
                    val amplitudes = intArrayOf(0, 220, 0, 220, 0, 220)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 25, 40, 25, 40, 25), -1)
                }
            }
        } catch (ignored: Exception) {}
    }

    fun playClickSound(context: Context) {
        if (!soundsEnabled) return
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audio?.playSoundEffect(AudioManager.FX_KEY_CLICK)
        } catch (ignored: Exception) {}
    }

    fun playLockSound(context: Context) {
        if (!soundsEnabled) return
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audio?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.8f)
        } catch (ignored: Exception) {}
    }

    fun playUnlockSound(context: Context) {
        if (!soundsEnabled) return
        try {
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audio?.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 0.7f)
        } catch (ignored: Exception) {}
    }

    fun playRingtonePreview(context: Context, ringtoneName: String) {
        if (!soundsEnabled) return
        try {
            val toneType = when (ringtoneName.lowercase()) {
                "reflection", "refleksi" -> android.media.ToneGenerator.TONE_PROP_BEEP2
                "opening", "pembukaan" -> android.media.ToneGenerator.TONE_CDMA_HIGH_L
                "cosmic", "kosmik" -> android.media.ToneGenerator.TONE_CDMA_MED_PBX_L
                "radar" -> android.media.ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE
                "chimes", "lonceng" -> android.media.ToneGenerator.TONE_PROP_PROMPT
                "circuit", "sirkuit" -> android.media.ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE
                "apex", "puncak" -> android.media.ToneGenerator.TONE_PROP_ACK
                "silk", "sutra" -> android.media.ToneGenerator.TONE_CDMA_LOW_PBX_L
                else -> android.media.ToneGenerator.TONE_PROP_BEEP
            }
            val toneGenerator = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(toneType, 350)
        } catch (ignored: Exception) {}
    }
}
