package com.midicommander.app.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.midicommander.app.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MidiDatabase =
        Room.databaseBuilder(context, MidiDatabase::class.java, "midi_commander.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideControlDao(db: MidiDatabase): ControlDao = db.controlDao()

    @Provides
    fun provideBankDao(db: MidiDatabase): BankDao = db.bankDao()

    @Provides
    fun providePresetDao(db: MidiDatabase): PresetDao = db.presetDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
