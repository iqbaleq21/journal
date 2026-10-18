package com.iqbal.journal.ui.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import com.iqbal.journal.ui.theme.swatchFor

// Ukuran dari mockup Editor: lingkaran 48 dp, ring terpilih 3 dp dengan celah sewarna latar.
private val SwatchSize = 48.dp
private val SelectedRing = 3.dp

/** Pemilih warna latar catatan (US-15); warnanya ikut tampil di widget. */
@Composable
internal fun NoteColorPicker(selected: NoteColor, onSelect: (NoteColor) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        Text(stringResource(R.string.editor_color), style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            NoteColor.entries.forEach { color ->
                ColorSwatch(color = color, isSelected = color == selected, onClick = { onSelect(color) })
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: NoteColor, isSelected: Boolean, onClick: () -> Unit) {
    val label = stringResource(color.labelRes())
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .border(
                width = if (isSelected) SelectedRing else Sizes.outlineThin,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = label }
    ) {
        Surface(
            shape = CircleShape,
            color = JournalTheme.noteColors.swatchFor(color).container,
            modifier = Modifier
                .padding(if (isSelected) SelectedRing * 2 else Sizes.outlineThin)
                .size(SwatchSize)
        ) {}
    }
}

private fun NoteColor.labelRes(): Int = when (this) {
    NoteColor.Butter -> R.string.color_butter
    NoteColor.Mint -> R.string.color_mint
    NoteColor.Sky -> R.string.color_sky
    NoteColor.Peach -> R.string.color_peach
    NoteColor.Lilac -> R.string.color_lilac
    NoteColor.Plain -> R.string.color_plain
}
