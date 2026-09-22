package dev.elysium.structure;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModStructures;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Finds actual short river crossings using noise columns; never requests or loads neighbouring chunks. */
public final class ElysianBridgeStructure extends Structure {
    public static final MapCodec<ElysianBridgeStructure> CODEC = simpleCodec(ElysianBridgeStructure::new);
    private static final int[][] PROBES = {{8, 8}, {2, 2}, {13, 2}, {2, 13}, {13, 13}};

    public ElysianBridgeStructure(StructureSettings settings) { super(settings); }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        // Cache belongs to this one candidate only: no retained worlds, seeds, or unbounded global maps.
        Map<Long, Integer> heights = new HashMap<>();
        BridgePlanner.Terrain terrain = (x, z) -> heights.computeIfAbsent(columnKey(x, z), key ->
                context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                        context.heightAccessor(), context.randomState()));
        boolean firstAxis = context.random().nextBoolean();
        int firstProbe = context.random().nextInt(PROBES.length);
        for (int index = 0; index < PROBES.length; index++) {
            int[] probe = PROBES[(index + firstProbe) % PROBES.length];
            int x = context.chunkPos().getMinBlockX() + probe[0];
            int z = context.chunkPos().getMinBlockZ() + probe[1];
            if (terrain.floorHeight(x, z) >= seaLevel) continue;
            // Height alone could also describe a dry depression in an unfamiliar generator.
            if (!context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState())
                    .getBlock(seaLevel - 1).is(Blocks.WATER)) continue;
            for (boolean eastWest : new boolean[]{firstAxis, !firstAxis}) {
                Optional<BridgePlanner.Span> plan = BridgePlanner.find(terrain, x, z, eastWest, seaLevel);
                if (plan.isEmpty()) continue;
                BridgePlanner.Span span = plan.get();
                if (!validEndBiome(context, span, 0) || !validEndBiome(context, span, span.length() - 1)) continue;
                return Optional.of(new GenerationStub(new BlockPos(x, span.deckHeight(), z),
                        builder -> builder.addPiece(new ElysianBridgePiece(span))));
            }
        }
        return Optional.empty();
    }

    private static boolean validEndBiome(GenerationContext context, BridgePlanner.Span span, int along) {
        return context.validBiome().test(context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(span.x(along, 0)), QuartPos.fromBlock(span.walkingHeight(along)),
                QuartPos.fromBlock(span.z(along, 0)), context.randomState().sampler()));
    }

    private static long columnKey(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    @Override
    public StructureType<?> type() { return ModStructures.ELYSIAN_BRIDGE.get(); }
}
