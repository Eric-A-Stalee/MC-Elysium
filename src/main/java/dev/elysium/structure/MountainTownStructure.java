package dev.elysium.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Rare mountain settlements composed from data-defined rigid modules and terrain-fitted roads. */
public final class MountainTownStructure extends Structure {
    public record Module(ResourceLocation template,int radius,int relief) {
        public static final Codec<Module> CODEC=RecordCodecBuilder.create(i->i.group(
                ResourceLocation.CODEC.fieldOf("template").forGetter(Module::template),
                Codec.intRange(3,16).fieldOf("radius").forGetter(Module::radius),
                Codec.intRange(0,8).fieldOf("max_relief").forGetter(Module::relief)
        ).apply(i,Module::new));
        TerracePlanner.Module shape() { return new TerracePlanner.Module(radius,relief); }
    }
    public static final MapCodec<MountainTownStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            settingsCodec(i),
            ResourceLocation.CODEC.fieldOf("plaza").forGetter(s->s.plaza),
            Module.CODEC.fieldOf("hall").forGetter(s->s.hall),
            Module.CODEC.listOf(1,8).fieldOf("cottages").forGetter(s->s.cottages),
            Codec.intRange(1,6).fieldOf("minimum_cottages").forGetter(s->s.minimum),
            Codec.intRange(1,6).fieldOf("maximum_cottages").forGetter(s->s.maximum),
            Codec.intRange(8,96).fieldOf("min_height_above_sea").forGetter(s->s.minHeight)
    ).apply(i,MountainTownStructure::new));
    private final ResourceLocation plaza;
    private final Module hall;
    private final List<Module> cottages;
    private final int minimum,maximum,minHeight;
    public MountainTownStructure(StructureSettings settings,ResourceLocation plaza,Module hall,List<Module> cottages,
                                 int minimum,int maximum,int minHeight) {
        super(settings);
        if(minimum>maximum) throw new IllegalArgumentException("Town size range is reversed");
        this.plaza=plaza;this.hall=hall;this.cottages=List.copyOf(cottages);
        this.minimum=minimum;this.maximum=maximum;this.minHeight=minHeight;
    }
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x=context.chunkPos().getMiddleBlockX(),z=context.chunkPos().getMiddleBlockZ();
        int sea=context.chunkGenerator().getSeaLevel();var terrain=new TerrainSampler(context);
        int height=terrain.height(x,z);
        if(height<sea+minHeight || !validBiome(context,x,height,z)) return Optional.empty();
        // Seek a sheltered shoulder with higher land on at least two sides.
        int shelter=0;
        for(Direction d:Direction.Plane.HORIZONTAL) {
            if(terrain.height(x+d.getStepX()*64,z+d.getStepZ()*64)>=height+8) shelter++;
        }
        if(shelter<2) return Optional.empty();
        var plan=TerracePlanner.plan(terrain,x,z,sea,hall.shape(),cottages.stream().map(Module::shape).toList(),minimum,maximum);
        if(plan.isEmpty()) return Optional.empty();
        for(var lot:plan.get().lots()) {
            if(!validBiome(context,lot.x(),lot.ground()+1,lot.z())) return Optional.empty();
            ResourceLocation id=lot.module()<0 ? plaza : module(lot.module()).template();
            var size=context.structureTemplateManager().getOrCreate(id).getSize();
            if(size.getX()!=lot.radius()*2+1 || size.getZ()!=lot.radius()*2+1) return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(x,height,z),builder->{
            for(var lot:plan.get().lots()) {
                ResourceLocation id=lot.module()<0 ? plaza : module(lot.module()).template();
                int relief=lot.module()<0 ? 3 : module(lot.module()).relief();
                builder.addPiece(new LandscapePiece(context.structureTemplateManager(),id,
                        new BlockPos(lot.x()-lot.radius(),lot.ground()-1,lot.z()-lot.radius()),
                        fromSouth(lot.face()),lot.radius(),relief));
            }
            builder.addPiece(new TerracePathPiece(plan.get().paths()));
        }));
    }
    private Module module(int index) { return index==0 ? hall : cottages.get(index-1); }
    private static boolean validBiome(GenerationContext context,int x,int y,int z) {
        return context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x),QuartPos.fromBlock(y),
                QuartPos.fromBlock(z),context.randomState().sampler()));
    }
    public static Rotation fromSouth(Direction face) {
        return switch(face) { case NORTH->Rotation.CLOCKWISE_180;case EAST->Rotation.COUNTERCLOCKWISE_90;
            case WEST->Rotation.CLOCKWISE_90;default->Rotation.NONE; };
    }
    @Override public StructureType<?> type() { return ModStructures.MOUNTAIN_TOWN.get(); }
}
