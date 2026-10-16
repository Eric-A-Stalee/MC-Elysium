package dev.elysium.structure;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class TartarusChainStructure extends Structure {
    public static final int ANCHOR_X_OFFSET=-40, ANCHOR_Z_OFFSET=-80;
    public static final MapCodec<TartarusChainStructure> CODEC = simpleCodec(TartarusChainStructure::new);
    public TartarusChainStructure(StructureSettings settings) { super(settings); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var terrain = new TerrainSampler(context);
        int floor = TartarusDepthsPlan.ANCHOR_FLOOR;
        boolean axis = context.random().nextBoolean();
        for (int dx = 1; dx <= 13; dx += 4) for (int dz = 1; dz <= 13; dz += 4) {
            int x = context.chunkPos().getMinBlockX() + ANCHOR_X_OFFSET + dx;
            int z = context.chunkPos().getMinBlockZ() + ANCHOR_Z_OFFSET + dz;
            if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(128), QuartPos.fromBlock(z), context.randomState().sampler()))) continue;
            var plan = TartarusChainPlan.find(terrain::height, x, z, floor, axis);
            if (plan.isEmpty()) continue;
            int top = plan.get().top();
            if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(top), QuartPos.fromBlock(z), context.randomState().sampler()))) continue;
            // Keep the entire chamber deeply covered even beside a river valley.
            boolean covered = true;
            int r=plan.get().chamberRadius();
            for(int cx=-r;cx<=r;cx+=6)for(int cz=-r;cz<=r;cz+=6)
                if(cx*cx+cz*cz<=r*r)covered &= terrain.height(x+cx,z+cz)>plan.get().roof(cx,cz)+20;
            if (!covered) continue;
            long seed=context.random().nextLong();
            var chainPiece=new TartarusChainPiece(plan.get());
            var depthsPiece=new TartarusDepthsPiece(TartarusDepthsPlan.belowAnchor(x,z,seed));
            if(!withinReferenceReach(context.chunkPos(),chainPiece.getBoundingBox())
                    ||!withinReferenceReach(context.chunkPos(),depthsPiece.getBoundingBox()))continue;
            return Optional.of(new GenerationStub(new BlockPos(x, top, z),
                    builder -> {
                        builder.addPiece(chainPiece);
                        builder.addPiece(depthsPiece);
                    }));
        }
        return Optional.empty();
    }
    public static boolean withinReferenceReach(ChunkPos source,BoundingBox bounds) {
        return (bounds.minX()>>4)>=source.x-8 && (bounds.maxX()>>4)<=source.x+8
                && (bounds.minZ()>>4)>=source.z-8 && (bounds.maxZ()>>4)<=source.z+8;
    }
    @Override public StructureType<?> type() { return ModStructures.TARTARUS_CHAIN.get(); }
}
