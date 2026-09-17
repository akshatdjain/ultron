# Ultron Voice Control Integration

## Overview
This stage adds Google Assistant "App Actions" voice control to Ultron, enabling commands like "Hey Google, turn on ambient light" and "Hey Google, set ambient light to blue". The implementation consists of:

1. **shortcuts.xml** — Google App Actions capabilities definition
2. **VoiceActionActivity.kt** — Lightweight trigger Activity that handles voice commands
3. **AndroidManifest.xml entries** — Registers the activity and metadata

---

## AndroidManifest Configuration

### Activity Entry
Add the following `<activity>` block inside the `<application>` element in `AndroidManifest.xml`:

```xml
<activity
    android:name=".voice.VoiceActionActivity"
    android:exported="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar"
    android:noHistory="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.VOICE" />
        <category android:name="android.intent.category.BROWSABLE" />
    </intent-filter>
</activity>
```

**Key attributes:**
- `android:exported="true"` — Required for Google Assistant to invoke the activity
- `android:theme="@android:style/Theme.Translucent.NoTitleBar"` — Makes the activity invisible to the user
- `android:noHistory="true"` — Prevents the activity from appearing in back stack
- `android:name=".voice.VoiceActionActivity"` — Must match the activity class location

### Meta-data Reference to shortcuts.xml
Add the following `<meta-data>` block inside the `<application>` element (after the `<activity>` block):

```xml
<meta-data
    android:name="com.google.android.actions"
    android:resource="@xml/shortcuts" />
```

This tells Android that your app supports Google App Actions and defines them in `res/xml/shortcuts.xml`.

---

## File Locations

- **shortcuts.xml:** `app/src/main/res/xml/shortcuts.xml`
- **VoiceActionActivity:** `app/src/main/java/com/akshatdjain/ultron/voice/VoiceActionActivity.kt`
- **AndroidManifest.xml:** `app/src/main/AndroidManifest.xml` (modify existing file)

---

## Supported Voice Commands

### Power Control
- "Hey Google, turn on ambient light" → Sends `POWER_ON_2WAY` command
- "Hey Google, turn off ambient light" → Sends `POWER_OFF_2WAY` command

### Color Control
- "Hey Google, set ambient light to red" → Sends `ADJUST_ALL_ZONES + FF0000`
- "Hey Google, set ambient light to green" → Sends `ADJUST_ALL_ZONES + 00FF00`
- "Hey Google, set ambient light to blue" → Sends `ADJUST_ALL_ZONES + 0000FF`
- "Hey Google, set ambient light to white" → Sends `ADJUST_ALL_ZONES + FFFFFF`
- "Hey Google, set ambient light to orange" → Sends `ADJUST_ALL_ZONES + FFA500`
- "Hey Google, set ambient light to purple" → Sends `ADJUST_ALL_ZONES + 800080`
- "Hey Google, set ambient light to pink" → Sends `ADJUST_ALL_ZONES + FFC0CB`

### Brightness Control
- "Hey Google, set ambient light brightness to 50 percent" → Sends `BRIGHTNESS + 32` (hex for 50)

---

## Implementation Details

### VoiceActionActivity Behavior
1. Receives intent from Google Assistant containing:
   - `action` — One of TURN_ON, TURN_OFF, SET_COLOR, SET_BRIGHTNESS
   - `android.intent.extra.DEVICE_NAME` — Always present (device name)
   - `actions.fulfillment.fulfillment_intent.color` — Present for SET_COLOR (e.g., "red", "blue")
   - `actions.fulfillment.fulfillment_intent.brightness` — Present for SET_BRIGHTNESS (e.g., "50", "75%")

2. Maps color names to RGB hex values using the internal `colorMap`

3. Converts brightness percentage (0-100) to hex format expected by LightProtocol.BRIGHTNESS command

4. Checks if BLE manager is connected:
   - **If already Ready:** Sends command immediately
   - **If not Ready:** 
     - Retrieves last-saved device address from DeviceRepository (SharedPreferences)
     - Gets BluetoothDevice via BluetoothAdapter
     - Calls `LightBleManager.getInstance(context).connect(device)`
     - Waits up to 3 seconds for connectionState to transition to Ready
     - If connected, sends command; otherwise shows error Toast

5. Shows a short Toast message confirming success or error

6. Finishes the activity immediately (invisible to user)

### Why Invisible/Translucent?
The activity uses `Theme.Translucent.NoTitleBar` and finishes immediately after sending the command. This ensures:
- The user never sees a UI (they only hear the verbal response from Google Assistant)
- The activity does not interfere with the phone's current screen/app
- Android Auto can work with this without visual disruption

### Connection Handling
The 3-second timeout and 100ms polling loop allow sufficient time for the BLE state machine to:
1. Discover GATT services
2. Enable notifications
3. Request MTU 255
4. Transition to Ready state (includes internal 1000ms delay before sending handshake)

If the device does not become Ready within 3 seconds, the user receives a "Ambient light not connected" Toast message, signaling either a BLE range/link issue or that the device is offline.

---

## Testing on Debug Build

You can test voice control without any Play Store review:

1. Install the debug APK on a device with Google Assistant enabled
2. Say "Hey Google, turn on ambient light" (or any supported command)
3. Google Assistant will route the command to VoiceActionActivity
4. If the device BLE connection succeeds, the light will respond
5. A Toast message confirms success or failure

---

## Integration with Other Stages

- **BLE Layer:** Uses `LightBleManager.getInstance(context)` (singleton shared with UI and Android Auto)
- **Data Layer:** Uses `DeviceRepository(context)` to retrieve the last-paired device address
- **No UI Dependency:** This stage does not modify or depend on Compose UI or Android Auto files
- **No Permissions Added:** Uses existing Bluetooth permissions from the base scaffold

---

## Known Limitations

1. **No app-level confirmation dialog** — The voice action completes silently if BLE is ready; Google Assistant provides the spoken confirmation
2. **3-second connection timeout** — If the device is far away or unresponsive, the command will fail with a Toast
3. **No command queuing** — If multiple voice commands arrive during connection setup, only the final one is guaranteed to be sent
4. **Requires shared device** — VoiceActionActivity must find the saved device address; pairing must be done via the main UI first (handled by future UI stage)
