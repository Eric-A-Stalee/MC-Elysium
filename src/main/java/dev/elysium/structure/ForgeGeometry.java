package dev.elysium.structure;

import dev.elysium.block.ForgeDebrisBlock;
import dev.elysium.block.ForgeServiceBlock;
import dev.elysium.registry.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

/** An intact working plan interrupted by coherent failures. Samples are independent of chunk order. */
public final class ForgeGeometry {
    private final ForgePlan plan;
    private final Map<Long, ForgeFixtures.Fixture> fixtures = new HashMap<>();
    public ForgeGeometry(ForgePlan plan) {
        this.plan=plan;
        for(var f:ForgeFixtures.ALL) fixtures.put(BlockPos.asLong(f.u(),f.h(),f.v()),f);
    }
    private static BlockState b(Block block) { return block.defaultBlockState(); }
    private Direction direction(Direction d) { for(int i=0;i<plan.turn();i++)d=d.getClockWise();return d; }
    private BlockState stair(Block block,Direction d) { return b(block).setValue(StairBlock.FACING,direction(d)); }
    private BlockState slab(Block block) { return b(block).setValue(SlabBlock.TYPE,SlabType.BOTTOM); }
    private BlockState pillar(Block block,Direction.Axis axis) {
        if(axis!=Direction.Axis.Y&&(plan.turn()&1)==1)axis=axis==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X;
        return b(block).setValue(RotatedPillarBlock.AXIS,axis);
    }
    private int noise(int u,int h,int v,int salt) {
        long n=(long)u*73428767L+(long)v*912931L+(long)h*19349663L+plan.key()+salt*83492791L;
        n=(n^(n>>>27))*0x3C79AC492BA7B653L;
        return (int)((n^(n>>>33))&0x7fffffff);
    }
    private BlockState debris(int kind) { return ModBlocks.FORGE_DEBRIS.get().defaultBlockState().setValue(ForgeDebrisBlock.KIND,kind); }

    /** Null leaves the mountain untouched; cave air deliberately excavates an authored space. */
    public BlockState at(int u,int y,int v) {
        int h=y-plan.floor();
        if(v>=ForgePlan.MIN_V&&v<=7&&y>=plan.entranceFloor()-3)return entrance(u,y,v);
        var descent=ForgeDescent.at(plan,u,h,v);if(descent!=null)return descent;
        int roof=plan.hallRoof(u,v),floor=plan.floor();
        if(roof==Integer.MIN_VALUE||y<floor-2||y>roof)return null;
        var fixture=fixtures.get(BlockPos.asLong(u,h,v));
        if(fixture!=null)return fixture.local().equals(ForgeFixtures.MISSING)?b(Blocks.CAVE_AIR):ForgeFixtures.state(plan,fixture);
        if(y<=floor)return floor(u,y,v,floor);
        if(u==0&&v==61&&h==1)return b(ModBlocks.FORGE_HEARTH.get());
        if(u==-37&&v==50&&h==1)return b(ModBlocks.FORGE_LEDGER.get());
        if(u==0&&v==112&&h==1)return b(ModBlocks.TARTARUS_INSCRIPTION.get());
        BlockState state=collapse(u,h,v);
        if(state!=null)return state;
        if(plan.edge(u,v)||y>=roof-1)return masonry(u,h,v);
        state=machinery(u,h,v);if(state!=null)return state;
        state=galleries(u,h,v);if(state!=null)return state;
        if(u==-42&&h==1&&v==60)return ModBlocks.KEEPER_WORKBENCH.get().defaultBlockState();
        state=workshops(u,h,v);if(state!=null)return state;
        state=rearShrine(u,h,v);if(state!=null)return state;
        // Low niches flank the last bend; the central approach has no lamp pillars.
        if((u==-5&&v==40||u==5&&v==45)&&h<=5)return masonry(u,h,v);
        
        if(v==39&&u>=-5&&u<=-3&&h<=2)return b(Blocks.CHISELED_DEEPSLATE);
        if(Math.abs(u)==24&&(v==64||v==76)&&h<=10)return b(Blocks.POLISHED_BASALT);
        // Surviving overhead duct connects the workshops; broken branches end in bare metal.
        if(v==60&&Math.abs(u)>=10&&Math.abs(u)<=40&&h==(Math.abs(u)<=24?12:6))
            return b(Math.floorMod(u,8)==0?Blocks.WAXED_OXIDIZED_CUT_COPPER:Blocks.WAXED_WEATHERED_CUT_COPPER);
        if(v==60&&Math.abs(u)==24&&h>=6&&h<=12)return b(Blocks.WAXED_WEATHERED_CUT_COPPER);
        if(Math.abs(u)==30&&v==60&&h>=3&&h<=6)return b(Blocks.WAXED_WEATHERED_CUT_COPPER);
        if(h==1&&Math.abs(u)==3&&v>=44&&v<=65&&!(v>=53&&v<=55))
            return b(Blocks.RAIL).setValue(RailBlock.SHAPE,(plan.turn()&1)==0?RailShape.NORTH_SOUTH:RailShape.EAST_WEST);
        if(h==1&&grime(u,v)&&noise(u,0,v,31)%5<2) {
            if(u>29&&v>65)return debris(2);
            if(u<-12&&v>75)return debris(1);
            return debris(0);
        }
        return b(Blocks.CAVE_AIR);
    }

