# COUNCIL VERDICT — Sabeel (سبيل), v1
**Session:** 2026-11-17 · **Workspace:** E:\android_apps\steps_app · **Maestro:** Lead
**Evidence base (six independent reviews, all cited to file:line):** [core-logic](core-logic.md) · [android-platform](android-platform.md) · [ui-ux-a11y](ui-ux-a11y.md) · [build-security](build-security.md) · [product-legal](product-legal.md) · [market-strategy](market-strategy.md)
**Maestro re-verification:** ACTIVITY_RECOGNITION requirement for TYPE_STEP_COUNTER on API 29+ (official docs); health-FGS runtime prerequisite and SecurityException-after-startForeground rule (official docs + AOSP policy + API reference); 41/41 tests and merged-manifest contents independently reproduced by build-security; dist APK SHA-256 and merged permissions re-checked by the Maestro. **Not verified anywhere:** any install on a device, TalkBack, native Arabic review.

---

## 1. METHOD — Qiyas (causal analogy), not vote-counting

Each finding was tested as an analogy: **aṣl** = the platform/product law or measured fact; **ʿillah** = the causal mechanism it guarantees; **farʿ** = this artifact. A finding survives only if the ʿillah is present in the code. Findings that survived a hostile seating were kept even when inconvenient; findings that could not produce a mechanism were dropped.

**Council seats (dynamic, 9).** Expert (product/systems), Engineer (Android/platform), Devil's Advocate (falsifies every claim), Naive User (Arabic first-time user, 30 seconds of patience), Policy Guardian (Play/legal), Privacy & Security Auditor, Data Steward (integrity/ownership), Marketer (outside-in market), Accountant (time/opportunity cost). Maestro chairs and rules; no seat may flatter.

**Global invariants (hold for every recommendation):**
1. Offline is absolute: no INTERNET, no analytics SDK, no network call, ever. Measurement must be local-only.
2. Proven privacy beats claimed privacy: every privacy statement must be provable from the artifact (permissions, manifest, no egress).
3. Numbers must be honest even when sad: no invented days, no inflated first day, no silent gaps, no denominator tricks.
4. Arabic is first-class, not a translated afterthought: locale, RTL, notifications, and copy must survive an Arabic phone.
5. No feature work before the trust-critical defects are closed; no polish before the release pipeline exists.
6. A claim without an acceptance test is marketing.

---

## 2. COUNCIL TRANSCRIPT (condensed)

### Session 1 — "Is v1 feature-complete?"
**Expert:** Features are present: pipeline, two FGS, CSV, reminders, 9 screens, 2 languages.
**Devil's Advocate:** Present ≠ complete. Walk history is writable and invisible — `WalkRepository.walksFlow()` has no consumer. There is no in-app status surface for counting. README says "feature-complete" while `lintDebug` exits 1 and no release artifact exists. The claim is falsifiable from the tree.
**Engineer:** Agreed; also the restore story is broken: CSV import writes rows the live pipeline overwrites (missing spec §3.5 max).
**Ruling:** "v1 feature-complete" **falls**. Correct statement: *feature-present, integration-incomplete, release-unbuilt.*

### Session 2 — "Does the number tell the truth?"
**Data Steward:** Four independent failures. (a) First sample on a fresh install is counted in full because baseline starts at 0 (`StepPipeline.kt:10,29-30`; test `StepPipelineTest.kt:15-21` locks it in) — a phone up for three days shows thousands of "today" steps. (b) Spec's max(db, pipeline) is not implemented, so a restored day is overwritten within 1.5 s. (c) Rollover books the whole unrecorded delta to the new day, so an overnight outage inflates today. (d) Midnight writer stamps yesterday's active minutes onto today and `seed()` re-imports them. Add clock-rollback clobbering, Insights dividing by present rows while Home divides by 7, and a streak silently capped at 60.
**Devil's Advocate:** Is (a) real or theory? **Engineer:** real where the hardware counter retains since-boot totals (most devices); magnitude device-dependent, which is worse — it is unbounded and invisible. Fix is ten lines: seed baseline from the first sample, today = 0.
**Privacy Auditor:** This is existential, not cosmetic. The product's only differentiator is "honest numbers". A single inflated first day destroys it more thoroughly than any ad SDK could.
**Ruling:** "Honest numbers" **falls as current fact**; becomes P0. Spec-vs-code conflict on rollover (H3) must be decided, not left to a test that encodes the bug.

