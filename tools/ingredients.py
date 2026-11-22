"""
Ingredient table for the food database.

Most values come from IFCT 2017 (ICMR-NIN, via the MIT-licensed @ifct2017/compositions package,
values per 100 g edible portion). A few everyday items that IFCT does not cover (salt, sugar,
toned milk, curd, bread, packaged drink powders, sauces) are added by hand from USDA FoodData
Central / product labels and are marked source="custom".
"""

# Friendly key -> IFCT 2017 food code
IFCT = {
    # cereals & millets
    "rice_raw": "A015", "rice_parboiled": "A014", "rice_brown": "A013", "poha": "A011", "puffed_rice": "A012",
    "atta": "A019", "maida": "A018", "rava": "A022", "vermicelli": "A023", "ragi": "A010", "bajra": "A003",
    "jowar": "A005", "maize": "A006", "sweet_corn": "A008",
    # pulses
    "urad_dal": "B003", "toor_dal": "B021", "chana_dal": "B001", "chickpea": "B002", "moong_dal": "B010",
    "green_gram": "B011", "horse_gram": "B012", "rajma": "B020", "cowpea": "B005", "peas_dry": "B017", "soya": "B025",
    # leafy
    "amaranth": "C004", "spinach": "C033", "drumstick_leaves": "C019", "fenugreek_leaves": "C020", "cabbage": "C015",
    # vegetables
    "ash_gourd": "D001", "bitter_gourd": "D004", "bottle_gourd": "D007", "brinjal": "D031", "broad_beans": "D032",
    "cauliflower": "D036", "cluster_beans": "D039", "cucumber": "D043", "drumstick": "D046", "beans": "D049",
    "raw_jackfruit": "D051", "knol_khol": "D053", "ladies_finger": "D056", "raw_mango": "D057", "spring_onion": "D058",
    "peas": "D061", "banana_flower": "D062", "raw_banana": "D063", "banana_stem": "D064", "pumpkin": "D066",
    "snake_gourd": "D070", "tomato": "D076", "beetroot": "F001", "carrot": "F002", "potato": "F006",
    "radish": "F010", "sweet_potato": "F013", "mushroom": "J001",
    # spices & condiments
    "green_chilli": "G008", "coriander_leaves": "G009", "curry_leaves": "G010", "garlic": "G011", "ginger": "G014",
    "onion": "G017", "shallots": "G018", "asafoetida": "G019", "cardamom": "G020", "red_chilli": "G022",
    "cloves": "G023", "coriander_seeds": "G024", "cumin": "G025", "fenugreek": "G026", "pepper": "G031",
    "poppy": "G032", "turmeric": "G033",
    # nuts & seeds
    "almond": "H001", "cashew": "H005", "dry_coconut": "H006", "coconut": "H007", "groundnut": "H012",
    "mustard": "H013", "sesame": "H011",
    # sugars & misc
    "jaggery": "I001", "sugarcane_juice": "I002", "coconut_water": "K002",
    # milk, egg, meat, fish
    "milk_full": "L002", "milk_buffalo": "L001", "paneer": "L003", "khoa": "L004",
    "egg": "M001", "egg_boiled": "M004", "egg_omelette": "M007",
    "chicken": "N002", "chicken_breast": "N003", "chicken_liver": "N005",
    "mutton": "O001", "mutton_liver": "O008", "beef": "O025", "pork": "O048",
    "fish_seer": "P087", "fish_mackerel": "P034", "fish_sardine": "P071", "fish_pomfret": "P057",
    "fish_tilapia": "P083", "fish_anchovy": "P003", "fish_tuna": "P084", "prawn": "S009", "crab": "Q001", "squid": "R007",
    # fruits
    "banana": "E012", "banana_small": "E010", "mango": "E036", "orange": "E047", "lemon_juice": "E033",
    "tamarind": "E064", "apple": "E001", "grapes": "E026", "guava": "E028", "papaya": "E049", "pineapple": "E053",
    "pomegranate": "E055", "sapota": "E060", "watermelon": "E065", "dates": "E017", "raisins": "E058",
    "jackfruit": "E030", "musk_melon": "E045", "fig": "E020",
    # more
    "pistachio": "H018", "walnut": "H021", "tapioca": "F015", "yam": "F017", "colocasia": "F004", "samai": "A016", "varagu": "A017",
    "black_gram_whole": "B004", "bengal_gram_whole": "B002",
    # fats
    "coconut_oil": "T001", "gingelly_oil": "T004", "groundnut_oil": "T005", "sunflower_oil": "T012",
    "ghee": "T013", "palm_oil": "T007",
}

