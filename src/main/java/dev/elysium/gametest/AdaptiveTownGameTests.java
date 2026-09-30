package dev.elysium.gametest;

import dev.elysium.registry.ModStructures;
import dev.elysium.structure.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class AdaptiveTownGameTests {
    private static final List<MountainBuildingPlan.Style> STYLES=List.of(MountainBuildingPlan.Style.LONGHOUSE,
            MountainBuildingPlan.Style.CROSS_GABLE,MountainBuildingPlan.Style.HILLSIDE_LODGE,MountainBuildingPlan.Style.COURTYARD);
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void dynamicRoomsBedsAndCellarsHaveUsableCirculation(GameTestHelper helper) {
        MountainPlanChecks.grammarMatrix();helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void contourStreetsConnectBothBanksWithoutLevelingHousePlots(GameTestHelper helper) {
        var terrain=new LandscapePlanner.Terrain() {
            public int height(int x,int z){return Math.abs(x)<6?59:64+Math.max(0,Math.abs(x)-17)*3/5+(int)(Math.sin(z/16.)*2);}
            public boolean water(int x,int z){return Math.abs(x)<6;}
        };
        var bridge=BridgePlanner.find(terrain::height,0,0,true,63).orElseThrow();
        for(long seed=0;seed<3;seed++) {
            var plan=ContourTownPlanner.plan(terrain,bridge,63,seed,STYLES,14,24,ignored->{}).orElseThrow();
            helper.assertTrue(plan.buildings().size()>=16,"The adaptive layout must remain a substantial settlement");
            helper.assertTrue(plan.landscape().trees().size()>=4,"Town needs substantial reserved trees between its districts");
            for(var tree:plan.landscape().trees()) {
                helper.assertTrue(plan.buildings().stream().noneMatch(b->b.occupies(tree.x(),tree.z(),3)),"House footprints must respect reserved tree roots");
                helper.assertTrue(plan.streets().stream().noneMatch(c->tree.root(c.x(),c.z(),1)),"Streets must curve around reserved trunks");
            }
            helper.assertTrue(plan.streets().stream().filter(c->c.kind()==ValleyTownPlanner.COURT).count()==121,
                    "Only the public square receives a level pad; no house gets a square yard");
            var walk=new HashMap<MountainBuildingPlan.Point,Integer>();
            for(var c:plan.streets()) {
                helper.assertTrue(Math.abs(c.ground()-c.original())<=3,"Street cutting and filling may not exceed three blocks");
                walk.put(new MountainBuildingPlan.Point(c.x(),c.z()),c.ground());
            }
            for(int a=0;a<bridge.length();a++)for(int b=-1;b<=1;b++)
                walk.put(new MountainBuildingPlan.Point(bridge.x(a,b),bridge.z(a,b)),bridge.walkingHeight(a)-1);
            for(var b:plan.buildings()) {
                MountainPlanChecks.verifyGeometry(b);
                var e=b.entry();for(var g:b.ground())if(e.porch(g.u(),g.v(),0))walk.put(b.point(g.u(),g.v()),e.floor());
            }
            var seen=new HashSet<MountainBuildingPlan.Point>();var queue=new ArrayDeque<MountainBuildingPlan.Point>();
            queue.add(new MountainBuildingPlan.Point(0,0));
            while(!queue.isEmpty()) {
                var at=queue.remove();if(!seen.add(at))continue;
                for(Direction d:Direction.Plane.HORIZONTAL) {
                    var next=new MountainBuildingPlan.Point(at.x()+d.getStepX(),at.z()+d.getStepZ());
                    if(walk.containsKey(next) && !seen.contains(next) && Math.abs(walk.get(next)-walk.get(at))<=1)queue.add(next);
                }
            }
            for(var b:plan.buildings())helper.assertTrue(seen.contains(b.street()),"Disconnected contour street/porch at "+b.street());
            helper.assertTrue(plan.buildings().stream().filter(b->ContourTownPlanner.besideRock(terrain,b)).count()>=2,
                    "The settlement must have outcrops alongside its buildings");
            helper.assertTrue(plan.buildings().stream().mapToInt(b->b.main().floor()).max().orElseThrow()-63>=20,
                    "The town must climb beyond the flat riverbank");
            var repeat=ContourTownPlanner.plan(terrain,bridge,63,seed,STYLES,14,24,ignored->{}).orElseThrow();
            helper.assertTrue(plan.buildings().equals(repeat.buildings()) && plan.streets().equals(repeat.streets()) && plan.landscape().equals(repeat.landscape()),
                    "Site selection and street routing must be deterministic");
        }
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void adaptiveBuildingsRejectCliffsAndRemoteMountainBackdrops(GameTestHelper helper) {
        for(var style:STYLES)for(int turn=0;turn<4;turn++)
            helper.assertTrue(MountainBuildingPlan.fit(MountainPlanChecks.slope(30),0,0,turn,0,style,63,
                    new MountainBuildingPlan.Point(0,30)).isEmpty(),"An unbuildable cliff must be rejected, not graded away");
        var flat=new LandscapePlanner.Terrain(){public int height(int x,int z){return Math.abs(x)<6?59:64;}
            public boolean water(int x,int z){return Math.abs(x)<6;}};
        helper.assertTrue(ContourTownPlanner.plan(flat,BridgePlanner.find(flat::height,0,0,true,63).orElseThrow(),63,0,
                STYLES,14,24,ignored->{}).isEmpty(),"A flat neighborhood is not a mountain town even if peaks exist beyond the survey");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void savedDynamicBuildingsPlaceAcrossClipsAndKeepRotatedDoorsAndBeds(GameTestHelper helper) {
        var level=helper.getLevel();var context=StructurePieceSerializationContext.fromLevel(level);
        for(int turn=0;turn<4;turn++) {
            var at=helper.absolutePos(new BlockPos(1600+turn*64,140,1600));
            var terrain=new LandscapePlanner.Terrain(){public int height(int x,int z){return at.getY()+Math.floorDiv((x-at.getX())*4,10);}
                public boolean water(int x,int z){return false;}};
            java.util.Optional<MountainBuildingPlan> fitted=java.util.Optional.empty();
            for(long variant=0;variant<32 && fitted.isEmpty();variant++)fitted=MountainBuildingPlan.fit(terrain,at.getX(),at.getZ(),turn,variant,
                    MountainBuildingPlan.Style.CROSS_GABLE,0,new MountainBuildingPlan.Point(at.getX(),at.getZ()+30));
            helper.assertTrue(fitted.isPresent(),"Fixture needs a valid surveyed plan for rotation "+turn);
            var plan=fitted.orElseThrow();
            var piece=new MountainBuildingPiece(plan);var tag=piece.createTag(context);
            var saved=(MountainBuildingPiece)ModStructures.MOUNTAIN_BUILDING_PIECE.get().load(context,tag);
            helper.assertTrue(saved.plan().equals(plan) && saved.createTag(context).equals(tag),"Room levels, door, seed and ground profile must round-trip exactly");
            var blueprint=MountainArchitecture.build(plan);var box=saved.getBoundingBox();
            for(var c:blueprint.keySet())helper.assertTrue(box.isInside(new BlockPos(plan.worldX(c.u(),c.v()),c.y(),plan.worldZ(c.u(),c.v()))),
                    "Every emitted block must fit the saved piece bounds");
            // A column inside the overall box but outside the irregular footprint must remain untouched.
            BlockPos sentinel=null;
            for(int x=box.minX()+1;x<box.maxX() && sentinel==null;x++)for(int z=box.minZ()+1;z<box.maxZ();z++) {
                int u=plan.localU(x,z),v=plan.localV(x,z);
                if(!plan.occupies(x,z,1) && !blueprint.containsKey(new MountainArchitecture.Cell(u,plan.main().floor(),v))) {
                    sentinel=new BlockPos(x,plan.main().floor(),z);break;
                }
            }
            helper.assertTrue(sentinel!=null,"Fixture must include untouched ground between wings");
            level.setBlock(sentinel,Blocks.RED_CONCRETE.defaultBlockState(),2);
            for(int x=box.maxX()>>4;x>=box.minX()>>4;x--)for(int z=box.maxZ()>>4;z>=box.minZ()>>4;z--) {
                var chunk=new ChunkPos(x,z);var clip=new BoundingBox(chunk.getMinBlockX(),level.getMinBuildHeight(),chunk.getMinBlockZ(),
                        chunk.getMaxBlockX(),level.getMaxBuildHeight()-1,chunk.getMaxBlockZ());
                saved.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,chunk,at);
            }
            helper.assertTrue(level.getBlockState(sentinel).is(Blocks.RED_CONCRETE),"A room piece may not level the empty part of its bounding rectangle");
            var e=plan.entry();var door=new BlockPos(plan.worldX(e.u(),e.v()),e.floor()+1,plan.worldZ(e.u(),e.v()));
            var expected=saved.state(new MountainArchitecture.Voxel(MountainArchitecture.Material.DOOR_LOW,e.du()>0?3:e.du()<0?1:e.dv()>0?0:2));
            helper.assertTrue(level.getBlockState(door).is(Blocks.SPRUCE_DOOR)
                    && level.getBlockState(door).getValue(DoorBlock.FACING)==expected.getValue(DoorBlock.FACING),
                    "Rotated door must survive actual chunk placement: turn="+turn+", entry="+e+", got="+level.getBlockState(door));
            for(var b:blueprint.entrySet())if(b.getValue().material()==MountainArchitecture.Material.BED_FOOT || b.getValue().material()==MountainArchitecture.Material.BED_HEAD) {
                var c=b.getKey();var p=new BlockPos(plan.worldX(c.u(),c.v()),c.y(),plan.worldZ(c.u(),c.v()));
                helper.assertTrue(level.getBlockState(p).is(Blocks.YELLOW_BED),"Both rotated bed halves need to survive placement: "+p);
            }
            for(var b:blueprint.entrySet())if(b.getValue().material()==MountainArchitecture.Material.LANTERN) {
                var c=b.getKey();var p=new BlockPos(plan.worldX(c.u(),c.v()),c.y(),plan.worldZ(c.u(),c.v()));
                helper.assertTrue(level.getBlockState(p).is(Blocks.LANTERN) && level.getBlockState(p.below()).is(Blocks.SPRUCE_SLAB),
                        "Window lanterns and their table supports must survive real block placement");
            }
            helper.assertTrue(saved.createTag(context).getInt("Residents")==3,"Both residents must spawn at usable saved locations");
        }
        helper.succeed();
    }
    private AdaptiveTownGameTests() {}
}
