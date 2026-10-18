package com.iqbal.journal

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.iqbal.journal.sync.ForegroundSync
import com.iqbal.journal.widget.NoteWidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JournalApplication : Application() {
    @Inject lateinit var foregroundSync: ForegroundSync

    @Inject lateinit var noteWidgetUpdater: NoteWidgetUpdater

    override fun onCreate() {
        super.onCreate()
        val processLifecycleOwner = ProcessLifecycleOwner.get()
        foregroundSync.start(processLifecycleOwner.lifecycle)
        // Seumur proses, termasuk saat dibangunkan di background, agar widget ikut setiap perubahan.
        noteWidgetUpdater.start(processLifecycleOwner.lifecycleScope)
    }
}
