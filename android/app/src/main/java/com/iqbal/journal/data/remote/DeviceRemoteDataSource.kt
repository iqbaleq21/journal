package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class DeviceRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /** Dilewati bila belum ada sesi (profil belum diisi); token didaftarkan nanti saat app dibuka. */
    suspend fun upsert(token: String) {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        client.from(Tables.DEVICES).upsert(DeviceDto(fcmToken = token, userId = userId))
    }
}
