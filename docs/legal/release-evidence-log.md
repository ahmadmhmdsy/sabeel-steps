# Release evidence log — Sabeel v1.0

**Status: FINAL STATIC EVIDENCE RECEIVED · RUNTIME PARTIAL (NOT VERIFIED — EMULATOR-ATTEMPTED) · RELEASE-ARTIFACT GATE OPEN.**
Independent verification batch: **C:/dl/legal-evidence/final-20261117-000050** (collected 2026-11-17 00:00:50–00:34 +03:00). Durable copies live in **[docs/legal/evidence/](evidence/)**; the APK copy is dist/Sabeel-1.0-debug.apk.

**Artifact under test:** app/build/outputs/apk/debug/app-debug.apk · 11,488,512 bytes · mtime 2026-11-16T23:51:37+03:00 · SHA-256 324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E. buildsmith did **not** rebuild the APK; the hash matches the platform-engineer candidate and the dist/ copy exactly (SHA256SUMS.txt).

**Build identity:** Gradle wrapper 8.11.1, JDK 21.0.13 LTS, build-tools 36.0.0; build-file aggregate sha256 c51264f96e512c4f365527cb516ddf3d02e0af53ea2773fad5a00a3123a008a5; app/src/main source-tree sha256 5695a188c482aa9cf5899589d50db1615d8826793e20af26b2de096563d64dd9 (59 .kt+.xml files). No git repository; identity is the hash pair above.

---

## 1. Provenance

| Stream | Scope | Result | Where |
|---|---|---|---|
| Platform-engineer green run | assembleDebug + testDebugUnitTest; aapt2 facts | 41/41 unit tests; build SUCCESSFUL (self-report) | static facts superseded by the batch below; test evidence not independently reproduced |
| buildsmith static batch | C1–C10 static on the existing APK + C12; no rebuild | **PASS=33 FAIL=0 FINDING=1; OPEN=1; stale files present: none** | C:/dl/legal-evidence/final-20261117-000050/EVIDENCE.txt; copy docs/legal/evidence/EVIDENCE-20261117-000050.txt |
| buildsmith AVD smoke | API 36 aosp_atd x86_64, headless, WHPX; one focused retry | Environment steps succeeded; all runtime rows **NOT VERIFIED — EMULATOR-ATTEMPTED**; deeper flows untestable; demo video OPEN | C:/dl/legal-evidence/final-20261117-000050/AVD/; copies docs/legal/evidence/AVD-*.txt |

---

## 2. Evidence entries

