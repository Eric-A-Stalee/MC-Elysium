package dev.elysium.structure;

import java.util.Optional;
import java.util.function.IntBinaryOperator;

/** A surveyed entrance and a fully buried foundry. Coordinates are relative to the doorway. */
public record ForgePlan(int x, int z, int entranceFloor, int turn) {
    public static final int MIN_U = -46, MAX_U = 46, MIN_V = -5, MAX_V = 119;
    public ForgePlan {
        if (turn < 0 || turn > 3) throw new IllegalArgumentException("Invalid forge orientation");
    }
    public int floor() { return entranceFloor - 26; }
    public int worldX(int u, int v) { return x + switch (turn) { case 0 -> u; case 1 -> -v; case 2 -> -u; default -> v; }; }
    public int worldZ(int u, int v) { return z + switch (turn) { case 0 -> v; case 1 -> u; case 2 -> -v; default -> -u; }; }
    public int localU(int wx, int wz) { int a=wx-x,b=wz-z; return switch(turn) { case 0->a;case 1->b;case 2->-a;default->-b; }; }
    public int localV(int wx, int wz) { int a=wx-x,b=wz-z; return switch(turn) { case 0->b;case 1->-a;case 2->-b;default->a; }; }
    public int stairFloor(int v) { return entranceFloor - Math.min(26, Math.max(0, v-6)); }

    public static Optional<ForgePlan> find(IntBinaryOperator heights, int x, int z, int sea, int minY) {
        int entry=heights.applyAsInt(x,z)-1;
        if(entry<sea+30 || entry-28<minY+32) return Optional.empty();
        for(int turn=0;turn<4;turn++) {
            var p=new ForgePlan(x,z,entry,turn);
            // A genuine slope into a mountain, with open ground in front of the doorway.
            if(p.height(heights,0,68)<entry+12 || p.height(heights,0,-8)>entry+3) continue;
            boolean fits=true;
            entrance: for(int u=-5;u<=5;u+=2) for(int v=-5;v<=2;v++) {
                int y=p.height(heights,u,v);
                if(y<entry-2 || y>entry+3) {fits=false;break entrance;}
            }
            if(!fits) continue;
            // Every room is checked against base terrain, never a loaded neighbouring chunk.
            cover: for(int u=MIN_U;u<=MAX_U;u+=3) for(int v=8;v<=MAX_V;v+=3) {
                int roof=p.roof(u,v);
                if(roof!=Integer.MIN_VALUE && p.height(heights,u,v)<roof+(v<19?2:6)) {fits=false;break cover;}
            }
            if(fits) return Optional.of(p);
        }
        return Optional.empty();
    }
    private int height(IntBinaryOperator h,int u,int v) { return h.applyAsInt(worldX(u,v),worldZ(u,v))-1; }

    /** The union of vaulted spaces. Negative infinity leaves natural rock completely intact. */
    public int roof(int u,int v) {
        int roof=Integer.MIN_VALUE;
        if(Math.abs(u)<=4 && v>=0 && v<=40) roof=stairFloor(v)+7-Math.abs(u)/2;
        if(Math.abs(u)<=10 && v>=34 && v<=49) roof=Math.max(roof,floor()+12-Math.abs(u)/3);
        double main=(double)(u*u)/(27*27)+(double)((v-70)*(v-70))/(26*26);
        if(main<=1) roof=Math.max(roof,floor()+12+(int)Math.round(16*Math.sqrt(1-main)));
        if(Math.abs(u)>=22 && Math.abs(u)<=37 && v>=57 && v<=63) roof=Math.max(roof,floor()+7);
        if(u>=-46 && u<=-29 && v>=47 && v<=82) roof=Math.max(roof,floor()+12-Math.abs(u+37)/3);
        if(u>=29 && u<=46 && v>=54 && v<=89) roof=Math.max(roof,floor()+14-Math.abs(u-37)/3);
        if(Math.abs(u)<=4 && v>=90 && v<=106) roof=Math.max(roof,floor()+8-Math.abs(u)/3);
        double rear=(double)(u*u)/(13*13)+(double)((v-106)*(v-106))/(13*13);
        if(rear<=1) roof=Math.max(roof,floor()+9+(int)Math.round(9*Math.sqrt(1-rear)));
        return roof;
    }
    public int floorAt(int u,int v) { return v<34 ? stairFloor(v) : floor(); }
    public boolean inside(int u,int v) { return roof(u,v)!=Integer.MIN_VALUE; }
    public boolean edge(int u,int v) { return !inside(u-1,v)||!inside(u+1,v)||!inside(u,v-1)||!inside(u,v+1); }
}
