package com.iqbal.journal.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Profil pemilik HP ini. Tabel berisi paling banyak satu baris. */
@Entity(tableName = "my_profile")
data class MyProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val email: String
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "member_count") val memberCount: Int,
    @ColumnInfo(name = "invite_code") val inviteCode: String?,
    @ColumnInfo(name = "invite_expires_at") val inviteExpiresAtEpochMillis: Long?
)
