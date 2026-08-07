package io.github.easyhooon.ding.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

public class InMemoryDingCaptureStore(
    private val maxSnapshots: Int = DEFAULT_MAX_SNAPSHOTS,
) : DingCaptureStore {
    private val mutex = Mutex()
    private val storedSnapshots = mutableListOf<String>()
    private val registrationTokens = mutableMapOf<RegistrationTokenKind, String>()

    init {
        require(maxSnapshots > 0) { "maxSnapshots must be greater than zero" }
    }

    override suspend fun append(snapshotJson: String) {
        mutex.withLock {
            val overflow = storedSnapshots.size - maxSnapshots + 1
            repeat(overflow.coerceAtLeast(0)) {
                storedSnapshots.removeAt(0)
            }
            storedSnapshots += snapshotJson
        }
    }

    override suspend fun snapshots(): List<String> =
        mutex.withLock { storedSnapshots.toList() }

    override suspend fun clearSnapshots() {
        mutex.withLock { storedSnapshots.clear() }
    }

    override suspend fun registrationToken(kind: RegistrationTokenKind): String? =
        mutex.withLock { registrationTokens[kind] }

    override suspend fun updateRegistrationToken(
        kind: RegistrationTokenKind,
        value: String,
    ) {
        mutex.withLock { registrationTokens[kind] = value }
    }

    private companion object {
        private const val DEFAULT_MAX_SNAPSHOTS = 50
    }
}
