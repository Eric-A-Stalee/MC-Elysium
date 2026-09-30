package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class GrandTownGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void grandTownConnectsLargeHomesOnBothBanks(GameTestHelper helper) {
        var terrain=new LandscapePlanner.Terrain() {
            @Override public int height(int x,int z) {return Math.abs(x)<6?59:64+Math.max(0,Math.abs(x)-20)/12+Math.floorDiv(z,80);}
            @Override public boolean water(int x,int z) {return Math.abs(x)<6;}
        };
        var bridge=BridgePlanner.find(terrain::height,0,0,true,63).orElseThrow();
        var plan=ValleyTownPlanner.plan(terrain,bridge,63,new TerracePlanner.Module(13,8),new TerracePlanner.Module(6,6),
                List.of(new TerracePlanner.Module(11,8)),14,24).orElseThrow();
        helper.assertTrue(plan.lots().size()>=17 && plan.lots().size()<=27,"Grand town needs 14-24 houses, hall, tower and square");
        helper.assertTrue(plan.lots().stream().filter(l->l.x()<0).count()>=5 && plan.lots().stream().filter(l->l.x()>0).count()>=5,
                "The crossing must connect inhabited districts on both banks");
        var walk=new HashMap<TerracePlanner.Point,Integer>();
        for(var c:plan.columns()) {
            helper.assertTrue(Math.abs(c.ground()-c.original())<=6,"Earthworks must remain bounded");
            if(c.kind()==ValleyTownPlanner.ROAD)walk.put(new TerracePlanner.Point(c.x(),c.z()),c.ground());
        }
        for(int a=0;a<bridge.length();a++)for(int s=-1;s<=1;s++)walk.put(new TerracePlanner.Point(bridge.x(a,s),bridge.z(a,s)),bridge.walkingHeight(a)-1);
        for(var lot:plan.lots())walk.put(lot.door(),lot.ground());
        var seen=new HashSet<TerracePlanner.Point>();var queue=new ArrayDeque<TerracePlanner.Point>();
        queue.add(new TerracePlanner.Point(0,0));
        while(!queue.isEmpty()) {
            var p=queue.remove();if(!seen.add(p))continue;
            for(Direction d:Direction.Plane.HORIZONTAL) {
                var next=new TerracePlanner.Point(p.x()+d.getStepX(),p.z()+d.getStepZ());
                if(walk.containsKey(next) && !seen.contains(next) && Math.abs(walk.get(next)-walk.get(p))<=1)queue.add(next);
            }
        }
        for(var lot:plan.lots())helper.assertTrue(seen.contains(lot.door()),"Every building must reach the bridge via the actual saved road graph: "+lot);
        var cliff=new LandscapePlanner.Terrain() {
            @Override public int height(int x,int z){return 100+x*3;}
            @Override public boolean water(int x,int z){return false;}
        };
        helper.assertTrue(ValleyTownPlanner.pad(cliff,0,0,11,63,8).isEmpty(),"A large house must reject a cliff-sized cut");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=80)
    public static void townEarthworksPersistCutsAndStayInsideTheirClip(GameTestHelper helper) {
        var level=helper.getLevel();var at=helper.absolutePos(new BlockPos(2,60,2));
        var piece=new TownTerrainPiece(List.of(new ValleyTownPlanner.Column(at.getX(),at.getZ(),at.getY(),at.getY()-3,0),
                new ValleyTownPlanner.Column(at.getX()+1,at.getZ(),at.getY(),at.getY()+2,1)));
        var context=StructurePieceSerializationContext.fromLevel(level);var tag=piece.createTag(context);
        var loaded=ModStructures.TOWN_TERRAIN_PIECE.get().load(context,tag);
        helper.assertTrue(tag.equals(loaded.createTag(context)),"Terrace cuts and fills must survive the registered NBT loader");
        for(int y=-3;y<=3;y++)level.setBlock(at.offset(1,y,0),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(at,Blocks.RED_CONCRETE.defaultBlockState(),2);
        var clip=new BoundingBox(at.getX()+1,at.getY()-4,at.getZ(),at.getX()+1,at.getY()+4,at.getZ());
        loaded.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,new ChunkPos(at),at);
        helper.assertTrue(level.getBlockState(at).is(Blocks.RED_CONCRETE),"Terrain preparation must not touch the next chunk");
        helper.assertTrue(level.getBlockState(at.offset(1,2,0)).isAir(),"A cut must actually remove the original high ground");
        helper.assertTrue(!level.getBlockState(at.offset(1,0,0)).isAir(),"The road needs a real solid surface");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=80)
    public static void expandedVaultHasUsableGalleriesAndKeepsLegacyDimensions(GameTestHelper helper) {
        var plan=new TartarusChainPlan(0,0,150,-48,true);var piece=new TartarusChainPiece(plan);
        helper.assertTrue(piece.getBoundingBox().getXSpan()==73 && plan.roof(0,0)-plan.floor()==54,"The new vault must be substantially larger");
        helper.assertTrue(piece.materialAt(28,plan.floor()+16,8).is(Blocks.POLISHED_BLACKSTONE_BRICKS),"Gallery must have a real floor");
        helper.assertTrue(piece.materialAt(28,plan.floor()+17,8).isAir(),"Gallery needs headroom");
        for(int along=11;along<=26;along++) {
            int y=plan.floor()+along-10;
            helper.assertTrue(piece.materialAt(along,y,0).getBlock() instanceof StairBlock,"Gallery stair flight must be continuous");
            helper.assertTrue(piece.materialAt(along,y+1,0).isAir() && piece.materialAt(along,y+2,0).isAir(),"Gallery floor may not cover its own staircase");
        }
        var context=StructurePieceSerializationContext.fromLevel(helper.getLevel());var tag=piece.createTag(context);
        tag.remove("ChamberRadius");tag.remove("VaultHeight");
        var legacy=(TartarusChainPiece)ModStructures.TARTARUS_CHAIN_PIECE.get().load(context,tag);
        helper.assertTrue(legacy.plan().chamberRadius()==12 && legacy.plan().vaultHeight()==22,"Older partly generated chains must retain their original chamber dimensions");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=3600)
    public static void naturalGrandTownCandidateHasCrossingAndCompleteDistricts(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);var generator=realm.getChunkSource().getGenerator();
        var id=ResourceLocation.fromNamespaceAndPath("elysium","grand_mountain_town");
        var structure=(ContourTownStructure)realm.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        var placement=(RandomSpreadStructurePlacement)realm.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        class Probe implements Runnable {
            int attempt;
            final java.util.Map<String,Integer> survey=new java.util.TreeMap<>();
            @Override public void run() {
                for(int n=0;n<8 && attempt<12000;n++,attempt++) {
                    var chunk=placement.getPotentialStructureChunk(realm.getSeed(),(attempt%120-60)*52,(attempt/120-50)*52);
                    if(!placement.isStructureChunk(realm.getChunkSource().getGeneratorState(),chunk.x,chunk.z))continue;
                    var context=new Structure.GenerationContext(realm.registryAccess(),generator,generator.getBiomeSource(),realm.getChunkSource().randomState(),
                            realm.getServer().getStructureManager(),realm.getSeed(),chunk,realm,structure.biomes()::contains);
                    var stub=structure.surveyGenerationPoint(context,key->survey.merge(key,1,Integer::sum));if(stub.isEmpty())continue;
                    var pieces=stub.get().getPiecesBuilder().build().pieces();
                    helper.assertTrue(pieces.size()>=19 && pieces.getFirst() instanceof TownTerrainPiece && pieces.getLast() instanceof ElysianBridgePiece,
                            "A real town needs complete districts, saved terraces and a crossing");
                    var serialization=StructurePieceSerializationContext.fromLevel(realm);
                    for(var p:pieces) {
                        var tag=p.createTag(serialization);
                        helper.assertTrue(p.getType().load(serialization,tag).createTag(serialization).equals(tag),"Every town piece must survive reload");
                    }
                    var actual=StructurePlacementProbe.place(helper,realm,structure,chunk);
                    helper.assertTrue(actual!=null && actual.isValid() && actual.getPieces().size()==pieces.size(),
                            "The natural candidate must retain every piece through generation and placement");
                    for(int i=2;i<pieces.size()-1;i++) {
                        var b=((MountainBuildingPiece)pieces.get(i)).plan();
                        MountainPlanChecks.verifyGeometry(b);
                        var floor=new BlockPos(b.worldX(0,0),b.main().floor(),b.worldZ(0,0));
                        helper.assertTrue(realm.getBlockState(floor).is(Blocks.SPRUCE_PLANKS),
                                "Generated halls, watch lodge and homes must retain their surveyed interior floors: "+floor);
                        var e=b.entry();var door=new BlockPos(b.worldX(e.u(),e.v()),e.floor()+1,b.worldZ(e.u(),e.v()));
                        helper.assertTrue(realm.getBlockState(door).is(Blocks.SPRUCE_DOOR),"Every saved entry needs a real door: "+door);
                    }
                    var crossing=((ElysianBridgePiece)pieces.getLast()).span();int middle=crossing.length()/2;
                    var deck=new BlockPos(crossing.x(middle,0),crossing.walkingHeight(middle)-1,crossing.z(middle,0));
                    helper.assertTrue(realm.getBlockState(deck).is(Blocks.STONE_BRICKS) && realm.getBlockState(deck.above()).isAir(),
                            "The naturally generated town needs an unobstructed stone crossing");
                    LogUtils.getLogger().info("Elysium grand mountain town candidate: seed={}, chunk={}, pieces={}, candidates={}",realm.getSeed(),chunk,pieces.size(),attempt+1);
                    LogUtils.getLogger().info("Elysium grand town survey: {}",survey);
                    helper.succeed();return;
                }
                if(attempt>=12000)LogUtils.getLogger().info("Elysium grand town survey: {}",survey);
                helper.assertTrue(attempt<12000,"No complete grand mountain town found in bounded natural candidate survey: "+survey);
                helper.runAfterDelay(1,()->run());
            }
        }
        new Probe().run();
    }
    private GrandTownGameTests() {}
}
