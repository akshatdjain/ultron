# Ultron BLE Controller - Project Scaffold Notes

## Overview
This is the foundational scaffold for Ultron, a native Android BLE controller for a car ambient-light strip. This stage covers the core BLE/data layer only; UI, Android Auto, and voice features will be added by subsequent stages.

## Dependency Versions

### Gradle & Build
- Gradle: 8.14
- Android Gradle Plugin: 8.7.0
- Kotlin: 2.0.0
- Kotlin Compose Plugin: 2.0.0
- Kotlin Compose Compiler: 2.0.0

### Android Framework
- compileSdk: 35
- minSdk: 26
- targetSdk: 35
- androidx.core:core-ktx: 1.13.1
- androidx.lifecycle:lifecycle-runtime-ktx: 2.8.0
- androidx.lifecycle:lifecycle-viewmodel-compose: 2.8.0
- androidx.activity:activity-compose: 1.9.0
- androidx.compose.bom: 2024.09.00 (BOM for Compose)
- androidx.compose.material3:material3: (via BOM)
- androidx.compose.ui:ui: (via BOM)
- androidx.compose.ui:ui-tooling-preview: (via BOM)
- androidx.car.app:app: 1.7.0
- org.jetbrains.kotlinx:kotlinx-coroutines-android: 1.8.0

### Testing
- junit:junit: 4.13.2
- androidx.test.ext:junit: 1.1.5
- androidx.test.espresso:espresso-core: 3.5.1
- androidx.compose.ui:ui-test-junit4: (via BOM)

## Package Structure

```
com/akshatdjain/ultron/
├── ble/
│   ├── LightProtocol.kt
│   └── LightBleManager.kt
├── data/
│   ├── LightState.kt
│   └── DeviceRepository.kt
└── MainActivity.kt
```

## Core API Reference

### ble/LightProtocol.kt

**Object: `LightProtocol`**

#### UUIDs
```kotlin
val SERVICE_UUID: UUID = "0000FFE0-0000-1000-8000-00805f9b34fb"
val WRITE_CHAR_UUID: UUID = "0000FFE1-0000-1000-8000-00805f9b34fb"
val NOTIFY_CHAR_UUID: UUID = "0000FFE2-0000-1000-8000-00805f9b34fb"
val CCCD_UUID: UUID = "00002902-0000-1000-8000-00805f9b34fb"
```

#### Public Functions
```kotlin
fun frameCommand(hexString: String): ByteArray
// Wraps a hex command string in ASCII "<" ">" framing
// Example: frameCommand("0800") -> [0x3C, 0x30, 0x38, 0x30, 0x30, 0x3E]

fun splitFrames(data: ByteArray): List<String>
// Splits a raw notification ByteArray stream into individual frame hex strings
// Handles concatenated frames by splitting on "><" boundaries

fun decodeFrame(frameString: String): ByteArray?
// Decodes a hex string frame into its byte representation
// Returns null if the hex string is malformed
```

#### Nested Objects: Commands
Command constants for constructing hex command strings:
```kotlin
object Commands {
    const val STATIC_RED = "0800"
    const val STATIC_ORANGE = "0801"
    const val STATIC_YELLOW = "0802"
    const val STATIC_GREEN = "0803"
    const val STATIC_BLUE = "0804"
    const val STATIC_PURPLE = "0806"
    
    const val WATER_MODE_BASE = "04"  // + 2 hex digits (00-14)
    const val EXTENDED_MODE_BASE = "1E"  // + 2 hex digits (00-1E)
    
    const val CUSTOM_ZONES_OFF = "2B00"
    const val CUSTOM_ZONES_ON = "2B01"
    const val SET_CUSTOM_ZONES = "2A"  // + 8 hex nibbles
    const val RESET_CUSTOM_ZONES = "2AFFFFFFFF"
    
    const val ADJUST_ALL_ZONES = "06"  // + 6 hex digits RRGGBB
    const val ADJUST_ZONE_1 = "10"
    const val ADJUST_ZONE_2 = "09"
    
    const val BRIGHTNESS = "3E"  // + 2 hex digits (00-64)
    
    const val POWER_TOGGLE_3WAY = "0700"
    const val POWER_ON_2WAY = "0701"
    const val POWER_OFF_2WAY = "0702"
    const val FEATURE_ON = "1401"
    const val FEATURE_OFF = "1400"
    
    const val VOICE_LEVEL = "0300"  // + 2 hex digits
    
    const val FACTORY_RESET = "4200"
    
    const val SYMPHONY_MODE_OFF = "3B00"
    const val SYMPHONY_MODE_ON = "3B01"
    const val SYMPHONY_RESET = "3AFFFFFFFF"
    const val SYMPHONY_WATER_BASE = "29"
    const val SYMPHONY_SPEED = "0B"
    const val SYMPHONY_BRIGHTNESS_1 = "28"
    const val SYMPHONY_BRIGHTNESS_2 = "3C"
    const val SYMPHONY_BRIGHTNESS_3 = "3D"
    
    const val HANDSHAKE = "0A01"
    const val ALERT_BASE = "30"  // + digit (0-9) + "00"/"01"
}
```

