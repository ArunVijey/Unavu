package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.AppData
import app.unavu.data.Combo
import app.unavu.data.DATE
import app.unavu.data.FoodDb
import app.unavu.data.LogEntry
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.NewItem
import app.unavu.data.NutrientDef
import app.unavu.data.Nutr
import app.unavu.data.Smart
import app.unavu.data.Targets
import app.unavu.data.addWater
import app.unavu.data.copyDay
import app.unavu.data.copyMeal
import app.unavu.data.deleteEntry
import app.unavu.data.deleteMeal
import app.unavu.data.entriesOn
import app.unavu.data.food
import app.unavu.data.isCheatDay
import app.unavu.data.liquidsMl
import app.unavu.data.newKey
import app.unavu.data.nutr
import app.unavu.data.portionText
import app.unavu.data.saveCombo
import app.unavu.data.setCheatDay
import app.unavu.data.setMealCheat
import app.unavu.data.targets
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.data.updateEntry
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
import app.unavu.ui.Divider
import app.unavu.ui.IconCircle
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.MealPicker
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SectionLabel
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TamilText
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.AppColors
import app.unavu.ui.theme.U
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun mealColors(c: AppColors): List<Color> = listOf(c.tur, c.leaf, c.m3, c.blue)

fun statusOf(d: NutrientDef, have: Double, target: Double): Status = if (d.limit) {
    if (have > target) Status.OVER else Status.OK
} else {
    if (have >= target * 0.9) Status.OK else Status.LOW
}

/** Whole numbers from 10 up (53.3 → 53), one decimal below 10 (2.4 mg iron). */
fun fmtNum(v: Double): String = if (v >= 10 || v % 1.0 == 0.0) "%,d".format(v.roundToInt()) else "%.1f".format(v)

fun dayTitle(date: String): String {
    val d = LocalDate.parse(date)
    return when (date) {
        today() -> "Today"
        LocalDate.now().minusDays(1).format(DATE) -> "Yesterday"
        else -> "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
    }
}

