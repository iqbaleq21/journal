package com.iqbal.journal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.iqbal.journal.data.local.UserPreferences
import com.iqbal.journal.sync.ForegroundSync
import com.iqbal.journal.sync.PeriodicSyncWorker
import com.iqbal.journal.widget.NoteWidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class JournalApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var foregroundSync: ForegroundSync

    @Inject lateinit var noteWidgetUpdater: NoteWidgetUpdater

    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()
        // Sekali baca file preferensi kecil; harus selesai sebelum layar pertama agar tidak berkedip ganti bahasa.
        runBlocking { userPreferences.applyStoredLanguage() }
        val processLifecycleOwner = ProcessLifecycleOwner.get()
        foregroundSync.start(processLifecycleOwner.lifecycle)
        // Seumur proses, termasuk saat dibangunkan di background, agar widget ikut setiap perubahan.
        noteWidgetUpdater.start(processLifecycleOwner.lifecycleScope)
        PeriodicSyncWorker.schedule(this)
    }

    // Worker dibuat lewat Hilt agar bisa menerima dependensi di konstruktor.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
