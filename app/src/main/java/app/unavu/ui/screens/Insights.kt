package app.unavu.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.DATE
import app.unavu.data.FoodDb
import app.unavu.data.LogEntry
import app.unavu.data.NUTRIENTS
import app.unavu.data.Nutr
import app.unavu.data.Report
import app.unavu.data.Smart
import app.unavu.data.entriesOn
import app.unavu.data.isCheatDay
import app.unavu.data.nutr
import app.unavu.data.targets
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.BarChart
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private class InsightsCalc(
    val dates: List<LocalDate>, val ds: List<String>, val kcals: List<Double>, val logged: List<String>, val counted: List<String>,
    val avg: Nutr, val weekAvgs: List<Double>, val topFoods: List<Pair<String, Double>>,
)

@Composable
fun InsightsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val context = LocalContext.current
    val feedback = LocalFeedback.current
    var range by rememberSaveable { mutableIntStateOf(0) }
    val t = data.targets() ?: return
    val days = listOf(7, 30, 90)[range]
    val today = LocalDate.now()

    val calc = remember(data.entries, data.cheatDays, days, today) {
        val dates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val ds = dates.map { it.format(DATE) }
        val kcals = ds.map { data.entriesOn(it).total().kcal }
        val logged = ds.filter { d -> data.entriesOn(d).isNotEmpty() }
        val counted = logged.filterNot { data.isCheatDay(it) }
        val avg = if (counted.isEmpty()) Nutr() else counted.map { data.entriesOn(it).filterNot { e -> e.cheat }.total() }.fold(Nutr()) { a, b -> a + b } * (1.0 / counted.size)
        val weekAvgs = dates.chunked(7).map { w ->
            w.map { it.format(DATE) }.filter { d -> data.entriesOn(d).isNotEmpty() && !data.isCheatDay(d) }
                .let { l -> if (l.isEmpty()) 0.0 else l.map { d -> data.entriesOn(d).total().kcal }.average() }
        }
        val top = ds.flatMap { data.entriesOn(it) }.groupBy(LogEntry::foodId)
            .map { (id, es) -> (FoodDb.get(id)?.let { "${it.emoji} ${it.name}" } ?: id) to es.sumOf { e -> e.nutr().kcal } }
            .sortedByDescending { it.second }.take(6)
        InsightsCalc(dates, ds, kcals, logged, counted, avg, weekAvgs, top)
    }
    val trend = remember(data.weights) { Smart.trend(data.weights) }
    val wNow = trend.lastOrNull()?.second
    val first = calc.dates.first()
    val wThen = trend.lastOrNull { !it.first.isAfter(first) }?.second ?: trend.firstOrNull { !it.first.isBefore(first) }?.second
    val enough = calc.logged.size >= 2

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_ins_title))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "range", contentType = "range") {
                Segmented(listOf(stringResource(R.string.g_ins_week), stringResource(R.string.g_ins_month), stringResource(R.string.g_ins_3months)), range) { range = it }
            }
            item(key = "cards", contentType = "cards") {
                val diff = calc.avg.kcal.roundToInt() - t.kcal
                val avgText = if (calc.counted.isEmpty()) "–" else "%,d".format(calc.avg.kcal.roundToInt())
                val avgSub = if (calc.counted.isEmpty()) stringResource(R.string.g_ins_no_logs) else if (diff <= 0) stringResource(R.string.g_ins_under, -diff) else stringResource(R.string.g_ins_over, diff)
                val wText = wNow?.let { stringResource(R.string.g_kg, "%.1f".format(it)) } ?: "–"
                val wSub = if (wNow != null && wThen != null) stringResource(if (wNow <= wThen) R.string.g_ins_w_down else R.string.g_ins_w_up, "%.1f".format(abs(wNow - wThen))) else stringResource(R.string.g_ins_log_weighins)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppCard(Modifier.weight(1f), padding = PaddingValues(16.dp)) {
                        Text(stringResource(R.string.g_ins_avg_day), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2)
                        Text(avgText, style = U.t.headline)
                        Text(avgSub, style = U.t.caption, fontWeight = FontWeight.Bold, color = if (diff <= 0 || calc.counted.isEmpty()) c.leafInk else c.chiInk)
                    }
                    AppCard(Modifier.weight(1f), padding = PaddingValues(16.dp), onClick = { nav.push(Page.Weight) }) {
                        Text(stringResource(R.string.g_ins_weight), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2)
                        Text(wText, style = U.t.headline)
                        Text(wSub, style = U.t.caption, fontWeight = FontWeight.Bold, color = c.leafInk)
                    }
                }
            }
            if (!enough) {
                item(key = "empty", contentType = "empty") {
                    AppCard {
                        Text(stringResource(R.string.g_ins_empty_title), style = U.t.title, modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.g_ins_empty_body), style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        SmallSecondary(stringResource(R.string.g_ins_empty_action)) { nav.quickAdd.value = QuickAddRequest() }
                    }
                }
            } else {
                item(key = "logged", contentType = "caption") {
                    Text(stringResource(R.string.g_ins_logged_line, calc.logged.size, calc.logged.size - calc.counted.size), style = U.t.caption, color = c.ink2)
                }
                item(key = "kcal-chart", contentType = "chart") {
                    val desc = remember(calc, range, t.kcal) {
                        calc.run { if (range == 0) ds.indices.joinToString("; ") { i -> "${dates[i].dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${kcals[i].roundToInt()}" } else weekAvgs.indices.joinToString("; ") { i -> "week ${i + 1} ${weekAvgs[i].roundToInt()}" } }
                    }
                    AppCard {
                        SectionLabel(stringResource(R.string.g_ins_kcal_per_day))
                        val chartDesc = stringResource(R.string.g_ins_kcal_chart_desc, t.kcal, desc)
                        if (range == 0) {
                            BarChart(
                                calc.kcals, calc.dates.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) }, t.kcal.toDouble(),
                                calc.kcals.mapIndexed { i, k -> if (data.isCheatDay(calc.ds[i])) c.m3 else if (k > t.kcal * 1.05) c.chi else c.leaf },
                                modifier = Modifier.clearAndSetSemantics { contentDescription = chartDesc },
                                marks = calc.ds.map { if (data.isCheatDay(it)) "🍕" else null },
                            )
                        } else {
                            val weeks = calc.dates.chunked(7)
                            BarChart(
                                calc.weekAvgs, weeks.map { "${it.first().dayOfMonth}/${it.first().monthValue}" }, t.kcal.toDouble(),
                                calc.weekAvgs.map { if (it > t.kcal * 1.05) c.chi else c.leaf },
                                modifier = Modifier.clearAndSetSemantics { contentDescription = chartDesc },
                            )
                            Text(stringResource(R.string.g_ins_weekly_avgs), style = U.t.caption, color = c.ink2)
                        }
                    }
                }
                item(key = "nutrients", contentType = "card") {
                    AppCard {
                        SectionLabel(stringResource(R.string.g_ins_avg_vs_target))
                        NUTRIENTS.forEach { n ->
                            val have = n.value(calc.avg); val target = n.target(t)
                            val st = statusOf(n, have, target)
                            val line = stringResource(R.string.g_ins_nutrient_desc, n.name, fmtNum(have), fmtNum(target))
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp).semantics(mergeDescendants = true) { contentDescription = line }, verticalAlignment = Alignment.CenterVertically) {
                                Text(n.name, style = U.t.label, modifier = Modifier.widthIn(min = 88.dp, max = 120.dp))
                                Box(Modifier.weight(1f)) { Track((have / target).toFloat(), when (st) { app.unavu.ui.Status.OK -> c.leaf; app.unavu.ui.Status.LOW -> c.tur; else -> c.chi }, height = 8.dp) }
                                Text("  ${fmtNum(have)}/${fmtNum(target)}", style = U.t.caption, color = c.ink2)
                            }
                        }
                    }
                }
                if (range == 0) item(key = "gaps", contentType = "card") { GapGrid(calc.ds) }
                item(key = "timing", contentType = "card") { MealTiming(calc.ds) }
                item(key = "top", contentType = "card") {
                    AppCard {
                        SectionLabel(stringResource(R.string.g_ins_top_foods))
                        calc.topFoods.forEach { (n, k) -> SourceRow(n, stringResource(R.string.g_kcal, k.roundToInt())) }
                    }
                }
            }
            item(key = "report", contentType = "card") {
                val last = YearMonth.now().minusMonths(1)
                val thisM = YearMonth.now()
                val openLabel = stringResource(R.string.g_ins_report_open)
                val failFallback = stringResource(R.string.g_ins_report_failed_generic)
                AppCard {
                    SectionLabel(stringResource(R.string.g_ins_report_title))
                    Text(stringResource(R.string.g_ins_report_body), style = U.t.bodyS, color = c.ink2)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(last, thisM).forEach { ym ->
                            val label = "${ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${ym.year}"
                            val savedMsg = stringResource(R.string.g_ins_report_saved, label)
                            val failedMsg = stringResource(R.string.g_ins_report_failed, label)
                            Box(Modifier.weight(1f)) {
                                SmallSecondary("📄 $label") {
                                    Report.save(context, data, ym).fold({ uri ->
                                        feedback?.show(savedMsg)
                                        runCatching {
                                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), openLabel))
                                        }
                                    }, { feedback?.show(failedMsg + " " + (it.message ?: failFallback)) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
