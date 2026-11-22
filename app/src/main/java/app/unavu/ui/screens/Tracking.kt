package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.DATE
import app.unavu.data.Meal
import app.unavu.data.Smart
import app.unavu.data.addWater
import app.unavu.data.copyDay
import app.unavu.data.deleteWeight
import app.unavu.data.entriesOn
import app.unavu.data.food
import app.unavu.data.grams
import app.unavu.data.isCheatDay
import app.unavu.data.liquidsMl
import app.unavu.data.nutr
import app.unavu.data.setCheatDay
import app.unavu.data.targets
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
import app.unavu.ui.BarChart
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Divider
import app.unavu.ui.InputDialog
import app.unavu.ui.LineChart
import app.unavu.ui.ListRow
import app.unavu.ui.AppSwitch
import app.unavu.ui.LocalData
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Tab
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

// ======================= Liquids =======================

@Composable
fun LiquidsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val date = nav.date.value
    val (water, drinksMl) = data.liquidsMl(date)
    val goal = data.settings.waterGoalMl
    val drinks = data.entriesOn(date).filter { it.food()?.liquid == true }
    val byDrink = drinks.groupBy { it.foodId }.map { (id, es) -> es.first() to (es.sumOf { it.grams() } to es.sumOf { it.nutr().kcal }) }
    val counted = water + if (data.settings.countDrinksAsWater) drinks.filter { it.foodId in setOf("buttermilk", "tender_coconut", "fresh_juice", "lime_juice") }.sumOf { it.grams() }.roundToInt() else 0
    val coffee = drinks.filter { it.foodId == "filter_coffee" || it.foodId == "tea" }
    val sugar = drinks.sumOf { it.nutr().sugar }
    var editGoal by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Liquids · ${dayTitle(date)}", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("TODAY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.ink2)
                        Text("${"%.1f".format((water + drinksMl) / 1000.0)} L", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        StatusBadge(if (counted >= goal) "Water goal ✓" else "Water ${"%.1f".format(counted / 1000.0)} / ${"%.1f".format(goal / 1000.0)} L", if (counted >= goal) Status.OK else Status.INFO)
                        Text("${drinks.sumOf { it.nutr().kcal }.roundToInt()} kcal from drinks", fontSize = 11.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Track(counted / goal.toFloat(), c.blue, height = 10.dp)
                Text("Goal: ${"%.1f".format(goal / 1000.0)} L · tap to change", fontSize = 11.sp, color = c.ink2, modifier = Modifier.padding(top = 6.dp).clickable { editGoal = true })
            }
            SectionLabel("Add water")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Triple("🥛", "Glass", 250), Triple("🍶", "Tumbler", 150), Triple("🧴", "Bottle", 500)).forEach { (e, n, ml) ->
                    LiquidTile(e, n, "+$ml ml", false, Modifier.weight(1f)) { store.addWater(date, ml) }
                }
                LiquidTile("↩️", "Undo", "−250 ml", false, Modifier.weight(1f)) { store.addWater(date, -250) }
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                ListRow("💧 Water", "Plain water", trailing = "$water ml")
                byDrink.forEach { (e, v) ->
                    Divider()
                    val f = e.food()!!
                    ListRow("${f.emoji} ${f.name}", "In ${e.meal.label} · ${v.second.roundToInt()} kcal", trailing = "${v.first.roundToInt()} ml")
                }
                Divider()
                ListRow("＋ Add a drink", "Coffee, tea, juice, Boost… (counts in a meal too)", trailing = "›") { nav.quickAdd.value = QuickAddRequest(mode = "LIQUID") }
            }
            AppCard(color = c.turS, border = null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("☕ Coffee & sugar", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusBadge("${"%.1f".format(sugar / 4.2)} tsp sugar", if (sugar > 25) Status.OVER else Status.LOW)
                }
                Text(
                    "${app.unavu.data.fmtQty(coffee.sumOf { it.qty })} coffee/tea · ${"%.0f".format(sugar)} g added sugar from drinks (limit 25 g a day for everything)",
                    fontSize = 12.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                ListRow("Count buttermilk, tender coconut & juices as water", trailing = null, end = {
                    AppSwitch(data.settings.countDrinksAsWater) { v -> store.update { d -> d.copy(settings = d.settings.copy(countDrinksAsWater = v)) } }
                })
            }
            // week
            val monday = LocalDate.parse(date).with(DayOfWeek.MONDAY)
            val days = (0..6).map { monday.plusDays(it.toLong()) }
            AppCard {
                SectionLabel("This week · water")
                BarChart(days.map { (data.water[it.format(DATE)] ?: 0) / 1000.0 }, days.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) }, goal / 1000.0, days.map { c.blue }, height = 90)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (editGoal) {
        InputDialog("Daily water goal", "Litres", "%.1f".format(goal / 1000.0), numeric = true, onDismiss = { editGoal = false }) { v ->
            v.toDoubleOrNull()?.let { l -> store.update { d -> d.copy(settings = d.settings.copy(waterGoalMl = (l * 1000).roundToInt().coerceIn(500, 6000))) } }
        }
    }
}

