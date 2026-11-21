# Council on "counting must never fail" — Qiyas mode C
Date: 2026-11-21 · Subject: step-counting resilience / manifold design · Language: English
Evidence base: `RESEARCH.md` in this folder, the read-only audits by `core-logic` and `android-platform`, the current tree at `7af0828`, and the prior plan `docs/superpowers/plans/2026-11-19-step-source-resilience.md`.

## Method

Qiyas (analogical reasoning) is applied per proposal: identify the effective cause (`illa`) that makes the precedent work or fail, verify that the cause is present in our case, then accept, reject or amend. The adversarial council is deliberately hostile; every role must name at least one fatal flaw in the leading proposal, and at least one proposal must actually fall. No role may praise the plan without evidence. The Maestro does not average opinions: it rules with an implementable spec and one falsification test that must pass before code is written.

## Role cards

| Role | Identity | What it optimises | Hard constraint it defends |
|---|---|---|---|
| R1 | **Rashid** — sensor HAL & device-fleet engineer (Samsung/Xiaomi/Huawei/Pixel, Android 8–16) | Count survives every HAL behaviour on the fleet | Never lose a source that could still have evidence; registration is the only proof of counting |
| R2 | **Nadia** — Android platform & Play policy counsel | Legal, reviewable, no rejection | Absolute marketing claims are indefensible; sensitive Health Connect grants need a declared, scoped purpose |
| R3 | **Tariq** — power & thermal engineer | 24 h battery budget | No wakelocks, no always-on high-rate accelerometer, no poll storm |
| R4 | **Mona** — on-device reliability/SRE | No crash, no silent data loss, fast recovery | Every callback is an entry point; every swallowed failure is a future lie |
| R5 | **Yusuf** — numerics & provenance auditor | One number that is real | Monotonic day, no double count, provenance marker always truthful |
| R6 | **Huda** — field product owner | User trust | The UI must never claim more than the data supports |

Dynamic roles convened for this topic: R2 doubles as Health Connect data-protection reviewer; R4 doubles as boot/WorkManager recovery owner.

## Proposals on the table

- **P1 — Keep the exclusive source ladder, just shorten the watchdog.** One active source; counter registered for recovery; estimate only when nothing else.
- **P2 — Manifold / hot standby.** Keep counter *and* detector registered for the whole day; per-source ledgers (`counterToday`, `detectorToday`, `hcToday`, `estimateToday`); fused day = max of evidence; estimated portion = total minus confirmed; HC as an always-polled floor when granted.
- **P3 — Health Connect as the primary source.** Read HC first every minute; fall back to sensors.
- **P4 — Wake-up sensors plus a partial wakelock to guarantee screen-off delivery.**
- **P5 — Let WorkManager carry counting when the service cannot run.**
- **P6 — Global uncaught-exception handler plus AlarmManager self-restart to guarantee uptime.**

## Transcript (condensed, adversarial)

**R1 (Rashid):** Start with the platform truth. `TYPE_STEP_COUNTER` only counts while registered, and it is cumulative since boot. `TYPE_STEP_DETECTOR` emits one event per step only while registered. The current ladder registers the detector but ignores its events unless it is the active source. That is not a fallback; that is a witness with its eyes closed. P1 is dead on arrival: `counterCatchUpAfterFallbackIsIgnored` is not a bug fix, it is a design decision to throw away evidence. Fatal flaw of P1: it keeps a backup source that is not accumulating.

**R5 (Yusuf):** Agreed, but P2 as written is also dangerous. If I take max(counter, detector) and the detector double-emits on one OEM, I publish an overcount every single day with no upper bound. The `illa` of "max fusion is safe" in the prior design is "the sources agree because they measure the same steps"; that is only true if per-event sanity holds. Precedent: the j4velin reports are full of overcounts, not sensor silence — arithmetic is the enemy. Amendment A1: one detector event adds exactly one, events above 10 are rejected, more than 5 per second is rejected; then max fusion is defensible.

**R4 (Mona):** Two more ways P2 lies. First, persistence: the current writer swallows write failures and the heartbeat keeps saying COUNTING. A manifold that cannot save is a manifold that loses the day; a failed DataStore write must surface as a visible degraded state. Second, the watchdog: it sets `probeInFlight=true` before it has actually started a probe, and `startMotionProbe` can return early (no accelerometer, no registration), so `onProbeResult` never runs and the watchdog self-disables forever. Fatal flaw of the current watchdog: it can be disabled by the exact condition it exists to detect. Amendment A6: set in-flight only after a probe is scheduled, clear it on every no-op, and feed detector events in as liveness so a probe is not the only heartbeat.

