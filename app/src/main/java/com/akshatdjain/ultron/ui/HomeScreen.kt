package com.akshatdjain.ultron.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akshatdjain.ultron.ble.BleScanResult
import com.akshatdjain.ultron.data.LightState
import com.akshatdjain.ultron.ui.components.BrightnessSlider
import com.akshatdjain.ultron.ui.components.ColorWheel
import com.akshatdjain.ultron.ui.components.ConnectionStatusBar
import com.akshatdjain.ultron.ui.components.DevicePickerDialog
import com.akshatdjain.ultron.ui.components.ModeGrid
import android.graphics.Color as AndroidColor

private val PRESET_COLORS = listOf(
    0xFFFF3B30.toInt(), 0xFFFF9500.toInt(), 0xFFFFCC00.toInt(), 0xFF34C759.toInt(),
    0xFF00C7BE.toInt(), 0xFF32ADE6.toInt(), 0xFF007AFF.toInt(), 0xFF5856D6.toInt(),
    0xFFAF52DE.toInt(), 0xFFFF2D55.toInt()
)

@Composable
fun HomeScreen(
    state: LightState,
    onColorPick: (Int) -> Unit,
    onModeSelect: (String) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onPowerToggle: () -> Unit,
    onConnectClick: () -> Unit,
    onSettingsClick: () -> Unit,
    pickerVisible: Boolean = false,
    isScanning: Boolean = false,
    discoveredDevices: List<BleScanResult> = emptyList(),
    onDeviceSelected: (BleScanResult) -> Unit = {},
    onDismissPicker: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = Color(state.colorPrimary),
        animationSpec = tween(400),
        label = "accentColor"
    )
    val hsv = remember(state.colorPrimary) {
        FloatArray(3).also { AndroidColor.colorToHSV(state.colorPrimary, it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.22f), Color.Transparent)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionStatusBar(
                    connectionState = state.connectionState,
                    onConnectClick = onConnectClick,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            PowerRow(state.power, accentColor, onPowerToggle)

            Crossfade(targetState = state.power, label = "powerCrossfade") { isOn ->
                if (isOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        ColorWheel(
                            hue = hsv[0],
                            saturation = hsv[1],
                            onColorChange = { h, s -> onColorPick(Color.hsv(h, s, 1f).toArgb()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        )
                        PresetSwatches(state.colorPrimary, onColorPick)
                        BrightnessSlider(state.brightness, accentColor, onBrightnessChange)
                        ModeGrid(state.selectedModeCommand, onModeSelect)
                    }
                } else {
                    Text(
                        text = "Power is off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 40.dp)
                    )
                }
            }
        }
    }

    if (pickerVisible) {
        DevicePickerDialog(
            isScanning = isScanning,
            devices = discoveredDevices,
            onDeviceSelected = onDeviceSelected,
            onDismiss = onDismissPicker
        )
    }
}

@Composable
private fun PowerRow(power: Boolean, accentColor: Color, onPowerToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ambient Light",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(
            checked = power,
            onCheckedChange = { onPowerToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun PresetSwatches(currentColor: Int, onColorPick: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(PRESET_COLORS) { colorInt ->
            val isSelected = (colorInt and 0xFFFFFF) == (currentColor and 0xFFFFFF)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = CircleShape
                    )
                    .clickable { onColorPick(colorInt) }
            )
        }
    }
}
