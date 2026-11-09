package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DeviceRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /**
     * Dilewati bila belum ada sesi (profil belum diisi); token didaftarkan nanti saat app dibuka.
     * Lewat RPC, bukan upsert: token yang masih tercatat milik akun lama di HP ini (sesi dicabut) tertutup RLS.
     */
    suspend fun upsert(token: String) {
        client.auth.awaitInitialization()
        if (client.auth.currentUserOrNull() == null) return
        client.postgrest.rpc(Rpc.REGISTER_DEVICE, buildJsonObject { put("p_token", token) })
    }
}
