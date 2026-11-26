# Sabeel 1.3.1-field — tester card / بطاقة المختبر

**Build:** versionName `1.3.1-field` · versionCode **4** · package `com.sabeel.steps` · signed with the **same release/upload key** as 1.3.0.
**Artifact:** `dist/Sabeel-1.3.1-field-release.apk` — 2,164,470 bytes — SHA-256 `4c6a132f0d0e2782fb3497491cf637ab1a7a7b36af46b41cdc0eebc4e090d979`.
**Signing certificate SHA-256:** `7b12ad6bfaf07817e76a2539954f7b4bde2e13f6b33454824830102566b6152d` (identical to 1.3.0).
**Status:** internal field diagnostics build, **not published** on GitHub Releases or Google Play. The public 1.3.1 (when approved) will be versionCode 5 and will not include the recorder.

---

## 1) What this build adds (and what it does not)

| # | Change | Why |
|---|---|---|
| 1 | **Flight recorder** — a private, rotating JSONL event log (2 × 512 KB, 14-day retention) recording service start/stop, heartbeats with power/doze state, sensor-arrival markers, write results, worker runs, boot attempts, midnight alarm, crash class + 12 frames, and process-end facts. | The old Diagnostics screen showed *state*, not *history*. A stopped service leaves no trace except a stale time. |
| 2 | **Post-mortem capture** — on app open, before anything else runs, Sabeel reads Android's `ApplicationExitInfo` (API 30+) for the previous process and persists it. | Reopening the app rewrites the heartbeat; this preserves *why* the process ended (force-stop, low memory, freezer, crash, user stop) and the gap boundary. |
| 3 | **One-tap full report** — Diagnostics → **Share full report** → preview of `report.txt` → confirm → ZIP `{report.txt, meta.json, events.jsonl}` via the system share sheet. **Delete diagnostic data** wipes the log and facts. | Gives support a reproducible artifact instead of a screenshot. |
| 4 | **Honest pause banner** — when counting is stopped by the system, the Home card now uses the error container and a secondary **Share report** action; a foreground-start refusal posts a **visible** "Step counting stopped" notification (alerts channel, not the silent 12-hour reminder). | A dead counter must look dead. No silent failure. |
| 5 | **Worker self-heal** — the 15-minute `ReconcileWorker` and `BootHealthWorker` re-start the counting service **only when Sabeel is exempt from battery optimization** (the documented background-FGS-start exemption). If not exempt, they post the visible notification instead of attempting an illegal start. | Legal recovery where Android permits it; no crash loops where it does not. It never invents steps. |
| 6 | **Midnight baseline alarm** — one inexact `AlarmManager.setAndAllowWhileIdle` wake-up at ~00:05 local time (no `SCHEDULE_EXACT_ALARM`). If counting should be on, the device is exempt, and the service is dead, it starts the service so the new day gets a real baseline. If the alarm does not run, the existing rule still **drops** the unattributable cross-midnight delta. | The "today = 0" symptom happens because no sample is processed at midnight. This gives Android a legal nudge; it never guesses steps for time it did not see. |

**Not changed:** step math, source priority, the no-INTERNET-permission rule, and the honest limit that steps taken while Android kept Sabeel dead cannot be recovered.

---

## 2) Step Zero — do this first (5 minutes)

This is the falsification test. Do it **before** normal use so the log is clean.

1. Tap the APK and install **as an update** (do **not** uninstall 1.3.0). The app keeps its database, settings, and battery-optimization exemption.
2. Open Sabeel once and confirm counting is ON (Home ring / status says counting).
3. **Settings → Apps → Sabeel → Force stop.** Confirm.
4. Leave the phone alone for **at least 2 minutes** (screen off is fine).
5. Open Sabeel again. Go straight to **Diagnostics** (Home red card → *Share report*, or Settings → Diagnostics). **Do not tap "Restart counting" first.**
6. Scroll to the bottom → **Share full report** → read the preview → **Share report** → send the ZIP to the developer (WhatsApp / Telegram / email).
7. Write down: the exact time you force-stopped, the phone brand/model, and the Android version.

**Pass criteria (checked in `report.txt`):**

- `[PROCESS]` shows a **new pid and process start time** after the gap.
- `[PROCESS END]` shows an exit record whose time matches your force-stop. Expected reason: **`USER_REQUESTED` with the description mentioning `force stop`** on Android 12+, or `USER_STOPPED` on older versions (an OEM may use another `REASON_*`). Android 10 and older keeps no such record — that is expected.
- `[HEARTBEAT]` shows the last heartbeat **before** the force-stop (or "none" if the service died before its first heartbeat).
- `[START]` shows that start was requested when you reopened the app.
- `[COUNTING]` today total did not jump: no steps are invented for the dead window.

