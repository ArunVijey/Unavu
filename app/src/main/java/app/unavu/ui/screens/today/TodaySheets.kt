package app.unavu.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.AppData
import app.unavu.data.LogEntry
import app.unavu.data.Meal
import app.unavu.data.Nutr
import app.unavu.data.NutrientDef
import app.unavu.data.Targets
import app.unavu.data.food
import app.unavu.data.nutr
import app.unavu.data.portionText
import app.unavu.data.total
import app.unavu.ui.MealPicker
import app.unavu.ui.SectionLabel
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TamilText
import app.unavu.ui.Track
import app.unavu.ui.screens.MenuAction
import app.unavu.ui.screens.QuickLook
import app.unavu.ui.screens.RemoveButton
import app.unavu.ui.screens.SourceRow
import app.unavu.ui.screens.fmtNum
import app.unavu.ui.screens.mealColors
import app.unavu.ui.screens.statusOf
import app.unavu.ui.theme.U
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NutrientLook(d: NutrientDef, entries: List<LogEntry>, byMeal: Map<Meal, List<LogEntry>>, total: Nutr, t: Targets, onDismiss: () -> Unit) {
    val c = U.c
    val colors = mealColors(c)
    val have = d.value(total); val target = d.target(t); val st = statusOf(d, have, target)
    QuickLook(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.name, style = U.t.title, modifier = Modifier.weight(1f))
            StatusBadge(
                when (st) {
                    Status.OK -> stringResource(if (d.limit) R.string.d_within_limit else R.string.d_enough)
                    Status.LOW -> stringResource(R.string.d_low_pct, (have / target * 100).roundToInt())
                    else -> stringResource(R.string.d_over_limit)
                },
                st,
            )
        }
        TamilText(d.tamil)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text(fmtNum(have), style = U.t.display, color = c.ink)
            Text(
                stringResource(R.string.d_limit_target, fmtNum(target), d.unit, stringResource(if (d.limit) R.string.d_limit else R.string.d_target)),
                style = U.t.bodyS, color = c.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Track((have / target).toFloat(), when (st) { Status.OK -> c.leaf; Status.LOW -> c.tur; else -> c.chi }, contentDescription = "${d.name} ${fmtNum(have)} / ${fmtNum(target)} ${d.unit}")
        SectionLabel(stringResource(R.string.d_by_meal))
        val perMeal = Meal.entries.map { m -> d.value(byMeal.getValue(m).total()) }
        val maxV = perMeal.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val summary = Meal.entries.mapIndexed { i, m -> "${m.label} ${fmtNum(perMeal[i])} ${d.unit}" }.joinToString(", ")
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp).semantics(mergeDescendants = true) { contentDescription = summary },
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom,
        ) {
            perMeal.forEachIndexed { i, v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height((22 * (v / maxV)).coerceAtLeast(2.0).dp).clip(RoundedCornerShape(5.dp)).background(colors[i].copy(alpha = if (v > 0) 1f else 0.25f)))
                    Text(Meal.entries[i].label.take(6), style = U.t.caption, color = c.ink2, maxLines = 1)
                }
            }
        }
        SectionLabel(stringResource(R.string.d_top_sources))
        val sources = entries.groupBy { it.foodId }.map { (_, es) -> es.first() to es.sumOf { d.value(it.nutr()) } }
            .filter { it.second > 0 }.sortedByDescending { it.second }.take(4)
        if (sources.isEmpty()) Text(stringResource(R.string.d_nothing_yet), color = c.ink2, style = U.t.caption)
        sources.forEach { (e, v) -> SourceRow("${e.food()?.emoji} ${e.food()?.name}", "${fmtNum(v)} ${d.unit}") }
        Text(
            "💡 ${d.tip}", style = U.t.caption, fontWeight = FontWeight.SemiBold, color = c.leafInk,
            modifier = Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.leafS).padding(10.dp),
        )
    }
}

