# The buried Forge — alpha 17

Elysium now has an optional exploration chapter: find a surviving archive,
locate the Forge of Hephaestus, recover an instrument from its cold hearth,
and listen at a deep Chain of Tartarus anchor. The golden afternoon continues
at **11,500 ticks**. Visiting, reading or collecting anything in this chapter
does not start a timer or a world event.

## Finding the old work

New grand mountain-town halls contain a copper-marked **Survey Archive**.
Abandoned **Forge Waystations** on suitable mountain shoulders contain the
same record. Right-click an archive for *The Last Survey* and a
**Surveyor's Folio**. Each player gets their own first copy. Offer a blank
book to make a later replacement copy.

Elysium cartographers and librarians also offer the folio, including villagers
in existing towns. Their supplemental trade starts at six emeralds and follows
vanilla merchant pricing and restocking. Other dimensions and professions are
unaffected. This route requires no changes to an old town's blocks.

Use the folio in Elysium. A bounded survey searches nearby structure candidates
over several ticks; the result gives the entrance's X/Z and a directional
bearing. Use it again for the bearing and a brief particle trail. Sneak-use to
survey from a new location. If nothing is found, travel to another mountain
region and try again. It does not promise the nearest Forge or unlimited range.

Only actual saved structure starts are accepted. An old chunk without a Forge
will not acquire one merely because the new terrain planner could fit it there.

## Inside the mountain

A narrow cleft beside scree and roots leads past buried masonry. Only a small
corroded fragment of the original entrance survives. The foundry floor is
**72 blocks below the entrance**, reached by roughly 160 blocks of stairs,
landings and passages. Broken stairs wind around a hauling shaft with a
failed timber lift, hanging counterweight, rubble bottom and incomplete rails.
Rockfall narrows several passages; exposed edges can drop into the shaft.
Bring a pick and your own light. A few weak pilot embers mark surviving stations.

The maintenance adit opens onto a high overlook, then joins the west gallery.
The ordinary gallery stair leads down to the foundry floor. Entry, descent,
overlook and hall are distinct spaces; the upper entrance cannot see the hall.
The planner retains at least 80 blocks above the dimension's minimum height
for the foundry floor. In the current dimension this means Y=16 or higher,
above the existing Tartarus anchor vault, whose floor is Y=-48.

The main vault contains a heavy suspended hammer and its aligned anvil, guide
piers, a stepped crosshead and an unfinished Chain of Tartarus link resting in
its cradle. The cold, slag-filled crucible stands separately. One upper gallery
ends in a collapse with a corresponding rubble fan and raw rock protruding
through its vault. Broken rails, mineral streaks, cracked masonry, slag and
small walk-through debris collect where the old work and damage occurred.
Weathered Forge Iron supplies dark machine plate using an active-pack vanilla
metal texture. It is a decorative salvage block, with no netherite or ingot
conversion recipe.

The west workshop contains an archive, smithing benches, racks and casting
moulds. The east workshop has a cold furnace bank, exhaust hoods, quench trough
and casting stations. Surviving stairways reach the galleries, and a passage
continues to the rear power shrine. All of this is inside the surveyed mountain;
the chamber does not deliberately open a skylight through the peak.

## Restoring a little light

The **Forge Service Brazier** stands near the front of the hall. Add coal or
charcoal with right-click: one item supplies ten minutes of burning time, up
to eighty minutes stored. Ignite it with flint and steel or a fire charge,
then use an empty hand on the wheel to open its draught. Fuel and ignition
alone cannot light a closed network, and the wheel cannot supply fuel.
Opening an ignited, supplied brazier awards **A Little Light Remains**.

Embers advance along actual neighbouring **Forge Ember Channels**, roughly
six blocks per second. The exposed copper runners lead to branch braziers
around the machinery, workbenches and galleries. Dark branches tell you where
the old system has failed. Light does not jump directly to a list of fixtures.

- The west workshop runner is clogged with ash. Right-click it to clear it.
- The east workshop runner is cracked. Use one gunpowder to pack an ignition
  trail across the short break. This repair survives later extinguishing.
  Mining an unrepaired cracked runner yields only an iron nugget.
- The rear shrine runner has an actual missing piece. Replace it with an
  Ember Channel: two iron ingots above three copper ingots craft eight.

Repairs leave the physical blocks intact and persist with the chunks. Mining
any connecting runner interrupts its downstream fire; the simulation never
replaces missing blocks. Replacement channels bind through adjacent original
fixtures, and operate only at authored service-route positions. This is a
Forge repair system, not a general-purpose cable network for arbitrary builds.

