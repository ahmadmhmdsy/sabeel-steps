# Design implementation evidence — 2026-11-19

Screen captures of the debug build after the approved Phase 1–3 design work. They
complement the 2026-11-17 review screenshots and the static mockups in
[`docs/design/prototypes/`](../../design/prototypes/README.md).

## Build under test

- Source: `580bb12` (Phase 1 `99bc109`, Phase 2 `6fbc4d7`, Phase 3 `0693e4f`,
  plus the string cleanup `9509ca5`). The UI code is the Phase 3 tree.
- `:app:testDebugUnitTest :app:assembleDebug` — **BUILD SUCCESSFUL** 2026-11-19
  00:26 +03:00, **79 tests, 0 failures, 0 errors**.
- Debug APK: 12,005,495 B, SHA-256
  `965cb3e178821b7cdfdfd19f78f59edcdd58f9ba19d4dda942a361f45e240f3c`.
- `:app:lintDebug` — **BUILD SUCCESSFUL** 2026-11-19 00:44 +03:00: **0 errors,
  42 warnings, 1 hint** (`app/build/reports/lint-results-debug.txt`). The three
  remaining `UnusedResources` warnings predate this work.
- Device: Android 16 (API 36) emulator AVD `api36`, 1080 × 2400 at 420 dpi,
  headless. The AVD has **no hardware step counter**, so the honest "No step
  sensor on this phone" card is visible in every Home capture.

## Data context

The database was seeded with 21 daily rows (2026-10-29 … 2026-11-18) plus a row
for the capture day: 7,240 steps, 5,107.8 m, 289.6 kcal, 72 active minutes. The
emulator clock crossed midnight during the session, so captures before 00:06
show 2026-11-18 and later ones 2026-11-19. The goal was 8,000; for the completed
state it was temporarily lowered to 7,000 (7,240 ≥ 7,000) and restored afterwards.

## What the captures show

| File | Source build | State |
| --- | --- | --- |
| `en-home-partial.png` | `580bb12` APK | Light, English Home: 7,240 of 8,000, dawn-halo partial ring with a quiet 10 % grey remainder, no-sensor card, 72 active minutes, 5.1 km / 290 kcal, calm status line |
| `en-home-complete.png` | `0693e4f` | Light Home at goal (goal temporarily 7,000): solid ring, clay check badge at the top-right, "Goal reached. Enjoy the rest of your day." |
| `en-home-complete-dark.png` | `0693e4f` | The same completed state in dark theme: mint ring, clay badge on dark surfaces |
| `en-settings-top.png` | `580bb12` APK | Light Settings: Daily goal with the one-line stepper, "Suggest from my routine", Tracking rows/switches, start of Units & language |
| `en-settings-bottom.png` | `580bb12` APK | Light Settings: Gender, Your data (Export/Import CSV), App (Privacy/About), the error-container "Delete all data" danger card, estimate footer |
| `en-history.png` | `580bb12` APK | Light History: 7-day bars with the goal line, 7-day total, per-day list |
| `en-insights.png` | `580bb12` APK | Light Insights: 30-day average, best day, goal-hit rate, all-time distance, named 30-day summary card |
| `en-dark-home.png` | `580bb12` APK | Dark Home, same partial state |
| `en-dark-settings.png` | `580bb12` APK | Dark Settings |
| `en-fontscale-1.5-settings.png` | `0693e4f` | Settings at font scale 1.5: stacked rows put the chevron/switch on the trailing edge |
| `ar-home.png` | `0693e4f` | Arabic RTL Home at goal (8,500 of 8,000): badge mirrored to the top-left, mirrored nav, Arabic status line |
| `ar-settings-top.png` | `0693e4f` | Arabic RTL Settings: stepper mirrored (+ on the left), chevrons flipped, right-aligned copy |
| `en-delete-dialog.png` | `0693e4f` | Delete confirmation opened from the danger zone |
| `en-units-dialog.png` | `0693e4f` | Units chooser dialog |

### Capture provenance

- The `580bb12` captures are the final installed APK.
- `en-home-complete*` were taken from the Phase 3 build just before the string
  cleanup; the removed strings had no remaining call sites, so the UI is identical.
- `ar-*`, the two dialogs and the font-scale capture are from the Phase 3 build
  `0693e4f`, i.e. before the final 4° ring-start colour tweak. That tweak only
  affects the first degrees of a partial ring and does not change any layout,
  RTL or dialog claim here.
- The emulator was recycled by unrelated headless AVD processes from another
  project on the same machine during the session, which is why the capture set is
  not a single uninterrupted run.

## Gaps this evidence does not close

- No physical device and no real step sensor: sensor permission, foreground
  service, reboot persistence and notification behaviour are still untested here.
- Arabic copy has not had a native-speaker review (owner action); these captures
  show layout only.
- TalkBack was not walked through manually in this pass. The controls carry
  semantics (role, content descriptions on the stepper buttons, headings on
  section titles), but a screen-reader run is still required.
- Font scale was checked at 1.5 only; 0.85 and 2.0 are unverified, and
  landscape/tablet layouts were not captured.
- Digit shaping in Arabic is mixed by existing behaviour (step numbers use Latin
  digits, some plural-formatted numbers follow the locale); this work did not
  change it.
- The 3-working-day / 5-user validation gate for the brand direction cannot be
  observed from a seeded emulator; it remains an owner/field task.

## QA note for the next run

`StepsRepository.reconcile()` resolves today's total as
`max(DataStore cached steps, Room row)`, so manually seeding a lower total needs
the DataStore cache cleared as well, otherwise the next launch restores the cached
value. This is expected pipeline behaviour, not a defect.
