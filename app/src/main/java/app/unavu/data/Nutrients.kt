package app.unavu.data

/** A nutrient shown as a chip/bar: how to read it from [Nutr] and its daily target or limit. */
data class NutrientDef(
    val key: String,
    val name: String,
    val tamil: String,
    val unit: String,
    /** true = keep under (sodium, sugar…); false = get enough (protein, fibre…). */
    val limit: Boolean,
    val value: (Nutr) -> Double,
    val target: (Targets) -> Double,
    val tip: String,
)

val NUTRIENTS = listOf(
    NutrientDef("carbs", "Carbs", "மாவுச்சத்து", "g", true, { it.carbs }, { it.carbs.toDouble() },
        "Rice, idli and dosa are the main sources. On heavy days swap half the rice for extra poriyal or kootu."),
    NutrientDef("protein", "Protein", "புரதம்", "g", false, { it.protein }, { it.protein.toDouble() },
        "Add an egg, a cup of sundal, curd, dal or a piece of chicken / fish to close the gap."),
    NutrientDef("fat", "Fat", "கொழுப்பு", "g", true, { it.fat }, { it.fat.toDouble() },
        "Hotel food, ghee roast and fried snacks add the most. Pick “Less oil” where you can."),
    NutrientDef("sodium", "Sodium", "உப்பு", "mg", true, { it.sodium }, { it.sodium },
        "Pickle, appalam, chips, sambar/rasam refills and hotel gravies are the big salt sources."),
    NutrientDef("fibre", "Fibre", "நார்ச்சத்து", "g", false, { it.fibre }, { it.fibre.toDouble() },
        "Add a poriyal, keerai, sundal, a fruit or a cucumber-carrot salad."),
    NutrientDef("iron", "Iron", "இரும்புச்சத்து", "mg", false, { it.iron }, { it.iron },
        "Keerai, dal, sundal, ragi, egg and meat help. Lemon / vitamin C with meals helps absorption."),
    NutrientDef("calcium", "Calcium", "கால்சியம்", "mg", false, { it.calcium }, { it.calcium },
        "Curd, milk, buttermilk, ragi, keerai and sesame (ellu) are good sources."),
    NutrientDef("sugar", "Added sugar", "சர்க்கரை", "g", true, { it.sugar }, { it.sugar },
        "Coffee/tea sugar, sweets, juices and soft drinks add up. Try ½ tsp in coffee."),
    NutrientDef("satfat", "Saturated fat", "நிறைவுற்ற கொழுப்பு", "g", true, { it.satFat }, { it.satFat },
        "Ghee, coconut, butter, bakery puffs and fried foods are the main sources."),
    NutrientDef("vitc", "Vitamin C", "வைட்டமின் சி", "mg", false, { it.vitC }, { it.vitC },
        "Guava, orange, lemon, tomato, amla and raw salads."),
    NutrientDef("potassium", "Potassium", "பொட்டாசியம்", "mg", false, { it.potassium }, { it.potassium },
        "Banana, tender coconut, potato, dal, keerai and curd."),
)

fun nutrient(key: String) = NUTRIENTS.first { it.key == key }
