# Verification evidence — 2026-11-17

This folder keeps the durable copies of the independent verification batch produced outside the repo
(`C:\dl\legal-evidence\final-20261117-000050`). The originals, including raw dumpsys output and the
black-frame screenshots, remain outside the repository.

- `EVIDENCE-20261117-000050.txt` — static batch: build identity, C2 badging, C4 permissions,
  C5 manifest flags, C6 merged manifests, C8 dependency audit, C9 16 KB alignment, C10-S static checks,
  C12 signing note. Result: PASS=33, FAIL=0, FINDING=1 (no release APK/AAB built), OPEN=1,
  "stale files present: none".
- `AVD-EVIDENCE-20261117.txt` — API 36 emulator smoke attempt, logs and every runtime row with its
  observed output. All runtime rows are labelled NOT VERIFIED — EMULATOR-ATTEMPTED, never PASS.
- `AVD-SMOKE-20261117.md` — the same run as a table (boot/install/launch PASS, onboarding reachable,
  reboot PASS, deeper flows untestable).
- `ENVIRONMENT-CONTAMINATION-NOTE.txt` — a foreign app was present on the shared emulator and affected
  the reachable-screen context of a few rows; no product finding.
- `SCREENSHOT-LIMITATION-NOTE.txt` — headless swiftshader produced black frames; visual evidence is
  untestable in that environment.

Measured artifact: `dist/Sabeel-1.0-debug.apk` — 11,488,512 bytes, mtime 2026-11-16T23:51:37+03:00,
SHA-256 324690190282F9E63A01F8437BBFA259C8BF69C0BD57072F3FC9498B12C89B9E.
