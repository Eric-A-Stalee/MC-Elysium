#!/usr/bin/env python3
"""Generate authored models/loot/recipes using vanilla references and original art.

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
asset("blockstates/tartarus_chain", {"variants": {"": {"model": "elysium:block/tartarus_chain"}}})
asset("models/block/tartarus_chain", {"parent": "minecraft:block/cube_all",
      "textures": {"all": "elysium:block/tartarus_chain"}})
asset("models/item/tartarus_chain", {"parent": "elysium:block/tartarus_chain"})
loot("tartarus_chain", [pool([item("elysium:tartarus_chain")], [SURVIVES])])
asset("particles/golden_leaf", {"textures": ["elysium:golden_leaf"]})
# Particles and block/item models use separate atlases. Stitch the same original
# leaf image into the blocks atlas as well, without duplicating its PNG.
GRASS_SPRITES = ("grass_block_top", "grass_block_side_overlay", "short_grass",
                 "tall_grass_top", "tall_grass_bottom")
emit("assets/minecraft/atlases/blocks.json", {"sources": [
    {"type": "minecraft:single", "resource": "elysium:particle/golden_leaf"},
    # Approved L3 curve; G4.1 below retains the G4 curve, with its 0.95 gain
    # folded into the biome tint (#FFD34A -> #F2C846, within 8-bit rounding).
    # Transparency, animation and resolution remain owned by the source sprite.
    {"type": "elysium:tone_mapped", "resource": "minecraft:block/birch_leaves",
     "sprite": "elysium:block/golden_birch_leaves", "gamma": 0.40, "contrast": 1.8, "pivot": 195},
    *({"type": "elysium:tone_mapped", "resource": f"minecraft:block/{name}",
       "sprite": f"elysium:block/golden_{name}", "gamma": 0.38, "contrast": 1.55, "pivot": 207}
      for name in GRASS_SPRITES)]})
asset("sounds", {"woodland_breeze": {"sounds": [{"name": "elysium:woodland_breeze", "stream": True, "volume": 0.32}]}})
for name, texture in [("elysium_shard", "quartz"), ("elysium_fragment", "prismarine_crystals"),
                      ("elysium_sigil", "nether_star")]:
    asset(f"models/item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"minecraft:item/{texture}"}})

faces = {side: {"texture": "#all", "cullface": side, "tintindex": 0}
         for side in ["down", "up", "north", "south", "west", "east"]}
asset("models/block/golden_birch_leaves", {
    "parent": "minecraft:block/block", "render_type": "minecraft:cutout_mipped",
    # Keep sky/block light and directional face shading, but avoid the extra
    # dark corner shading that makes dense golden crowns muddy in vanilla.
    "ambientocclusion": False,
    "textures": {"all": "elysium:block/golden_birch_leaves", "particle": "#all"},
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]})
asset("models/block/golden_birch_sapling", {"parent": "minecraft:block/cross",
      "render_type": "minecraft:cutout", "textures": {"cross": "minecraft:block/birch_sapling"}})
asset("models/block/wild_grain", {"parent": "minecraft:block/crop", "render_type": "minecraft:cutout",
      "textures": {"crop": "minecraft:block/wheat_stage7"}})
asset("models/block/leaf_litter", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
      "textures": {"leaf": "elysium:particle/golden_leaf", "particle": "#leaf"},
      "elements": [{"from": [x, y, z], "to": [x + 6, y, z + 6],
                    "faces": {"up": {"texture": "#leaf", "tintindex": 0, "uv": [0, 0, 16, 16], "rotation": rotation}}}
                   for x, y, z, rotation in [(0, 0.05, 1, 0), (8, 0.10, 0, 90), (3, 0.15, 9, 270), (9, 0.20, 8, 180)]]})


def cuboid(start, end, texture):
    return {"from": start, "to": end,
            "faces": {s: {"texture": texture} for s in ["down", "up", "north", "south", "west", "east"]}}


# The Forge's models are original block geometry with active-pack vanilla references.
# Distinct in-world plaques and a three-dimensional tuning fork need no copied art.
for name, surface, emblem in [
        ("forge_archive", "bookshelf", "waxed_copper_block"),
        ("forge_ledger", "chiseled_deepslate", "gold_block"),
        ("tartarus_inscription", "chiseled_polished_blackstone", "amethyst_block"),
        ("forge_hearth", "polished_blackstone", "copper_block")]:
    # Waxing is a block state/material choice; vanilla's texture is named copper_block.
    emblem = emblem.removeprefix("waxed_")
    pieces = [cuboid([0, 0, 0], [16, 16, 16], "#stone")]
    for a, b in [([3, 3, -0.02], [13, 13, 0.12]), ([3, 3, 15.88], [13, 13, 16.02]),
                 ([-0.02, 3, 3], [0.12, 13, 13]), ([15.88, 3, 3], [16.02, 13, 13])]:
        pieces.append(cuboid(a, b, "#emblem"))
    pieces += [cuboid([5, 15.9, 5], [11, 16.1, 11], "#socket"),
               cuboid([7, 5, -0.1], [9, 11, 0.15], "#socket"),
               cuboid([5, 9, -0.1], [11, 11, 0.15], "#socket")]
    asset(f"models/block/{name}", {"parent": "minecraft:block/block",
          "textures": {"stone": f"minecraft:block/{surface}", "emblem": f"minecraft:block/{emblem}",
                       "socket": "minecraft:block/obsidian" if name == "forge_hearth" else "minecraft:block/deepslate_tiles", "particle": "#stone"},
          "elements": pieces})
    asset(f"blockstates/{name}", {"variants": {"": {"model": f"elysium:block/{name}"}}})
    asset(f"models/item/{name}", {"parent": f"elysium:block/{name}"})
    loot(name, [pool([item(f"elysium:{name}")], [SURVIVES])])

# Dark plate stock is its own decorative iron block, not a cache of netherite resources.
asset("blockstates/forge_iron", {"variants":{"":{"model":"elysium:block/forge_iron"}}})
asset("models/block/forge_iron", {"parent":"minecraft:block/cube_all", "textures":{"all":"minecraft:block/netherite_block"}})
asset("models/item/forge_iron", {"parent":"elysium:block/forge_iron"})
loot("forge_iron",[pool([item("elysium:forge_iron")],[SURVIVES])])

# Exposed coal bowls and connected runners. Heat has a separate restored state;
# animated, contained fire occupies the air above an active exposed fixture.
HEAT_STATES = [("", {"lit": "false"}, "coal_block"),
               ("_lit", {"lit": "true", "restored": "false"}, "magma"),
               ("_restored", {"lit": "true", "restored": "true"}, "lava_still")]
for name in ("forge_service_lamp", "forge_service_switch", "forge_pilot_light"):
    states = [("_lit", {}, "magma")] if name == "forge_pilot_light" else HEAT_STATES
    variants = {}
    for suffix, condition, texture in states:
        pieces = [cuboid([0, 0, 0], [16, 16, 16], "#stone")]
        if name == "forge_pilot_light":
            pieces += [cuboid([6,6,-.02],[10,10,.02],"#ember"),cuboid([6,15.98,6],[10,16.02,10],"#ember")]
        else:
            pieces += [cuboid([3,16,3],[13,16.4,13],"#ember"),
                       cuboid([1,15,1],[3,19,15],"#metal"),cuboid([13,15,1],[15,19,15],"#metal"),
                       cuboid([3,15,1],[13,19,3],"#metal"),cuboid([3,15,13],[13,19,15],"#metal")]
            for lo, hi in [([4,9,-.02],[12,13,.02]),([4,9,15.98],[12,13,16.02]),
                           ([-.02,9,4],[.02,13,12]),([15.98,9,4],[16.02,13,12])]:
                pieces.append(cuboid(lo, hi, "#ember"))
        if name == "forge_service_switch":
            pieces += [cuboid([3,3,-1],[5,13,0],"#metal"),cuboid([11,3,-1],[13,13,0],"#metal"),
                       cuboid([5,3,-1],[11,5,0],"#metal"),cuboid([5,11,-1],[11,13,0],"#metal"),
                       cuboid([7,5,-1],[9,11,0],"#metal"),cuboid([7,7,-2],[9,9,-1],"#stone")]
        asset(f"models/block/{name}{suffix}", {"parent":"minecraft:block/block",
              "ambientocclusion": not bool(suffix),
              "textures":{"stone":"minecraft:block/polished_blackstone","metal":"minecraft:block/weathered_copper",
                          "ember":f"minecraft:block/{texture}","particle":"#stone"},"elements":pieces})
        variants[",".join(f"{k}={v}" for k,v in condition.items())] = {"model":f"elysium:block/{name}{suffix}"}
    asset(f"blockstates/{name}", {"variants":variants})
    asset(f"models/item/{name}", {"parent":f"elysium:block/{name}"+("_lit" if name=="forge_pilot_light" else "")})
    loot(name,[pool([item(f"elysium:{name}")],[SURVIVES])])

channel_parts = []
for decay in range(3):
    for suffix, condition, texture in HEAT_STATES:
        name = f"forge_ember_channel_{decay}{suffix}"
        pieces = [cuboid([0,0,0],[16,16,16],"#stone"),cuboid([4,16,4],[12,16.04,12],"#ember")]
        if decay == 1:
            pieces += [cuboid([2,16,2],[14,18,14],"#ash"),cuboid([4,18,6],[9,19,11],"#ash")]
        if decay == 2:
            pieces += [cuboid([6,16.05,3],[9,16.1,13],"#ash")]
        asset(f"models/block/{name}", {"parent":"minecraft:block/block", "ambientocclusion":not bool(suffix), "textures":{
            "stone":"minecraft:block/polished_blackstone", "ember":f"minecraft:block/{texture if suffix else 'weathered_copper'}",
            "ash":"minecraft:block/basalt_top" if decay==1 else "minecraft:block/obsidian", "particle":"#stone"},"elements":pieces})
        channel_parts.append({"when":{"decay":str(decay),**condition},"apply":{"model":f"elysium:block/{name}"}})
for direction,(lo,hi) in {
    "north":([5,16.01,0],[11,16.05,8]),"south":([5,16.01,8],[11,16.05,16]),
    "west":([0,16.01,5],[8,16.05,11]),"east":([8,16.01,5],[16,16.05,11]),
    "up":([5,8,-.02],[11,16,.02]),"down":([5,0,-.02],[11,8,.02])}.items():
    for suffix, condition, texture in HEAT_STATES:
        name = f"forge_channel_{direction}{suffix}"
        pieces = [cuboid(lo,hi,"#ember")]
        if direction in ("up","down"):
            pieces += [cuboid([5,lo[1],15.98],[11,hi[1],16.02],"#ember"),
                       cuboid([-.02,lo[1],5],[.02,hi[1],11],"#ember"),cuboid([15.98,lo[1],5],[16.02,hi[1],11],"#ember")]
        asset(f"models/block/{name}", {"parent":"minecraft:block/block", "ambientocclusion":not bool(suffix), "textures":{
            "ember":f"minecraft:block/{texture if suffix else 'weathered_copper'}", "particle":"#ember"},"elements":pieces})
        channel_parts.append({"when":{direction:"true",**condition},"apply":{"model":f"elysium:block/{name}"}})
asset("blockstates/forge_ember_channel", {"multipart":channel_parts})
asset("models/item/forge_ember_channel", {"parent":"elysium:block/forge_ember_channel_0"})

flame_variants = {}
for bowl in (False, True):
    for restored in (False, True):
        name = f"forge_flame_{'bowl' if bowl else 'runner'}_{'restored' if restored else 'low'}"
        bottom, top = (3, 16 if restored else 11) if bowl else (.1, 13 if restored else 7)
        width = 14 if bowl else 11 if restored else 8
        pieces = []
        for angle, texture in [(45,"#fire0"),(-45,"#fire1")]:
            pieces.append({"from":[8-width/2,bottom,8], "to":[8+width/2,top,8], "shade":False,
                "rotation":{"origin":[8,8,8],"axis":"y","angle":angle},
                "faces":{side:{"texture":texture,"uv":[0,0,16,16]} for side in ("north","south")}})
        asset(f"models/block/{name}", {"parent":"minecraft:block/block","render_type":"minecraft:cutout",
            "ambientocclusion":False,"textures":{"fire0":"minecraft:block/fire_0","fire1":"minecraft:block/fire_1","particle":"#fire0"},
            "elements":pieces})
        flame_variants[f"brazier={str(bowl).lower()},restored={str(restored).lower()}"] = {"model":f"elysium:block/{name}"}
asset("blockstates/forge_flame", {"variants":flame_variants})
loot("forge_ember_channel",[pool([
    item("elysium:forge_ember_channel") | {"conditions":[{"condition":"minecraft:inverted","term":{"condition":"minecraft:block_state_property","block":"elysium:forge_ember_channel","properties":{"decay":"2"}}}]},
    item("minecraft:iron_nugget") | {"conditions":[{"condition":"minecraft:block_state_property","block":"elysium:forge_ember_channel","properties":{"decay":"2"}}]}
],[SURVIVES])])
data("recipe/forge_ember_channel",{"type":"minecraft:crafting_shaped","category":"building","pattern":["I I","CCC"],
     "key":{"I":{"item":"minecraft:iron_ingot"},"C":{"item":"minecraft:copper_ingot"}},"result":{"id":"elysium:forge_ember_channel","count":8}})
data("advancement/recipes/forge_ember_channel",{"parent":"minecraft:recipes/root","criteria":{"copper":{"trigger":"minecraft:inventory_changed",
     "conditions":{"items":[{"items":"minecraft:copper_ingot"}]}}},"rewards":{"recipes":["elysium:forge_ember_channel"]}})

for kind,texture in enumerate(("basalt_top","cobbled_deepslate","blackstone","calcite")):
    pieces = [cuboid([1,0,2],[7,1,7],"#dust"), cuboid([8,0,8],[15,.7,15],"#dust"),
              cuboid([3,.5,3],[6,2,6],"#chips"), cuboid([10,.5,9],[13,3,12],"#chips"),
              cuboid([9,0,2],[12,1.5,4],"#chips"),cuboid([1,0,11],[4,1,14],"#chips")]
    asset(f"models/block/forge_debris_{kind}", {"parent":"minecraft:block/block",
          "textures":{"chips":f"minecraft:block/{texture}","dust":"minecraft:block/tuff","particle":"#chips"},"elements":pieces})
asset("blockstates/forge_debris",{"variants":{f"kind={k}":[{"model":f"elysium:block/forge_debris_{k}","y":r} for r in (0,90,180,270)] for k in range(4)}})
asset("models/item/forge_debris",{"parent":"elysium:block/forge_debris_1"})
loot("forge_debris",[pool([item("elysium:forge_debris")],[SURVIVES])])

asset("models/item/forge_survey", {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/written_book"}})
asset("models/item/chain_resonator", {
    "parent": "minecraft:block/block", "gui_light": "front",
    "textures": {"metal": "minecraft:block/netherite_block", "copper": "minecraft:block/copper_block",
                 "crystal": "minecraft:block/amethyst_block", "particle": "#copper"},
    "elements": [cuboid([7, 0, 7], [9, 8, 9], "#copper"), cuboid([4, 7, 7], [12, 9, 9], "#metal"),
                 cuboid([3, 8, 7], [5, 15, 9], "#metal"), cuboid([11, 8, 7], [13, 15, 9], "#metal"),
                 cuboid([2.5, 13, 6.5], [5.5, 16, 9.5], "#crystal"), cuboid([10.5, 13, 6.5], [13.5, 16, 9.5], "#crystal"),
                 cuboid([6.5, 2, 6.5], [9.5, 4, 9.5], "#metal")],
    "display": {"gui": {"rotation": [0, 0, -25], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                "firstperson_righthand": {"rotation": [0, -20, 15], "translation": [1, 3, 1], "scale": [0.75, 0.75, 0.75]},
                "firstperson_lefthand": {"rotation": [0, 20, -15], "translation": [1, 3, 1], "scale": [0.75, 0.75, 0.75]},
                "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.75, 0.75, 0.75]},
                "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 1], "scale": [0.75, 0.75, 0.75]}}})


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

for name in ["golden_birch_leaves", "golden_birch_sapling", "wild_grain", "leaf_litter"]:
    asset(f"blockstates/{name}", {"variants": {"": {"model": f"elysium:block/{name}"}}})
for name in ["golden_birch_leaves", "golden_birch_sapling", "wild_grain", "harvest_altar", "leaf_litter"]:
    asset(f"models/item/{name}", {"parent": f"elysium:block/{name}"})
# Cross/crop inventory sprites should remain legible when held.
for name, tex in [("golden_birch_sapling", "birch_sapling"), ("wild_grain", "wheat_stage7")]:
    asset(f"models/item/{name}", {"parent": "minecraft:item/generated", "textures": {"layer0": f"minecraft:block/{tex}"}})

loot("golden_birch_sapling", [pool([item("elysium:golden_birch_sapling")], [SURVIVES])])
asset("models/item/leaf_litter", {"parent": "minecraft:item/generated", "textures": {"layer0": "elysium:particle/golden_leaf"}})
loot("leaf_litter", [pool([item("elysium:leaf_litter")], [SURVIVES])])
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
                    "mineable/hoe": ["elysium:golden_birch_leaves"], "mineable/pickaxe": ["elysium:harvest_altar", "elysium:tartarus_chain", "elysium:forge_hearth", "elysium:forge_ledger", "elysium:tartarus_inscription", "elysium:forge_service_lamp", "elysium:forge_service_switch", "elysium:forge_pilot_light", "elysium:forge_debris", "elysium:forge_iron", "elysium:forge_ember_channel"],
                    "mineable/axe": ["elysium:forge_archive"],
                    "needs_iron_tool": ["elysium:tartarus_chain", "elysium:forge_iron"],
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

for name, parent, icon, title, description in [
        ("forge_survey", "elysium", "forge_survey", "An Older Craft", "Read the mountain archive and recover a survey of the old Forge."),
        ("cold_hearth", "forge_survey", "forge_hearth", "The Unfinished Work", "Examine the dormant hearth of Hephaestus and recover a Chain Resonator."),
        ("borrowed_light", "forge_survey", "forge_service_switch", "A Little Light Remains", "Fuel and ignite the service brazier, then open its draught. The great hearth still waits for the First Flame."),
        ("voice_in_iron", "cold_hearth", "chain_resonator", "A Voice in the Iron", "Listen to the terminal anchor of a Chain of Tartarus.")]:
    data(f"advancement/{name}", {"parent": f"elysium:{parent}",
         "display": {"icon": {"id": f"elysium:{icon}"}, "title": title, "description": description,
                     "frame": "goal" if name != "forge_survey" else "task", "announce_to_chat": False},
         "criteria": {"discovered": {"trigger": "minecraft:impossible"}}})

# Tartarus expedition: authored geometry, material references owned by the active pack.
for name, texture in [('burnt_clinker','blackstone'),('cinder_ash','gray_terracotta'),('smouldering_clinker','blackstone'),
                      ('keeper_workbench','smithing_table_side'),('tartarus_counterweight','chiseled_deepslate'),('tartarus_heart','chiseled_polished_blackstone')]:
    pieces=[cuboid([0,0,0],[16,16,16],'#base')]
    textures={'base':f'minecraft:block/{texture}','particle':'#base','metal':'minecraft:block/weathered_copper','ember':'minecraft:block/magma','iron':'minecraft:block/netherite_block'}
    if name=='smouldering_clinker':
        pieces += [cuboid([5,15.99,2],[6,16.01,13],'#ember'),cuboid([6,15.99,10],[12,16.01,11],'#ember')]
    if name=='keeper_workbench':
        pieces += [cuboid([1,16,1],[15,17,15],'#metal'),cuboid([3,17,4],[5,19,12],'#iron'),cuboid([11,17,4],[13,19,12],'#iron')]
    if name=='tartarus_counterweight':
        pieces += [cuboid([6,16,6],[10,23,10],'#iron'),cuboid([1,21,6],[15,23,10],'#metal'),cuboid([1,17,6],[3,21,10],'#metal'),cuboid([13,17,6],[15,21,10],'#metal')]
    if name=='tartarus_heart':
        pieces += [cuboid([1,16,1],[4,21,4],'#iron'),cuboid([12,16,12],[15,21,15],'#iron'),cuboid([6,16,6],[10,17,10],'#metal')]
    asset(f'models/block/{name}',{'parent':'minecraft:block/block','textures':textures,'elements':pieces})
    asset(f'blockstates/{name}',{'variants':{'':{'model':f'elysium:block/{name}'}}})
    asset(f'models/item/{name}',{'parent':f'elysium:block/{name}'})
    if name not in ('tartarus_counterweight','tartarus_heart'):loot(name,[pool([item(f'elysium:{name}')],[SURVIVES])])
asset('models/block/chimera_glow',{'textures':{'particle':'minecraft:block/blackstone'},'elements':[]})
asset('blockstates/chimera_glow',{'variants':{'':{'model':'elysium:block/chimera_glow'}}})
# Cage holds an amber light. A normal block light when placed; no held dynamic-light dependency.
lamp=[cuboid([3,0,3],[13,2,13],'#iron'),cuboid([4,11,4],[12,13,12],'#copper'),cuboid([5,3,5],[11,10,11],'#light')]
for x in (4,11):
    for z in (4,11):lamp.append(cuboid([x,2,z],[x+1,11,z+1],'#copper'))
lamp += [cuboid([7,13,7],[9,16,9],'#iron')]
asset('models/block/keepers_lamp',{'parent':'minecraft:block/block','ambientocclusion':False,
      'textures':{'iron':'minecraft:block/netherite_block','copper':'minecraft:block/copper_block','light':'minecraft:block/glowstone','particle':'#copper'},'elements':lamp})
asset('blockstates/keepers_lamp',{'variants':{'':{'model':'elysium:block/keepers_lamp'}}})
asset('models/item/keepers_lamp',{'parent':'elysium:block/keepers_lamp'})
loot('keepers_lamp',[pool([item('elysium:keepers_lamp')],[SURVIVES])])
asset('models/item/first_flame',{'parent':'minecraft:block/block','gui_light':'front','ambientocclusion':False,
      'textures':{'shell':'minecraft:block/obsidian','flame':'minecraft:block/glowstone','hot':'minecraft:block/orange_concrete','particle':'#flame'},
      'elements':[cuboid([3,1,3],[13,4,13],'#shell'),cuboid([5,4,5],[11,12,11],'#hot'),
                  cuboid([6,5,6],[10,14,10],'#flame'),cuboid([7,14,7],[9,16,9],'#hot')]})
hearth=json.loads(OUTPUTS['assets/elysium/models/block/forge_hearth.json'])
hearth['textures']['socket']='minecraft:block/glowstone'
asset('models/block/forge_hearth_flame',hearth)
asset('blockstates/forge_hearth',{'variants':{'flame=false':{'model':'elysium:block/forge_hearth'},'flame=true':{'model':'elysium:block/forge_hearth_flame'}}})
for name,parent,icon,title,description in [
    ('keepers_lamp','borrowed_light','keepers_lamp','A Light to Carry','Restore the workshop embers, then bring a lantern, copper ingot and amethyst shard to the Keeper workbench. Place and recover the lamp along the descent.'),
    ('tartarus_crossing','voice_in_iron','tartarus_counterweight','Across the Cinder Rift','Repair the fallen crossing with four chains at its counterweight.'),
    ('first_flame','tartarus_crossing','first_flame','The Fire Within','Survive the one-eyed chimera. When its attack ends, the lowered eye is vulnerable.'),
    ('flame_returned','first_flame','forge_hearth','A Promise Carried Home','Return the First Flame to the original Forge hearth. The great mechanism is still silent; the afternoon has not ended.')]:
    data(f'advancement/{name}',{'parent':f'elysium:{parent}','display':{'icon':{'id':f'elysium:{icon}'},'title':title,'description':description,'frame':'challenge' if name=='first_flame' else 'goal','announce_to_chat':False},'criteria':{'discovered':{'trigger':'minecraft:impossible'}}})
# Append to the generated pickaxe tag without duplicating the earlier list.
path='data/minecraft/tags/block/mineable/pickaxe.json'
mining=json.loads(OUTPUTS[path]);mining['values'] += [f'elysium:{n}' for n in ('keepers_lamp','burnt_clinker','smouldering_clinker','cinder_ash','keeper_workbench')];emit(path,mining)

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
