#!/usr/bin/env python3
"""Build Elysium's data pack from one parameterized, immutable biome catalog.

Uses Python's standard library only. All geometry and terrain formulas here are
original; Minecraft block, feature and noise identifiers reference game assets.
Run from any directory; --check verifies generated files without modifying them.
"""
from __future__ import annotations

import argparse
import gzip
import io
import json
import struct
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/elysium"
WORLDGEN = DATA / "worldgen"
MIN_Y, HEIGHT, SEA_LEVEL = -64, 384, 63
GOLDEN_HOUR = 11000


@dataclass(frozen=True)
class Palette:
    grass: int
    foliage: int = 0xE8BB39
    sky: int = 0xB5D2E7
    fog: int = 0xF0DAB1
    water: int = 0x55A6AE
    water_fog: int = 0x287E88

    def effects(self) -> dict:
        return {f"{key}_color": value for key, value in vars(self).items()}


@dataclass(frozen=True)
class ClimateBox:
    humidity: tuple[float, float]
    erosion: tuple[float, float]
    temperature: tuple[float, float] = (-1.0, 1.0)
    continentalness: tuple[float, float] = (-1.2, 1.2)
    weirdness: tuple[float, float] = (-1.2, 1.2)

    def parameters(self) -> dict:
        return {**{key: list(value) for key, value in vars(self).items()},
                "depth": [-2.0, 2.0], "offset": 0.0}


@dataclass(frozen=True)
class GrovePattern:
    clearing_attempts: int
    noise_threshold: float = -0.15

    def placement(self, woodland_attempts: int) -> dict:
        # Vanilla samples a coherent X/Z noise at a 200-block scale. A shared
        # count per chunk creates woodland openings instead of isolated misses.
        return {"type": "minecraft:noise_threshold_count",
                "noise_level": self.noise_threshold,
                "below_noise": self.clearing_attempts,
                "above_noise": woodland_attempts}


@dataclass(frozen=True)
class Vegetation:
    tree_attempts: int
    grain_patches: int
    flower_patches: int
    grass_patches: int
    tall_tree_chance: float
    branching_tree_chance: float = 0.0
    groves: GrovePattern | None = None
    riverside_attempts: int = 0
    canopy_tree_chance: float = 0.0
    leaf_patches: int = 0


@dataclass(frozen=True)
class TreeShape:
    id: str
    height: tuple[int, int, int]
    radius: tuple[int, int]
    foliage_height: int
    branching: bool = False
    forking: bool = False


TREE_SHAPES = (
    TreeShape("golden_birch", (5, 2, 1), (2, 2), 3),
    # Narrower variable crowns and a wider height range interrupt the old roof.
    TreeShape("tall_golden_birch", (9, 3, 2), (2, 3), 4),
    TreeShape("branching_golden_birch", (8, 4, 0), (2, 2), 4, branching=True),
    TreeShape("canopy_golden_birch", (12, 3, 2), (3, 4), 4, forking=True),
)


@dataclass(frozen=True)
class Spawn:
    entity: str
    weight: int
    minimum: int = 2
    maximum: int = 4

    def entry(self) -> dict:
        return {"type": f"minecraft:{self.entity}", "weight": self.weight,
                "minCount": self.minimum, "maxCount": self.maximum}


@dataclass(frozen=True)
class BiomeDefinition:
    id: str
    name: str
    climates: tuple[ClimateBox, ...]
    palette: Palette
    vegetation: Vegetation
    creatures: tuple[Spawn, ...]
    temperature: float = 0.85
    downfall: float = 0.45
    pale_cliffs: bool = False
    settlements: tuple[str, ...] = ()
    ambient_loop: str | None = None

    @property
    def key(self) -> str:
        return f"elysium:{self.id}"


FARM_CREATURES = (Spawn("sheep", 12), Spawn("cow", 8), Spawn("chicken", 8),
                  Spawn("rabbit", 5, 2, 3))
# Corridors use the same ridge and temperature signals as the river/lake
# density functions. Temperature is a geographic palette selector, not time.
def inland_climates(humidity):
    return tuple(ClimateBox(humidity, (-0.22, 1.0), temperature=temp, weirdness=band)
                 for temp, width in (((-1.0, 0.25), 0.10), ((0.25, 1.0), 0.22))
                 for band in ((-1.2, -width), (width, 1.2)))


