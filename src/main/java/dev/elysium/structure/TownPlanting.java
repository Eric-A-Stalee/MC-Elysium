package dev.elysium.structure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import dev.elysium.structure.MountainBuildingPlan.Point;
import dev.elysium.structure.TownLandscapePlan.Cell;
import dev.elysium.structure.TownLandscapePlan.Detail;
import dev.elysium.structure.TownLandscapePlan.Kind;
import dev.elysium.structure.TownLandscapePlan.Tree;

/** Small habitat-specific patches; every chosen height is saved with the town. */
public final class TownPlanting {
    private static final int DETAIL_BUDGET=18000;
    private static final int[][] CARDINALS={{1,0},{-1,0},{0,1},{0,-1}};
    private record Mix(int flowers,int whiteFlowers,int litter,int grain,double cover) {
        Kind choose(Random random) {
            int n=random.nextInt(100);
            if(n<flowers)return Kind.FLOWER;
            if(n<flowers+whiteFlowers)return Kind.WHITE_FLOWER;
            if(n<flowers+whiteFlowers+litter)return Kind.LITTER;
            if(n<flowers+whiteFlowers+litter+grain)return Kind.GRAIN;
            return Kind.GRASS;
        }
    }
    private static final Mix BANK=new Mix(35,18,5,10,.84);
    private static final Mix GROVE=new Mix(17,9,42,0,.74);
    private static final Mix VERGE=new Mix(27,13,10,8,.70);
    private final LandscapePlanner.Terrain terrain;
    private final List<MountainBuildingPlan> buildings;
    private final List<Tree> trees;
    private final TerracePlanner.Lot square;
    private final Set<Point> roads;
    private final Map<Cell,Detail> details;
    private final Map<Point,Integer> occupied=new HashMap<>();
    private final int sea;

    private TownPlanting(LandscapePlanner.Terrain terrain,List<MountainBuildingPlan> buildings,List<Tree> trees,
            TerracePlanner.Lot square,Set<Point> roads,Map<Cell,Detail> details,int sea) {
        this.terrain=terrain;this.buildings=buildings;this.trees=trees;this.square=square;
        this.roads=roads;this.details=details;this.sea=sea;
        for(var d:details.values())occupied.merge(new Point(d.x(),d.z()),d.y(),Math::max);
    }
    static void add(LandscapePlanner.Terrain terrain,List<MountainBuildingPlan> buildings,List<Tree> trees,
            List<ValleyTownPlanner.Column> streets,TerracePlanner.Lot square,Set<Point> roads,Map<Cell,Detail> details,int sea) {
        var planting=new TownPlanting(terrain,buildings,trees,square,roads,details,sea);
        for(var tree:trees) {
            var random=new Random(tree.seed()^0xB17C4L);
            for(int n=0;n<4;n++) {
                double a=random.nextDouble()*Math.PI*2,r=2.5+random.nextDouble()*3;
                planting.patch(tree.x()+(int)Math.round(Math.cos(a)*r),tree.z()+(int)Math.round(Math.sin(a)*r),
                        3+random.nextInt(2),2+random.nextInt(2),GROVE,random.nextLong());
            }
        }
        // One verge pocket per eight-block district, rather than a regular line
        // of isolated flowers. Gaps keep sightlines and walking space open.
        var districts=new HashSet<Point>();
        for(var c:streets)if(c.kind()==ValleyTownPlanner.ROAD && Math.floorMod(c.x()*7+c.z()*11,19)==0) {
            if(!districts.add(new Point(Math.floorDiv(c.x(),8),Math.floorDiv(c.z(),8))))continue;
            long seed=seed(c.x(),c.z());var random=new Random(seed);var d=CARDINALS[random.nextInt(4)];
            planting.patch(c.x()+d[0]*4,c.z()+d[1]*4,3+random.nextInt(3),2+random.nextInt(3),VERGE,seed);
        }
        if(streets.isEmpty())return;
        // A bounded, coarse bank survey follows the village's actual waterline.
        // No chunk reads or changes to river depth are involved.
        int minX=streets.stream().mapToInt(ValleyTownPlanner.Column::x).min().orElseThrow()-10;
        int maxX=streets.stream().mapToInt(ValleyTownPlanner.Column::x).max().orElseThrow()+10;
        int minZ=streets.stream().mapToInt(ValleyTownPlanner.Column::z).min().orElseThrow()-10;
        int maxZ=streets.stream().mapToInt(ValleyTownPlanner.Column::z).max().orElseThrow()+10;
        for(int x=minX;x<=maxX;x+=8)for(int z=minZ;z<=maxZ;z+=8) {
            long seed=seed(x,z);var random=new Random(seed);
            int px=x+random.nextInt(5)-2,pz=z+random.nextInt(5)-2,y=terrain.height(px,pz);
            if(y<sea || y>sea+7 || terrain.water(px,pz))continue;
            for(var d:CARDINALS)if(terrain.water(px+d[0]*6,pz+d[1]*6)) {
                planting.patch(px,pz,d[0]==0?6:3,d[0]==0?3:6,BANK,seed);break;
            }
        }
    }
    private void patch(int cx,int cz,int rx,int rz,Mix mix,long seed) {
        int centre=terrain.height(cx,cz);
        if(centre<sea || terrain.water(cx,cz))return;
        var random=new Random(seed);
        for(int dx=-rx;dx<=rx;dx++)for(int dz=-rz;dz<=rz;dz++) {
            double edge=1+.16*Math.sin(dx*1.2+dz*.7+(seed&15));
            if(dx*dx/(double)(rx*rx)+dz*dz/(double)(rz*rz)>edge || random.nextDouble()>mix.cover())continue;
            if(details.size()>=DETAIL_BUDGET)return;
            int x=cx+dx,z=cz+dz,y=terrain.height(x,z);var p=new Point(x,z);
            if(y<sea || Math.abs(y-centre)>3 || terrain.water(x,z) || roads.contains(p) || square.distance(x,z)<=2
                    || occupied.getOrDefault(p,Integer.MIN_VALUE)>=y-1
                    || trees.stream().anyMatch(t->t.root(x,z,1))
                    || buildings.stream().anyMatch(b->b.occupies(x,z,1)))continue;
            // Reject sharp ledges: patches follow gentle soil, not vertical rock.
            if(Math.abs(terrain.height(x+1,z)-y)>2 || Math.abs(terrain.height(x,z+1)-y)>2)continue;
            var kind=mix.choose(random);details.put(new Cell(x,y,z),new Detail(x,y,z,kind));occupied.put(p,y);
        }
    }
    private static long seed(int x,int z) {return ((long)x*341873128712L)^((long)z*132897987541L)^0xE1751A1L;}
}
