package app.unavu.ui.screens

import app.unavu.ui.EmojiText
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import app.unavu.data.AppFont
import app.unavu.data.Backup
import app.unavu.data.FoodDb
import app.unavu.data.Meal
import app.unavu.data.NUTRIENTS
import app.unavu.data.NewItem
import app.unavu.data.Portion
import app.unavu.data.ThemeMode
import app.unavu.data.fmtQty
import app.unavu.data.targets
import app.unavu.data.unitLabel
import app.unavu.ui.AppCard
import app.unavu.ui.AppSwitch
import app.unavu.ui.ConfirmDialog
import app.unavu.ui.Divider
import app.unavu.ui.ListRow
import app.unavu.ui.LocalData
import app.unavu.ui.Page
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SecondaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Status
import app.unavu.ui.StatusBadge
import app.unavu.ui.Stepper
import app.unavu.ui.Tab
import app.unavu.ui.TimeDialog
import app.unavu.ui.TopBar
import app.unavu.ui.fmtTime
import app.unavu.ui.nav
import app.unavu.ui.theme.GoogleSansFlex
import app.unavu.ui.theme.InterFamily
import app.unavu.ui.theme.U
import app.unavu.work.Scheduler
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private val WHITE = Color.White

@Composable
fun SettingsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val p = data.profile ?: return
    val t = data.targets() ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Settings")
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard(onClick = { nav.push(Page.Profile) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(c.turS), contentAlignment = Alignment.Center) { EmojiText("🙂", 22) }
                    Column(Modifier.weight(1f)) {
                        Text("You", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${if (p.male) "Male" else "Female"} · ${p.age} · ${p.heightCm.roundToInt()} cm · ${"%.1f".format(p.weightKg)} kg", fontSize = 11.5.sp, color = c.ink2)
                    }
                    Text("Edit", color = c.leafInk, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                ListRow("Goal & targets", "${t.kcal} kcal · meal split · water · home chips", "🎯") { nav.push(Page.Goals) }
                Divider()
                ListRow("My portions", "${data.portions.size} foods remembered", "🥄") { nav.push(Page.Portions) }
                Divider()
                ListRow("My foods", data.customFoods.count { it.facts != null }.let { n -> if (n == 0) "Add anything new you eat" else "$n added · add something new" }, "➕") { nav.push(Page.MyFoods) }
                Divider()
                ListRow(
                    "Smart target",
                    when (data.smart.mode) { app.unavu.data.SmartMode.AUTO -> "Automatic"; app.unavu.data.SmartMode.ASK -> "Asks before changing"; app.unavu.data.SmartMode.MANUAL -> "Set by you" } +
                        (if (data.smart.pendingBurn != null) " · new estimate waiting" else ""),
                    "🧠",
                ) { nav.push(Page.SmartTarget) }
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                val theme = when (data.settings.theme) { ThemeMode.SYSTEM -> "System"; ThemeMode.LIGHT -> "Light"; ThemeMode.DARK -> "Dark" }
                ListRow("Appearance", "$theme${if (data.settings.amoled) " · AMOLED" else ""} · ${data.settings.font.label}", "🎨") { nav.push(Page.Appearance) }
                Divider()
                val b = data.backup
                val sub = when {
                    b.folderUri == null -> "Not set up yet"
                    b.lastAt == null -> "Folder set · not run yet"
                    else -> "Last: " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(b.lastAt)) + if (!b.lastOk) " · failed" else ""
                }
                ListRow("Backup", sub, "📁") { nav.push(Page.Backup) }
                Divider()
                ListRow("Reminders", "${data.reminders.count { it.on }} on", "🔔") { nav.push(Page.Reminders) }
                Divider()
                ListRow("Saved combos & my recipes", "In the Meals tab", "🍲") { nav.tab.value = Tab.MEALS }
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                ListRow("About & food data", "IFCT 2017 · ${FoodDb.dishes.size} dishes", "ℹ️") { nav.push(Page.About) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProfileScreen() {
    val nav = nav
    val data = LocalData.current
    var step by remember { mutableStateOf(0) }
    var draft by remember { mutableStateOf(data.profile!!) }
    if (step == 0) {
        AboutYouScreen(draft, "Your details", onBack = { nav.pop() }, buttonText = "See new targets →") { draft = it; step = 1 }
    } else {
        androidx.activity.compose.BackHandler { step = 0 }
        TargetsScreen(draft, onBack = { step = 0 }, buttonText = "Save") {
            nav.store.update { it.copy(profile = draft.copy(goalWeightKg = it.profile?.goalWeightKg)) }
            nav.pop()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val t = data.targets() ?: return
    var split by remember { mutableStateOf(data.settings.mealSplit.map { (it * 100).roundToInt() }) }
    Column(Modifier.fillMaxSize()) {
        TopBar("Goal & targets", onBack = { nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard(color = c.leaf, border = null) {
                Text("Daily target", color = WHITE.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${t.kcal} kcal", color = WHITE, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                Text("P ${t.protein} g · C ${t.carbs} g · F ${t.fat} g · Fibre ${t.fibre} g", color = WHITE, fontSize = 12.sp)
                Text("Change your weight, activity or goal in your details.", color = WHITE.copy(alpha = 0.85f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
            SecondaryButton("Edit my details & goal") { nav.push(Page.Profile) }

            SectionLabel("Meal split · % of the day's calories")
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                Meal.entries.forEachIndexed { i, m ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${m.emoji} ${m.label}", modifier = Modifier.weight(1f))
                        Text("${(split[i] * t.kcal / 100.0).roundToInt()} kcal  ", fontSize = 11.sp, color = c.ink2)
                        Stepper(split[i].toDouble(), 5.0, min = 0.0) { v -> split = split.toMutableList().also { it[i] = v.roundToInt().coerceIn(0, 80) } }
                    }
                }
                val sum = split.sum()
                Text(if (sum == 100) "Total 100% ✓" else "Total $sum%: must add up to 100%", color = if (sum == 100) c.leafInk else c.chi, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp))
                PrimaryButton("Save split", enabled = sum == 100 && split != data.settings.mealSplit.map { (it * 100).roundToInt() }) {
                    store.update { d -> d.copy(settings = d.settings.copy(mealSplit = split.map { it / 100.0 })) }
                }
                Spacer(Modifier.height(6.dp))
            }

            SectionLabel("Water goal")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily water", modifier = Modifier.weight(1f))
                    Stepper(data.settings.waterGoalMl / 1000.0, 0.25, min = 0.5) { v -> store.update { d -> d.copy(settings = d.settings.copy(waterGoalMl = (v * 1000).roundToInt())) } }
                    Text(" L", fontWeight = FontWeight.Bold)
                }
            }

            SectionLabel("Nutrient chips on Today · up to 6")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NUTRIENTS.forEach { n ->
                    val chips = data.settings.homeChips
                    val on = n.key in chips
                    ChoiceChip(n.name, on) {
                        val next = if (on) chips - n.key else if (chips.size < 6) chips + n.key else chips
                        if (next.isNotEmpty()) store.update { d -> d.copy(settings = d.settings.copy(homeChips = NUTRIENTS.map { it.key }.filter { it in next })) }
                    }
                }
            }
            Text("Targets: ICMR-NIN 2020 RDA for Indians. Limits: sodium 2,000 mg, added sugar 25 g, saturated fat 10% of calories.", fontSize = 11.sp, color = c.ink2)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AppearanceScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val s = data.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Appearance", onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            SectionLabel("Style")
            app.unavu.ui.Segmented(listOf("Clean", "Sketch ✎"), if (s.sketch) 1 else 0) { i ->
                store.update { d -> d.copy(settings = d.settings.copy(sketch = i == 1)) }
            }
            Text(
                if (s.sketch) "Hand-drawn look: Excalifont, sketchy outlines and dotted paper. Works with light, dark and pure black."
                else "Sharp, minimal look.",
                fontSize = 12.sp, color = c.ink2, modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
            SectionLabel("Theme")
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                listOf(ThemeMode.SYSTEM to ("System default" to "Follows your phone"), ThemeMode.LIGHT to ("Light" to null), ThemeMode.DARK to ("Dark" to null))
                    .forEachIndexed { i, (mode, label) ->
                        if (i > 0) Divider()
                        ListRow(label.first, label.second, trailing = null, end = { Radio(s.theme == mode) }) {
                            store.update { d -> d.copy(settings = d.settings.copy(theme = mode)) }
                        }
                    }
            }
            Spacer(Modifier.height(9.dp))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                ListRow("Pure black (AMOLED)", "True black whenever dark is on. Saves battery.", trailing = null, end = {
                    AppSwitch(s.amoled) { v -> store.update { d -> d.copy(settings = d.settings.copy(amoled = v)) } }
                }) { store.update { d -> d.copy(settings = d.settings.copy(amoled = !d.settings.amoled)) } }
            }
            SectionLabel("Accent colour")
            AccentSection()
            SectionLabel("Font")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppFont.GOOGLE_SANS_FLEX to GoogleSansFlex, AppFont.INTER to InterFamily, AppFont.EXCALIFONT to app.unavu.ui.theme.ExcalifontFamily).forEach { (font, family) ->
                    val on = if (s.sketch) font == AppFont.EXCALIFONT else s.font == font
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(c.sf)
                            .border(if (on) 2.dp else 1.dp, if (on) c.acc else c.line, RoundedCornerShape(18.dp))
                            .clickable { store.update { d -> d.copy(settings = d.settings.copy(font = font)) } }
                            .padding(12.dp),
                    ) {
                        Text("Aa 638", fontFamily = family, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(font.label, fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(when (font) { AppFont.GOOGLE_SANS_FLEX -> "Default"; AppFont.INTER -> "Compact"; AppFont.EXCALIFONT -> "Hand-drawn" }, fontSize = 10.5.sp, color = c.ink2)
                    }
                }
            }
            if (s.sketch) Text("Sketch style always uses Excalifont.", fontSize = 11.sp, color = c.ink2, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            Text("Tamil text always uses Noto Sans Tamil · தமிழ்", fontSize = 11.sp, color = c.ink2, modifier = Modifier.padding(6.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Radio(on: Boolean) {
    val c = U.c
    Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) c.acc else c.ink2, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(c.acc))
    }
}

@Composable
fun PortionsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    var editing by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("My portions", onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard(color = c.leafS, border = null) {
                Text("🥄 The last portion and choices you used for each food are remembered and pre-filled next time. Change them while logging, or tap one here.", color = c.leafInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            val list = data.portions.entries.mapNotNull { (id, p) -> FoodDb.get(id)?.let { it to p } }.sortedBy { it.first.name }
            if (list.isEmpty()) {
                Text("Nothing remembered yet. Log a food and it will appear here.", color = c.ink2, modifier = Modifier.padding(6.dp))
            } else {
                AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    list.forEachIndexed { i, (f, p) ->
                        if (i > 0) Divider()
                        val grams = f.unit(p.unit).grams
                        val extra = f.choiceSummary(p.choices).takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                        ListRow(f.name, "${fmtQty(p.qty)} ${unitLabel(p.unit, p.qty)} · 1 ${p.unit} = ${grams.roundToInt()} ${if (f.liquid) "ml" else "g"}$extra", f.emoji, trailing = "✏️") { editing = f.id }
                    }
                }
            }
        }
    }
    editing?.let { id ->
        val f = FoodDb.get(id)
        val p = data.portions[id]
        if (f != null && p != null) {
            CustomiseSheet(f, NewItem(id, p.qty, p.unit, p.choices), saveLabel = "Remember", onDismiss = { editing = null }, onRemove = {
                nav.store.update { d -> d.copy(portions = d.portions - id) }; editing = null
            }) { ni ->
                nav.store.update { d -> d.copy(portions = d.portions + (id to Portion(ni.qty, ni.unit, ni.choices))) }; editing = null
            }
        } else editing = null
    }
}

@Composable
fun BackupScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val context = LocalContext.current
    val b = data.backup
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var timeDialog by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val name = DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment
            store.update { d -> d.copy(backup = d.backup.copy(folderUri = uri.toString(), folderName = name)) }
            Scheduler.scheduleBackup(context)
            busy = true
            msg = Backup.run(context).fold({ "Backed up: $it" }, { it.message }); busy = false
        }
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) restoreUri = uri }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Backup", onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val ok = b.lastOk && b.lastAt != null
            AppCard(color = if (b.lastAt == null) c.sf2 else if (ok) c.leafS else c.chiS, border = null) {
                Text(
                    when {
                        b.folderUri == null -> "📁 Pick a folder to start backing up"
                        b.lastAt == null -> "Not backed up yet"
                        ok -> "✅ Last backup: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(b.lastAt))
                        else -> "⚠️ Last backup failed"
                    },
                    fontWeight = FontWeight.Bold, color = if (b.lastAt == null) c.ink else if (ok) c.leafInk else c.chi,
                )
                b.lastMessage?.let { Text(it, fontSize = 12.sp, color = c.ink2) }
            }
            PrimaryButton(if (busy) "Backing up…" else "⇪ Back up now", enabled = b.folderUri != null && !busy) {
                busy = true
                msg = Backup.run(context).fold({ "Backed up: $it" }, { it.message }); busy = false
            }
            msg?.let { Text(it, fontSize = 12.sp, color = c.ink2) }

            SectionLabel("Folder on this phone")
            AppCard(onClick = { pickFolder.launch(null) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Emoji("📁")
                    Column(Modifier.weight(1f)) {
                        Text(b.folderName ?: "No folder chosen", fontWeight = FontWeight.Bold)
                        Text("The same files are overwritten each time", fontSize = 11.sp, color = c.ink2)
                    }
                    Text(if (b.folderUri == null) "Choose" else "Change", color = c.leafInk, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                listOf(
                    "🗂" to (Backup.JSON_NAME to "Everything: logs, recipes, combos, portions, settings"),
                    "🛟" to (Backup.PREVIOUS_NAME to "Safety copy of the previous backup"),
                    "📄" to (Backup.LOG_CSV to "Every food you logged (opens in Excel/Sheets)"),
                    "📊" to (Backup.SUMMARY_CSV to "One row per day: totals, water, weight, cheat"),
                ).forEachIndexed { i, (icon, f) ->
                    if (i > 0) Divider()
                    ListRow(f.first, f.second, icon, trailing = null)
                }
            }
            SectionLabel("Daily automatic backup")
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                ListRow("Back up every day", "Runs even if the app is closed; catches up next time you open the app", trailing = null, end = {
                    AppSwitch(b.daily) { v -> store.update { d -> d.copy(backup = d.backup.copy(daily = v)) }; Scheduler.scheduleBackup(context) }
                })
                Divider()
                ListRow("Time", null, trailing = fmtTime(b.hour, b.minute)) { timeDialog = true }
            }
            SectionLabel("Restore")
            SecondaryButton("⟲ Restore from a backup file") { pickRestore.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }
            Text("Restore replaces everything in the app with the file's contents.", fontSize = 11.sp, color = c.ink2)
            Spacer(Modifier.height(24.dp))
        }
    }
    if (timeDialog) {
        TimeDialog(b.hour, b.minute, onDismiss = { timeDialog = false }) { h, m ->
            store.update { d -> d.copy(backup = d.backup.copy(hour = h, minute = m)) }
            Scheduler.scheduleBackup(context)
        }
    }
    restoreUri?.let { uri ->
        ConfirmDialog("Restore this backup?", "Everything currently in the app will be replaced. Tip: tap “Back up now” first if you want to keep today's data.", "Restore", true, { restoreUri = null }) {
            msg = Backup.restore(context, uri).fold({ "Restored $it food entries" }, { "Restore failed: ${it.message}" })
        }
    }
}

