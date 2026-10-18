package com.iqbal.journal

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.iqbal.journal.sync.ForegroundSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JournalApplication : Application() {
    @Inject lateinit var foregroundSync: ForegroundSync

    override fun onCreate() {
        super.onCreate()
        foregroundSync.start(ProcessLifecycleOwner.get().lifecycle)
    }
}
