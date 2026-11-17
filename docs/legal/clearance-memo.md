# Clearance memo — Sabeel (formerly Khatwa) clean-room step tracker vs. six public competitor listings

**Status:** INTERNAL DECISION MEMO — draft for the owner, qualified counsel and a qualified scholar.
**Date:** 2026-11-16 · **Author:** legal (Agent Team, task-2) · **Reviewers:** human owner (pending), Iraqi counsel (pending), scholar (pending).
**Subject plan:** clean-room Android app **Sabeel** (working name; renamed from Khatwa during build on 2026-11-16; Android label Sabeel), package com.sabeel.steps: a local-only step/walking tracker built from (a) public Google Play / App Store listings, (b) public developer policies and terms, (c) our own product brief and specification. **No APK was downloaded, installed or decompiled; no target account was accessed; no scraping tool was run.**

> **Not legal advice. Not a fatwa.** I am neither a lawyer nor a mufti. This memo is a reasoning aid and an evidence record. A commercial launch needs review by qualified Iraqi counsel (IP, consumer, privacy, distribution) and by a qualified scholar for the sharīʿah questions. Where a fact could not be verified it is marked **UNVERIFIED** or **UNKNOWN**.

---

## 1. Scope and status

### 1.1 What is being assessed

Six public competitor listings, their public developer sites and their published policies:

| # | App | Owner / developer | Store | Identifier | Listing snapshot |
|---|-----|-------------------|-------|------------|------------------|
| 1 | Step Counter - Pedometer | Leap Fitness Group | Google Play | pedometer.steptracker.calorieburner.stepcounter | tmp/research/app1.html |
| 2 | StepsApp – Step Counter | StepsApp GmbH (Austria) | Google Play | com.stepsappgmbh.stepsapp | tmp/research/app2.html |
| 3 | Step Tracker - Pedometer | Leap Fitness Group | Google Play | steptracker.healthandfitness.walkingtracker.pedometer | tmp/research/app3.html |
| 4 | Map My Walk: Walking Tracker | MapMyFitness, Inc. / Outside Interactive, Inc. (US) | Google Play | com.mapmywalk.android2 | tmp/research/app4.html |
| 5 | Runify (Android) | Domaine du Net (France) | Google Play | com.domainedunet.runify | tmp/research/app5.html |
| 6 | Runify: Running Tracker (iOS) | OneDegree Labs | Apple App Store | id6746146450 | tmp/research/app6.html |

The factual observations used in our brief (feature set, data-safety declarations, price model, rating) are recorded in the team research notes and in the snapshot table in §2.

### 1.2 How the material was obtained

- Manual retrieval, on **2026-11-16**, of public store listing pages and public developer web pages (privacy policies, terms) using a normal browser session; plain-text/HTML snapshots saved under docs/legal/sources/.
- **No** app download, **no** APK/AAB acquisition, **no** installation, **no** decompilation, **no** extraction of strings, resources, assets or databases, **no** account creation or login on any target, **no** automated scraping, and **no** circumvention of any technical measure.
- Store listing data-safety summaries are **self-declared by the developers**, not audited by Google and not a licence. They are treated here as public facts only.

### 1.3 Licence / EULA status

- **None accepted.** The team has not installed any target app and has not accepted any target EULA or account terms. We are not licensees of any target.
- A licence, if we had one, would not transfer ownership and would not authorise copying, redistribution or derivative works; the only route to a lawful rebuild is a genuinely clean room or a written licence/API permission (see §6).
- The developers' website terms (e.g. Outside Inc.) govern use of those websites; our access was ordinary reading of publicly posted pages for legal review, not scraping or commercial exploitation of their content.

### 1.4 Jurisdictions

| Jurisdiction | Relevance | Status |
|---|---|---|
| **Iraq** (development and publishing location) | Copyright of the targets and of our app; consumer/privacy questions; Play registration | Copyright baseline: Law No. 3 of 1971; **no decompilation/interoperability exception located**; verify with counsel. Play: developer registration **supported**, merchant registration **not supported** (§2, checklist §1). |
| **Target owners' countries** (US, Germany/Austria, France) | Their IP rights and contract terms; cross-border enforcement | Their national copyright/trade-mark law and terms apply to their works; not analysed in detail here — refer to counsel before commercial launch. |
| **Distribution markets** (Google Play worldwide) | Consumer law, privacy/GDPR-type regimes, health claims regulation | v1 transmits no user data off device, which reduces exposure, but market-by-market consumer/health-advertising rules still need counsel review. **UNVERIFIED** beyond Play policy. |
| **Data subjects** | Users' step/location data | v1: none leaves the device; no account, no server, no SDKs that transmit. Policy still required by Play. |

