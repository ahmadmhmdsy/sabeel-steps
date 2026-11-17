# Competitor research — six public store listings (black-box)

Status: v1 · Research date: 2026-11-16 · Owner: researcher (task-1)
Method: public store-listing pages only (Play web + App Store web + public iTunes lookup/RSS API),
plus web search for ownership/maintenance facts. **No APK was downloaded, nothing decompiled,
no user data scraped, no screenshots reused.** Features below are what the *listing* states —
not what the app does when installed. 'unverified' marks anything not visible on a listing.

**Important:** targets #5 and #6 share the name "Runify" but are **different products by different
publishers** (French race-communication app vs. US gamified running app). See §4.2.

## 1. Fact table

| # | App (store) | Developer | Category | Downloads | Rating (count) | Last updated | Price / IAP | Ads badge |
|---|---|---|---|---|---|---|---|---|
| 1 | [Step Counter - Pedometer](https://play.google.com/store/apps/details?id=pedometer.steptracker.calorieburner.stepcounter) (Play) | Leap Fitness Group | Health & Fitness | 100M+ | 4.8 (1.72M) | Oct 20, 2026 | Free · IAP $1.99–$119.99 per item | Contains ads: **yes** |
| 2 | [StepsApp – Step Counter](https://play.google.com/store/apps/details?id=com.stepsappgmbh.stepsapp) (Play) | StepsApp | Health & Fitness | 10M+ | 4.7 (163K) | Nov 16, 2026 | Free · IAP $1.99–$39.99 per item | Contains ads: **yes** |
| 3 | [Step Tracker - Pedometer](https://play.google.com/store/apps/details?id=steptracker.healthandfitness.walkingtracker.pedometer) (Play) | Leap Fitness Group | Health & Fitness | 50M+ | 4.8 (818K) | Nov 16, 2026 | Free · IAP $1.49–$39.99 per item | Contains ads: **yes** |
| 4 | [Map My Walk: Walking Tracker](https://play.google.com/store/apps/details?id=com.mapmywalk.android2) (Play) | MapMyFitness, Inc. | Health & Fitness | 10M+ | 4.8 (369K) | Nov 7, 2026 | Free · IAP $5.99–$39.99 per item | Contains ads: **yes** |
| 5 | [Runify](https://play.google.com/store/apps/details?id=com.domainedunet.runify) (Play) | Domaine du Net | Sports | 10K+ | no rating shown | Oct 12, 2026 | Free · IAP $9.49–$189.99 per item | no ads label |
| 6 | [Runify: Running Tracker](https://apps.apple.com/us/app/runify-running-tracker/id6746146450) (App Store, US) | OneDegree Labs | Health & Fitness | n/a (App Store shows no installs) | 4.8 (245 ratings) | Nov 10, 2026 (v100.2.12) | Free · IAP "Runify Pro" $4.99/mo–$79.99 | n/a (iOS has no ad badge) |

Notes (all accessed 2026-11-16):
- Rating/download/date figures are the values printed on the listing that day; Play rounds counts ("1.72M").
- #4's privacy policy is an Outside Inc. corporate policy: https://www.outsideinc.com/privacy-policy/ (linked from listing).
- #5 privacy: https://runify.appliendirect.fr/legal · ToS: https://runify.appliendirect.fr/fr/terms · listing languages: French (France), English (United States) — **no Arabic**.
- #6 privacy: https://runifyapp.com/privacy-policy · ToS: https://runifytermsandconditions.carrd.co/ · Apple page: 97.6 MB, iOS 16.0+, "Only for iPhone", language **English only**.
- Play "Data safety" (developer-declared, not verified by us): #1 "No data shared with third parties" (collects Location, Personal info +4); #2 "may share Personal info" (collects Personal info, Health and fitness +5); #3 "No data shared" (Location, Health and fitness +3); #4 "may share Location, Personal info +3" (Location, Personal info +5); #5 "No data shared" (Personal info, **"Data isn't encrypted"**).

## 2. Feature matrix (from listing text only)

Legend: Y = stated on listing · – = not stated · ? = implied, unverified

| Feature | 1 Leap | 2 StepsApp | 3 Leap | 4 MapMyWalk | 5 Runify (Android) | 6 Runify (iOS) |
|---|---|---|---|---|---|---|
| Passive step counting | Y | Y | Y | Y | – | ? |
| Daily step goal | Y | Y | Y | Y | – | – |
| History (day/week/month) | Y | Y | Y | Y | – | Y (lifetime/weeks) |
| Charts | Y | Y | Y | Y | – | Y (XP graphs) |
| GPS walk + route map | – | – | Y | Y | – | Y |
| Streaks | Y | Y | ? | ? | – | Y |
| Achievements / badges / ranks | Y | Y | Y | Y | – | Y (Bronze to Iridescent) |
| Home-screen widget | Y | Y | ? | ? | – | ? |
| Watch / wearable sync | – | – | – | Y (Garmin, watches) | – | ? |
| Health-platform sync | Samsung Health | Google Fit | Google Fit | Google Fit | – | Apple Health (per review) |
| Arabic / RTL | ? (no claim on listing) | ? (claims "20+ languages", Arabic unconfirmed) | ? | ? | **– (FR/EN only, verified)** | **– (English only, verified)** |
| Account required | ? (has "backup") | Y (friends/leaderboard) | – (claims "No login required") | Y (community/account) | – (claims no account) | Y (clubs/leaderboards) |
| Ads | Y | Y | Y | Y | – | – |
| Paid tier / subscription | Y | Y | Y | Y | Y | Y |
| Extra modules in one app | workouts, water, HR, weight/BMI, sleep | themes, social sharing | indoor + GPS modes | 600+ activities, routes, audio cues, challenges | race calendar, organiser tools | run clubs, country leaderboards, share templates |

## 3. Review pain-point themes (short quotes; source = reviews shown on the store listing, 2026-11-16)

### 3.1 Ads sit inside the core loop
- "Yes it has ads, but I don't find them overly invasive." — 5★ review, #3 (Play listing).
- "Ads are present but at least they're not popping up constantly." — 5★ review, #1 (Play listing).
- Fact: 4 of 5 Play listings carry the "Contains ads" badge, and all six sell IAP.
  "No ads" is a recognized selling point — a competing Play listing markets itself as
  "StepGo – Pedometer without Ads" (in.codingvalue.stepgo), seen in the same search results (2026-11-16).

### 3.2 Battery / power saving breaks counting
- "And don't run the app and battery saver at the same time. It really goes screwy." — 5★ review, #3.
- The #1 listing itself warns: "Due to device power-saving settings, some phones may stop counting
  steps when the screen is locked" (Play listing, #1 description).

### 3.3 Accuracy complaints
- "my calorie burn keeps going up even if I am sitting still" — 2★ review, Map My Walk iOS, 2026-04-28
  (https://apps.apple.com/us/app/map-my-walk-walking-tracker/id307861492).
- "doesn't log all my steps some days" — 3★ review, #2.
- "this app does tell you it's overall accuracy depends on phone hardware" — 5★ review, #3.

### 3.4 Subscription pressure
- IAP ranges printed on the listings: $1.99–$119.99 (#1), $1.49–$39.99 (#3), $5.99–$39.99 (#4),
  $9.49–$189.99 (#5), "Runify Pro Monthly $4.99" up to "$79.99" (#6). #3's listing claims
  "100% Free… No locked features" while showing IAP — a marketing/reality gap worth avoiding.

### 3.5 Privacy is claimed, not provable
- "I feel like when I post my runs, it shares my location to people… can someone tell me if this
  app shares your location with other people publicly?" — 3★ review, Runify iOS (id6746146450).
- Listing-declared sharing: #2 shares "Personal info" with third parties; #4 shares
  "Location, Personal info and 3 others"; #5 states "Data isn't encrypted".

### 3.6 Lost walks / sync failures
- "It freezes during walks, loses parts of my activity, and today it failed to sync and lost my
  whole walk." — review, #4 (Play listing).
- "every time I go on it says access token expired… now I have to re add my friends again" — review, #2.
- "the map and elevation gain were missing from my data" — Map My Walk iOS review, 2026-10-30.
- "Prevented entirely from creating an account and actually using the app." — 1★ Map My Walk iOS
  review, 2026-11-13 (account gate = hard failure for that user).

### 3.7 Date/localization bugs (directly relevant to Arabic/RTL)
- "my app says 'Yesterday' for the current day, and every day going back is also off by one" and
  "it has also stopped counting calories, and stays at 0 kcal" — review, #2.
- "my calendar suddenly switched to German days of the week. I live in the US" — review, #2.

## 4. Answers to the Lead's specific questions

### 4.1 Is MapMyWalk still maintained? — Yes, actively.
- Android listing: "Updated on Nov 7, 2026" (Play, accessed 2026-11-16).
- iOS: current version **26.16.0**, currentVersionReleaseDate **2026-11-10**, seller
  **Outside Television Inc**, 4.83★ / 486,749 ratings (iTunes lookup API,
  https://itunes.apple.com/lookup?id=307861492&country=us).
- Ownership: Outside Interactive acquired MapMyFitness (MapMyRun/Ride/Walk) **from Under Armour,
  announced 2024-08-30** — https://www.outsideinc.com/outside-interactive-inc-acquires-mapmyfitness-outdoor-fitness-tracking-applications-from-under-armour-inc/
- So "abandoned" is **not** a usable precedent. Usable negatives for #4 instead: account gate
  (1★ review above), listing-declared third-party sharing of location, network/map dependency,
  subscription upsell, and heavy legacy structure. 'unverified': whether #4 can count steps without
  an account (not installed).

### 4.2 Runify (iOS, id6746146450) — facts
- Free + "Runify Pro" IAP: page lists **$4.99 ("Runify Pro Monthly"), $14.99, $19.99, $29.99, $49.99, $79.99**.
- 4.8★ / **245 ratings**; released **2025-07-21**; version **100.2.12**, released **2026-11-10**;
  97.6 MB; iOS 16.0+; **iPhone only**; **English only**.
- Standout features (listing): XP + rank tiers (Bronze to Iridescent), friends/global/country/club
  leaderboards, private run clubs via invite link, streak history, post-run share templates.
- Android version: **none found** — Play search for "runify" (2026-11-16) returns no OneDegree Labs
  app (only unrelated "Runnify: Running Plans", club.runnify.*). Treat as iOS-only.

### 4.3 What built-in counters already give free (so our wedge is not "we count steps")
- **Samsung Health** (Play): 1,000,000,000+ downloads, free, **no "Contains ads" badge**, automatic
  step counting with goals — https://play.google.com/store/apps/details?id=com.sec.android.app.shealth
- **Health Connect** is the platform step-data hub (Android docs "Track steps":
  https://developer.android.com/health-and-fitness/health-connect/features/steps); Google's own
  Fit API guidance is now "Fit Android API to Health Connect migration"
  (https://developer.android.com/health-and-fitness/health-connect/migration/fit).
- Therefore step counting + goal + a chart is already free on most Android phones. Our wedge must be
  *trust and language*, not feature count: provable offline (no INTERNET permission), no account,
  no ads, no SDKs, Arabic-first RTL, own Canvas route drawing (no map tiles/keys), honest estimates.

## 5. Gap analysis and our wedge

| Gap in the six | Evidence | Our v1 answer |
|---|---|---|
| Ads inside the core loop | 4/5 Play listings "Contains ads"; §3.1 | No ad SDK at all in v1 |
| Accounts / cloud / data loss | #4 account gate + lost walks; #2 token/sync bugs; §3.6 | No account, no cloud, local Room only; export via SAF |
| Privacy claims unverifiable | #2/#4 declared third-party sharing; #5 "not encrypted"; §3.5 | No INTERNET permission; Data safety = nothing collected; in-app privacy screen |
| No Arabic-first competitor surfaces | Arabic-keyword Play search returns the same English-first apps (Leap, StepsApp, ITO, Pacer, Sweatcoin…); #5 FR/EN only; #6 English only; no listing claims Arabic | Arabic + English at parity, full RTL, tested day/date boundaries |
| GPS map needs network/tiles | #3/#4 use maps; #1/#2 have no GPS tracking at all (matrix) | Own Canvas polyline; no tiles, no keys, offline; sensor-only fallback |
| Date-boundary and localization bugs | §3.7 | Pure-Kotlin date keys, boot-safe daily aggregation, rollover tests |
| Battery / OEM killing background counting | §3.2; #1's own caveat | Event-driven TYPE_STEP_COUNTER + reconciliation on boot, limits documented |
| Fake precision | calorie-inflation review; "accuracy depends on hardware" | Estimates labelled as estimates; stride from height/weight, never claimed as measured |

**Wedge in one sentence:** a free, ad-free, no-account, no-INTERNET Arabic-first step & walk tracker
whose route map works offline, with a privacy claim verifiable from its permission list — no listing
we found combines those (one Play listing does market "without Ads", so "no ads" alone is not a moat).
**Risk noted for the brief:** "privacy" is already marketing copy for #3 ("100% Free & 100% Private")
and #5 ("No account management or disclosure of personal data"); our claim must be *specific and
provable*, never the same vague sentence.

## 6. Sources (all accessed 2026-11-16)
- Play listings #1–#5 (URLs in §1); raw HTML saved under tmp/research/app1..5.html.
- #6: https://apps.apple.com/us/app/runify-running-tracker/id6746146450 · API https://itunes.apple.com/lookup?id=6746146450&country=us · reviews RSS https://itunes.apple.com/us/rss/customerreviews/page=1/id=6746146450/sortby=mostrecent/json
- Map My Walk iOS: https://apps.apple.com/us/app/map-my-walk-walking-tracker/id307861492 · API https://itunes.apple.com/lookup?id=307861492&country=us · reviews RSS https://itunes.apple.com/us/rss/customerreviews/page=1/id=307861492/sortby=mostrecent/json
- Outside/MapMyFitness (2024-08-30): https://www.outsideinc.com/outside-interactive-inc-acquires-mapmyfitness-outdoor-fitness-tracking-applications-from-under-armour-inc/
- Samsung Health: https://play.google.com/store/apps/details?id=com.sec.android.app.shealth · Health Connect steps: https://developer.android.com/health-and-fitness/health-connect/features/steps · Fit→Health Connect: https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Arabic-keyword Play search: https://play.google.com/store/search?q=%D8%B9%D8%AF%20%D8%A7%D9%84%D8%AE%D8%B7%D9%88%D8%A7%D8%AA&c=apps

Limits: store ratings are rounded and self-selected; only up to 3 Play reviews are exposed per listing,
so themes are indicative, not statistically representative; no app was installed or run.
