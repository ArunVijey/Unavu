# Unavu: audit round 7 (independent)

Date: 2026-11-23 · Build: `app.unavu.review` (debug, versionName 1.2.1-review) · Device: 1084×2412, 420 dpi (override 375), 90/120 Hz
Screenshots: `C:\Users\labbh\AppData\Local\Temp\claude\3b4474cb3f5abe74\36febc13-0dc3-4022-aad5-4c3d5f7ba31a\scratchpad\round7\` (names below like `28c-food-detail-stepper.png`)

## What I covered

- **Onboarding:** step 1 (validation by clearing age, error copy), the step 1 to 2 transition, the step 2 targets count-up, then Start.
- **Today:** empty (top and scrolled), populated (top, scrolled, bottom), Yesterday, week strip, overflow menu, +Glass, the weight card, the meal quick-look sheet, the budget sheet and the nutrient sheet (Fibre). I deleted a meal and undid it.
- **Quick add:**
  - Food: `rendu idli oru vadai sambar`, then editing an item, the item sheet, and the add snackbar with Undo.
  - Liquid: `oru kaapi 1 glass water`, plus the water tiles.
  - Weight: a weigh-in today and one for yesterday.
  - Closing the sheet with typed text. Keyboard behaviour.
- **Plate builder:** tap and tap-again, hold to customise (curry oil level), search `dosa`, hold for Food detail, log the plate, and rotation (landscape and back).
- **Food detail:** top and bottom.
- **Meals:**
  - Combos: empty, build, name dialog (empty name), saved list, the log dialog.
  - Recipes: empty, the recipe editor (ingredient search `toor`, `toor dal`, `red gram`, grams dialog, totals), and backing out with unsaved edits.
  - The My food editor and saving.
- **Insights:** Week, Month and 3 months, empty and with 2 days of data, scrolled to the end.
- **Other screens:** History (empty day, logged day, open day), Liquids (yesterday), Weight (1 point, 2 points, goal dialog including the invalid value 5).
- **Settings and every sub-screen:** You/Your details, Goal & targets, My portions, My foods, Smart target, Backup, Reminders, About.
- **Appearance:** Light, Dark, Pure black, Sketch (Today, Insights, Your details, Settings), Ocean accent, the Custom picker with a pale custom accent in light theme, and the font cards.
- **System checks:**
  - Font scale 1.3.
  - Rotation.
  - Predictive-back gesture mid-swipe.
  - `dumpsys gfxinfo` while scrolling and switching tabs.
  - Cold start with `am start -W`.
- **Code read:** Shell/Nav, Today and its parts, FoodDetail, PlateBuilder, QuickAdd, Insights, Theme, Motion, Widgets, Store, the editors and the settings screens.

**System settings changed:** font_scale (1.0 to 1.3 to **1.0**), accelerometer_rotation/user_rotation (1/0 to 0/1 to **1/0**). I restored both and checked them afterwards. The animator scales (1.0) and night mode were not touched. The app's own appearance was reset to Clean, turmeric, AMOLED off; the theme is left on Light.

## Scores

| Category | Score | Evidence (details in the defect list) |
|---|---|---|
| UI | **7.0** | The visual language is strong and consistent (cards, pills, drawn icons, turmeric; Sketch is charming). But Food detail's portion stepper is visibly broken (#1), snackbars draw a double frame (#9), button casing is mixed ("LOG LUNCH", "⭐ SAVE COMBO" against sentence case elsewhere) (#16), and the plate list steppers and Insights bars don't line up (#13, #14). |
| UX | **7.0** | Logging flows are fast and smart (parser, remembered portions, undo everywhere). However: Meals tab/filter and Today scroll reset on every push/pop (#3); editors throw away work silently (#6); "water" can't be typed in Liquid mode (#7); "Add a food by calories" lives on My recipes but the food appears elsewhere (#18); weigh-ins change the target silently while Smart target says "Asks before changing" (#11); averages include the unfinished day (#19). |
| Animation / Motion | **8.0** | Tokenised durations and easings, reduced-motion respected everywhere (`Motion.kt:46-58`), fade-through tabs, push/pop slide, staggered Today, count-ups, a sliding week pill. Gaps: predictive back previews an empty page instead of the destination (#15); no shared motion between the sheet, the snackbar and the hero (#26). |
| Typography | **8.0** | One central type scale (`Type.kt`), tnum, Tamil fallback works, Excalifont tuned. Issues: an instruction rendered as a 2-line bold title (#17), key values truncated at 1.3× and in Sketch (#12), thousands separators mixed (#20), ALL-CAPS buttons (#16). |
| Layout & spacing | **7.0** | The Food detail card balloons and loses its "+" (#1). The gutter is 18 dp on Today/editors and 16 dp elsewhere (#21). Insights bar tracks end at different x (#14), the plate stepper column jitters (#13), and in landscape the plate footer covers about 40 % of the viewport (#23). |
| Color | **7.0** | A good palette with light/dark/black parity and accent ink derivation (`Theme.kt:76-96`). But the status bar icons are invisible with app Light on system Dark (#2); white text on `leaf` in dark is 2.75:1 on 3 screens (#5); the History badge colour contradicts the calendar (#10). |
| Accessibility | **7.0** | Semantics are good overall (merged rows, roles, chart descriptions, `selected` on days, headings). However: the "Increase" button has zero size on Food detail (#1); contrast fails (#2, #5); values truncate at 1.3× (#12); "751" kcal on meal rows is spoken without a unit (#22); the week "Next" arrow is enabled but does nothing (#24). |
| Performance | **8.5** | 1,521 frames with 0.13 % janky (new metric), p50 10 ms / p90 19 ms on a debug build; cold start 1.2–1.7 s in debug. Risk: `LocalData` is a `staticCompositionLocalOf` holding the whole `AppData`, so every write recomposes the whole tree (#27). No baseline profile is checked in (#27). |
| Code quality | **7.5** | Clean structure, tests for parser/store/contrast/scheduler, central tokens. But about 60 hard-coded UI strings sit in 15 files despite `strings_*.xml` (#25); a leading-space string bug (#8); `!!` and fully-qualified inline types (`androidx.compose.ui.graphics.Color.White`) in editors; duplicated rounding helpers produce different numbers (#4). |
| Robustness | **6.5** | Plate builder and Quick-add item overrides are lost on rotation and process death (#3); tab state is lost on push (#3); the goal-weight dialog accepts 5 and silently discards it (#28); rounding disagrees across screens (308 vs 309, 9.7 vs 10 g) (#4); editor data loss (#6); History status is inconsistent (#10). |
| Polish | **7.0** | "Unavu 0.3" in About against version 1.2.1; the backup file is still named `calorie-tracker-backup.json`; "Light· AMOLED"; "Trend 72.4 kg · last today"; "Breakf"; "Step 1 of 2" when editing details; the generic "Food" title; the double snackbar frame (#8, #9, #29, #30). |

**Average: 7.32 / 10. FAIL** (no category reaches 9).

---

## Ranked defects to fix

Each item gives where it is, what's wrong, the fix, and which categories it lifts.

### 1. Food detail portion stepper is broken: "+" gone, label one character per line
- **Where:** `ui/screens/FoodDetail.kt:240-258` (`PortionStepper`). Screenshots `28-food-detail.png` and `28c-food-detail-stepper.png`. The UI dump shows `Increase` with bounds `[0,0][0,0]` and the quantity text spanning 193→948 px.
- **What's wrong:** In the `AnimatedContent`, the inner `Text(... modifier = Modifier.fillMaxWidth())` (line 256) makes it take all remaining width. The "+" button is measured at zero width, and the label (`"2 pieces ≈ 130 g"`) wraps one glyph per line, so the macro card grows to about 400 dp tall. Users cannot increase a portion on Food detail, and TalkBack focuses a 0×0 node.
- **Fix:**
  - Give `AnimatedContent` a fixed slot: `modifier = Modifier.width(56.dp)` with `contentAlignment = Alignment.Center`.
  - Drop `fillMaxWidth()` from the inner Text.
  - Put the label in `Modifier.weight(1f).padding(start = 10.dp)` with `maxLines = 2`.
  - Better still, reuse the shared `Stepper` from `Customise.kt:127` so all three steppers (Customise, Food detail, Plate) are one component.
  - Add a screenshot/UI test that asserts "Increase" has a width of at least 48 dp.
- **Lifts:** UI, Layout, Accessibility, UX, Polish.

### 2. Status bar icons invisible in app Light theme while the system is Dark
- **Where:**
  - `ui/theme/Theme.kt:209-227` (`DisposableEffect(dark, navArgb)` plus an 800 ms re-apply)
  - `MainActivity.kt:84` (`enableEdgeToEdge()` with defaults, which follow the system)
  - `res/values-night/themes.xml` (`windowLightStatusBar=false`)
  - Screenshots `127-appearance-reset.png`, `128-light-sysdark.png`, `129-settings-light-sysdark-bar.png` (white clock and icons on #FBF8F2). Also in `126-predictive-back.png`.
- **What's wrong:** With Appearance set to Light and the phone in dark mode (night schedule), the bar style is overridden by the system-dark defaults (from the activity `enableEdgeToEdge()`, the splash exit and the XML night theme), and the delayed re-apply races them. The clock, battery and notifications become unreadable at about 1.1:1.
- **Fix:**
  - Stop relying on a one-shot `DisposableEffect` and a timer. In `AppTheme`, add `SideEffect { val w = (view.context as Activity).window; WindowCompat.getInsetsController(w, view).run { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark } }`, so it runs on every composition (including after recreation and the splash exit).
  - In `MainActivity.onCreate`, call `enableEdgeToEdge` with styles derived from the saved app theme, not the defaults.
  - Remove `windowLightStatusBar` from `values-night/themes.xml`, or set the splash `postSplashScreenTheme` to a neutral theme.
  - Verify all three app themes against both system modes.
- **Lifts:** Color, Accessibility, Polish.

### 3. Screen state is lost on push/pop, rotation and process death
- **Where:**
  - `ui/shell/Shell.kt:193-252`: `AnimatedContent` renders tab screens with no `SaveableStateHolder`, so every `rememberSaveable` inside a tab screen (and `LazyListState`) is dropped when a page is pushed.
  - `ui/screens/PlateBuilder.kt:154`: `items = remember { mutableStateListOf<NewItem>() }`.
  - `ui/screens/QuickAdd.kt:145-146`: `overrides` and `comboItems` use `remember`.
- **Seen:**
  - Meals → My recipes → Build a recipe → back: Meals is back on **Saved combos** (`80-recipe-back.png`). Saving a My food also returns to Saved combos (`83-myfoods-list`).
  - In the Plate builder with 4 items (751 kcal), rotating gives an **empty plate, 0 kcal** (`25-landscape-plate.png`, `26-plate-after-rotate`).
- **Fix:**
  - Wrap each destination in `val holder = rememberSaveableStateHolder()` and call `holder.SaveableStateProvider(key = d.page?.toString() ?: d.tab.name) { … }` inside the `AnimatedContent` content in `Shell.kt`.
  - Call `holder.removeState(key)` when a page is popped.
  - Hoist each tab's `LazyListState` with `rememberLazyListState()` inside that provider.
  - In PlateBuilder, use `rememberSaveable(saver = listSaver({ it.map(Json::encodeToString) }, { it.map(Json::decodeFromString<NewItem>).toMutableStateList() }))` (NewItem is already `@Serializable`, or make it so).
  - Do the same for QuickAdd `overrides`/`comboItems` (use a map saver keyed by item key).
- **Lifts:** Robustness, UX.

### 4. Rounding and number formatting disagree across screens
- **Where:**
  - `ui/screens/today/TodayParts.kt:221` (`(t.kcal * split).toInt()`) against `ui/screens/today/TodaySheets.kt:123` and `:191` (`roundToInt()`).
  - Fat in `TodaySheets.kt` (meal sheet) uses `roundToInt` while the hero chip uses `fmtNum` (`Today.kt:152`).
  - `FoodDetail.kt:208` donut description `(p/4).roundToInt()` against the visible "8.4 g".
- **Seen:**
  - Next-up says "Snacks: 0 of ~**308** kcal" while the meal sheet says "share **309**" and Goals says **310** (after the target moved). These are the same quantity.
  - Fat shows **9.7 g** in the hero and **10 g** in the meal sheet for the same entries.
  - TalkBack says "protein 8 g" where the screen shows 8.4 g.
- **Fix:**
  - Add one helper in `data/Calc.kt`: `fun mealShare(t: Targets, split: List<Double>, m: Meal): Int = (t.kcal * split[m.ordinal]).roundToInt()`. Use it in TodayParts, TodaySheets, Goals and Quick-look.
  - Use `fmtNum` for every gram/mg value on screen and in content descriptions (meal sheet macros, Food detail donut description, plate macro tiles).
  - Add a unit test that sums per-meal shares and checks they are consistent.
- **Lifts:** Robustness, Polish, Code quality.

### 5. White text on `leaf` fails contrast in dark/black themes
- **Where:**
  - `ui/screens/SmartTargetScreen.kt:86-91`. `ON_LEAF_LIGHT` is chosen because dark `leaf #4FAE76` has luminance < 0.4.
  - `ui/screens/meals/RecipeEditor.kt:167-171` and `ui/screens/meals/MyFoodEditor.kt:164-168` (hard-coded `Color.White`).
  - Screenshots `90-smart.png`, `79-recipe-with-ing.png`, `82-myfood-bottom.png`.
