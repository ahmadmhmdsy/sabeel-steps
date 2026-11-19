# Release blockers — Sabeel v1.0

Items that **cannot be closed by code**. Each has an owner action, an acceptance
test, and what it blocks. Status as of 2026-11-17.

Legend: **OPEN** = blocks release · **PARTIAL** = code side ready, owner action
pending · **CLOSED** = evidence recorded.

---

## B1 — Privacy policy: placeholders + HTTPS hosting — OPEN

**Blocks:** Play submission, in-app privacy link, Data safety consistency.

The policy source exists in two places and both still contain placeholders:

```
docs/legal/privacy-policy.md
docs/privacy-site/index.html
```

Replace **every** placeholder (English and Arabic copies):

| Placeholder | Arabic counterpart | Replace with |
|---|---|---|
| `[DEVELOPER LEGAL NAME]` | `[اسم المطوّر القانوني]` | Full legal name as registered with Play |
| `[CONTACT EMAIL]` | `[البريد الإلكتروني للتواصل]` | A monitored support email (same as Play listing) |
| `[POSTAL ADDRESS]` | `[العنوان البريدي]` | A real contact address |
| `[DATE OF FIRST PUBLICATION]` | `[تاريخ أول نشر]` | Effective date of the hosted policy |

Then host **only** `docs/privacy-site/` on a public HTTPS host (GitHub Pages,
Cloudflare Pages, Netlify or equivalent), verify the URL in a clean browser,
remove the internal notes section from what is hosted, and pass the URL to the
build as `-Psabeel.privacyPolicyUrl=...`. Full steps:
[docs/privacy-site/FILL-AND-HOST.md](../privacy-site/FILL-AND-HOST.md).

**Acceptance:** public URL returns 200, shows the final identity, no bracket
placeholders, no trackers, ARIA/Arabic renders, and equals the URL in the
Play listing and in the built app. **Owner:** developer/legal.

---

## B2 — Trademark / name clearance for "Sabeel" (سبيل) — OPEN

**Blocks:** publishing under the Sabeel name and the permanent package name
`com.sabeel.steps`.

Sabeel is a religiously and culturally meaningful word; it is also used by
other products. No clearance search has been recorded. Before the first upload:

1. Search Play Store for "Sabeel" and Arabic "سبيل" — check for apps in the
   Health & Fitness category or with the same icon/name.
