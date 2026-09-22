# Elysium

An eternal harvest hidden in an ordinary wheat field. **Minecraft 1.21.1 · NeoForge 21.1.233+ · Java 21.**

Elysium is a separate dimension of golden birches, olive meadows, wild grain, clear rivers, pale uplands and small harvest settlements. Its sun stays in the late afternoon. People who never seek what lies beneath it can simply live there.

This repository develops the first playable **paradise** milestone. Hephaestus' Forge, Tartarus, bosses, a moving sun and the later crisis are future work. The current realm has no automatic countdown or hidden catastrophe.

## Current foundation

- Three related biomes: Golden Fields, Golden Birch Woods and Elysian Highlands.
- Parameterized biome definitions generate climate entries, palettes, feature budgets and settlement tags together, informed by the architecture of BicBiomeCraft.
- Golden birch leaves and saplings, taller trees, wild grain and passive animals. Models reference vanilla textures; no Minecraft texture files are redistributed.
- Independent terrain with river cuts and uplands over mostly uninterrupted stone. Natural noise caves and ore veins are deliberately absent in this first realm.
- Two original harvest hamlet layouts with pale cottages, grain plots, villagers, beds, paths and lanterns. Terrain-aware birch bridges span suitable rivers, with stair approaches and pale stone piers.
- Shard → fragment → sigil progression, an open-air harvest shrine, remembered return travel, and an arrival title.
- Dedicated-server GameTests and a GitHub Actions build. See [validation notes](docs/TESTING.md) for what has actually run.

## Try it

Install NeoForge for Minecraft **1.21.1**, then put the built `elysium-0.1.0-alpha.1.jar` in `mods/` on both client and server. No biome or structure mod is required for the standalone version. Start a **new test world** while terrain is under active development; already generated chunks retain their old terrain when the generator changes.

With commands enabled, `/elysium visit` enters safely from the Overworld. `/elysium return` works for any player inside Elysium as an escape route if a return altar is lost. Wait three seconds between crossings. The Elysium creative tab exposes all blocks and progression items. Use `/locate biome elysium:golden_fields` and `/locate structure elysium:harvest_hamlet` inside the dimension to inspect content.

For the survival route, mature vanilla wheat has a default **0.01%** chance to drop one Elysium Shard. Craft four shards in a 2×2 square into a fragment, then four fragments in a 2×2 square into a Sigil of Elysium. Sixteen shards are therefore needed: **160,000 mature harvests on average** at the default rate, with substantial random variation. This intentionally preserves the proposed rarity rather than silently making the discovery common. The server config `entrance.wheatShardChance` accepts 0–1; `0.001` is 0.1% and averages 16,000 harvests per sigil. Wild Elysian grain does not award shards.

Craft a Harvest Altar with a gold ingot above a hay bale and five quartz (recipe unlocks after finding a shard). Place it outdoors, with four vertical pillars of three stripped birch logs at cardinal distance three and four hay bales at diagonal offsets `(±2, ±2)`, all bases level with the altar. Insert the sigil. At Overworld sunset (ticks 11000–13000), offer one wheat. Touch the awakened altar with an empty hand. [The entrance guide](docs/ENTRANCE.md) gives exact coordinates, return behavior and persistence details.

## Develop

```sh
./gradlew build
./gradlew runClient
```

The Gradle wrapper is included. On Windows use `gradlew.bat`. A Java 21 JDK and internet access for the first dependency download are required. Committed generated resources mean Python is **not** required to compile or play.

When changing content definitions, use Python 3.10+:

```sh
python3 tools/generate_assets.py
python3 tools/generate_worldgen.py
python3 tools/validate_worldgen.py
```

CI uses both generators' `--check` modes to catch uncommitted generated changes. The source of truth and extension points are explained in [world generation](docs/WORLDGEN.md), [structures](docs/STRUCTURES.md) and [testing](docs/TESTING.md).

[ReTerraForged / FreeTerraForged research](docs/RETERRAFORGED.md) identifies the 1.21.1 Uplift fork and a possible future dimension adapter. Its jar passed a co-installation GameTest smoke test, but its terrain engine is **not integrated into Elysium** and an active FreeTerraForged Overworld preset has not been tested.

The visual target includes rich golden light and dramatic scenery. Vanilla rendering supplies the working baseline; shader lighting, composition across many seeds, and terrain-fitting village refinement need in-game visual iteration. This alpha has been tested on a headless server, not through a graphical client.

Original Elysium code and content are currently **All Rights Reserved** pending an explicit project license decision. The NeoForge MDK template retains its separate [MIT notice](TEMPLATE_LICENSE.txt). Minecraft assets remain referenced game assets. Third-party structure files have not been copied.
