package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** A read-only journal/status aid, available without cheats. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class StoryCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("elysium").then(Commands.literal("story").executes(context->{
            var source=context.getSource();var level=source.getServer().getLevel(ElysiumTravel.DIMENSION);if(level==null)return 0;
            var cycle=RealmCycle.get(level);
            source.sendSuccess(()->Component.translatable("message.elysium.story.status",Component.translatable("phase.elysium."+cycle.phase().name().toLowerCase(java.util.Locale.ROOT)),Integer.bitCount(cycle.watches()),Integer.bitCount(cycle.bindings())),false);
            String next=switch(cycle.phase()){case PARADISE->"forge";case LIVING->"living";case GOLDEN->"golden";default->cycle.watches()!=7?"watches":cycle.bindings()!=7?"bindings":"governor";};
            source.sendSuccess(()->Component.translatable("message.elysium.story.next."+next),false);
            return 1;
        })));
    }
    private StoryCommands(){}
}