| ID | Check | Measurement | Status |
|---|---|---|---|
| EV-01 | Artifact identity | APK sha256 324690…9B9E, 11,488,512 B, mtime 2026-11-16T23:51:37; dist/ copy hash identical | **PASS** |
| EV-02 | C2 badging | package com.sabeel.steps; targetSdk 36; minSdk 26 (independent batch). Label Sabeel / icon mipmap-anydpi-v26/ic_launcher.xml are platform-engineer self-report, not in the batch excerpt | **PASS** (package/target/min); label/icon **PROVISIONAL** |
| EV-03 | C4 permissions | 0 INTERNET; 0 ACCESS_NETWORK_STATE; expected 8 present (ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FGS_HEALTH, FGS_LOCATION, WAKE_LOCK, RECEIVE_BOOT_COMPLETED); ACCESS_COARSE_LOCATION also present in the runtime permission dump; app DYNAMIC_RECEIVER permission | **PASS** |
| EV-04 | C5 manifest attrs | allowBackup=false; foregroundServiceType present (health; location\|health) | **PASS** |
| EV-05 | C6 debug merged manifest | allowBackup=false; no INTERNET; no ACCESS_NETWORK_STATE; targetSdk 36; merged-manifest sha256 D904BCE02B173EAC0968D4DAB0CA97FF7F261AC73D7D2E80847A593E6BB9D75A | **PASS (debug)** |
| EV-06 | C6 release merged manifest | release build never run: merged manifests total=1 debug=1 release=0 | **FINDING — OPEN** (release-artifact gate) |
| EV-07 | C8 dependency audit | releaseRuntimeClasspath resolves; no ad/analytics/crashlytics/firebase/play-services dependency | **PASS** |
| EV-08 | C9 16 KB | zipalign -c -P 16 successful; every bundled .so PT_LOAD p_align=16384 (libandroidx.graphics.path, libdatastore_shared_counter) | **PASS (debug APK)** |
| EV-09 | C10-S static Data-safety basis | no NetworkType/setRequiredNetworkType/NetworkRequest/Constraints.Builder in Kotlin; WorkManager inventory 18 refs; boot-reschedule 2; reminder 2; no INTERNET in APK; no transmitting dependency | **PASS** |
| EV-10 | C12 signing | NOT VERIFIED BY DESIGN; no signingConfig/storeFile/keyAlias; release artifacts unsigned by design; name-only keystore scan = none present | **NOT VERIFIED BY DESIGN** (expected) |
| EV-11 | Folder hygiene | stale files present: none; 23:15 pre-change files never copied in | **PASS** |
| EV-12 | AVD environment | BOOT 132 s, INSTALL PASS, LAUNCH PASS (COLD, TotalTime 2889 ms), 4 grants PASS, REBOOT 52 s; FATAL scan 0 hits; BootReceiver + ReconcileWorker reschedule lines present after reboot | **INSTRUMENTATION OK — classification stays NOT VERIFIED — EMULATOR-ATTEMPTED** |
| EV-13 | Onboarding content | uiautomator saw exact Sabeel step 1/4 disclosure: "No account. No internet. No ads, no trackers. Nothing is uploaded, ever."; English + العربية + Next | **CONFIRMED (uiautomator text, not visual)** |
| EV-14 | FGS / notification / walk flow | not reachable: flow never advanced past onboarding step 1; service_present=False, isForeground=False, fgs_type_lines=0; notification package lines=0 | **NOT VERIFIED — EMULATOR-ATTEMPTED / OPEN** |
| EV-15 | FGS demo video | fgs-demo.mp4 = **0 bytes**; screenrecord cannot render headless surface; explicit escalation line | **OPEN** |
| EV-16 | Screenshots | four files byte-identical black frames, sha256 C35BACDB…F514, 15,580 B each, 1080×2400 | **NOT USABLE — environment limitation** |
| EV-17 | Step counter / accuracy | aosp_atd has no real TYPE_STEP_COUNTER (matches=0); step and location accuracy NOT TESTABLE-IN-EMULATOR | **NOT VERIFIED — NOT TESTABLE** |
| EV-18 | Unit tests | 41/41 (CsvCodec/StepMath/StepPipeline/StepCadence) is the platform-engineer self-report; batch did not run tests | **PROVISIONAL — not independently reproduced** (not a Play gate) |
| EV-19 | Artifact staleness warning | batch warned APK predates newest input; newest non-build input = app/src/test/.../StepPipelineTest.kt (23:55:20) plus wrapper/docs. **No app/src/main source or manifest file changed after the APK** (23:51:37), so packaged runtime content is current; warning is benign | **CLOSED — benign** (documented) |
| EV-20 | Release manifest gate | no release APK/AAB and no release merged manifest → release-artifact gate stays OPEN; C2/C4/C5/C9 measured on debug only | **OPEN** |

---

## 3. Static batch summary (verbatim key lines)

    artifact app\build\outputs\apk\debug\app-debug.apk | bytes=11488512 | sha256=324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E | mtime=2026-11-16T23:51:37
    WARNING: app\build\outputs\apk\debug\app-debug.apk predates newest project input - artifact may be STALE for this tree
    FINDING: no release APK/AAB present - C2/C4/C5/C9 are measured on the debug build only; release-artifact gate OPEN (owner/Lead to run bundleRelease or provide the release artifact)
    PASS: package is com.sabeel.steps
    PASS: targetSdkVersion 36
    PASS: minSdkVersion 26
    PASS: ABSENT (required): android.permission.INTERNET
    PASS: ABSENT (required): android.permission.ACCESS_NETWORK_STATE
    PASS: PRESENT (expected): all 8 expected permissions
    PASS: allowBackup=false
    PASS: foregroundServiceType present on FGS declarations
    PASS: zipalign -c -P 16 verification successful
    PASS: ELF PT_LOAD p_align=16384 for all bundled .so files
    C6 merged manifests: total=1 debug=1 release=0
    C6: release merged manifest NOT present (release build not run yet) - release manifest gate OPEN
    PASS: no ad/analytics/crashlytics/firebase/play-services dependency in release runtime classpath
    PASS: no WorkManager network constraints / network-request usage in Kotlin sources
    C12: NOT VERIFIED BY DESIGN
    C12: keystore material scan (name-only, read-only) = none present
    static batch summary = PASS=33 FAIL=0 FINDING=1
    OPEN items (FAIL+FINDING for owner/legal triage) = 1
    stale files present: none

