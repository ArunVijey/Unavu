# Unavu (உணவு)

A calorie and nutrient tracker built for **Tamil Nadu food**: idli, dosa, sambar, kuzhambu, biryani, parotta,
bakery puffs, filter coffee, sweets, savouries and common fast food. Android only, works fully offline,
no account, no ads, no tracking. Your data stays on your phone.

## Features

- **Your targets.** Enter age, height, weight, activity and goal; the app works out calories (Mifflin-St Jeor)
  and nutrients (ICMR-NIN 2020) and shows each as a bar.
- **Build a plate.** Tap tiles to add items (tap again for more), add sides from "Goes well with",
  customise each item (sambar vegetables, oil level, milk and sugar in coffee…), save plates as combos.
- **Type it like you say it.** `rendu idli oru vadai sambar` → parsed and ready to log (Tamil, English and Tanglish).
- **~260 dishes** calculated from the IFCT 2017 food tables, plus every IFCT ingredient by weight.
- **My foods.** Add anything new with its calories / protein / fat (per serving or per 100 g from a label).
- **My recipes.** Build a home dish from its ingredients.
- **Liquids, weight trend, history calendar, cheat meals / days.**
- **Insights.** Weekly and monthly charts, nutrient gaps, meal timing, monthly PDF report.
- **Smart target.** Re-estimates how much you burn from your logs and weight trend.
- **Widgets and Quick Settings tiles**, reminders, daily backup to a folder you choose.
- Light / dark / pure black themes, accent colour picker, Tamil text throughout.
- **Sketch style**: a hand-drawn, Excalidraw-like look (Excalifont, sketchy outlines, dotted paper).

## Install (for friends)

1. Open the **[Releases](../../releases)** page on your Android phone and download the latest `Unavu-x.y.z.apk`.
2. Open the downloaded file. If Android asks, allow your browser / Files app to **install unknown apps**.
3. Tap **Install**. Needs Android 8.0 or newer.

Play Protect may show a warning because the app isn't from the Play Store. Choose **Install anyway**.
To update later, install the newer APK from Releases over the old one; your data is kept.

## Build from source

Requirements: Android Studio (JDK 17+) and the Android SDK (compileSdk 37).

```sh
./gradlew :app:assembleDebug        # debug build
./gradlew :app:assembleRelease      # optimised build
```

Release builds are signed with the key described in `../signing/keystore.properties` if that file exists
(keys are never committed); otherwise they fall back to the debug key.

### Food database

`tools/` holds the data pipeline. It turns IFCT 2017 ingredient values and the recipes in `recipes.py` /
`recipes_more.py` into `app/src/main/assets/foods.json` and `ingredients.json`:

```sh
python -I tools/build_foods.py
```

Each dish is a typical home / shop recipe (raw ingredient grams + cooked weight). Values are estimates,
so a specific hotel or brand can differ.

## Data sources

- **IFCT 2017**, Indian Food Composition Tables, ICMR-National Institute of Nutrition, via the
  MIT-licensed [@ifct2017/compositions](https://github.com/ifct2017/compositions) package (see `tools/ifct2017_LICENSE.txt`).
- A few items not in IFCT (instant coffee, cocoa, condensed milk, honey, bread, sauces, packaged drink powders…)
  from USDA FoodData Central or product labels, marked `custom` in `tools/ingredients.py`.
- Targets: ICMR-NIN Nutrient Requirements for Indians (2020).
- Fonts: Google Sans Flex, Inter, Noto Sans Tamil and Excalifont (by Excalidraw, used in the Sketch style), all under the SIL Open Font License.

This app is for general information only and is not medical advice.