### Session 3 — "Can it ship on Android 14+?"
**Engineer:** Critical, verified independently. `StepCounterService` calls `startForeground(type=health)` in `onCreate` unconditionally (`StepCounterService.kt:73-77,212-221`). Android 14+ enforces the health FGS runtime prerequisite (ACTIVITY_RECOGNITION, HIGH_SAMPLING_RATE_SENSORS, or a health permission) and throws `SecurityException` *after* the call, on the service main thread. `ACTIVITY_RECOGNITION` is optional in onboarding, Finish is always enabled (`OnboardingScreen.kt:164-170`), and `finish()` starts the service regardless (`AppViewModels.kt:295-305`). The app's own copy invites the user to skip — and the reward is a process crash at the end of onboarding.
**Policy Guardian:** This is also a declarations problem: Play's health-FGS declaration expects the service to be legitimately usable; a crash triggered by a skipped prerequisite is the kind of defect reviewers and users punish.
**Devil's Advocate:** Then worse than F1: there is no re-arm. Only onboarding, BOOT_COMPLETED and a 15-minute worker start the service; a background FGS start is illegal on API 31+ and the exception is swallowed (`Reminders.kt:133-139`). Opening the app never restarts counting. So after any force-stop or update, steps silently stop forever, with no in-app sign (`F2`, `F3`).
**Ruling:** F1 **Critical**, F2/F3 **High**. Ship-readiness: **NO**.

### Session 4 — "Is Arabic-first real?"
**Naive User:** Installed on an Arabic phone. The first screen was English (`Models.kt:10` defaults EN; `AppLocale.kt:26-33` always applies it). I chose Arabic. The charts now put every weekday under the wrong bar because bars are drawn left-to-right on Canvas while the labels are mirrored by RTL (`BarChart.kt:78-116`). The notification and its channel stayed English. "You can start without permissions" is false — without ACTIVITY_RECOGNITION the step counter is unusable on Android 10+ (verified). The body-data page says "optional, clear any time" but settings only offer steppers, and "Imperial (mi, lb)" leaves height/weight in cm/kg.
**Engineer:** All confirmed; plus AAB language splits can strip Arabic resources on an English-locale install unless `language.enableSplit=false` is set and verified (`lint AppBundleLocaleChanges`).
**Marketer:** And the wedge itself is occupied: Leap (100M+) already markets "100% private, no login" in Arabic; Hitchhike خطواتي (50M+, 4.8★) markets offline. "First Arabic offline" is not available as a claim.
**Ruling:** "Arabic-first" **falls** to *Arabic-present, Arabic-not-delivered*; the market wedge **falls** as a uniqueness claim. Arabic becomes table stakes plus a craft moat, not the pitch.

### Session 5 — "Is privacy real, and is it enough?"
**Privacy Auditor:** Architecture survives hostile review: no INTERNET/ACCESS_NETWORK_STATE/AD_ID in the merged manifest or APK; no ad/analytics/network SDK in an 847-line dependency graph; allowBackup=false with every domain excluded; 9 permissions total; PendingIntents immutable; zero logging; no WebView/dynamic code; policy site is CSP-locked and request-free. This is genuinely rare and provable.
**Devil's Advocate:** The claims layer does not survive. "Delete all data" leaves the step service running, so the next step re-inflates today from the boot counter (`AppViewModels.kt:241-251` + `StepCounterService.kt:167-190`). The About screen claims license notices ship — there is no LICENSE/NOTICE file. "Export your history" exports daily totals only. And the privacy policy is an unhosted draft with bracketed placeholders and no in-app link, which alone fails Play policy.
**Ruling:** Privacy **architecture**: survives (A). Privacy **promises**: partially fall (C+). The egress-free design is the one asset worth protecting from every future feature.