- **What's wrong:** White on #4FAE76 is **2.75:1**. The captions and the "P · C · F" line need 4.5:1; even the display number needs 3:1.
- **Fix:**
  - Add tokens `heroBg`/`onHero` to `AppColors`: light `heroBg = leaf (#2F7D4F)` with `onHero = White` (5.0:1); dark `heroBg = #1C3326` (leafS) or `#2F7D4F` with `onHero = #F2EEE6`.
  - Use them in all three cards instead of `c.leaf` plus white.
  - Extend `ContrastTest.kt` to assert onHero/heroBg ≥ 4.5 in all three palettes.
- **Lifts:** Color, Accessibility.

### 6. Editors and quick add silently discard work on back or close
- **Where:**
  - `ui/screens/meals/RecipeEditor.kt:109` (`TopBar(... onBack = { nav.pop() })`) and system back.
  - `ui/screens/meals/MyFoodEditor.kt:109`.
  - PlateBuilder back.
  - `QuickAdd.kt` close ✕ and scrim tap.
  - Seen: I named a recipe, added 150 g red gram and pressed back; it was gone with no prompt (`80-recipe-back`). The typed quick-add text was dropped on ✕ (`41-qa-close`).
- **Fix:**
  - Track `dirty` (anything differs from the initial state).
  - On back or close when dirty, show an `AlertDialog` "Discard this recipe?" with Keep editing / Discard, using `BackHandler(enabled = dirty)`. With PredictiveBack, cancel the gesture and show the dialog.
  - For quick add, keep a draft in `Nav` (`quickAddDraft`) for 10 min, so reopening restores the text. Alternatively, show an "Undo close" snackbar.
