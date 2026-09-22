package dev.elysium.gametest;

import dev.elysium.portal.AltarPattern;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.portal.HarvestAltarBlock;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real-world checks for the ritual and the travel safety/loot contracts; excluded from the release jar. */
@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class PortalGameTests {
    private static final BlockPos CENTER = new BlockPos(4, 1, 4);

    @GameTest(template = "portal_test_empty", timeoutTicks = 40, skyAccess = true)
    public static void shrineRequiresCompletePillarsOfferingsAndOpenSky(GameTestHelper helper) {
        buildShrine(helper);
        BlockPos center = helper.absolutePos(CENTER);
        helper.assertTrue(AltarPattern.validateStructure(helper.getLevel(), center) == AltarPattern.Problem.NONE,
                "An intact open-air harvest shrine must be valid: " + AltarPattern.validateStructure(helper.getLevel(), center)
                        + "; altar=" + center + "; height=" + helper.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, center.getX(), center.getZ()));

        helper.setBlock(CENTER.offset(2, 0, 2), Blocks.AIR);
        helper.assertTrue(AltarPattern.validateStructure(helper.getLevel(), center) == AltarPattern.Problem.MISSING_HAY,
                "An absent diagonal harvest offering must block the ritual");
        helper.setBlock(CENTER.offset(2, 0, 2), Blocks.HAY_BLOCK);

        helper.setBlock(CENTER.east(3).above(), Blocks.STRIPPED_BIRCH_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        helper.assertTrue(AltarPattern.validateStructure(helper.getLevel(), center) == AltarPattern.Problem.MISSING_PILLARS,
                "A sideways log must not count as an upright pillar");
        helper.setBlock(CENTER.east(3).above(), Blocks.STRIPPED_BIRCH_LOG);

        helper.setBlock(CENTER.above(3), Blocks.STONE);
        helper.assertTrue(AltarPattern.validateStructure(helper.getLevel(), center) == AltarPattern.Problem.BLOCKED_SKY,
                "A roof directly over the altar must block the ritual");
        helper.setBlock(CENTER.above(3), Blocks.GLASS);
        helper.assertTrue(AltarPattern.validateStructure(helper.getLevel(), center) == AltarPattern.Problem.BLOCKED_SKY,
                "An open-air ritual must reject a glass roof even when skylight passes through it");
        helper.setBlock(CENTER.above(3), Blocks.AIR);
        helper.assertTrue(AltarPattern.validateStructure(helper.getLevel(), center) == AltarPattern.Problem.NONE,
                "Repairing the same shrine must restore validity");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 20)
    public static void sunsetWindowSurvivesDayRollover(GameTestHelper helper) {
        helper.assertTrue(!AltarPattern.isSunset(10_999), "An offering before sunset must wait");
        helper.assertTrue(AltarPattern.isSunset(11_000), "The opening boundary belongs to sunset");
        helper.assertTrue(AltarPattern.isSunset(13_000), "The closing boundary belongs to sunset");
        helper.assertTrue(!AltarPattern.isSunset(13_001), "The activation window must close");
        helper.assertTrue(AltarPattern.isSunset(24_000L * 800 + 12_000), "Late-game days must keep the same window");
        helper.assertTrue(!AltarPattern.isSunset(24_000L * 800), "Day rollover must not activate a shrine");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void landingRejectsLavaMagmaAndBlockedHeadroom(GameTestHelper helper) {
        BlockPos feet = new BlockPos(4, 2, 4);
        helper.setBlock(feet.below(), Blocks.SMOOTH_SANDSTONE);
        Vec3 point = Vec3.atBottomCenterOf(helper.absolutePos(feet));
        helper.assertTrue(ElysiumTravel.isSafeStandingPosition(helper.getLevel(), point),
                "Clear two-block headroom over solid ground must accept a player");

        helper.setBlock(feet.above(), Blocks.STONE);
        helper.assertTrue(!ElysiumTravel.isSafeStandingPosition(helper.getLevel(), point),
                "A one-block-high gap would suffocate a returning player");
        helper.setBlock(feet.above(), Blocks.AIR);

        helper.setBlock(feet, Blocks.LAVA);
        helper.assertTrue(!ElysiumTravel.isSafeStandingPosition(helper.getLevel(), point),
                "A return must never place a player into lava");
        helper.setBlock(feet, Blocks.WATER);
        helper.assertTrue(!ElysiumTravel.isSafeStandingPosition(helper.getLevel(), point),
                "Submerged feet do not count as a safe dry landing");
        helper.setBlock(feet, Blocks.AIR);

        helper.setBlock(feet.below(), Blocks.MAGMA_BLOCK);
        helper.assertTrue(!ElysiumTravel.isSafeStandingPosition(helper.getLevel(), point),
                "A solid damage block must still be rejected as a floor");
        helper.setBlock(feet.below(), Blocks.AIR);
        helper.assertTrue(!ElysiumTravel.isSafeStandingPosition(helper.getLevel(), point),
                "Clear air without supporting ground must not count as a landing");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void generatedReturnAltarCannotDuplicateSigils(GameTestHelper helper) {
        BlockPos altar = helper.absolutePos(CENTER);
        BlockState natural = ModBlocks.HARVEST_ALTAR.get().defaultBlockState()
                .setValue(HarvestAltarBlock.ACTIVE, true).setValue(HarvestAltarBlock.HAS_SIGIL, false);
        int naturalSigils = Block.getDrops(natural, helper.getLevel(), altar, null).stream()
                .filter(stack -> stack.is(ModItems.ELYSIUM_SIGIL.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(naturalSigils == 0, "A naturally active return altar must never mint a free sigil");

        BlockState installed = natural.setValue(HarvestAltarBlock.HAS_SIGIL, true);
        int installedSigils = Block.getDrops(installed, helper.getLevel(), altar, null).stream()
                .filter(stack -> stack.is(ModItems.ELYSIUM_SIGIL.get())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(installedSigils == 1, "Breaking an entrance must return exactly its one installed sigil");
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void replacingReturnStoneKeepsTheWayHomeWithoutBypassingEntry(GameTestHelper helper) {
        var elysium = helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        helper.assertTrue(elysium != null, "The Elysium dimension must load before testing return stone placement");
        ItemStack altarItem = new ItemStack(ModBlocks.HARVEST_ALTAR.get());
        var overworldContext = new DirectionalPlaceContext(helper.getLevel(), helper.absolutePos(CENTER),
                Direction.DOWN, altarItem, Direction.UP);
        BlockState entrance = ModBlocks.HARVEST_ALTAR.get().getStateForPlacement(overworldContext);
        helper.assertTrue(!entrance.getValue(HarvestAltarBlock.ACTIVE)
                        && !entrance.getValue(HarvestAltarBlock.HAS_SIGIL),
                "Placing an Overworld altar must still require a sigil and the harvest ritual");

        var returnContext = new DirectionalPlaceContext(elysium, new BlockPos(0, 100, 0),
                Direction.DOWN, altarItem, Direction.UP);
        BlockState returnStone = ModBlocks.HARVEST_ALTAR.get().getStateForPlacement(returnContext);
        helper.assertTrue(returnStone.getValue(HarvestAltarBlock.ACTIVE)
                        && !returnStone.getValue(HarvestAltarBlock.HAS_SIGIL),
                "An altar replaced inside Elysium must return its owner without creating a sigil");
        helper.succeed();
    }

    private static void buildShrine(GameTestHelper helper) {
        helper.setBlock(CENTER, ModBlocks.HARVEST_ALTAR.get());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int y = 0; y < 3; y++) {
                helper.setBlock(CENTER.relative(direction, 3).above(y), Blocks.STRIPPED_BIRCH_LOG);
            }
        }
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                helper.setBlock(CENTER.offset(x, 0, z), Blocks.HAY_BLOCK);
            }
        }
    }
}
