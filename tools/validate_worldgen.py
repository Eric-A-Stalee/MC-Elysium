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
    axes = ("humidity", "erosion", "temperature", "weirdness")
    for entry in entries:
        parameters = entry["parameters"]
        assert parameters["offset"] == 0
        for axis in ("continentalness", "depth"):
            low, high = bounds(parameters[axis])
            assert low <= -1 and high >= 1
        name = entry["biome"].split(":", 1)[1]
        assert (DATA / f"worldgen/biome/{name}.json").is_file()
    samples = {}
    for axis in axes:
        boundaries = sorted({v for entry in entries for v in bounds(entry["parameters"][axis])})
        samples[axis] = sorted(set(boundaries + [(a + b) / 2 for a, b in zip(boundaries, boundaries[1:])]))
        assert boundaries[0] <= -1 and boundaries[-1] >= 1
    checked = 0
    for values in itertools.product(*(samples[axis] for axis in axes)):
        matches = [entry for entry in entries if all(
            bounds(entry["parameters"][axis])[0] <= value <= bounds(entry["parameters"][axis])[1]
            for axis, value in zip(axes, values))]
        assert matches, f"Climate hole at {values}"
        strict = [entry for entry in matches if all(
            bounds(entry["parameters"][axis])[0] < value < bounds(entry["parameters"][axis])[1]
            for axis, value in zip(axes, values))]
        assert len(strict) <= 1, f"Interior climate overlap at {values}"
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
    for name in ("harvest_hamlet", "elder_spring", "sunlit_lookout", "waterside_cottage"):
        site = read_json(f"worldgen/structure/{name}.json")
        assert site["type"] == "elysium:landscape_site" and site["terrain_adaptation"] == "none"
        for template in site["templates"]:
            path = DATA / "structure" / f"{template.split(':', 1)[1]}.nbt"
            root = NbtReader(gzip.decompress(path.read_bytes())).root()
            assert root["size"][0] == root["size"][2] == site["radius"] * 2 + 1
    assert read_json("worldgen/structure/waterside_cottage.json")["require_water_approach"]
    town = read_json("worldgen/structure/mountain_town.json")
    assert 1 <= town["minimum_cottages"] <= town["maximum_cottages"] <= 6
    for module in [town["hall"], *town["cottages"]]:
        name = module["template"].split(":")[1]
        root = NbtReader(gzip.decompress((DATA / f"structure/{name}.nbt").read_bytes())).root()
        assert root["size"][0] == root["size"][2] == module["radius"] * 2 + 1
        assert 0 <= module["max_relief"] <= 4


