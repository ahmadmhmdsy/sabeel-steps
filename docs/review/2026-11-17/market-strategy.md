# Market & strategy review — Sabeel (سبيل), 2026-11-17

**Reviewer:** market-strategist (outside-in). **Scope:** market viability/GTM only; code correctness is other reviewers' scope. **Verdict:** technically credible, but the consumer bet is weak — publish free as a portfolio/credential asset, don't invest in polish/ASO, and treat B2B as the only lawful revenue path.

## 1. SCOPE & METHOD

Read README.md, brief.md, spec.md (head), competitors.md, qiyas-council.md, name-check.md, play-compliance-checklist.md (grep). Inspected the live Play Store as an Iraqi Arabic user (hl=ar, gl=IQ) and vendor/policy/AdMob pages. No Gradle, no code changes, no installs. Web sources accessed 2026-11-17 unless dated; projections are **est.**

## 2. VERIFIED STRENGTHS

- **Offline claim provable from the phone:** no INTERNET, no ACCESS_NETWORK_STATE, allowBackup=false (README.md:38-45).
- **Genuinely ad-free:** no ad SDK (brief.md:15, README.md:43); every Play leader sampled says "Contains ads".
- **Arabic/EN parity is an acceptance criterion** (brief.md:16,28), and language matters in MENA (62% prefer Arabic apps, go-globe 2026-06-23; 9/10 Saudi users, RedSeer via techscoop 2025-10-22).
- **Meets 2026 Play rules:** targetSdk 36 (brief.md:27; required since 2026-08-31), Health Connect excluded (README.md:33).
- **Unusual portfolio signal:** 41/41 tests, static PASS=33/FAIL=0 (README.md:97-103); clean-room discipline (README.md:3, brief.md:12-15); name screening states its own limits (name-check.md:8,14).

## 3. FINDINGS TABLE & ANALYSIS

### 3.1 Market snapshot (2026)

**Bundling is accelerating.** Google Fit APIs end in 2026 (developer.android.com Fit-migration); Health Connect counts phone steps natively (Android Authority 2025-09-17); Fitbit became **Google Health** on 2026-05-07 with an AI coach and phone step tracking, requiring a Google login (blog.google 2026-05-07; support.google.com/googlehealth/answer/14236404). Google Health: **100M+ installs, 4.0★**; Samsung Health: **1B+, 4.3★**; Transsion **My Health** is "preset in a TRANSSION phone", 100M+ (Play gl=IQ, 2026-11-17).

**Leaders monetise with ads + subscriptions, not the counter:** Leap **100M+, 4.9★ (1.72M), ads+IAP**; StepsApp **10M+, 4.7★ (163K), ads+IAP**; Arabic-named Hitchhike خطواتي **50M+, 4.8★ (843K), ads+IAP**; Pacer 100M+ downloads, ~$3.99/mo (mypacer.com; apppricer 2026-11-08); Sweatcoin 180M+ registered, marketplace+ads+sub (Sub Club 2025-03-05). **Iraq:** 42.1M internet users, 87.3% penetration, Oct 2026 (DataReportal Digital 2027: Iraq, 2026-11-05); Apple 13% of mobile (StatCounter Iraq, Oct 2026). 2025-26 policy raised the bar rather than resetting the category.

### 3.2 Wedge test

**Arabic-first is not whitespace.** In the Arabic Iraqi store the top results already carry Arabic titles/descriptions — Leap ("عداد الخطوات - برنامج المشي"), Hitchhike ("عداد الخطوات - خطواتي") (Play search q=عداد الخطوات, gl=IQ, 2026-11-17). Worse, **Leap's 100M+ Arabic description already claims the exact privacy sentence**: "خاص بنسبة 100% … لا يلزمك تسجيل الدخول. نحن لا نجمع بياناتك الشخصية". Council fact F4 ("No target listing claims Arabic / RTL", council:17) held only for its six-app sample; it is false for the Iraqi store. Only "no INTERNET permission" and honest-estimate framing remain distinctive — and a listing cannot show either.

