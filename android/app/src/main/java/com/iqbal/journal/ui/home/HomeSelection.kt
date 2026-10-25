package com.iqbal.journal.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.ui.editor.NoteColorPicker
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

// Mode pilih di Beranda (US-23): aksi massal untuk catatan yang dipilih.

/** Pengganti baris wordmark selama mode pilih. */
@Composable
internal fun SelectionBar(
    count: Int,
    canDelete: Boolean,
    isBusy: Boolean,
    onClose: () -> Unit,
    onOpenActions: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        IconButton(onClick = onClose, enabled = !isBusy, modifier = Modifier.size(Sizes.touchTarget)) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.home_selection_close))
        }
        Text(
            pluralStringResource(R.plurals.home_selection_count, count, count),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.small)
        )
        FilledTonalIconButton(onClick = onOpenActions, enabled = !isBusy, modifier = Modifier.size(Sizes.touchTarget)) {
            Icon(painterResource(R.drawable.ic_more), stringResource(R.string.home_selection_actions))
        }
        Spacer(Modifier.size(Spacing.small))
        FilledTonalIconButton(
            onClick = onDelete,
            enabled = canDelete && !isBusy,
            modifier = Modifier.size(Sizes.touchTarget)
        ) {
            Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.home_selection_delete))
        }
    }
}

/** Warna untuk catatan yang boleh diedit; akses edit hanya untuk catatan sendiri. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionActionsSheet(
    editableCount: Int,
    ownCount: Int,
    onColorChange: (NoteColor) -> Unit,
    onSharedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
            modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.large)
        ) {
            Text(stringResource(R.string.home_selection_actions), style = MaterialTheme.typography.titleLarge)
            if (editableCount > 0) {
                // Tanpa pilihan awal: warna catatan yang dipilih bisa berbeda-beda.
                NoteColorPicker(selected = null, onSelect = onColorChange)
            }
            if (ownCount > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    Text(stringResource(R.string.editor_share), style = MaterialTheme.typography.titleSmall)
                    Text(
                        pluralStringResource(R.plurals.home_selection_share_body, ownCount, ownCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                        SheetButton(R.string.home_selection_share_on, Modifier.weight(1f)) { onSharedChange(true) }
                        SheetButton(R.string.home_selection_share_off, Modifier.weight(1f)) { onSharedChange(false) }
                    }
                }
            }
            if (editableCount == 0) {
                Text(
                    stringResource(R.string.home_selection_read_only),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SheetButton(label: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(Sizes.buttonHeight)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    }
}

/** [skippedCount] = catatan orang lain yang ikut dipilih tapi tidak bisa dihapus. */
@Composable
internal fun SelectionDeleteDialog(count: Int, skippedCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.home_selection_delete_title, count, count)) },
        text = {
            Text(
                buildString {
                    append(stringResource(R.string.editor_delete_body))
                    if (skippedCount > 0) {
                        append("\n\n")
                        append(
                            pluralStringResource(R.plurals.home_selection_delete_skipped, skippedCount, skippedCount)
                        )
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.editor_delete_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } }
    )
}