#### Nested Objects: ResponseTypes
Response frame type constants (first byte of decoded frame):
```kotlin
object ResponseTypes {
    const val HANDSHAKE_ACK = 0x0A
    const val STATUS_FRAME_1 = 0x12
    const val STATUS_FRAME_2 = 0x1F
    const val STATUS_FRAME_3 = 0x4E
    const val VERSION_INFO = 0x13
    const val CUSTOM_COLOR_ECHO = 0x27
    const val ERROR_ALERT = 0x2E
}
```

#### Nested Objects: StatusFrameLayout
Byte indices for status frame parsing (0-indexed into decoded ByteArray):
```kotlin
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
```

---

### ble/LightBleManager.kt

**Enum: `ConnectionState`**
```kotlin
enum class ConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Ready
}
```

**Class: `LightBleManager`**

Constructor:
```kotlin
constructor(context: Context)
```

Public Properties:
```kotlin
val connectionState: StateFlow<ConnectionState>
// Emits connection state changes: Disconnected -> Connecting -> Connected -> Ready
// Use .collect() or .stateIn() to observe

val responseFrames: SharedFlow<ByteArray>
// Emits decoded response frames (raw ByteArray per frame)
// One emission per frame received from device
```

Public Methods:
```kotlin
fun connect(device: BluetoothDevice)
// Initiates GATT connection to the device
// Sets connectionState to Connecting, then Connected when established
// Automatically discovers services, enables notifications, and transitions to Ready

fun disconnect()
// Closes GATT connection and sets connectionState to Disconnected

fun sendCommand(hexString: String)
// Sends a hex command string to the device
// Automatically frames it with "<" ">" and writes to WRITE_CHAR_UUID
// Write type is WRITE_TYPE_NO_RESPONSE

fun scan(onDeviceFound: (BluetoothDevice) -> Unit)
// Scans for BLE devices advertising SERVICE_UUID
// Invokes callback for each device found
```

Companion Object:
```kotlin
companion object {
    fun getInstance(context: Context): LightBleManager
    // Singleton accessor
    // Always returns the same instance for a given application context
    // Safe for phone UI, Android Auto screens, and voice-trigger activities to share
}
```

**Connection Flow:**
1. Call `getInstance(context)` to get singleton
2. Call `connect(device)` with a BluetoothDevice
3. Observe `connectionState.collect()` - transitions Disconnected → Connecting → Connected → Ready
4. Once Ready, observe `responseFrames.collect()` for incoming frames
5. Use `sendCommand()` to send commands
6. Call `disconnect()` to close connection

---

### data/LightState.kt

**Data Class: `LightState`**

Constructor (all fields optional with defaults):
```kotlin
data class LightState(
    val power: Boolean = false,
    val mode: Int = 0,
    val colorPrimary: Int = Color.WHITE,      // Android Color Int (0xAARRGGBB)
    val colorSecondary: Int = Color.BLACK,    // Android Color Int
    val brightness: Int = 50,                  // 0-100
    val speed: Int = 0,                        // Device-specific scale
    val voiceLevel: Int = 0,                   // Device-specific scale
    val connectionState: ConnectionState = ConnectionState.Disconnected
)
```

**Object: `LightStateParser`**

Public Methods:
```kotlin
fun parseStatusFrame(frameBytes: ByteArray): LightState?
// Parses a decoded response frame ByteArray into a LightState object
// Accepts frame types 0x12, 0x1F, 0x4E (status frames)
// Returns null if frame is invalid or unrecognized
// Extracts: mode, colors (RGB), brightness, speed, voiceLevel
// Sets connectionState to Ready and power to true (only for status frames)
```

