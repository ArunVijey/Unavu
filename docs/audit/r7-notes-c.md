# Round 7: agent C notes (Today, History, Insights)

Files changed: `ui/screens/Today.kt`, `ui/screens/today/BudgetHero.kt`, `ui/screens/today/TodayParts.kt`, `ui/screens/today/TodaySheets.kt`, `ui/screens/tracking/History.kt`, `ui/screens/Insights.kt`, `ui/screens/insights/Sections.kt`, `ui/screens/QuickLook.kt`. New file: `res/values/strings_r7c.xml`. I didn't build, run Gradle or touch git.

## Fixed

- **#4 Rounding.**
  - Every meal share now comes from `mealShare(...)`: Next-up (it was `toInt()`), the meal sheet and the budget sheet.
  - The meal-sheet macros use `fmtNum`, so fat shows 9.7 g there as it does in the hero. Their TalkBack text says "Fat, 9.7 grams".
  - `fmtNum` now rounds to one decimal first, so 9.96 shows "10" and never "10.0". It groups through `fmtInt`, uses `Locale.ROOT` for decimals, and keeps its signature.
- **#10 History verdict.** There is now one rule, `dayState(kcal, target, inProgress)` (NONE / PROGRESS / ON / UNDER / OVER), in `History.kt`.
  - The calendar dot, the selected-day badge (UNDER → `Status.LOW`), the Today week strip and the Insights bars all use it.
  - Today shows as "in progress" until 22:00 or until dinner is logged: a grey ring, the badge reads "1,715 / 2,064 kcal so far" with a note, and the legend gains "Today so far".
