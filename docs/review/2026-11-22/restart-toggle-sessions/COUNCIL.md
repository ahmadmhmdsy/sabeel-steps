# Council — "keep counting, tap the ring, custom sessions"

Date: 2026-11-22 · Subject: the owner's v1.2 field report and the requested counting control / custom sessions · Language: English
Evidence base: `RESEARCH.md` in this folder; the read-only role reviews of `core-logic`, `android-platform`, `ui-ux-a11y`, `product-legal`, `build-security` and `market-strategist`; the tree at `5a44290`; the 2026-11-21 council and plan.
Mode: Qiyas mode C (merged adversarial council + analogical reasoning), 6 core roles + 2 dynamic roles.

## Method

Every proposal is tested with Qiyas: name the precedent (`al-far'`), the origin rule (`al-asl`), the effective cause that makes the precedent work (`al-'illa`), then verify that cause in our case and refine the conditions (`tanqih`). The council is hostile by construction: every role must name at least one fatal flaw, and at least one proposal must fall. The Maestro does not average opinions; it issues an implementable ruling and one falsification test that must pass before the claim is made.

## Role cards

| Role | Identity | Optimises | Hard constraint |
|---|---|---|---|
| R1 | **Rashid** — sensor HAL & device-fleet engineer | Count survives every HAL and OEM behaviour | Registration and legal start are the only proof of counting |
| R2 | **Nadia** — Android platform & Play policy counsel | Reviewable, truthful, no rejection | No absolute marketing claims; no restricted permissions without need |
| R3 | **Tariq** — power & thermal engineer | 24 h battery budget | No wakelock, no always-on high-rate sensor, no poll storm |
| R4 | **Mona** — on-device reliability/SRE | No crash, no silent data loss, fast recovery | Every callback is an entry point; every swallowed failure is a future lie |
| R5 | **Yusuf** — numerics & provenance auditor | One number that is real | Monotonic day, no double count, provenance marker always truthful |
| R6 | **Huda** — field product owner | User trust | The UI must never claim more than the data supports |
| R7 | **Salma** — OEM field support (dynamic) | The owner's phone actually recovers | No fix is "done" without device evidence |
| R8 | **Omar** — interaction & accessibility (dynamic) | One-handed, RTL, TalkBack-correct control | The state must be visible and reversible; the ring cannot be the only control |

## Proposals on the table

