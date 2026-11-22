package app.unavu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

class ParserTest {
    companion object {
        @BeforeClass @JvmStatic fun load() {
            val dir = File("src/main/assets")
            FoodDb.load(File(dir, "ingredients.json").readText(), File(dir, "foods.json").readText())
        }
    }

    private fun p(text: String, portions: Map<String, Portion> = emptyMap()) =
        QuickParser.parse(text, portions).items.map { "${it.food.id}:${fmtQty(it.qty)}:${it.unit}" }

    @Test fun tanglishNumbers() =
        assertEquals(listOf("idli:2:piece", "medu_vada:1:piece", "sambar:1:ladle", "filter_coffee:1:tumbler"), p("rendu idli oru vadai sambar kaapi"))

    @Test fun vadakariIsVadaCurry() =
        assertEquals(listOf("poori:4:piece", "vada_curry:1:ladle"), p("4 poori vadakari"))

    @Test fun unknownWordsAreReportedNotGuessed() {
        val r = QuickParser.parse("2 idli xyzabc", emptyMap())
        assertEquals(listOf("idli"), r.items.map { it.food.id })
        assertEquals(listOf("xyzabc"), r.unknown)
    }

    @Test fun digitsAndMultiWord() =
        assertEquals(listOf("chapathi:3:piece", "chicken_curry:1:cup"), p("3 chapathi chicken curry"))

    @Test fun longestMatchWins() =
        assertEquals(listOf("egg_fried_rice:1:plate", "egg_puff:1:piece", "egg_boiled:2:piece"), p("egg fried rice, egg puff and 2 eggs"))

    @Test fun halfAndUnits() =
        assertEquals(listOf("parotta:½:piece", "salna:2:ladle"), p("half parotta 2 karandi salna"))

    @Test fun tamilScript() =
        assertEquals(listOf("idli:3:piece", "sambar:1:ladle"), p("மூன்று இட்லி சாம்பார்"))

    @Test fun typoTolerance() = assertEquals(listOf("chapathi:2:piece"), p("2 chapathy"))

    @Test fun rememberedPortionAndChoices() {
        val portions = mapOf(
            "sambar" to Portion(2.0, "ladle", mapOf("veg" to listOf("carrot", "nookal"))),
            "rice" to Portion(1.5, "cup"),
        )
        val items = QuickParser.parse("rice sambar", portions).items
        assertEquals(listOf("rice:1.5:cup", "sambar:2:ladle"), items.map { "${it.food.id}:${fmtQty(it.qty)}:${it.unit}" })
        assertTrue(items.all { it.remembered })
        assertEquals(listOf("carrot", "nookal"), items[1].choices["veg"])
    }

    @Test fun choicesChangeNutrition() {
        val coffee = FoodDb.get("filter_coffee")!!
        val noSugar = coffee.nutrFor(1.0, "tumbler", mapOf("sugar" to listOf("0"))).kcal
        val twoSugar = coffee.nutrFor(1.0, "tumbler", mapOf("sugar" to listOf("2"))).kcal
        assertTrue("2 tsp sugar adds ~33 kcal", abs(twoSugar - noSugar - 32.5) < 2)
        val dosa = FoodDb.get("dosa")!!
        assertTrue(dosa.nutrFor(1.0, "piece", mapOf(OIL to listOf("extra"))).kcal > dosa.nutrFor(1.0, "piece", mapOf(OIL to listOf("less"))).kcal + 15)
    }

    @Test fun sensiblePortions() {
        fun kcal(id: String, q: Double = 1.0) = FoodDb.get(id)!!.let { it.nutrFor(q, it.units.first().name) }.kcal.roundToInt()
        assertTrue(kcal("idli") in 40..65)
        assertTrue(kcal("dosa") in 120..190)
        assertTrue(kcal("sambar") in 40..80)
        assertTrue(kcal("rice", 1.5) in 250..320)
        assertTrue(kcal("chicken_biryani") in 500..700)
    }

    @Test fun targets() {
        val t = targetsFor(Profile(true, 28, 172.0, 72.0, 1.55, -500))
        assertEquals(1660, t.bmr); assertEquals(2573, t.maintenance); assertEquals(2073, t.kcal)
    }
}
