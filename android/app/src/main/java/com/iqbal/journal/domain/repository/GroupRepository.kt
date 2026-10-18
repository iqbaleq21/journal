package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Outcome
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    /** Grup yang saya ikuti, dari cache lokal, diurutkan berdasarkan nama. */
    val groups: Flow<List<Group>>

    /** Grup yang sedang dipilih di Beranda; jatuh ke grup pertama bila belum pernah memilih. */
    val selectedGroupId: Flow<String?>

    suspend fun selectGroup(groupId: String)

    /** Ambil ulang daftar grup dari server ke cache lokal. */
    suspend fun refresh(): Outcome<Unit>

    suspend fun createGroup(name: String): Outcome<Group>

    suspend fun joinGroup(inviteCode: String): Outcome<Group>
}
