package com.akshatdjain.ultron.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akshatdjain.ultron.ble.BleScanResult
import com.akshatdjain.ultron.ble.LightBleManager
import com.akshatdjain.ultron.ble.LightProtocol
import com.akshatdjain.ultron.data.DeviceRepository
import com.akshatdjain.ultron.data.LightState
import com.akshatdjain.ultron.data.LightStateParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(context: Context) : ViewModel() {
    private val bleManager = LightBleManager.getInstance(context)
    private val deviceRepository = DeviceRepository(context)

    private val _uiState = MutableStateFlow(LightState())
    val uiState: StateFlow<LightState> = _uiState

    private val _pickerVisible = MutableStateFlow(false)
    val pickerVisible: StateFlow<Boolean> = _pickerVisible

    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val discoveredDevices: StateFlow<List<BleScanResult>> = bleManager.discoveredDevices

    init {
        viewModelScope.launch {
            bleManager.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }
        viewModelScope.launch {
            bleManager.responseFrames.collect { frame ->
                LightStateParser.parseStatusFrame(frame)?.let { parsed ->
                    _uiState.update { current ->
                        parsed.copy(
                            welcomeEnabled = current.welcomeEnabled,
                            welcomeModeIndex = current.welcomeModeIndex,
                            welcomeColorIndex = current.welcomeColorIndex
                        )
                    }
                }
            }
        }
    }

    fun onConnectClick() {
        _pickerVisible.value = true
        bleManager.startScan()
    }

    fun onDeviceSelected(result: BleScanResult) {
        _pickerVisible.value = false
        bleManager.stopScan()
        deviceRepository.saveDeviceAddress(result.address)
        bleManager.connect(result.device)
    }

    fun onDismissPicker() {
        _pickerVisible.value = false
        bleManager.stopScan()
    }

    fun reconnectToSavedDevice(): Boolean {
        val address = deviceRepository.getSavedDeviceAddress() ?: return false
        return bleManager.connectByAddress(address)
    }

    fun onPowerToggle() {
        val turningOn = !_uiState.value.power
        bleManager.sendCommand(
            if (turningOn) LightProtocol.Commands.POWER_ON_2WAY else LightProtocol.Commands.POWER_OFF_2WAY
        )
        _uiState.update { it.copy(power = turningOn) }
    }

    fun onColorPick(colorInt: Int) {
        val rgbHex = "%06X".format(colorInt and 0xFFFFFF)
        bleManager.sendCommand(LightProtocol.Commands.ADJUST_ALL_ZONES + rgbHex)
        _uiState.update { it.copy(colorPrimary = colorInt) }
    }

    fun onBrightnessChange(brightness: Int) {
        val clamped = brightness.coerceIn(0, 100)
        bleManager.sendCommand(LightProtocol.Commands.BRIGHTNESS + "%02X".format(clamped))
        _uiState.update { it.copy(brightness = clamped) }
    }

    fun onModeSelect(command: String) {
        bleManager.sendCommand(command)
    }

    fun onWelcomeToggle(enabled: Boolean) {
        bleManager.sendCommand(if (enabled) LightProtocol.Commands.WELCOME_ON else LightProtocol.Commands.WELCOME_OFF)
        _uiState.update { it.copy(welcomeEnabled = enabled) }
    }

    fun onWelcomeModeSelect(index: Int) {
        bleManager.sendCommand(LightProtocol.Commands.WELCOME_MODE_BASE + "%02X".format(index))
        _uiState.update { it.copy(welcomeModeIndex = index) }
    }

    fun onWelcomeColorSelect(index: Int) {
        bleManager.sendCommand(LightProtocol.Commands.WELCOME_COLOR_BASE + "%02X".format(index))
        _uiState.update { it.copy(welcomeColorIndex = index) }
    }

    fun onWelcomeColorSync() {
        bleManager.sendCommand(LightProtocol.Commands.WELCOME_COLOR_SYNC)
        _uiState.update { it.copy(welcomeColorIndex = -1) }
    }
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(context.applicationContext) as T
    }
}
