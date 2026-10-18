package com.iqbal.journal.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.iqbal.journal.data.local.AuthorDao
import com.iqbal.journal.data.local.GroupDao
import com.iqbal.journal.data.local.JournalDatabase
import com.iqbal.journal.data.local.MyProfileDao
import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.local.WidgetSelectionDao
import com.iqbal.journal.data.repository.DeviceRepositoryImpl
import com.iqbal.journal.data.repository.GroupRepositoryImpl
import com.iqbal.journal.data.repository.NoteRepositoryImpl
import com.iqbal.journal.data.repository.ProfileRepositoryImpl
import com.iqbal.journal.data.repository.WidgetRepositoryImpl
import com.iqbal.journal.domain.repository.DeviceRepository
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.repository.WidgetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDataModule {
    private const val PREFERENCES_FILE = "user_preferences"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JournalDatabase =
        Room.databaseBuilder(context, JournalDatabase::class.java, JournalDatabase.NAME).build()

    @Provides
    fun provideMyProfileDao(database: JournalDatabase): MyProfileDao = database.myProfileDao()

    @Provides
    fun provideGroupDao(database: JournalDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideNoteDao(database: JournalDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideAuthorDao(database: JournalDatabase): AuthorDao = database.authorDao()

    @Provides
    fun provideWidgetSelectionDao(database: JournalDatabase): WidgetSelectionDao = database.widgetSelectionDao()

    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(PREFERENCES_FILE) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindWidgetRepository(impl: WidgetRepositoryImpl): WidgetRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository
}