    private BlockState entrance(int u,int y,int v) {
        if(Math.abs(u)>10)return null;
        int floor=plan.stairFloor(v),e=y-floor;
        if(e< -2||e>10)return null;
        int centre=ForgePlan.approachCenter(v);
        boolean route=u>=centre&&u<=centre+1;
        if(route) {
            if(v<0&&e<=0)return b(e<0?Blocks.STONE:noise(u,0,v,2)%3==0?Blocks.GRAVEL:Blocks.TUFF);
            if(v<0&&e==1&&noise(u,0,v,12)%3==0)return debris(3);
            if(e<=0)return e==0&&v>=7?stair(Blocks.COBBLED_DEEPSLATE_STAIRS,Direction.NORTH):b(Blocks.COBBLED_DEEPSLATE);
            if(e<=2)return b(Blocks.CAVE_AIR);
            if(v>=-1&&e==3)return b(Blocks.STONE);
        }
        // Only a broken jamb and short lintel survive the blocked original doorway.
        if(u==1&&v>=3&&v<=4&&e>=0&&e<=2)
            return b(e==2?Blocks.WAXED_OXIDIZED_CUT_COPPER:Blocks.CRACKED_DEEPSLATE_BRICKS);
        double r=(u-2.0)*(u-2.0)/100.0+(v-3.0)*(v-3.0)/121.0;
        int height=r<1?Math.max(0,(int)(8*(1-r))+noise(u/2,0,v/2,5)%2):0;
        // Roots follow the shallow breach only; they do not overgrow the deep foundry.
        if(u==4&&v>=1&&v<=6&&e==Math.max(1,height))return pillar(Blocks.DARK_OAK_LOG,Direction.Axis.Z);
        if(!route&&height>0&&e>=-1&&e<=height) {
            int patch=noise(Math.floorDiv(u,2),Math.floorDiv(e,2),Math.floorDiv(v,3),9)%9;
            return b(patch<4?Blocks.CALCITE:patch<7?Blocks.STONE:Blocks.TUFF);
        }
        if(!route&&height>0&&e==height+1&&noise(u,0,v,13)%5==0)return debris(3);
        if(v>=5&&Math.abs(u)<=4&&e>=4&&e<=6)return b(Blocks.STONE);
        return null;
    }

    private BlockState floor(int u,int y,int v,int floor) {
        if(y<floor)return b(Blocks.DEEPSLATE);
        if(u==-7&&v==52)return b(ModBlocks.FORGE_PILOT_LIGHT.get());
                if(v>=48&&v<=92&&(u==-6||u==6))return b(Blocks.WAXED_WEATHERED_COPPER_GRATE);
        if(grime(u,v)&&noise(Math.floorDiv(u,3),0,Math.floorDiv(v,4),3)%4!=0)
            return b(u>29&&v>65?Blocks.BLACKSTONE:Blocks.TUFF);
        if(Math.abs(u)<=4&&v>=44&&v<=65)return pillar(Blocks.BASALT,Direction.Axis.X);
        int patch=noise(Math.floorDiv(u,4),0,Math.floorDiv(v,5),7)%11;
        return b(patch<2?Blocks.CRACKED_DEEPSLATE_TILES:patch==2?Blocks.COBBLED_DEEPSLATE:Blocks.DEEPSLATE_TILES);
    }

