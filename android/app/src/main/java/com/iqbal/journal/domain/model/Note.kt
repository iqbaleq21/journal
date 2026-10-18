package com.iqbal.journal.domain.model

import java.time.Instant

/** Warna latar catatan; nilainya disimpan sebagai kunci di kolom `notes.color`. */
enum class NoteColor { Butter, Mint, Sky, Peach, Lilac, Plain }

data class Note(
    val id: String,
    val groupId: String,
    val authorId: String,
    /** Null jika profil penulis belum/tidak bisa dibaca (mis. sudah keluar dari grup). */
    val authorName: String?,
    val content: String,
    val color: NoteColor,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Perubahan di HP ini yang belum terkirim ke server (mis. sedang offline). */
    val isPending: Boolean = false
)
