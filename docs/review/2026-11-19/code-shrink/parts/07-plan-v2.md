# Beat 6 — Engineer, plan v2 (forced revision)

**Tomas Reyes — Engineer.** Owns the build; three "safe" refactors, each about a thousand lines removed, each broke exactly one thing. v2 exists because v1 did what that sentence warns about: it chose a metric first and called the consequence a floor.
**Read:** `02-plan-v1.md`, `04-attack1-advocate.md`, `05-attack1-customer.md`, `03-audit.md` A1–A14, `06-expert-dissection.md` §1–§7. Re-measured at HEAD `b687f16`: `git diff 7c48fe2..HEAD -- app/src app/build.gradle.kts` empty; **350 tracked files**; tracked text (pack F10 extensions) **286 files / 142,456 lines**; Kotlin **62 / 10,348**; tokens main 31,198 · test 4,455. **Writes:** this file only.

## 1. What I concede, with the corrected number

| # | v1 said | what is true | concession |
|---|---|---|---|
| 1 | "committed to 94.7%" (`02-plan-v1.md:25`) | −546 lines out of **142,456 tracked text lines = 0.38%** of the tree an agent opens; **85.15%** of that tree is `docs/legal/sources/` evidence | The customer is right. v1 answered "how many Kotlin lines leave `app/src/main`", not "how small is the project". v2 reports all three planes (§2) and moves the tree first (§3.1); both customer demands are now plan items. |
| 2 | P1 structural split, Δ0, no timebox (`02-plan-v1.md:37–43`) | imports are file-scoped: 68 import lines (`SettingsScreen.kt:3–70`), 26 locals (`:97–171`), 16 `stacked =` call sites (`:186…358`). Bundled-state ≥50 + re-imports 40–55; plumbing 80–130 → **Δ +80…+130**; the v1 gate `abs(Δ − 0) ≤ 20% of 0` is unsatisfiable for any nonzero edit; the goal-change task read goes 4 files → ~6 with the same 2,246 lines | Flaw 3 correct. **P1 is removed from v2**, priced at **1.5 days / +110 lines** if the owner ever buys it as readability debt. It is not a shrink phase; no later row depends on it. |
| 3 | P7 shared FGS machinery −45 (`02-plan-v1.md:76`) | the signature passes `type`/`notification` in, so both type ladders (`StepCounterService.kt:602–606` vs `WalkTrackingService.kt:501–507`), both builders (`:619–633` vs `:528–546`), both failure policies (`:607–611` vs `:508–516`), both start gates (`:658–675` vs `:721–733`) stay with the callers. Shareable = `if (started) return` + `runCatching(startForeground)` + flag ≈4–5 lines/site + throttled post ≈2–4 → **ceiling −10…−18** (Expert concurs, `06-expert-dissection.md:15`) | Flaw 2 correct on both legs. **P7 removed from v2**, deferred to a named trigger (§3.3), re-priced ≤ −18. Its witness does not exist: no `app/src/androidTest`, no recorded device run (B7, `docs/legal/BLOCKERS.md:146–159`), the saved AVD has no step counter, saved captures are blank (`04-attack1-advocate.md:29`). |
| 4 | dead surface "~0, fully enumerated", 15 lines (`02-plan-v1.md:19`) | audit A4: the detector sees only column-0 declarations. Real set **5 symbols / 27 raw lines**: `StepCadence.kt:21,48,111,138,147` `minuteKeys` (5, write-only), `WalkTrackingService.kt:760–766` `resetSession` (7, 0 call sites), `Permissions.kt:31–37` `hasStepSensor` (7), `StepSource.kt:57–62` `bestMeasured` (6), `Daos.kt:21–22` `getSince` (2) | v1 missed two symbols and its "~0" framing was wrong. P8 re-priced **25 → 34**. |
| 5 | "duplication is small (7.8%)" used to calibrate P2 (`02-plan-v1.md:19`) | audit A7: 149/676 duplicated lines (22.0%) sit in import windows; the denominator adds 978 import lines; semantic duplicates (formatters, status polls, day resets) score 0 | the calibration sentence is withdrawn. **No v2 row derives from `dup.py`**; rows are gross site lines minus measured helper cost. |
| 6 | the oracle: 180 captures inside P0's 0.5 day (`02-plan-v1.md:38`) | 136 cells × 80 s = **3.0 h/pass**; five passes = **15 h**; the 3×4 state matrix needs an FGS and SensorManager defaults v1 named no mechanism for (Expert §3) | v1's oracle was imaginary and is now the largest single cost line; it also gains the cells v1's own attacks said it lacked: 1.35/1.4 thresholds (A11), rotate-with-dialog-open (A13). |
| 7 | "I refuse to plan to 50%" (`02-plan-v1.md:33`) | the protocol reserves one refusal for v3 (`evidence-pack.md:82`; `04-attack1-advocate.md:17`); v1 spent it on the owner's question | **withdrawn, not spent twice.** The deletion branches are priced (§4) and put to the owner; the one refusal kept is §7 R. |
| 8 | "main 9,026 → ≤8,480, corpus ≤9,802" (`02-plan-v1.md:32`) | Expert ledger (`06-expert-dissection.md:19–30`): P4 43–53, P8 37, P7 10–18 → 503–521 → corpus **9,827–9,845**; v1's 9,802 is **25–43 below its own corrected floor** | v1 was optimistic, not conservative. v2 commits to rows it can witness: **corpus 9,852 / main 8,530** (§3). |

