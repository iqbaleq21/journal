package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Profil pengguna di HP ini; null jika belum mengisi layar Kenalan. */
    val myProfile: Flow<Profile?>

    /** Simpan profil (Kenalan atau Pengaturan); identitas anonim dibuat dulu bila HP ini belum punya. */
    suspend fun saveProfile(displayName: String, email: String): Outcome<Profile>
}
