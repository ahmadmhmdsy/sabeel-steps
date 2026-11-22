# Sabeel v1.3 — Play Console answers, declarations and listing copy (DRAFT)

**Status:** DRAFT for Lead/owner review. Nothing in this document has been entered in Play Console, and no submission has been made. **Play decides the outcome; this document never claims approval.** · **Date:** 2026-11-23 (build evidence added) · **Owner:** product-legal (task-19) · **Build under submission:** `com.sabeel.steps`, **versionCode 3 / versionName 1.3.0** (`app/build.gradle.kts:40-41`), compileSdk/targetSdk 36, minSdk 26.
**Supersedes:** the App-content, Data-safety, permissions, FGS, store-listing and gate sections of `docs/release/PLAY-SUBMISSION-CHECKLIST.md` (v1.0, 2026-11-16) for this release; that file remains the historical v1.0 record. `docs/legal/play-compliance-checklist.md` remains the engineering policy checklist.
**Policy sources:** support.google.com/googleplay/android-developer articles read during this work (specific IDs listed in §15). Policy changes; re-read every source within 7 days of submission. **No policy quotation is invented here** — where the exact console wording/field label matters, the item is marked **CONSOLE-CHECK** instead of guessed.

> **Evidence state (updated 2026-11-23):** the v1.3 tree now builds green. `assembleDebug`/`assembleRelease`/`bundleRelease` succeeded in one serialized run; release APK 2,138,010 B (SHA-256 `487ee9c9…7a4dfe5d`), AAB 4,608,495 B (`ffde823d…a4a9fc4e`), versionCode 3 / versionName 1.3.0, signed with the upload key. The merged release manifest was audited: no INTERNET, no background location, no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; boot receiver only `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`; FGS types health and location|health. Runtime flows were exercised on the **debug** build on an emulator (ring toggle, reboot ON/OFF, session pause/resume, CSV/GPX export, place auto-stop) — **no physical-device run** and **no demo video** exist yet. Play-form answers below are evidence-backed where noted but still owner-verified only in the console; do not claim approval or upload the AAB built without `-Psabeel.privacyPolicyUrl`.

---

## 1. What v1.3 changes (and what the declarations must therefore say)

**User-visible additions used by the Play answers:**

| Area | v1.3 behaviour (as designed in the plan) | Where it shows |
|---|---|---|
| Counting control | A persisted on/off switch. The Home ring and the status pill are the same switch (tap the ring or the pill); Settings mirrors it. The choice survives reboot and app restart. | Home, Settings, Diagnostics |
| Boot restart + evidence | `BootReceiver` records a boot attempt (permission/sensor/Health Connect flags, result, whether the service was alive after a check) before trying to restart counting; if the service is still not alive after a short retry it may post **one** "counting paused" recovery notification. Diagnostics lists the last attempts. | Boot, notification, Diagnostics |
| Custom sessions | Walk or run; optional goal (time, steps, distance, saved place); optional route recorded only while the session screen is open; pause/resume with no back-filling; finish/discard; one session at a time; midnight is split into legs. Session steps are a **slice of the global day count**, never a second counter. | Session setup, live session, History |
| Places & presets | Saved places (local label + coordinates + radius; no search, no geocoding, no map tiles) and saved session setups. Stored on the phone only. | Session setup |
| Diagnostics & share | Boot-attempt evidence and a "share diagnostics" text (no location/place data in the shared text). | Diagnostics |
| Session export helpers | CSV/GPX builders exist in the domain/data layer; the UI entry point is **not verified** at the time of writing. **Do not declare export in any Play form**; the Data safety story does not change either way (user-initiated export is not collection). | — |

**What did *not* change and controls several answers:** no INTERNET permission, no server, no ads/analytics/crash SDKs, no account, no `ACCESS_BACKGROUND_LOCATION`, no background location of any kind, no body sensors, no storage/All-files permissions, no exact alarms, no new third-party SDKs. FGS types remain `health` (step counting) and `location|health` (active session with GPS). Health Connect remains **read-only** (Steps; optional feature-gated background read; never writes). Backups remain disabled (`allowBackup="false"`).

---

## 2. Build identity and submission prerequisites

| Item | Value / answer | Status |
|---|---|---|
| Package name | `com.sabeel.steps` | VERIFIED (gradle) |
| Version name / code | `1.3.0` / `3` | VERIFIED (gradle + release artifact `aapt2 dump badging`) |
| targetSdk / compileSdk | 36 / 36 | VERIFIED (gradle + merged release manifest) |
| minSdk | 26 (Android 8.0) | VERIFIED (gradle) |
| Target API policy | New apps/updates must target API 36 (Android 16) since 2026-08-31; this build targets 36. | VERIFIED policy fact (first-party Play article, §15); re-read before submit |
| Account type | Determines the testing gate. If a **personal** account created after 2023-11-13: closed test with **≥12 testers opted in continuously for ≥14 days** before production access; organisation accounts are not named by that rule. | CONSOLE-CHECK (account type + exact gate) |
| Testing-track compliance | Closed testing is still distribution: Data safety, health declaration, FGS declaration, content rating and privacy policy apply before the first closed-test upload. | VERIFY with owner |
| Signing | Upload key + Play App Signing are owner actions; no agent reads/copies/generates keystore material. | OWNER |
| Privacy policy live | Required even though the app collects nothing; must be public HTTPS, non-geofenced, no PDF, linked **in Play Console and in-app**. Source: `docs/release/PRIVACY-POLICY.md`; hosting: `docs/privacy-site/FILL-AND-HOST.md`. | **BLOCKED — owner hosting** |
| Store assets | Screenshots, feature graphic, icon and the FGS demo video are not ready. | OWNER |

