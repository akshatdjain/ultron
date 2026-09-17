package com.akshatdjain.ultron.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Ready
}

class LightBleManager(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _responseFrames = MutableSharedFlow<ByteArray>()
    val responseFrames: SharedFlow<ByteArray> = _responseFrames

    private val scope = CoroutineScope(Dispatchers.Main)

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.Disconnected
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            gatt ?: return
            val service = gatt.getService(LightProtocol.SERVICE_UUID) ?: return

            writeCharacteristic = service.getCharacteristic(LightProtocol.WRITE_CHAR_UUID)
            notifyCharacteristic = service.getCharacteristic(LightProtocol.NOTIFY_CHAR_UUID)

            if (notifyCharacteristic != null) {
                gatt.setCharacteristicNotification(notifyCharacteristic, true)
                val descriptor = notifyCharacteristic?.getDescriptor(LightProtocol.CCCD_UUID)
                descriptor?.value = byteArrayOf(0x01, 0x00)
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (descriptor?.uuid == LightProtocol.CCCD_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.requestMtu(255)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && mtu >= 255) {
                scope.launch {
                    delay(1000)
                    _connectionState.value = ConnectionState.Ready
                    sendCommand(LightProtocol.Commands.HANDSHAKE)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic ?: return

            val data = characteristic.value ?: return
            val frameStrings = LightProtocol.splitFrames(data)
            for (frameString in frameStrings) {
                val decoded = LightProtocol.decodeFrame(frameString)
                if (decoded != null) {
                    scope.launch {
                        _responseFrames.emit(decoded)
                    }
                }
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.Connecting
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    fun sendCommand(hexString: String) {
        val frameBytes = LightProtocol.frameCommand(hexString)
        writeCharacteristic?.let {
            it.value = frameBytes
            it.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    fun scan(onDeviceFound: (BluetoothDevice) -> Unit) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        val scanner = adapter.bluetoothLeScanner ?: return

        val callback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult?) {
                result?.device?.let { onDeviceFound(it) }
            }
        }

        val scanFilters = listOf(
            android.bluetooth.le.ScanFilter.Builder()
                .setServiceUuid(android.os.ParcelUuid(LightProtocol.SERVICE_UUID))
                .build()
        )

        scanner.startScan(scanFilters, android.bluetooth.le.ScanSettings.Builder().build(), callback)
    }

    companion object {
        private var instance: LightBleManager? = null

        fun getInstance(context: Context): LightBleManager {
            if (instance == null) {
                instance = LightBleManager(context.applicationContext)
            }
            return instance!!
        }
    }
}
