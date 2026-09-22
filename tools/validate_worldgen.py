#!/usr/bin/env python3
"""Validate climate coverage, generation ordering, and the actual shipped NBT.

This is an independent structural check, not a replacement for Minecraft's
registry codecs or a visual playtest. Python standard library only.
"""
from __future__ import annotations

import argparse
import gzip
import itertools
import json
import struct
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/elysium"


def read_json(path: str) -> dict:
    return json.loads((DATA / path).read_text())


def bounds(value) -> tuple[float, float]:
    return tuple(value) if isinstance(value, list) else (value, value)


def validate_climates() -> int:
    dimension = read_json("dimension/elysium.json")
    fixture = json.loads((DATA.parent / "minecraft/worldgen/world_preset/flat.json").read_text())
    assert fixture["dimensions"]["elysium:elysium"] == dimension, "GameTest world differs from production Elysium"
    source = dimension["generator"]["biome_source"]
    assert source["type"] == "minecraft:multi_noise"
    entries = source["biomes"]
    assert len({entry["biome"] for entry in entries}) == len(entries)
    for entry in entries:
        parameters = entry["parameters"]
        assert parameters["offset"] == 0
        for axis in ("temperature", "continentalness", "depth", "weirdness"):
            low, high = bounds(parameters[axis])
            assert low <= -1 and high >= 1, f"Uncovered {axis}: {entry}"
        name = entry["biome"].split(":", 1)[1]
        assert (DATA / f"worldgen/biome/{name}.json").is_file()
    samples = {}
    for axis in ("humidity", "erosion"):
        boundaries = sorted({v for entry in entries for v in bounds(entry["parameters"][axis])})
        samples[axis] = sorted(set(boundaries + [(a + b) / 2 for a, b in zip(boundaries, boundaries[1:])]))
        assert boundaries[0] <= -1 and boundaries[-1] >= 1
    checked = 0
    for humidity, erosion in itertools.product(samples["humidity"], samples["erosion"]):
        matches = [entry for entry in entries if
                   bounds(entry["parameters"]["humidity"])[0] <= humidity <= bounds(entry["parameters"]["humidity"])[1]
                   and bounds(entry["parameters"]["erosion"])[0] <= erosion <= bounds(entry["parameters"]["erosion"])[1]]
        assert matches, f"Climate hole at humidity {humidity}, erosion {erosion}"
        # A shared boundary may tie; the interior of two boxes may not overlap.
        strict = [entry for entry in matches if
                  bounds(entry["parameters"]["humidity"])[0] < humidity < bounds(entry["parameters"]["humidity"])[1]
                  and bounds(entry["parameters"]["erosion"])[0] < erosion < bounds(entry["parameters"]["erosion"])[1]]
        assert len(strict) <= 1, f"Interior climate overlap at {humidity}, {erosion}"
        checked += 1
    return checked


def validate_features() -> int:
    graphs = [dict() for _ in range(11)]
    features = set()
    for path in sorted((DATA / "worldgen/biome").glob("*.json")):
        biome = json.loads(path.read_text())
        assert not biome["carvers"] and not biome["spawners"]["monster"]
        assert not biome["has_precipitation"]
        assert len(biome["features"]) == 11
        for index, stage in enumerate(biome["features"]):
            assert len(stage) == len(set(stage)), f"Duplicate feature in {path}"
            for feature in stage:
                namespace, name = feature.split(":", 1)
                assert namespace == "elysium"
                placed = read_json(f"worldgen/placed_feature/{name}.json")
                configured_namespace, configured = placed["feature"].split(":", 1)
                assert configured_namespace == "elysium"
                assert (DATA / f"worldgen/configured_feature/{configured}.json").is_file()
                features.add(feature)
                graphs[index].setdefault(feature, set())
            for first, second in zip(stage, stage[1:]):
                graphs[index][first].add(second)
    for graph in graphs:
        visiting, complete = set(), set()

        def visit(node):
            assert node not in visiting, f"Cross-biome feature ordering cycle at {node}"
            if node in complete:
                return
            visiting.add(node)
            for child in graph[node]:
                visit(child)
            visiting.remove(node)
            complete.add(node)

        for node in graph:
            visit(node)
    return len(features)


