# Sabeel — سبيل

An original, offline, ad-free step counter and walking tracker for Android,
built clean-room from public product research (no decompilation, no copied
assets or copy). Arabic and English are supported; native Arabic review is
still pending (see *Known limitations*).

- **Package / applicationId:** `com.sabeel.steps` (permanent once published)
- **Version:** 1.0.0 (versionCode 1)
- **Status:** v1.0 production-readiness fix pass in progress. **Not published.**
  Emulator validation exists; physical-device validation is still pending — see
  *Verification status* below for exactly what is evidenced and what is not.
- **Platform:** Android 8.0+ (minSdk 26), targetSdk/compileSdk 36.
- **Stack:** Kotlin 2.0.21 · Jetpack Compose (Material 3) · Room (KSP) ·
  DataStore · WorkManager · Navigation-Compose · AGP 8.10.1 · Gradle wrapper
  8.11.1 · JDK 21.
- **License:** Apache-2.0 (see [LICENSE](LICENSE), [NOTICE](NOTICE) and
  [third-party notices](app/src/main/assets/THIRD-PARTY-NOTICES.txt)).

## What it does (v1)

- **Steps:** reads the hardware step counter (`TYPE_STEP_COUNTER`) from a
  foreground service; the step pipeline seeds its baseline from the first
  sample, rolls over at local midnight and keeps per-day totals monotonic.
- **Home:** progress ring against a personal goal, distance, active minutes,
  streak, last-7-day bars, counting-status card and one-tap walk.
- **History:** 7/30-day bars with goal line, per-day list and saved walks.
- **Walk:** GPS walk in a location foreground service with a Canvas route (no
  map tiles, no API key, no network); sensor-only fallback when location is not
  granted; checkpoint/resume after process death.
- **Insights:** window averages, best day, goal-hit rate, total distance.
- **Reminders & goal notification:** WorkManager reminder plus a goal-reached
  notification (POST_NOTIFICATIONS aware).
- **Settings:** goal, units, reminder, body data, language (EN/AR), CSV export
  and import (SAF), delete-all.
- **Privacy & About screens:** what is stored on the device and what the app
  can/cannot do.

Deliberately **not** in v1: accounts, cloud sync, ads, subscriptions, social,
Health Connect, widgets.

## Architecture

```
app/src/main/java/com/sabeel/steps/
  domain/     pure Kotlin: models, step math, step pipeline, window math
  data/       Room DB + DAOs, repositories, DataStore settings, CSV codec/IO
  service/    StepCounterService (health FGS), WalkTrackingService (location FGS),
              WalkTracker (checkpoint state)
  platform/   permissions, notifications, reminders/workers, boot receiver,
              service status store
  ui/         Compose theme/components/screens, view models, i18n (EN/AR)
app/src/test/ JVM unit tests (pipeline, math, CSV, cadence, merge rules)
```

Data flow: sensor events → `StepPipeline` → `StepsRepository` → Room; user
settings → DataStore; walks → `WalkTracker` + Room. Nothing leaves the device:
the merged manifest has no `INTERNET` permission and the app ships no
networking, ads or analytics SDKs.

## Build, test, lint, package

Prerequisites: JDK 21, Android SDK platform 36 + build-tools 36, Gradle wrapper
(included; do not install Gradle separately). Windows PowerShell example:

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
Release AAB: `app/build/outputs/bundle/release/app-release.aab`.
Unit-test report: `app/build/reports/tests/testDebugUnitTest/index.html`.

### Release signing

The release build has `isMinifyEnabled = true` and `isShrinkResources = true`.
Signing is loaded from `keystore.properties` **only when that file exists**;
CI and fresh clones therefore still build unsigned release artifacts.

```properties
# keystore.properties (gitignored — never commit)
storeFile=keystore/sabeel-upload.jks
storePassword=<secret>
keyAlias=sabeel-upload
keyPassword=<secret>
```

The upload keystore and Play App Signing procedure are documented in
[docs/release/KEYSTORE.md](docs/release/KEYSTORE.md). Passwords are never
committed or written into documentation.

### Privacy-policy URL

The in-app link is driven by a BuildConfig field supplied at build time:

```powershell
.\gradlew.bat --console=plain :app:bundleRelease `
  -Psabeel.privacyPolicyUrl=https://your-hosted-policy.example/privacy
