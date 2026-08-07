package io.github.easyhooon.ding.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okio.Path.Companion.toPath

public class PersistentDingCaptureStore private constructor(
    private val dataStore: DataStore<Preferences>,
    private val maxSnapshots: Int,
) : DingCaptureStore {
    override suspend fun append(snapshotJson: String) {
        dataStore.edit { preferences ->
            val snapshots = decodeSnapshots(preferences[SNAPSHOTS_KEY])
            preferences[SNAPSHOTS_KEY] = encodeSnapshots(
                (snapshots + snapshotJson).takeLast(maxSnapshots),
            )
        }
    }

    override suspend fun snapshots(): List<String> =
        decodeSnapshots(dataStore.data.first()[SNAPSHOTS_KEY])

    override suspend fun clearSnapshots() {
        dataStore.edit { preferences -> preferences.remove(SNAPSHOTS_KEY) }
    }

    override suspend fun registrationToken(kind: RegistrationTokenKind): String? =
        dataStore.data.first()[kind.preferencesKey()]

    override suspend fun updateRegistrationToken(
        kind: RegistrationTokenKind,
        value: String,
    ) {
        dataStore.edit { preferences -> preferences[kind.preferencesKey()] = value }
    }

    public companion object {
        private const val DEFAULT_MAX_SNAPSHOTS = 50
        private const val DATA_STORE_EXTENSION = ".preferences_pb"
        private val SNAPSHOTS_KEY = stringPreferencesKey("snapshots")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val APNS_TOKEN_KEY = stringPreferencesKey("apns_token")
        private val storeMutex = Mutex()
        private val stores = mutableMapOf<String, StoreRegistration>()

        public suspend fun get(
            storagePath: String,
            maxSnapshots: Int = DEFAULT_MAX_SNAPSHOTS,
        ): PersistentDingCaptureStore {
            require(storagePath.isNotBlank()) { "storagePath must not be blank" }
            require(storagePath.endsWith(DATA_STORE_EXTENSION)) {
                "storagePath must end with $DATA_STORE_EXTENSION"
            }
            require(maxSnapshots > 0) { "maxSnapshots must be greater than zero" }

            return storeMutex.withLock {
                stores[storagePath]?.let { existing ->
                    require(existing.maxSnapshots == maxSnapshots) {
                        "A store for this path already uses maxSnapshots=${existing.maxSnapshots}"
                    }
                    return@withLock existing.store
                }

                val store = PersistentDingCaptureStore(
                    dataStore = PreferenceDataStoreFactory.createWithPath(
                        produceFile = { storagePath.toPath() },
                    ),
                    maxSnapshots = maxSnapshots,
                )
                stores[storagePath] = StoreRegistration(store, maxSnapshots)
                store
            }
        }

        private fun encodeSnapshots(snapshots: List<String>): String =
            JsonArray(snapshots.map(::JsonPrimitive)).toString()

        private fun decodeSnapshots(value: String?): List<String> {
            value ?: return emptyList()
            return runCatching {
                val jsonArray = Json.parseToJsonElement(value) as? JsonArray
                    ?: return@runCatching emptyList()
                jsonArray.mapNotNull { element ->
                    (element as? JsonPrimitive)?.contentOrNull
                }
            }.getOrDefault(emptyList())
        }

        private fun RegistrationTokenKind.preferencesKey(): Preferences.Key<String> =
            when (this) {
                RegistrationTokenKind.FCM -> FCM_TOKEN_KEY
                RegistrationTokenKind.APNS -> APNS_TOKEN_KEY
            }

        private data class StoreRegistration(
            val store: PersistentDingCaptureStore,
            val maxSnapshots: Int,
        )
    }
}
