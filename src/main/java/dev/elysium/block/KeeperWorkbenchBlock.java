package dev.elysium.block;

import dev.elysium.forge.ForgeLore;
import dev.elysium.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Restoration reward: a supplied workshop can assemble a recoverable expedition lamp. */
public final class KeeperWorkbenchBlock extends Block {
    public KeeperWorkbenchBlock(Properties properties){super(properties);}
    public static boolean supplied(Level level,BlockPos pos) {
        for(var p:BlockPos.betweenClosed(pos.offset(-6,-2,-6),pos.offset(6,3,6)))
            if(level.hasChunkAt(p)) {
                var s=level.getBlockState(p);
                if(s.is(ModBlocks.FORGE_SERVICE_LAMP.get())&&s.getValue(ForgeServiceBlock.LIT))return true;
            }
        return false;
    }
    public static boolean craft(Level level,BlockPos pos,Player player,ItemStack held) {
        if(!held.is(Items.LANTERN)||!supplied(level,pos))return false;
        int copper=-1,crystal=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++) {
            var stack=player.getInventory().getItem(i);
            if(stack.is(Items.COPPER_INGOT))copper=i;
            if(stack.is(Items.AMETHYST_SHARD))crystal=i;
        }
        if(!player.getAbilities().instabuild&&(copper<0||crystal<0))return false;
        if(!player.getAbilities().instabuild) {
            held.shrink(1);player.getInventory().getItem(copper).shrink(1);player.getInventory().getItem(crystal).shrink(1);
        }
        if(player instanceof ServerPlayer server) {
            ForgeLore.give(server,new ItemStack(ModBlocks.KEEPERS_LAMP.get()));
            ForgeLore.award(server,"keepers_lamp");
        }
        return true;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide) {
            boolean made=craft(level,pos,player,stack);
            player.displayClientMessage(Component.translatable("message.elysium.lamp."+(made?"crafted":supplied(level,pos)?"materials":"cold")),false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.elysium.lamp."+(supplied(level,pos)?"materials":"cold")),false);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
