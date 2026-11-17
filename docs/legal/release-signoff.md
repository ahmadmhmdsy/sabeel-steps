# Release sign-off — Sabeel v1.0 (one page)

**Status: FINAL EVIDENCE STAGED — NOT APPROVED FOR SUBMISSION.** Static gates are based on the independent buildsmith batch (EV-01…EV-20 in [release-evidence-log.md](release-evidence-log.md)); runtime rows stay **NOT VERIFIED — EMULATOR-ATTEMPTED**; **C12 stays NOT VERIFIED BY DESIGN**; the **release-artifact gate is OPEN** (no release AAB / release merged manifest). No row below is a Play approval.

**Artifact:** app-debug.apk, 11,488,512 B, mtime 2026-11-16T23:51:37+03:00, SHA-256 324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E (matches dist/ copy). Batch: PASS=33, FAIL=0, FINDING=1, OPEN=1, stale files none. Prepared 2026-11-17 00:45 +03:00.

---

## 1. Gate table

| # | Gate | Evidence | Status |
|---|---|---|---|
| G1 | Package / target API | C2: com.sabeel.steps, targetSdk 36, minSdk 26 | **PASS** |
| G2 | Backup flag | C5/C6: allowBackup=false | **PASS** |
| G3 | No INTERNET | C4: permission absent | **PASS** |
| G4 | No ACCESS_NETWORK_STATE | C4: permission absent | **PASS** |
| G5 | No transmitting SDKs | C8: releaseRuntimeClasspath free of ad/analytics/crashlytics/firebase/play-services; C10-S | **PASS (static)** |
| G6 | 16 KB page sizes | C9: zipalign -P 16 + ELF p_align=16384 | **PASS (debug artifact; recheck release AAB)** |
| G7 | FGS declaration + demo video | types health + location\|health PASS static; no Play declaration; demo video 0 bytes | **OPEN** |
| G8 | User-facing permissions | C4: expected 8 present (ACCESS_FINE included); ACCESS_COARSE also present in runtime dump; no background location | **PASS (static)** |
| G9 | Runtime QA | boot 132 s, install, cold launch 2889 ms, 4 grants, reboot 52 s; onboarding uiautomator text; FATAL 0 | **OPEN — NOT VERIFIED — EMULATOR-ATTEMPTED** |
| G10 | Privacy policy live | draft policy + hosted page exist; placeholders, hosting, in-app link, Arabic native review pending | **OPEN — OWNER** |
| G11 | Data safety form | not submitted; C10-S is the evidence basis | **OPEN** |
| G12 | Health apps declaration | not submitted | **OPEN** |
| G13 | Content rating | not obtained | **OPEN** |
| G14 | Store listing | not written; screenshots black | **OPEN** |
| G15 | App access / ads / audience | not declared | **OPEN** |
| G16 | Name / package clearance | Khatwa rejected, Sabeel chosen; registry + applicationId check pending | **PARTIAL — OWNER** |
| G17 | Signing | no signingConfig; no release artifact yet; eventual release unsigned by design until owner signs; keystore none present; C12 NOT VERIFIED BY DESIGN | **OWNER ACTION** |
| G18 | Closed testing 12/14 | not started | **OWNER ACTION** |
| G19 | Counsel + scholar review | memo ready, reviews pending | **OPEN** |
| G20 | Clean-room statement | pending Lead/owner confirmation | **OPEN** |
| R1 | Release artifact | FINDING: no release APK/AAB, no release merged manifest; C2/C4/C5/C9 measured on debug only | **OPEN** |
| S1 | Unit tests (supporting, not a Play gate) | 41/41 platform-engineer self-report; not in batch | **PROVISIONAL** |

**Counts:** 7 PASS static (G1–G6, G8) · 10 OPEN (G7, G9–G15, G19, G20) · 1 PARTIAL/OWNER (G16) · 2 OWNER ACTION (G17, G18) · R1 OPEN · S1 PROVISIONAL. **0 final Play approvals.**

---

## 2. Owner actions

| # | Action | Blocks |
|---|---|---|
| O1 | Build unsigned release AAB and rerun C2/C4/C5/C6/C9 on the release artifact; then owner signs | R1, G6 recheck |
| O2 | Fill privacy-policy placeholders (legal name EN/AR, monitored email, optional address, date), host only docs/privacy-site, link in console + in-app; native Arabic review | G10 |
| O3 | Create upload keystore outside the repo, configure signing, enrol Play App Signing; keep keystore SEALED | G17 |
| O4 | Name/package clearance: Iraqi trade-mark registry, WIPO, Play applicationId | G16 |
| O5 | Closed testing with 12 real testers for 14 continuous days (if personal account created after 2023-11-13) | G18 |
| O6 | Play Console: Data safety, Health declaration, FGS declaration + demo video, content rating, listing, app access/ads/audience | G7, G11–G15 |
| O7 | Capture real visual evidence on a windowed emulator or device; never use black frames or fake screenshots | G14, part of G7 |
| O8 | Counsel review (Iraq + markets) and scholar review; accept memo §7.1 gate | G19 |
| O9 | Confirm no team member worked on the target apps; grep repo for target names/logos/comparisons | G20 |

---

## 3. Owner-facing readiness (≈120 words)

Sabeel v1.0 (com.sabeel.steps) is not ready to publish. Static verification reproduced the candidate APK (SHA-256 324690…9B9E): targetSdk 36, minSdk 26, allowBackup false, no INTERNET or ACCESS_NETWORK_STATE, expected health/location permissions, declared foreground-service types, 16 KB alignment, no ad, analytics, Firebase or Play-services dependencies, and no keystore material in the repository. Seven build gates pass on that artifact. Submission remains blocked because no release AAB or release merged manifest exists; runtime evidence is emulator-attempted only (boot, install, cold launch, onboarding, reboot), screenshots are black, the FGS demo video is missing, and FGS, notification, reminder and walk flows are unverified. Still open: Play declarations, live bilingual privacy policy, rating, listing, signing, name clearance, closed testing, and counsel and scholar reviews. C12 stays NOT VERIFIED BY DESIGN.

---

## 4. Do not publish until (hard list)

1. An unsigned release AAB exists and C2/C4/C5/C6/C9 are rerun on the release artifact (release merged manifest present), and the owner-signed artifact is re-checked.
2. The FGS declaration demo video is captured from a real walk flow on a windowed emulator/device (never faked) and the Play Console FGS declaration is submitted.
3. Real screenshots exist for the Play listing and for any written claim; the four black frames are discarded.
4. Runtime coverage exists for FGS start/stop, notification and denial behaviour, reminder scheduling, walk flow/route, CSV export and delete-all on a clean emulator or device; sensor accuracy is verified on real hardware.
5. Privacy-policy placeholders are filled, the page is live at its final public URL, linked in Play Console and in-app, and native Arabic review is signed off.
6. Data safety, Health declaration, content rating, app access/ads/target audience and store listing are completed truthfully and match the build.
7. Name/applicationId clearance is complete.
8. Closed testing with 12 real testers lasts 14 continuous days.
9. Counsel and scholar reviews are complete and the memo §7.1 publication gate is accepted.
10. C12 is re-run on the owner-signed artifact; keystore material stays outside the repo and SEALED.

---

## 5. Signature

| Role | Name | Date | Reference |
|---|---|---|---|
| Legal (agent) | legal | 2026-11-17 | task-2; evidence log |
| Build/verification | buildsmith | | final-20261117-000050 |
| Team Lead | lead | | |
| Owner | | | |
