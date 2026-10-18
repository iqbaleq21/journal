package com.iqbal.journal.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.iqbal.journal.data.local.GroupDao
import com.iqbal.journal.data.local.JournalDatabase
import com.iqbal.journal.data.local.MyProfileDao
import com.iqbal.journal.data.repository.GroupRepositoryImpl
import com.iqbal.journal.data.repository.ProfileRepositoryImpl
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
}
