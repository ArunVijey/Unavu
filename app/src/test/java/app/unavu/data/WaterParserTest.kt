package app.unavu.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Round 7 #7: plain water typed in quick add goes to Liquids (ml), in English, Tanglish and Tamil script. */
class WaterParserTest {
    companion object {
        @BeforeClass @JvmStatic fun load() {
            val dir = File("src/main/assets")
            FoodDb.load(File(dir, "ingredients.json").readText(), File(dir, "foods.json").readText())
        }
    }

    private fun parse(text: String, liquidsOnly: Boolean = false) = QuickParser.parse(text, emptyMap(), liquidsOnly)

    /** The water in [text] in ml, checking that nothing became a food or an unknown word. */
    private fun waterOnly(text: String, liquidsOnly: Boolean = false): Int {
        val r = parse(text, liquidsOnly)
        assertTrue("no foods in [$text]: ${r.items.map { it.food.id }}", r.items.isEmpty())
        assertTrue("no unknown words in [$text]: ${r.unknown}", r.unknown.isEmpty())
        return r.waterMl
    }

    private fun foods(r: QuickParser.Result) = r.items.map { "${it.food.id}:${fmtQty(it.qty)}:${it.unit}" }

    @Test fun englishWithoutAmountIsOneGlass() {
        assertEquals(250, waterOnly("water"))
        assertEquals(250, waterOnly("Water"))
        assertEquals(250, waterOnly("water", liquidsOnly = true))
        assertEquals(QuickParser.GLASS_ML, waterOnly("water"))
    }

    @Test fun englishAmountsAndUnits() {
        assertEquals(250, waterOnly("1 glass water"))
        assertEquals(500, waterOnly("2 glasses of water"))
        assertEquals(500, waterOnly("a bottle of water"))
        assertEquals(300, waterOnly("2 tumblers water"))
        assertEquals(200, waterOnly("1 cup water"))
        assertEquals(500, waterOnly("500 ml water"))
        assertEquals(500, waterOnly("500ml water"))
        assertEquals(1500, waterOnly("1.5 litre water"))
        assertEquals(1500, waterOnly("1.5L water"))
        assertEquals(500, waterOnly("half litre water"))
        assertEquals(1000, waterOnly("1 l water"))
    }

    @Test fun amountAfterTheWord() {
        assertEquals(500, waterOnly("water 500 ml"))
        assertEquals(750, waterOnly("water 750ml"))
        assertEquals(1500, waterOnly("water 1.5 l"))
        assertEquals(500, waterOnly("water 2 glass"))
        // a bare number at the end: small = glasses, large = ml
        assertEquals(500, waterOnly("water 2"))
        assertEquals(400, waterOnly("water 400"))
        assertEquals(400, waterOnly("400 water"))
    }

    @Test fun describingWordsBelongToTheWater() {
        assertEquals(250, waterOnly("hot water"))
        assertEquals(500, waterOnly("2 glass warm water"))
        assertEquals(250, waterOnly("drinking water"))
    }

    @Test fun tanglish() {
        assertEquals(250, waterOnly("thanni"))
        assertEquals(250, waterOnly("tanni"))
        assertEquals(500, waterOnly("rendu glass thanni"))
        assertEquals(300, waterOnly("thanni rendu tumbler"))
        assertEquals(500, waterOnly("oru bottle thanni"))
        assertEquals(250, waterOnly("sudu thanni"))
        assertEquals(1000, waterOnly("kudineer 1 litre"))
        assertEquals(500, waterOnly("thanni rendu"))
        assertEquals(250, waterOnly("neer"))
    }

    @Test fun tamilScript() {
        assertEquals(250, waterOnly("தண்ணீர்"))
        assertEquals(250, waterOnly("தண்ணி"))
        assertEquals(250, waterOnly("ஒரு கிளாஸ் தண்ணீர்"))
        assertEquals(300, waterOnly("இரண்டு டம்ளர் தண்ணீர்"))
        assertEquals(500, waterOnly("அரை லிட்டர் தண்ணீர்"))
        assertEquals(500, waterOnly("தண்ணீர் 2 கிளாஸ்"))
        assertEquals(250, waterOnly("சுடு தண்ணீர்"))
    }

    @Test fun waterNextToFoodsAndDrinks() {
        // the review case: "oru kaapi 1 glass water" in Liquid mode
        val liquid = parse("oru kaapi 1 glass water", liquidsOnly = true)
        assertEquals(listOf("filter_coffee:1:tumbler"), foods(liquid))
        assertEquals(250, liquid.waterMl)
        assertTrue(liquid.unknown.isEmpty())

        val meal = parse("rendu idli sambar thanni")
        assertEquals(listOf("idli:2:piece", "sambar:1:ladle"), foods(meal))
        assertEquals(250, meal.waterMl)

        // a number after "water" that belongs to the next food stays with that food
        val next = parse("water 2 idli")
        assertEquals(listOf("idli:2:piece"), foods(next))
        assertEquals(250, next.waterMl)
    }

    @Test fun severalWaterMentionsAddUp() {
        val r = parse("1 glass water, kaapi, 500ml thanni")
        assertEquals(listOf(250, 500), r.water.map { it.ml })
        assertEquals(750, r.waterMl)
        assertEquals(listOf("1 glass water", "500ml thanni"), r.water.map { it.source })
    }

    @Test fun dishesNamedWithWaterStayFoods() {
        val coconut = parse("coconut water")
        assertEquals(listOf("tender_coconut"), coconut.items.map { it.food.id })
        assertEquals(0, coconut.waterMl)
        val mor = parse("neer mor")
        assertEquals(listOf("buttermilk"), mor.items.map { it.food.id })
        assertEquals(0, mor.waterMl)
    }

    @Test fun waterIsNeverAnUnknownWord() {
        for (t in listOf("water", "thanni", "தண்ணீர்", "1 glass water", "hot water")) {
            assertTrue("[$t]", parse(t).unknown.isEmpty())
            assertTrue("[$t]", parse(t, liquidsOnly = true).unknown.isEmpty())
        }
    }

    @Test fun decimalsAndMetricAmountsForFoods() {
        assertEquals(listOf("rice:1.5:cup"), foods(parse("1.5 cup rice")))
        assertEquals(listOf("milk:200:ml"), foods(parse("200 ml milk")))
        assertEquals(listOf("milk:500:ml"), foods(parse("half litre milk")))
        // a full stop still ends a phrase
        assertEquals(listOf("idli:2:piece", "medu_vada:1:piece"), foods(parse("2 idli. 1 vadai.")))
    }

    @Test fun wordsBeingTypedAreRecognised() {
        assertTrue(QuickParser.startsKnownWord("thann"))
        assertTrue(QuickParser.startsKnownWord("wat"))
        assertTrue(QuickParser.startsKnownWord("தண்ண"))
        assertTrue(QuickParser.startsKnownWord("litr"))
        assertFalse(QuickParser.startsKnownWord("qqqz"))
        assertFalse(QuickParser.startsKnownWord(""))
    }
}
