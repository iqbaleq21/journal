package com.iqbal.journal.ui.groupdetail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.iqbal.journal.R
import com.iqbal.journal.domain.validation.GroupRules

@Composable
internal fun GroupDetailDialogs(
    dialog: GroupDetailDialog,
    groupName: String,
    isBusy: Boolean,
    onRenameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val text = when (dialog) {
        is GroupDetailDialog.Rename -> DialogText(
            stringResource(R.string.group_rename),
            null,
            stringResource(R.string.editor_save),
            false
        )

        is GroupDetailDialog.ConfirmRemove -> DialogText(
            stringResource(
                R.string.group_remove_title,
                dialog.member.displayName ?: stringResource(R.string.note_author_unknown)
            ),
            stringResource(R.string.group_remove_body),
            stringResource(R.string.group_remove_confirm),
            true
        )

        GroupDetailDialog.ConfirmLeave -> DialogText(
            stringResource(R.string.group_leave_title, groupName),
            stringResource(R.string.group_leave_body),
            stringResource(R.string.group_leave_confirm),
            true
        )

        GroupDetailDialog.ConfirmRegenerate -> DialogText(
            stringResource(R.string.group_regenerate_title),
            stringResource(R.string.group_regenerate_body),
            stringResource(R.string.group_invite_regenerate),
            false
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text.title) },
        text = {
            if (dialog is GroupDetailDialog.Rename) {
                RenameField(dialog = dialog, onRenameChange = onRenameChange)
            } else if (text.body != null) {
                Text(text.body)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isBusy) {
                Text(
                    text.confirm,
                    color = if (text.isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

/** Kursor di akhir nama lama dan langsung fokus, agar pengguna bisa langsung mengetik atau menghapus. */
@Composable
private fun RenameField(dialog: GroupDetailDialog.Rename, onRenameChange: (String) -> Unit) {
    var field by remember { mutableStateOf(TextFieldValue(dialog.name, selection = TextRange(dialog.name.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        // Teks mengikuti ViewModel (yang memotong panjangnya); posisi kursor disimpan di sini.
        value = field.copy(text = dialog.name),
        onValueChange = {
            field = it
            onRenameChange(it.text)
        },
        singleLine = true,
        isError = dialog.showError,
        supportingText = if (dialog.showError) {
            { Text(stringResource(R.string.create_group_error, GroupRules.MAX_NAME_LENGTH)) }
        } else {
            null
        },
        modifier = Modifier.focusRequester(focusRequester)
    )
}

private data class DialogText(val title: String, val body: String?, val confirm: String, val isDestructive: Boolean)
