package com.example.relab_tool.data

import com.example.relab_tool.utils.StabilizationConstants as C
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BatteryHistoryRepository(context: Context, private val clock: () -> Long = System::currentTimeMillis) {
    companion object { private val historyLock = Any() }
    private val prefs: SharedPreferences by lazy { context.getSharedPreferences("battery_history", Context.MODE_PRIVATE) }


    fun getBatteryHistory(): List<Triple<Long, Int, Boolean>> = synchronized(historyLock) {
        val historyJson = prefs.getString("history", "[]") ?: "[]"
        try {
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
            BatteryHistoryPolicy.retained(history, clock())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLastSavedDate(): String {
        return prefs.getString("last_date", "") ?: ""
    }

    fun saveBatteryHistory(history: List<Triple<Long, Int, Boolean>>) = synchronized(historyLock) {
        val today = SimpleDateFormat(C.DATE_FORMAT, Locale.US).format(Date(clock()))
        val jsonArray = JSONArray()
        BatteryHistoryPolicy.retained(history, clock()).forEach { (timestamp, level, isCharging) ->
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

    fun getDischargeRatePerHour(): Float = BatteryHistoryPolicy.dischargeRate(getBatteryHistory())

    fun recordBatteryPoint(level: Int, isCharging: Boolean) = synchronized(historyLock) {
        if (level !in 0..100) return@synchronized
        val now = clock()
        val history = getBatteryHistory().toMutableList()
        val last = history.lastOrNull()
        if (level == 100 && last?.second != 100 && (last != null || getLastFullChargeTs() == 0L)) {
            setLastFullChargeTs(now)
        }
        if (last?.third == true && !isCharging) setLastStoppedChargingTs(now)
        if (last == null || last.second != level || last.third != isCharging ||
            now - last.first >= BatteryHistoryPolicy.SAMPLE_INTERVAL_MS) {
            history.add(Triple(now, level, isCharging))
            saveBatteryHistory(history)
        }
    }

}
