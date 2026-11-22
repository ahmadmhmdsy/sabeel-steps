# Sabeel — سبيل

An original, offline, ad-free step counter and walking/session tracker for Android, built clean-room from public product research (no decompilation, no copied assets or copy). Arabic and English are supported; the v1.3 strings are translated, but native Arabic review is still pending (see *Known limitations*).

- **Package / applicationId:** `com.sabeel.steps` (permanent once published)
- **Version:** 1.3.0 (versionCode 3) — `app/build.gradle.kts`
- **Status:** v1.3 release-preparation. **Not published.** The v1.3 debug APK, signed release APK and signed release AAB were built and measured on 2026-11-23 (see *Verification status*); nothing has been uploaded to Play and no physical-device run of v1.3 is claimed.
- **Platform:** Android 8.0+ (minSdk 26), targetSdk/compileSdk 36.
- **Stack:** Kotlin · Jetpack Compose (Material 3) · Room v3 (KSP) · DataStore · WorkManager · Navigation-Compose · AGP 8.10.1 · Gradle wrapper 8.11.1 · JDK 21.
- **License:** Apache-2.0 (see [LICENSE](LICENSE), [NOTICE](NOTICE) and [third-party notices](app/src/main/assets/THIRD-PARTY-NOTICES.txt)).

## What it does (v1.3)

- **Counting control:** one persisted on/off switch for step counting. It is the same switch on Home (tap the ring), on the status pill, and in Settings; the choice survives reboot and restart. When it is off the counting service is not running, and the UI says so.
- **Boot restart and evidence:** after a restart, `BootReceiver` records a boot attempt (onboarded/enabled flags, activity permission, sensor, Health Connect, result, and whether the service was alive after a check) and tries to resume counting if it was on. If Android blocks the background start, the app records it and may post one recovery notification. Diagnostics lists the last ten attempts and can share a text report with no location/place data.
- **Steps:** a manifold of parallel sources in a health foreground service: `TYPE_STEP_COUNTER` and `TYPE_STEP_DETECTOR` run together, **Health Connect** (opt-in, read-only, absolute floor) contributes a watch-synced total, and a clearly labelled accelerometer estimate fills only the part no measured/synced source has seen. Nothing is switched or re-based: each source keeps its own ledger and the day is the maximum of the evidence. Reboots/sensor resets are detected from a falling raw value; impossible jumps are rejected; counter catch-up after process death is recovered from the cumulative raw. The "≈" marker is derived from the ledgers and persisted with the day.
- **Custom sessions:** start a walk or run from Home. Optional goal (time, steps, distance, or a saved place), optional foreground route/distance, pause/resume with no back-fill, finish/save or discard, one active session, and midnight-crossing sessions split into legs. Session steps are a projection of the same global day count, never a second counter — `session.steps ≤ daily.steps` by construction.
- **Places and presets:** save a place (label + lat/lon + radius, local only — no search, no geocoding, no map tiles) and save session setups for reuse.
- **Home / History / Insights:** progress ring against a personal goal, distance, active minutes, streak, 7-day bars; 7/30-day history with goal line, list and saved sessions; window averages, best day, goal-hit rate and total distance.
- **Reminders, goal notification, Diagnostics:** WorkManager reminder plus a goal-reached notification (POST_NOTIFICATIONS aware); Diagnostics covers counting state, source/health rows, boot attempts, share and a one-tap "Restart counting".
- **Settings:** counting switch, goal, units, reminder, body data, language (EN/AR), CSV export/import (daily totals, SAF), delete-all.
- **Privacy & About screens:** what is stored on the device and what the app can/cannot do.

