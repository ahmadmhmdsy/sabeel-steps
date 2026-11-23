# Beat 8 — Customer, second look

**Seat:** the owner who asked the half/quarter/tenth question. I read v2 in full and re-checked its numbers; where
it answered me I say so — these are two fatal attacks.

## 1. Which headline I believe — and what I would fund

**I believe the 6.70×.** I re-derived it from the tree, not the prose: 142,456 tracked text lines;
`docs/legal/sources/` 73 files / 121,300 lines; `git rm` + 73-line manifest + 25-line pointer = **21,254 =
14.92%**, 279 tracked files (350 − 73 + 2). A folder operation with a hash proof and a five-minute revert; I can
audit it today. **v2's tree move would have kept me: yes.** It is exactly my beat-3 demand, priced at 3.0 h (2 eng
+ 1 legal, `07-plan-v2.md:49`) instead of my 2; I sign B1 now, subject to legal co-sign, with one added AC: 9
lines in 4 docs still point at `docs/legal/sources/` (`clearance-memo.md:65,174`;
`play-compliance-checklist.md:6,439`; …), so the 25-line `SOURCES.md` must redirect them — +20 minutes. Label it
L3 / working tree, never "repo": a clone still carries the 121,300 lines in the tag's objects; v2 concedes this at
`:74`.

**B4 is the tenth, nearly.** 14,251 / 142,456 = **10.0038%**; the exact tenth is 14,245.6, so B4 misses by 5.4
lines, and a *usable* pointer (tag, restore command, manifest path, index) is more than 5 lines, landing
~10.01–10.02%. I fund B4 at +2 h (`:101`) only if we print "≈10% (10.0038%)" and not "10.0%": the tenth is a
rounding convention, not a product change.

**I do not believe 95.2% is a shrink headline; it is retention.** −496 = **−4.79%** of 10,348 for 80 h = **9.7
minutes per deleted line**. The tree phase moves 121,202 lines in 3 h = 40,401 lines/h — 6,516× cheaper per line.
The code prize also shrank under attack: v1 546 lines → v2 496 (−9.2%), while the bill finally became explicit at
80 h. I would fund **5 h (B1 + B4)** and **0 of the 80**.

## 2. Fatal: true — Attack A: the decision is a fuse, and the default is the whole bill

**Mechanism.** v2 asks the owner to decide C1 at `:108` while the only C1 information in the table is the words
"C1 false" and a line count (`:102–103`); it sets a 2026-11-20 12:00 default that is **B1 + all code phases** —
bundling the 3-hour folder move I can decide today with the 80-hour code plan I cannot. The branches that
genuinely need my judgement (B5/B6) are priced in files, not consequences, so they are not decidable; the ones the
engineer already decided (B7 0% compression, B8 self-refused, B9 arithmetic) sit in the same table (`:96–106`).

**Evidence.** `:108` (date + default); `:60` (80 h); `:102–103` (B5 "C1 false — no screens", B6 "C1 false and the
target still missed"); `:96–106` (six offers mixed with a refusal and an impossibility proof); `:115` (the C1
oracle is "adjudicated by the owner"). The deadline is 14 h 45 m from this writing.

**Consequence.** At 12:01 tomorrow the engineer starts a ten-day, 80-hour spend and 18 hours of human witness on
an implicit yes, while the only thing the table lets me decide (B1 + B4, 5 h) is buried in that default — so the
room gets a signature it can quote and no decision I made. B5/B6 would change the product without showing which
capabilities die — deleting Onboarding's 399 lines deletes the first-run disclosure the legal record tracks as
C11-FIRST-RUN-DISCLOSURE (`docs/legal/evidence/AVD-SMOKE-20261117.md:15`) — and the row never says so.

**Falsification.** Produce the one page below for B5/B6 and split the default into "B1 + B4 only" (or no default).
If that page is decidable, this attack falls.

**The one page — five rows per branch (B1–B6):**

