package app.unavu.work

import app.unavu.data.Reminder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SchedulerTest {
    private fun at(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault())
    private fun dt(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi)

    private val dinner = Reminder("dinner", "Dinner", 21, 0, true)
    private val weigh = Reminder("weigh", "Weigh-in", 7, 0, true)
    private val water = Reminder("water", "Water", 9, 0, true)

    @Test fun laterTodayStaysToday() =
        assertEquals(dt(2026, 11, 23, 21, 0), at(Scheduler.nextTrigger(dinner, dt(2026, 11, 23, 18, 0))))

    @Test fun afterTimeRollsToTomorrow() =
        assertEquals(dt(2026, 11, 24, 21, 0), at(Scheduler.nextTrigger(dinner, dt(2026, 11, 23, 21, 0))))

    @Test fun lateNightReminderCrossesMidnightToMorning() =
        assertEquals(dt(2026, 11, 24, 7, 0), at(Scheduler.nextTrigger(weigh, dt(2026, 11, 23, 23, 50))))

    @Test fun justAfterMidnightStillFiresThisMorning() =
        assertEquals(dt(2026, 11, 23, 7, 0), at(Scheduler.nextTrigger(weigh, dt(2026, 11, 23, 0, 10))))

    @Test fun monthEndRollsOver() =
        assertEquals(dt(2026, 12, 1, 7, 0), at(Scheduler.nextTrigger(weigh, dt(2026, 11, 30, 8, 0))))

    @Test fun waterRepeatsEveryTwoHours() {
        assertEquals(dt(2026, 11, 23, 11, 0), at(Scheduler.nextTrigger(water, dt(2026, 11, 23, 9, 0))))
        assertEquals(dt(2026, 11, 23, 15, 0), at(Scheduler.nextTrigger(water, dt(2026, 11, 23, 14, 30))))
    }

    @Test fun waterStopsAtNineAndResumesTomorrow() {
        assertEquals(dt(2026, 11, 24, 9, 0), at(Scheduler.nextTrigger(water, dt(2026, 11, 23, 21, 0))))
        assertEquals(dt(2026, 11, 24, 9, 0), at(Scheduler.nextTrigger(water, dt(2026, 11, 23, 23, 30))))
    }

    @Test fun waterBeforeStartWaitsForNineToday() =
        assertEquals(dt(2026, 11, 23, 9, 0), at(Scheduler.nextTrigger(water, dt(2026, 11, 23, 6, 0))))
}
