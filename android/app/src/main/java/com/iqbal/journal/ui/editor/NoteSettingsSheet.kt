package com.iqbal.journal.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.NoteKind
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

/** Pengaturan per catatan: jenis, akses edit, dan hapus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoteSettingsSheet(
    state: EditorUiState,
    onKindChange: (NoteKind) -> Unit,
    onSharedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
            modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.large)
        ) {
            Text(stringResource(R.string.editor_settings), style = MaterialTheme.typography.titleLarge)
            KindSelector(selected = state.kind, onSelect = onKindChange)
            if (state.isMine) ShareSwitch(shared = state.shared, onChange = onSharedChange)
            if (state.canDelete) {
                DeleteButton(isDeleting = state.pendingAction == EditorAction.Delete, onClick = onDeleteClick)
            }
        }
    }
}

@Composable
private fun KindSelector(selected: NoteKind, onSelect: (NoteKind) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        NoteKind.entries.forEachIndexed { index, kind ->
            SegmentedButton(
                selected = kind == selected,
                onClick = { onSelect(kind) },
                shape = SegmentedButtonDefaults.itemShape(index, NoteKind.entries.size)
            ) {
                Text(
                    stringResource(
                        if (kind == NoteKind.Checklist) R.string.editor_kind_checklist else R.string.editor_kind_text
                    )
                )
            }
        }
    }
}

@Composable
private fun ShareSwitch(shared: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(value = shared, role = Role.Switch, onValueChange = onChange)
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.editor_share), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.editor_share_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Baris sudah toggleable; switch hanya penanda agar tidak terbaca dua kali oleh pembaca layar.
        Switch(checked = shared, onCheckedChange = null)
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
