package com.midicommander.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.midicommander.app.data.db.BankDao
import com.midicommander.app.data.db.ControlDao
import com.midicommander.app.data.db.PresetDao
import com.midicommander.app.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MidiRepository @Inject constructor(
    private val controlDao: ControlDao,
    private val bankDao: BankDao,
    private val presetDao: PresetDao,
    private val gson: Gson
) {
    // ─── Banks ────────────────────────────────────────────────────────────────

    fun getAllBanks(): Flow<List<Bank>> = bankDao.getAllBanks()

    suspend fun ensureDefaultBanks() {
        val defaults = listOf("A", "B", "C", "D", "E", "F", "G", "H").mapIndexed { i, label ->
            Bank(id = "bank_$label", label = label, index = i)
        }
        bankDao.upsertBanks(defaults)
    }

    // ─── Controls ─────────────────────────────────────────────────────────────

    fun getControlsForBank(bankId: String): Flow<List<ControlConfig>> =
        controlDao.getControlsForBank(bankId)

    suspend fun upsertControl(control: ControlConfig) =
        controlDao.upsertControl(control)

    suspend fun resetControl(control: ControlConfig) {
        controlDao.upsertControl(
            ControlConfig(
                id = control.id,
                bankId = control.bankId,
                row = control.row,
                col = control.col
            )
        )
    }

    suspend fun copyControl(source: ControlConfig, targetBankId: String, targetRow: Int, targetCol: Int) {
        val newId = "${targetBankId}_${targetRow}_${targetCol}"
        controlDao.upsertControl(
            source.copy(id = newId, bankId = targetBankId, row = targetRow, col = targetCol)
        )
    }

    // ─── Presets ─────────────────────────────────────────────────────────────

    fun getAllPresets(): Flow<List<Preset>> = presetDao.getAllPresets()

    suspend fun savePreset(name: String) {
        val controls = controlDao.getAllControls()
        val json = gson.toJson(controls)
        val preset = Preset(
            id = UUID.randomUUID().toString(),
            name = name,
            controlsJson = json
        )
        presetDao.upsertPreset(preset)
    }

    suspend fun loadPreset(preset: Preset) {
        val type = object : TypeToken<List<ControlConfig>>() {}.type
        val controls: List<ControlConfig> = gson.fromJson(preset.controlsJson, type)
        // Clear all and re-insert
        controls.groupBy { it.bankId }.forEach { (bankId, bankControls) ->
            controlDao.deleteControlsForBank(bankId)
            controlDao.upsertControls(bankControls)
        }
    }

    suspend fun deletePreset(preset: Preset) = presetDao.deletePreset(preset)

    // ─── JSON Export/Import ───────────────────────────────────────────────────

    suspend fun exportAllToJson(): String {
        val controls = controlDao.getAllControls()
        return gson.toJson(controls)
    }

    suspend fun importFromJson(json: String) {
        val type = object : TypeToken<List<ControlConfig>>() {}.type
        val controls: List<ControlConfig> = gson.fromJson(json, type)
        controls.groupBy { it.bankId }.forEach { (bankId, bankControls) ->
            controlDao.deleteControlsForBank(bankId)
            controlDao.upsertControls(bankControls)
        }
    }
}
