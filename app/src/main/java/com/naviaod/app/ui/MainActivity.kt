package com.naviaod.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.naviaod.app.R
import com.naviaod.app.databinding.ActivityMainBinding
import com.naviaod.app.service.AodForegroundService
import com.naviaod.app.service.NavigationStateManager
import com.naviaod.app.simulator.NavigationSimulator
import com.naviaod.app.util.PreferencesManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var simulator: NavigationSimulator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        simulator = NavigationSimulator(this)

        setupPermissionButtons()
        setupProfileRadios()
        setupFeatureSwitches()
        setupAccentColorButtons()
        setupActionButtons()
        observeNavigationStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionCardStates()
    }

    private fun setupPermissionButtons() {
        binding.btnGrantNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        binding.btnGrantBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        // Request Location for passive speedometer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
        }
    }

    private fun updatePermissionCardStates() {
        // 1. Notification Listener
        val notifGranted = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        binding.btnGrantNotification.isEnabled = !notifGranted
        binding.btnGrantNotification.text = if (notifGranted) getString(R.string.btn_granted) else getString(R.string.btn_grant)

        // 2. Overlay
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
        binding.btnGrantOverlay.isEnabled = !overlayGranted
        binding.btnGrantOverlay.text = if (overlayGranted) getString(R.string.btn_granted) else getString(R.string.btn_grant)

        // 3. Battery Optimization
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else true
        binding.btnGrantBattery.isEnabled = !batteryGranted
        binding.btnGrantBattery.text = if (batteryGranted) getString(R.string.btn_granted) else getString(R.string.btn_grant)

        // Auto start background service if permissions ready
        if (notifGranted) {
            AodForegroundService.start(this)
        }
    }

    private fun setupProfileRadios() {
        when (prefs.profile) {
            PreferencesManager.PROFILE_EXTREME -> binding.rbProfileExtreme.isChecked = true
            PreferencesManager.PROFILE_BALANCED -> binding.rbProfileBalanced.isChecked = true
        }

        binding.rgProfiles.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbProfileExtreme -> {
                    prefs.applyPreset(PreferencesManager.PROFILE_EXTREME)
                    syncSwitchesWithPrefs()
                    Toast.makeText(this, "Extreme AMOLED Saver activated", Toast.LENGTH_SHORT).show()
                }
                R.id.rbProfileBalanced -> {
                    prefs.applyPreset(PreferencesManager.PROFILE_BALANCED)
                    syncSwitchesWithPrefs()
                    Toast.makeText(this, "Balanced OLED HUD activated", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFeatureSwitches() {
        syncSwitchesWithPrefs()

        binding.switchWakeOnTurn.setOnCheckedChangeListener { _, isChecked ->
            prefs.isWakeOnTurnEnabled = isChecked
            prefs.profile = PreferencesManager.PROFILE_CUSTOM
        }

        binding.switchPocketDetection.setOnCheckedChangeListener { _, isChecked ->
            prefs.isPocketDetectionEnabled = isChecked
            prefs.profile = PreferencesManager.PROFILE_CUSTOM
        }

        binding.switchBurnInProtection.setOnCheckedChangeListener { _, isChecked ->
            prefs.isBurnInProtectionEnabled = isChecked
            prefs.profile = PreferencesManager.PROFILE_CUSTOM
        }

        binding.switchFpsThrottling.setOnCheckedChangeListener { _, isChecked ->
            prefs.isFpsThrottlingEnabled = isChecked
            prefs.profile = PreferencesManager.PROFILE_CUSTOM
        }

        binding.switchSoftwareDimmer.setOnCheckedChangeListener { _, isChecked ->
            prefs.isSoftwareDimmerEnabled = isChecked
            prefs.profile = PreferencesManager.PROFILE_CUSTOM
        }

        binding.switchMapPeek.setOnCheckedChangeListener { _, isChecked ->
            prefs.isMapPeekEnabled = isChecked
            prefs.profile = PreferencesManager.PROFILE_CUSTOM
        }

        binding.switchAutoLaunch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAutoLaunchEnabled = isChecked
        }
    }

    private fun syncSwitchesWithPrefs() {
        binding.switchWakeOnTurn.isChecked = prefs.isWakeOnTurnEnabled
        binding.switchPocketDetection.isChecked = prefs.isPocketDetectionEnabled
        binding.switchBurnInProtection.isChecked = prefs.isBurnInProtectionEnabled
        binding.switchFpsThrottling.isChecked = prefs.isFpsThrottlingEnabled
        binding.switchSoftwareDimmer.isChecked = prefs.isSoftwareDimmerEnabled
        binding.switchMapPeek.isChecked = prefs.isMapPeekEnabled
        binding.switchAutoLaunch.isChecked = prefs.isAutoLaunchEnabled
    }

    private fun setupAccentColorButtons() {
        binding.btnColorCyan.setOnClickListener {
            prefs.accentColorHex = PreferencesManager.COLOR_CYAN
            Toast.makeText(this, "Electric Cyan Accent Selected", Toast.LENGTH_SHORT).show()
        }
        binding.btnColorAmber.setOnClickListener {
            prefs.accentColorHex = PreferencesManager.COLOR_AMBER
            Toast.makeText(this, "AMOLED Amber Accent Selected", Toast.LENGTH_SHORT).show()
        }
        binding.btnColorGreen.setOnClickListener {
            prefs.accentColorHex = PreferencesManager.COLOR_GREEN
            Toast.makeText(this, "Racing Green Accent Selected", Toast.LENGTH_SHORT).show()
        }
        binding.btnColorWhite.setOnClickListener {
            prefs.accentColorHex = PreferencesManager.COLOR_WHITE
            Toast.makeText(this, "Stealth White Accent Selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons() {
        binding.btnTestAod.setOnClickListener {
            val intent = Intent(this, AodNavigationActivity::class.java)
            startActivity(intent)
        }

        binding.btnToggleSimulation.setOnClickListener {
            if (simulator.isRunning) {
                simulator.stop()
                binding.btnToggleSimulation.text = getString(R.string.btn_start_simulation)
                Toast.makeText(this, "Simulation Stopped", Toast.LENGTH_SHORT).show()
            } else {
                simulator.start()
                binding.btnToggleSimulation.text = getString(R.string.btn_stop_simulation)
                Toast.makeText(this, "Pune Route Simulator Started! Tap 'Test AOD HUD Now'", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeNavigationStatus() {
        lifecycleScope.launch {
            NavigationStateManager.navigationState.collectLatest { nav ->
                if (nav.isNavigating) {
                    if (nav.isSimulated) {
                        binding.tvLiveStatus.text = getString(R.string.status_simulating)
                        binding.viewStatusDot.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.accent_amber)
                    } else {
                        binding.tvLiveStatus.text = getString(R.string.status_active)
                        binding.viewStatusDot.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.success_green)
                    }
                } else {
                    binding.tvLiveStatus.text = getString(R.string.status_idle)
                    binding.viewStatusDot.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.oled_gray_medium)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (simulator.isRunning) {
            simulator.stop()
        }
    }

    companion object {
        private const val REQ_LOCATION = 101
    }
}
