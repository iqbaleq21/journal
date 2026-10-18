package com.iqbal.journal.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iqbal.journal.data.repository.OutboxScheduler
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.widget.NoteWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Mengirim tulis/edit/hapus yang tertunda begitu ada koneksi, meski aplikasinya sudah ditutup (US-12). */
@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val noteRepository: NoteRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val empty = noteRepository.flushPending()
        // Tanda "belum terkirim" di widget tidak ada, tapi updated_at resmi dari server bisa mengubah waktu relatifnya.
        NoteWidget().updateAll(applicationContext)
        return if (empty) Result.success() else Result.retry()
    }
}

class WorkManagerOutboxScheduler @Inject constructor(@ApplicationContext private val context: Context) :
    OutboxScheduler {
    override fun scheduleFlush() {
        val request = OneTimeWorkRequestBuilder<OutboxWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // KEEP: satu worker sudah mengirim seluruh antrean, termasuk yang ditambahkan belakangan.
        WorkManager.getInstance(context).enqueueUniqueWork("note-outbox", ExistingWorkPolicy.KEEP, request)
    }
}
