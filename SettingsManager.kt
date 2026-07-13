package com.piyushkuca.androidvibe

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create a single instance of DataStore attached to the Context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val API_KEY = stringPreferencesKey("nvidia_api_key")
    }

    // Get the API Key as a stream (Flow)
    val apiKeyFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[API_KEY]
        }

    // Save the API Key
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }
    
    // Clear the API Key (useful for a "Logout" or "Clear Key" feature)
    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }
}
