# Plan — counting control, restart hardening and custom sessions (v1.3)

Date: 2026-11-22 · Status: **proposed — awaiting owner decisions; no code implemented in this turn** · Owner: lead
Companions: `docs/review/2026-11-22/restart-toggle-sessions/RESEARCH.md`, `docs/review/2026-11-22/restart-toggle-sessions/COUNCIL.md`.
Supersedes the UI/recovery assumptions of `docs/superpowers/plans/2026-11-21-step-manifold-resilience.md` for the counting control and session feature; the v1.2 manifold, Health Connect floor and honesty rules stay in force.
This document is the updated plan requested by the owner. It records what was found, what the council ruled, what will be built, in what order, and what remains an owner decision.

## 1. Goal

Make the owner's three requests true without lying about Android:

1. **Counting follows the user's intent.** Once counting is on, Sabeel asks Android to keep it on across reboots, package updates, process death, app opens and notification taps — until the user turns it off. The app records every attempt and shows when Android or the OEM stopped it.
2. **The Home ring toggles counting.** One tap off, next tap on, with visible state, switch semantics, Undo and a Settings mirror. Off persists across reboot.
3. **The primary CTA opens a custom session builder**, not global counting. Walk or run, optional GPS, target by time/steps/distance or arrival at a saved place, saved sessions and history. Home always shows the global day total; the session screen shows session-only stats.

The user-visible promise is: **Sabeel keeps counting while it can, shows the truth when it cannot, and never invents missing steps.** It is not "nothing can ever stop it".

## 2. Non-goals / honest limits

- No restart after a user force-stop, an OEM cleaner, or an OEM autostart block before the user opens Sabeel. Android provides no public API for those.
- No background activity launch. Sabeel cannot open its own UI after boot; it can post a notification the user taps.
- No guarantee of per-step delivery while the AP is suspended. The cumulative counter heals the day total at wake; detector-only steps during suspend are lost (v1.2 rule, unchanged).
- No wakelock, no WorkManager counting, no exact-alarm self-restart, no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no `ACCESS_BACKGROUND_LOCATION`, no Geofencing API, no INTERNET.
- No auto-resume of a GPS session from boot: location is while-in-use; the legal path is a checkpoint plus a "session interrupted — resume" notification/app-open card.
- No address search, geocoding, routing, map tiles or ETA. A place is a locally saved label + coordinate + radius.
- No simultaneous sessions: one active session at a time; "multiple sessions" means saved setups and a history, not two concurrent counters.
- No session-only counting while global counting is off: that would make Home and the session disagree about the same walk.
- No direct-boot counting in v1.3 (`LOCKED_BOOT_COMPLETED`); it adds a second storage domain and a migration risk for a few seconds of pre-unlock counting. Revisit only with device evidence.
- No claim that the owner's reboot failure is fixed until the real-phone Step Zero in Phase 0 is captured and repeated after Phase 1.

## 3. Decisions locked by the council

