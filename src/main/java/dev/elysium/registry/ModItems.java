package dev.elysium.registry;

import dev.elysium.Elysium;
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

    static {
        ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_BIRCH_LEAVES);
        ITEMS.registerSimpleBlockItem(ModBlocks.GOLDEN_BIRCH_SAPLING);
        ITEMS.registerSimpleBlockItem(ModBlocks.WILD_GRAIN);
        ITEMS.registerSimpleBlockItem(ModBlocks.HARVEST_ALTAR);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELYSIUM_TAB = TABS.register("elysium",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.elysium"))
                    .icon(() -> ELYSIUM_SIGIL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ELYSIUM_SHARD); output.accept(ELYSIUM_FRAGMENT); output.accept(ELYSIUM_SIGIL);
                        output.accept(ModBlocks.HARVEST_ALTAR); output.accept(ModBlocks.GOLDEN_BIRCH_LEAVES);
                        output.accept(ModBlocks.GOLDEN_BIRCH_SAPLING); output.accept(ModBlocks.WILD_GRAIN);
                    }).build());

    private ModItems() {}
}
