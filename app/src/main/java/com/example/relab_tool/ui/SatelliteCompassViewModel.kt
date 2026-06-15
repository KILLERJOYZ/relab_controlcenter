package com.example.relab_tool.ui

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.model.GnssSatellite
import com.example.relab_tool.model.SatelliteCompassUiState
import com.example.relab_tool.model.TurnDirection
import com.example.relab_tool.model.TurnInstruction
import com.example.relab_tool.utils.CompassManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SatelliteCompassViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val compassManager: CompassManager
) : ViewModel() {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _uiState = MutableStateFlow(SatelliteCompassUiState())
    val uiState: StateFlow<SatelliteCompassUiState> = _uiState.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private val _satellitesList = MutableStateFlow<List<GnssSatellite>>(emptyList())

    private var trackingJob: Job? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _lastLocation.value = location
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val satList = mutableListOf<GnssSatellite>()
            for (i in 0 until status.satelliteCount) {
                satList.add(
                    GnssSatellite(
                        svid = status.getSvid(i),
                        constellationType = status.getConstellationType(i),
                        cn0DbHz = status.getCn0DbHz(i),
                        elevationDegrees = status.getElevationDegrees(i),
                        azimuthDegrees = status.getAzimuthDegrees(i),
                        usedInFix = status.usedInFix(i)
                    )
                )
            }
            _satellitesList.value = satList
            _uiState.update { it.copy(allSatellites = satList) }
            
            if (_uiState.value.targetSatellite == null) {
                evaluateBestSatellite()
            }
        }
    }

    init {
        viewModelScope.launch {
            compassManager.headingFlow.collect { heading ->
                _uiState.update { state ->
                    val sat = state.targetSatellite
                    if (sat != null) {
                        val relativeBearing = ((sat.azimuthDegrees - heading) + 360f) % 360f
                        val direction = if (relativeBearing <= 180f) TurnDirection.RIGHT else TurnDirection.LEFT
                        val degrees = if (relativeBearing <= 180f) relativeBearing.toInt() else (360f - relativeBearing).toInt()
                        val turnInstruction = TurnInstruction(direction, degrees, sat.elevationDegrees.toInt())
                        
                        state.copy(
                            deviceHeading = heading,
                            relativeBearing = relativeBearing,
                            turnInstruction = turnInstruction
                        )
                    } else {
                        state.copy(
                            deviceHeading = heading,
                            relativeBearing = 0f,
                            turnInstruction = null
                        )
                    }
                }
            }
        }
    }

    fun startTracking() {
        registerLocationUpdates()
        registerGnssStatusCallback()

        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            while (isActive) {
                evaluateBestSatellite()
                repeat(600) { elapsed ->
                    _uiState.update { it.copy(nextUpdateSec = 600 - elapsed) }
                    delay(1_000L)
                }
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        unregisterLocationUpdates()
        unregisterGnssStatusCallback()
    }

    private fun evaluateBestSatellite() {
        val best = _satellitesList.value.maxByOrNull { it.cn0DbHz }
        _uiState.update { state ->
            val heading = state.deviceHeading
            if (best != null) {
                val relativeBearing = ((best.azimuthDegrees - heading) + 360f) % 360f
                val direction = if (relativeBearing <= 180f) TurnDirection.RIGHT else TurnDirection.LEFT
                val degrees = if (relativeBearing <= 180f) relativeBearing.toInt() else (360f - relativeBearing).toInt()
                val turnInstruction = TurnInstruction(direction, degrees, best.elevationDegrees.toInt())
                state.copy(
                    targetSatellite = best,
                    relativeBearing = relativeBearing,
                    turnInstruction = turnInstruction
                )
            } else {
                state.copy(
                    targetSatellite = null,
                    relativeBearing = 0f,
                    turnInstruction = null
                )
            }
        }
    }

    private fun registerLocationUpdates() {
        try {
            locationManager?.let { lm ->
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener)
                    lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                        _lastLocation.value = it
                    }
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, locationListener)
                    if (_lastLocation.value == null) {
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let {
                            _lastLocation.value = it
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
        } catch (e: Exception) {}
    }

    private fun registerGnssStatusCallback() {
        try {
            locationManager?.registerGnssStatusCallback(
                gnssStatusCallback,
                Handler(Looper.getMainLooper())
            )
        } catch (e: SecurityException) {
        } catch (e: Exception) {}
    }

    private fun unregisterLocationUpdates() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {}
    }

    private fun unregisterGnssStatusCallback() {
        try {
            locationManager?.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (e: Exception) {}
    }

    override fun onCleared() {
        trackingJob?.cancel()
        unregisterLocationUpdates()
        unregisterGnssStatusCallback()
        compassManager.stop()
    }
}
