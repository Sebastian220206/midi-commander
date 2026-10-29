package com.midicommander.app.viewmodel

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.midicommander.app.data.SettingsDataStore
import com.midicommander.app.data.model.*
import com.midicommander.app.data.repository.MidiRepository
import com.midicommander.app.midi.MidiConnectionState
import com.midicommander.app.midi.MidiEngine
import com.midicommander.app.midi.MidiInputParser
import com.midicommander.app.midi.UsbMidiDeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

// ─── UI State ────────────────────────────────────────────────────────────────

data class MainUiState(
    val banks: List<Bank> = emptyList(),
    val selectedBankId: String = "bank_A",
    val controls: List<ControlConfig> = emptyList(),
    val editMode: Boolean = false,
    val presets: List<Preset> = emptyList(),
    val settings: GlobalSettings = GlobalSettings(),
    val midiLearnTargetId: String? = null,
    val showMonitor: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MidiRepository,
    private val settingsDataStore: SettingsDataStore,
    val midiEngine: MidiEngine,
    val deviceManager: UsbMidiDeviceManager,
    private val midiInputParser: MidiInputParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Tracks the active bank-controls collection coroutine so it can be cancelled on bank switch. */
    private var bankCollectionJob: Job? = null

    val connectionState: StateFlow<MidiConnectionState> = midiEngine.connectionState
    val midiLog: SharedFlow<MidiLogEntry> = midiEngine.midiLog
    val availableDevices: StateFlow<List<MidiDeviceInfo>> = deviceManager.availableDevices

    init {
        viewModelScope.launch { repository.ensureDefaultBanks() }

        // Observe banks
        viewModelScope.launch {
            repository.getAllBanks().collect { banks ->
                _uiState.update { it.copy(banks = banks) }
            }
        }

        // Observe presets
        viewModelScope.launch {
            repository.getAllPresets().collect { presets ->
                _uiState.update { it.copy(presets = presets) }
            }
        }

        // Observe settings
        viewModelScope.launch {
            settingsDataStore.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
                midiEngine.setMidiThru(settings.midiThru)
            }
        }

        // Load initial bank controls
        selectBank("bank_A")

        // Init USB device manager
        deviceManager.initialize()

        // Watch connection state — open output port receiver for MIDI Learn / Monitor
        viewModelScope.launch {
            midiEngine.connectionState.collect { state ->
                if (state is MidiConnectionState.Connected) {
                    startMidiReceiver()
                }
            }
        }

        // MIDI Learn — auto-assign first incoming CC/Note/PC
        viewModelScope.launch {
            midiInputParser.events.collect { event ->
                val targetId = _uiState.value.midiLearnTargetId ?: return@collect
                val control = _uiState.value.controls.firstOrNull { it.id == targetId } ?: return@collect
                val updated = control.copy(
                    midiChannel = event.channel,
                    messageType = event.messageType,
                    value = event.value1,
                    secondaryValue = event.value2
                )
                upsertControl(updated)
                stopMidiLearn()
            }
        }
    }

    // ─── MIDI Receiver ─────────────────────────────────────────────────────────

    private fun startMidiReceiver() {
        val receiver = midiEngine.openMidiReceiver { msg, offset, count, _ ->
            if (_uiState.value.midiLearnTargetId != null ||
                _uiState.value.showMonitor) {
                midiInputParser.parse(msg, offset, count)
            }
        }
        // Connect the receiver to the device's output port so incoming MIDI
        // data is actually delivered — required for MIDI Learn & Monitor.
        midiEngine.connectReceiver(receiver)
    }

    // ─── Bank ─────────────────────────────────────────────────────────────────

    fun selectBank(bankId: String) {
        _uiState.update { it.copy(selectedBankId = bankId) }
        // Cancel the previous bank's collection to prevent coroutine leak
        bankCollectionJob?.cancel()
        bankCollectionJob = viewModelScope.launch {
            repository.getControlsForBank(bankId).collect { controls ->
                _uiState.update { it.copy(controls = controls) }
            }
        }
    }

    // ─── Edit Mode ────────────────────────────────────────────────────────────

    fun toggleEditMode() {
        _uiState.update { it.copy(editMode = !it.editMode) }
    }

    // ─── Controls ─────────────────────────────────────────────────────────────

    fun upsertControl(control: ControlConfig) {
        viewModelScope.launch { repository.upsertControl(control) }
    }

    fun resetControl(control: ControlConfig) {
        viewModelScope.launch { repository.resetControl(control) }
    }

    fun copyControl(source: ControlConfig, targetBankId: String, targetRow: Int, targetCol: Int) {
        viewModelScope.launch { repository.copyControl(source, targetBankId, targetRow, targetCol) }
    }

    // ─── MIDI Send ────────────────────────────────────────────────────────────

    fun sendControl(control: ControlConfig) {
        val settings = _uiState.value.settings
        midiEngine.sendControl(control, settings.globalMidiChannel)
        if (settings.hapticFeedback) triggerHaptic()
    }

    fun sendSliderCc(control: ControlConfig, value: Int) {
        val ch = (_uiState.value.settings.globalMidiChannel.let {
            if (it in 1..16) it - 1 else control.midiChannel - 1
        }).coerceIn(0, 15)
        midiEngine.sendControlChange(ch, control.value, value, control.cableId)
    }

    // ─── MIDI Learn ───────────────────────────────────────────────────────────

    fun startMidiLearn(controlId: String) {
        _uiState.update { it.copy(midiLearnTargetId = controlId) }
    }

    fun stopMidiLearn() {
        _uiState.update { it.copy(midiLearnTargetId = null) }
    }

    fun applyMidiLearn(channel: Int, ccNumber: Int) {
        val targetId = _uiState.value.midiLearnTargetId ?: return
        val control = _uiState.value.controls.firstOrNull { it.id == targetId } ?: return
        val updated = control.copy(
            midiChannel = channel,
            messageType = MidiMessageType.CONTROL_CHANGE,
            value = ccNumber
        )
        upsertControl(updated)
        stopMidiLearn()
    }

    // ─── Presets ─────────────────────────────────────────────────────────────

    fun savePreset(name: String) {
        viewModelScope.launch { repository.savePreset(name) }
    }

    fun loadPreset(preset: Preset) {
        viewModelScope.launch {
            repository.loadPreset(preset)
            selectBank(_uiState.value.selectedBankId)
        }
    }

    fun deletePreset(preset: Preset) {
        viewModelScope.launch { repository.deletePreset(preset) }
    }

    suspend fun exportJson(): String = repository.exportAllToJson()

    fun importJson(json: String) {
        viewModelScope.launch {
            repository.importFromJson(json)
            selectBank(_uiState.value.selectedBankId)
        }
    }

    // ─── Settings ────────────────────────────────────────────────────────────

    fun updateSettings(settings: GlobalSettings) {
        viewModelScope.launch { settingsDataStore.updateSettings(settings) }
    }

    // ─── SysEx File ──────────────────────────────────────────────────────────

    fun sendSysExFile(inputStream: InputStream) {
        val delay = _uiState.value.settings.sysExDelay
        viewModelScope.launch { midiEngine.sendSysExFile(inputStream, delay) }
    }

    // ─── Monitor ─────────────────────────────────────────────────────────────

    fun toggleMonitor() {
        _uiState.update { it.copy(showMonitor = !it.showMonitor) }
    }

    // ─── Device ──────────────────────────────────────────────────────────────

    fun connectToDevice(info: MidiDeviceInfo, portIndex: Int = 0) {
        deviceManager.connectToDevice(info, portIndex)
    }

    // ─── Haptic ──────────────────────────────────────────────────────────────

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(30)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        deviceManager.shutdown()
    }
}
