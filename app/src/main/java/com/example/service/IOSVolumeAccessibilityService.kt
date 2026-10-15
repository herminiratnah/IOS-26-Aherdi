package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.viewmodel.DynamicIslandData
import com.example.viewmodel.DynamicIslandMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Global Volume Key Interceptor.
 * Intercepts hardware volume up/down events before Android SystemUI can show
 * its stock volume slider/dialog over third-party apps (WhatsApp, YouTube, Settings, etc.).
 * Dispatches the volume event directly to the sleek iOS Dynamic Island pill.
 */
class IOSVolumeAccessibilityService : AccessibilityService() {

    companion object {
        var instance: IOSVolumeAccessibilityService? = null
            private set
    }

    private var audioManager: AudioManager? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }

    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun triggerBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun triggerHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val audio = audioManager ?: (getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            if (event.action == KeyEvent.ACTION_DOWN && audio != null) {
                val direction = if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }

                val streamType = if (audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION) {
                    AudioManager.STREAM_VOICE_CALL
                } else {
                    AudioManager.STREAM_MUSIC
                }

                // Adjust volume with flag 0: strictly suppresses the Android SystemUI volume dialog
                audio.adjustStreamVolume(streamType, direction, 0)

                val cur = audio.getStreamVolume(streamType)
                val max = audio.getStreamMaxVolume(streamType)
                val pct = if (max > 0) ((cur.toFloat() / max.toFloat()) * 100).toInt() else 0

                // Dispatch to Dynamic Island only if NOT currently in a call (Cellular or WhatsApp/VoIP)
                if (!OverlayBridge.isInCall.value && OverlayBridge.dynamicIslandData.value.mode != DynamicIslandMode.CALL) {
                    OverlayBridge.dynamicIslandData.value = DynamicIslandData(
                        mode = DynamicIslandMode.VOLUME,
                        title = "Volume",
                        subtitle = "$pct%",
                        icon = if (cur == 0) "volume_mute" else "volume",
                        accentColor = 0xFFFFFFFF
                    )

                    // Auto-collapse Dynamic Island back to COMPACT after 2.5s
                    resetJob?.cancel()
                    resetJob = serviceScope.launch {
                        delay(2500)
                        if (OverlayBridge.dynamicIslandData.value.mode == DynamicIslandMode.VOLUME) {
                            OverlayBridge.dynamicIslandData.value = DynamicIslandData(mode = DynamicIslandMode.COMPACT)
                        }
                    }
                }
            }
            // Return true: CONSUMES the event so Android SystemUI never gets it
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
        resetJob?.cancel()
        serviceScope.cancel()
    }
}
