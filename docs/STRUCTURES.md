# Settlements and river crossings

Elysium ships original harvest hamlets and terrain-aware bridges, with no
structure-mod dependency. `tools/generate_worldgen.py` owns the hamlet geometry,
template pools, biome membership, and both structure sets. The bridge geometry
is Java because its length and approaches depend on the actual river banks.

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
network. Vanilla jigsaw placement adapts their footprint to the terrain, but
steep-edge placement still needs visual inspection. See [WORLDGEN.md](WORLDGEN.md)
for the generator, palette, spawn spacing, and template validation.

## Elysian bridges

`elysium:elysian_bridge` is a registered structure type and a registered saved
piece type. Its data-pack configuration contains ordinary structure settings;
its current geometry constraints live together in `BridgePlanner`.

- The random-spread structure set has 12-chunk spacing and 5-chunk separation.
  These are candidate positions, not a promise of a bridge in each region.
  A three-chunk exclusion around harvest-hamlet candidates prevents overlap.
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

A separate packaged-jar server smoke test also exercised natural placement in
a normal seed-0 world. `/locate` found a hamlet at `[160, 208]` and a bridge at
`[-528, -336]`. After loading the areas, three hamlet villagers were alive, and
the saved full chunks contained both structure starts. The natural bridge was
44 blocks long, north/south, starting at `(-515, 66, -346)`. Its saved piece
included the complete floor-height profile. See [TESTING.md](TESTING.md) for the
launcher and verification boundaries.

With cheats enabled inside a development world:

```mcfunction
/execute in elysium:elysium run locate structure elysium:harvest_hamlet
/execute in elysium:elysium run locate structure elysium:elysian_bridge
/execute in elysium:elysium run place structure elysium:elysian_bridge ~ ~ ~
```

The bridge `place` command can fail on unsuitable terrain, intentionally. Locate
and visit a valid naturally generated crossing when assessing bank alignment.
Check that the approach has dry footing at both ends, that the walkway is
continuous across chunk borders, and that reloading preserves the crossing.
