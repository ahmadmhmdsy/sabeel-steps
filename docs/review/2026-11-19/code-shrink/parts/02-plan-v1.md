# Beat 2 — Engineer, plan v1

**Tomas Reyes — Engineer.** Owns the build; has done three "safe" refactors that each removed a thousand lines and broke exactly one thing. That record is why this plan prices the risk before the prize.
**Evidence base:** `evidence-pack.md` F1–F15 in full, `parts/01-expert-first.md` (beat 1), every file this plan edits read at function level, all 11 test files as signatures + assertion inventory; corpus numbers re-derived independently in this session. **Writes:** this file only; no source, test, build or resource file was touched.

## 1. Measured state (pack cited; every number the plan leans on re-verified)

| pack fact | pack | re-verified in this session |
|---|---|---|
| F2, F3 | 62 files / 10,348 raw = 7,677 code + 978 imports + 889 comments + 804 blank; main 51 / 9,026 (6,615·933·832·646); test 11 / 1,322 | identical, line for line, Python over `app/src` |
| F4 | ui 5,021 · service 1,710 · test 1,322 · data 780 · domain 751 · platform 686 · root 78 | identical |
| F5 | 108 tests / 11 suites / 0 fail / 0 skip; lint exit 0 | not re-run (10 min; P0 re-runs it before the first edit) |
| F6 | 676 of 8,655 code lines = 7.8% normalised duplication | identical |
| F7 | `SettingsScreen()` 511 lines (`SettingsScreen.kt:90`); HomeContent 230; DiagnosticsScreen 223; HistoryScreen 156 | identical; 10 functions >100 lines, 18 >60 |
| F8, F9 | 72 composables, 316 `stringResource`; 260 strings + 4 plurals per locale | identical |
| beat-1 Expert | behaviour-free ceremony ~407 code lines; T1 floor ~9,796 (range 9,780–9,850); "witnessed shrink" metric | re-checked its dead-symbol claims and its per-file sizes against the tree: they hold (`01-expert-first.md:53–93`) |