### Session 6 — "What is this worth, and what should be spent?"
**Accountant:** The engineering is real, but every hour now goes to defects, not features. No VCS, no CI, no signing, lint red, 0% platform tests.
**Marketer:** Consumer-growth bet: NO-GO. Commodity category absorbed by the OS (Google Health/Health Connect, OEM preinstalls); Arabic-offline occupied; Iraq has no Play merchant registration and no PayPal/Stripe, so zero lawful consumer revenue. Three kill-metric experiments cost ≤$30 and ≤10 days and replace an unfalsifiable "Step Zero" (no analytics by design yet D4-open-rate KPIs).
**Policy Guardian:** Before any of that: privacy-policy URL + in-app link, signed AAB, health/FGS/data-safety/IARC declarations, real screenshots and FGS videos, and — for personal accounts — 12 testers continuously for 14 days. All currently missing; the saved "evidence" is black screenshots and a 0-byte video.
**Ruling:** Value = portfolio/credential (best EV/hour) or B2B white-label (only lawful revenue). Consumer growth: **NO-GO** until demand evidence exists.

### Session 7 — Forced evolution
Every seat had to nominate an idea it brought or defended that must now fall. Result in §5.

---

## 3. MAESTRO VERDICT

**One line:** *Sabeel is a carefully built, genuinely private, well-documented prototype that is not yet a product: it can crash the exact user its own copy invites, reports numbers it cannot defend, claims an Arabic-first identity it does not deliver on first launch, and has no release or legal path to a store — but its offline architecture and engineering discipline are real assets worth finishing.*

### Status scorecard
| Dimension | Grade | One-line judgment |
|---|---|---|
| Vision & scope discipline | A− | Focused v1, no ads/no network, clear DoD; wedge partly falsified |
| Domain logic | B+ | Pure, readable, tested; first-run baseline wrong, rollover deviates from spec |
| Data/persistence integrity | C | No max() rule, no transactions, destructive migration, dishonest denominators, streak cap |
| Services / platform | D | Critical FGS crash, no re-arm, no status surface, checkpoint race, silent notification denial |
| UI/UX | B | Calm, consistent, gentle; primary CTA below fold, walks invisible |
| Arabic / RTL | C− | Exact string parity and RTL primitives, but EN default, mirrored-chart bug, English notifications, grammar defects |
| Accessibility | B− | Content descriptions, 48dp, fontScale on 3 screens; unlabeled switch, no headings, contrast fails |
| Privacy architecture | A | Provable offline posture, no egress, backup blocked |
| Privacy truthfulness | C+ | Delete-all broken, licence claim false, export overstated, policy unreachable |
| Build / release engineering | D | No VCS/CI/signing/AAB; lint red; reproducibility not shown |
| Testing | C− | 41/41 JVM tests pass; 0% of platform/data/service/UI |
| Documentation | B | Unusually thorough and self-critical in evidence labels; README overstates verification |
| Market evidence | F | No users, no waitlist, no store assets, no instrumented learning loop |
| Play submission readiness | F | No policy URL, no signed AAB, no declarations, no assets, 12/14 clock not started |

### What is proven right (keep, defend, do not regress)
1. **Offline is real and verifiable** — merged manifest and APK contain no INTERNET, no ACCESS_NETWORK_STATE, no AD_ID; `tools:node="remove"` demonstrably works.
2. **No monetization/analytics SDKs** — 847/776-line dependency graphs contain no ads/analytics/crash/attribution libraries.
3. **Backup genuinely blocked** — allowBackup=false plus data-extraction/full-backup rules excluding every domain and device transfer.
4. **Security hygiene is above hobby standard** — immutable PendingIntents, no logging, no WebView/dynamic code/sockets, SAF-only I/O, no background location, no exported sensitive components.
5. **Architecture is sane and separable** — pure domain, repository, DataStore for prefs, Room for rows, work-managed reminders, two FGS; the pure pipeline handles mid-day restarts and reboots correctly.
6. **Tests exist and pass** — 41/41 on a fresh build (independently re-run); CSV codec has real adversarial coverage (RFC4180-ish, locale-independent decimals, no formula injection).
7. **Material/design discipline** — original palette per spec, 20dp cards, no dynamic colour, edge-to-edge with safeDrawing, RTL-safe start/end primitives, auto-mirrored icons, 48dp targets, fontScale adaptations on 3 of 4 screens, honest and gentle copy.
8. **Accessibility foundations** — text alternatives on every canvas visual, per-button descriptions, EN/AR string parity exact (170+1/170+1, sensor 22/22, zero placeholder drift).
9. **Documentation honesty** — brief/spec/legal/evidence packs are unusually complete; runtime evidence is labelled NOT VERIFIED; the sign-off says "NOT APPROVED FOR SUBMISSION".
10. **The privacy-site itself** — static, CSP-locked to `default-src 'none'`, no external requests.

