# Step-source resilience — emulator evidence (2026-11-19)

**Build under test:** debug APK built 2026-11-19 18:44 +03:00 from commits `8246615` and
`90801b8` plus the walk-estimate-label change recorded with this evidence. APK: 12,015,507 B,
SHA-256 `595CF33F6DDDECEB48AB482BEF4F29E769E96F18BD1D55A0DED8D99B96E22FAB`.
Package `com.sabeel.steps` 1.0.0 (versionCode 1, targetSdk 36, minSdk 26).
Captures qa-01…qa-16 were taken at 18:22 from the same code without the walk-only label
(APK 12,014,371 B, SHA-256 `830B7AAC…E017795`); captures qa-17/18 are from the final build.

**Emulator:** `emulator-5556`, AVD `api36`, image `sdk_slim_x86_64-userdebug` (Android 16 / API 36),
1080x2400, 420 dpi, headless. This image has **no step counter and no step detector** and does have the
Goldfish 3-axis accelerometer, so it exercises only the **estimate** tier. See
[qa-13-sensor-absence.txt](qa-13-sensor-absence.txt) for the sensor list and the registry proof.

## What was run

1. `adb uninstall com.sabeel.steps` then `adb install app-debug.apk` (fresh data and prefs).
2. Granted `ACTIVITY_RECOGNITION` and `POST_NOTIFICATIONS` before onboarding.
3. Completed the four onboarding steps in English, starting from an Arabic system locale and choosing
   English in the onboarding language picker; then finished with **Start walking**.
4. Waited 30 s without touching the device: [qa-01](qa-01-home-estimated-en.xml),
   [qa-02](qa-02-home-stationary-30s-en.xml) — both **≈ 0** steps.
5. Injected a walking waveform for 60 s at roughly 7 Hz through the emulator sensor console:
   `adb emu sensor set acceleration 0:<y>:0` with `y = 9.81 + 3.0*sin(2*pi*1.8*t)` (427 injections in
   60.2 s), then reset to `0:9.81:0`. This is a 1.8 Hz, 3 m/s² vertical oscillation around gravity;
   the detector sees one magnitude peak per cycle.
6. Captured Home, Diagnostics, the notification, and the persisted source status in English; switched
   the in-app language to Arabic in Settings and captured the same screens again.

## Results

| Check | Result | Evidence |
|---|---|---|
| App opens without a crash after fresh install | Pass | [qa-14-crash-buffer.txt](qa-14-crash-buffer.txt) is empty; [qa-16-logcat-errors.txt](qa-16-logcat-errors.txt) has 0 matches |
| Home knows the phone has no measured sensor and says the number is estimated | Pass | **≈ 0 of 8,000 steps** + "Estimated from the phone's motion, not a hardware step counter." — [qa-01](qa-01-home-estimated-en.xml) |
| Stationary 30 s adds no steps | Pass | Still **≈ 0** — [qa-02](qa-02-home-stationary-30s-en.xml) |
| Injected 1.8 Hz walking produces a positive estimate | Pass | **≈ 97 of 8,000 steps** in 60 s (108 expected cycles; detector is intentionally conservative) — [qa-03](qa-03-home-walking-estimated-en.xml) |
| Diagnostics names the live source | Pass | Source **Phone motion (estimated)**, status **Counting (estimated)**, today 97 steps, last sensor reading **6:27 PM**, fallbacks today **None** — [qa-04](qa-04-diagnostics-estimated-en.xml) |
| Diagnostics copy is honest about the ladder | Pass | "Sabeel counts with the best motion source this phone offers: a hardware step counter, a hardware step detector, or a labelled estimate from phone motion." — same file |
| Foreground notification is labelled estimated | Pass | "Sabeel is estimating your steps" / "No step counter is reporting, so this number comes from phone motion. Your steps stay on this phone." — [qa-05](qa-05-notification-estimate-en.txt) |
| Persisted status carries the source | Pass | `mode=COUNTING`, `source=MOTION_ESTIMATE`, `source_switches=0`, `source_last_event_ms` fresh — [qa-06](qa-06-status-estimate-en.xml) |
| Arabic Home marks the estimate | Pass | **≈ 97 من 8,000 خطوة** + "عدد تقديري من حركة الهاتف، وليس من عدّاد خطوات مادي." — [qa-07](qa-07-home-walking-estimated-ar.xml) |
| Arabic Diagnostics names the source and copy | Pass | "يتم العدّ (تقديري)"، "حركة الهاتف (تقديري)"، "التحويلات اليوم: لا يوجد" — [qa-08](qa-08-diagnostics-estimated-ar.xml) |
| Service is the only sensor client and reads the accelerometer at UI rate | Pass | samplingPeriod 66667 µs, batchingPeriod 500000 µs, package `StepCounterService` — [qa-11-sensorservice.txt](qa-11-sensorservice.txt) |
| Switch/fallback count is 0 because the estimate is the initial source | Pass | [qa-06](qa-06-status-estimate-en.xml), [qa-10](qa-10-status-estimate-ar.xml) |
| Walk screen labels its step count when the phone has no step sensor | Pass | Walk live, Arabic: "≈ 0" steps plus "لا يحتوي هاتفك على مستشعر للخطوات، لذا تُقدَّر خطوات هذه النزهة من حركة الهاتف." — [qa-17](qa-17-walk-estimated-live-ar.xml) |
| Walk summary keeps the estimate label after saving | Pass | Summary: "≈ 0" steps plus the same note — [qa-18](qa-18-walk-summary-estimated-ar.xml) |
| Creating, running and saving a walk does not crash | Pass | app-filtered crash buffer and logcat are empty — [qa-20](qa-20-crash-buffer-walk.txt), [qa-21](qa-21-logcat-errors-walk.txt) |

