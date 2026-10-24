package com.iqbal.journal.domain.model

import java.time.Instant

/** Warna latar catatan; nilainya disimpan sebagai kunci di kolom `notes.color`. */
enum class NoteColor { Butter, Mint, Sky, Peach, Lilac, Plain }

/** Bentuk isi catatan; nilainya disimpan sebagai kunci di kolom `notes.kind`. */
enum class NoteKind { Text, Checklist }

/** Bagian catatan yang bisa diubah lewat Editor. */
data class NoteDraft(
    val content: String,
    /** Kosong = tanpa judul. */
    val title: String = "",
    val color: NoteColor = NoteColor.Plain,
    val kind: NoteKind = NoteKind.Text,
    val shared: Boolean = false
)

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
    val isPending: Boolean = false,
    val kind: NoteKind = NoteKind.Text,
    /** Penulis mengizinkan anggota lain mengedit (tidak menghapus) catatan ini. */
    val shared: Boolean = false,
    /** Kosong = tanpa judul. */
    val title: String = ""
) {
    /** Isi untuk ditampilkan apa adanya (kartu, widget, notifikasi). */
    val previewText: String get() = if (kind == NoteKind.Checklist) Checklist.preview(content) else content
}
