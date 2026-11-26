# Changelog

All notable changes to Sabeel (Android). Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project follows the version in `app/build.gradle.kts` (`versionName` / `versionCode`), not the internal doc shorthand.

> **Version numbering note:** the step-manifold work is called "v1.2" in the planning docs, but no 1.2.0 artifact was ever versioned or released; the build that preceded v1.3 kept **versionName 1.1.0 / versionCode 2**. v1.3 jumps to **versionName 1.3.0 / versionCode 3**. Do not cite a 1.2.0 APK — it does not exist.

## [1.3.1-field] — 2026-11-26 (versionCode 4, internal diagnostics build)

**Not a public release.** A release-key-signed field APK for the owner's phone, shipped to investigate "count works but today stays 0 / last update not counted / Android keeps stopping the app". Tester card: [`docs/release/FIELD-BUILD-1.3.1.md`](docs/release/FIELD-BUILD-1.3.1.md).

### Added
- **Flight recorder (`diagnostics/FlightRecorder.kt`):** private, rotating JSONL event log (2 × 512 KB, 14-day retention, no free text/identifiers) covering service create/destroy, start requests and results, heartbeats with power/doze state, sensor-arrival markers, write results, worker runs, boot attempts, midnight-alarm runs, crash class + 12 frames, and persisted process facts.
- **Post-mortem capture (`diagnostics/PostMortem.kt`):** reads `ApplicationExitInfo` (API 30+) before `AppContainer` work on app open, persists the previous process's exit reason/gap, and keeps the last records for the report. The stored snapshot is only a fallback: every open re-queries Android so a later force-stop cannot be hidden by an older record.
- **One-tap full report (`diagnostics/DiagnosticsReport*.kt`, `DiagnosticsShare.kt`):** Diagnostics → *Share full report* → preview of `report.txt` → `ACTION_SEND` ZIP `{report.txt, meta.json, events.jsonl}` through a FileProvider; *Delete diagnostic data* wipes the recorder. Report sections: PROCESS, COUNTING, HEARTBEAT, SENSOR, WORKER, START, MIDNIGHT ALARM, CRASH, PROCESS END, POWER, PERMISSIONS, RECORDER, event tail, hints.
- **Midnight baseline (`diagnostics/MidnightBaseline.kt`):** one inexact `AlarmManager.setAndAllowWhileIdle` wake-up at ~00:05 local, no `SCHEDULE_EXACT_ALARM`; starts the counting service only when counting is wanted, the app is battery-exempt and the service is dead, and re-arms itself. When it does not run, the existing rule still drops the unattributable cross-midnight delta.
- **Self-heal + honest pause:** `ReconcileWorker` (15 min) and `BootHealthWorker` restart the foreground service when the app is exempt from battery optimization, and otherwise post a visible "Step counting stopped" alert (new `counting_alerts` channel, urgent variant) instead of attempting an illegal background start. Home's system-stopped card is now an error-colored critical card with a *Share report* action.
- **`PipelineObserver` events:** pure `StepPipeline` now emits DAY_ROLLOVER / BASELINE / REBOOT_REBASE / OUT_OF_ORDER / CLAMPED / CATCH_UP_UNKNOWN for the recorder without adding Android dependencies.
- **Tests:** `MidnightBaselineTest` (next-trigger math incl. exactly at 00:05, before/after midnight, strictly future) and `DiagnosticsReportTextTest` (all sections present, exit reason rendered, no null interpolation).

### Changed
- `versionCode` 4; `versionName` `1.3.1` normally and `1.3.1-field` when built with `-Psabeel.fieldDiagnostics=true`, which sets `BuildConfig.FIELD_DIAGNOSTICS`. The public 1.3.1 must use versionCode 5 and leave the flag off (recorder dormant).
- Manifest adds the non-exported midnight receiver and the diagnostics FileProvider; no new runtime permission and **no INTERNET permission** were added.

