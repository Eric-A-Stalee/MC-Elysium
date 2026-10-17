package dev.elysium.block;

import dev.elysium.forge.*;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModItems;
import dev.elysium.story.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The answer to the night is chosen at a working original shrine, not by consuming a remote menu item. */
public final class CelestialGovernorBlock extends Block {
    public CelestialGovernorBlock(Properties p){super(p);}
    public boolean choose(ServerPlayer player,BlockPos pos,ItemStack held,boolean deliberate) {
        var level=player.serverLevel();if(!level.dimension().equals(ElysiumTravel.DIMENSION)||!deliberate)return false;
        var feed=ForgeLighting.loadedService(level,pos.below());if(feed==null||feed.plan()==null)return false;
        var plan=feed.plan();if(!ForgeHammer.world(plan,0,1,110).equals(pos))return false;
        var source=ForgeLighting.loadedService(level,dev.elysium.structure.ForgeFixtures.source(plan));
        if(source==null||!source.burning()||!source.restored().contains(pos.below()))return false;
        boolean living=held.is(Items.CLOCK);if(!living&&!held.is(ModItems.ELYSIUM_FRAGMENT.get()))return false;
        var cycle=RealmCycle.get(level);if(!cycle.resolve(living))return false;
        RealmClock.announce(level,living?"release":"golden");ForgeLore.award(player,living?"living_realm":"chosen_hour");return true;
    }
    private void explain(Player player){player.displayClientMessage(Component.translatable("message.elysium.story.governor"),false);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide)explain(player);return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof ServerPlayer sp&&!choose(sp,pos,stack,player.isShiftKeyDown()))explain(player);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
}
