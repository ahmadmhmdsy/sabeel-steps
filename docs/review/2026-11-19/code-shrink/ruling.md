# Ruling - Sabeel (سبيل) / steps_app / code-shrink room

**Session** SB-2026-11-19 · 2026-11-19 · Asia/Baghdad · **Maestro** (silent until this ruling)  
**Target** `app/src` byte-identical to `7c48fe2`; HEAD `b687f16` (docs/artifacts only).  
**Artifacts** [`review-room-transcript.html`](review-room-transcript.html) · [`session.json`](session.json) · room notes in `parts/`.

---

## 0 / The decision

**Rewrite nothing for line count. By 2026-11-24 spend 21.5 hours to archive 121,300 lines of legal evidence out of the working tree (6.62x), delete 34 dead lines, correct four false documents and build the only witness; the 342-line consolidation and the 2x feature decision wait for three gates that can be observed.**

What follows is built from the corrections in the transcript, not from a summary of the debate. The code cannot be halved under the owner's own invariants, and the plan that tried to answer anyway was rejected twice before it could become work.

## 1 / The four questions, answered

| target | verdict | arithmetic / what it would take |
|---|---|---|
| **code 1/2** (5,174 lines) | **impossible** | tests are frozen at 1,322, so main may be 3,852. Deleting **all 5,021 lines of `ui`** - every screen and component - still leaves 5,327: **153 lines over the line.** 2x is a delete-the-product target, not a compression target. |
| **code 1/4** (2,587) | **impossible** | below main's 2,411-line import/comment/blank floor plus the tests that must exist. |
| **code 1/10** (1,035) | **impossible** | below the frozen test files alone (1,322). |
| **tree 1/2** (71,228) | **done in 3.5 h** | 85.15% of tracked text is 73 legal-evidence snapshot files (121,300 lines). Tag + 73-file SHA-256 manifest + pointer: **142,456 to 21,511 (6.62x)**. |
| **tree 1/4, 1/10** | **only by deleting the project's docs** | 1/10 lands 10.02-10.10% and requires deleting `docs/legal/BLOCKERS.md` (the B7 record) and the Room migration contract. Refused as a target: it is not code and it removes the record that keeps the room honest. |

**What is deleted, removed or reformatted** - and what is explicitly not:

| action | item | lines | why |
|---|---|---|---|
| delete | 5 dead declarations: `minuteKeys`, `resetSession`, `hasStepSensor`, `bestMeasured`, `getSince` | 27 | measured dead: write-only or zero call sites; witness `StepCadenceTest` + compile |
| delete | leftovers: `Stepper` unused params, `CsvIo.HEADER` alias, `Reminders` extraction | 7 | zero call sites / never overridden |
| remove from the working tree (archived, not destroyed) | `docs/legal/sources/` 73 files | 121,300 | legal evidence, kept as evidence; tag + hash manifest keep it citable |
| reformat | four KDoc statements the audit proved false (`syncLanguage`, checkpoint version, `CsvIo` guard, channel importance) | 0 | an agent acting on them breaks C1; this is the cheapest correctness win in the tree |
| reformat | `AGENTS.md` code map: modules, three task paths, which test pins what, untested areas | +60 | the only legibility artifact this ruling buys outright |
| **not deleted** | ~450 lines of boundary redundancy (language stores, erase ordering, checkpoint codecs, formatters, status polls, day resets) | 0 | each merge has a named user-visible failure and no test: audit A1, A3, A6, A9, A10, A12 |
| **not deleted** | 2,671 lines of imports/comments/blanks (25.8% of the corpus) | 0 | buys 1.9% of the tree, kills C5, does not reach even 2x |
| **not rewritten** | the 108 tests | 0 | C2/T1; a plan that needs them rewritten is a product plan |

## 2 / Preconditions

| check | command / evidence | state |
|---|---|---|
| tree clean at HEAD | `git status --porcelain`; `git diff 7c48fe2..HEAD -- app/src` empty | in flight |
| tests frozen and green | 108 / 11 / 0 / 0; `git diff --name-only -- app/src/test` empty | green |
| instrument in repo | `python tools/shrink/tree.py` prints 286 files / 142,456 lines and 62 / 10,348 raw | not yet |
| legal signature | owner and legal co-sign the tag and manifest wording | owner action |
| owner's C1 signature | the five-row page for B5/B6 signed in this file | by 2026-12-10 |

