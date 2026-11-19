# Council Review — TrueStep (base + gamified prototypes)

**Method:** Qiyas Council, Mode C (merged: Qiyas cards + closed deliberation room, 0–3 dynamic roles, 4 rounds, Maestro ruling).
**Date:** 2026-11-17 (Asia/Baghdad).
**Subjects:** [alshahia/truestep](https://github.com/alshahia/truestep) and [alshahia/truestep_gamified](https://github.com/alshahia/truestep_gamified), cloned at `.external/truestep` and `.external/truestep_gamified`.
**Beneficiary of the review:** Sabeel (`com.sabeel.steps`) — the offline-only, EN/AR step counter in this repository.
**Output language:** English.

---

## 0. Step Zero — Frame and ground

### 0.1 Restated goal

Decide, with evidence and adversarial review, **what Sabeel should learn from these two TrueStep prototypes and what it must refuse to copy**, and give the owner a decision the council can defend: a ranked adoption bundle, a falsification test, and an explicit non-goals list. Done means: every surviving proposal carries a precedent and a causal mechanism, every rejected idea is named, and the first action is executable today.

### 0.2 Hard constraints

| Constraint | Evidence | Consequence |
|---|---|---|
| Sabeel is structurally offline: no `INTERNET` permission, `allowBackup="false"` | `app/src/main/AndroidManifest.xml:21`; `docs/legal/` | Any proposal needing a network path, analytics or cloud is out regardless of value. |
| Sabeel is calm/honest, EN+AR with RTL, 189 string keys parity, 55 JVM tests | `app/src/main/res/values*/strings.xml` | Adoption is judged by: calm information, or external pressure? |
| Owner capacity: solo dev, pre-release, owner-side blockers open (privacy hosting, upload key, Play testing lane, physical-device pass, native Arabic review) | `docs/legal/BLOCKERS.md`, `docs/release/PLAY-SUBMISSION-CHECKLIST.md` | New scope delays the actual Play submission. Low-risk, test-verifiable items first. |
| Play policy surface for step/health apps is heavy (health permissions, FGS type, Data Safety, Health Connect declarations) | `docs/legal/play-compliance-checklist.md` + `docs/legal/sources/` | Health Connect is a compliance project, not a feature ticket. |
| Prototypes are public, single-commit, **unlicensed**, AI-Studio-generated | GitHub API `license: null`; commits `6526c66`/`836c47e`, both 2026-11-17 18:39 +03 | Do not copy code; adopt ideas, rewrite in Sabeel idiom, record provenance. |
| Owner is presumably `alshahia` (repos sit on that account; the user asked to learn from these projects) | GitHub account matching the requested URLs | If ownership is confirmed, reuse is a product/quality question, not a licensing one. The council still rules "patterns, not copy-paste". |

### 0.3 Ownership, licence and provenance (read before any reuse)

* Both repositories are **public** and carry **no licence file** (`LICENSE`, `LICENSE.md`, `LICENSE.txt`, `COPYING`, `NOTICE` all absent). Public visibility is not a licence grant; default is all-rights-reserved.
* Both are **single-commit** repositories created within 60 seconds of each other (15:27:58Z and 15:28:34Z) with a Google AI Studio footprint: `metadata.json` naming `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`, `.env.example` with `GEMINI_API_KEY`, app id `com.aistudio.steptrack.pgydle`, namespace `com.example`.
* If the account is the owner's, the practical rule is still: **take the design idea, re-implement it in Sabeel's code style, do not import files, strings, drawables or names.** That protects Sabeel from inheriting placeholder identifiers, hardcoded strings and the identity references below.
* Recommended durable habit: a `docs/research/provenance.md` entry per adopted idea: "idea borrowed from TrueStep prototype; implementation independent".

### 0.4 Evidence dossier (verified facts)

| # | Fact | Evidence |
|---|---|---|
| 1 | 1 commit each, same message, pushed 2026-11-17 15:39Z; 125 Kotlin files each; main source 15,290 LOC (base) vs 18,992 LOC (gamified); 114 vs 122 `@Test` methods | git log, file counts, `Select-String "@Test"` |
| 2 | The "calm, transparent, offline-first" README is **identical** in both repos (8,227 bytes), yet the "base" repo also ships `ui/screens/journey/components/HeroLoadoutView.kt` and the Hunter model | `README.md` both; file checks |
| 3 | Gamified adds `SystemHologramDialog.kt`, 10 cyber icons, design images. Its own root `implementation_plan.md` says: *"Restore TrueStep's core differentiators (privacy, transparency, adaptive goals, and calm usability). Eliminate gaming distractions from the primary dashboard"*, with remediation 12.2–12.8 still `[PLANNED]` | `.external/truestep_gamified/implementation_plan.md`; `docs/implementation_plan.md` Milestone 12 |
| 4 | Placeholder identity in both: `namespace = "com.example"`, `applicationId = "com.aistudio.steptrack.pgydle"`; `docs/memory.md:5` claims `com.aistudio.steptrack.jigkjt` | `app/build.gradle.kts:12,16`; `docs/memory.md:5` |
| 5 | Release signing silently falls back to the bundled debug keystore (`storePassword = "android"`, `keyAlias = "androiddebugkey"`) when signing env vars are absent | `app/build.gradle.kts:31–45` |
| 6 | "100% offline / nothing leaves the device" vs backup rules that **include** Room DB, shared prefs and DataStore in cloud backup and device transfer | `res/xml/backup_rules.xml`, `data_extraction_rules.xml`, `allowBackup="true"`, `docs/ASO_PLAY_STORE_METADATA.md` ("no personal data collection") |
| 7 | "Zero hardcoded UI strings" is false: **87** literal-String composable occurrences in the gamified UI (e.g. `WalkingSessionSheet.kt:494` `text = "SPM"`; `HeroLoadoutView.kt:258/282/306` "SPARKS/CORES/SHARDS", `:625` "ACTIVATE") | grep over `app/src/main/java`; claim in `docs/environment.md` / `README.md` |
| 8 | IP risk: default hero `Ren Amamiya` (Persona 5 protagonist) in `domain/model/HunterProfile.kt:4`, `ui/viewmodel/StepViewModel.kt:460`, `res/values/strings.xml:607` and AR; "Shadow Monarch", "Solo Leveling", "Jinwoo Sung", "Hunter Dossier", CP/SPARKS/CORES/SHARDS across UI, registries and plans | grep, files in Appendix A |
| 9 | Tests are substantive: 12 named edge cases (multi-source, midnight, timezone change, permission denial, no sensor, recovery, rest day, manual edit, stale data) duplicated in `TrueStepComprehensiveTest` and `StrideTrustComprehensiveTest`; Robolectric `LocalizationParityTest` resolves real EN/AR resources and asserts non-blank + different; Compose tests use test tags; 24-hour cumulative timeline tests | `app/src/test/java/com/example/*.kt` |
| 10 | Toolchain is real and buildable: AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10, `compileSdk = release(36) { minorApiLevel = 1 }`, minSdk 26, Room 2.7.0 (KSP), Health Connect `1.1.0-alpha07`, Robolectric 4.16.1; `compileDebugKotlin` succeeded in 2m20s on JDK 21 (SDK Platform 36.1 auto-installed) | Appendix C |
| 11 | Reconciliation is **lossy and under-documented**: accepted intervals win whole; overlap > 10s excludes the later record entirely; "prefer HEALTH_CONNECT" only breaks ties on equal start times. Watch 08:00–09:00 + phone 08:15–08:45 → 4,000 kept, phone 1,500 dropped | `domain/reconciliation/StepReconciliationEngine.kt` |
| 12 | FGS resilience is aspirational: `startForeground` exceptions are swallowed and the service stops; WorkManager watchdog restarts the FGS from background (restricted on Android 12+); `StepSyncWorker` catches `Exception` and retries | `service/StepCounterForegroundService.kt`, `data/sync/StepSyncWorker.kt` |
| 13 | Documentation drift is systematic: appId mismatch (#4), false "zero hardcoded" (#7), "Play readiness" for a debug-key fallback (#5), "zero unused dependencies" while Firebase/OkHttp/Retrofit/Moshi/Coil/camera deps are commented out in the same file | `app/build.gradle.kts`; `docs/environment.md` |
| 14 | No CI, no wrapper JAR, no `gradlew`/`gradlew.bat`; cannot be reproduced with the documented `./gradlew` commands | file checks both repos |
| 15 | Localization is numerically sound (646 keys base / 697 gamified, EN=AR counts) but the Arabic is machine-register and the "never hardcoded" guarantee is broken; `LocalizationParityTest` proves resource parity, not translation quality | `values*/strings.xml`; `LocalizationParityTest.kt` |

**Unverified at delivery:** physical-device behaviour with real sensors and Health Connect (no evidence exists in either repo). The gamified JVM suite is verified (§C.2); the base-repo suite is verified only if §C.2 records it.

---

## 1. The council

### 1.1 Core roles

| Role | Qiyas duty | Signature question |
|---|---|---|
| **The Expert** — Android health/wellness product architect | Takhrij al-Manat | Why did the precedent really work, and what breaks in 6–24 months? |
| **The Engineer** — senior Android engineer who must implement inside Sabeel's 44 files / 55 tests | Al-Far' | What is the precedent, and what is the cause? |
| **The Devil's Advocate** | Al-Qawadih | Where does the cause break for Sabeel specifically? |
| **The Naive User** — "Amina, 34, walks to work, already has a step counter she ignores" | Tahqiq al-Manat | What is in it for me, and why would I bother? |
| **The Maestro** | Silent judge; rules at the end | — |

### 1.2 Dynamic roles (3 seated)

**Role card — Policy Guardian**
*Mandate:* Play health/privacy policy, permissions, Data Safety and Health Connect declarations.
*Signature question:* "Which declaration does this feature add, and who signs it?"
*Qiyas tool:* Invalid premise (فساد الاعتبار) + second-order effect.
*Red line:* No health-data feature ships if its Play declaration is not prepared before upload.

**Role card — IP & Provenance Counsel**
*Mandate:* licensing, AI-generated code provenance, third-party IP references, brand safety.
*Signature question:* "Whose work is this, and what exactly are we importing?"
*Qiyas tool:* Material difference (الفارق المؤثر) between learning from a design and copying an artifact.
*Red line:* No file, drawable, string or name from these repos enters Sabeel.

**Role card — Behavioral Scientist / Retention Ethicist**
*Mandate:* habit psychology, streak/goal mechanics, pressure vs calm, user wellbeing.
*Signature question:* "Does this motivate, or does it create anxiety and then abandonment?"
*Qiyas tool:* Non-causation (عدم التأثير) + second-order effect.
*Red line:* No mechanic whose retention comes from loss aversion in a "no guilt" product.

---

## 2. Global invariants (Expert)

> **I1. Sabeel's moat is visible, structural honesty.** No INTERNET permission, no analytics SDK, `allowBackup=false` are verifiable in seconds by a reviewer or a curious user. Everything adopted must keep that verification possible. A feature that quietly needs a permission, a backend or a backup exception is a moat breach, not a feature.
>
> **I2. Multi-source trust is TrueStep's real innovation, not the HUD.** The reconciliation + provenance + confidence reasoning treats "where did this number come from?" as product surface. It matters to Sabeel only when Sabeel reads a second source; today there is one source, so the full Trust Center solves a problem Sabeel does not yet have.
>
> **I3. The prototypes are an evidence bank and a warning at once.** Their own plan calls for "eliminating gaming distractions from the primary dashboard". A prototype that needs a de-cluttering plan is telling you what not to build.
>
> **I4. Play policy risk is asymmetric for a solo developer.** A delayed submission costs weeks; a wrong health declaration costs a rejection or a suspension. Ship the safe app, then earn the right to add health-data surfaces.
>
> **I5. No telemetry means no A/B tests.** Sabeel cannot measure adoption with analytics. Decision metrics must be store reviews, support friction and explicit feedback. Proposals that can only be validated with telemetry are unverifiable in Sabeel and must be downgraded.

---

## 3. Qiyas cards

> Al-Far' (proposal) → Al-Asl (precedent) → Al-'Illa (causal invariant) → Tanqih al-Manat (incidental noise) → Conditions (what must be true in Sabeel).

### Card A — Trust/provenance surface

* **Al-Far':** a small always-visible status strip: active source, last update time, plain-language note when data is stale or merged.
* **Al-Asl:** Apple Health "Data Sources & Access" priority list; Strava's per-activity source line. Both answer "my watch says 8,000, your app says 6,200 — which is right?".
* **Al-'Illa:** when a metric can come from more than one collector, users blame the app for discrepancies unless the app shows provenance and merge policy.
* **Tanqih al-Manat:** Apple's full settings matrix is incidental; the invariant is one line: "this number came from X at HH:MM".
* **Conditions:** pays off with ≥ 2 sources or stale/frozen data. Sabeel has one source but real failure modes (OEM kill, no sensor) where "source + last update" is the honest answer.
* **Verdict input:** full Trust Center overbuilds; status strip is liftable.

### Card B — Adaptive goal tiers (minimum / target / stretch)

* **Al-Far':** tiers from a rolling 7-day median (60/100/120%), recalculated weekly, ±10% weekly cap, visible "why this number", user override always wins.
* **Al-Asl:** Apple Watch weekly Move-goal suggestion; Garmin adaptive daily goals. Behavioral precedent: goals anchored to one's own recent baseline preserve self-efficacy; fixed external anchors (10,000) produce failure attribution and churn.
* **Al-'Illa:** the driver is relative anchoring, not the number three. A goal built from your own median is attainable by construction, so a missed day reads "a low day", not "I failed".
* **Tanqih al-Manat:** three tiers are incidental; running median + capped adjustment + explanation is the cause. The gamified multipliers/XP attached to tiers in TrueStep are hostile to Sabeel's brand.
* **Conditions:** ≥ 7 days history; median resists outliers; change cap prevents oscillation; one-sentence explanation; manual override; show nothing until enough data.
* **Verdict input:** survived in reduced, gated form.

### Card C — OEM battery/background diagnostics

* **Al-Far':** "Why is my count low?" — detect manufacturer, show device-specific battery-restriction steps, best-effort deep links, surface proactively only on evidence (permission granted, 0 steps for N hours).
* **Al-Asl:** Samsung Health and Xiaomi Mi Fitness ship "background usage" help; OEM task killers routinely kill FGS trackers.
* **Al-'Illa:** a tracker that silently stops produces the worst review ("doesn't work") while the cause is invisible; a diagnostic turns an invisible OEM policy into 3 actionable steps.
* **Tanqih al-Manat:** the hardcoded component names are implementation detail; the invariant is detect probable cause → instructions → deep link best effort → never blame the user.
* **Conditions:** evidence-gated, dismissible, generic fallback, no new permission, never fire on rest day/manual pause.
* **Verdict input:** survived; cheap and on-brand.

### Card D — Hourly timeline / scrub ribbon

* **Al-Far':** hour-by-hour bars with scrub interaction, as on TrueStep's Home.
* **Al-Asl:** Google Fit hourly activity chart; Apple Health hourly bars.
* **Al-'Illa:** hourly granularity turns one daily number into a narrative without collecting more data.
* **Tanqih al-Manat:** the scrub ribbon, animated KPI grid and "full-day vs scrubbing" state machine are presentation; the invariant is a 24-bucket distribution visible in one chart.
* **Conditions:** increases Home complexity; Sabeel's calm Home is a differentiator. A one-strip Insights variant could be tested later; the full ribbon is not pre-release scope.
* **Verdict input:** downgraded/suspended.

### Card E — Streak resilience (freeze bank, recovery mode, rest days)

* **Al-Far':** protect the streak with earned freezes, recovery mode, planned rest days.
* **Al-Asl:** Duolingo streak freeze — the strongest real-world precedent; it reduces streak-loss churn.
* **Al-'Illa:** loss aversion: users return to avoid losing accumulated identity. It works, and it is a pressure mechanic.
* **Tanqih al-Manat:** the freeze economy is incidental; the invariant is "a streak that does not break on a bad day".
* **Conditions:** Sabeel promises "no guilt, no pressure"; importing loss aversion violates the brand even if retention improves. Honest variant: explicit user-chosen rest days and a streak that pauses rather than breaks — no currency, no purchase, no notification pressure.
* **Verdict input:** freeze economy dropped; honest pause/rest-day variant survives.

### Card F — Evidence discipline: store metadata doc + resource-parity test

* **Al-Far':** (a) an ASO/store-listing doc with char-count validation and EN/AR variants; (b) a JVM test that parses both `strings.xml` files and fails on missing keys, placeholder mismatches, blank values and identical EN/AR strings.
* **Al-Asl:** Play listing limits (title ≤ 30, short description ≤ 80, full description ≤ 4,000); Android resource tooling fails on missing default resources but never checks translation drift; `LocalizationParityTest` is a known Robolectric technique.
* **Al-'Illa:** both turn a policy/quality claim into a checkable artifact. A listing draft catches metadata-policy violations before submission; a parity test catches "shipped Arabic with English leftovers", a class of bug manual review misses.
* **Tanqih al-Manat:** their tests are Robolectric-dependent and their ASO text contains false claims (#4, #6, #7). Copy the practice, not the content, and re-verify every claim against Sabeel's build.
* **Conditions:** Sabeel has pure JUnit; parse XML directly (no new dependency); validate EN against AR; draft ASO in `docs/release/` against the real manifest (no INTERNET, no Health Connect).
* **Verdict input:** survived.

### Card G — Health Connect read-only step import

* **Al-Far':** read `StepsRecord` from Health Connect, merge with the phone sensor, show unified totals.
* **Al-Asl:** TrueStep's implementation; HealthKit/Health Connect as the watch bridge generally.
* **Al-'Illa:** watch owners expect one number; reading Health Connect is the only local way to get it.
* **Tanqih al-Manat:** "zero telemetry" framing hides the compliance cost: health-data declaration, rationale screen, Data Safety answers, double-count risk.
* **Conditions:** Sabeel is pre-release with open owner blockers. Post-release decision: demand evidence + completed health declaration + reconciliation design + precise privacy wording.
* **Verdict input:** suspended on test, not dropped forever.

### Card H — The gamified HUD (negative proposal)

* **Al-Far':** hunter dossier, level/CP, mana banner, SPARKS/CORES/SHARDS, raids, grades, "⚡ START LIVE STRIDE".
* **Al-Asl:** Solo Leveling / Persona-flavoured RPG HUDs (references literally in the code) plus Strava-style gamification.
* **Al-'Illa:** the mechanism that works in games is visible progression feedback. It works when the activity is game-like; for a fitness tracker it becomes noise or coercion.
* **Tanqih al-Manat:** all incidental. The prototype's own plan says: restore calm, eliminate gaming distractions from the primary dashboard, remove arcade mini-bosses/currencies.
* **Conditions:** none that preserve Sabeel's brand. Only quiet milestone recognition survives, without currencies, levels or peer comparison.
* **Verdict input:** dropped.

---

## 4. Transcript

### Round 1 — What the prototypes actually are

**Expert (Takhrij al-Manat):** Start with what these repositories *are*. Two single-commit, unlicensed AI-Studio exports sharing ~90% of their files, created 60 seconds apart, 15.3k and 19.0k lines of Kotlin, 114 and 122 test methods, both shipping the same "calm" README while their code contains the hunter HUD. That is not a product; it is a large, well-tested design exploration. The valuable part is that someone thought hard about (1) multi-source step trust, (2) adaptive goals, (3) honest failure states, (4) evidence discipline. The dangerous part is that the same repo proves how quickly a calm tracker becomes a game.

**Engineer:** Four cards on the table: Trust surface (A), Adaptive tiers (B), OEM diagnostics (C), Hourly timeline (D). Each has a precedent and a cause; all four fit inside Sabeel without adding a dependency.

**Devil's Advocate:** (Naqd — counter-case) Four? The incumbent already has all four, more complete, and it has zero stars and one commit. Traffic on these ideas is not evidence they work. (Material difference — الفارق المؤثر) And here is the fatal difference: TrueStep is a *multi-source* app; its Trust Center solves double counting, which Sabeel does not have. Importing it imports a solution without its problem.

**Naive User (Tahqiq al-Manat):** I already have a step counter. If I open this app and see a grey strip saying "hardware step counter — last update 09:41", I will assume it is broken. Why would I care which chip counted my steps? I care that the number is right and that the app does not nag me.

**Engineer (revising in-line):** Amina is right about the strip's default reading. I revise Card A: the strip appears only when the state is *not* normal (stale data, service stopped, no sensor); normal state stays quiet. That matches Sabeel's existing NO_SENSOR honesty card.

**Policy Guardian (invalid premise):** "Health Connect is local, therefore privacy-neutral" is an invalid premise for Play. Reading Health Connect requires a health-app declaration, a rationale screen and Data Safety answers; "local processing" answers one question but not the declaration. And Card A as first drawn ("show where every step came from") is honest only if accurate; a provenance strip showing a single source is theatre. (Second-order effect) Add Health Connect now and get the declaration wrong, and the submission slips instead of shipping.

**IP & Provenance Counsel (material difference):** The difference that matters is between an idea and an artifact. The reconciliation algorithm is an idea; `StepReconciliationEngine.kt` is an artifact whose behaviour does not match its comment ("prefer primary source" only on equal start times). Copy that file and Sabeel inherits a lossy algorithm and a false comment. Both repos also use `Ren Amamiya` (Persona 5's protagonist) as the default hero name, and "Shadow Monarch"/"Solo Leveling" in registries and plans — trademark/copyright exposure even in a private prototype. None of it may enter Sabeel, and "no licence file" means all rights reserved.

**Behavioral Scientist (non-causation):** The streak freeze is being smuggled in as a UX nicety, but its real cause is loss aversion, which is how Duolingo keeps people. In a calm product it reads as pressure. (Second-order effect) Month one it looks like engagement; month six it is notification fatigue and one-star reviews saying "guilt trip". Card E must split: honest part (planned rest day, pause instead of break) vs. coercive part (currency, freeze economy).

### Round 2 — The plan is forced to change

**Expert:** Two revisions accepted. Card A is not a Trust Center; it is a state strip (stale/stopped/no-sensor). Card D must justify itself against I1/I4: it adds Home complexity and is a nice insight — an experiment, not pre-release scope. Engineer, cut it.

**Engineer:** I revise: D suspended. Home stays as is. A becomes: last-updated timestamp near the ring plus the existing NO_SENSOR card; a "Why is my count low?" screen (Card C) reachable from Settings and shown proactively only on evidence. B stays but gated. F stays. Health Connect (G) suspended pending compliance.

**Devil's Advocate:** (Non-causation) You assume "explaining the median in one sentence" is enough. Users do not read sentences. If the goal changes without warning they will say "the app changed my goal". The cause of adaptive goals working at Apple is *the user opts in and confirms the change*. Also (Reality check) a 7-day median on a new install is nearly meaningless; the first week is atypical precisely because they just installed a tracker.

**Engineer:** Accepted. B activates after 14 days (not 7), proposes the change explicitly ("Your last two weeks averaged 7,400. Keep 8,000, or set 7,400?"), default is "keep", ±10% weekly cap, no silent change ever.

**Naive User:** Good. Now tell me what I actually *see* differently on day one. If the answer is "nothing, plus a screen I'll never open", this whole review produced nothing.

**Engineer:** Day one: nothing changes except the app is more honest when it fails. That is the point — the prototypes' value is in failure handling, not adding surface.

**Policy Guardian:** Then record the Play constraint before it is forgotten: Card C must not casually use `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. A deep link to the battery screen is fine; the exemption intent is policy-sensitive. Keep it settings guidance, not "force unrestricted".

**IP & Provenance Counsel:** `0.3` becomes operational for Card F: write our own XML-parser test; do not import their Robolectric test. Their version asserts AR ≠ EN everywhere, which is itself a wrong invariant (proper nouns and numerals must match). Ours should assert non-blank, placeholder-shape parity, and allow a whitelist of legitimately identical strings.

**Behavioral Scientist:** One more test for the survivor bundle: quiet celebration. A full-screen confetti interruption is pressure; a one-line acknowledgement inside Home is positive feedback. Keep the one-liner, drop the modal.

### Round 3 — Surgery: what falls

**Devil's Advocate:** Force the question: what do these repos give Sabeel that a week of reading its own reviews would not? I claim exactly three things: (1) failure-state honesty (A/C), cheap and brand-aligned; (2) gated adaptive goals with confirmation (B), addressing a real limitation of the fixed goal; (3) evidence discipline (F): parity test + metadata draft. Everything else is already in Sabeel, already rejected by the prototype's own remediation plan, or unverifiable without telemetry.

**Expert:** I accept the three-item core and add a fourth, process-level: the *negative* lessons are worth more than the features. Both repos demonstrate placeholder app ids, debug-key "Play readiness", backup rules contradicting the privacy claim, hardcoded strings despite a "zero hardcoded" claim, and one README for two different products. Sabeel's release gate needs a **claims audit**: every public claim traces to a file or a test.

**Naive User:** And drop the 16 themes. Sixteen themes means sixteen ways for the Arabic layout to break, and I still want one calm app. (Reality check)

**Engineer:** Agreed: themes dropped (Sabeel has fixed theme + dark mode; correct for brand and QA budget). Also dropped: JSON export (CSV exists; no demand evidence), accelerometer fallback (honest NO_SENSOR card beats a fake 80%-accurate count), rating prompt (dark-pattern risk), Health Connect for now. The share card is suspended too: the system share sheet needs no INTERNET permission, but it muddies the trust story. Keep the one-line celebration.

**Behavioral Scientist:** One line survives from Card E: a "pause my streak" control and explicit rest-day marking. Honest, prevents the worst streak anxiety, no currency. Settings-level ability, not a dashboard feature.

**Devil's Advocate:** (Reality check) On B: Sabeel has no telemetry, so you cannot know if the adaptive tier confuses people. But since every change needs confirmation, confusion shows up as "Keep" clicks — that is the metric. I withdraw the objection. On C: the proactive trigger (0 steps for N hours) can misfire on a rest day or a disabled sensor; it must never fire when a rest day is marked.

**Engineer:** Accepted: C's trigger requires permission granted, no rest day, no manual pause, no steps in 6h, and a visible dismissal.

### Round 4 — Stress and stop

**Policy Guardian:** Final check on the survivor set: A no new permission; B no new permission (explanation must not claim medical benefit); C no new permission (settings deep links only); F internal; E' no new permission. Zero new permissions and zero new dependencies across the bundle — the privacy story stays literally true.

**IP & Provenance Counsel:** Final check: no imported artifacts; the parity test is Sabeel's own XML parser; store metadata written fresh and fact-checked against the manifest; no names, strings, drawables or algorithms copied; provenance note recorded.

**Behavioral Scientist:** Final check: nothing in the survivor set uses loss aversion, social comparison or interruption. The only new nudge (goal suggestion) needs explicit consent; the only interruption-like surface (diagnostics) is evidence-gated and dismissible.

**Naive User:** Would I use it? The diagnostic screen, yes, the first time my count looks wrong. The goal suggestion, maybe once. The status line, only when something is wrong — good. Everything else I won't notice. That is acceptable.

**Devil's Advocate:** Stop condition met, no new objection. Recorded what fell: gamified HUD/currencies, 16 themes, Duolingo-style freeze economy, accelerometer fallback, full Trust Center, hourly scrub, Health Connect now, JSON export, rating prompt, share card. Nothing survived by inertia.

**Expert (closing):** Then the plan is smaller than the repos, and that is the correct outcome.

---

## 5. Maestro's verdict

### 5.1 Verdict table

| Branch | Status | Al-Asl | Al-'Illa | Constraint imposed |
|---|---|---|---|---|
| A. Trust Center (full provenance matrix) | **Downgraded → state line** | Apple Health data sources; Strava source line | Provenance resolves discrepancy attribution | One source only; show abnormal states (stale, stopped, no sensor); no permanent single-source theatre. |
| B. Adaptive goal tiers | **Survived (gated)** | Apple Watch Move-goal suggestion | Relative anchoring preserves self-efficacy | Activate after 14 days; explicit confirmation; default keep; ±10%/week; one-sentence explanation; manual override. |
| C. OEM battery diagnostics | **Survived (evidence-gated)** | Samsung Health / Xiaomi battery help | Invisible OEM kills become invisible "broken app" reviews | No new permission; no battery-exemption abuse; dismissible; never on rest day/pause. |
| D. Hourly ribbon/scrub | **Suspended on test** | Google Fit hourly activity | Hourly distribution gives narrative without new data | Not pre-release; only a 24-bucket Insights strip after release, if reviews ask. |
| E. Streak freeze economy | **Dropped** | Duolingo streak freeze | Loss aversion drives retention | Conflicts with "no guilt" promise. |
| E'. Honest rest day / pause streak | **Survived (small)** | Product principle | Honesty > pressure | Settings-level; no currency; no notifications. |
| F. Parity test + store metadata draft | **Survived** | Play listing limits; resource parity testing | Claims become checkable artifacts | Own XML parser (no Robolectric); fix the "EN≠AR" wrong invariant; metadata fact-checked against manifest. |
| G. Health Connect read import | **Suspended on test** | HealthKit watch bridge | Watch owners expect one number | Post-release; needs health declaration + Data Safety + demand evidence; merge must avoid double count. |
| H. Gamified HUD / CP / currencies / IP names | **Dropped** | Prototype's own remediation plan | Progression feedback only works when activity is game-like | Excluded from Sabeel permanently. |
| I. 16 themes | **Dropped** | — | — | QA matrix ×16 in RTL; brand dilution. |
| J. Accelerometer fallback | **Dropped** | Negative precedent | Accelerometer pedometers under/over-count and drain battery | NO_SENSOR honesty stays. |
| K. Debug-key release fallback / "Play readiness" | **Rejected as anti-pattern** | Negative precedent | Silent fallback ships unshippable artifacts | Sabeel already hard-fails release signing; keep it; add a claims audit. |
| L. Backup-enabled step DB | **Rejected as anti-pattern** | Negative precedent | Backup copies the step DB off-device | Keep `allowBackup=false`; if changed, change the privacy copy first. |

### 5.2 Step Zero — cheapest falsification test

**Test (one day, no new deps):** on a branch, implement only the smallest survivor surface: (1) Home stale/no-update state line; (2) Settings → "Why is my count low?" with generic + OEM paths; (3) the EN/AR XML parity test. Build, run the 55 tests + the new test, install on the api36 AVD (no step sensor, so it genuinely exercises NO_SENSOR and diagnostics), inspect AR screens.

**Decision table:**

| Result | Decision |
|---|---|
| Parity test finds a real divergence, or diagnostics is unreachable/confusing in AR | Fix before anything else; do not start B. |
| All three land cleanly, no new dependency, AR/RTL correct | Proceed to B next, behind the 14-day gate. |
| Deep links fail on AVD/OEM variants but instructions readable | Keep generic instructions only; drop deep links. |
| Stale line cannot be exercised on the AVD | Keep it code-review-only; verify on the physical-device pass. |

### 5.3 The plan

**Wave 1 — "Honest failure" (this week, before Play submission)**
1. `HomeScreen`: quiet line only when data is not fresh (source label + "last updated HH:MM" or "counting paused"). Reuse the existing NO_SENSOR card; no new composables beyond one status line.
2. `SettingsScreen` → "Why is my count low?": permission state, service state, rest-day/manual-pause state, generic OEM instructions, best-effort deep links (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`, battery optimization screen), explanation that the phone sensor counts. No new permission.
3. `LocalizationParityTest`: JUnit-only XML parser over `values/strings.xml` and `values-ar/strings.xml`; asserts same key set, non-blank values, ordered placeholders (`%1$s` etc.), reports identical strings with a whitelist. No Robolectric, no new dependency.
4. `docs/release/CLAIMS-AUDIT.md`: every public claim mapped to evidence file/line (NO INTERNET → manifest; no upload → manifest + privacy text; no analytics → dependency list; not a medical device → about text). This is the TrueStep lesson applied to Sabeel.

**Wave 2 — "Gentle goals" (after v1 ships)**
5. Adaptive suggestion after 14 recorded days: median of last 7 days rounded to nearest 100; 60/100/120 as ring labels, not three competing goals; confirm dialog; default keep; ±10%/week cap; "why" line with the user's own median; user-set goal wins.
6. Honest streak control: "Pause streak for a rest day" in Settings; pauses rather than breaks; no currency, no notification pressure.

**Wave 3 — "Earned surfaces" (post-store-traction or explicit owner decision)**
7. Health Connect read-only import behind the compliance checklist (health declaration, rationale, Data Safety, merge design). Not before the first store release; declaration work is larger than the code.
8. Optional 24-bucket hourly strip on Insights if store reviews ask.

**Explicit non-goals:** gamified HUD/levels/currencies/raids/quests, IP-flavoured names or art, 16 themes, accelerometer fallback, JSON export, rating prompt, cloud backup, Health Connect before compliance, any network permission, any analytics.

### 5.4 Continue / kill metrics (no telemetry)

| Decision | Observable metric | Date |
|---|---|---|
| Wave 1 ships | 55 existing tests + new parity test pass; lint 0 errors; AR smoke reachable; release still builds/signs | 2026-11-24 |
| B proceeds | Wave 1 verified; no new permissions/deps; no new review-blocking declaration | 2026-11-24 |
| B suggestion acceptable | Owner dogfooding + first store reviews; if reviews call the suggestion confusing, freeze it behind a setting | 30 days after v1 store release |
| C diagnostic worth keeping | If "stopped counting" reviews appear, make it more prominent; if nobody opens it and no reports, leave as is | 30 days after store release |
| G starts | ≥ 5 independent watch-data requests (reviews/emails) **and** compliance checklist complete | Not before 2027 Q1 |

### 5.5 Risks and mitigations

1. **Policy Guardian:** diagnostics slip into battery-exemption prompting → no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; settings deep links only; decision recorded in the Play checklist.
2. **Policy Guardian:** Health Connect re-proposed without compliance → Wave 3 gate written into `docs/legal/play-compliance-checklist.md`; suspended, not forgotten.
3. **IP & Provenance Counsel:** accidental copy/paste → provenance log; rewrite from scratch; parity test written fresh; no assets imported.
4. **IP & Provenance Counsel:** brand contamination from Persona/Solo Leveling-adjacent naming → hard exclusion list; Sabeel naming already clean.
5. **Behavioral Scientist:** goal suggestion becomes silent pressure → confirmation required, default keep, no streak-loss tie-in, no missed-tier notification.
6. **Behavioral Scientist:** celebration/share becomes social pressure → one-line acknowledgement only; share sheet suspended.
7. **Devil's Advocate:** scope creep delays release → Wave 1 is 3 code touches + 1 test + 1 doc, verifiable this week; B/C do not block submission.
8. **Naive User:** "why bother?" → nothing new on day one except honesty when something is wrong; the app stays quiet when it works.

### 5.6 First action (today)

Create `feat/honest-failure` and implement item 1 only: the Home stale/no-update status line, sourced from the existing `StatusStore`/service state; write EN+AR strings; run `testDebugUnitTest` and `lintDebug`; commit. Then item 3 (parity test) the same day — it is the cheapest and protects every later wave.

---

## 6. Review of the two repositories as artifacts

**Verdict: not shippable as products today, but unusually strong raw material.**

Not shippable:
- Placeholder identity (`com.example`, `com.aistudio.steptrack.pgydle`, a third id in docs), versionCode 1, generic name.
- Release signing falls back to the debug keystore — a build produced that way cannot be a legitimate release.
- No licence, no CI, no wrapper JAR, single commit; not reproducible by a stranger with the documented commands.
- Documentation contradicts code on the four claims that matter most (privacy, hardcoded strings, app id, Play readiness); the "calm" README is identical in the repo that ships the HUD.
- The gamified repo's own plan admits the core problem and lists unstarted remediation.

Valuable:
- The reconciliation/confidence design and the 12 named edge-case tests are genuine thinking about multi-source step data.
- `LocalizationParityTest` and test-tag-based Compose tests are reusable *practices*.
- OEM battery helper, adaptive-goal math, hourly-timeline math and streak-freeze model are documented, testable prototypes of ideas that would otherwise take days to spec.
- `ASO_PLAY_STORE_METADATA.md` is a useful template for the listing work Sabeel still owes.
- A live demonstration of the failure mode Sabeel is positioned to avoid: adding game systems to a calm tracker, then planning their removal.

**Difference between the repos:** the "base" repo is not the calm product; it is the same gamified code one generation earlier, with the README already describing the calm vision. The gamified repo is the experiment after the design direction was approved (its plan cites confirmed decisions). Neither contains the calm implementation the README promises.

---

## Appendix A — Evidence ledger (repo-relative paths)

| Claim | Path(s) |
|---|---|
| Placeholder app id / namespace | `app/build.gradle.kts:12,16` |
| Documented app id drift | `docs/memory.md:5` |
| Debug-keystore fallback | `app/build.gradle.kts:31–45` |
| Backup contradiction | `AndroidManifest.xml`; `res/xml/backup_rules.xml`; `data_extraction_rules.xml`; `docs/ASO_PLAY_STORE_METADATA.md` |
| Hardcoded UI strings | `ui/components/WalkingSessionSheet.kt:494`; `ui/screens/journey/components/HeroLoadoutView.kt:258,282,306,625` |
| IP-flavoured defaults | `domain/model/HunterProfile.kt:4`; `ui/viewmodel/StepViewModel.kt:460`; `res/values/strings.xml:607`; `domain/content/DestinationRegistry.kt:178–184` |
| Own remediation plan | `implementation_plan.md` (root, gamified); `docs/implementation_plan.md` Milestone 12 |
| Reconciliation algorithm | `domain/reconciliation/StepReconciliationEngine.kt` |
| FGS + sync worker | `service/StepCounterForegroundService.kt`; `data/sync/StepSyncWorker.kt` |
| Tests | `app/src/test/java/com/example/TrueStepComprehensiveTest.kt`, `StrideTrustComprehensiveTest.kt`, `LocalizationParityTest.kt`, `HourlyTimelineTest.kt`, Compose tests |
| Store metadata claims | `docs/ASO_PLAY_STORE_METADATA.md` |
| Build stack | `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts` |

## Appendix B — What may be reused, and how

| Thing | Reuse? | How |
|---|---|---|
| Source files, algorithms, strings, drawables, names | **No** | Rewrite from the idea; provenance log entry. |
| Reconciliation/confidence ideas | Later only | When Sabeel reads a second source; design against Sabeel's own model. |
| Adaptive-goal idea | Yes | Sabeel version: median, confirmation, cap, explanation. |
| OEM diagnostics idea | Yes | Manifest-verified state; generic fallback; settings deep links. |
| Parity-test practice | Yes | Fresh XML-parser JUnit test. |
| ASO document structure | Yes | Fresh facts, verified against Sabeel's manifest. |
| HUD/levels/currencies/themes/IP content | **No** | Explicit non-goal. |

## Appendix C — Build and verification log

### C.1 Toolchain (verified, not assumed)

- Downloaded Gradle 9.3.1 (137,037,885 bytes) and ran the project directly: AGP 9.1.1 resolved from Google Maven, Kotlin 2.2.10, `compileSdk 36.1` (Android SDK Platform 36.1 downloaded and installed automatically), JDK 21.
- `gradle :app:compileDebugKotlin --offline`: **BUILD SUCCESSFUL in 2m 20s**, 9 tasks executed. Only warning: `google-services.json is missing` (plugin configured to WARN, so it does not block).
- Conclusion: the version stack is real, the Kotlin sources compile, and "it builds" is verified. The first non-offline attempt was interrupted by the environment's background-job limit during dependency download; the offline rerun used the warmed cache.

### C.2 JVM unit tests (verified)

- Gamified checkout — `gradle :app:testDebugUnitTest`: **BUILD SUCCESSFUL in 4m 11s**; JUnit result XML parsed and totalled: **122 tests, 0 failures, 0 errors, 0 skipped**. The prototype's "122 green tests" claim is verified for this repo.
- Base checkout — same command: **BUILD SUCCESSFUL in 6m 20s**, **114 tests, 0 failures, 0 errors, 0 skipped**. The claim is verified for both repositories.
- Note: the run needs network on first execution to fetch Robolectric's Android runtime jars and Maven dependencies; it is not an offline-hermetic suite.

---

*Council report prepared 2026-11-17. Sources: the two cloned repositories (commits `6526c66` and `836c47e`), the GitHub REST API, and local build logs. No changes were made to Sabeel or to the prototypes during this review.*

## Appendix D — Adoption record (2026-11-17)

What was taken from this council into Sabeel, where it lives, and what was deliberately left out.
Each item is a fresh implementation written against Sabeel's own architecture; no file, asset, string
or code block from the reference repositories was copied, and their clones stay untracked under
`.external/`. **Zero new permissions and zero new dependencies** were added for any item below.

| Wave / item | What was adopted | Where it lives | How it is verified |
|---|---|---|---|
| Wave 1 — failure-state honesty | Home "Updated HH:mm" line and rest-day note; a Diagnostics screen with honest status, permission state, missing-sensor explanation, "what can pause counting", OEM battery guidance and app/battery-settings deep links | `ui/screens/DiagnosticsScreen.kt`, `HomeScreen.kt`, `platform/OemBatteryGuide.kt`, EN+AR strings | 5 `OemBatteryGuideTest` tests; 4 EN + 4 AR adoption UI dumps; crash-buffer transcript |
| Wave 1 — gated adaptive goal | Settings "Suggest from my routine" button, explicit-confirm dialog (default keep), median of recorded days, needs ≥14 recorded days, moves ≤10 % and ≥200 steps, manual goal always wins | `domain/GoalAdvisor.kt`, `AppViewModels.kt`, `SettingsScreen.kt` | 9 `GoalAdvisorTest` tests; EN and AR dialogs show median 7,450 → proposal 7,500 and the keep path leaving 8,000 |
| Wave 1 — EN/AR parity guard | JUnit test that parses both string XMLs: same keys, no blanks, equal placeholders, no untranslated prose outside a small allowlist | `app/src/test/java/com/sabeel/steps/i18n/LocalizationParityTest.kt` | 4 tests, no new dependencies; it caught wording drift during this pass |
| Wave 2 — explicit rest day | Settings switch marking today as a rest day; streak pauses without adding a day; counted steps untouched; markers pruned after 365 days | `StepMath.kt`, `SettingsStore.kt`, `StepsRepository.kt`, `AppViewModels.kt`, `SettingsScreen.kt` | 6 rest-day `StepMathTest` tests; the emulator pass caught a real precedence bug (rest day that also hit the goal added a day), fixed and covered |
| Claims discipline | Read-only claims audit of user-facing copy, then corrections to strings, README, policy and checklist | `docs/release/CLAIMS-AUDIT.md` including its §6 resolution addendum | Audit plus the resolution log; final numbers in README and `docs/legal/release-signoff.md` |
| Wave 3 (suspended) — gamified layer | **Not adopted**: HUD, currencies/economy, streak-freeze shop, theme system, accelerometer fallback, hourly scrub, Health Connect, JSON export, rating prompt, social sharing | — | Council suspended Wave 3; prototype measurement claims were unsupported |

**Final state of the adopting tree:** 79 JVM tests, 0 failures; `lintDebug` exit 0 (0 errors);
signed debug and release artifacts rebuilt (hashes in `dist/SHA256SUMS.txt`); emulator evidence in
`docs/review/2026-11-17/adoption-smoke/`. Physical-device validation is still pending, and the
prototype-only gamified material stayed out because the council found its measurement claims
unsupported.

**Superseding note (2026-11-19):** the row "accelerometer fallback — not adopted" above is kept as the
2026-11-17 record, but the 2026-11-19 Qiyas Council re-opened **only** a last-resort, visibly labelled,
no-new-permission accelerometer estimate behind a measured-first ladder (counter -> detector -> labelled
estimate). It is not the prototype's silent fallback and no prototype code or copy was used. See
`docs/superpowers/plans/2026-11-19-step-source-resilience.md`. The rest of the suspended Wave 3 list
(HUD/economy, theme system, hourly scrub, Health Connect, JSON export, rating prompt, social sharing)
stays suspended.

