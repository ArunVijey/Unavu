package app.unavu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.DATE
import app.unavu.data.LogEntry
import app.unavu.data.addWater
import app.unavu.data.entriesOn
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.data.food
import app.unavu.data.grams
import app.unavu.data.liquidsMl
import app.unavu.data.nutr
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.BarChart
import app.unavu.ui.InputDialog
import app.unavu.ui.ListRow
import app.unavu.ui.LocalData
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SectionLabel
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val WATERY = setOf("buttermilk", "tender_coconut", "fresh_juice", "lime_juice")

private data class DrinkRow(val first: LogEntry, val ml: Int, val kcal: Int)

private data class LiquidsSummary(
    val water: Int, val drinksMl: Int, val counted: Int, val drinksKcal: Int,
    val rows: List<DrinkRow>, val coffeeQty: Double, val sugar: Double,
)

@Composable
fun LiquidsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val date = nav.date.value
    val goal = data.settings.waterGoalMl
    val undo = rememberUndoer(stringResource(R.string.g_undo)) { store.restoreSnapshot(it) }
    var editGoal by rememberSaveable { mutableStateOf(false) }
    var chip by remember { mutableStateOf<Pair<Int, Int>?>(null) } // (id, ml)

    val s = remember(data.entries, data.water, data.settings.countDrinksAsWater, date) {
        val (water, drinksMl) = data.liquidsMl(date)
        val drinks = data.entriesOn(date).filter { it.food()?.liquid == true }
        val rows = drinks.groupBy { it.foodId }.map { (_, es) -> DrinkRow(es.first(), es.sumOf { it.grams() }.roundToInt(), es.sumOf { it.nutr().kcal }.roundToInt()) }
        val extra = if (data.settings.countDrinksAsWater) drinks.filter { it.foodId in WATERY }.sumOf { it.grams() }.roundToInt() else 0
        LiquidsSummary(
            water, drinksMl, water + extra, drinks.sumOf { it.nutr().kcal }.roundToInt(), rows,
            drinks.filter { it.foodId == "filter_coffee" || it.foodId == "tea" }.sumOf { it.qty }, drinks.sumOf { it.nutr().sugar },
        )
    }
    val monday = remember(date) { LocalDate.parse(date).with(DayOfWeek.MONDAY) }
    val days = remember(monday) { (0..6).map { monday.plusDays(it.toLong()) } }
    val weekL = days.map { (data.water[it.format(DATE)] ?: 0) / 1000.0 }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_liq_title, dayLabel(date)), onBack = { nav.pop() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
            item(key = "summary", contentType = "card") {
                val totalL = "%.1f".format((s.water + s.drinksMl) / 1000.0)
                AppCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            // the day being shown (Today, Yesterday, Sun, 21 Nov), not always "TODAY"
                            Text(dayLabel(date).uppercase(), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2)
                            Text(stringResource(R.string.g_liters, totalL), style = U.t.display)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            StatusBadge(
                                if (s.counted >= goal) stringResource(R.string.g_liq_goal_done) else stringResource(R.string.g_liq_goal_progress, "%.1f".format(s.counted / 1000.0), "%.1f".format(goal / 1000.0)),
                                if (s.counted >= goal) Status.OK else Status.INFO,
                            )
                            Text(stringResource(R.string.g_liq_drinks_kcal, s.drinksKcal), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box {
                        SpringTrack(s.counted / goal.toFloat(), c.blue, height = 10.dp)
                        chip?.let { (id, ml) -> key(id) { FloatChip(stringResource(R.string.g_liq_plus_ml, ml), Modifier.align(Alignment.TopEnd)) } }
                    }
                    Text(
                        stringResource(R.string.g_liq_goal_edit, "%.1f".format(goal / 1000.0)), style = U.t.label, color = c.ink2,
                        modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { editGoal = true }.padding(top = 12.dp),
                    )
                }
            }
            item(key = "add-label", contentType = "label") { SectionLabel(stringResource(R.string.g_liq_add_water)) }
            item(key = "tiles", contentType = "tiles") {
                val glass = stringResource(R.string.g_liq_glass); val tumbler = stringResource(R.string.g_liq_tumbler); val bottle = stringResource(R.string.g_liq_bottle)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("🥛", glass, 250), Triple("🍶", tumbler, 150), Triple("🧴", bottle, 500)).forEach { (e, n, ml) ->
                        val desc = stringResource(R.string.g_liq_add_desc, ml, n)
                        val added = stringResource(R.string.g_liq_added, ml)
                        LiquidTile(e, n, stringResource(R.string.g_liq_plus_ml, ml), false, Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = desc }) {
                            val before = store.data.value
                            store.addWater(date, ml)
                            chip = (chip?.first ?: 0) + 1 to ml
                            undo.announce(added, before)
                        }
                    }
                }
            }
            item(key = "water", contentType = "card") {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    ListRow(stringResource(R.string.g_liq_water), stringResource(R.string.g_liq_plain), trailing = stringResource(R.string.r7d_ml, fmtInt(s.water)))
                }
            }
            items(s.rows, key = { "drink-" + it.first.foodId }, contentType = { "drink" }) { r ->
                val f = r.first.food()
                if (f != null) {
                    AppCard(Modifier.animateItem(), padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                        ListRow("${f.emoji} ${f.name}", stringResource(R.string.g_liq_in_meal, r.first.meal.label, r.kcal), trailing = stringResource(R.string.r7d_ml, fmtInt(r.ml)))
                    }
                }
            }
            item(key = "add-drink", contentType = "card") {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    ListRow(stringResource(R.string.g_liq_add_drink), stringResource(R.string.g_liq_add_drink_sub)) { nav.quickAdd.value = QuickAddRequest(mode = "LIQUID") }
                }
            }
            item(key = "coffee", contentType = "card") {
                AppCard(color = c.turS, border = null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.g_liq_coffee), style = U.t.titleS, modifier = Modifier.weight(1f))
                        StatusBadge(stringResource(R.string.g_liq_tsp, "%.1f".format(s.sugar / 4.2)), if (s.sugar > 25) Status.OVER else Status.LOW)
                    }
                    Text(
                        stringResource(R.string.g_liq_coffee_line, fmtQty(s.coffeeQty), "%.0f".format(s.sugar)),
                        style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            item(key = "count-switch", contentType = "card") {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    ListRow(stringResource(R.string.g_liq_count_as_water), trailing = null, end = {
                        AppSwitch(data.settings.countDrinksAsWater) { v -> store.update { d -> d.copy(settings = d.settings.copy(countDrinksAsWater = v)) } }
                    })
                }
            }
            item(key = "week", contentType = "card") {
                val names = days.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) }
                val full = days.zip(weekL).joinToString(", ") { (d, l) -> d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + "%.1f".format(l) }
                val desc = stringResource(R.string.g_liq_chart_desc, full, "%.1f".format(goal / 1000.0))
                AppCard {
                    SectionLabel(stringResource(R.string.g_liq_week))
                    BarChart(weekL, names, goal / 1000.0, days.map { c.blue }, modifier = Modifier.clearAndSetSemantics { contentDescription = desc }, height = 90)
                }
            }
        }
    }
    if (editGoal) {
        val rangeError = stringResource(R.string.r7d_water_range)
        fun litresOf(v: String) = v.trim().toDoubleOrNull()?.takeIf { it in 0.5..6.0 }
        InputDialog(
            stringResource(R.string.g_liq_goal_dialog), stringResource(R.string.g_liq_litres), "%.2f".format(goal / 1000.0).trimEnd('0').trimEnd('.'), numeric = true,
            validate = { v -> if (litresOf(v) == null) rangeError else null },
            onDismiss = { editGoal = false },
        ) { v ->
            litresOf(v)?.let { l -> store.update { d -> d.copy(settings = d.settings.copy(waterGoalMl = (l * 1000).roundToInt())) } }
        }
    }
}