### 1.5 Plan recorded before acting (skill step 0)

> We will read six public store listings and the developers' public privacy/terms pages, record facts and at most five-line verbatim extracts with URL and date, and write our own feature/specification baseline. We will not download, install or decompile any target, not scrape at scale, and not copy listing text, UI, icons or assets into the product. The build will be original Kotlin/Compose code with original strings, icons and layout; only unprotected facts, ideas and industry standards (step counting, MET-based calorie estimate, route drawing, streaks) inform it. Snapshots are stored in docs/legal/sources/, raw listing HTML in tmp/research/ (internal only), both inside the workspace with access limited to the team.

### 1.6 Team clean-room statement

No member of this team has worked on, contracted for, or had access to the internal code, documentation or assets of any target. The specification is derived from public facts plus our own product brief. **The Lead and the human owner must confirm this statement before publication** (it is a factual assertion, not a legal conclusion).

### 1.7 Out of scope

No iOS build; no comparison/“better than X” marketing; no use of any target name, logo, iconography or trade dress; no access to target users' data; no reverse engineering of any kind; no training of any model on target content.

---

## 2. Sources

All sources were fetched on **2026-11-16**. Snapshot file names are relative to docs/legal/sources/. A full index is in docs/legal/sources/README.md.

### 2.1 Target listings (public facts only)

| App | Listing URL | Data-safety declaration shown on listing | Notes |
|---|---|---|---|
| 1. Step Counter - Pedometer | https://play.google.com/store/apps/details?id=pedometer.steptracker.calorieburner.stepcounter | “No data shared with third parties”; collects Location, Personal info and 4 more; data encrypted in transit; “Contains ads” | 100M+ installs |
| 2. StepsApp | https://play.google.com/store/apps/details?id=com.stepsappgmbh.stepsapp | May share Personal info; collects Personal info, Health and fitness and 5 more; “Contains ads” | 10M+ installs |
| 3. Step Tracker - Pedometer | https://play.google.com/store/apps/details?id=steptracker.healthandfitness.walkingtracker.pedometer | “No data shared with third parties”; collects Location, Health and fitness and 3 more; “Contains ads” | 50M+ installs |
| 4. Map My Walk | https://play.google.com/store/apps/details?id=com.mapmywalk.android2 | May share Location, Personal info and 3 more; “Contains ads” | 10M+ installs |
| 5. Runify (Android) | https://play.google.com/store/apps/details?id=com.domainedunet.runify | “No data shared with third parties”; collects Personal info; “Data isn't encrypted”; no “Contains ads” badge | 10K+ installs; Sports |
| 6. Runify (iOS) | https://apps.apple.com/us/app/runify-running-tracker/id6746146450 | App Store privacy label not captured as a policy substitute | 4.8 stars / 245 ratings; IAP $4.99/mo–$79.99; 13+ |

### 2.2 Target terms, policies and developer sites

| App | Document | URL | Document date | Status at fetch |
|---|---|---|---|---|
| 1 | Privacy Policy | https://leap.app/privacypolicy.html?pkg=pedometer.steptracker.calorieburner.stepcounter | Last updated 2025-01-10 | Fetched. No public ToS/EULA located (leap.app terms/legal paths return a JSON 404 body). |
| 2 | Privacy Policy | https://steps.app/privacy?os=android | Not stated on snapshot | Fetched. |
| 2 | Terms of Service v1.2 | https://steps.app/terms | Last updated 2026-10-28 | Fetched. |
| 3 | Privacy Policy | https://leap.app/privacypolicy.html?pkg=steptracker.healthandfitness.walkingtracker.pedometer | Last updated 2025-01-10 | Fetched. Same publisher as #1; no public ToS located. |
| 4 | Privacy Policy | https://www.outsideinc.com/privacy-policy/ | Not stated on snapshot | Fetched (MapMyWalk links here). |
| 4 | Terms of Use | https://www.outsideinc.com/terms-of-use/ | Last updated 2025-10-03 | Fetched. |
| 5 | Privacy Policy (French) | https://runify.appliendirect.fr/legal | Not stated on snapshot | Fetched. |
| 5 | CGU / Terms (French) | https://runify.appliendirect.fr/fr/terms | Last updated 23 September 2026 | Fetched. The /en/terms page also served French text at fetch time. |
| 6 | Privacy Policy (linked from listing) | https://runifyprivacy.carrd.co/ | — | **404 / dead** at access date. |
| 6 | Terms of Service (linked from listing) | https://runifytermsandconditions.carrd.co/ | — | **404 / dead** at access date. |
| 6 | Privacy Policy (archived) | http://web.archive.org/web/20261024171201/https://runifyapp.com/privacy-policy | Archive 2026-10-24 | Archived copy; live site returned HTTP 402. Terms remain **UNKNOWN**. |
| 6 | Licensed Application EULA | https://www.apple.com/legal/internet-services/itunes/dev/stdeula/ | — | Apple standard EULA, linked in the listing; applies by default unless a custom EULA is used. |

