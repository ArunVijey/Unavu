package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.unavu.data.Profile
import app.unavu.data.Targets
import app.unavu.data.targetsFor
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.Pill
import app.unavu.ui.PrimaryButton
import app.unavu.ui.SectionLabel
import app.unavu.ui.Segmented
import app.unavu.ui.TamilText
import app.unavu.ui.TopBar
import app.unavu.ui.theme.Tnum
import app.unavu.ui.theme.U
import androidx.compose.material3.LocalTextStyle

private val ACTIVITIES = listOf(1.2 to "Desk job", 1.375 to "Light walk", 1.55 to "Moderate", 1.725 to "Very active")
private val GOALS = listOf(-500 to "Lose", 0 to "Maintain", 300 to "Gain")

/** Setup step 1 (also used from Settings to edit your details). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutYouScreen(initial: Profile, title: String = "About you", onBack: (() -> Unit)? = null, buttonText: String = "Calculate my plan →", onDone: (Profile) -> Unit) {
    var male by remember { mutableStateOf(initial.male) }
    var age by remember { mutableStateOf(initial.age.toString()) }
    var height by remember { mutableStateOf(fmt(initial.heightCm)) }
    var weight by remember { mutableStateOf(fmt(initial.weightKg)) }
    var activity by remember { mutableStateOf(initial.activity) }
    var goal by remember { mutableStateOf(initial.goalKcal) }

    val valid = age.toIntOrNull()?.let { it in 10..100 } == true &&
        height.toDoubleOrNull()?.let { it in 100.0..230.0 } == true &&
        weight.toDoubleOrNull()?.let { it in 25.0..250.0 } == true

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(title, onBack)
        Column(Modifier.padding(horizontal = 16.dp)) {
            TamilText("உங்களைப் பற்றி · used to calculate your daily needs", Modifier.padding(start = 2.dp))
            SectionLabel("Sex")
            Segmented(listOf("Male", "Female"), if (male) 0 else 1) { male = it == 0 }
            SectionLabel("Body")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberBox(age, "Age · yrs", Modifier.weight(1f)) { age = it }
                NumberBox(height, "Height · cm", Modifier.weight(1f)) { height = it }
                NumberBox(weight, "Weight · kg", Modifier.weight(1f)) { weight = it }
            }
            SectionLabel("Activity")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ACTIVITIES.forEach { (v, label) -> Pill(label, activity == v) { activity = v } }
            }
            SectionLabel("Goal")
            Segmented(GOALS.map { it.second }, GOALS.indexOfFirst { it.first == goal }.coerceAtLeast(0)) { goal = GOALS[it].first }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(buttonText, enabled = valid) {
                onDone(Profile(male, age.toInt(), height.toDouble(), weight.toDouble(), activity, goal))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
private fun NumberBox(value: String, label: String, modifier: Modifier, onChange: (String) -> Unit) {
    val c = U.c
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.sf).border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' }.take(5)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink),
            cursorBrush = SolidColor(c.leaf),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(label, fontSize = 11.sp, color = c.ink2, fontWeight = FontWeight.SemiBold)
    }
}

/** Setup step 2: the calculated targets. */
@Composable
fun TargetsScreen(profile: Profile, onBack: (() -> Unit)?, buttonText: String = "Looks good, start tracking", onDone: () -> Unit) {
    val t: Targets = targetsFor(profile)
    val c = U.c
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar("Your daily targets", onBack)
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TamilText("Mifflin-St Jeor + ICMR-NIN 2020 RDA", Modifier.padding(start = 2.dp))
            AppCard(color = c.leaf, border = null) {
                Text("Eat about", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row {
                    Text("${t.kcal}", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, style = Tnum)
                    Text("  kcal / day", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
                }
                val goalText = when {
                    profile.goalKcal < 0 -> "−${-profile.goalKcal} to lose ~0.5 kg/week"
                    profile.goalKcal > 0 -> "+${profile.goalKcal} for lean gain"
                    else -> "maintain weight"
                }
                Text("BMR ${t.bmr} · Maintenance ${t.maintenance} · $goalText", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
            AppCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                SectionLabel("Macros")
                KV("🍗 Protein", "${t.protein} g"); Divider()
                KV("🍚 Carbs", "${t.carbs} g"); Divider()
                KV("🥥 Fat", "${t.fat} g"); Divider()
                KV("🥬 Fibre", "${t.fibre} g")
            }
            AppCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                SectionLabel("Key nutrients")
                KV("Iron", "${t.iron.toInt()} mg"); Divider()
                KV("Calcium", "${t.calcium.toInt()} mg"); Divider()
                KV("Sodium (limit)", "≤ ${t.sodium.toInt()} mg"); Divider()
                KV("Added sugar (limit)", "≤ ${t.sugar.toInt()} g")
            }
            PrimaryButton(buttonText, onClick = onDone)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k)
        Text(v, fontWeight = FontWeight.Bold, style = Tnum)
    }
}