### Evidence
- Unit tests: **212 tests / 20 suites / 0 failures** (`testDebugUnitTest`), including the two new suites.
- Lint: **0 errors**, 94 warnings, 2 hints (`lintDebug`; warning count differs from 1.3.0 because of new code).
- Release field build (`-Psabeel.fieldDiagnostics=true`): APK **2,164,470 B** SHA-256 `4c6a132f0d0e2782fb3497491cf637ab1a7a7b36af46b41cdc0eebc4e090d979`; AAB **4,667,151 B** SHA-256 `ca370dd579e991f1b2e32689cd9553ecc0d80f668afed579f6bf00e1f2ba517c`. Signed with the same upload certificate as 1.3.0 (`7b12ad6b…6152d`), so it installs as an in-place update preserving data and the battery exemption. No `INTERNET` permission in the built manifest (verified with `aapt2 dump permissions`).
- **Emulator Step Zero (API 36, debug-signed field build over an existing install):** force stop at 18:03:39, reopen at 18:06:06 → 173 s dead window; `report.txt` showed the new pid/start in `[PROCESS]`, `reason=USER_REQUESTED(10)` with `desc=[FORCE STOP] …` in `[PROCESS END]`, and `[COUNTING]` stayed 0 (no invented steps). The preview rendered the real `report.txt`; *Share report* opened the system chooser with the ZIP filename and a working FileProvider read grant. This run also caught and fixed two report bugs before the final artifacts: stale stored exit records hid later force-stops, and process age used device uptime instead of the process start.
- **Not run yet:** physical-device Step Zero (install → force stop 2 min → open → share report before restarting) and the 48–72 h field observation. Those are the next evidence steps; emulator evidence alone is not claimed as device proof.

## [Unreleased] — 1.3.0 (versionCode 3)

