# Notification Inspector Work Strategy

## Goal

Build `easyhooon/notification-inspector` as a reusable Android debug library for inspecting notification-related payloads in real apps.

The library starts from the Hime-incubated implementation, then grows toward a Dari-like developer experience without coupling itself to Hime.

## Scope

### Phase 1: Repository Extraction

- Create an independent Gradle Android library repository.
- Preserve the debug/no-op split:
  - `notification-inspector`: debug implementation.
  - `notification-inspector-noop`: release/staging no-op API surface.
- Move the current Hime-incubated API:
  - `NotificationInspector.capture(context, remoteMessage)`
  - `NotificationInspector.captureNotification(...)`
  - `NotificationInspector.open(context)`
- Keep DataStore Preferences as the local storage backend.
- Keep `org.json` for snapshot storage while the schema is still moving.
- Add a minimal sample app that can exercise:
  - local notification capture
  - FCM-style manual capture
  - open inspector UI

### Phase 2: Dari-Like UI/UX

- Replace the current simple Activity UI with Compose.
- Provide a compact message list optimized for repeated debugging.
- Add chips for:
  - all
  - FCM / remote message
  - local notification
  - future tags
- Add status/category affordances inspired by Dari:
  - success
  - in-progress
  - error
  - informational/local
- Add search over:
  - title/body
  - source
  - tag
  - data keys
  - raw JSON text
- Add detail tabs:
  - Overview
  - Data
  - Notification
  - Raw JSON

### Phase 3: Persistent Notification

- Add an optional persistent debug notification.
- Tapping the notification opens the inspector UI.
- Notification content should summarize recent captured events without leaking too much payload text.
- Keep this feature opt-in or debug-default configurable.

### Phase 4: Export / Copy UX

- Improve single-message copy:
  - copy raw JSON
  - copy data payload
  - copy notification payload
- Add share/export:
  - selected message as text
  - all filtered messages as JSON/text
- Keep privacy guidance visible in docs because notification payloads may contain sensitive data.

## Backlog

### Shake-To-Open

Shake-to-open is useful but must stay backlog for now.

Reason:

- Dari already uses shake-to-open in apps that include it.
- If both libraries are installed, duplicate shake handlers can feel broken or noisy.
- Notification Inspector should first support explicit entry points:
  - launcher Activity
  - persistent notification
  - `NotificationInspector.open(context)`

When implemented later, shake-to-open must be:

- disabled by default
- configurable
- documented with Dari coexistence guidance

## Design Principles

- Debug-only usefulness first.
- No-op module must preserve the same public API.
- Release/staging must have zero visible UI and no payload persistence.
- API should be small and stable before Maven publication.
- Avoid Hime-specific assumptions.
- Prefer dependency choices that fit a reusable Android library:
  - DataStore over SharedPreferences.
  - `org.json` until snapshot schema stabilizes.
  - consider kotlinx.serialization only after schema/model boundaries settle.

## Initial Repository Shape

```text
notification-inspector/
├── docs/
│   └── WORK_STRATEGY.md
├── notification-inspector/
├── notification-inspector-noop/
├── sample/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Definition Of Done For First Extracted Version

- Independent repo opens in Android Studio.
- `:notification-inspector:ktlintCheck` passes.
- `:notification-inspector-noop:ktlintCheck` passes.
- `:notification-inspector:detekt` passes if detekt is configured.
- Sample app can trigger local notification capture.
- README shows debug/no-op installation and basic API usage.
- Hime can later replace project modules with Maven coordinates without API changes.
