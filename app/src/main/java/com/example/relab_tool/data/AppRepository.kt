package com.example.relab_tool.data

import android.content.Context
import android.content.pm.PackageManager
import com.example.relab_tool.R
import com.example.relab_tool.model.AppInfo
import com.example.relab_tool.model.InstallationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private val crawler = ApkCrawler()

    private val _apps = MutableStateFlow<List<AppInfo>>(initialApps())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _apps.update { currentApps ->
            currentApps.map { app ->
                val installedVersion = getInstalledVersion(app.packageName)
                val (latestVersion, downloadUrl) = crawler.getLatestVersionInfo(app.packageName)
                
                app.copy(
                    installedVersion = installedVersion,
                    latestVersion = latestVersion,
                    apkUrl = downloadUrl ?: app.apkUrl,
                    hasUpdate = isUpdateAvailable(installedVersion, latestVersion),
                    status = if (installedVersion != null) InstallationStatus.INSTALLED else InstallationStatus.NOT_INSTALLED
                )
            }
        }
    }

    private fun getInstalledVersion(packageName: String): String? {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun isUpdateAvailable(installed: String?, latest: String?): Boolean {
        if (installed == null || latest == null) return false
        // Simple version comparison logic
        return installed != latest
    }

    private fun initialApps() = listOf(
        AppInfo("Geekbench 6", "com.primatelabs.geekbench6", "Benchmark", nameRes = R.string.app_geekbench),
        AppInfo("Antutu Benchmark", "com.antutu.ABenchMark", "Benchmark", nameRes = R.string.app_antutu),
        AppInfo("3DMark", "com.futuremark.dmandroid.application", "Benchmark", nameRes = R.string.app_3dmark),
        AppInfo("Genshin Impact VN", "com.miHoYo.GenshinImpact.vn", "Games", nameRes = R.string.app_genshin_vn),
        AppInfo("Aurora Store", "com.aurora.store", "Utilities", nameRes = R.string.app_aurora_store)
    )
}
