package com.akshatdjain.ultron.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
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

private const val ACTION_TOGGLE_POWER = "com.akshatdjain.ultron.widget.ACTION_TOGGLE_POWER"
private const val ACTION_BRIGHTNESS_UP = "com.akshatdjain.ultron.widget.ACTION_BRIGHTNESS_UP"
private const val ACTION_BRIGHTNESS_DOWN = "com.akshatdjain.ultron.widget.ACTION_BRIGHTNESS_DOWN"
private const val ACTION_SET_COLOR = "com.akshatdjain.ultron.widget.ACTION_SET_COLOR"
private const val EXTRA_COLOR = "color_hex"

private const val PREFS_NAME = "ultron_widget_prefs"
private const val KEY_POWER_ON = "power_on"
private const val KEY_BRIGHTNESS = "brightness"

// Fixed quick-access palette shown on the widget (a handful of presets, not the full picker).
private val WIDGET_COLORS = listOf("FF0000", "FFA500", "00FF00", "00BFFF", "FFFFFF")

class UltronWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val command: String = when (intent.action) {
            ACTION_TOGGLE_POWER -> {
                val turnOn = !prefs.getBoolean(KEY_POWER_ON, false)
                prefs.edit { putBoolean(KEY_POWER_ON, turnOn) }
                if (turnOn) LightProtocol.Commands.POWER_ON_2WAY else LightProtocol.Commands.POWER_OFF_2WAY
            }
            ACTION_BRIGHTNESS_UP -> {
                val level = (prefs.getInt(KEY_BRIGHTNESS, 50) + 10).coerceIn(0, 100)
                prefs.edit { putInt(KEY_BRIGHTNESS, level) }
                LightProtocol.Commands.BRIGHTNESS + "%02X".format(level)
            }
            ACTION_BRIGHTNESS_DOWN -> {
                val level = (prefs.getInt(KEY_BRIGHTNESS, 50) - 10).coerceIn(0, 100)
                prefs.edit { putInt(KEY_BRIGHTNESS, level) }
                LightProtocol.Commands.BRIGHTNESS + "%02X".format(level)
            }
            ACTION_SET_COLOR -> {
                val hex = intent.getStringExtra(EXTRA_COLOR) ?: return
                LightProtocol.Commands.ADJUST_ALL_ZONES + hex
            }
            else -> return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val manager = LightBleManager.getInstance(context.applicationContext)
                val ready = ensureReady(context, manager)
                if (ready) manager.sendCommand(command)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, UltronWidgetProvider::class.java))
                for (id in ids) updateWidget(context, appWidgetManager, id)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun ensureReady(context: Context, manager: LightBleManager): Boolean {
        if (manager.connectionState.value == ConnectionState.Ready) return true

        val savedAddress = DeviceRepository(context.applicationContext).getSavedDeviceAddress() ?: return false
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return false
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

    companion object {
        private val COLOR_VIEW_IDS = listOf(
            R.id.widget_color_1, R.id.widget_color_2, R.id.widget_color_3, R.id.widget_color_4, R.id.widget_color_5
        )

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isOn = prefs.getBoolean(KEY_POWER_ON, false)
            val brightness = prefs.getInt(KEY_BRIGHTNESS, 50)

            val views = RemoteViews(context.packageName, R.layout.widget_ultron)
            views.setTextViewText(R.id.widget_status, if (isOn) "On" else "Off")
            views.setTextViewText(R.id.widget_brightness_label, "$brightness%")

            views.setOnClickPendingIntent(
                R.id.widget_power_button,
                broadcastPendingIntent(context, appWidgetId, ACTION_TOGGLE_POWER)
            )
            views.setOnClickPendingIntent(
                R.id.widget_brightness_up,
                broadcastPendingIntent(context, appWidgetId, ACTION_BRIGHTNESS_UP, requestCodeOffset = 1)
            )
            views.setOnClickPendingIntent(
                R.id.widget_brightness_down,
                broadcastPendingIntent(context, appWidgetId, ACTION_BRIGHTNESS_DOWN, requestCodeOffset = 2)
            )

            WIDGET_COLORS.forEachIndexed { index, hex ->
                val viewId = COLOR_VIEW_IDS[index]
                views.setInt(viewId, "setColorFilter", 0xFF000000.toInt() or hex.toLong(16).toInt())
                val intent = Intent(context, UltronWidgetProvider::class.java).apply {
                    action = ACTION_SET_COLOR
                    putExtra(EXTRA_COLOR, hex)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + index + 3,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(viewId, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun broadcastPendingIntent(
            context: Context,
            appWidgetId: Int,
            action: String,
            requestCodeOffset: Int = 0
        ): PendingIntent {
            val intent = Intent(context, UltronWidgetProvider::class.java).apply { this.action = action }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + requestCodeOffset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