**R3 (Tariq):** P4 is rejected. There is no guaranteed wake-up step counter; a partial wakelock keeps the CPU awake and still loses detector events to FIFO loss; "guarantee counting with a wakelock" is a battery tax that does not buy the guarantee it sells. P2's cost must be quantified before I sign: counter and detector registration are near-free (the detector is 0.3 mA on the Samsung dump), Health Connect polls are binder reads every 1–3 minutes (acceptable), accelerometer is the expensive part. Therefore: accelerometer only for a bounded probe after measured silence, or continuously only on phones with no counter and no detector. Fatal flaw of "always estimate so we always count": it drains the battery and still stops when the AP suspends.

**R2 (Nadia):** P3 is rejected as primary. Health Connect is aggregated, batched about once per minute, absent on Android <= 13 without the provider, and its background reads need a *separate sensitive grant* plus feature support. It is a floor and cross-check, not a live source. Also, the user's phrase "will not fail to count" cannot ship as an absolute marketing claim — we can ship "multiple independent sources, visible degradation, recovery on the next legal start". Fatal flaw of the uncritical version: requesting the background-read permission on devices whose provider does not know it can cause the whole permission request to be rejected, and Play review will ask why a pedometer needs a sensitive background health grant. Amendment A5: declare it, request it only when `FEATURE_READ_HEALTH_DATA_IN_BACKGROUND` is available, and never make it a prerequisite for counting.

**R6 (Huda):** Keep P3's floor anyway. Today an HC-only user (no counter, no detector, motion permission granted) sees a live estimate that can sit beside a synced total; the settings copy says "synced steps replace the estimate" while the runtime never applies a grant made after the service started. That is a lie in the product. Fatal flaw of the current UX: it shows COUNTING without an age, without provenance, and without a storage/paused state. P2 must add: source age in Diagnostics, estimate badge derived from the gap, and an explicit "counting paused — tap to resume" recovery notification for OEM kills.

**R1:** P5 falls. WorkManager's 15-minute minimum, Doze deferral and the rare-bucket 10-minute runtime cannot count steps. It can reconcile and, with the extra grant, read Health Connect while the FGS is not running. Fatal flaw: treating a batched background job as a counter is a category error.

**R4:** P6 falls too. A global uncaught-exception handler that swallows exceptions is how you get a process that is alive, broken and confusing. AlarmManager cannot start a foreground service from the background; a force-stop kills alarms and receivers. The legal recovery matrix is: boot, package-replace, app-open, and a user tap on the recovery notification.

**R5:** On P2 arithmetic I need invariants, not slogans. Fatal flaw of naive "max": the estimate can become a co-equal source and then its accumulated marker is rewritten as measured when a sync arrives; the marker must be *derived* (total − confirmed), not accumulated, and may shrink. Also the four-step-per-second clamp currently uses wall clock; wall clock jumps on timezone/manual changes. Use the sensor's monotonic timestamp for plausibility, and never rebase away a counter's catch-up.

**R1:** One more platform limit nobody may paper over: a non-wake-up counter/detector does not deliver events while the AP is suspended. The counter's cumulative value heals the total when it wakes; the detector's per-step events are simply gone. No hot-standby design recovers per-step events during suspend, and no design can attribute active minutes across it. We must say so in the UI/docs and in the council verdict, or we are selling the same lie.

**R6:** Then the claim becomes: never lose the *day's total* if any registered cumulative source or Health Connect has evidence; never crash; never silently degrade; recover at the next legal start. That is defensible and testable.

**R3:** Power conditions for P2: detector+counter registration always; accelerometer probe max 8 s at `SENSOR_DELAY_GAME` only after 3 minutes of total measured silence, then exponential backoff to 10 minutes; continuous accelerometer only when no counter and no detector; HC poll 180 s normally, 60 s when measured sources are silent; no wakelock, no unlimited sensor batching.

**R2:** Play conditions: add `READ_HEALTH_DATA_IN_BACKGROUND` to the manifest with the existing Health Connect declarations; update the Play data-safety/health form when the next build is uploaded; keep `foregroundServiceType="health"` and `FOREGROUND_SERVICE_HEALTH`; do not loosen the `ACTIVITY_RECOGNITION` start gate (it is the FGS health prerequisite) without a device test proving the HC read permission qualifies.

**R4:** Crash containment conditions: wrap every sensor callback, every service lifecycle entry, the notification builder, the HC poll and the writer in `runCatching` of `Exception` (not `Throwable`); keep the process alive on a bad sample; surface a storage fault flag; flush synchronously only in `onDestroy` with a short timeout, never on the sensor path. The solver's detector race (cadence read from Default while written from Main) is fixed by confining cadence to the writer or locking it.