- **Lifts:** UX, Robustness.

### 7. Liquid quick add can't parse "water"
- **Where:**
  - `data/Parser.kt:62-105` (parses foods only).
  - `ui/screens/QuickAdd.kt:207-232` (unknown-word card), `:266-276` (water tiles).
  - Screenshot `40-qa-liquid-water.png`: "oru kaapi 1 glass water" gives "Didn't find "water"" with **＋ Add "water" to My foods**.
- **Fix:**
  - In `Parser.parse`, recognise `water | thanni | tanni | தண்ணீர் | neer` (plus an optional quantity and unit such as glass/tumbler/bottle/ml/l/litre) and return `Result.waterMl`.
  - In QuickAdd, add that amount to `waterMl` and show a "💧 Water 250 ml → Liquids" row in the parsed card.
  - Never offer "Add water to My foods".
  - Add ParserTest cases.
- **Lifts:** UX, Robustness.

### 8. "Light· AMOLED" in Settings and AMOLED shown in light theme
- **Where:** `res/values/strings_i.xml:33` (`<string name="i_amoled_short"> · AMOLED</string>`) and `ui/screens/settings/SettingsScreen.kt:94`. Screenshot `125-settings-fs130.png`.
- **What's wrong:** Android trims leading whitespace in string resources, so the text renders as "Light· AMOLED". It also says AMOLED while the theme is Light, where AMOLED has no effect.
- **Fix:** Change the resource to `\u0020· AMOLED`, or better, use a format string `i_appearance_summary = "%1$s · %2$s"` and build a list joined with " · ". Only add AMOLED when the effective theme is dark (DARK, or SYSTEM while the system is dark).
- **Lifts:** Polish, Code quality.

