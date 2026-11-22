package app.unavu.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.targets
import app.unavu.ui.AppCard
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.Pill
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Stepper
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.Page
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val t = data.targets() ?: return
    val saved = data.settings.mealSplit.map { (it * 100).roundToInt() }
    var split by rememberSaveable { mutableStateOf(ArrayList(saved)) }
    val splitSaved = stringResource(R.string.i_split_saved)
    val chipsLimit = stringResource(R.string.i_chips_limit)
    val chipsMin = stringResource(R.string.i_chips_min)
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.i_goal_targets), onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard(color = c.leafS, border = null) {
                Text(stringResource(R.string.i_daily_target), color = c.leafInk, style = U.t.label.iBold())
                Text(stringResource(R.string.i_kcal_big, t.kcal), color = c.ink, style = U.t.display.iBold())
                Text(stringResource(R.string.i_macros_line, t.protein, t.carbs, t.fat, t.fibre), color = c.ink, style = U.t.caption)
                Text(stringResource(R.string.i_change_hint), color = c.leafInk, style = U.t.caption, modifier = Modifier.padding(top = 4.dp))
            }
            SecondaryButton(stringResource(R.string.i_edit_details)) { nav.push(Page.Profile) }

            SectionLabel(stringResource(R.string.i_meal_split))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                Meal.entries.forEachIndexed { i, m ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${m.emoji} ${m.label}", style = U.t.body, color = c.ink)
                            Text("${split[i]}% · " + stringResource(R.string.i_kcal_n, (split[i] * t.kcal / 100.0).roundToInt()), style = U.t.caption, color = c.ink2)
                        }
                        Stepper(split[i].toDouble(), 5.0, min = 0.0) { v ->
                            split = ArrayList(split).also { it[i] = v.roundToInt().coerceIn(0, 80) }
                        }
                    }
                }
                val sum = split.sum()
                Text(
                    if (sum == 100) stringResource(R.string.i_total_ok) else stringResource(R.string.i_total_bad, sum),
                    color = if (sum == 100) c.leafInk else c.chiInk, style = U.t.label.iBold(), modifier = Modifier.padding(vertical = 6.dp),
                )
                PrimaryButton(stringResource(R.string.i_save_split), enabled = sum == 100 && split != saved) {
                    store.update { d -> d.copy(settings = d.settings.copy(mealSplit = split.map { it / 100.0 })) }
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    feedback?.show(splitSaved)
                }
                Spacer(Modifier.height(6.dp))
            }

            SectionLabel(stringResource(R.string.i_water_goal))
            AppCard {
                Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.i_daily_water), style = U.t.body, color = c.ink, modifier = Modifier.weight(1f))
                    Stepper(data.settings.waterGoalMl / 1000.0, 0.25, min = 0.5) { v -> store.update { d -> d.copy(settings = d.settings.copy(waterGoalMl = (v * 1000).roundToInt())) } }
                    Text(" " + stringResource(R.string.i_litres_unit, "%.2f".format(data.settings.waterGoalMl / 1000.0).trimEnd('0').trimEnd('.')), style = U.t.body.iBold(), color = c.ink)
                }
            }

            SectionLabel(stringResource(R.string.i_chips_title))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NUTRIENTS.forEach { n ->
                    val chips = data.settings.homeChips
                    val on = n.key in chips
                    Pill(n.name, on) {
                        if (on && chips.size <= 1) { feedback?.show(chipsMin); return@Pill }
                        if (!on && chips.size >= 6) { feedback?.show(chipsLimit); return@Pill }
                        val next = if (on) chips - n.key else chips + n.key
                        store.update { d -> d.copy(settings = d.settings.copy(homeChips = NUTRIENTS.map { it.key }.filter { it in next })) }
                    }
                }
            }
            Text(stringResource(R.string.i_targets_source), style = U.t.caption, color = c.ink2)
            Spacer(Modifier.height(24.dp))
        }
    }
}
