---
name: resolve-coderabbit-review
description: Triage unresolved CodeRabbit review threads, apply valid fixes, and reply in English with traceable outcomes.
argument-hint: "[PR number] [--brief]"
user-invocable: true
disable-model-invocation: true
---

# Resolve CodeRabbit review

Process unresolved CodeRabbit threads on the current pull request. Keep every change traceable to its review thread and follow `AGENTS.md`.

## 1. Resolve the pull request

Use the supplied PR number, or detect the current branch PR:

```bash
gh pr view --json number,headRefName,baseRefName,url,isDraft
```

Stop when no PR exists. Do not process a draft PR.

## 2. Load learned patterns

Read `learned-patterns.md` when present. Repeated accepted or declined patterns inform decisions but never override the current code and specification.

## 3. Fetch unresolved CodeRabbit threads

Use `gh api graphql` to read `reviewThreads`, including thread ID, `isResolved`, author, body, path, line, original line, and diff hunk. Keep only threads where:

- `isResolved` is false;
- the first author is `coderabbitai[bot]` or `coderabbitai`;
- the thread is an inline review, not a general PR summary;
- no reply already contains the accepted or declined response markers below.

Accepted marker: `✅ Addressed in`

Declined marker: `Thanks for the suggestion. After review, we've decided not to apply this change.`

## 4. Classify and decide

Sort findings by security/crash, bug/logic, quality/readability, then style. Read the target file and surrounding design before deciding.

Prefer accepting findings involving crashes, races, leaks, security, missing error handling, clear correctness defects, or violations of `AGENTS.md`. Decline suggestions that introduce speculative abstraction, disproportionate scope, unintended behavior changes, or defenses for unobserved edge cases.

Never decline immediately. Present every proposed decline and its concrete reason to the user and wait for approval.

## 5. Apply accepted findings

For each accepted thread:

1. Make the smallest complete fix.
2. Run the relevant checks despite the upstream Dari skill omitting builds; this repository requires validation.
3. Commit one review thread per commit with `fix:` or `refactor:` and an English subject no longer than 72 characters.
4. Push without `--no-verify`.
5. Reply in English with `✅ Addressed in <full commit URL>`.
6. Resolve the GraphQL review thread.

Do not include AI attribution in commits or PR comments.

## 6. Process approved declines

After user approval, reply in English:

```text
Thanks for the suggestion. After review, we've decided not to apply this change.

**Reason**: <specific technical or product reason>
```

Resolve the thread after posting the response.

## 7. Update the PR and summary

Update the PR body only when a review fix changes behavior, architecture, security, or another major claim. Post or update one PR comment headed `## 🤖 CodeRabbit Review Resolution` with counts and tables for accepted and declined findings.

## 8. Learn repeated patterns

Update `learned-patterns.md` after processing. Increment matching patterns or add new rows. When a pattern reaches three consistent outcomes, propose adding it to this skill. Do not change the skill criteria without user approval.

## Safety

- Review responses and all repository documents must be English.
- Never print credentials or tokens.
- Never use `--no-verify`.
- Do not reply, resolve, decline, commit, or push without the authority required by the current request.
