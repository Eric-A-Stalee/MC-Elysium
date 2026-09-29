package dev.elysium.structure;

import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

/** Saved cuts, retaining walls and connected streets, applied before the building modules. */
public final class TownTerrainPiece extends StructurePiece {
    private final List<ValleyTownPlanner.Column> columns;
    public TownTerrainPiece(List<ValleyTownPlanner.Column> columns) {
        super(ModStructures.TOWN_TERRAIN_PIECE.get(),0,bounds(columns));
        this.columns=List.copyOf(columns);setOrientation(null);
    }
    public TownTerrainPiece(CompoundTag tag) { this(read(tag.getIntArray("Columns"))); }
    public List<ValleyTownPlanner.Column> columns() { return columns; }
    private static List<ValleyTownPlanner.Column> read(int[] data) {
        if(data.length==0 || data.length%5!=0 || data.length>250000)throw new IllegalArgumentException("Invalid town terrain");
        var result=new ArrayList<ValleyTownPlanner.Column>();
        for(int i=0;i<data.length;i+=5)result.add(new ValleyTownPlanner.Column(data[i],data[i+1],data[i+2],data[i+3],data[i+4]));
        return result;
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        int[] data=new int[columns.size()*5];int i=0;
        for(var c:columns) {data[i++]=c.x();data[i++]=c.z();data[i++]=c.ground();data[i++]=c.original();data[i++]=c.kind();}
        tag.putIntArray("Columns",data);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,
            BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        var saved=new HashMap<Long,ValleyTownPlanner.Column>();
        for(var c:columns)saved.put(TerrainSampler.key(c.x(),c.z()),c);
        for(var c:columns) {
            if(c.x()<clip.minX() || c.x()>clip.maxX() || c.z()<clip.minZ() || c.z()>clip.maxZ())continue;
            int top=c.ground(),x=c.x(),z=c.z();
            for(int y=Math.min(top,c.original())-1;y<top;y++)
                placeBlock(level,(y%4==0?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS).defaultBlockState(),x,y,z,clip);
            var surface=Blocks.GRASS_BLOCK.defaultBlockState();
            if(c.kind()==ValleyTownPlanner.ROAD) {
                surface=(Math.floorMod(x*31+z*17,9)==0?Blocks.ANDESITE:Blocks.STONE_BRICKS).defaultBlockState();
                for(Direction d:Direction.Plane.HORIZONTAL) {
                    var next=saved.get(TerrainSampler.key(x+d.getStepX(),z+d.getStepZ()));
                    if(next!=null && next.ground()==top-1) {
                        surface=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,d.getOpposite());break;
                    }
                }
            }
            placeBlock(level,surface,x,top,z,clip);
            for(int y=top+1;y<=Math.max(c.original()+1,top+3);y++)placeBlock(level,Blocks.AIR.defaultBlockState(),x,y,z,clip);
            if(c.kind()==ValleyTownPlanner.COURT && top-c.original()>=3) {
                boolean edge=false;
                for(Direction d:Direction.Plane.HORIZONTAL)edge|=!saved.containsKey(TerrainSampler.key(x+d.getStepX(),z+d.getStepZ()));
                if(edge)placeBlock(level,Blocks.STONE_BRICK_WALL.defaultBlockState(),x,top+1,z,clip);
            }
        }
    }
    private static BoundingBox bounds(List<ValleyTownPlanner.Column> columns) {
        if(columns.isEmpty() || columns.size()>50000)throw new IllegalArgumentException("Invalid town terrain extent");
        var c=columns.getFirst();var box=new BoundingBox(c.x(),Math.min(c.ground(),c.original())-1,c.z(),c.x(),Math.max(c.ground()+3,c.original()+1),c.z());
        for(var p:columns) {
            box.encapsulate(new BlockPos(p.x(),Math.min(p.ground(),p.original())-1,p.z()));
            box.encapsulate(new BlockPos(p.x(),Math.max(p.ground()+3,p.original()+1),p.z()));
        }
        return box;
    }
}
