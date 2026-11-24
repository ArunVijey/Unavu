package app.unavu.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.FoodDb
import app.unavu.data.FoodSource
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.NewItem
import app.unavu.data.batch
import app.unavu.data.fmtInt
import app.unavu.data.logItems
import app.unavu.data.nutrFor
import app.unavu.data.targets
import app.unavu.data.unitLabel
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.MealPicker
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Stepper
import app.unavu.ui.TamilText
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import app.unavu.ui.theme.motionTween
import kotlin.math.roundToInt

/** "g" or "ml": the mass/volume symbol a food's amounts are shown in. */
internal fun amountSymbol(liquid: Boolean): String = if (liquid) "ml" else "g"

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
    var choices by rememberSaveable(stateSaver = ChoicesSaver) { mutableStateOf(food.resolve(p?.choices ?: emptyMap())) }
    var mealName by rememberSaveable { mutableStateOf(page.meal?.name) }
    val meal = mealName?.let { nm -> Meal.entries.firstOrNull { it.name == nm } }
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val res = LocalResources.current
    var customise by rememberSaveable { mutableStateOf(false) }
    val n = food.nutrFor(qty, unit, choices)
    val t = data.targets()
    val sym = amountSymbol(food.liquid)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.r7a_food_details), onBack = { nav.pop() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Emoji(food.emoji, 54)
                Column(Modifier.weight(1f)) {
                    Text(food.name, style = U.t.title, fontWeight = FontWeight.Bold)
                    if (food.tamil.isNotBlank()) TamilText(food.tamil)
                    StatusBadge(food.source.label, Status.INFO, Modifier.padding(top = 3.dp))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                food.units.forEach { u ->
                    val perWeight = u.name == "g" || u.name == "ml"
                    val label = if (perWeight) stringResource(R.string.r7a_unit_per_100, u.name)
                    else stringResource(R.string.r7a_unit_one, u.name, fmtInt(u.grams), sym)
                    ChoiceChip(label, unit == u.name && (qty == 1.0 || (u.grams == 1.0 && qty == 100.0))) {
                        unit = u.name; qty = if (u.grams == 1.0) 100.0 else 1.0
                    }
                }
                ChoiceChip(stringResource(R.string.r7a_customise_chip), false) { customise = true }
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroDonut(n.protein * 4, n.carbs * 4, n.fat * 9, n.kcal.roundToInt())
                    Column(Modifier.weight(1f)) {
                        val tot = (n.protein * 4 + n.carbs * 4 + n.fat * 9).coerceAtLeast(1.0)
                        listOf(
                            Triple(c.leaf, stringResource(R.string.d_protein), n.protein),
                            Triple(c.tur, stringResource(R.string.d_carbs), n.carbs),
                            Triple(c.m3, stringResource(R.string.d_fat), n.fat),
                        ).forEachIndexed { i, (col, name, grams) ->
                            val kc = grams * (if (i == 2) 9 else 4)
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Canvas(Modifier.size(9.dp)) { drawCircle(col) }
                                Text(name, style = U.t.bodyS, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                Text(stringResource(R.string.r7a_grams, fmtNum(grams)), fontWeight = FontWeight.Bold, style = U.t.bodyS)
                                Text(
                                    stringResource(R.string.r7a_percent, (kc / tot * 100).roundToInt()), style = U.t.caption, color = c.ink2,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
                val summary = food.choiceSummary(choices)
                if (summary.isNotBlank()) Text(summary, style = U.t.caption, color = c.leafInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                val n100 = food.nutrFor(100.0, sym, choices)
                Text(
                    stringResource(R.string.r7a_per_100, sym, fmtInt(n100.kcal), fmtNum(n100.protein), fmtNum(n100.carbs), fmtNum(n100.fat), fmtNum(n100.fibre)),
                    style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
                )
                // the shared stepper: fixed 56 dp value slot so "+" never gets squeezed, label takes what's left
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val step = if (unit == "g" || unit == "ml") 10.0 else 0.5
                    Stepper(qty, step, min = step, itemName = food.name, slotWidth = 56.dp) { qty = it }
                    Text(
                        stringResource(R.string.r7a_portion_label, unitLabel(unit, qty), fmtInt(qty * food.unit(unit).grams), sym),
                        style = U.t.caption, color = c.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    )
                }
            }
            if (t != null) {
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    SectionTitle(stringResource(R.string.r7a_nutrients_title), hint = stringResource(R.string.r7a_nutrients_hint))
                    NUTRIENTS.forEachIndexed { i, d ->
                        StaggerIn(i) {
                            Column {
                                val v = d.value(n); val tv = d.target(t)
                                Row(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                                    Text(d.name, style = U.t.bodyS, modifier = Modifier.weight(1f))
                                    Text("${fmtNum(v)} ${d.unit}", fontWeight = FontWeight.Bold, style = U.t.bodyS)
                                    Text(
                                        stringResource(R.string.r7a_percent, if (tv > 0) (v / tv * 100).roundToInt() else 0), style = U.t.caption, color = c.ink2,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                                Track(if (tv > 0) (v / tv).toFloat() else 0f, if (d.limit) c.chi else c.leaf, Modifier.padding(top = 3.dp, bottom = 3.dp), height = 5.dp)
                            }
                        }
                    }
                }
            }
            // what's in it (per this portion)
            val (batch, yieldG) = food.batch(choices)
            val factor = qty * food.unit(unit).grams / yieldG
            AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                SectionTitle(stringResource(R.string.r7a_whats_in_title))
                batch.entries.filter { (k, g) -> k != "water" && g * factor >= 0.5 }.sortedByDescending { it.value }.forEachIndexed { i, (k, g) ->
                    if (i > 0) Divider()
                    val ing = FoodDb.ingredient(k)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(ing?.name ?: k, style = U.t.bodyS, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.r7a_grams, fmtNum(g * factor)), style = U.t.bodyS, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    stringResource(
                        when {
                            food.category == "My foods" -> R.string.r7a_source_my_food
                            food.source == FoodSource.CUSTOM -> R.string.r7a_source_recipe
                            else -> R.string.r7a_source_ifct
                        },
                    ),
                    style = U.t.caption, color = c.ink2, modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            SectionTitle(stringResource(R.string.r7a_add_to_title))
            MealPicker(meal) { mealName = it.name }
            val target = meal
            PrimaryButton(
                if (target == null) stringResource(R.string.f_btn_pick_meal) else stringResource(R.string.r7a_add_to_meal, target.label.lowercase()),
                enabled = target != null,
            ) {
                if (target == null) return@PrimaryButton
                val before = nav.store.data.value
                nav.store.logItems(listOf(NewItem(food.id, qty, unit, choices)), target, nav.date.value, cheat = false)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                feedback?.show(res.getString(R.string.f_added_to, food.name, target.label), res.getString(R.string.f_undo)) { nav.store.restoreSnapshot(before) }
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

/** Protein / carbs / fat ring with the kcal counting up in the middle. Values are kcal from each macro. */
@Composable
fun MacroDonut(p: Double, cb: Double, f: Double, kcal: Int) {
    val c = U.c
    val tot = (p + cb + f).coerceAtLeast(1.0)
    // same numbers as the visible macro rows (fmtNum), so TalkBack never disagrees with the screen
    val desc = stringResource(R.string.r7a_donut_desc, fmtInt(kcal), fmtNum(p / 4), fmtNum(cb / 4), fmtNum(f / 9))
    val reduced = LocalReducedMotion.current
    val sweepP = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) sweepP.animateTo(1f, tween(Motion.LONG, easing = Motion.EmphasizedDecelerate)) }
    val kcalShown by animateIntAsState(kcal, motionTween(Motion.LONG), label = "donutKcal")
    Box(Modifier.size(92.dp).clearAndSetSemantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
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
            Text(fmtInt(kcalShown), fontWeight = FontWeight.ExtraBold, style = U.t.title, maxLines = 1)
            Text(stringResource(R.string.d_kcal_unit), style = U.t.caption, color = c.ink2)
        }
    }
}
