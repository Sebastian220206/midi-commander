# MIDI Commander Pro

A premium Android MIDI controller app built with Jetpack Compose and the Android USB MIDI API.

---

## Features

| Category | Details |
|---|---|
| **MIDI Messages** | Program Change, Control Change, Note On/Off, SysEx |
| **USB MIDI** | Auto-detection of class-compliant devices, permission flow, multi-port support |
| **Controls** | 4×4 button grid, drawbars (CC sliders), rotary knobs |
| **Banks** | 8 banks (A–H) with independent control layouts |
| **Edit Mode** | Long-press any control to open the config dialog |
| **Presets** | Save, load, delete named presets; JSON export/import |
| **MIDI Monitor** | Real-time message log with channel/type filtering |
| **MIDI Learn** | Long-press → MIDI Learn → touch a knob on your synth to auto-assign |
| **Global Settings** | Channel override, cable/port selection, MIDI Thru, SysEx delay, haptics, dark/light theme |
| **SysEx Files** | Load a `.syx` file from storage and send it with configurable inter-message delay |
| **Error Handling** | USB host check, permission dialog, retry on alternate port, graceful disconnect |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| MIDI | `android.media.midi` + `android.hardware.usb` |
| Persistence | Room DB (controls, banks, presets) + DataStore (settings) |
| DI | Hilt |
| Serialization | Gson |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 34 |

---

## Project Structure

```
app/src/main/java/com/midicommander/app/
├── MainActivity.kt              # Entry point, USB intent handling, navigation
├── MidiCommanderApp.kt          # Hilt Application class
├── midi/
│   ├── MidiEngine.kt            # Core MIDI send/receive, SysEx, Thru, retry
│   ├── UsbMidiDeviceManager.kt  # USB host detection, permission, auto-connect
│   └── MidiInputParser.kt       # Parse raw MIDI bytes → IncomingMidiEvent (for MIDI Learn)
├── data/
│   ├── model/Models.kt          # ControlConfig, Bank, Preset, GlobalSettings, MidiLogEntry
│   ├── db/MidiDatabase.kt       # Room database + DAOs
│   ├── SettingsDataStore.kt     # DataStore wrapper for GlobalSettings
│   └── repository/MidiRepository.kt
├── di/
│   └── AppModule.kt             # Hilt module (DB, DAOs, Gson)
├── viewmodel/
│   └── MainViewModel.kt         # All state, MIDI send, preset, MIDI Learn, haptic
└── ui/
    ├── theme/Theme.kt           # Midnight Pro dark theme, color palette
    ├── screens/
    │   ├── MainScreen.kt        # Grid, drawbars, bank selector, monitor, header
    │   └── SettingsScreen.kt    # Global settings, preset manager, SysEx file sender
    └── components/
        ├── MidiButton.kt        # Animated button with glow border
        ├── MidiDrawbar.kt       # Vertical CC slider with drag gesture
        ├── MidiKnob.kt          # Rotary knob with arc + sweep gradient
        ├── BankSelector.kt      # A–H bank strip
        ├── ControlConfigDialog.kt  # Full per-control config dialog
        ├── DevicePickerDialog.kt   # USB device + port selection
        └── MidiMonitorPanel.kt  # Real-time log panel with filters
```

---

## Setup & Build

### Requirements
- Android Studio Jellyfish (2023.3) or later
- JDK 17
- Android device with **USB Host / OTG** support
- USB MIDI class-compliant device (keyboard, interface, etc.)

### Steps
```bash
# 1. Open project in Android Studio
#    File → Open → select the root "New folder" directory

# 2. Sync Gradle
#    Android Studio will prompt; click "Sync Now"

# 3. Connect Android device in Developer Mode
#    Enable USB Debugging in device settings

# 4. Run
#    Run → Run 'app'  (or Shift+F10)
```

> **First launch:** plug in your MIDI device *before* or *after* launching the app.
> The USB permission dialog will appear. Tap **OK** to allow access.

---

## Using the App

### Main Screen
- **Grid buttons** – each triggers a MIDI message (Program Change by default).
- **Bank strip (A–H)** – switch between 8 independent control layouts.
- **USB icon** (top-right) – open the device picker if multiple MIDI devices are connected.
- **EDIT toggle** – enable Edit Mode. Long-press any control to configure it.
- **Analytics icon** – toggle the MIDI Monitor panel.
- **⚙ icon** – navigate to Settings.

### Configuring a Control
In **Edit Mode**, long-press any button/slider/knob:
- Set **Label**, **MIDI Channel** (1–16), **Message Type**, **Value**, **Color**
- Switch between **Button / Slider / Knob** control type
- Set **Port/Cable** for multi-port MIDI interfaces
- Enable **Toggle Mode** (momentary vs. latching)
- Tap **MIDI Learn** — then move a knob on your MIDI device to auto-assign

### Drawbars & Knobs
To add sliders or knobs to the sidebar, configure a button in Edit Mode and change its **Control Type** to `SLIDER` or `KNOB`. It will move from the grid to the sidebar automatically.

### Presets
In Settings → Presets:
- Type a name and tap **Save** to save the current layout
- Tap **Load** next to any preset to restore it
- **Export JSON** / **Import JSON** for backup or sharing

---

## Error Handling

| Scenario | Behavior |
|---|---|
| No USB Host support | Toast shown on launch with advisory message |
| USB permission denied | OS permission dialog; denied → warning log |
| No MIDI input ports on device | Error state shown in header |
| MIDI send failure | Retries on next cable port; logs to monitor |
| Device disconnected | Header shows "No Device Connected"; engine cleans up gracefully |

---

## Roadmap / Nice-to-Have

- [ ] Bluetooth MIDI (BLE MIDI protocol)
- [ ] Configurable grid size (e.g. 2×4, 4×6, 8×8)
- [ ] Per-bank MIDI channel defaults
- [ ] Animated LED-style value indicators
- [ ] Tablet landscape two-panel layout
- [ ] SysEx template library
