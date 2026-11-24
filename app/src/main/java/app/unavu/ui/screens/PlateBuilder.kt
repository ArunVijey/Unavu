package app.unavu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.AppData
import app.unavu.data.Combo
import app.unavu.data.ComboItem
import app.unavu.data.Food
import app.unavu.data.FoodDb
import app.unavu.data.Meal
import app.unavu.data.NewItem
import app.unavu.data.Nutr
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.data.logItems
import app.unavu.data.newKey
import app.unavu.data.nutrFor
import app.unavu.data.saveCombo
import app.unavu.data.toCombo
import app.unavu.data.toNew
import app.unavu.data.unitLabel
import app.unavu.ui.AppCard
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.EmojiText
import app.unavu.ui.EmptyState
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.MealPicker
import app.unavu.ui.Page
import app.unavu.ui.SearchField
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Stepper
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.pressScale
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import app.unavu.ui.theme.edge
import app.unavu.ui.theme.motionSpring
import app.unavu.ui.theme.motionTween
import kotlinx.coroutines.delay
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/** Sides that usually go with a main. */
val PAIRINGS: Map<String, List<String>> = run {
    val tiffin = listOf("sambar", "coconut_chutney", "tomato_chutney", "kara_chutney", "mint_chutney", "idli_podi", "gingelly_oil_side", "vada_curry", "puli_kuzhambu", "chicken_curry", "chicken_salna", "medu_vada")
    val bread = listOf("veg_kurma", "salna", "chicken_salna", "chicken_curry", "egg_curry", "dal_fry", "paneer_butter_masala", "chana_masala", "omelette")
    val rice = listOf("sambar", "rasam", "kara_kuzhambu", "puli_kuzhambu", "mor_kuzhambu", "fish_curry", "chicken_curry", "mutton_curry", "dal_fry",
        "cabbage_poriyal", "beans_poriyal", "carrot_poriyal", "kootu", "keerai", "appalam", "curd", "pickle", "egg_boiled", "omelette", "fish_fry")
    mapOf(
        "idli" to tiffin, "dosa" to tiffin, "set_dosa" to tiffin, "uthappam" to tiffin, "rava_dosa" to tiffin, "ghee_roast" to tiffin,
        "masala_dosa" to tiffin, "onion_dosa" to tiffin, "egg_dosa" to tiffin, "paniyaram" to tiffin, "ragi_dosa" to tiffin, "pesarattu" to tiffin,
        "adai" to listOf("aviyal", "coconut_chutney", "kara_chutney", "sambar"),
        "pongal" to listOf("sambar", "coconut_chutney", "gothsu", "medu_vada"),
        "upma" to listOf("coconut_chutney", "sambar", "banana_small"), "rava_kichadi" to listOf("coconut_chutney", "sambar"), "semiya_upma" to listOf("coconut_chutney"),
        "poori" to listOf("potato_masala", "chana_masala", "veg_kurma"),
        "chapathi" to bread, "parotta" to bread, "butter_naan" to bread,
        "appam" to listOf("coconut_milk_sugar", "veg_stew", "chicken_stew", "kadala_curry", "egg_curry", "veg_kurma"),
        "idiyappam" to listOf("coconut_milk_sugar", "veg_kurma", "egg_curry", "chicken_stew", "veg_stew"),
        "puttu" to listOf("kadala_curry", "banana_small", "coconut_milk_sugar"),
        "rice" to rice, "rice_raw" to rice, "brown_rice" to rice,
        "chicken_biryani" to listOf("raita", "egg_boiled", "chicken_salna", "chicken_65"), "mutton_biryani" to listOf("raita", "egg_boiled", "chicken_salna"),
        "veg_biryani" to listOf("raita", "salna"), "egg_biryani" to listOf("raita", "salna"),
        "chicken_fried_rice" to listOf("gobi_manchurian", "chilli_chicken"), "egg_fried_rice" to listOf("gobi_manchurian", "chilli_chicken"),
        "veg_fried_rice" to listOf("gobi_manchurian"), "veg_noodles" to listOf("gobi_manchurian", "chilli_chicken"),
        "curd_rice" to listOf("pickle", "appalam", "potato_fry"), "lemon_rice" to listOf("potato_fry", "appalam"), "tamarind_rice" to listOf("appalam", "potato_fry"),
        "sambar_rice" to listOf("potato_fry", "appalam"),
    )
}

