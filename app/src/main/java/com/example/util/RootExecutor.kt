package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootExecutor {
    private const val TAG = "RootExecutor"

    @Volatile
    private var cachedRootAvailable: Boolean? = null

    @Volatile
    private var cachedWorkingShell: String? = null

    private fun Process.waitForTimeout(timeoutMs: Long): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return try {
                waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            } catch (_: Exception) {
                false
            }
        }
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                try { Thread.sleep(50) } catch (_: InterruptedException) { return false }
            }
        }
        return false
    }

    /**
     * Checks if SU can be executed and granted root (uid=0).
     * Caches the result to prevent repeated process creation lag, but allows re-verification.
     */
    fun isRootAvailable(): Boolean {
        cachedRootAvailable?.let { return it }

        val hasRoot = try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine() ?: ""
            val finished = process.waitForTimeout(1500)
            if (finished && process.exitValue() == 0 && line.contains("uid=0")) {
                cachedWorkingShell = "su"
                true
            } else {
                try { process.destroy() } catch (_: Exception) {}
                false
            }
        } catch (e: Exception) {
            // Check known root binary paths as fallback
            val paths = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su/bin/su",
                "/magisk/.core/bin/su"
            )
            paths.any {
                try {
                    File(it).exists()
                } catch (ignored: Exception) {
                    false
                }
            }
        }

        cachedRootAvailable = hasRoot
        if (hasRoot) {
            cachedWorkingShell = "su"
        }
        return hasRoot
    }

    /**
     * Synchronous command execution helper.
     * Always attempts su binaries first when forceRoot is true.
     */
    fun execute(cmd: String, forceRoot: Boolean = true): Pair<Boolean, String> {
        val shellsToTry = if (forceRoot) {
            listOf("su", "/system/bin/su", "/system/xbin/su", "/sbin/su", "sh")
        } else {
            val ws = cachedWorkingShell
            if (ws != null) listOf(ws) else listOf("sh")
        }

        for (shell in shellsToTry) {
            try {
                val process = Runtime.getRuntime().exec(shell)
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("$cmd\n")
                os.writeBytes("exit\n")
                os.flush()
                val output = StringBuilder()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                val finished = process.waitForTimeout(2500)
                if (finished && process.exitValue() == 0) {
                    if (shell.contains("su")) {
                        cachedWorkingShell = "su"
                        cachedRootAvailable = true
                    }
                    return Pair(true, output.toString().trim())
                } else if (!finished) {
                    try { process.destroy() } catch (_: Exception) {}
                }
            } catch (ignored: Exception) {}
        }
        return Pair(false, "Command failed: $cmd")
    }

    /**
     * Executes a command via su or sh, returns stdout + stderr and success status.
     * Always attempts su first when forceRoot is true to ensure superuser permissions.
     */
    suspend fun executeCommand(cmd: String, forceRoot: Boolean = true): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val shellsToTry = if (forceRoot) {
                listOf("su", "/system/bin/su", "/system/xbin/su", "/sbin/su", "sh")
            } else {
                val ws = cachedWorkingShell
                if (ws != null) listOf(ws) else listOf("sh")
            }

            var success = false
            val output = StringBuilder()
            var lastException: Exception? = null

            for (shell in shellsToTry) {
                var process: Process? = null
                var os: DataOutputStream? = null
                var reader: BufferedReader? = null
                var errReader: BufferedReader? = null

                try {
                    process = Runtime.getRuntime().exec(shell)
                    os = DataOutputStream(process.outputStream)
                    // Execute wrapped command followed by exit code check and exit
                    val payload = "$cmd\necho __RET__$?\nexit\n"
                    os.write(payload.toByteArray(Charsets.UTF_8))
                    os.flush()

                    reader = BufferedReader(InputStreamReader(process.inputStream))
                    errReader = BufferedReader(InputStreamReader(process.errorStream))

                    val stdOut = StringBuilder()
                    val stdErr = StringBuilder()
                    var line: String?
                    var returnCode = -1

                    while (reader.readLine().also { line = it } != null) {
                        if (line!!.startsWith("__RET__")) {
                            returnCode = line!!.removePrefix("__RET__").trim().toIntOrNull() ?: 0
                        } else {
                            stdOut.append(line).append("\n")
                        }
                    }

                    while (errReader.readLine().also { line = it } != null) {
                        val errLine = line!!
                        // Filter out benign system notices on older/newer Android releases
                        if (!errLine.contains("cmd: not found") &&
                            !errLine.contains("unknown command: overscan") &&
                            !errLine.contains("No such file or directory")
                        ) {
                            stdErr.append(errLine).append("\n")
                        }
                    }

                    val finished = process.waitForTimeout(3000)
                    val exitVal = if (finished) process.exitValue() else -1
                    if (!finished) {
                        try { process.destroy() } catch (_: Exception) {}
                    }
                    val outText = stdOut.toString().trim()
                    val errText = stdErr.toString().trim()

                    if (returnCode == 0 || exitVal == 0) {
                        success = true
                        if (shell.contains("su")) {
                            cachedWorkingShell = "su"
                            cachedRootAvailable = true
                        }
                        val finalMsg = if (outText.isNotEmpty()) outText else if (errText.isNotEmpty()) errText else "Executed successfully (exit code 0)"
                        output.append(finalMsg)
                        break
                    } else if (returnCode != -1) {
                        success = (returnCode == 0)
                        val finalMsg = if (outText.isNotEmpty()) outText else if (errText.isNotEmpty()) errText else "Process exited with code $returnCode"
                        output.append(finalMsg)
                        if (success) {
                            if (shell.contains("su")) {
                                cachedWorkingShell = "su"
                                cachedRootAvailable = true
                            }
                            break
                        }
                    } else {
                        val finalMsg = if (outText.isNotEmpty()) outText else if (errText.isNotEmpty()) errText else "Exited with code $exitVal"
                        output.append(finalMsg)
                    }
                } catch (e: Exception) {
                    lastException = e
                    continue
                } finally {
                    try { os?.close() } catch (ignored: Exception) {}
                    try { reader?.close() } catch (ignored: Exception) {}
                    try { errReader?.close() } catch (ignored: Exception) {}
                    try { process?.destroy() } catch (ignored: Exception) {}
                }
            }

            if (!success && output.isEmpty()) {
                val errMsg = lastException?.localizedMessage ?: "SU not granted or binary inaccessible"
                output.append("Execution error: $errMsg")
            }

            Pair(success, output.toString().trim())
        }

    // Specific root tools tailored for Samsung Galaxy J2 Prime & Magisk

    /**
     * Hides Android Navigation Bar and blanks stock Status Bar icons cleanly (via icon_blacklist)
     * while preserving Android system dialogs (USB plugged-in popup, permissions, volume prompt).
     */
    suspend fun killAndroidBars(): Pair<Boolean, String> {
        val cmd = """
            settings put secure immersive_mode_confirmations confirmed
            settings put secure icon_blacklist wifi,battery,clock,signal,bluetooth,headset,alarm,zen,volume,location,mute,speakerphone,nfc,hotspot,all
            which cmd >/dev/null 2>&1 && cmd statusbar collapse 2>/dev/null || true
            am broadcast -a com.android.systemui.statusbar.phone.COLLAPSE_PANELS 2>/dev/null || true
            wm overscan reset 2>/dev/null || wm overscan 0,0,0,0 2>/dev/null || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Cleans up any destructive boot scripts to prevent OS crash on restart
     */
    suspend fun cleanMagiskScripts(): Pair<Boolean, String> {
        val cmd = """
            rm -f /data/adb/service.d/iroot_init.sh /data/adb/post-fs-data.d/iroot_init.sh /data/iroot_boot.sh 2>/dev/null || true
            pm unhide com.android.systemui 2>/dev/null || true
            pm enable com.android.systemui 2>/dev/null || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Safely suppresses Android stock status bar icons via icon_blacklist and immersive control
     * without killing or disabling com.android.systemui (which would break Home, Back, and cause boot crashes).
     */
    suspend fun disableSystemUICompletely(): Pair<Boolean, String> {
        val cmd = """
            # 1. Clean any old destructive scripts
            rm -f /data/adb/service.d/iroot_init.sh /data/adb/post-fs-data.d/iroot_init.sh /data/iroot_boot.sh 2>/dev/null || true

            # 2. Ensure SystemUI is running for navigation and input handling
            pm unhide com.android.systemui 2>/dev/null || true
            pm enable com.android.systemui 2>/dev/null || true

            # 3. Suppress all stock status bar icons cleanly
            settings put secure icon_blacklist wifi,battery,clock,signal,bluetooth,headset,alarm,zen,volume,location,mute,speakerphone,nfc,hotspot,all
            settings put secure immersive_mode_confirmations confirmed
            wm overscan reset 2>/dev/null || wm overscan 0,0,0,0 2>/dev/null || true

            which cmd >/dev/null 2>&1 && cmd statusbar collapse 2>/dev/null || true
            am broadcast -a com.android.systemui.statusbar.phone.COLLAPSE_PANELS 2>/dev/null || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Restores Android default bars and re-enables SystemUI, removes persistent boot scripts
     */
    suspend fun restoreAndroidBars(): Pair<Boolean, String> {
        val cmd = """
            rm -f /data/adb/service.d/iroot_init.sh /data/adb/post-fs-data.d/iroot_init.sh /data/iroot_boot.sh 2>/dev/null || true
            pkill -CONT -f com.android.systemui 2>/dev/null || killall -CONT com.android.systemui 2>/dev/null || true
            pm unhide com.android.systemui 2>/dev/null || true
            pm enable com.android.systemui 2>/dev/null || true
            am startservice -n com.android.systemui/.SystemUIService 2>/dev/null || true
            wm overscan reset 2>/dev/null || wm overscan 0,0,0,0 2>/dev/null || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Resets screen overscan so Status bar and Dynamic Island remain at the very top of physical screen: Y = 0
     */
    suspend fun resetTopDisplayInset(): Pair<Boolean, String> {
        val cmd = "wm overscan reset 2>/dev/null || wm overscan 0,0,0,0 2>/dev/null || true"
        return executeCommand(cmd)
    }

    /**
     * Ensures dedicated space by keeping physical display at 0,0,0,0
     */
    suspend fun setTopDisplayInset(topPx: Int = 0): Pair<Boolean, String> {
        val cmd = "wm overscan reset 2>/dev/null || wm overscan 0,0,0,0 2>/dev/null || true"
        return executeCommand(cmd)
    }

    /**
     * Automatically grants notification listener permission to our iOS launcher via root
     * Compatible with Android 6.0 (Marshmallow / J2 Prime) through Android 14+
     */
    suspend fun grantNotificationListenerRoot(packageName: String): Pair<Boolean, String> {
        val serviceName = "$packageName/com.example.service.IOSNotificationListenerService"
        val cmd = """
            cur="${'$'}(settings get secure enabled_notification_listeners)"
            if [ "${'$'}cur" = "null" ] || [ -z "${'$'}cur" ]; then
                settings put secure enabled_notification_listeners "$serviceName"
            else
                case "${'$'}cur" in
                    *"$serviceName"*) ;;
                    *) settings put secure enabled_notification_listeners "${'$'}cur:$serviceName" ;;
                esac
            fi
            cmd notification allow_listener "$serviceName" 2>/dev/null || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Optimizes CPU Governor to power-efficient balanced mode (schedutil / interactive)
     * Resets minimum frequency to lowest hardware clock so CPU can sleep and charge coolly
     */
    suspend fun boostCpuGovernor(): Pair<Boolean, String> {
        val cmd = """
            for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do
                avail="${'$'}{g%/*}/scaling_available_governors"
                if [ -f "${'$'}avail" ]; then
                    if grep -q "schedutil" "${'$'}avail" 2>/dev/null; then
                        echo schedutil > "${'$'}g" 2>/dev/null || true
                    elif grep -q "interactive" "${'$'}avail" 2>/dev/null; then
                        echo interactive > "${'$'}g" 2>/dev/null || true
                    elif grep -q "ondemand" "${'$'}avail" 2>/dev/null; then
                        echo ondemand > "${'$'}g" 2>/dev/null || true
                    fi
                fi
            done
            for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
                min_f="${'$'}{f%/*}/cpuinfo_min_freq"
                if [ -f "${'$'}min_f" ]; then
                    cat "${'$'}min_f" > "${'$'}f" 2>/dev/null || true
                fi
            done
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Restores optimal charging and thermal management so battery charges quickly and stays cool
     */
    suspend fun restoreHealthyChargingState(): Pair<Boolean, String> {
        val cmd = """
            start thermal-engine 2>/dev/null || true
            start sec-thermald 2>/dev/null || true
            start thermald 2>/dev/null || true
            start thermalservice 2>/dev/null || true
            chmod 644 /sys/class/thermal/thermal_zone*/temp 2>/dev/null || true
            setprop sys.thermal.policy 1 2>/dev/null || true
            for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_min_freq; do
                min_f="${'$'}{f%/*}/cpuinfo_min_freq"
                if [ -f "${'$'}min_f" ]; then
                    cat "${'$'}min_f" > "${'$'}f" 2>/dev/null || true
                fi
            done
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Clears cached RAM and compacts ZRAM for Samsung J2 Prime (1.5GB RAM)
     */
    suspend fun optimizeJ2PrimeRam(): Pair<Boolean, String> {
        val cmd = "sync; echo 3 > /proc/sys/vm/drop_caches; echo 1 > /proc/sys/vm/compact_memory 2>/dev/null || true"
        return executeCommand(cmd)
    }

    /**
     * Spoofs model and build prop to iPhone SE 2nd Gen / iPhone 16 Pro Max
     */
    suspend fun spoofIphoneModel(model: String = "iPhone SE (2nd generation)"): Pair<Boolean, String> {
        val cmd = """
            resetprop ro.product.model "$model" 2>/dev/null || setprop ro.product.model "$model"
            resetprop ro.product.device "iPhone" 2>/dev/null || setprop ro.product.device "iPhone"
            resetprop ro.product.brand "Apple" 2>/dev/null || setprop ro.product.brand "Apple"
            resetprop ro.product.manufacturer "Apple" 2>/dev/null || setprop ro.product.manufacturer "Apple"
            resetprop ro.build.display.id "iOS 26.0.1 (24A301)" 2>/dev/null || setprop ro.build.display.id "iOS 26.0.1 (24A301)"
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Sets screen density to crisp iPhone Retina DPI
     */
    suspend fun setIosRetinaDpi(dpi: Int = 320): Pair<Boolean, String> {
        return executeCommand("wm density $dpi")
    }

    /**
     * Resets screen density back to default J2 Prime (240 DPI)
     */
    suspend fun resetDpi(): Pair<Boolean, String> {
        return executeCommand("wm density reset")
    }

    /**
     * Restarts SystemUI without full device reboot (Soft Restart)
     */
    suspend fun restartSystemUI(): Pair<Boolean, String> {
        val cmd = "pkill -9 -f com.android.systemui || killall -9 com.android.systemui; sleep 1; am startservice -n com.android.systemui/.SystemUIService || true"
        return executeCommand(cmd)
    }

    /**
     * Shuts down device via root
     */
    suspend fun powerOffDevice(): Pair<Boolean, String> {
        return executeCommand("reboot -p || svc power shutdown || poweroff")
    }

    /**
     * Sets SELinux mode (0 = Permissive, 1 = Enforcing)
     */
    suspend fun setSELinuxPermissive(permissive: Boolean): Pair<Boolean, String> {
        val mode = if (permissive) 0 else 1
        return executeCommand("setenforce $mode")
    }

    /**
     * Unlocks 120Hz / High Refresh Rate in Android system settings
     */
    suspend fun setHighRefreshRate(enable: Boolean): Pair<Boolean, String> {
        val rate = if (enable) "120.0" else "60.0"
        val cmd = """
            settings put system peak_refresh_rate $rate 2>/dev/null || true
            settings put system min_refresh_rate $rate 2>/dev/null || true
            settings put system user_refresh_rate ${if (enable) 120 else 60} 2>/dev/null || true
            settings put global peak_refresh_rate $rate 2>/dev/null || true
            settings put secure refresh_rate_mode ${if (enable) 2 else 0} 2>/dev/null || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Disables Thermal Throttling for maximum gaming performance
     */
    suspend fun setThermalThrottling(disabled: Boolean): Pair<Boolean, String> {
        val cmd = if (disabled) {
            "stop thermal-engine 2>/dev/null; stop sec-thermald 2>/dev/null; stop thermald 2>/dev/null; stop thermalservice 2>/dev/null; setprop sys.thermal.policy 0 2>/dev/null; chmod 000 /sys/class/thermal/thermal_zone*/temp 2>/dev/null || true"
        } else {
            "start thermal-engine 2>/dev/null; start sec-thermald 2>/dev/null; start thermald 2>/dev/null || true"
        }
        return executeCommand(cmd)
    }

    /**
     * Reboots device in chosen mode
     */
    suspend fun rebootDevice(mode: String = "system"): Pair<Boolean, String> {
        return when (mode) {
            "recovery" -> executeCommand("reboot recovery")
            "soft" -> executeCommand("pkill -9 -f com.android.systemui || killall -9 zygote")
            else -> executeCommand("reboot || svc power reboot")
        }
    }

    /**
     * Automatically enables IOSVolumeAccessibilityService using root to suppress stock Android volume dialog
     * and redirect hardware volume keys 100% to the sleek iOS Dynamic Island pill.
     */
    suspend fun enableVolumeInterception(context: Context): Pair<Boolean, String> {
        val serviceComponent = "${context.packageName}/${com.example.service.IOSVolumeAccessibilityService::class.java.name}"
        val cmd = """
            settings put secure accessibility_enabled 1
            cur=${'$'}(settings get secure enabled_accessibility_services)
            if [ -z "${'$'}cur" ] || [ "${'$'}cur" = "null" ]; then
                settings put secure enabled_accessibility_services "$serviceComponent"
            else
                case "${'$'}cur" in
                    *"$serviceComponent"*) ;;
                    *) settings put secure enabled_accessibility_services "${'$'}cur:$serviceComponent" ;;
                esac
            fi
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Checks if volume interception accessibility service is currently enabled.
     */
    fun isVolumeInterceptionEnabled(context: Context): Boolean {
        return try {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val serviceName = com.example.service.IOSVolumeAccessibilityService::class.java.name
            enabledServices.contains(serviceName)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Disables volume interception accessibility service using root.
     */
    suspend fun disableVolumeInterception(context: Context): Pair<Boolean, String> {
        val serviceComponent = "${context.packageName}/${com.example.service.IOSVolumeAccessibilityService::class.java.name}"
        val cmd = """
            cur=${'$'}(settings get secure enabled_accessibility_services)
            new_val=${'$'}(echo "${'$'}cur" | sed "s|:$serviceComponent||g; s|$serviceComponent:||g; s|$serviceComponent||g")
            settings put secure enabled_accessibility_services "${'$'}new_val"
        """.trimIndent()
        return executeCommand(cmd)
    }
}
