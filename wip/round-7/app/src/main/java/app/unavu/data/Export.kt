package app.unavu.data

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.unavu.R
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Backup to a folder the user picked on the phone. Every run overwrites the same files; before the
 * main JSON is overwritten, the old one is kept as backup-previous.json (one safety copy).
 */
object Backup {
    const val JSON_NAME = "unavu-backup.json"
    /** The main file's name before 1.2.2. Restore takes any picked file; a run migrates the old file to [JSON_NAME]. */
    const val LEGACY_JSON_NAME = "calorie-tracker-backup.json"
    const val PREVIOUS_NAME = "backup-previous.json"
    const val LOG_CSV = "food-log.csv"
    const val SUMMARY_CSV = "daily-summary.csv"

    fun run(context: Context): Result<String> {
        val store = Store.get(context)
        val d = store.data.value
        val res = context.resources
        val result = runCatching {
            val uri = d.backup.folderUri ?: error(res.getString(R.string.r7d_backup_no_folder))
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(uri))
            if (tree == null || !tree.exists() || !tree.canWrite()) error(res.getString(R.string.r7d_backup_cant_reach))
            val cr = context.contentResolver
            fun file(name: String, mime: String): DocumentFile =
                tree.findFile(name) ?: tree.createFile(mime, name) ?: error(res.getString(R.string.r7d_backup_cant_create, name))
            fun write(f: DocumentFile, text: String) {
                (cr.openOutputStream(f.uri, "wt") ?: error(res.getString(R.string.r7d_backup_cant_write, f.name ?: ""))).use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }
            // The last good backup becomes the safety copy: the current file, or the old-named one on the first run
            // after the rename.
            val legacy = tree.findFile(LEGACY_JSON_NAME)
            (tree.findFile(JSON_NAME) ?: legacy)?.let { old ->
                val prev = cr.openInputStream(old.uri)?.use { it.readBytes() }
                if (prev != null && prev.isNotEmpty()) write(file(PREVIOUS_NAME, "application/json"), String(prev, Charsets.UTF_8))
            }
            write(file(JSON_NAME, "application/json"), store.exportJson())
            write(file(LOG_CSV, "text/csv"), foodLogCsv(d))
            write(file(SUMMARY_CSV, "text/csv"), dailySummaryCsv(d))
            // Migrated: its contents now live in the new file and the safety copy, so a stale file can't be restored by mistake.
            legacy?.let { runCatching { it.delete() } }
            res.getString(R.string.r7d_backup_done)
        }
        val now = System.currentTimeMillis()
        store.update { x ->
            x.copy(backup = x.backup.copy(lastAt = now, lastOk = result.isSuccess, lastMessage = result.fold({ it }, { readable(it, res) })))
        }
        return result.fold({ Result.success(it) }, { Result.failure(IllegalStateException(readable(it, res), it)) })
    }

    /** Reads and validates a backup file (any name, including the old calorie-tracker-backup.json). Nothing is replaced unless the whole file parses. */
    fun restore(context: Context, uri: Uri): Result<Int> = try {
        val res = context.resources
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error(res.getString(R.string.r7d_restore_cant_open))
        val d = try {
            Store.JSON.decodeFromString(AppData.serializer(), text)
        } catch (e: Exception) {
            error(res.getString(R.string.r7d_restore_not_backup))
        }
        if (d.version > AppData().version) error(res.getString(R.string.r7d_restore_newer))
        val store = Store.get(context)
        val keepBackup = store.data.value.backup
        store.replaceAll(d.copy(backup = keepBackup))
        Result.success(d.entries.size)
    } catch (e: Exception) {
        Result.failure(IllegalStateException(readable(e, context.resources), e))
    }

    /** A message people can act on. Pass [res] for the translated text; without it (callers with no Context) it falls back to English. */
    internal fun readable(e: Throwable, res: Resources? = null): String = when (e) {
        is SecurityException -> res?.getString(R.string.r7d_err_no_access) ?: "Unavu no longer has access to that folder. Pick it again in Settings › Backup."
        is java.io.FileNotFoundException -> res?.getString(R.string.r7d_err_not_there) ?: "The file or folder isn't there any more."
        is java.io.IOException -> {
            val detail = e.message ?: res?.getString(R.string.r7d_err_storage) ?: "storage error"
            res?.getString(R.string.r7d_err_io, detail) ?: "Couldn't read or write the file ($detail)."
        }
        else -> e.message ?: res?.getString(R.string.r7d_err_generic) ?: "Something went wrong."
    }

    private fun csv(vararg cells: Any?): String = cells.joinToString(",") { c ->
        val s = when (c) {
            null -> ""
            is Double -> if (c % 1.0 == 0.0) c.toLong().toString() else "%.2f".format(java.util.Locale.US, c)
            else -> c.toString()
        }
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    fun foodLogCsv(d: AppData): String = buildString {
        appendLine(csv("date", "time", "meal", "food", "tamil", "choices", "qty", "unit", "grams_or_ml", "kcal", "protein_g", "carbs_g", "fat_g", "fibre_g", "sodium_mg", "iron_mg", "calcium_mg", "added_sugar_g", "cheat"))
        d.entries.sortedWith(compareBy({ it.date }, { it.meal.ordinal }, { it.time })).forEach { e ->
            val f = e.food(); val n = e.nutr()
            appendLine(csv(e.date, e.time, e.meal.label, f?.name ?: e.foodId, f?.tamil, f?.choiceSummary(e.choices), e.qty, e.unit, e.grams(),
                n.kcal, n.protein, n.carbs, n.fat, n.fibre, n.sodium, n.iron, n.calcium, n.sugar, if (e.cheat || d.isCheatDay(e.date)) "yes" else ""))
        }
    }

    fun dailySummaryCsv(d: AppData): String = buildString {
        appendLine(csv("date", "kcal", "target_kcal", "protein_g", "carbs_g", "fat_g", "fibre_g", "sodium_mg", "iron_mg", "calcium_mg", "added_sugar_g", "water_ml", "drinks_ml", "weight_kg", "cheat_day", "note"))
        val dates = (d.entries.map { it.date } + d.water.keys + d.weights.map { it.date }).toSortedSet()
        dates.forEach { day ->
            val n = d.entriesOn(day).total()
            val (water, drinks) = d.liquidsMl(day)
            appendLine(csv(day, n.kcal, d.targetKcalOn(day), n.protein, n.carbs, n.fat, n.fibre, n.sodium, n.iron, n.calcium, n.sugar, water, drinks,
                d.weights.firstOrNull { it.date == day }?.kg, if (d.isCheatDay(day)) "yes" else "", d.dayNotes[day]))
        }
    }

    /** Due if daily backup is on, a folder is set and the last run was > 24 h ago (catch-up on app open). */
    fun isDue(d: AppData): Boolean {
        val b = d.backup
        if (!b.daily || b.folderUri == null) return false
        val last = b.lastAt ?: return true
        return System.currentTimeMillis() - last > 24 * 3600 * 1000L
    }

    @Suppress("unused")
    fun todayStamp(): String = LocalDate.now().toString()

    fun kcalRound(v: Double) = v.roundToInt()
}
