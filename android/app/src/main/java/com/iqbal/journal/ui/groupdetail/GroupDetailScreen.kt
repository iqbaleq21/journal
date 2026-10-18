package com.iqbal.journal.ui.groupdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Member
import com.iqbal.journal.ui.common.inviteDaysLeft
import com.iqbal.journal.ui.common.messageRes
import com.iqbal.journal.ui.common.shareInvite
import com.iqbal.journal.ui.components.InviteCodeDisplay
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import java.time.Instant

/** Batas anggota per grup; harus sama dengan RPC join_group di database. */
private const val MAX_MEMBERS = 10

@Composable
fun GroupDetailRoute(onBack: () -> Unit, onGone: () -> Unit, viewModel: GroupDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.isGone) { if (state.isGone) onGone() }
    GroupDetailScreen(
        state = state,
        onBack = onBack,
        onShare = { group, code -> context.shareInvite(group.name, code) },
        onRenameClick = viewModel::onRenameClick,
        onRenameChange = viewModel::onRenameChange,
        onRemoveClick = viewModel::onRemoveClick,
        onLeaveClick = viewModel::onLeaveClick,
        onRegenerateClick = viewModel::onRegenerateClick,
        onDialogDismiss = viewModel::onDialogDismiss,
        onDialogConfirm = viewModel::onDialogConfirm,
        onErrorShown = viewModel::onErrorShown
    )
}

@Composable
fun GroupDetailScreen(
    state: GroupDetailUiState,
    onBack: () -> Unit,
    onShare: (Group, String) -> Unit,
    onRenameClick: () -> Unit,
    onRenameChange: (String) -> Unit,
    onRemoveClick: (Member) -> Unit,
    onLeaveClick: () -> Unit,
    onRegenerateClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onDialogConfirm: () -> Unit,
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
    val group = state.group ?: return

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(Sizes.touchTarget)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.common_back))
                }
                Box(Modifier.weight(1f))
                if (state.isOwner) {
                    FilledTonalIconButton(onClick = onRenameClick, modifier = Modifier.size(Sizes.touchTarget)) {
                        Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.group_rename))
                    }
                }
            }
            Header(group = group, ownerName = state.members.ownerName(group), isOwner = state.isOwner)
            InviteCard(
                group = group,
                isOwner = state.isOwner,
                enabled = !state.isBusy,
                onShare = onShare,
                onRegenerate = onRegenerateClick
            )
            Text(
                stringResource(R.string.group_members_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.extraSmall)
            )
            MemberList(
                members = state.members,
                ownerId = group.ownerId,
                myUserId = state.myUserId,
                canManage = state.isOwner && !state.isBusy,
                onRemove = onRemoveClick
            )
            TextButton(
                onClick = onLeaveClick,
                enabled = !state.isBusy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.buttonHeight)
            ) {
                Text(stringResource(R.string.group_leave), style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    state.dialog?.let { dialog ->
        GroupDetailDialogs(
            dialog = dialog,
            groupName = group.name,
            isBusy = state.isBusy,
            onRenameChange = onRenameChange,
            onConfirm = onDialogConfirm,
            onDismiss = onDialogDismiss
        )
    }
}

@Composable
private fun Header(group: Group, ownerName: String?, isOwner: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
        Text(group.name, style = MaterialTheme.typography.displayMedium)
        val count = stringResource(R.string.group_member_count, group.memberCount, MAX_MEMBERS)
        Text(
            when {
                isOwner -> stringResource(R.string.group_meta_you_owner, count)
                ownerName != null -> stringResource(R.string.group_meta_owner, count, ownerName)
                else -> count
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InviteCard(
    group: Group,
    isOwner: Boolean,
    enabled: Boolean,
    onShare: (Group, String) -> Unit,
    onRegenerate: () -> Unit
) {
    val expiresAt = group.inviteExpiresAt
    val daysLeft = expiresAt?.let { inviteDaysLeft(it, Instant.now()) }
    val code = group.inviteCode?.takeIf { daysLeft != 0 }
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.group_invite_title),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                if (code != null && daysLeft != null) {
                    Text(
                        pluralStringResource(R.plurals.invite_days_left, daysLeft, daysLeft),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            if (code != null) {
                InviteCodeDisplay(
                    code,
                    container = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = CODE_BOX_ALPHA)
                )
            } else {
                Text(
                    stringResource(
                        if (isOwner) R.string.group_invite_none_owner else R.string.group_invite_none_member
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                if (code != null) {
                    Button(
                        onClick = { onShare(group, code) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JournalTheme.noteColors.butter.container,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(Sizes.buttonHeight)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            Modifier.size(Sizes.progress)
                        )
                        Text(
                            stringResource(R.string.invite_share),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = Spacing.small)
                        )
                    }
                }
                if (isOwner) {
                    OutlinedButton(
                        onClick = onRegenerate,
                        enabled = enabled,
                        border = BorderStroke(Sizes.outlineThin, MaterialTheme.colorScheme.inverseOnSurface),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(Sizes.buttonHeight)
                    ) {
                        Text(
                            stringResource(R.string.group_invite_regenerate),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberList(
    members: List<Member>,
    ownerId: String,
    myUserId: String?,
    canManage: Boolean,
    onRemove: (Member) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(vertical = Spacing.small)) {
            members.forEach { member ->
                MemberRow(
                    member = member,
                    isOwner = member.userId == ownerId,
                    isMe = member.userId == myUserId,
                    canRemove = canManage && member.userId != myUserId,
                    onRemove = { onRemove(member) }
                )
            }
        }
    }
}

@Composable
private fun MemberRow(member: Member, isOwner: Boolean, isMe: Boolean, canRemove: Boolean, onRemove: () -> Unit) {
    val name = member.displayName ?: stringResource(R.string.note_author_unknown)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.touchTarget + Spacing.medium)
            .padding(start = Spacing.medium, end = Spacing.small)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Sizes.avatar)
                .background(JournalTheme.noteColors.butter.container, CircleShape)
        ) {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            if (isMe) stringResource(R.string.group_member_you, name) else name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isOwner) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    stringResource(R.string.group_owner_badge),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.cardGap, vertical = Spacing.extraSmall)
                )
            }
        }
        if (canRemove) MemberMenu(name = name, onRemove = onRemove)
    }
}

@Composable
private fun MemberMenu(name: String, onRemove: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_more), stringResource(R.string.group_member_options, name))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.group_remove_member), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    onRemove()
                }
            )
        }
    }
}

private fun List<Member>.ownerName(group: Group): String? = firstOrNull { it.userId == group.ownerId }?.displayName

private const val CODE_BOX_ALPHA = 0.12f

@Preview(showBackground = true)
@Composable
private fun GroupDetailScreenPreview() {
    val members = listOf(
        Member("g1", "me", "Ayah", Instant.EPOCH),
        Member("g1", "ibu", "Ibu", Instant.EPOCH)
    )
    JournalTheme {
        GroupDetailScreen(
            state = GroupDetailUiState(
                group = Group(
                    "g1",
                    "Rumah",
                    "me",
                    memberCount = 2,
                    inviteCode = "K7QM4X",
                    inviteExpiresAt = Instant.now()
                ),
                members = members,
                myUserId = "me"
            ),
            onBack = {},
            onShare = { _, _ -> },
            onRenameClick = {},
            onRenameChange = {},
            onRemoveClick = {},
            onLeaveClick = {},
            onRegenerateClick = {},
            onDialogDismiss = {},
            onDialogConfirm = {},
            onErrorShown = {}
        )
    }
}