| # | Decision | Why |
|---|---|---|
| D1 | `countingEnabled` lives in one synchronous SharedPreferences file (same pattern as `StatusStore`), mirrored to Compose by a `StateFlow`; the service never self-enables. | Boot receiver must read it without a cold DataStore read; one authority avoids divergence. |
| D2 | The flag is written before any start/stop action; every entry point re-checks it; `START_STICKY` null-intent restart exits when the flag is false. | A reboot or sticky restart must not override an OFF decision. |
| D3 | Pause = flush + stop cadence + unregister counter/detector/estimate + stop HC + set `counterLastRaw = NaN` + stop FGS. | Keeping sensors alive while off wastes battery and would leak off-period steps. |
| D4 | Resume uses a new explicit rebase entry (baseline = current raw, delta 0), resets detector/estimate accumulators, and offsets or suppresses HC for the day. | Reusing `onCounter` would accept the whole paused gap as catch-up. |
| D5 | Home ring is a real switch (`toggleable`, `Role.Switch`, `stateDescription`, `onClickLabel`) with a visible pill, a Settings switch, haptic and 5 s Undo. | The owner gets the gesture; accessibility and discoverability are not sacrificed. |
| D6 | Distinct states: `ON`, `OFF_BY_USER`, `STOPPED_BY_SYSTEM`, `NO_PERMISSION`, `NO_SENSOR`; copy never says "failed" for a deliberate off. | The current `STOPPED` conflates user intent, crash and permission. |
| D7 | Custom sessions are a projection of the global day delta; session code never writes `DailyStepsDao`; `session.steps ≤ daily.steps` per date. | One physical walk must not be counted twice. |
| D8 | One active session; starting while global counting is off prompts to turn counting on; turning global off during a session pauses the session with a warning. | Home and the session must agree. |
| D9 | Room v3 with an explicit `MIGRATION_2_3`; no destructive fallback; walk checkpoint JSON gets a version and a tolerant v1 parse. | A missing migration would crash every v1.2 install. |
| D10 | Places are local label + coordinate + radius; "end at place" auto-stops on arrival; no search or routing. | The app has no INTERNET and no map SDK. |
| D11 | GPS session interruption = checkpoint + notification/app-open resume; no boot-start of a location FGS. | Android 14+ while-in-use rule. |
| D12 | Honest copy EN/AR and Play listing; no absolute claims; no battery-optimization exemption; HC read-only; foreground-only location. | Product truth and Play compliance. |
| D13 | User-off day is labelled "counting was off", excluded from goal-hit rate/median and streak advancement (rest-like but not silently a rest day); system-stopped day is "no data". Owner decision; default recommended. | The owner must choose how strict the streak is. |
| D14 | versionCode 3 / versionName 1.3.0; staged rollout; real-phone evidence before the reboot claim. | Migration + release discipline. |

## 4. Phase 0 — Step Zero on the owner's phone (before claiming anything)

Goal: capture **why** v1.2 failed on the real phone, not just add another start attempt.

- Ask the owner for: manufacturer/model, Android version, OEM skin, install source, whether a Sabeel notification appeared after reboot, whether the phone was unlocked, battery-saver state, and the OEM autostart state for Sabeel.
- If adb is available: `adb logcat -b all -d` filtered for `Sabeel`, `BOOT_COMPLETED`, `ForegroundServiceStartNotAllowedException`, `Background start not allowed`; `dumpsys activity services com.sabeel.steps`; `dumpsys package com.sabeel.steps | grep stopped`; `dumpsys deviceidle whitelist`; three cold boots with a 50–100 step walk after each.
- If adb is not available: proceed with Phase 1, which adds the Diagnostics report and a share button; ask the owner to install the Phase 1 build, reproduce, and send the report. Do not call the reboot issue fixed before that.
- Record the result in `docs/legal/release-evidence-log.md` and, if it is a code bug, add a regression test before the fix.

**Exit criteria:** either a captured log/evidence trail that identifies the failure class, or a Phase 1 diagnostics build in the owner's hands with a reproduction request. No claim of a fix yet.

## 5. Phase 1 — Counting authority, pause/resume, boot hardening, ring toggle (fixes #1 and #2)

Ship this phase first (v1.3.0-alpha1) so the owner gets the control and the evidence before the session feature lands.

### 5.1 Counting authority

- New `platform/CountingStore.kt`: synchronous SharedPreferences (`sabeel_counting`), keys `enabled` (default false until onboarding completes, then true), `changed_at`, `paused_at`, `pause_reason`; `StateFlow<CountingState>` via a listener; `Volatile` cached read for hot paths.
- New `service/CountingController.kt`: serialized (Mutex) `setEnabled(Boolean, reason)`, `toggle()`, `syncOnAppOpen()`, `onBoot()`; persists first, then acts; idempotent; never throws.
- Onboarding completion sets `enabled = true` explicitly; existing installs with no key default to true only after `onboardingDone` (the update must not turn a deliberate future OFF back on).
- Delete-all blocks toggles while `StepCounterService.beginErase()` is active.

### 5.2 Domain: pause and resume without backfill

