# Elysium pack compatibility

Optional pack adapters for **Minecraft 1.21.1 / NeoForge 21.1.238 / Java 21**. Elysium does not need these for standalone use. This is a separate Gradle build: the repository's normal Elysium build and jar do not include either adapter.

## Modules and scope

| Module | Output | Purpose |
| --- | --- | --- |
| `elysiumcompat` | `elysium-pack-compat-1.0.0.jar` | Keeps `elysium:elysium` independent of the tested ReTerraForged hooks and preserves its declared height with the tested Dynamic Height build. |
| `guard` | `rtf-erosion-dimension-guard-1.0.0-test.jar` | Allows ReTerraForged erosion in the Overworld by default; other dimensions require explicit opt-in. This is a required runtime dependency of `elysiumcompat`, and is also usable separately with the tested RTF build. |

The Elysium adapter:

- Clears the foreign RTF generator context/preset for Elysium and prevents later reinitialization of that protected state.
- Excludes Elysium biome keys from RTF's extra-feature insertion, retaining Elysium's own feature ordering.
- Skips RTF's snow-decoration feature in Elysium.
- Preserves Elysium's declared `minY=-64`, height `384` on the server and in the client height-range handler.

It does **not** make RTF generate Elysium, replace Elysium's terrain, change its gameplay values, or repair already-generated terrain. Dimension-specific checks leave other dimensions on their existing paths; the separate erosion guard deliberately has a broader Overworld-only default.

## Elysium version policy

The adapter has its own stable `1.0.0` version. Elysium is required, but its version is not pinned or capped (`[0,)`). Routine Elysium releases do not require rebuilding or renumbering this adapter. This intentionally replaces the earlier alpha-specific metadata releases; its implementation is unchanged.

Record tested Elysium versions here rather than enforcing a release-by-release gate. A substantial release such as Elysium 1.1 may warrant inspection if it changes dimension identity, biome namespaces, generator integration or height handling, but it is not automatically blocked. Change the adapter version only for actual adapter changes, not because Elysium released another build. Accepting a version is not a claim that every future release has been tested.

## Tested inputs and external constraints

This source publishes the existing pack adapters, not a general compatibility promise for every RTF or Dynamic Height release. The addon uses internal fields/methods, and its required mixins should fail visibly if those targets change.

| Component | Tested identity |
| --- | --- |
| Elysium | Latest pack-tested release: `0.1.0-alpha.7`; required, with no release-specific pin or upper bound |
| NeoForge | Tested on `21.1.238`; metadata allows `[21.1.238,21.2)` |
| ReTerraForged | `reterraforged-0.0.6003R2-neoforge-1.21.1.jar`; embedded mod version `0.0.6` |
| Dynamic Height | `dynamicheight-0.3.5+neoforge-1.21.1-biomefix-local.4.jar`, a separately maintained private patched build |

The build verifies these SHA-256 values before compiling `elysiumcompat`:

```text
RTF:            5e15c2a86136f41e8f6d5f7fe4f2abcecc6bcb5f6972fe90e68aa19ae795baae
Dynamic Height: 98706dd23a385d9df5d4a95717caf66c93ecc458f2acffcabad5d09b27913998
```

**Neither external jar is included or downloaded by this build.** Supply authorized copies locally. In particular, stock Dynamic Height is not a substitute for the named private build. Publishing these adapters does not publish that private patch or grant redistribution rights to either dependency. Someone without the exact inputs can still inspect the adapter source and build/test `guard`, but cannot reproduce the complete addon build from this repository alone.

## Build

From the repository root, using a Java 21 JDK:

```sh
# No external mod jars required for the erosion policy's unit tests or guard build.
./gradlew -p compat :guard:test :guard:build

# Build both adapters using the exact external jars above.
./gradlew -p compat build \
  -PrtfJar=/absolute/path/to/reterraforged.jar \
  -PdynamicHeightJar=/absolute/path/to/dynamicheight.jar
```

