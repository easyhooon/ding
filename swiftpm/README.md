# SwiftPM packaging

This directory contains local and remote manifests plus Swift import fixtures used to validate Ding's binary package shape and exported API. Generated XCFrameworks are never committed.

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

`Package.remote.swift.template` generates the repository-root manifest for a release:

```bash
./gradlew :ding-core:prepareDingRemoteSwiftPackage \
  -PdingSwiftPMVersion=0.5.0
```

The task writes `Package.swift` with the exact archive checksum. Upload the already-built ZIP with the matching GitHub Release; do not rebuild it after generating the manifest because separate Kotlin/Native builds are not guaranteed to produce identical bytes.

`verifyDingLocalSwiftPackage` also type-checks `Smoke.swift` against the simulator slice to catch module-name or exported-API regressions.

After a release is public, `verify-remote-release.sh <version>` resolves the tagged repository as an external package, downloads and checksum-validates the binary target, and type-checks the exported API. The `SwiftPM Release` workflow runs this check for every published GitHub Release.
