package app.unavu.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.Combo
import app.unavu.data.FoodDb
import app.unavu.data.Meal
import app.unavu.data.deleteCombo
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtQty
import app.unavu.data.nutrFor
import app.unavu.data.useCombo
import app.unavu.ui.AppCard
import app.unavu.ui.Fab
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.MealPicker
import app.unavu.ui.Page
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SlidingSegmented
import app.unavu.ui.EmptyState
import app.unavu.ui.pressScale
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MealsScreen() {
    val c = U.c
    val t = U.t
    val nav = nav
    val store = nav.store
    val data = LocalData.current
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalResources.current
    var tab by rememberSaveable { mutableStateOf(0) }
    var filterName by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = filterName?.let { n -> Meal.entries.firstOrNull { it.name == n } }
    var logComboId by rememberSaveable { mutableStateOf<String?>(null) }
    val undo = stringResource(R.string.f_undo)

    val combos = remember(data.combos, filter) {
        data.combos.filter { filter == null || it.meal == filter }.sortedWith(compareBy<Combo> { it.order }.thenByDescending { it.uses })
    }
    val recipes = remember(data.customFoods) { data.customFoods.filter { it.facts == null } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "top", contentType = "top") { TopBar(stringResource(R.string.f_meals_title)) }
            item(key = "tabs", contentType = "tabs") {
                SlidingSegmented(listOf(stringResource(R.string.f_tab_combos), stringResource(R.string.f_tab_recipes)), tab, Modifier.padding(horizontal = 16.dp)) { tab = it }
            }
            if (tab == 0) {
                item(key = "filters", contentType = "filters") {
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip(stringResource(R.string.f_filter_all), filter == null) { filterName = null }
                        Meal.entries.forEach { m -> ChoiceChip(m.label, filter == m) { filterName = m.name } }
                    }
                }
                if (combos.isEmpty()) {
                    item(key = "empty-combos", contentType = "empty") {
                        EmptyState("⭐", stringResource(R.string.f_no_combos_title), stringResource(R.string.f_no_combos_body), stringResource(R.string.f_build_combo), Modifier.animateItem()) { nav.push(Page.Plate(filter, nav.date.value, asCombo = true)) }
                    }
                }
                items(combos, key = { it.id }, contentType = { "combo" }) { cb ->
                    val idx = combos.indexOf(cb)
                    var menu by rememberSaveable(cb.id) { mutableStateOf(false) }
                    val kcal = remember(cb) { cb.items.sumOf { FoodDb.get(it.foodId)?.nutrFor(it.qty, it.unit, it.choices)?.kcal ?: 0.0 }.roundToInt() }
                    AppCard(onClick = { logComboId = cb.id }, padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), modifier = Modifier.padding(horizontal = 16.dp).animateItem()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Emoji(cb.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(cb.name, style = t.titleS, fontWeight = FontWeight.Bold)
                                Text(cb.items.joinToString(" · ") { "${fmtQty(it.qty)} ${FoodDb.get(it.foodId)?.name ?: "?"}" }, style = t.caption, color = c.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    cb.meal?.let { StatusBadge(it.label, Status.OK) }
                                    Text(stringResource(R.string.f_used_times, cb.uses), style = t.caption, color = c.ink2)
                                }
                            }
                            Text(stringResource(R.string.f_kcal_plain, kcal), style = t.bodyS, fontWeight = FontWeight.ExtraBold)
                            Box {
                                val more = stringResource(R.string.f_more_options, cb.name)
                                Text(
                                    "⋮", style = t.title,
                                    modifier = Modifier.defaultMinSize(48.dp, 48.dp).pressScale().clip(CircleShape).semantics { role = Role.Button; contentDescription = more }
                                        .clickable { menu = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                                )
                                DropdownMenu(menu, { menu = false }, containerColor = c.sf) {
                                    DropdownMenuItem({ Text(stringResource(R.string.f_edit)) }, { menu = false; nav.push(Page.Plate(cb.meal, nav.date.value, comboId = cb.id)) })
                                    if (idx > 0) DropdownMenuItem({ Text(stringResource(R.string.f_move_up)) }, { menu = false; swap(store, combos, idx, idx - 1) })
                                    if (idx < combos.lastIndex) DropdownMenuItem({ Text(stringResource(R.string.f_move_down)) }, { menu = false; swap(store, combos, idx, idx + 1) })
                                    DropdownMenuItem({ Text(stringResource(R.string.f_delete), color = c.chiInk) }, {
                                        menu = false
                                        val before = store.data.value
                                        store.deleteCombo(cb.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        feedback?.show(ctx.getString(R.string.f_combo_deleted, cb.name), undo) { store.restoreSnapshot(before) }
                                    })
                                }
                            }
                        }
                    }
                }
            } else {
                item(key = "add-macros", contentType = "button") {
                    Box(Modifier.padding(horizontal = 16.dp)) { SecondaryButton(stringResource(R.string.f_add_by_macros)) { nav.push(Page.MyFood(null)) } }
                }
                if (recipes.isEmpty()) {
                    item(key = "empty-recipes", contentType = "empty") {
                        EmptyState("🍲", stringResource(R.string.f_no_recipes_title), stringResource(R.string.f_no_recipes_body), stringResource(R.string.f_build_recipe), Modifier.animateItem()) { nav.push(Page.Recipe(null)) }
                    }
                }
                items(recipes, key = { it.id }, contentType = { "recipe" }) { r ->
                    val per = remember(r) { FoodDb.get(r.id)?.nutrFor(1.0, r.unitName) }
                    AppCard(onClick = { nav.push(Page.Recipe(r.id)) }, padding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), modifier = Modifier.padding(horizontal = 16.dp).animateItem()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Emoji(r.emoji)
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = t.titleS, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(R.string.f_recipe_meta, pluralStringResource(R.plurals.f_ingredients, r.ing.size, r.ing.size), r.unitName, r.unitG.roundToInt(), if (r.liquid) "ml" else "g"),
                                    style = t.caption, color = c.ink2,
                                )
                            }
                            Text(stringResource(R.string.f_kcal_plain, per?.kcal?.roundToInt() ?: 0), style = t.bodyS, fontWeight = FontWeight.ExtraBold)
                            val del = stringResource(R.string.f_delete_named, r.name)
                            Text(
                                "🗑", style = t.body,
                                modifier = Modifier.defaultMinSize(48.dp, 48.dp).pressScale().clip(CircleShape).semantics { role = Role.Button; contentDescription = del }
                                    .clickable {
                                        val before = store.data.value
                                        store.deleteCustomFood(r.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        feedback?.show(ctx.getString(R.string.f_recipe_deleted, r.name), undo) { store.restoreSnapshot(before) }
                                    }.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
        if (if (tab == 0) combos.isNotEmpty() else recipes.isNotEmpty()) {
            val pillShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            Text(
                stringResource(if (tab == 0) R.string.f_build_combo else R.string.f_build_recipe),
                style = U.t.label, fontWeight = FontWeight.Bold, color = U.c.ink,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .clip(pillShape)
                    .background(U.c.sf2)
                    .border(1.dp, U.c.line, pillShape)
                    .clickable(role = Role.Button) {
                        if (tab == 0) nav.push(Page.Plate(filter, nav.date.value, asCombo = true)) else nav.push(Page.Recipe(null))
                    }
                    .padding(horizontal = 18.dp, vertical = 13.dp),
            )
        }
    }

    logComboId?.let { id ->
        val cb = data.combos.firstOrNull { it.id == id }
        if (cb == null) logComboId = null
        else {
            var mealName by rememberSaveable(id) { mutableStateOf(cb.meal?.name) }
            val meal = mealName?.let { n -> Meal.entries.firstOrNull { it.name == n } }
            AlertDialog(
                onDismissRequest = { logComboId = null }, containerColor = c.sf,
                title = { Text(stringResource(R.string.f_log_combo_title, cb.name), style = t.title, fontWeight = FontWeight.Bold) },
                text = { Column { Text(stringResource(R.string.f_which_meal_q), color = c.ink2, style = t.caption); Spacer(Modifier.height(6.dp)); MealPicker(meal) { mealName = it.name } } },
                confirmButton = {
                    TextButton({
                        val before = store.data.value
                        store.useCombo(cb, meal!!, nav.date.value)
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        feedback?.show(ctx.getString(R.string.f_combo_logged, cb.name, meal.label), undo) { store.restoreSnapshot(before) }
                        logComboId = null; nav.home()
                    }, enabled = meal != null, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text(stringResource(R.string.f_log), color = c.accInk, style = t.body, fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton({ logComboId = null }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text(stringResource(R.string.f_cancel), color = c.ink2, style = t.body) } },
            )
        }
    }
}

private fun swap(store: app.unavu.data.Store, list: List<Combo>, a: Int, b: Int) {
    val reordered = list.toMutableList().apply { add(b, removeAt(a)) }
    store.update { d -> d.copy(combos = d.combos.map { cb -> reordered.indexOfFirst { it.id == cb.id }.takeIf { it >= 0 }?.let { cb.copy(order = it) } ?: cb }) }
}
