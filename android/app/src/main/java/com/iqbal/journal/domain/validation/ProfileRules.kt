package com.iqbal.journal.domain.validation

/** Aturan input profil; harus sama dengan constraint tabel `profiles` di database. */
object ProfileRules {
    const val MAX_NAME_LENGTH = 40

    // Sengaja longgar (sama dengan check di database); email belum diverifikasi di v1.
    private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeName(input: String): String = input.trim()

    fun normalizeEmail(input: String): String = input.trim().lowercase()

    fun isValidName(name: String): Boolean = normalizeName(name).length in 1..MAX_NAME_LENGTH

    fun isValidEmail(email: String): Boolean = EmailPattern.matches(normalizeEmail(email))
}
