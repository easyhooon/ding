---
name: ding-apple-swiftpm
description: Develop and validate Ding's Apple Kotlin/Native targets, APNs userInfo capture, static XCFramework, and SwiftPM integration. Use when changing ding-core iOS APIs or persistence, exported Swift APIs, XCFramework slices, Package.swift templates, or diagnosing Apple and SwiftPM build, import, and resolution failures.
---

# Ding Apple and SwiftPM

Apply Ding-specific Apple constraints while developing or diagnosing the iOS integration. Keep publication in the existing `/release` workflow.

## Choose the integration path

- Treat a KMP host using `ding-core` from shared Kotlin as a Maven dependency consumer. It does not need the Swift package.
- Treat a native Apple host calling Ding from Swift as a SwiftPM consumer. It needs the tagged XCFramework release and root `Package.swift`.
- Preserve the Android artifacts and public API when changing shared code.

## Preserve the capture contract

- Keep the host responsible for forwarding the original APNs `userInfo`; do not install or replace notification delegates.
- Normalize Foundation values through the existing `ding-core` pipeline instead of introducing Firebase types into public APIs.
- Keep Firebase integration optional. Record FCM and APNs tokens separately and retain the token present at capture time.
- Keep process-local and Room-backed stores interchangeable through `DingCaptureStore`.
- Require a stable app-private `.db` path for `PersistentDingCaptureStore`. Do not imply Notification Service Extension or App Group sharing unless that support is implemented.

## Maintain Apple packaging

- Keep `iosArm64` and `iosSimulatorArm64` framework slices in the static `Ding.xcframework`.
- Keep the Swift product and framework module named `Ding`.
- Update `swiftpm/Smoke.swift` whenever the intended exported Swift API changes.
- Validate both the local binary package and the remote manifest template. Do not accept a successful Kotlin compile as proof that Swift can import the API.
- Do not add signing, notarization, or CocoaPods setup for the current static XCFramework GitHub asset. Reassess only when the distribution model requires it.

## Verify changes

Run the Apple CI-equivalent checks on macOS:

```bash
./gradlew \
  :ding-core:ktlintIosMainSourceSetCheck \
  :ding-core:ktlintIosTestSourceSetCheck \
  :ding-core:compileKotlinIosArm64 \
  :ding-core:iosSimulatorArm64Test \
  :ding-core:packageDingSwiftPM \
  :ding-core:verifyDingRemoteSwiftPackageTemplate \
  :ding-core:verifyDingLocalSwiftPackage
```

For capture or persistence changes, also run the affected common and iOS tests. Inspect the generated archive only when packaging shape is relevant; never commit generated XCFrameworks.

## Hand off publication

Use `/release` for Maven Central, remote manifest generation, GitHub asset upload, tagging, and post-release verification.

Never rebuild or replace `Ding.xcframework.zip` after generating the release `Package.swift`: the checksum must describe the exact uploaded bytes, and separate Kotlin/Native builds are not guaranteed to be byte-identical.
