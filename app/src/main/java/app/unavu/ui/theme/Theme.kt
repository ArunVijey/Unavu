package app.unavu.ui.theme

import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontSynthesis
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.unavu.R
import app.unavu.data.AppFont
import app.unavu.data.Settings
import app.unavu.data.ThemeMode

/** Colour tokens, same names as in the mockups. */
data class AppColors(
    val bg: Color, val sf: Color, val sf2: Color, val line: Color,
    val ink: Color, val ink2: Color,
    val leaf: Color, val leafS: Color, val leafInk: Color,
    val tur: Color, val turS: Color, val turInk: Color,
    val chi: Color, val chiS: Color,
    val blue: Color, val blueS: Color,
    val m3: Color, val scrim: Color,
    val dark: Boolean,
    /** Accent (user-pickable): buttons, selected chips, bottom bar, + button. */
    val acc: Color = Color(0xFFF7C600),
    /** Soft accent background (selected chips/tiles). */
    val accS: Color = Color(0xFF3A2E14),
    /** Accent for text/lines on the normal background (readable in light and dark). */
    val accInk: Color = Color(0xFFF7C600),
    /** Text/icons drawn ON the accent colour. */
    val onAcc: Color = Color(0xFF111111),
)

const val DEFAULT_ACCENT = 0xFFF7C600.toInt()

/** Fill in the accent tokens from one picked colour, keeping text readable on both themes. */
fun AppColors.withAccent(a: Color): AppColors {
    val lum = a.luminance()
    val ink = if (dark) {
        if (lum < 0.18f) lerp(a, Color.White, 0.5f) else a
    } else {
        if (lum > 0.3f) lerp(a, Color.Black, 0.5f) else a
    }
    return copy(
        acc = a,
        accS = lerp(sf, a, if (dark) 0.22f else 0.16f),
        accInk = ink,
        onAcc = if (lum > 0.42f) Color(0xFF111111) else Color.White,
    )
}

val LightColors = AppColors(
    bg = Color(0xFFFBF8F2), sf = Color(0xFFFFFFFF), sf2 = Color(0xFFF1ECE1), line = Color(0xFFECE6DA),
    ink = Color(0xFF1C1B18), ink2 = Color(0xFF6B675E),
    leaf = Color(0xFF2F7D4F), leafS = Color(0xFFE3F1E8), leafInk = Color(0xFF2F7D4F),
    tur = Color(0xFFE8A317), turS = Color(0xFFFDF1D6), turInk = Color(0xFF9A6A00),
    chi = Color(0xFFD64545), chiS = Color(0xFFFBE3E3),
    blue = Color(0xFF4A6FA5), blueS = Color(0xFFE5ECF6),
    m3 = Color(0xFFC9733A), scrim = Color(0x8CF0ECE4), dark = false,
)

val DarkColors = AppColors(
    bg = Color(0xFF141311), sf = Color(0xFF1F1D1A), sf2 = Color(0xFF2B2824), line = Color(0xFF34312C),
    ink = Color(0xFFF2EEE6), ink2 = Color(0xFFA39E93),
    leaf = Color(0xFF4FAE76), leafS = Color(0xFF1C3326), leafInk = Color(0xFF7AD3A0),
    tur = Color(0xFFF0B43A), turS = Color(0xFF3A2E14), turInk = Color(0xFFF2C25E),
    chi = Color(0xFFF06B6B), chiS = Color(0xFF3D2020),
    blue = Color(0xFF7C9DD1), blueS = Color(0xFF1F2A3B),
    m3 = Color(0xFFE08B55), scrim = Color(0x8C0A0A09), dark = true,
)

val AmoledColors = DarkColors.copy(
    bg = Color(0xFF000000), sf = Color(0xFF0D0D0C), sf2 = Color(0xFF1A1917), line = Color(0xFF242320),
    ink2 = Color(0xFF9C978C), leafS = Color(0xFF132519), turS = Color(0xFF2A210D),
    chiS = Color(0xFF2E1616), blueS = Color(0xFF151E2B), scrim = Color(0x99000000),
)

val LocalColors = staticCompositionLocalOf { LightColors }

/** Shortcut: `U.c.leaf` */
object U {
    val c: AppColors @Composable get() = LocalColors.current
    val sketch: Boolean @Composable get() = LocalSketch.current
}

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(res: Int) = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

val GoogleSansFlex = variableFamily(R.font.googlesansflex)
val InterFamily = variableFamily(R.font.inter)
val ExcalifontFamily = FontFamily(Font(R.font.excalifont))
val TamilFamily = variableFamily(R.font.notosanstamil)

/**
 * Current text style (app font + tabular figures, which are on globally so numbers don't jump).
 * Use as `style = Tnum` where a style is needed explicitly.
 */
val Tnum: TextStyle @Composable get() = LocalTextStyle.current

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f), headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f), bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun AppTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val c = when {
        !dark -> LightColors
        settings.amoled -> AmoledColors
        else -> DarkColors
    }.withAccent(Color(settings.accent ?: DEFAULT_ACCENT))
    val family = when {
        settings.sketch || settings.font == AppFont.EXCALIFONT -> ExcalifontFamily
        settings.font == AppFont.INTER -> InterFamily
        else -> GoogleSansFlex
    }
    val exf = family == ExcalifontFamily
    // Excalifont is a thin pen: everything is drawn a bit heavier (Android's fake-bold),
    // a bit larger, and muted text gets more contrast, so it stays easy to read.
    val synth = FontSynthesis.All
    val cc = if (exf) c.copy(ink2 = lerp(c.ink2, c.ink, 0.35f)) else c

    val density = LocalDensity.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.navigationBarColor = c.sf.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    val scheme = if (dark) {
        darkColorScheme(primary = c.acc, onPrimary = c.onAcc, background = c.bg, onBackground = c.ink, surface = c.sf, onSurface = c.ink, surfaceVariant = c.sf2, outline = c.line, secondary = c.tur, error = c.chi)
    } else {
        lightColorScheme(primary = c.acc, onPrimary = c.onAcc, background = c.bg, onBackground = c.ink, surface = c.sf, onSurface = c.ink, surfaceVariant = c.sf2, outline = c.line, secondary = c.tur, error = c.chi)
    }

    MaterialTheme(colorScheme = scheme, typography = Typography().withFont(family)) {
        CompositionLocalProvider(
            LocalColors provides cc,
            LocalDensity provides Density(density.density, density.fontScale * if (exf) 1.14f else 1f),
            LocalSketch provides settings.sketch,
            LocalContentColor provides c.ink,
            LocalTextStyle provides TextStyle(
                fontFamily = family, fontSize = 14.sp, color = c.ink, fontFeatureSettings = "tnum", fontSynthesis = synth,
                fontWeight = if (exf) FontWeight.SemiBold else null,
                // keeps emoji (taller than the Latin font) from being clipped / squished
                platformStyle = PlatformTextStyle(includeFontPadding = true),
            ),
            content = content,
        )
    }
}
