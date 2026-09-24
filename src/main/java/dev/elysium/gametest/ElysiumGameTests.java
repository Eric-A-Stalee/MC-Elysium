package dev.elysium.gametest;

import dev.elysium.ElysiumConfig;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration checks execute against the actual registered mod and loaded datapack. */
@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class ElysiumGameTests {
    @GameTest(template = "portal_test_empty", timeoutTicks = 40)
    public static void shardRollRequiresMatureVanillaWheat(GameTestHelper helper) {
        double previous = ElysiumConfig.WHEAT_SHARD_CHANCE.get();
        try {
            ElysiumConfig.WHEAT_SHARD_CHANCE.set(1.0);
            helper.assertTrue(shards(helper, 7) == 1, "A guaranteed mature-wheat roll should add exactly one shard");
            helper.assertTrue(shards(helper, 0) == 0, "Immature wheat must never grant a shard");
            helper.assertTrue(shards(helper, 6) == 0, "Almost-mature wheat must not qualify");
            ElysiumConfig.WHEAT_SHARD_CHANCE.set(0.0);
            helper.assertTrue(shards(helper, 7) == 0, "A configured zero chance must disable the drop");
        } finally {
            ElysiumConfig.WHEAT_SHARD_CHANCE.set(previous);
        }
        helper.succeed();
    }

    private static int shards(GameTestHelper helper, int age) {
        return Block.getDrops(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age),
                helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), null).stream()
                .filter(stack -> stack.is(ModItems.ELYSIUM_SHARD.get())).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 80)
    public static void paradiseWeatherDoesNotChangeOverworldWeather(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        helper.assertTrue(realm != null, "Weather test requires the real Elysium dimension");
        var overworld = helper.getLevel().getServer().overworld();
        float previous = overworld.getRainLevel(1.0F);
        overworld.setRainLevel(1.0F);
        realm.setRainLevel(1.0F);
        realm.setThunderLevel(1.0F);
        helper.runAfterDelay(3, () -> {
            try {
                helper.assertTrue(realm.getRainLevel(1.0F) == 0.0F && realm.getThunderLevel(1.0F) == 0.0F,
                        "Elysium must clear its own rain and thunder");
                helper.assertTrue(overworld.getRainLevel(1.0F) > 0.8F,
                        "Keeping paradise clear must not clear the Overworld's weather");
            } finally {
                overworld.setRainLevel(previous);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 600)
    public static void paradiseLoadsWithSolidDepthsAndFixedSun(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        helper.assertTrue(realm != null, "Elysium must load as an independent dimension");
        var type = realm.dimensionType();
        helper.assertTrue(type.hasFixedTime(), "The paradise sun must remain fixed");
        helper.assertTrue(type.timeOfDay(0) == type.timeOfDay(12000), "The Overworld clock must not move Elysium's sun");
        helper.assertTrue(type.hasSkyLight() && type.bedWorks(), "The realm needs daylight and usable beds");
        var biomes = realm.registryAccess().registryOrThrow(Registries.BIOME);
        for (String id : new String[]{"golden_fields", "golden_birch_woods", "elysian_highlands"}) {
            var biome = biomes.get(ResourceLocation.fromNamespaceAndPath("elysium", id));
            helper.assertTrue(biome != null, "Missing biome: " + id);
            helper.assertTrue(!biome.hasPrecipitation(), "Paradise biome should not rain: " + id);
            helper.assertTrue(biome.getMobSettings().getMobs(MobCategory.MONSTER).isEmpty(),
                    "Paradise should not register ordinary hostile spawns: " + id);
        }
        // Force real chunk generation, including feature sorting and custom block placement.
        for (BlockPos pos : new BlockPos[]{new BlockPos(0, 32, 0), new BlockPos(256, 32, 256), new BlockPos(-256, 32, -256)}) {
            helper.assertTrue(realm.getBlockState(pos).is(Blocks.STONE),
                    "The initial buried world should be solid stone at " + pos);
        }
        helper.succeed();
    }

    @GameTest(template = "portal_test_empty", timeoutTicks = 600)
    public static void portalPiglinsCannotSpawnInParadise(GameTestHelper helper) {
        var realm = helper.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        helper.assertTrue(realm != null, "Spawn test requires the real Elysium dimension");
        BlockPos surface = realm.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.ZERO);
        // Nether portal random ticks spawn zombified piglins with the STRUCTURE spawn type.
        helper.assertTrue(EntityType.ZOMBIFIED_PIGLIN.spawn(realm, surface, MobSpawnType.STRUCTURE) == null,
                "A lit nether portal must not bring zombified piglins into paradise");
        var summoned = EntityType.ZOMBIFIED_PIGLIN.spawn(realm, surface, MobSpawnType.COMMAND);
        helper.assertTrue(summoned != null, "Commands and spawn eggs should still work inside Elysium");
        summoned.discard();
        var overworldPiglin = EntityType.ZOMBIFIED_PIGLIN.spawn(helper.getLevel(),
                helper.absolutePos(new BlockPos(2, 1, 2)), MobSpawnType.STRUCTURE);
        helper.assertTrue(overworldPiglin != null, "Overworld nether portals must keep their vanilla behavior");
        overworldPiglin.discard();
        helper.succeed();
    }
}
