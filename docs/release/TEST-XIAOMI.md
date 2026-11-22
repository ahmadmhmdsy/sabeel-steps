# Sabeel v1.3 — physical test checklist for a Redmi Note 7 (MIUI)

**Who:** the owner (or someone with the phone in hand), with the Lead on the other end of the report. **Why:** v1.3 adds counting on/off, boot-restart evidence and custom sessions; the emulator has no real step sensor, so this phone is the only place the counting behaviour can be believed. **Build to test:** the **release** APK built from the v1.3 tree on 2026-11-23 — `dist/Sabeel-1.3.0-release.apk` (2,138,010 B, SHA-256 `487ee9c9f7e7d0e1bbe490c4b7692c72cb83c0f8d963cf1f2a4545677a4dfe5d`, versionName 1.3.0 / versionCode 3, signed with the upload key). If you test a debug build instead, write "DEBUG" on every result — a debug result is supporting evidence, not a release result.
**Time:** about 45–60 minutes if nothing fails (plus the walk). **Risk:** none to the phone; the app has no INTERNET permission. Keep the old app data — the upgrade test matters (Room v2 → v3 migration).

**Device target:** Xiaomi Redmi Note 7 (codename `lavender`). It shipped with Android 9 (MIUI 10) and can be on Android 9/API 28 or Android 10/API 29 after updates. **Record which one you have** in §0; several expectations differ by version (notification permission does not exist on Android 9; the foreground-service behaviour differs).

---

## 0. Record the environment first

| Field | Your value |
|---|---|
| Phone model / codename | Redmi Note 7 (`lavender`) |
| Android version + API level (Settings → About phone → see "Android version"; API 28 = Android 9, 29 = Android 10) | |
| MIUI version (Settings → About phone → MIUI version) | |
| Build tested: release or debug | |
| APK file name + SHA-256 (from the build folder; if you cannot compute it, ask the Lead) | |
| Date + time (with timezone) | |
| Old Sabeel version installed before this test (if any) | |
| Did you keep the old data during install? (upgrade / clean install) | |

**Tip:** take screenshots as you go. Name them `01-onboarding.png`, `02-home-on.png`, … so they match the row numbers below. Put them all in one folder named `v13-redmi-<date>`.

---

## A. Install / upgrade

