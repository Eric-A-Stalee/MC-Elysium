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
    foliage: int = 0xEAC447
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
class Vegetation:
    tree_attempts: int
    grain_patches: int
    flower_patches: int
    grass_patches: int
    tall_tree_chance: float


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
    climate: ClimateBox
    palette: Palette
    vegetation: Vegetation
    creatures: tuple[Spawn, ...]
    temperature: float = 0.85
    downfall: float = 0.45
    pale_cliffs: bool = False
    settlements: tuple[str, ...] = ()

    @property
    def key(self) -> str:
        return f"elysium:{self.id}"


FARM_CREATURES = (Spawn("sheep", 12), Spawn("cow", 8), Spawn("chicken", 8),
                  Spawn("rabbit", 5, 2, 3))
# The climate boxes tile humidity/erosion space. Highlands follow the same
# low-erosion signal that raises terrain, rather than an unrelated biome roll.
BIOMES = (
    BiomeDefinition("golden_fields", "Golden Fields",
                    ClimateBox((-1.0, 0.10), (-0.22, 1.0)),
                    Palette(0xA4B75F), Vegetation(1, 12, 2, 2, 0.20),
                    FARM_CREATURES + (Spawn("horse", 3, 2, 4),),
                    settlements=("harvest_hamlet",)),
    BiomeDefinition("golden_birch_woods", "Golden Birch Woods",
                    ClimateBox((0.10, 1.0), (-0.22, 1.0)),
                    Palette(0x8EA653, fog=0xE6D6AA),
                    Vegetation(9, 3, 3, 5, 0.65), FARM_CREATURES,
                    downfall=0.65),
    BiomeDefinition("elysian_highlands", "Elysian Highlands",
                    ClimateBox((-1.0, 1.0), (-1.0, -0.22)),
                    Palette(0x9DAD69, sky=0xBCD6E9, fog=0xF1E2C7),
                    Vegetation(3, 4, 2, 3, 0.35),
                    (Spawn("sheep", 10), Spawn("rabbit", 5, 2, 3)),
                    temperature=0.8, downfall=0.35, pale_cliffs=True),
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
    river = spline(unary("abs", "elysium:ridges"),
                   ((0.0, 0.0), (0.018, 0.0), (0.055, 0.28), (0.12, 1.0), (1.2, 1.0)))
    mountains = spline("elysium:erosion",
                       ((-1.0, 118.0), (-0.7, 104.0), (-0.45, 58.0),
                        (-0.22, 12.0), (0.0, 3.0), (0.5, 0.0), (1.0, 0.0)))
    relief = {"type": "minecraft:noise", "noise": "elysium:gentle_relief",
              "xz_scale": 0.25, "y_scale": 0.0}
    upland = binary("add", 17.0,
                    binary("add", binary("mul", 9.0, "elysium:continents"),
                           binary("add", mountains, binary("mul", 3.0, relief))))
    terrain_height = unary("flat_cache", binary("add", 60.0, binary("mul", river, upland)))
    terrain_density = binary("mul", 0.05, binary("add", "elysium:terrain_height",
                                                    gradient(MIN_Y, 320, 64.0, -320.0)))
    emit(entries, "worldgen/noise/gentle_relief.json", {"firstOctave": -4, "amplitudes": [1.0, 0.5]})
    emit(entries, "worldgen/density_function/terrain_height.json", terrain_height)
    emit(entries, "worldgen/density_function/terrain_density.json", terrain_density)

    cliff_biomes = [biome.key for biome in BIOMES if biome.pale_cliffs]
    pale_cliffs = condition({"type": "minecraft:biome", "biome_is": cliff_biomes},
                            condition({"type": "minecraft:steep"}, block_rule("calcite")))
    # Submerged beds are sand; land keeps olive grass and dirt. No deepslate,
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
        "spawn_target": [biome.climate.parameters() for biome in BIOMES if biome.settlements],
        "surface_rule": surface,
    })


def tree(tall: bool = False) -> dict:
    return {"type": "minecraft:tree", "config": {
        "trunk_provider": simple_provider("birch_log", axis="y"),
        "trunk_placer": {"type": "minecraft:straight_trunk_placer",
                         "base_height": 8 if tall else 5, "height_rand_a": 3 if tall else 2,
                         "height_rand_b": 1 if tall else 0},
        "foliage_provider": simple_provider("elysium:golden_birch_leaves", distance="7",
                                            persistent="false", waterlogged="false"),
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "radius": 3 if tall else 2,
                           "offset": 0, "height": 4 if tall else 3},
        "dirt_provider": simple_provider("dirt"), "force_dirt": False, "ignore_vines": True,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1,
                         "lower_size": 0, "upper_size": 2 if tall else 1},
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