If those lines appear, the recorder proves it can see Android's kill and the boundary of the lost window. That is the evidence we could not get from screenshots.

---

## 3) Normal use after Step Zero (48–72 hours)

Use the phone normally. Whenever the number resets or stays frozen:

1. Open Sabeel → Diagnostics → **Share full report**.
2. Send the ZIP, and say roughly when you noticed the problem (date + time) and whether you rebooted the phone.

Every report is a snapshot of the last events; sending two reports (one right after a reset, one at the end of the day) is more useful than one.

---

## 4) Privacy — what the report contains

- **Included:** device model/brand, Android and app versions, today's step total and stored pipeline total, status names, event timestamps, heartbeat/sensor/write/worker/start/alarm times, power state (battery %, charging, screen, doze, power-save, standby bucket, background restriction), permission states, exit reasons, crash **class + stack frames** (no message), event log.
- **Not included:** location, contacts, notifications content, installed-app list, account identifiers, raw counter values per sensor event (only "a sample arrived"), free-text fields.
- **Only shared when you tap Share.** The app has **no INTERNET permission**; nothing is uploaded automatically. The recorder is local to the app's private storage and can be wiped from Diagnostics.

---

## 5) Known limits (honesty)

1. Steps taken while Sabeel was dead are gone; this build observes and documents the failure, it does not resurrect those steps.
2. `ApplicationExitInfo` exists from Android 11; on Android 8–10 the report falls back to Sabeel's own persisted facts (gap + last clean stop), which still proves *when* counting stopped but not always *why*.
3. OEM "deep clean" / force-stop can also block the 00:05 alarm and the 15-minute worker. In that case no tool inside the app can run — the next step would be one `adb bugreport` from a computer.
4. This is a diagnostics build: it writes more to private storage and shows extra controls. It is not the public release.
5. If reports are still inconclusive after ~72 hours and several reproductions, we stop building tools and take one stock-Android `bugreport` with timestamps.

---

## 5b) Validation so far (emulator, 2026-11-26)

The exact Step Zero sequence above was run on an **API 36 emulator** with the field build (debug-signed copy over an existing install, so data and settings were kept):

- Force stop at `18:03:39`; app reopened at `18:06:06` — a **173 s dead window**, no steps processed.
- `report.txt` `[PROCESS]` showed the new pid `26992` and start time `18:06:06`; `[PROCESS END]` showed `reason=USER_REQUESTED(10)` with `desc=[FORCE STOP] stop com.sabeel.steps …`; `[COUNTING]` stayed at 0 (no invented steps).
- The preview dialog rendered the real `report.txt`, and **Share report** opened the system chooser with `Sharing 1 file` and the ZIP name — the FileProvider grant works.

That run also caught two bugs, both fixed before this APK was built: the post-mortem preferred its stored snapshot and hid every later force-stop (it now always refreshes from `ApplicationExitInfo`), and the report printed device **uptime** as the process age. **Physical-device Step Zero is still pending** — the emulator proves the recorder and the report pipeline, not the owner's OEM behaviour.

