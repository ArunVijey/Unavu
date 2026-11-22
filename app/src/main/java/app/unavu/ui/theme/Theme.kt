package app.unavu.ui.theme

import androidx.compose.ui.text.font.FontSynthesis
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.ReadOnlyComposable
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
    /** Red text readable on chiS / sf / bg. */
    val chiInk: Color = chi,
    /** Blue text readable on blueS / sf / bg. */
    val blueInk: Color = blue,
    /** Placeholder text (≥4.5:1 on bg / sf / sf2). */
    val hint: Color = ink2,
)

const val DEFAULT_ACCENT = 0xFFF7C600.toInt()

/** WCAG 2.x contrast ratio between two opaque colours (1..21). */
fun contrast(a: Color, b: Color): Float {
    val la = a.luminance(); val lb = b.luminance()
    val hi = maxOf(la, lb); val lo = minOf(la, lb)
    return (hi + 0.05f) / (lo + 0.05f)
}

private val InkDark = Color(0xFF111111)

/** Fill in the accent tokens from one picked colour, keeping text readable on both themes (>= 4.5:1). */
fun AppColors.withAccent(a: Color): AppColors {
    // text on the accent: whichever of near-black / white has the better real contrast
    val onAcc = if (contrast(a, InkDark) >= contrast(a, Color.White)) InkDark else Color.White
    val accS = lerp(sf, a, if (dark) 0.22f else 0.16f)
    // accent-coloured text: push towards white (dark) or black (light) until it passes on bg, sf and accS
    val target = if (dark) Color.White else Color.Black
    var ink = a
    var t = 0f
    while (t < 1f && (contrast(ink, bg) < 4.5f || contrast(ink, sf) < 4.5f || contrast(ink, accS) < 4.5f)) {
        t += 0.04f
        ink = lerp(a, target, t.coerceAtMost(1f))
    }
    return copy(acc = a, accS = accS, accInk = ink, onAcc = onAcc)
}

val LightColors = AppColors(
    bg = Color(0xFFFBF8F2), sf = Color(0xFFFFFFFF), sf2 = Color(0xFFF1ECE1), line = Color(0xFFECE6DA),
    ink = Color(0xFF1C1B18), ink2 = Color(0xFF6B675E),
    leaf = Color(0xFF2F7D4F), leafS = Color(0xFFE3F1E8), leafInk = Color(0xFF2C764B),
    tur = Color(0xFFE8A317), turS = Color(0xFFFDF1D6), turInk = Color(0xFF8D6100),
    chi = Color(0xFFD64545), chiS = Color(0xFFFBE3E3),
    blue = Color(0xFF4A6FA5), blueS = Color(0xFFE5ECF6),
    m3 = Color(0xFFC9733A), scrim = Color(0x8CF0ECE4), dark = false,
    chiInk = Color(0xFFB83B3B), blueInk = Color(0xFF476BA0), hint = Color(0xFF6B675E),
)

val DarkColors = AppColors(
    bg = Color(0xFF141311), sf = Color(0xFF1F1D1A), sf2 = Color(0xFF2B2824), line = Color(0xFF34312C),
    ink = Color(0xFFF2EEE6), ink2 = Color(0xFFA39E93),
    leaf = Color(0xFF4FAE76), leafS = Color(0xFF1C3326), leafInk = Color(0xFF7AD3A0),
    tur = Color(0xFFF0B43A), turS = Color(0xFF3A2E14), turInk = Color(0xFFF2C25E),
    chi = Color(0xFFF06B6B), chiS = Color(0xFF3D2020),
    blue = Color(0xFF7C9DD1), blueS = Color(0xFF1F2A3B),
    m3 = Color(0xFFE08B55), scrim = Color(0x8C0A0A09), dark = true,
    chiInk = Color(0xFFF06B6B), blueInk = Color(0xFF7C9DD1), hint = Color(0xFFA39E93),
)

val AmoledColors = DarkColors.copy(
    bg = Color(0xFF000000), sf = Color(0xFF0D0D0C), sf2 = Color(0xFF1A1917), line = Color(0xFF242320),
    ink2 = Color(0xFF9C978C), leafS = Color(0xFF132519), turS = Color(0xFF2A210D),
    chiS = Color(0xFF2E1616), blueS = Color(0xFF151E2B), scrim = Color(0x99000000),
    hint = Color(0xFF9C978C),
)

