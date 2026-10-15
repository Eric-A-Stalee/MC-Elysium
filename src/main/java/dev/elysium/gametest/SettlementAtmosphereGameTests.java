package dev.elysium.gametest;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class SettlementAtmosphereGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void matureBirchesKeepBroadCrownsAndClearTrunks(GameTestHelper helper) {
        for(int radius=5;radius<=6;radius++)for(int height=16;height<=20;height+=2) {
            var tree=new TownLandscapePlan.Tree(0,0,0,height,radius,42);var geometry=tree.geometry();
            long leaves=geometry.values().stream().filter(k->k==TownLandscapePlan.Kind.LEAVES).count();
            helper.assertTrue(leaves>200,"Branching settlement trees need substantial separate crowns");
            var leafHeights=geometry.entrySet().stream().filter(e->e.getValue()==TownLandscapePlan.Kind.LEAVES).mapToInt(e->e.getKey().y()).summaryStatistics();
            helper.assertTrue(leafHeights.getMax()-leafHeights.getMin()>=10,"Crowns must have substantial vertical depth, not a shared flat canopy");
            helper.assertTrue(geometry.entrySet().stream().filter(e->e.getKey().y()<4).allMatch(e->Math.abs(e.getKey().x())<=1 && e.getKey().z()==0
                    && e.getValue()!=TownLandscapePlan.Kind.LEAVES),"Only a narrow root flare belongs below the branching trunk");
            var logs=geometry.entrySet().stream().filter(e->e.getValue()!=TownLandscapePlan.Kind.LEAVES).map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
            var seen=new java.util.HashSet<TownLandscapePlan.Cell>();var queue=new java.util.ArrayDeque<TownLandscapePlan.Cell>();queue.add(new TownLandscapePlan.Cell(0,0,0));
            while(!queue.isEmpty()) {
                var p=queue.remove();if(!seen.add(p))continue;
                for(var d:net.minecraft.core.Direction.values()) {
                    var q=new TownLandscapePlan.Cell(p.x()+d.getStepX(),p.y()+d.getStepY(),p.z()+d.getStepZ());
                    if(logs.contains(q) && !seen.contains(q))queue.add(q);
                }
            }
            helper.assertTrue(seen.containsAll(logs),"Every visible limb needs a face-connected route to the trunk");
            helper.assertTrue(logs.stream().anyMatch(p->p.y()<10 && Math.abs(p.x())+Math.abs(p.z())>=2),"Trees must branch before becoming bare poles");
            helper.assertTrue(geometry.equals(tree.geometry()),"Tree geometry must not depend on chunk placement order");
        }
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void townLandscapeSurvivesReloadAndRespectsClipsAndBuildings(GameTestHelper helper) {
        var level=helper.getLevel();var at=helper.absolutePos(new BlockPos(3200,90,3200));
        var tree=new TownLandscapePlan.Tree(at.getX(),at.getY(),at.getZ(),20,6,77);
        var protectedCell=tree.geometry().keySet().stream().filter(c->c.x()!=tree.x() && c.y()>tree.y()+15).findFirst().orElseThrow();
        var obstacle=new TownLandscapePlan.Obstacle(protectedCell.x(),protectedCell.y(),protectedCell.z(),protectedCell.x(),protectedCell.y(),protectedCell.z());
        var plan=new TownLandscapePlan(List.of(tree),List.of(),List.of(obstacle));var piece=new TownLandscapePiece(plan);
        var context=StructurePieceSerializationContext.fromLevel(level);var tag=piece.createTag(context);
        var saved=(TownLandscapePiece)ModStructures.TOWN_LANDSCAPE_PIECE.get().load(context,tag);
        helper.assertTrue(saved.plan().equals(plan) && saved.createTag(context).equals(tag),"Trees, obstacles and decorations must round-trip exactly");
        var mark=new BlockPos(protectedCell.x(),protectedCell.y(),protectedCell.z());
        var box=piece.getBoundingBox();var clip=new BoundingBox(box.minX(),box.minY(),box.minZ(),at.getX()-1,box.maxY(),box.maxZ());
        saved.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,new ChunkPos(at),at);
        helper.assertTrue(level.getBlockState(at).isAir(),"The neighboring chunk's trunk must remain untouched");
        saved.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,box,new ChunkPos(at),at);
        helper.assertTrue(level.getBlockState(at).is(Blocks.BIRCH_LOG),"Saved trunk must be placed");
        helper.assertTrue(level.getBlockState(mark).isAir(),"Saved building clearance must trim a canopy even before that building exists");
        var leaf=tree.geometry().entrySet().stream().filter(e->e.getValue()==TownLandscapePlan.Kind.LEAVES && !obstacle.contains(e.getKey())).findFirst().orElseThrow().getKey();
        var state=level.getBlockState(new BlockPos(leaf.x(),leaf.y(),leaf.z()));
        helper.assertTrue(state.is(ModBlocks.GOLDEN_BIRCH_LEAVES.get()) && state.getValue(LeavesBlock.PERSISTENT),"Broad authored crowns must not decay at branch tips");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void legacyMountainPiecesKeepTheirSavedGrammar(GameTestHelper helper) {
        var fitted=MountainBuildingPlan.fit(MountainPlanChecks.slope(0),0,0,0,2,MountainBuildingPlan.Style.CROSS_GABLE,63,new MountainBuildingPlan.Point(0,30)).orElseThrow();
        var rooms=fitted.rooms().stream().map(r->new MountainBuildingPlan.Room(r.u(),r.v(),r.width(),r.depth(),r.floor(),r.storeys(),r.crossRoof())).toList();
        var old=new MountainBuildingPlan(0,0,0,2,fitted.style(),rooms,fitted.entry(),fitted.ground(),false,0);
        var context=StructurePieceSerializationContext.fromLevel(helper.getLevel());var tag=new MountainBuildingPiece(old).createTag(context);
        tag.putInt("Grammar",1);int[] data=new int[rooms.size()*7];int i=0;
        for(var r:rooms){data[i++]=r.u();data[i++]=r.v();data[i++]=r.width();data[i++]=r.depth();data[i++]=r.floor();data[i++]=r.storeys();data[i++]=r.crossRoof()?1:0;}
        tag.putIntArray("Rooms",data);
        tag.putIntArray("Entry",java.util.Arrays.copyOf(tag.getIntArray("Entry"),5));
        var saved=(MountainBuildingPiece)ModStructures.MOUNTAIN_BUILDING_PIECE.get().load(context,tag);
        var expected=LegacyMountainArchitecture.build(old);var actual=saved.geometry();
        helper.assertTrue(actual.size()==expected.size(),"Legacy geometry size must remain stable");
        expected.forEach((p,b)->helper.assertTrue(actual.get(new MountainArchitecture.Cell(p.u(),p.y(),p.v())).material().name().equals(b.material().name()),
                "Old saved pieces must not acquire the new wall and roof grammar"));
        helper.assertTrue(saved.createTag(context).getInt("Grammar")==1 && saved.createTag(context).getIntArray("Rooms").length==rooms.size()*7,
                "Saving a legacy piece must preserve its original version and layout");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void alphaEightBuildingsAndTreesRetainTheirOriginalGeometry(GameTestHelper helper) {
        var b=MountainBuildingPlan.fit(MountainPlanChecks.slope(0),0,0,0,2,MountainBuildingPlan.Style.CROSS_GABLE,63,new MountainBuildingPlan.Point(0,30)).orElseThrow();
        var context=StructurePieceSerializationContext.fromLevel(helper.getLevel());var tag=new MountainBuildingPiece(b).createTag(context);
        tag.putInt("Grammar",2);int[] data=new int[b.rooms().size()*10];int i=0;
        for(var r:b.rooms()) {data[i++]=r.u();data[i++]=r.v();data[i++]=r.width();data[i++]=r.depth();data[i++]=r.floor();data[i++]=r.storeys();
            data[i++]=r.crossRoof()?1:0;data[i++]=r.jetty();data[i++]=r.roofShift();data[i++]=r.facade();}
        tag.putIntArray("Rooms",data);tag.putIntArray("Entry",java.util.Arrays.copyOf(tag.getIntArray("Entry"),5));
        var saved=(MountainBuildingPiece)ModStructures.MOUNTAIN_BUILDING_PIECE.get().load(context,tag);
        var expected=MountainArchitectureV2.build(saved.plan());var actual=saved.geometry();
        helper.assertTrue(actual.size()==expected.size(),"Grammar-2 geometry size must remain stable");
        expected.forEach((p,v)->helper.assertTrue(actual.get(new MountainArchitecture.Cell(p.u(),p.y(),p.v())).material().name().equals(v.material().name()),
                "Saved alpha-8 walls, roof and lights must retain their original grammar"));
        helper.assertTrue(saved.createTag(context).getInt("Grammar")==2 && saved.createTag(context).getIntArray("Rooms").length==data.length,
                "Grammar-2 room data must not migrate on reload");
        helper.assertTrue(saved.state(new MountainArchitecture.Voxel(MountainArchitecture.Material.GLASS)).is(Blocks.LIGHT_GRAY_STAINED_GLASS),
                "Old saved windows must keep their original material");
        var tree=new TownLandscapePlan.Tree(0,100,0,26,6,77,1);
        var treePiece=new TownLandscapePiece(new TownLandscapePlan(List.of(tree),List.of(),List.of()));var treeTag=treePiece.createTag(context);
        var reloaded=(TownLandscapePiece)ModStructures.TOWN_LANDSCAPE_PIECE.get().load(context,treeTag);
        helper.assertTrue(treeTag.getInt("Version")==1 && reloaded.createTag(context).equals(treeTag)
                && reloaded.plan().trees().getFirst().geometry().equals(tree.geometry()),"Partially generated alpha-8 trees must keep their original crowns");
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void alphaNineBuildingsAndTreesKeepTheirSavedGeometry(GameTestHelper helper) {
        var rooms=List.of(new MountainBuildingPlan.Room(-4,-7,9,15,99,2,false,1,1,1,0),
                new MountainBuildingPlan.Room(-10,-5,7,11,99,1,false,0,0,2,-1));
        var entry=new MountainBuildingPlan.Entry(0,7,0,1,99,2);var ground=new java.util.ArrayList<MountainBuildingPlan.Ground>();
        for(int u=-10;u<=4;u++)for(int v=-7;v<=9;v++) {
            final int x=u,z=v;
            if(rooms.stream().anyMatch(r->r.contains(x,z,0)) || entry.porch(u,v,0))ground.add(new MountainBuildingPlan.Ground(u,v,99));
        }
        var plan=new MountainBuildingPlan(0,0,0,77,MountainBuildingPlan.Style.CROSS_GABLE,rooms,entry,ground,false,0);
        var context=StructurePieceSerializationContext.fromLevel(helper.getLevel());var tag=new MountainBuildingPiece(plan).createTag(context);
        tag.putInt("Grammar",3);
        var reloaded=(MountainBuildingPiece)ModStructures.MOUNTAIN_BUILDING_PIECE.get().load(context,tag);
        // Captured from the published alpha-9 implementation before adding grammar 4.
        helper.assertTrue(geometryHash(reloaded.geometry()).equals("86ba49a914acfd81bc60fabed32fcbdbe3ab6d0eb7d98b85306a52be109e1fd1"),
                "Saved alpha-9 houses must retain every block and orientation");
        helper.assertTrue(reloaded.createTag(context).equals(tag),"An older building must retain its saved grammar on resave");
        var oldTree=new TownLandscapePlan.Tree(0,100,0,18,6,77,2);
        var piece=new TownLandscapePiece(new TownLandscapePlan(List.of(oldTree),List.of(),List.of()));var treeTag=piece.createTag(context);
        var saved=(TownLandscapePiece)ModStructures.TOWN_LANDSCAPE_PIECE.get().load(context,treeTag);
        helper.assertTrue(treeTag.getInt("Version")==2 && saved.createTag(context).equals(treeTag)
                && geometryHash(saved.plan().trees().getFirst().geometry()).equals("9ca98a90f51bb8ae8e12e19e0109ec1499f36fc3c214aedc552a1d41ba842d8f"),
                "Saved alpha-9 crowns and branches must not change when neighboring chunks generate");
        helper.succeed();
    }
    private static String geometryHash(java.util.Map<?,?> geometry) {
        try {
            var lines=geometry.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).sorted().toList();
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(
                    String.join("\n",lines).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}
    }
    private SettlementAtmosphereGameTests() {}
}
