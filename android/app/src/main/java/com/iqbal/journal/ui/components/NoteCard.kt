package com.iqbal.journal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.ui.common.relativeTimeText
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import com.iqbal.journal.ui.theme.hardShadow
import com.iqbal.journal.ui.theme.swatchFor
import java.time.Instant

/** Catatan pendek tampil besar seperti kertas tempel; yang panjang memakai teks biasa. */
private const val SHORT_NOTE_MAX_LENGTH = 40
private const val PREVIEW_MAX_LINES = 8
private const val TITLE_MAX_LINES = 2
private const val AVATAR_ALPHA = 0.12f
private const val BADGE_ALPHA = 0.09f

/**
 * Kartu catatan di grid Beranda. [isMine] mengganti nama penulis dengan "Kamu".
 * [isSelected] non-null = mode pilih (US-23): kartu menandai apakah ikut dipilih.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isMine: Boolean,
    isOnWidget: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean? = null,
    onLongClick: (() -> Unit)? = null
) {
    val swatch = JournalTheme.noteColors.swatchFor(note.color)
    val author = when {
        isMine -> stringResource(R.string.note_author_you)
        else -> note.authorName ?: stringResource(R.string.note_author_unknown)
    }
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = swatch.container,
        border = if (isSelected == true) {
            BorderStroke(Sizes.outlineThick * 2, MaterialTheme.colorScheme.primary)
        } else {
            JournalTheme.cardStyle.border
        },
        modifier = modifier
            .hardShadow(shape)
            .clip(shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(if (isSelected != null) Modifier.semantics { selected = isSelected } else Modifier)
    ) {
        Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
            if (isOnWidget || isSelected == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOnWidget) WidgetBadge()
                    Spacer(Modifier.weight(1f))
                    if (isSelected == true) SelectedMark()
                }
            }
            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = note.previewText,
                // Dengan judul, isi selalu teks biasa agar judul tetap yang paling menonjol.
                style = if (note.title.isEmpty() && note.previewText.length <= SHORT_NOTE_MAX_LENGTH) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            if (note.isPending) {
                // Baris sendiri agar tidak memotong nama penulis di kartu yang sempit.
                Text(
                    stringResource(R.string.note_pending),
                    style = MaterialTheme.typography.labelSmall,
                    color = swatch.onContainerMuted
                )
            }
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

@Composable
private fun SelectedMark() {
    Icon(
        painterResource(R.drawable.ic_check),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .padding(Spacing.extraSmall)
            .size(Sizes.badgeIcon)
    )
}

@Composable
private fun WidgetBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = BADGE_ALPHA), CircleShape)
            .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall)
    ) {
        Icon(painterResource(R.drawable.ic_widget), contentDescription = null, Modifier.size(Sizes.badgeIcon))
        Text(stringResource(R.string.widget_badge), style = MaterialTheme.typography.labelSmall)
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
        title = "Les Kiara",
        content = "Jemput Kiara jam 3 sore, Kamis ini Ayah ya.",
        color = NoteColor.Sky,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )
    JournalTheme { NoteCard(note = note, isMine = false, isOnWidget = true, onClick = {}) }
}
