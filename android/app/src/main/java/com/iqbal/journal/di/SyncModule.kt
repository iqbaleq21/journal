package com.iqbal.journal.di

import com.iqbal.journal.data.repository.OutboxScheduler
import com.iqbal.journal.sync.NoteNotifier
import com.iqbal.journal.sync.SystemNoteNotifier
import com.iqbal.journal.sync.WorkManagerOutboxScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindNoteNotifier(impl: SystemNoteNotifier): NoteNotifier

    @Binds
    abstract fun bindOutboxScheduler(impl: WorkManagerOutboxScheduler): OutboxScheduler
}