def placed(feature: str, count: int, tree_feature: bool = False) -> dict:
    modifiers = [{"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"}]
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
    emit(entries, "worldgen/configured_feature/golden_birch.json", tree())
    emit(entries, "worldgen/configured_feature/tall_golden_birch.json", tree(tall=True))
    emit(entries, "worldgen/configured_feature/wild_grain_patch.json", patch(simple_provider("elysium:wild_grain"), 96))
    emit(entries, "worldgen/configured_feature/soft_grass_patch.json", patch(simple_provider("short_grass"), 32))
    flowers = {"type": "minecraft:weighted_state_provider", "entries": [
        {"data": state(name), "weight": weight} for name, weight in
        (("dandelion", 5), ("oxeye_daisy", 4), ("azure_bluet", 3), ("cornflower", 1), ("white_tulip", 2))]}
    emit(entries, "worldgen/configured_feature/meadow_flowers.json", patch(flowers, 48, 6))

    for biome in BIOMES:
        vegetation = biome.vegetation
        mixed_trees = {"type": "minecraft:random_selector", "config": {
            "default": {"feature": "elysium:golden_birch", "placement": []},
            "features": [{"chance": vegetation.tall_tree_chance,
                          "feature": {"feature": "elysium:tall_golden_birch", "placement": []}}]}}
        emit(entries, f"worldgen/configured_feature/{biome.id}/trees.json", mixed_trees)
        emit(entries, f"worldgen/placed_feature/{biome.id}/trees.json",
             placed(f"elysium:{biome.id}/trees", vegetation.tree_attempts, True))
        for suffix, feature, count in (("flowers", "meadow_flowers", vegetation.flower_patches),
                                       ("grain", "wild_grain_patch", vegetation.grain_patches),
                                       ("grass", "soft_grass_patch", vegetation.grass_patches)):
            emit(entries, f"worldgen/placed_feature/{biome.id}/{suffix}.json", placed(f"elysium:{feature}", count))
        features = [[] for _ in range(11)]
        # Stable order everywhere: trees, flowers, grain, grass. Biome-specific
        # placed-feature IDs keep density choices independent at boundaries.
        features[9] = [f"elysium:{biome.id}/{suffix}" for suffix in ("trees", "flowers", "grain", "grass")]
        spawners = {name: [] for name in ("ambient", "axolotls", "creature", "misc", "monster",
                                        "underground_water_creature", "water_ambient", "water_creature")}
        spawners["creature"] = [spawn.entry() for spawn in biome.creatures]
        spawners["water_ambient"] = [Spawn("salmon", 6, 2, 4).entry()]
        emit(entries, f"worldgen/biome/{biome.id}.json", {
            "has_precipitation": False, "temperature": biome.temperature, "downfall": biome.downfall,
            "effects": biome.palette.effects(), "carvers": {}, "features": features,
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
    villagers = []
    residents = ((7.5, 6.5, "farmer"), (26.5, 26.5, "farmer"), (18.5, 21.5, "none")) if layout == "courtyard" else (
        (7.5, 6.5, "farmer"), (26.5, 6.5, "none"), (6.5, 26.5, "none"), (18.5, 21.5, "farmer"))
    for x, z, job in residents:
        villagers.append({"pos": Tag(9, (6, [x, 3.0, z])), "blockPos": Tag(9, (3, [int(x), 3, int(z)])),
                          "nbt": nbt_compound({"id": Tag(8, "minecraft:villager"), "PersistenceRequired": Tag(1, 1),
                                               "VillagerData": nbt_compound({"type": Tag(8, "minecraft:plains"),
                                                                            "profession": Tag(8, f"minecraft:{job}"),
                                                                            "level": Tag(3, 1)})})})
    root = nbt_compound({"DataVersion": Tag(3, 3955), "author": Tag(8, "Elysium"), "layout": Tag(8, layout),
                         "size": Tag(9, (3, [33, 11, 33])), "palette": Tag(9, (10, palette)),
                         "blocks": Tag(9, (10, block_list)), "entities": Tag(9, (10, villagers))})
    # GzipFile writes a stable OS byte across Python 3.11/3.12/3.13; bare
    # gzip.compress(..., mtime=0) changed that header between Python releases.
    output = io.BytesIO()
    with gzip.GzipFile(filename="", mode="wb", fileobj=output, mtime=0) as stream:
        stream.write(b"\x0a\x00\x00" + nbt_payload(root))
    return output.getvalue()


def make_settlements(entries: dict[Path, bytes]) -> None:
    settlements = sorted({name for biome in BIOMES for name in biome.settlements})
    for name in settlements:
        if name != "harvest_hamlet":
            raise ValueError(f"Unknown settlement geometry: {name}")
        emit(entries, f"tags/worldgen/biome/has_structure/{name}.json",
             {"replace": False, "values": [biome.key for biome in BIOMES if name in biome.settlements]})
        emit(entries, f"worldgen/structure/{name}.json", {
            "type": "minecraft:jigsaw", "biomes": f"#elysium:has_structure/{name}",
            "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "beard_thin",
            # SinglePoolElement ground-level delta is 1. Our templates reserve
            # Y=0 for soil and use Y=1 as the court, so offset -1 aligns that
            # court with the last solid terrain block rather than one above it.
            "start_pool": f"elysium:{name}/start", "size": 1, "start_height": {"absolute": -1},
            "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 80,
            "use_expansion_hack": False,
        })
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
    emit(entries, "tags/worldgen/structure/is_elysium.json",
         {"replace": False, "values": [f"elysium:{name}" for name in settlements] + ["elysium:elysian_bridge"]})


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
                {"biome": biome.key, "parameters": biome.climate.parameters()} for biome in BIOMES]}}}
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
        assert -1.2 <= biome.climate.erosion[0] <= biome.climate.erosion[1] <= 1.2
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
