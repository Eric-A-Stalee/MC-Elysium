package dev.elysium.structure;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TartarusTerrain;
import java.util.*;
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

/** A ruined winding stair in the Eye's chamber continues into the actual deep terrain. */
public final class UnderworldDescentPiece extends StructurePiece {
    public static final int TOP=-232;
    public record Step(int x,int y,int z) {}
    private final int x,z,bottom;
    private final int[] exit;
    private final List<Step> route=new ArrayList<>();
    private record Cell(int floor,int distance) {}
    private final Map<Long,List<Cell>> columns=new HashMap<>();

    public UnderworldDescentPiece(TartarusDepthsPlan depths,TartarusTerrain terrain) {
        this(depths.x+140,depths.z+236,survey(depths.x+140,depths.z+236,terrain));
    }
    private UnderworldDescentPiece(int x,int z,int[] exit) {
        super(ModStructures.UNDERWORLD_DESCENT_PIECE.get(),0,new BoundingBox(x-10,Arrays.stream(exit).min().orElse(-520)-8,z-10,x+26,TOP+4,z+10));
        this.x=x;this.z=z;this.exit=exit;bottom=exit[0];setOrientation(null);
        int drop=TOP-bottom,steps=Math.max(56,((drop+55)/56)*56);
        for(int i=0;i<=steps;i++) {
            int p=i%56,u,v;
            // Start halfway along the eastern wall, where a radial way reaches open ground.
            int shifted=(p+7)%56;
            if(shifted<14){u=7;v=shifted-7;}else if(shifted<28){u=21-shifted;v=7;}
            else if(shifted<42){u=-7;v=35-shifted;}else{u=shifted-49;v=-7;}
            var step=new Step(u,TOP-(int)Math.floor((double)i*drop/steps),v);route.add(step);
            for(int du=-1;du<=1;du++)for(int dv=-1;dv<=1;dv++) {
                var cells=columns.computeIfAbsent(key(u+du,v+dv),ignored->new ArrayList<>());
                var cell=new Cell(step.y,Math.max(Math.abs(du),Math.abs(dv)));
                int nearby=-1;
                for(int c=0;c<cells.size();c++)if(Math.abs(cells.get(c).floor-step.y)<8){nearby=c;break;}
                if(nearby<0)cells.add(cell);else if(cell.distance<cells.get(nearby).distance)cells.set(nearby,cell);
            }
        }
        for(int i=1;i<exit.length;i++)route.add(new Step(7+i,exit[i],0));
    }
    public UnderworldDescentPiece(CompoundTag tag){this(tag.getInt("X"),tag.getInt("Z"),tag.getIntArray("Exit"));}
    private static int[] survey(int x,int z,TartarusTerrain terrain) {
        int[] floors=new int[20];
        for(int i=0;i<floors.length;i++) {
            int wanted=terrain.floor(x+7+i,z);
            floors[i]=i==0?wanted:Math.max(floors[i-1]-1,Math.min(floors[i-1]+1,wanted));
        }
        return floors;
    }
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    public int x(){return x;} public int z(){return z;} public int bottom(){return bottom;}
    public List<Step> route(){return List.copyOf(route);}
    public BlockPos at(Step s){return new BlockPos(x+s.x,s.y,z+s.z);}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){tag.putInt("X",x);tag.putInt("Z",z);tag.putIntArray("Exit",exit);}

    public BlockState materialAt(int wx,int y,int wz) {
        int u=wx-x,v=wz-z,r=Math.max(Math.abs(u),Math.abs(v));
        if(!boundingBox.isInside(wx,y,wz))return null;
        if(u>=8&&u<7+exit.length&&Math.abs(v)<=1) {
            int ground=exit[u-7];
            if(y>=ground-3&&y<=ground+4)return y<=ground?Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState():Blocks.CAVE_AIR.defaultBlockState();
        }
        if(r>9)return null;
        // Only the first steps break the chamber floor. The rest is visibly a shaft below it.
        if(y>TOP&&Math.abs(u-7)>1||y>TOP&&Math.abs(v)>2)return null;
        if(y==TOP&&!(u>=6&&Math.abs(v)<=3))return null;
        if(y<=bottom-2)return Blocks.DEEPSLATE.defaultBlockState();
        for(var step:columns.getOrDefault(key(u,v),List.of())) {
            if(y==step.floor||y==step.floor-1)return (Math.floorMod(u+v+y,9)==0?Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
            if(y>step.floor&&y<=step.floor+3)return Blocks.CAVE_AIR.defaultBlockState();
        }
        if(r==9&&y<TOP)return (Math.floorMod(y,28)==0?ModBlocks.FORGE_IRON.get():Blocks.DEEPSLATE_BRICKS).defaultBlockState();
        if(Math.abs(u)<=1&&v==0&&y<TOP-3)return ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState();
        if(y>=bottom-1&&y<=TOP)return Blocks.CAVE_AIR.defaultBlockState();
        return null;
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        var pos=new BlockPos.MutableBlockPos();
        for(int wx=Math.max(clip.minX(),boundingBox.minX());wx<=Math.min(clip.maxX(),boundingBox.maxX());wx++)
            for(int wz=Math.max(clip.minZ(),boundingBox.minZ());wz<=Math.min(clip.maxZ(),boundingBox.maxZ());wz++)
                for(int y=boundingBox.minY();y<=boundingBox.maxY();y++) {
                    var state=materialAt(wx,y,wz);if(state!=null)level.setBlock(pos.set(wx,y,wz),state,2);
                }
    }
}