- `StepState` gains `hcOffsetToday: Int = 0` and `hcSuppressedForDay: Boolean = false`; `SettingsStore` schema 3 with migration from schema 2 (old state -> offset 0, not suppressed).
- `StepPipeline.pause(state, now)`: set `counterLastRaw = Float.NaN`, `counterLastEventNanos = 0`, `detectorLastEventNanos = 0`; keep totals; keep `estimateToday` as historical evidence but stop the watchdog; do not lower `total`.
- `StepPipeline.resume(state, raw, today, now)`: baseline = raw with no addition; reset detector/estimate accumulators in the service; restore HC with `hcOffsetToday = max(0, lastHcToday)` or `hcSuppressedForDay = true` when no HC baseline exists.
- `StepPipeline.onExternalTotal(total, offset, suppressed)`: apply `max(0, total - offset)` only when not suppressed; reset offset/suppression on day rollover; never lower the day.
- Service writer skips while paused (same suppression pattern as erase); `flush` on pause; `StepCadence` stopped and restarted on resume so paused time is not active time.
- Midnight: flush/roll over the old date before pausing; on resume after midnight, `reconcile(today)` rolls to the new day with zero and the old row stays frozen.

### 5.3 Service lifecycle

- `StepCounterService.ensureStarted` returns a result enum (`STARTED`, `NO_PERMISSION`, `NO_SENSOR`, `REFUSED(exceptionClass)`) and records it; never swallows a refusal silently.
- `onCreate`/`onStartCommand`/`START_STICKY` re-check `CountingStore.enabled`; if false, `stopSelf()` before `startForeground`; if true but permission missing, mark `NO_PERMISSION` and stop.
- `ACTION_STOP` for the notification/ring: flush, pause pipeline, mark `OFF_BY_USER`, stop foreground and `stopSelf`.
- `ACTION_START`: re-check the flag and permission, resume pipeline, start foreground.
- `onTaskRemoved`: flush as today; do not stop the FGS (unless OEM kills it).
- `onDestroy`: mark `STOPPED` only when the stop was not a user-off or erase; user-off remains `OFF_BY_USER`.
- `runningInstance` stays for UI; add `lastHeartbeatAt` to StatusStore (already there) and expose `STOPPED_BY_SYSTEM` when `enabled && permission && heartbeat stale`.

### 5.4 Boot, package replace and recovery

- `BootReceiver`: read `CountingStore` synchronously; if `onboardingDone && enabled`, record a boot attempt (time, action, boot count, AR grant, sensors, HC availability, enabled flag) **before** the start; call `CountingController.onBoot()`; record result/exception; reschedule reminders/reconcile; keep `exported=false`; keep `goAsync()` short.
- One-time `BootHealthWorker` (WorkManager, ~1 min delay, no FGS start) checks the heartbeat and `enabled`; if the service is not alive, writes the failure and posts the recovery notification "Counting paused — tap to restart" (activity PendingIntent, no sound). At most one per boot.
- Recovery notification channel and strings EN/AR; the notification tap opens Home/Diagnostics and offers "Start counting" — a user action, so the FGS start is legal.
- App open: `SabeelRoot` calls `CountingController.syncOnAppOpen()` (start if enabled, stop if disabled, flush if running); Home shows a banner when the last heartbeat is stale and enabled.
- OEM guidance: extend `OemBatteryGuide` with the exact autostart/battery paths for the detected manufacturer; link to app details, battery settings and the OEM guide; never request a restricted exemption.
- No `LOCKED_BOOT_COMPLETED`, no `QUICKBOOT_POWERON`.

### 5.5 Diagnostics (the evidence instrument)

Extend `StatusStore` + `DiagnosticsScreen` with:

- Counting state and last change (on/off/system-stopped), last boot attempt and result (ring buffer of 10: time, action, boot count, onboarding, AR, sensors, HC, enabled, start result, exception class, alive-after-10 s).
- Last heartbeat/source/refusal, storage fault, notification permission, battery-optimization state, OEM autostart checklist, active session state.
- Buttons: "Restart counting" (user action), "Open battery settings", "Open app settings", "Share diagnostics" via `ACTION_SEND` text (no INTERNET; redact coordinates and place labels).
- A short "what can stop counting" explanation linking to the OEM guide.

### 5.6 Home ring toggle and off-state UI

