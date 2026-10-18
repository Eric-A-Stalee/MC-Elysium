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
        super(ModStructures.DEEP_BINDING_PIECE.get(),0,new BoundingBox(x-(kind==2?44:30),low,z-(kind==2?55:30),x+(kind==2?44:30),y+(kind==2?66:38),z+90));
        if(kind<0||kind>2||route.length!=64)throw new IllegalArgumentException("Invalid binding survey");
        this.x=x;this.y=y;this.z=z;this.low=low;this.kind=kind;this.seed=seed;this.route=route.clone();setOrientation(null);
    }
    public DeepBindingPiece(CompoundTag t){this(t.getInt("X"),t.getInt("Y"),t.getInt("Z"),t.getInt("Low"),t.getInt("Kind"),t.getLong("Seed"),t.getIntArray("Route"));}
    public BlockPos core(){return new BlockPos(x,y+2,z);}public int kind(){return kind;}
    public List<BlockPos> approachRoute(){return java.util.stream.IntStream.range(0,64).mapToObj(i->new BlockPos(x,y+route[i],z+27+i)).toList();}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("X",x);t.putInt("Y",y);t.putInt("Z",z);t.putInt("Low",low);t.putInt("Kind",kind);t.putLong("Seed",seed);t.putIntArray("Route",route);}
    private BlockState stone(int u,int h,int v){return (Math.floorMod(u*31+v*17+h+(int)seed,11)==0?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_BRICKS).defaultBlockState();}
    private static boolean ellipsoid(double x,double y,double z,double a,double b,double c){return x*x/(a*a)+y*y/(b*b)+z*z/(c*c)<1;}
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
            for(int i=0;i<11;i++){double a=i*Math.PI*2/11;int tx=(int)Math.round(Math.sin(a)*10),tz=-9+(int)Math.round(Math.cos(a)*8),height=5+(i*7)%5;
                if(tz<0&&h>=3&&h<=height&&Math.hypot(u-tx,v-tz)<2.4-(h-3)*.19)return ModBlocks.CHARRED_BONE.get().defaultBlockState();
            }
            for(int rib:new int[]{-20,-8,6})if(h>=0&&h<25-Math.abs(rib+6)/6&&Math.abs(v-rib)<1.6&&Math.abs(Math.abs(u)-(21-.018*h*h))<1.6)
                return h<3?Blocks.BASALT.defaultBlockState():ModBlocks.CHARRED_BONE.get().defaultBlockState();
        }else if(kind==1){
            if(r>5&&r<18&&Math.abs(u)>2){if(h==-3)return Blocks.PACKED_MUD.defaultBlockState();if(h>-3&&h<=0)return Blocks.WATER.defaultBlockState();if(h>0&&h<12)return Blocks.CAVE_AIR.defaultBlockState();}
            if(r>=18&&r<21&&h>=-3&&h<=2+(u<0?2:0))return (h==2?Blocks.CHISELED_DEEPSLATE:Blocks.MUD_BRICKS).defaultBlockState();
            // A slumped, faceless mourner and a broken aqueduct. No ice or blue light.
            boolean head=ellipsoid(u+6,h-23,v+19,4.5,5,5),body=ellipsoid(u+7,h-11,v+22,7,10,5);
            boolean hands=(Math.abs(u+10)<2||Math.abs(u+2)<2)&&h>15&&h<24&&v>-18&&v<-14;
            if(head||body||hands){
                if(head&&Math.abs(u+6)<2&&h>=22&&h<=25&&v>-17)return h==24&&v==-16?ModBlocks.SMOULDERING_CLINKER.get().defaultBlockState():Blocks.CAVE_AIR.defaultBlockState();
                return (hands||head?Blocks.POLISHED_BLACKSTONE:Math.floorMod(u+v,5)==0?Blocks.BASALT:Blocks.DEEPSLATE).defaultBlockState();
            }
            if(u>20&&u<25&&Math.floorMod(v+30,12)<3&&v<13&&h>=0&&h<16)return stone(u,h,v);
        }else{
            // The Crownless is an entity in a surveyed bowl, with stone anchors
            // outside his body. No masonry statue occupies his breathing space.
            double bowl=Math.hypot(u,(v+23)*.92);
            for(int side:new int[]{-1,1}){
                int px=side*34;
                if(Math.abs(u-px)<=3&&Math.abs(v+26)<=4&&h>=low-y&&h<=58-(side<0?0:5)){
                    if(h==43||h==44||h==15)return ModBlocks.FORGE_IRON.get().defaultBlockState();
                    if(h>48&&Math.floorMod(u*7+v,5)<2)return null;
                    return stone(u,h,v);
                }
                // Broken cover is useful against the king's low travelling command.
                if(Math.abs(u-side*13)<=2&&Math.abs(v-6)<=2&&h>=0&&h<=5-Math.floorMod(v+u,2))return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            }
            if(bowl<28&&v<-4){
                if(h<=1&&h>=Math.max(low-y,-12))return h==1?ModBlocks.BURNT_CLINKER.get().defaultBlockState():Blocks.BLACKSTONE.defaultBlockState();
                if(h>1&&h<43)return Blocks.CAVE_AIR.defaultBlockState();
            }
            // A shattered apse is open above the captive's crown, not a rectangular room.
            if(bowl>=28&&bowl<32&&v<-16&&h>=0&&h<30+8*Math.sin(u*.17)){
                if(Math.floorMod(u+v,13)<3&&h>10)return null;
                return Math.floorMod(h,9)==0?ModBlocks.FORGE_IRON.get().defaultBlockState():stone(u,h,v);
            }
            if(bowl<39&&v<-4&&h>=low-y&&h<=Math.max(1,(int)(bowl-27)*.8))return Blocks.BASALT.defaultBlockState();
        }

        if(r<25){
            int floor=(kind==2&&r<15)||Math.sin(u*.27+v*.11)+Math.cos(v*.31)>-.6?1:0;
            if(h<=floor&&h>=Math.max(low-y,-9))return h==floor?(Math.sin(u*.23-v*.17)>.65?ModBlocks.CINDER_ASH.get().defaultBlockState():stone(u,h,v)):Blocks.BLACKSTONE.defaultBlockState();
            if(h>floor&&h<=1)return Blocks.CAVE_AIR.defaultBlockState();
            if(h>1&&h<11)return Blocks.CAVE_AIR.defaultBlockState();
        }
        if(r>=25&&r<29+2*Math.sin(v*.4)&&h>=low-y&&h<=3+3*Math.sin(u*.2+v*.4))return (kind==1?Blocks.PACKED_MUD:Blocks.BASALT).defaultBlockState();
        return null;
    }
    @Override public void postProcess(WorldGenLevel l,StructureManager m,ChunkGenerator g,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
        var p=new BlockPos.MutableBlockPos();for(int wx=Math.max(clip.minX(),boundingBox.minX());wx<=Math.min(clip.maxX(),boundingBox.maxX());wx++)for(int wz=Math.max(clip.minZ(),boundingBox.minZ());wz<=Math.min(clip.maxZ(),boundingBox.maxZ());wz++)for(int wy=low;wy<=boundingBox.maxY();wy++){var s=materialAt(wx,wy,wz);if(s!=null)l.setBlock(p.set(wx,wy,wz),s,2);}
    }
}
