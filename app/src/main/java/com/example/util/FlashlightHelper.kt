package com.example.util

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FlashlightHelper {
    private var isTorchOn = false

    fun toggleTorch(context: Context, onStateChanged: (Boolean) -> Unit) {
        val targetState = !isTorchOn
        var success = false

        // 1. Camera2 API (Standard Android 8+)
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null) {
                val backCameraWithFlash = cameraManager.cameraIdList.firstOrNull { id ->
                    try {
                        val chars = cameraManager.getCameraCharacteristics(id)
                        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        val isBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                        hasFlash && isBack
                    } catch (e: Exception) {
                        false
                    }
                } ?: cameraManager.cameraIdList.firstOrNull()

                if (backCameraWithFlash != null) {
                    cameraManager.setTorchMode(backCameraWithFlash, targetState)
                    success = true
                }
            }
        } catch (e: Exception) {
            success = false
        }

        // 2. Hardware sysfs fallback for Samsung Galaxy J2 Prime (MediaTek MT6737T / LineageOS)
        if (!success) {
            CoroutineScope(Dispatchers.IO).launch {
                val valStr = if (targetState) "1" else "0"
                val brightnessStr = if (targetState) "255" else "0"
                val paths = listOf(
                    "/sys/class/camera/flash/rear_flash",
                    "/sys/class/leds/flashlight/brightness",
                    "/sys/class/leds/torch-light/brightness",
                    "/sys/class/leds/led:flash_torch/brightness"
                )
                for (p in paths) {
                    RootExecutor.execute("echo $valStr > $p || echo $brightnessStr > $p")
                }
            }
        }

        isTorchOn = targetState
        onStateChanged(isTorchOn)
    }

    fun isTorchActive(): Boolean = isTorchOn

    fun setTorch(context: Context, enabled: Boolean) {
        if (isTorchOn == enabled) return
        toggleTorch(context) { /* state updated */ }
    }
}
