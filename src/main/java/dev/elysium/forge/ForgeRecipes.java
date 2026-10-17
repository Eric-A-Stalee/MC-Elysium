package dev.elysium.forge;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** One catalog serves the press, its material checks, and the in-world recipe book. */
public final class ForgeRecipes {
    public record Cost(Item item, int count) {}
    public record Work(String id, Item selector, List<Cost> costs, Item result, int count) {
        public ItemStack output() { return new ItemStack(result, count); }
    }
    public static List<Work> all() {
        return List.of(
            new Work("binding_nail",ModItems.BRONZE_PINION.get(),List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),2),new Cost(ModItems.BRONZE_PINION.get(),1),new Cost(ModItems.CINDER_GLASS.get(),2)),ModItems.BINDING_NAIL.get(),4),
            new Work("hush_charm",ModItems.ASHEN_FANG.get(),List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),2),new Cost(ModItems.ASHEN_FANG.get(),6)),ModItems.HUSH_CHARM.get(),1),
            new Work("sunwrought", Items.IRON_INGOT, List.of(new Cost(Items.IRON_INGOT,3),new Cost(Items.COPPER_INGOT,2),new Cost(Items.GOLD_INGOT,1)), ModItems.SUNWROUGHT_INGOT.get(),4),
            new Work("watchfire_seal", Items.AMETHYST_SHARD, List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),2),new Cost(Items.AMETHYST_SHARD,1)), ModItems.WATCHFIRE_SEAL.get(),1),
            new Work("wayfinder", Items.COMPASS, List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),2),new Cost(Items.COMPASS,1),new Cost(Items.AMETHYST_SHARD,2)), ModItems.DAWN_WAYFINDER.get(),1),
            new Work("harvest_sickle", Items.IRON_HOE, List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),2),new Cost(Items.IRON_HOE,1)), ModItems.HARVEST_SICKLE.get(),1),
            new Work("ember_glass", Items.GLASS, List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),1),new Cost(Items.GLASS,4)), ModBlocks.EMBER_GLASS.get().asItem(),4),
            new Work("ward_lamp", ModBlocks.KEEPERS_LAMP.get().asItem(), List.of(new Cost(ModItems.SUNWROUGHT_INGOT.get(),2),new Cost(ModBlocks.KEEPERS_LAMP.get().asItem(),1),new Cost(Items.AMETHYST_SHARD,1)), ModBlocks.WARD_LAMP.get().asItem(),1));
    }
    public static Work byId(String id) { return all().stream().filter(w->w.id().equals(id)).findFirst().orElse(null); }
    public static Work select(ItemStack held) { return all().stream().filter(w->held.is(w.selector())).findFirst().orElse(null); }
    private ForgeRecipes() {}
}
