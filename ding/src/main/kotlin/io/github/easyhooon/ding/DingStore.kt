package io.github.easyhooon.ding

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

internal class DingStore(context: Context) {
    private val appContext = context.applicationContext
    private val packageName = appContext.packageName
    private val dataStore = dataStore(appContext)

    fun appendAsync(
        snapshot: JSONObject,
        onStored: suspend () -> Unit = {},
    ) {
        scope.launch {
            runCatching {
                append(snapshot)
                onStored()
            }.onFailure { error ->
                Log.w(TAG, "Failed to store notification snapshot", error)
            }
        }
    }

    fun appendWithFcmTokenAsync(
        suppliedFcmToken: String?,
        snapshotProvider: (String?) -> JSONObject,
        onStored: suspend () -> Unit = {},
    ) {
        enqueueFcmTokenOperation {
            runCatching {
                val fcmToken = suppliedFcmToken ?: readFcmToken()
                val snapshot = snapshotProvider(fcmToken)
                append(snapshot)
                onStored()
            }.onFailure { error ->
                Log.w(TAG, "Failed to store notification snapshot", error)
            }
        }
    }

    fun updateFcmTokenAsync(fcmToken: String) {
        enqueueFcmTokenOperation {
            runCatching {
                cacheFcmToken(fcmToken)
                setFcmToken(fcmToken)
            }.onFailure { error ->
                Log.w(TAG, "Failed to store FCM registration token", error)
            }
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

    suspend fun setPersistentNotificationEnabled(enabled: Boolean) {
        mutex.withLock {
            dataStore.edit { preferences ->
                preferences[KEY_PERSISTENT_NOTIFICATION_ENABLED] = enabled
            }
        }
    }

    suspend fun initializePersistentNotificationEnabled(defaultEnabled: Boolean): Boolean {
        return mutex.withLock {
            val existing = readPreferences()[KEY_PERSISTENT_NOTIFICATION_ENABLED]
            if (existing != null) {
                existing
            } else {
                dataStore.edit { preferences ->
                    preferences[KEY_PERSISTENT_NOTIFICATION_ENABLED] = defaultEnabled
                }
                defaultEnabled
            }
        }
    }

    suspend fun isPersistentNotificationEnabled(): Boolean {
        return mutex.withLock {
            readPreferences()[KEY_PERSISTENT_NOTIFICATION_ENABLED] ?: false
        }
    }

    suspend fun readDarkMode(): Boolean? {
        return mutex.withLock {
            readPreferences()[KEY_DARK_MODE]
        }
    }

    suspend fun setDarkMode(value: Boolean?) {
        mutex.withLock {
            dataStore.edit { preferences ->
                if (value == null) {
                    preferences.remove(KEY_DARK_MODE)
                } else {
                    preferences[KEY_DARK_MODE] = value
                }
            }
        }
    }

    suspend fun readFcmToken(): String? {
        cachedFcmToken()?.let { return it }
        return mutex.withLock {
            readPreferences()[KEY_FCM_TOKEN]
        }?.also(::cacheFcmToken)
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

    private suspend fun setFcmToken(fcmToken: String) {
        mutex.withLock {
            dataStore.edit { preferences ->
                preferences[KEY_FCM_TOKEN] = fcmToken
            }
        }
    }

    private suspend fun readMessages(): JSONArray {
        return parseArray(readPreferences()[KEY_MESSAGES])
    }

    private suspend fun readPreferences(): Preferences {
        return runCatching {
            dataStore.data.first()
        }.recoverCatching { error ->
            if (error is IOException) emptyPreferences() else throw error
        }.getOrThrow()
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

    private fun cachedFcmToken(): String? {
        return synchronized(fcmTokenCache) {
            fcmTokenCache[packageName]
        }
    }

    private fun cacheFcmToken(fcmToken: String) {
        synchronized(fcmTokenCache) {
            fcmTokenCache[packageName] = fcmToken
        }
    }

    private fun enqueueFcmTokenOperation(operation: suspend () -> Unit) {
        if (fcmTokenOperations.trySend(operation).isFailure) {
            Log.w(TAG, "Failed to enqueue FCM token operation")
        }
    }

    private companion object {
        private const val TAG = "Ding"
        private const val DATA_STORE_NAME = "notification_inspector.preferences_pb"
        private const val KEY_MESSAGES_NAME = "messages"
        private const val KEY_PERSISTENT_NOTIFICATION_ENABLED_NAME = "persistent_notification_enabled"
        private const val KEY_FCM_TOKEN_NAME = "fcm_token"
        private const val KEY_DARK_MODE_NAME = "dark_mode"
        private const val MAX_MESSAGES = 50
        private val KEY_MESSAGES = stringPreferencesKey(KEY_MESSAGES_NAME)
        private val KEY_PERSISTENT_NOTIFICATION_ENABLED =
            booleanPreferencesKey(KEY_PERSISTENT_NOTIFICATION_ENABLED_NAME)
        private val KEY_FCM_TOKEN = stringPreferencesKey(KEY_FCM_TOKEN_NAME)
        private val KEY_DARK_MODE = booleanPreferencesKey(KEY_DARK_MODE_NAME)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val mutex = Mutex()
        private val fcmTokenOperations =
            Channel<suspend () -> Unit>(capacity = Channel.UNLIMITED).also { operations ->
                scope.launch {
                    for (operation in operations) {
                        operation()
                    }
                }
            }
        private val stores = mutableMapOf<String, DataStore<Preferences>>()
        private val fcmTokenCache = mutableMapOf<String, String>()

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
