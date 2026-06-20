package com.example.relab_tool.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import java.util.concurrent.atomic.AtomicReference

sealed interface WifiSsidState {
    object PermissionDenied : WifiSsidState
    object PermissionPermanentlyDenied : WifiSsidState
    object LocationServicesDisabled : WifiSsidState
    object NotConnected : WifiSsidState
    data class Connected(val ssid: String) : WifiSsidState
}

class WifiInfoProvider(private val context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val latestWifiInfo = AtomicReference<WifiInfo?>(null)
    private var isCallbackRegistered = false

    private val networkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Api31NetworkCallbackHelper.createCallback(
            onCapabilitiesChanged = { latestWifiInfo.set(it) },
            onLost = { latestWifiInfo.set(null) }
        )
    } else {
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                latestWifiInfo.set(wifiInfo)
            }
            override fun onLost(network: Network) {
                latestWifiInfo.set(null)
            }
        }
    }

    init {
        registerCallback()
    }

    fun registerCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isCallbackRegistered) {
            try {
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                cm.registerNetworkCallback(request, networkCallback)
                isCallbackRegistered = true
            } catch (e: Exception) {
                // Fallback / Log
            }
        }
    }

    fun unregisterCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isCallbackRegistered) {
            try {
                cm.unregisterNetworkCallback(networkCallback)
                isCallbackRegistered = false
            } catch (e: Exception) {
                // Fallback / Log
            }
        }
    }

    fun getSsidState(isPermanentlyDenied: Boolean): WifiSsidState {
        // 1. Check Location Permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return if (isPermanentlyDenied) {
                WifiSsidState.PermissionPermanentlyDenied
            } else {
                WifiSsidState.PermissionDenied
            }
        }

        // Force callback re-registration on API 31+ if cached info is missing or redacted but we now have permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cached = latestWifiInfo.get()
            if (cached == null || cached.ssid == "<unknown ssid>" || cached.bssid == "02:00:00:00:00:00") {
                unregisterCallback()
                registerCallback()
            }
        }

        // 2. Check if Location Services (GPS) are enabled
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            return WifiSsidState.LocationServicesDisabled
        }

        // 3. Fetch WifiInfo
        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            latestWifiInfo.get() ?: getWifiInfoFromActiveNetwork()
        } else {
            @Suppress("DEPRECATION")
            wm.connectionInfo
        }

        if (wifiInfo == null) {
            return WifiSsidState.NotConnected
        }

        val rawSsid = wifiInfo.ssid
        if (rawSsid == null || rawSsid == "<unknown ssid>" || rawSsid == WifiManager.UNKNOWN_SSID) {
            return WifiSsidState.NotConnected
        }

        val cleanSsid = rawSsid.removeSurrounding("\"")
        if (cleanSsid.isEmpty() || cleanSsid == "<unknown ssid>") {
            return WifiSsidState.NotConnected
        }

        return WifiSsidState.Connected(cleanSsid)
    }

    private fun getWifiInfoFromActiveNetwork(): WifiInfo? {
        return try {
            val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) cm.activeNetwork else null
            val caps = cm.getNetworkCapabilities(activeNetwork)
            caps?.transportInfo as? WifiInfo
        } catch (e: Exception) {
            null
        }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
private object Api31NetworkCallbackHelper {
    fun createCallback(
        onCapabilitiesChanged: (WifiInfo?) -> Unit,
        onLost: () -> Unit
    ): ConnectivityManager.NetworkCallback {
        return object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                onCapabilitiesChanged(wifiInfo)
            }
            override fun onLost(network: Network) {
                onLost()
            }
        }
    }
}
