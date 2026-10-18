package com.iqbal.journal.data

import com.iqbal.journal.data.local.GroupEntity
import com.iqbal.journal.data.local.MyProfileEntity
import com.iqbal.journal.data.remote.GroupDto
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Profile
import java.time.Instant
import java.time.OffsetDateTime

internal fun MyProfileEntity.toDomain() = Profile(id = id, displayName = displayName, email = email)

internal fun Profile.toEntity() = MyProfileEntity(id = id, displayName = displayName, email = email)

internal fun GroupDto.toEntity() = GroupEntity(
    id = id,
    name = name,
    ownerId = ownerId,
    memberCount = members.firstOrNull()?.count ?: 0,
    inviteCode = inviteCode,
    // Postgres mengirim offset "+00:00", bukan "Z", jadi diparse sebagai OffsetDateTime.
    inviteExpiresAtEpochMillis = inviteExpiresAt?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
)

internal fun GroupEntity.toDomain() = Group(
    id = id,
    name = name,
    ownerId = ownerId,
    memberCount = memberCount,
    inviteCode = inviteCode,
    inviteExpiresAt = inviteExpiresAtEpochMillis?.let(Instant::ofEpochMilli)
)
