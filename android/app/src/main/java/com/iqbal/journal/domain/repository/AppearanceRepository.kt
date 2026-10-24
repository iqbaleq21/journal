package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.AppLanguage
import com.iqbal.journal.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Pilihan tampilan yang hanya berlaku di HP ini. */
interface AppearanceRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    val language: Flow<AppLanguage>

    /** Langsung diterapkan; layar yang terbuka dibuat ulang dengan bahasa baru. */
    suspend fun setLanguage(language: AppLanguage)
}
