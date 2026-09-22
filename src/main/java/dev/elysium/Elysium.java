package dev.elysium;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import dev.elysium.registry.ModStructures;
import dev.elysium.loot.WheatShardModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Elysium.MOD_ID)
public final class Elysium {
    public static final String MOD_ID = "elysium";

    public Elysium(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        WheatShardModifier.SERIALIZERS.register(modBus);
        ModStructures.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ElysiumConfig.SPEC);
    }
}
