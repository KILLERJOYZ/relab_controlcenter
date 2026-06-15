package com.example.relab_tool.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.location.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompassManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _headingFlow = MutableStateFlow(0f)
    val headingFlow: StateFlow<Float> = _headingFlow.asStateFlow()

    private var currentLocation: Location? = null
    private var filteredHeading = -1f
    private val alpha = 0.1f
    private var cachedDeclination = 0f
    // Throttle: only emit heading changes > 1° to avoid needless recompositions
    private var lastEmittedHeading = -1f
    private var lastEmitTimeNanos = 0L
    private companion object {
        /** Minimum heading change (degrees) to emit a new value */
        const val MIN_HEADING_CHANGE_DEG = 1.0f
        /** Minimum time between emissions (nanoseconds) — ~15Hz max */
        const val MIN_EMIT_INTERVAL_NS = 66_000_000L // 66ms
    }

    private val rotationMatrix = FloatArray(9)
    private val adjustedMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start(location: Location) {
        val prev = currentLocation
        currentLocation = location
        if (prev == null || prev.latitude != location.latitude || prev.longitude != location.longitude || prev.altitude != location.altitude) {
            try {
                val geo = GeomagneticField(
                    location.latitude.toFloat(),
                    location.longitude.toFloat(),
                    location.altitude.toFloat(),
                    System.currentTimeMillis()
                )
                cachedDeclination = geo.declination
            } catch (e: Exception) {
                cachedDeclination = 0f
            }
        }
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        try { sensorManager?.unregisterListener(this) } catch (_: Throwable) {}
        filteredHeading = -1f
        lastEmittedHeading = -1f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                adjustedMatrix
            )
            SensorManager.getOrientation(adjustedMatrix, orientationAngles)
            val rawHeadingDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalized = (rawHeadingDeg + 360f) % 360f

            if (filteredHeading < 0f) {
                filteredHeading = normalized
            } else {
                // Low-pass filter to remove jitter handling angle wrap-around
                var diff = normalized - filteredHeading
                if (diff < -180f) diff += 360f
                if (diff > 180f) diff -= 360f
                filteredHeading = (filteredHeading + alpha * diff + 360f) % 360f
            }

            val trueHeading = (filteredHeading + cachedDeclination + 360f) % 360f

            // Throttle emissions: only update if heading changed by ≥1° AND enough time has passed
            val now = System.nanoTime()
            val headingDiff = kotlin.math.abs(trueHeading - lastEmittedHeading)
            val wrappedDiff = if (headingDiff > 180f) 360f - headingDiff else headingDiff
            if (lastEmittedHeading < 0f ||
                (wrappedDiff >= MIN_HEADING_CHANGE_DEG && (now - lastEmitTimeNanos) >= MIN_EMIT_INTERVAL_NS)) {
                lastEmittedHeading = trueHeading
                lastEmitTimeNanos = now
                _headingFlow.value = trueHeading
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
