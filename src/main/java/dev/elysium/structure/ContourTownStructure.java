package dev.elysium.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Rare river settlements require inhabited slopes and nearby outcrops, not just a distant backdrop. */
public final class ContourTownStructure extends Structure {
    private static final Codec<MountainBuildingPlan.Style> STYLE=Codec.STRING.xmap(s->MountainBuildingPlan.Style.valueOf(s.toUpperCase(Locale.ROOT)),
            s->s.name().toLowerCase(Locale.ROOT));
    public static final MapCodec<ContourTownStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            settingsCodec(i),ResourceLocation.CODEC.fieldOf("plaza").forGetter(s->s.plaza),
            STYLE.listOf(1,4).fieldOf("styles").forGetter(s->s.styles),
            Codec.intRange(10,24).fieldOf("minimum_houses").forGetter(s->s.minimum),
            Codec.intRange(10,24).fieldOf("maximum_houses").forGetter(s->s.maximum)
    ).apply(i,ContourTownStructure::new));
    private final ResourceLocation plaza;
    private final List<MountainBuildingPlan.Style> styles;
    private final int minimum,maximum;
    public ContourTownStructure(StructureSettings settings,ResourceLocation plaza,List<MountainBuildingPlan.Style> styles,int minimum,int maximum) {
        super(settings);this.plaza=plaza;this.styles=List.copyOf(styles);this.minimum=minimum;this.maximum=maximum;
        if(minimum>maximum || styles.stream().anyMatch(s->s==MountainBuildingPlan.Style.GREAT_HALL || s==MountainBuildingPlan.Style.WATCH_LODGE))
            throw new IllegalArgumentException("Invalid contour town families or size");
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) { return surveyGenerationPoint(context,ignored->{}); }
    public Optional<GenerationStub> surveyGenerationPoint(GenerationContext context,Consumer<String> survey) {
        var terrain=new TerrainSampler(context);int sea=context.chunkGenerator().getSeaLevel();long seed=context.random().nextLong();
        for(int dx:new int[]{4,12})for(int dz:new int[]{4,12}) {
            int x=context.chunkPos().getMinBlockX()+dx,z=context.chunkPos().getMinBlockZ()+dz;
            if(terrain.height(x,z)>=sea || !validBiome(context,x,sea,z) || !terrain.water(x,z))continue;
            survey.accept("mountain_water");
            int nearby=0;
            for(int u:new int[]{-88,-44,0,44,88})for(int v:new int[]{-88,-44,0,44,88})
                nearby=Math.max(nearby,terrain.height(x+u,z+v));
            if(nearby<sea+48)continue;
            survey.accept("nearby_peaks");
            for(boolean axis:new boolean[]{true,false}) {
                var bridge=BridgePlanner.find(terrain,x,z,axis,sea);if(bridge.isEmpty())continue;
                survey.accept("crossing");
                var result=ContourTownPlanner.plan(terrain,bridge.get(),sea,seed,styles,minimum,maximum,survey);
                if(result.isEmpty())continue;var plan=result.get();
                if(plan.buildings().stream().filter(b->validBiome(context,b.x(),b.main().floor(),b.z())).count()*4<plan.buildings().size()*3L) {
                    survey.accept("biome_extent");continue;
                }
                var size=context.structureTemplateManager().getOrCreate(plaza).getSize();
                if(size.getX()!=11 || size.getZ()!=11)continue;
                return Optional.of(new GenerationStub(new BlockPos(x,sea,z),builder->{
                    builder.addPiece(new TownTerrainPiece(plan.streets()));
                    var square=plan.square();
                    builder.addPiece(new LandscapePiece(context.structureTemplateManager(),plaza,
                            new BlockPos(square.x()-5,square.ground()-1,square.z()-5),MountainTownStructure.fromSouth(square.face()),5,0));
                    for(var b:plan.buildings())builder.addPiece(new MountainBuildingPiece(b));
                    builder.addPiece(new ElysianBridgePiece(plan.bridge(),true));
                    if(!plan.landscape().trees().isEmpty() || !plan.landscape().details().isEmpty())builder.addPiece(new TownLandscapePiece(plan.landscape()));
                }));
            }
        }
        return Optional.empty();
    }
    private static boolean validBiome(GenerationContext context,int x,int y,int z) {
        return context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),context.randomState().sampler()));
    }
    @Override public StructureType<?> type() {return ModStructures.CONTOUR_TOWN.get();}
}
