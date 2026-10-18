package com.iqbal.journal.domain.model

/** Kegagalan yang dipahami aplikasi; kode server dipetakan ke sini di lapisan data. */
sealed interface JournalError {
    data object Network : JournalError
    data object ProfileRequired : JournalError
    data object InviteNotFound : JournalError
    data object InviteExpired : JournalError
    data object GroupFull : JournalError
    data object AlreadyMember : JournalError
    data object NotOwner : JournalError
    data object NotMember : JournalError
    data object CannotRemoveSelf : JournalError
    data object Unknown : JournalError
}

/** Hasil operasi yang bisa gagal tanpa melempar exception ke UI. */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: JournalError) : Outcome<Nothing>
}
