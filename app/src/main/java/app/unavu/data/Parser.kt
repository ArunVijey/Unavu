package app.unavu.data

import kotlin.math.roundToInt

/**
 * "Type it like you say it": turns text such as "rendu idli oru vadai sambar kaapi" or
 * "3 chapathi chicken curry" into food items. Offline; understands English and Tamil number
 * words (Tanglish and Tamil script), unit words and Tanglish spellings of dishes. Words it can't
 * place are returned in [Result.unknown] so the UI can offer suggestions instead of guessing.
 *
 * Plain water ("1 glass water", "thanni", "ஒரு கிளாஸ் தண்ணீர்", "500ml") is returned separately in
 * [Result.water]: it goes to Liquids and is never a food or an unknown word.
 */
object QuickParser {

    private const val ML = "ml"
    private const val G = "g"

    /** One glass: the amount used for "water" without a unit, and for the quick-add glass tile. */
    const val GLASS_ML = 250

    data class Item(
        val food: Food,
        val qty: Double,
        val unit: String,
        val choices: Choices,
        /** Portion came from "My portions" (last time). */
        val remembered: Boolean,
        /** Typed text that produced this item. */
        val source: String,
    )

    /** Plain water found in the text, in millilitres, with the words that produced it ("2 glass water"). */
    data class Water(val ml: Int, val source: String)

    data class Result(val items: List<Item>, val unknown: List<String>, val water: List<Water> = emptyList()) {
        /** All plain water in the text, in millilitres. */
        val waterMl: Int get() = water.sumOf { it.ml }
    }

    private val numbers = mapOf(
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "oru" to 1.0, "onnu" to 1.0, "ondru" to 1.0, "ஒரு" to 1.0, "ஒன்று" to 1.0,
        "two" to 2.0, "rendu" to 2.0, "renda" to 2.0, "irandu" to 2.0, "இரண்டு" to 2.0, "ரெண்டு" to 2.0,
        "three" to 3.0, "moonu" to 3.0, "munu" to 3.0, "moondru" to 3.0, "மூன்று" to 3.0, "மூணு" to 3.0,
        "four" to 4.0, "naalu" to 4.0, "nalu" to 4.0, "naangu" to 4.0, "நான்கு" to 4.0, "நாலு" to 4.0,
        "five" to 5.0, "anju" to 5.0, "ainthu" to 5.0, "ஐந்து" to 5.0, "அஞ்சு" to 5.0,
        "six" to 6.0, "aaru" to 6.0, "ஆறு" to 6.0,
        "seven" to 7.0, "ezhu" to 7.0, "eight" to 8.0, "ettu" to 8.0, "nine" to 9.0, "ten" to 10.0, "pathu" to 10.0,
        "half" to 0.5, "arai" to 0.5, "அரை" to 0.5, "quarter" to 0.25, "kaal" to 0.25,
    )

    private val unitWords = mapOf(
        "piece" to "piece", "pieces" to "piece", "pc" to "piece", "pcs" to "piece",
        "ladle" to "ladle", "ladles" to "ladle", "karandi" to "ladle", "karandis" to "ladle",
        "cup" to "cup", "cups" to "cup", "bowl" to "bowl", "bowls" to "bowl", "kinnam" to "bowl",
        "glass" to "glass", "glasses" to "glass", "tumbler" to "tumbler", "tumblers" to "tumbler",
        "plate" to "plate", "plates" to "plate", "tbsp" to "tbsp", "spoon" to "tbsp", "spoons" to "tbsp",
        "tsp" to "tsp", "slice" to "slice", "slices" to "slice", "handful" to "handful", "scoop" to "scoop", "scoops" to "scoop",
        "roll" to "roll", "rolls" to "roll", "can" to "can", "bottle" to "bottle", "bottles" to "bottle", "mug" to "mug", "mugs" to "mug",
        // metric: every liquid has an "ml" unit and everything else a "g" unit
        "ml" to ML, "mls" to ML, "millilitre" to ML, "milliliter" to ML, "millilitres" to ML, "milliliters" to ML,
        "g" to G, "gm" to G, "gms" to G, "gram" to G, "grams" to G,
        // Tamil script
        "கிளாஸ்" to "glass", "டம்ளர்" to "tumbler", "கப்" to "cup", "கிண்ணம்" to "bowl", "கரண்டி" to "ladle",
        "பாட்டில்" to "bottle", "மில்லி" to ML, "மிலி" to ML, "கிராம்" to G,
    )

    /** Litres become millilitres ("half litre" = 500 ml). */
    private val litreWords = setOf("l", "litre", "liter", "litres", "liters", "ltr", "ltrs", "லிட்டர்")

    /** "500ml", "1.5l", "250g" typed without a space. */
    private val joinedAmount = Regex("^(\\d+(?:\\.\\d+)?)(ml|mls|l|ltr|ltrs|litre|liter|litres|liters|g|gm|gms)$")