### 2.3 Law

| Source | URL | Date fetched | What it says |
|---|---|---|---|
| Iraq — Law No. 3 of 1971 on Copyright (WIPO Lex) | https://www.wipo.int/wipolex/en/legislation/details/10345 | 2026-11-16 | Computer programs (source or machine code) are protected as literary works (Art. 2(2)); the author alone decides use (Art. 7); written permission is required for reproduction (including digital/electronic storage), translation/adaptation/modification, distribution, import and communication to the public (Art. 8); one personal copy (Art. 13); analysis and short quotation with attribution permitted (Art. 14); three-step test (Art. 15(bis)). **No decompilation/interoperability exception located.** Penalty provisions not extracted — counsel to verify. Amendment instrument “Order No. 83” appears on WIPO Lex but was not analysed. |
| Iraq — data protection | DataGuidance country page attempt | 2026-11-16 | Page returned blocked/empty content (14 characters). **No comprehensive Iraqi personal-data statute verified.** The app processes everything on-device in v1, which limits the question; counsel to confirm. |

### 2.4 Google Play / Android policy and platform sources (all fetched 2026-11-16)

| Topic | URL | Snapshot |
|---|---|---|
| Target API level requirement (Play) | https://support.google.com/googleplay/android-developer/answer/11926878 | target-api-support.txt |
| Target API level requirement (Android developers) | https://developer.android.com/google/play/requirements/target-sdk | target-api-dev.txt |
| Health Content and Services | https://support.google.com/googleplay/android-developer/answer/16679511 | health-content.txt |
| Health app categories | https://support.google.com/googleplay/android-developer/answer/13996367 | health-app-categories.txt |
| Health apps declaration form | https://support.google.com/googleplay/android-developer/answer/14738291 | health-declaration-form.txt |
| Android health permissions guidance | https://support.google.com/googleplay/android-developer/answer/12991134 | health-permissions.txt |
| User Data policy | https://support.google.com/googleplay/android-developer/answer/10144311 | user-data.txt |
| Data safety form | https://support.google.com/googleplay/android-developer/answer/10787469 | data-safety.txt |
| Data safety (consumer explanation) | https://support.google.com/googleplay/answer/11416267 | data-safety-consumer.txt |
| Prominent disclosure and consent | https://support.google.com/googleplay/android-developer/answer/11150561 | prominent-disclosure.txt |
| Foreground service requirements (Play) | https://support.google.com/googleplay/android-developer/answer/13392821 | fgs-requirement.txt |
| Foreground service types (Android) | https://developer.android.com/develop/background-work/services/fgs/service-types | fgs-types.txt |
| Content ratings | https://support.google.com/googleplay/android-developer/answer/9898843 | content-ratings.txt |
| Metadata policy | https://support.google.com/googleplay/android-developer/answer/9898842 | metadata-policy.txt |
| Ads policy | https://support.google.com/googleplay/android-developer/answer/9857753 | ads-policy.txt |
| Deceptive behavior | https://support.google.com/googleplay/android-developer/answer/17006354 | deceptive-behavior.txt |
| App content / prepare for review | https://support.google.com/googleplay/android-developer/answer/9859455 | play-console-app-content.txt |
| Notification runtime permission | https://developer.android.com/develop/ui/compose/notifications/notification-permission | notification-permission.txt |
| Activity recognition permission (Android 10) | https://developer.android.com/about/versions/10/privacy/changes | android10-privacy.txt |
| Step counter sensor guide | https://developer.android.com/health-and-fitness/fitness/basic-app/read-step-count-data | android-step-count.txt |
| Android 16 behavior changes | https://developer.android.com/about/versions/16/behavior-changes-16 | android16-behavior.txt |
| 16 KB page sizes | https://developer.android.com/guide/practices/page-sizes | page-sizes.txt |
| New personal developer account testing | https://support.google.com/googleplay/android-developer/answer/14151465 | new-personal-account-testing.txt |
| Get started with Play Console (fee, payment cards) | https://support.google.com/googleplay/android-developer/answer/6112435 | get-started-play-console.txt |
| Required account information | https://support.google.com/googleplay/android-developer/answer/13628312 | developer-account-required-info.txt |
| Supported locations (developer/merchant) | https://support.google.com/googleplay/android-developer/answer/9306917 | supported-locations.txt |
| App promotion (not used by v1) | Play Console Help — App Promotion | app-promotion.txt |
| AGP 8.7 max API level (evidence for build fix) | https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes | agp-8.7-release-notes.txt |
| AGP 8.10 max API level (proposed build fix) | https://developer.android.com/build/releases/past-releases/agp-8-10-0-release-notes | agp-8.10-release-notes.txt |

