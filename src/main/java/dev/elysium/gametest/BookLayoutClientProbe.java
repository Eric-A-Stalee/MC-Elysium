package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.Elysium;
import dev.elysium.forge.ForgeLore;
import dev.elysium.forge.ForgeManual;
import dev.elysium.story.WatchLore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Measures the actual generated books with the loaded vanilla client font; never shipped. */
@EventBusSubscriber(modid=Elysium.MOD_ID,value=Dist.CLIENT)
public final class BookLayoutClientProbe {
    // BookViewScreen's 1.21.1 text rectangle is 114 x 128, with 9-pixel lines.
    private static final int WIDTH=114, MAX_LINES=128/9;
    private static boolean checked;
    private static int screenshotTicks;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        if(!Boolean.getBoolean("elysium.bookLayout"))return;
        var mc=Minecraft.getInstance();
        if(!checked) {
            if(!(mc.screen instanceof TitleScreen)||mc.getOverlay()!=null)return;
            checked=true;
            var report=new StringBuilder("Minecraft 1.21.1; English; default font; 114 x 128 book text area\n");
            var failures=new ArrayList<String>();
            String clipped="Do not mistake shelter for victory. The old watches can keep a home warm. They cannot recall what escapes.\n\nIn the deep world, seek the bindings of Hunger, Sorrow and Dominion. Take light. Leave a way back.\n\nI will not call this a gift without a cost.";
            int oldLines=mc.font.split(Component.literal(clipped),WIDTH).size();
            if(oldLines!=15)failures.add("Reported regression should reproduce as 15 lines; got "+oldLines);
            report.append("Original Prometheus page 5: ").append(oldLines).append(" lines (overflow reproduced)\n\n");
            var books=new ArrayList<ItemStack>();
            for(var record:ForgeLore.Record.values())books.add(ForgeLore.book(record));
            for(int kind=0;kind<3;kind++)books.add(WatchLore.book(kind));
            books.add(ForgeManual.book());
            int pages=0;
            for(var book:books)pages+=verify(mc,book,report,failures);
            var lesson=ForgeLore.book(ForgeLore.Record.PROMETHEUS);
            var last=lesson.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().getLast().raw().getString();
            if(!last.endsWith("I will not call this a gift without a cost."))failures.add("Prometheus's final sentence must remain complete");
            report.append("\nChecked ").append(books.size()).append(" books / ").append(pages).append(" pages.\n");
            for(var failure:failures)report.append("FAIL: ").append(failure).append('\n');
            Files.writeString(mc.gameDirectory.toPath().resolve("book-layout.txt"),report);
            if(!failures.isEmpty())throw new IllegalStateException(String.join("; ",failures));
            LogUtils.getLogger().info("ELYSIUM_BOOK_LAYOUT_PASSED books={} pages={}",books.size(),pages);
            var screen=new BookViewScreen(BookViewScreen.BookAccess.fromItem(lesson));
            mc.setScreen(screen);
            screen.setPage(4);
        } else if(++screenshotTicks==20) {
            Screenshot.grab(mc.gameDirectory,"prometheus-page-5.png",mc.getMainRenderTarget(),
                    message->LogUtils.getLogger().info("Book capture: {}",message.getString()));
        } else if(screenshotTicks==60) {
            LogUtils.getLogger().info("ELYSIUM_BOOK_LAYOUT_COMPLETE");
            mc.stop();
        }
    }

    private static int verify(Minecraft mc,ItemStack book,StringBuilder report,List<String> failures) {
        var content=book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if(content==null)throw new IllegalStateException("Expected a written book");
        String title=content.title().raw();
        int index=0;
        for(var page:content.pages()) {
            int lines=mc.font.split(page.raw(),WIDTH).size();
            String label=title+" page "+(++index)+"/"+content.pages().size();
            report.append(label).append(": ").append(lines).append(" lines\n").append(page.raw().getString()).append("\n\n");
            if(lines>MAX_LINES)failures.add(label+" needs "+lines+" lines; only "+MAX_LINES+" are visible");
            if(page.raw().getString().contains("lore.elysium."))failures.add(label+" contains an untranslated key");
        }
        return index;
    }
    private BookLayoutClientProbe() {}
}
