# Beat 1 — Expert, called first

**Nadia Okafor — Expert.** In 2023 I took a 9,000-line Android fitness app through a "halve it" refactor. Six weeks of it were reverted because the unit suite never touched a screen: every "simplified" screen passed the gate and failed the user. I have read code for a living since. I do not accept a shrink number whose witness I cannot name.

**Method.** Read the evidence pack in full (F1–F15), then the code. Re-derived the corpus independently (node over `app/src`, `splitlines` counting): **62 files, 10,348 lines = 7,677 code + 978 imports + 889 comments + 804 blank**, matching F2–F4 at every package boundary. Read 50 of 51 main files end to end (`Color.kt:1–40` only); all 11 test files as signatures plus assertion inventory (`@Test` sum = 108, F5 reproduced). No repo file modified.

## 1. What this is, from first principles

It is not a generic fitness app with screens. It is **five machine-checked invariants with a UI attached**:

1. **Step accuracy pipeline** — rebase on reboot/process death, midnight rollover, source-switch gap, impossible-catch-up clamp: 19 tests (`StepPipeline.kt:1–161`, `StepPipelineTest.kt:16–257`).
2. **Source ladder + stall watchdog + motion probe** — counter→detector→estimate, probe back-off, fallback toggling, rebase on switch: 15 tests (`StepSource.kt:42–81`, `StepSourceHealth.kt:11–99`, driven by `StepCounterService.kt:281–464`).
3. **Persistence invariants** — max-rule day merge, pipeline-before-row ordering, RFC-4180 CSV round trip: 19 tests (`StepsRepository.kt:30–45, 97–216`, `CsvCodec.kt:1–174`).
4. **Two concurrent foreground services** with different Android 14+ FGS types, permission gates and death-recovery paths (`AndroidManifest.xml:41–50`; `StepCounterService.kt:133–207`; `WalkTrackingService.kt:117–166, 667–691`).
5. **Exact copy in two locales** — 260 strings + 4 plurals per locale in `strings.xml`, parity/placeholder-tested, plus 26 sensor strings per locale in `strings_sensor.xml` that the parity test does not read (`LocalizationParityTest.kt:126–132`).

Sized file by file (raw lines, measured):

| where | files → lines | total |
|---|---|---|
| root | SabeelApp 13 · MainActivity 34 · AppContainer 31 | 78 |
| domain | StepPipeline 161 · StepMath 146 · MotionStepDetector 121 · StepSourceHealth 99 · GoalAdvisor 94 · StepSource 81 · Models 49 | 751 |
| data | StepsRepository 221 · CsvCodec 174 · SettingsStore 143 · Entities 90 · CsvIo 67 · Daos 53 · WalkRepository 18 · AppDatabase 14 | 780 |
| platform | Notifications 246 · Reminders 141 · StatusStore 104 · OemBatteryGuide 63 · BootReceiver 53 · Permissions 41 · StepSensors 38 | 686 |
| service | WalkTrackingService 778 · StepCounterService 693 · StepCadence 151 · WalkTracker 88 | 1,710 |
| ui | screens 3,027 (Settings 908 · Walk 433 · Onboarding 399 · Home 370 · Diagnostics 318 · History 284 · Insights 151 · Privacy 93 · About 71) · components 868 (Common 371 · Ring 161 · Chart 135 · Scaffold 125 · Route 76) · ViewModels 547 · root 203 · theme 191 · format 117 · i18n 68 | 5,021 |
| test | 11 files, 108 `@Test` | 1,322 |
| **total** | **62 files** | **10,348** |

Except the 9 dead code lines named in §3.4, every line is reachable from a product surface or a test. Nothing here is scaffolding waiting for a feature.

## 2. Where the 10,348 lines actually go

One line, one category (comments/imports/blank by lexer; the rest is code). (b) is itemised in §3 — it is what can be deleted without losing behaviour.

| package | raw | code | imports | comments | blank | (a) behaviour | (b) ceremony | (c) language |
|---|---|---|---|---|---|---|---|---|
| ui | 5,021 | 3,887 | 667 | 221 | 246 | 3,569 | **318** | 1,134 |
| service | 1,710 | 1,232 | 103 | 235 | 140 | 1,187 | **45** | 478 |
| test | 1,322 | 1,062 | 45 | 57 | 158 | 1,062 | 0 | 260 |
| data | 780 | 541 | 77 | 72 | 90 | 519 | **22** | 239 |
| domain | 751 | 501 | 9 | 156 | 85 | 496 | **5** | 250 |
| platform | 686 | 419 | 56 | 138 | 73 | 402 | **17** | 267 |
| root | 78 | 35 | 21 | 10 | 12 | 35 | 0 | 43 |
| **total** | **10,348** | **7,677** | **978** | **889** | **804** | **7,270** | **407** | **2,671** |

