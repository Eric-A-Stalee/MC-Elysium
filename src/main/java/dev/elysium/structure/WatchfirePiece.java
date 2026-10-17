package dev.elysium.structure;

import dev.elysium.block.WatchfireBlock;
import dev.elysium.block.WatchfireBlockEntity;
import dev.elysium.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import java.util.Arrays;

/** Ruin fragments and individual footings follow the surveyed slope; no broad flattened pad. */
public final class WatchfirePiece extends StructurePiece {
    private final int x,z,ground,watch;
    private final int[] heights;
    public WatchfirePiece(int x,int z,int ground,int watch,int[] heights) {
        super(ModStructures.WATCHFIRE_PIECE.get(),0,new BoundingBox(x-8,Arrays.stream(heights).min().orElse(ground)-2,z-8,x+8,Arrays.stream(heights).max().orElse(ground)+12,z+8));
        this.x=x;this.z=z;this.ground=ground;this.watch=watch;this.heights=heights.clone();setOrientation(null);
        if(heights.length!=289||watch<0||watch>2)throw new IllegalArgumentException("Invalid watch survey");
    }
    public WatchfirePiece(CompoundTag tag){this(tag.getInt("X"),tag.getInt("Z"),tag.getInt("Ground"),tag.getInt("Watch"),tag.getIntArray("Heights"));}
    public BlockPos socket(){return new BlockPos(x,ground+1,z);}
    public int watch(){return watch;}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){tag.putInt("X",x);tag.putInt("Z",z);tag.putInt("Ground",ground);tag.putInt("Watch",watch);tag.putIntArray("Heights",heights);}
    public BlockState material(int u,int y,int v) {
        int g=heights[(u+8)*17+v+8],h=y-g,r=u*u+v*v;
        Block stone=watch==2?Blocks.COBBLED_DEEPSLATE:watch==1?Blocks.TUFF_BRICKS:Blocks.MOSSY_STONE_BRICKS;
        if(u==0&&v==0&&y==ground+1)return ModBlocks.WATCHFIRE.get().defaultBlockState().setValue(WatchfireBlock.WATCH,watch);
        if(r<=5) {
            if(y>=Math.min(g,ground)-1&&y<=ground)return (y==ground?Blocks.CHISELED_STONE_BRICKS:stone).defaultBlockState();
            if(y>ground&&y<=ground+3)return Blocks.CAVE_AIR.defaultBlockState();
        }
        // Scattered approach stones retain each column's surface elevation.
        if(v>=2&&v<=8&&Math.abs(u-(v/3-1))<=1&&h==0)return (v%3==0?Blocks.COARSE_DIRT:stone).defaultBlockState();
        if(watch==0) {
            boolean pillar=(u==-5&&(v==-3||v==3))||(u==4&&v==-4)||(u==5&&v==2);
            if(pillar&&h>=-1&&h<=(u<0?5:v<0?3:2))return (h==0?stone:h%3==0?Blocks.STRIPPED_BIRCH_LOG:Blocks.BIRCH_LOG).defaultBlockState();
            if(u>=-5&&u<=-2&&v==-3&&y==heights[3*17+5]+5)return Blocks.BIRCH_SLAB.defaultBlockState();
            if(r>=18&&r<=45&&Math.floorMod(u*17+v*7,5)==0&&h==1)return ModBlocks.WILD_GRAIN.get().defaultBlockState();
        } else if(watch==1) {
            if(v==-5&&u>=-6&&u<=4&&h>=-1&&h<=(u< -2?4:1))return (h==3?Blocks.WAXED_WEATHERED_CUT_COPPER:stone).defaultBlockState();
            if((u==-5||u==5)&&Math.abs(v)<=2&&h>=-1&&h<=(v==0?3:1))return (h==3?Blocks.CHISELED_TUFF:stone).defaultBlockState();
            if(u==4&&v==3&&h==1)return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3);
        } else {
            if((u==-4||u==4)&&v==-4&&h>=-1&&h<=(u<0?9:5))return (h%4==0?Blocks.WAXED_OXIDIZED_CUT_COPPER:h%3==0?Blocks.CALCITE:Blocks.POLISHED_BASALT).defaultBlockState();
            if(v==-4&&u>=-4&&u<=0&&y==heights[4*17+4]+8)return Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState();
            if(u==-3&&v==-4&&y==heights[4*17+4]+7)return Blocks.CHAIN.defaultBlockState();
            if(u==-3&&v==-4&&y==heights[4*17+4]+6)return Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT,net.minecraft.world.level.block.state.properties.BellAttachType.CEILING);
        }
        if(r>=25&&r<=52&&Math.floorMod(u*31+v*13,11)==0&&h==1)return ModBlocks.LEAF_LITTER.get().defaultBlockState();
        if(r>=12&&r<=46&&Math.floorMod(u*11-v*19,17)==0&&h==1)return Blocks.OXEYE_DAISY.defaultBlockState();
        if((u==-6&&v==-6||u==6&&v==4)&&h>=-1&&h<=1)return stone.defaultBlockState();
        return null;
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        for(int u=-8;u<=8;u++)for(int v=-8;v<=8;v++)for(int y=boundingBox.minY();y<=boundingBox.maxY();y++) {
            var pos=new BlockPos(x+u,y,z+v);if(!clip.isInside(pos))continue;var state=material(u,y,v);
            if(state!=null)placeBlock(level,state,pos.getX(),y,pos.getZ(),clip);
        }
        if(clip.isInside(socket())&&level.getBlockEntity(socket()) instanceof WatchfireBlockEntity fire)fire.bind(watch);
    }
}