---

## 3. Acts assessed

Legend: **P** = permissible on the stated basis; **N** = not permissible; **C** = permissible only with conditions.

| # | Act | Legal basis | Ḥalāl basis | Verdict |
|---|-----|-------------|-------------|---------|
| A1 | Reading public store listings and public developer policies; saving snapshots for review | Public facts; ordinary access to publicly posted pages; short quotation for analysis (Iraqi Copyright Law Art. 14; attribution) | Seeking knowledge and industry facts is permitted; no deception, no taking of another's property | **P** |
| A2 | Recording which features exist, prices, ratings, data-safety declarations | Facts and ideas are not ownership; only expression is protected | Facts are not wealth taken from anyone | **P** |
| A3 | Quoting a clause verbatim for analysis (≤ 5 lines, attribution) | Quotation/analysis exception; attribution | Truthful attribution; no misrepresentation | **P** (≤5 lines; internal memo only) |
| A4 | Copying listing descriptions, policy text, UI text or assets into the app, store listing or marketing | Reproduction/adaptation without written permission; contract terms may also prohibit it | Copying expression is taking another's property; deception if passed off as ours | **N** |
| A5 | Pixel-for-pixel copying a target's screen layout, icon set, illustrations, sounds | Copyright in UI graphics/assets; trade dress | Copying expression and tricking users into thinking it is the same product | **N** |
| A6 | Using a target's name, logo or trade dress in the app, listing, keywords or ads | Trade-mark law; Play Metadata/Deceptive Behavior policies | Ghish (deception); unlawful use of another's mark | **N** |
| A7 | Comparing our app to a named competitor (“better than X”, “X alternative”) | Trade-mark use; Play Metadata policy prohibits data comparisons of apps/brands | Deception and disparagement risk | **N** |
| A8 | Downloading an APK/AAB, decompiling, disassembling, extracting strings/resources/assets/databases | Contract (if EULA accepted) and copyright; **no Iraqi decompilation exception located**; Play/website terms | Copying expression; breach of contract/condition; no genuine necessity exists | **N** |
| A9 | Buying or using an “unlocked”/modded APK, or bypassing licence checks | Copyright infringement; computer-misuse-type offences; contract breach | Consuming haram property and assisting wrong | **N** |
| A10 | Scraping Play listings or reviews at scale, or automating collection | Play/website terms prohibit automated scraping; database/compilation rights may apply | Taking others' data without consent; potential personal-data harm | **N** |
| A11 | Manual reading of a handful of public pages (this project) | Ordinary access; not “scraping” | Permitted | **P** |
| A12 | Short, anonymised quotes from public user reviews to understand pain points (2–3 maximum) | Fair quotation; no personal data; no usernames | Truthful; no backbiting (avoid identifying individuals); public statements | **C** (2–3 short quotes, no names/photos) |
| A13 | Using the target feature set as a checklist of *what exists* (step count, GPS route, reminders, history, export) | Ideas/methods/systems are not protected; industry standards | Competition in ideas and standards is ḥalāl | **P** |
| A14 | Deriving technical standards (MET calorie formula, stride-based distance, FGS/health permission patterns) from public documentation | Public standards/documentation; our own implementation | Permitted; no copying | **P** |
| A15 | Writing our own specification, code, strings, icons, layout, marketing copy | Our own expression | Permitted | **P** |
| A16 | Installing a target app on a team device and observing screens/behaviour (black-box) for analysis | Black-box observation of a licensed copy may be lawful, but it is unnecessary here and blurs the clean-room story | Not needed; avoid | **N for this project** (Lead ruling: no install; also no APK) |
| A17 | Training a model on target listings/policies/UI | Outside ToS bans use for software/AI training; copyright in expression | Not needed | **N** |
| A18 | Publishing our original app on Google Play | Play Developer Distribution Agreement and policies apply | Permitted if truthful, no haram monetisation, no deception | **C** (clearance + checklist + counsel/scholar review) |
| A19 | Hosting our own privacy policy and support pages | Our own content; public URL | Permitted | **P** |
| A20 | Using open-source libraries (Compose, Room, WorkManager, etc.) | Their licences (Apache-2.0 etc.) with attribution/NOTICE | Permitted; no ribā/gharar issue | **C** (licence compliance) |

