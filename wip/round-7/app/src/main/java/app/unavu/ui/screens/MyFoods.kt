package app.unavu.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
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
import app.unavu.data.CustomFood
import app.unavu.data.FoodDb
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtInt
import app.unavu.data.fmtQty
import app.unavu.data.nutrFor
import app.unavu.ui.AppCard
import app.unavu.ui.LocalData
import app.unavu.ui.LocalFeedback
import app.unavu.ui.Nav
import app.unavu.ui.Page
import app.unavu.ui.SecondaryButton
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Tab
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.pressScale
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import app.unavu.ui.theme.motionTween
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/*
 * "My foods" is one place for everything the user made: foods added by their calories (CustomFood.facts != null)
 * and recipes built from ingredients (facts == null). It is the second segment of the Meals tab, and Settings ›
 * My foods opens the same list as a page. The editors hand the saved id back through [MyFoodsFocus], so the list
 * the editor returns to shows that item and flashes it once.
 */

/** One-shot hand-off from the food / recipe editors to the My foods list: the id that was just saved. */
internal object MyFoodsFocus {
    var id by mutableStateOf<String?>(null)
}

/**
 * Leave an editor after saving [id]. When the editor was opened from a My foods list (Meals › My foods, or
 * Settings › My foods), that list scrolls to the item and flashes it; from anywhere else (plate builder, quick add)
 * it just goes back.
 */
internal fun Nav.popToMyFoods(id: String) {
    val below = stack.getOrNull(stack.lastIndex - 1)
    if (below == Page.MyFoods || (below == null && tab.value == Tab.MEALS)) MyFoodsFocus.id = id
    pop()
}

/** Rows of the My foods list: a header per section, then its items (or an empty hint). */
internal sealed interface MyFoodsRow {
    val key: String
    class Header(val recipes: Boolean, val count: Int) : MyFoodsRow { override val key = if (recipes) "mf-h-recipes" else "mf-h-foods" }
    class Empty(val recipes: Boolean) : MyFoodsRow { override val key = if (recipes) "mf-e-recipes" else "mf-e-foods" }
    class Item(val food: CustomFood) : MyFoodsRow { override val key = "mf-" + food.id }
}

/** Foods first (A–Z), then recipes (A–Z). */
internal fun myFoodsRows(custom: List<CustomFood>): List<MyFoodsRow> {
    val foods = custom.filter { it.facts != null }.sortedBy { it.name.lowercase() }
    val recipes = custom.filter { it.facts == null }.sortedBy { it.name.lowercase() }
    return buildList {
        add(MyFoodsRow.Header(false, foods.size))
        if (foods.isEmpty()) add(MyFoodsRow.Empty(false)) else foods.forEach { add(MyFoodsRow.Item(it)) }
        add(MyFoodsRow.Header(true, recipes.size))
        if (recipes.isEmpty()) add(MyFoodsRow.Empty(true)) else recipes.forEach { add(MyFoodsRow.Item(it)) }
    }
}

/** The My foods rows inside a LazyColumn (shared by Meals › My foods and Settings › My foods). */
internal fun LazyListScope.myFoodsItems(rows: List<MyFoodsRow>, flashId: String?) {
    items(rows, key = { it.key }, contentType = { r -> when (r) { is MyFoodsRow.Header -> "mf-header"; is MyFoodsRow.Empty -> "mf-empty"; is MyFoodsRow.Item -> "mf-item" } }) { row ->
        val m = Modifier.padding(horizontal = Space.gutter).animateItem()
        when (row) {
            is MyFoodsRow.Header -> MyFoodsHeader(row.recipes, row.count, m)
            is MyFoodsRow.Empty -> MyFoodsEmpty(row.recipes, m)
            is MyFoodsRow.Item -> MyFoodRow(row.food, row.food.id == flashId, m)
        }
    }
}

/**
 * Scrolls [state] to the item an editor just saved (only if it isn't fully in view), keeps it flashed for a
 * moment, then clears the hand-off. [firstIndex] is the LazyColumn index of the first My foods row.
 */
@Composable
internal fun FocusSavedFood(state: LazyListState, rows: List<MyFoodsRow>, firstIndex: Int) {
    val id = MyFoodsFocus.id
    val reduced = LocalReducedMotion.current
    LaunchedEffect(id, rows) {
        if (id == null) return@LaunchedEffect
        val i = rows.indexOfFirst { it is MyFoodsRow.Item && it.food.id == id }
        if (i < 0) return@LaunchedEffect
        val target = firstIndex + i
        snapshotFlow { state.layoutInfo.totalItemsCount }.first { it > target }
        val info = state.layoutInfo
        val seen = info.visibleItemsInfo.firstOrNull { it.index == target }
        val fullyVisible = seen != null && seen.offset >= info.viewportStartOffset && seen.offset + seen.size <= info.viewportEndOffset - 96
        if (!fullyVisible) {
            // land it a little below the top so the section header above stays in view
            val idx = (target - 1).coerceAtLeast(0)
            if (reduced) state.scrollToItem(idx) else state.animateScrollToItem(idx)
        }
        delay(1600)
        MyFoodsFocus.id = null
    }
}