class NbtReader:
    """Small general big-endian NBT reader; all twelve standard tag types."""
    def __init__(self, data: bytes):
        self.data = data
        self.cursor = 0

    def take(self, length: int) -> bytes:
        assert 0 <= length <= len(self.data) - self.cursor, "Truncated NBT"
        result = self.data[self.cursor:self.cursor + length]
        self.cursor += length
        return result

    def number(self, code: str):
        return struct.unpack(">" + code, self.take(struct.calcsize(">" + code)))[0]

    def string(self) -> str:
        return self.take(self.number("H")).decode("utf-8")

    def payload(self, kind: int):
        numeric = {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}
        if kind in numeric:
            return self.number(numeric[kind])
        if kind == 7:
            return self.take(self.number("i"))
        if kind == 8:
            return self.string()
        if kind == 9:
            subtype, count = self.number("B"), self.number("i")
            assert count >= 0
            return [self.payload(subtype) for _ in range(count)]
        if kind == 10:
            result = {}
            while (subtype := self.number("B")) != 0:
                name = self.string()
                assert name not in result, f"Duplicate NBT field {name}"
                result[name] = self.payload(subtype)
            return result
        if kind in (11, 12):
            count = self.number("i")
            assert count >= 0
            return [self.number("i" if kind == 11 else "q") for _ in range(count)]
        raise AssertionError(f"Unknown NBT tag type {kind}")

    def root(self) -> dict:
        assert self.number("B") == 10, "Structure NBT root must be a compound"
        self.string()
        result = self.payload(10)
        assert self.cursor == len(self.data), "Trailing garbage after structure NBT"
        return result


def validate_hamlet(filename: str) -> tuple[int, int, int]:
    root = NbtReader(gzip.decompress((DATA / "structure" / filename).read_bytes())).root()
    assert root["DataVersion"] == 3955
    assert root["size"] == [33, 11, 33]
    layout = root["layout"]
    assert layout in {"courtyard", "orchard"}
    palette, blocks = root["palette"], {}
    for entry in root["blocks"]:
        pos = tuple(entry["pos"])
        assert len(pos) == 3 and all(0 <= value < limit for value, limit in zip(pos, root["size"]))
        assert pos not in blocks, f"Duplicate structure position {pos}"
        assert 0 <= entry["state"] < len(palette)
        blocks[pos] = palette[entry["state"]]
    air = "minecraft:air"
    for (x, y, z), block in blocks.items():
        if block["Name"] == air:
            # Air may clear authored houses, paths, crop plots, and the court;
            # the template must not erase an entire bounding-box air volume.
            house_origins = ((2, 2), (22, 22)) if layout == "courtyard" else ((2, 2), (22, 2), (2, 22))
            house = any(hx - 1 <= x <= hx + 9 and hz - 1 <= z <= hz + 9 for hx, hz in house_origins)
            path = (14 <= x <= 18 or 14 <= z <= 18) and y <= 4
            court = 12 <= x <= 20 and 12 <= z <= 20 and y <= 6
            plot_origins = ((22, 2), (2, 22)) if layout == "courtyard" else ((22, 22),)
            plots = any(px <= x <= px + 8 and pz <= z <= pz + 8 for px, pz in plot_origins) and y <= 4
            assert house or path or court or plots, f"Air carves unauthored ground at {(x, y, z)}"
        if block["Name"] == "minecraft:birch_door" and block["Properties"]["half"] == "lower":
            upper = blocks[(x, y + 1, z)]
            assert upper["Name"] == block["Name"] and upper["Properties"]["half"] == "upper"
            assert blocks[(x, y - 1, z)]["Name"] != air
        if block["Name"].endswith("_bed") and block["Properties"]["part"] == "foot":
            facing = block["Properties"]["facing"]
            dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
            head = blocks[(x + dx, y, z + dz)]
            assert head["Name"] == block["Name"] and head["Properties"]["part"] == "head"
            assert blocks[(x, y + 1, z)]["Name"] == air
    for x, z in ((0, 0), (0, 32), (32, 0), (32, 32)):
        assert not any((x, y, z) in blocks for y in range(11)), "Unauthored corner was altered"
    assert len(root["entities"]) == (3 if layout == "courtyard" else 4)
    for entity in root["entities"]:
        assert entity["nbt"]["id"] == "minecraft:villager"
        assert all(0 <= value < limit for value, limit in zip(entity["pos"], root["size"]))
        x, y, z = (int(value) for value in entity["pos"])
        assert blocks.get((x, y, z), {"Name": air})["Name"] == air
        assert blocks.get((x, y + 1, z), {"Name": air})["Name"] == air
    assert sum(block["Name"] == "minecraft:wheat" for block in blocks.values()) == (84 if layout == "courtyard" else 42)
    assert sum(block["Name"].endswith("_bed") for block in blocks.values()) == (8 if layout == "courtyard" else 12)
    return len(blocks), len(palette), len(root["entities"])


