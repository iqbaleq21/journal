package com.iqbal.journal.domain.model

/**
 * Isi satu widget home screen: catatan tertentu, atau catatan terbaru di grup bila [noteId] null (US-16).
 */
data class WidgetSelection(val appWidgetId: Int, val noteId: String?, val groupId: String) {
    val followsLatest: Boolean get() = noteId == null
}