@Composable
fun RemindersScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val context = LocalContext.current
    var editing by remember { mutableStateOf<String?>(null) }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun ensurePermission() {
        if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Reminders", onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard(color = c.leafS, border = null) {
                Text("Meal reminders skip themselves if that meal is already logged. Water stops once you reach your goal; weigh-in skips if you already weighed in.", fontSize = 12.sp, color = c.leafInk, fontWeight = FontWeight.SemiBold)
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                data.reminders.forEachIndexed { i, r ->
                    if (i > 0) Divider()
                    val icon = when (r.id) { "water" -> "💧"; "weigh" -> "⚖️"; else -> Meal.entries.firstOrNull { it.name.equals(r.id, true) }?.emoji ?: "🔔" }
                    ListRow(r.label, (if (r.id == "water") "From " else "") + fmtTime(r.hour, r.minute) + " · tap to change", icon, trailing = null, end = {
                        AppSwitch(r.on) { v ->
                            if (v) ensurePermission()
                            store.update { d -> d.copy(reminders = d.reminders.map { if (it.id == r.id) it.copy(on = v) else it }) }
                            Scheduler.scheduleReminders(context)
                        }
                    }) { editing = r.id }
                }
            }
            SecondaryButton("Allow notifications") { ensurePermission() }
            Spacer(Modifier.height(24.dp))
        }
    }
    editing?.let { id ->
        val r = data.reminders.first { it.id == id }
        TimeDialog(r.hour, r.minute, onDismiss = { editing = null }) { h, m ->
            store.update { d -> d.copy(reminders = d.reminders.map { if (it.id == id) it.copy(hour = h, minute = m) else it }) }
            Scheduler.scheduleReminders(context)
        }
    }
}

