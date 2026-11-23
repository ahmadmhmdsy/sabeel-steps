# Beat 3 — Advocate, first attack on plan v1

**Seat:** Devil's Advocate. Read in order: `evidence-pack.md` F1–F15, `parts/01-expert-first.md`, `parts/02-plan-v1.md`; then re-read the code behind every citation this attack leans on — `StepCounterService.kt`, `WalkTrackingService.kt`, `Notifications.kt`, `SettingsScreen.kt`, `HomeScreen.kt`, `WalkScreen.kt`, `InsightsScreen.kt`, `AppViewModels.kt`, `CommonComponents.kt` — and grep-verified the test boundary and the dead symbols. No repo file modified.

**Rejected.** v1 is not a plan for the question on trial; it is the room's ruling written at beat 2, with a phase table attached.

## 0. The sentence that has to be quoted back

> **Committed:** main **9,026 → ≤8,480 raw (−546, 94.0 %); corpus 10,348 → ≤9,802 (−546, 94.7 %)**. […] This sits on the Expert's independently measured T1 floor (9,796, range 9,780–9,850), which I accept as the honest floor. — `parts/02-plan-v1.md:32`

The question is *"Can this source be compressed to ≤50% of its size — and how far below that is honest"* (`evidence-pack.md:7`). The head of the same section is `## 2. Committed target: reject 50 %, reject 25 %, reject 10 %; commit to 94.7 %` (`:25`). "Honest" has been redefined from *as far as the source can be compressed* to *where the Expert's floor sits*; and the commitment is printed in **retained** percent, 45 points from the frame the owner used, in a section whose only result is an impossibility proof for the owner's targets.

## Fatal flaw 1 — the plan answers a metric it chose, not the question it was asked

**Mechanism.** The plan converts a compression ratio into an invariance audit: T1 is fixed by the author (`:32`), the corpus is fixed to `app/src/**/*.kt` (`:112`), C1 is treated as absolute, and the target is then derived from the oracle's coverage — 10.7 % of the corpus (`01-expert-first.md:97`) — not from the owner's ambition. The operative rule is "remove the largest number of lines no observation can see". Nothing in §2 maps 50/25/10 to 94.7 %; the only candidate mapping is a non sequitur: "all of `ui` gone is still 5,327 > 5,174" (`:29`), therefore commit to the floor. An impossibility proof for target A is not a decision rule for target B.

**Evidence.** (i) The 50 % arithmetic is correct and re-derived: 5,021 `ui` lines removed leaves 5,327 > 5,174 (`:29`), and non-UI main raw alone is 4,005 against a main budget of 3,852. (ii) The middle that this creates is never priced: `DiagnosticsScreen` 318 + `InsightsScreen` 151 (`01-expert-first.md:26`) + their two ViewModel/state blocks (`AppViewModels.kt:154–205, 243–280`, 90) ≈ **560 raw lines — more than the entire 546-line plan** — for two secondary screens and no 8.5-day refactor. (iii) The plan spends at v1 (`:33`, "I refuse to plan to 50 % and will not sign a plan that claims it") the one refusal the protocol reserves for plan v3 (`evidence-pack.md:82`).

**Consequence.** The owner asked how far the source can be compressed and receives a 5.3 % deliverable — the nil-change floor — plus no price list for the decision that actually settles the question: hold C1, or spend ~560 lines of secondary screens. The room can now only revise the floor, not the answer; and 8.5 engineer-days (P0 .5, P2 2, P3 1.5, P4 1, P5 .5, P6 .5, P7 2, P8 .5; P1 has no timebox — `:37–43`) are committed before anyone asks whether C1 is a constraint or a default.

**Falsification test.** One sentence in v1 mapping the owner's 50/25/10 to 94.7 % without oracle coverage as the independent variable, or the owner's written answer choosing the floor over any priced C1 relaxation. Neither exists; if either appears, the flaw is false.

## Fatal flaw 2 — P7's helper cannot save the lines it was priced on, and its only witness cannot currently be run

