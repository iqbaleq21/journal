package com.iqbal.journal.ui.common

import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

private const val MINUTES_PER_DAY = 24 * 60

/** Sisa hari (dibulatkan ke atas) sampai kode undangan kedaluwarsa; 0 jika sudah lewat. */
fun inviteDaysLeft(expiresAt: Instant, now: Instant): Int {
    val minutesLeft = Duration.between(now, expiresAt).toMinutes()
    return if (minutesLeft <= 0) 0 else ceil(minutesLeft.toDouble() / MINUTES_PER_DAY).toInt()
}
