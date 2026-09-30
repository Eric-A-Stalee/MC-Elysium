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
        for(int radius=5;radius<=6;radius++)for(int height=24;height<=28;height+=2) {
            var tree=new TownLandscapePlan.Tree(0,0,0,height,radius,42);var geometry=tree.geometry();
            long leaves=geometry.values().stream().filter(k->k==TownLandscapePlan.Kind.LEAVES).count();
            helper.assertTrue(leaves>600,"Settlement trees need substantial crowns");
            helper.assertTrue(geometry.entrySet().stream().filter(e->e.getKey().y()<10).allMatch(e->e.getKey().x()==0 && e.getKey().z()==0),
                    "The crown must leave open space below for settlement views");
            helper.assertTrue(geometry.equals(tree.geometry()),"Tree geometry must not depend on chunk placement order");
        }
        helper.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void townLandscapeSurvivesReloadAndRespectsClipsAndBuildings(GameTestHelper helper) {
        var level=helper.getLevel();var at=helper.absolutePos(new BlockPos(3200,90,3200));
        var tree=new TownLandscapePlan.Tree(at.getX(),at.getY(),at.getZ(),26,6,77);
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
        var saved=(MountainBuildingPiece)ModStructures.MOUNTAIN_BUILDING_PIECE.get().load(context,tag);
        var expected=LegacyMountainArchitecture.build(old);var actual=saved.geometry();
        helper.assertTrue(actual.size()==expected.size(),"Legacy geometry size must remain stable");
        expected.forEach((p,b)->helper.assertTrue(actual.get(new MountainArchitecture.Cell(p.u(),p.y(),p.v())).material().name().equals(b.material().name()),
                "Old saved pieces must not acquire the new wall and roof grammar"));
        helper.assertTrue(saved.createTag(context).getInt("Grammar")==1 && saved.createTag(context).getIntArray("Rooms").length==rooms.size()*7,
                "Saving a legacy piece must preserve its original version and layout");
        helper.succeed();
    }
    private SettlementAtmosphereGameTests() {}
}