---

## 4. Artefact table

Classes per the skill: **KEEP** (may be kept and used), **SEALED** (kept internal, unpublished, not a build input), **SANITISE** (reduce to lawful facts), **DESTROY** (remove after hashing), **NEVER** (must not be created/used at all).

| Artefact | Class | Reason | Action |
|---|---|---|---|
| Public store-listing facts (feature names, prices, installs, ratings, data-safety rows) | **KEEP** | Facts, not expression | Record in research notes; no full listing text |
| Public privacy policies / terms / law snapshots (HTML or text) | **KEEP** | Public documents, kept for evidence and quotation | Keep under docs/legal/sources/ with URL + fetch date |
| Short attributed clause extracts (≤ 5 lines) in this memo | **SEALED** | Quotation for analysis, not a product input | Internal memo only; never ship or publish; max 5 lines |
| Raw listing HTML in tmp/research/app1–6.html | **SEALED** | Full pages contain third-party expression (marketing copy, image references) | Internal analysis only; not a build input; delete or archive outside the repo after analysis; never copy text from it |
| Any competitor screenshot/image taken for analysis | **SEALED → DESTROY** | Images are expression; unnecessary | None taken. If one is ever taken: internal only, never in repo/marketing, destroy after analysis |
| 2–3 anonymised public review quotes (if used) | **KEEP (limited)** | Short quotation, no personal data | No usernames/photos; max 3 quotes; internal research note |
| Our product brief, feature matrix, gap analysis, specification | **KEEP** | Our own expression derived from facts | Keep in docs/; review that it contains no copied listing prose |
| Our Kotlin/Compose source, strings, icons, colour palette, sounds, animations | **KEEP** | Our own expression | Keep; originality review before release |
| Our privacy policy, store listing copy, in-app disclosures (EN/AR) | **KEEP** | Our own expression, truthful | Keep; accuracy review vs. build/Data safety |
| App name “Sabeel” / “سبيل” (formerly “Khatwa” / “خطوة”), package com.sabeel.steps, and logo drafts | **KEEP (conditional)** | Our own expression, but name/logo/applicationId may conflict with existing marks or an existing Play app | Trademark/name clearance and applicationId collision check before launch; rename if conflict |
| Open-source libraries (AndroidX, Compose, Room, DataStore, WorkManager, coroutines) | **KEEP** | Licensed to us under their OSS licences | Keep licence/NOTICE information; verify no copyleft incompatibility |
| Bundled fonts (if any) | **KEEP (conditional)** | Font licence required | Use OFL/Apache-licensed fonts with licence file; else use system font |
| Any target APK/AAB/ZIP, downloaded or received | **NEVER** | Copyright + contract; Lead ruling | Not present. Must not be created. If found: hash, delete, report (see §7) |
| Decompiled code, pseudo-code, extracted strings/resources/databases/assets | **NEVER** | Expression copies; no decompilation right | Not present. Must not be created. If found: hash, delete, report |
| Full-text reproductions of listings/policies in docs or product | **SANITISE** | Long quotation = copying expression; not needed | Reduce to facts + ≤5-line quotes with attribution |
| Name/trademark research notes containing copied listing text | **SANITISE** | Facts okay, copied prose not | Keep collisions/facts; strip copied text |
| Temporary exports/test data from our app | **DESTROY** | Test data may contain personal patterns; not needed | Delete after testing; never commit |
| Play Console credentials, signing keys, keystore, API tokens | **SEALED** | Secret material | Never commit; store in owner's password manager/secure CI |
| Our signed release artefacts (AAB/APK) | **KEEP** | Our build | Keep for release records; keys sealed |
| Any “inspired-by” asset traced from a target | **DESTROY** | Derivative expression | Never create; if found, destroy and redo from own spec |

**Build-input rule:** the build tree (app/, gradle/, resources) must contain only KEEP items. Before release, grep the repo for target names, target package IDs, domain names and URL fragments; any hit outside docs/legal/sources/ and tmp/research/ is a stop signal.

---

## 5. Appendix A — verbatim clause extracts (five lines maximum each)

