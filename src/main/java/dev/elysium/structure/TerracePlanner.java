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
import java.util.Set;
import net.minecraft.core.Direction;

/** Small rigid pads with bounded path finding. A complete immutable plan precedes block placement. */
public final class TerracePlanner {
    public static final int REACH = 48, NODE_BUDGET = 1200, MAX_FILL = 4;
    private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final int[][] LOTS = {{-22,-12},{22,-12},{-22,12},{22,12},{-11,25},{11,25},
            {-11,-25},{11,-25},{-30,0},{30,0},{0,31},{0,-31},{-24,26},{24,26},{-24,-26},{24,-26}};
    public record Point(int x, int z) {
        Point move(Direction d) { return new Point(x + d.getStepX(), z + d.getStepZ()); }
    }
    public record Lot(int x, int z, int ground, int radius, Direction face, int module) {
        int distance(int px, int pz) { return Math.max(0, Math.max(Math.abs(px-x), Math.abs(pz-z)) - radius); }
        public Point door() { return new Point(x + face.getStepX()*radius, z + face.getStepZ()*radius); }
        Point approach() { return door().move(face); }
        boolean contains(int px, int pz) { return distance(px, pz) == 0; }
        boolean entrance(int px, int pz) {
            var d = door();
            return face.getAxis() == Direction.Axis.X ? px == d.x && Math.abs(pz-d.z) <= 1
                    : pz == d.z && Math.abs(px-d.x) <= 1;
        }
    }
    public record Tile(int x, int z, int ground, int floor) {
        public Tile {
            if (ground < floor || ground-floor > MAX_FILL) throw new IllegalArgumentException("Unbounded retaining wall");
        }
    }
    public record Plan(List<Lot> lots, List<Tile> paths) {
        public Plan { lots = List.copyOf(lots); paths = List.copyOf(paths); }
    }
    public record Module(int radius, int relief) {}
    private record Node(Point at, int cost, int score) {}
    private TerracePlanner() {}

    public static Optional<Plan> plan(LandscapePlanner.Terrain terrain, int x, int z, int sea,
                                     Module hall, List<Module> houses, int minimum, int maximum) {
        var center = LandscapePlanner.pad(terrain, x, z, 4, sea, 3);
        if (center.isEmpty()) return Optional.empty();
        List<Lot> lots = new ArrayList<>();
        lots.add(new Lot(x,z,center.getAsInt(),4,Direction.SOUTH,-1));
        Map<Point, Tile> roads = new LinkedHashMap<>();
        List<Lot> halls = new ArrayList<>();
        for (Direction d : DIRS) {
            int hx=x+d.getStepX()*23, hz=z+d.getStepZ()*23;
            var pad = LandscapePlanner.pad(terrain,hx,hz,hall.radius,sea,hall.relief);
            if (pad.isPresent() && Math.abs(pad.getAsInt()-center.getAsInt()) <= 12)
                halls.add(new Lot(hx,hz,pad.getAsInt(),hall.radius,d.getOpposite(),0));
        }
        halls.sort(Comparator.comparingInt(Lot::ground).reversed());
        for (Lot candidate : halls) {
            if (connect(terrain,lots,roads,candidate,x,z,sea)) break;
        }
        if (lots.size() != 2) return Optional.empty();
        for (int i=0; i<LOTS.length && lots.size()<maximum+2; i++) {
            var shape=houses.get(i%houses.size());
            int hx=x+LOTS[i][0], hz=z+LOTS[i][1];
            if (lots.stream().anyMatch(l -> Math.abs(l.x-hx) <= l.radius+shape.radius+2
                    && Math.abs(l.z-hz) <= l.radius+shape.radius+2)) continue;
            // Keep accepted roads beyond the new pad's four-block grade apron.
            if (roads.keySet().stream().anyMatch(p -> Math.abs(p.x-hx) <= shape.radius+MAX_FILL
                    && Math.abs(p.z-hz) <= shape.radius+MAX_FILL)) continue;
            var pad=LandscapePlanner.pad(terrain,hx,hz,shape.radius,sea,shape.relief);
            if (pad.isEmpty() || Math.abs(pad.getAsInt()-center.getAsInt())>14) continue;
            Direction face = Math.abs(hx-x)>Math.abs(hz-z) ? (hx>x ? Direction.WEST : Direction.EAST)
                    : (hz>z ? Direction.NORTH : Direction.SOUTH);
            connect(terrain,lots,roads,new Lot(hx,hz,pad.getAsInt(),shape.radius,face,1+i%houses.size()),x,z,sea);
        }
        if (lots.size()<minimum+2) return Optional.empty();
        return Optional.of(new Plan(lots,new ArrayList<>(roads.values())));
    }

