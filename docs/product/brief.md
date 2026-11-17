# Project brief — original step & walk tracker (clean-room)

Status: locked scope v1. Owner: Lead. Date: 2026-11-16.
Workspace: E:\android_apps\steps_app

## 1. What we are building
An ORIGINAL Android app in the step-counter / walking-tracker category. Same *idea* as six
public competitors (step count, daily goal, history, walk tracking), but every line of code,
every string, every icon, colour and layout is written by us. No decompiled code, no APK,
no screenshots, no copy, no brand assets, no trademarked names.

## 2. Positioning (the gap the competitors leave)
- Offline-first and private: the app requests NO INTERNET permission in v1. Steps, walks and
  routes never leave the phone. No account, no cloud, no analytics SDK.
- Zero ads in the core loop (v1 has no ad SDK at all).
- Arabic + English, full RTL. Arabic is a first-class language, not a translation afterthought.
- Accurate: read the hardware step counter (TYPE_STEP_COUNTER) and reconcile with a
  boot-safe daily aggregation; show honest estimates, never fake precision.
- Light on battery: no continuous sensor polling; event-driven sensor + FOREGROUND service only
  while a walk is being tracked.

## 3. Name & package (decided 2026-11-16 after name screening)
- Display name: "Sabeel" (سبيل) — chosen after screening (docs/research/name-check.md). The provisional
  "Khatwa" was rejected: same-concept Arabic offline step project + 12 same-name Play apps. Trademark
  clearance for Sabeel still required before the public listing.
- Application ID: com.sabeel.steps — permanent once the first build is published.
- minSdk 26, targetSdk 36, compileSdk 36 (Play requires API 36 for new apps since 2026-08-31).
- Languages: English (default) + Arabic (values-ar), RTL.

## 4. v1 feature set (nothing else ships in v1)
F1 Onboarding: language, daily step goal, units (metric/imperial), optional height/weight, privacy note.
F2 Step counting: hardware TYPE_STEP_COUNTER via a foreground service; survives reboot and
   date rollover; today's total always correct even after service death.
F3 Home: progress ring (steps/goal), distance (stride estimate), active calories (MET-style estimate),
   active minutes, streak, gentle status text; one-tap start walk.
F4 History: last 7/30 days bar chart + per-day list + week totals; data from local Room DB.
F5 Walk tracking: start/stop GPS walk (foreground service, type location); live time, distance,
   pace, route polyline drawn on our own Canvas (NO map tiles, NO Google Maps key, NO network);
   save walk; graceful sensor-only fallback if location denied.
F6 Insights: 7-day average, best day, goal-hit rate, streak; plain-language summary.
F7 Reminders & goal notifications: daily reminder (user-set time) + goal-reached notification;
   WorkManager + POST_NOTIFICATIONS runtime permission on API 33+.
F8 Settings: goal, units, reminder on/off + time, language switch (in-app, no system change needed),
   data export (CSV via SAF) and delete-all-data, privacy policy screen, about.
F9 Accessibility: content descriptions, 48dp targets, contrast, TalkBack order, font scaling.

Explicitly out of v1: accounts, cloud sync, ads, subscriptions, social, Health Connect,
wearables, maps/tiles, INTERNET permission, analytics.

## 5. Tech stack (fixed, buildsmith owns the build files)
Kotlin 2.0.21, Compose (BOM 2024.12.01) + Material3, Navigation-Compose 2.8.5,
Room 2.6.1 via KSP 2.0.21-1.0.28, DataStore Preferences 1.1.1, WorkManager 2.10.0,
kotlinx-coroutines, Lifecycle 2.8.x, Activity-Compose 1.9.3, AGP 8.7.3, Gradle 8.11.1,
JDK 21 (toolchain jvmTarget 17). Unit tests with JUnit4 + Robolectric only if cheap; DB tests may
use Robolectric or in-memory Room under Robolectric. No third-party UI/chart libraries: charts by Canvas.

## 6. Architecture (single :app module)
- data/db: Room entities (DailySteps, Walk, Reminder?), DAOs, AppDatabase.
- data/prefs: SettingsRepository (DataStore): goal, units, height, weight, stride, language, reminder.
- data/repo: StepsRepository (daily aggregation, boot reconciliation), WalkRepository.
- domain: StepMath (distance/calories/active minutes), formatters, date keys; pure Kotlin, unit-tested.
- service: StepCounterService (foreground, type health), WalkTrackingService (foreground, type location).
- platform: BootReceiver, reminders (WorkManager workers).
- ui: theme (original palette/type), components, screens (onboarding, home, history, walk, insights,
  settings, privacy), AppNav.
- util: notification helper, permission helper, CSV exporter.

## 7. Non-negotiables (from the halal/legal clearance)
1. No copied expression: code, text, icons, images, sounds, UI arrangement copied 1:1, internal names.
2. No competitor names/trade dress in the app, listing or marketing.
3. No false claims: what the listing says must equal what the code does (e.g. "offline" is provable).
4. No selling/sharing personal data; no ad SDK in v1; no unnecessary permissions.
5. Health estimates labelled as estimates; no medical claims.
6. Halal product rules: no gambling, no riba, no manipulative subscription dark patterns, no music by
   default (optional vibration/short tone), honest goals and no shaming language.
7. Accessibility and Arabic correctness are acceptance criteria, not polish.

## 8. Definition of done for the session
- 'gradle assembleDebug' succeeds on this machine; debug APK path reported.
- Unit tests for StepMath + daily aggregation pass; 'lintDebug' reviewed (no fatal).
- All screens present and navigable; Arabic + English strings; RTL; dark + light theme.
- Docs: docs/product/spec.md, docs/legal/clearance-memo.md, docs/council/qiyas-council.md.