private val TILES = mapOf(
    Meal.BREAKFAST to listOf("idli", "dosa", "poori", "chapathi", "pongal", "upma", "appam", "idiyappam", "medu_vada", "uthappam", "masala_dosa", "parotta", "bread_omelette", "filter_coffee", "tea"),
    Meal.LUNCH to listOf("rice", "sambar", "rasam", "kara_kuzhambu", "cabbage_poriyal", "beans_poriyal", "kootu", "curd", "appalam", "egg_boiled", "chicken_curry", "fish_fry", "chicken_biryani", "curd_rice", "lemon_rice"),
    Meal.SNACKS to listOf("filter_coffee", "tea", "boost", "egg_puff", "veg_puff", "bajji", "medu_vada", "samosa", "sundal", "biscuit", "murukku", "banana", "mixture", "buttermilk", "fresh_juice"),
    Meal.DINNER to listOf("chapathi", "dosa", "idli", "parotta", "chicken_fried_rice", "egg_fried_rice", "chicken_biryani", "rice", "chicken_curry", "egg_curry", "kothu_parotta", "idiyappam", "omelette", "curd_rice", "veg_noodles"),
)

/** Foods you actually log most in this meal (last 60 days), then the defaults. */
private fun tilesFor(d: AppData, meal: Meal?): List<Food> {
    val mine = d.entries.filter { meal == null || it.meal == meal }.takeLast(400).groupingBy { it.foodId }.eachCount()
        .entries.sortedByDescending { it.value }.map { it.key }
    val defaults = if (meal == null) TILES.values.flatten() else TILES.getValue(meal)
    return (mine + defaults).distinct().mapNotNull(FoodDb::get).take(15)
}

private fun step(item: NewItem): Double = if (item.unit == "g" || item.unit == "ml") 10.0 else if (item.unit == "piece" || item.unit == "egg" || item.unit == "slice") 1.0 else 0.5

// Plate items are saved as JSON (ComboItem is @Serializable and holds the same fields), so a half-built plate
// survives rotation and process death.
private val ItemsJson = ListSerializer(ComboItem.serializer())
private fun encodeItems(items: List<NewItem>): String = Json.encodeToString(ItemsJson, items.map { it.toCombo() })
private fun decodeItems(s: String): List<NewItem>? = runCatching { Json.decodeFromString(ItemsJson, s) }.getOrNull()?.map { it.toNew() }
private val PlateItemsSaver: Saver<SnapshotStateList<NewItem>, String> = Saver(save = { encodeItems(it) }, restore = { decodeItems(it)?.toMutableStateList() })
private val ItemListSaver: Saver<List<NewItem>, String> = Saver(save = { encodeItems(it) }, restore = { decodeItems(it) })

/** Below this height (landscape phones) the footer collapses to one row and the secondary "Save combo" hides. */
private val CompactFooterHeight = 480.dp

