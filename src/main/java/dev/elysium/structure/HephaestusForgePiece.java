package dev.elysium.structure;

import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Immutable geometry; each call writes only its chunk, including all supports and furnishings. */
public final class HephaestusForgePiece extends StructurePiece {
    private final ForgePlan plan;
    public HephaestusForgePiece(ForgePlan plan) {
        super(ModStructures.HEPHAESTUS_FORGE_PIECE.get(),0,bounds(plan));this.plan=plan;setOrientation(null);
    }
    public HephaestusForgePiece(CompoundTag tag) {
        this(new ForgePlan(tag.getInt("X"),tag.getInt("Z"),tag.getInt("EntranceFloor"),tag.getInt("Turn")));
        if(tag.getInt("Version")!=1) throw new IllegalArgumentException("Unknown Forge geometry version");
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
        tag.putInt("Version",1);tag.putInt("X",plan.x());tag.putInt("Z",plan.z());
        tag.putInt("EntranceFloor",plan.entranceFloor());tag.putInt("Turn",plan.turn());
    }
    private BlockState block(Block b) { return b.defaultBlockState(); }
    private BlockState stair(Direction local) {
        for(int i=0;i<plan.turn();i++) local=local.getClockWise();
        return Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,local);
    }
    private BlockState lamp() { return Blocks.WAXED_COPPER_BULB.defaultBlockState().setValue(BlockStateProperties.LIT,true); }

    /** Null is deliberately untouched geology, not air. Local Y is absolute world Y. */
    public BlockState materialAt(int u,int y,int v) {
        int base=plan.floor(),h=y-base;
        // A modest, asymmetrical surface portal, with the same copper emblem as the archive.
        if(v>=-5 && v<=3 && Math.abs(u)<=6) {
            int e=y-plan.entranceFloor();
            if(e>=-2 && e<=0) return block(e==0?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS);
            if(e>=1 && e<=9) {
                if(Math.abs(u)>=5 && v>=-1) return block(e==5?Blocks.WAXED_CUT_COPPER:Blocks.DEEPSLATE_BRICKS);
                if(e==8 && v>=-1 || e==9 && v>=0 && Math.abs(u)<=4) return block(Blocks.DEEPSLATE_TILES);
                if(v==-2 && (u==-5&&e<=5 || u==5&&e<=2))return block(Blocks.POLISHED_BASALT);
                if(u==-5 && e==6 && v==-2) return lamp();
                if(u==5 && e==3 && v==-2) return block(ModBlocks.FORGE_ARCHIVE.get());
                if(Math.abs(u)<=4 && e<=7) return block(Blocks.AIR);
            }
            return null;
        }
        int roof=plan.roof(u,v),floor=plan.floorAt(u,v);
        if(roof==Integer.MIN_VALUE || y<floor-2 || y>roof) return null;
        boolean edge=plan.edge(u,v);
        if(y<=floor) {
            if(y==floor && v>=7 && v<=32 && Math.abs(u)<4) return stair(Direction.NORTH);
            if(y<floor) return block(Blocks.DEEPSLATE_BRICKS);
            // Cold runner channels remain shallow and traversable, capped with aged copper grating.
            if(v>=50 && v<=92 && (Math.abs(u)==7 || Math.abs(u)==8)) return block(Blocks.WAXED_EXPOSED_COPPER_GRATE);
            if(Math.abs(u)<=2 && v>=38 && v<=108) return block(Blocks.POLISHED_BASALT);
            return block((Math.floorMod(u*31+v*17,19)==0)?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_TILES);
        }
        if(edge || y>=roof-1) {
            if(edge && h==5 && v>=40 && Math.floorMod(v,12)==0) return lamp();
            if(v%8==0 || h==8) return block(h==8?Blocks.WAXED_EXPOSED_CUT_COPPER:Blocks.POLISHED_BASALT);
            return block(Math.floorMod(u*17+v*11+y,29)==0?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_BRICKS);
        }
        // Regular ribs give the descent a workshop-built rhythm; the full centre stays walkable.
        if(v>=8 && v<=32 && v%6==0 && (Math.abs(u)==3 || y>=roof-2)) return block(Blocks.POLISHED_BASALT);
        if(v>=8 && v<=32 && v%6==3 && u==-3 && y==floor+3) return lamp();

        if(u==0 && v==61 && h==1) return block(ModBlocks.FORGE_HEARTH.get());
        if(u==-37 && v==50 && h==1) return block(ModBlocks.FORGE_LEDGER.get());
        if(u==0 && v==112 && h==1) return block(ModBlocks.TARTARUS_INSCRIPTION.get());

        // The foundry's central crucible and the missing heart: no lava and no activation timer.
        int du=Math.abs(u),dv=Math.abs(v-76);
        if(du<=2 && v==68 && h==1)return stair(Direction.SOUTH);
        if(du<=6 && dv<=7) {
            if(h<=2) return block(du>=5 || dv>=6?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BASALT);
            if(h<=5 && (du==6 || dv==7) && !(v==69 && du<=2)) return block(h==5?Blocks.WAXED_CUT_COPPER:Blocks.CHISELED_DEEPSLATE);
            if(h==3 && du<=2 && dv<=2) return block(Blocks.CRYING_OBSIDIAN);
        }
        // Overhead gantry, two suspended chains, and a monumental stationary forging hammer.
        if(v>=74 && v<=78 && du<=20 && h>=23 && h<=24) return block(du>=18?Blocks.POLISHED_BASALT:Blocks.WAXED_EXPOSED_CUT_COPPER);
        if(du==2 && v==76 && h>=15 && h<=22) return block(Blocks.CHAIN);
        if(du<=4 && dv<=2 && h>=12 && h<=14) return block(h==12?Blocks.IRON_BLOCK:Blocks.POLISHED_BLACKSTONE);
        if(du>=18 && du<=19 && v>=74 && v<=78 && h<=23) return block(h%6==0?Blocks.WAXED_CUT_COPPER:Blocks.POLISHED_BASALT);

        // Two straight flights reach the side galleries. Rail openings match the stair landings.
        if(du>=19 && du<=21 && v>=55 && v<=63 && h<=v-55) {
            return h==v-55?stair(Direction.SOUTH):block(Blocks.DEEPSLATE_BRICKS);
        }
        if(du>=19 && du<=24 && v>=63 && v<=82 && h==8) return block(Blocks.DEEPSLATE_TILES);
        if(du==19 && v>=65 && v<=82 && h==9) return block(Blocks.POLISHED_DEEPSLATE_WALL);
        // Fixtures stand on full supports; no neighbour-dependent attachment can fall during clipping.
        if(du==23 && v>=63 && v<=79 && v%8==7 && h==9) return lamp();
        if(du==12 && (v==50 || v==89) && h<=3) return h==3?lamp():block(Blocks.CHISELED_DEEPSLATE);
        if((du==3 && (v==38 || v==44 || v==98 || v==102)
                || du==4 && v==58 || du==9 && v==76 || du==32 && v==60) && h<=3)
            return h==3?lamp():block(Blocks.CHISELED_DEEPSLATE);

        // West: archive alcove, anvils and work benches. East: bellows, retorts and quench trough.
        if(u<=-30 && u>=-44 && v>=48 && v<=80) {
            if((u==-44 || u==-30) && v<55 && h<=3) return block(Blocks.BOOKSHELF);
            if(u==-42 && v>=60 && v<=76 && v%6==0 && h==1) return block(Blocks.SMITHING_TABLE);
            if(u==-32 && v>=60 && v<=76 && v%6==0 && h==1) return block(Blocks.ANVIL);
            if(u==-37 && v==77 && h<=2) return h==2?lamp():block(Blocks.WAXED_CUT_COPPER);
            if(u==-42 && v==56 && h==1) return block(Blocks.CRAFTING_TABLE);
        }
        if(u>=30 && u<=44 && v>=55 && v<=87) {
            if(u>=40 && v>=68 && v<=76 && h<=2) return block(h==2?Blocks.WAXED_EXPOSED_COPPER_GRATE:Blocks.DEEPSLATE_BRICKS);
            if((u==33 || u==36) && (v==78 || v==82) && h==1)
                return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3);
            if(u==43 && v>=57 && v<=65 && v%3==0 && h<=2) return block(Blocks.BLAST_FURNACE);
            if(u==37 && v==84 && h<=2) return h==2?lamp():block(Blocks.WAXED_CUT_COPPER);
        }
        // Rear power shrine: an empty socket, iron braces, and a readable account of the bindings.
        if(v>=105 && v<=110 && du<=4 && h<=4) {
            if(h==4 && du<=2 && v>=106 && v<=109) return block(Blocks.AIR);
            return block(h==4?Blocks.IRON_BLOCK:Blocks.CHISELED_POLISHED_BLACKSTONE);
        }
        if(du==7 && v==107 && h<=5) return h==5?lamp():block(Blocks.POLISHED_BASALT);
        return block(Blocks.CAVE_AIR);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,
            RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        for(int x=Math.max(clip.minX(),boundingBox.minX());x<=Math.min(clip.maxX(),boundingBox.maxX());x++)
            for(int z=Math.max(clip.minZ(),boundingBox.minZ());z<=Math.min(clip.maxZ(),boundingBox.maxZ());z++)
                for(int y=Math.max(clip.minY(),boundingBox.minY());y<=Math.min(clip.maxY(),boundingBox.maxY());y++) {
                    var state=materialAt(plan.localU(x,z),y,plan.localV(x,z));
                    if(state!=null)placeBlock(level,state,x,y,z,clip);
                }
    }
}
