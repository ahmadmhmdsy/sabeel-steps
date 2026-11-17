# WS2 — UI, Arabic and accessibility: change report

Date: 2026-11-17 · Baseline commit `0663edf` · Plan `478d134` · Shared task: `task-8` (owner `ui`)

Scope: Phase 3, Tasks 9–12 of `docs/superpowers/plans/2026-11-17-production-readiness.md`, plus the
follow-up asks from the Lead (device-locale default, app-open re-arm, localised notification channels
on language change, gated privacy link, compile fix in `ScreenScaffold`).

Findings from `docs/review/2026-11-17/ui-ux-a11y.md` addressed here: F-01, F-02, F-03, F-07, F-08,
F-09, F-10, F-12, F-13, F-19, F-21.

## Files changed

| File | Change |
| --- | --- |
| `ui/components/BarChart.kt` | RTL: bars are mirrored inside `Canvas` from `DrawScope.layoutDirection` (`x = size.width - (index+1)*(barWidth+gap) + gap`), so every bar sits above its own label in `ar`. Below-goal bars at alpha 0.7, goal line `tertiary`, zero-value track `outline`, single chart semantics node (`clearAndSetSemantics`), one-line ellipsised labels, contrast ratios documented in the file. |
| `ui/theme/Color.kt` | Light scheme: `tertiary = #8C6212` (5.08:1 on `#FAF7F2`), `onTertiary = white`; `outline = #79746C` (4.34:1); `ringGradientColors(dark=false)` end `#1F8B72` (3.93:1). Dark scheme untouched (already compliant). |
| `ui/components/ProgressRing.kt` | Track uses the new `outline`; the ring is one semantics node with a real steps content description instead of a decorative placeholder. |
| `ui/components/CommonComponents.kt` | `SectionHeader` and `EmptyState` titles get `heading()`; new `CountingStatusCard(title, body, modifier, actionLabel, onAction)` used by Home. |
| `ui/components/ScreenScaffold.kt` | Top-bar title gets `heading()`. `LoadingScreen`: `stringResource` hoisted out of `Modifier.semantics { }` and the progress indicator carries `cd_loading` (this was the Lead-reported compile error at line 121). |
| `ui/screens/HomeScreen.kt` | Start-walk CTA moved above stat cards and the 7-day chart (above the fold at 360×640). Status section: `PermissionCard` for a missing permission or `NO_PERMISSION`; `CountingStatusCard` for `STOPPED` (action = `home_status_action_start` → `onStartCounting`) and `NO_SENSOR` (explains the phone limits honestly, no action). Ring shrinks by font scale and digit count, cards stack at fontScale > 1.3, chart/ring have descriptions, permission re-check on resume. Stateless `HomeScreen(ui, onStartWalk, onStartCounting)` contract plus two stateful entry points (the 1-arg one the current `SabeelRoot` calls already forwards `HomeViewModel.startCounting()`, so the CTA works without further wiring). |
| `ui/screens/HistoryScreen.kt` | Top-level `SavedWalkUi` data class as frozen; scrollable column with chart, range label, total, day rows, then a Walks `SectionHeader` with rows (date · duration · distance · steps) and an empty line. Tapping a row opens a dialog with `RouteSketch` when the route is non-empty; selected walk survives config change (`rememberSaveable`). |
| `ui/screens/WalkScreen.kt` | Idle shows a resume card when `walkUi.pendingCheckpoint` (resume / discard, both honest copy), and `walk_start_blocked` when the service refused to start; summary heading; permission re-check on resume. |
| `ui/screens/SettingsScreen.kt` | Reminder row is one `toggleable(role = Switch)` target with title + description. CSV import asks for confirmation first (`settings_import_confirm_*`). Web privacy row only when `BuildConfig.PRIVACY_POLICY_URL` is non-blank. Imperial units show the body-data note. Body rows stack above fontScale 1.4. Language change also re-creates notification channels with the new locale. Notification permission re-check on resume. |
| `ui/screens/InsightsScreen.kt` | Plural range label inside the sentence, card title heading, stat cards stack at large font scale. |
| `ui/screens/OnboardingScreen.kt` | Language page preselects `AppLocale.current` (device default counts), page titles heading, permission re-check on resume; finish copy no longer promises counting without permissions. |
| `ui/i18n/AppLocale.kt` | `current()` = stored choice else device language (D7: `ar` → AR, anything else → EN); new `clear()` for delete-all; `wrap()`/`apply()` unchanged. |
| `ui/format/Formatters.kt` | `dateTimeText(millis)` for saved-walk rows (medium date + time, locale-aware). |
| `ui/SabeelRoot.kt` | Once onboarding is done, every foreground entry re-arms the step service and notification channels via `LifecycleResumeEffect`: `StepCounterService.ensureStarted(app)` + `Notifications.ensureChannels(AppLocale.wrap(context))`. |
| `res/values/strings.xml`, `res/values-ar/strings.xml` | All frozen contract keys added; `value_minutes` replaced by `minutes_value` plural; `history_range_days`, `streak_days` plurals with the full Arabic CLDR set; `cd_loading`; `settings_units_body_note`; `settings_privacy_web`. Arabic terminology pass: walk = مشية, streak = سلسلة, no جولة / تتابع / موجز, GPS written without الـ, imperial shown as ميل only with a metric body-data note; honest permission copy; reminders say "around the chosen time"; insights empty title "تحتاج الأنماط إلى بضعة أيام". EN and AR key sets are identical (192 keys each, checked by script), EN/AR format arguments match per key. |