## 6) For the developer (build commands)

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleRelease :app:bundleRelease "-Psabeel.fieldDiagnostics=true"
```

The flag produces `BuildConfig.FIELD_DIAGNOSTICS = true`, versionName `1.3.1-field`, versionCode 4. Without the flag, versionName is `1.3.1` and the recorder UI is dormant (the public build must bump to versionCode 5).

---

# بالعربية — بطاقة المختبر

**النسخة:** `1.3.1-field` · رقم البناء **4** · الحزمة `com.sabeel.steps` · موقّعة بنفس **مفتاح الإصدار** المستخدم في 1.3.0.
**الملف:** `dist/Sabeel-1.3.1-field-release.apk` — 2,164,470 بايت — SHA-256 `4c6a132f0d0e2782fb3497491cf637ab1a7a7b36af46b41cdc0eebc4e090d979`.
**الحالة:** بناء تشخيصي داخلي، **غير منشور** على GitHub أو Google Play. النسخة العامة 1.3.1 (عند الموافقة) ستكون برقم 5 وبدون مسجّل الأحداث.

## 1) ما الجديد في هذا البناء

1. **مسجّل أحداث (Flight recorder):** سجل خاص داخل التطبيق (JSONL، ملفان × 512 ك.ب، يُحفظ 14 يومًا) يسجّل تشغيل/إيقاف الخدمة، ونبضات القلب مع حالة الطاقة، ووصول عيّنات الحسّاس، ونتائج الكتابة، وتشغيل العامل الدوري، ومحاولات الإقلاع، ومنبّه منتصف الليل، ونوع الانهيار وأطر المكدّس، وأسباب إنهاء العملية. الغرض: معرفة **ماذا حدث** لا فقط الحالة الحالية.
2. **تشريح ما بعد الموت (Post-mortem):** عند فتح التطبيق، وقبل أي شيء آخر، يقرأ سبِيل `ApplicationExitInfo` من أندرويد (11+) ويحفظ سبب إنهاء العملية السابقة (إيقاف قسري، ذاكرة منخفضة، تجميد، انهيار) ووقت الفجوة. لأن فتح التطبيق يعيد كتابة آخر نبضة، وهذا يضيّع الدليل.
3. **تقرير بضغطة واحدة:** التشخيص → **شارك التقرير الكامل** → معاينة `report.txt` → تأكيد → ملف ZIP فيه `report.txt` و`meta.json` و`events.jsonl` عبر مشاركة النظام. وزر **احذف بيانات التشخيص** يمسح السجل.
4. **تنبيه صادق بالتوقف:** بطاقة الرئيسية عند توقف العدّ تظهر بألوان الخطأ مع زر ثانوي **شارك التقرير**، وإذا رفض أندرويد تشغيل الخدمة تظهر **إشعار مرئي** "توقّف عدّ الخطوات" (قناة التنبيهات، لا التذكير الصامت كل 12 ساعة).
5. **إصلاح ذاتي من العامل الدوري:** عامل المزامنة (كل 15 دقيقة) وعامل فحص الإقلاع يعيدان تشغيل الخدمة **فقط** إذا كان سبِيل مستثنى من تحسين البطارية (وهو الاستثناء الموثّق لتشغيل خدمة أمامية من الخلفية). إن لم يكن مستثنى، يُظهر الإشعار المرئي بدل محاولة غير مسموحة. لا يخترع خطوات أبدًا.
6. **منبّه خط الأساس عند منتصف الليل:** استيقاظ غير دقيق عبر `setAndAllowWhileIdle` قرابة 00:05 بالتوقيت المحلي (بدون إذن `SCHEDULE_EXACT_ALARM`). إذا كان العدّ مفعّلًا والجهاز مستثنى والخدمة ميتة، يبدأ الخدمة ليحصل اليوم الجديد على خط أساس حقيقي. وإن لم يعمل المنبّه تبقى القاعدة القديمة: **إسقاط** الفارق الذي لا يمكن نسبته لليومين، فلا تُخترع خطوات.

**ما لم يتغيّر:** حساب الخطوات، وأولوية المصادر، وشرط عدم وجود إذن الإنترنت، والحقيقة الثابتة أن الخطوات التي تُؤخذ وسبِيل ميت لا يمكن استرجاعها.

## 2) اختبار الصفر — افعله أولًا (5 دقائق)

1. اضغط ملف APK وثبّته **كتحديث** (لا تُزل نسخة 1.3.0). يحتفظ التطبيق بقاعدة البيانات والإعدادات واستثناء البطارية.
2. افتح سبِيل مرة وتأكد أن العدّ مفعّل.
3. **الإعدادات → التطبيقات → سبِيل → إيقاف قسري (Force stop)** ثم تأكيد.
4. اترك الهاتف **دقيقتين على الأقل**.
5. افتح سبِيل ثم اذهب مباشرة إلى **التشخيص** (البطاقة الحمراء في الرئيسية → شارك التقرير، أو الإعدادات → التشخيص). **لا تضغط "أعد تشغيل العدّ" قبل ذلك.**
6. انزل للأسفل → **شارك التقرير الكامل** → اقرأ المعاينة → **شارك التقرير** → أرسل ملف ZIP للمطوّر (واتساب / تلغرام / بريد).
7. سجّل: وقت الإيقاف القسري بالضبط، وطراز الهاتف، وإصدار أندرويد.

**علامات النجاح في `report.txt`:**

- `[PROCESS]`: **معرّف عملية جديد** ووقت بدء جديد بعد الفجوة.
- `[PROCESS END]`: سجل خروج وقته يطابق الإيقاف القسري. السَّبب المتوقّع `USER_REQUESTED` مع ذكر `force stop` في الوصف على أندرويد 12+، أو `USER_STOPPED` على الإصدارات الأقدم (وقد تستخدم الشركة المصنّعة `REASON_*` آخر). على أندرويد 10 وأقدم لا يُحفظ سجل — وهذا متوقّع.
- `[HEARTBEAT]`: آخر نبضة **قبل** الإيقاف (أو "لا يوجد" إذا ماتت الخدمة قبل أول نبضة).
- `[START]`: طلب بدء الخدمة عند إعادة فتح التطبيق.
- `[COUNTING]`: لم يزد مجموع اليوم — لم تُخترع خطوات عن فترة الموت.

## 3) الاستخدام العادي بعد اختبار الصفر (48–72 ساعة)

استخدم الهاتف طبيعيًا. كلما صفر العدّاد أو تجمّد:

1. افتح سبِيل → التشخيص → **شارك التقرير الكامل**.
2. أرسل ZIP مع وقت ملاحظة المشكلة (تاريخ + ساعة)، وهل أعدت تشغيل الهاتف.

## 4) الخصوصية

**يتضمّن التقرير:** الطراز والشركة، إصدارَي أندرويد والتطبيق، مجموع خطوات اليوم والمجموع المحفوظ، أسماء الحالات، أوقات الأحداث، ونبضات القلب والكتابة والعامل والمنبّه، وحالة الطاقة (نسبة البطارية، الشحن، الشاشة، وضع Doze، توفير الطاقة، تقييد الخلفية)، والأذونات، وأسباب الخروج، ونوع الانهيار وأطر المكدّس (بدون الرسالة)، وسجل الأحداث.
**لا يتضمّن:** الموقع، جهات الاتصال، محتوى الإشعارات، قائمة التطبيقات، معرّفات الحسابات، القيم الخام لكل حدث حسّاس (فقط "وصلت عيّنة")، أو نصوصًا حرّة.
**لا يُشارك إلا بضغطتك**؛ التطبيق **بلا إذن إنترنت**، ولا يُرفع شيء تلقائيًا، ويمكن مسح السجل من شاشة التشخيص.

## 5ب) ما تحقّق حتى الآن (محاكي، 2026-11-26)

نُفِّذ تسلسل اختبار الصفر نفسه على **محاكي أندرويد 16** بنسخة التشخيص (تحديث فوق تثبيت قائم فحُفظت البيانات والإعدادات):

- إيقاف قسري الساعة `18:03:39`، وإعادة فتح الساعة `18:06:06` — نافذة موت **173 ثانية** بلا خطوات.
- `report.txt` أظهر في `[PROCESS]` العملية الجديدة `26992` ووقت البدء، وفي `[PROCESS END]` السَّبب `USER_REQUESTED(10)` مع `desc=[FORCE STOP] …`، وبقي `[COUNTING]` صفرًا (لا خطوات مخترعة).
- ظهرت معاينة `report.txt` الحقيقية، وفتح زر **شارك التقرير** منتقي المشاركة بعبارة `Sharing 1 file` واسم ملف ZIP — إذن FileProvider يعمل.

وقد كشف هذا التشغيل خطأين أُصلحا قبل بناء هذا الملف: التشريح كان يفضّل اللقطة المحفوظة فيُخفي كل إيقاف قسري لاحق (صار يقرأ `ApplicationExitInfo` دائمًا)، والتقرير كان يعرض **مدة تشغيل الجهاز** كعمر للعملية. **اختبار الصفر على الهاتف الفعلي ما زال مطلوبًا**؛ المحاكي يثبت المسجّل وخط التقرير، لا سلوك الشركة المصنّعة.

## 5) حدود معروفة

1. الخطوات أثناء موت سبِيل لا تُسترجع؛ هذا البناء يرصد ويوثّق ولا يخترع.
2. `ApplicationExitInfo` متاح من أندرويد 11؛ على 8–10 يعتمد التقرير على حقائق سبِيل المحفوظة (الفجوة + آخر توقف نظيف).
3. "التنظيف العميق" من الشركات قد يمنع منبّه 00:05 والعامل أيضًا. عندها لا يعمل أي أداة داخل التطبيق، والخطوة التالية `adb bugreport` من حاسوب.
4. هذا بناء تشخيصي وليس النسخة العامة.
5. إذا بقيت التقارير غير حاسمة بعد ~72 ساعة، نتوقف عن بناء الأدوات ونأخذ `bugreport` واحدًا بتواقيت محدّدة.