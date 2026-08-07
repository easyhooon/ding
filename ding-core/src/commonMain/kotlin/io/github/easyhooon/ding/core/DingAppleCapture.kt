package io.github.easyhooon.ding.core

public class DingAppleCapture(
    private val store: DingCaptureStore,
) {
    public suspend fun captureAppleUserInfo(
        userInfo: Map<*, *>,
        transport: PushTransport,
        capturePoint: CapturePoint,
    ): String = captureAppleUserInfo(
        userInfo = userInfo,
        transport = transport,
        capturePoint = capturePoint,
        receivedAtMillis = currentTimeMillis(),
    )

    public suspend fun captureAppleUserInfo(
        userInfo: Map<*, *>,
        transport: PushTransport,
        capturePoint: CapturePoint,
        receivedAtMillis: Long,
    ): String {
        val tokenKind = transport.registrationTokenKind()
        val token = tokenKind?.let { kind ->
            store.registrationToken(kind)?.let { value ->
                RegistrationTokenSnapshotInput(kind = kind, value = value)
            }
        }
        val snapshot = DingSnapshotJson.applePush(
            input = ApplePushSnapshotInput(
                userInfo = userInfo,
                transport = transport,
                capturePoint = capturePoint,
                registrationToken = token,
            ),
            receivedAtMillis = receivedAtMillis,
        )
        store.append(snapshot)
        return snapshot
    }

    public suspend fun updateFcmToken(token: String) {
        updateToken(RegistrationTokenKind.FCM, token)
    }

    public suspend fun updateApnsToken(token: String) {
        updateToken(RegistrationTokenKind.APNS, token)
    }

    public suspend fun snapshots(): List<String> = store.snapshots()

    public suspend fun clearSnapshots() {
        store.clearSnapshots()
    }

    private suspend fun updateToken(
        kind: RegistrationTokenKind,
        token: String,
    ) {
        token.trim()
            .takeIf { it.isNotEmpty() }
            ?.let { store.updateRegistrationToken(kind, it) }
    }

    private fun PushTransport.registrationTokenKind(): RegistrationTokenKind? =
        when (this) {
            PushTransport.FCM, PushTransport.FCM_APNS -> RegistrationTokenKind.FCM
            PushTransport.APNS -> RegistrationTokenKind.APNS
            PushTransport.LOCAL, PushTransport.UNKNOWN -> null
        }
}
