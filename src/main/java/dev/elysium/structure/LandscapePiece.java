package dev.elysium.structure;

import dev.elysium.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Authored footprints only; short foundations and template entities are clipped to the generating chunk. */
public final class LandscapePiece extends TemplateStructurePiece {
    private final int radius, foundationDepth;

    public LandscapePiece(StructureTemplateManager manager, ResourceLocation template, BlockPos origin,
                          Rotation rotation, int radius, int foundationDepth) {
        super(ModStructures.LANDSCAPE_PIECE.get(), 0, manager, template, template.toString(), settings(rotation, radius), origin);
        this.radius = radius;
        this.foundationDepth = foundationDepth;
        includeFoundation();
    }

    public LandscapePiece(StructureTemplateManager manager, CompoundTag tag) {
        super(ModStructures.LANDSCAPE_PIECE.get(), tag, manager,
                template -> settings(Rotation.valueOf(tag.getString("Rotation")), tag.getInt("Radius")));
        radius = tag.getInt("Radius");
        foundationDepth = Math.clamp(tag.getInt("FoundationDepth"), 0, 4);
        includeFoundation();
    }

    private static StructurePlaceSettings settings(Rotation rotation, int radius) {
        return new StructurePlaceSettings().setRotation(rotation).setRotationPivot(new BlockPos(radius, 0, radius))
                .setIgnoreEntities(false).setFinalizeEntities(true).setLiquidSettings(LiquidSettings.IGNORE_WATERLOGGING);
    }

    private void includeFoundation() {
        boundingBox.encapsulate(new BlockPos(boundingBox.minX(), templatePosition.getY() - foundationDepth, boundingBox.minZ()));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putString("Rotation", getRotation().name());
        tag.putInt("Radius", radius);
        tag.putInt("FoundationDepth", foundationDepth);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        super.postProcess(level, structures, generator, random, clip, chunk, pivot);
        // Only authored bottom-layer soil/stone can grow a foundation. No rectangular
        // platform, cliff-length supports, or writes into another chunk.
        for (Block material : new Block[]{Blocks.DIRT, Blocks.SMOOTH_SANDSTONE}) {
            for (var block : template.filterBlocks(templatePosition, placeSettings, material)) {
                if (block.pos().getY() != templatePosition.getY()) continue;
                for (int depth = 1; depth <= foundationDepth; depth++) {
                    BlockPos below = block.pos().below(depth);
                    if (!clip.isInside(below)) break;
                    if (!level.getBlockState(below).isAir() && level.getFluidState(below).isEmpty()) break;
                    level.setBlock(below, material.defaultBlockState(), 2);
                }
            }
        }
        includeFoundation();
    }

    @Override
    protected void handleDataMarker(String marker, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox bounds) {
        // These original templates have no data markers, loot markers or command blocks.
    }
}
