#!/usr/bin/env python3
"""Check Elysium's concrete model/particle texture references and atlas membership.

Reads committed resources independently of the generators. This covers vanilla
1.21.1's block/item directories and our single/generated sprite atlas additions;
it is not a client renderer or a validator for third-party resource packs.
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"


def texture_file(assets, resource):
    namespace, path = resource.split(":", 1) if ":" in resource else ("minecraft", resource)
    return assets / namespace / "textures" / f"{path}.png"


def validate(root):
    assets = root / "assets"
    textures = assets / "elysium/textures"
    # The vanilla blocks atlas scans these directories across all namespaces.
    sprites = {"elysium:" + path.relative_to(textures).with_suffix("").as_posix(): path
               for directory in ("block", "item", "entity/conduit")
               for path in (textures / directory).rglob("*.png")}
    atlas = assets / "minecraft/atlases/blocks.json"
    if atlas.exists():
        for source in json.loads(atlas.read_text())["sources"]:
            if source["type"] == "elysium:tone_mapped":
                gamma = source["gamma"]
                if not isinstance(gamma, (int, float)) or not math.isfinite(gamma) or not 0.2 <= gamma <= 2.0:
                    raise ValueError("Invalid generated-sprite gamma")
                contrast, pivot = source.get("contrast", 1), source.get("pivot", 0)
                if not isinstance(contrast, (int, float)) or not math.isfinite(contrast) or not 0 <= contrast <= 4:
                    raise ValueError("Invalid generated-sprite contrast")
                if not isinstance(pivot, int) or not 0 <= pivot <= 255:
                    raise ValueError("Invalid generated-sprite contrast pivot")
                if not source["sprite"].startswith("elysium:") or source["sprite"] == source["resource"]:
                    raise ValueError("Generated sprites need a separate Elysium identifier")
                # Minecraft supplies vanilla pixels at reload time. Mod-owned
                # inputs must still exist locally; no generated PNG is shipped.
                sprites[source["sprite"]] = (None if source["resource"].startswith("minecraft:")
                                              else texture_file(assets, source["resource"]))
                continue
            if source["type"] not in ("single", "minecraft:single"):
                raise ValueError(f"Extend atlas validation for source type {source['type']}")
            resource = source["resource"]
            sprites[source.get("sprite", resource)] = texture_file(assets, resource)

    errors, references = [], 0
    for model in sorted((assets / "elysium/models").rglob("*.json")):
        # Vanilla references are provided by Minecraft. # aliases resolve to
        # concrete bindings in this model or a parent, all of which are scanned.
        for resource in json.loads(model.read_text()).get("textures", {}).values():
            if not resource.startswith("elysium:"):
                continue
            references += 1
            if resource not in sprites:
                errors.append(f"{model.relative_to(assets)}: {resource} is absent from the blocks atlas")
            elif sprites[resource] is not None and not sprites[resource].is_file():
                errors.append(f"{model.relative_to(assets)}: missing texture {sprites[resource]}")

    # Particle descriptions use a different atlas: its particle/ directory has
    # an empty sprite prefix, unlike model references to particle/golden_leaf.
    for particle in sorted((assets / "elysium/particles").glob("*.json")):
        for resource in json.loads(particle.read_text())["textures"]:
            if resource.startswith("elysium:"):
                references += 1
                path = texture_file(assets, resource.replace(":", ":particle/", 1))
                if not path.is_file():
                    errors.append(f"{particle.relative_to(assets)}: missing particle texture {path}")
    if errors:
        raise ValueError("Invalid client texture references:\n" + "\n".join(errors))
    return references


if __name__ == "__main__":
    print(f"Checked {validate(ROOT)} custom model/particle texture references and atlas membership.")
