package dev.elysium.structure;

import dev.elysium.block.ForgeServiceBlockEntity;
import dev.elysium.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Saved placement and chunk clipping are independent of the working/ruin geometry. */
public final class HephaestusForgePiece extends StructurePiece {
    private final ForgePlan plan;
    private final ForgeGeometry geometry;
    public HephaestusForgePiece(ForgePlan plan) {
        super(ModStructures.HEPHAESTUS_FORGE_PIECE.get(),0,bounds(plan));
        this.plan=plan; geometry=new ForgeGeometry(plan); setOrientation(null);
    }
    public HephaestusForgePiece(CompoundTag tag) {
        this(new ForgePlan(tag.getInt("X"),tag.getInt("Z"),tag.getInt("EntranceFloor"),tag.getInt("Turn")));
        if(tag.getInt("Version")!=2) throw new IllegalArgumentException("Regenerate alpha-15 Forge chunks for the alpha-16 ruin");
    }
    public ForgePlan plan() { return plan; }
    public BlockPos hearth() { return at(0,plan.floor()+1,61); }
    public BlockPos ledger() { return at(-37,plan.floor()+1,50); }
    public BlockPos at(int u,int y,int v) { return new BlockPos(plan.worldX(u,v),y,plan.worldZ(u,v)); }
    private static BoundingBox bounds(ForgePlan p) {
        int x1=Integer.MAX_VALUE,z1=x1,x2=Integer.MIN_VALUE,z2=x2;
        for(int u:new int[]{ForgePlan.MIN_U,ForgePlan.MAX_U})for(int v:new int[]{ForgePlan.MIN_V,ForgePlan.MAX_V}) {
            x1=Math.min(x1,p.worldX(u,v));x2=Math.max(x2,p.worldX(u,v));z1=Math.min(z1,p.worldZ(u,v));z2=Math.max(z2,p.worldZ(u,v));
        }
        return new BoundingBox(x1,p.floor()-2,z1,x2,p.entranceFloor()+11,z2);
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag) {
        tag.putInt("Version",2);tag.putInt("X",plan.x());tag.putInt("Z",plan.z());
        tag.putInt("EntranceFloor",plan.entranceFloor());tag.putInt("Turn",plan.turn());
    }
    public BlockState materialAt(int u,int y,int v) { return geometry.at(u,y,v); }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,
            RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        for(int x=Math.max(clip.minX(),boundingBox.minX());x<=Math.min(clip.maxX(),boundingBox.maxX());x++)
            for(int z=Math.max(clip.minZ(),boundingBox.minZ());z<=Math.min(clip.maxZ(),boundingBox.maxZ());z++) {
                int u=plan.localU(x,z),v=plan.localV(x,z);
                for(int y=Math.max(clip.minY(),boundingBox.minY());y<=Math.min(clip.maxY(),boundingBox.maxY());y++) {
                    var state=materialAt(u,y,v);
                    if(state!=null)placeBlock(level,state,x,y,z,clip);
                }
            }
        for(var fixture:ForgeFixtures.ALL) {
            var pos=fixture.worldPosition(plan);
            if(clip.isInside(pos)&&level.getBlockEntity(pos) instanceof ForgeServiceBlockEntity service)
                service.bind(plan,fixture.circuit());
        }
    }
}
