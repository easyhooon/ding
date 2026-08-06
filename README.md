# Notification Inspector

Android notification payload inspector for debug builds.

Notification Inspector captures notification-related payload snapshots from real apps and provides a debug UI for inspecting them.

## Current Status

The first extracted version is feature-complete and publication-ready. The debug and no-op artifacts share version `0.1.0`.

Initial supported capture paths:

- Firebase `RemoteMessage`
- app-created local notifications

Inspector UI:

- compact newest-first Compose message list
- FCM / Local source filters and payload search
- source/category badges
- Overview / Data / Notification / Raw JSON detail tabs
- per-message FCM registration token display, copy, and share actions
- per-section copy, single-message share, and filtered export

## Gradle

```kotlin
dependencies {
    debugImplementation("io.github.easyhooon:notification-inspector:<version>")
    releaseImplementation("io.github.easyhooon:notification-inspector-noop:<version>")
}
```

Until Maven publication is ready, consume the local modules directly from this repository.

## Usage

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    NotificationInspector.capture(this, remoteMessage)
}
```

`RemoteMessage` does not expose the registration token targeted by the sender. Keep Notification Inspector's latest-token cache synchronized from `FirebaseMessagingService.onNewToken`:

```kotlin
override fun onNewToken(token: String) {
    NotificationInspector.updateFcmToken(this, token)
}
```

Also refresh the cache from `FirebaseMessaging.getToken()` at app startup so a restored process does not depend on receiving a new callback:

```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    NotificationInspector.updateFcmToken(applicationContext, token)
}
```

The token is persisted by the debug implementation; subsequent two-argument `capture()` calls automatically attach the latest value to each new snapshot. Historical snapshots keep their original token when it changes.

If the host already has the token at capture time, the explicit overload records it and also refreshes the latest-token cache:

```kotlin
NotificationInspector.capture(
    context = this,
    remoteMessage = remoteMessage,
    fcmToken = latestFcmToken,
)
```

Messages captured before a token is registered show `Not captured` in the FCM token section. The stored value is host-supplied context, not proof that the sender targeted that token; topic and condition messages may not target one registration token directly.

```kotlin
NotificationInspector.captureNotification(
    context = context,
    source = "notification-test",
    notificationId = notificationId,
    title = title,
    body = body,
    data = mapOf("thread-id" to threadId),
)
```

The persistent inspector notification is opt-in. It only shows the captured count and latest category, and tapping it opens the inspector.

```kotlin
NotificationInspector.setPersistentNotificationEnabled(context, enabled = true)
```

Call the same API with `enabled = false` to remove it. On Android 13 and newer, the host app remains responsible for requesting `POST_NOTIFICATIONS` at an appropriate time.

## Strategy

See [docs/WORK_STRATEGY.md](docs/WORK_STRATEGY.md).

## Sample

Run the `sample` module and tap:

- `Send Local Notification`
- `Capture Mock FCM Message`
- `Rotate Sample FCM Token`
- `Enable Persistent Inspector`
- `Disable Persistent Inspector`
- `Open Inspector`

The captured local notification appears under the `Local` filter, and the mock remote message appears under `FCM`.

## Privacy

Notification payloads can contain personal data, authentication material, or internal identifiers. FCM registration tokens identify app instances and are included in raw JSON and exports when captured. Keep the inspector on debug builds, review copied or exported content before sharing it, and never attach raw production payloads or tokens to public issues.
