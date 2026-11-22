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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.defaultMinSize
import app.unavu.R
import app.unavu.ui.LocalFeedback
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
import app.unavu.data.CustomFood
import app.unavu.data.Facts
import app.unavu.data.deleteCustomFood
import app.unavu.data.fmtQty
import app.unavu.data.newKey
import app.unavu.data.saveCustomFood
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
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

/** Settings › My foods: anything new you eat, saved from a pack label, a menu, or your best guess. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyFoodsScreen() {
    val c = U.c
    val t = U.t
    val nav = nav
    val store = nav.store
    val data = LocalData.current
    val feedback = LocalFeedback.current
    val haptic = LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalResources.current
    val foods = remember(data.customFoods) { data.customFoods.filter { it.facts != null }.sortedBy { it.name.lowercase() } }
    val undo = stringResource(R.string.f_undo)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(key = "top", contentType = "top") { TopBar(stringResource(R.string.f_myfoods_title), onBack = { nav.pop() }) }
            item(key = "intro", contentType = "card") {
                AppCard(Modifier.padding(horizontal = 18.dp), color = c.leafS, border = null) {
                    Text(stringResource(R.string.f_ate_new), style = t.titleS, fontWeight = FontWeight.Bold, color = c.leafInk)
                    Text(stringResource(R.string.f_ate_new_body), style = t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton(stringResource(R.string.f_add_new_food)) { nav.push(Page.MyFood(null)) }
                }
            }
            item(key = "label", contentType = "label") {
                Box(Modifier.padding(horizontal = 18.dp)) {
                    SectionLabel(if (foods.isEmpty()) stringResource(R.string.f_no_foods_yet) else stringResource(R.string.f_your_foods, foods.size))
                }
            }
            items(foods, key = { it.id }, contentType = { "food" }) { f ->
                val x = f.facts!!
                AppCard(onClick = { nav.push(Page.MyFood(f.id)) }, padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), modifier = Modifier.padding(horizontal = 18.dp).animateItem()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Emoji(f.emoji)
                        Column(Modifier.weight(1f)) {
                            Text(f.name, style = t.body, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.f_food_meta, f.unitName, fmtQty(f.unitG), if (f.liquid) "ml" else "g", fmtQty(x.protein), fmtQty(x.carbs), fmtQty(x.fat)),
                                style = t.caption, color = c.ink2,
                            )
                        }
                        Text(stringResource(R.string.f_kcal_plain, x.kcal.roundToInt()), style = t.bodyS, fontWeight = FontWeight.ExtraBold)
                        val del = stringResource(R.string.f_delete_named, f.name)
                        Text(
                            "🗑", style = t.body,
                            modifier = Modifier.defaultMinSize(48.dp, 48.dp).clip(CircleShape).semantics { role = Role.Button; contentDescription = del }
                                .clickable {
                                    val before = store.data.value
                                    store.deleteCustomFood(f.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    feedback?.show(ctx.getString(R.string.f_food_deleted, f.name), undo) { store.restoreSnapshot(before) }
                                }.padding(12.dp),
                        )
                    }
                }
            }
            item(key = "home-label", contentType = "label") { Box(Modifier.padding(horizontal = 18.dp)) { SectionLabel(stringResource(R.string.f_cooked_home)) } }
            item(key = "home-card", contentType = "card") {
                AppCard(onClick = { nav.push(Page.Recipe(null)) }, modifier = Modifier.padding(horizontal = 18.dp)) {
                    Text(stringResource(R.string.f_build_recipe_title), style = t.titleS, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.f_build_recipe_body), style = t.bodyS, color = c.ink2, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        Fab(Modifier.align(Alignment.BottomEnd).padding(18.dp)) { nav.push(Page.MyFood(null)) }
    }
}