### What is wrong — the defects that decide the product
**P0 — blocks any external user (fix this week):**
| # | Defect | Evidence | Consequence |
|---|---|---|---|
| 1 | **Critical:** Android 14+ health-FGS crash when ACTIVITY_RECOGNITION is skipped | `StepCounterService.kt:73-77,212-221`; `OnboardingScreen.kt:164-170`; `AppViewModels.kt:295-305` | Process crash at the end of onboarding for the user the copy invited to skip |
| 2 | **Critical (data):** first run counts the whole since-boot counter | `StepPipeline.kt:10,29-30`; `StepPipelineTest.kt:15-21` | Thousands of fake steps on day one; trust gone in 30 seconds |
| 3 | **High (data):** "delete all data" does not stop/reset the service | `AppViewModels.kt:241-251`; `StepCounterService.kt:167-190` | Erasure promise broken; today re-inflates from residual in-memory raw |
| 4 | **High (data):** missing spec §3.5 max(); import overwritten; zeros written over history; rollover delta and midnight active-minutes pollution; clock rollback clobbers | `StepsRepository.kt:60-107`; `CsvIo.kt:39-52`; `StepPipeline.kt:29-38`; `StepCadence.kt:42-52` | Restore is a lie; wrong day gets the steps; history can be destroyed |
| 5 | **High (Arabic):** EN default on Arabic phones; mirrored-chart mismatch; notifications/channel names not localised | `Models.kt:10`; `AppLocale.kt:26-33`; `BarChart.kt:78-116`; `Notifications.kt:135-166` | First launch contradicts the identity; Arabic data is misread |
| 6 | **High (process):** no VCS, no CI, lint fails, no signed AAB | repo root; `lintDebug` exit 1; `app/build.gradle.kts` release block | No rollback, no provenance, no release path |

**P1 — blocks a Play submission (next 1–2 weeks):**
- No re-arm after force-stop/update + no status surface (F2/F3) → start from every foreground entry point, `MY_PACKAGE_REPLACED`, and a visible "counting / needs attention" card.
- Notification permission denial consumed silently; goal flag written before `notify` (F4).
- AAB language-split risk for the in-app switch (lint `AppBundleLocaleChanges`; fix `enableSplit=false`, verify `bundleRelease` on device).
- Release unshippable: no signingConfig, minify off, versionCode 1, empty proguard, no `bundleRelease`, unsigned APK "DOES NOT VERIFY" (F4 build).
- Room: `version=1`, `exportSchema=false`, `fallbackToDestructiveMigration()` → any future schema bump silently wipes all history.
- CSV import: not transactional, bypasses mutex, no walks, no version column, no confirmation before overwrite.
- Walk checkpoint race (CME swallowed), no resume-after-reboot offer, no walk-history surface, back gesture silently abandons a live walk.
- Insights/History denominators disagree with Home; streak capped at 60; "steps/120" active-minutes fallback undocumented.
- Arabic: default locale, plurals (`value_minutes`, export/import counts, "آخر 30 أيام"), terminology drift, "الـGPS", imperial/cm-kg contradiction, "two permissions only" contradiction.
- Accessibility: unlabeled reminder Switch; permission state stale on resume; no headings; ring TalkBack triple-announce; non-text contrast (ring mint end 1.77:1, goal line 2.10:1, below-goal bars 2.49:1); Settings fontScale 2.0 rows overflow.
- Play/legal: unhosted placeholder privacy policy and no in-app link (Critical for Play); unsigned AAB (Critical); Data safety/Health/FGS/IARC declarations (Critical); real screenshots/feature graphic/FGS video (High); 12-testers/14-days gate (High); trademark clearance before the permanent package name (High); LICENSE/NOTICE missing while About claims notices ship (Medium).

