package com.iqbal.journal.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Preferensi kecil per HP yang bukan data bersama. */
@Singleton
class UserPreferences @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val selectedGroupId: Flow<String?> = dataStore.data.map { it[SelectedGroupIdKey] }

    suspend fun setSelectedGroupId(groupId: String) {
        dataStore.edit { it[SelectedGroupIdKey] = groupId }
    }

    private companion object {
        val SelectedGroupIdKey = stringPreferencesKey("selected_group_id")
    }
}
