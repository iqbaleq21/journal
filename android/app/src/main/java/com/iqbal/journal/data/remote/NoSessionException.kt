package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

/** Tidak ada sesi: profil belum diisi, atau sesi dicabut server (lihat ProfileRepository.isSessionLost). */
class NoSessionException : IllegalStateException("Belum ada sesi")

/**
 * Client yang siap dipakai sebagai pengguna saat ini. Sesi dimuat dari penyimpanan secara asinkron; request
 * sebelum itu memakai anon key, sehingga RLS mengembalikan kosong (cache lokal ikut terhapus) atau menolak
 * (antrean offline membuang perubahan).
 */
internal suspend fun SupabaseClient.withSession(): SupabaseClient {
    auth.awaitInitialization()
    if (auth.currentSessionOrNull() == null) throw NoSessionException()
    return this
}
