# Round 7: agent D notes (meals, editors, settings, tracking)

Files changed: Meals.kt, MyFoods.kt, meals/RecipeEditor.kt, meals/MyFoodEditor.kt, new meals/DiscardGuard.kt, SmartTargetScreen.kt, Onboarding.kt, settings/* (About, Appearance, Backup, Goals, Portions, Profile, Reminders, Settings), tracking/Weight.kt, tracking/Liquids.kt, data/Export.kt, strings_i.xml, strings_d.xml, new strings_r7d.xml.

## Fixed
- #5 SmartTarget, RecipeEditor, MyFoodEditor hero cards use `heroBg`/`onHero`.
- #6 `rememberDiscardGuard` (BackHandler when dirty plus TopBar back) in both editors, with its own Keep editing / Discard dialog.
- #8 Appearance summary is a joined list; AMOLED only when the effective theme is dark (also Smart target and Backup subtitles).
- #18 Meals tab is "Saved combos" / "My foods" with Foods and Recipes sections, each with an add button. Saving returns to that tab and flashes the saved row. Settings > My foods shows foods and recipes counts.
- #20 `fmtInt` everywhere in my files. #25 hard-coded strings moved to `strings_r7d.xml`.
- #21 editors, Meals, Settings, Onboarding, Liquids and Weight use `Space.gutter` / `Space.cardGap`.
- #28 `InputDialog(validate=...)`: goal weight 20-300 kg, water goal 0.5-6 L, recipe grams 1-5000, smart-target kcal 1,000-6,000.
- #29 About reads the real version via `getPackageInfo`. Backup is `unavu-backup.json` and the old name is still accepted on restore. "Step 1 of 2" and StepBar are hidden when editing details. Liquids heading and title, and Weight row labels, use `dayLabel`.
- #30 Onboarding fields start empty with "e.g." placeholders, Calculate is disabled until all are valid, and Age is focused.
- Goals uses `mealShare`.
- C's request: deleted the 18 unused `d_*` keys from `strings_d.xml` (all had zero references in `app/src`).

## Not done / notes
- The Weight card wording on Today ("Trend ... weighed today") is in Today.kt (agent C, done there).
- The Weight delete snackbar and desc use `dayLabel` ("for Today"); `dayWhen` would need new strings.
- `g_liq_today` in `strings_g.xml` (C's file) is now unused.
- Settings > My foods is kept as a link to the same list (`MyFoodsScreen`).

## Requests
- C: delete `g_liq_today` if unused.