**P2 — before growth:** Health Connect export (write-only, no INTERNET), sensor-missing and permission-denied repair UX, previews/instrumentation tests, dependency currency (Compose 1.7.6 → 2026 line), dependency verification/lockfiles, battery measurement, local-only event log for measurement, design-system cleanup (clay unused, launcher mark weak).

### What fell (forced evolution — at least one idea must die)
| Idea | Status | Why |
|---|---|---|
| "v1 is feature-complete" | **Fallen** | Lint red, no release artifact, invisible walks, no status surface, restore broken |
| "Arabic-first offline is an unclaimed wedge" | **Fallen** | Leap/Hitchhike Arabic listings already claim privacy/offline; and app opens in English |
| "The numbers are honest" | **Fallen** | First-run overcount, missing max(), rollover, denominators, restore overwrite |
| "No analytics is pure virtue" | **Revised** | Purity is real, but Step Zero cannot measure itself; evolve to a local-only, opt-in event log — never network |
| "Privacy claims are backed" | **Partially fallen** | Architecture backed; delete-all/licence/export/policy claims not |
| "Health FGS declaration done right" | **Partially fallen** | Types/permissions/PendingIntents right; prerequisite gating absent → crashes |
| "Delete-all erases everything" | **Fallen** | Service, checkpoint, locale and notification prefs survive; next step re-inflates today |
| "Arabic RTL layout is handled" | **Partially fallen** | Primitives and parity right; Canvas chart mislabels days |
| "15-minute worker recovers counting" | **Fallen** | Background FGS start is illegal on API 31+ and swallowed |
| "Offline architecture, test discipline, privacy posture" | **Survived** | Independently reproduced six ways |

### Gaps that no amount of code review closes
1. **No user evidence** — zero installs, interviews, waitlist, reviews; the only runtime media are black frames and a 0-byte video.
2. **No release chain** — no upload key, no AAB, no CI, no reproducible artifact; the dist hash certifies one instance, not the tree (clean rebuild differs: 11,366,468 B vs 11,488,512 B).
3. **No measurement loop** — by design there is no telemetry; Step Zero asks for activation numbers it cannot observe.
4. **No legal path to publication** — unhosted policy, missing declarations, uncleared name, no 12/14 test clock.
5. **No commercial path** — Iraq merchant registration unavailable; no donations; ads would falsify the only provable claim.
6. **No native Arabic review** — an explicit launch precondition that has not happened.

### How to improve — sequenced, with acceptance tests
**This week (P0, ~2–3 days):**
1. `git init`; add `.gitignore` (build/, .gradle/, .kotlin/, .dsh/, local.properties), LICENSE, first commit; tag `pre-fix`.
2. Gate the FGS: check the health prerequisite inside the service; never start it without a grant; boot path and walk fallback guarded; degraded state becomes a UI state. **Accept:** emulator API 34+, skip permissions → no crash, Home shows "counting off — grant permission".
3. Seed the baseline on the first sample (today = 0); replace the test that enshrines the bug. **Accept:** fresh install on a phone up 3 days → today 0.
4. Make delete-all stop the service, reset the in-memory pipeline/checkpoint/tracker and clear locale/notification prefs. **Accept:** after delete-all, zero rows and zero resurrection on next step.
5. Implement max(db, pipeline) in `recordRawStep`/`reconcile`; never write 0 over an existing day; import seeds the pipeline under the mutex; decide rollover semantics explicitly and test the decision.
6. Fix Arabic P0: default language from device locale, RTL bar order, localise notifications/channels. **Accept:** Arabic-locale phone starts in Arabic; Arabic chart labels align with bars.
7. Fix or justify the 5 lint errors so `lintDebug` exits 0; add `exportSchema=true` + a real migration policy.

