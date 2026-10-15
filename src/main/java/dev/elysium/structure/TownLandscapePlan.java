package dev.elysium.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import dev.elysium.structure.MountainBuildingPlan.Point;

/** Trees are reserved before houses; small edge works retain surveyed terrain heights. */
public record TownLandscapePlan(List<Tree> trees,List<Detail> details,List<Obstacle> obstacles) {
    public enum Kind { BIRCH_Y, BIRCH_X, BIRCH_Z, LEAVES, STONE, CAP, ROCK, FLOWER, LITTER, POST, LANTERN, ROCK_SHADE }
    public record Cell(int x,int y,int z) {}
    public record Detail(int x,int y,int z,Kind kind) {}
    public record Obstacle(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public boolean contains(Cell p) {return p.x()>=minX && p.x()<=maxX && p.z()>=minZ && p.z()<=maxZ && p.y()>=minY && p.y()<=maxY;}
    }
    public record Tree(int x,int y,int z,int height,int radius,long seed,int form) {
        public Tree {if(height<14 || height>32 || radius<4 || radius>7 || form<1 || form>3)throw new IllegalArgumentException("Invalid settlement birch");}
        public Tree(int x,int y,int z,int height,int radius,long seed){this(x,y,z,height,radius,seed,3);}
        public boolean root(int px,int pz,int margin) {return Math.abs(px-x)<=margin && Math.abs(pz-z)<=margin;}
        public Map<Cell,Kind> geometry() {
            if(form==3)return layeredGeometry();
            if(form==2)return branchingGeometry();
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
        private Map<Cell,Kind> layeredGeometry() {
            var out=new LinkedHashMap<Cell,Kind>();var random=new Random(seed);
            int fork=4+random.nextInt(2);
            double turn=random.nextDouble()*Math.PI*2;
            // A tall, offset leader and lower side crowns give the tree depth.
            // Fractional centres and unequal heights avoid shared flat canopy rows.
            int leaderX=x+random.nextInt(3)-1,leaderZ=z+random.nextInt(3)-1;
            lobe(out,leaderX+.3,y+height-4.6,leaderZ-.2,3.1,4.7,3.4,seed);
            branch(out,x,y+fork,z,leaderX,y+height-6,leaderZ);
            for(int i=0;i<3;i++) {
                double angle=turn+i*2.12+random.nextDouble()*.35;
                int dx=(int)Math.round(Math.cos(angle)*(radius-1)),dz=(int)Math.round(Math.sin(angle)*(radius-1));
                double cy=y+Math.max(9.0,height-6.5-i*1.6+random.nextDouble());
                double rx=2.6+random.nextDouble(),rz=2.8+random.nextDouble(),ry=3.6+random.nextDouble();
                lobe(out,x+dx+.25,cy,z+dz-.3,rx,ry,rz,seed+i*37);
                // A lower, smaller cluster breaks the outline without widening the crown.
                lobe(out,x+dx*.7,cy-2.0,z+dz*.7,rx*.72,2.6,rz*.74,seed+i*71);
                branch(out,x,y+fork+i%2,z,x+dx,(int)cy-1,z+dz);
            }
            for(int h=0;h<=fork+1;h++)out.put(new Cell(x,y+h,z),Kind.BIRCH_Y);
            int side=(seed&1)==0?1:-1;
            for(int h=0;h<3;h++)out.put(new Cell(x+side,y+h,z),Kind.BIRCH_Y);
            return out;
        }
        private static void lobe(Map<Cell,Kind> out,double x,double y,double z,double rx,double ry,double rz,long seed) {
            for(int a=(int)Math.floor(x-rx-1);a<=Math.ceil(x+rx+1);a++)
                for(int b=(int)Math.floor(y-ry-1);b<=Math.ceil(y+ry+1);b++)
                    for(int c=(int)Math.floor(z-rz-1);c<=Math.ceil(z+rz+1);c++) {
                        double dx=(a-x)/rx,dy=(b-y)/ry,dz=(c-z)/rz;
                        double edge=1+.09*Math.sin((a-x)*1.3+(b-y)*.8+(seed&15))+.06*Math.cos((c-z)*1.7-(b-y));
                        if(dx*dx+dy*dy+dz*dz<=edge)out.putIfAbsent(new Cell(a,b,c),Kind.LEAVES);
                    }
        }
        private Map<Cell,Kind> branchingGeometry() {
            var out=new LinkedHashMap<Cell,Kind>();var random=new Random(seed);
            int fork=4+random.nextInt(3),arms=3+random.nextInt(2);
            double turn=random.nextDouble()*Math.PI*2;
            // Unequal upright leaders carry separate crowns. Their first forks stay visible
            // below foliage instead of ending a long pole in a single flat canopy.
            for(int i=0;i<arms;i++) {
                double angle=turn+i*Math.PI*2/arms+random.nextDouble()*.45;
                int dx=(int)Math.round(Math.cos(angle)*(radius-1)),dz=(int)Math.round(Math.sin(angle)*(radius-1));
                int tip=y+height-4-random.nextInt(4),rx=radius-2+random.nextInt(2),rz=radius-2+random.nextInt(2);
                crown(out,x+dx,tip,z+dz,rx,3+random.nextInt(2),rz);
                branch(out,x,y+fork+i%2,z,x+dx,tip-1,z+dz);
            }
            for(int h=0;h<=fork+1;h++)out.put(new Cell(x,y+h,z),Kind.BIRCH_Y);
            int side=(seed&1)==0?1:-1;
            for(int h=0;h<3;h++)out.put(new Cell(x+side,y+h,z),Kind.BIRCH_Y);
            return out;
        }
        private static void branch(Map<Cell,Kind> out,int x,int y,int z,int tx,int ty,int tz) {
            int sx=x,sy=y,sz=z,n=Math.max(Math.max(Math.abs(tx-x),Math.abs(tz-z)),ty-y);
            out.put(new Cell(x,y,z),Kind.BIRCH_Y);
            for(int i=1;i<=n;i++) {
                int nx=sx+(tx-sx)*i/n,ny=sy+(ty-sy)*i/n,nz=sz+(tz-sz)*i/n;
                while(y<ny)out.put(new Cell(x,++y,z),Kind.BIRCH_Y);
                while(x!=nx){x+=Integer.signum(nx-x);out.put(new Cell(x,y,z),Kind.BIRCH_X);}
                while(z!=nz){z+=Integer.signum(nz-z);out.put(new Cell(x,y,z),Kind.BIRCH_Z);}
            }
        }
        private static void crown(Map<Cell,Kind> out,int x,int y,int z,int rx,int ry,int rz) {
            for(int dx=-rx;dx<=rx;dx++)for(int dy=-ry;dy<=ry;dy++)for(int dz=-rz;dz<=rz;dz++) {
                double d=dx*dx/(double)(rx*rx)+dy*dy/(double)(ry*ry)+dz*dz/(double)(rz*rz);
                if(d<=1.0 && !(d>.9 && Math.floorMod(x+dx+(z+dz)*13+(y+dy)*7,9)==0))out.putIfAbsent(new Cell(x+dx,y+dy,z+dz),Kind.LEAVES);
            }
        }
    }
    public TownLandscapePlan {
        trees=List.copyOf(trees);details=List.copyOf(details);obstacles=List.copyOf(obstacles);
        if(trees.stream().map(Tree::form).distinct().count()>1)throw new IllegalArgumentException("Mixed tree grammars in one piece");
    }
    public static List<Tree> reserve(LandscapePlanner.Terrain terrain,int cx,int cz,int sea,long seed) {
        var trees=new ArrayList<Tree>();var random=new Random(seed^0x5EEDB1A7L);double angle=random.nextDouble()*6.28;
        for(int i=0;i<120 && trees.size()<8;i++) {
            double a=angle+i*2.3999632297,r=22+67*Math.sqrt((i+.5)/120);
            int x=cx+(int)Math.round(Math.cos(a)*r),z=cz+(int)Math.round(Math.sin(a)*r),y=terrain.height(x,z);
            if(y<sea+2 || y>sea+46 || terrain.water(x,z))continue;
            boolean clear=true;
            for(var tree:trees)if(Math.hypot(x-tree.x(),z-tree.z())<24)clear=false;
            for(int dx:new int[]{-2,0,2})for(int dz:new int[]{-2,0,2})if(Math.abs(terrain.height(x+dx,z+dz)-y)>3)clear=false;
            if(clear)trees.add(new Tree(x,y,z,16+random.nextInt(5),5+random.nextInt(2),random.nextLong()));
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
        var trees=reserved.stream().filter(t->!square.contains(t.x(),t.z()) && buildings.stream().noneMatch(b->b.occupies(t.x(),t.z(),3)))
                .map(t->clearBranches(t,obstacles)).flatMap(java.util.Optional::stream).toList();
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
        buildingEdges(terrain,buildings,trees,square,roads,details,sea);
        for(var tree:trees)for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
            int x=tree.x()+dx,z=tree.z()+dz,y=terrain.height(x,z);
            if(dx*dx+dz*dz>25 || dx*dx+dz*dz<3 || y<sea || Math.abs(y-tree.y())>4 || roads.contains(new Point(x,z))
                    || buildings.stream().anyMatch(b->b.occupies(x,z,2)))continue;
            int choice=Math.floorMod(x*17+z*31,11);
            if(choice<2 && details.keySet().stream().noneMatch(p->p.x()==x && p.z()==z && p.y()>=y))
                detail(details,x,y,z,choice==0?Kind.FLOWER:Kind.LITTER);
        }
        // Occasional lamps stand beside, never on, the circulation graph.
        for(var c:streets)if(c.kind()==ValleyTownPlanner.ROAD && Math.floorMod(c.x()*7+c.z()*13,67)==0) {
            int x=c.x()+2,z=c.z(),y=terrain.height(x,z);
            if(y<sea || Math.abs(y-c.ground()-1)>1 || roads.contains(new Point(x,z)) || !free(buildings,trees,square,x,z))continue;
            for(int h=0;h<3;h++)detail(details,x,y+h,z,Kind.POST);
            detail(details,x,y+3,z,Kind.LANTERN);
        }
        // Edge works must also respect overhanging eaves, bays and open galleries,
        // which are not ground footprints. Remove conflicts with the exact grammar.
        for(var b:buildings)for(var cell:MountainArchitecture.build(b).keySet())
            details.remove(new Cell(b.worldX(cell.u(),cell.v()),cell.y(),b.worldZ(cell.u(),cell.v())));
        return new TownLandscapePlan(trees,new ArrayList<>(details.values()),obstacles);
    }
    /** Narrow retaining shelves and exposed uphill rock follow the surveyed house edges. */
    private static void buildingEdges(LandscapePlanner.Terrain terrain,List<MountainBuildingPlan> buildings,List<Tree> trees,
            TerracePlanner.Lot square,java.util.Set<Point> roads,Map<Cell,Detail> details,int sea) {
        for(var b:buildings)for(var r:b.rooms())for(int side:new int[]{-1,1}) {
            int wall=side<0?r.u():r.maxU(),centre=r.v()+r.depth()/2;
            for(int v=r.v()-2;v<=r.maxV()+2;v++)for(int a=1;a<=5;a++) {
                var p=b.point(wall+side*a,v);int x=p.x(),z=p.z(),ground=terrain.height(x,z)-1;
                if(ground<sea || roads.contains(p) || b.access(x,z) || square.distance(x,z)<=2
                        || trees.stream().anyMatch(t->t.root(x,z,1)) || buildings.stream().anyMatch(other->other.occupies(x,z,0)))continue;
                int difference=ground-r.floor();
                if(difference>=2 && difference<=12 && Math.abs(v-centre)<=r.depth()/2+1-a/2) {
                    // Reface only existing soil/stone: preserve the actual hill's silhouette.
                    Kind rock=Math.floorMod(Math.floorDiv(x,4)+Math.floorDiv(z,5),7)==0?Kind.ROCK_SHADE:Kind.ROCK;
                    for(int y=Math.max(r.floor()-1,ground-6);y<=ground;y++)detail(details,x,y,z,rock);
                } else if(a<=2 && r.jetty()!=side && difference<=-2 && difference>=-5 && Math.abs(v-centre)<=3+(int)(b.seed()&1)) {
                    // A short terrace extends the foundation, never a level plot around the house.
                    for(int y=ground;y<=r.floor();y++)detail(details,x,y,z,Kind.STONE);
                    if(a==2)detail(details,x,r.floor()+1,z,Kind.CAP);
                }
            }
        }
    }
    private static boolean free(List<MountainBuildingPlan> buildings,List<Tree> trees,TerracePlanner.Lot square,int x,int z) {
        return square.distance(x,z)>2 && buildings.stream().noneMatch(b->b.occupies(x,z,1)) && trees.stream().noneMatch(t->t.root(x,z,1));
    }
    private static java.util.Optional<Tree> clearBranches(Tree tree,List<Obstacle> obstacles) {
        for(int n=0;n<8;n++) {
            var t=new Tree(tree.x(),tree.y(),tree.z(),tree.height(),tree.radius(),tree.seed()+n*0x5DEECE66DL,tree.form());
            boolean clear=t.geometry().entrySet().stream().filter(e->e.getValue()!=Kind.LEAVES)
                    .noneMatch(e->obstacles.stream().anyMatch(o->o.contains(e.getKey())));
            if(clear)return java.util.Optional.of(t);
        }
        return java.util.Optional.empty();
    }
    private static void detail(Map<Cell,Detail> out,int x,int y,int z,Kind kind) {out.put(new Cell(x,y,z),new Detail(x,y,z,kind));}
}
