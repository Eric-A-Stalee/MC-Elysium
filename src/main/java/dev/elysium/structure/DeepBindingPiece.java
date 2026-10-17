package dev.elysium.structure;

import dev.elysium.block.BindingBlock;
import dev.elysium.registry.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** The Maw, the Cistern and the Empty Throne share surveyed routes, not room geometry. */
public final class DeepBindingPiece extends StructurePiece {
    private final int x,y,z,low,kind;private final long seed;private final int[] route;
    public DeepBindingPiece(int x,int y,int z,int low,int kind,long seed,int[] route){
        super(ModStructures.DEEP_BINDING_PIECE.get(),0,new BoundingBox(x-30,low,z-30,x+30,y+38,z+90));
        if(kind<0||kind>2||route.length!=64)throw new IllegalArgumentException("Invalid binding survey");
        this.x=x;this.y=y;this.z=z;this.low=low;this.kind=kind;this.seed=seed;this.route=route.clone();setOrientation(null);
    }
    public DeepBindingPiece(CompoundTag t){this(t.getInt("X"),t.getInt("Y"),t.getInt("Z"),t.getInt("Low"),t.getInt("Kind"),t.getLong("Seed"),t.getIntArray("Route"));}
    public BlockPos core(){return new BlockPos(x,y+2,z);}public int kind(){return kind;}
    public List<BlockPos> approachRoute(){return java.util.stream.IntStream.range(0,64).mapToObj(i->new BlockPos(x,y+route[i],z+27+i)).toList();}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("X",x);t.putInt("Y",y);t.putInt("Z",z);t.putInt("Low",low);t.putInt("Kind",kind);t.putLong("Seed",seed);t.putIntArray("Route",route);}
    private BlockState stone(int u,int h,int v){return (Math.floorMod(u*31+v*17+h+(int)seed,11)==0?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_BRICKS).defaultBlockState();}
    public BlockState materialAt(int wx,int wy,int wz){
        if(!boundingBox.isInside(wx,wy,wz))return null;int u=wx-x,h=wy-y,v=wz-z;double r=Math.hypot(u,v);
        if(u==0&&v==0&&h==2)return ModBlocks.DEEP_BINDING.get().defaultBlockState().setValue(BindingBlock.KIND,kind);
        if(v>=27&&Math.abs(u)<=2){int floor=route[v-27];if(h>=low-y&&h<=floor)return h==floor?stone(u,h,v):Blocks.BASALT.defaultBlockState();if(h>floor&&h<=floor+5)return Blocks.CAVE_AIR.defaultBlockState();return null;}
        // A continuous central route reaches every restraint, even across the cistern and the broken jaw.
        if(Math.abs(u)<=2&&v>=-3&&v<=27){if(h<=1&&h>=low-y)return stone(u,h,v);if(h>1&&h<7)return Blocks.CAVE_AIR.defaultBlockState();}
        if(kind==0){
            double jaw=Math.sqrt(u*u/1.4+(v+9)*(v+9));
            if(jaw<7&&h>=-7&&h<10)return h==-7?Blocks.SOUL_SOIL.defaultBlockState():Blocks.CAVE_AIR.defaultBlockState();
            if(jaw>=7&&jaw<10&&v<1&&h>=0&&h<4)return ModBlocks.CHARRED_BONE.get().defaultBlockState();
            if(v<-2&&jaw>=7.5&&jaw<10&&Math.floorMod(u*3+v,9)<3&&h>=4&&h<10-Math.abs(u)/3)return ModBlocks.CHARRED_BONE.get().defaultBlockState();
            if(Math.abs(u)>17&&Math.abs(u)<21&&v<8&&v>-22&&Math.floorMod(v,9)<3&&h>=0&&h<25-Math.abs(v+6)/2)return h%7==0?ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState():ModBlocks.CHARRED_BONE.get().defaultBlockState();
        }else if(kind==1){
            if(r>5&&r<18&&Math.abs(u)>2){if(h==-3)return Blocks.PACKED_MUD.defaultBlockState();if(h>-3&&h<=0)return Blocks.WATER.defaultBlockState();if(h>0&&h<12)return Blocks.CAVE_AIR.defaultBlockState();}
            if(r>=18&&r<21&&h>=-3&&h<=2+(u<0?2:0))return (h==2?Blocks.CHISELED_DEEPSLATE:Blocks.MUD_BRICKS).defaultBlockState();
            // A slumped, faceless mourner and a broken aqueduct. No ice or blue light.
            if(v<-18&&v>-26&&Math.abs(u+7)<7&&h>=3&&h<21-Math.abs(u+7)*2)return (h>15?Blocks.BASALT:Blocks.POLISHED_BLACKSTONE).defaultBlockState();
            if(u>20&&u<25&&Math.floorMod(v+30,12)<3&&v<13&&h>=0&&h<16)return stone(u,h,v);
        }else{
            if(v>=-24&&v<=-18&&Math.abs(u)<12&&h>=2&&h<=31-Math.abs(u)/2)return (h%8==0?ModBlocks.FORGE_IRON.get():Blocks.CHISELED_DEEPSLATE).defaultBlockState();
            if(v>=-18&&v<=-10&&Math.abs(u)<12&&h>=2&&h<=7)return stone(u,h,v);
            if(v>=-18&&v<=-6&&Math.abs(u)>=9&&Math.abs(u)<=12&&h>=7&&h<=13)return ModBlocks.FORGE_IRON.get().defaultBlockState();
            for(int a:new int[]{-23,21})if(Math.abs(u-a)<=1&&h>=4&&h<=37){int k=(h-4)%12-6;double ring=(v+8)*(v+8)/12.0+k*k/36.0;if(ring>.5&&ring<1.4)return ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState();}
        }
        if(r<25){
            if(h<=1&&h>=Math.max(low-y,-9))return h==1?stone(u,h,v):Blocks.BLACKSTONE.defaultBlockState();
            if(h>1&&h<11)return Blocks.CAVE_AIR.defaultBlockState();
        }
        if(r>=25&&r<29+2*Math.sin(v*.4)&&h>=low-y&&h<=3+3*Math.sin(u*.2+v*.4))return (kind==1?Blocks.PACKED_MUD:Blocks.BASALT).defaultBlockState();
        return null;
    }
    @Override public void postProcess(WorldGenLevel l,StructureManager m,ChunkGenerator g,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var p=new BlockPos.MutableBlockPos();for(int wx=Math.max(clip.minX(),boundingBox.minX());wx<=Math.min(clip.maxX(),boundingBox.maxX());wx++)for(int wz=Math.max(clip.minZ(),boundingBox.minZ());wz<=Math.min(clip.maxZ(),boundingBox.maxZ());wz++)for(int wy=low;wy<=boundingBox.maxY();wy++){var s=materialAt(wx,wy,wz);if(s!=null)l.setBlock(p.set(wx,wy,wz),s,2);}
    }
}
