package com.iqbal.journal.data

import com.iqbal.journal.data.remote.CountDto
import com.iqbal.journal.data.remote.GroupDto
import com.iqbal.journal.data.remote.mapServerMessage
import com.iqbal.journal.domain.model.JournalError
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMappingTest {
    @Test
    fun `kode error dari RPC dipetakan ke JournalError`() {
        assertEquals(JournalError.InviteNotFound, mapServerMessage("INVITE_NOT_FOUND"))
        assertEquals(JournalError.GroupFull, mapServerMessage("ERROR: GROUP_FULL (SQLSTATE P0001)"))
        assertEquals(JournalError.AlreadyMember, mapServerMessage("ALREADY_MEMBER"))
    }

    @Test
    fun `pesan yang tidak dikenal menjadi Unknown`() {
        assertEquals(JournalError.Unknown, mapServerMessage("permission denied for table notes"))
        assertEquals(JournalError.Unknown, mapServerMessage(null))
    }
}

class GroupMappingTest {
    @Test
    fun `dto dari PostgREST dipetakan ke domain lewat entity`() {
        val dto = GroupDto(
            id = "g1",
            name = "Rumah",
            ownerId = "me",
            inviteCode = "K7QM4X",
            inviteExpiresAt = "2026-10-25T08:00:00.123456+00:00",
            members = listOf(CountDto(4))
        )

        val group = dto.toEntity().toDomain()

        assertEquals(4, group.memberCount)
        assertEquals(Instant.parse("2026-10-25T08:00:00.123Z"), group.inviteExpiresAt)
    }

    @Test
    fun `grup tanpa undangan dan tanpa embed anggota tetap aman`() {
        val group = GroupDto(id = "g1", name = "Rumah", ownerId = "me").toEntity().toDomain()

        assertEquals(0, group.memberCount)
        assertNull(group.inviteCode)
        assertNull(group.inviteExpiresAt)
    }
}
