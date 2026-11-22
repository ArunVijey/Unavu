package app.unavu.ui.screens

import app.unavu.ui.theme.edge
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.pressScale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import app.unavu.ui.EmojiText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.AppData
import app.unavu.data.Combo
import app.unavu.data.Food
import app.unavu.data.FoodDb
import app.unavu.data.Meal
import app.unavu.data.NewItem
import app.unavu.data.Nutr
import app.unavu.data.fmtQty
import app.unavu.data.logItems
import app.unavu.data.newKey
import app.unavu.data.nutrFor
import app.unavu.data.saveCombo
import app.unavu.data.toCombo
import app.unavu.data.toNew
import app.unavu.data.unitLabel
import app.unavu.ui.AppCard
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.MealPicker
import app.unavu.ui.Page
import app.unavu.ui.SearchField
import app.unavu.ui.SecondaryButton
import app.unavu.ui.EmptyState
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun PlateBuilderScreen(page: Page.Plate) {
    val c = U.c
    val nav = nav
    val reducedMotion = LocalReducedMotion.current
    val data = LocalData.current
    val store = nav.store
    val existing = page.comboId?.let { id -> data.combos.firstOrNull { it.id == id } }
    val comboMode = page.asCombo || existing != null
    val items = remember { mutableStateListOf<NewItem>().apply { existing?.items?.forEach { add(it.toNew()) } } }
    var mealName by rememberSaveable { mutableStateOf((page.meal ?: existing?.meal)?.name) }
    val meal = mealName?.let { nm -> Meal.entries.firstOrNull { it.name == nm } }
    val feedback = app.unavu.ui.LocalFeedback.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalResources.current
    var cheat by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    var addingId by rememberSaveable { mutableStateOf<String?>(null) }
    val adding = addingId?.let(FoodDb::get)
    var naming by rememberSaveable { mutableStateOf(false) }

    fun newItemFor(f: Food): NewItem {
        val p = data.portions[f.id]
        return NewItem(f.id, p?.qty ?: f.defaultQty, p?.unit?.takeIf { u -> f.units.any { it.name == u } } ?: f.units.first().name, p?.choices ?: emptyMap())
    }
    fun tap(f: Food) {
        val i = items.indexOfFirst { it.foodId == f.id }
        if (i >= 0) items[i] = items[i].copy(qty = items[i].qty + step(items[i])) else items.add(newItemFor(f))
    }
    val longDetails = stringResource(app.unavu.R.string.f_hold_details)
    val total = items.fold(Nutr()) { acc, it -> acc + (FoodDb.get(it.foodId)?.nutrFor(it.qty, it.unit, it.choices) ?: Nutr()) }
    val kcalShown by animateIntAsState(total.kcal.roundToInt(), motionTween(Motion.LONG), label = "plateKcal")
    var logged by remember { mutableStateOf(false) }
    LaunchedEffect(logged) { if (logged) { kotlinx.coroutines.delay(if (reducedMotion) 250L else 600L); nav.pop() } }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (comboMode) (existing?.let { "Edit combo" } ?: "New combo") else "Build a plate", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (!comboMode) {
                MealPicker(meal) { mealName = it.name }
                Spacer(Modifier.height(10.dp))
            }
            SearchField(query, "Search any food: kuzhambu, முட்டை, pizza…") { query = it }

            if (query.isNotBlank()) {
                val results = FoodDb.search(query)
                if (results.isNotEmpty()) SectionTitle("Results · tap to add, hold for details")
                if (results.isEmpty()) {
                    EmptyState("🔍", "No match for “${query.trim()}”", "Try another spelling, or save it as your own food.", "＋ Add as a new food") {
                        nav.push(Page.MyFood(null, query.trim().replaceFirstChar { it.uppercase() }))
                    }
                } else AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    results.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) { role = Role.Button }.combinedClickable(onClickLabel = null, onLongClickLabel = longDetails, onClick = { addingId = f.id }, onLongClick = { nav.push(Page.FoodDetail(f.id, nav.date.value, meal)) }).padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Emoji(f.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.Bold, style = U.t.bodyS, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val u = f.units.first()
                                    Text("${f.nutrFor(1.0, u.name).kcal.roundToInt()} kcal / ${u.name}", style = U.t.caption, color = c.ink2)
                                    StatusBadge(f.source.label.substringBefore(" ·"), if (f.category.startsWith("Ingredient")) Status.LOW else Status.INFO)
                                }
                            }
                            Text("+", style = U.t.title, fontWeight = FontWeight.Bold, color = c.leafInk)
                        }
                    }
                }
            } else {
                SectionTitle("Tap to add · tap again for more · hold to customise")
                val tiles = tilesFor(data, meal)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                    tiles.forEach { f ->
                        val count = items.firstOrNull { it.foodId == f.id }?.qty
                        Tile(f, count, Modifier.weight(1f), onClick = { tap(f) }, onLong = { addingId = f.id })
                    }
                }
                val sides = items.flatMap { PAIRINGS[it.foodId].orEmpty() }.distinct().filter { s -> items.none { it.foodId == s } }.mapNotNull(FoodDb::get)
                if (sides.isNotEmpty()) {
                    SectionTitle("Goes well with")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sides.take(14).forEach { s -> ChoiceChip("${s.emoji} ${s.name}", false) { tap(s) } }
                    }
                }
            }

            SectionTitle(if (items.isEmpty()) "Plate is empty" else "On the plate · tap to customise")
            if (items.isNotEmpty()) {
                AppCard(modifier = Modifier.animateContentSize(), padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items.forEachIndexed { i, it ->
                        val f = FoodDb.get(it.foodId) ?: return@forEachIndexed
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).pressScale().semantics(mergeDescendants = true) { role = Role.Button }.clickable { editing = i }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Emoji(f.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.Bold, style = U.t.bodyS)
                                Text("${fmtQty(it.qty)} ${unitLabel(it.unit, it.qty)}", style = U.t.caption, color = c.ink2)
                                f.choiceSummary(it.choices).takeIf { s -> s.isNotBlank() }?.let { s -> Text(s, style = U.t.caption, color = c.leafInk, fontWeight = FontWeight.SemiBold) }
                            }
                            MiniStep("−", stringResource(app.unavu.R.string.f_decrease, f.name)) { if (it.qty - step(it) <= 0) items.removeAt(i) else items[i] = it.copy(qty = it.qty - step(it)) }
                            Text("${f.nutrFor(it.qty, it.unit, it.choices).kcal.roundToInt()}", fontWeight = FontWeight.ExtraBold, style = U.t.bodyS)
                            MiniStep("+", stringResource(app.unavu.R.string.f_increase, f.name)) { items[i] = it.copy(qty = it.qty + step(it)) }
                        }
                    }
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Protein" to total.protein, "Carbs" to total.carbs, "Fat" to total.fat, "Fibre" to total.fibre).forEach { (n, v) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.sf).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(8.dp)) {
                            Text("${v.roundToInt()} g", fontWeight = FontWeight.Bold)
                            Text(n, style = U.t.caption, color = c.ink2)
                        }
                    }
                }
            }
            if (!comboMode && items.isNotEmpty()) Row(Modifier.padding(top = 10.dp)) { ChoiceChip("🍕 Cheat meal", cheat) { cheat = !cheat } }
            Spacer(Modifier.height(20.dp))
        }
        // bottom bar
        Column(Modifier.fillMaxWidth().background(c.sf).navigationBarsPadding().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Plate total", color = c.ink2, modifier = Modifier.weight(1f))
                Text("$kcalShown kcal", fontWeight = FontWeight.ExtraBold, style = U.t.title)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (comboMode) {
                    AddBtn("⭐ SAVE COMBO", items.isNotEmpty(), Modifier.weight(1f)) { naming = true }
                } else {
                    Box(Modifier.weight(1f)) { SmallSecondary("⭐ Save") { naming = true } }
                    Box(Modifier.weight(1.6f)) {
                        app.unavu.ui.screens.AddButton(if (meal == null) "PICK A MEAL" else "LOG ${meal!!.label.uppercase()}", items.isNotEmpty() && meal != null, success = logged) {
                            val before = store.data.value
                            store.logItems(items.toList(), meal!!, nav.date.value, cheat)
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                            feedback?.show(ctx.getString(app.unavu.R.string.f_added_to, items.mapNotNull { FoodDb.get(it.foodId)?.name }.joinToString(", "), meal.label), ctx.getString(app.unavu.R.string.f_undo)) { store.restoreSnapshot(before) }
                            logged = true
                        }
                    }
                }
            }
        }
    }

    adding?.let { f ->
        CustomiseSheet(f, items.firstOrNull { it.foodId == f.id } ?: newItemFor(f), saveLabel = "Add to plate", onDismiss = { addingId = null }) { ni ->
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
        InputDialog(if (existing != null) "Rename combo" else "Save as combo", "Name, e.g. My usual lunch", existing?.name ?: (meal?.let { "My usual ${it.label.lowercase()}" } ?: ""), onDismiss = { naming = false }) { name ->
            val combo = Combo(
                id = existing?.id ?: newKey("c_"), name = name.trim(), emoji = meal?.emoji ?: existing?.emoji ?: "⭐", meal = meal ?: existing?.meal,
                items = items.map { it.toCombo() }, uses = existing?.uses ?: 0, order = existing?.order ?: 0,
            )
            store.saveCombo(combo)
            if (comboMode) nav.pop()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(f: Food, count: Double?, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val c = U.c
    val on = count != null
    val longLabel = stringResource(app.unavu.R.string.f_hold_customise)
    val qtyDesc = stringResource(app.unavu.R.string.f_in_plate_qty, count?.let { fmtQty(it) } ?: "")
    val line by animateColorAsState(if (on) c.acc else c.line, motionTween(), label = "tileLine")
    val fill by animateColorAsState(if (on) c.accS else c.sf, motionTween(), label = "tileFill")
    val disc by animateColorAsState(if (on) c.sf else c.turS, motionTween(), label = "tileDisc")
    val reduced = LocalReducedMotion.current
    val badge by androidx.compose.animation.core.animateFloatAsState(
        if (on) 1f else 0f,
        if (reduced) androidx.compose.animation.core.snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "tileBadge",
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
            val u = f.units.first()
            Text("${f.nutrFor(1.0, u.name).kcal.roundToInt()}/${u.name}", style = U.t.caption, color = c.ink2)
        }
        if (badge > 0.01f && lastCount != null) {
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).graphicsLayer { scaleX = badge; scaleY = badge; alpha = badge.coerceIn(0f, 1f) }
                    .clip(RoundedCornerShape(50)).background(c.acc).padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(fmtQty(lastCount!!), color = c.onAcc, style = U.t.caption, fontWeight = FontWeight.ExtraBold)
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

@Composable
private fun MiniStep(t: String, desc: String, onClick: () -> Unit) {
    val c = U.c
    Box(Modifier.size(48.dp).pressScale().clip(CircleShape).semantics { contentDescription = desc; role = Role.Button }.clickable(onClick = onClick).padding(10.dp).border(1.5.dp, c.line, CircleShape), contentAlignment = Alignment.Center) {
        Text(t, fontWeight = FontWeight.Bold, style = U.t.body)
    }
}

@Composable
fun AddBtn(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) c.acc else c.acc.copy(alpha = 0.35f)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.ExtraBold, color = c.onAcc, style = U.t.bodyS) }
}

@Composable
fun SmallSecondary(text: String, onClick: () -> Unit) {
    val c = U.c
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.sf2).clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, fontWeight = FontWeight.Bold, style = U.t.bodyS)
    }
}

/** Section header shared by the plate / meals / foods screens. */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = U.t.title, modifier = modifier.padding(top = 16.dp, bottom = 8.dp).semantics { heading() })
}
