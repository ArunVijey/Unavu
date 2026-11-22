package app.unavu.data

/**
 * "Type it like you say it": turns text such as "rendu idli oru vadai sambar kaapi" or
 * "3 chapathi chicken curry" into food items. Offline; understands English and Tamil number
 * words (Tanglish and Tamil script), unit words and Tanglish spellings of dishes. Words it can't
 * place are returned in [Result.unknown] so the UI can offer suggestions instead of guessing.
 */
object QuickParser {

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

    data class Result(val items: List<Item>, val unknown: List<String>)

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
        "roll" to "roll", "rolls" to "roll", "can" to "can",
    )

    private val stopWords = setOf("and", "with", "um", "oda", "plus", "&", "+", "also", "konjam", "some", "of", "then", "apram", "and,")

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

    fun parse(text: String, portions: Map<String, Portion>, liquidsOnly: Boolean = false): Result {
        val tokens = text.lowercase()
            .replace(Regex("[,;.!?]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in stopWords }
        val out = mutableListOf<Item>()
        val unknown = mutableListOf<String>()
        var pendingQty: Double? = null
        var pendingUnit: String? = null
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val num = numberOf(t)
            val unitWord = unitWords[t]
            when {
                num != null -> { pendingQty = num; i++ }
                unitWord != null -> { pendingUnit = unitWord; i++ }
                else -> {
                    val match = matchFood(tokens, i, liquidsOnly)
                    if (match != null) {
                        val (food, used) = match
                        val remembered = portions[food.id]
                        val explicit = pendingQty != null
                        val pu = pendingUnit
                        val unit = pu?.takeIf { food.units.any { u -> u.name == pu } }
                            ?: remembered?.unit?.takeIf { ru -> food.units.any { it.name == ru } }
                            ?: food.units.first().name
                        val qty = pendingQty ?: remembered?.qty ?: food.defaultQty
                        out += Item(
                            food, qty, unit, choices = remembered?.choices ?: emptyMap(),
                            remembered = !explicit && remembered != null,
                            source = tokens.subList(i, i + used).joinToString(" "),
                        )
                        pendingQty = null; pendingUnit = null
                        i += used
                    } else {
                        unknown += t
                        i++
                    }
                }
            }
        }
        return Result(out, unknown)
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