BIOMES = (
    BiomeDefinition("golden_fields", "Golden Fields",
                    inland_climates((-1.0, 0.10)),
                    Palette(0xCBB16A), Vegetation(0, 12, 2, 2, 0.20, riverside_attempts=2),
                    FARM_CREATURES + (Spawn("horse", 3, 2, 4),),
                    settlements=("harvest_hamlet",)),
    BiomeDefinition("golden_birch_woods", "Golden Birch Woods",
                    inland_climates((0.10, 1.0)),
                    Palette(0xB99B59, fog=0xE6D6AA),
                    Vegetation(7, 3, 3, 5, 0.50, 0.20, GrovePattern(2)), FARM_CREATURES,
                    downfall=0.65, ambient_loop="elysium:woodland_breeze"),
    BiomeDefinition("golden_watermeadows", "Golden Watermeadows",
                    (ClimateBox((-1.0, 1.0), (-0.22, 1.0), temperature=(-1.0, 0.25), weirdness=(-0.10, 0.10)),),
                    Palette(0xC4AA60, foliage=0xEAC34C, water=0x65AEB3),
                    Vegetation(2, 1, 9, 4, 0.15, canopy_tree_chance=0.75), FARM_CREATURES,
                    downfall=0.65, ambient_loop="elysium:woodland_breeze", settlements=("waterside_cottage",)),
    BiomeDefinition("amber_lakes", "Amber Lakes",
                    (ClimateBox((-1.0, 1.0), (-0.22, 1.0), temperature=(0.25, 1.0), weirdness=(-0.22, 0.22)),),
                    Palette(0xA58D52, foliage=0xCC8537, sky=0xB9CEDD, fog=0xDFC39C,
                            water=0x386C79, water_fog=0x264B59),
                    Vegetation(3, 2, 3, 3, 0.25, 0.20, canopy_tree_chance=0.40, leaf_patches=3),
                    FARM_CREATURES, downfall=0.6, ambient_loop="elysium:woodland_breeze", settlements=("waterside_cottage",)),
    BiomeDefinition("elysian_highlands", "Elysian Highlands",
                    (ClimateBox((-1.0, 1.0), (-0.55, -0.22)),),
                    Palette(0xBBB28A, foliage=0xDDBB68, sky=0xAFC6DF, fog=0xD7DCE2,
                            water=0x527D93, water_fog=0x354F68),
                    Vegetation(2, 2, 2, 3, 0.70, 0.10),
                    (Spawn("sheep", 10), Spawn("rabbit", 5, 2, 3)),
                    temperature=0.45, downfall=0.35, pale_cliffs=True, settlements=("mountain_town",)),
    BiomeDefinition("ivory_peaks", "Ivory Peaks",
                    (ClimateBox((-1.0, 1.0), (-1.0, -0.55)),),
                    Palette(0xB9B496, foliage=0xD2B66F, sky=0xA7BDD5, fog=0xCDD6E1,
                            water=0x496D88, water_fog=0x334962),
                    Vegetation(1, 0, 1, 1, 0.80), (Spawn("sheep", 8), Spawn("rabbit", 4, 2, 3)),
                    temperature=0.35, downfall=0.25, pale_cliffs=True, settlements=("mountain_town",)),
)


def state(name: str, **properties: str) -> dict:
    result = {"Name": name if ":" in name else f"minecraft:{name}"}
    if properties:
        result["Properties"] = properties
    return result


def simple_provider(name: str, **properties: str) -> dict:
    return {"type": "minecraft:simple_state_provider", "state": state(name, **properties)}


def unary(kind: str, argument) -> dict:
    return {"type": f"minecraft:{kind}", "argument": argument}


def binary(kind: str, first, second) -> dict:
    return {"type": f"minecraft:{kind}", "argument1": first, "argument2": second}


def gradient(start: int, end: int, first: float, second: float) -> dict:
    return {"type": "minecraft:y_clamped_gradient", "from_y": start,
            "to_y": end, "from_value": first, "to_value": second}


def spline(coordinate, points: tuple[tuple[float, float], ...]) -> dict:
    return {"type": "minecraft:spline", "spline": {"coordinate": coordinate,
            "points": [{"location": x, "value": y, "derivative": 0.0}
                       for x, y in points]}}


def condition(test: dict, rule: dict) -> dict:
    return {"type": "minecraft:condition", "if_true": test, "then_run": rule}


def block_rule(name: str, **properties: str) -> dict:
    return {"type": "minecraft:block", "result_state": state(name, **properties)}


def sequence(*rules: dict) -> dict:
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def surface_depth(offset: int, surface_depth: bool = False) -> dict:
    return {"type": "minecraft:stone_depth", "offset": offset,
            "add_surface_depth": surface_depth, "secondary_depth_range": 0,
            "surface_type": "floor"}


