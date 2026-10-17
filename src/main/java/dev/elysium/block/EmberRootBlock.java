package dev.elysium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Sparse, low amber life in the ash. It supplies a bearing, not room-filling illumination. */
public final class EmberRootBlock extends Block {
    private static final VoxelShape SHAPE=box(3,0,3,13,13,13);
    public EmberRootBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){if(r.nextInt(12)==0)l.addParticle(ParticleTypes.SMALL_FLAME,p.getX()+.5,p.getY()+.65,p.getZ()+.5,0,.003,0);}
    @Override protected BlockState updateShape(BlockState s,Direction side,BlockState other,LevelAccessor l,BlockPos p,BlockPos q){return side==Direction.DOWN&&!other.isSolid()?Blocks.AIR.defaultBlockState():super.updateShape(s,side,other,l,p,q);}
}
