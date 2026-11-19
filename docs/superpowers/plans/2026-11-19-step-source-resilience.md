# Sabeel Step-Source Resilience Plan

> **Status:** executing (Phase A -> B -> C). Created 2026-11-19 (Asia/Baghdad).
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
- Health Connect remains **suspended** pending owner approval of dependency + \`READ_STEPS\` + Play health declaration.
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
| D9 | No new permissions, no new dependencies, no INTERNET. Sensor batching is used when the HAL reports it, but never delays the writer. |
| D10 | Walk sessions use the same ladder; a step-only walk on a detector phone must count steps instead of staying at 0. |
| D11 | EN and AR strings are added together; \`LocalizationParityTest\` is the gate. Arabic copy is marked for native review. |
| D12 | Every phase is committed separately with its tests; emulator evidence goes under \`docs/review/2026-11-19/\`. |

## Definition of Done (gates)

- G1 \`:app:testDebugUnitTest\` green, baseline 79 tests plus the new suites (target 95+), zero failures.
- G2 \`:app:assembleDebug\` succeeds; \`:app:lintDebug\` exit 0 (warnings may remain, list them).
- G3 Emulator: install debug APK, grant ACTIVITY_RECOGNITION, launch -> no crash; Diagnostics shows source
  "Phone motion (estimated)", Home shows "≈" + estimate note; stationary 60 s adds no steps; injected walking
  waveform adds > 0 estimated steps and one labelled source change.
- G4 EN and AR parity test green; Arabic screenshots of Home + Diagnostics captured in the evidence folder.
- G5 No path lowers a day, resurrects a deleted day, or counts a counter catch-up after a fallback (unit tests).
- G6 One commit per phase; working tree clean except untracked \`.external/\`.

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
- [ ] \`testDebugUnitTest assembleDebug lintDebug\`; fix all failures.
- [ ] Commit: \`feat(steps): measured step-source ladder with rebase, clamp and honest source status\`.

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
      walk schema already stores steps; do not add a field (keep DB unchanged) - the notification says estimate.
- [x] Keep the walk checkpoint format compatible (new fields optional).

### B3. Verify + commit Phase B
- [ ] \`testDebugUnitTest assembleDebug lintDebug\`; commit:
      \`feat(steps): labelled motion estimate as last-resort source; walks use the same ladder\`.

---

## Phase C - emulator evidence, docs, final verification

### C1. Emulator QA (API 36, no step counter/detector)
- [ ] Build + install debug APK on \`emulator-5556\`; grant ACTIVITY_RECOGNITION with \`adb shell pm grant\`;
      complete/seed onboarding as in prior smoke evidence.
- [ ] Stationary 60 s: steps stay 0; Diagnostics shows "Phone motion (estimated)" and a recent "Last sensor reading".
- [ ] Inject walking acceleration: \`adb emu sensor set acceleration <x>:<y>:<z>\` in a ~10 Hz PowerShell loop for 60 s
      with gravity on Y/Z and a 1.8 Hz sine on X (amplitude ~2.5 m/s^2). Expected: Home shows "≈" + a positive estimate;
      no crash; the source row stays Phone motion (estimated); switch count 0 (estimate is the initial source).
- [ ] Capture Home (EN + AR), Diagnostics (EN + AR), and the notification via \`dumpsys notification\`; save under
      \`docs/review/2026-11-19/step-source-resilience/\` with an \`EVIDENCE.md\` that states the emulator limitation
      (no hardware step sensors; estimate path exercised, measured path unit-tested only).
- [ ] If \`adb emu sensor set\` is too coarse to produce a clean waveform, fall back to a **debug-only** virtual source
      in the QA notes, not in the release code; record the exact command output as evidence.

### C2. Docs and claims
- [ ] Update \`README.md\` step-sensor paragraph (ladder + estimate labelling), \`docs/product/spec.md\` section 3
      (source ladder, estimate policy), \`docs/release/CLAIMS-AUDIT.md\` (remove "accelerometer fallback not adopted"),
      \`docs/review/2026-11-17/TRUESTEP-EXTERNAL-COUNCIL.md\` Appendix D note (superseding decision), and
      \`docs/design/prototypes/README.md\` if it mentions counting sources.
- [ ] Record the new evidence in \`docs/review/2026-11-19/step-source-resilience/EVIDENCE.md\`.
- [ ] Final \`testDebugUnitTest assembleDebug lintDebug\` and clean git status; final report to owner.

---

## Owner decisions / gaps to raise (not silently decided)

1. **Estimate counts toward goal/streak** - currently yes (recommended), always marked. Reversible.
2. **Health Connect** stays suspended until the owner approves dependency + \`READ_STEPS\` + Play health declaration;
   it is the only way to recover steps from another app/wearable.
3. **Physical-device validation** remains owner-blocked; the measured ladder needs a real counter phone and a real
   detector-only phone before Play submission. Emulator only proves the estimate path.
4. **Arabic copy** for the new estimate/fallback strings needs native review (same status as the existing copy).
5. **Handshake false positives** at walking cadence are physically indistinguishable to an accelerometer; the estimate
   label is the mitigation. Do not promise exact steps in estimate mode.
6. **Battery**: the estimate tier registers the accelerometer at UI rate only when no measured sensor exists; motion
   probes run for 5 s after 10 min of silence and back off exponentially (2, 4, 8 ... up to 30 min) while the phone
   stays still, so an overnight-still phone is not probed every minute. Revisit with a real detector-only phone if
   drain is measurable.

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