**"No ads / privacy" is a supporting claim, not an acquisition thesis.** A 2025 survey of 4,000 US/UK users found 3 in 4 now *more* willing to watch ads for free content (Verve 2025); privacy-paradox research keeps finding stated concern does not become behaviour or payment (SSRN 2025-02-13). Every Iraqi already has an ad-free first-party counter (Samsung 1B+, Transsion preset, Google Health). Ads do annoy where the alternative is ad-heavy — Hitchhike's Arabic review 2026-11-03 says ads cause lag — but that app still reached 50M+ installs. **JTBD:** not "count steps" but "make the number mean something" (FitAppsReviewed 2026-10-29); Sabeel's ring/streak/insights are parity, data-ownership/Arabic/honesty weak pull. Likely buyers: users whose built-in app under-counts (Samsung Arabic reviews 2026-10-07/2026-07-03), Arabic readers on weak OEM apps (unverified), privacy maximalists, CSV users — a segment, not a market.

### 3.3 Monetization reality from Iraq

Play's official table marks Iraq: developer ✔, **merchant registration ✘** (support.google.com/googleplay/android-developer/answer/9306917, 2026-11-17) → no paid app, no IAP (confirms council:23). **Ads:** AdMob accepts an Iraqi payments address (AdMob help 13885136; AdSense 2023 thread reports wire to an Iraqi bank) but needs INTERNET + SDK + Data-safety "collects/shared", deleting the only provable claim; 5,000 DAU × 3 banners/day × $0.1–0.3 CPM (MENA, **est.**) = **$45-135/month** before Play's cut. **Donations:** PayPal is not in Iraq (al-arjan.com 2026-10-25), Stripe does not onboard Iraqi businesses (getpaidatlas 2026-11-16), closing Ko-fi/Patreon/BMAC. **Only lawful path: B2B** — white-label walking challenges invoiced to Iraqi NGOs/employers, bank wire (**est.** $500-3,000/deployment, slow procurement).

### 3.4 Distribution/ASO

Store search is ~65% of discovery (AppFollow 2025-11-28; CO Consulting 2026-07-04) and Arabic step keywords are held by 50-100M-install, 4.8-4.9★ apps; a zero-review app will not rank page 1 for "عداد الخطوات". Long-tail ("عداد خطوات بدون انترنت") is plausible, but Hitchhike already markets offline. Closed testing needs 12 opted-in testers for 14 continuous days on new personal accounts (Play help 14151465; testerexchange 2026-11-06). Realistic path: 12-20 recruited testers → production → seed Iraqi walking/Facebook/Telegram groups, a university/NGO challenge, Arabic tech communities (**est.** 1,000 installs in 3-6 months if seeded actively). Paid CPI in MENA is low ($0.05-0.30, est.) but no verified Iraq-based ad-account payment (unverified). App Store needs $99/yr + Mac and paid-app payout from Iraq is unverified; a PWA cannot read the hardware step counter (inference).

### 3.5 Findings table

