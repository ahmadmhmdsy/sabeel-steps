# Release-engineering fix evidence — 2026-11-17

**Owner:** build-security (WS3) · **Task:** shared `task-9` · **Plan:**
[docs/superpowers/plans/2026-11-17-production-readiness.md](../../superpowers/plans/2026-11-17-production-readiness.md)
Phase 4, Tasks 13–15.
**Scope:** build system, release signing/keystore, licensing/notices, CI,
release docs, README, spec decision record. **No app source, resource,
manifest or test file was modified by WS3.** No Gradle task and no git command
was run by WS3 (Lead owns builds and commits).

## 1. Files changed / created

| File | Change |
|---|---|
| `app/build.gradle.kts` | Release signing config loaded from optional gitignored `keystore.properties`; release `isMinifyEnabled=true`, `isShrinkResources=true`, default proguard + `proguard-rules.pro`; `buildFeatures.buildConfig=true`; `BuildConfig.PRIVACY_POLICY_URL` from Gradle property `sabeel.privacyPolicyUrl` (default `""`); KSP `room.schemaLocation=app/schemas`; AAB language split disabled (`enableSplit=false`, D7); versionName `1.0.0` / versionCode 1. Dependencies, minSdk 26, compile/targetSdk 36 untouched. |
| `app/proguard-rules.pro` | Comments-only: **zero keep rules**. Explains that Compose/Room/DataStore/WorkManager consumer rules cover the current app and how to add a narrow rule only when a real reflection/JNI need appears. |
| `gradle.properties` | Added `org.gradle.caching=true` (keeps existing JVM args/parallel/AndroidX settings). |
| `.gitignore` | Grouped rules; added `*.p12`, `*.pfx`, `.externalNativeBuild/`, `.cxx/`, `*.apks`; signing material was already ignored. |
| `LICENSE` | Apache License 2.0 (full text + APPENDIX). |
| `NOTICE` | Attribution notice for the Android Open Source Project / AndroidX components. |
| `app/src/main/assets/THIRD-PARTY-NOTICES.txt` | Shipped third-party component list (all Apache-2.0) + build-only tooling note + "no copyleft linked" statement. |
| `keystore/sabeel-upload.jks` | **Generated locally, gitignored.** PKCS#12, RSA 4096, alias `sabeel-upload`, validity 10000 days (until 2054-04-04). |
| `keystore.properties` | **Generated locally, gitignored.** storeFile/storePassword/keyAlias/keyPassword. |
| `docs/release/upload-certificate.pem` | Public upload certificate for Play App Signing registration. |
| `docs/release/KEYSTORE.md` | Keystore identity incl. SHA-256 fingerprint, backup procedure, Play App Signing registration, rotation, loss consequences. No passwords. |
| `docs/release/PLAY-SUBMISSION-CHECKLIST.md` | Owner-facing Play submission runbook: identifiers, declarations (Data safety, Health apps, FGS health+location), testing tracks/rollout, assets, stop-ship conditions. |
| `docs/legal/BLOCKERS.md` | B1–B7 non-code blockers with acceptance evidence; closed code-side items for reference. |
| `docs/privacy-site/FILL-AND-HOST.md` | Owner runbook: exact placeholders, what to host (single `index.html`) and what never to host, hosting options, URL verification, `-Psabeel.privacyPolicyUrl` wiring. |
| `README.md` | Rewritten honestly: stack, build/test/lint/package commands, signing and privacy-URL build path, explicit verification table with `TO-BE-FILLED-BY-LEAD` markers, known limitations, owner blockers, repository map, Apache-2.0 license. |
| `docs/product/spec.md` | New §9 "Locked implementation decisions (production-readiness plan)": D1–D14 with one-line rationale and notes where each supersedes earlier spec text; earlier sections preserved as history. |
| `.github/workflows/android.yml` | CI: JDK 21 + Android SDK + Gradle cache; runs `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:bundleRelease` (unsigned, no secrets), uploads test/lint reports and build outputs. |

## 2. Keystore identity (public part only)

```
Alias:      sabeel-upload
Algorithm:  RSA 4096
Validity:   10000 days (created 2026-11-17, expires 2054-04-04)
Subject:    CN=Sabeel Upload, OU=Mobile, O=Sabeel, L=Baghdad, ST=Baghdad, C=IQ
SHA-256:    7B:12:AD:6B:FA:F0:78:17:E7:6A:25:39:95:4F:7B:4B:DE:2E:13:F6:B3:34:54:82:48:30:10:25:66:B6:15:2D
```

Passwords were generated randomly (40 alphanumeric characters each) and exist
only in the gitignored `keystore.properties` and in the owner's password
manager. They are not printed in this document, any other doc, or any message.
Backup and Play App Signing steps: [KEYSTORE.md](../../release/KEYSTORE.md).

## 3. Build behaviour to expect

- **Signed locally:** because `keystore.properties` exists on the build machine,
  `assembleRelease`/`bundleRelease` produce artifacts signed with the upload
  key. Verify with the commands in §5.
- **Unsigned on CI/fresh clone:** without the file, the same tasks succeed and
  produce unsigned artifacts — intended; never add signing secrets to CI.
- **Privacy URL:** default is an empty `BuildConfig.PRIVACY_POLICY_URL`. Release
  store builds must pass `-Psabeel.privacyPolicyUrl=https://<hosted-policy>`.
  The UI workstream owns hiding the link when empty and opening it when set.
- **Room schema:** KSP exports schemas to `app/schemas/`; commit that folder
  after the first successful build so migrations are reviewable. This requires
  `AppDatabase` to keep `exportSchema = true` (Lead-owned source task).

## 4. Exact Gradle commands for the Lead

Environment (Windows):

```powershell
$env:JAVA_HOME      = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:ANDROID_HOME   = 'C:\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
```

