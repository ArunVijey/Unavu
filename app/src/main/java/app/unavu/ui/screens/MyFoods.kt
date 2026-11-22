package app.unavu.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.CustomFood
import app.unavu.data.Facts
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.saveCustomFood
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Fab
import app.unavu.ui.LocalData
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.TextField
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

/** Settings › My foods: anything new you eat, saved from a pack label, a menu, or your best guess. */
@Composable
fun MyFoodsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val foods = data.customFoods.filter { it.facts != null }
    val recipes = data.customFoods.filter { it.facts == null }
    var delete by remember { mutableStateOf<CustomFood?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TopBar("My foods", onBack = { nav.pop() })
            Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppCard(color = c.leafS, border = null) {
                    Text("Ate something new?", fontWeight = FontWeight.Bold, color = c.leafInk)
                    Text(
                        "Add it once with its calories (from the pack, the menu, or a guess). It then shows up in search, the plate builder and typing, like any other food.",
                        fontSize = 12.5.sp, color = c.ink2, modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("＋ Add a new food") { nav.push(Page.MyFood(null)) }
                }

                SectionLabel(if (foods.isEmpty()) "No foods added yet" else "Your foods · ${foods.size}")
                foods.sortedBy { it.name.lowercase() }.forEach { f ->
                    val x = f.facts!!
                    AppCard(onClick = { nav.push(Page.MyFood(f.id)) }, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Emoji(f.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(f.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "1 ${f.unitName} (${fmtQty(f.unitG)} ${if (f.liquid) "ml" else "g"}) · P ${fmtQty(x.protein)} · C ${fmtQty(x.carbs)} · F ${fmtQty(x.fat)}",
                                    fontSize = 12.sp, color = c.ink2,
                                )
                            }
                            Text("${x.kcal.roundToInt()} kcal", fontWeight = FontWeight.ExtraBold)
                            Text("🗑", modifier = Modifier.clip(CircleShape).clickable { delete = f }.padding(6.dp))
                        }
                    }
                }

                SectionLabel("Cooked at home?")
                AppCard(onClick = { nav.push(Page.Recipe(null)) }) {
                    Text("Build a recipe from ingredients", fontWeight = FontWeight.Bold)
                    Text(
                        "For home dishes, add the ingredients and the app works out every nutrient. You have ${recipes.size} recipe${if (recipes.size == 1) "" else "s"} (Meals › My recipes).",
                        fontSize = 12.5.sp, color = c.ink2, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.height(90.dp))
            }
        }
        Fab(Modifier.align(Alignment.BottomEnd).padding(18.dp)) { nav.push(Page.MyFood(null)) }
    }

    delete?.let { f ->
        val used = data.entries.any { it.foodId == f.id }
        ConfirmDialog(
            "Delete “${f.name}”?",
            if (used) "It's in your log. Those entries will show as unknown food." else "It will be removed from search.",
            "Delete", true, { delete = null },
        ) { nav.store.deleteCustomFood(f.id); delete = null }
    }
}

private val EMOJIS = listOf("🍽", "🍲", "🍛", "🍜", "🥘", "🍗", "🍖", "🐟", "🍤", "🥚", "🍔", "🍕", "🌯", "🥪", "🍟", "🥗", "🍞", "🥐", "🍰", "🍩", "🍪", "🍫", "🍬", "🍨", "☕", "🍵", "🥤", "🧃", "🍺", "🍎", "🍌", "🥜")

