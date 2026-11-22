package app.unavu.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

data class Targets(
    val bmr: Int,
    val maintenance: Int,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fibre: Int,
    val iron: Double,
    val calcium: Double = 1000.0,
    val sodium: Double = 2000.0,
    val sugar: Double = 25.0,
    val satFat: Double = 0.0,
    val vitC: Double = 80.0,
    val vitA: Double = 1000.0,
    val potassium: Double = 3500.0,
    /** Where the burn number came from. */
    val burnSource: String = "Formula (Mifflin-St Jeor)",
)

fun bmrOf(p: Profile): Int = (10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (p.male) 5 else -161).roundToInt()

/** Mifflin-St Jeor + activity + goal (or the smart-target burn); nutrients per ICMR-NIN 2020 RDA. */
fun targetsFor(p: Profile, smart: SmartTarget? = null): Targets {
    val bmr = bmrOf(p)
    val formula = (bmr * p.activity).roundToInt()
    val maintenance = smart?.appliedBurn ?: formula
    val kcal = when {
        smart?.mode == SmartMode.MANUAL && smart.manualKcal != null -> smart.manualKcal
        smart?.manualKcal != null && smart.mode != SmartMode.AUTO -> smart.manualKcal
        else -> max(1200, maintenance + p.goalKcal)
    }
    val protein = (p.weightKg * 1.2).roundToInt()
    val fat = (kcal * 0.28 / 9).roundToInt()
    val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt()
    val fibre = (kcal / 1000.0 * 15).roundToInt()
    return Targets(
        bmr, maintenance, kcal, protein, carbs, fat, fibre,
        iron = if (p.male) 19.0 else 29.0,
        satFat = kcal * 0.10 / 9,
        vitC = if (p.male) 80.0 else 65.0,
        vitA = if (p.male) 1000.0 else 840.0,
        burnSource = if (smart?.appliedBurn != null) "Smart target (your logs + weight)" else "Formula (Mifflin-St Jeor)",
    )
}

fun AppData.targets(): Targets? = profile?.let { targetsFor(it, smart) }

val DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
fun today(): String = LocalDate.now().format(DATE)
fun nowTime(): String = LocalTime.now().format(TIME)

fun LogEntry.food(): Food? = FoodDb.get(foodId)
fun LogEntry.nutr(): Nutr = food()?.nutrFor(qty, unit, choices) ?: Nutr()
fun LogEntry.grams(): Double = (food()?.unit(unit)?.grams ?: 0.0) * qty

fun AppData.entriesOn(date: String): List<LogEntry> = entriesByDate[date] ?: emptyList()

/** Entries with from <= date <= to (ISO dates compare correctly as strings), ordered by date. */
fun AppData.entriesBetween(from: String, to: String): List<LogEntry> =
    if (from > to) emptyList() else entriesByDate.filterKeys { it >= from && it <= to }.toSortedMap().values.flatten()

fun List<LogEntry>.total(): Nutr = fold(Nutr()) { acc, e -> acc + e.nutr() }

/** A day counts as a cheat day if marked, or if every logged meal is a cheat meal. */
fun AppData.isCheatDay(date: String): Boolean = date in cheatDays

/** Liquids for a day: plain water + every drink logged in meals (ml). */
fun AppData.liquidsMl(date: String): Pair<Int, Int> {
    val water = water[date] ?: 0
    val drinks = entriesOn(date).filter { it.food()?.liquid == true }.sumOf { it.grams() }.roundToInt()
    return water to drinks
}

/** Human-friendly quantity: 1.5 -> "1.5", 2.0 -> "2", 0.5 -> "½". */
fun fmtQty(q: Double): String = when (q) {
    0.5 -> "½"
    0.25 -> "¼"
    else -> if (q % 1.0 == 0.0) q.toInt().toString() else "%.2f".format(q).trimEnd('0').trimEnd('.')
}

fun unitLabel(unit: String, qty: Double): String = when (unit) {
    "g", "ml", "100 g", "tbsp", "tsp" -> unit
    "egg" -> if (qty > 1.0) "eggs" else "egg"
    else -> if (qty > 1.0 && !unit.endsWith("s")) unit + "s" else unit
}

fun LogEntry.portionText(): String = "${fmtQty(qty)} ${unitLabel(unit, qty)}"
