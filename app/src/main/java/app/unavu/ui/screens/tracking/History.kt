package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import app.unavu.ui.pressScale
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.DATE
import app.unavu.data.Meal
import app.unavu.data.copyDay
import app.unavu.data.entriesOn
import app.unavu.data.food
import app.unavu.data.isCheatDay
import app.unavu.data.setCheatDay
import app.unavu.data.targets
import app.unavu.data.today
import app.unavu.data.total
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Tab
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private enum class DayState { NONE, ON, UNDER, OVER }

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
    var noteDialog by rememberSaveable { mutableStateOf(false) }
    var copyConfirm by rememberSaveable { mutableStateOf(false) }
    val undo = rememberUndoer(stringResource(R.string.g_undo)) { store.restoreSnapshot(it) }
    val todayStr = today()

    // kcal per day of the visible month, recomputed only when the entries or month change
    val monthKcal = remember(data.entries, ym) { (1..ym.lengthOfMonth()).associate { ym.atDay(it).format(DATE) to data.entriesOn(ym.atDay(it).format(DATE)).total().kcal } }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_hist_title), onBack = { nav.pop() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "month", contentType = "month") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArrowButton(Arrow.LEFT, contentDescription = stringResource(R.string.g_hist_prev_month)) { month = ym.minusMonths(1).toString() }
                    Text(
                        "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}", style = U.t.titleS, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    ArrowButton(Arrow.RIGHT, contentDescription = stringResource(R.string.g_hist_next_month)) { if (ym < YearMonth.now()) month = ym.plusMonths(1).toString() }
                }
            }
            item(key = "calendar", contentType = "calendar") {
                val onLbl = stringResource(R.string.g_hist_on); val underLbl = stringResource(R.string.g_hist_under); val overLbl = stringResource(R.string.g_hist_over)
                val cheatLbl = stringResource(R.string.g_hist_cheat)
                AppCard(padding = PaddingValues(10.dp)) {
                    Row(Modifier.clearAndSetSemantics { }) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, style = U.t.caption, fontWeight = FontWeight.Bold, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
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
                                    val kcal = monthKcal[ds] ?: 0.0
                                    val state = when {
                                        kcal <= 0 || t == null -> DayState.NONE
                                        kcal > t.kcal * 1.05 -> DayState.OVER
                                        kcal < t.kcal * 0.85 -> DayState.UNDER
                                        else -> DayState.ON
                                    }
                                    val dot = when (state) { DayState.NONE -> Color.Transparent; DayState.OVER -> c.chi; DayState.UNDER -> c.tur; DayState.ON -> c.leaf }
                                    val cheat = data.isCheatDay(ds)
                                    val desc = buildString {
                                        append(d.dayOfMonth).append(' ').append(d.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                                        if (kcal > 0) append(", ").append(kcal.roundToInt()).append(" kcal")
                                        when (state) { DayState.ON -> append(", ").append(onLbl); DayState.UNDER -> append(", ").append(underLbl); DayState.OVER -> append(", ").append(overLbl); else -> {} }
                                        if (cheat) append(", ").append(cheatLbl)
                                    }
                                    val bgAnim by animateColorAsState(if (on) c.ink else c.ink.copy(alpha = 0f), motionTween(Motion.SHORT), label = "dayBg")
                                    val numAnim by animateColorAsState(if (on) c.bg else if (future) c.ink2.copy(alpha = 0.5f) else c.ink, motionTween(Motion.SHORT), label = "dayNum")
                                    Column(
                                        Modifier.weight(1f).pressScale().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).background(bgAnim)
                                            .semantics(mergeDescendants = true) { contentDescription = desc; this.selected = on }
                                            .clickable(enabled = !future, role = Role.Button) { selected = ds },
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text("${d.dayOfMonth}", style = U.t.label, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold, color = numAnim)
                                        if (cheat) Text("🍕", style = U.t.caption) else Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (on && dot != Color.Transparent) c.bg else dot))
                                    }
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp).clearAndSetSemantics { }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        listOf(c.leaf to onLbl, c.tur to underLbl, c.chi to overLbl).forEach { (col, l) ->
                            Box(Modifier.size(7.dp).clip(CircleShape).background(col)); Text(" $l   ", style = U.t.caption, color = c.ink2)
                        }
                        Text("🍕 $cheatLbl", style = U.t.caption, color = c.ink2)
                    }
                }
            }
            item(key = "day", contentType = "day") {
                val es = data.entriesOn(selected)
                val tot = es.total()
                val cheat = data.isCheatDay(selected)
                AppCard(border = if (cheat) c.m3 else c.line) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dayTitle(selected), style = U.t.body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).semantics { heading() })
                        if (cheat) StatusBadge("🍕 " + stringResource(R.string.g_hist_cheat), Status.LOW)
                        else if (t != null && es.isNotEmpty()) StatusBadge("${tot.kcal.roundToInt()} / ${t.kcal}", if (tot.kcal > t.kcal * 1.05) Status.OVER else Status.OK)
                    }
                    if (cheat) Text(stringResource(R.string.g_hist_cheat_line, data.dayNotes[selected] ?: stringResource(R.string.g_hist_cheat), tot.kcal.roundToInt()), style = U.t.caption, color = c.ink2)
                    Spacer(Modifier.height(6.dp))
                    if (es.isEmpty()) {
                        Text(stringResource(R.string.g_hist_empty), style = U.t.bodyS, color = c.ink2)
                        Spacer(Modifier.height(8.dp))
                        if (selected <= todayStr) SmallSecondary(stringResource(R.string.g_hist_log_this_day)) { nav.date.value = selected; nav.quickAdd.value = QuickAddRequest() }
                    }
                    Meal.entries.forEach { m ->
                        val list = es.filter { it.meal == m }
                        if (list.isNotEmpty()) {
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = c.ink)) { append(m.label) }
                                    append(" · " + list.joinToString(", ") { it.food()?.name ?: "?" } + " · ${list.total().kcal.roundToInt()}")
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
                    val markedMsg = stringResource(R.string.g_hist_marked, dayTitle(selected))
                    val unmarkedMsg = stringResource(R.string.g_hist_unmarked, dayTitle(selected))
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
        val copied = stringResource(R.string.g_hist_copied, dayTitle(selected))
        ConfirmDialog(stringResource(R.string.g_hist_copy_title), stringResource(R.string.g_hist_copy_body, dayTitle(selected)), stringResource(R.string.g_hist_copy_ok), onDismiss = { copyConfirm = false }) {
            val before = store.data.value
            store.copyDay(selected, todayStr); undo.announce(copied, before)
            nav.date.value = todayStr; nav.stack.clear(); nav.tab.value = Tab.TODAY
        }
    }
}
