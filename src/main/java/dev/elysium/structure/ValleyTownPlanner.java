package dev.elysium.structure;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import net.minecraft.core.Direction;

/** Crossing-led town planning. No chunk loads, block writes, or global seed caches. */
public final class ValleyTownPlanner {
    public static final int REACH = 112, MAX_GRADE = 6, NODE_BUDGET = 4500;
    public static final int COURT = 0, ROAD = 1, CROSSING = 2;
    private static final int[][] SITE_OFFSETS = {{0,0},{0,6},{0,-6},{6,0},{-6,0},{6,6},{-6,6},{6,-6},{-6,-6}};
    public record Column(int x, int z, int ground, int original, int kind) {
        public Column {
            if (Math.abs(ground-original)>(kind==CROSSING?20:MAX_GRADE) || kind<0 || kind>2) throw new IllegalArgumentException("Unbounded town earthwork");
        }
    }
    public record Plan(List<TerracePlanner.Lot> lots, List<Column> columns, BridgePlanner.Span crossing) {
        public Plan { lots=List.copyOf(lots); columns=List.copyOf(columns); }
    }
    private record Node(TerracePlanner.Point point,int cost,int score) {}
    private ValleyTownPlanner() {}

    public static Optional<Plan> plan(LandscapePlanner.Terrain terrain, BridgePlanner.Span bridge, int sea,
            TerracePlanner.Module hall, TerracePlanner.Module tower, List<TerracePlanner.Module> houses, int minimum, int maximum) {
        return plan(terrain,bridge,sea,hall,tower,houses,minimum,maximum,ignored->{});
    }
    public static Optional<Plan> plan(LandscapePlanner.Terrain terrain, BridgePlanner.Span bridge, int sea,
            TerracePlanner.Module hall, TerracePlanner.Module tower, List<TerracePlanner.Module> houses, int minimum, int maximum, Consumer<String> survey) {
        int cx=bridge.x(bridge.length()/2,0),cz=bridge.z(bridge.length()/2,0);
        boolean eastWest=bridge.eastWest();
        var roads=new LinkedHashMap<TerracePlanner.Point,Column>();
        for(int along=0;along<bridge.length();along++) for(int across=-1;across<=1;across++) {
            int x=bridge.x(along,across),z=bridge.z(along,across);
            roads.put(new TerracePlanner.Point(x,z),new Column(x,z,bridge.walkingHeight(along)-1,
                    bridge.floorHeight(along,across)-1,CROSSING));
        }
        int near=-bridge.length()/2,far=bridge.length()-1-bridge.length()/2;
        List<TerracePlanner.Lot> lots=new ArrayList<>();
        // A public square at one landing anchors the street graph, not a radial ring of houses.
        for(int side:new int[]{1,-1}) {
            var p=point(cx,cz,eastWest,(side>0?far:near)+side*9,0);
            var pad=pad(terrain,p.x(),p.z(),5,sea,5);
            if(pad.isEmpty())continue;
            var plaza=new TerracePlanner.Lot(p.x(),p.z(),pad.getAsInt(),5,face(p.x(),p.z(),cx,cz),-1);
            if(connect(terrain,lots,roads,plaza,cx,cz,sea))break;
        }
        if(lots.isEmpty()){survey.accept("no_square");return Optional.empty();}
        var square=lots.getFirst();
        // Higher halls receive preference, while every candidate must still have a walkable approach.
        List<TerracePlanner.Lot> halls=new ArrayList<>();
        for(int side:new int[]{1,-1})for(int u:new int[]{45,35,55})for(int v:new int[]{-24,24,-54,54}) {
            var p=point(cx,cz,eastWest,(side>0?far:near)+side*u,v);
            var pad=pad(terrain,p.x(),p.z(),hall.radius(),sea,hall.relief());
            if(pad.isPresent() && pad.getAsInt()-square.ground()<=26)
                halls.add(new TerracePlanner.Lot(p.x(),p.z(),pad.getAsInt(),hall.radius(),face(p.x(),p.z(),square.x(),square.z()),0));
        }
        halls.sort(Comparator.comparingInt(TerracePlanner.Lot::ground).reversed());
        for(var lot:halls)if(available(lot,lots,roads) && connect(terrain,lots,roads,lot,cx,cz,sea))break;
        if(lots.size()!=2){survey.accept("no_hall");return Optional.empty();}
        towerSites: for(int side:new int[]{1,-1})for(int v:new int[]{26,-26,54,-54}) {
            var p=point(cx,cz,eastWest,(side>0?far:near)+side*12,v);
            var pad=pad(terrain,p.x(),p.z(),tower.radius(),sea,tower.relief());
            if(pad.isEmpty())continue;
            var lot=new TerracePlanner.Lot(p.x(),p.z(),pad.getAsInt(),tower.radius(),face(p.x(),p.z(),cx,cz),1);
            if(available(lot,lots,roads) && connect(terrain,lots,roads,lot,cx,cz,sea))break towerSites;
        }
        if(lots.size()!=3){survey.accept("no_tower");return Optional.empty();}
        int count=0,index=0;
        sites: for(int distance:new int[]{30,60,90,0})for(int sign:new int[]{1,-1})for(int row:new int[]{0,1,2})for(int side:new int[]{-1,1}) {
            if(distance==0 && sign<0)continue;
            var module=houses.get(index++%houses.size());
            int u=(side>0?far:near)+side*(module.radius()+5+row*30),v=distance*sign;
            // Follow nearby usable terraces instead of forcing a rigid row onto a hillside.
            for(var offset:SITE_OFFSETS) {
                var p=point(cx,cz,eastWest,u+offset[0],v+offset[1]);
                if(Math.abs(p.x()-cx)+module.radius()>REACH || Math.abs(p.z()-cz)+module.radius()>REACH)continue;
                var lot=new TerracePlanner.Lot(p.x(),p.z(),0,module.radius(),face(p.x(),p.z(),cx,cz),2+(index-1)%houses.size());
                if(!available(lot,lots,roads))continue;
                var pad=pad(terrain,p.x(),p.z(),module.radius(),sea,module.relief());
                if(pad.isEmpty() || Math.abs(pad.getAsInt()-square.ground())>26)continue;
                lot=new TerracePlanner.Lot(p.x(),p.z(),pad.getAsInt(),module.radius(),lot.face(),lot.module());
                if(connect(terrain,lots,roads,lot,cx,cz,sea)) {
                    if(++count==maximum)break sites;
                    break;
                }
            }
        }
        if(count<minimum){survey.accept("houses_"+count);return Optional.empty();}
        long nearHouses=lots.stream().filter(l->l.module()>=2 && (eastWest?l.x()<cx:l.z()<cz)).count();
        if(nearHouses<3 || count-nearHouses<3){survey.accept("one_bank");return Optional.empty();}
        // Save every prepared column. Generation never resamples this terrain after another piece has changed it.
        var columns=new LinkedHashMap<TerracePlanner.Point,Column>();
        for(var lot:lots)for(int x=lot.x()-lot.radius();x<=lot.x()+lot.radius();x++)for(int z=lot.z()-lot.radius();z<=lot.z()+lot.radius();z++)
            columns.put(new TerracePlanner.Point(x,z),new Column(x,z,lot.ground(),terrain.height(x,z)-1,COURT));
        roads.forEach((p,c)->{if(c.kind()!=CROSSING)columns.put(p,c);});
        return Optional.of(new Plan(lots,new ArrayList<>(columns.values()),bridge));
    }

