package com.iqbal.journal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MyProfileEntity::class, GroupEntity::class],
    version = 1,
    exportSchema = true
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun myProfileDao(): MyProfileDao

    abstract fun groupDao(): GroupDao

    companion object {
        const val NAME = "journal.db"
    }
}
