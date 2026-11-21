# Play submission checklist — Sabeel v1.0

**Owner:** account owner (human). Engineering cannot complete these items; this
file records exactly what must be done and what evidence proves it.
**Companion:** [docs/legal/play-compliance-checklist.md](../legal/play-compliance-checklist.md)
(policy research, sources, status legend) and
[docs/legal/BLOCKERS.md](../legal/BLOCKERS.md) (open blockers).
**Status legend:** [ ] not started · [~] in progress · [x] done with evidence · [!] blocker.

> This is not legal advice. Play policy and target-API rules change; re-read the
> console prompts and the linked policy pages within 7 days of submission.

## 0. Locked identifiers

| Field | Value |
|---|---|
| App name | Sabeel (سبيل) |
| Package / applicationId | `com.sabeel.steps` (permanent once first uploaded) |
| Version | versionName `1.0.0`, versionCode `1` |
| Category | Health & Fitness (alternate: Health & Fitness → activity tracker) |
| Price | Free, no ads, no in-app purchases, no subscriptions |
| Accounts / login | None. All functionality works without an account. |
| Min / target SDK | 26 / 36 |
| Permissions | ACTIVITY_RECOGNITION, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_HEALTH, FOREGROUND_SERVICE_LOCATION, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, RECEIVE_BOOT_COMPLETED, plus the normal library-merged WAKE_LOCK and DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION (not prompted). **No INTERNET, no ACCESS_BACKGROUND_LOCATION.** |
| Privacy policy URL | `https://<host>/privacy` — filled and hosted per [docs/privacy-site/FILL-AND-HOST.md](../privacy-site/FILL-AND-HOST.md) |
| Support email | `<OWNER CONTACT EMAIL>` (must match the hosted policy) |

## 1. Before creating the app in Play Console

- [ ] **1.1 Account type decided** — personal vs organisation. Organisation
      requires a D-U-N-S number. *Evidence: console screenshot.*
- [ ] **1.2 Registration fee paid** (US$25, non-prepaid card; Iraqi card
      acceptance unverified). *Evidence: console receipt.*
- [ ] **1.3 Identity verification completed** (legal name, address, contact;
      keep ID/address proof ready). *Evidence: console status page.*
- [ ] **1.4 Name cleared** — trademark/domain/Play search for "Sabeel" and
      "سبيل"; confirm `com.sabeel.steps` is free on Play. *Evidence:
      docs/legal/BLOCKERS.md B2 closed with search results.*
- [ ] **1.5 If personal account created after 2023-11-13:** plan the closed
      testing obligation — **≥12 testers opted in continuously for ≥14 days**
      before production access can be requested. *Evidence: tester list export.*
- [ ] **1.6 Privacy policy hosted at a public, non-geofenced HTTPS URL, no PDF,
      no login, no redirect to an editable doc** — see FILL-AND-HOST. *Evidence:
      URL loads in a clean browser with the final developer identity.*
- [ ] **1.7 Store assets produced** (see §7). The current local assets are
      placeholders (black screenshots, 0-byte video) and must not be uploaded.

## 2. Create the app and configure signing

- [ ] **2.1 Create app** with the exact package `com.sabeel.steps`, name
      "Sabeel", free app, default language English (US) **or** Arabic — the
      listing supports both; default language must match the primary audience.
- [ ] **2.2 Enrol in Play App Signing** (mandatory for new apps). Upload the
      upload certificate `docs/release/upload-certificate.pem`; verify the
      SHA-256 fingerprint in the console matches
      `7B:12:AD:6B:FA:F0:78:17:E7:6A:25:39:95:4F:7B:4B:DE:2E:13:F6:B3:34:54:82:48:30:10:25:66:B6:15:2D`.
      Procedure: [docs/release/KEYSTORE.md](KEYSTORE.md) §4.
- [ ] **2.3 Signed AAB uploaded** by the owner from the local machine (CI is
      unsigned by design). Build:
      `gradlew :app:bundleRelease -Psabeel.privacyPolicyUrl=<hosted URL>`.
      *Evidence: AAB hash added to `dist/SHA256SUMS.txt`.*

