# Unavu v1.2.0 — Audit round 6 (Opus, independent)

Build verified on emulator-5554: versionName=1.2.0 (installed from app/build/outputs/apk/release/app-release.apk; emulator had 1.1.1).
Covered: Today (top + scrolled), quick add "rendu idli oru vadai" -> Snacks, Insights, Meals, Settings, Appearance, Today in dark mode.
Code read: ui/theme/Motion.kt, ui/shell/Shell.kt, ui/shell/BottomBar.kt, parts of QuickAdd.kt, Today.kt, settings/SettingsScreen.kt; animation usage grepped across ui/.

## Scores

| Category | Score | Evidence |
|---|---|---|
| UI | 7 | Uneven meal rows (Today.kt:309-310); orphaned "Fix" link on its own line in Settings (SettingsScreen.kt:139-145); five different "add" affordance styles on one screen |
| UX | 7 | Sheet says "Add 2 items · 242 kcal", Today then shows Snacks 241 (QuickAdd.kt:138 vs Today.kt:302); "Which meal? · tap one" when a meal is already preselected; Meals empty state says "tap + here" but there is no + on Meals other than the global FAB |
| Animation | 7 | Good token system and reduced-motion support (Motion.kt), but: the bottom bar's AnimatedVisibility changes the height of the weight(1f) content during every push/pop (Shell.kt:162-230); predictive back on a tab resets the scale to 1 and then fades through, so the preview snaps (Shell.kt:153-161); the FAB rotates to x underneath a full-height sheet that hides it (BottomBar.kt BarFab + skipPartiallyExpanded); raw tween(210/90/300) literals skip the reduced-motion helpers |
| Color | 7 | The turmeric selection ring and outline (radio, meal chip, "All" chip) is about 1.6:1 against white/cream, under the 3:1 non-text minimum; nutrient bars are all grey, so there is no over/under-target colour signal; the divider between Lunch and Snacks is tinted pink while the others are neutral |
| Typography | 8 | Strong hero numerals and a clear hierarchy; the "kcal" unit sits on the baseline while "343 eaten of 2073" floats lower; macro tiles have no units (565/2,000 with no mg, 52/288 with no g) |
| Accessibility | 7 | Contrast of selection indicators (see Color); macro tiles have no units for TalkBack; tab Column sets contentDescription and also shows a Text label (BottomBar.kt), so the label can be announced twice |
| Performance | 8 | Not a priority. Relayout on every frame of the bottom-bar animation is the only visible cost |
| Code quality | 6 | Many inline fully-qualified names (androidx.compose.animation.core.tween(...), android.content.Intent... in SettingsScreen.kt:124-131, Shell.kt:100-104); Components.kt is 610 lines and Today.kt is 535; motion literals outside the tokens (Shell.kt:183, AddParts.kt:104/122, FoodDetail.kt:243) |
| Robustness | 7 | The IME stays on screen over Today after the sheet closes (screenshot 4added: keyboard covers Today about 350 ms after Add); kcal rounding disagrees between the preview and the stored totals |
| Polish | 6 | Large gap under Reminders with a lone "Fix"; at 03:36 the clock preselects Breakfast; "Cheat meal" wraps alone onto a second row; a blank ⚪ emoji for Idli; the redirect row "Saved combos & my recipes · In the Meals tab" is dead weight; meal filter chips show on an empty Meals list; the PDF report buttons show with no data |

**Average: 7.0 — FAIL** (every category is below 9).

## Defects to fix to reach 9, ranked by impact

