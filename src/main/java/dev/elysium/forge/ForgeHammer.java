package dev.elysium.forge;

import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.ForgePlan;
import java.util.LinkedHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Authored head and telescoping ram. Every move is checked before any block changes. */
public final class ForgeHammer {
    public static BlockPos world(ForgePlan p,int u,int h,int v) {
        return new BlockPos(p.worldX(u,v),p.floor()+h,p.worldZ(u,v));
    }
    public static BlockState material(int u,int h,int v,int drop) {
        int du=Math.abs(u),dv=Math.abs(v-76),height=h+drop;
        if(du<=1&&dv<=1&&h>=18-drop&&h<=22)return Blocks.BASALT.defaultBlockState();
        if(dv<=2&&du<=3&&height>=15&&height<=17)
            return (height==15?Blocks.WAXED_EXPOSED_CUT_COPPER:Blocks.POLISHED_BLACKSTONE).defaultBlockState();
        if(dv<=(height>=13?3:2)&&du<=(height>=13?4:3)&&du+dv<=6&&height>=11&&height<=14)
            return (height==14?Blocks.CHISELED_DEEPSLATE:Blocks.POLISHED_BLACKSTONE).defaultBlockState();
        if(dv<=2&&du<=3&&height==10)return ModBlocks.FORGE_IRON.get().defaultBlockState();
        return null;
    }
    public static boolean move(ServerLevel level,ForgePlan plan,int oldDrop,int newDrop) {
        if(oldDrop<0||oldDrop>4||newDrop<0||newDrop>4)return false;
        var writes=new LinkedHashMap<BlockPos,BlockState>();
        for(int u=-4;u<=4;u++)for(int v=73;v<=79;v++)for(int h=6;h<=22;h++) {
            var old=material(u,h,v,oldDrop);var next=material(u,h,v,newDrop);
            if(old==null&&next==null)continue;
            var pos=world(plan,u,h,v);
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if(chunk==null)return false;
            var actual=chunk.getBlockState(pos);
            if(old!=null?!actual.equals(old):!actual.isAir())return false;
            if(!java.util.Objects.equals(old,next))writes.put(pos,next==null?Blocks.AIR.defaultBlockState():next);
        }
        // No player blocks are cleared, and missing head sections must be repaired before moving.
        writes.forEach((pos,state)->level.setBlock(pos,state,3));
        return true;
    }
    private ForgeHammer() {}
}