---

## 4. Runtime results (all rows NOT VERIFIED — EMULATOR-ATTEMPTED)

| Area | Result | Note |
|---|---|---|
| Boot / install / cold launch | instrumented OK (132 s / Success / 2889 ms) | environment steps, not user-flow PASS |
| Onboarding/startup disclosure | uiautomator text confirmed | visual proof unavailable |
| Permissions granted (4) | adb grants OK | grant is setup, not behaviour evidence |
| Reboot persistence | reboot 52 s; BootReceiver + ReconcileWorker rescheduled in logcat | background restore observed in logs |
| FGS foreground service | not observed | flow never advanced past onboarding |
| Notification behaviour / denial paths | not observed | screenshots black; untestable here |
| Reminder scheduling | not observed | UI-only flow; raw dumps attached |
| Walk flow / route / CSV export / delete-all | not exercised | untestable past onboarding |
| Step sensor / step accuracy | absent sensor | NOT TESTABLE-IN-EMULATOR |
| Location accuracy / route accuracy | no real GNSS (single mock fix) | NOT TESTABLE-IN-EMULATOR |
| FGS demo video | 0 bytes | OPEN — escalation recorded |
| FATAL scan | 0 hits | no crash observed during attempted flows |

---

## 5. Environment findings (not product findings)

1. **Black screenshots** — headless -no-window + swiftshader produced four byte-identical black frames; no visual evidence. UI content evidence is uiautomator XML only.
2. **Foreign app contamination** — com.sigha.converter installed/foregrounded by a concurrent process; rows whose reachable_screen shows "Sigha" (C7-ACTIVITY-DENIED, C10-R-REMINDER-CREATION, C10-R-LOGCAT and the three sensor/accuracy rows) must not be read as Sabeel UI; their adb-derived observed values stand. Boot/install/launch/onboarding/reboot rows are unaffected (Sabeel context explicit).
3. **Empty FGS demo video** — screenrecord produced a 0-byte file; escalation to windowed emulator + host capture or google_apis;android-36 image is recorded; video remains OPEN.
4. **No step sensor** — aosp_atd has no TYPE_STEP_COUNTER; hardware step and location accuracy untestable here.
5. **Staleness warning** — closed as benign: newer inputs are test-only/wrapper/docs; no packaged source or manifest change after the APK (EV-19).

---

## 6. Findings / discrepancy log

| # | Finding | Class | Action |
|---|---|---|---|
| F1 | No release APK/AAB and no release merged manifest; C2/C4/C5/C9 measured on debug only | RELEASE GATE OPEN | Owner/Lead: build unsigned release AAB (bundleRelease/assembleRelease) and rerun C2/C4/C5/C6/C9 before submission; owner later signs |
| F2 | C7 FGS demo video empty (0 bytes) | OPEN — Play FGS declaration | Windowed emulator + host capture (Game Bar/ffmpeg gdigrab) or google_apis;android-36; capture real walk flow; never fake |
| F3 | Screenshots black; visual evidence untestable | ENVIRONMENT | Visual evidence later on windowed emulator/device; Play listing screenshots must be real captures |
| F4 | Foreign app contaminated some UI-context rows | ENVIRONMENT | Rows excluded as Sabeel UI evidence; no product finding; rerun in a clean emulator |
| F5 | FGS/notification/reminder/walk flows never reached | OPEN — runtime coverage | UI-automation driver that advances onboarding by uiautomator bounds (not keyword taps); then rerun |
| F6 | Unit tests 41/41 not independently reproduced | PROVISIONAL | Optional: buildsmith runs testDebugUnitTest if independent test evidence is wanted |
| F7 | Artifact staleness warning | CLOSED — benign | Documented EV-19; no rebuild needed for packaged-content purposes |
| F8 | C12 signing NOT VERIFIED BY DESIGN | EXPECTED | Owner creates upload keystore outside repo; C12 re-run on signed artifact |

