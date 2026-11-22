"""
Builds the app's food database from IFCT 2017 + recipes.

  python -I tools/build_foods.py            (run from the Unavu folder)

Writes app/src/main/assets/ingredients.json and app/src/main/assets/foods.json and prints a
per-portion table so the numbers can be eyeballed.
"""
import csv
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from ingredients import IFCT, CUSTOM, FATS, ADDED_SUGAR  # noqa: E402
from recipes import RECIPES as BASE  # noqa: E402
from recipes_more import MORE  # noqa: E402

RECIPES = BASE + MORE
_dupes = {r["id"] for r in RECIPES if [x["id"] for x in RECIPES].count(r["id"]) > 1}
if _dupes:
    raise SystemExit(f"duplicate dish ids: {sorted(_dupes)}")

ASSETS = os.path.join(HERE, "..", "app", "src", "main", "assets")
# vector order used by the app
KEYS = ["kcal", "protein", "carbs", "fat", "fibre", "sodium", "iron", "calcium", "potassium", "vitc", "vita", "satfat", "sugar"]


def num(x):
    try:
        return float(x)
    except (TypeError, ValueError):
        return 0.0


def load_ifct():
    rows = list(csv.reader(open(os.path.join(HERE, "ifct2017_compositions.csv"), encoding="utf-8")))
    head = [h.split("; ")[-1] for h in rows[0]]
    col = {k: head.index(k) for k in head}
    out = {}
    for r in rows[1:]:
        g = lambda k: num(r[col[k]]) if k in col else 0.0  # noqa: E731
        p, c, f, fib = g("protcnt"), g("choavldf"), g("fatce"), g("fibtg")
        tamil = ""
        for part in r[col["lang"]].split(";"):
            part = part.strip()
            if part.startswith("Tam."):
                tamil = part[4:].strip().rstrip(".")
        out[r[col["code"]]] = dict(
            name=r[col["name"]], group=r[col["grup"]], tamil=tamil,
            # Energy from macros (IFCT factors 17/17/37/8 kJ ≈ 4/4/9/2 kcal); some raw enerc cells are 0/odd.
            kcal=round(4 * p + 4 * c + 9 * f + 2 * fib, 1), protein=p, carbs=c, fat=f, fibre=fib,
            sodium=g("na") * 1000, iron=g("fe") * 1000, calcium=g("ca") * 1000, potassium=g("k") * 1000,
            vitc=g("vitc") * 1000, vita=g("vita") * 1e6, satfat=g("fasat"), sugar=g("fsugar"),
        )
    return out


def build_ingredients(ifct):
    ing = {}
    for code, d in ifct.items():  # every IFCT food is available for "My recipes"
        ing[code] = dict(d, source="IFCT 2017", code=code)
    for key, code in IFCT.items():
        if code not in ifct:
            raise SystemExit(f"IFCT code {code} for {key} not found")
        ing[key] = dict(ifct[code], source="IFCT 2017", code=code)
    for key, d in CUSTOM.items():
        ing[key] = dict(d, group="Other", tamil="", source="custom: " + (d.get("note") or ""), code=key)
    for k, d in ing.items():
        d["fat_role"] = k in FATS
        d["added_sugar"] = k in ADDED_SUGAR
    return ing


def flatten(recipes, ing):
    """Resolve @dish references to plain ingredient grams; returns {id: (ing_dict, yield)}."""
    by_id = {r["id"]: r for r in recipes}
    cache = {}

    def resolve(rid, stack=()):
        if rid in cache:
            return cache[rid]
        if rid in stack:
            raise SystemExit("recipe cycle: " + " > ".join(stack + (rid,)))
        r = by_id[rid]
        flat = {}
        for k, grams in r["ing"].items():
            if k.startswith("@"):
                sub, sub_yield = resolve(k[1:], stack + (rid,))
                for sk, sg in sub.items():
                    flat[sk] = flat.get(sk, 0) + sg * grams / sub_yield
            else:
                if k not in ing:
                    raise SystemExit(f"{rid}: unknown ingredient '{k}'")
                flat[k] = flat.get(k, 0) + grams
        cache[rid] = (flat, r["yield_"])
        return cache[rid]

    for r in recipes:
        resolve(r["id"])
    return cache


def nutr_of(flat, ing):
    tot = {k: 0.0 for k in KEYS}
    added = 0.0
    for k, g in flat.items():
        d = ing[k]
        for key in KEYS:
            tot[key] += d[key] * g / 100
        if d["added_sugar"]:
            added += d["sugar"] * g / 100
    tot["added_sugar"] = added
    return tot


def default_flat(r, flat, ing):
    """Base ingredients + default choices (same rule the app uses)."""
    f = dict(flat)
    y = r["yield_"]
    for gr in r["choices"]:
        opts = [o for o in gr["options"] if o["default"]]
        if gr["multi"]:
            share = gr["total"] / max(1, len(opts))
            for o in opts:
                for k, w in o["ing"].items():
                    f[k] = f.get(k, 0) + share * w
            if opts:
                y += gr["total"] * gr["yieldf"]
        else:
            for o in opts:
                for k, g in o["ing"].items():
                    f[k] = f.get(k, 0) + g
                    y += g * gr["yieldf"]
    return f, y


def main():
    ifct = load_ifct()
    ing = build_ingredients(ifct)
    flat_all = flatten(RECIPES, ing)
    used = set()
    foods = []
    table = []
    for r in RECIPES:
        flat, _ = flat_all[r["id"]]
        for gr in r["choices"]:
            for o in gr["options"]:
                for k in o["ing"]:
                    if k not in ing:
                        raise SystemExit(f"{r['id']}: unknown choice ingredient '{k}'")
                    used.add(k)
        used.update(flat)
        if r["hidden"]:
            continue
        fdef, ydef = default_flat(r, flat, ing)
        n = nutr_of(fdef, ing)
        per100 = {k: round(v / ydef * 100, 3) for k, v in n.items()}
        unit_name, unit_g = next(iter(r["units"].items()))
        portion = r["defq"] * unit_g
        table.append((r["id"], f"{r['defq']:g} {unit_name}", round(per100["kcal"] * portion / 100), round(per100["protein"] * portion / 100, 1)))
        fat_g = sum(g for k, g in fdef.items() if ing[k]["fat_role"])
        foods.append(dict(
            id=r["id"], name=r["name"], tamil=r["tamil"], emoji=r["emoji"], cat=r["cat"],
            aliases=[a.strip().lower() for a in r["aliases"].split("|") if a.strip()],
            ing={k: round(v, 3) for k, v in flat.items()}, yield_=r["yield_"],
            units=[{"name": k, "g": v} for k, v in r["units"].items()], defq=r["defq"], liquid=r["liquid"],
            choices=r["choices"], oil=fat_g / ydef > 0.015, source=r["source"], per100=per100,
        ))
    ing_out = {k: {kk: (round(v, 4) if isinstance(v, float) else v) for kk, v in d.items()} for k, d in ing.items()}
    os.makedirs(ASSETS, exist_ok=True)
    json.dump(ing_out, open(os.path.join(ASSETS, "ingredients.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    json.dump(foods, open(os.path.join(ASSETS, "foods.json"), "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print(f"{len(foods)} dishes, {len(ing_out)} ingredients ({len(used)} used by recipes)")
    for row in table:
        print(f"  {row[0]:<22} {row[1]:<14} {row[2]:>5} kcal  {row[3]:>5} g protein")


if __name__ == "__main__":
    main()