def validate_terrain() -> None:
    noise = read_json("worldgen/noise_settings/elysium.json")
    assert not noise["aquifers_enabled"] and not noise["ore_veins_enabled"]
    assert noise["default_block"]["Name"] == "minecraft:stone"
    density = read_json("worldgen/density_function/terrain_density.json")
    assert density["type"] == "minecraft:mul" and density["argument1"] > 0
    combined = density["argument2"]
    assert combined["type"] == "minecraft:add" and combined["argument1"] == "elysium:terrain_height"
    vertical = combined["argument2"]
    assert vertical["type"] == "minecraft:y_clamped_gradient"
    assert vertical["from_y"] < vertical["to_y"] and vertical["from_value"] > vertical["to_value"]
    assert vertical["from_y"] <= noise["noise"]["min_y"]
    assert vertical["to_y"] >= noise["noise"]["min_y"] + noise["noise"]["height"]
    height = read_json("worldgen/density_function/terrain_height.json")

    def check_horizontal(value, seen=frozenset()):
        if isinstance(value, dict):
            assert value.get("type") != "minecraft:y_clamped_gradient"
            if value.get("type") in {"minecraft:noise", "minecraft:shifted_noise"}:
                assert value["y_scale"] == 0
            if value.get("type") == "minecraft:shifted_noise":
                assert value["shift_y"] == 0
            for key, child in value.items():
                if key in {"type", "noise"}:
                    continue
                if key == "argument" and value.get("type") in {"minecraft:shift_a", "minecraft:shift_b"}:
                    continue  # This is a noise parameter key, not a density function.
                check_horizontal(child, seen)
        elif isinstance(value, list):
            for child in value:
                check_horizontal(child, seen)
        elif isinstance(value, str) and ":" in value:
            assert value.startswith("elysium:"), f"External density reference in height field: {value}"
            assert value not in seen, f"Density reference cycle: {value}"
            name = value.split(":", 1)[1]
            check_horizontal(read_json(f"worldgen/density_function/{name}.json"), seen | {value})

    check_horizontal(height)
    assert "caves/" not in json.dumps(noise)
    dimension = read_json("dimension_type/elysium.json")
    assert dimension["fixed_time"] == 11000 and not dimension["has_raids"]
    assert (dimension["min_y"], dimension["height"]) == (noise["noise"]["min_y"], noise["noise"]["height"])


def validate_density_isolation() -> int:
    """Reject dependency on any externally overridable density-function ID."""
    functions = {f"elysium:{path.stem}": json.loads(path.read_text())
                 for path in (DATA / "worldgen/density_function").glob("*.json")}
    graph = {name: set() for name in functions}

    def walk(value, edges):
        if isinstance(value, dict):
            kind = value.get("type", "")
            assert not kind or kind.startswith("minecraft:"), f"External density implementation: {kind}"
            for key, child in value.items():
                if key == "type":
                    continue
                if key == "noise" or (key == "argument" and kind in {"minecraft:shift_a", "minecraft:shift_b"}):
                    # Intentionally reuse normal-noise parameters, not vanilla
                    # density-function aliases which terrain mods replace.
                    assert child.startswith(("minecraft:", "elysium:"))
                    if child.startswith("elysium:"):
                        assert (DATA / f"worldgen/noise/{child.split(':', 1)[1]}.json").is_file()
                    continue
                walk(child, edges)
        elif isinstance(value, list):
            for child in value:
                walk(child, edges)
        elif isinstance(value, str) and ":" in value:
            assert value in functions, f"External/missing density dependency: {value}"
            edges.add(value)

    for name, function in functions.items():
        walk(function, graph[name])
    router = read_json("worldgen/noise_settings/elysium.json")["noise_router"]
    roots = set()
    walk(router, roots)
    visiting, complete = set(), set()

    def visit(name):
        assert name not in visiting, f"Density graph cycle: {name}"
        if name in complete:
            return
        visiting.add(name)
        for child in graph[name]:
            visit(child)
        visiting.remove(name)
        complete.add(name)

    for root in roots:
        visit(root)
    assert complete == set(functions), "Orphaned generated density function"
    return len(complete)