def make_noise(entries: dict[Path, bytes]) -> None:
    # Own every density-function entry point, including domain shifts. Terrain
    # mods may replace minecraft:overworld/* with Overworld-only sampler markers;
    # following those registry aliases from another dimension can yield zeroes.
    # These raw vanilla operators retain vanilla noise parameters/seed semantics
    # without depending on another dimension's registry overrides or engine.
    for name, kind in (("shift_x", "shift_a"), ("shift_z", "shift_b")):
        emit(entries, f"worldgen/density_function/{name}.json",
             unary("flat_cache", unary("cache_2d", unary(kind, "minecraft:offset"))))
    for name, noise in (("continents", "continentalness"), ("erosion", "erosion"),
                        ("ridges", "ridge"), ("temperature", "temperature"),
                        ("vegetation", "vegetation")):
        function = {"type": "minecraft:shifted_noise", "noise": f"minecraft:{noise}",
                    "shift_x": "elysium:shift_x", "shift_y": 0.0,
                    "shift_z": "elysium:shift_z", "xz_scale": 0.25, "y_scale": 0.0}
        if name in {"continents", "erosion", "ridges"}:
            function = unary("flat_cache", function)
        emit(entries, f"worldgen/density_function/{name}.json", function)
    # Height varies only across X/Z. Density decreases monotonically with Y,
    # so a ground column cannot contain underground air pockets or noise caves.
    # abs(ridges) near zero cuts linked river channels down below sea level.
    emit(entries, "worldgen/density_function/river_distance.json", unary("abs", "elysium:ridges"))
    emit(entries, "worldgen/density_function/autumn_weight.json",
         spline("elysium:temperature", ((-1.2, 0.0), (0.05, 0.0), (0.30, 1.0), (1.2, 1.0))))
    river = spline("elysium:river_distance",
                   ((0.0, 0.0), (0.012, 0.0), (0.028, 0.12), (0.065, 0.40), (0.14, 1.0), (1.2, 1.0)))
    lake = spline("elysium:river_distance",
                  ((0.0, 0.0), (0.075, 0.0), (0.12, 0.16), (0.19, 0.65), (0.27, 1.0), (1.2, 1.0)))
    # Broad quiet reaches connect to the brook network. The blend avoids a
    # terrain step at biome borders; neither mask depends on a biome lookup.
    channel = binary("add", river, binary("mul", "elysium:autumn_weight",
                    binary("add", lake, binary("mul", -1.0, river))))
    mountains = spline("elysium:erosion",
                       ((-1.0, 118.0), (-0.7, 104.0), (-0.45, 58.0),
                        (-0.22, 12.0), (0.0, 3.0), (0.5, 0.0), (1.0, 0.0)))
    relief = {"type": "minecraft:noise", "noise": "elysium:gentle_relief",
              "xz_scale": 0.25, "y_scale": 0.0}
    upland = binary("add", 17.0,
                    binary("add", binary("mul", 9.0, "elysium:continents"),
                           binary("add", mountains, binary("mul", 3.0, relief))))
    terrain_height = unary("flat_cache", binary("add", 60.0, binary("mul", channel, upland)))
    terrain_density = binary("mul", 0.05, binary("add", "elysium:terrain_height",
                                                    gradient(MIN_Y, 320, 64.0, -320.0)))
    emit(entries, "worldgen/noise/gentle_relief.json", {"firstOctave": -4, "amplitudes": [1.0, 0.5]})
    emit(entries, "worldgen/density_function/terrain_height.json", terrain_height)
    emit(entries, "worldgen/density_function/terrain_density.json", terrain_density)

    cliff_biomes = [biome.key for biome in BIOMES if biome.pale_cliffs]
    pale_cliffs = condition({"type": "minecraft:biome", "biome_is": cliff_biomes},
                            condition({"type": "minecraft:steep"}, block_rule("calcite")))
    # Submerged beds are sand; land keeps biome-tinted grass and dirt. No deepslate,
    # ore veins, lakes, aquifers, springs or carvers obscure the buried future.
    surface = sequence(
        condition({"type": "minecraft:vertical_gradient", "random_name": "elysium:bedrock_floor",
                   "true_at_and_below": {"above_bottom": 0},
                   "false_at_and_above": {"above_bottom": 5}}, block_rule("bedrock")),
        condition(surface_depth(0), sequence(
            pale_cliffs,
            condition({"type": "minecraft:water", "offset": 0,
                       "surface_depth_multiplier": 0, "add_stone_depth": False},
                      block_rule("grass_block", snowy="false")),
            block_rule("sand"))),
        condition(surface_depth(3), sequence(pale_cliffs, block_rule("dirt"))),
    )
    router = {"barrier": 0.0, "fluid_level_floodedness": 0.0,
              "fluid_level_spread": 0.0, "lava": 0.0,
              "continents": "elysium:continents", "erosion": "elysium:erosion",
              "depth": 0.0, "ridges": "elysium:ridges",
              "initial_density_without_jaggedness": "elysium:terrain_density",
              "final_density": unary("squeeze", unary("interpolated", unary("blend_density", "elysium:terrain_density"))),
              "vein_toggle": 0.0, "vein_ridged": 0.0, "vein_gap": 0.0}
    for target in ("temperature", "vegetation"):
        router[target] = f"elysium:{target}"
    emit(entries, "worldgen/noise_settings/elysium.json", {
        "aquifers_enabled": False, "default_block": state("stone"),
        "default_fluid": state("water", level="0"), "disable_mob_generation": False,
        "legacy_random_source": False, "noise": {"min_y": MIN_Y, "height": HEIGHT,
                                                  "size_horizontal": 1, "size_vertical": 2},
        "noise_router": router, "ore_veins_enabled": False, "sea_level": SEA_LEVEL,
        "spawn_target": [climate.parameters() for biome in BIOMES if biome.settlements for climate in biome.climates],
        "surface_rule": surface,
    })


def tree(shape: TreeShape) -> dict:
    base, random_a, random_b = shape.height
    radius_min, radius_max = shape.radius
    radius = radius_min if radius_min == radius_max else {
        "type": "minecraft:uniform", "min_inclusive": radius_min, "max_inclusive": radius_max}
    minimum_size = {"type": "minecraft:two_layers_feature_size", "limit": 1,
                    "lower_size": 0, "upper_size": 1}
    if shape.branching:
        # Fancy limbs perform their own collision checks. Require the full
        # configured height so a crowded site cannot produce a clipped stump.
        minimum_size.update(limit=0, upper_size=0)
    return {"type": "minecraft:tree", "config": {
        "trunk_provider": simple_provider("birch_log", axis="y"),
        "trunk_placer": {"type": "minecraft:fancy_trunk_placer" if shape.branching else (
                         "minecraft:forking_trunk_placer" if shape.forking else "minecraft:straight_trunk_placer"),
                         "base_height": base, "height_rand_a": random_a, "height_rand_b": random_b},
        "foliage_provider": simple_provider("elysium:golden_birch_leaves", distance="7",
                                            persistent="false", waterlogged="false"),
        "foliage_placer": {"type": "minecraft:fancy_foliage_placer" if shape.branching else "minecraft:blob_foliage_placer",
                           "radius": radius, "offset": 4 if shape.branching else 0,
                           "height": shape.foliage_height},
        "dirt_provider": simple_provider("dirt"), "force_dirt": False, "ignore_vines": True,
        "minimum_size": minimum_size,
        "decorators": [{"type": "minecraft:beehive", "probability": 0.03}],
    }}


def patch(provider: dict, tries: int, spread: int = 7) -> dict:
    return {"type": "minecraft:random_patch", "config": {
        "tries": tries, "xz_spread": spread, "y_spread": 3,
        "feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": provider}},
                    "placement": [{"type": "minecraft:block_predicate_filter",
                                   "predicate": {"type": "minecraft:matching_blocks",
                                                 "blocks": "minecraft:air"}}]},
    }}


