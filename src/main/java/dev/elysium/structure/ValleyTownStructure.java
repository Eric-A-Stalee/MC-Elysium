package dev.elysium.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** A rare town spanning a real river in a mountain valley, with larger reusable house families. */
public final class ValleyTownStructure extends Structure {
    public static final MapCodec<ValleyTownStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            settingsCodec(i),ResourceLocation.CODEC.fieldOf("plaza").forGetter(s->s.plaza),
            MountainTownStructure.Module.CODEC.fieldOf("hall").forGetter(s->s.hall),
            MountainTownStructure.Module.CODEC.fieldOf("tower").forGetter(s->s.tower),
            MountainTownStructure.Module.CODEC.listOf(1,8).fieldOf("houses").forGetter(s->s.houses),
            Codec.intRange(10,26).fieldOf("minimum_houses").forGetter(s->s.minimum),
            Codec.intRange(10,26).fieldOf("maximum_houses").forGetter(s->s.maximum)
    ).apply(i,ValleyTownStructure::new));
    private final ResourceLocation plaza;
    private final MountainTownStructure.Module hall,tower;
    private final List<MountainTownStructure.Module> houses;
    private final int minimum,maximum;
    public ValleyTownStructure(StructureSettings settings,ResourceLocation plaza,MountainTownStructure.Module hall,
            MountainTownStructure.Module tower,List<MountainTownStructure.Module> houses,int minimum,int maximum) {
        super(settings);
        if(minimum>maximum)throw new IllegalArgumentException("Reversed valley town size");
        this.plaza=plaza;this.hall=hall;this.tower=tower;this.houses=List.copyOf(houses);this.minimum=minimum;this.maximum=maximum;
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var terrain=new TerrainSampler(context);int sea=context.chunkGenerator().getSeaLevel();
        for(int dx:new int[]{4,12})for(int dz:new int[]{4,12}) {
            int x=context.chunkPos().getMinBlockX()+dx,z=context.chunkPos().getMinBlockZ()+dz;
            if(terrain.height(x,z)>=sea || !validBiome(context,x,sea,z) || !terrain.water(x,z))continue;
            for(boolean axis:new boolean[]{true,false}) {
                // Both sides must rise into real mountains. Lowland river villages do not qualify.
                int lx=x+(axis?-104:0),lz=z+(axis?0:-104),rx=x+(axis?104:0),rz=z+(axis?0:104);
                if(terrain.height(lx,lz)<sea+32 || terrain.height(rx,rz)<sea+32)continue;
                var bridge=BridgePlanner.find(terrain,x,z,axis,sea);
                if(bridge.isEmpty())continue;
                var result=ValleyTownPlanner.plan(terrain,bridge.get(),sea,hall.shape(),tower.shape(),
                        houses.stream().map(MountainTownStructure.Module::shape).toList(),minimum,maximum);
                if(result.isEmpty())continue;
                var plan=result.get();
                if(plan.lots().stream().filter(l->validBiome(context,l.x(),l.ground(),l.z())).count()*4<plan.lots().size()*3L)continue;
                boolean valid=true;
                for(var lot:plan.lots()) {
                    var size=context.structureTemplateManager().getOrCreate(template(lot.module())).getSize();
                    valid &= size.getX()==lot.radius()*2+1 && size.getZ()==lot.radius()*2+1;
                }
                if(!valid)continue;
                return Optional.of(new GenerationStub(new BlockPos(x,sea,z),builder->{
                    builder.addPiece(new TownTerrainPiece(plan.columns()));
                    for(var lot:plan.lots())builder.addPiece(new LandscapePiece(context.structureTemplateManager(),template(lot.module()),
                            new BlockPos(lot.x()-lot.radius(),lot.ground()-1,lot.z()-lot.radius()),
                            MountainTownStructure.fromSouth(lot.face()),lot.radius(),0));
                    builder.addPiece(new ElysianBridgePiece(plan.crossing(),true));
                }));
            }
        }
        return Optional.empty();
    }
    private ResourceLocation template(int index) {return index<0?plaza:index==0?hall.template():index==1?tower.template():houses.get(index-2).template();}
    private static boolean validBiome(GenerationContext context,int x,int y,int z) {
        return context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),context.randomState().sampler()));
    }
    @Override public StructureType<?> type() {return ModStructures.VALLEY_TOWN.get();}
}
