package dev.elysium.worldgen;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Riparian birches require exposed surface water within six blocks, not just low terrain. */
public final class NearWaterFilter extends PlacementFilter {
    public static final MapCodec<NearWaterFilter> CODEC = MapCodec.unit(NearWaterFilter::new);
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        int sea = context.generator().getSeaLevel();
        if (pos.getY() < sea || pos.getY() > sea + 4) return false;
        for (int distance : new int[]{3, 6}) {
            for (int[] direction : DIRECTIONS) {
                // Keep diagonal probes within the same six-block disk.
                if (distance == 6 && direction[0] != 0 && direction[1] != 0) continue;
                BlockPos water = new BlockPos(pos.getX() + direction[0] * distance, sea - 1,
                        pos.getZ() + direction[1] * distance);
                // Decoration has neighbouring chunks available; never ask it to load one.
                if (!context.getLevel().hasChunk(water.getX() >> 4, water.getZ() >> 4)) continue;
                if (context.getBlockState(water).is(Blocks.WATER) && context.getBlockState(water.above()).isAir()) return true;
            }
        }
        return false;
    }

    @Override public PlacementModifierType<?> type() { return ModWorldgen.NEAR_WATER.get(); }
}
