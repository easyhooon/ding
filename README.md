# Notification Inspector

Android notification payload inspector for debug builds.

Notification Inspector captures notification-related payload snapshots from real apps and provides a debug UI for inspecting them.

## Current Status

The first extracted version is feature-complete and publication-ready. The debug and no-op artifacts share version `0.1.0`.

Initial supported capture paths:

- Firebase `RemoteMessage`
- app-created local notifications

Inspector UI:

- dynamic `Inspector` shortcut on the host app icon
- persistent debug notification that opens the inspector when tapped
- compact newest-first Compose message list
- FCM / Local source filters and payload search
- source/category badges
- Overview / Data / Notification / Raw JSON detail tabs
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

The debug implementation initializes automatically when the host app starts. Long-press the host app icon and select `Inspector`, or tap the persistent Inspector notification, to open the captured payload list. The library no longer adds a separate launcher icon.

The persistent notification is enabled by default. It only shows the captured count and latest category. Disable it when the notification entry point is not wanted:

```kotlin
NotificationInspector.setPersistentNotificationEnabled(context, enabled = false)
```

Call the same API with `enabled = true` to restore it. The explicit choice persists across process restarts. On Android 13 and newer, the host app remains responsible for requesting `POST_NOTIFICATIONS` at an appropriate time.

## Strategy

See [docs/WORK_STRATEGY.md](docs/WORK_STRATEGY.md).

## Sample

Run the `sample` module and tap:

- `Send Local Notification`
- `Capture Mock FCM Message`
- `Enable Persistent Inspector`
- `Disable Persistent Inspector`
- `Open Inspector`

The captured local notification appears under the `Local` filter, and the mock remote message appears under `FCM`.

## Privacy

Notification payloads can contain personal data, authentication material, or internal identifiers. Keep the inspector on debug builds, review exported content before sharing it, and never attach raw production payloads to public issues.