@Composable
fun AboutScreen() {
    val c = U.c
    val nav = nav
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("About & food data", onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                Text("Unavu 0.3", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Personal calorie & nutrient tracker for Tamil Nadu food. Everything stays on this phone.", fontSize = 12.5.sp, color = c.ink2)
            }
            AppCard {
                Text("Where the numbers come from", fontWeight = FontWeight.Bold)
                Text(
                    "• Ingredients: Indian Food Composition Tables 2017 (ICMR-NIN), ${FoodDb.allIngredients().size} items.\n" +
                        "• Dishes (${FoodDb.dishes.size}): calculated from typical home recipes: raw ingredient weights ÷ cooked weight. Your choices (vegetables, dal, sugar, milk, oil level) change the numbers.\n" +
                        "• A few everyday items not in IFCT (salt, sugar, toned milk, curd, bread, Boost/Horlicks, sauces) use USDA or label values.\n" +
                        "• Fast-food items are home-recipe estimates, not official chain values.\n" +
                        "• Targets: Mifflin-St Jeor for calories; ICMR-NIN 2020 RDA for nutrients.",
                    fontSize = 12.sp, color = c.ink2, modifier = Modifier.padding(top = 4.dp),
                )
            }
            StatusBadge("IFCT 2017 data via @ifct2017/compositions (MIT)", Status.INFO)
            Spacer(Modifier.height(24.dp))
        }
    }
}
