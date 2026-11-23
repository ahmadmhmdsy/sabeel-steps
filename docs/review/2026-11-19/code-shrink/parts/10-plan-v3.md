# Beat 10 — Engineer, plan v3 — the revision the room can act on

**Tomas Reyes — Engineer.** v3 is not v2 repaired; it is v2 cut down to what can be witnessed today, with everything else behind one gate and one dated decision. v2 stays at `07-plan-v2.md`; nothing here restates it.
**v2 → v3, forced by attack 2:** (1) silence no longer funds the program — commitment is **13.5 h**, not 80; (2) the manual matrix is gone — the gated set waits for a scripted harness with committed hashes, a pinned image and a second operator; (3) 9,852/9,862 was arithmetic on rows the plan's own Expert cut — the gated ceiling is **9,932**, stated; (4) matrix and gate costs were under-priced — v3 prices 4 commits and a 1.5 h/pass harness, and buys no manual oracle at all; (5) `minuteKeys` is not "grep-zero" — its witness is `StepCadenceTest` (5 tests) and 0 reads across its declaration and 4 writes.

## 1. What is committed today: 13.5 h, and silence funds exactly this

| phase | work | h | witness | ΔL1 | ΔL3 |
|---|---|---|---|---|---|
| P0-lite | `tools/shrink/` committed (tree, tokens, split, sha256, read-closure); L1 5-tuple + tokens; T1 manifest; L2 three-task read-closure baseline; `docs/shrink/BASELINE.md` | 4.5 | every number re-derivable from a clean clone | 0 | +236 |
| P1 tree | archive `docs/legal/sources/` (73 files / 121,300 lines) to annotated tag + 73-line manifest + 30-line `SOURCES.md`; redirect the 9 citations in 4 docs (+20 min) | 3.5 | 73/73 `sha256sum -c`; tag object SHA recorded | 0 | −121,197 |
| P8 | dead surface 27 + `Stepper` 4 + `CsvIo.HEADER` 1 + `Reminders` 2; truthful KDocs; `AGENTS.md` code map | 3.5 | per-row table §1.2 | −34 | +16 |
| | 4 commit gates × 0.25 h + 1.0 h buffer | 2.0 | 4-task gate each | | |
| | **total** | **13.5** | | **10,348 → 10,314** | **142,456 → 21,511** |

**Committed numbers, exact.** **L1:** corpus **10,348 → 10,314 (−34; 99.67% retained)**, main **9,026 → 8,992 (99.62%)**, test 1,322 unchanged. **L3 — working tree, never "repo":** **142,456 → 21,511 (−120,945; 15.10% retained; 6.62×)**, tracked files **350 → 286**. A clone still carries the 121,300 lines in the tag's objects; that sentence accompanies every L3 figure.

### 1.1 P1 ACs (hash proof, owner/legal sign-off)
1. Annotated tag `legal-sources-2026-11-19` on a commit whose `docs/legal/sources/` tree is byte-identical to HEAD's (73 files, 121,300 lines); tag object SHA written into `SOURCES.md`.
2. `docs/legal/SOURCES.sha256.txt` — 73 lines, `sha256sum` format; `sha256sum -c` from a tag checkout passes **73/73**.
3. `docs/legal/SOURCES.md` — 30 lines: F15 §6 rules verbatim, tag, manifest path, restore command, and the L3/working-tree label.
4. The 9 remaining citations in 4 docs (`clearance-memo.md:65,174`; `play-compliance-checklist.md:6,439`; 6 more) redirect to tag+manifest; `git grep -n "docs/legal/sources/"` finds no stale pointer outside `SOURCES.md`.
5. `git rm -r docs/legal/sources` (73 files) plus the two files above; one commit, owner + legal sign-off in the body.
6. After: `git ls-files` 286; L3 re-count 21,511; nothing under `app/`, `.github/`, `gradle/` or build files changes; `git grep "docs/legal" -- app/src/test` = 0.
7. `git revert` restores the tree in 5 minutes; the tag is signed or recorded by object SHA and legal keeps an out-of-repo copy of the 73 files, so a re-pointed tag is detectable.

### 1.2 P8 witness per row (the reason each line may go)
| lines | symbol | witness |
|---|---|---|
| 5 | `StepCadence.minuteKeys` | 0 reads across declaration + writes `:48,111,138,147`; `StepCadenceTest` 5 tests |
| 7 | `WalkTrackingService.resetSession` | 0 call sites (grep); compile |
| 7 | `Permissions.hasStepSensor` | 0 call sites; its wrong KDoc trap goes with it |
| 6 | `StepSourcePolicy.bestMeasured` | 0 call sites; compile |
| 2 | `Daos.getSince` | 0 call sites; compile |
| 4 | `Stepper(canDecrease,canIncrease)` | 5 call sites, none passes them; both default `true`; compile |
| 1 | `CsvIo.HEADER` | 0 references; compile |
| 2 | `Reminders` settings extraction | compile only — named as the one row without a behavioural witness |
KDoc truth pass: 3 stale claims (the `hasStepSensor` trap, the `Stepper` parameter doc, the `CsvIo.HEADER` alias note) corrected or removed; net 0 lines. `AGENTS.md` (50 lines): tree map, entry points, module roles, test layout, the L1/L3 metric commands.

