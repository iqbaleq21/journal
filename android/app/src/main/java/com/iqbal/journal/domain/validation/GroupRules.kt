package com.iqbal.journal.domain.validation

/** Aturan nama grup dan kode undangan; harus sama dengan database (`groups`, `generate_invite_code`). */
object GroupRules {
    const val MAX_NAME_LENGTH = 50
    const val INVITE_CODE_LENGTH = 6

    // Tanpa karakter ambigu: 0 O 1 I L
    private const val INVITE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun normalizeName(input: String): String = input.trim()

    fun isValidName(name: String): Boolean = normalizeName(name).length in 1..MAX_NAME_LENGTH

    /** Huruf besar, buang karakter di luar alfabet kode, potong ke panjang kode. */
    fun normalizeInviteCode(input: String): String =
        input.uppercase().filter { it in INVITE_ALPHABET }.take(INVITE_CODE_LENGTH)

    fun isCompleteInviteCode(code: String): Boolean = normalizeInviteCode(code).length == INVITE_CODE_LENGTH
}
