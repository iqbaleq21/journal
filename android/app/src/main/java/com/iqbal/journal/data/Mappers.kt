package com.iqbal.journal.data

import com.iqbal.journal.data.local.GroupEntity
import com.iqbal.journal.data.local.MemberEntity
import com.iqbal.journal.data.local.MemberWithName
import com.iqbal.journal.data.local.MyProfileEntity
import com.iqbal.journal.data.remote.GroupDto
import com.iqbal.journal.data.remote.MembershipDto
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Member
import com.iqbal.journal.domain.model.Profile
import java.time.Instant
import java.time.OffsetDateTime

internal fun MyProfileEntity.toDomain() = Profile(id = id, displayName = displayName, email = email)

internal fun Profile.toEntity() = MyProfileEntity(id = id, displayName = displayName, email = email)

internal fun GroupDto.toEntity(notify: Boolean) = GroupEntity(
    id = id,
    name = name,
    ownerId = ownerId,
    memberCount = members.firstOrNull()?.count ?: 0,
    inviteCode = inviteCode,
    inviteExpiresAtEpochMillis = inviteExpiresAt?.let(::parseTimestampMillis),
    notify = notify
)

internal fun GroupEntity.toDomain() = Group(
    id = id,
    name = name,
    ownerId = ownerId,
    memberCount = memberCount,
    inviteCode = inviteCode,
    inviteExpiresAt = inviteExpiresAtEpochMillis?.let(Instant::ofEpochMilli),
    notify = notify
)

internal fun MembershipDto.toEntity() =
    MemberEntity(groupId = groupId, userId = userId, joinedAtEpochMillis = parseTimestampMillis(joinedAt))

internal fun MemberWithName.toDomain() = Member(
    groupId = member.groupId,
    userId = member.userId,
    displayName = displayName,
    joinedAt = Instant.ofEpochMilli(member.joinedAtEpochMillis)
)

// Postgres mengirim offset "+00:00", bukan "Z", jadi diparse sebagai OffsetDateTime.
internal fun parseTimestampMillis(value: String): Long = OffsetDateTime.parse(value).toInstant().toEpochMilli()