(c) is 25.8% of the corpus and is not available: imports are required, blanks are syntax hygiene, and the 889 comment lines carry the accuracy rationale (C5). Its one legitimate defence is deduplication: 933 main import lines cover only 340 distinct symbols — 593 duplicate copies — but collecting them requires god-files, which C5 forbids, so I credit only ~90 lines of small-file merges.

Therefore the whole behaviour-free budget is **407 code lines = 5.3% of code**, plus ~55 blank/comment lines inside the deleted blocks and ≤90 import lines: **~552 raw lines (5.3%)**. The other 7,270 code lines are the product and its test contract. The Compose ceremony in F8 (228 `Modifier` occurrences, 49 `Spacer`) is how behaviour is *expressed*, not a separate pile to delete: a `Modifier.padding` line is layout behaviour, and deleting it moves pixels, which C1 forbids. My own scan finds 322 layout-opener lines and 702 closing-brace lines in ui; none of them can be deleted without deleting the node whose syntax they are.

## 3. Four qiyas

*Qiyas 1 — data-driven UI in place of hand-written screens.* aṣl: descriptor/schema-driven rendering works where screens are lists of uniform rows (settings-list builders, preference inflation). ʿilla: one row schema covers N rows, so generator cost is amortised. **Where it breaks here:** the "rows" are distinct kinds with per-row side effects (`SettingsScreen.kt:176–373`: steppers write three preference keys; switches set a rest day and arm WorkManager reminders behind a runtime permission request; rows launch two document pickers, four selector dialogs, three nav destinations), and three screens are canvases with no row structure (`ProgressRing.kt:42–161`, `BarChart.kt:1–135`, `RouteSketch.kt:1–76`, 251 code lines). A generic renderer must encode every kind *plus* the fontScale branches (`HomeScreen.kt:146–164`, `WalkScreen.kt:274–310`, `SettingsScreen.kt:686–817`); its size grows toward the sum of its cases. **farʿ here:** 245 code lines (stats 70, chooser dialogs 51, row primitives 55, info lists 42, chip groups 12, channel table 15), 6.3% of ui code.

*Qiyas 2 — merge the two foreground-service hosts.* aṣl: consolidate hosts that share lifecycle, notification and FGS-type ceremony. ʿilla: the host bodies differ only in payload and the lifecycle contract is identical. **Where it breaks here:** the services run *concurrently* (a walk records its own steps from the same sensors while the step service counts: `WalkTrackingService.kt:371–450`), they hold different declared types (`health` vs `location|health`, `AndroidManifest.xml:41–50`), different permission gates (`StepCounterService.kt:139–142` vs `WalkTrackingService.kt:123–127`), different stickiness and intents (START_STICKY with no action vs `ACTION_START/ACTION_STOP`: `WalkTrackingService.kt:143–165`), and different teardown (DB flush vs JSON checkpoint: `WalkTrackingService.kt:562–605, 667–691`). Merging hosts moves the ceremony inside one class; it does not delete it, and it risks serialising two products. **farʿ here:** extract the shared foreground/notification/companion machinery — 103 shared lines become a ~58-line helper, saving **45 lines — not a merge**.

*Qiyas 3 — replace hand-rolled widgets with platform/Material components.* aṣl: swap custom Canvas drawing for `CircularProgressIndicator`/Material charts. ʿilla: the custom widget's affordances are a subset of the platform component's, so the swap is pixel- and semantics-neutral. **Where it breaks here:** the premise is false. The ring carries a two-stop sweep gradient, seam workarounds, a goal badge and one custom TalkBack node (`ProgressRing.kt:61, 70, 97–128`); the chart draws seven bars plus a dashed goal line with explicit RTL mirroring and measured contrast (`BarChart.kt:42–47, 91–92`); the route sketch draws the walked polyline (`RouteSketch.kt:1–76`). Material 3 has no chart and no route primitive; a chart dependency violates C3. **farʿ here: 0 lines.** The 251 lines are the price of the visual identity: replacing them changes pixels (C1) or adds a dependency (C3).