**Next cycle (P1, ~1–2 weeks):** start-on-foreground + MY_PACKAGE_REPLACED; Home status card; notification-denial fallbacks; release signing + R8 + `bundleRelease` + `language.enableSplit=false` verified on device; CSV transactions + walks + version + overwrite confirmation; walk history/resume/back contract; one definition of "7-day average"; Arabic native pass; contrast/fontScale/Switch fixes; Robolectric tests for the P0 paths; then the Play apparatus (hosted bilingual policy + in-app link, Data safety, Health/FGS declarations with real videos, IARC, screenshots, 12 testers/14 days clock started).

**Then (P2, growth gate):** replace Step Zero with a local-only instrumented pilot — 20 strangers from a walking group/university, opt-in event log exported by CSV, measuring invite→install, first successful count, D1/D4 voluntary opens, reboot/rollover/force-stop accuracy against a reference count, 24h battery. Kill: >2% step error, >3%/24h drain, D4 <40%, or any "silent stop" observed.

### Decision register (Maestro's calls, owner may overrule with reasons)
| # | Decision | Maestro call |
|---|---|---|
| D1 | Is English-first intentional? | No — default to device locale; override only on explicit choice |
| D2 | Rollover: spec (drop unrecorded delta) vs code (book to new day) | Follow the spec: drop the unattributable delta; amend the spec only with written rationale |
| D3 | Is CSV the backup story? | Only if walks + version + merge preview are added; otherwise say "daily totals only" everywhere |
| D4 | Revenue objective | Portfolio/credential first; B2B services as the only lawful revenue; never ads (would falsify the one provable claim) |
| D5 | Name | Full Iraq/WIPO/USPTO/package clearance **before** the first AAB; the package name is permanent |
| D6 | "You can start without permissions" copy | Delete it: without ACTIVITY_RECOGNITION the app cannot count on Android 10+; say what is actually true |
| D7 | Feature work | Frozen until P0 is green; no widget/Health Connect "for retention" before demand evidence |

### First action today (one sentence)
Run `git init` and commit the tree, then fix the three trust-critical defects — FGS crash gate, first-run baseline, delete-all — in one branch with three new tests and a skip-permissions run on an API 34+ emulator; nothing else ships until that is green.

---

## 4. PERSPECTIVES BEYOND THE CODE (the "all other angles" the brief asked for)

- **User psychology:** the product's promise is calm honesty; a single fake number or a crash at the moment of trust is unrecoverable. First run must be quiet and truthful (0 steps is a valid, credible answer).
- **Ethics/trust:** "delete all data" is a moral statement, not a button. It must be atomic, observable and true.
- **Maintenance economics:** a solo offline app lives or dies on its release chain and migrations. Destructive migration and no VCS are slow-motion data loss.
- **Competition:** the OS is absorbing step counting (Google Health, Health Connect, OEM preinstalls); differentiation must be craft and provable restraint, not feature parity.
- **Business:** Iraq cannot pay through Play; ads break the thesis; B2B services are the only revenue path; the portfolio/credential value is real and near-term.
- **Legal:** Play's gates are calendar time (12/14) plus declarations, not code; privacy policy hosting, health/FGS declarations and trademark clearance all precede any launch.
- **Learning loop:** no telemetry by design is right, but no measurement at all is a blind spot; a local-only, opt-in log preserves both.
- **Long-term:** if the app remains a 1.0 that is honest, private and Arabic-correct, it is a durable small asset; every added sensor/cloud feature reduces that durability.

---

## 5. EVIDENCE NOTES / CAVEATS
- Severity tags: core-logic 4 High, 8 Medium, 8 Low; android-platform 1 Critical, 2 High, 4 Medium, 3 Low; ui-ux 5 High, 9 Medium, 7 Low; build-security 4 High, 6 Medium, 4 Low; product-legal 3 Critical, 3 High, 4 Medium, 2 Low; market 4 High, 5 Medium, 2 Low. Overlaps were merged by the Maestro; where reviewers disagreed (tests pass vs unverified), the executed evidence (build-security) wins.
- Legal Criticals are launch blockers by policy, not code vulnerabilities.
- No device/emulator execution, TalkBack, native Arabic review, or AAB behaviour was verified by anyone; every such claim is marked inference in the source reports.
- The workspace was being rebuilt live during the review (APK timestamp changed); artifact facts are time-stamped in each report.
