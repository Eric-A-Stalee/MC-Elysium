# The dormant Forge — alpha 15

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

A modest copper-marked entrance leads down 26 blocks through a ribbed gallery.
Beyond it are a vaulted foundry, two upper galleries, an archive and smithing
workshop, a room of retorts and quench troughs, and a rear power shrine. A large
stationary hammer hangs above the central crucible. Cold runner channels and
aged copper masonry suggest how the machinery once connected.

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
restarting the Forge are **not implemented in alpha 15**. The machinery is a
dormant landmark, not a functioning smelter. Ordinary fuel cannot activate it.

## Commands and upgrading

Replace the Elysium jar on client and server and restart. New Forges,
waystations and town archives require fresh chunks. Existing towns can supply
the folio through their scholars, and existing chain anchors support the new
interaction. A dimension reset is not required.

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
reconnection. The current chapter introduces three advancements.

## Implementation boundaries

`ForgePlan` surveys base noise heights without loading neighbouring chunks.
It selects one of four orientations on a mountain shoulder and checks cover
over every vaulted space on a three-block grid. The 93-by-125-block saved
envelope includes untouched rock; the rooms occupy only part of that area.
No mountain is flattened to place the complex. `HephaestusForgePiece` stores
the plan and geometry version and writes only the current chunk's clip.

`StructureSurvey` reads the registered structure placements, including their
exclusion rules. A request visits at most 289 candidate regions around the
player, with at most eight queued players and one candidate inspected globally
per server tick. A candidate can still require synchronous structure-start
generation; this is a work-count budget, not a millisecond guarantee. The
search does not fully generate every target chunk or keep it force-loaded.
Changing dimensions, disconnecting or losing the requesting item cancels a
pending request. Results and hearth memory use normal item data components.

Progress is separate per-player saved data. Books use vanilla written-book
components and localized pages. Models are original cuboid geometry referencing
active-pack vanilla textures. There is no new dependency, packet protocol,
global trade-pool replacement, Minecraft texture redistribution, or automatic
retrogen. Existing town building grammar is retained; only newly planned
great halls save the archive-placement flag.
