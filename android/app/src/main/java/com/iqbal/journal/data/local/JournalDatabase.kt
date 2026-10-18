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
        WidgetSelectionEntity::class,
        MemberEntity::class,
        PendingOpEntity::class
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun myProfileDao(): MyProfileDao

    abstract fun groupDao(): GroupDao

    abstract fun noteDao(): NoteDao

    abstract fun authorDao(): AuthorDao

    abstract fun widgetSelectionDao(): WidgetSelectionDao

    abstract fun memberDao(): MemberDao

    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        const val NAME = "journal.db"
    }
}
