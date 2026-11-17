# Qiyas Council — verdict: a clean-room Arabic-first step & walk tracker

**Mode:** C (deep causal reasoning + adversarial council, subagents used where knowledge was missing)
**Convened:** 2026-11-16 · **Maestro:** this session · **Inputs:** six public store listings (5 Play + 1 App Store), product brief, competitor research, name screening, legal/ḥalāl clearance memo.
**Question:** _Can we build a legitimate, shippable product with the same idea as these six apps — and what must be true for it to survive contact with the market?_
**Hard constraints:** clean-room only (no APK, no decompilation, no scrape, no copied expression); Play policy and target-SDK 36; honest claims; halal product rules; v1 must be free (Iraq cannot register a merchant account).

---

## 1. Facts checked before seating

| # | Claim | Status | Source |
|---|---|---|---|
| F1 | Step counting + goal + chart is already free on most Android phones | VERIFIED | Samsung Health 1B+ installs, free, no ad badge; Health Connect is the OS step hub |
| F2 | Ads are the category norm | VERIFIED | 4 of 5 Play listings carry "Contains ads"; all six sell IAP ($1.49–$189.99) |
| F3 | "No ads" alone is not a moat | VERIFIED | A competing Play listing already markets "Pedometer without Ads" |
| F4 | No target listing claims Arabic / RTL | VERIFIED | #5 FR/EN only; #6 English only; Arabic Play search returns the same English-first apps |
| F5 | MapMyWalk is alive, so "abandoned" is not our precedent | VERIFIED | Android updated 2026-11-07; iOS 26.16.0 on 2026-11-10; Outside Inc. acquired MapMyFitness 2024-08-30 |
| F6 | #5 and #6 "Runify" are different products by different publishers | VERIFIED | FR race-communication app (Domaine du Net) vs US gamified run app (OneDegree Labs) |
| F7 | Acceptance pain points are systemic, not individual bugs | VERIFIED | 4/5 listings' reviews cite ads, battery-kill, accuracy, subscription, sync loss, date bugs |
| F8 | The name "Khatwa" collides with a same-concept Arabic offline step project | VERIFIED | 12 Play apps + ahmad-nayfeh.github.io/khatwa (offline, Arabic, streak, APK v0.10.0) |
| F9 | Android requires targetSdk 36 for new apps (since 2026-08-31, extension ended 2026-11-01) | VERIFIED | Play target-API policy; our build now targets 36 |
| F10 | From Iraq: Play developer registration possible, **merchant registration not available** | VERIFIED | Play Console supported-locations list; treasury/payout requirements |
| F11 | A "no data collected" Data-safety declaration is truthful only if nothing leaves the device | VERIFIED | Not possible with INTERNET permission or default auto-backup; our manifest forbids both |
| F12 | Clean-room reimplementation of ideas is legally and sharably permissible; expression is not | VERIFIED | Clearance memo §3–§6 (IP/publicity/trade-dress + ḥalāl analysis) |

**Not verified (residual risk):** trademark status of "Sabeel" (screen only, not a clearance); whether any specific phone's step counter is accurate; ad-network eligibility for an Iraq-registered developer; Play account/payment eligibility for the owner. These are handled in §6.

---

## 2. Council seating