1. **Kcal shown in the sheet disagrees with what Today shows** — QuickAdd.kt:138 rounds each line and then sums (242); Today.kt:302 sums and then rounds (241). Fix: use one rounding rule everywhere. Store or derive the per-entry kcal as the rounded value and sum those for meal and day totals, or show the unrounded sum in the sheet. Add a test asserting button total == meal row delta. Lifts: UX, Robustness, Polish.
2. **The keyboard stays over Today after Add** — QuickAdd.kt, the `LaunchedEffect(done)` block (~line 143). Fix: call `LocalSoftwareKeyboardController.current?.hide()` and `focusManager.clearFocus()` as soon as Add is pressed, before the 250 ms delay and `sheet.hide()`, so the keyboard and sheet leave together and the undo snackbar is visible. Lifts: Robustness, UX, Animation.
3. **Content height animates during push/pop** — Shell.kt:162-230. The bottom bar sits in the same Column as `Box(weight(1f))`, so its slide resizes the page every frame. Fix: overlay the bar (a `Box` with the bar aligned to BottomCenter), give the tab content fixed bottom padding equal to the bar height, and animate the bar with `graphicsLayer` translationY only. Lifts: Animation, Performance, UI.
4. **The predictive-back preview snaps on tab-level back** — Shell.kt:153-161. Fix: when popping from a non-Today tab, keep `backProgress` until the AnimatedContent transition starts, or animate it back to 0 with `Animatable` in the same spec as the fade-through. Better still, follow the system and do not intercept back on root tabs (or only scale with a cross-fade to Today). Lifts: Animation, Polish.
5. **Selection indicators fail 3:1 non-text contrast** — the turmeric ring on the theme radios (Appearance), the preselected meal chip (QuickAdd MealPicker) and the "All" filter chip (Meals). Fix: on light surfaces use `c.accInk` (dark ochre) for selected strokes and checks and keep turmeric as the fill; or switch selected chips to a filled `c.acc` background with `c.onAcc` text, matching the "Food" segment. Lifts: Color, Accessibility, UI.
6. **Meal rows have uneven heights** — Today.kt:309-310 (`heightIn 56 vs 64`, `padding 4 vs 14`). Fix: one fixed row height (72dp) and one padding for both states, so Snacks and Dinner don't jump when logged. Remove the pink divider tint so every divider is `c.line`. Lifts: UI, Polish, Animation (no layout jump on log).
7. **The "Fix" link is orphaned** — SettingsScreen.kt:139-145. Fix: put the action inside SettingsRow as a trailing text button (replacing the chevron), or show a warning-coloured subtitle "Notifications blocked — Fix". Remove the extra 48dp line. Lifts: UI, Polish.
8. **Selection language is inconsistent** — selected items appear as a filled white segment (Insights, Meals tabs, Appearance), a filled yellow segment (QuickAdd mode), an outlined yellow pill (meal chip, "All") and a filled yellow ring (radio). Fix: pick one chip selected style (filled `accS` with an `accInk` border and text) and one segmented style, and apply them everywhere. Lifts: UI, Color, Polish.
9. **Add affordances are inconsistent** — the yellow "+ Log" pill, yellow ring "+", blue "+ glass" and green "+ weigh-in" all sit on Today. Fix: one secondary add-button component in two tones at most. Lifts: UI, Polish.
10. **Quick-add meal copy and layout** — "Which meal? · tap one" when one is already selected; "Cheat meal" wraps alone; at 03:36 the clock preselects Breakfast. Fix: change the label to "Adding to" with the selected chip; make Cheat a toggle switch row below the chips rather than a fifth chip; map 23:00-04:59 to Snacks/Dinner (late night) in `mealByClock()`. Lifts: UX, Polish.
11. **The FAB rotation is invisible** — BarFab rotates to x under a full-height `skipPartiallyExpanded` sheet. Fix: either delete the rotation, or animate the sheet's own close (X) button from a rotated + so the morph is visible (shared-element feel). Lifts: Animation.
12. **Motion literals bypass the tokens and reduced motion** — Shell.kt:183 (210/90), AddParts.kt:104/122 (300, 90), FoodDetail.kt:243 (90). Fix: add `Motion.XSHORT = 90` and `Motion.FADE_THROUGH = 210`, and route them all through `motionTween()`. Lifts: Animation, Code quality.
13. **Macro tiles have no units and no status colour** — Today hero tiles. Fix: "52 / 288 g", "565 / 2,000 mg"; colour the bar fill (leaf when on track, chilli when over the limit for sodium) and give the tile a contentDescription like "Carbs 52 of 288 grams". Lifts: Color, Accessibility, Typography.
14. **Hero baseline mismatch** — "kcal" and "343 eaten of 2073" aren't baseline-aligned with the 1730 numeral. Fix: `Row(verticalAlignment = Alignment.Bottom)` with `Modifier.alignByBaseline()` on all three texts. Lifts: Typography, Polish.
15. **Empty-state noise** — the Meals filter chips show with zero combos; the "tap + here" copy points to the global FAB, which opens quick add, not the combo builder; the Insights PDF buttons show with no data; the Settings "Saved combos & my recipes" redirect row is pointless. Fix: hide the chips when the list is empty, rewrite the copy to "tap Build a combo", disable or hide report months without entries, and delete the redirect row. Lifts: UX, Polish.
16. **Missing food emoji** — Idli shows a ⚪ placeholder in the quick-add preview. Fix: give FoodDb a real glyph (for example 🍚, or a custom vector) for idli and audit the other common foods. Lifts: Polish.
17. **Bottom-bar semantics** — the `contentDescription` on the tab Column duplicates the visible Text. Fix: remove the contentDescription and use `Modifier.semantics(mergeDescendants = true)` so the label is read once with "Tab, selected". Lifts: Accessibility.
18. **Code hygiene** — import the fully-qualified calls (SettingsScreen.kt:120-131, Shell.kt:100-104, QuickAdd.kt:143-152); split Components.kt (610 lines) and Today.kt (535 lines) by section; move the notification-fix logic out of the composable into a helper. Lifts: Code quality.

## Verdict

Average 7.0. The bar is every category ≥ 9 and an average ≥ 9. **FAIL.**
