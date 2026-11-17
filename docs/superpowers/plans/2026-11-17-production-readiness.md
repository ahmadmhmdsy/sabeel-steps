# Sabeel Production-Readiness Implementation Plan

> **For agentic workers:** This plan is executed with Agent Teams. Each workstream has a disjoint write scope. The Lead owns integration, Gradle builds, git, and final verification. Steps use checkbox (- [ ]) syntax.

**Goal:** Turn the reviewed Sabeel v1 codebase into a trustworthy, crash-free, release-buildable Android app (signed AAB, lint clean, tests green, Arabic/English parity) and document the owner-only blockers that no code change can clear.

**Architecture:** Two-layer refinement. Layer 1 makes the step pipeline idempotent and self-healing: an explicit initialized flag, spec-compliant midnight rollover, monotonic per-day totals, transactional CSV restore, day-aware active minutes. Layer 2 makes the platform shell obey Android 14+ rules: never start the health foreground service without its prerequisite, never start services from background, surface counting status in the UI, localize notifications and channels, capture walk routes with immutable snapshots, and release through a signed, minified AAB.

**Tech Stack:** Kotlin 2.0.21, Compose Material3, Room 2.6.1 (KSP), DataStore, WorkManager, Navigation-Compose, AGP 8.10.1, Gradle 8.11.1, JDK 21, minSdk 26, target/compileSdk 36, JUnit4 unit tests, API 36 emulator for smoke tests.

**Sources:** docs/review/2026-11-17/COUNCIL-VERDICT.md and the six specialist reports. Baseline commit: 0663edf.

**Environment (every Gradle call):**

    $env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
    $env:ANDROID_HOME = 'C:/Android/Sdk'
    $env:ANDROID_SDK_ROOT = 'C:/Android/Sdk'
    .\gradlew.bat <task> --no-daemon

Only the Lead runs Gradle (single daemon owner). Teammates write files only.

---

## Definition of Done (gates)

- G1  .\gradlew.bat testDebugUnitTest : all tests pass (baseline 41; target 55+), zero failures.
- G2  .\gradlew.bat lintDebug : exit 0, zero lint errors (warnings allowed but listed).
- G3  .\gradlew.bat assembleDebug assembleRelease bundleRelease : all succeed; release APK verifies with apksigner and AAB exists.
- G4  API 36 emulator smoke: fresh install, skip permissions -> no crash, Home shows counting-off status card, onboarding completes; grant Activity permission -> counting status; delete-all -> onboarding again, no ghost today row.
- G5  Emulator ar-IQ locale screenshots: Home/History/Insights render Arabic RTL with chart labels under the correct bars and no clipped ring number.
- G6  .\gradlew.bat :room:schemaLocation output committed under app/schemas; no fallbackToDestructiveMigration remains.
- G7  git log shows baseline + one commit per workstream + verification commit.

## Locked decisions (do not relitigate during implementation)

- D1 First run and delete-all seed the baseline from the first sensor sample: StepState gains initialized=false until then. First run shows 0, not the lifetime counter.
- D2 Any date change (forward, or clock moving backward) rolls over: todaySteps=0 and baselineRaw=raw, so the unattributable delta is dropped. This is spec section 3.5; the old carry-over test is deleted.
- D3 DB is monotonic per day: writes are max(pipeline steps, existing row steps); no code path writes a smaller number.
- D4 CSV restore is one Room transaction, max-merges per date, refuses oversized files, and seeds pipeline state from today's restored row (initialized=false until the next sensor sample).
- D5 Active minutes have no synthetic fallback; StepCadence is day-aware and reset at midnight; the writer never stamps yesterday's minutes onto today.
- D6 Streak window is 365 days. Home/History/Insights averages divide by the full window and zero-fill missing days from ONE shared function.
- D7 Language defaults to the device locale (ar -> Arabic, else English); explicit choice persists; notifications and channels are created in the active language; AAB language splits disabled.
- D8 Arabic copy is corrected (plurals, terminology, GPS wording, no false permission claims); body data is honest metric with stated defaults; imperial setting renamed to miles-only.
- D9 Health FGS starts only when ACTIVITY_RECOGNITION is granted; no background service starts; foreground entry points, BOOT_COMPLETED and MY_PACKAGE_REPLACED re-arm; Home reports status.
- D10 Delete-all stops both services, suppresses service writes, erases DB + DataStore + walk checkpoint + locale + notification + status prefs, then returns to onboarding.
- D11 Walk service uses immutable route snapshots, flushes checkpoints on destroy, requires location or activity permission, supports resuming an interrupted checkpoint, and History lists saved walks.
- D12 Release build is minified + resource-shrunk, signed from a local gitignored keystore, produces AAB with language split disabled, has CI, LICENSE and third-party notices; versionName 1.0.0, versionCode 1.
- D13 Privacy policy URL is a BuildConfig field from a Gradle property; in-app link appears when configured. Policy placeholders and hosting remain owner actions.
- D14 Play Console declarations (Health apps, FGS, Data safety, IARC, content rating, testing track) remain owner actions; code/docs prepare the evidence.

