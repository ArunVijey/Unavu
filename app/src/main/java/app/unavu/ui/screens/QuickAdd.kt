package app.unavu.ui.screens

import app.unavu.ui.EmojiText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.Combo
import app.unavu.data.FoodDb
import app.unavu.data.Meal
import app.unavu.data.NewItem
import app.unavu.data.QuickParser
import app.unavu.data.addWater
import app.unavu.data.addWeight
import app.unavu.data.fmtQty
import app.unavu.data.logItems
import app.unavu.data.nutrFor
import app.unavu.data.toNew
import app.unavu.data.unitLabel
import app.unavu.ui.AppCard
import app.unavu.ui.Arrow
import app.unavu.ui.ArrowButton
import app.unavu.ui.LocalData
import app.unavu.ui.MealPicker
import app.unavu.ui.NumberField
import app.unavu.ui.Page
import app.unavu.ui.QuickAddRequest
import app.unavu.ui.SectionLabel
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import kotlin.math.roundToInt

private enum class Mode { FOOD, LIQUID, WEIGHT }

/** The + sheet: mode tabs → type it like you say it → parsed preview → pick the meal → add. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(request: QuickAddRequest, onDismiss: () -> Unit) {
    val c = U.c
    val data = LocalData.current
    val nav = nav
    val store = nav.store
    val date = nav.date.value
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(runCatching { Mode.valueOf(request.mode) }.getOrDefault(Mode.FOOD)) }
    var text by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf(request.meal) }
    var cheat by remember { mutableStateOf(false) }
    var waterMl by remember { mutableIntStateOf(0) }
    var waist by remember { mutableStateOf("") }
    /** Items the user customised, keyed by "position:food id" so they survive re-parsing. */
    val overrides = remember { mutableStateMapOf<String, NewItem>() }
    var comboItems by remember { mutableStateOf<List<NewItem>>(emptyList()) }
    var editing by remember { mutableStateOf<Pair<String, NewItem>?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(mode) { runCatching { focus.requestFocus() } }

    val parsed = if (mode == Mode.WEIGHT) QuickParser.Result(emptyList(), emptyList())
    else QuickParser.parse(text, data.portions, liquidsOnly = mode == Mode.LIQUID)
    val typed: List<Pair<String, NewItem>> = parsed.items.mapIndexed { i, it -> "$i:${it.food.id}".let { k -> k to (overrides[k] ?: it.toNew()) } }
    val all: List<Pair<String, NewItem>> = typed + comboItems.mapIndexed { i, it -> "combo$i" to it }
    val needsMeal = all.isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = c.bg, dragHandle = null) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(RoundedCornerShape(50)).background(c.sf2).padding(4.dp)) {
                    Mode.entries.forEach { m ->
                        val on = m == mode
                        Text(
                            m.name, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, fontSize = 12.5.sp,
                            color = if (on) Color(0xFF1C1B18) else c.ink2,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.acc else Color.Transparent)
                                .clickable { mode = m; text = ""; overrides.clear(); comboItems = emptyList() }.padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                ArrowButton(Arrow.CLOSE, onClick = onDismiss)
            }

            // big natural-language field
            Box(Modifier.padding(top = 14.dp, bottom = 6.dp)) {
                if (text.isEmpty()) {
                    Text(
                        when (mode) { Mode.FOOD -> "rendu idli oru vadai sambar"; Mode.LIQUID -> "2 kaapi 1 boost"; Mode.WEIGHT -> "71.4" },
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.ink2.copy(alpha = 0.55f),
                    )
                }
                BasicTextField(
                    value = text, onValueChange = { text = it },
                    textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.ink),
                    cursorBrush = SolidColor(c.acc),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (mode == Mode.WEIGHT) KeyboardType.Decimal else KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }

            when (mode) {
                Mode.FOOD, Mode.LIQUID -> {
                    // words we couldn't place → suggestions, never a silent guess
                    // don't nag about the word that is still being typed
                    val typing = if (text.isNotEmpty() && !text.last().isWhitespace()) text.trim().split(Regex("\\s+")).last().lowercase() else null
                    val stillTyping = typing != null && (typing.length < 4 || FoodDb.dishes.any { f -> f.aliases.any { it.startsWith(typing) } })
                    parsed.unknown.distinct().filterNot { stillTyping && it == typing }.forEach { word ->
                        val sugg = FoodDb.search(word, limit = 3, includeRaw = false).filter { mode == Mode.FOOD || it.liquid }
                        AppCard(color = c.turS, border = null, padding = PaddingValues(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            Text("Didn't find “$word”", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = c.turInk)
                            if (sugg.isNotEmpty()) {
                                Text("Did you mean:", fontSize = 11.5.sp, color = c.ink2, modifier = Modifier.padding(vertical = 4.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    sugg.forEach { f ->
                                        ChoiceChip("${f.emoji} ${f.name}", false) {
                                            val alias = f.aliases.firstOrNull { it.split(" ").size == 1 } ?: f.aliases.first()
                                            text = text.replace(Regex("(?i)\\b" + Regex.escape(word) + "\\b"), alias)
                                        }
                                    }
                                }
                            }
                            Text(
                                "Search all foods →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.accInk,
                                modifier = Modifier.padding(top = 6.dp).clickable { onDismiss(); nav.push(Page.Plate(meal, date)) },
                            )
                            Text(
                                "＋ Add “$word” as a new food", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.accInk,
                                modifier = Modifier.padding(top = 6.dp).clickable { onDismiss(); nav.push(Page.MyFood(null, word.replaceFirstChar { it.uppercase() })) },
                            )
                        }
                    }
                    if (all.isNotEmpty()) {
                        AppCard(padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                            all.forEach { (key, item) -> ParsedRow(item) { editing = key to item } }
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
                                Text("Total · tap an item to change it", color = c.ink2, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text("${all.sumOf { (_, i) -> FoodDb.get(i.foodId)?.nutrFor(i.qty, i.unit, i.choices)?.kcal ?: 0.0 }.roundToInt()} kcal", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (mode == Mode.FOOD && text.isEmpty() && comboItems.isEmpty()) {
                        val combos = data.combos.sortedWith(compareByDescending<Combo> { it.uses }.thenBy { it.order }).take(6)
                        if (combos.isNotEmpty()) {
                            SectionLabel("Your usual · one tap")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                combos.forEach { cb ->
                                    ChoiceChip("${cb.emoji} ${cb.name}", false) {
                                        comboItems = cb.items.map { it.toNew() }
                                        if (meal == null) meal = cb.meal
                                        store.update { d -> d.copy(combos = d.combos.map { if (it.id == cb.id) it.copy(uses = it.uses + 1) else it }) }
                                    }
                                }
                            }
                        }
                    }
                    if (mode == Mode.LIQUID) {
                        SectionLabel("Water · tap to add")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Triple("🥛", "Glass", 250), Triple("🍶", "Tumbler", 150), Triple("🧴", "Bottle", 500)).forEach { (e, n, ml) ->
                                LiquidTile(e, n, "$ml ml", false, Modifier.weight(1f)) { waterMl += ml }
                            }
                        }
                        if (waterMl > 0) {
                            Text("💧 +$waterMl ml water", fontWeight = FontWeight.Bold, color = c.blue, modifier = Modifier.padding(top = 8.dp).clickable { waterMl = 0 })
                        }
                        SectionLabel("Drinks · tap to add")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("☕ kaapi" to "kaapi", "🍵 tea" to "tea", "🥤 boost" to "boost", "🥤 horlicks" to "horlicks", "🥛 milk" to "milk",
                                "🥛 buttermilk" to "buttermilk", "🥥 elaneer" to "elaneer", "🧃 juice" to "juice", "🍋 lime juice" to "lime juice").forEach { (label, w) ->
                                ChoiceChip(label, false) { text = (text.trim() + " 1 $w").trim() }
                            }
                        }
                    }

                    if (needsMeal) {
                        SectionLabel("Which meal? · tap one")
                        MealPicker(meal) { meal = it }
                        Row(Modifier.padding(top = 8.dp)) { ChoiceChip("🍕 Cheat meal", cheat) { cheat = !cheat } }
                    }
                    Spacer(Modifier.height(12.dp))
                    val count = all.size + if (waterMl > 0) 1 else 0
                    val canAdd = count > 0 && (!needsMeal || meal != null)
                    AddButton(
                        text = when {
                            count == 0 -> if (mode == Mode.FOOD) "TYPE WHAT YOU ATE" else "ADD A DRINK"
                            needsMeal && meal == null -> "PICK A MEAL ABOVE"
                            mode == Mode.FOOD -> "ADD TO ${meal!!.label.uppercase()}"
                            else -> "ADD $count ${if (count == 1) "DRINK" else "DRINKS"}"
                        },
                        enabled = canAdd,
                    ) {
                        if (all.isNotEmpty()) store.logItems(all.map { it.second }, meal!!, date, cheat)
                        if (waterMl > 0) store.addWater(date, waterMl)
                        onDismiss()
                    }
                    Text(
                        "Build a plate instead →", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.accInk, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable { onDismiss(); nav.push(Page.Plate(meal, date)) },
                    )
                    Text(
                        if (mode == Mode.FOOD) "Try “3 chapathi chicken curry”, “half parotta 2 karandi salna” or “மூன்று இட்லி”"
                        else "Water goes to Liquids. Drinks with calories also count in the meal you pick.",
                        fontSize = 11.5.sp, color = c.ink2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    )
                }
                Mode.WEIGHT -> {
                    val kg = text.replace(",", ".").filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                    Text("kg · weigh-in for ${if (date == app.unavu.data.today()) "today" else date}", color = c.ink2, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    NumberField(waist, "Waist cm (optional)", Modifier.width(180.dp)) { waist = it }
                    Spacer(Modifier.height(12.dp))
                    AddButton("SAVE WEIGHT", enabled = kg != null && kg in 25.0..250.0) {
                        store.addWeight(date, kg!!, waist.toDoubleOrNull())
                        onDismiss()
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    editing?.let { (key, item) ->
        val food = FoodDb.get(item.foodId)
        if (food != null) {
            CustomiseSheet(food, item, onDismiss = { editing = null }, onRemove = {
                if (key.startsWith("combo")) comboItems = comboItems.filterIndexed { i, _ -> "combo$i" != key }
                else {
                    val src = parsed.items.getOrNull(key.substringBefore(":").toInt())?.source
                    if (src != null) text = text.replaceFirst(Regex("(?i)(\\S+\\s+)?" + Regex.escape(src)), "").trim()
                    overrides.remove(key)
                }
                editing = null
            }) { updated ->
                if (key.startsWith("combo")) comboItems = comboItems.mapIndexed { i, it -> if ("combo$i" == key) updated else it }
                else overrides[key] = updated
                editing = null
            }
        }
    }
}

@Composable
private fun ParsedRow(item: NewItem, onClick: () -> Unit) {
    val food = FoodDb.get(item.foodId) ?: return
    val data = LocalData.current
    val remembered = data.portions[item.foodId]?.let { it.qty == item.qty && it.unit == item.unit } == true
    val summary = food.choiceSummary(item.choices)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text("${food.emoji} ${food.name} × ${fmtQty(item.qty)} ${unitLabel(item.unit, item.qty)}", fontSize = 13.5.sp)
            if (summary.isNotBlank()) Text(summary, fontSize = 11.sp, color = U.c.leafInk, fontWeight = FontWeight.SemiBold)
            if (remembered || food.liquid) {
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (remembered) StatusBadge("your usual", Status.OK)
                    if (food.liquid) StatusBadge("→ Liquids", Status.INFO)
                }
            }
        }
        Text("${food.nutrFor(item.qty, item.unit, item.choices).kcal.roundToInt()}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
    }
}

@Composable
fun AddButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = U.c
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (enabled) c.acc else c.acc.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color(0xFF1C1B18), fontSize = 15.sp) }
}

@Composable
fun LiquidTile(emoji: String, name: String, sub: String, on: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = U.c
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (on) c.blueS else c.sf)
            .border(1.5.dp, if (on) c.blue else c.line, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(top = 12.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmojiText(emoji, 24)
        androidx.compose.foundation.layout.Spacer(Modifier.height(4.dp))
        Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(sub, fontSize = 10.5.sp, color = c.ink2)
    }
}

@Suppress("unused")
private fun keepMeal(m: Meal) = m
