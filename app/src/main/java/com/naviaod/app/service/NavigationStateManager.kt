package com.naviaod.app.service

import com.naviaod.app.model.NavigationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NavigationStateManager {

    private val _navigationState = MutableStateFlow(NavigationData())
    val navigationState: StateFlow<NavigationData> = _navigationState.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0)
    val currentSpeedKmh: StateFlow<Int> = _currentSpeedKmh.asStateFlow()

    private val _batteryPercent = MutableStateFlow(100)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    private val _isScreenBlanked = MutableStateFlow(false)
    val isScreenBlanked: StateFlow<Boolean> = _isScreenBlanked.asStateFlow()

    fun updateNavigation(data: NavigationData) {
        _navigationState.value = data
    }

    fun updateSpeed(speedKmh: Int) {
        _currentSpeedKmh.value = speedKmh.coerceAtLeast(0)
    }

    fun updateBattery(percent: Int) {
        _batteryPercent.value = percent.coerceIn(0, 100)
    }

    fun setScreenBlanked(blanked: Boolean) {
        _isScreenBlanked.value = blanked
    }

    fun clearNavigation() {
        _navigationState.value = NavigationData(isNavigating = false)
        _isScreenBlanked.value = false
    }
}
