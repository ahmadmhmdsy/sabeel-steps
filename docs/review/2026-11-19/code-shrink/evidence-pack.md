# Review room — evidence pack

**Room:** Sabeel code-shrink room · **Convened:** 2026-11-19 (Asia/Baghdad)
**Target:** `E:\android_apps\steps_app` — Android app **Sabeel (سبيل)**, Kotlin 2.0.21 + Compose (Material 3)
**Revision:** `7c48fe2` on `main`. `app/src` is clean at this commit; `git status --porcelain` shows 7 modified docs/README/dist files and 2 untracked docs — no source file is dirty.
**Mode:** standard (2 attack rounds + 1 independent audit) · **Recorder + Maestro:** the Lead
**Question on trial:** *Can this source be compressed to ≤50% of its size — and how far below that is honest — while the user-visible product, the tested accuracy, reusability/extensibility, and readability for a human, a machine and an agent are all preserved?*
**Deliverables:** `docs/review/2026-11-19/code-shrink/review-room-transcript.html`, `session.json`, `ruling.md`.

## Hard constraints (the user's invariance, not the room's preference)

- C1 **No user-visible change.** Screens, copy, behaviour, accuracy, permissions, battery posture, offline/no-INTERNET invariant: identical.
- C2 **No test change required** — the 108 existing tests must still pass; ideally the test files are byte-identical (interpretation T1), at minimum the 108 assertions and their coverage survive a rewrite (T2).
- C3 **No new runtime dependency**, no new permission, no network.
- C4 **CI gate stays green:** `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `bundleRelease` (`.github/workflows/android.yml`).
- C5 **Readable for machine, agent and human.** "Smaller" must not mean obfuscated, comment-stripped, truncated names, or one god-file. The room must name a metric that cannot be gamed.
- C6 **Reusable and expandable:** public types/behaviour stay stable enough that a future feature (widget, Health Connect export) can be added without reopening the shrink.

## Measured facts (beat 0)

Every row was produced in this session; re-runnable commands are given.

| # | Claim | How verified | Value |
|---|---|---|---|
| F1 | Tracked tree size | `git ls-files` | 346 files |
| F2 | Kotlin corpus | Python `split.py` over `app/src` | 62 files, **10,348 raw lines**: 7,677 code · 978 imports · 889 comments · 804 blank |
| F3 | Split main/test | same | **main 51 files / 9,026 raw** (6,615 code · 933 imports · 832 comments · 646 blank); **test 11 files / 1,322 raw** (1,062 code · 45 imports · 57 comments · 158 blank) |
| F4 | Package split | Python `dead.py` | ui 5,021 · service 1,710 · test 1,322 · data 780 · domain 751 · platform 686 · app-root 78 |
| F5 | Gate is green | `:app:testDebugUnitTest --rerun-tasks` exit 0 (JDK 21, offline) | **108 tests, 11 suites, 0 failures, 0 skipped**; `:app:lintDebug` exit 0 (1m43s) |
| F6 | Duplication is small | Python `dup.py` (normalized 6-line windows across files) | **676 of 8,655 code lines = 7.8%**; top pair AboutScreen↔PrivacyScreen (8 shared windows), Onboarding↔Walk (7) |
| F7 | Biggest functions | Python `metrics.py` | `SettingsScreen()` is **511 lines** (`SettingsScreen.kt:90`); HomeContent 230 (`HomeScreen.kt:133`); DiagnosticsScreen 223 (`DiagnosticsScreen.kt:62`); History 156; Onboarding 127; SabeelRoot 126; Walk 122; ProgressRing 120. **27 of 345 main functions ≥45 lines** |
| F8 | UI corpus | Python `metrics.py` | 72 `@Composable`, 464 `fun`, 28 data classes, 316 `stringResource(`, 206 `Text(`, 83 `Row(`, 61 `Card(`, 49 `Spacer(`, 228 `Modifier` |
| F9 | Resources | `strings.xml` ×2 locales + `strings_sensor.xml` ×2 | 260 strings + 4 plurals per locale (316 / 332 lines), 26 sensor strings ×2 (36 / 35 lines); `LocalizationParityTest.kt` enforces identical key sets and placeholders |
| F10 | Repo text, not code | Python over `git ls-files` | **.html 121,592 lines** (24 files, third-party legal snapshots in `docs/legal/sources/`), .md 6,182 (39), .txt 2,222 (72), .kt 10,348, .xml 1,215 (78), .kts 192, css 154, mjs 136, json 126, yml 74 |
| F11 | Build surface | `app/build.gradle.kts` | 21 dependency lines (Compose BOM 2024.12.01, Room 2.6.1 + KSP, DataStore 1.1.1, WorkManager 2.10.0, Navigation 2.8.5, lifecycle 2.8.7); minSdk 26 / target 36; R8 + resource shrinking on release |
| F12 | CI gate | `.github/workflows/android.yml` | ubuntu, JDK 21, `assembleDebug + testDebugUnitTest + lintDebug + bundleRelease`; lint is a hard gate |
| F13 | README claim vs measured | `README.md` lines 100-110 | README claims 108 tests and lint exit 0 — both reproduced above; no drift found on those two claims |
| F14 | House precedent | `docs/council/qiyas-council.md`, `docs/review/2026-11-17/COUNCIL-VERDICT.md` | this project already uses the Qiyas Council idiom (aṣl / ʿilla / farʿ + Maestro verdict); this room keeps that idiom, with the review-room protocol as the mechanical frame |
| F15 | Legal snapshot handling rule | `docs/legal/sources/README.md` §6 | snapshots are KEPT as evidence for the clearance memo; "if this directory is ever published or shared outside the team, remove the third-party documents first" — deleting them is a legal/process decision, not a code cleanup |

Reproduction scripts (this session, disposable): `C:/Users/AhmadMhmoud/AppData/Local/Temp/sabeel-council/{split,dup,metrics,dead}.py`.
Gate command: set `JAVA_HOME=C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot`, `ANDROID_HOME=C:/Android/Sdk`, then `./gradlew.bat --offline :app:testDebugUnitTest --rerun-tasks` / `:app:lintDebug`.

## Feasibility arithmetic (the room must not ignore this)

Raw-line floor from language + readability overhead alone: imports 978 + comments 889 + blank 804 = **2,671 lines = 25.8% of the corpus**. Those lines are not logic; imports cannot be removed, blanks are syntax hygiene, comments are C5-required. So the *maximum* raw-line shrink with all logic intact is ~74%, and any target below ~2,671 total is arithmetically impossible even for an empty program.

| Target | Total lines allowed | Test files (T1, frozen) | Main lines allowed | Main code lines allowed | Verdict |
|---|---|---|---|---|---|
| 50% (2×) | 5,174 | 1,322 | 3,852 | ≈3,100 (of 6,615 today) | **requires ~53% of main logic to disappear** — hard, not impossible |
| 25% (4×) | 2,587 | 1,322 | 1,265 | ≈900 | **impossible**: 2,671 overhead floor already exceeds it |
| 10% (10×) | 1,035 | 1,322 | — | — | **impossible by arithmetic**: the test files alone are 1,322 lines |

Under T2 (tests may be rewritten, assertions preserved) the floor moves down but not by 4×: the test corpus (1,322 lines) is 13% of today's total, and the 108 assertions still have to exist somewhere.
If the real target is **repo text size** (agent/context cost), the dominant mass is F10's 121,592 lines of archived legal HTML — a legal decision (F15) with zero effect on code or tests; it is a separate branch, not a code shrink.

## What the room must decide

1. **Metric.** Which "size" is on trial: (a) Kotlin code lines/tokens of `app/src/main`, (b) repo text lines/tokens (context cost), (c) release artifact bytes? The room should rule out gaming (deleting comments/blank lines/imports to hit a number) because it violates C5.
2. **Invariance.** T1 (test files frozen) or T2 (tests may be rewritten, same 108 assertions)? The floor differs and C2 does not say which.
3. **Ceiling.** 50% / 25% / 10% — accept, reject, or replace each with a measured floor and a plan that reaches the honest one.

## Room rules (binding on every seat)

- No compliments. No claim without a file:line, a command, or a measured number.
- Attackers reject the first draft, always. A fatal flaw has four parts: mechanism, evidence, consequence, falsification test.
- Revisions list what changed, which attack forced it, and what was refused.
- The Maestro is silent until the argument is exhausted; the ruling is not a summary.
- **Read-only on the repo.** Nobody edits source, tests, build files or resources. Each seat writes only its own file under `docs/review/2026-11-19/code-shrink/parts/`.

## Beat sheet and seats

| Beat | Seat | Deliverable file |
|---|---|---|
| 1 | Expert — called first, no plan yet | `parts/01-expert-first.md` |
| 2 | Engineer — plan v1 | `parts/02-plan-v1.md` |
| 3 | Advocate + Customer — first attack | `parts/04-attack1-advocate.md`, `parts/05-attack1-customer.md` |
| 4 | Independent auditor (read-only) | `parts/03-audit.md` |
| 5 | Expert — dissection after v1 | `parts/07-expert-dissection.md` |
| 6 | Engineer — plan v2 | `parts/06-plan-v2.md` |
| 7 | Advocate + Customer — second attack | `parts/08-attack2-advocate.md`, `parts/09-attack2-customer.md` |
| 8 | Engineer — plan v3 (one refusal allowed) | `parts/10-plan-v3.md` |
| 9 | Expert — closing condition (dated, measurable) | `parts/11-expert-closing.md` |
| 10 | Maestro — ruling | `ruling.md` |
