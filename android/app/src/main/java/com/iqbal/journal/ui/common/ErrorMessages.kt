package com.iqbal.journal.ui.common

import androidx.annotation.StringRes
import com.iqbal.journal.R
import com.iqbal.journal.domain.model.JournalError

@StringRes
fun JournalError.messageRes(): Int = when (this) {
    JournalError.Network -> R.string.error_network
    JournalError.ProfileRequired -> R.string.error_profile_required
    JournalError.InviteNotFound -> R.string.error_invite_not_found
    JournalError.InviteExpired -> R.string.error_invite_expired
    JournalError.GroupFull -> R.string.error_group_full
    JournalError.AlreadyMember -> R.string.error_already_member
    JournalError.NotOwner -> R.string.error_not_owner
    JournalError.NotMember -> R.string.error_not_member
    JournalError.CannotRemoveSelf -> R.string.error_cannot_remove_self
    JournalError.Unknown -> R.string.error_unknown
}
