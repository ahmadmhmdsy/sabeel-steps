# Sabeel Privacy Policy — v1.3 source text (ready to paste)

**Status:** DRAFT v1.3 for Lead/owner review, updated 2026-11-22 for custom sessions, saved places/presets, session export and the v1.3 counting behaviour. It supersedes the v1.2 source at `docs/legal/privacy-policy.md` once the Lead accepts it. **Remaining OWNER actions:** fill the placeholders in [square brackets], decide the hosted URL, host the page, and commission the Arabic native review (launch precondition). Before publishing, run the internal checklist at the end and complete every item.
**Last updated:** 2026-11-22
**Effective date:** [DATE OF FIRST PUBLICATION]
**Hosted page:** `docs/privacy-site/index.html` (self-contained HTML, English then Arabic with `dir="rtl"`; zero external scripts/fonts/trackers). Regenerate it from this text; keep the two in sync.
**Consistency:** the same facts feed `docs/release/PLAY-1.3.md` (Data safety, health declaration) — if one changes, change both.

> **Internal note — remove before hosting.** §6b (session CSV/GPX export) is written for the build in which the session-export entry point actually exists. At the time of this draft the export helpers are in the code but the UI is **not verified**. If the release build does not offer session export, delete §6b and the GPX sentence in §6a before publishing; never describe a feature the build does not expose.

---

## English

Sabeel is an offline step counter and walking tracker for Android, published by [DEVELOPER LEGAL NAME] (the developer named on the Google Play listing). This policy explains what happens to your information when you use the app. It is short because the app keeps everything on your device and has no server.

### 1. What we collect

**We do not collect personal information.**

- There is no account, no sign-up, no email address, no phone number, no name and no advertising ID.
- We do not use advertising, analytics, crash-reporting, attribution or social-login services.
- The app does not request the Android Internet permission, so the app itself cannot send information over the internet.
- We do not sell, rent or share personal information, because we do not have any.

### 2. What stays on your device

The app stores the information you create or that your phone measures, only in the app's private storage on your device:

- step counts and activity history;
- custom walk/run **sessions**: activity type, start/end time, elapsed time while paused excluded, session steps, distance, goal and whether it was met, and the parts of a session that cross midnight;
- **routes** for sessions, only if you allowed location while that session screen was open;
- **saved places** you create for a place goal: a name you type, the latitude/longitude, and a radius. This is local data; the app does not search for addresses, does not reverse-geocode them and does not download maps;
- **saved session setups** (presets): the name and choices you saved;
- if you opt in: today's step total read from Health Connect (a watch or another app). This is read-only: the app never writes anything back to Health Connect. If you grant the optional background-read permission, the counting service may read that total while the app is not open so a watch-synced day is not missed; without that grant the app still counts from the phone's own motion and simply does not read Health Connect in the background;
- distance, active minutes and calorie estimates shown for your days and sessions;
- goals, streaks, achievements and reminder settings;
- your app preferences, including whether step counting is switched on or off.

This information is used only on your device to show your activity and to run the features you asked for. It is not uploaded anywhere by us. Session steps are a slice of the same device day total, not a separate account or a second counter sent anywhere.

### 3. Permissions and why we ask for them

| Permission | When it is used | Why | If you say no |
|---|---|---|---|
| Physical activity / activity recognition | When you use step counting | To read the phone's step sensor and count your steps (Android 10 and later asks at runtime) | Step counting will not work; other features that do not need it still work |
| Health Connect steps (read-only) | Only if you tap "Use synced steps" in Settings; the optional background-read grant is requested through the same Health Connect permission screen | To read today's step total that another app or watch already saved to Health Connect, including while the counting service runs with the app closed. The app never writes to Health Connect | The app still counts from the phone's sensors (or its labelled motion estimate); nothing else changes |
| Notifications | When you turn on reminders, reach your daily goal, or start tracking a session | To show the reminders you set, the goal notification, the recovery notice if counting could not resume, and the ongoing notification Android requires while counting or a session runs | You may not see reminders or notifications; counting and sessions still work, but Android may show them only in the system's active-apps area |
| Location (precise or approximate, depending on what you allow) | Only after you start a session and allow it; used while the session screen is open | To draw your route and improve distance for that session. The app does not request background location and does not record location when the session screen is not active | The session is still tracked by steps and time; no route or location-based distance is recorded |
| Start at boot | After your phone restarts | To try to resume step counting if you left it on, and to reschedule the reminders you already set | Counting resumes when you next open the app; reminders may not be restored until then |

