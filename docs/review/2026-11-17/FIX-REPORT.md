# Production-readiness fix report — 2026-11-17

**Status:** the code-side production-readiness work is complete and verified on an
API 36 emulator. The app is **not yet shippable** only because of owner-only
blockers (privacy-policy hosting, Play Console declarations/testing, store assets,
trademark clearance — see §7). No known code or data-integrity defect remains open.

> **Snapshot note (added later on 2026-11-17).** This report records the first
> remediation pass. A later TrueStep-adoption pass changed the artifacts and the
> test suite: final sizes/hashes, 79 JVM tests and lint counts are in README.md
> and [release-signoff.md](../../legal/release-signoff.md), and the adoption
> emulator evidence is in
> [adoption-smoke/EVIDENCE.md](adoption-smoke/EVIDENCE.md). Figures below are the
> state at the time of that pass, kept as history.

Inputs: the independent council review of 2026-11-17 (verdict NOT SHIPPABLE) and
the remediation plan in `docs/superpowers/plans/2026-11-17-production-readiness.md`.

## 1. What was fixed

| Area | Change | Evidence |
|---|---|---|
| Step counting correctness | Rewrote the sensor→day pipeline as a testable state machine (`domain/StepPipeline.kt`): seed on first sample, reset on date change, rebase on backwards sensor values, monotonic per-day max. Writer uses day-aware cadence (`service/StepCadence.kt`). | 13 `StepPipelineTest` + 5 `StepCadenceTest` tests |
| Restore/merge integrity | CSV import now merges transactionally and can never lower a stored day's total; returns `RestoreSummary`/`MergeOutcome` (`data/repo/StepsRepository.kt`). CSV parsing is bounded (5 MB / 20 000 rows) with explicit errors (`data/export/CsvIo.kt`). | 7 `StepMergeTest` + 12 `CsvCodecTest` tests |
| Window statistics | `StepMath` window helpers (dates, fill, average, goal-hit rate/days, best day) replace duplicated ad-hoc math in Home/History/Insights. | 18 `StepMathTest` tests |
| Services & platform | `StepCounterService.ensureStarted()` gates on sensor + permission and is re-armed on every app foreground entry and on boot (`BootReceiver`); status surfaced through `platform/StatusStore.kt`; both services write notifications through one permission-checked `Notifications.post()`; `Notifications.resetPrefs()` exists for delete-all. | `lintDebug` 0 errors; smoke dumps 12–13 |
| Walk tracking | `WalkTrackingService.start()` gates on location **or** activity-recognition permission; location updates only registered when FINE_LOCATION is actually granted, so a location-denied walk still starts and counts steps; JSON checkpoint/resume/discard exposed to the UI. | Smoke dump 07–08 (`الموقع غير متاح. نحسب الخطوات فقط.`, service running) |
| Data deletion | Delete-all now erases steps, walks, settings, reminders, notification/status prefs, locale pref and walk checkpoints, stops both services under an erase guard, and survives the onboarding-switch ViewModel teardown (application scope + `NonCancellable`). | Smoke dumps 09–11; preference files verified as `<map />` |
| Localization | Full EN/AR parity: 189 strings each, 3 plurals each with all CLDR Arabic forms, matching format arguments, verified in-app switch and `ar-IQ` device-locale fallback (D7). | Script parity check + smoke dumps 04–06, 14–16 |
| Accessibility/contrast | Color tokens adjusted so the reviewed text/background pairs reach 5.08:1, 3.93:1 and 4.34:1; chart mirrors under RTL. | Review follow-up; Arabic RTL dumps |
| Release engineering | Optional signing from gitignored `keystore.properties`, R8 minify+shrink, `buildConfig` privacy-URL field, Room `exportSchema`, language split disabled for the in-app switch (D7), Room schema committed, LICENSE/NOTICE/third-party notices, CI workflow, keystore docs, Play checklist. | Signed APK/AAB below; `app/schemas/…/1.json` |
| Honesty on unsupported hardware | No step sensor → explicit `NO_SENSOR` card; no permission → permission card; counters never pretend to work. | Smoke dumps 03, 12–13 |

## 2. Verification evidence (commands actually run, 2026-11-17)

