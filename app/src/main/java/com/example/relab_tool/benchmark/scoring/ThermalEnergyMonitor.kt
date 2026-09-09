package com.example.relab_tool.benchmark.scoring

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.delay

/**
 * ThermalEnergyMonitor — measures thermal throttling behaviour and power consumption
 * during a benchmark run, then produces the two dynamic scoring coefficients:
 *
 *   C_th  (Thermal Stability Coefficient):  [0.40, 1.00]
 *   C_epwr(Energy Efficiency Coefficient):  [0.50, 1.50]
 *
 * These coefficients are applied multiplicatively to the raw benchmark score:
 *   Final Score = Raw Score × C_th × C_epwr
 *
 * ADPF usage note:
 *   PowerManager.getThermalHeadroom() may return NaN if polled more frequently
 *   than once every ~10 seconds on some devices. This class always polls at 10s intervals.
 *
 * BatteryManager note:
 *   BATTERY_PROPERTY_CURRENT_NOW and BATTERY_PROPERTY_ENERGY_COUNTER require
 *   hardware fuel-gauge support. On devices without fuel-gauge (common on budget SoCs),
 *   both values return Long.MIN_VALUE. The monitor handles this gracefully by setting
 *   C_epwr = 1.0 (neutral, no penalty/bonus).
 */
class ThermalEnergyMonitor(private val context: Context) {

    companion object {
        private const val TAG = "ThermalEnergyMonitor"
        private const val POLL_INTERVAL_MS = 10_000L
    }

    private val powerManager: PowerManager? =
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val batteryManager: BatteryManager? =
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    // Snapshot taken at benchmark start
    private var startPerfMarker: Double = 0.0
    private var startEnergyNwh: Long = Long.MIN_VALUE
    private var startTimeMs: Long = 0L

    // Running accumulators
    private var maxThermalHeadroom: Float = 0f
    private var totalOperations: Double = 0.0
    private var sampleCount: Int = 0
    private val headroomSamples = mutableListOf<Float>()

    /** Call at the very beginning of the benchmark run to record start state. */
    fun start(initialPerfOps: Double = 0.0) {
        startPerfMarker = initialPerfOps
        startTimeMs = System.currentTimeMillis()
        startEnergyNwh = readEnergyCounter()
        maxThermalHeadroom = 0f
        headroomSamples.clear()
        sampleCount = 0
        totalOperations = 0.0
        Log.d(TAG, "Monitor started. InitialEnergy=$startEnergyNwh nWh")
    }

    /**
     * Call periodically during the benchmark (every POLL_INTERVAL_MS).
     * Records thermal headroom and accumulates operation count.
     */
    fun record(operationsDeltaSinceLastRecord: Double = 0.0) {
        val headroom = getThermalHeadroom()
        if (!headroom.isNaN()) {
            headroomSamples.add(headroom)
            if (headroom > maxThermalHeadroom) maxThermalHeadroom = headroom
        }
        totalOperations += operationsDeltaSinceLastRecord
        sampleCount++
    }

