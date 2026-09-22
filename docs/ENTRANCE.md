# The harvest sanctuary

The first prototype uses an open-air harvest shrine. Its four pale pillars frame the sky; there is no obsidian rectangle or portal plane. A sigil is placed in the central altar and awakened by offering wheat as the Overworld sun sets. The active stone is then used deliberately to travel.

## Obtaining a sigil

Harvest fully mature **vanilla wheat**. The default Elysium Shard chance is **0.01% per harvested crop**, independent of Fortune. Four shards make one Elysium Fragment and four fragments make one Sigil of Elysium. This means sixteen shards per sigil and an average of **160,000 mature wheat harvests** at the default rate. It is deliberately rare and highly variable; it is not a guaranteed drop after that many crops. The server configuration exposes `entrance.wheatShardChance` for a less demanding playthrough. Wild grain inside Elysium does not produce shards.

Craft the Harvest Altar using its recipe. Once placed, put the sigil into it with a normal right-click while holding the sigil in your main hand. Its position is the center of the sanctuary.

## Building and awakening the shrine

Use a flat, open area in the **Overworld**. Relative to the altar's block coordinates `(0, 0, 0)`, place:

| Part | Relative block positions | Required material |
|---|---|---|
| North pillar | `(0, 0..2, -3)` | Three vertically oriented stripped birch logs |
| South pillar | `(0, 0..2, 3)` | Three vertically oriented stripped birch logs |
| East pillar | `(3, 0..2, 0)` | Three vertically oriented stripped birch logs |
| West pillar | `(-3, 0..2, 0)` | Three vertically oriented stripped birch logs |
| Harvest offerings | `(-2, 0, -2)`, `(-2, 0, 2)`, `(2, 0, -2)`, `(2, 0, 2)` | One hay bale at each position |

The bottom logs and hay bales are at the **same height as the altar**, rather than the block supporting it. Leave the sky above the altar unobstructed; glass and leaves above the altar count as a roof too. Paths, flowers, lanterns and additional decoration are optional and do not change the test.

With a sigil inserted, right-click the altar with **one wheat in your main hand** between Overworld day ticks **11,000 and 13,000 inclusive**. For a creative test, `/time set 12000` puts the sky in this window. Successful activation consumes the wheat, keeps the sigil in the altar, lights the altar and produces a brief sound and golden-white particles. An unsuccessful attempt explains the first missing requirement and consumes nothing.

Empty your main hand and right-click the awakened altar to enter Elysium. It can be used at any time of day after activation, provided its pillars, hay bales and open sky remain intact. Breaking the pattern deactivates the altar on its next use; its sigil remains installed and it can be awakened again with wheat at sunset. There is no idle world scan or ticking block entity.

The portal takes players only. Dismount first; animals and vehicles do not travel with you. A three-second server-side travel cooldown prevents accidental immediate return. Other players may use the same activated altar without supplying another sigil.

## Arrival and return

The first visitor establishes an already active return altar near Elysium's origin. The mod first looks for an open, naturally flat clearing with safe ground on all four sides, and places only the altar in an empty cell. If that bounded search finds no suitable clearing, it creates a small 5×5 sandstone plinth in empty space above the local surface. Its position is stored with the dimension and shared by all players. Neither path flattens terrain or clears an existing structure or flower. The fallback can put its plinth above a tree canopy on particularly wooded seeds; fuller terrain integration is a later refinement.

Right-click its active altar with an empty main hand to return. Each player retains their own source dimension, feet position and view direction, even when several people arrive through different shrines. If the original standing position is still safe, the player returns exactly there. Otherwise the mod searches nearby for supported, unobstructed ground. If the entrance was completely obstructed or the dimension removed, it tries the player's Overworld respawn area, then nearby surface ground, then a minimal unobstructed sandstone landing. It never clears blocks to force a return. In the exceptional case where the bounded search cannot create a safe landing, travel is refused and the player stays where they are.

