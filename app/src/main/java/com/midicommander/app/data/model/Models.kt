package com.midicommander.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── MIDI Message Types ───────────────────────────────────────────────────────

enum class MidiMessageType(val label: String) {
    PROGRAM_CHANGE("PC"),
    CONTROL_CHANGE("CC"),
    NOTE_ON("Note On"),
    NOTE_OFF("Note Off"),
    SYSEX("SysEx")
}

// ─── Control Types ────────────────────────────────────────────────────────────

enum class ControlType {
    BUTTON, SLIDER, KNOB
}

// ─── Control Config ───────────────────────────────────────────────────────────

/**
 * Represents a single configurable MIDI control (button, slider, or knob).
 */
@Entity(tableName = "controls")
data class ControlConfig(
    @PrimaryKey val id: String,          // "{bankId}_{row}_{col}"
    val bankId: String,
    val row: Int,
    val col: Int,
    val controlType: ControlType = ControlType.BUTTON,
    val label: String = "—",
    val midiChannel: Int = 1,            // 1–16
    val messageType: MidiMessageType = MidiMessageType.PROGRAM_CHANGE,
    val value: Int = 0,                  // 0–127
    val secondaryValue: Int = 0,         // For Note Off velocity, etc.
    val colorHex: String = "#7C4DFF",    // Default accent purple
    val sysExData: String = "",          // Hex string for SysEx
    val isToggle: Boolean = false,       // Toggle vs momentary for buttons
    val isToggled: Boolean = false,      // Current toggle state
    val cableId: Int = 0                 // Port/cable index for multi-port
)

// ─── Bank ────────────────────────────────────────────────────────────────────

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey val id: String,
    val label: String,       // "A", "B", "C", "D" …
    val index: Int,
    val gridRows: Int = 4,
    val gridCols: Int = 4
)

// ─── Preset ───────────────────────────────────────────────────────────────────

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val controlsJson: String = "[]"      // Serialized list of ControlConfig
)

// ─── MIDI Log Entry ──────────────────────────────────────────────────────────

data class MidiLogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val direction: LogDirection,
    val channel: Int,
    val messageType: MidiMessageType,
    val value1: Int,
    val value2: Int = 0,
    val rawBytes: String = ""
)

enum class LogDirection { OUT, IN }

// ─── Global Settings (DataStore) ─────────────────────────────────────────────

data class GlobalSettings(
    val globalMidiChannel: Int = 0,       // 0 = per-control, 1–16 = override
    val defaultCableId: Int = 0,
    val midiThru: Boolean = false,
    val useNativeMidi: Boolean = true,    // Android 6+ native MIDI driver
    val sysExDelay: Long = 20L,           // ms between SysEx messages
    val hapticFeedback: Boolean = true,
    val darkTheme: Boolean = true,
    val midiLearnEnabled: Boolean = false
)
