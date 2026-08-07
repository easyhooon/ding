# KMP and iOS push payload support

Date: 2026-08-07

## Decision summary

Supporting Ding in a Kotlin Multiplatform project is feasible, including capture of push payloads on iOS. It should not, however, be described as capturing the same raw FCM request on both platforms.

- Android can capture the Android client representation exposed as `RemoteMessage`.
- iOS can capture the APNs payload delivered as `UNNotificationContent.userInfo`, whether the sender used FCM's APNs interface or APNs directly.
- Android does not receive the sender's `apns` configuration, and iOS does not receive the sender's `android` configuration.
- Neither client receives the original FCM HTTP v1 request envelope. Reconstructing that envelope, including both platform blocks and APNs request headers, requires server-side logging or explicitly embedding a safe copy in custom data.
- KMP can share the normalized snapshot model, persistence logic, filtering, export, and most inspector UI logic. Receipt callbacks and entry points remain platform-specific.
- The lowest-risk first step is additive: preserve the existing `io.github.easyhooon:ding` Android artifact and API, introduce a KMP core plus an Apple adapter/framework, and avoid a required Firebase Apple SDK dependency.
- Complete capture is not guaranteed on either platform. In particular, iOS background notifications are opportunistic, and alert notifications delivered while the app is not running are not observed at arrival by the app unless the notification is eligible for a Notification Service Extension.

## What each platform can actually observe

