# Elysium

An eternal harvest hidden in an ordinary wheat field. **Minecraft 1.21.1 · NeoForge 21.1.233+ · Java 21.**

Elysium is a separate dimension of golden birches, straw-colored meadows, wild grain, clear rivers, pale uplands and small harvest settlements. Its sun stays in the late afternoon. People who never seek what lies beneath it can simply live there.

This repository develops the first playable **paradise** milestone. An exposed Chain of Tartarus now leads to a buried anchor chamber. Hephaestus' Forge, the full Tartarus region, bosses, a moving sun and the later crisis are future work. The current realm has no automatic countdown or hidden catastrophe.

## Current foundation

- Six related biomes: Golden Fields, Golden Birch Woods, Golden Watermeadows, Amber Lakes, Elysian Highlands and Ivory Peaks.
- Parameterized biome definitions generate climate entries, palettes, feature budgets and settlement tags together, informed by the architecture of BicBiomeCraft.
- Open Golden Fields, straw/ochre biome grass colors, and golden woods with sparse openings, varied tall crowns and occasional branching birches. Wild grain and passive animals fill the landscape. Models reference vanilla textures; no Minecraft texture files are redistributed.
- Flowered stream corridors with high-branching birches, wider amber lake reaches, copper foliage, scattered fallen leaves and cooler mountain palettes.
- Independent terrain with river cuts and uplands over mostly uninterrupted stone. Natural noise caves and ore veins are deliberately absent in this first realm.
- Two original harvest hamlet layouts with pale cottages, grain plots and villagers. Gentle sites can gain a short path to a nearby riverbank; terrain-aware birch bridges span suitable rivers.
- Sparse waterside cottages face verified bank approaches. Rare mountain towns combine a communal hall, a bell plaza and three to five individually fitted cottages, linked by graded stone paths and short retaining walls.
- A separate grand mountain-town tier plans both riverbanks around a stone crossing: 14–24 large homes, a great hall, a watchtower and a public square. Homes have connected rooms, upper floors, attic access, side wings, dormers and varied roof materials. Whole sites are rejected if their mountain setting, building terraces or connecting streets fail.
- Occasional elder birches beside contained springs, and pale lookout pergolas facing lower terrain. Sparse riverside groves leave field interiors open.
- Quiet woodland wind and occasional golden leaves falling from real canopies. Ambient sound volume and particle density follow normal Minecraft settings.
- Shard → fragment → sigil progression, an open-air harvest shrine, remembered return travel, and an arrival title.
- Dedicated-server GameTests and a GitHub Actions build. See [validation notes](docs/TESTING.md) for what has actually run.

## Try it

Install NeoForge for Minecraft **1.21.1**, then put the built `elysium-0.1.0-alpha.6.jar` in `mods/` on both client and server. No biome or structure mod is required for the standalone version. Alpha 6 adds grand mountain towns, foliage built from offset clusters, space for crowns around settlement roofs and paving, broader mountain valley floors, more continuous pale rock, and a much larger Tartarus vault. Explore new chunks or regenerate Elysium to see the new generation; existing trees and terrain are not rewritten.

Alpha 5.1 fixes transparent held and dropped leaf items by supplying the opacity required by Minecraft's item tint renderer. The same correction covers golden birch leaves, shards, fragments and sigils. Replace the old jar and restart; this hotfix needs no new chunks or dimension reset.

For the complete alpha 6 terrain layout, use a new test world or [regenerate only Elysium](docs/WORLDGEN.md#regenerating-elysium). Existing chunks keep their terrain and trees; palette and texture updates apply after restarting. Mixing terrain revisions can produce chunk-boundary seams.

With commands enabled, `/elysium visit` enters safely from the Overworld. `/elysium return` works for any player inside Elysium as an escape route if a return altar is lost. Wait three seconds between crossings. The Elysium creative tab exposes all blocks and progression items. Use `/locate biome elysium:golden_fields` and `/locate structure elysium:harvest_hamlet` inside the dimension to inspect content. Also inspect `/locate biome elysium:golden_watermeadows`, `/locate biome elysium:amber_lakes`, `/locate structure elysium:waterside_cottage` and `/locate structure elysium:mountain_town`. For the larger river-valley settlement, use `/locate structure elysium:grand_mountain_town`. Both town tiers deliberately reject unsuitable terrain, so locate searches can be longer. `/locate structure elysium:tartarus_chain` finds a rare Ivory Peaks chain: follow its custom blocks through solid rock to the vaulted anchor chamber at Y -48. New chambers are 73 blocks across and 54 blocks high, with a gallery and four stairways. Older saved chains retain their original chamber dimensions. The chamber is an initial piece of Tartarus, with no boss or progression trigger yet.

For the survival route, mature vanilla wheat has a default **0.01%** chance to drop one Elysium Shard. Craft four shards in a 2×2 square into a fragment, then four fragments in a 2×2 square into a Sigil of Elysium. Sixteen shards are therefore needed: **160,000 mature harvests on average** at the default rate, with substantial random variation. This intentionally preserves the proposed rarity rather than silently making the discovery common. The server config `entrance.wheatShardChance` accepts 0–1; `0.001` is 0.1% and averages 16,000 harvests per sigil. Any break that drops mature wheat's loot counts, so villager farmers and automated farms are the practical route and are intended. Wild Elysian grain does not award shards.

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
python3 tools/validate_client_assets.py
python3 tools/generate_worldgen.py
python3 tools/validate_worldgen.py
```

CI checks both content generators and `tools/generate_ambience.py --check`. The latter verifies the original leaf sprite and the committed sound asset. `tools/validate_client_assets.py` independently verifies that custom model textures are present in the blocks atlas and that model/particle texture files exist. Regenerating the sound uses Python synthesis and ffmpeg; players and ordinary builds need neither. The source of truth and extension points are explained in [world generation](docs/WORLDGEN.md), [structures](docs/STRUCTURES.md) and [testing](docs/TESTING.md).

[ReTerraForged / FreeTerraForged research](docs/RETERRAFORGED.md) identifies the 1.21.1 Uplift fork and a possible future dimension adapter. Its jar passed a co-installation GameTest smoke test, but its terrain engine is **not integrated into Elysium** and an active FreeTerraForged Overworld preset has not been tested.

The visual target includes rich golden light and dramatic scenery. Vanilla rendering supplies the working baseline. The first player screenshots informed the golden grass and open-field revisions. Alpha 6's tree silhouettes, terrain transitions, grand-town architecture, vault lighting and villager circulation still need a graphical playtest; automated validation uses a headless server.

Original Elysium code and content are currently **All Rights Reserved** pending an explicit project license decision. The NeoForge MDK template retains its separate [MIT notice](TEMPLATE_LICENSE.txt). Minecraft assets remain referenced game assets. Third-party structure files have not been copied.