| ID | Severity | Title | Evidence (file:line / URL) | Why it matters | Recommended fix |
|---|---|---|---|---|---|
| H1 | High (risk) | Arabic-first "whitespace" occupied; council F4 false | council:17 vs Play gl=IQ 2026-11-17: Hitchhike 50M+/4.8★/843K Arabic listing claims offline; Leap 100M+ Arabic description claims "100% private, no login" | Differentiation is parity; "first Arabic offline" is falsifiable | Re-baseline on trust/quality; drop "first/only"; native Arabic review |
| H2 | High (risk) | "No ads/privacy" is a weak acquisition thesis | Verve 2025; SSRN 2025; Samsung 1B+, Transsion preset 100M+ (Play 2026-11-17) | Users install 50-100M+ ad apps and own ad-free counters | Keep as feature; test install intent, not stated preference |
| H3 | High (risk) | Revenue from Iraq ≈ zero; no lawful store path | Play table Iraq merchant ✘ (2026-11-17); PayPal/Stripe absent (2026-10-25/2026-11-16); AdMob Iraq wire needs INTERNET | v1 cannot earn from Play; ads would falsify the only claim | Declare zero-revenue; choose portfolio or B2B; no IAP |
| H4 | High (defect) | Step Zero cannot measure its own KPIs | council:123-137 requires D4 opens; no analytics by design (README.md:43); n=10; 7 days; ±10% vs same sensor; leading (f) | The only gate before spend is unfalsifiable | Debug-only local event log; 20 strangers; invite→install; reboot/rollover tests |
| M1 | Medium | Cold start: search channel is owned | AppFollow 2025-11-28 (65%); Play gl=IQ 2026-11-17; 12/14 gate (help 14151465) | Without a seeded channel the app is invisible | Seed walking groups, university/NGO challenge, tech community |
| M2 | Medium | Platform bundling accelerating | Google Health 2026-05-07; Health Connect steps 2025-09-17; Fit APIs EOL 2026; OEM preinstalls | Free substitute keeps improving | Complement/export/trust layer or B2B |
| M3 | Medium | Health-app review friction | Health declaration mandatory (Play 16679511); FGS health declaration + demo video (Play 13392821; checklist:285,411) | Launch slips or refusal for declaration mismatch | Declaration + video + hosted policy before closed test |
| M4 | Medium | Trademark exposure of "Sabeel" | US Class 9 app filed 2026-09-18 (Justia 50116279, games); UK SABEEL drinks registered 2024-04-26 (UK00004007580); sabeel.qa charity app; name-check:8,14 | Forced rename after publication loses installs; weak distinctiveness | Iraq/WIPO Class 9 clearance or Pacelet before listing |
| M5 | Medium | "Own your data" weaker than it sounds | README.md:33 (no Health Connect); CSV only; no cloud/platform sync | Users expect one health hub; phone loss loses the year | v1.1 Health Connect write (no INTERNET) + export reminders |
| L1 | Low (taste) | Title misses Arabic search | name-check:32-36; Play gl=IQ 2026-11-17 | Wasted keyword surface | Bilingual 30-char title after clearance |
| L2 | Low | Verification gates sideloading; iOS not near-term | developer.android.com/developer-verification (2026-09-30, global 2027); Apple 13% Iraqi mobile (StatCounter Oct 2026) | Fallback channels shrink; iOS costs weeks for little reach | Keep Play primary; defer iOS |

**Type note:** no product-code bug is claimed; rows are market/strategy risks plus one measurement-design defect (H4); L1 is taste/ASO.

### 3.6 High findings in detail

**H1.** F4 (council:17) held only for its six-app sample: in the Iraqi store the leaders' listings are fully Arabic, a 50M+ Arabic-named competitor already markets offline, and Leap's 100M+ listing already markets the exact privacy sentence. The need may be real in *quality* (RTL/date bugs, OEM under-counting), but it is a craft fight against incumbents, not an unclaimed position — no "first/only"; lead with the verifiable permission list and export story.

**H2.** Three in four survey respondents are now more willing to watch ads (Verve 2025), and stated privacy concern rarely becomes payment (SSRN 2025); every Iraqi phone already has an ad-free counter. Ads annoy users of the ad-heavy Arabic leader, but it still reached 50M+. "No ads" is a supporting claim for a minority, not the thesis.

**H3.** Merchant registration ✘ closes paid app/IAP; PayPal/Stripe closure kills donations and global checkout; AdMob is receivable but requires INTERNET, an SDK and a "collects/shared" declaration — deleting the only claim a reviewer or user can verify. At 5k DAU the ad arithmetic is **$45-135/mo (est.)**; B2B invoicing is the only real revenue path, and it is services, not product.

**H4.** Step Zero asks for D4 opens from a product with **no analytics by design** (README.md:43); ten friends measure courtesy; seven days cannot measure D7/D30 (D30 ≥12% is unobservable pre-launch); ±10% vs the built-in counter reads the same sensor; (f) is leading. Real risks — reboot/midnight double counts, OEM battery killers — are untested.

