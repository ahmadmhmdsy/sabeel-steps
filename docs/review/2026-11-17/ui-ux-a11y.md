# UI / UX / A11y review — Sabeel (سبيل)

Reviewer: ui-ux-a11y · 2026-11-17 · Scope: user-facing product only

## 1. SCOPE & METHOD

Read all 22 files under `app/src/main/java/com/sabeel/steps/ui/` (3,386 lines), both string files and the sensor string pair, `res/values*/themes.xml`, `colors.xml`, launcher/status vectors, `AndroidManifest.xml`, `MainActivity`, `AppViewModels`, and the UI-facing parts of `StepsRepository`, `StepMath`, `Notifications`, both services and `CsvIo`. Executed: programmatic XML key/placeholder diff, WCAG contrast computation for the palette, a JDK 21 `jshell` probe of Arabic date/number formatting, and greps for `@Preview`, hardcoded text, semantics, `rememberSaveable`, `collectAsState`, RTL paddings, colour-token usage and `androidTest`. Did **not** run Gradle, install the APK, use TalkBack or inspect a rendered screen.

## 2. VERIFIED STRENGTHS

- **Exact string parity**: EN 170 keys + 1 plural, AR 170 + 1, sensor 22/22; missing/extra/placeholder mismatches = 0 (programmatic diff). AR plural has all 6 CLDR forms (`values-ar/strings.xml:44-51`).
- **No hardcoded copy** except the `"−"` glyph (`components/CommonComponents.kt:239`); all text is `stringResource`.
- **Lifecycle-aware state**: no bare `collectAsState`; all screens use `collectAsStateWithLifecycle` (`HomeScreen.kt:61`, `SabeelRoot.kt:79`).
- **Insets correct**: root Scaffold consumes `WindowInsets.safeDrawing` (`SabeelRoot.kt:95`); top bar asks for zero insets so nothing double-pads (`ScreenScaffold.kt:71`).
- **RTL-safe primitives**: zero `padding(left/right)` in `ui/`; start/end only (`SettingsScreen.kt:266`); `Icons.AutoMirrored.ArrowBack` (`ScreenScaffold.kt:58`); `supportsRtl=true` (`AndroidManifest.xml:27`).
- **Palette matches spec §7 exactly** (`Color.kt:13-26`): #0F6B5C, #B4653A, #D9A441, #FAF7F2, #FFFFFF, #101817, #18211F, #B3261E, teal→#4FD1B5 ring; 20dp cards (`Shape.kt:11`); dynamic colour deliberately off (`Theme.kt:7-21`).
- **Canvas visuals have text alternatives** (`ProgressRing.kt:53`, `BarChart.kt:66`, `RouteSketch.kt:39`); chart descriptions include computed averages (`HomeScreen.kt:114`, `HistoryScreen.kt:55-59`) — spec §6 met in substance.
- **Touch targets**: steppers explicit 48dp with per-button descriptions (`CommonComponents.kt:232-256`); buttons `heightIn(min=52.dp)` (`:273,299`); M3 chips/switch/nav items inherit 48dp.
- **Font-scale adaptation on 3 screens** (Home `HomeScreen.kt:95-101`, Insights `:44`, Walk `WalkScreen.kt:199-249`), though not Settings — see F-10.
- **Gentle, non-shaming copy** (brief §7.6): four status thresholds (`HomeScreen.kt:235-239`), quiet neutral reminder/goal notifications (`values/strings_sensor.xml:24-30`).
- **Onboarding value-first**: privacy promise on page 1 (`OnboardingScreen.kt:198-216`), permissions only on page 4 (`:133-142`), body data skippable.
- **Walk discard really discards**: no row written, checkpoint deleted, live state reset (`WalkTrackingService.kt:214-217,599-609`).
- **Locale survives process death** (SharedPreferences read in `attachBaseContext`: `AppLocale.kt:20-42`, `MainActivity.kt:21-23`); and Home's CTA only navigates, so a walk cannot start by one accidental tap (`SabeelRoot.kt:140`).

## 3. FINDINGS

Tags: **[BUG]** intended behaviour wrong · **[RISK]** conditional · **[TASTE]** quality.

