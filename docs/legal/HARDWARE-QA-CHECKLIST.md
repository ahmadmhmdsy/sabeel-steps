# Hardware QA checklist — step-source ladder and release smoke

Use this together with [BLOCKERS.md](BLOCKERS.md) B7 and [release-evidence-log.md](release-evidence-log.md).
It is written so someone with two phones and no project context can run it and paste results back.

**Status: OPEN — no physical-device run has been recorded.** The emulator can only exercise the
labelled estimate tier (the API 36 slim image has no step counter and no step detector); the measured
counter → detector ladder is unit-tested only.

## 0. What you need

- Phone **A**: Android 13+ (API 33+) with a hardware step counter (`TYPE_STEP_COUNTER`). Nearly every
  phone from 2016+ has one.
- Phone **B**: a different phone, ideally Android 8–10 (API 26–30), with either a counter or at least
  a step detector. If you have a detector-only device (some budget phones), it is the most valuable
  test phone.
- The signed release APK from `dist/` and the matching `dist/SHA256SUMS.txt`.
- A measuring tape or a known 100-step corridor (optional, for estimate accuracy).
- Optional but useful: a second person to count 100 physical steps.

Do **not** reuse the emulator dumps as device evidence; record new files with the phone model and the
APK hash.

## 1. Build identity to record

```powershell
Get-FileHash dist\Sabeel-1.0-release.apk -Algorithm SHA256
Get-FileHash dist\Sabeel-1.0-release.aab -Algorithm SHA256
```

Paste both hashes into the results header. **If a hash does not match `dist/SHA256SUMS.txt`, stop.**

## 2. Install and grant

```powershell
adb install dist\Sabeel-1.0-release.apk
adb shell pm grant com.sabeel.steps android.permission.ACTIVITY_RECOGNITION
adb shell pm grant com.sabeel.steps android.permission.POST_NOTIFICATIONS
```

Open the app, finish onboarding, choose English on phone A and Arabic on phone B (to spread locale
coverage). Then answer one question per phone: **which source does Diagnostics name?** Expect
"Phone's step counter" on a counter phone, "Phone's step detector" on a detector-only phone, and
"Phone motion (estimated)" only on a phone without either measured sensor.

Record:
```powershell
adb shell dumpsys sensorservice | Select-String -Pattern "Step Counter|Step Detector|Accelerometer"
adb shell dumpsys activity services com.sabeel.steps
```

## 3. Measured-source checks (phone A, counter)

| # | Do | Expected | Result |
|---|---|---|---|
| A1 | Note Home steps. Close the app (keep service). Walk 100 steps with the screen off, phone in pocket. Open Home. | Steps rise by roughly 100 (phone vendors differ); Diagnostics source stays the counter and "Last sensor reading" is fresh. | |
| A2 | In Diagnostics, read "Fallbacks today". | `None`. The Home number must **not** show "≈". | |
| A3 | Reboot the phone, unlock, open the app. | Service restarts (BOOT_COMPLETED or the 15-min reconcile), today's total continues, no jump, source still the counter. | |
| A4 | Force-stop the app, reopen it. | Total continues; no double count of the missing window. | |
| A5 | Walk 100 steps again while another app is in front. | Home total keeps rising; notification/status stays truthful. | |
| A6 | Cross local midnight (or set the date forward with the app open, then restore it). | Yesterday is finalised, today starts at 0, source unchanged. | |

Counter reset handling: if the phone reboots while walking, A3 covers the same code path (raw value
drops → baseline reset, today's total kept). Record anything odd verbatim.

## 4. Detector-only checks (phone B, if available)

| # | Do | Expected | Result |
|---|---|---|---|
| B1 | Open Diagnostics. | Source says the step detector, not the estimate. | |
| B2 | Walk 100 steps screen off. | Home and the walk screen rise by an amount in the right range; no "≈" mark. | |
| B3 | Put the phone on a table for 2 minutes. | No phantom steps (or ≤ a couple, vendor-dependent); Diagnostics does not switch source. | |
| B4 | Start a walk, walk 2 minutes, save. | Walk steps are non-zero and the summary is **not** marked estimated. | |
| B5 | Kill the app during a walk, reopen. | Resume offer appears, walk continues, steps do not double count. | |

If phone B has a counter too, it cannot prove the detector tier; say so in the result instead of
pretending B1 passed.

## 5. Estimate-only tier (only if you have a phone with no counter and no detector — or reuse the emulator)

| # | Do | Expected | Result |
|---|---|---|---|
| E1 | Open Home. | Number shows "≈" and the estimate note; Diagnostics says "Phone motion (estimated)". | |
| E2 | Leave the phone still on a table for 2 minutes. | ≈ 0 new steps; no phantom walk. | |
| E3 | Walk 100 counted steps in a normal pocket carry. | Estimate is positive and in the right order of magnitude; record the observed number. It is an estimate: do not fail it for being 10–20% off, but record the delta. | |
| E4 | Start a walk on this phone, walk 2 minutes, save. | Walk live and summary show "≈" and the note; the walk notification says steps are estimated. | |
| E5 | Exported CSV (Settings → export) | Rows contain the same step numbers; note whether the file is marked as estimated (today it is not — record the gap if you consider it a release blocker). | |

## 6. Stall and fallback, on hardware (best effort)

The silence watchdog is hard to trigger deliberately on a healthy phone. Do not fake it. On a counter
phone, these are the reachable parts:

| # | Do | Expected | Result |
|---|---|---|---|
| F1 | Leave the phone stationary for 15+ minutes with the app counting. | No battery cliff, no source switch, no phantom steps; Diagnostics "Fallbacks today" stays None. | |
| F2 | Check battery usage (Settings → Battery) after 24 h of normal use. | No unexplained drain; record %/day and phone model. | |
| F3 | Turn off activity recognition permission in system Settings, wait, reopen the app. | The app explains the missing permission honestly; no crash. | |

A true counter→detector switch needs a device whose counter silently stops while the detector keeps
working; if you cannot produce it, write "not exercisable on this hardware" and leave it unit-tested.

## 7. Result header to paste back

```
Run date/time:
Phone A: model, Android version, ABI, counter/detector present:
Phone B: model, Android version, ABI, counter/detector present:
APK SHA-256:            (must equal dist/SHA256SUMS.txt)
AAB SHA-256:            (must equal dist/SHA256SUMS.txt)
Locale(s):
Tested by:
Files:                  (paths to dumps/screenshots, named by phone and test id)
Summary:                PASS count / FAIL count / NOT EXERCISABLE count
```

Record the same header in an addendum to [release-evidence-log.md](release-evidence-log.md), then
update BLOCKERS B7 from OPEN to CLOSED (or leave it open and say exactly what failed). A FAIL is more
useful than a guessed PASS.
