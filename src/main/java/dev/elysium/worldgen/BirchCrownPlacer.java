package dev.elysium.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/** Tapered, offset oval crowns shared by straight and branching birches. */
public final class BirchCrownPlacer extends FoliagePlacer {
    public static final MapCodec<BirchCrownPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).and(Codec.intRange(3, 8).fieldOf("height").forGetter(p -> p.height))
                    .apply(instance, BirchCrownPlacer::new));
    private final int height;
    public BirchCrownPlacer(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset); this.height = height;
    }
    @Override protected FoliagePlacerType<?> type() { return ModWorldgen.BIRCH_CROWN.get(); }
    @Override public int foliageHeight(RandomSource random, int trunkHeight, TreeConfiguration config) {
        return Math.min(trunkHeight - 2, height + random.nextInt(2));
    }
    @Override protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random,
            TreeConfiguration config, int trunkHeight, FoliageAttachment attachment, int crownHeight, int radius, int offset) {
        int leanX = random.nextInt(3) - 1, leanZ = random.nextInt(3) - 1;
        for (int layer = 0; layer <= crownHeight; layer++) {
            double t = (double) layer / crownHeight;
            int width = Math.max(1, (int) Math.round(radius * Math.sin(Math.PI * (0.08 + 0.84 * t))));
            BlockPos center = attachment.pos().offset(layer < crownHeight / 2 ? leanX : 0,
                    offset - layer, layer < crownHeight / 2 ? leanZ : 0);
            placeLeavesRow(level, setter, random, config, center, width, 0, false);
        }
    }
    @Override protected boolean shouldSkipLocation(RandomSource random, int x, int y, int z, int radius, boolean doubleTrunk) {
        int distance = x * x + z * z;
        return distance > radius * radius + 1 || (distance >= radius * radius && random.nextInt(5) == 0);
    }
}