- `ProgressRing` gains an optional toggle contract: `Modifier.toggleable(value = countingEnabled, role = Role.Switch, onValueChange = ...)`, `stateDescription` ("Counting on"/"Counting off"), `onClickLabel` ("Stop counting"/"Start counting"); the existing content description still reports steps; do not put `clearAndSetSemantics` on the clickable node.
- Home: ring tap -> `HomeViewModel.toggleCounting()`; haptic `CONFIRM`; 5 s Undo snackbar with `liveRegion`; no confirm dialog. Visible pill under the ring: "Counting on · stop" / "Counting off · start"; tappable. Settings gets the same switch.
- Off visuals: pause glyph at the top of the ring, muted/dashed track, frozen number, caption "Today's total — paused", status line "Counting is off — today's steps are not being counted". No red/error styling; this is a user choice, not a failure.
- System-stopped visuals: "Android stopped counting — tap to restart" with the last alive time; a "start" action; not "you stopped it".
- Permission/sensor states keep the existing cards; the ring state is `NO_PERMISSION`/ON but not counting.
- The primary CTA is renamed to **"Start a session"** / **"ابدأ جلسة"**; it navigates to the session setup screen (Phase 2). Until Phase 2 ships, it opens the existing Walk screen but the label must not imply counting starts.
- Strings EN/AR for every state; Arabic 6-form plurals for minutes/steps; standardise "عدّاد" vs "العدّ" (one term for the counting feature, one for the number).

### 5.7 Phase 1 tests

- JVM: `CountingStore` default/migration/persistence; `CountingController` toggle idempotency and double-tap serialization; `StepPipeline` pause -> resume with 0 backfill; HC offset/suppression; midnight pause/resume; detector/estimate reset; `START_STICKY` decision table.
- Compose: ring has `Role.Switch`, state description, click toggles, Undo restores; off-state copy; TalkBack labels; RTL layout; fontScale 2.0.
- Emulator: toggle off -> walk -> on (no backfill), reboot with on/off, force-stop documents the blocked state, recovery notification, Diagnostics report.
- Build: `gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`.

**Phase 1 acceptance:** owner can stop/start counting from the ring and Settings; OFF survives reboot; ON re-arms on boot/app open and Diagnostics shows the attempt; 500 paused steps do not appear on resume; no crash when the service is refused.

## 6. Phase 2 — Custom sessions (fixes #3)

### 6.1 Data model (Room v3)

- Extend `walks` with `activityType TEXT NOT NULL DEFAULT 'WALK'`, `goalType TEXT NOT NULL DEFAULT 'NONE'`, `goalValue REAL NOT NULL DEFAULT 0`, `targetMet INTEGER NOT NULL DEFAULT 0`, `status TEXT NOT NULL DEFAULT 'COMPLETED'`, `gpsUsed INTEGER NOT NULL DEFAULT 0`, `startPlaceId INTEGER`, `endPlaceId INTEGER`, `sessionPresetId INTEGER`.
- New tables:
  - `places` (`id`, `label`, `lat`, `lon`, `radiusM`, `createdAt`).
  - `session_presets` (`id`, `name`, `activityType`, `goalType`, `goalValue`, `useGps`, `startPlaceId`, `endPlaceId`, `lastUsedAt`).
  - `walk_legs` (`id`, `walkId`, `date`, `steps`, `distanceMeters`, `durationSec`) for midnight splits.
- `MIGRATION_2_3` with defaults and indices; `AppDatabase` version 3; export schema JSON; `MigrationTestHelper` test v1->v2->v3 and v2->v3 with real rows.
- Existing v1.2 walk rows become `WALK`/`NONE`/`COMPLETED`, no route loss.

### 6.2 Session domain

- New `domain/SessionGoal.kt` (pure): goal types `NONE`, `TIME`, `STEPS`, `DISTANCE`, `PLACE`; `GoalProgress` (current, target, reached, label); goal reached exactly once; no float drift.
- New `domain/SessionProjection.kt` (pure): session steps = `max(0, globalDayTotal - baseline)` for the current date; midnight roll captures the previous leg and resets the baseline; `session.steps ≤ daily.steps` invariant; returns legs for persistence.
- Session step source = the global manifold total, not the walk service's own sensor. `WalkTrackingService` stops owning a second step counter; it observes `StepsRepository.todayFlow` (or a bound snapshot) and updates the session projection. If the global service is not running, the session pauses with "counting interrupted" and tries to re-arm; no session-only fallback.
- GPS: foreground-only; distance from haversine; route points as today; no map tiles; route sketch on Canvas.
- Goal engine ticks every second; `PLACE`/end-place arrival uses the saved radius; auto-stop on arrival; numeric goals notify once and continue unless "stop at goal" is enabled (default off; owner decision).
- Checkpoint v2 JSON: `version: 2`, activity/goal/place/status/legs/baseline; tolerant parse of v1.2 `walk_checkpoint.json` as WALK/NONE/COMPLETED; checkpoint every 20 s; flush on pause/stop.
- On process death/reboot: checkpoint survives; app open shows "session interrupted — resume/discard"; notification "session interrupted" (no auto-start of a location FGS from boot). Step-only session could use health FGS, but v1.3 keeps one path for simplicity.

