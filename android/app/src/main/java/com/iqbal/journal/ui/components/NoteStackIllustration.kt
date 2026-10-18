package com.iqbal.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iqbal.journal.R
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Spacing

private val IllustrationHeight = 320.dp
private val BackdropSize = 280.dp
private val CardElevation = 6.dp

private data class StackedNote(
    val textRes: Int,
    val width: Dp,
    val x: Dp,
    val y: Dp,
    val rotation: Float,
    val color: @Composable () -> Color,
    val style: @Composable () -> TextStyle
)

private val StackedNotes = listOf(
    StackedNote(
        R.string.illustration_note_1,
        width = 180.dp,
        x = (-60).dp,
        y = (-70).dp,
        rotation = -8f,
        color = { JournalTheme.noteColors.sky.container },
        style = { MaterialTheme.typography.bodyMedium }
    ),
    StackedNote(
        R.string.illustration_note_2,
        width = 190.dp,
        x = 50.dp,
        y = 10.dp,
        rotation = 6f,
        color = { JournalTheme.noteColors.butter.container },
        style = { MaterialTheme.typography.headlineSmall }
    ),
    StackedNote(
        R.string.illustration_note_3,
        width = 150.dp,
        x = (-50).dp,
        y = 100.dp,
        rotation = -3f,
        color = { JournalTheme.noteColors.plain.container },
        style = { MaterialTheme.typography.titleSmall }
    )
)

/** Ilustrasi dekoratif tumpukan catatan untuk layar Kenalan. */
@Composable
fun NoteStackIllustration(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(IllustrationHeight)
            .clearAndSetSemantics {}
    ) {
        Box(
            Modifier
                .size(BackdropSize)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        )
        StackedNotes.forEach { note ->
            Surface(
                color = note.color(),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = CardElevation,
                modifier = Modifier
                    .offset(note.x, note.y)
                    .rotate(note.rotation)
                    .width(note.width)
            ) {
                Text(
                    text = stringResource(note.textRes),
                    style = note.style(),
                    modifier = Modifier.padding(Spacing.medium)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteStackIllustrationPreview() {
    JournalTheme { NoteStackIllustration() }
}
