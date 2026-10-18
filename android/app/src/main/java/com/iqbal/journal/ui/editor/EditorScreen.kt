package com.iqbal.journal.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.validation.NoteRules
import com.iqbal.journal.ui.common.messageRes
import com.iqbal.journal.ui.common.relativeTimeText
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.NoteSwatch
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import com.iqbal.journal.ui.theme.swatchFor
import java.time.Instant

@Composable
fun EditorRoute(onDone: () -> Unit, onBack: () -> Unit, viewModel: EditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    EditorScreen(
        state = state,
        onBack = onBack,
        onContentChange = viewModel::onContentChange,
        onColorChange = viewModel::onColorChange,
        onSave = viewModel::onSave,
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onErrorShown = viewModel::onErrorShown
    )
}

@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onContentChange: (String) -> Unit,
    onColorChange: (NoteColor) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(context.getString(it.messageRes()))
            onErrorShown()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
        ) {
            TopBar(state = state, onBack = onBack, onSave = onSave)
            NoteEditorCard(state = state, onContentChange = onContentChange, modifier = Modifier.weight(1f))
            if (state.canEdit) NoteColorPicker(selected = state.color, onSelect = onColorChange)
            if (!state.isMine) {
                Text(
                    stringResource(R.string.editor_read_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.canDelete) {
                DeleteButton(
                    isDeleting = state.pendingAction == EditorAction.Delete,
                    onClick = onDeleteClick
                )
            }
        }
    }

    if (state.isDeleteConfirmOpen) DeleteDialog(onConfirm = onDeleteConfirm, onDismiss = onDeleteDismiss)
}

@Composable
private fun TopBar(state: EditorUiState, onBack: () -> Unit, onSave: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(Sizes.touchTarget)) {
            Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.common_back))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (state.groupName.isNotEmpty()) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        state.groupName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)
                    )
                }
            }
        }
        if (state.canEdit) {
            Button(onClick = onSave, enabled = !state.isBusy, modifier = Modifier.height(Sizes.touchTarget)) {
                if (state.pendingAction == EditorAction.Save) {
                    CircularProgressIndicator(
                        strokeWidth = Sizes.outlineThick,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Sizes.progress)
                    )
                } else {
                    Text(stringResource(R.string.editor_save), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            // Penyeimbang agar label grup tetap di tengah.
            Spacer(Modifier.size(Sizes.touchTarget))
        }
    }
}

@Composable
private fun NoteEditorCard(state: EditorUiState, onContentChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val swatch = JournalTheme.noteColors.swatchFor(state.color)
    val focusRequester = remember { FocusRequester() }
    // Catatan baru langsung siap diketik.
    LaunchedEffect(state.isNew) { if (state.isNew) focusRequester.requestFocus() }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = swatch.container,
        border = if (state.color == NoteColor.Plain) {
            BorderStroke(Sizes.outlineThin, MaterialTheme.colorScheme.outline)
        } else {
            null
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
            Text(
                stringResource(R.string.editor_label),
                style = MaterialTheme.typography.labelSmall,
                color = swatch.onContainerMuted
            )
            BasicTextField(
                value = state.content,
                onValueChange = onContentChange,
                readOnly = !state.canEdit,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { innerTextField ->
                    if (state.content.isEmpty()) {
                        Text(
                            stringResource(R.string.editor_placeholder),
                            style = MaterialTheme.typography.headlineMedium,
                            color = swatch.onContainerMuted
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            if (state.showEmptyError) {
                Text(
                    stringResource(R.string.editor_empty_error),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            EditorMeta(state = state, swatch = swatch)
        }
    }
}

@Composable
private fun EditorMeta(state: EditorUiState, swatch: NoteSwatch) {
    val updatedAt = state.updatedAt
    val meta = when {
        state.isNew || updatedAt == null -> stringResource(R.string.editor_new)

        state.isMine -> stringResource(R.string.editor_by_you, relativeTimeText(updatedAt))

        else -> stringResource(
            R.string.editor_by,
            state.authorName ?: stringResource(R.string.note_author_unknown),
            relativeTimeText(updatedAt)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), verticalAlignment = Alignment.CenterVertically) {
        Text(
            meta,
            style = MaterialTheme.typography.labelMedium,
            color = swatch.onContainerMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (state.canEdit) {
            Text(
                stringResource(R.string.editor_counter, state.content.length, NoteRules.MAX_NOTE_LENGTH),
                style = MaterialTheme.typography.labelMedium,
                color = swatch.onContainerMuted
            )
        }
    }
}

@Composable
private fun DeleteButton(isDeleting: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isDeleting,
        border = BorderStroke(Sizes.outlineThin, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.buttonHeight)
    ) {
        if (isDeleting) {
            CircularProgressIndicator(
                strokeWidth = Sizes.outlineThick,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Sizes.progress)
            )
        } else {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null, Modifier.size(Sizes.progress))
            Spacer(Modifier.size(Spacing.small))
            Text(stringResource(R.string.editor_delete), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DeleteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_delete_title)) },
        text = { Text(stringResource(R.string.editor_delete_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.editor_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    JournalTheme {
        EditorScreen(
            state = EditorUiState(
                groupName = "Rumah",
                content = "Kode wifi rumah: rumahbahagia2026",
                color = NoteColor.Butter,
                isNew = false,
                updatedAt = Instant.now()
            ),
            onBack = {},
            onContentChange = {},
            onColorChange = {},
            onSave = {},
            onDeleteClick = {},
            onDeleteDismiss = {},
            onDeleteConfirm = {},
            onErrorShown = {}
        )
    }
}
