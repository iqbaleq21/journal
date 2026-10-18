package com.iqbal.journal.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
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

@Entity(tableName = "notes", indices = [Index(value = ["group_id", "updated_at"])])
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "author_id") val authorId: String,
    val content: String,
    /** Kunci warna dari database (`butter`, `mint`, …); null = warna bawaan. */
    val color: String?,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long
)

/** Nama penulis catatan, agar daftar catatan tetap lengkap saat offline. */
@Entity(tableName = "authors")
data class AuthorEntity(@PrimaryKey val id: String, @ColumnInfo(name = "display_name") val displayName: String)

data class NoteWithAuthor(@Embedded val note: NoteEntity, @ColumnInfo(name = "author_name") val authorName: String?)
