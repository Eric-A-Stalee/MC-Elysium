# Validation and first playtest

## Reproduce the automated checks

Use a Java 21 JDK, Minecraft 1.21.1 and the pinned NeoForge 21.1.233 development environment. Python 3.10+ is needed only for the resource checks.

```sh
python3 tools/generate_assets.py --check
python3 tools/generate_ambience.py --check
python3 tools/validate_client_assets.py
python3 tools/generate_worldgen.py --check
python3 tools/validate_worldgen.py
./gradlew build
./gradlew runGameTestServer
```

Minecraft's server may require accepting its EULA in `run/eula.txt` before starting. The GitHub Actions workflow runs these checks and uploads the mod jar and test logs.

The development flat world preset includes the exact same Elysium dimension definition as the shipped dimension. This is necessary because Minecraft's GameTest server constructs its dimensions from the flat preset. Both that development preset and all GameTest classes/fixtures are excluded from the player jar; ordinary worlds keep their normal vanilla preset.

## Results recorded on September 22, 2026

`build` completed successfully with Java 21.0.12.1 and NeoForge 21.1.233. All **12 required GameTests passed** on the actual headless Minecraft server, both standalone and with the official FreeTerraForged 1.0.0 NeoForge jar installed. A 13th test for portal spawns was added on September 24, 2026; all 13 passed standalone, and the FreeTerraForged co-installation run has not been repeated.

| Checks | What they establish |
| --- | --- |
| Mature wheat loot | Guaranteed and disabled configured rolls behave correctly; immature wheat cannot award shards. The rare default itself is not tested by waiting for a lucky roll. |
| Dimension and terrain | All three biomes load; real chunks generate; sampled underground positions contain stone; the sun stays fixed; ordinary monster spawn lists are empty. |
| Weather isolation | Elysium clears its own rain/thunder without clearing Overworld rain. |
| Portal spawns | Nether-portal zombified piglin spawns are blocked in Elysium; command spawns there and Overworld portal spawns still work. |
| Shrine and time | Missing hay, sideways pillars, and solid/glass roofs are rejected; repairs restore validity; sunset boundaries work on later days. |
| Landing safety | Blocked headroom, lava, water, magma, and unsupported air are rejected. |
| Return altar | Generated return stones do not duplicate sigils; breaking an entrance returns its installed sigil; replacing a return stone inside Elysium keeps it usable. |
| Bridge geometry | Dry ground, overly wide water, and cliffs are rejected; stair profiles survive the registered structure-piece save/load path. |
| Bridge placement | Decks, rails, and supports respect the current chunk's generation box. |
| Real river crossing | The planner finds a viable crossing using Elysium's actual noise columns rather than neighboring generated chunks. |

The real-terrain bridge probe used seed `0` and found a 32-block east/west crossing starting at `(135, 66, -318)`, from source chunk `[8, -20]`, after 15 probes. This check exercises the planner, not the natural structure-set spacing selection.

A separate ordinary dedicated-server smoke test then loaded the **packaged player jar** from `mods/`, without the development source folders or flat-preset fixture. It used the mapped NeoForge development launcher, a new normal world, and seed `0`. Startup completed, Elysium loaded automatically, and normal `/locate` selection found a harvest hamlet at `[160, 208]` and an Elysian bridge at `[-528, -336]`. Loading those areas produced three living hamlet villagers and saved both structures in fully generated chunks. The saved natural bridge has 44 blocks along its north/south axis, starting at `(-515, 66, -346)`, with its terrain profile serialized. This validates natural structure selection and generation separately from the GameTests, but not graphical appearance or an installer-created production server distribution.

The Python validator independently decodes both generated NBT hamlets, checks their 11,368 authored block positions and seven villagers, probes 25 climate points, checks 12 ordered features, verifies the nine Elysium-owned density functions form an isolated acyclic graph, and checks the solid-depth terrain invariant. With `--minecraft-jar /path/to/minecraft_1.21.1_client.jar`, it also verified six raw vanilla noise parameters and seven climate formulas against the actual Minecraft archive. Both generators reproduce all 87 committed outputs.

The co-installation test loaded both mod IDs and passed the same 12 tests with identical bridge probe geometry. It **did not activate a FreeTerraForged Overworld preset**, test its multiplayer flow behavior, or make its terrain engine generate Elysium. See [the integration investigation](RETERRAFORGED.md).

## Alpha 2 visual revision (September 28, 2026)

The first in-game screenshots informed a focused landscape revision: no ordinary
tree spawning in Golden Fields; straw/ochre biome grass tints; coherent sparse
patches within Golden Birch Woods; and small, tall and branching birch forms.
The generators now emit 88 resources in total. Run the same 13-test suite and
resource checks above; CI uploads the resulting alpha 2 jar and server logs.

Compare new chunks for vegetation changes and existing Elysium chunks after a
world restart for grass colors. Hamlet garden trees and player-planted trees
remain valid in the fields. The new grass tints, tree silhouettes and woodland
openings still need visual comparison with the player's resource pack/shaders.
Grass uses normal biome coloring; packs that override it can produce a different
result. River-specific groves are not implemented in this revision.

## Alpha 3 landscape pass (September 28, 2026)

The landscape build compiles and runs **19 required GameTests**. The six added
checks cover full-footprint rejection of narrow ridges and submerged corners;
real-water bank paths and bounded slopes; registered template/path save-reload;
path chunk clipping; dry-field and covered-water rejection for riverside trees;
actual spring placement and lantern support; and all three loaded site codecs
against the real Elysium noise generator. They preserve all thirteen earlier
portal, weather, loot, dimension and bridge checks.

In the first passing run, seed 0 yielded an elder spring and a hamlet candidate
in chunk `[-45, -45]` on the first probe, and a lookout in chunk `[3, -45]` after
17 probes. The entire 19-test suite took 3.993 seconds. These are independent
planner probes, not simultaneous natural placements or a claim that structure
spacing selects those chunks. The hamlet found in that probe had no bank path;
path geometry and rejection conditions are tested separately.

Resource checks reproduce 54 worldgen resources and 42 other JSON assets,
validate 13 ordered features and the two new landmark templates' 1,655 authored
positions, and retain the original hamlet, climate and density checks. Spring
sources must have authored floors and containing rims. The original sound is
mono Vorbis, 24 kHz, exactly 24 seconds; the leaf sprite is 8 × 8 RGBA.

