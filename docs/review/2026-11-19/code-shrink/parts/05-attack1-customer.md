# Beat 3 — Customer, first attack

**Seat:** the owner who asked the question. Five minutes and a grudge; not a line counter. This file is my only write — the repo is untouched.

## 1. What I asked, and what I was handed

I asked: *if you rewrite the project smaller — half, quarter, tenth — same result, same tests, still readable for humans, machines and agents, how?*

What plan v1 answers, in its own words:

> "## 2. Committed target: reject 50 %, reject 25 %, reject 10 %; commit to 94.7 %" — `parts/02-plan-v1.md:25`
> "**Committed:** main 9,026 → ≤8,480 raw (−546, 94.0 %); corpus 10,348 → ≤9,802 (−546, 94.7 %)." — `:32`
> "**Primary metric: raw Kotlin lines of `app/src/main`**" — `:27`
> "**50 % is not a compression target; it is a delete-the-product target.**" … "I refuse to plan to 50 % and will not sign a plan that claims it." — `:29,33`
> "**Not in the plan:** T2 (it buys ~300–600 test lines, 0 on the primary metric …)" — `:81`

My three numbers are refused. The one that remains, 94.7 %, is not a size of shrink; it is the fraction that stays of `app/src`. The move itself is 546 lines (`:81`): −5.3 % of the 10,348-line Kotlin corpus, −6.0 % of the 9,026-line main corpus. The plan also never totals its own bill: 8.5 priced days (P0 ½ + P2 2 + P3 1½ + P4 1 + P5 ½ + P6 ½ + P7 2 + P8 ½), and **P1 — the five structural splits every later phase leans on — has no timebox at all** (`:40–42`). The room says ten days; the document does not.

So I asked how the *project* gets smaller, and I was handed an honest, careful, number-clean answer to *how many lines leave `app/src/main` without changing anything*. Those are not the same question, and the plan never notices the difference.

## 2. The fatal flaw, as the thing I will feel

Ten days from now I sit down with the result. The app is the same, the 108 tests are byte-identical, and the part of the repository that made me ask — the part an agent chews through before it can be useful — has moved by 0.38 %.

I measured the working tree at HEAD (`b687f16`; `git diff 7c48fe2..HEAD -- app/src` is empty): 350 tracked files; the ten text extensions the pack counts (F10) hold **142,456 lines**. Of those, `.html` is **121,592 lines in 24 files**, and `docs/legal/sources/` alone is **73 files / 121,300 lines** (16 HTML = 121,147; 56 txt = 56; 1 md = 97). The Kotlin corpus is 62 files / 10,348 lines = **7.3 %** of tracked text. The plan's 546 lines takes 142,456 → 141,910: **−0.38 %**. At the ten days the room quotes (~80 engineer-hours) that is 6.8 lines per engineer-hour, or 8.8 minutes per deleted line.

The evidence pack had already put this denominator on the table — F10 (`evidence-pack.md:34`), "the dominant mass … a legal decision" (`:39`), and the open metric decision "(b) repo text lines/tokens (context cost)" (`:55`, `:59`). Plan v1 settles that decision by fiat at `:27` and then never mentions the mass again: a case-insensitive grep of the plan for `121|html|legal|context cost|repo text` returns **nothing**, and the word "agent" does not appear in the plan at all, though C5 in the pack says "human, machine and agent" (`evidence-pack.md:16`). The only agent-facing figure it carries is 35,653 corpus tokens (`:20`) — the tokens of the 7.3 % slice.

The one phase that touches readability is Δ0 by construction (`:40–42`). Splitting `SettingsScreen.kt` (908 lines, my count) into `settings/SettingsSections.kt`, `settings/SettingsDialogs.kt` and a ~60-line root redistributes the same lines into more files: the read cost of a settings change is unchanged and the hop count rises (1 file becomes 3; 62 → ~63 files after P5 folds `WalkRepository` in). Today a goal change is found across `SettingsScreen.kt` (908), `AppViewModels.kt` (547), `SettingsStore.kt` (143) and two `strings.xml` (316 + 332): ~2,246 lines in 4 files. After P1 it is the same ~2,246 lines in 6 files. §6's gates — file ≤800, function ≤120, KDoc, comment ratios — are human style rules; none of them measures what an agent must load to answer one question.

