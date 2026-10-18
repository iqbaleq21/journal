package com.iqbal.journal.data

import com.iqbal.journal.data.local.AuthorEntity
import com.iqbal.journal.data.local.GroupEntity
import com.iqbal.journal.data.local.MyProfileEntity
import com.iqbal.journal.data.local.NoteEntity
import com.iqbal.journal.data.local.NoteWithAuthor
import com.iqbal.journal.data.remote.AuthorDto
import com.iqbal.journal.data.remote.GroupDto
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
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
    inviteExpiresAtEpochMillis = inviteExpiresAt?.let(::parseTimestampMillis)
)

internal fun GroupEntity.toDomain() = Group(
    id = id,
    name = name,
    ownerId = ownerId,
    memberCount = memberCount,
    inviteCode = inviteCode,
    inviteExpiresAt = inviteExpiresAtEpochMillis?.let(Instant::ofEpochMilli)
)

internal fun NoteDto.toEntity() = NoteEntity(
    id = id,
    groupId = groupId,
    authorId = authorId,
    content = content,
    color = color,
    createdAtEpochMillis = parseTimestampMillis(createdAt),
    updatedAtEpochMillis = parseTimestampMillis(updatedAt)
)

internal fun NoteWithAuthor.toDomain() = Note(
    id = note.id,
    groupId = note.groupId,
    authorId = note.authorId,
    authorName = authorName,
    content = note.content,
    color = noteColorOf(note.color),
    createdAt = Instant.ofEpochMilli(note.createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(note.updatedAtEpochMillis)
)

internal fun AuthorDto.toEntity() = AuthorEntity(id = id, displayName = displayName)

/** Kunci di database berupa huruf kecil (`butter`); kunci tak dikenal jatuh ke warna bawaan. */
private fun noteColorOf(key: String?): NoteColor =
    NoteColor.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: NoteColor.Plain

// Postgres mengirim offset "+00:00", bukan "Z", jadi diparse sebagai OffsetDateTime.
private fun parseTimestampMillis(value: String): Long = OffsetDateTime.parse(value).toInstant().toEpochMilli()