    /** Plain water in English, Tanglish and Tamil script. */
    private val waterWords = setOf(
        "water", "paani",
        "thanni", "tanni", "thannir", "thanneer", "thaneer", "thannee", "tannir", "tanneer", "neer",
        "kudineer", "kudinir", "venneer", "vennir", "suduthanni", "sudutanni",
        "தண்ணீர்", "தண்ணி", "நீர்", "குடிநீர்", "வெந்நீர்", "சுடுதண்ணீர்", "சுடுதண்ணி",
    )

    /** Words that only describe water ("hot water", "sudu thanni"): part of the water, never unknown words. */
    private val waterModifiers = setOf(
        "hot", "warm", "lukewarm", "cold", "chilled", "cool", "plain", "drinking", "mineral", "filtered", "boiled",
        "sudu", "chudu", "kudi", "kudikka", "kulir", "சுடு", "குளிர்ந்த", "வெதுவெதுப்பான", "குடிக்க",
    )

    /** Millilitres in one unit of water. A unit not listed here counts as a glass. */
    private val waterMlPer = mapOf(
        "glass" to GLASS_ML, "tumbler" to 150, "cup" to 200, "mug" to 300, "bottle" to 500, "bowl" to 250,
        "tbsp" to 15, "tsp" to 5, "can" to 330, ML to 1, G to 1,
    )

    private val stopWords = setOf("and", "with", "um", "oda", "plus", "&", "+", "also", "konjam", "some", "of", "then", "apram", "and,")

    /** A dot inside a number ("1.5") is a decimal point; everywhere else it is punctuation, like , ; ! ? */
    private val punctuation = Regex("[,;!?]|(?<!\\d)\\.|\\.(?!\\d)")

    private var indexFor: List<Food>? = null
    private var aliasIndex: List<Pair<List<String>, Food>> = emptyList()

    /** Aliases sorted so the longest dish name wins ("egg fried rice" before "egg"). */
    private fun index(): List<Pair<List<String>, Food>> {
        val foods = FoodDb.dishes
        if (indexFor !== foods || aliasIndex.isEmpty()) {
            aliasIndex = foods.flatMap { food -> food.aliases.map { it.split(" ").filter(String::isNotBlank) to food } }
                .filter { it.first.isNotEmpty() }
                .sortedByDescending { it.first.size }
            indexFor = foods
        }
        return aliasIndex
    }

    /**
     * True when [prefix] starts a word the parser knows besides dish names (water words, units, number words), so the
     * UI doesn't flag a word that is still being typed ("thann…").
     */
    fun startsKnownWord(prefix: String): Boolean {
        val p = prefix.lowercase()
        if (p.isEmpty()) return false
        return waterWords.any { it.startsWith(p) } || waterModifiers.any { it.startsWith(p) } ||
            unitWords.keys.any { it.startsWith(p) } || litreWords.any { it.startsWith(p) } || numbers.keys.any { it.startsWith(p) }
    }

    fun parse(text: String, portions: Map<String, Portion>, liquidsOnly: Boolean = false): Result {
        val tokens = text.lowercase()
            .replace(punctuation, " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in stopWords }
        val out = mutableListOf<Item>()
        val unknown = mutableListOf<String>()
        val water = mutableListOf<Water>()
        var pendingQty: Double? = null
        var pendingUnit: String? = null
        // where the pending amount started, so a water row's source covers "2 glass water"
        var pendingFrom = -1
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val num = numberOf(t)
            val joined = joinedAmountOf(t)
            val unitWord = unitWords[t]
            when {
                num != null -> { pendingQty = num; if (pendingFrom < 0) pendingFrom = i; i++ }
                joined != null -> { pendingQty = joined.first; pendingUnit = joined.second; if (pendingFrom < 0) pendingFrom = i; i++ }
                t in litreWords -> { pendingQty = (pendingQty ?: 1.0) * 1000; pendingUnit = ML; if (pendingFrom < 0) pendingFrom = i; i++ }
                unitWord != null -> { pendingUnit = unitWord; if (pendingFrom < 0) pendingFrom = i; i++ }
                else -> {
                    val match = matchFood(tokens, i, liquidsOnly)
                    // a dish name of two or more words wins ("coconut water", "neer mor"), then water, then one-word dishes
                    val waterAt = if (match != null && match.second >= 2) null else waterWordAt(tokens, i)
                    if (waterAt != null) {
                        var end = waterAt + 1
                        var qty = pendingQty
                        var unit = pendingUnit
                        if (qty == null && unit == null) {
                            // the amount can also come after: "water 500 ml", "thanni rendu glass", "தண்ணீர் 1 லிட்டர்"
                            trailingAmount(tokens, end)?.let { (q, u, used) -> qty = q; unit = u; end += used }
                        }
                        val ml = waterMl(qty, unit)
                        val from = if (pendingFrom in 0 until i) pendingFrom else i
                        if (ml > 0) water += Water(ml, tokens.subList(from, end).joinToString(" "))
                        pendingQty = null; pendingUnit = null; pendingFrom = -1
                        i = end
                    } else if (match != null) {
                        val (food, used) = match
                        val remembered = portions[food.id]
                        val pu = pendingUnit
                        val metric = pu == ML || pu == G
                        // "200 ml milk", "100 g paneer": a liquid is measured in ml and everything else in g
                        val metricUnit = if (!metric) null
                        else food.units.firstOrNull { it.name == pu } ?: food.units.firstOrNull { it.name == ML || it.name == G }
                        val unit = metricUnit?.name
                            ?: pu?.takeIf { !metric && food.units.any { u -> u.name == pu } }
                            ?: remembered?.unit?.takeIf { ru -> food.units.any { it.name == ru } }
                            ?: food.units.first().name
                        // a metric amount for a food without a metric unit would mean "200 pieces": use the usual portion
                        val typedQty = if (metric && metricUnit == null) null else pendingQty
                        val qty = typedQty ?: remembered?.qty ?: food.defaultQty
                        out += Item(
                            food, qty, unit, choices = remembered?.choices ?: emptyMap(),
                            remembered = typedQty == null && remembered != null,
                            source = tokens.subList(i, i + used).joinToString(" "),
                        )
                        pendingQty = null; pendingUnit = null; pendingFrom = -1
                        i += used
                    } else {
                        unknown += t
                        i++
                    }
                }
            }
        }
        return Result(out, unknown, water)
    }

