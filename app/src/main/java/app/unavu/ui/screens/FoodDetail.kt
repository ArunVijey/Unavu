package app.unavu.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.FoodDb
import app.unavu.data.NUTRIENTS
import app.unavu.data.NewItem
import app.unavu.data.batch
import app.unavu.data.logItems
import app.unavu.data.nutrFor
import app.unavu.data.targets
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.LocalData
import app.unavu.ui.MealPicker
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TamilText
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDetailScreen(page: Page.FoodDetail) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val food = FoodDb.get(page.foodId) ?: run { nav.pop(); return }
    val p = data.portions[food.id]
    var unit by remember { mutableStateOf(p?.unit?.takeIf { u -> food.units.any { it.name == u } } ?: food.units.first().name) }
    var qty by remember { mutableStateOf(p?.qty ?: food.defaultQty) }
    var choices by remember { mutableStateOf(food.resolve(p?.choices ?: emptyMap())) }
    var meal by remember { mutableStateOf(page.meal) }
    var customise by remember { mutableStateOf(false) }
    val n = food.nutrFor(qty, unit, choices)
    val t = data.targets()

    Column(Modifier.fillMaxSize()) {
        TopBar("Food", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Emoji(food.emoji, 54)
                Column {
                    Text(food.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (food.tamil.isNotBlank()) TamilText(food.tamil)
                    StatusBadge(food.source.label, Status.INFO, Modifier.padding(top = 3.dp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                food.units.forEach { u ->
                    ChoiceChip(if (u.name == "g" || u.name == "ml") "100 ${u.name}" else "1 ${u.name} · ${u.grams.roundToInt()}${if (food.liquid) "ml" else "g"}", unit == u.name && (qty == 1.0 || (u.grams == 1.0 && qty == 100.0))) {
                        unit = u.name; qty = if (u.grams == 1.0) 100.0 else 1.0
                    }
                }
                ChoiceChip("Customise…", false) { customise = true }
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MacroDonut(n.protein * 4, n.carbs * 4, n.fat * 9, n.kcal.roundToInt())
                    Column(Modifier.weight(1f)) {
                        val tot = (n.protein * 4 + n.carbs * 4 + n.fat * 9).coerceAtLeast(1.0)
                        listOf(Triple(c.leaf, "Protein", n.protein * 4), Triple(c.tur, "Carbs", n.carbs * 4), Triple(c.m3, "Fat", n.fat * 9)).forEach { (col, name, kc) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).padding(0.dp)) { Canvas(Modifier.size(9.dp)) { drawCircle(col) } }
                                Text("  $name", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text("${"%.1f".format(kc / (if (name == "Fat") 9 else 4))} g", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("  ${(kc / tot * 100).roundToInt()}%", fontSize = 12.sp, color = c.ink2)
                            }
                        }
                    }
                }
                val summary = food.choiceSummary(choices)
                if (summary.isNotBlank()) Text(summary, fontSize = 11.5.sp, color = c.leafInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                val n100 = food.nutrFor(100.0, if (food.liquid) "ml" else "g", choices)
                Text(
                    "Per 100 ${if (food.liquid) "ml" else "g"}: ${n100.kcal.roundToInt()} kcal · P ${"%.1f".format(n100.protein)} g · C ${"%.1f".format(n100.carbs)} g · F ${"%.1f".format(n100.fat)} g · fibre ${"%.1f".format(n100.fibre)} g",
                    fontSize = 12.sp, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
                )
                Text("Amount: ${app.unavu.data.fmtQty(qty)} ${app.unavu.data.unitLabel(unit, qty)} ≈ ${(qty * food.unit(unit).grams).roundToInt()} ${if (food.liquid) "ml" else "g"}", fontSize = 11.5.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
            }
            if (t != null) {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                    SectionLabel("Nutrients · % of your daily target")
                    NUTRIENTS.forEach { d ->
                        val v = d.value(n); val tv = d.target(t)
                        Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                            Text(d.name, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                            Text("${if (v < 10) "%.1f".format(v) else v.roundToInt().toString()} ${d.unit}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text("  ${(v / tv * 100).roundToInt()}%", fontSize = 12.sp, color = c.ink2)
                        }
                        Track((v / tv).toFloat(), if (d.limit) c.chi else c.leaf, Modifier.padding(top = 3.dp, bottom = 3.dp), height = 5.dp)
                    }
                }
            }
            // what's in it (per this portion)
            val (batch, yieldG) = food.batch(choices)
            val factor = qty * food.unit(unit).grams / yieldG
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                SectionLabel("What's in this portion")
                batch.entries.filter { (k, g) -> k != "water" && g * factor >= 0.5 }.sortedByDescending { it.value }.forEachIndexed { i, (k, g) ->
                    if (i > 0) Divider()
                    val ing = FoodDb.ingredient(k)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(ing?.name ?: k, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                        Text("${"%.1f".format(g * factor)} g", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (food.category == "My foods") "Your own values (My foods)." else if (food.source == app.unavu.data.FoodSource.CUSTOM) "Your recipe." else "Calculated from IFCT 2017 (ICMR-NIN) ingredient values; batch and cooked weight are typical home amounts.",
                    fontSize = 10.5.sp, color = c.ink2, modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            SectionLabel("Add to")
            MealPicker(meal) { meal = it }
            PrimaryButton(if (meal == null) "Pick a meal above" else "＋ Add to ${meal!!.label}", enabled = meal != null) {
                nav.store.logItems(listOf(NewItem(food.id, qty, unit, choices)), meal!!, nav.date.value, cheat = false)
                nav.pop()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (customise) {
        CustomiseSheet(food, NewItem(food.id, qty, unit, choices), onDismiss = { customise = false }) { ni ->
            qty = ni.qty; unit = ni.unit; choices = ni.choices; customise = false
        }
    }
}

@Composable
fun MacroDonut(p: Double, cb: Double, f: Double, kcal: Int) {
    val c = U.c
    val tot = (p + cb + f).coerceAtLeast(1.0)
    Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(92.dp)) {
            val sw = 12.dp.toPx()
            val r = Size(size.width - sw, size.height - sw)
            val o = Offset(sw / 2, sw / 2)
            drawArc(c.sf2, 0f, 360f, false, o, r, style = Stroke(sw))
            var start = -90f
            listOf(p to c.leaf, cb to c.tur, f to c.m3).forEach { (v, col) ->
                val sweep = (v / tot * 360).toFloat()
                if (sweep > 0) drawArc(col, start, sweep, false, o, r, style = Stroke(sw))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$kcal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("kcal", fontSize = 9.sp, color = c.ink2)
        }
    }
}
