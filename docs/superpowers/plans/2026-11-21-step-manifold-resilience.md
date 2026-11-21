# Plan — step manifold resilience (v1.2)

Date: 2026-11-21 · Status: implementation + emulator verification complete; physical Step Zero pending · Owner: lead
Companions: `docs/review/2026-11-21/step-resilience/RESEARCH.md`, `docs/review/2026-11-21/step-resilience/COUNCIL.md`.
Supersedes the source-selection part of `docs/superpowers/plans/2026-11-19-step-source-resilience.md`; the earlier plan's HC floor, health-FGS and watchdog work stays.

## Goal

Replace the exclusive source ladder with a hot-standby manifold so that the day's total is never lost or double counted when any of these happen: counter stalls/resets/reboots, detector is the only live sensor, process death, FGS refusal, Health Connect grant after start, HC hung read, midnight/timezone change, storage hiccup. History and Diagnostics must show provenance and staleness instead of claiming success.

## Non-goals (honest limits)

- No wakelock and no promise of per-step delivery while the AP is suspended. The cumulative counter heals the total at wake; detector events can be lost.
- No attempt to restart after a force-stop/OEM clean; only boot, package replace, app open and a user tap are legal.
- No new network/cloud anything; HC stays read-only and optional.
- Active-minute attribution across a dead-service gap is not reconstructed (no timestamps to attribute).

## Work breakdown

### 1. Domain (pure, TDD)
- `domain/StepPipeline.kt` rewritten as the **manifold** state machine. New `StepState(day, total, confirmed, counterToday, counterLastRaw, counterLastEventNanos, detectorToday, hcToday, estimateToday, initialized, updatedAt)`.
  - `onCounter(raw, eventNanos, today)`: negative delta ≤ −1 = reboot/reset, rebase without adding; monotonic event-timestamp guard; plausibility clamp 6 steps/s + 5 grace using sensor time; catch-up after silence always counted.
  - `onDetector(steps, today)`: each event adds 1, values outside 1..10 rejected, ≤5 events/s.
  - `onEstimate(steps, today)`: baseline = current total on first event, only grows.
  - `onExternalTotal(total, today)`: HC floor, never lowers.
  - `rollover(today)`: resets ledgers, drops the unattributable cross-midnight delta.
  - `fuse`: confirmed = max(confirmed, counterToday, detectorToday, hcToday); total = max(total, confirmed, estimateToday); estimated = total − confirmed.
- `domain/SilenceWatchdog.kt` (new): pure state machine for "both measured sources silent → bounded probe → walking? → estimate". Cannot self-disable: in-flight is set only when a probe is actually scheduled and is cleared on every no-op. Detector events feed `onMeasuredEvent`.
- Delete `StepSourcePolicy`/its tests only if no longer referenced; keep `StepSource` enum and `SourceAvailability` for display/status. `StepSourceHealth` is replaced by `SilenceWatchdog` (remove the old class and its tests).
- Rewrite `StepPipelineTest` (new invariants + old cases that still hold) and add `SilenceWatchdogTest`.

### 2. Persistence
- `SettingsStore`: persist the full manifold state (schema version, day, total, confirmed, counterToday, counterLastRaw, counterLastEventNanos, detectorToday, hcToday, estimateToday, initialized, updatedAt) plus the **estimated marker fix**; migrate the legacy `step_baseline_*` keys on first read; keep the old keys until migration has shipped.
- `StepsRepository`: replace `recordRawStep`/`recordExternalTotal` with one `record(Snapshot)` that applies counter/detector/estimate/HC in one DataStore write and one row upsert; `reconcile()` rolls the day, resolves max(state, row), seeds from CSV; keep the marker rule from existing rows for restored days.
- Persistence fault counter in `StatusStore` (visible in Diagnostics) instead of silent `continue`.

