# Notes: group 4 (letter f)

## Changed
- QuickAdd.kt (+ add/AddParts.kt): mode tabs are selectableGroup + Role.Tab with c.onAcc text; placeholder c.hint; text field has a spoken label; parse wrapped in remember(text, mode, data.portions); mode/text/meal/cheat/water/waist/editing-key are rememberSaveable; "Added X to Meal" snackbar with Undo + haptic (also weight and water); unknown-word card has "Search all foods" and "Add to My foods" (Page.MyFood(null, word)); weight validated 20-300 with error() semantics; Add button label states why it is disabled; U.t roles; all targets >= 48dp.
- Customise.kt: qty/unit saveable, ChoiceChip is selectable and >= 48dp, type roles, strings. (Choices map is not saved across process death.)
- Meals.kt: LazyColumn with keys/contentType/animateItem, empty states with an action, undo for combo + recipe delete, log-combo snackbar with Undo, 48dp menu/delete buttons with descriptions, saveable tab/filter/dialog id. Recipe editor moved to meals/RecipeEditor.kt.
- MyFoods.kt: LazyColumn, undo delete, empty state; editor moved to meals/MyFoodEditor.kt with saveable fields, 48dp radio-style emoji picker, "why Save is disabled" line, chiInk.
- FoodDetail.kt: type roles, saveable unit/qty/meal/customise, add snackbar + Undo + haptic, macro donut has a spoken summary.
- PlateBuilder.kt: type roles, saveable meal/cheat/query/editing/naming/adding id, 48dp steppers with descriptions, tiles/result rows merged with Role.Button + long-press labels + selected quantity, log snackbar with Undo + haptic.
- strings_f.xml: strings for QuickAdd, Meals, My foods, plate builder.

## Not done
- PlateBuilder: the plate item list (`items`) is still `remember`, not saved across process death (NewItem is not Parcelable/Serializable here). Still hard-coded English: most PlateBuilder strings, FoodDetail strings, MyFoodEditor field labels, RecipeEditor strings. PlateBuilder not split into add/ (about 340 lines).
- FoodDetail/PlateBuilder macro-total "Protein/Carbs" tiles are not described individually.
- Delete recipe in RecipeEditor and the ingredient-gram dialog are still plain dialogs.
- Test failure seen in the final build: `ContrastTest` (group 2's file), not mine.

## Requests for other owners
- Group 3: `Stepper` (Widgets2) should be 48dp; `AppCard(onClick=)` should merge + Role.Button (I rely on it in Meals/MyFoods).
