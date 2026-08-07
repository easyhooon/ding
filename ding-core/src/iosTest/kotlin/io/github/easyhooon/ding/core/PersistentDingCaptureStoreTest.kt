package io.github.easyhooon.ding.core

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentDingCaptureStoreTest {
    @Test
    fun snapshotsAndTokensPersistInTheRoomStore() = runTest {
        val storagePath = temporaryPersistentStorePath()
        val store = PersistentDingCaptureStore.get(storagePath, maxSnapshots = 2)
        store.append("first")
        store.append("second")
        store.append("third")
        store.updateRegistrationToken(RegistrationTokenKind.FCM, "fcm-token")
        store.updateRegistrationToken(RegistrationTokenKind.APNS, "apns-token")

        val sameStore = PersistentDingCaptureStore.get(storagePath, maxSnapshots = 2)

        assertSame(store, sameStore)
        assertEquals(listOf("second", "third"), sameStore.snapshots())
        assertEquals("fcm-token", sameStore.registrationToken(RegistrationTokenKind.FCM))
        assertEquals("apns-token", sameStore.registrationToken(RegistrationTokenKind.APNS))
        assertTrue(persistentStoreFileExists(storagePath))
    }

    @Test
    fun clearingSnapshotsKeepsPersistedTokens() = runTest {
        val store = PersistentDingCaptureStore.get(temporaryPersistentStorePath())
        store.append("snapshot")
        store.updateRegistrationToken(RegistrationTokenKind.FCM, "fcm-token")

        store.clearSnapshots()

        assertEquals(emptyList(), store.snapshots())
        assertEquals("fcm-token", store.registrationToken(RegistrationTokenKind.FCM))
    }

    @Test
    fun openingRoomStoreAppliesRetentionToExistingRows() = runTest {
        val storagePath = temporaryPersistentStorePath()
        val database = Room.databaseBuilder<DingDatabase>(
            name = storagePath,
            factory = { DingDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        database.dingDao().apply {
            insertSnapshot(NotificationSnapshotEntity.from("first"))
            insertSnapshot(NotificationSnapshotEntity.from("second"))
            insertSnapshot(NotificationSnapshotEntity.from("third"))
        }
        database.close()

        val store = PersistentDingCaptureStore.get(storagePath, maxSnapshots = 2)

        assertEquals(listOf("second", "third"), store.snapshots())
    }

    @Test
    fun aPathCannotBeReopenedWithDifferentRetention() = runTest {
        val storagePath = temporaryPersistentStorePath()
        PersistentDingCaptureStore.get(storagePath, maxSnapshots = 2)

        val result = runCatching {
            PersistentDingCaptureStore.get(storagePath, maxSnapshots = 3)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun storagePathMustUseTheRoomDatabaseExtension() = runTest {
        val result = runCatching {
            PersistentDingCaptureStore.get("invalid-path")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
