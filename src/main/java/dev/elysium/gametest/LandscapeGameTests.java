package dev.elysium.gametest;

import com.mojang.logging.LogUtils;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModStructures;
import dev.elysium.structure.BankPathPiece;
import dev.elysium.structure.LandscapePiece;
import dev.elysium.structure.LandscapePlanner;
import dev.elysium.structure.LandscapeStructure;
import dev.elysium.worldgen.NearWaterFilter;
import java.util.Optional;
import java.util.function.IntBinaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class LandscapeGameTests {
    private LandscapeGameTests() {}

    private static LandscapePlanner.Terrain terrain(IntBinaryOperator heights, boolean wet) {
        return new LandscapePlanner.Terrain() {
            @Override public int height(int x, int z) { return heights.applyAsInt(x, z); }
            @Override public boolean water(int x, int z) { return wet && height(x, z) < 63; }
        };
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void sitesRejectWetCornersNarrowRidgesAndFalseVistas(GameTestHelper helper) {
        helper.assertTrue(LandscapePlanner.pad(terrain((x, z) -> 65, false), 0, 0, 6, 63, 2).orElseThrow() == 64,
                "Dry level ground must support a landmark");
        helper.assertTrue(LandscapePlanner.pad(terrain((x, z) -> x == 2 && z == 3 ? 90 : 65, false),
                0, 0, 6, 63, 2).isEmpty(), "A ridge between coarse probes must fail the full footprint check");
        helper.assertTrue(LandscapePlanner.pad(terrain((x, z) -> x == -6 && z == 6 ? 60 : 65, true),
                0, 0, 6, 63, 2).isEmpty(), "A submerged corner must reject the site");
        helper.assertTrue(LandscapePlanner.outlook(terrain((x, z) -> 80, false), 0, 0, 79, 8).isEmpty(),
                "Flat ground must not be presented as a scenic overlook");
        helper.assertTrue(LandscapePlanner.outlook(terrain((x, z) -> z < -20 ? 65 : 80, false),
                0, 0, 79, 8).orElseThrow() == Direction.NORTH, "Lookouts must face the actual drop");
        helper.assertTrue(LandscapePlanner.outlook(terrain((x, z) -> z == -24 ? 65 : 80, false),
                0, 0, 79, 8).isEmpty(), "An isolated depression is not a sustained view");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void bankRoutesRequireRealWaterGentleStepsAndDryEdges(GameTestHelper helper) {
        var river = terrain((x, z) -> x >= 28 ? 60 : 64, true);
        var route = LandscapePlanner.bankPath(river, 0, 0, 16, 63, 63).orElseThrow();
        helper.assertTrue(route.direction() == Direction.EAST && route.length() == 11 && route.x(10, 0) == 27,
                "The path must meet the bank and stop before water");
        helper.assertTrue(LandscapePlanner.bankPath(terrain((x, z) -> x >= 28 ? 60 : 64, false),
                0, 0, 16, 63, 63).isEmpty(), "A dry basin must not receive a water landing");
        helper.assertTrue(LandscapePlanner.bankPath(terrain((x, z) -> x >= 28 ? 60 : 80, true),
                0, 0, 16, 79, 63).isEmpty(), "A cliff at the water must not receive an unsupported path");
        helper.assertTrue(LandscapePlanner.bankPath(terrain((x, z) -> x >= 28 || x == 20 && z == 1 ? 60 : 64, true),
                0, 0, 16, 63, 63).isEmpty(), "The whole three-block path must remain dry");
        helper.assertTrue(LandscapePlanner.bankPath(terrain((x, z) -> x >= 60 ? 60 : 64, true),
                0, 0, 16, 63, 63).isEmpty(), "Searching for a bank must stop at the fixed distance limit");
        int[] exported = route.surfaces(); exported[0] = -1000;
        helper.assertTrue(route.surface(0) == 63, "Exported arrays cannot mutate the saved route");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void landscapePiecesKeepRotationsPathsAndChunkClippingOnReload(GameTestHelper helper) {
        var level = helper.getLevel();
        var context = StructurePieceSerializationContext.fromLevel(level);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 2, 4));
        for (Rotation rotation : Rotation.values()) {
            var original = new LandscapePiece(context.structureTemplateManager(),
                    ResourceLocation.fromNamespaceAndPath("elysium", "sunlit_lookout"), origin, rotation, 6, 3);
            var tag = original.createTag(context);
            var loaded = ModStructures.LANDSCAPE_PIECE.get().load(context, tag);
            helper.assertTrue(loaded.createTag(context).equals(tag), "Template rotation, pivot, bounds and foundations must survive reload");
        }
        int y = origin.getY();
        var path = new LandscapePlanner.BankPath(origin.getX(), origin.getZ(), Direction.EAST,
                new int[]{y, y, y + 1, y + 1, y, y, y});
        var original = new BankPathPiece(path);
        var tag = original.createTag(context);
        var loaded = ModStructures.BANK_PATH_PIECE.get().load(context, tag);
        helper.assertTrue(loaded.createTag(context).equals(tag), "Path heights and bounds must survive reload");
        BlockPos sentinel = new BlockPos(path.x(0, 0), y, path.z(0, 0));
        level.setBlock(sentinel, Blocks.RED_CONCRETE.defaultBlockState(), 2);
        var clip = new BoundingBox(path.x(2, -1), y - 1, path.z(2, -1), path.x(5, 1), y + 4, path.z(5, 1));
        loaded.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                clip, new ChunkPos(origin), origin);
        helper.assertTrue(level.getBlockState(sentinel).is(Blocks.RED_CONCRETE), "Path must never write outside its current chunk clip");
        helper.assertTrue(level.getBlockState(new BlockPos(path.x(2, 0), y + 1, path.z(2, 0)))
                .is(Blocks.SMOOTH_SANDSTONE_STAIRS), "A rise must receive a walkable stair");
        helper.assertTrue(level.getBlockState(new BlockPos(path.x(5, 1), y + 2, path.z(5, 1))).is(Blocks.LANTERN),
                "The bank landing must receive its lantern");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void riversideTreesRejectDryFieldsAndCoveredWater(GameTestHelper helper) {
        var level = helper.getLevel();
        var generator = level.getChunkSource().getGenerator();
        int sea = generator.getSeaLevel();
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4)).atY(sea + 1);
        var context = new PlacementContext(level, generator, Optional.empty());
        var filter = new NearWaterFilter();
        helper.assertTrue(filter.getPositions(context, level.random, at).findAny().isEmpty(), "Dry field must reject riverside trees");
        BlockPos water = at.offset(3, 0, 0).atY(sea - 1);
        var oldWater = level.getBlockState(water);
        var oldCover = level.getBlockState(water.above());
        level.setBlock(water, Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(water.above(), Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(filter.getPositions(context, level.random, at).findAny().isPresent(), "Actual nearby surface water must allow a tree attempt");
        level.setBlock(water.above(), Blocks.STONE.defaultBlockState(), 2);
        helper.assertTrue(filter.getPositions(context, level.random, at).findAny().isEmpty(), "Covered water must not green-light a field tree");
        level.setBlock(water, oldWater, 2);
        level.setBlock(water.above(), oldCover, 2);
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void springTemplatePlacesContainedWaterWithoutTouchingOutsideItsClip(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(-9, 2, -8));
        var piece = new LandscapePiece(level.getServer().getStructureManager(),
                ResourceLocation.fromNamespaceAndPath("elysium", "elder_spring"), origin, Rotation.NONE, 9, 2);
        var clip = new BoundingBox(origin.getX() + 9, origin.getY() - 2, origin.getZ() + 8,
                origin.getX() + 17, origin.getY() + 3, origin.getZ() + 14);
        BlockPos untouched = origin.offset(7, 0, 7); // An authored foundation, outside this chunk slice.
        var original = level.getBlockState(untouched);
        level.setBlock(untouched, Blocks.RED_CONCRETE.defaultBlockState(), 2);
        piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random,
                clip, new ChunkPos(origin), origin);
        helper.assertTrue(level.getBlockState(origin.offset(13, 1, 11)).is(Blocks.WATER), "Spring template must place actual water");
        helper.assertTrue(level.getBlockState(origin.offset(13, 0, 11)).is(Blocks.SMOOTH_SANDSTONE), "Spring must have a solid floor");
        helper.assertTrue(level.getBlockState(origin.offset(17, 1, 11)).is(Blocks.SMOOTH_SANDSTONE), "Spring must have a containing rim");
        helper.assertTrue(level.getBlockState(untouched).is(Blocks.RED_CONCRETE), "Template and foundation must respect the clip");
        level.setBlock(untouched, original, 2);
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 600)
    public static void landscapeCodecsFindAllSitesInActualElysiumTerrain(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        helper.assertTrue(realm != null, "Elysium must exist for the landmark terrain probe");
        var generator = realm.getChunkSource().getGenerator();
        String[] names = {"elder_spring", "sunlit_lookout", "harvest_hamlet"};
        class Probe implements Runnable {
            int site, attempts;
            @Override public void run() {
                var registered = realm.registryAccess().registryOrThrow(Registries.STRUCTURE)
                        .get(ResourceLocation.fromNamespaceAndPath("elysium", names[site]));
                helper.assertTrue(registered instanceof LandscapeStructure, "The parameterized site codec must load");
                var structure = (LandscapeStructure) registered;
                for (int batch = 0; batch < 3 && attempts < 900; batch++, attempts++) {
                    var chunk = new ChunkPos((attempts % 30 - 15) * 3, (attempts / 30 - 15) * 3);
                    var context = new Structure.GenerationContext(realm.registryAccess(), generator, generator.getBiomeSource(),
                            realm.getChunkSource().randomState(), realm.getServer().getStructureManager(), realm.getSeed(),
                            chunk, realm, structure.biomes()::contains);
                    var stub = structure.findGenerationPoint(context);
                    if (stub.isEmpty()) continue;
                    var pieces = stub.get().getPiecesBuilder().build().pieces();
                    helper.assertTrue(pieces.getFirst() instanceof LandscapePiece, "Every site must create its saved template piece");
                    helper.assertTrue(pieces.size() <= 2, "A site has at most one bounded bank approach");
                    LogUtils.getLogger().info("Elysium landscape probe: site={}, seed={}, chunk={}, pieces={}, attempts={}",
                            names[site], realm.getSeed(), chunk, pieces.size(), attempts + 1);
                    site++; attempts = 0;
                    if (site == names.length) { helper.succeed(); return; }
                    helper.runAfterDelay(1, () -> run()); return;
                }
                helper.assertTrue(attempts < 900, "No valid " + names[site] + " found in 900 bounded Elysium terrain probes");
                helper.runAfterDelay(1, () -> run());
            }
        }
        new Probe().run();
    }
}
