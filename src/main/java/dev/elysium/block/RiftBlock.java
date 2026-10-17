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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class RiftBlock extends BaseEntityBlock {
    public static final MapCodec<RiftBlock> CODEC=simpleCodec(RiftBlock::new);
    public static final BooleanProperty SEALED=BooleanProperty.create("sealed");
    public RiftBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(SEALED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(SEALED);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new RiftBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ModBlockEntities.SURFACE_RIFT.get(),RiftBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack item,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof RiftBlockEntity r)r.use(player,item);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof RiftBlockEntity r)r.use(player,ItemStack.EMPTY);return InteractionResult.sidedSuccess(l.isClientSide);}
}