- **#11 Past-day targets.**
  - `targetKcalOn(date)` drives the History calendar and badge, the week-strip dots, the Insights averages (the average of each counted day's own target), the bar colours and the weekly verdicts.
  - Today viewing a past date uses that day's kcal target for the hero, Next-up and the sheets.
  - The "now X kcal" text on the smart-target banner still shows the current target.
- **#12 Truncation.**
  - Nutrient chips use `BasicText` with `TextAutoSize.StepBased` (min 10 sp). In Sketch or above 1.15× font scale, the value and the "/ target" part each get their own line.
  - Above 1.15× the hero's "x eaten of y" line moves under the big number.
  - The meal sheet's by-meal labels show the full name, shrunk to fit ("Breakfast", not "Breakf").
  - The hero legend is a `FlowRow`.
- **#14 Insights bars.**
  - Labels are a fixed 112 dp with ellipsis; values are a fixed 96 dp, end-aligned and auto-sized. Every bar now starts and ends at the same x.
  - In Sketch, above 1.15× font scale or below 360 dp width (`stackedRows()`), the label and value sit above a full-width bar.
  - The Gap grid uses the same columns.
- **#19 Unfinished day.**
  - Today is left out of the averages while it's in progress, and a caption says so. The cheat-day count is now computed on its own.
  - With no finished days the card shows "– / no finished days yet".
  - The Gap grid fades today and doesn't count it as "low".
  - Meal timing drops back-filled entries: an entry is dropped if it was logged after a later day already had entries (ids only grow), or if its time is far outside that meal's usual hours.
  - Dinners logged just after midnight count as late, not early.
  - Hour labels are centred on their gridlines (a custom `Layout`), instead of equal columns that didn't line up.
- **#20 Number format.** Every count and kcal value in my files goes through `fmtInt`, and every gram/mg value through `fmtNum`. That covers the hero, the eaten line, TalkBack summaries, Next-up, the banner, liquids, both sheets, History, the Insights average, chart descriptions, top foods and the late-dinner delta (which now uses a real minus sign "−").
- **#21 Gutter.** Today, History, Insights and the quick-look card use `Space.gutter`, and the card spacing uses `Space.cardGap`.
- **#22 Spoken text.**
  - Meal rows say "Lunch, 751 kilocalories: 2 idli · sambar", or "Lunch, not logged yet". They also carry a click label ("Open Lunch" / "Add food to Lunch") and mention a cheat meal.
  - Logged rows now show a small "kcal" caption under the number.
  - The week arrows say "Previous week" / "Next week".
  - Sheet tracks and entries speak their units.
- **#24 Disabled arrow.** New `PagerArrow(dir, contentDescription, enabled, onClick)` in `TodayParts.kt`. When disabled it shows at 38 % alpha, ignores taps, gives no press feedback, and TalkBack hears "disabled".
  - Next week is disabled on the current week.
  - History's "Next month" had the same no-op bug and now uses it too.
- **#26 Motion.**
  - The hero's eaten count and the Sketch ring animate, as does each meal segment's width (`animateFloatAsState`, `Motion.LONG`). The "left" number already animated.
  - A meal's kcal counts up to its new value.
  - A meal row flashes `accS` (0 → 0.6 → 0 over `Motion.MEDIUM`) when it gains entries: quick add, plate, or undoing a delete. The last count is saveable, so coming back from the plate builder still flashes the row. Under reduced motion nothing animates.
- **#29 Weight card.** The title is the trend ("Weight 73.0 kg"). The subtitle is "Trend · last weigh-in 72.4 kg today", or "First weigh-in today · log another to see your trend".
- **#25 Strings.** New `dayLabel(date)` / `dayWhen(date)` composables read "Today"/"Yesterday"/"today"/"on …" from resources. Today and History use them.
  - The History weekday letters come from `DayOfWeek`. The hard-coded " kcal" and "week N" strings are now resources.
  - The leading-space resources (`d_kcal_share`, `d_of_kcal_left`, `d_kcal_over`, `d_limit_target`), whose space Android trims, are replaced by spaceless r7c strings plus a 6 dp `Spacer`. That fixes "309kcal · share".

## Not done / judgement calls
- **#19, the optional "days with fewer than 2 meals" rule:** not applied. On sparse logs it would empty Insights. Only the in-progress day is excluded.
- **`dayTitle(date)`** keeps its non-composable signature and English words for callers outside composition. Composables should use `dayLabel`.
- **Past-day Targets object:** only `kcal` follows `targetKcalOn`. The macro and nutrient targets for a past day still come from the current profile, because there is no per-day history for them.

## Requests for other owners
- **A (`Buttons.kt`):** add `enabled: Boolean = true` to `ArrowButton` (alpha 0.38, `clickable(enabled)`). `PagerArrow` could then be removed.
- **A (`Widgets2.kt`):** in `BarChart`, use `fmtInt` for the tap chip (`v.toInt()`) and the default summary (`${v.toInt()}`, `target ${it.toInt()}`). Those are the only unformatted numbers left on Insights.
- **D (`Liquids.kt`, `Weight.kt`):** use `dayLabel(date)` / `dayWhen(date)` from `Today.kt` instead of `dayTitle(date)` inside composables, so "Today/Yesterday" come from resources. "Weighed today / on 21 Nov" is exactly `dayWhen`.
- **D (`strings_d.xml`):** these keys are now unused and can be deleted: `d_eaten_of`, `d_eaten_of_line`, `d_budget_summary_left`, `d_budget_summary_over`, `d_meal_kcal`, `d_next_up_budget`, `d_new_target`, `d_new_target_sub`, `d_liquids_sub`, `d_weight_title`, `d_weight_sub`, `d_over_share`, `d_under_share`, `d_kcal_share`, `d_grams`, `d_of_kcal_left`, `d_kcal_over`, `d_limit_target`. Grep again first.
- **C cleanup after the build (mine, deferred to avoid races):** `strings_g.xml` keys now unused: `g_kcal`, `g_ins_under`, `g_ins_over`, `g_ins_no_logs`, `g_ins_kcal_chart_desc`, `g_ins_nutrient_desc`, `g_ins_late_delta`, `g_ins_timing_note`, `g_hist_cheat_line`.
- **E (`Models.kt`/`Store.kt`):** an optional `loggedOn: String? = null` (ISO date the entry was created) on `LogEntry` would let meal timing drop back-filled entries exactly, instead of using the id-order and meal-hours heuristic in `Sections.kt` `timedEntries()`.
- **E:** `judgeDay` calls `targetKcalOn` for every visible day (up to 90 on Insights, about 31 in History, 7 in the week strip), so please keep it cheap (a sorted-list lookup).
- **Coordinator, compile notes:** the new APIs used are `BasicText(autoSize = TextAutoSize.StepBased(...), color = ColorProducer)` (foundation 1.12, checked in the cached jar) and `FlowRow` under `@OptIn(ExperimentalLayoutApi::class)`. The day-judgement helpers (`DayState`, `dayState`, `judgeDay`, `dayInProgress`, `DayDot`, `spoken`, `color`, `status`) are public in `History.kt`, package `app.unavu.ui.screens`.