No changes were needed under `values-night`: the dark palette already passed the contrast check and the
light-theme fixes do not leak into it.

## Behaviour notes

- Home is usable with every permission denied: the screen renders, the status card explains what is
  missing, and the CTA is still present (it navigates to Walk and the service refuses safely).
- The saved-walks section is driven only by `HistoryUiState.walks`; when the repository returns an
  empty list the honest empty line is shown.
- Import is described as a merge (larger day wins), matching `CsvIo.import → StepsRepository.restore`
  and its `StepMerge` rule.
- The "no step sensor" copy only tells the user that this device cannot count steps or track walks.
- A resumed walk never duplicates the interrupted one: the card is shown while a checkpoint exists and
  disappears once the ViewModel resolves it.

## Contract / cross-workstream dependencies (all confirmed present in the current tree)

- `platform.StatusStore.StepServiceStatus { COUNTING, NO_PERMISSION, NO_SENSOR, STOPPED }` —
  Home maps `NO_PERMISSION` to the permission card, `STOPPED`/`NO_SENSOR` to the status card.
- `HomeUiState.serviceStatus`, `HomeViewModel.startCounting()`; `HistoryUiState.walks:
  List<SavedWalkUi>` (imports `com.sabeel.steps.ui.screens.SavedWalkUi`); `WalkUiState.pendingCheckpoint`,
  `startBlocked`, `WalkViewModel.state`, `resumePending()`, `discardPending()`.
- `Notifications.ensureChannels(Context)`, `StepCounterService.ensureStarted(Context)`,
  `AppLocale.wrap/apply/clear/current`.
- `BuildConfig.PRIVACY_POLICY_URL` is already generated by the release workstream (blank default →
  the row is hidden, which is the intended state until the owner sets `sabeel.privacyPolicyUrl`).

## Residual risks / notes for the Lead

1. The release owner has not passed `-Psabeel.privacyPolicyUrl` yet, so the web privacy link stays
   hidden; the in-app privacy page is always reachable.
2. Notification channels cannot change importance after creation; names/descriptions are refreshed by
   `ensureChannels`, but a channel created by an older build keeps its old text until the app data is
   cleared. Reinstalling or delete-all clears this.
3. Until the user taps a language chip, the DataStore `language` value may still say EN on an Arabic
   phone while resources follow the device locale. Resources and DOM direction are correct; the value
   converges on the first explicit choice.
4. `HomeScreen` keeps three overloads on purpose so the frozen stateless contract and the current
   `SabeelRoot` call site both work; a later cleanup can delete the unused stateful overload.
5. I could not run Gradle in this workstream. Every touched file was re-read and mentally compiled
   (imports, composable/Modifier scoping, `@Composable` calls kept out of semantics lambdas); the
   authoritative compile/lint run is the Lead's Task 17.
6. Arabic copy was reviewed for terminology and CLDR plural forms, but native-speaker review before
   release is still recommended.

## Suggested commit message

`feat(ui): RTL chart, counting status, saved walks, resume prompt, Arabic/a11y pass`

```
- BarChart mirrors bars in RTL; light-theme contrast (tertiary/outline/ring) >= 3:1, documented
- Home: status card for STOPPED/NO_SENSOR, CTA above chart, font-scale and permission handling
- History: saved walks list + route dialog; Walk: resume/blocked states
- Settings: confirm before import, toggleable reminder switch, gated privacy link, localized channels
- AppLocale: device-locale default + clear; SabeelRoot re-arms service/channels on foreground
- en+ar strings: all contract keys, plurals, terminology and honest copy
```