Deliberately **not** in v1.3: accounts, cloud sync, ads, subscriptions, social, Health Connect **write/export**, background location, map tiles/search, widgets, simultaneous sessions. (Reading today's step total from Health Connect is an offline, opt-in fallback; the app never writes to Health Connect.)

## Architecture

```
app/src/main/java/com/sabeel/steps/
  domain/     pure Kotlin: models, step math, step pipeline, window math
  domain/session/  activity/goal types, place/preset/leg models, projection rules
  data/       Room DB + DAOs (v3), repositories, DataStore settings, CSV codec/IO,
              export/session folder (SessionExport CSV/GPX builders)
  service/    StepCounterService (health FGS), WalkTrackingService (location|health FGS),
              WalkTracker (live state + checkpoint), CountingController (persisted intent),
              SessionConfig (session request carried in the service Intent)
  platform/   permissions, notifications, reminders/workers, BootReceiver,
              BootHealthWorker, CountingStore, service status store
  ui/         Compose theme/components/screens, view models, i18n (EN/AR)
app/src/debug/  DebugStepReceiver — debug-only step injector for emulator QA
app/src/test/   JVM unit tests (pipeline, math, CSV, session projection/export, cadence)
```

Data flow: sensor events → `StepPipeline` (per-source ledgers, fused day) → `StepsRepository` (one atomic snapshot write) → Room; a session's live steps are re-derived from the day rows + its own baseline; user settings → DataStore. Nothing leaves the device: the merged manifest has no `INTERNET` permission and the app ships no networking, ads or analytics SDKs.

## Build, test, lint, package

Prerequisites: JDK 21, Android SDK platform 36 + build-tools 36, Gradle wrapper (included; do not install Gradle separately). Windows PowerShell example:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:ANDROID_HOME='C:\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME

.\gradlew.bat --console=plain :app:assembleDebug          # debug APK
.\gradlew.bat --console=plain :app:testDebugUnitTest      # unit tests
.\gradlew.bat --console=plain :app:lintDebug              # lint gate (must be exit 0)
.\gradlew.bat --console=plain :app:assembleRelease        # release APK
.\gradlew.bat --console=plain :app:bundleRelease          # AAB for Play
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Release APK: `app/build/outputs/apk/release/app-release.apk`.
Release AAB: `app/build/outputs/bundle/release/app-release.aab`.
Unit-test report: `app/build/reports/tests/testDebugUnitTest/index.html`.
Lint report: `app/build/reports/lint-results-debug.html`.

### Emulator QA and fake steps

The API 36 AOSP-ATD image has **no hardware step counter or detector**, so real counting cannot be validated there; use a physical phone for that (see `docs/release/TEST-XIAOMI.md`). On a **debug** build only, a broadcast receiver injects steps through the same snapshot path the service uses:

```powershell
adb shell am broadcast -a com.sabeel.steps.DEBUG_STEPS --ei steps 100 -n com.sabeel.steps/.debug.DebugStepReceiver
```

The receiver is absent from release builds. Emulator screenshots from the ATD image may be black; use `adb exec-out screencap -p` from the host or a Google-APIs image if approved. Location on the emulator: `adb emu geo fix <lon> <lat>`.

### Release signing

The release build has `isMinifyEnabled = true` and `isShrinkResources = true`. Signing is loaded from `keystore.properties` **only when that file exists**; CI and fresh clones therefore still build unsigned release artifacts.

```properties
# keystore.properties (gitignored — never commit)
storeFile=keystore/sabeel-upload.jks
storePassword=<secret>
keyAlias=sabeel-upload
keyPassword=<secret>
```

The upload keystore and Play App Signing procedure are documented in [docs/release/KEYSTORE.md](docs/release/KEYSTORE.md). Passwords are never committed or written into documentation.

### Privacy-policy URL

The in-app link is driven by a BuildConfig field supplied at build time:

```powershell
.\gradlew.bat --console=plain :app:bundleRelease `
  -Psabeel.privacyPolicyUrl=https://your-hosted-policy.example/privacy
```

Without the property, `BuildConfig.PRIVACY_POLICY_URL` is an empty string and the in-app link must be hidden. Filling and hosting the policy is an owner action: [docs/privacy-site/FILL-AND-HOST.md](docs/privacy-site/FILL-AND-HOST.md).

## Verification status

> Everything below is either measured in this repository or explicitly marked pending. The **v1.3 artifacts were built and measured on 2026-11-23** in one serialized Gradle run (unit tests, then `assembleDebug` + `assembleRelease` + `bundleRelease`); the measured sizes and hashes are in the table below. Emulator evidence is included; no physical-device validation of v1.3 and no publication is claimed.

### v1.3 artifact table — measured 2026-11-23 (task-20)

Every row was produced by the command named in it, in one serialized `--no-daemon` Gradle run that started 2026-11-22 23:52 and finished 2026-11-23 00:16 (Asia/Baghdad), on JDK 21 (Temurin 21.0.13), Android SDK platform 36, build-tools 36.0.0, Gradle wrapper 8.11.1 and AGP 8.10.1. No release artifact was rebuilt after that run.

| Artifact | Built (date + tool) | Size | SHA-256 | notes |
|---|---|---|---|---|
| Debug APK | 2026-11-22 23:56 — `assembleDebug` | 14,493,718 B (13.82 MiB) | `a61444db33db63971642fc53af4795bb48e19d16c3c7f8dce3614b43facd1fbf` | not a release artifact; `debuggable=true` |
| Release APK | 2026-11-23 00:15 — `assembleRelease` (R8 + resource shrink) | 2,138,010 B (2.04 MiB) | `487ee9c9f7e7d0e1bbe490c4b7692c72cb83c0f8d963cf1f2a4545677a4dfe5d` | signed with the upload key (v2 scheme; cert `7b12ad6b…6152d`); `apksigner verify` exit 0; `zipalign -c -P 16` OK; **versionCode 3 / versionName 1.3.0**; not debuggable |
| Release AAB | 2026-11-23 00:16 — `bundleRelease` | 4,608,495 B (4.40 MiB) | `ffde823d8855a7d2e8f3987156dd93124c89cf2e92894e310ba01f43a4a9fc4e` | Play upload artifact; `jarsigner -verify` exit 0 (`jar verified.`, warning lines only); signed with the upload key; built without `-Psabeel.privacyPolicyUrl`, so `PRIVACY_POLICY_URL` is empty — rebuild with the property before upload |
| Unit tests | 2026-11-23 — `testDebugUnitTest --no-daemon --console=plain` (after the final `AppViewModels.kt` fix) | 205 tests, 18 suites, 0 failures, 0 skipped | — | exit 0; 7m14s; JUnit XML under `app/build/test-results/testDebugUnitTest/` |
| Lint | 2026-11-23 — `lintDebug --no-daemon --console=plain` (Lead gate run on the final tree) | 0 errors, 80 warnings, 2 hints | — | exit 0; 6m04s; `app/build/reports/lint-results-debug.html` |

Deliverable copies (git-ignored binaries; hashes tracked): `dist/Sabeel-1.3.0-debug.apk`, `dist/Sabeel-1.3.0-release.apk`, `dist/Sabeel-1.3.0-release.aab`; `dist/SHA256SUMS.txt` lists all three plus the historical 1.1.0 files. The release APK/AAB are byte-identical to the Gradle outputs above.

### Last measured snapshot — v1.2-era code (2026-11-21), historical, not a v1.3 claim

| Item | State |
|---|---|
| Debug build | `assembleDebug` BUILD SUCCESSFUL 2026-11-21 — 13,581,629 B, SHA-256 `117b51fd…714dc` |
| Unit tests | **127 tests, 0 failures** (`testDebugUnitTest` exit 0, 2026-11-21; 10 suites) |
| Lint | `lintDebug` exit 0 — 0 errors, 46 warnings, 1 hint (non-blocking) |
| Release APK / AAB | `assembleRelease` + `bundleRelease` BUILD SUCCESSFUL 2026-11-21 — signed with the upload key (v2, cert `7b12ad6b…6152d`), `apksigner verify` OK, `zipalign -c -P 16` OK; **versionCode 2 / versionName 1.1.0**; APK 1,864,306 B, AAB 3,992,650 B; `dist/SHA256SUMS.txt`; not uploaded |
| Release signing | Upload keystore generated; fingerprint in [docs/release/KEYSTORE.md](docs/release/KEYSTORE.md); Play App Signing registration pending (owner) |
| Device / emulator | Emulator runs on the API 36 ATD image: onboarding EN/AR, permission grant/deny, labelled estimate tier (injected walking), walk foreground service with location denied, delete-all + relaunch, diagnostics, goal advice, Room v1→v2 migration. **No hardware step counter/detector and no Health Connect step data on the image.** Evidence folders under `docs/review/2026-11-*`; ATD `screencap` frames are blank, so no PNGs are kept. |
| Play publication | **Not published.** Declarations, privacy hosting and assets are owner actions |

**Version history note:** the step-manifold change is called "v1.2" in the docs, but the artifact built for it kept **versionName 1.1.0 / versionCode 2** (the bump was never applied). The tree now declares **versionCode 3 / versionName 1.3.0** (`app/build.gradle.kts`), so there is no 1.2.0 artifact. The v1.3 build exists under `app/build/outputs/` (built 2026-11-23) and is not copied into `dist/`; `dist/` still contains the older 1.1.0/vc2 build and `dist/SHA256SUMS.txt`.

Do not copy a count or hash into a release note unless it was produced by the command that built that artifact.

## Known limitations

- **v1.3 is built and tested but not device-verified.** The counting-control code, session domain/data/migrations, session setup/live screens, walk-service projection and navigation wiring are in the tree; the v1.3 build, the 205-test JVM suite and lint now pass (2026-11-23, table above). The **session flow has not been exercised on a device** and no physical-device run of v1.3 exists, so where the docs say "v1.3 behaviour", read it as "the behaviour this release is built to deliver", not as device evidence.
- **Sessions and export:** the session export helpers (CSV/GPX) exist but the export entry point was not wired when this README was updated; the privacy policy and release notes must drop the session-export text if the release build does not expose it. Likewise, saved places can be read/picked in session setup but a place-creation UI was not found in the tree — the place goal may be unusable until one lands.
- **Arabic:** the v1.3 strings are translated (EN/AR key parity holds), but no native Arabic reviewer has checked them; native review is still a launch precondition. RTL layout of the new session screens also needs a device check.
- **Counting cannot be guaranteed.** Android does not allow an app to restart itself after a force-stop, and OEM power managers (MIUI and others) can stop a foreground service. The app tries at boot, records the attempt, and tells the user when it was blocked; it never claims "always counting".
- Validation so far is unit tests plus emulator-only runs (API 36 ATD image, no hardware step counter or step detector); a physical-device matrix over API 26–36 with a real step counter and a detector-only device is still required before publishing. The **Step Zero falsification test** in [COUNCIL.md](docs/review/2026-11-21/step-resilience/COUNCIL.md) must pass on a real phone before the fused counter+detector claim is trusted; if the detector disagrees with the counter by more than the tolerance, it is demoted to liveness-only. Process death cannot recover detector-only steps taken while the process was dead (the hardware counter can); Health Connect can fill that gap only when granted. A force-stop or OEM kill cannot be restarted from the app; the app recovers on the next open, boot or package replace.
- The accelerated estimate is deliberately labelled and conservative. It cannot distinguish walking from some hand-carried or handshake motion; do not present it as exact step counts.
- The Health Connect tier is unit-tested, and its card and read-only Steps permission screen were captured on the emulator, but no Health Connect step data existed on the image, so the read path, the floor reconciliation and the background-read permission still need a phone whose watch or app writes steps to Health Connect (owner hardware item, [HARDWARE-QA-CHECKLIST.md §5b](docs/legal/HARDWARE-QA-CHECKLIST.md)). Health Connect has no step timestamps, so an HC-only day contributes no active minutes.
- The v1.1 CSV format adds a sixth `estimated_steps` column. Five-column files from v1.0 still import (the estimate is stored as 0); other apps that read the CSV may need the new header. CSV covers daily totals, not sessions.
- Store assets (screenshots, feature graphic, FGS demo video) are not ready.
- There is no analytics or crash reporting by design; release verification must come from Play vitals and manual runs.
- R8/minification is applied to release builds; repeat the emulator smoke list on a physical device before uploading.

## Owner blockers (not fixable in code)

See [docs/legal/BLOCKERS.md](docs/legal/BLOCKERS.md): privacy-policy placeholders and HTTPS hosting, trademark/name clearance, Play Console account/declarations, testing-track obligations, store assets and the FGS demo video, and the distribution decision. The v1.3-specific Play answers and listing copy are in [docs/release/PLAY-1.3.md](docs/release/PLAY-1.3.md); the physical test script for the owner's Redmi Note 7 is [docs/release/TEST-XIAOMI.md](docs/release/TEST-XIAOMI.md); user-facing notes are [docs/release/NOTES-1.3.md](docs/release/NOTES-1.3.md).

## Repository layout

```
app/                Android application module
docs/product/       brief and locked engineering spec (incl. decisions D1-D14)
docs/release/       keystore procedure, Play answers (1.3), release notes, claims audit
docs/legal/         privacy policy source, compliance checklist, evidence, blockers
docs/privacy-site/  static, tracker-free privacy page for hosting
docs/review/        independent review reports and fix evidence
dist/               last signed 1.1.0/vc2 artifacts + SHA256SUMS (NOT v1.3)
.github/workflows/  Android CI (build + tests + lint + unsigned bundle)
```

## Contributing

The project is currently a single-owner release effort. Before any external contribution becomes possible, the owner must clear the name (see blockers) and choose a contribution/licensing policy. Do not add network permissions, analytics, ads or cloud sync: those would falsify the product's only provable claim.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Third-party attributions are in [NOTICE](NOTICE) and [app/src/main/assets/THIRD-PARTY-NOTICES.txt](app/src/main/assets/THIRD-PARTY-NOTICES.txt).
