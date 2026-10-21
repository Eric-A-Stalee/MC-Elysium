package dev.elysium.story;

import dev.elysium.forge.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.WrittenBookContent;

public final class WatchLore {
    private static final String[] TITLES={"The Harvest Watch","The Water Watch","The High Watch"};
    public static ItemStack book(int kind) {
        var book=new ItemStack(Items.WRITTEN_BOOK);
        var pages=java.util.stream.IntStream.rangeClosed(1,2)
                .mapToObj(page->Filterable.<Component>passThrough(Component.translatable("lore.elysium.watch."+kind+"."+page))).toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(TITLES[kind]),"Watch Records",0,pages,true));return book;
    }
    public static void give(ServerPlayer player,int kind){if(ForgeDiscoveries.get(player.serverLevel()).discover(player.getUUID(),128<<kind))ForgeLore.give(player,book(kind));}
    private WatchLore(){}
}
