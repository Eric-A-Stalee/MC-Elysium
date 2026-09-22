package dev.elysium.structure;

import dev.elysium.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Original five-block-wide birch crossing with three-block walkway and bounded stone piers. */
public final class ElysianBridgePiece extends StructurePiece {
    private final BridgePlanner.Span span;

    public ElysianBridgePiece(BridgePlanner.Span span) {
        super(ModStructures.ELYSIAN_BRIDGE_PIECE.get(), 0, bounds(span));
        this.span = span;
        setOrientation(null); // Explicit world coordinates also keep asymmetric stair facings stable after reload.
    }

    public ElysianBridgePiece(CompoundTag tag) {
        super(ModStructures.ELYSIAN_BRIDGE_PIECE.get(), tag);
        span = new BridgePlanner.Span(tag.getInt("StartX"), tag.getInt("StartZ"), tag.getBoolean("EastWest"),
                tag.getInt("Length"), tag.getInt("StartHeight"), tag.getInt("EndHeight"), tag.getInt("DeckHeight"),
                tag.getIntArray("FloorHeights"));
        setOrientation(null);
        // Derive, rather than trust, bounds so terrain profile and chunk clipping cannot disagree.
        boundingBox = bounds(span);
    }

    public BridgePlanner.Span span() { return span; }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("StartX", span.startX());
        tag.putInt("StartZ", span.startZ());
        tag.putBoolean("EastWest", span.eastWest());
        tag.putInt("Length", span.length());
        tag.putInt("StartHeight", span.startHeight());
        tag.putInt("EndHeight", span.endHeight());
        tag.putInt("DeckHeight", span.deckHeight());
        tag.putIntArray("FloorHeights", span.floorHeights());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBounds, ChunkPos chunkPos, BlockPos pivot) {
        Direction alongDirection = span.eastWest() ? Direction.EAST : Direction.SOUTH;
        BlockState rail = Blocks.BIRCH_FENCE.defaultBlockState()
                .setValue(span.eastWest() ? FenceBlock.EAST : FenceBlock.NORTH, true)
                .setValue(span.eastWest() ? FenceBlock.WEST : FenceBlock.SOUTH, true);
        for (int along = 0; along < span.length(); along++) {
            int feet = span.walkingHeight(along);
            boolean landing = along < 2 || along >= span.length() - 2;
            boolean pier = along == BridgePlanner.APPROACH_LENGTH
                    || along == span.length() - 1 - BridgePlanner.APPROACH_LENGTH
                    || (along > BridgePlanner.APPROACH_LENGTH && along < span.length() - 1 - BridgePlanner.APPROACH_LENGTH
                    && (along - BridgePlanner.APPROACH_LENGTH) % 7 == 0);
            Direction stairFacing = null;
            if (along > 0 && feet > span.walkingHeight(along - 1)) stairFacing = alongDirection;
            else if (along < span.length() - 1 && feet > span.walkingHeight(along + 1)) stairFacing = alongDirection.getOpposite();

            for (int across = -BridgePlanner.HALF_WIDTH; across <= BridgePlanner.HALF_WIDTH; across++) {
                int x = span.x(along, across);
                int z = span.z(along, across);
                boolean edge = Math.abs(across) == BridgePlanner.HALF_WIDTH;
                if (landing || (edge && pier)) {
                    // The saved noise floor bounds every pier, and every individual block is chunk-clipped.
                    for (int y = span.floorHeight(along, across) - 1; y < feet; y++) {
                        placeBlock(level, Blocks.SMOOTH_SANDSTONE.defaultBlockState(), x, y, z, chunkBounds);
                    }
                }
                BlockState deck = edge ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.BIRCH_PLANKS.defaultBlockState();
                if (!edge && stairFacing != null) {
                    deck = Blocks.BIRCH_STAIRS.defaultBlockState().setValue(StairBlock.FACING, stairFacing);
                }
                placeBlock(level, deck, x, feet - 1, z, chunkBounds);
                if (edge && !landing && !pier) {
                    placeBlock(level, Blocks.BIRCH_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP),
                            x, feet - 2, z, chunkBounds);
                }
                // Structures generate before trees. Clearing only walkway headroom preserves river and banks.
                for (int y = feet; y < feet + 3; y++) {
                    placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, chunkBounds);
                }
                if (edge) {
                    placeBlock(level, rail, x, feet, z, chunkBounds);
                    if (pier) {
                        placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), x, feet, z, chunkBounds);
                        placeBlock(level, Blocks.LANTERN.defaultBlockState(), x, feet + 1, z, chunkBounds);
                    }
                }
            }
        }
    }

    private static BoundingBox bounds(BridgePlanner.Span span) {
        return new BoundingBox(span.x(0, -BridgePlanner.HALF_WIDTH), span.minimumFloor() - 1,
                span.z(0, -BridgePlanner.HALF_WIDTH), span.x(span.length() - 1, BridgePlanner.HALF_WIDTH),
                span.deckHeight() + 3, span.z(span.length() - 1, BridgePlanner.HALF_WIDTH));
    }
}