### 9. Snackbar draws a double frame
- **Where:** `ui/shell/Shell.kt:268-272`. Screenshots `63-history-after.png` and `14c-snackbar-crop.png`.
- **What's wrong:** The M3 `Snackbar(snackbarData, modifier)` overload internally applies `padding(12.dp)` to the modifier, so the `.border(1.dp, c.line, shape)` ends up 12 dp outside the filled pill. The result is an outer outline box around an inner pill.
- **Fix:** Use the slot overload: `Snackbar(modifier = Modifier.padding(12.dp).border(1.dp, c.line, shape), shape = shape, containerColor = c.sf, contentColor = c.ink, action = data.visuals.actionLabel?.let { { TextButton(onClick = data::performAction) { Text(it, color = c.accInk) } } }) { Text(data.visuals.message) }`. Alternatively, drop the border and use `tonalElevation`/shadow only.
- **Lifts:** UI, Polish.

### 10. History: badge colour contradicts the calendar classification
- **Where:** `ui/screens/tracking/History.kt:132` (calendar: `< 0.85 × target → UNDER`) against `:175` (badge: only `> 1.05` is OVER, otherwise OK/green). Screenshots `63-history-after.png` and `126-predictive-back.png`.
- **Seen:** 22 Nov shows an orange "Under" dot while the selected-day badge "379 / 2064" is green. Today (83 %) is also judged and coloured "Under" while the day is still in progress.
- **Fix:**
  - Use one `dayState(kcal, target)` function (UNDER/ON/OVER) for both the dot and the badge, with `Status.LOW` for UNDER.
  - For today, skip the classification (grey ring "in progress") until 22:00 or until dinner is logged.
