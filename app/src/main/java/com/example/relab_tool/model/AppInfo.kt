package com.example.relab_tool.model

enum class InstallationStatus {
    UNKNOWN,
    NOT_INSTALLED,
    DOWNLOADING,
    READY_TO_INSTALL,
    INSTALLED
}

data class AppInfo(
    val name: String = "",
    val packageName: String,
    val category: String,
    @androidx.annotation.StringRes val nameRes: Int? = null,
    val iconUrl: String? = null,
    val apkUrl: String? = null,
    val status: InstallationStatus = InstallationStatus.UNKNOWN,
    val downloadProgress: Float = 0f,
    val localApkPath: String? = null,
    val installedVersion: String? = null,
    val latestVersion: String? = null,
    val hasUpdate: Boolean = false,
    val isRecommended: Boolean = false
)
