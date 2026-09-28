package dev.elysium.block;

import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModAmbience;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class GoldenBirchLeavesBlock extends LeavesBlock {
    public GoldenBirchLeavesBlock(Properties properties) { super(properties); }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // Only exposed undersides shed leaves; no ticking entities, scans, packets,
        // server work, or particles scattered through open field interiors.
        if (level.dimension().equals(ElysiumTravel.DIMENSION) && random.nextInt(100) == 0
                && level.getBlockState(pos.below()).isAir()) {
            level.addParticle(ModAmbience.GOLDEN_LEAF.get(), pos.getX() + random.nextDouble(),
                    pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}
