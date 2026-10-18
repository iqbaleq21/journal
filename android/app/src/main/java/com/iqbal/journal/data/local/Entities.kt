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
    @ColumnInfo(name = "invite_expires_at") val inviteExpiresAtEpochMillis: Long?,
    @ColumnInfo(name = "notify", defaultValue = "1") val notify: Boolean = true
)

@Entity(tableName = "members", primaryKeys = ["group_id", "user_id"])
data class MemberEntity(
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "joined_at") val joinedAtEpochMillis: Long
)

data class MemberWithName(
    @Embedded val member: MemberEntity,
    @ColumnInfo(name = "display_name") val displayName: String?
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

data class NoteWithAuthor(
    @Embedded val note: NoteEntity,
    @ColumnInfo(name = "author_name") val authorName: String?,
    @ColumnInfo(name = "is_pending") val isPending: Boolean = false
)

enum class PendingOpType { Create, Update, Delete }

/** Perubahan catatan yang dibuat di HP ini dan belum terkirim; dikirim berurutan sesuai [id]. */
@Entity(tableName = "pending_ops", indices = [Index(value = ["note_id"])])
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    val type: PendingOpType,
    /** Isi catatan untuk Create/Update; null untuk Delete. */
    val content: String?
)

/** Pilihan catatan per widget. Catatan di sini tidak ikut dibuang oleh batas cache per grup. */
@Entity(tableName = "widget_selections")
data class WidgetSelectionEntity(
    @PrimaryKey @ColumnInfo(name = "app_widget_id") val appWidgetId: Int,
    @ColumnInfo(name = "note_id") val noteId: String,
    @ColumnInfo(name = "group_id") val groupId: String
)