val LocalColors = staticCompositionLocalOf { LightColors }

/** Shortcut: `U.c.leaf` */
object U {
    val c: AppColors @Composable get() = LocalColors.current
    val sketch: Boolean @Composable get() = LocalSketch.current
    /** Type roles: `U.t.body`, `U.t.title` … */
    val t: AppType @Composable get() = currentType
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

/** Material typography mapped from our roles so any stock M3 component matches the app. */
private fun AppType.toTypography() = Typography(
    displayLarge = display, displayMedium = display, displaySmall = headline,
    headlineLarge = headline, headlineMedium = headline, headlineSmall = title,
    titleLarge = title, titleMedium = titleS, titleSmall = titleS,
    bodyLarge = body, bodyMedium = bodyS, bodySmall = caption,
    labelLarge = label, labelMedium = label, labelSmall = caption,
)

private fun colorScheme(c: AppColors) =
    if (c.dark) darkColorScheme(
        primary = c.acc, onPrimary = c.onAcc, primaryContainer = c.accS, onPrimaryContainer = c.accInk,
        secondary = c.tur, onSecondary = c.ink, secondaryContainer = c.turS, onSecondaryContainer = c.turInk,
        tertiary = c.leaf, onTertiary = Color.White, tertiaryContainer = c.leafS, onTertiaryContainer = c.leafInk,
        background = c.bg, onBackground = c.ink, surface = c.sf, onSurface = c.ink,
        surfaceVariant = c.sf2, onSurfaceVariant = c.ink2, surfaceTint = c.acc,
        inverseSurface = c.ink, inverseOnSurface = c.bg, inversePrimary = c.acc,
        error = c.chi, onError = Color.White, errorContainer = c.chiS, onErrorContainer = c.chiInk,
        outline = c.line, outlineVariant = c.line, scrim = c.scrim,
        surfaceBright = c.sf, surfaceDim = c.bg, surfaceContainerLowest = c.bg, surfaceContainerLow = c.sf,
        surfaceContainer = c.sf, surfaceContainerHigh = c.sf2, surfaceContainerHighest = c.sf2,
    ) else lightColorScheme(
        primary = c.acc, onPrimary = c.onAcc, primaryContainer = c.accS, onPrimaryContainer = c.accInk,
        secondary = c.tur, onSecondary = c.ink, secondaryContainer = c.turS, onSecondaryContainer = c.turInk,
        tertiary = c.leaf, onTertiary = Color.White, tertiaryContainer = c.leafS, onTertiaryContainer = c.leafInk,
        background = c.bg, onBackground = c.ink, surface = c.sf, onSurface = c.ink,
        surfaceVariant = c.sf2, onSurfaceVariant = c.ink2, surfaceTint = c.acc,
        inverseSurface = c.ink, inverseOnSurface = c.bg, inversePrimary = c.acc,
        error = c.chi, onError = Color.White, errorContainer = c.chiS, onErrorContainer = c.chiInk,
        outline = c.line, outlineVariant = c.line, scrim = c.scrim,
        surfaceBright = c.sf, surfaceDim = c.bg, surfaceContainerLowest = c.bg, surfaceContainerLow = c.sf,
        surfaceContainer = c.sf, surfaceContainerHigh = c.sf2, surfaceContainerHighest = c.sf2,
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
    // Excalifont is a thin pen: heavier (Android's fake-bold), +1sp per role, and muted text gets more contrast.
    val cc = if (exf) c.copy(ink2 = lerp(c.ink2, c.ink, 0.35f)) else c

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

    val base = TextStyle(
        fontFamily = family, fontSize = 14.sp, color = c.ink, fontFeatureSettings = "tnum", fontSynthesis = FontSynthesis.All,
        fontWeight = if (exf) FontWeight.SemiBold else null,
        // keeps emoji (taller than the Latin font) from being clipped / squished
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
    val type = appType(base, exf)
    val reduced = rememberReducedMotion()

    MaterialTheme(colorScheme = colorScheme(c), typography = type.toTypography()) {
        CompositionLocalProvider(
            LocalColors provides cc,
            LocalSketch provides settings.sketch,
            LocalContentColor provides c.ink,
            LocalTextStyle provides base,
            LocalAppType provides type,
            LocalReducedMotion provides reduced,
            content = content,
        )
    }
}
