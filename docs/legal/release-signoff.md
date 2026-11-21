# Release sign-off — Sabeel v1.0 (one page)

**Status: EVIDENCE STAGED — NOT APPROVED FOR SUBMISSION.** Static gates below are measured on the
final signed artifacts of 2026-11-21 22:44 +03:00, rebuilt after the Health Connect + estimate
provenance change (v1.1), not on the earlier debug-only evidence. Runtime rows remain
**EMULATOR-ATTEMPTED**; no row below is a Play approval.

**Artifacts (built from this tree, signed locally with the upload key):**

| File | Size | SHA-256 |
|---|---|---|
| `dist/Sabeel-1.0-debug.apk` | 15,188,660 B | `72d97c3b3faf8889834afc5871c9a4ca8a09c232dae3571eea7913891156b238` |
| `dist/Sabeel-1.0-release.apk` | 1,861,838 B | `c5bd200da95d3fd1f1c417fb6c11fbe8e06700cb6a75c07e5ca793f1a1dc4958` |
| `dist/Sabeel-1.0-release.aab` | 3,977,681 B | `7a2fd53aa12603127403d852a35c168325a9ad4fa7f5843cd9782819ea06bc77` |

`dist/SHA256SUMS.txt` is regenerated from these files. Signer (release APK): upload cert
`CN=Sabeel Upload, OU=Mobile, O=Sabeel, L=Baghdad, ST=Baghdad, C=IQ`, SHA-256
`7B:12:AD:6B:FA:F0:78:17:E7:6A:25:39:95:4F:7B:4B:DE:2E:13:F6:B3:34:54:82:48:30:10:25:66:B6:15:2D`;
`apksigner verify`: **Verifies** (v2 scheme). Keystore material is local and gitignored.
Supporting gates on this tree: `testDebugUnitTest` 121 tests / 0 failures in 11 suites; `lintDebug`
exit 0 (0 errors, 44 warnings, 1 hint); emulator UI passes in EN and AR recorded in
[docs/review/2026-11-17/adoption-smoke/](../review/2026-11-17/adoption-smoke/EVIDENCE.md), for the
step-source ladder and labelled estimate [docs/review/2026-11-19/step-source-resilience/](../review/2026-11-19/step-source-resilience/EVIDENCE.md),
and for the Health Connect card, its read-only Steps permission screen and the v1→v2 upgrade
[docs/review/2026-11-21/health-connect/](../review/2026-11-21/health-connect/EVIDENCE.md).

---

## 1. Gate table

| # | Gate | Evidence | Status |
|---|---|---|---|
| G1 | Package / target API | release APK: com.sabeel.steps, targetSdk 36, minSdk 26 | **PASS** |
| G2 | Backup flag | release APK manifest: allowBackup=false | **PASS** |
| G3 | No INTERNET | release APK permissions: absent | **PASS** |
| G4 | No ACCESS_NETWORK_STATE | release APK permissions: absent | **PASS** |
| G5 | No transmitting SDKs | release runtime classpath free of ad/analytics/crashlytics/firebase/play-services | **PASS (static)** |
| G6 | 16 KB page sizes | `zipalign -c -P 16 4` exit 0 on release APK; 8 stored `.so` entries (androidx graphics path, datastore shared counter) are 16 KB-aligned and `extractNativeLibs=false` | **PASS (release APK)** |
| G7 | FGS declaration + demo video | service types health + location\|health static; Play declaration and demo video still missing | **OPEN** |
| G8 | User-facing permissions | release APK: ACTIVITY_RECOGNITION, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_HEALTH, FOREGROUND_SERVICE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, plus the optional read-only `android.permission.health.READ_STEPS` and the optional feature-gated `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`; no background location; no INTERNET, AD_ID or ACCESS_NETWORK_STATE | **PASS (static)** |
| G9 | Runtime QA | emulator install, onboarding, Home/Settings/History/Insights EN + AR, diagnostics, goal suggestion, rest day, walk denial path; 2026-11-19 source-ladder run: estimate tier from a sensor-less image, labelled Home/Diagnostics/notification/walk, ≈ 97 steps from injected walking, crash buffers empty. Emulator-only, synthetic seed for data UI; measured counter→detector ladder still needs physical devices. Release APK smoke on a wiped image: onboarding, labelled estimate (≈ 40), estimate notification, crash buffer empty. 2026-11-21 v1.1 pass: release APK upgraded over the previous release install (v1→v2 migration; old 19 Nov rows render), Settings Health Connect card, Health Connect's read-only Steps permission screen, EN + AR estimate labels, synthetic-input estimated walk rendered live/summary/History/dialog, crash buffer empty; HC grant and synced data not exercised | **OPEN — EMULATOR-ATTEMPTED** |
| G10 | Privacy policy live | policy draft + static site exist; placeholders, hosting, console/in-app links, native Arabic review pending | **OPEN — OWNER** |
| G11 | Data safety form | not submitted | **OPEN** |
| G12 | Health apps declaration | not submitted | **OPEN** |
| G13 | Content rating | not obtained | **OPEN** |
| G14 | Store listing | not written; real screenshots missing | **OPEN** |
| G15 | App access / ads / audience | not declared | **OPEN** |
| G16 | Name / package clearance | Sabeel chosen; trade-mark registry + applicationId clearance pending | **PARTIAL — OWNER** |
| G17 | Signing | upload keystore exists (local, gitignored), release APK/AAB signed and verified (v2); Play App Signing registration pending | **PARTIAL — OWNER** |
| G18 | Closed testing 12/14 | not started | **OWNER ACTION** |
| G19 | Counsel + scholar review | memo ready, reviews pending | **OPEN** |
| G20 | Clean-room statement | pending owner confirmation | **OPEN** |
| R1 | Release artifact | **CLOSED** — signed release APK + AAB exist, rebuilt 2026-11-21 after the Health Connect + provenance change; C2/C4/C5 measured on the release APK; manifests inspected | **PASS** |
| S1 | Unit tests (supporting, not a Play gate) | JUnit XML: 121/121, 0 failures, 0 errors, 0 skipped in 11 suites (2026-11-21 22:47) | **PASS** |