**Revision log — what forced what.** Advocate flaw 1 + customer denominator → §2 contract and §3.1 tree phase; Advocate flaw 2 → P7 out, trigger named; Advocate flaw 3 → P1 out and priced; audit A4 → 27-line dead set; A7 → no `dup`-derived rows; A8 → baseline at HEAD and tooling committed (R5); A1/A3/A6/A9/A10/A12 → rows cut or constrained plus a do-not-touch list (§3.4); Expert §1–§2 (`:19–§:51`) → P2 total kept but StatCardGrid and SettingsRowShell redesigned, status flows parameterised, StatCardGrid/status-flow residual risk named in §6; Expert §4 (`:67`) → L1/L2/L3 hierarchy; Expert §6 (`:77`) → the closing condition gap stated, not hidden (§3).

## 2. Metric contract — what "smaller" means, and what cannot be gamed

**Three planes, never summed** (`06-expert-dissection.md:67`). **L1 code plane:** every `.kt` the Gradle build compiles, from any module or source set — main 51 / 9,026 (6,615 code · 933 imports · 832 comments · 646 blank), test 11 / 1,322, corpus 10,348; tokens 31,198 / 4,455 / 35,653. Only L1 may be called a code shrink. **L2 task plane:** for three fixed tasks (change the goal default; add a settings row; resume an interrupted walk) the files, lines and tokens an agent must read and change — measured at P0, reported at close, **not claimed**; v1's only L2 effect was negative (P1: 4 files → 6). **L3 repo plane:** tracked text by class — reported with every phase, owned by the §4 decision, **never** presented as code compression.

| L3 class | files | lines | share | after P1 |
|---|---|---|---|---|
| legal evidence `docs/legal/sources/` | 73 | 121,300 | 85.15% | 0 (+73 manifest, +25 pointer) |
| Kotlin, all | 62 | 10,348 | 7.26% | 10,348 |
| app resources (xml/txt) | 24 | 1,191 | 0.84% | 1,191 |
| other docs and text | 127 | 9,617 | 6.75% | 9,617 |
| **total text** | **286** | **142,456** | 100% | **21,254** |

64 non-text tracked files (48 PNGs, 5 woff2, 1 ttf, jars, keystore, …) are outside the text metric and reported separately; untracked room artifacts (`parts/`, temp scripts) count nowhere until committed.

