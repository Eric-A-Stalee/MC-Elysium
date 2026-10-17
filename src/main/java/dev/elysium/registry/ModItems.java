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

    public static final DeferredItem<Item> ASHEN_FANG=ITEMS.registerSimpleItem("ashen_fang",new Item.Properties().fireResistant());
    public static final DeferredItem<Item> HUSH_CHARM=ITEMS.registerSimpleItem("hush_charm",new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<Item> BRONZE_PINION=ITEMS.registerSimpleItem("bronze_pinion",new Item.Properties().fireResistant());
    public static final DeferredItem<Item> FIRST_FLAME = ITEMS.registerSimpleItem("first_flame", new Item.Properties().stacksTo(1).fireResistant());
    public static final DeferredItem<Item> CINDER_GLASS = ITEMS.registerSimpleItem("cinder_glass",new Item.Properties().fireResistant());
    public static final DeferredItem<Item> SUNWROUGHT_INGOT = ITEMS.registerSimpleItem("sunwrought_ingot",new Item.Properties().fireResistant());
    public static final DeferredItem<Item> WATCHFIRE_SEAL = ITEMS.registerSimpleItem("watchfire_seal",new Item.Properties().fireResistant());
    public static final DeferredItem<dev.elysium.item.DawnWayfinderItem> DAWN_WAYFINDER = ITEMS.register("dawn_wayfinder",()->new dev.elysium.item.DawnWayfinderItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final DeferredItem<dev.elysium.item.HarvestSickleItem> HARVEST_SICKLE = ITEMS.register("harvest_sickle",()->new dev.elysium.item.HarvestSickleItem(new Item.Properties().durability(768).fireResistant()));
    static {
        ITEMS.registerSimpleBlockItem(ModBlocks.CHARRED_BONE);ITEMS.registerSimpleBlockItem(ModBlocks.EMBER_ROOT);
        ITEMS.registerSimpleBlockItem(ModBlocks.PROMETHEUS_VOICE);
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_PRESS);
        ITEMS.registerSimpleBlockItem(ModBlocks.EMBER_GLASS);
        ITEMS.registerSimpleBlockItem(ModBlocks.WATCHFIRE);
        ITEMS.registerSimpleBlockItem(ModBlocks.CELESTIAL_GOVERNOR);
        ITEMS.registerSimpleBlockItem(ModBlocks.WARD_LAMP,new Item.Properties().fireResistant());
        ITEMS.registerSimpleBlockItem(ModBlocks.KEEPERS_LAMP, new Item.Properties().fireResistant());
        ITEMS.registerSimpleBlockItem(ModBlocks.KEEPER_WORKBENCH);
        ITEMS.registerSimpleBlockItem(ModBlocks.BURNT_CLINKER);
        ITEMS.registerSimpleBlockItem(ModBlocks.CINDER_ASH);
        ITEMS.registerSimpleBlockItem(ModBlocks.SMOULDERING_CLINKER);
        ITEMS.registerSimpleBlockItem(ModBlocks.TARTARUS_COUNTERWEIGHT);
        ITEMS.registerSimpleBlockItem(ModBlocks.TARTARUS_HEART);
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
        ITEMS.registerSimpleBlockItem(ModBlocks.FORGE_EMBER_CHANNEL);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ELYSIUM_TAB = TABS.register("elysium",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.elysium"))
                    .icon(() -> ELYSIUM_SIGIL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ASHEN_FANG);output.accept(HUSH_CHARM);output.accept(ModBlocks.CHARRED_BONE);output.accept(ModBlocks.EMBER_ROOT);
                        output.accept(BRONZE_PINION);output.accept(ModBlocks.PROMETHEUS_VOICE);
                        output.accept(CINDER_GLASS);output.accept(ModBlocks.EMBER_GLASS);output.accept(SUNWROUGHT_INGOT);output.accept(WATCHFIRE_SEAL);output.accept(DAWN_WAYFINDER);output.accept(HARVEST_SICKLE);
                        output.accept(ModBlocks.FORGE_PRESS);output.accept(ModBlocks.WARD_LAMP);output.accept(ModBlocks.WATCHFIRE);output.accept(ModBlocks.CELESTIAL_GOVERNOR);
                        output.accept(FIRST_FLAME);output.accept(ModBlocks.KEEPERS_LAMP);output.accept(ModBlocks.KEEPER_WORKBENCH);
                        output.accept(ModBlocks.BURNT_CLINKER);output.accept(ModBlocks.CINDER_ASH);output.accept(ModBlocks.SMOULDERING_CLINKER);
                        output.accept(ModBlocks.TARTARUS_COUNTERWEIGHT);output.accept(ModBlocks.TARTARUS_HEART);
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
                        output.accept(ModBlocks.FORGE_IRON);output.accept(ModBlocks.FORGE_EMBER_CHANNEL);
                    }).build());

    private ModItems() {}
}
