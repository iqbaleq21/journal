package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.AuthorDao
import com.iqbal.journal.data.remote.ProfileRemoteDataSource
import com.iqbal.journal.data.remote.outcomeOf
import com.iqbal.journal.data.toEntity
import javax.inject.Inject

/** Menyimpan nama penulis catatan di HP agar daftar catatan tetap lengkap saat offline. */
class AuthorCache @Inject constructor(
    private val profileRemote: ProfileRemoteDataSource,
    private val authorDao: AuthorDao
) {
    /** Ambil ulang nama semua [ids]; nama bisa berubah di Pengaturan. */
    suspend fun refresh(ids: Set<String>) {
        if (ids.isEmpty()) return
        authorDao.upsertAll(profileRemote.fetchAuthors(ids).map { it.toEntity() })
    }

    /** Ambil nama penulis yang belum dikenal; kegagalan diabaikan karena catatan tetap bisa tampil tanpa nama. */
    suspend fun ensureKnown(id: String) {
        if (!authorDao.exists(id)) outcomeOf { refresh(setOf(id)) }
    }
}