@Composable
private fun MyFoodsHeader(recipes: Boolean, count: Int, modifier: Modifier) {
    val c = U.c
    val nav = nav
    val title = stringResource(if (recipes) R.string.r7d_sec_recipes else R.string.r7d_sec_foods)
    val action = stringResource(if (recipes) R.string.r7d_add_recipe else R.string.r7d_add_food)
    val actionDesc = stringResource(if (recipes) R.string.r7d_add_recipe_desc else R.string.r7d_add_food_desc)
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(if (count > 0) stringResource(R.string.r7d_sec_count, title, count) else title, Modifier.weight(1f))
            Text(
                action, style = U.t.label, fontWeight = FontWeight.Bold, color = c.accInk,
                modifier = Modifier.padding(top = 8.dp).heightIn(min = 48.dp).pressScale().clip(RoundedCornerShape(12.dp))
                    .semantics { contentDescription = actionDesc }
                    .clickable(role = Role.Button) { nav.push(if (recipes) Page.Recipe(null) else Page.MyFood(null)) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            )
        }
        Text(
            stringResource(if (recipes) R.string.r7d_sec_recipes_sub else R.string.r7d_sec_foods_sub),
            style = U.t.caption, color = c.ink2, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun MyFoodsEmpty(recipes: Boolean, modifier: Modifier) {
    val c = U.c
    val nav = nav
    AppCard(modifier, color = c.sf2, border = null) {
        Text(stringResource(if (recipes) R.string.r7d_recipes_empty else R.string.r7d_foods_empty), style = U.t.bodyS, color = c.ink2)
        Spacer(Modifier.height(10.dp))
        SecondaryButton(stringResource(if (recipes) R.string.f_build_recipe else R.string.f_add_new_food)) {
            nav.push(if (recipes) Page.Recipe(null) else Page.MyFood(null))
        }
    }
}

/** A food or recipe card: tap to edit, 🗑 to delete (with Undo). [flash] tints it after it was just saved. */
@Composable
private fun MyFoodRow(f: CustomFood, flash: Boolean, modifier: Modifier) {
    val c = U.c
    val t = U.t
    val nav = nav
    val store = nav.store
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val res = LocalResources.current
    val undo = stringResource(R.string.f_undo)
    val facts = f.facts
    val isRecipe = facts == null
    val kcal = remember(f) { facts?.kcal ?: FoodDb.get(f.id)?.nutrFor(1.0, f.unitName)?.kcal ?: 0.0 }
    val mu = if (f.liquid) "ml" else "g"
    val meta = if (facts != null) {
        stringResource(R.string.f_food_meta, f.unitName, fmtQty(f.unitG), mu, fmtQty(facts.protein), fmtQty(facts.carbs), fmtQty(facts.fat))
    } else {
        stringResource(R.string.f_recipe_meta, pluralStringResource(R.plurals.f_ingredients, f.ing.size, f.ing.size), f.unitName, f.unitG.roundToInt(), mu)
    }
    val a by animateFloatAsState(if (flash) 1f else 0f, motionTween(Motion.LONG), label = "savedFlash")
    AppCard(
        modifier, color = lerp(c.sf, c.accS, a), border = lerp(c.line, c.acc, a),
        padding = PaddingValues(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        onClick = { nav.push(if (isRecipe) Page.Recipe(f.id) else Page.MyFood(f.id)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Emoji(f.emoji)
            Column(Modifier.weight(1f)) {
                Text(f.name, style = t.titleS, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(meta, style = t.caption, color = c.ink2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (flash) StatusBadge(stringResource(R.string.r7d_just_saved), Status.OK, Modifier.padding(top = 4.dp))
            }
            Text(stringResource(R.string.r7d_kcal, fmtInt(kcal)), style = t.bodyS, fontWeight = FontWeight.ExtraBold)
            val del = stringResource(R.string.f_delete_named, f.name)
            Box(
                Modifier.defaultMinSize(48.dp, 48.dp).pressScale().clip(CircleShape).semantics { role = Role.Button; contentDescription = del }
                    .clickable {
                        val before = store.data.value
                        store.deleteCustomFood(f.id)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val msg = if (isRecipe) R.string.f_recipe_deleted else R.string.f_food_deleted
                        feedback?.show(res.getString(msg, f.name), undo) { store.restoreSnapshot(before) }
                    },
                contentAlignment = Alignment.Center,
            ) { Text("🗑", style = t.body) }
        }
    }
}

/** Settings › My foods: the same list as Meals › My foods, as a page with a back arrow. */
@Composable
fun MyFoodsScreen() {
    val nav = nav
    val data = LocalData.current
    val rows = remember(data.customFoods) { myFoodsRows(data.customFoods) }
    val state = rememberLazyListState()
    FocusSavedFood(state, rows, firstIndex = 1)
    LazyColumn(
        Modifier.fillMaxSize(), state = state,
        contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(Space.cardGap),
    ) {
        item(key = "top", contentType = "top") { TopBar(stringResource(R.string.f_myfoods_title), onBack = { nav.pop() }) }
        myFoodsItems(rows, MyFoodsFocus.id)
    }
}
