package dev.elysium.structure;

import dev.elysium.block.ForgeDebrisBlock;
import dev.elysium.registry.ModBlocks;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** A surveyed maintenance route wraps an abandoned hauling shaft, then reaches the high overlook. */
public final class ForgeDescent {
    public static final int DROP=72;
    public record Step(int u,int h,int v,Direction uphill,boolean stair) {}
    public record Sample(Step step,int distance) {}
    public static final List<Step> ROUTE;
    private static final Map<Long,List<Sample>> COLUMNS;
    static {
        var steps=new ArrayList<Step>();
        int[] points={0,72,7, 0,56,23, -14,50,23, -14,38,9, 14,26,9, 14,14,30,
                14,12,42, 0,12,42, 0,12,50, -24,8,50, -24,8,64};
        for(int i=3;i<points.length;i+=3) {
            int u=points[i-3],h=points[i-2],v=points[i-1],tu=points[i],th=points[i+1],tv=points[i+2];
            int length=Math.abs(tu-u)+Math.abs(tv-v);
            var uphill=tu>u?Direction.WEST:tu<u?Direction.EAST:tv>v?Direction.NORTH:Direction.SOUTH;
            for(int n=0;n<length;n++) {
                int height=h+(int)Math.round((th-h)*(double)n/length);
                int next=h+(int)Math.round((th-h)*(double)(n+1)/length);
                steps.add(new Step(u+Integer.signum(tu-u)*n,height,v+Integer.signum(tv-v)*n,uphill,next<height));
            }
        }
        steps.add(new Step(-24,8,64,Direction.NORTH,false));ROUTE=List.copyOf(steps);
        var map=new HashMap<Long,List<Sample>>();
        for(var step:ROUTE)for(int du=-2;du<=2;du++)for(int dv=-2;dv<=2;dv++)
            map.computeIfAbsent(key(step.u()+du,step.v()+dv),k->new ArrayList<>()).add(new Sample(step,Math.max(Math.abs(du),Math.abs(dv))));
        COLUMNS=Map.copyOf(map);
    }
    private static long key(int u,int v) { return (long)u<<32|(v&0xffffffffL); }
    public static int roof(int u,int v) {
        int roof=Integer.MIN_VALUE;
        for(var sample:COLUMNS.getOrDefault(key(u,v),List.of()))roof=Math.max(roof,sample.step.h()+6);
        if(Math.abs(u)<=12&&v>=12&&v<=28)roof=Math.max(roof,52);
        return roof;
    }
    public static BlockState at(ForgePlan plan,int u,int h,int v) {
        Sample best=null;
        for(var sample:COLUMNS.getOrDefault(key(u,v),List.of())) {
            int e=h-sample.step.h();if(e< -2||e>6)continue;
            if(best==null||sample.distance<best.distance||sample.distance==best.distance&&Math.abs(e)<Math.abs(h-best.step.h()))best=sample;
        }
        if(best!=null) {
            var step=best.step;int e=h-step.h();
            if(e<=0) {
                // The final route enters the hall on a substantial masonry balcony.
                if(e==0&&step.stair&&best.distance<=1) {
                    var d=step.uphill;for(int i=0;i<plan.turn();i++)d=d.getClockWise();
                    return Blocks.COBBLED_DEEPSLATE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,d);
                }
                return block(v>=44?Blocks.DEEPSLATE_TILES:Math.floorMod(u+v,9)==0?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.TUFF);
            }
            if(v>=44) {
                if(best.distance==2&&e==1&&v<58&&Math.floorMod(u+v,7)!=0)return block(Blocks.POLISHED_DEEPSLATE_WALL);
                if(e<=4)return block(Blocks.CAVE_AIR);
                return plan.hallRoof(u,v)>=plan.floor()+h?null:block(Blocks.STONE);
            }
            if(e>=5)return block(Math.floorMod(u+v+h,11)<3?Blocks.DRIPSTONE_BLOCK:Blocks.STONE);
            // A readable record before the long, damaged descent, with one weak pilot ember.
            if(u==-1&&v==14&&e==1)return block(ModBlocks.FORGE_ARCHIVE.get());
            if(u==-1&&v==14&&e==2)return block(ModBlocks.FORGE_PILOT_LIGHT.get());
            boolean shaftEdge=(Math.abs(u)==12&&v>=12&&v<=28)||(v==11&&Math.abs(u)<=12);
            if(best.distance==2) {
                if(shaftEdge&&h<=51) {
                    if(e==1&&Math.floorMod(u+v,7)<4)return block(Blocks.POLISHED_DEEPSLATE_WALL);
                    return block(Blocks.CAVE_AIR);
                }
                return block(Math.floorMod(u+v,7)==0?Blocks.POLISHED_BASALT:Blocks.STONE);
            }
            // Rock falls interrupt one side of the stair, leaving irregular one-block passages.
            if(e<=2&&((u==1&&v>=17&&v<=20)||(u==-15&&v>=15&&v<=19)||(u==15&&v>=23&&v<=26)))
                return block(Math.floorMod(h+v,3)==0?Blocks.COBBLED_DEEPSLATE:Blocks.TUFF);
            if(e==1&&Math.floorMod(u*17+v*11,9)==0)return ModBlocks.FORGE_DEBRIS.get().defaultBlockState().setValue(ForgeDebrisBlock.KIND,1);
            if(u==13&&v==29&&e==2)return block(ModBlocks.FORGE_PILOT_LIGHT.get());
            return block(Blocks.CAVE_AIR);
        }
        if(Math.abs(u)<=12&&v>=12&&v<=28&&h>=2&&h<=52) {
            if((u==-12&&h==48&&v==23)||(u==0&&h==32&&v==12)||(u==12&&h==20&&v==23))return block(ModBlocks.FORGE_PILOT_LIGHT.get());
            if(h<=4)return block(h==4&&Math.floorMod(u+v,4)==0?Blocks.TUFF:Blocks.DEEPSLATE);
            if(h==52)return block(Blocks.STONE);
            if(Math.abs(u)==12&&(v==12||v==28))return block(h%8==0?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.POLISHED_BASALT);
            if(h==50&&v>=18&&v<=21)return block(Blocks.DARK_OAK_LOG);
            if(u==4&&v==20&&h>=10&&h<50)return block(Blocks.CHAIN);
            if(u>=3&&u<=5&&v>=19&&v<=21&&h>=7&&h<=10)return block(ModBlocks.FORGE_IRON.get());
            if(u>=-4&&u<=2&&v>=18&&v<=23&&h==36&&!(u>=0&&v>=21))return block(Blocks.DARK_OAK_PLANKS);
            if(u==-4&&v==18&&h>=36&&h<50)return block(Blocks.CHAIN);
            if(u>=6&&v>=24&&h<=5+(u-6)/2+(v-24)/2)return block(Blocks.TUFF);
            if(h==5&&Math.floorMod(u*11+v*17,5)==0)return ModBlocks.FORGE_DEBRIS.get().defaultBlockState().setValue(ForgeDebrisBlock.KIND,1);
            return block(Blocks.CAVE_AIR);
        }
        // Corbels and piers carry the gallery where it emerges from the service adit.
        if(v>=48&&v<=52&&u>=-26&&u<=2&&h>=1&&h<12) {
            for(var sample:COLUMNS.getOrDefault(key(u,v),List.of()))if(h<sample.step.h()-2) {
                if((u==0||u==-10||u==-24)&&v==50)return block(Blocks.POLISHED_BASALT);
                if(h>=sample.step.h()-4&&Math.abs(v-50)<=sample.step.h()-h-2)return block(Blocks.DEEPSLATE_BRICKS);
            }
        }
        return null;
    }
    private static BlockState block(Block b) { return b.defaultBlockState(); }
    private ForgeDescent() {}
}
