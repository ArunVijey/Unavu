package app.unavu.ui.screens

import androidx.compose.ui.platform.LocalResources
import app.unavu.data.Meal
import app.unavu.ui.LocalFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import app.unavu.data.unitLabel
import app.unavu.data.FoodSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import app.unavu.R
import app.unavu.data.fmtQty
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import app.unavu.ui.EmptyState
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TamilText
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.motionTween
import app.unavu.ui.pressScale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDetailScreen(page: Page.FoodDetail) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val food = FoodDb.get(page.foodId) ?: run { nav.pop(); return }
    val p = data.portions[food.id]
    var unit by rememberSaveable { mutableStateOf(p?.unit?.takeIf { u -> food.units.any { it.name == u } } ?: food.units.first().name) }
    var qty by rememberSaveable { mutableStateOf(p?.qty ?: food.defaultQty) }
    var choices by remember { mutableStateOf(food.resolve(p?.choices ?: emptyMap())) }
    var mealName by rememberSaveable { mutableStateOf(page.meal?.name) }
    val meal = mealName?.let { nm -> Meal.entries.firstOrNull { it.name == nm } }
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val ctx = LocalResources.current
    var customise by rememberSaveable { mutableStateOf(false) }
    val n = food.nutrFor(qty, unit, choices)
    val t = data.targets()

    Column(Modifier.fillMaxSize()) {
        TopBar("Food", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Emoji(food.emoji, 54)
                Column {
                    Text(food.name, style = U.t.title, fontWeight = FontWeight.Bold)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroDonut(n.protein * 4, n.carbs * 4, n.fat * 9, n.kcal.roundToInt())
                    Column(Modifier.weight(1f)) {
                        val tot = (n.protein * 4 + n.carbs * 4 + n.fat * 9).coerceAtLeast(1.0)
                        listOf(Triple(c.leaf, "Protein", n.protein * 4), Triple(c.tur, "Carbs", n.carbs * 4), Triple(c.m3, "Fat", n.fat * 9)).forEach { (col, name, kc) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).padding(0.dp)) { Canvas(Modifier.size(9.dp)) { drawCircle(col) } }
                                Text("  $name", style = U.t.bodyS, modifier = Modifier.weight(1f))
                                Text("${"%.1f".format(kc / (if (name == "Fat") 9 else 4))} g", fontWeight = FontWeight.Bold, style = U.t.bodyS)
                                Text("  ${(kc / tot * 100).roundToInt()}%", style = U.t.caption, color = c.ink2)
                            }
                        }
                    }
                }
                val summary = food.choiceSummary(choices)
                if (summary.isNotBlank()) Text(summary, style = U.t.caption, color = c.leafInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                val n100 = food.nutrFor(100.0, if (food.liquid) "ml" else "g", choices)
                Text(
                    "Per 100 ${if (food.liquid) "ml" else "g"}: ${n100.kcal.roundToInt()} kcal · P ${"%.1f".format(n100.protein)} g · C ${"%.1f".format(n100.carbs)} g · F ${"%.1f".format(n100.fat)} g · fibre ${"%.1f".format(n100.fibre)} g",
                    style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
                )
                PortionStepper(qty, step = if (unit == "g" || unit == "ml") 10.0 else 0.5, label = "${unitLabel(unit, qty)} ≈ ${(qty * food.unit(unit).grams).roundToInt()} ${if (food.liquid) "ml" else "g"}") { qty = it }
            }
            if (t != null) {
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    SectionTitle("Nutrients · % of your daily target")
                    NUTRIENTS.forEachIndexed { i, d -> StaggerIn(i) { Column {
                        val v = d.value(n); val tv = d.target(t)
                        Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                            Text(d.name, style = U.t.bodyS, modifier = Modifier.weight(1f))
                            Text("${if (v < 10) "%.1f".format(v) else v.roundToInt().toString()} ${d.unit}", fontWeight = FontWeight.Bold, style = U.t.bodyS)
                            Text("  ${(v / tv * 100).roundToInt()}%", style = U.t.caption, color = c.ink2)
                        }
                        Track((v / tv).toFloat(), if (d.limit) c.chi else c.leaf, Modifier.padding(top = 3.dp, bottom = 3.dp), height = 5.dp)
                    } } }
                }
            }
            // what's in it (per this portion)
            val (batch, yieldG) = food.batch(choices)
            val factor = qty * food.unit(unit).grams / yieldG
            AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                SectionTitle("What's in this portion")
                batch.entries.filter { (k, g) -> k != "water" && g * factor >= 0.5 }.sortedByDescending { it.value }.forEachIndexed { i, (k, g) ->
                    if (i > 0) Divider()
                    val ing = FoodDb.ingredient(k)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(ing?.name ?: k, style = U.t.bodyS, modifier = Modifier.weight(1f))
                        Text("${"%.1f".format(g * factor)} g", style = U.t.bodyS, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    if (food.category == "My foods") "Your own values (My foods)." else if (food.source == FoodSource.CUSTOM) "Your recipe." else "Calculated from IFCT 2017 (ICMR-NIN) ingredient values; batch and cooked weight are typical home amounts.",
                    style = U.t.caption, color = c.ink2, modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            SectionTitle("Add to")
            MealPicker(meal) { mealName = it.name }
            PrimaryButton(if (meal == null) "Pick a meal above" else "＋ Add to ${meal!!.label}", enabled = meal != null) {
                val before = nav.store.data.value
                nav.store.logItems(listOf(NewItem(food.id, qty, unit, choices)), meal!!, nav.date.value, cheat = false)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                feedback?.show(ctx.getString(R.string.f_added_to, food.name, meal.label), ctx.getString(R.string.f_undo)) { nav.store.restoreSnapshot(before) }
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
    val desc = "${kcal} kcal: protein ${(p / 4).roundToInt()} g, carbs ${(cb / 4).roundToInt()} g, fat ${(f / 9).roundToInt()} g"
    val reduced = LocalReducedMotion.current
    val sweepP = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) sweepP.animateTo(1f, tween(Motion.LONG, easing = Motion.EmphasizedDecelerate)) }
    val kcalShown by animateIntAsState(kcal, motionTween(Motion.LONG), label = "donutKcal")
    Box(Modifier.size(92.dp).semantics(mergeDescendants = true) { contentDescription = desc }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(92.dp)) {
            val sw = 12.dp.toPx()
            val r = Size(size.width - sw, size.height - sw)
            val o = Offset(sw / 2, sw / 2)
            drawArc(c.sf2, 0f, 360f, false, o, r, style = Stroke(sw))
            var start = -90f
            listOf(p to c.leaf, cb to c.tur, f to c.m3).forEach { (v, col) ->
                val sweep = (v / tot * 360).toFloat() * sweepP.value
                if (sweep > 0) drawArc(col, start, sweep, false, o, r, style = Stroke(sw))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$kcalShown", fontWeight = FontWeight.ExtraBold, style = U.t.title)
            Text("kcal", style = U.t.caption, color = c.ink2)
        }
    }
}