Alpha 3 is not yet a graphical client playtest, a multi-seed frequency/performance
survey, or a repeat of the packaged-jar/FreeTerraForged smoke tests above. In
particular, listen for sound seams/volume and inspect particle frequency with the
actual resource pack and shaders. Natural site frequency, bank path availability
and terrain seams still need exploration in new chunks.

## Alpha 4 autumn and settlement pass (September 28, 2026)

[GitHub Actions run 36411321707](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36411321707) built commit `dbd9b8534170293902f8e122b8b75168d2ef1f94` successfully and passed **all 23 required GameTests** in **4.886 seconds**. This includes the previous 19 checks, expanded to cover all six registered biomes and the required water approach for cottages.

The four additional tests verify:

- Every doorway in a synthetic sloping town connects to its plaza with at most one-block rises; building elevations differ, intersections have one saved surface, and steep terrain is rejected.
- Terrace paths retain their terrain/foundation profile through registered NBT loading, place actual retaining blocks and stairs, and respect chunk clips.
- A 4,225-column seed-0 survey across ±6,144 blocks finds all six biomes and real water in both new lowland corridors. The wet samples included 182 Watermeadow and 109 Amber Lake columns. This is a coarse occurrence check, not a shoreline-width or visual-quality measurement.
- The actual registered mountain-town codec finds a complete site in seed 0 at candidate chunk `[-4, -108]` after 210 probes: plaza, hall, four cottages and a saved path piece. Every resulting piece round-trips through its registered loader.

The waterside cottage probe found candidate chunk `[-24, -45]` after eight attempts and produced both the house and required bank path. The bridge probe still found a valid crossing, now 33 blocks long under the revised channel profile. These site coordinates are planner probes: they do not assert that random-spread structure sets select those chunks, nor that a player has visited them.

The independent validator checks 1,925 four-axis climate samples, 26 ordered features and 11 isolated density functions. It decodes five new settlement modules containing 10,578 authored positions, checks paired beds/doors and lantern supports, and finds a geometric route from each resident's starting position to the external path connector. The initial check caught a hall resident intersecting the communal table; the corrected spawn is in the aisle. These checks supplement the original hamlet/landmark validation. The generators reproduce 88 worldgen resources and 46 other JSON assets.

This pass was tested standalone. The older packaged-server and FreeTerraForged co-install results above have not been repeated for alpha 4. No graphical Minecraft client was available; natural settlement frequency across multiple seeds, in-game appearance and actual villager behaviour still need player inspection. Use a fresh world for the full new biome source and terrain layout.

## Alpha 4.1 fallen-leaf texture fix (September 29, 2026)

Player screenshots showed the missing-texture checkerboard on placed leaf litter and its inventory icon. The original PNG was present, but only the particle atlas loaded it. The generated `minecraft:blocks` atlas now adds `elysium:particle/golden_leaf` with a single-sprite source, sharing the image between the ground model, inventory item and falling particles.

The independent client-asset validator reproduces the original failure for both litter models before the atlas entry is added. Corrected resources pass all three custom texture references. Removing the PNG from a temporary resource copy also fails for both models and the particle definition. CI runs this validator alongside the existing generated-resource and worldgen checks. This verifies resource wiring; a graphical client is still needed to confirm the rendered result with the player's resource pack and shaders.

This update changes no world generation or saved block IDs. Existing leaf litter needs only the updated jar and a client restart, not new chunks or a dimension reset.

[GitHub Actions run 36514620141](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36514620141) built commit `a2455c4b169b8f6633a68e69287f334b9e9f1d03` successfully. All **23 required GameTests passed** in **5.755 seconds**, and the generated-resource, client-atlas and worldgen checks passed. These server tests do not exercise rendering.

## What still needs a client playtest

No graphical Minecraft client was available in the development environment. The mod is an early alpha, with server and data validation rather than a completed visual playthrough.

1. Install the jar on both client and server with NeoForge 21.1.233+, and create a new world. Use `/elysium visit` with operator permission for a quick inspection.
2. Inspect all six biomes across several seeds: tree shapes, foliage and item tinting, water and sky colors, mountain slopes, lighting, and frame/chunk-generation performance.
3. Locate `elysium:harvest_hamlet`, `elysium:elysian_bridge`, `elysium:elder_spring` `elysium:sunlit_lookout`, `elysium:waterside_cottage`, `elysium:mountain_town` and `elysium:grand_mountain_town` inside Elysium. Check cottage doors, beds, villager pathfinding, farms, terrain fit, bridge rails, bank landings, spring containment and the lookout’s orientation. Harvest hamlets remain complete layouts; mountain towns now connect independently fitted houses. Check their junctions, short retaining walls, door approaches and lanterns on several slopes.
4. Follow [the survival entrance guide](ENTRANCE.md). Check item insertion, the sunset offering, particles, title, cooldown, two-way travel, and inventory on an actual connected player.
5. Test two players entering from different shrines, reconnecting, dying/respawning, server restarts, rebuilding a return stone, and `/elysium return` after losing it. Safe-position predicates and persistence code have been checked, but these are not automated end-to-end connected-player tests.
6. If experimenting with FreeTerraForged, test an explicitly enabled Overworld preset in a disposable world. Merely installing its jar is a narrower compatibility check.

Natural ore generation, underground caves, advanced hydrology/waterfalls, custom NPC behavior, custom shaders, and later story realms are outside this first paradise milestone. The independent terrain is a height field with river cuts, not an overhang-capable Uplift integration.

## Alpha 5 validation targets

The new tests grow all four registered birch variants and require a trunk, at least five foliage layers and a tapered top. Chain tests reject unsuitable terrain, trace every vertical level down to the anchor, verify the excavated chamber and intact surrounding geology, round-trip through the registered NBT loader, and reconstruct the complete structure from reversed clipped halves. A seed-0 survey probes actual random-spread chain candidate chunks rather than arbitrary cliff coordinates.

Graphical playtesting must inspect tree silhouettes and leaf retention, chain appearance with resource packs/shaders, exposed-link readability, and excavation into the chamber. Use `/locate structure elysium:tartarus_chain` inside Elysium. New generation needs fresh chunks; the retained texture fix works on existing leaf litter.

