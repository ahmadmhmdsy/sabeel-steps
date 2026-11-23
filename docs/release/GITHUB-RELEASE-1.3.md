**Sabeel 1.3.0 — an offline, ad-free step counter and walk/run tracker for Android. No account, no ads, no analytics, and no `INTERNET` permission. English and Arabic.**

Sabeel (سبيل) is an original, open-source (Apache-2.0) step counter for Android 8.0+ that keeps everything on your phone. Version 1.3.0 adds a single on/off switch for counting, boot-attempt evidence in Diagnostics, and custom walk/run sessions with optional goals — while adding **no new permissions, no network, no ads and no new third-party SDKs**.

**بالعربية:** سبيل عدّاد خطوات ومتتبّع مشي وجري يعمل دون إنترنت على أندرويد 8.0 فأحدث، ويحفظ كل شيء على هاتفك. يضيف الإصدار 1.3.0 مفتاح تشغيل/إيقاف واحدًا للعدّ، وأدلة محاولة التشغيل عند الإقلاع في شاشة التشخيص، وجلسات مشي/جري مخصّصة بأهداف اختيارية — دون أي أذونات جديدة، ودون شبكة، ودون إعلانات، ودون حزم خارجية جديدة. *(الترجمة العربية لم تُراجَع بعد من متحدث أصلي.)*

> **Sabeel is not published on Google Play.** This GitHub release is the only distribution channel. There is no Play listing and no Play approval is implied. Download and install the signed APK (details below); updates are manual.

## What's new in 1.3.0

**You are in control of counting.** One clear on/off switch for step counting — tap the ring on Home, the status pill underneath it, or the switch in Settings; they are the same switch. Sabeel remembers your choice after a restart, so if you turned counting off it stays off, and Diagnostics distinguishes "off by user" from "stopped by the system".

**A restart is no longer a guess.** After your phone restarts, Sabeel tries to resume counting if you left it on. If Android or your phone maker blocks it, Sabeel says so instead of pretending everything is fine: Diagnostics records each boot attempt (time, permissions, sensor, result, whether counting was alive after the check) and shows the last ten. If counting could not resume you may get one quiet notification to reopen the app.

**Custom sessions: walk or run, your way.** Start a session from Home; choose walk or run, an optional goal (time, steps, distance, or a saved place), and whether to record a route. Pause and resume without the app quietly counting the time or steps you skipped. Finish and save, or discard; sessions appear in History. Session steps are shown separately but are part of the same day total — Sabeel does not count them twice.

**Small things that help.** Saved setups for your usual walk/run; saved places for a place goal (stored on this phone only — no address search, no map download); a goal-reached notification; a "Share diagnostics" button that produces a text report with **no location or saved-place data**; and session export (all sessions as CSV, a session as GPX) through the system file picker.

**Still true, and still the point.** No account. No ads. No analytics. No `INTERNET` permission — the app cannot send your data anywhere. All health and activity data stays in the app's private storage; Android cloud backup is off. Optional Health Connect is read-only (Sabeel never writes to it) and can be revoked at any time. Delete-all in Settings removes everything.

### ما الجديد في 1.3.0

**التحكّم في العدّ أصبح بيدك.** مفتاح واضح لتشغيل عدّ الخطوات وإيقافه: اضغط الحلقة في الرئيسية، أو شريط الحالة تحتها، أو المفتاح في الإعدادات — إنه المفتاح نفسه. ويتذكّر سبيل اختيارك بعد إعادة التشغيل، وتفرّق شاشة التشخيص بين «موقوف بقرار المستخدم» و«أوقفه النظام».

**إعادة التشغيل لم تعد مجهولة.** بعد إعادة تشغيل الهاتف يحاول سبيل استئناف العدّ إن تركته مشغّلًا، وإذا أوقفه أندرويد أو صانع الهاتف يسجّل المحاولة ويعرضها: الوقت والأذونات والمستشعر والنتيجة وهل كان العدّ حيًّا بعد الفحص، مع عرض آخر عشر محاولات. وإذا تعذّر الاستئناف فقد يصلك إشعار هادئ واحد لإعادة فتح التطبيق.

**جلسات مخصّصة: مشي أو جري كما تريد.** ابدأ جلسة من الرئيسية، واختر مشيًا أو جريًا وهدفًا اختياريًا (وقت أو خطوات أو مسافة أو مكان محفوظ) وهل تسجّل المسار. أوقف مؤقتًا واستأنف دون أن يعدّ التطبيق الوقت أو الخطوات التي تخطّيتها، ثم أنهِ الجلسة واحفظها أو ألغِها؛ وتظهر في السجل. وخطوات الجلسة جزء من إجمالي اليوم نفسه، ولا يعدّها سبيل مرتين.

**أشياء صغيرة تساعد.** إعدادات محفوظة لمشي أو جري المعتاد؛ وأماكن محفوظة محليًا بلا بحث عناوين ولا تنزيل خرائط؛ وإشعار بلوغ الهدف؛ وزر «مشاركة التشخيص» بتقرير نصي **بلا بيانات موقع أو أماكن**؛ وتصدير الجلسات (CSV لكل الجلسات وGPX لجلسة) عبر منتقي الملفات في النظام.

