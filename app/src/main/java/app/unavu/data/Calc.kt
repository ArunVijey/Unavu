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

/**
 * The daily kcal target that applied on [date] (ISO). Past days keep the target they were judged against, so a later
 * weigh-in or goal change doesn't recolour history.
 *
 * - No history yet (older data, or the target never changed): the current target, as before.
 * - On or after the newest change: the current target (today is always judged against the live number).
 * - Between changes: the newest change made on or before [date].
 * - Before the first recorded change: the first known target (the one in force when tracking started).
 *
 * Null only when there is no profile yet.
 */
fun AppData.targetKcalOn(date: String): Int? {
    val current = targets()?.kcal ?: return null
    val days = targetDays
    if (days.isEmpty() || date >= days.last()) return current
    // index of the newest change on or before the date; before the first change, the first entry is used
    val i = days.binarySearch(date).let { if (it >= 0) it else -it - 2 }.coerceAtLeast(0)
    return targetHistory[days[i]] ?: current
}

/**
 * Records a change of the daily kcal target (called by [Store.update] for every write).
 *
 * [beforeKcal] is the target before the change (null when unknown, e.g. a restore). When the history is still empty,
 * the old target is first stored as a baseline dated at the earliest logged day, so every day before [on] keeps it.
 * Entries that repeat the previous value are dropped, so undoing a change on the same day leaves no trace.
 */
fun AppData.withTargetRecorded(beforeKcal: Int?, on: String = today()): AppData {
    val now = targets()?.kcal ?: return this
    val hist = targetHistory
    if (hist.isEmpty()) {
        if (beforeKcal == null || beforeKcal == now) return this
        val dayBefore = java.time.LocalDate.parse(on).minusDays(1).toString()
        val firstData = listOfNotNull(entries.minOfOrNull { it.date }, weights.minOfOrNull { it.date }, water.keys.minOrNull()).minOrNull()
        val baseline = if (firstData != null && firstData < dayBefore) firstData else dayBefore
        return copy(targetHistory = mapOf(baseline to beforeKcal, on to now))
    }
    val latest = targetDays.last()
    if (hist[latest] == now) return this
    // a change dated earlier than the newest record (clock moved back) still lands on the newest record's day
    val key = if (on >= latest) on else latest
    val next = (hist + (key to now)).toSortedMap()
    // drop entries that repeat the value before them (e.g. a change undone later the same day)
    var prev: Int? = null
    val pruned = LinkedHashMap<String, Int>()
    for ((k, v) in next) { if (v != prev) pruned[k] = v; prev = v }
    return copy(targetHistory = pruned)
}

/**
 * The one place that knows the meal time windows (minutes after midnight):
 * 04:00-11:00 breakfast, -15:30 lunch, -18:30 snacks, -23:00 dinner; late night counts as snacks.
 */
fun mealForMinutes(mins: Int): Meal = when {
    mins < 4 * 60 -> Meal.SNACKS
    mins < 11 * 60 -> Meal.BREAKFAST
    mins < 15 * 60 + 30 -> Meal.LUNCH
    mins < 18 * 60 + 30 -> Meal.SNACKS
    mins < 23 * 60 -> Meal.DINNER
    else -> Meal.SNACKS
}

fun nowMinutes(): Int = LocalTime.now().let { it.hour * 60 + it.minute }

/**
 * The meal to pre-select for a new log: the one the clock says, or, when that meal is already in [logged], the next
 * meal (in day order, wrapping round) that isn't. All four logged: the clock's meal. Only a suggestion, never filed
 * automatically. Callers pass the meals logged on the day being logged to, e.g. `data.entriesOn(date).map { it.meal }.toSet()`.
 */
fun suggestMeal(logged: Set<Meal>, minutes: Int = nowMinutes()): Meal {
    val clock = mealForMinutes(minutes)
    if (clock !in logged) return clock
    val all = Meal.entries
    for (step in 1 until all.size) {
        val m = all[(clock.ordinal + step) % all.size]
        if (m !in logged) return m
    }
    return clock
}

/** One meal's share of the day's kcal target: the single rounding rule used by every screen. */
fun mealShare(targetKcal: Int, split: List<Double>, meal: Meal): Int =
    (targetKcal * (split.getOrNull(meal.ordinal) ?: 0.0)).roundToInt()

/** Every whole number shown to people goes through this: Indian grouping ("2,064", "1,00,000"). */
fun fmtInt(v: Int): String = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("en-IN")).format(v)
fun fmtInt(v: Double): String = fmtInt(v.roundToInt())

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
