package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.BridgePlanner;
import dev.elysium.structure.ElysianBridgePiece;
import dev.elysium.structure.ElysianBridgeStructure;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Bridge geometry, persistence, and clipping contracts; development tests are excluded from the release jar. */
@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class BridgeGameTests {
    private BridgeGameTests() {}

    @GameTest(template = "portal_test_empty", timeoutTicks = 400)
    public static void bridgeFindsRealElysiumRiverUsingOnlyNoiseColumns(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        helper.assertTrue(realm != null, "Elysium must exist for the real terrain bridge probe");
        var generator = realm.getChunkSource().getGenerator();
        var registered = realm.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .get(ResourceLocation.fromNamespaceAndPath("elysium", "elysian_bridge"));
        helper.assertTrue(registered instanceof ElysianBridgeStructure, "The bridge codec must load a registered structure");
        var structure = (ElysianBridgeStructure) registered;
        // Run small batches across ticks so an unsuccessful bounded search never blocks the server watchdog.
        class RiverProbe implements Runnable {
            private int attempts;

            @Override
            public void run() {
                for (int batch = 0; batch < 4 && attempts < 400; batch++, attempts++) {
                    int chunkX = (attempts % 20 - 10) * 2;
                    int chunkZ = (attempts / 20 - 10) * 2;
                    var context = new Structure.GenerationContext(realm.registryAccess(), generator,
                            generator.getBiomeSource(), realm.getChunkSource().randomState(),
                            realm.getServer().getStructureManager(), realm.getSeed(), new ChunkPos(chunkX, chunkZ),
                            realm, structure.biomes()::contains);
                    var generation = structure.findGenerationPoint(context);
                    if (generation.isEmpty()) continue;
                    var pieces = generation.get().getPiecesBuilder().build().pieces();
                    helper.assertTrue(pieces.size() == 1 && pieces.getFirst() instanceof ElysianBridgePiece,
                            "A valid noise crossing must produce exactly one saved bridge piece");
                    var span = ((ElysianBridgePiece) pieces.getFirst()).span();
                    helper.assertTrue(span.length() <= BridgePlanner.MAX_LENGTH,
                            "Real terrain must obey the same bounded span contract as synthetic terrain");
                    LogUtils.getLogger().info("Elysium bridge terrain probe: seed={}, sourceChunk={}, start=({}, {}, {}), length={}, eastWest={}, attempts={}",
                            realm.getSeed(), context.chunkPos(), span.startX(), span.startHeight(), span.startZ(),
                            span.length(), span.eastWest(), attempts + 1);
                    helper.succeed();
                    return;
                }
                helper.assertTrue(attempts < 400, "No valid bridge found in 400 bounded real Elysium terrain probes");
                // The harness keys scheduled work by Runnable identity and removes the current key after running.
                helper.runAfterDelay(1, () -> run());
            }
        }
        new RiverProbe().run();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void bridgeFindsWaterAndRejectsDryOceanAndCliffs(GameTestHelper helper) {
        BridgePlanner.Terrain river = (x, z) -> Math.abs(x) <= 6 ? 60 : 63;
        var eastWest = BridgePlanner.find(river, 0, 0, true, 63);
        helper.assertTrue(eastWest.isPresent(), "A shallow 13-block river with dry banks must yield a crossing");
        var span = eastWest.orElseThrow();
        helper.assertTrue(span.length() <= BridgePlanner.MAX_LENGTH, "Bridge search and piece length must be bounded");
        helper.assertTrue(span.deckHeight() == 65, "Deck must stand two blocks above the sea surface");
        helper.assertTrue(BridgePlanner.find(river, 0, 0, false, 63).isEmpty(),
                "A route following the river must not pretend to reach an opposite bank");
        helper.assertTrue(BridgePlanner.find((x, z) -> 64, 0, 0, true, 63).isEmpty(),
                "Dry plains must never receive a decorative fake river bridge");
        helper.assertTrue(BridgePlanner.find((x, z) -> 60, 0, 0, true, 63).isEmpty(),
                "Open water beyond the bounded search must reject the bridge");
        helper.assertTrue(BridgePlanner.find((x, z) -> Math.abs(x) <= 6 ? 60 : 90, 0, 0, true, 63).isEmpty(),
                "Cliffs must reject a bridge instead of receiving unsupported approaches");
        helper.assertTrue(BridgePlanner.find((x, z) -> Math.abs(x) <= 6 ? 30 : 63, 0, 0, true, 63).isEmpty(),
                "Deep ravines must not create unbounded stone columns");
        helper.assertTrue(BridgePlanner.find((x, z) -> Math.abs(x) <= 6 || z == 2 ? 60 : 63,
                0, 0, true, 63).isEmpty(), "A centerline bank with a submerged edge must not count as a dry landing");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void bridgeStairsAndTerrainProfileSurviveRegisteredPieceReload(GameTestHelper helper) {
        var plan = BridgePlanner.find((x, z) -> Math.abs(z) <= 6 ? 60 : z < 0 ? 63 : 65,
                0, 0, false, 63).orElseThrow();
        var context = StructurePieceSerializationContext.fromLevel(helper.getLevel());
        ElysianBridgePiece original = new ElysianBridgePiece(plan);
        CompoundTag tag = original.createTag(context);
        helper.assertTrue(tag.getString("id").equals("elysium:elysian_bridge"),
                "Saved pieces must use the registered mod piece type");
        ElysianBridgePiece loaded = (ElysianBridgePiece) ModStructures.ELYSIAN_BRIDGE_PIECE.get().load(context, tag);
        helper.assertTrue(loaded.createTag(context).equals(tag), "Serializing a reloaded piece must preserve every saved field");
        helper.assertTrue(original.getBoundingBox().equals(loaded.getBoundingBox()), "Reload must retain identical chunk bounds");
        for (int along = 0; along < plan.length(); along++) {
            helper.assertTrue(plan.walkingHeight(along) == loaded.span().walkingHeight(along),
                    "Reload must never recalculate an approach with different stair heights");
            if (along > 0) helper.assertTrue(Math.abs(plan.walkingHeight(along) - plan.walkingHeight(along - 1)) <= 1,
                    "Walkway steps must never skip a block of height");
        }
        int[] exportedFloors = plan.floorHeights();
        exportedFloors[0] = -1_000;
        helper.assertTrue(plan.floorHeight(0, -2) >= 63, "External array mutation must not corrupt a saved bridge profile");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void bridgePlacementClipsEveryDeckRailAndPierToItsChunkBox(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 4));
        int[] floors = new int[7 * BridgePlanner.WIDTH];
        Arrays.fill(floors, origin.getY() - 1);
        var span = new BridgePlanner.Span(origin.getX(), origin.getZ(), true, 7,
                origin.getY(), origin.getY(), origin.getY(), floors);
        var piece = new ElysianBridgePiece(span);
        BlockPos untouched = new BlockPos(span.x(0, 0), origin.getY() - 1, span.z(0, 0));
        helper.getLevel().setBlock(untouched, Blocks.RED_CONCRETE.defaultBlockState(), 2);
        BoundingBox clippingBox = new BoundingBox(span.x(2, -2), origin.getY() - 2, span.z(2, -2),
                span.x(5, 2), origin.getY() + 3, span.z(5, 2));
        var level = helper.getLevel();
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                clippingBox, new ChunkPos(origin), origin);
        helper.assertTrue(level.getBlockState(untouched).is(Blocks.RED_CONCRETE),
                "A piece must not overwrite any block outside its current chunk clipping box");
        helper.assertTrue(level.getBlockState(new BlockPos(span.x(3, 0), origin.getY() - 1, span.z(3, 0)))
                        .is(Blocks.BIRCH_PLANKS), "The current chunk must receive its walkway");
        helper.assertTrue(level.getBlockState(new BlockPos(span.x(5, 2), origin.getY() + 1, span.z(5, 2)))
                        .is(Blocks.LANTERN), "The saved pier row must receive its warm lantern");
        helper.assertTrue(level.getBlockState(new BlockPos(span.x(6, 2), origin.getY(), span.z(6, 2))).isAir(),
                "Rails outside the current clip must wait for their own chunk");
        helper.succeed();
    }
}
