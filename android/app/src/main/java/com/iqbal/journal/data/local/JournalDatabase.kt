package com.iqbal.journal.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MyProfileEntity::class, GroupEntity::class, NoteEntity::class, AuthorEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun myProfileDao(): MyProfileDao

    abstract fun groupDao(): GroupDao

    abstract fun noteDao(): NoteDao

    abstract fun authorDao(): AuthorDao

    companion object {
        const val NAME = "journal.db"
    }
}