- **Lifts:** Color, Robustness, UX.

### 11. Weigh-ins change the daily target silently, contradicting "Smart target: Asks before changing"
- **Where:** quick add weight → profile weight sync (Store/AppViewModel); Settings "Smart target · Asks before changing".
- **Seen:** The target moved from 2058 to 2064 after logging 72.4 kg, with no card, toast or explanation (`06-today-empty` against `62-today-full`). The calendar and Insights then use the new target retroactively.
- **Fix:** Pick one of two behaviours:
  - (a) Don't write the weigh-in into `profile.weightKg` automatically. Instead, raise the same "New target" card used by Smart target when the BMR-derived target changes by more than 25 kcal.
  - (b) Show a snackbar "Target updated to 2064 kcal (weight 72.4 kg) · Undo".

  Either way, history days must keep the target that applied on that date. Store `targetKcal` per day (or a target history) so past days don't change colour.
- **Lifts:** UX, Robustness.

### 12. Key values truncate at font scale 1.3 and in Sketch
- **Where:** the hero nutrient chips in `ui/screens/today/BudgetHero.kt` (3 fixed columns, `maxLines = 1` with ellipsis). Screenshots `122-today-fs130.png` ("3,496/2,00…") and `110-today-sketch.png` ("3,496/2,000 …"). Also `TodaySheets.kt:98` `label.take(6)` gives "Breakf" (`101-meallook-light.png`).
- **Fix:**
  - In the chip, put the value on its own line using a two-step size (`style = if (text.length > 9) U.t.bodyS else U.t.label`). Alternatively, use the `TextAutoSize`/`BasicText(autoSize = TextAutoSize.StepBased(minFontSize = 11.sp))` API, which exists in the 2026 BOM.
  - Drop the "/target" part to a second caption line when the scale is above 1.15.
  - Replace `label.take(6)` with `maxLines = 1, overflow = Ellipsis` and `Modifier.weight(1f)`, or use short labels from resources ("Bkfst").
- **Lifts:** Typography, Accessibility, Layout.

### 13. Plate list steppers: the column jitters and the number is ambiguous
- **Where:** `ui/screens/PlateBuilder.kt:246-248`. Screenshot `34-plate-scrolled.png`: "− 282 +" against "− 55 +". The "−" moves 20 px depending on digit count, and the number between − and + is **kcal**, not the quantity being stepped.
- **Fix:**
  - Give the middle Text `Modifier.widthIn(min = 56.dp)` with `textAlign = Center`, and add "kcal" as a caption under it. Alternatively, show the quantity ("1.5 cups") between the buttons and kcal on the right.
  - Better: use the same `Stepper` component as #1.
- **Lifts:** Layout, UX, UI.

