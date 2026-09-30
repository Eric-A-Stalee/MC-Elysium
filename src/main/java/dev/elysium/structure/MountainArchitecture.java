package dev.elysium.structure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import dev.elysium.structure.MountainBuildingPlan.Room;

/** Deterministic Nordic building grammar. Pure geometry can be inspected without starting Minecraft. */
public final class MountainArchitecture {
    public enum Material {
        AIR, STONE, RUBBLE, PLANK, CLADDING, PALE, LOG, STRIPPED, LOG_U, LOG_V, BEAM_U, BEAM_V,
        ROOF, ROOF_STAIR, ROOF_SLAB, WOOD_STAIR, WOOD_SLAB, FENCE, GLASS, DOOR_LOW, DOOR_HIGH,
        BED_FOOT, BED_HEAD, LANTERN, HANGING_LANTERN, BARREL, BOOKSHELF, FURNACE, CRAFTING, TABLE_TOP,
        LADDER, TRAPDOOR, CHIMNEY, CAMPFIRE
    }
    public record Cell(int u,int y,int v) {}
    /** Facing is local: south=0, west=1, north=2, east=3. */
    public record Voxel(Material material,int facing) { public Voxel(Material material){this(material,0);} }
    private final MountainBuildingPlan plan;
    private final Map<Cell,Voxel> blocks=new LinkedHashMap<>();
    private final Set<Cell> passages=new HashSet<>();
    private MountainArchitecture(MountainBuildingPlan plan) { this.plan=plan; }
    public static Map<Cell,Voxel> build(MountainBuildingPlan plan) {
        var b=new MountainArchitecture(plan);b.build();return b.blocks;
    }
    private void put(int u,int y,int v,Material m) { put(u,y,v,m,0); }
    private void put(int u,int y,int v,Material m,int facing) { blocks.put(new Cell(u,y,v),new Voxel(m,facing)); }
    private void box(int a,int y,int b,int c,int top,int d,Material m) {
        for(int u=a;u<=c;u++)for(int v=b;v<=d;v++)for(int h=y;h<=top;h++)put(u,h,v,m);
    }
    private void build() {
        var main=plan.main();
        // No rectangular yard, perimeter clearing, or world-height resampling.
        for(var g:plan.ground()) {
            int base=plan.rooms().stream().filter(r->r.contains(g.u(),g.v(),0)).mapToInt(Room::floor).min().orElse(main.floor());
            if(plan.cellar() && main.contains(g.u(),g.v(),0))base=Math.min(base,main.floor()-5);
            for(int y=Math.min(g.original()-1,base-1);y<base;y++)put(g.u(),y,g.v(),foundation(g.u(),y,g.v()));
            for(int y=base;y<=plan.maxY();y++)put(g.u(),y,g.v(),Material.AIR);
        }
        for(var r:plan.rooms())shell(r,r==main && plan.cellar());
        for(var r:plan.rooms())roof(r);
        for(var r:plan.rooms())interior(r,r==main);
        for(int i=1;i<plan.rooms().size();i++)join(main,plan.rooms().get(i));
        entrance();
        // A small, useful attic sits beneath the main ridge, reached from the upper landing.
        int ladderV=main.v()+3;
        for(int y=main.floor()+6;y<=main.eaves()+1;y++) {
            put(0,y,ladderV-1,Material.STRIPPED);put(0,y,ladderV,Material.LADDER,0);
        }
        put(0,main.eaves(),ladderV,Material.LADDER,0);
        chimney(main);
        plan.gallery().ifPresent(this::gallery);
        furnishBeds();
    }
    private Material foundation(int u,int y,int v) { return Math.floorMod(u*13+v*7+y,11)<3?Material.RUBBLE:Material.STONE; }
    private void shell(Room r,boolean cellar) {
        int low=cellar?r.floor()-5:r.floor();
        if(cellar)box(r.u(),low,r.v(),r.maxU(),low,r.maxV(),Material.STONE);
        for(int u=r.u();u<=r.maxU();u++)for(int v=r.v();v<=r.maxV();v++) {
            int original=plan.original(u,v);
            for(int y=Math.min(original-1,low-1);y<r.floor();y++) {
                boolean edge=u==r.u() || u==r.maxU() || v==r.v() || v==r.maxV();
                if(!cellar || edge || y<=low)put(u,y,v,foundation(u,y,v));
                else put(u,y,v,Material.AIR);
                if(cellar && edge && y==r.floor()-3 && original<y-1 && Math.floorMod(u+v,4)==0)put(u,y,v,Material.GLASS);
            }
            for(int s=0;s<=r.storeys();s++)put(u,r.floor()+s*5,v,Material.PLANK);
            if(u!=r.u() && u!=r.maxU() && v!=r.v() && v!=r.maxV())continue;
            boolean corner=(u==r.u() || u==r.maxU()) && (v==r.v() || v==r.maxV());
            boolean post=corner || ((u==r.u() || u==r.maxU())?Math.floorMod(v-r.v(),4)==0:Math.floorMod(u-r.u(),4)==0);
            for(int y=r.floor();y<r.eaves();y++) {
                int band=Math.floorMod(y-r.floor(),5);
                Material m;
                if(post)m=corner?Material.LOG:Material.STRIPPED;
                else if(band==0)m=(u==r.u() || u==r.maxU())?Material.LOG_V:Material.LOG_U;
                else {
                    // Whole bays have a purpose and material; there is no random wall confetti.
                    int bay=(u==r.u() || u==r.maxU())?(v-r.v())/4:(u-r.u())/4;
                    m=(Math.floorMod(bay+(int)plan.seed(),4)==0 && band>=2)?Material.PALE:Material.CLADDING;
                    if((band==2 || band==3) && (Math.floorMod(u+v,4)==1 || Math.floorMod(u+v,4)==2))m=Material.GLASS;
                }
                put(u,y,v,m);
            }
        }
        // Projecting upper-floor beams and brackets give the timber construction depth.
        for(int v=r.v()+2;v<r.maxV();v+=4)for(int side:new int[]{-1,1}) {
            int u=side<0?r.u()-1:r.maxU()+1;
            for(int y=r.floor()+5;y<r.eaves();y+=5)put(u,y,v,Material.BEAM_U);
        }
    }
    private void roof(Room r) {
        int lo=r.crossRoof()?r.v()-1:r.u()-1,hi=r.crossRoof()?r.maxV()+1:r.maxU()+1;
        int start=r.crossRoof()?r.u()-1:r.v()-1,end=r.crossRoof()?r.maxU()+1:r.maxV()+1;
        int middle=(lo+hi)/2;
        for(int a=lo;a<=hi;a++) {
            int height=r.eaves()-1+Math.min(a-lo,hi-a);
            int facing=r.crossRoof()?(a<middle?0:2):(a<middle?3:1);
            for(int along=start;along<=end;along++) {
                int u=r.crossRoof()?along:a,v=r.crossRoof()?a:along;
                boolean buried=plan.rooms().stream().anyMatch(other->other!=r && other.contains(u,v,0) && height<other.eaves());
                if(buried)continue;
                boolean verge=along==start || along==end;
                put(u,height,v,a==middle?Material.ROOF:(verge?Material.WOOD_STAIR:Material.ROOF_STAIR),facing);
                if(a==middle)put(u,height+1,v,r.crossRoof()?Material.LOG_U:Material.LOG_V);
                if(along==start+1 || along==end-1)for(int y=r.eaves();y<height;y++) {
                    if(plan.rooms().stream().anyMatch(other->other!=r && other.contains(u,v,0) && other.eaves()>r.eaves()))continue;
                    put(u,y,v,(a==middle || (y-r.eaves())%4==0)?Material.STRIPPED:Material.CLADDING);
                    if(Math.abs(a-middle)<=1 && y>=r.eaves()+1 && y<=r.eaves()+2)put(u,y,v,Material.GLASS);
                }
            }
        }
        int ridge=r.eaves()-1+Math.min(middle-lo,hi-middle);
        for(int tip:new int[]{start,end}) {
            int u=r.crossRoof()?tip:middle,v=r.crossRoof()?middle:tip;
            put(u,ridge+2,v,Material.LOG);
            put(u,ridge+3,v,Material.WOOD_SLAB);
        }
        if(r==plan.main() && (plan.seed()&1)==0)dormer(r);
    }
    private void dormer(Room r) {
        int u=r.maxU()-1,v=r.v()+5,y=r.eaves()+1;
        // Five-block cross gable, fitted into the roof rather than pasted above the ridge.
        for(int a=-2;a<=2;a++) {
            put(u,y,v+a,Math.abs(a)<2?Material.GLASS:Material.LOG);
            for(int dx=-2;dx<=1;dx++)put(u+dx,y+1+2-Math.abs(a),v+a,Material.ROOF_STAIR,a<0?0:2);
        }
    }
    private void interior(Room r,boolean main) {
        // Connected rooms on both storeys, with a wide internal doorway.
        int divider=r.v()+r.depth()/2;
        for(int s=0;s<r.storeys();s++) {
            int floor=r.floor()+s*5;
            if(r.depth()>=13)for(int u=r.u()+1;u<r.maxU();u++)for(int y=floor+1;y<floor+5;y++)
                if(Math.abs(u-(r.u()+r.maxU())/2)>1)put(u,y,divider,y==floor+4?Material.BEAM_U:Material.CLADDING);
            put(r.maxU()-1,floor+1,r.v()+2,s==0?Material.BARREL:Material.BOOKSHELF);
            put(r.maxU()-1,floor+1,r.v()+3,s==0?Material.CRAFTING:Material.BOOKSHELF);
            put((r.u()+r.maxU())/2,floor+4,r.maxV()-3,Material.HANGING_LANTERN);
            put((r.u()+r.maxU())/2,floor+4,r.v()+3,Material.HANGING_LANTERN);
            if(s==0 && main) {
                for(int v=r.maxV()-4;v<=r.maxV()-3;v++) {
                    put(0,floor+1,v,Material.FENCE);put(0,floor+2,v,Material.TABLE_TOP);
                    put(2,floor+1,v,Material.WOOD_STAIR,1);
                }
            }
        }
        if(main) {
            for(int s=plan.cellar()?-1:0;s<r.storeys()-1;s++)stair(r,r.floor()+s*5);
            if(plan.cellar()) {
                put(r.maxU()-1,r.floor()-4,r.v()+2,Material.BARREL);
                put(r.maxU()-1,r.floor()-4,r.v()+3,Material.BARREL);
            }
        }
    }
    private void furnishBeds() {
        for(var r:plan.rooms())for(int s=r==plan.main()?1:0;s<r.storeys();s++) {
            int y=r.floor()+s*5+1,count=0,wanted=r==plan.main()?2:1;
            search:for(int v=r.maxV()-2;v>=r.v()+2;v--)for(int u=r.u()+2;u<=r.maxU()-2;u+=2) {
                var foot=new Cell(u,y,v);var head=new Cell(u,y,v+1);
                if(passages.contains(foot) || passages.contains(head) || !isAir(foot) || !isAir(head)
                        || !isAir(new Cell(u,y+1,v)) || !isAir(new Cell(u,y+1,v+1))
                        || material(new Cell(u,y-1,v))!=Material.PLANK || material(new Cell(u,y-1,v+1))!=Material.PLANK)continue;
                put(u,y,v,Material.BED_FOOT,0);put(u,y,v+1,Material.BED_HEAD,0);
                if(++count==wanted)break search;
            }
        }
    }
    private Material material(Cell cell) { return blocks.getOrDefault(cell,new Voxel(Material.AIR)).material(); }
    private boolean isAir(Cell cell) { return material(cell)==Material.AIR; }
    private void stair(Room r,int floor) {
        int start=r.v()+2;
        for(int step=0;step<5;step++)for(int u=r.u()+1;u<=r.u()+2;u++) {
            int v=start+step,y=floor+step+1;
            // Open-backed flights can stack above a cellar stair with real headroom.
            put(u,y,v,Material.WOOD_STAIR,0);
            for(int h=y+1;h<=Math.max(y+3,floor+5);h++){put(u,h,v,Material.AIR);passages.add(new Cell(u,h,v));}
        }
        // Rail only along the opening, never across either landing.
        for(int v=start;v<start+4;v++)put(r.u()+3,floor+6,v,Material.FENCE);
    }
    private void join(Room main,Room wing) {
        int side=wing.u()==main.maxU()?1:-1,wall=side>0?main.maxU():main.u();
        // Join toward the front, clear of the core's stacked rear staircase.
        int v=Math.min(wing.maxV()-2,main.maxV()-3);
        for(int storey=0;storey<Math.min(main.storeys(),wing.storeys());storey++) {
            int mf=main.floor()+storey*5,wf=wing.floor()+storey*5;
            for(int a=-3;a<=3;a++) {
                int floor=mf+Integer.signum(wf-mf)*Math.clamp(a+2,0,Math.abs(wf-mf));
                for(int across=-1;across<=1;across++) {
                    int u=wall+side*a,z=v+across;
                    put(u,floor,z,Material.PLANK);
                    if(a>=-1 && a<Math.abs(wf-mf)-1 && wf!=mf)put(u,floor,z,Material.WOOD_STAIR,wf>mf?(side>0?3:1):(side>0?1:3));
                    for(int y=floor+1;y<=Math.max(mf,wf)+3;y++){put(u,y,z,Material.AIR);passages.add(new Cell(u,y,z));}
                }
            }
        }
    }
    private void entrance() {
        var e=plan.entry();int face=e.du()>0?3:e.du()<0?1:e.dv()>0?0:2;
        for(int a=1;a<=3;a++)for(int b=-2;b<=2;b++) {
            int u=e.u()+e.du()*a+e.dv()*b,v=e.v()+e.dv()*a-e.du()*b;
            for(int y=plan.original(u,v)-1;y<e.floor();y++)put(u,y,v,foundation(u,y,v));
            put(u,e.floor(),v,Material.PLANK);
            for(int y=e.floor()+1;y<=e.floor()+3;y++)put(u,y,v,Material.AIR);
            put(u,e.floor()+4,v,Material.ROOF_SLAB);
            if(a==3 && Math.abs(b)==2)for(int y=e.floor()+1;y<=e.floor()+3;y++)put(u,y,v,Material.LOG);
        }
        put(e.u(),e.floor()+1,e.v(),Material.DOOR_LOW,face);
        put(e.u(),e.floor()+2,e.v(),Material.DOOR_HIGH,face);
        put(e.u(),e.floor(),e.v(),Material.PLANK);
        // Clear the actual threshold and its internal landing, including a side entry near furniture.
        for(int a=-2;a<=0;a++)for(int b=-1;b<=1;b++)for(int y=e.floor()+1;y<=e.floor()+3;y++) {
            if(a==0 && b!=0)continue;
            if(a==0 && b==0 && y<=e.floor()+2)continue;
            put(e.u()+e.du()*a+e.dv()*b,y,e.v()+e.dv()*a-e.du()*b,Material.AIR);
        }
        put(e.u()+e.du()*2,e.floor()+3,e.v()+e.dv()*2,Material.HANGING_LANTERN);
    }
    private void chimney(Room r) {
        int u=r.maxU()-2,v=r.v()+2;
        for(int y=r.floor()+1;y<=r.eaves()+5;y++)put(u,y,v,Material.CHIMNEY);
        put(u,r.floor()+1,v+1,Material.FURNACE);
        put(u,r.eaves()+6,v,Material.CAMPFIRE);
        put(u,r.eaves()+7,v,Material.ROOF_SLAB);
    }
    private void gallery(MountainBuildingPlan.Gallery g) {
        for(int v=g.start();v<=g.end();v++)for(int a=1;a<=2;a++) {
            int u=g.wall()+g.side()*a;
            put(u,g.floor(),v,Material.PLANK);
            for(int y=g.floor()+1;y<=g.floor()+3;y++)put(u,y,v,Material.AIR);
            put(u,g.floor()+4,v,Material.ROOF_SLAB);
            if(a==2 || v==g.start() || v==g.end())put(u,g.floor()+1,v,Material.FENCE);
            if(v==g.start() || v==g.end()) {
                for(int y=g.floor()+1;y<=g.floor()+3;y++)put(u,y,v,Material.LOG);
                put(u,g.floor()-1,v,Material.BEAM_U);
            }
        }
        int v=plan.main().maxV()-4,face=g.side()>0?3:1;
        put(g.wall(),g.floor()+1,v,Material.DOOR_LOW,face);
        put(g.wall(),g.floor()+2,v,Material.DOOR_HIGH,face);
        for(int a=-2;a<=1;a++)for(int y=g.floor()+1;y<=g.floor()+2;y++) {
            var c=new Cell(g.wall()+g.side()*a,y,v);passages.add(c);
            if(a!=0)put(c.u(),c.y(),c.v(),Material.AIR);
        }
        put(g.wall()+g.side(),g.floor()+3,g.start()+1,Material.HANGING_LANTERN);
    }
}
