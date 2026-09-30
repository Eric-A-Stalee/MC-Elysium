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

## Adaptive grand mountain towns (alpha 7)

The existing `elysium:grand_mountain_town` locator now uses the `contour_town`
codec. Its data selects four architectural families and a 14–24 house range.
The smaller `mountain_town` tier and the old `valley_town` codec, piece loaders
and template IDs remain available. Existing buildings are not retrofitted.

`ContourTownPlanner` surveys 240 irregularly distributed sites within 110
blocks of an actual river crossing. A higher hall anchors a street leading
into the slope; a timber watch lodge and homes then grow beside the connected
street network. Site ranking responds to nearby roads, elevation and relief.
Eight-direction path searches allow diagonal contour paths, with at most 5,000
visited nodes per route, three-block-wide walking space and one-block steps.
Roads permit at most three blocks of local cutting or filling. Only the 11 × 11
public square receives a level pad. The rest of the earthwork is inside actual
rooms, foundations and porches; no 23 × 23 lawns are prepared.

A complete town must inhabit both banks, include at least four elevated homes,
put at least two buildings beside terrain seven blocks higher, use at least
three house families, and keep three quarters of its sites in mountain biomes.
Nearby peaks are checked inside the settlement survey. A distant mountainous
backdrop around an otherwise flat neighborhood is insufficient. A failed
complete plan writes no blocks.

`MountainBuildingPlan` surveys a narrow core in four orientations, one or two
attached wings, and alternative gable-end or side entrances. Core widths,
lengths, wing sizes and roof proportions vary by family and seed. Wing floors
can differ by two blocks, connected by internal steps. Main floors allow three
blocks of cut and eight of fill; a five-block cellar can occupy the downhill
lower storey, with additional excavation confined to the house interior.
Covered galleries favour the lower free side and require clearance above the
surveyed ground. They use timber brackets, without a raised lawn underneath.

`MountainArchitecture` is a shared, pure block grammar: spruce and dark-oak
logs, stripped beams, variable plank cladding, limited pale infill, stone lower
walls, framed windows, deep gables, crossing roofs, occasional dormers, ridge
timbers and chimneys. The hall is a larger member of this grammar; the watch
lodge has three storeys and a covered viewing gallery. Upper rooms, split-level
connections, cellars, beds, lights and entrances are checked for usable
circulation. The geometry and block palette adapter are separate so the same
plans can be inspected without a graphical client.

`MountainBuildingPiece` saves the complete room and ground survey, entrance,
gallery side, seed, rotation, grammar version and resident flags. Placement is
clipped and never resamples modified terrain. Supports and the shell precede
doors, beds and hanging attachments, preventing intermediate block updates
from removing them. Two persistent residents start in each house and the hall;
the watch lodge is unstaffed. This is a controlled architectural grammar, not a
general-purpose procedural city or a third-party structure dependency.

## Grand mountain towns (alpha 6, retained save support)

`elysium:grand_mountain_town` is a separate, rarer settlement tier; `mountain_town` retains its smaller village layout. The new `valley_town` codec reads the plaza, hall, tower, house families and population bounds from generated data. Its 52-chunk candidate grid has 20-chunk separation and excludes candidates within eight chunks of the smaller town set. The centre requires actual river water in a mountain biome, with terrain at least 32 blocks above sea level on both sides, sampled in a fan 144 and 192 blocks away beyond the inhabited footprint. At least three houses must stand on each bank, and at least three quarters of the accepted building sites must remain in mountain biomes.

`ValleyTownPlanner` establishes a five-block-wide stone crossing first, anchors a public square at one landing, prefers a higher site for the hall, then grows inhabited districts on both banks. Candidate rows can shift by up to six blocks in either direction to fit nearby terraces. A complete plan requires 14–24 houses, a hall, a watchtower, a square and connected streets. The source chunk's water probes and all lot/path searches are bounded. A route visits at most 4,500 nodes inside a 112-block reach. Every footprint is checked; no large house may hide a submerged corner or an excessive slope. Pads allow up to three blocks of cut and six of fill; their aprons and three-wide streets ease between distinct elevations. A failed complete-town plan places nothing.

`TownTerrainPiece` persists original and final heights per prepared column, then applies clipped cuts, stepped stone retaining foundations and streets before the modules. `LandscapePiece` supplies the existing rotation, clipping and entity handling. The shared bridge piece now persists a stone/birch style flag; older bridges default to birch. The stone variant has masonry decks, stairs, parapets and shallow arch detailing. Unauthored terrain between the lots remains intact. Trees use a bounded settlement-clearance placement filter to keep their crowns away from paving and roofs.