> Quoted only for analysis, with attribution. These are the clauses that would have bound us **if** we had installed/used the apps or used their websites outside ordinary reading. They reinforce the clean-room decision; they are not a licence and do not transfer any right.

### 5.1 StepsApp GmbH — Terms of Service v1.2 (last updated 2026-10-28), https://steps.app/terms

> “We grant you a limited, non-exclusive, non-transferable, non-sublicensable, revocable license to use StepsApp for personal, non-commercial use, subject to these terms.”
>
> “Prohibitions: You may not: Reverse engineer, decompile, or attempt to extract source code, except where law permits.”
>
> “Use automated tools to scrape, overload, or abuse our services.”

Significance: confirms no copying/RE licence; “except where law permits” is narrow and Iraqi law provides no decompilation exception. Personal, non-commercial licence only — a commercial rebuild would breach it even if installed lawfully.

### 5.2 Outside Interactive, Inc. — Terms of Use (last updated 2025-10-03), https://www.outsideinc.com/terms-of-use/

> “…provided that you do not (and do not allow any third party to) copy, modify, creative derivative works, reverse engineer, sell, assign, sublease, transfer or otherwise commercially exploit any right in the Outside Services or Content.” [“creative” appears in the published text]

> “Use robots, spiders, scripts, service, software or any manual or automatic device, tool, or process designed to data mine or scrape Content available on the Services…”

> “Noncommercial use does not include the use of the Services without prior written consent from Outside in connection with the development of any software program, including, but not limited to, training a machine learning or artificial intelligence (AI) system.”

Significance: broad anti-copying, anti-RE, anti-scraping and anti-AI-training language. Our manual reading of a few public pages is not scraping; we must not use their content to develop or train anything.

### 5.3 Domaine du Net — Conditions générales d'utilisation, Runify (last updated 23 September 2026), https://runify.appliendirect.fr/fr/terms

> « L'ensemble du contenu du site et de l'application (textes, images, logos, icônes, vidéos, logiciels) est la propriété exclusive de Domaine du Net ou de ses partenaires et est protégé par les lois françaises et internationales relatives à la propriété intellectuelle. Toute reproduction sans autorisation écrite préalable est interdite. »

Significance: all content reserved; reproduction without prior written authorisation prohibited; French law. Any use of Runify content beyond reading would require permission.

### 5.4 Apple — Licensed Application End User License Agreement (standard EULA linked from listing #6), https://www.apple.com/legal/internet-services/itunes/dev/stdeula/

> “You may not copy (except as permitted by this license and the Usage Rules), reverse-engineer, disassemble, attempt to derive the source code of, modify, or create derivative works of the Licensed Application, any updates, or any part thereof…”

Significance: standard Apple EULA applies to the iOS app; not our platform, but confirms the pattern and that no extraction is permitted.

### 5.5 Leap Fitness Group (apps #1 and #3) — no public ToS/EULA located

Public privacy policy exists (last updated 2025-01-10) but contains no copying/reverse-engineering clause and is not a licence. leap.app/terms, /legal, /terms-of-use, /terms-of-service and /eula returned JSON 404 bodies at fetch time. The in-app EULA, if any, was not inspected (we did not install the app). **Status: UNKNOWN — do not rely on any assumption that no EULA exists.** The clean-room plan avoids the question entirely.

### 5.6 Runify iOS (#6) — link rot

Listing links to runifyprivacy.carrd.co and runifytermsandconditions.carrd.co, both **404** on 2026-11-16. The archived privacy policy (Wayback 2026-10-24) was retrieved; terms remain **UNKNOWN**. Apple's default EULA applies. This is a factual observation about link rot, not a basis for treating the app as unlicensed.

### 5.7 A note on Play data-safety declarations

The “No data shared” / “may share” badges are **developer self-declarations**, not audited evidence and not terms of use. We record them as public facts (useful for positioning: our v1 is local-only and collects nothing), never as a licence or as proof of the targets' internal behaviour. Our own declarations must be equally truthful and will be backed by the build (no INTERNET permission, no analytics SDKs — checklist §4).

---

## 6. Conditions and open questions

