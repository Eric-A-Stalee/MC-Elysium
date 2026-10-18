package dev.elysium.gametest;

import dev.elysium.block.*;
import dev.elysium.entity.*;
import dev.elysium.registry.*;
import dev.elysium.story.RealmCycle;
import dev.elysium.structure.DeepBindingPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class InhabitantGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=160)
    public static void mournersWarnAtLockedGroundAndRespectHopeAndCover(GameTestHelper h){
        var l=h.getLevel();var home=h.absolutePos(new BlockPos(4500,110,0));
        for(var pos:BlockPos.betweenClosed(home.offset(-5,0,-3),home.offset(8,7,18)))l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        for(int x=(home.getX()-8)>>4;x<=(home.getX()+12)>>4;x++)for(int z=(home.getZ()-8)>>4;z<=(home.getZ()+22)>>4;z++)l.setChunkForced(x,z,true);
        h.runAfterDelay(20,()->{
            var old=l.getDifficulty();l.getServer().setDifficulty(Difficulty.NORMAL,true);
            var p=CombatTestPlayer.create(l,home.south(10));var m=ModEntities.MIRE_MOURNER.get().create(l);m.setPos(Vec3.atBottomCenterOf(home));m.setNoAi(true);m.setNoGravity(true);l.addFreshEntity(m);
            try{
                h.assertTrue(m.beginLament(p),"An exposed survivor can hear the false warmth");
                for(int i=0;i<39;i++)m.tick();h.assertTrue(p.getHealth()==20,"The entire marked warning precedes damage");m.tick();h.assertTrue(p.getHealth()<20,"Remaining inside the warning causes real damage");
                for(int i=0;i<100;i++)m.tick();p.setHealth(20);p.invulnerableTime=0;p.setDeltaMovement(Vec3.ZERO);
                h.assertTrue(m.beginLament(p),"The lament recovers for another attempt");p.setPos(Vec3.atBottomCenterOf(home.south(10).east(5)));
                for(int i=0;i<40;i++)m.tick();h.assertTrue(p.getHealth()==20,"The marked ground cannot chase a dodging player");
                for(int i=0;i<100;i++)m.tick();p.setPos(Vec3.atBottomCenterOf(home.south(10)));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.HOPES_EMBER.get()));
                h.assertTrue(!m.beginLament(p),"Hope suppresses the lament without being consumed");p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                for(var pos:BlockPos.betweenClosed(home.offset(-3,0,5),home.offset(3,5,5)))l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
                h.assertTrue(!m.beginLament(p),"The mourner cannot choose a target through solid cover");h.succeed();
            }finally{m.discard();l.getServer().getPlayerList().remove(p);l.getServer().setDifficulty(old,true);for(int x=(home.getX()-8)>>4;x<=(home.getX()+12)>>4;x++)for(int z=(home.getZ()-8)>>4;z<=(home.getZ()+22)>>4;z++)l.setChunkForced(x,z,false);}
        });
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void mothsFindEmittingBlocksButNotCounterfeitGlowOrLightBehindWalls(GameTestHelper h){
        var l=h.getLevel();var home=h.absolutePos(new BlockPos(4600,110,0));
        for(var pos:BlockPos.betweenClosed(home.offset(-8,-4,-8),home.offset(8,8,8)))l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        var moth=ModEntities.ASH_MOTH.get().create(l);moth.setPos(Vec3.atCenterOf(home));
        var mourner=ModEntities.MIRE_MOURNER.get().create(l);mourner.setPos(Vec3.atCenterOf(home.east(3)));l.addFreshEntity(mourner);
        try{
            moth.findLight();h.assertTrue(moth.light()==null,"An emissive creature face does not emit real block light");
            var lamp=home.east(4);l.setBlock(lamp,ModBlocks.KEEPERS_LAMP.get().defaultBlockState(),2);moth.findLight();
            h.assertTrue(lamp.equals(moth.light()),"The moth identifies a real Keeper's Lamp");
            for(var pos:BlockPos.betweenClosed(home.offset(2,-3,-5),home.offset(2,6,5)))l.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
            moth.findLight();h.assertTrue(moth.light()==null,"A wall stops light acquisition instead of guiding the moth into it");h.succeed();
        }finally{mourner.discard();}
    }
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void theCaptiveFitsHisPrisonAndEveryApproachStillReachesTheBinding(GameTestHelper h){
        int[] route=new int[64];java.util.Arrays.fill(route,1);var p=new DeepBindingPiece(0,-480,0,-500,2,19,route);
        for(int x=-9;x<=9;x++)for(int z=-32;z<=-14;z++)for(int y=2;y<=36;y++){
            var state=p.materialAt(x,-480+y,z);h.assertTrue(state!=null&&state.isAir(),"The titan's full body volume has room at "+x+","+y+","+z);
        }
        for(int side:new int[]{-1,1})h.assertTrue(p.materialAt(side*34,-436,-26).is(ModBlocks.FORGE_IRON.get()),"Each wrist tether meets a real anchor collar");
        h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=180)
    public static void aSavedCaptiveCommandsOnlyUntilHisOwnRestraintIsRepaired(GameTestHelper h){
        var l=h.getLevel();var core=h.absolutePos(new BlockPos(4800,110,0));
        for(var p:BlockPos.betweenClosed(core.offset(-10,0,-34),core.offset(10,39,20)))l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        for(int x=(core.getX()-44)>>4;x<=(core.getX()+44)>>4;x++)for(int z=(core.getZ()-55)>>4;z<=(core.getZ()+45)>>4;z++)l.setChunkForced(x,z,true);
        h.runAfterDelay(20,()->{
            var old=l.getDifficulty();var saved=RealmCycle.get(l).save(new CompoundTag(),l.registryAccess());l.getServer().setDifficulty(Difficulty.NORMAL,true);
            var cycle=new RealmCycle();l.getDataStorage().set("elysium_realm_cycle",cycle);cycle.awaken(core);
            l.setBlock(core,ModBlocks.DEEP_BINDING.get().defaultBlockState().setValue(BindingBlock.KIND,2),3);var binding=(BindingBlockEntity)l.getBlockEntity(core);
            var p=CombatTestPlayer.create(l,core.south(7));CrownlessEntity titan=null;
            try{
                h.assertTrue(binding.summonTitan()&&!binding.summonTitan(),"The restraint owns exactly one living captive");titan=(CrownlessEntity)l.getEntity(binding.titan());
                var restored=(BindingBlockEntity)BlockEntity.loadStatic(core,binding.getBlockState(),binding.saveWithFullMetadata(l.registryAccess()),l.registryAccess());restored.setLevel(l);l.setBlockEntity(restored);binding=restored;
                h.assertTrue(titan.getUUID().equals(binding.titan())&&!binding.summonTitan(),"Reloading the restraint preserves its captive UUID");
                titan.phase(CrownlessEntity.INHALE,0);for(int i=0;i<60;i++)titan.tick();h.assertTrue(p.getHealth()==20&&titan.phase()==CrownlessEntity.COMMAND,"The captive's full breath is a safe warning");
                for(int i=0;i<35;i++)titan.tick();h.assertTrue(p.getHealth()<20,"The travelling command can reach a survivor in the open");
                p.setHealth(20);p.invulnerableTime=0;p.setDeltaMovement(Vec3.ZERO);
                for(var at:BlockPos.betweenClosed(core.offset(-3,0,-10),core.offset(3,8,-10)))l.setBlock(at,Blocks.STONE.defaultBlockState(),2);
                h.assertTrue(!titan.canCommand(p),"Broken walls shelter against the low travelling command");
                h.assertTrue(binding.summon(),"The restraint still owns its original Sentenced encounter");binding.wardenDefeated(binding.warden());
                h.assertTrue(binding.seal(p,new ItemStack(ModItems.BINDING_NAIL.get())),"Ending the sentence permits the physical binding repair");titan.tick();
                h.assertTrue(titan.phase()==CrownlessEntity.DORMANT&&!titan.isRemoved(),"Repair quiets a living prisoner instead of deleting a prop");h.succeed();
            }finally{if(titan!=null)titan.discard();if(binding.warden()!=null&&l.getEntity(binding.warden())!=null)l.getEntity(binding.warden()).discard();l.getServer().getPlayerList().remove(p);l.removeBlock(core,false);l.getDataStorage().set("elysium_realm_cycle",RealmCycle.load(saved,l.registryAccess()));l.getServer().setDifficulty(old,true);for(int x=(core.getX()-44)>>4;x<=(core.getX()+44)>>4;x++)for(int z=(core.getZ()-55)>>4;z<=(core.getZ()+45)>>4;z++)l.setChunkForced(x,z,false);}
        });
    }
}
