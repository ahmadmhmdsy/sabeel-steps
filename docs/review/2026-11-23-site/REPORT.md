# Site build and verification record — 2026-11-23

This folder records how the static project site in [`site/`](../../../site) was built and checked, so the
published pages can be reviewed without trusting the author's summary.

## What was built

- `site/index.html` — English landing page (hero, why, features, privacy, limits, screenshots, FAQ, download).
- `site/ar/index.html` — full Arabic mirror, `lang="ar" dir="rtl"`, Arabic copy and Arabic alt text.
- `site/privacy/index.html` — privacy policy, English and Arabic on one page (the hosted policy the app links to).
- `site/404.html`, `site/robots.txt`, `site/sitemap.xml`, `site/manifest.webmanifest`.
- `site/assets/` — `styles.css` (no external fonts, scripts or trackers), hero images (EN/AR, 640/960/1600 px),
  the 1200×630 social card, favicons in SVG/PNG, and the screenshot set.
- `.github/workflows/pages.yml` — deploys **only `site/`** as a Pages artifact. The `/docs` tree is never published
  (see `docs/privacy-site/FILL-AND-HOST.md`, which warns against that).

SEO surface: unique title/description per page, canonical URLs, `hreflang` en/ar/x-default, Open Graph and Twitter
cards, `SoftwareApplication` + `WebSite` + `FAQPage` + `BreadcrumbList` JSON-LD, sitemap, robots, semantic headings,
favicons and a web manifest. No external requests: the pages load no third-party font, script or tracker.

## Images: what is real and what was edited

The screenshots come from the **real emulator captures** in [`docs/assets/screenshots/`](../../assets/screenshots)
(API 36 AOSP-ATD image, 1080×2400). The **only** content edit is that the emulator-only
"No step sensor on this phone" notice band was removed from the three Home screenshots used on the site
(see the docstring of `tools/build_images.py`, function `crop_notice`). Everything else is the real UI.

That edit is disclosed on the site itself: the screenshots lede and the note under the gallery on
`site/index.html` and `site/ar/index.html` say the emulator notice was hidden and point to the README, which
carries the **unedited** captures. No step data was invented: the values shown were injected through the debug
build's step receiver on the emulator, which the same note states.

## Measured runtime results

`runtime-report.json` was produced by `tools/capture_screenshots.py` (headless Chrome via Playwright, served under
the same `/sabeel-steps/` path GitHub Pages uses). `tools/summarize_evidence.py` summarises it:

- 8 page renders: EN/AR × desktop 1440×1000, mobile 390×844, narrow 320×800, plus privacy and 404.
- **0 broken images, 0 horizontal overflow, 0 external requests**, one `h1` per page, `dir` ltr/rtl as expected.
- 96 local href/src references, 33 fragment anchors and 22 images (all with alt text) checked.
- Contrast ratios (WCAG 2.x): ink/cream 13.78:1, teal-deep/cream 8.87:1, muted/cream 6.43:1, accent/paper 5.38:1,
  teal/cream 6.00:1 — all above the 4.5:1 AA threshold for normal text.

`screenshots/` holds the rendered page captures; `tools/` holds the scripts used (`build_images.py` also builds the
README hero and social card; `verify_static.py` is the static checker; `analyze_bands.py` measured the notice band
before cropping).

## Independent Lead check (this repository's review pass)

Run after the build, separately from the scripts above (see the commit that introduced this folder):

- Parsed all four HTML pages: one `h1` each, `lang`/`dir` correct, no duplicate ids, every `img` has alt text, no
  external script or stylesheet, all local links/assets resolve and every fragment anchor exists.
- Parsed all JSON-LD blocks, `manifest.webmanifest`, `sitemap.xml` and `robots.txt`: valid, all sitemap URLs map to a
  real page, robots points at the sitemap.
- Cross-checked the README/README.ar/CONTRIBUTING/SECURITY/CODE_OF_CONDUCT/release-body markdown links and GitHub
  anchors against the files they point at: no missing targets.
- Visual check of the rendered EN/AR desktop and mobile pages.

## Known limits of this evidence

- Emulator and headless-browser only: no physical-device rendering, no Lighthouse/Core Web Vitals field data and no
  screen-reader session. Contrast was computed from the CSS palette, not from rendered pixels.
- `404.html` uses root-absolute `/sabeel-steps/...` asset paths so it works at any depth on the project Pages site;
  it will 404 locally if the site is served from the server root instead of `/sabeel-steps/`.
- The Arabic copy is self-reviewed; native-speaker review of the app and site is still pending (stated on the site).
