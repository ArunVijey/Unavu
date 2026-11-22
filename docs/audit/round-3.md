# Round 3 audit (independent)

| Category | Score | Evidence |
|---|---|---|
| UI | 8 | Cohesive cards/chips; Insights and Meals empty states large and sparse (ins.png, meals.png); snackbar sits in a second outlined frame (ins.png y~1660-1800) |
| UX | 7 | Meal hero says "Breakfast: 1832 kcal left" = whole-day remainder (today.png); same generic protein tip for every meal (dark.png); no Today/Meals shortcut CTA hierarchy beyond duplicate yellow buttons |
| Animation | 8 | Tokens + reduced-motion in Motion.kt; tab pill/FAB rotate (BottomBar.kt:68-110); FAB has no ripple/indication (BottomBar.kt:117); only 3 durations, no shared-element on add |
| Color | 8 | Dark palette good (dark.png); amber/green macro tiles encode "low vs ok" but Fat 8/64 and Carbs 32/288 are green while Protein 8/86 is amber, inconsistent meaning |
| Typography | 8 | Strong hierarchy, 1832 hero; tiny caps labels and 12sp tab captions; "Total · tap an item to change it" small grey (add2.png) |
| Accessibility | 7 | Status bar clock/icons nearly invisible on light screens (today/ins/meals/set.png) vs readable in add2.png; FAB 54dp < 48 ok but no focus indicator; snackbar auto-persists over nav; contrast of grey secondary text on cream borderline |
| Performance | 8 | Not a priority; no jank observed |
| Code quality | 8 | Tokenised Motion/Theme; stale header comments "Agent C / Round-1 skeleton" (Motion.kt:24); Theme.kt re-calls enableEdgeToEdge from composition (Theme.kt:214) |
| Robustness | 8 | Empty states handled; snackbar Undo present; reduced motion observed |
| Polish | 7 | Snackbar double frame, status-bar contrast, emoji-in-circle empty states look stock (📊 ⭐), Cheat meal chip wrapped alone to a second row (add2.png) |

Average: 7.7  
Result: FAIL (no category at 9, average < 9)

## Defects (ranked)
1. Status bar icons washed out on light screens. Theme.kt:210-218: icon style applied from an effect that misses initial/tab state. Fix: call enableEdgeToEdge once in MainActivity with SystemBarStyle.auto(TRANSPARENT, TRANSPARENT){ appDark } keyed to app dark theme, and use a SideEffect on `dark` only.
2. Meal hero copy "Breakfast: 1832 kcal left" equals whole-day remainder (today.png ~y1320). Fix: show the meal's own budget ("Breakfast: 0 of ~520 kcal") from the meal split, or drop the number.
3. Snackbar has an extra outlined container and lingers across tab switches (ins.png/meals.png y1660-1800). Fix: remove the outer border/padding wrapper at Shell.kt:222, dismiss on tab change, duration Short.
4. Generic protein tip repeats on every meal (today.png, dark.png). Fix: pick tip per meal slot and suppress when slot already logged; vary suggestions (idli+sambar, sundal for snacks).
5. Macro tile colours inconsistent: Fat 8/64 green, Protein 8/86 amber at similar progress. Fix: single rule (e.g. <50% of pro-rated-by-time target = amber) and show it with a text cue, not only colour.
6. FAB has no pressed ripple/focus ring (BottomBar.kt:117 indication = null). Fix: indication = ripple(bounded=false) or add 2dp focus outline; enlarge to 56dp.
7. Cheat meal chip wraps alone below meal row (add2.png y~925). Fix: horizontally scrollable single row, or render as a distinct secondary text button.
8. Empty states on Insights/Meals are sparse with stock emoji (ins.png, meals.png). Fix: add a ghost chart/skeleton preview on Insights; shrink container, move CTA up; use custom illustrations.
9. Insights shows two heavy empty cards (avg/weight "–") before the empty state. Fix: collapse stat cards into the empty state when no data.
10. Add sheet gives no entry/exit sense of continuity with FAB (FAB turns to + only; sheet header Food/Liquid/Weight appears abruptly in add2.png). Fix: shared-container transform from FAB to sheet using Emphasized 300 ms; stagger item rows 30 ms.
11. Secondary text (grey on cream, e.g. "Total · tap an item to change it", "Water 0 / 2.5 L") borderline contrast. Fix: darken ink2 to >= 4.5:1 on c.bg and c.sf.
12. Tab caption 12sp with SemiBold ellipsis risk at large font scale (BottomBar.kt:100). Fix: allow 2 lines or hide label for unselected beyond 1.3x scale.
13. Today list is below the fold; day card + tip + water push meal logging to y>1700 (today.png). Fix: collapse macro grid to one expandable row or move meal cards above water.
14. Stale comments / ownership notes (Motion.kt:24). Fix: replace with real docs; also expose Standard/Emphasized only with a usage table.
15. Settings "Reminders: 5 on · notifications blocked" is a dead state with no inline fix (set.png). Fix: show an "Allow" action chip that opens the app notification settings.
