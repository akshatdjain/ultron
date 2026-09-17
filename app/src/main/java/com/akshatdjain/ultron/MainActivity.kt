package com.akshatdjain.ultron

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akshatdjain.ultron.data.DeviceRepository
import com.akshatdjain.ultron.ui.HomeScreen
import com.akshatdjain.ultron.ui.HomeViewModel
import com.akshatdjain.ultron.ui.HomeViewModelFactory
import com.akshatdjain.ultron.ui.LogsScreen
import com.akshatdjain.ultron.ui.SettingsScreen
import com.akshatdjain.ultron.ui.theme.UltronTheme

private enum class Screen { Home, Settings, Logs }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UltronTheme {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(applicationContext))
                val state by viewModel.uiState.collectAsState()
                val pickerVisible by viewModel.pickerVisible.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()
                val discoveredDevices by viewModel.discoveredDevices.collectAsState()

                var pendingBleAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    if (permissions.all { it.value }) {
                        pendingBleAction?.invoke()
                    }
                    pendingBleAction = null
                }

                fun requestBlePermissions(action: () -> Unit) {
                    pendingBleAction = action
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    permissionLauncher.launch(permissions)
                }

                LaunchedEffect(Unit) {
                    val savedDevice = DeviceRepository(applicationContext).getSavedDeviceAddress()
                    if (savedDevice != null) {
                        requestBlePermissions { viewModel.reconnectToSavedDevice() }
                    }
                }

                var screen by remember { mutableStateOf(Screen.Home) }

                Crossfade(targetState = screen, label = "screenCrossfade") { current ->
                    when (current) {
                        Screen.Settings -> SettingsScreen(
                            state = state,
                            onBack = { screen = Screen.Home },
                            onWelcomeToggle = viewModel::onWelcomeToggle,
                            onWelcomeModeSelect = viewModel::onWelcomeModeSelect,
                            onWelcomeColorSelect = viewModel::onWelcomeColorSelect,
                            onWelcomeColorSync = viewModel::onWelcomeColorSync,
                            onViewLogs = { screen = Screen.Logs }
                        )
                        Screen.Logs -> LogsScreen(onBack = { screen = Screen.Settings })
                        Screen.Home -> HomeScreen(
                            state = state,
                            onColorPick = viewModel::onColorPick,
                            onModeSelect = viewModel::onModeSelect,
                            onBrightnessChange = viewModel::onBrightnessChange,
                            onPowerToggle = viewModel::onPowerToggle,
                            onConnectClick = { requestBlePermissions { viewModel.onConnectClick() } },
                            onSettingsClick = { screen = Screen.Settings },
                            pickerVisible = pickerVisible,
                            isScanning = isScanning,
                            discoveredDevices = discoveredDevices,
                            onDeviceSelected = viewModel::onDeviceSelected,
                            onDismissPicker = viewModel::onDismissPicker
                        )
                    }
                }
            }
        }
    }
}
