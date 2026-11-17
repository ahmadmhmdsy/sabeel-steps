# Adoption smoke evidence — diagnostics, goal advice, rest day (2026-11-17)

This folder records the emulator pass that exercised the three TrueStep-inspired features on the
debug build, after the fixes listed at the bottom of this file. It is functional evidence for UI
wiring and wording only; it is **not** step-accuracy evidence.

## Build under test

- Artifact: `app/build/outputs/apk/debug/app-debug.apk`, 12,001,632 B, SHA-256
  `dcd1f3359e8554fbffc775b36eb83137bec01db662b78b7c07a5eb00dcbe615d` (debug keystore).
- Source state: working tree after the rest-day precedence fix and the OEM label fix (the same tree
  that runs 79 JVM tests, see `app/build/test-results/testDebugUnitTest/`).
- Unit-test report of record: 79 tests, 0 failures, 0 errors, 0 skipped (8 XML files, run
  2026-11-17 22:26 +03:00).

## Device

- `emulator-5554`, AVD `api36`, image `sdk_slim_x86_64` / `Android_ATD_built_for_x86_64`,
  API 36 (`Android/sdk_slim_x86_64/emu64x:16/BE2A.250530.027/13847098:userdebug/test-keys`),
  1080x2400, Arabic `ar-IQ` device locale.
- The ATD image has **no step sensor**, so live counting cannot be exercised there. That is why the
  diagnostics screen correctly reports "No step sensor" in every dump below.
- Install: `adb install -r app-debug.apk` over a previous debug install (app data kept); the app
  was force-stopped before each launch.

## Synthetic data disclosure

Because the image cannot count steps, and the goal-advice rule is gated at 14 recorded days, 20
synthetic daily rows for 2026-10-29 .. 2026-11-17 (6,500 → 8,400 steps, median 7,450) were inserted
into the debug build's own Room database (`Sabeel.db`) with the app force-stopped. The data was
removed afterwards with `adb shell pm clear com.sabeel.steps`; nothing synthetic was committed to
the repo and no step-accuracy claim is made. Without this seed the suggestion UI cannot be reached
on this emulator at all.

## Checks and observations

| # | Check | Observation | Dump |
|---|---|---|---|
| 1 | Home, English, rest day marked for today, seeded data | Ring 100% (8,400 of 8,000), "Goal reached", "Updated 10:30 PM", "Rest day. Your streak is paused.", streak **4 days in a row** (today is a hit *and* marked rest, and correctly does not add a day) | `home-en.xml` |
| 2 | Settings > Daily goal, English | "Suggest from my routine" appears only after 20 recorded days, with "Based on 20 recorded days. A suggestion changes your goal by at most 10%, and only if you tap it." | `settings-en-goal-advice.xml` |
| 3 | Tap the suggestion, English | Dialog: "Your median recorded day over the last four weeks was 7,450 steps. A goal of 7,500 steps stays close to that." Buttons "Keep 8,000" / "Use 7,500"; tapping Keep leaves the goal at 8,000 (default is keep, nothing auto-applies) | `goal-advice-dialog-en.xml` |
| 4 | Diagnostics, English | Status "No step sensor", today 8,400 steps, last update 10:27 PM, permission allowed; corrected wording "reconnects when you open the app and after your phone restarts"; "A rest day never changes the counted steps"; maker title **"On this phone (Android)"** (no raw `unknown`) | `diagnostics-en.xml` |
| 5 | Switch to Arabic in Settings, then Home | Arabic UI, streak "4 أيام في السلسلة", "آخر تحديث 10:30 م", rest-day pause note | `home-ar.xml` |
| 6 | Settings > Daily goal, Arabic | "اقترح من روتيني" with the recorded-days hint; suggestion dialog: "كان وسيط أيامك المسجّلة ... 7,450 خطوة. هدف 7,500 خطوة ..."; "أبقِ 8,000" keeps the goal | `settings-ar-goal-advice.xml`, `goal-advice-dialog-ar.xml` |
| 7 | Diagnostics, top and bottom, Arabic | Corrected reconnect and rest-day wording, "على هذا الهاتف (Android)", OEM guidance, "Open battery settings" / "Open app settings" | `diagnostics-ar.xml`, `diagnostics-ar-bottom.xml` |

## Crash evidence

`adb logcat -d -b crash` returned an empty buffer, and
`adb logcat -d | Select-String 'FATAL EXCEPTION|AndroidRuntime.*com.sabeel'` returned no matches
after the whole pass; the captured transcript is in [crash-buffer.txt](crash-buffer.txt). This is a
sampled check, not a proof about other devices.

## Bugs found by this pass and fixed before commit

1. **Rest day could still add a day to the streak.** `StepMath.streak` checked "reached goal" before
   "marked rest", so a day that was both a hit and a rest counted. The emulator showed 5 days where
   the contract says 4. Fix: check rest first (a rest day never adds, even with enough steps); two
   new JVM tests (`restDayWinsOverAGreatStepDay`,
   `markedRestDayIsNotCountedEvenWhenStepsReachTheGoal`) fail before the fix and pass after it.
2. **OEM label leaked the raw value `unknown`.** The ATD reports `Build.MANUFACTURER=unknown`, and
   the diagnostics title rendered "On this phone (unknown)". Fix: treat a blank or `unknown`
   manufacturer as the generic "Android" label; `OemBatteryGuideTest` covers aliases, siblings,
   unknown/blank and an unusual maker.

## Files here

- `home-en.xml`, `home-ar.xml`
- `settings-en-goal-advice.xml`, `settings-ar-goal-advice.xml`
- `goal-advice-dialog-en.xml`, `goal-advice-dialog-ar.xml`
- `diagnostics-en.xml`, `diagnostics-ar.xml`, `diagnostics-ar-bottom.xml`

Raw `uiautomator dump` XML, exactly as produced on the emulator (one line each; open in an XML
viewer). No screenshots: the ATD image returns black `screencap` frames, as recorded in
`docs/review/2026-11-17/smoke/SMOKE-EVIDENCE.md`.
