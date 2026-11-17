# Fill and host the privacy policy — owner runbook

The Play policy needs a live, public, HTTPS privacy-policy URL, and the app's
in-app link must point to the same page. The page source is ready; only owner
data and hosting are missing. **This is a blocker (B1 in
[docs/legal/BLOCKERS.md](../legal/BLOCKERS.md)) — do not submit to Play before
it is closed.**

## 1. What gets hosted

| File | Host it? | Why |
|---|---|---|
| `docs/privacy-site/index.html` | **YES — this single file (plus nothing else)** | Self-contained, tracker-free, bilingual EN/AR page with a strict CSP. |
| `docs/privacy-site/` other files | Only if `index.html` gains assets | Keep the folder self-contained. |
| `docs/legal/privacy-policy.md` | **NO** | Markdown source + internal notes (release checklist, hosting notes). |
| Anything else under `docs/` | **NO** | Contains internal clearance memos and review material. |

> **Never point GitHub Pages (or any host) at the repository's `/docs` folder.**
> That would publish the whole internal docs tree, including legal analysis and
> the policy's internal release checklist. Use a separate public repository, or
> a folder-only deploy (Cloudflare Pages / Netlify direct upload) containing
> just `index.html`.

## 2. Fill every placeholder (English and Arabic)

Open `docs/privacy-site/index.html` and replace **all** occurrences:

| Placeholder | Arabic counterpart | Where (line numbers) |
|---|---|---|
| `[DEVELOPER LEGAL NAME]` | `[اسم المطوّر القانوني]` | ~19, ~101, ~168 / ~180, ~247 |
| `[CONTACT EMAIL]` | `[البريد الإلكتروني للتواصل]` | ~19, ~169 / ~248 |
| `[POSTAL ADDRESS]` | `[العنوان البريدي]` | ~19, ~170 / ~249 |
| `[DATE OF FIRST PUBLICATION]` | `[تاريخ أول نشر]` | ~19, ~96 / ~178 |

Rules:

- The developer name must match the Google Play developer name **exactly**
  (same string in the listing, the policy and the Data safety answers).
- The email must be monitored — Play reviewers and users use it.
- The postal address is optional in the page text; delete the line consistently
  in both languages instead of leaving an empty placeholder.
- Set **Effective date** = the day the page first goes public; also update
  "Last updated" to the same day.
- Keep English and Arabic content equivalent; do not add claims the app does not
  meet (no accounts, no analytics, no ads, no INTERNET permission, no cloud
  sync).
- Remove the publishing instructions HTML comment at the top of the file before
  publishing (it is only guidance for the owner).

Also mirror the same values in the repository record
`docs/legal/privacy-policy.md` (lines ~6, 13, 89–91, 97, 173–175) and remove
its internal release-checklist block (lines ~180–195) from any copy that could
become public. The markdown is the source of record; the HTML is the hosted
copy.

## 3. Check for leftovers

From the repository root:

```powershell
Select-String -Path docs/privacy-site/index.html `
  -Pattern '\[(DEVELOPER|CONTACT|POSTAL|DATE|اسم|البريد|العنوان|تاريخ)'
```

Expected: **no output**. Then read the page top-to-bottom in both languages.

## 4. Host it (any of these)

Recommended: a **separate public repository** named e.g. `sabeel-privacy`
containing only the filled `index.html` (rename to `index.html` at the root),
published with GitHub Pages:

1. Create the public repository (no other files, no README with private data).
2. Add `index.html`, commit, push.
3. Repository **Settings → Pages → Deploy from a branch → main / root**.
4. Wait for the HTTPS URL, e.g. `https://<owner>.github.io/sabeel-privacy/`.

Alternatives (both fine, both folder-only):

- **Cloudflare Pages** — direct upload of the folder containing only
  `index.html`; gives `https://<project>.pages.dev`.
- **Netlify Drop** — drag the single file/folder; gives
  `https://<name>.netlify.app`.

Requirements the URL must meet (Play User Data policy): publicly reachable
without login, **HTTPS**, not geofenced, no PDF, not an editable document, stable
for the life of the listing.

## 5. Verify the hosted page

- [ ] URL opens in a clean browser and on a phone; no mixed-content warnings.
- [ ] Page source shows no external requests (the CSP is
      `default-src 'none'; style-src 'unsafe-inline'` — nothing loads).
- [ ] English and Arabic sections render; Arabic is RTL-correct.
- [ ] Developer name, email, address and dates are final and placeholder-free.
- [ ] Page works with JavaScript disabled (it must — it uses none).
- [ ] Save a screenshot/PDF-free evidence note (URL + date) in
      `docs/legal/release-evidence-log.md`.

## 6. Wire the URL into the build and app

Build the release with the property (no trailing slash needed):

```powershell
.\gradlew.bat --console=plain :app:bundleRelease `
  -Psabeel.privacyPolicyUrl=https://<host>/privacy
```

This sets `BuildConfig.PRIVACY_POLICY_URL` (see `app/build.gradle.kts`). The UI
must hide/disable the privacy link when the value is empty and open the URL when
it is set; verify on the signed release build that:
1. the link opens the hosted page,
2. the page is the same URL you will paste into the Play listing,

and add that URL to the Play Console **App content → Privacy policy** field and
to the Data safety free-text where asked.

CI builds are unsigned and do not set the property; that is expected. If a CI
build is ever used for a store artifact, add the property to the CI command
instead of hard-coding the URL in source.

## 7. Close the blocker

Update `docs/legal/BLOCKERS.md` B1 to CLOSED with: hosted URL, date verified,
the developer identity used, and the build command that embedded it. Then update
`docs/legal/release-signoff.md`.
