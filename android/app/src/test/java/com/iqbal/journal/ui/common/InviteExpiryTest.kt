package com.iqbal.journal.ui.common

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class InviteExpiryTest {
    private val now = Instant.parse("2026-10-18T10:00:00Z")

    @Test
    fun `sisa hari dibulatkan ke atas`() {
        assertEquals(7, inviteDaysLeft(now.plus(Duration.ofDays(7)), now))
        assertEquals(7, inviteDaysLeft(now.plus(Duration.ofDays(6).plusHours(1)), now))
        assertEquals(1, inviteDaysLeft(now.plus(Duration.ofMinutes(5)), now))
    }

    @Test
    fun `kode yang sudah lewat bernilai nol`() {
        assertEquals(0, inviteDaysLeft(now, now))
        assertEquals(0, inviteDaysLeft(now.minus(Duration.ofDays(1)), now))
    }
}
