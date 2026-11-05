package com.naviaod.app.model

import android.graphics.Bitmap

enum class ManeuverType {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN,
    ROUNDABOUT,
    DESTINATION,
    UNKNOWN
}

data class NavigationData(
    val instruction: String = "Proceed to highlighted route",
    val distance: String = "--",
    val distanceMeters: Int = 0,
    val remainingDistance: String = "--",
    val remainingTime: String = "--",
    val eta: String = "--",
    val maneuverType: ManeuverType = ManeuverType.STRAIGHT,
    val maneuverIcon: Bitmap? = null,
    val isNavigating: Boolean = false,
    val isSimulated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
