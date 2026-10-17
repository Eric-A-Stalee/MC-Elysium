package dev.elysium.gametest;

import dev.elysium.forge.KeeperLampLight;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.*;
import dev.elysium.story.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class SunwroughtGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void harvestSickleSelectsRipePlantsAndPaysForEveryReplant(GameTestHelper h) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(20,80,20));
        var player=CombatTestPlayer.create(level,center.south(3));
        for(var p:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,-1,1)))level.setBlock(p,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),3);
        level.setBlock(center.above(3),Blocks.GLOWSTONE.defaultBlockState(),3);
        // Newly generated test chunks need a lighting update before a real crop can survive there.
        h.runAfterDelay(20,()->{try {
            h.assertTrue(level.getRawBrightness(center,0)>=8,"Harvest arena must have real crop-supporting light");
            for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))level.setBlock(p,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),3);
            var young=center.north().west();level.setBlock(young,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,3),3);
            var tool=new ItemStack(ModItems.HARVEST_SICKLE.get());player.setItemInHand(InteractionHand.MAIN_HAND,tool);
            player.getInventory().add(new ItemStack(Items.WHEAT_SEEDS,9));
            var result=tool.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(center.getCenter(),Direction.UP,center,false)));
            h.assertTrue(result.consumesAction()&&tool.getDamageValue()==8,"Eight ripe plants cost eight durability");
            h.assertTrue(level.getBlockState(young).getValue(CropBlock.AGE)==3,"The sickle must leave young crops alone");
            for(var p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,0,1)))if(!p.equals(young))h.assertTrue(level.getBlockState(p).is(Blocks.WHEAT)&&level.getBlockState(p).getValue(CropBlock.AGE)==0,"Harvested crops replant at age zero");
            int seeds=0;for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(Items.WHEAT_SEEDS))seeds+=player.getInventory().getItem(i).getCount();
            h.assertTrue(seeds==1,"Replanting uses the player's seeds, not free harvest copies");h.succeed();
        } finally {level.getServer().getPlayerList().remove(player);}});
    }
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void cindersTelegraphDamageRetreatFromWardsAndEndWithTheCrisis(GameTestHelper h) {
        var realm=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);var original=RealmCycle.get(realm).save(new CompoundTag(),realm.registryAccess());
        var oldDifficulty=realm.getDifficulty();realm.getServer().setDifficulty(Difficulty.NORMAL,true);
        var base=new BlockPos(707000,200,707000);
        for(var p:BlockPos.betweenClosed(base.offset(-14,-2,-14),base.offset(14,10,14)))realm.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        var player=CombatTestPlayer.create(realm,base);
        var wisp=ModEntities.CINDER_WISP.get().create(realm);
        try {
            var cycle=new RealmCycle();realm.getDataStorage().set("elysium_realm_cycle",cycle);RealmClock.load(new LevelEvent.Load(realm));cycle.awaken(base);for(int i=0;i<2500;i++)cycle.advance(true);
            wisp.setPos(Vec3.atBottomCenterOf(base.south(8)).add(0,1.5,0));realm.addFreshEntity(wisp);
            boolean warned=false;for(int i=0;i<240&&player.getHealth()==20;i++){wisp.tick();warned|=wisp.charging();}
            h.assertTrue(warned&&player.getHealth()<20,"A visible charge must precede actual survival damage: health="+player.getHealth()+" wisp="+wisp.position());
            player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModBlocks.WARD_LAMP.get()));player.setHealth(20);player.invulnerableTime=0;
            for(int i=0;i<100;i++)wisp.tick();
            h.assertTrue(player.getHealth()==20&&!wisp.charging()&&KeeperLampLight.carrying(player),"The carried ward repels cinders and works above ground");
            var ward=base.east(4);realm.setBlock(ward,ModBlocks.WARD_LAMP.get().defaultBlockState(),3);
            h.assertTrue(Nightfall.protectedAt(realm,base),"A placed ward protects its actual local area");realm.removeBlock(ward,false);
            h.assertTrue(!Nightfall.protectedAt(realm,base),"Mining the lamp removes its protection");
            for(int k=0;k<3;k++){cycle.answerWatch(k,base.offset(k*100,0,0));cycle.bindDeep(k);}cycle.resolve(false);wisp.tick();
            h.assertTrue(wisp.isRemoved()&&!Nightfall.eligible(player),"Resolving the night removes its stray cinders and stops new ones");h.succeed();
        } finally {
            wisp.discard();realm.getServer().getPlayerList().remove(player);realm.getServer().setDifficulty(oldDifficulty,true);
            realm.getDataStorage().set("elysium_realm_cycle",RealmCycle.load(original,realm.registryAccess()));RealmClock.load(new LevelEvent.Load(realm));
        }
    }
    private SunwroughtGameTests(){}
}
