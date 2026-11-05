package com.naviaod.app.battery

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.util.Log
import android.view.View
import com.naviaod.app.service.NavigationStateManager
import com.naviaod.app.util.PreferencesManager

class BatteryOptimizationManager(
    private val context: Context,
    private val shiftTargetView: View?,
    private val blankOverlay: View?,
    private val dimmerOverlay: View?
) : SensorEventListener {

    private val prefs = PreferencesManager(context)
    private val handler = Handler(Looper.getMainLooper())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private var isProximityCovered = false
    private var isFaceDown = false
    private var isHighwaySleeping = false
    private var pixelShiftStep = 0
    private var hasVibratedForCurrentTurn = false

    // 1. Pixel Shift Runnable for pOLED Burn-in Protection (every 60 seconds)
    private val pixelShiftRunnable = object : Runnable {
        override fun run() {
            if (prefs.isBurnInProtectionEnabled && shiftTargetView != null) {
                // Shift in a subtle 5x5 matrix (-3 to +3 pixels)
                pixelShiftStep = (pixelShiftStep + 1) % 9
                val offsetX = (pixelShiftStep % 3 - 1) * 3f
                val offsetY = (pixelShiftStep / 3 - 1) * 3f

                shiftTargetView.translationX = offsetX
                shiftTargetView.translationY = offsetY
                Log.d(TAG, "OLED pixel shift applied: dx=$offsetX, dy=$offsetY")
            }
            handler.postDelayed(this, 60_000L) // 1 minute interval
        }
    }

    fun start() {
        // Start Burn-in Protection
        handler.postDelayed(pixelShiftRunnable, 60_000L)

        // Register Pocket / Proximity and Orientation sensors
        if (prefs.isPocketDetectionEnabled) {
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        // Apply Software Dimmer
        applySoftwareDimmer()
    }

    fun stop() {
        handler.removeCallbacks(pixelShiftRunnable)
        sensorManager.unregisterListener(this)
        shiftTargetView?.translationX = 0f
        shiftTargetView?.translationY = 0f
    }

    fun applySoftwareDimmer() {
        if (prefs.isSoftwareDimmerEnabled && dimmerOverlay != null) {
            dimmerOverlay.visibility = View.VISIBLE
            dimmerOverlay.alpha = prefs.dimmerAlpha
        } else {
            dimmerOverlay?.visibility = View.GONE
            dimmerOverlay?.alpha = 0f
        }
    }

    // Evaluates Wake-on-Approach (Highway Deep Sleep)
    fun evaluateDistanceForSleep(distanceMeters: Int) {
        if (!prefs.isWakeOnTurnEnabled) {
            if (isHighwaySleeping) {
                setHighwaySleep(false)
            }
            return
        }

        val threshold = prefs.wakeThresholdMeters

        // If distance is far ahead (> threshold, e.g. 500m on straight road), enter OLED deep blank
        if (distanceMeters > threshold) {
            if (!isHighwaySleeping) {
                setHighwaySleep(true)
                hasVibratedForCurrentTurn = false
            }
        } else if (distanceMeters in 1..threshold) {
            if (isHighwaySleeping) {
                setHighwaySleep(false)
                triggerManeuverAlertHaptic()
            }
        }
    }

    private fun setHighwaySleep(sleep: Boolean) {
        isHighwaySleeping = sleep
        updateScreenBlankState()
    }

    private fun updateScreenBlankState() {
        val shouldBlank = isProximityCovered || isFaceDown || isHighwaySleeping
        NavigationStateManager.setScreenBlanked(shouldBlank)

        handler.post {
            blankOverlay?.visibility = if (shouldBlank) View.VISIBLE else View.GONE
        }
        Log.d(TAG, "Screen blank state: $shouldBlank (prox=$isProximityCovered, faceDown=$isFaceDown, highway=$isHighwaySleeping)")
    }

    private fun triggerManeuverAlertHaptic() {
        if (hasVibratedForCurrentTurn) return
        hasVibratedForCurrentTurn = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Haptic error: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !prefs.isPocketDetectionEnabled) return

        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val maxRange = proximitySensor?.maximumRange ?: 5f
                val distance = event.values[0]
                val covered = distance < maxRange && distance < 4f

                if (covered != isProximityCovered) {
                    isProximityCovered = covered
                    updateScreenBlankState()
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val z = event.values[2]
                // When Z is roughly -9.8 m/s², phone is resting face-down
                val faceDown = z < -8.5f

                if (faceDown != isFaceDown) {
                    isFaceDown = faceDown
                    updateScreenBlankState()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "BatteryOptimization"
    }
}