[GitHub Actions run 36518494657](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36518494657) built commit `54edd0729ec51a8700ab90b4c901fc8522c96ec7` and passed **all 27 required GameTests** in **7.469 seconds**. The four new tests passed alongside all previous portal, dimension, climate, hydrology and settlement tests. The generated-content checks reproduce 92 worldgen resources and 52 other JSON resources, plus the original leaf and chain textures.

The natural-placement survey found a qualifying seed-0 random-spread candidate at chunk `[985, -911]` after 636 candidates: chain centre `(15761, -14571)`, planned top Y 116, chamber floor Y -48. This is a valid structure-start candidate, not a claimed graphical visit. All six biomes remained present in the climate survey, with actual water in both lowland corridors.

The initial chamber test compared cave-air identity in empty sky sections, where Minecraft can retain ordinary air; the final test excavates a prefilled stone volume and verifies the entire result, untouched encasing stone, and an ungenerated neighbouring half. The stricter placement rule also rejected the first synthetic cliff fixture because too few actual ring blocks were exposed; its corrected cliff exposes 13. No graphical client or renewed FreeTerraForged integration test was run for alpha 5.

## Alpha 5.1 item opacity fix (September 29, 2026)

The player reported a visible leaf-litter inventory icon but invisible held and dropped items. The pinned Minecraft 1.21.1 source confirms that `ItemRenderer.renderQuadList` reads alpha from the item color handler's ARGB value and forwards it to the vertex consumer. Both Elysium item handlers returned 24-bit RGB, leaving alpha at zero. They now use vanilla's `FastColor.ARGB32.opaque` convention: foliage is `0xFFE8BB39` and relics are `0xFFFFE4A1`. Block biome colors and all texture resources remain unchanged.

The existing dedicated-server tests cannot verify item rendering. In a graphical client, check fallen leaves and golden birch leaves in the inventory, both hands, and as dropped items; repeat for a shard, fragment and sigil. Check placed leaf litter and falling canopy particles as well. This hotfix changes no generation or saved IDs and requires only replacing the jar and restarting.

[GitHub Actions run 36535195310](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36535195310) built commit `bd80b02c547eee04785d1dd936063e90ecaa496b` successfully. All **27 required GameTests passed** in **6.746 seconds**, along with the generated-resource, client-atlas and worldgen checks. No graphical client test was run.


## Alpha 6 grand settlements and vaults (September 29, 2026)

Four additional server tests cover connected grand-town streets, saved terrain
cuts/fills, the expanded vault's gallery and stairs, and a complete natural town
candidate. The synthetic valley must have homes on both banks, a real crossing,
and a route from every doorway over the saved street graph. A cliff-sized house
cut is rejected. The earthwork test verifies actual block removal, registered
NBT loading and chunk clipping. Legacy chain NBT retains its original vault
size; new vaults have a 73-block span and 54-block height.

The natural town survey respects random-spread selection and its small-town
exclusion zone. Minecraft's GameTest world uses `WorldOptions(0, false, false)`,
so it disables automatic structure generation. The test therefore calls the
normal `Structure.generate` API explicitly at the qualifying candidate,
round-trips the complete `StructureStart` through NBT, and places it onto actual
Elysium terrain with `placeInChunk`, using reversed chunk order and ordinary
chunk clips. It checks interior floors in the hall, tower and every home, plus
the stone crossing and its headroom. The chain survey uses the same path and
checks the buried anchor and an expanded gallery in a neighbouring chunk.
This exercises real server terrain and structure placement, but does not replace
an ordinary-world automatic-generation smoke test, graphical playtest or
multi-seed frequency survey.

Early runs caught placement constraints that synthetic terrain did not expose:
the original mountain shoulders were too steep for larger buildings, the
backdrop check sampled inside the settlement's footprint, and fixed rows could
miss nearby usable terraces. The revised planner keeps its full-footprint,
road-grade, cut/fill and minimum-size requirements. It can shift sites and use
outer terraces, while sampling the enclosing mountains beyond the town.

The independent NBT validator verifies all three larger house families, the
hall and tower: connected multi-room floor plans, substantial accessible upper
floors, resident-to-street routes, paired doors and beds, supported lanterns,
attic ladders and the tower's upper landing. This geometric reachability check
treats doors as operable; it does not simulate villager decisions. The generators
reproduce 100 worldgen resources and 52 other JSON assets. Client checks retain
the four custom texture/atlas references and the alpha 5.1 item-opacity fix.

Player inspection should focus on the town's overall silhouette and mountain
setting, the transitions between individual terraces and streets, house-room
proportions, roof intersections, tree crowns near structures, and the scale and
lighting of the new vault. The generated geometry was inspected outside
Minecraft, but no graphical Minecraft client, shader comparison, new
FreeTerraForged co-installation test or connected-player traversal was run.


[GitHub Actions run 36557856432](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36557856432)
built commit `274e1e76d769404f71ef1e6b1e2946b23e993f5b` successfully. All **31 required
GameTests passed in 29.01 seconds**, including the complete-start placement
checks above. Seed 0 produced a qualifying grand-town candidate at chunk
`[1103, -2468]` after 322 candidate probes: **17 houses, hall, tower, square,
terrain piece and stone crossing** (22 saved pieces). The chain survey selected
chunk `[-408, -811]` after 906 probes, with centre `(-6519, -12963)`, top Y 117
and chamber floor Y -48. These search positions are reproducible test candidates,
not measurements of average settlement spacing or graphical visits.

The explicit placement pass logged one POI/deferred-block-entity warning at a
position replaced by a door. It places structures over already decorated chunks;
normal world generation places structures before vegetation. The warning's
behaviour in a normal world has not been checked in this revision. All required
geometry, saved-state, floor, bridge, anchor and gallery assertions passed.

The downloaded player jar passed ZIP integrity checks. All **157 shipped source
resources match** the repository byte for byte, all four custom client texture
references validate from the jar, and development tests, fixtures and the flat
world preset are excluded. The alpha 6 jar is 372,031 bytes, with SHA-256
`69a2cd36a2b28b6abd0e9433c48ffcc15e6f5b74a824407423ea5d63a1b2deeb`.

## Alpha 7 adaptive Nordic settlements (September 30, 2026)

[GitHub Actions run 36659749968](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36659749968)
built commit `3abe45bfe6bb4fe9cb5ebaa2085f759436ae7a0f` successfully. All **35
required server GameTests passed**, along with generated resources, worldgen,
texture/atlas validation, and the separate optional erosion-guard CI job.

