package com.example.relab_tool.utils

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.relab_tool.utils.AppConfig
import com.google.ai.client.generativeai.GenerativeModel
import org.json.JSONArray

object DeviceNameResolver {
    private const val TAG = "DeviceNameResolver"

    fun resolveDeviceName(context: Context): String {
        // Step 1: Settings.Global "device_name"
        // This is often resolved by GMS or set by the user
        val globalDeviceName = Settings.Global.getString(context.contentResolver, "device_name")
        if (!globalDeviceName.isNullOrEmpty() && !isJustModelCode(globalDeviceName)) {
            return globalDeviceName
        }

        // Step 2: Google Play CSV local DB
        val playStoreName = getPlayStoreMarketingName(context)
        if (!playStoreName.isNullOrEmpty()) {
            return playStoreName
        }

        // Step 3: OEM system props
        val oemName = getOemMarketingName()
        if (!oemName.isNullOrEmpty()) {
            return oemName
        }

        // Step 4: Bluetooth adapter name
        // Users often set their device name here, or it defaults to marketing name
        val bluetoothName = try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothManager?.adapter?.name
        } catch (e: SecurityException) {
            null
        }
        if (!bluetoothName.isNullOrEmpty() && !isJustModelCode(bluetoothName)) {
            return bluetoothName
        }

        // Step 5: Gemini AI (Requires async, so we return a placeholder or fallback for now)
        // Step 6: Fallback to Brand + Model
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * Attempts to resolve the device name using Gemini AI.
     * This is a suspend function as it involves network.
     */
    suspend fun resolveWithGemini(apiKey: String, manufacturer: String, model: String, device: String): String? {
        return try {
            val generativeModel = GenerativeModel(
                modelName = AppConfig.GEMINI_MODEL_NAME,
                apiKey = apiKey
            )
            val prompt = String.format(AppConfig.GEMINI_PROMPT_TEMPLATE, manufacturer, model, device)
            val response = generativeModel.generateContent(prompt)
            response.text?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini resolution failed", e)
            null
        }
    }

    private fun getPlayStoreMarketingName(context: Context): String? {
        return try {
            val json = loadJsonFromAsset(context, AppConfig.ASSET_DEVICES_JSON) ?: return null
            val array = JSONArray(json)
            val model = Build.MODEL
            val device = Build.DEVICE

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val m = obj.optString("model")
                val d = obj.optString("device")
                
                // Match by model or device code
                if (m.equals(model, ignoreCase = true) || d.equals(device, ignoreCase = true)) {
                    val brand = obj.optString("brand")
                    val marketingName = obj.optString("name")
                    
                    return if (marketingName.startsWith(brand, ignoreCase = true)) {
                        marketingName
                    } else {
                        "$brand $marketingName"
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading Google Play devices JSON", e)
            null
        }
    }

    private fun getOemMarketingName(): String? {
        val props = listOf(
            "ro.product.marketname",
            "ro.product.model.marketname",
            "ro.vendor.product.marketname",
            "ro.product.odm.marketname",
            "ro.config.marketing_name",
            "ro.product.display",
            "ro.product.name.display",
            "ro.vivo.marketname",
            "ro.vivo.product.marketname",
            "ro.oppo.market.name",
            "ro.oppo.marketname",
            "ro.huawei.marketname",
            "ro.config.device_name",
            "market.name",
            "ro.product.nickname",
            "ro.product.alias",
            "ro.product.model.display",
            "ro.product.model.market",
            "ro.vendor.product.display",
            "ro.product.vendor.marketname",
            "ro.product.system.marketname",
            "ro.product.product.marketname",
            "persist.sys.device_name",
            "ro.product.brand.display"
        )
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (!value.isNullOrEmpty() && !isJustModelCode(value)) return value
        }
        return null
    }

    private fun isJustModelCode(name: String): Boolean {
        // Heuristic: if name is just Build.MODEL or Build.DEVICE, it's a code
        if (name.equals(Build.MODEL, ignoreCase = true) || name.equals(Build.DEVICE, ignoreCase = true)) return true
        
        // If it's too short and has numbers, it's likely a code (e.g. V2538)
        if (name.length < 10 && name.any { it.isDigit() } && !name.contains(" ")) return true
        
        return false
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val value = get.invoke(systemProperties, key) as String
            if (value.isEmpty()) null else value
        } catch (e: Exception) {
            null
        }
    }

    private fun loadJsonFromAsset(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
