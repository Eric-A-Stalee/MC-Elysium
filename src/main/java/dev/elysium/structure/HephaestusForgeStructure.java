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
        for(int dx:new int[]{4,12}) for(int dz:new int[]{4,12}) {
            int x=context.chunkPos().getMinBlockX()+dx,z=context.chunkPos().getMinBlockZ()+dz;
            int y=terrain.height(x,z);
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
