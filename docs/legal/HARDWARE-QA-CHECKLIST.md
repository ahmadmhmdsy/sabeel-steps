# Hardware QA checklist — step manifold (v1.2) and release smoke

Use this together with [BLOCKERS.md](BLOCKERS.md) B7 and [release-evidence-log.md](release-evidence-log.md).
It is written so someone with two phones and no project context can run it and paste results back.

**Status: OPEN — no physical-device run has been recorded.** The emulator can only exercise the
labelled estimate tier and the Health Connect opt-in card (the API 36 slim image has no step counter and
no step detector, and no watch steps to read); the counter+detector manifold, the reboot/catch-up
recovery and the Health Connect floor are unit-tested only. Run **§2 Step Zero first**: if it fails,
the counter+detector fusion claim is not trustworthy and the detector must be demoted to
liveness-only before the remaining measured tests are meaningful.

## 0. What you need

- Phone **A**: Android 13+ (API 33+) with a hardware step counter (`TYPE_STEP_COUNTER`). Nearly every
  phone from 2016+ has one.
- Phone **B**: a different phone, ideally Android 8–10 (API 26–30), with either a counter or at least
  a step detector. If you have a detector-only device (some budget phones), it is the most valuable
  test phone.
- Phone **C** (HP1–HP6): a phone with **no counter and no detector** (the estimate-tier phone), with the
  **Health Connect** app installed and a watch or another app writing steps to it. If phone C has no
  Health Connect data, HP1–HP6 cannot be exercised — write "not exercisable", do not fake it.
- The signed release APK from `dist/` and the matching `dist/SHA256SUMS.txt`.
- A measuring tape or a known 100-step corridor (optional, for estimate accuracy).
- Optional but useful: a second person to count 100 physical steps.

Do **not** reuse the emulator dumps as device evidence; record new files with the phone model and the
APK hash.

## 1. Build identity to record

```powershell
Get-FileHash dist\Sabeel-1.1-release.apk -Algorithm SHA256
Get-FileHash dist\Sabeel-1.1-release.aab -Algorithm SHA256
```

Paste both hashes into the results header. **If a hash does not match `dist/SHA256SUMS.txt`, stop.**

## 2. Step Zero — falsification test (run before A1)

This is the Maestro ruling from
[COUNCIL.md](../review/2026-11-21/step-resilience/COUNCIL.md). Use phone A (hardware counter) and a
physical count; a second person counting is ideal.

| # | Do | Expected | Result |
|---|---|---|---|
| Z1 | Note Home's number. Walk exactly 300 steps beside the physical counter. | Home rises by roughly the counted number. Record the raw Home delta and the counter's own delta (Diagnostics last reading) separately. | |
| Z2 | Compare the fused app total with the physical count. | abs(appDelta - 300) / 300 <= 2% and the hardware detector's own contribution is between 90% and 110% of the counter's delta. | |
| Z3 | Walk 100 steps on a phone where the counter is not delivering but the detector is (a detector-only phone, or a device whose counter HAL is silent) and confirm the detector is the display source. | Detector delivers 90–110 steps; Home does not show "≈"; Diagnostics source = "Phone's step detector". | |

**Z3 note:** do not force-stop the app to suppress the counter (that also kills the detector). If you
cannot produce a counter-silent device, write "not exercisable".

**Decision rule:** if Z2 or Z3 misses the tolerance, record the raw numbers and demote the detector to
liveness-only (one constant) before trusting anything else. Do not average away a failure.

## 2b. Install and grant

```powershell
adb install dist\Sabeel-1.1-release.apk
adb shell pm grant com.sabeel.steps android.permission.ACTIVITY_RECOGNITION
adb shell pm grant com.sabeel.steps android.permission.POST_NOTIFICATIONS
# Health Connect (phone C only): the grant normally comes from the system dialog; a manual grant is
# possible on some builds but does not replace the user-facing opt-in check:
adb shell pm grant com.sabeel.steps android.permission.health.READ_STEPS
```

Open the app, finish onboarding, choose English on phone A and Arabic on phone B (to spread locale
coverage). Then answer one question per phone: **which source does Diagnostics name?** Expect
"Phone's step counter" on a counter phone (the detector runs too; the counter is displayed as the
stronger measured source), "Phone's step detector" on a detector-only phone, "Health Connect
(synced)" when the granted floor is ahead, and "Phone motion (estimated)" only when no measured
source is delivering and no synced floor is available.

Record:
```powershell
adb shell dumpsys sensorservice | Select-String -Pattern "Step Counter|Step Detector|Accelerometer"
adb shell dumpsys activity services com.sabeel.steps
```

## 3. Measured-source checks (phone A, counter)

| # | Do | Expected | Result |
|---|---|---|---|
| A1 | Note Home steps. Close the app (keep service). Walk 100 steps with the screen off, phone in pocket. Open Home. | Steps rise by roughly 100 (phone vendors differ); Diagnostics display source stays the counter and "Last event" is fresh. | |
| A2 | In Diagnostics, read the source switches and storage rows. | Switches stay 0 or small; the Home number must **not** show "≈"; Saving = OK. | |
| A3 | Reboot the phone, unlock, open the app, walk 100 steps. | Service restarts (BOOT_COMPLETED or the 15-min reconcile); today's total continues; no phantom jump at reboot; the post-reboot walk counts; display source still the counter. | |
| A4 | Force-stop the app, walk 100 steps, reopen it. | The counter catch-up is recovered: Home jumps by roughly the 100 missed steps (intended in v1.2; this differs from v1.1), no double count of before-force-stop steps. | |
| A5 | Walk 100 steps again while another app is in front. | Home total keeps rising; notification/status stays truthful. | |
| A6 | Cross local midnight (or set the date forward with the app open, then restore it). | Yesterday is finalised, today starts at 0; the post-midnight delta is attributed to the new day; no negative or frozen total. | |
| A7 | Open Diagnostics after 2+ minutes without walking. | The last-event age is shown honestly (fresh vs stale); no fake "counting" claim if the service was killed. | |
| A8 | Kill only the app process from developer options (not force-stop), walk 100 steps, reopen. | The counter catch-up is recovered; process death alone loses nothing on a counter phone. | |

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
pretending B1 passed. A detector-only phone is also the place to check that detector catch-up while
the process was dead is **not** promised (the detector has no cumulative value): record what happens
and do not fail it.