### 3. Service
- `StepCounterService` rewritten around the manifold:
  - register counter and detector for the whole day, never unregister them except service teardown; retry failed registrations every 30 s with backoff; `flush()` on app-open intent;
  - coalescing writer every 1 s sends one snapshot; after commit feed `StepCadence` from the fused row total (no more per-source rebase);
  - HC poll: refresh readiness in `onStartCommand` and on every heartbeat; poll when `hasPermission`; 180 s / 60 s when measured silent; 8 s timeout; apply floor; never lower;
  - estimate: continuous only when no counter and no detector; otherwise only after `SilenceWatchdog` walking probes; stop on any measured event;
  - watchdog heartbeat 15 s with exponential probe backoff; detector events are liveness;
  - crash containment: `runCatching` (catch `Exception`) around every callback/lifecycle/poll/notification; a failed write sets the storage fault; bounded flush only in `onDestroy`;
  - status source = contributor of the max evidence with last-event age, switches, estimate marker; notification text follows provenance.
- `StepCadence`: make it writer-confined (single coroutine) or `@Synchronized`; feed cumulative fused totals; drop `rebase()`.

### 4. Recovery
- `ReconcileWorker`: refresh HC read when the grant/feature exists (works even if the FGS cannot start, e.g. no motion permission); if the service heartbeat is stale and counting is enabled and permissions allow, post at most one localized "counting paused — tap to resume" notification per 12 h (permission-gated, no sound). Never start an FGS from the worker.
- `BootReceiver`: same re-arm; keep BOOT_COMPLETED + MY_PACKAGE_REPLACED.
- `ensureStarted`: distinguish "refused by platform" from "no permission"; never throw; add `ACTION_FLUSH` on app resume.
- `Notifications`: recovery channel/notification + strings (EN/AR); no new permission beyond `POST_NOTIFICATIONS` (already optional).

### 5. Health Connect
- Manifest: add `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`.
- `HealthConnect`: `requiredPermissions(context)` includes the background permission only when `HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND == FEATURE_STATUS_AVAILABLE`; `readToday` gets an 8 s timeout; keep every call defensive.
- Settings/permission launch uses `requiredPermissions`; after grant, nudge the service (start/re-arm + flush) so the new grant is applied without a process restart.

### 6. UI
- Home: unchanged semantics but source badge reflects manifold (`estimated` when marker > 0, `synced` when HC is the confirmed floor); stale source age on Diagnostics.
- Diagnostics: add "Storage writes" (OK/failing), per-source last event, HC readiness, switches; strings EN/AR.
- Settings: HC copy stays truthful ("synced floor; the phone's sensors keep counting").
- README/spec/claims audit/QA checklist: describe the manifold, the estimate marker, the recovery matrix and the residual limits; add hardware QA rows (reboot mid-walk, HC grant after start, HC revoke, force-stop recovery).

### 7. Verification
- Unit: new pipeline + watchdog tests green; old tests that encode the discarded rebase semantics removed or rewritten. The ≥140 target was optimistic because the v1.1 source-policy/health suites (15 tests) were deleted with the ladder; the meaningful replacement is the manifold invariant suite plus migration/marker tests, reported with the build output.
- Build: `gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`; release `assembleRelease bundleRelease` before sign-off.
- Emulator: install, walk waveform, process kill/restart, reboot, HC grant path (if provider present), storage/estimate path; record evidence dumps. The emulator has no step counter, so counter recovery is proven by unit tests + the physical-device Step Zero when hardware is available; state that limit in the evidence file.
- Docs: evidence file with commands, dumps, pass/fail; update claims audit and release evidence log.

## Order

1. Domain tests + implementation (`StepPipeline`, `SilenceWatchdog`), no Android dependencies.
2. Settings migration + repository snapshot API; unit-test migration through a fake DataStore if feasible (otherwise instrumentation-free pure mapping test).
3. Service rewrite; build + emulator smoke.
4. Recovery worker + notification + boot flush; manifest/HC permission.
5. Diagnostics/strings/docs.
6. Full verification, commit, final report with residual limits.

## Risks and rollback

