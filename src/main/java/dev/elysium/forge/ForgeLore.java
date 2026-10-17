package dev.elysium.forge;

import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

public final class ForgeLore {
    public enum Record {
        PROMETHEUS("What Fire Remembers",ForgeDiscoveries.PROMETHEUS,5),
        SURVEY("The Last Survey",ForgeDiscoveries.SURVEY,4),
        LEDGER("The Unfinished Work",ForgeDiscoveries.LEDGER,5),
        BINDINGS("On the Bindings",ForgeDiscoveries.BINDINGS,4),
        ANCHOR("A Voice in the Iron",ForgeDiscoveries.ANCHOR,3);
        public final String title;
        public final int flag,pages;
        Record(String title,int flag,int pages){this.title=title;this.flag=flag;this.pages=pages;}
        public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static ItemStack book(Record record) {
        var book=new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages=IntStream.rangeClosed(1,record.pages)
                .mapToObj(n->Filterable.<Component>passThrough(Component.translatable("lore.elysium."+record.key()+"."+n))).toList();
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(Filterable.passThrough(record.title),record==Record.PROMETHEUS?"Prometheus":"The Keepers of the Hearth",0,pages,true));
        return book;
    }
    public static void give(ServerPlayer player,ItemStack stack) {
        if(!player.addItem(stack)) player.drop(stack,false);
    }
    public static void award(ServerPlayer player,String id) {
        var advancement=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("elysium",id));
        if(advancement!=null) player.getAdvancements().award(advancement,"discovered");
    }
    public static void tell(ServerPlayer player,String key,Object...args) {
        player.displayClientMessage(Component.translatable("message.elysium.forge."+key,args),false);
    }
    private ForgeLore(){}
}
