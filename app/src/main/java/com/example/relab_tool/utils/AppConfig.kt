package com.example.relab_tool.utils

object AppConfig {
    // APK Crawler Configuration
    const val APKPURE_BASE_URL = "https://d.apkpure.com/b/APK"
    const val APKPURE_URL_QUERY_TEMPLATE = "%s/%s?version=latest"
    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile"
    const val HTTP_TIMEOUT_SECONDS = 15L
    const val HTTP_RETRY_DELAY_MS = 1000L
    const val HTTP_MAX_RETRIES = 3
    const val HEADER_CACHE_CONTROL = "Cache-Control"
    const val VALUE_NO_CACHE = "no-cache"
    const val HEADER_USER_AGENT = "User-Agent"
    const val HEADER_APK_VERSION = "X-APK-Version"
    const val MOCK_LATEST_VERSION = "1.2.0"

    // Gemini AI Configuration
    const val GEMINI_MODEL_NAME = "gemini-1.5-flash"
    const val GEMINI_PROMPT_TEMPLATE = "What is the marketing name of an Android device with Manufacturer: %s, Model: %s, Device: %s? Return ONLY the marketing name, nothing else."

    // Assets
    const val ASSET_DEVICES_JSON = "google_play_devices.json"
}
