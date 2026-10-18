package com.iqbal.journal.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.ui.common.inviteDaysLeft
import com.iqbal.journal.ui.components.InviteCodeDisplay
import com.iqbal.journal.ui.theme.Sizes
import com.iqbal.journal.ui.theme.Spacing
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteSheet(group: Group, onShare: (code: String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.extraLarge)
        ) {
            Text(group.name, style = MaterialTheme.typography.titleLarge)
            val code = group.inviteCode
            if (code == null) {
                Text(stringResource(R.string.invite_unavailable), style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.invite_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                group.inviteExpiresAt?.let { ExpiryLabel(it) }
            }
            InviteCodeDisplay(code)
            Button(
                onClick = { onShare(code) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.buttonHeight)
            ) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = null)
                Text(
                    stringResource(R.string.invite_share),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = Spacing.small)
                )
            }
        }
    }
}

@Composable
private fun ExpiryLabel(expiresAt: Instant) {
    val daysLeft = inviteDaysLeft(expiresAt, Instant.now())
    Text(
        text = if (daysLeft == 0) {
            stringResource(R.string.invite_expired)
        } else {
            pluralStringResource(R.plurals.invite_days_left, daysLeft, daysLeft)
        },
        style = MaterialTheme.typography.labelMedium,
        color = if (daysLeft == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
