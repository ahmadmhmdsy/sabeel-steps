# Play compliance checklist — Sabeel (formerly Khatwa) Android v1

**Status:** DRAFT — living checklist for engineering, product and release. **Project rename (2026-11-16):** the app is now **Sabeel** (Android label Sabeel), package **com.sabeel.steps**; references below to com.khatwa.steps are historical and must not be used in build commands or Play forms. **Build state (Lead-confirmed, 2026-11-16):** the tree now builds green with AGP 8.10.1 / compileSdk+targetSdk 36 / JDK 21 / Gradle 8.11.1; the platform engineer's manifest pass (allowBackup=false, ACCESS_NETWORK_STATE removal, FGS types, boot/workers) is in progress and the Gradle wrapper will be added after the current implementation wave. Final evidence must come from the post-manifest-pass artifact (see §10.0).
**Date:** 2026-11-16 · **Owner:** legal (Agent Team, task-2) · **Engineering counterpart:** buildsmith / UI engineer · **Final sign-off:** human owner.
**Companion document:** docs/legal/clearance-memo.md (clean-room/legal decision).
**Sources:** all URLs in §12 were read on **2026-11-16**; snapshots in docs/legal/sources/.

Legend: **[ ]** not started · **[~]** in progress · **[x]** done with evidence · **[!]** blocker / stop-ship. Every "done" needs evidence: a command output in §10, a Play Console screenshot, or a file path.

> Not legal advice. This checklist maps our build to Google Play's published requirements as of the fetch date; re-read the sources within 7 days of submission because Play policy and target-API rules change.

---

## 1. Distribution prerequisites (answers to the Lead's three questions)

### 1.1 Account, fee, identity verification, Iraq (Lead question 2)

| Item | What the docs say (2026-11-16) | Consequence for us | Status |
|---|---|---|---|
| Developer registration fee | **US$25 one-time**, paid with Mastercard, Visa, American Express, Discover (US locations only) or Visa Electron (outside the US). **Prepaid cards are not accepted.** Card acceptance varies by location. (Play Console Help: Get started with Play Console) | Budget US$25; use an accepted non-prepaid card. **A card issued in Iraq is not guaranteed to work — UNVERIFIED.** Fallback: ask Google Play developer support; do not use a virtual/prepaid card. | [ ] |
| Account type | Personal or organisation. Personal: developer name, legal name, legal address, contact email, contact phone, developer email. Organisation: linked Google Payments profile with **D-U-N-S number**, organisation name/address, website, contact details. | Decide before registering: personal (simpler, but closed-testing rule applies) vs organisation (D-U-N-S required). A one-person studio can use a personal account. | [ ] |
| Identity verification | Google verifies the account information during signup (legal name/address/contact details). Exact ID-document requirements are not in the fetched pages — **UNVERIFIED**; keep passport/ID and address proof ready. | Do not publish under a pseudonym that does not match the payments profile; the developer name appears on the listing and must match the privacy policy entity. | [ ] |
| Registering/publishing from Iraq | Supported-locations table: **Iraq = developer registration supported (✔), merchant registration NOT supported (✘), default currency USD.** | We can register as a developer and publish a **free** app from Iraq. We **cannot** sell the app, use IAP or receive merchant payments from Iraq. v1 is free, so this aligns. | [ ] |
| Paid app / in-app purchases | Merchant registration unsupported for Iraq; no merchant account | v1: **free, no IAP, no subscriptions, no ads**. If monetisation is ever added, revisit merchant eligibility (another country/entity) and re-run this checklist. | [ ] |
| AAB upload | New apps on Google Play are distributed as Android App Bundles and use Play App Signing; confirm the current console wording at upload. (Fetched policy pages do not re-state this; mark as console-verified.) | Build a release AAB, enrol in Play App Signing, keep the upload keystore in the owner's password manager (SEALED artefact; never in repo). | [ ] |

### 1.2 Closed testing for new personal accounts (Lead question 1)

| Item | What the docs say | Consequence | Status |
|---|---|---|---|
| 12 testers / 14 days | "Developers with personal accounts created after November 13, 2023, must run a closed test for their app with a minimum of 12 testers who have been opted in continuously for at least 14 days. When you meet these criteria, you can apply for production access…" (Play Console Help: App testing requirements for new personal developer accounts, fetched 2026-11-16 — **still current**). | If we register a **new personal account**, production access is gated: recruit **≥12 real testers**, keep them opted in **continuously for 14 days**, then apply for production. Plan for **+2–3 weeks** before public launch. Testers must actually install/use the closed-test build; the clock starts when they are opted in. | [ ] |
| Applies to organisation accounts? | The article names personal accounts created after 2023-11-13; organisation accounts are not covered by that sentence. | Confirm account type first. If personal: budget the test. If organisation: verify in console whether any testing gate applies before relying on exemption. | [ ] |
| Closed-testing build must still be compliant | Closed testing is still distribution: Data safety (closed testing is **not** exempt; only internal testing is), health declaration, content rating, privacy policy, target API all apply. | Do not treat closed test as a "policy-free" track. Complete §3–§6 before the first closed-test upload. | [ ] |

### 1.3 Privacy policy even with no data collection (Lead question 3 — summary)

| Question | Answer | Reference |
|---|---|---|
| Is a privacy policy URL required if the app collects nothing? | **Yes.** "Apps that do not access any personal and sensitive user data must still submit a privacy policy." The Data safety form must also be completed even when no data is collected, with a privacy-policy link. | User Data policy; Data safety form |
| What must the hosted policy contain for a local-only health/fitness app? | Developer identity + privacy contact; what data is accessed (steps, activity, location, body metrics if collected, reminders), purpose, **on-device only**, no sharing/selling, no third-party SDKs, retention (until deleted), deletion (in-app delete-all + uninstall), cloud backup disabled, user-initiated CSV export, permissions explained, children, changes/effective date, health disclaimer, EN + AR. Full checklist in §5. | User Data; Health Content and Services; Data safety |
| URL requirements | Active, publicly accessible, **non-geofenced**, **no PDFs**, non-editable; link in Play Console **and** in-app link/text. | User Data policy |
| What must the Data safety form say about allowBackup=false, no INTERNET, on-device data? | The form has **no field** for allowBackup or INTERNET. Answer "does your app collect or share required user data types?" = **No**; on-device-only processing is **not collection**; no sharing; no SDKs. The policy must explain that the app has no INTERNET permission, cannot transmit data, disables Android cloud backup, and only exports when the user chooses. | Data safety; Data safety consumer page |

