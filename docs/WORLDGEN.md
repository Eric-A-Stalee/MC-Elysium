# Elysium world generation

The world has six profiles sharing white birch trunks, harvest colours and a sun held at time 11000. Golden Fields stay open; Golden Birch Woods have varied crowns and clearings; Golden Watermeadows open beneath taller trees along flowered streams; Amber Lakes have wider water, copper canopies and fallen leaves. Elysian Highlands and Ivory Peaks bring pale grass, honey foliage, cooler water and exposed pale stone. Surface rain,
raids, ordinary hostile biome spawns, and zombified piglins from lit nether
portals are disabled in Elysium while its sun is fixed. This does not
override the rest of a player's world or promise that another mod cannot spawn
an entity here.

## Parameterized source of truth

`tools/generate_worldgen.py` is the editable source. The generated JSON and NBT
are committed so ordinary players and Gradle builds do not need Python. Run:

```sh
python3 tools/generate_worldgen.py
python3 tools/generate_worldgen.py --check
python3 tools/validate_worldgen.py
```

The catalog uses frozen `BiomeDefinition`, `ClimateBox`, `Palette`, `Vegetation`,
`GrovePattern`, `TreeShape`, `SiteDefinition`, and `Spawn` dataclasses. Each definition provides the biome's stable resource ID,
display name, climate boxes, colors, vegetation budgets, animal list, temperature,
surface treatment, and settlement affinities. The same definition emits its biome
JSON, its placed features, tree mixture, dimension biome-source entry, and biome
tags. Adding another biome using the existing tree/terrain/settlement vocabulary
means adding one definition; no independent switch statements or hand-maintained
biome membership lists are needed. Localization must also be provided by the
client's language catalog when introducing a new display name.

Configured features describe the shared small, tall, branching and high-canopy golden birches,
grain, flowers, and grass. Placed features apply each biome's attempt budgets. All
vegetation uses the order ordinary trees → optional riverside trees → flowers → grain → grass → optional fallen leaves. Keeping that order
consistent avoids cross-biome feature sorting cycles and gives trees priority
before filling their clearings. Attempts are not guaranteed placements: vanilla
survival predicates, terrain, existing blocks, and tree collisions can reject
them. Decorations use Minecraft's feature RNG, never global random state.

| Biome | Tree attempts | Grain patches | Flower patches | Grass patches | Tall / fancy / high-canopy shares |
| --- | ---: | ---: | ---: | ---: | --- |
| Golden Fields | 0 inland; 0 or 2 near water | 7 | 3 | 2 | 20% / 0% / 0% |
| Golden Birch Woods | 2 in openings; 7 in groves | 3 | 3 | 5 | 50% / 20% / 0% |
| Golden Watermeadows | 2 | 1 | 9 | 4 | 15% / 0% / 75% |
| Amber Lakes | 3 | 2 | 3 | 3 | 25% / 20% / 40% |
| Elysian Highlands | 2 | 2 | 2 | 3 | 70% / 10% / 0% |
| Ivory Peaks | 1 | 0 | 1 | 1 | 80% / 0% / 0% |

The remaining share uses small birches. Amber Lakes also attempt three patches of fallen leaves. Ground-cover columns show maximum patch counts; sparse bands use one quarter (rounded down). The catalog is authoritative. A grain
patch makes 48 survival-checked placement attempts and needs ordinary soil, not
farmland. Hamlet crops use normal cultivated wheat separately.

Golden Fields retain a zero-count tree feature under their existing resource ID
for compatibility. They generate no ordinary scattered trees. Trees authored in
hamlets and rare spring landmarks, planted by players, or extending across a woodland boundary can still
appear there; this is not a rule that deletes trees or prohibits saplings.

The woods use vanilla `noise_threshold_count`, sampled before the per-attempt
position is chosen. Its coherent X/Z noise is evaluated at a 200-block scale,
with two attempts below -0.15 and seven above. This creates broad sparse patches
within the woods. The vanilla noise field is fixed across seeds; terrain, biome
boundaries, positions and tree shapes still depend on the world seed. These
woodland openings are independent of rivers. Golden Fields additionally use a
separate riparian feature: zero attempts below noise -0.05, two above, then a
bounded `near_surface_water` filter. A candidate must be dry, within four blocks
of sea level, and find exposed water in one of twelve probes within six blocks.
This creates intermittent bank groves rather than returning trees to field
interiors. It reads only available decoration chunks and never loads neighbours.