The new pure grammar checks traverse **617 fitted building configurations**
across six architectural families, four rotations and flat/sloping terrain.
They cover 311 distinct room/height combinations, 218 cellar cases and 228
split-level cases. They require access from the porch to every inhabited floor,
cellars and covered galleries, reachable paired beds, supported hanging lights
and complete entrance doors. Doors are treated as operable; this is geometric
circulation, not a simulation of villager AI.

Three synthetic valley seeds each produce 24 homes plus their hall and watch
lodge. Tests traverse the saved street/porch graph from the bridge to every
building, enforce three-block road earthwork limits and exactly 121 square-pad
columns, require buildings beside higher natural terrain, and verify repeatable
planning. A cliff-sized cut and a flat neighborhood fail. No per-house square
pad is permitted. Registered save/load and real block placement check all four
rotations, chunk clips, untouched ground inside irregular bounding boxes,
resident creation, doors and both halves of beds.

The natural-candidate survey still respects the structure set's random spread
and exclusion rules. At seed 0, chunk **[230, -2369]** qualifies after 545
candidate probes: **24 homes, hall, watch lodge, square, streets and stone
crossing**, represented by 29 saved pieces. The 26 building plans span main
floors **Y 64–99**, with **15 cellars, 19 buildings with split-level wings and
13 covered galleries**. House families comprise four longhouses, seven
cross-gabled homes, six hillside lodges and seven courtyard houses. A saved
terrain/footprint report, `run/logs/contour-town.json` in the CI log artifact,
supports inspection of the actual surveyed layout.

An initial placement run caught doors reacting to incomplete supports during
construction. Shells and supports now precede attached blocks, with paired
attachments placed using Minecraft's known-shape update flag. The final run
checks actual doors, beds and interior floors after a complete StructureStart
is reloaded and placed in reversed chunk order. A test fixture was also revised
to select a valid surveyed building in each orientation, since a single fixed
plan deliberately cannot fit every orientation on a slope.

As in alpha 6, GameTest disables ordinary automatic structure generation. The
natural survey therefore places the saved start explicitly on real generated
terrain. Four deferred block-entity warnings occurred at positions overwritten
in already decorated chunks. Normal generation places structures before trees;
an ordinary-world automatic-generation smoke test has not been repeated for
this revision. There was no graphical Minecraft client, shader comparison,
multi-seed natural-frequency study or new full ReTerraForged pack test. Client
inspection should assess the settlement silhouette, rock between buildings,
roof intersections, galleries, street steps and villagers' actual behaviour.

The downloaded player jar passed ZIP integrity and Java 21 class checks. All
**157 shipped source resources match** the repository. Development tests,
fixtures and the GameTest flat-world override are excluded. The jar is
**428,186 bytes**, with SHA-256
`965631cb0789e8b90f1600254d09f11934451e9d3c1803fd6db58f71c65e9cab`.

## Alpha 8 composed houses and settlement atmosphere (September 30, 2026)

[GitHub Actions run 36666361371](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36666361371)
built commit `c4fd92b729a4687b3702155016c4948a28b6374a` successfully. All **38
required server GameTests passed in 1.260 minutes**. Generated assets, client
texture references, 101 worldgen resources, packaging and the separate optional
erosion-guard checks also passed.

The revised building grammar passes **613 fitted plans**, covering 311 distinct
room/height combinations, 211 cellars and 313 split-level cases. Circulation
checks require access to every inhabited floor, including taller wings on
single-storey longhouses; they also cover paired beds, doors, supported lights
and standing lanterns beside real exterior windows. Early geometry checks
caught stairs intersecting room junctions and too-small upper wings. The final
plans move those stairs and provide enough usable floor space.

Three synthetic valley seeds produce 24, 24 and 23 homes, each with a hall and
watch lodge. The added server tests verify broad tree crowns and clear lower
trunks, deterministic landscape planning, registered landscape save/load,
chunk clipping, persisted building clearances and persistent placed leaves.
Frozen grammar-1 geometry survives the old seven-value room NBT format, while
new pieces retain their extended attributes. Actual placed window tables use
top slabs with supported standing lanterns.

The seed-0 natural-candidate survey selected chunk **[-2986, -1351]** after
2,883 candidate probes: **16 homes, hall, watch lodge, square, streets, stone
crossing and landscape**, represented by 22 saved pieces. Main floors span
**Y 63–92**; the surrounding surveyed ground spans **Y 58–124**. House families
include two longhouses, five cross-gabled homes, five hillside lodges and four
courtyard houses. Eight mature birches are reserved before buildings and roads.
The saved landscape includes 41 stone edging blocks with 41 caps, 50 exposed
rock blocks, 124 flower positions, 47 leaf-litter positions, 30 post blocks and
10 lanterns. An initial run found that the town's gently graded roads never
triggered two-block retaining edges; the final planner also admits intermittent
one-block accents, and the natural-town test requires some masonry or rock.

The complete saved start is reloaded and explicitly placed in reversed chunk
order over real generated terrain. Its layout is available in
`run/logs/contour-town.json` in the CI log artifact. This respects natural
candidate selection, but GameTest disables ordinary automatic structure
generation. It does not establish average town frequency or replace an
ordinary-world generation smoke test. The Tartarus test still finds and places
the vaulted chain chamber; its seed-0 candidate is chunk [-332, -778], with
centre (-5307, -12443), top Y 165 and floor Y -48.

Explicit placement over completed chunks logs 289 unsupported deferred
post-processing warnings and one stale deferred block entity at a position
replaced by spruce planks. This test path differs from normal generation into
ProtoChunks; ordinary-world behaviour has not been rechecked in this revision.
The synchronous terrain searches also cause two server tick-lag warnings.
Required block, attachment, circulation and saved-state assertions all pass.
There was no graphical client run, shader comparison, villager-AI traversal or
new ReTerraForged co-installation test. Player inspection should assess roof
intersections, upper-floor projections, trees against buildings, vanilla gold
colours, riverbed readability and the light seen through windows.

The downloaded player jar passed ZIP integrity and Java 21 class checks. All
**158 shipped source resources match** the repository; development tests,
fixtures and the GameTest flat-world override are excluded. The jar is
**464,808 bytes**, with SHA-256
`309a75cba1b8d22870d8f1257af0b5e2ee49d4c9f95cc129d689c85368c3e5b8`.


