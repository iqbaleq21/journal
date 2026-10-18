package com.iqbal.journal.ui.start

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.R
import com.iqbal.journal.domain.validation.GroupRules
import com.iqbal.journal.ui.common.messageRes
import com.iqbal.journal.ui.components.InviteCodeField
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

@Composable
fun StartRoute(onGroupReady: () -> Unit, onBack: (() -> Unit)?, viewModel: StartViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.groupReady.collect { onGroupReady() } }
    StartScreen(
        state = state,
        onBack = onBack,
        onCreateClick = viewModel::onCreateClick,
        onCreateDismiss = viewModel::onCreateDismiss,
        onGroupNameChange = viewModel::onGroupNameChange,
        onCreateConfirm = viewModel::onCreateConfirm,
        onInviteCodeChange = viewModel::onInviteCodeChange,
        onJoin = viewModel::onJoin,
        onErrorShown = viewModel::onErrorShown
    )
}

@Composable
fun StartScreen(
    state: StartUiState,
    onBack: (() -> Unit)?,
    onCreateClick: () -> Unit,
    onCreateDismiss: () -> Unit,
    onGroupNameChange: (String) -> Unit,
    onCreateConfirm: () -> Unit,
    onInviteCodeChange: (String) -> Unit,
    onJoin: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
        ) {
            if (onBack != null) {
                FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(Sizes.touchTarget)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.common_back))
                }
            }
            Greeting(state.displayName)
            Text(stringResource(R.string.start_headline), style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(R.string.start_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CreateGroupCard(enabled = !state.isBusy, onClick = onCreateClick)
            JoinGroupCard(
                code = state.inviteCode,
                isJoining = state.pendingAction == StartAction.Join,
                enabled = !state.isBusy,
                canJoin = state.canJoin,
                onCodeChange = onInviteCodeChange,
                onJoin = onJoin
            )
        }
    }

    if (state.isCreateDialogOpen) {
        CreateGroupDialog(
            name = state.groupName,
            showError = state.showGroupNameError,
            isCreating = state.pendingAction == StartAction.Create,
            onNameChange = onGroupNameChange,
            onConfirm = onCreateConfirm,
            onDismiss = onCreateDismiss
        )
    }
}

@Composable
private fun Greeting(displayName: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Sizes.avatar)
                .background(JournalTheme.noteColors.butter.container, CircleShape)
        ) {
            Text(displayName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            stringResource(R.string.start_greeting, displayName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreateGroupCard(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.large)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Sizes.iconTile)
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = ICON_TILE_ALPHA),
                        MaterialTheme.shapes.small
                    )
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(stringResource(R.string.start_create_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.start_create_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun JoinGroupCard(
    code: String,
    isJoining: Boolean,
    enabled: Boolean,
    canJoin: Boolean,
    onCodeChange: (String) -> Unit,
    onJoin: () -> Unit
) {
    val label = stringResource(R.string.start_join_body, GroupRules.INVITE_CODE_LENGTH)
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Sizes.outlineThin, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(stringResource(R.string.start_join_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            InviteCodeField(
                value = code,
                onValueChange = onCodeChange,
                label = label,
                onDone = onJoin,
                enabled = enabled
            )
            Button(
                onClick = onJoin,
                enabled = canJoin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.buttonHeight)
            ) {
                if (isJoining) {
                    CircularProgressIndicator(
                        strokeWidth = Sizes.outlineThick,
                        modifier = Modifier.size(Sizes.progress)
                    )
                } else {
                    Text(stringResource(R.string.start_join_action), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    name: String,
    showError: Boolean,
    isCreating: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_group_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text(stringResource(R.string.create_group_placeholder)) },
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.create_group_error, GroupRules.MAX_NAME_LENGTH)) }
                } else {
                    null
                },
                singleLine = true,
                enabled = !isCreating,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm() })
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isCreating) {
                if (isCreating) {
                    CircularProgressIndicator(
                        strokeWidth = Sizes.outlineThick,
                        modifier = Modifier.size(Sizes.progress)
                    )
                } else {
                    Text(stringResource(R.string.create_group_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCreating) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

private const val ICON_TILE_ALPHA = 0.18f

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    JournalTheme {
        StartScreen(
            state = StartUiState(displayName = "Ayah", inviteCode = "K7Q"),
            onBack = {},
            onCreateClick = {},
            onCreateDismiss = {},
            onGroupNameChange = {},
            onCreateConfirm = {},
            onInviteCodeChange = {},
            onJoin = {},
            onErrorShown = {}
        )
    }
}
