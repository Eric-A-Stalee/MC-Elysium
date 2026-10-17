package dev.elysium.story;

import dev.elysium.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** A small surveyed wound. Never excavates an inventory or an unfamiliar player-built material. */
public record RupturePlan(int x,int z,int[] heights) {
    public static final int RADIUS=10,WIDTH=21;
    public RupturePlan {if(heights.length!=WIDTH*WIDTH)throw new IllegalArgumentException("Invalid rupture survey");heights=heights.clone();}
    @Override public int[] heights(){return heights.clone();}
    public int ground(int u,int v){return heights[(u+RADIUS)*WIDTH+v+RADIUS];}
    public BlockPos core(){return new BlockPos(x,ground(0,0)-5,z);}
    public boolean loaded(ServerLevel l){for(int cx=(x-RADIUS)>>4;cx<=(x+RADIUS)>>4;cx++)for(int cz=(z-RADIUS)>>4;cz<=(z+RADIUS)>>4;cz++)if(l.getChunkSource().getChunkNow(cx,cz)==null)return false;return true;}
    public static boolean natural(BlockState s){
        return s.isAir()||s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)||s.is(BlockTags.FLOWERS)
            ||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY)||s.is(Blocks.SHORT_GRASS)||s.is(Blocks.TALL_GRASS)||s.is(Blocks.FERN)||s.is(Blocks.LARGE_FERN)||s.is(Blocks.SNOW)
            ||s.is(Blocks.CALCITE)||s.is(ModBlocks.LEAF_LITTER.get())||s.is(ModBlocks.WILD_GRAIN.get());
    }
    public static Optional<RupturePlan> survey(ServerLevel l,int x,int z){
        int[] heights=new int[WIDTH*WIDTH];int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
        for(int u=-RADIUS;u<=RADIUS;u++)for(int v=-RADIUS;v<=RADIUS;v++){
            var chunk=l.getChunkSource().getChunkNow((x+u)>>4,(z+v)>>4);if(chunk==null)return Optional.empty();
            int y=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(x+u)&15,(z+v)&15);heights[(u+RADIUS)*WIDTH+v+RADIUS]=y;lo=Math.min(lo,y);hi=Math.max(hi,y);
            var p=new BlockPos(x+u,y,z+v);if(y<l.getSeaLevel()+1||!l.canSeeSky(p.above())||Nightfall.protectedAt(l,p))return Optional.empty();
            for(int h=-6;h<=6;h++){var q=p.above(h);var s=chunk.getBlockState(q);if(s.hasBlockEntity()||!s.getFluidState().isEmpty()||!natural(s))return Optional.empty();}
        }
        return hi-lo>10?Optional.empty():Optional.of(new RupturePlan(x,z,heights));
    }
    public BlockState material(int u,int y,int v){
        double r=Math.hypot(u,v),crack=Math.abs(v-2*Math.sin(u*.55));int surface=ground(u,v);
        int depth=Math.max(0,(int)(5*(1-r/10)));if(crack<1.8&&Math.abs(u)<9)depth=Math.max(depth,3);
        int floor=surface-depth;
        if(r<9.5&&y>=floor&&y<=surface){if(y>floor)return Blocks.AIR.defaultBlockState();return (crack<1.8?ModBlocks.SMOULDERING_CLINKER.get():ModBlocks.CINDER_ASH.get()).defaultBlockState();}
        if(r>=6&&r<10&&Math.floorMod(u*7+v*13,19)<3&&y>surface&&y<=surface+2+Math.floorMod(u+v,4))return (y==surface+1?ModBlocks.FORGE_IRON.get():Blocks.BASALT).defaultBlockState();
        return null;
    }
    /** Revalidate each write after the warning, so placing a chest during it cannot sacrifice the chest. */
    public void carve(ServerLevel l){
        if(!loaded(l))return;
        for(int u=-RADIUS;u<=RADIUS;u++)for(int v=-RADIUS;v<=RADIUS;v++)for(int y=ground(u,v)-5;y<=ground(u,v)+6;y++){
            var p=new BlockPos(x+u,y,z+v);if(p.equals(core()))continue;var s=material(u,y,v);
            if(s!=null&&natural(l.getBlockState(p))&&!l.getBlockState(p).hasBlockEntity())l.setBlock(p,s,3);
        }
    }
}
