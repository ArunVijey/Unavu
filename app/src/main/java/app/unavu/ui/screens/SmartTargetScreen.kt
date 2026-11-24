package app.unavu.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.unavu.data.fmtInt
import app.unavu.ui.theme.Space
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.Smart
import app.unavu.data.SmartMode
import app.unavu.data.SmartTarget
import app.unavu.data.bmrOf
import app.unavu.data.targets
import app.unavu.ui.AppCard
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Divider
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.TopBar
import app.unavu.ui.Track
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Settings > Smart target: watch, accept, override or reset the self-correcting calorie target. */
@Composable
fun SmartTargetScreen() {
    val c = U.c
    val nav = nav
    val store = nav.store
    val data = LocalData.current
    val feedback = LocalFeedback.current
    val p = data.profile ?: return
    val t = data.targets() ?: return
    val s = data.smart
    val formula = (bmrOf(p) * p.activity).roundToInt()
    val est = remember(data.entries, data.weights) { Smart.estimate(data) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    fun set(f: (SmartTarget) -> SmartTarget) = store.update { d -> d.copy(smart = f(d.smart)) }
    val undoLabel = stringResource(R.string.g_undo)
    fun setUndoable(message: String, f: (SmartTarget) -> SmartTarget) {
        val before = store.data.value
        set(f)
        feedback?.show(message, undoLabel) { store.restoreSnapshot(before) }
    }
    val rangeMsg = stringResource(R.string.r7d_kcal_range)
    val updatedMsg = stringResource(R.string.g_smart_updated)
    val resetMsg = stringResource(R.string.g_smart_reset_done)
    val manualMsg = stringResource(R.string.g_smart_manual_set)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.g_smart_title), onBack = { nav.pop() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
            item(key = "now", contentType = "card") {
                val sub = if (s.manualKcal != null && s.mode != SmartMode.AUTO) stringResource(R.string.g_smart_set_by_you) else stringResource(R.string.r7d_smart_burn, fmtInt(t.maintenance), t.burnSource)
                val kcalShown by countUp(t.kcal.toFloat())
                // heroBg/onHero: solid text at >= 4.5:1 in light, dark and pure black (no translucent captions).
                AppCard(color = c.heroBg, border = null, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text(stringResource(R.string.g_smart_now), color = c.onHero, style = U.t.label, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.r7d_kcal, fmtInt(kcalShown.toInt())), color = c.onHero, style = U.t.display)
                    Text(sub, color = c.onHero, style = U.t.caption)
                }
            }

            s.pendingBurn?.let { burn ->
                item(key = "pending", contentType = "card") {
                    val newKcal = (burn + p.goalKcal).coerceAtLeast(1200)
                    AppCard(color = c.turS, border = null) {
                        Text(stringResource(R.string.r7d_smart_new_est, fmtInt(burn)), style = U.t.titleS, color = c.turInk)
                        Text(stringResource(R.string.r7d_smart_new_target, fmtInt(newKcal), fmtInt(t.kcal)), style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { SecondaryButton(stringResource(R.string.g_smart_not_now)) { set { it.copy(pendingBurn = null) } } }
                            Box(Modifier.weight(1f)) { PrimaryButton(stringResource(R.string.g_smart_use_it)) { setUndoable(updatedMsg) { it.copy(appliedBurn = burn, pendingBurn = null, manualKcal = null) } } }
                        }
                    }
                }
            }

            item(key = "how", contentType = "how") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(stringResource(R.string.g_smart_how))
                    Segmented(listOf(stringResource(R.string.g_smart_auto), stringResource(R.string.g_smart_ask), stringResource(R.string.g_smart_manual)), s.mode.ordinal) { i ->
                        val m = SmartMode.entries[i]
                        if (m == SmartMode.MANUAL) editing = true else set { it.copy(mode = m) }
                    }
                    Text(
                        stringResource(when (s.mode) { SmartMode.AUTO -> R.string.g_smart_auto_desc; SmartMode.ASK -> R.string.g_smart_ask_desc; SmartMode.MANUAL -> R.string.g_smart_manual_desc }),
                        style = U.t.bodyS, color = c.ink2,
                    )
                }
            }

            item(key = "estimate", contentType = "card") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(stringResource(R.string.g_smart_latest))
                    AppCard {
                        if (est == null) {
                            val from = LocalDate.now().minusDays(Smart.WINDOW_DAYS)
                            val days = Smart.loggedDays(data, from, LocalDate.now().minusDays(1)).size
                            val weighIns = data.weights.count { it.date >= from.toString() }
                            Text(stringResource(R.string.g_smart_not_enough), style = U.t.titleS)
                            Text(stringResource(R.string.r7d_smart_not_enough_body, fmtInt(formula)), style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                            Need(stringResource(R.string.g_smart_need_days), days, 14)
                            Spacer(Modifier.height(8.dp))
                            Need(stringResource(R.string.g_smart_need_weighins), weighIns, 6)
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(fmtInt(est.burn), style = U.t.display)
                                Text("  " + stringResource(R.string.g_smart_burn_unit), style = U.t.body, color = c.ink2, modifier = Modifier.padding(bottom = 5.dp))
                            }
                            Text(stringResource(R.string.r7d_smart_formula_conf, fmtInt(formula), est.confidence.toString()), style = U.t.bodyS, color = c.ink2)
                            Spacer(Modifier.height(6.dp))
                            Divider()
                            listOf(
                                stringResource(R.string.g_smart_avg_eaten) to stringResource(R.string.r7d_smart_kcal_day, fmtInt(est.avgIntake)),
                                stringResource(R.string.g_smart_trend) to stringResource(R.string.g_smart_kg_week, "%+.2f".format(est.kgPerWeek)),
                                stringResource(R.string.g_smart_days_used) to stringResource(R.string.g_smart_days_used_v, est.loggedDays, est.cheatDays),
                                stringResource(R.string.g_smart_weighins_used) to "${est.weighIns}",
                            ).forEach { (k, v) -> SourceRow(k, v) }
                            if (s.mode != SmartMode.AUTO && s.appliedBurn != est.burn) {
                                Spacer(Modifier.height(6.dp))
                                PrimaryButton(stringResource(R.string.r7d_smart_use_burn, fmtInt(est.burn))) { setUndoable(updatedMsg) { it.copy(appliedBurn = est.burn, pendingBurn = null, manualKcal = null) } }
                            }
                        }
                    }
                }
            }

            if (s.history.isNotEmpty()) {
                item(key = "history", contentType = "card") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel(stringResource(R.string.g_smart_weekly))
                        AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                            val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
                            s.history.entries.sortedByDescending { it.key }.take(12).forEach { (d, burn) ->
                                SourceRow(runCatching { LocalDate.parse(d).format(fmt) }.getOrDefault(d), stringResource(R.string.r7d_kcal, fmtInt(burn)))
                            }
                        }
                    }
                }
            }

            item(key = "override", contentType = "card") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(stringResource(R.string.g_smart_override))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SecondaryButton(stringResource(R.string.g_smart_set_own)) { editing = true } }
                        Box(Modifier.weight(1f)) { SecondaryButton(stringResource(R.string.g_smart_reset)) { confirmReset = true } }
                    }
                    Text(stringResource(R.string.g_smart_reset_note), style = U.t.caption, color = c.ink2)
                }
            }
        }
    }

    if (editing) {
        fun kcalOf(v: String) = v.trim().toDoubleOrNull()?.roundToInt()?.takeIf { it in 1000..6000 }
        InputDialog(
            stringResource(R.string.g_smart_dialog_title), stringResource(R.string.g_smart_dialog_label), (s.manualKcal ?: t.kcal).toString(), numeric = true,
            validate = { v -> if (kcalOf(v) == null) rangeMsg else null },
            onDismiss = { editing = false },
        ) { v ->
            kcalOf(v)?.let { k -> setUndoable(manualMsg) { it.copy(mode = SmartMode.MANUAL, manualKcal = k, pendingBurn = null) } }
        }
    }
    if (confirmReset) {
        ConfirmDialog(stringResource(R.string.g_smart_reset_title), stringResource(R.string.r7d_smart_reset_body, fmtInt(formula)), stringResource(R.string.g_smart_reset), destructive = true, onDismiss = { confirmReset = false }) {
            setUndoable(resetMsg) { SmartTarget() }
        }
    }
}

@Composable
private fun Need(label: String, have: Int, need: Int) {
    val c = U.c
    val desc = stringResource(R.string.g_smart_need_desc, label, have.coerceAtMost(need), need)
    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = desc }) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = U.t.bodyS, modifier = Modifier.weight(1f))
            Text("${have.coerceAtMost(need)} / $need", style = U.t.bodyS, fontWeight = FontWeight.Bold, color = if (have >= need) c.leafInk else c.ink2)
        }
        Spacer(Modifier.height(4.dp))
        Track(have / need.toFloat(), if (have >= need) c.leaf else c.tur)
    }
}

