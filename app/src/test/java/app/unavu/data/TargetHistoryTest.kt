package app.unavu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 7 #11 (per-day target history), #4 (one meal-share rule) and the shared meal suggestion. */
class TargetHistoryTest {
    private val p72 = Profile(true, 28, 172.0, 72.0, 1.55, -500)
    private val p80 = p72.copy(weightKg = 80.0)
    private val k72 = targetsFor(p72).kcal
    private val k80 = targetsFor(p80).kcal

    private fun entry(date: String) = LogEntry(1, date, Meal.LUNCH, "idli", 2.0, "piece", "12:00")
    private val base = AppData(profile = p72, entries = listOf(entry("2026-11-01")))

    @Test fun noHistoryMeansEveryDayUsesTheCurrentTarget() {
        assertNotEquals(k72, k80)
        assertEquals(k72, base.targetKcalOn("2026-10-01"))
        assertEquals(k72, base.targetKcalOn("2026-11-15"))
        assertNull(AppData().targetKcalOn("2026-11-15"))
    }

    @Test fun aWeighInKeepsOldDaysOnTheOldTarget() {
        val after = base.copy(profile = p80).withTargetRecorded(beforeKcal = k72, on = "2026-11-20")
        assertEquals(mapOf("2026-11-01" to k72, "2026-11-20" to k80), after.targetHistory)
        assertEquals(k72, after.targetKcalOn("2026-10-01")) // before tracking started
        assertEquals(k72, after.targetKcalOn("2026-11-10"))
        assertEquals(k72, after.targetKcalOn("2026-11-19"))
        assertEquals(k80, after.targetKcalOn("2026-11-20"))
        assertEquals(k80, after.targetKcalOn("2026-12-01"))
    }

    @Test fun laterChangesAddUpAndSameDayUndoLeavesNoTrace() {
        val a = base.copy(profile = p80).withTargetRecorded(k72, "2026-11-20")
        val b = a.copy(profile = p72).withTargetRecorded(k80, "2026-11-22")
        assertEquals(k72, b.targetKcalOn("2026-11-10"))
        assertEquals(k80, b.targetKcalOn("2026-11-21"))
        assertEquals(k72, b.targetKcalOn("2026-11-22"))
        // changed and changed back on the same day: the history is as before
        val undone = a.copy(profile = p72).withTargetRecorded(k80, "2026-11-20")
        assertEquals(mapOf("2026-11-01" to k72), undone.targetHistory)
        // an unchanged target records nothing
        assertEquals(a, a.withTargetRecorded(k80, "2026-11-25"))
        assertEquals(base, base.withTargetRecorded(k72, "2026-11-25"))
    }

    @Test fun lookupIsCorrectForManyDays() {
        var d = base
        var prev = k72
        for ((i, w) in listOf(73.0, 75.0, 78.0, 80.0).withIndex()) {
            d = d.copy(profile = p72.copy(weightKg = w)).withTargetRecorded(prev, "2026-11-%02d".format(5 + i * 5))
            prev = d.targets()!!.kcal
        }
        for (day in 1..30) {
            val date = "2026-11-%02d".format(day)
            val expected = when {
                day >= 20 -> targetsFor(p72.copy(weightKg = 80.0)).kcal
                day >= 15 -> targetsFor(p72.copy(weightKg = 78.0)).kcal
                day >= 10 -> targetsFor(p72.copy(weightKg = 75.0)).kcal
                day >= 5 -> targetsFor(p72.copy(weightKg = 73.0)).kcal
                else -> k72
            }
            assertEquals(date, expected, d.targetKcalOn(date))
        }
    }

    @Test fun oldBackupsWithoutHistoryStillDecode() {
        val json = Store.JSON.encodeToString(AppData.serializer(), base)
            .replace(Regex(",?\"targetHistory\":\\{[^}]*\\}"), "")
        val d = Store.JSON.decodeFromString(AppData.serializer(), json)
        assertTrue(d.targetHistory.isEmpty())
        assertEquals(k72, d.targetKcalOn("2026-11-10"))
    }

    @Test fun oldEntriesWithoutLoggedOnStillDecode() {
        val e = Store.JSON.decodeFromString(
            LogEntry.serializer(),
            """{"id":1,"date":"2026-11-01","meal":"LUNCH","foodId":"idli","qty":2.0,"unit":"piece","time":"12:00"}""",
        )
        assertNull(e.loggedOn)
    }

    @Test fun mealSharesAreConsistent() {
        val split = Settings().mealSplit
        for (target in listOf(1200, 1500, 1999, 2000, 2058, 2064, 2310, 2750, 3333)) {
            val shares = Meal.entries.map { mealShare(target, split, it) }
            // each share is the rounded exact share, and the four shares re-add to the target within rounding
            Meal.entries.forEach { assertEquals((target * split[it.ordinal]).let { x -> Math.round(x).toInt() }, shares[it.ordinal]) }
            assertTrue("shares $shares for $target", kotlin.math.abs(shares.sum() - target) <= 2)
        }
        // the 2064 example from the audit: 25 / 35 / 15 / 25 %
        assertEquals(listOf(516, 722, 310, 516), Meal.entries.map { mealShare(2064, split, it) })
        // the same inputs always give the same answer, whichever screen asks
        assertEquals(mealShare(2058, split, Meal.SNACKS), mealShare(2058, split, Meal.SNACKS))
        assertEquals(309, mealShare(2058, split, Meal.SNACKS))
        // a split that is too short never crashes
        assertEquals(0, mealShare(2000, listOf(1.0), Meal.DINNER))
    }

    @Test fun suggestedMealPrefersTheClockThenTheNextUnloggedOne() {
        val noon = 12 * 60
        assertEquals(Meal.LUNCH, suggestMeal(emptySet(), noon))
        assertEquals(Meal.SNACKS, suggestMeal(setOf(Meal.LUNCH), noon))
        assertEquals(Meal.DINNER, suggestMeal(setOf(Meal.LUNCH, Meal.SNACKS), noon))
        assertEquals(Meal.BREAKFAST, suggestMeal(setOf(Meal.LUNCH, Meal.SNACKS, Meal.DINNER), noon))
        assertEquals(Meal.LUNCH, suggestMeal(Meal.entries.toSet(), noon))
        assertEquals(Meal.BREAKFAST, suggestMeal(emptySet(), 8 * 60))
        assertEquals(Meal.SNACKS, suggestMeal(emptySet(), 17 * 60))
        assertEquals(Meal.DINNER, suggestMeal(emptySet(), 20 * 60))
        assertEquals(Meal.SNACKS, suggestMeal(emptySet(), 2 * 60))
        assertEquals(Meal.LUNCH, Meal.forHour(12))
    }
}
