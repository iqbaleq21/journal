package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Member
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

    /** Anggota sebuah grup dari cache lokal, urut dari yang paling lama bergabung. */
    fun members(groupId: String): Flow<List<Member>>

    suspend fun leaveGroup(groupId: String): Outcome<Unit>

    /** Hanya pemilik grup. */
    suspend fun removeMember(groupId: String, userId: String): Outcome<Unit>

    /** Hanya pemilik grup. */
    suspend fun renameGroup(groupId: String, name: String): Outcome<Unit>

    /** Hanya pemilik grup; kode lama langsung tidak berlaku. */
    suspend fun regenerateInvite(groupId: String): Outcome<Unit>

    suspend fun setNotify(groupId: String, enabled: Boolean): Outcome<Unit>
}