While step counting or a session runs, Android requires a foreground service with a visible notification so that it is not interrupted. That notification is about your activity; it is not advertising.

We do not ask for the camera, microphone, contacts, SMS, call log, photos, or general file access, and we do not ask for background location.

### 4. No third parties and no ads

There are no third-party advertising, analytics, crash-reporting, tracking or social SDKs in Sabeel. The app has no server and no cloud account. We use standard Android and AndroidX libraries that run on your device and do not send your data to us.

Google Play and the Android operating system may collect their own information about app installs and device use under Google's own privacy policy. That collection happens at the platform level, not through Sabeel, and we do not receive it.

### 5. Backup and storage

Sabeel turns off the Android backup feature for its app data (the manifest sets android:allowBackup="false"). This means app data is not included in Android cloud backups.

Your data stays in the app's private storage until you delete it or uninstall the app. Android protects app data with the operating system's app sandbox; if your device has encryption enabled, the operating system encrypts the storage. We cannot access your device's storage.

### 6a. Exporting your daily totals

You can export your daily step totals as a CSV file. The app uses Android's system file picker, and you choose where the file goes (for example Downloads or another folder). We do not receive a copy and we are not told where you saved it. If you later share that file with someone, their handling of it is governed by their own privacy practices.

### 6b. Exporting a session (only if your version offers it)

If your version offers it, you can export a saved session as a CSV or GPX file using the same system file picker. A session export can include the route coordinates recorded during that session, which are more sensitive than a daily total: treat the exported file as personal data, choose a destination you trust, and delete it when you no longer need it. We do not receive a copy.

### 7. Deleting your data

- Use "Delete all data" in the app to erase your step history, sessions and their routes, saved places, saved setups, goals, reminders and preferences.
- Uninstalling Sabeel removes the app and its data from your device.
- Exported files are outside the app's storage: deleting app data does not delete a CSV, GPX or other file you saved elsewhere. Delete those files yourself if you want them gone.
- There is no account to delete, so there is nothing to request from us.

Because we do not collect data, we cannot access, correct or delete your data on our side; it is under your control on your device.

### 8. Children

Sabeel is not directed to children under 13. We do not knowingly collect personal information from children, and in fact the app does not collect personal information from anyone. If a child uses the app on a family device, the data stays on that device and can be deleted by the device's owner.

### 9. Health note

Sabeel is a fitness and activity tracker, not a medical device. Step counts, distance, active minutes, session goals and calorie estimates are approximations calculated on your device. They are not medical advice, diagnosis or treatment. Talk to a qualified health professional before making health decisions.

### 10. Changes to this policy

If the app changes in a way that affects this policy, we will update this page and its "Last updated" date. If the change is material, we will also note it in the app or in the Google Play listing before the change takes effect.

### 11. Contact

Questions about this policy or the app's privacy:

- Developer: [DEVELOPER LEGAL NAME]
- Email: [CONTACT EMAIL]
- Postal address (optional): [POSTAL ADDRESS]

---

## العربية

تطبيق «سبيل» هو تطبيق عدّاد خطوات وتتبّع للمشي يعمل دون اتصال بالإنترنت على نظام أندرويد، وينشره [اسم المطوّر القانوني] (المطوّر المذكور في صفحة التطبيق على Google Play). توضّح هذه السياسة ما يحدث لمعلوماتك عند استخدام التطبيق. وهي قصيرة لأن التطبيق يحفظ كل شيء على جهازك ولا يملك خادمًا.

