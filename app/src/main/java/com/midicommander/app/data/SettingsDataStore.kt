package com.midicommander.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.midicommander.app.data.model.GlobalSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "global_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val GLOBAL_MIDI_CHANNEL = intPreferencesKey("global_midi_channel")
        val DEFAULT_CABLE_ID    = intPreferencesKey("default_cable_id")
        val MIDI_THRU           = booleanPreferencesKey("midi_thru")
        val USE_NATIVE_MIDI     = booleanPreferencesKey("use_native_midi")
        val SYSEX_DELAY         = longPreferencesKey("sysex_delay")
        val HAPTIC_FEEDBACK     = booleanPreferencesKey("haptic_feedback")
        val DARK_THEME          = booleanPreferencesKey("dark_theme")
        val MIDI_LEARN          = booleanPreferencesKey("midi_learn")
    }

    val settings: Flow<GlobalSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            GlobalSettings(
                globalMidiChannel = prefs[Keys.GLOBAL_MIDI_CHANNEL] ?: 0,
                defaultCableId    = prefs[Keys.DEFAULT_CABLE_ID] ?: 0,
                midiThru          = prefs[Keys.MIDI_THRU] ?: false,
                useNativeMidi     = prefs[Keys.USE_NATIVE_MIDI] ?: true,
                sysExDelay        = prefs[Keys.SYSEX_DELAY] ?: 20L,
                hapticFeedback    = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
                darkTheme         = prefs[Keys.DARK_THEME] ?: true,
                midiLearnEnabled  = prefs[Keys.MIDI_LEARN] ?: false
            )
        }

    suspend fun updateSettings(settings: GlobalSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_MIDI_CHANNEL] = settings.globalMidiChannel
            prefs[Keys.DEFAULT_CABLE_ID]    = settings.defaultCableId
            prefs[Keys.MIDI_THRU]           = settings.midiThru
            prefs[Keys.USE_NATIVE_MIDI]     = settings.useNativeMidi
            prefs[Keys.SYSEX_DELAY]         = settings.sysExDelay
            prefs[Keys.HAPTIC_FEEDBACK]     = settings.hapticFeedback
            prefs[Keys.DARK_THEME]          = settings.darkTheme
            prefs[Keys.MIDI_LEARN]          = settings.midiLearnEnabled
        }
    }
}