| ID | Sev | Title | Evidence | Why it matters | Fix |
|---|---|---|---|---|---|
| F-01 | High | [BUG] "Delete all data" can resurrect a huge fake step count | `AppViewModels.kt:241-251` wipes DataStore incl. step baseline (`SettingsStore.kt:80-82`) but never stops `StepCounterService`; next event replays raw-since-boot against baseline 0 (`StepsRepository.kt:62-67`, `StepPipeline.kt:26-44`) | User asks to erase data and gets today inflated to ~raw count (e.g. 200,000) written to History; core correctness promise broken | Stop/reset the step service before clearing prefs; regression test |
| F-02 | High | [BUG] Arabic devices launch in English; system locale overridden | `AppLanguage.fromCode(null) ?: EN` (`domain/Models.kt:10`) always applied in `AppLocale.wrap` (`AppLocale.kt:26-33`); no `localeConfig`/per-app language in manifest | Stated wedge ("Arabic first-class") fails on first launch for Arabic phones | Default stored value else `LocaleList.getDefault()`; override only after explicit choice; add `localeConfig` |
| F-03 | High | [BUG] RTL charts label the wrong bars | Canvas draws `entry[0]` leftmost (`BarChart.kt:78-100`); labels `Row` lays out RTL so `label[0]` is rightmost (`:102-116`) | In Arabic every weekday sits under a different day's bar → misread data on Home and History | Mirror bars and labels together under RTL, or force LTR for both |
| F-04 | High | [BUG] Body data called optional/clearable but is neither; imperial toggle contradicts it | `values/strings.xml:173-174` ("Every field is optional", "change or clear"); Settings has steppers only, no clear (`SettingsScreen.kt:212-291`); "Imperial (mi, lb)" (`strings.xml:128`) while height/weight stay cm/kg (`SettingsScreen.kt:232,254`) | False claim (brief §7.3); imperial users see cm/kg; estimates silently use defaults for users who think fields are empty | Add a "Not set"/clear state or fix copy; convert or relabel units |
| F-05 | High | [RISK] In-app language does not localise notifications or channel names | Only Activity context is wrapped (`MainActivity.kt:21-23`); notifications use app/service context (`Notifications.kt:44-79,135-159`, `StepCounterService.kt:231-238`); channels created once, never renamed | Arabic-on-English-device keeps English FGS/reminder/goal notifications and English channel names in system settings | Wrap service contexts with `AppLocale.wrap`; recreate channels on language change |
| F-06 | Med | [RISK] Primary CTA below the fold | Home order: 230dp ring → status → cards → section → 120dp chart → button (`HomeScreen.kt:147-229`); content ≈ 696dp vs ≈ 605dp visible on 360×800dp | "Start a walk" needs scrolling on a typical phone, worse with the permission card shown | Move CTA above the chart or sticky bottom |
| F-07 | Med | [BUG] Arabic grammar/plural/terminology defects | `strings.xml:82,84` "آخر 30 أيام" (should be يومًا); `:23` دقيقة for 2-10; `:149,151` "%d يومًا" for 1-10; `:123` missing "إلى"; `:100-102` "الـGPS"; `:169` "موجز"=summary not feed; `:132` "إمبراطوري"; `:162` "أفضّل عدم الذكر"; terminology drift جولة/مشية and سلسلة/تتابع (`strings_sensor.xml:18,28,30`, `strings.xml:42`) | Arabic correctness is an acceptance criterion and this is the first-launch surface | Pluralise count strings; rewrite listed strings; native review gate |
| F-08 | Med | [BUG] Reminder switch unlabeled for TalkBack; permission state goes stale | `Switch` with no label/semantics, row not `toggleable` (`SettingsScreen.kt:176-185`); permission checks are `remember(permissionEpoch)` only (`HomeScreen.kt:63-70`, `WalkScreen.kt:62-74`) | TalkBack reads "Switch, off" with no name; warning card persists after granting in system Settings | `Modifier.toggleable(role=Switch)`; re-check on `ON_RESUME` |
| F-09 | Med | [RISK] Light-theme non-text contrast failures | Computed WCAG: mint ring end #4FD1B5 on #FAF7F2 = **1.77:1** (`Color.kt:25`, `ProgressRing.kt:24-26`); sand goal line = **2.10:1** (`BarChart.kt:71-77`); below-goal bars = **2.49:1** (`BarChart.kt:94`); ring track 1.11:1 | 1.4.11 needs 3:1 for meaningful graphics; ring mint half and goal line are near-invisible | Darken mint/sand in light scheme; raise bar alpha; add track contrast |
| F-10 | Med | [BUG] fontScale 2.0 gaps contradict spec §6 claim | Settings body rows leave ≈72dp for the value so "170 سم" wraps from ≈1.3× (`SettingsScreen.kt:219-239,247-260`); ring stays 190dp so ≥5-digit counts ellipsis (`HomeScreen.kt:96,154-160`); chart labels `maxLines=1, Clip` (`BarChart.kt:104-113`) | README claims font-scale-2.0 layouts; Settings/History/ring degrade | Stack label over stepper >1.3×; size ring to content; abbreviate labels |
| F-11 | Med | [BUG] "7-day average" is two different numbers on two screens | Home divides by 7 (`HomeScreen.kt:105-113`); Insights divides by rows present (`AppViewModels.kt:130-134`, `StepMath.kt:44-45`; `StepsRepository.kt:42-45`) | New user sees e.g. 142 on Home vs 500 on Insights, both "7-day" | One definition; label honestly or pad to 7 |
| F-12 | Med | [RISK] Saved walks invisible; live walk has no back contract | `WalkRepository.walksFlow()/get()` unused by UI (`WalkRepository.kt:11-13`); back is plain `popBackStack` (`WalkScreen.kt:88`, `SabeelRoot.kt:144-149`) | Users cannot review saved walks; back gesture silently leaves a running walk | Walk list/detail in History; "walk in progress" banner; confirm on back while live |
| F-13 | Med | [RISK] CSV import overwrites existing days without confirmation | Picker launches then upserts immediately (`SettingsScreen.kt:339-342`, `CsvIo.kt:39-52`); copy only says "Restore" | A stale backup silently replaces real days — the app's only backup path | Confirm with overwrite warning; show progress/result |
| F-14 | Low | [TASTE] Steppers never disable at bounds and are the only input | `canDecrease/canIncrease` never passed (`CommonComponents.kt:224-226`; `SettingsScreen.kt:127-133`); goal clamped silently (`AppViewModels.kt:203`) | Dead +/− look broken; 8,000→20,000 takes 24 taps | Disable at bounds; numeric entry with validation |
| F-15 | Low | [TASTE] Charts goal-scaled with no axis values | `top = max(maxData, goal)*1.15` (`BarChart.kt:69`); no y labels | First weeks show near-flat bars; values unreadable | Scale to data-aware max; label y-max; value on tap |
| F-16 | Low | [BUG] "Two permissions, nothing else" contradicted later | `values/strings.xml:175-176`; location is a third runtime permission (`WalkScreen.kt:76-84`) | Trust hit exactly where trust is built | Reword to "two permissions for step counting" |
| F-17 | Low | [TASTE] Destructive actions styled as primary | Delete-all is filled primary (`SettingsScreen.kt:353-357`); discard confirm default colour (`WalkScreen.kt:140-149`); only delete confirm is error-coloured (`SettingsScreen.kt:417-421`) | Destructive affordances don't read as destructive | Consistent error/outlined destructive style |
| F-18 | Low | [TASTE] Design-system drift; icon scale | Clay `secondary` #B4653A defined but 0 uses in `ui/` (`Color.kt:34`); `shapes.large` 28dp unused; launcher mark ~40% of 108dp canvas, ~2dp strokes at 48dp (`ic_launcher_foreground.xml:12-31`); notification icon is footprints vs launcher stride arcs (`ic_stat_steps.xml`) | Weakens "original design system" claim; inconsistent brand | Use clay once deliberately; enlarge/embolden icon; unify mark |
| F-19 | Low | [RISK] TalkBack noise; no heading semantics | Ring announces description + both child Texts (3 nodes; `ProgressRing.kt:49-80`, `HomeScreen.kt:148-167`); titles are plain Text; `LoadingScreen` unlabeled (`ScreenScaffold.kt:107-117`) | Redundant announcements; no heading navigation | `clearAndSetSemantics` on ring; `heading()`; label loading |
| F-20 | Low | [RISK] Transient state not saveable | Only onboarding page uses `rememberSaveable` (`OnboardingScreen.kt:59`); dialogs and `pendingStart` use plain `remember` (`SettingsScreen.kt:77-79,377`, `WalkScreen.kt:63-64`) | Rotation dismisses open pickers/dialogs | `rememberSaveable` for dialog flags |
| F-21 | Low | [TASTE] Numerals forced Latin even in Arabic | `NumberFormat.getIntegerInstance(Locale.US)` (`StepMath.kt:49-50`); `String.format(Locale.US,…)` (`Formatters.kt:32,39,49`); ASCII `%` (`strings.xml:25`) | Consistent today, but ignores Arabic-Indic preference and risks mixing if formatters diverge (JDK probe: ar date digits Latin, ar NumberFormat Arabic-Indic) | Choose/document one policy; on-device Arabic format test |

