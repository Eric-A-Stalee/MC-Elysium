package dev.elysium.forge;

import dev.elysium.Elysium;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** A supplemental Elysium-only offer also reaches existing towns without rewriting their chunks. */
@EventBusSubscriber(modid=Elysium.MOD_ID)
public final class ForgeTradeEvents {
    @SubscribeEvent public static void trade(PlayerInteractEvent.EntityInteract event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||!player.level().dimension().equals(ElysiumTravel.DIMENSION)
                ||!(event.getTarget() instanceof Villager villager)||villager.isBaby())return;
        var profession=villager.getVillagerData().getProfession();
        if(profession!=VillagerProfession.CARTOGRAPHER && profession!=VillagerProfession.LIBRARIAN)return;
        var offers=villager.getOffers();
        if(offers.stream().noneMatch(offer->offer.getResult().is(ModItems.FORGE_SURVEY.get())))
            offers.add(new MerchantOffer(new ItemCost(Items.EMERALD,6),new ItemStack(ModItems.FORGE_SURVEY.get()),12,2,.05f));
    }
    private ForgeTradeEvents(){}
}