/** Add or edit one of "My foods". Values are for ONE serving, the way pack labels and menus show them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyFoodEditorScreen(page: Page.MyFood) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val existing = page.id?.let { id -> data.customFoods.firstOrNull { it.id == id } }
    val f0 = existing?.facts
    fun s(v: Double?) = v?.takeIf { it > 0 }?.let { fmtQty(it) } ?: ""

    var name by remember { mutableStateOf(existing?.name ?: page.name) }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🍽") }
    var unitName by remember { mutableStateOf(existing?.unitName ?: "serving") }
    var unitG by remember { mutableStateOf(s(existing?.unitG)) }
    var liquid by remember { mutableStateOf(existing?.liquid ?: false) }
    var per100 by remember { mutableStateOf(false) }
    var kcal by remember { mutableStateOf(s(f0?.kcal)) }
    var protein by remember { mutableStateOf(s(f0?.protein)) }
    var carbs by remember { mutableStateOf(s(f0?.carbs)) }
    var fat by remember { mutableStateOf(s(f0?.fat)) }
    var fibre by remember { mutableStateOf(s(f0?.fibre)) }
    var sugar by remember { mutableStateOf(s(f0?.sugar)) }
    var satFat by remember { mutableStateOf(s(f0?.satFat)) }
    var sodium by remember { mutableStateOf(s(f0?.sodium)) }
    var calcium by remember { mutableStateOf(s(f0?.calcium)) }
    var iron by remember { mutableStateOf(s(f0?.iron)) }
    var more by remember { mutableStateOf(f0 != null && (f0.sugar + f0.satFat + f0.sodium + f0.calcium + f0.iron) > 0) }

    val mu = if (liquid) "ml" else "g"
    val grams = unitG.toDoubleOrNull()
    // label values per 100 g need a serving weight to convert
    val scale = if (per100) (grams ?: 0.0) / 100.0 else 1.0
    fun v(x: String) = (x.toDoubleOrNull() ?: 0.0) * scale
    val kcalOk = (kcal.toDoubleOrNull() ?: 0.0) > 0
    val macroKcal = v(protein) * 4 + v(carbs) * 4 + v(fat) * 9
    val canSave = name.isNotBlank() && kcalOk && (!per100 || (grams ?: 0.0) > 0)

    Column(Modifier.fillMaxSize()) {
        TopBar(if (existing == null) "New food" else "Edit food", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TextField(name, "Name, e.g. Chicken shawarma roll (Arabian Hut)") { name = it }
            SectionLabel("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJIS.forEach { e ->
                    Box(
                        Modifier.clip(CircleShape).clickable { emoji = e }
                            .then(if (e == emoji) Modifier.padding(0.dp) else Modifier),
                    ) { Emoji(e, if (e == emoji) 46 else 38) }
                }
            }

            SectionLabel("Serving")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(unitName, "Unit (piece, plate, cup…)", Modifier.weight(1f)) { unitName = it.lowercase().take(14) }
                NumberField(unitG, "1 $unitName = ? $mu${if (per100) "" else " (optional)"}", Modifier.weight(1f)) { unitG = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("It's a drink (counts in Liquids)", fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                AppSwitch(liquid) { liquid = it }
            }

            SectionLabel("Nutrition")
            Segmented(listOf("Per $unitName", "Per 100 $mu (pack label)"), if (per100) 1 else 0) { per100 = it == 1 }
            if (per100 && (grams ?: 0.0) <= 0) Text("Enter how many $mu one $unitName is, above, so the label values can be converted.", fontSize = 12.sp, color = c.chi)
            NumberField(kcal, "Calories (kcal) · required", Modifier.fillMaxWidth()) { kcal = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(protein, "Protein g", Modifier.weight(1f)) { protein = it }
                NumberField(carbs, "Carbs g", Modifier.weight(1f)) { carbs = it }
                NumberField(fat, "Fat g", Modifier.weight(1f)) { fat = it }
            }
            NumberField(fibre, "Fibre g", Modifier.fillMaxWidth()) { fibre = it }
            if (!more) {
                SecondaryButton("More: sugar, sat fat, sodium, calcium, iron") { more = true }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(sugar, "Added sugar g", Modifier.weight(1f)) { sugar = it }
                    NumberField(satFat, "Sat. fat g", Modifier.weight(1f)) { satFat = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(sodium, "Sodium mg", Modifier.weight(1f)) { sodium = it }
                    NumberField(calcium, "Calcium mg", Modifier.weight(1f)) { calcium = it }
                    NumberField(iron, "Iron mg", Modifier.weight(1f)) { iron = it }
                }
            }
            if (kcalOk && macroKcal > 0) {
                val diff = macroKcal - v(kcal)
                if (kotlin.math.abs(diff) > v(kcal) * 0.25 + 20) {
                    Text("Heads-up: protein, carbs and fat add up to ${macroKcal.roundToInt()} kcal, not ${v(kcal).roundToInt()}. Worth a second look.", fontSize = 12.sp, color = c.turInk)
                }
            }
            Text("Don't know the values? Calories alone is fine. Protein, carbs and fat can stay empty.", fontSize = 12.sp, color = c.ink2)

            AppCard(color = c.leaf, border = null) {
                Text("ONE ${unitName.uppercase()}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${v(kcal).roundToInt()} kcal", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("P ${fmtQty(v(protein))} · C ${fmtQty(v(carbs))} · F ${fmtQty(v(fat))} g", color = Color.White, fontSize = 12.sp)
                }
            }
            PrimaryButton(if (existing == null) "Save food" else "Save changes", enabled = canSave) {
                val g = grams?.takeIf { it > 0 } ?: 100.0
                nav.store.saveCustomFood(
                    CustomFood(
                        id = existing?.id ?: newKey("u_"), name = name.trim(), emoji = emoji, ing = emptyMap(), yieldG = g,
                        unitName = unitName.trim().ifBlank { "serving" }, unitG = g, liquid = liquid,
                        facts = Facts(
                            kcal = v(kcal), protein = v(protein), carbs = v(carbs), fat = v(fat), fibre = v(fibre),
                            sugar = v(sugar), satFat = v(satFat), sodium = v(sodium), calcium = v(calcium), iron = v(iron),
                        ),
                    ),
                )
                nav.pop()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Suppress("unused")
private fun keepWidth() = Modifier.width(0.dp)
