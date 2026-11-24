package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.graphics.graphicsLayer
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import app.unavu.data.entriesOn
import app.unavu.data.suggestMeal
import app.unavu.data.today
import app.unavu.ui.SwitchRow
import kotlinx.coroutines.delay
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.Combo
import app.unavu.data.FoodDb
import app.unavu.data.Meal
import app.unavu.data.NewItem
import app.unavu.data.QuickParser
import app.unavu.data.addWater
import app.unavu.data.addWeight
import app.unavu.data.logItems
import app.unavu.data.nutrFor
import app.unavu.data.toNew
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.MealPicker
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SectionLabel
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.data.targets
import app.unavu.ui.AppViewModel
import app.unavu.ui.QuickAddDraft
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private enum class Mode(val labelRes: Int) { FOOD(R.string.f_mode_food), LIQUID(R.string.f_mode_liquid), WEIGHT(R.string.f_mode_weight) }

/** The + sheet: mode tabs, type it like you say it, parsed preview, pick the meal, add. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(request: QuickAddRequest, onDismiss: () -> Unit) {
    val c = U.c
    val reducedMotion = LocalReducedMotion.current
    val t = U.t
    val data = LocalData.current
    val nav = nav
    val store = nav.store
    val date = nav.date.value
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val ctx = LocalResources.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val vm: AppViewModel = viewModel()
    // what was in the sheet when it was last closed without adding (kept for 10 minutes, same mode only)
    val draft = remember { vm.quickAddDraft(request.mode) }
    var restored by rememberSaveable { mutableStateOf(draft != null) }
    var modeName by rememberSaveable { mutableStateOf(request.mode) }
    val mode = runCatching { Mode.valueOf(modeName) }.getOrDefault(Mode.FOOD)
    var text by rememberSaveable { mutableStateOf(draft?.text ?: "") }
    var mealName by rememberSaveable { mutableStateOf<String?>(request.meal?.name ?: draft?.meal ?: suggestMeal(data.entriesOn(date).map { it.meal }.toSet()).name) }
    val meal: Meal? = mealName?.let { n -> Meal.entries.firstOrNull { it.name == n } }
    var cheat by rememberSaveable { mutableStateOf(draft?.cheat ?: false) }
    var waterMl by rememberSaveable { mutableIntStateOf(draft?.waterMl ?: 0) }
    var waist by rememberSaveable { mutableStateOf(draft?.waist ?: "") }
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    /** Items the user customised, keyed by "position:food id" so they survive re-parsing, rotation and process death. */
    val overrides = rememberSaveable(saver = OverridesSaver) {
        mutableStateMapOf<String, NewItem>().apply { draft?.let { putAll(it.overrides) } }
    }
    var comboItems by rememberSaveable(stateSaver = ItemsSaver) { mutableStateOf<List<NewItem>>(draft?.comboItems ?: emptyList()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(mode) { runCatching { focus.requestFocus() } }

    val parsed = remember(text, mode, data.portions) {
        if (mode == Mode.WEIGHT) QuickParser.Result(emptyList(), emptyList())
        else QuickParser.parse(text, data.portions, liquidsOnly = mode == Mode.LIQUID)
    }
    val typed: List<Pair<String, NewItem>> = parsed.items.mapIndexed { i, it -> "$i:${it.food.id}".let { k -> k to (overrides[k] ?: it.toNew()) } }
    val all: List<Pair<String, NewItem>> = typed + comboItems.mapIndexed { i, it -> "combo$i" to it }
    val needsMeal = all.isNotEmpty()
    // water typed in the text ("1 glass water", "thanni") plus the tiles: it goes to Liquids, never to a meal
    val waterTotal = waterMl + parsed.waterMl
    // the total is the sum of the rounded line values shown above it, so the lines always add up to the total
    val totalKcal = all.sumOf { (_, i) -> FoodDb.get(i.foodId)?.nutrFor(i.qty, i.unit, i.choices)?.kcal?.roundToInt() ?: 0 }
    var done by remember { mutableStateOf(false) }
    // the keyboard leaves the moment Add is pressed, together with the sheet
    val closeAfter: () -> Unit = { keyboard?.hide(); focusManager.clearFocus(); done = true }
    val saveDraft: () -> Unit = {
        vm.saveQuickAddDraft(QuickAddDraft(mode.name, text, mealName, cheat, waterMl, waist, overrides.toMap(), comboItems))
    }
    val startOver: () -> Unit = {
        text = ""; overrides.clear(); comboItems = emptyList(); waterMl = 0; waist = ""; cheat = false
        restored = false; vm.clearQuickAddDraft()
    }
    var dismissed by remember { mutableStateOf(false) }
    // added: nothing to keep; closed with ✕, the scrim, back or a link: keep what was typed for 10 minutes
    val dismissOnce: () -> Unit = {
        if (!dismissed) {
            dismissed = true
            if (done) vm.clearQuickAddDraft() else saveDraft()
            onDismiss()
        }
    }
    val leaveTo: (Page) -> Unit = { page -> dismissOnce(); nav.push(page) }
    LaunchedEffect(done) {
        if (done && !dismissed) {
            delay(250) // let the success tick land, then slide the sheet away
            if (!dismissed) { runCatching { sheet.hide() }; dismissOnce() }
        }
    }
    val kcalShown by animateIntAsState(totalKcal, motionTween(Motion.LONG), label = "kcalCount")

    // continuity with the FAB: content grows from the bottom-right corner (where the FAB sits)
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterScale by animateFloatAsState(if (entered || reducedMotion) 1f else 0.94f, motionTween(Motion.LONG, Motion.EmphasizedDecelerate), label = "sheetEnter")
    ModalBottomSheet(onDismissRequest = dismissOnce, sheetState = sheet, containerColor = c.bg, dragHandle = null) {
        Column(Modifier.fillMaxWidth().graphicsLayer { transformOrigin = TransformOrigin(0.92f, 1f); scaleX = enterScale; scaleY = enterScale }.padding(horizontal = 16.dp).padding(top = 16.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SlidingSegments(Mode.entries, mode, { stringResource(it.labelRes) }) { m ->
                    modeName = m.name; text = ""; overrides.clear(); comboItems = emptyList(); restored = false
                }
                Spacer(Modifier.weight(1f))
                ArrowButton(Arrow.CLOSE, contentDescription = stringResource(R.string.f_close), onClick = dismissOnce)
            }

            // big natural-language field
            val fieldLabel = stringResource(when (mode) { Mode.FOOD -> R.string.f_field_food; Mode.LIQUID -> R.string.f_field_liquid; Mode.WEIGHT -> R.string.f_field_weight })
            Box(Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                if (text.isEmpty()) {
                    Text(
                        stringResource(when (mode) { Mode.FOOD -> R.string.f_hint_food; Mode.LIQUID -> R.string.f_hint_liquid; Mode.WEIGHT -> R.string.f_hint_weight }),
                        style = t.headline, color = c.hint.copy(alpha = 0.5f),
                    )
                }
                BasicTextField(
                    value = text, onValueChange = { text = it; restored = false },
                    textStyle = t.headline.copy(color = c.ink),
                    cursorBrush = SolidColor(c.acc),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mode == Mode.WEIGHT) KeyboardType.Decimal else KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = fieldLabel },
                )
            }
            if (restored && (text.isNotBlank() || waterMl > 0 || comboItems.isNotEmpty() || waist.isNotBlank())) {
                // the sheet was closed a moment ago with this in it: say so, and offer a clean start
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.r7e_draft_restored), style = t.caption, color = c.ink2, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).semantics { role = Role.Button }
                            .clickable(onClick = startOver).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.r7e_draft_start_over), style = t.label, fontWeight = FontWeight.Bold, color = c.accInk)
                    }
                }
            }

            when (mode) {
                Mode.FOOD, Mode.LIQUID -> {
                    // words we couldn't place get suggestions, never a silent guess
                    // don't nag about the word that is still being typed
                    val typing = if (text.isNotEmpty() && !text.last().isWhitespace()) text.trim().split(Regex("\\s+")).last().lowercase() else null
                    val stillTyping = typing != null && (
                        typing.length < 4 || QuickParser.startsKnownWord(typing) || FoodDb.dishes.any { f -> f.aliases.any { it.startsWith(typing) } }
                    )
                    parsed.unknown.distinct().filterNot { stillTyping && it == typing }.forEach { word ->
                        val sugg = remember(word, mode) { FoodDb.search(word, limit = 3, includeRaw = false).filter { mode == Mode.FOOD || it.liquid } }
                        AppCard(color = c.turS, border = null, padding = PaddingValues(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(stringResource(R.string.f_didnt_find, word), style = t.label, fontWeight = FontWeight.Bold, color = c.turInk)
                            if (sugg.isNotEmpty()) {
                                Text(stringResource(R.string.f_did_you_mean), style = t.caption, color = c.ink2, modifier = Modifier.padding(vertical = 4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    sugg.forEach { f ->
                                        ChoiceChip("${f.emoji} ${f.name}", false) {
                                            val alias = f.aliases.firstOrNull { it.split(" ").size == 1 } ?: f.aliases.first()
                                            text = text.replace(Regex("(?i)\\b" + Regex.escape(word) + "\\b"), alias)
                                        }
                                    }
                                }
                            }
                            Text(
                                stringResource(R.string.f_search_all), style = t.label, fontWeight = FontWeight.Bold, color = c.accInk,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { role = Role.Button }
                                    .clickable { leaveTo(Page.Plate(meal, date)) }.padding(top = 12.dp),
                            )
                            Text(
                                stringResource(R.string.f_add_to_my_foods, word), style = t.label, fontWeight = FontWeight.Bold, color = c.accInk,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { role = Role.Button }
                                    .clickable { leaveTo(Page.MyFood(null, word.replaceFirstChar { it.uppercase() })) }.padding(top = 12.dp),
                            )
                        }
                    }
                    if (all.isNotEmpty() || parsed.water.isNotEmpty()) {
                        AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            all.forEachIndexed { n, (key, item) -> key(key) { StaggerIn(n) { ParsedRow(item) { editingKey = key } } } }
                            parsed.water.forEachIndexed { n, w -> key("water$n") { StaggerIn(all.size + n) { WaterRow(w.ml) } } }
                            if (all.isNotEmpty()) {
                                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
                                    Text(stringResource(R.string.f_total_hint), color = c.ink2, style = t.caption, modifier = Modifier.weight(1f))
                                    Text(stringResource(R.string.r7e_kcal, fmtInt(kcalShown)), style = t.bodyS, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (mode == Mode.FOOD && text.isEmpty() && comboItems.isEmpty()) {
                        val combos = remember(data.combos) { data.combos.sortedWith(compareByDescending<Combo> { it.uses }.thenBy { it.order }).take(6) }
                        if (combos.isNotEmpty()) {
                            SectionLabel(stringResource(R.string.f_usual_title))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                combos.forEach { cb ->
                                    ChoiceChip("${cb.emoji} ${cb.name}", false) {
                                        comboItems = cb.items.map { it.toNew() }
                                        if (meal == null) mealName = cb.meal?.name
                                        store.update { d -> d.copy(combos = d.combos.map { if (it.id == cb.id) it.copy(uses = it.uses + 1) else it }) }
                                    }
                                }
                            }
                        }
                    }
                    if (mode == Mode.LIQUID) {
                        SectionLabel(stringResource(R.string.f_water_title))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Triple("🥛", R.string.f_glass, QuickParser.GLASS_ML), Triple("🍶", R.string.f_tumbler, 150), Triple("🧴", R.string.f_bottle, 500)).forEach { (e, n, ml) ->
                                LiquidTile(e, stringResource(n), stringResource(R.string.r7e_ml, fmtInt(ml)), false, Modifier.weight(1f)) { waterMl += ml }
                            }
                        }
                        if (waterMl > 0) {
                            val clear = stringResource(R.string.f_water_clear)
                            Text(
                                stringResource(R.string.r7e_water_added, fmtInt(waterMl)), style = t.body, fontWeight = FontWeight.Bold, color = c.blueInk,
                                modifier = Modifier.heightIn(min = 48.dp).padding(top = 8.dp).semantics { role = Role.Button; contentDescription = clear }.clickable { waterMl = 0 },
                            )
                        }
                        SectionLabel(stringResource(R.string.f_drinks_title))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("☕ kaapi" to "kaapi", "🍵 tea" to "tea", "🥤 boost" to "boost", "🥤 horlicks" to "horlicks", "🥛 milk" to "milk",
                                "🥛 buttermilk" to "buttermilk", "🥥 elaneer" to "elaneer", "🧃 juice" to "juice", "🍋 lime juice" to "lime juice").forEach { (label, w) ->
                                ChoiceChip(label, false) { text = (text.trim() + " 1 $w").trim() }
                            }
                        }
                    }

                    if (needsMeal) {
                        SectionLabel(stringResource(R.string.f_which_meal))
                        MealPicker(meal) { mealName = it.name }
                        SwitchRow(stringResource(R.string.f_cheat_meal), cheat) { cheat = it }
                    }
                    Spacer(Modifier.height(12.dp))
                    val count = all.size + if (waterTotal > 0) 1 else 0
                    val canAdd = count > 0 && (!needsMeal || meal != null)
                    AddButton(
                        text = when {
                            count == 0 -> stringResource(if (mode == Mode.FOOD) R.string.f_btn_type_food else R.string.f_btn_add_drink)
                            needsMeal && meal == null -> stringResource(R.string.f_btn_pick_meal)
                            all.isEmpty() -> stringResource(R.string.r7e_btn_add_water, fmtInt(waterTotal))
                            mode == Mode.FOOD && waterTotal > 0 ->
                                pluralStringResource(R.plurals.r7e_btn_add_items_water, all.size, all.size, fmtInt(totalKcal), fmtInt(waterTotal))
                            mode == Mode.FOOD -> pluralStringResource(R.plurals.r7e_btn_add_items, all.size, all.size, fmtInt(totalKcal))
                            count == 1 -> stringResource(R.string.f_btn_add_drink_one)
                            else -> stringResource(R.string.f_btn_add_drinks, count)
                        },
                        enabled = canAdd, success = done,
                    ) {
                        val before = store.data.value
                        val m = meal
                        val names = all.mapNotNull { FoodDb.get(it.second.foodId)?.name }
                        if (all.isNotEmpty() && m != null) store.logItems(all.map { it.second }, m, date, cheat)
                        if (waterTotal > 0) store.addWater(date, waterTotal)
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        val msg = when {
                            names.isNotEmpty() && m != null && waterTotal > 0 ->
                                ctx.getString(R.string.r7e_added_to_with_water, names.joinToString(", "), m.label, fmtInt(waterTotal))
                            names.isNotEmpty() && m != null -> ctx.getString(R.string.f_added_to, names.joinToString(", "), m.label)
                            else -> ctx.getString(R.string.r7e_water_snack, fmtInt(waterTotal))
                        }
                        feedback?.show(msg, ctx.getString(R.string.f_undo)) { store.restoreSnapshot(before) }
                        closeAfter()
                    }
                    Text(
                        stringResource(R.string.f_build_plate), style = t.label, fontWeight = FontWeight.Bold, color = c.accInk, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 12.dp).semantics { role = Role.Button }
                            .clickable { leaveTo(Page.Plate(meal, date)) },
                    )
                    Text(
                        stringResource(if (mode == Mode.FOOD) R.string.f_tip_food else R.string.f_tip_liquid),
                        style = t.caption, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    )
                }
                Mode.WEIGHT -> {
                    val kg = text.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                    val bad = text.isNotBlank() && (kg == null || kg !in 20.0..300.0)
                    val rangeMsg = stringResource(R.string.f_weight_range_error)
                    Text(
                        if (date == today()) stringResource(R.string.f_weigh_today) else stringResource(R.string.f_weigh_date, date),
                        color = c.ink2, style = t.caption,
                    )
                    if (bad) Text(rangeMsg, color = c.chiInk, style = t.caption, modifier = Modifier.semantics { error(rangeMsg) })
                    Spacer(Modifier.height(10.dp))
                    NumberField(waist, stringResource(R.string.f_waist_optional), Modifier.width(180.dp)) { waist = it }
                    Spacer(Modifier.height(12.dp))
                    AddButton(stringResource(R.string.f_btn_save_weight), enabled = kg != null && kg in 20.0..300.0, success = done) {
                        val w = kg ?: return@AddButton
                        val before = store.data.value
                        store.addWeight(date, w, waist.replace(",", ".").toDoubleOrNull())
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        // a new weight can move the daily target (Mifflin-St Jeor): never silently. Past days keep
                        // their old target (target history); Undo puts back both the weigh-in and the target.
                        val oldTarget = before.targets()?.kcal
                        val newTarget = store.data.value.targets()?.kcal
                        val msg = if (oldTarget != null && newTarget != null && newTarget != oldTarget) {
                            ctx.getString(R.string.r7e_weight_saved_target, fmtQty(w), fmtInt(oldTarget), fmtInt(newTarget))
                        } else {
                            ctx.getString(R.string.f_weight_saved, fmtQty(w))
                        }
                        feedback?.show(msg, ctx.getString(R.string.f_undo)) { store.restoreSnapshot(before) }
                        closeAfter()
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    editingKey?.let { key ->
        val item = all.firstOrNull { it.first == key }?.second
        val food = item?.let { FoodDb.get(it.foodId) }
        if (item == null || food == null) editingKey = null
        else CustomiseSheet(food, item, onDismiss = { editingKey = null }, onRemove = {
            if (key.startsWith("combo")) comboItems = comboItems.filterIndexed { i, _ -> "combo$i" != key }
            else {
                val src = parsed.items.getOrNull(key.substringBefore(":").toInt())?.source
                if (src != null) text = text.replaceFirst(Regex("(?i)(\\S+\\s+)?" + Regex.escape(src)), "").trim()
                overrides.remove(key)
            }
            editingKey = null
        }) { updated ->
            if (key.startsWith("combo")) comboItems = comboItems.mapIndexed { i, it -> if ("combo$i" == key) updated else it }
            else overrides[key] = updated
            editingKey = null
        }
    }
}

/** A "💧 Water 250 ml → Liquids" row for water typed in the text: it is logged to Liquids, not to the meal. */
@Composable
private fun WaterRow(ml: Int) {
    val t = U.t
    val amount = stringResource(R.string.r7e_ml, fmtInt(ml))
    val spoken = stringResource(R.string.r7e_water_row_desc, fmtInt(ml))
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clearAndSetSemantics { contentDescription = spoken }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.r7e_water_row), style = t.bodyS)
            Row(Modifier.padding(top = 3.dp)) { StatusBadge(stringResource(R.string.f_to_liquids), Status.INFO) }
        }
        Text(amount, style = t.bodyS, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private val itemListSerializer = ListSerializer(NewItem.serializer())
private val itemMapSerializer = MapSerializer(String.serializer(), NewItem.serializer())

/** Keeps a list of unsaved items across rotation and process death (as JSON). */
private val ItemsSaver = Saver<List<NewItem>, String>(
    save = { Json.encodeToString(itemListSerializer, it) },
    restore = { runCatching { Json.decodeFromString(itemListSerializer, it) }.getOrNull() },
)

/** Keeps the customised quick-add items (keyed "position:food id") across rotation and process death. */
private val OverridesSaver = Saver<SnapshotStateMap<String, NewItem>, String>(
    save = { Json.encodeToString(itemMapSerializer, it.toMap()) },
    restore = { s ->
        mutableStateMapOf<String, NewItem>().apply { runCatching { Json.decodeFromString(itemMapSerializer, s) }.getOrNull()?.let { putAll(it) } }
    },
)
