package app.unavu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.Smart
import app.unavu.data.WeightEntry
import app.unavu.data.deleteWeight
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.IconCircle
import app.unavu.ui.InputDialog
import app.unavu.ui.LineChart
import app.unavu.ui.LocalData
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.Segmented
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private class WeighRow(val w: WeightEntry, val diff: Double?)

@Composable
fun WeightScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    var range by rememberSaveable { mutableIntStateOf(1) }
    var goalDialog by rememberSaveable { mutableStateOf(false) }
    val undo = rememberUndoer(stringResource(R.string.g_undo)) { store.restoreSnapshot(it) }
    val days = listOf(7L, 30L, 90L, 3650L)[range]
    val from = LocalDate.now().minusDays(days)
    val all = remember(data.weights) { data.weights.sortedBy { it.date } }
    val trend = remember(all) { Smart.trend(all) }
    val shown = all.filter { !LocalDate.parse(it.date).isBefore(from) }
    val shownTrend = trend.filter { !it.first.isBefore(from) }
    val cur = trend.lastOrNull()?.second
    val weekAgo = trend.lastOrNull { it.first <= LocalDate.now().minusDays(7) }?.second
    val rate = if (cur != null && weekAgo != null) cur - weekAgo else null
    val goalW = data.profile?.goalWeightKg
    val rows = remember(all) { all.mapIndexed { i, w -> WeighRow(w, all.getOrNull(i - 1)?.let { w.kg - it.kg }) }.reversed().take(30) }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_wt_title), onBack = { nav.pop() }) {
            IconCircle("＋", contentDescription = stringResource(R.string.g_wt_add)) { nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT") }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "summary", contentType = "card") {
                AppCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.g_wt_current), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2)
                            Text(cur?.let { stringResource(R.string.g_kg, "%.1f".format(it)) } ?: "–", style = U.t.display)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            rate?.let {
                                StatusBadge(
                                    stringResource(if (it <= 0) R.string.g_wt_rate_down else R.string.g_wt_rate_up, "%.1f".format(abs(it))),
                                    if ((data.profile?.goalKcal ?: 0) < 0 == (it <= 0)) Status.OK else Status.LOW,
                                )
                            }
                            val eta = if (goalW != null && cur != null && rate != null && rate != 0.0 && (goalW - cur) / rate > 0) stringResource(R.string.g_wt_eta, ((goalW - cur) / rate).roundToInt()) else null
                            Text(
                                goalW?.let { stringResource(R.string.g_wt_goal, "%.1f".format(it)) + (eta?.let { e -> " · $e" } ?: "") } ?: stringResource(R.string.g_wt_set_goal),
                                style = U.t.label, color = c.ink2,
                                modifier = Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { goalDialog = true }.padding(top = 12.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Segmented(listOf("1W", "1M", "3M", stringResource(R.string.g_wt_all)), range) { range = it }
                    Spacer(Modifier.height(8.dp))
                    if (shown.size >= 2) {
                        val start = LocalDate.parse(shown.first().date)
                        val span = ChronoUnit.DAYS.between(start, LocalDate.parse(shown.last().date)).coerceAtLeast(1).toFloat()
                        fun x(d: LocalDate) = ChronoUnit.DAYS.between(start, d) / span
                        val desc = stringResource(R.string.g_wt_chart_desc, shown.size, "%.1f".format(shown.first().kg), "%.1f".format(shown.last().kg), "%.1f".format(shown.minOf { it.kg }), "%.1f".format(shown.maxOf { it.kg }))
                        LineChart(shown.map { x(LocalDate.parse(it.date)) to it.kg }, shownTrend.map { x(it.first) to it.second }, modifier = Modifier.semantics { contentDescription = desc })
                        Text(stringResource(R.string.g_wt_legend), style = U.t.caption, color = c.ink2)
                    } else {
                        Text(stringResource(R.string.g_wt_need_two), style = U.t.bodyS, color = c.ink2)
                        if (all.isEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            SmallSecondary(stringResource(R.string.g_wt_log_first)) { nav.quickAdd.value = QuickAddRequest(mode = "WEIGHT") }
                        }
                    }
                }
            }
            items(rows, key = { "w-" + it.w.date }, contentType = { "weigh" }) { r ->
                val w = r.w
                val dateLabel = dayTitle(w.date)
                val del = stringResource(R.string.g_wt_delete_desc, dateLabel)
                val msg = stringResource(R.string.g_wt_deleted, dateLabel)
                Column(Modifier.animateItem()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f).padding(vertical = 6.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                            Text(dateLabel, style = U.t.body, modifier = Modifier.weight(1f))
                            w.waistCm?.let { Text(stringResource(R.string.g_wt_waist, it.roundToInt()) + "   ", style = U.t.caption, color = c.ink2) }
                            Text(stringResource(R.string.g_kg, "%.1f".format(w.kg)), style = U.t.body, fontWeight = FontWeight.Bold)
                            r.diff?.let { Text("  %+.1f".format(it), style = U.t.caption, color = if (it <= 0) c.leafInk else c.ink2) }
                        }
                        IconCircle("✕", contentDescription = del) {
                            val before = store.data.value
                            store.deleteWeight(w.date)
                            undo.announce(msg, before, reject = true)
                        }
                    }
                    Divider()
                }
            }
        }
    }
    if (goalDialog) {
        InputDialog(stringResource(R.string.g_wt_goal_dialog), "kg", goalW?.let { "%.1f".format(it) } ?: "", numeric = true, onDismiss = { goalDialog = false }) { v ->
            v.toDoubleOrNull()?.takeIf { it in 20.0..300.0 }?.let { g -> store.update { d -> d.copy(profile = d.profile?.copy(goalWeightKg = g)) } }
        }
    }
}