## Alpha 9 cliff districts, branching trees and sunset windows (September 30, 2026)

The pure building matrix exercises **669 fitted plans**, covering 426 distinct
room/height combinations, 261 cellars and 367 split-level cases. Three synthetic
valley seeds each retain 24 homes plus a hall and watch lodge. Their final
street graphs contain 2,802, 3,052 and 2,769 saved columns respectively; every
porch is reachable from the bridge with at most one-block steps. Eight, four
and seven mature trees remain after branch-clearance checks.

The first server run exposed a missing approach underneath an upper gallery.
A shorter porch ended inside that overhead footprint, which the road writer
mistakenly treated as a ground-level obstruction. Road placement now permits
the entrance approach underneath the gallery, and grading measures distance
from the entire porch edge. The existing complete street-graph assertion found
the issue; the graph was reproduced locally and all three valley seeds pass
with the correction.

Server coverage additionally checks connected branching logs, lower forks,
clear trunk bases, save/load and clipping, real glowstone window centres with
clear upper panes and quartz sills, and preservation of both older house
grammars and the alpha-8 tree geometry. The natural-town survey requires nearby
steep faces overlooking at least three buildings. Smooth, high hills alone do
not satisfy the backdrop test. Geometry inspections use generated blocks and
terrain reports, not a graphical Minecraft client.


[GitHub Actions run 36672261255](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/36672261255)
built commit `96de20162b548ec1191d26aadf9651d2f3d9ddf3` and passed **all 39
required GameTests in 1.404 minutes**. Generated resources, client assets,
packaging and the independent optional compatibility-adapter checks passed too.
This build includes the newer compatibility change that removes the Elysium
release pin.

The final seed-0 survey found chunk **[-2335, -2316]** after 616 candidate
probes: **16 homes, a hall and watch lodge**, plus square, streets, crossing and
landscape, for 22 saved pieces. Main floors span **Y 65–113** and the surrounding
survey spans **Y 58–176**. Six buildings meet the close-cliff relationship; six
branching birches remain, with height parameters 16–19. The complete reloaded
start passes placement in reversed chunk order. The CI log artifact contains
`run/logs/contour-town.json`, including surveyed cliff-foot and crest positions.
These are reproducible candidate results, not average discovery distances.

The explicit structure-placement test writes into completed chunks and logs
302 unsupported deferred post-processing warnings; normal world generation
uses ProtoChunks. The synchronous searches also log two tick-lag warnings.
No required assertion failed. An ordinary-world automatic-generation smoke
test, graphical client comparison, villager-AI traversal and full ReTerraForged
co-installation run were not repeated. In particular, the later sun and new
windows remain a visual trial with the player's shaders enabled and disabled.

The downloaded player jar passes ZIP integrity and Java 21 class checks; all
**158 shipped source resources match**, and development tests, fixtures and the
flat-world override are excluded. The jar is **486,344 bytes**, SHA-256
`0db8248486922e6c79e27bd1a413881118ba1f943d2959495b2c07b03d27e18e`.


## Alpha 10 palette, crowns and hillside building edges (October 15, 2026)

[GitHub Actions run 38684483376](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38684483376)
built commit `3633f4a573859ee6f1fae0dabab6078270adea4d` and passed **all 40
required GameTests in 1.708 minutes**. The generated-resource checks, Java 21
NeoForge package and optional compatibility guard checks passed.

The 669-plan building matrix retains usable doors, rooms, stairs, beds, galleries
and table lanterns. The three complete synthetic towns keep 24 homes plus hall
and watch lodge, with every porch connected to the bridge. Landscape checks
now verify that hillside details cannot overwrite any authored building block
or its cleared interior/gallery air, that exposed rock does not raise the
surveyed hill, and that saved detail counts stay within the format's budget.
The pure checks caught a projecting bay replacing a table lantern's window;
placing bays before window furnishings resolves that conflict without relaxing
the window-visibility assertion.

New tree checks require at least ten blocks of vertical canopy depth alongside
face-connected branches, clear lower trunks, saved bounds and chunk clipping.
An additional regression test loads grammar-3 buildings and version-2 trees,
round-trips their NBT, and compares geometry hashes captured from alpha 9 before
these edits. Older grammar and tree fixtures still run. Flat-colour renders of
actual generated blocks were inspected for the new crowns and roof variants;
these are geometry inspections, not Minecraft client screenshots.

The natural seed-0 survey again finds chunk **[-2335, -2316]** after 616
candidates. The revised terrain supports **14 homes, a hall and watch lodge**,
plus square, streets, bridge and landscape, for 20 saved pieces. Floors span
**Y65–115**; the surrounding height survey spans **Y55–181**. Seven buildings
sit beneath close cliff backdrops. Six version-3 mature birches remain, with
height parameters 16–18. The 2,624 saved landscape details include 1,573 pale or
andesite rock-facing blocks, 722 masonry blocks, 143 wall caps, 120 flowers,
26 leaf-litter positions and ten post lamps. The complete start survives reload
and reversed-chunk placement on real generated terrain. The CI log artifact
contains `run/logs/contour-town.json` with the exact plans and height samples.

The chain candidate is now seed 0, chunk **[-70, -938]**, at X−1111/Z−15003,
with surface top Y129 and the unchanged chamber floor Y−48. Terrain changes
can alter which candidate is discovered first; the chain geometry and its
progression remain unchanged.

The completed-chunk placement probe reports 273 unsupported PostProcessing
warnings and the synchronous searches report two tick-lag warnings. Ordinary
world generation uses ProtoChunks, and this explicit placement test does not
exercise that deferred-update path. There are no DUMMY block-entity warnings.
The standard first-boot missing `server.properties` message is unrelated to
the required tests. A graphical client, ordinary automatic structure-generation
smoke test, villager-AI traversal and full ReTerraForged co-installation were
not run for this revision. The player comparison should specifically judge
11,500-tick vanilla lighting, foliage shading, new roof intersections, cliff
exposure and shader-water readability.

The downloaded player jar passes ZIP integrity and Java 21 class checks. All
**158 shipped source resources match** the tested code; development tests,
fixtures and the flat-world override are excluded. The jar is **490,689 bytes**,
SHA-256 `61d54bfae8f4fd667870b48a965c041329b9b3dbb6ab84e1b1fcc1c6e483016b`.


## Alpha 11 golden planting and mountain ribs (October 15, 2026)

