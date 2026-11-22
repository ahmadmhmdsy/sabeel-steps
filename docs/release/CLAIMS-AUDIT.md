# Claims audit — Sabeel (com.sabeel.steps) v1.0

## 1. Header and scope

**Purpose:** prove that every user-facing claim the app and its release docs make has repository evidence behind it, or is explicitly softened. This is a release-quality audit, not a Play approval.

**Date:** 2026-11-18 (audit executed 2026-11-17 21:52-22:16 +03:00; all artifact timestamps below are local 2026-11-17).

**Scope:** user-facing copy in README.md, English and Arabic strings (including About, Privacy, Onboarding, Diagnostics, goal advice and rest day), docs/legal/privacy-policy.md, docs/release/PLAY-SUBMISSION-CHECKLIST.md, plus release docs whose claims are quoted as evidence (KEYSTORE.md, release-signoff.md, BLOCKERS.md). Read-only: no code, strings, build files or other docs were changed; .external/ untouched; no Gradle command was run.

**Method:** each claim was extracted, quoted with file:line, and checked by reading source/config, parsing both string XML files, recomputing artifact hashes, and inspecting the shipped release APK with Android build-tools 36.0.0 (aapt2, apksigner, zipalign). Status: VERIFIED = code/config/artifact proves the claim; CAVEAT = true but wording needs precision (replacement in section 3); NOT VERIFIED YET = evidence is missing (named in section 4); MISMATCH = contradicted by the repository.

**Result:** 31 VERIFIED, 11 CAVEAT, 4 MISMATCH, 4 NOT VERIFIED YET (50 claims checked). Highest-value fixes: the stale README verification table (tests quoted as 55, now 72; emulator contradiction; 17 dumps quoted for 16) and the stale docs/legal/release-signoff.md.

## 2. Claim, evidence, status

### A. Privacy, network and data claims

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| The app does not request the Android Internet permission, so it cannot send data | README.md:56-58; app/src/main/res/values/strings.xml:207,214; docs/legal/privacy-policy.md:21 | AndroidManifest.xml:5-17 declares no INTERNET and removes ACCESS_NETWORK_STATE (tools:node=remove); aapt2 dump permissions dist/Sabeel-1.0-release.apk: 10 permissions, none of them INTERNET or ACCESS_NETWORK_STATE | VERIFIED |
| No advertising, analytics, crash-reporting, attribution or social SDK | strings.xml:216; README.md:38-39,57-58,140-141; privacy-policy.md:20,51 | app/build.gradle.kts:117-161: only Kotlin, AndroidX, Compose, Room, DataStore, WorkManager, coroutines; no ad, analytics, crash or network library; APK permissions contain no AD_ID | VERIFIED |
| No account, sign-up or server | strings.xml:217-218; privacy-policy.md:19 | No auth or account code and no auth dependency in build.gradle.kts:117-161; no INTERNET permission (previous row) | VERIFIED |
| android:allowBackup=false, so app data is not in Android cloud backups | AndroidManifest.xml:21-23; privacy-policy.md:57 | aapt2 dump xmltree dist/Sabeel-1.0-release.apk: allowBackup=false; dataExtractionRules and fullBackupContent are declared at AndroidManifest.xml:22-23 | VERIFIED |
| Nothing is uploaded, ever | strings.xml:197; README.md:56 | No INTERNET permission; the only data path out is a user-initiated CSV write through SAF (CsvIo.kt:29-39,63); the user can pick any storage provider, so the file itself can leave the phone | CAVEAT |
| CSV export writes daily totals and does nothing else | strings.xml:166-167,220; CsvIo.kt:17-18 | CsvIo.kt:28-39: reads DailySteps, writes one CSV to a user-chosen content Uri via ContentResolver; no network code involved | VERIFIED |
| You can export your history as a CSV file | privacy-policy.md:63 | CsvIo.kt:17-18: the CSV is one row per local day; walks are explicitly excluded; README.md:138-139 states the same limitation | CAVEAT |
| CSV import restores daily totals, keeps the larger count per day, and is size-limited | strings.xml:169-171; docs/product/spec.md:88-89 | CsvIo.kt:41-60 (5 MB and 20,000-row limits, CsvCodec.decode, steps.restore); max-merge in StepsRepository.kt:135; CsvCodecTest 12 tests green in the 22:09:20 test report | VERIFIED |
| Delete all data erases every day, walk and setting; it cannot be undone | strings.xml:178-181; privacy-policy.md:67 | AppViewModels.kt:415-448 clears steps, walks and settings under NonCancellable + Dispatchers.IO; SettingsScreen.kt:590-600 shows the confirm dialog; SMOKE-EVIDENCE.md:33-36 records prefs and locale cleared after delete | VERIFIED |
| Location is foreground-only; the app does not request background location | privacy-policy.md:42 | AndroidManifest.xml:11-13 declares FINE and COARSE only; APK permission dump has no ACCESS_BACKGROUND_LOCATION; foregroundServiceType location and health at AndroidManifest.xml:47-50 | VERIFIED |
| The app does not ask for camera, microphone, contacts, SMS, call log, photos or general file access | privacy-policy.md:47 | APK permission dump contains none of those permissions and no READ_MEDIA or READ_EXTERNAL_STORAGE; CSV uses SAF only (CsvIo.kt:35,48) | VERIFIED |
| The in-app policy link is driven by BuildConfig.PRIVACY_POLICY_URL and is hidden when empty | README.md:101-110; SettingsScreen.kt:467-473 | app/build.gradle.kts:29,47-51 builds the field from -Psabeel.privacyPolicyUrl; generated release BuildConfig.java has PRIVACY_POLICY_URL empty; SMOKE-EVIDENCE.md:27 records the privacy web row absent with the empty value | VERIFIED |
| A link to the hosted privacy policy exists in the shipped build, and the page is live | privacy-policy.md:190,197; docs/legal/play-compliance-checklist.md:119 | BuildConfig release value is empty (previous row); privacy-policy.md:89-91 and docs/privacy-site/index.html:96,101,168-170 still contain square-bracket placeholders; no hosted URL anywhere in the repo; BLOCKERS.md:11-39 B1 is OPEN | NOT VERIFIED YET |

### B. Permission wording

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| Two permissions, nothing else | strings.xml:205-206 | Onboarding requests ACTIVITY_RECOGNITION and POST_NOTIFICATIONS, but location is a third runtime permission requested later from the walk flow (AndroidManifest.xml:11-13; WalkTrackingService at AndroidManifest.xml:47-50) | CAVEAT |
| That is the full list. Sabeel asks for nothing else. | strings.xml:225; PrivacyScreen.kt:44-61 | The screen lists the three runtime permission groups (activity, notifications, location); the merged release APK also carries the normal permissions WAKE_LOCK and DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION pulled in by libraries (aapt2 permissions dump) | CAVEAT |
| Play checklist permission list | docs/release/PLAY-SUBMISSION-CHECKLIST.md:24 | The list omits WAKE_LOCK and DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, both present in the merged release APK; the no-INTERNET part is correct; Play shows requested permissions, so keep the list exact | CAVEAT |