---

## 2. Blocking corrections to the product brief / current build

These are not "nice to have". The target API gate is now resolved (B1/B2); the privacy policy is the remaining hard Play gate; release evidence is gated by the manifest pass and by owner-only signing actions (B11); runtime tests need a device/emulator (§10.1).

| # | Item | Current state (2026-11-16) | Required | Why | Owner | Status |
|---|---|---|---|---|---|---|
| B1 | Target API level | **Resolved 2026-11-16:** green build with compileSdk 36 + targetSdk 36, AGP 8.10.1, JDK 21, Gradle 8.11.1; value still to be recorded from the post-pass artifact by C2. | **compileSdk 36, targetSdk 36** | "Starting August 31, 2026: New apps and app updates must target Android 16 (API level 36) or higher…"; the extension deadline (Nov 1, 2026) has passed. A target-35 build **cannot be submitted**. | buildsmith → UI engineer | [!] |
| B2 | AGP / Gradle for SDK 36 | **Resolved 2026-11-16:** AGP 8.10.1 with Gradle 8.11.1 builds successfully (escaped-quote line fixed by Lead). | **AGP 8.10.x (8.10.1 confirmed available) + Gradle 8.11.1** | AGP 8.7 max supported API = 35; AGP 8.10 max API = 36 and minimum Gradle = 8.11.1 (already installed). Do not jump to AGP 8.11 (needs Gradle 8.13). | buildsmith → UI engineer | [!] |
| B3 | Backup flag | Manifest pass in progress by platform engineer (allowBackup=false expected); must be verified on the post-pass artifact by C5/C6. | **android:allowBackup="false"** (and no cloud-backup rule files needed) | Brief's privacy promise; supports "no cloud copy of health data" in policy/Data safety; Data safety "no collection" story. | UI engineer | [!] |
| B4 | 16 KB page sizes | Debug APK verified: libandroidx.graphics.path.so and libdatastore_shared_counter.so have PT_LOAD p_align=16384 on all 4 ABIs; zipalign -c -P 16 passes. | Keep verifying on **release APK/AAB**; no dependency bump needed. | Apps targeting API 35+ must support 16 KB on 64-bit devices; Play blocks non-compliant updates from 2027-02-01. | buildsmith/UI | [~] |
| B5 | Edge-to-edge / large screens | Manifest has no windowOptOutEdgeToEdgeEnforcement, no screenOrientation/resizeable attrs. | Target 36: edge-to-edge is enforced (no opt-out); orientation/resizability/aspect-ratio restrictions are ignored on displays with smallest width ≥600dp. | Android 16 behavior changes. | UI engineer | [ ] |
| B6 | Permission surface claim | Stale pre-change debug manifest: WAKE_LOCK, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION; **no INTERNET**. | Keep no INTERNET. ACCESS_NETWORK_STATE removal was requested and forwarded to Lead for the manifest pass (see §6.4); expected final state: absent. Never claim "no permissions" — claim "no INTERNET permission; app cannot send data off device". | Merged manifest + WorkManager transitives; truthful marketing. | UI engineer | [~] |
| B7 | Foreground service types | Not yet declared (features not built) | health + location types with correct FGS permissions, notification, Play Console FGS declaration. | FGS types + Play FGS requirement (Android 14+). | UI engineer | [ ] |
| B8 | App name / trademark | **Sabeel** (renamed from Khatwa on 2026-11-16); no clearance done | Trademark/name search for Sabeel/سبيل before listing; confirm applicationId com.sabeel.steps is free on Play; rename if conflict. | Metadata/trade-mark/package-collision risk. | owner/legal | [ ] |
| B9 | Build was broken at 23:15 on 2026-11-16 | Escaped quotes in root build.gradle.kts AGP version (version \"8.10.1\") caused a Gradle script-compilation error. **Fixed by Lead; tree builds green.** | Fix landed (plain quotes); no further action. | Lead | [x] |
| B10 | Gradle wrapper | **Decision:** Lead will add the wrapper (8.11.1) after the current implementation wave and will confirm; until then, C1 uses absolute C:\gradle\gradle-8.11.1\bin\gradle.bat with JAVA_HOME/ANDROID_HOME set. | Keep the absolute-gradle fallback documented; switch commands to .\gradlew.bat once the wrapper is confirmed, and record the wrapper version in the evidence identity. | Lead | [~] |
| B11 | Release signing / keystore | **Lead decision: no agent creates a production keystore.** Release stays unsigned for evidence; keystore generation and Play App Signing enrolment are OWNER actions. C12 therefore remains NOT VERIFIED — expected. | Owner creates upload keystore outside the repo, configures signingConfig, enrols Play App Signing, stores the keystore SEALED; then C12 is re-run by buildsmith. No agent generates, copies or reads the keystore. | Owner | [OWNER] |

**Definition of done for B1–B3:** merged release manifest shows targetSdkVersion 36, allowBackup="false" and no ACCESS_NETWORK_STATE; aapt2 badging shows targetSdkVersion:'36'; Play Console accepts the AAB.

---

## 3. App content declarations (Play Console → App content)

| Declaration | Required answer / action | Why | Status |
|---|---|---|---|
| Privacy policy | Public URL (active, non-geofenced, no PDF, non-editable) + in-app link/text; content per §5. Answer every field; entity name must match developer name. | User Data policy: all apps, even with no data access. | [ ] |
| Ads | **No ads.** No ad SDK, no banner/interstitial/native/rewarded, no ad wrapper. Do not claim "Contains ads". | Ads policy; App content ads declaration. If ads are ever added: update declaration, Data safety, privacy policy, possibly INTERNET, and re-run this checklist. | [ ] |
| App access | **All functionality available without special access** (no login, no account, no invite). | Reviewers must be able to test everything. | [ ] |
| Target audience | **13+** (not directed to children; no Families programme). Answer "is the app designed for children?" honestly = No. | Families policy only applies if target audience includes children; health data + ads rules differ. | [ ] |
| Content rating (IARC) | Complete the questionnaire honestly: health/fitness content, no violence/sex/drugs/gambling, no user-generated content, no ads. Expect a low rating (e.g. Everyone/PEGI 3) — do not pre-claim; use the issued rating. Re-do if content changes. | "All apps must have a content rating from the IARC." | [ ] |
| Health apps declaration | **All developers with a published app must complete it**, including closed/open/production. Ours: **Activity and Fitness** (activity tracker). Not a medical device, no diagnosis/treatment, no Health Connect, no research, no nutrition/weight-management feature unless v1 includes weight goals (it does not). | Health Content and Services; Health apps declaration form. | [ ] |
| Foreground service declaration | Declare each type used (health; location). For each: description of functionality; user impact if deferred; user impact if interrupted; link to a **demo video** showing the user steps that trigger it. The demo video requires a device/emulator + screen recording (§10.1). | Play FGS requirement (Android 14+). Missing declaration = review/rejection risk. | [ ] |
| Permissions declaration | Not required for our permissions (the restricted-permissions declaration covers SMS/Call Log/All files etc.). Health permissions still need prominent disclosure (§6.3). | Play permissions policy. | [ ] |
| Other declarations | News: No. COVID-19: No. Financial features: No. Government: No. VPN: No. AI-generated content: No (no generative AI in-app). Data deletion: no account, so account-deletion requirement N/A; app-level delete-all provided. | App content forms. | [ ] |

---

## 4. Data safety form walk-through

### 4.1 Answers (v1, local-only build)

| Form question | Answer | Basis |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | No data is transmitted off-device. "Data is generally considered 'collected' when the developer uses their app to retrieve data off your device." On-device-only processing is explicitly not in scope for collection. |
| Data types accessed on-device (for the policy, not the form) | Health and fitness (steps, active minutes, calorie estimate, walk history); Location (route, only while a walk is tracked); preferences/reminders | Processed locally in Room/DataStore; never transmitted. |
| Data shared with third parties? | **No** | No SDKs, no servers, no analytics/ads/crash reporting; CSV export is user-initiated to a destination the user picks (SAF) — "user-initiated action" exception. |
| Is all data encrypted in transit? | **Not applicable** — no data leaves the device | If the form allows "no data collected", this question is skipped. |
| Data deletion | In-app "Delete all data" (Room + DataStore + files); uninstall removes everything. No account → account-deletion request flow not applicable. | User Data policy account-deletion requirement applies only where accounts exist. |
| Security practices | No transmission; OS app sandbox; backups disabled (allowBackup=false); no third-party SDK data collection. | Data safety security section; verify with dependency audit (§10 C8). |
| Families / children | Not applicable (13+). | Target audience. |

### 4.2 Evidence that backs the "No" answer

- [ ] No INTERNET permission in merged manifest / APK (C4, C5).
- [ ] No networking/analytics/ads/crash SDKs in dependency tree (C8).
- [ ] No data sent: airplane-mode test shows full functionality (C11).
- [ ] allowBackup=false in merged manifest (C6); no cloud backup of app data.
- [ ] CSV export only through the system file picker (SAF), no storage permission, no upload.
- [ ] Privacy policy states all of the above and matches the form (§5).

### 4.3 "No collection" is a claim with consequences

If any future SDK transmits data (ads, analytics, crash reporting, remote config, push, account sync), the form answer, privacy policy, ads declaration and possibly permissions must change **before** shipping that build. Re-run this checklist on every dependency addition; treat "just add Firebase" as a stop signal.

---

## 5. Privacy policy: hosting + required contents

### 5.1 Hosting requirements

- [ ] **Public HTTPS URL**, no login, no geofence, no PDF, not an editable document (no Google Docs link), not a redirect to a store listing.
- [ ] Reachable from a normal phone browser **in Iraq** and from at least one other country (test with mobile data and with a VPN).
- [ ] Linked in **Play Console** (designated field) **and** in-app (Settings → Privacy, plus a first-run link).
- [ ] Entity/developer name in the policy **matches** the Play developer name and listing; privacy contact email is monitored.
- [ ] EN and AR versions (or one bilingual page); Arabic page must be real Arabic text, not machine-translated claims.
- [ ] Version/effective date shown; changes are dated.
- [ ] URL is stable for the app's lifetime (do not point at a temporary preview URL).

### 5.2 Required contents (User Data policy + health policy)

- [ ] Developer identity, address/contact, privacy point of contact.
- [ ] What the app accesses: step counter sensor (ACTIVITY_RECOGNITION), location while tracking, notifications, local storage; that body/health data is **on-device only**.
- [ ] Purpose of each access, tied to the visible feature (steps, walk route, reminders, history, export).
- [ ] **No collection, no transmission, no sale, no sharing** with third parties; no analytics/ads/crash SDKs; no account.
- [ ] Retention: until the user deletes it; deletion via in-app "Delete all data" and uninstall.
- [ ] **Cloud backup disabled** (allowBackup=false): app data is not backed up to Google or to the developer.
- [ ] **CSV export** is user-initiated to a location the user chooses; the developer never receives it; the exported file is outside the app's protection.
- [ ] Permission explanations (why ACTIVITY_RECOGNITION, location, notifications) and how to revoke them in Android Settings.
- [ ] Location specifics: foreground-only, only during an active walk, no background location, route stored locally.
- [ ] Health disclaimer: not a medical device; estimates only; consult a professional.
- [ ] Children: not directed to children under 13.
- [ ] Changes/effective date; contact for privacy questions.
- [ ] Consistency with the Data safety form and every in-app disclosure (no contradictions).
- [ ] **Draft text ready:** docs/legal/privacy-policy.md (EN + AR) with a pre-publication checklist; fill the placeholders, verify C4/C5/C6, publish, then link it in Play Console and in-app.
- [ ] **Hosted page ready:** docs/privacy-site/index.html is generated from the Markdown source (v1.0), self-contained (no scripts/fonts/images/trackers), English then Arabic with dir="rtl"; publish only that folder via GitHub Pages or copy it to a separate repo — never publish docs/legal or the repo docs tree.

---

## 6. Permissions, disclosures and foreground services

### 6.1 Permission set (target state)

| Permission | Type | Why | User-facing requirement | Status |
|---|---|---|---|---|
| android.permission.ACTIVITY_RECOGNITION | Runtime (API 29+) | Read TYPE_STEP_COUNTER / step detector. On API 26–28 no runtime permission is needed for the built-in step counter, but declaring it is harmless and future-proof. | **Prominent disclosure immediately before the system prompt**, affirmative consent, explain data stays on device. Health policy in scope. | [ ] |
| android.permission.ACCESS_FINE_LOCATION | Runtime | Draw the walk route on our own Canvas while the user tracks a walk. Optional feature. | Prominent disclosure before the prompt, at the point the user starts a walk; no ACCESS_BACKGROUND_LOCATION; if denied, app still works (steps only). | [ ] |
| android.permission.POST_NOTIFICATIONS | Runtime (API 33+) | Reminders and the foreground-service notification. | Contextual request when the user enables reminders; denial must not crash; explain FGS notification; deep-link to settings. | [ ] |
| android.permission.FOREGROUND_SERVICE | Normal | Required for any FGS. | — | [ ] |
| android.permission.FOREGROUND_SERVICE_HEALTH | Normal | Health-type FGS (step tracking service). | Play Console FGS declaration; demo video. | [ ] |
| android.permission.FOREGROUND_SERVICE_LOCATION | Normal | Location-type FGS during a walk. | Play Console FGS declaration; demo video; location services on. | [ ] |
| WAKE_LOCK, RECEIVE_BOOT_COMPLETED | Normal (from WorkManager) | WorkManager scheduling/rescheduling. | Not runtime; document in permission review. | [ ] |
| ACCESS_NETWORK_STATE | Normal (from WorkManager) | Only used by WorkManager network constraints; v1 uses no network constraints. Removal recommended and forwarded to Lead for the manifest pass (see §6.4); expected final state: removed via tools:node="remove". | Keep WAKE_LOCK/RECEIVE_BOOT_COMPLETED/FOREGROUND_SERVICE; verify reminders/boot after removal (C10). | [~] |
| android.permission.INTERNET | — | **Must NOT be declared.** | Store copy may say "no INTERNET permission; the app cannot send your data anywhere". | [ ] |
| BODY_SENSORS / ACCESS_BACKGROUND_LOCATION / READ_MEDIA_* / All files / exact alarms | — | Not needed. Do not add. | — | [ ] |

### 6.2 Foreground service rules

- [ ] Each FGS declares android:foregroundServiceType (health, location) and the matching FOREGROUND_SERVICE_* permission.
- [ ] Health type: at least one of the runtime prerequisites granted (ACTIVITY_RECOGNITION is listed as acceptable).
- [ ] Location type: location services on and ACCESS_COARSE/FINE granted; do not start from background without an exemption.
- [ ] startForeground() called within the Android 14 time limit after service start; notification posted; channel created.
- [ ] Do not start the location FGS from BOOT_COMPLETED or from the background; start only from a user action (walk start) or a valid exemption.
- [ ] If step tracking at boot is attempted via a health FGS from BOOT_COMPLETED: **test on Android 15/16**; background-start restrictions and while-in-use rules may block it. If blocked, fall back to resuming counting when the user opens the app, or use a non-FGS approach; do not ship a crashing boot path.
- [ ] POST_NOTIFICATIONS denied: service must still function; do not assume the notification is visible; surface rationale and settings link.
- [ ] FGS declaration in Play Console includes the demo video link; video shows the real user flow.

### 6.3 Prominent disclosure UX (health permissions)

- [ ] Disclosure is **in-app, in normal usage**, not buried in a menu or a policy link.
- [ ] States **what** is accessed (step sensor; location), **why** (count steps, draw route), **how it is used** (locally to show progress/history), and that **no data is shared**.
- [ ] Appears immediately before the runtime permission request; the user takes an affirmative action (button) — no pre-checked consent.
- [ ] Separate disclosure for location before the first walk; separate for notifications when reminders are enabled; separate for activity recognition at onboarding.
- [ ] Arabic and English; privacy-policy link available from the disclosure screen.
- [ ] If the user declines, show a non-blocking explanation and continue with degraded features; do not re-prompt aggressively ("user fatigue" guidance).

### 6.4 Decision requested: ACCESS_NETWORK_STATE

**Recommendation: remove it with tools:node="remove" in the manifest, because v1 uses no network constraints and an offline app has no legitimate need to query connectivity.** Keep WAKE_LOCK, RECEIVE_BOOT_COMPLETED and FOREGROUND_SERVICE (they are functional for WorkManager/FGS). Note:
- ACCESS_NETWORK_STATE is a normal permission: it **cannot transmit data**; its presence does not falsify "no INTERNET permission".
- Removing it strengthens the "offline by construction" story and avoids reviewer questions; it has no user-facing effect.
- Verify reminders/WorkManager still schedule and fire after removal (C10). If a future feature needs network constraints, re-add it and update this checklist (but v1 is offline).
- DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION is an AndroidX internal signature permission — not a user permission; mention it only if someone audits the manifest.

---

## 7. Health content, claims and copy

| Rule | What we must do | Where it appears | Status |
|---|---|---|---|
| No harmful/misleading health content | The app must not diagnose, treat, cure or prevent disease; no medical advice; no eating-disorder encouragement or body-shaming. | UI, listing, screenshots, notifications | [ ] |
| Not a medical device | State plainly "not a medical device" in the app's about/privacy screen and in the store description if health claims are made. | About, policy, listing | [ ] |
| Estimates labelled | Calorie burn, distance and active minutes are **estimates**; label them and explain the method briefly (MET-based calorie estimate; stride/height-based distance). No "accurate", "precise", "guaranteed" claims. | Dashboard, detail screens, listing | [ ] |
| No weight-loss promises | Do not promise weight loss or a number of calories burned; do not present calorie targets as medical advice. | UI/listing | [ ] |
| No diagnosis/treatment claims | Do not claim to detect, prevent or manage any condition. | UI/listing | [ ] |
| Health apps declaration matches | Activity and Fitness only; do not select medical/nutrition/weight-management categories unless the feature exists. | Play Console | [ ] |
| Privacy policy details health data handling | On-device only, no sharing, retention/deletion, permission explanations. | Policy | [ ] |
| Content rating | Answer the IARC questionnaire about health content truthfully; the rating may vary by country. | Play Console | [ ] |
| Claims provable | Every claim in listing/UI must be true of the build (offline, no ads, no account, local-only). | Listing/UI + evidence | [ ] |

Suggested plain-language disclaimer (to be reviewed by counsel/scholar, not final): "Sabeel is a fitness and activity tracker, not a medical device. Step counts, distance, active minutes and calorie estimates are approximations and are not medical advice. If you have a health condition or are starting a new exercise programme, consult a qualified healthcare professional."

---

## 8. Store listing, metadata, name and Arabic

| Item | Rule | Action | Status |
|---|---|---|---|
| App title | ≤30 characters; no emojis/emoticons/repeated special characters; avoid ALL CAPS unless brand; no "best/#1"/ranking/price/promo text. | Example only: "Sabeel — Steps" (count characters; no keyword stuffing) | [ ] |
| Developer name | Must match the privacy policy entity; no misleading symbols/claims. | Use the verified developer name. | [ ] |
| Short/full description | Accurate, matches functionality; no unattributed testimonials; no data comparison of apps/brands; no competitor names or trademarks; no "better than X"; no fake awards. | Write original copy; legal review; Arabic translation by a human. | [ ] |
| "No ads" claim | True only if no ad SDK/ads. v1: true. | Keep proof (dependency audit). | [ ] |
| "Offline/no data collection" claims | True only with no INTERNET permission and no transmitting SDKs; phrase precisely: "no INTERNET permission — your data stays on your device", not "zero permissions". | Align with Data safety/policy. | [ ] |
| Screenshots | Show the real app; no competitor UI, no fake features, no Google Play badges or device frames implying endorsement; Arabic RTL screenshots; no misleading health claims (e.g. "burn 500 kcal"). | Capture from the release build; legal/UI review. | [ ] |
| Icon / feature graphic | Original; no copied icon style/trade dress; no misleading symbols; no ranking/price text. | Keep source files and authorship records. | [ ] |
| Category / tags | Health & Fitness; tags must not use competitor trademarks. | No competitor names in ASO keywords. | [ ] |
| Localisation | EN + AR listings; Arabic text accurate and RTL-correct; listing text matches the app language. | Human review. | [ ] |
| App name clearance | "Sabeel"/"سبيل" (formerly Khatwa) provisional; search Play + Iraqi/WIPO trademark registries; check applicationId com.sabeel.steps is free. | Owner/legal before listing. | [ ] |

---

## 9. Technical and security checks

| # | Check | Requirement | Evidence | Status |
|---|---|---|---|---|
| T1 | targetSdk/compileSdk | 36 both | C1, C2 | [!] |
| T2 | AGP/Gradle | AGP 8.10.x, Gradle 8.11.1 | C1 (build passes) | [!] |
| T3 | allowBackup | false in merged manifest | C6 | [!] |
| T4 | No INTERNET | Not in merged manifest or APK/AAB | C4, C5 | [ ] |
| T5 | Permission surface | Exactly the set in §6.1; no background location, no body sensors, no exact alarms, no storage permissions | C4 | [ ] |
| T6 | SDK audit | No ads/analytics/crash/remote-config/push SDKs; dependencies are AndroidX/Compose/Room/DataStore/WorkManager/coroutines only | C8 | [ ] |
| T7 | FGS types | health + location declared; permissions present; notification; start rules | C5, C7, manual test | [ ] |
| T8 | Runtime permissions | ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS on API 29/33+; denial paths; settings links | C11 (device tests) | [ ] |
| T9 | 16 KB page sizes | Release APK/AAB aligned; any .so PT_LOAD p_align=16384 | C9 | [~] |
| T10 | Release build hygiene | release not debuggable; minify/ProGuard decision documented; Play App Signing; **release signingConfig configured** (currently missing, B11); signed AAB | C1, C12 | [!] |
| T11 | Edge-to-edge/large screens | Renders edge-to-edge on API 36; usable at ≥600dp widths; no fixed orientation assumption | Manual/emulator | [ ] |
| T12 | Lint | lintDebug/lintRelease clean or triaged | C3 | [ ] |
| T13 | No secrets | No keystore/API keys/emails in repo; .gitignore covers signing files | manual/git grep | [ ] |
| T14 | RTL/Arabic | All screens RTL; strings complete; no clipped text | manual/emulator ar | [ ] |
| T15 | Accessibility | content descriptions, 48dp touch targets, contrast (not a Play policy, but quality/claims) | manual/lint | [ ] |

---

## 10. Verification commands (copy-paste)

Paths assume working directory E:\android_apps\steps_app and SDK at C:\Android\Sdk. Run each and paste the key output into the release evidence log.

### 10.0 Evidence status — read before running anything

**Current state (after Lead's correction, 2026-11-16): the tree builds green.** assembleDebug SUCCESSFUL with AGP 8.10.1, compileSdk/targetSdk 36, JDK 21, Gradle 8.11.1; debug APK at app/build/outputs/apk/debug/app-debug.apk. This artifact is **post-bump but pre-manifest-pass**, so final evidence must come from the artifact built after the platform engineer's pass.

| Fact | Current state | Consequence |
|---|---|---|
| Build | **Green:** assembleDebug SUCCESSFUL with AGP 8.10.1 / compileSdk+targetSdk 36 / JDK 21 / Gradle 8.11.1 (B9 fixed) | C1 can run; because this artifact predates the manifest pass, C4/C5/C6 must be re-run after it |
| Artifacts on disk | Debug APK built from the current tree; release AAB will be **unsigned** (B11) | Debug APK is not a substitute for the release manifest checks; release evidence requires the post-pass artifact |
| Gradle invocation | Wrapper (8.11.1) to be added by Lead after the current implementation wave; absolute C:\gradle\gradle-8.11.1\bin\gradle.bat is the fallback | C1 prefers .\gradlew.bat when present, else $gradle |
| Release signing | No signingConfig; **no agent creates a production keystore** (Lead decision) | C12 remains NOT VERIFIED until the OWNER generates the keystore, configures signing and enrols Play App Signing — expected, not a defect |
| 16 KB check | Pre-change debug APK: both .so files p_align=16384, zipalign -P 16 passed | Repeat on the post-manifest-pass release APK/AAB |

**Rule:** valid evidence = output from the artifact built **after the manifest pass**, on the current build files, naming **com.sabeel.steps**, targetSdk 36, allowBackup=false, no INTERNET, no ACCESS_NETWORK_STATE. Pre-change files under C:\dl\legal-evidence are STALE and must not be pasted. buildsmith runs C2/C4/C5/C6/C8/C9/C10-static on the post-pass artifact; C12 only after the owner signs.

**Independent evidence batch (buildsmith, 2026-11-17 00:00:50–00:34 +03:00, C:/dl/legal-evidence/final-20261117-000050; durable copies docs/legal/evidence/):** measured on the same debug APK (SHA-256 324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E, 11,488,512 B, mtime 2026-11-16T23:51:37): **PASS=33 FAIL=0 FINDING=1 OPEN=1, stale files none**. C2/C4/C5/C6-debug/C8/C9/C10-S PASS; C6 release merged manifest **ABSENT** (release-artifact gate OPEN — build an unsigned release AAB and rerun C2/C4/C5/C6/C9 before submission); C12 **NOT VERIFIED BY DESIGN**, keystore material none present. AVD API 36: boot 132 s, install, cold launch 2889 ms, 4 grants, reboot 52 s; onboarding disclosure seen via uiautomator; FATAL scan 0 — but every runtime row stays **NOT VERIFIED — EMULATOR-ATTEMPTED** (screenshots black frames, FGS demo video 0 bytes, no step sensor/GNSS, deeper flows unreachable; foreign-app contamination documented). Full detail: `docs/legal/release-evidence-log.md`; one-page status: `docs/legal/release-signoff.md`. Unit tests 41/41 remain a platform-engineer self-report, not part of this batch.

**Expected post-bump values (verify each):** package com.sabeel.steps; targetSdkVersion 36; allowBackup=false; no android.permission.INTERNET; no ACCESS_NETWORK_STATE (after the manifest pass); FGS types health and location; no debuggable=true in release; 16 KB alignment OK.

**Environment for all commands:**

    $env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
    $env:ANDROID_HOME='C:\Android\Sdk'
    $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
    $gradle='C:\gradle\gradle-8.11.1\bin\gradle.bat'

Use .\gradlew.bat when the wrapper exists (Lead decision: added after the implementation wave); otherwise use & $gradle ... in C1/C3/C8. Keep the same arguments either way.

### 10.1 Device/emulator dependency (runtime evidence)

**C7 (FGS/notification sanity), C10-R (reminders fire, boot reschedule) and C11 (permission matrix) cannot be produced from APK/manifest evidence, and buildsmith's host has no Android device and no AVD.**

- Static C10 evidence (C10-S: no network constraints, WorkManager inventory, BOOT_COMPLETED path, no INTERNET, dependency audit) is sufficient to support the privacy claims and Data safety story — the app demonstrably cannot transmit.
- Runtime QA is still needed for release quality and for Play submission: the **FGS declaration demo video**, **store screenshots**, and **closed testing** all require an installable build on a device or emulator.
- **Lead decision (2026-11-16): AVD approved — API 36 only** (system image system-images;android-36;aosp_atd;x86_64). buildsmith is pre-staging and will boot it after the tree is green; API 33/29 images are not required because older-API behaviour is QA breadth, not a submission gate.
- Emulator scope: C7 (FGS/notification), C10-R (reminder/boot as far as UI automation allows), C11 (install/launch + permission prompts), store screenshots, and the FGS declaration demo video via adb shell screenrecord.
- **Known emulator limits:** the API 36 aosp_atd image has no real step sensor (TYPE_STEP_COUNTER probe expected absent), so step-driven screens may show zero/absent data — runtime rows must state exactly what was reachable, never infer. If adb screenrecord returns an empty file, escalate rather than skip: run the emulator windowed and capture host-side, or install a google_apis;android-36 x86_64 image (same API level; still within the Lead's API 36 approval). The FGS demo video is a Play submission prerequisite.
- **Semantics (Lead decision):** runtime rows are recorded as **NOT VERIFIED — emulator-attempted**, never PASS. Their output is supporting evidence, not proof of physical-device behaviour. C10-S static evidence remains the basis for the Data safety "no data leaves the device" answer.
- **Closed testing with 12 real testers remains an OWNER action** — an emulator cannot substitute for it, and that gate row stays OPEN/OWNER.
- Until the emulator run completes, runtime rows stay NOT VERIFIED.

### C1 — Build release AAB and debug APK

The tree builds green (B9 fixed). The Gradle wrapper (8.11.1) is being added by Lead after the current implementation wave; prefer it when present, with the absolute install as fallback:

    if (Test-Path .\gradlew.bat) { .\gradlew.bat :app:assembleDebug; .\gradlew.bat :app:bundleRelease }
    else { & $gradle :app:assembleDebug; & $gradle :app:bundleRelease }

Expected: BUILD SUCCESSFUL; AAB at app\build\outputs\bundle\release\app-release.aab (unsigned until the OWNER configures signing, B11); APK at app\build\outputs\apk\debug\app-debug.apk. Record the wrapper/fallback Gradle version and build-file hashes as build identity.

### C2 — Package, versions and target SDK (APK)

    $b='C:\Android\Sdk\build-tools\36.0.0'
    $apk='app\build\outputs\apk\debug\app-debug.apk'
    & "$b\aapt2.exe" dump badging $apk | Select-String "package:|sdkVersion:|targetSdkVersion:|application-label:"

Expected: package name com.sabeel.steps; targetSdkVersion:'36'. For the AAB, use the release APK produced from the bundle (C12) or bundletool (below).

### C3 — Lint

    & $gradle :app:lintRelease

Expected: no fatal/error issues; warnings triaged with a note.

### C4 — Merged manifest permissions (APK)

    & "$b\aapt2.exe" dump permissions $apk

Expected: ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_HEALTH, FOREGROUND_SERVICE_LOCATION, WAKE_LOCK, RECEIVE_BOOT_COMPLETED — **no ACCESS_NETWORK_STATE** (after the manifest pass) and **no android.permission.INTERNET**.

### C5 — XML tree: allowBackup, debuggable, FGS types

    & "$b\aapt2.exe" dump xmltree --file AndroidManifest.xml $apk | Select-String "allowBackup|debuggable|foregroundServiceType"

Expected: allowBackup=false; no android:debuggable="true" in release; foregroundServiceType=health and location on the services.

### C6 — Merged manifest file (release)

    Get-ChildItem app\build\intermediates\merged_manifest\release -Recurse -Filter AndroidManifest.xml |
      Select-Object -ExpandProperty FullName
    Select-String -Path (Get-ChildItem app\build\intermediates\merged_manifest\release -Recurse -Filter AndroidManifest.xml).FullName -Pattern "allowBackup|uses-permission|foregroundServiceType"

Expected: allowBackup="false"; no INTERNET; the declared normal permissions; FGS types.

### C7 — FGS/notification sanity (runtime — NOT VERIFIED without a device/emulator)

**Status: NOT VERIFIED.** No device and no AVD on buildsmith's host; runtime behaviour cannot be inferred from APK/manifest evidence (§10.1). If a device/emulator is available:

    adb install -r app\build\outputs\apk\debug\app-debug.apk
    adb shell dumpsys package com.sabeel.steps | Select-String "INTERNET|ACTIVITY_RECOGNITION|ACCESS_FINE_LOCATION|POST_NOTIFICATIONS|FOREGROUND_SERVICE"
    adb shell dumpsys activity services com.sabeel.steps

Then: start a walk with location on; confirm the FGS notification and foregroundServiceType; deny notifications and confirm tracking continues; revoke activity recognition and confirm graceful degradation; reboot to test boot behaviour. Emulator location: adb emu geo fix <lon> <lat> or Extended Controls > Location > Routes (GPX). Capture text output and a screen recording — the recording is the Play FGS declaration demo video (submission prerequisite).

### C8 — Dependency audit (no transmitting SDKs)

    & $gradle :app:dependencies --configuration releaseRuntimeClasspath | Select-String -Pattern "firebase|play-services|ads|analytics|crashlytics|amplitude|sentry|onesignal"

Expected: no matches (or only androidx.* lines you have reviewed). Also inspect the merged manifest providers/receivers for unexpected third-party entries.

### C9 — 16 KB page-size check (release APK)

    & "$b\zipalign.exe" -c -P 16 -v 4 $apk | Select-Object -Last 1
    & "$b\zipalign.exe" -c -P 16 -v 4 app\build\outputs\apk\release\app-release.apk | Select-Object -Last 1

Expected: "Verification successful". For .so files: unzip the APK and run llvm-readelf -l on each .so (or ask buildsmith for the ELF PT_LOAD script); expected p_align=16384 on 64-bit ABIs. The pre-change debug APK was verified OK on 2026-11-16 (legal-evidence C9 file), but that is not release evidence: repeat on the release APK from the green build.

### C10 — Offline behaviour and WorkManager after ACCESS_NETWORK_STATE removal

Split into static (recordable now) and runtime (needs a device/emulator).

#### C10-S — static evidence (required; buildsmith can produce)

- Grep source for NetworkType, setRequiredNetworkType, Constraints.Builder, NetworkRequest: expect no network constraint.
- WorkManager inventory (workers, schedules, tags) from source and merged manifest.
- BOOT_COMPLETED receiver + rescheduling code present; RECEIVE_BOOT_COMPLETED in manifest.
- C8 dependency audit: no transmitting SDKs; C4: no INTERNET permission — this is what backs the Data safety/no-collection claim.

#### C10-R — runtime behaviour (NOT VERIFIED without device/emulator; §10.1)

1. Airplane mode: cold-start, use every feature; expect full functionality, no crash, no network error dialogs.
2. Reminders: schedule one, force-stop and reopen, reboot, confirm it still fires/reschedules.
3. Capture adb shell dumpsys jobscheduler | Select-String -Pattern "com.sabeel.steps" and logcat before/after reboot.

Runtime QA is functional evidence, not a Play requirement; if deferred, the Lead must record explicit risk acceptance.

### C11 — Runtime permission matrix (device/emulator)

**Status: NOT VERIFIED.** Requires a device/emulator (§10.1); cannot be inferred from build artifacts.

Test on API 26/29/33/34/36 (at minimum one device each for 29, 33, 36):
- Fresh install: onboarding disclosure shown before ACTIVITY_RECOGNITION prompt.
- Deny ACTIVITY_RECOGNITION: app explains, no steps shown, no crash, can enable later via Settings.
- Deny location at walk start: walk still tracked by steps, route absent, message explains.
- Deny POST_NOTIFICATIONS: reminders toggled off with explanation; FGS notification behaviour observed.
- Revoke permission in Settings while app running: app handles loss.

### C12 — AAB manifest and signature

    $bundletool='bundletool.jar'   # path if installed
    java -jar $bundletool dump manifest --bundle app\build\outputs\bundle\release\app-release.aab | Select-String "targetSdkVersion|allowBackup|uses-permission"
    keytool -printcert -jarfile app\build\outputs\bundle\release\app-release.aab

Expected: targetSdkVersion 36, allowBackup false, no INTERNET. **Owner action:** no agent creates a production keystore (Lead decision, B11). Release stays unsigned for evidence; C12 remains **NOT VERIFIED** until the owner generates the upload key outside the repo, configures the release signingConfig, enrols Play App Signing, and buildsmith re-runs apksigner --print-certs on the signed release APK (state explicitly that the certificate is the upload key, not the Play App Signing certificate). If bundletool is unavailable, use the merged manifest in C6 plus aapt2 on the signed release APK. **Safety rule:** if signingConfig, storeFile or keyAlias appears in app/build.gradle.kts without Lead/owner instruction, buildsmith reports a FINDING and does not inspect any certificate or keystore — no agent reads, copies or generates keystore material.

---

## 11. Release gate and build-team quick rules

### 11.1 Sign-off (all must be [x])

- [ ] Green build confirmed 2026-11-16 (B9 fixed; C1 passes; wrapper added or absolute-gradle fallback documented) and the **post-manifest-pass** artifact verifies B1–B3 (C2/C4/C5/C6). 2026-11-17 batch: debug C2/C4/C5/C6 PASS; release merged manifest still absent — release-artifact gate OPEN.
- [ ] Data safety form completed truthfully; privacy policy live (§5) and linked in-app + console.
- [ ] Health declaration = Activity and Fitness; FGS declaration with demo video; content rating issued.
- [ ] No ads declaration; target audience 13+; app access = all available.
- [ ] Runtime QA on the approved **API 36 emulator**: C11 install/launch + permission prompts, C7 FGS/notification, C10-R reminders/boot as far as UI automation allows — recorded as **NOT VERIFIED — emulator-attempted**, not PASS; screenshots and FGS demo video captured (§10.1). 2026-11-17 attempt reached boot/install/launch/onboarding/reboot only; FGS/notification/reminder/walk untestable, screenshots black, demo video 0 bytes — OPEN.
- [ ] No INTERNET, no transmitting SDKs (C4, C8); allowBackup=false (C5, C6).
- [ ] OWNER: upload keystore generated outside the repo, release signingConfig configured, Play App Signing enrolled, keystore sealed; C12 then re-run (currently NOT VERIFIED — expected).
- [ ] Evidence log contains only outputs from the current green build naming com.sabeel.steps; no stale pre-change artifact was pasted (§10.0).
- [ ] Store listing copy/screenshots/name cleared (memo §6 items 2, 8 and 10; this doc §8).
- [ ] Memo publication gate accepted; counsel/scholar reviews done.
- [ ] OWNER: closed testing complete if personal account (12 real testers opted in continuously for 14 days; an emulator cannot substitute — stays OPEN/OWNER until real testers finish).

### 11.2 NEVER do (build team)

- NEVER download, install, unpack, decompile or inspect a competitor APK/AAB/ZIP — not even "to check a detail".
- NEVER copy competitor strings, descriptions, UI text, icons, screenshots, sounds, colours-as-trade-dress or layouts into the repo or the app.
- NEVER use a competitor name/logo in code, strings, listing, screenshots, ASO keywords or "compatible with" copy.
- NEVER add INTERNET permission or any SDK that phones home (ads, analytics, crash reporting, remote config, push, accounts) without re-running this checklist and the Data safety/legal gates.
- NEVER claim "zero permissions", "100% accurate", "burns X calories", "doctor recommended", "cures/treats/detects", or any health benefit that is not true of the build.
- NEVER request background location, BODY_SENSORS, exact alarms or storage permissions for v1.
- NEVER commit keystores, keys, Play credentials or personal data; never ship a debuggable release.
- NEVER host the privacy policy as a PDF, an editable Google Doc, or a geofenced/private page.
- NEVER publish while B1–B3 are open; a target-35 build is not submittable.

### 11.3 DESTROY / SEAL (build team)

- DESTROY (hash first: SHA-256 + size + path) any competitor APK/AAB/ZIP, decompiled output, extracted strings/resources/assets/databases, or competitor screenshot that somehow enters the workspace; report to Lead.
- SANITISE research notes to facts + ≤5-line attributed quotes; strip full listing/policy text.
- SEAL raw listing HTML (tmp/research/) as internal-only, never a build input; remove it from the repo before any public release of the repo.
- NEVER create "inspired-by" assets traced from a target; redo from our own spec if a similarity is found.
- Keep our own original-source evidence (design notes, icon sources, string authorship) for the originality review.

---

## 12. Sources

All fetched 2026-11-16; snapshots in docs/legal/sources/.

| Topic | URL |
|---|---|
| Target API level (Play) | https://support.google.com/googleplay/android-developer/answer/11926878 |
| Target API level (Android) | https://developer.android.com/google/play/requirements/target-sdk |
| Health Content and Services | https://support.google.com/googleplay/android-developer/answer/16679511 |
| Health app categories | https://support.google.com/googleplay/android-developer/answer/13996367 |
| Health apps declaration | https://support.google.com/googleplay/android-developer/answer/14738291 |
| Android health permissions FAQ | https://support.google.com/googleplay/android-developer/answer/12991134 |
| User Data policy | https://support.google.com/googleplay/android-developer/answer/10144311 |
| Data safety form | https://support.google.com/googleplay/android-developer/answer/10787469 |
| Data safety consumer page | https://support.google.com/googleplay/answer/11416267 |
| Prominent disclosure | https://support.google.com/googleplay/android-developer/answer/11150561 |
| FGS Play requirement | https://support.google.com/googleplay/android-developer/answer/13392821 |
| FGS types (Android) | https://developer.android.com/develop/background-work/services/fgs/service-types |
| Content ratings | https://support.google.com/googleplay/android-developer/answer/9898843 |
| Metadata policy | https://support.google.com/googleplay/android-developer/answer/9898842 |
| Ads policy | https://support.google.com/googleplay/android-developer/answer/9857753 |
| Deceptive behavior | https://support.google.com/googleplay/android-developer/answer/17006354 |
| App content / prepare for review | https://support.google.com/googleplay/android-developer/answer/9859455 |
| Notification runtime permission | https://developer.android.com/develop/ui/compose/notifications/notification-permission |
| Activity recognition (Android 10) | https://developer.android.com/about/versions/10/privacy/changes |
| Step counter guide | https://developer.android.com/health-and-fitness/fitness/basic-app/read-step-count-data |
| Android 16 behavior changes | https://developer.android.com/about/versions/16/behavior-changes-16 |
| 16 KB page sizes | https://developer.android.com/guide/practices/page-sizes |
| New personal account testing | https://support.google.com/googleplay/android-developer/answer/14151465 |
| Get started / fee / cards | https://support.google.com/googleplay/android-developer/answer/6112435 |
| Required account info | https://support.google.com/googleplay/android-developer/answer/13628312 |
| Supported locations (Iraq row) | https://support.google.com/googleplay/android-developer/answer/9306917 |
| AGP 8.7 release notes (max API 35) | https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes |
| AGP 8.10 release notes (max API 36, min Gradle 8.11.1) | https://developer.android.com/build/releases/past-releases/agp-8-10-0-release-notes |
| Clean-room decision + target terms | docs/legal/clearance-memo.md |