Small birches request 6–9-block trunks, tall birches 10–15, branching birches 9–14, and high-canopy birches 12–18. All share the custom `birch_crown` foliage placer, with radius parameters 2–3 and profile heights 5–7. Alpha 6 builds four offset foliage lobes around each attachment, with a narrow connecting core and small variations at their outer edges. This replaces the stack of identically rounded rows. The branching and high-canopy variants use vanilla fancy trunks to support overlapping crowns; actual trunk/branch lengths follow that placer's geometry. The immutable shared catalog owns the profiles. Per-biome shares are absolute selection
probabilities; the generator converts them to the conditional probabilities
needed by Minecraft's sequential random selector. Placement failures can change
the mix among surviving trees.

Grass colors are biome effects. Leaves, fallen-leaf particles and ground litter use the biome foliage palette within Elysium, including Minecraft's normal biome blending. Outside Elysium, the custom leaves and litter retain their original golden tint; inventory items remain gold. Birch leaf models still reference vanilla textures. The small original grayscale leaf sprite is reused for particles and scattered ground leaves, avoiding a texture set for every colour. Particles use the particle atlas; `assets/minecraft/atlases/blocks.json` explicitly adds that same PNG to the blocks atlas for the litter block and inventory model. Merely referencing a particle texture from a model does not register it in that atlas.

| Biome | Grass | Foliage | Water |
| --- | --- | --- | --- |
| Golden Fields | `#CBB16A` | `#E8BB39` | `#55A6AE` |
| Golden Birch Woods | `#B99B59` | `#E8BB39` | `#55A6AE` |
| Golden Watermeadows | `#C4AA60` | `#EAC34C` | `#65AEB3` |
| Amber Lakes | `#A58D52` | `#CC8537` | `#386C79` |
| Elysian Highlands | `#BBB28A` | `#DDBB68` | `#527D93` |
| Ivory Peaks | `#B9B496` | `#D2B66F` | `#496D88` |

The autumn atmosphere is geographic, not a moving season. The sun remains fixed and even Ivory Peaks avoid snow/ice temperatures. Resource packs and shaders can change the final appearance. New terrain and the revised biome source are best inspected in a fresh world or a regenerated Elysium dimension.

### Regenerating Elysium

Texture and palette fixes apply to existing chunks after restarting and need no reset. To replace old terrain, return all players to the Overworld, fully stop the game/server, and back up the save. Move or delete the entire `<world>/dimensions/elysium/elysium/` directory, including its region, entity, POI and dimension-data files. Elysium regenerates on reopening; its builds and stored items are lost, while the other dimensions remain intact. Do not edit `level.dat` or delete an Overworld/Nether/End directory.

In the pinned Minecraft 1.21.1 loader, the current datapack dimension definition takes precedence over the saved dimension entry when registries are combined. Regenerating this custom dimension therefore picks up Elysium's current biome source and terrain settings without replacing the entire save. Already generated chunks are otherwise retained.

## Architecture informed by BicBiomeCraft

The user's `BicBiomeCraft` branch `mc/1.21.1-neoforge` was reviewed at commit
`cc9e5d491c7598da10743f3ac27d4e051a7bda6f`, including `ParameterizedBiome`,
`BiomeDefinition`, `BiomeDefinitions`, `BiomeType`, `TerrainProfile`,
`BiomeJsonGenerator`, `RuntimeDataPack`, block registration, model tinting,
`MixedTreeBudget`, and `ZoneStorage`.

The main ideas carried forward are:

- Separate biome behavior, palette, and terrain/climate metadata instead of
  introducing a Java class for every visual variant.
- Derive registry resources, feature parameters, and tag membership from the
  same immutable definitions so a content edit cannot silently miss one list.
- Use stable resource IDs. In current Minecraft, loaded biome objects belong to
  data-pack registries; object-identity lookups are the wrong contract across
  world loads. BBC's `ParameterizedBiome` explicitly moved to key-based lookup.
- Reuse vanilla texture references with tint handlers. Gold leaves need no copy
  of a vanilla texture and can retain resource-pack replacements.
- Keep render queries read-only. BBC's sparse, revisioned zone state solves a
  larger system; Elysium's first version needs no persistent per-chunk zone cache
  and therefore no zone sync protocol or mutable global generation state.

BBC has runtime data-pack generation because its definitions must populate many
variants during loading. Elysium currently uses build-time generation to retain
the same maintenance benefit with standard inspectable data-pack resources. Its
small dimension owns its entire biome source, so it does not need BBC's Overworld
injection hooks, climate competition fixes, or BiomeInjectionFix dependency.
No BBC source code or artwork was copied. The generator and hamlet geometry are
original implementations of these architectural ideas.

## Terrain and the buried future

