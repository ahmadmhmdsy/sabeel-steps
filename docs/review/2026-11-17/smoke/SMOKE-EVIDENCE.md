# Emulator smoke evidence — 2026-11-17

**Device:** Android Emulator, AVD `api36`, system image `system-images;android-36;aosp_atd;x86_64`
(API 36, ATD = Automated Test Device image), 1080x2400, 420 dpi, emulator 37.2.13.
**Build under test:** signed release APK `dist/Sabeel-1.0-release.apk`
(`versionCode 1`, `versionName 1.0.0`, package `com.sabeel.steps`), installed with
`adb install -t`, all app data wiped before the run.
**Host:** Windows, JDK 21, Android SDK build-tools 36.0.0.

## Why there are no PNG screenshots here

The ATD system image does not composite a guest framebuffer for capture: every
`adb shell screencap` returned a valid 1080x2400 PNG of the same all-black frame
(identical SHA-256 `c35bacdb…af514`, reproduced with `-gpu swiftshader_indirect`,
`-gpu angle_indirect` and a windowed emulator). Those blank images were deleted
rather than kept as misleading evidence. The UI hierarchy dumps below are the
actual evidence: uiautomator reads the live accessibility tree, so they show the
real strings, RTL order, widget bounds and enabled/disabled state that were on
screen at that moment.

## Sequence and files

| Step | File | What it proves |
|---|---|---|
| 1 | `ui-01-onboarding-en.xml` | First launch, English onboarding, language choice visible |
| 2 | `ui-03-home-no-sensor-en.xml` | Home after onboarding: honest `NO_SENSOR` card ("This phone does not report a built-in step counter…"), never a fake zero-counting claim |
| 3 | `ui-04-settings-en.xml` | Settings: goal, units, reminder, body data, language; privacy web row absent because `BuildConfig.PRIVACY_POLICY_URL` is empty |
| 4 | `ui-05-settings-ar.xml` | In-app language switch to Arabic: full RTL mirroring (labels right-aligned, navigation order reversed) |
| 5 | `ui-06-home-ar.xml` | Arabic Home: `مشية`/`سلسلة` terminology, RTL layout, honest no-sensor copy |
| 6 | `ui-07-walk-before-start-ar.xml` | Walk screen with optional-location explanation ("you can walk without location") |
| 7 | `ui-08-walk-live-ar.xml` | Live walk after denying location: `الموقع غير متاح. نحسب الخطوات فقط.`; foreground `WalkTrackingService` is running (`dumpsys activity services`), no crash |
| 8 | `ui-09-settings-scrolled-ar.xml` | "Your data" section: CSV export/import and delete-all |
| 9 | `ui-10-delete-confirm-ar.xml` | Delete-all confirmation with irreversible-data warning |
| 10 | `ui-11-after-delete-onboarding-ar.xml` | After confirming: onboarding again — steps, walks, settings, locale and notification prefs cleared |
| 11 | `ui-12-relaunch-after-delete-en.xml` | Force-stop + relaunch: app falls back to the device language (English), i.e. the explicit Arabic choice was really cleared |
| 12 | `ui-13-home-no-permission-en.xml` | Fresh data (`pm clear`), onboarding completed **without granting anything**: Home shows the Activity-recognition permission card, not a fake counting state |
| 13 | `ui-14-home-after-grant-en.xml` | Granting Activity recognition from that card: permission card disappears and the honest no-sensor status remains; `dumpsys package` shows `ACTIVITY_RECOGNITION: granted=true` |
| 14 | `ui-15-home-device-ar-iq.xml` | Device locale set to `ar-IQ` (no in-app language choice): app starts in Arabic, full RTL Home |
| 15 | `ui-16-history-device-ar-iq.xml` | Arabic History: 7/30-day range chips, Arabic weekday names, RTL chart labels/day rows |
| 16 | `ui-17-insights-device-ar-iq.xml` | Arabic Insights: 7-day average, best day, goal-hit rate, distance estimate copy |

## Results

- No `FATAL EXCEPTION`/AndroidRuntime crash at any point in the run
  (`adb logcat -d -s AndroidRuntime:E` empty).
- Permissions: ACTIVITY_RECOGNITION and POST_NOTIFICATIONS granted through the
  system dialogs on onboarding step 4; location denied on purpose for step 7.
- The ATD image has no `FEATURE_SENSOR_STEP_COUNTER`, so the run exercised the
  honest `NO_SENSOR` path rather than live step counting. Hardware step counting
  still needs a physical device (owner/QA action).
- `com.android.fakesystemapp` is the ATD launcher/system stand-in; it is not part
  of Sabeel and appears only in host-emulator screenshots/dumps if the app is not
  in the foreground.

## Reproduce

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
& $adb install -t dist\Sabeel-1.0-release.apk
& $adb shell am start -n com.sabeel.steps/.MainActivity
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml smoke/ui.xml
```

## Defect found and fixed during this smoke run

The first delete-all attempt cleared steps/walks/settings but left the locale and
service-status preferences and the service erase flag behind: `deleteAll` ran on
`viewModelScope`, and resetting settings flips the root to onboarding, which
destroys the ViewModel — the coroutine was cancelled at `delay(400)` before the
platform-level cleanup. Fixed by running the erase on the application scope under
`NonCancellable` (`AppViewModels.kt`) and re-verified: after delete all three
preference files are `<map />` and a relaunch starts in the device language.
