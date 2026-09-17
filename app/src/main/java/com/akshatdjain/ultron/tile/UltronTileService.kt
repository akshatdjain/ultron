package com.akshatdjain.ultron.tile

import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.edit
import com.akshatdjain.ultron.R
import com.akshatdjain.ultron.ble.ConnectionState
import com.akshatdjain.ultron.ble.LightBleManager
import com.akshatdjain.ultron.ble.LightProtocol
import com.akshatdjain.ultron.data.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val PREFS_NAME = "ultron_tile_prefs"
private const val KEY_POWER_ON = "power_on"

class UltronTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val turnOn = !prefs.getBoolean(KEY_POWER_ON, false)

        scope.launch {
            val manager = LightBleManager.getInstance(applicationContext)
            val command = if (turnOn) {
                LightProtocol.Commands.POWER_ON_2WAY
            } else {
                LightProtocol.Commands.POWER_OFF_2WAY
            }

            val ready = ensureReady(manager)
            if (ready) {
                manager.sendCommand(command)
                prefs.edit { putBoolean(KEY_POWER_ON, turnOn) }
            }
            refreshTile(if (ready) turnOn else null)
        }
    }

    private suspend fun ensureReady(manager: LightBleManager): Boolean {
        if (manager.connectionState.value == ConnectionState.Ready) return true

        val savedAddress = DeviceRepository(applicationContext).getSavedDeviceAddress() ?: return false
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter ?: return false
        val device = adapter.getRemoteDevice(savedAddress)
        manager.connect(device)

        return withTimeoutOrNull(3000) {
            var attempts = 0
            while (manager.connectionState.value != ConnectionState.Ready && attempts < 30) {
                delay(100)
                attempts++
            }
            manager.connectionState.value == ConnectionState.Ready
        } ?: false
    }

    private fun refreshTile(knownState: Boolean? = null) {
        val tile = qsTile ?: return
        val isOn = knownState ?: getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_POWER_ON, false)
        tile.state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_power)
        tile.label = "Ambient Light"
        tile.subtitle = if (isOn) "On" else "Off"
        tile.updateTile()
    }
}
