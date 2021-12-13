package com.woxich.locatalert.model.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.woxich.locatalert.model.Alarm
import com.woxich.locatalert.model.Note

/**
 * SQLite database of the app, managed by Room.
 */
@Database(version = 1, entities = [CachedProfilePicture::class, Note::class, Alarm::class], exportSchema = false)
@androidx.room.TypeConverters(TypeConverters::class)
abstract class LocatAlertDatabase: RoomDatabase() {

    abstract val noteDao: NoteDao
    abstract val alarmsDao: AlarmsDao
    /**
     * Get data access object of [CachedProfilePicture] table.
     */
    abstract fun getCachedProfilePictureDao(): CachedProfilePictureDao

}

/**
 * Name of the database.
 */
const val DATABASE_NAME = "gaspar_chat_database"