# Ingredients that count as cooking fat (used by the "Less oil / As usual / Extra oil" choice).
FATS = {"coconut_oil", "gingelly_oil", "groundnut_oil", "sunflower_oil", "ghee", "palm_oil", "oil", "vanaspati", "butter"}

# Ingredients whose sugar counts as ADDED sugar.
ADDED_SUGAR = {"sugar", "jaggery", "rose_syrup", "condensed_milk", "chocolate", "honey", "jam", "syrup", "ketchup", "cola", "boost_powder", "horlicks_powder", "ice_cream", "biscuit"}

# Hand-added items: kcal, protein, carbs, fat, fibre, sodium mg, iron mg, calcium mg, potassium mg,
# vitamin C mg, vitamin A µg, sat fat g, sugar g (total sugar of the item)
def C(kcal, p, c, f, fib=0, na=0, fe=0, ca=0, k=0, vc=0, va=0, sat=0, sugar=0, name="", note=""):
    return dict(kcal=kcal, protein=p, carbs=c, fat=f, fibre=fib, sodium=na, iron=fe, calcium=ca, potassium=k,
                vitc=vc, vita=va, satfat=sat, sugar=sugar, name=name, note=note)

CUSTOM = {
    "water": C(0, 0, 0, 0, name="Water"),
    "salt": C(0, 0, 0, 0, na=38758, name="Salt (table)", note="USDA"),
    "sugar": C(387, 0, 100, 0, sugar=100, name="Sugar (white)", note="USDA"),
    "oil": C(884, 0, 0, 100, sat=14, name="Cooking oil (refined, generic)", note="USDA"),
    "vanaspati": C(884, 0, 0, 100, sat=45, name="Vanaspati / bakery fat", note="USDA shortening"),
    "butter": C(717, 0.9, 0.1, 81, na=580, ca=24, va=684, sat=51, name="Butter (salted)", note="USDA"),
    "milk_toned": C(58, 3.1, 4.7, 3.0, na=45, ca=120, k=150, sat=1.9, sugar=4.7, name="Milk, toned (3% fat)", note="Aavin/Amul label"),
    "curd": C(60, 3.1, 4.4, 3.2, na=46, ca=121, k=155, sat=2.0, sugar=4.4, name="Curd (toned milk)", note="USDA yogurt, adjusted"),
    "bread": C(265, 9.0, 49, 3.2, 2.7, 490, 3.6, 150, 115, sat=0.7, sugar=5, name="White bread", note="USDA"),
    "bun": C(280, 8.5, 50, 4.5, 2.3, 450, 3.0, 100, 110, sat=1.2, sugar=8, name="Burger bun", note="USDA"),
    "biscuit": C(440, 7, 75, 12, 2, 350, 2, 30, 120, sat=6, sugar=22, name="Biscuit (Marie-type)", note="label"),
    "boost_powder": C(377, 7.5, 84, 1.5, 3, 300, 20, 300, 500, vc=60, sat=0.8, sugar=40, name="Boost powder", note="label estimate"),
    "horlicks_powder": C(377, 11, 78, 2.0, 3, 300, 9, 500, 600, vc=40, sat=1.0, sugar=25, name="Horlicks powder", note="label estimate"),
    "rose_syrup": C(270, 0, 68, 0, sugar=68, name="Rose syrup", note="label estimate"),
    "ice_cream": C(207, 3.5, 24, 11, 0.7, 80, 0.1, 128, 200, sat=6.8, sugar=21, name="Ice cream (vanilla)", note="USDA"),
    "cola": C(42, 0, 10.6, 0, na=4, sugar=10.6, name="Cola / soft drink", note="USDA"),
    "soy_sauce": C(53, 8, 5, 0.6, 0.8, 5493, 1.5, 33, 435, name="Soy sauce", note="USDA"),
    "ketchup": C(101, 1.0, 27, 0.1, 0.3, 907, 0.4, 15, 280, vc=4, sugar=22, name="Tomato ketchup", note="USDA"),
    "mayonnaise": C(680, 1.0, 0.6, 75, 0, 635, 0.2, 8, 20, sat=12, name="Mayonnaise", note="USDA"),
    "cheese": C(311, 20, 2, 25, 0, 1200, 0.2, 600, 120, sat=16, name="Processed cheese", note="label estimate"),
    "cornflour": C(381, 0.3, 91, 0.1, 0.9, 9, 0.5, 2, 3, name="Corn flour", note="USDA"),
    "noodles_dry": C(350, 10, 72, 1.5, 3, 20, 1.2, 20, 120, name="Noodles, dry (maida)", note="label estimate"),
    "mint": C(70, 3.8, 15, 0.9, 8, 31, 5, 243, 569, vc=32, va=212, name="Mint leaves", note="USDA"),
    "fennel": C(345, 15.8, 52, 14.9, 40, 88, 18.5, 1196, 1694, name="Fennel seeds", note="USDA"),
    "cinnamon": C(247, 4, 81, 1.2, 53, 10, 8.3, 1002, 431, name="Cinnamon", note="USDA"),
    "coffee_decoction": C(2, 0.1, 0, 0, na=2, k=50, name="Coffee decoction", note="USDA brewed coffee"),
    "tea_decoction": C(1, 0, 0.3, 0, na=3, k=37, name="Tea decoction", note="USDA brewed tea"),
    "coffee_instant": C(353, 12.2, 75.4, 0.5, 0, 37, 4.4, 141, 3535, name="Instant coffee powder (Nescafé / Bru)", note="USDA"),
    "cocoa": C(228, 19.6, 21, 13.7, 37, 21, 13.9, 128, 1524, sat=8.1, name="Cocoa powder", note="USDA"),
    "condensed_milk": C(321, 7.9, 54.4, 8.7, 0, 127, 0.2, 284, 371, vc=2.6, va=74, sat=5.5, sugar=54.4, name="Condensed milk (Milkmaid)", note="USDA"),
    "cream": C(241, 2.0, 3.0, 25, 0, 40, 0.1, 70, 95, va=200, sat=16, sugar=3, name="Fresh cream (25%)", note="Amul label"),
    "chocolate": C(535, 7.6, 59.4, 29.7, 3.4, 79, 2.35, 189, 372, va=50, sat=18.5, sugar=51.5, name="Milk chocolate", note="USDA"),
    "honey": C(304, 0.3, 82.4, 0, 0.2, 4, 0.4, 6, 52, sugar=82, name="Honey", note="USDA"),
    "jam": C(278, 0.4, 69, 0.1, 1.1, 32, 0.5, 20, 77, vc=9, sugar=48.5, name="Fruit jam", note="USDA"),
    "syrup": C(280, 0, 70, 0, na=10, sugar=70, name="Sarbath / flavour syrup (nannari, chocolate)", note="label estimate"),
    "sago": C(358, 0.2, 88.7, 0, 0.9, 1, 1.6, 20, 11, name="Sago (javvarisi)", note="USDA tapioca pearls"),
    "rice_flour": C(366, 6.0, 80, 1.4, 2.4, 0, 0.35, 10, 76, name="Rice flour", note="USDA"),
    "milk_powder": C(496, 26.3, 38.4, 26.7, 0, 371, 0.5, 912, 1330, vc=8.6, va=258, sat=16.7, sugar=38.4, name="Milk powder (whole)", note="USDA"),
    "soda": C(0, 0, 0, 0, na=20, name="Soda water", note="label"),
    "corn_kernels": C(86, 3.3, 17, 1.4, 2.4, 15, 0.5, 2, 270, vc=7, va=9, sugar=6, name="Sweet corn kernels", note="USDA"),
    "popcorn_kernels": C(375, 11, 64, 4.5, 13, 8, 3.2, 7, 274, name="Popcorn kernels", note="USDA"),
    "sev": C(560, 16, 45, 35, 9, 650, 4.5, 50, 600, sat=8, name="Sev / omapodi (fried besan)", note="label estimate"),
    "peanut_butter": C(588, 25.1, 13.6, 50.4, 6.0, 459, 1.9, 49, 649, sat=10.1, name="Peanut butter (regular, sweetened & salted)", note="USDA"),
    "peanut_butter_natural": C(600, 25.0, 12.0, 51.0, 8.0, 5, 1.9, 45, 650, sat=8.5, name="Peanut butter (natural, 100% peanuts)", note="USDA / label"),
    "papdi": C(500, 8, 58, 26, 2, 600, 2, 20, 100, sat=11, name="Papdi / puri (fried maida)", note="label estimate"),
}