def validate_structures() -> None:
    structures = {f"elysium:{path.stem}" for path in (DATA / "worldgen/structure").glob("*.json")}
    tagged = set(read_json("tags/worldgen/structure/is_elysium.json")["values"])
    assert structures == tagged
    for path in (DATA / "worldgen/structure_set").glob("*.json"):
        resource = json.loads(path.read_text())
        for item in resource["structures"]:
            assert item["structure"] in structures
        placement = resource["placement"]
        assert placement["spacing"] > placement["separation"] > 0
        if "exclusion_zone" in placement:
            other = placement["exclusion_zone"]["other_set"].split(":", 1)[1]
            assert (DATA / f"worldgen/structure_set/{other}.json").is_file()
            assert other != path.stem, "Structure set cannot exclude itself"
    bridge = read_json("worldgen/structure/elysian_bridge.json")
    assert bridge["terrain_adaptation"] == "none" and bridge["type"] == "elysium:elysian_bridge"
    for element in read_json("worldgen/template_pool/harvest_hamlet/start.json")["elements"]:
        template = element["element"]["location"].split(":", 1)[1]
        assert (DATA / f"structure/{template}.nbt").is_file()


def validate_vanilla_refs(jar: Path) -> tuple[int, int]:
    """Verify noise parameters and exact climate formula parity with the game."""
    references = set()

    def walk(value, parent="", kind=""):
        if isinstance(value, dict):
            for key, child in value.items():
                walk(child, key, value.get("type", ""))
        elif isinstance(value, list):
            for child in value:
                walk(child, parent, kind)
        elif isinstance(value, str) and value.startswith("minecraft:"):
            name = value.split(":", 1)[1]
            if parent == "noise" or (parent == "argument" and kind in {"minecraft:shift_a", "minecraft:shift_b"}):
                references.add(f"data/minecraft/worldgen/noise/{name}.json")

    for folder in ("noise_settings", "density_function"):
        for path in (DATA / "worldgen" / folder).rglob("*.json"):
            walk(json.loads(path.read_text()))
    with zipfile.ZipFile(jar) as archive:
        present = set(archive.namelist())
        missing = sorted(references - present)
        assert not missing, "Missing vanilla noise parameters:\n" + "\n".join(missing)

        def localize_shifts(value):
            if isinstance(value, dict):
                return {key: localize_shifts(child) for key, child in value.items()}
            if isinstance(value, list):
                return [localize_shifts(child) for child in value]
            if value in ("minecraft:shift_x", "minecraft:shift_z"):
                return value.replace("minecraft:", "elysium:")
            return value

        verified = 0
        for name, vanilla in (("shift_x", "shift_x"), ("shift_z", "shift_z"),
                              ("continents", "overworld/continents"), ("erosion", "overworld/erosion"),
                              ("ridges", "overworld/ridges")):
            original = json.loads(archive.read(f"data/minecraft/worldgen/density_function/{vanilla}.json"))
            actual = read_json(f"worldgen/density_function/{name}.json")
            assert actual == localize_shifts(original), f"Raw climate formula drifted from vanilla: {name}"
            verified += 1
        original_router = json.loads(archive.read("data/minecraft/worldgen/noise_settings/overworld.json"))["noise_router"]
        for name in ("temperature", "vegetation"):
            actual = read_json(f"worldgen/density_function/{name}.json")
            assert actual == localize_shifts(original_router[name]), f"Raw climate formula drifted from vanilla: {name}"
            verified += 1
    return len(references), verified


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", type=Path, help="also verify noise parameters and climate formulas against this jar")
    args = parser.parse_args()
    climates = validate_climates()
    features = validate_features()
    templates = [validate_hamlet(path.name) for path in sorted((DATA / "structure").glob("harvest_hamlet*.nbt"))]
    assert len(templates) == 2
    validate_terrain()
    isolated = validate_density_isolation()
    validate_structures()
    suffix = ""
    if args.minecraft_jar:
        parameters, formulas = validate_vanilla_refs(args.minecraft_jar)
        suffix = f"; {parameters} vanilla noise parameters, {formulas} exact climate formulas"
    print(f"Worldgen validation passed: {climates} climate probes, {features} ordered features, "
          f"{len(templates)} templates/{sum(template[0] for template in templates)} positions, "
          f"{sum(template[2] for template in templates)} villagers, {isolated} isolated density functions, solid-depth density{suffix}.")


if __name__ == "__main__":
    main()
