package app.unavu.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.CustomFood
import app.unavu.data.FoodDb
import app.unavu.data.Nutr
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.saveCustomFood
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.Divider
import app.unavu.ui.InputDialog
import app.unavu.ui.LocalData
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SearchField
import app.unavu.ui.SectionLabel
import app.unavu.ui.TextField
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U

/** Ingredient list (id to raw grams) kept across rotation and process death as "id|grams" strings. */
private val IngredientsSaver = listSaver<SnapshotStateList<Pair<String, Double>>, String>(
    save = { list -> list.map { (k, g) -> "$k|$g" } },
    restore = { saved ->
        saved.mapNotNull { s ->
            val cut = s.lastIndexOf('|')
            if (cut <= 0) null else s.substring(cut + 1).toDoubleOrNull()?.let { s.substring(0, cut) to it }
        }.toMutableStateList()
    },
)

/** Build your own dish from ingredients. */
@Composable
fun RecipeEditorScreen(page: Page.Recipe) {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val existing = page.id?.let { id -> data.customFoods.firstOrNull { it.id == id } }
    val name0 = existing?.name ?: ""
    val emoji0 = existing?.emoji ?: "🍲"
    val ing0 = existing?.ing?.toList() ?: emptyList()
    val cooked0 = existing?.yieldG?.let { fmtQty(it) } ?: ""
    val unitName0 = existing?.unitName ?: "ladle"
    val unitG0 = existing?.unitG?.let { fmtQty(it) } ?: "100"
    val liquid0 = existing?.liquid ?: false

    var name by rememberSaveable { mutableStateOf(name0) }
    var emoji by rememberSaveable { mutableStateOf(emoji0) }
    val ing = rememberSaveable(saver = IngredientsSaver) { ing0.toMutableStateList() }
    var cooked by rememberSaveable { mutableStateOf(cooked0) }
    var unitName by rememberSaveable { mutableStateOf(unitName0) }
    var unitG by rememberSaveable { mutableStateOf(unitG0) }
    var liquid by rememberSaveable { mutableStateOf(liquid0) }
    var query by rememberSaveable { mutableStateOf("") }
    var gramsForId by rememberSaveable { mutableStateOf<String?>(null) }

    val dirty = name != name0 || emoji != emoji0 || ing.toList() != ing0 || cooked != cooked0 ||
        unitName != unitName0 || unitG != unitG0 || liquid != liquid0
    val leave = rememberDiscardGuard(
        dirty,
        stringResource(if (existing == null) R.string.r7d_discard_recipe_title else R.string.r7d_discard_changes_title),
    ) { nav.pop() }

    val mu = if (liquid) "ml" else "g"
    val rawTotal = ing.sumOf { it.second }
    val yieldG = cooked.toDoubleOrNull() ?: rawTotal
    val unitGrams = unitG.toDoubleOrNull() ?: 0.0
    val batch = ing.fold(Nutr()) { acc, (k, g) -> acc + (FoodDb.ingredient(k)?.per100 ?: Nutr()) * (g / 100.0) }
    val perUnit = if (yieldG > 0) batch * (unitGrams / yieldG) else Nutr()
    val servings = if (unitGrams > 0) yieldG / unitGrams else 0.0

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(if (existing == null) R.string.r7d_recipe_new else R.string.r7d_recipe_edit), onBack = leave)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (existing == null && ing.isEmpty()) {
                // Only offered before any ingredient is added, so switching never drops work. Same place in the stack,
                // so saving the food returns to wherever this recipe was opened from.
                AppCard(color = c.turS, border = null, onClick = { nav.pop(); nav.push(Page.MyFood(null, name)) }) {
                    Text(stringResource(R.string.r7d_recipe_know_title), fontWeight = FontWeight.Bold, color = c.turInk)
                    Text(stringResource(R.string.r7d_recipe_know_body), style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(emoji, stringResource(R.string.r7d_icon), Modifier.width(70.dp)) { emoji = it.take(2) }
                TextField(name, stringResource(R.string.r7d_recipe_name), Modifier.weight(1f)) { name = it }
            }
            SectionLabel(stringResource(R.string.r7d_ingredients_label))
            if (ing.isNotEmpty()) {
                AppCard(padding = PaddingValues(start = 18.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                    ing.forEachIndexed { i, (k, g) ->
                        if (i > 0) Divider()
                        val ii = FoodDb.ingredient(k)
                        val label = ii?.name ?: k
                        val kcal = fmtInt((ii?.per100?.kcal ?: 0.0) * g / 100)
                        val rowDesc = stringResource(R.string.r7d_ingredient_row, label, fmtQty(g), kcal)
                        val remove = stringResource(R.string.r7d_remove_named, label)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                Modifier.weight(1f).heightIn(min = 48.dp).clickable(role = Role.Button) { gramsForId = k }
                                    .clearAndSetSemantics { contentDescription = rowDesc; role = Role.Button; onClick { gramsForId = k; true } }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, style = U.t.bodyS, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(R.string.r7d_grams, fmtQty(g)), fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.r7d_kcal, kcal), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(start = 8.dp))
                            }
                            Text(
                                "✕", color = c.ink2,
                                modifier = Modifier.defaultMinSize(48.dp, 48.dp).clip(CircleShape)
                                    .semantics { role = Role.Button; contentDescription = remove }
                                    .clickable { ing.removeAt(i) }.padding(15.dp),
                            )
                        }
                    }
                }
            }
            SearchField(query, stringResource(R.string.r7d_ing_search)) { query = it }
            if (query.isNotBlank()) {
                AppCard(padding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    val res = FoodDb.searchIngredients(query)
                    if (res.isEmpty()) Text(stringResource(R.string.r7d_no_match), style = U.t.bodyS, color = c.ink2, modifier = Modifier.padding(vertical = 12.dp))
                    res.forEach { r ->
                        val add = stringResource(R.string.r7d_add_named, r.name)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { gramsForId = r.id }
                                .semantics(mergeDescendants = true) { contentDescription = add }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = U.t.bodyS, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.r7d_per100, fmtInt(r.per100.kcal), r.group), style = U.t.caption, color = c.ink2)
                            }
                            Text("+", style = U.t.title, color = c.leafInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            SectionLabel(stringResource(R.string.r7d_after_cooking))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(cooked, stringResource(R.string.r7d_cooked_total, mu, fmtInt(rawTotal)), Modifier.weight(1f)) { cooked = it }
                Column(Modifier.weight(1f)) {
                    TextField(unitName, stringResource(R.string.r7d_serving_unit)) { unitName = it.lowercase().take(12) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField(unitG, stringResource(R.string.r7d_unit_equals, unitName.ifBlank { "serving" }, mu), Modifier.weight(1f)) { unitG = it }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.r7d_is_drink), style = U.t.bodyS, modifier = Modifier.weight(1f))
                    AppSwitch(liquid) { liquid = it }
                }
            }
            Text(stringResource(R.string.r7d_cook_tip), style = U.t.caption, color = c.ink2)
            // Solid hero card: heroBg/onHero pass 4.5:1 in every palette (no translucent text on it).
            AppCard(color = c.heroBg, border = null, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(stringResource(R.string.r7d_per_unit_servings, unitName.ifBlank { "serving" }, "%.1f".format(servings)), color = c.onHero, style = U.t.caption, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.r7d_kcal, fmtInt(perUnit.kcal)), color = c.onHero, style = U.t.headline, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.r7d_macros_line, "%.1f".format(perUnit.protein), "%.1f".format(perUnit.carbs), "%.1f".format(perUnit.fat)), color = c.onHero, style = U.t.caption)
                        Text(stringResource(R.string.r7d_sodium_line, fmtInt(perUnit.sodium)), color = c.onHero, style = U.t.caption)
                    }
                }
            }
            val why = when {
                name.isBlank() -> R.string.f_why_name
                ing.isEmpty() -> R.string.r7d_why_ingredients
                yieldG <= 0 || unitGrams <= 0 -> R.string.r7d_why_unit
                else -> null
            }
            if (why != null) Text(stringResource(why), style = U.t.caption, color = c.ink2)
            PrimaryButton(stringResource(if (existing == null) R.string.r7d_save_recipe else R.string.r7d_save_changes), enabled = why == null) {
                val id = existing?.id ?: newKey("u_")
                nav.store.saveCustomFood(
                    CustomFood(id, name.trim(), emoji.ifBlank { "🍲" }, ing.groupBy { it.first }.mapValues { (_, v) -> v.sumOf { it.second } }, yieldG, unitName.ifBlank { "serving" }, unitGrams, liquid),
                )
                nav.popToMyFoods(id)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    gramsForId?.let { id ->
        val ii = FoodDb.ingredient(id)
        if (ii == null) {
            gramsForId = null
        } else {
            val rangeError = stringResource(R.string.r7d_grams_range)
            InputDialog(
                title = ii.name,
                label = stringResource(R.string.r7d_raw_grams),
                initial = ing.firstOrNull { it.first == ii.id }?.second?.let { fmtQty(it) } ?: "",
                numeric = true,
                validate = { s -> if (s.toDoubleOrNull()?.let { it in 1.0..5000.0 } == true) null else rangeError },
                onDismiss = { gramsForId = null },
            ) { s ->
                val v = s.toDoubleOrNull() ?: return@InputDialog
                val i = ing.indexOfFirst { it.first == ii.id }
                if (i >= 0) ing[i] = ii.id to v else ing.add(ii.id to v)
                query = ""
            }
        }
    }
}