/** Target tile width: the quick-pick grid fits as many 110 dp columns as it can (3 to 6). */
private val TileMinWidth = 110.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlateBuilderScreen(page: Page.Plate) {
    val c = U.c
    val nav = nav
    val reducedMotion = LocalReducedMotion.current
    val data = LocalData.current
    val store = nav.store
    val existing = page.comboId?.let { id -> data.combos.firstOrNull { it.id == id } }
    val comboMode = page.asCombo || existing != null
    val items = rememberSaveable(saver = PlateItemsSaver) { mutableStateListOf<NewItem>().apply { existing?.items?.forEach { add(it.toNew()) } } }
    // what "unchanged" means: the combo being edited, an empty plate, or the plate as last saved as a combo
    var baseline by rememberSaveable(stateSaver = ItemListSaver) { mutableStateOf(existing?.items?.map { it.toNew() }.orEmpty()) }
    var mealName by rememberSaveable { mutableStateOf((page.meal ?: existing?.meal)?.name) }
    val meal = mealName?.let { nm -> Meal.entries.firstOrNull { it.name == nm } }
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val res = LocalResources.current
    var cheat by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    var addingId by rememberSaveable { mutableStateOf<String?>(null) }
    val adding = addingId?.let(FoodDb::get)
    var naming by rememberSaveable { mutableStateOf(false) }
    var askDiscard by rememberSaveable { mutableStateOf(false) }

    fun newItemFor(f: Food): NewItem {
        val p = data.portions[f.id]
        return NewItem(f.id, p?.qty ?: f.defaultQty, p?.unit?.takeIf { u -> f.units.any { it.name == u } } ?: f.units.first().name, p?.choices ?: emptyMap())
    }
    fun tap(f: Food) {
        val i = items.indexOfFirst { it.foodId == f.id }
        if (i >= 0) items[i] = items[i].copy(qty = items[i].qty + step(items[i])) else items.add(newItemFor(f))
    }
    val longDetails = stringResource(R.string.f_hold_details)
    val total = items.fold(Nutr()) { acc, it -> acc + (FoodDb.get(it.foodId)?.nutrFor(it.qty, it.unit, it.choices) ?: Nutr()) }
    val kcalShown by animateIntAsState(total.kcal.roundToInt(), motionTween(Motion.LONG), label = "plateKcal")
    var logged by remember { mutableStateOf(false) }
    LaunchedEffect(logged) { if (logged) { delay(if (reducedMotion) 250L else 600L); nav.pop() } }

    // back (system gesture or the arrow) asks before throwing away a plate that hasn't been logged or saved
    val dirty = !logged && items.toList() != baseline
    BackHandler(enabled = dirty) { askDiscard = true }
    fun back() {
        if (dirty) askDiscard = true else nav.pop()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < CompactFooterHeight
        Column(Modifier.fillMaxSize()) {
            TopBar(
                stringResource(
                    when {
                        !comboMode -> R.string.r7a_plate_title
                        existing != null -> R.string.r7a_combo_edit
                        else -> R.string.r7a_combo_new
                    },
                ),
                onBack = { back() },
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter)) {
                if (!comboMode) {
                    MealPicker(meal) { mealName = it.name }
                    Spacer(Modifier.height(10.dp))
                }
                SearchField(query, stringResource(R.string.r7a_search_hint)) { query = it }

                if (query.isNotBlank()) {
                    val results = FoodDb.search(query)
                    if (results.isEmpty()) {
                        EmptyState(
                            "🔍", stringResource(R.string.r7a_no_match_title, query.trim()), stringResource(R.string.r7a_no_match_body),
                            stringResource(R.string.r7a_add_as_new_food),
                        ) {
                            nav.push(Page.MyFood(null, query.trim().replaceFirstChar { it.uppercase() }))
                        }
                    } else {
                        SectionTitle(stringResource(R.string.r7a_results_title), hint = stringResource(R.string.r7a_results_hint))
                        AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                            results.forEach { f -> ResultRow(f, longDetails, onClick = { addingId = f.id }, onLong = { nav.push(Page.FoodDetail(f.id, nav.date.value, meal)) }) }
                        }
                    }
                } else {
                    SectionTitle(stringResource(R.string.r7a_quick_picks), hint = stringResource(R.string.r7a_quick_picks_hint))
                    TileGrid(tilesFor(data, meal), items, onTap = { tap(it) }, onLong = { addingId = it.id })
                    val sides = items.flatMap { PAIRINGS[it.foodId].orEmpty() }.distinct().filter { s -> items.none { it.foodId == s } }.mapNotNull(FoodDb::get)
                    if (sides.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.r7a_goes_well))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sides.take(14).forEach { s -> ChoiceChip("${s.emoji} ${s.name}", false) { tap(s) } }
                        }
                    }
                }

                SectionTitle(
                    stringResource(R.string.r7a_on_plate),
                    hint = stringResource(if (items.isEmpty()) R.string.r7a_plate_empty_hint else R.string.r7a_on_plate_hint),
                )
                if (items.isNotEmpty()) {
                    AppCard(modifier = Modifier.animateContentSize(motionTween()), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                        items.forEachIndexed { i, it ->
                            val f = FoodDb.get(it.foodId) ?: return@forEachIndexed
                            PlateRow(
                                f, it,
                                onEdit = { editing = i },
                                onRemove = { items.removeAt(i) },
                                onQty = { q -> items[i] = it.copy(qty = q) },
                            )
                        }
                    }
                    MacroTiles(total, Modifier.padding(top = 12.dp))
                }
                if (!comboMode && items.isNotEmpty()) Row(Modifier.padding(top = 10.dp)) { ChoiceChip(stringResource(R.string.f_cheat_meal), cheat) { cheat = !cheat } }
                Spacer(Modifier.height(20.dp))
            }

            // sticky footer: total + actions; one compact row when the window is short (landscape)
            val target = meal
            val primaryLabel = when {
                comboMode -> stringResource(R.string.r7a_save_combo)
                target == null -> stringResource(R.string.r7a_pick_a_meal)
                else -> stringResource(R.string.r7a_log_meal, target.label.lowercase())
            }
            val primaryEnabled = items.isNotEmpty() && (comboMode || target != null)
            val primary: @Composable () -> Unit = {
                AddButton(primaryLabel, primaryEnabled, success = logged) {
                    if (comboMode) { naming = true; return@AddButton }
                    if (target == null) return@AddButton
                    val before = store.data.value
                    store.logItems(items.toList(), target, nav.date.value, cheat)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    feedback?.show(
                        res.getString(R.string.f_added_to, items.mapNotNull { FoodDb.get(it.foodId)?.name }.joinToString(", "), target.label),
                        res.getString(R.string.f_undo),
                    ) { store.restoreSnapshot(before) }
                    logged = true
                }
            }
            val totalLabel = stringResource(R.string.r7a_plate_total)
            val totalValue = stringResource(R.string.r7a_kcal, fmtInt(kcalShown))
            Column(Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(horizontal = Space.gutter, vertical = 12.dp)) {
                if (compact) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                            Text(totalLabel, style = U.t.caption, color = c.ink2)
                            Text(totalValue, fontWeight = FontWeight.ExtraBold, style = U.t.title, maxLines = 1)
                        }
                        Box(Modifier.weight(1.4f)) { primary() }
                    }
                } else {
                    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        Text(totalLabel, color = c.ink2, modifier = Modifier.weight(1f))
                        Text(totalValue, fontWeight = FontWeight.ExtraBold, style = U.t.title)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!comboMode) {
                            Box(Modifier.weight(1f)) { SmallSecondary(stringResource(R.string.r7a_save_combo), enabled = items.isNotEmpty()) { naming = true } }
                            Box(Modifier.weight(1.6f)) { primary() }
                        } else {
                            Box(Modifier.weight(1f)) { primary() }
                        }
                    }
                }
            }
        }
    }

    adding?.let { f ->
        CustomiseSheet(f, items.firstOrNull { it.foodId == f.id } ?: newItemFor(f), saveLabel = stringResource(R.string.r7a_add_to_plate), onDismiss = { addingId = null }) { ni ->
            val i = items.indexOfFirst { it.foodId == f.id }
            if (i >= 0) items[i] = ni else items.add(ni)
            addingId = null; query = ""
        }
    }
    editing?.let { i ->
        val it = items.getOrNull(i)
        val f = it?.let { x -> FoodDb.get(x.foodId) }
        if (it != null && f != null) {
            CustomiseSheet(f, it, onDismiss = { editing = null }, onRemove = { items.removeAt(i); editing = null }) { ni -> items[i] = ni; editing = null }
        }
    }
    if (naming) {
        val defaultName = meal?.let { stringResource(R.string.r7a_combo_default_name, it.label.lowercase()) } ?: ""
        InputDialog(
            stringResource(if (existing != null) R.string.r7a_rename_combo else R.string.r7a_save_as_combo),
            stringResource(R.string.r7a_combo_name_label),
            existing?.name ?: defaultName,
            onDismiss = { naming = false },
        ) { name ->
            val combo = Combo(
                id = existing?.id ?: newKey("c_"), name = name.trim(), emoji = meal?.emoji ?: existing?.emoji ?: "⭐", meal = meal ?: existing?.meal,
                items = items.map { it.toCombo() }, uses = existing?.uses ?: 0, order = existing?.order ?: 0,
            )
            store.saveCombo(combo)
            if (comboMode) {
                baseline = items.toList()
                nav.pop()
            } else {
                // the plate is now saved as a combo: leaving no longer loses anything, but it can still be logged
                baseline = items.toList()
                feedback?.show(res.getString(R.string.r7a_combo_saved, combo.name))
            }
        }
    }
    if (askDiscard) {
        ConfirmDialog(
            title = stringResource(if (comboMode) R.string.r7a_discard_combo_title else R.string.r7a_discard_plate_title),
            text = if (comboMode) stringResource(R.string.r7a_discard_combo_body)
            else pluralStringResource(R.plurals.r7a_discard_plate_body, items.size, items.size),
            confirm = stringResource(R.string.r7a_discard),
            destructive = true,
            onDismiss = { askDiscard = false },
            dismissLabel = stringResource(R.string.r7a_keep_editing),
        ) { nav.pop() }
    }
}

