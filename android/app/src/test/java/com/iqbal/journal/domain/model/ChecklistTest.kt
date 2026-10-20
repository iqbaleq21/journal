package com.iqbal.journal.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistTest {
    @Test
    fun `format lalu parse menghasilkan item yang sama, item kosong dibuang`() {
        val items = listOf(ChecklistItem("Beli susu", checked = true), ChecklistItem("  "), ChecklistItem("Telur"))

        val content = Checklist.format(items)

        assertEquals("[x] Beli susu\n[ ] Telur", content)
        assertEquals(
            listOf(ChecklistItem("Beli susu", checked = true), ChecklistItem("Telur")),
            Checklist.parse(content)
        )
    }

    @Test
    fun `teks biasa jadi item belum dicentang dan pratinjau memakai kotak centang`() {
        assertEquals(listOf(ChecklistItem("Susu"), ChecklistItem("Roti")), Checklist.parse("Susu\n\nRoti"))
        assertEquals("☑ Susu\n☐ Roti", Checklist.preview("[x] Susu\n[ ] Roti"))
    }
}
