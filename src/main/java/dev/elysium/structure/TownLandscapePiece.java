package dev.elysium.structure;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Saved landscape decisions generate identically in either chunk order without surveying neighbors. */
public final class TownLandscapePiece extends StructurePiece {
    private final TownLandscapePlan plan;
    private final int version;
    public TownLandscapePiece(TownLandscapePlan plan) {
        this(plan,plan.trees().isEmpty()?2:plan.trees().getFirst().form());
    }
    private TownLandscapePiece(TownLandscapePlan plan,int version) {
        super(ModStructures.TOWN_LANDSCAPE_PIECE.get(),0,bounds(plan));this.plan=plan;this.version=version;setOrientation(null);
    }
    public TownLandscapePiece(CompoundTag tag) {this(read(tag),tag.getInt("Version"));}
    public TownLandscapePlan plan() {return plan;}
    private static TownLandscapePlan read(CompoundTag tag) {
        int version=tag.getInt("Version");
        if(version<1 || version>2)throw new IllegalArgumentException("Unknown town landscape version");
        int[] ts=tag.getIntArray("Trees"),ds=tag.getIntArray("Details"),os=tag.getIntArray("Obstacles");long[] seeds=tag.getLongArray("Seeds");
        if(ts.length%5!=0 || ts.length>60 || seeds.length!=ts.length/5 || ds.length%4!=0 || ds.length>80000 || os.length%6!=0 || os.length>1200)
            throw new IllegalArgumentException("Invalid saved town landscape");
        var trees=new ArrayList<TownLandscapePlan.Tree>();var details=new ArrayList<TownLandscapePlan.Detail>();var obstacles=new ArrayList<TownLandscapePlan.Obstacle>();
        for(int i=0;i<ts.length;i+=5)trees.add(new TownLandscapePlan.Tree(ts[i],ts[i+1],ts[i+2],ts[i+3],ts[i+4],seeds[i/5],version));
        for(int i=0;i<ds.length;i+=4)details.add(new TownLandscapePlan.Detail(ds[i],ds[i+1],ds[i+2],TownLandscapePlan.Kind.values()[ds[i+3]]));
        for(int i=0;i<os.length;i+=6)obstacles.add(new TownLandscapePlan.Obstacle(os[i],os[i+1],os[i+2],os[i+3],os[i+4],os[i+5]));
        return new TownLandscapePlan(trees,details,obstacles);
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.putInt("Version",version);
        int[] trees=new int[plan.trees().size()*5],details=new int[plan.details().size()*4],obstacles=new int[plan.obstacles().size()*6];
        long[] seeds=new long[plan.trees().size()];int i=0,j=0;
        for(var t:plan.trees()){trees[i++]=t.x();trees[i++]=t.y();trees[i++]=t.z();trees[i++]=t.height();trees[i++]=t.radius();seeds[j++]=t.seed();}
        i=0;for(var d:plan.details()){details[i++]=d.x();details[i++]=d.y();details[i++]=d.z();details[i++]=d.kind().ordinal();}
        i=0;for(var o:plan.obstacles()){obstacles[i++]=o.minX();obstacles[i++]=o.minY();obstacles[i++]=o.minZ();obstacles[i++]=o.maxX();obstacles[i++]=o.maxY();obstacles[i++]=o.maxZ();}
        tag.putIntArray("Trees",trees);tag.putLongArray("Seeds",seeds);tag.putIntArray("Details",details);tag.putIntArray("Obstacles",obstacles);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,
            BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        for(var t:plan.trees()) {
            int reach=t.radius()*2;
            if(t.x()+reach<clip.minX() || t.x()-reach>clip.maxX() || t.z()+reach<clip.minZ() || t.z()-reach>clip.maxZ())continue;
            for(var cell:t.geometry().entrySet()) {
                var c=cell.getKey();var pos=new BlockPos(c.x(),c.y(),c.z());
                if(!clip.isInside(pos) || plan.obstacles().stream().anyMatch(o->o.contains(c)))continue;
                var current=level.getBlockState(pos);
                if(current.isAir() || current.canBeReplaced() || current.is(BlockTags.LEAVES))placeBlock(level,state(cell.getValue()),pos.getX(),pos.getY(),pos.getZ(),clip);
            }
        }
        for(var d:plan.details()) {
            var pos=new BlockPos(d.x(),d.y(),d.z());if(!clip.isInside(pos))continue;
            var state=state(d.kind());
            if(d.kind()==TownLandscapePlan.Kind.FLOWER || d.kind()==TownLandscapePlan.Kind.LITTER) {
                if(!level.getBlockState(pos).canBeReplaced() || !state.canSurvive(level,pos))continue;
            }
            placeBlock(level,state,pos.getX(),pos.getY(),pos.getZ(),clip);
        }
    }
    private static BlockState state(TownLandscapePlan.Kind kind) {
        return switch(kind) {
            case BIRCH_Y,BIRCH_X,BIRCH_Z->Blocks.BIRCH_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                    kind==TownLandscapePlan.Kind.BIRCH_X?Direction.Axis.X:kind==TownLandscapePlan.Kind.BIRCH_Z?Direction.Axis.Z:Direction.Axis.Y);
            case LEAVES->ModBlocks.GOLDEN_BIRCH_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT,true).setValue(LeavesBlock.DISTANCE,1);
            case STONE->Blocks.STONE_BRICKS.defaultBlockState();
            case CAP->Blocks.STONE_BRICK_WALL.defaultBlockState();
            case ROCK->Blocks.CALCITE.defaultBlockState();
            case FLOWER->Blocks.DANDELION.defaultBlockState();
            case LITTER->ModBlocks.LEAF_LITTER.get().defaultBlockState();
            case POST->Blocks.SPRUCE_FENCE.defaultBlockState();
            case LANTERN->Blocks.LANTERN.defaultBlockState();
        };
    }
    private static BoundingBox bounds(TownLandscapePlan plan) {
        BoundingBox result=null;
        for(var t:plan.trees()) {
            int r=t.radius()*2;var box=new BoundingBox(t.x()-r,t.y()-1,t.z()-r,t.x()+r,t.y()+t.height()+2,t.z()+r);
            if(result==null)result=box;else {result.encapsulate(new BlockPos(box.minX(),box.minY(),box.minZ()));result.encapsulate(new BlockPos(box.maxX(),box.maxY(),box.maxZ()));}
        }
        for(var d:plan.details()) {
            var pos=new BlockPos(d.x(),d.y(),d.z());if(result==null)result=new BoundingBox(pos);else result.encapsulate(pos);
        }
        if(result==null)throw new IllegalArgumentException("Empty town landscape");return result;
    }
}