- **P1 — Literal "always counting until I turn it off":** no user switch; the service restarts after every reboot and never stops except force-stop.
- **P2 — Persisted intent + gated boot + diagnostics:** `countingEnabled` is the single authority; every entry point starts only when it is true; boot attempts and refusal reasons are recorded.
- **P3 — Ring as the sole toggle:** tap the ring to stop/start; no other visible control.
- **P4 — DataStore-only flag:** store the flag in the existing Settings DataStore and read it in the boot receiver.
- **P5 — Resume by reusing the existing counter catch-up:** on re-enable, feed the first counter sample through `onCounter` and let the pipeline count the gap.
- **P6 — Keep the Health Connect floor live while paused:** HC is a floor, so leave it running; the day total stays "complete".
- **P7 — Session as a second counter:** the session counts its own steps and adds them to the daily total.
- **P8 — Auto-resume a GPS session at boot:** start the walk service from `BOOT_COMPLETED`.
- **P9 — Place/end-place by address search:** use geocoding or a map SDK for destinations.
- **P10 — Direct boot + battery-optimization exemption:** `LOCKED_BOOT_COMPLETED` and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` to maximise uptime.
- **P11 — Custom session setup page with goals:** walk/run, optional GPS, time/steps/distance targets, saved sessions; Home keeps the global total.
- **P12 — A day the user turned counting off is an automatic rest day** (streak pauses, not breaks).
- **P13 — A session started while counting is off counts only itself:** session-only sensors, Home total stays frozen.

## Transcript (condensed, adversarial)

**R1 (Rashid):** Start with the platform truth. The v1.2 boot path is legal on stock Android 14/15/16: `BOOT_COMPLETED` is an FGS-start exemption, the Android 15 restricted list excludes `health`, and a health service whose prerequisite is `ACTIVITY_RECOGNITION` is not blocked by the while-in-use rule. So the owner's failure is not "Android forbids it". It is one of five things: OEM autostart blocked, force-stop, a silent receiver no-op, a start-then-die, or an unhonoured sticky restart. P1 is dead: no app can promise a restart after force-stop, and no app can open its UI from the background. The strongest legal path is P2 plus a boot-attempt record. Direct boot (P10) is unnecessary complexity: the counter needs the credential-encrypted DataStore and the user has to unlock anyway; adding a second storage domain for a few seconds of pre-unlock counting is not worth the migration risk.

**R4 (Mona):** P2 is necessary but not sufficient. The current receiver does a cold DataStore read inside its `goAsync()` budget and ignores the boolean from `ensureStarted`; a slow boot can turn the whole path into a silent no-op. The flag must be readable synchronously in the receiver, and the attempt must be written before the start, with a result and an exception class after it. `START_STICKY` must re-check the flag; otherwise an OFF user gets counting resurrected by a null-intent restart. The OFF state must be a distinct status (`OFF_BY_USER`), not `STOPPED`, or Home will show "counting stopped — start" to a user who deliberately stopped it.

**R2 (Nadia):** P3 is a policy and a truth problem. A progress ring is a data display, not a control; making it the only toggle hides the state and makes an accidental multi-day stop likely. The owner asked for the gesture, and he can have it, but there must be a visible pill and a Settings switch with the same state, and the copy must never say "guaranteed". P1's wording cannot ship. The listing and Settings must say: Sabeel asks Android to keep counting; Android or the phone maker can still stop it after a force-stop, on some restarts, or under strict battery saving. P10's battery exemption is a restricted permission with Play risk; do not request it. Keep the OEM guide and the system settings links instead. Location stays foreground-only; no `ACCESS_BACKGROUND_LOCATION`, no Geofencing API.

**R3 (Tariq):** P1 would also be a battery lie: an all-day health FGS with counter+detector is already the cost we accepted; keeping it alive while "off" would pay that cost for nothing. Pause must actually unregister the sensors, stop the HC poll, stop the cadence and stop the service. The cost of the remaining design is unchanged from v1.2; the new session FGS is foreground-only and bounded by the session, so it is acceptable. Measure the owner's phone before/after; a persistent FGS that the OEM kills is not a battery problem, it is a reliability problem.

**R5 (Yusuf):** P5 and P6 both fail arithmetic. On re-enable, `onCounter` with an old `lastRawAt` accepts the full catch-up because the timestamp path is permissive when the event gap is unknown (`StepPipeline.kt:160-169`). The paused gap would be added as if it were measured. The fix is the same rule as a fresh install: set `counterLastRaw = NaN` on pause, so the first sample after resume only rebases (`StepPipeline.kt:130-135`). Detector and estimate accumulators are unregistered while paused; reset them on resume. P6 is worse: HC is an absolute daily total, so a live floor retroactively adds every paused step (`StepPipeline.kt:230-246`). Either capture the HC day total at pause and subtract it on resume, or suppress HC until the next local midnight. P7 is a category error: the session and the day measure the same physical steps; the session must be a projection of the global delta, never an extra stream.

**R6 (Huda):** The owner's intent is clear and should be honoured: he wants control, not a hidden switch. P3 as written is not that; it is a hidden switch with a big target. Give him the ring tap, but also a visible state pill, an Undo snackbar, a haptic and a Settings mirror. The OFF state must say "you stopped it", not "counting failed"; the system-stopped state must say "Android stopped it — tap to restart". P11 is the right shape, but the CTA must be renamed: "Start walking" that does not start counting is the same bug report again. "Start a session" / "ابدأ جلسة". P12 is the humane choice for streaks, but it is the owner's decision; label the day "counting was off" and never count it as goal-met. P9 is impossible offline; do not promise address search in a no-INTERNET app.

**R8 (Omar):** The ring must be a real switch semantically: `toggleable(value = countingEnabled, role = Role.Switch)`, a `stateDescription` ("counting on"/"counting off"), and an `onClickLabel` ("stop counting"/"start counting"), while the existing content description still reads the step value. The visual state cannot rely on colour: a pause glyph, a muted/dashed track, the frozen number and a caption "today's total — paused". A 230 dp target is easy to hit accidentally; make the change reversible with a 5 s Undo snackbar and a confirm haptic, not a modal. Put a pill under the ring and a Settings switch for motor accessibility and OEM recovery. On the session side: one-handed bottom primary, back gesture opens a save/discard sheet instead of silently popping, and Arabic 6-form plurals for minutes/steps.

**R7 (Salma):** The owner's phone is the only evidence that matters. The current build cannot tell him why it failed. Phase 1 must add a Diagnostics section: last 10 boot attempts (time, action, boot count, onboarding, AR grant, sensors, HC availability, enabled flag, start result, exception class, alive-after-10 s), last service heartbeat, OEM autostart state (manual checklist), battery optimization state, and a "share diagnostics" button that uses the system share sheet (no network). Then ask him for three cold boots with a 50–100 step walk after each, and the OEM autostart/battery settings. Only after that can we say the issue is fixed. Also: if the OEM autostart is off, the app cannot fix it; the Diagnostics screen must show the exact OEM path for his manufacturer.

**R1:** P8 falls. A location FGS cannot be created from the background on Android 14+ because location is while-in-use; boot does not give a visible activity. A GPS session must checkpoint and offer resume via notification/app open. A step-only session could theoretically use the health FGS, but mixing session types for this edge is not worth it. P13 falls too: if the global count is off and a session counts only itself, Home and the session disagree about the same walk, which is exactly the dishonesty the product forbids. Starting a session while off must prompt to turn counting on (one tap), and turning global off during a session must pause the session with a warning.

**R4:** P4 falls on boot latency: a cold DataStore read in a receiver is a silent-failure vector. Keep the single authority in a small synchronous SharedPreferences file (the same pattern as `StatusStore`), expose a `StateFlow` for Compose, and write it before any service action. The DataStore stays for the rest of the settings. This is one authority, not two: the sync file is the only place the flag lives.

**R2:** One more thing must fall: any claim that the app "opens itself" after reboot. It cannot. The notification can say "Sabeel is counting" and tapping it opens the app; that is the legal maximum. The Play listing must not promise automatic reopening.

**R5:** For the session model, the invariant is: `session.steps ≤ daily.steps` for every date the session touches, and session code never writes `DailyStepsDao`. A midnight-crossing session splits into legs by local date; the daily rows are untouched. The session's own measured steps may differ slightly from the day total because they start from different baselines; the UI must say "this session", not "today". CSV keeps the daily file unchanged and adds a separate, opt-in sessions export.

## Qiyas cards (key rulings)

| Ruling | `al-asl` (origin) | `al-far'` (precedent) | `al-'illa` (effective cause) | `tanqih` / conditions |
|---|---|---|---|---|
| Persisted intent is the authority | The user's decision outlives the process | Rest-day marker already persisted; onboarding flag | A service cannot infer intent from "onboarding done"; only an explicit flag survives reboot | Synchronous read in boot receiver; write-before-action; every entry point re-checks; default true only after onboarding |
| Pause rebases, never catches up | Fresh install establishes a baseline and counts nothing | First counter sample after install | The counter has no evidence that off-period steps were "measured by us"; accepting them fabricates data | `counterLastRaw = NaN`; detector/estimate reset; HC offset or suppression; midnight rollover |
| Ring is a switch with a visible twin | A control must be discoverable and reversible | Settings switches, rest-day toggle | A hidden gesture cannot be the only path to a multi-day state | `toggleable` semantics; pill; Settings mirror; haptic + Undo; distinct OFF_BY_USER status |
| Session is a projection, not a counter | The day total is the single authority | Estimate marker = total − confirmed; CSV rows are derived | Session and day measure the same physical steps; adding both double counts | Never write daily; `session ≤ day`; midnight legs; separate export |
| Place is local geometry, not an address | The product is offline by design | Canvas route sketch; saved rest days | Geocoding/routing needs a network the app refuses to request | Label + coordinate + radius; no search, no ETA, no map tiles |

## Verdict table

| # | Proposal | `al-'illa` test | Verdict | Required amendment |
|---|---|---|---|---|
| P1 | Literal absolute always-on | Android execution cannot be guaranteed | **Falls** | A1, A12 honest scope |
| P2 | Persisted intent + gated boot + diagnostics | User intent must outlive process; failure must be attributable | **Survives, amended** | A1–A6 |
| P3 | Ring as sole toggle | Hidden state is not a control | **Falls as sole control; survives as accelerator** | A6 |
| P4 | DataStore-only flag | Boot receiver cannot afford a cold read | **Falls** | A1 sync authority |
| P5 | Resume via existing catch-up | Permissive clamp would add off-period steps | **Falls** | A3/A4 rebase |
| P6 | HC floor live while paused | Absolute floor backfills paused time | **Falls** | A4 offset/suppress |
| P7 | Session as second counter | Same steps counted twice | **Falls** | A7 projection |
| P8 | GPS session auto-resume at boot | Location is while-in-use; background start throws | **Falls** | A9 checkpoint + notification |
| P9 | Address search / geocoding | No INTERNET, no map SDK | **Falls** | A10 local places only |
| P10 | Direct boot + battery exemption | Complexity and Play risk outweigh benefit | **Falls for v1.3; revisit only with device evidence** | A5, A12 |
| P11 | Custom session setup page + goals | Owner asked for it; one active session is measurable | **Survives, amended** | A7–A9 |
| P12 | User-off day = rest-like | Streak must not reward an accidental off, but must not punish a deliberate pause | **Survives as recommended default; owner decision** | A11 |
| P13 | Session-only counting while global off | Home and session would disagree | **Falls** | A8 prompt to enable |

