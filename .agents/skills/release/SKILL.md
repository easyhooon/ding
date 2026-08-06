---
name: release
description: Publish Ding artifacts to Maven Central and create English GitHub release notes in the Dari format.
argument-hint: "[version]"
user-invocable: true
disable-model-invocation: true
---

# Release Ding

Release `ding` and `ding-noop` together. All release notes, tags, commits, and repository documents must be English.

## 1. Verify the version

Read `ding` in `gradle/libs.versions.toml`. If a version argument is supplied, it must match the catalog value. If it differs, stop and ask the user to update and commit the catalog version before restarting the release. Both published modules must use that single catalog version.

## 2. Verify prerequisites

Run `git fetch origin main`, then confirm the working tree is clean, the current branch is `main`, local `main` matches the refreshed `origin/main` commit ID, and the required Maven Central and signing credentials are available. Check only whether credentials exist; never print their values.

Stop if any prerequisite fails.

## 3. Validate publication locally

Run:

```bash
./gradlew clean
./gradlew publishToMavenLocal
```

Confirm that both artifacts and their POM files were generated for the intended version.

## 4. Confirm external publication

Show the version and artifact coordinates, then request explicit user confirmation before uploading:

- `io.github.easyhooon:ding:<version>`
- `io.github.easyhooon:ding-noop:<version>`

After approval, run the clean publication command required by `AGENTS.md`:

```bash
./gradlew clean publishAndReleaseToMavenCentral
```

Stop on any build, signing, or upload failure.

## 5. Draft GitHub release notes

Use `gh release list --limit 3` to find the previous tag, `git log --oneline <previous-tag>..HEAD` for the change set, and `gh release view <previous-tag>` to preserve the existing tone when a prior release exists.

Use the Dari release note structure:

```markdown
## What's Changed
* <PR or feature title> by @easyhooon in <PR URL>
  * <key change>
  * <developer impact>

**Full Changelog**: https://github.com/easyhooon/notification-inspector/compare/<previous-tag>...<version>
```

For the first release, replace the comparison link with the repository release URL and state that it is the initial release.

## 6. Create the GitHub release

Show the final notes and request confirmation. Then create the tag and release:

```bash
gh release create <version> --title "<version>" --notes-file <notes-file>
```

Report the Maven coordinates, version, tag, release URL, and validation results.

## Safety

- Never skip `clean` before Maven Central publication.
- Never expose credentials or signing material.
- Never invent a version or continue after failed verification.
- Never use `--no-verify`.
