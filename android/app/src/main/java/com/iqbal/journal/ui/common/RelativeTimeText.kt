package com.iqbal.journal.ui.common

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.iqbal.journal.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Dipakai juga oleh widget, yang tidak punya akses ke `stringResource`. */
fun Context.formatRelativeTime(instant: Instant): String =
    when (val time = relativeTime(instant, Instant.now(), ZoneId.systemDefault())) {
        RelativeTime.JustNow -> getString(R.string.time_just_now)

        is RelativeTime.Minutes -> getString(R.string.time_minutes, time.count)

        is RelativeTime.Hours -> getString(R.string.time_hours, time.count)

        RelativeTime.Yesterday -> getString(R.string.time_yesterday)

        is RelativeTime.Days -> getString(R.string.time_days, time.count)

        // Nama bulan mengikuti bahasa app (Pengaturan), bukan bahasa sistem.
        is RelativeTime.Date -> DateTimeFormatter.ofPattern(
            "d MMM",
            resources.configuration.locales[0]
        ).format(time.date)
    }

@Composable
fun relativeTimeText(instant: Instant): String = LocalContext.current.formatRelativeTime(instant)
