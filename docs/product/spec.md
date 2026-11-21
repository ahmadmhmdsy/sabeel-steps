# Product spec v1 — Sabeel (working name)

Read docs/product/brief.md first. This file is the build contract: if code and this file disagree,
fix one of them explicitly. Status: v1.1, 2026-11-21 (owner-approved read-only Health Connect tier,
persisted estimate markers). v1.0 was 2026-11-16.

## 1. Data model (Room, versions start at 1)

### DailySteps (table daily_steps)
| column | type | notes |
|---|---|---|
| date | TEXT PK | local date 'yyyy-MM-dd' (ISO, device zone) |
| steps | INTEGER | total steps that local day |
| distanceMeters | REAL | estimated |
| calories | REAL | estimated kcal |
| activeMinutes | INTEGER | minutes with cadence >= 60 spm, best-effort |
| updatedAt | INTEGER | epoch millis |
| estimatedSteps | INTEGER NOT NULL DEFAULT 0 | how many of `steps` came from the labelled estimate; 0..steps, monotonic within a day, exactly 0 on measured/synced sources |

### Walk (table walks)
| column | type | notes |
|---|---|---|
| id | INTEGER PK autogen | |
| startedAt / endedAt | INTEGER | epoch millis |
| durationSec | INTEGER | active duration |
| steps | INTEGER | step delta during walk (0 if sensor unavailable) |
| distanceMeters | REAL | GPS polyline length when available, else stride estimate |
| avgPaceSecPerKm | INTEGER | derived, 0 if < 50 m |
| routeJson | TEXT | JSON array [{"lat":..,"lng":..,"t":millis}, ...], empty when no GPS |
| estimated | INTEGER NOT NULL DEFAULT 0 | 1 when any step counted during the walk came from the estimate |

CSV export/import (SAF, round-trip safe): header `date,steps,distance_meters,calories,active_minutes,estimated_steps`;
import also accepts the legacy 5-column header/rows (estimated_steps = 0). `estimated_steps` must be
0..steps; a row that violates that is skipped, not silently repaired.

Preferences (DataStore): goalSteps (default 8000), units ('metric'|'imperial'), heightCm (default 170),
weightKg (default 70), gender ('unspecified'|'male'|'female'), strideCm (0 = auto), language ('en'|'ar'),
reminderEnabled (false), reminderMinutes (e.g. 1140 = 19:00), onboardingDone (false), notification
timestamps, and the manifold step state (`step_state_schema=2`; see §3 rule 11). v1.1 keys are read
once for migration, then removed.

## 2. Algorithms (pure Kotlin in domain/StepMath.kt — unit tested)

Stride (cm): if strideCm > 0 use it; else heightCm * (gender == female ? 0.413 : 0.415); clamp 40..110 cm.
Distance (m) = steps * strideCm / 100.
Calories (kcal) = steps * 0.04 * (weightKg / 70.0), rounded to 1 decimal. Label "estimate".
Active minutes: count minutes in which cadence >= 60 steps/min; service keeps a 60-slot ring of
(currentMinuteKey, stepsThisMinute); flush finished minutes into today's counter. Approximate but stable.
Pace: durationSec / (distanceMeters / 1000.0), only if distanceMeters >= 50; formatted m:ss /km.
BMI (optional display only): weight / (height/100)^2, one decimal, never a diagnosis.

## 3. Step sources and the manifold pipeline (correctness is a feature)

The v1.2 pipeline is a **manifold**, not an exclusive ladder: every available source accumulates in
parallel into one fused day, so losing one does not reset or freeze the others. This section is the
source of truth for the counting rules.

1. Sources, in order of evidentiary strength for display only:
   - `TYPE_STEP_COUNTER` — measured, cumulative since boot.
   - `TYPE_STEP_DETECTOR` — measured, one event per step.
   - Health Connect — synced read-only daily total from another app/watch (opt-in, owner-approved
     2026-11-21).
   - Accelerometer estimate — always labelled "estimated".
   All of them are registered/listened to at the same time when present; no tier is "chosen" and no
   source switch rebases anything. The strongest contributor is named only for display
   (Diagnostics/status), not used to select a writing source.
