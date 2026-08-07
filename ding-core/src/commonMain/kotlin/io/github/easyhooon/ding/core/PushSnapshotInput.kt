package io.github.easyhooon.ding.core

public enum class PushPlatform(public val jsonValue: String) {
    ANDROID("android"),
    IOS("ios"),
}

public enum class PushTransport(public val jsonValue: String) {
    FCM("fcm"),
    FCM_APNS("fcm-apns"),
    APNS("apns"),
    LOCAL("local"),
    UNKNOWN("unknown"),
}

public enum class CapturePoint(public val jsonValue: String) {
    HOST_CALLBACK("host-callback"),
    HOST_API("host-api"),
    FOREGROUND("foreground"),
    BACKGROUND_CALLBACK("background-callback"),
    NOTIFICATION_RESPONSE("notification-response"),
    SERVICE_EXTENSION("service-extension"),
}

public enum class RegistrationTokenKind(public val jsonValue: String) {
    FCM("fcm"),
    APNS("apns"),
}

public data class RegistrationTokenSnapshotInput(
    public val kind: RegistrationTokenKind,
    public val value: String,
)

public data class ApplePushSnapshotInput(
    public val userInfo: Map<*, *>,
    public val transport: PushTransport,
    public val capturePoint: CapturePoint,
    public val registrationToken: RegistrationTokenSnapshotInput?,
)