1. **Not legal advice / not a fatwa** — qualified Iraqi counsel and a qualified scholar must review before commercial publication. This memo can be handed to them as the fact/evidence record.
2. **Trademark and name clearance for the final name is partially done.** Owner screening on 2026-11-16: provisional “Khatwa” / “خطوة” was **rejected** (a same-concept Arabic offline step project plus 12 Play apps), and **“Sabeel” / “سبيل”** was chosen with **no step-tracker collision found on Play or the App Store**; package **com.sabeel.steps**. What remains: registry clearance (Iraqi trade-mark registry, WIPO Global Brand Database, major markets) and confirmation that the applicationId com.sabeel.steps is not already used on Play (Play requires a unique package). *Screening done — registry clearance still owner action.*
3. **Iraqi copyright law currency:** Law No. 3 of 1971 as amended; “Order No. 83” appears on WIPO Lex but was not analysed. Counsel to confirm the operative text, penalty provisions and whether any interoperability/backup exception applies (none found). *Open — counsel.*
4. **No Lebanese/Jordanian/GCC/UAE distribution-specific analysis** was done. If we distribute beyond Play's default worldwide reach, counsel should check local consumer/health-advertising rules; the UAE has no decompilation exception either (skill baseline). *Open.*
5. **Target EULA gaps:** Leap Fitness terms UNKNOWN; Runify iOS terms UNKNOWN. Irrelevant to the clean-room plan but relevant if anyone later installs or uses the apps for analysis: install acceptance binds. Keep the “no install” rule. *Resolved by plan.*
6. **Play policy freshness:** all Play policy pages were read on 2026-11-16. Policies change; re-read the checklist sources within 7 days of submission. *Recurring.*
7. **Target API 36 — RESOLVED 2026-11-16:** Lead fixed the AGP version line; the tree builds green with **AGP 8.10.1 + Gradle 8.11.1 + JDK 21 + compileSdk/targetSdk 36**. Remaining step: record the value from the post-manifest-pass artifact (checklist C2) and keep it in the evidence log. *Resolved — verify artifact only.*
8. **Privacy policy — v1.0 accepted by Lead (2026-11-16):** docs/legal/privacy-policy.md contains English + Arabic text for a zero-collection offline app. Remaining OWNER actions: fill the legal entity name (EN/AR), monitored contact email, optional address and effective date; publish at an active, publicly accessible, non-geofenced, non-PDF, non-editable URL (isolated page only — never publish docs/legal); link it in Play Console and in-app; commission the Arabic native review (launch precondition). Confirm C4/C5/C6 so every sentence stays true. *Accepted v1.0 — owner actions listed.*
9. **Play account and distribution:** personal vs organisation account; US$25 fee; identity verification; Iraq supports developer registration but **not merchant registration** → v1 must be free (no paid app, no IAP). Payment-card acceptance from Iraq is **UNVERIFIED**; a personal account created after 2023-11-13 needs a 12-tester/14-day closed test before production. *Open — owner; checklist §1.*
10. **Health claims:** the app is not a medical device; calorie/distance/active-minute values are estimates. All UI, store and marketing text must avoid diagnosis/treatment/cure/weight-loss promises and absolute accuracy claims; add a plain-language disclaimer. *Open — product/legal; checklist §6.*
11. **No-haram-monetisation:** v1 has no ads, no IAP, no subscriptions, no interest, no gambling, no data sale. If monetisation is added later, re-run this gate (ads content, tracking, ribā, gharar, dark patterns). *Deferred.*
12. **Team statement:** Lead/owner must confirm no team member worked on the targets and that the build is original. *Open — Lead.*
13. **If a breach is discovered** (e.g. someone downloaded an APK or copied a screen): stop, hash the copy (SHA-256 + size + path only), destroy it, sanitise notes to facts/≤5-line quotes, do not ship, and report to the Lead. Tawbah requires stopping, removing, regret and resolving the right where possible. *Procedure.*
14. **Publication gate** (all must be true): this memo accepted; checklist §11 sign-off complete; **post-manifest-pass** artifact verified (targetSdk 36, allowBackup=false, no INTERNET, no ACCESS_NETWORK_STATE); privacy policy live and linked; Data safety/health/FGS declarations submitted truthfully; name/trademark cleared; counsel + scholar sign-off; no target expression in repo; no target names in listing/keywords. *Gate.*

---

## 7. Decision