def validate_modules() -> int:
    """Read the actual NBT and verify usable entrances, beds and villager exits."""
    from collections import deque
    total = 0
    for name in ("waterside_cottage", "mountain_cottage", "mountain_cottage_store", "mountain_hall", "mountain_plaza"):
        root = NbtReader(gzip.decompress((DATA / f"structure/{name}.nbt").read_bytes())).root()
        blocks = {}
        for record in root["blocks"]:
            pos = tuple(record["pos"])
            assert pos not in blocks and all(0 <= p < n for p, n in zip(pos, root["size"]))
            blocks[pos] = root["palette"][record["state"]]
        total += len(blocks)
        for (x, y, z), block in blocks.items():
            props, kind = block.get("Properties", {}), block["Name"]
            if kind.endswith("_door") and props["half"] == "lower":
                assert blocks[x, y + 1, z]["Properties"]["half"] == "upper"
                assert blocks[x, y - 1, z]["Name"] not in ("minecraft:air", "minecraft:water")
            if kind.endswith("_bed") and props["part"] == "foot":
                assert props["facing"] == "north" and blocks[x, y, z - 1]["Properties"]["part"] == "head"
            if kind == "minecraft:lantern":
                support = blocks.get((x, y - 1, z), {})
                assert support.get("Name") not in (None, "minecraft:air", "minecraft:water"), f"Floating lantern in {name}"
        if not root["entities"]:
            continue
        # Closed doors are operable by villagers, so count their two halves as
        # clear for this geometric check. This is not a simulation of NPC AI.
        def clear(pos):
            kind = blocks.get(pos, {}).get("Name", "minecraft:air")
            return kind == "minecraft:air" or kind.endswith("_door")
        def walkable(pos):
            x, y, z = pos
            return (all(0 <= p < n for p, n in zip(pos, root["size"])) and clear(pos)
                    and clear((x, y + 1, z)) and not clear((x, y - 1, z)))
        exit_pos = (root["size"][0] // 2, 2, root["size"][2] - 1)
        assert walkable(exit_pos), f"Missing authored path connector in {name}"
        for entity in root["entities"]:
            start = tuple(entity["blockPos"])
            assert walkable(start), f"Blocked villager in {name}"
            seen, queue = set(), deque([start])
            while queue:
                at = queue.popleft()
                if at in seen:
                    continue
                seen.add(at)
                x, y, z = at
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    for dy in (-1, 0, 1):
                        nxt = (x + dx, y + dy, z + dz)
                        if nxt not in seen and walkable(nxt):
                            queue.append(nxt)
            assert exit_pos in seen, f"Villager cannot reach the outside connector in {name}"
    return total


def validate_grand_modules() -> int:
    """Independently traverse the shipped floors, stairs, rooms and ladder landings."""
    from collections import deque
    town = read_json("worldgen/structure/grand_mountain_town.json")
    assert 14 <= town["minimum_houses"] <= town["maximum_houses"] <= 26
    assert town["type"] == "elysium:contour_town"
    assert set(town["styles"]) == {"longhouse", "cross_gable", "hillside_lodge", "courtyard"}
    total = 0
    # Old template IDs stay shipped for partially generated alpha 6 structures.
    legacy = [{"template": "elysium:"+name, "radius": radius} for name,radius in (
        ("mountain_great_hall",13), ("mountain_watchtower",6), ("mountain_manor",11),
        ("mountain_manor_workshop",11), ("mountain_manor_gabled",11))]
    for module in legacy:
        name = module["template"].split(":")[1]
        root = NbtReader(gzip.decompress((DATA / f"structure/{name}.nbt").read_bytes())).root()
        size = root["size"]
        assert size[0] == size[2] == module["radius"] * 2 + 1
        blocks = {tuple(b["pos"]): root["palette"][b["state"]] for b in root["blocks"]}
        assert len(blocks) == len(root["blocks"])
        total += len(blocks)
        def kind(p):
            return blocks.get(p, {}).get("Name", "minecraft:air")
        def clear(p):
            return kind(p) in {"minecraft:air", "minecraft:ladder"} or kind(p).endswith("_door")
        def stand(p):
            x, y, z = p
            return (all(0 <= n < limit for n, limit in zip(p, size)) and clear(p) and clear((x, y+1, z))
                    and (not clear((x, y-1, z)) or kind(p) == "minecraft:ladder"))
        start = (size[0] // 2, 2, size[2] - 1)
        assert stand(start), f"Missing grand-town connector: {name}"
        seen, queue = set(), deque([start])
        while queue:
            p = queue.popleft()
            if p in seen:
                continue
            seen.add(p)
            x, y, z = p
            candidates = [(x+dx, y+dy, z+dz) for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)) for dy in (-1,0,1)]
            if kind(p) == "minecraft:ladder":
                candidates.extend(((x,y-1,z),(x,y+1,z)))
            queue.extend(q for q in candidates if q not in seen and stand(q))
        for resident in root["entities"]:
            assert tuple(resident["blockPos"]) in seen, f"Resident cannot reach street: {name} {resident['blockPos']}"
        doors = []
        for (x,y,z), block in blocks.items():
            props, material = block.get("Properties",{}), block["Name"]
            if material.endswith("_door") and props["half"] == "lower":
                assert blocks[x,y+1,z]["Properties"]["half"] == "upper"
                assert not clear((x,y-1,z)), f"Unsupported door: {name} {(x,y,z)}"
                assert (x,y,z) in seen, f"Inaccessible room door: {name} {(x,y,z)}"
                doors.append((x,y,z))
            if material.endswith("_bed") and props["part"] == "foot":
                assert blocks[x,y,z-1]["Properties"]["part"] == "head"
                assert any((x+dx,y,z+dz) in seen for dx,dz in ((1,0),(-1,0),(0,1),(0,-1))), f"Inaccessible bed: {name}"
            if material == "minecraft:lantern":
                assert not clear((x,y-1,z)), f"Unsupported lantern: {name} {(x,y,z)}"
        if "watchtower" in name:
            assert any(y>=27 for x,y,z in seen), "Watchtower ladder must reach the top floor"
        else:
            assert len(doors)>=7, f"Grand houses need multiple real rooms: {name}"
            assert sum(y==8 for x,y,z in seen)>=70, f"Upper floor must be substantial and connected: {name}"
            assert any(y==13 for x,y,z in seen), f"Attic ladder is inaccessible: {name}"
            assert sum("glass" in b["Name"] for b in blocks.values())>=40
    return total