**R5:** Numeric spec: per-source ledgers; fused total monotonic per day; confirmed = max(counter, detector, hc, prior confirmed); estimate accumulator starts at the current fused total and only grows; displayed estimate = total − confirmed; counter reset detected by a negative delta and rebases without adding; counter deltas clamped against monotonic sensor time; detector delta ∈ {0,1} per event with a rate guard; HC never lowers; new day resets ledgers and drops the unattributable cross-midnight delta.

**R1:** Registration recovery conditions: retry any failed `registerListener` every 30 s with backoff; re-check availability on `onStartCommand`, on boot, on package replace and on the Settings permission callback; Health Connect readiness must be refreshed in `onStartCommand` (the current code computes it once in `onCreate` — that is the reason a grant made after the service starts is invisible until the process dies).

**R6:** Diagnostics must expose: per-source last-event age, current fused source, estimate marker, switches, storage fault, HC readiness. The user asked "tell me what I missed"; this is the list of things the current build hides.

**R3:** Battery acceptance: < 2%/day added by the whole layer on a mid-range phone, measured with the counter+detector registration and HC polling only; probes quantified separately. If a real device shows more, the estimate path is the first suspect.

**R4:** Step Zero before implementation: on a real counter+detector phone, walk 300 steps; assert fused total within 2% of the counter and detector total inside 90–110% of the counter. If the detector fails that, P2's max fusion must demote the detector to liveness-only. That is the falsification test; without it the whole design is a hypothesis.

**R2:** Something must fall. The council explicitly kills: (1) the exclusive-source ladder and its "rebase, discard catch-up" semantics; (2) wakelocks/wake-up-sensor guarantees; (3) WorkManager counting; (4) global exception swallowing + AlarmManager self-restart; (5) "silence means the user is idle"; (6) the belief that HC is a live sensor. What survives is P2 with amendments A1–A7.

## Verdict table

| # | Proposal | `illa` (effective cause) | Precedent | Verdict | Required amendment |
|---|---|---|---|---|---|
| P1 | Exclusive ladder + shorter watchdog | One source owns the count, so switch must rebase | Old pedometers; current code | **Falls** | — |
| P2 | Hot-standby manifold, max fusion, estimate gap marker | Independent sources measuring the same physical steps; cumulative counter heals gaps | Dual-sensor cross-check in Android pedometer practice; HC floor already shipped as v1.1 | **Survives, amended** | A1–A7 below |
| P3 | Health Connect primary | HC provides an absolute floor, not live evidence | HC docs: batched, permissioned, absent <=13 | **Falls as primary; survives as always-on floor** | A5 |
| P4 | Wakelock/wake-up guarantee | Keeping CPU awake does not make a non-wake-up sensor deliver | No wake-up step-counter guarantee; Play/battery | **Falls** | — |
| P5 | WorkManager counts | A deferred 15-minute job is not a sensor | WorkManager docs | **Falls; survives as HC reconcile + recovery** | — |
| P6 | Global catch-all + Alarm restart | Swallowing errors keeps a broken process alive; BFSL blocks alarm-started FGS | Android 12+ BFSL; force-stop semantics | **Falls** | — |
| A1 | Detector sanity + rate guard | One event = one step only if the HAL says so | j4velin overcount reports | **Accepted** | reject >10/event, >5 events/s |
| A2 | Estimate = gap filler, marker derived | Estimated portion is exactly the unconfirmed remainder | Provenance rules from v1.1 | **Accepted** | marker = total − confirmed; may shrink |
| A3 | Persistence failures visible | A silent failed write is data loss | SRE invariant | **Accepted** | degraded status + Diagnostics row |
| A4 | HC readiness refreshed + timeout | Permissions change while the process lives | Audit #1/#2 | **Accepted** | refresh on start/permission epoch; 8 s read timeout |
| A5 | Background HC permission scoped | Play treats it as sensitive | HC docs | **Accepted** | manifest + request only if feature available |
| A6 | Watchdog cannot self-disable | in-flight must mean a probe exists | Audit #3 | **Accepted** | set after scheduling; clear on no-op; detector feeds liveness |
| A7 | Honest limits | Platform cannot attribute gaps or survive force-stop | Research §5 | **Accepted** | UI/docs state it; recovery notification for pause |

## Maestro ruling

**Verdict: build P2 with A1–A7. Do not build P1, P3-primary, P4, P5 or P6. The promise we ship is "the day's total survives every legal restart and every source failure we can observe", not "zero failures on every device".**

