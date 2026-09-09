package com.example.relab_tool.utils

/** Root mutations are disabled until device-specific units and rollback are verified. */
object RootUtils {
    fun isRootAvailable(): Boolean = false
    fun runShellCommand(command: String): Boolean = false
    fun setChargeLimit(limit: Int): Boolean = false
    fun setGovernor(governor: String): Boolean = false
}