### C. Build, signing and verification claims

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| Package com.sabeel.steps, versionName 1.0.0, versionCode 1 | README.md:7-8; PLAY-SUBMISSION-CHECKLIST.md:18-19 | aapt2 dump badging dist/Sabeel-1.0-release.apk: package com.sabeel.steps, versionCode 1, versionName 1.0.0, targetSdk 36, minSdk 26 | VERIFIED |
| Debug APK is 11,482,549 B with SHA-256 6be50393...5fc41 | README.md:119 | Recomputed SHA-256 of dist/Sabeel-1.0-debug.apk is 6be5039321463f5b6d6c0181f43bb79ce4637a1d524d63d74bdc385f32b5fc41; size and hash match dist/SHA256SUMS.txt | VERIFIED |
| Release APK is 1,529,962 B and AAB 3,332,738 B, hashes in dist/SHA256SUMS.txt | README.md:122 | Recomputed: APK 2b905e0c647871c583a02703288eaed505478a70bd0f1274fc9a560dcc5188e0 and AAB 97d41636b6353e6f5cda84596e6dd99b1340614e61eb9738d8db09d3ce9dfaa3; sizes and hashes match SHA256SUMS.txt | VERIFIED |
| Signed with the upload key and apksigner verify OK | README.md:122-123 | apksigner verify -v dist/Sabeel-1.0-release.apk: Verifies, v2 scheme true, 1 signer; cert SHA-256 7b12ad6bfaf07817e76a2539954f7b4bde2e13f6b33454824830102566b6152d equals KEYSTORE.md:22; debug APK cert is CN=Android Debug | VERIFIED |
| zipalign -c -P 16 OK | README.md:122 | zipalign -c -P 16 dist/Sabeel-1.0-release.apk prints Verification successful | VERIFIED |
| Signing loads keystore.properties only when the file exists; CI and fresh clones build unsigned; there is no debug fallback | README.md:84-85; app/build.gradle.kts:21-24,54-76 | signingConfigs.release is created only when hasReleaseKeystore; the release block sets signingConfig only in that branch (:74-76); the release certificate is a Sabeel upload cert, not CN=Android Debug (previous row) | VERIFIED |
| Release has isMinifyEnabled=true and isShrinkResources=true; the R8 build was smoke-tested | README.md:83,142-145 | app/build.gradle.kts:67-69; app/build/outputs/mapping/release/mapping.txt exists (26,450,270 B); SMOKE-EVIDENCE.md:5 names the signed release APK as the build under test | VERIFIED |
| CI runs build, tests, lint and an unsigned release bundle | README.md:162 | .github/workflows/android.yml:18,42-48 runs assembleDebug, testDebugUnitTest, lintDebug, bundleRelease; workflow comment :40-41 states the unsigned path; no CI run log is stored in the repo | VERIFIED |
| lintDebug exit 0: 0 errors, 37 warnings, 1 hint | README.md:121 | app/build/reports/lint-results-debug.txt contains exactly 0 errors, 37 warnings, 1 hint (mtime 22:12:22, after the newest source file at 22:08:18); the Gradle exit code itself was not re-observed because this audit did not run Gradle | CAVEAT |
| 55 tests, 0 failures (exit 0): CsvCodec 12, StepMerge 7, StepMath 18, StepPipeline 13, StepCadence 5 | README.md:120 | Source now has 72 @Test in 7 files: adds GoalAdvisorTest 9 and LocalizationParityTest 4, and StepMathTest is 22. The 22:09:20 test report has 7 XML files: tests=72, failures=0, errors=0, skipped=0 | MISMATCH |
| No device/emulator run is claimed by this repository's final state; nothing below claims device/emulator validation or publication | README.md:9-11,114-115 | Contradicted by README.md:124 (Emulator run completed) and by docs/review/2026-11-17/smoke/SMOKE-EVIDENCE.md:23-40 | MISMATCH |
| Evidence is 17 UI-hierarchy dumps in docs/review/2026-11-17/smoke/ | README.md:124 | The directory has 16 ui-*.xml dumps (ui-01, ui-03 through ui-17) plus SMOKE-EVIDENCE.md: 17 files, but only 16 dumps; the smoke table has 16 steps | MISMATCH |
| Emulator run completed on the API 36 ATD image with the signed release APK (onboarding, grant and skip, no-sensor status, Arabic RTL, walk foreground service, delete-all, relaunch) | README.md:124 | SMOKE-EVIDENCE.md:5,23-40 describes that run and the 16 dumps in docs/review/2026-11-17/smoke/ exist; the ATD has no step sensor, so the NO_SENSOR path is what was exercised (SMOKE-EVIDENCE.md:48-50) | VERIFIED |
| No crashes during the emulator run | README.md:124; SMOKE-EVIDENCE.md:44-45 | Only a narrative sentence says the AndroidRuntime log was empty; no raw logcat or bugreport file is stored, and UI dumps cannot prove the absence of crashes | NOT VERIFIED YET |
| Physical-device validation is still pending and the R8 smoke list must be repeated on hardware | README.md:124,132-134,142-145 | No physical-device evidence in the repo; the ATD image used has no step sensor (SMOKE-EVIDENCE.md:48-50); BLOCKERS.md:146-159 B7 is OPEN | VERIFIED |
| Release state: no release AAB, no release merged manifest, no signingConfig, no release artifact, 41/41 tests | docs/legal/release-signoff.md:3,5,29,33,34 | Contradicted by dist/Sabeel-1.0-release.apk and .aab (rows above), app/build.gradle.kts:54-76, and the 72-test report; signoff:5 also quotes an obsolete debug hash 324690...9B9E, not the current 6be50393...5fc41 | MISMATCH |

### D. Localization

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| English and Arabic declare the same keys with matching placeholders | app/src/main/res/values/strings.xml and values-ar/strings.xml | Independent XML parse: 242 string keys each, 3 plurals each, key diff none, no blank values; LocalizationParityTest.kt:24-63 covers keys, blanks and placeholders; its 4 tests are green in the 22:09:20 report | VERIFIED |
| Arabic and English are first-class languages | README.md:5; docs/product/brief.md:16 | Key and placeholder parity verified (previous row) and Arabic RTL smoke dumps exist (SMOKE-EVIDENCE.md:28-29,38-40); the native Arabic review and RTL/digit sign-off are still open (README.md:135-136; BLOCKERS.md:132-142 B6) | CAVEAT |
| RTL is supported | AndroidManifest.xml:27; values-ar/strings.xml | supportsRtl=true; Arabic smoke dumps ui-05, ui-06 and ui-15 to ui-17 show mirrored layout and ar-IQ locale | VERIFIED |

### E. Product behaviour and other user-facing copy

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| Step pipeline seeds its baseline from the first sample, rolls over at local midnight and keeps per-day totals monotonic | README.md:21-23; docs/product/brief.md:13-14 | StepPipeline.kt and StepMerge.kt; StepPipelineTest 13 and StepMergeTest 7 tests green in the 22:09:20 report; runtime evidence is sensor-less (ATD), so this is unit-level only | VERIFIED |
| Walk uses a location foreground service and a Canvas route, with no map tiles, API key or network; sensor fallback when location is denied; checkpoint and resume | README.md:27-29 | WalkTrackingService.kt:661 starts the FGS; AndroidManifest.xml:47-50 declares location and health types; no maps SDK in build.gradle.kts:117-161; SMOKE-EVIDENCE.md:30-31 covers the location-denied walk | VERIFIED |
| Reminders are WorkManager-based and the goal notification is POST_NOTIFICATIONS-aware | README.md:31-32 | Reminders.kt:20-52 (WorkManager only, no exact alarms or wakelocks), Notifications.kt:137-176, AndroidManifest.xml:8 | VERIFIED |
| v1 has no accounts, cloud sync, ads, subscriptions, social, Health Connect or widgets | README.md:38-39 | No such dependencies in build.gradle.kts:117-161 and no widgets or Health Connect components in AndroidManifest.xml | VERIFIED |
| Distance, energy and active minutes are estimates, not medical measurements or advice | strings.xml:76-77,143,185,226-227,235; privacy-policy.md:79 | Every surface that shows those values carries the word estimate; About and Privacy say the app is not a medical device and gives no health advice | VERIFIED |
| The app is free with no ads, in-app purchases, subscriptions or accounts | PLAY-SUBMISSION-CHECKLIST.md:21-22; README.md:38-39 | No billing, ad, auth or subscription dependency in build.gradle.kts:117-161; permission surface (row 1) has no network or billing permission | VERIFIED |
| Goal advice needs about two weeks of days, looks at the last four weeks, moves the goal by at most 10% and only when tapped | strings.xml:238-245 | GoalAdvisor.kt:19-32 sets MIN_RECORDED_DAYS=14, WINDOW_DAYS=28, MAX_WEEKLY_CHANGE_FRACTION=0.10, MIN_CHANGE_STEPS=200; :62-89 implements advice and caps; AppViewModels.kt:338-367 wires it; GoalAdvisorTest 9 tests green | VERIFIED |
| Goal advice describes your middle day over the last four weeks | strings.xml:243 | GoalAdvisor.kt:60-77 computes a median over recorded days, explicitly ignoring zero-step days and duplicate dates, so it is not a calendar median | CAVEAT |
| A rest day pauses the streak, never adds a day, and steps are still counted | strings.xml:83,249-251 | StepMath.kt:29-41 streak(restDays); StepMathTest.kt:164-205 has four rest-day tests including restDayNeverAddsADayToTheStreak; SettingsStore.kt:98-107, AppViewModels.kt:97,367-373, SettingsScreen.kt:186-215 | VERIFIED |
| Sabeel reconnects when you open the app and once a day | strings.xml:288 | Foreground entry calls StepCounterService.ensureStarted (SabeelRoot.kt:96) and BootReceiver.kt:41 restarts it; the periodic reconcile runs every 15 minutes (Reminders.kt:47-56) and deliberately does not start the service (Reminders.kt:108-116). Once a day is wrong in both frequency and meaning | CAVEAT |
| A rest day or a paused goal never changes the counted steps; only the streak pauses | strings.xml:290 | Only rest days exist: StepMath.kt:29-41 and the rest-day settings; there is no paused-goal feature anywhere in code or strings | CAVEAT |
| Sabeel never invents a total to fill a gap | strings.xml:288 | No synthetic-fill path found (reconcile only rolls today over and posts the goal notification, Reminders.kt:122-140; CSV import is user-initiated, CsvIo.kt:46-60), but no test names this invariant and the ATD run only covers the NO_SENSOR path | NOT VERIFIED YET |
| Kotlin, Compose and AndroidX are Apache 2.0, and full license notices ship with the source | strings.xml:234; README.md:16-17,175-176 | LICENSE and NOTICE exist; app/src/main/assets/THIRD-PARTY-NOTICES.txt lists the components used (first 25 lines read); completeness against the full transitive dependency tree was not audited | CAVEAT |
| Sabeel is not directed to children under 13 | privacy-policy.md:75 | Play target-audience declaration has not been made (BLOCKERS.md:78-83 B3); nothing in code contradicts the statement, but it is a declaration matter | NOT VERIFIED YET |
| Privacy policy substantive claims: no collection, no third-party SDKs, backup off, delete-all, estimates disclaimer | privacy-policy.md:17-22,51,57,67,79 | Each claim is supported by rows 1-4, 9 and 40, and the policy text matches the build; publication is blocked separately (row 13), not by the substantive content | VERIFIED |

