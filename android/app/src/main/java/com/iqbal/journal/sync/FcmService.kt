package com.iqbal.journal.sync

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = RegisterDeviceWorker.enqueue(this, token)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data[KEY_TYPE] != TYPE_NOTE_CHANGED) return
        val noteId = data[KEY_NOTE_ID] ?: return
        // Kerja jaringan tidak dilakukan di sini: layanan ini hanya diberi waktu singkat.
        NoteSyncWorker.enqueue(
            this,
            NoteChangedMessage(
                noteId = noteId,
                isNew = data[KEY_IS_NEW].toBoolean(),
                notify = data[KEY_NOTIFY].toBoolean()
            )
        )
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val TYPE_NOTE_CHANGED = "note_changed"
        const val KEY_NOTE_ID = "note_id"
        const val KEY_IS_NEW = "is_new"
        const val KEY_NOTIFY = "notify"
    }
}
