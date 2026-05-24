package com.example.relab_tool.utils

import java.io.DataOutputStream
import java.io.File

object RootUtils {

    fun isRootAvailable(): Boolean {
        return try {
            File("/system/xbin/su").exists() || File("/system/bin/su").exists()
        } catch (e: Exception) {
            false
        }
    }

    fun runShellCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun setChargeLimit(limit: Int): Boolean {
        // Common paths for charge limit (vendor specific)
        val paths = listOf(
            "/sys/class/power_supply/battery/charge_control_limit_max",
            "/sys/class/power_supply/battery/constant_charge_current_max"
        )
        for (path in paths) {
            if (runShellCommand("echo $limit > $path")) return true
        }
        return false
    }

    fun setGovernor(governor: String): Boolean {
        return runShellCommand("echo $governor > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
    }
}
