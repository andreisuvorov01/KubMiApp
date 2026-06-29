package com.example.kubmi.presentation.screens.admin.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kubmi.kiosk.GaokeViewKioskDetector
import com.example.kubmi.kiosk.GaokeViewKioskDetector.GaokeViewStatus
import com.example.kubmi.util.KioskPermissionManager
import com.example.kubmi.util.KioskPermissionManager.KioskPermissionStatus
import com.example.kubmi.util.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val kioskEnabled: Boolean = true,
    val screensaverDelay: Int = 300,
    val dataRefreshInterval: Long = 30,
    val apiEndpoint: String = "https://kubmi.ru",
    val autoStartEnabled: Boolean = true,
    val bellScheduleText: String = "",
    val mainScreenTitle: String = "",
    val permissionStatus: KioskPermissionStatus? = null,
    val gaokeViewStatus: GaokeViewStatus? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferences,
    private val permissionManager: KioskPermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        startPermissionPolling()
        detectGaokeView()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            kioskEnabled = securePreferences.isKioskModeEnabled(),
            screensaverDelay = securePreferences.getScreensaverDelay(),
            dataRefreshInterval = securePreferences.getDataRefreshInterval(),
            apiEndpoint = securePreferences.getApiEndpoint(),
            autoStartEnabled = securePreferences.isAutoStartEnabled(),
            bellScheduleText = securePreferences.getBellScheduleText(),
            mainScreenTitle = securePreferences.getMainScreenTitle(),
            permissionStatus = permissionManager.getPermissionStatus()
        )
    }

    private fun startPermissionPolling() {
        viewModelScope.launch {
            while (isActive) {
                _uiState.value = _uiState.value.copy(
                    permissionStatus = permissionManager.getPermissionStatus()
                )
                delay(2000)
            }
        }
    }

    private fun detectGaokeView() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                gaokeViewStatus = GaokeViewKioskDetector.detect(context)
            )
        }
    }

    fun setKioskEnabled(enabled: Boolean) {
        securePreferences.saveKioskModeEnabled(enabled)
        _uiState.value = _uiState.value.copy(kioskEnabled = enabled)
    }

    fun setScreensaverDelay(delaySeconds: Int) {
        securePreferences.saveScreensaverDelay(delaySeconds)
        _uiState.value = _uiState.value.copy(screensaverDelay = delaySeconds)
    }

    fun setDataRefreshInterval(minutes: Long) {
        securePreferences.saveDataRefreshInterval(minutes)
        _uiState.value = _uiState.value.copy(dataRefreshInterval = minutes)
    }

    fun setApiEndpoint(url: String) {
        securePreferences.saveApiEndpoint(url)
        _uiState.value = _uiState.value.copy(apiEndpoint = url)
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        securePreferences.saveAutoStartEnabled(enabled)
        _uiState.value = _uiState.value.copy(autoStartEnabled = enabled)
    }

    fun setBellScheduleText(text: String) {
        securePreferences.saveBellScheduleText(text)
        _uiState.value = _uiState.value.copy(bellScheduleText = text)
    }

    fun setMainScreenTitle(title: String) {
        securePreferences.saveMainScreenTitle(title)
        _uiState.value = _uiState.value.copy(mainScreenTitle = title)
    }

    fun refreshPermissions() {
        _uiState.value = _uiState.value.copy(
            permissionStatus = permissionManager.getPermissionStatus()
        )
    }

    fun getPermissionManager(): KioskPermissionManager = permissionManager
}