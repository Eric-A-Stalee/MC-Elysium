package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.LandscapePlanner;
import dev.elysium.structure.MountainTownStructure;
import dev.elysium.structure.TerracePathPiece;
import dev.elysium.structure.TerracePlanner;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class AutumnGameTests {
    private static LandscapePlanner.Terrain terrain(boolean cliff) {
        return new LandscapePlanner.Terrain() {
            @Override public int height(int x,int z) { return 100+Math.floorDiv(x,cliff ? 1 : 12); }
            @Override public boolean water(int x,int z) { return false; }
        };
    }
    @GameTest(template="portal_test_empty",timeoutTicks=80)
    public static void terracesConnectEveryDoorAcrossDifferentGroundHeights(GameTestHelper helper) {
        var hall=new TerracePlanner.Module(8,4);var houses=List.of(new TerracePlanner.Module(6,4));
        var plan=TerracePlanner.plan(terrain(false),0,0,63,hall,houses,3,5).orElseThrow();
        helper.assertTrue(plan.lots().size()>=5,"Town must have a plaza, hall, and at least three cottages");
        helper.assertTrue(plan.lots().stream().map(TerracePlanner.Lot::ground).distinct().count()>1,
                "Sloping terrain must create different building elevations");
        Map<TerracePlanner.Point,Integer> walk=new HashMap<>();
        for(var tile:plan.paths()) {
            helper.assertTrue(tile.ground()-tile.floor()<=4,"Every retaining wall must remain bounded");
            helper.assertTrue(walk.put(new TerracePlanner.Point(tile.x(),tile.z()),tile.ground())==null,
                    "Intersections must have exactly one saved surface");
        }
        var plaza=plan.lots().getFirst();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) walk.put(new TerracePlanner.Point(x,z),plaza.ground());
        for(var lot:plan.lots().subList(1,plan.lots().size())) walk.put(lot.door(),lot.ground());
        Set<TerracePlanner.Point> seen=new HashSet<>();var queue=new ArrayDeque<TerracePlanner.Point>();
        queue.add(new TerracePlanner.Point(0,0));
        while(!queue.isEmpty()) {
            var p=queue.remove();if(!seen.add(p))continue;
            for(Direction d:Direction.Plane.HORIZONTAL) {
                var next=new TerracePlanner.Point(p.x()+d.getStepX(),p.z()+d.getStepZ());
                if(walk.containsKey(next) && !seen.contains(next) && Math.abs(walk.get(next)-walk.get(p))<=1)queue.add(next);
            }
        }
        for(var lot:plan.lots()) helper.assertTrue(seen.contains(lot.door()),"Every door must connect to the plaza with one-block-or-smaller rises");
        helper.assertTrue(TerracePlanner.plan(terrain(true),0,0,63,hall,houses,3,5).isEmpty(),
                "Steep slopes must reject the town rather than create cliff-sized foundations");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=40)
    public static void terracePathsPersistRetainingWallsAndRespectChunkClips(GameTestHelper helper) {
        var level=helper.getLevel();var at=helper.absolutePos(new BlockPos(2,2,2));
        var piece=new TerracePathPiece(List.of(new TerracePlanner.Tile(at.getX(),at.getZ(),at.getY(),at.getY()-2),
                new TerracePlanner.Tile(at.getX()+1,at.getZ(),at.getY()+1,at.getY()-2),
                new TerracePlanner.Tile(at.getX()+2,at.getZ(),at.getY()+1,at.getY()-2)));
        var context=StructurePieceSerializationContext.fromLevel(level);var tag=piece.createTag(context);
        var loaded=ModStructures.TERRACE_PATH_PIECE.get().load(context,tag);
        helper.assertTrue(loaded.createTag(context).equals(tag),"Path tiles and foundations must round-trip through saved NBT");
        level.setBlock(at,Blocks.RED_CONCRETE.defaultBlockState(),2);
        var clip=new BoundingBox(at.getX()+1,at.getY()-2,at.getZ(),at.getX()+2,at.getY()+4,at.getZ());
        loaded.postProcess(level,level.structureManager(),level.getChunkSource().getGenerator(),level.random,clip,new ChunkPos(at),at);
        helper.assertTrue(level.getBlockState(at).is(Blocks.RED_CONCRETE),"Town paths may not cross their generation clip");
        helper.assertTrue(level.getBlockState(at.offset(1,-2,0)).is(Blocks.STONE_BRICKS),"Raised paths need real retaining blocks");
        helper.assertTrue(level.getBlockState(at.offset(1,1,0)).is(Blocks.STONE_BRICK_STAIRS),"A rise needs a stair");
        helper.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=1200)
    public static void newBiomesHaveActualWaterAndDistinctPalettes(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var generator=realm.getChunkSource().getGenerator();var random=realm.getChunkSource().randomState();
        var registry=realm.registryAccess().registryOrThrow(Registries.BIOME);
        Set<String> found=new HashSet<>();Map<String,Integer> wet=new HashMap<>();
        class Probe implements Runnable {
            int index;
            @Override public void run() {
                for(int n=0;n<12 && index<4225;n++,index++) {
                    int x=(index%65-32)*192,z=(index/65-32)*192;
                    int y=generator.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,realm,random);
                    var biome=generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),QuartPos.fromBlock(z),random.sampler());
                    String id=registry.getKey(biome.value()).getPath();found.add(id);
                    if(y<generator.getSeaLevel() && generator.getBaseColumn(x,z,realm,random).getBlock(62).is(Blocks.WATER))wet.merge(id,1,Integer::sum);
                }
                if(index<4225) {helper.runAfterDelay(1,() -> run());return;}
                LogUtils.getLogger().info("Elysium hydrology survey: seed={}, biomes={}, wet columns={}",realm.getSeed(),found,wet);
                helper.assertTrue(found.containsAll(List.of("golden_fields","golden_birch_woods","golden_watermeadows","amber_lakes","elysian_highlands","ivory_peaks")),
                        "All six biomes must occur in the real seeded climate survey: "+found);
                helper.assertTrue(wet.getOrDefault("golden_watermeadows",0)>0 && wet.getOrDefault("amber_lakes",0)>0,
                        "Both new lowland biomes must follow actual water: "+wet);
                var meadow=registry.get(ResourceLocation.fromNamespaceAndPath("elysium","golden_watermeadows"));
                var lake=registry.get(ResourceLocation.fromNamespaceAndPath("elysium","amber_lakes"));
                helper.assertTrue(meadow.getFoliageColor()!=lake.getFoliageColor() && meadow.getWaterColor()!=lake.getWaterColor(),
                        "Amber shores must have their own foliage and water palette");
                helper.succeed();
            }
        }
        new Probe().run();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=1800)
    public static void mountainTownCodecFindsACompleteTerracedSite(GameTestHelper helper) {
        var realm=helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var generator=realm.getChunkSource().getGenerator();
        var structure=(MountainTownStructure)realm.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath("elysium","mountain_town"));
        class Probe implements Runnable {
            int attempt;
            @Override public void run() {
                for(int batch=0;batch<3 && attempt<3600;batch++,attempt++) {
                    var chunk=new ChunkPos((attempt%60-30)*4,(attempt/60-30)*4);
                    var context=new Structure.GenerationContext(realm.registryAccess(),generator,generator.getBiomeSource(),
                            realm.getChunkSource().randomState(),realm.getServer().getStructureManager(),realm.getSeed(),chunk,realm,structure.biomes()::contains);
                    var stub=structure.findGenerationPoint(context);if(stub.isEmpty())continue;
                    var pieces=stub.get().getPiecesBuilder().build().pieces();
                    helper.assertTrue(pieces.size()>=6 && pieces.size()<=8,"A town needs a plaza, hall, three to five cottages and its paths");
                    helper.assertTrue(pieces.getLast() instanceof TerracePathPiece,"Town must persist its connected terrace paths");
                    var serialization=StructurePieceSerializationContext.fromLevel(realm);
                    for(var piece:pieces) {
                        var tag=piece.createTag(serialization);
                        helper.assertTrue(piece.getType().load(serialization,tag).createTag(serialization).equals(tag),"Real town pieces must survive reload");
                    }
                    LogUtils.getLogger().info("Elysium mountain town probe: seed={}, chunk={}, pieces={}, attempts={}",realm.getSeed(),chunk,pieces.size(),attempt+1);
                    helper.succeed();return;
                }
                helper.assertTrue(attempt<3600,"No complete mountain town found in bounded real-terrain survey");
                helper.runAfterDelay(1,() -> run());
            }
        }
        new Probe().run();
    }
    private AutumnGameTests() {}
}