- The rewrite touches the only writer; keep the old code path in git history (`7af0828`) and ship no partial build. Any regression under 24 h field use reverted to `7af0828`.
- Detector max fusion is falsifiable (Step Zero); if it overcounts, demote detector to liveness-only with one constant change.
- Background HC permission adds Play review scope; if rejected, remove it and keep foreground HC polling — counting from sensors is unaffected.

## Implementation status (2026-11-21)

Delivered and verified in the working tree (commit follows):

- **Domain:** `StepPipeline` manifold with per-source ledgers, derived marker, counter reboot
  rebase, 6 steps/s + 5 clamp, detector sanity, HC floor, day rollover; `SilenceWatchdog`
  (probe/backoff/estimate, cannot self-disable); `StepState.migrateLegacyState` (pure, tested);
  `preserveLegacyMarker` (tested). `StepSourcePolicy`/`StepSourceHealth` and their tests deleted.
- **Persistence:** `SettingsStore` schema 2 with legacy migration and a manifold-schema cache;
  `StepsRepository.record(reconcile)` writes all ledgers in one DataStore write + one row upsert,
  preserves the legacy row marker on the first v1.2 write, seeds/rolls days, restores from CSV.
- **Service:** counter+detector registered for the whole day at `SENSOR_DELAY_NORMAL`; 1 s
  coalesced writer; counter catch-up accepted after process death via the sensor timestamp; HC poll
  (3 s first, 180 s / 60 s silent, 8 s timeout, granted floor only); watchdog probe + continuous
  estimate only when nothing measured is delivering; bounded estimate-silence re-registration;
  crash containment per concern, storage faults marked, bounded `onDestroy` flush with a write
  mutex; synchronized status/cadence/watchdog state across the sensor and writer threads.
- **Recovery:** `ReconcileWorker` midnight reconcile, HC read when granted, goal catch-up and a
  rate-limited "counting paused" notification when the service is not running but counting is
  allowed; it never starts an FGS. Boot receiver and app-resume flush unchanged in intent.
- **UI/docs:** Diagnostics storage-health row, source/estimate/staleness rows, EN+AR strings,
  Settings HC permission launch uses `requiredPermissions`; spec §3, README, claims audit,
  hardware QA and the release evidence log updated.
- **Verification (done 2026-11-21):** 127/127 JVM tests in 10 suites (0 failures/errors/skips),
  `assembleDebug` BUILD SUCCESSFUL, `lintDebug` exit 0 (0 errors / 46 warnings / 1 hint),
  `assembleRelease` + `bundleRelease` BUILD SUCCESSFUL with `lintVitalRelease` passing; signed
  APK 1,864,306 B and AAB 3,992,650 B, versionCode 2 / versionName 1.1.0, `apksigner`/`zipalign`/
  `jarsigner` OK. Emulator pass (API 36 ATD): install/onboarding, health FGS, stationary zero,
  synthetic ~55 s walk → 56 labelled estimated steps, force-stop + reopen kept the total and
  restarted the service, a second walk added to the restored total, delete-all reset onboarding,
  boot auto-started the FGS; crash buffer empty. The emulator has no counter/detector and no HC
  step data, so the sensor fusion paths are unit-tested only; the **physical Step Zero** (§2 of
  the hardware QA checklist) remains the gate for the measured fusion claim. Full evidence:
  [docs/review/2026-11-21/step-resilience/EVIDENCE.md](../../review/2026-11-21/step-resilience/EVIDENCE.md).

### Deviations from the plan text

- Status/permission names keep the v1.1 UI wording where it is still accurate ("Fallbacks today"
  now counts display-source changes, not ladder switches).
- The HC first poll is ~3 s (not 5 s) so a granted floor lands before the first watchdog window;
  the poll cadence is 180 s, or 60 s while measured sources are silent (plan said 180/60).
- The estimate stall re-registration and status synchronization were added after a service audit;
  they are hardening, not new user-visible behavior.