package dev.elysium.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Block;
import java.util.List;

/** Three-by-three selective harvest. Vanilla destruction hooks/loot apply; seeds pay for replanting. */
public final class HarvestSickleItem extends Item {
    public HarvestSickleItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var center=context.getClickedPos();
        if(player==null||!(level.getBlockState(center).getBlock() instanceof CropBlock))return InteractionResult.PASS;
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        boolean harvested=false;
        for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1))) {
            if(!server.hasChunkAt(p)||!server.mayInteract(player,p)||!player.mayUseItemAt(p,context.getClickedFace(),context.getItemInHand()))continue;
            var state=server.getBlockState(p);
            if(!(state.getBlock() instanceof CropBlock crop)||!crop.isMaxAge(state))continue;
            // The normal server break path honors protection/event cancellation and wheat shard loot.
            if(!(player instanceof net.minecraft.server.level.ServerPlayer sp)||!sp.gameMode.destroyBlock(p.immutable()))continue;
            harvested=true;
            Item seed=state.getBlock()==net.minecraft.world.level.block.Blocks.WHEAT?Items.WHEAT_SEEDS:
                state.getBlock()==net.minecraft.world.level.block.Blocks.CARROTS?Items.CARROT:
                state.getBlock()==net.minecraft.world.level.block.Blocks.POTATOES?Items.POTATO:
                state.getBlock()==net.minecraft.world.level.block.Blocks.BEETROOTS?Items.BEETROOT_SEEDS:null;
            if(seed!=null&&server.getBlockState(p).isAir()&&crop.defaultBlockState().canSurvive(server,p)) {
                for(int i=0;i<player.getInventory().getContainerSize();i++) {
                    var stack=player.getInventory().getItem(i);
                    if(stack.is(seed)){stack.shrink(1);server.setBlock(p,crop.defaultBlockState(),3);break;}
                }
            }
            context.getItemInHand().hurtAndBreak(1,player,net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
            if(context.getItemInHand().isEmpty())break;
        }
        return harvested?InteractionResult.CONSUME:InteractionResult.PASS;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.elysium.harvest_sickle"));
    }
}
