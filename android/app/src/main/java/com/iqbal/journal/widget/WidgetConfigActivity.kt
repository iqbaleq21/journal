package com.iqbal.journal.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.iqbal.journal.MainActivity
import com.iqbal.journal.domain.repository.AppearanceRepository
import com.iqbal.journal.ui.theme.setJournalContent
import com.iqbal.journal.ui.widgetpicker.WidgetPickerRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Layar pilih catatan saat widget dipasang (US-7) dan saat diatur ulang (US-9). */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject lateinit var appearance: AppearanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // Keluar tanpa memilih saat pemasangan pertama = widget batal dipasang.
        setResult(Activity.RESULT_CANCELED, resultIntent(appWidgetId))

        setJournalContent(appearance.themeMode) {
            WidgetPickerRoute(
                onDone = { finishWithWidget(appWidgetId) },
                onClose = ::finish,
                onOpenApp = {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }

    private fun finishWithWidget(appWidgetId: Int) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
            NoteWidget().update(this@WidgetConfigActivity, glanceId)
            setResult(Activity.RESULT_OK, resultIntent(appWidgetId))
            finish()
        }
    }

    private fun resultIntent(appWidgetId: Int) = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