**Mechanism.** P7 prices "103 lines → ~58" = −45 (`:76`) on a helper whose own signature is `foreground(service, id, notification, type)` + `update(...)` (`01-expert-first.md:55`). A helper that *receives* `type` leaves both type ladders in the callers; one that *receives* `notification` leaves both builders in the callers; one that receives `service`/`id` leaves both failure policies in the callers. What remains shareable is `if (started) return; runCatching { ServiceCompat.startForeground(...) }.isSuccess; flag = …` — ≈4–5 lines per site, ≈10 total. `Notifications.post` is already the shared post path (`Notifications.kt:214–222`), so `update(...)` adds 2–4. And of the 103 cited lines, 31 are static start gates with different predicates (`StepCounterService.kt:658–675` vs `WalkTrackingService.kt:721–733`) and 15 are `StepCounterService.kt:619–633`'s notification builder, which the signature excludes. The plan itself forbids the only way to reach −58: "both services keep their own type, stickiness, intents and teardown; only the four notification/foreground call sites move" (`:76`).

**Evidence.** Windows read: `StepCounterService.kt:599–635, 658–675`, `WalkTrackingService.kt:493–526, 721–733`. The three policies a shared body must preserve are in the code: FGS type ladder (2 branches `StepCounterService.kt:602–606`; 4 branches including LOCATION `WalkTrackingService.kt:501–507`), failure reaction (`return started` `:611`, user-visible as Home's status card; vs `blocked = true; stopSelf(); return` `:511–515`, user-visible as the Walk blocked state), refresh cadence (no throttle `:614–617`; 5 s throttle with `force` `:520–526`). The plan's own gate: "accept only if |measured Δ − planned Δ| ≤ 20 % of the step" (`:95`) → P7 must land ≥ −36; its stated signature caps it near −12.

**Consequence.** P7 cannot reach −45. Either it lands short — 10,348 − 501 = 9,847, not the committed ≤9,802 — or the missing ~30 lines are bought by letting the helper share the builders and start refusals, i.e. changing the exact `startForeground` sequencing the plan names as its worst Android risk (`ForegroundServiceStartNotAllowedException` on 12+, FGS-type kill on 14+, `:85`). Its only witness is the device matrix (`:106`): API 26/34/36 emulators + one OEM. That matrix cannot be run today: no physical-device run has ever been recorded (`docs/legal/BLOCKERS.md:146–161` B7 OPEN; `docs/legal/HARDWARE-QA-CHECKLIST.md:6`), the recorded release-validation image returns blank `screencap` frames so no PNGs are kept (`README.md:135`), and the recorded screenshot AVD has no step counter, so Home always renders the NO_SENSOR branch on it (`docs/review/2026-11-19/design-implementation/EVIDENCE.md:18–20`). No `androidTest` source set exists and no test imports `ui`, `service` or `platform` (grep over `app/src/test`).

**Ordering.** "Riskiest, deliberately last" (`:75`) is defended as monotone risk (`:86`). But the six gates P7 hides behind are compile-and-screenshot gates over code no test sees; the only observation that can falsify P7 is the one that does not exist. Last means maximum sunk cost (≥6.0 preceding days merged), no later phase to absorb the revert, and no schedule left to build the missing witness. First costs two days and keeps the rest of the plan fundable.

**Falsification test.** Implement the helper exactly as specified, apply it to the four cited windows, and measure −45 ± 9 with both type ladders and both failure reactions intact; or produce one recorded physical-device run of the P7 matrix. Neither is available from this tree.

## Fatal flaw 3 — P1 is priced at Δ0 with no timebox, and by construction it is Δ>0

**Mechanism.** `SettingsScreen()` is 511 lines (`SettingsScreen.kt:90–600`) over a 26-value local scope (`:97–171`); P1 moves six section bodies and all seven dialogs into new files (`:40–42`). Kotlin imports are file-scoped, so each new file re-imports the symbols both files use; each extracted section needs `@Composable`, KDoc (C5), a parameter list and a call site; and `stacked` is not removed by P1 — P2 removes it in the next phase (`:51`). Even a bundled-state design costs ≥6 × (annotation + KDoc + signature + closing) + 6 call sites ≈ 50 lines before duplicated imports; a plumbing design costs 80–130.

**Evidence.** `SettingsScreen.kt` has 68 import lines (`:3–70`), 26 locals (`:97–171`), 16 `stacked = stackedRows,` call sites (`:186,191,204,218,226,234,256,269,282,292,305,316,327,337,350,358`). P1's own acceptance rule, `|Δ − 0| ≤ 20 % of 0` (`:95`), is unsatisfiable by any nonzero edit, and P1 is the only phase with no timebox (`:37–43`).

**Consequence.** −546 becomes −496 to −446; the corpus lands 9,852–9,902 = 95.2–95.7 %, outside the 9,780–9,850 band the plan adopts and short of the 9,802 commit. §6's "P1 lowers the worst file/function, so readability improves while raw falls" (`:114`) is false on the second clause: M1 is raw `app/src/main` (`:112`) and P1 raises it.

**Falsification test.** Perform P1 as written and run the plan's own `split.py`; if raw `app/src/main` is unchanged and the six sections need no new parameters, the flaw is false.

## The metric, audited

- **The oracle is not in the repo.** A line counts only if "the behaviour it carries is observed green before and after by a frozen oracle" (`01-expert-first.md:107`); for the 481 lines P2–P4/P7 delete there is no such oracle. P0's matrix is 30 state-cells (Home 3 · Settings 6+7 · Walk 3 · Onboarding 4 · History 2 · Insights 2 · Diagnostics 1 · About 1 · Privacy 1, `:38`) × 3 fontScales × 2 locales = **180 captures**, hand-made and unversioned. The repo's entire committed image evidence is 48 PNGs under `docs/` — 2 fontScale variants, 2 dialogs — with no matrix baseline, no capture script, no diff tool, no CI job (`.github/workflows/android.yml:42–48` runs four Gradle tasks and uploads reports). The spec is not even frozen: P0 says fontScale 1.0/1.3/1.5 (`:38`), §5 says 1.0/1.4/1.5 (`:99`) — the oracle drifts before it is built.
- **Price the hidden witnesses.** 90–150 s per capture cell (state setup + capture + inspection) = 4.5–7.5 h for the P0 baseline alone; re-running the matrix after P2/P3/P4 = +13.5–22.5 h; P7's 4 targets × 6 checks with installs, reboots, force-stops and screen-off waits ≈ 10–16 h, plus an OEM phone the repo says is absent (`BLOCKERS.md:150–157`). **≈4–7 engineer-days, uncounted** — P0's own box is 0.5 day including a full green-gate run (`:38`).
- **The numerator is counted by the hand that deletes the lines.** Nothing in the tree re-derives a witness after merge; the Expert's gate asks for a "human sign-off" (`01-expert-first.md:116`) — a signature, not an observation.
- **P1 does not improve the number it claims to improve.** M1 is raw lines of `app/src/main`; P1 is Δ0 by declaration and Δ>0 by construction (flaw 3). Readability improves; the metric does not.

## The 2x path, steelmanned before rejecting

| candidate (C1–C3 held unless noted) | best honest win | corpus after | reaches 5,174? |
|---|---|---|---|---|
| ground-up rewrite, different framework, same product | re-express 5,021 `ui` raw lines; C5 keeps ui's 1,134 imports/comments/blanks and its 318 ceremony (F8); a generous 35 % cut of the 3,887 ui code lines saves ≈1,360 | ≈8,990 | no — non-UI main alone is 4,005 > the 3,852 main budget |
| code generation from a descriptor | generated files must count (`:112`); the whole data-driven-renderer farʿ is 245 (`01-expert-first.md:53`), and it replaces rather than adds to P2 | ≈9,850 | no |
| delete one feature (needs C1 relaxed) | walk: 433+778+88+76+18+69 VM ≈1,460; diagnostics+insights+VM ≈560 | 8,890 / 9,788 | no — only *all* of `ui` (5,021) gives 5,327, still 153 over |
| T2 test rewrite | 1,322 → ≈1,022 (Expert's 300) | 9,502 | no; T2 + delete-all-UI = 5,027 ≤ 5,174, still C1 |
| move code out of the measured corpus (second Gradle module) | `app/src/main` 9,026 − ui 5,021 − service 1,710 = 2,295; corpus 2,295 + 1,322 = 3,617 | **3,617 = 35.0 %** | **numerically yes — but this is relocation, not shrink: it reaches the number only because the corpus definition (`:112`) is the definition on trial (flaw 1)** |
| merge files | 933 main import lines cover 340 symbols (`01-expert-first.md:47`); C5's 800-line cap (`:113`) floors the file count at 12 → ≈400–500 import lines | 9,300–9,400 | no |

The steelman's result is not a technique the plan missed; it is the **denominator**. Under module extraction the room's "≤50 %" is answered with every user-visible byte intact; under the plan's corpus it is not. So "50 % is impossible" is not a fact about the code — it is a fact about `app/src/**/*.kt`, a definition the plan asserts in §6 and never argues to the owner. That is fatal flaw 1 with arithmetic: the plan refuses to answer because it refuses to let the owner choose what "the project" means.

No summary of v1 is offered. It should not reach beat 4 without the owner's ruling on C1 and on the corpus.
