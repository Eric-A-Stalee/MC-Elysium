package dev.elysium.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import dev.elysium.structure.MountainBuildingPlan.Point;

/** Trees are reserved before houses; small edge works retain surveyed terrain heights. */
public record TownLandscapePlan(List<Tree> trees,List<Detail> details,List<Obstacle> obstacles) {
    public enum Kind { BIRCH_Y, BIRCH_X, BIRCH_Z, LEAVES, STONE, CAP, ROCK, FLOWER, LITTER, POST, LANTERN }
    public record Cell(int x,int y,int z) {}
    public record Detail(int x,int y,int z,Kind kind) {}
    public record Obstacle(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public boolean contains(Cell p) {return p.x()>=minX && p.x()<=maxX && p.z()>=minZ && p.z()<=maxZ && p.y()>=minY && p.y()<=maxY;}
    }
    public record Tree(int x,int y,int z,int height,int radius,long seed) {
        public Tree {if(height<18 || height>32 || radius<4 || radius>7)throw new IllegalArgumentException("Invalid settlement birch");}
        public boolean root(int px,int pz,int margin) {return Math.abs(px-x)<=margin && Math.abs(pz-z)<=margin;}
        public Map<Cell,Kind> geometry() {
            var out=new LinkedHashMap<Cell,Kind>();var random=new Random(seed);
            // Overlapping full crowns and a tall clear trunk leave usable space below.
            crown(out,x,y+height-4,z,radius,5,radius);
            for(int i=0;i<3;i++) {
                double angle=i*Math.PI*2/3+random.nextDouble()*.65;
                int dx=(int)Math.round(Math.cos(angle)*(radius-2)),dz=(int)Math.round(Math.sin(angle)*(radius-2));
                int by=y+height-10+i;
                crown(out,x+dx,by+3,z+dz,radius-1,4,radius-1);
                int length=Math.max(Math.abs(dx),Math.abs(dz));
                for(int n=0;n<=length;n++)out.put(new Cell(x+dx*n/length,by+n/2,z+dz*n/length),
                        Math.abs(dx)>Math.abs(dz)?Kind.BIRCH_X:Kind.BIRCH_Z);
            }
            for(int h=0;h<height-2;h++)out.put(new Cell(x,y+h,z),Kind.BIRCH_Y);
            return out;
        }
        private static void crown(Map<Cell,Kind> out,int x,int y,int z,int rx,int ry,int rz) {
            for(int dx=-rx;dx<=rx;dx++)for(int dy=-ry;dy<=ry;dy++)for(int dz=-rz;dz<=rz;dz++) {
                double d=dx*dx/(double)(rx*rx)+dy*dy/(double)(ry*ry)+dz*dz/(double)(rz*rz);
                if(d<=1.0 && !(d>.9 && Math.floorMod(x+dx+(z+dz)*13+(y+dy)*7,9)==0))out.putIfAbsent(new Cell(x+dx,y+dy,z+dz),Kind.LEAVES);
            }
        }
    }
    public TownLandscapePlan {trees=List.copyOf(trees);details=List.copyOf(details);obstacles=List.copyOf(obstacles);}
    public static List<Tree> reserve(LandscapePlanner.Terrain terrain,int cx,int cz,int sea,long seed) {
        var trees=new ArrayList<Tree>();var random=new Random(seed^0x5EEDB1A7L);double angle=random.nextDouble()*6.28;
        for(int i=0;i<120 && trees.size()<8;i++) {
            double a=angle+i*2.3999632297,r=22+67*Math.sqrt((i+.5)/120);
            int x=cx+(int)Math.round(Math.cos(a)*r),z=cz+(int)Math.round(Math.sin(a)*r),y=terrain.height(x,z);
            if(y<sea+2 || y>sea+46 || terrain.water(x,z))continue;
            boolean clear=true;
            for(var tree:trees)if(Math.hypot(x-tree.x(),z-tree.z())<24)clear=false;
            for(int dx:new int[]{-2,0,2})for(int dz:new int[]{-2,0,2})if(Math.abs(terrain.height(x+dx,z+dz)-y)>3)clear=false;
            if(clear)trees.add(new Tree(x,y,z,24+random.nextInt(5),5+random.nextInt(2),random.nextLong()));
        }
        return List.copyOf(trees);
    }
    public static TownLandscapePlan finish(LandscapePlanner.Terrain terrain,List<Tree> reserved,List<MountainBuildingPlan> buildings,
            List<ValleyTownPlanner.Column> streets,TerracePlanner.Lot square,int sea) {
        var obstacles=new ArrayList<Obstacle>();
        for(var b:buildings)for(var r:b.rooms()) {
            var a=b.point(r.upperU()-2,r.v()-2);var c=b.point(r.upperMaxU()+2,r.maxV()+2);
            obstacles.add(new Obstacle(Math.min(a.x(),c.x()),b.minFloor(),Math.min(a.z(),c.z()),
                    Math.max(a.x(),c.x()),r.ridge()+2,Math.max(a.z(),c.z())));
        }
        obstacles.add(new Obstacle(square.x()-6,square.ground(),square.z()-6,square.x()+6,square.ground()+8,square.z()+6));
        var trees=reserved.stream().filter(t->!square.contains(t.x(),t.z()) && buildings.stream().noneMatch(b->b.occupies(t.x(),t.z(),3))).toList();
        var roads=new java.util.HashSet<Point>();for(var c:streets)roads.add(new Point(c.x(),c.z()));
        var details=new LinkedHashMap<Cell,Detail>();
        for(var c:streets)if(c.kind()==ValleyTownPlanner.ROAD)for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
            int x=c.x()+d[0],z=c.z()+d[1],ground=terrain.height(x,z)-1;
            if(roads.contains(new Point(x,z)) || !free(buildings,trees,square,x,z))continue;
            boolean accent=Math.floorMod(Math.floorDiv(x,6)+Math.floorDiv(z,6),3)==0;
            if(ground>=sea && c.ground()-ground>=1 && c.ground()-ground<=5 && (c.ground()-ground>=2 || accent)) {
                for(int y=ground;y<c.ground();y++)detail(details,x,y,z,Kind.STONE);
                detail(details,x,c.ground(),z,Kind.CAP);
            } else if(ground>=c.ground()+1 && ground<=c.ground()+5 && (ground-c.ground()>=2 || accent)) {
                for(int y=c.ground();y<=ground;y++)detail(details,x,y,z,Kind.ROCK);
            }
            int px=x+d[0],pz=z+d[1],py=terrain.height(px,pz);
            if(py>=sea && Math.floorMod(px*31+pz*17,13)==0 && free(buildings,trees,square,px,pz)
                    && !roads.contains(new Point(px,pz)))detail(details,px,py,pz,Kind.FLOWER);
        }
        for(var tree:trees)for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
            int x=tree.x()+dx,z=tree.z()+dz,y=terrain.height(x,z);
            if(dx*dx+dz*dz>25 || dx*dx+dz*dz<3 || y<sea || Math.abs(y-tree.y())>4 || roads.contains(new Point(x,z))
                    || buildings.stream().anyMatch(b->b.occupies(x,z,2)))continue;
            int choice=Math.floorMod(x*17+z*31,11);
            if(choice<2)detail(details,x,y,z,choice==0?Kind.FLOWER:Kind.LITTER);
        }
        // Occasional lamps stand beside, never on, the circulation graph.
        for(var c:streets)if(c.kind()==ValleyTownPlanner.ROAD && Math.floorMod(c.x()*7+c.z()*13,67)==0) {
            int x=c.x()+2,z=c.z(),y=terrain.height(x,z);
            if(y<sea || Math.abs(y-c.ground()-1)>1 || roads.contains(new Point(x,z)) || !free(buildings,trees,square,x,z))continue;
            for(int h=0;h<3;h++)detail(details,x,y+h,z,Kind.POST);
            detail(details,x,y+3,z,Kind.LANTERN);
        }
        return new TownLandscapePlan(trees,new ArrayList<>(details.values()),obstacles);
    }
    private static boolean free(List<MountainBuildingPlan> buildings,List<Tree> trees,TerracePlanner.Lot square,int x,int z) {
        return square.distance(x,z)>2 && buildings.stream().noneMatch(b->b.occupies(x,z,1)) && trees.stream().noneMatch(t->t.root(x,z,1));
    }
    private static void detail(Map<Cell,Detail> out,int x,int y,int z,Kind kind) {out.put(new Cell(x,y,z),new Detail(x,y,z,kind));}
}