### 14. Insights "Average per day vs target" tracks end at different x
- **Where:** `ui/screens/Insights.kt:172-176`. Screenshots `68-insights-month.png` and `113-insights-sketch.png`.
- **What's wrong:** The value text has no fixed width, so the bar for "2,137/2,000" is shorter than "0.3/25". In Sketch, "Saturated fat" collides with its bar.
- **Fix:**
  - Use the value `Text(..., textAlign = TextAlign.End, modifier = Modifier.width(96.dp))`.
  - Use the label `Modifier.width(112.dp)` with `maxLines = 1` and ellipsis. In Sketch, put the label above the bar on narrow widths, or measure the longest label once with `rememberTextMeasurer`.
- **Lifts:** Layout, UI.

### 15. Predictive back previews an empty page instead of the destination
- **Where:** `ui/shell/Shell.kt:159-191`. Screenshot `126-predictive-back.png`: the page scales down to 0.9 and shifts right over a flat `bg`, and nothing of the previous screen is visible.
- **Fix:**
  - During the gesture, render the previous destination underneath. Keep `prev = nav.stack.getOrNull(lastIndex-1)` composed in a `Box` under the current one while `backProgress > 0`, translated −30 %→0 with `progress`.
  - Alternatively, use `SeekableTransitionState` with `AnimatedContent` and `seekTo(progress)` (Compose 1.7+), which gives the true pop transition scrubbed by the gesture.
  - Round the corners of the shrinking page (`clip(RoundedCornerShape(lerp(0,28.dp,bp)))`) per the M3 predictive-back spec.
- **Lifts:** Animation, Polish.

### 16. Button casing is inconsistent
- **Where:** `ui/screens/PlateBuilder.kt:273` ("⭐ SAVE COMBO") and `:277` ("PICK A MEAL" / "LOG ${label.uppercase()}"). Every other primary button is sentence case ("Save recipe", "Add 3 items · 296 kcal", "Log").
- **Fix:** Use "Log lunch", "Pick a meal" and "Save combo", moved into string resources.
- **Lifts:** UI, Typography, Polish.

### 17. Instructions styled as section titles
- **Where:** `ui/screens/PlateBuilder.kt:192` and `:217` (`SectionTitle("Tap to add · tap again for more · hold to customise")`). Screenshots `22-plate.png` and `71-new-combo.png`: a 2-line bold 20 sp heading for hint text, also announced as a heading.
- **Fix:** Add a "Popular" or "Quick picks" `SectionTitle`, and render the instruction as `Text(..., style = U.t.caption, color = c.ink2)` under it, without heading semantics.
- **Lifts:** Typography, UI, Accessibility.

### 18. "Add a food by its calories" entry point is on My recipes, but the food appears elsewhere
- **Where:** `ui/screens/Meals.kt:160` (button on the My recipes tab); saved foods appear only in Settings › My foods (`MyFoods.kt`). Screenshots `83-myfoods-list` and `84-myrecipes-list` (after saving Shawarma roll, My recipes still says "No recipes yet").
- **Fix:** Rename the Meals tab to "My foods" and list both custom foods and recipes there (sections "Foods" / "Recipes", each with its add button). Keep Settings › My foods as a link to that tab, or remove it. After saving, return to that tab with the new item highlighted (`animateItem` plus a brief accS flash).
- **Lifts:** UX.

### 19. Averages count the unfinished day, and meal timing uses log time for back-filled days
- **Where:**
  - `ui/screens/Insights.kt` (calc for the range).
  - `MealTiming` (`Insights.kt:180`).
  - Screenshots `64/68`: "AVG / DAY 1,047 · 1017 under target" from 379 (yesterday, one meal) plus 1715 (today, in progress).
  - `67-insights-week-4.png`: yesterday's breakfast is plotted at 4:4x PM because it was logged then ("Eating window 4:33 PM – 4:41 PM").
- **Fix:**
  - Exclude today from averages until 22:00, or label it "(today so far excluded)".
  - Optionally exclude days with fewer than 2 meals, mirroring Smart target's "2+ meals" rule, and say so in the caption.
  - In meal timing, ignore entries whose `loggedAt` date ≠ entry date, or use a per-meal default time for them.
- **Lifts:** UX, Robustness.

### 20. Thousands separators are mixed
- **Where:** `ui/screens/today/BudgetHero.kt` ("1715 eaten of 2064", "349"), `Onboarding.kt:174`, `GoalsScreen` "2064 kcal" and `SmartTargetScreen` "2064 kcal", against `fmtNum` ("1,047", "3,496/2,000", "2,137/2,000") in Insights and the chips.
- **Fix:** One formatter for every integer shown to users: `fmtInt(v) = NumberFormat.getIntegerInstance(Locale("en","IN"))`. Use it everywhere, including `stringResource` args (pass Strings, not Ints). Note the en-IN grouping gives "1,00,000" for large values.
- **Lifts:** Typography, Polish.