---

## 3. Play Console → App content — exact answers

| Declaration | Answer to give | Notes / proof | Status |
|---|---|---|---|
| Privacy policy | The hosted URL from `docs/release/PRIVACY-POLICY.md` (same text as `docs/legal/privacy-policy.md`, updated for v1.3). Link it in the designated console field and in-app (Settings → Privacy). | Entity name in the policy must match the Play developer name and listing. Placeholders must be filled before hosting. | BLOCKED (owner) |
| Ads | **No ads.** No ad SDK, no banner/interstitial/native/rewarded, no ad wrapper. Do **not** tick "contains ads". | Dependency audit (no ad SDK) + no INTERNET. | BUILD-VERIFY (C8) |
| App access | **All functionality is available without special access**: no login, no account, no invite, no paid gate. If the console asks for instructions: open the app, allow physical-activity permission when asked (or skip), Home shows counting; "Start a session" opens session setup; everything else is reachable from the bottom tabs. | Reviewers must be able to test every flow. | READY (copy above) |
| Target audience | **13+** — not designed for children; no Families programme. Answer "is the app designed for children?" = **No**. | Health data + ads rules differ for children; we do not target them. | READY |
| Content rating (IARC) | Complete the questionnaire honestly: **health/fitness content = yes** (activity tracking, no medical claims); violence/sex/drugs/gambling = no; user-generated content = no; ads = no; in-app purchases = no; location sharing = no; personal information sharing = no. Take the issued rating; do not pre-claim a rating. | Mandatory for all apps; re-do if content changes. | READY; OWNER submits questionnaire |
| Health apps declaration | See §4. | Required for a health/fitness app; includes Health Connect usage. | SEE §4 |
| Foreground service declaration | See §5 (health + location types). | Per-type descriptions + demo video. | SEE §5 |
| Health Connect declaration | See §6 (read-only Steps; optional background read). | Separate Health Connect form/declaration; CONSOLE-CHECK wording. | SEE §6 |
| Permissions declaration | **No restricted-permission declaration** (SMS, Call Log, All files, etc. are not requested). Location is foreground-only; ACTIVITY_RECOGNITION and notifications are runtime permissions covered by the in-app disclosure. Do not fill a "background location" form — the app does not request background location. | Verify in console which declaration forms appear for this package. | CONSOLE-CHECK |
| Background location | **Not used / not declared.** No `ACCESS_BACKGROUND_LOCATION` in the manifest; location FGS starts only from a user action while the session screen is open. | Merged manifest check C4/C5. | BUILD-VERIFY |
| Data deletion | **N/A — no accounts** (no sign-up, no login, no server-side account), so the account-deletion requirement and the "delete account URL" field do not apply. The app offers in-app **Delete all data** (Settings), which removes the local database, preferences and files; uninstall removes everything. | State this in the Data safety/deletion notes if the console asks. | READY |
| Other declarations | News: No. COVID-19: No. Financial features: No. Government: No. VPN: No. AI-generated content: No (no generative AI in-app). | — | READY |
| Category / tags | **Health & Fitness**; no competitor names/trademarks in tags. | — | OWNER |

---

## 4. App content → Health apps declaration

**Category:** **Activity and fitness** (activity tracker). Not medical, not nutrition/weight management, not research, not a medical device.
**Field labels below are paraphrased.** The console labels may differ; match the meaning, not the wording. If a field has no honest answer, stop and ask the owner — do not improvise.

### 4.1 Proposed answers

| Console question (paraphrase) | Answer |
|---|---|
| Does your app have health features? | **Yes** — physical activity tracking (step counting; optional walk/run sessions; optional route). |
| Which category? | **Activity and fitness.** |
| Is the app a medical device, or does it diagnose/treat/prevent a disease? | **No.** It is a fitness/activity tracker; counts and estimates are informational, not medical advice. |
| Does the app provide medical or health advice, diagnosis or treatment? | **No.** No diagnosis, treatment, cure or prevention claims; no medical advice; no weight-loss promises. |
| Does the app conduct or support medical research? | **No.** |
| Does the app include nutrition or weight-management features? | **No.** |
| Does the app read or write Health Connect data? | **Reads Steps only, read-only.** Optionally reads the same today total in the background (feature-gated). **Never writes** to Health Connect; no write permissions declared. |
| Rationale for Health Connect access (free text) | See §4.2. |
| Does the app use the physical-activity permission (ACTIVITY_RECOGNITION)? | **Yes** — to read the phone's step counter/detector and count steps. |
| Is the data processed only on the device? | **Yes** — sessions, routes, places and Health Connect totals stay in the app's private storage; the app has no INTERNET permission and no server. |
| Demo video | Required for the FGS declaration (§11–§12); link the same 90-second video unless the console asks for something specific. |

### 4.2 Declaration copy — English (paste into free-text fields)

**What health features does your app provide?**
> Sabeel is an offline activity tracker. It counts the phone's steps with a visible foreground service, shows today's progress against the user's own goal, and can record an optional walk or run session with time, steps, distance or saved-place goals. Session routes are recorded only while the session screen is open, and only if the user allows location. No health information is sent off the device.

