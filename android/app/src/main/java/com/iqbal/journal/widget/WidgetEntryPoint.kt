package com.iqbal.journal.widget

import com.iqbal.journal.domain.repository.WidgetRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Glance membuat widget dan receiver sendiri, jadi dependensi diambil lewat entry point, bukan konstruktor. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun stateLoader(): NoteWidgetStateLoader

    fun widgetRepository(): WidgetRepository
}
