package dev.elysium.block;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ForgePressBlock extends BaseEntityBlock {
    public static final MapCodec<ForgePressBlock> CODEC=simpleCodec(ForgePressBlock::new);
    public ForgePressBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ForgePressBlockEntity(pos,state);}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack) {
        if(level.getBlockEntity(pos) instanceof ForgePressBlockEntity press&&level.getBlockEntity(pos.below()) instanceof ForgeServiceBlockEntity feed&&feed.plan()!=null) {
            var p=feed.plan();
            if(dev.elysium.forge.ForgeHammer.world(p,-9,1,76).equals(pos))press.bind(p);
        }
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.FORGE_PRESS.get(),ForgePressBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ForgePressBlockEntity press)press.use(player,stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof ForgePressBlockEntity press)press.use(player,ItemStack.EMPTY);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock())&&level.getBlockEntity(pos) instanceof ForgePressBlockEntity press)press.releaseContents();
        super.onRemove(state,level,pos,next,moving);
    }
}