/** One search result: tap opens the customise sheet, hold opens Food detail. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(f: Food, longLabel: String, onClick: () -> Unit, onLong: () -> Unit) {
    val c = U.c
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) { role = Role.Button }
            .combinedClickable(onLongClickLabel = longLabel, onClick = onClick, onLongClick = onLong).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Emoji(f.emoji)
        Column(Modifier.weight(1f)) {
            Text(f.name, fontWeight = FontWeight.Bold, style = U.t.bodyS, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val u = f.units.first()
                Text(stringResource(R.string.r7a_kcal_per_unit, fmtInt(f.nutrFor(1.0, u.name).kcal), u.name), style = U.t.caption, color = c.ink2)
                StatusBadge(f.source.label.substringBefore(" ·"), if (f.category.startsWith("Ingredient")) Status.LOW else Status.INFO)
            }
        }
        Text("+", style = U.t.title, fontWeight = FontWeight.Bold, color = c.leafInk, modifier = Modifier.clearAndSetSemantics {})
    }
}

/** Quick-pick tiles in an adaptive grid: as many ~110 dp columns as fit (3 on a phone, more in landscape). */
@Composable
private fun TileGrid(tiles: List<Food>, items: List<NewItem>, onTap: (Food) -> Unit, onLong: (Food) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cols = (maxWidth / TileMinWidth).toInt().coerceIn(3, 6)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tiles.chunked(cols).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { f ->
                        val count = items.firstOrNull { it.foodId == f.id }?.qty
                        Tile(f, count, Modifier.weight(1f), onClick = { onTap(f) }, onLong = { onLong(f) })
                    }
                    // keep the last row on the same column grid instead of stretching its tiles
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A plate line: name and "2 pieces · 130 kcal", then the shared stepper for the amount ("−" at the minimum removes it). */
@Composable
private fun PlateRow(f: Food, item: NewItem, onEdit: () -> Unit, onRemove: () -> Unit, onQty: (Double) -> Unit) {
    val c = U.c
    val s = step(item)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics(mergeDescendants = true) { role = Role.Button }.clickable(onClick = onEdit).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Emoji(f.emoji)
        Column(Modifier.weight(1f)) {
            Text(f.name, fontWeight = FontWeight.Bold, style = U.t.bodyS, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.r7a_qty_kcal, fmtQty(item.qty), unitLabel(item.unit, item.qty), fmtInt(f.nutrFor(item.qty, item.unit, item.choices).kcal)),
                style = U.t.caption, color = c.ink2,
            )
            f.choiceSummary(item.choices).takeIf { it.isNotBlank() }?.let { Text(it, style = U.t.caption, color = c.leafInk, fontWeight = FontWeight.SemiBold) }
        }
        // min = half a step: "−" never leaves a zero amount, it removes the item instead
        Stepper(item.qty, s, min = s / 2, itemName = f.name, onBelowMin = onRemove, onChange = onQty)
    }
}

