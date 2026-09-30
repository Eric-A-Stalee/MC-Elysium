package dev.elysium.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** A surveyed building, not a leveled lot. Coordinates in rooms are local; heights are absolute. */
public record MountainBuildingPlan(int x, int z, int rotation, long seed, Style style,
        List<Room> rooms, Entry entry, List<Ground> ground, boolean cellar) {
    public enum Style { LONGHOUSE, CROSS_GABLE, HILLSIDE_LODGE, COURTYARD, GREAT_HALL, WATCH_LODGE }
    public record Point(int x, int z) {}
    public record Room(int u, int v, int width, int depth, int floor, int storeys, boolean crossRoof) {
        public Room {
            if (width<7 || width>15 || depth<7 || depth>25 || storeys<1 || storeys>3
                    || Math.abs(u)>24 || Math.abs(v)>24) throw new IllegalArgumentException("Invalid mountain room");
        }
        public int maxU() { return u+width-1; }
        public int maxV() { return v+depth-1; }
        public boolean contains(int a,int b,int margin) { return a>=u-margin && a<=maxU()+margin && b>=v-margin && b<=maxV()+margin; }
        public int eaves() { return floor+storeys*5; }
        public int ridge() { return eaves()+(crossRoof?depth:width)/2+2; }
    }
    /** Wall opening; (du,dv) is the outward normal. Its three-block porch ends at the street. */
    public record Entry(int u,int v,int du,int dv,int floor) {
        public Entry { if(Math.abs(du)+Math.abs(dv)!=1)throw new IllegalArgumentException("Invalid doorway direction"); }
        public boolean porch(int a,int b,int margin) {
            int along=(a-u)*du+(b-v)*dv, across=(a-u)*dv-(b-v)*du;
            return along>=1-margin && along<=3+margin && Math.abs(across)<=2+margin;
        }
    }
    public record Ground(int u,int v,int original) {}
    public MountainBuildingPlan {
        rooms=List.copyOf(rooms); ground=List.copyOf(ground);
        if(rotation<0 || rotation>3 || rooms.isEmpty() || rooms.size()>3 || ground.size()>1200 || ground.isEmpty())
            throw new IllegalArgumentException("Invalid mountain building plan");
        var main=rooms.getFirst();
        if(entry.floor()!=main.floor() || !main.contains(entry.u(),entry.v(),0))throw new IllegalArgumentException("Detached entry");
        for(var r:rooms)if(Math.abs(r.floor()-main.floor())>2)throw new IllegalArgumentException("Disconnected split level");
    }
    public Room main() { return rooms.getFirst(); }
    public int worldX(int u,int v) { return x+switch(rotation){case 1->-v;case 2->-u;case 3->v;default->u;}; }
    public int worldZ(int u,int v) { return z+switch(rotation){case 1->u;case 2->-v;case 3->-u;default->v;}; }
    public int localU(int wx,int wz) { return switch(rotation){case 1->wz-z;case 2->x-wx;case 3->z-wz;default->wx-x;}; }
    public int localV(int wx,int wz) { return switch(rotation){case 1->x-wx;case 2->z-wz;case 3->wx-x;default->wz-z;}; }
    public Point street() { return point(entry.u()+entry.du()*3,entry.v()+entry.dv()*3); }
    public Point approach() { return point(entry.u()+entry.du()*4,entry.v()+entry.dv()*4); }
    public Point point(int u,int v) { return new Point(worldX(u,v),worldZ(u,v)); }
    public boolean occupies(int wx,int wz,int margin) {
        int u=localU(wx,wz),v=localV(wx,wz);
        return entry.porch(u,v,margin) || rooms.stream().anyMatch(r->r.contains(u,v,margin));
    }
    public boolean access(int wx,int wz) {
        int u=localU(wx,wz)-entry.u(),v=localV(wx,wz)-entry.v();
        return u*entry.du()+v*entry.dv()>=1 && Math.abs(u*entry.dv()-v*entry.du())<=1;
    }
    public int minFloor() { return rooms.stream().mapToInt(Room::floor).min().orElseThrow()-(cellar?5:0); }
    public int maxY() { return rooms.stream().mapToInt(Room::ridge).max().orElseThrow()+3; }
    public int original(int u,int v) { return ground.stream().filter(g->g.u()==u && g.v()==v).mapToInt(Ground::original).findFirst().orElse(main().floor()); }
    public boolean home() { return style!=Style.GREAT_HALL && style!=Style.WATCH_LODGE; }

    /** Tries a grammar on this slope. Failed wings reject the site; they are never flattened into place. */
    public static Optional<MountainBuildingPlan> fit(LandscapePlanner.Terrain terrain,int x,int z,int rotation,
            long seed,Style style,int sea,Point streetTarget) {
        var random=new Random(seed); var rooms=new ArrayList<Room>();
        int width=9+2*random.nextInt(3),depth=13+2*random.nextInt(4),storeys=2;
        switch(style) {
            case LONGHOUSE -> {width=9+2*random.nextInt(2);depth=19+2*random.nextInt(2);}
            case HILLSIDE_LODGE -> {width=9;depth=15+2*random.nextInt(3);}
            case COURTYARD -> {width=11;depth=13+2*random.nextInt(2);}
            case GREAT_HALL -> {width=15;depth=23;}
            case WATCH_LODGE -> {width=9;depth=15;storeys=3;}
            default -> {}
        }
        // Only a narrow core spans the slope; a long axis can follow its contour.
        var shell=new Room(-width/2,-depth/2,width,depth,0,storeys,false);
        var sample=sample(terrain,x,z,rotation,shell);
        int floor=sample.get((sample.size()*3)/4);
        if(!fits(sample,floor,sea))return Optional.empty();
        var main=new Room(shell.u(),shell.v(),width,depth,floor,storeys,false);rooms.add(main);
        int side=random.nextBoolean()?1:-1;
        int wings=switch(style){case LONGHOUSE->0;case COURTYARD->2;default->1;};
        for(int i=0;i<wings;i++) {
            int sign=i==0?side:-side,ww=7+2*random.nextInt(2),dd=9+2*random.nextInt(2);
            int wu=sign>0?main.maxU():main.u()-ww+1;
            int wv=style==Style.COURTYARD?main.maxV()-5:main.v()+2+random.nextInt(Math.max(1,depth-dd-2));
            var wing=new Room(wu,wv,ww,dd,0,1,true);
            var heights=sample(terrain,x,z,rotation,wing);
            int wf=Math.clamp(heights.get(heights.size()*3/4),floor-2,floor+2);
            if(!fits(heights,wf,sea))return Optional.empty();
            int ws=(style==Style.CROSS_GABLE || style==Style.GREAT_HALL || (style==Style.COURTYARD && i==0))?2:1;
            rooms.add(new Room(wu,wv,ww,dd,wf,ws,true));
        }
        // An uphill side entry is often a better fit than a door fixed to the gable end.
        var entrances=new ArrayList<Entry>();
        entrances.add(new Entry(0,main.maxV(),0,1,floor));
        entrances.add(new Entry(0,main.v(),0,-1,floor));
        entrances.add(new Entry(main.maxU(),main.maxV()-3,1,0,floor));
        entrances.add(new Entry(main.u(),main.maxV()-3,-1,0,floor));
        entrances.sort(Comparator.comparingInt(e->{
            var p=transform(x,z,rotation,e.u()+e.du()*4,e.v()+e.dv()*4);
            return Math.abs(terrain.height(p.x(),p.z())-1-floor)*12
                    +(Math.abs(p.x()-streetTarget.x())+Math.abs(p.z()-streetTarget.z()))/3;
        }));
        for(var entry:entrances) {
            boolean valid=true; var ground=new LinkedHashMap<Point,Ground>();
            for(var r:rooms)for(int u=r.u();u<=r.maxU();u++)for(int v=r.v();v<=r.maxV();v++) {
                var p=transform(x,z,rotation,u,v);
                ground.put(new Point(u,v),new Ground(u,v,terrain.height(p.x(),p.z())-1));
            }
            for(int a=1;a<=4;a++)for(int b=-2;b<=2;b++) {
                int u=entry.u()+entry.du()*a+entry.dv()*b,v=entry.v()+entry.dv()*a-entry.du()*b;
                if(rooms.stream().anyMatch(r->r.contains(u,v,1)) && a>=2)valid=false;
                var p=transform(x,z,rotation,u,v);int h=terrain.height(p.x(),p.z())-1;
                if(h<sea-1 || floor-h>3 || h-floor>2)valid=false;
                if(a<=3)ground.put(new Point(u,v),new Ground(u,v,h));
            }
            if(!valid)continue;
            return Optional.of(new MountainBuildingPlan(x,z,rotation,seed,style,rooms,entry,new ArrayList<>(ground.values()),
                    sample.getFirst()<=floor-3 && style!=Style.WATCH_LODGE));
        }
        return Optional.empty();
    }
    private static List<Integer> sample(LandscapePlanner.Terrain terrain,int x,int z,int rotation,Room r) {
        var values=new ArrayList<Integer>();
        for(int u=r.u();u<=r.maxU();u++)for(int v=r.v();v<=r.maxV();v++) {
            var p=transform(x,z,rotation,u,v);values.add(terrain.height(p.x(),p.z())-1);
        }
        values.sort(Integer::compare);return values;
    }
    private static boolean fits(List<Integer> h,int floor,int sea) { return h.getFirst()>=sea-1 && h.getLast()-floor<=3 && floor-h.getFirst()<=8; }
    private static Point transform(int x,int z,int r,int u,int v) {
        return new Point(x+switch(r){case 1->-v;case 2->-u;case 3->v;default->u;},
                z+switch(r){case 1->u;case 2->-v;case 3->-u;default->v;});
    }
}
