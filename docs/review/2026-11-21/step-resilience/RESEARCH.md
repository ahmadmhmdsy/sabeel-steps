# Step-counting resilience research (2026-11-21)

Owner request in one line: *the app must never fail to count steps — no matter what happens — and the count must stay real, accurate and fast.*
This file records what the platform actually guarantees, what it does not, and the sources behind every claim. It is the evidence base for the council verdict in `COUNCIL.md` and for the implementation plan `docs/superpowers/plans/2026-11-21-step-manifold-resilience.md`.

Tag legend: **[Doc]** official Android documentation · **[Report]** reproducible community report · **[Anecdote]** single-user/OEM observation. Platform claims that could not be verified from official docs are marked as such.

## 1. The two hardware step sensors

1. **[Doc]** `TYPE_STEP_COUNTER` returns the number of steps since the last reboot *while activated*: "Returns the number of steps taken by the user since the last reboot while activated... reset to zero only on a system reboot." and "Application needs to stay registered for this sensor because step counter does not count steps if it is not activated." It uses `REPORTING_MODE_ON_CHANGE`, API 19+, requires `ACTIVITY_RECOGNITION`. It keeps counting while the application processor is suspended and reports the aggregate on wake.
   <https://developer.android.com/reference/android/hardware/Sensor#TYPE_STEP_COUNTER>
2. **[Doc]** `TYPE_STEP_DETECTOR` emits one event per detected step (value `1.0`, timestamp = foot strike), `REPORTING_MODE_SPECIAL_TRIGGER`; the docs say it is "only for detecting every individual step... If you only need aggregate number of steps... register for TYPE_STEP_COUNTER". Sampling-period arguments are meaningless for special-trigger sensors, and the hardware FIFO/batching arguments do not apply.
   <https://developer.android.com/reference/android/hardware/Sensor#TYPE_STEP_DETECTOR>
3. **[Doc]** `registerListener()` returns "true if the sensor is supported and successfully enabled". `getDefaultSensor()` may return null on phones without the sensor; a missing sensor produces silence, never an exception. Always check the boolean and the null.
   <https://developer.android.com/reference/android/hardware/SensorManager#registerListener(android.hardware.SensorEventListener,%20android.hardware.Sensor,%20int,%20int)>
4. **[Doc]** Batching semantics for FIFO-capable sensors: events may stay in the hardware FIFO up to `maxReportLatencyUs`; once one event must be reported the whole FIFO is delivered sequentially; `0` means deliver as soon as possible; when `maxFifoEventCount() == 0` there is no FIFO and the latency argument is ignored.
   <https://developer.android.com/reference/android/hardware/SensorManager#registerListener(android.hardware.SensorEventListener,%20android.hardware.Sensor,%20int,%20int)>
5. **[Report]** Step counters can go "stuck" or silent even inside a foreground service; several threads show no events while the screen is off and `PARTIAL_WAKE_LOCK` does not fix it; some OEMs only update the counter while the app is focused.
   <https://stackoverflow.com/questions/43429248/android-step-counter-sensor-stops-counting-up> · <https://stackoverflow.com/questions/40056449/android-type-step-counter-sensor-does-not-work-while-screen-off> · <https://stackoverflow.com/questions/62499192/android-steps-counter-sensor-is-updated-only-if-application-is-in-focus-on-some>
6. **[Doc]** The most common "counter returns nothing" cause on API 29+ is the missing `ACTIVITY_RECOGNITION` runtime permission, introduced in Android 10 specifically for step detection.
   <https://developer.android.com/about/versions/10/privacy/changes> · <https://stackoverflow.com/questions/59592685/android-10-cannot-register-sensor-type-step-counter>

## 2. Foreground service and background rules

7. **[Doc]** Since Android 9 a background app does not receive on-change sensor events; a foreground service is mandatory. `TYPE_STEP_COUNTER` is on-change, so it is covered by this rule.
   <https://developer.android.com/about/versions/pie/android-9.0-changes-all>
8. **[Doc]** On API 34+ a foreground service of type `health` needs `FOREGROUND_SERVICE_HEALTH` plus at least one qualifying prerequisite: `HIGH_SAMPLING_RATE_SENSORS` declared, or a granted runtime permission from `BODY_SENSORS` (<= API 35), `READ_HEART_RATE`, `READ_SKIN_TEMPERATURE`, `READ_OXYGEN_SATURATION`, `ACTIVITY_RECOGNITION`. `ACTIVITY_RECOGNITION` alone is sufficient, and only the body-sensor/read permissions are while-in-use restricted — a pure step health FGS is not blocked by the while-in-use clause. An undeclared type throws `MissingForegroundServiceTypeException`.
   <https://developer.android.com/develop/background-work/services/fg-service-types>
