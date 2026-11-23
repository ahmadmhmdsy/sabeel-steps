# Security Policy — Sabeel (سبيل)

Sabeel is an offline Android step counter and walk/run tracker. It has no server, no accounts, no
telemetry, and its merged manifest contains **no `android.permission.INTERNET`**. That removes most
of the classic mobile attack surface; what remains is local data, the exported surface, permissions,
files the user explicitly exports, and the signing chain.

## Supported versions

| Version | Supported | Notes |
|---|---|---|
| **1.3.x** (versionName `1.3.0`, versionCode 3) | **Yes** | The current source tree and builds made from it. Security fixes land on `main` and ship in the next release. |
| 1.1.0 (versionCode 2) | No | Internal snapshot; never published. |
| 1.0.x (versionCode 1) | No | Initial internal version; never published. |

The version identity is defined in `app/build.gradle.kts`. Sabeel is distributed **only through
[GitHub Releases](https://github.com/ahmadmhmdsy/sabeel-steps/releases/latest)** and is **not
published on Google Play**, so there is no store build to patch; supported artifacts are builds from
this source tree and the files attached to the latest GitHub Release, checked against the published
`SHA256SUMS.txt`.

## Reporting a vulnerability

Use GitHub's private vulnerability reporting:

https://github.com/ahmadmhmdsy/sabeel-steps/security/advisories/new

Do **not** open a public issue, pull request, or discussion containing vulnerability details. If you
cannot use GitHub Security Advisories, open a minimal public issue asking for a private channel and
include **no** technical detail in it.

A useful report includes:

1. The affected version and how you obtained the build: versionName/versionCode (Settings → About),
   whether it was a release or debug build, and whether it came from source or from a published
   artifact.
2. Android version/API level, device model and OEM skin.
3. Reproduction steps and the observed impact.
4. Whether any data leaves the device (by design it must not) and what data was involved.
5. Any crash/stack trace or Diagnostics report, **with personal data removed**. The in-app
   Diagnostics → Share report is built to exclude location and saved-place data, but read it before
   pasting; never attach CSV/GPX exports, screenshots showing saved places or maps, or your device
   backup.
6. A suggested fix or mitigation, if you have one.

English or Arabic reports are both accepted (English is easier to triage; the app's Arabic UI review
status does not apply to your report).

## Testing boundaries

- Test only on devices and accounts you own or are explicitly authorized to test.
- Do not run denial-of-service or storage/battery-exhaustion tests, social engineering, or tests
  against other people's devices or data. There is no server-side data to target.
- Give us a chance to fix the issue before publishing details. We will agree a disclosure date with
  you; if we cannot, we will tell you rather than stall silently.
- We do **not** operate a bug bounty and cannot pay for reports. There is no guaranteed response
  time; this is a best-effort, single-maintainer project.

## In scope (examples)

- **Local data at rest:** Room database, DataStore preferences, Room-exported schemas, temporary
  files, and anything that would let another app or another user read them.
- **Backup/restore:** a path that exports app data despite `android:allowBackup="false"` and the
  data-extraction rules; data surviving "delete all" or uninstall in an unexpected place.
- **Exported components and intents:** `MainActivity` (launcher), `.ViewPermissionUsageActivity`
  (activity-alias guarded by `android.permission.START_VIEW_PERMISSION_USAGE`),
  `.platform.BootReceiver` (not exported; filters `BOOT_COMPLETED` and
  `MY_PACKAGE_REPLACED`) and the foreground services (not exported). Any component that a
  third-party app can drive, spoof, or use to corrupt state is in scope.
- **Permissions and the privacy claim:** any code path that can reach the network; `INTERNET` or
  `ACCESS_NETWORK_STATE` reappearing in the merged manifest; data leaving the device; Health
  Connect writes or a write permission; background location; a new exported surface.
- **Exported files:** CSV and GPX are written only through the Android system document picker (SAF).
  In scope: path traversal, unsafe file names, silently exporting more data than the privacy policy
  says, CSV import that a hostile file can use to crash, overwrite data, or corrupt the database.
- **Notifications and foreground-service behaviour:** leaked place names or route data in
  notifications, spoofable service intents, or a notification action that performs an unintended
  privilege change.
- **Signing and update integrity:** a release APK/AAB signed with a certificate other than the
  documented upload key, versionCode downgrade, or an artifact on a GitHub Release whose SHA-256 does
  not match the published `SHA256SUMS.txt`. The public certificate and fingerprint are in
  `docs/release/KEYSTORE.md`; private key material is never in the repository.
- **Build and CI supply chain:** workflow permissions, unexpected secrets in the build, or an action
  that gains more than read access. The CI workflow runs with `permissions: contents: read` and
  never signs.
- **Debug-only surfaces:** the debug build exports `.debug.DebugStepReceiver` (a step injector for
  emulator QA). That is **expected in debug builds**; it is in scope only if a receiver like it is
  reachable in a **release** build, or if a release build can be made to inject steps.

## Out of scope (documented behaviour, not vulnerabilities)

- Android/OEM limitations that are product limits, not bugs: a force-stop cannot be restarted from
  the app, OEM battery managers can stop a foreground service, hardware step sensors may be missing,
  Health Connect may be absent or hold no data, and sensor resets can lose steps. These are stated
  in [README › Honest limitations](README.md#honest-limitations).
- Incorrect step counts, wrong distances, estimate inaccuracy, or an OEM-specific counting gap: file
  a bug report, not a security report.
- Device compromise: root, a malicious accessibility service, an attacker with physical access to an
  unlocked device, or a compromised OS — these are outside the Android security model.
- Denial of service through storage exhaustion, normal battery consumption, or forcing the app to
  work in unreasonable conditions.
- Google Play policy, listing, moderation or distribution decisions; the app is not published yet.
- The known open release items: pending native Arabic review, missing physical-device validation of
  v1.3, unhosted privacy policy. Those are tracked in `docs/legal/BLOCKERS.md`, not security
  reports.
- Vulnerabilities in third-party dependencies with no reachable path in this app — report them
  upstream (a heads-up is still welcome).
- Forks and redistributed builds: report to the maintainer of that fork; only this repository's main
  branch is covered.

## What we will do

- Triage the report, confirm or challenge it with evidence, and tell you what we found.
- Fix in `main`, add a regression test where one is feasible, and record the change in
  `CHANGELOG.md`.
- Credit you in the advisory and/or changelog if you want credit, and agree on disclosure timing.
- Publish a GitHub Security Advisory once a fix or mitigation exists.

## Known and documented — do not report as new

- No `INTERNET` permission; `ACCESS_NETWORK_STATE` is removed with `tools:node="remove"`. The app
  ships no networking, ads, analytics or crash-reporting code.
- `allowBackup="false"` with explicit data-extraction and full-backup rules.
- Health Connect access is **read-only and opt-in**; the app never writes health data.
- The debug step-injection receiver exists only in the debug source set
  (`app/src/debug/AndroidManifest.xml`).
- Release builds are signed locally from a gitignored upload key; CI produces unsigned artifacts by
  design.

## Contact

Security: use the GitHub Security Advisories link above, not e-mail.
Code of conduct: see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