    private boolean grime(int u,int v) { return u<-12&&v>74&&v<94||u>29&&v>65||u<-40&&v>54||Math.abs(u)>22||v>106; }

    private BlockState masonry(int u,int h,int v) {
        boolean damp=u< -17&&v>=78&&v<=91;
        if(damp&&(u+v)%5==0&&h>6)return b(h%5==0?Blocks.CALCITE:Blocks.DRIPSTONE_BLOCK);
        if(h==8&&!damp)return b(Blocks.WAXED_WEATHERED_CUT_COPPER);
        if(v%8==0)return b(damp?Blocks.BASALT:Blocks.POLISHED_BASALT);
        int patch=noise(Math.floorDiv(u,3),Math.floorDiv(h,3),Math.floorDiv(v,4),17)%13;
        if(damp)return b(patch<5?Blocks.TUFF:patch<8?Blocks.COBBLED_DEEPSLATE:Blocks.CRACKED_DEEPSLATE_BRICKS);
        if(u>39&&v>=66&&h>4)return b(patch<8?Blocks.BLACKSTONE:Blocks.BASALT);
        return b(patch<2?Blocks.CRACKED_DEEPSLATE_BRICKS:patch==2?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.DEEPSLATE_BRICKS);
    }

    private BlockState collapse(int u,int h,int v) {
        double r=(u+22.0)*(u+22.0)/144+(v-82.0)*(v-82.0)/169;
        int height=r<1?(int)(11*Math.pow(1-r,1.35)):0;
        // The raw mountain protrudes through the failed vault above the matching rubble fan.
        if(r<0.38&&h>=plan.roof(u,v)-plan.floor()-3)
            return b(noise(u/2,h/2,v/2,23)%4==0?Blocks.CALCITE:Blocks.STONE);
        if(height>0&&h<=height) {
            int patch=noise(Math.floorDiv(u,2),Math.floorDiv(h,2),Math.floorDiv(v,2),21)%9;
            return b(patch<3?Blocks.STONE:patch<5?Blocks.CALCITE:patch<7?Blocks.COBBLED_DEEPSLATE:Blocks.TUFF);
        }
        if(height>0&&h==height+1&&noise(u,h,v,27)%3==0)return debris(1);
        if(u>=-22&&u<=-15&&v>=77&&v<=78&&h>=1&&h<=2)
            return b(u%5==0?Blocks.WAXED_OXIDIZED_CUT_COPPER:Blocks.POLISHED_BASALT);
        return null;
    }