## 3. Store listing

- [ ] **3.1 Short description** (≤80 chars), honest, no "best"/"medical":
      e.g. English "Offline step counter and walking tracker. No ads, no
      accounts." Arabic equivalent.
- [ ] **3.2 Full description** (≤4000 chars) in EN and AR: features, that all
      data stays on the device, no INTERNET permission, walk tracking uses GPS,
      streak/goal/CSV, health disclaimer ("not a medical device; step counts are
      estimates"). No medical claims, no permission misstatements.
- [ ] **3.3 App icon** 512×512 PNG, 32-bit, below 1 MB.
- [ ] **3.4 Feature graphic** 1024×500 PNG/JPEG, no transparency.
- [ ] **3.5 Phone screenshots** 2–8 per language, 16:9 or 9:16, min 320 px on
      the short side, max 3840 px, PNG/JPEG, no device framing required.
- [ ] **3.6 Tablet screenshots** (optional but recommended) 7" and 10".
- [ ] **3.7 Localization** — EN and AR listing text; Arabic RTL screenshots.
- [ ] **3.8 Contact details** — support email matching the privacy policy,
      website optional (privacy page).
- [ ] **3.9 Privacy policy URL** in the designated field and in-app link built
      with `-Psabeel.privacyPolicyUrl`.

## 4. App content declarations (Console → App content)

- [ ] **4.1 Privacy policy** field filled with the hosted URL.
- [ ] **4.2 App access** — "All functionality is available without special
      access" (no login, no credentials).
- [ ] **4.3 Ads** — **No ads**. No ad SDKs are present.
- [ ] **4.4 Content ratings (IARC)** — complete the questionnaire for a
      health/fitness utility; step counting/GPS are user-generated activity, not
      violence/sexual/gambling content. Expect "Everyone"/"3+"; owner answers.
- [ ] **4.5 Target audience** — choose the age groups the app is designed for
      (fitness utility; recommend 13+ unless owner decides otherwise). Do not
      select "designed for families" unless the app truly meets the Families
      policy — it does not need to.
- [ ] **4.6 News app** — No. **Government app** — No. **COVID-19** — N/A.
- [ ] **4.7 Data safety** — complete the form:
  - Data collected: **No**.
  - Data shared: **No**.
  - Data processed on device only (steps, GPS route, body data, settings) is
    **not collection**; state this in the privacy policy and any free-text field.
  - The opt-in Health Connect read is also on-device only: today's step total is
    read from Health Connect, never transmitted, never written back. Say so in
    the free-text field if one is offered.
  - No SDKs that collect data; no INTERNET permission.
  - Encryption in transit: not applicable (no transmission).
  - Users can delete data in-app (delete-all). No account deletion URL needed
    (no accounts).
  - *Evidence: console screenshot of the completed form; the wording must match
    the hosted policy.*
- [ ] **4.8 Health apps declaration** — declare the app as a health/fitness
      app. Step counting and walking are fitness tracking; the app makes no
      diagnostic/treatment claims. Complete the Health Content and Services
      declaration truthfully (no medical device claims, disclaimer present).
      **Health Connect:** declare the Steps data type as **read** (opt-in, only on
      phones with no hardware step sensor, used as a daily floor; no write
      access is requested and the app never writes to Health Connect).
- [ ] **4.9 Foreground service declarations** — declare both types:
  - **health** — continuous step counting using `ACTIVITY_RECOGNITION`.
  - **location** — user-started GPS walk tracking.
  The console requires the declared use to match the in-app feature; record a
  short **demo video** showing each feature (Walk screen, notification, stop
  action) if the console asks for one. *Evidence: uploaded declaration +
  video link.*
- [ ] **4.10 Location permissions** — the walk feature uses foreground location
      only, started by the user; in-app disclosure explains it before the
      runtime prompt. Do not declare background location (app does not request
      it).
- [ ] **4.11 Photo/video, contacts, SMS, call log, microphone** — not requested;
      no declarations needed.
- [ ] **4.12 Permissions declaration** — no restricted permissions are used
      beyond the FGS/health/location items above. The build declares
      `android.permission.health.READ_STEPS` for the opt-in Health Connect read
      (runtime health permission, requested through the system Health Connect
      screen from the Settings card; revocable there) and the optional,
      feature-gated `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND`
      so the counting service can keep reading that synced total while the app
      is closed. Decline leaves counting unaffected.
- [ ] **4.13 Health Connect data access form** — in the Health Connect section,
      list Steps as read-only, describe the fallback reason (a mixed-source
      floor so a watch/other app's day is not missed; the app also has its own
      sensor counting), note the optional background read, and keep the privacy
      policy/Data safety answers consistent.

## 5. Testing tracks and rollout

- [ ] **5.1 Internal testing** — upload the signed AAB to internal testing;
      install on 2+ physical devices (API 26–36), run the manual smoke list:
      onboarding/permissions, step counting with screen off, midnight rollover,
      goal notification, walk start/stop/resume, history/insights, CSV
      export/import, delete-all, language switch EN↔AR, reboot re-arm.
      *Evidence: test log in docs/legal/release-evidence-log.md.*
- [ ] **5.2 Pre-launch report** — review crashes/ANRs/accessibility notes from
      the Play-generated report; fix crashes before production.
- [ ] **5.3 Closed testing** — required for new personal accounts: ≥12 testers,
      14 continuous days, then apply for production access. Closed testing is
      still distribution: Data safety, health declaration, rating and privacy
      policy must already be complete.
- [ ] **5.4 Production rollout** — staged rollout 10% → 50% → 100%, watching
      Play vitals (ANR/crash), or a full rollout if the owner accepts the risk.
- [ ] **5.5 Rollback plan** — if a release is broken: halt rollout, upload a
      fixed AAB with a higher versionCode. Play App Signing keeps signing
      continuity.

## 6. Post-launch (first 7 days)

- [ ] **6.1** Check Play vitals daily (crashes, ANRs, startup time).
- [ ] **6.2** Read reviews for permission/GPS/language issues; respond without
      requesting personal/health data.
- [ ] **6.3** Verify the privacy policy URL still loads and still matches the
      build (privacy URL property used for the uploaded AAB).
- [ ] **6.4** Archive the signed AAB, R8 `mapping.txt`, SHA-256 hashes and the
      certificate fingerprint in the release record.

## 7. Assets inventory (must exist before §3)

| Asset | Size / format | State |
|---|---|---|
| App icon 512 | 512×512 PNG 32-bit | TO-BE-PRODUCED |
| Feature graphic | 1024×500 PNG/JPEG | TO-BE-PRODUCED |
| Phone screenshots EN | 2–8, 1080×1920 or similar | REPLACE PLACEHOLDERS |
| Phone screenshots AR | 2–8, RTL | TO-BE-PRODUCED |
| Tablet screenshots | 7"/10" (recommended) | TO-BE-PRODUCED |
| FGS demo video | ≤30 s, unlisted link | TO-BE-RECORDED |
| Listing text EN/AR | short + full | TO-BE-WRITTEN |
| Privacy page | hosted HTTPS | BLOCKED — see FILL-AND-HOST |

## 8. Stop-ship conditions

Do **not** submit to production while any of these is true:

1. A blocker in [docs/legal/BLOCKERS.md](../legal/BLOCKERS.md) is open (privacy
   policy placeholders, name clearance, account/declaration items).
2. `gradlew :app:lintDebug` exits non-zero (release gate).
3. The release AAB was not built with `-Psabeel.privacyPolicyUrl` and the
   in-app link is visible but empty/broken.
4. The uploaded AAB's upload certificate fingerprint differs from
   docs/release/KEYSTORE.md without a recorded rotation.
5. No device smoke test of the signed release build was recorded.