def placed(feature: str, count: int, tree_feature: bool = False, groves: GrovePattern | None = None) -> dict:
    counter = groves.placement(count) if groves else {"type": "minecraft:count", "count": count}
    modifiers = [counter, {"type": "minecraft:in_square"}]
    if tree_feature:
        modifiers.append({"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0})
    modifiers.extend([{"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR" if tree_feature else "WORLD_SURFACE_WG"},
                      {"type": "minecraft:biome"}])
    if tree_feature:
        modifiers.append({"type": "minecraft:block_predicate_filter",
                          "predicate": {"type": "minecraft:would_survive",
                                        "state": state("elysium:golden_birch_sapling", stage="0")}})
    return {"feature": feature, "placement": modifiers}


def make_biomes(entries: dict[Path, bytes]) -> None:
    for shape in TREE_SHAPES:
        emit(entries, f"worldgen/configured_feature/{shape.id}.json", tree(shape))
    emit(entries, "worldgen/configured_feature/wild_grain_patch.json", patch(simple_provider("elysium:wild_grain"), 96))
    emit(entries, "worldgen/configured_feature/soft_grass_patch.json", patch(simple_provider("short_grass"), 32))
    flowers = {"type": "minecraft:weighted_state_provider", "entries": [
        {"data": state(name), "weight": weight} for name, weight in
        (("dandelion", 5), ("oxeye_daisy", 4), ("azure_bluet", 3), ("cornflower", 1), ("white_tulip", 2))]}
    emit(entries, "worldgen/configured_feature/meadow_flowers.json", patch(flowers, 48, 6))
    emit(entries, "worldgen/configured_feature/autumn_leaves.json", patch(simple_provider("elysium:leaf_litter"), 32, 5))

    for biome in BIOMES:
        vegetation = biome.vegetation
        # RandomSelector checks entries sequentially, so convert absolute shares
        # to conditional probabilities rather than silently reducing tall trees.
        branches = vegetation.branching_tree_chance
        tall = vegetation.tall_tree_chance
        canopy = vegetation.canopy_tree_chance
        assert all(0 <= n <= 1 for n in (branches, tall, canopy)) and branches + tall + canopy <= 1.00001
        choices = []
        remaining = 1.0
        for shape, share in (("canopy_golden_birch", canopy), ("branching_golden_birch", branches),
                              ("tall_golden_birch", tall)):
            if share > 0:
                choices.append({"chance": min(1.0, share / remaining),
                                "feature": {"feature": f"elysium:{shape}", "placement": []}})
                remaining -= share
        mixed_trees = {"type": "minecraft:random_selector", "config": {
            "default": {"feature": "elysium:golden_birch", "placement": []},
            "features": choices}}
        emit(entries, f"worldgen/configured_feature/{biome.id}/trees.json", mixed_trees)
        emit(entries, f"worldgen/placed_feature/{biome.id}/trees.json",
             placed(f"elysium:{biome.id}/trees", vegetation.tree_attempts, True, vegetation.groves))
        if vegetation.riverside_attempts:
            river_trees = placed(f"elysium:{biome.id}/trees", vegetation.riverside_attempts, True,
                                 GrovePattern(0, noise_threshold=-0.05))
            river_trees["placement"].append({"type": "elysium:near_surface_water"})
            emit(entries, f"worldgen/placed_feature/{biome.id}/riverside_trees.json", river_trees)
        for suffix, feature, count in (("flowers", "meadow_flowers", vegetation.flower_patches),
                                       ("grain", "wild_grain_patch", vegetation.grain_patches),
                                       ("grass", "soft_grass_patch", vegetation.grass_patches)):
            emit(entries, f"worldgen/placed_feature/{biome.id}/{suffix}.json", placed(f"elysium:{feature}", count))
        features = [[] for _ in range(11)]
        # Stable order everywhere: trees, flowers, grain, grass. Biome-specific
        # placed-feature IDs keep density choices independent at boundaries.
        features[9] = [f"elysium:{biome.id}/{suffix}" for suffix in ("trees", "flowers", "grain", "grass")]
        if vegetation.riverside_attempts:
            features[9].insert(1, f"elysium:{biome.id}/riverside_trees")
        if vegetation.leaf_patches:
            emit(entries, f"worldgen/placed_feature/{biome.id}/leaves.json",
                 placed("elysium:autumn_leaves", vegetation.leaf_patches))
            features[9].append(f"elysium:{biome.id}/leaves")
        effects = biome.palette.effects()
        if biome.ambient_loop:
            effects["ambient_sound"] = biome.ambient_loop
        spawners = {name: [] for name in ("ambient", "axolotls", "creature", "misc", "monster",
                                        "underground_water_creature", "water_ambient", "water_creature")}
        spawners["creature"] = [spawn.entry() for spawn in biome.creatures]
        spawners["water_ambient"] = [Spawn("salmon", 6, 2, 4).entry()]
        emit(entries, f"worldgen/biome/{biome.id}.json", {
            "has_precipitation": False, "temperature": biome.temperature, "downfall": biome.downfall,
            "effects": effects, "carvers": {}, "features": features,
            "spawn_costs": {}, "spawners": spawners, "creature_spawn_probability": 0.12,
        })
    emit(entries, "tags/worldgen/biome/is_elysium.json", {"replace": False, "values": [biome.key for biome in BIOMES]})


# Minimal typed NBT writer: deterministic, dependency-free, and readable source
# for the original template. No downloaded or copied structure binaries.
@dataclass(frozen=True)
class Tag:
    kind: int
    value: object


def nbt_string(value: str) -> bytes:
    data = value.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def nbt_payload(tag: Tag) -> bytes:
    if tag.kind == 1:
        return struct.pack(">b", tag.value)
    if tag.kind == 3:
        return struct.pack(">i", tag.value)
    if tag.kind == 6:
        return struct.pack(">d", tag.value)
    if tag.kind == 8:
        return nbt_string(tag.value)
    if tag.kind == 9:
        subtype, values = tag.value
        return bytes([subtype]) + struct.pack(">i", len(values)) + b"".join(nbt_payload(Tag(subtype, value)) for value in values)
    if tag.kind == 10:
        return b"".join(bytes([value.kind]) + nbt_string(key) + nbt_payload(value)
                        for key, value in tag.value.items()) + b"\0"
    raise ValueError(f"Unsupported NBT kind {tag.kind}")


def nbt_compound(values: dict) -> Tag:
    return Tag(10, values)


def encode_template(blocks: dict, size: tuple[int, int, int], label: str, entities=()) -> bytes:
    palette, palette_ids, block_list = [], {}, []
    for position, (blockstate, data) in sorted(blocks.items()):
        key = json.dumps(blockstate, sort_keys=True)
        if key not in palette_ids:
            palette_ids[key] = len(palette)
            record = {"Name": Tag(8, blockstate["Name"])}
            if "Properties" in blockstate:
                record["Properties"] = nbt_compound({k: Tag(8, v) for k, v in blockstate["Properties"].items()})
            palette.append(record)
        record = {"pos": Tag(9, (3, list(position))), "state": Tag(3, palette_ids[key])}
        if data:
            record["nbt"] = nbt_compound(data)
        block_list.append(record)
    root = nbt_compound({"DataVersion": Tag(3, 3955), "author": Tag(8, "Elysium"), "layout": Tag(8, label),
                         "size": Tag(9, (3, list(size))), "palette": Tag(9, (10, palette)),
                         "blocks": Tag(9, (10, block_list)), "entities": Tag(9, (10, entities))})
    # GzipFile writes a stable OS byte across Python 3.11/3.12/3.13; bare
    # gzip.compress(..., mtime=0) changed that header between Python releases.
    output = io.BytesIO()
    with gzip.GzipFile(filename="", mode="wb", fileobj=output, mtime=0) as stream:
        stream.write(b"\x0a\x00\x00" + nbt_payload(root))
    return output.getvalue()


def make_hamlet(layout: str = "courtyard") -> bytes:
    if layout not in {"courtyard", "orchard"}:
        raise ValueError(f"Unknown hamlet layout: {layout}")
    blocks: dict[tuple[int, int, int], tuple[dict, dict | None]] = {}

    def put(x, y, z, name, nbt=None, **properties):
        blocks[(x, y, z)] = (state(name, **properties), nbt)

    def fill(x1, y1, z1, x2, y2, z2, name, **properties):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    put(x, y, z, name, **properties)

    # Support and clear only authored footprints. A rectangular air blanket
    # would cut the whole surrounding hillside into a 33 x 33 excavation.
    def pad(x1, z1, x2, z2):
        fill(x1, 0, z1, x2, 0, z2, "dirt")
        fill(x1, 1, z1, x2, 1, z2, "grass_block", snowy="false")

    for x1, z1, x2, z2 in ((14, 0, 18, 32), (0, 14, 32, 18), (12, 12, 20, 20)):
        pad(x1, z1, x2, z2)
        fill(x1, 2, z1, x2, 4, z2, "air")
    fill(14, 1, 0, 18, 1, 32, "dirt_path")
    fill(0, 1, 14, 32, 1, 18, "dirt_path")
    fill(12, 1, 12, 20, 1, 20, "smooth_sandstone")
    fill(12, 2, 12, 20, 6, 20, "air")

    def house(x, z, door_south=True, bed_color="yellow"):
        pad(x, z, x + 8, z + 8)
        fill(x - 1, 2, z - 1, x + 9, 10, z + 9, "air")
        fill(x, 1, z, x + 8, 1, z + 8, "stone_bricks")
        fill(x, 2, z, x + 8, 5, z + 8, "smooth_sandstone")
        fill(x + 1, 2, z + 1, x + 7, 2, z + 7, "birch_planks")
        fill(x + 1, 3, z + 1, x + 7, 5, z + 7, "air")
        for dx in (0, 8):
            for dz in (0, 8):
                fill(x + dx, 2, z + dz, x + dx, 5, z + dz, "birch_log", axis="y")
        # Original stepped gable roof, spruce eaves over pale walls.
        for layer in range(5):
            left, right = x - 1 + layer, x + 9 - layer
            fill(left, 6 + layer, z - 1, right, 6 + layer, z + 9, "spruce_planks")
            fill(left, 6 + layer, z - 1, left, 6 + layer, z + 9,
                 "spruce_stairs", facing="east", half="bottom", shape="straight", waterlogged="false")
            fill(right, 6 + layer, z - 1, right, 6 + layer, z + 9,
                 "spruce_stairs", facing="west", half="bottom", shape="straight", waterlogged="false")
        dz = z + 8 if door_south else z
        facing = "south" if door_south else "north"
        for y, half in ((3, "lower"), (4, "upper")):
            put(x + 4, y, dz, "birch_door", facing=facing, half=half, hinge="left", open="false", powered="false")
        put(x + 4, 2, dz + (1 if door_south else -1), "birch_stairs", facing="north" if door_south else "south",
            half="bottom", shape="straight", waterlogged="false")
        for dx in (2, 6):
            put(x + dx, 4, z, "glass")
            put(x + dx, 4, z + 8, "glass")
        for dz2 in (2, 6):
            put(x, 4, z + dz2, "glass")
            put(x + 8, 4, z + dz2, "glass")
        put(x + 2, 3, z + 2, "crafting_table")
        put(x + 6, 3, z + 2, "barrel", facing="up", open="false")
        for bx in (2, 5):
            put(x + bx, 3, z + 5, f"{bed_color}_bed", facing="south", part="foot", occupied="false")
            put(x + bx, 3, z + 6, f"{bed_color}_bed", facing="south", part="head", occupied="false")
        put(x + 4, 5, z + 4, "lantern", hanging="true", waterlogged="false")

    house(2, 2)
    if layout == "courtyard":
        house(22, 22, False, "white")
    else:
        house(22, 2, True, "white")
        house(2, 22, False, "orange")

    def grain_plot(x, z):
        pad(x, z, x + 8, z + 8)
        fill(x, 2, z, x + 8, 4, z + 8, "air")
        fill(x, 1, z, x + 8, 1, z + 8, "stripped_birch_log", axis="y")
        for dx in range(1, 8):
            for dz in range(1, 8):
                if dx == 4:
                    put(x + dx, 1, z + dz, "water", level="0")
                    put(x + dx, 2, z + dz, "air")
                else:
                    put(x + dx, 1, z + dz, "farmland", moisture="7")
                    put(x + dx, 2, z + dz, "wheat", age="7")
        put(x, 2, z + 4, "composter", level="0")

    if layout == "courtyard":
        grain_plot(22, 2)
        grain_plot(2, 22)
        # Sheltered central fountain with an old bell whose story comes later.
        fill(14, 2, 14, 18, 2, 18, "smooth_sandstone")
        fill(15, 2, 15, 17, 2, 17, "water", level="0")
        for x, z in ((13, 13), (19, 13), (13, 19), (19, 19)):
            fill(x, 2, z, x, 5, z, "birch_log", axis="y")
        fill(12, 6, 12, 20, 6, 20, "birch_slab", type="bottom", waterlogged="false")
        put(16, 5, 16, "lantern", hanging="true", waterlogged="false")
    else:
        grain_plot(22, 22)
        # Distinct second plan: three cottages face a living village tree,
        # one shared crop plot, low benches and an uncovered bright court.
        fill(15, 1, 15, 17, 1, 17, "grass_block", snowy="false")
        fill(16, 2, 16, 16, 8, 16, "birch_log", axis="y")
        for y, radius in ((6, 3), (7, 3), (8, 2), (9, 2), (10, 1)):
            for dx in range(-radius, radius + 1):
                for dz in range(-radius, radius + 1):
                    if (dx or dz or y > 8) and abs(dx) + abs(dz) <= radius + 1:
                        put(16 + dx, y, 16 + dz, "elysium:golden_birch_leaves",
                            distance="1", persistent="true", waterlogged="false")
        for x, z, facing in ((13, 15, "east"), (13, 16, "east"), (19, 15, "west"), (19, 16, "west")):
            put(x, 2, z, "birch_stairs", facing=facing, half="bottom", shape="straight", waterlogged="false")
        put(13, 2, 18, "lantern", hanging="false", waterlogged="false")
        put(19, 2, 18, "lantern", hanging="false", waterlogged="false")
    put(16, 2, 12, "chiseled_sandstone")
    put(16, 3, 12, "bell", facing="south", attachment="floor", powered="false")
    for x, z in ((12, 4), (20, 28), (4, 12), (28, 20)):
        pad(x, z, x, z)
        fill(x, 2, z, x, 3, z, "birch_fence", north="false", south="false", east="false", west="false", waterlogged="false")
        put(x, 4, z, "lantern", hanging="false", waterlogged="false")
    for x, z in ((5, 17), (27, 15)):
        pad(x - 2, z - 2, x + 2, z + 2)
        fill(x, 2, z, x, 6, z, "birch_log", axis="y")
        for y, radius in ((5, 2), (6, 2), (7, 1)):
            for dx in range(-radius, radius + 1):
                for dz in range(-radius, radius + 1):
                    if (dx or dz) and abs(dx) + abs(dz) < radius * 2:
                        put(x + dx, y, z + dz, "elysium:golden_birch_leaves",
                            distance="1", persistent="true", waterlogged="false")
        put(x, 7, z, "elysium:golden_birch_leaves", distance="1", persistent="true", waterlogged="false")
    for x, z in ((12, 24), (20, 8), (10, 19), (22, 13)):
        put(x, 2, z, "hay_block", axis="y")

    villagers = []
    residents = ((7.5, 6.5, "farmer"), (26.5, 26.5, "farmer"), (18.5, 21.5, "none")) if layout == "courtyard" else (
        (7.5, 6.5, "farmer"), (26.5, 6.5, "none"), (6.5, 26.5, "none"), (18.5, 21.5, "farmer"))
    for x, z, job in residents:
        villagers.append({"pos": Tag(9, (6, [x, 3.0, z])), "blockPos": Tag(9, (3, [int(x), 3, int(z)])),
                          "nbt": nbt_compound({"id": Tag(8, "minecraft:villager"), "PersistenceRequired": Tag(1, 1),
                                               "VillagerData": nbt_compound({"type": Tag(8, "minecraft:plains"),
                                                                            "profession": Tag(8, f"minecraft:{job}"),
                                                                            "level": Tag(3, 1)})})})
    return encode_template(blocks, (33, 11, 33), layout, villagers)


@dataclass(frozen=True)
class SiteDefinition:
    id: str
    templates: tuple[str, ...]
    biomes: str
    radius: int
    max_relief: int
    min_height: int = 0
    view_drop: int = 0
    water_approach: bool = False
    require_water_approach: bool = False


def emit_site(entries, site: SiteDefinition):
    emit(entries, f"worldgen/structure/{site.id}.json", {
        "type": "elysium:landscape_site", "biomes": site.biomes,
        "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none",
        "templates": [f"elysium:{name}" for name in site.templates],
        "radius": site.radius, "max_relief": site.max_relief,
        "min_height_above_sea": site.min_height, "view_drop": site.view_drop,
        "water_approach": site.water_approach,
        "require_water_approach": site.require_water_approach,
    })


def make_settlements(entries: dict[Path, bytes]) -> None:
    from settlement_templates import house, plaza, WATERSIDE, NORDIC
    modules = (house(state, "waterside_cottage", WATERSIDE),
               house(state, "mountain_cottage", NORDIC),
               house(state, "mountain_cottage_store", NORDIC, variant=1),
               house(state, "mountain_hall", NORDIC, radius=8, hall=True), plaza(state))
    for module in modules:
        c = module.size[0] // 2
        spawn_x = c - 1 if module.name == "mountain_hall" else c
        villagers = [] if module.name == "mountain_plaza" else [{
            "pos": Tag(9, (6, [spawn_x + 0.5, 3.0, c + 1.5])), "blockPos": Tag(9, (3, [spawn_x, 3, c + 1])),
            "nbt": nbt_compound({"id": Tag(8, "minecraft:villager"), "PersistenceRequired": Tag(1, 1),
                "VillagerData": nbt_compound({"type": Tag(8, "minecraft:taiga" if module.name.startswith("mountain") else "minecraft:plains"),
                    "profession": Tag(8, "minecraft:none"), "level": Tag(3, 1)})})}]
        entries[DATA / "structure" / f"{module.name}.nbt"] = encode_template(module.blocks, module.size, module.name, villagers)
    emit_site(entries, SiteDefinition("waterside_cottage", ("waterside_cottage",),
              "#elysium:has_structure/waterside_cottage", 6, 2, require_water_approach=True))
    emit(entries, "worldgen/structure_set/waterside_cottage.json", {
        "structures": [{"structure": "elysium:waterside_cottage", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 18, "separation": 7,
            "salt": 459270831, "exclusion_zone": {"other_set": "elysium:harvest_hamlet", "chunk_count": 3}}})
    emit(entries, "worldgen/structure/mountain_town.json", {
        "type": "elysium:mountain_town", "biomes": "#elysium:has_structure/mountain_town",
        "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none",
        "plaza": "elysium:mountain_plaza",
        "hall": {"template": "elysium:mountain_hall", "radius": 8, "max_relief": 4},
        "cottages": [{"template": f"elysium:{name}", "radius": 6, "max_relief": 4}
                     for name in ("mountain_cottage", "mountain_cottage_store")],
        "minimum_cottages": 3, "maximum_cottages": 5, "min_height_above_sea": 24})
    emit(entries, "worldgen/structure_set/mountain_town.json", {
        "structures": [{"structure": "elysium:mountain_town", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 38, "separation": 14,
                      "salt": 760491532, "spread_type": "linear"}})
    settlements = sorted({name for biome in BIOMES for name in biome.settlements})
    for name in settlements:
        emit(entries, f"tags/worldgen/biome/has_structure/{name}.json",
             {"replace": False, "values": [biome.key for biome in BIOMES if name in biome.settlements]})
        if name != "harvest_hamlet":
            continue  # Other settlements use the module definitions above.
        emit_site(entries, SiteDefinition(name, (name, f"{name}_orchard"),
                                          f"#elysium:has_structure/{name}", 16, 3, water_approach=True))
        emit(entries, f"worldgen/structure_set/{name}.json", {
            "structures": [{"structure": f"elysium:{name}", "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": 28, "separation": 10,
                          "salt": 192837461, "spread_type": "linear"},
        })
        emit(entries, f"worldgen/template_pool/{name}/start.json", {
            "fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
                "element_type": "minecraft:single_pool_element", "location": f"elysium:{template}",
                "processors": {"processors": []}, "projection": "rigid"}}
                for template in (name, f"{name}_orchard")],
        })
        # 1.21 uses singular `structure`, not the older `structures` directory.
        entries[DATA / "structure" / f"{name}.nbt"] = make_hamlet()
        entries[DATA / "structure" / f"{name}_orchard.nbt"] = make_hamlet("orchard")
    emit(entries, "worldgen/structure/elysian_bridge.json", {
        "type": "elysium:elysian_bridge", "biomes": "#elysium:is_elysium",
        "step": "surface_structures", "terrain_adaptation": "none", "spawn_overrides": {},
    })
    emit(entries, "worldgen/structure_set/elysian_bridge.json", {
        "structures": [{"structure": "elysium:elysian_bridge", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 12, "separation": 5,
                      "salt": 842763195, "spread_type": "linear",
                      "exclusion_zone": {"other_set": "elysium:harvest_hamlet", "chunk_count": 3}},
    })
    from landmark_templates import spring, lookout
    sites = (SiteDefinition("elder_spring", ("elder_spring",), "#elysium:is_elysium", 9, 2),
             SiteDefinition("sunlit_lookout", ("sunlit_lookout",), "#elysium:is_elysium", 6, 3,
                            min_height=8, view_drop=8))
    for site in sites:
        emit_site(entries, site)
    for template in (spring(state), lookout(state)):
        entries[DATA / "structure" / f"{template.name}.nbt"] = encode_template(
            template.blocks, template.size, template.name)
    # One shared candidate grid keeps the two kinds of landmark apart. Hamlet
    # candidates take priority; exclusions do not trigger neighbouring chunk loads.
    emit(entries, "worldgen/structure_set/quiet_landmarks.json", {
        "structures": [{"structure": f"elysium:{site.id}", "weight": weight}
                       for site, weight in zip(sites, (3, 2))],
        "placement": {"type": "minecraft:random_spread", "spacing": 20, "separation": 8,
                      "salt": 62419387, "spread_type": "linear",
                      "exclusion_zone": {"other_set": "elysium:harvest_hamlet", "chunk_count": 3}},
    })
    emit(entries, "tags/worldgen/structure/is_elysium.json",
         {"replace": False, "values": [f"elysium:{name}" for name in settlements]
          + ["elysium:elysian_bridge"] + [f"elysium:{site.id}" for site in sites]})


def emit(entries: dict[Path, bytes], relative: str, data: dict) -> None:
    path = DATA / relative
    if path in entries:
        raise ValueError(f"Duplicate generated resource: {relative}")
    entries[path] = (json.dumps(data, indent=2, ensure_ascii=False) + "\n").encode()


def make_test_preset(entries: dict[Path, bytes], dimension: dict) -> None:
    """GameTestServer builds its world from the flat preset, not data dimensions.

    The Gradle jar task excludes this development-only override. Reuse the exact
    production dimension dictionary so the test never drifts from the catalog.
    """
    preset = {"dimensions": {
        "minecraft:overworld": {"type": "minecraft:overworld", "generator": {
            "type": "minecraft:flat", "settings": {
                "biome": "minecraft:plains", "features": False, "lakes": False,
                "layers": [{"block": "minecraft:bedrock", "height": 1},
                           {"block": "minecraft:dirt", "height": 2},
                           {"block": "minecraft:grass_block", "height": 1}],
                "structure_overrides": ["minecraft:strongholds", "minecraft:villages"],
            }}},
        "minecraft:the_end": {"type": "minecraft:the_end", "generator": {
            "type": "minecraft:noise", "biome_source": {"type": "minecraft:the_end"},
            "settings": "minecraft:end"}},
        "minecraft:the_nether": {"type": "minecraft:the_nether", "generator": {
            "type": "minecraft:noise", "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"},
            "settings": "minecraft:nether"}},
        "elysium:elysium": dimension,
    }}
    path = DATA.parent / "minecraft/worldgen/world_preset/flat.json"
    entries[path] = (json.dumps(preset, indent=2, ensure_ascii=False) + "\n").encode()


def generate() -> dict[Path, bytes]:
    assert len({biome.id for biome in BIOMES}) == len(BIOMES), "Duplicate biome resource ID"
    entries: dict[Path, bytes] = {}
    make_biomes(entries)
    make_noise(entries)
    make_settlements(entries)
    emit(entries, "dimension_type/elysium.json", {
        "fixed_time": GOLDEN_HOUR, "has_skylight": True, "has_ceiling": False,
        "ultrawarm": False, "natural": True, "coordinate_scale": 1.0,
        "bed_works": True, "respawn_anchor_works": False, "piglin_safe": True,
        "has_raids": False, "logical_height": HEIGHT, "min_y": MIN_Y, "height": HEIGHT,
        "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld",
        "ambient_light": 0.05, "monster_spawn_block_light_limit": 0, "monster_spawn_light_level": 0,
    })
    dimension = {
        "type": "elysium:elysium", "generator": {"type": "minecraft:noise", "settings": "elysium:elysium",
            "biome_source": {"type": "minecraft:multi_noise", "biomes": [
                {"biome": biome.key, "parameters": climate.parameters()} for biome in BIOMES for climate in biome.climates]}}}
    emit(entries, "dimension/elysium.json", dimension)
    make_test_preset(entries, dimension)
    validate(entries)
    return entries


def validate(entries: dict[Path, bytes]) -> None:
    """Cross-resource guarantees; Minecraft's registry codecs remain authoritative."""
    resources = {str(path.relative_to(DATA)): json.loads(content) for path, content in entries.items()
                 if path.suffix == ".json" and path.is_relative_to(DATA)}
    for biome in BIOMES:
        body = resources[f"worldgen/biome/{biome.id}.json"]
        assert not body["spawners"]["monster"] and not body["carvers"]
        for feature in body["features"][9]:
            assert f"worldgen/placed_feature/{feature.split(':')[1]}.json" in resources
        assert all(-1.2 <= climate.erosion[0] <= climate.erosion[1] <= 1.2 for climate in biome.climates)
    for name, resource in resources.items():
        if name.startswith("worldgen/placed_feature/"):
            feature = resource["feature"]
            assert feature.startswith("elysium:")
            assert f"worldgen/configured_feature/{feature.split(':')[1]}.json" in resources
    terrain = resources["worldgen/noise_settings/elysium.json"]
    assert not terrain["aquifers_enabled"] and not terrain["ore_veins_enabled"]
    assert "cave" not in json.dumps(terrain)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if any generated output is missing or stale")
    args = parser.parse_args()
    entries = generate()
    stale = []
    for path, content in entries.items():
        if args.check:
            if not path.is_file() or path.read_bytes() != content:
                stale.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(content)
    if stale:
        raise SystemExit("Worldgen outputs are stale; run tools/generate_worldgen.py:\n" + "\n".join(stale))
    print(f"{'Verified' if args.check else 'Generated'} {len(entries)} resources for {len(BIOMES)} parameterized biomes.")


if __name__ == "__main__":
    main()