| Member | Function |
|---|---|
| **Expert** (consumer-mobile growth and habit mechanics) | Extracts the causal invariants; runs qiyas cards; decides what "must remain true" |
| **Engineer** (Android, build + platform) | Grounds every proposal in what can be built and kept green |
| **Devil's Advocate** | Constructs the strongest case for "this will not work", attacks every consensus |
| **Naive User** (الزبون الغبي: 29, nurse in Baghdad, Samsung A-series, 7k steps/day, Samsung Health preinstalled, hates ads, saw a relative's location shared by an app, reads Arabic first but tolerates English) | Tests product claims with zero technical charity |
| **Policy Guardian** (dynamic) | Play policy + store claims + declarations; asks "which line gets us rejected or suspended?" |
| **Privacy & Security Auditor** (dynamic) | Provability of the offline claim: backup, permissions, export paths |
| **Accountant** (dynamic) | Unit economics from Iraq: which money is even receivable, and when |

**Dynamic role cards**

- **Policy Guardian** — signature question: does every sentence in the listing match the manifest? Invalidation: an invalid premise ("privacy app" + auto-backup, or "no permissions" + transitive OS permissions). Red line: a claim the build cannot prove. Edge: new apps need 12 testers / 14 continuous days.
- **Privacy & Security Auditor** — signature question: can a user prove from the phone that data never left? Invalidation: INTERNET, ACCESS_NETWORK_STATE that stays declared, backup that uploads the database, an export that silently overwrites. Red line: data loss without a user-owned copy.
- **Accountant** — signature question: where does the first dollar come from, and can it be deposited from Iraq? Invalidation: revenue modelled on IAP/paid app/ad payouts when merchant registration is unavailable. Red line: burn (fee + hours) without a retention gate.

---

## 3. Global invariants (Expert)

1. **Substitution is the first-order risk.** The phone already counts steps for free. Our app survives only if it supplies something the built-in does not: provable local-only data, Arabic-first UX, and a daily trigger that feels like care rather than nagging. Feature count is not a wedge.
2. **Retention is a trigger problem.** The product is opened because something external asks it to be opened (notification, ring, streak, goal), not because the app exists. Measured in D4/D7 opens, not installs.
3. **Accuracy perception is set in the first three days.** If the count disagrees with the phone's built-in counter, the user leaves and tells others. The pipeline must reconcile, not just count.
4. **Trust is binary and provable.** "No INTERNET permission" is a stronger privacy sentence than any prose, and a single false claim (backup, hidden network, vague "no permissions") destroys it. Our listing words must be derivable from the manifest.
5. **Expression is not an asset; ideas are.** The only shippable path is original code, original design, original copy, original brand. The competitor listing is evidence of demand, not a template.
6. **v1 revenue is zero by construction.** No merchant registration from Iraq → no paid app, no IAP; no ad SDK without INTERNET → v1 is free, no-ads. v1 is an audience and credibility asset; revenue is a later experiment.

---

## 4. Qiyas cards

**Q1 — Core offline step tracker.**
Asl: Samsung Health (hardware counter, zero friction) fused with StepsApp's goal ring (immediate visual feedback).
ʿIlla: the cost of opening is zero and every open returns one honest number against a personal goal.
Ṭanqīḥ (conditions): hardware step counter present; notification permission; onboarding under 60 s.
Noise rejected: preinstallation (cannot copy), StepsApp's visual identity (expression), streak shaming.

**Q2 — GPS walk with our own drawn route.**
Asl (positive): Strava — effort becomes an artifact worth keeping.
Asl (negative): MapMyWalk — account gate, location sharing, sync loss, subscription pressure → 1★ churn.
ʿIlla: the artifact (route + stats) is valuable without a network, a map tile, or an account.
Conditions: foreground location permission; 20 s checkpointing; sensor-only fallback; no tiles, no map keys.

**Q3 — Arabic-first, English at parity.**
Asl: localized-first MENA apps (language + norms = trust). ʿIlla: people keep what speaks their language and does not punish them for it. Noise: religious content (not our category). Conditions: real translations, full RTL, date-boundary correctness (competitors ship "Yesterday" bugs), Arabic screenshots and listing.

**Q4 — Ads-supported free tier.**
Asl: Leap Fitness (100M+ installs, ads). ʿIlla: huge free reach + short sessions → ad inventory.
Conditions: INTERNET + ad SDK + Data-safety "sharing/collection" + tier-1 eCPMs + user tolerance.
Verdict condition failed: destroys invariant 4 and is not the reason users switch. Downgraded.

**Q5 — Paid Pro / subscription.**
Asl: StepsApp IAP, Runify Pro ($4.99/mo–$79.99). ʿIlla: power users pay for exports/analytics once retained.
Conditions: retention first, merchant registration, store billing. Suspended: cannot be received from Iraq, and it would pressure the first 1,000 users before trust exists.

**Q6 — 1:1 clone of a listing's UX/brand.**
Dropped outright: expression copying (code/UI arrangement/copy/assets), trade dress, likelihood of confusion; contradicts both the legal memo and the ḥalāl conditions. The idea is used; the expression is not.

---

## 5. Transcript (condensed, decisions only)

**Round 1 — scope.** Engineer proposed feature parity: steps + GPS + streaks + challenges + leaderboards + widgets + subscription. **Expert:** the cause of retention is not the feature; three of those require network and would falsify the only defensible claim. **Devil's Advocate (counter-case):** "Samsung Health is free and already on the phone; nothing here gives a user a reason to install a second counter." **Naive User:** "My phone already counts my steps. I'm not installing another app that shows me ads and asks for my location." → **Fatal flaw #1: substitutability.** Engineer revises scope to: offline core, Arabic parity, walk artifact, adaptive goal, export.

**Round 2 — claims audit.** Policy Guardian rejects the first draft: targetSdk 35 is not submittable for a new app; allowBackup=true makes the privacy claim false; "no permissions" is false (OS transitive permissions); Data-safety must be "no collection". Revised: targetSdk 36, allowBackup=false, remove ACCESS_NETWORK_STATE via tools:node="remove", listing says **"no INTERNET permission"**, health-FGS declaration + demo video, privacy policy URL before closed test.

**Round 3 — data-loss audit.** Auditor: no cloud means a lost phone loses a year of walks; a crash mid-walk loses a route; without import, export is decorative. Revised: user-owned CSV export **and import**; 20 s walk checkpointing; date-rollover and reboot tests in pure Kotlin; widget deferred to v1.1 (fast follow, not v1 risk).

**Round 4 — the money question.** Accountant: from Iraq the Play merchant registration is unavailable, so IAP and paid app are impossible; ad revenue needs INTERNET and a network SDK (breaking the wedge); the only certain cost is the $25 fee plus ~20 hours. Maestro ruling: **v1 free and ad-free, revenue test deferred until D30 retention and country eligibility are proven.**

**Closing — second-order effects.** Expert: if the wedge works, the differentiator is provable trust + Arabic quality, so the store listing must be written like an engineering claim, and data portability protects the rating when users change phones. Devil's Advocate: the most likely failure is not competitors, it is indifference — the trigger is too weak, so Step Zero must test it before polish. Naive User: "I'll open it if it tells me something my phone doesn't, and doesn't nag me." → becomes Step Zero's qualitative test.

---

## 6. Maestro verdict

| Branch | Verdict | Asl | ʿIlla (effective cause) | Binding constraint |
|---|---|---|---|---|
| Clean-room offline step tracker | **Survived — core** | SHealth + goal ring | zero-friction honest daily feedback | match built-in accuracy; no ads/account/INTERNET |
| Arabic-first + full RTL | **Survived — core** | localized-first MENA apps | language trust creates retention | real translations, date-boundary tests, Arabic listing |
| GPS walk with own Canvas route | **Survived, downgraded** | Strava artifact / MapMyWalk failure | artifact without network | permission fallback, 20 s checkpoints, no tiles |
| Ads monetisation | **Dropped from v1** | Leap ad scale | free reach → inventory | requires INTERNET + data sharing; breaks claim |
| IAP / Pro subscription | **Suspended** | StepsApp / Runify Pro | retained power users pay | no merchant registration from Iraq yet |
| Social, challenges, leaderboards | **Dropped (v2)** | Strava/StepsApp social | social proof amplifies retention | needs network + new data-safety review |
| 1:1 UI/brand clone | **Dropped** | — | — | expression copying; legal + ḥalāl red line |
| Home-screen widget | **Suspended → v1.1** | Leap ads/widgets | surface proximity = trigger | do after Step Zero validates the trigger |
| Cloud sync / accounts | **Dropped** | competitor sync bugs | — | contradicts wedge; adds breach surface |
| CSV export **and import** | **Survived — added** | — | user owns the data, device change survivable | plain CSV, SAF, no network |
| "No permissions" claim | **Dropped — false** | — | — | claim exact: **no INTERNET permission** |

**Step Zero (falsification test — run before polish or publishing).**
Ship the debug APK to 10 walkers (6 Arabic-primary, 4 English-primary) for 7 days. Measure:
(a) D4 voluntary-open rate ≥ 40%; (b) opens/day ≥ 1.5; (c) step accuracy within ±10% of the phone's built-in counter over 3 days; (d) crash-free sessions ≥ 99%; (e) battery drain ≤ 3% per 24 h while counting; (f) ≥ 6/10 testers name privacy or Arabic as a reason they kept it.

| Outcome | Ruling |
|---|---|
| (a)+(f) pass | Invest in polish → closed test → production |
| (a) fails, (f) passes | Trigger is too weak → build the widget / fix notification → retest before any new feature |
| (f) fails | Wedge is wrong → reposition (Arabic walking plans, or B2B) or stop; do not add features |
| (c) fails | Accuracy bug → fix pipeline first; no feature work until green |

**The solution (what was built today).** App **Sabeel** (سبيل), package com.sabeel.steps; offline, no account, no ads, no INTERNET permission; English + Arabic with full RTL; minSdk 26, targetSdk 36. Scope: onboarding, foreground step service with reboot/midnight reconciliation, home ring + stats, 7/30-day history, walk tracking with own Canvas route and checkpointing, insights, reminder + goal notifications, settings (goal, units, body, language, export/import, delete-all), privacy screen, original icon and design system. Build: Kotlin + Compose, Room, DataStore, WorkManager; AGP 8.10.1, Gradle 8.11.1, JDK 21.

**Launch plan (owner actions in brackets).** 1) Privacy policy live and linked [owner: host on GitHub Pages]. 2) Play Console account + $25 + identity [owner; Iraq supported]. 3) Closed test: 12 testers / 14 continuous days [owner + 12 people]. 4) Production listing with Arabic + English screenshots [owner + us]. 5) Sabeel trademark screen in Iraq/WIPO + counsel/scholar sign-off [owner] — before the first public build, not after.
**Timeline:** Week 0 build (done) · Week 1 Step Zero · Weeks 2–3 closed test · Week 4 production decision.

