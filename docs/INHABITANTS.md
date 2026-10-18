# The things Tartarus holds — alpha 26

Tartarus now has a living prisoner at Dominion, alongside its giant remains and older Cinderbound Eye. This is the beginning of a bestiary with relationships, not a claim that the whole prison is populated or finished.

| Inhabitant | Place in the world | Player relationship |
|---|---|---|
| Cinderbound Eye | The chained chimera at the threshold | Recover its First Flame to restore the Forge. |
| Bronze Eagle | Prometheus's airborne jailer | Survive marked dives and pinions; strike during recovery. |
| Ashen Hunger | A blind hunter in the dry depths | Footsteps and hits carry sound; crouching and cover matter. |
| The Sentenced | Walking remnants of the Crownless's orders | Chain lanes have a warning and recovery; one belongs to Dominion's restraint. |
| The Crownless | A living, bound sovereign | His voice escapes after divine forging. Contain him by repairing Dominion. |
| Mire Mourner | Grief wearing the appearance of warmth | Leave the marked ground; real light and Hope protect you. |
| Stray Cinder | Fire loose upon the unsettled surface | The watches and wards offer refuge. |
| Ash Moth | Harmless life around real embers | Observe where it gathers; its false counterpart does not attract it. |

These are eight non-projectile entity types. Prometheus's monumental body is
generated architecture with an interactive voice, rather than a ninth mob.

## The Crownless

An original imprisoned sovereign, roughly 34 blocks high while seated. His wrists are visibly chained to two 58-block anchors. His body breathes; his head follows nearby survivors; his crown is broken. He survives the repair of his restraint.

Before the first completed divine forging, he sleeps. After the First Flame opens the vessel, his voice can escape while his body remains bound. A three-second intake of breath precedes a travelling command across the ground. The command deals five damage once per wave, pushes exposed players, and tells nearby Hungers and Sentenced where its target is. Broken stone cover stops it. The long pause between commands leaves time to fight the restraint's own Sentenced jailer and use a Binding Nail. Repairing Dominion returns the captive to sleep.

This is a containment encounter. The Crownless is not a conventional 800-health damage sponge: ordinary attacks cannot kill him. The vulnerable part of his prison is its named jailer and binding. The record at the restraint explains this before the player needs to guess. Peaceful leaves the giant dormant and preserves the existing peaceful binding path.

The old Empty Seat is rebuilt into an open, broken apse around the living body. Its expanded survey, natural terrain placement, approach, body clearance, and chunk references remain part of normal structure generation. Locate it with `/locate structure elysium:dominion_binding`, or use the resonator's Dominion destination.

## Mire Mourner

A tall, hollow shroud over long hands and a false amber hearth. Naturally inhabits the Hollow Mire. Unrepaired Sorrow wounds can also release mourners onto the surface after the First Flame is used. It marks a fixed patch of ground with ash for two seconds, then harms and briefly slows a survivor still inside it. Move out of the marked patch; the attack does not follow you. Solid cover, Hope held in either hand, a watch ward, or actual block light of level 10 at the player prevents acquisition and damage. A glowing face is not a source of block light.

## Ash Moth

A harmless, roughly two-block winged inhabitant of the deep strata, also present on Peaceful. Four eyespots and amber cells are drawn in its original wing sprite. It circles real emitting blocks within sight, including ember roots and a Keeper's Lamp, while ignoring the mourner's fake glow. Moths provide a small visual clue about genuine warmth. They are not a guaranteed route finder and have no farmable combat drop.

## Existing inhabitants, revisited

The eagle has a substantially larger body (6.4 × 5.4 block collision bounds) and a roughly 26-block spread-wing silhouette. Separate coverts, primary feathers, hooked beak, brow, crest and talons use original materials. The impact warning now marks the enlarged body footprint, with a 48-tick windup and 28 ticks after its aim locks. The grounded recovery remains; health is unchanged. The projectile and carried Bronze Pinion use an original feather sprite as well.

Hungers and Sentenced also use original hide, horn, bone and bronze materials instead of stretched vanilla building blocks. Sentenced now sometimes patrol the Chain Forest; Hungers inhabit the other dry regions. Their four-creature local limit and forty-creature realm limit are shared with mourners. Moths have a separate five-local / forty-eight-realm cap. Ecology never forces chunks merely to find a spawn and honors `doMobSpawning`.

## Art and evidence

`tools/generate_tartarus_art.py` reproduces thirteen original PNGs from code without sampling Minecraft or concept-image pixels. `--check` verifies the committed bytes. `--preview` adds a Pillow contact sheet of the exact sprites. The first skin study resembled masonry. The next looked like wood grain on the real models, so the final skin uses subdued, irregular ash patches and shorter scars. Bone was likewise revised after inspecting its large structures. The art sheet lives at `docs/art/alpha26-materials.png`.

The entity renderer uses textured faces and face normals directly. The four terrain materials replace former vanilla references: warm ash, burnt clinker, smouldering clinker and charred bone. Resource packs can override the new `elysium:textures/...` files normally.

The opt-in `[capture-inhabitants]` review generates actual natural prisons, checks the titan's full collision volume, then captures ten Minecraft client views without shaders. Geometry inspection uses clearly documented night vision; `inhabitant_dark` removes it and uses one real Keeper's Lamp. The command view waits for a real warning against a damage-resistant survival observer. The close creature views use posed entities so materials and silhouette can be judged consistently. These are in-game captures, not concept renders.
