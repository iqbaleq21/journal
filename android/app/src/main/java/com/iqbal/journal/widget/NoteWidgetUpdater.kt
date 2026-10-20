package com.iqbal.journal.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.iqbal.journal.domain.repository.AppearanceRepository
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Memperbarui semua widget setiap kali cache lokal berubah, dari jalur mana pun
 * (Realtime, refresh, atau push/WorkManager di M4), selama proses aplikasi hidup.
 */
@Singleton
class NoteWidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val groupRepository: GroupRepository,
    private val profileRepository: ProfileRepository,
    private val appearanceRepository: AppearanceRepository
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            merge(
                noteRepository.changes,
                groupRepository.groups.map { },
                profileRepository.myProfile.map { },
                appearanceRepository.themeMode.map { }
            )
                // Satu sinkron menulis banyak baris; cukup sekali render ulang setelah tenang.
                .debounce(UPDATE_DEBOUNCE_MILLIS)
                .collect { NoteWidget().updateAll(context) }
        }
    }

    private companion object {
        const val UPDATE_DEBOUNCE_MILLIS = 500L
    }
}
