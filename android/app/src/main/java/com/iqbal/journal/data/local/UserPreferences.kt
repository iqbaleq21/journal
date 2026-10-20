package com.iqbal.journal.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbal.journal.domain.model.ThemeMode
import com.iqbal.journal.domain.repository.AppearanceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferensi kecil per HP yang bukan data bersama. */
@Singleton
class UserPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) : AppearanceRepository {
    val selectedGroupId: Flow<String?> = dataStore.data.map { it[SelectedGroupIdKey] }

    suspend fun setSelectedGroupId(groupId: String) {
        dataStore.edit { it[SelectedGroupIdKey] = groupId }
    }

    // Nilai tak dikenal (mis. dari versi app lain) jatuh ke tema sistem.
    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.entries.firstOrNull { it.name == prefs[ThemeModeKey] } ?: ThemeMode.System
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[ThemeModeKey] = mode.name }
    }

    private companion object {
        val SelectedGroupIdKey = stringPreferencesKey("selected_group_id")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
    }
}
