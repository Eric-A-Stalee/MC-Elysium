# ReTerraForged / FreeTerraForged investigation

**Status: investigated; not an Elysium terrain dependency.** The initial Elysium
dimension uses its own generator settings. Installing FreeTerraForged does not
give Elysium its Uplift terrain. A real adapter is required; this repository does
not include a datapack that pretends otherwise.

The fork matching the requested Uplift continents, waterfalls, elevated rivers,
and customizable lakes is [ETcodehome/FreeTerraForged][repository], formerly
ETcodehome/ReTerraForged. Its [1.21.1 branch][branch] descends from
racoonman2/ReTerraForged and equalizer32/NeoTerraForged. The reviewed release is
[FreeTerraForged 1.0.0][release], published September 18, 2026, at commit
`dfa43688a6205bbb88509db6de867b0a63f580c5`. The June [3D Everything release][uplift]
documents Uplift, highland lakes, and the river-valley changes; the September
release adds directional water flow, mountain variation, and further fixes.

| Verified property | Finding |
| --- | --- |
| Minecraft / loader | Source targets Minecraft 1.21.1, NeoForge 21.1.219; Fabric is a separate project. |
| Java / build | Java 21; Architectury plugin 3.4.164 and Loom 1.11.445. Source build task: `./gradlew :neoforge:build`. |
| Source dependencies | Architectury API 13.0.8; common project references TerraBlender 4.1.0.0. Biolith 3.0.14 and Lithostitched 1.7.13 are compile-only integrations. These declarations do not by themselves establish every runtime requirement. |
| Release artifact | `freeterraforged-1.0.0-neoforge-1.21.1.jar`, downloaded and inspected. |
| Verified SHA-256 | `bef259b9a9947959c9c87f3628aaca992698e858e11e57766f5bb799c3b29b69` |
| License | [MIT][license], copyright 2023 ReTerraForged. Copying substantial source requires retaining its notice. No source or assets are vendored here. |
| Verification boundary | Source review, release-archive inspection, and a co-installation server smoke test completed: all 12 Elysium GameTests passed with this jar loaded. An active FTF Overworld preset, an FTF source build, and Elysium generation with its engine have not been tested. |

The build facts come from [gradle.properties][properties], the [root build][build],
the [common build][common-build], and the [NeoForge build][neo-build]. The released
jar has the same [mod metadata][metadata] as the pinned source, no nested jars,
and only Architectury transformation-annotation references in its class files.
Its optional compatibility declarations and Gradle compile dependencies should
not be converted blindly into new required Elysium dependencies.

## Why a dimension datapack is insufficient

The engine uses Minecraft's `NoiseBasedChunkGenerator` with custom density
functions and extensive mixins. There is no separate, registered FreeTerraForged
chunk-generator codec to substitute in Elysium's dimension JSON. The registered
[density-function codecs][density-codecs] include `freeterraforged:noise` and
`freeterraforged:cell`; recognizing those JSON types is only the first step.

1. [MixinChunkMap][chunk-map] explicitly sets its initialization context from
   `serverLevel.dimension() == Level.OVERWORLD`.
2. [MixinRandomState][random-state] replaces cell markers with zero outside that
   context. It creates the terrain context only for the accepted Overworld
   router. Referencing the exported height function from `elysium:elysium` would
   therefore not activate terrain generation.
3. The same class loads one global `Preset.KEY`. The [preset builder][preset]
   patches a shared registry, while [PresetNoiseGeneratorSettings][noise-settings]
   and [PresetDimensionTypes][dimension-type] specifically replace
   `minecraft:overworld`. An ordinary exported preset is an Overworld conversion,
   not a per-dimension configuration.
4. [FlowSettings][flow] stores the active flow options statically. Multiple
   independently configured dimensions would need this state scoped properly.
   [CellSampler][cell-sampler] also has a fallback cache keyed by position and
   equality based on the sampled field; context identity needs review before
   reusing that path in more than one engine instance. Its newer
   [PointCellCache][point-cache] does explicitly rebind to a changed WorldLookup.

The upstream dimensional guard prevents its Overworld machinery leaking into
unrelated dimensions. Simply removing that guard would not solve the global
preset, registry, cache, and flow-state issues.

Co-installation is a different question. Elysium owns its biome source and has
no reason to join an Overworld biome-injection system. Its climate density
functions should also be owned under `elysium:*`, using vanilla raw noise
parameters instead of referencing mutable `minecraft:overworld/*` density
functions or `minecraft:shift_x` / `minecraft:shift_z`. An FTF preset replaces
some of those shared density resources. Its [preset noise bootstrap][noise-data]
uses FTF's separate noise registry; the reviewed builder does not patch
Minecraft's ordinary `Registries.NOISE` parameter registry.

The global climate mixin can adjust the depth axis. Elysium's three initial
biomes all accept the same depth interval, and the reviewed
[underground-band classifier][banding] does not classify that interval as a cave
biome. That removes an obvious selection conflict, but does not substitute for
a co-installation test with an actually enabled FTF Overworld preset.

## Concrete integration path

Prefer an optional, version-pinned adapter with an explicit saved generator
choice. Keep the standalone generator as the default. A world that was created
with the adapter must report a missing engine clearly rather than silently
changing generator and making terrain seams. Existing saves must not switch
their generator automatically when an optional jar appears.

