package com.example.relab_tool.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.R
import com.example.relab_tool.model.AppInfo
import com.example.relab_tool.model.InstallationStatus
import com.example.relab_tool.data.ApkCrawler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class AppInstallerViewModel(application: Application) : AndroidViewModel(application) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val packageManager = application.packageManager
    private val crawler = ApkCrawler(application)

    init {
        _apps.value = listOf(
            // Benchmark
            AppInfo(nameRes = R.string.app_geekbench, packageName = "com.primatelabs.geekbench6", category = "Benchmark", playStorePackageName = "com.primatelabs.geekbench6", sourceUrl = "https://apkpure.com/geekbench-6/com.primatelabs.geekbench6"),
            AppInfo(nameRes = R.string.app_antutu, packageName = "com.antutu.ABenchMark", category = "Benchmark", playStorePackageName = null, sourceUrl = "https://www.antutu.com/en/index.htm"),
            AppInfo(nameRes = R.string.app_3dmark, packageName = "com.futuremark.dmandroid.application", category = "Benchmark", playStorePackageName = "com.futuremark.dmandroid.application", sourceUrl = "https://apkpure.com/3dmark/com.futuremark.dmandroid.application"),

            // Games
            AppInfo(nameRes = R.string.app_genshin_vn, packageName = "com.miHoYo.GenshinImpact.vn", category = "Games", playStorePackageName = "com.miHoYo.GenshinImpact.vn", sourceUrl = "https://genshin.hoyoverse.com/"),
            AppInfo(nameRes = R.string.app_war_thunder, packageName = "com.gaijinent.warthunder", category = "Games", playStorePackageName = "com.gaijinent.warthunder", sourceUrl = "https://wtmobile.com/"),
            AppInfo(nameRes = R.string.app_fortnite, packageName = "com.epicgames.fortnite", category = "Games", playStorePackageName = null, sourceUrl = "https://www.epicgames.com/fortnite/"),
            AppInfo(nameRes = R.string.app_garena_df, packageName = "com.garena.game.df", category = "Games", playStorePackageName = "com.garena.game.df", sourceUrl = "https://dnf.garena.com/"),
            AppInfo(nameRes = R.string.app_chinese_df, packageName = "com.tencent.tmgp.dfm", category = "Games", playStorePackageName = null, sourceUrl = "https://mdnf.qq.com/"),
            AppInfo(nameRes = R.string.app_chinese_val, packageName = "com.tencent.tmgp.codev", category = "Games", playStorePackageName = null, sourceUrl = "https://val.qq.com/"),
            AppInfo(nameRes = R.string.app_wuthering_waves, packageName = "com.kurogame.wutheringwaves.global", category = "Games", playStorePackageName = "com.kurogame.wutheringwaves.global", sourceUrl = "https://wutheringwaves.kurogames.com/"),
            AppInfo(nameRes = R.string.app_pubg_vn, packageName = "com.vng.pubgmobile", category = "Games", playStorePackageName = "com.vng.pubgmobile", sourceUrl = "https://pubgm.zing.vn/"),
            AppInfo(nameRes = R.string.app_wild_rift_vn, packageName = "com.riotgames.league.wildriftvn", category = "Games", playStorePackageName = "com.riotgames.league.wildriftvn", sourceUrl = "https://tocchien.zing.vn/"),
            AppInfo(nameRes = R.string.app_lien_quan, packageName = "com.garena.game.kgvn", category = "Games", playStorePackageName = "com.garena.game.kgvn", sourceUrl = "https://lienquan.garena.vn/"),
            AppInfo(nameRes = R.string.app_cod_vn, packageName = "com.vng.codmvn", category = "Games", playStorePackageName = "com.vng.codmvn", sourceUrl = "https://codm.vnggames.com/"),
            AppInfo(nameRes = R.string.app_arknight_vn, packageName = "com.gryphline.endfield.gp.vn", category = "Games", playStorePackageName = "com.gryphline.endfield.gp.vn", sourceUrl = "https://endfield.gryphline.com/"),

            // Utilities
            AppInfo(nameRes = R.string.app_stremio, packageName = "com.stremio.one", category = "Utilities", playStorePackageName = "com.stremio.one", sourceUrl = "https://www.stremio.com/"),
            AppInfo(nameRes = R.string.app_aurora_store, packageName = "com.aurora.store", category = "Utilities", playStorePackageName = null, sourceUrl = "https://auroraoss.com/"),
            AppInfo(nameRes = R.string.app_apkpure, packageName = "com.apkpure.aegon", category = "Utilities", playStorePackageName = null, sourceUrl = "https://apkpure.com/"),
            AppInfo(nameRes = R.string.app_taptap, packageName = "com.taptap.global", category = "Utilities", playStorePackageName = null, sourceUrl = "https://www.taptap.io/"),
            AppInfo(nameRes = R.string.app_epic_games, packageName = "com.epicgames.portal", category = "Utilities", playStorePackageName = null, sourceUrl = "https://store.epicgames.com/"),
            AppInfo(nameRes = R.string.app_xbox, packageName = "com.microsoft.xboxone.smartglass", category = "Utilities", playStorePackageName = "com.microsoft.xboxone.smartglass", sourceUrl = "https://www.xbox.com/"),
            AppInfo(nameRes = R.string.app_petal_maps, packageName = "com.huawei.maps.app", category = "Utilities", playStorePackageName = "com.huawei.maps.app", sourceUrl = "https://petalmaps.com"),
            AppInfo(nameRes = R.string.app_gamehub, packageName = "com.xiaoji.egggame", category = "Utilities", playStorePackageName = null, sourceUrl = "https://www.egggame.cn/"),
            AppInfo(nameRes = R.string.app_scene, packageName = "com.omarea.vtools", category = "Utilities", playStorePackageName = null, sourceUrl = "https://github.com/helloklf/vtools"),
            AppInfo(nameRes = R.string.app_gfx_tool, packageName = "eu.tsoml.graphicssettings", category = "Utilities", playStorePackageName = "eu.tsoml.graphicssettings", sourceUrl = "https://apkpure.com/gfx-tool-for-pubg-mobile/eu.tsoml.graphicssettings"),
            AppInfo(nameRes = R.string.app_sai, packageName = "com.aefyr.sai", category = "Utilities", playStorePackageName = "com.aefyr.sai", sourceUrl = "https://github.com/Aefyr/SAI"),
            AppInfo(nameRes = R.string.app_localsend, packageName = "org.localsend.localsend_app", category = "Utilities", playStorePackageName = "org.localsend.localsend_app", sourceUrl = "https://localsend.org"),

            // System Services
            AppInfo(name = "Huawei Mobile Services", packageName = "com.huawei.hwid", category = "System Services", playStorePackageName = "com.huawei.hwid", sourceUrl = "https://appgallery.huawei.com"),
            AppInfo(name = "Huawei AppGallery", packageName = "com.huawei.appmarket", category = "System Services", playStorePackageName = null, sourceUrl = "https://appgallery.huawei.com"),
            AppInfo(name = "microG Services", packageName = "com.google.android.gms", category = "System Services", playStorePackageName = null, sourceUrl = "https://microg.org"),
            AppInfo(name = "microG Companion", packageName = "com.android.vending", category = "System Services", playStorePackageName = null, sourceUrl = "https://microg.org"),

            // Social
            AppInfo(nameRes = R.string.app_wechat, packageName = "com.tencent.mm", category = "Social", playStorePackageName = "com.tencent.mm", sourceUrl = "https://wechat.com"),
            AppInfo(nameRes = R.string.app_qq, packageName = "com.tencent.mobileqq", category = "Social", playStorePackageName = "com.tencent.mobileqq", sourceUrl = "https://im.qq.com")
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
    }

    fun refreshStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            _apps.update { currentApps ->
                currentApps.map { app ->
                    val isInstalled = isAppInstalled(app.packageName)
                    val nextStatus = if (isInstalled) {
                        InstallationStatus.INSTALLED
                    } else {
                        InstallationStatus.NOT_INSTALLED
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

    fun openAppListing(context: Context, app: AppInfo) {
        val packageName = app.playStorePackageName
        val fallbackUrl = app.sourceUrl
        if (packageName != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                    setPackage("com.android.vending")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // Play Store app not installed, fall through to web
            }
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                return
            } catch (e: Exception) {
                // no browser? extremely unlikely, fall through
            }
        }
        // No package name, or app not on Play Store — open source page directly
        if (fallbackUrl.isNotEmpty()) {
            try {
                val sourceIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sourceIntent)
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }
}