# v1.1 Health Connect and estimate-provenance verification — 2026-11-21

Scope: the v1.1 change set (read-only Health Connect step floor, estimate/source
provenance in the schema/CSV/UI, Room v1→v2 migration). This folder holds the
emulator evidence; unit and static evidence is summarised below and in
[CLAIMS-AUDIT.md §8](../../../release/CLAIMS-AUDIT.md).

## Environment and artifacts

- Emulator: API 36 ATD image (1080x2400), package `com.sabeel.steps` versionCode 1 /
  versionName 1.0.0, **no hardware step counter or detector**, Health Connect
  controller present on the image.
- Build verified in this pass with
  `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` and
  `gradlew.bat :app:assembleRelease :app:bundleRelease` (BUILD SUCCESSFUL).
- Release artifacts installed: `dist/Sabeel-1.0-release.apk`, 1,861,838 B,
  SHA-256 `c5bd200da95d3fd1f1c417fb6c11fbe8e06700cb6a75c07e5ca793f1a1dc4958`,
  signed with the upload key (cert `7b12ad6b…6152d`), `zipalign` and
  `apksigner verify` OK. Debug APK: 15,188,660 B,
  SHA-256 `72d97c3b…6b238`. AAB: 3,977,681 B,
  SHA-256 `7a2fd53a…6bc77`, `jarsigner -verify` OK.

## Emulator pass (release APK over the previous release install)

The APK was installed with `adb install -r` over the previously installed,
release-signed build. The signature matched, so the existing database was kept;
this is the upgrade path a Play update takes.

| # | Evidence file | Observed |
|---|---|---|
| 1 | [ui-01-home-estimate.xml](ui-01-home-estimate.xml) | App launched after the upgrade in 1.3 s (`Displayed … +1s325ms`). Home rendered the labelled estimate: "≈ 0 of 8,000 steps" and "Estimated from the phone's motion, not a hardware step counter." No crash. |
| 2 | [ui-02-settings-hc-card.xml](ui-02-settings-hc-card.xml) | Settings shows the Health Connect card only because this phone has no measured step sensor: heading "Health Connect", row "Use synced steps", explanatory text, "Connect". |
| 3 | [ui-03-health-connect-read-steps.xml](ui-03-health-connect-read-steps.xml) | Tapping "Connect" opens Health Connect's own permission screen for Sabeel: "Allow Sabeel to access your fitness and wellness data?", **read** access only, the single data type **Steps** ("Allow “Sabeel” to read"), and Don't allow / Allow. No write option is requested. |
| 4 | [ui-04-settings-after-decline.xml](ui-04-settings-after-decline.xml) | Backing out without granting leaves the app unchanged: still "Connect", no permission assumed, counting continues as the labelled estimate. |
| 5 | [ui-05-diagnostics-estimate.xml](ui-05-diagnostics-estimate.xml) | Diagnostics states the truth: "Counting (estimated)", 0 steps, Source "Phone motion (estimated)", Fallbacks "None", and the Health Connect paragraph: steps "are applied only as a floor — Sabeel never writes to Health Connect". |
| 6 | [ui-06-settings-ar.xml](ui-06-settings-ar.xml), [ui-07-diagnostics-ar.xml](ui-07-diagnostics-ar.xml) | In-app Arabic switch: the Health Connect card and the source/estimate labels are localised (`استخدام الخطوات المزامنة`, `يتم العدّ (تقديري)`, `حركة الهاتف (تقديري)`). |
| 7 | [ui-08-insights-ar-after-migration.xml](ui-08-insights-ar-after-migration.xml), [ui-09-history-ar-after-migration.xml](ui-09-history-ar-after-migration.xml) | Old rows survived the Room v1→v2 migration: Insights reports the best day "الخميس 19 نوفمبر" with 40 steps, and History lists 19 Nov 2026 — 28 m — 40 steps plus the empty days around it. The new `estimated_steps` / `estimated` columns therefore migrated without data loss. |

### Estimate walk on the same sensor-less image

