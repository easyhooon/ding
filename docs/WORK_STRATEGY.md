# Ding Work Strategy

## Goal

Build `easyhooon/ding` as a reusable Android debug library for inspecting notification-related payloads in real apps.

The library starts from the Hime-incubated implementation, then grows toward a Dari-like developer experience without coupling itself to Hime.

## Implementation Status

- Phase 1 complete: independent debug/no-op modules and sample capture paths.
- Phase 2 complete: Compose list, search, source filters, category affordances, and detail tabs.
- Phase 3 complete: default persistent notification with payload-safe summaries and automatic entry-point setup.
- Phase 4 complete: section copy, single-message sharing, filtered export, and privacy guidance.
- Ding `0.2.0` rebrands the package, API, modules, and Maven coordinates before external adoption.

## Scope

### Phase 1: Repository Extraction

- Create an independent Gradle Android library repository.
- Preserve the debug/no-op split:
  - `ding`: debug implementation.
  - `ding-noop`: release/staging no-op API surface.
- Move the current Hime-incubated API:
  - `Ding.capture(context, remoteMessage)`
  - `Ding.captureNotification(...)`
  - `Ding.open(context)`
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
- Add source/category affordances inspired by Dari:
  - FCM / remote message
  - local notification
- Add search over:
  - title/body
  - source
  - tag
  - data keys
  - raw JSON text
- Add detail tabs:
  - Overview
  - Raw JSON

### Phase 3: Ding Entry Points

- Register a dynamic shortcut on the host app icon at application startup.
- Keep the inspector Activity out of the standalone launcher list.
- Add a persistent debug notification enabled by default.
- Tapping the notification opens the inspector UI.
- Notification content should summarize recent captured events without leaking too much payload text.
- Persist an explicit runtime opt-out across process restarts.

### Phase 4: Export / Copy UX

- Improve single-message copy:
  - copy raw JSON
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
- Ding should first support explicit entry points:
  - host app shortcut
  - persistent notification
  - `Ding.open(context)`

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
ding/
├── docs/
│   └── WORK_STRATEGY.md
├── ding/
├── ding-noop/
├── sample/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Definition Of Done For First Extracted Version

- Independent repo opens in Android Studio.
- `:ding:ktlintCheck` passes.
- `:ding-noop:ktlintCheck` passes.
- `:ding:detekt` passes if detekt is configured.
- Sample app can trigger local notification capture.
- README shows debug/no-op installation and basic API usage.
- Hime can later replace project modules with Maven coordinates without API changes.
