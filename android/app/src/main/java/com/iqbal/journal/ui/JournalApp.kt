package com.iqbal.journal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.iqbal.journal.R
import com.iqbal.journal.ui.components.Wordmark
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Spacing

/** Akar UI. Navigasi antar layar ditambahkan mulai M1. */
@Composable
fun JournalApp() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically)
        ) {
            Wordmark(style = MaterialTheme.typography.displayLarge)
            Text(
                text = stringResource(R.string.setup_headline),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.setup_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalAppPreview() {
    JournalTheme { JournalApp() }
}
