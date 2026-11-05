package com.naviaod.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.naviaod.app.R
import com.naviaod.app.battery.BatteryOptimizationManager
import com.naviaod.app.databinding.ActivityAodNavigationBinding
import com.naviaod.app.model.ManeuverType
import com.naviaod.app.model.NavigationData
import com.naviaod.app.service.NavigationStateManager
import com.naviaod.app.util.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AodNavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAodNavigationBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var batteryManager: BatteryOptimizationManager
    private lateinit var gestureDetector: GestureDetector
    private val handler = Handler(Looper.getMainLooper())

    private var isMapPeekActive = false
    private val mapPeekTimeoutRunnable = Runnable { hideMapPeek() }

    private val clockTickRunnable = object : Runnable {
        override fun run() {
            updateAmbientClock()
            handler.postDelayed(this, 10_000L) // Update clock every 10 seconds
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val pct = (level * 100 / scale.toFloat()).toInt()
                    NavigationStateManager.updateBattery(pct)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockscreenAodWindow()

        binding = ActivityAodNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        batteryManager = BatteryOptimizationManager(
            context = this,
            shiftTargetView = binding.shiftContainer,
            blankOverlay = binding.deepBlankOverlay,
            dimmerOverlay = binding.softwareDimmerOverlay
        )

        setupGestures()
        applyAccentColor()
        observeNavigationState()
    }

    override fun onStart() {
        super.onStart()
        isActive = true
        batteryManager.start()
        handler.post(clockTickRunnable)

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        isActive = false
        batteryManager.stop()
        handler.removeCallbacks(clockTickRunnable)
        handler.removeCallbacks(mapPeekTimeoutRunnable)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun configureLockscreenAodWindow() {
        // Show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Immersive sticky full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun applyAccentColor() {
        val accent = prefs.accentColor
        binding.tvManeuverDistance.setTextColor(accent)
    }

    private fun observeNavigationState() {
        lifecycleScope.launch {
            NavigationStateManager.navigationState.collectLatest { navData ->
                updateNavigationUi(navData)
                batteryManager.evaluateDistanceForSleep(navData.distanceMeters)
            }
        }

        lifecycleScope.launch {
            NavigationStateManager.currentSpeedKmh.collectLatest { speed ->
                binding.tvSpeed.text = "$speed km/h"
            }
        }

        lifecycleScope.launch {
            NavigationStateManager.batteryPercent.collectLatest { pct ->
                binding.tvBatteryPercent.text = "$pct%"
            }
        }
    }

    private fun updateNavigationUi(data: NavigationData) {
        // Update distance
        binding.tvManeuverDistance.text = data.distance

        // Update instruction
        binding.tvInstruction.text = data.instruction

        // Update trip stats
        binding.tvRemainingDistance.text = data.remainingDistance
        binding.tvEta.text = "${data.remainingTime} · ${data.eta}"

        // Update maneuver arrow icon
        if (data.maneuverIcon != null) {
            binding.ivManeuverArrow.setImageBitmap(data.maneuverIcon)
            binding.ivManeuverArrow.colorFilter = null
        } else {
            val arrowRes = getVectorDrawableForManeuver(data.maneuverType)
            binding.ivManeuverArrow.setImageResource(arrowRes)
            binding.ivManeuverArrow.setColorFilter(Color.WHITE)
        }

        // Update map peek view
        binding.oledMapView.updateManeuver(data.maneuverType, data.instruction)
    }

    private fun getVectorDrawableForManeuver(type: ManeuverType): Int {
        return when (type) {
            ManeuverType.STRAIGHT -> R.drawable.ic_arrow_straight
            ManeuverType.TURN_LEFT -> R.drawable.ic_arrow_turn_left
            ManeuverType.TURN_RIGHT -> R.drawable.ic_arrow_turn_right
            ManeuverType.SLIGHT_LEFT -> R.drawable.ic_arrow_slight_left
            ManeuverType.SLIGHT_RIGHT -> R.drawable.ic_arrow_slight_right
            ManeuverType.SHARP_LEFT -> R.drawable.ic_arrow_sharp_left
            ManeuverType.SHARP_RIGHT -> R.drawable.ic_arrow_sharp_right
            ManeuverType.U_TURN -> R.drawable.ic_arrow_u_turn
            ManeuverType.ROUNDABOUT -> R.drawable.ic_roundabout
            ManeuverType.DESTINATION -> R.drawable.ic_destination
            ManeuverType.UNKNOWN -> R.drawable.ic_arrow_straight
        }
    }

    private fun updateAmbientClock() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        binding.tvAodClock.text = currentTime
        binding.tvSleepClock.text = "$currentTime · AOD Sleep"
    }

    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                launchGoogleMaps()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (prefs.isMapPeekEnabled) {
                    toggleMapPeek()
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Swipe down to dismiss AOD
                if (e1 != null && e2.y - e1.y > 200 && velocityY > 300) {
                    finish()
                    return true
                }
                return false
            }
        })

        binding.rootAodLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun toggleMapPeek() {
        if (isMapPeekActive) {
            hideMapPeek()
        } else {
            showMapPeek()
        }
    }

    private fun showMapPeek() {
        isMapPeekActive = true
        binding.oledMapView.visibility = View.VISIBLE
        binding.shiftContainer.visibility = View.INVISIBLE
        handler.removeCallbacks(mapPeekTimeoutRunnable)
        handler.postDelayed(mapPeekTimeoutRunnable, 8000L) // Auto return after 8 seconds
    }

    private fun hideMapPeek() {
        isMapPeekActive = false
        binding.oledMapView.visibility = View.GONE
        binding.shiftContainer.visibility = View.VISIBLE
        handler.removeCallbacks(mapPeekTimeoutRunnable)
    }

    private fun launchGoogleMaps() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // fallback
        }
    }

    companion object {
        var isActive = false
            private set
    }
}
