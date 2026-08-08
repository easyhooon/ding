# Project Guidelines

## Commit & PR Rules

- Do NOT include `Co-Authored-By` lines referencing Codex in commit messages.
- Do NOT include "Generated with Codex" or similar AI attribution in PR descriptions.
- **Keep commit messages compact.** Subject ≤ 72 chars. Body only when the "why" isn't obvious from the diff — 1–3 short lines, no multi-paragraph essays. Don't restate what the diff already shows.

## Branch Rules

- Do NOT use the `codex/` prefix for branch names.
- Use conventional, change-specific prefixes such as `feat/`, `fix/`, `refactor/`, `docs/`, or `chore/`.
- Delete completed branches after they have been merged into `main`.

## Code Style

- Do NOT use fully-qualified names (FQN) inline in Kotlin code. Always add a proper `import` statement at the top of the file and reference the type by its simple name. This applies to both production and test code.
- **Keep comments compact.** Explain non-obvious intent in one or two lines. No multi-paragraph KDoc unless the API is genuinely complex. Skip comments that just restate the code.

## Code Style

- Do NOT use fully-qualified names (FQN) inline in Kotlin code. Always add a proper `import` statement at the top of the file and reference the type by its simple name. This applies to both production and test code.

## Issues

- Write all issues in **English**.
- Use clear section headers: `## Summary`, `## Problem` / `## Motivation`, `## Proposed Approach` / `## Proposed Behavior`, etc.
- Include relevant code snippets, color values, or architecture details where helpful.

## Release

- Always run `./gradlew clean` before `publishAllPublicationsToMavenCentralRepository` to ensure freshly compiled artifacts are uploaded (not stale build cache).

## Skills

- Code review follow-up: use the `/resolve-coderabbit-review` skill to triage and apply CodeRabbit comments on the current PR.
- Apple and SwiftPM development: use the `/ding-apple-swiftpm` skill when changing or diagnosing Ding's Apple targets, APNs capture, XCFramework, or SwiftPM integration.
- Release: use the `/release` skill to publish to Maven Central and draft GitHub release notes.
