package dev.elysium.block;

import dev.elysium.entity.SlagChimeraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** A small real eye/held-lamp light; expires without a living source. */
public final class ChimeraGlowBlock extends Block {
    public ChimeraGlowBlock(Properties properties){super(properties);}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){level.scheduleTick(pos,this,20);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        boolean source=level.getEntitiesOfClass(SlagChimeraEntity.class,new AABB(pos).inflate(18),e->e.isAlive()&&e.eyeCenter(0).distanceToSqr(pos.getCenter())<5).size()>0;
        if(!source)source=level.players().stream().anyMatch(p->dev.elysium.forge.KeeperLampLight.carrying(p)&&p.getEyePosition().distanceToSqr(pos.getCenter())<5);
        if(!source)level.removeBlock(pos,false);else level.scheduleTick(pos,this,20);
    }
}