### High findings in detail

**F-01.** Delete-all clears DataStore, which also holds `baselineRaw/todaySteps/baselineDate` (`SettingsStore.kt:63-82`), while the foreground service keeps running and `recordRawStep` re-reads that state on every write (`StepsRepository.kt:60-68`). The next sensor event computes the delta against baseline 0, i.e. steps since boot, and upserts it; after onboarding the user sees an enormous fabricated day. Fix by stopping/resetting `StepCounterService` around the wipe and covering it with a test.

**F-02.** `AppLocale.wrap` always installs the stored language and the stored default is English regardless of device, with no `localeConfig` for Android 13+ per-app language. The first screen an Arabic phone shows is therefore English — the exact opposite of the positioning. Default to the device locale when the user has not chosen.

**F-03.** `Canvas` geometry is never mirrored by layout direction, but the weekday label `Row` is; in RTL the first label lands under the last bar, so every weekday name is attached to the wrong day on Home and History. Mirror bars and labels together (or force LTR for both) and verify with an Arabic screenshot.

**F-04.** Onboarding promises optional, clearable body fields, but Settings exposes only ±1 steppers with fixed defaults that feed stride/calorie math, and the "Imperial (mi, lb)" chip changes distances while height/weight remain cm/kg. Both are honesty issues the brief forbids (§7.3) and both are cheap: add a "Not set" option and lb/ft display, or fix the copy.

