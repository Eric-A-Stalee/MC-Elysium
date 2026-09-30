package dev.elysium.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** A surveyed building, not a leveled lot. Coordinates in rooms are local; heights are absolute. */
public record MountainBuildingPlan(int x, int z, int rotation, long seed, Style style,
        List<Room> rooms, Entry entry, List<Ground> ground, boolean cellar, int gallerySide) {
    public enum Style { LONGHOUSE, CROSS_GABLE, HILLSIDE_LODGE, COURTYARD, GREAT_HALL, WATCH_LODGE }
    public record Point(int x, int z) {}
    /** Each mass has its own upper-floor projection, ridge and construction treatment. */
    public record Room(int u, int v, int width, int depth, int floor, int storeys, boolean crossRoof,
            int jetty, int roofShift, int facade, int shed) {
        public Room {
            if (width<7 || width>15 || depth<7 || depth>25 || storeys<1 || storeys>3
                    || Math.abs(u)>30 || Math.abs(v)>30 || Math.abs(jetty)>1 || Math.abs(roofShift)>1
                    || facade<0 || facade>3 || Math.abs(shed)>1) throw new IllegalArgumentException("Invalid mountain room");
        }
        public Room(int u,int v,int width,int depth,int floor,int storeys,boolean crossRoof) {
            this(u,v,width,depth,floor,storeys,crossRoof,0,0,0,0);
        }
        public Room(int u,int v,int width,int depth,int floor,int storeys,boolean crossRoof,int jetty,int roofShift,int facade) {
            this(u,v,width,depth,floor,storeys,crossRoof,jetty,roofShift,facade,0);
        }
        public int maxU() { return u+width-1; }
        public int maxV() { return v+depth-1; }
        public boolean contains(int a,int b,int margin) { return a>=u-margin && a<=maxU()+margin && b>=v-margin && b<=maxV()+margin; }
        public int eaves() { return floor+storeys*5; }
        public int ridge() { return eaves()+(crossRoof?depth:width)/2+4; }
        public int upperU() { return u+(storeys>1?Math.min(0,jetty):0); }
        public int upperMaxU() { return maxU()+(storeys>1?Math.max(0,jetty):0); }
        public boolean envelope(int a,int b,int margin) {return a>=upperU()-margin && a<=upperMaxU()+margin && b>=v-margin && b<=maxV()+margin;}
    }
    /** Wall opening; (du,dv) is the outward normal. Its three-block porch ends at the street. */
    public record Entry(int u,int v,int du,int dv,int floor,int length) {
        public Entry { if(Math.abs(du)+Math.abs(dv)!=1 || length<1 || length>3)throw new IllegalArgumentException("Invalid doorway direction"); }
        public Entry(int u,int v,int du,int dv,int floor) {this(u,v,du,dv,floor,3);}
        public boolean porch(int a,int b,int margin) {
            int along=(a-u)*du+(b-v)*dv, across=(a-u)*dv-(b-v)*du;
            return along>=1-margin && along<=length+margin && Math.abs(across)<=2+margin;
        }
    }
    public record Ground(int u,int v,int original) {}
    /** A cantilevered timber gallery changes the silhouette without excavating a yard underneath. */
    public record Gallery(int wall,int side,int start,int end,int floor) {
        public boolean contains(int u,int v,int margin) {
            int a=(u-wall)*side;
            return a>=1-margin && a<=2+margin && v>=start-margin && v<=end+margin;
        }
    }
    public MountainBuildingPlan {
        rooms=List.copyOf(rooms); ground=List.copyOf(ground);
        if(rotation<0 || rotation>3 || rooms.isEmpty() || rooms.size()>4 || ground.size()>1600 || ground.isEmpty() || Math.abs(gallerySide)>1)
            throw new IllegalArgumentException("Invalid mountain building plan");
        var main=rooms.getFirst();
        if(entry.floor()!=main.floor() || !main.contains(entry.u(),entry.v(),0))throw new IllegalArgumentException("Detached entry");
        for(var r:rooms)if(Math.abs(r.floor()-main.floor())>2)throw new IllegalArgumentException("Disconnected split level");
    }
    public MountainBuildingPlan(int x,int z,int rotation,long seed,Style style,List<Room> rooms,Entry entry,List<Ground> ground,boolean cellar) {
        this(x,z,rotation,seed,style,rooms,entry,ground,cellar,defaultGallerySide(rooms,style,seed));
    }
    public Room main() { return rooms.getFirst(); }
    public int worldX(int u,int v) { return x+switch(rotation){case 1->-v;case 2->-u;case 3->v;default->u;}; }
    public int worldZ(int u,int v) { return z+switch(rotation){case 1->u;case 2->-v;case 3->-u;default->v;}; }
    public int localU(int wx,int wz) { return switch(rotation){case 1->wz-z;case 2->x-wx;case 3->z-wz;default->wx-x;}; }
    public int localV(int wx,int wz) { return switch(rotation){case 1->x-wx;case 2->z-wz;case 3->wx-x;default->wz-z;}; }
    public Point street() { return point(entry.u()+entry.du()*entry.length(),entry.v()+entry.dv()*entry.length()); }
    public Point approach() { return point(entry.u()+entry.du()*(entry.length()+1),entry.v()+entry.dv()*(entry.length()+1)); }
    public Point point(int u,int v) { return new Point(worldX(u,v),worldZ(u,v)); }
    public boolean occupies(int wx,int wz,int margin) {
        int u=localU(wx,wz),v=localV(wx,wz);
        return entry.porch(u,v,margin) || rooms.stream().anyMatch(r->r.envelope(u,v,margin))
                || gallery().map(g->g.contains(u,v,margin)).orElse(false);
    }
    public Optional<Gallery> gallery() {
        return gallerySide==0?Optional.empty():gallery(rooms,style,gallerySide);
    }
    private static int defaultGallerySide(List<Room> rooms,Style style,long seed) {
        if(rooms.getFirst().storeys()<2 || style==Style.COURTYARD || ((seed&2)!=0 && style!=Style.LONGHOUSE && style!=Style.GREAT_HALL && style!=Style.WATCH_LODGE))return 0;
        for(int side:new int[]{1,-1})if(gallery(rooms,style,side).isPresent())return side;
        return 0;
    }
    private static Optional<Gallery> gallery(List<Room> rooms,Style style,int side) {
        var r=rooms.getFirst();
        {
            int wall=side>0?r.upperMaxU():r.upperU(),start=r.v()+3,end=r.maxV()-2;
            boolean clear=true;
            int lo=Math.min(wall+side,wall+side*2),hi=Math.max(wall+side,wall+side*2);
            for(int i=1;i<rooms.size();i++) {
                var other=rooms.get(i);
                if(lo<=other.maxU()+1 && hi>=other.u()-1 && start<=other.maxV()+1 && end>=other.v()-1)clear=false;
            }
            if(clear)return Optional.of(new Gallery(wall,side,start,end,r.floor()+(style==Style.WATCH_LODGE?10:5)));
        }
        return Optional.empty();
    }
    public List<Point> projection() {
        var points=new ArrayList<Point>();for(var g:ground)points.add(point(g.u(),g.v()));
        for(var r:rooms)for(int u=r.upperU();u<=r.upperMaxU();u++)for(int v=r.v();v<=r.maxV();v++)points.add(point(u,v));
        gallery().ifPresent(g->{for(int v=g.start();v<=g.end();v++)for(int a=1;a<=2;a++)points.add(point(g.wall()+g.side()*a,v));});
        return points;
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
        int width=9+2*random.nextInt(2),depth=13+2*random.nextInt(3),storeys=2;
        switch(style) {
            case LONGHOUSE -> {width=11+2*random.nextInt(2);depth=19+2*random.nextInt(3);storeys=1;}
            case HILLSIDE_LODGE -> {width=9;depth=13+2*random.nextInt(2);}
            case COURTYARD -> {width=9+2*random.nextInt(2);depth=15+2*random.nextInt(2);}
            case GREAT_HALL -> {width=13;depth=21;}
            case WATCH_LODGE -> {width=9;depth=11;storeys=3;}
            default -> {}
        }
        // Only a narrow core spans the slope; a long axis can follow its contour.
        var shell=new Room(-width/2,-depth/2,width,depth,0,storeys,false);
        var sample=sample(terrain,x,z,rotation,shell);
        int floor=sample.get((sample.size()*3)/4);
        if(!fits(sample,floor,sea))return Optional.empty();
        int facade=random.nextInt(4);
        var main=new Room(shell.u(),shell.v(),width,depth,floor,storeys,false,0,random.nextInt(3)-1,facade);rooms.add(main);
        int side=random.nextBoolean()?1:-1;
        int wings=switch(style){case COURTYARD,GREAT_HALL->2;case CROSS_GABLE->(seed&3)==0?2:1;default->1;};
        for(int i=0;i<wings;i++) {
            int sign=i==0?side:-side,ww=7+2*random.nextInt(2),dd=9+2*random.nextInt(2);
            int wu=sign>0?main.maxU():main.u()-ww+1;
            int wv=main.v()+Math.max(1,depth-dd-2);
            boolean cross=true;
            if(style==Style.COURTYARD){dd=13+2*random.nextInt(3);wv=main.maxV()-5;cross=i==0;}
            if(style==Style.CROSS_GABLE){dd=11+2*random.nextInt(3);wv=main.v()-2-random.nextInt(3);cross=false;}
            if(style==Style.HILLSIDE_LODGE){ww=7;wu=sign>0?main.maxU():main.u()-ww+1;dd=15;wv=main.maxV()-6;cross=false;}
            if(style==Style.GREAT_HALL){dd=i==0?13:9;wv=i==0?main.v()-3:main.maxV()-5;}
            if(style==Style.LONGHOUSE){dd=9;wv=main.maxV()-6;}
            int ws=(style==Style.LONGHOUSE || (style==Style.CROSS_GABLE && (seed&1)==0)
                    || (style==Style.GREAT_HALL && i==0) || (style==Style.COURTYARD && i==0))?2:1;
            if(ws==2){ww=Math.max(9,ww);dd=Math.max(11,dd);wu=sign>0?main.maxU():main.u()-ww+1;}
            // Fit attached volumes to their own shelves. The chosen side, offset and length
            // respond to actual earthwork rather than copying the seed's preferred rectangle.
            Room bestWing=null;long bestCost=Long.MAX_VALUE;int preferred=wv;
            int roofShift=random.nextInt(3)-1;
            for(int candidateSide:i==0?new int[]{sign,-sign}:new int[]{-side}) {
                for(int offset:new int[]{0,-3,3})for(int shorten:new int[]{0,2}) {
                    int cd=Math.max(ws==2?11:9,dd-shorten),cv=preferred+offset;
                    if(Math.min(main.maxV(),cv+cd-1)-Math.max(main.v(),cv)<5)continue;
                    if(i>0 && Math.min(rooms.get(1).maxV()-2,main.maxV()-3)<=main.v()+7
                            && Math.min(cv+cd-3,main.maxV()-3)<=main.v()+7)continue;
                    int cu=candidateSide>0?main.maxU():main.u()-ww+1;
                    var raw=new Room(cu,cv,ww,cd,0,ws,cross);
                    var heights=sample(terrain,x,z,rotation,raw);
                    int wf=Math.clamp(heights.get(heights.size()*3/4),floor-2,floor+2);
                    if(!fits(heights,wf,sea))continue;
                    long cost=heights.stream().mapToLong(h->h>wf?(h-wf)*4L:wf-h).sum()*20/heights.size()
                            +Math.abs(offset)+(candidateSide==sign?0:2)+shorten;
                    if(cost<bestCost) {
                        bestCost=cost;
                        int shed=ws==1 && ((seed+i)&3)!=0?candidateSide:0;
                        bestWing=new Room(cu,cv,ww,cd,wf,ws,shed==0 && cross,ws>1?candidateSide:0,
                                roofShift,(facade+i+1)%4,shed);
                    }
                }
            }
            if(bestWing==null)return Optional.empty();
            if(i==0)side=bestWing.u()==main.maxU()?1:-1;
            rooms.add(bestWing);
        }
        // Project only into the free side, retaining rock below rather than widening the excavation.
        if(wings==1 && storeys>1) {
            main=new Room(main.u(),main.v(),width,depth,floor,storeys,false,-side,main.roofShift(),facade);
            rooms.set(0,main);
        }
        // An uphill side entry is often a better fit than a door fixed to the gable end.
        var entrances=new ArrayList<Entry>();
        int porch=1+random.nextInt(3);
        entrances.add(new Entry((seed&4)==0?0:1,main.maxV(),0,1,floor,porch));
        entrances.add(new Entry(0,main.v(),0,-1,floor,porch));
        entrances.add(new Entry(main.maxU(),main.maxV()-3,1,0,floor,porch));
        entrances.add(new Entry(main.u(),main.maxV()-3,-1,0,floor,porch));
        entrances.sort(Comparator.comparingInt(e->{
            var p=transform(x,z,rotation,e.u()+e.du()*(e.length()+1),e.v()+e.dv()*(e.length()+1));
            return Math.abs(terrain.height(p.x(),p.z())-1-floor)*12
                    +(Math.abs(p.x()-streetTarget.x())+Math.abs(p.z()-streetTarget.z()))/3;
        }));
        for(var entry:entrances) {
            boolean valid=true; var ground=new LinkedHashMap<Point,Ground>();
            for(var r:rooms)for(int u=r.u();u<=r.maxU();u++)for(int v=r.v();v<=r.maxV();v++) {
                var p=transform(x,z,rotation,u,v);
                ground.put(new Point(u,v),new Ground(u,v,terrain.height(p.x(),p.z())-1));
            }
            for(int a=1;a<=entry.length()+1;a++)for(int b=-2;b<=2;b++) {
                int u=entry.u()+entry.du()*a+entry.dv()*b,v=entry.v()+entry.dv()*a-entry.du()*b;
                if(rooms.stream().anyMatch(r->r.contains(u,v,1)) && a>=2)valid=false;
                var p=transform(x,z,rotation,u,v);int h=terrain.height(p.x(),p.z())-1;
                if(h<sea-1 || floor-h>3 || h-floor>2)valid=false;
                if(a<=entry.length())ground.put(new Point(u,v),new Ground(u,v,h));
            }
            if(!valid)continue;
            int gallerySide=0,best=Integer.MAX_VALUE;
            if(defaultGallerySide(rooms,style,seed)!=0)for(int direction:new int[]{1,-1}) {
                var proposed=gallery(rooms,style,direction);if(proposed.isEmpty())continue;
                var g=proposed.get();int score=0;boolean clear=true;
                for(int v=g.start();v<=g.end();v++)for(int a=1;a<=2;a++) {
                    var p=transform(x,z,rotation,g.wall()+g.side()*a,v);int h=terrain.height(p.x(),p.z())-1;
                    score+=h;if(h>=g.floor())clear=false;
                }
                if(clear && score<best){best=score;gallerySide=direction;}
            }
            return Optional.of(new MountainBuildingPlan(x,z,rotation,seed,style,rooms,entry,new ArrayList<>(ground.values()),
                    sample.getFirst()<=floor-3 && style!=Style.WATCH_LODGE,gallerySide));
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
