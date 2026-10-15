package dev.elysium.forge;

import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.structure.ForgeFixtures;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Bounded flood through existing, loaded, intact blocks. No address-list activation or chunk tickets. */
public final class ForgeLighting {
    public static ForgeServiceBlockEntity loadedService(ServerLevel level,BlockPos pos) {
        var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        return chunk!=null&&chunk.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service?service:null;
    }
    public static void advance(ServerLevel level,ForgeServiceBlockEntity source,int elapsed) {
        var p=source.plan();if(p==null)return;
        Set<BlockPos> reachable=new HashSet<>();
        if(source.burning()) {
            var queue=new ArrayDeque<BlockPos>();queue.add(source.getBlockPos());
            while(!queue.isEmpty()) {
                var pos=queue.remove();if(reachable.contains(pos))continue;
                var service=loadedService(level,pos);
                if(service==null||!service.belongsTo(p)||service.getBlockState().getValue(ForgeServiceBlock.DECAY)!=0
                        ||ForgeFixtures.at(p,pos)==null)continue;
                reachable.add(pos);
                for(var d:Direction.values())if(ForgeFixtures.at(p,pos.relative(d))!=null)queue.add(pos.relative(d));
            }
        }
        var previous=new HashSet<>(source.energized());
        var next=new HashSet<>(previous);next.retainAll(reachable);
        if(!reachable.isEmpty())next.add(source.getBlockPos());
        // Six blocks per second: a branch catches from its burning neighbour, including after repair.
        for(int wave=0;wave<3;wave++) {
            var additions=new HashSet<BlockPos>();
            for(var pos:next)for(var d:Direction.values())if(reachable.contains(pos.relative(d)))additions.add(pos.relative(d));
            next.addAll(additions);
        }
        source.setEnergized(next);
        previous.addAll(next);
        for(var pos:previous) {
            var service=loadedService(level,pos);
            if(service!=null&&service.belongsTo(p))service.reconcile(level);
        }
        source.consume(elapsed);
    }
    private ForgeLighting() {}
}