[GitHub Actions run 38692951262](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38692951262)
built commit `b1bdeeaa3cfa432165cca7ba3574a081a60dfa66` and passed **all 41
required GameTests in 1.451 minutes**. Generated resources, client-asset
validation, the Java 21 NeoForge package and optional compatibility guard
checks passed.

The 669-plan building matrix retains connected rooms, doors, stairs, galleries,
beds and window lanterns. All three complete synthetic towns retain 24 homes,
hall and watch lodge, with connected circulation. Expanded landscape checks
require mixed planting, saved-height fidelity, dry supported ground and clear
streets, porches, tree roots and bridge approaches. They caught planting on the
separately generated bridge's dry landings; reserving its full five-block
footprint fixes that conflict. Each town remains within the landscape save
budget, and exact building geometry takes priority over landscape details.

The added server test round-trips every ground-cover kind through NBT and
places it on real blocks. It checks valid grass support, water preservation,
rejection of unsupported plants and chunk clipping. Existing regression tests
now also hash alpha-10 house grammar 4 and tree form 3 against geometry captured
before this revision. Older saved plans retain their geometry. Flat-colour
renders of actual form-3 and form-4 blocks were inspected for the tapered crown
undersides; these are geometry inspections, not Minecraft screenshots.

The natural seed-0 survey again finds chunk **[-2335, -2316]** after 616
candidates. The new rib profile supports **15 homes, a hall and watch lodge**,
plus square, streets, bridge and landscape, for 21 saved pieces. Main floors
span **Y66–92** and the surrounding height survey spans **Y55–183**. Nine
buildings have close cliff backdrops. Eight form-4 birches remain, with height
parameters 16–19. The 4,856 saved landscape details include 2,904 ground-cover
positions: 892 yellow flowers, 443 white flowers, 1,020 short-grass patches,
231 wild-grain plants and 318 leaf-litter positions. These are planned
positions; final placement still requires suitable support and dry replaceable
space. The report is reproducible candidate evidence, not an average discovery
distance or guarantee of the same composition in other seeds.

The seed-0 chain candidate is chunk **[-278, -1033]**, at X−4439/Z−16523,
with surface top Y123 and chamber floor Y−48. Its geometry and progression
are unchanged. Terrain changes can alter which candidate is discovered first.

The complete town start survives NBT reload and reversed-chunk placement on
real generated terrain. The added furnishing checks verify the state and
block entity of every authored bed, barrel and other block-entity furnishing
after the entire town is placed. The explicit test writes into completed
chunks, unlike ordinary world generation. Its diagnostic identifies a
pre-existing bee nest at X−37424/Y69/Z−37062 replaced with air by the town's
clearance; a subsequent DUMMY block-entity warning names that same position.
No authored furnishing is lost. Unsupported deferred PostProcessing and
synchronous-search tick-lag warnings remain limitations of this test setup.
The CI artifact includes the full log and `run/logs/contour-town.json`.

No graphical client, ordinary automatic structure-generation smoke test,
villager-AI traversal or full ReTerraForged co-installation was run for this
revision. The player comparison should judge the more saturated vanilla
palette, bank planting, tree undersides, foundation transitions and narrow
mountain ribs. Fixed time remains 11,500 ticks and channel depth is unchanged.

The downloaded player jar passes ZIP integrity and Java 21 class checks. All
**158 shipped source resources match**, and GameTests, fixtures and the flat
test preset are excluded. The jar is **498,760 bytes**, SHA-256
`dbd57a9bc3f0527abd4e1057e94018ec4a93dab88eb6f5ade50d8113a648f937`.


## Alpha 12 warm-gold sprite treatment (October 15, 2026)

[GitHub Actions run 38699775953](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38699775953)
built commit `98dfe98f34932e22a8abc00a52c47fb986af8eab` and passed **all 41
required GameTests**. Generated assets and worldgen, client-resource validation,
the Java 21 NeoForge package and the independent compatibility guard checks
passed. This revision changes six biome colour pairs, the fallback foliage
tint and the client leaf-sprite loading path. Terrain, structures, tree geometry,
fixed time and water remain unchanged from alpha 11.

`./gradlew verifyClientSprites` is now part of `check` and therefore `build`.
It exercises the real client registration handler, atlas-source codec,
Minecraft sprite decoder and mip generator without opening a window or GL
context. Minecraft's actual 16-by-16 birch texture retains all 144 opaque
pixels and every transparent pixel; mean red-channel value rises from 130.6
to 157.2. Five mip levels are generated successfully. A separate coloured,
partially transparent pack fixture verifies 32-by-32 frames, both animation
frames, timing, interpolation metadata and mip generation. An explicit Elysium
texture override bypasses the tonal transform and retains its original pixels.
The probe also checks every alpha value, monotone tone order, exact transparent
padding, missing-source handling and invalid gamma rejection. The isolated
client-test source set is excluded from the player jar.

These checks cover sprite decoding and pixel integrity; they do not exercise
GPU atlas stitching, held/dropped item rendering or shader integration. No
graphical client was run for this revision. The existing opaque item-tint
handling remains unchanged. Player comparison should judge the selected B
direction with slightly reduced saturation, quieter grass and lifted leaf
midtones, using both vanilla lighting and the preferred shader/resource pack.
Snowlike ivory mountain faces remain intentional. Existing chunks receive
these changes after restarting; no dimension reset is needed.

The downloaded CI player jar passes ZIP integrity checks and contains **99
Java 21 classes**. All **158 shipped source resources match**; server/client
tests, fixtures and the flat test preset are excluded. The jar is **502,856
bytes**, SHA-256
`c880041ee3028d39ddec1f08cdb7c0fb976d20ac52d46096eb64e66364e9cf4b`.

## Alpha 13 approved L3/G4.1 sprite recipes (October 15, 2026)

[GitHub Actions run 38708226797](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38708226797)
built commit `ef2d90701e4b4294a34f2bd7c30a2c930af4239e`. The Java 21 build,
generated-content checks, client probes, independent compatibility guard and
**all 41 required dedicated-server GameTests passed**.

