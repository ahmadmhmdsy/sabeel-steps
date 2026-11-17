# Platform-reliability fix evidence — 2026-11-17

**Owner:** platform (WS1) · **Task:** shared `task-7` · **Plan:**
[docs/superpowers/plans/2026-11-17-production-readiness.md](../../superpowers/plans/2026-11-17-production-readiness.md)
Phase 2, Tasks 5–8.
**Scope:** step-counting foreground service, walk-tracking foreground service, service status,
notifications/reminders/boot re-arm, step cadence, walk checkpointing, manifest receiver flags,
platform sensor strings. **No build file, test file, database/repository, UI screen or legal/privacy
content was modified by WS1.** No Gradle task and no git command was run by WS1 (Lead owns builds and
commits).

Source evidence this work closes: [android-platform.md](android-platform.md) findings F1–F4, F7–F8
(F1 critical: health-FGS `SecurityException` on Android 14+; F2 no re-arm; F3 no status truth;
F4 silent notification denial + goal flag written before the post; F7 checkpoint race; F8 receiver
export/boot actions). Frozen decisions honoured: D5 (day-aware cadence), D7 (device locale,
localized + idempotent channels, no AAB split), D9 (health FGS only with its prerequisite, no
background starts, re-arm from allowed entry points, Home status), D10 (delete-all stops services
and suppresses writes), D11 (walk snapshots/checkpoints/resume).

## 1. Files changed / created

