package dev.elysium;

import dev.elysium.portal.ElysiumTravel;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Development entry and an escape route share the portal's safe landing and return path. */
@EventBusSubscriber(modid = Elysium.MOD_ID)
public final class ElysiumCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("elysium")
                .then(Commands.literal("visit").requires(source -> source.hasPermission(2)).executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    return ElysiumTravel.enter(player, player.blockPosition()) ? 1 : 0;
                }))
                .then(Commands.literal("return").executes(context ->
                        ElysiumTravel.returnHome(context.getSource().getPlayerOrException()) ? 1 : 0)));
    }

    private ElysiumCommands() {}
}
