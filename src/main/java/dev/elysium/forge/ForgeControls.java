package dev.elysium.forge;

import dev.elysium.Elysium;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Vanilla normally skips block use while sneaking with an item, including an offhand lamp. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class ForgeControls {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void deliberateUse(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getEntity().isShiftKeyDown()||event.getUseBlock()==TriState.FALSE)return;
        var state=event.getLevel().getBlockState(event.getPos());var held=event.getItemStack();
        if(state.is(ModBlocks.CELESTIAL_GOVERNOR.get())&&(held.is(Items.CLOCK)||held.is(ModItems.ELYSIUM_FRAGMENT.get()))
                ||state.is(ModBlocks.FORGE_PRESS.get())&&held.isEmpty())event.setUseBlock(TriState.TRUE);
    }
    private ForgeControls(){}
}
