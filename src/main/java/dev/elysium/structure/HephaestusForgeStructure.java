package dev.elysium.structure;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class HephaestusForgeStructure extends Structure {
    public static final MapCodec<HephaestusForgeStructure> CODEC=simpleCodec(HephaestusForgeStructure::new);
    public HephaestusForgeStructure(StructureSettings settings) { super(settings); }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var terrain=new TerrainSampler(context);
        // A doorway needs a narrow shoulder, not an arbitrary point near the chunk
        // centre. Search its immediate neighbourhood; even the far end of a
        // rotated vault remains within vanilla's eight-chunk reference radius.
        for(int dx=-8;dx<=20;dx+=4) for(int dz=-8;dz<=20;dz+=4) {
            int x=context.chunkPos().getMinBlockX()+dx,z=context.chunkPos().getMinBlockZ()+dz;
            int y=terrain.height(x,z);
            if(y>context.heightAccessor().getMaxBuildHeight()-12)continue;
            if(!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),
                    QuartPos.fromBlock(z),context.randomState().sampler()))) continue;
            var plan=ForgePlan.find(terrain::height,x,z,context.chunkGenerator().getSeaLevel(),context.heightAccessor().getMinBuildHeight());
            if(plan.isPresent()) return Optional.of(new GenerationStub(new BlockPos(x,plan.get().entranceFloor(),z),
                    builder->builder.addPiece(new HephaestusForgePiece(plan.get()))));
        }
        return Optional.empty();
    }
    @Override public StructureType<?> type() { return ModStructures.HEPHAESTUS_FORGE.get(); }
}
