# Sabeel 1.3.0 — release notes (user-facing, EN + AR)

**Status:** DRAFT for Lead review, 2026-11-22. This file has two jobs: (a) the text users see in the Play listing ("What's new") and in-app; (b) the honest limitations the owner must be ready to repeat. **Do not publish until** the v1.3 build is green (task-20), the session flow is exercised on the release build, and the wording's feature list matches what actually ships. **Arabic native review is a launch precondition**; the Arabic below is a faithful draft, not reviewed copy.
**Short Play "What's new" strings** (`docs/release/PLAY-1.3.md` §10.5–10.6) are repeated at the end for copy-paste. Character counts: EN 401/500, AR 375/500.

---

## What's new in 1.3.0 — English (user-facing)

**You are in control of counting.**
Sabeel now has one clear on/off switch for step counting. Tap the ring on Home, the status pill underneath it, or the switch in Settings — they are the same switch. Sabeel remembers your choice after a restart, so if you turned counting off it stays off.

**A restart is no longer a guess.**
After your phone restarts, Sabeel tries to resume counting if you left it on. If Android or your phone maker blocks it, Sabeel says so instead of pretending everything is fine: Diagnostics now records each boot attempt (time, permissions, sensor, result, whether counting was alive after the check) so you — or the person helping you — can see what happened. If counting could not resume, you may get one quiet notification to reopen the app.

**Custom sessions: walk or run, your way.**
Start a session from Home. Choose walk or run, an optional goal (time, steps, distance, or a saved place), and whether to record a route. Pause and resume without the app quietly counting the time or steps you skipped. Finish and save; your session appears in History. Session steps are shown separately, but they are part of the same day total — Sabeel does not count them twice.

**Small things that help.**
- Saved setups for your usual walk/run (presets).
- Saved places for a place goal — stored on this phone only; there is no address search and no map download.
- A goal-reached notification you can tap to open the app.
- A "Share diagnostics" button that produces a text report with **no location or saved-place data**.
- If your version shows session export: save a session as CSV or GPX through the system file picker.

**Still true, and still the point.**
No account. No ads. No analytics. No INTERNET permission — the app cannot send your data anywhere. All health and activity data stays in the app's private storage; Android cloud backup is off. Optional Health Connect is read-only (Sabeel never writes to it) and can be revoked at any time. Delete-all in Settings removes everything.

## Honest limits — English

Please read these; they are real, not fine print.

- **Sabeel cannot restart itself after a force-stop.** If Android or your phone maker force-stops the app, counting stops until you reopen Sabeel or restart the phone. This is an Android rule; no app can bypass it.
- **The restart attempt is not a guarantee.** On newer Android versions, starting the step-counting service from the background at boot can be blocked. Sabeel records the attempt and tells you when it was blocked; it does not claim counting "never stops".
- **Aggressive battery managers (MIUI and others) can stop counting.** Add Sabeel to Autostart and set battery saver to "No restrictions" (the Redmi checklist explains this). Even then, a maker's update can change the rules.
- **Location is foreground-only.** A route is recorded only while the session screen is open and only if you allow location. There is no background location; distance can pause while you are away from the screen even though steps and time continue.
- **Session steps are a slice of the day total.** Pausing a session freezes its own time and steps; it does not stop the day counter. Resuming does not back-fill the paused period into the session.
- **Places are local and simple.** You save a name and coordinates on the phone; Sabeel does not search addresses, reverse-geocode, download maps or share places.
- **One session at a time.**
- **Estimates are estimates.** Step counts, distance, active minutes and calories are approximations from phone sensors (and optionally Health Connect), not medical measurements. Sabeel is not a medical device and does not diagnose, treat, cure or prevent anything.
- **Export files leave the app's protection.** A CSV/GPX you export can be read by other apps and people you share it with; the developer never receives it. Delete those files yourself if you want them gone.
- **Arabic translations are included but not yet reviewed by a native speaker.** Report any awkward or unclear Arabic wording; a native review is still required before the Arabic listing goes live.
- **No Play approval is implied.** This release note is a product description, not a promise of store availability.

---

