package app.unavu.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.DATE
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.Nutr
import app.unavu.data.Report
import app.unavu.data.Smart
import app.unavu.data.entriesOn
import app.unavu.data.isCheatDay
import app.unavu.data.targets
import app.unavu.data.total
import app.unavu.data.nutr
import app.unavu.ui.AppCard
import app.unavu.ui.BarChart
import app.unavu.ui.LocalData
import app.unavu.ui.Page
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun InsightsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val context = LocalContext.current
    var range by rememberSaveable { mutableStateOf(0) }
    var reportMsg by remember { mutableStateOf<String?>(null) }
    val t = data.targets() ?: return
    val days = listOf(7, 30, 90)[range]
    val dates = (days - 1 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val ds = dates.map { it.format(DATE) }
    val kcals = ds.map { data.entriesOn(it).total().kcal }
    val logged = ds.filter { d -> data.entriesOn(d).isNotEmpty() }
    val counted = logged.filterNot { data.isCheatDay(it) }
    val avg = if (counted.isEmpty()) Nutr() else counted.map { data.entriesOn(it).filterNot { e -> e.cheat }.total() }.fold(Nutr()) { a, b -> a + b } * (1.0 / counted.size)
    val trend = Smart.trend(data.weights)
    val wNow = trend.lastOrNull()?.second
    val wThen = trend.lastOrNull { !it.first.isAfter(dates.first()) }?.second ?: trend.firstOrNull { !it.first.isBefore(dates.first()) }?.second

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Insights")
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Segmented(listOf("Week", "Month", "3 months"), range) { range = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppCard(Modifier.weight(1f), padding = PaddingValues(16.dp)) {
                    Text("AVG / DAY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.ink2)
                    Text(if (counted.isEmpty()) "–" else "%,d".format(avg.kcal.roundToInt()), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    val diff = avg.kcal.roundToInt() - t.kcal
                    Text(if (counted.isEmpty()) "no logs yet" else if (diff <= 0) "${-diff} under target" else "$diff over target", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (diff <= 0) c.leafInk else c.chi)
                }
                AppCard(Modifier.weight(1f), padding = PaddingValues(16.dp), onClick = { nav.push(Page.Weight) }) {
                    Text("WEIGHT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = c.ink2)
                    Text(wNow?.let { "%.1f kg".format(it) } ?: "–", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(if (wNow != null && wThen != null) "%s %.1f kg".format(if (wNow <= wThen) "↓" else "↑", kotlin.math.abs(wNow - wThen)) else "log weigh-ins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.leafInk)
                }
            }
            Text("${logged.size} days logged · ${logged.size - counted.size} cheat days left out of averages", fontSize = 11.sp, color = c.ink2)

            AppCard {
                SectionLabel("Calories per day")
                if (range == 0) {
                    BarChart(
                        kcals, dates.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) }, t.kcal.toDouble(),
                        kcals.mapIndexed { i, k -> if (data.isCheatDay(ds[i])) c.m3 else if (k > t.kcal * 1.05) c.chi else c.leaf },
                        marks = ds.map { if (data.isCheatDay(it)) "🍕" else null },
                    )
                } else {
                    // weekly averages for longer ranges
                    val weeks = dates.chunked(7)
                    val avgs = weeks.map { w -> w.map { it.format(DATE) }.filter { d -> data.entriesOn(d).isNotEmpty() && !data.isCheatDay(d) }.let { l -> if (l.isEmpty()) 0.0 else l.map { d -> data.entriesOn(d).total().kcal }.average() } }
                    BarChart(avgs, weeks.map { "${it.first().dayOfMonth}/${it.first().monthValue}" }, t.kcal.toDouble(), avgs.map { if (it > t.kcal * 1.05) c.chi else c.leaf })
                    Text("Weekly averages (cheat days excluded)", fontSize = 10.5.sp, color = c.ink2)
                }
            }

            AppCard {
                SectionLabel("Average per day vs target")
                NUTRIENTS.forEach { n ->
                    val have = n.value(avg); val target = n.target(t)
                    val st = statusOf(n, have, target)
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(n.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(100.dp))
                        Box(Modifier.weight(1f)) { app.unavu.ui.Track((have / target).toFloat(), when (st) { Status.OK -> c.leaf; Status.LOW -> c.tur; else -> c.chi }, height = 8.dp) }
                        Text("  ${fmtNum(have)}/${fmtNum(target)}", fontSize = 11.sp, color = c.ink2)
                    }
                }
            }

            if (range == 0) GapGrid(ds)

            MealTiming(ds)

            AppCard {
                SectionLabel("Top foods by calories")
                ds.flatMap { data.entriesOn(it) }.groupBy { it.foodId }
                    .map { (id, es) -> (app.unavu.data.FoodDb.get(id)?.let { "${it.emoji} ${it.name}" } ?: id) to es.sumOf { e -> e.nutr().kcal } }
                    .sortedByDescending { it.second }.take(6).forEach { (n, k) -> SourceRow(n, "${k.roundToInt()} kcal") }
            }

            val last = YearMonth.now().minusMonths(1)
            val thisM = YearMonth.now()
            AppCard {
                SectionLabel("Monthly report (PDF)")
                Text("Averages, weight change, nutrient bars and top foods for a month. Saved to your backup folder.", fontSize = 12.sp, color = c.ink2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(last, thisM).forEach { ym ->
                        Box(Modifier.weight(1f)) {
                            SmallSecondary("📄 ${ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${ym.year}") {
                                Report.save(context, data, ym).fold({ uri ->
                                    reportMsg = "Saved ${ym} report"
                                    runCatching {
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Open report"))
                                    }
                                }, { reportMsg = it.message })
                            }
                        }
                    }
                }
                reportMsg?.let { Text(it, fontSize = 11.5.sp, color = c.leafInk, modifier = Modifier.padding(top = 6.dp)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 7-day grid: one square per day for the main nutrients. */
@Composable
private fun GapGrid(ds: List<String>) {
    val c = U.c
    val data = LocalData.current
    val t = data.targets() ?: return
    val keys = listOf("protein", "fibre", "iron", "calcium", "sodium", "sugar")
    AppCard {
        SectionLabel("Nutrient gaps · 7 days")
        val lowCounts = mutableMapOf<String, Int>()
        keys.forEach { k ->
            val n = NUTRIENTS.first { it.key == k }
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(88.dp))
                ds.forEach { d ->
                    val es = data.entriesOn(d)
                    val v = n.value(es.total()) / n.target(t)
                    val col = when {
                        es.isEmpty() -> c.sf2
                        n.limit -> if (v > 1) c.chi else if (v > 0.85) c.tur else c.leaf
                        else -> if (v >= 0.9) c.leaf else if (v >= 0.6) c.tur else c.turS
                    }
                    if (es.isNotEmpty() && ((n.limit && v > 1) || (!n.limit && v < 0.9))) lowCounts[k] = (lowCounts[k] ?: 0) + 1
                    Box(Modifier.weight(1f).padding(horizontal = 1.5.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(col))
                }
            }
        }
        val worst = lowCounts.maxByOrNull { it.value }
        if (worst != null && worst.value >= 3) {
            val n = NUTRIENTS.first { it.key == worst.key }
            Text("💡 ${n.name} was ${if (n.limit) "over" else "low"} on ${worst.value} days. ${n.tip}", fontSize = 11.5.sp, color = c.ink2, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** When each meal was logged, and the late-dinner effect. */
@Composable
private fun MealTiming(ds: List<String>) {
    val c = U.c
    val data = LocalData.current
    val colors = mealColors(c)
    val rows = ds.takeLast(7).map { d -> d to Meal.entries.map { m -> data.entriesOn(d).filter { it.meal == m }.minOfOrNull { it.time } } }
    if (rows.none { r -> r.second.any { it != null } }) return
    fun hour(t: String) = LocalTime.parse(t).let { it.hour + it.minute / 60f }
    AppCard {
        SectionLabel("Meal timing · last 7 days")
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val x0 = 0f; val w = size.width
            fun x(h: Float) = x0 + (h - 6f) / 18f * w
            listOf(6, 9, 12, 15, 18, 21, 24).forEach { h -> drawLine(c.line, Offset(x(h.toFloat()), 0f), Offset(x(h.toFloat()), size.height), 1f) }
            drawRect(c.chi.copy(alpha = 0.10f), Offset(x(22f), 0f), androidx.compose.ui.geometry.Size(x(24f) - x(22f), size.height))
            rows.forEachIndexed { i, (_, times) ->
                val y = 10f + i * (size.height - 20f) / 6f
                times.forEachIndexed { mi, t -> if (t != null) drawCircle(colors[mi], 5.dp.toPx(), Offset(x(hour(t).coerceIn(6f, 24f)), y)) }
            }
        }
        Row(Modifier.fillMaxWidth()) { listOf("6a", "9a", "12p", "3p", "6p", "9p", "12a").forEach { Text(it, fontSize = 10.sp, color = c.ink2, modifier = Modifier.weight(1f)) } }
        // late dinners (logged at/after 22:00) vs others, cheat days excluded
        val dinnerTimes = ds.filterNot { data.isCheatDay(it) }.mapNotNull { d -> data.entriesOn(d).filter { it.meal == Meal.DINNER }.minOfOrNull { it.time }?.let { d to hour(it) } }
        val late = dinnerTimes.filter { it.second >= 22f }.map { it.first }
        val normal = dinnerTimes.filter { it.second < 22f }.map { it.first }
        if (late.isNotEmpty()) {
            val lateAvg = late.map { data.entriesOn(it).total().kcal }.average()
            val normAvg = normal.map { data.entriesOn(it).total().kcal }.average().takeIf { !it.isNaN() }
            Text(
                "🌙 ${late.size} dinner${if (late.size > 1) "s" else ""} logged after 10 PM" + (normAvg?.let { ". Those days averaged %+d kcal vs other days.".format((lateAvg - it).roundToInt()) } ?: "."),
                fontSize = 11.5.sp, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
            )
        }
        val firsts = ds.mapNotNull { d -> data.entriesOn(d).minOfOrNull { it.time }?.let(::hour) }
        val lasts = ds.mapNotNull { d -> data.entriesOn(d).maxOfOrNull { it.time }?.let(::hour) }
        if (firsts.isNotEmpty()) {
            fun fmt(h: Double) = app.unavu.ui.fmtTime(h.toInt(), ((h % 1) * 60).roundToInt().coerceAtMost(59))
            Text("Eating window: ${fmt(firsts.average())} – ${fmt(lasts.average())} (logging times)", fontSize = 11.5.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
        }
        Text("Times are when you logged, so log close to eating for this to be accurate.", fontSize = 10.5.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp))
    }
}

@Suppress("unused")
private fun keep(s: StatusBadge?) = s
private object StatusBadge
