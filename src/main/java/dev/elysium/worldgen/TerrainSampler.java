package dev.elysium.worldgen;

import dev.elysium.structure.BridgePlanner;
import dev.elysium.structure.LandscapePlanner;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Candidate-local noise queries. No chunk requests, static world state, or retained seed caches. */
public final class TerrainSampler implements LandscapePlanner.Terrain, BridgePlanner.Terrain {
    public record Column(int groundY, OptionalInt waterSurfaceY) {}
    private final Structure.GenerationContext context;
    private final Map<Long, Integer> ground = new HashMap<>();
    private final Map<Long, Column> columns = new HashMap<>();

    public TerrainSampler(Structure.GenerationContext context) { this.context = context; }

    public static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    @Override public int height(int x, int z) {
        return ground.computeIfAbsent(key(x, z), ignored -> context.chunkGenerator().getBaseHeight(x, z,
                Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()));
    }

    @Override public int floorHeight(int x, int z) { return height(x, z); }

    public Column column(int x, int z) {
        return columns.computeIfAbsent(key(x, z), ignored -> {
            int surface = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState());
            boolean water = surface > height(x, z) && context.chunkGenerator()
                    .getBaseColumn(x, z, context.heightAccessor(), context.randomState())
                    .getBlock(surface - 1).getFluidState().is(FluidTags.WATER);
            return new Column(height(x, z) - 1, water ? OptionalInt.of(surface) : OptionalInt.empty());
        });
    }

    @Override public boolean water(int x, int z) { return column(x, z).waterSurfaceY().isPresent(); }
}