### 3.7 Verdict + three cheapest experiments

**Consumer-growth bet: NO-GO.** The category is a commodity being absorbed by the OS (Google Health/Health Connect, 2026), the Arabic-offline position is occupied, and no lawful store revenue exists from Iraq. **Ship-to-learn/portfolio: conditional GO** — $25, ~40 hours, no paid marketing, no feature work until demand evidence; do not add the widget or Health Connect "for retention" first.

- **E1 — Store-demand audit ($0, 2 days).** Top-10 Iraqi Arabic step apps: installs, rating, ads badge, offline/Arabic/privacy claims, Arabic review complaints. **Kill:** ≥3 already claim offline/privacy at ≥10M installs with ad complaints <20% → uniqueness dead → portfolio only; else continue.
- **E2 — Arabic fake-door/waitlist (≤$30, 5-7 days).** Real screenshots + install/waitlist CTA seeded in 3-5 Iraqi walking/Facebook/Telegram groups (optional $20-30 boost; verify Iraq-based Meta payment first — unverified). **Kill:** <20 sign-ups or <2% click-to-signup → no pull → stop before polish, pivot to B2B; ≥50 sign-ups → closed test.
- **E3 — Stranger pilot, instrumented ($0, 10 days, n≈20).** Via a walking group/university; debug-only local event log + WhatsApp diary; measure invite→install, D4 voluntary opens, reboot/rollover accuracy vs a *reference wearable*, 24h battery. **Kill:** D4 <40% or >2% step error or >3%/24h drain → fix before listing.

**Versus council Step Zero (council:123-137):** direction right, design cannot fail honestly — keep accuracy/battery intent, replace sensor-vs-sensor with reboot/rollover tests, instrument locally, recruit strangers, add the missing acquisition test. Council's D7 ≥25%/D30 ≥12% are 4-8× category medians (D1 20-27%, D7 ~7%, D30 ~3%; digipeak 2026-09-03) with no social graph — post-launch gates only.

### 3.8 Alternative uses, ranked

1. **Portfolio/credential (best EV/hour):** publish free; case-study the no-INTERNET architecture, Arabic RTL and test pipeline — value in Gulf/remote hiring.
2. **B2B white-label walking challenge** for Iraqi NGOs/employers/ministries: only lawful revenue (bank wire), reuses CSV/import; needs sales, not engineering.
3. **Open-source/community:** reputation in Arabic Android circles; donations unreceivable from Iraq, so no cash.
4. **Arabic fitness content/coaching:** different product, minimal reuse.
5. **Paid consumer product:** blocked by merchant registration.

## 4. GAPS

- Zero market evidence: no real user has run it (council:135-136); no interviews, waitlist, reviews or install data.
- No measurement plan: no analytics/crash reporting by design (README.md:43); Step Zero has no instrumentation.
- No store assets: Arabic+EN screenshots, feature graphic, demo video, listing copy, hosted privacy policy still open (checklist:285,411; README launch gates); no ASO/keyword research or distribution owner.
- No trademark clearance (screening only, name-check:8); release builds unsigned (README); no Arabic-native review or device QA (README gate 4); no support/rating-recovery plan.

## 5. UNVERIFIED / OPEN QUESTIONS

- Competitors' in-app Arabic/RTL quality: only listing language verified; if poor, H1 drops to Medium.
- Play search volumes/ranking factors: no keyword tool used; "no page-1 ranking" is inference.
- AdMob payout method for Iraq: Iraq is in the valid-payments-address list, but the method rests on a 2023 community reply — verify in-account.
- Meta ad payment from Iraq; Apple developer/payout from Iraq — not verified.
- Trademark registers (Iraq/WIPO) not queried; Justia/Trademarkia are secondary databases.
- StatCounter share is browser-traffic-based, not device sales; the Appfigures Iraq chart snippet was blocked.
- All revenue/install/CAC figures are arithmetic estimates, not observed. I did not run the app or Gradle.