The real sprite loader now processes the actual vanilla birch texture and all
five grass textures. Each 256-value curve matches a SHA-256 reference exported
from the approved Python preview. Birch's mean source red rises from 130.6 to
193.3; grass-block top rises from 147.4 to 206.6 before its biome tint. Folding
G4.1's 0.95 gain into `#F2C846` stays within **one 8-bit channel value** of the
preview for every possible source value. Reference-biome and inventory tints
are checked against the selection. All six sources pass alpha/padding,
five-level mip generation, animated 32-by-32 pack frames, partial alpha,
explicit-artwork override, invalid-parameter and legacy-codec checks.

`GrassModelProbe` exercises the actual baking hook with Minecraft baked quads
and atlas sprites. It checks both snowy grass-block states, short grass, both
tall-grass halves, Elysium/other-dimension model-data transitions, item-model
isolation, preservation of pack model data, UV relocation, geometry, vertex
colours, light, normals, tint/shading flags and missing-target fallback. These
headless checks do not create a window, stitch/upload a GPU atlas or exercise
a third-party shader/renderer. The six-source loader also tests pack textures;
the player's complete texture pack has not been run here.

The downloaded CI artifact matches GitHub's ZIP digest. Its player jar passes
ZIP integrity checks, contains **100 Java 21 classes**, and matches all **158
shipped source resources**. Client/server probes and flat test fixtures are
excluded. The jar is **508,561 bytes**, SHA-256
`5fd768f58ee536512824c88fa36dab38510bcd9faca88bf379a71f00b8bc1e85`.

Replace the jar on client and server and restart. Existing chunks receive the
palette and sprite changes; no regeneration is required. The remaining visual
check is the same mountain-town view with vanilla lighting and the preferred
shader/resource pack, especially ground warmth, leaf brightness and the
contrast against the intentionally pale rock.

## Alpha 14 selected warmer-amber B (October 15, 2026)

[GitHub Actions run 38714086972](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38714086972)
built commit `cb226cc244fcf61c778226935c1434f9ff14dbac`. The Java 21 build,
generated-content checks, existing sprite/model probes, independent
compatibility guard and **all 41 required server GameTests passed**.

The reference biome and inventory foliage tint matches the selected V4 B
preview exactly: `#FFB947`, on the existing L3 curve. The six biome-resource
diffs contain only foliage-colour changes; their grass, sky, fog, water and
worldgen settings are unchanged. Regional foliage preserves red/blue and
applies the same `185/208` green-channel ratio. The atlas and dimension-type
resources are byte-identical to alpha 13. No new rendering code or graphical
client test was added for this palette-only revision.

The downloaded CI artifact matches its published ZIP digest. The player jar
passes ZIP integrity checks, contains **100 Java 21 classes**, and matches all
**158 shipped source resources**; test classes and fixtures are excluded.
It is **508,557 bytes**, SHA-256
`2b4c9b59103b0ce41659a08764940612ef355394a57c30ba78727cc5da3f48cb`.
Existing chunks receive the foliage update after replacing the jar on client
and server and restarting; no dimension reset is required.

## Alpha 15 dormant Forge chapter (October 15, 2026)

[GitHub Actions run 38730244394](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38730244394)
built commit `2c9819e78e396c94e778f5d0092e5eb75038f39b`. The Java 21 build,
generated assets/worldgen, existing client sprite/model probes, independent
compatibility guard and **all 47 required dedicated-server GameTests passed**.
The 41 existing tests remain enabled, alongside six new Forge tests.

New checks cover four terrain orientations and registered piece NBT loading;
player-sized paths through the descent, workshops, crucible, rear shrine and
both upper galleries; clipped placement and untouched covering rock; personal
multiplayer rewards; repeat readings and paid replacements; written-book,
instrument and discovery-data save/reload; dimension-scoped scholar offers;
and the real anchor interaction with Minecraft's connected test player. The
anchor test rejects carried chain blocks and high links, then verifies the
book, retained echo and awarded advancement at a registered buried anchor.

The natural terrain probe uses the folio's bounded placement list, including
the real exclusion rules. On seed **0**, candidate **146** yields a Forge at
chunk **[-221, -239]**, doorway **X−3524/Y101/Z−3804**, with orientation 1 and
foundry floor **Y74**. The complete structure start survives Minecraft NBT
loading and reversed-chunk placement on actual Elysium terrain. Its hearth,
ledger and excavated approach are present afterward. A completed chunk with
no saved Forge is rejected even when the new noise plan would fit there;
after the test explicitly installs the real start, the survey returns its
saved doorway. This is one reproducible candidate, not a multi-seed estimate
of discovery distance. The initial four-point entrance survey found no Forge
within this range; the final planner surveys a 32-block neighbourhood while
retaining mountain-cover constraints and vanilla's eight-chunk reference limit.

`run/logs/forge-sections.json` in the CI log artifact exports actual block-state
slices for layout review. The production models' vanilla texture references
were also checked against the actual Minecraft resource archive. The new
models require no copied vanilla PNGs. The six biome palettes, sprite curves,
terrain noise, fixed time, existing building grammar and chain-vault geometry
are unchanged from alpha 14.

These are headless checks. No graphical client, GPU/model appearance review,
full queued folio interaction in a normal survival world, multi-seed frequency
study or new ReTerraForged co-installation test was run. GameTest world options
disable automatic structures, so the terrain probe explicitly exercises real
structure generation, serialization, saved-start lookup and clipped placement.
The older natural-generation smoke test above does not validate the new Forge.
The existing explicit town-placement test still reports deferred
post-processing and a replaced pre-existing bee-nest block entity; all authored
furnishing assertions pass. A survey limits work to one candidate per server
tick, but individual structure-start generation can still cause a pause.

The downloaded artifact matches GitHub's ZIP digest. The player jar passes
ZIP integrity, contains **114 Java 21 classes**, and matches all **185 shipped
source resources**. Tests, probes and development world fixtures are excluded.
It is **563,937 bytes**, SHA-256
`6081243b514221188cfac44671bef0edf29d0fae69ca49ac54a15bad75f0349a`.

Install on both client and server and restart. New structures need fresh
chunks, while existing scholars and natural chain anchors support the new
items. Follow [the player guide](FORGE.md) for the discovery route, replacement
rules and inspection commands. The current chapter leaves the Forge dormant;
the full Tartarus region, boss and restart sequence remain future work.

## Alpha 16 buried ruin and service lighting (October 15, 2026)

[GitHub Actions run 38788338097](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38788338097)
built commit `d6ba15284a6731c6acc804ed2c3decdbe8445da5`. The Java 21 build,
generated assets and worldgen, client sprite/model probes, independent
compatibility guard and **all 51 required dedicated-server GameTests passed**.
The previous 47 tests remain enabled, with four new lighting/ruin tests.