1. **If an older Sabeel release is installed** (v1.1/v1.2): do **not** uninstall it. Install the v1.3 APK over it (`adb install -r app-release.apk` or tap the file in MIUI's File Manager and allow "Install unknown apps" for that app).
   - **Expect:** install succeeds, app opens, previous step history and settings are still there, no crash. This tests the Room v2→v3 migration and the new session tables.
   - **If it crashes or the history disappears: STOP and report** (logcat below) — data loss is a stop-ship bug.
2. **If there is no older install:** clean install. Then this build's onboarding test (B) applies.
3. Open Sabeel → Settings → About (or the Diagnostics footer): confirm it says **1.3.0** / version code 3. Screenshot.
4. Lock the phone, unlock, reopen: still works.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## B. Onboarding and permissions (fresh install only)

1. The first screen must explain what Sabeel reads (physical activity / step sensor) **before** Android's permission dialog appears.
2. Tap the "allow" path → Android's physical-activity dialog appears → Allow. **Expect:** onboarding continues; Home opens.
3. Repeat a fresh install (uninstall + install) and **deny** the permission this time. **Expect:** the app explains, does not crash, and shows a non-counting state with a way to enable later in Settings. It must not loop the permission prompt.
4. Set language to Arabic (Settings → Language) and back to English. **Expect:** no crash and correct RTL layout, including the new v1.3 session setup/live screens (Arabic strings are in the tree, but no native speaker has reviewed them yet). Record any clipped, mixed-language or reversed-layout text — those are exactly the reports the native review needs.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## C. MIUI setup (do this before the counting tests)

MIUI kills background apps aggressively; the app must be tested in a state the owner will actually use.

1. **Autostart:** Settings → Apps → Manage apps → Sabeel → **Autostart** = ON. (Path may be Settings → Apps → Autostart on some MIUI versions.)
2. **Battery:** Settings → Apps → Manage apps → Sabeel → **Battery saver** = **No restrictions**. On MIUI 11/12 the option may be under "Battery & performance → App battery saver → Sabeel → No restrictions".
3. **Lock in Recents (optional, recommended):** open Recents, long-press the Sabeel card, tap the lock icon so MIUI keeps it.
4. **Notifications:** long-press the Sabeel notification when it appears → make sure the "Step counting" channel is not blocked; on Android 10 check Settings → Apps → Sabeel → Notifications is ON.
5. Note which steps existed on your MIUI version, because "the app stopped" reports often come from these three switches.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## D. Count 100 real steps

1. Counting ON (ring ring shows "Counting on"). Home shows today's number.
2. Walk **exactly 100 steps** with the phone in your hand or pocket (count them or use a second phone's counter; a treadmill count is not valid here). Keep the screen off or the app in the background for at least part of the walk — that is the real use.
3. Open Home: the number should have increased by about 100 (written tolerance: record the actual delta; do NOT expect exact 100 — step sensors and pocket motion differ; report the delta, e.g. "98" or "103").
4. Check the ongoing notification exists while counting (Android 9/10 shows the foreground-service notification; Android 10 may group it).
5. Open Diagnostics: display source / last event age look alive; no "storage fault" row.

☐ PASS ☐ FAIL ☐ BLOCKED — notes: (before → after, delta)

---

## E. Ring off → on, and the Settings mirror

1. Tap the Home **ring** once. **Expect:** ring dims, pill becomes "Counting off · Tap to start", notification disappears, Diagnostics later shows OFF_BY_USER (not "stopped by system").
2. Tap the **pill** (or the ring again) → counting ON. Pill: "Counting on · Tap to stop".
3. Open Settings: the "Count steps" switch must show the **same** state. Toggle it there → Home reflects it.
4. Turn counting OFF, force-stop the app (see G), reopen: it must still be OFF (the choice is remembered; it must not silently turn itself on).
5. Walk 30 steps while OFF: the Home total must **not** increase while the app is open (counting is off); turn ON afterwards and note whether the walk made it in (it should not; the off period is not captured).

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## F. Reboot with counting ON (the owner's main worry)

1. Counting ON. Note the Home total.
2. Reboot the phone (power button → Restart). Do not open Sabeel after the reboot for 2–3 minutes.
3. **Watch the notification shade after boot:** either (a) the "Sabeel is counting your steps" notification appears again, or (b) a single "Step counting is paused" recovery notification appears. Both are honest outcomes; (b) means Android/MIUI blocked the restart.
4. Open Sabeel → Diagnostics → **"Last boot attempts"**. **Expect:** one entry for this boot with time, flags (onboarded / activity permission / sensor / Health Connect), a result, and "Still counting after the check: yes/no". Screenshot it.
5. Walk 50 steps without opening the app first if the notification appeared, then open Home: did the count include them? Record the answer; do not assume.
6. Repeat the reboot with counting OFF: after boot it must **stay off** (no counting notification, Diagnostics may show the attempt with "enabled=false").

If the notification never appears and Diagnostics shows a blocked start, that is the **expected Android/MIUI limitation** for this build, not a test failure — send the Diagnostics text (§M) because the boot evidence is exactly what the owner needs.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## G. Force-stop — expected failure mode

1. Settings → Apps → Manage apps → Sabeel → **Force stop** (or long-press in Recents → App info → Force stop).
2. Wait 1 minute. **Expected:** counting does **not** come back automatically and the notification is gone. Android does not allow an app to restart itself after a force-stop; Sabeel cannot fix this, and the release notes say so.
3. Reopen Sabeel. **Expect:** counting resumes (the app re-syncs on open), Home still shows the saved day, and no crash. The "counting off" pill must not be shown just because the service was killed — the persisted choice is still ON.
4. Diagnostics: check the "service status" row and the boot attempts (a force-stop is not a boot; do not expect a boot row).

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## H. Session without GPS (walk/run)

1. Home → "Start a session" → setup. Choose **Walk**, **no goal** (or Steps goal 60), **location OFF**. Start.
2. Walk ~60 steps while the session screen is open. Session shows its own steps and time; Home still shows the whole day.
3. **Pause** the session. Walk 100 steps while paused (phone in hand). **Resume.** **Expect:** the paused 100 do **not** appear in the session's elapsed/steps after resuming (no back-fill); session steps continue only from new steps. The Home day total **may** include the paused steps (global counting is separate) — record both numbers.
4. Finish and save. **Expect:** a summary, and the session appears in History with the right steps/time, labelled walk/run. No duplicate counting: the day total must not jump by the session's steps again.
5. Start a new session while one is running (if the UI allows it): only one active session is expected; record what happens.
6. If the session screen is closed/backgrounded mid-session: open Home and check whether the session is still running and how you get back to it. Record the behaviour.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## I. Session with GPS

1. Start a session with **location ON**. **Expect:** an in-app explanation before Android's location dialog; allow "while using the app" (precise).
2. Walk a short loop (100–200 m). **Expect:** the route line grows and distance increases; no map tiles appear (none should be downloaded); no network error.
3. Press Home for 2 minutes, then return to the session screen. **Expect:** the app's own description says route/distance are recorded while this screen is open; record whether distance paused while you were away and whether the session itself kept counting steps (it should).
4. Deny location in a second session: the session must still track steps/time and say no route is recorded. No crash, no repeated prompt.
5. Finish, save, open History → the session: route/thumbnail or distance as designed; delete-all later must remove it.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## J. Place goal near/far — check whether it is testable in this build

**Important:** the v1.3 tree read for this checklist has the place **data layer** and the place picker in session setup, but **no screen or button that creates a saved place** (no "add place" UI was found in the build sources at the time of writing). If the build you are testing has no way to save a place:
- open the session setup → goal = Place: it will most likely say "No saved places yet";
- **mark this section BLOCKED (no place-creation UI)** and tell the Lead; do not invent a place by ADB.

If a place-creation UI exists in your build:
1. Save a place at your current location (label it "Test near", radius default).
2. Start a session with goal = that place, auto-stop ON, walk **towards** it; stop just before the radius. **Expect:** no auto-stop yet.
3. Walk into the radius. **Expect:** the session stops and saves automatically (or the goal is reported reached, depending on the build); no double-count on Home.
4. Save a far place (~2 km away) and repeat with auto-stop: the session must keep running; with no auto-stop it reports "goal not reached" when you finish.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## K. Daily goal notification

1. Set the daily goal low (Settings → goal, e.g. 100 steps) so the walk reaches it.
2. Walk until the goal is crossed. **Expect:** one "Daily goal reached" notification (not a stream of them), tapping it opens Sabeel; the Home ring shows the goal met.
3. Repeat on the next day is optional (do not change the phone date — a fake date can confuse the day rollover and is not evidence).

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## L. Share diagnostics (the report the Lead needs)

1. Open Diagnostics → "Share diagnostics".
2. **Expect:** the Android share sheet opens with a text preview; the text contains boot attempts, counting state, permissions/sensor flags — and **no coordinates or saved-place names**. Screenshot the preview.
3. Send it to yourself (email/Telegram/Notes) and then forward it with the test report. This text is the single most useful artifact from this phone: it says whether counting was meant to be on, whether the boot attempt was blocked, and what the last heartbeat was.

☐ PASS ☐ FAIL ☐ BLOCKED — notes:

---

## M. Logs (optional, only if something fails)

Enable USB debugging (Settings → About phone → tap MIUI version 7 times → Developer options → USB debugging), connect to a PC with the Android SDK, then:

```powershell
adb devices
adb install -r app-release.apk
adb shell dumpsys package com.sabeel.steps | Select-String "versionName|versionCode|ACTIVITY_RECOGNITION|ACCESS_FINE_LOCATION|POST_NOTIFICATIONS|FOREGROUND_SERVICE|INTERNET"
adb shell dumpsys activity services com.sabeel.steps
adb logcat -c
# reproduce the problem, then:
adb logcat -d -v time | Select-String "sabeel|AndroidRuntime|FATAL|ForegroundService" | Tee-Object -FilePath v13-logcat.txt
adb shell dumpsys jobscheduler | Select-String "com.sabeel.steps" | Tee-Object -FilePath v13-jobs.txt
adb shell dumpsys deviceidle whitelist | Select-String "sabeel"
```
If a crash happens, the important line starts with `FATAL EXCEPTION` / `AndroidRuntime`. Save the file, do not paraphrase it.

☐ logs attached ☐ not needed

---

## N. Emulator commands (for the Lead/build team, not a substitute for this phone)

Use the API 36 AOSP-ATD AVD; it has **no hardware step sensor**, so real counting cannot be validated there.

```powershell
# start the AVD (adjust the path)
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd <avd-name> -no-snapshot
adb wait-for-device
adb install -r app-debug.apk
# route / permission checks
adb shell pm grant com.sabeel.steps android.permission.ACCESS_FINE_LOCATION
adb emu geo fix 44.36 33.31        # Baghdad lon lat; watch the session route
# debug-only step injection (DEBUG build; absent from release)
adb shell am broadcast -a com.sabeel.steps.DEBUG_STEPS --ei steps 100 -n com.sabeel.steps/.debug.DebugStepReceiver
# reboot behaviour
adb reboot
adb shell dumpsys activity services com.sabeel.steps
```
Expected emulator limits: no step counter/detector (Home shows zero or estimated only), location is synthetic, and screenshots from the ATD image may be black — use `adb exec-out screencap -p > shot.png` from the host or a Google-APIs image if the Lead approves. **Emulator rows are "NOT VERIFIED — emulator-attempted", never PASS for physical behaviour.**

### N.1 Evidence actually captured on `emulator-5564` (2026-11-22, debug, versionCode 3)

This is supporting evidence only; the debug injector caps each broadcast at 10 detector steps, so the numbers below test logic/flows, not sensor accuracy.

| Check | Command / action | Observed |
|---|---|---|
| Reboot with counting ON | `adb reboot`, then `dumpsys activity services` | `StepCounterService` warning-free FGS started from `BOOT_COMPLETED`; log `Background started FGS: Allowed ... code:BOOT_COMPLETED`; diagnostics: `10:57 PM — BOOT_COMPLETED / Result: STARTED / Still counting after the check: Yes` |
| Reboot with counting OFF | ring off, `adb reboot` | no `StepCounterService` record; Home opens with `Counting off · Tap to start` and `Today's total — paused`; OFF survived reboot |
| Ring toggle | tap ring | ON → OFF stops the service immediately; OFF → ON restarts it; pill/Settings mirror update |
| Session while counting OFF | Start a session → prompt | dialog `Counting is off / Turn counting on to start this session`; `Turn on and start` starts both `WalkTrackingService` and `StepCounterService` |
| Session pause/resume | 4×10 steps, Pause, 2×10 steps, Resume | paused screen froze at 40; resume kept 40; 10 more → 50; saved session #1 = 50 steps / 44 s / 35 m (projection, no double count) |
| History export CSV | `Export all sessions (CSV)` → SAF save | 18-column CSV written; row `1,2026-11-22,...,WALK,COMPLETED,50,35.28,44,...,1` |
| History export GPX | session details → `Export GPX` | valid GPX 1.1 track with 1 synthetic point; no crash when no route exists |
| Place goal + auto-stop | Add place `Home` 39.237255,-123.150032 r100; PLACE goal; auto-stop ON; `adb emu geo fix` | session auto-saved as #2 `Goal met`, WalkTrackingService stopped, goal notification `ID_SESSION_GOAL=4107` posted (`Goal reached`) |
| Diagnostics | Settings → Why is my count low? | two boot attempts with `Result: STARTED` and `Still counting after the check: Yes`; battery optimisation reported `Restricted` on the AVD (expected — allow it on the real phone per §C) |

**Release-signed smoke test (2026-11-23, separate clean API 36 emulator):** `dist/Sabeel-1.3.0-release.apk` (the measured, R8-minified artifact) installed and launched; onboarding completed; Home opened with counting on; a session started with both `WalkTrackingService` and `StepCounterService` alive and the live screen reported `Location is recording`; discarding removed the walk service and left counting on. This proves the release build runs, but it is still not physical-device evidence.

**Not covered here:** real step-sensor accuracy (Step Zero), MIUI autostart/battery behaviour, force-stop recovery, real GPS, Android 9/10 notification differences, migration from a real v1.2 install on the phone.

---

## O. What to send back

Send one message (Telegram/email) to the Lead with:
1. §0's environment table (copy it).
2. Each section letter with PASS / FAIL / BLOCKED + the short note you wrote.
3. Screenshots named `01-…` etc. in one folder/zip.
4. The **Diagnostics share text** (§L) — paste it in the message.
5. If anything failed: the `v13-logcat.txt` (§M) and the exact time it happened.
6. A one-line verdict: "safe to continue to Play prep / needs fixes first".

**Stop and report immediately if:** the app crashes on install/migration, the old data is lost, counting produces a wildly impossible number (e.g. thousands of steps while still), the session double-counts on Home, or delete-all leaves sessions/routes behind.

---

## P. Honest limits this checklist does not test

- The **physical Step Zero** counter-vs-detector falsification test (COUNCIL.md) is only meaningful with a real step sensor; this checklist covers the owner-visible behaviour, not the sensor tolerance.
- Health Connect with real watch data (the phone may have no HC step source); that stays on the hardware checklist.
- Google Play review, closed testing with 12 testers, the FGS demo video, and the privacy host are **not** tested by this checklist.
- MIUI behaviour varies by version and by the owner's three switches (§C); a pass here is not a promise that every Redmi/Android 10 phone behaves the same.
