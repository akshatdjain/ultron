package com.akshatdjain.ultron.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    init {
        viewModelScope.launch {
            bleManager.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }
        viewModelScope.launch {
            bleManager.responseFrames.collect { frame ->
                LightStateParser.parseStatusFrame(frame)?.let { parsed -> _uiState.update { parsed } }
            }
        }
    }

    fun onConnectClick() {
        bleManager.scan { device ->
            deviceRepository.saveDeviceAddress(device.address)
            bleManager.connect(device)
        }
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
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(context.applicationContext) as T
    }
}
