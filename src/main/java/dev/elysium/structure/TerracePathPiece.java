package dev.elysium.structure;

import dev.elysium.registry.ModStructures;
import dev.elysium.worldgen.TerrainSampler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/** One saved canonical height per path tile, including intersections and short retaining walls. */
public final class TerracePathPiece extends StructurePiece {
    private final List<TerracePlanner.Tile> tiles;
    public TerracePathPiece(List<TerracePlanner.Tile> tiles) {
        super(ModStructures.TERRACE_PATH_PIECE.get(),0,bounds(tiles));
        this.tiles=List.copyOf(tiles);setOrientation(null);
    }
    public TerracePathPiece(CompoundTag tag) { this(read(tag.getIntArray("Tiles"))); }
    public List<TerracePlanner.Tile> tiles() { return tiles; }
    private static List<TerracePlanner.Tile> read(int[] data) {
        if(data.length==0 || data.length%4!=0 || data.length>40000) throw new IllegalArgumentException("Invalid town path");
        List<TerracePlanner.Tile> result=new ArrayList<>();
        for(int i=0;i<data.length;i+=4) result.add(new TerracePlanner.Tile(data[i],data[i+1],data[i+2],data[i+3]));
        return result;
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        int[] data=new int[tiles.size()*4];int i=0;
        for(var tile:tiles) { data[i++]=tile.x();data[i++]=tile.z();data[i++]=tile.ground();data[i++]=tile.floor(); }
        tag.putIntArray("Tiles",data);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,
                                      RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        Map<Long,Integer> heights=new HashMap<>();
        for(var tile:tiles) heights.put(TerrainSampler.key(tile.x(),tile.z()),tile.ground());
        for(var tile:tiles) {
            int x=tile.x(),z=tile.z(),top=tile.ground();
            for(int y=tile.floor();y<top;y++) placeBlock(level,Blocks.STONE_BRICKS.defaultBlockState(),x,y,z,clip);
            var surface=Blocks.STONE_BRICKS.defaultBlockState();
            for(Direction direction:Direction.Plane.HORIZONTAL) {
                if(heights.getOrDefault(TerrainSampler.key(x+direction.getStepX(),z+direction.getStepZ()),top)==top-1) {
                    surface=Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,direction.getOpposite());break;
                }
            }
            placeBlock(level,surface,x,top,z,clip);
            for(int y=top+1;y<=top+3;y++) placeBlock(level,Blocks.AIR.defaultBlockState(),x,y,z,clip);
        }
    }
    private static BoundingBox bounds(List<TerracePlanner.Tile> tiles) {
        if(tiles.isEmpty() || tiles.size()>10000) throw new IllegalArgumentException("Invalid town path extent");
        var first=tiles.getFirst();var box=new BoundingBox(first.x(),first.floor(),first.z(),first.x(),first.ground()+3,first.z());
        for(var tile:tiles) { box.encapsulate(new BlockPos(tile.x(),tile.floor(),tile.z()));box.encapsulate(new BlockPos(tile.x(),tile.ground()+3,tile.z())); }
        return box;
    }
}