### ١. ما الذي نجمعه

**لا نجمع أي بيانات شخصية.**

- لا يوجد حساب ولا تسجيل ولا بريد إلكتروني ولا رقم هاتف ولا اسم ولا معرّف إعلاني.
- لا نستخدم خدمات إعلانات أو تحليلات أو تقارير أعطال أو تتبّع أو تسجيل دخول اجتماعي.
- لا يطلب التطبيق إذن الإنترنت في أندرويد، لذلك لا يستطيع التطبيق نفسه إرسال المعلومات عبر الإنترنت.
- لا نبيع البيانات الشخصية ولا نؤجّرها ولا نشاركها، لأننا لا نملك أيًّا منها.

### ٢. ما الذي يبقى على جهازك

يحفظ التطبيق المعلومات التي تنشئها أو التي يقيسها هاتفك، وذلك فقط في مساحة التخزين الخاصة بالتطبيق على جهازك:

- عدد الخطوات وسجل النشاط؛
- **الجلسات** المخصّصة للمشي أو الجري: نوع النشاط، وقت البدء والانتهاء، الوقت المنقضي دون فترات الإيقاف، خطوات الجلسة، المسافة، الهدف وهل تحقق، وأجزاء الجلسة التي تعبر منتصف الليل؛
- **المسارات** للجلسات، فقط إذا سمحت بالوصول إلى الموقع أثناء فتح شاشة تلك الجلسة؛
- **الأماكن المحفوظة** التي تنشئها لهدف الوصول إلى مكان: اسم تكتبه، وخط العرض والطول، ونطاق الوصول. هذه بيانات محلية؛ لا يبحث التطبيق عن العناوين ولا يحوّل الإحداثيات إلى عناوين ولا ينزّل خرائط؛
- **الإعدادات المحفوظة** للجلسات: الاسم والخيارات التي حفظتها؛
- إذا اخترت ذلك: مجموع خطوات اليوم المقروء من Health Connect (ساعة أو تطبيق آخر). القراءة فقط: لا يكتب التطبيق شيئًا في Health Connect. وإذا منحت إذن القراءة في الخلفية الاختياري، فقد تقرأ خدمة العدّ هذا المجموع والتطبيق غير مفتوح حتى لا يضيع يوم مزامَن من الساعة؛ وبدون هذا الإذن يظل التطبيق يعدّ من حركة الهاتف ولا يقرأ Health Connect في الخلفية؛
- المسافة والدقائق النشطة وتقديرات السعرات الحرارية المعروضة لأيامك وجلساتك؛
- الأهداف والسلاسل المتتالية والإنجازات وإعدادات التذكير؛
- تفضيلات التطبيق، بما فيها هل عدّ الخطوات مشغّل أم متوقف.

تُستخدم هذه المعلومات على جهازك فقط لعرض نشاطك وتشغيل الميزات التي طلبتها، ولا نرفعها إلى أي مكان. وخطوات الجلسة شريحة من إجمالي اليوم نفسه على الجهاز، وليست حسابًا آخر ولا عدّادًا ثانيًا يُرسَل إلى أي مكان.

### ٣. الأذونات وأسباب طلبها

