package dev.elysium.gametest;

import dev.elysium.block.*;
import dev.elysium.registry.*;
import dev.elysium.story.*;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class RuptureGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void rupturesWarnRespectWardsAndLeaveAnInventorySafeScar(GameTestHelper h){
        var l=h.getLevel();var centre=h.absolutePos(new BlockPos(2850,110,0));
        for(var p:BlockPos.betweenClosed(centre.offset(-13,-8,-13),centre.offset(13,9,13)))l.setBlock(p,p.getY()<=centre.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var old=RealmCycle.get(l).save(new CompoundTag(),l.registryAccess());boolean grief=l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING),spawns=l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);var difficulty=l.getDifficulty();
        var cycle=new RealmCycle();l.getDataStorage().set("elysium_realm_cycle",cycle);cycle.awaken(centre);l.getServer().setDifficulty(Difficulty.NORMAL,true);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,l.getServer());
        int[] heights=new int[441];Arrays.fill(heights,centre.getY());var plan=new RupturePlan(centre.getX(),centre.getZ(),heights);var player=CombatTestPlayer.create(l,centre.above());
        try{
            l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,l.getServer());h.assertTrue(!RuptureDirector.open(l,plan,0)&&l.getBlockState(plan.core()).is(Blocks.STONE),"Disabled griefing prevents all rupture terrain writes");
            l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,l.getServer());var ward=centre.east(6).above();l.setBlock(ward,ModBlocks.WARD_LAMP.get().defaultBlockState(),3);cycle.addWard(ward);
            h.assertTrue(!RuptureDirector.open(l,plan,0),"A real ward keeps the surface intact");l.removeBlock(ward,false);cycle.removeWard(ward);
            h.assertTrue(RuptureDirector.open(l,plan,2),"An unprotected surveyed wound can begin");var r=(RiftBlockEntity)l.getBlockEntity(plan.core());
            for(int i=0;i<60;i++)r.advance();h.assertTrue(!r.opened()&&player.getHealth()==20&&l.getBlockState(centre).is(Blocks.STONE),"The warning does not silently carve or damage the ground early");
            var chest=centre.east(3);l.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(chest)).setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
            var restored=(RiftBlockEntity)BlockEntity.loadStatic(r.getBlockPos(),r.getBlockState(),r.saveWithFullMetadata(l.registryAccess()),l.registryAccess());restored.setLevel(l);l.setBlockEntity(restored);
            h.assertTrue(restored.age()==60&&!restored.opened(),"Saving during a warning preserves its countdown");for(int i=0;i<20;i++)restored.advance();
            h.assertTrue(restored.opened()&&player.getHealth()<20&&l.getBlockState(centre).isAir(),"The warned rupture causes real damage and actual excavation");
            var area=new net.minecraft.world.phys.AABB(centre).inflate(20);
            h.assertTrue(l.getEntitiesOfClass(dev.elysium.entity.AshenHoundEntity.class,area).size()==2&&l.getEntitiesOfClass(dev.elysium.entity.ChainWardenEntity.class,area).size()==1,"Both hunters and the wide jailer must physically fit over the uneven crater floor");
            h.assertTrue(((net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(chest)).getItem(0).getCount()==3,"An inventory placed during the warning survives every terrain write");
            var nails=new ItemStack(ModItems.BINDING_NAIL.get(),2);restored.use(player,nails);restored.use(player,nails);
            h.assertTrue(restored.sealed()&&nails.getCount()==1&&l.getBlockState(centre).isAir(),"Repair consumes one nail and leaves a permanent scar");
            var far=new RupturePlan(910000,910000,heights);h.assertTrue(!RuptureDirector.open(l,far,1)&&l.getChunkSource().getChunkNow(910000>>4,910000>>4)==null,"Ruptures cannot force-load remote chunks");h.succeed();
        }finally{
            for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new net.minecraft.world.phys.AABB(centre).inflate(30)))if(e instanceof dev.elysium.entity.AshenHoundEntity||e instanceof dev.elysium.entity.ChainWardenEntity)e.discard();l.getServer().getPlayerList().remove(player);l.getDataStorage().set("elysium_realm_cycle",RealmCycle.load(old,l.registryAccess()));l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,l.getServer());l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(spawns,l.getServer());l.getServer().setDifficulty(difficulty,true);
        }
    }
    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void sorrowCanReachTheSurfaceButAllEmergenceSharesOnePopulationBudget(GameTestHelper h){
        var l=h.getLevel();var centre=h.absolutePos(new BlockPos(4950,110,0));
        for(var p:BlockPos.betweenClosed(centre.offset(-13,-8,-13),centre.offset(13,9,13)))l.setBlock(p,p.getY()<=centre.getY()?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        var old=RealmCycle.get(l).save(new CompoundTag(),l.registryAccess());boolean grief=l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING),spawns=l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);var difficulty=l.getDifficulty();
        var cycle=new RealmCycle();l.getDataStorage().set("elysium_realm_cycle",cycle);cycle.awaken(centre);l.getServer().setDifficulty(Difficulty.NORMAL,true);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,l.getServer());l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,l.getServer());
        int[] heights=new int[441];Arrays.fill(heights,centre.getY());var plan=new RupturePlan(centre.getX(),centre.getZ(),heights);var player=CombatTestPlayer.create(l,centre.above());
        var area=new net.minecraft.world.phys.AABB(centre).inflate(35);
        try{
            h.assertTrue(RuptureDirector.open(l,plan,1),"An unbound Sorrow wound can open");var r=(RiftBlockEntity)l.getBlockEntity(plan.core());
            for(int i=0;i<80;i++)r.advance();
            var mourners=l.getEntitiesOfClass(dev.elysium.entity.MireMournerEntity.class,area);
            h.assertTrue(mourners.size()==1&&l.noCollision(mourners.getFirst()),"A real, fully unobstructed mourner emerges with Sorrow");
            for(int i=0;i<2600;i++)r.advance();
            h.assertTrue(l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,area,DeepEcology::hostile).size()==4,"Repeated emergence shares a four-creature budget across species");
            cycle.bindDeep(1);r.advance();h.assertTrue(r.sealed(),"Repairing Sorrow closes its surface wounds too");
            h.assertTrue(!mourners.getFirst().isRemoved(),"Repair does not erase an escaped inhabitant");h.succeed();
        }finally{
            for(var e:l.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,area,DeepEcology::hostile))e.discard();l.getServer().getPlayerList().remove(player);l.getDataStorage().set("elysium_realm_cycle",RealmCycle.load(old,l.registryAccess()));l.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,l.getServer());l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(spawns,l.getServer());l.getServer().setDifficulty(difficulty,true);
        }
    }

}
