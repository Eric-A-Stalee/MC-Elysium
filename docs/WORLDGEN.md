# Elysium world generation

The initial world has Golden Fields, Golden Birch Woods, and Elysian Highlands.
They share white birch trunks, gold canopies, straw/ochre grass, clear blue-green water,
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
`GrovePattern`, `TreeShape`, `SiteDefinition`, and `Spawn` dataclasses. Each definition provides the biome's stable resource ID,
display name, climate box, colors, vegetation budgets, animal list, temperature,
surface treatment, and settlement affinities. The same definition emits its biome
JSON, its placed features, tree mixture, dimension biome-source entry, and biome
tags. Adding a fourth biome using the existing tree/terrain/settlement vocabulary
means adding one definition; no independent switch statements or hand-maintained
biome membership lists are needed. Localization must also be provided by the
client's language catalog when introducing a new display name.

Configured features describe the shared small, tall and branching golden birches,
grain, flowers, and grass. Placed features apply each biome's attempt budgets. All
vegetation uses the order ordinary trees → optional riverside trees → flowers → grain → grass. Keeping that order
consistent avoids cross-biome feature sorting cycles and gives trees priority
before filling their clearings. Attempts are not guaranteed placements: vanilla
survival predicates, terrain, existing blocks, and tree collisions can reject
them. Decorations use Minecraft's feature RNG, never global random state.

| Biome | Tree attempts | Grain patches | Flower patches | Grass patches | Tall / branching shares |
| --- | ---: | ---: | ---: | ---: | ---: |
| Golden Fields | 0 inland; 0 or 2 near water | 12 | 2 | 2 | Riverside mixture: 20% / 0% |
| Golden Birch Woods | 2 in sparse areas; 7 in groves | 3 | 3 | 5 | 50% / 20% |
| Elysian Highlands | 3 | 4 | 2 | 3 | 35% / 8% |

The table describes alpha 3; the catalog is authoritative. A grain
patch makes 96 survival-checked placement attempts and needs ordinary soil, not
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

Small birches have requested trunk heights of 5–8 blocks. Tall birches request
9–14 blocks and sample a crown radius of 2 or 3, reducing the former uniform
wide canopy. Branching specimens use vanilla fancy trunk/foliage placers with
birch logs and golden leaves, producing multiple rounded foliage clusters. The
shared tree catalog owns their geometry. Per-biome shares are absolute selection
probabilities; the generator converts them to the conditional probabilities
needed by Minecraft's sequential random selector. Placement failures can change
the mix among surviving trees.

Grass colors are biome effects: Golden Fields use `#CBB16A` (muted straw),
Golden Birch Woods `#B99B59` (ochre), and Elysian Highlands `#C3AE79` (pale gold).
Vanilla grass blocks and biome-tinted grass plants pick up these colors without
new blocks or copied textures. The canopy remains more saturated than the
ground, while birch trunks, pale stone, water and flowers provide contrast.
Resource packs and shaders can change the final appearance or override tinting.
On world restart, these palette changes affect already generated Elysium biomes;
the new tree distributions only affect newly generated chunks.

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
aquifers and ore veins; biome lists contain no carvers, ore features, underground fluid springs,
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

All site decisions use base noise heights/columns with a per-candidate cache,
never neighbouring chunk generation. Saved template rotation, pivot, foundation
depth and bank-path surface profiles keep partial generation stable across
reloads. Block writes and template entities use the generating chunk's clip.
The algorithms assume Elysium's sea-level rivers, not elevated hydrology from
another terrain engine.

Golden Birch Woods have a quiet 24-second looping breeze under Minecraft's
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
/execute in elysium:elysium run locate structure elysium:harvest_hamlet
/execute in elysium:elysium run locate structure elysium:elysian_bridge
/execute in elysium:elysium run locate structure elysium:elder_spring
/execute in elysium:elysium run locate structure elysium:sunlit_lookout
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
