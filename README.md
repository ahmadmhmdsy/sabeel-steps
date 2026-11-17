# Sabeel — سبيل

An original, offline, ad-free step counter and walking tracker for Android, built clean-room from
public store-listing research (no APK, no decompilation, no copied assets or copy). Arabic and
English are first-class languages.

- **Package / applicationId:** com.sabeel.steps (permanent once published)
- **Status:** v1 feature-complete, debug APK green, unit tests green. Not yet published.
- **Platform:** Android 8.0+ (minSdk 26), targetSdk/compileSdk 36 (Play requirement for new apps).
- **Stack:** Kotlin 2.0.21 · Jetpack Compose (Material 3) · Room · DataStore · WorkManager ·
  Navigation-Compose · AGP 8.10.1 · Gradle 8.11.1 · JDK 21.

## What it does (v1)

- **Steps:** reads the hardware step counter (TYPE_STEP_COUNTER) in a foreground service; survives
  reboot, midnight rollover and process death; honest distance/calorie estimates from stride.
- **Home:** progress ring against a personal goal, distance, active calories, active minutes,
  streak, last-7-day bars, one-tap walk.
- **History:** 7/30-day bars with goal line and a per-day list.
- **Walk:** GPS walk in a location foreground service with a route drawn on our own Canvas —
  no map tiles, no Google Maps key, no network. Sensor-only fallback when location is denied.
  Checkpointed every 20 s; discard really discards.
- **Insights:** 7-day average, best day, goal-hit rate, total distance, plain-language summary.
- **Reminders & goal notification:** user-set daily reminder + goal-reached notification
  (WorkManager, POST_NOTIFICATIONS).
- **Settings:** goal, units, reminder, height/weight/gender, language (EN/AR switch in-app),
  CSV export **and import** (SAF), delete all data.
- **Privacy screen:** what is on the device and what the app can/cannot do — verifiable from the
  permission list.
- **Accessibility / RTL:** full RTL, start/end layout, content descriptions, 48dp targets,
  font-scale 2.0 layouts, light/dark theme.

Deliberately **not** in v1: accounts, cloud sync, ads, subscriptions, social, Health Connect.
A home-screen widget is the planned v1.1 fast follow.

## Privacy (what the build actually does)

- **No INTERNET permission.** The merged manifest contains no `android.permission.INTERNET`.
- `ACCESS_NETWORK_STATE` (pulled in transitively by WorkManager) is removed with
  `tools:node="remove"`.
- `android:allowBackup="false"` plus backup/data-extraction rules that exclude everything: Android
  auto-backup cannot upload the database.
- No analytics, crash-reporting, ads or attribution SDKs.
- Data lives in Room/DataStore on the device. The only data that ever leaves the app is the CSV the
  user explicitly exports.

## Build and run

Prerequisites: JDK 21, Android SDK platform 36 + build-tools 36, and either Android Studio or the
Gradle wrapper (included).

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:ANDROID_HOME='C:\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME

.\gradlew.bat --console=plain ':app:assembleDebug'        # debug APK
.\gradlew.bat --console=plain ':app:testDebugUnitTest'    # 41 unit tests
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Install with
`adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Release builds are unsigned by design (no production keystore in this repository). Signing and
Play App Signing enrolment are owner actions.

## Project layout

```
app/src/main/java/com/sabeel/steps/
  domain/        models, step math, step pipeline (pure Kotlin, unit-tested)
  data/          Room DB + DAOs, DataStore settings, repositories, CSV codec
  service/       StepCounterService (health FGS), WalkTrackingService (location FGS), WalkTracker
  platform/      permissions, notifications, reminders/workers, boot receiver
  ui/            theme, components, screens, view models, i18n (EN/AR)
app/src/test/    pure JUnit tests (pipeline, math, CSV, cadence)
docs/product/    brief (locked v1 scope) and engineering spec
docs/research/   black-box competitor research and name screening
docs/legal/      clearance memo, Play compliance checklist, privacy policy (+ hostable page)
docs/council/    Qiyas Council verdict and Step Zero test plan
docs/privacy-site/  static, tracker-free privacy page for hosting
```

## Roadmap / gates before publishing

1. Run the **Step Zero** pilot (docs/council/qiyas-council.md §6): 10 walkers, 7 days, kill metrics.
2. Host the privacy policy (only `docs/privacy-site/`) and fill the placeholders (legal name,
   email, effective date).
3. Play Console account, $25 fee, identity verification; closed test with 12 testers for 14
   continuous days (new personal accounts).
4. Arabic native review of strings, screenshots and listing; Sabeel trademark screen (Iraq/WIPO)
   and counsel/scholar sign-off.
5. Then production listing with EN + AR store assets.

## Verification snapshot (2026-11-17)

- Build: `BUILD SUCCESSFUL` (AGP 8.10.1, Gradle wrapper 8.11.1, JDK 21); **41/41 unit tests pass**
  (step pipeline, step math, CSV codec, cadence).
- Debug APK: [dist/Sabeel-1.0-debug.apk](dist/Sabeel-1.0-debug.apk), 11,488,512 bytes,
  SHA-256 `324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E`.
- Independent static verification: PASS=33, FAIL=0 — package com.sabeel.steps, targetSdk 36,
  minSdk 26, **no INTERNET**, **no ACCESS_NETWORK_STATE**, `allowBackup=false`, health/location
  foreground services, no ads/analytics/network SDKs in the release dependency graph, 16 KB
  zipalign + ELF `p_align=16384`. Evidence: [docs/legal/evidence/](docs/legal/evidence/).
- Emulator smoke (API 36): install PASS, cold launch PASS (2.9 s), onboarding renders, runtime
  permission grants PASS, reboot + boot receiver/WorkManager reschedule PASS, 0 FATAL hits.
  Deeper flows and screenshots were untestable headless; they remain open device/owner actions.

## License

No license has been chosen yet. Decide before making the repository public or accepting outside
contributions.
