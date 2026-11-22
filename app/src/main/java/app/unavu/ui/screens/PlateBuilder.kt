package app.unavu.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import app.unavu.ui.SectionLabel
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
    val data = LocalData.current
    val store = nav.store
    val existing = page.comboId?.let { id -> data.combos.firstOrNull { it.id == id } }
    val comboMode = page.asCombo || existing != null
    val items = remember { mutableStateListOf<NewItem>().apply { existing?.items?.forEach { add(it.toNew()) } } }
    var meal by remember { mutableStateOf(page.meal ?: existing?.meal) }
    var cheat by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf<Food?>(null) }
    var naming by remember { mutableStateOf(false) }

    fun newItemFor(f: Food): NewItem {
        val p = data.portions[f.id]
        return NewItem(f.id, p?.qty ?: f.defaultQty, p?.unit?.takeIf { u -> f.units.any { it.name == u } } ?: f.units.first().name, p?.choices ?: emptyMap())
    }
    fun tap(f: Food) {
        val i = items.indexOfFirst { it.foodId == f.id }
        if (i >= 0) items[i] = items[i].copy(qty = items[i].qty + step(items[i])) else items.add(newItemFor(f))
    }
    val total = items.fold(Nutr()) { acc, it -> acc + (FoodDb.get(it.foodId)?.nutrFor(it.qty, it.unit, it.choices) ?: Nutr()) }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (comboMode) (existing?.let { "Edit combo" } ?: "New combo") else "Build a plate", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            if (!comboMode) {
                MealPicker(meal) { meal = it }
                Spacer(Modifier.height(10.dp))
            }
            SearchField(query, "Search any food: kuzhambu, முட்டை, pizza…") { query = it }

            if (query.isNotBlank()) {
                val results = FoodDb.search(query)
                SectionLabel(if (results.isEmpty()) "No match. Try another spelling" else "Results · tap to add, hold for details")
                if (results.isEmpty()) {
                    SecondaryButton("＋ Add “${query.trim()}” as a new food") { nav.push(Page.MyFood(null, query.trim().replaceFirstChar { it.uppercase() })) }
                    Spacer(Modifier.height(8.dp))
                }
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    results.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().combinedClickable(onClick = { adding = f }, onLongClick = { nav.push(Page.FoodDetail(f.id, nav.date.value, meal)) }).padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Emoji(f.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val u = f.units.first()
                                    Text("${f.nutrFor(1.0, u.name).kcal.roundToInt()} kcal / ${u.name}", fontSize = 11.sp, color = c.ink2)
                                    StatusBadge(f.source.label.substringBefore(" ·"), if (f.category.startsWith("Ingredient")) Status.LOW else Status.INFO)
                                }
                            }
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.leafInk)
                        }
                    }
                }
            } else {
                SectionLabel("Tap to add · tap again for more · hold to customise")
                val tiles = tilesFor(data, meal)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp), maxItemsInEachRow = 3) {
                    tiles.forEach { f ->
                        val count = items.firstOrNull { it.foodId == f.id }?.qty
                        Tile(f, count, Modifier.weight(1f), onClick = { tap(f) }, onLong = { adding = f })
                    }
                }
                val sides = items.flatMap { PAIRINGS[it.foodId].orEmpty() }.distinct().filter { s -> items.none { it.foodId == s } }.mapNotNull(FoodDb::get)
                if (sides.isNotEmpty()) {
                    SectionLabel("Goes well with")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sides.take(14).forEach { s -> ChoiceChip("${s.emoji} ${s.name}", false) { tap(s) } }
                    }
                }
            }

            SectionLabel(if (items.isEmpty()) "Plate is empty" else "On the plate · tap to customise")
            if (items.isNotEmpty()) {
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    items.forEachIndexed { i, it ->
                        val f = FoodDb.get(it.foodId) ?: return@forEachIndexed
                        Row(Modifier.fillMaxWidth().clickable { editing = i }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Emoji(f.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text("${fmtQty(it.qty)} ${unitLabel(it.unit, it.qty)}", fontSize = 11.5.sp, color = c.ink2)
                                f.choiceSummary(it.choices).takeIf { s -> s.isNotBlank() }?.let { s -> Text(s, fontSize = 11.sp, color = c.leafInk, fontWeight = FontWeight.SemiBold) }
                            }
                            MiniStep("−") { if (it.qty - step(it) <= 0) items.removeAt(i) else items[i] = it.copy(qty = it.qty - step(it)) }
                            Text("${f.nutrFor(it.qty, it.unit, it.choices).kcal.roundToInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
                            MiniStep("+") { items[i] = it.copy(qty = it.qty + step(it)) }
                        }
                    }
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Protein" to total.protein, "Carbs" to total.carbs, "Fat" to total.fat, "Fibre" to total.fibre).forEach { (n, v) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.sf).border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(8.dp)) {
                            Text("${v.roundToInt()} g", fontWeight = FontWeight.Bold)
                            Text(n, fontSize = 10.5.sp, color = c.ink2)
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
                Text("${total.kcal.roundToInt()} kcal", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (comboMode) {
                    AddBtn("⭐ SAVE COMBO", items.isNotEmpty(), Modifier.weight(1f)) { naming = true }
                } else {
                    Box(Modifier.weight(1f)) { SmallSecondary("⭐ Save") { naming = true } }
                    AddBtn(if (meal == null) "PICK A MEAL" else "LOG ${meal!!.label.uppercase()}", items.isNotEmpty() && meal != null, Modifier.weight(1.6f)) {
                        store.logItems(items.toList(), meal!!, nav.date.value, cheat)
                        nav.pop()
                    }
                }
            }
        }
    }

    adding?.let { f ->
        CustomiseSheet(f, items.firstOrNull { it.foodId == f.id } ?: newItemFor(f), saveLabel = "Add to plate", onDismiss = { adding = null }) { ni ->
            val i = items.indexOfFirst { it.foodId == f.id }
            if (i >= 0) items[i] = ni else items.add(ni)
            adding = null; query = ""
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
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (on) c.accS else c.sf)
                .border(1.5.dp, if (on) c.acc else c.line, RoundedCornerShape(14.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLong).padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(if (on) c.sf else c.turS), contentAlignment = Alignment.Center) { EmojiText(f.emoji, 24) }
            Spacer(Modifier.height(6.dp))
            Text(f.name.substringBefore(" ("), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            val u = f.units.first()
            Text("${f.nutrFor(1.0, u.name).kcal.roundToInt()}/${u.name}", fontSize = 10.5.sp, color = c.ink2)
        }
        if (on) {
            Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-6).dp).clip(RoundedCornerShape(50)).background(c.acc).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(fmtQty(count!!), color = c.onAcc, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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
private fun MiniStep(t: String, onClick: () -> Unit) {
    val c = U.c
    Box(Modifier.size(28.dp).clip(CircleShape).border(1.5.dp, c.line, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(t, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun AddBtn(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(if (enabled) c.acc else c.acc.copy(alpha = 0.35f)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp, color = c.onAcc, fontSize = 14.sp) }
}

@Composable
fun SmallSecondary(text: String, onClick: () -> Unit) {
    val c = U.c
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.sf2).clickable(onClick = onClick).padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
