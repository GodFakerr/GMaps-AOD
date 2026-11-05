package com.naviaod.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import kotlin.math.roundToInt

class PassiveSpeedometerService(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var isListening = false

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (isListening) return
        try {
            // PASSIVE_PROVIDER consumes 0 extra battery by piggybacking on Google Maps' active GPS
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.PASSIVE_PROVIDER,
                    1000L, // 1 second
                    1f,    // 1 meter
                    this
                )
                isListening = true
                Log.d(TAG, "Passive location listener started")
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L,
                    5f,
                    this
                )
                isListening = true
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location listener: ${e.message}")
        }
    }

    fun stopListening() {
        if (!isListening) return
        try {
            locationManager.removeUpdates(this)
            isListening = false
            Log.d(TAG, "Passive location listener stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing location updates: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        if (location.hasSpeed()) {
            val speedKmh = (location.speed * 3.6f).roundToInt()
            NavigationStateManager.updateSpeed(speedKmh)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    companion object {
        private const val TAG = "PassiveSpeedometer"
    }
}
