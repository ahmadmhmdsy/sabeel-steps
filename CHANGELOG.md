# Changelog

All notable changes to Sabeel (Android). Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project follows the version in `app/build.gradle.kts` (`versionName` / `versionCode`), not the internal doc shorthand.

> **Version numbering note:** the step-manifold work is called "v1.2" in the planning docs, but no 1.2.0 artifact was ever versioned or released; the build that preceded v1.3 kept **versionName 1.1.0 / versionCode 2**. v1.3 jumps to **versionName 1.3.0 / versionCode 3**. Do not cite a 1.2.0 APK — it does not exist.

## [Unreleased] — 1.3.0 (versionCode 3)

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