### Step Zero — falsification test (run before implementation is accepted)
On a physical phone with both sensors, in a 5-minute session:
1. Walk 300 steps. Assert `|fused − counter| ≤ 2%` and `0.90 × counter ≤ detectorToday ≤ 1.10 × counter`.
2. Register a fake failure: unregister the counter (debug action or `adb shell dumpsys sensorservice`), walk 100 steps, assert the detector/fused total advances ≥ 90 and the source shown is DETECTOR.
3. Kill the process mid-walk (`adb shell am crash` / force-stop), walk 50 steps, reopen; assert the counter catch-up raises the total by ≥ 45 within one write cycle (≤ 5 s after reopen).
4. If step 1 fails the detector tolerance, demote detector to liveness-only in the fusor and re-run; if step 3 fails, the persistence order is wrong and P2 is not shippable.

### Concrete spec (implementable)
**State (persisted atomically per write):** `day`, `total`, `confirmed`, `counterToday`, `counterLastRaw`, `counterLastEventNanos`, `detectorToday`, `hcToday`, `estimateToday`, `initialized`, `updatedAt`.
**Fusor:** `confirmed = max(confirmed, counterToday, detectorToday, hcToday)`; `total = max(total, confirmed, estimateToday)`; `estimated = total − confirmed` (derived, never accumulated). Monotonic within a day; new day resets ledgers and drops the cross-midnight delta.
**Counter:** deltas from `raw − lastRaw`; negative delta ≤ −1 ⇒ reboot/sensor reset, rebase without adding; clamp against `(event.timestamp − lastEventNanos)` at 6 steps/s + 5 grace; ignore out-of-order timestamps; always count catch-up after any silence.
**Detector:** each event adds 1 (0 if value invalid); rate guard 5/s; totals are evidence only when the counter is not ahead (max rule) — the Step Zero test decides final trust.
**Estimator:** starts with baseline = current total; adds only detected steps; only active when no counter/detector event has been seen for 3 min *and* an 8 s probe is walking, or continuously when the phone has no counter and no detector.
**HC:** poll only when `hasPermission`; 180 s normal / 60 s when measured sources are silent; 8 s timeout; apply as floor; never lower.
**Recovery:** retry registration every 30 s with backoff; refresh HC readiness in `onStartCommand` and on permission epoch; boot/package-replace/app-open restart; ReconcileWorker performs HC read when permitted and posts at most one "counting paused — tap to resume" notification per 12 h when the heartbeat is stale.
**Crash containment:** `runCatching {}` around sensor callbacks, service lifecycle, writer, HC poll, notification builder; catch `Exception`, never `Throwable`; bounded flush only in `onDestroy`; storage failure sets a visible fault flag.
**Power:** no wakelock; no continuous accelerometer on counter/detector phones; probe ≤ 8 s, backoff 1 → 2 → 4 → 8 → 10 min; HC polls as above.

### Metrics (acceptance)
- Daily total divergence vs. the hardware counter: ≤ 1% on a 7-day field log; 0 negative days; 0 double-counted reboots in unit tests.
- Kill/restart recovery: catch-up visible within 5 s of process restart; no lost day.
- Health Connect grant made after service start takes effect within 60 s without a restart.
- Writer latency p95 ≤ 50 ms; battery addition ≤ 2%/day (Battery Historian); 0 crashes attributable to the counting layer over 72 h.
- UI truth: every estimated day shows the marker; every stale source shows last-event age; storage fault and paused states are visible.

### Risks (residual, stated honestly)
1. Detector overcount if a HAL emits duplicate events — bounded by A1, falsified by Step Zero; demotion path ready.
2. Health Connect floor can exceed phone-only steps when a watch also syncs — max is the agreed truth policy; UI provenance separates synced from measured.
3. Non-wake-up sensors lose detector events and active-minute attribution during AP suspend; counter aggregate heals the total only.
4. OEM force-stop/deep-clean kills all recovery until boot/app-open/user tap; only the recovery notification is legal.
5. Extra Play review for the sensitive background health grant; if denied, HC degrades to foreground polling and the app still counts from sensors.
6. Android <= 13 emulator/device HC may be absent; Step Zero must run on a physical phone, not the emulator.

### First action
Write the failing unit tests for the fusor (reboot, reboot mid-day, out-of-order, detector duplicate guard, HC floor, estimate marker shrink, midnight rollover, process-death catch-up) and run Step Zero items 1–3 on the available physical device if one is attached; if only the emulator is available, run items 2–3 with the estimate path and record the sensor limitation as evidence. Then implement the state layer before the service.
