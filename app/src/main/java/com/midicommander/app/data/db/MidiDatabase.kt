package com.midicommander.app.data.db

import androidx.room.*
import com.midicommander.app.data.model.ControlConfig
import com.midicommander.app.data.model.Bank
import com.midicommander.app.data.model.Preset
import kotlinx.coroutines.flow.Flow

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface ControlDao {
    @Query("SELECT * FROM controls WHERE bankId = :bankId ORDER BY row, col")
    fun getControlsForBank(bankId: String): Flow<List<ControlConfig>>

    @Query("SELECT * FROM controls")
    suspend fun getAllControls(): List<ControlConfig>

    @Upsert
    suspend fun upsertControl(control: ControlConfig)

    @Upsert
    suspend fun upsertControls(controls: List<ControlConfig>)

    @Delete
    suspend fun deleteControl(control: ControlConfig)

    @Query("DELETE FROM controls WHERE bankId = :bankId")
    suspend fun deleteControlsForBank(bankId: String)
}

@Dao
interface BankDao {
    @Query("SELECT * FROM banks ORDER BY `index`")
    fun getAllBanks(): Flow<List<Bank>>

    @Query("SELECT * FROM banks WHERE id = :id")
    suspend fun getBankById(id: String): Bank?

    @Upsert
    suspend fun upsertBank(bank: Bank)

    @Upsert
    suspend fun upsertBanks(banks: List<Bank>)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun getAllPresets(): Flow<List<Preset>>

    @Upsert
    suspend fun upsertPreset(preset: Preset)

    @Delete
    suspend fun deletePreset(preset: Preset)

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun getPresetById(id: String): Preset?
}

// ─── Converters ───────────────────────────────────────────────────────────────

class Converters {
    @TypeConverter
    fun fromMidiMessageType(value: String) =
        com.midicommander.app.data.model.MidiMessageType.valueOf(value)

    @TypeConverter
    fun midiMessageTypeToString(type: com.midicommander.app.data.model.MidiMessageType) =
        type.name

    @TypeConverter
    fun fromControlType(value: String) =
        com.midicommander.app.data.model.ControlType.valueOf(value)

    @TypeConverter
    fun controlTypeToString(type: com.midicommander.app.data.model.ControlType) = type.name
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [ControlConfig::class, Bank::class, Preset::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MidiDatabase : RoomDatabase() {
    abstract fun controlDao(): ControlDao
    abstract fun bankDao(): BankDao
    abstract fun presetDao(): PresetDao
}
