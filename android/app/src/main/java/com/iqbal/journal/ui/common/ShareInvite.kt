package com.iqbal.journal.ui.common

import android.content.Context
import android.content.Intent
import com.iqbal.journal.R

fun Context.shareInvite(groupName: String, code: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_share_text, groupName, code))
    startActivity(Intent.createChooser(send, getString(R.string.invite_share_chooser)))
}