| الإذن | وقت الاستخدام | السبب | إذا رفضت |
|---|---|---|---|
| النشاط البدني / التعرّف على النشاط | عند استخدام عدّاد الخطوات | لقراءة مستشعر الخطوات في الهاتف وعدّ خطواتك (يُطلب وقت التشغيل في أندرويد 10 وما بعده) | لن يعمل عدّاد الخطوات، وتبقى الميزات الأخرى التي لا تحتاجه تعمل |
| خطوات Health Connect (قراءة فقط) | فقط إذا ضغطت «استخدام الخطوات المزامنة» في الإعدادات؛ ويُطلب إذن القراءة في الخلفية الاختياري من شاشة أذونات Health Connect نفسها | لقراءة مجموع خطوات اليوم الذي حفظته ساعة أو تطبيق آخر في Health Connect، بما في ذلك أثناء عمل خدمة العدّ والتطبيق مغلق. لا يكتب التطبيق في Health Connect أبدًا | يظل التطبيق يعدّ من مستشعرات الهاتف (أو التقدير الحركي المعلَّم)، ولا يتغير شيء آخر |
| الإشعارات | عند تفعيل التذكيرات، أو بلوغ الهدف اليومي، أو بدء تتبّع جلسة | لعرض التذكيرات التي ضبطتها وإشعار الهدف وإشعار الاستعادة إذا تعذّر استئناف العدّ، والإشعار المستمر الذي يتطلبه أندرويد أثناء العدّ أو الجلسة | قد لا ترى التذكيرات أو الإشعارات؛ ويستمر العدّ والجلسات، لكن أندرويد قد يعرضها فقط في منطقة التطبيقات النشطة |
| الموقع (الدقيق أو التقريبي، حسب ما تسمح به) | فقط بعد أن تبدأ جلسة وتسمح بالوصول إلى الموقع؛ ويُستخدم ما دامت شاشة الجلسة مفتوحة | لرسم مسارك وتحسين المسافة لتلك الجلسة. لا يطلب التطبيق الوصول إلى الموقع في الخلفية ولا يسجّل الموقع عندما لا تكون شاشة الجلسة نشطة | تظل الجلسة محسوبة بالخطوات والوقت، ولا يُسجَّل أي مسار أو مسافة قائمة على الموقع |
| بدء التشغيل | بعد إعادة تشغيل هاتفك | لمحاولة استئناف عدّ الخطوات إذا تركته مشغّلًا، ولإعادة جدولة التذكيرات التي ضبطتها سابقًا | يستأنف العدّ عند فتح التطبيق في المرة التالية؛ وقد لا تُستعاد التذكيرات حتى ذلك الحين |

أثناء عمل عدّ الخطوات أو الجلسة، يتطلب أندرويد خدمة تعمل في الواجهة مع إشعار ظاهر حتى لا يتوقف النشاط. هذا الإشعار يتعلق بنشاطك وليس إعلانًا.

لا نطلب إذن الكاميرا أو الميكروفون أو جهات الاتصال أو الرسائل النصية أو سجل المكالمات أو الصور أو الوصول العام إلى الملفات، ولا نطلب تسجيل الموقع في الخلفية.

### ٤. لا أطراف ثالثة ولا إعلانات

لا توجد في «سبيل» أي أدوات إعلانات أو تحليلات أو تقارير أعطال أو تتبّع أو تسجيل دخول اجتماعي تابعة لأطراف ثالثة. لا يملك التطبيق خادمًا ولا حسابًا سحابيًا. نستخدم مكتبات أندرويد وAndroidX القياسية التي تعمل على جهازك ولا ترسل بياناتك إلينا.

قد تجمع Google Play ونظام أندرويد معلوماتهما الخاصة عن عمليات التثبيت واستخدام الجهاز وفق سياسة خصوصية Google. يحدث ذلك على مستوى المنصة، وليس من خلال «سبيل»، ولا نستلمه.

### ٥. النسخ الاحتياطي والتخزين

يعطّل «سبيل» ميزة النسخ الاحتياطي في أندرويد لبيانات التطبيق (يضبط ملف بيان التطبيق السمة android:allowBackup="false"). وهذا يعني أن بيانات التطبيق لا تُدرج في النسخ الاحتياطية السحابية لأندرويد.

تبقى بياناتك في مساحة التخزين الخاصة بالتطبيق حتى تحذفها أو تُزيل التطبيق. يحمي أندرويد بيانات التطبيقات عبر العزل الأمني للتطبيقات في نظام التشغيل، وإذا كان التشفير مفعّلًا على جهازك فسيشفّر نظام التشغيل مساحة التخزين. ولا يمكننا الوصول إلى مساحة تخزين جهازك.

