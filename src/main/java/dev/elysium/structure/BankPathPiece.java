package dev.elysium.structure;

import dev.elysium.registry.ModStructures;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A saved three-block bank approach with steps and a small dry-side landing. */
public final class BankPathPiece extends StructurePiece {
    private final LandscapePlanner.BankPath path;

    public BankPathPiece(LandscapePlanner.BankPath path) {
        super(ModStructures.BANK_PATH_PIECE.get(), 0, bounds(path));
        this.path = path;
        setOrientation(null);
    }

    public BankPathPiece(CompoundTag tag) {
        this(new LandscapePlanner.BankPath(tag.getInt("StartX"), tag.getInt("StartZ"),
                Direction.from3DDataValue(tag.getInt("Direction")), tag.getIntArray("Surfaces")));
    }

    public LandscapePlanner.BankPath path() { return path; }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("StartX", path.startX()); tag.putInt("StartZ", path.startZ());
        tag.putInt("Direction", path.direction().get3DDataValue()); tag.putIntArray("Surfaces", path.surfaces());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
        for (int along = 0; along < path.length(); along++) {
            int surface = path.surface(along);
            boolean landing = along >= path.length() - 3;
            Direction up = null;
            if (along > 0 && surface > path.surface(along - 1)) up = path.direction();
            else if (along + 1 < path.length() && surface > path.surface(along + 1)) up = path.direction().getOpposite();
            for (int across = -1; across <= 1; across++) {
                int x = path.x(along, across), z = path.z(along, across);
                placeBlock(level, Blocks.DIRT.defaultBlockState(), x, surface - 1, z, clip);
                var top = landing ? Blocks.BIRCH_PLANKS.defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState();
                if (up != null) top = Blocks.SMOOTH_SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up);
                placeBlock(level, top, x, surface, z, clip);
                for (int y = surface + 1; y <= surface + 3; y++) placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, clip);
                if (along == path.length() - 2 && across == 1) {
                    placeBlock(level, Blocks.BIRCH_FENCE.defaultBlockState(), x, surface + 1, z, clip);
                    placeBlock(level, Blocks.LANTERN.defaultBlockState(), x, surface + 2, z, clip);
                }
            }
        }
    }

    private static BoundingBox bounds(LandscapePlanner.BankPath path) {
        int[] xs = {path.x(0, -1), path.x(0, 1), path.x(path.length() - 1, -1), path.x(path.length() - 1, 1)};
        int[] zs = {path.z(0, -1), path.z(0, 1), path.z(path.length() - 1, -1), path.z(path.length() - 1, 1)};
        return new BoundingBox(Arrays.stream(xs).min().orElseThrow(), Arrays.stream(path.surfaces()).min().orElseThrow() - 1,
                Arrays.stream(zs).min().orElseThrow(), Arrays.stream(xs).max().orElseThrow(),
                Arrays.stream(path.surfaces()).max().orElseThrow() + 3, Arrays.stream(zs).max().orElseThrow());
    }
}