## 2. The gate: P2/P3/P4 wait for a harness, an L2 proof and an explicit yes

**What is gated:** P2 **226** (296 − StatCardGrid 70), P3 **77**, P4 **39** (49 − status flows 10) = **342 L1 lines, 36 h** — the 422 of v2 minus the 80 lines its own expert cut. **If every gate passes and the second tranche P5/P6 (+40, 10.5 h) is funded:** best case over the whole plan = 34 (P8) + 342 (gated) + 40 (P5/P6) = **416 lines** → corpus **9,932 (95.98%)** — **87 above the Expert's 9,845**. The 87 lines are the rows this room has refused (P7 10–18), the chosen-interval option (+10), A1 (+4) and the unattributed remainder: no witness exists for them, so they are not promised. Cutting both Expert-cut rows changes the arithmetic from v2's 9,862 to **9,932**, and v3 says so.

**P0-h — harness, 8 h, its own phase with ACs (prerequisite, not a cost of the refactor):**
1. Pinned image: API 34, fixed ABI, system-image hash + AVD config committed under `tools/visual/`.
2. Four fixed configs (fontScale 1.0/1.5 × app locale en/ar) plus 1.35/1.4 threshold configs; locale by `cmd locale set-app-locales`, font by `settings put system font_scale`.
3. `states.json` enumerates every cell and its setup mechanism; any state that cannot be produced reproducibly is removed and printed as uncovered — no phantom cells.
4. Capture: `adb exec-out screencap -p`, cropped to the app window (status bar excluded by fixed rect), deterministic PNG.
5. SHA-256 per cell committed in `tools/visual/hashes/p0.json`; mismatch = diff image + fail.
6. CI job `visual-regression` on the pinned image, **≤1.5 h/pass** measured and recorded.
7. **Two independent operators** — the second is neither author nor sponsor — reproduce byte-identical hashes from a clean clone on another machine; both runs in `operators.md`.
8. Any diff adjudicated by a **non-author** (Expert/independent), recorded before any re-baseline; the engineer never re-baselines alone.
9. Proof: pass on the pre-refactor commit with **136 cells or fewer**; 8 h includes image acquisition and CI wiring.

**Gates — all three, or P2/P3/P4 do not start:**
- **G1** each P0-h AC (1–9) demonstrated and committed.
- **G2** L2 is a gate, not a footnote: before a phase starts, its row set shows a **measured read-closure drop of ≥5%** on at least one named task in a dry-run; after landing, **no task regresses**. `tools/shrink/readclosure.mjs` computes files × lines × tokens for the three tasks from explicit seeds + transitive imports.
- **G3** the owner's explicit yes, by **2026-11-24 12:00 Asia/Baghdad**, with the page in §4 and the harness proof delivered **2026-11-23 18:00** (one working day earlier). Any gate missing → no start, no C1 claim.
**Cost if approved:** 36 h engineering + 8 h harness + 3 passes × 1.5 h + 1.5 h close ≈ **50 h** — priced, not default.

## 3. The two fatals, answered in one line each
**(i) No default funds the 80 h.** Silence funds §1 only (13.5 h, 4 commits). P2–P4 need G1 + G2 + G3 and an explicit yes; P5/P6 need a second explicit yes (free unit witnesses, 10.5 h); nothing auto-escalates, and no branch of §5 starts on silence.
**(ii) The oracle cannot be adjudicated by its sponsor.** Stated rule: the owner signs the C1 claim only after (a) committed baseline hashes, (b) a second operator's byte-identical run, (c) any diff adjudicated by a non-author. The owner adjudicates the *rule and the exemption*, never the pixels; the engineer cannot re-baseline a diff alone, and with no second operator there is no C1 claim and no gated work.

## 4. The five-row consequence page for the 2× branches (B5/B6)

Basis: L1 10,348. **B5** delete all `ui` 5,021 + rewrite T2 tests ≈300 → 5,027 = **48.6%**, 24 h. **B6** delete walk 1,462 + diagnostics/insights+VM 559 + onboarding 399 + history 284 = 2,704 → 7,644 = **73.9%**, ~30 h, still 2,470 over 2×.

