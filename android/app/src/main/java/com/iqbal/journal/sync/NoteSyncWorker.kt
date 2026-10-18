package com.iqbal.journal.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.widget.NoteWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Dijalankan segera setelah push `note_changed`, termasuk saat aplikasi tertutup (PRD §5.5). */
@HiltWorker
class NoteSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val handler: NoteChangeHandler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val message = NoteChangedMessage(
            noteId = inputData.getString(KEY_NOTE_ID) ?: return Result.failure(),
            isNew = inputData.getBoolean(KEY_IS_NEW, false),
            notify = inputData.getBoolean(KEY_NOTIFY, false)
        )
        return when (val result = handler.handle(message)) {
            is Outcome.Success -> {
                // Langsung, tidak menunggu NoteWidgetUpdater: proses bisa dimatikan begitu worker selesai.
                NoteWidget().updateAll(applicationContext)
                Result.success()
            }

            is Outcome.Failure -> {
                Log.w(TAG, "Sinkron catatan gagal (percobaan ke-${runAttemptCount + 1}): ${result.error}")
                Result.retry()
            }
        }
    }

    // Expedited work di Android < 12 berjalan sebagai foreground service dan butuh notifikasi.
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SYNC,
                applicationContext.getString(R.string.notification_channel_sync),
                NotificationManager.IMPORTANCE_MIN
            )
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.notification_syncing))
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(SYNC_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(SYNC_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "NoteSyncWorker"
        private const val KEY_NOTE_ID = "note_id"
        private const val KEY_IS_NEW = "is_new"
        private const val KEY_NOTIFY = "notify"
        private const val CHANNEL_SYNC = "sync"
        private const val SYNC_NOTIFICATION_ID = 1

        fun enqueue(context: Context, message: NoteChangedMessage) {
            val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(
                    workDataOf(KEY_NOTE_ID to message.noteId, KEY_IS_NEW to message.isNew, KEY_NOTIFY to message.notify)
                )
                .build()
            // Push terbaru menggantikan yang lama (syncNote selalu mengambil versi terbaru), termasuk yang sedang
            // menunggu ulang dengan backoff; dengan APPEND push baru ikut tertahan di belakangnya.
            WorkManager.getInstance(context)
                .enqueueUniqueWork("note-sync-${message.noteId}", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