@Composable
fun MealLook(
    m: Meal, list: List<LogEntry>, t: Targets, split: List<Double>,
    onDismiss: () -> Unit, onAddMore: () -> Unit, onCopy: () -> Unit, onCombo: () -> Unit,
    onToggleCheat: (Boolean) -> Unit, onDeleteMeal: () -> Unit, onEdit: (LogEntry) -> Unit, onDeleteEntry: (LogEntry) -> Unit,
) {
    val c = U.c
    val colors = mealColors(c)
    val kcal = list.total().kcal.roundToInt()
    val share = (split[m.ordinal] * t.kcal).roundToInt()
    val isCheat = list.isNotEmpty() && list.all { it.cheat }
    QuickLook(
        onDismiss = onDismiss,
        actions = listOf(
            MenuAction(stringResource(R.string.d_ql_add_more), "＋") { onAddMore() },
            MenuAction(stringResource(R.string.d_ql_copy_to), "⧉") { onCopy() },
            MenuAction(stringResource(R.string.d_ql_save_combo), "⭐") { onCombo() },
            MenuAction(stringResource(if (isCheat) R.string.d_ql_unmark_cheat_meal else R.string.d_ql_mark_cheat_meal), "🍕") { onToggleCheat(!isCheat) },
            MenuAction(stringResource(R.string.d_ql_delete_meal), "🗑", destructive = true) { onDeleteMeal() },
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${m.emoji} ${m.label}", style = U.t.title, modifier = Modifier.weight(1f))
            StatusBadge(
                if (kcal > share) stringResource(R.string.d_over_share, kcal - share) else stringResource(R.string.d_under_share, share - kcal),
                if (kcal > share) Status.OVER else Status.OK,
            )
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text("$kcal", style = U.t.display, color = c.ink)
            Text(stringResource(R.string.d_kcal_share, share), style = U.t.bodyS, color = c.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(6.dp))
        Track(kcal / share.toFloat(), if (kcal > share) c.chi else colors[m.ordinal], contentDescription = "${m.label} $kcal / $share kcal")
        val mt = list.total()
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(R.string.d_protein to mt.protein, R.string.d_carbs to mt.carbs, R.string.d_fat to mt.fat).forEach { (n, v) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(c.sf2).padding(horizontal = 8.dp, vertical = 6.dp).semantics(mergeDescendants = true) {}) {
                    Text(stringResource(n), style = U.t.caption, color = c.ink2, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.d_grams, v.roundToInt()), style = U.t.bodyS, fontWeight = FontWeight.Bold)
                }
            }
        }
        SectionLabel(stringResource(R.string.d_on_the_plate))
        list.forEach { e ->
            val f = e.food()
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp).semantics(mergeDescendants = true) {}.clickable(role = Role.Button) { onEdit(e) },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("${f?.emoji} ${f?.name} · ${e.portionText()}", style = U.t.bodyS)
                    f?.choiceSummary(e.choices)?.takeIf { it.isNotBlank() }?.let { Text(it, style = U.t.caption, color = c.leafInk) }
                }
                Text("${e.nutr().kcal.roundToInt()}", style = U.t.bodyS, fontWeight = FontWeight.Bold)
                RemoveButton { onDeleteEntry(e) }
            }
        }
    }
}

@Composable
fun BudgetLook(data: AppData, t: Targets, eaten: Int, left: Int, byMeal: Map<Meal, List<LogEntry>>, onDismiss: () -> Unit) {
    val c = U.c
    val colors = mealColors(c)
    val split = data.settings.mealSplit
    QuickLook(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.d_todays_budget), style = U.t.title, modifier = Modifier.weight(1f))
            StatusBadge(stringResource(R.string.d_pct_used, (eaten * 100.0 / t.kcal).roundToInt()), if (left >= 0) Status.OK else Status.OVER)
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text("${abs(left)}", style = U.t.display, color = c.ink)
            Text(if (left >= 0) stringResource(R.string.d_of_kcal_left, t.kcal) else stringResource(R.string.d_kcal_over, t.kcal), style = U.t.bodyS, color = c.ink2, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 5.dp))
        }
        SectionLabel(stringResource(R.string.d_meal_vs_share))
        Meal.entries.forEachIndexed { i, m ->
            val k = byMeal.getValue(m).total().kcal.roundToInt(); val s = (split[i] * t.kcal).roundToInt()
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(m.label, fontWeight = FontWeight.Bold, style = U.t.bodyS, modifier = Modifier.weight(1f))
                Text("$k / $s", style = U.t.bodyS, color = if (k > s) c.chiInk else c.ink2)
            }
            Track(k / s.toFloat(), if (k > s) c.chi else colors[i], Modifier.padding(top = 3.dp), height = 6.dp, contentDescription = "${m.label} $k / $s kcal")
        }
        SectionLabel(stringResource(R.string.d_how_target))
        val p = data.profile ?: return@QuickLook
        SourceRow(stringResource(R.string.d_bmr), "${t.bmr}")
        SourceRow(
            if (data.smart.appliedBurn != null) stringResource(R.string.d_real_burn) else stringResource(R.string.d_daily_activity),
            if (data.smart.appliedBurn != null) "${t.maintenance}" else "${t.maintenance - t.bmr}",
        )
        SourceRow(
            stringResource(if (p.goalKcal < 0) R.string.d_goal_loss else if (p.goalKcal > 0) R.string.d_goal_gain else R.string.d_goal_maintain),
            "${p.goalKcal}",
        )
    }
}

@Composable
fun CopyMealDialog(from: Meal, onDismiss: () -> Unit, onCopy: (Meal) -> Unit) {
    val c = U.c
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = target?.let { runCatching { Meal.valueOf(it) }.getOrNull() }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = c.sf,
        title = { Text(stringResource(R.string.d_copy_title, from.label.lowercase()), style = U.t.title) },
        text = {
            Column {
                Text(stringResource(R.string.d_todays_meal), color = c.ink2, style = U.t.caption)
                Spacer(Modifier.height(6.dp))
                MealPicker(picked) { target = it.name }
            }
        },
        confirmButton = { TextButton({ picked?.let(onCopy) }, enabled = picked != null) { Text(stringResource(R.string.d_copy), color = c.leafInk, style = U.t.label, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.d_cancel), color = c.ink2, style = U.t.label) } },
    )
}