// ======================= History =======================

@Composable
fun HistoryScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    var month by rememberSaveable { mutableStateOf(YearMonth.from(LocalDate.parse(nav.date.value)).toString()) }
    val ym = YearMonth.parse(month)
    var selected by rememberSaveable { mutableStateOf(nav.date.value) }
    val t = data.targets()
    var cheatNote by remember { mutableStateOf(false) }
    var copyConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar("History", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArrowButton(Arrow.LEFT) { month = ym.minusMonths(1).toString() }
                Text("${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}", fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                ArrowButton(Arrow.RIGHT) { if (ym < YearMonth.now()) month = ym.plusMonths(1).toString() }
            }
            AppCard(padding = PaddingValues(10.dp)) {
                Row { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
                Spacer(Modifier.height(6.dp))
                val first = ym.atDay(1)
                val offset = first.dayOfWeek.value - 1
                val cells = (0 until offset).map { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 3.dp)) {
                        week.forEach { d ->
                            if (d == null) Spacer(Modifier.weight(1f)) else {
                                val ds = d.format(DATE)
                                val on = ds == selected
                                val future = d.isAfter(LocalDate.now())
                                val kcal = data.entriesOn(ds).total().kcal
                                val dot = when {
                                    kcal <= 0 || t == null -> Color.Transparent
                                    kcal > t.kcal * 1.05 -> c.chi
                                    kcal < t.kcal * 0.85 -> c.tur
                                    else -> c.leaf
                                }
                                Column(
                                    Modifier.weight(1f).aspectRatio(0.95f).clip(RoundedCornerShape(10.dp)).background(if (on) c.ink else Color.Transparent)
                                        .clickable(enabled = !future) { selected = ds },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("${d.dayOfMonth}", fontSize = 12.5.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = if (on) c.bg else if (future) c.ink2.copy(alpha = 0.5f) else c.ink)
                                    if (data.isCheatDay(ds)) Text("🍕", fontSize = 8.sp) else Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (on && dot != Color.Transparent) c.bg else dot))
                                }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                    listOf(c.leaf to "On target", c.tur to "Under", c.chi to "Over").forEach { (col, l) ->
                        Box(Modifier.padding(top = 4.dp).size(7.dp).clip(CircleShape).background(col)); Text(" $l   ", fontSize = 10.5.sp, color = c.ink2)
                    }
                    Text("🍕 Cheat day", fontSize = 10.5.sp, color = c.ink2)
                }
            }
            // selected day
            val es = data.entriesOn(selected)
            val tot = es.total()
            val cheat = data.isCheatDay(selected)
            AppCard(border = if (cheat) c.m3 else c.line) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dayTitle(selected), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    if (cheat) StatusBadge("🍕 Cheat day", Status.LOW)
                    else if (t != null && es.isNotEmpty()) StatusBadge("${tot.kcal.roundToInt()} / ${t.kcal}", if (tot.kcal > t.kcal * 1.05) Status.OVER else Status.OK)
                }
                if (cheat) Text("${data.dayNotes[selected] ?: "Cheat day"} · ${tot.kcal.roundToInt()} kcal · not in averages", fontSize = 11.5.sp, color = c.ink2)
                Spacer(Modifier.height(6.dp))
                if (es.isEmpty()) Text("Nothing logged.", color = c.ink2, fontSize = 12.5.sp)
                Meal.entries.forEach { m ->
                    val list = es.filter { it.meal == m }
                    if (list.isNotEmpty()) {
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(m.label, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text(" · " + list.joinToString(", ") { it.food()?.name ?: "?" } + " · ${list.total().kcal.roundToInt()}", fontSize = 12.5.sp, color = c.ink2, maxLines = 2)
                            if (list.any { it.cheat }) Text(" 🍕", fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) { SmallSecondary("✏️ Open day") { nav.date.value = selected; nav.stack.clear(); nav.tab.value = Tab.TODAY } }
                    if (selected != today() && es.isNotEmpty()) Box(Modifier.weight(1f)) { SmallSecondary("⧉ Copy to today") { copyConfirm = true } }
                }
                Spacer(Modifier.height(8.dp))
                SmallSecondary(if (cheat) "Unmark cheat day" else "🍕 Mark as cheat day") { if (cheat) store.setCheatDay(selected, false) else cheatNote = true }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (cheatNote) {
        InputDialog("Mark as cheat day", "Note (optional): wedding, festival…", "", onDismiss = { cheatNote = false; store.setCheatDay(selected, true) }) { store.setCheatDay(selected, true, it) }
    }
    if (copyConfirm) {
        ConfirmDialog("Copy to today?", "Everything from ${dayTitle(selected)} is added to today's meals.", "Copy", onDismiss = { copyConfirm = false }) {
            store.copyDay(selected, today()); nav.date.value = today(); nav.stack.clear(); nav.tab.value = Tab.TODAY
        }
    }
}

// ======================= Weight =======================

@Composable
fun WeightScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    var range by rememberSaveable { mutableStateOf(1) }
    var goalDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    val days = listOf(7L, 30L, 90L, 3650L)[range]
    val from = LocalDate.now().minusDays(days)
    val all = data.weights.sortedBy { it.date }
    val trend = Smart.trend(all)
    val shown = all.filter { !LocalDate.parse(it.date).isBefore(from) }
    val shownTrend = trend.filter { !it.first.isBefore(from) }
    val cur = trend.lastOrNull()?.second
    val weekAgo = trend.lastOrNull { it.first <= LocalDate.now().minusDays(7) }?.second
    val rate = if (cur != null && weekAgo != null) cur - weekAgo else null
    val goalW = data.profile?.goalWeightKg

    Column(Modifier.fillMaxSize()) {
        TopBar("Weight & body", onBack = { nav.pop() }) { app.unavu.ui.IconCircle("＋") { nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT") } }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("CURRENT (TREND)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.ink2)
                        Text(cur?.let { "%.1f kg".format(it) } ?: "–", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        rate?.let { StatusBadge("%s %.1f kg / week".format(if (it <= 0) "↓" else "↑", kotlin.math.abs(it)), if ((data.profile?.goalKcal ?: 0) < 0 == (it <= 0)) Status.OK else Status.LOW) }
                        val eta = if (goalW != null && cur != null && rate != null && rate != 0.0 && (goalW - cur) / rate > 0) "~${((goalW - cur) / rate).roundToInt()} weeks" else null
                        Text(
                            goalW?.let { "Goal %.1f kg".format(it) + (eta?.let { e -> " · $e" } ?: "") } ?: "Set a goal weight",
                            fontSize = 11.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp).clickable { goalDialog = true },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Segmented(listOf("1W", "1M", "3M", "All"), range) { range = it }
                Spacer(Modifier.height(8.dp))
                if (shown.size >= 2) {
                    val start = LocalDate.parse(shown.first().date)
                    val span = ChronoUnit.DAYS.between(start, LocalDate.parse(shown.last().date)).coerceAtLeast(1).toFloat()
                    fun x(d: LocalDate) = ChronoUnit.DAYS.between(start, d) / span
                    LineChart(shown.map { x(LocalDate.parse(it.date)) to it.kg }, shownTrend.map { x(it.first) to it.second })
                    Text("● weigh-ins   ━ trend (smooths water weight)", fontSize = 10.5.sp, color = c.ink2)
                } else {
                    Text("Log at least 2 weigh-ins to see the chart. Weigh in the morning, before breakfast.", fontSize = 12.sp, color = c.ink2)
                }
            }
            if (all.isNotEmpty()) {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    all.reversed().take(30).forEachIndexed { i, w ->
                        if (i > 0) Divider()
                        val prev = all.getOrNull(all.indexOf(w) - 1)
                        val diff = prev?.let { w.kg - it.kg }
                        Row(Modifier.fillMaxWidth().clickable { deleting = w.date }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(dayTitle(w.date), modifier = Modifier.weight(1f))
                            w.waistCm?.let { Text("waist ${it.roundToInt()} cm   ", fontSize = 11.sp, color = c.ink2) }
                            Text("%.1f kg".format(w.kg), fontWeight = FontWeight.Bold)
                            diff?.let { Text("  %+.1f".format(it), fontSize = 11.sp, color = if (it <= 0) c.leafInk else c.ink2) }
                        }
                    }
                }
                Text("Tap an entry to delete it.", fontSize = 11.sp, color = c.ink2)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (goalDialog) {
        InputDialog("Goal weight", "kg", goalW?.let { "%.1f".format(it) } ?: "", numeric = true, onDismiss = { goalDialog = false }) { v ->
            v.toDoubleOrNull()?.let { g -> store.update { d -> d.copy(profile = d.profile?.copy(goalWeightKg = g)) } }
        }
    }
    deleting?.let { d -> ConfirmDialog("Delete weigh-in?", "Remove the entry for ${dayTitle(d)}.", "Delete", true, { deleting = null }) { store.deleteWeight(d) } }
}

@Suppress("unused")
private fun keepPage(p: Page) = p
@Suppress("unused")
private val keepMod = Modifier.fillMaxHeight().border(0.dp, Color.Transparent)
