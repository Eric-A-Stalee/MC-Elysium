package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.item.ResonanceItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Elysium.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Elysium.MOD_ID);
    public static final DeferredItem<Item> ELYSIUM_SHARD = ITEMS.registerSimpleItem("elysium_shard");
    public static final DeferredItem<Item> ELYSIUM_FRAGMENT = ITEMS.registerSimpleItem("elysium_fragment");
    public static final DeferredItem<Item> ELYSIUM_SIGIL = ITEMS.registerSimpleItem("elysium_sigil", new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<ResonanceItem> FORGE_SURVEY = ITEMS.register("forge_survey", () -> new ResonanceItem(new Item.Properties().stacksTo(1),true));
    public static final DeferredItem<ResonanceItem> CHAIN_RESONATOR = ITEMS.register("chain_resonator", () -> new ResonanceItem(new Item.Properties().stacksTo(1).fireResistant(),false));

    static {
        ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_BIRCH_LEAVES);
        ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_BIRCH_SAPLING);
        ITEMS.registerSimpleBlockItem(ModBlocks.WILD_GRAIN);
        ITEMS.registerSimpleBlockItem(ModBlocks.LEAF_LITTER);
        ITEMS.registerSimpleBlockItem(ModBlocks.TARTARUS_CHAIN_BLOCK);
        ITEMS.registerSimpleBlockItem(ModBlocks.HARVEST_ALTAR);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_ARCHIVE);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_LEDGER);
        ITEMS.registerSimpleBlockItem(ModBlocks.TARTARUS_INSCRIPTION);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_HEARTH);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_SERVICE_LAMP);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_SERVICE_SWITCH);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_PILOT_LIGHT);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_DEBRIS);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_IRON);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELYSIUM_TAB = TABS.register("elysium",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.elysium"))
                    .icon(() -> ELYSIUM_SIGIL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ELYSIUM_SHARD); output.accept(ELYSIUM_FRAGMENT); output.accept(ELYSIUM_SIGIL);
                        output.accept(ModBlocks.HARVEST_ALTAR); output.accept(ModBlocks.GOLDEN_BIRCH_LEAVES);
                        output.accept(ModBlocks.GOLDEN_BIRCH_SAPLING); output.accept(ModBlocks.WILD_GRAIN);
                        output.accept(ModBlocks.LEAF_LITTER);
                        output.accept(ModBlocks.TARTARUS_CHAIN_BLOCK);
                        output.accept(FORGE_SURVEY);output.accept(CHAIN_RESONATOR);
                        output.accept(ModBlocks.FORGE_ARCHIVE);output.accept(ModBlocks.FORGE_LEDGER);
                        output.accept(ModBlocks.TARTARUS_INSCRIPTION);output.accept(ModBlocks.FORGE_HEARTH);
                        output.accept(ModBlocks.FORGE_SERVICE_LAMP);output.accept(ModBlocks.FORGE_SERVICE_SWITCH);
                        output.accept(ModBlocks.FORGE_PILOT_LIGHT);output.accept(ModBlocks.FORGE_DEBRIS);
                        output.accept(ModBlocks.FORGE_IRON);
                    }).build());

    private ModItems() {}
}
