package com.iqbal.journal.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.widget.NoteWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Cadangan bila push terlewat (Doze, optimasi baterai vendor), sekaligus merapikan cache lokal (PRD §5.6). */
@HiltWorker
class PeriodicSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profileRepository: ProfileRepository,
    private val fullSync: FullSync,
    private val noteRepository: NoteRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (profileRepository.myProfile.first() == null) return Result.success()
        val synced = fullSync.run()
        noteRepository.trimCache()
        // Juga menyegarkan waktu relatif ("5 mnt") di widget meski tidak ada perubahan.
        NoteWidget().updateAll(applicationContext)
        return if (synced) Result.success() else Result.retry()
    }

    companion object {
        /** Interval minimum WorkManager. */
        private const val INTERVAL_MINUTES = 15L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("periodic-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