FCM HTTP v1 defines a common `notification` and `data` section plus separate `android`, `apns`, and `webpush` configurations. `ApnsConfig` itself separates APNs `headers` from its `payload`. The common notification is a cross-platform template, while platform-specific fields override or augment it. See the [FCM `Message` REST resource](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages) and [cross-platform customization guide](https://firebase.google.com/docs/cloud-messaging/customize-messages/cross-platform).

The result is a platform-specific delivery, not a copy of the send request:

| Sender field | Android client | iOS client |
| --- | --- | --- |
| Common `data` | Available through `RemoteMessage.data` when the app callback runs | Delivered as custom keys in APNs `userInfo` when an iOS callback runs |
| Common `notification` | Exposed as the resolved `RemoteMessage.Notification` when applicable | Converted into the APNs `aps.alert` representation |
| `android` | Applied to Android delivery; selected resolved fields are exposed by `RemoteMessage` | Not delivered |
| `apns.payload` | Not delivered | Delivered as the APNs payload, including `aps` and custom keys |
| `apns.headers` | Not delivered | Used by APNs transport; not part of `userInfo` |
| Complete FCM HTTP v1 envelope | Not delivered | Not delivered |

The last four statements are an inference from the first-party send schema and receive APIs: Android's documented callback receives a `RemoteMessage`, while Firebase's Apple receive guide reads the full received message from `notification.request.content.userInfo`; neither client API exposes the opposite platform's configuration or the original request object. See [receive messages on Android](https://firebase.google.com/docs/cloud-messaging/android/receive-messages) and [receive messages on Apple platforms](https://firebase.google.com/docs/cloud-messaging/ios/receive-messages).

This distinction should be visible in Ding's terminology:

- Call the platform-preserved object `raw delivered payload`, not `raw FCM request`.
- Record `platform` separately from `transport`, for example `platform = ios` and `transport = fcm-apns` or `apns`.
- Preserve both a raw platform payload and a normalized overview. Do not force APNs `aps` into Android's `notification` schema or vice versa.

## Capture points and limitations

### Android

The current integration remains valid:

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    Ding.capture(this, remoteMessage)
}
```

`FirebaseMessagingService.onMessageReceived` receives foreground notification, data, and combined messages. In the background, data-only messages reach the callback, but notification messages are displayed by the SDK in the system tray; for a combined notification-and-data message, the data is placed in the launcher activity intent after the user taps. This means the existing Ding callback path cannot claim complete arrival-time coverage. See Firebase's [Android message handling table](https://firebase.google.com/docs/cloud-messaging/android/receive-messages) and [`FirebaseMessagingService` reference](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService).

This limitation exists today and is independent of KMP. An optional Android intent capture helper could improve tap-time coverage later, but it would not be the same as receiving `onMessageReceived` at arrival time.

### iOS app callbacks

FCM sends every message targeting an Apple app through APNs. Firebase instructs apps to read the payload from these platform callbacks:

- `UNUserNotificationCenterDelegate.userNotificationCenter(_:willPresent:)` for an alert arriving while the app is in the foreground.
- `UNUserNotificationCenterDelegate.userNotificationCenter(_:didReceive:)` after the user interacts with a delivered notification.
- `UIApplicationDelegate.application(_:didReceiveRemoteNotification:fetchCompletionHandler:)` for silent/background pushes.

All three expose the APNs dictionary as `userInfo`. They are therefore sufficient for a Firebase-independent Ding capture function such as `captureAppleUserInfo(userInfo:)`. See Firebase's [Apple receive guide](https://firebase.google.com/docs/cloud-messaging/ios/receive-messages) and Apple's [notification handling guide](https://developer.apple.com/documentation/usernotifications/handling-notifications-and-notification-related-actions).

Coverage differs by application state:

| iOS situation | Best capture point | Coverage |
| --- | --- | --- |
| Alert while foreground | `willPresent` | Arrival-time capture |
| Alert while background/terminated | `didReceive` | Only after user interaction |
| Silent/background push | `didReceiveRemoteNotification` | Best effort; delivery is not guaranteed |
| Eligible alert with service extension | `UNNotificationServiceExtension.didReceive` | Arrival-time capture before display |

Apple explicitly treats background notifications as low priority, does not guarantee delivery, and may throttle them. See [Pushing background updates to your app](https://developer.apple.com/documentation/usernotifications/pushing-background-updates-to-your-app). Consequently, Ding must describe iOS capture as callback-based and best effort rather than an authoritative delivery log.

### Notification Service Extension

A Notification Service Extension can capture certain alerts before the system displays them, even when the main app is not active. It is optional and should be a later integration tier rather than a baseline requirement.

Apple's constraints are material:

- The push must have `mutable-content: 1` inside `aps`.
- The payload must contain an alert with title, subtitle, or body information.
- The extension only operates for remote notifications configured to display an alert; it is not a general interceptor for silent, sound-only, or badge-only pushes.
- The extension has about 30 seconds to finish and must call its content handler.
- The extension is a separate bundle and process from the app.

See Apple's [Modifying content in newly delivered notifications](https://developer.apple.com/documentation/usernotifications/modifying-content-in-newly-delivered-notifications) and [`UNNotificationServiceExtension`](https://developer.apple.com/documentation/usernotifications/unnotificationserviceextension).

Because it is a separate process, an extension cannot rely on Ding's in-process singleton or the main app's private container. The app and extension must enable the same App Group and store snapshots in the shared group container. Apple documents that App Groups allow members from the same team to access a shared container; see the [App Groups entitlement](https://developer.apple.com/documentation/BundleResources/Entitlements/com.apple.security.application-groups).

Even with an extension, Ding cannot guarantee every iOS push is captured. It improves arrival-time coverage only for eligible mutable alert notifications.

## Firebase Apple SDK and Kotlin/Native interop

There is no need for Ding's baseline Apple adapter to depend on Firebase. `userInfo` is an APNs representation and is present whether the provider sent through FCM or directly through APNs. Keeping the adapter Firebase-independent has three advantages:

1. APNs-only apps can use Ding.
2. Host apps retain their existing delegate and Firebase setup.
3. Ding avoids forcing Firebase's Apple SDK, CocoaPods, and cinterop constraints on all KMP consumers.

The FCM registration token should remain host-supplied, matching Ding's Android design. Firebase's Apple guide exposes token changes through `MessagingDelegate.messaging(_:didReceiveRegistrationToken:)` and notes that it is called at startup and when a new token is generated. The host can forward that value to `Ding.updateFcmToken(...)`. Firebase also distinguishes the APNs device token from the FCM registration token and maps them automatically through method swizzling unless the app disables it; see [Apple FCM setup](https://firebase.google.com/docs/cloud-messaging/ios/get-started) and the [`Messaging` API reference](https://firebase.google.com/docs/reference/swift/firebasemessaging/api/reference/Classes/Messaging).

An optional Firebase-specific Apple adapter is technically possible:

- Firebase distributes `FirebaseMessaging` through Swift Package Manager and CocoaPods, and documents the `FirebaseMessaging` pod in its [Apple setup guide](https://firebase.google.com/docs/ios/setup).
- Kotlin/Native can use Objective-C libraries through cinterop, and Kotlin's CocoaPods plugin can add native pod dependencies; see [Kotlin CocoaPods integration](https://kotlinlang.org/docs/multiplatform/multiplatform-cocoapods-overview.html) and [Swift/Objective-C interoperability](https://kotlinlang.org/docs/native-objc-interop.html).

However, Kotlin marks generated Objective-C interop declarations as Beta, [CocoaPods/cinterop Apple builds require a macOS host](https://kotlinlang.org/docs/native-target-support.html), and [Firebase recommends Swift Package Manager for new Apple projects](https://firebase.google.com/docs/ios/installation-methods) while KMP's most direct first-party native dependency workflow is CocoaPods. Those trade-offs are not justified merely to receive `userInfo` or forward a token callback. A Firebase adapter should only be added if it later provides substantial automation beyond those two operations.

## Evaluation: `ienground/firebase-kotlin-sdk`

### Recommendation

Do not make [`ienground/firebase-kotlin-sdk`](https://github.com/ienground/firebase-kotlin-sdk) a required dependency of Ding's KMP core or baseline Apple adapter. It is relevant as an optional interoperability target for apps that already use it, especially for FCM token updates, but it does not eliminate the host-owned iOS notification callbacks Ding needs and its `RemoteMessage` conversion is intentionally too lossy for Ding's raw-payload inspector.

A later `ding-firebase-kotlin` adapter or documentation example could support its common `RemoteMessage` and `tokenUpdates`. The primary Apple API should still accept the original APNs `userInfo`, and APNs-only apps should not be forced to link Firebase.

### Project and distribution status

As of 2026-08-07, the project is new but actively developed: the repository was created on 2026-06-30, published [`v1.0.0` on 2026-07-28](https://github.com/ienground/firebase-kotlin-sdk/releases/tag/v1.0.0), and its GitHub contributor history currently shows one contributor. This is enough to justify a compatibility experiment, but not enough operational history to make it a foundational Ding dependency. The code is [Apache-2.0 licensed](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/LICENSE).

The messaging artifact is `zone.ien.firebase:firebase-messaging:1.0.0`; the repository generates that group and module coordinate in its [publishing configuration](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/build.gradle.kts#L16-L38). Its current README installation example still refers to nonexistent or stale `1.0.0-beta03` coordinates while [Maven Central metadata](https://repo.maven.apache.org/maven2/zone/ien/firebase/firebase-messaging/maven-metadata.xml) lists `1.0.0-beta01`, `beta02`, `beta08`, and `1.0.0`; that documentation mismatch is a small maturity warning.

The published module is built with Kotlin `2.4.10`, AGP `9.2.1`, kotlinx.coroutines `1.11.0`, Android minSdk 29, Firebase Android Messaging `25.1.1`, and Firebase Apple SDK `12.14.0`; see the [version catalog](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/gradle/libs.versions.toml#L1-L12) and [Apple SDK version](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/gradle/libs.versions.toml#L42). It publishes Android, `iosArm64`, and `iosSimulatorArm64` variants only; there is no `iosX64`, macOS, tvOS, or watchOS target in the [messaging module](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/build.gradle.kts#L12-L48). Ding currently uses Kotlin `2.3.20` and AGP `9.0.1`, so adopting version 1.0.0 would also force an immediate Kotlin/Android build-tool upgrade.

### What its messaging API provides

The common API is more than a token/topic wrapper. It provides:

- `messages: Flow<RemoteMessage>` backed by a process-global replay buffer;
- `tokenUpdates: Flow<String>`;
- token get/delete and topic subscribe/unsubscribe operations;
- a common `RemoteMessage` model and `handleMessage` bridge;
- Android conversion from Google's `RemoteMessage` and an opt-in `FirebaseMessagingService`;
- iOS conversion from APNs `userInfo` through `remoteMessageFromUserInfo`.

These surfaces are visible in the [common messaging API](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/src/commonMain/kotlin/zone/ien/firebase/messaging/FirebaseMessaging.kt#L17-L100), [Android bridge](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/src/androidMain/kotlin/zone/ien/firebase/messaging/FirebaseMessaging.android.kt#L79-L126), and [iOS implementation](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/src/iosMain/kotlin/zone/ien/firebase/messaging/FirebaseMessaging.ios.kt#L20-L131).

It does not automatically intercept received iOS notifications. The host must still receive `userInfo` in its `UIApplicationDelegate` or `UNUserNotificationCenterDelegate`, call `remoteMessageFromUserInfo`, and then call `handleMessage`; the project's own [README states this explicitly](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/README.md#L161-L169), and its [sample callback leaves the bridge calls commented out](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/example/iosApp/iosApp/iOSApp.swift#L70-L88). Therefore it offers a common event bus after host forwarding, not an OS-level payload callback Ding can subscribe to automatically.

Its APNs conversion also cannot replace Ding's raw capture. The converter extracts `aps.alert` title/body and selected FCM metadata, drops the entire `aps` object, filters Google/GCM keys, and retains only custom values that are strings; nested dictionaries, arrays, badge, sound, interruption level, mutable/content-available flags, and non-string custom values do not survive in `RemoteMessage.data`. See [`remoteMessageFromApplePayload`](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/src/commonMain/kotlin/zone/ien/firebase/messaging/FirebaseMessaging.kt#L215-L243). Ding must capture and recursively normalize the original `userInfo` before any such conversion.

The most useful reusable feature for Ding is token handling. On iOS the wrapper links `FIRMessaging`, queries/deletes the FCM token, supports topics, and exposes token updates without replacing `FIRMessaging.delegate`; see the [iOS token implementation](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/src/iosMain/kotlin/zone/ien/firebase/messaging/FirebaseMessaging.ios.kt#L35-L105). An app already using this SDK could collect `Firebase.messaging.tokenUpdates` and forward values to Ding. This should be optional because Ding can obtain the same value from the host's existing Firebase delegate and APNs-only apps have no FCM token.

### SwiftPM and transitive-cost assessment

The library itself is consumed as a KMP Maven dependency, not as a standalone Swift package. Its iOS implementation uses Kotlin's `swiftPMDependencies` DSL to pull the official `FirebaseMessaging` product from `firebase/firebase-ios-sdk` and publishes SwiftPM dependency metadata; see the [messaging build configuration](https://github.com/ienground/firebase-kotlin-sdk/blob/d1886c9bc9f719c285de0bb0c811be9ed5affe39/firebase-messaging/build.gradle.kts#L50-L70). The repository's generated Xcode linkage package confirms that the host resolves Firebase Apple SDK `12.14.0` through SwiftPM. Android transitively adds the official Firebase Messaging SDK, Firebase BoM, coroutines Play Services, and the wrapper's common/components modules; Apple adds the wrapper's common/components modules, coroutines, and native `FirebaseCore` plus `FirebaseMessaging` products.

This is directionally aligned with Ding's preference for SwiftPM over CocoaPods, but it is not currently a low-risk basis for Ding's planned binary XCFramework distribution:

- Kotlin's [SwiftPM import documentation](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-import.html) marks the feature Alpha. It says transitive KMP build dependencies are handled, while exporting a KMP module that consumes SwiftPM dependencies as a Swift package is not yet generally supported and may not work.
- Kotlin's newer [Swift package export documentation](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-export.html) adds generated packaging support for XCFrameworks with SwiftPM dependencies starting in `2.4.20-Beta2`; this library and Ding are currently on earlier toolchains.
- Making it mandatory would force Firebase Core/Messaging into every Apple Ding consumer, create Firebase version-resolution constraints with the host app, and prevent APNs-only usage.
- On Android it would overlap with Ding's existing direct Firebase Messaging integration and could introduce duplicate bridge/service choices rather than reducing integration work.

The practical adoption path is therefore:

1. Keep `ding-core` and `captureAppleUserInfo` Firebase-independent and raw-payload-first.
2. Document how apps already using `zone.ien.firebase:firebase-messaging` can forward `tokenUpdates` and, if desired, its common `RemoteMessage` metadata.
3. Consider a tiny optional adapter only after a compatibility spike verifies Kotlin/Gradle metadata resolution, Android manifest behavior, iOS device and simulator linking, and Ding's exported SPM XCFramework with the transitive Firebase products.
4. Do not use the wrapper's normalized `RemoteMessage` as Ding's canonical raw record.

## Recommended API boundary

Do not attempt to make `RemoteMessage` or `NSDictionary` a `commonMain` type. Define a platform-neutral stored snapshot in common code, and normalize platform objects at the edge.

Illustrative shape:

```kotlin
enum class PushPlatform { Android, Ios }