**Counts:** 8 PASS (G1–G6, G8, R1) · 1 PASS static elsewhere (S1) · 1 OPEN-EMULATOR (G9) · 8 OPEN
(G7, G10–G15, G19, G20) · 2 PARTIAL/OWNER (G16, G17) · 1 OWNER ACTION (G18). **0 final Play
approvals.**

---

## 2. Owner actions

| # | Action | Blocks |
|---|---|---|
| O1 | Enrol Play App Signing and register the upload certificate; keep the keystore SEALED and backed up | G17 |
| O2 | Fill privacy-policy placeholders (legal name EN/AR, monitored email, date), host only docs/privacy-site, link in console and in-app; native Arabic review | G10 |
| O3 | Name/package clearance: Iraqi trade-mark registry, WIPO, Play applicationId | G16 |
| O4 | Closed testing with 12 real testers for 14 continuous days (if the account requires it) | G18 |
| O5 | Play Console: Data safety (including the Health Connect read), Health declaration + Health Connect data-access form, FGS declaration + demo video, content rating, listing, app access/ads/audience | G7, G11–G15 |
| O6 | Capture real screenshots and the FGS demo video on a windowed emulator or a physical device; never use black frames | G14, part of G7 |
| O7 | Counsel review (Iraq + markets) and scholar review; accept the memo §7.1 gate | G19 |
| O8 | Confirm no team member worked on the reference apps; grep repo for target names/logos/comparisons | G20 |

---

## 3. Owner-facing readiness (≈120 words)

Sabeel v1.0 (com.sabeel.steps) is not ready to publish, but the build itself is complete. The signed
release APK (SHA-256 c5bd200…dc4958) and AAB (7a2fd53…a06bc77) were built from the committed tree and
verified; targetSdk 36, minSdk 26, allowBackup false, no INTERNET or ACCESS_NETWORK_STATE, the
expected health/location permissions plus the optional read-only Health Connect Steps permission,
declared foreground-service types and 16 KB alignment all hold on the release artifact, and 121 JVM
tests in 11 suites plus lint pass. Counting has a measured counter → detector ladder with a labelled
accelerometer estimate as the last resort, and v1.1 adds an optional read-only Health Connect floor
for phones with no step sensor; the estimate and Health Connect tiers were exercised on an emulator,
while the measured ladder, real Health Connect data and sensor accuracy still need physical phones.
The release grew by ~178 KB (APK) for the Health Connect client. Remaining blockers are owner work,
not code: Play App Signing enrolment, hosted bilingual privacy policy and its in-app link, truthful
Data safety / Health / Health Connect / FGS declarations with a real demo video, store screenshots
and listing, name clearance, closed testing, and counsel and scholar reviews. Runtime evidence is
emulator-only; physical-device validation is still pending.

---

## 4. Do not publish until (hard list)

1. Play App Signing is enrolled with the upload certificate, the keystore is backed up and SEALED, and
   the final artifacts are re-hashed after any rebuild.
2. The FGS declaration demo video is captured from a real walk flow and the Play Console FGS
   declaration is submitted.
3. Real screenshots exist for the listing and for any written claim; black frames are never used.
4. Physical-device coverage exists for FGS start/stop, notification behaviour, reminder delivery,
   walk flow, CSV export/import and delete-all; sensor accuracy is verified on real hardware.
5. The v1.1 privacy policy (which lists the Health Connect read) has its placeholders filled, is live
   at its final public URL, is linked in Play Console and in-app, and native Arabic review is signed
   off; the HTML site is regenerated from the v1.1 source.
6. Data safety, Health declaration, the Health Connect data-access form, content rating,
   app access/ads/target audience and listing are completed truthfully and match the build.
7. Name/applicationId clearance is complete.
8. Closed testing with 12 real testers lasts 14 continuous days, if required.
9. Counsel and scholar reviews are complete and the memo §7.1 gate accepted.
10. C12 (`INTERNET` absence, signing identity, release merged manifest) is re-run on the
    owner-signed artifact before upload.

---

## 5. Signature

| Role | Name | Date | Reference |
|---|---|---|---|
| Legal (agent) | legal | 2026-11-17 | task-2; evidence log |
| Build/verification | buildsmith + Lead | 2026-11-17 22:35 | first signed release |
| Rebuild/verification | Lead | 2026-11-19 20:11 | artifacts above, re-hashed after the source ladder |
| Rebuild/verification | Lead | 2026-11-21 22:44 | v1.1 artifacts above, re-hashed after Health Connect + provenance |
| Team Lead | lead | | |
| Owner | | | |