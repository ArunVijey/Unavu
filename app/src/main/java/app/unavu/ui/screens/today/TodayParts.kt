package app.unavu.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.AppData
import app.unavu.data.DATE
import app.unavu.data.LogEntry
import app.unavu.data.Meal
import app.unavu.data.Nutr
import app.unavu.data.NutrientDef
import app.unavu.data.Store
import app.unavu.data.Targets
import app.unavu.data.food
import app.unavu.data.isCheatDay
import app.unavu.data.today
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
import app.unavu.ui.LocalFeedback
import app.unavu.ui.Page
import app.unavu.ui.Status
import app.unavu.ui.theme.edge
import androidx.compose.ui.unit.Dp
import app.unavu.ui.nav
import app.unavu.ui.pressScale
import app.unavu.ui.screens.Emoji
import app.unavu.ui.screens.fmtNum
import app.unavu.ui.theme.U
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun Modifier.edgeIfSketch(color: Color, radius: Dp, seed: Int, enabled: Boolean = true): Modifier =
    if (U.sketch && enabled) this.edge(color, radius, 1.5.dp, seed = seed) else this

/** Runs [block] and shows a snackbar with Undo that restores the data from before it ran. */
@Composable
fun rememberUndo(store: Store): (String, () -> Unit) -> Unit {
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val undo = stringResource(R.string.d_undo)
    return remember(store, feedback, haptic, undo) {
        { message, block ->
            val before = store.data.value
            block()
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            feedback?.show(message, undo) { store.restoreSnapshot(before) }
        }
    }
}

fun mealItemText(e: LogEntry): String {
    val f = e.food() ?: return "?"
    return if (e.unit == "piece" || e.unit == "egg") "${app.unavu.data.fmtQty(e.qty)} ${f.name}" else f.name
}

@Composable
fun NutrientChip(d: NutrientDef, have: Double, target: Double, st: Status, modifier: Modifier, onClick: () -> Unit) {
    val c = U.c
    val (bg, fg) = when (st) { Status.OK -> c.leafS to c.leafInk; Status.LOW -> c.turS to c.turInk; else -> c.chiS to c.chiInk }
    val sym = when (st) { Status.OK -> "✓"; Status.LOW -> "↓"; else -> "↑" }
    Column(
        modifier.edgeIfSketch(fg, 12.dp, d.key.length)
            .heightIn(min = 48.dp).pressScale()
            .clip(RoundedCornerShape(14.dp)).background(if (U.sketch) fg.copy(alpha = 0.12f) else bg)
            .semantics(mergeDescendants = true) {}.clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text("$sym ${d.name}", color = fg, style = U.t.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${fmtNum(have)}/${fmtNum(target)}", color = fg, style = U.t.caption, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun WeekStrip(data: AppData, selected: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val c = U.c
    val sel = LocalDate.parse(selected)
    val monday = sel.with(DayOfWeek.MONDAY)
    val todayD = LocalDate.now()
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            ArrowButton(Arrow.LEFT) { onSelect(sel.minusWeeks(1).format(DATE)) }
            Text(stringResource(R.string.d_week_of, "${monday.dayOfMonth} ${monday.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"), style = U.t.label, color = c.ink2)
            ArrowButton(Arrow.RIGHT) { if (sel.plusWeeks(1) <= todayD.with(DayOfWeek.SUNDAY)) onSelect(minOf(sel.plusWeeks(1), todayD).format(DATE)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (0..6).forEach { i ->
                val d = monday.plusDays(i.toLong())
                val ds = d.format(DATE)
                val on = ds == selected
                val logged = remember(data.entries, ds) { data.entries.any { it.date == ds } }
                val cheat = data.isCheatDay(ds)
                val future = d.isAfter(todayD)
                val label = "${d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${d.dayOfMonth}"
                val desc = when { cheat -> stringResource(R.string.d_day_cheat, label); logged -> stringResource(R.string.d_day_logged, label); else -> label }
                Column(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .edgeIfSketch(c.ink, 10.dp, i, enabled = on)
                        .clip(RoundedCornerShape(12.dp)).background(if (on) (if (U.sketch) c.acc else c.ink) else Color.Transparent)
                        .semantics(mergeDescendants = true) { contentDescription = desc; this.selected = on }
                        .clickable(enabled = !future, role = Role.Button) { onSelect(ds) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    val fg = if (on) (if (U.sketch) c.onAcc else c.bg) else c.ink2
                    Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH), style = U.t.caption, fontWeight = FontWeight.SemiBold, color = fg)
                    Text("${d.dayOfMonth}", style = U.t.label, fontWeight = FontWeight.Bold, color = if (on) fg else if (future) c.ink2 else c.ink)
                    if (cheat) Text("🍕", style = U.t.caption)
                    else Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (logged) (if (on) c.bg else c.leaf) else Color.Transparent))
                }
            }
        }
    }
}

@Composable
fun NextUpCard(date: String, byMeal: Map<Meal, List<LogEntry>>, left: Int, total: Nutr, t: Targets) {
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
                Text(stringResource(R.string.d_next_up_title, next.label, left.coerceAtLeast(0)), style = U.t.body, fontWeight = FontWeight.Bold)
                Text(
                    if (proteinLow) stringResource(R.string.d_next_up_low_protein) else stringResource(R.string.d_next_up_tap, next.label.lowercase()),
                    style = U.t.caption, color = c.ink2,
                )
            }
            Text(stringResource(R.string.d_log), color = c.onAcc, style = U.t.label, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(c.acc).padding(horizontal = 10.dp, vertical = 7.dp))
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = U.t.caption, color = U.c.ink2, fontWeight = FontWeight.SemiBold)
    }
}