## 3. Claims we should soften

- **README.md:120 (tests).** Replace the count with: Unit tests: 72 tests, 0 failures, 0 errors (report app/build/test-results/testDebugUnitTest, 2026-11-17 22:09:20): CsvCodec 12, StepMerge 7, StepMath 22, StepPipeline 13, StepCadence 5, GoalAdvisor 9, LocalizationParity 4. Re-run before quoting any count.
- **README.md:9-11 and 114-115 (device/emulator contradiction).** Replace with: An emulator smoke run on the API 36 ATD image was completed with the signed release APK on 2026-11-17 (16 UI-hierarchy dumps in docs/review/2026-11-17/smoke/); physical-device validation is still pending and is not claimed.
- **README.md:124 (dump count).** 17 UI-hierarchy dumps becomes 16 UI-hierarchy dumps (the 17th file is the smoke notes).
- **README.md:121 (lint).** Attribute the numbers: the lint report shows 0 errors, 37 warnings, 1 hint (2026-11-17 22:12); the build lint task must stay exit 0. Do not state exit 0 as independently re-observed.
- **README.md:124 (no crashes).** Either cite a stored raw logcat path or soften to: no crash dialog or ANR observed in the captured session; the raw logcat was not archived.
- **README.md:56 and strings.xml:197 (upload absolutes).** Replace with: Sabeel never uploads anything. If you export a CSV, the file goes only where you save or share it.
- **strings.xml:205-206 (Two permissions, nothing else).** Replace with: Two permissions at setup. Location is asked only when you start a walk.
- **strings.xml:225 (full list).** Replace with: That is the full list of permissions Sabeel asks you to allow.
- **PLAY-SUBMISSION-CHECKLIST.md:24 (permission list).** Keep No INTERNET, and add: WAKE_LOCK and DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION are merged from libraries as normal, non-prompted permissions.
- **strings.xml:288 (reconnect frequency).** Replace with: Sabeel restarts counting when you open the app and after your phone restarts, and it re-checks your day in the background every 15 minutes.
- **strings.xml:288 (never invents a total).** Replace with: and it does not fill a gap with made-up steps. Add a unit test for this gap-honesty invariant.
- **strings.xml:290 (paused goal).** Replace with: A rest day never changes the counted steps; only the streak pauses. (No paused-goal feature exists.)
- **strings.xml:243 (middle day).** Replace with: Your median recorded day over the last four weeks was X steps. (Zero-step days are ignored; say so if space allows.)
- **privacy-policy.md:63 (export history).** Replace with: You can export your daily step totals as a CSV file. Walks are not part of that file.
- **strings.xml:234 (full license notices).** Replace with: Third-party license notices are listed in THIRD-PARTY-NOTICES.txt in the source. Upgrade to full only after the list is checked against the dependency tree.
- **README.md:5 (first-class languages).** Replace with: English and Arabic are both launch languages; key and placeholder parity is unit-tested, and native Arabic review is still open.
- **docs/legal/release-signoff.md:3-5,29,33,34.** Regenerate from the current signed release APK/AAB and the 72-test report; delete 41/41 and the obsolete debug hash 324690...9B9E, and update G17, R1 and S1, which still say there is no release artifact and no signingConfig.

## 4. Evidence not found

- Raw logcat/bugreport from the emulator smoke run. Only SMOKE-EVIDENCE.md is stored, and UI dumps cannot prove the absence of crashes. Settled by archiving adb logcat -d and the crash buffer with the run.
- Step-sensor validation on real hardware. The ATD image has no step sensor (SMOKE-EVIDENCE.md:48-50), so only the NO_SENSOR path ran. Settled by the BLOCKERS.md B7 two-device matrix (API 26-30 and API 33+) with counting observed while the screen is off.
- Hosted privacy policy URL, filled identity placeholders, and an in-app policy link in a real build. BuildConfig.PRIVACY_POLICY_URL is empty in the dist artifacts, privacy-policy.md:89-91 and docs/privacy-site/index.html still hold placeholders, and no HTTPS URL exists in the repo. Settled by filling the placeholders, hosting docs/privacy-site/, rebuilding with -Psabeel.privacyPolicyUrl=..., and capturing the Settings row plus a 200 response.
- Arabic native review sign-off (BLOCKERS.md:132-142 B6). No signed review note exists; needed before calling Arabic production quality.
- Trademark/name clearance searches (BLOCKERS.md:43-62 B2). No recorded searches or go/no-go note.
- Play Console evidence: account type and identity verification, Play App Signing registration, Data safety, Health apps declaration (Activity and Fitness), FGS declarations plus demo video, IARC rating, target audience, closed testing with 12 testers for 14 days (release-signoff.md:19-24; BLOCKERS.md:66-89 B3). Only static build evidence exists.
- CI execution evidence. .github/workflows/android.yml exists, but no run log or uploaded artifact is stored in the repo. Settled by a green Actions run.
- AAB signature and AAB manifest verification. This audit verified the release APK with apksigner; the AAB was hash-checked only (jarsigner or bundletool not run).
- Gradle exit codes for testDebugUnitTest and lintDebug. The reports exist; the audit was instructed not to run Gradle, so exit 0 is taken from the reports and README, not independently re-observed.
- mapping.txt permanence. The R8 mapping exists only as a build output (app/build/outputs/mapping/release/mapping.txt); archive it with release evidence so obfuscation stays recoverable.
- Privacy-policy EN/AR equivalence and index.html sync with the Markdown source. Not audited here because the policy is still a draft with placeholders.

## 5. Method notes

- Read-only audit. Commands/tools: PowerShell Get-ChildItem, Get-FileHash, Select-String; file reads and greps; aapt2 dump badging, permissions and xmltree; apksigner verify -v and --print-certs; zipalign -c -P 16; a PowerShell XML comparison of values/ and values-ar/ (242 string keys and 3 plurals each, no blanks, no placeholder drift); an XML parse of app/build/test-results/testDebugUnitTest (7 suites, 72 tests, 0 failures, 0 errors); reading app/build/generated/source/buildConfig/release/com/sabeel/steps/BuildConfig.java; SHA-256 recomputation of all three dist artifacts against dist/SHA256SUMS.txt.
- Release APK permissions (aapt2): ACTIVITY_RECOGNITION, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_HEALTH, FOREGROUND_SERVICE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. None is INTERNET, ACCESS_NETWORK_STATE, AD_ID or ACCESS_BACKGROUND_LOCATION.
- gradle/libs.versions.toml does not exist in this repository; dependency versions are inline at app/build.gradle.kts:117-161. A copy exists only under .external/ (out of scope and not read).
- The workspace was being edited during the audit: GoalAdvisor.kt 21:58, StepMath.kt 21:58, DiagnosticsScreen.kt 22:00, strings.xml 22:01, LocalizationParityTest.kt 22:08; the test report is 22:09:20 and the lint report 22:12:22. README.md dates from 19:04 and release-signoff.md from an earlier debug-only evidence batch, which explains several stale claims.
- Date note: the header uses 2026-11-18 as instructed; the audit ran at 2026-11-17 22:xx +03:00 and every artifact timestamp quoted is local 2026-11-17.
- Not modified: any code, string resource, build file, keystore, other documentation, or .external/. No Gradle command was run.

