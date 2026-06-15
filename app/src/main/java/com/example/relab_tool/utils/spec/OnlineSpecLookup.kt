package com.example.relab_tool.utils.spec

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Online device camera spec lookup service.
 *
 * Fetches an extended device_cameras_online.json from a GitHub raw URL,
 * caches it locally in SharedPreferences, and provides lookup functions
 * using the same matching logic as the bundled database.
 *
 * This allows new devices to be added to the database without app releases —
 * just commit the updated JSON to the GitHub repository.
 */
object OnlineSpecLookup {
    private const val TAG = "OnlineSpecLookup"

    private const val ONLINE_DB_URL =
        "https://raw.githubusercontent.com/KILLERJOYZ/relab_controlcenter/main/device_cameras_online.json"

    private const val PREF_NAME = "online_spec_cache"
    private const val CACHE_KEY = "device_cameras_json"
    private const val CACHE_TS_KEY = "device_cameras_ts"
    private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000  // 7 days

    @Volatile
    private var cachedArray: JSONArray? = null

    @Volatile
    private var fetchAttempted = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetch the online device camera database and cache it locally.
     * Should be called once per app launch on a background thread.
     *
     * @return The parsed JSONArray, or null on failure
     */
    suspend fun fetchAndCache(context: Context): JSONArray? {
        if (fetchAttempted) return cachedArray
        fetchAttempted = true

        // 1. Check local cache first
        val cached = loadFromCache(context)
        if (cached != null) {
            cachedArray = cached
            Log.d(TAG, "Using cached online database (${cached.length()} devices)")
            return cached
        }

        // 2. Fetch from GitHub
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(ONLINE_DB_URL)
                    .addHeader("Accept", "application/json")
                    .addHeader("Cache-Control", "no-cache")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    response.close()
                    if (body != null) {
                        val array = JSONArray(body)
                        saveToCache(context, body)
                        cachedArray = array
                        Log.i(TAG, "Fetched online database: ${array.length()} devices")
                        return@withContext array
                    }
                } else {
                    response.close()
                    Log.w(TAG, "Online fetch failed: HTTP ${response.code}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Online fetch failed: ${e.message}")
            }
            null
        }
    }

    /**
     * Look up a camera override from the online cached database.
     */
    fun lookupCamera(focalLength: Float, facing: String): SpecLoader.DeviceCameraOverride? {
        val array = cachedArray ?: return null
        return SpecLoader.lookupCameraInArray(array, focalLength, facing)
    }

    /**
     * Get the cached online database (may be null if not yet fetched).
     */
    fun getCachedArray(): JSONArray? = cachedArray

    /**
     * Check if the online database has been loaded (from cache or network).
     */
    fun isAvailable(): Boolean = cachedArray != null

    private fun loadFromCache(context: Context): JSONArray? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val ts = prefs.getLong(CACHE_TS_KEY, 0)
            if (System.currentTimeMillis() - ts > CACHE_TTL_MS) {
                return null  // Cache expired
            }
            val json = prefs.getString(CACHE_KEY, null) ?: return null
            return JSONArray(json)
        } catch (e: Exception) {
            Log.w(TAG, "Cache load failed: ${e.message}")
            return null
        }
    }

    private fun saveToCache(context: Context, json: String) {
        try {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(CACHE_KEY, json)
                .putLong(CACHE_TS_KEY, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Cache save failed: ${e.message}")
        }
    }
}