On Windows, use `gradlew.bat` and quote paths containing spaces. Relative dependency paths resolve from `compat/`, not the module directory. Alternatively, place the exact binaries at `compat/libs/reterraforged.jar` and `compat/libs/dynamicheight.jar`; that directory is ignored by Git.

Outputs are under `compat/guard/build/libs/` and `compat/elysiumcompat/build/libs/`. The external jars are `compileOnly`: they are not embedded in either output. Use `--rerun-tasks` when you need a fresh execution of the policy tests rather than an up-to-date result.

Both adapter jars include this directory's MIT notice at `META-INF/LICENSE`.

Do not add the compatibility build to Elysium's root `settings.gradle`. Keeping it separate lets Elysium build and run without these external inputs. Routine Elysium updates leave this adapter untouched. RTF and Dynamic Height still have exact external-build constraints because the adapter hooks their internal implementation; inspect those targets and repeat relevant integration checks before changing their constraints or accepted hashes.

## Install and configure

For the full supported stack, install both adapter jars alongside the exact external mods and Elysium on the client and server. Do not replace or modify the original dependency jars. If the erosion guard is already installed, keep one copy rather than adding a duplicate.

The guard registers a SERVER config named `rtf-erosion-dimensions.toml`:

```toml
additional_dimensions = []
```

This always permits `minecraft:overworld`. Add explicit namespaced dimension IDs only when you intentionally want RTF erosion in those dimensions. Leave Elysium out of the allowlist. The policy is read on config loading and reset on unloading; changes require a restart, not a live config reload. In the tested NeoForge dedicated-server setup the file was loaded from the server's `config/` directory; verify the actual loader-created location for your instance instead of assuming `world/serverconfig/`.

For diagnostic runs, `-Drtferosionguard.audit=true` prints per-dimension allowed/blocked call totals on server stop. It is off by default.

Back up saves before changing the stack. These jars do not delete dimension files, rewrite `level.dat`, migrate saved biome sources, or reset progression. Already-generated terrain is not repaired. A terrain reset or world metadata migration is a separate operation, never an installation side effect.

## Validation boundaries

The included `guard` unit suite exercises the actual policy implementation: Overworld allowance, dimension opt-in, invalid IDs and immutable snapshots. The addon module currently has no standalone unit or GameTest suite; an empty Gradle `test` task is not integration evidence.

The repository's `compat-guard` CI job builds the guard, runs that suite, checks the packaged license, and verifies rejection of missing and wrong RTF inputs. It runs separately from Elysium's normal build and 31 server GameTests. It does not build or launch the full adapter with the private dependencies. SHA-256 checks apply to compile inputs; runtime metadata checks mod versions and mixin targets, not the hashes of installed jars. Use the exact documented binaries even when another jar advertises the same mod version.

Before this source publication, controlled dedicated-server runs against the older RTF build established the erosion guard's dimension opt-in behavior, terrain preservation across reload, and exact Overworld-only block/biome parity in the bounded comparison. Separate Elysium pack runs verified the protected RTF context, declared height, valid nonduplicated chunk-section coordinates, natural hamlet/bridge generation and save/reload. The alpha.5 pack run completed 134 generation requests and retained block/biome hashes for all 133 unique chunks after reload. These integration harnesses and private pack/world data are not included here.

For alpha.6, the adapter's implementation stayed byte-identical apart from version/dependency metadata. Elysium's 31 standalone upstream GameTests were executed successfully, but the full-pack generation campaign was **not** repeated for alpha.6. Client height-hook targets were inspected, not validated in a graphical or connected-player playtest. Existing unrelated pack errors were not resolved by these adapters. None of these results proves universal seed, dimension or mod compatibility.

## License

The adapter module metadata already declares **MIT**. [LICENSE](LICENSE) applies only to the authored compatibility modules and build support in this directory. Elysium's original code/content outside `compat/` retains the repository's separate licensing terms. Minecraft, RTF, Dynamic Height and other dependencies retain their own licenses and are not redistributed here.