### 21. Inconsistent side gutters (18 dp vs 16 dp)
- **Where:** `ui/screens/Today.kt:264` (18 dp), `meals/RecipeEditor.kt:110` and `meals/MyFoodEditor.kt:110` (18 dp), and 16 dp in Insights, Meals, Settings, Onboarding, FoodDetail and PlateBuilder. Card edges sit at x = 42 px on Today and 38 px elsewhere, so the content "jumps" on tab switch.
- **Fix:** Add `object Space { val gutter = 16.dp; val cardGap = 14.dp; ... }` in the theme and use `Space.gutter` everywhere.
- **Lifts:** Layout, Code quality.

### 22. Meal row values spoken without units; generic labels
- **Where:**
  - `ui/screens/Today.kt:334-358`: the merged row reads "Lunch, Rice…, 751" with no unit.
  - Week arrows read "Previous"/"Next" (`TodayParts.kt` ArrowButton).
  - Food detail StepBtn descriptions "Decrease"/"Increase" are hard-coded English with no food name (`FoodDetail.kt:250, 257`).
- **Fix:**
  - Set `contentDescription = "$label, $kcal kilocalories, ${items}"` or "not logged, add" on the meal row.
  - Make the arrows "Previous week" / "Next week".
  - Use `R.string.f_decrease/f_increase` with the food name, as PlateBuilder does.
- **Lifts:** Accessibility.

### 23. Landscape is barely usable on Plate builder and quick add
- **Where:** `25-landscape-plate.png`: the sticky footer (total plus two 56 dp buttons) takes about 40 % of the height, only one row of 3 huge tiles fits, and the empty plate's "Save" looks enabled.
- **Fix:**
  - Use `LazyVerticalGrid(GridCells.Adaptive(110.dp))` for tiles.
  - When `maxHeight < 480.dp`, collapse the footer into a single 56 dp row (total on the left, "Log lunch" on the right) and hide "Save".
  - Disable "⭐ Save" when `items.isEmpty()` in all orientations (`PlateBuilder.kt:275`).
- **Lifts:** Layout, UX.

### 24. Week strip "Next" arrow is enabled on the current week but does nothing
- **Where:** `ui/screens/today/TodayParts.kt:160`. The `if (...)` guard is inside the click handler.
- **Fix:** Pass `enabled = sel.plusWeeks(1) <= today.with(SUNDAY)` to ArrowButton, render it at 38 % alpha and expose `disabled()` semantics.
- **Lifts:** Accessibility, UX, Polish.

### 25. Hard-coded UI strings
- **Where:** about 60 occurrences in 15 files (count of `Text/TopBar/PrimaryButton/SectionTitle/TextField/NumberField/SearchField("literal")`):
  - RecipeEditor 15, MyFoodEditor 10, PlateBuilder 10, FoodDetail 9, TodaySheets 5, Customise 5, and others.
  - Examples: `FoodDetail.kt:116` "Food", `:186-192` "Add to" / "Pick a meal above" / "＋ Add to ${meal!!.label}"; `Today.kt:157-159` "Today"/"Yesterday"; `Models.kt:7-11` Meal labels; `Nav.kt:41` Tab labels.
- **Fix:** Move them to `strings_f.xml`/`strings_g.xml`, use `stringResource`/`pluralStringResource`, and replace `meal!!` with smart-cast locals. Add a lint check (`HardcodedText` for Compose via a detekt/ktlint custom rule, or a simple grep test in CI).
- **Lifts:** Code quality, Polish.

### 26. Motion continuity gaps
- **Where:** quick-add sheet close, then the snackbar appears and the hero number updates. The hero kcal count does not animate between values on Today (only the donut and plate do), and the "Added …" snackbar slides over the FAB area.
- **Fix:**
  - Animate the hero "left" number with `animateIntAsState(left, motionTween(Motion.LONG))`.
  - Animate the segmented meal bar widths with `animateFloatAsState`.
  - Pulse the affected meal row once (accS background fade 0→0.6→0 over MEDIUM) when entries are added via undoable/feedback.
- **Lifts:** Animation, UX.