**ما زال صحيحًا، وما زال هو الهدف.** لا حساب، ولا إعلانات، ولا تحليلات، ولا إذن `INTERNET` — لا يستطيع التطبيق إرسال بياناتك إلى أي مكان. تبقى كل بيانات الصحة والنشاط في التخزين الخاص بالتطبيق، والنسخ الاحتياطي السحابي معطّل. وتكامل Health Connect اختياري وللقراءة فقط (لا يكتب فيه سبيل أبدًا) ويمكن إلغاؤه في أي وقت. و«حذف جميع البيانات» يحذف كل شيء.

## Install and verify

1. Download **`Sabeel-1.3.0-release.apk`** (signed release APK) from this release. The **`Sabeel-1.3.0-release.aab`** is the signed App Bundle for bundle/Play tooling — it is *not* installable with `adb install`.
2. Verify the download against `SHA256SUMS.txt` (also tracked at https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/dist/SHA256SUMS.txt):

```bash
sha256sum -c SHA256SUMS.txt
# release APK must match:
# 487ee9c9f7e7d0e1bbe490c4b7692c72cb83c0f8d963cf1f2a4545677a4dfe5d  Sabeel-1.3.0-release.apk
```

3. Install with adb (USB debugging on) — use `adb install`, **not** `adb sideload`:

```bash
adb install -r Sabeel-1.3.0-release.apk
```

Sideloading means no store review, no store-mediated trust signal and no automatic updates: Android may warn about unknown sources, and when a new version ships you install the new APK over the old one (same signing key). **Android 8.0 (API 26) or newer is required.**

### Build facts for this release

| Item | Value |
|---|---|
| Signed release APK | 2,138,010 B — SHA-256 `487ee9c9…7a4dfe5d` |
| Signed release AAB | 4,608,495 B — SHA-256 `ffde823d…a4a9fc4e` |
| Version | versionName 1.3.0 / versionCode 3; compileSdk/targetSdk 36; minSdk 26 |
| JVM tests | 205 tests / 18 suites / 0 failures (2026-11-23) |
| Lint | 0 errors, 80 warnings, 2 hints (exit 0, 2026-11-23) |
| Signing | Upload key, APK signature scheme v2; cert SHA-256 `7b12ad6b…6152d` |

Full measured table and provenance: https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/README.md#verification-status

## Honest limits

Please read these; they are real, not fine print.

- **Not on Google Play, and not device-verified.** v1.3.0 was built, unit-tested and linted on 2026-11-23 and exercised on an **emulator** (API 36 AOSP-ATD image, which has no hardware step sensor). **No physical-device run of v1.3 exists yet.**
- **Sabeel cannot restart itself after a force-stop.** If Android or your phone maker force-stops the app, counting stops until you reopen Sabeel or restart the phone. No app can bypass that Android rule.
- **The restart attempt is not a guarantee.** On newer Android versions, starting the counting service from the background at boot can be blocked. Sabeel records the attempt and tells you when it was blocked; it never claims counting "never stops".
- **Aggressive battery managers (MIUI and others) can stop counting.** Add Sabeel to Autostart and set battery saver to "No restrictions"; even then a maker's update can change the rules.
- **Location is foreground-only.** A route is recorded only while the session screen is open and only if you allow location. There is no background location; distance can pause while you are away from the screen even though steps and time continue.
- **Session steps are a slice of the day total.** Pausing freezes the session's own time and steps; it does not stop the day counter, and resuming does not back-fill the paused period.
- **Places are local and simple.** A saved name and coordinates on the phone — no address search, no reverse geocoding, no map download, no sharing.
- **One session at a time. Estimates are estimates.** Step counts, distance, active minutes and calories are approximations from phone sensors (and optionally Health Connect), not medical measurements. Sabeel is not a medical device.
- **The Health Connect read path is unit-tested but not yet validated against real Health Connect data** on a phone whose watch or app writes steps there.
- **Arabic translations are included but not yet reviewed by a native speaker.** A native review is still required before the Arabic listing or store page goes live.
- **Session export, places and the session flow were exercised on a debug build in an emulator**, not on the release-signed build or a physical phone.
- **Exported files leave the app's protection.** A CSV/GPX you export can be read by other apps and the people you share it with; the developer never receives it.
- **Store assets and Play declarations are not ready/submitted.** No Play approval is implied by this release.

## Links

- README (English): https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/README.md
- README (العربية): https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/README.ar.md
- Changelog: https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/CHANGELOG.md
- User-facing release notes (EN + AR): https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/docs/release/NOTES-1.3.md
- Privacy and security model: https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/README.md#privacy-and-security-model
- Checksums: https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/dist/SHA256SUMS.txt
- Project website: https://ahmadmhmdsy.github.io/sabeel-steps/
- License: Apache-2.0 — https://github.com/ahmadmhmdsy/sabeel-steps/blob/main/LICENSE

Sabeel is an independent, original project built clean-room from public product research. It is not affiliated with, endorsed by, or derived from any other step-counter app.
