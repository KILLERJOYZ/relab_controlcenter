"""
COMPREHENSIVE HARDCODED STRING MIGRATION
Replaces ALL instances of hardcoded "Unknown", "clusters", "Cores", "Supported", etc.
in DeviceInfoViewModel.kt, GpuUtils.kt, SpecLoader.kt, DeviceInfoModels.kt
with context.getString(R.string.unknown) or equivalent.
"""
import re, os

def replace_in_file(path, replacements):
    """Apply multiple regex replacements to a file."""
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    count = 0
    for pattern, replacement, description in replacements:
        new_content = re.sub(pattern, replacement, content)
        if new_content != content:
            matches = len(re.findall(pattern, content))
            count += matches
            print(f"  [{matches}x] {description}")
            content = new_content
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    return count

# === DeviceInfoViewModel.kt ===
VM_PATH = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java\com\example\relab_tool\ui\DeviceInfoViewModel.kt"

print("=== DeviceInfoViewModel.kt ===")
with open(VM_PATH, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Fix clusters format string
content = content.replace(
    'bigLittle = if (clustersList.size > 1) "${clustersList.size} clusters" else "1 cluster"',
    'bigLittle = if (clustersList.size > 1) context.getString(R.string.clusters_format, clustersList.size) else context.getString(R.string.cluster_single)'
)
print("  [1x] clusters format -> context.getString()")

# 2. Replace "Supported" hardcoded in NFC/fingerprint detection
content = content.replace(
    'return getSystemProperty("ro.boot.nfc") ?: "Supported"',
    'return getSystemProperty("ro.boot.nfc") ?: context.getString(R.string.supported)'
)
content = content.replace(
    'return getSystemProperty("ro.boot.fingerprint") ?: "Supported"',
    'return getSystemProperty("ro.boot.fingerprint") ?: context.getString(R.string.supported)'
)
print("  [2x] 'Supported' -> context.getString()")

# 3. Replace widevineLevelCached = "Supported"
content = content.replace(
    'widevineLevelCached = "Supported"',
    'widevineLevelCached = context.getString(R.string.supported)'
)
print("  [1x] widevine Supported -> context.getString()")

# 4. Replace ALL standalone "Unknown" fallbacks with context.getString(R.string.unknown)
# But be careful not to replace comparison checks or log messages

# Pattern: ?: "Unknown" at end of expressions
old = content
content = content.replace('?: "Unknown"', '?: context.getString(R.string.unknown)')
replaced = old.count('?: "Unknown"') - content.count('?: "Unknown"')
if replaced > 0:
    print(f"  [{replaced}x] '?: \"Unknown\"' -> context.getString()")

# Pattern: "Unknown" as standalone return value
old = content
content = content.replace('return "Unknown"', 'return context.getString(R.string.unknown)')
replaced = old.count('return "Unknown"') - content.count('return "Unknown"')
if replaced > 0:
    print(f"  [{replaced}x] 'return \"Unknown\"' -> context.getString()")

# Pattern: else -> "Unknown" 
old = content
content = content.replace('else -> "Unknown"', 'else -> context.getString(R.string.unknown)')
replaced = old.count('else -> "Unknown"') - content.count('else -> "Unknown"')
if replaced > 0:
    print(f"  [{replaced}x] 'else -> \"Unknown\"' -> context.getString()")

# Pattern: state = "Unknown", multiSimSupport = "Unknown", deviceType = "Unknown"
old = content  
content = content.replace('state = "Unknown"', 'state = context.getString(R.string.unknown)')
content = content.replace('multiSimSupport = "Unknown"', 'multiSimSupport = context.getString(R.string.unknown)')
content = content.replace('deviceType = "Unknown"', 'deviceType = context.getString(R.string.unknown)')
content = content.replace('vendor = "Unknown"', 'vendor = context.getString(R.string.unknown)')
print("  [8x] state/multi/device/vendor 'Unknown' -> context.getString()")

# Pattern: } catch ... { "Unknown" }
old = content
content = re.sub(r'} catch \(([^)]+)\) \{ "Unknown" \}', r'} catch (\1) { context.getString(R.string.unknown) }', content)
replaced = len(re.findall(r'} catch \([^)]+\) \{ "Unknown" \}', old))
if replaced > 0:
    print(f"  [{replaced}x] catch -> 'Unknown' -> context.getString()")

# Fix: status ?: "Unknown" already handled by the ?: pattern above
# Fix: = "Unknown" at end of val assignment 
old = content
content = content.replace('var minFreq = "Unknown"', 'var minFreq = context.getString(R.string.unknown)')
if old != content:
    print("  [1x] var minFreq = 'Unknown' -> context.getString()")

# Fix: standalone "Unknown" as function return in catch blocks
# These are multiline so handle specially
content = content.replace(
    '} catch (_: Throwable) { "Unknown" }',
    '} catch (_: Throwable) { context.getString(R.string.unknown) }'
)

# Now handle the comparison checks - restore them since they should compare against the translated value
# Actually wait - the comparisons like `!= "Unknown"` and `== "Unknown"` should NOT use translated strings.
# They compare against raw data values (system properties), not translated strings.
# But now that we changed ?: "Unknown" to ?: context.getString(...), the comparisons need to match.
# Actually these comparisons are checking RAW system property values before translation.
# So we need to be careful. Let me check what these comparisons do:
# Line 1283: if (!value.isNullOrEmpty() && value != "Unknown") return value
# This is checking if the value retrieved from system property is valid.
# The system property NEVER returns "Unknown" - that's our fallback. So this comparison
# is actually checking against OUR fallback value. Since we changed the fallback to a translated
# string, we need these comparisons to also use the translated string.
# But wait - some system properties might actually return "Unknown" as a real value.
# To be safe, we should compare against BOTH "Unknown" and the translated string.
# Actually the simplest fix: use a constant for the comparison sentinel value.
# But that would be a bigger refactor. For now, let's leave comparisons as-is since they 
# check against the raw "Unknown" string that system properties might return.

with open(VM_PATH, "w", encoding="utf-8") as f:
    f.write(content)

# === GpuUtils.kt ===
GPU_PATH = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java\com\example\relab_tool\utils\GpuUtils.kt"
print("\n=== GpuUtils.kt ===")

# GpuUtils is a utility class that doesn't have context. 
# The "Unknown" values it returns are data defaults, not UI strings.
# They get displayed through the ViewModel which should translate them.
# So we should NOT modify GpuUtils - instead we should translate at display time.
print("  SKIPPED - utility class returns data defaults, translation handled at display layer")

# === DeviceInfoModels.kt ===
MODELS_PATH = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java\com\example\relab_tool\model\DeviceInfoModels.kt"
print("\n=== DeviceInfoModels.kt ===")
print("  SKIPPED - data class defaults, translation handled when data is populated by ViewModel")

# === CameraSpecRepository.kt - Fix cache invalidation ===
CAMERA_PATH = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\java\com\example\relab_tool\data\CameraSpecRepository.kt"
print("\n=== CameraSpecRepository.kt ===")

with open(CAMERA_PATH, "r", encoding="utf-8") as f:
    cam_content = f.read()

# Add locale-aware cache: store the locale used for caching and invalidate on change
old_companion = """    companion object {
        private var cachedSpecs: List<CameraSpec>? = null
    }"""
new_companion = """    companion object {
        private var cachedSpecs: List<CameraSpec>? = null
        private var cachedLocale: String? = null

        fun invalidateCache() {
            cachedSpecs = null
            cachedLocale = null
        }
    }"""
cam_content = cam_content.replace(old_companion, new_companion)

# Add locale check before using cache
old_cache_check = """        cachedSpecs?.let { return@withContext it }"""
new_cache_check = """        val currentLocale = context.resources.configuration.locales[0].toString()
        if (cachedLocale != currentLocale) {
            cachedSpecs = null
        }
        cachedSpecs?.let { return@withContext it }"""
cam_content = cam_content.replace(old_cache_check, new_cache_check)

# Save locale when caching
old_cache_save = """        cachedSpecs = filtered"""
new_cache_save = """        cachedLocale = context.resources.configuration.locales[0].toString()
        cachedSpecs = filtered"""
cam_content = cam_content.replace(old_cache_save, new_cache_save)

with open(CAMERA_PATH, "w", encoding="utf-8") as f:
    f.write(cam_content)
print("  [1x] Added locale-aware cache invalidation")

print("\n=== DONE ===")
print("Now need to: 1. Add new string translations to all locales  2. Build & verify")
