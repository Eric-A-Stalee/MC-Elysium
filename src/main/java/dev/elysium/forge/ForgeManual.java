package dev.elysium.forge;

import java.util.ArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;

public final class ForgeManual {
    public static ItemStack book() {
        var pages=new ArrayList<Filterable<Component>>();
        pages.add(Filterable.passThrough(Component.literal("THE SMITH'S CHARGE\n\nThe first completed work opens the old vessel. The sun will fall; things below will rise. This is a choice. Paradise may remain as it is.\n\nReturning the First Flame alone does not move the sky.")));
        pages.add(Filterable.passThrough(Component.literal("LOADING THE WORKS\n\nRepair and heat the runner beneath the control. Offer a recipe's selector to load its entire bill.\n\nStand clear of the anvil. Sneak-use an empty hand to begin. Three strokes temper a batch. Use again to collect.")));
        pages.add(Filterable.passThrough(Component.literal("INTERRUPTED WORK\n\nSneak-use the control to pause or resume. Lost heat pauses the work too. Materials remain in the control.\n\nA blocked or damaged hammer will not move. Inspect the head and its path; the machine never clears your blocks.")));
        pages.add(Filterable.passThrough(Component.literal("THE FIRST NIGHT\n\nForge a Dawn Wayfinder, three Watchfire Seals and a Ward Lamp. The wayfinder follows the unanswered harvest, water and mountain watches.\n\nSneak-use it to recall this Forge. These fires shelter travellers. They cannot repair Tartarus.")));
        pages.add(Filterable.passThrough(Component.literal("CHOOSING THE SKY\n\nWhen the watches AND the deep bindings hold, heat the rear shrine. At its governor, sneak-use a clock for a living cycle, or an Elysium Fragment for lasting golden hour.\n\nNeither is consumed. The repaired works let you choose again.")));
        pages.add(Filterable.passThrough(Component.literal("THE DEEP REPAIR\n\nTake Hope from the opened vessel. Forge Binding Nails. Below the Eye, your Chain Resonator seeks Hunger, Sorrow and Dominion.\n\nAn offering stills Hunger. Quiet light reaches Sorrow. Dominion has a warden.")));
        for(var recipe:ForgeRecipes.all()) {
            var page=Component.empty().append(recipe.output().getHoverName()).append(" × "+recipe.count()+"\n\nLoad with: ").append(recipe.selector().getDescription()).append("\n\n");
            for(var cost:recipe.costs())page.append(cost.count()+" × ").append(cost.item().getDescription()).append("\n");
            pages.add(Filterable.passThrough(page));
        }
        var book=new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough("The Smith's Charge"),"The Last Smith",0,pages,true));
        return book;
    }
    public static void give(ServerPlayer player) {
        if(ForgeDiscoveries.get(player.serverLevel()).discover(player.getUUID(),32))ForgeLore.give(player,book());
    }
    private ForgeManual() {}
}
