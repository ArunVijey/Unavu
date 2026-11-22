package app.unavu.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.Combo
import app.unavu.data.CustomFood
import app.unavu.data.FoodDb
import app.unavu.data.Ingredient
import app.unavu.data.Meal
import app.unavu.data.Nutr
import app.unavu.data.deleteCombo
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.nutrFor
import app.unavu.data.saveCombo
import app.unavu.data.saveCustomFood
import app.unavu.data.useCombo
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Divider
import app.unavu.ui.Fab
import app.unavu.ui.LocalData
import app.unavu.ui.MealPicker
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SearchField
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TextField
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.unavu.R

/** Build your own dish from ingredients. */
@Composable
fun RecipeEditorScreen(page: Page.Recipe) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val existing = page.id?.let { id -> data.customFoods.firstOrNull { it.id == id } }
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "🍲") }
    val ing = remember { mutableStateListOf<Pair<String, Double>>().apply { existing?.ing?.forEach { (k, v) -> add(k to v) } } }
    var cooked by rememberSaveable { mutableStateOf(existing?.yieldG?.let { fmtQty(it) } ?: "") }
    var unitName by rememberSaveable { mutableStateOf(existing?.unitName ?: "ladle") }
    var unitG by rememberSaveable { mutableStateOf(existing?.unitG?.let { fmtQty(it) } ?: "100") }
    var liquid by rememberSaveable { mutableStateOf(existing?.liquid ?: false) }
    var query by rememberSaveable { mutableStateOf("") }
    var gramsFor by remember { mutableStateOf<Ingredient?>(null) }

    val rawTotal = ing.sumOf { it.second }
    val yieldG = cooked.toDoubleOrNull() ?: rawTotal
    val batch = ing.fold(Nutr()) { acc, (k, g) -> acc + (FoodDb.ingredient(k)?.per100 ?: Nutr()) * (g / 100.0) }
    val perUnit = if (yieldG > 0) batch * ((unitG.toDoubleOrNull() ?: 0.0) / yieldG) else Nutr()
    val ctxRemove = stringResource(R.string.f_remove)
    val servings = if ((unitG.toDoubleOrNull() ?: 0.0) > 0) yieldG / unitG.toDouble() else 0.0

    Column(Modifier.fillMaxSize()) {
        TopBar(if (existing == null) "New recipe" else "Edit recipe", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (existing == null) {
                AppCard(color = c.turS, border = null, onClick = { nav.pop(); nav.push(Page.MyFood(null, name)) }) {
                    Text("Know the calories, protein and fat already?", fontWeight = FontWeight.Bold, color = c.turInk)
                    Text("From a pack label, a menu or a guess: tap here to type the values in directly instead of building from ingredients.", style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(emoji, "Icon", Modifier.width(70.dp)) { emoji = it.take(2) }
                TextField(name, "Recipe name, e.g. Amma's puli kuzhambu", Modifier.weight(1f)) { name = it }
            }
            SectionLabel("Ingredients (raw weight) · from IFCT 2017")
            if (ing.isNotEmpty()) {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    ing.forEachIndexed { i, (k, g) ->
                        if (i > 0) Divider()
                        val ii = FoodDb.ingredient(k)
                        Row(Modifier.fillMaxWidth().clickable { gramsFor = ii }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(ii?.name ?: k, style = U.t.bodyS, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${fmtQty(g)} g", fontWeight = FontWeight.Bold)
                            Text("  ${((ii?.per100?.kcal ?: 0.0) * g / 100).roundToInt()} kcal", style = U.t.caption, color = c.ink2)
                            Text("✕", color = c.ink2, modifier = Modifier.defaultMinSize(48.dp, 48.dp).clip(CircleShape).semantics { role = Role.Button; contentDescription = ctxRemove }.clickable { ing.removeAt(i) }.padding(12.dp))
                        }
                    }
                }
            }
            SearchField(query, "Add ingredient: toor dal, tamarind, oil…") { query = it }
            if (query.isNotBlank()) {
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    val res = FoodDb.searchIngredients(query)
                    if (res.isEmpty()) Text("No match", color = c.ink2, modifier = Modifier.padding(8.dp))
                    res.forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { gramsFor = r }.padding(vertical = 8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = U.t.bodyS, fontWeight = FontWeight.SemiBold)
                                Text("${r.per100.kcal.roundToInt()} kcal / 100 g · ${r.group}", style = U.t.caption, color = c.ink2)
                            }
                            Text("+", style = U.t.title, color = c.leafInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            SectionLabel("After cooking")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(cooked, "Cooked total ${if (liquid) "ml" else "g"} (raw ${rawTotal.roundToInt()})", Modifier.weight(1f)) { cooked = it }
                Column(Modifier.weight(1f)) {
                    TextField(unitName, "Serving unit") { unitName = it.lowercase().take(12) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(unitG, "1 $unitName = ? ${if (liquid) "ml" else "g"}", Modifier.weight(1f)) { unitG = it }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text("It's a drink", style = U.t.bodyS, modifier = Modifier.weight(1f))
                    AppSwitch(liquid) { liquid = it }
                }
            }
            Text("Tip: weigh the pot empty and full after cooking to get the cooked total.", style = U.t.caption, color = c.ink2)
            AppCard(color = c.leaf, border = null) {
                Text("PER ${unitName.uppercase()} · ${"%.1f".format(servings)} servings", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f), style = U.t.caption, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${perUnit.kcal.roundToInt()} kcal", color = androidx.compose.ui.graphics.Color.White, style = U.t.headline, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("P ${"%.1f".format(perUnit.protein)} · C ${"%.1f".format(perUnit.carbs)} · F ${"%.1f".format(perUnit.fat)} g\nNa ${perUnit.sodium.roundToInt()} mg", color = androidx.compose.ui.graphics.Color.White, style = U.t.caption)
                }
            }
            PrimaryButton("Save recipe", enabled = name.isNotBlank() && ing.isNotEmpty() && yieldG > 0 && (unitG.toDoubleOrNull() ?: 0.0) > 0) {
                nav.store.saveCustomFood(
                    CustomFood(existing?.id ?: newKey("u_"), name.trim(), emoji.ifBlank { "🍲" }, ing.groupBy { it.first }.mapValues { (_, v) -> v.sumOf { it.second } }, yieldG, unitName.ifBlank { "serving" }, unitG.toDouble(), liquid),
                )
                nav.pop()
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    gramsFor?.let { ii ->
        var g by remember { mutableStateOf(ing.firstOrNull { it.first == ii.id }?.second?.let { fmtQty(it) } ?: "") }
        AlertDialog(
            onDismissRequest = { gramsFor = null }, containerColor = c.sf,
            title = { Text(ii.name, fontWeight = FontWeight.Bold) },
            text = { NumberField(g, "Raw grams in the whole batch") { g = it } },
            confirmButton = {
                TextButton({
                    val v = g.toDoubleOrNull() ?: 0.0
                    val i = ing.indexOfFirst { it.first == ii.id }
                    if (v > 0) { if (i >= 0) ing[i] = ii.id to v else ing.add(ii.id to v) }
                    gramsFor = null; query = ""
                }) { Text("Save", color = c.accInk, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ gramsFor = null }) { Text("Cancel", color = c.ink2) } },
        )
    }
}