---

## 7. Sufficiency statement

The folder is **enough** to close the independent static gate: artifact identity reproduced, C2/C4/C5/C6(debug)/C8/C9/C10-S PASS, C12 by design, folder hygiene clean. It is **not enough** to support Play submission on its own and does **not** close: the release-artifact gate (no release AAB/merged manifest); FGS declaration + demo video; visual evidence (black screenshots); real runtime coverage of FGS, notifications, reminder scheduling and the walk flow; hardware step/GPS accuracy; a signed release artifact with C12 re-run. Those are recorded as OPEN above, not as failures of the app.

---

## 8. Addendum — v1.1 Health Connect / provenance verification (2026-11-21 22:44 +03:00)

This is a **self-verification** by the building agent, not a new independent batch. Sections 1–7
remain the dated 2026-11-17 record.

| Item | Measurement | Status |
|---|---|---|
| Release artifacts | `assembleRelease` + `bundleRelease` BUILD SUCCESSFUL 2026-11-21 22:44. APK 1,861,838 B / `C5BD200DA95D3FD1F1C417FB6C11FBE8E06700CB6A75C07E5CA793F1A1DC4958`; AAB 3,977,681 B / `7A2FD53AA12603127403D852A35C168325A9AD4FA7F5843CD9782819EA06BC77`; debug APK 15,188,660 B / `72D97C3B…B238` | **PASS (self-measured)** |
| Signing | Upload cert SHA-256 `7b12ad…6152d` (same key as v1.0); `apksigner verify` OK (v2); `jarsigner -verify` on the AAB "jar verified"; `zipalign -c -P 16 4` exit 0 | **PASS (self-measured)** |
| Permissions (release APK) | 11 uses-permissions: the v1.0 ten plus `android.permission.health.READ_STEPS`. No INTERNET, ACCESS_NETWORK_STATE, AD_ID or ACCESS_BACKGROUND_LOCATION | **PASS** |
| Gates | `testDebugUnitTest` 121/121 in 11 suites (0 failures/errors/skips); `assembleDebug`; `lintDebug` exit 0 (0 errors, 44 warnings, 1 hint); `lintVitalRelease` passed inside the release build | **PASS** |
| Room upgrade | Release APK installed with `adb install -r` over the previous release-signed install (same signature, data kept); after launch the old 19 Nov rows (40 steps) still render in History/Insights; no crash; crash buffer empty | **PASS (emulator upgrade)** |
| Health Connect UI | Settings card gated on no hardware sensor; tapping Connect opens Health Connect's read-only permission screen for Sabeel with **Steps** as the only data type; declining leaves the app on the labelled estimate. Emulator pass in EN and AR; a synthetic-input estimated walk rendered live → summary → History badge → saved-walk dialog | **PASS (emulator, read-only contract + estimate walk)** |
| Health Connect data path | The grant was not completed on the emulator and no app/watch writes steps there, so synced reads, the floor reconciliation, the synced notification and the 30-minute fallback remain unexercised | **OPEN — DEVICE / OWNER** (HARDWARE-QA-CHECKLIST §5b) |
| Privacy / Play declarations | v1.1 docs updated (privacy policy, Play compliance checklist, submission checklist §4.7/4.8/4.12/4.13, BLOCKERS B8); console declarations and the Health Connect data-access form are not submitted, and the v1.1 HTML site still needs regeneration and hosting | **OPEN — OWNER** |

Evidence folder: [docs/review/2026-11-21/health-connect/](../review/2026-11-21/health-connect/EVIDENCE.md).
Claim-by-claim audit: [CLAIMS-AUDIT.md §8](../release/CLAIMS-AUDIT.md).


---

## 9. Addendum — v1.2 step manifold resilience (2026-11-21 23:5x +03:00)

This is a **self-verification** by the building agent, not a new independent batch. Sections 1–8
remain dated records; §9 measures the v1.2 build (manifold fusion + crash/boot recovery + optional
Health Connect background read), which supersedes the v1.1 artifacts in §8.

