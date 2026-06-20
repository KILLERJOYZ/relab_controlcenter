package com.example.relab_tool.utils

import android.content.Context
import android.os.Build
import com.example.relab_tool.utils.spec.SpecLoader
import java.io.File
import java.io.RandomAccessFile

object SoCUtils {

    private val socMap = mapOf(
        // Qualcomm
        "sm8650" to "Snapdragon 8 Gen 3",
        "sm8550" to "Snapdragon 8 Gen 2",
        "sm8450" to "Snapdragon 8 Gen 1",
        "sm8475" to "Snapdragon 8+ Gen 1",
        "sm8350" to "Snapdragon 888",
        "sm8250" to "Snapdragon 865",
        "sm8150" to "Snapdragon 855",
        "sdm845" to "Snapdragon 845",
        "msm8998" to "Snapdragon 835",
        "sm7675" to "Snapdragon 7+ Gen 3",
        "sm7550" to "Snapdragon 7 Gen 3",
        "sm7475" to "Snapdragon 7+ Gen 2",
        "sm7450" to "Snapdragon 7 Gen 1",
        "sm6450" to "Snapdragon 6 Gen 1",
        "sm6435" to "Snapdragon 6s Gen 4",
        "sm4450" to "Snapdragon 4 Gen 2",
        "sm6850" to "Snapdragon 6 Gen 5",
        "sm4850" to "Snapdragon 4 Gen 5",
        "iq9075" to "Qualcomm Dragonwing IQ-9075",
        "q6690" to "Qualcomm Dragonwing Q-6690",
        "qcm6490" to "Qualcomm Dragonwing QCM6490",
        "qcs6490" to "Qualcomm Dragonwing QCS6490",

        // MediaTek Dimensity
        "mt6995" to "Dimensity 9500",
        "mt6995s" to "Dimensity 9500S",
        "d9500s" to "Dimensity 9500S",
        "mt6991" to "Dimensity 9400",
        "mt6989" to "Dimensity 9300",
        "mt6985" to "Dimensity 9200",
        "mt6983" to "Dimensity 9000",
        "mt6899p" to "Dimensity 8550",
        "mt6899s" to "Dimensity 8550",
        "d8550" to "Dimensity 8550",
        "mt6895" to "Dimensity 8100",
        "mt6881" to "Dimensity 7500",
        "d7500" to "Dimensity 7500",
        "mt6893" to "Dimensity 1200",
        "mt6889" to "Dimensity 1000+",
        "mt6877" to "Dimensity 900",
        "mt6833" to "Dimensity 700",
        "mt6833p" to "Dimensity 810",
        // MediaTek Helio
        "mt6789" to "Helio G99",
        "mt6785" to "Helio G95",
        "mt6765" to "Helio P35",
        "mt6768" to "Helio P65",
        "mt6769" to "Helio G70",
        "mt6771" to "Helio P60",
        "mt6781" to "Helio G96",

        // Samsung Exynos
        "s5e9945" to "Exynos 2400",
        "exynos2400" to "Exynos 2400",
        "s5e9925" to "Exynos 2200",
        "exynos2200" to "Exynos 2200",
        "exynos2100" to "Exynos 2100",
        "exynos990" to "Exynos 990",
        "exynos9820" to "Exynos 9820",
        "s5e8845" to "Exynos 1480",
        "exynos1480" to "Exynos 1480",
        "s5e8835" to "Exynos 1380",
        "exynos1380" to "Exynos 1380",
        "s5e8825" to "Exynos 1280",
        "exynos1280" to "Exynos 1280",
        "s5e8535" to "Exynos 1330",
        "exynos1330" to "Exynos 1330",
        "s5e8855" to "Exynos 1580",
        "exynos1580" to "Exynos 1580",
        "s5e8865" to "Exynos 1680",
        "exynos1680" to "Exynos 1680",
        "s5e9955" to "Exynos 2500",
        "exynos2500" to "Exynos 2500",
        "s5e9965" to "Exynos 2600",
        "exynos2600" to "Exynos 2600",
        "exynos8890" to "Exynos 8890",
        "s5e8890" to "Exynos 8890",
        "exynos8895" to "Exynos 8895",
        "s5e8895" to "Exynos 8895",
        "exynos9810" to "Exynos 9810",
        "s5e9810" to "Exynos 9810",
        "exynos9825" to "Exynos 9825",
        "s5e9825" to "Exynos 9825",
        "exynos9611" to "Exynos 9611",
        "s5e9611" to "Exynos 9611",
        "exynos850" to "Exynos 850",
        "s5e3830" to "Exynos 850",
        "exynos1080" to "Exynos 1080",
        "s5e9815" to "Exynos 1080",

        // Huawei Kirin
        "kirin9030" to "Kirin 9030",
        "kirin9100" to "Kirin 9100",

        // Xiaomi XRING
        "o1" to "Xiaomi XRING O1",
        "xring" to "Xiaomi XRING O1",
        "xring o1" to "Xiaomi XRING O1",

        // Google Tensor
        "gs301" to "Google Tensor G3",
        "gs201" to "Google Tensor G2",
        "gs101" to "Google Tensor G1",

        // UNISOC
        "ums9230" to "Unisoc T606",
        "t610" to "Unisoc T610",
        "t612" to "Unisoc T612",
        "t616" to "Unisoc T616",
        "t700" to "Unisoc T700",
        "t8300" to "Unisoc T8300",
        "t820" to "Unisoc T820",
        "t765" to "Unisoc T765",
        "t760" to "Unisoc T760",
        "t750" to "Unisoc T750",
        "t770" to "Unisoc T770"
    )

