package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.IOSPersistenceManager
import com.example.util.RootExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered automatically upon device startup or reboot.
 * Safely restores system telemetry and root configurations without crashing or
 * blocking Android OS boot sequence.
 */
class IOSBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("IOSBootReceiver", "Boot broadcast received: $action")

        val validBootActions = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (action !in validBootActions) return

        // Run non-blocking background initialization safely
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Give Magisk daemon and system services 2.5 seconds to settle after reboot
                kotlinx.coroutines.delay(2500)

                // Ensure no destructive scripts exist in Magisk service directories
                RootExecutor.cleanMagiskScripts()

                // Reset screen overscan safely
                RootExecutor.resetTopDisplayInset()

                // Restore CPU boost if previously activated
                val isCpuBoost = try {
                    IOSPersistenceManager.loadRootBoolean(context, "cpu_boost", false)
                } catch (_: Throwable) { false }
                if (isCpuBoost) {
                    RootExecutor.boostCpuGovernor()
                }

                // Restore 120Hz ProMotion if unlocked
                val is120Hz = try {
                    IOSPersistenceManager.loadRootBoolean(context, "120hz", false)
                } catch (_: Throwable) { false }
                if (is120Hz) {
                    RootExecutor.setHighRefreshRate(true)
                }

                // Restore Thermal engine modification
                val isThermalDisabled = try {
                    IOSPersistenceManager.loadRootBoolean(context, "thermal_disabled", false)
                } catch (_: Throwable) { false }
                if (isThermalDisabled) {
                    RootExecutor.setThermalThrottling(true)
                }

                // Restore SELinux mode
                val isSELinuxPermissive = try {
                    IOSPersistenceManager.loadRootBoolean(context, "selinux_permissive", false)
                } catch (_: Throwable) { false }
                if (isSELinuxPermissive) {
                    RootExecutor.setSELinuxPermissive(true)
                }

                // Restore volume interception ONLY if user explicitly enabled it in Settings
                val isVolumeInterception = try {
                    IOSPersistenceManager.loadRootBoolean(context, "volume_interception", false)
                } catch (_: Throwable) { false }
                if (isVolumeInterception) {
                    RootExecutor.enableVolumeInterception(context)
                }

                // NOTE: We deliberately do NOT start Foreground Service directly from BroadcastReceiver
                // to prevent ForegroundServiceStartNotAllowedException crashes on Android 12/13/14+.
                // MainActivity (as the default launcher) will start the overlay cleanly upon displaying.
            } catch (e: Throwable) {
                Log.e("IOSBootReceiver", "Safely caught boot error: ${e.message}")
            }
        }
    }
}