Activity recognition and location were granted with `adb shell pm grant` (the
walk screen's location prompt was bypassed for the test). A walk was started,
a ~2 Hz acceleration square wave was injected through the emulator sensor
console for about a minute, and the walk was saved.

| # | Evidence file | Observed |
|---|---|---|
| 8 | [ui-10-walk-live-estimated-ar.xml](ui-10-walk-live-estimated-ar.xml) | Live walk: "≈ 81" steps with the note "your phone has no step sensor, so this walk's steps are estimated from phone motion"; the notification/summary wording is the estimated one. |
| 9 | [ui-11-walk-summary-estimated-ar.xml](ui-11-walk-summary-estimated-ar.xml) | Saved summary: 1:30, **≈ 81** steps, 57 m (stride fallback; the emulator had no GPS fix), the same estimated note. |
| 10 | [ui-12-history-estimated-badges-ar.xml](ui-12-history-estimated-badges-ar.xml) | History: today's row shows 81 steps with the "includes estimated steps" badge, and the saved-walk row repeats the badge with 1:30 · 57 m · 81 steps. Two separate surfaces now carry the marker. |
| 11 | [ui-13-saved-walk-dialog-estimated-ar.xml](ui-13-saved-walk-dialog-estimated-ar.xml) | The saved-walk dialog repeats the provenance: part of this walk's steps is estimated from phone motion, not measured by a step sensor. |

This proves the **rendering and persistence plumbing** of the walk estimate
flag (live → summary → history row → saved-walk dialog). It is not an accuracy
test: the steps came from a synthetic waveform, and the walk counted through the
estimate ladder because the image has no measured sensor.

The crash buffer was empty after the pass. One crash entry did appear while
dumping UI hierarchies — `UiAutomationService … already registered` from the
`uiautomator` dump tool itself, not from Sabeel — and the buffer was empty on
re-check after it was cleared.

What this pass does **not** prove: the Health Connect grant was not completed on
the emulator (the coordinate tap for "Allow" fell through to the gesture
navigation area and left the permission screen, so the dialog was backed out
instead). No watch or app writes steps to Health Connect on this image, so the
synced reading, the floor reconciliation (`max(current, hcToday)`), the synced
notification line and the 30-minute staleness fallback are still unexercised and
remain an owner hardware item. The walk pass used the estimate ladder only: a
walk counted by a real counter or detector, and the GPS route sketch, still need
real hardware. The measured counter → detector ladder is unchanged and still
needs real hardware.

## Unit and static evidence (same tree)

- `testDebugUnitTest`: **121 tests, 0 failures/errors/skips, 11 suites** —
  CsvCodec 15, StepMerge 7, StepMath 24, StepPipeline 26, StepCadence 5,
  GoalAdvisor 9, LocalizationParity 4, OemBatteryGuide 5, MotionStepDetector 8,
  StepSourceHealth 9, StepSourcePolicy 9. Relevant cases:
  `StepPipelineTest` covers the Health Connect absolute floor, day rollover,
  negative totals, estimate tracking/preservation and the trusted-clock clamp
  skip; `StepSourcePolicyTest` covers the HC ordering/fallback/flags;
  `CsvCodecTest` covers legacy 5-column import, 6-column round-trip and a
  rejected row whose estimate exceeds its step total.
- `lintDebug`: 0 errors, 44 warnings, 1 hint (non-blocking).
- Room schema version 2 is exported at
  `app/schemas/com.sabeel.steps.data.db.AppDatabase/2.json`
  (identity hash `457d96f3d646b8b49e6481015a89a024`) and `MIGRATION_1_2` adds
  `daily_steps.estimatedSteps` and `walks.estimated` with
  `INTEGER NOT NULL DEFAULT 0`; the build has no
  `fallbackToDestructiveMigration`.
- An audit of `app/src/main` finds no Health Connect write API
  (`insertRecords`, `writeRecords`, `deleteRecords`): the only client calls are
  permission checks and an aggregate `StepsRecord.COUNT_TOTAL` read.
- Release build: `lintVitalRelease` and R8 completed; the APK's permission list
  is unchanged except for `android.permission.health.READ_STEPS`, and it still
  has **no INTERNET permission**.

## Follow-up (owner)

1. Run §5b HP1–HP7 of [HARDWARE-QA-CHECKLIST.md](../../../legal/HARDWARE-QA-CHECKLIST.md)
   on a phone whose watch/app writes steps to Health Connect.
2. Play Console: Data safety and Health Connect declarations per
   [PLAY-SUBMISSION-CHECKLIST.md](../../../release/PLAY-SUBMISSION-CHECKLIST.md)
   §4.7, §4.8, §4.12, §4.13, and the Health Connect data-access form.
3. Privacy policy v1.1 must be filled and hosted before the Health Connect
   declaration is accurate.
