package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.TartarusChainPlan;
import dev.elysium.structure.TartarusChainPiece;
import dev.elysium.structure.TartarusChainStructure;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class TartarusGameTests {
    @GameTest(template="portal_test_empty", timeoutTicks=80)
    public static void chainRequiresACliffAndReachesItsAnchor(GameTestHelper helper) {
        helper.assertTrue(TartarusChainPlan.find((x,z)->150,0,0,-48,true).isEmpty(),"Flat peaks must not produce exposed chains");
        helper.assertTrue(TartarusChainPlan.find((x,z)->70,0,0,-48,true).isEmpty(),"Lowlands must be rejected");
        var p = TartarusChainPlan.find((x,z)->150-Math.max(0,x)*10,0,0,-48,true).orElseThrow();
        var piece = new TartarusChainPiece(p);
        int lastCenter = p.bottomCenter() + ((p.top()-5-p.bottomCenter())/7)*7;
        for (int y=p.floor()+6;y<=lastCenter+5;y++) {
            int count=0;
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(p.chainAt(x,y,z))count++;
            helper.assertTrue(count>0,"Every vertical level must contain a traceable link: "+y);
        }
        helper.assertTrue(p.chainAt(3,p.bottomCenter(),0) && p.chainAt(0,p.bottomCenter()+7,3),"Successive links must alternate planes");
        helper.assertTrue(piece.materialAt(0,p.floor()+6,0).is(ModBlocks.TARTARUS_CHAIN_BLOCK.get()),"Bottom link must enter a real anchor");
        helper.assertTrue(piece.materialAt(6,p.floor()+3,0).isAir(),"Chamber must have an excavated walkable interior");
        helper.assertTrue(piece.materialAt(6,p.roof(6,0),0).is(Blocks.POLISHED_BASALT),"Vault must have actual masonry ribs");
        helper.assertTrue(piece.materialAt(2,80,2)==null,"Buried ring interiors must preserve surrounding stone");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty", timeoutTicks=100)
    public static void chainReloadAndClippingPreserveBothHalves(GameTestHelper helper) {
        var level=helper.getLevel(); var at=helper.absolutePos(new BlockPos(2,90,2));
        // Keep the exhaustive block-by-block reload test on the legacy-size vault.
        var p=new TartarusChainPlan(at.getX(),at.getZ(),at.getY()+55,at.getY(),true,12,22);
        var original=new TartarusChainPiece(p);var context=StructurePieceSerializationContext.fromLevel(level);
        var tag=original.createTag(context);
        var loaded=(TartarusChainPiece)ModStructures.TARTARUS_CHAIN_PIECE.get().load(context,tag);
        helper.assertTrue(tag.equals(loaded.createTag(context)),"Complete route and chamber must survive registered NBT loading");
        var outside=at.offset(13,3,0);level.setBlock(outside,Blocks.RED_CONCRETE.defaultBlockState(),2);
        var b=loaded.getBoundingBox();
        // Excavate actual geology. Minecraft may skip setting cave_air into an
        // empty air section, which is visually equivalent but not state-equal.
        for(int x=b.minX();x<=b.maxX();x++)for(int z=b.minZ();z<=b.maxZ();z++)for(int y=b.minY();y<=b.maxY();y++)
            level.setBlock(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState(),2);
        var left=new BoundingBox(b.minX(),b.minY(),b.minZ(),at.getX()-1,b.maxY(),b.maxZ());
        var right=new BoundingBox(at.getX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ());
        // Reverse generation order relative to coordinate order, reloading in between.
        loaded.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(1),right,new ChunkPos(at),at);
        helper.assertTrue(level.getBlockState(at.offset(-3,11,0)).is(Blocks.STONE),"Right clip may not write into the ungenerated left half");
        loaded=(TartarusChainPiece)ModStructures.TARTARUS_CHAIN_PIECE.get().load(context,tag);
        loaded.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),RandomSource.create(99),left,new ChunkPos(at),at);
        for(int x=b.minX();x<=b.maxX();x++)for(int z=b.minZ();z<=b.maxZ();z++)for(int y=b.minY();y<=b.maxY();y++) {
            var expected=original.materialAt(x-at.getX(),y,z-at.getZ());
            var actual=level.getBlockState(new BlockPos(x,y,z));
            if(expected!=null)helper.assertTrue(actual.equals(expected),"Chunk halves differ at "+x+","+y+","+z+": expected "+expected+", got "+actual);
            else helper.assertTrue(actual.is(Blocks.STONE),"Buried route must retain the original encasing stone");
        }
        helper.assertTrue(level.getBlockState(outside).is(Blocks.RED_CONCRETE),"No writes outside the saved structure bounds");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty", timeoutTicks=160)
    public static void everyBirchVariantGrowsWithAVerticalCrown(GameTestHelper helper) {
        var level=helper.getLevel();var at=helper.absolutePos(new BlockPos(2,200,2));
        var registry=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for(String name:new String[]{"golden_birch","tall_golden_birch","branching_golden_birch","canopy_golden_birch"}) {
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++) {
                level.setBlock(at.offset(x,-1,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);
                for(int y=0;y<30;y++)level.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
            }
            var feature=registry.get(ResourceLocation.fromNamespaceAndPath("elysium",name));
            helper.assertTrue(feature.place(level,level.getChunkSource().getGenerator(),RandomSource.create(71),at),"Registered tree must actually grow: "+name);
            Map<Integer,Integer> layers=new HashMap<>();int logs=0;
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)for(int y=0;y<30;y++) {
                var state=level.getBlockState(at.offset(x,y,z));
                if(state.is(ModBlocks.GOLDEN_BIRCH_LEAVES.get()))layers.merge(y,1,Integer::sum);
                if(state.is(Blocks.BIRCH_LOG))logs++;
            }
            helper.assertTrue(logs>=6 && layers.size()>=5,"Tree must retain a trunk and at least five foliage layers: "+name);
            int top=layers.keySet().stream().max(Integer::compareTo).orElseThrow();
            helper.assertTrue(layers.get(top)<layers.values().stream().max(Integer::compareTo).orElseThrow(),"Crown must taper above its widest layer: "+name);
        }
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=2400)
    public static void naturalChainCandidatesHaveAnIvoryExposureAndBuriedChamber(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var generator=realm.getChunkSource().getGenerator();
        var id=ResourceLocation.fromNamespaceAndPath("elysium","tartarus_chain");
        var structure=(TartarusChainStructure)realm.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        var placement=(RandomSpreadStructurePlacement)realm.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(id).placement();
        class Probe implements Runnable {
            int index;
            @Override public void run() {
                for(int batch=0;batch<8 && index<6400;batch++,index++) {
                    var chunk=placement.getPotentialStructureChunk(realm.getSeed(),(index%80-40)*28,(index/80-40)*28);
                    var context=new Structure.GenerationContext(realm.registryAccess(),generator,generator.getBiomeSource(),
                            realm.getChunkSource().randomState(),realm.getServer().getStructureManager(),realm.getSeed(),chunk,realm,structure.biomes()::contains);
                    var stub=structure.findGenerationPoint(context);if(stub.isEmpty())continue;
                    var pieces=stub.get().getPiecesBuilder().build().pieces();
                    helper.assertTrue(pieces.size()==1 && pieces.getFirst() instanceof TartarusChainPiece,"Exposure and chamber must share one saved piece");
                    var p=((TartarusChainPiece)pieces.getFirst()).plan();
                    helper.assertTrue(p.top()>100 && p.floor()==-48,"Surface clue must continue deep underground");
                    var actual=realm.getChunk(chunk.x,chunk.z).getStartForStructure(structure);
                    helper.assertTrue(actual!=null && actual.isValid(),"The exposed chain must generate through its real structure set");
                    helper.assertTrue(realm.getBlockState(new BlockPos(p.x(),p.floor()+6,p.z())).is(ModBlocks.TARTARUS_CHAIN_BLOCK.get()),
                            "The natural chain must reach the buried anchor");
                    helper.assertTrue(realm.getBlockState(new BlockPos(p.x()+28,p.floor()+16,p.z()+8)).is(Blocks.POLISHED_BLACKSTONE_BRICKS),
                            "Natural generation must place the expanded gallery across chunk boundaries");
                    LogUtils.getLogger().info("Elysium natural chain candidate: seed={}, chunk={}, x={}, z={}, top={}, floor={}, candidates={}",realm.getSeed(),chunk,p.x(),p.z(),p.top(),p.floor(),index+1);
                    helper.succeed();return;
                }
                helper.assertTrue(index<6400,"No exposed chain found in bounded natural candidate survey");
                helper.runAfterDelay(1,()->run());
            }
        }
        new Probe().run();
    }
    private TartarusGameTests() {}
}
