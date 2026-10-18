package com.iqbal.journal.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(val id: String, @SerialName("display_name") val displayName: String, val email: String)

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("invite_expires_at") val inviteExpiresAt: String? = null,
    /** Hasil embed PostgREST `group_members(count)`: selalu satu elemen. */
    @SerialName("group_members") val members: List<CountDto> = emptyList()
)

@Serializable
data class CountDto(val count: Int)

@Serializable
data class CreateGroupResultDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("invite_code") val inviteCode: String
)

@Serializable
data class AuthorDto(val id: String, @SerialName("display_name") val displayName: String)

@Serializable
data class NoteDto(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("author_id") val authorId: String,
    val content: String,
    val color: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class NewNoteDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("author_id") val authorId: String,
    val content: String
)

/** Hanya kolom id; isi `old_record` Realtime untuk hard delete (replica identity default). */
@Serializable
data class NoteIdDto(val id: String)
