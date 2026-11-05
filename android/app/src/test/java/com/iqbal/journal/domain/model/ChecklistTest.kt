package com.iqbal.journal.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistTest {
    @Test
    fun `format lalu parse menghasilkan item yang sama, item kosong dibuang`() {
        val items = listOf(ChecklistItem("Telur"), ChecklistItem("  "), ChecklistItem("Beli susu", checked = true))

        val content = Checklist.format(items)

        assertEquals("[ ] Telur\n[x] Beli susu", content)
        assertEquals(
            listOf(ChecklistItem("Telur"), ChecklistItem("Beli susu", checked = true)),
            Checklist.parse(content)
        )
    }

    @Test
    fun `teks biasa jadi item belum dicentang dan pratinjau memakai kotak centang`() {
        assertEquals(listOf(ChecklistItem("Susu"), ChecklistItem("Roti")), Checklist.parse("Susu\n\nRoti"))
        assertEquals("☐ Roti\n☑ Susu", Checklist.preview("[x] Susu\n[ ] Roti"))
    }

    @Test
    fun `item tercentang selalu di bawah dengan urutan relatif tetap`() {
        assertEquals(
            listOf(
                ChecklistItem("Telur"),
                ChecklistItem("Roti"),
                ChecklistItem("Susu", true),
                ChecklistItem("Kopi", true)
            ),
            Checklist.parse("[x] Susu\n[ ] Telur\n[x] Kopi\n[ ] Roti")
        )
    }
}
