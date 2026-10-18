package com.iqbal.journal.di

import com.iqbal.journal.sync.NoteNotifier
import com.iqbal.journal.sync.SystemNoteNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindNoteNotifier(impl: SystemNoteNotifier): NoteNotifier
}