The central reservoir spends fuel while burning and ticking. Closing the
draught banks its ignition and preserves the remaining fuel while extinguishing
the branches. Exhaustion extinguishes the fire and requires another ignition.
Fuel is saved in the brazier, never copied to branch fixtures. Unloaded source
chunks spend no fuel; the network does not force-load them. A loaded branch
losing its source goes dark. On reload the saved brazier rebuilds its fire
through the physical loaded connections, so an unloaded/missing section cannot
silently conduct. Other Forges have independent reservoirs and bindings.

Restored lighting remains uneven, with small flames above the runners and
braziers, warm pools around old workstations and dark heights beyond them.
This service fire cannot restart the hammer, supply the First Flame, light
the great hearth or start a world event.

Read the **Keeper's Ledger** in the archive workshop and the **Binding
Inscription** behind the foundry. These provide ordinary written books, with
the same personal first-copy and blank-book replacement rules as the archive.
They introduce the missing First Flame and the older work beneath the realm.

Examine the **Hearth of Hephaestus** to receive a **Chain Resonator**. Each
player can recover one independently. If it is lost, offer an amethyst shard
at a hearth to tune a replacement; a shard is not consumed when an instrument
is already in your inventory. The new instrument remembers that hearth.

## Following the iron

Use the resonator in Elysium to survey for a Chain of Tartarus, then use it
again to hear its bearing. Sneak-use recalls the hearth's direction instead.
The chain target is its upper exposure; follow the physical links through
stone to the deep chamber. The instrument does not excavate a path.

At the central plinth, use the resonator on the lowest iron of the anchor.
An ordinary placed chain block or a high exposed link is not sufficient.
The echo awards *A Voice in the Iron*, marks the instrument and completes the
current discovery sequence. Existing naturally generated chain chambers can
also be used; their saved structure identity is checked.

The full Tartarus region, its keeper/boss, obtaining the First Flame and
restarting the Forge are **not implemented in alpha 17**. The great hammer
and hearth remain dormant; ordinary fuel cannot restart them. The smaller
vanilla workshop furnaces can still be used normally by a player.

## Commands and upgrading

Replace the Elysium jar on client and server and restart. Use a new alpha test
world or regenerate Elysium for this update. Earlier Forge structure saves are
not supported by the new geometry. No conversion or automatic retrogen is
included. The rest of the realm's terrain, palette, towns and chain chambers
retain their current design.

Inside Elysium, these commands help inspect the chapter:

```mcfunction
/locate structure elysium:forge_waystation
/locate structure elysium:hephaestus_forge
/locate structure elysium:tartarus_chain
/give @s elysium:forge_survey
```

The Forge deliberately requires suitable mountain cover, so a synchronous
vanilla `/locate` can take time. The folio spreads its work across ticks. A
creative resonator has no remembered hearth until you obtain one at a hearth.
Discovery state persists per player with the Elysium world, across death and
reconnection. The current chapter introduces four advancements.

## Implementation boundaries

`ForgePlan` surveys base noise heights without loading neighbouring chunks.
It selects one of four orientations on a mountain shoulder and checks cover
over every vaulted space on a three-block grid. The 93-by-128-block saved
envelope includes untouched rock; the rooms occupy only part of that area.
No mountain is flattened to place the complex. `HephaestusForgePiece` stores
the plan and geometry version and writes only the current chunk's clip.
`ForgeGeometry` composes the entrance, shell, machinery, workshops and damage.
`ForgeDescent` supplies a separate deterministic 3D route and shaft, allowing
several passage levels in one column. `ForgeFixtures` describes the service
network once in local coordinates for generation and bounded connectivity.
Each fixture saves its Forge binding. Only the source brazier has a block-entity
ticker: twice per second `ForgeLighting` traverses existing, loaded, intact
connections and advances the flame front. Illuminated fixtures also reconcile
with their source on scheduled ticks, so source unloading cannot leave eternal
lights. No chunk tickets are requested by the lighting system.

`StructureSurvey` reads the registered structure placements, including their
exclusion rules. A request visits at most 289 candidate regions around the
player, with at most eight queued players and one candidate inspected globally
per server tick. A candidate can still require synchronous structure-start
generation; this is a work-count budget, not a millisecond guarantee. The
search does not fully generate every target chunk or keep it force-loaded.
Changing dimensions, disconnecting or losing the requesting item cancels a
pending request. Results and hearth memory use normal item data components.

Lore progress is separate per-player saved data. Books use vanilla written-book
components and localized pages. Models are original cuboid geometry referencing
active-pack vanilla textures. There is no new dependency, packet protocol,
global trade-pool replacement, Minecraft texture redistribution, or automatic
retrogen. Existing town building grammar is retained; only newly planned
great halls save the archive-placement flag.
