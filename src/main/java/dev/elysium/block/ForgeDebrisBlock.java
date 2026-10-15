package dev.elysium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low chips, ash and slag preserve the scale of decay between full-block rockfalls. */
public final class ForgeDebrisBlock extends Block {
    public static final IntegerProperty KIND = IntegerProperty.create("kind", 0, 3);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 3, 15);
    public ForgeDebrisBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(KIND, 0)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(KIND); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
