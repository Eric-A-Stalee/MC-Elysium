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
            () -> BlockEntityType.Builder.of(ForgeServiceBlockEntity::new, ModBlocks.FORGE_SERVICE_LAMP.get(), ModBlocks.FORGE_SERVICE_SWITCH.get()).build(null));
    private ModBlockEntities() {}
}