1. **Screens and routes kept/removed** — count and names against today's 4 tabs and 9 routes
(`SabeelRoot.kt:51–70, 151–198`): which tabs remain, where the app opens, what a user can press.
2. **Capabilities lost, in the user's words** — walk start/stop/resume, saved walks, history, CSV export/import
(`SettingsScreen.kt:148,532`), reminders and notifications, goal advice, diagnostics, first-run
disclosure/onboarding.
3. **The 108 tests** — which stay meaningful, which become tests of deleted code, T1 or T2, who rewrites them (B5
says "rewrite T2 tests ≈300" at 0 dev hours), and what "green" proves afterwards.
4. **Data and external promises** — saved walks / Room rows / DataStore keys / CSV files: migrated, orphaned or
exported; manifest FGS types, permissions, notification channels; Play Data Safety and prominent-disclosure
declarations.
5. **Irreversibility and signature** — hours, who signs the C1 exemption in `ruling.md`, what a revert cannot
return after ship, and what the app *is* in one sentence.

**B5 and B6 are not decidable from v2's table.** It prices files, not consequences; B7/B8/B9 are not offers to me
at all and must leave the owner's decision.

## 3. Fatal: true — Attack B: 80 h buys 4.79%, and 15 of those hours are rented eyes

**Mechanism.** 422 of the 496 lines (**85.1%**) are the UI/VM rows (P2 296 + P3 77 + P4 49). Their only witness is
a 136-cell × 5-pass × 3 h = **15 h** manual matrix (`:115`), plus 3 h manual P5/P8 = **18 h human witness = 22.5%
of the plan** (`:62`). The plan's only committed tool is `tools/shrink/` for tree/tokens/split/dup/sha256 (`:40`)
— no capture, crop or diff. After 80 h the repo still has 0 `androidTest`, 0 `@Preview` and no CI screenshot step
(my counts), so the next refactor pays the 15 h again; `:126` admits pixels cannot see TalkBack or 1.31–1.41
renders, so the 15 h does not even complete C1. Worse, `:115` says the matrix is "adjudicated by the owner": the
plan puts my calendar in its bill without asking.

**Evidence.** All from v2: `:62` (80 = 47 + 18 + 15), `:115` (136 cells, 3 h/pass, five passes, owner
adjudicates), `:40` (no screenshot tool), `:126` (three admitted invisibilities).

**Consequence.** The second week either (a) needs 18 h of human eyes — mine or hired — for a manual check that
expires the next time anyone touches a screen, or (b) drops the 422 untestable lines and leaves 74 lines (P5 26 +
P6 14 + P8 34) for 15.5 h = 4.8 lines/h and −0.72% of the code. Neither is something I fund: (a) is a QA ritual
for a refactor that is supposed to change nothing; (b) tells me the code plan is not worth the week.

**Falsification / the evidence that would have kept me, hours not days.** One afternoon: (i) commit a scripted
capture+diff harness — fixed four configs, 136 cells, status bar cropped, hash/pixel diff, manifest — prove it on
the P0 baseline, wire it into the 4-task gate, ≤1.5 h/pass (half the manual rate, a conservative target); 15 h of
eyes becomes a ~6 h one-off plus minutes, and the evidence outlives the refactor. (ii) Make L2 a gate, not a
footnote (`:23`: "measured at P0, reported at close, **not claimed**"): 2 h for the three-task read-closure, and
each UI row ships only if its own task read-closure drops — then "readable for agents" is what I am buying, not a
line count. (iii) If neither, the honest plan is B1 + B4 and no code phases; I will not fund a manual oracle for
4.79%.

## 4. The date I accept

I reject **2026-11-20 12:00**: it is 14 h 45 m away; the five-row page, the L2 baseline and the harness do not
exist; and the default binds 80 h to my silence. A 15-hour fuse on a ten-day spend is not a decision.

I accept **2026-11-24 12:00 Asia/Baghdad** for the code plan, because (i) the missing material — five rows, L2
baseline, harness proof — is 1–2 working days after Friday; (ii) the plan's own close is 2026-12-10 (`:136`) and
no code phase depends on P1 (`:73`), so four days cost the calendar nothing; (iii) I would rather spend two days
deciding an 80-hour spend than sign it in fifteen hours. If the material arrives earlier I decide on receipt:
sheet and harness at **2026-11-23 18:00**, decision the same day.

**B1 and B4 I decide now: yes, 5 hours**, legal co-signs — record that as the owner's answer. If the room insists
on a default, it is **B1 + B4 only**; the 80 h needs an explicit yes.

The tree move kept me. The 95.2% still does not. Five rows and a harness I can run, and I will talk about the
second week.