*Qiyas 4 — delete one-use indirection.* aṣl: inline wrappers that have one call site and no policy. ʿilla: indirection pays only from ≥2 implementations, ≥2 call sites, or a test seam. **Where it breaks here:** many single-use seams are deliberate C6 surface (`AppContainer.kt:11–31`; repository classes; `ViewModel.factory`; published pure functions), and deleting them moves coupling into UI/services. **farʿ here:** the genuinely dead symbols are `StepSourcePolicy.bestMeasured` (`StepSource.kt:57–62`, zero references), `Permissions.hasStepSensor` (`Permissions.kt:31–37`, zero), `Daos.getSince` (`Daos.kt:21–22`, zero) — 9 code lines. Add wiring that exists only to forward: two HomeScreen stateful wrappers (`HomeScreen.kt:56–91`, one call site at `SabeelRoot.kt:161`), the copy-pasted per-screen ViewModel preamble (9×, ~36 lines), WalkViewModel's three overlapping state surfaces (`AppViewModels.kt:283–299` while `WalkScreen.kt:61–63` collects all three), the duplicated Home/Diagnostics status flows (`AppViewModels.kt:87–117` vs `:166–184`), the duplicated repository write paths (`StepsRepository.kt:97–152`), and the 18-line `WalkRepository` whose class shape duplicates `StepsRepository`: **117 lines**. Not the 1,000+ the slogan implies.

**The 407, itemised** (current sizes are measured from the files; replacement sizes are estimates, shown so the arithmetic can be attacked line by line):

| # | candidate | now | after | save |
|---|---|---|---|---|
| 1 | adaptive stat sections (`HomeScreen.kt:316–342`, `InsightsScreen.kt:59–107`, `WalkScreen.kt:274–310, 386–407`) | 135 | 65 | 70 |
| 2 | three chooser dialogs (`SettingsScreen.kt:417–519`) | 101 | 50 | 51 |
| 3 | three row primitives (`SettingsScreen.kt:686–817`) | 132 | 77 | 55 |
| 4 | Diagnostics info rows (`DiagnosticsScreen.kt:144–184`) | 41 | 18 | 23 |
| 5 | Privacy info cards (`PrivacyScreen.kt:28–56`) | 29 | 10 | 19 |
| 6 | onboarding chip groups (`OnboardingScreen.kt:231–242, 338–357`) | 31 | 19 | 12 |
| 7 | notification channel table (`Notifications.kt:52–90`) | 39 | 24 | 15 |
| 8 | two Home stateful wrappers (`HomeScreen.kt:56–91`) | 36 | 14 | 22 |
| 9 | per-screen ViewModel preamble (9 screens) | 36 | 10 | 26 |
| 10 | WalkViewModel three state surfaces (`AppViewModels.kt:283–299`) | — | — | 20 |
| 11 | duplicate status flows (`AppViewModels.kt:87–117` vs `:166–184`) | 50 | 30 | 20 |
| 12 | duplicated repository write paths (`StepsRepository.kt:97–152`) | 56 | 41 | 15 |
| 13 | dead symbols (`StepSource.kt:57–62`, `Permissions.kt:31–37`, `Daos.kt:21–22`) | 9 | 0 | 9 |
| 14 | merge `WalkRepository` into `StepsRepository` | 18 | 13 | 5 |
| 15 | shared foreground-service machinery (`StepCounterService.kt:599–635, 658–675`; `WalkTrackingService.kt:493–526, 721–733`) | 103 | 58 | 45 |
| | **total** | | | **407** |

## 4. What the arithmetic permits

**T1 floor: ~9,796 lines (−5.3%, 94.7% of today).** 10,348 − 407 ceremony − 55 block padding (estimate) − 90 import dedupe (estimate from merging six small file pairs). Range 9,780–9,850.
**T2 floor: ~9,473 (−8.5%).** T1 minus 23 lines of production code that exists only because tests call it (`StepSourcePolicy.nextMeasured`, `canEstimate`, `StepMath.formatPace`, `formatCalories`, `DateKey.epochDay` — 17 code + 6 KDoc, all referenced only from tests) minus ~300 lines from rewriting the 108 test methods as tables (estimate; failure locality is the cost).

| target | lines allowed | what must be true | verdict |
|---|---|---|---|
| 50% | 5,174 | 57–78% of the 6,615 main code lines must vanish (proportional vs fixed overhead) | **impossible under C1**: the entire measured removable budget is 407 (6.2% of main code) |
| 25% | 2,587 | — | **impossible by arithmetic**: the import/comment/blank floor alone is 2,671 |
| 10% | 1,035 | — | **impossible by arithmetic**: the frozen test files alone are 1,322 |