@Composable
fun TodayScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val date = nav.date.value
    val t = data.targets() ?: return
    val entries = data.entriesOn(date)
    val total = entries.total()
    val eaten = total.kcal.roundToInt()
    val left = t.kcal - eaten
    val split = data.settings.mealSplit
    val colors = mealColors(c)
    val byMeal = Meal.entries.associateWith { m -> entries.filter { it.meal == m } }
    val cheatDay = data.isCheatDay(date)

    var nutrientLook by remember { mutableStateOf<NutrientDef?>(null) }
    var mealLook by remember { mutableStateOf<Meal?>(null) }
    var budgetLook by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<LogEntry?>(null) }
    var copyFrom by remember { mutableStateOf<Meal?>(null) }
    var comboFrom by remember { mutableStateOf<Meal?>(null) }
    var menu by remember { mutableStateOf(false) }
    var cheatNote by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(dayTitle(date)) {
            if (date != today()) IconCircle("⟲") { nav.date.value = today() }
            IconCircle("📅") { nav.push(Page.History) }
            Box {
                IconCircle("⋯") { menu = true }
                DropdownMenu(menu, { menu = false }, containerColor = c.sf) {
                    DropdownMenuItem({ Text(if (cheatDay) "Unmark cheat day" else "🍕 Mark as cheat day") }, {
                        menu = false
                        if (cheatDay) store.setCheatDay(date, false) else cheatNote = true
                    })
                    if (date != today()) DropdownMenuItem({ Text("⧉ Copy this day to today") }, { menu = false; store.copyDay(date, today()); nav.date.value = today() })
                    DropdownMenuItem({ Text("⚖️ Log weight") }, { menu = false; nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT") })
                    DropdownMenuItem({ Text("💧 Liquids") }, { menu = false; nav.push(Page.Liquids) })
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WeekStrip(data, date) { nav.date.value = it }

            data.smart.pendingBurn?.let { burn ->
                val newKcal = (burn + (data.profile?.goalKcal ?: 0)).coerceAtLeast(1200)
                AppCard(color = c.leafS, border = null, onClick = { nav.push(Page.SmartTarget) }) {
                    Text("🧠 New target suggested: $newKcal kcal", fontWeight = FontWeight.Bold, color = c.leafInk)
                    Text("Based on your logs and weight trend (now ${t.kcal}). Tap to review.", fontSize = 12.sp, color = c.ink2)
                }
            }
            if (cheatDay) {
                AppCard(color = c.turS, border = null) {
                    Text("🍕 Cheat day${data.dayNotes[date]?.let { " · $it" } ?: ""}", fontWeight = FontWeight.Bold, color = c.turInk)
                    Text("Not counted in averages and insights.", fontSize = 12.sp, color = c.ink2)
                }
            }

            // ---------- Budget hero ----------
            AppCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text(if (left >= 0) "YOU CAN STILL EAT" else "OVER TARGET BY", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = c.ink2, letterSpacing = 0.8.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${abs(left)}", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = if (left >= 0) c.ink else c.chi, lineHeight = 58.sp)
                            Text(" kcal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink2, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                    Text("$eaten eaten\nof ${t.kcal}", fontSize = 13.sp, color = c.ink2, lineHeight = 18.sp, textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(8.dp)).background(c.sf2).clickable { budgetLook = true }) {
                    val parts = Meal.entries.map { m -> (byMeal.getValue(m).total().kcal / t.kcal).toFloat() }
                    val scale = if (parts.sum() > 1f) 1f / parts.sum() else 1f
                    parts.forEachIndexed { i, p -> if (p > 0f) Box(Modifier.fillMaxHeight().weight(p * scale).background(colors[i])) }
                    val rest = 1f - parts.sum() * scale
                    if (rest > 0.0001f) Spacer(Modifier.weight(rest))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Meal.entries.forEachIndexed { i, m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(colors[i]))
                            Text(" ${m.label}", fontSize = 11.5.sp, color = c.ink2, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                val chips = data.settings.homeChips.mapNotNull { k -> NUTRIENTS.firstOrNull { it.key == k } }
                chips.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        row.forEach { d ->
                            val have = d.value(total); val target = d.target(t)
                            NutrientChip(d, have, target, statusOf(d, have, target), Modifier.weight(1f)) { nutrientLook = d }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            NextUpCard(date, byMeal, left, total, t)

            // ---------- Liquids ----------
            val (water, drinks) = data.liquidsMl(date)
            val goal = data.settings.waterGoalMl
            AppCard(onClick = { nav.push(Page.Liquids) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("💧 Liquids  ${"%.1f".format((water + drinks) / 1000.0)} L", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Water ${"%.2f".format(water / 1000.0)} / ${"%.1f".format(goal / 1000.0)} L · drinks ${drinks} ml", fontSize = 11.5.sp, color = c.ink2)
                    }
                    Text(
                        "+ glass", color = c.blue, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.blueS).clickable { store.addWater(date, 250) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Track(water / goal.toFloat(), c.blue, height = 8.dp)
            }

            // ---------- Meals ----------
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                Meal.entries.forEachIndexed { i, m ->
                    if (i > 0) Divider()
                    val list = byMeal.getValue(m)
                    val kcal = list.total().kcal.roundToInt()
                    Row(
                        Modifier.fillMaxWidth().clickable { if (list.isEmpty()) nav.push(Page.Plate(m, date)) else mealLook = m }.padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(colors[i]))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(m.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (list.any { it.cheat }) StatusBadge("🍕 Cheat meal", Status.LOW)
                            }
                            Text(
                                if (list.isEmpty()) "Not logged · tap to build a plate" else list.joinToString(" · ") { e -> mealItemText(e) },
                                fontSize = 13.sp, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Text(if (kcal > 0) "$kcal" else "+", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (kcal > 0) c.ink else c.ink2)
                    }
                }
            }

            // ---------- Weight ----------
            val lastW = data.weights.lastOrNull()
            val trend = Smart.trend(data.weights).lastOrNull()?.second
            AppCard(onClick = { nav.push(Page.Weight) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("⚖️ Weight  ${lastW?.let { "%.1f kg".format(it.kg) } ?: "–"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (lastW == null) "Log your first weigh-in" else "Trend ${"%.1f".format(trend)} kg · last ${dayTitle(lastW.date).lowercase()}",
                            fontSize = 11.5.sp, color = c.ink2,
                        )
                    }
                    Text(
                        "+ weigh-in", color = c.leafInk, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.leafS).clickable { nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT") }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }

    // ---------- quick looks ----------
    nutrientLook?.let { d -> NutrientLook(d, entries, byMeal, total, t) { nutrientLook = null } }

    mealLook?.let { m ->
        val list = byMeal.getValue(m)
        val kcal = list.total().kcal.roundToInt()
        val share = (split[m.ordinal] * t.kcal).roundToInt()
        val isCheat = list.isNotEmpty() && list.all { it.cheat }
        QuickLook(
            onDismiss = { mealLook = null },
            actions = listOf(
                MenuAction("Add more", "＋") { nav.push(Page.Plate(m, date)) },
                MenuAction("Copy to…", "⧉") { copyFrom = m },
                MenuAction("Save as combo", "⭐") { comboFrom = m },
                MenuAction(if (isCheat) "Unmark cheat meal" else "Mark as cheat meal", "🍕") { store.setMealCheat(date, m, !isCheat) },
                MenuAction("Delete meal", "🗑", destructive = true) { store.deleteMeal(date, m) },
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${m.emoji} ${m.label}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusBadge(if (kcal > share) "${kcal - share} over share" else "${share - kcal} under share", if (kcal > share) Status.OVER else Status.OK)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text("$kcal", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("  kcal · share $share", fontSize = 13.sp, color = c.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
            }
            Spacer(Modifier.height(6.dp))
            Track(kcal / share.toFloat(), if (kcal > share) c.chi else colors[m.ordinal])
            val mt = list.total()
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Protein" to mt.protein, "Carbs" to mt.carbs, "Fat" to mt.fat).forEach { (n, v) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.sf2).padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(n, fontSize = 10.5.sp, color = c.ink2, fontWeight = FontWeight.SemiBold)
                        Text("${v.roundToInt()} g", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            SectionLabel("On the plate · tap to change")
            list.forEach { e ->
                val f = e.food()
                Row(Modifier.fillMaxWidth().clickable { mealLook = null; editEntry = e }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${f?.emoji} ${f?.name} · ${e.portionText()}", fontSize = 12.5.sp)
                        f?.choiceSummary(e.choices)?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 10.5.sp, color = c.leafInk) }
                    }
                    Text("${e.nutr().kcal.roundToInt()}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    Text("  ✕", fontSize = 12.sp, color = c.ink2, modifier = Modifier.clip(CircleShape).clickable { store.deleteEntry(e.id) }.padding(4.dp))
                }
            }
        }
    }

    if (budgetLook) {
        QuickLook(onDismiss = { budgetLook = false }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Today's budget", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusBadge("${(eaten * 100.0 / t.kcal).roundToInt()}% used", if (left >= 0) Status.OK else Status.OVER)
            }
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text("${abs(left)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (left >= 0) "  of ${t.kcal} kcal left" else "  kcal over ${t.kcal}", fontSize = 13.sp, color = c.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
            }
            SectionLabel("Meal vs its share")
            Meal.entries.forEachIndexed { i, m ->
                val k = byMeal.getValue(m).total().kcal.roundToInt(); val s = (split[i] * t.kcal).roundToInt()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text(m.label, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                    Text("$k / $s", fontSize = 12.5.sp, color = if (k > s) c.chi else c.ink2)
                }
                Track(k / s.toFloat(), if (k > s) c.chi else colors[i], Modifier.padding(top = 3.dp), height = 6.dp)
            }
            SectionLabel("How your target is made")
            val p = data.profile!!
            SourceRow("Body at rest (BMR)", "${t.bmr}")
            SourceRow(if (data.smart.appliedBurn != null) "Your real burn (smart target)" else "+ Daily activity", if (data.smart.appliedBurn != null) "${t.maintenance}" else "${t.maintenance - t.bmr}")
            SourceRow(if (p.goalKcal < 0) "− Weight-loss goal" else if (p.goalKcal > 0) "+ Gain goal" else "Maintain", "${p.goalKcal}")
        }
    }

    editEntry?.let { e ->
        val f = e.food()
        if (f != null) {
            CustomiseSheet(f, NewItem(e.foodId, e.qty, e.unit, e.choices), saveLabel = "Save", onDismiss = { editEntry = null }, onRemove = { store.deleteEntry(e.id); editEntry = null }) { ni ->
                store.updateEntry(e.copy(qty = ni.qty, unit = ni.unit, choices = ni.choices)); editEntry = null
            }
        } else editEntry = null
    }

    copyFrom?.let { from ->
        var target by remember { mutableStateOf<Meal?>(null) }
        AlertDialog(
            onDismissRequest = { copyFrom = null }, containerColor = c.sf,
            title = { Text("Copy ${from.label.lowercase()} to…", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Today's meal:", color = c.ink2, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    MealPicker(target) { target = it }
                }
            },
            confirmButton = { TextButton({ store.copyMeal(date, from, today(), target!!); nav.date.value = today(); copyFrom = null }, enabled = target != null) { Text("Copy", color = c.leafInk, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ copyFrom = null }) { Text("Cancel", color = c.ink2) } },
        )
    }

    comboFrom?.let { m ->
        InputDialog("Save as combo", "Name", "My usual ${m.label.lowercase()}", onDismiss = { comboFrom = null }) { name ->
            val items = byMeal.getValue(m).map { app.unavu.data.ComboItem(it.foodId, it.qty, it.unit, it.choices) }
            store.saveCombo(Combo(newKey("c_"), name.trim(), m.emoji, m, items))
        }
    }

    if (cheatNote) {
        InputDialog("Mark as cheat day", "Note (optional): wedding, festival…", "", onDismiss = { cheatNote = false; store.setCheatDay(date, true) }) { note ->
            store.setCheatDay(date, true, note)
        }
    }
}

@Composable
private fun NutrientLook(d: NutrientDef, entries: List<LogEntry>, byMeal: Map<Meal, List<LogEntry>>, total: Nutr, t: Targets, onDismiss: () -> Unit) {
    val c = U.c
    val colors = mealColors(c)
    val have = d.value(total); val target = d.target(t); val st = statusOf(d, have, target)
    QuickLook(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StatusBadge(
                when (st) { Status.OK -> if (d.limit) "Within limit" else "Enough"; Status.LOW -> "Low · ${(have / target * 100).roundToInt()}%"; else -> "Over limit" },
                st,
            )
        }
        TamilText(d.tamil)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text(fmtNum(have), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("  / ${fmtNum(target)} ${d.unit} ${if (d.limit) "limit" else "target"}", fontSize = 13.sp, color = c.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(6.dp))
        Track((have / target).toFloat(), when (st) { Status.OK -> c.leaf; Status.LOW -> c.tur; else -> c.chi })
        SectionLabel("By meal")
        val perMeal = Meal.entries.map { m -> d.value(byMeal.getValue(m).total()) }
        val maxV = perMeal.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            perMeal.forEachIndexed { i, v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height((22 * (v / maxV)).coerceAtLeast(2.0).dp).clip(RoundedCornerShape(5.dp)).background(colors[i].copy(alpha = if (v > 0) 1f else 0.25f)))
                    Text(Meal.entries[i].label.take(6), fontSize = 10.sp, color = c.ink2)
                }
            }
        }
        SectionLabel("Top sources")
        val sources = entries.groupBy { it.foodId }.map { (_, es) -> es.first() to es.sumOf { d.value(it.nutr()) } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(4)
        if (sources.isEmpty()) Text("Nothing logged yet.", color = c.ink2, fontSize = 12.sp)
        sources.forEach { (e, v) -> SourceRow("${e.food()?.emoji} ${e.food()?.name}", "${fmtNum(v)} ${d.unit}") }
        Text(
            "💡 ${d.tip}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.leafInk,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.leafS).padding(10.dp),
        )
    }
}

private fun mealItemText(e: LogEntry): String {
    val f = e.food() ?: return "?"
    return if (e.unit == "piece" || e.unit == "egg") "${app.unavu.data.fmtQty(e.qty)} ${f.name}" else f.name
}

@Composable
private fun NutrientChip(d: NutrientDef, have: Double, target: Double, st: Status, modifier: Modifier, onClick: () -> Unit) {
    val c = U.c
    val (bg, fg) = when (st) { Status.OK -> c.leafS to c.leafInk; Status.LOW -> c.turS to c.turInk; else -> c.chiS to c.chi }
    val sym = when (st) { Status.OK -> "✓"; Status.LOW -> "↓"; else -> "↑" }
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp)) {
        Text("$sym ${d.name}", color = fg, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${fmtNum(have)}/${fmtNum(target)}", color = fg.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun WeekStrip(data: AppData, selected: String, onSelect: (String) -> Unit) {
    val c = U.c
    val sel = LocalDate.parse(selected)
    val monday = sel.with(DayOfWeek.MONDAY)
    val todayD = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        ArrowButton(Arrow.LEFT) { onSelect(sel.minusWeeks(1).format(DATE)) }
        (0..6).forEach { i ->
            val d = monday.plusDays(i.toLong())
            val ds = d.format(DATE)
            val on = ds == selected
            val logged = data.entries.any { it.date == ds }
            val future = d.isAfter(todayD)
            Column(
                Modifier.width(34.dp).clip(RoundedCornerShape(12.dp)).background(if (on) c.ink else Color.Transparent)
                    .clickable(enabled = !future) { onSelect(ds) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) c.bg else c.ink2)
                Text("${d.dayOfMonth}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (on) c.bg else if (future) c.ink2 else c.ink)
                if (data.isCheatDay(ds)) Text("🍕", fontSize = 7.sp)
                else Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (logged) (if (on) c.bg else c.leaf) else Color.Transparent))
            }
        }
        ArrowButton(Arrow.RIGHT) { if (sel.plusWeeks(1) <= todayD.with(DayOfWeek.SUNDAY)) onSelect(minOf(sel.plusWeeks(1), todayD).format(DATE)) }
    }
}

@Composable
private fun NextUpCard(date: String, byMeal: Map<Meal, List<LogEntry>>, left: Int, total: Nutr, t: Targets) {
    val c = U.c
    val nav = nav
    if (date != today()) return
    val now = Meal.forHour(LocalTime.now().hour)
    val next = Meal.entries.drop(now.ordinal).firstOrNull { byMeal.getValue(it).isEmpty() } ?: return
    val proteinLow = total.protein < t.protein * 0.6
    AppCard(color = c.turS, border = null, onClick = { nav.push(Page.Plate(next, date)) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Emoji(next.emoji, 44)
            Column(Modifier.weight(1f)) {
                Text("${next.label}: ${left.coerceAtLeast(0)} kcal left", fontWeight = FontWeight.Bold)
                Text(
                    if (proteinLow) "Protein is low. Add egg, chicken, fish, sundal or dal." else "Tap to build your ${next.label.lowercase()} plate.",
                    fontSize = 11.5.sp, color = c.ink2,
                )
            }
            Text("+ Log", color = c.onAcc, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.acc).padding(horizontal = 10.dp, vertical = 7.dp))
        }
    }
}

@Suppress("unused")
private fun keepFoodDb() = FoodDb.loaded
