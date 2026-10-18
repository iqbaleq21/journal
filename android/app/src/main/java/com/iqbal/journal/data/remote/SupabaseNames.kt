package com.iqbal.journal.data.remote

/** Nama tabel dan RPC; harus sama dengan supabase/migrations. */
internal object Tables {
    const val PROFILES = "profiles"
    const val GROUPS = "groups"
    const val NOTES = "notes"
    const val DEVICES = "devices"
}

internal object Rpc {
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
}