Under T2 the three verdicts do not move: the overhead floor barely changes when tests are rewritten (main imports/comments/blanks alone are 2,411), so 25% leaves ~96 code lines for 108 behaviours and 10% is below the main overhead floor by itself; 50% would still need more than half of main code gone. 50% is not "hard". The pack's table says it needs ~53% of main logic gone; measured duplication (676 normalised lines, F6) plus all measured ceremony (407) plus every dead line does not reach half of that, and the 676 are mostly product copy in About/Privacy/Onboarding. A 5,174-line Sabeel is a different product, not a smaller one. The ruling belongs to the Maestro; the numbers are these. For the other metric on the table: main-only raw lines (9,026) would floor at ~8,474 under T1 (−6.1%), and repo-text lines are dominated by 121,592 lines of legal HTML (F10) — a legal decision (F15), not code.

## 5. The boundary that decides the outcome

The room's invariance guarantee is only as strong as its oracle, and the oracle is small. The 11 test files import exactly four of this project's symbols by name (`R`, `MergeOutcome`, `StepMerge`, `DailyStep`); everything else they exercise is same-package pure Kotlin. No test touches `ui/`, `service/`, `platform/` (except pure `OemBatteryGuide`), `data/db`, `data/prefs`, `CsvIo`, notifications or receivers. **The frozen oracle observes 1,106 raw lines — 10.7% of the corpus.** The other 9,242 lines have no automated witness, and there is no `androidTest` source set at all.

That is the boundary. Every plan that reaches a large number must delete lines in the 89% the gate cannot see; deletion there is unverifiable by CI and by T1. The outcome is therefore decided by a *scope* question — may the product change? — not by a code technique. If C1 stands and the oracle stays as it is, the honest shrink is 5–9% and every additional line "saved" is a risk taken without a witness.

**The one question every plan will be judged against:** *for each line you delete, what observation fails if that line was load-bearing — which of the 108 tests, which golden screen state, which runtime measurement — and what is `wc -l app/src` afterwards?* A hunk that cannot name a failing observation is not shrink; it is an unverified change.

I will speak twice: now, and at beat 5 on what v1's deletions guarantee in thirty days (a shared primitive every future feature must modify; test tables that lose failure locality; import dedupe bought with a file that taxes every future read).

## 6. The metric

Name it **witnessed shrink**: a line counts only if (i) its deletion removes no user-visible behaviour or test assertion, and (ii) the behaviour it carries is observed green before and after by a frozen oracle. Report the pair — certified lines removed **and** raw `wc -l app/src` — and never optimise the raw number alone.

Machine-checkable gates, all runnable in CI:

- Oracle frozen: `git diff --exit-code app/src/test` at T1; at T2, `@Test` count = 108 and the tested-symbol list unchanged.
- Corpus pinned: the `find app/src -name '*.kt' | xargs cat` corpus from F2, no exclusions, no relocation of lines into `res/`, `build/` or generated code.
- Readability floor: comment/raw ratio per file ≥ baseline; KDoc on every public declaration; no `;`; max line length 120; max file 800 lines; max function 120 lines (this flags `SettingsScreen()`'s 511-line body, `SettingsScreen.kt:90` — desired).
- Copy frozen: `strings.xml` key set and values byte-identical in both locales, and the 26×2 sensor strings added to parity first; 9 routes + 4 tabs unchanged (`SabeelRoot.kt:51–70, 151–203`).
- Build surface frozen: no new dependency, permission or INTERNET; `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `bundleRelease` green.
- Witness map: one row per deleted hunk — hunk id, witness (test name / screenshot state / measurement), and the human sign-off for oracle-blind hunks.

It must forbid, explicitly: deleting, skipping or weakening tests; stripping comments or KDoc to buy lines; one-lining and multi-statement lines; god-files; deleting blank-line structure; moving code out of the measured corpus; claiming import cleanup of still-used symbols; counting the 121,592 lines of legal HTML (F10) — a legal decision (F15) — as shrink; and redefining the corpus or the oracle after the plan is accepted. With those gates the numerator is printed by one command, the denominator is owned outside the plan, and the only profitable move is deleting a line that no observation covers — which the rule classifies as unverified, not as shrink.