**The clean-room, black-box, public-information plan is permissible — legally and under the sharīʿah reasoning used here — provided no expression is copied and no target is downloaded, installed, decompiled or scraped.** What the targets sell is lawful to compete with: step counting, GPS routes, history, reminders, exports and streaks are ideas, methods and industry standards, not protected expression; the listings and policies are public facts and short quotable documents. What is not permissible is taking their code, text, images, icons, layout, names, logos or trade dress — and no such taking is needed or planned. Iraqi copyright law protects computer programs as literary works and provides no decompilation exception that could launder extraction, and the targets' own terms (where they can be found) forbid reverse engineering, copying and commercial exploitation; a clean room avoids both the statutory and the contractual problems. The same conclusion holds under the sharīʿah analysis: competition in ideas is ḥalāl; copying expression, deceiving users, or benefiting from another's property without permission is not, and no genuine necessity exists here because we can build the same user benefit from public facts and our own work. **Proceed to build**, with these hard boundaries: (1) build tree contains only our own expression plus facts; (2) no target name, logo, trade dress or comparison in the app, listing or keywords; (3) all store/health/data claims truthful and provable from the build (no INTERNET permission, no SDKs, no ads, local-only data); (4) Play declarations, privacy policy and content rating completed honestly; (5) targetSdk 36 + allowBackup=false build verified; (6) name/trademark clearance, counsel and scholar review completed before the first production submission. **Destroy or seal** the raw listing HTML and any full-text research copies at the end of analysis (keep facts, URLs, dates and hashes); **never create** APK/RE artefacts. This memo is the decision record; it is not a licence, not legal advice and not a fatwa.

### 7.1 Publication gate (must all be ticked)

- [ ] Memo accepted by the human owner.
- [ ] Team clean-room statement confirmed by Lead/owner.
- [ ] docs/legal/play-compliance-checklist.md §11 signed off.
- [ ] Build verified on the **post-manifest-pass** artifact: targetSdk 36, allowBackup=false, no INTERNET, no ACCESS_NETWORK_STATE, no transmitting SDKs, FGS types, user-facing permissions as documented.
- [ ] Privacy policy (draft: docs/legal/privacy-policy.md) finalised, live at a public, non-PDF, non-geofenced URL and linked in-app + Play Console.
- [ ] Data safety, Health apps, FGS declarations and content rating submitted truthfully.
- [ ] Final name (“Sabeel” / “سبيل”, formerly Khatwa; screening done, no step-tracker collision found) trademark/name clearance and applicationId collision check done.
- [ ] FGS declaration demo video and screenshots recorded on the approved API 36 emulator (Play submission prerequisite; runtime rows labelled NOT VERIFIED — emulator-attempted, checklist §10.1).
- [ ] Arabic native review of the privacy policy completed (launch precondition).
- [ ] OWNER: closed testing with 12 real testers, 14 continuous days, if personal account (emulator cannot substitute).
- [ ] Qualified counsel review completed (Iraq + distribution markets).
- [ ] Qualified scholar review completed (claims, monetisation, privacy as amanah).
- [ ] No target expression, names or logos in repo or listing (grep evidence).

### 7.2 Review log

| Date | Reviewer | Scope | Outcome |
|---|---|---|---|
| 2026-11-16 | legal (agent) | First pass, clean-room plan, policy and law baseline | Draft — proceed subject to gate above |
| 2026-11-16 23:xx | legal (agent) | Post-build update: project renamed Sabeel / com.sabeel.steps; build was broken at that moment, so all pre-change artifacts are non-evidence (checklist §10.0); decision unchanged | Draft — superseded by the next row |
| 2026-11-16 23:xx | legal (agent) | Lead corrections applied: build green (AGP 8.10.1 / SDK 36 / JDK 21 / Gradle 8.11.1); wrapper to be added; no agent creates keystore (owner action, C12 expected NOT VERIFIED); name screening: Khatwa rejected, Sabeel chosen with no step-tracker collision; privacy policy drafted EN+AR | Draft — decision unchanged; owner actions recorded |
| 2026-11-16 23:xx | legal (agent) | Lead decisions: AVD approved (API 36 only; runtime rows NOT VERIFIED — emulator-attempted; closed testing OWNER); privacy policy v1.0 accepted; C10-S is the Data safety basis | Draft — decision unchanged; sign-off summary pending buildsmith batch |
| 2026-11-17 00:45 | legal (agent) | Independent buildsmith batch: static PASS=33/FAIL=0/FINDING=1 (no release artifact) on APK sha256 324690…9B9E; AVD boot/install/cold launch/onboarding/reboot observed but all runtime rows NOT VERIFIED — EMULATOR-ATTEMPTED; screenshots black, FGS demo video 0 bytes, sensors absent, contamination documented; C12 NOT VERIFIED BY DESIGN | Evidence logged (release-evidence-log.md); one-page sign-off staged (release-signoff.md); release-artifact gate OPEN; decision unchanged |
| — | human owner | Accept/decide | Pending |
| — | Iraqi counsel | IP, contract, consumer/privacy, distribution | Pending |
| — | Scholar | Ḥalāl analysis, claims, monetisation | Pending |
