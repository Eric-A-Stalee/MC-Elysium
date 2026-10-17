package dev.elysium.gametest;

import dev.elysium.registry.ModEntities;
import dev.elysium.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class DeepEcologyGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void blindHungersHearMovementButLoseSilentTravellers(GameTestHelper h){
        var l=h.getLevel();var base=h.absolutePos(new BlockPos(2300,110,0));
        for(var pos:BlockPos.betweenClosed(base.offset(-3,0,-3),base.offset(35,4,3)))l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        var p=CombatTestPlayer.create(l,base.east(9));var hound=ModEntities.ASHEN_HOUND.get().create(l);hound.setPos(Vec3.atBottomCenterOf(base));l.addFreshEntity(hound);
        try{
            p.setDeltaMovement(Vec3.ZERO);h.assertTrue(!hound.hears(p),"A still traveller outside touch range is not a visual target");
            p.setDeltaMovement(.1,0,0);h.assertTrue(hound.hears(p),"Ordinary walking carries through open air");
            p.setShiftKeyDown(true);h.assertTrue(!hound.hears(p),"Crouching can avoid initial detection");p.setShiftKeyDown(false);
            p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.HUSH_CHARM.get()));h.assertTrue(!hound.hears(p),"The forged charm suppresses ordinary walking noise at this distance");
            p.setSprinting(true);h.assertTrue(hound.hears(p),"The charm does not make sprinting silent");p.setSprinting(false);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            for(var pos:BlockPos.betweenClosed(base.offset(4,0,-2),base.offset(4,3,2)))l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(!hound.hears(p),"A solid intervening wall blocks direct sound acquisition");
            hound.hear(p.position());var last=hound.lastSound();p.setPos(Vec3.atBottomCenterOf(base.east(32)));p.setDeltaMovement(Vec3.ZERO);p.setShiftKeyDown(true);
            for(int i=0;i<25;i++)hound.tick();h.assertTrue(hound.lastSound().equals(last),"A quiet player does not silently update the remembered pursuit position");
            h.succeed();
        }finally{hound.discard();l.getServer().getPlayerList().remove(p);}
    }
}
