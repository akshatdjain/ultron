package com.akshatdjain.ultron.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.akshatdjain.ultron.util.UltronLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "UltronBLE"
private const val SCAN_TIMEOUT_MS = 12_000L

enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Ready
}

data class BleScanResult(
    val device: BluetoothDevice,
    val name: String?,
    val address: String,
    val rssi: Int
)

class LightBleManager(private val context: Context) {
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyCharacteristic: BluetoothGattCharacteristic? = null
    private var scanTimeoutJob: Job? = null
    private var activeScanCallback: ScanCallback? = null

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _discoveredDevices = MutableStateFlow<List<BleScanResult>>(emptyList())
    val discoveredDevices: StateFlow<List<BleScanResult>> = _discoveredDevices

    private val _responseFrames = MutableSharedFlow<ByteArray>()
    val responseFrames: SharedFlow<ByteArray> = _responseFrames

    private val scope = CoroutineScope(Dispatchers.Main)

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            UltronLogger.d(TAG, "onConnectionStateChange status=$status newState=$newState")
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.Connected
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    UltronLogger.e(TAG, "GATT disconnected with error status=$status")
                }
                _connectionState.value = ConnectionState.Disconnected
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            UltronLogger.d(TAG, "onServicesDiscovered status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) return

            gatt ?: return
            val service = gatt.getService(LightProtocol.SERVICE_UUID)
            if (service == null) {
                UltronLogger.e(TAG, "Service ${LightProtocol.SERVICE_UUID} not found on device")
                return
            }

            writeCharacteristic = service.getCharacteristic(LightProtocol.WRITE_CHAR_UUID)
            notifyCharacteristic = service.getCharacteristic(LightProtocol.NOTIFY_CHAR_UUID)

            if (writeCharacteristic == null) UltronLogger.e(TAG, "Write characteristic not found")
            if (notifyCharacteristic == null) UltronLogger.e(TAG, "Notify characteristic not found")

            if (notifyCharacteristic != null) {
                gatt.setCharacteristicNotification(notifyCharacteristic, true)
                val descriptor = notifyCharacteristic?.getDescriptor(LightProtocol.CCCD_UUID)
                descriptor?.value = byteArrayOf(0x01, 0x00)
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            UltronLogger.d(TAG, "onDescriptorWrite status=$status")
            if (descriptor?.uuid == LightProtocol.CCCD_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.requestMtu(255)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            UltronLogger.d(TAG, "onMtuChanged mtu=$mtu status=$status")
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
        UltronLogger.d(TAG, "connect() address=${device.address}")
        _connectionState.value = ConnectionState.Connecting
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun connectByAddress(address: String): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            UltronLogger.e(TAG, "connectByAddress: no Bluetooth adapter")
            return false
        }
        return try {
            connect(adapter.getRemoteDevice(address))
            true
        } catch (e: IllegalArgumentException) {
            UltronLogger.e(TAG, "connectByAddress: invalid saved address=$address")
            false
        }
    }

    fun disconnect() {
        UltronLogger.d(TAG, "disconnect() requested")
        bluetoothGatt?.disconnect()
    }

    fun sendCommand(hexString: String) {
        val characteristic = writeCharacteristic
        if (characteristic == null) {
            UltronLogger.e(TAG, "sendCommand($hexString) dropped: no write characteristic")
            return
        }
        val frameBytes = LightProtocol.frameCommand(hexString)
        characteristic.value = frameBytes
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun startScan() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            UltronLogger.e(TAG, "startScan: no Bluetooth adapter on this device")
            return
        }
        if (!adapter.isEnabled) {
            UltronLogger.e(TAG, "startScan: Bluetooth is turned off")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            UltronLogger.e(TAG, "startScan: bluetoothLeScanner unavailable")
            return
        }

        stopScan()
        _discoveredDevices.value = emptyList()
        _isScanning.value = true
        UltronLogger.d(TAG, "startScan: scanning for nearby BLE devices (unfiltered)")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device ?: return
                val name = try {
                    result.scanRecord?.deviceName ?: device.name
                } catch (e: SecurityException) {
                    null
                }
                val entry = BleScanResult(device, name, device.address, result.rssi)
                _discoveredDevices.update { current ->
                    if (current.none { it.address == entry.address }) {
                        UltronLogger.d(TAG, "found device name=${entry.name} address=${entry.address} rssi=${entry.rssi}")
                        current + entry
                    } else {
                        current.map { if (it.address == entry.address) entry else it }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                UltronLogger.e(TAG, "onScanFailed errorCode=$errorCode")
                _isScanning.value = false
            }
        }
        activeScanCallback = callback

        try {
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        } catch (e: SecurityException) {
            UltronLogger.e(TAG, "startScan: missing BLUETOOTH_SCAN permission")
            _isScanning.value = false
            activeScanCallback = null
            return
        }

        scanTimeoutJob = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            UltronLogger.d(TAG, "startScan: timed out, found ${_discoveredDevices.value.size} device(s)")
            stopScan()
        }
    }

    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        val callback = activeScanCallback ?: return
        activeScanCallback = null
        _isScanning.value = false
        val scanner = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeScanner ?: return
        try {
            scanner.stopScan(callback)
        } catch (e: SecurityException) {
            UltronLogger.e(TAG, "stopScan: missing BLUETOOTH_SCAN permission")
        }
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