    @Volatile
    private var cachedCommercialName: String? = null
    @Volatile
    private var cachedWithGpu: Boolean = false

    fun getCommercialName(context: Context, gpuRenderer: String? = null): String {
        val cached = cachedCommercialName
        if (cached != null && (gpuRenderer == null || cachedWithGpu)) {
            return cached
        }
        synchronized(this) {
            if (gpuRenderer == null) {
                cachedCommercialName?.let { return it }
            } else if (cachedWithGpu) {
                cachedCommercialName?.let { return it }
            }

            val model = getSoCModel().lowercase()

            // 1. Try JSON specifications mapping
            var name = SpecLoader.getSoCName(context, model) ?: socMap[model] ?: ""

            // 2. Try prefix matches if still empty
            if (name.isEmpty()) {
                for ((key, value) in socMap) {
                    if (model.contains(key, ignoreCase = true)) {
                        name = value
                        break
                    }
                }
            }

            // 3. Fallback to high-level info if available (API 31+)
            if (name.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val socModel = Build.SOC_MODEL
                if (socModel.isNotEmpty() && socModel != Build.UNKNOWN) {
                    name = SpecLoader.getSoCName(context, socModel.lowercase()) ?: socMap[socModel.lowercase()] ?: socModel
                }
            }

            // 4. Try reading from sysfs
            if (name.isEmpty()) {
                val sysfsMachine = readSysfs("/sys/devices/soc0/machine")
                if (sysfsMachine.isNotEmpty()) name = sysfsMachine
            }

            if (name.isEmpty()) name = getCpuNameFromProc()

            val refined = refineName(name, gpuRenderer, model)
            if (refined.isNotEmpty() && refined != Build.UNKNOWN) {
                cachedCommercialName = refined
                if (gpuRenderer != null) {
                    cachedWithGpu = true
                }
            }
            return refined
        }
    }

