# Elysium

An eternal harvest hidden in an ordinary wheat field. **Minecraft 1.21.1 · NeoForge 21.1.233+ · Java 21.**

Elysium is a separate dimension of golden birches, straw-colored meadows, wild grain, clear rivers, pale uplands and small harvest settlements. Its sun begins arrested in the late afternoon. People who never seek what lies beneath it can simply live there.

This repository develops the playable **paradise** and its first optional exploration chapter. Mountain archives lead to the dormant Forge of Hephaestus; an instrument recovered there can follow a Chain of Tartarus to its buried anchor. A deeper inspection route now reaches a burnt Tartarus basin, a broken counterweight crossing and the one-eyed Cinderbound Eye. Recover the First Flame there and carry it home to restore full heat to the connected forge runners. The restored hammer now makes useful instruments and can release the first sunset. Three old surface watches lead through the interrupted night to a choice: a living day/night cycle, or an enduring golden hour freely chosen. Read [the first-dawn guide](docs/FIRST_DAWN.md). A wider Tartarus realm remains future work; there is no offline deadline or block-destroying catastrophe.

## Current foundation

- Six related biomes: Golden Fields, Golden Birch Woods, Golden Watermeadows, Amber Lakes, Elysian Highlands and Ivory Peaks.
- Parameterized biome definitions generate climate entries, palettes, feature budgets and settlement tags together, informed by the architecture of BicBiomeCraft.
- Open Golden Fields, straw/ochre biome grass colors, and golden woods with sparse openings, varied tall crowns and occasional branching birches. Wild grain and passive animals fill the landscape. Models reference vanilla textures; no Minecraft texture files are redistributed.
- Flowered stream corridors with high-branching birches, wider amber lake reaches, copper foliage, scattered fallen leaves and cooler mountain palettes.
- Independent terrain with river cuts and uplands over mostly uninterrupted stone. Natural noise caves and ore veins are deliberately absent in this first realm.
- Two original harvest hamlet layouts with pale cottages, grain plots and villagers. Gentle sites can gain a short path to a nearby riverbank; terrain-aware birch bridges span suitable rivers.
- Sparse waterside cottages face verified bank approaches. Rare mountain towns combine a communal hall, a bell plaza and three to five individually fitted cottages, linked by graded stone paths and short retaining walls.
- A separate grand mountain-town tier climbs both riverbanks around a stone crossing: 14–24 substantial Nordic homes, a great hall, a timber watch lodge and a public square. Semi-dynamic longhouses, cross-gabled homes, hillside lodges and courtyard houses fit their rooms, split-level wings, cellars and entrances to surveyed terrain. Mixed timber construction, covered galleries and intersecting roofs vary their silhouettes. Connected contour streets leave rock and ground between buildings intact.
- Occasional elder birches beside contained springs, and pale lookout pergolas facing lower terrain. Sparse riverside groves leave field interiors open.
- Quiet woodland wind and occasional golden leaves falling from real canopies. Ambient sound volume and particle density follow normal Minecraft settings.
- Shard → fragment → sigil progression, an open-air harvest shrine, remembered return travel, and an arrival title.
- A buried, dormant mountain foundry: a scree-choked entrance, failed gallery and rockfall, heavy suspended hammer, unfinished chain link, cold furnace bank and workshops. A 72-block descent winds around a ruined hauling shaft. A coal-fed service brazier sends embers through physical copper channels; ash, cracks and missing runners interrupt its branches while the First Flame remains absent. Personal lore discoveries, a Surveyor's Folio and a Chain Resonator connect settlements to the Forge and Tartarus anchors. See [the Forge chapter](docs/FORGE.md).
- A bounded Tartarus basin beneath each new chain: a long inspection descent, charcoal cliffs, sparse ash, buried embers, a repairable crossing, and a dark chamber containing the tar-and-slag chimera. Its real eye hitbox lowers after visible cinder volleys, a hooked-tail sweep and an expanding ground pulse. Recover the First Flame and return it to the original Forge hearth. [Expedition guide](docs/TARTARUS.md).
- A physical three-stroke forge press with persistent work, Sunwrought Ingots, a replanting Harvest Sickle, a Dawn Wayfinder, protective Ward Lamps and luminous Ember Glass. The first strike deliberately starts an independent realm clock. Three terrain-fitted watch ruins answer the interrupted night; a repaired governor lets players choose and later revisit the realm's future.
- Dedicated-server GameTests and a GitHub Actions build. See [validation notes](docs/TESTING.md) for what has actually run.

## Try it