9. **[Doc]** Android 15's `BOOT_COMPLETED`-to-FGS restrictions block `dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection` and `microphone`. `health` is not in the list, so restarting this service from boot stays legal (still test per OEM). The Android 15 `dataSync` 6-hour/24-hour cap therefore does not apply to this service type.
   <https://developer.android.com/about/versions/15/behavior-changes-15> · <https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start>
10. **[Doc]** Android 16 adds no extra health-type limit; it reorganizes body/health runtime permissions under `android.permissions.health.*` and counts jobs concurrent with an FGS against the job quota.
    <https://developer.android.com/about/versions/16/behavior-changes-16>
11. **[Doc]** Denying `POST_NOTIFICATIONS` does not stop a foreground service; the notification is hidden from the drawer but remains in the Task Manager. Any "paused" alert that depends on a notification must be treated as best-effort.
    <https://developer.android.com/develop/ui/views/notifications/notification-permission>
12. **[Doc]** WorkManager has a 15-minute minimum periodic interval, is deferred in Doze, and an app in the "rare" standby bucket gets roughly 10 minutes of job runtime per 24 h. It cannot count steps; it can reconcile, and it can do a Health Connect read when the FGS cannot start.
    <https://developer.android.com/develop/background-work/services/alarms/schedule> · <https://developer.android.com/topic/performance/power/power-details>

## 3. Health Connect

13. **[Doc]** Health Connect exposes `StepsRecord` (interval record) and `READ_STEPS`; reads return aggregates, not a live feed. Background reads need `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` **and** `FEATURE_READ_HEALTH_DATA_IN_BACKGROUND == FEATURE_STATUS_AVAILABLE`; the docs' own example is a 1-hour `PeriodicWorkRequest`. Background-read is a sensitive grant and adds Play review scope.
    <https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data>
14. **[Doc]** On Android 14+ with SDK Extension >= 20, Health Connect itself tracks on-device steps (using `TYPE_STEP_COUNTER`) and exposes them through the unfiltered aggregate; writes are batched about once per minute and tracking is active only while some app holds `READ_STEPS`. The unfiltered aggregate is not affected by the June 2026 DataOrigin change from `android` to per-device SPNs.
    <https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data> · <https://developer.android.com/about/versions/14/behavior-changes-14>
15. **[Doc]** Health Connect client 1.1.0 (the version in this project) exposes `HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND` and `HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND`, verified in the installed AAR; `HealthConnectException` has `ERROR_REMOTE`, `ERROR_RATE_LIMIT_EXCEEDED` etc., and quota exhaustion can surface as `IllegalStateException`. Every call must be wrapped.
16. **[Doc]** Health Connect is absent on most emulators and on Android <= 13 without the provider app; Android 14+ devices may have the provider built in. It can never be assumed present.

## 4. What actually breaks shipped pedometers

17. **[Report]** The recurring shipped-pedometer bugs are arithmetic and state bugs, not sensor bugs: j4velin/Pedometer reports negative daily totals after a counter reset (Huawei P20 Android 9; Honor 5C), a ~10x overcount in one day, and 32k–80k step days caused by timezone changes.
    <https://github.com/j4velin/Pedometer/issues/165> · <https://github.com/j4velin/Pedometer/issues/143> · <https://github.com/j4velin/Pedometer/issues/121> · <https://github.com/j4velin/Pedometer/issues/177>
18. **[Report/Anecdote]** OEM process killers are vendor-ranked and unfixable from inside the app: Xiaomi devices stopping foreground services, dontkillmyapp scores Xiaomi/Samsung/OnePlus/Huawei 5/5. Only user-side autostart/battery settings help; there is no Play-legal way to force-restart after a force-stop.
    <https://stackoverflow.com/questions/57976116/xiaomi-devices-are-stopping-foreground-services> · <https://dontkillmyapp.com/samsung>
19. **[Doc]** Android 14 requires `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED` for context-registered receivers; a manifest receiver with `exported=false` keeps receiving protected system broadcasts (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) and nothing else.
    <https://developer.android.com/about/versions/14/behavior-changes-14>

## 5. What the platform cannot do (stated up front)

- No public API exposes per-OEM step-counter reliability. Google Issue Tracker could not be searched without a browser in this environment; OEM claims above are community evidence, not vendor statements.
- A since-boot counter has no per-step timestamps. If the process is dead for an hour, the counter aggregate recovers the count but not the minute-by-minute distribution; active minutes for that gap cannot be attributed.
- Non-wake-up counter/detector events can be lost while the application processor is suspended; the counter's cumulative nature heals the total, the detector's per-step events do not.
- No app can restart itself after a user force-stop or an OEM "deep clean"; boot, package-replace and app-open are the only legal re-arms.
- Health Connect is a floor/cross-check with sync lag, not a live sensor; background reads need a separate sensitive grant.
