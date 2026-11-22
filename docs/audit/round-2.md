# Unavu audit, round 2 (independent)

Method: ran the release build on the Pixel 9 Pro XL emulator (Today scrolled, quick-add of "rendu idli oru vadai" into Lunch, Insights, Meals, Settings, Appearance, Backup, Reminders, dark mode), then read Motion.kt, Shell.kt, BottomBar.kt, QuickAdd.kt, Theme.kt and grepped the rest. Screenshots: scratchpad `audit/` (today1-3, qa1-4, ins1, meals1, set1-2, app1-2, rem1, d1-2, l).

## Scores

| # | Category | Score | Evidence |
|---|---|---|---|
| 1 | UI | 7 | Clear card hierarchy, bold hero number (today1). But: two identical yellow + buttons stacked on Meals (meals1); Snackbar draws a ghost double border (qa4); Reminders "Allow notifications" CTA is grey inside a red card (rem1); Insights shows stats while its empty state says no data (ins1). |
| 2 | UX | 7 | Quick-add parse is excellent (qa2). But the meal is not preselected, so the primary button is disabled ("Pick a meal above") until an extra tap (qa2, QuickAdd.kt:110). NextUp banner still says "Breakfast" at 14:58 after Lunch was logged (qa4, d2). Close waits 550 ms (QuickAdd.kt:133). Redundant Settings row "Saved combos & my recipes" (set1). |
| 3 | Animation | 7 | Tokens 150/250/300 ms with M3 easings (Motion.kt:28-35). Tab fade-through (Shell.kt:154), shared-axis push (Shell.kt:159), pill indicator and FAB rotate (BottomBar.kt:89-119); all honour reduced motion. Gaps: bottom bar vanishes instantly on push/pop (Shell.kt:202-203); predictive back has no in-app progress (Shell.kt:139-142); sheet runs its own spring on top of the M3 slide (QuickAdd.kt:134-137); several raw springs/tweens bypass tokens; Standard and Emphasized are the same curve (Motion.kt:31-32). |
| 4 | Color | 8 | Warm palette with matching dark and AMOLED sets (Theme.kt:95-120). Contrast estimates: ink2 #6B675E on bg #FBF8F2 about 5.3:1; turInk on turS about 5:1; dark ink2 on bg about 6.6:1. Weak: disabled yellow button label about 4:1 (qa2); hint colour equals ink2 so the placeholder looks like input (qa1); the bold green info card reads as success (rem1). |
| 5 | Typography | 8 | Consistent scale, tabular numerals, Tamil falls back to Noto Sans Tamil (app2). Minor: placeholder same size and colour as input (qa1); dense caption at the bottom of the sheet (qa2). |
| 6 | Accessibility | 7 | Role.Tab with 64dp targets (BottomBar.kt:82), FAB description, reduced motion handled. Issues: status bar icons white on cream in light mode (today1, ins1, set1; Theme.kt:213); wrong haptic type (BottomBar.kt:83); faint meal dots (today3); low-contrast disabled CTA. |
| 7 | Performance | 8 | Not a priority. animateItem lists, no visible jank. paperDots behind every screen and Today stagger plus per-item dayFade are heavy but fine. |
| 8 | Code quality | 7 | Good tokens and screen split. Inline fully-qualified animation calls (AccentPicker.kt:106, PlateBuilder.kt:324, Liquids.kt:225, QuickAdd.kt:137, Widgets2.kt:245); duplicate easing constants (Motion.kt:31-32); large files (Today.kt 528, PlateBuilder.kt 388). |
| 9 | Robustness | 7 | Load-issue dialog and backup safety are good. Weak: `done` + delay(550) then onDismiss can fire after a manual dismiss (QuickAdd.kt:132-133); Insights "1832 under target" from one day is misleading (ins1). |
| 10 | Polish | 6 | Rough edges in the first minute: unreadable status bar, ghost Snackbar border, double + buttons, stale banner, disabled-by-default primary action. |

**Average: 7.2. Result: FAIL** (pass needs every category 9 or above and average 9 or above).

## Defects (ranked)

