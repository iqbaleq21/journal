package com.iqbal.journal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.ui.common.relativeTimeText
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import com.iqbal.journal.ui.theme.swatchFor
import java.time.Instant

/** Catatan pendek tampil besar seperti kertas tempel; yang panjang memakai teks biasa. */
private const val SHORT_NOTE_MAX_LENGTH = 40
private const val PREVIEW_MAX_LINES = 8
private const val AVATAR_ALPHA = 0.12f

/** Kartu catatan di grid Beranda. [isMine] mengganti nama penulis dengan "Kamu". */
@Composable
fun NoteCard(note: Note, isMine: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val swatch = JournalTheme.noteColors.swatchFor(note.color)
    val author = when {
        isMine -> stringResource(R.string.note_author_you)
        else -> note.authorName ?: stringResource(R.string.note_author_unknown)
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = swatch.container,
        border = if (note.color == NoteColor.Plain) {
            BorderStroke(Sizes.outlineThin, MaterialTheme.colorScheme.outline)
        } else {
            null
        },
        modifier = modifier
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
            Text(
                text = note.content,
                style = if (note.content.length <= SHORT_NOTE_MAX_LENGTH) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Sizes.avatarSmall)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = AVATAR_ALPHA), CircleShape)
                ) {
                    Text(author.take(1).uppercase(), style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    author,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    stringResource(R.string.note_meta_separator, relativeTimeText(note.updatedAt)),
                    style = MaterialTheme.typography.labelMedium,
                    color = swatch.onContainerMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview
@Composable
private fun NoteCardPreview() {
    val note = Note(
        id = "n1",
        groupId = "g1",
        authorId = "a",
        authorName = "Ibu",
        content = "Jemput Kiara jam 3 sore, Kamis ini Ayah ya.",
        color = NoteColor.Sky,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
    JournalTheme { NoteCard(note = note, isMine = false, onClick = {}) }
}