**Rules, binding on this plan and on anyone re-deriving it.**
R1 **No exclusion by relabeling.** Code moved to another directory, module, source set, variant or generated folder counts wherever Gradle compiles it. `app/src/main` shrinking because `ui`/`service` moved to `:core` is **0 on L1** and adds L3 (module build files); the Advocate's 35% row is a denominator, not a compression (§4 B7).
R2 **Moved and generated code counts.** A move counts at both ends until the source line is deleted; generated files count at generated size; codegen replacing source is a replacement, not a deletion.
R3 **A deleted line needs a named witness** observed before/after: unit tests (P5 `commitDay` → StepMergeTest 7; P6 → 19 + 6), screenshot cells (all UI rows), grep+compile (dead symbols), tag+manifest+legal signature (P1). A row without a witness is cut from the table, not from the code.
R4 **Tests frozen (T1).** `git diff --name-only -- app/src/test` empty after every phase and a SHA-256 manifest of the 11 files committed at P0; any test edit voids every claim below.
R5 **Tooling in the repo.** `tools/shrink/` (tree, tokens, split, dup, sha256) tracked, its own lines reported separately, so another seat can re-derive every number (A8: the pack's scripts are in `%TEMP%`).
R6 **Per-phase report:** L1 5-tuple for main/test + tokens; L3 by class; AAB bytes reported, never targeted (non-linear); row table with `file:line`, planned vs measured Δ, witness, observed-by, commit. Gate: Δ within 20% of plan **and** code ≥90% of raw Δ.
R7 **No plane laundering.** Never add L1+L3; never quote the P1 move as code percent; never quote L3 after P1 as a code result; every percentage names its plane.

## 3. Plan v2 — tree first, then code, every hour priced

| phase | work | hours | witness | Δ L1 | Δ L3 |
|---|---|---|---|---|---|
| P0 | baseline: L1 5-tuple + tokens, L3 by class, `tools/shrink/`, test manifest, clean 4-task gate, 136-cell baseline | 6.0 | 108/11/0/0; manifest; 3.0 h matrix | 0 | 0 |
| P1 | **tree: legal evidence archive** (§3.1) | 3.0 (2.0 eng + 1.0 legal) | 73/73 sha256; L3 re-count | 0 | **−121,202** |
| P2 | shared primitives (§3.2) | 19.0 (16 + 3 matrix) | matrix, all screens | −296 | 0 |
| P3 | screen-local compaction | 15.0 (12 + 3) | matrix: Home/Walk/History/Settings | −77 | 0 |
| P4 | AppViewModels | 11.0 (8 + 3) | matrix + dialog dismiss paths | −49 | 0 |
| P5 | data / repository | 6.0 (4 + 2 manual) | StepMergeTest 7 + export/import round-trip | −26 | 0 |
| P6 | domain | 4.5 (4 + gate) | StepPipeline 19 + StepSourcePolicy 6 | −14 | 0 |
| P7 | **removed** — trigger §3.3 | 0 | — | 0 | 0 |
| P8 | dead surface + leftovers | 5.0 (3 + 1 manual + 1 gate) | grep/compile; reminder fires | −34 | 0 |
| close | final matrix + 4-task gate + metric report + L2 measurement | 5.5 | matrix + gate | 0 | 0 |
| | **subtotal** | **75.0** | | **−496** | **−121,202** |
| | + 8 per-commit gate runs × 0.25 h = 2.0; revert/bisect buffer 3.0 | **+5.0** | | | |
| | **total** | **80.0** | | **10,348 → 9,852 (95.2%)** | **142,456 → 21,254 (14.9%, 6.70×)** |

**Committed:** L1 corpus **9,852 (−496; 95.2%)**, main **8,530 (94.5%)**, test **1,322 unchanged**; L3 **21,254 (−121,202; 14.9% of today, 85.1% removed)**. v1's 9,802 → 9,852 decomposes exactly (+50): **+45** P7 removed, **+10** A10 parameterised instead of chosen, **+4** A1 onboarding row cut, **−9** P8 25→34. 80 h = **47 h code + 18 h human witness + 15 h inventory/gates/legal/report/buffer**; per output line: L1 6.2 lines/h (9.6 min/line), L3 working tree 40,400 lines/h.
**Rows cut as falsified, with the reason:** P1 (Advocate flaw 3: Δ>0 by construction), P7 (Advocate flaw 2: signature caps it, no witness can run), P4 status flows 20→**10** (A10: one shared flow must keep 5 s and 3 s — parameterised, never one interval for both), P4 onboarding `persist {}` −4 (A1: the natural dedupe deletes `AppLocale.apply` and the activity never recreates), P8 `Reminders` 6→**2** (re-measured: 4+6 lines → 2+2 + 4-line helper), P2 row totals kept but redesigned (below). **Closing-condition gap, stated:** the Expert's closing condition asks corpus ≤9,845 (`06-expert-dissection.md:77`) — it includes P7 at 10–18, which I refuse without a witness. v2 lands **9,852, seven lines above**; the ruling should record 9,852, or 9,845 only if it accepts the P7 row unwitnessed, which I do not sign.

### 3.1 P1 — tree phase, 2 h + sign-off, numbered with ACs

1. Annotated tag `legal-sources-2026-11-19` on a commit whose `docs/legal/sources/` tree is byte-identical to HEAD's (73 files, 121,300 lines); tag message records file count and legal sign-off.
2. `docs/legal/SOURCES.sha256.txt` — 73 lines, `sha256sum` format; `sha256sum -c` against a tag checkout passes **73/73**.
3. `docs/legal/SOURCES.md` — 25 lines: §6 handling rules verbatim (`docs/legal/sources/README.md` §6, F15), tag name, manifest path, and "archived, not destroyed: evidence is citable by hash".
4. `git rm -r docs/legal/sources` (73 files) + the two files above; one commit, owner/legal sign-off in the body.
5. After: `git ls-files` = **279** (−71); tracked text **21,254** (−121,202, 14.9%, 6.70×); nothing under `app/`, `.github/`, `gradle/`, `settings.gradle*` or build files changes; `git grep -n "docs/legal" -- app/src/test` = 0, so 108/11/0/0 cannot be affected by construction.
6. Reversal `git revert` (tag and history stay), 5 minutes.
7. If legal refuses, P1 is abandoned, L3 stays 142,456 **and is printed as such**; no code phase depends on P1.
**Honesty note:** clone/history does not shrink — the 121,300 lines remain in the tag's objects. What shrinks is the tree an agent, IDE or checkout opens; clone size is a separate artifact-branch decision, not priced here.

### 3.2 Code rows — what each is, and the constraint that keeps it behaviour-free

**P2 shared primitives −296**, twelve rows, totals unchanged from v1 but each gross-minus-helper: `StatCardGrid` **−70** (gross 98, helper 22; signature `cards: List<Pair<String,String>>, stacked: Boolean, columns: Int` — all three vary at sites 2/2/3/1 true/false, so no fixed parameter and no spec type; stacked is passed in, never read inside, so A11's 1.3 cannot leak) · `ChoiceDialog` −51 (flags stay at call sites, A13) · `SettingsRowShell` −50 (**`stacked` passed in from the call site; threshold 1.4 never moves inside**) · `InfoRowList` −23 · `ChoiceChips` −12 · card/panel merges −19 · channel table −15 (same resource ids; no importance/sound change) · `ConfirmDialog` −16 (erase ordering untouched, A3) · `StatusCard` −16 · button core −10 · extra sites −8 · version-name read −6.
**P3 −77:** Home 3-arg overload 18 + wrapper trim 4 (A5) · `HomeNote` 12 · `HistoryScreen` 30 · `RouteCard` 13.
**P4 −49:** status ladder + poll **parameterised** 10 (Home 5 s at `:96,115`, Diagnostics 3 s at `:175,182` passed per site — A10) · preamble 12 · factory 12 · start/`resumePending` 5 · Walk surfaces 10.
**P5 −26:** `commitDay` 15 (StepMergeTest 7) · settings mapping 6 · `WalkRepository` fold 5.
**P6 −14:** pipeline state helper 8 (StepPipeline 19) · source-policy table 6 (StepSourcePolicy 6).
**P8 −34:** dead 5 symbols/27 (grep-zero) · `Stepper` `canDecrease/canIncrease` 4 (5 call sites, none passes them, defaults true) · `CsvIo.HEADER` 1 (0 refs) · `Reminders` settings extraction 2.
Any row measured short of plan by >20% is cut and re-derived, not padded by the next row; the plan's total moves down with it and the table is reprinted before the next commit.

### 3.3 P7 trigger (stated, not silently dropped)
Reopen only when **(a)** a recorded physical-device matrix exists with B7 closed, **or (b)** an instrumentation test covers both services' start/stop paths. Then re-price at ≤ −18, never −45. Absent both, P7 stays out and none of the −496 depends on it.

### 3.4 Do-not-touch list (tempting, measured, refused for now)
A1 `AppLocale.apply`/`syncLanguage` (recreate + language reload, no oracle) · A2 unguarded sensor-missing strings · A3 `deleteAll` erase ordering (`StepCounterService.kt:649,678–685`) · A6 the two JSON codecs · A9 formatter ASCII-vs-localized divergence · A10 only parameterisation, never one interval for both polls · A11 thresholds 1.3/1.4/1.5 move into no shared component · A12 three day resets. Each would save lines and each would change user-visible behaviour or erase the only witness.

## 4. Answer to 2×/4×/10× — priced, and put to the owner

Basis: L1 10,348 (2× = 5,174, 4× = 2,587, 10× = 1,035); L3 142,456 (2× = 71,228, 4× = 35,614, 10× = 14,246).

| branch | what happens | measured result | C1 consequence | price / owner |
|---|---|---|---|---|
| B1 tree 1× | P1 (§3.1) | tree 21,254 = **14.9%**, 6.70× | none to the product; evidence citable by tag+hash | 3.0 h; legal co-signs |
| B2 tree 2× | B1 alone | 21,254 ≤ 71,228 ✓ | none | 0 extra |
| B3 tree 4× | B1 alone | 21,254 ≤ 35,614 ✓ | none | 0 extra |
| B4 tree 10× | B1 + archive all md/html/css/mjs (md 6,273, html 445, css 154, mjs 136), keep a 5-line pointer as txt | 14,251 = **10.0%** (5 lines over exact 14,245.6); + `app/schemas` json 126 → 14,125 = 9.9% | READMEs and docs leave the working tree; agents lose local docs | +2 h; owner/legal |
| B5 code 2× by deleting UI | delete all `ui` 5,021 + rewrite T2 tests ≈300 | 5,027 = **48.6%** ✓ | **C1 false — no screens**; C2 moves to T2 | 0 dev + ~24 h test rewrite/QA; product dead |
| B6 code 2× by features | walk 1,462 + diagnostics/insights + VM 559 + onboarding 399 + history 284 = 2,704 | 7,644 = 73.9% — still 2,470 over 2×; the rest is Home+Settings+shared (≈2,317) | **C1 false** and the target still missed | ~30 h; owner |
| B7 relocating to a second module (Advocate row) | move `ui`/`service` into `:core` | `app/src/main` 2,295, corpus 3,617 = **35.0%** by denominator only; **L1 stays 10,348**, L3 +≈40 (module build files) | none — but it is repo layout, **0% compression** under R1 | 0.5 day if wanted; no metric claim |
| B8 strip comments/blanks/imports | 2,671 lines (25.8%) | 7,677 = 74.2% — not even 2×; tree gains **1.9%** | C5 broken; refused (§7 R) | — |
| B9 code 25% / 10% | — | 25% = 2,587 < 6,615 lines of live main code (a 1,265-line main cannot hold them); 10% = 1,035 < frozen tests 1,322 | impossible under C1+C2 (`06-expert-dissection.md:71`) | arithmetic |

**Decision, dated and owned: 2026-11-20 12:00 Asia/Baghdad, owner = product owner (customer seat); legal co-signs B1/B4.** Default if silent: **B1 + all code phases**; no B5/B6/B8 work starts without an explicit yes, and a yes to B5/B6 must name the C1 exemption in the ruling (`ruling.md`).
**Module-extraction answer, directly:** the Advocate's 2,295 is a denominator change under filenames (`04-attack1-advocate.md:60`). R1 counts every Gradle-compiled Kotlin line wherever it sits, so B7 is **0%**; the real structural split is v1's P1 with the Δ cost conceded in §1 row 2, and it is priced there, not smuggled in as a phase.

## 5. Verification — what green means, and what is only owed

- **Gate (C4), after every commit:** `./gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease`; expect **108 tests / 11 suites / 0 failures / 0 skipped**; `lintDebug` no new issues vs P0.
- **T1 (C2):** `git diff --name-only -- app/src/test` empty and manifest equal at every phase.
- **Screens (C1):** 136-cell matrix per pass — 30 states × {1.0, 1.5} × {en, ar} = 120, plus 16 threshold cells at 1.35/1.4 for Home/Insights/Walk/Settings — diffed to the P0 baseline and adjudicated by the owner; 3.0 h/pass, five passes.
- **A11/A13 targeted:** threshold cells 1.35 and 1.4, and rotate with each dialog open (choice, confirm, import) in both locales.
- **P8:** `git grep -n "<symbol>" -- app/src` returns the declaration only; compile green; reminder actually fires (`adb shell cmd jobscheduler run`, 1 h manual).
- **P5/P6:** existing tests green (7 + 19 + 6 named); no new test file.
- **P1:** `sha256sum -c docs/legal/SOURCES.sha256.txt` 73/73 from a tag checkout; L3 re-count with `tools/shrink/tree.mjs`; 4-task gate.
- **L2 at close:** the three named tasks measured (files/lines/tokens) before and after; **reported, not claimed**.
- **Owed, not green:** physical walk tracking (B7), TalkBack copy and traversal order, real DataStore upgrade paths beyond unit tests.

## 6. Risk posture — the riskiest row and the sentence that makes v2 sound safer than it is

**Riskiest: P2 (296 lines, 12 rows, untested UI).** Inside it the stat grids (gross 98, helper 22) — the biggest row, and the only one that changes card layout on every screen at once. Mitigation: `cards`, `stacked`, `columns` all vary at call sites, so it passes the Expert's test (`06-expert-dissection.md:51`); stacked is computed by each screen and passed in, so A11's divergence is preserved; witness is the matrix at 1.0/1.35/1.4/1.5; it is the first row to revert if a cell moves. Residual: the Expert recommends cutting this row and the status flows outright — if attack 2 sustains that, P4 drops to 39 and the commit becomes 9,862; I will not pad other rows to keep the number.
**The sentence that makes v2 seem safe:** *every row is either code the tests can see or layout the screenshots can see.* **It is false for three things**, and v2 pays rather than pretends: import-dialog state across rotation (A13) is invisible to screenshots → rotate-with-dialog-open check added; 1.31–1.41 renders are not 1.0/1.5 → 1.35/1.4 cells added; TalkBack copy/order is invisible to pixels → `Stepper` semantics are unchanged because both removed parameters default `true` and are never overridden, but accessibility stays named unverified.
**Why C1 holds:** every row deletes duplicated layout structure, dead symbols or parameter plumbing; no string, threshold, format, erase-ordering, day-reset or locale path is touched (§3.4). C3 holds: no dependency, permission or network change — every row is a move or deletion inside existing code.

## 7. The one refusal (kept for v3, defended now)

**R — I will not delete comments, KDoc, blank lines or imports, nor skip or weaken tests, to reach a number.** Those lines are 2,671 of 10,348 (25.8%): stripping all of them yields 7,677 (74.2%) — it does not reach even 2×, it buys **1.9%** of the tracked tree, it destroys C5 (the metric stops being falsifiable and the code stops being readable for humans, machines and agents), and it removes the witness P5/P6's savings rest on. Falsification test of my refusal: produce a task-read (L2) or gate-time win from stripping that survives C1–C4 and is worth that loss; none exists — the customer's complaint is a 142k-line tree, answered by P1, not by stripping.
**Considered and not kept:** (b) refusing to price 2× — I price it (§4) because the owner, not the engineer, owns C1; (c) refusing the legal move — delegated to owner/legal with terms (§3.1) because the evidence rule is theirs.

## 8. Rollback

One commit per row; tag `shrink/pN-green` per phase after the gate; P1 revert keeps tag and manifest and no code phase depends on it. A failed row is reverted whole, never half-patched under a 4-task gate. Closing condition: 4-task gate green on the final commit, L1/L2/L3 report committed, ruling decision box answered — target **2026-12-10** (`06-expert-dissection.md:77`).
