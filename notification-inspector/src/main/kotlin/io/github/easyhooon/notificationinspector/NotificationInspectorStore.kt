package io.github.easyhooon.notificationinspector

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

internal class NotificationInspectorStore(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = dataStore(appContext)

    fun appendAsync(snapshot: JSONObject) {
        scope.launch {
            append(snapshot)
        }
    }

    suspend fun readAll(): List<JSONObject> {
        return mutex.withLock {
            readMessages().toJsonObjects()
        }
    }

    suspend fun clear() {
        mutex.withLock {
            dataStore.edit { preferences ->
                preferences.remove(KEY_MESSAGES)
            }
        }
    }

    private suspend fun append(snapshot: JSONObject) {
        mutex.withLock {
            dataStore.edit { preferences ->
                val current = parseArray(preferences[KEY_MESSAGES])
                val next = JSONArray()

                val startIndex = (current.length() - MAX_MESSAGES + 1).coerceAtLeast(0)
                for (index in startIndex until current.length()) {
                    next.put(current.getJSONObject(index))
                }
                next.put(snapshot)

                preferences[KEY_MESSAGES] = next.toString()
            }
        }
    }

    private suspend fun readMessages(): JSONArray {
        val preferences = runCatching {
            dataStore.data.first()
        }.recoverCatching { error ->
            if (error is IOException) emptyPreferences() else throw error
        }.getOrThrow()

        return parseArray(preferences[KEY_MESSAGES])
    }

    private fun parseArray(raw: String?): JSONArray {
        return raw?.let { runCatching { JSONArray(it) }.getOrNull() } ?: JSONArray()
    }

    private fun JSONArray.toJsonObjects(): List<JSONObject> {
        return buildList {
            for (index in 0 until length()) {
                add(getJSONObject(index))
            }
        }
    }

    private companion object {
        private const val DATA_STORE_NAME = "notification_inspector.preferences_pb"
        private const val KEY_MESSAGES_NAME = "messages"
        private const val MAX_MESSAGES = 50
        private val KEY_MESSAGES = stringPreferencesKey(KEY_MESSAGES_NAME)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutex = Mutex()
        private val stores = mutableMapOf<String, DataStore<Preferences>>()

        private fun dataStore(context: Context): DataStore<Preferences> {
            return synchronized(stores) {
                stores.getOrPut(context.packageName) {
                    PreferenceDataStoreFactory.create(
                        scope = scope,
                        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) },
                    )
                }
            }
        }
    }
}
