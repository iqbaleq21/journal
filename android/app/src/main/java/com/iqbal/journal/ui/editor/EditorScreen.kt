package com.iqbal.journal.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteKind
import com.iqbal.journal.domain.validation.NoteRules
import com.iqbal.journal.ui.common.messageRes
import com.iqbal.journal.ui.common.relativeTimeText
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.NoteSwatch
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import com.iqbal.journal.ui.theme.hardShadow
import com.iqbal.journal.ui.theme.swatchFor
import java.time.Instant

@Composable
fun EditorRoute(onDone: () -> Unit, viewModel: EditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDone) { if (state.isDone) onDone() }
    // Tanpa tombol Simpan: kembali (tombol atau gestur) menyimpan perubahan terakhir dulu.
    BackHandler(onBack = viewModel::onLeave)
    EditorScreen(
        state = state,
        onBack = viewModel::onLeave,
        onTitleChange = viewModel::onTitleChange,
        onContentChange = viewModel::onContentChange,
        onColorChange = viewModel::onColorChange,
        onKindChange = viewModel::onKindChange,
        onSharedChange = viewModel::onSharedChange,
        onSettingsClick = viewModel::onSettingsClick,
        onSettingsDismiss = viewModel::onSettingsDismiss,
        checklist = ChecklistCallbacks(
            onTextChange = viewModel::onItemTextChange,
            onCheckedChange = viewModel::onItemCheckedChange,
            onAdd = viewModel::onAddItem,
            onRemove = viewModel::onRemoveItem
        ),
        onDeleteClick = viewModel::onDeleteClick,
        onDeleteDismiss = viewModel::onDeleteDismiss,
        onDeleteConfirm = viewModel::onDeleteConfirm,
        onErrorShown = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onColorChange: (NoteColor) -> Unit,
    onKindChange: (NoteKind) -> Unit,
    onSharedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsDismiss: () -> Unit,
    checklist: ChecklistCallbacks,
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
                // Tanpa ini inset nav bar dihitung dua kali saat keyboard muncul.
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
        ) {
            TopBar(state = state, onBack = onBack, onSettingsClick = onSettingsClick)
            NoteEditorCard(
                state = state,
                onTitleChange = onTitleChange,
                onContentChange = onContentChange,
                checklist = checklist,
                modifier = Modifier.weight(1f)
            )
            // Saat mengetik, seluruh ruang di atas keyboard dipakai untuk isi catatan.
            if (state.canEdit && !WindowInsets.isImeVisible) {
                NoteColorPicker(selected = state.color, onSelect = onColorChange)
            }
            if (!state.canEdit && !state.isLoading) {
                Text(
                    stringResource(R.string.editor_read_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (state.isSettingsOpen) {
        NoteSettingsSheet(
            state = state,
            onKindChange = onKindChange,
            onSharedChange = onSharedChange,
            onDeleteClick = onDeleteClick,
            onDismiss = onSettingsDismiss
        )
    }
    if (state.isDeleteConfirmOpen) DeleteDialog(onConfirm = onDeleteConfirm, onDismiss = onDeleteDismiss)
}

@Composable
private fun TopBar(state: EditorUiState, onBack: () -> Unit, onSettingsClick: () -> Unit) {
    val focusManager = LocalFocusManager.current
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
            FilledTonalIconButton(
                onClick = {
                    // Tutup keyboard agar lembar pengaturan tidak tertimpa.
                    focusManager.clearFocus()
                    onSettingsClick()
                },
                modifier = Modifier.size(Sizes.touchTarget)
            ) {
                Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.editor_settings))
            }
        } else {
            // Penyeimbang agar label grup tetap di tengah.
            Spacer(Modifier.size(Sizes.touchTarget))
        }
    }
}

/** Aksi item checklist dari Editor, dikelompokkan agar daftar parameter layar tetap pendek. */
data class ChecklistCallbacks(
    val onTextChange: (index: Int, text: String) -> Unit,
    val onCheckedChange: (index: Int, checked: Boolean) -> Unit,
    val onAdd: () -> Unit,
    val onRemove: (index: Int) -> Unit
)

@Composable
private fun NoteEditorCard(
    state: EditorUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    checklist: ChecklistCallbacks,
    modifier: Modifier = Modifier
) {
    val swatch = JournalTheme.noteColors.swatchFor(state.color)
    val focusRequester = remember { FocusRequester() }
    // Catatan baru langsung siap diketik, mulai dari judul.
    LaunchedEffect(state.isNew) { if (state.isNew) focusRequester.requestFocus() }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = swatch.container,
        border = JournalTheme.cardStyle.border,
        modifier = modifier
            .fillMaxWidth()
            .hardShadow(MaterialTheme.shapes.extraLarge)
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
            Text(
                stringResource(R.string.editor_label),
                style = MaterialTheme.typography.labelSmall,
                color = swatch.onContainerMuted
            )
            TitleField(
                title = state.title,
                canEdit = state.canEdit,
                mutedColor = swatch.onContainerMuted,
                onTitleChange = onTitleChange,
                modifier = Modifier.focusRequester(focusRequester)
            )
            if (state.kind == NoteKind.Checklist) {
                ChecklistEditor(
                    items = state.items,
                    canEdit = state.canEdit,
                    mutedColor = swatch.onContainerMuted,
                    onTextChange = checklist.onTextChange,
                    onCheckedChange = checklist.onCheckedChange,
                    onAdd = checklist.onAdd,
                    onRemove = checklist.onRemove,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                BasicTextField(
                    value = state.content,
                    onValueChange = onContentChange,
                    readOnly = !state.canEdit,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { innerTextField ->
                        if (state.content.isEmpty()) {
                            Text(
                                stringResource(R.string.editor_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = swatch.onContainerMuted
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            EditorMeta(state = state, swatch = swatch)
        }
    }
}

/** Judul opsional; tidak ditampilkan pada catatan orang lain yang tanpa judul. */
@Composable
private fun TitleField(
    title: String,
    canEdit: Boolean,
    mutedColor: Color,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!canEdit && title.isEmpty()) return
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        readOnly = !canEdit,
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        decorationBox = { innerTextField ->
            if (title.isEmpty()) {
                Text(
                    stringResource(R.string.editor_title_placeholder),
                    style = MaterialTheme.typography.headlineLarge,
                    color = mutedColor
                )
            }
            innerTextField()
        },
        modifier = modifier.fillMaxWidth()
    )
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
                stringResource(R.string.editor_counter, state.body.length, NoteRules.MAX_NOTE_LENGTH),
                style = MaterialTheme.typography.labelMedium,
                color = swatch.onContainerMuted
            )
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
                title = "Wifi",
                content = "Kode wifi rumah: rumahbahagia2026",
                color = NoteColor.Butter,
                isNew = false,
                updatedAt = Instant.now()
            ),
            onBack = {},
            onTitleChange = {},
            onContentChange = {},
            onColorChange = {},
            onKindChange = {},
            onSharedChange = {},
            onSettingsClick = {},
            onSettingsDismiss = {},
            checklist = ChecklistCallbacks({ _, _ -> }, { _, _ -> }, {}, {}),
            onDeleteClick = {},
            onDeleteDismiss = {},
            onDeleteConfirm = {},
            onErrorShown = {}
        )
    }
}
