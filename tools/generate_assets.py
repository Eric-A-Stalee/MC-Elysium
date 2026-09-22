#!/usr/bin/env python3
"""Generate authored models/loot/recipes using references to vanilla textures only.

No Mojang texture bytes are copied into the project. Run --check in CI.
"""
from pathlib import Path
import argparse
import json

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
OUTPUTS = {}


def emit(path, obj):
    OUTPUTS[path] = json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def asset(path, obj):
    emit(f"assets/elysium/{path}.json", obj)


def data(path, obj):
    emit(f"data/elysium/{path}.json", obj)


def item(name):
    return {"type": "minecraft:item", "name": name}


def loot(name, pools):
    data(f"loot_table/blocks/{name}", {"type": "minecraft:block", "pools": pools})


def pool(entries, conditions=None):
    return {"rolls": 1, "entries": entries, "conditions": conditions or []}


SURVIVES = {"condition": "minecraft:survives_explosion"}
for name, texture in [("elysium_shard", "quartz"), ("elysium_fragment", "prismarine_crystals"),
                      ("elysium_sigil", "nether_star")]:
    asset(f"models/item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"minecraft:item/{texture}"}})

faces = {side: {"texture": "#all", "cullface": side, "tintindex": 0}
         for side in ["down", "up", "north", "south", "west", "east"]}
asset("models/block/golden_birch_leaves", {
    "parent": "minecraft:block/block", "render_type": "minecraft:cutout_mipped",
    "textures": {"all": "minecraft:block/birch_leaves", "particle": "#all"},
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]})
asset("models/block/golden_birch_sapling", {"parent": "minecraft:block/cross",
      "render_type": "minecraft:cutout", "textures": {"cross": "minecraft:block/birch_sapling"}})
asset("models/block/wild_grain", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
      "textures": {"crop": "minecraft:block/wheat_stage7"}})


def cuboid(start, end, texture):
    return {"from": start, "to": end,
            "faces": {s: {"texture": texture} for s in ["down", "up", "north", "south", "west", "east"]}}


for active in [False, True]:
    for sigil in [False, True]:
        suffix = ("_active" if active else "") + ("_sigil" if sigil else "")
        elements = [cuboid([0, 0, 0], [16, 3, 16], "#stone"),
                    cuboid([3, 3, 3], [13, 11, 13], "#pillar"),
                    cuboid([0, 11, 0], [16, 14, 16], "#stone"),
                    cuboid([2, 14, 2], [14, 15, 14], "#top")]
        if sigil:
            elements.append(cuboid([6, 15, 6], [10, 16, 10], "#sigil"))
        asset(f"models/block/harvest_altar{suffix}", {"parent": "minecraft:block/block",
              "textures": {"particle": "minecraft:block/quartz_block_side", "stone": "minecraft:block/quartz_block_side",
                           "pillar": "minecraft:block/chiseled_quartz_block", "top": "minecraft:block/glowstone" if active else "minecraft:block/gold_block",
                           "sigil": "minecraft:block/gold_block"}, "elements": elements})
asset("blockstates/harvest_altar", {"variants": {
    f"active={str(a).lower()},has_sigil={str(s).lower()}": {"model": "elysium:block/harvest_altar" + ("_active" if a else "") + ("_sigil" if s else "")}
    for a in [False, True] for s in [False, True]}})

for name in ["golden_birch_leaves", "golden_birch_sapling", "wild_grain"]:
    asset(f"blockstates/{name}", {"variants": {"": {"model": f"elysium:block/{name}"}}})
for name in ["golden_birch_leaves", "golden_birch_sapling", "wild_grain", "harvest_altar"]:
    asset(f"models/item/{name}", {"parent": f"elysium:block/{name}"})
# Cross/crop inventory sprites should remain legible when held.
for name, tex in [("golden_birch_sapling", "birch_sapling"), ("wild_grain", "wheat_stage7")]:
    asset(f"models/item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"minecraft:block/{tex}"}})

loot("golden_birch_sapling", [pool([item("elysium:golden_birch_sapling")], [SURVIVES])])
loot("wild_grain", [pool([item("minecraft:wheat")], [SURVIVES]), pool([item("minecraft:wheat_seeds")],
                   [SURVIVES, {"condition": "minecraft:random_chance", "chance": 0.35}])])
loot("harvest_altar", [pool([item("elysium:harvest_altar")], [SURVIVES]),
     pool([item("elysium:elysium_sigil")], [{"condition": "minecraft:block_state_property",
          "block": "elysium:harvest_altar", "properties": {"has_sigil": "true"}}])])
silk = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments":
        [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}
shears = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
leaf_item = item("elysium:golden_birch_leaves") | {"conditions": [{"condition": "minecraft:any_of", "terms": [silk, shears]}]}
sapling_item = item("elysium:golden_birch_sapling") | {"conditions": [SURVIVES,
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.05, 0.0625, 0.083333336, 0.1]}]}
loot("golden_birch_leaves", [pool([{"type": "minecraft:alternatives", "children": [leaf_item, sapling_item]}])])

