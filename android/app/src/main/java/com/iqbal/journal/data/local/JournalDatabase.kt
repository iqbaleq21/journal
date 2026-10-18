package com.iqbal.journal.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MyProfileEntity::class,
        GroupEntity::class,
        NoteEntity::class,
        AuthorEntity::class,
        WidgetSelectionEntity::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)]
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun myProfileDao(): MyProfileDao

    abstract fun groupDao(): GroupDao

    abstract fun noteDao(): NoteDao

    abstract fun authorDao(): AuthorDao

    abstract fun widgetSelectionDao(): WidgetSelectionDao

    companion object {
        const val NAME = "journal.db"
    }
}
