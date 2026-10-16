package dev.elysium.structure;

import dev.elysium.block.DepthsControlBlockEntity;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** A bounded burnt basin, its inspection descent and its dark sealed chamber. */
public final class TartarusDepthsPiece extends StructurePiece {
    private final TartarusDepthsPlan plan;
    public TartarusDepthsPiece(TartarusDepthsPlan p) {
        super(ModStructures.TARTARUS_DEPTHS_PIECE.get(),0,new BoundingBox(p.x+TartarusDepthsPlan.MIN_X,-245,p.z+TartarusDepthsPlan.MIN_Z,
                p.x+TartarusDepthsPlan.MAX_X,-43,p.z+TartarusDepthsPlan.MAX_Z));
        plan=p;setOrientation(null);
    }
    public TartarusDepthsPiece(CompoundTag tag){this(new TartarusDepthsPlan(tag.getInt("X"),tag.getInt("Z"),tag.getLong("Seed")));}
    public TartarusDepthsPlan plan(){return plan;}
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.putInt("X",plan.x);tag.putInt("Z",plan.z);tag.putLong("Seed",plan.seed);
    }
    private static BlockState b(Block block){return block.defaultBlockState();}
    public BlockState materialAt(int u,int y,int v){return materialAt(u,y,v,plan.floor(u,v),plan.roof(u,v),plan.basinRadius(u,v),plan.chamberRadius(u,v),plan.path(u,v));}
    private BlockState materialAt(int u,int y,int v,int floor,int roof,double basin,double chamber,TartarusDepthsPlan.PathCell path) {
        if(u==-3&&y==-183&&v==188)return b(ModBlocks.TARTARUS_COUNTERWEIGHT.get());
        if(u==116&&y==-231&&v==246)return b(ModBlocks.TARTARUS_HEART.get());
        if(u>=-4&&u<=-2&&v>=187&&v<=189&&y>=floor&&y<=-184)return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
        // The missing deck survives at the bottom of the drop, held on old chains.
        if(u>=6&&u<=22&&v>=189&&v<=191&&y==-200)return fallenDeck(v);
        if((u==7||u==21)&&(v==189||v==191)&&y>=-199&&y<=-179)return b(Blocks.CHAIN);
        if(u>=5&&u<=23&&(v==189||v==191)&&y==-178)return b(ModBlocks.FORGE_IRON.get());
        if((u==5||u==23)&&(v==188||v==192)&&y>=-184&&y<=-178)return b(ModBlocks.FORGE_IRON.get());
        if(u>=-2&&u<=7&&v==188&&y==-183)return b(Blocks.CHAIN).setValue(net.minecraft.world.level.block.ChainBlock.AXIS,net.minecraft.core.Direction.Axis.X);
        if(TartarusDepthsPlan.bridgeGap(u,v)) {
            if(y>=-203&&y<=-179)return b(Blocks.CAVE_AIR);
            floor=Math.min(floor,-204);
        }
        // The passage cuts the old vault wall and joins the basin without a teleport.
        if(path!=null&&!TartarusDepthsPlan.bridgeGap(u,v)) {
            if(path.distance()<=1&&v>=143&&y<path.floor()-1&&y>=(chamber<1?TartarusDepthsPlan.BOSS_FLOOR:floor))return stone(u,y,v);
            if(path.distance()<=1&&y>=path.floor()-1&&y<=path.floor()+4) {
                if(y<=path.floor())return b(plan.hash(u,y,v)%7==0?Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BLACKSTONE_BRICKS);
                if(path.distance()==1&&y==path.floor()+1&&plan.hash(u,0,v)%23==0)return b(ModBlocks.FORGE_DEBRIS.get());
                return b(Blocks.CAVE_AIR);
            }
            if(path.distance()<=2&&y>=path.floor()-2&&y<=path.floor()+5&&((basin>1&&chamber>1.10)||v<143)) {
                if(y<=path.floor()||y==path.floor()+5||path.distance()==2)return stone(u,y,v);
                return b(Blocks.CAVE_AIR);
            }
        }
        if(plan.lowerChain(u,y,v))return b(ModBlocks.TARTARUS_CHAIN_BLOCK.get());
        // Boss chamber: no glowstone, lanterns or maintained galleries.
        if(chamber<=1.10&&y>=TartarusDepthsPlan.BOSS_FLOOR-2) {
            int cap=-208+(int)Math.round(30*Math.sqrt(Math.max(0,1-chamber)));
            if(y<=cap+2) {
                if(y<=TartarusDepthsPlan.BOSS_FLOOR||chamber>=.96||y>=cap) {
                    if(y==TartarusDepthsPlan.BOSS_FLOOR&&plan.hash(u,0,v)%19==0)return b(Blocks.BASALT);
                    return stone(u,y,v);
                }
                int a=u-TartarusDepthsPlan.BOSS_X,c=v-TartarusDepthsPlan.BOSS_Z;
                if((Math.abs(a)>25&&Math.abs(c%17)<=1||Math.abs(c)>28&&Math.abs(a%19)<=1)&&y<-186)return b(ModBlocks.FORGE_IRON.get());
                if(Math.abs(c)<=1&&Math.abs(a)>=17&&Math.abs(a)<=30&&Math.abs(y-(-220+Math.abs(a)/3))<=1)
                    return b(ModBlocks.TARTARUS_CHAIN_BLOCK.get());
                if(chamber>.72&&plan.hash(u,0,v)%11==0&&y<=TartarusDepthsPlan.BOSS_FLOOR+1)return stone(u,y,v);
                return b(Blocks.CAVE_AIR);
            }
        }
        if(basin>1.03||y<floor-3||y>roof+2)return null;
        if(y<=floor||y>=roof||basin>=.98) {
            if(y==floor&&floor<-215&&plan.hash(u,0,v)%83==0)return b(ModBlocks.SMOULDERING_CLINKER.get());
            if(y==floor&&plan.hash(Math.floorDiv(u,5),0,Math.floorDiv(v,5))%17==0)return b(ModBlocks.CINDER_ASH.get());
            return stone(u,y,v);
        }
        // Sparse raw columns and rubble; the walk route above always wins.
        int cx=Math.floorMod(u+23,43)-21,cz=Math.floorMod(v+11,47)-23;
        double column=(double)(cx*cx+cz*cz);
        if(column<7+(roof-y)*.12&&y<=roof)return stone(u,y,v);
        if(y==floor+1&&plan.hash(u,0,v)%31==0)return b(ModBlocks.FORGE_DEBRIS.get());
        // A hanging lateral chain across the fracture, far above the broken foot crossing.
        if(Math.abs(v-221)<=2&&u>=-28&&u<=44) {
            int cy=-147-(int)Math.round(7*Math.sin((u+28)*Math.PI/72));
            int ring=Math.floorMod(u+28,7),dy=y-cy;
            if((ring==0||ring==6)&&Math.abs(dy)<=2&&v==221||ring>0&&ring<6&&Math.abs(dy)==2&&v==221)
                return b(ModBlocks.TARTARUS_CHAIN_BLOCK.get());
        }
        return b(Blocks.CAVE_AIR);
    }
    public static BlockState fallenDeck(int v) {
        return b(v==190?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.WAXED_OXIDIZED_COPPER_GRATE);
    }
    private BlockState stone(int u,int y,int v) {
        int h=plan.hash(Math.floorDiv(u,3),Math.floorDiv(y,3),Math.floorDiv(v,3))%16;
        return b(h<9?ModBlocks.BURNT_CLINKER.get():h<13?Blocks.BASALT:h<15?Blocks.BLACKSTONE:Blocks.OBSIDIAN);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        for(int x=Math.max(clip.minX(),boundingBox.minX());x<=Math.min(clip.maxX(),boundingBox.maxX());x++)
            for(int z=Math.max(clip.minZ(),boundingBox.minZ());z<=Math.min(clip.maxZ(),boundingBox.maxZ());z++) {
                int u=x-plan.x,v=z-plan.z,floor=plan.floor(u,v),roof=plan.roof(u,v);
                double basin=plan.basinRadius(u,v),chamber=plan.chamberRadius(u,v);var path=plan.path(u,v);
                if(basin>1.03&&chamber>1.10&&path==null&&!(Math.abs(u)<=2&&v>=TartarusDepthsPlan.ANCHOR_Z&&v<=221))continue;
                for(int y=Math.max(clip.minY(),boundingBox.minY());y<=Math.min(clip.maxY(),boundingBox.maxY());y++) {
                    var state=materialAt(u,y,v,floor,roof,basin,chamber,path);
                    if(state!=null)placeBlock(level,state,x,y,z,clip);
                }
            }
        for(var pos:new BlockPos[]{plan.bridgeControl(),plan.heart()})
            if(clip.isInside(pos)&&level.getBlockEntity(pos) instanceof DepthsControlBlockEntity control)control.bind(plan);
    }
}
