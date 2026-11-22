package app.unavu.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.CustomFood
import app.unavu.data.Facts
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.saveCustomFood
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Fab
import app.unavu.ui.LocalData
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.TextField
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U

import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import app.unavu.R
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.heightIn

private val EMOJIS = listOf("🍽", "🍲", "🍛", "🍜", "🥘", "🍗", "🍖", "🐟", "🍤", "🥚", "🍔", "🍕", "🌯", "🥪", "🍟", "🥗", "🍞", "🥐", "🍰", "🍩", "🍪", "🍫", "🍬", "🍨", "☕", "🍵", "🥤", "🧃", "🍺", "🍎", "🍌", "🥜")

/** Add or edit one of "My foods". Values are for ONE serving, the way pack labels and menus show them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyFoodEditorScreen(page: Page.MyFood) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val existing = page.id?.let { id -> data.customFoods.firstOrNull { it.id == id } }
    val f0 = existing?.facts
    fun s(v: Double?) = v?.takeIf { it > 0 }?.let { fmtQty(it) } ?: ""

    var name by rememberSaveable { mutableStateOf(existing?.name ?: page.name) }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "🍽") }
    var unitName by rememberSaveable { mutableStateOf(existing?.unitName ?: "serving") }
    var unitG by rememberSaveable { mutableStateOf(s(existing?.unitG)) }
    var liquid by rememberSaveable { mutableStateOf(existing?.liquid ?: false) }
    var per100 by rememberSaveable { mutableStateOf(false) }
    var kcal by rememberSaveable { mutableStateOf(s(f0?.kcal)) }
    var protein by rememberSaveable { mutableStateOf(s(f0?.protein)) }
    var carbs by rememberSaveable { mutableStateOf(s(f0?.carbs)) }
    var fat by rememberSaveable { mutableStateOf(s(f0?.fat)) }
    var fibre by rememberSaveable { mutableStateOf(s(f0?.fibre)) }
    var sugar by rememberSaveable { mutableStateOf(s(f0?.sugar)) }
    var satFat by rememberSaveable { mutableStateOf(s(f0?.satFat)) }
    var sodium by rememberSaveable { mutableStateOf(s(f0?.sodium)) }
    var calcium by rememberSaveable { mutableStateOf(s(f0?.calcium)) }
    var iron by rememberSaveable { mutableStateOf(s(f0?.iron)) }
    var more by rememberSaveable { mutableStateOf(f0 != null && (f0.sugar + f0.satFat + f0.sodium + f0.calcium + f0.iron) > 0) }

    val ctx = androidx.compose.ui.platform.LocalResources.current
    val mu = if (liquid) "ml" else "g"
    val grams = unitG.toDoubleOrNull()
    // label values per 100 g need a serving weight to convert
    val scale = if (per100) (grams ?: 0.0) / 100.0 else 1.0
    fun v(x: String) = (x.toDoubleOrNull() ?: 0.0) * scale
    val kcalOk = (kcal.toDoubleOrNull() ?: 0.0) > 0
    val macroKcal = v(protein) * 4 + v(carbs) * 4 + v(fat) * 9
    val canSave = name.isNotBlank() && kcalOk && (!per100 || (grams ?: 0.0) > 0)

    Column(Modifier.fillMaxSize()) {
        TopBar(if (existing == null) "New food" else "Edit food", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TextField(name, "Name, e.g. Chicken shawarma roll (Arabian Hut)") { name = it }
            SectionLabel("Icon")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJIS.forEach { e ->
                    Box(
                        Modifier.defaultMinSize(48.dp, 48.dp).clip(CircleShape).semantics { contentDescription = ctx.getString(R.string.f_icon_n, e) }
                            .selectable(selected = e == emoji, role = Role.RadioButton) { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Emoji(e, if (e == emoji) 46 else 38) }
                }
            }

            SectionLabel("Serving")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(unitName, "Unit (piece, plate, cup…)", Modifier.weight(1f)) { unitName = it.lowercase().take(14) }
                NumberField(unitG, "1 $unitName = ? $mu${if (per100) "" else " (optional)"}", Modifier.weight(1f)) { unitG = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("It's a drink (counts in Liquids)", style = U.t.bodyS, modifier = Modifier.weight(1f))
                AppSwitch(liquid) { liquid = it }
            }

            SectionLabel("Nutrition")
            Segmented(listOf("Per $unitName", "Per 100 $mu (pack label)"), if (per100) 1 else 0) { per100 = it == 1 }
            if (per100 && (grams ?: 0.0) <= 0) Text("Enter how many $mu one $unitName is, above, so the label values can be converted.", style = U.t.caption, color = c.chiInk)
            NumberField(kcal, "Calories (kcal) · required", Modifier.fillMaxWidth()) { kcal = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(protein, "Protein g", Modifier.weight(1f)) { protein = it }
                NumberField(carbs, "Carbs g", Modifier.weight(1f)) { carbs = it }
                NumberField(fat, "Fat g", Modifier.weight(1f)) { fat = it }
            }
            NumberField(fibre, "Fibre g", Modifier.fillMaxWidth()) { fibre = it }
            if (!more) {
                SecondaryButton("More: sugar, sat fat, sodium, calcium, iron") { more = true }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(sugar, "Added sugar g", Modifier.weight(1f)) { sugar = it }
                    NumberField(satFat, "Sat. fat g", Modifier.weight(1f)) { satFat = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(sodium, "Sodium mg", Modifier.weight(1f)) { sodium = it }
                    NumberField(calcium, "Calcium mg", Modifier.weight(1f)) { calcium = it }
                    NumberField(iron, "Iron mg", Modifier.weight(1f)) { iron = it }
                }
            }
            if (kcalOk && macroKcal > 0) {
                val diff = macroKcal - v(kcal)
                if (kotlin.math.abs(diff) > v(kcal) * 0.25 + 20) {
                    Text("Heads-up: protein, carbs and fat add up to ${macroKcal.roundToInt()} kcal, not ${v(kcal).roundToInt()}. Worth a second look.", style = U.t.caption, color = c.turInk)
                }
            }
            Text("Don't know the values? Calories alone is fine. Protein, carbs and fat can stay empty.", style = U.t.caption, color = c.ink2)

            AppCard(color = c.leaf, border = null) {
                Text("ONE ${unitName.uppercase()}", color = Color.White.copy(alpha = 0.85f), style = U.t.caption, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${v(kcal).roundToInt()} kcal", color = Color.White, style = U.t.headline, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("P ${fmtQty(v(protein))} · C ${fmtQty(v(carbs))} · F ${fmtQty(v(fat))} g", color = Color.White, style = U.t.caption)
                }
            }
            val why = when { name.isBlank() -> R.string.f_why_name; !kcalOk -> R.string.f_why_kcal; per100 && (grams ?: 0.0) <= 0 -> R.string.f_why_serving; else -> null }
            if (why != null) Text(stringResource(why), style = U.t.caption, color = c.ink2)
            PrimaryButton(if (existing == null) "Save food" else "Save changes", enabled = canSave) {
                val g = grams?.takeIf { it > 0 } ?: 100.0
                nav.store.saveCustomFood(
                    CustomFood(
                        id = existing?.id ?: newKey("u_"), name = name.trim(), emoji = emoji, ing = emptyMap(), yieldG = g,
                        unitName = unitName.trim().ifBlank { "serving" }, unitG = g, liquid = liquid,
                        facts = Facts(
                            kcal = v(kcal), protein = v(protein), carbs = v(carbs), fat = v(fat), fibre = v(fibre),
                            sugar = v(sugar), satFat = v(satFat), sodium = v(sodium), calcium = v(calcium), iron = v(iron),
                        ),
                    ),
                )
                nav.pop()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

