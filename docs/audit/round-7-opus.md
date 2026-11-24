# Unavu v1.2.1: Audit round 7 (Opus, independent)

Tested on emulator-5554 in light and dark mode, at 04:00 to 04:02 local time. Screens covered: Today (top and scrolled), Build a plate (push transition captured about 250 ms in), Quick add ("rendu idli oru vadai", then add), Insights, Meals, Settings > Appearance (light and dark), Today in dark mode.

| Category | Score | Evidence |
|---|---|---|
| UI | 8 | Clean, consistent card system. Three different "selected" styles (black day chip, yellow filled Food tab, yellow outlined segments). Sodium tile is truncated. |
| UX | 7 | At 4 am the Next-up card says "Dinner" while Quick add picks Breakfast. Duplicate items show as "2 Idli · 2 Idli". The Undo snackbar is dismissed on any tab switch. |
| Animation | 8 | Solid token system (Motion.kt:29-41). Reduced motion snaps everywhere. Push and pop use z-order, plus predictive back (Shell.kt:160-216). Gaps listed below. |
| Color | 8 | The warm palette works in both themes. In dark mode the empty-meal dot is nearly invisible and the selected segment looks muddy olive. |
| Typography | 8 | Good hierarchy. The Build-a-plate hint is set as a two-line bold title. "LOG DINNER" is all caps while everything else is sentence case. |
| Accessibility | 8 | Touch targets of 48dp or more, Role.Tab, focus ring on the FAB. Meal kcal numbers have no unit. The faded dot shows state by colour alone. |
| Performance | 8 | Not a priority. No jank observed. The bar is translated, not laid out again (Shell.kt:254-260). |
| Code quality | 8 | Meal time windows are duplicated in two places with different rules. A remembered array is written during composition. |
| Robustness | 7 | Quick-add line items don't sum to the total (102 + 140 ≠ 241). The suggestion logic ignores the early-morning part of "today". |
| Polish | 7 | Idli shows as 🍘 (rice cracker) and Medu vada as 🍩 (doughnut). Three flatbreads share 🫓. The sodium unit is cut off by an ellipsis. |

**Average: 7.7. FAIL** (no category reaches 9).

## Defects blocking 9 (ranked within each category)

### UX
1. **TodayParts.kt:211-214 vs QuickAdd.kt:101-114.** At 04:01 the Next-up card says "Dinner: 0 of ~518 kcal" because Dinner is the only unlogged meal. The + sheet preselects Breakfast by the clock, and Breakfast is already logged. Fix: put one shared `suggestMeal(mins, byMeal)` in data/. Hide Next-up between 23:00 and 05:00, or have it suggest the clock meal. In QuickAdd, prefer the clock meal, but fall back to the Next-up meal when the clock meal is already logged. Lifts UX, Robustness, Code quality.
2. **Today.kt:351.** After a second add, Breakfast reads "2 Idli · 2 Idli · 1 Medu vada" (screenshot 11). Fix: group entries by (foodId, unit, choices) and sum qty before `joinToString`. Do this for display only, and keep entries separate for undo and edit. Lifts UX, Polish.
3. **Shell.kt:157.** `LaunchedEffect(tab, page) { currentSnackbarData?.dismiss() }` dismisses "Added … · Undo" the moment the user taps another tab. Fix: dismiss only on push or pop, or let the Undo snackbar run its full duration across tab changes. Lifts UX.
4. **Insights empty state.** It shows "Log food now" even when today is already logged, so it looks as if the app ignored the data. Fix: once one or more days exist, show today's partial summary plus "1 more day unlocks trends". Lifts UX.

### UI
1. **Selection style.** The week day uses a black filled chip. The Quick-add mode uses a yellow filled pill. The Insights, Meals and Appearance segments and the meal chips use a pale yellow fill with an olive outline. Fix: use one selected treatment (accent fill with onAcc text) for all segmented controls and chips. Keep the black day chip only if it is the single "date" metaphor. Lifts UI, Polish.
2. **TodayParts.kt:143.** The sodium tile shows "1,295/2,000 …" and the unit is cut off (screenshots 6 and 11). Fix: drop the thousands separator in tiles, or show `1.3k/2k mg`, or use `maxLines = 2` or autosize. Lifts UI, Polish.
3. **PlateBuilder.kt:217.** The instruction "Tap to add · tap again for more · hold to customise" uses SectionTitle (bold, two lines) and visually outranks the grid. Fix: render it as one line in `U.t.bodyS` with `c.ink2`, and move "hold to customise" into a first-run tooltip. Lifts UI, Typography.
4. **Header baseline jump.** The Today title sits at y≈258px, but Insights and Meals sit at about 243px, because the icon buttons make Today's header taller. Fix: give all tab roots a fixed 64dp header height. Lifts UI and Animation (the title stops shifting during the fade-through).

