package com.midicommander.app.midi

import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.*
import android.util.Log
import com.midicommander.app.data.model.ControlConfig
import com.midicommander.app.data.model.MidiLogEntry
import com.midicommander.app.data.model.MidiMessageType
import com.midicommander.app.data.model.LogDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

// ─── Connection State ─────────────────────────────────────────────────────────

sealed class MidiConnectionState {
    object Disconnected : MidiConnectionState()
    object Connecting : MidiConnectionState()
    data class Connected(val deviceName: String, val portCount: Int) : MidiConnectionState()
    data class Error(val message: String) : MidiConnectionState()
}

// ─── MIDI Engine ──────────────────────────────────────────────────────────────

@Singleton
class MidiEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MidiEngine"
        private const val MAX_SYSEX_SIZE = 65536
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State
    private val _connectionState = MutableStateFlow<MidiConnectionState>(MidiConnectionState.Disconnected)
    val connectionState: StateFlow<MidiConnectionState> = _connectionState.asStateFlow()

    private val _midiLog = MutableSharedFlow<MidiLogEntry>(replay = 50)
    val midiLog: SharedFlow<MidiLogEntry> = _midiLog.asSharedFlow()

    // MIDI internals
    private var midiDevice: MidiDevice? = null
    private var inputPort: MidiInputPort? = null  // We send TO device via input port

    // MIDI Thru / Receive
    private var outputPort: MidiOutputPort? = null
    private var midiThruEnabled = false
    private var activeReceiver: MidiReceiver? = null

    // ─── USB Host Check ───────────────────────────────────────────────────────

    fun hasUsbHostSupport(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    // ─── Connection ───────────────────────────────────────────────────────────

    fun connect(device: MidiDevice, portIndex: Int = 0) {
        scope.launch {
            try {
                _connectionState.value = MidiConnectionState.Connecting
                midiDevice?.close()
                midiDevice = device
                val info = device.info
                val portCount = info.inputPortCount
                if (portCount == 0) {
                    _connectionState.value = MidiConnectionState.Error("No MIDI input ports available")
                    return@launch
                }
                val safePort = portIndex.coerceIn(0, portCount - 1)
                inputPort = device.openInputPort(safePort)
                val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
                    ?: info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
                    ?: "Unknown MIDI Device"
                _connectionState.value = MidiConnectionState.Connected(name, portCount)
                Log.d(TAG, "Connected to $name on port $safePort")

                // MIDI Thru
                if (midiThruEnabled && info.outputPortCount > 0) {
                    setupMidiThru()
                }
            } catch (e: Exception) {
                _connectionState.value = MidiConnectionState.Error(e.message ?: "Connection failed")
                Log.e(TAG, "Connection error", e)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                outputPort?.close()
                inputPort?.close()
                midiDevice?.close()
                outputPort = null
                inputPort = null
                midiDevice = null
                _connectionState.value = MidiConnectionState.Disconnected
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error", e)
            }
        }
    }

    fun setMidiThru(enabled: Boolean) {
        midiThruEnabled = enabled
        if (enabled && midiDevice != null) {
            scope.launch { setupMidiThru() }
        } else {
            outputPort?.close()
            outputPort = null
        }
    }

    private suspend fun setupMidiThru() {
        val device = midiDevice ?: return
        val inPort = inputPort ?: return
        outputPort?.close()
        outputPort = device.openOutputPort(0)
        outputPort?.connect(inPort)
    }

    // ─── MIDI Send ────────────────────────────────────────────────────────────

    fun sendControl(control: ControlConfig, globalChannelOverride: Int = 0) {
        val channel = if (globalChannelOverride in 1..16) globalChannelOverride - 1
                      else (control.midiChannel - 1).coerceIn(0, 15)

        scope.launch {
            when (control.messageType) {
                MidiMessageType.PROGRAM_CHANGE -> sendProgramChangeInternal(channel, control.value, control.cableId)
                MidiMessageType.CONTROL_CHANGE -> sendControlChangeInternal(channel, control.value, control.secondaryValue, control.cableId)
                MidiMessageType.NOTE_ON        -> sendNoteOnInternal(channel, control.value, control.secondaryValue.coerceAtLeast(64), control.cableId)
                MidiMessageType.NOTE_OFF       -> sendNoteOffInternal(channel, control.value, 0, control.cableId)
                MidiMessageType.SYSEX          -> sendSysEx(control.sysExData)
            }
        }
    }

    /** Thread-safe public API — dispatches to IO scope */
    fun sendControlChange(channel: Int, cc: Int, value: Int, cableId: Int = 0) {
        scope.launch { sendControlChangeInternal(channel, cc, value, cableId) }
    }

    fun sendProgramChange(channel: Int, program: Int, cableId: Int = 0) {
        scope.launch { sendProgramChangeInternal(channel, program, cableId) }
    }

    fun sendNoteOn(channel: Int, note: Int, velocity: Int, cableId: Int = 0) {
        scope.launch { sendNoteOnInternal(channel, note, velocity, cableId) }
    }

    fun sendNoteOff(channel: Int, note: Int, velocity: Int = 0, cableId: Int = 0) {
        scope.launch { sendNoteOffInternal(channel, note, velocity, cableId) }
    }

    // ─── Internal send (must be called from IO scope) ─────────────────────────

    private fun sendControlChangeInternal(channel: Int, cc: Int, value: Int, cableId: Int = 0) {
        val msg = byteArrayOf(
            (0xB0 or (channel and 0x0F)).toByte(),
            (cc and 0x7F).toByte(),
            (value and 0x7F).toByte()
        )
        sendRaw(msg, MidiMessageType.CONTROL_CHANGE, channel, cc, value, cableId)
    }

    private fun sendProgramChangeInternal(channel: Int, program: Int, cableId: Int = 0) {
        val msg = byteArrayOf(
            (0xC0 or (channel and 0x0F)).toByte(),
            (program and 0x7F).toByte()
        )
        sendRaw(msg, MidiMessageType.PROGRAM_CHANGE, channel, program, 0, cableId)
    }

    private fun sendNoteOnInternal(channel: Int, note: Int, velocity: Int, cableId: Int = 0) {
        val msg = byteArrayOf(
            (0x90 or (channel and 0x0F)).toByte(),
            (note and 0x7F).toByte(),
            (velocity and 0x7F).toByte()
        )
        sendRaw(msg, MidiMessageType.NOTE_ON, channel, note, velocity, cableId)
    }

    private fun sendNoteOffInternal(channel: Int, note: Int, velocity: Int = 0, cableId: Int = 0) {
        val msg = byteArrayOf(
            (0x80 or (channel and 0x0F)).toByte(),
            (note and 0x7F).toByte(),
            (velocity and 0x7F).toByte()
        )
        sendRaw(msg, MidiMessageType.NOTE_OFF, channel, note, velocity, cableId)
    }

    fun sendSysEx(hexData: String) {
        scope.launch {
            try {
                val bytes = hexStringToBytes(hexData)
                if (bytes.isEmpty()) return@launch
                // Wrap in F0 ... F7 if not already
                val wrapped = if (bytes.first() == 0xF0.toByte() && bytes.last() == 0xF7.toByte()) {
                    bytes
                } else {
                    byteArrayOf(0xF0.toByte()) + bytes + byteArrayOf(0xF7.toByte())
                }
                sendRaw(wrapped, MidiMessageType.SYSEX, 0, 0, 0, 0)
            } catch (e: Exception) {
                Log.e(TAG, "SysEx error", e)
            }
        }
    }

    suspend fun sendSysExFile(inputStream: InputStream, delayMs: Long) {
        withContext(Dispatchers.IO) {
            try {
                val bytes = inputStream.readBytes()
                // Split by F7 (end of SysEx) boundaries
                val messages = splitSysExMessages(bytes)
                messages.forEach { msg ->
                    sendRaw(msg, MidiMessageType.SYSEX, 0, 0, 0, 0)
                    if (delayMs > 0) delay(delayMs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "SysEx file error", e)
            }
        }
    }

    private fun splitSysExMessages(data: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var start = 0
        for (i in data.indices) {
            if (data[i] == 0xF7.toByte()) {
                result.add(data.copyOfRange(start, i + 1))
                start = i + 1
            }
        }
        return result
    }

    // ─── Raw Send with Retry ──────────────────────────────────────────────────

    private fun sendRaw(
        bytes: ByteArray,
        type: MidiMessageType,
        channel: Int,
        v1: Int,
        v2: Int,
        cableId: Int
    ) {
        val port = inputPort
        if (port == null) {
            Log.w(TAG, "No input port available")
            return
        }
        try {
            port.send(bytes, 0, bytes.size)
            logEntry(LogDirection.OUT, type, channel + 1, v1, v2, bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Send failed on cable $cableId, retrying…", e)
            // Retry on next cable port if available
            retryOnNextPort(bytes, type, channel, v1, v2, cableId)
        }
    }

    private fun retryOnNextPort(bytes: ByteArray, type: MidiMessageType, channel: Int, v1: Int, v2: Int, cableId: Int) {
        val device = midiDevice ?: return
        val portCount = device.info.inputPortCount
        val nextPort = (cableId + 1) % portCount
        try {
            val altPort = device.openInputPort(nextPort)
            altPort?.send(bytes, 0, bytes.size)
            altPort?.close()
            logEntry(LogDirection.OUT, type, channel + 1, v1, v2, bytes)
        } catch (e: Exception) {
            Log.e(TAG, "Retry also failed", e)
        }
    }

    // ─── MIDI In ─────────────────────────────────────────────────────────────

    fun openMidiReceiver(onReceive: (ByteArray, Int, Int, Long) -> Unit): MidiReceiver {
        return object : MidiReceiver() {
            override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                onReceive(msg, offset, count, timestamp)
                parseMidiIn(msg, offset, count)
            }
        }
    }

    /**
     * Connects a [MidiReceiver] to the device's first output port so that
     * incoming MIDI data is delivered to the receiver. Required for MIDI Learn
     * and the MIDI Monitor to function.
     */
    fun connectReceiver(receiver: MidiReceiver) {
        activeReceiver = receiver
        val device = midiDevice ?: return
        if (device.info.outputPortCount > 0) {
            scope.launch {
                try {
                    // Close previous output port if it was only used for receiving
                    // (MIDI Thru uses its own management via setupMidiThru)
                    val outPort = device.openOutputPort(0)
                    outPort?.connect(receiver)
                    Log.d(TAG, "Receiver connected to output port 0")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect receiver", e)
                }
            }
        } else {
            Log.w(TAG, "Device has no output ports — MIDI Learn unavailable")
        }
    }

    private fun parseMidiIn(msg: ByteArray, offset: Int, count: Int) {
        if (count < 1) return
        val status = msg[offset].toInt() and 0xFF
        val channel = status and 0x0F
        when (status and 0xF0) {
            0x80 -> logEntry(LogDirection.IN, MidiMessageType.NOTE_OFF, channel + 1,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F,
                msg.getOrElse(offset + 2) { 0 }.toInt() and 0x7F, msg)
            0x90 -> logEntry(LogDirection.IN, MidiMessageType.NOTE_ON, channel + 1,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F,
                msg.getOrElse(offset + 2) { 0 }.toInt() and 0x7F, msg)
            0xB0 -> logEntry(LogDirection.IN, MidiMessageType.CONTROL_CHANGE, channel + 1,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F,
                msg.getOrElse(offset + 2) { 0 }.toInt() and 0x7F, msg)
            0xC0 -> logEntry(LogDirection.IN, MidiMessageType.PROGRAM_CHANGE, channel + 1,
                msg.getOrElse(offset + 1) { 0 }.toInt() and 0x7F, 0, msg)
            0xF0 -> logEntry(LogDirection.IN, MidiMessageType.SYSEX, 0, 0, 0, msg)
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun logEntry(dir: LogDirection, type: MidiMessageType, ch: Int, v1: Int, v2: Int, raw: ByteArray) {
        val entry = MidiLogEntry(
            direction = dir,
            channel = ch,
            messageType = type,
            value1 = v1,
            value2 = v2,
            rawBytes = raw.joinToString(" ") { "%02X".format(it) }
        )
        scope.launch { _midiLog.emit(entry) }
    }

    private fun hexStringToBytes(hex: String): ByteArray {
        val cleaned = hex.replace(" ", "").replace("0x", "").replace(",", "")
        return try {
            ByteArray(cleaned.length / 2) { i ->
                cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (e: Exception) { byteArrayOf() }
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }
}
