package dev.elysium.block;

import dev.elysium.portal.ElysiumTravel;
import dev.elysium.story.RealmCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WardLampBlock extends KeepersLampBlock {
    public WardLampBlock(Properties p){super(p);}
    @Override protected void onPlace(BlockState s,Level l,BlockPos pos,BlockState old,boolean moving){super.onPlace(s,l,pos,old,moving);if(l instanceof ServerLevel server&&l.dimension().equals(ElysiumTravel.DIMENSION))RealmCycle.get(server).addWard(pos);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos pos,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l instanceof ServerLevel server&&l.dimension().equals(ElysiumTravel.DIMENSION))RealmCycle.get(server).removeWard(pos);super.onRemove(s,l,pos,next,moving);}
}
