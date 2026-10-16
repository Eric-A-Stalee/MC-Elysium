package dev.elysium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Portable placed light; no forced chunk ticking or dynamic-light dependency. */
public final class KeepersLampBlock extends Block {
    private static final VoxelShape SHAPE=box(4,0,4,12,13,12);
    public KeepersLampBlock(Properties properties){super(properties);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
}