`elysium:terrain_height` combines vanilla climate noise references with an
original height formula. Low erosion raises mountains; the Highlands climate
boxes follow the same erosion signal: highlands span erosion -0.55 to -0.22, and peaks lie below -0.55. Values near zero in ridge noise cut channels below sea level 63. Cooler geographic temperature bands use narrow stream profiles and Watermeadows within ridge ±0.10; temperature above 0.25 selects Amber Lakes within ridge ±0.22. A smoothly blended wider channel profile creates lake-like reaches there. Outside the corridors, humidity separates fields and woods. A smaller custom noise adds gentle ground relief.

The river-distance and autumn-weight functions feed the terrain formula directly; biome boxes use the same raw signals. Climate coverage is checked across humidity, erosion, temperature and ridges, including shared boundaries. This is connected sea-level water shaped by noise, not watershed simulation, flowing upland rivers or a waterfall system.

Alpha 6 separates the lowland bank profile from mountain uplift. Mountain height now enters through a broader shoulder profile, leaving room for flowered banks and settlement terraces before the uplands rise. Highlands and Peaks have a thin soil mantle over pale rock above sea level; calcite can therefore show through stepped cliff faces even where Minecraft's steep-column condition misses a column. The inland field tree budget remains zero. `settlement_clearance` examines available surface columns within six blocks of tree candidates and rejects those close to constructed paving, stairs, slabs, walls or roofs; it never loads a missing chunk.

Every density-function entry point is owned by Elysium: `continents`, `erosion`,
`ridges`, `temperature`, `vegetation`, and both coordinate shifts. They use raw
vanilla noise operators and reference Minecraft's normal-noise parameter sets.
They do not call `minecraft:overworld/*` density aliases. This distinction
matters when co-installing terrain mods: FreeTerraForged can replace those
Overworld aliases with custom markers whose non-Overworld behavior returns zero.
Elysium's functions preserve the vanilla formulas and seed semantics while
avoiding that source-level dependency. No FreeTerraForged engine, mixin, or preset
is required. A co-installation smoke test passed all 12 Elysium GameTests with
FreeTerraForged 1.0.0 loaded. An explicitly enabled FTF Overworld preset remains
untested; this change is not a FreeTerraForged dimension adapter.

Density is proportional to **terrain height minus Y**. It decreases monotonically
upward for each column: there are no 3D noise caves. The settings also disable
aquifers and ore veins; biome lists contain no carvers, ore features, underground fluid springs,
lava lakes, geodes, dungeons, or other underground decorators. Below the shallow
soil and sand, the initial dimension is stone down to its bedrock floor at Y -64,
with pale calcite replacing upper mountain rock above sea level. Explicit
Tartarus structures carve their own space; there are no natural cave systems.

This deliberate height-field design produces hills, valleys, and steep slopes,
but does not yet produce natural arches, overhangs, or floating rocks. It also
does not implement the forge, the sunset crisis, or any progression state. The
fixed-time dimension is the permanent paradise phase until that later feature
is explicitly added.

## Original harvest hamlet

`elysium:harvest_hamlet` chooses between two original 33 × 33 block layouts made
by the generator's `make_hamlet()` function. The courtyard layout has two pale
sandstone cottages with birch beams and spruce roofs, four beds, two irrigated
mature wheat plots, a sheltered fountain, and three villagers. The orchard
layout has three cottages arranged around a large living golden birch, six
beds, a shared wheat plot, benches, and four villagers. Both include a bell,
work blocks, lanterns, and smaller roadside birches. Their geometry is auditable
Python; their compressed NBT is reproducible and contains no imported structure
asset. They use Minecraft blocks and the mod's leaves, with no required
structure mod.

The shared `elysium:landscape_site` codec places hamlets in Golden Fields,
retaining the 28-chunk spacing and 10-chunk separation. Every noise-floor column
in the 33 × 33 footprint must be dry; total relief can be at most three blocks.
Unsuitable hills and wet corners reject the candidate rather than carving a
courtyard into them. Terrain adaptation is `none`. Foundations extend only below
authored bottom-layer soil/stone, for at most the permitted relief. Unused corners
remain untouched. The old jigsaw pool and both template IDs remain available,
and already saved vanilla jigsaw pieces still deserialize normally.

When a suitable bank is nearby, a three-block path continues one of the court's
four exits. It checks dry, gentle terrain up to 48 blocks from the hamlet centre,
requires actual water beyond its endpoint, and stops on the bank. Smooth
sandstone steps handle one-block changes; a short birch landing has a lantern.
The shortest valid cardinal route wins. Paths cannot cross cliffs or water and
are not a road network connecting settlements. A hamlet with no valid nearby
bank simply has its original courtyard paths. Normal villager AI/POIs, beds,
bell and composters supply village behaviour.

