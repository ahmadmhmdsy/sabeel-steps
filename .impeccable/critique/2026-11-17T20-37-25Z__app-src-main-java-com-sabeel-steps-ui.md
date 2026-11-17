---
target: app/src/main/java/com/sabeel/steps/ui (whole Sabeel UI)
total_score: 30
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:E:\\android_apps\\steps_app\\app\\src\\main\\java\\com\\sabeel\\steps\\ui"
timestamp: 2026-11-17T20-37-25Z
slug: app-src-main-java-com-sabeel-steps-ui
---
Method: design review only (detector not applicable, native platform)

# Design Critique — Sabeel (Android)

**Target:** `app/src/main/java/com/sabeel/steps/ui` — whole UI tree; primary entry `ui/screens/HomeScreen.kt`
**Scope:** 4-tab shell (Home / History / Insights / Settings) + Walk, Onboarding, Privacy, About, Diagnostics; theme and shared components; English and Arabic (RTL); light and dark; 1.0 and 1.3 font scale. Evidence: 27 rendered screenshots plus UIAutomator trees on an AOSP API 36 emulator (1080×2400, synthetic seeded data).

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 4 | Ring + status sentence + "Updated HH:mm" + service/permission cards + Diagnostics live facts. Loading and save states exist. |
| 2 | Match Between System and Real World | 3 | Copy is plain and human, but "This week" over a 30-day window and "7-day average" that is actually a 30-day average break the mental model. |
| 3 | User Control and Freedom | 3 | Back, cancel, discard, delete confirmations are present; onboarding has no skip and the destructive delete is visually inviting. |
| 4 | Consistency and Standards | 2 | Settings mixes bare controls, inline cards and full-width pills; "Daily reminder" is both header and row label; Insights has three conflicting time windows. |
| 5 | Error Prevention | 3 | Steppers clamp, CSV import confirms, delete and walk-discard confirm; but primary-styled deletion is easy to start and remaining data-loss paths are only dialog-guarded. |
| 6 | Recognition Rather Than Recall | 3 | Labeled bottom nav, visible chips, inline hints; but stat terminology ("goal hit rate", "active minutes") is never defined and the chart has no axis values. |
| 7 | Flexibility and Efficiency | 2 | Fixed ±500 goal and ±1 body steppers, no presets, no direct entry, no quick goal chip, no shortcuts. |
| 8 | Aesthetic and Minimalist Design | 3 | Calm palette and clean spacing, but Home stacks warning card + hero + CTA + three stats + chart, and Settings runs ~2.5 screens. |
| 9 | Error Recovery | 3 | Plain-language errors near the source, snackbar messaging, no technical leakage; some write failures end in a generic line without a retry affordance. |
| 10 | Help and Documentation | 4 | "Why is my count low?" is genuinely excellent contextual help; privacy and about are plain-language and on-brand. |
| **Total** | | **30/40** | **Good — solid foundation, fix the P1s before users see them.** |

## Design Specificity Verdict

**LLM assessment:** Sabeel has a recognizable mood — warm "Desert Dawn" paper background, deep teal, a peach selection, sand-toned streak chip, a gradient progress ring, and a voice that refuses to scold ("Goal reached. Enjoy the rest of your day.", "A short walk is a good beginning.", "A rest day never adds a day."). That is real authorship in color and copy.

The structure underneath is not authored: a stock Material 3 top app bar, a stock bottom navigation bar, stock Material icons, stock cards with one 20 dp radius everywhere, stock chips, stock switches, and system Roboto in both languages. Swap the strings for water or sleep tracking and every screen still works unchanged. The only custom-drawn object is the ring. The result reads as "a well-themed Material 3 steps app," not as a visual world that could only be Sabeel.

**Specificity verdict:** partial — the brand lives in palette and voice, not in structure, typography, iconography, or motion. Score equivalent: 2/5 (adjacent-category, identifiable palette, transplantable skeleton).

**Deterministic scan:** not applicable (native platform).

**Visual overlays:** none. Assessment B (detector, browser, overlay injection) does not apply to native Android; no browser or server was used, so no [Human] tab exists. The rendered review above is the substitute evidence.

## Overall Impression

Calm, honest trust is the product, and the design mostly earns it: the ring answers "how am I doing?" in one glance, the status and permission cards explain what the phone is doing rather than blaming the user, and the Diagnostics screen is better than most paid fitness apps. What lets it down is not taste but finishing: a headline metric that truncates its own value, an Insights screen that contradicts itself about time, a hero ring that visibly seams exactly when the user succeeds, a destructive action dressed as a primary CTA, and settings that grew into an uneven form instead of a system.

The single biggest opportunity: decide that the ring is the product's face and build one authored visual language around it — type, shape, and a completed-state celebration — instead of leaving a crafted object inside a stock frame.

## What's Working

