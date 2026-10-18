package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Outcome

/** Token push HP ini, agar anggota lain bisa membangunkannya saat catatan berubah. */
interface DeviceRepository {
    /** Daftarkan token push HP ini saat ini; aman dipanggil berulang. */
    suspend fun registerThisDevice(): Outcome<Unit>

    /** Daftarkan token yang baru diterbitkan Firebase. */
    suspend fun register(token: String): Outcome<Unit>
}
