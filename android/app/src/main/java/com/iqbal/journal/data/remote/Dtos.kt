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
