package io.github.easyhooon.ding.core

public interface DingCaptureStore {
    public suspend fun append(snapshotJson: String)

    public suspend fun snapshots(): List<String>

    public suspend fun clearSnapshots()

    public suspend fun registrationToken(kind: RegistrationTokenKind): String?

    public suspend fun updateRegistrationToken(
        kind: RegistrationTokenKind,
        value: String,
    )
}