    private static TerracePlanner.Point point(int x,int z,boolean eastWest,int u,int v) {
        return new TerracePlanner.Point(x+(eastWest?u:v),z+(eastWest?v:u));
    }
    private static Direction face(int x,int z,int tx,int tz) {
        return Math.abs(x-tx)>Math.abs(z-tz) ? (x>tx?Direction.WEST:Direction.EAST) : (z>tz?Direction.NORTH:Direction.SOUTH);
    }
    public static OptionalInt pad(LandscapePlanner.Terrain terrain,int x,int z,int radius,int sea,int relief) {
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(int pass=0;pass<2;pass++)for(int dx=-radius;dx<=radius;dx+=pass==0?radius:1)for(int dz=-radius;dz<=radius;dz+=pass==0?radius:1) {
            int h=terrain.height(x+dx,z+dz)-1;
            if(h<sea-1)return OptionalInt.empty();
            low=Math.min(low,h);high=Math.max(high,h);
            if(high-low>relief)return OptionalInt.empty();
        }
        // A little cut and fill makes a terrace; it does not flatten an entire valley.
        int target=Math.max(sea,high-Math.min(3,(high-low)/2));
        return target-low<=MAX_GRADE ? OptionalInt.of(target) : OptionalInt.empty();
    }
    private static boolean available(TerracePlanner.Lot lot,List<TerracePlanner.Lot> lots,Map<TerracePlanner.Point,Column> roads) {
        for(var other:lots)if(Math.abs(lot.x()-other.x())<=lot.radius()+other.radius()+3 &&
                Math.abs(lot.z()-other.z())<=lot.radius()+other.radius()+3)return false;
        return roads.keySet().stream().noneMatch(p->lot.distance(p.x(),p.z())<=MAX_GRADE);
    }
    private static int grade(LandscapePlanner.Terrain terrain,List<TerracePlanner.Lot> lots,Map<TerracePlanner.Point,Column> roads,TerracePlanner.Point p) {
        if(roads.containsKey(p))return roads.get(p).ground();
        int h=terrain.height(p.x(),p.z())-1;
        for(var lot:lots) {
            int d=lot.distance(p.x(),p.z());
            if(d<=MAX_GRADE)h=Math.clamp(h,lot.ground()-d,lot.ground()+d);
        }
        return h;
    }
    private static boolean passable(LandscapePlanner.Terrain terrain,List<TerracePlanner.Lot> lots,Map<TerracePlanner.Point,Column> roads,
            TerracePlanner.Point p,int cx,int cz,int sea) {
        if(Math.abs(p.x()-cx)>=REACH || Math.abs(p.z()-cz)>=REACH)return false;
        int center=grade(terrain,lots,roads,p);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var q=new TerracePlanner.Point(p.x()+dx,p.z()+dz);
            for(var lot:lots)if(lot.contains(q.x(),q.z()) && !lot.entrance(q.x(),q.z()))return false;
            var saved=roads.get(q);
            if(saved!=null && saved.kind()==CROSSING)continue;
            int original=terrain.height(q.x(),q.z())-1,top=grade(terrain,lots,roads,q);
            if(original<sea-1 || Math.abs(top-original)>MAX_GRADE || Math.abs(top-center)>1)return false;
        }
        return true;
    }
    private static boolean connect(LandscapePlanner.Terrain terrain,List<TerracePlanner.Lot> lots,Map<TerracePlanner.Point,Column> roads,
            TerracePlanner.Lot candidate,int cx,int cz,int sea) {
        var proposed=new ArrayList<>(lots);proposed.add(candidate);
        var start=candidate.approach();
        if(!passable(terrain,proposed,roads,start,cx,cz,sea))return false;
        var target=roads.keySet().stream().min(Comparator.comparingInt(p->Math.abs(p.x()-start.x())+Math.abs(p.z()-start.z()))).orElseThrow();
        var open=new PriorityQueue<Node>(Comparator.comparingInt(Node::score).thenComparingInt(Node::cost)
                .thenComparingInt(n->n.point.x()).thenComparingInt(n->n.point.z()));
        var costs=new HashMap<TerracePlanner.Point,Integer>();var parents=new HashMap<TerracePlanner.Point,TerracePlanner.Point>();
        var seen=new HashSet<TerracePlanner.Point>();costs.put(start,0);open.add(new Node(start,0,0));
        while(!open.isEmpty() && seen.size()<NODE_BUDGET) {
            var current=open.remove();var at=current.point;
            if(!seen.add(at))continue;
            if(roads.containsKey(at)) {
                var additions=new LinkedHashMap<TerracePlanner.Point,Column>();
                for(var p=at;p!=null;p=parents.get(p))for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                    var q=new TerracePlanner.Point(p.x()+dx,p.z()+dz);
                    if(roads.containsKey(q) || proposed.stream().anyMatch(l->l.contains(q.x(),q.z())))continue;
                    additions.put(q,new Column(q.x(),q.z(),grade(terrain,proposed,roads,q),terrain.height(q.x(),q.z())-1,ROAD));
                }
                lots.add(candidate);roads.putAll(additions);return true;
            }
            for(Direction d:Direction.Plane.HORIZONTAL) {
                var next=at.move(d);
                int climb=Math.abs(grade(terrain,proposed,roads,next)-grade(terrain,proposed,roads,at));
                if(seen.contains(next) || climb>1 || !passable(terrain,proposed,roads,next,cx,cz,sea))continue;
                int cost=current.cost+10+climb*5;
                if(cost>=costs.getOrDefault(next,Integer.MAX_VALUE))continue;
                costs.put(next,cost);parents.put(next,at);
                open.add(new Node(next,cost,cost+10*(Math.abs(next.x()-target.x())+Math.abs(next.z()-target.z()))));
            }
        }
        return false;
    }
}
