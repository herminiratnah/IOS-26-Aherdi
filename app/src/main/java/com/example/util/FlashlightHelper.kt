package com.example.util

import android.content.Context
import android.hardware.camera2.CameraManager

object FlashlightHelper {
    private var isTorchOn = false

    fun toggleTorch(context: Context, onStateChanged: (Boolean) -> Unit) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                isTorchOn = !isTorchOn
                cameraManager.setTorchMode(cameraId, isTorchOn)
                onStateChanged(isTorchOn)
            } else {
                isTorchOn = !isTorchOn
                onStateChanged(isTorchOn)
            }
        } catch (e: Exception) {
            isTorchOn = !isTorchOn
            onStateChanged(isTorchOn)
        }
    }

    fun isTorchActive(): Boolean = isTorchOn
}
