package app.unavu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class ParserEdgeTest {
    companion object {
        @BeforeClass @JvmStatic fun load() {
            val dir = File("src/main/assets")
            FoodDb.load(File(dir, "ingredients.json").readText(), File(dir, "foods.json").readText())
        }
    }

    private fun parse(t: String) = QuickParser.parse(t, emptyMap())

    @Test fun emptyAndBlankInputGiveNothing() {
        for (t in listOf("", "   ", "\n\t", ",,,")) {
            val r = parse(t)
            assertTrue("input [$t]", r.items.isEmpty() && r.unknown.isEmpty())
        }
    }

    @Test fun numbersAloneAreNotFoods() = assertTrue(parse("2 3 4").items.isEmpty())

    @Test fun upperCaseAndPunctuation() =
        assertEquals(listOf("idli"), parse("2 IDLI!!").items.map { it.food.id })

    @Test fun unknownOnlyIsReported() {
        val r = parse("qqqzzz")
        assertTrue(r.items.isEmpty())
        assertEquals(listOf("qqqzzz"), r.unknown)
    }

    @Test fun veryLongInputDoesNotCrash() {
        val r = parse("2 idli, ".repeat(300))
        assertTrue(r.items.size >= 300)
    }
}
