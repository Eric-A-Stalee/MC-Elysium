package dev.elysium.structure;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** One chunk-clipped piece owns chain, chamber excavation, vault and terminal anchor. */
public final class TartarusChainPiece extends StructurePiece {
    private final TartarusChainPlan plan;
    public TartarusChainPiece(TartarusChainPlan plan) {
        super(ModStructures.TARTARUS_CHAIN_PIECE.get(), 0, bounds(plan));
        this.plan = plan; setOrientation(null);
    }
    public TartarusChainPiece(CompoundTag tag) {
        super(ModStructures.TARTARUS_CHAIN_PIECE.get(), tag);
        plan = new TartarusChainPlan(tag.getInt("CenterX"), tag.getInt("CenterZ"), tag.getInt("Top"),
                tag.getInt("Floor"), tag.getBoolean("FirstEastWest"));
        boundingBox = bounds(plan); setOrientation(null);
    }
    public TartarusChainPlan plan() { return plan; }
    private static BoundingBox bounds(TartarusChainPlan p) {
        int r = TartarusChainPlan.CHAMBER_RADIUS;
        return new BoundingBox(p.x()-r, p.floor()-1, p.z()-r, p.x()+r, p.top(), p.z()+r);
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CenterX", plan.x()); tag.putInt("CenterZ", plan.z()); tag.putInt("Top", plan.top());
        tag.putInt("Floor", plan.floor()); tag.putBoolean("FirstEastWest", plan.firstEastWest());
    }
    /** Null preserves surrounding geology. Exposed ring interiors are never artificially cleared. */
    public BlockState materialAt(int dx, int y, int dz) {
        int radial = dx*dx + dz*dz, floor = plan.floor(), r = TartarusChainPlan.CHAMBER_RADIUS;
        if (plan.chainAt(dx, y, dz)) return ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState();
        if (radial > r*r || y > plan.roof(dx, dz) || y < floor-1) return null;
        if (y <= floor) return (dx == 0 || dz == 0 ? Blocks.POLISHED_BASALT : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
        if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2 && y <= floor+6)
            return (y == floor+6 ? ModBlocks.TARTARUS_CHAIN_BLOCK.get() : Blocks.CHISELED_POLISHED_BLACKSTONE).defaultBlockState();
        boolean wall = radial >= (r-2)*(r-2), roof = y >= plan.roof(dx, dz)-1;
        if (wall || roof) {
            if (wall && y >= floor+5 && y <= floor+7 && (dx == 0 || dz == 0)) return Blocks.GLOWSTONE.defaultBlockState();
            boolean rib = dx == 0 || dz == 0 || Math.abs(dx) == Math.abs(dz);
            return (rib ? Blocks.POLISHED_BASALT : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
        }
        return Blocks.CAVE_AIR.defaultBlockState();
    }
    @Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox clip, ChunkPos chunkPos, BlockPos pivot) {
        // Intersect first: no neighbouring chunk reads or writes, independent of generation order.
        for (int x=Math.max(clip.minX(),boundingBox.minX()); x<=Math.min(clip.maxX(),boundingBox.maxX()); x++)
            for (int z=Math.max(clip.minZ(),boundingBox.minZ()); z<=Math.min(clip.maxZ(),boundingBox.maxZ()); z++)
                for (int y=Math.max(clip.minY(),boundingBox.minY()); y<=Math.min(clip.maxY(),boundingBox.maxY()); y++) {
                    BlockState state = materialAt(x-plan.x(),y,z-plan.z());
                    if (state != null) placeBlock(level,state,x,y,z,clip);
                }
    }
}