### ٦أ. تصدير إجمالياتك اليومية

يمكنك تصدير مجاميع خطواتك اليومية في ملف CSV. يستخدم التطبيق منتقي الملفات في أندرويد، وأنت تختار مكان حفظ الملف (مثل مجلد التنزيلات أو أي مجلد آخر). لا نستلم نسخة منه ولا يُبلَّغ لنا بمكان حفظه. وإذا شاركت الملف لاحقًا مع شخص آخر، فإن تعامله معه يخضع لممارساته الخاصة في الخصوصية.

### ٦ب. تصدير جلسة (فقط إذا كانت نسختك تعرض ذلك)

إذا كانت نسختك تعرض ذلك، يمكنك تصدير جلسة محفوظة في ملف CSV أو GPX عبر منتقي الملفات نفسه. قد يتضمن تصدير الجلسة إحداثيات المسار المسجّل خلالها، وهي أكثر حساسية من الإجمالي اليومي: عامل الملف المصدَّر كبيانات شخصية، واختر وجهة تثق بها، واحذف الملف عندما لا تحتاج إليه. لا نستلم نسخة منه.

### ٧. حذف بياناتك

- استخدم «حذف جميع البيانات» داخل التطبيق لمحو سجل الخطوات والجلسات ومساراتها والأماكن المحفوظة والإعدادات المحفوظة والأهداف والتذكيرات والتفضيلات.
- تؤدي إزالة تطبيق «سبيل» إلى حذف التطبيق وبياناته من جهازك.
- الملفات المصدَّرة خارج مساحة تخزين التطبيق: حذف بيانات التطبيق لا يحذف ملف CSV أو GPX أو غيره مما حفظته في مكان آخر. احذف تلك الملفات بنفسك إذا أردت إزالتها.
- لا يوجد حساب لحذفه، لذلك لا توجد طلبات يجب تقديمها إلينا.

وبما أننا لا نجمع البيانات، فلا يمكننا الوصول إلى بياناتك أو تصحيحها أو حذفها من جهتنا؛ فهي تحت سيطرتك على جهازك.

### ٨. الأطفال

تطبيق «سبيل» غير موجّه إلى الأطفال دون سن 13 عامًا. ولا نجمع بيانات شخصية من الأطفال عن علم، بل إن التطبيق لا يجمع بيانات شخصية من أي شخص. وإذا استخدم طفل التطبيق على جهاز عائلي، فتبقى البيانات على ذلك الجهاز ويمكن لمالكه حذفها.

### ٩. ملاحظة صحية

«سبيل» تطبيق للياقة والنشاط، وليس جهازًا طبيًا. عدد الخطوات والمسافة والدقائق النشطة وأهداف الجلسات وتقديرات السعرات الحرارية تقريبات تُحسب على جهازك، وليست نصيحة طبية ولا تشخيصًا ولا علاجًا. استشر مختصًا صحيًا مؤهلًا قبل اتخاذ قرارات صحية.

### ١٠. التغييرات على هذه السياسة

إذا تغيّر التطبيق بما يؤثر في هذه السياسة، فسنحدّث هذه الصفحة وتاريخ «آخر تحديث». وإذا كان التغيير جوهريًا، فسنشير إليه أيضًا داخل التطبيق أو في صفحة التطبيق على Google Play قبل أن يصبح ساريًا.

### ١١. التواصل

للأسئلة حول هذه السياسة أو خصوصية التطبيق:

- المطوّر: [اسم المطوّر القانوني]
- البريد الإلكتروني: [البريد الإلكتروني للتواصل]
- العنوان البريدي (اختياري): [العنوان البريدي]

في حال وجود اختلاف في المعنى بين النصين الإنجليزي والعربي، يُعتمد النص الإنجليزي.

---

## Internal notes — remove this section before hosting

**Do not publish this file or the hosted page until every item below is true.**

