package com.agmnetwork.md5analyzer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agmnetwork.md5analyzer.data.model.HistoryEntry
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "agm_md5_prefs")

class DataStoreManager(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val historyListAdapter = moshi.adapter<List<HistoryEntry>>(
        Types.newParameterizedType(List::class.java, HistoryEntry::class.java)
    )

    companion object {
        private val KEY_INSTALLATION_ID = stringPreferencesKey("installation_id")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_TOKEN_EXPIRY = stringPreferencesKey("token_expiry")
        private val KEY_SESSION_ID = stringPreferencesKey("session_id")
        private val KEY_SHORT_URL = stringPreferencesKey("short_url")
        private val KEY_BUBBLE_X = intPreferencesKey("bubble_x")
        private val KEY_BUBBLE_Y = intPreferencesKey("bubble_y")
        private val KEY_HISTORY = stringPreferencesKey("history")
    }

    suspend fun getInstallationId(): String {
        val prefs = context.dataStore.data.first()
        var currentId = prefs[KEY_INSTALLATION_ID]
        if (currentId.isNullOrEmpty()) {
            currentId = UUID.randomUUID().toString()
            context.dataStore.edit { it[KEY_INSTALLATION_ID] = currentId }
        }
        return currentId
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { prefs ->
            prefs[KEY_ACCESS_TOKEN]
        }

    val tokenExpiryFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { prefs ->
            prefs[KEY_TOKEN_EXPIRY]
        }

    val sessionIdFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { prefs ->
            prefs[KEY_SESSION_ID]
        }

    val shortUrlFlow: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { prefs ->
            prefs[KEY_SHORT_URL]
        }

    val bubblePositionFlow: Flow<Pair<Int, Int>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { prefs ->
            val x = prefs[KEY_BUBBLE_X] ?: -1
            val y = prefs[KEY_BUBBLE_Y] ?: -1
            Pair(x, y)
        }

    val historyFlow: Flow<List<HistoryEntry>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }.map { prefs ->
            val jsonString = prefs[KEY_HISTORY]
            if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    historyListAdapter.fromJson(jsonString) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun saveAuthData(accessToken: String, expiry: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_TOKEN_EXPIRY] = expiry
        }
    }

    suspend fun saveSessionData(sessionId: String, shortUrl: String, expiry: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SESSION_ID] = sessionId
            prefs[KEY_SHORT_URL] = shortUrl
            prefs[KEY_TOKEN_EXPIRY] = expiry // reuse token_expiry temporarily to show expiry on lock screen
        }
    }

    suspend fun saveBubblePosition(x: Int, y: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BUBBLE_X] = x
            prefs[KEY_BUBBLE_Y] = y
        }
    }

    suspend fun addHistoryEntry(entry: HistoryEntry) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[KEY_HISTORY]
            val currentList = if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    historyListAdapter.fromJson(jsonString) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }.toMutableList()

            // Prepend new entry
            currentList.add(0, entry)

            // Keep only last 20 entries
            val trimmedList = if (currentList.size > 20) {
                currentList.subList(0, 20)
            } else {
                currentList
            }

            prefs[KEY_HISTORY] = historyListAdapter.toJson(trimmedList)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_TOKEN_EXPIRY)
            prefs.remove(KEY_SESSION_ID)
            prefs.remove(KEY_SHORT_URL)
        }
    }
}
