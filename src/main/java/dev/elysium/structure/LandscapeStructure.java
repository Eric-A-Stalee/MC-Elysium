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

/** One parameterized placer for original hamlets, spring trees, and lookout shrines. */
public final class LandscapeStructure extends Structure {
    public static final MapCodec<LandscapeStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.listOf(1, 8).fieldOf("templates").forGetter(site -> site.templates),
            Codec.intRange(2, 20).fieldOf("radius").forGetter(site -> site.radius),
            Codec.intRange(0, 4).fieldOf("max_relief").forGetter(site -> site.maxRelief),
            Codec.intRange(0, 96).optionalFieldOf("min_height_above_sea", 0).forGetter(site -> site.minHeight),
            Codec.intRange(0, 64).optionalFieldOf("view_drop", 0).forGetter(site -> site.viewDrop),
            Codec.BOOL.optionalFieldOf("water_approach", false).forGetter(site -> site.waterApproach),
            Codec.BOOL.optionalFieldOf("require_water_approach", false).forGetter(site -> site.requireWater)
    ).apply(instance, LandscapeStructure::new));

    private final List<ResourceLocation> templates;
    private final int radius, maxRelief, minHeight, viewDrop;
    private final boolean waterApproach, requireWater;

    public LandscapeStructure(StructureSettings settings, List<ResourceLocation> templates, int radius,
                              int maxRelief, int minHeight, int viewDrop, boolean waterApproach, boolean requireWater) {
        super(settings);
        this.templates = List.copyOf(templates);
        this.radius = radius;
        this.maxRelief = maxRelief;
        this.minHeight = minHeight;
        this.viewDrop = viewDrop;
        this.waterApproach = waterApproach;
        this.requireWater = requireWater;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX(), z = context.chunkPos().getMiddleBlockZ();
        int sea = context.chunkGenerator().getSeaLevel();
        var terrain = new TerrainSampler(context);
        if (terrain.height(x, z) < sea + minHeight) return Optional.empty();
        for (int dx : new int[]{-radius, 0, radius}) {
            for (int dz : new int[]{-radius, 0, radius}) {
                if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x + dx),
                        QuartPos.fromBlock(terrain.height(x + dx, z + dz)), QuartPos.fromBlock(z + dz),
                        context.randomState().sampler()))) return Optional.empty();
            }
        }
        var pad = LandscapePlanner.pad(terrain, x, z, radius, sea, maxRelief);
        if (pad.isEmpty()) return Optional.empty();
        int ground = pad.getAsInt();
        Optional<Direction> view = viewDrop > 0 ? LandscapePlanner.outlook(terrain, x, z, ground, viewDrop) : Optional.empty();
        if (viewDrop > 0 && view.isEmpty()) return Optional.empty();
        ResourceLocation template = templates.get(context.random().nextInt(templates.size()));
        var size = context.structureTemplateManager().getOrCreate(template).getSize();
        if (size.getX() != radius * 2 + 1 || size.getZ() != radius * 2 + 1) return Optional.empty();
        var path = waterApproach || requireWater ? LandscapePlanner.bankPath(terrain, x, z, radius, ground, sea) : Optional.<LandscapePlanner.BankPath>empty();
        if (requireWater && path.isEmpty()) return Optional.empty();
        // Lookouts face north in their template; cottage doors face south.
        Rotation rotation = requireWater ? facing(path.orElseThrow().direction().getOpposite())
                : view.map(LandscapeStructure::facing).orElseGet(() -> Rotation.getRandom(context.random()));
        return Optional.of(new GenerationStub(new BlockPos(x, ground + 1, z), builder -> {
            builder.addPiece(new LandscapePiece(context.structureTemplateManager(), template,
                    new BlockPos(x - radius, ground - 1, z - radius), rotation, radius, maxRelief));
            path.ifPresent(route -> builder.addPiece(new BankPathPiece(route)));
        }));
    }

    private static Rotation facing(Direction direction) {
        return switch (direction) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Override public StructureType<?> type() { return ModStructures.LANDSCAPE_SITE.get(); }
}
