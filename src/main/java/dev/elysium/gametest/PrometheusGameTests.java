package dev.elysium.gametest;

import com.mojang.authlib.GameProfile;
import dev.elysium.block.PrometheusBlockEntity;
import dev.elysium.entity.*;
import dev.elysium.forge.*;
import dev.elysium.registry.*;
import dev.elysium.structure.PrometheusPrisonPiece;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class PrometheusGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void prisonKeepsAnOpenArenaAndSerializesItsSurvey(GameTestHelper h){
        int[] approach=new int[64];for(int i=0;i<64;i++)approach[i]=6-Math.min(i,30);
        var p=new PrometheusPrisonPiece(-1024,-480,2016,-520,81,approach);
        var c=StructurePieceSerializationContext.fromLevel(h.getLevel());
        var q=new PrometheusPrisonPiece(p.createTag(c));
        h.assertTrue(p.createTag(c).equals(q.createTag(c)),"Prison survey and independent seed must round-trip");
        for(var step:q.approachRoute()){
            h.assertTrue(!q.materialAt(step.getX(),step.getY(),step.getZ()).isAir(),"The surveyed approach keeps its floor");
            for(int dy=1;dy<=2;dy++)h.assertTrue(q.materialAt(step.getX(),step.getY()+dy,step.getZ()).isAir(),"Arena rim cannot obstruct the approach at "+step);
        }
        for(int dx=-13;dx<=13;dx++)for(int dz=-13;dz<=13;dz++)if(dx*dx+dz*dz<=169){
            var pos=q.arena().offset(dx,0,dz);
            h.assertTrue(!q.materialAt(pos.getX(),pos.getY()-1,pos.getZ()).isAir(),"Fighting ground is complete");
            for(int dy=0;dy<3;dy++)h.assertTrue(q.materialAt(pos.getX(),pos.getY()+dy,pos.getZ()).isAir(),"A grounded player can dodge through the arena");
        }
        var voice=q.voice();h.assertTrue(q.materialAt(voice.getX(),voice.getY()-1,voice.getZ()).isSolid(),"The voice block has a real footing");
        h.assertTrue(q.materialAt(voice.getX(),voice.getY(),voice.getZ()).is(ModBlocks.PROMETHEUS_VOICE.get()),"The teacher is physically discoverable");h.succeed();
    }
    @GameTest(template="portal_test_empty",timeoutTicks=120)
    public static void onlyTheOwnedEagleUnlocksPersistentPersonalTeaching(GameTestHelper h){
        var l=h.getLevel();BlockPos arena=h.absolutePos(new BlockPos(1800,110,0)),socket=arena.offset(-22,2,-10);
        for(int x=(arena.getX()-34)>>4;x<=(arena.getX()+34)>>4;x++)for(int z=(arena.getZ()-34)>>4;z<=(arena.getZ()+34)>>4;z++)l.getChunk(x,z);
        l.setBlock(socket,ModBlocks.PROMETHEUS_VOICE.get().defaultBlockState(),3);
        var prison=(PrometheusBlockEntity)l.getBlockEntity(socket);prison.bind(arena);
        var reader=new FakePlayer(l,new GameProfile(UUID.randomUUID(),"TitanListener"));
        h.assertTrue(!prison.teach(reader),"A bound titan cannot teach over the eagle");
        h.assertTrue(prison.summon()&&!prison.summon(),"The saved encounter owns exactly one eagle");
        var mob=(BronzeEagleEntity)l.getEntity(prison.eagle());
        try{
            prison.defeat(UUID.randomUUID());h.assertTrue(!prison.defeated(),"An unrelated kill cannot complete the prison");
            prison.defeat(mob.getUUID());
            var restored=(PrometheusBlockEntity)BlockEntity.loadStatic(socket,prison.getBlockState(),prison.saveWithFullMetadata(l.registryAccess()),l.registryAccess());restored.setLevel(l);l.setBlockEntity(restored);
            h.assertTrue(restored.defeated()&&!restored.summon(),"Reloading a defeated prison cannot recreate its eagle");
            h.assertTrue(restored.teach(reader)&&!restored.teach(reader),"The first lesson rewards once; returning is dialogue");
            var memories=ForgeDiscoveries.load(ForgeDiscoveries.get(l).save(new net.minecraft.nbt.CompoundTag(),l.registryAccess()),l.registryAccess());
            reader.getInventory().clearContent();
            h.assertTrue(memories.has(reader.getUUID(),ForgeDiscoveries.PROMETHEUS),"Knowledge survives actual serialization and loss of the book");h.succeed();
        }finally{if(mob!=null)mob.discard();l.removeBlock(socket,false);}
    }
    @GameTest(template="portal_test_empty",timeoutTicks=220)
    public static void eagleWarningsPrecedePhysicalPinionsAndMeleeRecovery(GameTestHelper h){
        var l=h.getLevel();BlockPos home=h.absolutePos(new BlockPos(2000,110,0)),socket=home.offset(-22,2,-10);
        for(var p:BlockPos.betweenClosed(home.offset(-6,0,-6),home.offset(6,32,22)))l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
        for(int x=(home.getX()-8)>>4;x<=(home.getX()+8)>>4;x++)for(int z=(home.getZ()-8)>>4;z<=(home.getZ()+24)>>4;z++)l.setChunkForced(x,z,true);
        h.runAfterDelay(20,()->{
            var previous=l.getDifficulty();l.getServer().setDifficulty(Difficulty.NORMAL,true);
            l.setBlock(socket,ModBlocks.PROMETHEUS_VOICE.get().defaultBlockState(),3);((PrometheusBlockEntity)l.getBlockEntity(socket)).bind(home);
            var player=CombatTestPlayer.create(l,home.south(15));var bird=ModEntities.BRONZE_EAGLE.get().create(l);bird.bind(socket,home);bird.setPos(Vec3.atBottomCenterOf(home.above(9)));l.addFreshEntity(bird);
            try{
                bird.phase(BronzeEagleEntity.CIRCLE,69);bird.tick();
                h.assertTrue(bird.phase()==BronzeEagleEntity.TELL&&player.getHealth()==20,"The dive announces its locked ground position before doing damage");
                for(int i=0;i<BronzeEagleEntity.DIVE_WARNING;i++)bird.tick();
                h.assertTrue(bird.phase()==BronzeEagleEntity.DIVE&&player.getHealth()==20,"The complete dive warning is safe");
                for(int i=0;i<28;i++)bird.tick();
                h.assertTrue(player.getHealth()<20,"A player who stays in the marked dive lane is struck by the moving body");
                h.assertTrue(bird.phase()==BronzeEagleEntity.RECOVER,"The dive exposes the eagle rather than circling out of reach");
                for(int i=0;i<45;i++)bird.tick();
                h.assertTrue(bird.getY()<home.getY()+1&&bird.phase()==BronzeEagleEntity.RECOVER,"Recovery leaves a long reachable melee window");
                player.setHealth(20);player.invulnerableTime=0;bird.setPos(Vec3.atBottomCenterOf(home.above(9)));
                bird.phase(BronzeEagleEntity.CIRCLE,69);bird.tick();h.assertTrue(bird.phase()==BronzeEagleEntity.PINIONS,"The second attack alternates instead of repeating the dive");
                bird.phase(BronzeEagleEntity.PINIONS,37);bird.tick();
                var projectiles=l.getEntitiesOfClass(BronzeFeatherEntity.class,bird.getBoundingBox().inflate(30));h.assertTrue(projectiles.size()==1,"A sound must accompany a real moving pinion");
                var pinion=projectiles.getFirst();for(int i=0;i<80&&!pinion.isRemoved();i++)pinion.tick();
                h.assertTrue(player.getHealth()<20&&pinion.isRemoved(),"The visible pinion can actually damage a survival player");h.succeed();
            }finally{bird.discard();l.getServer().getPlayerList().remove(player);l.removeBlock(socket,false);l.getServer().setDifficulty(previous,true);for(int x=(home.getX()-8)>>4;x<=(home.getX()+8)>>4;x++)for(int z=(home.getZ()-8)>>4;z<=(home.getZ()+24)>>4;z++)l.setChunkForced(x,z,false);}
        });
    }
}
