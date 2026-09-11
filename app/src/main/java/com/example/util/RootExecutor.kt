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

    /**
     * Checks if SU can be executed and granted root (uid=0).
     * Caches the result to prevent repeated process creation lag.
     */
    fun isRootAvailable(): Boolean {
        cachedRootAvailable?.let { return it }

        val hasRoot = try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine() ?: ""
            val exitCode = process.waitFor()
            (exitCode == 0 && line.contains("uid=0")).also { ok ->
                if (ok) cachedWorkingShell = "su"
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
        if (!hasRoot && cachedWorkingShell == null) {
            cachedWorkingShell = "sh"
        }
        return hasRoot
    }

    /**
     * Synchronous command execution helper.
     * Uses cached working shell to avoid iterating failing su binaries.
     */
    fun execute(cmd: String, forceRoot: Boolean = true): Pair<Boolean, String> {
        val workingShell = cachedWorkingShell
        val shellsToTry = when {
            workingShell != null -> listOf(workingShell)
            forceRoot -> listOf("su", "sh")
            else -> listOf("sh")
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
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    cachedWorkingShell = shell
                    return Pair(true, output.toString().trim())
                }
            } catch (ignored: Exception) {}
        }
        return Pair(false, "Command failed: $cmd")
    }

    /**
     * Executes a command via su or sh, returns stdout + stderr and success status.
     * Always attempts su first when forceRoot is true.
     */
    suspend fun executeCommand(cmd: String, forceRoot: Boolean = true): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val workingShell = cachedWorkingShell
            val shellsToTry = when {
                workingShell != null -> listOf(workingShell)
                forceRoot -> listOf("su", "sh")
                else -> listOf("sh")
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
                    // Write command followed by exit code check and exit
                    os.writeBytes("$cmd\n")
                    os.writeBytes("echo __RET__$?\n")
                    os.writeBytes("exit\n")
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
                        stdErr.append(line).append("\n")
                    }

                    val exitVal = process.waitFor()
                    val combined = (stdOut.toString() + stdErr.toString()).trim()

                    if (returnCode == 0 || exitVal == 0) {
                        success = true
                        output.append(if (combined.isNotEmpty()) combined else "Executed successfully (exit code 0)")
                        break
                    } else if (returnCode != -1) {
                        success = (returnCode == 0)
                        output.append(if (combined.isNotEmpty()) combined else "Process exited with code $returnCode")
                        if (success) break
                    } else {
                        output.append(if (combined.isNotEmpty()) combined else "Exited with code $exitVal")
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
     * Hides Android Status Bar & Navigation Bar system-wide via policy_control
     */
    suspend fun killAndroidBars(): Pair<Boolean, String> {
        val cmd = """
            settings put global policy_control immersive.full=*
            settings put secure immersive_mode_confirmations confirmed
            am broadcast -a com.android.systemui.statusbar.phone.COLLAPSE_PANELS
            pkill -9 -f com.android.systemui || killall -9 com.android.systemui
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Disables SystemUI package completely & blocks top pull-down swipe shade (100% pure iPhone - no Android status/nav bar ever)
     */
    suspend fun disableSystemUICompletely(): Pair<Boolean, String> {
        val cmd = """
            # Stop SystemUI process completely from processing touch/swipes
            pkill -STOP -f com.android.systemui || killall -STOP com.android.systemui || true
            # Block statusbar pull down via StatusBarManager service call
            service call statusbar 1 i32 1073741823 || true
            service call statusbar 2 i32 1073741823 || true
            # Disable SystemUI package (user 0 and global)
            pm disable-user --user 0 com.android.systemui || pm disable com.android.systemui || true
            settings put global policy_control immersive.full=*
            settings put secure immersive_mode_confirmations confirmed
            am broadcast -a com.android.systemui.statusbar.phone.COLLAPSE_PANELS || true
            cmd statusbar collapse || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Restores Android default bars and re-enables SystemUI
     */
    suspend fun restoreAndroidBars(): Pair<Boolean, String> {
        val cmd = """
            pkill -CONT -f com.android.systemui || killall -CONT com.android.systemui || true
            pm enable com.android.systemui || pm default-state --user 0 com.android.systemui || true
            service call statusbar 1 i32 0 || true
            service call statusbar 2 i32 0 || true
            am startservice -n com.android.systemui/.SystemUIService || true
            settings put global policy_control null*
            wm overscan reset || true
        """.trimIndent()
        return executeCommand(cmd)
    }

    /**
     * Automatically grants notification listener permission to our iOS launcher via root
     */
    suspend fun grantNotificationListenerRoot(packageName: String): Pair<Boolean, String> {
        val serviceName = "$packageName/com.example.service.IOSNotificationListenerService"
        val cmd = "cmd notification allow_listener $serviceName || true; settings put secure enabled_notification_listeners \"\$(settings get secure enabled_notification_listeners):$serviceName\""
        return executeCommand(cmd)
    }

    /**
     * Boosts CPU Governor to performance mode for all CPU cores (Mediatek MT6737T / Exynos 3475)
     */
    suspend fun boostCpuGovernor(): Pair<Boolean, String> {
        val cmd = "for g in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \"${'$'}g\"; done"
        return executeCommand(cmd)
    }

    /**
     * Clears cached RAM and compacts ZRAM for Samsung J2 Prime (1.5GB RAM)
     */
    suspend fun optimizeJ2PrimeRam(): Pair<Boolean, String> {
        val cmd = "sync; echo 3 > /proc/sys/vm/drop_caches; echo 1 > /proc/sys/vm/compact_memory"
        return executeCommand(cmd)
    }

    /**
     * Spoofs model and build prop to iPhone SE 2nd Gen
     */
    suspend fun spoofIphoneModel(model: String = "iPhone SE (2nd generation)"): Pair<Boolean, String> {
        val cmd = """
            setprop ro.product.model "$model"
            setprop ro.build.display.id "iOS 26.0.1 (24A301)"
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
     * Reboots device in chosen mode
     */
    suspend fun rebootDevice(mode: String = "system"): Pair<Boolean, String> {
        return when (mode) {
            "recovery" -> executeCommand("reboot recovery")
            "soft" -> executeCommand("killall -9 zygote")
            else -> executeCommand("reboot")
        }
    }
}