def validate_landmarks() -> int:
    total = 0
    for name in ("elder_spring", "sunlit_lookout"):
        root = NbtReader(gzip.decompress((DATA / f"structure/{name}.nbt").read_bytes())).root()
        assert root["DataVersion"] == 3955 and not root["entities"]
        blocks = {}
        for block in root["blocks"]:
            pos = tuple(block["pos"])
            assert pos not in blocks
            assert all(0 <= p < size for p, size in zip(pos, root["size"]))
            blocks[pos] = root["palette"][block["state"]]
        for (x, y, z), state in blocks.items():
            if state["Name"] == "minecraft:water":
                # Every source is contained at its own level and has a solid
                # authored floor. Sparse template edges must not spill water.
                assert blocks[x, y - 1, z]["Name"] == "minecraft:smooth_sandstone"
                for dx, dz in ((0, 1), (0, -1), (1, 0), (-1, 0)):
                    assert blocks.get((x + dx, y, z + dz), {}).get("Name") in {
                        "minecraft:water", "minecraft:smooth_sandstone"}, f"Uncontained spring at {(x, y, z)}"
            if state["Name"] == "minecraft:lantern":
                support_y = y + 1 if state["Properties"]["hanging"] == "true" else y - 1
                support = blocks.get((x, support_y, z), {})
                assert support.get("Name") not in {None, "minecraft:air", "minecraft:water"}
                if support["Name"].endswith("_slab"):
                    face = "bottom" if state["Properties"]["hanging"] == "true" else "top"
                    assert support["Properties"]["type"] in {face, "double"}, "Lantern needs a solid attachment face"
        # Terrain around the authored footprint remains untouched.
        for x in (0, root["size"][0] - 1):
            for z in (0, root["size"][2] - 1):
                assert not any((x, y, z) in blocks for y in range(root["size"][1]))
        if name == "elder_spring":
            assert sum(state["Name"] == "minecraft:water" for state in blocks.values()) >= 15
            assert max(y for (x, y, z), state in blocks.items() if state["Name"] == "elysium:golden_birch_leaves") >= 17
        total += len(blocks)
    return total


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
    landmarks = validate_landmarks()
    modules = validate_modules()
    grand_modules = validate_grand_modules()
    suffix = ""
    if args.minecraft_jar:
        parameters, formulas = validate_vanilla_refs(args.minecraft_jar)
        suffix = f"; {parameters} vanilla noise parameters, {formulas} exact climate formulas"
    print(f"Worldgen validation passed: {climates} climate probes, {features} ordered features, "
          f"{len(templates)} templates/{sum(template[0] for template in templates)} positions, "
          f"{sum(template[2] for template in templates)} villagers, 2 landmarks/{landmarks} positions, "
          f"5 small settlement modules/{modules} positions, 5 grand building modules/{grand_modules} positions, "
          f"{isolated} isolated density functions, solid-depth density{suffix}.")


if __name__ == "__main__":
    main()