1. **Status bar icons are white on cream in light mode** (today1, ins1, meals1, set1, app1, rem1: clock and battery nearly invisible). Correct only after a theme toggle (l.png) or under a scrim. Theme.kt:211-215 sets `isAppearanceLightStatusBars = !dark` in a `SideEffect`, but `enableEdgeToEdge()` (MainActivity.kt:82) installs the default auto style which wins on cold start. Fix: call `enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { dark }, navigationBarStyle = ...)` with `dark` from the app setting, re-applied in a `DisposableEffect(dark)` inside AppTheme; drop the SideEffect. Verify on a cold start in light mode.
2. **Quick-add has no preselected meal** (qa2; QuickAdd.kt:110 uses only `request.meal?.name`). Primary button reads "Pick a meal above" and is disabled. Fix: when `request.meal == null`, default `mealName` from the clock (breakfast before 11:00, lunch before 15:30, snacks before 18:30, else dinner) so the button reads "Add 2 items · 241 kcal" immediately.
3. **Snackbar draws a ghost second rounded border** (qa4, y about 1660-1800). Shell.kt:196-199 puts `Modifier.border(...)` on the outer Snackbar, whose own container is the inner shape, plus 12dp host padding. Fix: remove the modifier border and wrap in `Box(Modifier.clip(shape).border(1.dp, c.line, shape))`, or use the Snackbar `shape` and a shadow only.
4. **Meals shows two identical yellow + buttons** (meals1: local FAB y about 1730 above the global one). Same glyph, different meaning. Fix: delete the local FAB (the empty state already has "Build a combo"); when combos exist use an extended FAB "New combo" in a secondary style.
5. **NextUp banner is stale**: "Breakfast: 1832 kcal left" at 14:58 after Lunch was logged (qa4, d2; TodayParts.kt about :204). Fix: next meal = first unlogged meal whose window has not passed for the current clock, falling back to the next unlogged in order; hide after dinner.
6. **Insights shows stats while the empty state says "Not enough data"** (ins1: AVG/DAY 241, "1832 under target" from one day). Fix: show "–" and "log 2+ days" until at least 2 days have entries.
7. **Bottom bar is removed instantly on push/pop** (Shell.kt:202-203) while the page slides. Fix: `AnimatedVisibility(page == null, enter = slideInVertically { it } + fadeIn(tween(Motion.MEDIUM)), exit = slideOutVertically { it } + fadeOut(tween(Motion.SHORT)))`.
8. **Predictive back gives no in-app feedback** (Shell.kt:139-142, `progress.collect { }` is empty). Fix: drive `graphicsLayer { scaleX = scaleY = 1f - 0.1f * p; translationX = p * 24.dp.toPx() }` on the page container from `progress`, reset on cancel.
9. **"Allow notifications" is a grey tonal button inside a red alert** (rem1). Fix: use the filled accent PrimaryButton (acc/onAcc) for this primary action.
10. **Sheet double-animates and delays close**: custom enter spring (QuickAdd.kt:134-137) sits on top of the M3 slide, and `closeAfter` waits 550 ms (QuickAdd.kt:132-133). Fix: remove the extra translationY/alpha spring; after the success tick call `sheet.hide()` then `onDismiss()` within about 250 ms.
11. **Disabled CTA contrast** (qa1, qa2): dark-grey label on pale yellow is about 4:1. Fix: disabled container `c.sf2`, label `c.ink2`, accent only when enabled.
12. **Placeholder looks like typed input** (qa1): same headline style, `c.hint` equals `ink2`. Fix: lighter alpha or smaller style for the example, or move the example under the field as a label.
13. **Wrong haptic on tab select** (BottomBar.kt:83 uses `TextHandleMove`). Fix: `HapticFeedbackType.SegmentTick` (or `ContextClick`).
14. **Motion tokens**: Motion.kt:31-32 Standard and Emphasized are identical; raw springs/tweens at AccentPicker.kt:106, PlateBuilder.kt:324, Liquids.kt:225, QuickAdd.kt:137, Widgets2.kt:245 bypass reduced-motion helpers. Fix: add `motionSpring()` beside `motionTween()` and use both everywhere; give Emphasized its own curve (0.2, 0, 0, 1 vs Standard 0.4, 0, 0.2, 1).
15. **Hard clip under the pinned Today header** (today2, y about 235): cards are cut with no fade. Fix: 12dp gradient from `c.bg` to transparent just below the header.
16. **Inconsistent headers**: Today pins its title, Insights, Meals, Settings and sub-pages scroll it away and content slides under the status bar with no scrim (set2, app2). Fix: one shared scaffold; add a status-bar-height `c.bg` scrim.
17. **Redundant Settings row "Saved combos & my recipes: In the Meals tab"** (set1). Fix: remove it or make it navigate to the Meals tab.
18. **"Cheat meal" chip wraps to a second row and is styled differently** (qa3). Fix: same chip style in a FlowRow with a small separator label, or a toggle below.
19. **Quick-add total mismatch**: lines 102 + 140 but total 241 (qa2). Fix: total the displayed rounded lines, or show consistent rounding.
20. **Faint meal dots in light mode** (today3: Breakfast/Snacks/Dinner pastel dots under 3:1 on white). Fix: darker dot tokens or a 2dp ring.
21. **Reminders info card is bold full-width green text** (rem1), reads as success. Fix: `c.sf2` card, regular-weight `ink2` text, small leaf icon.
22. **Accent grid has an empty cell** (app1: 5 + 4 swatches). Fix: add a 10th swatch or use 3 rows of 3.
23. **Tab indicator animates scaleX only** (BottomBar.kt:91) so the pill looks stretched while growing. Fix: animate `scaleX` and `scaleY` from 0.6 to 1.
24. **Current-day marker only shown in dark mode** (d1 has a dot under the selected day; today1 does not). Fix: show the dot in both themes.
25. **Backup top row "Pick a folder to start backing up" looks like a button but is status** (Backup screenshot). Fix: render as a banner with a "Choose" action and give "Back up now" a visible disabled reason.