**GitHub release:** [Sabeel v1.3.0 — signed APK + AAB assets](https://github.com/ahmadmhmdsy/sabeel-steps/releases/latest) · checksums: [`dist/SHA256SUMS.txt`](dist/SHA256SUMS.txt).

**Status: built and tested 2026-11-23; not published.** Unit tests (205 / 18 suites / 0 failures), lint (0 errors), and debug/release/AAB builds are green; emulator flows are recorded in `TEST-XIAOMI.md` §N.1. Physical Redmi Note 7 evidence is still pending. See `README.md` *Verification status* for the measured artifact table and `docs/release/CLAIMS-AUDIT.md` §10 for per-claim evidence.

### Added
- **Counting control:** one persisted on/off switch for step counting, shared by the Home ring, the status pill and Settings; the choice survives reboot and restart, and Diagnostics distinguishes "off by user" from "stopped by system".
- **Boot restart evidence:** `BootReceiver` records each boot attempt (flags, result, service-alive check), retries once, and Diagnostics shows the last ten attempts plus a shareable text report that contains no location or place data.
- **Recovery notice:** at most one "step counting is paused" notification when counting should be on but the service is not alive after a boot check; it stops after the app is opened.
- **Custom sessions:** walk or run with optional time/steps/distance/place goals, optional foreground route and distance, pause/resume without back-filling, finish/save or discard, one active session, midnight sessions split into legs. Session steps are a projection of the global day count (never a second counter).
- **Places and presets:** locally saved places (label + coordinates + radius; no search/geocoding/map tiles) and saved session setups.
- **Session export (CSV/GPX):** History exposes "Export all sessions (CSV)" and per-session "Export GPX" through the system document picker; both were exercised on the emulator. No network, no automatic upload.
- **Room v3:** `Place`, `SessionPreset`, `WalkLeg` tables and the `MIGRATION_2_3` migration; schema JSON exported; no destructive fallback.
- **Sleep/pause plumbing:** `CountingStore`, `CountingController` (write-before-act, serialized, idempotent), `SessionConfig`, `BootHealthWorker`, new Diagnostics rows and strings.

### Changed
- Diagnostics now reports counting state, boot attempts and heartbeat/restart controls.
- Home: the ring is the counting switch (switch role for TalkBack) and the status pill mirrors it.
- History: sessions and their labels are shown alongside daily totals (UI landing in this release).
- Privacy policy v1.3 source: sessions, session routes, saved places/presets, session-export scope, deletion coverage.

### Honest limits (unchanged by this release)
- Android/OEM can still stop counting after force-stop, aggressive battery management or blocked background starts; the app cannot restart itself after a force-stop.
- No auto-open UI, no background location, no map tiles/address search, one session at a time, estimates are labelled estimates, not medical data.
- Arabic translations for the new screens are present, but no native Arabic review has signed them off; native review remains a launch precondition.

### Evidence at the time
- Final-tree gate (2026-11-23): 205 JVM tests / 0 failures / 0 skipped in 18 suites; `lintDebug` 0 errors (80 warnings, 2 hints); release APK 2,138,010 B SHA-256 `487ee9c9f7e7d0e1bbe490c4b7692c72cb83c0f8d963cf1f2a4545677a4dfe5d`; release AAB 4,608,495 B SHA-256 `ffde823d8855a7d2e8f3987156dd93124c89cf2e92894e310ba01f43a4a9fc4e`; signed with the upload key; versionCode 3 / versionName 1.3.0. Copies and checksums in `dist/`.
- Release-signed smoke test (2026-11-23, clean API 36 emulator): the measured R8 release APK installed, completed onboarding, opened Home with counting on, started a session with both step and walk services alive, and discarded cleanly.
- Emulator-5564 (debug build): reboot with counting ON auto-restarted the foreground service (diagnostics `Result: STARTED`, `Still counting after the check: Yes`); reboot with counting OFF stayed off; ring toggle off→on; session pause froze steps and resume preserved them; CSV/GPX exported; PLACE goal + auto-stop saved a session and posted the goal notification.

### Not released
- The v1.3 artifacts are built but **not uploaded or published**; Play needs a rebuild with `-Psabeel.privacyPolicyUrl` once the policy is hosted, plus the owner's physical-device run, demo video and native Arabic review.

## [1.1.0] — 2026-11-21 (versionCode 2)

The last built+signed snapshot (also called "v1.2" in the docs; see the note above).

### Added
- **Health Connect, read-only, opt-in:** reads today's `StepsRecord` total as an absolute floor; optional feature-gated `READ_HEALTH_DATA_IN_BACKGROUND`; the app never writes to Health Connect.
- **Step manifold:** counter + detector run in parallel with per-source ledgers, Health Connect as a floor and a labelled accelerometer estimate as gap-filler; no source switching or re-basing, reboot/sensor-reset rebasing, counter catch-up after process death, persisted "≈" marker.
- **Release hardening:** Room v1→v2 migration, R8 minify/shrink with a smoke-tested signed release, boot auto-start and reminders, Diagnostics storage/source health, EN/AR UI.

### Fixed
- Rest-day streak bug found in emulator smoke; covered by tests.
- Escaped-quote build-file break; swapped to a plain AGP version.

### Evidence at the time
- 127 JVM tests / 0 failures, `lintDebug` exit 0 (46 warnings), signed release APK 1,864,306 B and AAB 3,992,650 B, device/emulator evidence under `docs/review/2026-11-*`. These are **historical** and not carried forward as v1.3 evidence.

## [1.0.0] — initial internal version (versionCode 1)

### Added
- Offline step counter and walking tracker: foreground step service, Home progress ring + goal, 7/30-day History, Insights, GPS walk with Canvas route (no map tiles, no API key, no network), reminders and goal notification, EN/AR UI, CSV export/import of daily totals (SAF), delete-all, Privacy/About screens.
- No INTERNET permission, no ads, no analytics, no account; `allowBackup="false"`.

[Unreleased]: #unreleased--130-versioncode-3
[1.1.0]: #110--2026-11-21-versioncode-2
[1.0.0]: #100--initial-internal-version-versioncode-1
