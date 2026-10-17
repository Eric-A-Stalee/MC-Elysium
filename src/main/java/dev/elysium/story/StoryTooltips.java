package dev.elysium.story;

import dev.elysium.Elysium;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Compact reminders; the smith's book retains the full bills and deliberate world choice. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class StoryTooltips {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack=event.getItemStack();String key=null;
        if(stack.is(ModItems.WATCHFIRE_SEAL.get()))key="watchfire_seal";
        else if(stack.is(ModItems.SUNWROUGHT_INGOT.get()))key="sunwrought_ingot";
        else if(stack.is(ModItems.CINDER_GLASS.get()))key="cinder_glass";
        else if(stack.is(ModBlocks.WARD_LAMP.get().asItem()))key="ward_lamp";
        else if(stack.is(ModBlocks.EMBER_GLASS.get().asItem()))key="ember_glass";
        if(key!=null)event.getToolTip().add(Component.translatable("tooltip.elysium."+key).withStyle(ChatFormatting.GRAY));
    }
    private StoryTooltips(){}
}
