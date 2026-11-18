# Sabeel — UI prototypes for decision

Static, phone-sized mockups of the three directions that came out of the Impeccable
native-platform review (30/40, three P1 defects) and the Qiyas Council mode C review.
They are decision artifacts, not shipped code.

## What is here

| File | Question | What it shows |
| --- | --- | --- |
| `p1-home.html` / `p1-home.png` | Q1-a, Q2-a | Home with the trust fix: solid 100% ring (no seam), intact value pair, 3-up metric row replaced by 2 honest cards, no truncation |
| `p1-insights.html` / `p1-insights.png` | Q1-a | Insights with one named 30-day window; "5 of 30 days" (no "days days"); all-time distance labelled all-time |
| `p2-settings-top.html` / `.png` | Q1-b | Settings grouped into Daily goal / Tracking / Units & language; one row pattern with one control per row |
| `p2-settings-bottom.html` / `.png` | Q1-b | Your data and App as plain rows; deletion moved out of the primary slot into an error-container danger zone |
| `p3a-home.html` / `.png` | Q1-c, Q2-b | Authored accents: Fraunces for title and big numbers, custom line icons, clay completion badge, teal-tinted selected tab, dawn halo |
| `p3b-home.html` / `.png` | Q1-c, Q2-c | Full "Desert Dawn": wordmark + date line, larger ring, serif status voice, tri-panel metrics, overline headers, clay tab underline, paper gradient |
| `index.html` | all | Gallery view with the options side by side and the recommended order |
| `proto.css`, `build.mjs` | — | Shared tokens/components and the generator that writes the six pages |
| `fonts/` | — | Local Roboto and Fraunces woff2 subsets used for rendering |

## How the PNGs were produced

The pages are authored at 360 × 800 CSS px and rendered with headless Edge:

```powershell
& $edge --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=3 \
  --window-size=360,800 --screenshot=rendered.png file:///.../page.html
```

That yields 1080 × 2400 PNGs, the same frame as the emulator screenshots in
`docs/review/2026-11-17/design-review/`. Re-render after changing `build.mjs`:

```powershell
node docs/design/prototypes/build.mjs
```

## Mapping to the decision

- **Q1 (priority direction).** a = P1 Home + P1 Insights (truth fixes only). b = P2
  Settings top + bottom (structure, one row pattern, danger zone). c = P3a + P3b
  (brand specificity, light and full).
- **Q2 (design intent).** a = P1 Home (quiet finish, stock system). b = P3a Home
  (authored accents, recommended middle). c = P3b Home (full Desert Dawn).
- **Q3 (order).** Phase 1 = Q1-a (ship-blocking), Phase 2 = Q1-b, Phase 3 = Q2-b,
  with full Desert Dawn only after a brand-recognition test.

## What is faithful to the build, and what is not

- Colors are the shipped tokens from `ui/theme/Color.kt` (paper `#FAF7F2`, teal
  `#0F6B5C`, clay `#B4653A`, sand `#8C6212`, error `#B3261E`); shapes follow the
  current 20/28 dp radii.
- Values and most copy are from the seeded review build (8,400 steps, 8,000 goal,
  84 active minutes, 6.3 km, 336 kcal, 5-day streak) or from the existing
  `strings.xml`. New wording (for example "Change it any time.", "A quiet nudge
  around a time you choose.", the danger-zone description) is proposal text only.
- The prototypes fake the status bar and the Android system nav bar; they do not
  render real status icons. The Home warning card in the review screenshots is an
  emulator sensor artifact and is deliberately absent from the healthy-state
  mockups.
- Roboto is the shipped typeface. Fraunces is a stand-in serif used only in the C
  prototypes; the app does not bundle it. Bundling a serif is a real decision with
  an Arabic pairing consequence — these mockups are Latin-only and cannot prove
  Arabic/RTL parity.
- No new permissions, dependencies or product behavior are implied. Nothing here
  is wired into the app.

## Known gaps in this first set

- Arabic and RTL variants are not mocked; the shipped app has EN + AR parity and
  every identity decision has to pass through it.
- Body data (height, weight, gender) appears in the Settings sequence description
  but is not in a screenshot; it uses the same row pattern as Units & language.
- The scroll states are single frames, not scrollable pages; each page is one
  screen at 360 × 800.
- Dark mode is not mocked for the C prototypes; the tokens exist, the identity
  treatment in dark is an open design task.
