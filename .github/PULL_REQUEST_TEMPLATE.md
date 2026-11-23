## Summary

<!-- What changes, and why. One or two sentences. Link the issue if there is one. -->

Related issue: <!-- #123, or "none" -->

## Type of change

- [ ] Bug fix — `fix(scope): ...`
- [ ] Feature — `feat(scope): ...`
- [ ] Documentation only — `docs(...): ...`
- [ ] Build / CI / dependencies — `build(...)`, `chore(...)`
- [ ] Refactor or performance change with no intended behaviour change
- [ ] Other (describe):

## Evidence

Required for behaviour, build, version or claim changes. Paste the commands you ran and their real
output; do not estimate or reuse numbers from another run.

```powershell
.\gradlew.bat --console=plain :app:testDebugUnitTest
.\gradlew.bat --console=plain :app:lintDebug
```

- [ ] `:app:testDebugUnitTest` passes — tests / suites / failures: <!-- e.g. 205 / 18 / 0 at v1.3.0 -->
- [ ] `:app:lintDebug` exits 0 — errors / warnings / hints: <!-- e.g. 0 / 80 / 2 at v1.3.0 -->
- [ ] `:app:assembleDebug` succeeds and the debug APK installs and opens, or this PR does not affect the app
- [ ] `:app:bundleRelease` succeeds on a machine without `keystore.properties` (unsigned build), or this PR does not affect the build
- [ ] New or changed behaviour is covered by JVM tests in `app/src/test/` (or I explain below why not)
- [ ] Smoke-tested on an emulator (API level: ___) — the ATD image has no step sensor, so counting needs a physical phone
- [ ] **Not** device-verified: I am not claiming a physical-device run unless I state it here with evidence
- [ ] Room schema change: migration added and schema JSON committed under `app/schemas/` (or not applicable)
- [ ] Docs updated in the same PR if user-visible behaviour or measured evidence changed, and `README.md`/`README.ar.md` (and EN/AR release notes) stay in sync

## Claims checklist (docs, README, CHANGELOG, release notes, store copy)

- [ ] Every number, size, hash or date in this PR comes from a command I actually ran; none are estimated or copied from an older run.
- [ ] I do not claim Google Play availability, a physical-device verification of v1.3, or a completed native Arabic review.
- [ ] Anything not yet verified is marked "Pending" / "Not released" instead of being implied as done.
- [ ] Emulator-only evidence is labelled emulator-only.
- [ ] SHA-256 values are full 64-character hashes, not prefixes.
- [ ] v1.3 artifacts are described as built-and-tested and distributed through GitHub Releases only (never Google Play).

## Privacy guard (must all hold)

- [ ] The merged manifest still has no `INTERNET` and no `ACCESS_NETWORK_STATE` (`tools:node="remove"` kept).
- [ ] No analytics, ads, tracking, crash-SDK, account or cloud-sync code or dependency added.
- [ ] No background location; location is still used only during a user-started walk.
- [ ] Health Connect is still read-only and opt-in (no write permissions).
- [ ] `allowBackup="false"` and the backup/data-extraction rules are unchanged (or the change is explained below and reviewed).
- [ ] No new exported component without a permission guard; `DebugStepReceiver` stays in `app/src/debug/`.
- [ ] No keystore material, `keystore.properties`, passwords, tokens or personal data in the diff.
- [ ] `versionName`/`versionCode` unchanged (owner-only) and `applicationId` still `com.sabeel.steps`.

## Reviewer notes

<!-- Risky bits: R8 keep rules, Room migrations, service lifecycle, EN/AR parity, RTL, accessibility. -->

- [ ] I confirm I read the full task output, not just "BUILD SUCCESSFUL".