The clean upstream path is to make the terrain context and preset explicit
per-dimension inputs, support registries under a caller-selected namespace, and
scope flow settings to that context. Then an Elysium adapter can supply its own
noise settings, biome parameters, surface rules, features, and dimension type
while leaving the selected Overworld generator intact.

A more independent path is an Elysium-owned chunk generator that calls the
public-but-internal [GeneratorContext][generator-context] terrain engine.
`makeCached` accepts a preset, noise lookup, seed, and tile settings;
[WorldLookup][world-lookup] can populate terrain cells. This avoids claiming
Elysium is the Overworld, but is a substantial integration: the adapter must
provide its own codec, seeded lifecycle, filtered tile-cache lifecycle, biome
sampling, block and fluid filling, height and column queries, and surface rules.
It would couple to internal APIs, so it needs a pinned release and focused
compatibility tests. Sampling unfiltered point heights while filling from
filtered tiles would produce mismatched terrain and structure placements.

Hydrology is part of that work. A cell contains terrain height, river zone,
water-table information, and flow information. [RiverGasketFeature][river-gasket]
uses `ContinentalHydrology.getComplexWaterHeight` and `Levels.scale` to derive
the local water height and support elevated water. A raw height-only adapter
would omit the very rivers and waterfalls motivating the integration. Structure
placement and bridge clearance must query the same final columns, including
their local water surface; a fixed Y=63 assumption is insufficient.
The river feature runs after structure starts, so ordinary early base-column
queries may not expose the eventual elevated water. The adapter needs a shared
planned-water-height query for structures as well as its later fluid placement.

The [preset codec][preset] and its component codecs do expose the requested
controls. These are verified field paths for future implementation, not a
ready-to-install Elysium preset:

| Purpose | Verified fields |
| --- | --- |
| Elevated continent hydrology | `world.continent.continentType` accepts uppercase `UPLIFT`; continent scale and control points remain important. |
| Main and branch rivers | `rivers.riverCount`, `rivers.mainRivers`, `rivers.branchRivers`; each river has `bedDepth`, `minBankHeight`, `maxBankHeight`, `bankWidth`, `bedWidth`, and `fade`. |
| Lakes / wetlands | `rivers.lakes` includes chance, start-distance bounds, depth, size bounds, and bank heights; `rivers.wetlands` has chance and size bounds. |
| Terrain shape | `terrain.general` has region size, horizontal and vertical scale, mountain options; terrain families have weight and scale controls. |
| Underground restraint | `caves` distinguishes entrance, cheese, spaghetti, noodle, ordinary/deep carver, and ravine probabilities, plus large ore veins. |
| Water behavior | `flow.flowParticles`, `flow.boatFlowDynamics`, `flow.navigableWaterfalls`. |

See [WorldSettings][world-settings], [ContinentType][continent-type],
[RiverSettings][rivers], [TerrainSettings][terrain], and [CaveSettings][caves].
Preset export runs the registry bootstrap through codecs; an isolated preset
JSON is not the complete generated datapack. Do not hand-author a purported
compatibility pack with guessed resources or copy its Overworld export into the
main mod.

Before enabling an adapter, verify standalone startup without FTF; startup and
dimension travel with FTF both installed and actively generating the Overworld;
unchanged Elysium output in co-install mode; generation with the adapter enabled;
the fixed Elysian sky and cave-free stone contract; elevated river banks,
waterfalls, hamlets, and bridges; reloads and two worlds with different seeds in
one JVM; parallel chunk generation in both dimensions; and a clear failure when
an adapter-created save loses its engine dependency. Actual generated terrain
must be inspected in-game before claiming visual parity with the references.

[repository]: https://github.com/ETcodehome/FreeTerraForged
[branch]: https://github.com/ETcodehome/FreeTerraForged/tree/1.21.1
[release]: https://github.com/ETcodehome/FreeTerraForged/releases/tag/1.21.1_v1.0.0
[uplift]: https://github.com/ETcodehome/FreeTerraForged/releases/tag/1.21.1_v0.0.6003R2
[license]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/LICENSE
[properties]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/gradle.properties
[build]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/build.gradle
[common-build]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/build.gradle
[neo-build]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/neoforge/build.gradle
[metadata]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/neoforge/src/main/resources/META-INF/neoforge.mods.toml
[density-codecs]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/densityfunction/FTFDensityFunctions.java
[chunk-map]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/mixin/MixinChunkMap.java
[random-state]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/mixin/MixinRandomState.java
[preset]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/Preset.java
[noise-settings]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/PresetNoiseGeneratorSettings.java
[dimension-type]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/PresetDimensionTypes.java
[flow]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/FlowSettings.java
[cell-sampler]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/densityfunction/CellSampler.java
[point-cache]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/densityfunction/PointCellCache.java
[noise-data]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/PresetNoiseData.java
[banding]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/biome/UndergroundBiomeBanding.java
[generator-context]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/GeneratorContext.java
[world-lookup]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/cell/heightmap/WorldLookup.java
[river-gasket]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/world/worldgen/feature/RiverGasketFeature.java
[world-settings]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/WorldSettings.java
[continent-type]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/ContinentType.java
[rivers]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/RiverSettings.java
[terrain]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/TerrainSettings.java
[caves]: https://github.com/ETcodehome/FreeTerraForged/blob/dfa43688a6205bbb88509db6de867b0a63f580c5/common/src/main/java/etcodehome/freeterraforged/data/worldgen/preset/settings/CaveSettings.java
