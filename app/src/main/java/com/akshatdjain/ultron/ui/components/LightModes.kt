package com.akshatdjain.ultron.ui.components

import com.akshatdjain.ultron.ble.LightProtocol

enum class ModeCategory { STATIC, FLOW, EXTENDED }

data class LightMode(val label: String, val command: String, val category: ModeCategory)

// command is always baseCode(2 hex) + index(2 hex); the device's status-frame MODE byte
// mirrors that index but doesn't carry category, so selection matching is best-effort.
val LIGHT_MODES: List<LightMode> = buildList {
    add(LightMode("Red", LightProtocol.Commands.STATIC_RED, ModeCategory.STATIC))
    add(LightMode("Orange", LightProtocol.Commands.STATIC_ORANGE, ModeCategory.STATIC))
    add(LightMode("Yellow", LightProtocol.Commands.STATIC_YELLOW, ModeCategory.STATIC))
    add(LightMode("Green", LightProtocol.Commands.STATIC_GREEN, ModeCategory.STATIC))
    add(LightMode("Blue", LightProtocol.Commands.STATIC_BLUE, ModeCategory.STATIC))
    add(LightMode("Cyan", "0805", ModeCategory.STATIC))
    add(LightMode("Purple", LightProtocol.Commands.STATIC_PURPLE, ModeCategory.STATIC))

    listOf(
        "Flow", "Wave", "Ripple", "Cascade", "Drift",
        "Current", "Surge", "Whirl", "Glide", "Tide"
    ).forEachIndexed { i, name ->
        add(LightMode(name, LightProtocol.Commands.WATER_MODE_BASE + "%02X".format(i), ModeCategory.FLOW))
    }

    listOf(
        "Breathing", "Pulse", "Strobe", "Fade", "Rainbow",
        "Chase", "Sparkle", "Comet", "Aurora", "Heartbeat",
        "Shimmer", "Fireworks"
    ).forEachIndexed { i, name ->
        add(LightMode(name, LightProtocol.Commands.EXTENDED_MODE_BASE + "%02X".format(i), ModeCategory.EXTENDED))
    }
}
