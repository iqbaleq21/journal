package com.iqbal.journal.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.ui.common.messageRes
import com.iqbal.journal.ui.components.Wordmark
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

@Composable
fun HomeRoute(onAddGroup: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    HomeScreen(
        state = state,
        onSelectGroup = viewModel::onSelectGroup,
        onAddGroup = onAddGroup,
        onInviteClick = viewModel::onInviteClick,
        onInviteDismiss = viewModel::onInviteDismiss,
        onShareInvite = { group, code -> context.shareInvite(group.name, code) },
        onErrorShown = viewModel::onErrorShown
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSelectGroup: (String) -> Unit,
    onAddGroup: () -> Unit,
    onInviteClick: () -> Unit,
    onInviteDismiss: () -> Unit,
    onShareInvite: (Group, String) -> Unit,
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
                .padding(top = Spacing.medium)
        ) {
            Wordmark(
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = Spacing.screen)
            )
            GroupChips(
                groups = state.groups,
                selectedGroupId = state.selectedGroupId,
                onSelect = onSelectGroup,
                onAdd = onAddGroup
            )
            state.selectedGroup?.let { group ->
                GroupSummaryCard(
                    group = group,
                    onClick = onInviteClick,
                    modifier = Modifier.padding(horizontal = Spacing.screen)
                )
            }
            Text(
                stringResource(R.string.home_notes_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.large)
            )
            EmptyNotes(Modifier.weight(1f))
        }
    }

    val group = state.selectedGroup
    if (state.isInviteSheetOpen && group != null) {
        InviteSheet(group = group, onShare = { onShareInvite(group, it) }, onDismiss = onInviteDismiss)
    }
}

@Composable
private fun GroupChips(groups: List<Group>, selectedGroupId: String?, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = Spacing.screen)
    ) {
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
        item {
            OutlinedIconButton(onClick = onAdd, modifier = Modifier.size(Sizes.touchTarget)) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.home_add_group))
            }
        }
    }
}

@Composable
private fun GroupSummaryCard(group: Group, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.large, vertical = Spacing.medium)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                Text(group.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    pluralStringResource(R.plurals.home_member_count, group.memberCount, group.memberCount),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small)
                ) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = null, Modifier.size(Sizes.progress))
                    Text(stringResource(R.string.home_invite_action), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun EmptyNotes(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.extraLarge)
    ) {
        Text(stringResource(R.string.home_notes_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.home_notes_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun Context.shareInvite(groupName: String, code: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_share_text, groupName, code))
    startActivity(Intent.createChooser(send, getString(R.string.invite_share_chooser)))
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val groups = listOf(
        Group("1", "Rumah", "a", memberCount = 4, inviteCode = "K7QM4X", inviteExpiresAt = null),
        Group("2", "Kita Berdua", "a", memberCount = 2, inviteCode = null, inviteExpiresAt = null)
    )
    JournalTheme {
        HomeScreen(
            state = HomeUiState(groups = groups, selectedGroupId = "1"),
            onSelectGroup = {},
            onAddGroup = {},
            onInviteClick = {},
            onInviteDismiss = {},
            onShareInvite = { _, _ -> },
            onErrorShown = {}
        )
    }
}