## Amendments (binding)

- **A1 — Counting authority.** One synchronous SharedPreferences file (same pattern as `StatusStore`) holds `countingEnabled` (default true only after onboarding), `changedAt`, `pausedAt`, `pauseReason`. The service never self-enables. A `StateFlow` mirrors it for Compose; all gates read it synchronously.
- **A2 — Write before act.** Every toggle persists the flag first, then starts/stops the service; a serialized controller + Mutex collapses double taps; `START_STICKY` null-intent restart and `onCreate` re-check the flag; delete-all blocks toggles.
- **A3 — Pause.** Flush, stop cadence, unregister counter/detector/estimate, stop HC, set `counterLastRaw = NaN`, record `pausedAt`/`pauseReason`, stop FGS; status becomes `OFF_BY_USER` (user) or `STOPPED_BY_SYSTEM` (detected stale heartbeat with enabled=true).
- **A4 — Resume.** New explicit pipeline entry (not `onCounter`): baseline = current raw, delta 0; reset detector/estimate accumulators; HC uses `max(0, hcRead - hcAtPause)` for the same local day, or is suppressed until midnight if no pause baseline exists; active-minute cadence restarts at resume.
- **A5 — Boot hardening and evidence.** Keep `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`; read the sync flag; record attempt before start and result/exception after; 5–10 s self-check; one bounded retry in the grace window; if the service is not alive, post a recovery notification (activity PendingIntent, no sound). No `LOCKED_BOOT_COMPLETED` in v1.3.
- **A6 — Ring control.** `ProgressRing` gains an optional toggle contract: `toggleable(value, role = Role.Switch)`, `stateDescription`, `onClickLabel`; content description still reports steps. Visible pill under the ring and a Settings switch mirror the same state; haptic + 5 s Undo snackbar; no confirm dialog. OFF is visually distinct without colour alone.
- **A7 — Session model.** One active session at a time; `walks` extended with activity type, goal type/value, target-met, status, place refs; a `session_presets` table for saved setups; `walk_legs` for midnight splits; Room v3 + explicit `MIGRATION_2_3`; session steps are a projection of the global day delta and never write `DailyStepsDao`; `session.steps ≤ daily.steps` per date.
- **A8 — Session/counting interaction.** Starting a session while counting is off prompts "Counting is off — turn it on to measure this session?" and turns it on on confirm. Turning counting off during a session pauses the session with a warning (or requires stopping the session first). No session-only hidden counting.
- **A9 — GPS and reboot.** Session FGS type `location` (plus `health` if it reads sensors directly); foreground-only permission; checkpoint every 20 s (versioned JSON, tolerant v1 parse); on process death/reboot, offer "session interrupted — resume" notification/app-open card; never start a location FGS from boot.
- **A10 — Places.** A place is a locally saved label + coordinate + radius, chosen from current location or a saved point. "End at place" auto-stops when the user enters the radius (one notification). No address search, geocoding, routing, map tiles or ETA.
- **A11 — Streak and history.** User-off days are labelled "counting was off", excluded from goal-hit rate/median and from streak advancement (rest-like, but not silently a rest day); system-stopped days are "no data" and never rest. Owner decision; align goal advisor, CSV and docs.
- **A12 — Truth and Play.** Honest copy EN/AR/listing: "Sabeel asks Android to keep counting; Android or your phone maker can still stop it." No absolute claims; no battery-optimization exemption; no background location; HC read-only; FGS health declaration + demo video; Data safety on-device rationale; no INTERNET.
- **A13 — Diagnostics.** Persist and show boot attempts (10), last heartbeat/source/refusal, OEM autostart checklist, battery-optimization state, permission states, session checkpoint state; "share diagnostics" via system share sheet, redacting coordinates; "restart counting" test action.
- **A14 — Migration and rollout.** Room v2→v3 with `MigrationTestHelper`; checkpoint v1→v2 tolerant parse; versionCode 3 / versionName 1.3.0; staged rollout; real-phone evidence required before the reboot claim.
- **A15 — Tests.** Toggle idempotency/concurrency; pause same-day and across midnight with zero backfill; reboot-while-off starts nothing; HC offset/suppression; session ≤ day; midnight legs; migration with real rows; Compose ring semantics; emulator reboot/toggle; physical-device reboot matrix.