## Workstreams and write scopes

| WS | Owner | Files |
| --- | --- | --- |
| WS0 Lead integration | lead | domain/StepPipeline.kt, domain/StepMath.kt, data/prefs/SettingsStore.kt, data/repo/StepsRepository.kt, data/db/*, data/export/CsvIo.kt, AppContainer.kt, SabeelApp.kt, MainActivity.kt, ui/viewmodel/AppViewModels.kt, ui/screens/SabeelAppRoot (navigation), app/src/test/**, build fixes |
| WS1 Platform reliability | platform | service/**, platform/**, AndroidManifest.xml, res/values/strings_sensor.xml + res/values-ar/strings_sensor.xml |
| WS2 UI, Arabic, a11y | ui | ui/screens/**, ui/components/**, ui/theme/**, ui/i18n/**, res/values/strings.xml + res/values-ar/strings.xml, res/values/colors? |
| WS3 Release engineering | release | app/build.gradle.kts, app/proguard-rules.pro, gradle.properties, .gitignore, .github/**, LICENSE, NOTICE, app/src/main/assets/THIRD-PARTY-NOTICES.txt, docs/release/**, docs/legal/BLOCKERS.md, docs/privacy-site/FILL-AND-HOST.md, docs/product/spec.md, README.md, keystore/ + keystore.properties (generated, gitignored) |

Out of scope for v1.0: Health Connect, widgets, cloud sync, ranking, ads, any network permission.

---

## Interface contract (freeze before parallel work)

### Domain and data (Lead implements)

    data class StepState(
        val baselineRaw: Float,
        val todaySteps: Int,
        val baselineDate: String,
        val initialized: Boolean = false,
    )

    object StepPipeline {
        fun onSensor(state: StepState, raw: Float, today: String): StepUpdate
        fun resolveTodaySteps(state: StepState, dbTodaySteps: Int?): Int
    }

Rules for onSensor:
- if raw is not finite: return unchanged state, rolledOver=false.
- if state.initialized == false: seed state = StepState(raw, state.todaySteps, today, initialized=true). Do not zero todaySteps (this preserves a restored today row).
- else if today != state.baselineDate: rolledOver=true, state = StepState(raw, 0, today, initialized=true).
- else: rebase if raw < baselineRaw - 1f; delta = (raw - baselineRaw).coerceAtLeast(0).toInt() using the possibly rebased baseline; todaySteps = state.todaySteps + delta; state = StepState(raw, todaySteps, today, initialized=true).

StepsRepository public API (used by services and VMs):

    class StepsRepository(database: AppDatabase, dao: DailyStepsDao, settings: SettingsRepository) {
        fun todayFlow(): Flow<DailyStep?>
        fun lastDaysFlow(days: Int): Flow<List<DailyStep>>
        fun streakFlow(): Flow<Int>                     // uses 365-day window internally
        suspend fun recordRawStep(raw: Float, now: Long, activeMinutesToday: Int = -1): DailyStep?
        suspend fun reconcile(now: Long, raw: Float? = null, activeMinutesToday: Int = -1): DailyStep?
        suspend fun restore(rows: List<DailyStep>): RestoreSummary
        suspend fun clearAll()
    }

    data class RestoreSummary(val inserted: Int, val updated: Int, val kept: Int)

SettingsStore additions: boolean key step_initialized; readStepState returns initialized=false when the key is absent; writeStepState persists it.

StepMath additions:

    fun windowDates(today: String, days: Int): List<String>
    fun valuesForWindow(rows: List<DailyStep>, window: List<String>): List<Int>
    fun averageOverWindow(rows: List<DailyStep>, window: List<String>): Int
    fun goalHitRateOverWindow(rows: List<DailyStep>, window: List<String>, goalSteps: Int): Double

### Platform (platform workstream implements, Lead consumes)

    enum class StepServiceStatus { COUNTING, NO_PERMISSION, NO_SENSOR, STOPPED }
    object StatusStore { fun mark(context, status); fun markStopped(context); fun read(context): StepServiceStatus; fun clear(context) }
    object Permissions { fun hasStepSensor(context): Boolean; existing helpers kept }

    StepCounterService.ensureStarted(context: Context): Boolean   // false when permissions/sensor missing; never throws
    StepCounterService.beginErase() / endErase()                  // suppress pending writes during delete-all
    StepCounterService.stopForErase(context)                      // stopService

    WalkTracker.hasCheckpoint(context): Boolean
    WalkTracker.deleteCheckpoint(context)
    WalkTrackingService.start(context): Boolean                   // false when neither location nor activity permission
    WalkTrackingService.stop(context) / discard(context)
    WalkTrackingService.resetSession(context)                     // delete checkpoint + reset in-memory state

    Notifications.ensureChannels(context)                         // idempotent, localized, updates names
    Notifications.resetPrefs(context)                             // goal-once-per-day flags
    AppLocale.clear(context)                                      // ui/i18n owned by ui workstream
    AppLocale.current(context): AppLanguage                       // device locale fallback

### ViewModel / UI state contract (Lead implements VMs, ui workstream renders)

HomeUiState gains: serviceStatus: StepServiceStatus. HomeViewModel gains startCounting().
HomeScreen signature gains onStartCounting: () -> Unit; renders CountingStatusCard when status is STOPPED or NO_SENSOR; keeps PermissionCard for NO_PERMISSION.

HistoryUiState gains walks: List<SavedWalkUi>. HistoryScreen defines:

    data class SavedWalkUi(
        val id: Long,
        val startedAtMillis: Long,
        val durationSeconds: Long,
        val steps: Int,
        val distanceMeters: Double,
        val secondsPerKm: Int,
        val route: List<RoutePoint>,
    )

HistoryScreen renders a Walks section (title, rows, empty line, tap opens a dialog with RouteSketch when route is non-empty).

WalkUiState gains pendingCheckpoint: Boolean and startBlocked: Boolean.
WalkViewModel gains resumePending(), discardPending().
WalkScreen renders a resume card when idle and pendingCheckpoint, and a short blocked message when startBlocked.

String keys the ui workstream must add (en + ar) for Lead-wired screens:
home_status_stopped_title, home_status_stopped_body, home_status_no_sensor_title, home_status_no_sensor_body, home_status_action_start,
history_walks_title, history_walks_empty, history_walk_detail_title, history_walk_close,
walk_resume_title, walk_resume_body, walk_resume_action, walk_resume_discard, walk_start_blocked,
settings_import_confirm_title, settings_import_confirm_body, settings_import_confirm_action,
plus plurals: minutes_value (one/other + ar forms), history_range_days (ar forms).

---

## Phase 1 - Core data correctness (Lead, TDD)

### Task 1: Step pipeline seeded from first sample, spec rollover

**Files:** app/src/main/java/com/sabeel/steps/domain/StepPipeline.kt, app/src/test/java/com/sabeel/steps/domain/StepPipelineTest.kt

- [ ] Step 1: Replace StepPipelineTest fresh-install test with failing tests: freshInstallStartsWithZero (raw=137 -> todaySteps=0, initialized=true); firstRunKeepsRestoredToday (initial todaySteps=420 -> stays 420); midnightRolloverDropsUnattributedDelta (yesterday 5000, then date changes and raw jumps by 300 -> todaySteps=0, rolledOver=true); backwardClockRolloverResetsDay; sameDayRebaseAfterCounterReset; addsOnlyNewDeltaWithinDay; resolveTodayStepsTakesMax.
- [ ] Step 2: .\gradlew.bat testDebugUnitTest --tests com.sabeel.steps.domain.StepPipelineTest -> expect failures.
- [ ] Step 3: Implement initialized flag and onSensor rules above, plus resolveTodaySteps.
- [ ] Step 4: tests pass.
- [ ] Step 5: commit "fix(core): seed step baseline on first sample and follow spec rollover".

### Task 2: Monotonic repository writes and restore

**Files:** StepsRepository.kt, data/db/Daos.kt, data/db/AppDatabase.kt (schema export flag lives in build later), AppContainer.kt, CsvIo.kt, new test file app/src/test/java/com/sabeel/steps/data/StepMergeRulesTest.kt (pure rules extracted if Room cannot run on JVM).

- [ ] Add DAO method suspend fun today(date: String): DailyStepEntity?.
- [ ] recordRawStep: update pipeline state first, resolve steps = max(update.todaySteps, existing.steps), persist state, write row; skip first-seed 0-step ghost row (no existing row, steps==0, no active minutes).
- [ ] reconcile: raw path uses pipeline and max rule; no-raw path on date change resets state to StepState(0f, 0, today, initialized=false) and still max-merges with existing row.
- [ ] restore: mutex + database.withTransaction; per date max-merge (keep larger steps, count kept); after transaction seed pipeline state from today's row with initialized=false.
- [ ] upsertDay active minutes: explicit=-1 -> existing row value else 0 (delete the steps/120 fallback).
- [ ] CsvIo.import delegates to repository.restore and returns RestoreSummary; enforce 5 MB input cap and 20k row cap with CsvFormatException.
- [ ] Unit tests for max-merge and window functions (Room-free pure helper, e.g. StepMerge.decide(existingSteps, importedSteps): MergeDecision).
- [ ] Commit "fix(data): monotonic writes, transactional restore, no synthetic active minutes".

### Task 3: SettingsStore initialized flag and clear

**Files:** SettingsStore.kt.
- [ ] Add stepInitialized boolean key; read/write it; absent -> false.
- [ ] Keep clearAll() clearing everything (used by delete-all).
- [ ] Commit with Task 2 if small.

### Task 4: Shared window math for Home/History/Insights

**Files:** StepMath.kt, AppViewModels.kt (home/history/insights VMs), StepMathTest.kt.
- [ ] Add windowDates/valuesForWindow/averageOverWindow/goalHitRateOverWindow with zero-fill.
- [ ] HomeViewModel: build 7-day window from windowDates + valuesForWindow (no db guarantee), average = averageOverWindow.
- [ ] HistoryViewModel: zero-filled 30-day list, current streak from streakFlow, total = sum over window.
- [ ] InsightsViewModel: averages/goal rate/best day over the 30-day window (zero days count as not hit, average divides by 30).
- [ ] streakFlow window 365.
- [ ] Tests: average includes zero days; goal rate denominator is window size; window dates order oldest->newest.
- [ ] Commit "fix(insights): one window definition with zero-filled denominators".

## Phase 2 - Platform reliability (platform workstream)

### Task 5: StatusStore and permission gates

**Files:** platform/StatusStore.kt (new), platform/Permissions.kt, service/StepCounterService.kt.
- [ ] StatusStore: SharedPreferences sabeel_service_status with mode + last_alive_ms; read returns STOPPED when older than 120 s; mark/markStopped/clear.
- [ ] Permissions.hasStepSensor uses PackageManager.FEATURE_SENSOR_STEP_COUNTER.
- [ ] StepCounterService.ensureStarted(context): if !hasActivityRecognition -> mark(NO_PERMISSION), false; if !hasStepSensor -> mark(NO_SENSOR), false; else startForegroundService with try/catch, true.
- [ ] onCreate: guard before any startForeground (missing permission/sensor -> mark + stopSelf, return). This is the API 34+ SecurityException fix.
- [ ] Writer: mark(COUNTING) and refresh last_alive each tick; onDestroy markStopped unless erase in progress.
- [ ] beginErase/endErase volatile flag; flushPending returns immediately when set.
- [ ] Remove all background start call sites (see Task 8).

### Task 6: StepCadence day awareness

**Files:** service/StepCadence.kt.
- [ ] Track active day; expose activeMinutesFor(today) that rolls over first.
- [ ] On day change (also on destroy): optionally count the in-progress minute if its projected spm >= threshold, then reset slot ring and counters.
- [ ] Writer passes activeMinutesFor(today); midnight never stamps yesterday onto today.
- [ ] Unit tests for rollover and threshold behavior.

### Task 7: Service lifecycle and notification correctness

**Files:** service/StepCounterService.kt, platform/Notifications.kt, platform/BootReceiver.kt, platform/Reminders.kt.
- [ ] All notifications use AppLocale.wrap(context); ensureChannels localized and idempotent.
- [ ] Goal notification: mark flag only after notify() succeeded and POST_NOTIFICATIONS granted; skip otherwise.
- [ ] Reminder notifications: permission-gated and localized.
- [ ] ReconcileWorker: remove StepCounterService.start; only repository.reconcile() (no background FGS).
- [ ] BootReceiver: exported=false; actions BOOT_COMPLETED + MY_PACKAGE_REPLACED; drop QUICKBOOT_POWERON; call ensureStarted.
- [ ] Commit "fix(platform): gate health FGS on its prerequisite and re-arm only from allowed entry points".

### Task 8: Walk tracking hardening

**Files:** service/WalkTrackingService.kt, service/WalkTracker.kt (or same file), AndroidManifest.xml (only if receiver/service flags change).
- [ ] Keep route as immutable snapshot: on each point, update a @Volatile List<RoutePoint> copy; checkpoint writer reads the snapshot only.
- [ ] saveCheckpoint off main; onDestroy flush with runBlocking + withTimeoutOrNull(1500).
- [ ] start(context): false when neither location nor activity permission; health fallback only when activity permission present (API 34+ prerequisite).
- [ ] WalkTracker.hasCheckpoint/deleteCheckpoint/resetSession; resume loads checkpoint when service restarted with null intent.
- [ ] stop()/discard() keep clearing notification and state.
- [ ] Unit-testable pure pieces (route distance/pace) if time allows.
- [ ] Commit "fix(walk): immutable route snapshots, reliable checkpoint flush, safe start gating".

## Phase 3 - UI, Arabic, accessibility (ui workstream)

### Task 9: RTL bar chart and theme contrast

**Files:** ui/components/BarChart.kt, ui/theme/Color.kt, ui/theme/Theme.kt.
- [ ] BarChart: read layoutDirection in DrawScope; when RTL mirror bar x (x = size.width - (index+1)*(barWidth+gap) + gap) so each bar sits above its label. Labels Row stays mirrored by layout.
- [ ] Light theme: below-goal bar alpha 0.7 and a darker goal line/ring end color with >= 3:1 contrast on the surface (test with a WCAG calculator; document ratios in code comment).
- [ ] Commit "fix(ui): mirror bars for RTL and raise chart contrast".

### Task 10: Home usability, status card, saved walks, walk resume

**Files:** ui/screens/HomeScreen.kt, ui/screens/HistoryScreen.kt, ui/screens/WalkScreen.kt, ui/screens/SabeelAppRoot.kt.
- [ ] Home: Start Walk CTA before the 7-day chart (above the fold at 360x640); CountingStatusCard for STOPPED/NO_SENSOR with action; keep PermissionCard for NO_PERMISSION; wire onStartCounting.
- [ ] History: Walks section with SavedWalkUi rows, empty state, route dialog using RouteSketch.
- [ ] Walk: resume card when idle and pendingCheckpoint; blocked message when startBlocked.
- [ ] Add all contract strings (en+ar), plurals, headings semantics, ring clearAndSetSemantics, loading label.
- [ ] Commit "feat(ui): counting status, saved walks, resume prompt and above-fold CTA".

### Task 11: Arabic copy, pluralization, locale default, notification language

**Files:** ui/i18n/AppLocale.kt, res/values/strings.xml, res/values-ar/strings.xml, SettingsScreen.kt, OnboardingScreen.kt.
- [ ] AppLocale.current: stored preference else device locale (Configuration locales[0].language == "ar" -> AR else EN); add clear(context).
- [ ] Settings language row displays AppLocale.current; after apply call Notifications.ensureChannels(context) and recreate.
- [ ] Onboarding language page preselects AppLocale.current; finish no longer promises counting without permissions.
- [ ] Replace value_minutes with plurals in Home/History/Insights; fix Arabic plurals (zero/one/two/few/many/other) and "آخر X أيام" plural.
- [ ] Terminology pass: walk = مشية, streak = سلسلة, remove جولة/تتابع/موجز; GPS without "الـ"; imperial label "ميل" only; body-data copy names metric defaults; insights empty title "تحتاج الأنماط إلى بضعة أيام".
- [ ] Reminder copy says "about/around" the chosen time.
- [ ] Commit "fix(i18n): device-locale default, Arabic plurals and terminology, honest permission copy".

### Task 12: Accessibility and font scale

**Files:** ui/screens/SettingsScreen.kt, HomeScreen.kt, HistoryScreen.kt, InsightsScreen.kt, ui/components/*.
- [ ] Reminder switch row: toggleable with role Switch, label for target size.
- [ ] Permission state re-check on ON_RESUME (LifecycleEventObserver) in Home/Walk/Settings.
- [ ] Screen titles use heading semantics; chart/ring have content descriptions; no placeholder as contentDescription.
- [ ] Font scale 2.0: settings rows stack vertically at fontScale > 1.4; ring number style shrinks at > 1.5; key numbers avoid ellipsis (5-digit steps, 4+ digit goal).
- [ ] Commit "fix(a11y): switch semantics, resume permission checks, font-scale layouts".

## Phase 4 - Release engineering (release workstream, no Gradle runs)

### Task 13: Release build configuration

**Files:** app/build.gradle.kts, app/proguard-rules.pro, gradle.properties, app/src/main/AndroidManifest.xml? (no).
- [ ] Load keystore.properties if present into signingConfigs.release; signing optional so unsigned CI builds still work.
- [ ] release buildType: isMinifyEnabled=true, isShrinkResources=true, proguard default + app file.
- [ ] buildConfigField String PRIVACY_POLICY_URL from property sabeel.privacyPolicyUrl default "".
- [ ] KSP arg room.schemaLocation = projectDir/schemas; commit generated schemas after Lead build.
- [ ] bundle { language { enableSplit = false } }.
- [ ] versionName "1.0.0", versionCode 1; keep debug suffix? no.
- [ ] Commit "build(release): signed minified AAB with exported Room schema".

### Task 14: Upload keystore and signing docs

**Files:** keystore/ (gitignored), keystore.properties (gitignored), docs/release/KEYSTORE.md.
- [ ] keytool -genkeypair -v -keystore keystore/sabeel-upload.jks -alias sabeel-upload -keyalg RSA -keysize 4096 -validity 10000 with random passwords; write keystore.properties.
- [ ] Record SHA-256 fingerprint in docs/release/KEYSTORE.md plus backup/rotation instructions; never commit keystore or passwords.
- [ ] Commit docs only.

### Task 15: Licensing, CI, README, submission docs

**Files:** LICENSE, NOTICE, app/src/main/assets/THIRD-PARTY-NOTICES.txt, .github/workflows/android.yml, README.md, docs/release/PLAY-SUBMISSION-CHECKLIST.md, docs/legal/BLOCKERS.md, docs/privacy-site/FILL-AND-HOST.md, .gitignore, docs/product/spec.md.
- [ ] Apache-2.0 LICENSE + NOTICE + third-party list for AndroidX/Compose/Room/DataStore/WorkManager/Navigation/kotlinx.
- [ ] CI: JDK 21, assembleDebug, testDebugUnitTest, lintDebug, bundleRelease (unsigned path).
- [ ] README rewrite: honest state, build/run instructions, test/lint/build evidence markers, known limitations, owner blockers.
- [ ] spec.md decisions D1-D12 updates (rollover, delete-all, locale, active minutes, walk visibility, release).
- [ ] Play checklist and blocked-items file with exact owner actions.
- [ ] Commit "docs(release): licensing, CI, honest README, submission checklist".

## Phase 5 - Integration and verification (Lead)

### Task 16: Wire ViewModels and navigation

**Files:** AppViewModels.kt, SabeelAppRoot.kt, OnboardingScreen.kt owner wiring? (ui file - coordinate), SabeelApp.kt, MainActivity.kt, AppContainer.kt.
- [ ] Home VM: status polling (5 s), startCounting via ensureStarted, refresh after permission result; 7-day window math.
- [ ] History VM: walks flow -> SavedWalkUi; zero-filled window; streak 365.
- [ ] Insights VM: window math.
- [ ] Settings VM: import returns counts, shows confirm dialog state; deleteAll calls beginErase, stop services, clears DB+DataStore+checkpoint+locale+notification+status prefs, waits 400 ms, clears again, endErase, onboarding.
- [ ] Walk VM: pendingCheckpoint from WalkTracker, resumePending -> WalkTrackingService.start, discardPending -> resetSession, startBlocked from start() result.
- [ ] Onboarding finish: ensureStarted (safe when permission denied).
- [ ] SabeelRoot: on app open, if onboardingDone, ensureStarted and refresh status; Notifications.ensureChannels(AppLocale.wrap(context)) in SabeelApp.onCreate.
- [ ] Commit "feat(integration): status, restore, delete-all and walk resume wiring".

### Task 17: Build, test, lint, fix stragglers

- [ ] .\gradlew.bat testDebugUnitTest -> all pass; fix compile breaks from contract drift.
- [ ] .\gradlew.bat lintDebug -> fix remaining MissingPermission with explicit runtime checks or narrowly scoped @SuppressLint with rationale.
- [ ] .\gradlew.bat assembleDebug assembleRelease bundleRelease -> succeed; verify APK with apksigner; check zipalign -P 16 for release APK and AAB native libs.
- [ ] Copy app/schemas into git; confirm no fallbackToDestructiveMigration.
- [ ] Update dist/SHA256SUMS.txt -> release APK + AAB hashes; record evidence in docs/review/2026-11-17/FIX-REPORT.md.
- [ ] Commit "build: production artifacts and verification evidence".

### Task 18: Emulator smoke tests (API 36)

- [ ] Start emulator api36 headless; install debug APK; am start; logcat FATAL check; screenshot onboarding.
- [ ] Skip both permissions on page 3; finish; verify Home renders status card and no crash; screenshot.
- [ ] Delete-all: Settings -> delete data -> verify onboarding returns, relaunch and confirm no crash; inspect DB via run-as to confirm no ghost today row (0 rows acceptable).
- [ ] Grant ACTIVITY_RECOGNITION via adb pm grant; relaunch; confirm status card clears or sensor-absent message appears; screenshot.
- [ ] Set locale ar-IQ (adb shell cmd locale set-app-locales or emulator locale); screenshots Home/History/Insights; verify RTL chart alignment visually.
- [ ] Save screenshots to docs/review/2026-11-17/screenshots/ and link in report.
- [ ] Commit evidence.

## Phase 6 - Close-out

### Task 19: Fix report and final answer

- [ ] Write docs/review/2026-11-17/FIX-REPORT.md: what changed per finding, verification evidence (commands, outputs, counts, hashes), remaining owner blockers, residual risks.
- [ ] Update README verification markers with final numbers.
- [ ] Final git commit and status check.
- [ ] Report to user: plan + what was fixed + evidence + what only they can do (policy fill/host, trademark clearance, Play account/track, store declarations).

## Owner-only blockers (cannot be fixed in code)

1. Privacy policy: fill [DEVELOPER LEGAL NAME], [CONTACT EMAIL], [POSTAL ADDRESS], [DATE OF FIRST PUBLICATION] in docs/legal/privacy-policy.md and docs/privacy-site/index.html, host on HTTPS, set sabeel.privacyPolicyUrl.
2. Trademark clearance for the name Sabeel (existing registered mark for a blood-pressure app in the same broad category) before the first production submission.
3. Play Console: account type/creation date (personal accounts created after 2023-11-13 need 12 testers for 14 continuous days), Health apps declaration, FGS declarations + per-type videos, Data safety form, IARC rating, store listing assets (current screenshots are black frames and the promo video is 0 bytes), upload key registration.
4. Decision on distribution strategy (Play vs portfolio/B2B) given no consumer payment path in Iraq.
5. Run the 3 kill-metric experiments from the council before investing in growth features.

## Risks and rollback

- Risk: R8 breaks Room/Compose at runtime. Mitigation: keep release build minimal rules, verify release APK on emulator, and ship debug-equivalent smoke results; if R8 failure appears, disable minify for v1.0 and document.
- Risk: contract drift between parallel workstreams. Mitigation: contract section is frozen; Lead compiles early and reconciles.
- Risk: emulator instability. Mitigation: cold boot with -no-snapshot; fall back to unit/lint/build evidence and report gap honestly.
- Rollback: every workstream is a separate commit; revert commits individually.