    /** Index of the water word when the text at [start] is "water", "hot water", "sudu thanni"…; null otherwise. */
    private fun waterWordAt(tokens: List<String>, start: Int): Int? {
        var j = start
        while (j < tokens.size && tokens[j] in waterModifiers) j++
        return j.takeIf { it < tokens.size && tokens[it] in waterWords }
    }

    /**
     * An amount right after a water word: "500 ml", "1.5l", "rendu glass", "oru bottle", or a bare number at the very
     * end ("thanni rendu"). A bare number followed by more words is left alone, so "water 2 idli" keeps the 2 for the
     * idli. Returns (quantity, unit, tokens used).
     */
    private fun trailingAmount(tokens: List<String>, j: Int): Triple<Double?, String?, Int>? {
        val t0 = tokens.getOrNull(j) ?: return null
        joinedAmountOf(t0)?.let { (q, u) -> return Triple(q, u, 1) }
        val n = numberOf(t0) ?: return null
        val t1 = tokens.getOrNull(j + 1) ?: return Triple(n, null, 1)
        if (t1 in litreWords) return Triple(n * 1000, ML, 2)
        val u = unitWords[t1] ?: return null
        return Triple(n, u, 2)
    }

    /** Millilitres for a typed amount of water. Without a unit, a big number is ml ("water 500") and a small one glasses. */
    private fun waterMl(qty: Double?, unit: String?): Int {
        val per = unit?.let { waterMlPer[it] }
        val ml = when {
            per != null -> (qty ?: 1.0) * per
            unit == null && qty != null && qty >= 50 -> qty
            else -> (qty ?: 1.0) * GLASS_ML
        }
        return ml.roundToInt().coerceAtLeast(0)
    }

    /** "500ml" -> (500, ml), "1.5l" -> (1500, ml), "250g" -> (250, g). */
    private fun joinedAmountOf(t: String): Pair<Double, String>? {
        val m = joinedAmount.matchEntire(t) ?: return null
        val n = m.groupValues[1].toDoubleOrNull() ?: return null
        val u = m.groupValues[2]
        return when {
            u in litreWords -> n * 1000 to ML
            u.startsWith("m") -> n to ML
            else -> n to G
        }
    }

    private fun numberOf(t: String): Double? {
        t.toDoubleOrNull()?.let { return it }
        if (t.matches(Regex("\\d+/\\d+"))) {
            val (a, b) = t.split("/").map { it.toDouble() }
            if (b != 0.0) return a / b
        }
        return numbers[t]
    }

    private fun matchFood(tokens: List<String>, start: Int, liquidsOnly: Boolean): Pair<Food, Int>? {
        val idx = index()
        // 1) exact multi-word / single-word alias, longest first
        for ((words, food) in idx) {
            if (liquidsOnly && !food.liquid) continue
            if (start + words.size > tokens.size) continue
            if (words.indices.all { tokens[start + it] == words[it] }) return food to words.size
        }
        // 2) typo tolerance for one word: plural "s", or 1 letter off (5+ letters). Never a different longer word.
        val t = tokens[start]
        if (t.length >= 4) {
            idx.firstOrNull { (words, food) ->
                (!liquidsOnly || food.liquid) && words.size == 1 &&
                    (t == words[0] + "s" || (t.length >= 5 && words[0].length >= 5 && editDistance(t, words[0]) <= 1))
            }?.let { return it.second to 1 }
        }
        return null
    }
}
