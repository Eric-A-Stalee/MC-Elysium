package dev.elysium.block;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class WatchfireBlock extends BaseEntityBlock {
    public static final MapCodec<WatchfireBlock> CODEC=simpleCodec(WatchfireBlock::new);
    public static final IntegerProperty WATCH=IntegerProperty.create("watch",0,2),EMBER=IntegerProperty.create("ember",0,2);
    public WatchfireBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(WATCH,0).setValue(EMBER,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(WATCH,EMBER);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new WatchfireBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,ModBlockEntities.WATCHFIRE.get(),WatchfireBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(pos) instanceof WatchfireBlockEntity fire)fire.use(p,stack);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos pos,Player p,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(pos) instanceof WatchfireBlockEntity fire)fire.use(p,ItemStack.EMPTY);return InteractionResult.sidedSuccess(l.isClientSide);}
}
