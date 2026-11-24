package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import app.unavu.ui.pressScale
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.AppData
import app.unavu.data.DATE
import app.unavu.data.Meal
import app.unavu.data.copyDay
import app.unavu.data.entriesOn
import app.unavu.data.fmtInt
import app.unavu.data.food
import app.unavu.data.isCheatDay
import app.unavu.data.setCheatDay
import app.unavu.data.targetKcalOn
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Tab
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.screens.today.PagerArrow
import app.unavu.ui.theme.AppColors
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// ---------------- day judgement (shared by History, the Today week strip and Insights) ----------------

/** How a day went against the target that applied on it. PROGRESS: today, not judged yet. */
enum class DayState { NONE, PROGRESS, ON, UNDER, OVER }

/** The one rule for judging a day: within −15 % / +5 % of that day's target is on target. */
fun dayState(kcal: Double, target: Int?, inProgress: Boolean): DayState = when {
    kcal <= 0.0 || target == null || target <= 0 -> DayState.NONE
    inProgress -> DayState.PROGRESS
    kcal > target * 1.05 -> DayState.OVER
    kcal < target * 0.85 -> DayState.UNDER
    else -> DayState.ON
}

/** Today stays "in progress" (never called under or over) until 22:00, or until dinner is logged. */
fun AppData.dayInProgress(date: String, now: LocalDateTime = LocalDateTime.now()): Boolean =
    date == now.toLocalDate().format(DATE) && now.hour < 22 && entriesOn(date).none { it.meal == Meal.DINNER }

/** kcal eaten on [date], the target that applied that day, and the verdict. */
class DayJudge(val kcal: Double, val target: Int?, val state: DayState)

fun AppData.judgeDay(date: String): DayJudge {
    val kcal = entriesOn(date).total().kcal
    val target = targetKcalOn(date)
    return DayJudge(kcal, target, dayState(kcal, target, dayInProgress(date)))
}

fun DayState.color(c: AppColors): Color = when (this) {
    DayState.ON -> c.leaf
    DayState.UNDER -> c.tur
    DayState.OVER -> c.chi
    DayState.PROGRESS -> c.ink2
    DayState.NONE -> Color.Transparent
}

fun DayState.status(): Status = when (this) {
    DayState.ON -> Status.OK
    DayState.UNDER -> Status.LOW
    DayState.OVER -> Status.OVER
    DayState.PROGRESS, DayState.NONE -> Status.INFO
}

/** Spoken verdict for content descriptions ("on target", "in progress"); null when there is nothing to judge. */
@Composable
fun DayState.spoken(): String? = when (this) {
    DayState.ON -> stringResource(R.string.r7c_day_on)
    DayState.UNDER -> stringResource(R.string.r7c_day_under)
    DayState.OVER -> stringResource(R.string.r7c_day_over)
    DayState.PROGRESS -> stringResource(R.string.r7c_day_progress)
    DayState.NONE -> null
}

/** The small status dot under a day number: filled for a verdict, a ring while today is in progress. */
@Composable
fun DayDot(state: DayState, selected: Boolean, selectedInk: Color, modifier: Modifier = Modifier, size: Dp = 6.dp) {
    val base = state.color(U.c)
    val col = if (selected && state != DayState.NONE) selectedInk else base
    val shape = CircleShape
    Box(
        modifier.size(size).clip(shape)
            .then(if (state == DayState.PROGRESS) Modifier.border(1.5.dp, col, shape) else Modifier.background(col)),
    )
}

