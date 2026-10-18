package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    /** Profil pengguna di HP ini; null jika belum mengisi layar Kenalan. */
    val myProfile: Flow<Profile?>

    /** Membuat identitas (anonymous sign-in bila perlu) lalu menyimpan profil. */
    suspend fun createProfile(displayName: String, email: String): Outcome<Profile>
}
