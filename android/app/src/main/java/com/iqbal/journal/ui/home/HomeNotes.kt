package com.iqbal.journal.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.ui.components.NoteCard
import com.iqbal.journal.ui.theme.FabShape
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

// Bagian daftar catatan di Beranda.

private const val NOTE_COLUMNS = 2

@Composable
internal fun NotesHeader() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.large)
    ) {
        Text(stringResource(R.string.home_notes_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.home_notes_order),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun NotesGrid(
    notes: List<Note>,
    myUserId: String?,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(NOTE_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        verticalItemSpacing = Spacing.cardGap,
        // Ruang bawah agar kartu terakhir tidak tertutup FAB.
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            bottom = Sizes.fabHeight + Spacing.extraLarge
        ),
        modifier = modifier
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                isMine = note.authorId == myUserId,
                onClick = { onNoteClick(note) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
internal fun WriteButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        shape = FabShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
        text = { Text(stringResource(R.string.home_write), style = MaterialTheme.typography.labelLarge) },
        modifier = Modifier.height(Sizes.fabHeight)
    )
}

@Composable
internal fun EmptyNotes(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.extraLarge)
    ) {
        Text(stringResource(R.string.home_notes_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.home_notes_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