**Continue / kill metrics.** Continue if: D7 open-rate ≥ 25%, D30 ≥ 12%, crash-free ≥ 99.5%, store rating ≥ 4.2 after 50 ratings, support < 1 h/week, zero policy strikes. Kill/pivot if: D7 < 15% after one widget iteration, or the Step Zero wedge test fails twice. Budget cap: $25 + 20 owner-hours; no paid spend before D30.

**Risks and mitigations.** Substitutability → wedge + Step Zero; Play account/payment from Iraq → owner verifies before closed test; claim falsification → manifest-provable wording + legal checklist C2/C4; data loss → export + import; Arabic quality → native review of strings and screenshots; OEM battery killers → documented limits + reconciliation; GPS battery drain → sample interval tuned, no map rendering; trademark → screen now, clearance before listing; "free now, pay later" trust → v1 makes no subscription promise.

**First action (30 minutes, today).** Install the debug APK on two physical phones (one Samsung, one Xiaomi/Huawei) and compare 24 h step counts against the built-in counter. In parallel, the owner opens Play Console registration from Iraq to confirm identity + payment eligibility. Both are cheap, and either can invalidate the plan before any polish is spent.

---

## 7. Clean-room record

**Used:** public listing pages (features, ratings, review excerpts, declared data-safety), public app-store APIs, public ownership/news pages, general OS documentation.
**Never used:** APK/AAB or any binary, decompilation, disassembly, source scraping, screenshots or artwork, paid API internals, competitor names/brands in our product.
**Artefacts:** KEEP = our own code/design/text/icon and public listing facts; SEALED = legal memo + evidence (none produced by RE); SANITISE = quotes in docs must stay short and attributed; DESTROY = raw HTML scratch (tmp/research/); NEVER = competitor binaries. Full table and commands: docs/legal/clearance-memo.md, docs/legal/play-compliance-checklist.md.

