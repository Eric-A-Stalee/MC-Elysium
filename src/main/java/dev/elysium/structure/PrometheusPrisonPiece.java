package dev.elysium.structure;

import dev.elysium.block.PrometheusBlockEntity;
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

/** The living prisoner is part of the rock: a monumental body, broken restraints and a fighting ledge. */
public final class PrometheusPrisonPiece extends StructurePiece {
    private final int x,y,z,low;private final long seed;private final int[] approach;
    public PrometheusPrisonPiece(int x,int y,int z,int low,long seed,int[] approach){
        super(ModStructures.PROMETHEUS_PRISON_PIECE.get(),0,new BoundingBox(x-45,low,z-36,x+44,y+86,z+96));
        if(approach.length!=64)throw new IllegalArgumentException("Invalid prison approach");
        this.x=x;this.y=y;this.z=z;this.low=low;this.seed=seed;this.approach=approach.clone();setOrientation(null);
    }
    public PrometheusPrisonPiece(CompoundTag tag){this(tag.getInt("X"),tag.getInt("Y"),tag.getInt("Z"),tag.getInt("Low"),tag.getLong("Seed"),tag.getIntArray("Approach"));}
    public BlockPos arena(){return new BlockPos(x+19,y+7,z+15);}
    public BlockPos voice(){return new BlockPos(x-3,y+9,z+5);}
    public BlockPos approach(){return new BlockPos(x+19,y+approach[63]+1,z+96);}
    public java.util.List<BlockPos> approachRoute(){return java.util.stream.IntStream.range(0,64).mapToObj(i->new BlockPos(x+19,y+approach[i],z+33+i)).toList();}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("X",x);t.putInt("Y",y);t.putInt("Z",z);t.putInt("Low",low);t.putLong("Seed",seed);t.putIntArray("Approach",approach);}
    private static boolean ellipsoid(double a,double b,double c,double rx,double ry,double rz){return a*a/(rx*rx)+b*b/(ry*ry)+c*c/(rz*rz)<=1;}
    private int hash(int u,int h,int v){long n=seed+u*73428767L+v*912931L+h*438289L;n=(n^(n>>>31))*0x9E3779B97F4A7C15L;return (int)(n^(n>>>32))&0x7fffffff;}
    public BlockState materialAt(int wx,int wy,int wz) {
        if(!boundingBox.isInside(wx,wy,wz))return null;
        int u=wx-x,h=wy-y,v=wz-z,n=hash(u,h,v);double r=Math.hypot(u-19,v-15);
        if(wx==voice().getX()&&wy==voice().getY()&&wz==voice().getZ())return ModBlocks.PROMETHEUS_VOICE.get().defaultBlockState();
        // The broad arena is deliberately interrupted at its rim, but its centre stays usable for a grounded player.
        if(r<=19&&!(v>=33&&Math.abs(u-19)<=2)) {
            if(h<=6&&h>=Math.max(low-y,-35+(int)r))return (h==6?(n%9==0?Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BLACKSTONE_BRICKS):n%6==0?Blocks.BASALT:Blocks.BLACKSTONE).defaultBlockState();
            if(h>6&&h<12&&r<17)return Blocks.CAVE_AIR.defaultBlockState();
            if(r>17.4&&r<18.8&&h==7&&Math.floorMod((int)(Math.atan2(v-15,u-19)*10),9)<5)return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        }
        // Narrow approach and a rising side stair to the lowered hand. Their footings disappear into the outcrop.
        if(v>=33&&v<=96&&Math.abs(u-19)<=2) {
            int floor=approach[v-33];
            if(h<=floor&&h>=low-y)return (h==floor?Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS:Blocks.BASALT).defaultBlockState();
            if(h>floor&&h<=floor+3)return Blocks.CAVE_AIR.defaultBlockState();
        }
        if(u>=-6&&u<=4&&Math.abs(v-5)<=2) {
            int floor=6+Math.max(0,(4-u)/3);
            if(h<=floor&&h>=low-y)return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
            if(h>floor&&h<=floor+3)return Blocks.CAVE_AIR.defaultBlockState();
        }
        // Tarnished cuffs and vertical shackles. Alternating link planes remain legible in silhouette.
        for(int anchor:new int[]{-40,1}) {
            if(Math.abs(u-anchor)<=4&&Math.abs(v+12)<=4&&h>=19&&h<=23)return ModBlocks.FORGE_IRON.get().defaultBlockState();
            if(h>23&&h<84) {
                int a=(h/12&1)==0?u-anchor:v+12,p=(h/12&1)==0?v+12:u-anchor,k=Math.floorMod(h-23,12)-6;
                double ring=a*a/12.0+k*k/36.0;
                if(Math.abs(p)<=1&&ring>=.50&&ring<=1.40)return ModBlocks.TARTARUS_CHAIN_BLOCK.get().defaultBlockState();
            }
        }
        // Head, slumped shoulders, embedded chest, massive forearms and knees have unequal centres.
        boolean head=ellipsoid(u+22,h-39,v+15,6,7,6);
        boolean body=ellipsoid(u+20,h-24,v+20,12,17,8);
        boolean shoulder=ellipsoid(u+20,h-29,v+17,20,5,6);
        boolean arm=(u>=-41&&u<=2)&&ellipsoid(0,h-(20+Math.abs(u+20)*.12),v+11,1,4,4);
        boolean knees=ellipsoid(u+27,h-7,v+8,6,9,7)||ellipsoid(u+12,h-8,v+12,6,10,7);
        if(head||body||shoulder||arm||knees) {
            if(head&&h>=39&&(v<-15||h>43))return Blocks.BASALT.defaultBlockState();
            if(head&&h==40&&v>=-10&&(u==-25||u==-20))return ModBlocks.SMOULDERING_CLINKER.get().defaultBlockState();
            if(h>17&&h<28&&v>-15&&Math.floorMod(u*3+h,19)==0)return ModBlocks.SMOULDERING_CLINKER.get().defaultBlockState();
            return (n%17==0?Blocks.CRACKED_DEEPSLATE_TILES:n%5==0?Blocks.TUFF:Blocks.DEEPSLATE).defaultBlockState();
        }
        if(u>=-23&&u<=-21&&h>=35&&h<=39&&v>=-10&&v<=-7)return Blocks.DEEPSLATE.defaultBlockState();
        // The eagle's ruined gantry is not a complete symmetrical doorway.
        if((Math.abs(u-1)<=2&&Math.abs(v-15)<=2&&h>=7&&h<=34)||(Math.abs(u-38)<=2&&Math.abs(v-15)<=2&&h>=7&&h<=29))
            return (h%7==0?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.DEEPSLATE_BRICKS).defaultBlockState();
        if(h>=30&&h<=32&&u>=0&&u<=31&&Math.abs(v-15)<=2)return (h==31?ModBlocks.FORGE_IRON.get():Blocks.WAXED_EXPOSED_CUT_COPPER).defaultBlockState();
        // Jagged rock embraces the body. No clearing of the surrounding terrain or artificial rectangular room.
        double cliff=1-Math.pow((u+20)/27.0,2)-Math.pow((v+25)/15.0,2);
        if(cliff>0&&h<=56*cliff+4*Math.sin(u*.6+v*.4)&&h>=low-y)return (n%8==0?Blocks.BASALT:Blocks.DEEPSLATE).defaultBlockState();
        if(h>=low-y&&h<=0&&r<23)return Blocks.BLACKSTONE.defaultBlockState();
        return null;
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        var pos=new BlockPos.MutableBlockPos();
        for(int wx=Math.max(clip.minX(),boundingBox.minX());wx<=Math.min(clip.maxX(),boundingBox.maxX());wx++)
            for(int wz=Math.max(clip.minZ(),boundingBox.minZ());wz<=Math.min(clip.maxZ(),boundingBox.maxZ());wz++)
                for(int wy=boundingBox.minY();wy<=boundingBox.maxY();wy++){var state=materialAt(wx,wy,wz);if(state!=null)level.setBlock(pos.set(wx,wy,wz),state,2);}
        if(clip.isInside(voice())&&level.getBlockEntity(voice()) instanceof PrometheusBlockEntity control)control.bind(arena());
    }
}
