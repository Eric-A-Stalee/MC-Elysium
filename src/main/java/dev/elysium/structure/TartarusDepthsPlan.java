package dev.elysium.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** Saved region coordinates and a continuous, one-block-grade expedition route. */
public final class TartarusDepthsPlan {
    public static final int ANCHOR_FLOOR=-48, BASIN_Z=210, BOSS_X=128, BOSS_Z=220, BOSS_FLOOR=-232;
    public static final int MIN_X=-88, MAX_X=165, MIN_Z=-5, MAX_Z=299;
    public record Step(int x,int y,int z) {}
    public record PathCell(int floor,int distance) {}
    public final int x,z;
    public final long seed;
    private final List<Step> route=new ArrayList<>();
    private final Map<Long,PathCell> path=new HashMap<>();
    public TartarusDepthsPlan(int x,int z,long seed) {
        this.x=x;this.z=z;this.seed=seed;
        int[][] points={{10,-48,30},{10,-48,42},{30,-58,42},{30,-68,62},{2,-82,62},
            {2,-92,82},{30,-106,82},{30,-116,102},{-2,-132,102},{-2,-142,122},
            {-26,-154,122},{-26,-164,142},{-10,-172,142},{-10,-184,166},
            {-26,-184,166},{-26,-184,190},{38,-184,190},{58,-184,210},{70,-186,210},
            {86,-190,210},{86,-207,232},{104,-215,232},{104,-222,246},{116,-232,246}};
        for(int n=1;n<points.length;n++) {
            var a=points[n-1];var b=points[n];
            int length=Math.max(Math.abs(b[0]-a[0]),Math.abs(b[2]-a[2]));
            for(int i=0;i<=length;i++) {
                double t=(double)i/length;
                var step=new Step(a[0]+(int)Math.round((b[0]-a[0])*t),a[1]+(int)Math.round((b[1]-a[1])*t),a[2]+(int)Math.round((b[2]-a[2])*t));
                route.add(step);
                for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++) {
                    int d=Math.max(Math.abs(dx),Math.abs(dz));long key=key(step.x+dx,step.z+dz);
                    var old=path.get(key);
                    if(old==null||d<old.distance)path.put(key,new PathCell(step.y,d));
                }
            }
        }
    }
    public static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public List<Step> route(){return List.copyOf(route);}
    public PathCell path(int u,int v){return path.get(key(u,v));}
    public BlockPos at(int u,int y,int v){return new BlockPos(x+u,y,z+v);}
    public BlockPos bridgeControl(){return at(-3,-183,188);}
    public BlockPos heart(){return at(116,BOSS_FLOOR+1,246);}
    public BlockPos monster(){return at(BOSS_X,BOSS_FLOOR+1,BOSS_Z);}
    public static boolean bridgeGap(int u,int v){return u>=6&&u<=22&&Math.abs(v-190)<=3;}
    /** The anchor's buried lower run slopes into Tartarus and emerges above the rift. */
    public boolean lowerChain(int u,int y,int v) {
        if(Math.abs(u)>2||v<0||v>221)return false;
        int dy=y-(-52-(int)Math.round(v*.43));if(Math.abs(dy)>2)return false;
        int first=Math.floorDiv(v,7);
        for(int n=first;n<=first+1;n++) {
            int dz=Math.abs(v-n*7);if(dz>5)continue;
            if((n&1)==0&&u==0&&(dz==5||Math.abs(dy)==2))return true;
            if((n&1)==1&&dy==0&&(dz==5||Math.abs(u)==2))return true;
        }
        return false;
    }
    public double basinRadius(int u,int v){return (double)(u*u)/(84*84)+(double)((v-BASIN_Z)*(v-BASIN_Z))/(86*86);}
    public double chamberRadius(int u,int v){return (double)((u-BOSS_X)*(u-BOSS_X))/(35*35)+(double)((v-BOSS_Z)*(v-BOSS_Z))/(39*39);}
    public int floor(int u,int v) {
        double r=basinRadius(u,v);
        double ridge=10*Math.sin((u+seed%97)*.061)+7*Math.cos((v-seed%71)*.054)+4*Math.sin((u+v)*.11);
        double fracture=Math.abs(u-18-7*Math.sin((v-BASIN_Z)*.045));
        int cut=fracture<8?(int)Math.round(35*(1-fracture/8)):0;
        return Math.max(-242,Math.min(-164,-194+(int)Math.round(ridge+Math.max(0,r-.5)*32)-cut));
    }
    public int roof(int u,int v) {
        double r=basinRadius(u,v);
        return -146+(int)Math.round(52*Math.sqrt(Math.max(0,1-r))+6*Math.sin(u*.09)*Math.cos(v*.07));
    }
    public int hash(int u,int y,int v) {
        long h=seed^u*0x632BE59BD9B4E019L^v*0x9E3779B97F4A7C15L^y*0x85157AF5L;
        h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;
        return (int)(h^(h>>>31))&0x7fffffff;
    }
}
