package dev.elysium.gametest;

import dev.elysium.entity.CinderBoltEntity;
import dev.elysium.entity.SlagChimeraEntity;
import dev.elysium.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium")
@PrefixGameTestTemplate(false)
public final class CinderboundCombatGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void gazeLaunchesRealBoltsThatDamagePlayersAndStopAtWalls(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(1000,170,0));clear(level,base);
        var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        var player=h.makeMockServerPlayerInLevel();var mob=body(level,base);
        try {
            player.setGameMode(GameType.SURVIVAL);player.setPos(Vec3.atBottomCenterOf(base.south(20)));player.setHealth(20);
            for(int n=0;n<15;n++)mob.tick();
            mob.phase(1,44);mob.tick();
            var bolts=level.getEntitiesOfClass(CinderBoltEntity.class,mob.getBoundingBox().inflate(36));
            h.assertTrue(bolts.size()==1,"The shooting sound must accompany a real projectile entity");
            var bolt=bolts.getFirst();
            h.assertTrue(player.getHealth()==20,"Launch may not apply invisible instant gaze damage");
            Vec3 before=bolt.position();bolt.tick();
            h.assertTrue(bolt.position().distanceToSqr(before)>.1,"The visible bolt must travel before impact");
            for(int n=0;n<60&&!bolt.isRemoved();n++)bolt.tick();
            h.assertTrue(player.getHealth()<20&&bolt.isRemoved(),"An unobstructed volley must actually hurt a survival player");
            player.setHealth(20);player.invulnerableTime=0;
            for(var p:BlockPos.betweenClosed(base.offset(-5,0,11),base.offset(5,14,11)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            mob.phase(1,50);mob.tick();
            bolts=level.getEntitiesOfClass(CinderBoltEntity.class,mob.getBoundingBox().inflate(36),e->!e.isRemoved());
            h.assertTrue(bolts.size()==1,"A later volley must launch even when its aim is obstructed");
            bolt=bolts.getFirst();for(int n=0;n<60&&!bolt.isRemoved();n++)bolt.tick();
            h.assertTrue(bolt.isRemoved()&&player.getHealth()==20,"The wall must consume the projectile without damage behind it");
            h.assertTrue(level.getBlockState(base.offset(0,6,11)).is(Blocks.STONE),"Cinder bolts cannot excavate or ignite cover");
            player.setGameMode(GameType.CREATIVE);
            var observer=ModEntities.CINDER_BOLT.get().create(level);observer.setOwner(mob);
            observer.setPos(player.getX(),player.getY()+1,player.getZ()-3);observer.setDeltaMovement(0,0,.8);level.addFreshEntity(observer);
            for(int n=0;n<8&&!observer.isRemoved();n++)observer.tick();
            h.assertTrue(player.getHealth()==20,"Creative observers may not take projectile damage");observer.discard();
            h.succeed();
        } finally {mob.discard();level.getServer().getPlayerList().remove(player);level.getServer().setDifficulty(difficulty,true);}
    }
    @GameTest(template="portal_test_empty",timeoutTicks=200)
    public static void tailAndExpandingPulseDamageOnlyTheirSweptAreas(GameTestHelper h) {
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(1300,170,0));clear(level,base);
        var difficulty=level.getDifficulty();level.getServer().setDifficulty(Difficulty.NORMAL,true);
        var player=h.makeMockServerPlayerInLevel();var mob=body(level,base);
        try {
            player.setGameMode(GameType.SURVIVAL);reset(player,base.north(10));
            mob.phase(2,0);mob.phase(2,41);mob.tick();
            h.assertTrue(player.getHealth()<20,"The tail's middle sweep must hit a player standing behind it");
            float health=player.getHealth();player.invulnerableTime=0;mob.tick();
            h.assertTrue(player.getHealth()==health,"One sweep may damage a player only once");
            reset(player,base.south(15));mob.phase(3,0);mob.phase(3,55);mob.tick();
            h.assertTrue(player.getHealth()<20,"The visible expanding ring must damage its current radius");
            reset(player,base.south(5));mob.phase(3,0);mob.phase(3,55);mob.tick();
            h.assertTrue(player.getHealth()==20,"The inner pocket must remain outside the ring's damage");
            reset(player,base.south(15));
            for(var p:BlockPos.betweenClosed(base.offset(-4,0,8),base.offset(4,5,8)))level.setBlock(p,Blocks.STONE.defaultBlockState(),2);
            mob.phase(3,0);mob.phase(3,55);mob.tick();
            h.assertTrue(player.getHealth()==20,"Solid cover must stop the actual pulse, not just its prediction");
            player.setGameMode(GameType.SPECTATOR);mob.phase(2,0);mob.phase(2,41);mob.tick();
            h.assertTrue(mob.phase()==0,"The encounter must bank its attacks when only spectators remain");
            h.succeed();
        } finally {mob.discard();level.getServer().getPlayerList().remove(player);level.getServer().setDifficulty(difficulty,true);}
    }
    private static void reset(ServerPlayer p,BlockPos pos){p.setPos(Vec3.atBottomCenterOf(pos));p.setHealth(20);p.invulnerableTime=0;p.setDeltaMovement(Vec3.ZERO);}
    private static SlagChimeraEntity body(ServerLevel l,BlockPos p) {
        var mob=ModEntities.SLAG_CHIMERA.get().create(l);mob.bind(p,Vec3.atBottomCenterOf(p));mob.setYRot(0);l.addFreshEntity(mob);return mob;
    }
    private static void clear(ServerLevel l,BlockPos p) {
        for(var at:BlockPos.betweenClosed(p.offset(-6,0,-22),p.offset(6,15,26)))l.setBlock(at,Blocks.AIR.defaultBlockState(),2);
    }
    private CinderboundCombatGameTests(){}
}
