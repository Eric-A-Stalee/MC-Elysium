package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.forge.ForgeDiscoveries;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** A read-only journal/status aid, available without cheats. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class StoryCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("elysium").then(Commands.literal("story").executes(context->{
            var source=context.getSource();var level=source.getServer().getLevel(ElysiumTravel.DIMENSION);if(level==null)return 0;
            var player=source.getPlayerOrException();
            for(var entry:StoryJournal.entries(RealmCycle.get(level),ForgeDiscoveries.get(level),player.getUUID()))
                source.sendSuccess(()->entry,false);
            return 1;
        })));
    }
    private StoryCommands(){}
}
