package dev.elysium.structure;

import com.mojang.serialization.MapCodec;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.StratifiedChunkGenerator;
import dev.elysium.worldgen.TartarusTerrain;
import java.util.Optional;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.structure.*;

/** A prison grows from a dry deep outcrop, independently of the surface biome. */
public final class PrometheusPrisonStructure extends Structure {
    public static final MapCodec<PrometheusPrisonStructure> CODEC=simpleCodec(PrometheusPrisonStructure::new);
    public PrometheusPrisonStructure(StructureSettings settings){super(settings);}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
        if(!(c.chunkGenerator() instanceof StratifiedChunkGenerator generator))return Optional.empty();
        var terrain=generator.terrain(c.randomState());int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ();
        int base=terrain.floor(x,z),low=base,high=base;
        if(base<TartarusTerrain.WATERLINE+12)return Optional.empty();
        for(int u=-44;u<=44;u+=11)for(int v=-33;v<=99;v+=11){int y=terrain.floor(x+u,z+v);low=Math.min(low,y);high=Math.max(high,y);}
        if(high-low>55||terrain.roof(x,z)-base<100)return Optional.empty();
        if(!c.validBiome().test(c.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(base+10),QuartPos.fromBlock(z),c.randomState().sampler())))return Optional.empty();
        var approach=new int[64];approach[0]=6;
        for(int i=1;i<approach.length;i++){int wanted=terrain.floor(x+19,z+33+i)-high;approach[i]=Math.max(approach[i-1]-1,Math.min(approach[i-1]+1,wanted));}
        if(Math.abs(high+approach[63]-terrain.floor(x+19,z+96))>1)return Optional.empty();
        var piece=new PrometheusPrisonPiece(x,high,z,low-6,c.seed(),approach);
        return Optional.of(new GenerationStub(piece.voice(),builder->builder.addPiece(piece)));
    }
    @Override public StructureType<?> type(){return ModStructures.PROMETHEUS_PRISON.get();}
}