| Item | Measurement | Status |
|---|---|---|
| Release artifacts | `assembleRelease` + `bundleRelease` BUILD SUCCESSFUL 2026-11-21. APK 1,864,306 B / `00088FDC172CFAFE17BC7AB90D112250D30EA2BAE5CA1EF3DE95F5CDBE518DE1`; AAB 3,992,650 B / `92D4E5DA2BB2054B8156B88A2B8FEE40335831E1ED0B0594F2ACA526982F33AB`; debug APK 13,581,629 B / `117B51FD7D120500EBCBAF5781B0E62446C7DA13EEFF432EB2AE9014B89714DC`; copies + `SHA256SUMS.txt` in `dist/` | **PASS (self-measured)** |
| Version | `aapt2 badging`: versionCode **2**, versionName **1.1.0** (first bump; required before upload) | **PASS** |
| Signing | Upload cert SHA-256 `7b12ad…6152d` (same key as v1.0/v1.1); `apksigner verify` OK (v1/v2); `jarsigner -verify` on the AAB exit 0; `zipalign -c -P 16 4` exit 0 | **PASS (self-measured)** |
| Permissions (release APK) | 12 uses-permissions: the v1.1 eleven plus `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`. No INTERNET, ACCESS_NETWORK_STATE, AD_ID or ACCESS_BACKGROUND_LOCATION | **PASS** |
| Gates | `testDebugUnitTest` **127/127 in 10 suites** (0 failures/errors/skips); `assembleDebug` BUILD SUCCESSFUL; `lintDebug` exit 0 (0 errors, 46 warnings, 1 hint); `lintVitalRelease` passed inside the release build | **PASS** |
| Manifest | Merged debug manifest: `foregroundServiceType="health"`; BOOT_COMPLETED + MY_PACKAGE_REPLACED receivers; both Health Connect permissions; no INTERNET | **PASS** |
| Counting manifold (emulator) | Fresh install → onboarding/permissions → FGS foreground (`isForeground=true`, `types=0x100`, id 4101) → stationary total stays 0 → synthetic 1.8 Hz walk ≈55 s yields **56 estimated steps** labelled in Home, Diagnostics and the notification | **PASS (estimate path plumbing; no counter/detector on the image)** |
| Recovery (emulator) | After `am force-stop` and reopen the persisted 56 steps remain and the service restarts; a second synthetic walk adds to the restored total (Home distance 40 m → 54 m, ≈19 steps) instead of resetting it; second reboot (after onboarding) auto-starts the health FGS from `BOOT_COMPLETED` ("Background started FGS: Allowed"); crash buffer has no Sabeel entry throughout | **PASS (persistence/boot on emulator)** |
| Delete-all (emulator) | Confirming "Delete all data" returns onboarding step 1/4, stops the service (dumpsys empty), leaves no crash record | **PASS** |
| Counter/detector fusion, catch-up, reboot rebase, HC floor, storage fault, midnight | Not exercisable on this image (no step counter/detector, no HC step data, no disk-fault injection, no date change) — covered by unit tests and reviewed code only; the **physical Step Zero** test (hardware checklist §2) is still OPEN | **OPEN — HARDWARE / DEVICE** |
| Device-time limits | Any aggressive power manager/OEM kill can stop counting until the next legal start; detector-only steps while the process is dead are lost by platform design; the app does not use a wakelock, WorkManager counting or an alarm restart (council ruling P2/A7) | **DISCLOSED, NOT DEFECT** |
| Privacy / Play declarations | v1.2 docs updated: privacy policy (EN+AR background-read wording), Play compliance checklist §6.1/6.3/6.3b, submission checklist §4.12/4.13, release signoff G8, claims audit §9. Console forms, hosted policy regeneration and the health declaration are **still owner actions**; the Health Connect background permission widens the Play health-review scope | **OPEN — OWNER** |

Evidence folder: [docs/review/2026-11-21/step-resilience/EVIDENCE.md](../review/2026-11-21/step-resilience/EVIDENCE.md).
Research and council: [RESEARCH.md](../review/2026-11-21/step-resilience/RESEARCH.md),
[COUNCIL.md](../review/2026-11-21/step-resilience/COUNCIL.md).
Claim-by-claim audit: [CLAIMS-AUDIT.md §9](../release/CLAIMS-AUDIT.md).