    private BlockState machinery(int u,int h,int v) {
        int du=Math.abs(u),dv=Math.abs(v-76);
        // Footings, guide piers, corbels and an overhead crosshead carry the load.
        if(du>=15&&du<=19&&dv<=3&&h<=2)return b(Blocks.CHISELED_DEEPSLATE);
        if(du>=16&&du<=18&&dv<=2&&h<=23)
            return b(h==7||h==15||h>=21?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.POLISHED_BASALT);
        if(du>=13&&du<=18&&dv<=2&&h>=19&&h<=22&&du>=35-h)return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
        if(du<=18&&dv<=2&&h>=23&&h<=24)return b(h==24?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.CHISELED_DEEPSLATE);
        if(du==6&&dv<=1&&h>=2&&h<=22)return b(h%5==0?Blocks.WAXED_EXPOSED_CUT_COPPER:ModBlocks.FORGE_IRON.get());
        if(du<=1&&dv<=1&&h>=17&&h<=22)return pillar(Blocks.BASALT,Direction.Axis.Y);
        if(heavyChain(u-4,h,v-76)||heavyChain(u+4,h,v-76))return b(ModBlocks.TARTARUS_CHAIN_BLOCK.get());
        // Neck, shouldered body and a replaceable striking face give the hammer a readable silhouette.
        if(dv<=2&&du<=3&&h>=15&&h<=17)return b(h==15?Blocks.WAXED_EXPOSED_CUT_COPPER:Blocks.POLISHED_BLACKSTONE);
        if(dv<=(h>=13?3:2)&&du<=(h>=13?4:3)&&du+dv<=6&&h>=11&&h<=14)
            return b(h==14?Blocks.CHISELED_DEEPSLATE:Blocks.POLISHED_BLACKSTONE);
        if(dv<=2&&du<=3&&h==10)return b(ModBlocks.FORGE_IRON.get());
        if(du<=6&&dv<=5&&h<=1)return b(Blocks.POLISHED_BLACKSTONE_BRICKS);
        if(du<=4&&dv<=3&&h==2)return b(Blocks.CHISELED_DEEPSLATE);
        if(du<=2&&dv<=2&&h>=3&&h<=4)return b(Blocks.POLISHED_BASALT);
        if(du<=5&&dv<=3&&h==5)return b(ModBlocks.FORGE_IRON.get());
        if(u>=6&&u<=8&&dv<=1&&h==5)return slab(Blocks.CUT_COPPER_SLAB);
        // An unfinished link rests in its cradle; its unclosed shoulder is deliberate.
        int a=Math.abs(u-11),z=Math.abs(v-69),vertical=Math.abs(h-5);
        boolean outer=a<=5&&vertical<=4&&a+vertical<=8;
        boolean inner=a<=3&&vertical<=2&&a+vertical<=4;
        if(z<=1&&outer&&!inner&&!(u>=14&&h>=7))return b(ModBlocks.TARTARUS_CHAIN_BLOCK.get());
        if(u>=5&&u<=17&&v>=67&&v<=71&&h==1)return b(u%5==0?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.POLISHED_BLACKSTONE_BRICKS);
        if((u==6||u==16)&&v>=67&&v<=71&&h==2)return b(Blocks.WAXED_WEATHERED_CUT_COPPER);
        // Casting is separate from striking: a cold, slag-filled crucible sits beside the anvil.
        int cu=Math.abs(u+10),cv=Math.abs(v-85);
        if(cu<=4&&cv<=4&&cu+cv<=6) {
            if(h==1)return b(Blocks.BLACKSTONE);
            if(h<=3&&(cu>=3||cv>=3))return b(h==3?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            if(h==2)return debris(2);
        }
        return null;
    }

    private boolean heavyChain(int u,int h,int v) {
        if(h<18||h>22)return false;
        int phase=(h-18)%4;
        return (phase==0||phase==3)?Math.abs(u)<=1&&v==0:Math.abs(u)==1&&v==0;
    }

    private BlockState galleries(int u,int h,int v) {
        int du=Math.abs(u);
        if(du>=20&&du<=22&&v>=56&&v<=64&&h<=v-56)
            return h==v-56?stair(Blocks.DEEPSLATE_BRICK_STAIRS,Direction.SOUTH):b(Blocks.DEEPSLATE_BRICKS);
        boolean broken=u<0&&v>=78;
        if(du>=20&&du<=24&&v>=64&&v<=85&&h==8&&!broken)return b(Blocks.DEEPSLATE_TILES);
        if(du==20&&v>=66&&v<=85&&h==9&&!broken)return b(Blocks.POLISHED_DEEPSLATE_WALL);
        if(du==24&&v>=61&&v<=76&&v%8==2&&h<=10)return b(h==8?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.POLISHED_BASALT);
        if(du>=22&&du<=24&&v>=64&&v<=84&&v%8==4&&h>=5&&h<8&&du>=29-h)return b(Blocks.DEEPSLATE_BRICKS);
        if(u==24&&v>=81&&v<=84&&h==9)return b(Blocks.WAXED_OXIDIZED_COPPER_GRATE);
        return null;
    }

    private BlockState workshops(int u,int h,int v) {
        if(u>=-44&&u<=-30&&v>=48&&v<=80) {
            if((u==-44||u==-30)&&v<=54&&h<=5)return b(h==3?Blocks.DARK_OAK_SLAB:Blocks.BOOKSHELF);
            if(u>=-40&&u<=-35&&v>=49&&v<=50&&h==1)return b(Blocks.DARK_OAK_PLANKS);
            if(u>=-44&&u<=-42&&v>=56&&v<=78&&h==1)return b(v%5==0?Blocks.SMITHING_TABLE:Blocks.DARK_OAK_PLANKS);
            if(u==-43&&v>=56&&v<=78&&h==2)return slab(Blocks.DARK_OAK_SLAB);
            if(u==-44&&v>=56&&v<=78&&h>=3&&h<=5)return b(v%5==2?Blocks.WAXED_OXIDIZED_COPPER_GRATE:Blocks.DARK_OAK_LOG);
            if(u==-42&&v>=65&&v<=76&&v%5==0&&h==2)return b(Blocks.CHIPPED_ANVIL).setValue(AnvilBlock.FACING,direction(Direction.WEST));
            if(u>=-32&&u<=-30&&v>=66&&v<=79&&h==1)return b(Blocks.SMOOTH_BASALT);
            if(u==-31&&v>=66&&v<=79&&h==2&&v%4!=0)return slab(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB);
            if(u==-30&&v>=66&&v<=79&&h>=2&&h<=4)return b(v%4==0?Blocks.DARK_OAK_LOG:Blocks.WAXED_WEATHERED_COPPER_GRATE);
            if(u>=-38&&u<=-35&&v>=68&&v<=76&&h==1)return b(Blocks.POLISHED_BLACKSTONE);
            if(u>=-38&&u<=-35&&v>=68&&v<=76&&h==2&&(u==-38||u==-35||v%4==0))return slab(Blocks.POLISHED_BLACKSTONE_SLAB);
            if(u==-44&&v==79&&h<=4)return b(Blocks.COBBLED_DEEPSLATE);
            if(u==-43&&v==79&&h<=2)return b(Blocks.TUFF);
        }
        if(u>=30&&u<=44&&v>=55&&v<=87) {
            if(u>=33&&u<=35&&v==67&&h<=2)return b(Blocks.CHISELED_DEEPSLATE);
            if(u>=40&&v>=66&&v<=85) {
                if(h==1)return b(Blocks.DEEPSLATE_BRICKS);
                if(u==40&&h>=2&&h<=3&&(v==69||v==75||v==81))return b(Blocks.BLAST_FURNACE).setValue(AbstractFurnaceBlock.FACING,direction(Direction.WEST));
                if(h>=2&&h<=5)return b(v%6==3&&u==41?Blocks.BLACKSTONE:Blocks.CRACKED_DEEPSLATE_BRICKS);
                if(h==6&&u>=41||h==7&&u>=42||h>=8&&h<=10&&u==43)return b(h>=8?Blocks.BASALT:Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            if(u>=31&&u<=35&&v>=74&&v<=84) {
                boolean rim=u==31||u==35||v==74||v==84;
                if(h==1)return b(rim?Blocks.DEEPSLATE_BRICKS:Blocks.WATER);
                if(h==2&&rim)return slab(Blocks.POLISHED_DEEPSLATE_SLAB);
            }
            if(u>=37&&u<=38&&v>=73&&v<=85&&h==1)return b(Blocks.POLISHED_BLACKSTONE);
            if(u==38&&v>=73&&v<=85&&v%3==0&&h==2)return b(Blocks.CAULDRON);
            if(u>=40&&v>=56&&v<=58&&h<=2)return b(h==2?Blocks.COAL_BLOCK:Blocks.DEEPSLATE_BRICKS);
        }
        return null;
    }

    private BlockState rearShrine(int u,int h,int v) {
        int du=Math.abs(u);
        if(v>=105&&v<=110&&du<=4&&h<=4) {
            if(h==4&&du<=2&&v>=106&&v<=109)return b(Blocks.CAVE_AIR);
            return b(h==4?ModBlocks.FORGE_IRON.get():Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        }
        if(du==7&&v==107&&h<=5)return b(h==4?Blocks.WAXED_WEATHERED_CUT_COPPER:Blocks.POLISHED_BASALT);
        if(u==-3&&v==101&&h==2)return b(ModBlocks.FORGE_PILOT_LIGHT.get());
        if(u==-3&&v==101&&h==1)return b(Blocks.CHISELED_DEEPSLATE);
        return null;
    }
}
