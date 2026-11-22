package app.unavu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.time.LocalDate

class CalcSmartTest {
    companion object {
        @BeforeClass @JvmStatic fun load() {
            val dir = File("src/main/assets")
            FoodDb.load(File(dir, "ingredients.json").readText(), File(dir, "foods.json").readText())
        }
    }

    @Test fun bmrMaleAndFemale() {
        assertEquals(1660, bmrOf(Profile(true, 28, 172.0, 72.0)))
        assertEquals(1494, bmrOf(Profile(false, 28, 172.0, 72.0)))
    }

    @Test fun targetsFollowGoal() {
        val lose = targetsFor(Profile(true, 28, 172.0, 72.0, 1.55, -500))
        val gain = targetsFor(Profile(true, 28, 172.0, 72.0, 1.55, 300))
        assertEquals(lose.maintenance, gain.maintenance)
        assertEquals(lose.maintenance - 500, lose.kcal)
        assertEquals(gain.maintenance + 300, gain.kcal)
    }

    @Test fun smartBurnOverridesFormula() {
        val t = targetsFor(Profile(goalKcal = -500), SmartTarget(appliedBurn = 2400))
        assertEquals(1900, t.kcal)
    }

    @Test fun fmtQtyWholeNumbers() {
        assertEquals("2", fmtQty(2.0))
    }

    @Test fun cheatDayFlagged() {
        val d = AppData(cheatDays = setOf("2026-11-01"))
        assertTrue(d.isCheatDay("2026-11-01"))
        assertTrue(!d.isCheatDay("2026-11-02"))
    }

    private val today = LocalDate.of(2026, 11, 23)

    @Test fun weeklyCheckWaitsSevenDays() {
        val s = SmartTarget(lastCheckDate = today.minusDays(3).toString())
        assertEquals(s, Smart.weeklyCheck(AppData(smart = s), today))
    }

    @Test fun weeklyCheckWithTooLittleDataOnlyStampsTheDate() {
        val s = SmartTarget(lastCheckDate = today.minusDays(8).toString(), appliedBurn = 2300)
        val out = Smart.weeklyCheck(AppData(smart = s), today)
        assertEquals(today.toString(), out.lastCheckDate)
        assertEquals(2300, out.appliedBurn)
        assertNull(out.pendingBurn)
    }

    @Test fun firstCheckRunsWithNoHistory() {
        assertEquals(today.toString(), Smart.weeklyCheck(AppData(), today).lastCheckDate)
    }

    @Test fun estimateNeedsFourteenWellLoggedDays() {
        val entries = (1..10).flatMap { i ->
            val day = today.minusDays(i.toLong()).toString()
            listOf(
                LogEntry(i * 2L, day, Meal.BREAKFAST, "idli", 2.0, "piece", "08:00"),
                LogEntry(i * 2L + 1, day, Meal.LUNCH, "rice", 1.0, "cup", "13:00"),
            )
        }
        assertNull(Smart.estimate(AppData(profile = Profile(), entries = entries), today))
    }

    @Test fun trendSmoothsWeights() {
        val tr = Smart.trend(listOf(WeightEntry("2026-11-01", 80.0), WeightEntry("2026-11-02", 82.0)))
        assertEquals(2, tr.size)
        assertTrue(tr[1].second > 80.0 && tr[1].second < 82.0)
    }
}
