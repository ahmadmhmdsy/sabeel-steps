# Research — counting control, restart hardening and custom sessions

Date: 2026-11-22 · Author: lead · Status: evidence gathered, physical-device evidence still missing
Scope: the owner's v1.2 field report (real phone): counting does not restart after reboot; the Home ring should toggle counting; the primary CTA should open custom walk/run sessions instead of starting global counting.
Companions: `COUNCIL.md` (same folder), `docs/superpowers/plans/2026-11-22-counting-control-and-custom-sessions.md`.

Method: (1) inspect the shipped v1.2 tree; (2) read the current Android platform documentation for boot-started foreground services, while-in-use permissions, Health Connect and Play requirements; (3) cross-check OEM background-killing behaviour from the public field database; (4) separate what is provably legal from what is environmental and cannot be fixed in-app. No claim below is based on a device log from the owner's phone yet — that is the largest evidence gap.

## 1. Field report and what it does and does not prove

The owner installed v1.2 on a real phone and reports:

1. After a reboot/power cycle, counting does not restart; he wants counting to continue until **he** turns it off.
2. Tapping the Home progress ring should turn counting off on the first tap and on on the next tap.
3. "Start walking" (`home_start_walk` / "ابدأ المشي") must not start global counting; it should open a customization page for a custom walk/run session: optional GPS, target by time/steps/distance/place/end place, multiple sessions tracked. Home keeps the global day total; the session page shows session-only stats.

What this proves: the current product does not meet the owner's intent, and the UI has no control for stopping/starting counting deliberately.

What it does **not** prove: whether the boot failure is (a) the boot receiver not being delivered, (b) the receiver running but `ensureStarted()` returning false, (c) the platform refusing the FGS start, (d) the service starting and then being killed by the OEM battery manager, or (e) the user having force-stopped the app. The code currently records none of these outcomes, so the failure is unattributable from the repo alone.

## 2. Platform facts that constrain the design

### 2.1 Boot-completed receivers may start a health FGS on stock Android

Android lists `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` among the exemptions from background foreground-service start restrictions ([FGS background-start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)):

> "Your app receives an event that's related to geofencing or activity recognition transition. After the device reboots and receives the ACTION_BOOT_COMPLETED, ACTION_LOCKED_BOOT_COMPLETED, or ACTION_MY_PACKAGE_REPLACED intent action in a broadcast receiver."

