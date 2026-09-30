package dev.elysium.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import dev.elysium.structure.MountainBuildingPlan.Point;
import dev.elysium.structure.MountainBuildingPlan.Style;
import dev.elysium.structure.ValleyTownPlanner.Column;

/** A bounded survey grows streets and irregular clusters through existing mountain shelves. */
public final class ContourTownPlanner {
    public static final int REACH=110, MAX_ROAD_GRADE=3, NODE_BUDGET=5000, SITE_BUDGET=240;
    private static final int[][] STEPS={{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};
    public record Plan(List<MountainBuildingPlan> buildings,List<Column> streets,TerracePlanner.Lot square,BridgePlanner.Span bridge) {
        public Plan { buildings=List.copyOf(buildings);streets=List.copyOf(streets); }
    }
    private record Site(Point at,long seed,int height,int relief) {}
    private record Access(Point street,Point approach,int floor) {}
    private record Node(Point at,int cost,int score) {}
    private ContourTownPlanner() {}

    public static Optional<Plan> plan(LandscapePlanner.Terrain terrain,BridgePlanner.Span bridge,int sea,long seed,
            List<Style> styles,int minimum,int maximum,Consumer<String> survey) {
        int cx=bridge.x(bridge.length()/2,0),cz=bridge.z(bridge.length()/2,0);
        var roads=new LinkedHashMap<Point,Column>();var buildings=new ArrayList<MountainBuildingPlan>();
        for(int a=0;a<bridge.length();a++)for(int b=-1;b<=1;b++) {
            int x=bridge.x(a,b),z=bridge.z(a,b);
            roads.put(new Point(x,z),new Column(x,z,bridge.walkingHeight(a)-1,bridge.floorHeight(a,b)-1,ValleyTownPlanner.CROSSING));
        }
        TerracePlanner.Lot square=null;
        squareSearch:for(int side:new int[]{1,-1})for(int offset:new int[]{0,6,-6}) {
            int a=side>0?bridge.length()-1:0;
            int x=bridge.x(a,offset)+(bridge.eastWest()?side*9:0),z=bridge.z(a,offset)+(bridge.eastWest()?0:side*9);
            var pad=ValleyTownPlanner.pad(terrain,x,z,5,sea,5);
            if(pad.isEmpty())continue;
            var face=bridge.eastWest()?(side>0?Direction.WEST:Direction.EAST):(side>0?Direction.NORTH:Direction.SOUTH);
            var proposed=new TerracePlanner.Lot(x,z,pad.getAsInt(),5,face,-1);
            var d=proposed.door();var p=proposed.approach();
            if(connect(terrain,roads,buildings,proposed,null,new Access(new Point(d.x(),d.z()),new Point(p.x(),p.z()),proposed.ground()),cx,cz,sea)) {
                square=proposed;break squareSearch;
            }
        }
        if(square==null){survey.accept("no_square");return Optional.empty();}
        var random=new Random(seed);var sites=new ArrayList<Site>();double angle=random.nextDouble()*Math.PI*2;
        // Low-discrepancy positions have no rows or repeating plot pitch. Terrain and streets rank them next.
        for(int i=0;i<SITE_BUDGET;i++) {
            double a=angle+i*2.399963229728653,r=14+86*Math.sqrt((i+.5)/SITE_BUDGET);
            int x=cx+(int)Math.round(Math.cos(a)*r),z=cz+(int)Math.round(Math.sin(a)*r);
            int h=terrain.height(x,z)-1;
            if(h<sea || h>sea+56)continue;
            int lo=h,hi=h;
            for(int[] d:new int[][]{{8,0},{-8,0},{0,8},{0,-8}}) {int v=terrain.height(x+d[0],z+d[1])-1;lo=Math.min(lo,v);hi=Math.max(hi,v);}
            sites.add(new Site(new Point(x,z),random.nextLong(),h,hi-lo));
        }
        var halls=new ArrayList<>(sites);
        halls.removeIf(s->s.height()<sea+11 || s.relief()>20);
        halls.sort(Comparator.comparingInt((Site s)->s.height()-s.relief()/2).reversed());
        for(var s:halls)if(add(terrain,roads,buildings,square,s,Style.GREAT_HALL,cx,cz,sea))break;
        if(buildings.isEmpty()){survey.accept("no_hillside_hall");return Optional.empty();}
        var lookouts=new ArrayList<>(sites);
        lookouts.sort(Comparator.comparingInt((Site s)->s.height()+s.relief()).reversed());
        for(var s:lookouts)if(s.height()>sea+8 && add(terrain,roads,buildings,square,s,Style.WATCH_LODGE,cx,cz,sea))break;
        if(buildings.size()!=2){survey.accept("no_watch_lodge");return Optional.empty();}
        int houses=0;var remaining=new ArrayList<>(sites);
        while(!remaining.isEmpty() && houses<maximum) {
            // Prefer the growing street edge, retaining a bias toward inhabited slopes and outcrops.
            int best=0,bestScore=Integer.MAX_VALUE;
            for(int i=0;i<remaining.size();i++) {int score=siteScore(remaining.get(i),roads,sea);if(score<bestScore){best=i;bestScore=score;}}
            var site=remaining.remove(best);
            var family=styles.get(Math.floorMod((int)site.seed(),styles.size()));
            if(add(terrain,roads,buildings,square,site,family,cx,cz,sea))houses++;
        }
        if(houses<minimum){survey.accept("adaptive_houses_"+houses);return Optional.empty();}
        long near=buildings.stream().filter(MountainBuildingPlan::home).filter(b->bridge.eastWest()?b.x()<cx:b.z()<cz).count();
        long high=buildings.stream().filter(MountainBuildingPlan::home).filter(b->b.main().floor()>=sea+8).count();
        long rock=buildings.stream().filter(b->besideRock(terrain,b)).count();
        long families=buildings.stream().filter(MountainBuildingPlan::home).map(MountainBuildingPlan::style).distinct().count();
        if(near<3 || houses-near<3){survey.accept("one_bank");return Optional.empty();}
        if(high<4 || rock<2){survey.accept("insufficient_mountain_integration");return Optional.empty();}
        if(families<Math.min(3,styles.size())){survey.accept("insufficient_building_variety");return Optional.empty();}
        var saved=new ArrayList<Column>();
        for(int x=square.x()-5;x<=square.x()+5;x++)for(int z=square.z()-5;z<=square.z()+5;z++)
            saved.add(new Column(x,z,square.ground(),terrain.height(x,z)-1,ValleyTownPlanner.COURT));
        for(var c:roads.values())if(c.kind()!=ValleyTownPlanner.CROSSING)saved.add(c);
        return Optional.of(new Plan(buildings,saved,square,bridge));
    }
    private static int siteScore(Site s,Map<Point,Column> roads,int sea) {
        int distance=roads.keySet().stream().mapToInt(p->distance(p,s.at())).min().orElseThrow();
        return distance*3+Math.max(0,s.relief()-12)*3-(s.height()>sea+8?14:0);
    }
    public static boolean besideRock(LandscapePlanner.Terrain terrain,MountainBuildingPlan b) {
        var r=b.main();int hi=b.main().floor();
        for(var p:List.of(b.point(r.u()-8,0),b.point(r.maxU()+8,0),b.point(0,r.v()-8),b.point(0,r.maxV()+8)))
            hi=Math.max(hi,terrain.height(p.x(),p.z())-1);
        return hi-b.main().floor()>=7;
    }
    private static boolean add(LandscapePlanner.Terrain terrain,Map<Point,Column> roads,List<MountainBuildingPlan> buildings,
            TerracePlanner.Lot square,Site site,Style style,int cx,int cz,int sea) {
        if(buildings.stream().anyMatch(b->b.occupies(site.at().x(),site.at().z(),6)))return false;
        var target=roads.keySet().stream().min(Comparator.comparingInt(p->distance(p,site.at()))).orElseThrow();
        for(int n=0;n<4;n++) {
            int rotation=Math.floorMod((int)site.seed()+n,4);
            var result=MountainBuildingPlan.fit(terrain,site.at().x(),site.at().z(),rotation,site.seed(),style,sea,target);
            if(result.isEmpty())continue;var b=result.get();
            if(!available(b,buildings,square,roads,cx,cz))continue;
            if(connect(terrain,roads,buildings,square,b,new Access(b.street(),b.approach(),b.entry().floor()),cx,cz,sea)) {
                buildings.add(b);return true;
            }
        }
        return false;
    }
    private static boolean available(MountainBuildingPlan b,List<MountainBuildingPlan> buildings,TerracePlanner.Lot square,
            Map<Point,Column> roads,int cx,int cz) {
        for(var g:b.ground()) {
            int x=b.worldX(g.u(),g.v()),z=b.worldZ(g.u(),g.v());
            if(Math.abs(x-cx)>REACH-3 || Math.abs(z-cz)>REACH-3 || square.distance(x,z)<=3)return false;
            for(var other:buildings)if(other.occupies(x,z,3))return false;
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(roads.containsKey(new Point(x+dx,z+dz)))return false;
        }
        return true;
    }
    private static int distance(Point a,Point b) { return Math.abs(a.x()-b.x())+Math.abs(a.z()-b.z()); }
    private static int grade(LandscapePlanner.Terrain terrain,Map<Point,Column> roads,Access access,Point p) {
        var existing=roads.get(p);if(existing!=null)return existing.ground();
        int original=terrain.height(p.x(),p.z())-1,d=distance(p,access.street());
        return d<=4?Math.clamp(original,access.floor()-d,access.floor()+d):original;
    }
    private static boolean passable(LandscapePlanner.Terrain terrain,Map<Point,Column> roads,List<MountainBuildingPlan> buildings,
            TerracePlanner.Lot square,MountainBuildingPlan candidate,Access access,Point p,int cx,int cz,int sea) {
        if(Math.abs(p.x()-cx)>REACH-1 || Math.abs(p.z()-cz)>REACH-1)return false;
        int center=grade(terrain,roads,access,p);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var q=new Point(p.x()+dx,p.z()+dz);
            if(square.contains(q.x(),q.z()) && !square.entrance(q.x(),q.z()))return false;
            for(var b:buildings)if(b.occupies(q.x(),q.z(),1))return false;
            if(candidate!=null && candidate.occupies(q.x(),q.z(),1) && !candidate.access(q.x(),q.z()))return false;
            var saved=roads.get(q);if(saved!=null && saved.kind()==ValleyTownPlanner.CROSSING)continue;
            int original=terrain.height(q.x(),q.z())-1,top=grade(terrain,roads,access,q);
            if(original<sea-1 || Math.abs(top-original)>MAX_ROAD_GRADE || Math.abs(top-center)>1)return false;
        }
        return true;
    }
    private static boolean connect(LandscapePlanner.Terrain terrain,Map<Point,Column> roads,List<MountainBuildingPlan> buildings,
            TerracePlanner.Lot square,MountainBuildingPlan candidate,Access access,int cx,int cz,int sea) {
        var start=access.approach();if(!passable(terrain,roads,buildings,square,candidate,access,start,cx,cz,sea))return false;
        var target=roads.keySet().stream().min(Comparator.comparingInt(p->distance(p,start))).orElseThrow();
        var open=new PriorityQueue<Node>(Comparator.comparingInt(Node::score).thenComparingInt(Node::cost)
                .thenComparingInt(n->n.at().x()).thenComparingInt(n->n.at().z()));
        var costs=new HashMap<Point,Integer>();var parents=new HashMap<Point,Point>();var seen=new HashSet<Point>();
        open.add(new Node(start,0,0));costs.put(start,0);
        while(!open.isEmpty() && seen.size()<NODE_BUDGET) {
            var current=open.remove();var p=current.at();if(!seen.add(p))continue;
            if(roads.containsKey(p)) {
                var additions=new LinkedHashMap<Point,Column>();
                for(var step=p;step!=null;step=parents.get(step))for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                    var q=new Point(step.x()+dx,step.z()+dz);
                    if(roads.containsKey(q) || square.contains(q.x(),q.z()) || (candidate!=null && candidate.occupies(q.x(),q.z(),0)))continue;
                    additions.put(q,new Column(q.x(),q.z(),grade(terrain,roads,access,q),terrain.height(q.x(),q.z())-1,ValleyTownPlanner.ROAD));
                }
                roads.putAll(additions);return true;
            }
            for(var d:STEPS) {
                var q=new Point(p.x()+d[0],p.z()+d[1]);
                int climb=Math.abs(grade(terrain,roads,access,p)-grade(terrain,roads,access,q));
                if(seen.contains(q) || climb>1 || !passable(terrain,roads,buildings,square,candidate,access,q,cx,cz,sea))continue;
                int cost=current.cost()+(d[0]!=0 && d[1]!=0?14:10)+climb*14+Math.abs(grade(terrain,roads,access,q)-terrain.height(q.x(),q.z())+1)*3;
                if(cost>=costs.getOrDefault(q,Integer.MAX_VALUE))continue;
                costs.put(q,cost);parents.put(q,p);
                int dx=Math.abs(q.x()-target.x()),dz=Math.abs(q.z()-target.z());
                open.add(new Node(q,cost,cost+10*Math.max(dx,dz)+4*Math.min(dx,dz)));
            }
        }
        return false;
    }
}
