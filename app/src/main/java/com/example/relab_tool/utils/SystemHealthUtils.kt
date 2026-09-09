package com.example.relab_tool.utils

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

object SystemHealthUtils {

    fun getStorageSmartStatus(): Int = com.example.relab_tool.R.string.unknown

    fun check16kbCompatibility(context: Context, packageName: String): Boolean {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val file = File(appInfo.nativeLibraryDir)
            // Heuristic: Apps with native libs need explicit 16KB alignment in Android 15.
            // If it has native libs, we mark it as "Review Needed" or similar.
            // A true check involves parsing ELF headers.
            file.exists() && file.list()?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }
}