## Maestro ruling

The owner asked for three things. The council grants two as asked and amends the first:

1. **"Keep counting until I turn it off" is granted as an intent, not as a platform guarantee.** Sabeel will persist the intent, re-arm on every legal entry point (boot, package replace, app open, notification tap, sticky restart), instrument every attempt, and tell the truth when Android or the OEM stops it. It will not claim to survive force-stop or to reopen its own UI; those are impossible.
2. **"Tap the ring to toggle" is granted**, with a visible pill, a Settings mirror, switch semantics for TalkBack, a haptic and an Undo snackbar. The ring is the fast path, not the only path.
3. **"The CTA opens custom sessions" is granted** as a dedicated setup page: walk/run, optional GPS, time/steps/distance goals, one active session, saved sessions, history; Home keeps the global day total. Places are local saved coordinates with a radius, not address search. A session cannot run while global counting is off without turning it on; a session cannot be resumed automatically after reboot if it uses GPS.

**What falls:** absolute always-on; ring-only toggle; DataStore-only flag; catch-up resume; live HC floor while paused; session as a second counter; GPS auto-resume at boot; geocoded places; direct boot + battery exemption for v1.3; session-only counting while global is off.

**Step Zero (before any claim of a fix):** on the owner's real phone, capture the current v1.2 reboot failure — OEM/model/Android, notification presence, autostart and battery settings, `adb logcat` for `ForegroundServiceStartNotAllowedException`, `dumpsys activity services com.sabeel.steps`, `dumpsys package com.sabeel.steps | grep stopped`. Then, after Phase 1, repeat three cold boots and a 50–100 step walk after each. If adb is unavailable, the Phase 1 diagnostics build must produce a shareable report. Without this evidence the fix is a hypothesis, not a result.

