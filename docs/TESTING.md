# Validation and first playtest

## Reproduce the automated checks

Use a Java 21 JDK, Minecraft 1.21.1 and the pinned NeoForge 21.1.233 development environment. Python 3.10+ is needed only for the resource checks.

```sh
python3 tools/generate_assets.py --check
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

## What still needs a client playtest

No graphical Minecraft client was available in the development environment. The mod is an early alpha, with server and data validation rather than a completed visual playthrough.

1. Install the jar on both client and server with NeoForge 21.1.233+, and create a new world. Use `/elysium visit` with operator permission for a quick inspection.
2. Inspect all three biomes across several seeds: tree shapes, foliage and item tinting, water and sky colors, mountain slopes, lighting, and frame/chunk-generation performance.
3. Locate `elysium:harvest_hamlet` and `elysium:elysian_bridge` inside Elysium. Check cottage doors, beds, villager pathfinding, farms, terrain fit, bridge rails and approaches. Hamlet templates are complete layouts, not a village road network connecting independently fitted houses; slopes need particular attention.
4. Follow [the survival entrance guide](ENTRANCE.md). Check item insertion, the sunset offering, particles, title, cooldown, two-way travel, and inventory on an actual connected player.
5. Test two players entering from different shrines, reconnecting, dying/respawning, server restarts, rebuilding a return stone, and `/elysium return` after losing it. Safe-position predicates and persistence code have been checked, but these are not automated end-to-end connected-player tests.
6. If experimenting with FreeTerraForged, test an explicitly enabled Overworld preset in a disposable world. Merely installing its jar is a narrower compatibility check.

Natural ore generation, underground caves, advanced hydrology/waterfalls, custom NPC behavior, custom shaders, and later story realms are outside this first paradise milestone. The independent terrain is a height field with river cuts, not an overhang-capable Uplift integration.