**Usage Example:**
```kotlin
// When receiving a response frame via responseFrames.collect():
val frame = byteArrayOf(...) // from responseFrames
val state = LightStateParser.parseStatusFrame(frame)
if (state != null) {
    // Update UI with state.brightness, state.colorPrimary, etc.
}
```

---

### data/DeviceRepository.kt

**Class: `DeviceRepository`**

Constructor:
```kotlin
constructor(context: Context)
// Uses context.getSharedPreferences("ultron_prefs", Context.MODE_PRIVATE)
```

Public Methods:
```kotlin
fun saveDeviceAddress(address: String)
// Persists device MAC address to SharedPreferences
// Key: "last_paired_device_address"

fun getSavedDeviceAddress(): String?
// Retrieves last-paired device MAC address from SharedPreferences
// Returns null if no device has been saved yet
```

**Usage Example:**
```kotlin
val repo = DeviceRepository(context)
repo.saveDeviceAddress("AA:BB:CC:DD:EE:FF")
val savedAddr = repo.getSavedDeviceAddress()  // "AA:BB:CC:DD:EE:FF"
```

---

## BLE Protocol Reference (Implemented)

**Verified against real device capture.** Implement exactly as specified.

### Transport
- GATT service UUID: 0000FFE0-0000-1000-8000-00805f9b34fb
- Write characteristic (no-response write): 0000FFE1-0000-1000-8000-00805f9b34fb
- Notify characteristic: 0000FFE2-0000-1000-8000-00805f9b34fb
- CCCD UUID: 00002902-0000-1000-8000-00805f9b34fb
- **NO encryption, NO authentication**

### Framing
Every command is ASCII text wrapped in angle brackets:
```
frame_bytes = 0x3C ('<') + ASCII_BYTES(hexCommandString) + 0x3E ('>')
```
Example: hex "0800" → bytes [0x3C, 0x30, 0x38, 0x30, 0x30, 0x3E] (the ASCII string "<0800>")

### Connection Handshake (Automatic in LightBleManager)
1. BLE connect → GATT discover services
2. Get FFE1 and FFE2 characteristics from FFE0 service
3. Enable notifications on FFE2 (write CCCD)
4. Request MTU 255
5. Wait ~1000ms
6. Send "0A01" handshake command
7. Ready for other commands

---

## Known Limitations & Notes

1. **No encryption/authentication** - This is verified fact against the real device; it is not implemented by design.
2. **Frame parsing assumes valid UTF-8 ASCII** - Invalid hex in frames will be silently dropped by `decodeFrame()`.
3. **Singleton LightBleManager** - Multiple calls to `getInstance()` return the same instance. For multi-connection scenarios (unlikely for this use case), new instances would need explicit construction.
4. **No auto-reconnect** - Disconnections are not automatically retried; client must call `connect()` again.
5. **No command queue** - Commands sent while not Ready will be dropped. Client must wait for `connectionState == Ready`.

---

## Next Steps (For Future Stages)

- **UI Stage**: Build Compose screens using `LightBleManager.getInstance()` and `LightStateParser`
- **Android Auto Stage**: Access BLE manager via same singleton; post states to car screens
- **Voice Stage**: Trigger commands from voice handler, use `LightBleManager.sendCommand()` with parsed intents
- **Settings Stage**: Build device discovery flow, persist device choice via `DeviceRepository`

---

## Testing the Scaffold

```bash
# From project root
cd C:\Users\aksha\Desktop\ambient\ultron

# Verify gradle structure
gradlew tasks

# (Full build not expected to succeed without Android SDK, but project structure is valid)
```

---

## Project Structure

```
C:\Users\aksha\Desktop\ambient\ultron\
├── .gitignore
├── SCAFFOLD_NOTES.md (this file)
├── settings.gradle.kts
├── build.gradle.kts (root)
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/akshatdjain/ultron/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ble/
│       │   │   │   ├── LightProtocol.kt
│       │   │   │   └── LightBleManager.kt
│       │   │   └── data/
│       │   │       ├── LightState.kt
│       │   │       └── DeviceRepository.kt
│       │   └── res/
│       │       └── values/
│       │           ├── strings.xml
│       │           └── themes.xml
```

---

## API Stability Commitment

The public method signatures and class names documented above are the stable foundation that subsequent stages depend on. They follow the exact protocol specification verified against a real device and will not change.
