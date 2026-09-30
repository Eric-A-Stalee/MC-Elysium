package dev.elysium.gametest;

import dev.elysium.structure.*;
import dev.elysium.structure.MountainArchitecture.Cell;
import dev.elysium.structure.MountainArchitecture.Material;
import dev.elysium.structure.MountainArchitecture.Voxel;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure functional checks shared by the headless server and the fast geometry inspection tool. */
public final class MountainPlanChecks {
    private static final int[][] DIRS={{1,0},{-1,0},{0,1},{0,-1}};
    private MountainPlanChecks() {}
    public static LandscapePlanner.Terrain slope(int gradient) {
        return new LandscapePlanner.Terrain() {
            public int height(int x,int z){return 100+Math.floorDiv(x*gradient,10);}
            public boolean water(int x,int z){return false;}
        };
    }
    public static Set<Cell> verifyGeometry(MountainBuildingPlan plan) {
        var blocks=MountainArchitecture.build(plan);var e=plan.entry();
        Cell start=new Cell(e.u()+e.du()*e.length(),e.floor()+1,e.v()+e.dv()*e.length());
        require(material(blocks,new Cell(e.u(),e.floor()+1,e.v()))==Material.DOOR_LOW
                && material(blocks,new Cell(e.u(),e.floor()+2,e.v()))==Material.DOOR_HIGH,"Missing entry door",plan);
        require(walkable(blocks,start),"Blocked porch",plan);
        var seen=new HashSet<Cell>();var queue=new ArrayDeque<Cell>();queue.add(start);
        int low=plan.minFloor(),high=plan.maxY();
        while(!queue.isEmpty()) {
            var p=queue.remove();if(!seen.add(p))continue;
            for(var dir:DIRS)for(int dy=-1;dy<=1;dy++) {
                var q=new Cell(p.u()+dir[0],p.y()+dy,p.v()+dir[1]);
                if(q.y()<low || q.y()>high || seen.contains(q) || !walkable(blocks,q))continue;
                if(dy>0 && !passable(material(blocks,new Cell(p.u(),p.y()+2,p.v()))))continue;
                if(dy<0 && !passable(material(blocks,new Cell(q.u(),q.y()+2,q.v()))))continue;
                queue.add(q);
            }
            if(material(blocks,p)==Material.LADDER)for(int dy:new int[]{-1,1}) {
                var q=new Cell(p.u(),p.y()+dy,p.v());if(walkable(blocks,q) && !seen.contains(q))queue.add(q);
            }
        }
        for(var room:plan.rooms())for(int s=0;s<room.storeys();s++) {
            int feet=room.floor()+s*5+1;
            long reached=seen.stream().filter(c->c.y()==feet && c.u()>room.u() && c.u()<room.maxU() && c.v()>room.v() && c.v()<room.maxV()).count();
            require(reached>=18,"Unreachable room/floor ("+reached+" cells): "+room+" storey "+s,plan);
        }
        if(plan.cellar())require(seen.stream().anyMatch(c->c.y()==plan.main().floor()-4),"Inaccessible cellar",plan);
        plan.gallery().ifPresent(g->require(seen.contains(new Cell(g.wall()+g.side(),g.floor()+1,plan.main().maxV()-4)),"Inaccessible timber gallery",plan));
        int beds=0,windowLights=0;
        for(var b:blocks.entrySet()) {
            var p=b.getKey();var m=b.getValue().material();
            if(m==Material.BED_FOOT) {
                beds++;var head=new Cell(p.u(),p.y(),p.v()+1);
                require(material(blocks,head)==Material.BED_HEAD,"Orphan bed foot",plan);
                require(List.of(new Cell(p.u()+1,p.y(),p.v()),new Cell(p.u()-1,p.y(),p.v()),
                        new Cell(p.u(),p.y(),p.v()-1)).stream().anyMatch(seen::contains),"Unreachable bed at "+p,plan);
            }
            if(m==Material.BED_HEAD)require(material(blocks,new Cell(p.u(),p.y(),p.v()-1))==Material.BED_FOOT,"Orphan bed head",plan);
            if(m==Material.HANGING_LANTERN)require(solid(material(blocks,new Cell(p.u(),p.y()+1,p.v()))),"Unsupported lantern "+p,plan);
            if(m==Material.LANTERN) {
                windowLights++;
                require(material(blocks,new Cell(p.u(),p.y()-1,p.v()))==Material.SIDE_TABLE,"Window lantern needs a supporting table",plan);
                require(java.util.Arrays.stream(DIRS).anyMatch(d->material(blocks,new Cell(p.u()+d[0],p.y(),p.v()+d[1]))==Material.GLASS),
                        "Table lantern must be visible directly through a window",plan);
            }
        }
        require(beds>=1,"House without beds",plan);
        require(windowLights>=1,"House without a lit window",plan);
        return seen;
    }
    public static void grammarMatrix() {
        int count=0,cellars=0,split=0;var shapes=new HashSet<String>();
        for(var style:MountainBuildingPlan.Style.values())for(int gradient:new int[]{0,4,6})for(long seed=0;seed<12;seed++)for(int turn=0;turn<4;turn++) {
            var result=MountainBuildingPlan.fit(slope(gradient),0,0,turn,seed,style,63,new MountainBuildingPlan.Point(0,30));
            if(result.isEmpty())continue;var b=result.get();verifyGeometry(b);count++;
            if(b.cellar())cellars++;
            if(b.rooms().stream().anyMatch(r->r.floor()!=b.main().floor()))split++;
            shapes.add(style+":"+b.rooms());
        }
        if(count<200 || shapes.size()<45 || cellars<20 || split<20)throw new IllegalStateException("Insufficient grammar coverage: "+count+" plans, "+shapes.size()+" shapes, "+cellars+" cellars, "+split+" split levels");
        System.out.println("Mountain grammar: "+count+" plans, "+shapes.size()+" geometries, "+cellars+" cellars, "+split+" split levels passed");
    }
    public static Material material(Map<Cell,Voxel> blocks,Cell p) { return blocks.getOrDefault(p,new Voxel(Material.AIR)).material(); }
    private static boolean passable(Material m) { return m==Material.AIR || m==Material.DOOR_LOW || m==Material.DOOR_HIGH || m==Material.LADDER; }
    private static boolean solid(Material m) {return !passable(m) && m!=Material.HANGING_LANTERN && m!=Material.LANTERN && m!=Material.TABLE_TOP;}
    private static boolean walkable(Map<Cell,Voxel> blocks,Cell p) {
        return passable(material(blocks,p)) && passable(material(blocks,new Cell(p.u(),p.y()+1,p.v())))
                && (solid(material(blocks,new Cell(p.u(),p.y()-1,p.v()))) || material(blocks,p)==Material.LADDER);
    }
    private static void require(boolean condition,String reason,MountainBuildingPlan plan) {
        if(!condition)throw new IllegalStateException(reason+"; style="+plan.style()+", seed="+plan.seed()+", turn="+plan.rotation()+", rooms="+plan.rooms()+", entry="+plan.entry());
    }
    public static void main(String[] args) { grammarMatrix(); }
}