## Capture limitation

The `sdk_slim` image returns blank `screencap` frames. `adb exec-out screencap -p` produced a 15,580-byte
PNG and a sampled brightness check found **0 of 1,620 sampled pixels non-black**
([qa-00-screencap-attempt.png](qa-00-screencap-attempt.png)). UI-hierarchy dumps are therefore the visual
evidence, exactly as in the 2026-11-17 smoke pass. There is no PNG screenshot of Home or Diagnostics in
this folder that shows the rendered UI.

## Limitations stated plainly

- **Only the estimate tier ran on this emulator.** The measured ladder (counter → detector), the
  stall probe, the rebase on a counter return, and the clamp are covered by JVM tests
  (`StepPipelineTest`, `StepSourcePolicyTest`, `StepSourceHealthTest`, `MotionStepDetectorTest`) but
  were **not** exercised on hardware. A real counter phone and a real detector-only phone are still
  required before Play submission (BLOCKERS.md B7 remains OPEN).
- **The waveform is synthetic and near-perfect.** 97 estimated steps from 108 injected cycles is evidence
  that the estimate path works and is conservative, not evidence of real-world accuracy. Bag-damped,
  hand-carry and handshake cadences remain physically ambiguous; the "estimated" label is the mitigation.
- **The Arabic notification was not re-rendered.** Switching the in-app language while a foreground
  notification already exists does not rebuild that notification until its next update; the Arabic
  capture shows the English notification text. Home and Diagnostics are Arabic. This is an existing
  locale-refresh limitation, not specific to the estimate tier.
- **Walk estimate numbers were not exercised on the emulator.** The walk screen and summary were
  verified with the estimate labels ("≈ 0" and the note), but the injected motion produced 0 walk
  steps: after the emulator reboot the console injection stopped reaching the accelerometer
  (`sensorservice` showed both services registered at 66.7 ms while the event stream stayed at
  9.81 m/s²). The same detector produced ≈ 97 steps in the main service earlier (qa-03), and the walk
  path is unit-tested, so treat walk estimate accuracy as hardware-pending, not as verified here.
- **The emulator's status bar clock and locale were not controlled**; timestamps in the dumps are the
  device's local time.

## File index

- `qa-00-screencap-attempt.png` — blank framebuffer proof (see above)
- `onboarding-01-language-ar.xml` — first onboarding step in Arabic before choosing English
- `qa-01-home-estimated-en.xml` — Home after onboarding, 0 steps
- `qa-02-home-stationary-30s-en.xml` — Home after 30 s still, still 0 steps
- `qa-03-home-walking-estimated-en.xml` — Home after the injected walk, ≈ 97
- `qa-04-diagnostics-estimated-en.xml` — Diagnostics, English
- `qa-05-notification-estimate-en.txt` — `dumpsys notification --noredact` excerpt (English build)
- `qa-06-status-estimate-en.xml` — `sabeel_service_status.xml`, English pass
- `qa-07-home-walking-estimated-ar.xml` — Home, Arabic
- `qa-08-diagnostics-estimated-ar.xml` — Diagnostics, Arabic
- `qa-09-notification-estimate-ar.txt` — notification dump after the Arabic switch (text still English; see limitations)
- `qa-10-status-estimate-ar.xml` — `sabeel_service_status.xml`, Arabic pass
- `qa-11-sensorservice.txt` — full sensorservice dump (shows the app's accelerometer registration)
- `qa-12-service-state.txt` — `dumpsys activity services com.sabeel.steps`
- `qa-13-sensor-absence.txt` — image identity, no step_counter/step_detector, accelerometer present
- `qa-14-crash-buffer.txt` — empty crash buffer
- `qa-16-logcat-errors.txt` — filtered scan of `adb logcat -d` for FATAL/AndroidRuntime errors (0 matches; the 8.2 MB full dump is not committed)
- `qa-17-walk-estimated-live-ar.xml` — walk screen with estimated steps label (Arabic)
- `qa-18-walk-summary-estimated-ar.xml` — saved-walk summary with the label (Arabic)
- `qa-20-crash-buffer-walk.txt` — app-filtered crash buffer after the walk (empty)
- `qa-21-logcat-errors-walk.txt` — app-filtered FATAL scan after the walk (empty)
