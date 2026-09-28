package dev.elysium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A quiet, non-ticking ground detail. It needs soil and has no collision. */
public final class LeafLitterBlock extends BushBlock {
    public static final MapCodec<LeafLitterBlock> CODEC = simpleCodec(LeafLitterBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    public LeafLitterBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override protected MapCodec<LeafLitterBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