| # | consequence | B5 — delete all screens (48.6%) | B6 — delete four features (73.9%) |
|---|---|---|---|
| 1 | **Screens/routes kept** (today 4 tabs / 9 routes, `SabeelRoot.kt:51–70,151–198`) | 0 tabs, 0 routes: no shell compiles without a replacement; the app opens to a stub or crashes — the replacement shell is unpriced | 2 tabs (Home, Settings), 4 routes (home, settings, about, privacy); opens on Home, which may show NO_SENSOR forever |
| 2 | **Capabilities lost, in the user's words** | Everything: no counting view, goal, settings, reminders, export, walk, history, onboarding | "I cannot start or resume a walk or see live pace/distance/GPS; my saved walks and history are gone; insights and diagnostics are gone; the first-run setup/disclosure never appears." Kept: Home step count, goal, units/language, reminders, daily CSV |
| 3 | **What the 108 tests prove** | Still green — they are all domain/data (StepMath 24 · StepPipeline 19 · CsvCodec 12 · …); none ever tested a screen, service or ViewModel, so green proves domain invariants only; T2 rewrite ≈300 lines is priced at 24 h, not 0 | Same 108 green for the same reason; `LocalizationParityTest` forces matching string deletions in both locales; no test covered any deleted feature |
| 4 | **Data and Play/disclosure promises** | Saved walks orphaned in Room (schema contract `app/schemas/com.sabeel.steps.data.db.AppDatabase/1.json`); walk/location FGS types and ACTIVITY_RECOGNITION become unused; Play Data Safety (health/location) and prominent-disclosure declarations must change; **first-run disclosure C11-FIRST-RUN-DISCLOSURE** (`docs/legal/evidence/AVD-SMOKE-20261117.md:15`) disappears | Walk rows orphaned and unreadable after the feature dies; walk history CSV backups fail to import if walk columns leave the codec; FGS/location declarations need review; the same disclosure artifact is touched; Data Safety can drop location only after a migration decision |
| 5 | **Irreversibility and signature** | After ship, `git revert` restores code but not user data or the removed shell; B5 needs a schema/backup decision even though it "only deletes UI"; C1 exemption signed by the **product owner** in `ruling.md`, legal co-signs disclosure/Data-Safety lines; afterwards the app is "a broken build, not a product" | Revert restores code but not users' saved walks/history; the shipped app is "a step counter, not a walking app"; same signer, same legal co-sign; no return path after the first release |

## 5. Decision, date and the B4 correction
**2× decision: 2026-11-24 12:00 Asia/Baghdad** (page above + harness proof delivered 2026-11-23 18:00). The owner's **B1 yes is recorded now** (subject to legal co-sign). **B4 is not in the committed set.** Its 10% required archiving every remaining doc — including `docs/legal/BLOCKERS.md` (the record that B7 is open), the hardware-QA checklist, the disclosure evidence and the Room migration contract `app/schemas/…/1.json`. On v3's tree that path lands **14,393 = 10.10%** (all docs 7,118 net, 5-line pointer kept), and even with the schema **14,267 = 10.02%** — still over the exact tenth, because the committed tooling, BASELINE.md and AGENTS.md must stay for the number to be re-checkable. v3 therefore prints no tenth and promises none; B4 waits for 2026-11-24 with the exact doc list printed before anyone signs.
**Not refused, deferred:** P5 (+26) / P6 (+14) have free unit witnesses but push the committed bill past 16 h; second tranche, same decision. P7 stays out (§3 trigger of v2). The settings structural split stays out at 1.5 d / +110.

## 6. The one refusal, and the invariants (not a second refusal)
**I refuse to run the 422 UI lines against a manual oracle adjudicated by the person who ordered the refactor; they wait for the harness, the two operators and a non-author adjudicator.** That is the single refusal v3 retains.
Invariants, not proposals: comments, KDoc, blank lines and imports are never stripped to move a number (the 2,671-line floor is 25.8% of the corpus and would fake 1.9% of L3); a second module or source-set relabeling is 0% — the measured set is every Gradle-compiled `.kt`, wherever it sits; generated code counts; moves net zero; tests are never rewritten to buy a total (T1).

## 7. Verification of the committed set
Gate after each of the 4 commits: `./gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease` → **108 / 11 / 0 / 0**; `lintDebug` no new issues; `git diff --name-only -- app/src/test` empty and the committed T1 manifest matches; L1 5-tuple + tokens printed by `tools/shrink/split.mjs`; L3 printed by `tree.mjs` with the "working tree, not repo" label; P1 proves 73/73 and the tag object SHA; P8 proves each row's witness in §1.2. L2 is measured, never claimed for the committed set — dead code does not make a task shorter; it is only forbidden to grow.
