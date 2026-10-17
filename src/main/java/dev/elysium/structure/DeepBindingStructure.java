package dev.elysium.structure;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.*;
import java.util.Optional;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.structure.*;

/** One placement contract, three different prisons. The approach follows the existing cavern floor. */
public final class DeepBindingStructure extends Structure {
    public static final MapCodec<DeepBindingStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(settingsCodec(i),Codec.intRange(0,2).fieldOf("binding").forGetter(s->s.kind)).apply(i,DeepBindingStructure::new));
    private final int kind;
    public DeepBindingStructure(StructureSettings s,int kind){super(s);this.kind=kind;}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        if(!(c.chunkGenerator() instanceof StratifiedChunkGenerator g))return Optional.empty();
        var t=g.terrain(c.randomState());int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ(),base=t.floor(x,z),low=base,high=base;
        if(kind!=1&&base<TartarusTerrain.WATERLINE+3)return Optional.empty();
        for(int u=-28;u<=28;u+=7)for(int v=-28;v<=28;v+=7){int y=t.floor(x+u,z+v);low=Math.min(low,y);high=Math.max(high,y);}
        if(high-low>32)return Optional.empty();base=Math.max(base,kind==1?TartarusTerrain.WATERLINE+2:base);
        if(!c.validBiome().test(c.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(base+8),QuartPos.fromBlock(z),c.randomState().sampler())))return Optional.empty();
        int[] route=new int[64];route[0]=1;
        for(int i=1;i<64;i++){int wanted=Math.max(t.floor(x,z+27+i),kind==1?TartarusTerrain.WATERLINE:Integer.MIN_VALUE)-base;route[i]=Math.max(route[i-1]-1,Math.min(route[i-1]+1,wanted));low=Math.min(low,base+route[i]);}
        if(Math.abs(base+route[63]-t.floor(x,z+90))>2)return Optional.empty();
        var p=new DeepBindingPiece(x,base,z,low-9,kind,c.seed(),route);return Optional.of(new GenerationStub(p.core(),b->b.addPiece(p)));
    }
    @Override public StructureType<?> type(){return ModStructures.DEEP_BINDING.get();}
}
