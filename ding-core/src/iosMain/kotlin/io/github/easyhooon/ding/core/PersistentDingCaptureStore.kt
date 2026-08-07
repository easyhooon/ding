package io.github.easyhooon.ding.core

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class PersistentDingCaptureStore private constructor(
    private val dao: DingDao,
    private val maxSnapshots: Int,
) : DingCaptureStore {
    override suspend fun append(snapshotJson: String) {
        dao.insertAndTrim(
            snapshot = NotificationSnapshotEntity.from(snapshotJson),
            maxSnapshots = maxSnapshots,
        )
    }

    override suspend fun snapshots(): List<String> = dao.snapshots()

    override suspend fun clearSnapshots() {
        dao.clearSnapshots()
    }

    override suspend fun registrationToken(kind: RegistrationTokenKind): String? =
        dao.registrationToken(kind.jsonValue)

    override suspend fun updateRegistrationToken(
        kind: RegistrationTokenKind,
        value: String,
    ) {
        dao.upsertRegistrationToken(
            RegistrationTokenEntity(kind = kind.jsonValue, value = value),
        )
    }

    public companion object {
        private const val DEFAULT_MAX_SNAPSHOTS = 50
        private const val DATABASE_EXTENSION = ".db"
        private val storeMutex = Mutex()
        private val stores = mutableMapOf<String, StoreRegistration>()

        public suspend fun get(
            storagePath: String,
            maxSnapshots: Int = DEFAULT_MAX_SNAPSHOTS,
        ): PersistentDingCaptureStore {
            require(storagePath.isNotBlank()) { "storagePath must not be blank" }
            require(storagePath.endsWith(DATABASE_EXTENSION)) {
                "storagePath must end with $DATABASE_EXTENSION"
            }
            require(maxSnapshots > 0) { "maxSnapshots must be greater than zero" }

            return storeMutex.withLock {
                stores[storagePath]?.let { existing ->
                    require(existing.maxSnapshots == maxSnapshots) {
                        "A store for this path already uses maxSnapshots=${existing.maxSnapshots}"
                    }
                    return@withLock existing.store
                }

                val database = Room.databaseBuilder<DingDatabase>(
                    name = storagePath,
                    factory = { DingDatabaseConstructor.initialize() },
                ).setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.Default)
                    .build()
                val store = PersistentDingCaptureStore(
                    dao = database.dingDao(),
                    maxSnapshots = maxSnapshots,
                )
                stores[storagePath] = StoreRegistration(store, maxSnapshots)
                store
            }
        }

        private data class StoreRegistration(
            val store: PersistentDingCaptureStore,
            val maxSnapshots: Int,
        )
    }
}