enum class PushTransport { Fcm, FcmApns, Apns, Local, Unknown }

enum class CapturePoint {
    Foreground,
    BackgroundCallback,
    NotificationResponse,
    ServiceExtension,
}

data class PushSnapshot(
    val platform: PushPlatform,
    val transport: PushTransport,
    val capturePoint: CapturePoint,
    val receivedAtEpochMillis: Long,
    val title: String?,
    val body: String?,
    val data: Map<String, String>,
    val registrationToken: RegistrationToken?,
    val rawDeliveredPayload: JsonValue,
)
```

Recommended platform entry points:

```kotlin
// androidMain; preserve source compatibility
fun Ding.capture(context: Context, message: RemoteMessage)

// iosMain, exported to Swift/Objective-C
fun Ding.captureAppleUserInfo(
    userInfo: Map<Any?, *>,
    transport: PushTransport,
    capturePoint: CapturePoint,
)
```

Kotlin/Native maps Kotlin `Map` and Objective-C `NSDictionary`, so a Swift dictionary can cross this boundary. Nested values still need a recursive normalizer for `NSDictionary`, `NSArray`, `NSString`, `NSNumber`, `NSNull`, and unknown values. See Kotlin's [Swift/Objective-C collection mappings](https://kotlinlang.org/docs/native-objc-interop.html#collections).

For a polished Swift API, a small native Swift facade can accept `[AnyHashable: Any]` exactly as provided by Apple's delegate callbacks and call the generated Kotlin framework API. This also isolates generated Objective-C naming from Ding's public documentation.

The common API should not own notification delegates automatically. A host app may already use `UNUserNotificationCenterDelegate`, Firebase swizzling, analytics hooks, or another messaging SDK. Explicit forwarding is predictable and composable.

## Recommended module and migration plan

The existing project is an Android library, not a multiplatform library:

- `ding` applies `com.android.library` and exposes `Context`, `Intent`, `RemoteMessage`, AndroidX Startup, Android DataStore, Activity Compose, and Android Material 3.
- `RemoteMessageSnapshot` builds its raw form with Android/JVM `org.json.JSONObject`.
- `ding-noop` mirrors the Android-specific public signatures.

A direct in-place conversion of the published `ding` artifact would combine a build-system migration, persistence migration, public API evolution, iOS integration, and UI port in one release. It also risks changing the publication layout for existing Android consumers.

Use an additive migration instead:

### Phase 1: common capture core

Add a KMP module such as `ding-core` containing:

- canonical snapshot and registration-token models;
- recursive JSON-safe normalization;
- title/body/data fallback logic;
- query, filtering, copy/share serialization, and schema tests;
- a storage interface with platform-provided paths.

Replace `org.json` in shared code with a multiplatform JSON tree or serializer. AndroidX DataStore Preferences is officially supported in KMP, with only creation/path selection implemented per platform; see [Set up DataStore for KMP](https://developer.android.com/kotlin/multiplatform/datastore). If snapshots remain larger JSON documents rather than settings, a small file-backed store may still be a better fit; that is an implementation decision, not a KMP limitation.

### Phase 2: preserve Android compatibility

Keep `io.github.easyhooon:ding` and its current public methods. Internally, its Android adapter converts `RemoteMessage` to the common snapshot and delegates storage/query logic to `ding-core`.

Keep `ding-noop` available for Android release builds. Do not require existing Android adopters to move coordinates as part of initial iOS support.

### Phase 3: Apple adapter and framework

Publish or embed an Apple framework that provides:

- `captureAppleUserInfo` for foreground, notification-response, and background callbacks;
- `updateFcmToken` and optionally `updateApnsToken`, with the token kind stored explicitly;
- a host-supplied storage location;
- an explicit inspector entry point suitable for a debug menu.

The first Apple release can use app-private storage and support app callbacks. Notification Service Extension capture should be a separate opt-in integration because it requires another Xcode target, `mutable-content` on sender payloads, App Group entitlements, and shared-container configuration.

### Phase 4: iOS inspector UI

Choose one of these separately from payload capture:

- Share the inspector UI with Compose Multiplatform and expose an iOS view controller.
- Keep the Android Compose UI and build a small native SwiftUI inspector over the common query/export API.

Payload support does not require choosing the shared-UI option immediately. Android-only concepts such as dynamic app shortcuts, a persistent notification entry point, `Activity`, notification channels, and `POST_NOTIFICATIONS` remain in `androidMain`. iOS needs a host-owned debug entry point; it should not pretend to provide Android's launcher shortcut or persistent notification behavior.

KMP supports common interfaces with platform implementations and `expect`/`actual` declarations for platform APIs; see [Use platform-specific APIs](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html) and [Share code on platforms](https://kotlinlang.org/docs/multiplatform/multiplatform-share-on-platforms.html).

## Suggested host integration on iOS

The exact generated framework names will depend on the final export surface, but expected host calls are:

```swift
func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification
) async -> UNNotificationPresentationOptions {
    Ding.captureAppleUserInfo(
        userInfo: notification.request.content.userInfo,
        transport: .fcmApns,
        capturePoint: .foreground
    )
    return [.list, .banner, .sound]
}

