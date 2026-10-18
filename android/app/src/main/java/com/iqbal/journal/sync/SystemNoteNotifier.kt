package com.iqbal.journal.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.ui.navigation.NoteLink
import com.iqbal.journal.ui.navigation.openNoteIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SystemNoteNotifier @Inject constructor(@ApplicationContext private val context: Context) : NoteNotifier {
    override fun notifyNewNote(note: Note, groupName: String) {
        // Tanpa izin (Android 13+) notifikasi dilewati; widget tetap diperbarui.
        if (!canPostNotifications()) return
        createChannel()
        val author = note.authorName ?: context.getString(R.string.note_author_unknown)
        val openNote = PendingIntent.getActivity(
            context,
            note.id.hashCode(),
            context.openNoteIntent(NoteLink(note.groupId, note.id)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_NOTES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(groupName.ifEmpty { context.getString(R.string.app_name) })
            .setContentText(context.getString(R.string.notification_new_note, author, note.content))
            .setStyle(NotificationCompat.BigTextStyle().bigText(note.content))
            .setContentIntent(openNote)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(note.id.hashCode(), notification)
    }

    private fun canPostNotifications(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_NEW_NOTES,
            context.getString(R.string.notification_channel_new_notes),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_NEW_NOTES = "new_notes"
    }
}
