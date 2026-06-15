package com.example.relab_tool.ui

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.R
import com.example.relab_tool.model.AppInfo
import com.example.relab_tool.model.InstallationStatus
import com.example.relab_tool.utils.NetworkUtils
import com.example.relab_tool.data.ApkCrawler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File

class AppInstallerViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    private val packageManager = application.packageManager
    private val crawler = ApkCrawler(application)

    private val downloadIds = mutableMapOf<Long, String>() // downloadId -> packageName

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val packageName = downloadIds[id] ?: return
            
            val query = DownloadManager.Query().setFilterById(id)
            val cursor = downloadManager?.query(query) ?: return
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    val uri = downloadManager?.getUriForDownloadedFile(id)
                    if (uri != null) {
                        _apps.update { currentApps ->
                            currentApps.map { 
                                if (it.packageName == packageName) {
                                    it.copy(status = InstallationStatus.READY_TO_INSTALL, localApkPath = uri.toString())
                                } else it 
                            }
                        }
                        triggerApkInstall(uri)
                    } else {
                        // Fallback to old method if getUriForDownloadedFile fails
                        val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        val file = when {
                            uriString == null -> null
                            uriString.startsWith("file://") -> File(Uri.parse(uriString).path!!)
                            uriString.startsWith("content://") -> null
                            else -> File(uriString)
                        }
                        
                        val localFilename = try {
                            cursor.getString(cursor.getColumnIndexOrThrow("local_filename"))
                        } catch (e: Exception) { null }
                        
                        val finalFile = file ?: localFilename?.let { File(it) }
                        
                        if (finalFile != null) {
                            _apps.update { currentApps ->
                                currentApps.map { 
                                    if (it.packageName == packageName) {
                                        it.copy(status = InstallationStatus.READY_TO_INSTALL, localApkPath = finalFile.absolutePath)
                                    } else it 
                                }
                            }
                            triggerApkInstall(Uri.fromFile(finalFile))
                        }
                    }
                } else if (status == DownloadManager.STATUS_FAILED) {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    val reasonText = when(reason) {
                        DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
                        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
                        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
                        DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
                        DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
                        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
                        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
                        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
                        DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                        else -> "HTTP $reason"
                    }
                    android.util.Log.e("AppInstaller", "Download failed for $packageName. Reason: $reasonText ($reason)")
                    _apps.update { currentApps ->
                        currentApps.map { 
                            if (it.packageName == packageName) it.copy(status = InstallationStatus.NOT_INSTALLED) else it 
                        }
                    }
                }
            }
            cursor.close()
            downloadIds.remove(id)
        }
    }

    init {
        _apps.value = listOf(
            // Benchmark
            AppInfo(nameRes = R.string.app_geekbench, packageName = "com.primatelabs.geekbench6", category = "Benchmark", apkUrl = "https://d.apkpure.com/b/APK/com.primatelabs.geekbench6?version=latest"),
            AppInfo(nameRes = R.string.app_antutu, packageName = "com.antutu.ABenchMark", category = "Benchmark", apkUrl = "https://file.antutu.com/soft/antutu-benchmark-v11.apk"),
            AppInfo(nameRes = R.string.app_3dmark, packageName = "com.futuremark.dmandroid.application", category = "Benchmark", apkUrl = "https://d.apkpure.com/b/APK/com.futuremark.dmandroid.application?version=latest"),

            // Games
            AppInfo(nameRes = R.string.app_genshin_vn, packageName = "com.miHoYo.GenshinImpact.vn", category = "Games", apkUrl = ""),
            AppInfo(nameRes = R.string.app_war_thunder, packageName = "com.gaijinent.warthunder", category = "Games", apkUrl = "https://wtmobile.com/apk"),
            AppInfo(nameRes = R.string.app_fortnite, packageName = "com.epicgames.fortnite", category = "Games"),
            AppInfo(nameRes = R.string.app_garena_df, packageName = "com.garena.game.df", category = "Games"),
            AppInfo(nameRes = R.string.app_chinese_df, packageName = "com.tencent.tmgp.dfm", category = "Games"),
            AppInfo(nameRes = R.string.app_chinese_val, packageName = "com.tencent.tmgp.codev", category = "Games"),
            AppInfo(nameRes = R.string.app_wuthering_waves, packageName = "com.kurogame.wutheringwaves.global", category = "Games", apkUrl = ""),
            AppInfo(nameRes = R.string.app_pubg_vn, packageName = "com.vng.pubgmobile", category = "Games", apkUrl = ""),
            AppInfo(nameRes = R.string.app_wild_rift_vn, packageName = "com.riotgames.league.wildriftvn", category = "Games", apkUrl = ""),
            AppInfo(nameRes = R.string.app_lien_quan, packageName = "com.garena.game.kgvn", category = "Games", apkUrl = ""),
            AppInfo(nameRes = R.string.app_cod_vn, packageName = "com.vng.codmvn", category = "Games", apkUrl = ""),
            AppInfo(nameRes = R.string.app_arknight_vn, packageName = "com.gryphline.endfield.gp.vn", category = "Games"),

            // Utilities
            AppInfo(nameRes = R.string.app_stremio, packageName = "com.stremio.one", category = "Utilities", apkUrl = "https://dl.strem.io/android/v2.1.5/stremio-2.1.5-arm64-v8a.apk"),
            AppInfo(nameRes = R.string.app_aurora_store, packageName = "com.aurora.store", category = "Utilities", apkUrl = "https://files.auroraoss.com/AuroraStore/Stable/AuroraStore-4.8.1.apk"),
            AppInfo(nameRes = R.string.app_apkpure, packageName = "com.apkpure.aegon", category = "Utilities", apkUrl = "https://d.apkpure.com/b/APK/com.apkpure.aegon?version=latest"),
            AppInfo(nameRes = R.string.app_taptap, packageName = "com.taptap.global", category = "Utilities", apkUrl = "https://d.taptap.io/latest"),
            AppInfo(nameRes = R.string.app_epic_games, packageName = "com.epicgames.portal", category = "Utilities"),
            AppInfo(nameRes = R.string.app_xbox, packageName = "com.microsoft.xboxone.smartglass", category = "Utilities", apkUrl = ""),
            AppInfo(nameRes = R.string.app_petal_maps, packageName = "com.huawei.maps.app", category = "Utilities", apkUrl = "https://appgallery.cloud.huawei.com/appdl/C102457337"), 
            AppInfo(nameRes = R.string.app_gamehub, packageName = "com.xiaoji.egggame", category = "Utilities", apkUrl = "https://d.apkpure.com/b/APK/com.xiaoji.egggame?version=latest"),
            AppInfo(nameRes = R.string.app_scene, packageName = "com.omarea.vtools", category = "Utilities", apkUrl = "https://download.omarea.com/scene8/scene_8.3.7.apk"),
            AppInfo(nameRes = R.string.app_gfx_tool, packageName = "eu.tsoml.graphicssettings", category = "Utilities", apkUrl = "https://d.apkpure.com/b/APK/eu.tsoml.graphicssettings?version=latest"),
            AppInfo(nameRes = R.string.app_sai, packageName = "com.aefyr.sai", category = "Utilities", apkUrl = "https://github.com/Aefyr/SAI/releases/download/4.5/SAI-4.5.apk"),
            AppInfo(nameRes = R.string.app_localsend, packageName = "org.localsend.localsend_app", category = "Utilities", apkUrl = "https://github.com/localsend/localsend/releases/download/v1.17.0/LocalSend-1.17.0-android-arm64v8.apk"),

            // System Services
            AppInfo(name = "Huawei Mobile Services", packageName = "com.huawei.hwid", category = "System Services", apkUrl = "https://appgallery.cloud.huawei.com/appdl/C10132067"),
            AppInfo(name = "Huawei AppGallery", packageName = "com.huawei.appmarket", category = "System Services", apkUrl = "https://url.cloud.huawei.com/qbIr42azmg"),
            AppInfo(name = "microG Services", packageName = "com.google.android.gms", category = "System Services", apkUrl = "https://github.com/microg/GmsCore/releases/download/v0.3.15.250932/com.google.android.gms-250932030.apk"), 
            AppInfo(name = "microG Companion", packageName = "com.android.vending", category = "System Services", apkUrl = "https://github.com/microg/GmsCore/releases/download/v0.3.15.250932/com.android.vending-84022630.apk"),
            
            // Social
            AppInfo(nameRes = R.string.app_wechat, packageName = "com.tencent.mm", category = "Social", apkUrl = "https://dldir1.vcloud.qq.com/weixin/android/WeChat.apk"),
            AppInfo(nameRes = R.string.app_qq, packageName = "com.tencent.mobileqq", category = "Social")
        )

        viewModelScope.launch(Dispatchers.IO) {
            // SoC-based Recommendations
            val socModel = com.example.relab_tool.utils.SoCUtils.getSoCModel().lowercase()
            val isHighEnd = socModel.contains("sm8") || socModel.contains("mt69") || socModel.contains("gs3")
            
            _apps.update { currentApps ->
                currentApps.map { app ->
                    if (isHighEnd && app.category == "Benchmark") {
                        app.copy(isRecommended = true)
                    } else if (!isHighEnd && app.name.contains("Lite", ignoreCase = true)) {
                        app.copy(isRecommended = true)
                    } else app
                }
            }
            refreshStatuses()
            _isLoaded.value = true

            // Fetch missing icons from internet (Google Play Store) in parallel
            try {
                coroutineScope {
                    val currentList = _apps.value
                    val updatedList = currentList.map { app ->
                        async {
                            if (app.iconUrl.isNullOrEmpty()) {
                                val fetchedIcon = crawler.getAppIconUrl(app.packageName)
                                if (fetchedIcon != null) {
                                    app.copy(iconUrl = fetchedIcon)
                                } else app
                            } else app
                        }
                    }.awaitAll()
                    _apps.value = updatedList
                }
            } catch (e: Exception) {
                android.util.Log.e("AppInstaller", "Error fetching internet icons", e)
            }
        }
        application.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        
        // Polling for download progress
        viewModelScope.launch {
            while (true) {
                updateDownloadProgress()
                delay(1000)
            }
        }
    }

    fun refreshStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            _apps.update { currentApps ->
                currentApps.map { app ->
                    val isInstalled = isAppInstalled(app.packageName)
                    val nextStatus = if (isInstalled) {
                        InstallationStatus.INSTALLED
                    } else if (app.status != InstallationStatus.DOWNLOADING && app.status != InstallationStatus.READY_TO_INSTALL) {
                        InstallationStatus.NOT_INSTALLED
                    } else {
                        app.status
                    }
                    if (app.status != nextStatus) {
                        app.copy(status = nextStatus)
                    } else app
                }
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun installApp(app: AppInfo) {
        if (app.status == InstallationStatus.READY_TO_INSTALL && app.localApkPath != null) {
            triggerApkInstall(Uri.parse(app.localApkPath))
        } else if (app.apkUrl == "") {
            openPlayStore(app.packageName)
        } else {
            viewModelScope.launch {
                val downloadUrl = app.apkUrl ?: "https://d.apkpure.com/b/APK/${app.packageName}?version=latest"
                val finalUrl = NetworkUtils.resolveRedirect(downloadUrl)
                startApkDownload(app, finalUrl)
            }
        }
    }

    fun openPlayStore(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                setPackage("com.android.vending")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser if Play Store app is not available
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        }
    }

    fun openAlternativeStore(packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            // No store found
        }
    }

    private fun startApkDownload(app: AppInfo, url: String) {
        val uri = Uri.parse(url)
        val referer = if (uri.host?.contains("apkpure") == true) {
            "https://apkpure.com/"
        } else {
            "${uri.scheme}://${uri.host}/"
        }

        // Prevent ERROR_FILE_ALREADY_EXISTS by removing old file
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${app.packageName}.apk")
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle(getApplication<Application>().getString(R.string.downloading_format, app.name))
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
            .addRequestHeader("Referer", referer)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${app.packageName}.apk")

        val dm = downloadManager ?: return
        val id = dm.enqueue(request)
        downloadIds[id] = app.packageName
        
        _apps.update { currentApps ->
            currentApps.map { 
                if (it.packageName == app.packageName) it.copy(status = InstallationStatus.DOWNLOADING) else it 
            }
        }
    }

    private fun updateDownloadProgress() {
        if (downloadIds.isEmpty()) return

        _apps.update { currentApps ->
            currentApps.map { app ->
                val downloadId = downloadIds.entries.find { it.value == app.packageName }?.key
                if (downloadId != null) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager?.query(query) ?: return@map app
                    if (cursor.moveToFirst()) {
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total > 0) {
                            app.copy(downloadProgress = downloaded.toFloat() / total)
                        } else app
                    } else {
                        cursor.close()
                        app
                    }
                } else app
            }
        }
    }

    private fun triggerApkInstall(uri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(uri, "application/vnd.android.package-archive")
        }
        getApplication<Application>().startActivity(installIntent)
    }

    override fun onCleared() {
        super.onCleared()
        try { getApplication<Application>().unregisterReceiver(onDownloadComplete) } catch (_: Throwable) {}
    }
}