## Landscape landmarks and ambience

`elder_spring` combines an original broad, branching golden birch, a contained
pale-stone spring, flowers and a small bench. Its 19 × 19 site tolerates two
blocks of relief. `sunlit_lookout` is a 13 × 13 open pergola with pale pillars,
birch slats, benches and lanterns. It requires three blocks or less of local
relief, ground at least eight blocks above sea level, and a view falling at
least eight blocks at both 24 and 40 blocks away. It rotates its open face toward
the strongest qualifying cardinal view.

Both use `SiteDefinition` and the same `LandscapeStructure`/`LandscapePiece`
implementation as hamlets. Their geometry lives in `tools/landmark_templates.py`
and uses the shared deterministic NBT writer. A common `quiet_landmarks`
structure set has spacing 20, separation 8, a spring/lookout weight of 3:2, and a
three-chunk exclusion around hamlet candidates. These are candidate budgets,
not guaranteed densities: terrain checks can reject either kind. The bridge set
remains independent. No loot or story trigger is attached to these peaceful sites.

`TerrainSampler` shares immutable solid-ground/local-water columns between bridges, landscape sites and mountain towns. Its cache belongs to one candidate. All site decisions use base noise heights/columns,
never neighbouring chunk generation. Saved template rotation, pivot, foundation
depth and bank-path surface profiles keep partial generation stable across
reloads. Block writes and template entities use the generating chunk's clip.
The algorithms assume Elysium's sea-level rivers, not elevated hydrology from
another terrain engine.

Golden Birch Woods, Golden Watermeadows and Amber Lakes have a quiet 24-second looping breeze under Minecraft's
Ambient/Environment volume control. `tools/generate_ambience.py` synthesizes the
original sound and an original 8 × 8 leaf sprite; no third-party recordings or
Minecraft texture bytes are copied. Gold leaves use vanilla cherry-leaf motion,
with a one-in-100 chance per leaf animation tick at exposed canopy undersides.
They appear only in Elysium, respect normal particle settings, and add no server
ticks, entities or network traffic. The wind uses vanilla biome sound fading;
its volume and the leaf frequency still need subjective client tuning.

`elysium:elysian_bridge` is a separate original structure implementation. Its
registered codec uses normal structure settings; its Java locator searches for
real water channels and dry banks before creating a crossing. Candidate regions
use spacing 12 chunks, separation 5, and a three-chunk exclusion around candidate
hamlet chunks. This exclusion is conservative: it excludes a hamlet candidate
even when the biome later prevents that village from generating. Terrain
adaptation is disabled so the structure cannot fill in the river it crosses.
Bridge geometry and terrain checks are in `dev.elysium.structure` Java classes.

Useful inspection commands with cheats enabled:

```mcfunction
/execute in elysium:elysium run locate biome elysium:golden_fields
/execute in elysium:elysium run locate biome elysium:golden_birch_woods
/execute in elysium:elysium run locate biome elysium:elysian_highlands
/execute in elysium:elysium run locate biome elysium:golden_watermeadows
/execute in elysium:elysium run locate biome elysium:amber_lakes
/execute in elysium:elysium run locate biome elysium:ivory_peaks
/execute in elysium:elysium run locate structure elysium:harvest_hamlet
/execute in elysium:elysium run locate structure elysium:elysian_bridge
/execute in elysium:elysium run locate structure elysium:elder_spring
/execute in elysium:elysium run locate structure elysium:sunlit_lookout
/execute in elysium:elysium run locate structure elysium:waterside_cottage
/execute in elysium:elysium run locate structure elysium:mountain_town
/execute in elysium:elysium run place structure elysium:harvest_hamlet ~ ~ ~
```

The generator checks deterministic output and important resource links.
`validate_worldgen.py` independently decodes the shipped NBT and checks climate
coverage and interior overlap, feature-order cycles, structure references,
template bounds, paired doors/beds, clear villager positions, authored air
volumes, the monotone terrain-density contract, and the complete density graph's
independence from external density registry aliases. Its optional
`--minecraft-jar /path/to/minecraft_1.21.1_client.jar` checks the referenced noise
parameters and exact equivalence of all seven climate/shift formulas against the
actual game archive. The
Minecraft registry codecs, actual server chunk generation, and in-game visual
review are separate validation layers; passing the Python check alone is not
evidence of any of those. Vanilla identifiers and codec shapes were checked
against the extracted 1.21.1 data in `misode/mcmeta`'s `1.21.1-data` branch.