**Decision summary:** build the clean-room product, ship it free and offline, prove the wedge with ten people before spending a dollar, and keep every public sentence provable from the binary.

---

## 8. Execution note (2026-11-17, after the build)

- **Built in this session:** the full v1 above — Kotlin/Compose app Sabeel (com.sabeel.steps), 8 screens,
  platform services, 41 pure-JUnit tests green, Gradle wrapper 8.11.1, JDK 21.
- **Artifact:** app/build/outputs/apk/debug/app-debug.apk, 11,488,512 bytes, mtime 2026-11-16T23:51:37+03:00,
  SHA-256 324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E.
- **Independent static verification:** C:\dl\legal-evidence\final-20261117-000050\EVIDENCE.txt —
  build identity, C2 badging (targetSdk 36, minSdk 26, package com.sabeel.steps), C4 permissions
  (0 INTERNET, 0 ACCESS_NETWORK_STATE; the expected health/location/boot set present), C5 allowBackup=false
  + foregroundServiceType, C6 merged manifest, C8 dependency audit (no ads/analytics/network SDKs),
  C9 16 KB zipalign + ELF p_align=16384, C10-S static no-transmission checks. Unit tests 41/41 pass.
- **Runtime:** attempted on an API 36 emulator (WHPX); emulator boot failed on this host, so C7/C10-R/C11
  runtime rows are **NOT VERIFIED — emulator-attempted**, not PASS. The FGS demo video, screenshots and the
  12-tester / 14-day closed test remain device/owner actions.
- **Not yet done (by design):** Step Zero pilot (no user has run it on a physical phone), privacy-policy
  hosting, Play Console registration, signing keystore, trademark clearance, Arabic native review.