Arrival state and individual return points survive world saves, reconnects and player cloning on death. Repeated visits reuse the same arrival. If the return altar is removed, it is restored only when that exact cell is air over a safe floor. If the site has been built over or made unsafe, a nearby natural clearing is tried first, followed by a new **3×3** landing in empty space above the local surface. The replacement becomes the saved arrival. The old site and player builds are left intact.

The generated return altar has `active=true,has_sigil=false`. It contains no sigil to recover. A player-made entrance instead keeps `has_sigil=true`, and breaking it drops the installed sigil through the block's conditional loot table. In the Overworld, the altar item places unactivated and empty. Inside Elysium, any placed Harvest Altar immediately becomes an active **return** stone without a sigil. This lets a visitor pick up the arrival altar and place it somewhere more convenient without losing the route home. It provides no shortcut into Elysium.

If a return stone is lost, `/elysium return` is available to every player while inside Elysium and uses the same stored return point and safety checks as the altar. Administrative `/elysium visit` requires operator permission.

## Implementation contracts

`HarvestAltarBlock` has two boolean blockstate properties, `has_sigil` and `active`, and takes ordinary `BlockBehaviour.Properties`. Its model uses a 16×16 base from Y 0 to 3, a centered 10×10 stem from Y 3 to 11, and a 16×16 top from Y 11 to 14. The registry should set `noOcclusion()` and light level 12 while active. The loot table should drop the altar normally and **exactly one** Elysium Sigil only when `has_sigil=true`.

`ElysiumTravel.DIMENSION` is the `elysium:elysium` dimension key. `enter(ServerPlayer, BlockPos)` and `returnHome(ServerPlayer)` are server-side travel entry points; `enter` expects its caller to validate the shrine and enforces an Overworld source. `AltarPattern.validateStructure`, `validateActivation`, `isSunset` and `ElysiumTravel.isSafeStandingPosition` expose focused integration-test seams. `ElysiumTravel` automatically subscribes its player-clone handler on the game bus. There are no client class references in the common travel code.

The pinned 1.21.1 `ServerPlayer.teleportTo` overload reports success even when NeoForge cancels its inner `changeDimension` call. The travel wrapper therefore verifies the player's actual destination level before writing a return point or cooldown and sending the arrival title. This guard is checked against the pinned Minecraft/NeoForge source; a real connected client's canceled crossing remains a manual integration check.

Required English language entries:

```json
{
  "message.elysium.altar.overworld_only": "The harvest sanctuary must be awakened beneath the Overworld sky.",
  "message.elysium.altar.pillars": "Each of the four pillars must be three upright stripped birch logs tall.",
  "message.elysium.altar.hay": "Place four hay bales at the diagonal offerings, two blocks out in each direction.",
  "message.elysium.altar.sky": "The altar must see the open sky.",
  "message.elysium.altar.sunset": "Offer wheat when the Overworld sun is setting.",
  "message.elysium.altar.sigil_present": "A sigil is already resting in the altar.",
  "message.elysium.altar.sigil_inserted": "The sigil settles into the stone. Complete the shrine, then offer wheat at sunset.",
  "message.elysium.altar.needs_sigil": "The hollow in the altar awaits a Sigil of Elysium.",
  "message.elysium.altar.offer_wheat": "The sigil waits for an offering of wheat at sunset.",
  "message.elysium.altar.awakened": "The sanctuary awakens. Touch the altar with an empty hand to enter.",
  "message.elysium.travel.unavailable": "Elysium is unavailable. Check that its dimension data is installed.",
  "message.elysium.travel.no_landing": "No safe landing could be found. The sanctuary leaves you here.",
  "message.elysium.travel.dismount": "Dismount and leave passengers behind before using the sanctuary.",
  "message.elysium.travel.cooldown": "Let the last crossing settle for a moment.",
  "message.elysium.travel.prevented": "The crossing was prevented.",
  "message.elysium.travel.fallback": "Your old path was obstructed. The sanctuary returned you near your Overworld spawn.",
  "title.elysium.arrival": "Elysium",
  "title.elysium.arrival.subtitle": "The fields remember only golden hours."
}
```