**How does the app use Health Connect?**
> Read-only, opt-in, and off by default. If the phone has Health Connect and the user turns on "Use synced steps", Sabeel reads today's Steps total (StepsRecord COUNT_TOTAL) so a day synced from a watch or another app is not missed on a phone with no hardware step sensor. If the device supports it and the user grants it, the counting service may read the same total in the background; declining that grant leaves foreground counting unchanged. Sabeel never writes to Health Connect, never requests write permissions, and never shares or uploads the total.

**Medical disclaimer**
> Sabeel is not a medical device and does not diagnose, treat, cure or prevent any condition. Step counts, distance, active minutes and calorie figures are estimates. Users with a health condition or a new exercise programme are told to consult a qualified professional.

**Data handling summary**
> All health and activity data is processed and stored on the device in the app's private storage. The app has no INTERNET permission, no server, no account, no ads, no analytics and no crash-reporting SDK. Android cloud backup is disabled. Users can delete all data in the app; uninstalling removes it. The privacy policy is at the URL supplied in the Play listing.

### 4.3 Declaration copy — Arabic (paste where an Arabic field is offered; native review pending)

**ما ميزات الصحة التي يقدّمها التطبيق؟**
> سبِيل متتبّع نشاط يعمل دون إنترنت. يعدّ خطوات الهاتف عبر خدمة أمامية ظاهرة، ويعرض تقدّم اليوم نحو هدف المستخدم، ويمكنه تسجيل جلسة مشي أو جري اختيارية بهدف زمني أو بعدد الخطوات أو بالمسافة أو بالوصول إلى مكان محفوظ. لا يُسجَّل المسار إلا ما دامت شاشة الجلسة مفتوحة، وفقط إذا سمح المستخدم بالموقع. لا تُرسل أي معلومات صحية خارج الجهاز.

**كيف يستخدم التطبيق Health Connect؟**
> قراءة فقط، اختيارية، ومعطّلة افتراضيًا. إذا كان Health Connect متاحًا على الهاتف وشغّل المستخدم «استخدام الخطوات المتزامنة»، يقرأ سبِيل إجمالي خطوات اليوم فقط (StepsRecord COUNT_TOTAL) حتى لا يضيع يوم متزامن من ساعة أو تطبيق آخر على هاتف بلا مستشعر خطوات. وإذا دعم الجهاز القراءة في الخلفية وسمح بها المستخدم، يمكن لخدمة العدّ قراءة الإجمالي نفسه في الخلفية؛ ورفض ذلك لا يغيّر العدّ في المقدمة. لا يكتب سبِيل في Health Connect أبدًا، ولا يطلب أذونات كتابة، ولا يشارك الإجمالي أو يرفعه.

**إخلاء المسؤولية الطبي**
> سبِيل ليس جهازًا طبيًا، ولا يشخّص أي حالة أو يعالجها أو يمنعها. عدد الخطوات والمسافة والدقائق النشطة والسعرات تقديرات. وعلى من لديه حالة صحية أو يبدأ برنامج تمرين جديد استشارة مختص مؤهل.

**معالجة البيانات**
> تُعالج جميع بيانات الصحة والنشاط وتُخزَّن على الجهاز في التخزين الخاص بالتطبيق. لا يطلب التطبيق إذن الإنترنت، ولا خادم له، ولا حساب، ولا إعلانات، ولا تحليلات، ولا تقارير أعطال. النسخ الاحتياطي السحابي في أندرويد معطّل. يمكن للمستخدم حذف كل البيانات من داخل التطبيق، وإزالة التطبيق تحذفها. سياسة الخصوصية على الرابط المقدَّم في صفحة المتجر.


---

## 5. App content → Foreground service declaration (health + location)

The app declares exactly two FGS types. Give the declaration **per type**; the demo video must show the user-triggered flow for each. Everything below is proposed copy; the console's exact questions may differ (CONSOLE-CHECK).

### 5.1 Health type (step counting)

