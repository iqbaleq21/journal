package com.iqbal.journal.data

import com.iqbal.journal.data.local.AuthorEntity
import com.iqbal.journal.data.local.NoteEntity
import com.iqbal.journal.data.local.NoteWithAuthor
import com.iqbal.journal.data.local.WidgetSelectionEntity
import com.iqbal.journal.data.remote.AuthorDto
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteKind
import com.iqbal.journal.domain.model.WidgetSelection
import java.time.Instant

internal fun NoteDto.toEntity() = NoteEntity(
    id = id,
    groupId = groupId,
    authorId = authorId,
    content = content,
    color = color,
    kind = kind,
    shared = shared,
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
    kind = noteKindOf(note.kind),
    shared = note.shared,
    createdAt = Instant.ofEpochMilli(note.createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(note.updatedAtEpochMillis),
    isPending = isPending
)

internal fun WidgetSelectionEntity.toDomain() =
    WidgetSelection(appWidgetId = appWidgetId, noteId = noteId, groupId = groupId)

internal fun WidgetSelection.toEntity() =
    WidgetSelectionEntity(appWidgetId = appWidgetId, noteId = noteId, groupId = groupId)

internal fun AuthorDto.toEntity() = AuthorEntity(id = id, displayName = displayName)

/** Kunci kolom `notes.color`; warna bawaan disimpan sebagai null. */
internal fun NoteColor.toKey(): String? = if (this == NoteColor.Plain) null else name.lowercase()

/** Kunci di database berupa huruf kecil (`butter`); kunci tak dikenal jatuh ke warna bawaan. */
private fun noteColorOf(key: String?): NoteColor =
    NoteColor.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: NoteColor.Plain

internal fun NoteKind.toKey(): String = name.lowercase()

/** Kunci tak dikenal (mis. dari versi app yang lebih baru) ditampilkan sebagai teks biasa. */
private fun noteKindOf(key: String): NoteKind =
    NoteKind.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: NoteKind.Text
