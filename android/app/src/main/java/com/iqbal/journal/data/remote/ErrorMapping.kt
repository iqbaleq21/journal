package com.iqbal.journal.data.remote

import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Kode error yang di-`raise` oleh fungsi RPC di database. */
private val ServerErrorCodes = mapOf(
    "PROFILE_REQUIRED" to JournalError.ProfileRequired,
    "INVITE_NOT_FOUND" to JournalError.InviteNotFound,
    "INVITE_EXPIRED" to JournalError.InviteExpired,
    "GROUP_FULL" to JournalError.GroupFull,
    "ALREADY_MEMBER" to JournalError.AlreadyMember,
    "NOT_OWNER" to JournalError.NotOwner
)

internal fun mapServerMessage(message: String?): JournalError =
    ServerErrorCodes.entries.firstOrNull { (code, _) -> message?.contains(code) == true }?.value
        ?: JournalError.Unknown

internal fun Throwable.toJournalError(): JournalError = when (this) {
    is RestException -> mapServerMessage(message)
    is HttpRequestException, is IOException -> JournalError.Network
    else -> JournalError.Unknown
}

/** Jalankan [block] dan ubah exception menjadi [Outcome.Failure]; pembatalan coroutine tetap diteruskan. */
internal suspend fun <T> outcomeOf(block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (
    @Suppress("TooGenericExceptionCaught") e: Exception
) {
    Outcome.Failure(e.toJournalError())
}