The checks cover all four rotations, every authored lamp and handwheel, dark
initial states, the obstructed original doorway, matching gallery rockfall,
hammer/anvil alignment and the unfinished chain link. Player-sized routes
connect the winding approach, workrooms, surviving galleries, shrine and all
three controls. An early test caught an overhead duct crossing a stair; it now
passes above that route. Natural terrain placement, registered piece NBT and
chunk clipping continue to pass.

Lighting tests operate a real registered handwheel block with a server test
player; exercise circuit isolation, shared idempotence and per-Forge saved data;
save/load a fixture through Minecraft's registered block-entity NBT; and check
that a late-loaded lamp reconciles without the restoration loading its chunk.
Isolated test fixtures receive temporary ticking tickets because no connected
player supplies them. Production restoration grants no chunk tickets. Mining
a fixture does not reconstruct it, placed unbound handwheels remain unbound,
and the main hearth stays at light level 1. Pilots emit 3, dormant handwheels
2, restored handwheels 4 and restored work lights 12. There are 28 work lights
across three circuits, positioned around work surfaces and surviving passages.

Arrival guidance also checks vertical distance, so standing on the mountain
above the hearth does not count as reaching it. Surface palettes, tree and
settlement generation, terrain noise and the Tartarus vault remain unchanged.

For this pass a **real graphical Minecraft client also ran successfully** in
CI, using Xvfb and software OpenGL against a disposable local dedicated server.
The `forge-client-review` artifact contains eight 1280-by-720 screenshots:
entrance, dormant foundry, first circuit, west workshop, retorts, all service
circuits, gallery and collapse. These use default Minecraft textures, no shader
pack, brightness 50% and the unchanged Elysium time/ambient light. The dark and
restored foundry frames use the same camera. The actual captures were reviewed;
they are not generated concept images or approximations of the block geometry.

The visual server explicitly places the full chunk-clipped production piece
on the previously surveyed seed-zero mountain at X -3524 / Z -3804, entry
floor 100 and orientation 1. It disables automatic structures to avoid a second
overlapping placement. This exercises rendering and the real lighting network;
the separate natural terrain GameTest exercises structure selection and saved
starts. The first capture pass exposed overly clean iron rails, a raised
straight approach and weak illumination at the machine. The final pass uses
Weathered Forge Iron, a lower rubble approach and selective machine work lights.
The foundry remains deliberately dim after restoration, especially overhead.

To reproduce on Linux with Java 21, Xvfb and Mesa installed, run
`python3 tools/run_forge_visual_probe.py` with fresh `run-visual-server` and
`run-visual-client` directories. The opt-in harness binds its disposable server
to localhost, uses an offline development client, captures the scenes and stops
both processes. All harness classes are excluded from the player jar. This is
not a shader/resource-pack comparison or a manual survival playthrough. The
CI runner has no working audio device, so audible ambience was not assessed.
No new ReTerraForged co-installation or multi-seed frequency study was performed.

The downloaded player jar matches GitHub's artifact ZIP digest and passes ZIP
integrity checks. It contains **122 Java 21 classes** and **209 source-matching
resources**. GameTests, visual harnesses and development world fixtures are
excluded. Its size is **595,232 bytes**, SHA-256
`b4e0cd18dd83d4d9a961c6c165e057a232c07b64f33786c474e9a9693972fe4c`.

Use a fresh alpha world or regenerate Elysium. This pass does not maintain
alpha-15 Forge structure-save compatibility; it verifies the new design's own
save/load behavior. See [the player guide](FORGE.md) for the discovery and
lighting controls.

## Alpha 17 fuelled ember routes and deep descent (October 15, 2026)

Verified implementation: `29b8ae0c9436a4af717f44e30376ebde39b8c92a`.
[GitHub Actions run 38817522050](https://github.com/Eric-A-Stalee/MC-Elysium/actions/runs/38817522050)
passed the build, generated-resource checks, optional compatibility guard and
**all 53 required dedicated-server GameTests**. The downloadable jar is
`elysium-0.1.0-alpha.17.jar` (617,275 bytes), SHA-256
`d799d97360d05be5badded84e25c3cb91a490de76e04201f496d75a08a512afb`.
Its 126 shipped class files target Java 21; 234 packaged resources match the
committed files byte-for-byte, and development probes/GameTests are excluded.

The tests exercise fuel and flint-and-steel consumption through block
interactions, an empty/unignited/closed source, banking fuel, exhaustion,
progressive ignition, all three damaged branches, repairs, complete connectivity
after repairs, downstream extinguishing after mining, source removal, fuel
capacity, real Minecraft block-entity NBT, unbound fixtures and unloaded branch
isolation. Physical routes are surveyed through actual existing blocks, with
no chunk tickets. Fuel is stored only in the central brazier. A late-loaded
isolated terminal cannot recover light merely from a shared permanent flag.

Geometry checks cover the 72-block drop, substantial multi-stage route, hidden
entrance, physical network placement, four rotations, saved structure loading,
chunk clipping and the connected path through the gallery to the foundry and
its records. The seed-zero natural survey found a Forge in chunk `[123, 163]`,
entrance `(1983,124,2606)`, turn 2, foundry floor Y=52, after 59 candidates.
The independent visual fixture uses a real seed-zero mountain shoulder at
`(-3524,-3804)`, entrance floor Y=100 and foundry floor Y=28.

A real Minecraft client captured **eleven unmodified 1280×720 frames**:
entrance, hauling shaft, overlook, dark foundry, initial ignition, supplied
foundry, cleared west workshop, repaired east retorts, fully repaired network,
gallery and collapse. The client uses vanilla textures, gamma 0.5, no external
shader pack, Xvfb and Mesa software OpenGL. An initial ten-frame review led to
lower broken parapets at the first vista and a damaged shaft wall/talus. The
final captures were inspected again after those changes. This is actual game
rendering, not concept imagery or a synthetic block preview.

The final jar was built in CI: this workspace's offline NeoForm cache could
not create the Minecraft compilation artifact. The client probes use scripted
cameras and service interactions; they are not a manual survival playthrough,
a shader-pack comparison or an audio listening test. The existing surface
palette, fixed time, towns and Tartarus chamber design remain unchanged.
