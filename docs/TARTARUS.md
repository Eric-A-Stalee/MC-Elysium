# Tartarus expedition — alpha 18 design and player notes

This chapter extends the optional Forge expedition: restore a keeper's lamp,
follow a chain into its anchor vault, descend through an abandoned inspection
route, repair a crossing over the burnt basin, confront the original one-eyed
chimera, and return the First Flame to the Forge.

The creature is the original tar-and-slag chimera: one furnace-like eye on an
elongated neck, a low dragging body and hooked stinger tail. There is no
mechanical keeper. The keepers named in the records and lamp are the vanished
people who once tended the works. The sealed chamber has no maintained lights;
the wider basin has only sparse buried embers. Charcoal, burnt umber, clinker
and sparse ash replace the pale continuous ground of the early concept.

Elysium extends down to Y=-256, retaining its existing upper boundary and
surface height formula. Each newly generated chain includes a saved basin and
encounter plan, clipped to the current generating chunk. The anchor vault stays
at Y=-48. The first Tartarus regions are bounded installations in the geology,
not a dimension-wide cave generator. No ReTerraForged dependency is introduced.

The main Forge restart, moving sun and later crisis are a subsequent chapter.
This expedition ends with the First Flame returned to its socket; ordinary
exploration, the keeper's lamp and the service brazier never advance the sun.

## Play the expedition

1. Find the Forge using a Surveyor's Folio. Restore the service brazier and clear
   the ash blocking the west workshop's channels. At the **Keeper's Workbench**,
   hold a vanilla lantern with one copper ingot and one amethyst shard in your
   inventory. The bench consumes one of each and supplies a **Keeper's Lamp**.
   It emits light level 13 **when placed** and can be mined and carried onward.
   It does not supply dynamic light while held; no shader or lighting mod is required.
2. Take the Chain Resonator from the cold hearth. Follow its bearing to a chain,
   excavate along the links to the Y=-48 anchor, and listen to the bottom link.
   Its book points to the inspection way through the vault's southern wall.
3. Follow the switchbacks into the burnt basin. There are exposed drops; bring
   light, equipment and food. Hold **four vanilla chains** against the seized
   counterweight to raise the 17-block missing span. It refuses an obstructed
   deck and consumes nothing on failure. Building your own crossing is also possible.
4. Continue through the rock to **The Last Binding**, at the entrance to the
   separate, dark chamber. Use the Chain Resonator on this socket to awaken
   **The Cinderbound Eye**. It is the original tarry, one-eyed chimera, with a
   dragging body, long neck and hooked tail—not a humanoid keeper.
5. The creature warns before a forward gaze, rear tail sweep or outer ground
   pulse. Solid cover stops attacks. After each attack its eye lowers and becomes
   vulnerable for three seconds; the slag body resists damage. It has 360 health.
   It does not destroy terrain, spread fire or attack creative/spectator players.
6. Defeating the creature releases a **First Flame** for nearby players. Others
   can collect their first copy at the binding. A saved claim prevents repeat
   collection; if a flame is lost, offer one Elysium Fragment there to recover it.
7. Sneak-use the resonator for the remembered Forge bearing. Place the flame in
   the **original Forge hearth**. The socket begins to glow, but the great hammer
   remains still and the afternoon stays at 11,500 ticks.

The encounter and repair record persist across reloads. A chamber does not
respawn its defeated creature. Reconciliation of a missing entity waits for its
home chunks to be loaded; normal exploration never force-loads that arena.

## Generation and implementation

`TartarusDepthsPlan` owns the basin seed, route, crossing and chamber coordinates.
`TartarusDepthsPiece` evaluates those coordinates independently of neighbouring
chunks and clips every write. The saved chain structure contains both the old
anchor-vault piece and the new depths piece. Chain placement is rarer (48-chunk
spacing / 24-chunk separation), leaving room between the underground regions.
A buried lower run continues from the anchor and emerges above the basin,
so the giant iron can still be followed through the rock. The surface density
function is unchanged; the new lower volume remains solid
except where these pieces excavate their bounded spaces.

`DepthsControlBlockEntity` owns repair status, the single creature UUID, victory
and player claims. `SlagChimeraEntity` owns combat, health and the real multipart
eye target. Client-only rendering builds its anatomy from authored voxel geometry
using active-pack vanilla material references. Only the eye and stinger use
emissive rendering; the creature's body receives ordinary world light. A small,
expiring light source follows the eye without overwriting solid player blocks.

Use `/locate structure elysium:tartarus_chain` for a natural start. For inspection,
`/give @s elysium:keepers_lamp`, `/give @s elysium:chain_resonator` and
`/summon elysium:slag_chimera` are available. A manually summoned creature has no
binding and does not award the expedition flame. A creative resonator has no
remembered Forge until you obtain one from a hearth.

This update changes dimension height and generation. Start a new test world or
regenerate Elysium fully. No retrogen or old-world conversion is provided.

