package com.naviaod.app.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var profile: String
        get() = prefs.getString(KEY_PROFILE, PROFILE_EXTREME) ?: PROFILE_EXTREME
        set(value) = prefs.edit().putString(KEY_PROFILE, value).apply()

    var isWakeOnTurnEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_ON_TURN, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_ON_TURN, value).apply()

    var wakeThresholdMeters: Int
        get() = prefs.getInt(KEY_WAKE_THRESHOLD, 500)
        set(value) = prefs.edit().putInt(KEY_WAKE_THRESHOLD, value).apply()

    var isPocketDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_POCKET_DETECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_POCKET_DETECTION, value).apply()

    var isBurnInProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_BURN_IN_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_BURN_IN_PROTECTION, value).apply()

    var isFpsThrottlingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FPS_THROTTLING, true)
        set(value) = prefs.edit().putBoolean(KEY_FPS_THROTTLING, value).apply()

    var isSoftwareDimmerEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOFTWARE_DIMMER, false)
        set(value) = prefs.edit().putBoolean(KEY_SOFTWARE_DIMMER, value).apply()

    var dimmerAlpha: Float
        get() = prefs.getFloat(KEY_DIMMER_ALPHA, 0.45f)
        set(value) = prefs.edit().putFloat(KEY_DIMMER_ALPHA, value).apply()

    var isMapPeekEnabled: Boolean
        get() = prefs.getBoolean(KEY_MAP_PEEK, true)
        set(value) = prefs.edit().putBoolean(KEY_MAP_PEEK, value).apply()

    var isAutoLaunchEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_LAUNCH, value).apply()

    var accentColorHex: String
        get() = prefs.getString(KEY_ACCENT_COLOR, COLOR_CYAN) ?: COLOR_CYAN
        set(value) = prefs.edit().putString(KEY_ACCENT_COLOR, value).apply()

    val accentColor: Int
        get() = try {
            Color.parseColor(accentColorHex)
        } catch (e: Exception) {
            Color.parseColor(COLOR_CYAN)
        }

    fun applyPreset(presetName: String) {
        profile = presetName
        when (presetName) {
            PROFILE_EXTREME -> {
                isWakeOnTurnEnabled = true
                wakeThresholdMeters = 500
                isPocketDetectionEnabled = true
                isBurnInProtectionEnabled = true
                isFpsThrottlingEnabled = true
                isSoftwareDimmerEnabled = true
                dimmerAlpha = 0.5f
                isMapPeekEnabled = false
            }
            PROFILE_BALANCED -> {
                isWakeOnTurnEnabled = false
                isPocketDetectionEnabled = true
                isBurnInProtectionEnabled = true
                isFpsThrottlingEnabled = true
                isSoftwareDimmerEnabled = false
                isMapPeekEnabled = true
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "naviaod_settings"

        const val PROFILE_EXTREME = "EXTREME"
        const val PROFILE_BALANCED = "BALANCED"
        const val PROFILE_CUSTOM = "CUSTOM"

        const val COLOR_CYAN = "#00E5FF"
        const val COLOR_AMBER = "#FFB300"
        const val COLOR_GREEN = "#00E676"
        const val COLOR_WHITE = "#FFFFFF"

        private const val KEY_PROFILE = "key_profile"
        private const val KEY_WAKE_ON_TURN = "key_wake_on_turn"
        private const val KEY_WAKE_THRESHOLD = "key_wake_threshold"
        private const val KEY_POCKET_DETECTION = "key_pocket_detection"
        private const val KEY_BURN_IN_PROTECTION = "key_burn_in_protection"
        private const val KEY_FPS_THROTTLING = "key_fps_throttling"
        private const val KEY_SOFTWARE_DIMMER = "key_software_dimmer"
        private const val KEY_DIMMER_ALPHA = "key_dimmer_alpha"
        private const val KEY_MAP_PEEK = "key_map_peek"
        private const val KEY_AUTO_LAUNCH = "key_auto_launch"
        private const val KEY_ACCENT_COLOR = "key_accent_color"
    }
}