## ما الجديد في 1.3.0 — العربية

**التحكّم في العدّ أصبح بيدك.**
أصبح لسبِيل مفتاح واضح لتشغيل عدّ الخطوات وإيقافه. اضغط الحلقة في الرئيسية، أو شريط الحالة تحتها، أو المفتاح في الإعدادات — إنه المفتاح نفسه. يتذكّر سبِيل اختيارك بعد إعادة التشغيل، فإذا أوقفت العدّ يبقى متوقفًا.

**إعادة التشغيل لم تعد مجهولة.**
بعد إعادة تشغيل هاتفك يحاول سبِيل استئناف العدّ إذا تركته مشغّلًا. وإذا أوقفه أندرويد أو صانع الهاتف يقول لك ذلك بدل أن يتظاهر أن كل شيء بخير: تسجّل شاشة التشخيص كل محاولة تشغيل (الوقت، الأذونات، المستشعر، النتيجة، وهل كان العدّ حيًّا بعد الفحص) لترى أنت — أو من يساعدك — ما حدث. وإذا تعذّر استئناف العدّ فقد يصلك إشعار هادئ واحد لإعادة فتح التطبيق.

**جلسات مخصّصة: مشي أو جري، كما تريد.**
ابدأ جلسة من الرئيسية. اختر مشيًا أو جريًا، وهدفًا اختياريًا (وقت، خطوات، مسافة، أو مكان محفوظ)، وهل تسجّل المسار. أوقف مؤقتًا واستأنف دون أن يعدّ التطبيق بهدوء الوقت أو الخطوات التي تخطّيتها. أنهِ الجلسة واحفظها؛ وتظهر في السجل. تُعرض خطوات الجلسة على حدة، لكنها جزء من إجمالي اليوم نفسه — لا يعدّها سبِيل مرتين.

**أشياء صغيرة تساعد.**
- إعدادات محفوظة لمشي أو جري المعتاد.
- أماكن محفوظة لهدف الوصول إلى مكان — تُحفظ على هذا الهاتف فقط؛ لا بحث عن العناوين ولا تنزيل خرائط.
- إشعار بلوغ الهدف اليومي يمكنك الضغط عليه لفتح التطبيق.
- زر «شارك التشخيص» يُنتج تقريرًا نصيًا **بلا بيانات موقع أو أماكن محفوظة**.
- إذا كانت نسختك تعرض تصدير الجلسات: احفظ جلسة كملف CSV أو GPX عبر منتقي الملفات في النظام.

**ما زال صحيحًا، وما زال هو الهدف.**
لا حساب. لا إعلانات. لا تحليلات. لا إذن إنترنت — لا يستطيع التطبيق إرسال بياناتك إلى أي مكان. تبقى كل بيانات الصحة والنشاط في التخزين الخاص بالتطبيق؛ والنسخ الاحتياطي السحابي في أندرويد معطّل. تكامل Health Connect اختياري وقراءة فقط (لا يكتب سبِيل فيه أبدًا) ويمكن إلغاؤه في أي وقت. و«حذف جميع البيانات» في الإعدادات يحذف كل شيء.

## حدود صريحة — العربية

يرجى قراءة هذه الحدود؛ فهي حقيقية وليست شروطًا مخفية.