## 6. Addendum — resolution log (2026-11-17 22:50 +03:00)

Sections 1–5 are the read-only audit snapshot and stay as history. This section records what changed
afterwards (the TrueStep-adoption pass and the rebuild), so the audit can be read against the final
tree. It was written by the Lead, not by the auditor.

### 6.1 Final artifacts and gates (measured on the final tree, build-tools 36.0.0)

Refreshed 2026-11-19 after the source-ladder change; the 2026-11-17 values stay in git history.

| Item | Final value |
|---|---|
| Debug APK | 12,015,507 B, SHA-256 `595cf33f6dddeceb48ab482bef4f29e769e96f18bd1d55a0ded8d99b96e22fab` |
| Release APK | 1,683,698 B, SHA-256 `35723c87330a11500186f6eeae69245a0646e8ff2c58e6ee82d6ef2a4ec6c1aa` |
| Release AAB | 3,538,379 B, SHA-256 `f8ee8b26a6d49b43e6ded3ad2c17eaf3e2898b3d30040928ac5b3d8283027d7b` |
| Signatures | `apksigner verify`: Verifies (v2, 1 signer, upload cert `7B:12:AD:…:15:2D`); `jarsigner -verify` on the AAB: "jar verified." (self-signed upload cert, no timestamp) |
| 16 KB alignment | `zipalign -c -P 16 4 dist/Sabeel-1.0-release.apk` exit 0; 8 stored `.so` entries are 16 KB-aligned, `extractNativeLibs=false` |
| Release manifest | com.sabeel.steps, versionCode 1, versionName 1.0.0, minSdk 26, targetSdk 36, allowBackup false |
| Permissions (release APK) | the same 10 as §5; no INTERNET, ACCESS_NETWORK_STATE, ACCESS_BACKGROUND_LOCATION or AD_ID |
| Unit tests | **108 tests, 0 failures, 0 errors, 0 skipped** in 11 suites (report 2026-11-19 18:44): CsvCodec 12, StepMerge 7, StepMath 24, StepPipeline 19, StepCadence 5, GoalAdvisor 9, LocalizationParity 4, OemBatteryGuide 5, MotionStepDetector 8, StepSourceHealth 9, StepSourcePolicy 6 |
| Lint | `lintDebug` **exit 0** — 0 errors, 44 warnings, 1 hint (2026-11-19 19:02) |
| Emulator evidence | 2026-11-17 passes (25 UI dumps EN+AR across [smoke/](../review/2026-11-17/smoke/SMOKE-EVIDENCE.md) and [adoption-smoke/](../review/2026-11-17/adoption-smoke/EVIDENCE.md)) plus the 2026-11-19 estimate-tier run (EN+AR, ≈ 97 estimated steps, walk labels; [step-source-resilience/EVIDENCE.md](../review/2026-11-19/step-source-resilience/EVIDENCE.md)); crash buffers empty |
| dist/SHA256SUMS.txt | regenerated from the three artifacts above and verified by recomputation |

### 6.2 Resolution of every MISMATCH in §2 and §3

| Audit item | Resolution |
|---|---|
| README test count (55/72) | README verification row now states 79 tests / 0 failures with all 8 suites; report XML agrees. |
| README "no device/emulator run" contradiction | README no longer claims that; it states the emulator passes exist, links both evidence folders, and keeps physical-device validation pending. |
| Dump count 17 vs 16 | README says 16 dumps for the earlier smoke pass; the adoption pass adds 9 more, cited by folder. |
| Stale `docs/legal/release-signoff.md` (no release AAB, no signingConfig, 41/41, obsolete debug hash) | Rewritten from the final signed artifacts: R1 **CLOSED**, G17 **PARTIAL — OWNER** (keystore exists, Play App Signing enrolment pending), S1 **PASS 79/79**, hashes and sizes replaced, owner actions renumbered. |

### 6.3 Resolution of the §3 wording items