### 27. Recomposition scope and the startup profile
- **Where:** `ui/Nav.kt:82` (`LocalData = staticCompositionLocalOf { AppData() }`, provided from MainActivity). Any write (a glass of water) invalidates every reader and the full subtree under the provider. `app/src/main/baseline-prof.txt` does not exist, even though `profileinstaller` is a dependency.
- **Fix:**
  - Switch to `compositionLocalOf`, or better, pass narrow `State`/derived values (`remember { derivedStateOf { data.entriesOn(date) } }`) to screens.
  - Mark `AppData` and its lists `@Immutable` with `kotlinx.collections.immutable`.
  - Generate a Baseline Profile with a `:baselineprofile` module (Macrobenchmark: onboarding → Today → quick add → plate) and check it in.
  - Measure release cold start (target < 600 ms TTID).
- **Lifts:** Performance, Code quality.

### 28. Goal-weight dialog accepts invalid input and silently ignores it
- **Where:** `ui/screens/tracking/Weight.kt:152-155`. `InputDialog` enables Save for any non-blank text, and the `takeIf { it in 20.0..300.0 }` throws away 5 with no message (`49-goal-5.png`). The same pattern applies to the Liquids goal dialog (`Liquids.kt:211`) and grams dialogs.
- **Fix:** Add `validate: (String) -> String?` to `InputDialog` (`Widgets2.kt:192`). Show the error under the field (`isError`, `supportingText`) and disable Save while it is invalid. Use ranges 20–300 kg, 0.5–6 L and 1–5000 g.
- **Lifts:** Robustness, UX.

### 29. Copy, version and naming polish
- **About:** "Unavu 0.3" (`strings_i.xml:166`) against versionName 1.2.1. Use `BuildConfig.VERSION_NAME` (enable `buildConfig = true`) or `packageManager.getPackageInfo`.
- **Backup file name:** `calorie-tracker-backup.json` (`data/Export.kt:14`). Rename to `unavu-backup.json`. Still accept the old name on restore, and migrate by writing the new name next time.
- **Weight card:** "Trend 72.4 kg · last today" (`strings_d.xml:79` with lowercase `dayTitle`). Change to "Trend 73.0 kg · weighed today/yesterday/on 21 Nov".
- **Weight number consistency:** Today shows "Weight 72.4 kg" (last weigh-in) while Weight and Insights show 73.0 kg (trend) as the headline. Make the Today title use the trend as well ("Weight 73.0 kg · last 72.4 today").
- **Your details (from Settings):** shows "Step 1 of 2" and a step bar (`Onboarding.kt:98-99`, `114-state.png`). Hide StepBar and the step text when `onBack != null` (the edit mode).
- **Food detail:** the TopBar title is "Food" (`FoodDetail.kt:116`). Use the food name and drop the duplicate heading, or use "Food details".
- **Liquids (yesterday):** the heading says "TODAY" (`Liquids.kt:124`, `58-liquids.png`). Use `dayTitle(date).uppercase()`.
- **Lifts:** Polish, UX.

### 30. Onboarding pre-fills body values that look like real input
- **Where:** `ui/screens/Onboarding.kt:85-90`. Age 28, height 172 and weight 72 are filled in as real values (`02-onb1`), so a user who taps "Calculate my plan" right away gets targets for someone else.
- **Fix:** Start with empty fields and show the defaults as placeholders ("e.g. 28"). Keep Calculate disabled until all three are entered, and focus Age on entry.
- **Lifts:** UX, Robustness.

### 31. The widget ignores app theme and accent, and has no picker preview
- **Where:** `widget/Widgets.kt:60-66` (hard-coded dark BG/INK/LEAF) and `res/xml/widget_*.xml` (no `previewLayout`/`previewImage`).
- **Fix:** Use Glance `ColorProvider(day = …, night = …)` from Light/Dark tokens plus the accent for the bar. Add `android:previewLayout="@layout/widget_preview_small"` (and a `previewImage` for API < 31).
- **Lifts:** Color, Polish.

---

## Target after fixes (my estimate)

- **Fixing #1 to #12** raises:
  - UI, Layout, Color, Accessibility and Robustness to about 8.5.
  - UX to about 8.5.
- **Fixing #13 to #31 as well** takes every category to at least 9. Motion needs #15 and #26 specifically, and Performance needs #27 plus a measured release build.
