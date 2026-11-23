package app.unavu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.CustomFood
import app.unavu.data.Facts
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.saveCustomFood
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
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
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import kotlin.math.abs

private val EMOJIS = listOf("🍽", "🍲", "🍛", "🍜", "🥘", "🍗", "🍖", "🐟", "🍤", "🥚", "🍔", "🍕", "🌯", "🥪", "🍟", "🥗", "🍞", "🥐", "🍰", "🍩", "🍪", "🍫", "🍬", "🍨", "☕", "🍵", "🥤", "🧃", "🍺", "🍎", "🍌", "🥜")

/** Add or edit one of "My foods". Values are for ONE serving, the way pack labels and menus show them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyFoodEditorScreen(page: Page.MyFood) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val res = LocalResources.current
    val existing = page.id?.let { id -> data.customFoods.firstOrNull { it.id == id } }
    val f0 = existing?.facts
    fun s(v: Double?) = v?.takeIf { it > 0 }?.let { fmtQty(it) } ?: ""

    // Starting values: what "unchanged" means for the discard check.
    val start = listOf(
        existing?.name ?: page.name, existing?.emoji ?: "🍽", existing?.unitName ?: "serving", s(existing?.unitG),
        s(f0?.kcal), s(f0?.protein), s(f0?.carbs), s(f0?.fat), s(f0?.fibre),
        s(f0?.sugar), s(f0?.satFat), s(f0?.sodium), s(f0?.calcium), s(f0?.iron),
    )
    val liquid0 = existing?.liquid ?: false

    var name by rememberSaveable { mutableStateOf(start[0]) }
    var emoji by rememberSaveable { mutableStateOf(start[1]) }
    var unitName by rememberSaveable { mutableStateOf(start[2]) }
    var unitG by rememberSaveable { mutableStateOf(start[3]) }
    var liquid by rememberSaveable { mutableStateOf(liquid0) }
    var per100 by rememberSaveable { mutableStateOf(false) }
    var kcal by rememberSaveable { mutableStateOf(start[4]) }
    var protein by rememberSaveable { mutableStateOf(start[5]) }
    var carbs by rememberSaveable { mutableStateOf(start[6]) }
    var fat by rememberSaveable { mutableStateOf(start[7]) }
    var fibre by rememberSaveable { mutableStateOf(start[8]) }
    var sugar by rememberSaveable { mutableStateOf(start[9]) }
    var satFat by rememberSaveable { mutableStateOf(start[10]) }
    var sodium by rememberSaveable { mutableStateOf(start[11]) }
    var calcium by rememberSaveable { mutableStateOf(start[12]) }
    var iron by rememberSaveable { mutableStateOf(start[13]) }
    var more by rememberSaveable { mutableStateOf(f0 != null && (f0.sugar + f0.satFat + f0.sodium + f0.calcium + f0.iron) > 0) }

    val now = listOf(name, emoji, unitName, unitG, kcal, protein, carbs, fat, fibre, sugar, satFat, sodium, calcium, iron)
    val dirty = now != start || liquid != liquid0 || per100
    val leave = rememberDiscardGuard(
        dirty,
        stringResource(if (existing == null) R.string.r7d_discard_food_title else R.string.r7d_discard_changes_title),
    ) { nav.pop() }

    val mu = if (liquid) "ml" else "g"
    val unitShown = unitName.ifBlank { "serving" }
    val grams = unitG.toDoubleOrNull()
    // label values per 100 g need a serving weight to convert
    val scale = if (per100) (grams ?: 0.0) / 100.0 else 1.0
    fun v(x: String) = (x.toDoubleOrNull() ?: 0.0) * scale
    val kcalOk = (kcal.toDoubleOrNull() ?: 0.0) > 0
    val macroKcal = v(protein) * 4 + v(carbs) * 4 + v(fat) * 9

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(if (existing == null) R.string.r7d_food_new else R.string.r7d_food_edit), onBack = leave)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TextField(name, stringResource(R.string.r7d_food_name)) { name = it }
            SectionLabel(stringResource(R.string.r7d_icon))
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EMOJIS.forEach { e ->
                    Box(
                        Modifier.defaultMinSize(48.dp, 48.dp).clip(CircleShape).semantics { contentDescription = res.getString(R.string.f_icon_n, e) }
                            .selectable(selected = e == emoji, role = Role.RadioButton) { emoji = e },
                        contentAlignment = Alignment.Center,
                    ) { Emoji(e, if (e == emoji) 46 else 38) }
                }
            }

            SectionLabel(stringResource(R.string.r7d_serving))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(unitName, stringResource(R.string.r7d_unit_hint), Modifier.weight(1f)) { unitName = it.lowercase().take(14) }
                NumberField(
                    unitG,
                    stringResource(if (per100) R.string.r7d_unit_equals else R.string.r7d_unit_equals_opt, unitShown, mu),
                    Modifier.weight(1f),
                ) { unitG = it }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.r7d_is_drink_liquids), style = U.t.bodyS, modifier = Modifier.weight(1f))
                AppSwitch(liquid) { liquid = it }
            }

            SectionLabel(stringResource(R.string.r7d_nutrition))
            Segmented(listOf(stringResource(R.string.r7d_per_unit_seg, unitShown), stringResource(R.string.r7d_per100_seg, mu)), if (per100) 1 else 0) { per100 = it == 1 }
            if (per100 && (grams ?: 0.0) <= 0) Text(stringResource(R.string.r7d_need_serving, mu, unitShown), style = U.t.caption, color = c.chiInk)
            NumberField(kcal, stringResource(R.string.r7d_kcal_required), Modifier.fillMaxWidth()) { kcal = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(protein, stringResource(R.string.r7d_protein_g), Modifier.weight(1f)) { protein = it }
                NumberField(carbs, stringResource(R.string.r7d_carbs_g), Modifier.weight(1f)) { carbs = it }
                NumberField(fat, stringResource(R.string.r7d_fat_g), Modifier.weight(1f)) { fat = it }
            }
            NumberField(fibre, stringResource(R.string.r7d_fibre_g), Modifier.fillMaxWidth()) { fibre = it }
            if (!more) {
                SecondaryButton(stringResource(R.string.r7d_more_nutrients)) { more = true }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(sugar, stringResource(R.string.r7d_sugar_g), Modifier.weight(1f)) { sugar = it }
                    NumberField(satFat, stringResource(R.string.r7d_satfat_g), Modifier.weight(1f)) { satFat = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberField(sodium, stringResource(R.string.r7d_sodium_mg), Modifier.weight(1f)) { sodium = it }
                    NumberField(calcium, stringResource(R.string.r7d_calcium_mg), Modifier.weight(1f)) { calcium = it }
                    NumberField(iron, stringResource(R.string.r7d_iron_mg), Modifier.weight(1f)) { iron = it }
                }
            }
            if (kcalOk && macroKcal > 0 && abs(macroKcal - v(kcal)) > v(kcal) * 0.25 + 20) {
                Text(stringResource(R.string.r7d_macro_mismatch, fmtInt(macroKcal), fmtInt(v(kcal))), style = U.t.caption, color = c.turInk)
            }
            Text(stringResource(R.string.r7d_values_tip), style = U.t.caption, color = c.ink2)

            // Solid hero card: heroBg/onHero pass 4.5:1 in every palette (no translucent text on it).
            AppCard(color = c.heroBg, border = null, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(stringResource(R.string.r7d_one_unit, unitShown), color = c.onHero, style = U.t.caption, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.r7d_kcal, fmtInt(v(kcal))), color = c.onHero, style = U.t.headline, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.r7d_macros_line, fmtQty(v(protein)), fmtQty(v(carbs)), fmtQty(v(fat))), color = c.onHero, style = U.t.caption)
                }
            }
            val why = when { name.isBlank() -> R.string.f_why_name; !kcalOk -> R.string.f_why_kcal; per100 && (grams ?: 0.0) <= 0 -> R.string.f_why_serving; else -> null }
            if (why != null) Text(stringResource(why), style = U.t.caption, color = c.ink2)
            PrimaryButton(stringResource(if (existing == null) R.string.r7d_save_food else R.string.r7d_save_changes), enabled = why == null) {
                val g = grams?.takeIf { it > 0 } ?: 100.0
                val id = existing?.id ?: newKey("u_")
                nav.store.saveCustomFood(
                    CustomFood(
                        id = id, name = name.trim(), emoji = emoji, ing = emptyMap(), yieldG = g,
                        unitName = unitShown.trim(), unitG = g, liquid = liquid,
                        facts = Facts(
                            kcal = v(kcal), protein = v(protein), carbs = v(carbs), fat = v(fat), fibre = v(fibre),
                            sugar = v(sugar), satFat = v(satFat), sodium = v(sodium), calcium = v(calcium), iron = v(iron),
                        ),
                    ),
                )
                nav.popToMyFoods(id)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
