package com.iqbal.journal.ui.widgetpicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.ui.common.relativeTimeText
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import com.iqbal.journal.ui.theme.swatchFor
import java.time.Instant

// Ukuran preview mengikuti widget 2×2 di mockup.
private val WidgetPreviewSize = 168.dp
private val ColorSwatchSize = 36.dp
private const val PREVIEW_MAX_LINES = 4

@Composable
fun WidgetPickerRoute(
    onDone: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    viewModel: WidgetPickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    WidgetPickerScreen(
        state = state,
        onClose = onClose,
        onOpenApp = onOpenApp,
        onSelectGroup = viewModel::onSelectGroup,
        onSelectNote = viewModel::onSelectNote,
        onSave = viewModel::onSave
    )
}

@Composable
fun WidgetPickerScreen(
    state: WidgetPickerUiState,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onSelectNote: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)
            ) {
                FilledTonalIconButton(onClick = onClose, modifier = Modifier.size(Sizes.touchTarget)) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.widget_picker_close))
                }
                Text(stringResource(R.string.widget_picker_title), style = MaterialTheme.typography.titleMedium)
            }
            when {
                !state.hasProfile -> NeedsApp(onOpenApp = onOpenApp, modifier = Modifier.weight(1f))

                state.isLoading -> Box(Modifier.weight(1f))

                else -> {
                    WidgetPreview(
                        note = state.selectedNote,
                        groupName = state.selectedGroup?.name.orEmpty(),
                        myUserId = state.myUserId
                    )
                    GroupChips(groups = state.groups, selectedGroupId = state.selectedGroupId, onSelect = onSelectGroup)
                    NoteList(
                        notes = state.notes,
                        selectedNoteId = state.selectedNoteId,
                        myUserId = state.myUserId,
                        onSelect = onSelectNote,
                        modifier = Modifier.weight(1f)
                    )
                    SaveButton(state = state, onSave = onSave)
                }
            }
        }
    }
}

@Composable
private fun WidgetPreview(note: Note?, groupName: String, myUserId: String?) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.shapes.large)
            .padding(Spacing.large)
    ) {
        val swatch = JournalTheme.noteColors.swatchFor(note?.color ?: NoteColor.Plain)
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .size(WidgetPreviewSize)
                .background(swatch.container, MaterialTheme.shapes.large)
                .padding(Spacing.medium)
        ) {
            Text(
                note?.content ?: stringResource(R.string.widget_picker_preview_empty),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis
            )
            if (note != null) {
                Text(
                    stringResource(R.string.widget_meta, authorLabel(note, myUserId), groupName),
                    style = MaterialTheme.typography.labelSmall,
                    color = swatch.onContainerMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GroupChips(groups: List<Group>, selectedGroupId: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        items(groups, key = { it.id }) { group ->
            FilterChip(
                selected = group.id == selectedGroupId,
                onClick = { onSelect(group.id) },
                label = { Text(group.name, style = MaterialTheme.typography.titleSmall) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.inverseOnSurface
                ),
                modifier = Modifier.height(Sizes.chipHeight)
            )
        }
    }
}

@Composable
private fun NoteList(
    notes: List<Note>,
    selectedNoteId: String?,
    myUserId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        if (notes.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(Spacing.large)) {
                Text(
                    stringResource(R.string.widget_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Surface
        }
        LazyColumn(contentPadding = PaddingValues(Spacing.small), modifier = Modifier.selectableGroup()) {
            items(notes, key = { it.id }) { note ->
                NoteRow(note = note, isSelected = note.id == selectedNoteId, myUserId = myUserId, onClick = {
                    onSelect(note.id)
                })
            }
        }
    }
}

@Composable
private fun NoteRow(note: Note, isSelected: Boolean, myUserId: String?, onClick: () -> Unit) {
    val swatch = JournalTheme.noteColors.swatchFor(note.color)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.medium
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = Spacing.medium, vertical = Spacing.cardGap)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = swatch.container,
            border = if (note.color ==
                NoteColor.Plain
            ) {
                BorderStroke(Sizes.outlineThin, MaterialTheme.colorScheme.outline)
            } else {
                null
            },
            modifier = Modifier.size(ColorSwatchSize)
        ) {}
        Column(Modifier.weight(1f)) {
            Text(
                note.content,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.widget_meta, authorLabel(note, myUserId), relativeTimeText(note.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Klik ditangani baris agar target sentuh selebar baris.
        RadioButton(selected = isSelected, onClick = null)
    }
}

@Composable
private fun SaveButton(state: WidgetPickerUiState, onSave: () -> Unit) {
    Button(
        onClick = onSave,
        enabled = state.canSave,
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.buttonHeight)
    ) {
        if (state.isSaving) {
            CircularProgressIndicator(
                strokeWidth = Sizes.outlineThick,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Sizes.progress)
            )
        } else {
            Text(
                stringResource(
                    if (state.isReconfiguring) R.string.widget_picker_change else R.string.widget_picker_add
                ),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun NeedsApp(onOpenApp: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium, Alignment.CenterVertically),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(R.string.widget_needs_profile),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Button(onClick = onOpenApp) { Text(stringResource(R.string.widget_picker_open_app)) }
    }
}

@Composable
private fun authorLabel(note: Note, myUserId: String?): String = when {
    note.authorId == myUserId -> stringResource(R.string.note_author_you)
    else -> note.authorName ?: stringResource(R.string.note_author_unknown)
}

@Preview(showBackground = true)
@Composable
private fun WidgetPickerScreenPreview() {
    val notes = listOf(
        Note(
            "n1",
            "g1",
            "me",
            "Ayah",
            "Kode wifi rumah: rumahbahagia2026",
            NoteColor.Butter,
            Instant.now(),
            Instant.now()
        ),
        Note(
            "n2",
            "g1",
            "ibu",
            "Ibu",
            "Jemput Kiara jam 3 sore, Kamis ini Ayah ya.",
            NoteColor.Sky,
            Instant.now(),
            Instant.now()
        )
    )
    JournalTheme {
        WidgetPickerScreen(
            state = WidgetPickerUiState(
                isLoading = false,
                groups = listOf(Group("g1", "Rumah", "me", memberCount = 4, inviteCode = null, inviteExpiresAt = null)),
                selectedGroupId = "g1",
                notes = notes,
                selectedNoteId = "n1",
                myUserId = "me"
            ),
            onClose = {},
            onOpenApp = {},
            onSelectGroup = {},
            onSelectNote = {},
            onSave = {}
        )
    }
}
