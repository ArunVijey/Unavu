package app.unavu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.unavu.R
import app.unavu.data.AppFont
import app.unavu.data.ThemeMode
import app.unavu.ui.AppCard
import app.unavu.ui.LocalData
import app.unavu.ui.Segmented
import app.unavu.ui.SectionLabel
import app.unavu.ui.TopBar
import app.unavu.ui.nav
import app.unavu.ui.Divider
import app.unavu.ui.theme.ExcalifontFamily
import app.unavu.ui.theme.GoogleSansFlex
import app.unavu.ui.theme.InterFamily
import app.unavu.ui.theme.U

@Composable
fun AppearanceScreen() {
    val c = U.c
    val nav = nav
    val data = LocalData.current
    val store = nav.store
    val s = data.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(stringResource(R.string.i_appearance), onBack = { nav.pop() })
        Column(Modifier.padding(horizontal = 18.dp)) {
            SectionLabel(stringResource(R.string.i_style))
            Segmented(listOf(stringResource(R.string.i_style_clean), stringResource(R.string.i_style_sketch)), if (s.sketch) 1 else 0) { i ->
                store.update { d -> d.copy(settings = d.settings.copy(sketch = i == 1)) }
            }
            Text(
                stringResource(if (s.sketch) R.string.i_sketch_desc else R.string.i_clean_desc),
                style = U.t.caption, color = c.ink2, modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
            SectionLabel(stringResource(R.string.i_theme))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                Column(Modifier.selectableGroup()) {
                    val options = listOf(
                        Triple(ThemeMode.SYSTEM, R.string.i_theme_system_default, R.string.i_theme_system_sub),
                        Triple(ThemeMode.LIGHT, R.string.i_theme_light, null),
                        Triple(ThemeMode.DARK, R.string.i_theme_dark, null),
                    )
                    options.forEachIndexed { i, (mode, title, sub) ->
                        if (i > 0) Divider()
                        SettingRadioRow(stringResource(title), sub?.let { stringResource(it) }, s.theme == mode) {
                            store.update { d -> d.copy(settings = d.settings.copy(theme = mode)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            AppCard(padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                SettingSwitchRow(stringResource(R.string.i_amoled), stringResource(R.string.i_amoled_sub), s.amoled) { v ->
                    store.update { d -> d.copy(settings = d.settings.copy(amoled = v)) }
                }
            }
            SectionLabel(stringResource(R.string.i_accent))
            AccentSection()
            SectionLabel(stringResource(R.string.i_font))
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AppFont.GOOGLE_SANS_FLEX to GoogleSansFlex, AppFont.INTER to InterFamily, AppFont.EXCALIFONT to ExcalifontFamily).forEach { (font, family) ->
                    val on = if (s.sketch) font == AppFont.EXCALIFONT else s.font == font
                    val desc = stringResource(when (font) { AppFont.GOOGLE_SANS_FLEX -> R.string.i_font_default; AppFont.INTER -> R.string.i_font_compact; AppFont.EXCALIFONT -> R.string.i_font_hand })
                    Column(
                        Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp)).background(c.sf)
                            .border(if (on) 2.dp else 1.dp, if (on) c.acc else c.line, RoundedCornerShape(18.dp))
                            .selectable(selected = on, role = Role.RadioButton) { store.update { d -> d.copy(settings = d.settings.copy(font = font)) } }
                            .padding(12.dp),
                    ) {
                        Text(stringResource(R.string.i_font_sample), style = U.t.title.copy(fontFamily = family, fontWeight = FontWeight.Bold), color = c.ink)
                        Text(font.label, style = U.t.label.copy(fontFamily = family, fontWeight = FontWeight.SemiBold), color = c.ink)
                        Text(desc, style = U.t.caption, color = c.ink2)
                    }
                }
            }
            if (s.sketch) Text(stringResource(R.string.i_sketch_font_note), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            Text(stringResource(R.string.i_tamil_font_note), style = U.t.caption, color = c.ink2, modifier = Modifier.padding(6.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}