**Falsification test (must pass before v1.3 sign-off):**
- Reboot 3× with counting ON: counting resumes, post-boot steps counted, no double count, attempt visible in Diagnostics.
- Turn counting OFF, walk 500 steps, turn ON: the day total increases by only the post-ON steps; HC does not backfill.
- Reboot with counting OFF: stays OFF, no FGS, no notification.
- Start a session, walk, finish: session steps ≤ day steps; Home unchanged except the normal global count; no duplicate daily row.
- Upgrade a real v1.2 install to v1.3: no crash, no data loss, migration test green.
- Rapid 10 taps: persisted state matches the last tap; no zombie service or notification.

## Owner decisions still open

1. Phone model/OEM/Android version, and whether adb is available for the Step Zero log.
2. Streak rule for a user-off day: rest-like exclusion (recommended) vs break the streak.
3. Starting a session while counting is off: auto-enable on confirm (recommended) vs block.
4. Places: saved coordinate + radius (recommended, offline) vs address search (requires network/map; conflicts with the no-INTERNET promise).
5. Goal completion: notify only (recommended) vs auto-stop.
6. One active session at a time (recommended; multiple concurrent cannot be measured separately) vs queued/simultaneous.
7. Turning global counting off during an active session: pause the session (recommended) vs stop it vs keep it.
8. CTA naming: "Start a session" / "ابدأ جلسة" (recommended) vs keep "Start walking".
9. Whether to ship a Phase 1 diagnostics-only build first to capture the reboot failure, or go straight to the full v1.3.
