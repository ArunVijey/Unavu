package app.unavu.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Self-correcting calorie target (adaptive TDEE).
 *
 *   real burn ≈ average intake − (weight change per day × 7700 kcal/kg)
 *
 * The weight change comes from a smoothed trend (exponential moving average) so a salty dinner
 * or water weight doesn't swing it. Needs at least 14 well-logged days and 6 weigh-ins spread
 * over 14+ days within the last 28 days. Cheat days DO count here (the weight impact is real).
 */
object Smart {

    data class Estimate(
        val burn: Int,
        val avgIntake: Int,
        val kgPerWeek: Double,
        val loggedDays: Int,
        val weighIns: Int,
        val cheatDays: Int,
        val confidence: String,
    )

    const val WINDOW_DAYS = 28L
    private const val KCAL_PER_KG = 7700.0

    /** Days with at least two meals logged are "well logged" (one coffee isn't a full day). */
    fun loggedDays(d: AppData, from: LocalDate, to: LocalDate): List<String> =
        d.entries.filter { it.date >= from.toString() && it.date <= to.toString() }
            .groupBy { it.date }
            .filter { (_, es) -> es.map { it.meal }.distinct().size >= 2 }
            .keys.sorted()

    /** Exponential moving average of weigh-ins (α = 0.1 per day, like popular trend-weight apps). */
    fun trend(weights: List<WeightEntry>): List<Pair<LocalDate, Double>> {
        if (weights.isEmpty()) return emptyList()
        val sorted = weights.sortedBy { it.date }
        val out = mutableListOf<Pair<LocalDate, Double>>()
        var t = sorted.first().kg
        var prev = LocalDate.parse(sorted.first().date)
        for (w in sorted) {
            val day = LocalDate.parse(w.date)
            val gap = ChronoUnit.DAYS.between(prev, day).coerceAtLeast(1).toDouble()
            val alpha = 1 - Math.pow(0.9, gap)
            t += alpha * (w.kg - t)
            out += day to t
            prev = day
        }
        return out
    }

    fun estimate(d: AppData, today: LocalDate = LocalDate.now()): Estimate? {
        val from = today.minusDays(WINDOW_DAYS)
        val days = loggedDays(d, from, today.minusDays(1))
        if (days.size < 14) return null
        val weights = d.weights.filter { it.date >= from.toString() && it.date <= today.toString() }
        if (weights.size < 6) return null
        val span = ChronoUnit.DAYS.between(LocalDate.parse(weights.first().date), LocalDate.parse(weights.last().date))
        if (span < 14) return null
        val tr = trend(d.weights.filter { it.date <= today.toString() })
            .filter { !it.first.isBefore(from) }
        if (tr.size < 2) return null
        // least-squares slope of the trend line (kg per day)
        val xs = tr.map { ChronoUnit.DAYS.between(from, it.first).toDouble() }
        val ys = tr.map { it.second }
        val mx = xs.average(); val my = ys.average()
        val slope = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / xs.indices.sumOf { (xs[it] - mx) * (xs[it] - mx) }.coerceAtLeast(1e-9)
        val intake = days.map { day -> d.entriesOn(day).total().kcal }.average()
        val burn = (intake - slope * KCAL_PER_KG).roundToInt()
        val cheat = days.count { d.isCheatDay(it) }
        val confidence = when {
            days.size >= 24 && weights.size >= 16 -> "High"
            days.size >= 18 && weights.size >= 10 -> "Medium"
            else -> "Low"
        }
        // guard rails: never trust something wildly off the formula
        val formula = d.profile?.let { (bmrOf(it) * it.activity).roundToInt() } ?: burn
        val clamped = burn.coerceIn((formula * 0.7).roundToInt(), (formula * 1.3).roundToInt())
        return Estimate(clamped, intake.roundToInt(), slope * 7, days.size, weights.size, cheat, confidence)
    }

    /** Weekly check, called when the app opens. Returns updated smart settings (or the same). */
    fun weeklyCheck(d: AppData, today: LocalDate = LocalDate.now()): SmartTarget {
        val s = d.smart
        val last = s.lastCheckDate?.let(LocalDate::parse)
        if (last != null && ChronoUnit.DAYS.between(last, today) < 7) return s
        val e = estimate(d, today) ?: return s.copy(lastCheckDate = today.toString())
        val hist = s.history + (today.toString() to e.burn)
        return when (s.mode) {
            SmartMode.AUTO -> s.copy(appliedBurn = e.burn, pendingBurn = null, lastCheckDate = today.toString(), history = hist)
            SmartMode.ASK -> s.copy(pendingBurn = if (s.appliedBurn == e.burn) null else e.burn, lastCheckDate = today.toString(), history = hist)
            SmartMode.MANUAL -> s.copy(lastCheckDate = today.toString(), history = hist)
        }
    }
}
