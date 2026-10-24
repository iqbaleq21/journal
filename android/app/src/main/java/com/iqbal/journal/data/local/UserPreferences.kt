package com.iqbal.journal.data.local

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.iqbal.journal.domain.model.AppLanguage
import com.iqbal.journal.domain.model.ThemeMode
import com.iqbal.journal.domain.repository.AppearanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Preferensi kecil per HP yang bukan data bersama. */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : AppearanceRepository {
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

    override val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        AppLanguage.entries.firstOrNull { it.name == prefs[LanguageKey] } ?: AppLanguage.Indonesian
    }

    // Dipanggil dari main thread (viewModelScope): AppCompat membuat ulang activity yang terbuka.
    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[LanguageKey] = language.name }
        apply(language)
    }

    /**
     * Disimpan sendiri, bukan oleh sistem, agar bawaan tetap Indonesia di HP berbahasa Inggris, dan agar
     * Android < 13 (tanpa bahasa per app) ikut memakainya. Dipanggil di awal proses sebelum activity dibuat.
     */
    suspend fun applyStoredLanguage() = apply(language.first())

    private fun apply(language: AppLanguage) {
        val tag = when (language) {
            AppLanguage.Indonesian -> "id"
            AppLanguage.English -> "en"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // AppCompat di Android 13+ butuh activity yang sudah hidup; tanpa itu (awal proses) panggilannya diabaikan.
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private companion object {
        val SelectedGroupIdKey = stringPreferencesKey("selected_group_id")
        val ThemeModeKey = stringPreferencesKey("theme_mode")
        val LanguageKey = stringPreferencesKey("language")
    }
}
