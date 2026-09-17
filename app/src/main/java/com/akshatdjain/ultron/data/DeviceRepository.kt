package com.akshatdjain.ultron.data

import android.content.Context

class DeviceRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ultron_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ADDRESS = "last_paired_device_address"
    }

    fun saveDeviceAddress(address: String) {
        prefs.edit().putString(KEY_DEVICE_ADDRESS, address).apply()
    }

    fun getSavedDeviceAddress(): String? {
        return prefs.getString(KEY_DEVICE_ADDRESS, null)
    }
}
