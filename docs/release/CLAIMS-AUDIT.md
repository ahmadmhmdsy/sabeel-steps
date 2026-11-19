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
