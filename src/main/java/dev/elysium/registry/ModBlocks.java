package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.block.WildGrainBlock;
import dev.elysium.block.KeepersLampBlock;
import dev.elysium.block.KeeperWorkbenchBlock;
import dev.elysium.block.ChimeraGlowBlock;
import dev.elysium.block.DepthsControlBlock;
import dev.elysium.block.GoldenBirchLeavesBlock;
import dev.elysium.block.LeafLitterBlock;
import dev.elysium.block.ForgeRecordBlock;
import dev.elysium.block.DormantHearthBlock;
import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeFlameBlock;
import dev.elysium.block.ForgeDebrisBlock;
import dev.elysium.forge.ForgeLore;
import dev.elysium.structure.ForgeFixtures;
import dev.elysium.portal.HarvestAltarBlock;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
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
    public static final DeferredBlock<net.minecraft.world.level.block.TransparentBlock> EMBER_GLASS = BLOCKS.register("ember_glass",
            () -> new net.minecraft.world.level.block.TransparentBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).lightLevel(s -> 10)));
    public static final DeferredBlock<dev.elysium.block.WatchfireBlock> WATCHFIRE = BLOCKS.register("watchfire",
            () -> new dev.elysium.block.WatchfireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noLootTable().lightLevel(s -> switch(s.getValue(dev.elysium.block.WatchfireBlock.EMBER)){case 1->3;case 2->15;default->0;})));
    public static final DeferredBlock<dev.elysium.block.CelestialGovernorBlock> CELESTIAL_GOVERNOR = BLOCKS.register("celestial_governor",
            () -> new dev.elysium.block.CelestialGovernorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(8)));
    public static final DeferredBlock<dev.elysium.block.ForgePressBlock> FORGE_PRESS = BLOCKS.register("forge_press",
            () -> new dev.elysium.block.ForgePressBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(6)));
    public static final DeferredBlock<dev.elysium.block.WardLampBlock> WARD_LAMP = BLOCKS.register("ward_lamp",
            () -> new dev.elysium.block.WardLampBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).lightLevel(s -> 15)));
    public static final DeferredBlock<KeepersLampBlock> KEEPERS_LAMP = BLOCKS.register("keepers_lamp",
            () -> new KeepersLampBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).lightLevel(s -> 13)));
    public static final DeferredBlock<KeeperWorkbenchBlock> KEEPER_WORKBENCH = BLOCKS.register("keeper_workbench",
            () -> new KeeperWorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE)));
    public static final DeferredBlock<Block> BURNT_CLINKER = BLOCKS.register("burnt_clinker",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE)));
    public static final DeferredBlock<Block> CINDER_ASH = BLOCKS.register("cinder_ash",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF).strength(.8f)));
    public static final DeferredBlock<Block> SMOULDERING_CLINKER = BLOCKS.register("smouldering_clinker",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).lightLevel(s -> 4)));
    public static final DeferredBlock<ChimeraGlowBlock> CHIMERA_GLOW = BLOCKS.register("chimera_glow",
            () -> new ChimeraGlowBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().noLootTable().lightLevel(s -> s.getValue(ChimeraGlowBlock.LIGHT))));
    public static final DeferredBlock<DepthsControlBlock> TARTARUS_COUNTERWEIGHT = BLOCKS.register("tartarus_counterweight",
            () -> new DepthsControlBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noLootTable()));
    public static final DeferredBlock<DepthsControlBlock> TARTARUS_HEART = BLOCKS.register("tartarus_heart",
            () -> new DepthsControlBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noLootTable()));
    public static final DeferredBlock<dev.elysium.block.PrometheusBlock> PROMETHEUS_VOICE = BLOCKS.register("prometheus_voice",()->new dev.elysium.block.PrometheusBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).noLootTable().lightLevel(s->5)));
    public static final DeferredBlock<Block> CHARRED_BONE=BLOCKS.register("charred_bone",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BONE_BLOCK)));
    public static final DeferredBlock<dev.elysium.block.EmberRootBlock> EMBER_ROOT=BLOCKS.register("ember_root",()->new dev.elysium.block.EmberRootBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().instabreak().sound(SoundType.ROOTS).lightLevel(s->5)));
    public static final int GOLDEN_LEAF_COLOR = 0xFFB947;
    public static final DeferredBlock<ForgeFlameBlock> FORGE_FLAME = BLOCKS.register("forge_flame",
            () -> new ForgeFlameBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion().replaceable().noLootTable().instabreak()
                    .lightLevel(s -> s.getValue(ForgeFlameBlock.RESTORED)?(s.getValue(ForgeFlameBlock.BRAZIER)?15:14):(s.getValue(ForgeFlameBlock.BRAZIER)?13:10))));
    public static final DeferredBlock<ForgeServiceBlock> FORGE_SERVICE_LAMP = BLOCKS.register("forge_service_lamp",
            () -> new ForgeServiceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WAXED_WEATHERED_COPPER_BULB)
                    .lightLevel(s -> s.getValue(ForgeServiceBlock.LIT) ? (s.getValue(ForgeServiceBlock.RESTORED)?15:12) : 0), ForgeFixtures.Kind.LAMP));
    public static final DeferredBlock<ForgeServiceBlock> FORGE_SERVICE_SWITCH = BLOCKS.register("forge_service_switch",
            () -> new ForgeServiceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WAXED_WEATHERED_COPPER_BULB)
                    .strength(4).lightLevel(s -> s.getValue(ForgeServiceBlock.LIT) ? (s.getValue(ForgeServiceBlock.RESTORED)?15:13) : 0), ForgeFixtures.Kind.BRAZIER));
    public static final DeferredBlock<ForgeServiceBlock> FORGE_EMBER_CHANNEL = BLOCKS.register("forge_ember_channel",
            () -> new ForgeServiceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE)
                    .strength(2).lightLevel(s -> s.getValue(ForgeServiceBlock.LIT) ? (s.getValue(ForgeServiceBlock.RESTORED)?14:10) : 0), ForgeFixtures.Kind.CHANNEL));
    public static final DeferredBlock<Block> FORGE_PILOT_LIGHT = BLOCKS.register("forge_pilot_light",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.WAXED_OXIDIZED_COPPER_BULB).lightLevel(s -> 3)));
    public static final DeferredBlock<Block> FORGE_IRON = BLOCKS.register("forge_iron",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(7,30).sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<ForgeDebrisBlock> FORGE_DEBRIS = BLOCKS.register("forge_debris",
            () -> new ForgeDebrisBlock(BlockBehaviour.Properties.of().strength(.3f).noOcclusion().noCollission()
                    .sound(SoundType.TUFF).mapColor(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<ForgeRecordBlock> FORGE_ARCHIVE = BLOCKS.register("forge_archive",
            () -> new ForgeRecordBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).strength(2.5F),ForgeLore.Record.SURVEY));
    public static final DeferredBlock<ForgeRecordBlock> FORGE_LEDGER = BLOCKS.register("forge_ledger",
            () -> new ForgeRecordBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_DEEPSLATE).strength(4.0F),ForgeLore.Record.LEDGER));
    public static final DeferredBlock<ForgeRecordBlock> TARTARUS_INSCRIPTION = BLOCKS.register("tartarus_inscription",
            () -> new ForgeRecordBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_POLISHED_BLACKSTONE).strength(4.0F),ForgeLore.Record.BINDINGS));
    public static final DeferredBlock<DormantHearthBlock> FORGE_HEARTH = BLOCKS.register("forge_hearth",
            () -> new DormantHearthBlock(BlockBehaviour.Properties.of().strength(12.0F,1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_GRAY).lightLevel(s->s.getValue(DormantHearthBlock.FLAME)?15:1)));
    public static final DeferredBlock<Block> TARTARUS_CHAIN_BLOCK = BLOCKS.register("tartarus_chain",
            () -> new Block(BlockBehaviour.Properties.of().strength(8.0F, 1200.0F).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK).mapColor(MapColor.COLOR_GRAY)));
    private static final TreeGrower GOLDEN_BIRCH_GROWER = new TreeGrower("elysium_golden_birch",
            Optional.empty(), Optional.of(ResourceKey.create(Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID, "golden_birch"))), Optional.empty());

    public static final DeferredBlock<LeavesBlock> GOLDEN_BIRCH_LEAVES = BLOCKS.register("golden_birch_leaves",
            () -> new GoldenBirchLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LEAVES).mapColor(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<SaplingBlock> GOLDEN_BIRCH_SAPLING = BLOCKS.register("golden_birch_sapling",
            () -> new SaplingBlock(GOLDEN_BIRCH_GROWER, BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_SAPLING)));
    public static final DeferredBlock<WildGrainBlock> WILD_GRAIN = BLOCKS.register("wild_grain",
            () -> new WildGrainBlock(BlockBehaviour.Properties.of().noCollission().instabreak()
                    .sound(SoundType.CROP).mapColor(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<LeafLitterBlock> LEAF_LITTER = BLOCKS.register("leaf_litter",
            () -> new LeafLitterBlock(BlockBehaviour.Properties.of().noCollission().instabreak()
                    .sound(SoundType.GRASS).mapColor(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<HarvestAltarBlock> HARVEST_ALTAR = BLOCKS.register("harvest_altar",
            () -> new HarvestAltarBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).noOcclusion()
                    .sound(SoundType.STONE).mapColor(MapColor.QUARTZ)
                    .lightLevel(state -> state.getValue(HarvestAltarBlock.ACTIVE) ? 12 : 0)));

    private ModBlocks() {}
}
