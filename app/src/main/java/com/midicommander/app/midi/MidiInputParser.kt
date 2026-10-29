package com.midicommander.app.midi

import com.midicommander.app.data.model.MidiLogEntry
import com.midicommander.app.data.model.MidiMessageType
import com.midicommander.app.data.model.LogDirection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parsed incoming MIDI event emitted when MIDI Learn mode is active.
 */
data class IncomingMidiEvent(
    val messageType: MidiMessageType,
    val channel: Int,       // 1–16
    val value1: Int,        // CC number, note, program
    val value2: Int = 0     // CC value, velocity
)

/**
 * Parses raw MIDI bytes from the device's output port and emits structured events.
 * Used by MIDI Learn to auto-assign controls.
 */
@Singleton
class MidiInputParser @Inject constructor() {

    private val _events = MutableSharedFlow<IncomingMidiEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<IncomingMidiEvent> = _events.asSharedFlow()

    fun parse(msg: ByteArray, offset: Int, count: Int) {
        if (count < 1) return
        val status = msg[offset].toInt() and 0xFF
        val type = status and 0xF0
        val ch = (status and 0x0F) + 1  // 1-based

        val event: IncomingMidiEvent? = when (type) {
            0x80 -> IncomingMidiEvent(
                MidiMessageType.NOTE_OFF, ch,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F,
                msg.getOrElse(offset + 2) { 0 }.toInt() and 0x7F
            )
            0x90 -> {
                val vel = msg.getOrElse(offset + 2) { 0 }.toInt() and 0x7F
                IncomingMidiEvent(
                    if (vel > 0) MidiMessageType.NOTE_ON else MidiMessageType.NOTE_OFF,
                    ch,
                    msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F,
                    vel
                )
            }
            0xB0 -> IncomingMidiEvent(
                MidiMessageType.CONTROL_CHANGE, ch,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F,
                msg.getOrElse(offset + 2) { 0 }.toInt() and 0x7F
            )
            0xC0 -> IncomingMidiEvent(
                MidiMessageType.PROGRAM_CHANGE, ch,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F
            )
            else -> null
        }
        event?.let { _events.tryEmit(it) }
    }
}