    private fun refineName(name: String, gpuRenderer: String? = null, socId: String = ""): String {
        val lowerName = name.lowercase()
        val cores = Runtime.getRuntime().availableProcessors()
        val gpu = gpuRenderer?.lowercase() ?: ""

        // ── 1. Snapdragon 425 / 430 / 435 Fix ──
        // Many devices report "msm8937" (SD430) even when they are "msm8917" (SD425)
        // SD425/427 are Quad-core with Adreno 308
        // SD430/435 are Octa-core with Adreno 505
        if (lowerName.contains("430") || lowerName.contains("435") || lowerName.contains("msm8937") ||
            socId.contains("msm8937") || socId.contains("msm8917")) {
            if (cores <= 4 || gpu.contains("308")) {
                return "Snapdragon 425"
            }
        }

        // ── 2. Dimensity 700 / 810 Fix ──
        // Both use MT6833; distinguished by max CPU freq
        if (lowerName.contains("dimensity 700") ||
            lowerName.contains("d700") ||
            lowerName.contains("mt6833") ||
            socId.contains("mt6833")) {
            val maxFreq = getMaxCpuFreq()
            if (maxFreq >= 2350000) return "Dimensity 810"
            if (maxFreq > 0 && maxFreq < 2350000) return "Dimensity 700"
        }

        // ── 2b. Snapdragon 7s Gen 3 / 7s Gen 4 Fix ──
        // Both use SM7635; distinguished by max CPU freq (Gen 3 max is 2.5 GHz, Gen 4 max is 2.7 GHz)
        if (lowerName.contains("7s gen 3") ||
            lowerName.contains("7s gen 4") ||
            lowerName.contains("sm7635") ||
            socId.contains("sm7635")) {
            val maxFreq = getMaxCpuFreq()
            if (maxFreq >= 2600000) return "Snapdragon 7s Gen 4"
            return "Snapdragon 7s Gen 3"
        }

        // ── 3. Dimensity 9400 / 9500 Series Clarification ──
        // Both series can feature high-clocked Cortex-X925 cores.
        // D9400 (MT6991): Standard @ 3.62 GHz, Plus @ 3.73 GHz.
        // D9500 (MT6995): Flagship @ 4.21 GHz, 9500S (Efficiency) @ 3.73 GHz.
        // 3.73 GHz is the point of overlap where SoC ID or branding is critical.

        if (lowerName.contains("9500") || socId.contains("mt6995")) {
            val maxFreq = getMaxCpuFreq()
            // D9500 flagship is pushed over 4GHz (4.21GHz)
            if (maxFreq >= 4000000) return "Dimensity 9500"
            // D9500S is the efficiency variant typically at 3.73GHz
            if (maxFreq > 0) return "Dimensity 9500S"
        } else if (lowerName.contains("9400") || socId.contains("mt6991")) {
            val maxFreq = getMaxCpuFreq()
            // D9400 Plus is overclocked to 3.73GHz (threshold 3.68GHz)
            if (maxFreq >= 3680000) return "Dimensity 9400 Plus"
            // D9400 standard is 3.62GHz
            if (maxFreq > 0) return "Dimensity 9400"
        }

        // ── 5. Generic "Qualcomm" or "Snapdragon" cleanup ──
        if (name == "Qualcomm" || name == "Snapdragon") {
            val hardware = Build.HARDWARE
            if (hardware != Build.UNKNOWN) return hardware
        }

        return name
    }

    private fun getMaxCpuFreq(): Long {
        var maxFreq = 0L
        try {
            val cpuDir = File("/sys/devices/system/cpu/")
            val cpuFiles = cpuDir.listFiles { _, name -> name.matches(Regex("cpu[0-9]+")) } ?: emptyArray()
            for (file in cpuFiles) {
                val f = File(file, "cpufreq/cpuinfo_max_freq")
                if (f.exists()) {
                    try {
                        val freq = f.readText().trim().toLong()
                        if (freq > maxFreq) maxFreq = freq
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {}
        return maxFreq
    }

    fun getSoCModel(): String {
        // 1. Try Build.SOC_MODEL (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val socModel = Build.SOC_MODEL
            if (socModel.isNotEmpty() && socModel != Build.UNKNOWN) return socModel
        }

        // 2. Try System Properties
        val props = arrayOf("ro.soc.model", "ro.board.platform", "ro.hardware", "ro.mediatek.platform")
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (value.isNotEmpty()) return value
        }

        // 3. Try /sys/devices/soc0/soc_id
        val socId = readSysfs("/sys/devices/soc0/soc_id")
        if (socId.isNotEmpty()) return socId

        // 4. Fallback to Build.HARDWARE or Build.BOARD
        return if (Build.HARDWARE != Build.UNKNOWN) Build.HARDWARE else Build.BOARD
    }

    fun getSoCManufacturer(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vendor = Build.SOC_MANUFACTURER
            if (vendor.isNotEmpty() && vendor != Build.UNKNOWN) return vendor
        }

        val props = arrayOf("ro.soc.manufacturer", "ro.hardware.vendor", "ro.product.manufacturer")
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (value.isNotEmpty()) return value
        }

        return Build.MANUFACTURER
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val result = get.invoke(systemProperties, key) as String
            if (result.isNullOrEmpty()) "" else result
        } catch (e: Exception) {
            ""
        }
    }

    private fun readSysfs(path: String): String {
        return try {
            File(path).readText().trim()
        } catch (e: Exception) {
            ""
        }
    }

    private fun getCpuNameFromProc(): String {
        return try {
            RandomAccessFile("/proc/cpuinfo", "r").use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("Hardware") == true || line?.contains("model name") == true) {
                        return line!!.split(":")[1].trim()
                    }
                }
            }
            Build.HARDWARE
        } catch (e: Exception) {
            Build.HARDWARE
        }
    }
}
