package dev.elysium.client;

import dev.elysium.Elysium;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import dev.elysium.registry.ModAmbience;
import dev.elysium.portal.ElysiumTravel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Every client-class reference stays in this client-only subscriber. */
@EventBusSubscriber(modid = Elysium.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ElysiumClient {
    @SubscribeEvent public static void entities(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.CHAIN_WARDEN.get(),ChainWardenRenderer::new);
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.ASHEN_HOUND.get(),AshenHoundRenderer::new);
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.BRONZE_EAGLE.get(),BronzeEagleRenderer::new);
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.BRONZE_FEATHER.get(),c->new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(c,1.4f,false));
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.CINDER_WISP.get(),CinderWispRenderer::new);
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.SLAG_CHIMERA.get(), SlagChimeraRenderer::new);
        event.registerEntityRenderer(dev.elysium.registry.ModEntities.CINDER_BOLT.get(), context ->
                new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context,1.5f,true));
    }
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent event){event.register(ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID,"elysium"),new ElysiumSky());}
    @SubscribeEvent
    public static void grassModels(ModelEvent.ModifyBakingResult event) {
        // The supplier runs when chunk model data is requested, not during baking.
        ElysiumGrassModel.register(event, () -> {
            var level = Minecraft.getInstance().level;
            return level != null && level.dimension().equals(ElysiumTravel.DIMENSION);
        });
    }

    @SubscribeEvent
    public static void spriteSources(RegisterSpriteSourceTypesEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID, "tone_mapped"), ToneMappedSpriteSource.TYPE);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModAmbience.GOLDEN_LEAF.get(), sprites ->
                (type, level, x, y, z, dx, dy, dz) -> new GoldenLeafParticle(level, x, y, z, sprites));
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state,level,pos,tint)->0xA28A70,ModBlocks.CHARRED_BONE.get());
        event.register((state, level, pos, tint) -> foliageColor(level, pos),
                ModBlocks.GOLDEN_BIRCH_LEAVES.get(), ModBlocks.LEAF_LITTER.get());
    }

    static int foliageColor(BlockAndTintGetter level, BlockPos pos) {
        var world = Minecraft.getInstance().level;
        return level != null && pos != null && world != null && world.dimension().equals(ElysiumTravel.DIMENSION)
                ? BiomeColors.getAverageFoliageColor(level, pos) : ModBlocks.GOLDEN_LEAF_COLOR;
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack,tint)->FastColor.ARGB32.opaque(0xA28A70),ModBlocks.CHARRED_BONE.get());
        // 1.21.1's ItemRenderer reads ARGB, including alpha. Our biome palette
        // uses RGB, so pass opaque tints or held/dropped items become invisible.
        event.register((stack, tint) -> FastColor.ARGB32.opaque(ModBlocks.GOLDEN_LEAF_COLOR),
                ModBlocks.GOLDEN_BIRCH_LEAVES.get(), ModBlocks.LEAF_LITTER.get());
        event.register((stack, tint) -> FastColor.ARGB32.opaque(0xFFE4A1),
                ModItems.ELYSIUM_SHARD.get(), ModItems.ELYSIUM_FRAGMENT.get(), ModItems.ELYSIUM_SIGIL.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.EMBER_GLASS.get(),RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLDEN_BIRCH_LEAVES.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLDEN_BIRCH_SAPLING.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.WILD_GRAIN.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LEAF_LITTER.get(), RenderType.cutout());
        });
    }

    private ElysiumClient() {}
}
