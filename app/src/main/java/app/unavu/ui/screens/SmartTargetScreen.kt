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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.SecondaryButton
import app.unavu.ui.Segmented
import app.unavu.ui.Track
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Settings › Smart target: watch, accept, override or reset the self-correcting calorie target. */
@Composable
fun SmartTargetScreen() {
    val c = U.c
    val nav = nav
    val store = nav.store
    val data = LocalData.current
    val p = data.profile ?: return
    val t = data.targets() ?: return
    val s = data.smart
    val formula = (bmrOf(p) * p.activity).roundToInt()
    val est = remember(data.entries, data.weights) { Smart.estimate(data) }
    var editing by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    fun set(f: (SmartTarget) -> SmartTarget) = store.update { d -> d.copy(smart = f(d.smart)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("Smart target", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard(color = c.leaf, border = null) {
                Text("Daily target now", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${t.kcal} kcal", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    when {
                        s.manualKcal != null && s.mode != SmartMode.AUTO -> "Set by you"
                        else -> "Burn ${t.maintenance} kcal · ${t.burnSource}"
                    },
                    color = Color.White, fontSize = 12.sp,
                )
            }

            s.pendingBurn?.let { burn ->
                val newKcal = (burn + p.goalKcal).coerceAtLeast(1200)
                AppCard(color = c.turS, border = null) {
                    Text("New estimate: you burn about $burn kcal/day", fontWeight = FontWeight.Bold, color = c.turInk)
                    Text("That makes your target $newKcal kcal (now ${t.kcal}).", fontSize = 12.sp, color = c.ink2, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { SecondaryButton("Not now") { set { it.copy(pendingBurn = null) } } }
                        Box(Modifier.weight(1f)) { PrimaryButton("Use it") { set { it.copy(appliedBurn = burn, pendingBurn = null, manualKcal = null) } } }
                    }
                }
            }

            SectionLabel("How it updates")
            Segmented(listOf("Automatic", "Ask me", "Manual"), s.mode.ordinal) { i ->
                val m = SmartMode.entries[i]
                if (m == SmartMode.MANUAL) editing = true else set { it.copy(mode = m) }
            }
            Text(
                when (s.mode) {
                    SmartMode.AUTO -> "Every week the app re-estimates your burn from what you ate and your weight trend, and updates the target by itself."
                    SmartMode.ASK -> "Every week the app re-estimates your burn. If it changed, you get a card on Today and decide."
                    SmartMode.MANUAL -> "Your own number is used. Estimates are still worked out and listed below so you can compare."
                },
                fontSize = 12.sp, color = c.ink2,
            )

            SectionLabel("Latest estimate")
            AppCard {
                if (est == null) {
                    val from = LocalDate.now().minusDays(Smart.WINDOW_DAYS)
                    val days = Smart.loggedDays(data, from, LocalDate.now().minusDays(1)).size
                    val weighIns = data.weights.count { it.date >= from.toString() }
                    Text("Not enough data yet", fontWeight = FontWeight.Bold)
                    Text("It needs 4 weeks of mostly-logged days and regular weigh-ins. Until then the formula is used ($formula kcal burn).", fontSize = 12.sp, color = c.ink2, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                    Need("Days with 2+ meals logged", days, 14)
                    Spacer(Modifier.height(8.dp))
                    Need("Weigh-ins (last 4 weeks)", weighIns, 6)
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${est.burn}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("  kcal/day burn", color = c.ink2, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Text("Formula says $formula · confidence ${est.confidence}", fontSize = 12.sp, color = c.ink2)
                    Spacer(Modifier.height(6.dp))
                    Divider()
                    listOf(
                        "Average eaten" to "${est.avgIntake} kcal/day",
                        "Weight trend" to "%+.2f kg/week".format(est.kgPerWeek),
                        "Days used" to "${est.loggedDays} (${est.cheatDays} cheat, still counted)",
                        "Weigh-ins used" to "${est.weighIns}",
                    ).forEach { (k, v) -> SourceRow(k, v) }
                    if (s.mode != SmartMode.AUTO && s.appliedBurn != est.burn) {
                        Spacer(Modifier.height(6.dp))
                        PrimaryButton("Use ${est.burn} kcal burn") { set { it.copy(appliedBurn = est.burn, pendingBurn = null, manualKcal = null) } }
                    }
                }
            }

            if (s.history.isNotEmpty()) {
                SectionLabel("Weekly estimates")
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
                    s.history.entries.sortedByDescending { it.key }.take(12).forEach { (d, burn) ->
                        SourceRow(runCatching { LocalDate.parse(d).format(fmt) }.getOrDefault(d), "$burn kcal")
                    }
                }
            }

            SectionLabel("Override")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { SecondaryButton("Set my own target") { editing = true } }
                Box(Modifier.weight(1f)) { SecondaryButton("Reset") { confirmReset = true } }
            }
            Text("Reset goes back to the formula and clears the estimate history.", fontSize = 11.sp, color = c.ink2)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (editing) {
        InputDialog("Your daily target", "kcal per day", (s.manualKcal ?: t.kcal).toString(), numeric = true, onDismiss = { editing = false }) { v ->
            v.trim().toIntOrNull()?.takeIf { it in 1000..6000 }?.let { k -> set { it.copy(mode = SmartMode.MANUAL, manualKcal = k, pendingBurn = null) } }
            editing = false
        }
    }
    if (confirmReset) {
        ConfirmDialog("Reset smart target?", "Your target goes back to the formula ($formula kcal burn) and the estimate history is cleared.", "Reset", destructive = true, onDismiss = { confirmReset = false }) {
            set { SmartTarget() }
            confirmReset = false
        }
    }
}

@Composable
private fun Need(label: String, have: Int, need: Int) {
    val c = U.c
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("${have.coerceAtMost(need)} / $need", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (have >= need) c.leafInk else c.ink2)
    }
    Spacer(Modifier.height(4.dp))
    Track(have / need.toFloat(), if (have >= need) c.leaf else c.tur)
}