## 3 / Phases

### Phase 1 - Measure before touching anything · day 1 · 4.5 h
Commit `tools/shrink/` (tree by class, L1 five-tuple, tokens, SHA-256). Record three planes: L1 10,348 raw (main 9,026 / test 1,322); L2 the files, lines and tokens for three named tasks (change the goal default; add a settings row; resume an interrupted walk); L3 142,456 by class. Freeze T1: 11-file manifest, 108 `@Test`, one green four-task gate.

- **AC-1** the tool reproduces 286/142,456 and 62/10,348 from a clean checkout.
- **AC-2** the three L2 tasks are recorded in `metrics/l2-baseline.md`, one row per task.
- **AC-3** T1 manifest matches all 11 files; gate is 108 / 11 / 0 / 0.

### Phase 2 - Archive the evidence, do not destroy it · day 1 · 3.5 h + legal sign-off
Annotated tag `legal-sources-2026-11-19` on a byte-identical tree; `SOURCES.sha256.txt` (73 lines); 25-line `SOURCES.md` carrying the house rule, tag and manifest path; redirect the nine citations; `git rm -r` in one commit. Nothing under `app/`, `.github/`, `gradle/` or build files changes.

- **AC-4** `sha256sum -c` passes 73/73 from a tag checkout; `git revert` restores in under five minutes.
- **AC-5** tracked text 21,511 (6.62x); no path under `app/`, `.github/`, `gradle/` or a build file in the diff.
- **AC-6** nine citations resolve to the tag; every quote of 6.62x says working tree, never repository or code.

### Phase 3 - Kill the dead code and the four lies · day 2 · 3.5 h
Five dead symbols (27 lines) and three leftovers (7 lines) grep-verified and deleted; four KDoc statements corrected to what the code does; `AGENTS.md` written with the module map, the three task paths, the oracle map and the untested areas named plainly.

- **AC-7** `git grep` returns the declaration only for the five symbols; `StepCadenceTest` green.
- **AC-8** the four KDocs agree with the code; no new KDoc claim without a test or a witness.
- **AC-9** a reader with no chat context answers the three L2 tasks from `AGENTS.md` plus the tree in under ten minutes, and the answers are recorded.

### Phase 4 - Build the witness nobody can authorise alone · day 2-3 · 8 h
Scripted capture for four fixed configs plus the 1.35 and 1.4 threshold cells, status bar cropped, one SHA-256 per cell, pixel diff, pinned API 34 image and ABI; proven on the pre-refactor commit; a deliberately wrong edit produces a red job; two independent operators reproduce byte-identical hashes; a non-author adjudicates diffs; a CI job uploads the manifest.

- **AC-10** baseline hashes byte-identical on two independent machines, and the harness runs in CI.
- **AC-11** one pass is 1.5 h or less and covers Home granted/stopped, Settings dialogs, Walk live/summary, History, Insights in en and ar.
- **AC-12** the deliberately wrong edit produces a non-empty diff and a red job.

### Phase 5 - Consolidate only behind three gates · from 2026-11-24 12:00 · 36 h + 10.5 h second tranche
G1 the witness (AC-10..12); G2 read-closure at least 5% better on a named task, no regression after the row; G3 the owner's explicit yes on the subset and hours. Rows: P2 226, P3 77, P4 39; `StatCardGrid` and the shared status flows stay cut; P5/P6 (40 lines, free unit witnesses) open the second tranche. **No default: silence funds nothing here.**

- **AC-13** no row starts before all three gates are evidenced, with operator and adjudicator named.
- **AC-14** with every row landed L1 is at most 9,932 (416 removed); a row measuring >20% short is cut and the total moves, never padded.
- **AC-15** the three L2 tasks beat baseline; the test tree is still byte-identical.

### Phase 6 - Decide the only 2x from one page · page 2026-11-23 18:00 · signed 2026-11-24 12:00 · closed 2026-12-10
One page, five rows per branch (B5 delete the UI; B6 delete features): screens/routes kept; capabilities lost in the user's words; what the 108 tests prove afterwards; data, Play Data Safety and the first-run disclosure; hours, irreversibility and who signs the C1 exemption. A yes starts a new product plan; a no closes 2x/4x/10x permanently.

