# Validation and first playtest

## Reproduce the automated checks

Use a Java 21 JDK, Minecraft 1.21.1 and the pinned NeoForge 21.1.233 development environment. Python 3.10+ is needed only for the resource checks.

```sh
python3 tools/generate_assets.py --check
python3 tools/generate_ambience.py --check
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

## What still needs a client playtest

No graphical Minecraft client was available in the development environment. The mod is an early alpha, with server and data validation rather than a completed visual playthrough.

1. Install the jar on both client and server with NeoForge 21.1.233+, and create a new world. Use `/elysium visit` with operator permission for a quick inspection.
2. Inspect all three biomes across several seeds: tree shapes, foliage and item tinting, water and sky colors, mountain slopes, lighting, and frame/chunk-generation performance.
3. Locate `elysium:harvest_hamlet`, `elysium:elysian_bridge`, `elysium:elder_spring` and `elysium:sunlit_lookout` inside Elysium. Check cottage doors, beds, villager pathfinding, farms, terrain fit, bridge rails, bank landings, spring containment and the lookout’s orientation. Hamlet templates are complete layouts, not a village road network connecting independently fitted houses; slopes need particular attention.
4. Follow [the survival entrance guide](ENTRANCE.md). Check item insertion, the sunset offering, particles, title, cooldown, two-way travel, and inventory on an actual connected player.
5. Test two players entering from different shrines, reconnecting, dying/respawning, server restarts, rebuilding a return stone, and `/elysium return` after losing it. Safe-position predicates and persistence code have been checked, but these are not automated end-to-end connected-player tests.
6. If experimenting with FreeTerraForged, test an explicitly enabled Overworld preset in a disposable world. Merely installing its jar is a narrower compatibility check.

Natural ore generation, underground caves, advanced hydrology/waterfalls, custom NPC behavior, custom shaders, and later story realms are outside this first paradise milestone. The independent terrain is a height field with river cuts, not an overhang-capable Uplift integration.
