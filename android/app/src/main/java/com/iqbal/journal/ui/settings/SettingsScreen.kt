package com.iqbal.journal.ui.settings

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.BuildConfig
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.validation.ProfileRules
import com.iqbal.journal.ui.common.messageRes
import com.iqbal.journal.ui.theme.JournalTheme
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isBatteryUnrestricted by remember { mutableStateOf(context.isIgnoringBatteryOptimizations()) }
    // Dibaca ulang saat kembali dari pengaturan sistem.
    LifecycleResumeEffect(Unit) {
        isBatteryUnrestricted = context.isIgnoringBatteryOptimizations()
        onPauseOrDispose {}
    }
    SettingsScreen(
        state = state,
        isBatteryUnrestricted = isBatteryUnrestricted,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onSaveProfile = viewModel::onSaveProfile,
        onNotifyChange = viewModel::onNotifyChange,
        onOpenBatterySettings = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        onErrorShown = viewModel::onErrorShown
    )
}

private fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    isBatteryUnrestricted: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onNotifyChange: (groupId: String, enabled: Boolean) -> Unit,
    onOpenBatterySettings: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.medium)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.cardGap)
            ) {
                FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(Sizes.touchTarget)) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.common_back))
                }
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
            }
            ProfileSection(state, onNameChange, onEmailChange, onSaveProfile)
            NotificationSection(state.groups, state.pendingNotifyGroupIds, onNotifyChange)
            BatterySection(isBatteryUnrestricted, onOpenBatterySettings)
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.extraSmall)
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.extraSmall)
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                content()
            }
        }
    }
}

@Composable
private fun ProfileSection(
    state: SettingsUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Section(stringResource(R.string.settings_profile)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_name_label)) },
            singleLine = true,
            isError = state.showNameError,
            supportingText = if (state.showNameError) {
                { Text(stringResource(R.string.onboarding_name_error, ProfileRules.MAX_NAME_LENGTH)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.onboarding_email_label)) },
            singleLine = true,
            isError = state.showEmailError,
            supportingText = if (state.showEmailError) {
                { Text(stringResource(R.string.onboarding_email_error)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSave,
            enabled = state.hasProfileChanges && !state.isSavingProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.buttonHeight)
        ) {
            if (state.isSavingProfile) {
                CircularProgressIndicator(
                    strokeWidth = Sizes.outlineThick,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Sizes.progress)
                )
            } else {
                Text(stringResource(R.string.editor_save), style = MaterialTheme.typography.labelLarge)
            }
        }
        // Akun anonim: tidak ada logout, dan identitas tidak bisa dipulihkan di HP lain (PRD §5.1).
        Text(
            stringResource(R.string.settings_identity_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationSection(
    groups: List<Group>,
    pendingGroupIds: Set<String>,
    onNotifyChange: (groupId: String, enabled: Boolean) -> Unit
) {
    Section(stringResource(R.string.settings_notifications)) {
        Text(
            stringResource(R.string.settings_notifications_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        groups.forEach { group ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(group.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Switch(
                    checked = group.notify,
                    onCheckedChange = { onNotifyChange(group.id, it) },
                    enabled = group.id !in pendingGroupIds
                )
            }
        }
    }
}

@Composable
private fun BatterySection(isUnrestricted: Boolean, onOpenSettings: () -> Unit) {
    Section(stringResource(R.string.settings_battery)) {
        Text(
            stringResource(if (isUnrestricted) R.string.settings_battery_ok else R.string.settings_battery_body),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(R.string.settings_battery_vendor),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_battery_open))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    JournalTheme {
        SettingsScreen(
            state = SettingsUiState(
                name = "Ayah",
                email = "ayah@rumah.id",
                savedName = "Ayah",
                savedEmail = "ayah@rumah.id",
                groups = listOf(
                    Group("g1", "Rumah", "me", memberCount = 4, inviteCode = null, inviteExpiresAt = null),
                    Group(
                        "g2",
                        "Kantor",
                        "x",
                        memberCount = 3,
                        inviteCode = null,
                        inviteExpiresAt = null,
                        notify = false
                    )
                )
            ),
            isBatteryUnrestricted = false,
            onBack = {},
            onNameChange = {},
            onEmailChange = {},
            onSaveProfile = {},
            onNotifyChange = { _, _ -> },
            onOpenBatterySettings = {},
            onErrorShown = {}
        )
    }
}
