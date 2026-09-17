# Ultron Phone UI — Notes for Integration Stage

## Files added

```
ui/theme/Color.kt, Theme.kt, Type.kt   — custom dark Material 3 theme (UltronTheme)
ui/components/ColorWheel.kt            — HSV color wheel (hue/sat drag control)
ui/components/BrightnessSlider.kt      — animated brightness slider with % readout
ui/components/ConnectionStatusBar.kt   — connect/scanning/connected status pill
ui/components/LightModes.kt            — LightMode data + LIGHT_MODES list (invented names -> hex commands)
ui/components/ModeGrid.kt              — Static/Flow/Extended mode card grid
ui/HomeScreen.kt                       — stateless screen composable (no ViewModel/BLE inside)
ui/HomeViewModel.kt                    — ViewModel + HomeViewModelFactory, owns LightBleManager
```

## HomeScreen — pure stateless composable

```kotlin
@Composable
fun HomeScreen(
    state: LightState,
    onColorPick: (Int) -> Unit,        // android Color Int (RGB), alpha ignored by BLE layer
    onModeSelect: (String) -> Unit,    // full hex command string, e.g. "0800", "0403", "1E0B"
    onBrightnessChange: (Int) -> Unit, // 0-100
    onPowerToggle: () -> Unit,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

Wrap it in `UltronTheme { HomeScreen(...) }`. It takes no ViewModel/context and does no BLE work itself, so it previews and tests with a plain `LightState()`.

## HomeViewModel — owns the BLE singleton

```kotlin
class HomeViewModel(context: Context) : ViewModel() {
    val uiState: StateFlow<LightState>
    fun onConnectClick()
    fun onPowerToggle()
    fun onColorPick(colorInt: Int)
    fun onBrightnessChange(brightness: Int)
    fun onModeSelect(command: String)
}

class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory
```

`HomeViewModel` calls `LightBleManager.getInstance(context)` in its constructor, collects `connectionState` and `responseFrames` (parsed via `LightStateParser.parseStatusFrame`) into `uiState`, and translates each UI callback into the matching `sendCommand(hex)` call:

- `onColorPick(colorInt)` → `"06" + "%06X".format(colorInt and 0xFFFFFF)` (ADJUST_ALL_ZONES)
- `onBrightnessChange(brightness)` → `"3E" + "%02X".format(brightness.coerceIn(0,100))` (BRIGHTNESS)
- `onModeSelect(command)` → sent as-is; `ModeGrid`/`LIGHT_MODES` already built the full hex command (base + index, or the static-color 4-digit code)
- `onPowerToggle()` → `POWER_ON_2WAY` ("0701") or `POWER_OFF_2WAY` ("0702") depending on current `state.power`
- `onConnectClick()` → `bleManager.scan { device -> deviceRepository.saveDeviceAddress(device.address); bleManager.connect(device) }`

## Suggested MainActivity wiring (not done here — integration stage)

```kotlin
setContent {
    UltronTheme {
        val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(applicationContext))
        val state by viewModel.uiState.collectAsState()
        HomeScreen(
            state = state,
            onColorPick = viewModel::onColorPick,
            onModeSelect = viewModel::onModeSelect,
            onBrightnessChange = viewModel::onBrightnessChange,
            onPowerToggle = viewModel::onPowerToggle,
            onConnectClick = viewModel::onConnectClick
        )
    }
}
```

Runtime BLE permissions (BLUETOOTH_SCAN/CONNECT on API 31+, location on older) are not requested anywhere in the UI layer — that still needs to happen before `onConnectClick` will do anything on a real device.

## Known pre-existing issue outside this stage's scope

`ble/LightBleManager.kt` (built by the earlier BLE/data-layer stage) fails to compile as-is: `scan()` passes a `java.util.UUID` to `ScanFilter.Builder.setServiceUuid`, which requires a `ParcelUuid` (`ParcelUuid(LightProtocol.SERVICE_UUID)`). Left untouched since it's outside this stage's scope, but the module won't build until it's fixed.