```

Without the property, `BuildConfig.PRIVACY_POLICY_URL` is an empty string and
the in-app link must be hidden. Filling and hosting the policy is an owner
action: [docs/privacy-site/FILL-AND-HOST.md](docs/privacy-site/FILL-AND-HOST.md).

## Verification status (2026-11-17)

> Everything below is either measured in this repository or explicitly marked
> pending. Emulator evidence is included; no physical-device validation or
> publication is claimed.

| Item | State |
|---|---|
| Debug build | `assembleDebug` BUILD SUCCESSFUL 2026-11-17 22:31 — `dist/Sabeel-1.0-debug.apk`, 12,001,632 B, SHA-256 `dcd1f335…be615d` |
| Unit tests | **79 tests, 0 failures** (`testDebugUnitTest` exit 0, 2026-11-17 22:26): CsvCodec 12, StepMerge 7, StepMath 24, StepPipeline 13, StepCadence 5, GoalAdvisor 9, LocalizationParity 4, OemBatteryGuide 5 |
| Lint | `lintDebug` **exit 0** — 0 errors, 39 warnings, 1 hint (non-blocking; report in `app/build/reports/lint-results-debug.txt`) |
| Release APK / AAB | `assembleRelease` + `bundleRelease` BUILD SUCCESSFUL 2026-11-17 22:35 — signed with the upload key (v2, upload cert `7b12ad6b…6152d`), `apksigner verify` and `zipalign -c -P 16` OK; APK 1,577,122 B, AAB 3,374,452 B; hashes in `dist/SHA256SUMS.txt` |
| Release signing | Upload keystore generated; fingerprint in docs/release/KEYSTORE.md; Play App Signing registration pending (owner) |
| Device / emulator run | **Emulator runs completed** on the API 36 ATD image (1080x2400). The signed release APK pass covered onboarding EN, skip-permissions and grant paths, honest no-sensor status, Arabic RTL (in-app switch and `ar-IQ` device locale) for Home/History/Insights, walk foreground service with location denied, delete-all and relaunch (evidence: 16 UI-hierarchy dumps plus [SMOKE-EVIDENCE.md](docs/review/2026-11-17/smoke/SMOKE-EVIDENCE.md); the ATD image returns blank `screencap` frames, so no PNGs are kept). A second pass on the debug build exercised the diagnostics screen, the goal-advice suggestion and the rest-day streak pause in English and Arabic, and caught a rest-day streak bug that is now fixed and covered by tests (evidence: 9 UI-hierarchy dumps, [EVIDENCE.md](docs/review/2026-11-17/adoption-smoke/EVIDENCE.md)); the crash buffer was empty. Physical-device validation is still pending |
| Play publication | **Not published.** Declarations and assets are owner actions |

Do not copy a count or hash into a release note unless it was produced by the
command that built that artifact.

## Known limitations

- Validation is emulator-only so far (API 36 ATD image, no hardware step
  sensor); a physical-device matrix over API 26–36 with a real step counter is
  still required before publishing.
- Arabic copy has not had a native-speaker review; Arabic launch is blocked
  until that review happens (owner action).
- Store assets (screenshots, feature graphic, FGS demo video) are not ready.
- CSV export/import covers daily totals; individual walks are stored in the app
  but are not part of the CSV format.
- There is no analytics or crash reporting by design; release verification must
  come from Play vitals and manual runs.
- R8/minification is new in this pass: the signed release build was
  smoke-tested on the emulator (onboarding, Home, Settings, Arabic RTL, walk
  foreground service, delete-all); repeat that list on a physical device before
  uploading.

## Owner blockers (not fixable in code)

See [docs/legal/BLOCKERS.md](docs/legal/BLOCKERS.md): privacy-policy placeholders
and HTTPS hosting, trademark/name clearance, Play Console account/declarations,
testing-track obligations, store assets, and the distribution decision.

## Repository layout

```
app/                Android application module
docs/product/       brief and locked engineering spec (incl. decisions D1-D14)
docs/release/       keystore procedure, Play submission checklist
docs/legal/         privacy policy source, compliance checklist, evidence, blockers
docs/privacy-site/  static, tracker-free privacy page for hosting
docs/review/        independent review reports and fix evidence
.github/workflows/  Android CI (build + tests + lint + unsigned bundle)
```

## Contributing

The project is currently a single-owner release effort. Before any external
contribution becomes possible, the owner must clear the name (see blockers) and
choose a contribution/licensing policy. Do not add network permissions,
analytics, ads or cloud sync: those would falsify the product's only provable
claim.

## License

Apache License 2.0 — see [LICENSE](LICENSE). Third-party attributions are in
[NOTICE](NOTICE) and [app/src/main/assets/THIRD-PARTY-NOTICES.txt](app/src/main/assets/THIRD-PARTY-NOTICES.txt).
