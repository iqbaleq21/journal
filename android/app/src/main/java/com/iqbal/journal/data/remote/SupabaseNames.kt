package com.iqbal.journal.data.remote

/** Nama tabel dan RPC; harus sama dengan supabase/migrations. */
internal object Tables {
    const val PROFILES = "profiles"
    const val GROUPS = "groups"
    const val NOTES = "notes"
    const val DEVICES = "devices"
    const val GROUP_MEMBERS = "group_members"
}

internal object Rpc {
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val LEAVE_GROUP = "leave_group"
    const val REMOVE_MEMBER = "remove_member"
    const val RENAME_GROUP = "rename_group"
    const val REGENERATE_INVITE = "regenerate_invite"
}
