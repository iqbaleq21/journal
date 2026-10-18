package com.iqbal.journal.sync

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Sinkronisasi selama aplikasi terlihat (PRD §5.5): ambil ulang grup & catatan, lalu dengarkan Realtime.
 * Berhenti saat aplikasi ke background; di sana push + WorkManager (M4) yang mengambil alih.
 */
@Singleton
class ForegroundSync @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val groupRepository: GroupRepository,
    private val noteRepository: NoteRepository
) {
    fun start(processLifecycle: Lifecycle) {
        processLifecycle.coroutineScope.launch {
            processLifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Sebelum profil diisi belum ada sesi, jadi belum ada yang bisa disinkron.
                profileRepository.myProfile.map { it != null }.distinctUntilChanged().collectLatest { hasProfile ->
                    if (hasProfile) syncWhileVisible()
                }
            }
        }
    }

    private suspend fun syncWhileVisible() {
        while (true) {
            refreshAll()
            // Normalnya tidak pernah selesai; jika koneksi putus, catatan yang terlewat diambil di putaran berikut.
            val result = noteRepository.listenForChanges()
            if (result is Outcome.Failure) Log.w(TAG, "Realtime terputus: ${result.error}")
            delay(RECONNECT_DELAY_MILLIS)
        }
    }

    private suspend fun refreshAll() {
        groupRepository.refresh()
        groupRepository.groups.first().forEach { noteRepository.refresh(it.id) }
    }

    private companion object {
        const val TAG = "ForegroundSync"
        const val RECONNECT_DELAY_MILLIS = 5_000L
    }
}
