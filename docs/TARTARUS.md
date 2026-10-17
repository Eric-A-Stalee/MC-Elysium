# Tartarus expedition — design and player notes

Alpha 20 reshapes the creature with unequal dragging limbs, a bowed neck, an
irregular amber eye and an articulated hooked tail. Cinder volleys now travel
as visible entities and collide with cover; the tail and ground pulse have
spatially timed damage. Broader stone strata and localized collapse soften
the basin's repetitive material pattern without changing its route or size.

Alpha 19 corrected the alpha 18 generation cutoff: the earlier footprint extended
beyond the chunks that Minecraft discovers from one structure start. The
descent now folds through the rock, and the saved start lies nearer the centre
of the complete installation. Basin size, chamber size and depth are retained.
Previously generated partial structures need regeneration.

This chapter extends the optional Forge expedition: restore a keeper's lamp,
follow a chain into its anchor vault, descend through an abandoned inspection
route, repair a crossing over the burnt basin, confront the original one-eyed
chimera, and return the First Flame to the Forge.

The creature is the original tar-and-slag chimera: one furnace-like eye on an
elongated neck, a low dragging body and hooked stinger tail. There is no
mechanical keeper. The keepers named in the records and lamp are the vanished
people who once tended the works. The sealed chamber has no maintained lights;
the wider basin has only sparse buried embers. Charcoal, burnt umber, clinker
and sparse ash replace the pale continuous ground of the early concept. Below
Y=-64, distant air gradually fades to warm darkness instead of pale sky fog;
water and lava retain their own fog.

Elysium extends down to Y=-256, retaining its existing upper boundary and
surface height formula. Each newly generated chain includes a saved basin and
encounter plan, clipped to the current generating chunk. The anchor vault stays
at Y=-48. The first Tartarus regions are bounded installations in the geology,
not a dimension-wide cave generator. No ReTerraForged dependency is introduced.

The [first-dawn chapter](FIRST_DAWN.md) continues at the great hammer after
the First Flame returns.
Returning the First Flame now restores full heat to the connected, burning
Forge network. Coal, ignition and airflow are still required. Ordinary
exploration, the keeper's lamp and the service brazier never advance the sun.

## Play the expedition

1. Find the Forge using a Surveyor's Folio. Restore the service brazier and clear
   the ash blocking the west workshop's channels. At the **Keeper's Workbench**,
   hold a vanilla lantern with one copper ingot and one amethyst shard in your
   inventory. The bench consumes one of each and supplies a **Keeper's Lamp**.
   It emits a small light (level 8) when held below Y=0 in Elysium, including in
   the off hand, and a stronger light (level 13) when placed. Mine it to carry
   it onward. Both use ordinary Minecraft lighting; no shader or lighting mod
   is required. The held light updates twice per second and expires after use.
2. Take the Chain Resonator from the cold hearth. Follow its bearing to a chain,
   excavate along the links to the Y=-48 anchor, and listen to the bottom link.
   Its book points to the inspection way through the vault's southern wall.
   At floor Y=-48, look roughly ten blocks east and thirty blocks south of the
   chain centre. The brick-floored passage should be open and continuous; a
   sheer wall of ordinary stone across it is not an intended mining puzzle.
3. Follow the switchbacks into the burnt basin. There are exposed drops; bring
   light, equipment and food. Hold **four vanilla chains** against the seized
   counterweight to raise the 17-block missing span. It refuses an obstructed
   deck and consumes nothing on failure. The surviving deck is lifted from below,
   not duplicated; if you salvage its blocks first, restore them or build your
   own crossing. Repair currently changes block positions immediately, with sound.
4. Continue through the rock to **The Last Binding**, at the entrance to the
   separate, dark chamber. Use the Chain Resonator on this socket to awaken
   **The Cinderbound Eye**. It is the original tarry, one-eyed chimera, with a
   dragging body, long neck and hooked tail—not a humanoid keeper.
5. The eye locks its aim before releasing three travelling cinder bolts. Each
   bolt deals six damage before armour/difficulty effects, collides with solid
   cover and expires; it never ignites or explodes terrain. The rear tail sweep
   deals nine damage where the moving hook passes, and the ground pulse deals
   seven as its ring expands from ten to twenty blocks. Cover blocks both.
   After each attack the eye lowers and becomes vulnerable for three seconds;
   the slag body resists damage. It has 360 health. Peaceful difficulty and
   creative/spectator players do not activate attacks.
6. Defeating the creature releases a **First Flame** for nearby players. Others
   can collect their first copy at the binding. A saved claim prevents repeat
   collection; if a flame is lost, offer one Elysium Fragment there to recover it.
7. Sneak-use the resonator for the remembered Forge bearing. Place the flame in
   the **original Forge hearth**. Stronger heat spreads from its physical feed
   through the connected, burning runners. Repaired channels burn brighter
   and are more dangerous; coal lasts five minutes at full heat instead of ten.
   The afternoon stays at 11,500 until you deliberately operate the Great
   Hammer Control. Read its Smith's Charge before beginning the next chapter.

The encounter and repair record persist across reloads. A chamber does not
respawn its defeated creature. Reconciliation of a missing entity waits for its
home chunks to be loaded; normal exploration never force-loads that arena.

## Generation and implementation

`TartarusDepthsPlan` owns the basin seed, route, crossing and chamber coordinates.
`TartarusDepthsPiece` evaluates those coordinates independently of neighbouring
chunks and clips every write. The saved chain structure contains both the old
anchor-vault piece and the new depths piece. Chain placement is rarer (48-chunk
spacing / 24-chunk separation), leaving room between the underground regions.
The anchor sits north of the depths plan, and every candidate offset is checked
against Minecraft's eight-chunk structure-reference reach. The client review
now loads a naturally generated region, including the far-end chunk first,
and checks the complete route and chamber before taking screenshots.
A buried lower run continues from the anchor and emerges above the basin,
so the giant iron can still be followed through the rock. The surface density
function is unchanged; the new lower volume remains solid
except where these pieces excavate their bounded spaces.

`DepthsControlBlockEntity` owns repair status, the single creature UUID, victory
and player claims. `SlagChimeraEntity` owns combat, health and the real multipart
eye target. Client-only rendering builds its anatomy from authored voxel geometry
using active-pack vanilla material references. The iris, stinger and thin ember fissures use emissive rendering; the dark
anatomy receives ordinary world light. The rendered socket and hook share
their poses with the eye hitbox and swept damage volume. A cinder bolt is an
entity with a full-bright fire-charge model, visible even with minimal particles. A small,
expiring light source follows the eye or a held Keeper’s Lamp without overwriting
solid player blocks. Held light never loads chunks and stops outside the depths.

Use `/locate structure elysium:tartarus_chain` for a natural start. This marks
the overall structure start; the Chain Resonator gives the exposed chain's
actual bearing. For inspection,
`/give @s elysium:keepers_lamp`, `/give @s elysium:chain_resonator` and
`/summon elysium:slag_chimera` are available. A manually summoned creature has no
binding and does not award the expedition flame. A creative resonator has no
remembered Forge until you obtain one from a hearth.

This update changes dimension height and generation. Start a new test world or
regenerate Elysium fully. No retrogen or old-world conversion is provided.

