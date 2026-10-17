package dev.elysium.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Ordinary Elysium above, a seeded underworld below. No runtime excavation or foreign dimension hooks. */
public final class StratifiedChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<StratifiedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(StratifiedChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(StratifiedChunkGenerator::generatorSettings)
    ).apply(instance, StratifiedChunkGenerator::new));
    private record TerrainState(RandomState random, TartarusTerrain terrain) {}
    private volatile TerrainState cached;

    public StratifiedChunkGenerator(BiomeSource biomes, Holder<NoiseGeneratorSettings> settings) { super(biomes, settings); }
    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
    public TartarusTerrain terrain(RandomState random) {
        var value = cached;
        if(value == null || value.random != random) {
            synchronized(this) {
                value=cached;
                if(value == null || value.random != random) cached=value=new TerrainState(random,new TartarusTerrain(random));
            }
        }
        return value.terrain;
    }

    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
                                                                StructureManager structures, ChunkAccess chunk) {
        return super.fillFromNoise(blender,random,structures,chunk).thenApply(filled -> {
            var terrain=terrain(random);
            var pos=new BlockPos.MutableBlockPos();
            int min=Math.max(filled.getMinBuildHeight(),TartarusTerrain.MIN_Y);
            int max=Math.min(filled.getMaxBuildHeight()-1,TartarusTerrain.TOP);
            for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
                int wx=filled.getPos().getMinBlockX()+x,wz=filled.getPos().getMinBlockZ()+z;
                var column=terrain.column(wx,wz);
                for(int y=min;y<=max;y++)filled.setBlockState(pos.set(wx,y,wz),column.at(y),false);
            }
            return filled;
        });
    }

    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor height,RandomState random) {
        var column=super.getBaseColumn(x,z,height,random);
        var terrain=terrain(random).column(x,z);
        for(int y=Math.max(height.getMinBuildHeight(),TartarusTerrain.MIN_Y);y<=Math.min(height.getMaxBuildHeight()-1,TartarusTerrain.TOP);y++)
            column.setBlock(y,terrain.at(y));
        return column;
    }
}