- **The ring as a single-glance answer.** 230 dp hero, 16 dp stroke, a teal→mint sweep gradient, value and goal inside, a status sentence and "Updated 22:33" underneath. It is the one place where brand, data, and layout agree. It is also exposed to TalkBack as a single node instead of three readings.
- **A voice that does not nag.** "Goal reached. Enjoy the rest of your day.", "A short walk is a good beginning.", "A suggestion changes your goal by at most 10%, and only if you tap it." The copy states limits honestly — estimates are labeled estimates, rest days never invent steps — and EN/AR parity holds (242 strings + 3 plurals, kept equal by test).
- **Diagnostics as trust architecture.** "Why is my count low?" shows what the app can actually observe (permission, sensor, service state, stored row, last update), then the OEM battery settings that can throttle it, and never claims to recover lost steps. This is the design asset a competitor cannot copy with a color change.

## Priority Issues

### [P1] Home's three-metric row truncates the data it exists to show

**What:** `StatCard` renders its value with `maxLines = 1` and `TextOverflow.Ellipsis`; Home lays three cards across one row. At the default font scale the third value already reads **"84 min…"** instead of "84 minutes". At `font_scale = 1.3` (confirmed by capture), **two of three values** truncate — "336 k…" and "84 mi…" — while the labels wrap underneath. Arabic happens to fit ("84 دقيقة"), which shows this is width luck, not a designed limit.
**Why it matters:** Active minutes is one of the three headline metrics; a user cannot read it on a normal phone, and low-vision users lose two of three values exactly when they enlarge text. In a stats app, a number that ellipsizes is a broken product, not a polish issue.
**Fix:** Stop forcing three columns. Use a 2-up grid (Distance + Energy) and move Active minutes under the ring, or a vertical metric list; switch to 2-up at `fontScale > 1.15` (not `> 1.3`), allow the value to shrink via `autoSize` or 2 lines, inline the unit, and shorten labels ("Distance", "Energy", "Active"). Add a screenshot-free layout test with the longest localized value at 1.0/1.3/1.5 scale.
**Suggested command:** `$impeccable adapt`

### [P1] Insights contradicts itself about time — and prints "days days"

**What:** The screen computes one 30-day window (`lastDaysFlow(30)`, `windowDates(..., 30)`) but labels it three different ways: the card title says **"This week"**, the first stat says **"7-day average"** while being a 30-day average (4,966 = 149,000 ÷ 30), the sentence ends **"5 of 30 days days"** (the plural string already contains "days" and the format adds another), and **"Total distance"** is all-time (`totalDistanceMeters(rows)`), not 30-day.
**Why it matters:** This is the app whose brand promise is honesty about numbers. The visible typo undermines it in the same breath as the mismatched windows: users cannot answer "is this week or this month?" The goal-hit rate of 17% next to "This week" invites a wrong conclusion about their own behaviour.
**Fix:** One range label, one meaning. Pass the raw day count to the sentence (`…on %2$d of %3$d days`) and drop the duplicate unit, or use a plural resource; retitle the card to match the actual window ("Last 30 days" or split into an explicit 7-day block and 30-day block); either label Total distance as "All time" or window it. Add a copy test that fails on duplicated units and on "7-day" labels fed a 30-day window.
**Suggested command:** `$impeccable clarify`

### [P1] The hero ring visibly seams at the moment of success

**What:** At 100%+ the ring shows a notch at 12 o'clock and a lighter wedge around 2–4 o'clock — visible in light, dark, English and Arabic captures. Cause: `Brush.sweepGradient` starts at 0° (3 o'clock) while the arc starts at −90° (12 o'clock), so gradient start and end colors meet at the top; with `StrokeCap.Round` and a 360° sweep the two round caps collide at the seam.
**Why it matters:** The ring is the product's face, and goal completion is the emotional peak. The one frame users are most likely to look at, screenshot and share is the one that looks glitched.
**Fix:** Rotate the brush so its 0° aligns with the arc start, draw the completion arc to 359.6° (or into a dedicated completed state: full solid ring plus a check or a single soft pulse), and add golden captures at 0/25/50/99/100/105% in both themes.
**Suggested command:** `$impeccable polish`

### [P2] "Delete all data" wears the same uniform as "Start a walk"

**What:** The destructive entry point is a `SabeelPrimaryButton` — solid teal, full width, 52 dp, same shape and fill as the Home CTA and the "Allow" buttons — placed inside the "Your data" card under a divider, after Export and Import. Only the confirm dialog tints its button with `colorScheme.error`.
**Why it matters:** Visual language says "primary, safe, recommended" while the action is irreversible. Every other action on that card is reversible; this one is the most expensive tap in the app, and it is the most visually attractive control in the card.
**Fix:** Make the entry a destructive secondary — outlined or text in `error` with the trash icon, separated under a "Danger zone" label at the end of Settings — and state the consequence in the row ("Removes 20 days, 0 walks and all settings"). Keep the confirm dialog and its error-colored action.
**Suggested command:** `$impeccable harden`

