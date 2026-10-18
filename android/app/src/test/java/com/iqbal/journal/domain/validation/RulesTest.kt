package com.iqbal.journal.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileRulesTest {
    @Test
    fun `nama harus terisi dan tidak lebih dari batas`() {
        assertTrue(ProfileRules.isValidName("Ayah"))
        assertFalse(ProfileRules.isValidName("   "))
        assertTrue(ProfileRules.isValidName("a".repeat(ProfileRules.MAX_NAME_LENGTH)))
        assertFalse(ProfileRules.isValidName("a".repeat(ProfileRules.MAX_NAME_LENGTH + 1)))
    }

    @Test
    fun `email dinormalisasi dan divalidasi longgar`() {
        assertEquals("ayah@rumah.id", ProfileRules.normalizeEmail("  Ayah@Rumah.ID "))
        assertTrue(ProfileRules.isValidEmail("ayah@rumah.id"))
        assertFalse(ProfileRules.isValidEmail("ayah@rumah"))
        assertFalse(ProfileRules.isValidEmail("ayah rumah@x.id"))
    }
}

class GroupRulesTest {
    @Test
    fun `kode undangan dinormalisasi ke huruf besar tanpa karakter ambigu`() {
        assertEquals("K7QM4X", GroupRules.normalizeInviteCode("k7q-m4x"))
        // 0, O, 1, I, L tidak pernah dipakai di kode, jadi dibuang.
        assertEquals("AB", GroupRules.normalizeInviteCode("a0o1ilb"))
        assertEquals("ABCDEF", GroupRules.normalizeInviteCode("abcdefgh"))
    }

    @Test
    fun `kode lengkap hanya jika panjangnya pas`() {
        assertTrue(GroupRules.isCompleteInviteCode("K7QM4X"))
        assertFalse(GroupRules.isCompleteInviteCode("K7QM4"))
    }

    @Test
    fun `nama grup harus terisi dan tidak lebih dari batas`() {
        assertTrue(GroupRules.isValidName(" Rumah "))
        assertFalse(GroupRules.isValidName(""))
        assertFalse(GroupRules.isValidName("a".repeat(GroupRules.MAX_NAME_LENGTH + 1)))
    }
}
