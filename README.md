# Notification Inspector

Android notification payload inspector for debug builds.

Notification Inspector captures notification-related payload snapshots from real apps and provides a debug UI for inspecting them.

## Current Status

This repository is being extracted from the Hime-incubated implementation.

Initial supported capture paths:

- Firebase `RemoteMessage`
- app-created local notifications

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

## Strategy

See [docs/WORK_STRATEGY.md](docs/WORK_STRATEGY.md).

## Sample

Run the `sample` module and tap:

- `Send Local Notification`
- `Open Inspector`

The captured local notification should appear under the `Local` filter.