Consequently, the day the plan lands I cannot tell it apart from a no-op by the only reader I named. That is the fatal flaw: **the metric was chosen where the plan's own work is largest and the repository is smallest; the 85.4 % of tracked text an agent trips over is neither reduced, nor scheduled, nor refused — it is absent, which is worse than a refusal because there is nothing to argue with.**

Falsification test: show me a line in my brief, or in the README, that limits "the project" to `app/src`, and I withdraw the denominator point. The pack says the opposite at `:55`: if the real target is repo text size, the dominant mass is the legal HTML.

## 3. The second thing I felt: "same tests" bought with a manual oracle

"Same tests" is preserved the way a locked door is preserved by never opening it. The plan's own witness map (`:96–105`) gives the tests P6 (14 lines) and at most P5 (26) — P5's witness is `StepMergeTest` 7 plus a manual round-trip; the other 506 of 546 lines (**92.7 %**) are witnessed by screenshots, a device matrix, compile and grep — the plan says it itself: for P3/P5/P7, "'green' means 'compiles'" (`:87`). The expert measured the frozen oracle at **10.7 %** of the corpus (`01-expert-first.md:97`).

And the screenshot oracle does not exist yet: tracked `app/src/androidTest` files = 0, `@Preview` = 0 (my counts, matching `:20`), and `.github/workflows/android.yml` runs build + tests + lint + bundle and uploads test/lint/APK/AAB — no screenshot step. P0 (`:38`) budgets **0.5 day** for a 30-state × 3 fontScale × 2 locale = **180-image baseline**: 80 seconds per image, before any code is touched, with no capture or diff tooling anywhere in the repo. Same result, to me, means the app behaves identically, and I will pay for that. What I did not agree to buy is ten days of refactor plus a 180-image manual QA ritual as the only thing standing between "same result" and "the gate was green". That is what the ten days actually purchase: not the 546 lines, but the new oracle it silently installs.

## 4. The cheapest demand that would have kept me

One command, one number, one decision, one afternoon. Not another document.

**Command.** `git ls-files -z | xargs -0 wc -l` grouped by tree (or the room's `split.py` with a `--tree` flag), five minutes, read-only:

| where | files | lines | share of tracked text |
|---|---|---|---|
| `docs/legal/sources/` | 73 | 121,300 | 85.2 % |
| `app/src` (all text) | 88 | 11,826 | 8.3 % |
| `app/src` `.kt` only | 62 | 10,348 | 7.3 % |
| tracked text (F10 extensions) | 286 | 142,456 | 100 % |
| plan v1 delta | — | 546 | **0.38 %** |

**Number.** The plan's answer is not "94.7 %". It is **−0.38 %** of the tree an agent reads and **−5.3 %** of the code. Print that next to the ten days and I decide with my eyes open.

**Decision, mine, today.** The shrink is measured on the working tree I hand an agent, not on the Kotlin corpus. Given that, the first phase is **two hours**, not two days: move `docs/legal/sources/` (73 files, 121,300 lines) out of the working tree into a tagged evidence branch/artifact with a SHA-256 manifest, and point `docs/legal/sources/README.md` §6 at the tag. Nothing is destroyed — F15 says the snapshots are "KEPT as evidence", not "kept in the working tree"; a tag plus manifest keeps them citable by hash, and the legal owner signs the wording. Working-tree text drops **142,456 → 21,156 (6.7×)** with zero app, test, copy, permission or build change; C1–C4 untouched, the 108 tests untouched by definition.

If legal refuses to let the evidence leave the tree, print that refusal on the cover — "the legal archive is 85 % of the repo; agents cannot be given a small tree" — and I will price the ten days against the truth. What I will not accept is a plan that never asks the question and then calls 546 lines the honest floor.

I gave five minutes. I still have the grudge.
