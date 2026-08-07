---
name: release
description: Publish Ding artifacts to Maven Central and SwiftPM, then create English GitHub release notes in the Dari format.
argument-hint: "[version]"
user-invocable: true
disable-model-invocation: true
---

# Release Ding

Release `ding`, `ding-noop`, and `ding-core` together. Publish the exact Ding XCFramework archive referenced by the tagged Swift package manifest. All release notes, tags, commits, and repository documents must be English.

## 1. Verify the version

Read `ding` in `gradle/libs.versions.toml`. If a version argument is supplied, it must match the catalog value. If it differs, stop and ask the user to update and commit the catalog version before restarting the release. All published modules must use that single catalog version.

## 2. Verify prerequisites

Run `git fetch origin main`, then confirm the working tree is clean, the current branch is `main`, local `main` matches the refreshed `origin/main` commit ID, and the required Maven Central and signing credentials are available. Check only whether credentials exist; never print their values.

Stop if any prerequisite fails.

## 3. Validate publication locally

Run:

```bash
./gradlew clean
./gradlew publishToMavenLocal
```

Confirm that `ding`, `ding-noop`, the KMP root artifact, and its platform variants generated signed artifacts and POM files for the intended version.

## 4. Confirm external publication

Show the version and artifact coordinates, then request explicit user confirmation before uploading:

- `io.github.easyhooon:ding:<version>`
- `io.github.easyhooon:ding-noop:<version>`
- `io.github.easyhooon:ding-core:<version>` and its Gradle-selected platform variants

After approval, run the clean publication command required by `AGENTS.md`:

```bash
./gradlew clean publishAndReleaseToMavenCentral
```

Stop on any build, signing, or upload failure.

## 5. Prepare the exact SwiftPM release

After Maven Central publication succeeds, build the XCFramework archive and generate the repository-root remote manifest in one task:

```bash
./gradlew :ding-core:prepareDingRemoteSwiftPackage -PdingSwiftPMVersion=<version>
```

Confirm that:

- `Package.swift` contains the intended version and a 64-character checksum.
- `ding-core/build/swiftpm/release/Ding.xcframework.zip` exists.
- `swift package compute-checksum` for that exact ZIP matches `Package.swift`.

Commit only the generated `Package.swift` with subject `chore: prepare SwiftPM <version>` and push `main`. Confirm local `main` matches `origin/main` again. Do not run `clean`, rebuild the XCFramework, or replace the archive after this point: Kotlin/Native framework archives are not guaranteed to be byte-for-byte reproducible across separate builds.

## 6. Draft GitHub release notes

Use `gh release list --limit 3` to find the previous tag, `git log --oneline <previous-tag>..HEAD` for the change set, and `gh release view <previous-tag>` to preserve the existing tone when a prior release exists.

Use the Dari release note structure:

```markdown
## What's Changed
* <PR or feature title> by @easyhooon in <PR URL>
  * <key change>
  * <developer impact>

**Full Changelog**: https://github.com/easyhooon/ding/compare/<previous-tag>...<version>
```

For the first release, replace the comparison link with the repository release URL and state that it is the initial release.

## 7. Create the GitHub release

Show the final notes and request confirmation. Create a draft release with the exact archive generated in step 5, then publish it only after the asset upload succeeds:

```bash
gh release create <version> \
  ding-core/build/swiftpm/release/Ding.xcframework.zip \
  --draft \
  --title "<version>" \
  --notes-file <notes-file>
gh release edit <version> --draft=false
./swiftpm/verify-remote-release.sh <version>
```

The published release event runs the remote SwiftPM verification workflow independently. Report the Maven coordinates, SwiftPM repository URL, archive checksum, version, tag, release URL, and validation results.

## Safety

- Never skip `clean` before Maven Central publication.
- Never rebuild or substitute the XCFramework archive after generating `Package.swift`.
- Never expose credentials or signing material.
- Never invent a version or continue after failed verification.
- Never use `--no-verify`.