2. Counter adapter: cumulative since boot, event-driven at `SENSOR_DELAY_NORMAL`, never batched and
   never unregistered while the service lives. On boot/process death the first raw after restart is
   compared with the persisted last raw; the monotonic event timestamp gives the elapsed sensor time
   so the full catch-up delta is accepted (this is what recovers steps taken while the process or the
   whole app was dead).
3. Detector adapter: one event per step; registered for the whole day alongside the counter. Each
   event adds one step to its own ledger. A single value outside 1..10, or more than 5 events per
   second, is rejected as sensor noise (sanity guard, not a rate limiter).
4. Estimate adapter: accelerometer at `SENSOR_DELAY_UI` -> MotionStepDetector (time-based EMA
   coefficients; 1.2 m/s^2 threshold with 0.15 hysteresis; 250-2000 ms refractory; 3-peak burst;
   2.5 s stop). It is **only a gap-filler**: its baseline is the current fused total on the first
   estimated step, so it can add only what no measured/synced source has already confirmed. It is
   always marked: "≈" at the number, a note in Home, in Diagnostics, in the walk live/summary, in the
   foreground notification and in the goal notification. Estimated steps count toward the daily
   goal/streak while marked (reversible owner decision).
5. Health Connect adapter (read-only floor): polls the provider's today total within seconds of a
   usable service, then every 3 min, and every 1 min while the measured sensors are silent. The read
   is bounded by an 8 s timeout. The provider total is absolute, so it is applied as
   `confirmed = max(confirmed, hcToday, counterToday, detectorToday)` and never added; repeated or
   lower reads change nothing. HC is never written. A background read needs
   `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` (granted through the Health Connect
   permission screen; requested only when the device's Health Connect feature reports it available);
   without it the read is skipped. Health Connect has no step timestamps, so it contributes no active
   minutes. The HC permission is offered in Settings only on phones that report neither a counter nor
   a detector (`SourceAvailability.noMeasuredStepSensor`); once granted it is polled as a floor even
   if a sensor later appears.
6. Fusion, after every accepted event:
   ```
   confirmed = max(confirmed, counterToday, detectorToday, hcToday)
   total     = max(total, confirmed, estimateToday)
   estimated = total - confirmed        // derived, monotonic non-increasing for a fixed confirmed set
   ```
   Consequences: the detector does not double count the counter (both measure the same steps; the
   larger ledger wins), a synced HC total never adds to the phone's sensor total, and a late HC sync
   only shrinks the "≈" portion. `todayBase` no longer exists; the marker is never persisted
   separately from the ledgers.
7. Plausibility clamp (counter only): a single accepted delta cannot exceed
   `floor(elapsed_seconds * 6) + 5`, where elapsed comes from the sensor's own monotonic timestamp.
   It is skipped when the previous timestamp is unknown or when the delta follows a reboot. A
   counter that fell by more than 1 (`raw < lastRaw - 1`) means reboot/sensor reset: rebase to the new
   raw and add nothing. This is what keeps a reboot or HAL glitch from producing a phantom day.
8. Silence watchdog (pure state machine): while both measured ledgers have been silent for 3 min, an
   8 s accelerometer probe (max one per minute, exponential backoff up to 10 min) checks for walking.
   If the estimate is not already active, two consecutive walking probes start the continuous
   estimate; a non-walking probe resets the streak and backs off. Any real counter or detector event
   stops the estimate immediately. Because the counter/detector registration is never removed,
   continued walking is also noticed by those sensors. A failed probe can never disable the watchdog.
9. Process death / restart: the persisted manifold state is the recovery point. On restart the
   service seeds a fresh day (or preserves yesterday until the first sample rolls it over), reads
   row-vs-state max so a crash between the two writes cannot lose steps, and re-registers both
   measured sensors. Counter catch-up is recovered exactly by rule 2; detector events that occurred
   while the process was dead cannot be recovered from the sensor (documented limit), and HC can fill
   the gap when granted.
10. Day rollover: at the first event/pub after local midnight, all ledgers reset to zero, the day
    changes, the estimate detector is reset and any cross-midnight delta is dropped (unattributable).
    Timezone changes use the same local-date rule; History keys stay local dates.
11. State: persisted atomically in DataStore after every meaningful change
    (`step_state_schema=2`, day, total, confirmed, counterToday, counterLastRaw,
    counterLastEventNanos, detectorToday, detectorLastEventNanos, hcToday, estimateToday, initialized,
    updatedAt). The Room daily row is upserted as part of the same coalesced write (at most about
    once per second while events arrive) and reconciled on service start/worker runs. v1.1 keys are
    migrated on first read: the old total becomes confirmed/counterToday, the old raw baseline is kept
    and the old persisted estimate marker is preserved through the daily row
    (`StepPipeline.preserveLegacyMarker`) so an upgrade does not relabel estimated steps as measured.
12. Resilience of the writing path: the service coalesces sensor callbacks into one snapshot per
    second; a write failure is marked and shown in Diagnostics instead of being swallowed; a crash in
    a callback/lifecycle/poll is contained; the service never crash-loops. Storage failure or process
    death cannot create phantom steps (row/state max) and cannot make the day negative.
13. Status/provenance: Diagnostics shows the current display source, last event age, switch count,
    Health Connect readiness, estimate activity and storage-write health. A stale source is labelled
    stale after 2 min; a stopped/unavailable count is shown honestly (NO_SENSOR/NO_PERMISSION instead
    of a fake zero). Notifications follow provenance and label estimated steps.
14. No continuous sensor polling beyond the registered sensors, no AlarmManager exact alarms, no
    wakelocks. The polling loops that exist are the opt-in Health Connect read (above) and the
    bounded watchdog probe; both stop with the foreground service.

## 4. Services

StepCounterService: foreground service, type health; persistent low-priority notification
("Sabeel is counting your steps", tap opens app, no sound). START_STICKY. Stops only if user
disables counting in settings (v1: always on once onboarding completes).
WalkTrackingService: foreground service, type location while tracking; requests fine location at
runtime from the Walk screen; collects LocationManager updates (no Play Services dependency) every
~3 s or 5 m; computes GPS distance; persists an in-progress walk checkpoint every 20 s so a process
death does not lose the route; notification with Stop action. If permission denied, tracking runs
sensor/step-only and says so in the UI. Walk steps use the same ladder as rule 1: counter delta,
detector event, or the labelled accelerometer estimate when neither measured sensor exists.

## 5. Screens & navigation (Compose, Material3, Navigation-Compose)

Routes: onboarding (when !onboardingDone), home, history, walk, insights, settings, privacy, about.
- Onboarding (3 steps, skippable height/weight): welcome/privacy promise -> goal -> units + body data.
- Home: today ring (steps/goal), 3 stat cards (distance, kcal, active min), streak chip, gentle status
  line ("A short walk after lunch keeps the streak alive"), primary CTA "Start walk", last-7-days mini
  bars. Pull-to-refresh not needed; state from Flow.
- History: range toggle 7/30 days, bar chart with goal line, list of day rows (date, steps, distance,
  "≈ estimated" badge when part of the day came from the estimate), saved walks (same badge per walk).
- Walk: idle state (permission explainer) -> live (timer, distance, pace, steps, our own Canvas route
  trace) -> summary (save/discard, stats, route thumbnail). No map tiles, no API key, no network.
- Insights: 7-day average, best day, goal-hit rate, total distance, streak; one plain-language
  sentence; all numbers derived, no invented coaching.
- Settings: goal stepper, units toggle, reminder switch + time, height/weight/gender, language switch
  (en/ar), export AND import CSV (SAF CreateDocument/OpenDocument, round-trip safe — council add: import
  is the mitigation for "no cloud backup"), Health Connect opt-in card (only on phones with no counter
  and no detector: connect/manage the read-only `READ_STEPS` grant), delete all data (confirm dialog,
  clears DB + prefs), privacy policy, About (version, licenses note).
- Privacy screen: plain-language offline statement, permissions list and why, no-ads statement.

## 6. Accessibility & i18n acceptance criteria
- Every icon button has contentDescription; charts have a text summary; ring exposes percentage.
- Touch targets >= 48dp; contrast >= 4.5:1 body text; supports fontScale 2.0 without clipping.
- All user-visible strings in res/values/strings*.xml and res/values-ar/strings*.xml; no hardcoded
  English in composables. RTL: use start/end padding, mirror arrows; test Arabic layout.
- Language switch in-app via AppCompatDelegate.setApplicationLocales (androidx.appcompat) or a
  CompositionLocal + saved locale; document the approach.

## 7. Design system (original; do not imitate competitors)
Palette "Desert Dawn": primary teal #0F6B5C, onPrimary #FFFFFF, secondary clay #B4653A,
tertiary sand #D9A441, light background #FAF7F2, light surface #FFFFFF, dark background #101817,
dark surface #18211F, error #B3261E. Ring gradient teal -> mint #4FD1B5. Charts: teal bars, sand goal line.
Type: system sans (Roboto) with Material3 scale; Arabic uses system font. No downloaded fonts in v1.
Shape: 20dp cards, 28dp large containers, pill buttons. Motion: subtle (ring animates on value change,
screen fade/slide), respect 'remove animations' setting.
Icon: original adaptive icon — sand background, teal abstract stride arc + dot.

## 7b. Privacy hardening (Security/Privacy Auditor requirements)
- android:allowBackup="false" + dataExtractionRules/fullBackupContent excluding everything: default
  Google auto-backup would upload the DB to the user's Drive and falsify the "stays on phone" claim.
- No INTERNET permission in the merged manifest; no crash SDK, no analytics, no ad ID.
- No clipboard writes of health data; export touches the filesystem only on explicit user tap.
- Edge-to-edge is enforced by targetSdk 36: every screen consumes WindowInsets; no content under bars.
- Widget is deliberately deferred to v1.1 (fast follow). v1 trigger = daily reminder + goal notification.
  Glance dependency must NOT be added in v1.

## 7c. Distribution prerequisites (developer actions, outside the build)
- Play Console: $25 one-time registration fee + identity verification. Country/payment support for
  publishing from Iraq must be confirmed before buying store assets (operational risk, not a build task).
- New personal accounts: closed testing with 12+ testers for 14 continuous days before production access
  (verify current policy; plan the launch calendar around it).
- A hosted privacy policy URL is required in the listing even for a no-collection app; we ship the text
  and an in-app screen, the developer hosts the page (e.g. GitHub Pages).
- Closed testers install the internal-testing build; they collect the Step Zero metrics below.

## 8. Definition of done (QA checklist)
[ ] gradle assembleDebug BUILD SUCCESSFUL from a clean checkout on this machine
[ ] gradle testDebugUnitTest green (StepMath, date rollover, aggregation, CSV escaping)
[ ] lintDebug has no Error-severity issue introduced by us
[ ] Fresh install -> onboarding -> home shows 0 steps (or sensor baseline) -> sensor events update ring
[ ] Kill app process -> reopen -> today's steps not lost (persisted state)
[ ] Simulate reboot state (raw < baseline) -> no negative/lost counts (unit test with fake sensor)
[ ] Walk: permission denied path works; grant path records route points and saves a walk
[ ] Reminder + goal notification fire (WorkManager; POST_NOTIFICATIONS on 33+)
[ ] Arabic locale: all screens translated, RTL layout correct, no clipped text
[ ] Export CSV opens SAF and writes rows; delete-all-data empties DB and resets prefs
[ ] Adaptive icon + app name render; launcher icon not the default Android robot
[ ] No INTERNET permission in merged manifest (verify with aapt2/apkanalyzer)

## 9. Locked implementation decisions (production-readiness plan, 2026-11-17)

These decisions were locked in `docs/superpowers/plans/2026-11-17-production-readiness.md`
after the six-review council. They refine this spec; where a line below conflicts
with earlier sections, the decision supersedes that line (noted per item) and the
older text is kept only as history.

1. **D1 — First run and delete-all seed the baseline from the first sensor
   sample.** `StepState.initialized=false` until a sample arrives; a fresh
   install shows 0, not the lifetime boot counter. *Rationale: a fake first day
   destroys the "honest numbers" promise. Supersedes the implicit baseline=0
   behaviour and the old fresh-install test.*
2. **D2 — Any date change rolls over and drops the unattributable delta.**
   Forward or backward clock movement sets `todaySteps=0`, `baselineRaw=raw`;
   the old carry-over test is deleted. *Rationale: spec §3.5 says drop the
   unrecorded delta; an overnight outage must not inflate today. Supersedes any
   carry-over behaviour in §3.4.*
3. **D3 — Per-day rows are monotonic.** Writes are
   `max(pipeline steps, existing row steps)`; no path writes a smaller number.
   *Rationale: restarts, restore and reconcile must never erase history.
   Supersedes the "upsert" wording in §3.2/§3.5.*
4. **D4 — CSV restore is one Room transaction + per-date max-merge + size
   caps.** Oversized files are refused; the pipeline is seeded from today's
   restored row with `initialized=false`. *Rationale: import must not be able to
   truncate or duplicate history; replaces §5 Settings "round-trip safe" as an
   untested claim with an enforced rule.*
5. **D5 — Active minutes have no synthetic fallback; cadence is day-aware.**
   The minutes counter resets at midnight and the writer never stamps
   yesterday's minutes onto today. *Rationale: §2's cadence ring must not leak
   across days; the old steps/120 fallback was undocumented and dishonest.*
6. **D6 — One 365-day streak window and one shared window function.** Home,
   History and Insights use `StepMath.windowDates/valuesForWindow/averageOverWindow`
   with zero-filled days. *Rationale: three different denominators made the same
   data report three different averages. Supersedes the ad-hoc windows in §5.*
7. **D7 — Language defaults to the device locale (ar → Arabic, else English).**
   An explicit choice persists; channels and notifications are created in the
   active language; AAB language splits are disabled (`enableSplit=false`).
   *Rationale: an Arabic-first app must not open in English; Play language
   splits would strip Arabic resources on non-AR installs. Supersedes the
   AppCompatDelegate suggestion in §6 — the app uses its own `AppLocale`
   configuration wrapper.*
8. **D8 — Arabic copy is corrected and body data is honest.** Plurals,
   terminology, GPS wording, no false permission claims; metric defaults stated;
   imperial setting renamed to miles-only. *Rationale: §5 copy and §7 palette are
   design contracts; a translation that misstates units or permissions breaks
   trust.*
9. **D9 — The health FGS starts only with ACTIVITY_RECOGNITION granted; no
   background starts.** Foreground entry points, `BOOT_COMPLETED` and
   `MY_PACKAGE_REPLACED` re-arm it; Home shows counting status. *Rationale:
   Android 14+ throws after `startForeground(type=health)` when its prerequisite
   is missing — the app's own onboarding invited users to skip it. Supersedes
   §3.6's boot-time FGS start and §4's always-on assumption.*
10. **D10 — Delete-all stops both services, suppresses writes and erases
    everything.** DB, DataStore, walk checkpoint, locale, notification and
    status prefs; the app returns to onboarding. *Rationale: "delete all data"
    is a moral statement; previously the service re-inflated today from its
    in-memory counter. Supersedes §5's "clears DB + prefs" as incomplete.*
11. **D11 — Walk tracking uses immutable route snapshots and supports resume.**
    Checkpoints flush on destroy, resume is offered after an interrupted walk,
    and History lists saved walks. *Rationale: §4 promises checkpointing, but a
    concurrent list mutation and an invisible walk history made it unverifiable.
    Supersedes §4's checkpoint wording.*
12. **D12 — Release is a minified, resource-shrunk, signed AAB.** Signed from a
    local gitignored keystore, `language.enableSplit=false`, CI, LICENSE and
    third-party notices; versionName `1.0.0`, versionCode 1. *Rationale:
    "release-buildable" is a gate, not a README claim. Supersedes the "unsigned
    by design" stance.*
13. **D13 — Privacy-policy URL is a BuildConfig field.** Driven by the Gradle
    property `sabeel.privacyPolicyUrl`; the in-app link appears only when it is
    configured. *Rationale: Play requires a live policy URL and the in-app link
    must point at the same page. Hosting/filling remains an owner action.*
14. **D14 — Play Console declarations remain owner actions.** Health apps,
    FGS per-type videos, Data safety, IARC, testing track and rollout are
    prepared in docs but cannot be completed in code. *Rationale: only the
    account owner can submit them; see docs/release/PLAY-SUBMISSION-CHECKLIST.md
    and docs/legal/BLOCKERS.md.*