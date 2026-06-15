package com.example.relab_tool.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BatteryHistoryRepository(context: Context) {
    private val prefs: SharedPreferences by lazy { context.getSharedPreferences("battery_history", Context.MODE_PRIVATE) }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getBatteryHistory(): List<Triple<Long, Int, Boolean>> {
        val today = dateFormat.format(Date())
        val lastSavedDate = prefs.getString("last_date", "")
        
        if (today != lastSavedDate) {
            clearHistory()
            return emptyList()
        }

        val historyJson = prefs.getString("history", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(historyJson)
            val history = mutableListOf<Triple<Long, Int, Boolean>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                history.add(Triple(
                    obj.getLong("timestamp"),
                    obj.getInt("level"),
                    obj.optBoolean("isCharging", false)
                ))
            }
            history
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLastSavedDate(): String {
        return prefs.getString("last_date", "") ?: ""
    }

    fun saveBatteryHistory(history: List<Triple<Long, Int, Boolean>>) {
        val today = dateFormat.format(Date())
        val jsonArray = JSONArray()
        history.forEach { (timestamp, level, isCharging) ->
            val obj = JSONObject()
            obj.put("timestamp", timestamp)
            obj.put("level", level)
            obj.put("isCharging", isCharging)
            jsonArray.put(obj)
        }
        
        prefs.edit()
            .putString("history", jsonArray.toString())
            .putString("last_date", today)
            .apply()
    }

    fun clearHistory() {
        prefs.edit()
            .remove("history")
            .remove("last_date")
            .remove("last_full_charge_ts")
            .remove("last_stopped_charging_ts")
            .apply()
    }

    fun setLastFullChargeTs(ts: Long) {
        prefs.edit().putLong("last_full_charge_ts", ts).apply()
    }

    fun getLastFullChargeTs(): Long {
        return prefs.getLong("last_full_charge_ts", 0L)
    }

    fun setLastStoppedChargingTs(ts: Long) {
        prefs.edit().putLong("last_stopped_charging_ts", ts).apply()
    }

    fun getLastStoppedChargingTs(): Long {
        return prefs.getLong("last_stopped_charging_ts", 0L)
    }

    fun getAccumulatedDischarge(): Float {
        return prefs.getFloat("acc_discharge", 0f)
    }

    fun saveAccumulatedDischarge(value: Float) {
        prefs.edit().putFloat("acc_discharge", value).apply()
    }

    fun getManualCycleCount(): Int {
        return prefs.getInt("manual_cycle_count", 0)
    }

    fun saveManualCycleCount(count: Int) {
        prefs.edit().putInt("manual_cycle_count", count).apply()
    }

    // Battery Wear Tracking
    fun getChargeStartLevel(): Int = prefs.getInt("charge_start_level", -1)
    fun saveChargeStartLevel(level: Int) = prefs.edit().putInt("charge_start_level", level).apply()

    fun getChargeStartCounter(): Long = prefs.getLong("charge_start_counter", -1L)
    fun saveChargeStartCounter(counter: Long) = prefs.edit().putLong("charge_start_counter", counter).apply()

    fun getCalculatedCapacity(): Float = prefs.getFloat("calc_capacity", -1f)
    fun saveCalculatedCapacity(mah: Float) = prefs.edit().putFloat("calc_capacity", mah).apply()

    fun getCalculatedHealth(): Float = prefs.getFloat("calc_health", -1f)
    fun saveCalculatedHealth(pct: Float) = prefs.edit().putFloat("calc_health", pct).apply()

    // Charging Curve Storage
    fun saveChargingPoint(percent: Int, wattage: Float) {
        val curveJson = prefs.getString("charging_curve", "{}") ?: "{}"
        try {
            val json = JSONObject(curveJson)
            json.put(percent.toString(), wattage.toDouble())
            prefs.edit().putString("charging_curve", json.toString()).apply()
        } catch (e: Exception) {}
    }

    fun getChargingCurve(): Map<Int, Float> {
        val curveJson = prefs.getString("charging_curve", "{}") ?: "{}"
        val map = mutableMapOf<Int, Float>()
        try {
            val json = JSONObject(curveJson)
            json.keys().forEach { key ->
                map[key.toInt()] = json.getDouble(key).toFloat()
            }
        } catch (e: Exception) {}
        return map
    }

    fun clearChargingCurve() {
        prefs.edit().remove("charging_curve").apply()
    }

    fun getDischargeRatePerHour(): Float {
        val history = getBatteryHistory()
        if (history.size < 2) return 0f
        
        val nonCharging = history.filter { !it.third }
        if (nonCharging.size < 2) return 0f
        
        val first = nonCharging.first()
        val last = nonCharging.last()
        
        val deltaPct = first.second - last.second
        val deltaTimeMs = last.first - first.first
        
        if (deltaTimeMs <= 0 || deltaPct <= 0) return 0f
        
        return (deltaPct.toFloat() / (deltaTimeMs / 3600000f))
    }

    fun recordBatteryPoint(level: Int, isCharging: Boolean) {
        val today = dateFormat.format(Date())
        if (today != getLastSavedDate()) {
            clearHistory()
        }

        if (level == 100) {
            setLastFullChargeTs(System.currentTimeMillis())
        }

        val history = getBatteryHistory().toMutableList()
        val lastPoint = history.lastOrNull()

        if (lastPoint == null || lastPoint.second != level || lastPoint.third != isCharging) {
            val newPoint = Triple(System.currentTimeMillis(), level, isCharging)
            history.add(newPoint)
            saveBatteryHistory(history)
        }
    }
}
