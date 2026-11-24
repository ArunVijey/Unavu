package app.unavu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.FoodDb
import app.unavu.data.NewItem
import app.unavu.data.Portion
import app.unavu.data.fmtQty
import app.unavu.data.unitLabel
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SecondaryButton
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

@Composable
fun PortionsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val removedMsg = stringResource(R.string.i_portion_removed)
    val savedMsg = stringResource(R.string.i_portion_saved)
    val undo = stringResource(R.string.i_undo)
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val list = remember(data.portions) { data.portions.entries.mapNotNull { (id, p) -> FoodDb.get(id)?.let { it to p } }.sortedBy { it.first.name } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_my_portions), onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.cardGap)) {
            AppCard(color = c.leafS, border = null) {
                Text(stringResource(R.string.i_portions_hint), color = c.leafInk, style = U.t.bodyS.iBold())
            }
            if (list.isEmpty()) {
                Text(stringResource(R.string.i_portions_empty), color = c.ink2, style = U.t.body, modifier = Modifier.padding(6.dp))
                SecondaryButton(stringResource(R.string.i_portions_empty_action)) { nav.quickAdd.value = QuickAddRequest() }
            } else {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    list.forEachIndexed { i, (f, p) ->
                        if (i > 0) Divider()
                        val grams = f.unit(p.unit).grams
                        val extra = f.choiceSummary(p.choices).takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                        SettingsRow(
                            f.name,
                            stringResource(R.string.i_portion_line, fmtQty(p.qty), unitLabel(p.unit, p.qty), p.unit, grams.roundToInt(), if (f.liquid) "ml" else "g", extra),
                            f.emoji, trailing = stringResource(R.string.i_edit), chevron = true,
                        ) { editing = f.id }
                    }
                }
            }
        }
    }
    editing?.let { id ->
        val f = FoodDb.get(id)
        val p = data.portions[id]
        if (f != null && p != null) {
            CustomiseSheet(f, NewItem(id, p.qty, p.unit, p.choices), saveLabel = stringResource(R.string.i_remember), onDismiss = { editing = null }, onRemove = {
                val before = nav.store.data.value
                nav.store.update { d -> d.copy(portions = d.portions - id) }
                editing = null
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                feedback?.show(removedMsg, undo) { nav.store.restoreSnapshot(before) }
            }) { ni ->
                nav.store.update { d -> d.copy(portions = d.portions + (id to Portion(ni.qty, ni.unit, ni.choices))) }
                editing = null
                feedback?.show(savedMsg)
            }
        } else editing = null
    }
}