Measured here, beyond the pack:
- **Dead surface is ~0 and now fully enumerated:** 76 top-level `fun` all used; no test-only function; **3 dead members** — `StepSourcePolicy.bestMeasured` (`StepSource.kt:57–62`), `Permissions.hasStepSensor` (`Permissions.kt:31–37`), `Daos.getSince` (`Daos.kt:21–22`) — 15 raw lines. One dead overload: `HomeScreen` 3-arg at `HomeScreen.kt:74–91` (only `SabeelRoot.kt:161` calls the 2-arg one). `CsvIo.HEADER` alias unused; `Stepper(canDecrease/canIncrease)` never overridden (5 call sites).
- **No preview scaffolding:** `@Preview` = 0. **No instrumentation tests:** `app/src/androidTest/` absent. **Tokens:** main 31,198 · test 4,455 · corpus 35,653.
- **UI duplication shapes:** 17 `Card(` (10 identical surfaceContainerLow wrappers), 9 `AlertDialog(` (7 in `SettingsScreen.kt:377–604` = 223 lines), 135 lines of repeated stacked/unstacked `StatCard` layout (`HomeScreen.kt:316–342`, `InsightsScreen.kt:59–107`, `WalkScreen.kt:274–310, 386–407`), 112 `Text(` of which 24 share two identical style/colour pairs.
- **Test coverage boundary:** 108 tests cover StepMath 24 · StepPipeline 19 · CsvCodec 12 · StepSourceHealth 9 · GoalAdvisor 9 · MotionStepDetector 8 · StepMerge 7 · StepSourcePolicy 6 · StepCadence 5 · OemBatteryGuide 5 · LocalizationParity 4. **Not covered at all:** every composable, every ViewModel, both services, Notifications, Reminders, StepsRepository, SettingsStore. The frozen oracle observes ~10.7% of the corpus (`01-expert-first.md:97`).
- **Ceiling of behaviour-free edit:** the whole measured non-behavioural budget is ~550–700 raw lines (409 ceremony + block padding + dead; F6's 676 duplicated code lines are the same material counted differently). Every target below is derived from that ceiling, not from ambition.

## 2. Committed target: reject 50 %, reject 25 %, reject 10 %; commit to 94.7 %

**Interpretation: T1** (test files byte-identical). **Primary metric: raw Kotlin lines of `app/src/main`**; the corpus number is reported too so the pack's yardstick never disappears. Always reported as the 5-tuple (raw · code · imports · comments · blank) + tokens, so a comment/blank/import "win" is visible (C5).

**50 % is not a compression target; it is a delete-the-product target.** Under T1 the corpus must be ≤5,174, tests are frozen at 1,322, so main may be ≤3,852. **Delete the entire `ui` package (all 5,021 raw lines, F4) and the corpus is still 10,348 − 5,021 = 5,327 > 5,174.** So under T1, ≤50 % requires deleting every screen and component **and** 153 more lines of service/domain/data/platform logic. Under T2 it is reachable only by deleting all 5,021 UI lines and rewriting the 108 tests to ≤1,169 lines. Both mechanisms are C1 violations; no technique changes that arithmetic. The Expert reached the same verdict from the other direction: the entire measured removable budget is ~407 code lines (6.2 % of main logic), so ~53 % of main logic cannot be "found" — it would have to be deleted (`01-expert-first.md:89`).
**25 %:** 2,587 allowed; main's own overhead floor is 2,411 and tests must exist (≥~400 under T2) → floor ≈2,811 > 2,587. Impossible under T1 **and** T2.
**10 %:** 1,035 allowed, below the main overhead floor alone; impossible under both T.
**Committed:** main **9,026 → ≤8,480 raw (−546, 94.0 %); corpus 10,348 → ≤9,802 (−546, 94.7 %)**. The phase table sums to exactly −546 (main 8,480 / corpus 9,802); the commit is that sum, with each step priced at its own net. This sits on the Expert's independently measured T1 floor (9,796, range 9,780–9,850), which I accept as the honest floor.
**Delta proof to 50 %:** after the full plan, remaining gap = 9,802 − 5,174 = **4,628 lines**. Closing it requires one of: (i) deleting ≥4,628 lines of live declarations = ~55 % of all remaining main code — screens, services, the tested domain; (ii) stripping main overhead — comments 832 + blanks 646 + imports 933 = 2,411 (C5's forbidden move) and still 2,217 short; (iii) shrinking tests (C2/T1). Any mix still crosses C1 or C5, and the 108-test oracle covers none of the UI/service lines being "saved". **No line in this plan is deleted without a named witness (§5); a plan to 50 % cannot name one for 4,600 lines, so it is a plan to change the product.** I refuse to plan to 50 % and will not sign a plan that claims it.

## 3. Ordered plan (timeboxes; Δ = net raw Kotlin lines; sum must equal §2)

### P0 — Baseline + harness · 0.5 day · Δ0
Snapshot the 5-tuple + tokens + `bundleRelease` bytes (`parts/metrics/p0.json`); SHA-256 manifest of all 11 test files (`t1-manifest.txt`); screenshot matrix (360×640, fontScale 1.0/1.3/1.5, `en`+`ar`) for Home (granted/STOPPED/NO_SENSOR), Settings (6 sections, each of the 7 dialogs), Walk (idle/live/summary), Onboarding (4 pages), History (empty/data), Insights (empty/data), Diagnostics, About, Privacy. Re-run the gate once: 108 / 11 / 0 / 0.

### P1 — Structural splits, Δ0 (C5 precondition: no 500-line function may stay one)
- `SettingsScreen.kt:90–600` (511 lines) → `settings/SettingsSections.kt` (`ConnectionSection`, `GoalSection`, `DisplaySection`, `DataSection`, `AboutSection`, `DangerSection`) + `settings/SettingsDialogs.kt` (dialog bodies from `377–604`); root becomes a ~60-line composition. Δ0.
- `HomeScreen.kt:133–362` `HomeContent` (230) → `HomeStatus`, `HomeRing`, `HomeNotes`, `HomeChartLink`; `DiagnosticsScreen.kt:62–284` (223) → `DiagnosticsNow/Sources/Service/Data`; `HistoryScreen.kt:64–220` (156) → `HistoryDays/Walks`; `WalkScreen.kt:57–180` (122) → `WalkBody`. `OnboardingScreen` is already page-split (Welcome/Goal/Body/Permissions) — no P1 work. Δ0.
- Gate after each move; any screenshot delta in P1 is a bug (P1 is a pure move).

### P2 — Shared primitives + call sites · 2 days · **Δ−296**
| # | step | where | Δ |
|---|---|---|---|
| 1 | `StatCardGrid(entries: List<Pair<String,String>>, wideColumns: Int = 2)`, stacked when fontScale > 1.3 | `CommonComponents.kt` +25; `HomeScreen.kt:316–342` −21, `InsightsScreen.kt:59–107` −39, `WalkScreen.kt:274–310` −30, `386–407` −14 → net | −70 |
| 2 | `ChoiceDialog(options, selected, onSelect, onDismiss)` | `SettingsScreen.kt` units 418, language 450, gender 482 | −51 |
| 3 | `ConfirmDialog(title, body, confirmLabel, destructive, onConfirm, onDismiss)` | import 522, delete 576 | −16 |
| 4 | `SettingsRowShell` behind `SettingsClickableRow`/`SettingsSwitchRow`/`SettingsStepperRow`; the 16 `stacked = stackedRows,` call-site lines and 4 `stacked` params disappear (fontScale read inside) | `SettingsScreen.kt:686–817` | −50 |
| 5 | `InfoRowList(rows)` for 6 × `InfoRow`+`HorizontalDivider` | `DiagnosticsScreen.kt:144–183` | −23 |
| 6 | `InfoCard` to `CommonComponents.kt`; `AboutScreen.kt:41–59` inline card becomes a call; `SabeelCard` for the remaining identical wrappers | Privacy, About, Diagnostics, Walk, Onboarding | −19 |
| 7 | `ChoiceChips(options, selected, onSelect)` | `OnboardingScreen.kt` welcome/body/goal chip groups | −12 |
| 8 | channel spec table (id, nameRes, descRes, importance, badge, sound) | `Notifications.kt:52–88` | −15 |
| 9 | `StatusCard` merging `PermissionCard` + `CountingStatusCard` | `CommonComponents.kt:174–266` | −16 |
| 10 | one button core behind `SabeelPrimaryButton`/`SabeelSecondaryButton` | `CommonComponents.kt:314–370` | −10 |
| 11 | `StatCardGrid`/`SabeelCard` extra sites | Onboarding, Walk, Settings cards | −8 |
| 12 | `versionName(context)` shared by About + Settings | 1 helper | −6 |

### P3 — Screen-local compaction · 1.5 days · **Δ−77**
- Home stateful wrappers `HomeScreen.kt:56–91` (36 → 14): delete the uncalled 3-arg overload `74–91` (−18), trim the used wrapper (−4) = **−22**. `HomeNote(text, colour)` for the 5 centred notes at `255–263, 267–277, 279–285, 289–297, 299–307` = **−12**.
- `HistoryScreen`: one row composable for day rows `134–160` and walk rows `174–204`; `SavedWalkDialog` `222–270` renders `DetailRow`s from a list = **−30**.
- `WalkScreen`: `RouteCard(route, height)` for `325–346` + `415–427` = **−13**.

### P4 — AppViewModels · 1 day · **Δ−63**
Duplicate status flows `AppViewModels.kt:87–117` vs `:166–184` → one `ServiceStatusFlows` −20; per-screen ViewModel preamble helper (9 screens × 4 lines → 1–2) −12 (Expert prices 26; I price the conservative end); WalkViewModel's three surfaces `live`/`settings`/`state` `280–306` and WalkScreen `61–63` collapsed to `state` −10; `viewModelFactory` helper replacing 8 `companion object factory` blocks −12; `start`/`resumePending` `309–322` merged −5; onboarding setter `persist {}` `504–526` −4.

### P5 — data/repository · 0.5 day · **Δ−26**
`StepsRepository.recordRawStep` `97–115` + `reconcile` `116–152` share one `commitDay(...)` −15; `SettingsStore.update`/`toUserSettings` field mapping via one ordered list −6; `WalkRepository` (18 lines) folded into `StepsRepository` −5. No schema, DAO, DataStore key or Room-schema change.

### P6 — domain, tests are the witness · 0.5 day · **Δ−14**
`StepPipeline`: three `StepState(...)` constructions via one local `state(raw, steps, source, now)` −8 (19 tests). `StepSourcePolicy`: the availability ladder as a table over `SourceAvailability` −6 (6 tests). Any red = revert, never a test edit (T1).

### P7 — services + notifications · 2 days · **Δ−45** — riskiest, deliberately last
Shared foreground machinery: `foreground(service, id, notification, type)` + `update(...)` extracted from `StepCounterService.kt:599–635, 658–675` and `WalkTrackingService.kt:493–526, 721–733` (103 lines → ~58) = **−45**, exactly the Expert's measured 45 (`01-expert-first.md:55`). **No host merge, no lifecycle change**: both services keep their own type, stickiness, intents and teardown; only the four notification/foreground call sites move.

### P8 — leftovers + resources · 0.5 day · **Δ−25**
Delete the 3 dead members `StepSourcePolicy.bestMeasured`, `Permissions.hasStepSensor`, `Daos.getSince` (−15); `Reminders` worker-scope helper (−6); `CsvIo.HEADER` alias (−1); `Stepper` unused params + always-true `enabled` lines (−3). **Strings: no edit.** The 3 zero-reference keys (verified) stay: deleting them would trip the Expert's copy-freeze gate for 0 Kotlin lines — not worth it. No comment, blank line or test is touched anywhere: they are the 2,671-line floor.

**Total: −296 −77 −63 −26 −14 −45 −25 = −546.** Delivered: main 8,480, corpus 9,802. **Not in the plan:** T2 (it buys ~300–600 test lines, 0 on the primary metric, and loses failure locality — refused); import dedupe via merged files (god-file risk, Expert credits only ~90 lines — refused); any data-driven screen renderer (below).

## 4. Risk posture

**Riskiest step: P7**, the shared foreground/notification machinery — specifically the `startForeground`/`stopForeground` sequencing, not the notification text. One wrong order on Android 12+ crashes the walk start with `ForegroundServiceStartNotAllowedException`, one wrong type kills the service on 14+, one wrong `stopForeground` loses the step notification. Why this is real, not rhetorical: **0 of 108 tests touch a service**; the two hosts run concurrently with different declared FGS types (`AndroidManifest.xml:41–50`) and different teardown (`WalkTrackingService.kt:562–605` checkpoint flush vs StepCounter DB flush).
**Position:** last edit phase, behind six phases of green gates, one commit per service so a bisect has a clean target. **Why the order is safe:** risk rises monotonically — pure moves (P1) → shared UI (P2/P3, witnessed by screenshots) → ViewModels (P4, witnessed by screens) → data/domain (P5/P6, witnessed by tests) → services (P7, witnessed by device matrix). A P7 regression cannot hide behind unrelated churn, and no phase depends on a later phase's abstraction.
**The sentence that made this plan seem safe:** *"Every phase is pure de-duplication — the same composables, callbacks and notifications, just factored once, so if it compiles and the 108 tests and lint stay green, nothing can have changed."* It is **false for P3, P5 and P7**: the suite sees none of that code, so "green" means "compiles". The real witnesses are the screenshot matrix and the device matrix; the plan stands on those, not on the suite.
Other risks named: fontScale 1.4 breakpoint in settings rows (P2.4) — screenshots at 1.3/1.5 catch it; `HomeScreen` public-overload deletion and `Stepper` param removal are the only public-surface deletions (both uncalled; grep + compile witness; C6 untouched otherwise); no dependency, permission, manifest, schema or string change anywhere.

## 5. Verification (per phase; commands, observables, witnesses)

Gate after every commit: `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot`, `ANDROID_HOME=C:/Android/Sdk`, then
`./gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` → exit 0; test XML = **108 / 11 / 0 failures / 0 skipped**; lint report 0 errors. `:app:bundleRelease` at P0, P2, P7, close.
**Oracle frozen (T1), every gate:** `git diff --name-only -- app/src/test` empty **and** sha256 manifest match; `@Test` count 108.
**Metric gate, every phase:** `split.py` 5-tuple + tokens; accept only if |measured Δ − planned Δ| ≤ 20 % of the step **and** code lines carry ≥90 % of the raw delta (imports/comments/blanks may fall only as a consequence of deleted declarations). Any phase that fails is reverted, not re-labelled.
**Witness map (what fails if a deleted hunk was load-bearing):**
- P2.1/11 stat grids, P2.9 status cards, P2.10 buttons, P2.12 helpers, P3 Home/route/notes → screenshot matrix: Home 3 states × 3 fontScales × 2 locales, Walk live+summary, Insights, Settings; compare to P0, any unexpected pixel move reverts.
- P2.2/3 dialogs → open each of the 5 dialogs in both locales; cancel/confirm paths; dismiss by back.
- P2.4 rows, P2.6 cards, P2.5 info rows, P2.7 chips → Settings/About/Privacy/Diagnostics/Onboarding screenshots at fontScale 1.0/1.4/1.5.
- P2.8 channels → fresh install, Android Settings › Notifications shows the same 3 channels with the same names/descriptions; `dumpsys notification` channel list differs only in format, not fields.
- P4 preamble/factory/flows, P4 Walk surfaces → every screen renders; Home STOPPED/NO_SENSOR cards behave; manual start/stop counting; walk start/resume/save.
- P5 repo/settings → `StepMergeTest` 7 green; manual: change each setting, kill, reopen; CSV export→import round-trip unchanged (`CsvCodecTest` 12).
- P6 → `StepPipelineTest` 19 and `StepSourcePolicyTest` 6 green; no test file touched.
- P8 dead members → compile + grep zero call sites + 108 green; `Reminders`: schedule a reminder, confirm it fires; reboot path (BootReceiver) unchanged.
**UI, where 0 unit tests exist:** screenshots are the only C1 oracle; lint is **not** a Compose oracle (it checks no Compose semantics) and `LocalizationParityTest` covers strings only. Also: TalkBack spot-check on the two merged dialogs and the merged status card.
**P7 device matrix (accept/reject):** API 26 (minSdk), API 34, API 36 emulators + one OEM. Checks: (1) permission-denied cold start — no crash, Home shows the permission card; (2) count start/stop 20× — no `ForegroundServiceStartNotAllowedException` in logcat, `dumpsys activity services` shows the declared type while running; (3) walk: screen off 2 min — notification still updates, GPS still records; (4) force-stop mid-walk, reopen — checkpoint restored, no duplicate walk; (5) reboot — service re-armed, reminders still scheduled; (6) app kill mid-day — step count resumes with no double count.
**Close:** full CI command (added `bundleRelease`), metric report vs P0, screenshot matrix signed off, written C1 statement listing every deleted public symbol and the (zero) XML/string changes.

## 6. Readability contract (C5) and the metric that cannot be gamed

**Adopt the Expert's metric — "witnessed shrink"** (`01-expert-first.md:105–118`): a deleted line counts only if (i) it removes no user-visible behaviour or test assertion and (ii) its behaviour is observed green before and after by the frozen oracle. Report both **certified lines removed** and raw `wc -l app/src`; never optimise raw alone.
**M1** = raw `app/src/main/**/*.kt` (primary); **M2** = corpus; **M3** = artifact bytes (reported only; R8 makes it non-linear, so it is not a target).
A phase is **rejected** regardless of line win if: raw Δ misses plan by >20 %; code lines <90 % of the raw Δ; comment/raw or blank/raw ratio moves >10 % from baseline; any file >800 raw lines (P1 must bring SettingsScreen.kt from 908 under 700, final ≤700) or any function >120 lines (today 511 → P1 ≤120); a new helper has one call site or costs more than it saves (every table row above already nets helper cost); an abstraction changes the call shape it replaces (`@Suppress`, reflection, one-line/multi-statement lines, comment or blank-line stripping, tests weakened, resources with references deleted, files merged to reduce imports, `res/`/`build/`/generated code used to game the corpus).
**Why it survives:** no comment, blank line or test is removed by design (2,671 lines stay); the plan's net includes its new helpers; P1 lowers the worst file/function, so readability improves while raw falls; every step has a witness in §5 or is not in the table.

## 7. Rollback (no half-shrunk tree, ever)

One branch per phase `shrink/p0…p8`; one commit per step (≤3 per phase); gate after every commit; merge into `shrink/integration` only green; tag `shrink/pN-green`. A phase that misses its gate in its timebox: `git revert` its commits on the branch (never forward-fix past a red gate), delete the branch, record the reason; the tree returns to the last tag — that is the abandon path, and it cannot leave a half-applied step because no step spans commits. Abandon after merge: `git revert -m 1 <merge>` + same gate. Nothing in the plan touches Room schema, DataStore keys, the manifest or the build file, so a rollback is always source-only — no migration or data repair. `parts/metrics/p0…p8.json` + the 5-tuple history are stored per tag, so any claim can be re-derived from the tag, not from memory.
