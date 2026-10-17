package dev.elysium.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import net.minecraft.core.*;
import net.minecraft.world.level.levelgen.structure.*;
import java.util.Optional;

/** Three watch traditions use the same surveyed, small-footprint placement contract. */
public final class WatchfireStructure extends Structure {
    public static final MapCodec<WatchfireStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(settingsCodec(i),Codec.intRange(0,2).fieldOf("watch").forGetter(s->s.watch)).apply(i,WatchfireStructure::new));
    private final int watch;
    public WatchfireStructure(StructureSettings settings,int watch){super(settings);this.watch=watch;}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var terrain=new TerrainSampler(context);int x=context.chunkPos().getMiddleBlockX(),z=context.chunkPos().getMiddleBlockZ();
        int ground=terrain.height(x,z)-1,sea=context.chunkGenerator().getSeaLevel();
        if(ground<sea+1||ground>context.heightAccessor().getMaxBuildHeight()-16)return Optional.empty();
        if(!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(ground),QuartPos.fromBlock(z),context.randomState().sampler())))return Optional.empty();
        int[] heights=new int[17*17];int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        for(int u=-8;u<=8;u++)for(int v=-8;v<=8;v++) {
            int y=terrain.height(x+u,z+v)-1;heights[(u+8)*17+v+8]=y;low=Math.min(low,y);high=Math.max(high,y);
            if(y<sea||Math.abs(u)<=2&&Math.abs(v)<=2&&Math.abs(y-ground)>3)return Optional.empty();
        }
        if(high-low>14)return Optional.empty();
        var piece=new WatchfirePiece(x,z,ground,watch,heights);
        return Optional.of(new GenerationStub(piece.socket(),builder->builder.addPiece(piece)));
    }
    @Override public StructureType<?> type(){return ModStructures.WATCHFIRE.get();}
}