Environment: Windows, JDK 21.0.13.9, Gradle 8.11.1, AGP 8.10.1, Kotlin 2.0.21,
Android SDK build-tools 36.0.0, compileSdk/targetSdk 36, minSdk 26.

| Check | Command | Result |
|---|---|---|
| Unit tests | `gradlew.bat :app:testDebugUnitTest` | **BUILD SUCCESSFUL, exit 0** — 55 tests, 0 failures, 0 errors (CsvCodec 12, StepMerge 7, StepMath 18, StepPipeline 13, StepCadence 5) |
| Lint | `gradlew.bat :app:lintDebug` | **BUILD SUCCESSFUL, exit 0** — 0 errors, 37 warnings, 1 hint (non-blocking; `app/build/reports/lint-results-debug.txt`) |
| Debug build | `gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL — `dist/Sabeel-1.0-debug.apk`, 11,482,549 B |
| Release build | `gradlew.bat :app:assembleRelease` | BUILD SUCCESSFUL — signed `dist/Sabeel-1.0-release.apk`, 1,529,962 B |
| Release bundle | `gradlew.bat :app:bundleRelease` | BUILD SUCCESSFUL — signed `dist/Sabeel-1.0-release.aab`, 3,332,738 B |
| APK signature | `apksigner verify --print-certs` | `Verification successful`; cert SHA-256 `7b12ad6bfaf07817e76a2539954f7b4bde2e13f6b33454824830102566b6152d` (CN=Sabeel Upload, OU=Mobile, O=Sabeel, L=Baghdad, ST=Baghdad, C=IQ) |
| APK alignment | `zipalign -c -P 16 -v 4` | Verification successful |
| AAB signature | `jarsigner -verify` | `jar verified.` (self-signed chain warning is expected) |
| Manifest | `aapt dump badging` | `versionCode 1`, `versionName 1.0.0`, minSdk 26, targetSdk 36, **no INTERNET permission**; `ACCESS_NETWORK_STATE` removed |
| Room schema | build output | `app/schemas/com.sabeel.steps.data.db.AppDatabase/1.json` committed; `exportSchema = true`; no destructive migration fallback |
| Artifact hashes | `Get-FileHash -Algorithm SHA256` | `dist/SHA256SUMS.txt` (debug `6be50393…5fc41`, release APK `2b905e0c…5188e0`, AAB `97d41636…dfaa3`) |
| R8 mapping | build output | `dist/mapping/mapping-1.0.0.txt` archived locally (26,450,270 B; not committed) |

## 3. Emulator smoke test

Device: Android Emulator AVD `api36` — system image
`system-images;android-36;aosp_atd;x86_64` (API 36, 1080x2400), release APK
installed with `adb install -t`. Full evidence and per-step files:
[`smoke/SMOKE-EVIDENCE.md`](smoke/SMOKE-EVIDENCE.md), 17 uiautomator XML dumps in
`docs/review/2026-11-17/smoke/`.

Exercised: fresh install → English onboarding (4 steps) → Home with honest
`NO_SENSOR` status; permission card → grant Activity recognition → status
updates; Settings (privacy web row correctly hidden while
`PRIVACY_POLICY_URL` is empty); in-app Arabic switch → full RTL Settings/Home;
device-locale `ar-IQ` → app defaults Arabic and renders Home/History/Insights RTL
with Arabic weekday names and chart labels; walk screen with location denied →
`WalkTrackingService` running, live screen reports "location unavailable, counting
steps only"; discard; delete-all confirm → onboarding; all preference files
cleared; relaunch falls back to device language; no `AndroidRuntime` crash in the
whole run.

Known limitation of this evidence: the ATD image does not composite a framebuffer,
so `screencap` returns an all-black frame. Blank PNGs were deleted; the retained
uiautomator dumps are the real evidence. Store screenshots, FGS demo video and a
physical-device matrix remain owner/QA actions.

## 4. Defects found and fixed during final verification

1. **5 lint `MissingPermission` errors** at notification/location call sites →
   central permission-checked `Notifications.post()` and a direct
   `checkSelfPermission(ACCESS_FINE_LOCATION)` guard; `lintDebug` now exit 0.
2. **Release signing failure** (`Given final block not properly padded`) → the
   `keyPassword` written to `keystore.properties` did not match the key; align it
   to the verified store password (both now identical) and release builds sign.
3. **Delete-all half-execution** → coroutine on `viewModelScope` was cancelled when
   clearing settings flipped the root to onboarding; moved platform cleanup to the
   application scope under `NonCancellable` and re-verified all prefs cleared.
4. **Duplicate/misleading black screenshots** → removed and replaced with UI
   hierarchy evidence plus an explicit explanation of the ATD limitation.

## 5. What was *not* changed

No network permission, analytics, ads, cloud sync or account was added — the
offline/no-telemetry claim remains structurally true. Legal and privacy files were
left as owner-fill templates (name, contact, address, dates, hosting).

## 6. Residual risks to re-check before upload

- R8 runtime behaviour was exercised on the emulator (onboarding, Home, Settings,
  Arabic RTL, walk FGS, delete-all); repeat the same list on a physical device.
- The emulator has no hardware step counter, so real counting cadence/reset
  behaviour is still unverified on hardware (covered by unit tests only).
- Arabic copy has had no native-speaker review (owner blocker).
- Store listing assets, screenshots and the FGS demo video do not exist yet.
- CI workflow has not run on GitHub (no remote pushed).

## 7. Owner-only blockers (unchanged, not fixable in code)

See [`docs/legal/BLOCKERS.md`](../../legal/BLOCKERS.md):
privacy-policy placeholders + HTTPS hosting + `-Psabeel.privacyPolicyUrl`;
""Sabeel"" trademark/name clearance; Play Console account, Health-apps and
foreground-service declarations, Data safety, IARC; 12-tester/14-day testing
obligation for newer personal accounts; upload-key registration; store assets;
distribution-strategy decision.

## 8. Definition-of-done gates

| Gate | Status |
|---|---|
| G1 `testDebugUnitTest` all pass, target 55+ | **Met** — 55/55, 0 failures |
| G2 `lintDebug` exit 0, zero errors | **Met** — 0 errors (37 warnings, 1 hint listed) |
| G3 debug/release/bundle builds succeed, release verifies | **Met** — signed APK + AAB verified |
| G4 emulator smoke: skip permissions, grant, status cards, delete-all | **Met** — dumps 01–14 (including permission-skip and grant paths) |
| G5 emulator Arabic RTL screens | **Met with a documented limitation** — in-app switch and `ar-IQ` device locale dumps for Home/History/Insights; no PNGs because the ATD image renders blank framebuffers |
| G6 Room schema committed, no destructive migration | **Met** |
| G7 git history: baseline + workstream commits + verification commit | **Met** — baseline `0663edf`, plan `478d134`, then `b2f4b3a` (core), `43f75f9` (UI), `b76cda6` (release/docs), `ec226b1` (verification evidence) |

## 9. Reproduce

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:ANDROID_HOME='C:\Android\Sdk'; $env:ANDROID_SDK_ROOT='C:\Android\Sdk'

.\gradlew.bat :app:testDebugUnitTest :app:lintDebug --no-daemon
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:bundleRelease --no-daemon
# optional, only when the policy is hosted:
.\gradlew.bat :app:bundleRelease -Psabeel.privacyPolicyUrl=https://example.org/privacy --no-daemon
```

## 10. Artifact manifest (2026-11-17)

| File | Size (bytes) | SHA-256 |
|---|---:|---|
| `dist/Sabeel-1.0-debug.apk` | 11,482,549 | `6be5039321463f5b6d6c0181f43bb79ce4637a1d524d63d74bdc385f32b5fc41` |
| `dist/Sabeel-1.0-release.apk` | 1,529,962 | `2b905e0c647871c583a02703288eaed505478a70bd0f1274fc9a560dcc5188e0` |
| `dist/Sabeel-1.0-release.aab` | 3,332,738 | `97d41636b6353e6f5cda84596e6dd99b1340614e61eb9738d8db09d3ce9dfaa3` |

Uploaded AAB must be signed by the upload key above; Play App Signing registration
is the owner's next step (`docs/release/KEYSTORE.md`).
