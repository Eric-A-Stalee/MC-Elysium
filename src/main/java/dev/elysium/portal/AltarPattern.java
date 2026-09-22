package dev.elysium.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.levelgen.Heightmap;

/** The little harvest sanctuary is checked only when someone uses its altar. */
public final class AltarPattern {
    private AltarPattern() {}

    public enum Problem {
        NONE(""),
        WRONG_DIMENSION("message.elysium.altar.overworld_only"),
        MISSING_PILLARS("message.elysium.altar.pillars"),
        MISSING_HAY("message.elysium.altar.hay"),
        BLOCKED_SKY("message.elysium.altar.sky"),
        WRONG_TIME("message.elysium.altar.sunset");

        private final String translationKey;

        Problem(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    /** All offsets are relative to the altar block, not the ground beneath it. */
    public static Problem validateStructure(Level level, BlockPos altar) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return Problem.WRONG_DIMENSION;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int height = 0; height < 3; height++) {
                var pillar = level.getBlockState(altar.relative(direction, 3).above(height));
                if (!pillar.is(Blocks.STRIPPED_BIRCH_LOG)
                        || pillar.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
                    return Problem.MISSING_PILLARS;
                }
            }
        }
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-2, 2}) {
                if (!level.getBlockState(altar.offset(x, 0, z)).is(Blocks.HAY_BLOCK)) {
                    return Problem.MISSING_HAY;
                }
            }
        }
        // Heightmaps update with the block change; skylight propagation can lag behind a new roof.
        // WORLD_SURFACE counts glass and leaves too, so those roofs also block the open-air ritual.
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, altar.getX(), altar.getZ()) <= altar.getY() + 1
                ? Problem.NONE : Problem.BLOCKED_SKY;
    }

    public static Problem validateActivation(Level level, BlockPos altar) {
        Problem structure = validateStructure(level, altar);
        if (structure != Problem.NONE) {
            return structure;
        }
        return isSunset(level.getDayTime()) ? Problem.NONE : Problem.WRONG_TIME;
    }

    public static boolean isSunset(long dayTime) {
        long time = Math.floorMod(dayTime, 24_000L);
        return time >= 11_000L && time <= 13_000L;
    }
}
