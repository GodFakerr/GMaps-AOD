package com.naviaod.app.simulator

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.naviaod.app.model.ManeuverType
import com.naviaod.app.model.NavigationData
import com.naviaod.app.service.NavigationStateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NavigationSimulator(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    var isRunning = false
        private set

    private var currentStepIndex = 0
    private var countdownMeters = 0
    private var totalRemainingKm = 16.5f

    data class SimStep(
        val maneuverType: ManeuverType,
        val instruction: String,
        val initialDistanceMeters: Int,
        val targetSpeedKmh: Int
    )

    private val puneRouteSteps = listOf(
        SimStep(
            ManeuverType.STRAIGHT,
            "Head east on Hinjewadi Phase 1 Main Road",
            800,
            42
        ),
        SimStep(
            ManeuverType.SLIGHT_LEFT,
            "Take the ramp onto Mumbai - Pune Bypass / NH 48",
            2400, // Demonstrates Highway Sleep! (> 1km)
            78
        ),
        SimStep(
            ManeuverType.SLIGHT_RIGHT,
            "Take the exit toward Wakad / Aundh",
            600,
            55
        ),
        SimStep(
            ManeuverType.ROUNDABOUT,
            "At the roundabout, take the 2nd exit onto Aundh-Ravet BRTS",
            450,
            38
        ),
        SimStep(
            ManeuverType.TURN_RIGHT,
            "Turn right onto Senapati Bapat Road",
            750,
            40
        ),
        SimStep(
            ManeuverType.TURN_LEFT,
            "Turn left toward Pune Railway Station Road",
            300,
            28
        ),
        SimStep(
            ManeuverType.DESTINATION,
            "You have arrived at Pune Railway Station",
            50,
            15
        )
    )

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val step = puneRouteSteps[currentStepIndex]

            // Decrement distance based on simulated speed
            val metersPerSecond = (step.targetSpeedKmh / 3.6f).toInt().coerceAtLeast(10)
            countdownMeters -= (metersPerSecond * 2)

            if (countdownMeters <= 0) {
                // Advance to next maneuver
                currentStepIndex++
                if (currentStepIndex >= puneRouteSteps.size) {
                    currentStepIndex = 0 // loop route
                    totalRemainingKm = 16.5f
                }
                val nextStep = puneRouteSteps[currentStepIndex]
                countdownMeters = nextStep.initialDistanceMeters
            }

            // Decrement overall remaining km
            totalRemainingKm = (totalRemainingKm - 0.05f).coerceAtLeast(0.1f)
            val minutesLeft = (totalRemainingKm / 35f * 60).toInt().coerceAtLeast(1)

            val etaTime = System.currentTimeMillis() + (minutesLeft * 60_000L)
            val etaFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(etaTime))

            val distFormatted = if (countdownMeters >= 1000) {
                String.format(Locale.US, "%.1f km", countdownMeters / 1000f)
            } else {
                "$countdownMeters m"
            }

            val navData = NavigationData(
                instruction = step.instruction,
                distance = distFormatted,
                distanceMeters = countdownMeters,
                remainingDistance = String.format(Locale.US, "%.1f km", totalRemainingKm),
                remainingTime = "$minutesLeft min",
                eta = etaFormatted,
                maneuverType = step.maneuverType,
                maneuverIcon = null,
                isNavigating = true,
                isSimulated = true,
                timestamp = System.currentTimeMillis()
            )

            NavigationStateManager.updateNavigation(navData)
            NavigationStateManager.updateSpeed(step.targetSpeedKmh)

            // Simulate slight battery variation
            val battery = 88 - (currentStepIndex % 5)
            NavigationStateManager.updateBattery(battery)

            handler.postDelayed(this, 1000L) // 1 FPS update
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        currentStepIndex = 0
        countdownMeters = puneRouteSteps[0].initialDistanceMeters
        totalRemainingKm = 16.5f
        handler.post(tickRunnable)
        Log.d(TAG, "Pune Navigation Simulator started")
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        handler.removeCallbacks(tickRunnable)
        NavigationStateManager.clearNavigation()
        Log.d(TAG, "Navigation Simulator stopped")
    }

    companion object {
        private const val TAG = "NavSimulator"
    }
}
