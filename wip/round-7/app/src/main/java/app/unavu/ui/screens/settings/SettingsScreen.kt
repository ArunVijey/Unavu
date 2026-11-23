package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import app.unavu.data.fmtInt
import app.unavu.ui.theme.Space
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.FoodDb
import app.unavu.data.SmartMode
import app.unavu.data.ThemeMode
import app.unavu.data.targets
import app.unavu.ui.AppCard
import app.unavu.ui.Divider
import app.unavu.ui.EmojiText
import app.unavu.ui.SectionLabel
import app.unavu.ui.LocalData
import app.unavu.ui.Page
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.theme.U
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val p = data.profile ?: return
    val t = data.targets() ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_settings_title))
        Column(Modifier.padding(horizontal = Space.gutter)) {
            AppCard(onClick = { nav.push(Page.Profile) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(c.turS).clearAndSetSemantics { }, contentAlignment = Alignment.Center) { EmojiText("🙂", 22) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.i_you), style = U.t.titleS.iBold(), color = c.ink)
                        Text(
                            stringResource(
                                R.string.i_profile_line,
                                stringResource(if (p.male) R.string.i_male else R.string.i_female),
                                p.age, p.heightCm.roundToInt(), "%.1f".format(p.weightKg),
                            ),
                            style = U.t.bodyS, color = c.ink2,
                        )
                    }
                    Text(stringResource(R.string.i_edit), color = c.accInk, style = U.t.label.iBold())
                }
            }
            SectionLabel(stringResource(R.string.i_sec_plan))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                SettingsRow(stringResource(R.string.i_goal_targets), stringResource(R.string.i_goal_targets_sub, fmtInt(t.kcal)), "🎯") { nav.push(Page.Goals) }
                Divider()
                SettingsRow(stringResource(R.string.i_my_portions), pluralStringResource(R.plurals.i_portions_remembered, data.portions.size, data.portions.size), "🥄") { nav.push(Page.Portions) }
                Divider()
                val foodsN = data.customFoods.count { it.facts != null }
                val recipesN = data.customFoods.size - foodsN
                // Same list as Meals › My foods: foods by calories and recipes from ingredients.
                SettingsRow(
                    stringResource(R.string.i_my_foods),
                    if (foodsN + recipesN == 0) stringResource(R.string.i_my_foods_none)
                    else stringResource(
                        R.string.r7d_my_foods_sub,
                        pluralStringResource(R.plurals.r7d_n_foods, foodsN, foodsN),
                        pluralStringResource(R.plurals.r7d_n_recipes, recipesN, recipesN),
                    ),
                    "➕",
                ) { nav.push(Page.MyFoods) }
                Divider()
                val smart = listOfNotNull(
                    stringResource(
                        when (data.smart.mode) { SmartMode.AUTO -> R.string.i_smart_auto; SmartMode.ASK -> R.string.i_smart_ask; SmartMode.MANUAL -> R.string.i_smart_manual },
                    ),
                    if (data.smart.pendingBurn != null) stringResource(R.string.i_smart_pending) else null,
                ).joinToString(" · ")
                SettingsRow(stringResource(R.string.i_smart_target), smart, "🧠") { nav.push(Page.SmartTarget) }
            }
            SectionLabel(stringResource(R.string.i_sec_app))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                val theme = stringResource(when (data.settings.theme) { ThemeMode.SYSTEM -> R.string.i_theme_system; ThemeMode.LIGHT -> R.string.i_theme_light; ThemeMode.DARK -> R.string.i_theme_dark })
                // AMOLED only does something while the app is dark, so only mention it then.
                val systemDark = isSystemInDarkTheme()
                val effectivelyDark = data.settings.theme == ThemeMode.DARK || (data.settings.theme == ThemeMode.SYSTEM && systemDark)
                val appearance = listOfNotNull(
                    theme,
                    if (data.settings.amoled && effectivelyDark) stringResource(R.string.i_amoled_short) else null,
                    if (data.settings.sketch) stringResource(R.string.i_style_sketch) else data.settings.font.label,
                ).joinToString(" · ")
                SettingsRow(stringResource(R.string.i_appearance), appearance, "🎨") { nav.push(Page.Appearance) }
                Divider()
                val b = data.backup
                val sub = when {
                    b.folderUri == null -> stringResource(R.string.i_backup_none)
                    b.lastAt == null -> stringResource(R.string.i_backup_not_run)
                    else -> listOfNotNull(
                        stringResource(R.string.i_backup_last, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(b.lastAt))),
                        if (!b.lastOk) stringResource(R.string.i_backup_failed_suffix) else null,
                    ).joinToString(" · ")
                }
                SettingsRow(stringResource(R.string.i_backup), sub, "📁") { nav.push(Page.Backup) }
                Divider()
                val on = data.reminders.count { it.on }
                val access = rememberNotificationAccess()
                val blocked = on > 0 && !access.allowed
                SettingsRow(
                    stringResource(R.string.i_reminders),
                    stringResource(if (blocked) R.string.i_reminders_off_perm else R.string.i_reminders_on, on),
                    "🔔",
                    action = if (blocked) stringResource(R.string.i_fix) else null,
                    onAction = access.fix,
                ) { nav.push(Page.Reminders) }
            }
            SectionLabel(stringResource(R.string.i_sec_about))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                SettingsRow(stringResource(R.string.i_about_food), stringResource(R.string.i_about_food_sub, FoodDb.dishes.size), "ℹ️") { nav.push(Page.About) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