func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse
) async {
    Ding.captureAppleUserInfo(
        userInfo: response.notification.request.content.userInfo,
        transport: .fcmApns,
        capturePoint: .notificationResponse
    )
}

func application(
    _ application: UIApplication,
    didReceiveRemoteNotification userInfo: [AnyHashable: Any]
) async -> UIBackgroundFetchResult {
    Ding.captureAppleUserInfo(
        userInfo: userInfo,
        transport: .fcmApns,
        capturePoint: .backgroundCallback
    )
    return .newData
}

func messaging(
    _ messaging: Messaging,
    didReceiveRegistrationToken fcmToken: String?
) {
    if let fcmToken {
        Ding.updateFcmToken(fcmToken)
    }
}
```

These calls must be composed into the host's existing delegates; Ding should not replace the delegate or alter the completion/presentation behavior.

## Issue-ready scope

### Suggested title

`Support KMP core and iOS APNs/FCM payload capture`

### Proposed behavior

- Add a platform-neutral snapshot schema with `platform`, `transport`, `capturePoint`, normalized fields, token kind, and raw delivered payload.
- Preserve the current Android `Ding.capture(Context, RemoteMessage)` API and Maven coordinates.
- Provide an Apple entry point that accepts APNs `userInfo` from foreground, response, and background callbacks.
- Keep Firebase optional on Apple platforms; allow host apps to forward FCM and/or APNs tokens.
- Expose an explicit iOS inspector entry point suitable for a debug menu.
- Document that platform-specific send blocks are not cross-visible and that capture coverage depends on OS callbacks.
- Track Notification Service Extension/App Group support as an opt-in follow-up unless it is explicitly accepted into the first milestone.

### Acceptance criteria

- An Android KMP target can use the current Ding integration without source changes.
- An iOS KMP target can forward an FCM-over-APNs or direct APNs `userInfo` dictionary and see `aps`, custom keys, normalized title/body, receipt time, capture point, and token context in Ding.
- Nested Apple Foundation values are converted deterministically to the raw JSON view.
- iOS records are clearly identified as `ios` and do not claim to contain the sender's Android block or complete FCM request.
- FCM and APNs token types are not conflated; historical snapshots retain the token recorded at capture time.
- Foreground, notification-response, and silent/background callback integrations are documented and tested with fixture payloads.
- Release/no-op integration remains available so Ding can be excluded from production behavior.
- Existing Android unit/build checks remain green, and shared normalization is covered by common tests.

### Non-goals for the first milestone

- Capturing the original FCM HTTP v1 request envelope on-device.
- Showing the Android block on iOS or the APNs block on Android.
- Guaranteed logging of every background or terminated-state push.
- Automatically replacing host notification delegates.
- Requiring Firebase Messaging in APNs-only Apple applications.
- Notification Service Extension capture unless App Group and extension packaging are intentionally included.

## Open design decisions

1. Whether the first Apple release includes UI or only capture, persistence, query, and export APIs.
2. Whether the Apple artifact is distributed through CocoaPods, an XCFramework, or both.
3. Whether `ding-core` is public or an internal implementation detail used by platform artifacts.
4. Whether the raw JSON schema is versioned before the first cross-platform release.
5. Whether Notification Service Extension support belongs in the initial issue or a linked follow-up.
6. Whether direct APNs capture asks the host to declare the transport explicitly or uses separate `captureFcmApple` and `captureApns` entry points.

## Primary sources

- [FCM REST `Message`, `AndroidConfig`, and `ApnsConfig`](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
- [Customize an FCM message across platforms](https://firebase.google.com/docs/cloud-messaging/customize-messages/cross-platform)
- [FCM message types](https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type)
- [Receive messages in Android apps](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
- [`FirebaseMessagingService` Android reference](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService)
- [Receive messages in Apple platform apps](https://firebase.google.com/docs/cloud-messaging/ios/receive-messages)
- [Get started with FCM on Apple platforms](https://firebase.google.com/docs/cloud-messaging/ios/get-started)
- [Firebase `Messaging` Apple API reference](https://firebase.google.com/docs/reference/swift/firebasemessaging/api/reference/Classes/Messaging)
- [Add Firebase to an Apple project](https://firebase.google.com/docs/ios/setup)
- [Apple notification handling](https://developer.apple.com/documentation/usernotifications/handling-notifications-and-notification-related-actions)
- [Apple background push behavior](https://developer.apple.com/documentation/usernotifications/pushing-background-updates-to-your-app)
- [Apple Notification Service Extension behavior](https://developer.apple.com/documentation/usernotifications/modifying-content-in-newly-delivered-notifications)
- [Apple App Groups entitlement](https://developer.apple.com/documentation/BundleResources/Entitlements/com.apple.security.application-groups)
- [Kotlin Multiplatform platform API guidance](https://kotlinlang.org/docs/multiplatform/multiplatform-connect-to-apis.html)
- [Kotlin/Native Swift and Objective-C interop](https://kotlinlang.org/docs/native-objc-interop.html)
- [Kotlin/Native target and host support](https://kotlinlang.org/docs/native-target-support.html)
- [Kotlin Multiplatform CocoaPods integration](https://kotlinlang.org/docs/multiplatform/multiplatform-cocoapods-overview.html)
- [AndroidX DataStore for KMP](https://developer.android.com/kotlin/multiplatform/datastore)
