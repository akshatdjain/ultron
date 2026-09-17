package com.akshatdjain.ultron.ble

import java.util.UUID

object LightProtocol {
    // GATT Service and Characteristic UUIDs
    val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805f9b34fb")
    val WRITE_CHAR_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805f9b34fb")
    val NOTIFY_CHAR_UUID = UUID.fromString("0000FFE2-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Command hex code constants
    object Commands {
        // Static color modes: "08" + color index (00-06)
        const val STATIC_COLOR_BASE = 0x08
        const val STATIC_RED = "0800"
        const val STATIC_ORANGE = "0801"
        const val STATIC_YELLOW = "0802"
        const val STATIC_GREEN = "0803"
        const val STATIC_BLUE = "0804"
        const val STATIC_PURPLE = "0806"

        // Water/flow animation: "04" + mode (00-14, 21 variants)
        const val WATER_MODE_BASE = "04"

        // Extended modes: "1E" + mode (00-1E, 30 variants)
        const val EXTENDED_MODE_BASE = "1E"

        // Custom 8-zone color toggle
        const val CUSTOM_ZONES_OFF = "2B00"
        const val CUSTOM_ZONES_ON = "2B01"

        // Set custom 8-zone colors: "2A" + 8 hex nibbles (0-F palette index per zone)
        const val SET_CUSTOM_ZONES = "2A"
        const val RESET_CUSTOM_ZONES = "2AFFFFFFFF"

        // Direct color adjustment
        const val ADJUST_ALL_ZONES = "06"       // all zones
        const val ADJUST_ZONE_1 = "10"
        const val ADJUST_ZONE_2 = "09"

        // Brightness: "3E" + value (00-64, 0-100 in hex)
        const val BRIGHTNESS = "3E"

        // Power/feature toggles
        const val POWER_TOGGLE_3WAY = "0700"
        const val POWER_ON_2WAY = "0701"
        const val POWER_OFF_2WAY = "0702"
        const val FEATURE_ON = "1401"
        const val FEATURE_OFF = "1400"

        // Voice/mic sensitivity: "0300" + level (00-FF)
        const val VOICE_LEVEL = "0300"

        // Open-alert toggles: "30"-"39" + "00"/"01"
        const val ALERT_BASE = "30"

        // Factory reset
        const val FACTORY_RESET = "4200"

        // Symphony (music-reactive) modes
        const val SYMPHONY_MODE_OFF = "3B00"
        const val SYMPHONY_MODE_ON = "3B01"
        const val SYMPHONY_RESET = "3AFFFFFFFF"

        // Symphony variants
        const val SYMPHONY_WATER_BASE = "29"
        const val SYMPHONY_SPEED = "0B"
        const val SYMPHONY_BRIGHTNESS_1 = "28"
        const val SYMPHONY_BRIGHTNESS_2 = "3C"
        const val SYMPHONY_BRIGHTNESS_3 = "3D"

        // Handshake/sync request
        const val HANDSHAKE = "0A01"
    }

    // Response frame type constants
    object ResponseTypes {
        const val HANDSHAKE_ACK = 0x0A
        const val STATUS_FRAME_1 = 0x12
        const val STATUS_FRAME_2 = 0x1F
        const val STATUS_FRAME_3 = 0x4E
        const val VERSION_INFO = 0x13
        const val CUSTOM_COLOR_ECHO = 0x27
        const val ERROR_ALERT = 0x2E
    }

    // Status frame byte layout indices
    object StatusFrameLayout {
        const val MODE = 1
        const val MODE2 = 2
        const val MODE3_ZONE = 3
        const val COLOR1_R = 4
        const val COLOR1_G = 5
        const val COLOR1_B = 6
        const val COLOR2_R = 7
        const val COLOR2_G = 8
        const val COLOR2_B = 9
        const val BRIGHTNESS = 10
        const val SPEED = 11
        const val VOICE_LEVEL = 12
        const val EXTRA_1 = 13
        const val EXTRA_2 = 14
    }

    fun frameCommand(hexString: String): ByteArray {
        val asciiBytes = hexString.toByteArray(Charsets.US_ASCII)
        return byteArrayOf(0x3C.toByte()) + asciiBytes + byteArrayOf(0x3E.toByte())
    }

    fun splitFrames(data: ByteArray): List<String> {
        val frames = mutableListOf<String>()
        var currentFrame = StringBuilder()
        var inFrame = false

        for (byte in data) {
            val char = byte.toInt().toChar()
            when {
                char == '<' -> {
                    inFrame = true
                    currentFrame = StringBuilder()
                }
                char == '>' && inFrame -> {
                    frames.add(currentFrame.toString())
                    inFrame = false
                }
                inFrame -> {
                    currentFrame.append(char)
                }
            }
        }

        return frames
    }

    fun decodeFrame(frameString: String): ByteArray? {
        return try {
            val pairs = frameString.chunked(2)
            ByteArray(pairs.size) { i ->
                pairs[i].toInt(16).toByte()
            }
        } catch (e: Exception) {
            null
        }
    }
}
