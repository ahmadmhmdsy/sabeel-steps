# Evidence — step manifold resilience (v1.2), 2026-11-21

Self-verification by the building agent (not an independent legal batch). Companion documents:
[RESEARCH.md](RESEARCH.md), [COUNCIL.md](COUNCIL.md), plan
[`docs/superpowers/plans/2026-11-21-step-manifold-resilience.md`](../../../superpowers/plans/2026-11-21-step-manifold-resilience.md).
Raw emulator dumps are in [evidence/](evidence/).

## 1. Artifacts measured

| Artifact | Bytes | SHA-256 | Notes |
|---|---|---|---|
| Debug APK | 13,581,629 | `117b51fd7d120500ebcbaf5781b0e62446c7da13eeff432eb2ae9014b89714dc` | `assembleDebug`, mtime 2026-11-21 23:44:57 +03:00 |
| Release APK | 1,864,306 | `00088fdc172cfafe17bc7ab90d112250d30ea2bae5ca1ef3de95f5cdbe518de1` | signed upload cert `7b12ad6b…6152d`, `apksigner verify` OK, `zipalign -c -P 16` OK |
| Release AAB | 3,992,650 | `92d4e5da2bb2054b8156b88a2b8fee40335831e1ed0b0594f2aca526982f33ab` | `jarsigner -verify` exit 0 (JarInputStream warnings only) |
| R8 mapping | 32,136,472 | (file) | `dist/mapping/mapping-1.1.0.txt` |

Version: `versionCode=2`, `versionName=1.1.0` (first bump; previous build was 1.0.0). The release APK/AAB
are also in `dist/` with `dist/SHA256SUMS.txt`; nothing was uploaded to Play.

## 2. Commands run (all from the repo root, JDK 21, build-tools 36.0.0)

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot'
$env:ANDROID_HOME='C:\Android\Sdk'; $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --no-daemon --console=plain
# BUILD SUCCESSFUL in 3m 50s
# 127 tests, 0 failures, 0 errors, 0 skipped, 10 suites
# lintDebug: 0 errors, 46 warnings, 1 hint
.\gradlew.bat :app:assembleRelease :app:bundleRelease --no-daemon --console=plain
# BUILD SUCCESSFUL in 2m 24s; lintVitalRelease passed
```

Unit-test count is 127, not the plan's optimistic ≥140: the deleted v1.1 source-policy/health suites
(15 tests) were replaced by the manifold invariant suite (`StepPipelineTest`), `SilenceWatchdogTest`
and the migration/marker tests.

## 3. Emulator run (emulator-5554, API 36, x86_64 ATD image)

Environment: `Android SDK built for x86_64`, API 36. The image has **no `TYPE_STEP_COUNTER` and no
`TYPE_STEP_DETECTOR`**, but it does expose a `Goldfish 3-axis Accelerometer`, so the only counting
path reachable here is the labelled estimate (as in the v1.1 evidence). Health Connect has no step
data on this image and the grant could not be completed earlier, so the HC floor is not exercised.

| # | Action | Observed | Result |
|---|---|---|---|
| E1 | `adb uninstall` then `install -r app-debug.apk`; grant ACTIVITY_RECOGNITION + POST_NOTIFICATIONS; launch | Onboarding step 1/4 rendered; no crash (crash buffer has no `com.sabeel.steps` entry) | PASS |
| E2 | Advance all 4 onboarding steps | Step 4 shows "Activity recognition: Allowed"; tapping "Start walking" opens Home and starts the count | PASS |
| E3 | Inspect the FGS | `StepCounterService`: `isForeground=true`, `foregroundId=4101`, manifest type `health` (`types=0x100`), ongoing "Sabeel" notification | PASS |
| E4 | Leave the phone stationary | Home stays at 0 steps / "0 active minutes"; no phantom count | PASS |
| E5 | Inject a synthetic 1.8 Hz walking waveform for ~55 s (`adb emu sensor set acceleration` alternating 0:0:9.8 and 3.5:0:12.8) | Diagnostics: 56 steps, source "Phone motion (estimated)", status "Counting (estimated)", Saving "Working", Activity recognition "Allowed"; notification title "Sabeel is estimating your steps"; Home shows the estimate note | PASS (estimate plumbing only) |
| E6 | `am force-stop`, reopen the app, inject a second ~20 s waveform | Persisted total stayed 56 after the force-stop and the service restarted; the second walk raised Home distance 40 m → 54 m (≈19 more estimated steps), i.e. new evidence added on top of the restored total instead of resetting it | PASS (persistence; counter catch-up not testable here) |
| E7 | Settings → Delete all data → confirm | Onboarding step 1/4 returns, the FGS is gone (`dumpsys activity services com.sabeel.steps` empty), crash buffer still has no Sabeel entry | PASS |
| E8 | Complete onboarding again, `adb reboot`, wait for boot | `BootReceiver` runs; log: `Background started FGS: Allowed … code:BOOT_COMPLETED`; `StepCounterService` is foreground after boot with no Sabeel crash | PASS |
| E9 | Repeat reboot while onboarding was still incomplete (after E7) | Boot receiver runs but no service starts — correct, counting is not enabled until onboarding completes | Expected |

Raw files: `evidence/ui-e2e-*.xml` (uiautomator hierarchy dumps; the ATD image returns blank
`screencap` frames, so no PNG evidence), `evidence/logcat-boot-fgs.txt`,
`evidence/dumpsys-service-boot.txt`, `evidence/crash-buffer-sabeel.txt` (empty).

## 4. What this evidence does NOT prove

- **No counter and no detector on the image.** The counter catch-up after process death, the
  reboot rebase, the detector ledger and the max-fusion invariants are proven by unit tests only;
  the physical **Step Zero** test ([COUNCIL.md](COUNCIL.md), hardware checklist §2) is still the
  gate before the fused measured claim can be trusted.
- **No Health Connect step data.** The read path, floor reconciliation, background-read permission
  and revoke path remain unit-tested only (hardware checklist §5b).
- **The estimate waveform is synthetic.** E5 proves the estimate is counted, labelled, persisted
  and notified; it does not prove real-world estimate accuracy (no such claim is made).
- **No storage-fault injection.** The fault is surfaced in Diagnostics by code inspection and the
  unit-level persistence tests, not by breaking the emulator's disk.
- **No midnight crossing.** Day rollover is unit-tested; the emulator test did not change the date
  mid-run.
- **No OEM power-manager kill / force-stop auto-restart.** Android does not allow the app to
  restart after a force-stop; the design recovers on the next open/boot/package replace, and that
  recovery (boot) is shown in E8.