for result, ingredient in [("elysium_fragment", "elysium_shard"), ("elysium_sigil", "elysium_fragment")]:
    data(f"recipe/{result}", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SS", "SS"],
         "key": {"S": {"item": f"elysium:{ingredient}"}}, "result": {"id": f"elysium:{result}", "count": 1}})
data("recipe/harvest_altar", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" G ", "QBQ", "QQQ"],
     "key": {"G": {"item": "minecraft:gold_ingot"}, "Q": {"item": "minecraft:quartz"}, "B": {"item": "minecraft:hay_block"}},
     "result": {"id": "elysium:harvest_altar", "count": 1}})
emit("data/neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": ["elysium:wheat_shard"]})
data("loot_modifiers/wheat_shard", {"type": "elysium:wheat_shard", "conditions": [
     {"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:blocks/wheat"},
     {"condition": "minecraft:block_state_property", "block": "minecraft:wheat", "properties": {"age": "7"}}]})

for tag, values in {"leaves": ["elysium:golden_birch_leaves"], "saplings": ["elysium:golden_birch_sapling"],
                    "mineable/hoe": ["elysium:golden_birch_leaves"], "mineable/pickaxe": ["elysium:harvest_altar"],
                    "bee_growables": ["elysium:golden_birch_sapling"]}.items():
    emit(f"data/minecraft/tags/block/{tag}.json", {"replace": False, "values": values})
for tag, values in {"leaves": ["elysium:golden_birch_leaves"], "saplings": ["elysium:golden_birch_sapling"]}.items():
    emit(f"data/minecraft/tags/item/{tag}.json", {"replace": False, "values": values})

# The advancement book supplies discovery/lore without requiring a guidebook dependency.
data("advancement/root", {"display": {"icon": {"id": "elysium:elysium_shard"}, "title": "A Glint in the Harvest",
     "description": "Something impossible was hiding in the wheat. Four shards make a fragment; four fragments make a sigil.",
     "background": "minecraft:textures/block/birch_planks.png", "show_toast": True, "announce_to_chat": False},
     "criteria": {"shard": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "elysium:elysium_shard"}]}}}})
data("advancement/sigil", {"parent": "elysium:root", "display": {"icon": {"id": "elysium:elysium_sigil"},
     "title": "The Remaining Light", "description": "An open sky. Four white pillars. A harvest laid between them. Offer wheat to the sigil as the sun descends.",
     "show_toast": True, "announce_to_chat": False}, "criteria": {"sigil": {"trigger": "minecraft:inventory_changed",
     "conditions": {"items": [{"items": "elysium:elysium_sigil"}]}}}})
data("advancement/elysium", {"parent": "elysium:sigil", "display": {"icon": {"id": "elysium:golden_birch_leaves"},
     "title": "An Afternoon Without End", "description": "Step into Elysium.", "frame": "goal", "announce_to_chat": False},
     "criteria": {"entered": {"trigger": "minecraft:changed_dimension", "conditions": {"to": "elysium:elysium"}}}})
for result, ingredient in [("elysium_fragment", "elysium_shard"), ("elysium_sigil", "elysium_fragment"), ("harvest_altar", "elysium_shard")]:
    data(f"advancement/recipes/{result}", {"parent": "minecraft:recipes/root", "criteria": {"has_item": {
         "trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"elysium:{ingredient}"}]}}},
         "rewards": {"recipes": [f"elysium:{result}"]}})

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    stale = []
    for name, content in sorted(OUTPUTS.items()):
        path = ROOT / name
        if args.check:
            if not path.exists() or path.read_text() != content:
                stale.append(name)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
    if stale:
        raise SystemExit("Outdated generated assets: " + ", ".join(stale))
    print(f"{'Checked' if args.check else 'Generated'} {len(OUTPUTS)} Elysium resources.")