Run from the repository root:

```powershell
.\gradlew.bat --console=plain :app:assembleDebug
.\gradlew.bat --console=plain :app:testDebugUnitTest
.\gradlew.bat --console=plain :app:lintDebug        # must be exit 0
.\gradlew.bat --console=plain :app:assembleRelease
.\gradlew.bat --console=plain :app:bundleRelease `
  -Psabeel.privacyPolicyUrl=https://<hosted-policy>
```

Expected outputs:

| Task | Output |
|---|---|
| `assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| `testDebugUnitTest` | `app/build/reports/tests/testDebugUnitTest/index.html` |
| `lintDebug` | `app/build/reports/lint-results-debug.html`; exit 0, zero errors |
| `assembleRelease` | `app/build/outputs/apk/release/app-release.apk` (signed) |
| `bundleRelease` | `app/build/outputs/bundle/release/app-release.aab` (signed) |
| Room schema | `app/schemas/com.sabeel.steps.data.db.\*.json` (commit) |
| R8 mapping | `app/build/outputs/mapping/release/mapping.txt` (archive per release) |

> **Known sequencing:** `:app:lintDebug` will remain red until the Lead-owned
> lint fixes (5 `MissingPermission` errors, plan Task 17) land. Do not weaken the
> lint configuration to make CI pass; fix the call sites. CI cannot be green
> before that commit.

## 5. Artifact verification (after the build)

```powershell
# APK: signature + certificate fingerprint
& 'C:\Android\Sdk\build-tools\36.0.0\apksigner.bat' verify --print-certs `
  app/build/outputs/apk/release/app-release.apk

# AAB: JAR signature (AABs are signed as a JAR)
keytool -printcert -jarfile app/build/outputs/bundle/release/app-release.aab

# 16 KB page-size alignment (64-bit devices) on the release APK
& 'C:\Android\Sdk\build-tools\36.0.0\zipalign.exe' -c -P 16 -v 4 `
  app/build/outputs/apk/release/app-release.apk

# No INTERNET permission in the release manifest
& 'C:\Android\Sdk\cmdline-tools\latest\bin\apkanalyzer.bat' manifest permissions `
  app/build/outputs/apk/release/app-release.apk

# Privacy URL actually compiled into the build
Select-String -Path app/build/generated/source/buildConfig/release/com/sabeel/steps/BuildConfig.java `
  -Pattern 'PRIVACY_POLICY_URL'

# Hashes for the release record
Get-FileHash app/build/outputs/apk/release/app-release.apk, `
  app/build/outputs/bundle/release/app-release.aab -Algorithm SHA256
```

Acceptance: `apksigner verify` says `Verified using v2/v3/v4 scheme` and the
certificate SHA-256 equals the fingerprint in §2; `zipalign -P 16` reports
verification successful; permissions list contains no `INTERNET`.

## 6. Residual risks / open items (owner or Lead)

1. **No build was run by WS3** — the Gradle changes are syntax-checked by review
   only. First risk is a Kotlin DSL error; the standard patterns used
   (`signingConfigs.create` + optional assignment, `buildConfigField`,
   `providers.gradleProperty`, `ksp { arg(...) }`, `bundle.language.enableSplit`)
   are documented AGP/KSP forms.
2. **R8 behaviour is runtime-unverified.** Shrinking is newly enabled; run the
   full manual smoke list on a signed release build. If a feature breaks, add the
   narrowest keep rule to `proguard-rules.pro` with a comment, not a blanket rule.
3. **Lint gate / CI** — stays red until Lead Task 17 fixes the
   `MissingPermission` call sites; CI workflow file is otherwise ready.
4. **Privacy URL wiring** — `BuildConfig.PRIVACY_POLICY_URL` exists, but the UI
   must hide the link when empty; otherwise a release could ship a broken link.
   Release builds must pass `-Psabeel.privacyPolicyUrl`.
5. **Room schema commit** — `app/schemas/` must be committed after the first
   build (Lead), and `exportSchema = true` must stay on `AppDatabase`.
6. **Keystore custody** — the generated keystore is currently only on this
   machine. Back it up (two encrypted copies) **before** the first Play upload;
   losing it without Play App Signing means a new package name forever.
7. **Play App Signing not yet registered** — owner action; the upload
   certificate and fingerprint are ready in `docs/release/KEYSTORE.md`.
8. **README verification cells** — `TO-BE-FILLED-BY-LEAD` values (test count,
   lint status, artifact hashes) must be filled from the actual build output,
   never estimated.
9. **CI workflow untested on GitHub** — no remote has been pushed; first run
   may need runner-image actions versions confirmed. Lint failure is the expected
   first red and is tracked as item 3.
10. **AAB size** increases slightly because language splits are disabled (D7);
    acceptable tradeoff for the in-app EN/AR switch.

## 7. Definition-of-done status for this workstream

| Plan gate | Status |
|---|---|
| Minified, signed release AAB path exists (D12) | Code/config ready; signed locally by Lead |
| Release keystore generated, documented, gitignored | Done (fingerprint recorded) |
| LICENSE + NOTICE + third-party notices shipped | Done |
| CI workflow builds/tests/lints/bundles | Written; green only after lint fix |
| Privacy URL configurable without source edits (D13) | Done (Gradle property + BuildConfig) |
| README describes the real build/test/verify path | Done (markers for measured values) |
| Spec records D1–D14 rather than silently editing requirements | Done (§9) |
| Owner blockers documented with acceptance evidence | Done (docs/legal/BLOCKERS.md) |

**Do not mark the release ready until:** a signed AAB is built and verified with
the commands in §5, lint is exit 0, the privacy URL is hosted and embedded, the
release smoke test is recorded, and Play App Signing registration is confirmed.