/** Progress bar whose fill springs (slight overshoot) whenever the value changes. */
@Composable
private fun SpringTrack(fraction: Float, color: androidx.compose.ui.graphics.Color, height: Dp) {
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val f by animateFloatAsState(
        if (shown) fraction.coerceIn(0f, 1f) else 0f,
        app.unavu.ui.theme.motionSpring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "liquidFill",
    )
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height)).background(U.c.sf2)) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(height)).background(color))
    }
}

/** A small "+250 ml" chip that floats up and fades out, once per composition. */
@Composable
private fun FloatChip(text: String, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val p = remember { Animatable(0f) }
    val spec = motionTween<Float>(900)
    LaunchedEffect(Unit) { if (!reduced) p.animateTo(1f, spec) else p.snapTo(1f) }
    val alpha = if (p.value < 0.6f) 1f else (1f - (p.value - 0.6f) / 0.4f)
    if (alpha <= 0.01f) return
    val dy = with(androidx.compose.ui.platform.LocalDensity.current) { (-28.dp * p.value).roundToPx() }
    Text(
        text, style = U.t.caption, fontWeight = FontWeight.Bold, color = U.c.blueInk,
        modifier = modifier.offset { IntOffset(0, dy - 4.dp.roundToPx()) }.graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(50)).background(U.c.blueS).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
