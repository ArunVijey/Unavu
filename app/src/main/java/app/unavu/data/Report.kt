package app.unavu.data

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Monthly PDF report: averages (cheat days excluded), weight change, top foods, nutrient gaps. */
object Report {

    data class MonthStats(
        val month: YearMonth,
        val loggedDays: Int,
        val cheatDays: Int,
        val avg: Nutr,
        val target: Targets?,
        val weightStart: Double?,
        val weightEnd: Double?,
        val topFoods: List<Pair<String, Double>>,
        val daysOver: Int,
        val daysUnder: Int,
    )

    fun stats(d: AppData, ym: YearMonth): MonthStats {
        val prefix = ym.toString()
        val days = d.entries.filter { it.date.startsWith(prefix) }.map { it.date }.distinct().sorted()
        val normal = days.filterNot { d.isCheatDay(it) }
        val totals = normal.map { d.entriesOn(it).filterNot { e -> e.cheat }.total() }
        val avg = if (totals.isEmpty()) Nutr() else totals.fold(Nutr()) { a, b -> a + b } * (1.0 / totals.size)
        val t = d.targets()
        val w = d.weights.filter { it.date.startsWith(prefix) }.sortedBy { it.date }
        val top = d.entries.filter { it.date.startsWith(prefix) }.groupBy { it.foodId }
            .map { (id, es) -> (FoodDb.get(id)?.name ?: id) to es.sumOf { it.nutr().kcal } }
            .sortedByDescending { it.second }.take(8)
        val over = t?.let { tt -> normal.count { day -> d.entriesOn(day).total().kcal > tt.kcal * 1.05 } } ?: 0
        val under = t?.let { tt -> normal.count { day -> d.entriesOn(day).total().kcal < tt.kcal * 0.85 } } ?: 0
        return MonthStats(ym, days.size, days.count { d.isCheatDay(it) }, avg, t, w.firstOrNull()?.kg, w.lastOrNull()?.kg, top, over, under)
    }

    fun pdf(d: AppData, ym: YearMonth): ByteArray {
        val s = stats(d, ym)
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas
        val title = Paint().apply { textSize = 22f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(28, 27, 24); isAntiAlias = true }
        val h = Paint().apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = Color.rgb(47, 125, 79); isAntiAlias = true }
        val p = Paint().apply { textSize = 11.5f; color = Color.rgb(28, 27, 24); isAntiAlias = true }
        val muted = Paint().apply { textSize = 10f; color = Color.rgb(107, 103, 94); isAntiAlias = true }
        val bar = Paint().apply { color = Color.rgb(47, 125, 79) }
        val track = Paint().apply { color = Color.rgb(241, 236, 225) }
        var y = 56f
        val monthName = ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.year
        c.drawText("Unavu · $monthName report", 40f, y, title); y += 20
        c.drawText("Generated ${LocalDate.now()} · averages exclude cheat days · food values from IFCT 2017 recipes", 40f, y, muted); y += 30

        c.drawText("Overview", 40f, y, h); y += 20
        val t = s.target
        listOf(
            "Days logged" to "${s.loggedDays}  (cheat days: ${s.cheatDays})",
            "Average calories" to "${s.avg.kcal.roundToInt()} kcal / day" + (t?.let { " · target ${it.kcal}" } ?: ""),
            "Days over / under target" to "${s.daysOver} over · ${s.daysUnder} well under",
            "Weight" to if (s.weightStart != null && s.weightEnd != null) "%.1f → %.1f kg (%+.1f kg)".format(s.weightStart, s.weightEnd, s.weightEnd - s.weightStart) else "Not enough weigh-ins",
        ).forEach { (k, v) -> c.drawText(k, 40f, y, muted); c.drawText(v, 220f, y, p); y += 18 }
        y += 16

        c.drawText("Average per day vs target", 40f, y, h); y += 18
        if (t != null) {
            NUTRIENTS.forEach { n ->
                val have = n.value(s.avg); val target = n.target(t)
                val frac = (have / target).coerceIn(0.0, 1.0).toFloat()
                c.drawText(n.name, 40f, y + 9, p)
                c.drawRoundRect(170f, y, 430f, y + 10, 5f, 5f, track)
                bar.color = when {
                    n.limit && have > target -> Color.rgb(214, 69, 69)
                    !n.limit && have < target * 0.9 -> Color.rgb(232, 163, 23)
                    else -> Color.rgb(47, 125, 79)
                }
                c.drawRoundRect(170f, y, 170f + 260f * frac, y + 10, 5f, 5f, bar)
                c.drawText("%.0f / %.0f %s".format(have, target, n.unit), 440f, y + 9, muted)
                y += 20
            }
        }
        y += 14
        c.drawText("Top foods by calories", 40f, y, h); y += 18
        s.topFoods.forEach { (name, kcal) -> c.drawText(name, 40f, y, p); c.drawText("${kcal.roundToInt()} kcal", 440f, y, muted); y += 16 }
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out); doc.close()
        return out.toByteArray()
    }

    /** Saves into the backup folder if set, else into the app's cache. Returns a shareable Uri or file path text. */
    fun save(context: Context, d: AppData, ym: YearMonth): Result<Uri> = runCatching {
        val bytes = pdf(d, ym)
        val name = "unavu-report-$ym.pdf"
        val folder = d.backup.folderUri?.let { DocumentFile.fromTreeUri(context, Uri.parse(it)) }
        if (folder != null && folder.canWrite()) {
            val f = folder.findFile(name) ?: folder.createFile("application/pdf", name) ?: error("Couldn't create $name")
            context.contentResolver.openOutputStream(f.uri, "wt")!!.use { it.write(bytes) }
            f.uri
        } else {
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val f = File(dir, name); f.writeBytes(bytes)
            androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", f)
        }
    }
}
