package com.iqbal.journal.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Pilihan widget yang dibuang tidak lagi menahan catatannya di cache.
        val pending = goAsync()
        val repository = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).widgetRepository()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                repository.remove(appWidgetIds.toList())
            } finally {
                pending.finish()
            }
        }
    }
}
