# Settlements and river crossings

Elysium ships original harvest hamlets, waterside cottages, terraced mountain towns, spring trees, lookout pergolas and
terrain-aware bridges, with no structure-mod dependency. The Python generators
own the static geometry, site profiles, biome membership and structure sets.
Java planners handle terrain acceptance, saved approaches and river spans.

## Why these are original structures

Additional Structures and Repurposed Structures were examined before choosing
the implementation. Additional Structures explicitly marks the relevant
1.21–1.21.1 NeoForge branch as **All Rights Reserved** in its
[gradle.properties](https://github.com/XxRexRaptorxX/Additional-Structures/blob/1.21-1.21.1-%40NEO/gradle.properties).
Its large collection of small, biome-selected landmarks is useful design
inspiration. Its buildings, NBT, and source were not copied.

Repurposed Structures has a
[1.21 architecture branch](https://github.com/TelepathicGrunt/RepurposedStructures/tree/1.21-Arch)
under [LGPL-3.0](https://github.com/TelepathicGrunt/RepurposedStructures/blob/1.21-Arch/LICENSE).
Its [birch village configuration](https://github.com/TelepathicGrunt/RepurposedStructures/blob/1.21-Arch/common/src/main/resources/data/repurposed_structures/worldgen/structure/village_birch.json)
uses shared jigsaw machinery, named pools, biome tags, and terrain-height checks.
Those are sensible architecture ideas for later larger settlements. Elysium's
first version keeps its own small palette and placements instead of depending
on that mod's whole content set. No RS code, pool definitions, processors, or
structure assets were copied or bundled. A future optional integration should
reference the installed mod's resources and keep licensing/attribution explicit.

The actual implementation uses the Minecraft 1.21.1 `Structure`,
`StructurePiece`, jigsaw, codec, and structure-set APIs exposed by NeoForge.

## Harvest hamlets

The two 33 × 33 templates contain pale houses, dark roofs, birch beams, mature
wheat, beds, workstations, villagers, bells, golden trees, and lanterns. The
courtyard variant surrounds a fountain; the orchard variant grows around a
larger golden birch. These are small inhabited hamlets, not a modular road
network. Alpha 3 uses the shared landscape-site placer: every column must be
dry and the whole court must fit within three blocks of relief. It rejects
steep edges and adds a short, saved bank approach where gentle terrain reaches
actual water within 48 blocks of the court centre. Old template/pool IDs and
saved jigsaw pieces remain compatible. See [WORLDGEN.md](WORLDGEN.md) for the
site profiles, spring and lookout geometry, spacing and validation.

## Waterside cottages and mountain towns (alpha 4)

`tools/settlement_templates.py` defines reusable, original building geometry with frozen material styles. Waterside cottages use birch, pale sandstone and spruce roofs. The Nordic-inspired mountain style uses dark spruce beams, calcite plaster, steep grey stair roofs, yellow glazing and real lantern light. Cottages have two beds, work blocks and a resident; the larger hall adds a communal table. A covered bell plaza provides the focal point. No third-party templates are bundled.

The 13 × 13 waterside cottage uses the shared landscape-site codec. Its whole footprint must fit within two blocks of relief, and **a bank approach is mandatory**. That approach verifies actual water, gentle dry footing and a short landing, then rotates the cottage's south-facing template entrance toward the bank. Its candidate grid has spacing 18 and separation 7, excludes nearby hamlet candidates, and only permits Watermeadows/Amber Lakes. Most shoreline remains undeveloped. The landing ends on dry ground; this pass does not add floating docks or lake-spanning bridges.

`mountain_town` uses a dedicated codec with data-defined hall/cottage templates, radii, relief limits and population bounds. The candidate grid has spacing 38 and separation 14. A town requires a mountain biome, elevation at least 24 blocks above sea level, and land at least eight blocks higher 64 blocks away in two cardinal directions. These conditions seek a sheltered mountain shoulder; they do not sculpt a mountain range around a town.

The placer first finds a 9 × 9 plaza pad, then chooses a 17 × 17 hall pad and three to five 13 × 13 cottage pads. Every footprint is checked column by column. The plaza tolerates three blocks of relief; buildings tolerate four. Houses face the plaza and retain distinct elevations. Candidates whose buildings overlap, obstruct existing paths, fail terrain checks or cannot connect are rejected. The whole settlement is planned before creating any pieces, so a failed town does not leave partial buildings.

`TerracePlanner` uses a bounded cardinal path search: at most 1,200 visited nodes per connection within a 48-block radius. Routes check a three-block width, one-block grade changes, dry ground and at most four blocks of fill. Building aprons ease down from their pads; one canonical height per path tile keeps junctions consistent. `TerracePathPiece` saves those tiles, their natural ground and resulting surface heights, and places stone stairs and short retaining walls with chunk clipping. It clears three blocks of headroom only on the authored paths. Building templates and paths remain separate saved pieces; there is no settlement-sized platform or blank air box.

`TerrainSampler` centralizes candidate-local base-noise heights and water queries for all three planners. Its immutable column record distinguishes solid ground from the optional local water surface. The current bridge and bank algorithms still require sea-level hydrology; a future terrain adapter must supply **planned** elevated water, including water that its features place later. The sampler is an architectural boundary, not a claim that FreeTerraForged integration is complete.

Lithostitched's released NeoForge 1.21.1 branch was also inspected at [`38779fe2`](https://github.com/Apollounknowndev/lithostitched/tree/38779fe2059d33cc92d9b8012c6eb73bfcaaac96). Its `DelegatingConfig` supports piece counts, depths, placement conditions and adaptation overrides. Its `AlternateJigsawGenerator` checks conditions at a connector before calculating the rotated footprint and aligns nonrigid pieces from an anchor height. Those tools are useful for a larger pool catalogue, but do not replace this full-footprint and connecting-road planner. No new dependency is needed for alpha 4. A future integration should be optional and tested: Lithostitched also redirects vanilla jigsaw generation globally.

## Elysian bridges

`elysium:elysian_bridge` is a registered structure type and a registered saved
piece type. Its data-pack configuration contains ordinary structure settings;
its current geometry constraints live together in `BridgePlanner`.

- The random-spread structure set has 12-chunk spacing and 5-chunk separation.
  These are candidate positions, not a promise of a bridge in each region.
  A three-chunk exclusion reserves space around harvest-hamlet candidates.
- A candidate examines five positions inside its source chunk and both cardinal
  axes. Probe order is seeded using Minecraft's structure RNG. Each noise-floor
  column is cached only for that one candidate; no static world cache is kept.
- The center must contain actual water at the generator's sea level. Noise
  heights must identify a continuous 4–48-block water crossing, with a bank
  found within 32 blocks on each side. Dry plains and open oceans fail.
- Five approach blocks extend past each bank. The resulting bridge is at most
  60 blocks long and five blocks wide, with a three-block walking lane. Both
  ends require two full-width dry rows plus checked ground beyond the exits.
- Banks can differ by at most three blocks, terrain can be at most four blocks
  above sea level, and the underlying water can be at most twelve blocks deep.
  A hill protruding through the planned deck rejects the site.
- The deck stands at least two blocks above sea level. Gentle stepped
  approaches use birch stairs; pale sandstone edging, birch rails and slab
  beams, sandstone piers, and warm lanterns share the hamlet palette.
- Piers reach their sampled floor at fixed intervals and at the bank shoulders.
  They do not run an unbounded fill-down search. The river bed and open channel
  remain in place. Terrain adaptation is `none`, so no terrain beard fills
  the river around the bridge.

Planning uses the chunk generator's base noise columns, not loaded block or
neighbouring chunk queries. Every placed deck, stair, rail, lantern, pier, and
headroom block passes the structure's current chunk clipping box. The complete
terrain profile and geometry are saved in the piece NBT. Reloading a partially
generated bridge therefore uses the same plan even if its first chunks have
already been decorated. The main class must register both deferred registries
with `ModStructures.register(modBus)` before the data-pack codecs load.

The current river model is the standalone Elysium height-field generator at sea
level 63. It deliberately skips unsuitable crossings; it does not connect every
village with roads or solve arbitrary diagonal rivers. Integration with terrain
mods that have elevated local rivers or variable water surfaces needs a new
water-surface contract rather than simply reusing the sea-level assumption.

## Verification and inspection

`BridgeGameTests` covers synthetic crossings versus dry land, endless water,
cliffs, deep ravines and wet landing edges; registered piece save/reload; bounded
stair profiles and immutable saved height arrays; and block placement clipped
to the supplied chunk box. Its fourth test runs the actual loaded structure
codec and Elysium noise generator, looking for a real river without loading
neighbouring chunks. All four bridge tests passed in the headless NeoForge
server. Seed 0 found a 32-block east–west crossing starting at (135, 66, -318),
from source chunk (8, -20), after 15 probes. That demonstrates a valid terrain
candidate; the test does not assert that the random-spread structure set selects
that particular chunk for natural generation. These tests are useful contracts,
not a substitute for visiting several naturally generated bridges and inspecting
their scenery.

An alpha 1 packaged-jar server smoke test also exercised natural placement in
a normal seed-0 world; the following hamlet coordinates predate the alpha 3 placer. `/locate` found a hamlet at `[160, 208]` and a bridge at
`[-528, -336]`. After loading the areas, three hamlet villagers were alive, and
the saved full chunks contained both structure starts. The natural bridge was
44 blocks long, north/south, starting at `(-515, 66, -346)`. Its saved piece
included the complete floor-height profile. See [TESTING.md](TESTING.md) for the
launcher and verification boundaries.

With cheats enabled inside a development world:

```mcfunction
/execute in elysium:elysium run locate structure elysium:harvest_hamlet
/execute in elysium:elysium run locate structure elysium:elysian_bridge
/execute in elysium:elysium run locate structure elysium:elder_spring
/execute in elysium:elysium run locate structure elysium:sunlit_lookout
/execute in elysium:elysium run locate structure elysium:waterside_cottage
/execute in elysium:elysium run locate structure elysium:mountain_town
/execute in elysium:elysium run place structure elysium:elysian_bridge ~ ~ ~
```

The bridge and landscape-site `place` commands can fail on unsuitable terrain, intentionally. Locate
and visit a valid naturally generated crossing when assessing bank alignment.
Check that the approach has dry footing at both ends, that the walkway is
continuous across chunk borders, and that reloading preserves the crossing.

## Chain of Tartarus (alpha 5)

`TartarusChainStructure` samples only Ivory Peaks candidates and verifies an actual exposed ring footprint. `TartarusChainPlan` defines one complete route to a buried vaulted chamber; `TartarusChainPiece` owns the route and chamber together, persists all variable geometry in NBT, and intersects every placement with the current chunk clip. The rings alternate vertical planes with overlapping heights and connected bevel corners. Surrounding stone and ring interiors remain intact until the chain breaks through the chamber roof. The chamber is the initial Tartarus destination; boss, Forge and crisis progression are future work. See [world generation](WORLDGEN.md#alpha-5-landscape-and-buried-chain) for dimensions, placement and material details.