### 6.3 Session service and notification

- Reuse `WalkTrackingService` as the session FGS (type `location|health`), with actions `START`, `PAUSE`, `RESUME`, `STOP`, `DISCARD`; notification shows time/steps/distance and Pause/Stop; `START_STICKY` resumes from checkpoint as today.
- Start from a visible activity only (permission/while-in-use); if the app is backgrounded, the existing FGS continues; no boot start.
- Session pause = stop GPS updates (optional) and freeze the projection; global counting continues (unless the user turns it off, which pauses the session with a warning).
- Save: one transaction inserts the walk + legs; no daily row writes; `targetMet`; estimated marker if the day has estimated steps and the session's delta includes them; notification "Session saved".

### 6.4 Session UI

- Routes: `SESSION_SETUP = "session/setup"`, `SESSION = "session"` (replaces the direct Walk route; keep a redirect/alias for the old route if needed), `SESSION_DETAIL = "session/{id}"` if the existing History detail is not reusable.
- Setup screen: activity segmented [Walk][Run]; GPS switch "Use location (optional)" with inline permission and battery note; goal segmented [None][Time][Steps][Distance][Place] (Place only when a saved place exists); value stepper with sensible presets (5/10/15/30/60 min; 1k/2k/5k/10k steps; 1/2/5/10 km); start/end place pickers (Phase 3); saved presets chips ("same as last time" default); bottom-anchored 56 dp primary "Start session"; cancel/back preserves draft.
- Live screen: session ring with progress to goal (or timer if no goal), big timer, stats time/steps/distance/pace, chips "This session", pause/resume, finish; back opens a save/discard sheet (today's raw pop is a UX bug); keep the screen awake only while visible (no wakelock).
- Summary: saved confirmation, goal met/missed, stats, "Done"; session detail reuses the existing walk detail; History gets filter chips [Days][Sessions].
- Home: global ring caption "Today's total"; active session chip "Session running 12:34" under the header, tappable to return; CTA "Start a session"; never show session numbers in the Home ring.
- Accessibility/RTL: segmented controls are radio groups with labels; steppers have content descriptions; Arabic plurals for minutes/steps; no color-only goal state; one-handed bottom controls; fontScale 2.0 layout test.
- Strings EN/AR (~40 new); rename `home_start_walk` to a session CTA; keep the old key as an alias only if needed for tests.

### 6.5 Phase 2 tests

- JVM: goal engine boundaries; projection/midnight legs; `session ≤ day`; checkpoint v1/v2 parse; session save transaction; estimated marker; goal reached once; pause/resume; place radius.
- Room: migration v2->v3 with rows; fresh v3; indices; delete-all covers places/presets/legs.
- Compose: setup validation, start -> live, pause/resume, finish -> save, back sheet, History filter, RTL/fontScale, TalkBack labels.
- Emulator: session with/without GPS, process kill, reboot mid-session, global off during session, goal reached, midnight crossing (clock injection if possible).
- Build: unit + debug + lint; release `assembleRelease bundleRelease` before sign-off.

**Phase 2 acceptance:** owner can build a walk/run session, start it, see session-only stats while Home keeps the global total, pause/finish, see it in History; no double count; process death resumes from checkpoint; no crash on migration.

## 7. Phase 3 — Places, presets, export and polish

- Places screen: save current location as a label (Home/Work/other), edit label/radius, delete; pick start/end place; no address search, no map tiles; show a Canvas marker.
- End-place auto-stop: when within radius, stop the session, mark target met, notify once; if GPS is lost, show "waiting for GPS" and do not guess.
- Saved presets: save the current setup as a named preset; "same as last time" default; delete/rename; presets are local only.
- Sessions CSV export (opt-in, separate from the daily CSV) with type/goal/result/places (labels only, no coordinates by default; a "include coordinates" toggle if the owner asks); daily CSV gets an optional trailing `counting_off` column with a tolerant importer (old files still import).
- History detail: activity/goal/places/legs; delete-all covers sessions, legs, places, presets; Settings "Your data" lists them.
- Polish: haptics, empty states, error states, notification copy, widget? (no widget in v1.3).

## 8. Phase 4 — Docs, Play, verification, release

### 8.1 Documentation

- `docs/product/spec.md`: new §3 rules for counting intent/pause/resume/off-state; session model; places; streak semantics; CSV scope; honest limits.
- `README.md`: counting control, sessions, offline places, reboot limits; no absolute claim.
- `docs/release/CLAIMS-AUDIT.md`: new section with approved EN/AR copy and rejected claims ("always", "guaranteed", "auto-opens", "never fails").
- `docs/legal/privacy-policy.md`: places/routes/sessions are local; CSV export scope; HC read-only; no upload.
- `docs/legal/HARDWARE-QA-CHECKLIST.md`: reboot ON/OFF, OEM autostart off/on, force-stop, session resume, migration; rows with pass/fail.
- `docs/legal/release-evidence-log.md`: Step Zero evidence, device model, logs, 3 cold boots.
- `docs/legal/play-compliance-checklist.md`: FGS health declaration + demo video, location foreground-only, HC declaration, Data safety.
- `docs/release/PLAY-SUBMISSION-CHECKLIST.md`: version, artifacts, declarations.
- `docs/legal/BLOCKERS.md`: update physical Step Zero/HC device rows; add owner-phone evidence gate.
- Old v1.2 plan: add a status note that the counting-control/session parts are superseded by this plan.

### 8.2 Verification matrix

| Layer | What | Evidence |
|---|---|---|
| JVM | CountingStore/controller, pipeline pause/resume, HC offset, goal engine, projection, checkpoint v1/v2, CSV | test output, coverage notes |
| Room | v2->v3 migration with real rows; fresh v3 | MigrationTestHelper output |
| Compose | ring semantics, toggle, Undo, off-state, setup/live/history, RTL/fontScale | test output/screenshots |
| Emulator | reboot ON/OFF, force-stop, toggle no-backfill, session kill/reboot, goal | commands + dumps |
| Real phone | Step Zero before/after, 3 cold boots, OEM autostart off/on, 500-step pause, session resume, migration from v1.2 | Diagnostics report + release evidence log |
| Build | debug unit/assemble/lint; release assemble/bundle | Gradle output tail |
| Artifacts | APK/AAB sizes, SHA-256, versionCode 3/1.3.0, upload cert | `dist/` + checksums |

### 8.3 Release gate

- All Phase 1/2 acceptance criteria green; Phase 3 optional but planned.
- Migration test from a real v1.2 install green; no destructive fallback.
- Diagnostics evidence from the owner's phone (or an explicit, documented "not yet reproduced" blocker).
- Claims audit, privacy policy, Play declarations and hardware QA updated.
- Staged rollout 10% -> 50% -> 100%; no downgrade path (allowBackup=false, CSV daily + sessions export is the backup).

## 9. File-level change map

| Area | Files |
|---|---|
| Counting authority | new `platform/CountingStore.kt`, new `service/CountingController.kt`; `platform/StatusStore.kt` |
| Boot/recovery | `platform/BootReceiver.kt`, new `platform/BootHealthWorker.kt`, `service/StepCounterService.kt`, `ui/SabeelRoot.kt` |
| Domain | `domain/StepPipeline.kt`, `domain/StepState.kt` (if separate), new `domain/SessionGoal.kt`, new `domain/SessionProjection.kt`, `domain/Walk.kt` |
| Persistence | `data/prefs/SettingsStore.kt` (schema 3, offset/suppression), `data/db/Entities.kt`, `data/db/Daos.kt`, `data/db/AppDatabase.kt`, `data/db/Migrations.kt`, `data/repo/WalkRepository.kt`, `AppContainer.kt` |
| Session service | `service/WalkTrackingService.kt`, `service/WalkTracker.kt`, `platform/Notifications.kt` |
| UI | `ui/screens/HomeScreen.kt`, `ui/components/ProgressRing.kt`, `ui/components/CountingTogglePill.kt` (new), `ui/screens/SessionSetupScreen.kt` (new), `ui/screens/WalkScreen.kt` (live), `ui/screens/HistoryScreen.kt` (filter), `ui/screens/SettingsScreen.kt`, `ui/screens/DiagnosticsScreen.kt`, `ui/SabeelRoot.kt`, `ui/viewmodel/AppViewModels.kt` |
| Export | `data/export/CsvCodec.kt`, `data/export/CsvIo.kt`, new sessions export |
| Resources | `res/values/strings.xml`, `res/values-ar/strings.xml`, notification icons/channels |
| Docs | spec, README, claims audit, privacy, hardware QA, release evidence, Play checklist, blockers, old plan note, this review folder |
| Tests | `app/src/test/...` new unit suites; `app/src/androidTest/...` migration/Compose; update affected tests |

## 10. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| OEM autostart/battery killer still stops counting | Owner sees another failure | Diagnostics + OEM guide + recovery notification + honest copy; cannot be fixed in-app; capture evidence |
| Room v3 migration breaks an upgrade | Crash/data loss for every v1.2 user | Explicit migration + MigrationTestHelper + real-device upgrade test; no destructive fallback |
| Pause/resume leaks off-period steps | Fabricated day total | NaN baseline + explicit rebase + HC offset/suppression + unit/emulator tests |
| HC offset edge cases (grant after pause, midnight) | Wrong floor | Offset only when a same-day baseline exists; otherwise suppress to midnight; tests |
| Ring toggle accidental multi-day off | Lost steps | Visible pill, haptic, 5 s Undo, Settings mirror, system-stopped detection |
| Session double count / session > day | Broken trust | Projection from global delta, session ≤ day invariant, never write daily |
| GPS session cannot auto-resume after reboot | User expects continuity | Checkpoint + notification + app-open resume; copy states it |
| Places scope creep / network temptation | Breaks offline promise | Local coordinate + radius only; no geocoding/routing; Phase 3 |
| Play rejection (FGS/location/HC) | Release blocked | Declarations, demo video, foreground-only location, no battery exemption, read-only HC |
| Owner's phone failure not reproduced | Fix unproven | Phase 0 + diagnostics build; do not claim fixed without evidence |
| Data loss on uninstall/backup off | User loses history | CSV daily + sessions export; documented |
| Scope too large for one release | Delay | Phase 1 first (control/boot/diagnostics), Phase 2 sessions, Phase 3 places/polish |

## 11. Open owner decisions

1. Phone model/OEM/Android version; is adb available for the Step Zero log?
2. Streak rule for a user-off day: rest-like exclusion (recommended) vs break the streak.
3. Starting a session while counting is off: auto-enable on confirm (recommended) vs block.
4. Places: saved coordinate + radius (recommended, offline) vs address search (requires network/map; conflicts with the no-INTERNET promise).
5. Goal completion: notify only (recommended) vs auto-stop.
6. One active session at a time (recommended) vs queued/simultaneous (simultaneous cannot be measured separately).
7. Turning global counting off during a session: pause the session (recommended) vs stop it vs keep it.
8. CTA naming: "Start a session" / "ابدأ جلسة" (recommended) vs keep "Start walking".
9. Ship Phase 1 (control/boot/diagnostics) first as a small update, or hold everything for one v1.3 release?
10. Version label: v1.3.0 / versionCode 3 (recommended) vs another number.

## 12. Definition of done

- Counting follows the persisted intent across boot, package replace, app open, sticky restart and notification tap; OFF survives reboot; system stops are visible and attributable.
- The ring toggles with switch semantics, a visible pill, Settings mirror and Undo; no accidental silent off.
- Pause contributes exactly zero steps on resume; HC cannot backfill a paused interval; midnight is safe.
- Custom sessions: walk/run, optional GPS, time/steps/distance/place goals, saved presets, one active session, history; session ≤ day; no double count; resume after process death; no auto-resume of GPS at boot.
- Places are local coordinates with labels/radii; no network.
- Diagnostics explains the last boot attempt and offers a shareable report.
- Migration from v1.2 is tested on a real install; no data loss.
- Docs, claims, privacy, Play declarations and QA evidence are updated; versionCode 3/1.3.0 artifacts built and signed.
- The owner's real-phone evidence is captured; if a failure remains, it is documented as a blocker, not hidden.

## 13. Status log

- 2026-11-22: research + council complete; this plan proposed; no code changed. Awaiting owner answers to §11 and Phase 0 evidence.