### Animation
1. **Shell.kt:200-202.** The tab fade-through scales the whole page from 0.96, title included, so the large title visibly "breathes" on every tab tap. Fix: scale only the content below the header, or use 0.98, or fade only for root tabs. Lifts Animation.
2. **Today.kt:357.** The meal subtitle crossfades (Today.kt:352), but the kcal number on the right jumps from 102 to 343 with no transition. Fix: use `AnimatedContent` with a slide-up count transition, as the hero number already does. Lifts Animation, Polish.
3. **Today.kt:356-357.** When a meal is logged, the empty row swaps its outlined + button for a number with no transition. Fix: use `AnimatedContent(empty)` with a fade and a 0.9 scale. Lifts Animation.

### Color
1. **Today.kt:345.** The empty meal dot is slate at alpha 0.45 on the dark surface, which makes it almost invisible (screenshot 11). Fix: for empty meals, draw a hollow ring in full colour (1.5dp stroke) instead of lowering alpha. Lifts Color, Accessibility.
2. **Dark selected segment** (Appearance, screenshot 10). In dark mode accS turns olive brown (about #4A3F25) and looks dirty. Fix: in dark mode use acc at 16% over sf, or a neutral sf2 fill with accent text. Lifts Color.

### Typography
1. **Build-a-plate bottom bar.** The button says "LOG DINNER" in all caps, while "Save", "Add 2 items" and "Log food now" are sentence case. Fix: change it to "Log dinner". Lifts Typography, Polish.
2. **Quick-add hint** (screenshot 5). The centred two-line hint leaves "இட்லி”" alone on the second line. Fix: left-align it, or shorten it to two examples. Lifts Typography.

### Accessibility
1. **Today.kt:357.** The meal kcal is a bare number such as "102", which TalkBack reads as "one hundred two". Fix: add " kcal" visually as a caption, or set `semantics { contentDescription = "$kcal kilocalories" }` on the row. Lifts Accessibility, UI.
2. **Plate tiles.** Tiles read "121/piece" with no unit. Fix: change to "121 kcal · piece". Lifts Accessibility, Polish.

### Code quality
1. **QuickAdd.kt:107-114 and TodayParts.kt:211.** Meal windows are hard-coded twice, with different rules. Fix: keep a single source in data/. Lifts Code quality, UX.
2. **TodayParts.kt:215-217.** `remember { arrayOfNulls<Meal>(1) }` is mutated during composition to keep the exit content on screen. Fix: use `AnimatedContent(targetState = nextNow)`, which keeps the outgoing value by itself. Lifts Code quality.

### Robustness
1. **AddParts.kt:75 vs QuickAdd.kt:158.** Each line is rounded on its own (102 + 140), but the total rounds the raw sum (241). Fix: make the total the sum of the rounded line values. Lifts Robustness, Polish.
2. **TodayParts.kt:213.** The Next-up logic treats 04:01 as "today" and suggests Dinner for a day that has only just begun. Fix: same as UX 1. Lifts Robustness.

### Polish
1. **assets/foods.json.** Idli 🍘 renders as a rice cracker in a black wrapper. Medu vada 🍩 is a doughnut. Chapathi, Dosa and Parotta all use 🫓. Fried rice, kothu parotta and omelette all use 🍳. Fix: draw custom vector icons for the top 30 dishes, or at least give each tile on the default grid its own emoji. Lifts Polish, UI.
2. **Decorative emoji.** Emoji such as ✎ on the Sketch label, 📄 on the PDF button and ⭐ on Save are mixed in with the line icons. Fix: use the app's NavIcon line set instead. Lifts Polish.
3. **Meals empty state.** It is not inside a card, while the Insights empty state is, and 55% of the screen is blank. Fix: use the same card container and add a secondary "Create a recipe" action. Lifts Polish, UI.

## Verdict
The average is 7.7 and every category is below 9. **FAIL.**
