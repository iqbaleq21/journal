package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.WidgetSelection
import kotlinx.coroutines.flow.Flow

/** Pilihan catatan per widget; disimpan lokal di HP ini saja. */
interface WidgetRepository {
    /** Id catatan yang sedang tampil di minimal satu widget. */
    val pinnedNoteIds: Flow<Set<String>>

    suspend fun selection(appWidgetId: Int): WidgetSelection?

    fun observeSelection(appWidgetId: Int): Flow<WidgetSelection?>

    suspend fun select(selection: WidgetSelection)

    suspend fun remove(appWidgetIds: List<Int>)
}
