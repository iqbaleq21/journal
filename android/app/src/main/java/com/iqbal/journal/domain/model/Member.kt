package com.iqbal.journal.domain.model

import java.time.Instant

data class Member(
    val groupId: String,
    val userId: String,
    /** Null jika profilnya belum terbaca. */
    val displayName: String?,
    val joinedAt: Instant
)