Android 15 added a restriction on **which service types** a `BOOT_COMPLETED` receiver may launch. The restricted list is `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` and `microphone`; `health` is absent ([Android 15 behavior changes](https://developer.android.com/about/versions/15/behavior-changes-15#fgs-boot-completed)):

> "BOOT_COMPLETED receivers are not allowed to launch the following types of foreground services: dataSync camera mediaPlayback phoneCall mediaProjection microphone … If a BOOT_COMPLETED receiver tries to launch any of those types of foreground services, the system throws ForegroundServiceStartNotAllowedException."

The health type's runtime prerequisite is satisfied by `ACTIVITY_RECOGNITION`; the while-in-use restriction that blocks location/camera/microphone from the background explicitly does not apply to that case ([FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)):

> "if your app is in the background, and it tries to create a foreground service of type camera, location, or microphone, the system sees that your app doesn't currently have the required permissions, and it throws a SecurityException. Similarly, if your app creates a health service that needs the BODY_SENSORS permission while in the background, the system throws an exception. (This doesn't apply if the health service needs different permissions, such as ACTIVITY_RECOGNITION.)"

Consequence: **the v1.2 boot path is legal on stock Android 14/15/16.** A real-phone failure is therefore environmental (OEM, force-stop, battery manager, permission) or a swallowed code no-op — not a platform prohibition. The fix must include evidence, not just another start attempt.

### 2.2 What cannot be fixed in the app

- **Force-stop.** When the user force-stops an app, Android places the package in the stopped state; its receivers and services do not run again until the user launches the app. `dumpsys package com.sabeel.steps` shows `stopped=true`. No in-app code can revive it. This is the hard boundary behind the owner's "no matter what" phrasing.
- **OEM autostart/battery managers.** Xiaomi/MIUI, Samsung, Huawei, Oppo, vivo and others add non-standard background restrictions. The public field database documents the Xiaomi case plainly ([dontkillmyapp.com/xiaomi](https://dontkillmyapp.com/xiaomi)):

  > "There are no APIs and no documentation for those extensions. In default settings, background processing simply does not work right and apps using them will break."

  Samsung's own 2024 statement is narrower than a guarantee: "Since One UI 6.0, foreground services of apps targeting Android 14 will be guaranteed to work as intended so long as they are developed according to Android's new foreground service API policy" ([dontkillmyapp.com/samsung](https://dontkillmyapp.com/samsung)). It does not cover every OEM or every battery mode.
- **No background activity launch.** Android does not let an app open its own UI from the background after boot. Only the counting service can auto-start; the owner must tap the app or its notification to see the screen. Copy must never imply "the app opens itself".
- **No background location for a boot-resumed session.** A location FGS started while the app is in the background throws a `SecurityException` on Android 14+ because location is a while-in-use permission; booting does not give a visible activity. A GPS session cannot auto-resume at boot without `ACCESS_BACKGROUND_LOCATION`, which this product does not request and Play scrutinises. The legal recovery is a checkpoint plus a "session interrupted — resume" notification the user taps.
- **No counting from WorkManager/alarm.** A 15-minute deferred job is not a sensor; an alarm cannot start a counting FGS from the background without an exemption. This was already settled by the 2026-11-21 council and is not reopened.

### 2.3 Why the owner's phone may have failed

Ranked hypotheses, all consistent with the code:

1. OEM autostart is off for Sabeel, or the OEM battery manager killed the service after boot. Most likely on a real Xiaomi/Samsung/Huawei/Oppo-class phone; no public API.
2. The app was force-stopped (by the owner, by a cleaner, or by the installer), so `BOOT_COMPLETED` was never delivered.
3. The receiver fired but silently did nothing: `onboardingDone` read failed, or `ensureStarted()` returned false because `ACTIVITY_RECOGNITION`/sensor checks failed, or `startForegroundService` threw `ForegroundServiceStartNotAllowedException` — every outcome is swallowed today.
4. The service started but died immediately: the health `startForeground()` gate or a permission check failed, then `stopSelf()`.
5. `START_STICKY` was not honoured after an OEM kill.

The current code cannot distinguish 3/4 from 1/2. `BootReceiver` wraps `ensureStarted` in `runCatching` and ignores the boolean (`BootReceiver.kt:38-47`); `ensureStarted` returns false and only writes a coarse status (`StepCounterService.kt:871-888`); `StatusStore` has a heartbeat but no boot-attempt record (`StatusStore.kt:37-125`).

### 2.4 Health Connect while paused

Health Connect returns an **absolute** "steps since local midnight" total. The v1.2 pipeline applies it as a floor and never lowers the day total (`StepPipeline.kt:230-246`). If counting is paused at 10:00 and HC is read again at 18:00, the floor jumps by the steps walked while paused and silently backfills the paused interval. Therefore pause must also stop HC reads and, on resume, either apply an offset captured at pause time or suppress HC for the rest of the local day. This is a correctness requirement, not a nice-to-have.

### 2.5 Play submission

The app already declares `FOREGROUND_SERVICE_HEALTH` and `foregroundServiceType="health"`; the health FGS is the legitimate type for a step counter, but Play requires the foreground-service declaration for the type and a short demo video showing the feature and its notification. The safest configuration remains: one health FGS, persisted user intent, gated boot restart, visible pause, no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no background location, no Geofencing API, Health Connect read-only, and no INTERNET permission. Play's policy page is [Understand foreground service and full-screen intent requirements](https://support.google.com/googleplay/android-developer/answer/13392821); the exact declaration flow is verified at submission, not in code.

## 3. Code evidence (v1.2 tree at `5a44290`)

| Fact | Evidence | Consequence |
|---|---|---|
| Boot starts counting whenever onboarding is done; result is discarded | `platform/BootReceiver.kt:29-49` | Cannot tell "not attempted" from "refused" |
| `ensureStarted` swallows platform refusal and only writes NO_PERMISSION/NO_SENSOR | `service/StepCounterService.kt:871-888` | No boot-attempt diagnostics |
| No persisted counting-enabled intent | `data/prefs/SettingsStore.kt:32-69` | OFF cannot survive reboot; every re-arm path would override it |
| Status has COUNTING/NO_PERMISSION/NO_SENSOR/STOPPED only | `platform/StatusStore.kt:16` | User-off and system-killed are indistinguishable in UI |
| Home ring is not clickable; its semantics are progress-only | `ui/screens/HomeScreen.kt:232-255`, `ui/components/ProgressRing.kt:42-70` | Owner's requested gesture does not exist |
| Primary CTA is wired to `onStartWalk` | `ui/screens/HomeScreen.kt:324-327`; `ui/SabeelRoot.kt:163-177` | "Start walking" currently opens the walk screen, not a session builder |
| Walk record has no activity type, goal, places or status | `data/db/Entities.kt:22-33` | Custom sessions need schema v3 |
| Room is v2 with one explicit migration and no destructive fallback | `data/db/AppDatabase.kt:8-9`; `data/db/Migrations.kt:14-19`; `AppContainer.kt:25-27` | v3 migration is mandatory; a broken one crashes every v1.2 upgrade |
| Walk checkpoint JSON is unversioned | `service/WalkTracker.kt:20-22` | A v1.2 in-flight walk must survive the upgrade |
| Counter catch-up is accepted when timestamps are missing | `domain/StepPipeline.kt:130-169` | Re-enabling without an explicit rebase would add off-period steps |
| HC floor never lowers the day | `domain/StepPipeline.kt:230-246` | HC must be paused/offset, not left live |
| All-day health FGS with counter+detector | `service/StepCounterService.kt` (service description and registration) | Pausing must stop the service, not just hide the UI |

## 4. Interaction and prior-art evidence

- Platform step counting is normally always-on: Apple Health/Fitness, Samsung Health and Google Fit count continuously and their cards open detail; they do not use the hero card as an on/off switch. Where third-party pedometers offer an off switch, it is a labelled toggle/button with visible state, not an unlabelled progress ring.
- Session trackers use two information architectures: setup-first (Strava: choose sport, then record) and action-first (Nike Run Club: start, then choose a goal). Walkers default to action-first with remembered settings; a setup page is acceptable if it is one tap for "same as last time".
- The owner explicitly asked for a page, not a sheet. The plan keeps a dedicated setup screen with remembered defaults, and treats the market preference for a compact sheet as a later refinement rather than overriding the owner.
- The app has no map SDK, no network permission and a Canvas route sketch. Therefore "place" can only mean a locally saved coordinate with a label and radius; address search, geocoding, routing and ETA are out of scope for an offline app.

## 5. Evidence still needed from the owner's phone

Before the plan claims the reboot issue is fixed:

1. Phone manufacturer/model, Android version, OEM skin, and whether Sabeel is installed from the APK or a store.
2. After a reboot, was there a Sabeel notification? Was the phone unlocked? Was battery saver on? Is Sabeel's autostart allowed in the OEM settings?
3. `adb logcat -b all -d` filtered for `Sabeel`, `BOOT_COMPLETED`, `ForegroundServiceStartNotAllowedException`, `Background start not allowed`; `dumpsys activity services com.sabeel.steps`; `dumpsys package com.sabeel.steps | grep stopped`; `dumpsys deviceidle whitelist`.
4. Three consecutive cold boots with the same check, plus a 50–100 step walk after each boot to prove post-boot steps are counted.
5. If adb is not available: install a diagnostics build (planned Phase 1) whose Diagnostics screen records boot attempts and offers "share diagnostics" through the system share sheet (no INTERNET). The owner can send the text to the developer.

Until that evidence exists, the correct statement is: **the boot path is legal and instrumented after Phase 1, but the owner's specific failure has not yet been reproduced or explained.**

## 6. Design implications

1. Persist the user's counting intent; the service never self-enables.
2. Gate every entry point (boot, package replace, app open, notification action, ring, START_STICKY) on that intent.
3. Record boot/start attempts and refusal reasons; show them in Diagnostics and make them shareable.
4. On pause: flush, stop the writer/cadence, unregister sensors, stop HC, stop the FGS, and mark the day as "counting off".
5. On resume: rebase the counter baseline (no catch-up), reset detector/estimate accumulators, apply an HC offset or suppress HC for the day; never backfill.
6. Keep the global day total as the single authority; sessions are a projection, never a second daily counter.
7. Ring tap is the primary toggle, but it needs a visible state pill, a Settings mirror and an Undo snackbar; it must not be the only way to discover or change the state.
8. Custom sessions are sequential, one active at a time, with a saved setup and history; GPS is foreground-only; places are local coordinates, not searchable addresses.
9. GPS sessions cannot auto-resume at boot; checkpoint + notification + app-open resume is the legal path.
10. Be honest in UI, docs, Play listing and claims audit: "Sabeel asks Android to keep counting; Android or your phone maker can still stop it."
