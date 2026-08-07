package io.github.easyhooon.ding.core

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersistentDingCaptureStoreTest {
    @Test
    fun snapshotsAndTokensPersistInThePathBackedStore() = runTest {
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
    fun aPathCannotBeReopenedWithDifferentRetention() = runTest {
        val storagePath = temporaryPersistentStorePath()
        PersistentDingCaptureStore.get(storagePath, maxSnapshots = 2)

        val result = runCatching {
            PersistentDingCaptureStore.get(storagePath, maxSnapshots = 3)
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun storagePathMustUseTheDataStoreExtension() = runTest {
        val result = runCatching {
            PersistentDingCaptureStore.get("invalid-path")
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