The generator also emits the development-only flat world-preset override used
by the headless GameTest server. Its Elysium dimension is the same dictionary as
the production dimension, so climate changes cannot silently diverge between
tests and the mod. Validation checks their equality. The Gradle jar task excludes
`data/minecraft/worldgen/world_preset/flat.json`; it must never override a player's
flat preset in the distributed mod.

## Alpha 5 landscape and buried chain

All four configured tree IDs now share the registered `elysium:birch_crown` foliage placer. Height and radius remain parameters in `TreeShape`; crowns taper vertically, shift slightly near the top and vary at their edges. Branching/canopy variants use collision-checked fancy trunks instead of the old forking umbrella shape. Minecraft still places the trees and updates leaf support distances.

The channel profile reaches full upland height more gradually, creating broader shoulders. A separate X/Z summit noise varies uplift without changing the biome climate signals or introducing natural caves. Steep highland/peak columns use calcite above sea level for continuous exposed faces; underground stone below sea level remains unchanged. Grain patches use 48 attempts over a smaller radius, and coherent density bands leave quieter ground between grain, grass and flower concentrations. Golden Fields still have zero interior tree attempts.

`TartarusChainPlan` owns a vertical route with 11-block-tall, seven-block-wide alternating links spaced seven blocks apart. The route is mostly encased in the existing mountain, without an artificially cleared shaft. A candidate must be in Ivory Peaks above Y 116 with a nearby slope 8–48 blocks beneath the buried top and at least eight actual ring blocks above the original ground; flat terrain and lowlands are rejected. The structure set uses 28-chunk spacing with separation 12, further reduced by biome/terrain rejection, so these are rare clues rather than common scenery.

One `TartarusChainPiece` saves its centre, top, floor and initial link orientation. It places only within the current chunk clip, preserves geology outside the links and chamber, and uses no neighbouring chunk requests. Its radius-12 vaulted chamber starts 16 blocks above the dimension floor (Y -48 in Elysium). The chain pierces the vault and ends in a real central anchor; the chamber has masonry ribs, an open floor and four recessed lights. There is no boss, Forge, timer or sunset trigger in this prototype.

The `elysium:tartarus_chain` block is named **Chain of Tartarus**, uses an original generated 16px texture, appears in the creative tab and requires an iron-tier pickaxe for drops. `generate_ambience.py --textures-only` reproduces both original textures without resynthesizing the audio. `validate_client_assets.py` checks texture existence and atlas membership.

## Alpha 6 vault and foliage revision

New chain chambers have a radius of 36 blocks and a 54-block vault height: a
73-block-wide hall around the buried anchor, with massive basalt piers, an
elevated gallery, four broad stairways and recessed lighting. The chamber floor
remains at Y -48. Candidate checks require stone cover over the entire vault.
The chain stays encased along its descent; no access shaft is carved for the
player. New saves persist both chamber dimensions. Pieces saved before alpha 6
retain their original radius-12, 22-block-high vault when loaded, preventing an
already partly generated chamber from changing size.

The surface clue now requires at least eight exposed ring blocks, including two
blocks belonging to a rounded bend. The terminal end stays buried, and the
texture uses subdued hammered metal with small mineral flecks. The texture
applies to existing blocks after restarting; the larger geometry needs fresh
chunks.

Birch crowns now combine offset ellipsoid clusters with an irregular outline
and a connected centre around their trunk attachments. Small, tall, branching
and canopy forms retain their shared parameterized foliage placer and vanilla
leaf-support updates. The settlement-clearance filter and broader mountain
shoulders described above accompany this change. Golden Fields retain their
open interior tree budget.

## Alpha 7 mountain shoulders and settlements

Adaptive buildings replace the large rigid plots introduced in alpha 6. The
river's immediate banks retain the gentle channel profile, while mountain
uplift starts closer to the river: the separate shoulder mask now rises through
river-distance points 0.12, 0.22, 0.35 and 0.50 before reaching full uplift at
0.70. Lowland biome palettes, rivers, lakes, the golden-field tree budget and
the Tartarus chamber are unchanged. This revises terrain in newly generated
chunks and can produce seams beside older chunks.

Grand towns must climb into this terrain, with higher homes and nearby natural
outcrops inside the inhabited area. Narrow cores, independently fitted wings,
stone cellars and contour roads replace a grid of leveled square plots. See
[the structure design](STRUCTURES.md#adaptive-grand-mountain-towns-alpha-7).
Saved alpha 6 pieces still load with their existing geometry. Use new chunks or
a backed-up dimension reset to inspect alpha 7's complete composition.