// ---------------- History ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    var month by rememberSaveable { mutableStateOf(YearMonth.from(LocalDate.parse(nav.date.value)).toString()) }
    val ym = YearMonth.parse(month)
    var selected by rememberSaveable { mutableStateOf(nav.date.value) }
    var noteDialog by rememberSaveable { mutableStateOf(false) }
    var copyConfirm by rememberSaveable { mutableStateOf(false) }
    val undo = rememberUndoer(stringResource(R.string.g_undo)) { store.restoreSnapshot(it) }
    val todayStr = today()
    val thisMonth = YearMonth.now()

    // Each visible day judged once against the target that applied on it; recomputed only when the data or month change.
    val monthJudge = remember(data, ym) { (1..ym.lengthOfMonth()).associate { ym.atDay(it).format(DATE).let { ds -> ds to data.judgeDay(ds) } } }
    val weekdays = remember { DayOfWeek.values().map { it.getDisplayName(TextStyle.NARROW, Locale.ENGLISH) } }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_hist_title), onBack = { nav.pop() })
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Space.cardGap),
        ) {
            item(key = "month", contentType = "month") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PagerArrow(Arrow.LEFT, stringResource(R.string.g_hist_prev_month)) { month = ym.minusMonths(1).toString() }
                    Text(
                        "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}", style = U.t.titleS, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    PagerArrow(Arrow.RIGHT, stringResource(R.string.g_hist_next_month), enabled = ym < thisMonth) { month = ym.plusMonths(1).toString() }
                }
            }
            item(key = "calendar", contentType = "calendar") {
                val onLbl = stringResource(R.string.g_hist_on); val underLbl = stringResource(R.string.g_hist_under); val overLbl = stringResource(R.string.g_hist_over)
                val cheatLbl = stringResource(R.string.g_hist_cheat)
                val progressLbl = stringResource(R.string.r7c_hist_legend_progress)
                AppCard(padding = PaddingValues(10.dp)) {
                    Row(Modifier.clearAndSetSemantics { }) {
                        weekdays.forEach { Text(it, style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    val offset = ym.atDay(1).dayOfWeek.value - 1
                    val cells = (0 until offset).map { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
                    cells.chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(bottom = 3.dp)) {
                            week.forEach { d ->
                                if (d == null) Spacer(Modifier.weight(1f)) else {
                                    val ds = d.format(DATE)
                                    val on = ds == selected
                                    val future = d.isAfter(LocalDate.now())
                                    val judge = monthJudge[ds]
                                    val kcal = judge?.kcal ?: 0.0
                                    val state = judge?.state ?: DayState.NONE
                                    val cheat = data.isCheatDay(ds)
                                    val kcalSpoken = if (kcal > 0) stringResource(R.string.r7c_kcal_spoken, fmtInt(kcal)) else null
                                    val verdict = state.spoken()
                                    val desc = listOfNotNull(
                                        "${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}",
                                        kcalSpoken, if (cheat) cheatLbl else verdict,
                                    ).joinToString(", ")
                                    val bgAnim by animateColorAsState(if (on) c.ink else c.ink.copy(alpha = 0f), motionTween(Motion.SHORT), label = "dayBg")
                                    val numAnim by animateColorAsState(if (on) c.bg else if (future) c.ink2.copy(alpha = 0.5f) else c.ink, motionTween(Motion.SHORT), label = "dayNum")
                                    Column(
                                        Modifier.weight(1f).pressScale().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).background(bgAnim)
                                            .semantics(mergeDescendants = true) { contentDescription = desc; this.selected = on }
                                            .clickable(enabled = !future, role = Role.Button) { selected = ds },
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text("${d.dayOfMonth}", style = U.t.label, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = numAnim)
                                        if (cheat) Text("🍕", style = U.t.caption)
                                        else DayDot(state, on, c.bg, Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 6.dp).clearAndSetSemantics { },
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        LegendEntry({ DayDot(DayState.ON, false, c.bg, size = 7.dp) }, onLbl)
                        LegendEntry({ DayDot(DayState.UNDER, false, c.bg, size = 7.dp) }, underLbl)
                        LegendEntry({ DayDot(DayState.OVER, false, c.bg, size = 7.dp) }, overLbl)
                        if (ym == thisMonth) LegendEntry({ DayDot(DayState.PROGRESS, false, c.bg, size = 7.dp) }, progressLbl)
                        LegendEntry({ Text("🍕", style = U.t.caption) }, cheatLbl)
                    }
                }
            }
            item(key = "day", contentType = "day") {
                val es = data.entriesOn(selected)
                val tot = es.total()
                val cheat = data.isCheatDay(selected)
                val judge = monthJudge[selected] ?: data.judgeDay(selected)
                val title = dayLabel(selected)
                AppCard(border = if (cheat) c.m3 else c.line) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = U.t.body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
                        val target = judge.target
                        if (cheat) StatusBadge("🍕 " + stringResource(R.string.g_hist_cheat), Status.LOW)
                        else if (target != null && es.isNotEmpty()) StatusBadge(
                            stringResource(
                                if (judge.state == DayState.PROGRESS) R.string.r7c_hist_badge_so_far else R.string.r7c_hist_badge,
                                fmtInt(tot.kcal), fmtInt(target),
                            ),
                            judge.state.status(),
                        )
                    }
                    if (cheat) Text(stringResource(R.string.r7c_hist_cheat_line, data.dayNotes[selected] ?: stringResource(R.string.g_hist_cheat), fmtInt(tot.kcal)), style = U.t.caption, color = c.ink2)
                    else if (judge.state == DayState.PROGRESS) Text(stringResource(R.string.r7c_hist_progress_note), style = U.t.caption, color = c.ink2)
                    Spacer(Modifier.height(6.dp))
                    if (es.isEmpty()) {
                        Text(stringResource(R.string.g_hist_empty), style = U.t.bodyS, color = c.ink2)
                        Spacer(Modifier.height(8.dp))
                        if (selected <= todayStr) SmallSecondary(stringResource(R.string.g_hist_log_this_day)) { nav.date.value = selected; nav.quickAdd.value = QuickAddRequest() }
                    }
                    Meal.entries.forEach { m ->
                        val list = es.filter { it.meal == m }
                        if (list.isNotEmpty()) {
                            val kcalText = stringResource(R.string.r7c_kcal_value, fmtInt(list.total().kcal))
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append(m.label) }
                                    append(" · " + list.joinToString(", ") { it.food()?.name ?: "?" } + " · " + kcalText)
                                    if (list.any { it.cheat }) append(" 🍕")
                                },
                                style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SmallSecondary(stringResource(R.string.g_hist_open_day)) { nav.date.value = selected; nav.stack.clear(); nav.tab.value = Tab.TODAY } }
                        if (selected != todayStr && es.isNotEmpty()) Box(Modifier.weight(1f)) { SmallSecondary(stringResource(R.string.g_hist_copy)) { copyConfirm = true } }
                    }
                    Spacer(Modifier.height(8.dp))
                    val markedMsg = stringResource(R.string.g_hist_marked, title)
                    val unmarkedMsg = stringResource(R.string.g_hist_unmarked, title)
                    SmallSecondary(if (cheat) stringResource(R.string.g_hist_unmark) else stringResource(R.string.g_hist_mark)) {
                        val before = store.data.value
                        if (cheat) { store.setCheatDay(selected, false); undo.announce(unmarkedMsg, before) } else { store.setCheatDay(selected, true); undo.announce(markedMsg, before) }
                    }
                    if (cheat) { Spacer(Modifier.height(8.dp)); SmallSecondary(stringResource(R.string.g_hist_note)) { noteDialog = true } }
                }
            }
        }
    }
    if (noteDialog) {
        InputDialog(stringResource(R.string.g_hist_note_title), stringResource(R.string.g_hist_note_hint), data.dayNotes[selected] ?: "", onDismiss = { noteDialog = false }) { store.setCheatDay(selected, true, it) }
    }
    if (copyConfirm) {
        val title = dayLabel(selected)
        val copied = stringResource(R.string.g_hist_copied, title)
        ConfirmDialog(stringResource(R.string.g_hist_copy_title), stringResource(R.string.g_hist_copy_body, title), stringResource(R.string.g_hist_copy_ok), onDismiss = { copyConfirm = false }) {
            val before = store.data.value
            store.copyDay(selected, todayStr); undo.announce(copied, before)
            nav.date.value = todayStr; nav.stack.clear(); nav.tab.value = Tab.TODAY
        }
    }
}

@Composable
private fun LegendEntry(mark: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        mark()
        Spacer(Modifier.width(5.dp))
        Text(label, style = U.t.caption, color = U.c.ink2)
    }
}
