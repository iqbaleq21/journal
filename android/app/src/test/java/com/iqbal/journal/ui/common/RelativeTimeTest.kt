package com.iqbal.journal.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val now = Instant.parse("2026-10-18T05:00:00Z") // 12.00 WIB

    private fun ago(value: String) = relativeTime(Instant.parse(value), now, zone)

    @Test
    fun `kurang dari satu jam ditampilkan dalam menit`() {
        assertEquals(RelativeTime.JustNow, ago("2026-10-18T04:59:30Z"))
        assertEquals(RelativeTime.Minutes(5), ago("2026-10-18T04:55:00Z"))
    }

    @Test
    fun `kurang dari sehari ditampilkan dalam jam`() {
        assertEquals(RelativeTime.Hours(2), ago("2026-10-18T03:00:00Z"))
        assertEquals(RelativeTime.Hours(23), ago("2026-10-17T05:30:00Z"))
    }

    @Test
    fun `lebih dari sehari memakai tanggal kalender di zona pengguna`() {
        assertEquals(RelativeTime.Yesterday, ago("2026-10-17T04:00:00Z"))
        assertEquals(RelativeTime.Days(3), ago("2026-10-15T05:00:00Z"))
        assertEquals(RelativeTime.Date(LocalDate.of(2026, 10, 1)), ago("2026-10-01T05:00:00Z"))
    }

    @Test
    fun `jam HP yang sedikit tertinggal dari server tidak menghasilkan waktu negatif`() {
        assertEquals(RelativeTime.JustNow, ago("2026-10-18T05:00:20Z"))
    }
}
