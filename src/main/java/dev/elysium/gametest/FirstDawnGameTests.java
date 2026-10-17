package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.*;
import dev.elysium.forge.*;
import dev.elysium.item.DawnWayfinderItem;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.*;
import dev.elysium.story.*;
import dev.elysium.structure.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class FirstDawnGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void aSeparateClockSurvivesReloadAndBothResolutions(GameTestHelper h) {
        var realm=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);
        var original=RealmCycle.get(realm).save(new CompoundTag(),realm.registryAccess());
        try {
            var cycle=new RealmCycle();install(realm,cycle);long overworld=h.getLevel().getServer().overworld().getDayTime();
            for(int i=0;i<100;i++)cycle.advance(true);
            h.assertTrue(realm.getDayTime()==11500&&!cycle.released(),"Ordinary visits must retain the golden hour through the real world-time mixin");
            h.assertTrue(cycle.awaken(new BlockPos(5,40,5))&&!cycle.awaken(BlockPos.ZERO),"Only the first release can bind the world's origin forge");
            for(int i=0;i<2500;i++)cycle.advance(true);
            h.assertTrue(cycle.phase()==RealmCycle.Phase.NIGHT&&realm.getDayTime()==14000,"The sun must move into the first night");
            cycle.advance(false);h.assertTrue(cycle.time()==14000,"An unoccupied realm has no offline deadline");
            for(int i=0;i<9000;i++)cycle.advance(true);
            h.assertTrue(cycle.phase()==RealmCycle.Phase.STALLED&&cycle.time()==22000&&!cycle.resolve(true),"Dawn cannot silently pass unanswered watches");
            cycle.answerWatch(0,new BlockPos(100,70,0));cycle.answerWatch(0,new BlockPos(200,70,0));
            h.assertTrue(cycle.watches()==1&&DawnWayfinderItem.next(cycle.watches())==1,"Two harvest fires cannot substitute for the water watch");
            var restored=RealmCycle.load(cycle.save(new CompoundTag(),realm.registryAccess()),realm.registryAccess());install(realm,restored);
            h.assertTrue(realm.getDayTime()==22000&&restored.watches()==1&&restored.forge().equals(new BlockPos(5,40,5)),"Clock, distinct watches and remembered forge survive serialization");
            restored.answerWatch(1,new BlockPos(0,70,100));restored.answerWatch(2,new BlockPos(0,150,200));
            h.assertTrue(restored.resolve(false)&&realm.getDayTime()==11500,"The chosen golden hour is a genuine resolution");
            for(int i=0;i<200;i++)restored.advance(true);h.assertTrue(realm.getDayTime()==11500,"A chosen golden hour remains arrested");
            h.assertTrue(restored.resolve(true),"The repaired governor can revisit the choice");
            for(int i=0;i<2000;i++)restored.advance(true);
            h.assertTrue(restored.firstDawn()&&realm.getDayTime()==24000,"A living realm reaches its first actual dawn");
            h.assertTrue(h.getLevel().getServer().overworld().getDayTime()==overworld,"No part of the story may write the Overworld clock");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),realm.registryAccess());
            try{var packet=new RealmClockPayload(restored.time(),true);RealmClockPayload.CODEC.encode(buffer,packet);h.assertTrue(packet.equals(RealmClockPayload.CODEC.decode(buffer)),"The client clock packet must round trip");}finally{buffer.release();}
            h.succeed();
        } finally {install(realm,RealmCycle.load(original,realm.registryAccess()));}
    }
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void watchesConsumeDistinctOfferingsAndTheGovernorRequiresPhysicalHeat(GameTestHelper h) {
        var realm=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);var original=RealmCycle.get(realm).save(new CompoundTag(),realm.registryAccess());
        try {
            var cycle=new RealmCycle();install(realm,cycle);cycle.awaken(new BlockPos(2,70,2));for(int i=0;i<2500;i++)cycle.advance(true);
            var player=new FakePlayer(realm,new GameProfile(UUID.randomUUID(),"WatchRestorer"));
            var seals=new ItemStack(ModItems.WATCHFIRE_SEAL.get(),3);
            var harvest=watch(realm,new BlockPos(705000,100,705000),0);
            player.getInventory().add(new ItemStack(Items.WHEAT,7));
            h.assertTrue(!harvest.repair(player,seals)&&seals.getCount()==3&&cycle.watches()==0,"Failed offerings cannot consume seals or grant a watch");
            player.getInventory().add(new ItemStack(Items.WHEAT));h.assertTrue(harvest.repair(player,seals)&&seals.getCount()==2,"Harvest requires its full offering");
            h.assertTrue(!harvest.repair(player,seals)&&seals.getCount()==2,"A second user cannot repair or collect the same fire twice");
            h.assertTrue(Nightfall.protectedAt(realm,harvest.getBlockPos().east(20)),"An actual restored watch supplies sanctuary");
            var water=watch(realm,new BlockPos(705100,100,705000),1);player.getInventory().add(new ItemStack(Items.WATER_BUCKET));
            h.assertTrue(water.repair(player,seals)&&player.getInventory().contains(new ItemStack(Items.BUCKET)),"Water is offered; its vessel is returned");
            var high=watch(realm,new BlockPos(705200,100,705000),2);player.getInventory().add(new ItemStack(Items.AMETHYST_SHARD,4));
            h.assertTrue(high.repair(player,seals)&&seals.isEmpty()&&cycle.watches()==7,"Three different traditions complete the repair");
            var p=new ForgePlan(706000,706000,172,1);var governor=ForgeHammer.world(p,0,1,110);realm.setBlock(governor,ModBlocks.CELESTIAL_GOVERNOR.get().defaultBlockState(),3);
            var clock=new ItemStack(Items.CLOCK);
            h.assertTrue(!ModBlocks.CELESTIAL_GOVERNOR.get().choose(player,governor,clock,true),"An isolated governor cannot resolve the sky");
            for(var f:ForgeFixtures.ALL){var pos=f.worldPosition(p);realm.setBlock(pos,ForgeFixtures.state(p,f).setValue(ForgeServiceBlock.DECAY,0),3);((ForgeServiceBlockEntity)realm.getBlockEntity(pos)).bind(p);}
            realm.setBlock(ForgeFixtures.hearth(p),ModBlocks.FORGE_HEARTH.get().defaultBlockState().setValue(DormantHearthBlock.FLAME,true),3);
            var source=ForgeLighting.loadedService(realm,ForgeFixtures.source(p));source.addFuel(48000);source.ignite();source.toggleAirflow();for(int i=0;i<160;i++)ForgeLighting.advance(realm,source,10);
            h.assertTrue(!ModBlocks.CELESTIAL_GOVERNOR.get().choose(player,governor,clock,false),"The choice is deliberately operated");
            h.assertTrue(ModBlocks.CELESTIAL_GOVERNOR.get().choose(player,governor,clock,true)&&clock.getCount()==1,"A supplied shrine releases the cycle without eating its instrument");
            source.toggleAirflow();h.assertTrue(!ModBlocks.CELESTIAL_GOVERNOR.get().choose(player,governor,new ItemStack(ModItems.ELYSIUM_FRAGMENT.get()),true),"A banked forge cannot change the world again");
            h.succeed();
        } finally {install(realm,RealmCycle.load(original,realm.registryAccess()));}
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void smallWatchRuinsFollowIndividualSurveyedColumns(GameTestHelper h) {
        int[] heights=new int[289];for(int u=-8;u<=8;u++)for(int v=-8;v<=8;v++)heights[(u+8)*17+v+8]=90+Math.floorDiv(u,3);
        for(int kind=0;kind<3;kind++) {
            var ruin=new WatchfirePiece(0,0,90,kind,heights);
            h.assertTrue(ruin.socket().equals(new BlockPos(0,91,0))&&ruin.material(0,91,0).is(ModBlocks.WATCHFIRE.get()),"All traditions need an accessible socket");
            h.assertTrue(ruin.material(7,80,7)==null&&ruin.material(7,91,7)==null,"The ruin cannot flatten its whole footprint");
            var tag=ruin.createTag(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext.fromLevel(h.getLevel()));
            var loaded=new WatchfirePiece(tag);h.assertTrue(loaded.socket().equals(ruin.socket())&&loaded.watch()==kind,"Survey and watch identity survive actual structure persistence");
        }
        h.succeed();
    }
    private static WatchfireBlockEntity watch(ServerLevel l,BlockPos pos,int kind){l.setBlock(pos,ModBlocks.WATCHFIRE.get().defaultBlockState().setValue(WatchfireBlock.WATCH,kind),3);var f=(WatchfireBlockEntity)l.getBlockEntity(pos);f.bind(kind);return f;}
    private static void install(ServerLevel realm,RealmCycle cycle){realm.getDataStorage().set("elysium_realm_cycle",cycle);RealmClock.load(new LevelEvent.Load(realm));}
    private FirstDawnGameTests(){}
}