### [P2] Settings grew into a long form with three competing grouping patterns

**What:** ~2.5 screens of content in one scroll. Some sections are bare controls (Language chips, Units chips), some are inline cards (Daily goal, Rest day, Daily reminder, Body data), and Privacy/About are two full-width outlined pills. "Daily reminder" appears as both a section header and the row title; "Streak" is a header over one switch; Language sits buried below body data; every section header is the same teal `titleSmall`, so the page reads as a wall of equal labels.
**Why it matters:** This is the Operate surface where scanability matters most. Users hunting "change my language" or "delete my data" must scroll past goal, streak, units, reminder and body data; nothing signals which rows are controls versus labels.
**Fix:** One row pattern for the whole screen — label left, control right, optional one-line description — grouped into cards ("Daily goal", "Tracking", "Your data", "App"), with the destructive action isolated. Move Language and Privacy/About into an "App" section near the top, keep one header style for true sections, and stop nesting buttons inside cards.
**Suggested command:** `$impeccable layout`

## Persona Red Flags

**Casey (distracted mobile user):** The primary CTA sits in the thumb zone and the walk checkpoint survives interruption — good. But the thing Casey needs when the count looks wrong ("Why is my count low?") is at the *bottom* of a second scroll on Home, below the ring, CTA and stats; on the go that reassurance is two swipes away. Home also has no pull-to-refresh or explicit "counting now" affordance near the number, so a stale-looking total has no in-place explanation.

**Jordan (first-timer):** The bottom nav is labeled, which saves them; the permission copy explains itself. But "Active minutes", "Goal hit rate", "7-day average" and "streak" are never defined inline; the "This week / 30 days" contradiction will read as a bug; onboarding has no skip and never explains the streak before it appears on Home. Jordan cannot tell whether 8,400 steps is good or merely above a target they guessed at.

**Sam (accessibility-dependent / low vision):** Strong fundamentals — 48 dp steppers, 52 dp buttons, single-node ring, chart descriptions, headings exposed, dark theme, RTL mirrored correctly. The failure is the one above: at 130% text (a routine setting, not an extreme) two of three Home metrics are ellipsized to "336 k…" and "84 mi…", so content is simply unavailable. Also, Delete all data is announced like any primary button; screen-reader users get no "destructive" cue.

## Minor Observations

- **Onboarding pages 1–2 are two-thirds empty** on a tall phone: content at the top, a lone pinned Next at the bottom, ~700 px of dead space between. The "Step 1 of 4" indicator is 12 sp and faint; there is no progress bar and no skip.
- **History repeats Home's chart.** The first thing under the History filter chips is the same "Last 7 days" chart Home already shows. Home could carry a sparkline/summary and History should own charting (7/30/90).
- **The 30-day History chart has no x-axis at all.** Bars render without date labels and empty days render as nothing rather than zero ticks, so the chart starts "mid-month" and cannot be mapped to dates; values are only readable in the list below.
- **Goal labels are not clickable context.** "Goal 8,000" top-right behaves like a label but is the one number users most often want to change; Settings is a screen away.
- **Walk idle duplicates its reassurance and its primaries.** "Without it Sabeel still counts steps and estimates distance" appears inside the Location card and again directly below it; "Start walk" (primary) sits under the card's "Allow" action, giving one screen two equally strong teal actions.
- **History labels mix relative and absolute dates.** "Today", "Yesterday", then "Sun 15 Nov" — fine, but the pattern stops abruptly and there is no weekday for older rows, which matters for a habit app.
- **Dark theme selection is muddy.** The selected bottom-nav pill uses the dark tertiary container (brown) with a mint icon, and the streak chip is olive — brand-warm, but the selected state reads weaker than the unselected teal-accented items. Light theme's peach pill is clearer.
- **No authored type or iconography.** System Roboto in EN and system Arabic font in AR; Material icons for Home/History/Star/Settings. "Insights" mapped to a star is semantically loose (star usually means favorite/achievement).
- **Arabic is well mirrored but numerals stay Latin** ("8,400 خطوة", "30 يومًا", "111.4 كم"), a deliberate-looking choice that sits between two conventions; pick one and state it (locale digits vs Latin digits) for consistency.
- **About uses a full-width outlined pill for a link** ("Read the privacy promise"), which is a lot of visual weight for a navigational aside; Privacy's five identical cards could be a scannable icon list.

## Questions to Consider

- If the ring is the product's face, why is it the only crafted object inside a stock Material 3 frame? What would "Desert Dawn" look like as a type and shape system, not only a palette?
- Is the three-up metric row worth three values that cannot all be read, or would two large metrics plus one quiet line under the ring feel calmer and more honest?
- Should Home and History both own a 7-day chart, or should Home own the single-glance ring and History own all charting with real axes?
- What is the completion moment supposed to feel like — a seam where a gradient ends, or an earned, deliberately designed state?
