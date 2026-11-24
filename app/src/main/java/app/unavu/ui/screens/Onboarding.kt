package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import app.unavu.ui.pressScale
import app.unavu.ui.theme.LocalReducedMotion
import app.unavu.ui.theme.Motion
import app.unavu.ui.theme.motionTween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.unavu.R
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
import app.unavu.ui.theme.Space
import app.unavu.ui.theme.U
import app.unavu.data.fmtInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

private val ACTIVITIES = listOf(1.2 to R.string.i_act_desk, 1.375 to R.string.i_act_light, 1.55 to R.string.i_act_moderate, 1.725 to R.string.i_act_very)
private val GOALS = listOf(-500 to R.string.i_goal_lose, 0 to R.string.i_goal_maintain, 300 to R.string.i_goal_gain)

private fun ageOk(s: String) = s.toIntOrNull()?.let { it in 10..100 } == true
private fun heightOk(s: String) = s.toDoubleOrNull()?.let { it in 100.0..250.0 } == true
private fun weightOk(s: String) = s.toDoubleOrNull()?.let { it in 20.0..300.0 } == true

/** Setup step 1 (also used from Settings to edit your details). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutYouScreen(
    initial: Profile,
    title: String = stringResource(R.string.i_about_you),
    onBack: (() -> Unit)? = null,
    buttonText: String = stringResource(R.string.i_calc_plan),
    /** The "Step 1 of 2" bar belongs to first-run setup only; editing your details from Settings hides it. */
    showSteps: Boolean = onBack == null,
    onDone: (Profile) -> Unit,
) {
    // First run starts with empty body fields (the defaults are only hints), so nobody gets someone else's plan by
    // tapping straight through. Editing, or coming back from step 2, shows the real values.
    val prefill = onBack != null || initial != Profile()
    var male by rememberSaveable { mutableStateOf(initial.male) }
    var age by rememberSaveable { mutableStateOf(if (prefill) initial.age.toString() else "") }
    var height by rememberSaveable { mutableStateOf(if (prefill) fmt(initial.heightCm) else "") }
    var weight by rememberSaveable { mutableStateOf(if (prefill) fmt(initial.weightKg) else "") }
    var activity by rememberSaveable { mutableStateOf(initial.activity) }
    var goal by rememberSaveable { mutableStateOf(initial.goalKcal) }

    // Blank fields aren't errors yet: they just keep the button disabled.
    val ageValid = ageOk(age); val heightValid = heightOk(height); val weightValid = weightOk(weight)
    val ageErr = age.isNotBlank() && !ageValid
    val heightErr = height.isNotBlank() && !heightValid
    val weightErr = weight.isNotBlank() && !weightValid
    val anyBlank = age.isBlank() || height.isBlank() || weight.isBlank()
    val valid = ageValid && heightValid && weightValid
    val ageFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (!prefill && age.isEmpty()) runCatching { ageFocus.requestFocus() } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(title, onBack)
        Column(Modifier.padding(horizontal = Space.gutter)) {
            if (showSteps) {
                StepBar(1, Modifier.padding(bottom = 8.dp))
                Text(stringResource(R.string.i_step_1_of_2), style = U.t.label, color = U.c.ink2, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
            TamilText(stringResource(R.string.i_about_you_tamil), Modifier.padding(start = 2.dp))
            SectionLabel(stringResource(R.string.i_sex))
            Segmented(listOf(stringResource(R.string.i_male), stringResource(R.string.i_female)), if (male) 0 else 1) { male = it == 0 }
            SectionLabel(stringResource(R.string.i_body))
            val d = Profile()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberBox(age, stringResource(R.string.i_age_label), if (ageErr) stringResource(R.string.i_err_age) else null, ImeAction.Next, Modifier.weight(1f), stringResource(R.string.r7d_eg, d.age.toString()), Modifier.focusRequester(ageFocus)) { age = it }
                NumberBox(height, stringResource(R.string.i_height_label), if (heightErr) stringResource(R.string.i_err_height) else null, ImeAction.Next, Modifier.weight(1f), stringResource(R.string.r7d_eg, fmt(d.heightCm))) { height = it }
                NumberBox(weight, stringResource(R.string.i_weight_label), if (weightErr) stringResource(R.string.i_err_weight) else null, ImeAction.Done, Modifier.weight(1f), stringResource(R.string.r7d_eg, fmt(d.weightKg))) { weight = it }
            }
            // spoken + visible reasons, one line per problem
            if (ageErr) Text(stringResource(R.string.i_err_age), style = U.t.caption, color = U.c.chiInk, modifier = Modifier.padding(top = 6.dp))
            if (heightErr) Text(stringResource(R.string.i_err_height), style = U.t.caption, color = U.c.chiInk, modifier = Modifier.padding(top = 2.dp))
            if (weightErr) Text(stringResource(R.string.i_err_weight), style = U.t.caption, color = U.c.chiInk, modifier = Modifier.padding(top = 2.dp))
            SectionLabel(stringResource(R.string.i_activity))
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ACTIVITIES.forEach { (v, label) -> Pill(stringResource(label), activity == v) { activity = v } }
            }
            SectionLabel(stringResource(R.string.i_goal))
            Segmented(GOALS.map { stringResource(it.second) }, GOALS.indexOfFirst { it.first == goal }.coerceAtLeast(0)) { goal = GOALS[it].first }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(buttonText, enabled = valid) {
                onDone(Profile(male, age.toInt(), height.toDouble(), weight.toDouble(), activity, goal))
            }
            if (!valid) Text(stringResource(if (anyBlank && !ageErr && !heightErr && !weightErr) R.string.r7d_enter_body else R.string.i_fix_fields), style = U.t.caption, color = U.c.ink2, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun fmt(d: Double) = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable
private fun NumberBox(value: String, label: String, error: String?, ime: ImeAction, modifier: Modifier, placeholder: String? = null, fieldModifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val c = U.c
    val shape = RoundedCornerShape(14.dp)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor by animateColorAsState(if (error != null) c.chiInk else if (focused) c.acc else c.line, motionTween(Motion.SHORT), label = "numBorder")
    Column(
        modifier.pressScale().heightIn(min = 64.dp).clip(shape).background(c.sf).border(if (error != null || focused) 2.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' }.take(5)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ime),
            interactionSource = interaction,
            textStyle = U.t.headline.copy(fontWeight = FontWeight.ExtraBold, color = c.ink),
            cursorBrush = SolidColor(c.acc),
            modifier = fieldModifier.fillMaxWidth().semantics {
                contentDescription = label
                if (error != null) error(error)
            },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    // the hint is only a placeholder: it never counts as an entered value
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = U.t.title.copy(fontWeight = FontWeight.Medium), color = c.hint, maxLines = 1, softWrap = false)
                    }
                    inner()
                }
            },
        )
        Text(label, style = U.t.caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink2)
    }
}