- **AC-16** the page exists with five rows per branch, measured lines and the legal artefact each touches.
- **AC-17** yes/no signed and dated; no feature deleted and no C1 exemption taken before it.
- **AC-18** a no is recorded as closed and does not reopen without a product change.

## 4 / What waits, and the evidence that reopens it

| item | when | the evidence that reopens it |
|---|---|---|
| the 342-line UI consolidation | after 2026-11-24 12:00 | harness green in CI, read-closure 5% better, owner's explicit yes - three gates, not a date |
| P5/P6, 40 lines with free witnesses | first tranche after the gates | the same three gates; no new oracle required |
| shared FGS/notification machinery (<=18 lines) | next device cycle | a recorded physical-device matrix with B7 closed, or an `androidTest` covering both services' start/stop |
| test rewrite under T2 | not in this plan | suite runtime over 90 s, or a failure-localisation defect T1 cannot find |
| archiving every doc for a 10x tree | never as a shrink target | a legal/compliance decision with its own owner; it removes READMEs, blockers and the migration contract |
| moving code to a second module | only for a build-time reason | a measured build-time or ownership win; 0% compression, every compiled Kotlin line counts |
| stripping comments/blanks/imports/tests | never | no witness exists, C5 dies, buys 1.9% of the tree |

## 5 / The run of commands

```
python tools/shrink/tree.py
./gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease

# after legal sign-off - archive, never destroy
git tag -a legal-sources-2026-11-19 -m '73 files / 121,300 lines / archived, not destroyed'
python tools/shrink/hash.py --write docs/legal/SOURCES.sha256.txt
git rm -r docs/legal/sources
git commit -m 'P1: archive legal evidence to tag legal-sources-2026-11-19'
python tools/shrink/tree.py

# witness - two operators, non-author adjudication
python tools/shrink/capture.py --configs en-ar-1.0-1.35-1.5 --hash --ci
```

## 6 / The scope fence

- No user-visible change (C1); the 11 test files byte-identical (T1); no task, string, permission, manifest, schema or dependency change.
- No code excluded by module, source set, variant, directory or generation: every Kotlin file Gradle compiles counts; relocation is 0% and is never claimed.
- No comment, blank line, import or test stripped to reach a number.
- No feature deleted, no test rewritten and no C1 exemption taken before the owner's signed decision.
- No legal evidence destroyed: tag, 73-file manifest, pointer and sign-off, or the phase does not run.
- 6.62x is a working-tree figure and is never quoted as code compression; clone size is a separate decision.

## 7 / The kill-criterion

**2026-12-10.** By then: the tree is at or below 21,511 lines with the tag and manifest verified; the harness runs from two operators in CI; the three L2 tasks have a recorded baseline and at least one has stopped getting harder; the owner has signed the 2x page. If any is missing, the shrink thesis is dead - not 'needs more time'. The correct conclusion is that the codebase is sized for its product, and the only remaining lever is the product decision already on the owner's desk.

---

## Numbers the ruling commits

| item | now | after the committed work | after every gate |
|---|---|---|---|
| L1 code, raw | 10,348 (main 9,026 / test 1,322) | **10,314** (-34) | **9,932** (-416, -4.0%) |
| L3 working-tree text | 142,456 lines / 350 files | **21,511 lines / 286 files (6.62x)** | unchanged |
| tests | 108 / 11 / 0 / 0 | byte-identical | byte-identical |
| hours | - | **21.5 h (3 days)** | +46.5 h, gated |

The committed work makes the tree 6.62x smaller and the code 34 lines shorter. The 6.70x quoted in the room was the archive delta alone; the committed work adds 257 lines of in-repo tooling, so the verified figure is **6.62x (21,511 lines)**. It does not pretend to halve anything. The only path to 2x is the owner's signature on the feature-deletion page, and the only path to a smaller *code* number that is honest is the gated 342 lines - 87 lines above the Expert's floor, and the ruling records the gap rather than hiding it.

---

*Maestro, session SB-2026-11-19. Artifacts: [`review-room-transcript.html`](review-room-transcript.html), [`session.json`](session.json). Evidence and every seat's full argument: `parts/01`-`parts/10`.*