| File | Change |
|---|---|
| `app/src/main/java/com/sabeel/steps/platform/StatusStore.kt` | **New.** `enum class StepServiceStatus { COUNTING, NO_PERMISSION, NO_SENSOR, STOPPED }`; `object StatusStore { mark(context,status), markStopped(context), read(context): StepServiceStatus, clear(context) }` on SharedPreferences `sabeel_service_status` (`mode`, `last_alive_ms`, `STALE_AFTER_MS = 120_000`). A mark older than 120 s reads back as `STOPPED`, so a killed process cannot leave Home claiming "counting". |
| `app/src/main/java/com/sabeel/steps/platform/Permissions.kt` | Added `hasStepSensor(context)` via `PackageManager.FEATURE_SENSOR_STEP_COUNTER` (cheap pre-flight before starting the health FGS). Existing permission helpers unchanged. |
| `app/src/main/java/com/sabeel/steps/service/StepCounterService.kt` | Rewritten around the contract: `ensureStarted(context): Boolean`, `beginErase()`, `endErase()`, `stopForErase(context)`; old blocking `start()`/`stop()` removed. Pre-flight permission/feature gate in `ensureStarted`; hard gate in `onCreate` **before** any `startForeground`; `startForeground(type=health)` wrapped so a permission race degrades instead of crashing (F1). Sensor/feature failure marks `NO_SENSOR`; missing `ACTIVITY_RECOGNITION` marks `NO_PERMISSION`. Heartbeat (30 s) keeps `StatusStore` fresh; `onDestroy` marks `STOPPED` only when it had been counting and no erase is in progress. `flushPending` writes at a lifecycle boundary with a bounded `runBlocking + withTimeoutOrNull(800 ms)` and returns immediately while `beginErase` is set. Writer passes the day-aware cadence value and records through `StepsRepository.recordRawStep`. |
| `app/src/main/java/com/sabeel/steps/service/StepCadence.kt` | Added `activeMinutesFor(today)` (rolls the day over first, so a midnight tick never stamps yesterday's minutes on today), `finish(now, today)` and an in-progress-minute projection used on day rollover/teardown. Existing `seed / onEvent / activeMinutesToday` API and thresholds kept; `StepCadenceTest` semantics unchanged (traced, see §4). |
| `app/src/main/java/com/sabeel/steps/platform/Notifications.kt` | `ensureChannels(context)` is idempotent **and** localized: it re-creates all three channels from `AppLocale.wrap(context)` names/descriptions on every call, so a language change updates the system settings labels while the user's importance/sound choices are preserved. All notification copy is built from `AppLocale.wrap`. Goal notification is permission-gated and channel-gated, posts first and only then writes the once-per-day flag (F4). Reminder is permission-gated and returns `Boolean`. Added `resetPrefs(context)` for delete-all. **All posting goes through one helper, `fun post(context, id, notification): Boolean`**, which performs the inline `POST_NOTIFICATIONS` check used by lint and returns false when posting is not allowed; both services' `updateNotification()` and both user notifications use it, so there is exactly one `notify()` call site in the app. IDs 4101–4104 unchanged. |
| `app/src/main/java/com/sabeel/steps/platform/Reminders.kt` | Reminder worker posts through the localized, permission-gated `Notifications.notifyReminder`. `ReconcileWorker` **no longer starts `StepCounterService`** (background FGS start was illegal on API 31+); it only reconciles through the repository and catches up the goal notification. Boot-focused one-shot reconcile kept. Docs state the no-background-start rule. |
| `app/src/main/java/com/sabeel/steps/platform/BootReceiver.kt` | Accepts only `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` (both protected broadcasts **and** FGS-start exemptions); `QUICKBOOT_POWERON` dropped; calls `StepCounterService.ensureStarted` (never throws) and re-arms the reminder/reconcile chain with `goAsync()` + `finally finish()`. |
| `app/src/main/java/com/sabeel/steps/service/WalkTracker.kt` | Now owns the checkpoint file: `checkpointFile`, `hasCheckpoint`, `deleteCheckpoint`, `resetSession` (checkpoint + live state), plus an internal suppression flag set by `resetSession` and cleared by `allowCheckpoints` so delete-all cannot be undone by a service teardown flush. `Live`/`state`/`update`/`reset` unchanged for the Walk screen. |
| `app/src/main/java/com/sabeel/steps/service/WalkTrackingService.kt` | Route held as an immutable `@Volatile List<RoutePoint>` replaced on every accepted fix (a callback never mutates a list the checkpoint writer is iterating — F7). Stat scalars `@Volatile`; checkpoint JSON written atomically (tmp + rename) on `Dispatchers.Default` every 20 s; bounded teardown flush (`runBlocking + withTimeoutOrNull(1500)`) skipped while delete-all suppression is set. `start(context): Boolean` gates on location **or** `ACTIVITY_RECOGNITION`, chooses `location` / `health` / no type accordingly, wraps `startForegroundService`, never throws. `ACTION_START` (and a sticky restart with a null intent) resumes a pending checkpoint instead of discarding it; `ACTION_STOP` saves, `ACTION_STOP + discard` drops walk/checkpoint/state; `stop`, `discard`, `resetSession` exposed. |
| `app/src/main/AndroidManifest.xml` | BootReceiver: `android:exported="false"` with only `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` (F8). Service declarations unchanged: both `exported=false`, step service `foregroundServiceType="health"`, walk service `"location|health"`; FGS permissions were already declared. |
| `app/src/main/res/values-ar/strings_sensor.xml` | Terminology pass for platform copy: walk = مشية (`sensor_notif_walk_title`, `sensor_notif_walk_step_only`, `sensor_walk_save_failed`, no-sensor text). English strings unchanged — every referenced key already existed. Key count and names are identical between `values` and `values-ar` (22 each). |

## 2. Contract symbols delivered (frozen interface)

```kotlin
enum class StepServiceStatus { COUNTING, NO_PERMISSION, NO_SENSOR, STOPPED }
object StatusStore { fun mark(context, status); fun markStopped(context); fun read(context): StepServiceStatus; fun clear(context) }
object Permissions { fun hasStepSensor(context): Boolean }   // hasActivityRecognition/hasNotifications/hasLocation unchanged
object StepCounterService { fun ensureStarted(context): Boolean; fun beginErase(); fun endErase(); fun stopForErase(context) }
object WalkTracker { fun hasCheckpoint(context): Boolean; fun deleteCheckpoint(context); fun resetSession(context) }
object WalkTrackingService { fun start(context): Boolean; fun stop(context); fun discard(context); fun resetSession(context) }
object Notifications { fun ensureChannels(context); fun resetPrefs(context) }
class StepCadence { fun activeMinutesFor(today: String): Int; fun finish(nowMillis: Long, today: String): Int }
```

`StepCounterService.start(context)` is intentionally gone: every call site must use
`ensureStarted` (see §5). `WalkTrackingService.start` returns `Boolean` as required by the Walk
ViewModel; callers may ignore the value in Kotlin without an overload. Two non-frozen helpers were
added for the single guarded post path: `Notifications.post(context, id, notification): Boolean` and
`Notifications.notifyReminder(context): Boolean` (the latter already existed in the interface).

## 3. Behaviour and key decisions

**Health-FGS prerequisite (D9, F1).** Android 14+ throws `SecurityException` *after*
`startForeground(type=health)` on the service main thread, so a caller-side `try/catch` cannot save
the process. The fix is defence in depth: (1) `ensureStarted` checks `ACTIVITY_RECOGNITION` and the
step-counter feature and marks `StatusStore` before any start; (2) `onCreate` repeats the gate before
`startForeground`, marking + `stopSelf()` on failure; (3) the `startForeground` call itself is
wrapped for the API 34 permission-race case. A device without a step counter (e.g. the emulator)
therefore ends in `NO_SENSOR` with a Home status card, not a crash.

**No background service starts (D9, F2).** The only re-arm entry points are foreground: onboarding
finish, Home "start counting", app open, `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`. WorkManager jobs
(`ReconcileWorker`, `ReminderWorker`) never call `startForegroundService`; the reconcile job only
repairs the repository and goal notification. `QUICKBOOT_POWERON` is gone: not an Android broadcast
and has no FGS exemption.

**Status truth (D9, F3).** Writes happen on start, every heartbeat (30 s), and every successful write;
`read` downgrades marks older than 120 s to `STOPPED`. delete-all stops services while the erase flag
is set, so no teardown can repaint a "counting" state after the wipe.

**Notifications (D7, F4).** Channel names/descriptions and every notification body come from
`AppLocale.wrap`. `ensureChannels` is safe to call on every app open and after a language change; it
re-creates (not just adds) channels so labels follow the in-app language. The goal-once flag is
written **only** after `notify()` succeeded and **only** when `POST_NOTIFICATIONS` is granted; a
denied post leaves the day's congratulation available for later that day. A user-disabled channel is
treated as "not posted" rather than consuming the flag.

**Day-aware active minutes (D5).** `StepCadence.activeMinutesFor(today)` rolls over before reading, so
the midnight writer tick writes 0 for the new day; on rollover (and on teardown) the in-progress
minute is counted when its projected cadence reaches the threshold, then the ring resets. The writer
reads only through this method; the teardown flush uses `finish(now, today)`.

**Walk resilience (D11, F7).** The service main thread publishes a new immutable route list per fix;
the checkpoint job serializes a snapshot from `@Volatile` fields on `Dispatchers.Default` and writes
tmp+rename, so a reader and a writer can never share a mutable list. Checkpoints every 20 s plus a
bounded flush in `onDestroy` make process death lose at most the current moment, and a sticky restart
with a null intent restores the walk. A user "resume" (`ACTION_START` from the Walk screen) also
restores the pending checkpoint. With neither location nor `ACTIVITY_RECOGNITION` the service refuses
to start (`start` returns false) instead of starting an FGS with no legal type; with no location it
runs step-only and reports `gpsActive = false`.

**Delete-all ordering (D10).** `beginErase()` suppresses step-service writes; `stopForErase` and
`WalkTracker.resetSession` drop the writers/checkpoint; `Notifications.resetPrefs` and
`StatusStore.clear` erase platform prefs; `endErase()` re-enables writes. The walk service's
teardown flush is skipped while suppression is set, so the checkpoint cannot be resurrected after the
wipe; the next walk clears suppression when it begins.

## 4. Verification performed by WS1 (no Gradle, no git)

1. **API cross-check against the real repository/domain code** (not just the plan wording): all
   service calls use the contract surface `steps.todayFlow()`, `steps.recordRawStep(raw, now,
   activeMinutesToday)`, `steps.reconcile(activeMinutesToday)`, `settings.current()`,
   `settings.settingsFlow`, `walks.save(walk)`, and the current `Walk`/`RoutePoint`/`WalkTracker.Live`
   shapes. No DAO method is called directly, so the platform layer is decoupled from the Phase-1
   repository rewrite.
2. **`StepCadenceTest` traced by hand against the new implementation** — all five tests
   (`aFullMinuteOfCadenceCountsAsOneActiveMinute`, `aSlowMinuteIsNotActive`,
   `seedRestoresActiveMinutesAfterProcessDeath`, `counterResetNeverCountsBackwardsOrNegative`,
   `newDayResetsTheActiveMinuteCounter`) produce their expected values (0/0/1/1; 0/0; 7/7; 3/3/3;
   0/0). The test file was not modified.
3. **Delimiter-balance check** over every rewritten Kotlin file (strings/comments stripped):
   braces/parens/brackets balanced in all nine files.
4. **Resource manifests validated** as XML with a parser; `values` and `values-ar`
   `strings_sensor.xml` both parse, contain 22 strings and the same key set — and every
   `R.string.sensor_*`/notification key referenced by the platform code exists.
5. **Call-site grep**: no `StepCounterService.start(...)` remains in `app/src/main`; all
   `WalkTrackingService.start(...)` call sites consume the new `Boolean`; the Lead-owned ViewModels
   already call `ensureStarted`, `beginErase/endErase/stopForErase`, `StatusStore.clear`,
   `WalkTracker.hasCheckpoint/resetSession`, `Notifications.resetPrefs` (see §5).
6. Not verified by WS1: an actual compile, lint run, or device/emulator execution — those are the
   Lead's Task 17 build gates. Static review can miss a type error; the first `assembleDebug` is the
   real proof.
7. **Lint `MissingPermission` pass** (the Lead's `app/build/reports/lint-results-debug.txt` was read
   by WS1; WS1 ran no Gradle task). That report listed exactly five `MissingPermission` **errors** in
   WS1 files: `Notifications.kt:168` (goal post), `Notifications.kt:196` (reminder post),
   `StepCounterService.kt:313` and `WalkTrackingService.kt:456` (both `updateNotification()` posts)
   and `WalkTrackingService.kt:289` (`LocationManager.requestLocationUpdates`). All five are now
   guarded inline without weakening lint (no `lintOptions`, no baseline, no global suppression):
   every notification posts through `Notifications.post(...)`, which on API 33+ returns false unless
   `ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED` before
   calling `notify()` (on API < 33 the permission does not exist, so posting there is unchanged), and
   `startLocationUpdates` checks `ACCESS_FINE_LOCATION` with an inline `checkSelfPermission` directly
   around each `requestLocationUpdates` call. Lint's `PermissionDetector` accepts an enclosing method
   that contains any `check*Permission`/`enforce*Permission` call, so both sites satisfy it. The Lead
   re-runs `:app:lintDebug` as the gate.
8. Remaining lint findings in WS1 files are **warnings only**: `ImplicitSamInstance` on
   `stopService(Intent(...))` (`StepCounterService.kt:380`; known false positive for
   `Context.stopService`), `InlinedApi` on `FOREGROUND_SERVICE_TYPE_HEALTH` (guarded by
   `SDK_INT >= R`; the constant is compile-time inlined) and `ObsoleteSdkInt` on `SDK_INT < O`
   guards (minSdk 26). No error, no lint configuration change needed.

## 5. Wiring the Lead must keep (and what is already in the tree)

At the time of writing, the Lead-owned integration code already matches the delivered contract:

| Integration point | Expectation |
|---|---|
| `ui/viewmodel/AppViewModels.kt` | `StepCounterService.ensureStarted(app)` on app open/onboarding/home start; delete-all order `beginErase()` → stop walk service + `stopForErase` → clear DB/DataStore → `Notifications.resetPrefs` / `StatusStore.clear` / `AppLocale.clear` → `endErase()`; `WalkTrackingService.start(app)` result stored as `startBlocked`; `WalkTracker.hasCheckpoint` for the resume card; `Notifications.resetPrefs`, `StatusStore.clear`, `AppLocale.clear`. |
| `ui/SabeelRoot.kt` / app open | `Notifications.ensureChannels(...)` is called from SabeelRoot (wrapped context) — keep it cheap and idempotent; no new state needed. |
| `ui/screens/HomeScreen.kt` | Renders `HomeUiState.serviceStatus` (`STOPPED`/`NO_SENSOR` cards, `NO_PERMISSION` permission card) and starts counting through `HomeViewModel.startCounting()`. |
| Settings language change (ui WS2) | After `AppLocale.apply`, call `Notifications.ensureChannels(context)` so system-settings channel names follow the new language (channel recreation is free and idempotent). |
| WorkManager / boot | `ReconcileWorker` and `ReminderWorker` must never gain a `StepCounterService.start`/`ensureStarted` call: boot and `MY_PACKAGE_REPLACED` are the only background re-arm points. |
| Release/lint (WS3 + Lead Task 17) | Lint `MissingPermission` in WS1 files is resolved by the guarded post path and the inline location check (§4.7); lint configuration must stay untouched. A future new `notify()` call must go through `Notifications.post`, not the compat API directly. |

## 6. Residual risks / open items

1. **No build or device run by WS1.** The F1 fix, API 36 emulator behaviour ("no step sensor →
   `NO_SENSOR` card, no crash"), boot re-arm and walk resume are verified by review only. The Lead's
   build + emulator pass is the acceptance evidence.
2. **Walk "Stop" notification action** uses `PendingIntent.getForegroundService` to deliver
   `ACTION_STOP` to the already-running walk service. If a target device refuses background FGS
   delivery on Android 12+ while the app is backgrounded, the walk keeps running until the user
   returns to the app; a broadcast-stop fallback would be needed if that is observed. Not exercised
   here.
3. **API 29 (Q) walk service type**: no type is passed there (health type is R+ only) while the
   manifest declares `location|health`; the app targets 36 and the emulator/device matrix is API 36,
   so Q-specific enforcement is unverified.
4. **Checkpoint format is version 1 JSON** in private app storage. A corrupt/partial file is ignored
   (the walk offers a fresh start); no migration is needed because the file is ephemeral.
5. **Teardown flush bound**: `withTimeoutOrNull(1500)` cannot interrupt the synchronous JSON write
   itself; the bound is the route size (hundreds to low thousands of points). The 20 s checkpoint
   interval bounds worst-case loss.
6. **GPS/step heuristics** (accuracy filter 65 m, speed filter 12 m/s, 5 m minimum move) are
   deliberately conservative; real walking quality is only testable on a device.
7. **`LocationListener.onStatusChanged`** is deprecated but still part of the API 36 interface; the
   override must be dropped only if a future SDK removes it.
8. **`StatusStore` staleness (120 s)** means Home can show `STOPPED` up to two minutes after an OEM
   kill — intended, and the heartbeat is cheap.
9. **The emulator has no step counter**, so the "counting" path cannot be demonstrated there; the
   acceptance evidence for counting is a physical device. The emulator acceptance is the degraded
   `NO_SENSOR` path (no crash) plus walk tracking with mocked/simulated location.
10. **No tests were added by WS1** (test directory is Lead-owned); the pure cadence logic is covered by
    the Lead's existing `StepCadenceTest`.

## 7. Definition-of-done status for this workstream

| Plan gate (Tasks 5–8) | Status |
|---|---|
| StatusStore + stale detection, permission/feature gates, erase flag | Done (code + review) |
| `onCreate` guard before `startForeground` (F1 fix) | Done (code + review; emulator proof = Lead Task 17) |
| No background FGS starts; boot/update/foreground re-arm only | Done |
| Day-aware cadence, no midnight carry-over | Done; existing `StepCadenceTest` traced pass |
| Localized, idempotent channels; goal flag after successful post; gated reminders | Done |
| `ReconcileWorker` no longer starts the step service | Done |
| BootReceiver exported=false, BOOT_COMPLETED + MY_PACKAGE_REPLACED, no QUICKBOOT | Done |
| Immutable route snapshots + checkpoint flush + safe start gating | Done |
| Walk checkpoint/erase hooks for delete-all | Done (suppression flag closes the resurrection race) |
| Build/test/lint/emulator evidence | **Lead Task 17** — not run by WS1 |

**Do not report the platform workstream "verified" until:** `assembleDebug` + `testDebugUnitTest`
are green, lint reports no new platform error, and the API 36 emulator shows (a) no crash with
`ACTIVITY_RECOGNITION` denied, (b) the `NO_SENSOR` status card on the sensor-less image, and (c)
service re-arm after `adb shell am force-stop` + reboot/reinstall and a walk resume after process
death.

## 8. Commands for the Lead (build + smoke)

```powershell
.\gradlew.bat --console=plain :app:assembleDebug
.\gradlew.bat --console=plain :app:testDebugUnitTest
.\gradlew.bat --console=plain :app:lintDebug
```

Suggested emulator smoke (API 36, target 36):

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 1) Deny ACTIVITY_RECOGNITION (fresh install) -> onboarding finishes, Home shows NO_PERMISSION,
#    no crash, no SecurityException in logcat: adb logcat -d | Select-String SecurityException
adb shell pm revoke com.sabeel.steps android.permission.ACTIVITY_RECOGNITION
# 2) Sensor-less image -> Home shows NO_SENSOR card after start attempt; logcat has no crash.
# 3) Re-arm: adb shell am force-stop com.sabeel.steps; adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
#    (or reinstall with: adb install -r ... for MY_PACKAGE_REPLACED) -> status returns to COUNTING/NO_SENSOR.
# 4) Walk: start walk, simulate location (Extended controls > Location), swipe app away,
#    relaunch -> resume card appears and continuing the walk keeps prior distance/steps.
# 5) Language: switch to Arabic in Settings -> notification channel labels/notification text are
#    Arabic (adb shell dumpsys notification | Select-String Sabeel).
# 6) Delete-all -> no day rows, no walk checkpoint (adb shell run-as com.sabeel.steps ls files),
#    status STOPPED, goal/reminder prefs cleared.
```
