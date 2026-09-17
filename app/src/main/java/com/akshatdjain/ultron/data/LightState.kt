package com.akshatdjain.ultron.data

import android.graphics.Color
import com.akshatdjain.ultron.ble.ConnectionState

data class LightState(
    val power: Boolean = false,
    val mode: Int = 0,
    val colorPrimary: Int = Color.WHITE,
    val colorSecondary: Int = Color.BLACK,
    val brightness: Int = 50,
    val speed: Int = 0,
    val voiceLevel: Int = 0,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    // "Welcome" feature: device-side greeting light-show config, set once and rarely touched.
    val welcomeEnabled: Boolean = false,
    val welcomeModeIndex: Int = 0,
    val welcomeColorIndex: Int = -1
)

object LightStateParser {
    fun parseStatusFrame(frameBytes: ByteArray): LightState? {
        if (frameBytes.isEmpty()) return null

        val frameType = frameBytes[0].toInt() and 0xFF

        return when (frameType) {
            0x12, 0x1F, 0x4E -> parseDetailedStatus(frameBytes)
            else -> null
        }
    }

    private fun parseDetailedStatus(frameBytes: ByteArray): LightState? {
        if (frameBytes.size < 15) return null

        val mode = frameBytes.getOrNull(1)?.toInt() ?: 0
        val r1 = frameBytes.getOrNull(4)?.toInt() ?: 0
        val g1 = frameBytes.getOrNull(5)?.toInt() ?: 0
        val b1 = frameBytes.getOrNull(6)?.toInt() ?: 0
        val r2 = frameBytes.getOrNull(7)?.toInt() ?: 0
        val g2 = frameBytes.getOrNull(8)?.toInt() ?: 0
        val b2 = frameBytes.getOrNull(9)?.toInt() ?: 0
        val brightness = frameBytes.getOrNull(10)?.toInt() ?: 50
        val speed = frameBytes.getOrNull(11)?.toInt() ?: 0
        val voiceLevel = frameBytes.getOrNull(12)?.toInt() ?: 0

        val colorPrimary = Color.rgb(r1, g1, b1)
        val colorSecondary = Color.rgb(r2, g2, b2)

        return LightState(
            power = true,
            mode = mode,
            colorPrimary = colorPrimary,
            colorSecondary = colorSecondary,
            brightness = brightness.coerceIn(0, 100),
            speed = speed,
            voiceLevel = voiceLevel,
            connectionState = ConnectionState.Ready
        )
    }
}
