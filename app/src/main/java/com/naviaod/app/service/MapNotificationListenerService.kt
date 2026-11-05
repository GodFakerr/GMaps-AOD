package com.naviaod.app.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.naviaod.app.model.ManeuverType
import com.naviaod.app.model.NavigationData
import com.naviaod.app.ui.AodNavigationActivity
import com.naviaod.app.util.PreferencesManager
import java.util.regex.Pattern

class MapNotificationListenerService : NotificationListenerService() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        Log.d(TAG, "MapNotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName == GOOGLE_MAPS_PACKAGE) {
            val notification = sbn.notification ?: return
            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0

            // Check if this is an active navigation notification
            if (isOngoing) {
                parseMapsNotification(notification)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        if (sbn.packageName == GOOGLE_MAPS_PACKAGE) {
            Log.d(TAG, "Google Maps navigation ended")
            NavigationStateManager.clearNavigation()
        }
    }

    private fun parseMapsNotification(notification: Notification) {
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        // 1. Parse Distance
        val (distanceStr, distanceMeters) = extractDistance(title, text)

        // 2. Parse Instruction
        val instruction = extractInstruction(title, text)

        // 3. Parse Maneuver Type
        val maneuverType = determineManeuverType(title, text)

        // 4. Parse Trip Stats (remaining distance, time, ETA)
        val (remDist, remTime, eta) = extractTripStats(text, subText)

        // 5. Extract Maneuver Icon if available
        var maneuverBitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val largeIcon: Icon? = notification.getLargeIcon()
                if (largeIcon != null) {
                    val drawable = largeIcon.loadDrawable(this)
                    if (drawable != null) {
                        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 120
                        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 120
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        maneuverBitmap = bitmap
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load maneuver icon: ${e.message}")
        }

        val navData = NavigationData(
            instruction = instruction,
            distance = distanceStr,
            distanceMeters = distanceMeters,
            remainingDistance = remDist,
            remainingTime = remTime,
            eta = eta,
            maneuverType = maneuverType,
            maneuverIcon = maneuverBitmap,
            isNavigating = true,
            isSimulated = false,
            timestamp = System.currentTimeMillis()
        )

        NavigationStateManager.updateNavigation(navData)

        // Auto-launch AOD if enabled and screen is locked or not yet open
        if (prefs.isAutoLaunchEnabled && !AodNavigationActivity.isActive) {
            launchAodActivity()
        }
    }

    private fun extractDistance(title: String, text: String): Pair<String, Int> {
        val pattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(m|km|ft|mi)", Pattern.CASE_INSENSITIVE)
        
        var matcher = pattern.matcher(title)
        if (!matcher.find()) {
            matcher = pattern.matcher(text)
            if (!matcher.find()) {
                return Pair("--", 0)
            }
        }

        val rawNum = matcher.group(1)?.replace(",", ".") ?: "0"
        val unit = matcher.group(2)?.lowercase() ?: "m"
        val value = rawNum.toFloatOrNull() ?: 0f

        val meters = when (unit) {
            "km" -> (value * 1000).toInt()
            "mi" -> (value * 1609.34).toInt()
            "ft" -> (value * 0.3048).toInt()
            else -> value.toInt()
        }

        return Pair("$rawNum $unit", meters)
    }

    private fun extractInstruction(title: String, text: String): String {
        return when {
            title.contains("turn", ignoreCase = true) || 
            title.contains("head", ignoreCase = true) ||
            title.contains("keep", ignoreCase = true) ||
            title.contains("continue", ignoreCase = true) -> title
            text.isNotBlank() -> text
            else -> title.ifBlank { "Proceed along route" }
        }
    }

    private fun determineManeuverType(title: String, text: String): ManeuverType {
        val combined = "$title $text".lowercase()
        return when {
            combined.contains("u-turn") -> ManeuverType.U_TURN
            combined.contains("sharp left") -> ManeuverType.SHARP_LEFT
            combined.contains("sharp right") -> ManeuverType.SHARP_RIGHT
            combined.contains("slight left") || combined.contains("keep left") || combined.contains("exit left") -> ManeuverType.SLIGHT_LEFT
            combined.contains("slight right") || combined.contains("keep right") || combined.contains("exit right") -> ManeuverType.SLIGHT_RIGHT
            combined.contains("left") -> ManeuverType.TURN_LEFT
            combined.contains("right") -> ManeuverType.TURN_RIGHT
            combined.contains("roundabout") || combined.contains("rotary") -> ManeuverType.ROUNDABOUT
            combined.contains("destination") || combined.contains("arrived") || combined.contains("arrive") -> ManeuverType.DESTINATION
            combined.contains("straight") || combined.contains("continue") || combined.contains("head") -> ManeuverType.STRAIGHT
            else -> ManeuverType.STRAIGHT
        }
    }

    private fun extractTripStats(text: String, subText: String): Triple<String, String, String> {
        val combined = "$text · $subText"
        val parts = combined.split("·", " - ").map { it.trim() }.filter { it.isNotBlank() }

        var remDist = "--"
        var remTime = "--"
        var eta = "--"

        for (part in parts) {
            val lower = part.lowercase()
            when {
                lower.contains("km") || lower.contains("mi") -> remDist = part
                lower.contains("min") || lower.contains("hr") || lower.contains("hour") -> remTime = part
                lower.contains("am") || lower.contains("pm") || lower.contains("eta") -> eta = part
            }
        }

        return Triple(remDist, remTime, eta)
    }

    private fun launchAodActivity() {
        val intent = Intent(this, AodNavigationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "MapNotificationListener"
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}
