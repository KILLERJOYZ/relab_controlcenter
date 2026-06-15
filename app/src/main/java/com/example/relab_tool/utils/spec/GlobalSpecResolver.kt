package com.example.relab_tool.utils.spec

import android.content.Context
import android.util.Log
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object GlobalSpecResolver {
    private const val TAG = "GlobalSpecResolver"
    private const val PREF_NAME = "global_spec_resolver_cache"
    private const val CACHE_KEY_PREFIX = "resolved_specs_"

    // Data class representing parsed camera specifications from GSMArena
    data class ParsedCameraSpec(
        val facing: String,         // "back" or "front"
        val res: String,            // e.g., "50 MP" or "108 MP"
        val sensor: String,         // e.g., "Sony IMX766" (if parsed, else empty)
        val equiv: Double?,         // 35mm equivalent focal length (e.g. 23.0)
        val aperture: Float?,       // f-stop (e.g. 1.8)
        val pixelSize: Float?,      // μm (e.g. 1.0)
        val sensorSize: String?,    // e.g., "1/1.56\""
        val role: String            // "wide", "ultrawide", "telephoto", "macro", "depth"
    ) {
        fun toJSONObject(): JSONObject {
            val obj = JSONObject()
            obj.put("facing", facing)
            obj.put("res", res)
            obj.put("sensor", sensor)
            obj.put("equiv", equiv ?: JSONObject.NULL)
            obj.put("aperture", aperture?.toDouble() ?: JSONObject.NULL)
            obj.put("pixelSize", pixelSize?.toDouble() ?: JSONObject.NULL)
            obj.put("sensorSize", sensorSize ?: JSONObject.NULL)
            obj.put("role", role)
            return obj
        }

        companion object {
            fun fromJSONObject(obj: JSONObject): ParsedCameraSpec {
                val equivVal = obj.opt("equiv")
                val equiv = if (equivVal != null && equivVal != JSONObject.NULL) obj.optDouble("equiv") else null
                
                val apertureVal = obj.opt("aperture")
                val aperture = if (apertureVal != null && apertureVal != JSONObject.NULL) obj.optDouble("aperture").toFloat() else null
                
                val pixelSizeVal = obj.opt("pixelSize")
                val pixelSize = if (pixelSizeVal != null && pixelSizeVal != JSONObject.NULL) obj.optDouble("pixelSize").toFloat() else null
                
                val sensorSizeVal = obj.opt("sensorSize")
                val sensorSize = if (sensorSizeVal != null && sensorSizeVal != JSONObject.NULL) obj.optString("sensorSize") else null

                return ParsedCameraSpec(
                    facing = obj.getString("facing"),
                    res = obj.getString("res"),
                    sensor = obj.optString("sensor", ""),
                    equiv = if (equiv != null && !equiv.isNaN()) equiv else null,
                    aperture = aperture,
                    pixelSize = pixelSize,
                    sensorSize = sensorSize,
                    role = obj.getString("role")
                )
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Resolve camera specifications for the current device.
     * Checks local SharedPreferences cache first, otherwise queries search engine + GSMArena.
     */
    suspend fun resolveSpecs(context: Context): List<ParsedCameraSpec>? {
        val modelKey = "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_").lowercase()
        val cacheKey = CACHE_KEY_PREFIX + modelKey

        // 1. Check local cache
        val cached = loadFromCache(context, cacheKey)
        if (cached != null) {
            Log.d(TAG, "Loaded dynamic specifications from cache for $modelKey (${cached.size} lenses)")
            return cached
        }

        // 2. Perform online lookup
        return withContext(Dispatchers.IO) {
            Log.i(TAG, "No cached specs found for $modelKey. Starting online resolution...")
            val resolved = fetchSpecsFromNetwork(Build.MANUFACTURER, Build.MODEL)
            if (resolved != null && resolved.isNotEmpty()) {
                saveToCache(context, cacheKey, resolved)
                resolved
            } else {
                null
            }
        }
    }

    private fun fetchSpecsFromNetwork(manufacturer: String, model: String): List<ParsedCameraSpec>? {
        try {
            // Step 1: Query DuckDuckGo HTML search for gsmarena link
            val query = "site:gsmarena.com $manufacturer $model"
            val searchUrl = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            
            Log.d(TAG, "Searching search engine: $searchUrl")
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val searchResponse = client.newCall(searchRequest).execute()
            if (!searchResponse.isSuccessful) {
                Log.w(TAG, "Search request failed with HTTP ${searchResponse.code}")
                searchResponse.close()
                return null
            }

            val searchBody = searchResponse.body?.string() ?: ""
            searchResponse.close()

            // Decode URL-encoded strings in HTML results
            val decodedHtml = try {
                java.net.URLDecoder.decode(searchBody, "UTF-8")
            } catch (e: Exception) {
                searchBody
            }

            // Find first GSMArena phone spec URL
            val gsmArenaRegex = """https://(?:www\.)?gsmarena\.com/[a-zA-Z0-9_-]+-\d+\.php""".toRegex()
            val gsmArenaMatch = gsmArenaRegex.find(decodedHtml)
            val gsmArenaUrl = gsmArenaMatch?.value

            if (gsmArenaUrl == null) {
                Log.w(TAG, "Could not find a valid GSMArena spec URL in search results.")
                return null
            }

            Log.i(TAG, "Found target specifications page: $gsmArenaUrl")

            // Step 2: Fetch the GSMArena page
            val specRequest = Request.Builder()
                .url(gsmArenaUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val specResponse = client.newCall(specRequest).execute()
            if (!specResponse.isSuccessful) {
                Log.w(TAG, "Specs fetch failed with HTTP ${specResponse.code}")
                specResponse.close()
                return null
            }

            val specHtml = specResponse.body?.string() ?: ""
            specResponse.close()

            // Step 3: Parse specifications from HTML
            return parseGsmArenaHtml(specHtml)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving specs online: ${e.message}", e)
        }
        return null
    }

    internal fun parseGsmArenaHtml(html: String): List<ParsedCameraSpec> {
        val list = mutableListOf<ParsedCameraSpec>()
        
        // Extract Main Camera and Selfie Camera table sections
        val mainCameraSection = extractSectionHtml(html, "Main Camera")
        val selfieCameraSection = extractSectionHtml(html, "Selfie camera")

        if (mainCameraSection != null) {
            Log.d(TAG, "Parsing main camera section...")
            val mainLenses = parseCameraSection(mainCameraSection, "back")
            list.addAll(mainLenses)
        } else {
            Log.w(TAG, "Main Camera section not found in HTML.")
        }

        if (selfieCameraSection != null) {
            Log.d(TAG, "Parsing selfie camera section...")
            val selfieLenses = parseCameraSection(selfieCameraSection, "front")
            list.addAll(selfieLenses)
        } else {
            Log.w(TAG, "Selfie camera section not found in HTML.")
        }

        return list
    }

    private fun extractSectionHtml(html: String, sectionHeader: String): String? {
        val index = html.indexOf(sectionHeader)
        if (index == -1) return null
        val endTableIndex = html.indexOf("</table>", index)
        if (endTableIndex == -1) return null
        return html.substring(index, endTableIndex)
    }

    private fun parseCameraSection(sectionHtml: String, facing: String): List<ParsedCameraSpec> {
        val lenses = mutableListOf<ParsedCameraSpec>()
        
        // Match table cells containing specifications (<td class="nfo">...</td>)
        val nfoRegex = """<td class="nfo"[^>]*>(.*?)</td>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val nfoMatches = nfoRegex.findAll(sectionHtml).map { it.groupValues[1] }.toList()

        for (cell in nfoMatches) {
            val cellText = cell.replace(Regex("<a[^>]*>|</a>"), "").trim()
            
            // Only process rows containing Megapixel data to avoid "Features", "Video", or formatting rows
            if (cellText.contains(" MP", ignoreCase = true) || cellText.contains("MP", ignoreCase = true)) {
                // Split multi-lens configurations (separated by <br> tags)
                val lines = cellText.split(Regex("(?i)<br\\s*/?>")).map { it.trim() }.filter { it.isNotEmpty() }
                
                for (line in lines) {
                    val spec = parseLensLine(line, facing)
                    if (spec != null) {
                        lenses.add(spec)
                        Log.d(TAG, "Parsed lens: $spec")
                    }
                }
            }
        }
        return lenses
    }

    internal fun parseLensLine(line: String, facing: String): ParsedCameraSpec? {
        try {
            // 1. Parse Resolution (MP)
            val mpRegex = """\b(\d+(?:\.\d+)?)\s*MP\b""".toRegex(RegexOption.IGNORE_CASE)
            val mpMatch = mpRegex.find(line) ?: return null
            val res = "${mpMatch.groupValues[1]} MP"

            // 2. Parse Aperture (e.g. f/1.8)
            val apertureRegex = """f/(\d+(?:\.\d+)?)""".toRegex()
            val apertureMatch = apertureRegex.find(line)
            val aperture = apertureMatch?.groupValues[1]?.toFloatOrNull()

            // 3. Parse Equivalent Focal Length (e.g. 23mm)
            val equivRegex = """\b(\d+)\s*mm\b""".toRegex()
            val equivMatch = equivRegex.find(line)
            val equiv = equivMatch?.groupValues[1]?.toDoubleOrNull()

            // 4. Parse Sensor Format Size (e.g. 1/1.56")
            val sensorSizeRegex = """\b(1/\d+(?:\.\d+)?|1\.0"|1"|1/1\.12)\b""".toRegex()
            val sensorSizeMatch = sensorSizeRegex.find(line)
            var sensorSize = sensorSizeMatch?.groupValues[1]
            if (sensorSize != null && !sensorSize.endsWith("\"")) {
                sensorSize = if (sensorSize == "1" || sensorSize == "1.0") {
                    "1.0\""
                } else {
                    "$sensorSize\""
                }
            }

            // 5. Parse Pixel Size (e.g. 1.0µm or 1.0 um)
            val pixelSizeRegex = """(\d+(?:\.\d+)?)\s*(?:µm|um|µ|u)""".toRegex()
            val pixelSizeMatch = pixelSizeRegex.find(line)
            val pixelSize = pixelSizeMatch?.groupValues[1]?.toFloatOrNull()

            // 6. Parse Role (wide, ultrawide, telephoto, macro, depth)
            val lineLower = line.lowercase()
            val role = when {
                lineLower.contains("ultrawide") || lineLower.contains("ultra-wide") || lineLower.contains("120˚") || lineLower.contains("118˚") -> "ultrawide"
                lineLower.contains("telephoto") || lineLower.contains("periscope") || lineLower.contains("zoom") -> "telephoto"
                lineLower.contains("macro") -> "macro"
                lineLower.contains("depth") -> "depth"
                else -> "wide"
            }

            // 7. Parse Sensor Model Name (e.g. Sony IMX766)
            var sensorModel = ""
            val sensorPatterns = listOf(
                """\b(IMX\s*\d+[a-zA-Z]*)\b""".toRegex(RegexOption.IGNORE_CASE),
                """\b(S5K\s*[a-zA-Z0-9_]+)\b""".toRegex(RegexOption.IGNORE_CASE),
                """\b(OV\s*\d+[a-zA-Z]*)\b""".toRegex(RegexOption.IGNORE_CASE),
                """\b(LYT\s*\d+[a-zA-Z]*)\b""".toRegex(RegexOption.IGNORE_CASE),
                """\b(Light Fusion\s*\d+[a-zA-Z]*)\b""".toRegex(RegexOption.IGNORE_CASE)
            )
            for (pattern in sensorPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    sensorModel = match.groupValues[1].replace(" ", "").uppercase()
                    break
                }
            }

            return ParsedCameraSpec(
                facing = facing,
                res = res,
                sensor = sensorModel,
                equiv = equiv,
                aperture = aperture,
                pixelSize = pixelSize,
                sensorSize = sensorSize,
                role = role
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse lens spec line: \"$line\", error: ${e.message}")
        }
        return null
    }

    private fun loadFromCache(context: Context, cacheKey: String): List<ParsedCameraSpec>? {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(cacheKey, null) ?: return null
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ParsedCameraSpec>()
            for (i in 0 until array.length()) {
                list.add(ParsedCameraSpec.fromJSONObject(array.getJSONObject(i)))
            }
            return list
        } catch (e: Exception) {
            Log.w(TAG, "Cache load failed for $cacheKey: ${e.message}")
        }
        return null
    }

    private fun saveToCache(context: Context, cacheKey: String, specs: List<ParsedCameraSpec>) {
        try {
            val array = JSONArray()
            for (spec in specs) {
                array.put(spec.toJSONObject())
            }
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(cacheKey, array.toString())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Cache save failed for $cacheKey: ${e.message}")
        }
    }

    /**
     * Find the best matching parsed spec based on role, equivalent focal length, etc.
     */
    fun findBestMatch(
        specs: List<ParsedCameraSpec>,
        facing: String,
        focalLengthMm: Float?,
        focalLength35mmEquiv: Float?,
        role: String
    ): ParsedCameraSpec? {
        val filtered = specs.filter { it.facing.equals(facing, ignoreCase = true) }
        if (filtered.isEmpty()) return null
        if (filtered.size == 1) return filtered.first()

        // Match by 35mm equivalent focal length if available
        if (focalLength35mmEquiv != null && focalLength35mmEquiv > 0) {
            return filtered.minByOrNull { spec ->
                val specEquiv = spec.equiv ?: 0.0
                kotlin.math.abs(specEquiv - focalLength35mmEquiv.toDouble())
            }
        }

        // Match by role (wide, ultrawide, telephoto, macro, depth)
        val roleMatched = filtered.filter { it.role.equals(role, ignoreCase = true) }
        if (roleMatched.isNotEmpty()) return roleMatched.first()

        // Default fallback to first element
        return filtered.first()
    }
}

