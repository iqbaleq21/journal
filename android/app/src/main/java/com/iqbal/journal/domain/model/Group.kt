package com.iqbal.journal.domain.model

import java.time.Instant

data class Group(
    val id: String,
    val name: String,
    val ownerId: String,
    val memberCount: Int,
    /** Hanya terlihat oleh anggota; null jika grup sudah tidak menerima undangan. */
    val inviteCode: String?,
    val inviteExpiresAt: Instant?
)
