package com.iqbal.journal.data.push

import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

class PushTokenSource @Inject constructor() {
    suspend fun currentToken(): String = FirebaseMessaging.getInstance().token.await()
}
