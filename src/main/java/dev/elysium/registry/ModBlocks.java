package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.block.WildGrainBlock;
import dev.elysium.portal.HarvestAltarBlock;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Elysium.MOD_ID);
    public static final int GOLDEN_LEAF_COLOR = 0xE8BB39;
    private static final TreeGrower GOLDEN_BIRCH_GROWER = new TreeGrower("elysium_golden_birch",
            Optional.empty(), Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID, "golden_birch"))), Optional.empty());

    public static final DeferredBlock<LeavesBlock> GOLDEN_BIRCH_LEAVES = BLOCKS.register("golden_birch_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LEAVES).mapColor(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<SaplingBlock> GOLDEN_BIRCH_SAPLING = BLOCKS.register("golden_birch_sapling",
            () -> new SaplingBlock(GOLDEN_BIRCH_GROWER, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SAPLING)));
    public static final DeferredBlock<WildGrainBlock> WILD_GRAIN = BLOCKS.register("wild_grain",
            () -> new WildGrainBlock(BlockBehaviour.Properties.of().noCollission().instabreak()
                    .sound(SoundType.CROP).mapColor(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<HarvestAltarBlock> HARVEST_ALTAR = BLOCKS.register("harvest_altar",
            () -> new HarvestAltarBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).noOcclusion()
                    .sound(SoundType.STONE).mapColor(MapColor.QUARTZ)
                    .lightLevel(state -> state.getValue(HarvestAltarBlock.ACTIVE) ? 12 : 0)));

    private ModBlocks() {}
}
