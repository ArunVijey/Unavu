package app.unavu.ui.screens

import app.unavu.data.fmtQty
import app.unavu.ui.SelectChip

import app.unavu.ui.theme.edge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.unavu.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.Choices
import app.unavu.data.Food
import app.unavu.data.NewItem
import app.unavu.data.OIL
import app.unavu.data.OIL_LEVELS
import app.unavu.data.nutrFor
import app.unavu.data.unitLabel
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.SlidingSegmented
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Stepper
import app.unavu.ui.TamilText
import app.unavu.ui.theme.U
import app.unavu.ui.theme.motionTween
import app.unavu.ui.pressScale
import androidx.compose.animation.animateColorAsState
import kotlin.math.roundToInt

/** Bottom sheet to customise one item: choices (vegetables, dal, milk, sugar…), oil, amount, unit. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomiseSheet(
    food: Food,
    initial: NewItem,
    saveLabel: String? = null,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onSave: (NewItem) -> Unit,
) {
    val c = U.c
    var qty by rememberSaveable { mutableStateOf(initial.qty) }
    var unit by rememberSaveable { mutableStateOf(initial.unit.takeIf { u -> food.units.any { it.name == u } } ?: food.units.first().name) }
    var choices by remember { mutableStateOf(food.resolve(initial.choices)) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val n = food.nutrFor(qty, unit, choices)
    val isWeight = unit == "g" || unit == "ml"
    val step = if (isWeight) 10.0 else 0.5

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.bg) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("${food.emoji} ${food.name}", style = U.t.title, fontWeight = FontWeight.Bold)
                    if (food.tamil.isNotBlank()) TamilText(food.tamil)
                    StatusBadge(food.source.label, Status.INFO, Modifier.padding(top = 4.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${n.kcal.roundToInt()}", style = U.t.headline, fontWeight = FontWeight.ExtraBold)
                    Text("kcal", style = U.t.caption, color = c.ink2)
                }
            }
            Text(
                "P ${n.protein.roundToInt()} g · C ${n.carbs.roundToInt()} g · F ${n.fat.roundToInt()} g · Fibre ${"%.1f".format(n.fibre)} g",
                style = U.t.caption, color = c.ink2, modifier = Modifier.padding(top = 6.dp),
            )

            food.choices.forEach { g ->
                SectionLabel(if (g.multi) stringResource(R.string.f_pick_any, g.label) else g.label)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    g.options.forEach { o ->
                        val picked = choices[g.id].orEmpty()
                        val on = o.id in picked
                        ChoiceChip(o.label, on) {
                            val next = if (g.multi) {
                                if (on) picked - o.id else picked + o.id
                            } else listOf(o.id)
                            choices = choices + (g.id to next)
                        }
                    }
                }
            }
            if (food.hasOil) {
                SectionLabel(stringResource(R.string.f_oil_ghee))
                val cur = choices[OIL]?.firstOrNull() ?: "usual"
                SlidingSegmented(OIL_LEVELS.map { it.second }, OIL_LEVELS.indexOfFirst { it.first == cur }) { i -> choices = choices + (OIL to listOf(OIL_LEVELS[i].first)) }
            }

            SectionLabel(stringResource(R.string.f_how_much))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(qty, step, min = step) { qty = it }
                Spacer(Modifier.weight(1f))
                Text("≈ ${(qty * food.unit(unit).grams).roundToInt()} ${if (food.liquid) "ml" else "g"}", style = U.t.caption, color = c.ink2)
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                food.units.forEach { u ->
                    ChoiceChip(if (u.name == "g" || u.name == "ml") u.name else "${u.name} · ${u.grams.roundToInt()}${if (food.liquid) "ml" else "g"}", u.name == unit) {
                        if (u.name != unit) {
                            // keep the same amount of food when switching units
                            val grams = qty * food.unit(unit).grams
                            unit = u.name
                            qty = if (u.name == "g" || u.name == "ml") (grams / 5).roundToInt() * 5.0 else ((grams / u.grams) * 2).roundToInt().coerceAtLeast(1) / 2.0
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("${saveLabel ?: stringResource(R.string.f_done)} · ${qtyText(qty, unit)}") { onSave(NewItem(food.id, qty, unit, choices)) }
            if (onRemove != null) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton(stringResource(R.string.f_remove)) { onRemove() }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun qtyText(q: Double, unit: String) = "${fmtQty(q)} ${unitLabel(unit, q)}"

/** Option chip in the shared selection style (see [SelectChip]). */
@Composable
fun ChoiceChip(text: String, on: Boolean, onClick: () -> Unit) =
    SelectChip(text, on, role = Role.Checkbox, onClick = onClick)
