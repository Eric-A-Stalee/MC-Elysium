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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The glowing seam in a restraint carries the buried titan's voice. */
public final class PrometheusBlock extends BaseEntityBlock {
    public static final MapCodec<PrometheusBlock> CODEC=simpleCodec(PrometheusBlock::new);
    public PrometheusBlock(Properties p){super(p);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PrometheusBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,ModBlockEntities.PROMETHEUS.get(),PrometheusBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof PrometheusBlockEntity prison)prison.use(player,stack);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof PrometheusBlockEntity prison)prison.use(player,ItemStack.EMPTY);return InteractionResult.sidedSuccess(l.isClientSide);}
}