## 4. GAPS

- No `app/src/androidTest`: no Compose UI, RTL, fontScale or screenshot tests; `app/build.gradle.kts:94-98` has JUnit + debug `ui-tooling` only.
- No `@Preview` composables anywhere (grep: zero), so design review has no tooling support.
- No evidence for the four README accessibility/RTL claims (no screenshots, no TalkBack runs; AVD smoke was "untestable headless").
- No walk-history UI; no sensor-missing screen (notification only: `StepCounterService.kt:236-237`); no permission-denied recovery deep link.
- No Arabic native review (README roadmap item 4 open; F-07 shows why it matters); no lint/a11y CI evidence.
- Spec drift: spec §5 says 3 onboarding steps, code has 4 (`OnboardingScreen.kt:49`) — undocumented.

## 5. UNVERIFIED / OPEN QUESTIONS

- **[inference]** F-01 derived from code reading, not executed; size depends on device raw value and service lifetime.
- **[inference, probed]** Arabic digits: JDK 21 gives Latin day digits and Arabic-Indic `NumberFormat` output for ar; Android ICU may differ, so F-21's mixing risk is unproven on-device.
- **[inference]** Contrast ratios computed from hex tokens; gradient/alpha rendering may vary slightly. Body/secondary text computes 8.5–17:1 (pass); only F-09 graphics fail.
- **[unverified]** Whether Compose animations honour "Remove animations" (spec §7); nothing explicit in code.
- **[unverified]** TalkBack order, M3 NavigationBar label clipping at 2.0, TimePicker dialog fit at 2.0, Arabic weekday-label clipping at default scale (estimated ≈45.8dp needed vs 45.7dp available for "الثلاثاء" on 360dp), walk save-failure recovery, dark-mode edge cases.

## Appendix A — string parity

| file | strings | plurals |
|---|---|---|
| `res/values/strings.xml` | 170 | 1 (`streak_days`: one, other) |
| `res/values-ar/strings.xml` | 170 | 1 (`streak_days`: zero/one/two/few/many/other) |
| `res/values/strings_sensor.xml` | 22 | 0 |
| `res/values-ar/strings_sensor.xml` | 22 | 0 |

Missing in AR: none. Extra in AR: none. Placeholder mismatches: none. Only one pluralised resource exists; other count-bearing strings are unpluralised (`value_minutes`, `settings_export_success`, `settings_import_success`, `history_range_label`, `history_cd_chart`, `home_cd_chart`, `goal_value`, `insights_sentence`, `sensor_notif_goal_text`).

## Appendix B — ranked top-10 UX fixes

1. Make delete-all safe (F-01) — stop/reset step service before wiping.
2. Default language from the device, not hard-coded EN (F-02).
3. Mirror chart bars+labels in RTL (F-03).
4. Resolve body-data "optional/clear" and imperial/cm-kg contradictions (F-04).
5. Localise notifications/channels with in-app language (F-05).
6. Move "Start a walk" above the chart (F-06).
7. Native Arabic pass: plurals, 30-day label, terminology (F-07).
8. Label the reminder switch; re-check permissions on resume (F-08).
9. Raise non-text contrast of ring/goal line/bars (F-09).
10. Finish fontScale 2.0 for Settings/ring/chart labels (F-10).
