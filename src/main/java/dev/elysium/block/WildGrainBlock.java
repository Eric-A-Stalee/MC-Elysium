package dev.elysium.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Perennial-looking wild grain: mature appearance on ordinary soil, no farmland ticking. */
public final class WildGrainBlock extends BushBlock {
    public static final MapCodec<WildGrainBlock> CODEC = simpleCodec(WildGrainBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 15, 15);

    public WildGrainBlock(Properties properties) { super(properties); }

    @Override
    public MapCodec<WildGrainBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
