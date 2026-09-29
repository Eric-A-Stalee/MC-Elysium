package dev.elysium.structure;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class TartarusChainStructure extends Structure {
    public static final MapCodec<TartarusChainStructure> CODEC = simpleCodec(TartarusChainStructure::new);
    public TartarusChainStructure(StructureSettings settings) { super(settings); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var terrain = new TerrainSampler(context);
        int floor = context.heightAccessor().getMinBuildHeight() + 16;
        boolean axis = context.random().nextBoolean();
        for (int dx = 1; dx <= 13; dx += 4) for (int dz = 1; dz <= 13; dz += 4) {
            int x = context.chunkPos().getMinBlockX() + dx, z = context.chunkPos().getMinBlockZ() + dz;
            if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(128), QuartPos.fromBlock(z), context.randomState().sampler()))) continue;
            var plan = TartarusChainPlan.find(terrain::height, x, z, floor, axis);
            if (plan.isEmpty()) continue;
            int top = plan.get().top();
            if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(top), QuartPos.fromBlock(z), context.randomState().sampler()))) continue;
            // Keep the entire chamber deeply covered even beside a river valley.
            boolean covered = true;
            for (int cx : new int[]{-12, 0, 12}) for (int cz : new int[]{-12, 0, 12})
                covered &= terrain.height(x + cx, z + cz) > floor + 40;
            if (!covered) continue;
            return Optional.of(new GenerationStub(new BlockPos(x, top, z),
                    builder -> builder.addPiece(new TartarusChainPiece(plan.get()))));
        }
        return Optional.empty();
    }
    @Override public StructureType<?> type() { return ModStructures.TARTARUS_CHAIN.get(); }
}