    /**
     * Call when the benchmark completes.
     * @param finalPerfOps  Performance metric at end of run (same unit as [startPerfMarker]).
     * @return [ThermalEnergyResult] containing both coefficients.
     */
    fun stop(finalPerfOps: Double = 0.0): ThermalEnergyResult {
        val elapsedMs = System.currentTimeMillis() - startTimeMs
        val endEnergyNwh = readEnergyCounter()

        // === Thermal Coefficient ===
        // C_th = (Perf_End / Perf_Start) × (1 - thermalPenalty)
        // If perf markers are unavailable, fall back to headroom-based estimate.
        val perfRatio = if (startPerfMarker > 0 && finalPerfOps > 0) {
            (finalPerfOps / startPerfMarker).coerceIn(0.1, 1.0).toFloat()
        } else {
            // Estimate from thermal headroom: if max headroom reached SEVERE (1.0),
            // performance was likely cut in half.
            1f - (maxThermalHeadroom * 0.5f)
        }
        val thermalPenalty = (maxThermalHeadroom * 0.25f).coerceIn(0f, 0.5f)
        val cTh = (perfRatio * (1f - thermalPenalty)).coerceIn(0.40f, 1.0f)

        // === Energy Efficiency Coefficient ===
        val cEpwr: Float = if (startEnergyNwh != Long.MIN_VALUE &&
            endEnergyNwh != Long.MIN_VALUE &&
            elapsedMs > 1000 &&
            endEnergyNwh < startEnergyNwh) // energy counter decreases as battery drains
        {
            val energyConsumedNwh = (startEnergyNwh - endEnergyNwh).toDouble()
            val energyConsumedJoules = energyConsumedNwh * 3.6e-6 // nWh → Joules
            if (energyConsumedJoules > 0 && totalOperations > 0) {
                val opsPerJoule = totalOperations / energyConsumedJoules
                Log.d(TAG, "opsPerJoule=$opsPerJoule energy=${energyConsumedJoules}J ops=$totalOperations")
                // Normalise: ~1e8 ops/J for mid-range → C_epwr = 1.0
                // ~2e8 ops/J for flagship    → C_epwr = 1.2+
                // ~5e7 ops/J for entry       → C_epwr = 0.7
                val normalized = (opsPerJoule / 1e8).toFloat()
                normalized.coerceIn(0.50f, 1.50f)
            } else 1.0f
        } else {
            // Fuel-gauge not supported; use voltage/current estimate
            val estimatedC = estimateEnergyCoeffFromCurrentNow(elapsedMs)
            estimatedC
        }

        val result = ThermalEnergyResult(
            thermalCoefficient = cTh,
            energyCoefficient = cEpwr,
            maxThermalHeadroom = maxThermalHeadroom,
            durationMs = elapsedMs
        )
        Log.d(TAG, "Monitor stopped. C_th=$cTh C_epwr=$cEpwr maxHeadroom=$maxThermalHeadroom")
        return result
    }

    /** Convenience suspend function for background polling during a long run. */
    suspend fun pollUntilStopped(onSample: (headroom: Float) -> Unit) {
        while (true) {
            delay(POLL_INTERVAL_MS)
            val headroom = getThermalHeadroom()
            record()
            if (!headroom.isNaN()) onSample(headroom)
        }
    }

    // ────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────

    private fun getThermalHeadroom(): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return Float.NaN
        return try {
            powerManager?.getThermalHeadroom(0) ?: Float.NaN
        } catch (e: Exception) {
            Float.NaN
        }
    }

    private fun readEnergyCounter(): Long {
        return try {
            batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
                ?: Long.MIN_VALUE
        } catch (e: Exception) {
            Long.MIN_VALUE
        }
    }

    /**
     * Fallback energy estimation using instantaneous current and voltage.
     * Less accurate than fuel-gauge but available on more devices.
     */
    private fun estimateEnergyCoeffFromCurrentNow(elapsedMs: Long): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 1.0f
        try {
            val currentMicroA = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                ?: return 1.0f
            if (currentMicroA == Int.MIN_VALUE) return 1.0f

            // Draw current in mA (take absolute value; charging = positive, discharging = negative)
            val drawMa = Math.abs(currentMicroA / 1000.0)
            // Low draw during heavy load = efficient chip → higher C_epwr
            // Typical flagship under load: 2000–3000 mA, Entry: 4000–5000 mA
            return when {
                drawMa < 1500 -> 1.30f   // very efficient
                drawMa < 2500 -> 1.10f   // efficient (flagship)
                drawMa < 3500 -> 1.00f   // typical mid-range
                drawMa < 4500 -> 0.85f   // high draw
                else          -> 0.70f   // very high draw (entry chip working hard)
            }
        } catch (e: Exception) {
            return 1.0f
        }
    }
}

data class ThermalEnergyResult(
    /** Thermal stability coefficient. 1.0 = no throttling. 0.4 = severe throttle. */
    val thermalCoefficient: Float,
    /** Energy efficiency coefficient. 1.0 = reference. > 1.0 = more efficient than reference. */
    val energyCoefficient: Float,
    /** Maximum thermal headroom reached during the run (0=cool, 1=critical). */
    val maxThermalHeadroom: Float,
    /** Total benchmark duration in ms. */
    val durationMs: Long
)
