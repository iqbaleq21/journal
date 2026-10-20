package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Pilihan tampilan yang hanya berlaku di HP ini. */
interface AppearanceRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
