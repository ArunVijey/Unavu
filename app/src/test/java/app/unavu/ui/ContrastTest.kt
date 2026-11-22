package app.unavu.ui

import androidx.compose.ui.graphics.Color
import app.unavu.ui.theme.AmoledColors
import app.unavu.ui.theme.AppColors
import app.unavu.ui.theme.DEFAULT_ACCENT
import app.unavu.ui.theme.DarkColors
import app.unavu.ui.theme.LightColors
import app.unavu.ui.theme.contrast
import app.unavu.ui.theme.withAccent
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text/background token pair, in all three palettes, with the default accent and the picker swatches, must reach 4.5:1. */
class ContrastTest {
    private val swatches = listOf(
        DEFAULT_ACCENT, 0xFF4FAE76.toInt(), 0xFFE5484D.toInt(), 0xFFFF8A1F.toInt(),
        0xFF3B82F6.toInt(), 0xFF14B8A6.toInt(), 0xFF8B5CF6.toInt(), 0xFFEC4899.toInt(),
    )
    private val palettes = mapOf("light" to LightColors, "dark" to DarkColors, "amoled" to AmoledColors)

    private fun pairs(c: AppColors): List<Triple<String, Color, Color>> = buildList {
        for ((bn, bg) in listOf("bg" to c.bg, "sf" to c.sf, "sf2" to c.sf2)) {
            add(Triple("ink on $bn", c.ink, bg)); add(Triple("ink2 on $bn", c.ink2, bg)); add(Triple("hint on $bn", c.hint, bg))
            add(Triple("chiInk on $bn", c.chiInk, bg)); add(Triple("blueInk on $bn", c.blueInk, bg))
            add(Triple("leafInk on $bn", c.leafInk, bg)); add(Triple("turInk on $bn", c.turInk, bg))
            add(Triple("accInk on $bn", c.accInk, bg))
        }
        add(Triple("chiInk on chiS", c.chiInk, c.chiS)); add(Triple("blueInk on blueS", c.blueInk, c.blueS))
        add(Triple("leafInk on leafS", c.leafInk, c.leafS)); add(Triple("turInk on turS", c.turInk, c.turS))
        add(Triple("accInk on accS", c.accInk, c.accS)); add(Triple("onAcc on acc", c.onAcc, c.acc))
        add(Triple("ink on accS", c.ink, c.accS))
    }

    @Test fun allPairsPass() {
        val failures = mutableListOf<String>()
        for ((pn, base) in palettes) for (sw in swatches) {
            val c = base.withAccent(Color(sw))
            for ((name, fg, bg) in pairs(c)) {
                val r = contrast(fg, bg)
                if (r < 4.5f) failures += "$pn accent=${Integer.toHexString(sw)} $name = %.2f".format(r)
            }
        }
        assertTrue("Contrast failures:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