| Console question (paraphrase) | Answer |
|---|---|
| Which FGS type? | **health** |
| What feature does it power? | Continuous step counting for the daily activity total. |
| When does the service start? | When the user turns counting on (Home ring/pill, Settings), during onboarding, or after the user opens the app if counting was left on. On boot, the app attempts to re-start it (§5.4 honestly describes Android's restrictions). |
| What does the user see? | An ongoing, low-priority notification ("Sabeel is counting your steps") and the counting state on Home/Settings; if the service is not alive after a boot attempt, one recovery notification can appear. |
| What is the user impact if this work is deferred? | Steps taken while it is not running are not counted; the app catches up only from same-boot hardware counter data when the process restarts, and Health Connect can fill a gap only if the user granted it. The previous day's saved totals are unaffected. |
| What is the user impact if the service is interrupted? | Counting stops until Android or the app restarts the service; the app does not hide this and shows the paused/stopped state. |
| Prerequisite runtime permission | Physical activity (ACTIVITY_RECOGNITION) on Android 10+. Without it, counting cannot run; the app explains and does not crash. |
| Demo video link | §11–§12 (the segment from 0:00–0:45 is the health/step-counting demo). |

**Health-type description copy (EN):**
> Sabeel counts the user's steps with an Android health foreground service so the day total stays accurate even when the app is not on screen. The service starts when the user enables step counting, and it shows an ongoing notification. If the phone maker or Android stops it, Sabeel cannot force it back on; it shows the state and offers a one-tap restart. No health data leaves the device.

### 5.2 Location type (active session route)

| Console question (paraphrase) | Answer |
|---|---|
| Which FGS type? | **location** (declared together with health on the walk/session service: `location|health`) |
| What feature does it power? | Optional route and distance for a walk/run session that the user starts. |
| When does it start? | Only from the user's **Start session** action while the session screen is open and the user has just allowed location. **Never from boot, never from the background, never automatically.** |
| Does it use background location? | **No.** The app does not request `ACCESS_BACKGROUND_LOCATION` and does not record location when the session screen is not active. |
| What happens if location is denied or unavailable? | The session still tracks steps/time; distance and route are absent; the app says so in the UI. No crash. |
| User impact if deferred/interrupted | Route/distance points are missing for that part of the session; steps/time continue. Location recording is foreground-only and stops with the session. |
| Demo video link | §11–§12 (the segment from 0:45–1:15 shows the location permission rationale and the route in a session). |

**Location-type description copy (EN):**
> Sabeel records an optional route and improves distance only during a walk/run session the user starts. Location is requested at that moment, with an explanation, and is used only while the session screen is open; there is no background location and no map download. If the user declines, the session still records steps and time.

### 5.3 Arabic copy (health + location)

**نوع الصحة (عدّ الخطوات):**
> يعدّ سبِيل خطوات المستخدم عبر خدمة أمامية من نوع الصحة، ليبقى إجمالي اليوم دقيقًا حتى عندما لا يكون التطبيق مفتوحًا. تبدأ الخدمة عندما يفعّل المستخدم عدّ الخطوات، وتُظهر إشعارًا مستمرًا. وإذا أوقفها أندرويد أو صانع الهاتف فلا يستطيع سبِيل إجبارها على العودة؛ يعرض الحالة ويقدّم زر إعادة تشغيل. لا تخرج أي بيانات صحية من الجهاز.

**نوع الموقع (مسار الجلسة):**
> يسجّل سبِيل مسارًا اختياريًا ويحسّن المسافة فقط أثناء جلسة مشي أو جري يبدأها المستخدم. يُطلب الموقع في تلك اللحظة مع شرح، ويُستخدم فقط ما دامت شاشة الجلسة مفتوحة؛ لا يوجد تسجيل موقع في الخلفية ولا تنزيل خرائط. وإذا رفض المستخدم، تستمر الجلسة في تسجيل الخطوات والوقت.

### 5.4 Honest constraint that must not be hidden from the reviewer

On Android 15/16, starting a foreground service from the background (including from a boot receiver) is restricted. The app's design is: **attempt** the start at boot, record whether it worked in Diagnostics, and otherwise wait for the user to open the app; if it still is not running after the boot check, the app may post one recovery notification. Do not write "always restarts after reboot" in the FGS declaration or the listing. The declaration should say the app "attempts to resume counting after a restart and tells the user when Android blocks it".

---

## 6. Health Connect declaration

| Item | Answer | Status |
|---|---|---|
| Data types read | **Steps** only (today's COUNT_TOTAL). No other Health Connect types. | CODE-VERIFY (manifest/HealthConnect client) |
| Data types written | **None** — read-only; no write permissions declared. | CODE-VERIFY |
| Background read | Optional, feature-gated `READ_HEALTH_DATA_IN_BACKGROUND`; requested only when the device reports `FEATURE_READ_HEALTH_DATA_IN_BACKGROUND`. | CODE-VERIFY |
| Purpose | Keep today's step total accurate when a watch/another app syncs steps, especially on phones without a hardware step sensor; alternative to the labelled motion estimate. | READY |
| Not required for core function | Correct — declining leaves sensor counting, sessions and the app fully usable. | READY |
| Privacy policy | Must name the Health Connect read and the optional background read (it does — §3 of the policy). | VERIFY policy version |
| Reason copy (EN) | "Sabeel reads today's step total from Health Connect only when the user turns it on, as an optional fallback/reconciliation source. It is read-only, stays on the device, and is never written back or shared." | READY |
| Reason copy (AR) | «يقرأ سبِيل إجمالي خطوات اليوم من Health Connect فقط عندما يفعّله المستخدم، كمصدر احتياطي/توفيقي اختياري. القراءة فقط، وتبقى على الجهاز، ولا تُكتب أبدًا في Health Connect ولا تُشارك.» | READY (native review pending) |

**CONSOLE-CHECK:** whether the health declaration and the Health Connect declaration are one form or two for this package, whether the background read has its own question, and whether Play asks for a separate Health Connect data-access justification. Do not guess field labels; screenshot the form and send it to the Lead if it differs from this table.

---

## 7. Permissions table (merged manifest is the authority)

| Permission | Type | Why the app needs it | User-facing disclosure | Play declaration |
|---|---|---|---|---|
| `android.permission.ACTIVITY_RECOGNITION` | Runtime (API 29+) | Read step counter/detector | Onboarding and Settings explanation before the system prompt; data stays on device | Health declaration; FGS health |
| `android.permission.health.READ_STEPS` | Health Connect runtime | Read today's Steps total, opt-in | Settings Health Connect card + system HC screen; read-only, revocable | Health Connect declaration |
| `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` | Health Connect runtime (feature-gated) | Optional background read of the same total | Named in the same Settings disclosure; declining keeps foreground counting | Health Connect declaration |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Reminders, goal notification, FGS notification | Contextual explanation; denial does not crash | None |
| `android.permission.FOREGROUND_SERVICE` | Normal | Any FGS | Notification text | FGS declaration |
| `android.permission.FOREGROUND_SERVICE_HEALTH` | Normal | Step-counting FGS | Notification text | FGS health |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | Normal | Session FGS with location | Notification text | FGS location |
| `android.permission.ACCESS_FINE_LOCATION` | Runtime | Route/distance during a session | Explanation immediately before the prompt at session start; optional | Location/FGS declaration |
| `android.permission.ACCESS_COARSE_LOCATION` | Runtime | Same, approximate option | Same disclosure | Location/FGS declaration |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Re-arm counting/reminders after restart; record boot evidence | Boot behaviour explained in Diagnostics/policy | Not a runtime permission; explain in review notes if asked |
| `android.permission.WAKE_LOCK` | Normal (transitive, WorkManager) | WorkManager scheduling | — | None; verify it is present only as a transitive permission |
| `android.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Signature (AndroidX internal) | Internal broadcast safety | — | None; not a user permission |

**Must be absent** (verify merged manifest before submission): `INTERNET`, `ACCESS_NETWORK_STATE` (removed), `ACCESS_BACKGROUND_LOCATION`, `BODY_SENSORS`, storage/media permissions, `SCHEDULE_EXACT_ALARM`/`USE_EXACT_ALARM`, `AD_ID`. **Never claim "zero permissions."** The true claim is: "no INTERNET permission; the app cannot send your data anywhere."

**Status:** VERIFIED against the v1.3 release artifact (2026-11-23) — `aapt2 dump permissions` and the merged release manifest show exactly the permissions in the table above plus the AndroidX-generated `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`; `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_BACKGROUND_LOCATION`, `BODY_SENSORS`, storage/media, exact-alarm and `AD_ID` are absent.

---

## 8. Location and background-location declaration

- Location is **optional**, foreground-only, and started only by the user's "Start session" action while the session screen is open. The service is declared with `location|health` and shows a notification.
- **No background location**: no `ACCESS_BACKGROUND_LOCATION`, no location from boot, no periodic background fixes, no geofencing, no place search/geocoding, no map tiles/network.
- Saved places store a **label, latitude/longitude and radius** on the device. They are user-created locally; the app does not search for or reverse-geocode addresses.
- If location is denied, sessions still work with steps/time and say so; no crash and no repeated prompts.
- Play answers: do **not** enter a background-location declaration; if the console shows "location permissions" questions, answer that location is foreground-only, user-initiated, used for the optional session route, and not shared.


---

## 9. Data safety — exact answers

The form logic (per Play's Data safety guidance): data processed only on the device and never sent off it is **not "collected"**; a user-initiated export to a destination the user chooses is not developer collection. That is the basis for the "No" answers below. **CONSOLE-CHECK** the form wording; do not let the console's summary pages force a "collected" answer.

| Form question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No.** |
| Data types accessed on the device (not collected) | Health & fitness: step counts, session time/distance/steps, route (optional), calorie/active-minute estimates, saved places and presets. Location: only during a user-started session with permission. Preferences: goals, reminders, units, counting state. Optional: today's Health Connect step total (read-only). |
| Is any data shared with third parties? | **No.** No SDKs, servers, ads, analytics or crash reporting. |
| Is data encrypted in transit? | **Not applicable** — no data is transmitted; the app has no INTERNET permission. |
| Data deletion | In-app **Delete all data** (Settings) removes the Room database (daily steps, sessions/legs, routes, places, presets), preferences and files; uninstall removes everything. No account exists, so no account-deletion URL/flow is required. |
| Security practices | No transmission; OS app sandbox; `allowBackup="false"` (no cloud backup); no third-party SDK data collection. |
| Families / children | Not applicable (13+). |
| User-initiated export | Daily-total CSV via the system file picker (SAF) — the file goes only where the user sends it; the developer never receives it. Session CSV/GPX helpers are not declared because the UI entry point is not verified yet; adding/removing that UI does not change the "no collection" answer. |

**Evidence required before the form is submitted (task-20):** release merged manifest has no INTERNET/ACCESS_NETWORK_STATE, `allowBackup="false"`, dependency audit shows no ads/analytics/crash SDK; ideally an airplane-mode run on the release build. Until then the answer is proposed, not proven.

**Data safety Arabic note** (if the console offers localized text): «لا يجمع التطبيق أي بيانات ولا يشاركها؛ كل المعالجة والتخزين على الجهاز، ولا يطلب التطبيق إذن الإنترنت. تُحذف كل البيانات من داخل التطبيق أو بإزالة التطبيق.»

---

## 10. Store listing copy (ready to paste)

Character counts below were computed on the exact strings; Play limits are title ≤30, short description ≤80, full description ≤4000 characters, "What's new" ≤500 characters. Arabic human review is still pending — do not publish the Arabic listing before a native review.

### 10.1 Title
- EN: **Sabeel — Step Counter** — 21 chars.
- AR: «سبيل — عدّاد الخطوات» (native review pending; count the string in the console, never exceed 30).

### 10.2 Short description
- EN (72/80): **Offline step counter and walk tracker. No ads, no accounts, no internet.**
- AR (63/80): **عدّاد خطوات ومتتبّع مشي يعمل دون إنترنت. لا إعلانات ولا حسابات.**

### 10.3 Full description (EN, 1722/4000)
```
Sabeel is an offline step counter and walking tracker for Android. It counts your steps with a quiet foreground service, shows today's progress against your goal, and keeps everything on your phone: no account, no ads, no analytics, and no internet permission at all.

WHAT YOU GET
• Today's steps in a progress ring, with distance, active minutes, a streak and 7-day bars.
• Custom walk or run sessions: optional time, steps, distance or place goal; optional route recorded only while the session screen is open; pause and resume; saved setups.
• Count control: switch step counting off or on from the ring or Settings, and Sabeel remembers your choice across restarts.
• Your daily goal notification and the reminders you set.
• Diagnostics that say whether counting is running and what happened at the last boot, in plain language.
• CSV export of daily totals that you start yourself through the system file picker.

HONEST LIMITS
• Android or your phone maker can still stop background counting, especially after force-stop or aggressive battery management. Sabeel cannot reopen itself; it resumes when you next open it, or after a restart when Android allows it.
• Location is used only while a session screen is open and you allow it. There is no background location and no map download.
• Step counts, distance, active minutes and calorie figures are estimates, not medical measurements.
• Sabeel is not a medical device. Consult a qualified professional for health advice.

PRIVACY
Everything Sabeel records stays in the app's private storage. The app has no INTERNET permission, no server and no third-party SDKs. The optional Health Connect integration only reads today's step total, and only if you turn it on.
```

### 10.4 Full description (AR, 1431/4000 — native review pending)
```
سبيل عدّاد خطوات ومتتبّع مشي يعمل دون إنترنت. يعدّ خطواتك بخدمة أمامية هادئة، ويعرض تقدّمك نحو هدفك اليومي، ويحفظ كل شيء على هاتفك: بلا حساب، بلا إعلانات، بلا تحليلات، وبلا إذن إنترنت إطلاقًا.

ما تحصل عليه
• خطوات اليوم في حلقة تقدّم، مع المسافة والدقائق النشطة وسلسلة الأيام وأعمدة آخر سبعة أيام.
• جلسات مشي أو جري مخصّصة: هدف اختياري بالوقت أو الخطوات أو المسافة أو الوصول إلى مكان؛ ومسار اختياري يُسجَّل فقط ما دامت شاشة الجلسة مفتوحة؛ وإيقاف مؤقت واستئناف؛ وإعدادات محفوظة.
• التحكم في العدّ: أوقف العدّ أو شغّله من الحلقة أو من الإعدادات، ويتذكّر سبِيل اختيارك بعد إعادة التشغيل.
• إشعار الهدف اليومي والتذكيرات التي تضبطها.
• شاشة تشخيص تقول بلغة واضحة هل العدّ يعمل، وماذا حدث عند آخر تشغيل.
• تصدير CSV لإجماليات الأيام تبدأه بنفسك عبر منتقي الملفات في النظام.

حدود صريحة
• قد يوقف أندرويد أو صانع هاتفك العدّ في الخلفية، خصوصًا بعد الإيقاف القسري أو إدارة بطارية متشدّدة. لا يستطيع سبِيل فتح نفسه؛ يستأنف عندما تفتحه في المرة التالية، أو بعد إعادة التشغيل إذا سمح أندرويد بذلك.
• لا يُستخدم الموقع إلا ما دامت شاشة الجلسة مفتوحة وسمحت به. لا يوجد تسجيل موقع في الخلفية ولا تنزيل خرائط.
• عدد الخطوات والمسافة والدقائق النشطة والسعرات تقديرات، وليست قياسات طبية.
• سبِيل ليس جهازًا طبيًا. استشر مختصًا مؤهلًا لأي نصيحة صحية.

الخصوصية
كل ما يسجّله سبِيل يبقى في تخزين التطبيق الخاص. لا يطلب التطبيق إذن الإنترنت، ولا خادم له، ولا حزم طرف ثالث. تكامل Health Connect الاختياري يقرأ إجمالي خطوات اليوم فقط، وفقط إذا شغّلته.
```

### 10.5 "What's new" / release notes (EN, 401/500)
```
Counting control: turn step counting off or on from the ring or Settings, and Sabeel remembers it across restarts. New custom sessions: walk or run with an optional route, time/steps/distance/place goals, pause and resume, and saved setups. Diagnostics now show what happened at the last boot, so a phone that stops counting in the background is easier to fix. Still offline, ad-free and account-free.
```

### 10.6 "What's new" (AR, 375/500 — native review pending)
```
التحكم في العدّ: أوقف عدّ الخطوات أو شغّله من الحلقة أو من الإعدادات، ويتذكّره سبِيل بعد إعادة التشغيل. جلسات مخصّصة جديدة: مشي أو جري مع مسار اختياري، وأهداف بالوقت أو الخطوات أو المسافة أو المكان، وإيقاف مؤقت واستئناف، وإعدادات محفوظة. تعرض شاشة التشخيص الآن ما حدث عند آخر تشغيل، ليسهل إصلاح هاتف يتوقف عن العدّ في الخلفية. ما زال يعمل دون إنترنت، وبلا إعلانات، وبلا حساب.
```

### 10.7 Listing rules to keep
- No "always counting", "never stops", "100% accurate", "guaranteed", "medical", "doctor recommended", "cures/treats/detects", calorie-burn promises or rankings/awards.
- "No ads" and "no account" are true for this build; keep the dependency audit.
- Phrase privacy precisely: "no INTERNET permission — your data stays on your device", not "collects nothing" alone and never "zero permissions".
- Screenshots: real app, release build, EN + AR (RTL), no fake data/device frames, no Play badges, no health claims.
- Category **Health & Fitness**; no competitor names in the listing or ASO tags.


---

## 11. Demo video shot list (Play FGS declaration)

One continuous 90-second screen recording (or one video with two clearly labelled segments) is enough if it shows both FGS types and the permission rationale. Record on a **release build** on a physical device if possible; on the emulator use the same flow and say it is a preview. Requirements: show the real app, no personal data (clear notifications; use a test goal), readable status bar, no copyrighted music. Do not show a competitor app. Upload as a link reviewers can open without requesting access — **CONSOLE-CHECK** the exact link requirements; unlisted YouTube is the usual route, and the console will reject a link it cannot access.

| Time | Shot | What it must prove |
|---|---|---|
| 0:00–0:06 | Home, ring and counting pill visible. | Counting state is user-visible. |
| 0:06–0:14 | Tap the ring to turn counting **off**; pill and ring update; tap the pill to turn it **on** again. | The user controls counting, and the ring/pill/Settings are one switch. |
| 0:14–0:22 | Settings → "Count steps" switch shows the same state; the disclosure text about Android/OEM is visible. | No hidden switch; honest limitation shown. |
| 0:22–0:32 | Steps arrive on Home (walk 100 steps or inject on debug); daily goal progress moves. | The service really counts steps while the app is backgrounded/held. |
| 0:32–0:40 | Reboot the phone (or show the reboot already done); after restart show the counting notification and Diagnostics → "Last boot attempts" with a successful result. | Boot re-arm is attempted and evidenced, not assumed. |
| 0:40–0:48 | Start a session without location; session screen shows activity/goal and steps. | Optional session flow is user-started. |
| 0:48–1:00 | Start/enable location mid-session: show the in-app rationale, then the system location prompt, then allow; route line grows. | Location is foreground-only, explained before the prompt. |
| 1:00–1:10 | Finish the session; summary shows the session steps and "these are this session's steps; Home shows the whole day". | Session steps are a slice of the daily total, not double-counted. |
| 1:10–1:18 | Enable airplane mode; app still counts/shows data; no network error. | The app cannot transmit and does not need the network. |
| 1:18–1:26 | Open Diagnostics → share diagnostics; show the shared text preview (no coordinates). | Diagnostics evidence exists and is privacy-safe. |
| 1:26–1:30 | End card: "Sabeel — offline step counter. No account, no ads, no INTERNET permission." | Correct claims. |

**Recording commands (physical device):**
```powershell
adb shell screenrecord --bit-rate 8000000 --time-limit 180 /sdcard/sabeel-demo.mp4
# then: adb pull /sdcard/sabeel-demo.mp4
```
If the device's screenrecord is unavailable, record host-side from the device mirror. Keep the raw file with the release evidence; do not re-encode in a way that hides the status bar.

---

## 12. 90-second demo script (EN + AR)

### 12.1 English voiceover / captions

| Time | Voiceover |
|---|---|
| 0:00 | "Sabeel counts your steps without an account, without ads, and without the INTERNET permission." |
| 0:06 | "Tap the ring — or the pill — to switch counting off. Tap again to switch it back on. It is one switch, and Sabeel remembers it." |
| 0:14 | "Settings shows the same switch. Android, or your phone maker, can still stop background counting — Sabeel says so here." |
| 0:22 | "When counting is on, steps arrive even while the app is not on screen. The ring shows today against your goal." |
| 0:32 | "After a restart, Sabeel attempts to resume counting. Diagnostics records what happened at the last boot, so you can fix a phone that stops counting." |
| 0:40 | "Start a session: walk or run, with a time, step, distance or place goal." |
| 0:48 | "Location is optional. Sabeel explains why before asking, and records a route only while this session screen is open. There is no background location." |
| 1:00 | "Finish and save. The session's steps are part of your day total; Home still shows the whole day." |
| 1:10 | "Airplane mode changes nothing. The app has no INTERNET permission and no server." |
| 1:18 | "Diagnostics can share a boot report — without your location or places." |
| 1:26 | "Sabeel — offline, ad-free, account-free step counting." |

### 12.2 Arabic voiceover / captions (native review pending)

| Time | Voiceover |
|---|---|
| 0:00 | «سبِيل يعدّ خطواتك بلا حساب، بلا إعلانات، وبلا إذن الإنترنت.» |
| 0:06 | «اضغط الحلقة — أو الشريط — لإيقاف العدّ، ثم اضغط مجددًا لتشغيله. إنه مفتاح واحد، وسبِيل يتذكّره.» |
| 0:14 | «تعرض الإعدادات المفتاح نفسه. قد يوقف أندرويد أو صانع هاتفك العدّ في الخلفية، ويقول سبِيل ذلك بوضوح.» |
| 0:22 | «عند تشغيل العدّ تصل الخطوات حتى والتطبيق ليس على الشاشة، وتُظهر الحلقة تقدّم اليوم نحو هدفك.» |
| 0:32 | «بعد إعادة التشغيل يحاول سبِيل استئناف العدّ، وتسجّل شاشة التشخيص ما حدث عند آخر تشغيل، لتسهيل إصلاح هاتف يتوقف عن العدّ.» |
| 0:40 | «ابدأ جلسة: مشي أو جري، بهدف زمني أو بعدد الخطوات أو بالمسافة أو بالمكان.» |
| 0:48 | «الموقع اختياري. يشرح سبِيل السبب قبل أن يطلب الإذن، ويسجّل المسار فقط ما دامت شاشة الجلسة مفتوحة. لا يوجد تسجيل موقع في الخلفية.» |
| 1:00 | «أنهِ الجلسة واحفظها. خطوات الجلسة جزء من إجمالي يومك، وتعرض الرئيسية اليوم كله.» |
| 1:10 | «وضع الطيران لا يغيّر شيئًا. لا يطلب التطبيق إذن الإنترنت ولا خادم له.» |
| 1:18 | «تستطيع شاشة التشخيص مشاركة تقرير التشغيل — دون موقعك أو أماكنك.» |
| 1:26 | «سبِيل — عدّ خطوات بلا إنترنت، بلا إعلانات، بلا حساب.» |


---

## 13. Pre-submission checklist for this document

- [x] **BUILD (task-20):** v1.3 release AAB/APK built 2026-11-23; versionCode 3 / versionName 1.3.0; `aapt2 dump badging` shows package `com.sabeel.steps`, targetSdk 36; artifacts in `dist/` with hashes in `README.md` and `dist/SHA256SUMS.txt`.
- [x] **MANIFEST:** merged release manifest and APK audit (2026-11-23): no INTERNET, no ACCESS_NETWORK_STATE, no ACCESS_BACKGROUND_LOCATION, no BODY_SENSORS, no storage/exact-alarm/AD_ID permissions; FGS types health and location|health; `allowBackup="false"`; release not debuggable; boot receiver only `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED`.
- [ ] **SESSION ENGINE:** the v1.3 session flow compiles and was exercised on the build used for the demo video (count start → session → finish; count off mid-session warning).
- [ ] **PRIVACY POLICY:** `docs/release/PRIVACY-POLICY.md` is hosted at a public HTTPS URL, placeholders filled, entity matches the Play developer name, linked in-app via `-Psabeel.privacyPolicyUrl`.
- [ ] **HEALTH DECLARATION (§4):** entered in the console; category = Activity and fitness; HC = Steps read-only; no medical claims.
- [ ] **FGS DECLARATION (§5):** health and location descriptions entered; demo video link opens without an access request and shows both types.
- [ ] **DATA SAFETY (§9):** "No collection / No sharing" entered; delete-all documented; no contradictory answers in the console's generated summary.
- [ ] **CONTENT RATING:** questionnaire submitted; issued rating recorded in the release evidence log.
- [ ] **LISTING:** EN title/short/full + "What's new" pasted; Arabic only after native review; screenshots from the release build in EN + AR.
- [ ] **TESTING GATE:** if the account is personal and subject to the 12-testers/14-days rule, the closed test is complete before applying for production.
- [ ] **EVIDENCE:** the release evidence log names the artifact hashes and the commands that produced them; no hash or size is copied from this document.

---

## 14. Open questions — verify in Play Console, do not guess

1. Exact wording/labels of the **Health apps declaration** and whether the **Health Connect** declaration is a separate form for this package. Record the console's field labels in the release evidence log.
2. Whether the background Health Connect read (`READ_HEALTH_DATA_IN_BACKGROUND`) is declared inside the same Health Connect form or in a separate data-access justification.
3. The exact accepted **demo-video link** requirements (host, unlisted/public, length) for the FGS declaration at the time of submission.
4. Whether the console asks a **location permissions** or **sensitive-permission rationale** form for a foreground-only step app; if it does, answer with §8 and do not invent a background-location declaration.
5. The current **Data safety** form wording for user-initiated export and for on-device Health Connect access; the "No collection" answer must not be contradicted by the generated summary.
6. Whether the account is personal or organisation, and therefore which **testing gate** applies before production.
7. The current **target API** deadline text and any extension regime in force on the submission date (re-read the Play target-API article).
8. Whether the closed-testing track needs the health/FGS declarations before upload (the current reading: yes — closed testing is distribution; only internal testing is exempt from Data safety, not from policy). Confirm in the console.

---

## 15. Sources and status

| Source | Used for | Read |
|---|---|---|
| Play Console Help: Health apps declaration (support.google.com/googleplay/android-developer/answer/14738291) | health declaration, Activity and Fitness | earlier research, 2026-11-21/22; re-read before submit |
| Play Console Help: Foreground service requirements / declaration (answer/13392821) | FGS types, per-type descriptions, demo video | earlier research; re-read |
| Play Console Help: Data safety (answer/10787469) | on-device processing is not collection; user-initiated export | earlier research; re-read |
| Play Console Help: User Data policy / privacy policy (answer/9859455) and in-app privacy link (answer/10144311) | policy required, URL rules, in-app link | earlier research; re-read |
| Play Console Help: Target API level requirement (answer/11926878) | API 36 from 2026-08-31 | earlier research; re-read |
| Play Console Help: Closed testing for new personal accounts (answer/14151465) | 12 testers / 14 days | earlier research; re-read |
| Play Console Help: Content ratings (answer/9898843) | IARC rating mandatory | earlier research; re-read |
| Health Connect: data types and permissions (developer.android.com/health-and-fitness/guides/health-connect) | Steps read-only, background-read feature gate | earlier research; re-read |

**No Play approval is claimed or implied.** Submitting these answers does not guarantee approval; only a Play review decides, and a rejection must be handled as a finding, not argued with.

---

## 16. One-paragraph summary for the Lead

v1.3 adds user-controlled counting and local custom sessions; it adds **no permissions, no network, no SDKs and no background location**. The Play answers are therefore still "No data collected / no data shared", with the health declaration = **Activity and fitness** (Steps read-only, never written), FGS declaration = **health + location** (location only during a user-started foreground session), and a demo video that shows the ring toggle, boot evidence, a session with GPS, and airplane-mode operation. Blockers are unchanged and owner-side: privacy-policy hosting + placeholders, FGS demo video, listing assets, Arabic native review, and the account/testing gate. Build and merged-manifest evidence for v1.3 now exists (release APK/AAB hashes, permission and FGS audit, 205 green unit tests, lint 0 errors); what remains is owner-side: physical Redmi Note 7 run, FGS demo video, hosted privacy policy + rebuild with `-Psabeel.privacyPolicyUrl`, listing assets, native Arabic review, and the account/testing gate.
