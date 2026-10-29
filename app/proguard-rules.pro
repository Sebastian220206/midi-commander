# Suppress Room schema export warning (we set exportSchema=false)
-dontwarn androidx.room.RoomMasterTable
-keep class com.midicommander.app.data.model.** { *; }
-keep class com.midicommander.app.data.db.** { *; }
-keepattributes *Annotation*
# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
