# Elysium world generation

The initial world has Golden Fields, Golden Birch Woods, and Elysian Highlands.
They share white birch trunks, gold canopies, olive grass, clear blue-green water,
flowers, passive animals, and a sun held at time 11000. The distinction is spatial:
open grain country, enclosed tall woodland, and pale rocky uplands. Surface rain,
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
and `Spawn` dataclasses. Each definition provides the biome's stable resource ID,
display name, climate box, colors, vegetation budgets, animal list, temperature,
surface treatment, and settlement affinities. The same definition emits its biome
JSON, four placed features, tree mixture, dimension biome-source entry, and biome
tags. Adding a fourth biome using the existing tree/terrain/settlement vocabulary
means adding one definition; no independent switch statements or hand-maintained
biome membership lists are needed. Localization must also be provided by the
client's language catalog when introducing a new display name.

Configured features describe the shared golden birch, tall golden birch, grain,
flowers, and grass. Placed features apply each biome's attempt budgets. All
vegetation uses the order trees → flowers → grain → grass. Keeping that order
consistent avoids cross-biome feature sorting cycles and gives trees priority
before filling their clearings. Attempts are not guaranteed placements: vanilla
survival predicates, terrain, existing blocks, and tree collisions can reject
them. Decorations use Minecraft's feature RNG, never global random state.

| Biome | Tree attempts | Grain patches | Flower patches | Grass patches | Tall tree fraction |
| --- | ---: | ---: | ---: | ---: | ---: |
| Golden Fields | 1 | 12 | 2 | 2 | 20% |
| Golden Birch Woods | 9 | 3 | 3 | 5 | 65% |
| Elysian Highlands | 3 | 4 | 2 | 3 | 35% |

The table describes the initial defaults; the catalog is authoritative. A grain
patch makes 96 survival-checked placement attempts and needs ordinary soil, not
farmland. Hamlet crops use normal cultivated wheat separately.

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
box follows the same erosion signal. Lower-relief areas become fields or woods
according to humidity. Values near zero in the ridge noise cut connected
channels below sea level 63. A smaller custom noise adds gentle ground relief.

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
aquifers and ore veins; biome lists contain no carvers, ore features, springs,
lava lakes, geodes, dungeons, or other underground decorators. Below the shallow
soil and sand, the initial dimension is ordinary stone down to its bedrock floor
at Y -64. Explicit future forge/Tartarus structures can carve their own space.

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

A single-piece jigsaw pool places it in Golden Fields, with random-spread spacing
28 chunks and separation 10. `beard_thin` terrain adaptation blends its footprint
with the ground. Templates clear only authored house, crop, path and court
volumes; they do not place a blanket of air over the full 33 × 33 bounds, and
their unused corners contain no foundation blocks. The layouts are level
courtyards: they do not yet
assemble multi-part roads along slopes or bridges across rivers, and awkward
placement near steep banks remains a visual playtest concern. The village uses
the normal villager AI/POI system; beds, bell, and composters supply the village
infrastructure. Minecraft 1.21.1 reads the template from the singular
`data/elysium/structure` directory.

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
/execute in elysium:elysium run locate structure elysium:harvest_hamlet
/execute in elysium:elysium run locate structure elysium:elysian_bridge
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
