package com.iqbal.journal.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.DeviceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Mendaftarkan token push baru; lewat WorkManager agar tetap selesai (dan diulang) setelah FcmService berhenti. */
@HiltWorker
class RegisterDeviceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val deviceRepository: DeviceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = inputData.getString(KEY_TOKEN) ?: return Result.failure()
        return when (deviceRepository.register(token)) {
            is Outcome.Success -> Result.success()
            is Outcome.Failure -> Result.retry()
        }
    }

    companion object {
        private const val KEY_TOKEN = "token"

        fun enqueue(context: Context, token: String) {
            val request = OneTimeWorkRequestBuilder<RegisterDeviceWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workDataOf(KEY_TOKEN to token))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("register-device", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