/** Protein / carbs / fat / fibre totals for the plate, same number format as everywhere else ([fmtNum]). */
@Composable
private fun MacroTiles(total: Nutr, modifier: Modifier = Modifier) {
    val c = U.c
    val macros = listOf(
        stringResource(R.string.d_protein) to total.protein,
        stringResource(R.string.d_carbs) to total.carbs,
        stringResource(R.string.d_fat) to total.fat,
        stringResource(R.string.r7a_fibre) to total.fibre,
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        macros.forEachIndexed { i, (name, v) ->
            val desc = stringResource(R.string.r7a_macro_desc, name, fmtNum(v))
            Column(
                Modifier.weight(1f).edge(c.line, 12.dp, 1.dp, seed = i + 20).clip(RoundedCornerShape(12.dp)).background(c.sf)
                    .clearAndSetSemantics { contentDescription = desc }.padding(8.dp),
            ) {
                Text(stringResource(R.string.r7a_grams, fmtNum(v)), fontWeight = FontWeight.Bold, maxLines = 1)
                Text(name, style = U.t.caption, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(f: Food, count: Double?, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val c = U.c
    val on = count != null
    val longLabel = stringResource(R.string.f_hold_customise)
    val qtyDesc = stringResource(R.string.f_in_plate_qty, count?.let { fmtQty(it) } ?: "")
    val u = f.units.first()
    val kcal = fmtInt(f.nutrFor(1.0, u.name).kcal)
    val perUnitDesc = stringResource(R.string.r7a_tile_kcal_desc, kcal, u.name)
    val line by animateColorAsState(if (on) c.acc else c.line, motionTween(), label = "tileLine")
    val fill by animateColorAsState(if (on) c.accS else c.sf, motionTween(), label = "tileFill")
    val disc by animateColorAsState(if (on) c.sf else c.turS, motionTween(), label = "tileDisc")
    val badge by animateFloatAsState(
        if (on) 1f else 0f,
        motionSpring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "tileBadge",
    )
    var lastCount by remember { mutableStateOf(count) }
    if (count != null) lastCount = count
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().pressScale().edge(line, 14.dp, 1.5.dp, seed = f.id.length).clip(RoundedCornerShape(14.dp)).background(fill)
                .semantics(mergeDescendants = true) { role = Role.Button; if (count != null) stateDescription = qtyDesc }
                .combinedClickable(onLongClickLabel = longLabel, onClick = onClick, onLongClick = onLong).heightIn(min = 48.dp).padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(disc), contentAlignment = Alignment.Center) { EmojiText(f.emoji, 24) }
            Spacer(Modifier.height(6.dp))
            Text(f.name.substringBefore(" ("), fontWeight = FontWeight.Bold, style = U.t.caption, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.r7a_tile_kcal_per_unit, kcal, u.name), style = U.t.caption, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { contentDescription = perUnitDesc },
            )
        }
        val shownCount = lastCount
        if (badge > 0.01f && shownCount != null) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).graphicsLayer { scaleX = badge; scaleY = badge; alpha = badge.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(50)).background(c.acc).padding(horizontal = 7.dp, vertical = 2.dp).clearAndSetSemantics {},
            ) {
                Text(fmtQty(shownCount), color = c.onAcc, style = U.t.caption, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun Emoji(e: String, size: Int = 42) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape((size * 0.3).dp)).background(U.c.turS).padding(3.dp), contentAlignment = Alignment.Center) {
        EmojiText(e, (size * 0.5).toInt())
    }
}

/** Secondary action in a footer: same height and corner as the main one, sketch outline in the Sketch style. */
@Composable
fun SmallSecondary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = U.c
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(if (U.sketch) Modifier.edge(c.ink.copy(alpha = if (enabled) 0.6f else 0.3f), 16.dp, 1.4.dp, seed = 7) else Modifier)
            .then(if (enabled) Modifier.pressScale() else Modifier)
            .clip(RoundedCornerShape(16.dp)).background(c.sf2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontWeight = FontWeight.Bold, style = U.t.bodyS, color = if (enabled) c.ink else c.ink2.copy(alpha = 0.6f),
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/**
 * Section header shared by the plate / meals / foods screens. [hint] adds a one-line instruction under it in caption
 * style (not part of the heading), so how-to text never reads as a title.
 */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier, hint: String? = null) {
    if (hint == null) {
        Text(text, style = U.t.title, modifier = modifier.padding(top = 16.dp, bottom = 8.dp).semantics { heading() })
    } else {
        Column(modifier.padding(top = 16.dp, bottom = 8.dp)) {
            Text(text, style = U.t.title, modifier = Modifier.semantics { heading() })
            Text(hint, style = U.t.caption, color = U.c.ink2, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
