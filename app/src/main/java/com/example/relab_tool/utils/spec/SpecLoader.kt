package com.example.relab_tool.utils.spec

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SpecLoader {

    private val arrayCache = java.util.concurrent.ConcurrentHashMap<String, org.json.JSONArray>()

    /**
     * Strict SoC ID matching to avoid false positives.
     *
     * Rules (in priority order):
     * 1. Exact match:           "mt6989" == "mt6989"
     * 2. Model is a suffix variant of the ID:
     *    "mt6989v2".startsWith("mt6989") → true
     *    This covers OEM-suffixed model strings.
     *
     * The reverse check `id.contains(model)` is intentionally excluded:
     * a short/generic model like "mt699" would otherwise match inside
     * "mt6991" (Dimensity 9400), producing a completely wrong chip name.
     */
    private fun socIdMatches(id: String, model: String): Boolean =
        id == model || model.startsWith(id)

    fun getSoCName(context: Context, socModel: String): String? {
        val model = socModel.lowercase()
        val files = listOf(
            "cpu_snapdragon.json", "cpu_mtk.json", "cpu_exynos.json",
            "cpu_google.json", "cpu_unisoc.json", "cpu_hisilicon.json", "cpu_xiaomi.json"
        )

        for (fileName in files) {
            val array = getJsonArrayFromAsset(context, fileName) ?: continue
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id").lowercase()

                val isMatch = if (id.contains("|")) {
                    id.split("|").any { socIdMatches(it, model) }
                } else {
                    socIdMatches(id, model)
                }

                if (isMatch) {
                    return formatSoCName(fileName, obj)
                }
            }
        }
        return null
    }

    private fun formatSoCName(fileName: String, obj: JSONObject): String {
        val name = obj.optString("name")
        val brand = when {
            fileName.contains("snapdragon") -> {
                if (name.startsWith("IQ-", ignoreCase = true) || name.startsWith("Q-", ignoreCase = true) || name.contains("qcs", ignoreCase = true) || name.contains("qcm", ignoreCase = true)) {
                    "Qualcomm Dragonwing"
                } else {
                    "Snapdragon"
                }
            }
            fileName.contains("mtk") -> resolveMtkBrand(name)
            fileName.contains("exynos") -> "Exynos"
            fileName.contains("google") -> "Google Tensor"
            fileName.contains("unisoc") -> "Unisoc"
            fileName.contains("hisilicon") -> "Kirin"
            fileName.contains("xiaomi") -> "Xiaomi"
            else -> ""
        }
        return if (brand.isNotEmpty()) "$brand $name" else name
    }

    /**
     * Resolve the correct MediaTek brand from the chip code name:
     * - Dimensity: codes starting with "D" (D700, D1000, D9300, …)
     * - Helio: codes starting with "G", "P", "X", or "A" (G99, P35, X30, A22, …)
     * - Kompanio / tablet SoCs: codes starting with "K"
     * - Fallback: "MediaTek" for anything else
     */
    private fun resolveMtkBrand(chipName: String): String {
        // Strip slashes to check primary code only (e.g. "D8000/8100" -> first token "D8000")
        val primary = chipName.split("/", " ").firstOrNull { it.isNotBlank() } ?: chipName
        return when {
            primary.startsWith("D", ignoreCase = true) -> "Dimensity"
            primary.startsWith("G", ignoreCase = true) -> "Helio"
            primary.startsWith("P", ignoreCase = true) -> "Helio"
            primary.startsWith("X", ignoreCase = true) -> "Helio"
            primary.startsWith("A", ignoreCase = true) -> "Helio"
            primary.startsWith("K", ignoreCase = true) -> "Kompanio"
            else -> "MediaTek"
        }
    }

    fun getCameraVendor(context: Context, sensorName: String): String? {
        val array = getJsonArrayFromAsset(context, "camera_vendors.json") ?: return null
        val lowerName = sensorName.lowercase()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val idPattern = obj.optString("id")
            try {
                if (Regex(idPattern, RegexOption.IGNORE_CASE).containsMatchIn(lowerName)) {
                    return obj.optString("name")
                }
            } catch (e: Exception) {}
        }
        return null
    }

    fun getCpuFamily(context: Context, partId: String, vendorId: String): String? {
        val array = getJsonArrayFromAsset(context, "cpu_family.json") ?: return null
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val vid = obj.optString("vid")
            val id = obj.optString("id")
            if (vid.equals(vendorId, ignoreCase = true) && id.equals(partId, ignoreCase = true)) {
                return obj.optString("name")
            }
        }
        return null
    }

    fun getArmCortexName(context: Context, partId: String): String? {
        val array = getJsonArrayFromAsset(context, "cpu_family_arm.json") ?: return null
        // Ensure partId is hex format for matching if needed, but the JSON seems to use 0x...
        val normalizedPart = if (partId.startsWith("0x")) partId.lowercase() else "0x${partId.lowercase()}"
        
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.optString("id").lowercase()
            if (id == normalizedPart) {
                return obj.optString("name")
            }
        }
        return null
    }

    fun getCameraMaxResolution(context: Context, sensorName: String): String? {
        val files = listOf(
            "sony_cameras_resolution.json", "samsung_cameras_resolution.json",
            "ovx_cameras_resolution.json", "scx_cameras_resolution.json",
            "toshiba_cameras_resolution.json", "sony_lyt_cameras_resolution.json",
            "panasonic_cameras_resolution.json", "custom_cameras_resolution.json"
        )

        val cleanName = sensorName.lowercase().trim()
        if (cleanName.isEmpty()) return null

        // Strip known sensor prefixes for matching against short IDs
        val strippedName = cleanName
            .removePrefix("imx").removePrefix("s5k").removePrefix("ov")
            .removePrefix("hi").removePrefix("gc").removePrefix("ar")
            .removePrefix("lyt-").removePrefix("lyt")

        for (fileName in files) {
            val array = getJsonArrayFromAsset(context, fileName) ?: continue
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id").lowercase().trim()
                if (id.isEmpty()) continue

                // 1. Exact match (full name or stripped)
                val ids = id.split("|")
                if (ids.any { it == cleanName || it == strippedName }) {
                    return obj.optString("res") + " MP"
                }

                // 2. Regex match (handles [] character classes and | alternation)
                try {
                    val regex = Regex("^($id)$")
                    if (regex.matches(cleanName) || regex.matches(strippedName)) {
                        return obj.optString("res") + " MP"
                    }
                } catch (_: Exception) {}

                // 3. Substring match
                if (ids.any { sub -> sub.length >= 3 && (cleanName.contains(sub) || sub.contains(cleanName)) }) {
                    return obj.optString("res") + " MP"
                }
            }
        }
        return null
    }

    fun getSensorDetails(context: Context, sensorName: String): Pair<String, String>? {
        val files = listOf(
            "sony_cameras_resolution.json", "samsung_cameras_resolution.json",
            "ovx_cameras_resolution.json", "scx_cameras_resolution.json",
            "toshiba_cameras_resolution.json", "sony_lyt_cameras_resolution.json",
            "panasonic_cameras_resolution.json", "custom_cameras_resolution.json"
        )

        val cleanName = sensorName.lowercase().trim()
        if (cleanName.isEmpty()) return null

        // Strip known sensor prefixes for matching against short IDs
        val strippedName = cleanName
            .removePrefix("imx").removePrefix("s5k").removePrefix("ov")
            .removePrefix("hi").removePrefix("gc").removePrefix("ar")
            .removePrefix("lyt-").removePrefix("lyt")
            .filter { it.isLetterOrDigit() }

        for (fileName in files) {
            val array = getJsonArrayFromAsset(context, fileName) ?: continue
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val ids = obj.optString("id").lowercase().trim().split("|")
                
                if (ids.any { it == cleanName || it == strippedName || (it.length >= 3 && strippedName.contains(it)) }) {
                    val manufacturer = when {
                        fileName.contains("sony") -> "Sony"
                        fileName.contains("samsung") -> "Samsung"
                        fileName.contains("ovx") -> "OmniVision"
                        fileName.contains("toshiba") -> "Toshiba"
                        fileName.contains("panasonic") -> "Panasonic"
                        else -> getCameraVendor(context, sensorName) ?: "Unknown"
                    }
                    val model = when {
                        manufacturer == "Sony" && !cleanName.startsWith("imx") && !cleanName.startsWith("lyt") -> "IMX$cleanName"
                        manufacturer == "Samsung" && !cleanName.startsWith("s5k") -> "S5K$cleanName"
                        manufacturer == "OmniVision" && !cleanName.startsWith("ov") -> "OV$cleanName"
                        else -> sensorName.uppercase()
                    }
                    return Pair(model, obj.optString("res") + " MP")
                }
            }
        }
        return null
    }

    /**
     * Look up camera sensor info by device model + focal length.
     * Returns Triple(resolution string, sensor name, equiv focal length?) or null.
     */
    fun getDeviceCameraResolution(context: Context, focalLength: Float, facing: String): Triple<String, String, Double?>? {
        val override = getDeviceCameraOverride(context, focalLength, facing)
        return override?.let { Triple(it.res, it.sensor, it.equiv) }
    }

    /**
     * Extended camera override data from the curated device database.
     * Contains all fields that Camera2 API commonly gets wrong.
     */
    data class DeviceCameraOverride(
        val res: String,            // "64 MP"
        val sensor: String,         // "Samsung S5KGW3"
        val equiv: Double?,         // 35mm equiv focal length
        val aperture: Float?,       // f-stop (e.g. 1.79)
        val pixelSize: Float?,      // μm (e.g. 0.7)
        val videoMax: String?,      // "4K@30", "8K@24"
        val sensorSize: String?     // "1/1.97\""
    )

    /**
     * Look up camera override from bundled device_cameras.json.
     * Returns full DeviceCameraOverride or null if device not found.
     */
    fun getDeviceCameraOverride(context: Context, focalLength: Float, facing: String): DeviceCameraOverride? {
        val array = getJsonArrayFromAsset(context, "device_cameras.json") ?: return null
        return lookupCameraInArray(array, focalLength, facing)
    }

    /**
     * Shared lookup logic for both bundled and online JSON arrays.
     * Matches by Build.DEVICE, Build.MODEL, or full manufacturer+model string.
     */
    fun lookupCameraInArray(array: JSONArray, focalLength: Float, facing: String): DeviceCameraOverride? {
        val deviceName = android.os.Build.DEVICE.lowercase()
        val modelName = android.os.Build.MODEL.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val fullId = "$manufacturer $modelName".lowercase()

        for (i in 0 until array.length()) {
            val device = array.getJSONObject(i)
            val patterns = device.optJSONArray("match") ?: continue

            var matched = false
            for (j in 0 until patterns.length()) {
                val pattern = patterns.getString(j).lowercase()
                if (deviceName.contains(pattern) || pattern.contains(deviceName) ||
                    modelName.contains(pattern) || pattern.contains(modelName) ||
                    fullId.contains(pattern) || pattern.contains(fullId)) {
                    matched = true
                    break
                }
            }
            if (!matched) continue

            val cameras = device.optJSONArray("cameras") ?: continue
            for (k in 0 until cameras.length()) {
                val cam = cameras.getJSONObject(k)
                val focalMin = cam.optDouble("focal_min", 0.0).toFloat()
                val focalMax = cam.optDouble("focal_max", 100.0).toFloat()
                val camFacing = cam.optString("facing", "back").lowercase()

                if (focalLength >= focalMin && focalLength <= focalMax &&
                    facing.lowercase().contains(camFacing)) {
                    val res = cam.optString("res") + " MP"
                    val sensor = cam.optString("sensor", "")
                    val rawEquiv = if (cam.has("equiv")) cam.optDouble("equiv", 0.0) else null
                    val equiv = if (rawEquiv != null && rawEquiv > 0) rawEquiv else null
                    val aperture = if (cam.has("aperture")) cam.optDouble("aperture").toFloat() else null
                    val pixelSize = if (cam.has("pixel_size")) cam.optDouble("pixel_size").toFloat() else null
                    val videoMax = if (cam.has("video_max")) cam.optString("video_max") else null
                    val sensorSize = if (cam.has("sensor_size")) cam.optString("sensor_size") else null
                    return DeviceCameraOverride(res, sensor, equiv, aperture, pixelSize, videoMax, sensorSize)
                }
            }
        }
        return null
    }

    fun getSoCProcess(context: Context, socModel: String): String? {
        val model = socModel.lowercase()
        val files = listOf(
            "cpu_snapdragon.json", "cpu_mtk.json", "cpu_exynos.json",
            "cpu_google.json", "cpu_unisoc.json", "cpu_hisilicon.json", "cpu_xiaomi.json"
        )

        for (fileName in files) {
            val array = getJsonArrayFromAsset(context, fileName) ?: continue
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id").lowercase()
                
                val isMatch = if (id.contains("|")) {
                    id.split("|").any { socIdMatches(it, model) }
                } else {
                    socIdMatches(id, model)
                }

                if (isMatch) {
                    val tp = obj.optString("tp", "")
                    if (tp.isNotEmpty()) return "${tp} nm"
                }
            }
        }
        return null
    }

    fun getSoCDdrType(context: Context, socModel: String): String? {
        val model = socModel.lowercase()
        val files = listOf(
            "cpu_snapdragon.json", "cpu_mtk.json", "cpu_exynos.json",
            "cpu_google.json", "cpu_unisoc.json", "cpu_hisilicon.json", "cpu_xiaomi.json"
        )

        for (fileName in files) {
            val array = getJsonArrayFromAsset(context, fileName) ?: continue
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id").lowercase()
                
                val isMatch = if (id.contains("|")) {
                    id.split("|").any { socIdMatches(it, model) }
                } else {
                    socIdMatches(id, model)
                }

                if (isMatch) {
                    val ddr = obj.optString("ddr", "")
                    if (ddr.isNotEmpty()) return if (ddr.startsWith("LPDDR", true)) ddr else "LPDDR$ddr"
                }
            }
        }
        return null
    }

    private fun getJsonArrayFromAsset(context: Context, fileName: String): JSONArray? {
        val cached = arrayCache[fileName]
        if (cached != null) return cached

        return try {
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            arrayCache[fileName] = array
            array
        } catch (e: Exception) {
            null
        }
    }
}
