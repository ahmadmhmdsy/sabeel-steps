# Sabeel Step-Source Resilience Plan

> **Status:** delivered 2026-11-19 (Phases A–C, commit `8246615`). **v1.1 update 2026-11-21:** owner approved
> (1) keeping the estimate in goal/streak arithmetic (recommended default, always marked), (2) the read-only
> **Health Connect** tier (dependency + `READ_STEPS` + Play health declaration), and (3) persisting the estimate
> marker in the DB/CSV/saved walks. Phases D deliver those; physical-device validation remains owner-blocked.
> Created 2026-11-19 (Asia/Baghdad).
> **Owner:** Lead (inline execution). **Baseline:** \`fe51d2d\`.
> **Skill:** \`writing-plans\`; verdict input: Qiyas Council Mode C, 2026-11-19 (council report in chat; prior TrueStep council at \`docs/review/2026-11-17/TRUESTEP-EXTERNAL-COUNCIL.md\`).

**Goal:** A phone with a working step counter counts exactly as today; a phone whose counter stops reporting falls back
to another *measured* sensor (step detector), and only when neither measured sensor can work falls back to a clearly
labelled accelerometer *estimate*. Every switch is rebased, physically impossible jumps are rejected, and Home +
Diagnostics tell the truth about which source is live and how old its last reading is. No new permission, no new
dependency, no network, no silent degradation.

**Architecture:** One pipeline, one writer, source adapters. \`StepPipeline\` stays the only place that turns a
cumulative raw value into a daily total. It gains a \`source\` tag and rebases on every source switch (and on every
date change), so a fallback never double-counts the gap. The service chooses the source from measured availability
(\`TYPE_STEP_COUNTER\` -> \`TYPE_STEP_DETECTOR\` -> accelerometer estimate), watches the active measured source for
silence, probes motion before switching, and keeps the counter registered after a fallback so it can return the
moment it reports again. \`StatusStore\` carries source + last event age + switch count; Home and Diagnostics render it;
the estimate is always marked with "≈" and plain-language copy. Walk sessions use the same ladder so a walk is not
step-less on a detector-only phone.

**Tech stack:** Kotlin 2.0.21, Compose Material3, Room 2.6.1, DataStore, WorkManager, JUnit4. Min SDK 26,
compile/target 36, JDK 21, package \`com.sabeel.steps\`. Offline only (no INTERNET).

**Environment (every Gradle call):**

    $env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
    $env:ANDROID_HOME = 'C:/Android/Sdk'
    $env:ANDROID_SDK_ROOT = 'C:/Android/Sdk'
    & .\gradlew.bat <tasks> --no-daemon --console=plain

Emulator QA: \`emulator-5556\` (API 36, 1080x2400, headless) has **no step counter and no step detector** but has an
accelerometer; it is the natural estimate-tier test bed. \`adb\` = \`C:/Android/Sdk/platform-tools/adb.exe\`.

---

## Provenance and decision history (do not relitigate silently)

- The TrueStep prototypes (\`alshahia/truestep_gamified\`, inspected 2026-11-17) were used as **evidence**, never as
  source: no file, string, drawable or code block is copied. Their clones stay untracked under \`.external/\`.
- The 2026-11-17 council suspended "Wave 3" including an **accelerometer fallback** because it was unlabelled and
  over-scoped. The 2026-11-19 council reversed that suspension **only** for a last-resort, visibly estimated,
  no-new-permission tier; measured fallback (detector) and transition safety are the primary scope.
- **Health Connect was unsuspended by the owner on 2026-11-21** for a read-only daily-total tier on phones
  with no hardware step sensor (dependency + \`READ_STEPS\` + Play health declaration approved). It is an
  absolute floor, never additive, never written back, and never active alongside a hardware sensor.
  Manual step editing stays dropped; midnight late-arrival recovery stays suspended.
- The estimate counting toward the daily goal / streak is the recommended default (always marked), reversible by
  hiding the estimate from goal arithmetic if the owner later prefers display-only.

## Locked decisions

| # | Decision |
|---|---|
| D1 | Source ladder: \`COUNTER\` (measured) -> \`DETECTOR\` (measured) -> \`MOTION_ESTIMATE\` (estimated) -> \`NONE\`. Chosen from \`SensorManager.getDefaultSensor\` availability, never from a feature flag alone. |
| D2 | One pipeline, one writer. No parallel counters; fallback sensors are adapters that produce a cumulative raw value for \`StepPipeline\`. |
| D3 | Rebase on every source switch: \`initialized = true\`, new baseline = first raw of the new source, today's total preserved, delta of the switch ignored (no double count). |
| D4 | Date change still wins over a source switch: \`todaySteps = 0\`, rolled over, per spec 3.5. |
| D5 | Plausibility clamp: a single delta cannot exceed \`floor(elapsed_seconds * 4) + 3\` steps when the previous event time is known. Physically impossible only; never clamps normal walking. |
| D6 | Silence watchdog: a measured source that reports an event after starting and then goes silent for 10 min triggers a 5 s motion probe. Two consecutive probes that detect walking switch to the next tier. A counter event arriving while on a fallback switches back immediately (delta ignored). |
| D7 | Estimate mode is only chosen when no counter and no detector exists or both registrations fail; it is labelled everywhere (Home number, Diagnostics, notification), never silently mixed into measured numbers. |
| D8 | Status/Diagnostics must expose source, last sensor reading age, and switch/fallback count. \`NO_SENSOR\` now means "no counter, no detector, no accelerometer" (truly cannot count). |
| D9 | No INTERNET and no new dependencies **except** the owner-approved \`androidx.health.connect:connect-client\` and the read-only \`health.READ_STEPS\` permission (owner approval 2026-11-21). Sensor batching is used when the HAL reports it, but never delays the writer. |
| D10 | Walk sessions use the same ladder; a step-only walk on a detector phone must count steps instead of staying at 0. |
| D11 | EN and AR strings are added together; \`LocalizationParityTest\` is the gate. Arabic copy is marked for native review. |
| D12 | Every phase is committed separately with its tests; emulator evidence goes under \`docs/review/2026-11-19/\`. |
| D13 | Health Connect tier: read-only \`StepsRecord.COUNT_TOTAL\` for today every 2 min (first read 5 s), applied as an absolute floor (\`max(current, providerTotal)\`), active only when the phone has no counter and no detector and the user opted in; only a rising total counts as liveness; 30 min of failed reads falls back to the estimate; no write access is declared or used. |
| D14 | The estimate portion of a day is persisted (\`daily_steps.estimatedSteps\`) and the saved-walk estimate flag (\`walks.estimated\`), Room v1->v2 migration, and exposed in CSV (\`estimated_steps\`) plus legacy 5-column import. Estimate markers never decrease within a day and are clamped to 0..steps. |

## Definition of Done (gates)

- G1 \`:app:testDebugUnitTest\` green, baseline 79 tests plus the new suites (target 95+), zero failures.
- G2 \`:app:assembleDebug\` succeeds; \`:app:lintDebug\` exit 0 (warnings may remain, list them).
- G3 Emulator: install debug APK, grant ACTIVITY_RECOGNITION, launch -> no crash; Diagnostics shows source
  "Phone motion (estimated)", Home shows "≈" + estimate note; stationary 60 s adds no steps; injected walking
  waveform adds > 0 estimated steps and one labelled source change.
- G4 EN and AR parity test green; Arabic screenshots of Home + Diagnostics captured in the evidence folder.
- G5 No path lowers a day, resurrects a deleted day, or counts a counter catch-up after a fallback (unit tests).
- G6 One commit per phase; working tree clean except untracked \`.external/\`.
- G7 (v1.1) The estimate marker survives restarts in Room and CSV (round-trip tests), and the Health Connect
  floor never lowers a day, never adds a total twice and is never active beside a hardware sensor (unit tests).

---

## Interface contract (freeze before parallel edits)

### Domain (pure, no Android imports)

    enum class StepSource { COUNTER, DETECTOR, MOTION_ESTIMATE, NONE;
        val measured: Boolean get() = this == COUNTER || this == DETECTOR
    }

    data class SourceAvailability(
        val counter: Boolean,
        val detector: Boolean,
        val accelerometer: Boolean,
    ) {
        val any: Boolean get() = counter || detector || accelerometer
    }

    object StepSourcePolicy {
        fun choose(available: SourceAvailability): StepSource
        fun nextMeasured(current: StepSource, available: SourceAvailability): StepSource? // DETECTOR after COUNTER
        fun bestMeasured(available: SourceAvailability): StepSource?
        fun canEstimate(available: SourceAvailability): Boolean // accelerometer present
        fun fallbackTarget(current: StepSource, available: SourceAvailability): StepSource? // never repeats a silent tier
    }

    data class StepState(
        val baselineRaw: Float,
        val todaySteps: Int,
        val baselineDate: String,
        val initialized: Boolean = false,
        val source: StepSource = StepSource.COUNTER,
        val lastRawAt: Long = 0L,
    )

    object StepPipeline {
        fun onSensor(
            state: StepState,
            raw: Float,
            today: String,
            source: StepSource = state.source,
            nowMillis: Long = 0L,
        ): StepUpdate
        fun resolveTodaySteps(state: StepState, dbTodaySteps: Int?): Int
    }

    class MotionStepDetector(
        threshold: Float = 1.2f,
        hysteresis: Float = 0.15f,
        minStepIntervalMs: Long = 250L,
        maxStepIntervalMs: Long = 2_000L,
        burstSize: Int = 3,
        burstWindowMs: Long = 3_000L,
        stopAfterMs: Long = 2_500L,
        smoothingTauMs: Double = 90.0,
        baselineTauMs: Double = 1_000.0,
        gravity: Float = 9.81f,
    ) {
        fun onSample(magnitude: Float, nowMillis: Long): Int // newly confirmed steps (0, burst flush, or 1)
        val isWalking: Boolean
        fun reset()
    }

    class StepSourceHealth(
        stallAfterMs: Long = 600_000L,
        probeIntervalMs: Long = 60_000L,
        walkingProbesToSwitch: Int = 2,
        maxProbeIntervalMs: Long = 30 * 60_000L, // still-phone probe backoff
    ) {
        fun onSourceStart(source: StepSource, nowMillis: Long)
        fun onEvent(nowMillis: Long)
        fun onTick(nowMillis: Long): Action          // service starts the 5 s probe on ProbeMotion
        fun onProbeResult(nowMillis: Long, walking: Boolean): Action
        sealed interface Action {
            data object None : Action
            data object ProbeMotion : Action
            data object Fallback : Action            // service picks the target from availability
        }
        val source: StepSource
        val switches: Int
    }

### Platform

    object StepSensors {
        fun availability(context: Context): SourceAvailability
        fun hasAny(context: Context): Boolean
        fun labelRes(source: StepSource): Int
    }

    data class StepSourceStatus(
        val source: StepSource = StepSource.NONE,
        val lastEventAt: Long = 0L,
        val switches: Int = 0,
    )

    object StatusStore {
        fun markSource(context: Context, status: StepSourceStatus)
        fun readSource(context: Context): StepSourceStatus
        // existing mark/read/clear stay
    }

### UI state

    data class HomeUiState(..., val sourceStatus: StepSourceStatus = StepSourceStatus())
    data class DiagnosticsUiState(..., val sourceStatus: StepSourceStatus = StepSourceStatus())

---

## Phase A - measured resilience (counter -> detector), truth surfaces

### A1. Source policy + availability (domain)
**Files:** \`app/src/main/java/com/sabeel/steps/domain/StepSource.kt\` (new),
\`app/src/main/java/com/sabeel/steps/platform/StepSensors.kt\` (new),
\`app/src/test/java/com/sabeel/steps/domain/StepSourcePolicyTest.kt\` (new).
- [x] Test: counter wins; detector when only detector; estimate only when only accelerometer; NONE when nothing;
      \`nextMeasured(COUNTER)\` = DETECTOR, \`nextMeasured(DETECTOR)\` = null; labels mapped for all four sources.
- [x] Implement pure policy, then \`StepSensors\` with \`getDefaultSensor(TYPE_STEP_COUNTER/DETECTOR)\` +
      \`TYPE_ACCELEROMETER\`. Deprecate (but keep) \`Permissions.hasStepSensor\` only where still referenced.

### A2. Pipeline: source tag, rebase, clamp
**Files:** \`domain/StepPipeline.kt\`, \`data/prefs/SettingsStore.kt\`, \`data/repo/StepsRepository.kt\`,
\`app/src/test/java/com/sabeel/steps/domain/StepPipelineTest.kt\`.
- [x] Test: source switch preserves today but ignores the switch delta; date change still resets; counter -> detector
      -> counter does not double count; clamp rejects a 10,000 jump after 1 s but keeps normal 4-step deltas; clamp is
      skipped when \`nowMillis == 0\`; \`lastRawAt\` round-trips.
- [x] Add DataStore keys \`step_source\` / \`step_last_raw_at\` (migration-safe defaults: COUNTER / 0).
- [x] Pass \`nowMillis\` and the active source from \`StepsRepository.recordRawStep\`; keep \`resolveTodaySteps\`.
- [x] Keep all 13 existing pipeline tests green (defaults preserve behaviour).

### A3. MotionStepDetector (pure)
**Files:** \`domain/MotionStepDetector.kt\` (new), \`app/src/test/java/com/sabeel/steps/domain/MotionStepDetectorTest.kt\` (new).
- [x] Synthetic harness in tests (50 Hz and 16.6 Hz): 120 s at 1.8 Hz -> 216 +/- 3 steps; 90 s at 1.2 Hz -> 108 +/- 3;
      stationary noise -> 0; typing-like 0.3 amplitude -> 0; amplitude 1.2 (bag-damped) may under-count (documented).
- [x] Implementation uses time-based EMA coefficients (\`1 - exp(-dt/tau)\`) so 10-50 Hz behave alike; hysteresis;
      refractory 250-2000 ms; 3-peak burst; 2.5 s stop. \`reset()\` clears all state.
- [x] \`isWalking\` is true after a confirmed burst and false 2.5 s after the last peak.

### A4. StepSourceHealth (pure watchdog policy)
**Files:** \`domain/StepSourceHealth.kt\` (new), \`app/src/test/java/com/sabeel/steps/domain/StepSourceHealthTest.kt\` (new).
- [x] Test: no action before 10 min silence; ProbeMotion at silence; one walking probe is not enough; two walking
      probes -> SwitchTo(DETECTOR); non-walking probe resets the streak; counter events reset the stall clock; switch
      count increments once.
- [x] Decision only; the service owns registration and persistence.

### A5. StatusStore + service ladder
**Files:** \`platform/StatusStore.kt\`, \`service/StepCounterService.kt\`, \`platform/Permissions.kt\`.
- [x] StatusStore: \`markSource\`/\`readSource\` (SharedPreferences, same file); \`clear()\` unchanged.
- [x] Service: resolve availability; start on \`StepSourcePolicy.choose\`; register counter and, when active, detector;
      always keep counter registered when available so recovery is immediate; on switch call \`flushPending\` first,
      reset pipeline raw, write status; detector adapter keeps a cumulative synthetic raw (count of detector events);
      batching via \`getMaxDelay()\` capped at 20 s, fall back to \`SENSOR_DELAY_NORMAL\`; estimate later in Phase B.
- [x] Silence watchdog in the heartbeat loop: when active source is measured and silent >= 10 min, register the
      accelerometer for a 5 s probe; feed \`MotionStepDetector\`; two walking probes -> switch. Probe registration is
      removed afterwards. Counter events while on fallback -> switch back (delta ignored by D3).
- [x] \`ensureStarted\` gates on \`StepSensors.hasAny\`; \`NO_SENSOR\` only when no sensor at all.
- [x] Notification unchanged for measured sources; estimated notification text added in Phase B.

### A6. UI truth surface (measured only)
**Files:** \`ui/viewmodel/AppViewModels.kt\`, \`ui/screens/HomeScreen.kt\`, \`ui/screens/DiagnosticsScreen.kt\`,
\`res/values/strings.xml\`, \`res/values-ar/strings.xml\`.
- [x] ViewModels poll \`StatusStore.readSource\` alongside service status; use \`StepSensors.hasAny\` instead of
      \`Permissions.hasStepSensor\`.
- [x] Home: no new card for measured fallback (calm); Diagnostics rows: "Source" (Hardware step counter / Hardware step
      detector / Phone motion (estimated) / None), "Last sensor reading" (time or "Not yet"), and a fallback count row
      only when > 0. Update \`diagnostics_no_sensor_note\` and \`home_status_no_sensor_body\` to the "no motion sensor
      at all" truth; \`sensor_notif_no_sensor_text\` likewise.
- [x] Keep the existing "Updated HH:mm" row.
- [x] New EN+AR keys: \`diagnostics_source_label\`, \`diagnostics_source_counter\`, \`diagnostics_source_detector\`,
      \`diagnostics_source_estimate\`, \`diagnostics_source_none\`, \`diagnostics_last_event_label\`,
      \`diagnostics_last_event_none\`, \`diagnostics_fallbacks_label\`, \`diagnostics_fallbacks_none\`,
      \`diagnostics_source_note\`.

### A7. Verify + commit Phase A
- [x] \`testDebugUnitTest assembleDebug lintDebug\`; fix all failures (final run: 108 tests, 0 failures; lint 0 errors; see Phase C).
- [x] Commit: \`feat(steps): measured-first source ladder with rebase, clamp and honest source status\` (\`8246615\`, with B1/B2).

---

## Phase B - last-resort labelled estimate + walks

### B1. Estimate tier in the counting service
- [x] When \`StepSourcePolicy.choose\` returns \`MOTION_ESTIMATE\` (or measured registrations fail), register the
      accelerometer at \`SENSOR_DELAY_UI\`, feed \`MotionStepDetector\`, and add confirmed steps to a synthetic
      cumulative raw so \`StepPipeline\` sees a monotonic counter. Source = \`MOTION_ESTIMATE\`.
- [x] On estimate start, \`StepPipeline\` rebases (D3); the first estimates are counted, never a catch-up.
- [x] If a measured sensor becomes available/registered (e.g. detector event, counter event, service restart after an
      OS update), switch back immediately and rebase.
- [x] Status writes source=MOTION_ESTIMATE, lastEventAt, switches; notification body uses
      \`sensor_notif_estimate_text\`; Home shows \`≈ <steps>\` + \`home_estimated_note\`; ring content description
      includes the estimate phrasing; Diagnostics source row already covers the label.
- [x] New EN+AR keys: \`home_estimated_note\`, \`estimated_steps_value\` ("≈ %1$s"), \`sensor_notif_estimate_text\`,
      \`diagnostics_status_estimated\` ("Counting (estimated)").
- [x] Tests: \`StepSourceHealthTest\` / service-independent estimate flow (policy + pipeline transitions);
      detector suite already covers the math.

### B2. Walk sessions
- [x] \`WalkTrackingService\`: replace counter-only registration with the same availability ladder. Counter keeps the
      existing delta logic; detector increments per event; estimate feeds \`MotionStepDetector\` from the accelerometer.
      Route/location behaviour unchanged; no new permission.
- [x] Walk with no step hardware but an accelerometer counts estimated steps and the saved walk is marked? The saved
      walk schema already stores steps; no field added (DB unchanged). Live walk, saved summary and notification say the
      count is estimated.
- [x] Keep the walk checkpoint format compatible (new fields optional).

### B3. Verify + commit Phase B
- [x] \`testDebugUnitTest assembleDebug lintDebug\` green; shipped with A7 in \`8246615\`.

---

## Phase C - emulator evidence, docs, final verification

### C1. Emulator QA (API 36, no step counter/detector)
- [x] Build + install debug APK on \`emulator-5556\`; grant ACTIVITY_RECOGNITION with \`adb shell pm grant\`;
      complete/seed onboarding as in prior smoke evidence.
- [x] Stationary 60 s: steps stay 0; Diagnostics shows "Phone motion (estimated)" and a recent "Last sensor reading".
- [x] Inject walking acceleration: \`adb emu sensor set acceleration <x>:<y>:<z>\` in a ~10 Hz PowerShell loop for 60 s
      with gravity on Y/Z and a 1.8 Hz sine on X (amplitude ~2.5 m/s^2). Expected: Home shows "≈" + a positive estimate;
      no crash; the source row stays Phone motion (estimated); switch count 0 (estimate is the initial source).
- [x] Capture Home (EN + AR), Diagnostics (EN + AR), and the notification via \`dumpsys notification\`; save under
      \`docs/review/2026-11-19/step-source-resilience/\` with an \`EVIDENCE.md\` that states the emulator limitation
      (no hardware step sensors; estimate path exercised, measured path unit-tested only).
- [x] If \`adb emu sensor set\` is too coarse to produce a clean waveform, fall back to a **debug-only** virtual source
      in the QA notes, not in the release code; record the exact command output as evidence.

### C2. Docs and claims
- [x] Update \`README.md\` step-sensor paragraph (ladder + estimate labelling), \`docs/product/spec.md\` section 3
      (source ladder, estimate policy), \`docs/release/CLAIMS-AUDIT.md\` (remove "accelerometer fallback not adopted"),
      \`docs/review/2026-11-17/TRUESTEP-EXTERNAL-COUNCIL.md\` Appendix D note (superseding decision), and
      \`docs/design/prototypes/README.md\` if it mentions counting sources.
- [x] Record the new evidence in \`docs/review/2026-11-19/step-source-resilience/EVIDENCE.md\`.
- [x] Final \`testDebugUnitTest assembleDebug lintDebug\` and clean git status; final report to owner.

## Phase D - owner-approved v1.1 additions (2026-11-21)

### D1. Persist the estimate marker (#3 approval)
- [x] Room v2: \`daily_steps.estimatedSteps INTEGER NOT NULL DEFAULT 0\`, \`walks.estimated INTEGER NOT NULL
      DEFAULT 0\`; \`MIGRATION_1_2\` added without destructive fallback; schema \`app/schemas/.../2.json\` exported.
- [x] \`StepPipeline\` tracks \`estimatedStepsToday\` (estimate source only, preserved across switches, reset at
      midnight, never above today's total); repository persists it monotonically.
- [x] \`WalkTrackingService\` keeps a sticky \`estimatedUsed\` flag (also restored from the checkpoint) and saves
      \`Walk(estimated = true)\` when any counted step came from the estimate.
- [x] CSV v2: header \`date,steps,distance_meters,calories,active_minutes,estimated_steps\`; legacy 5-column
      header/rows still import with 0; a row whose estimate exceeds its steps (or is negative) is skipped.
- [x] History: daily rows and saved walks show an "estimated" badge; the walk dialog says the count was estimated.
- [x] Tests: pipeline estimate tracking/preservation/midnight reset, CSV round-trip/legacy/impossible-row,
      localization parity for the new EN+AR strings.

### D2. Health Connect read-only tier (#2 approval)
- [x] Platform reader \`HealthConnect.kt\`: availability (provider installed/version), \`READ_STEPS\` permission
      check, settings intent; \`readToday\` aggregates \`StepsRecord.COUNT_TOTAL\` for the local day.
- [x] Manifest: \`android.permission.health.READ_STEPS\`, provider \`<queries>\`, \`ACTION_HEALTH_CONNECT_SETTINGS\`,
      and the exported \`VIEW_PERMISSION_USAGE\` activity-alias; dependency
      \`androidx.health.connect:connect-client:1.1.0\`.
- [x] Policy: \`HEALTH_CONNECT\` between detector and estimate; availability-aware choose/fallback; sync label.
- [x] Pipeline/repository: \`onExternalTotal\` absolute floor, day rollover, estimate portion preserved,
      negative/duplicate reads ignored; \`recordExternalTotal\` writes the row.
- [x] Service: readiness computed only when no counter/detector; polling every 2 min (5 s first); activation only
      when the provider total is ahead; only a *rising* total refreshes liveness (so a static provider cannot
      suppress the estimate watchdog); 30 min failed reads falls back; notification and Diagnostics copy.
- [x] UI: Home sync note; Diagnostics "Health Connect (synced)" source + "Last sync" row; Settings opt-in card
      (connect/manage, read-only note) shown only on phones with no measured step sensor; view-model status gates
      accept Health Connect-ready phones.
- [x] Tests: policy ordering/fallbacks/flags, external-total floor/day-rollover/negative cases.

### D3. Verification, docs and owner gaps
- [x] \`testDebugUnitTest assembleDebug lintDebug\` green after the changes: 121/121 tests in 11 suites, lint
      0 errors / 44 warnings / 1 hint; `assembleRelease` + `bundleRelease` rebuilt 2026-11-21 22:44 and the
      signed artifacts re-measured (APK 1,861,838 B, AAB 3,977,681 B; same upload key; `apksigner`,
      `jarsigner`, `zipalign -c -P 16` OK).
- [x] Spec, README, brief, Play compliance checklist, privacy policy, claims audit, blockers and hardware
      checklist updated for the new permission/data flow/CSV column; claims audit §8, release sign-off and
      evidence log §8 now carry the 2026-11-21 measurements. The privacy-site HTML still needs regeneration
      from the v1.1 Markdown (owner, B1).
- [ ] **Owner/Play:** health declaration + Health Connect data-type declaration, Data safety answer for the
      on-device Health Connect read, hosted privacy policy regeneration (docs/privacy-site), and a real
      no-sensor phone with Health Connect steps to verify the read/floor/revoke paths. The emulator pass on
      2026-11-21 shows the opt-in card and Health Connect's own read-only Steps permission screen, but the image
      has no steps to read, so the read/floor/revoke paths stay hardware/owner-blocked.
- [ ] **Owner:** decide whether the Health Connect tier should also run on phones that *do* have a step sensor
      (currently never, to avoid double counting); that needs a merge design before it is enabled.

### Post-plan closure (2026-11-19 evening, after the first owner report)
- [x] Found and fixed a truth gap: walk sessions used the ladder but showed the estimated count unmarked. Live walk,
      saved summary and walk notification now say "estimated"; no schema change. QA dumps: \`qa-17\`, \`qa-18\`.
- [x] Rebuilt \`assembleRelease\` + \`bundleRelease\` from the current tree (2026-11-19 20:11), verified
      \`apksigner\` (v2), \`jarsigner\` (AAB), \`zipalign -c -P 16\`, permissions and manifest, and regenerated
      \`dist/SHA256SUMS.txt\`. \`docs/legal/release-signoff.md\` and \`CLAIMS-AUDIT.md\` §6.1 now carry the new values.
- [x] Emulator pass on 2026-11-21 (release APK over the previous release install, Room v1->v2, old rows
      preserved; Health Connect card + read-only Steps permission screen; EN + AR estimate labels;
      synthetic-input estimated walk rendered live/summary/History/dialog; crash buffer empty) recorded in
      [docs/review/2026-11-21/health-connect/EVIDENCE.md](../../review/2026-11-21/health-connect/EVIDENCE.md).
- [x] Wrote [docs/legal/HARDWARE-QA-CHECKLIST.md](../../legal/HARDWARE-QA-CHECKLIST.md): counter-phone, detector-only,
      estimate-tier, stall/battery and midnight rows with commands and a results header, so the owner can run B7
      without project context.

---

## Owner decisions / gaps to raise (not silently decided)

1. **Estimate counts toward goal/streak** - currently yes (recommended), always marked. Reversible.
2. **Health Connect** - **APPROVED 2026-11-21** (dependency + read-only \`READ_STEPS\` + health declaration) and
   implemented as the D13 floor tier. Still owner-blocked: Play Console health declaration/Data safety, hosted
   policy, and a real no-sensor phone to verify the read/revoke paths.
3. **Physical-device validation** remains owner-blocked; the measured ladder needs a real counter phone and a real
   detector-only phone before Play submission. Emulator only proves the estimate path.
4. **Arabic copy** for the new estimate/fallback strings needs native review (same status as the existing copy).
5. **Handshake false positives** at walking cadence are physically indistinguishable to an accelerometer; the estimate
   label is the mitigation. Do not promise exact steps in estimate mode.
6. **Battery**: the estimate tier registers the accelerometer at UI rate only when no measured sensor exists; motion
   probes run for 5 s after 10 min of silence and back off exponentially (2, 4, 8 ... up to 30 min) while the phone
   stays still, so an overnight-still phone is not probed every minute. Revisit with a real detector-only phone if
   drain is measurable.
7. **Estimate marker in data** - **RESOLVED 2026-11-21** (owner approved #3): \`daily_steps.estimatedSteps\`,
   \`walks.estimated\` and the CSV \`estimated_steps\` column now carry it; legacy CSV still imports. The DB is v2
   and the v1->v2 migration is additive.
8. **versionCode before Play upload.** The current signed artifacts are versionCode 1 / versionName 1.0.0 (rebuilt
   2026-11-21). Any upload after an existing track entry needs a higher versionCode, so rebuild, re-verify and
   re-hash immediately before uploading; do not reuse the 2026-11-21 files if a build was already uploaded.

## Task order and write scopes

| Order | Task | Write scope |
|---|---|---|
| 1 | A1+A2 (domain policy, pipeline, prefs, repo) | domain/StepSource.kt, domain/StepPipeline.kt, data/prefs/SettingsStore.kt, data/repo/StepsRepository.kt, tests |
| 2 | A3+A4 (detector, health policy) | domain/MotionStepDetector.kt, domain/StepSourceHealth.kt, tests |
| 3 | A5 (platform sensors, status, service) | platform/StepSensors.kt, platform/StatusStore.kt, platform/Permissions.kt, service/StepCounterService.kt |
| 4 | A6 (viewmodels, Home, Diagnostics, strings) | ui/viewmodel/AppViewModels.kt, ui/screens/HomeScreen.kt, ui/screens/DiagnosticsScreen.kt, res/values*/strings.xml |
| 5 | A7 verify+commit | - |
| 6 | B1+B2 (estimate + walks) | service/StepCounterService.kt, service/WalkTrackingService.kt, strings, UI estimate marker |
| 7 | B3 verify+commit | - |
| 8 | C1+C2 (QA, evidence, docs, final verify) | docs/**, README.md |
| 9 | D1 estimate marker (Room v2, pipeline, walk flag, CSV, History) | data/db, domain/StepPipeline.kt, data/repo/StepsRepository.kt, data/export/CsvCodec.kt, service/WalkTrackingService.kt, ui/screens/HistoryScreen.kt, tests |
| 10 | D2 Health Connect tier (platform reader, manifest, dependency, policy, pipeline floor, service poll, UI) | platform/HealthConnect.kt, AndroidManifest.xml, app/build.gradle.kts, domain/StepSource.kt, domain/StepPipeline.kt, data/repo/StepsRepository.kt, service/StepCounterService.kt, ui/screens, res/values* |
| 11 | D3 docs + final verification | docs/, README.md |