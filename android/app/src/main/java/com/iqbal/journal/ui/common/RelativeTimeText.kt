package com.iqbal.journal.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.iqbal.journal.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ShortDateFormat = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id"))

@Composable
fun relativeTimeText(instant: Instant): String =
    when (val time = relativeTime(instant, Instant.now(), ZoneId.systemDefault())) {
        RelativeTime.JustNow -> stringResource(R.string.time_just_now)
        is RelativeTime.Minutes -> stringResource(R.string.time_minutes, time.count)
        is RelativeTime.Hours -> stringResource(R.string.time_hours, time.count)
        RelativeTime.Yesterday -> stringResource(R.string.time_yesterday)
        is RelativeTime.Days -> stringResource(R.string.time_days, time.count)
        is RelativeTime.Date -> ShortDateFormat.format(time.date)
    }