- **لا يستطيع سبِيل إعادة تشغيل نفسه بعد الإيقاف القسري.** إذا أوقف أندرويد أو صانع الهاتف التطبيق قسريًا يتوقف العدّ حتى تفتح سبِيل أو تعيد تشغيل الهاتف. هذه قاعدة من أندرويد ولا يستطيع أي تطبيق تجاوزها.
- **محاولة الاستئناف ليست ضمانًا.** في إصدارات أندرويد الأحدث قد يُمنع بدء خدمة عدّ الخطوات من الخلفية عند التشغيل. يسجّل سبِيل المحاولة ويخبرك عندما يُمنع، ولا يدّعي أن العدّ «لا يتوقف أبدًا».
- **قد توقف مديرو البطارية المتشدّدون (MIUI وغيره) العدّ.** أضف سبِيل إلى التشغيل التلقائي واضبط موفّر البطارية على «بلا قيود» (توضّح قائمة Redmi ذلك). حتى مع ذلك قد تغيّر تحديثات الصانع القواعد.
- **الموقع في المقدمة فقط.** يُسجَّل المسار فقط ما دامت شاشة الجلسة مفتوحة وفقط إذا سمحت بالموقع. لا يوجد تسجيل موقع في الخلفية؛ وقد تتوقف المسافة أثناء غيابك عن الشاشة بينما تستمر الخطوات والوقت.
- **خطوات الجلسة شريحة من إجمالي اليوم.** الإيقاف المؤقت يجمّد وقت الجلسة وخطواتها؛ ولا يوقف عدّاد اليوم. والاستئناف لا يضيف فترة الإيقاف إلى الجلسة بأثر رجعي.
- **الأماكن محلية وبسيطة.** تحفظ اسمًا وإحداثيات على الهاتف؛ لا يبحث سبِيل عن العناوين ولا يحوّلها ولا ينزّل خرائط ولا يشارك الأماكن.
- **جلسة واحدة في كل وقت.**
- **التقديرات تقديرات.** عدد الخطوات والمسافة والدقائق النشطة والسعرات تقريبات من مستشعرات الهاتف (واختياريًا من Health Connect)، وليست قياسات طبية. سبِيل ليس جهازًا طبيًا ولا يشخّص أو يعالج أو يمنع أي حالة.
- **الملفات المصدَّرة تخرج من حماية التطبيق.** يمكن لتطبيقات أخرى ولمن تشارك معهم قراءة CSV/GPX المصدَّر؛ ولا يستلم المطوّر نسخة منه. احذف تلك الملفات بنفسك إذا أردت إزالتها.
- **الترجمات العربية موجودة لكن لم يراجعها متحدث أصلي بعد.** أبلغ عن أي صياغة عربية غير واضحة؛ ولا تزال المراجعة العربية شرطًا قبل نشر صفحة المتجر العربية.
- **لا يعني هذا الإصدار أي موافقة من Google Play.** هذه ملاحظات إصدار عن المنتج، وليست وعدًا بتوفّر التطبيق في المتجر.

---

## Copy-paste: Play "What's new"

**EN (401/500):**
```
Counting control: turn step counting off or on from the ring or Settings, and Sabeel remembers it across restarts. New custom sessions: walk or run with an optional route, time/steps/distance/place goals, pause and resume, and saved setups. Diagnostics now show what happened at the last boot, so a phone that stops counting in the background is easier to fix. Still offline, ad-free and account-free.
```

**AR (375/500, native review pending):**
```
التحكم في العدّ: أوقف عدّ الخطوات أو شغّله من الحلقة أو من الإعدادات، ويتذكّره سبِيل بعد إعادة التشغيل. جلسات مخصّصة جديدة: مشي أو جري مع مسار اختياري، وأهداف بالوقت أو الخطوات أو المسافة أو المكان، وإيقاف مؤقت واستئناف، وإعدادات محفوظة. تعرض شاشة التشخيص الآن ما حدث عند آخر تشغيل، ليسهل إصلاح هاتف يتوقف عن العدّ في الخلفية. ما زال يعمل دون إنترنت، وبلا إعلانات، وبلا حساب.
```

---

## Internal notes (remove before publishing)

- [x] Session flow (start/pause/resume/finish/discard), CSV all-sessions export and per-session GPX export were exercised on the debug v1.3 build (emulator-5564, 2026-11-22); the export bullets stay. Repeat the smoke test on the release-signed build before publishing.
- [x] Places can be added/list/delete in the build (name, latitude, longitude, radius 25–1000 m, external `geo:` Maps link); a PLACE goal with auto-stop saved a session and posted the goal notification on the emulator. The "saved places" bullet stays; no geocoding/network is claimed.
- [ ] Re-check every number/wording against the actual release artifact: no version, size, test count or hash in these notes.
- [ ] Native Arabic review must sign off both the notes and the session strings before the Arabic listing goes live.
- [ ] Keep the same "What's new" text in Play Console and in-app (if the app shows release notes); do not let them drift.
