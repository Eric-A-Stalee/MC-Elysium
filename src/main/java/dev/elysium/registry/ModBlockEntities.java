package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.block.ForgeServiceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Elysium.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeServiceBlockEntity>> FORGE_SERVICE = TYPES.register("forge_service",
            () -> BlockEntityType.Builder.of(ForgeServiceBlockEntity::new, ModBlocks.FORGE_SERVICE_LAMP.get(), ModBlocks.FORGE_SERVICE_SWITCH.get(), ModBlocks.FORGE_EMBER_CHANNEL.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.elysium.block.DepthsControlBlockEntity>> DEPTHS_CONTROL = TYPES.register("depths_control",
            () -> BlockEntityType.Builder.of(dev.elysium.block.DepthsControlBlockEntity::new, ModBlocks.TARTARUS_COUNTERWEIGHT.get(), ModBlocks.TARTARUS_HEART.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.elysium.block.ForgePressBlockEntity>> FORGE_PRESS = TYPES.register("forge_press",
            () -> BlockEntityType.Builder.of(dev.elysium.block.ForgePressBlockEntity::new,ModBlocks.FORGE_PRESS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<dev.elysium.block.WatchfireBlockEntity>> WATCHFIRE = TYPES.register("watchfire",
            () -> BlockEntityType.Builder.of(dev.elysium.block.WatchfireBlockEntity::new,ModBlocks.WATCHFIRE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<dev.elysium.block.PrometheusBlockEntity>> PROMETHEUS=TYPES.register("prometheus",()->BlockEntityType.Builder.of(dev.elysium.block.PrometheusBlockEntity::new,ModBlocks.PROMETHEUS_VOICE.get()).build(null));
    private ModBlockEntities() {}
}
