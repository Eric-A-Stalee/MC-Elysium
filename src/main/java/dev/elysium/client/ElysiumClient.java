package dev.elysium.client;

import dev.elysium.Elysium;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Every client-class reference stays in this client-only subscriber. */
@EventBusSubscriber(modid = Elysium.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElysiumClient {
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> ModBlocks.GOLDEN_LEAF_COLOR, ModBlocks.GOLDEN_BIRCH_LEAVES.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> ModBlocks.GOLDEN_LEAF_COLOR, ModBlocks.GOLDEN_BIRCH_LEAVES.get());
        event.register((stack, tint) -> 0xFFE4A1, ModItems.ELYSIUM_SHARD.get(), ModItems.ELYSIUM_FRAGMENT.get(), ModItems.ELYSIUM_SIGIL.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLDEN_BIRCH_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLDEN_BIRCH_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WILD_GRAIN.get(), RenderType.cutout());
        });
    }

    private ElysiumClient() {}
}