/** Setup step 2: the calculated targets. */
@Composable
fun TargetsScreen(
    profile: Profile,
    onBack: (() -> Unit)?,
    buttonText: String = stringResource(R.string.i_start_tracking),
    /** Hide "Step 2 of 2" when this is shown from Settings (editing your details), not first-run setup. */
    showSteps: Boolean = true,
    onDone: () -> Unit,
) {
    val t: Targets = targetsFor(profile)
    val c = U.c
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_targets_title), onBack)
        Column(Modifier.padding(horizontal = Space.gutter), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (showSteps) {
                StepBar(2)
                Text(stringResource(R.string.i_step_2_of_2), style = U.t.label, color = c.ink2, modifier = Modifier.padding(start = 2.dp))
            }
            TamilText(stringResource(R.string.i_targets_source_short), Modifier.padding(start = 2.dp))
            AppCard(color = c.leafS, border = null) {
                Text(stringResource(R.string.i_eat_about), color = c.leafInk, style = U.t.label.copy(fontWeight = FontWeight.SemiBold))
                val kcalShown by countUp(t.kcal.toFloat())
                Row {
                    Text(fmtInt(kcalShown.toInt()), color = c.ink, style = U.t.display.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.alignByBaseline())
                    Text("  " + stringResource(R.string.i_kcal_day), color = c.ink, style = U.t.body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.alignByBaseline())
                }
                val goalText = when {
                    profile.goalKcal < 0 -> stringResource(R.string.i_goal_lose_text, -profile.goalKcal)
                    profile.goalKcal > 0 -> stringResource(R.string.i_goal_gain_text, profile.goalKcal)
                    else -> stringResource(R.string.i_goal_maintain_text)
                }
                Text(stringResource(R.string.i_bmr_line, fmtInt(t.bmr), fmtInt(t.maintenance), goalText), color = c.leafInk, style = U.t.caption)
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                SectionLabel(stringResource(R.string.i_macros))
                KV(stringResource(R.string.i_protein), stringResource(R.string.i_g, t.protein)); Divider()
                KV(stringResource(R.string.i_carbs), stringResource(R.string.i_g, t.carbs)); Divider()
                KV(stringResource(R.string.i_fat), stringResource(R.string.i_g, t.fat)); Divider()
                KV(stringResource(R.string.i_fibre), stringResource(R.string.i_g, t.fibre))
            }
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                SectionLabel(stringResource(R.string.i_key_nutrients))
                KV(stringResource(R.string.i_iron), stringResource(R.string.i_mg, t.iron.toInt())); Divider()
                KV(stringResource(R.string.i_calcium), stringResource(R.string.i_mg, fmtInt(t.calcium))); Divider()
                KV(stringResource(R.string.i_sodium_limit), stringResource(R.string.i_mg_limit, fmtInt(t.sodium))); Divider()
                KV(stringResource(R.string.i_sugar_limit), stringResource(R.string.i_g_limit, t.sugar.toInt()))
            }
            PrimaryButton(buttonText, onClick = onDone)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun KV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, style = U.t.body, color = U.c.ink)
        Text(v, style = U.t.body.copy(fontWeight = FontWeight.Bold), color = U.c.ink)
    }
}

/** Counts 0 -> target once when first composed (instant under reduced motion). */
@Composable
fun countUp(target: Float, durationMs: Int = 700): State<Float> {
    val reduced = LocalReducedMotion.current
    val a = remember { Animatable(if (reduced) target else 0f) }
    val spec = motionTween<Float>(durationMs)
    LaunchedEffect(target, reduced) { a.animateTo(target, spec) }
    return a.asState()
}

/** Two-segment setup progress; segments fill with a tween (the active one animates in when it appears). */
@Composable
private fun StepBar(step: Int, modifier: Modifier = Modifier) {
    val c = U.c
    Row(modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..2).forEach { i ->
            val fill = remember { Animatable(if (i < step) 1f else 0f) }
            val spec = motionTween<Float>(Motion.LONG)
            LaunchedEffect(step) { fill.animateTo(if (step >= i) 1f else 0f, spec) }
            Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c.line)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fill.value).background(c.acc, CircleShape))
            }
        }
    }
}
