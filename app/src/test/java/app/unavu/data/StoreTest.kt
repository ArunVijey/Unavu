package app.unavu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun file() = File(tmp.root, "unavu-data.json")
    private fun entry(id: Long, date: String = "2026-11-01") = LogEntry(id, date, Meal.LUNCH, "idli", 2.0, "piece", "12:00")

    @Test fun corruptFileIsKeptAndReported() {
        val f = file(); f.writeText("{ this is not json")
        val store = Store.forFile(f)
        assertTrue(store.data.value.entries.isEmpty())
        val issue = store.loadIssue.value
        assertNotNull(issue)
        assertEquals("{ this is not json", File(issue!!.keptCopyPath).readText())
        assertTrue(File(issue.keptCopyPath).name.contains(".corrupt-"))
        // the unreadable original is not replaced until something is saved
        assertEquals("{ this is not json", f.readText())
        store.clearLoadIssue()
        assertNull(store.loadIssue.value)
    }

    @Test fun corruptFileFallsBackToTmp() {
        val good = AppData(entries = listOf(entry(5)))
        File(tmp.root, "unavu-data.json.tmp").writeText(Store.JSON.encodeToString(AppData.serializer(), good))
        val f = file(); f.writeText("garbage")
        val store = Store.forFile(f)
        assertEquals(listOf(5L), store.data.value.entries.map { it.id })
        assertNotNull(store.loadIssue.value)
    }

    @Test fun cleanStartHasNoIssue() {
        val store = Store.forFile(file())
        assertNull(store.loadIssue.value)
        assertTrue(store.data.value.entries.isEmpty())
    }

    @Test fun atomicRoundTrip() {
        val f = file()
        val store = Store.forFile(f)
        store.update { it.copy(entries = listOf(entry(1), entry(2)), water = mapOf("2026-11-01" to 500)) }
        store.flush()
        assertFalse(File(tmp.root, "unavu-data.json.tmp").exists())
        val again = Store.forFile(f)
        assertEquals(store.data.value, again.data.value)
        assertEquals(3L, again.newId())
    }

    @Test fun migrationV1ToV2() {
        val v1 = AppData(
            version = 1,
            settings = Settings(waterGoalMl = 2000),
            entries = listOf(entry(1).copy(foodId = "dal"), entry(2).copy(foodId = "idli")),
            portions = mapOf("noodles" to Portion(1.0, "plate")),
        )
        val m = Store.migrate(v1)
        assertEquals(2, m.version)
        assertEquals(2500, m.settings.waterGoalMl)
        assertEquals(listOf("dal_fry", "idli"), m.entries.map { it.foodId })
        assertEquals(setOf("veg_noodles"), m.portions.keys)
        assertEquals(Store.DEFAULT_REMINDERS, m.reminders)
        // a custom water goal is kept
        assertEquals(3000, Store.migrate(AppData(version = 1, settings = Settings(waterGoalMl = 3000))).settings.waterGoalMl)
    }

    @Test fun oldJsonWithoutNewFieldsStillLoads() {
        val f = file()
        f.writeText("""{"version":2,"entries":[{"id":4,"date":"2026-11-02","meal":"DINNER","foodId":"dosa","qty":1.0,"unit":"piece","time":"20:00"}],"futureField":1}""")
        val store = Store.forFile(f)
        assertNull(store.loadIssue.value)
        assertEquals(1, store.data.value.entries.size)
        assertFalse(store.data.value.entries[0].cheat)
    }

    @Test fun lastWriteWinsAfterManyUpdates() {
        val f = file()
        val store = Store.forFile(f)
        repeat(1000) { i -> store.update { it.copy(water = mapOf("2026-11-01" to i)) } }
        store.flush()
        assertEquals(999, Store.forFile(f).data.value.water["2026-11-01"])
        // the background writer also ends on the newest snapshot
        Thread.sleep(300)
        assertEquals(999, Store.forFile(f).data.value.water["2026-11-01"])
    }

    @Test fun restoreSnapshotPutsDataBackAndKeepsIdsUnique() {
        val store = Store.forFile(file())
        store.logItems(listOf(NewItem("idli", 2.0, "piece")), Meal.BREAKFAST, "2026-11-01", false)
        val before = store.data.value
        val deletedId = before.entries.single().id
        store.deleteEntry(deletedId)
        assertTrue(store.data.value.entries.isEmpty())
        store.restoreSnapshot(before)
        assertEquals(before, store.data.value)
        assertTrue(store.newId() > deletedId)
    }

    @Test fun useComboLogsItemsAndCountsUseInOneUpdate() {
        val store = Store.forFile(file())
        val combo = Combo("c1", "Usual", items = listOf(ComboItem("idli", 2.0, "piece"), ComboItem("sambar", 1.0, "ladle")))
        store.update { it.copy(combos = listOf(combo)) }
        store.useCombo(combo, Meal.BREAKFAST, "2026-11-01")
        val d = store.data.value
        assertEquals(2, d.entries.size)
        assertEquals(1, d.combos.single().uses)
        assertEquals(setOf("idli", "sambar"), d.portions.keys)
    }

    @Test fun entriesOnAndBetween() {
        val d = AppData(entries = listOf(entry(1, "2026-11-03"), entry(2, "2026-11-01"), entry(3, "2026-11-01"), entry(4, "2026-11-10")))
        assertEquals(listOf(2L, 3L), d.entriesOn("2026-11-01").map { it.id })
        assertTrue(d.entriesOn("2026-12-01").isEmpty())
        assertEquals(listOf(2L, 3L, 1L), d.entriesBetween("2026-11-01", "2026-11-03").map { it.id })
        assertTrue(d.entriesBetween("2026-11-05", "2026-11-01").isEmpty())
        assertEquals(4, d.entriesBetween("2026-01-01", "2026-12-31").size)
    }
}
