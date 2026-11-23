package app.unavu.ui.screens

import android.content.Intent
import app.unavu.ui.Status
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import app.unavu.data.fmtInt
import app.unavu.data.targetKcalOn
import app.unavu.ui.screens.today.spokenUnit
import app.unavu.ui.theme.AppColors
import app.unavu.ui.theme.Space
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
import app.unavu.ui.SlidingSegmented
import app.unavu.ui.EmptyState
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

/**
 * [counted] = days that go into averages: logged, not a cheat day, and finished (today is left out while it is
 * still in progress, see [dayInProgress]). [states] judge each day against the target that applied on it.
 */
private class InsightsCalc(
    val dates: List<LocalDate>, val ds: List<String>, val kcals: List<Double>, val logged: List<String>, val counted: List<String>,
    val cheatCount: Int, val todayExcluded: Boolean,
    val avg: Nutr, val avgTarget: Int, val states: List<DayState>,
    val weekAvgs: List<Double>, val weekStates: List<DayState>, val topFoods: List<Pair<String, Double>>,
)

/** A judged day or week as a bar: History's verdict colours; neutral ink while today is in progress. */
private fun barColor(s: DayState, c: AppColors): Color = when (s) {
    DayState.NONE -> c.leaf
    DayState.PROGRESS -> c.ink2
    else -> s.color(c)
}

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

    val calc = remember(data, days, today) {
        val dates = (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val ds = dates.map { it.format(DATE) }
        val judged = ds.map { data.judgeDay(it) }
        val kcals = judged.map { it.kcal }
        val logged = ds.filter { d -> data.entriesOn(d).isNotEmpty() }
        val inProgress = logged.filter { data.dayInProgress(it) }
        val cheatCount = logged.count { data.isCheatDay(it) }
        val counted = logged.filterNot { data.isCheatDay(it) || it in inProgress }
        val avg = if (counted.isEmpty()) Nutr() else counted.map { data.entriesOn(it).filterNot { e -> e.cheat }.total() }.fold(Nutr()) { a, b -> a + b } * (1.0 / counted.size)
        // Average of the targets that applied on the counted days (a mid-range target change stays fair).
        val avgTarget = counted.mapNotNull { data.targetKcalOn(it) }.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: t.kcal
        val weeks = ds.chunked(7).map { w -> w.filter { it in counted } }
        val weekAvgs = weeks.map { l -> if (l.isEmpty()) 0.0 else l.map { d -> data.entriesOn(d).total().kcal }.average() }
        val weekStates = weeks.mapIndexed { i, l ->
            val wt = l.mapNotNull { data.targetKcalOn(it) }.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: t.kcal
            dayState(weekAvgs[i], wt, inProgress = false)
        }
        val top = ds.flatMap { data.entriesOn(it) }.groupBy(LogEntry::foodId)
            .map { (id, es) -> (FoodDb.get(id)?.let { "${it.emoji} ${it.name}" } ?: id) to es.sumOf { e -> e.nutr().kcal } }
            .sortedByDescending { it.second }.take(6)
        InsightsCalc(
            dates, ds, kcals, logged, counted, cheatCount, inProgress.isNotEmpty(),
            avg, avgTarget, judged.map { it.state }, weekAvgs, weekStates, top,
        )
    }
    val trend = remember(data.weights) { Smart.trend(data.weights) }
    val wNow = trend.lastOrNull()?.second
    val first = calc.dates.first()
    val wThen = trend.lastOrNull { !it.first.isAfter(first) }?.second ?: trend.firstOrNull { !it.first.isBefore(first) }?.second
    val enough = calc.logged.size >= 2

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_ins_title))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
            staggerItem("range", "range", 0) {
                SlidingSegmented(listOf(stringResource(R.string.g_ins_week), stringResource(R.string.g_ins_month), stringResource(R.string.g_ins_3months)), range) { range = it }
            }
            if (enough) staggerItem("cards", "cards", 1) {
                val none = calc.counted.isEmpty()
                val diff = calc.avg.kcal.roundToInt() - calc.avgTarget
                val avgText = if (none) "–" else fmtInt(calc.avg.kcal)
                val avgSub = when {
                    none -> stringResource(R.string.r7c_ins_no_finished)
                    diff <= 0 -> stringResource(R.string.r7c_ins_under, fmtInt(-diff))
                    else -> stringResource(R.string.r7c_ins_over, fmtInt(diff))
                }
                val wText = wNow?.let { stringResource(R.string.g_kg, String.format(Locale.ROOT, "%.1f", it)) } ?: "–"
                val wSub = if (wNow != null && wThen != null) stringResource(if (wNow <= wThen) R.string.g_ins_w_down else R.string.g_ins_w_up, String.format(Locale.ROOT, "%.1f", abs(wNow - wThen))) else stringResource(R.string.g_ins_log_weighins)
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.cardGap)) {
                    AppCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(16.dp)) {
                        Text(stringResource(R.string.g_ins_avg_day), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2)
                        Text(avgText, style = U.t.headline)
                        Text(avgSub, style = U.t.caption, fontWeight = FontWeight.Bold, color = if (none) c.ink2 else if (diff <= 0) c.leafInk else c.chiInk)
                    }
                    AppCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(16.dp), onClick = { nav.push(Page.Weight) }) {
                        Text(stringResource(R.string.g_ins_weight), style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2)
                        Text(wText, style = U.t.headline)
                        Text(wSub, style = U.t.caption, fontWeight = FontWeight.Bold, color = c.leafInk)
                    }
                }
            }
            if (!enough) {
                staggerItem("empty", "empty", 2) {
                    AppCard {
                        EmptyState("📊", stringResource(R.string.g_ins_empty_title), stringResource(R.string.g_ins_empty_body), stringResource(R.string.g_ins_empty_action)) { nav.quickAdd.value = QuickAddRequest() }
                    }
                }
            } else {
                staggerItem("logged", "caption", 3) {
                    Column {
                        Text(stringResource(R.string.g_ins_logged_line, calc.logged.size, calc.cheatCount), style = U.t.caption, color = c.ink2)
                        if (calc.todayExcluded) Text(stringResource(R.string.r7c_ins_today_excluded), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                staggerItem("kcal-chart", "chart", 4) {
                    val dayFmt = stringResource(R.string.r7c_ins_day_kcal)
                    val weekFmt = stringResource(R.string.r7c_ins_week_kcal)
                    val desc = remember(calc, range, dayFmt, weekFmt) {
                        calc.run {
                            if (range == 0) ds.indices.joinToString("; ") { i -> dayFmt.format(dates[i].dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH), fmtInt(kcals[i])) }
                            else weekAvgs.indices.joinToString("; ") { i -> weekFmt.format(i + 1, fmtInt(weekAvgs[i])) }
                        }
                    }
                    AppCard {
                        CardTitle(stringResource(R.string.g_ins_kcal_per_day))
                        val chartDesc = stringResource(R.string.r7c_ins_kcal_chart_desc, fmtInt(t.kcal), desc)
                        if (range == 0) {
                            // Bars use the History verdict colours; today stays neutral while it is in progress.
                            BarChart(
                                calc.kcals, calc.dates.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) }, t.kcal.toDouble(),
                                calc.states.mapIndexed { i, s -> if (data.isCheatDay(calc.ds[i])) c.m3 else barColor(s, c) },
                                modifier = Modifier.clearAndSetSemantics { contentDescription = chartDesc },
                                marks = calc.ds.map { if (data.isCheatDay(it)) "🍕" else null },
                            )
                        } else {
                            val weeks = calc.dates.chunked(7)
                            BarChart(
                                calc.weekAvgs, weeks.map { "${it.first().dayOfMonth}/${it.first().monthValue}" }, t.kcal.toDouble(),
                                calc.weekStates.map { barColor(it, c) },
                                modifier = Modifier.clearAndSetSemantics { contentDescription = chartDesc },
                            )
                            Text(stringResource(R.string.g_ins_weekly_avgs), style = U.t.caption, color = c.ink2)
                        }
                    }
                }
                staggerItem("nutrients", "card", 5) {
                    AppCard {
                        CardTitle(stringResource(R.string.g_ins_avg_vs_target))
                        // Fixed label and value columns, so every bar starts and ends at the same x. In Sketch, at large
                        // font scales and on narrow screens the label and value sit above a full-width bar instead.
                        val stacked = stackedRows()
                        val valueStyle = U.t.caption
                        val fit = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = valueStyle.fontSize, stepSize = 0.5.sp)
                        val valueInk = c.ink2
                        NUTRIENTS.forEach { n ->
                            val have = n.value(calc.avg); val target = n.target(t)
                            val st = statusOf(n, have, target)
                            val line = stringResource(R.string.r7c_ins_nutrient_desc, n.name, fmtNum(have), fmtNum(target), spokenUnit(n.unit))
                            val value = "${fmtNum(have)}/${fmtNum(target)}"
                            val barCol = when (st) { Status.OK -> c.leaf; Status.LOW -> c.tur; else -> c.chi }
                            val frac = if (target > 0) (have / target).toFloat() else 0f
                            if (stacked) {
                                Column(Modifier.fillMaxWidth().padding(top = 8.dp).clearAndSetSemantics { contentDescription = line }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(n.name, style = U.t.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(value, style = valueStyle, color = valueInk, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Track(frac, barCol, Modifier.padding(top = 4.dp), height = 8.dp)
                                }
                            } else {
                                Row(Modifier.fillMaxWidth().padding(top = 8.dp).clearAndSetSemantics { contentDescription = line }, verticalAlignment = Alignment.CenterVertically) {
                                    Text(n.name, style = U.t.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(INSIGHT_LABEL_W))
                                    Box(Modifier.weight(1f)) { Track(frac, barCol, height = 8.dp) }
                                    BasicText(
                                        value, style = valueStyle.copy(textAlign = TextAlign.End), color = ColorProducer { valueInk }, maxLines = 1, autoSize = fit,
                                        modifier = Modifier.width(INSIGHT_VALUE_W).padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (range == 0) staggerItem("gaps", "card", 6) { GapGrid(calc.ds) }
                staggerItem("timing", "card", 7) { MealTiming(calc.ds) }
                staggerItem("top", "card", 8) {
                    AppCard {
                        CardTitle(stringResource(R.string.g_ins_top_foods))
                        calc.topFoods.forEach { (n, k) -> SourceRow(n, stringResource(R.string.r7c_kcal_value, fmtInt(k))) }
                    }
                }
            }
            // report buttons only for months that have entries; no card at all without data
            val reportMonths = listOf(YearMonth.now().minusMonths(1), YearMonth.now()).filter { ym ->
                val prefix = ym.toString()
                data.entries.any { it.date.startsWith(prefix) }
            }
            if (reportMonths.isNotEmpty()) staggerItem("report", "card", 9) {
                val openLabel = stringResource(R.string.g_ins_report_open)
                val failFallback = stringResource(R.string.g_ins_report_failed_generic)
                AppCard {
                    CardTitle(stringResource(R.string.g_ins_report_title))
                    Text(stringResource(R.string.g_ins_report_body), style = U.t.bodyS, color = c.ink2)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        reportMonths.forEach { ym ->
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