Fixed in strings/docs: upload absolutes (now "nothing leaves the phone on its own; only a CSV you
export yourself can"), permission wording ("Two permissions to start" plus optional location, and
"These are the only permissions Sabeel ever asks you for"), Play-checklist permission list (adds
WAKE_LOCK and DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, no background location), reconnect wording
("when you open the app and after your phone restarts", the 15-minute misquote removed), gap honesty
("does not make up a total to fill a gap"), rest day ("never changes the counted steps; the streak
just pauses", no paused-goal), goal advice ("median recorded day" EN+AR), privacy-policy export
("daily step totals; walks are not part of that file" EN+AR), license wording ("kept with the
source"), and README language status ("supported; native review pending").

Still true by design, kept as caveats: a user-exported CSV can leave the phone; location is a third
runtime permission asked from the walk flow, not setup; two normal permissions come from libraries.

### 6.4 What is still not verified

Unchanged and owner- or hardware-bound: hosted privacy-policy URL and an in-app policy link in a
real build (G10), physical-device sensor accuracy and FGS/notification/reminder behaviour (G9, B7),
native Arabic review, name/applicationId clearance, Play Console declarations and closed testing, a
stored CI run log, and a mapping.txt archived outside the build directory. The "never invents a
total" invariant has wording and no dedicated test; the no-crash evidence is a sampled empty buffer,
not a device-wide proof; THIRD-PARTY-NOTICES completeness against the full dependency tree was not
audited.

Historical review/legal evidence files under `docs/review/2026-11-17/` and
`docs/legal/release-evidence-log.md` keep their original measured values as dated snapshots;
`FIX-REPORT.md` now carries a snapshot note pointing to the final numbers.


## 7. Addendum — step-source resilience (2026-11-19)

This section extends the audit; sections 1–6 remain the dated 2026-11-17/18 record. It covers the
measured-first source ladder and the labelled last-resort estimate shipped after the 2026-11-19 Qiyas
Council, in commit `8246615` plus its tests, strings and evidence.

**Artifacts measured for this addendum:** debug APK built 2026-11-19 18:44 +03:00,
12,015,507 B, SHA-256 `595CF33F6DDDECEB48AB482BEF4F29E769E96F18BD1D55A0DED8D99B96E22FAB`;
JVM tests 108 in 11 suites, 0 failures; `lintDebug` exit 0 (0 errors, 44 warnings, 1 hint).
Emulator evidence: [docs/review/2026-11-19/step-source-resilience/EVIDENCE.md](../review/2026-11-19/step-source-resilience/EVIDENCE.md).

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| Counting is measured-first: hardware counter → hardware detector → labelled accelerometer estimate → none, chosen from `SensorManager` availability | README.md "What it does"; docs/product/spec.md §3 rule 1; Diagnostics copy | `StepSource.kt` (`StepSourcePolicy.choose`), `StepSensors.kt`; `StepSourcePolicyTest` 6 tests; the emulator image has no counter/detector and the service chose `MOTION_ESTIMATE` (status XML + Diagnostics dumps) | VERIFIED |
| Every source switch rebases, preserves today's total and ignores the switch delta; a counter catch-up after a fallback is not counted | README.md; docs/product/spec.md §3 rule 6 | `StepPipeline.kt` source-switch branch; `StepPipelineTest` source-switch, counter-catch-up and midnight tests; hardware transition not yet exercised | VERIFIED (unit level) |
| Physically impossible deltas are clamped; normal walking is not | README.md; docs/product/spec.md §3 rule 7 | `MAX_STEPS_PER_SECOND = 4.0`, `CLAMP_GRACE_STEPS = 3` in `StepPipeline.kt`; `StepPipelineTest` clamp tests (10,000-step jump rejected, normal cadence kept, skip when timestamp unknown) | VERIFIED (unit level) |
| A measured source that goes silent is probed for walking before switching, and still-phone probes back off exponentially | docs/product/spec.md §3 rule 8 | `StepSourceHealth.kt`; `StepSourceHealthTest` 9 tests (stall, two-probe switch, streak reset, backoff, estimate never probed) | VERIFIED (unit level) |
| The estimate is labelled at the number, in Home, Diagnostics, the walk screen and summary, the foreground notification and the goal notification | README.md; strings.xml / values-ar/strings.xml; strings_sensor.xml | EN+AR Home: "≈ 97"; estimate note; Diagnostics "Phone motion (estimated)"/"حركة الهاتف (تقديري)"; walk live/summary "≈" + note (Arabic dumps qa-17/18); notification "Sabeel is estimating your steps"; goal text has an estimated variant; emulator dumps in the evidence folder | VERIFIED (walk numeric path hardware-pending) |
| Diagnostics exposes live source, last sensor reading age and fallback count; NO_SENSOR now means no counter, no detector and no accelerometer | README.md; docs/product/spec.md §3 rule 1 | `StatusStore` source fields; `AppViewModels.sourceStatusFlow`; `DiagnosticsScreen.kt`; emulator Diagnostics dump shows source / last reading / "Fallbacks today: None" | VERIFIED |
| Estimate accuracy is good enough to promise real-world step counts | — (no such claim is made) | Only a synthetic 1.8 Hz waveform was tested: 97 estimated from 108 injected cycles. Real bag-damped, hand-carry and handshake motion remains ambiguous; the label is the honesty mitigation | CAVEAT / NOT CLAIMED |
| Walk sessions use the same ladder, so a detector-only or estimate-only phone counts walk steps | docs/product/spec.md §4 | `WalkTrackingService.kt` registration ladder, detector accumulator and estimate detector; compiles and all unit tests pass; no walk ran on the sensor-less emulator with a measured source (there is none) | VERIFIED (code) / hardware pending |
| No new permission or dependency was added for any tier | README.md permission claims | Merged manifest still has the same 10 permissions, no INTERNET, `ACTIVITY_RECOGNITION` reused; no new dependency in `app/build.gradle.kts` | VERIFIED |
| Estimated steps count toward the goal and streak only while marked | docs/superpowers/plans/2026-11-19-step-source-resilience.md (owner decision, reversible) | Estimate flows through the same writer/DB as measured steps and Home/Diagnostics mark the source; hiding estimate from arithmetic is a config change, not a code rewrite. Owner has not objected to the recommended default | CAVEAT (owner decision, reversible) |

The 2026-11-17 §2 row "Step pipeline seeds its baseline… (unit-level only)" is superseded in wording by the
source-ladder row above; its evidence claim about hardware is still accurate — no physical-device sensor
validation exists. The Arabic estimate/fallback copy remains marked for native review (same status as the
rest of the Arabic copy).

## 8. Addendum — Health Connect read-only floor and estimate provenance (2026-11-21)

This section extends the audit; sections 1–7 remain dated records. It covers the optional read-only
Health Connect tier, the labelled estimate provenance now stored in the database/CSV/UI, and the
Room v1→v2 migration.

**Artifacts measured for this addendum (2026-11-21, build-tools 36.0.0):**

- Release APK: 1,861,838 B, SHA-256 `C5BD200DA95D3FD1F1C417FB6C11FBE8E06700CB6A75C07E5CA793F1A1DC4958`;
  AAB: 3,977,681 B, SHA-256 `7A2FD53AA12603127403D852A35C168325A9AD4FA7F5843CD9782819EA06BC77`;
  debug APK: 15,188,660 B, SHA-256 `72D97C3B3FAF8889834AFC5871C9A4CA8A09C232DAE3571EEA7913891156B238`.
  The release APK/AAB are signed with the upload key (cert `7b12ad6baf07817e76a2539954f7b4bde2e13f6b33454824830102566b6152d`);
  `apksigner verify`, `jarsigner -verify` and `zipalign -c -P 16` are OK. VersionCode 1,
  versionName 1.0.0 (unchanged; nothing uploaded).
- JVM tests: **121 tests in 11 suites, 0 failures, 0 errors, 0 skipped**;
  `lintDebug` exit 0 (0 errors, 44 warnings, 1 hint); `lintVitalRelease` passes.
- Emulator evidence: [docs/review/2026-11-21/health-connect/EVIDENCE.md](../review/2026-11-21/health-connect/EVIDENCE.md).

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| Health Connect is an optional extra tier, tried only when the phone has no hardware counter and no hardware detector | README.md; docs/product/spec.md §3 rule 4b; docs/superpowers/plans/2026-11-19-step-source-resilience.md | `StepSource.choose` receives `SourceAvailability.healthConnect`; `StepSourcePolicyTest` (9 tests); `StepCounterService` gates readiness on neither sensor existing; the Settings card is gated by `StepSensors.availability(context).noMeasuredStepSensor` (emulator Settings dump shows it on the sensor-less image) | VERIFIED |
| The app reads steps from Health Connect and never writes to it | README.md; docs/legal/privacy-policy.md; docs/legal/play-compliance-checklist.md §6.3b | The manifest declares only `android.permission.health.READ_STEPS`; `HealthConnect.kt` uses `HealthPermission.getReadPermission(StepsRecord::class)` and an aggregate `StepsRecord.COUNT_TOTAL` read; a source audit finds no `insertRecords`/`writeRecords`/`deleteRecords` call; Health Connect's own permission screen for Sabeel asks "Allow “Sabeel” to read" with **Steps** as the only type (ui-03) | VERIFIED |
| A Health Connect total is an absolute floor (`max(current, hcToday)`), never added to the estimate, so repeated polls and repeated activation cannot double count | docs/product/spec.md §3 rule 4b; README.md | `StepPipeline.onExternalTotal`; `StepPipelineTest` external-total tests (floor, same/lower total ignored, day rollover resets the estimate, negative total rejected) | VERIFIED (unit level) |
| Health Connect polling is bounded: first poll 5 s, then every 2 min; 30 min of failed reads fall back to the estimate; only a **rising** provider total counts as liveness, so a static provider cannot suppress the estimate watchdog | README.md; docs/product/spec.md §3 rule 4b and the polling paragraph | `StepCounterService` constants `HC_FIRST_POLL_MS`, `HC_POLL_MS`, `HC_STALE_MS`; `onExternalTotal` only refreshes liveness when the total rises; the estimate watchdog is untouched. The timing/device path is not exercised: the emulator has no Health Connect step data | VERIFIED (code) / device pending |
| The estimate provenance is persisted and exported: `daily_steps.estimatedSteps`, `walks.estimated`, and a sixth `estimated_steps` CSV column | README.md; docs/product/spec.md CSV schema; docs/release/PLAY-SUBMISSION-CHECKLIST.md | Entities/mappers and `StepsRepository` clamp the estimate to `0..steps`; `CsvCodec` 6-column header/round-trip; legacy 5-column header and rows still import with `estimatedSteps = 0`; `CsvCodecTest` 15 tests | VERIFIED (unit level) |
| The Room v1→v2 upgrade is additive and non-destructive | README.md; docs/product/spec.md | `MIGRATION_1_2` has two `ALTER TABLE … ADD COLUMN … NOT NULL DEFAULT 0` statements; there is no `fallbackToDestructiveMigration`; schema 2 is exported (`app/schemas/com.sabeel.steps.data.db.AppDatabase/2.json`, identity hash `457d96f3d646b8b49e6481015a89a024`); the release APK was installed over the previous release install and the old 19 Nov rows (40 steps) still render in History/Insights after the upgrade (ui-08, ui-09) | VERIFIED (emulator upgrade) |
| The estimate is marked at the number and in each surface; the Health Connect source is described as synced, not as a sensor | README.md; strings.xml / values-ar/strings.xml; Home, History, Diagnostics screens | Home note "Estimated from the phone's motion…"; History daily/saved-walk estimate badges; Diagnostics "(estimated)" status and source; localization parity test; emulator EN and AR dumps (ui-01, ui-05, ui-06, ui-07), plus an estimated walk rendered live → summary → History row → saved-walk dialog (ui-10…ui-13). The HC "synced" source branch was not rendered (no HC data on the image) | VERIFIED (unit + emulator walk/EN/AR estimate); HC synced render unexercised |
| The new permission is disclosed for Play | docs/legal/play-compliance-checklist.md; docs/release/PLAY-SUBMISSION-CHECKLIST.md §4.7, §4.8, §4.12, §4.13; docs/legal/privacy-policy.md v1.1 | Documentation updated; the Play Console Data safety / Health Connect declarations and the Health Connect data-access form are **not yet submitted** (owner action), and the privacy policy still needs to be filled and hosted | CAVEAT (owner action) |

**Superseded / amended earlier rows:**

- §7 row "No new permission or dependency was added for any tier" is superseded: this change adds
  `androidx.health.connect:connect-client:1.1.0` and `android.permission.health.READ_STEPS`. The
  merged release APK now has 11 permissions (the previous 10 plus `READ_STEPS`) and still has no
  INTERNET, AD_ID, ACCESS_NETWORK_STATE or background location.
- §1/§2 wording that v1 has "no … Health Connect" is superseded for v1.1: Health Connect is an
  optional read-only source, not an account or cloud feature; nothing is uploaded and the app still
  works with no Health Connect at all.
- §6.1 final artifacts and gates remain the dated 2026-11-19 snapshot; the measured values above
  supersede them. Historical review/legal evidence files keep their original values.
- The estimate counts toward the goal and streak but is always marked — the owner confirmed this as
  the intended default (2026-11-21 answers), matching §7's caveat.

**Still not verified for this addendum:**

- The Health Connect grant was not completed on the emulator (the "Allow" tap fell through to the
  gesture-navigation area and the screen was backed out), and no watch/app wrote steps to Health
  Connect, so the read path, the floor reconciliation, the synced notification line and the
  30-minute staleness fallback remain untested on a device; they need §5b HP1–HP7 of
  [HARDWARE-QA-CHECKLIST.md](../legal/HARDWARE-QA-CHECKLIST.md).
- The estimated walk above was driven by synthetic acceleration, so it proves the label plumbing,
  not accuracy; a walk counted by a real counter/detector and the GPS route sketch still need
  hardware. The v1.0 walks migrated with `estimated = 0`.
- CSV import of a legacy 5-column file is unit-tested, not exercised through the SAF picker on the
  emulator.
- The release grew by 178,140 B (APK) and 439,302 B (AAB) because of the Health Connect client after
  R8; this is recorded here rather than called free.


## 9. Addendum — step manifold and count resilience (v1.2, 2026-11-21)

This section extends the audit; sections 1–8 remain dated records. It covers the v1.2 manifold that
replaces the exclusive source ladder, the crash/boot/persistence recovery paths, the Health Connect
background-read permission, and the release built for this change. Council verdict and research:
[COUNCIL.md](../review/2026-11-21/step-resilience/COUNCIL.md),
[RESEARCH.md](../review/2026-11-21/step-resilience/RESEARCH.md); verification:
[EVIDENCE.md](../review/2026-11-21/step-resilience/EVIDENCE.md).

**Artifacts measured for this addendum (build-tools 36.0.0, JDK 21):**

- Debug APK 13,581,629 B, SHA-256 `117b51fd7d120500ebcbaf5781b0e62446c7da13eeff432eb2ae9014b89714dc`;
  release APK 1,864,306 B, SHA-256 `00088fdc172cfafe17bc7ab90d112250d30ea2bae5ca1ef3de95f5cdbe518de1`;
  AAB 3,992,650 B, SHA-256 `92d4e5da2bb2054b8156b88a2b8fee40335831e1ed0b0594f2aca526982f33ab`.
  Signed with the upload key (cert `7b12ad6baf07817e76a2539954f7b4bde2e13f6b33454824830102566b6152d`);
  `apksigner verify`, `zipalign -c -P 16` and `jarsigner -verify` are OK. **versionCode 2,
  versionName 1.1.0** (the bump that must precede the first upload).
- JVM tests: **127 in 10 suites, 0 failures/errors/skips**; `lintDebug` exit 0 (0 errors, 46
  warnings, 1 hint); `lintVitalRelease` passed.
- Emulator: install/onboarding/FGS, stationary, synthetic estimate walk, force-stop + reopen
  persistence, delete-all, and boot auto-start (evidence file above). No counter, no detector and
  no HC data on the image.

| Claim | Where it appears | Evidence | Status |
|---|---|---|---|
| All available sources accumulate in parallel into one fused day; no source is switched and no switch resets/re-bases the total | README.md "What it does"; docs/product/spec.md §3 rules 1, 6 | `StepPipeline.kt` (`fuse`, per-source ledgers, `total = max(total, confirmed, estimateToday)`, `confirmed = max(confirmed, counterToday, detectorToday, hcToday)`); `StepPipelineTest` fusion/parallel-source cases | VERIFIED (unit level) |
| A reboot/sensor reset (raw falls by more than 1) is rebased and adds nothing; a stale out-of-order higher raw is still ignored | README.md; docs/product/spec.md §3 rule 7 | Negative-delta check runs before the monotonic timestamp guard in `StepPipeline.onCounter`; reboot and out-of-order tests | VERIFIED (unit level) |
| Count steps taken while the app process was dead are recovered from the cumulative counter (same boot), instead of being discarded as in v1.1 | README.md "counter catch-up"; hardware checklist A4/A8 | `StepPipeline.onCounter` elapsed-time catch-up + persisted `counterLastRaw`/`counterLastEventNanos`; catch-up tests; not exercisable on the emulator (no counter) | VERIFIED (unit level) / hardware pending |
| The "≈" marker is derived from the ledgers and cannot relabel measured steps; a v1.1 upgrade preserves a marker that existed only in the daily row | README.md; docs/product/spec.md §3 rules 6, 11 | `StepState.estimated = total − confirmed`; `preserveLegacyMarker` + `isManifoldSchema` in `StepsRepository`; 5 marker/migration tests plus the legacy migration mapping test | VERIFIED (unit level) |
| The estimate is only a gap-filler: its baseline is the fused total at the first estimated step, so it can never add to what a measured/synced source already confirmed; a late HC sync shrinks the estimate portion | README.md; docs/product/spec.md §3 rules 4, 6 | `StepPipeline.onEstimate` baseline rule; estimate-does-not-inflate tests; legacy marker test | VERIFIED (unit level) |
| A Health Connect total is an absolute floor: never added, never lowered; repeated/lower reads are no-ops; polling is bounded (first within ~3 s, 180 s normal / 60 s while measured silent, 8 s timeout) | README.md; docs/product/spec.md §3 rule 5 | `onExternalTotal`; `StepCounterService` HC constants and `HealthConnect.readToday` `withTimeoutOrNull(8 s)`; HC floor tests; emulator has no HC data | VERIFIED (unit/code level) / device pending |
| Detector events are sanity-checked (value 1..10, at most 5 events/s) so noisy vendors cannot inflate a day, while real steps are still accumulated | README.md; docs/product/spec.md §3 rule 3 | `StepPipeline.onDetector` constants `MAX_DETECTOR_EVENT`, `MIN_DETECTOR_EVENT_GAP_NANOS`; sanity tests | VERIFIED (unit level) |
| The writing path is crash-tolerant: a failed seed still starts the writer loops; callback/lifecycle/poll failures are contained per concern; a storage write failure is shown in Diagnostics instead of being swallowed; the final write is bounded | README.md; docs/product/spec.md §3 rule 12 | `StepCounterService` seed fallback, per-concern `runCatching`, `onWriteFailure`/`StatusStore.markStorageFault`, `writeMutex` + bounded `flushPendingOnDestroy`; `DiagnosticsScreen` "Saving" row (emulator: "Working"); audit-driven changes are in the same commit | VERIFIED (code + emulator row) / fault injection not performed |
| Shared state is safe across the sensor and writer threads (watchdog, cadence, status, writer) | — (internal, not a user claim) | `SilenceWatchdog` methods synchronized + `@Volatile` flags; `StepCadence` methods `@Synchronized`; service status methods `@Synchronized`; `writeMutex`; no deadlock path between the two lock sets (audit) | VERIFIED (code) / no stress test |
| The app recovers on boot and can post at most one "counting paused" notification when the service is not running but counting is allowed | docs/product/spec.md §4; plan §4 (no README claim) | `BootReceiver` + `StepCounterService.ensureStarted`; emulator E8 shows `Background started FGS: Allowed … BOOT_COMPLETED` and a foreground service after reboot; `ReconcileWorker` recovery nudge is code/unit-level only | VERIFIED (boot, emulator); paused nudge untested on device |
| "The app will never fail to count steps" (the original request) | — (not a claim we make) | The honest claim is: the day total survives every legal restart and every failure we observed, with documented gaps (detector-only steps while the process was dead; force-stop/OEM kill until the next legal start; no wakelock while the AP is suspended) | NOT CLAIMED — the absolute wording is deliberately softened |
| No INTERNET permission or network dependency was added; no ads/trackers | README.md; privacy policy | Release APK permission dump: no INTERNET/ACCESS_NETWORK_STATE/AD_ID/ACCESS_BACKGROUND_LOCATION; dependencies unchanged | VERIFIED |
| The new background Health Connect permission is declared only when the device feature supports it, and widening Play scope is disclosed | docs/legal/play-compliance-checklist.md; docs/legal/BLOCKERS.md; privacy policy | Manifest declares `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` (12 permissions now); `HealthConnect.requiredPermissions` adds it only when `FEATURE_READ_HEALTH_DATA_IN_BACKGROUND` is available; the Play Console/Health Connect forms and the hosted policy are **owner actions, not done** | VERIFIED (code) / CAVEAT (owner action) |
| Version is bumped before upload | — | `versionCode=2`, `versionName=1.1.0` in the built APK/AAB | VERIFIED |

**Superseded / amended earlier rows:**

- §7 "Ladder: counter → detector → Health Connect → estimate, chosen from SensorManager availability"
  is superseded: in v1.2 the counter and detector run in parallel whenever present and HC is a floor,
  not a chosen tier. The §8 row "Health Connect is an optional extra tier, tried only when the phone
  has no counter and no detector" is superseded as a *counting* rule (a granted HC total is applied in
  parallel and never added); the *opt-in offer* is still limited to phones reporting neither counter
  nor detector (spec rule 5), so HC on a sensor phone only happens if the grant already exists.
- §7 rows "every source switch rebases…", "step source policy chooses…", "silence watchdog switches
  tier", "fallback count" are superseded by the manifold rows above; `StepSourcePolicy` and
  `StepSourceHealth` were deleted with their tests, and "Fallbacks today" now counts display-source
  changes rather than ladder switches.
- §8 "first poll 5 s, then every 2 min" is superseded by "first within ~3 s, 180 s normal / 60 s
  while measured silent, 8 s timeout".
- §6.1/§7/§8 artifact values are historical snapshots; the §9 artifacts supersede them.

**Still not verified for this addendum:**

- The **physical Step Zero** falsification test has not run. Until it does, the fused counter+detector
  tolerance (2% total, detector 90–110% of counter) is a design target, not a measurement. If it
  fails, the detector is demoted to liveness-only.
- Counter catch-up after process death, reboot rebase and detector-only behaviour need a real step
  sensor; they are unit-tested only (emulator has neither sensor).
- Health Connect read/floor/background permission are code- and unit-tested only: the emulator image had no Health Connect step data, and no real watch/phone has written steps to Health Connect in front of this repository's tests.

## 10. Addendum — counting control, boot evidence and custom sessions (v1.3, 2026-11-22)

This section extends the audit; sections 1–9 remain dated records. It covers the v1.3 change set: the persisted counting authority and ring/pill switch, the boot-attempt/recovery path, custom walk/run sessions as a projection of the global day count, saved places/presets, the Room v3 schema, and the new Diagnostics surfaces.

**Evidence state for this addendum (read this before using anything below).**

- **Built and tested on 2026-11-23 (updated after task-20).** The v1.3 tree ran `testDebugUnitTest` (205 tests / 18 suites / 0 failures / 0 skipped), `lintDebug` (0 errors, 80 warnings, 2 hints) and `assembleDebug` + `assembleRelease` + `bundleRelease` (all exit 0). Runtime flows were exercised on the debug build on an emulator; physical-device evidence is still absent.
- **Tests run:** 205 `@Test` methods across 18 test classes (new: `SessionProjectionTest`, `SessionGoalTest`, `SessionTypesTest`, `PlaceMathTest`, `SessionSchemaSqlTest`, `SessionExportTest`, `SessionExportIoTest`, `StepPipelinePauseResumeTest`), all green in the final-tree run.
- **Artifacts measured:** release APK 2,138,010 B SHA-256 `487ee9c9…7a4dfe5d`, release AAB 4,608,495 B SHA-256 `ffde823d…a4a9fc4e`, versionCode 3 / versionName 1.3.0; full table in `README.md`; copies in `dist/`.
- The v1.3 tree has **versionCode 3 / versionName 1.3.0** (`app/build.gradle.kts`); the last signed artifacts in `dist/` are the older **versionCode 2 / versionName 1.1.0** build and are not v1.3 evidence.
- Statuses used: **VERIFIED (code read)** — behaviour follows from code inspected here; **UNIT-TESTED (not run)** — a unit test exists but has not been executed for this audit; **NOT VERIFIED** — no evidence; **CAVEAT** — true only with a stated condition; **OWNER** — not fixable in code.

| Claim | Where it appears | Evidence (code, this tree) | Status |
|---|---|---|---|
| There is one persisted counting authority; the Home ring, the status pill and the Settings switch all read/write the same value | README; NOTES-1.3; Home/Settings copy | `CountingStore` (`sabeel_counting` prefs, state flow); `CountingController.setEnabled/toggle`; `HomeScreen` passes counting-enabled state and toggle callback to ring/pill; `SettingsScreen` mirrors it | VERIFIED (code read) |
| The user's off choice survives reboot and app restart; the service never self-enables, and a boot re-arm refuses when the persisted intent is off | NOTES-1.3; README; PRIVACY-POLICY §2/§3 | `CountingStore.setEnabled` persists first; `CountingController.onBoot` returns DISABLED when off and never calls the service; app-open sync stops the service when off | VERIFIED (code read) |
| A toggle is write-before-act, serialized through a mutex, idempotent, and never throws into the UI; a refused start is reported, not crashed | Internal guarantee supporting the counting copy | `CountingController`: persisted flag written before dispatch, actions under `actionMutex`, `reconcile()` re-reads the flag, entry points wrapped in try/catch | VERIFIED (code read); no unit test for this class |
| The ring behaves as a switch for accessibility (switch role, on/off state description, single tap target) | README; UI acceptance | `ProgressRing`: `toggleable(role = Role.Switch)` plus on/off state descriptions; `HomeScreen` supplies localized strings and haptic wrapper | VERIFIED (code read); TalkBack not exercised |
| Turning counting off stops the counting service and its ongoing notification; turning it on starts it | NOTES-1.3; PRIVACY-POLICY | `CountingController.reconcile()` calls ensureStarted/requestStop on the step service; stop path cancels the FGS notification | VERIFIED (code read) / device NOT VERIFIED |
| At boot the app records the attempt (flags, result, alive-after-check) before trying to start, retries once, and reschedules reminders/reconciliation | NOTES-1.3; Diagnostics copy | `BootReceiver` records a pending `BootAttempt` (action, enabled, onboarded, activity permission, sensor, Health Connect), calls `onBoot`, self-checks after 2.5 s, retries once, updates the record; reschedules reminders and reconciliation; enqueues a unique boot health worker | VERIFIED (code read); boot behaviour NOT VERIFIED on v1.3 or on a physical device |
| If counting should be on but is still not alive after the boot check, the app may post exactly one recovery notification per boot, and only when enabled and onboarded | NOTES-1.3; FGS/boot narrative | `BootHealthWorker`: never starts an FGS; at most two attempts about a minute apart; posts one recovery notification; `Notifications` dedupes it by boot epoch | VERIFIED (code read); notification delivery NOT VERIFIED |
| Diagnostics shows the last ten boot attempts, the counting state and a restart action; the shared diagnostics text contains no location or place data | NOTES-1.3; README; PRIVACY-POLICY | `DiagnosticsScreen` takes the last ten boot attempts from `StatusStore.listBootAttempts`; share text appends a no-location/no-place footer; restart control present | VERIFIED (code read); share sheet NOT VERIFIED on device |
| A session never owns a second counter: session steps are a projection of the global day total, and the current date's session steps can never exceed the global day total | NOTES-1.3; README; PRIVACY-POLICY | `SessionProjection`: baseline captured at start, update = max(0, day total minus baseline), total = closed legs plus current date; the projection never writes the daily row; the service persists the session row separately | VERIFIED (code read); UNIT-TESTED (not run): `SessionProjectionTest` |
| A session that crosses local midnight is split into legs, the previous day is closed with its observed steps, and the new day starts from a fresh baseline (no double count, no negative) | NOTES-1.3 (parts that cross midnight) | `SessionProjection.update` rollover branch (closing leg, baseline reset); `WalkLeg` entity and migration table | VERIFIED (code read); UNIT-TESTED (not run) |
| Pausing a session freezes its own elapsed time and steps; resuming does not back-fill the paused period into the session (while the global day counter keeps running) | NOTES-1.3; TEST-XIAOMI §H | `WalkTracker.Live` pause fields plus `WalkTrackingService` pause/resume bookkeeping (wall clock minus paused time) keeps the projection baseline; `StepPipelinePauseResumeTest` covers the pipeline rebase | VERIFIED (code read); UNIT-TESTED (not run); device NOT VERIFIED |
| Only one session can be active; starting another is blocked rather than silently replacing the running one | NOTES-1.3; plan | `WalkTrackingService` single-service and single-checkpoint model with a running guard; `WalkScreen` start guard | VERIFIED (code read); UI edge NOT VERIFIED |
| Goal types are NONE/TIME/STEPS/DISTANCE/PLACE; measured goals need a positive target; PLACE arrival is binary within the place radius; reaching is latched by the caller so the one-shot finish/notification happens once; auto-stop at goal is opt-in | NOTES-1.3; README; session UI strings | `SessionGoal.evaluate/isValidTarget`; `WalkTracker.Live` goal-reached latch; `SessionConfig.autoStop`; service goal evaluation and auto-stop path; `PlaceMath` radius check | VERIFIED (code read); UNIT-TESTED (not run): `SessionGoalTest`, `PlaceMathTest`; device NOT VERIFIED |
| Saved places are local data only: label plus latitude/longitude plus radius, no address search, no reverse geocoding, no map tiles, no network; deleting app data removes them | NOTES-1.3; PRIVACY-POLICY §2/§6 | `Place` entity and DAO; `WalkRepository.upsertPlace/deletePlace`; `PlaceMath`; no geocoder/map/network code found; manifest has no INTERNET permission | VERIFIED (code read); CAVEAT: no place-creation UI was found in the tree, so the place goal is not user-reachable yet — see gaps |
| Saved session setups (presets) are local, applied to the setup screen, and saved on the phone only | NOTES-1.3; session setup strings | `SessionPreset` entity; `WalkRepository.presetsFlow/upsertPreset`; `AppViewModels.savePreset`; `SessionSetupScreen` preset chips and save action | VERIFIED (code read); device NOT VERIFIED |
| Starting a session with counting off does not fake progress: the session cannot advance, the live screen shows a counting-is-off card, and turning counting on is offered in place | NOTES-1.3; session strings | `WalkScreen` live section reads the counting-off flag and shows the status card plus turn-on callback; service sets the flag while counting is disabled | VERIFIED (code read); device NOT VERIFIED |
| Room v3 adds places/presets/legs and a migration without a destructive fallback; a v1.2 database upgrades in place | README; plan; TEST-XIAOMI §A | Database version 3 with five entities/DAOs; non-destructive migration defined; exported schema JSON for version 3 present; no fallback-to-destructive-migration call found | VERIFIED (code read); UNIT-TESTED (not run): `SessionSchemaSqlTest`; on-device migration NOT VERIFIED |
| Session export can produce a CSV of sessions and a GPX of one session, pure formatting, no INTERNET; SAF writes only where the user picks | NOTES-1.3; PRIVACY-POLICY §6b | `SessionExport.buildSessionCsv/buildSessionGpx`; `SessionExportIo` writes via the system document picker URI; History screen has "Export all sessions (CSV)" and per-session "Export GPX" | UNIT-TESTED (205 green) + EMULATOR-EXERCISED (CSV 18-column row and valid GPX written on emulator-5564); device NOT VERIFIED |
| v1.3 adds no new permissions, no INTERNET, no background location and no new SDKs | README; PLAY-1.3 §7/§9; PRIVACY-POLICY | Source manifest: unchanged permission set (activity recognition, Health Connect read and optional background read, notifications, foreground service plus health/location types, fine/coarse location, boot completed); no INTERNET; no background-location permission; sessions run in the existing foreground service | VERIFIED: merged release manifest + APK audit 2026-11-23 (no INTERNET/background-location/battery-exemption; boot receiver only BOOT_COMPLETED + MY_PACKAGE_REPLACED) |
| The version is bumped before any upload | README; CHANGELOG | versionCode 3 and versionName 1.3.0 in the build file | VERIFIED (gradle + release artifact `aapt2 dump badging`) |
| EN/AR string parity holds for the v1.3 strings (399/399 keys; 30/30 sensor keys) | README; NOTES-1.3 | key-set comparison of the English and Arabic string files, both main and sensor | VERIFIED (static check) / CAVEAT: no native Arabic review |
| The app cannot restart itself after a force-stop, and OEM battery managers can still stop counting; the app says so instead of claiming always-counting | NOTES-1.3; PLAY-1.3 §5.4; README limits | No auto-open/auto-restart code; counting is restarted only from legal entry points (app open, boot or package replace, user toggle); force-stop is an OS rule; the recovery notification is the fallback | VERIFIED (code read/design) / platform behaviour not re-tested on this phone for v1.3 |
| The boot attempt does not start the location service or record background location | PLAY-1.3 §5/§8; PRIVACY-POLICY | Boot receiver only reconciles counting and reminders; the location foreground service is started from the session start flow only; manifest has no background-location permission | VERIFIED (code read) / merged manifest and device NOT VERIFIED |
| Sabeel has not been approved by Google Play, and no step-accuracy claim beyond the evidence is made | All release docs | No submission exists; this audit adds no accuracy claim; the estimate and fused-source limits from §8/§9 still apply | VERIFIED (state) |

### 10.1 Gaps that block or weaken the v1.3 claims (as of this addendum)

1. **Only emulator evidence exists.** Build/tests/lint are green (2026-11-23) and the debug build was exercised on emulator-5564, but no physical Redmi Note 7 run has happened; every "device" status stays NOT VERIFIED until TEST-XIAOMI is executed.
2. **Place goal is user-reachable now, but only by typed coordinates.** Saved places are created/deleted from the session setup screen (label, latitude, longitude, radius 25–1000 m, optional external `geo:` Maps link); there is **no embedded map or address search** (no INTERNET), so listing copy must not imply one.
3. **Session export is user-reachable now.** History exposes "Export all sessions (CSV)" and per-session "Export GPX" through the system document picker; both were exercised on the emulator. The conditional wording in NOTES-1.3 has been replaced with the verified flow.
4. **No physical-device evidence for v1.3**: boot after reboot, MIUI autostart/battery behaviour, recovery notification, session pause/resume, midnight legs, counting-off survival, Room v2 to v3 upgrade. The Redmi test plan (TEST-XIAOMI) is the intended evidence, and it has not been run.
5. **Merged manifest audited for v1.3** (task-20, 2026-11-23): the release APK/permissions and merged manifest match the claim rows above; the remaining physical-device limit is that MIUI/OEM behaviour cannot be proven from a manifest.
6. **Counting controller, boot receiver and boot health worker have no unit tests** in this tree (only source-read verification). A regression there is invisible to the JVM suite.
7. **Arabic**: string parity holds, but no native review; the release copy must keep the native-review precondition.
8. **Store prerequisites not met**: Privacy policy URL not hosted (placeholders unfilled), no IARC rating issued, no covers/screenshots for FGS demo video, no demo video, no Play Console declarations submitted, and the personal-account 12-tester/14-day gate (if the account is personal) is an owner step.
9. **Play approval was never claimed and must not be claimed** anywhere in the repo.

### 10.2 Bottom line

The v1.3 feature set is defensible at the code level for counting control, boot evidence, projection-based sessions, local places/presets and the Room v3 schema. It is **not evidence-complete**: no build, no test run, no device run, two features not user-reachable (places, session export), and the store declarations depend on the build that does not exist yet. Treat v1.3 as **release-preparation, not release-ready**.