/** Amount control: round -/+ buttons with a number that slides up or down with each step (instant under reduced motion). */
@Composable
private fun PortionStepper(qty: Double, step: Double, label: String, onChange: (Double) -> Unit) {
    val c = U.c
    val reduced = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        StepBtn("−", "Decrease") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange((qty - step).coerceAtLeast(step)) }
        AnimatedContent(
            qty, label = "portionQty", modifier = Modifier.widthIn(min = 52.dp),
            transitionSpec = {
                if (reduced) EnterTransition.None togetherWith ExitTransition.None
                else {
                    val up = targetState > initialState
                    (slideInVertically(tween(Motion.SHORT, easing = Motion.EmphasizedDecelerate)) { if (up) it else -it } + fadeIn(tween(Motion.SHORT))) togetherWith
                        (slideOutVertically(tween(Motion.SHORT, easing = Motion.EmphasizedAccelerate)) { if (up) -it else it } + fadeOut(tween(Motion.XSHORT)))
                }
            },
        ) { q -> Text(fmtQty(q), style = U.t.title, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        StepBtn("+", "Increase") { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(qty + step) }
        Text(label, style = U.t.caption, color = c.ink2, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun StepBtn(glyph: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).pressScale().clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) { Box(Modifier.size(34.dp).clip(CircleShape).border(1.5.dp, U.c.line, CircleShape), contentAlignment = Alignment.Center) { Text(glyph, style = U.t.title) } }
}
