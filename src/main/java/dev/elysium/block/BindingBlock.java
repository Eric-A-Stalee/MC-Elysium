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
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** A restraint, not a portal. Each carved face names what it holds. */
public final class BindingBlock extends BaseEntityBlock {
    public static final MapCodec<BindingBlock> CODEC=simpleCodec(BindingBlock::new);
    public static final IntegerProperty KIND=IntegerProperty.create("kind",0,2);
    public static final BooleanProperty SEALED=BooleanProperty.create("sealed");
    public BindingBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(KIND,0).setValue(SEALED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(KIND,SEALED);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new BindingBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,ModBlockEntities.DEEP_BINDING.get(),BindingBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof BindingBlockEntity core)core.use(player,stack);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){if(!l.isClientSide&&l.getBlockEntity(p) instanceof BindingBlockEntity core)core.use(player,ItemStack.EMPTY);return InteractionResult.sidedSuccess(l.isClientSide);}
}