## 5. Estimate-only tier (only if you have a phone with no counter and no detector — or reuse the emulator)

| # | Do | Expected | Result |
|---|---|---|---|
| E1 | Open Home. | Number shows "≈" and the estimate note; Diagnostics says "Phone motion (estimated)". | |
| E2 | Leave the phone still on a table for 2 minutes. | ≈ 0 new steps; no phantom walk. | |
| E3 | Walk 100 counted steps in a normal pocket carry. | Estimate is positive and in the right order of magnitude; record the observed number. It is an estimate: do not fail it for being 10–20% off, but record the delta. | |
| E4 | Start a walk on this phone, walk 2 minutes, save. | Walk live and summary show "≈" and the note; the walk notification says steps are estimated. | |
| E5 | Exported CSV (Settings → export) | Rows contain the same step numbers **and** a non-zero `estimated_steps` for estimated days; History day rows and saved walks show the "estimated" badge. | |
| E6 | If you have a phone with **no motion sensor at all** (no accelerometer), start a walk. | The walk is not blocked and GPS distance still records, but walk steps stay 0 and nothing claims measured steps; a Health Connect synced total is never presented as walk steps. | |

## 5b. Health Connect floor (phone C, no counter/detector, watch/app steps in Health Connect)

Prerequisite: Health Connect installed, the watch/app has written steps for today, the app is on the
**release APK above**. The tier is opt-in and only offered on phones with **no** counter and **no**
detector; if the card is missing on phone C, record why (Health Connect unavailable, or a sensor was
detected) and skip the rest.

| # | Do | Expected | Result |
|---|---|---|---|
| HP1 | Open Settings. | A "Health Connect" card appears (only on this no-sensor phone) with a "Use synced steps" row and a read-only note; on phones A/B the card must be absent. | |
| HP2 | Tap Connect and read the system Health Connect screen. | Only step **read** access is requested (no write); declining returns to the app without a crash and the estimate stays live. | |
| HP3 | Grant, go to Home, wait ~2 min (first read 5 s after the service starts). | Home shows the synced total with the Health Connect note (not "≈"); Diagnostics source = "Health Connect (synced)", "Last sync from Health Connect" is fresh, "Fallbacks today" unchanged. Compare with the Health Connect app's own today total for the same date: app steps must not be lower. | |
| HP4 | Walk with the phone in hand while the watch is **not** connected/updating. | Within ~10–12 min (watchdog probe) the app falls back to the labelled estimate and keeps the highest total; no double count when the watch catches up later (the floor only raises). | |
| HP5 | Revoke step access in Health Connect settings (or Android Settings → Health Connect → App permissions), reopen the app. | The app returns to the labelled estimate; no crash; Diagnostics no longer claims synced; the Settings card offers Connect again. | |
| HP6 | Open Health Connect → Data and access → App permissions (or the per-app usage screen). | Sabeel shows **read** access only; nothing was written by Sabeel; the privacy-usage activity opens without a crash. | |
| HP7 | Export CSV on this phone. | `estimated_steps` is 0 for days fully synced; if a day mixed estimate and sync, the estimate portion is preserved and `0 <= estimated_steps <= steps`. | |

Record the Health Connect app version and the watch/app data source in the results header.

## 6. Manifold-specific and stall checks, on hardware (best effort)

The manifold means there is no source switch to observe on a healthy counter phone; what matters is
that the other sources neither double count nor freeze the day. These are reachable:

| # | Do | Expected | Result |
|---|---|---|---|
| M1 | Install the previous release (v1.0.0) over existing data, collect a day, then install this release over it (not uninstall). | The day's total survives; a day that had estimated steps keeps the "≈" marker after the upgrade (preserved from the daily row); no negative estimate. | |
| M2 | Compare Home with the phone's own step counter reading after a long walk. | The app total is close to the counter's since-boot delta; the detector contribution does not add on top. | |
| M3 | Check Diagnostics after a walk on a counter phone. | Display source = counter; switches do not grow with walking; no "≈" unless the estimate had already been used. | |
| M4 | On a Health Connect phone, let the watch sync a large total while the phone counter is lower. | Home takes the larger floor once, never sums the two; the "≈" portion shrinks if it existed. | |
| M5 | Reboot the phone with Health Connect granted and the app stopped. | After boot/service start the HC floor is read again (or on the next open); the app does not crash and does not start a health FGS without the activity permission. | |

On a counter phone, also:

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
Phone C: model, Android version, ABI, counter/detector present, Health Connect app version, step data source:
Step Zero (Z1–Z3): PASS/FAIL/NOT EXERCISABLE + raw deltas:
Manifold (M1–M5): PASS/FAIL/NOT EXERCISABLE:
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