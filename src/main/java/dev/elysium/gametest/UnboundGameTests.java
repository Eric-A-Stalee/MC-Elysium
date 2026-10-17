package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.*;
import dev.elysium.forge.*;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.*;
import dev.elysium.story.*;
import dev.elysium.structure.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class UnboundGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void completedWorkOpensTheVesselButInterruptedWorkDoesNot(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(ElysiumTravel.DIMENSION);var original=RealmCycle.get(l).save(new CompoundTag(),l.registryAccess());
        try{
            var cycle=new RealmCycle();l.getDataStorage().set("elysium_realm_cycle",cycle);
            var plan=new ForgePlan(709000,709000,180,0);var press=ForgePressGameTests.prepare(l,plan);
            var vessel=PandoraVesselBlock.position(plan);l.setBlock(vessel,ModBlocks.PANDORA_VESSEL.get().defaultBlockState(),3);
            var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"FirstWork"));p.getAbilities().instabuild=true;
            h.assertTrue(press.queue(p,ForgeRecipes.byId("sunwrought"))&&press.start(),"A supplied taught press can begin");
            for(int i=0;i<7;i++)press.advance();press.pause();for(int i=0;i<40;i++)press.advance();
            h.assertTrue(!cycle.released()&&!l.getBlockState(vessel).getValue(PandoraVesselBlock.OPEN),"A first impact and a paused job cannot release the evils");
            press.start();for(int i=0;i<40;i++)press.advance();
            h.assertTrue(cycle.released()&&cycle.forgings()==1&&l.getBlockState(vessel).getValue(PandoraVesselBlock.OPEN),"Only a completed physical batch opens the vessel");
            for(int i=0;i<80;i++)press.advance();h.assertTrue(cycle.forgings()==1,"An idle completed press cannot repeatedly increase pressure");
            for(int i=0;i<4000;i++)cycle.advance(true);h.assertTrue(cycle.ruptureDue(),"Occupied unresolved nights accumulate pressure");
            var saved=RealmCycle.load(cycle.save(new CompoundTag(),l.registryAccess()),l.registryAccess());int pressure=saved.pressureTicks();for(int i=0;i<9000;i++)saved.advance(false);
            h.assertTrue(saved.ruptureDue()&&saved.pressureTicks()==pressure,"Reloads preserve pressure; an empty realm adds no offline deadline");h.succeed();
        }finally{l.getDataStorage().set("elysium_realm_cycle",RealmCycle.load(original,l.registryAccess()));RealmClock.load(new net.neoforged.neoforge.event.level.LevelEvent.Load(l));}
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void bindingsPreserveGradedApproachesAndDistinctGeometry(GameTestHelper h){
        int[] route=new int[64];for(int i=0;i<64;i++)route[i]=1-Math.min(26,i);
        for(int kind=0;kind<3;kind++){
            var piece=new DeepBindingPiece(-700,-480,811,-520,kind,55,route);
            var restored=new DeepBindingPiece(piece.createTag(StructurePieceSerializationContext.fromLevel(h.getLevel())));
            h.assertTrue(restored.core().equals(piece.core())&&restored.kind()==kind,"The restraint and its terrain survey survive saving");
            for(var p:restored.approachRoute()){
                h.assertTrue(restored.materialAt(p.getX(),p.getY(),p.getZ()).isSolid(),"A surveyed approach has continuous footing");
                for(int dy=1;dy<=3;dy++)h.assertTrue(restored.materialAt(p.getX(),p.getY()+dy,p.getZ()).isAir(),"The approach keeps headroom, independent of natural terrain");
            }
            for(int z=2;z<=26;z++)h.assertTrue(restored.materialAt(-700,-478,811+z).isAir(),"The path reaches the restraint from the entrance");
        }
        h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void restraintsRequireTheirOwnOfferingLightAndOwnedJailer(GameTestHelper h){
        var l=h.getLevel();var base=h.absolutePos(new BlockPos(2500,110,0));
        for(var pos:BlockPos.betweenClosed(base.offset(-4,-1,-4),base.offset(38,7,4)))l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        for(int x=(base.getX()-30)>>4;x<=(base.getX()+62)>>4;x++)for(int z=(base.getZ()-30)>>4;z<=(base.getZ()+30)>>4;z++)l.getChunk(x,z);
        for(int kind=0;kind<3;kind++)l.setBlock(base.east(kind*12),ModBlocks.DEEP_BINDING.get().defaultBlockState().setValue(BindingBlock.KIND,kind),3);
        l.setBlock(base.east(14).above(),Blocks.GLOWSTONE.defaultBlockState(),3);
        h.runAfterDelay(30,()->{
            var original=RealmCycle.get(l).save(new CompoundTag(),l.registryAccess());var previous=l.getDifficulty();var cycle=new RealmCycle();l.getDataStorage().set("elysium_realm_cycle",cycle);cycle.awaken(base);l.getServer().setDifficulty(Difficulty.NORMAL,true);
            // UNKNOWN chunk tickets from setup may expire during the light-engine wait.
            // Restore the whole encounter neighbourhood, just as a nearby real player would.
            for(int x=(base.getX()-30)>>4;x<=(base.getX()+62)>>4;x++)for(int z=(base.getZ()-30)>>4;z<=(base.getZ()+30)>>4;z++)l.getChunk(x,z);
            var p=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"DeepRepair"));var nail=new ItemStack(ModItems.BINDING_NAIL.get(),3);
            var hunger=(BindingBlockEntity)l.getBlockEntity(base);var sorrow=(BindingBlockEntity)l.getBlockEntity(base.east(12));var dominion=(BindingBlockEntity)l.getBlockEntity(base.east(24));
            try{
                var wheat=new ItemStack(Items.WHEAT,7);h.assertTrue(!hunger.offer(wheat,p)&&wheat.getCount()==7&&!hunger.seal(p,nail),"Hunger cannot take an incomplete offering or an unprepared nail");
                wheat.grow(1);h.assertTrue(hunger.offer(wheat,p)&&wheat.isEmpty()&&hunger.seal(p,nail),"The offering gives a real, consumable repair window");
                h.assertTrue(!sorrow.seal(p,nail),"Feeding Hunger does not satisfy Sorrow");p.setPos(Vec3.atBottomCenterOf(base.east(12).south(2)));p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.HOPES_EMBER.get()));
                for(int i=0;i<8;i++)sorrow.attend(p);p.setShiftKeyDown(false);sorrow.attend(p);p.setShiftKeyDown(true);for(int i=0;i<8;i++)sorrow.attend(p);
                h.assertTrue(!sorrow.seal(p,nail),"Breaking stillness resets an unfinished vigil");
                for(int i=0;i<8;i++)sorrow.attend(p);h.assertTrue(sorrow.seal(p,nail)&&p.getOffhandItem().is(ModItems.HOPES_EMBER.get()),"Four seconds of Hope and actual block light quiet Sorrow without consuming Hope");
                h.assertTrue(dominion.summon()&&!dominion.summon(),"Dominion owns one physical jailer");dominion.wardenDefeated(UUID.randomUUID());h.assertTrue(!dominion.seal(p,nail),"Another creature's death cannot end this sentence");
                dominion.wardenDefeated(dominion.warden());var restored=(BindingBlockEntity)BlockEntity.loadStatic(dominion.getBlockPos(),dominion.getBlockState(),dominion.saveWithFullMetadata(l.registryAccess()),l.registryAccess());restored.setLevel(l);l.setBlockEntity(restored);
                h.assertTrue(restored.defeated()&&!restored.summon()&&restored.seal(p,nail)&&nail.isEmpty()&&cycle.bindings()==7,"The owned defeat survives saving and each restraint consumes exactly one nail");h.succeed();
            }finally{if(dominion.warden()!=null&&l.getEntity(dominion.warden())!=null)l.getEntity(dominion.warden()).discard();l.getDataStorage().set("elysium_realm_cycle",RealmCycle.load(original,l.registryAccess()));l.getServer().setDifficulty(previous,true);}
        });
    }
    @GameTest(template="portal_test_empty",timeoutTicks=140)
    public static void theSentenceWarnsLocksItsLaneAndCannotStrikeThroughStone(GameTestHelper h){
        var l=h.getLevel();var home=h.absolutePos(new BlockPos(2700,110,0));for(var pos:BlockPos.betweenClosed(home.offset(-5,0,-2),home.offset(8,7,12)))l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        // Loading blocks alone does not make their entity sections queryable. Keep this
        // remote arena ticking before adding the real player, as the projectile tests do.
        for(int x=(home.getX()-8)>>4;x<=(home.getX()+12)>>4;x++)for(int z=(home.getZ()-8)>>4;z<=(home.getZ()+16)>>4;z++)l.setChunkForced(x,z,true);
        h.runAfterDelay(20,()->{
        var previous=l.getDifficulty();l.getServer().setDifficulty(Difficulty.NORMAL,true);
        var p=CombatTestPlayer.create(l,home.south(6));var w=ModEntities.CHAIN_WARDEN.get().create(l);w.setPos(Vec3.atBottomCenterOf(home));w.setNoAi(true);w.setNoGravity(true);w.setYRot(0);l.addFreshEntity(w);
        try{
            h.assertTrue(l.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,p.getBoundingBox().inflate(1)).contains(p),"The target is present in the real spatial entity index before striking");
            h.assertTrue(w.canStrike(p),"The survival target begins inside the unobstructed chain lane");
            w.phase(1,0);for(int i=0;i<36;i++)w.tick();h.assertTrue(p.getHealth()==20&&w.phase()==2,"All 36 warning ticks precede the chain's strike");w.tick();h.assertTrue(p.getHealth()<20,"Remaining in the marked lane causes real damage");
            for(int i=0;i<10;i++)w.tick();h.assertTrue(w.phase()==3,"The jailer exposes a long recovery after striking");
            p.setHealth(20);p.invulnerableTime=0;p.setPos(Vec3.atBottomCenterOf(home.south(6).east(4)));w.phase(2,0);w.tick();h.assertTrue(p.getHealth()==20,"Stepping sideways avoids the locked lane");
            p.setPos(Vec3.atBottomCenterOf(home.south(6)));for(var pos:BlockPos.betweenClosed(home.offset(-2,0,3),home.offset(2,4,3)))l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            w.phase(2,0);w.tick();h.assertTrue(p.getHealth()==20,"The chains cannot hit through solid cover");h.succeed();
        }finally{w.discard();l.getServer().getPlayerList().remove(p);l.getServer().setDifficulty(previous,true);for(int x=(home.getX()-8)>>4;x<=(home.getX()+12)>>4;x++)for(int z=(home.getZ()-8)>>4;z<=(home.getZ()+16)>>4;z++)l.setChunkForced(x,z,false);}
        });
    }
}