### Placeholders the owner must fill
- [ ] [DEVELOPER LEGAL NAME] — must match the Google Play developer name exactly (English and Arabic versions).
- [ ] [CONTACT EMAIL] — a monitored mailbox; Play reviewers and users must be able to reach it.
- [ ] [POSTAL ADDRESS] — optional; delete the line if not used.
- [ ] [DATE OF FIRST PUBLICATION] — set the day the page goes live.
- [ ] [HOSTED URL] — not in this file, but linked from Play Console (App content > Privacy policy) and from the app's Privacy screen.

### Hosting rules (Play User Data policy)
- [ ] **Publish only this policy page.** Do not publish the docs/legal/ folder or the repository docs tree: it contains the internal clearance memo, the Play compliance checklist and SEALED target research snapshots (see the memo's artefact table). Use an isolated folder, repo branch or separate public repo for the hosted page.
- [ ] Active, publicly accessible HTTPS URL — no login, no geofencing, no country blocks.
- [ ] Not a PDF and not an editable document; a plain HTML page on GitHub Pages or an owned domain is fine.
- [ ] Page must stay online and unchanged in meaning; keep an archive copy of each published version with its date.
- [ ] The in-app privacy screen must link to the same URL (build with `-Psabeel.privacyPolicyUrl=...`).

### v1.3 truth checks against the release build (must pass before publishing)
- [ ] No INTERNET permission (and no ACCESS_NETWORK_STATE) in the merged manifest/APK; dependency audit shows no advertising/analytics/crash SDKs.
- [ ] `android:allowBackup="false"` in the release merged manifest; no cloud backup.
- [ ] No `ACCESS_BACKGROUND_LOCATION` and no background location code path; location is requested only from the session start flow and the walk/session service is `location|health` with a visible notification.
- [ ] Step counting can be switched off/on from Home, the pill and Settings, and the state is persisted (privacy-relevant only because the notification says counting is on; verify the copy matches).
- [ ] Saved places store only label + latitude/longitude + radius locally; there is **no** geocoding, address search, map download or network call. If a place UI does not exist in the build, do not mention saved places in the policy until it does.
- [ ] Sessions, session routes and the midnight legs are stored locally; session steps are projected from the day total and are not described as a separate counter.
- [ ] CSV export (daily totals) exists and behaves as §6a says. **If session CSV/GPX export is not reachable in the build, delete §6b and the GPX sentence before publishing.**
- [ ] "Delete all data" clears daily steps, sessions + legs, routes, places, presets, preferences and files; verify by testing on the build.
- [ ] The in-app Privacy screen and the About screen do not contradict this policy (e.g. no "we may collect" text).
- [ ] Health Connect is read-only (Steps; no write), opt-in, revocable; the optional background read is named in the Settings disclosure and in §2/§3.
- [ ] Arabic text reviewed by a native Arabic reviewer (**launch precondition** — Lead decision 2026-11-16); the two versions are equivalent, and the "Last updated"/effective dates are the same on the hosted English and Arabic pages.
- [ ] If any sentence would become untrue (ads, accounts, sync, analytics, crash reporting, new permissions, cloud), rewrite this policy and re-run this checklist before shipping that build.

### Consistency with Play Console
- [ ] Data safety: "No data collected / No data shared"; on-device Health Connect read noted (local processing, never transmitted, never written back); user-initiated export (daily totals, and session export if shipped) noted as the exception; security practices = no transmission, app sandbox, backups disabled. Matches `docs/release/PLAY-1.3.md` §9.
- [ ] Health apps declaration: Activity and Fitness; Health Connect declaration lists the Steps data type as **read** only. Matches `docs/release/PLAY-1.3.md` §4 and §6.
- [ ] Store listing health wording matches §9 (estimates, not a medical device, no diagnosis/treatment claims).

### Maintenance
If the policy changes, publish a new version, update both languages and the date, and keep the old version archived. Do not silently edit the live page. Regenerate `docs/privacy-site/index.html` from this text and keep the old published version on file.
