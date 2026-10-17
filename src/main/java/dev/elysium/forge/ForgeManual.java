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
        pages.add(Filterable.passThrough(Component.literal("THE SMITH'S CHARGE\n\nReturn the First Flame. Feed and repair the runners. A hot branch beneath the hammer's control supplies its work.\n\nPlace a recipe's first material against the control to load a complete batch.")));
        pages.add(Filterable.passThrough(Component.literal("Stand clear of the anvil. Sneak-use the loaded control with an empty hand to release the hammer. Three strokes temper one batch.\n\nThe control holds finished work until collected. Interrupted work waits for heat; it is not lost. A blocked or damaged hammer cannot move.")));
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
