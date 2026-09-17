# Android Auto Integration Notes

This stage adds Android Auto support via the Car App Library (androidx.car.app:app). The integration allows users to control the ambient light strip directly from the car's display.

## Files Created

- **app/src/main/java/com/akshatdjain/ultron/car/UltronCarAppService.kt** - CarAppService entry point with permissive host validation (debug/sideload appropriate)
- **app/src/main/java/com/akshatdjain/ultron/car/UltronSession.kt** - Session that creates MainCarScreen
- **app/src/main/java/com/akshatdjain/ultron/car/MainCarScreen.kt** - GridTemplate-based UI with:
  - Color presets: Red, Orange, Yellow, Green, Blue, Purple
  - Animation modes: 3 Water + 3 Extended modes
  - ActionStrip: Power On/Off, Brightness +/-
  - Connection state handling: shows MessageTemplate when not Ready

## Gradle Dependency

The required dependency is **already present** in `app/build.gradle.kts`:
```
implementation("androidx.car.app:app:1.7.0")
```

No additional gradle changes needed.

## AndroidManifest.xml Changes

Add the following service declaration **inside the `<application>` block** (after the MainActivity):

```xml
<service
    android:name=".car.UltronCarAppService"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.car.app.CarAppService" />
    </intent-filter>
    <meta-data
        android:name="androidx.car.app.minCarApiLevel"
        android:value="1" />
</service>
```

Also add this meta-data **inside the `<application>` block** (after the service):

```xml
<meta-data
    android:name="com.google.android.gms.car.APPLICATION"
    android:resource="@xml/automotive_app_desc" />
```

## Resource File: automotive_app_desc.xml

Create a new file: `app/src/main/res/xml/automotive_app_desc.xml` with this content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="control" />
</automotiveApp>
```

This descriptor declares that Ultron is an IoT/device-control app (not navigation).

## UI Templates

- **Not Connected / Disconnected**: MessageTemplate showing "Not Connected"
- **Connecting**: MessageTemplate showing connection progress
- **Ready**: GridTemplate with two lists (colors + modes) + ActionStrip with power and brightness controls

The GridTemplate layout respects Car App Library constraints — no arbitrary custom drawing, only composed template elements.

## Integration Points

- **LightBleManager singleton**: Accessed via `getInstance(context)` to send commands and observe `connectionState`
- **Commands**: All commands routed through `LightBleManager.sendCommand(hexString)`
- **Connection state**: Observed from `bleManager.connectionState.value` to switch templates

## Next Steps (Future Stages)

- Add device discovery flow (pair device via phone UI before Android Auto)
- Persist selected device via DeviceRepository
- Add voice integration for voice commands on car screen
- Enhance ActionStrip with additional actions if Car App Library updates support more
