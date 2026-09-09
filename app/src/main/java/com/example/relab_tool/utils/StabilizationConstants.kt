package com.example.relab_tool.utils

object StabilizationConstants {
    const val SPEED_TEST_URL = "https://speed.cloudflare.com/__down?bytes=25000000"
    const val JSON_REPORT_PREFIX = "relab_diagnostic_"
    const val JSON_EXTENSION = ".json"
    const val PDF_EXTENSION = ".pdf"
    const val PDF_MIME = "application/pdf"
    const val JSON_MIME = "application/json"
    const val FILE_PROVIDER_SUFFIX = ".fileprovider"
    const val EMPTY_JSON_ARRAY = "[]"
    const val PREF_BATTERY = "battery_history"
    const val KEY_HISTORY = "history"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_LEVEL = "level"
    const val KEY_CHARGING = "isCharging"
    const val KEY_LAST_DATE = "last_date"
    const val KEY_FULL_CHARGE = "last_full_charge_ts"
    const val KEY_STOPPED_CHARGING = "last_stopped_charging_ts"
    const val DATE_FORMAT = "yyyy-MM-dd"
}
