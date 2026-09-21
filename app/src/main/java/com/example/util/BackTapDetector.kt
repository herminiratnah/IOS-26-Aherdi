package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

/**
 * Lightweight hardware accelerometer detector for iOS Back Tap feature.
 * Detects sharp double and triple taps on the back of the device with minimal CPU usage.
 */
class BackTapDetector(
    private val context: Context,
    private val onDoubleTap: () -> Unit,
    private val onTripleTap: () -> Unit = {}
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastZ = 0f
    private var lastTime = 0L
    private var tapCount = 0
    private var lastTapTimestamp = 0L
    private var cooldownUntil = 0L

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val evaluateTapRunnable = Runnable {
        if (tapCount == 2) {
            onDoubleTap()
        } else if (tapCount >= 3) {
            onTripleTap()
        }
        tapCount = 0
    }

    var isRunning = false
        private set

    fun start() {
        if (isRunning || accelerometer == null) return
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        handler.removeCallbacks(evaluateTapRunnable)
        sensorManager?.unregisterListener(this)
        isRunning = false
        tapCount = 0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        if (now < cooldownUntil) return

        val z = event.values[2]
        val deltaZ = abs(z - lastZ)
        val timeDelta = now - lastTime

        lastZ = z
        lastTime = now

        // Sharp impulse along Z-axis (back of phone tap)
        if (deltaZ > 12.0f && timeDelta < 250) {
            val sinceLastTap = now - lastTapTimestamp
            if (sinceLastTap in 120..550) {
                tapCount++
                lastTapTimestamp = now
                handler.removeCallbacks(evaluateTapRunnable)
                if (tapCount >= 3) {
                    cooldownUntil = now + 700L
                    handler.post(evaluateTapRunnable)
                } else {
                    handler.postDelayed(evaluateTapRunnable, 350L)
                }
            } else if (sinceLastTap > 600) {
                tapCount = 1
                lastTapTimestamp = now
                handler.removeCallbacks(evaluateTapRunnable)
                handler.postDelayed(evaluateTapRunnable, 500L)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
