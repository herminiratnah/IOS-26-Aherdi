package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.IOSPersistenceManager
import com.example.service.IOSOverlayService
import com.example.util.RootExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered automatically upon device startup or reboot.
 * Guarantees that iOS system customizations (disabling SystemUI, CPU Governor,
 * 120Hz ProMotion, and Dynamic Island overlays) persist across restarts!
 */
class IOSBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("IOSBootReceiver", "Boot broadcast received: $action")

        val validBootActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (action !in validBootActions) return

        // Always reset any residual screen overscan on boot so launcher is 100% fullscreen
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RootExecutor.resetTopDisplayInset()

                // Check if Android SystemUI was disabled by user in iRoot
                val isSystemUIDisabled = IOSPersistenceManager.loadRootBoolean(context, "systemui_disabled", false)
                if (isSystemUIDisabled) {
                    Log.d("IOSBootReceiver", "Enforcing persistent SystemUI suppression after reboot")
                    RootExecutor.disableSystemUICompletely()
                } else {
                    val isImmersive = IOSPersistenceManager.loadRootBoolean(context, "immersive", false)
                    if (isImmersive) {
                        RootExecutor.killAndroidBars()
                    }
                }

                // Restore CPU boost if previously activated
                val isCpuBoost = IOSPersistenceManager.loadRootBoolean(context, "cpu_boost", false)
                if (isCpuBoost) {
                    RootExecutor.boostCpuGovernor()
                }

                // Restore 120Hz ProMotion if unlocked
                val is120Hz = IOSPersistenceManager.loadRootBoolean(context, "120hz", false)
                if (is120Hz) {
                    RootExecutor.setHighRefreshRate(true)
                }

                // Restore Thermal engine modification
                val isThermalDisabled = IOSPersistenceManager.loadRootBoolean(context, "thermal_disabled", false)
                if (isThermalDisabled) {
                    RootExecutor.setThermalThrottling(true)
                }

                // Restore SELinux mode
                val isSELinuxPermissive = IOSPersistenceManager.loadRootBoolean(context, "selinux_permissive", false)
                if (isSELinuxPermissive) {
                    RootExecutor.setSELinuxPermissive(true)
                }

                // Start Dynamic Island & Status Bar overlay service
                IOSOverlayService.start(context)
            } catch (e: Exception) {
                Log.e("IOSBootReceiver", "Failed applying root persistence on boot: ${e.message}")
            }
        }
    }
}