Install NeoForge for Minecraft **1.21.1**, then put the built `elysium-0.1.0-alpha.22.jar` in `mods/` on both client and server. No biome or structure mod is required for the standalone version. Alpha 22 adds the working smithy, first sunset, three watchfire traditions, stray cinders and both resolutions of the first dawn. It retains the articulated Cinderbound Eye, visible projectiles, dangerous restored forge fire, and the natural-generation fixes for the [Tartarus expedition](docs/TARTARUS.md). Start with an archive in a grand mountain hall or abandoned waystation, or a folio bought from an Elysium librarian/cartographer. Use a **new alpha test world or fully regenerate Elysium** for this update: the dimension extends to Y=-256, and previously truncated Tartarus structures are not repaired in existing chunks. Newly generated Forges include the press control, physical hammer and celestial governor; new surface chunks contain the watch ruins.

The approved **warmer-amber B foliage** (`#FFB947`), L3 texture curve and G4.1 golden grass are unchanged. Grass tops, side overlays and grass plants receive the same treatment within Elysium. The client derives the sprites from the active resource pack, preserving their pixel patterns, transparency, resolution and animation. The untouched realm and chosen golden-hour ending stay at **11,500 ticks**, and the pale, snowlike Ivory Peaks rock remains part of the intended mountain atmosphere.

Alpha 5.1 fixes transparent held and dropped leaf items by supplying the opacity required by Minecraft's item tint renderer. The same correction covers golden birch leaves, shards, fragments and sigils. Replace the old jar and restart; this hotfix needs no new chunks or dimension reset.

For the historical palette-only alpha 11–14 updates, there was **no dimension reset**: these colour and sprite changes apply to existing chunks after restarting. Grass keeps its vanilla block IDs and behaviour; the extra sprite treatment is confined to Elysium's world rendering. For the earlier alpha 11 terrain, planting, foundations and tree-crown changes, use fresh chunks or [regenerate only Elysium](docs/WORLDGEN.md#regenerating-elysium). Mixing terrain revisions can produce chunk-boundary seams. Old settlement pieces and templates remain available for saved worlds.

With commands enabled, `/elysium visit` enters safely from the Overworld. `/elysium return` works for any player inside Elysium as an escape route if a return altar is lost. Wait three seconds between crossings. The Elysium creative tab exposes all blocks and progression items. Use `/locate biome elysium:golden_fields` and `/locate structure elysium:harvest_hamlet` inside the dimension to inspect content. Also inspect `/locate biome elysium:golden_watermeadows`, `/locate biome elysium:amber_lakes`, `/locate structure elysium:waterside_cottage` and `/locate structure elysium:mountain_town`. For the larger river-valley settlement, use `/locate structure elysium:grand_mountain_town`. Both town tiers deliberately reject unsuitable terrain, so locate searches can be longer. `/locate structure elysium:tartarus_chain` finds a rare Ivory Peaks chain: follow its custom blocks through solid rock to the vaulted anchor chamber at Y -48. New chambers are 73 blocks across and 54 blocks high, with a gallery and four stairways. The anchor answers a Chain Resonator. In alpha 18, a southern inspection passage descends from that vault to the cinder rift and the separate creature chamber at Y=-232. `/locate structure elysium:hephaestus_forge` and `/locate structure elysium:forge_waystation` inspect the new chapter directly.

Use `/elysium story` for a read-only chapter reminder. `/locate structure elysium:harvest_watch`, `elysium:water_watch` and `elysium:high_watch` inspect the new ruins. In survival, the forged Dawn Wayfinder leads to actual saved sites.

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

For the separately tested older ReTerraForged build with a specific patched Dynamic Height version, [optional pack-compatibility sources](compat/README.md) are available in an independent build. They isolate Elysium from foreign terrain hooks and preserve its declared height; they are not required for standalone Elysium, and the external dependency jars are not included.

The visual target includes rich golden light and dramatic scenery. Player screenshots informed the open fields, adaptive mountain towns and palette. Programmatic previews apply the actual texture/tint calculations to Minecraft's sprites. Alpha 14 pairs the L3 curve with the selected warmer-amber B tint to separate foliage from the golden ground. G4.1's 0.95 gain remains folded into Minecraft's integer biome tint, differing by at most one 8-bit channel value before lighting. The Golden Fields and both mountain biomes use the selected pair, while woods, watermeadows and Amber Lakes retain regional variants. World lighting, shadows, biome blending and resource packs still affect the result, so shader-on/off comparison remains useful. See the [current palette](docs/WORLDGEN.md#alpha-14-warmer-amber-b-foliage) and [sprite implementation](docs/WORLDGEN.md#alpha-13-approved-l3-leaves-and-g41-ground) for the exact recipe and pack behaviour.

Original Elysium code and content are currently **All Rights Reserved** pending an explicit project license decision. The optional adapters under `compat/` retain their separately declared [MIT license](compat/LICENSE); this does not relicense Elysium itself. The NeoForge MDK template retains its separate [MIT notice](TEMPLATE_LICENSE.txt). Minecraft assets remain referenced game assets. Third-party structure files have not been copied.
