package dev.elysium.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/** Offset foliage lobes around each trunk/branch attachment, with a narrow connecting core. */
public final class BirchCrownPlacer extends FoliagePlacer {
    public static final MapCodec<BirchCrownPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).and(Codec.intRange(3, 8).fieldOf("height").forGetter(p -> p.height))
                    .apply(instance, BirchCrownPlacer::new));
    private final int height;
    public BirchCrownPlacer(IntProvider radius, IntProvider offset, int height) {
        super(radius, offset); this.height = height;
    }
    @Override protected FoliagePlacerType<?> type() { return ModWorldgen.BIRCH_CROWN.get(); }
    @Override public int foliageHeight(RandomSource random, int trunkHeight, TreeConfiguration config) {
        return Math.min(trunkHeight - 2, height + random.nextInt(2));
    }
    @Override protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random,
            TreeConfiguration config, int trunkHeight, FoliageAttachment attachment, int crownHeight, int radius, int offset) {
        double angle=random.nextDouble()*Math.PI*2;
        double[][] lobes=new double[4][6];
        lobes[0]=new double[]{0, -1.0, 0, 1.65, 1.75, 1.65};
        for(int i=1;i<4;i++) {
            double direction=angle+i*2.15;
            lobes[i]=new double[]{Math.cos(direction)*(radius*0.60),-1.5-i*(crownHeight-2.0)/3,
                    Math.sin(direction)*(radius*0.60),1.5+random.nextDouble()*0.45,1.65+random.nextDouble()*0.45,1.5+random.nextDouble()*0.45};
        }
        int extent=radius+1;
        for(int y=-crownHeight-1;y<=1;y++)for(int x=-extent;x<=extent;x++)for(int z=-extent;z<=extent;z++) {
            double best=Double.MAX_VALUE;
            for(double[] l:lobes) {
                double a=(x-l[0])/l[3],b=(y-l[1])/l[4],c=(z-l[2])/l[5];
                best=Math.min(best,a*a+b*b+c*c);
            }
            // Keep a connected inner scaffold; only the outer lobe edges vary.
            boolean core=x*x+z*z<=1 && y<=0 && y>=-crownHeight+1;
            if(core || (best<=1.0 && !(best>0.86 && random.nextInt(7)==0)))
                tryPlaceLeaf(level,setter,random,config,attachment.pos().offset(x,offset+y,z));
        }
    }
    @Override protected boolean shouldSkipLocation(RandomSource random, int x, int y, int z, int radius, boolean doubleTrunk) {
        int distance = x * x + z * z;
        return distance > radius * radius + 1 || (distance >= radius * radius && random.nextInt(5) == 0);
    }
}
