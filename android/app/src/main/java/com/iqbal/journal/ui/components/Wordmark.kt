package com.iqbal.journal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/** Logo teks "jOURnal": bagian "OUR" diberi warna primary. */
@Composable
fun Wordmark(style: TextStyle, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primaryContainer
    val onAccent = MaterialTheme.colorScheme.onPrimaryContainer
    Text(
        text = buildAnnotatedString {
            append("j")
            withStyle(SpanStyle(color = onAccent, background = accent)) { append("OUR") }
            append("nal")
        },
        style = style,
        modifier = modifier
    )
}