    private static boolean connect(LandscapePlanner.Terrain terrain, List<Lot> lots, Map<Point,Tile> roads,
                                   Lot candidate, int cx, int cz, int sea) {
        List<Lot> proposed=new ArrayList<>(lots); proposed.add(candidate);
        var route=route(terrain,proposed,candidate.approach(),cx,cz,sea);
        if (route.isEmpty()) return false;
        Map<Point,Tile> additions=new LinkedHashMap<>();
        for (Point step : route.get()) for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            var point=new Point(step.x+dx,step.z+dz);
            if (proposed.stream().anyMatch(l->l.contains(point.x,point.z))) continue;
            int floor=terrain.height(point.x,point.z)-1;
            additions.put(point,new Tile(point.x,point.z,grade(terrain,proposed,point),floor));
        }
        lots.add(candidate); roads.putAll(additions);
        return true;
    }

    private static int grade(LandscapePlanner.Terrain terrain,List<Lot> lots,Point p) {
        int surface=terrain.height(p.x,p.z)-1;
        for(Lot lot:lots) {
            int distance=lot.distance(p.x,p.z);
            if(distance<=MAX_FILL) surface=Math.max(surface,lot.ground-distance);
        }
        return surface;
    }

    private static boolean passable(LandscapePlanner.Terrain terrain,List<Lot> lots,Point p,int cx,int cz,int sea) {
        if(Math.abs(p.x-cx)>REACH-1 || Math.abs(p.z-cz)>REACH-1) return false;
        int center=grade(terrain,lots,p);
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) {
            var cell=new Point(p.x+dx,p.z+dz);
            for(int i=0;i<lots.size();i++) {
                Lot lot=lots.get(i);
                // The square plaza is a destination. Other templates expose only their three-block entrance rim.
                if(lot.contains(cell.x,cell.z) && (i!=0 && !lot.entrance(cell.x,cell.z))) return false;
            }
            int floor=terrain.height(cell.x,cell.z)-1, top=grade(terrain,lots,cell);
            if(floor<sea-1 || top-floor>MAX_FILL || Math.abs(top-center)>1) return false;
        }
        return true;
    }

    private static Optional<List<Point>> route(LandscapePlanner.Terrain terrain,List<Lot> lots,Point start,int cx,int cz,int sea) {
        if(!passable(terrain,lots,start,cx,cz,sea)) return Optional.empty();
        var open=new PriorityQueue<Node>(Comparator.comparingInt(Node::score).thenComparingInt(Node::cost)
                .thenComparingInt(n->n.at.x).thenComparingInt(n->n.at.z));
        Map<Point,Integer> scores=new HashMap<>(); Map<Point,Point> parents=new HashMap<>();
        Set<Point> closed=new HashSet<>();
        scores.put(start,0);open.add(new Node(start,0,0));
        int visited=0;
        while(!open.isEmpty() && visited<NODE_BUDGET) {
            Node node=open.remove();Point at=node.at;
            if(!closed.add(at)) continue;visited++;
            if(Math.max(Math.abs(at.x-cx),Math.abs(at.z-cz))<=5) {
                List<Point> result=new ArrayList<>();
                for(Point p=at;p!=null;p=parents.get(p)) result.add(p);
                return Optional.of(result);
            }
            for(Direction direction:DIRS) {
                Point next=at.move(direction);
                int climb=Math.abs(grade(terrain,lots,next)-grade(terrain,lots,at));
                if(closed.contains(next) || climb>1 || !passable(terrain,lots,next,cx,cz,sea)) continue;
                int cost=node.cost+10+climb*4;
                if(cost>=scores.getOrDefault(next,Integer.MAX_VALUE)) continue;
                scores.put(next,cost);parents.put(next,at);
                int heuristic=Math.max(0,Math.abs(next.x-cx)+Math.abs(next.z-cz)-10)*10;
                open.add(new Node(next,cost,cost+heuristic));
            }
        }
        return Optional.empty();
    }
}
