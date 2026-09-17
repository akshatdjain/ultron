package com.akshatdjain.ultron.voice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.akshatdjain.ultron.ble.ConnectionState
import com.akshatdjain.ultron.ble.LightBleManager
import com.akshatdjain.ultron.ble.LightProtocol
import com.akshatdjain.ultron.data.DeviceRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class VoiceActionActivity : ComponentActivity() {

    private val colorMap = mapOf(
        "red" to "FF0000",
        "green" to "00FF00",
        "blue" to "0000FF",
        "white" to "FFFFFF",
        "orange" to "FFA500",
        "purple" to "800080",
        "pink" to "FFC0CB"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            handleVoiceCommand()
            finish()
        }
    }

    private suspend fun handleVoiceCommand() {
        val action = intent?.action ?: return
        val manager = LightBleManager.getInstance(this)
        val repo = DeviceRepository(this)

        try {
            val hexCommand = when {
                action.endsWith("TURN_ON") -> {
                    LightProtocol.Commands.POWER_ON_2WAY
                }
                action.endsWith("TURN_OFF") -> {
                    LightProtocol.Commands.POWER_OFF_2WAY
                }
                action.endsWith("SET_COLOR") -> {
                    val colorName = intent?.getStringExtra("actions.fulfillment.fulfillment_intent.color")
                        ?.lowercase() ?: return
                    val hexColor = colorMap[colorName] ?: return
                    LightProtocol.Commands.ADJUST_ALL_ZONES + hexColor
                }
                action.endsWith("SET_BRIGHTNESS") -> {
                    val brightnessStr = intent?.getStringExtra("actions.fulfillment.fulfillment_intent.brightness")
                        ?: return
                    val brightnessPct = brightnessStr.removeSuffix("%").toIntOrNull()?.coerceIn(0, 100) ?: return
                    val brightnessHex = String.format("%02X", brightnessPct)
                    LightProtocol.Commands.BRIGHTNESS + brightnessHex
                }
                else -> return
            }

            if (manager.connectionState.value != ConnectionState.Ready) {
                connectAndSend(manager, repo, hexCommand)
            } else {
                manager.sendCommand(hexCommand)
                showToast("Command sent")
            }
        } catch (e: Exception) {
            showToast("Voice command failed")
        }
    }

    private suspend fun connectAndSend(
        manager: LightBleManager,
        repo: DeviceRepository,
        hexCommand: String
    ) {
        val savedAddress = repo.getSavedDeviceAddress() ?: run {
            showToast("No ambient light device saved")
            return
        }

        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)
            ?.adapter ?: run {
            showToast("Bluetooth not available")
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(savedAddress)

        manager.connect(device)

        val connected = withTimeoutOrNull(3000) {
            var attempts = 0
            while (manager.connectionState.value != ConnectionState.Ready && attempts < 30) {
                kotlinx.coroutines.delay(100)
                attempts++
            }
            manager.connectionState.value == ConnectionState.Ready
        } ?: false

        if (connected) {
            manager.sendCommand(hexCommand)
            showToast("Command sent")
        } else {
            showToast("Ambient light not connected")
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