`tools/grand_town_templates.py` owns frozen residence profiles and common building geometry. The three 23 × 23 house templates contain multiple connected rooms, an upper floor reached by a two-block-wide stair, furnished bedrooms, work areas and a ladder-accessible attic. Wings, projecting window sills, supported entrance roofs, a dormer, chimneys, timber framing and intersecting roof surfaces provide depth. The 27 × 27 hall adds a double-height communal space with a guarded upper gallery; a separate 35-block-tall watchtower has accessible landings. The public square is 11 × 11. Two residents start in each home and hall. Templates use referenced vanilla materials and contain no copied third-party geometry.

The independent NBT validator checks door pairs and supports, bed pairs and access, supported lighting, substantial connected upper floors, every resident's route to the street, attic access and the watchtower's top landing. It treats doors as operable and ladders as climbable; it does not claim to simulate villager AI.

## Elysian bridge geometry

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
/execute in elysium:elysium run locate structure elysium:grand_mountain_town
/execute in elysium:elysium run place structure elysium:elysian_bridge ~ ~ ~
```

The bridge and landscape-site `place` commands can fail on unsuitable terrain, intentionally. Locate
and visit a valid naturally generated crossing when assessing bank alignment.
Check that the approach has dry footing at both ends, that the walkway is
continuous across chunk borders, and that reloading preserves the crossing.

## Chain of Tartarus (alpha 5)

`TartarusChainStructure` samples only Ivory Peaks candidates and verifies an actual exposed ring footprint. `TartarusChainPlan` defines one complete route to a buried vaulted chamber; `TartarusChainPiece` owns the route and chamber together, persists all variable geometry in NBT, and intersects every placement with the current chunk clip. The rings alternate vertical planes with overlapping heights and connected bevel corners. Surrounding stone and ring interiors remain intact until the chain breaks through the chamber roof. The chamber is the initial Tartarus destination; boss, Forge and crisis progression are future work. See [world generation](WORLDGEN.md#alpha-5-landscape-and-buried-chain) for dimensions, placement and material details.


## Alpha 8 composed buildings and settlement landscape

The adaptive building grammar now composes unequal masses. Longhouses use a
lower elongated main section with a taller attached section; offset parallel
gables, long downhill wings, and unequal courtyard arms create different
recesses and rooflines. Upper floors can project one block on an unobstructed
side. Each section persists its ridge offset and facade treatment: horizontal
logs, different framing rhythms, timber cladding and limited pale upper infill.
Ridge tips are reserved for selected main roofs rather than repeated everywhere.

Window-side top-slab tables support actual standing lanterns at the lower glass
band. Furnishing happens after doors, stairs and beds; candidates must have a
real exterior window and an open adjoining aisle. Additional upper-floor and
wing lamps are selective. Ceiling lights remain for usable interiors; shader
bloom is not required to see the fixtures.

`TownLandscapePlan` reserves up to eight mature birches before house placement
and street routing. Houses leave their trunks room, and streets route around
them. A saved `TownLandscapePiece` builds overlapping crowns, trims them against
building envelopes, adds short retaining masonry beside falling path edges,
exposes stone on uphill path cuts, and places occasional lamps, flowers and
fallen leaves. Decisions use surveyed heights; no rectangular lawns are added.
The hall ranking favours a higher slope with backing terrain over a bare summit.

Building grammar 2 saves the new room attributes. Grammar-1 pieces retain a
frozen `LegacyMountainArchitecture`, including their old clearing height and
bounds, so partially generated alpha-7 buildings keep their original geometry.
Landscape geometry is versioned independently and remains clipped to the
current generation chunk. There are no world caches or neighboring-chunk
surveys during piece placement.


## Alpha 9 slope-fitted additions and window materials

The planner evaluates wings on alternative sides of the core, shifts their
positions along it, and considers shorter lengths. It scores the actual cut and
fill under each option; room-floor differences remain bounded and every
accepted plan must preserve circulation. Unequal wings can use a lower shed
roof rising toward the core. Porch depth varies from one to three blocks, and
smaller groups are preferred along shared streets. Collision clearances still
leave space between roof envelopes. Main shapes remain assembled from
orthogonal rooms; this revision does not implement arbitrary polygonal rooms.

Facade windows use smaller pairs and **clear glass**. Selected lower window
cells become actual glowstone, retaining clear glass directly above and quartz
sills below; some also have quartz lintels. Table lanterns remain visible through
their own panes. These use ordinary game blocks and need no emissive resource
pack. Most ridges now use roof slabs rather than repeating bark across every
roof, and uphill cladding can become stone where the ground meets the wall.

Grammar 3 serializes the shed direction and porch length. Grammar 2 delegates
to the frozen `MountainArchitectureV2`, including its grey stained glass, while
grammar 1 retains the earlier frozen implementation. Old pieces are not migrated
on load. The new tree form uses landscape version 2; version-1 trees likewise
keep their original trunks and crowns in partially generated chunks.
