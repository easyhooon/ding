# SwiftPM fixture

This directory contains the local manifest and Swift import fixture used to validate Ding's binary package shape and exported API. Generated XCFrameworks are never committed.

Run from the repository root:

```bash
./gradlew :ding-core:packageDingSwiftPM
./gradlew :ding-core:verifyDingLocalSwiftPackage
```

Outputs:

- `ding-core/build/XCFrameworks/release/Ding.xcframework`
- `ding-core/build/swiftpm/release/Ding.xcframework.zip`
- `ding-core/build/swiftpm/release/Ding.xcframework.zip.sha256`
- `ding-core/build/swiftpm/local` for local manifest validation

Remote SwiftPM distribution will replace the fixture's local `path` with a versioned GitHub Release `url` and the generated checksum.

`verifyDingLocalSwiftPackage` also type-checks `Smoke.swift` against the simulator slice to catch module-name or exported-API regressions.
