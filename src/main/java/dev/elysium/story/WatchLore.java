package dev.elysium.story;

import dev.elysium.forge.*;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;

public final class WatchLore {
    private static final String[] TITLES={"The Harvest Watch","The Water Watch","The High Watch"};
    private static final String[][] PAGES={
        {"We kept the hour of ripening. The fields were never meant to hold it forever. Without dusk, nothing is lost; without dawn, nothing truly begins.","When the night returns, fit a Sunwrought Watchfire Seal and bring eight wheat. The watch remembers the life above the roots. A restored fire shelters the traveller from stray cinders."},
        {"We kept the hour of returning water. The river carries yesterday into tomorrow. In the still afternoon it remembers only its banks.","When the night returns, fit a Sunwrought Watchfire Seal and pour a bucket of water. The vessel is returned. The watch remembers the paths that join the realm."},
        {"We kept the hour before dawn. In the last watch we heard the bindings fail below. The smith stopped the sky to keep the wound asleep. Paradise was the mercy of an unfinished repair.","When the night returns, fit a Sunwrought Watchfire Seal and four amethyst shards. Together the watches shelter the living. The moon will wait until the deep bindings hold too. Seek Hunger, Sorrow and Dominion beneath the Eye."}
    };
    public static ItemStack book(int kind) {
        var book=new ItemStack(Items.WRITTEN_BOOK);var pages=java.util.Arrays.stream(PAGES[kind]).map(s->Filterable.<Component>passThrough(Component.literal(s))).toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(TITLES[kind]),"The Last Watch",0,pages,true));return book;
    }
    public static void give(ServerPlayer player,int kind){if(ForgeDiscoveries.get(player.serverLevel()).discover(player.getUUID(),128<<kind))ForgeLore.give(player,book(kind));}
    private WatchLore(){}
}
