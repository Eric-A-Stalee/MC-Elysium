package dev.elysium.worldgen;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Reserve crown room around authored paving and roofs without loading structure starts or chunks. */
public final class SettlementClearanceFilter extends PlacementFilter {
    public static final MapCodec<SettlementClearanceFilter> CODEC=MapCodec.unit(SettlementClearanceFilter::new);
    public static boolean built(BlockState state) {
        return state.is(Blocks.STONE_BRICKS) || state.is(Blocks.POLISHED_ANDESITE) || state.is(Blocks.SMOOTH_SANDSTONE)
                || state.is(BlockTags.PLANKS) || state.is(BlockTags.STAIRS) || state.is(BlockTags.WALLS) || state.is(BlockTags.SLABS);
    }
    @Override protected boolean shouldPlace(PlacementContext context,RandomSource random,BlockPos pos) {
        for(int dx=-6;dx<=6;dx+=2)for(int dz=-6;dz<=6;dz+=2) {
            if(dx*dx+dz*dz>45)continue;
            int x=pos.getX()+dx,z=pos.getZ()+dz;
            if(!context.getLevel().hasChunk(x>>4,z>>4))return false;
            int y=context.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z)-1;
            if(built(context.getBlockState(new BlockPos(x,y,z))))return false;
        }
        return true;
    }
    @Override public PlacementModifierType<?> type(){return ModWorldgen.SETTLEMENT_CLEARANCE.get();}
}