2. Search national trademark registers where distribution is planned (at least
   Iraq and the developer's jurisdiction; ideally WIPO Global Brand Database).
3. Check domain/handle availability if a web presence is planned.
4. Record the searches with dates in the release evidence log.
5. If a conflict is found, either rename (package name cannot change after first
   upload — decide **before** uploading) or obtain permission.

**Acceptance:** recorded search results + a go/no-go note by the owner.
**Owner:** owner/legal. *This is not a legal opinion; consult a local
trademark professional if a conflict is plausible.*

---

## B3 — Play Console account, declarations and testing obligations — OPEN

**Blocks:** publication; partly opens only after assets and policy are ready.

The account owner must, in the Play Console:

1. Choose account type (personal vs organisation; organisation needs a D-U-N-S
   number), pay the US$25 fee with an accepted non-prepaid card, and complete
   identity verification.
2. If it is a **personal account created after 2023-11-13**: run closed testing
   with **≥12 testers opted in continuously for ≥14 days**, then request
   production access. Plan 2–3 weeks.
3. Complete App content: privacy policy URL, app access (no login), ads (none),
   content rating (IARC), target audience, Data safety (**no data collected or
   shared**; on-device processing only), **Health apps declaration**, and the
   **foreground-service declarations** for `health` and `location` (with demo
   videos if the console asks).
4. Enrol in **Play App Signing** and upload the upload certificate
   `docs/release/upload-certificate.pem`; confirm the SHA-256 fingerprint in
   docs/release/KEYSTORE.md.

**Acceptance:** console dashboard shows every declaration complete; signed AAB
accepted on an internal testing track. **Owner:** account owner. Detailed
per-field checklist: [docs/release/PLAY-SUBMISSION-CHECKLIST.md](../release/PLAY-SUBMISSION-CHECKLIST.md).

---

## B4 — Store assets — OPEN

**Blocks:** store listing (Play will not accept a listing without screenshots,
icon and feature graphic).

Current local assets are placeholders (black screenshots, 0-byte video). Needed:
512×512 icon, 1024×500 feature graphic, 2–8 phone screenshots per language
(EN + AR RTL), optional tablet screenshots, short/full description EN+AR, FGS
demo video if required, and a first-run onboarding capture for the listing.
The screenshots must show the **final** build (minified release) and must not
show placeholder privacy links or test data containing personal information.

**Acceptance:** assets in §7 of the Play checklist; visual QA against the final
release build. **Owner:** product/design.

---

## B5 — Distribution decision — OPEN

**Blocks:** the release plan, the signing model and what "done" means.

Decide and record: **Google Play only**, or Play **plus** direct APK distribution
(website/Arabic app stores). Consequences:

- Play-only: Play App Signing, internal→closed→production tracks; users update
  through Play.
- Play + direct APK: direct builds must be signed with a stable key the owner
  controls; those installs will not receive Play updates and cannot be migrated
  without an in-app update path. Play App Signing means the Play-distributed
  build is re-signed by Google, so a direct-APK install of the same package can
  collide with the Play version; choose one signing/user journey deliberately.
- Any non-Play catalogue (e.g. an Arabic store) may impose its own content and
  health-app requirements.

**Acceptance:** a one-paragraph decision committed to
`docs/legal/release-signoff.md`. **Owner:** owner.

---

## B6 — Arabic copy native review — OPEN

**Blocks:** claiming Arabic is production quality (Arabic is a launch language).

A native Arabic speaker must review every user-visible string (plurals,
terminology for "steps/stride/goal/streak", GPS/permission wording, units,
notifications and the privacy-page Arabic column) on the release build, plus RTL
layout and Arabic-Indic digit rendering. Fixes go through the UI workstream.

**Acceptance:** signed review note with the build hash and the list of strings
changed. **Owner:** product/owner.

---

## B7 — Independent release evidence / device validation — OPEN

**Blocks:** any claim that v1.0 works on real devices.

The repository currently has no device-verified run for the fixed build. After
the Lead's green build: install the **signed release** AAB/APK on at least two
physical devices (API 26–30 and API 33+), run the manual smoke list
(onboarding, step counting with screen off, midnight rollover, walk resume,
CSV, delete-all, language switch, reboot re-arm), and record results with the
artifact SHA-256. The source ladder adds device-specific rows: a counter phone
must be measured (no "≈", Diagnostics names the counter) and a detector-only
phone must count through the detector. Do not reuse earlier emulator evidence;
it was labelled NOT VERIFIED — EMULATOR-ATTEMPTED and includes black
screenshots/0-byte video, and the 2026-11-19 emulator run covers only the
labelled estimate tier. A command-level checklist to run and paste back is
[docs/legal/HARDWARE-QA-CHECKLIST.md](HARDWARE-QA-CHECKLIST.md).

**Acceptance:** evidence entries in docs/legal/release-evidence-log.md.
**Owner:** engineering/owner.

---

## Blockers that are already closed by code (for reference)

| Was | Now |
|---|---|
| No VCS / no reproducible history | Git repository with baseline + plan commits (Lead) |
| Release unshippable (no minify, no signing) | `build.gradle.kts` release minify/resource-shrink + optional keystore signing; upload keystore generated |
| No license / notices | Apache-2.0 LICENSE, NOTICE, THIRD-PARTY-NOTICES in assets |
| No CI | `.github/workflows/android.yml` |
| Privacy URL not build-configurable | `BuildConfig.PRIVACY_POLICY_URL` from `sabeel.privacyPolicyUrl` |

Code-side fixes are in [docs/review/2026-11-17/fix-release.md](../review/2026-11-17/fix-release.md).
