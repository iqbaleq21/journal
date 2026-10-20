package com.iqbal.journal.domain.model

data class ChecklistItem(val text: String, val checked: Boolean = false)

/** Checklist disimpan sebagai teks di kolom `content`, satu item per baris: `[ ] item` / `[x] item`. */
object Checklist {
    private const val CHECKED = "[x] "
    private const val UNCHECKED = "[ ] "

    /** Baris tanpa penanda (mis. dari catatan teks yang diubah jadi checklist) menjadi item belum dicentang. */
    fun parse(content: String): List<ChecklistItem> = content.lines().filter { it.isNotBlank() }.map { line ->
        if (line.startsWith(CHECKED)) {
            ChecklistItem(line.removePrefix(CHECKED), checked = true)
        } else {
            ChecklistItem(line.removePrefix(UNCHECKED))
        }
    }

    /** Item kosong dibuang. */
    fun format(items: List<ChecklistItem>): String = items.filter { it.text.isNotBlank() }
        .joinToString("\n") { (if (it.checked) CHECKED else UNCHECKED) + it.text.trim() }

    fun preview(content: String): String =
        parse(content).joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }

    fun toPlainText(items: List<ChecklistItem>): String = items.joinToString("\n") { it.text }.trim()
}
