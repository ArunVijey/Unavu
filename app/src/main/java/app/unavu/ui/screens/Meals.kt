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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.Combo
import app.unavu.data.CustomFood
import app.unavu.data.FoodDb
import app.unavu.data.Ingredient
import app.unavu.data.Meal
import app.unavu.data.Nutr
import app.unavu.data.deleteCombo
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.nutrFor
import app.unavu.data.saveCombo
import app.unavu.data.saveCustomFood
import app.unavu.data.useCombo
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Divider
import app.unavu.ui.Fab
import app.unavu.ui.LocalData
import app.unavu.ui.MealPicker
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SearchField
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TextField
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var filter by rememberSaveable { mutableStateOf<Meal?>(null) }
    var logCombo by remember { mutableStateOf<Combo?>(null) }
    var deleteCombo by remember { mutableStateOf<Combo?>(null) }
    var deleteRecipe by remember { mutableStateOf<CustomFood?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopBar("Meals")
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Segmented(listOf("Saved combos", "My recipes"), tab) { tab = it }
                if (tab == 0) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("All", filter == null) { filter = null }
                        Meal.entries.forEach { m -> ChoiceChip(m.label, filter == m) { filter = m } }
                    }
                    val combos = data.combos.filter { filter == null || it.meal == filter }.sortedWith(compareBy<Combo> { it.order }.thenByDescending { it.uses })
                    if (combos.isEmpty()) {
                        AppCard {
                            Text("No combos yet", fontWeight = FontWeight.Bold)
                            Text("Save a plate you eat often (from a meal's popup on Today, or tap + here) and log it with one tap next time.", fontSize = 12.5.sp, color = c.ink2)
                        }
                    }
                    combos.forEachIndexed { idx, cb ->
                        var menu by remember(cb.id) { mutableStateOf(false) }
                        val kcal = cb.items.sumOf { FoodDb.get(it.foodId)?.nutrFor(it.qty, it.unit, it.choices)?.kcal ?: 0.0 }.roundToInt()
                        AppCard(onClick = { logCombo = cb }, padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Emoji(cb.emoji)
                                Column(Modifier.weight(1f)) {
                                    Text(cb.name, fontWeight = FontWeight.Bold)
                                    Text(cb.items.joinToString(" · ") { "${fmtQty(it.qty)} ${FoodDb.get(it.foodId)?.name ?: "?"}" }, fontSize = 11.sp, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        cb.meal?.let { StatusBadge(it.label, Status.OK) }
                                        Text("used ${cb.uses}×", fontSize = 10.5.sp, color = c.ink2)
                                    }
                                }
                                Text("$kcal", fontWeight = FontWeight.ExtraBold)
                                Box {
                                    Text("⋮", fontSize = 18.sp, modifier = Modifier.clip(CircleShape).clickable { menu = true }.padding(horizontal = 8.dp, vertical = 2.dp))
                                    DropdownMenu(menu, { menu = false }, containerColor = c.sf) {
                                        DropdownMenuItem({ Text("✏️ Edit") }, { menu = false; nav.push(Page.Plate(cb.meal, nav.date.value, comboId = cb.id)) })
                                        if (idx > 0) DropdownMenuItem({ Text("↑ Move up") }, { menu = false; swap(nav.store, combos, idx, idx - 1) })
                                        if (idx < combos.lastIndex) DropdownMenuItem({ Text("↓ Move down") }, { menu = false; swap(nav.store, combos, idx, idx + 1) })
                                        DropdownMenuItem({ Text("🗑 Delete", color = c.chi) }, { menu = false; deleteCombo = cb })
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val recipes = data.customFoods.filter { it.facts == null }
                    app.unavu.ui.SecondaryButton("＋ Add a food by its calories / protein / fat") { nav.push(Page.MyFood(null)) }
                    if (recipes.isEmpty()) {
                        AppCard {
                            Text("No recipes yet", fontWeight = FontWeight.Bold)
                            Text("Build a dish from its ingredients (e.g. Amma's puli kuzhambu). It's calculated from IFCT values and appears in search and typing like any other food.", fontSize = 12.5.sp, color = c.ink2)
                        }
                    }
                    recipes.forEach { r ->
                        val f = FoodDb.get(r.id)
                        val per = f?.nutrFor(1.0, r.unitName)
                        AppCard(onClick = { nav.push(Page.Recipe(r.id)) }, padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Emoji(r.emoji)
                                Column(Modifier.weight(1f)) {
                                    Text(r.name, fontWeight = FontWeight.Bold)
                                    Text("${r.ing.size} ingredients · 1 ${r.unitName} = ${r.unitG.roundToInt()} ${if (r.liquid) "ml" else "g"}", fontSize = 11.sp, color = c.ink2)
                                }
                                Text("${per?.kcal?.roundToInt() ?: 0} kcal", fontWeight = FontWeight.ExtraBold)
                                Text("🗑", modifier = Modifier.clip(CircleShape).clickable { deleteRecipe = r }.padding(6.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(90.dp))
            }
        }
        Fab(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            if (tab == 0) nav.push(Page.Plate(filter, nav.date.value, asCombo = true)) else nav.push(Page.Recipe(null))
        }
    }

    logCombo?.let { cb ->
        var meal by remember { mutableStateOf(cb.meal) }
        AlertDialog(
            onDismissRequest = { logCombo = null }, containerColor = c.sf,
            title = { Text("Log “${cb.name}”", fontWeight = FontWeight.Bold) },
            text = { Column { Text("Which meal?", color = c.ink2, fontSize = 12.sp); Spacer(Modifier.height(6.dp)); MealPicker(meal) { meal = it } } },
            confirmButton = {
                TextButton({ nav.store.useCombo(cb, meal!!, nav.date.value); logCombo = null; nav.home() }, enabled = meal != null) { Text("Log", color = c.accInk, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ logCombo = null }) { Text("Cancel", color = c.ink2) } },
        )
    }
    deleteCombo?.let { cb -> ConfirmDialog("Delete combo?", "“${cb.name}” will be removed. Logged meals stay.", "Delete", true, { deleteCombo = null }) { nav.store.deleteCombo(cb.id) } }
    deleteRecipe?.let { r ->
        val used = data.entries.any { it.foodId == r.id }
        ConfirmDialog("Delete recipe?", if (used) "“${r.name}” is used in your log. Those entries will show as unknown." else "“${r.name}” will be removed.", "Delete", true, { deleteRecipe = null }) {
            nav.store.deleteCustomFood(r.id)
        }
    }
}

private fun swap(store: app.unavu.data.Store, list: List<Combo>, a: Int, b: Int) {
    val reordered = list.toMutableList().apply { add(b, removeAt(a)) }
    store.update { d -> d.copy(combos = d.combos.map { cb -> reordered.indexOfFirst { it.id == cb.id }.takeIf { it >= 0 }?.let { cb.copy(order = it) } ?: cb }) }
}

/** Build your own dish from ingredients. */
@Composable
fun RecipeEditorScreen(page: Page.Recipe) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val existing = page.id?.let { id -> data.customFoods.firstOrNull { it.id == id } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🍲") }
    val ing = remember { mutableStateListOf<Pair<String, Double>>().apply { existing?.ing?.forEach { (k, v) -> add(k to v) } } }
    var cooked by remember { mutableStateOf(existing?.yieldG?.let { fmtQty(it) } ?: "") }
    var unitName by remember { mutableStateOf(existing?.unitName ?: "ladle") }
    var unitG by remember { mutableStateOf(existing?.unitG?.let { fmtQty(it) } ?: "100") }
    var liquid by remember { mutableStateOf(existing?.liquid ?: false) }
    var query by remember { mutableStateOf("") }
    var gramsFor by remember { mutableStateOf<Ingredient?>(null) }

    val rawTotal = ing.sumOf { it.second }
    val yieldG = cooked.toDoubleOrNull() ?: rawTotal
    val batch = ing.fold(Nutr()) { acc, (k, g) -> acc + (FoodDb.ingredient(k)?.per100 ?: Nutr()) * (g / 100.0) }
    val perUnit = if (yieldG > 0) batch * ((unitG.toDoubleOrNull() ?: 0.0) / yieldG) else Nutr()
    val servings = if ((unitG.toDoubleOrNull() ?: 0.0) > 0) yieldG / unitG.toDouble() else 0.0

    Column(Modifier.fillMaxSize()) {
        TopBar(if (existing == null) "New recipe" else "Edit recipe", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (existing == null) {
                AppCard(color = c.turS, border = null, onClick = { nav.pop(); nav.push(Page.MyFood(null, name)) }) {
                    Text("Know the calories, protein and fat already?", fontWeight = FontWeight.Bold, color = c.turInk)
                    Text("From a pack label, a menu or a guess: tap here to type the values in directly instead of building from ingredients.", fontSize = 12.5.sp, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(emoji, "Icon", Modifier.width(70.dp)) { emoji = it.take(2) }
                TextField(name, "Recipe name, e.g. Amma's puli kuzhambu", Modifier.weight(1f)) { name = it }
            }
            SectionLabel("Ingredients (raw weight) · from IFCT 2017")
            if (ing.isNotEmpty()) {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    ing.forEachIndexed { i, (k, g) ->
                        if (i > 0) Divider()
                        val ii = FoodDb.ingredient(k)
                        Row(Modifier.fillMaxWidth().clickable { gramsFor = ii }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ii?.name ?: k, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${fmtQty(g)} g", fontWeight = FontWeight.Bold)
                            Text("  ${((ii?.per100?.kcal ?: 0.0) * g / 100).roundToInt()} kcal", fontSize = 11.sp, color = c.ink2)
                            Text("  ✕", color = c.ink2, modifier = Modifier.clip(CircleShape).clickable { ing.removeAt(i) }.padding(4.dp))
                        }
                    }
                }
            }
            SearchField(query, "Add ingredient: toor dal, tamarind, oil…") { query = it }
            if (query.isNotBlank()) {
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    val res = FoodDb.searchIngredients(query)
                    if (res.isEmpty()) Text("No match", color = c.ink2, modifier = Modifier.padding(8.dp))
                    res.forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { gramsFor = r }.padding(vertical = 8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${r.per100.kcal.roundToInt()} kcal / 100 g · ${r.group}", fontSize = 11.sp, color = c.ink2)
                            }
                            Text("+", fontSize = 18.sp, color = c.leafInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            SectionLabel("After cooking")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(cooked, "Cooked total ${if (liquid) "ml" else "g"} (raw ${rawTotal.roundToInt()})", Modifier.weight(1f)) { cooked = it }
                Column(Modifier.weight(1f)) {
                    TextField(unitName, "Serving unit") { unitName = it.lowercase().take(12) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(unitG, "1 $unitName = ? ${if (liquid) "ml" else "g"}", Modifier.weight(1f)) { unitG = it }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("It's a drink", fontSize = 13.sp, modifier = Modifier.weight(1f))
                    AppSwitch(liquid) { liquid = it }
                }
            }
            Text("Tip: weigh the pot empty and full after cooking to get the cooked total.", fontSize = 11.sp, color = c.ink2)
            AppCard(color = c.leaf, border = null) {
                Text("PER ${unitName.uppercase()} · ${"%.1f".format(servings)} servings", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${perUnit.kcal.roundToInt()} kcal", color = androidx.compose.ui.graphics.Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("P ${"%.1f".format(perUnit.protein)} · C ${"%.1f".format(perUnit.carbs)} · F ${"%.1f".format(perUnit.fat)} g\nNa ${perUnit.sodium.roundToInt()} mg", color = androidx.compose.ui.graphics.Color.White, fontSize = 11.5.sp)
                }
            }
            PrimaryButton("Save recipe", enabled = name.isNotBlank() && ing.isNotEmpty() && yieldG > 0 && (unitG.toDoubleOrNull() ?: 0.0) > 0) {
                nav.store.saveCustomFood(
                    CustomFood(existing?.id ?: newKey("u_"), name.trim(), emoji.ifBlank { "🍲" }, ing.groupBy { it.first }.mapValues { (_, v) -> v.sumOf { it.second } }, yieldG, unitName.ifBlank { "serving" }, unitG.toDouble(), liquid),
                )
                nav.pop()
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    gramsFor?.let { ii ->
        var g by remember { mutableStateOf(ing.firstOrNull { it.first == ii.id }?.second?.let { fmtQty(it) } ?: "") }
        AlertDialog(
            onDismissRequest = { gramsFor = null }, containerColor = c.sf,
            title = { Text(ii.name, fontWeight = FontWeight.Bold) },
            text = { NumberField(g, "Raw grams in the whole batch") { g = it } },
            confirmButton = {
                TextButton({
                    val v = g.toDoubleOrNull() ?: 0.0
                    val i = ing.indexOfFirst { it.first == ii.id }
                    if (v > 0) { if (i >= 0) ing[i] = ii.id to v else ing.add(ii.id to v) }
                    gramsFor = null; query = ""
                }) { Text("Save", color = c.accInk, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ gramsFor = null }) { Text("Cancel", color = c.ink2) } },
        )
    }
}
