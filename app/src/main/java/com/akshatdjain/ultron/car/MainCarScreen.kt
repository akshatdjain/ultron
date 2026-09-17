package com.akshatdjain.ultron.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.akshatdjain.ultron.R
import com.akshatdjain.ultron.ble.ConnectionState
import com.akshatdjain.ultron.ble.LightBleManager
import com.akshatdjain.ultron.ble.LightProtocol

class MainCarScreen(carContext: CarContext) : Screen(carContext) {
    private val bleManager = LightBleManager.getInstance(carContext)

    override fun onGetTemplate(): Template {
        val connectionState = bleManager.connectionState.value

        return when (connectionState) {
            ConnectionState.Ready -> buildReadyTemplate()
            ConnectionState.Disconnected -> buildMessageTemplate("Not Connected", "Please connect to a device")
            ConnectionState.Connecting -> buildMessageTemplate("Connecting...", "Please wait")
            ConnectionState.Connected -> buildMessageTemplate("Connecting...", "Handshaking with device")
        }
    }

    private fun buildReadyTemplate(): Template {
        val colorCommands = listOf(
            "Red" to LightProtocol.Commands.STATIC_RED,
            "Orange" to LightProtocol.Commands.STATIC_ORANGE,
            "Yellow" to LightProtocol.Commands.STATIC_YELLOW,
            "Green" to LightProtocol.Commands.STATIC_GREEN,
            "Blue" to LightProtocol.Commands.STATIC_BLUE,
            "Purple" to LightProtocol.Commands.STATIC_PURPLE
        )

        val modeCommands = listOf(
            "Water 1" to buildWaterMode(0x00),
            "Water 2" to buildWaterMode(0x07),
            "Water 3" to buildWaterMode(0x14),
            "Extended 1" to buildExtendedMode(0x00),
            "Extended 2" to buildExtendedMode(0x10),
            "Extended 3" to buildExtendedMode(0x1E)
        )

        val icon = CarIcon.Builder(
            IconCompat.createWithResource(carContext, R.drawable.ic_dot)
        ).build()

        val listBuilder = ItemList.Builder()
        for ((name, command) in colorCommands + modeCommands) {
            listBuilder.addItem(
                GridItem.Builder()
                    .setTitle(name)
                    .setImage(icon, GridItem.IMAGE_TYPE_ICON)
                    .setOnClickListener(createCommandListener(command))
                    .build()
            )
        }

        return GridTemplate.Builder()
            .setHeader(
                Header.Builder()
                    .setTitle("Ultron Controls")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .setSingleList(listBuilder.build())
            .setActionStrip(buildActionStrip())
            .build()
    }


    private fun buildWaterMode(mode: Int): String {
        val hex = String.format("%02X", mode)
        return LightProtocol.Commands.WATER_MODE_BASE + hex
    }

    private fun buildExtendedMode(mode: Int): String {
        val hex = String.format("%02X", mode)
        return LightProtocol.Commands.EXTENDED_MODE_BASE + hex
    }

    private fun buildActionStrip(): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                androidx.car.app.model.Action.Builder()
                    .setTitle("On")
                    .setOnClickListener(createCommandListener(LightProtocol.Commands.POWER_ON_2WAY))
                    .build()
            )
            .addAction(
                androidx.car.app.model.Action.Builder()
                    .setTitle("Off")
                    .setOnClickListener(createCommandListener(LightProtocol.Commands.POWER_OFF_2WAY))
                    .build()
            )
            .addAction(
                androidx.car.app.model.Action.Builder()
                    .setTitle("+")
                    .setOnClickListener(createCommandListener(buildBrightness(100)))
                    .build()
            )
            .addAction(
                androidx.car.app.model.Action.Builder()
                    .setTitle("-")
                    .setOnClickListener(createCommandListener(buildBrightness(0)))
                    .build()
            )
            .build()
    }

    private fun buildBrightness(level: Int): String {
        val hex = String.format("%02X", level.coerceIn(0, 100))
        return LightProtocol.Commands.BRIGHTNESS + hex
    }

    private fun createCommandListener(command: String): OnClickListener {
        return OnClickListener {
            bleManager.sendCommand(command)
            invalidate()
        }
    }

    private fun buildMessageTemplate(title: String, message: String): MessageTemplate {
        return MessageTemplate.Builder(message)
            .setTitle(title)
            .build()
    }
}
