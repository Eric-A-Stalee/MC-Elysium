package dev.elysium.story;

import dev.elysium.forge.ForgeDiscoveries;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;

/** Reminders follow this reader's discoveries, even on a realm changed by other players. */
public final class StoryJournal {
    public static List<Component> entries(RealmCycle cycle,ForgeDiscoveries notes,UUID reader) {
        var entries=new ArrayList<Component>();
        entries.add(Component.translatable("message.elysium.story.status",
                Component.translatable("phase.elysium."+cycle.phase().name().toLowerCase(java.util.Locale.ROOT))));
        if(notes.has(reader,ForgeDiscoveries.WATCH_RECORDS))
            entries.add(Component.translatable("message.elysium.story.watches",Integer.bitCount(cycle.watches())));
        if(notes.has(reader,ForgeDiscoveries.DEEP_RECORDS))
            entries.add(Component.translatable("message.elysium.story.bindings",Integer.bitCount(cycle.bindings())));
        entries.add(Component.translatable("message.elysium.story.next."+hint(cycle,notes,reader)));
        return List.copyOf(entries);
    }

    private static String hint(RealmCycle cycle,ForgeDiscoveries notes,UUID reader) {
        boolean workshop=notes.has(reader,ForgeDiscoveries.HEARTH|ForgeDiscoveries.PROMETHEUS);
        if(workshop&&cycle.phase()==RealmCycle.Phase.LIVING)return "living";
        if(workshop&&cycle.phase()==RealmCycle.Phase.GOLDEN)return "golden";
        if(workshop&&cycle.released()&&cycle.watches()==7&&cycle.bindings()==7)return "governor";
        if(cycle.released()&&cycle.bindings()!=7&&notes.has(reader,ForgeDiscoveries.HOPE))return "bindings";
        if(cycle.unsettled()&&cycle.watches()!=7&&notes.has(reader,ForgeDiscoveries.WATCH_RECORDS|ForgeDiscoveries.PROMETHEUS))return "watches";
        if(notes.has(reader,ForgeDiscoveries.PROMETHEUS))return cycle.released()?"workshop":"smithing";
        if(notes.has(reader,ForgeDiscoveries.ANCHOR))return "depths";
        if(notes.has(reader,ForgeDiscoveries.HEARTH))return "chain";
        if(notes.has(reader,ForgeDiscoveries.SURVEY|ForgeDiscoveries.LEDGER|ForgeDiscoveries.BINDINGS))return "forge";
        return "explore";
    }
    private StoryJournal() {}
}
