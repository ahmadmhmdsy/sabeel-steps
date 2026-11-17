# Product spec v1 — Sabeel (working name)

Read docs/product/brief.md first. This file is the build contract: if code and this file disagree,
fix one of them explicitly. Status: v1.0, 2026-11-16.

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

Preferences (DataStore): goalSteps (default 8000), units ('metric'|'imperial'), heightCm (default 170),
weightKg (default 70), gender ('unspecified'|'male'|'female'), strideCm (0 = auto), language ('en'|'ar'),
reminderEnabled (false), reminderMinutes (e.g. 1140 = 19:00), onboardingDone (false), todayBaseSteps,
sensorBaselineRaw, baselineDate.

## 2. Algorithms (pure Kotlin in domain/StepMath.kt — unit tested)

Stride (cm): if strideCm > 0 use it; else heightCm * (gender == female ? 0.413 : 0.415); clamp 40..110 cm.
Distance (m) = steps * strideCm / 100.
Calories (kcal) = steps * 0.04 * (weightKg / 70.0), rounded to 1 decimal. Label "estimate".
Active minutes: count minutes in which cadence >= 60 steps/min; service keeps a 60-slot ring of
(currentMinuteKey, stepsThisMinute); flush finished minutes into today's counter. Approximate but stable.
Pace: durationSec / (distanceMeters / 1000.0), only if distanceMeters >= 50; formatted m:ss /km.
BMI (optional display only): weight / (height/100)^2, one decimal, never a diagnosis.

## 3. Step pipeline (correctness is a feature)

1. Sensor: Sensor.TYPE_STEP_COUNTER (cumulative since boot, float). Register with
   SensorManager.SENSOR_DELAY_NORMAL; it is event-driven and cheap.
2. State: baselineRaw (raw sensor value at last rebase), todayBase (steps already counted for the
   current local date). Persist both atomically (DataStore) after every meaningful delta; today's
   row in Room is upserted at most every ~5 s or on lifecycle boundaries.
3. On event: raw = event.values[0]; if raw < baselineRaw - 1 => device rebooted or sensor reset:
   baselineRaw = raw, keep todayBase unchanged. delta = max(0, raw - baselineRaw).
   todaySteps = todayBase + delta => upsert DailySteps(today).
4. Date rollover (checked on every event, on service start, and in a 15-min periodic WorkManager):
   if today's date != baselineDate, finalize yesterday (already in DB), set todayBase = 0,
   baselineDate = today, baselineRaw = raw.
5. Resilience: service START_STICKY; on service start re-read persisted state, never double count
   (todaySteps is monotonic inside a day: todaySteps = max(db.todaySteps, todayBase + delta)).
6. Boot: BOOT_COMPLETED receiver tries to start the FGS inside try/catch (Android 15 may restrict FGS
   from boot for some types; type 'health' is attempted, failure is non-fatal). A WorkManager one-time
   job guarantees a reconcile within 15 min if the service could not start.
7. No polling loops, no AlarmManager exact alarms, no wakelocks in v1.

## 4. Services

StepCounterService: foreground service, type health; persistent low-priority notification
("Sabeel is counting your steps", tap opens app, no sound). START_STICKY. Stops only if user
disables counting in settings (v1: always on once onboarding completes).
WalkTrackingService: foreground service, type location while tracking; requests fine location at
runtime from the Walk screen; collects LocationManager updates (no Play Services dependency) every
~3 s or 5 m; computes GPS distance; persists an in-progress walk checkpoint every 20 s so a process
death does not lose the route; notification with Stop action. If permission denied, tracking runs
sensor/step-only and says so in the UI.

## 5. Screens & navigation (Compose, Material3, Navigation-Compose)

Routes: onboarding (when !onboardingDone), home, history, walk, insights, settings, privacy, about.
- Onboarding (3 steps, skippable height/weight): welcome/privacy promise -> goal -> units + body data.
- Home: today ring (steps/goal), 3 stat cards (distance, kcal, active min), streak chip, gentle status
  line ("A short walk after lunch keeps the streak alive"), primary CTA "Start walk", last-7-days mini
  bars. Pull-to-refresh not needed; state from Flow.
- History: range toggle 7/30 days, bar chart with goal line, list of day rows (date, steps, distance).
- Walk: idle state (permission explainer) -> live (timer, distance, pace, steps, our own Canvas route
  trace) -> summary (save/discard, stats, route thumbnail). No map tiles, no API key, no network.
- Insights: 7-day average, best day, goal-hit rate, total distance, streak; one plain-language
  sentence; all numbers derived, no invented coaching.
- Settings: goal stepper, units toggle, reminder switch + time, height/weight/gender, language switch
  (en/ar), export AND import CSV (SAF CreateDocument/OpenDocument, round-trip safe — council add: import
  is the mitigation for "no cloud backup"), delete all data (confirm dialog, clears DB + prefs),
  privacy policy, About (version, licenses note).
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
