package com.iqbal.journal.ui.common

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Waktu relatif singkat untuk meta catatan ("5 mnt", "kemarin"). */
sealed interface RelativeTime {
    data object JustNow : RelativeTime
    data class Minutes(val count: Long) : RelativeTime
    data class Hours(val count: Long) : RelativeTime
    data object Yesterday : RelativeTime
    data class Days(val count: Long) : RelativeTime
    data class Date(val date: LocalDate) : RelativeTime
}

/** Lebih lama dari ini, tanggal ditampilkan apa adanya. */
private const val MAX_DAYS_AS_COUNT = 6

fun relativeTime(then: Instant, now: Instant, zone: ZoneId): RelativeTime {
    val elapsed = Duration.between(then, now)
    return when {
        elapsed < Duration.ofMinutes(1) -> RelativeTime.JustNow

        elapsed < Duration.ofHours(1) -> RelativeTime.Minutes(elapsed.toMinutes())

        elapsed < Duration.ofDays(1) -> RelativeTime.Hours(elapsed.toHours())

        else -> {
            val thenDate = then.atZone(zone).toLocalDate()
            val days = ChronoUnit.DAYS.between(thenDate, now.atZone(zone).toLocalDate())
            when {
                days <= 1 -> RelativeTime.Yesterday
                days <= MAX_DAYS_AS_COUNT -> RelativeTime.Days(days)
                else -> RelativeTime.Date(thenDate)
            }
        }
    }
}
